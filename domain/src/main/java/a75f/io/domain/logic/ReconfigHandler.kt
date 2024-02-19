package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.PointConfiguration

object ReconfigHandler {
    fun getEntityReconfiguration(equipRef: String, hayStack: CCUHsApi,
                                 config : EntityConfiguration, profileConfig: ProfileConfiguration, modelDef : SeventyFiveFProfileDirective) :
            EntityConfiguration {
        //val existingEntityMap = getAllConfig(equipGroup, hayStack)

        val existingEntityList = hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
            .map { it["domainName"].toString() }
        CcuLog.i(Domain.LOG_TAG, "Equip currently has ${existingEntityList.size} points")
        profileConfig.getEnableConfigs().forEach {
            CcuLog.i(Domain.LOG_TAG, "Enable config ${it.domainName} ${it.enabled}")
        }
        profileConfig.getValueConfigs().forEach {
            CcuLog.i(Domain.LOG_TAG, "Value config ${it.domainName} ${it.currentVal}")
        }
        profileConfig.getAssociationConfigs().forEach {
            CcuLog.i(Domain.LOG_TAG, "Association config ${it.domainName} ${it.associationVal}")
        }

        val newEntityConfig = EntityConfiguration()

        existingEntityList.forEach{ entityName ->
            if (config.tobeAdded.find { it.domainName == entityName} == null && !pointIsDynamicSensor(modelDef, entityName)) {
                newEntityConfig.tobeDeleted.add(EntityConfig(entityName))
            }
        }
        config.tobeAdded.forEach{
            if (existingEntityList.contains(it.domainName)) {
                if (isConfigPoint(it.domainName, profileConfig )) {
                    newEntityConfig.tobeUpdated.add(it)
                }
            } else {
                newEntityConfig.tobeAdded.add(it)
            }
        }
        return newEntityConfig;
    }

    private fun isConfigPoint (domainName : String, profileConfig: ProfileConfiguration) : Boolean {
        return (profileConfig.getEnableConfigs() + profileConfig.getValueConfigs())
            .find { it.domainName == domainName } != null ||
                (profileConfig.getAssociationConfigs())
                    .find { it.domainName == domainName } != null
    }

    private fun pointIsDynamicSensor(modelDef: SeventyFiveFProfileDirective, entityName: String) : Boolean {
        return modelDef.points.find {
            it.domainName.equals(entityName) && it.configuration.configType.equals(PointConfiguration.ConfigType.DYNAMIC_SENSOR)
        } != null
    }
}