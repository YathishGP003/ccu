package a75f.io.restserver.server

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import org.json.JSONException
import org.json.JSONObject
import org.projecthaystack.*
import org.projecthaystack.io.HZincWriter
import kotlin.math.abs

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
        var bacnetObjectId = ""
        var isEquip = false
        var extractedEquipRef = ""
        var isSystem = false
        var isOaoProfile = false
        var isByPassDamper = false
        var isModbus = false
        var isEmr = false
        var isBtu = false
        var isConnect = false
        var isBacnetMstp = false
        var isExternal = false
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
            if(e.key.toString() == "modbus"){
                isModbus = true
            }
            if(e.key.toString() == "bypassDamper"){
                isByPassDamper = true
            }
	        if (e.key.toString() == "oao") {
                isOaoProfile = true
            }
            if (e.key.toString() == "emr") {
                isEmr = true
            }
            if (e.key.toString() == "btu") {
                isBtu = true
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
            if (e.key.toString() == "bacnetObjectId") {
                bacnetObjectId = e.value.toString()
            }
            if (e.key.toString() == "equipRef") {
                extractedEquipRef = e.value.toString()
            }
            if (e.key.toString() == "bacnetMstp") {
                isBacnetMstp = true
            }
            if (e.key.toString() == "external") {
                isExternal = true
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
                lastLiteralFromDis, bacnetId, extractedEquipRef, profileName,isBacnetMstp)
        } else {
            if (isConnect) {
                hDictBuilder.add("dis" , "connect-$lastLiteralFromDis")
            } else {
                hDictBuilder.add("dis" , lastLiteralFromDis)
            }
        }

        if(isExternal && !isEquip && isVirtualZoneEnabled){
            hDictBuilder.add("bacnetId" , bacnetObjectId.toLong())
        } else if(isModbus && !isEquip && isVirtualZoneEnabled){
            val bacnetIdAfterModification = removeLeadingZeros(removeFirstNumberofChars(bacnetId,4)) //removeGroup(extractedGroup, bacnetId)
            hDictBuilder.add("bacnetId" , bacnetIdAfterModification.toLong())
        }

        if (isOaoProfile && isVirtualZoneEnabled && isEquip) {
            hDictBuilder.add("roomRef", "oao-fake-room-ref")
            hDictBuilder.add("dis", "${profileName}_${extractedGroup}")
        } else if (isByPassDamper && isVirtualZoneEnabled && isEquip) {
            hDictBuilder.add("roomRef", "by-pass-damper-fake-room-ref")
            hDictBuilder.add("dis", "${profileName}_${extractedGroup}")
        } else if (isEmr && isSystem && isVirtualZoneEnabled && isEquip) {
            hDictBuilder.add("roomRef", "emr-fake-room-ref")
            hDictBuilder.add("dis", "${profileName}_${extractedGroup}")
        } else if (isBtu && isSystem && isVirtualZoneEnabled && isEquip) {
            hDictBuilder.add("roomRef", "btu-fake-room-ref")
            hDictBuilder.add("dis", "${profileName}_${extractedGroup}")
        }
        mutableDictList.add(hDictBuilder.toDict())
    }
    return mutableDictList
}

private fun removeLeadingZeros(formatted: String): String {
    return formatted.toInt().toString()
}

private fun createFormattedDisName(isVirtualZoneEnabled: Boolean, isEquip : Boolean, hDictBuilder : HDictBuilder,
                                   zoneName: String, groupName : String, lastLiteralFromDis : String, bacnetId: String,
                                   extractedEquipRef: String, profileName : String, isBacnetMstp: Boolean) {
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
                    val bacnetIdAfterModification = if (isBacnetMstp) removeFirstNumberofChars(bacnetId,3)
                                                       else removeFirstNumberofChars(bacnetId,4)
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

fun removeFirstNumberofChars(input: String,numberOfChar : Int = 4): String {
    if(input.length < numberOfChar){
        return input
    }
    return input.substring(numberOfChar)
}

fun removeGroup(group: String, bacnetId: String): String {
    try {
        val bacnetIdInt = abs(bacnetId.toInt())
        if(bacnetIdInt > 2000000){
            val band = 2000000 - bacnetIdInt
            val result = abs(band.toString().replaceFirst(group, "").toInt())
            return result.toString()
        }else{
            return abs(bacnetId.toInt()).toString()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return abs(bacnetId.toInt()).toString()
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

fun createRowWithMinValue(minVal : Double): String {
    val b = HGridBuilder()
    b.addCol("level")
    b.addCol("val")
    b.addCol("who")
    b.addCol("duration")
    b.addCol("lastModifiedDateTime")

    val level = 16
    b.addRow(arrayOf(
            HNum.make(level),
            HNum.make(minVal),
            HStr.make("ccu"),
            HNum.make("0.0".toDouble()),
            HDateTime.make(System.currentTimeMillis())
    ))

    return HZincWriter.gridToString(b.toGrid())
}
