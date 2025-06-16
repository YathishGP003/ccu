package a75f.io.logic.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import org.projecthaystack.HDictBuilder
import java.util.ArrayList


private fun readDomainEntityList(domainName: String): ArrayList<java.util.HashMap<Any, Any>> {
    return CCUHsApi.getInstance().readAllEntities("domainName == \"$domainName\"")
}

fun updateDomain(oldDomainName: String, newDomainName: String) {
    val currentEntity = readDomainEntityList(oldDomainName)
    if (currentEntity.isNotEmpty()) {
        currentEntity.forEach {
            if (it.isNotEmpty()) {
                updateEntityWithNewDomain(it, newDomainName)
            }
        }
    } else {
        CcuLog.e(L.TAG_CCU_DOMAIN,"No points found for $oldDomainName")
    }
}


private fun updateEntityWithNewDomain(entityMap: HashMap<Any, Any>, newDomainName: String) {
    try {
        if (entityMap.containsKey(Tags.DOMAIN_NAME) && entityMap.containsKey(Tags.ID)) {
            val pointId = entityMap[Tags.ID].toString()
            val modifiedPoint =
                Point.Builder().setHDict(HDictBuilder().toHDict(entityMap)).setDomainName(newDomainName).build()
            CCUHsApi.getInstance().updatePoint(modifiedPoint, pointId)
            CcuLog.i(L.TAG_CCU_DOMAIN,"domainName migrated for $newDomainName = $pointId")
        }
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_DOMAIN,"error while migrating domainName ${e.message}", e)
    }
}