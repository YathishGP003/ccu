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
    fun getEntityReconfiguration(
        equipRef: String,
        hayStack: CCUHsApi,
        modelConfig: EntityConfiguration,
        profileConfig: ProfileConfiguration,
        modelDef: SeventyFiveFProfileDirective
    ): EntityConfiguration {

        val existingEntityList = hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
            .filter { it["domainName"] != null }.map { it["domainName"].toString() }
        CcuLog.i(Domain.LOG_TAG, "Reconfig -Equip currently has ${existingEntityList.size} points")

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

        existingEntityList.forEach { entityName ->
            if (modelConfig.tobeAdded.find { it.domainName == entityName } == null && !pointIsDynamicSensor( // it should not br present in the configuration
                    modelDef,
                    entityName
                )) {
                newEntityConfig.tobeDeleted.add(EntityConfig(entityName))
            }
        }
        modelConfig.tobeAdded.forEach {
            if (existingEntityList.contains(it.domainName)) {
                if (isConfigPoint(it.domainName, profileConfig)) {
                    newEntityConfig.tobeUpdated.add(it)
                }
            } else {
                newEntityConfig.tobeAdded.add(it)
            }
        }
        newEntityConfig.tobeAdded.forEach { CcuLog.i(Domain.LOG_TAG,"tobeAdded ${it.domainName}") }
        newEntityConfig.tobeUpdated.forEach { CcuLog.i(Domain.LOG_TAG,"tobeUpdated  ${it.domainName}") }
        newEntityConfig.tobeDeleted.forEach { CcuLog.i(Domain.LOG_TAG,"tobeDeleted ${it.domainName}") }
        return newEntityConfig
    }

    private fun isConfigPoint(domainName: String, profileConfig: ProfileConfiguration): Boolean {
        return (profileConfig.getEnableConfigs() + profileConfig.getValueConfigs()).find { it.domainName == domainName } != null || (profileConfig.getAssociationConfigs()).find { it.domainName == domainName } != null
    }

    private fun pointIsDynamicSensor(
        modelDef: SeventyFiveFProfileDirective,
        entityName: String
    ): Boolean {
        return modelDef.points.find {
            it.domainName == entityName && it.configuration.configType == PointConfiguration.ConfigType.DYNAMIC_SENSOR
        } != null
    }

}