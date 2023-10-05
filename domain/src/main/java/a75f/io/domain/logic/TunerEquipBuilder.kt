package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.sync.CcuRegistrationHandler
import a75f.io.domain.api.Domain
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.ph.core.Tags

class TunerEquipBuilder(private val hayStack : CCUHsApi) : DefaultEquipBuilder() {

    fun buildEquipAndPoints(siteRef: String) : String{
        return buildTunerEquipAndPoints(ModelLoader.getBuildingEquipModelDef(hayStack.context), siteRef)
    }
    fun buildTunerEquipAndPoints(modelDef: ModelDirective, siteRef : String): String {
        val hayStackEquip = buildEquip(modelDef, null, siteRef)
        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        DomainManager.addEquip(hayStackEquip)
        CcuLog.i(Domain.LOG_TAG," Created tuner point ${hayStackEquip.domainName}")
        createPoints(modelDef, equipId, siteRef)
        return equipId
    }

    private fun createPoints(modelDef: ModelDirective, equipRef: String, siteRef: String) {

        modelDef.points.forEach {
            val hayStackPoint = buildPoint(it, null, equipRef, siteRef)
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            hayStack.writeDefaultTunerValById(pointId, it.defaultValue.toString().toDouble())
            DomainManager.addPoint(hayStackPoint)
            CcuLog.i(Domain.LOG_TAG," Created tuner point ${it.domainName}")
        }
    }

    private fun createPoints(modelDef: ModelDirective, entityConfig: EntityConfiguration, equipRef: String, siteRef: String) {
        val tz = hayStack.timeZone
        entityConfig.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, null, equipRef, siteRef, tz)
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
                hayStack.writeDefaultTunerValById(pointId, modelPointDef.defaultValue.toString().toDouble())
                DomainManager.addPoint(hayStackPoint)
                CcuLog.i(Domain.LOG_TAG," Created Tuner point ${point.domainName}")
            }
        }
    }

    private fun updatePoints(modelDef: ModelDirective, entityConfiguration: EntityConfiguration, equipRef: String, siteRef: String) {
        val tz = hayStack.timeZone
        entityConfiguration.tobeUpdated.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, null, equipRef, siteRef, tz)
                hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                hayStackPoint.id = existingPoint["id"].toString()
                hayStack.writeDefaultTunerValById(hayStackPoint.id, modelPointDef.defaultValue.toString().toDouble())
                DomainManager.addPoint(hayStackPoint)
                CcuLog.i(Domain.LOG_TAG," Updated Tuner point ${point.domainName}")
            }
        }
    }

    private fun deletePoints(entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeDeleted.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            hayStack.deleteWritablePoint(existingPoint["id"].toString())
        }
    }

    fun updateEquipAndPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, siteRef: String) {
        val tunerEquip = hayStack.readEntity("tuner and equip")
        if (tunerEquip.isNotEmpty()) {
            updateEquipAndPoints(modelDef, updateConfig, tunerEquip[Tags.ID].toString(), siteRef)
        }

    }
    private fun updateEquipAndPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, equipId: String, siteRef: String) : String{

        val hayStackEquip = buildEquip(modelDef, null, siteRef)
        hayStack.updateEquip(hayStackEquip, equipId)

        DomainManager.addEquip(hayStackEquip)
        //val updateConfig = getEntityConfigForUpdate(modelDef, equipId)
        createPoints(modelDef, updateConfig, equipId, siteRef)
        updatePoints(modelDef, updateConfig, equipId, siteRef)
        deletePoints(updateConfig, equipId)
        propagateTunerPoints(modelDef, updateConfig, siteRef)
        return equipId
    }

    private fun propagateTunerPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, siteRef: String) {
        propagateAddZoneTunerPoints(modelDef, updateConfig, siteRef)
        propagateAddSystemTunerPoints(modelDef, updateConfig, siteRef)
        propagateUpdateTunerPoints(modelDef, updateConfig, siteRef)
        propagateDeleteTunerPoints(modelDef, updateConfig, siteRef)
    }
    private fun propagateAddZoneTunerPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, siteRef: String) {
        updateConfig.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.let {
                val groupTag = getZoneMarkerTagForTunerPointRef(modelPointDef)
                val equips = hayStack.readAllEntities("equip and zone and $groupTag")
                equips.forEach { equip ->
                    val hayStackPoint = buildPoint(
                        modelPointDef,
                        null,
                        equip["id"].toString(),
                        siteRef,
                        equip["tz"].toString()
                    )
                    val pointId = hayStack.addPoint(hayStackPoint)
                    hayStackPoint.id = pointId
                    hayStack.writeDefaultTunerValById(
                        pointId,
                        modelPointDef.defaultValue.toString().toDouble()
                    )
                    DomainManager.addPoint(hayStackPoint)
                    CcuLog.i(Domain.LOG_TAG, " Created Tuner point ${point.domainName} on " + equip)
                }
            } ?: CcuLog.i(Domain.LOG_TAG, "Migration zone point does not exist for ${point.domainName}")
        }
    }

    private fun propagateAddSystemTunerPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, siteRef: String) {
        updateConfig.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName && it.tagNames.contains("system")}
            modelPointDef?.let {
                val groupTag = getSystemMarkerTagForTunerPointRef(modelPointDef)
                val equip = hayStack.readEntity("equip and $groupTag")
                if (equip.isNotEmpty()) {
                    val hayStackPoint = buildPoint(
                        modelPointDef,
                        null,
                        equip["id"].toString(),
                        siteRef,
                        equip["tz"].toString()
                    )
                    val pointId = hayStack.addPoint(hayStackPoint)
                    hayStackPoint.id = pointId
                    hayStack.writeDefaultTunerValById(
                        pointId,
                        modelPointDef.defaultValue.toString().toDouble()
                    )
                    DomainManager.addPoint(hayStackPoint)
                    CcuLog.i(Domain.LOG_TAG, " Created Tuner point ${point.domainName} on " + equip)
                }
            } ?: CcuLog.i(Domain.LOG_TAG, "Migration system point does not exist for ${point.domainName}")
        }
    }

    private fun propagateUpdateTunerPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, siteRef: String) {
        updateConfig.tobeUpdated.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val points = hayStack.readAllEntities("point and not default and domainName == \"${point.domainName}\"")
                points.forEach { existingPoint ->
                    val hayStackPoint = buildPoint(modelPointDef, null,
                                existingPoint["equipRef"].toString(), siteRef, existingPoint["tz"].toString())
                    hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                    hayStackPoint.id = existingPoint["id"].toString()
                    hayStack.writeDefaultTunerValById(hayStackPoint.id, modelPointDef.defaultValue.toString().toDouble())
                    DomainManager.addPoint(hayStackPoint)
                    CcuLog.i(Domain.LOG_TAG," Updated Tuner point $point")
                }
            } ?: CcuLog.i(Domain.LOG_TAG, "Migration point does not exist for ${point.domainName}")
        }
    }

    private fun propagateDeleteTunerPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, siteRef: String) {
        updateConfig.tobeUpdated.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val points = hayStack.readAllEntities("point and not default and domainName == \"${point.domainName}\"")
                points.forEach { existingPoint ->
                    hayStack.deleteEntityTree( existingPoint["id"].toString())
                    CcuLog.i(Domain.LOG_TAG," Deleted Tuner point $point")
                }
            } ?: CcuLog.i(Domain.LOG_TAG, "Migration point does not exist for ${point.domainName}")
        }
    }

    /**
     * Model should have appropriate tuner group on Building tuner points.
     * Otherwise this method returns NULL and they wont be propagated.
     */
    private fun getZoneMarkerTagForTunerPointRef(pointDef: ModelPointDef) : String? {
        val tunerGroupTag = pointDef.tags.find { it.name == "tunerGroup" }
        if (tunerGroupTag == null) {
            CcuLog.e(Domain.LOG_TAG, " tunerGroup does not exist for ${pointDef.domainName}")
            return null
        }
        //TODO - define tag constants
        return when(tunerGroupTag.defaultValue) {
            "oao" -> return "oao"
            "vav" -> return "system and vav"
            "dab" -> return "system and dab"
            else -> null
        }
    }

    private fun getSystemMarkerTagForTunerPointRef(pointDef: ModelPointDef) : String? {
        val tunerGroupTag = pointDef.tags.find { it.name == "tunerGroup" }
        if (tunerGroupTag == null) {
            CcuLog.e(Domain.LOG_TAG, " tunerGroup does not exist for ${pointDef.domainName}")
            return null
        }
        //TODO - define tag constants
        return when(tunerGroupTag.defaultValue) {
            "vav" -> return "vav"
            "dab" -> return "dab"
            "ti" -> return "ti"
            "oao" -> return "oao"
            "otn" -> return "otn"
            "ti" -> return "ti"
            else -> null
        }
    }
    fun getEntityConfigForUpdate(modelDef: ModelDirective, equipRef: String) : EntityConfiguration{
        val entityNameMap = mutableMapOf<String, Double>()
        val tunerPoints =
            hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
        tunerPoints.forEach {
            val pointVal = hayStack.readDefaultValByLevel(it["id"].toString(), HayStackConstants.DEFAULT_INIT_VAL_LEVEL)
            //TODO - handle string type val if there is any.
            if (pointVal is Number) {
                entityNameMap[it["domainName"].toString()] = pointVal
            }
        }

        val newEntityConfig = EntityConfiguration()

        entityNameMap.keys.forEach{ entityName ->
            if (modelDef.points.find { it.domainName == entityName} == null) {
                newEntityConfig.tobeDeleted.add(EntityConfig(entityName))
            }
        }
       modelDef.points.forEach{
            if (entityNameMap.keys.contains(it.domainName)) {
                newEntityConfig.tobeUpdated.add(EntityConfig(it.domainName))
            } else {
                newEntityConfig.tobeAdded.add(EntityConfig(it.domainName))
            }
        }
        return newEntityConfig
    }

    fun updateBackendBuildingTuner(siteRef: String, hayStack: CCUHsApi) {
        val buildingTuner = hayStack.getRemoteBuildingTunerEquip(siteRef)
        CcuLog.i("CCU_TUNER", " Remote buildingTuner $buildingTuner")
        buildingTuner?.let {
            val ccuSyncHandler = CcuRegistrationHandler()
            val ccu = hayStack.readEntity("device and ccu")
            ccuSyncHandler.updateCcuDeviceId(ccu, ccu["id"].toString(), buildingTuner["id"].toString())
        }
    }

}