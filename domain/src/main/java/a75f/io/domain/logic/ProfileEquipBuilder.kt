package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.getConfig
import a75f.io.domain.util.TunerUtil
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

class ProfileEquipBuilder(private val hayStack : CCUHsApi) : DefaultEquipBuilder() {

    /**
     * Creates a new haystack equip and all the points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun buildEquipAndPoints(
        configuration: ProfileConfiguration,
        modelDef: ModelDirective,
        siteRef: String
    ) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val hayStackEquip = buildEquip(EquipBuilderConfig(modelDef, configuration, siteRef, hayStack.timeZone))
        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, entityConfiguration, equipId, siteRef)

        return equipId
    }

    /**
     * Updates and existing haystack equip and it points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun updateEquipAndPoints(
        configuration: ProfileConfiguration,
        modelDef: ModelDirective,
        siteRef: String
    ) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val equip = getEquip(configuration,modelDef.domainName)
        val equipId =  equip?.get("id").toString()
        val updatedConfiguration = ReconfigurationHandler
            .getEntityReconfiguration(equipId, hayStack, entityMapper.getEntityConfiguration(configuration),configuration)
        val hayStackEquip = buildEquip(EquipBuilderConfig(modelDef, configuration, siteRef, hayStack.timeZone))
        hayStack.updateEquip(hayStackEquip, equipId)

        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, updatedConfiguration, equipId, siteRef)
        updatePoints(modelDef, configuration, updatedConfiguration, equipId, siteRef)
        deletePoints(updatedConfiguration, equipId)
        return equipId
    }

    private fun createPoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration,
                                        equipRef: String, siteRef: String) {

        val tz = hayStack.timeZone
        entityConfiguration.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(PointBuilderConfig(modelPointDef, profileConfiguration, equipRef, siteRef, tz))
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
                val enableConfig = profileConfiguration.getEnableConfigs().getConfig(point.domainName)
                val value = if (modelPointDef.defaultValue == null) 0.0 else (modelPointDef.defaultValue as Number).toDouble()
                  if (isItTunerPoint(hayStackPoint.markers)) {
                      TunerUtil.copyDefaultBuildingTunerVal(
                          pointId,
                          hayStackPoint.domainName,
                          hayStack
                      )
                  } else if (enableConfig != null) {
                      initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt())
                  } else if (modelPointDef.tagNames.contains("writable")) {
                      initializeDefaultVal(hayStackPoint, value)
                  } else if (modelPointDef.tagNames.contains("his")) {
                      hayStack.writeHisValById(modelPointDef.id, value)
                  }

                DomainManager.addPoint(hayStackPoint)
            }
        }
    }

    private fun isItTunerPoint(mutableList: MutableList<String>): Boolean {
        return mutableList.contains(Tags.TUNER)
    }
    private fun updatePoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration,
                             entityConfiguration: EntityConfiguration, equipRef: String, siteRef: String) {
        val tz = hayStack.timeZone
        entityConfiguration.tobeUpdated.forEach { point -> // New changed point
            Log.i("DEV_DEBUG", "tobeUpdated : ${point.domainName}")
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"") // existing point

            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val valueConfigPoint = profileConfiguration.getDependencies().find { it.domainName == point.domainName }
                if (valueConfigPoint != null) {
                    val currentPointId = getPoint(point.domainName,equipRef)?.get("id").toString()
                    val currentValue = hayStack.readDefaultValById(currentPointId)
                    Log.i("DEV_DEBUG", "updatePoints $currentPointId")
                    Log.i("DEV_DEBUG", "updatePoints ${domainName}: ${valueConfigPoint.currentVal} = $currentValue")
                    if (valueConfigPoint.currentVal != currentValue.toDouble()) {
                        hayStack.writeDefaultValById(currentPointId,valueConfigPoint.currentVal)
                        Log.i("DEV_DEBUG", "After change change ${hayStack.readDefaultStrValById(currentPointId)}" )
                    }
                } else {
                    val enableConfig =
                        profileConfiguration.getEnableConfigs().getConfig(point.domainName)
                    val hayStackPoint = buildPoint(
                        PointBuilderConfig(modelPointDef, profileConfiguration, equipRef, siteRef, tz))
                    hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                    hayStackPoint.id = existingPoint["id"].toString()
                    if (enableConfig != null) {
                        initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt())
                    } else if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                        initializeDefaultVal(hayStackPoint, modelPointDef.defaultValue as Number)
                    }
                    DomainManager.addPoint(hayStackPoint)
                }
            }
        }
    }

    private fun deletePoints(entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeDeleted.forEach { point ->
            Log.i("DEV_DEBUG", "tobeDeleted : ${point.domainName}")
            val existingPoint = getPoint(point.domainName,equipRef)
            if (existingPoint!!.isNotEmpty())
                hayStack.deleteEntity(existingPoint["id"]!!.toString())
        }
    }

    private fun initializeDefaultVal(point: Point, defaultVal: Number) {
        hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
        if (point.markers.contains("his"))
            hayStack.writeHisValById(point.id, defaultVal.toDouble())
    /*
        when {
            point.markers.contains("config") *//*&& point.defaultVal is String*//*-> hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
            point.markers.contains("tuner") -> TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
            point.markers.contains("min") -> TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
            point.markers.contains("max") -> TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
        }*/
    }

    fun getEquip(configuration: ProfileConfiguration, domainName: String): HashMap<Any, Any>? {
        return if (configuration.roomRef.contentEquals("SYSTEM")) {
            hayStack.readEntity("equip and system and not modbus")
        } else {
            hayStack.readEntity("equip and domainName == \"$domainName\"")
        }
    }

    private fun getPoint(domainName: String, equipRef: String): HashMap<Any, Any>? {
        return hayStack.readEntity("domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
}