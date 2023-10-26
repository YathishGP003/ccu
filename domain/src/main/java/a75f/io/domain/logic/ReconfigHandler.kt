package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.logger.CcuLog

object ReconfigHandler {

    /**
     * Get map of all profile configurations and its current val from haystack database.
     * Map contains in domainName of config and rhw value in 8th level of point array.
     */
    private fun getAllConfig(equipRef : String, hayStack : CCUHsApi) : Map<String, Any> {
        CcuLog.i("DEV_DEBUG","Reading Existing configuration.. EquipRef = $equipRef")
        val domainNameMap = mutableMapOf<String, Double>()
        val configPoints = hayStack.readAllEntities("point and domainName and equipRef == \"$equipRef\"")
        configPoints.forEach {

            val pointVal = hayStack.readDefaultValById(it["id"].toString())
            //TODO - handle string type val if there is any.
            if (pointVal is Number) {
                domainNameMap[it["domainName"].toString()] = pointVal
            }
            if (pointVal is Double) {
                domainNameMap[it["domainName"].toString()] = pointVal
            }
        }
        domainNameMap.forEach { CcuLog.i("DEV_DEBUG","${it.key} : ${it.value}") }

        return domainNameMap
    }

    fun getEntityReconfiguration(equipRef: String, hayStack: CCUHsApi, config : EntityConfiguration) :
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
                newEntityConfig.tobeUpdated.add(it)
            } else {
                newEntityConfig.tobeAdded.add(it)
            }
        }
        return newEntityConfig;
    }
}