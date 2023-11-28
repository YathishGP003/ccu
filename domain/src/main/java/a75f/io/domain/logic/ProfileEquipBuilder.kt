package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.domain.api.Domain
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.getConfig
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
        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, entityConfiguration, equipId, siteRef, equipDis)

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
            CcuLog.i(Domain.LOG_TAG, "updateEquipAndPoints addPoint - ${point.domainName}")
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

    private fun updatePoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration,
                             entityConfiguration: EntityConfiguration, equipRef: String, siteRef: String, equipDis: String) {
        val tz = hayStack.timeZone
        entityConfiguration.tobeUpdated.forEach { point ->
            CcuLog.i(Domain.LOG_TAG, "updateEquipAndPoints updatePoint - ${point.domainName}")
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(PointBuilderConfig(modelPointDef, profileConfiguration, equipRef, siteRef, tz, equipDis))
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
            CcuLog.i(Domain.LOG_TAG, "updateEquipAndPoints deletePoint - ${point.domainName}")
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            hayStack.deleteEntity(existingPoint["id"].toString())
        }
    }

    private fun initializeDefaultVal(point : Point, defaultVal : Number) {
        CcuLog.i(Domain.LOG_TAG,"initializeDefaultVal ${point.domainName} - val $defaultVal")
        when{
            point.markers.contains("config") /*&& point.defaultVal is String*/-> hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
            point.markers.contains("tuner") -> TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
        }
    }

}