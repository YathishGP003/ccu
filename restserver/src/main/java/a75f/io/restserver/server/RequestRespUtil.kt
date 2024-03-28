package a75f.io.restserver.server

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.bacnet.BacnetConfigConstants
import org.json.JSONException
import org.json.JSONObject
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HRow
import org.projecthaystack.HVal

fun repackagePoints(tempGrid: HGrid, isVirtualZoneEnabled: Boolean, group: String): MutableList<HDict> {
    val mutableDictList = mutableListOf<HDict>()
    val gridIterator = tempGrid.iterator()
    while (gridIterator.hasNext()) {
        val row = gridIterator.next() as HRow
        val rowIterator = row.iterator()
        var hDictBuilder = HDictBuilder()
        var extractedDis = ""
        var extractedGroup = ""
        var extractedZoneRef = ""
        var bacnetId = ""
        var isEquip = false
        var extractedEquipRef = ""
        var isSystem = false
        while (rowIterator.hasNext()) {
            val e: HDict.MapEntry = (rowIterator.next() as HDict.MapEntry)
            when (e.value!!) {
                is String -> hDictBuilder.add(e.key.toString(), e.value as String)
                is Long -> hDictBuilder.add(e.key.toString(), e.value as Long)
                is Double -> hDictBuilder.add(e.key.toString(), e.value as Double)
                is Boolean -> hDictBuilder.add(e.key.toString(), e.value as Boolean)
                is HVal -> hDictBuilder.add(e.key.toString(), e.value as HVal)
                else -> hDictBuilder.add(e.key.toString(), e.value.toString())
            }
            if (e.key.toString() == "system") {
                isSystem = true
            }
            if (e.key.toString() == "equip") {
                isEquip = true
            }
            if (e.key.toString() == "dis") {
                extractedDis = e.value.toString()
            }
            if (e.key.toString() == "group") {
                extractedGroup = e.value.toString()
            }
            if (e.key.toString() == "roomRef") {
                extractedZoneRef = e.value.toString()
            }
            if (e.key.toString() == "bacnetId") {
                bacnetId = e.value.toString()
            }
            if (e.key.toString() == "equipRef") {
                extractedEquipRef = e.value.toString()
            }
        }
        val zoneName = CCUHsApi.getInstance()
            .readMapById(extractedZoneRef.replace("@", ""))["dis"].toString().trim()
        val pointDisName = extractedDis.split("-")
        val lastLiteralFromDis = pointDisName[pointDisName.size - 1]
        var profileName = ""
        if (pointDisName.size > 1) {
            profileName = pointDisName[1]
        }

        if (!isSystem) {
            if (isVirtualZoneEnabled) {
                if (isEquip) {
                    hDictBuilder.add("dis", "${zoneName}_${extractedGroup}")
                } else {
                    hDictBuilder.add("dis", lastLiteralFromDis)
                }


                try {
                    bacnetId = getBacNetId(bacnetId, group, extractedGroup, extractedEquipRef)
                    if (bacnetId != "0.0") {
                        hDictBuilder.add("bacnetId", bacnetId.toLong())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                if (zoneName.isEmpty() || zoneName == "null" || zoneName == "") {
                    hDictBuilder.add("dis", "${profileName}_$lastLiteralFromDis")
                } else {
                    hDictBuilder.add(
                        "dis",
                        "${zoneName}_${profileName}_${extractedGroup}_$lastLiteralFromDis"
                    )
                }
            }
        }else{
            hDictBuilder.add("dis", lastLiteralFromDis)
        }
        mutableDictList.add(hDictBuilder.toDict())
    }
    return mutableDictList
}

fun getBacNetId(
    bacnetId: String,
    group: String,
    extractedGroup: String,
    extractedEquipRef: String
): String {
    var tempId = ""
    if (group.isNotEmpty()) {
        tempId = bacnetId.replace(group, "").trim()
    } else if (extractedGroup.isNotEmpty()) {
        tempId = bacnetId.replace(extractedGroup, "").trim()
    } else if (extractedEquipRef.isNotEmpty()) {
        var tempExtractedEquipRef = extractedEquipRef.replace("@", "").trim()
        val groupFromEquipRef =
            CCUHsApi.getInstance().readMapById(tempExtractedEquipRef)["group"] as String
        tempId = bacnetId.replace(groupFromEquipRef, "").trim()
    }
    return tempId
}

fun getEquipRefId(input: String): String {
    val regex = "@[a-fA-F0-9\\-]+".toRegex()
    val matches = regex.findAll(input)
    for (match in matches) {
        return match.value
    }
    return ""
}

fun isVirtualZoneEnabled() : Boolean{
    var isVirtualZoneEnabled = false
    val confString: String? = HttpServer.sharedPreferences!!.getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
    if (confString != null) {
        try {
            isVirtualZoneEnabled = JSONObject(confString).getBoolean(BacnetConfigConstants.ZONE_TO_VIRTUAL_DEVICE_MAPPING)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    return isVirtualZoneEnabled
}