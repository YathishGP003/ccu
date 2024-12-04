package a75f.io.domain.cutover

import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog


fun getDomainNameFromDis(point : Map<Any, Any>, mapping : Map <String, String>) : String? {
    val displayNme = point["dis"].toString()
    return mapping.filterKeys { displayNme.replace("\\s".toRegex(),"").substringAfterLast("-") == it }
        .map { it.value }
        .firstOrNull()
}

fun getDeviceDomainNameFromDis(point : Map<Any, Any>, mapping : Map <String, String>) : String? {
    val displayNme = point["dis"].toString().substringBeforeLast("-")
    return mapping.filterKeys { displayNme.replace("\\s".toRegex(),"").substringAfterLast("-") == it }
        .map { it.value }
        .firstOrNull()
}

fun findDisFromDomainName(domainName : String, mapping : Map <String, String>) : String? {
    return mapping.filterValues { it.equals(domainName,true) }
        .map { it.key }
        .firstOrNull()
}

fun pointWithDomainNameExists(dbPoints : List<Map<Any, Any>>, domainName : String) : Boolean{
    return dbPoints.any { it["domainName"]?.toString().equals(domainName, true) }
}

fun devicePointWithDomainNameExists(dbPoints : List<Map<Any, Any>>, domainName : String, mapping : Map <String, String>) : Boolean{
    return dbPoints.any { dbPoint ->
        mapping.get(dbPoint["dis"].toString()
            .substringBeforeLast("-")
            .replace("\\s".toRegex(),"")
            .substringAfterLast("-")
        ).equals(domainName, ignoreCase = true) }
}

fun getDomainNameForMonitoringProfile(point: Map<Any, Any>): String? {
    val monitoringMappings = mapOf(
        Tags.ANALOG1 to HyperStatV2EquipCutoverMapping.getMonitoringAnalog1Entries(),
        Tags.ANALOG2 to HyperStatV2EquipCutoverMapping.getMonitoringAnalog2Entries(),
        Tags.TH1 to HyperStatV2EquipCutoverMapping.getMonitoringTh1Entries(),
        Tags.TH2 to HyperStatV2EquipCutoverMapping.getMonitoringTh2Entries()
    )

    val displayNameSuffix = point["dis"].toString().replace("\\s".toRegex(), "").substringAfterLast("-")

    monitoringMappings.forEach { (tag, entries) ->
        if (point.containsKey(tag) && point.containsKey(Tags.LOGICAL)) {
            CcuLog.i(
                Domain.LOG_TAG, "Found monitoring point with tag $tag and logical point for $displayNameSuffix")
            return entries[displayNameSuffix]
        }
    }

    CcuLog.i(Domain.LOG_TAG, "No Logical and monitoring point found for $displayNameSuffix")
    return null
}
