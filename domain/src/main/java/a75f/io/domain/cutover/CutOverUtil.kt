package a75f.io.domain.cutover


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