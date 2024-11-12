package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.getConfig
import a75f.io.domain.cutover.BuildingEquipCutOverMapping
import a75f.io.domain.cutover.getDomainNameFromDis
import a75f.io.domain.util.TunerUtil
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.PointConfiguration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.projecthaystack.HDateTime
import kotlin.system.measureTimeMillis

class ProfileEquipBuilder(private val hayStack : CCUHsApi) : DefaultEquipBuilder() {

    /**
     * Creates a new haystack equip and all the points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective, siteRef : String, equipDis: String) : String{
        CcuLog.i(Domain.LOG_TAG, "buildEquipAndPoints $configuration")
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val hayStackEquip = buildEquip(EquipBuilderConfig(modelDef, configuration, siteRef, hayStack.timeZone, equipDis))
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
        if (systemEquip.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "addEquip - Invalid System equip , ahuRef cannot be applied")
        } else if (isStandAloneEquip(hayStackEquip)) {
            hayStackEquip.gatewayRef = systemEquip[Tags.ID].toString()
            CcuLog.i(Domain.LOG_TAG, "Added gateway ref")
        } else {
            hayStackEquip.ahuRef = systemEquip[Tags.ID].toString()
            CcuLog.i(Domain.LOG_TAG, "Added ahu ref")
        }

        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        val time = measureTimeMillis {
            createPoints(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)
        }
        CcuLog.i(Domain.LOG_TAG, "Time taken to createPoints: $time ms")
        DomainManager.addDomainEquip(hayStackEquip)
        DomainManager.addEquip(hayStackEquip)
        return equipId
    }

    /**
     * Updates and existing haystack equip and it points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun updateEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective, siteRef: String, equipDis: String,
                             isReconfiguration : Boolean = false) : String{
        CcuLog.i(Domain.LOG_TAG, "updateEquipAndPoints $configuration isReconfiguration $isReconfiguration")
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)

        val equip = if(configuration.nodeAddress == 99){
            hayStack.readEntity("equip and system and not modbus and not connectModule")
        }else{
            hayStack.readEntity("equip and group == \"${configuration.nodeAddress}\"")
        }

        val equipId =  equip["id"].toString()

        val entityConfiguration = if (isReconfiguration) {
            ReconfigHandler.getEntityReconfiguration(
                equipId, hayStack,
                entityMapper.getEntityConfiguration(configuration), configuration, modelDef
            )
        } else {
            getEntityUpdateConfiguration(equipId, hayStack,entityMapper.getEntityConfiguration(configuration))
        }

        val hayStackEquip = buildEquip(EquipBuilderConfig(modelDef, configuration, siteRef, hayStack.timeZone, equipDis))
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
        if (systemEquip.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "addEquip - Invalid System equip , ahuRef cannot be applied")
        } else if (isStandAloneEquip(hayStackEquip)) {
            hayStackEquip.gatewayRef = systemEquip[Tags.ID].toString()
        } else {
            hayStackEquip.ahuRef = systemEquip[Tags.ID].toString()
        }

        hayStack.updateEquip(hayStackEquip, equipId)

        //TODO - Fix crash
        //DomainManager.addEquip(hayStackEquip)
        val time = measureTimeMillis {
            createPoints(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)
        }
        CcuLog.i(Domain.LOG_TAG, "Time taken to createPoints during update: $time ms")
        if (isReconfiguration) {
            val updatedPointsDuration = measureTimeMillis {
                updatePointValues(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)
            }
            CcuLog.i(Domain.LOG_TAG, "Time taken to updatePointValues: $updatedPointsDuration ms")
        } else {
            updatePoints(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)
        }
        deletePoints(entityConfiguration, equipId)
        return equipId
    }

    private fun getEntityUpdateConfiguration(equipRef: String, hayStack: CCUHsApi,
                                             config : EntityConfiguration) : EntityConfiguration {

        val existingEntityList = hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
            .map { it["domainName"].toString() }
        CcuLog.i(Domain.LOG_TAG, "Equip currently has ${existingEntityList.size} points")
        val newEntityConfig = EntityConfiguration()

        existingEntityList.forEach{ entityName ->
            if (config.tobeAdded.find { it.domainName == entityName} == null) {
                if (entityName != "null") {
                    newEntityConfig.tobeDeleted.add(EntityConfig(entityName))
                } else {
                    CcuLog.i(Domain.LOG_TAG, "Invalid point found in equip $equipRef")
                }
            }
        }
        config.tobeAdded.forEach{
            if (existingEntityList.contains(it.domainName)) {
                newEntityConfig.tobeUpdated.add(it)
            } else {
                newEntityConfig.tobeAdded.add(it)
            }
        }
        return newEntityConfig
    }

    private fun createPoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration,
                             equipRef: String, siteRef: String, equipDis: String) {

        val tz = hayStack.timeZone
        runBlocking {
            val deferredResults = entityConfiguration.tobeAdded.map { point ->
                async(highPriorityDispatcher) {
                    try {
                        CcuLog.i(Domain.LOG_TAG, "addPoint - ${point.domainName}")
                        val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
                        modelPointDef?.run {
                            createPoint(PointBuilderConfig(modelPointDef, profileConfiguration, equipRef, siteRef, tz, equipDis))
                        }
                    } catch (e: Exception) {
                        CcuLog.e(Domain.LOG_TAG, "Error adding point ${point.domainName}: ${e.message}")
                    }
                }
            }
            deferredResults.awaitAll()
        }
    }

    private fun updatePoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration,
                             entityConfiguration: EntityConfiguration, equipRef: String, siteRef: String, equipDis: String) {
        val tz = hayStack.timeZone
        entityConfiguration.tobeUpdated.forEach { point ->
            CcuLog.i(Domain.LOG_TAG, "updatePoint - ${point.domainName}")
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(PointBuilderConfig(modelPointDef, profileConfiguration, equipRef, siteRef, tz, equipDis))
                hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                hayStackPoint.id = existingPoint["id"].toString()
                if (profileConfiguration.getEnableConfigs().getConfig(point.domainName) != null) {
                    val enableConfig = profileConfiguration.getEnableConfigs().getConfig(point.domainName)
                    if (enableConfig != null) {
                        initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt() )
                    }
                } else if (profileConfiguration.getAssociationConfigs().getConfig(point.domainName) != null) {
                    val associationConfig = profileConfiguration.getAssociationConfigs().getConfig(point.domainName)
                    associationConfig?.associationVal?.let {
                        initializeDefaultVal(hayStackPoint, it)
                    }
                } else if (profileConfiguration.getValueConfigs().getConfig(point.domainName) != null) {
                    val valueConfig = profileConfiguration.getValueConfigs().getConfig(point.domainName)
                    if (valueConfig != null) {
                        initializeDefaultVal(hayStackPoint, valueConfig.currentVal )
                    }
                }
                else if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                    initializeDefaultVal(hayStackPoint, modelPointDef.defaultValue as Number)
                }
            }
        }
    }

    private fun updatePointValues(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration,
                                  entityConfiguration: EntityConfiguration, equipRef: String, siteRef: String, equipDis: String) {
        runBlocking {
            entityConfiguration.tobeUpdated.forEach { point ->
                async(highPriorityDispatcher) {
                    CcuLog.i(Domain.LOG_TAG, "updatePointValues - ${point.domainName}")
                    val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
                    val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
                    modelPointDef?.run {
                        var configVal = getConfigValue(profileConfiguration, modelPointDef , point.domainName)
                        if (configVal == null) {
                            if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                                configVal = modelPointDef.defaultValue as Double
                            }
                        }
                        configVal?.let {
                            val currentVal = hayStack.readDefaultValById(existingPoint["id"].toString())
                            if (configVal != currentVal) {
                                hayStack.writeDefaultValById(existingPoint["id"].toString(), configVal)
                            }
                        }
                        if (existingPoint.contains("his")) {
                            val priorityVal = hayStack.readPointPriorityVal(existingPoint["id"].toString())
                            hayStack.writeHisValById(existingPoint["id"].toString(), priorityVal)
                        }
                    }
                }.await()
            }

        }
    }
    private fun deletePoints(entityConfiguration: EntityConfiguration, equipRef: String) {
       runBlocking {
            entityConfiguration.tobeDeleted.forEach { point ->
                async(highPriorityDispatcher) {
                    CcuLog.i(Domain.LOG_TAG, "deletePoint - ${point.domainName}")
                    val existingPoint =
                        hayStack.readEntity("domainName == \"" + point.domainName + "\" and equipRef == \"" + equipRef + "\"")
                    if (existingPoint.isNotEmpty()) {
                        hayStack.deleteEntity(existingPoint["id"].toString())
                    }
                }.await()
            }
        }
    }

    private fun getConfigValue(profileConfiguration: ProfileConfiguration, modelPoint : ModelPointDef, domainName : String) : Double? {
        return if (profileConfiguration.getEnableConfigs().getConfig(domainName) != null) {
            val enableConfig = profileConfiguration.getEnableConfigs().getConfig(domainName)
            enableConfig?.enabled?.toDouble()
        } else if (profileConfiguration.getAssociationConfigs().getConfig(domainName) != null) {
            val associationConfig = profileConfiguration.getAssociationConfigs().getConfig(domainName)
            associationConfig?.associationVal?.toDouble()
        } else if (profileConfiguration.getValueConfigs().getConfig(domainName) != null) {
            val valueConfig = profileConfiguration.getValueConfigs().getConfig(domainName)
            valueConfig?.currentVal
        } else {
            return null
        }
    }

    private fun initializeDefaultVal(point : Point, defaultVal : Number) {
        CcuLog.i(Domain.LOG_TAG,"InitializeDefaultVal ${point.domainName} - val $defaultVal")
        if (point.markers.contains("tuner")) {
            if (point.markers.contains(Tags.SYSTEM)) {
                TunerUtil.updateSystemTunerLevels(point.id, point.domainName, hayStack, defaultVal.toDouble())
            } else {
                TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack, defaultVal.toDouble())
            }
        } else {
            hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
        }
        if (point.markers.contains("his")) {
            val priorityVal = hayStack.readPointPriorityVal(point.id)
            hayStack.writeHisValById(point.id, priorityVal)
        }
    }

    fun createPoint(pointConfig: PointBuilderConfig) : String {
        val hayStackPoint = buildPoint(pointConfig)
        val pointId = hayStack.addPoint(hayStackPoint)
        hayStackPoint.id = pointId

        if (pointConfig.configuration?.getEnableConfigs()?.getConfig(pointConfig.modelDef.domainName) != null) {
            val enableConfig = pointConfig.configuration?.getEnableConfigs()?.getConfig(pointConfig.modelDef.domainName)
            if (enableConfig != null) {
                initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt() )
            }
        } else if (pointConfig.configuration?.getAssociationConfigs()?.getConfig(pointConfig.modelDef.domainName) != null) {
            val associationConfig = pointConfig.configuration?.getAssociationConfigs()?.getConfig(pointConfig.modelDef.domainName)
            associationConfig?.associationVal?.let {
                initializeDefaultVal(hayStackPoint, it)
            }
        } else if (pointConfig.configuration?.getValueConfigs()?.getConfig(pointConfig.modelDef.domainName) != null) {
            val valueConfig = pointConfig.configuration?.getValueConfigs()?.getConfig(pointConfig.modelDef.domainName)
            if (valueConfig != null) {
                initializeDefaultVal(hayStackPoint, valueConfig.currentVal )
            }
        } else if (pointConfig.modelDef.tagNames.contains("writable") && pointConfig.modelDef.defaultValue is Number) {
            if (pointConfig.modelDef.valueConstraint is MultiStateConstraint) {
                var enumValue: Int
                (pointConfig.modelDef.valueConstraint as MultiStateConstraint).allowedValues.get(pointConfig.modelDef.defaultValue as Int)
                    .let { enumIndex->
                        if (enumIndex.value.filter { it.isDigit() }.isNotEmpty()) {
                            enumValue = enumIndex.value.filter { it.isDigit() }.toInt()
                            initializeDefaultVal(hayStackPoint, enumValue.toDouble())
                        } else {
                            initializeDefaultVal(hayStackPoint, pointConfig.modelDef.defaultValue as Number)
                        }
                    }
            } else {
                initializeDefaultVal(hayStackPoint, pointConfig.modelDef.defaultValue as Number)
            }
        } else if (pointConfig.modelDef.tagNames.contains("his") && !(pointConfig.modelDef.domainName.equals(
                DomainName.heartBeat))) {
            // heartBeat is the one point where we don't want to initialize a hisVal to zero (since we want a gray dot on the zone screen, not green)
            hayStack.writeHisValById(pointId, 0.0)
        }

        DomainManager.addPoint(hayStackPoint)
        CcuLog.i(Domain.LOG_TAG," Created Equip point ${pointConfig.modelDef.domainName}")
        return pointId
    }
    private fun updatePoint(pointConfig: PointBuilderConfig, existingPoint : HashMap<Any, Any>) {
        val hayStackPoint = buildPoint(pointConfig)
        hayStackPoint.id = existingPoint["id"].toString()
        hayStackPoint.roomRef = existingPoint["roomRef"].toString()
        hayStackPoint.floorRef = existingPoint["floorRef"].toString()
        hayStackPoint.group = existingPoint["group"].toString()
        existingPoint["createdDateTime"]?.let {
            hayStackPoint.createdDateTime = HDateTime.make(existingPoint["createdDateTime"].toString())
        }
        hayStackPoint.lastModifiedBy = hayStack.getCCUUserName();
        hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())

        //TODO- Not changing the value during migration as it might change user configurations
        //hayStack.writeDefaultTunerValById(hayStackPoint.id, pointConfig.modelDef.defaultValue.toString().toDouble())
        DomainManager.addPoint(hayStackPoint)
        CcuLog.i(Domain.LOG_TAG," Updated Equip point ${pointConfig.modelDef.domainName}")
    }

    private fun updateEquip(
        equipRef: String,
        modelDef: SeventyFiveFProfileDirective,
        equipDis: String,
        isSystem: Boolean,
        siteRef: String,
        profileConfiguration: ProfileConfiguration,
        equipHashMap : HashMap<Any, Any>
    ) {
        val equip = buildEquip(
            EquipBuilderConfig(
                modelDef,
                profileConfiguration,
                siteRef,
                hayStack.timeZone
            )
        )
        if(equipHashMap.containsKey("ahuRef")){
            equip.ahuRef = equipHashMap["ahuRef"].toString()
        }
        if(equipHashMap.containsKey("gatewayRef")){
            equip.gatewayRef = equipHashMap["gatewayRef"].toString()
        }
        equip.displayName = equipDis
        if (isSystem) {
            equip.group = "99"
        }
        equipHashMap["createdDateTime"]?.let {
            equip.createdDateTime = HDateTime.make(equipHashMap["createdDateTime"].toString())
        }
        equip.lastModifiedBy = hayStack.getCCUUserName();
        hayStack.updateEquip(equip, equipRef)
        CcuLog.i(Domain.LOG_TAG, " Updated Equip ${equip.group}-${equip.domainName}")
    }
    fun doCutOverMigration(equipRef: String, modelDef : SeventyFiveFProfileDirective, equipDis : String,
                           mapping : Map<String, String>, profileConfiguration: ProfileConfiguration,
    isSystem: Boolean = false, equipHashMap: HashMap<Any, Any> = HashMap()) {
        CcuLog.i(Domain.LOG_TAG, "doCutOverMigration for $equipDis")
        var equipPoints =
            hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
        val site = hayStack.site
        //TODO-To be removed after testing is complete.
        var update = 0
        var delete = 0
        var add = 0
        var pass = 0
        equipPoints.filter { it["domainName"] == null}
            .forEach { dbPoint ->
                val modelPointName = getDomainNameFromDis(dbPoint, mapping)

                if (modelPointName == null) {
                    delete++
                    //DB point does not exist in model. Should be deleted.
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration : Redundant Point $dbPoint")
                    hayStack.deleteEntityTree(dbPoint["id"].toString())
                } else {
                    update++
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration Update with domainName $modelPointName : $dbPoint")
                    //println("Cut-Over migration Update $dbPoint")
                    val modelPoint = modelDef.points.find { it.domainName.equals(modelPointName, true)}
                    if (modelPoint != null) {
                        updatePoint(PointBuilderConfig(modelPoint, null, equipRef, site!!.id, site!!.tz, equipDis), dbPoint)
                    } else {
                        delete++
                        CcuLog.e(Domain.LOG_TAG, " Model point does not exist for domain name $modelPointName")
                        hayStack.deleteEntityTree(dbPoint["id"].toString())
                    }
                }
            }

        modelDef.points.forEach { modelPointDef ->

            val displayName = BuildingEquipCutOverMapping.findDisFromDomainName(modelPointDef.domainName)
            if (displayName == null && modelPointDef.configuration.configType == PointConfiguration.ConfigType.BASE) {
                add++
                //Point exists in model but not in mapping table or local db. create it.
                CcuLog.e(Domain.LOG_TAG, " Cut-Over migration Add ${modelPointDef.domainName} - $modelPointDef")
                //println(" Cut-Over migration Add ${modelPointDef.domainName}- $modelPointDef")
                if (!pointWithDomainNameExists(equipPoints, modelPointDef.domainName, mapping)) {
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration createPoint ${modelPointDef.domainName}")
                    createPoint(PointBuilderConfig(modelPointDef, profileConfiguration, equipRef, site!!.id, site?.tz, equipDis))
                }
            } else {
                //TODO- Need to consider the case when point exists in map but not in DB.
                CcuLog.e(Domain.LOG_TAG, " Cut-Over migration PASS $modelPointDef")
                //println(" Cut-Over migration PASS $modelPointDef")
                pass++
            }
        }

        CcuLog.i(
            Domain.LOG_TAG, "CutOver migration for ${modelDef.domainName} Total points DB: ${equipPoints.size} " +
                    " Model: ${modelDef.points.size} Map: ${mapping.size} ")
        CcuLog.i(Domain.LOG_TAG, " Deleted $delete Updated $update added $add pass $pass")
        CcuLog.i(Domain.LOG_TAG, "equipHashMap: $equipHashMap")
        updateEquip(equipRef, modelDef, equipDis, isSystem, site!!.id, profileConfiguration, equipHashMap)
        CcuLog.i(Domain.LOG_TAG, " Cut-Over migration completed for Equip ${modelDef.domainName}")
    }

    private fun pointWithDomainNameExists(dbPoints : List<Map<Any, Any>>, domainName : String, mapping : Map <String, String>) : Boolean{
        return dbPoints.any { dbPoint ->
            mapping.get(dbPoint["dis"].toString()
                .replace("\\s".toRegex(),"")
                .substringAfterLast("-")
            ).equals(domainName, ignoreCase = true) }
    }

    private fun isStandAloneEquip(equip : Equip) : Boolean {
        return equip.markers.contains(Tags.STANDALONE)
    }
}