package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ResourceHelper
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective

class TunerEquipBuilder(private val hayStack : CCUHsApi) : DefaultEquipBuilder() {

    fun buildEquipAndPoints() : String{
        return buildTunerEquipAndPoints(ModelLoader.getBuildingEquipModelDef())
    }
    fun buildTunerEquipAndPoints(modelDef: ModelDirective): String {
        val hayStackEquip = buildEquip(modelDef, null)
        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, equipId)
        return equipId
    }

    private fun createPoints(modelDef: ModelDirective, equipRef: String) {

        modelDef.points.forEach {
            val hayStackPoint = buildPoint(it, null, equipRef)
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            hayStack.writeDefaultTunerValById(pointId, it.defaultValue.toString().toDouble())
            DomainManager.addPoint(hayStackPoint)
        }
    }

    private fun createPoints(modelDef: ModelDirective, entityConfig: EntityConfiguration, equipRef: String) {
        entityConfig.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, null, equipRef)
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
                hayStack.writeDefaultTunerValById(pointId, modelPointDef.defaultValue.toString().toDouble())
                DomainManager.addPoint(hayStackPoint)
            }
        }
    }

    private fun updatePoints(modelDef: ModelDirective, entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeUpdated.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, null, equipRef)
                hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                hayStackPoint.id = existingPoint["id"].toString()
                hayStack.writeDefaultTunerValById(hayStackPoint.id, modelPointDef.defaultValue.toString().toDouble())
                DomainManager.addPoint(hayStackPoint)
            }
        }
    }

    private fun deletePoints(entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeDeleted.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            hayStack.deleteWritablePoint(existingPoint["id"].toString())
        }
    }

    fun updateEquipAndPoints(modelDef: ModelDirective, equipId: String) : String{

        val hayStackEquip = buildEquip(modelDef, null)
        hayStack.updateEquip(hayStackEquip, equipId)

        DomainManager.addEquip(hayStackEquip)
        val updateConfig = getEntityConfigForUpdate(modelDef, equipId)
        createPoints(modelDef, updateConfig, equipId)
        updatePoints(modelDef, updateConfig, equipId)
        deletePoints(updateConfig, equipId)
        return equipId
    }

    private fun getEntityConfigForUpdate(modelDef: ModelDirective, equipRef: String) : EntityConfiguration{
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