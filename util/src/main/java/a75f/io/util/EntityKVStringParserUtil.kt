package a75f.io.util

fun getConfig(configString: String): MutableMap<String, String> {
    val pairs: Array<String> =
        configString.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    val configMap: MutableMap<String, String> = java.util.HashMap()
    for (pair in pairs) {
        val keyValue = pair.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (keyValue.size != 2) continue  // Skip invalid key value pairs (e.g. "destinationIp:
        val key = keyValue[0]
        val value = keyValue[1]
        configMap[key] = value
    }
    return configMap
}

fun getEnumsMap(enumsString: String): MutableMap<String, Int> {
    val pairs: Array<String> =
        enumsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    val enumsMap: MutableMap<String, Int> = mutableMapOf()
    for (pair in pairs) {
        val keyValue = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (keyValue.size != 2) continue  // Skip invalid key value pairs (e.g. "1:On")
        val key = keyValue[0]
        val value = keyValue[1].toInt()
        enumsMap[key] = value
    }

    // Sort enumMap by value and update enumMap to reflect the new order
    val sortedEnumMap = enumsMap.entries
        .sortedBy { it.value }
        .associate { it.key to it.value }

    enumsMap.clear()
    enumsMap.putAll(sortedEnumMap)

    return enumsMap
}

fun getRangedValueList(minVal : Number, maxVal: Number, incrementVal: Number): List<String> {
    val rangedValueList = mutableListOf<String>()
    var currentValue = minVal.toDouble()
    while (currentValue <= maxVal.toDouble() && incrementVal.toDouble() > 0.0) {
        rangedValueList.add(getStringFormat(currentValue, incrementVal.toDouble()))
        currentValue += incrementVal.toDouble()
    }
    return rangedValueList
}

fun getStringFormat(itVal: Double, incVal: Double): String {
    var decimalPlaces = 0
    var i : Double = incVal
    while (i < 1) {
        i *= 10
        decimalPlaces += 1
    }
    val formattedString = ("%." + decimalPlaces.toString() + "f").format(itVal)
    return if (formattedString.toDouble() != 0.0) formattedString else ("%." + decimalPlaces.toString() + "f").format(0.0)
}