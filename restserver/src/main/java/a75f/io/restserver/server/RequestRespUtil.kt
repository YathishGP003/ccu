package a75f.io.restserver.server

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.util.bacnet.BacnetConfigConstants
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
        var isOaoProfile = false
        var isByPassDamper = false
        var isConnect = false
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
            if(e.key.toString() == "bypassDamper"){
                isByPassDamper = true
            }
	        if (e.key.toString() == "oao") {
                isOaoProfile = true
            }
            if (e.key.toString() == "connectModule") {
                isConnect = true
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
                .readMapById(extractedZoneRef.replace("@" , ""))["dis"].toString().trim()
            val pointDisName = extractedDis.split("-")
            val lastLiteralFromDis = pointDisName[pointDisName.size - 1]
            var profileName = ""
            if (pointDisName.size > 1) {
                profileName = pointDisName[1]
            }

        if (!isSystem || isOaoProfile || isByPassDamper) {
            createFormattedDisName(isVirtualZoneEnabled, isEquip, hDictBuilder, zoneName, extractedGroup,
                lastLiteralFromDis, bacnetId, extractedEquipRef, profileName)
        } else {
            if (isConnect) {
                hDictBuilder.add("dis" , "connect-$lastLiteralFromDis")
            } else {
                hDictBuilder.add("dis" , lastLiteralFromDis)
            }
        }
        if (isOaoProfile && isVirtualZoneEnabled && isEquip) {
            hDictBuilder.add("roomRef", "oao-fake-room-ref")
            hDictBuilder.add("dis", "${profileName}_${extractedGroup}")
        } else if (isByPassDamper && isVirtualZoneEnabled && isEquip) {
            hDictBuilder.add("roomRef", "by-pass-damper-fake-room-ref")
            hDictBuilder.add("dis", "${profileName}_${extractedGroup}")
        }
        mutableDictList.add(hDictBuilder.toDict())
    }
    return mutableDictList
}

private fun createFormattedDisName(isVirtualZoneEnabled: Boolean, isEquip : Boolean, hDictBuilder : HDictBuilder,
                                   zoneName: String, groupName : String, lastLiteralFromDis : String, bacnetId: String,
                                   extractedEquipRef: String, profileName : String){
    var extractedGroup = groupName
    if (isVirtualZoneEnabled) {
        if (isEquip) {
            hDictBuilder.add("dis" , "${zoneName}_${extractedGroup}")
        } else {
            hDictBuilder.add("dis" , lastLiteralFromDis)
        }


        try {
            if (bacnetId != "0.0") {
                if (!isEquip) {
                    val bacnetIdAfterModification = removeFirstFourChars(bacnetId)
                    hDictBuilder.add("bacnetId" , bacnetIdAfterModification.toLong())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    } else {
        if (extractedGroup.isEmpty()) {
            extractedGroup = getGroupFromEquipRef(extractedEquipRef)
        }
        if (zoneName.isEmpty() || zoneName == "null" || zoneName == "") {
            hDictBuilder.add(
                "dis" ,
                "${profileName}_${extractedGroup}_$lastLiteralFromDis"
            )
        } else {
            hDictBuilder.add(
                "dis" ,
                "${zoneName}_${profileName}_${extractedGroup}_$lastLiteralFromDis"
            )
        }
    }
}

fun removeFirstFourChars(input: String): String {
    if(input.length < 4){
        return input
    }
    return input.substring(4)
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
        tempId = bacnetId.replace(getGroupFromEquipRef(extractedEquipRef), "").trim()
    }
    return tempId
}

fun getGroupFromEquipRef(extractedEquipRef: String): String{
    if(extractedEquipRef.isEmpty()){
        return ""
    }
    var tempExtractedEquipRef = extractedEquipRef.replace("@", "").trim()
    return CCUHsApi.getInstance().readMapById(tempExtractedEquipRef)["group"] as String
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
