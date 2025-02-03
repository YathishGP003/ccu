package a75f.io.logic.util.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.bacnet.parser.AllowedValues
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.bacnet.parser.BacnetPoint
import a75f.io.api.haystack.bacnet.parser.BacnetProperty
import a75f.io.api.haystack.bacnet.parser.BacnetProtocolData
import a75f.io.api.haystack.bacnet.parser.PresentationData
import a75f.io.api.haystack.bacnet.parser.ProtocolData
import a75f.io.api.haystack.bacnet.parser.ValueConstraint
import org.projecthaystack.HStr


const val ID = "id"
const val VERSION = "version"

/**
 * @param zoneRef
 * returns EquipmentDevice list for a given zone ref
 */
fun buildBacnetModel(zoneRef: String): List<BacnetModelDetailResponse> {
    val equipListMap = getParentEquipMapByZone(zoneRef)
    val equips = mutableListOf<BacnetModelDetailResponse>()
    equipListMap.forEach { equipMap ->
        val equipDevice = buildEquipModel(equipMap, null)
        //equipDevice.equips.addAll(getChildEquips(equipMap[ID].toString()))
        equips.add(equipDevice)
    }
    return equips
}

/**
 * @param equipMap
 * @param parentEquipRef
 * returns the EquipmentDevice object with all the details
 */
private fun getEquipByMap(equipMap: HashMap<Any, Any>, parentEquipRef: String?): BacnetModelDetailResponse {
    val equipDevice = BacnetModelDetailResponse()
    val equip = Equip.Builder().setHashMap(equipMap).build()
    equipDevice.id = equip.id
    equipDevice.name = getModelName(equip.displayName, equip.group)
    equipDevice.modelType = "BACNET-DEFAULT"
    equipDevice.points = mutableListOf()
    equipDevice.bacnetConfig = equip.tags["bacnetConfig"].toString()
    equipDevice.modelConfig = equip.tags["modelConfig"].toString()
    return equipDevice
}

private fun buildEquipModel(parentMap: HashMap<Any, Any>, parentEquipRef: String?): BacnetModelDetailResponse {
    val equipId = parentMap[ID].toString()
    val equipDevice = getEquipByMap(parentMap, parentEquipRef)
    val registersMapList = getRegistersMap(equipId)
    registersMapList.forEach { registerMap ->
        equipDevice.points.add(getRegister(registerMap))
    }
    return equipDevice
}

/**
 * @param equipId
 * return all the register map list for the the equip
 */
private fun getRegistersMap(equipId: String): ArrayList<HashMap<Any, Any>> {
    val hsApi = CCUHsApi.getInstance()
    //val deviceId = hsApi.readId("device and bacnet and equipRef == \"$equipId\"")
    //return hsApi.readAllEntities("physical and point and deviceRef == \"$deviceId\"")
    return hsApi.readAllEntities("logical and point and equipRef == \"$equipId\"")
}

/**
 * @param rawMap
 * returns register object with all the register details
 */
private fun getRegister(rawMap: HashMap<Any, Any>): BacnetPoint {
    val physicalPoint = RawPoint.Builder().setHashMap(rawMap).build()
    var isDisplayInUiEnabled = false
    for (marker in physicalPoint.markers) {
        if (marker.equals("displayInUi")) {
            isDisplayInUiEnabled = true
            break
        }
    }

    var defaultWriteLevel = "8"
    if (physicalPoint.tags.contains("defaultWriteLevel")) {
        defaultWriteLevel = (physicalPoint.tags["defaultWriteLevel"] as HStr).`val`
    }

    var incrementStep = ""
    var valueConstraints: ValueConstraint? = null
    if (physicalPoint.tags.containsKey("incrementStep")) {
        incrementStep = (physicalPoint.tags["incrementStep"] as HStr).`val`
        val min : Int = physicalPoint.minVal.toDouble().toInt()
        val max : Int = physicalPoint.maxVal.toDouble().toInt()
        valueConstraints = ValueConstraint("NUMERIC", min, max, null)
    }else if(physicalPoint.enums != null && physicalPoint.enums.isNotEmpty()){
        //val enumValues = physicalPoint.tags["enum"]?.toString()?.split(",")
        val enumValues = physicalPoint.enums.toString().split(",")
        val allowedValues = mutableListOf<AllowedValues>()
        enumValues?.forEachIndexed { index, it ->
            allowedValues.add(AllowedValues(index, it, it))
        }
        valueConstraints = ValueConstraint("MULTI_STATE", null, null, allowedValues)
    }



    val presentationData = PresentationData(incrementStep)
    val bacnetProtocolData = BacnetProtocolData(physicalPoint.tags["bacnetType"].toString(), physicalPoint.tags["bacnetId"].toString().toInt(), null, isDisplayInUiEnabled, null)
    val bacnetProperty = mutableListOf<BacnetProperty>()
    val shortDisplayName = if (physicalPoint.shortDis != null) physicalPoint.shortDis else physicalPoint.displayName
    val unit = if(physicalPoint.unit == null) "" else physicalPoint.unit
    return BacnetPoint(
        physicalPoint.id,
        physicalPoint.displayName,
        "",
        physicalPoint.kind,
        valueConstraints,
        presentationData,
        "cov",
        ProtocolData(bacnetProtocolData),
        unit,
        "",
        physicalPoint.markers,
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        bacnetProperty,
        disName = shortDisplayName,
        defaultWriteLevel = defaultWriteLevel
    )
}

/**
 * @param zoneRef
 * Read parent equip details by zone id
 * return list of hashmaps for all the parent equips
 */
private fun getParentEquipMapByZone(zoneRef: String): ArrayList<HashMap<Any, Any>> {
    return CCUHsApi.getInstance()
        .readAllEntities("equip and bacnet and not equipRef and roomRef == \"$zoneRef\"")
}

/**
 * @param
 */
private fun getModelName(name: String,slaveId: String): String {

   var modelName: String = name
    if (name.contains("-")){
        modelName = name.replace(CCUHsApi.getInstance().site!!.displayName+"-","")
        modelName = modelName.replace("-$slaveId","")
    }
    return modelName
}