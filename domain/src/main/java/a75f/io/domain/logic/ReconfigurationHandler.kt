package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.logger.CcuLog
import org.projecthaystack.HStr

object ReconfigurationHandler {

    /**
     * Get map of all profile configurations and its current val from haystack database.
     * Map contains in domainName of config and rhw value in 8th level of point array.
     */
    private fun getAllConfig(equipRef : String, hayStack : CCUHsApi) : Map<String, Any> {
        val domainNameMap = mutableMapOf<String, Double>()
        val configPoints = hayStack.readAllEntities("point and domainName and equipRef == \"$equipRef\"")
        configPoints.forEach {
            try {
                val kindVal = it["kind"] as HStr
                if (kindVal.toString().contentEquals("Number")) {
                    val pointVal = hayStack.readDefaultValById(it["id"].toString())
                    domainNameMap[it["domainName"].toString()] = pointVal
                } else  if (kindVal.toString().contentEquals("Str")) {
                    // TODO NEED TO HANDLE IF REQUIRED
                }

            } catch (e: Exception) {
                CcuLog.e("DEV_DEBUG","Error ${e.message}",e)
            }
        }
        return domainNameMap
    }

    fun getEntityReconfiguration(equipRef: String, hayStack: CCUHsApi, config : EntityConfiguration, profileConfig: ProfileConfiguration) :
                                                                        EntityConfiguration {
        val existingEntityMap = getAllConfig(equipRef, hayStack)
        val newEntityConfig = EntityConfiguration()
        existingEntityMap.keys.forEach{ entityName ->
            if (config.tobeAdded.find { it.domainName == entityName} == null) {
                newEntityConfig.tobeDeleted.add(EntityConfig(entityName))
            }
        }
        config.tobeAdded.forEach{
            if (existingEntityMap.keys.contains(it.domainName)) {
                if (hasPointUpdated(it.domainName, profileConfig)) {
                    newEntityConfig.tobeUpdated.add(it)
                }
            } else {
                newEntityConfig.tobeAdded.add(it)
            }
        }
        return newEntityConfig
    }

    private fun hasPointUpdated (domainName : String, profileConfig: ProfileConfiguration) : Boolean {
        val config =  profileConfig.getEnableConfigs()
        val valueConfig =  profileConfig.getValueConfigs()
        val associationConfigs =  profileConfig.getAssociationConfigs()
        val dependencies =  profileConfig.getDependencies()

        config.forEach {
            if (it.domainName == domainName)
                return true
        }
        valueConfig.forEach {
            if (it.domainName == domainName)
                return true
        }
        associationConfigs.forEach {
            if (it.domainName == domainName)
                return true
        }
        dependencies.forEach {
            if (it.domainName == domainName)
                return true
        }
        return false
    }
}