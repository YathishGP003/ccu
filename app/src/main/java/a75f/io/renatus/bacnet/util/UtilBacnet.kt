package a75f.io.renatus.bacnet.util

import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.ConfigUtil.Companion.TAG
import a75f.io.logic.bo.building.system.BacNetConstants
import a75f.io.logic.bo.building.system.BacnetObjectRef
import a75f.io.logic.bo.building.system.client.MultiReadResponse
import a75f.io.renatus.bacnet.BacnetData
import a75f.io.renatus.bacnet.DeviceProp
import a75f.io.renatus.bacnet.Point

const val BACNET = "BACnet"
const val MODEL = "Model"
const val LOADING = "Loading Bacnet Models"
const val SLAVE_ID = "Slave Id"
const val SELECT_ALL = "Select All Parameters"
const val SET = "SET"
const val PARAMETER = "PARAMETER"
const val SAME_AS_PARENT = "Same As Parent"
const val DISPLAY_UI = "DISPLAY UI"
const val SAVING = "Saving Modbus configuration"
const val SAVED = "Saved all the configuration"
const val NO_MODEL_DATA_FOUND = "No model data found..!"
const val MODBUS_DEVICE_LIST_NOT_FOUND = "Modbus device list not found..!"
const val NO_INTERNET = "Unable to fetch equipments, please confirm your Wifi connectivity"
const val WARNING = "Warning"
const val OK = "Ok"
const val CONFIGURATION_TYPE = "Configuration Type"
const val MSTP_CONFIGURATION = "MSTP Configuration"
const val IP_CONFIGURATION = "IP Configuration"
const val MAC_ADDRESS = "Mac Address"
const val MAC_ADDRESS_INFO_SLAVE = "In between 128 to 254"
const val MAC_ADDRESS_INFO_MASTER = "In between 1 to 127"
const val SEARCH_MODEL = "Search model"
const val SEARCH_SLAVE_ID = "Search Slave Id"
const val SELECT_MODEL = "Select Model"
const val SELECTED_MODEL = "Selected Model"
const val SELECT_ADDRESS = "Select Address"
const val CONST_AUTO_DISCOVERY = "Initiating auto discovery… This may take a few mins"

fun objectListValueParser(valueList : String): MutableList<BacnetObjectRef>{
    val result = mutableListOf<BacnetObjectRef>()
    CcuLog.d(TAG, "Raw object-list value -> $valueList")
    val entries = valueList.removePrefix("[").removeSuffix("]").split("}, {")
    entries.forEach { entry ->
        val cleanEntry = entry.replace("{", "").replace("}", "")
        val parts = cleanEntry.split(", ")
        if (parts.size == 2) {
            val objectTypeCode = parts[0].substringAfter("=")
            val objectInstance = parts[1].substringAfter("=")

            val objects = BacnetObjectRef(
                objectTypeCode,
                objectInstance
            )

            result.add(objects)
            CcuLog.d(TAG, "Parsed object-list entry -> $objects")
        }
    }
    return result
}

fun convertToSmartMapData(multiReadResponse: MultiReadResponse): BacnetData {
    val deviceProps = mutableListOf<DeviceProp>()
    val points = mutableListOf<Point>()

    val items = multiReadResponse.rpResponse.listOfItems ?: emptyList()

    for (item in items) {
        val identifier = item.objectIdentifier
        for (res in item.results) {
            val singleResultList = mutableListOf(res)

            if (identifier.objectType == BacNetConstants.ObjectType.OBJECT_DEVICE.value.toString()) {
                deviceProps.add(DeviceProp(object_identifier = identifier, results = singleResultList))
            }
            else if(identifier.objectType == BacNetConstants.ObjectType.OBJECT_NETWORK_PORT.value.toString()){
                continue
            } else {
                points.add(Point(object_identifier = identifier, results = singleResultList))
            }
        }
    }

    return BacnetData(deviceProps = deviceProps, points = points)
}