package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.logger.CcuLog
import org.projecthaystack.UnknownNameException

object ReconfigHandler {

    /**
     * Get map of all profile configurations and its current val from haystack database.
     * Map contains in domainName of config and rhw value in 8th level of point array.
     */
    /*private fun getAllConfig(equipRef : String, hayStack : CCUHsApi) : Map<String, Any> {
        val domainNameMap = mutableMapOf<String, Double>()
        val configPoints =
            hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
        configPoints.forEach {
            CcuLog.i(Domain.LOG_TAG, "Config point $it")
                try {
                val pointVal = hayStack.readDefaultValById(it["id"].toString())
                //TODO - handle string type val if there is any.
                if (pointVal is Number) {
                    domainNameMap[it["domainName"].toString()] = pointVal
                }
            } catch (e : UnknownNameException) {
                CcuLog.i(Domain.LOG_TAG, "Invalid point in domainModel $it", e.cause)
            }

        }
        return domainNameMap
    }*/


    /*private fun getAllConfig(equipGroup : Int, hayStack : CCUHsApi) : List<String> {
        //val equip = hayStack.readEntity(
        //    "equip and group == \"$equipGroup\"")
        //return getAllConfig(equip["id"].toString(), hayStack)
        return hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
            .map { it -> it["domainName"].toString() }
    }*/

    fun getEntityReconfiguration(equipGroup: Int, hayStack: CCUHsApi, config : EntityConfiguration) :
                                                                        EntityConfiguration {
        //val existingEntityMap = getAllConfig(equipGroup, hayStack)

        val existingEntityList = hayStack.readAllEntities("point and group == \"$equipGroup\"")
                                .map { it["domainName"].toString() }
        CcuLog.i(Domain.LOG_TAG, "Equip currently has ${existingEntityList.size} points")
        val newEntityConfig = EntityConfiguration()

        existingEntityList.forEach{ entityName ->
            if (config.tobeAdded.find { it.domainName == entityName} == null) {
                newEntityConfig.tobeDeleted.add(EntityConfig(entityName))
            }
        }
        config.tobeAdded.forEach{
            if (existingEntityList.contains(it.domainName)) {
                newEntityConfig.tobeUpdated.add(it)
            } else {
                newEntityConfig.tobeAdded.add(it)
            }
        }
        return newEntityConfig;
    }
}