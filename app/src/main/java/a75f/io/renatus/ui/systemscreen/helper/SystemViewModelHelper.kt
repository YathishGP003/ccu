package a75f.io.renatus.ui.systemscreen.helper

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.operatingMode
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.getExternalEquipId
import a75f.io.logic.bo.building.system.getPointByDomain
import a75f.io.logic.bo.building.system.logIt
import a75f.io.logic.tuners.TunerUtil
import a75f.io.renatus.ui.zonescreen.MAX_INSIDE_HUMIDITY
import a75f.io.renatus.ui.zonescreen.MIN_INSIDE_HUMIDITY
import a75f.io.renatus.ui.zonescreen.model.DetailedViewItem


fun getMinInsideHumidityView(isDefaultSystemProfile: Boolean): DetailedViewItem {
    if (isDefaultSystemProfile) {
        return DetailedViewItem(
            id = "defaultMinHumidity",
            disName = MIN_INSIDE_HUMIDITY,
            currentValue = 0.toString(),
            selectedIndex = 0,
            dropdownOptions = arrayListOf("0%"),
            usesDropdown = true,
            point = null,
            configuration = null,
            displayOrder = 1,
            shouldTakeFullRow = false
        )
    }
    val minHumidifierPoint = CCUHsApi.getInstance()
        .readEntity("domainName == \"" + DomainName.systemtargetMinInsideHumidity + "\"")
    val minHumidifierValue =
        TunerUtil.readSystemUserIntentVal("domainName == \"" + DomainName.systemtargetMinInsideHumidity + "\"")

    val arrayMinHumidityTargetList = ArrayList<String>()
    for (pos in 0..100) arrayMinHumidityTargetList.add("$pos%")
    return DetailedViewItem(
        id = minHumidifierPoint["id"].toString(),
        disName = MIN_INSIDE_HUMIDITY,
        currentValue = minHumidifierValue.toString(),
        selectedIndex = minHumidifierValue.toInt(),
        dropdownOptions = arrayMinHumidityTargetList,
        usesDropdown = true,
        point = null,
        configuration = null,
        displayOrder = 1,
        shouldTakeFullRow = false
    )
}

fun getMaxInsideHumidityView(isDefaultSystemProfile: Boolean): DetailedViewItem {
    if (isDefaultSystemProfile) {
        return DetailedViewItem(
            id = "defaultMaxHumidity",
            disName = MAX_INSIDE_HUMIDITY,
            currentValue = 0.toString(),
            selectedIndex = 0,
            dropdownOptions = arrayListOf("0%"),
            usesDropdown = true,
            point = null,
            configuration = null,
            displayOrder = 2,
            shouldTakeFullRow = false
        )
    }
    val maxHumidifierPoint = CCUHsApi.getInstance()
        .readEntity("domainName == \"" + DomainName.systemtargetMaxInsideHumidity + "\"")
    val maxHumidifierValue =
        TunerUtil.readSystemUserIntentVal("domainName == \"" + DomainName.systemtargetMaxInsideHumidity + "\"")

    val arrayMaxHumidityTargetList = ArrayList<String>()
    for (pos in 0..100) arrayMaxHumidityTargetList.add("$pos%")
    return DetailedViewItem(
        id = maxHumidifierPoint["id"].toString(),
        disName = MAX_INSIDE_HUMIDITY,
        currentValue = maxHumidifierValue.toString(),
        selectedIndex = maxHumidifierValue.toInt(),
        dropdownOptions = arrayMaxHumidityTargetList,
        usesDropdown = true,
        point = null,
        configuration = null,
        displayOrder = 2,
        shouldTakeFullRow = false
    )
}


fun getSetPointByDomainName(domainName: String): Pair<String, String> {
    val point = Domain.readPoint(domainName)
    if (point.isEmpty()) return Pair("", "")

    val id = point["id"]?.toString().orEmpty()
    val unit = point["unit"]?.toString().orEmpty()

    if (id.isEmpty()) return Pair("", "")

    val value = CCUHsApi.getInstance().readHisValById(id)?.toString().orEmpty()
    val formattedValue = "$value $unit".trim()

    return Pair(id, formattedValue)
}

fun getModbusPointValueByQuery(query: String): Pair<String, String> {
    val equipId = getExternalEquipId()
    val point = CCUHsApi.getInstance().readEntity("$query and equipRef == \"$equipId\"")

    if (point.isEmpty()) {
        logIt("$query = point not found for equipId: $equipId")
        return Pair("", "")
    }

    val pointId = point["id"]?.toString().orEmpty()
    val unit = point["unit"]?.toString().orEmpty()

    if (pointId.isEmpty()) {
        logIt("$query = pointId missing for equipId: $equipId")
        return Pair("", "")
    }

    val value = CCUHsApi.getInstance().readHisValById(pointId)?.toString().orEmpty()
    val formattedValue = "$value $unit".trim()

    return Pair(pointId, formattedValue)
}

fun getSystemOperatingMode(equipName: String): Pair<String, String> {
    val systemEquip = Domain.getSystemEquipByDomainName(equipName)
    val configPoint = getPointByDomain(systemEquip!!, operatingMode)
    val mode = configPoint?.readHisVal()?.toInt() ?: 0
    val value = when (SystemController.State.values()[mode]) {
        SystemController.State.COOLING -> " Cooling"
        SystemController.State.HEATING -> " Heating"
        else -> {
            " Off"
        }
    }
    return Pair(configPoint!!.id, value)
}
