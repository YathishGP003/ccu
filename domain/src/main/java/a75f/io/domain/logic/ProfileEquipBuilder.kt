package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.getConfig
import a75f.io.domain.util.TunerUtil
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

class ProfileEquipBuilder(private val hayStack : CCUHsApi) : DefaultEquipBuilder() {

    /**
     * Creates a new haystack equip and all the points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective, siteRef : String) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val hayStackEquip = buildEquip(modelDef, configuration, siteRef)
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
    fun updateEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective, siteRef: String) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = ReconfigHandler
            .getEntityReconfiguration(configuration.nodeAddress, hayStack, entityMapper.getEntityConfiguration(configuration))

        val equip = hayStack.readEntity(
            "equip and group == \"${configuration.nodeAddress}\"")

        val equipId =  equip["id"].toString()
        val hayStackEquip = buildEquip(modelDef, configuration, siteRef)
        hayStack.updateEquip(hayStackEquip, equipId)

        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, entityConfiguration, equipId, siteRef)
        updatePoints(modelDef, configuration, entityConfiguration, equipId, siteRef)
        deletePoints(entityConfiguration, equipId)
        return equipId
    }

    private fun createPoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration,
                                        equipRef: String, siteRef: String) {
        entityConfiguration.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, profileConfiguration, equipRef, siteRef)
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
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
        entityConfiguration.tobeUpdated.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, profileConfiguration, equipRef, siteRef)
                hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                hayStackPoint.id = existingPoint["id"].toString()
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

    private fun deletePoints(entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeDeleted.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            hayStack.deleteEntity(existingPoint["id"].toString())
        }
    }

    private fun initializeDefaultVal(point : Point, defaultVal : Number) {
        when{
            point.markers.contains("config") /*&& point.defaultVal is String*/-> hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
            point.markers.contains("tuner") -> TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
        }
    }

}