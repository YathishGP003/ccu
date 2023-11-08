package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.getConfig
import a75f.io.logger.CcuLog
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

class ProfileEquipBuilder(private val hayStack : CCUHsApi) : DefaultEquipBuilder() {

    /**
     * Creates a new haystack equip and all the points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective, siteRef : String, profileName: String?) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val hayStackEquip = buildEquip(EquipBuilderConfig(modelDef, configuration, siteRef, hayStack.timeZone),profileName)
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
    fun updateEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective, siteRef: String,profileName: String?) : String{
        CcuLog.i("DEV_DEBUG", "Updated configuration is started")
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val equip = getEquip(configuration,modelDef.domainName)
        CcuLog.i("DEV_DEBUG", "Equip Details :  ${equip.toString()}")
        val equipId =  equip?.get("id").toString()
        val updatedConfiguration = ReconfigHandler
            .getEntityReconfiguration(equipId, hayStack, entityMapper.getEntityConfiguration(configuration))

        val hayStackEquip = buildEquip(EquipBuilderConfig(modelDef, configuration, siteRef, hayStack.timeZone),profileName)
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
                Log.i("DEV_DEBUG", "createPoints: ${point.domainName}")
                val enableConfig = profileConfiguration.getEnableConfigs().getConfig(point.domainName)
                if (enableConfig != null) {
                    initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt() )
                } else if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                    initializeDefaultVal(hayStackPoint, modelPointDef.defaultValue as Number)
                }
                DomainManager.addPoint(hayStackPoint)
            }
        }
    }

    private fun updatePoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration,
                             entityConfiguration: EntityConfiguration, equipRef: String, siteRef: String) {
        val tz = hayStack.timeZone
        entityConfiguration.tobeUpdated.forEach { point -> // New changed point
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
            val existingPoint = getPoint(point.domainName,equipRef)
            if (existingPoint!!.isNotEmpty())
                hayStack.deleteEntity(existingPoint["id"]!!.toString())
        }
    }

    private fun initializeDefaultVal(point : Point, defaultVal : Number) {
        Log.i("DEV_DEBUG", "${point.domainName}: default : $defaultVal ")
        hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
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