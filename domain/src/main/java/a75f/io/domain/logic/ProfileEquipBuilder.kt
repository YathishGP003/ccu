package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.getConfig
import a75f.io.domain.cutover.BuildingEquipCutOverMapping
import a75f.io.domain.cutover.findDisFromDomainName
import a75f.io.domain.cutover.getDomainNameFromDis
import a75f.io.domain.cutover.pointWithDomainNameExists
import a75f.io.domain.util.TunerUtil
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

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
        val systemEquip = hayStack.readEntity("system and equip and not modbus")
        if (systemEquip.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "addEquip - Invalid System equip , ahuRef cannot be applied")
        } else if (systemEquip.contains(Tags.DEFAULT)) {
            hayStackEquip.gatewayRef = systemEquip[Tags.ID].toString()
        } else {
            hayStackEquip.ahuRef = systemEquip[Tags.ID].toString()
        }

        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        createPoints(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)
        DomainManager.addDomainEquip(hayStackEquip)
        DomainManager.addEquip(hayStackEquip)
        return equipId
    }

    /**
     * Updates and existing haystack equip and it points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun updateEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective, siteRef: String, equipDis: String) : String{
        CcuLog.i(Domain.LOG_TAG, "updateEquipAndPoints $configuration")
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)

        val entityConfiguration = ReconfigHandler
            .getEntityReconfiguration(configuration.nodeAddress, hayStack, entityMapper.getEntityConfiguration(configuration))

        val equip = hayStack.readEntity(
            "equip and group == \"${configuration.nodeAddress}\"")

        val equipId =  equip["id"].toString()
        val hayStackEquip = buildEquip(EquipBuilderConfig(modelDef, configuration, siteRef, hayStack.timeZone, equipDis))
        hayStack.updateEquip(hayStackEquip, equipId)

        //TODO - Fix crash
        //DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)
        updatePoints(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)
        deletePoints(entityConfiguration, equipId)
        return equipId
    }

    private fun createPoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration,
                                        equipRef: String, siteRef: String, equipDis: String) {
        val tz = hayStack.timeZone
        entityConfiguration.tobeAdded.forEach { point ->
            CcuLog.i(Domain.LOG_TAG, "addPoint - ${point.domainName}")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(PointBuilderConfig(modelPointDef, profileConfiguration, equipRef, siteRef, tz, equipDis))
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
                if (profileConfiguration.getEnableConfigs().getConfig(point.domainName) != null) {
                    val enableConfig = profileConfiguration.getEnableConfigs().getConfig(point.domainName)
                    if (enableConfig != null) {
                        initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt() )
                    }
                } else if (profileConfiguration.getAssociationConfigs().getConfig(point.domainName) != null) {
                    val associationConfig = profileConfiguration.getAssociationConfigs().getConfig(point.domainName)
                    if (associationConfig != null) {
                        initializeDefaultVal(hayStackPoint, associationConfig.associationVal )
                    }
                } else if (profileConfiguration.getValueConfigs().getConfig(point.domainName) != null) {
                    val valueConfig = profileConfiguration.getValueConfigs().getConfig(point.domainName)
                    if (valueConfig != null) {
                        initializeDefaultVal(hayStackPoint, valueConfig.currentVal )
                    }
                }
                else if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                    initializeDefaultVal(hayStackPoint, modelPointDef.defaultValue as Number)
                } else if (modelPointDef.tagNames.contains("his") && !(modelPointDef.domainName.equals(DomainName.heartBeat))) {
                    // heartBeat is the one point where we don't want to initialize a hisVal to zero (since we want a gray dot on the zone screen, not green)
                    hayStack.writeHisValById(pointId, 0.0)
                }

                DomainManager.addPoint(hayStackPoint)
            }
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
                DomainManager.addPoint(hayStackPoint)
            }

        }
    }

    private fun deletePoints(entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeDeleted.forEach { point ->
            CcuLog.i(Domain.LOG_TAG, "deletePoint - ${point.domainName}")
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            hayStack.deleteEntity(existingPoint["id"].toString())
        }
    }

    private fun initializeDefaultVal(point : Point, defaultVal : Number) {
        CcuLog.i(Domain.LOG_TAG,"InitializeDefaultVal ${point.domainName} - val $defaultVal")
        if (point.markers.contains("tuner")) {
            TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
        } else {
            hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
        }
        if (point.markers.contains("his")) {
            val priorityVal = hayStack.readPointPriorityVal(point.id)
            hayStack.writeHisValById(point.id, priorityVal)
        }
    }

    private fun updatePoint(pointConfig: PointBuilderConfig, existingPoint : HashMap<Any, Any>) {
        val hayStackPoint = buildPoint(pointConfig)
        hayStackPoint.id = existingPoint["id"].toString()
        hayStackPoint.roomRef = existingPoint["roomRef"].toString()
        hayStackPoint.floorRef = existingPoint["floorRef"].toString()
        hayStackPoint.group = existingPoint["group"].toString()
        hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())

        //TODO- Not changing the value during migration as it might change user configurations
        //hayStack.writeDefaultTunerValById(hayStackPoint.id, pointConfig.modelDef.defaultValue.toString().toDouble())
        DomainManager.addPoint(hayStackPoint)
        CcuLog.i(Domain.LOG_TAG," Updated Equip point ${pointConfig.modelDef.domainName}")
    }

    private fun updateEquip(equipRef: String, modelDef : SeventyFiveFProfileDirective, equipDis: String) {
        var equipDict = hayStack.readHDictById(equipRef)
        val equip = Equip.Builder().setHDict(equipDict).build()
        equip.domainName = modelDef.domainName
        equip.displayName = equipDis
        hayStack.updateEquip(equip, equip.id)
        CcuLog.i(Domain.LOG_TAG, " Updated Equip ${equip.group}-${equip.domainName}")
    }
    fun doCutOverMigration(equipRef: String, modelDef : SeventyFiveFProfileDirective, equipDis : String,
                           mapping : Map<String, String>) {
        var equipPoints =
            hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
        val site = hayStack.site
        //TODO-To be removed after testing is complete.
        var update = 0
        var delete = 0
        equipPoints.filter { it["domainName"] == null}
            .forEach { dbPoint ->
                val modelPointName = getDomainNameFromDis(dbPoint, mapping)

                if (modelPointName == null) {
                    delete++
                    //DB point does not exist in model. Should be deleted.
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration : Redundant Point $dbPoint")
                    //hayStack.deleteEntityTree(dbPoint["id"].toString())
                } else {
                    update++
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration Update with domainName $modelPointName : $dbPoint")
                    //println("Cut-Over migration Update $dbPoint")
                    val modelPoint = modelDef.points.find { it.domainName.equals(modelPointName, true)}
                    if (modelPoint != null) {
                        updatePoint(PointBuilderConfig(modelPoint, null, equipRef, site!!.id, site!!.tz, equipDis), dbPoint)
                    } else {
                        CcuLog.e(Domain.LOG_TAG, " Model point does not exist for domain name $modelPointName")
                    }
                }
            }

        CcuLog.e(
            Domain.LOG_TAG, "CutOver migration for ${modelDef.domainName} Total points DB: ${equipPoints.size} " +
                    " Model: ${modelDef.points.size} Map: ${mapping.size} ")
        CcuLog.e(Domain.LOG_TAG, " Deleted $delete Updated $update ")

        updateEquip(equipRef, modelDef, equipDis)
        CcuLog.e(Domain.LOG_TAG, " Cut-Over migration completed for Equip ${modelDef.domainName}")
    }

}