package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
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
        CcuLog.i("CCU_DM"," Created tuner point ${hayStackEquip.domainName}")
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
            CcuLog.i("CCU_DM"," Created tuner point ${it.domainName}")
        }
    }

    private fun createPoints(modelDef: ModelDirective, entityConfig: EntityConfiguration, equipRef: String, siteRef: String) {
        entityConfig.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, null, equipRef, siteRef)
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
                hayStack.writeDefaultTunerValById(pointId, modelPointDef.defaultValue.toString().toDouble())
                DomainManager.addPoint(hayStackPoint)
                CcuLog.i("CCU_DM"," Created Tuner point ${point.domainName}")
            }
        }
    }

    private fun updatePoints(modelDef: ModelDirective, entityConfiguration: EntityConfiguration, equipRef: String, siteRef: String) {
        entityConfiguration.tobeUpdated.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, null, equipRef, siteRef)
                hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                hayStackPoint.id = existingPoint["id"].toString()
                hayStack.writeDefaultTunerValById(hayStackPoint.id, modelPointDef.defaultValue.toString().toDouble())
                DomainManager.addPoint(hayStackPoint)
                CcuLog.i("CCU_DM"," Updated Tuner point ${point.domainName}")
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
            updateEquipAndPoints(modelDef, updateConfig, tunerEquip.get(Tags.ID).toString(), siteRef)
        }

    }
    fun updateEquipAndPoints(modelDef: ModelDirective, updateConfig: EntityConfiguration, equipId: String, siteRef: String) : String{

        val hayStackEquip = buildEquip(modelDef, null, siteRef)
        hayStack.updateEquip(hayStackEquip, equipId)

        DomainManager.addEquip(hayStackEquip)
        //val updateConfig = getEntityConfigForUpdate(modelDef, equipId)
        createPoints(modelDef, updateConfig, equipId, siteRef)
        updatePoints(modelDef, updateConfig, equipId, siteRef)
        deletePoints(updateConfig, equipId)
        return equipId
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

}