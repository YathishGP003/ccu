package a75f.io.renatus.ui.nontempprofiles.utilities

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HayStackConstants.FORCE_OVERRIDE_LEVEL
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem
import a75f.io.api.haystack.modbus.Command
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.api.haystack.observer.HisWriteObservable
import a75f.io.device.connect.ConnectModbusSerialComm.writeToConnectNode
import a75f.io.device.modbus.LModbus
import a75f.io.device.modbus.buildModbusModelByEquipRef
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.bacnet.BacnetEquip.TAG_BACNET
import a75f.io.logic.bo.building.system.BacnetServicesUtils
import a75f.io.logic.bo.building.system.BacnetWriteRequest
import a75f.io.logic.bo.building.system.DestinationMultiRead
import a75f.io.logic.bo.building.system.ObjectIdentifierBacNet
import a75f.io.logic.bo.building.system.PropertyValueBacNet
import a75f.io.logic.bo.building.system.WriteRequest
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.bo.util.SCHEDULE_STATUS
import a75f.io.logic.bo.util.isPointFollowingScheduleOrEvent
import a75f.io.logic.util.bacnet.ObjectType
import a75f.io.logic.util.bacnet.isValidMstpMacAddress
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import a75f.io.renatus.util.HeartBeatUtil
import android.util.Pair
import java.util.Date
import java.util.Objects
import java.util.stream.Collectors


fun getSortedParameterList(modbusDevice: EquipmentDevice): List<Parameter> {
    return fetchAllExternalParameterPoints(modbusDevice)
        .stream()
        .sorted(Comparator.comparing { param ->
            param.getLogicalPointTags()
                .stream()
                .filter { tag -> tag.tagName == "pointNum" }
                .map { tag -> tag.tagValue.toInt() }
                .findFirst()
                .orElse(Int.MAX_VALUE)
        })
        .collect(Collectors.toList())
}

private fun fetchAllExternalParameterPoints(equipmentDevice: EquipmentDevice): List<Parameter> {
    val parameterList = mutableListOf<Parameter>()

    getParametersList(equipmentDevice).forEach { parameter ->
        if (parameter.isDisplayInUI) {
            parameterList.add(parameter)
        }
    }

    return parameterList
}


fun readPoint(
    configParams: Parameter,
    equipRef: String,
    isConnectNodeView: Boolean
): Point? {
    if (isConnectNodeView) {
        val logicalPoint = CCUHsApi.getInstance().readHDict(
            "point " +
                    " and registerType == \"" + configParams.registerType + "\"" +
                    " and registerAddress == \"" + configParams.registerAddress + "\"" +
                    " and equipRef == \"" + equipRef + "\""
        )

        if (logicalPoint == null || logicalPoint.isEmpty) {
            return null
        }
        return Point.Builder().setHDict(logicalPoint).build()
    }

    var deviceRef: String? = null
    val devicePoint = CCUHsApi.getInstance().readEntity(
        "device and equipRef == \"$equipRef\""
    )
    if (devicePoint.isNotEmpty()) {
        deviceRef = devicePoint["id"].toString()
    }

    val phyPoint = CCUHsApi.getInstance().readHDict(
        "point and physical " +
                " and registerType == \"" + configParams.registerType + "\"" +
                " and registerAddress == \"" + configParams.registerAddress + "\"" +
                " and parameterId == \"" + configParams.getParameterId() + "\"" +
                " and deviceRef == \"" + deviceRef + "\""
    )

    if (phyPoint["pointRef", false] == null || phyPoint["pointRef"].toString() == "") {
        CcuLog.d(
            L.TAG_CCU_MODBUS, ("Physical point does not exist for register "
                    + configParams.registerAddress + " and device " + deviceRef)
        )
        return null
    }
    val logPoint = CCUHsApi.getInstance().readHDict("point and id == " + phyPoint["pointRef"])

    if (logPoint == null || logPoint.isEmpty) {
        return null
    }
    return Point.Builder().setHDict(logPoint).build()
}

fun getUserIntentsCommandMap(parameter: Parameter): HashMap<String, List<Command>> {
    val userIntentsMap = HashMap<String, List<Command>>()
    val userIntentPointTags = parameter.getUserIntentPointTags()
    var unit = ""
    for (tags in userIntentPointTags) {
        if (tags.tagName == "unit") {
            unit = tags.tagName
        }
    }

    userIntentsMap[unit] = parameter.getCommands()

    return userIntentsMap
}

fun readVal(id: String?): Double {
    val hayStack = CCUHsApi.getInstance()
    val values: ArrayList<*>? = hayStack.readPoint(id)
    if (values != null && values.size > 0) {
        for (l in 1..values.size) {
            val valMap = (values[l - 1] as java.util.HashMap<*, *>)
            if (valMap["val"] != null) {
                return valMap["val"].toString().toDouble()
            }
        }
    }
    return 0.0
}

fun getUserIntentsDoubleMap(parameter: Parameter): java.util.HashMap<String, java.util.ArrayList<Double>> {
    val userIntentsMap = java.util.HashMap<String, java.util.ArrayList<Double>>()
    val userIntentPointTags = parameter.getUserIntentPointTags()
    val doubleArrayList = java.util.ArrayList<Double>()
    var minValue = 0.0
    var maxValue = 0.0
    var incValue = 0.0
    var unit = ""

    for (i in userIntentPointTags.indices) {
        val tagName = userIntentPointTags[i].tagName
        val tagValue = userIntentPointTags[i].tagValue
        when (tagName) {
            "minVal" -> minValue = tagValue.toDouble()
            "maxVal" -> maxValue = tagValue.toDouble()
            "incrementVal" -> incValue = tagValue.toDouble()
            "unit" -> unit = tagValue
        }
    }
    if (maxValue <= minValue || incValue <= 0) {
        doubleArrayList.add(minValue)
        userIntentsMap[unit] = doubleArrayList
        return userIntentsMap
    }
    var pos = (100 * minValue).toInt()
    while (pos <= (100 * maxValue)) {
        doubleArrayList.add(pos / 100.0)
        pos = (pos + (100 * incValue)).toInt()
    }
    userIntentsMap[unit] = doubleArrayList

    return userIntentsMap
}

fun readHisVal(id: String?): Double {
    val hayStack = CCUHsApi.getInstance()
    return hayStack.readHisValById(id)
}


fun writePoint(point: Point, value: String, parameter: Parameter, isConnectNodeView: Boolean) {
    val equipHashMap = CCUHsApi.getInstance().readMapById(point.equipRef)
    val equip = Equip.Builder().setHashMap(equipHashMap).build()
    if (!isPointFollowingScheduleOrEvent(point.id)) {
        CCUHsApi.getInstance().writePoint(point.id, value.toDouble())
        if (point.markers.contains("his")) {
            CCUHsApi.getInstance().writeHisValById(point.id, value.toDouble())
        }
    }
    val highestPriorityValue = CCUHsApi.getInstance().readPointPriorityVal(point.id)
    val modbusSubEquipList: MutableList<EquipmentDevice> = java.util.ArrayList()

    if (isConnectNodeView) {
        writeToConnectNode(
            point.group.toInt(),
            parameter.registerNumber.toInt(), highestPriorityValue
        )
        return
    }
    if (equip.equipRef != null) {
        val parentEquip = buildModbusModelByEquipRef(equip.equipRef)
        if (parentEquip.equips.isNotEmpty()) {
            modbusSubEquipList.addAll(parentEquip.equips)
        }
    } else {
        modbusSubEquipList.add(buildModbusModelByEquipRef(equip.id))
    }
    for (modbusDevice in modbusSubEquipList) {
        for (register in modbusDevice.registers) {
            for (pam in register.parameters) {
                if (pam.getUserIntentPointTags() != null) {
                    if (pam.getName() == parameter.getName()) {
                        // if it connect node view, then write only in int
                        if (register.parameterDefinitionType != null && register.parameterDefinitionType == "float") {
                            LModbus.writeRegister(
                                point.group.toShort().toInt(),
                                register,
                                highestPriorityValue.toFloat()
                            )
                        } else {
                            LModbus.writeRegister(
                                point.group.toShort().toInt(),
                                register,
                                highestPriorityValue.toInt()
                            )
                        }
                        break
                    }
                }
            }
        }
    }
}

fun getSpinnerValues(spinnerValues: List<Pair<String, Int>>): List<String> {
    val values: MutableList<String> = java.util.ArrayList()
    for (pair in spinnerValues) {
        values.add(pair.first)
    }
    return values
}

fun searchIndexValue(enumString: String, inputValue: String): Int {
    CcuLog.d(TAG_BACNET, "---------searchIndexValue-------$enumString<--inputValue-->$inputValue")
    return try {
        val parts = enumString.split(",")
        for ((index, rawPart) in parts.withIndex()) {
            val part = rawPart.trim() // remove leading/trailing spaces
            val keyValue = part.split("=")
            if (keyValue.size == 2 && keyValue[1].trim() == inputValue) {
                return index
            }
        }
        -1
    } catch (e: Exception) {
        e.printStackTrace()
        -1
    }
}

fun findItemPosition(numberStrings: List<Pair<String, Int>>, targetValue: Double): Int {
    var index = 0

    for (i in numberStrings.indices) {
        val str = numberStrings[i].first
        val value = str.toDouble()
        if (value == targetValue) {
            index = i
            break
        }
    }
    return index
}

fun searchKeyForValue(
    enumString: String,
    inputValue: String
): String? {
    CcuLog.d(
        TAG_BACNET,
        ("---------searchRealValueForOperatingMode------one----" + enumString
                + "<--inputValue-->" + inputValue)
    )
    try {
        val reverseMapOne = getStringReverseMap(enumString)
        return reverseMapOne[inputValue]
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return inputValue
}

private fun getStringReverseMap(enumString: String): Map<String, String> {
    val mapOne = HashMap<String, String>()
    for (part in enumString.split(",")) {
        val keyValue = part.split("=")
        if (keyValue.size == 2) {
            mapOne[keyValue[0]] = keyValue[1]
        }
    }
    val reverseMapOne = HashMap<String, String>()
    for ((key, value) in mapOne) {
        reverseMapOne[value] = key
    }
    return reverseMapOne
}


const val DEVICE_ID = "deviceId"
const val DESTINATION_IP = "destinationIp"
const val DESTINATION_PORT = "destinationPort"
const val MAC_ADDRESS = "macAddress"
const val DEVICE_NETWORK = "deviceNetwork"

fun writeValueToBacnet(
    bacnetZoneViewItem: BacnetZoneViewItem,
    selectedValue: String,
    serverIpAddress: String,
    remotePointUpdateInterface: RemotePointUpdateInterface
) {
    val pairs = bacnetZoneViewItem.bacnetConfig.split(",")
    val configMap = HashMap<String, String>()
    for (pair in pairs) {
        val keyValue = pair.split(":")
        if (keyValue.size != 2) continue // Skip invalid key value pairs (e.g. "destinationIp:")
        val key = keyValue[0]
        val value = keyValue[1]
        configMap[key] = value
    }

    /*if (configMap[DESTINATION_IP] == null || configMap[DESTINATION_PORT] == null ||
        configMap[DEVICE_ID] == null || configMap[MAC_ADDRESS] == null) {
        CcuLog.e(TAG, "writeValue: Invalid config map")
        Toast.makeText(context, "Invalid configuration please check ip, port, deviceId and mac address", Toast.LENGTH_SHORT).show()
        return
    }*/

    val pointId = bacnetZoneViewItem.bacnetObj.id
    var level = bacnetZoneViewItem.bacnetObj.defaultWriteLevel
    if (isPointFollowingScheduleOrEvent(pointId)) {
        level = "" + FORCE_OVERRIDE_LEVEL
    }

    val objectId = CCUHsApi.getInstance()
        .readMapById(pointId)[Tags.BACNET_OBJECT_ID]
        .toString()
        .toDouble()
        .toInt()

    val objectType = bacnetZoneViewItem.objectType
    val isMSTPEquip = isValidMstpMacAddress(
        Objects.requireNonNull(configMap.getOrDefault(MAC_ADDRESS, ""))
    )

    val bacnetServicesUtils = BacnetServicesUtils()

    if (bacnetZoneViewItem.bacnetObj.isSystem) {
        CcuLog.d(TAG_BACNET, "--this is a system point, objectId--$objectId")
        bacnetServicesUtils.sendWriteRequest(
            generateWriteObject(configMap, objectId, selectedValue, objectType, level, isMSTPEquip),
            serverIpAddress,
            remotePointUpdateInterface,
            selectedValue,
            bacnetZoneViewItem.bacnetObj.id,
            isMSTPEquip
        )
    } else {
        CcuLog.d(TAG_BACNET, "--this is a normal bacnet client point with level: $level")
        bacnetServicesUtils.sendWriteRequest(
            generateWriteObject(configMap, objectId, selectedValue, objectType, level, isMSTPEquip),
            serverIpAddress,
            remotePointUpdateInterface,
            selectedValue,
            bacnetZoneViewItem.bacnetObj.id,
            isMSTPEquip
        )
    }
}

private fun generateWriteObject(
    configMap: Map<String, String>,
    objectId: Int,
    selectedValue: String,
    objectType: String,
    priority: String,
    isMSTPEquip: Boolean
): BacnetWriteRequest {

    var macAddress = ""
    if (configMap[MAC_ADDRESS] != null) {
        macAddress = configMap[MAC_ADDRESS]!!
    }

    val destinationMultiRead = DestinationMultiRead(
        Objects.requireNonNull(configMap.getOrDefault(DESTINATION_IP, "")),
        Objects.requireNonNull(configMap.getOrDefault(DESTINATION_PORT, "0")),
        Objects.requireNonNull(configMap.getOrDefault(DEVICE_ID, "0")),
        Objects.requireNonNull(configMap.getOrDefault(DEVICE_NETWORK, "0")),
        macAddress
    )

    val dataType: Int
    val selectedValueAsPerType: String

    when (BacNetConstants.ObjectType.valueOf(objectType).value) {
        ObjectType.OBJECT_ANALOG_VALUE.value, ObjectType.OBJECT_ANALOG_INPUT.value, ObjectType.OBJECT_ANALOG_OUTPUT.value -> {
            dataType = BacNetConstants.DataTypes.BACNET_DT_REAL.ordinal + 1
            selectedValueAsPerType = selectedValue
        }
        ObjectType.OBJECT_BINARY_VALUE.value, ObjectType.OBJECT_BINARY_INPUT.value, ObjectType.OBJECT_BINARY_OUTPUT.value -> {
            dataType = BacNetConstants.DataTypes.BACNET_DT_ENUM.ordinal + 1
            selectedValueAsPerType = selectedValue
        }
        else -> {
            dataType = if (isMSTPEquip) {
                BacNetConstants.DataTypes.BACNET_DT_UNSIGNED32.ordinal + 1
            } else {
                BacNetConstants.DataTypes.BACNET_DT_UNSIGNED.ordinal + 1
            }
            selectedValueAsPerType = Integer.parseInt(selectedValue).toString()
        }
    }

    val objectIdentifierBacNet = ObjectIdentifierBacNet(
        BacNetConstants.ObjectType.valueOf(objectType).value,
        objectId.toString()
    )

    val propertyValueBacNet = PropertyValueBacNet(dataType, selectedValueAsPerType)

    val writeRequest = WriteRequest(
        objectIdentifierBacNet,
        propertyValueBacNet,
        priority,
        BacNetConstants.PropertyType.valueOf("PROP_PRESENT_VALUE").value,
        null
    )

    return BacnetWriteRequest(destinationMultiRead, writeRequest)
}

fun getIndexOf(value: String, options: List<String>): Int {
    val valueAsDouble = value.toDouble()
    if (isNumericOptions(options)) {
        val doubleMatch = options.indexOfFirst { it.toDoubleOrNull() == valueAsDouble }
        if (doubleMatch >= 0) return doubleMatch

        if (valueAsDouble % 1.0 == 0.0) {
            val intMatch = options.indexOf(valueAsDouble.toInt().toString())
            if (intMatch >= 0) return intMatch
        }
    } else {
        var index = value.toDouble().toInt()
        if (index !in options.indices) index = 0
        return index
    }
    return 0
}

fun isNumericOptions(options: List<String>): Boolean {
    return options.firstOrNull()?.toDoubleOrNull() != null
}

fun getPointScheduleHeaderViewItem(equipRef: String): HeaderViewItem {

    val equipScheduleStatusPoint = CCUHsApi.getInstance().readEntity(
        "$SCHEDULE_STATUS and ${Tags.EQUIPREF} == \"$equipRef\""
    )

    val equipScheduleStatusString = CCUHsApi.getInstance()
        .readDefaultStrValById(equipScheduleStatusPoint["id"].toString())

    val pointScheduleStatusString = equipScheduleStatusString
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

    return HeaderViewItem(
            id = equipScheduleStatusPoint["id"].toString(),
            disName = "Equip Schedule Status: ",
            currentValue = pointScheduleStatusString
        )
}



fun getLastUpdatedViewItem(
    slaveId: String,
    subscriber: NonTempProfileViewModel
): HeaderViewItem {
    var lastUpdatedDateTime = getLastUpdatedTime(slaveId, subscriber)
    if (lastUpdatedDateTime?.second == null) {
        lastUpdatedDateTime = Pair(
            lastUpdatedDateTime?.first,
            "--"
        )
    }
    return HeaderViewItem(
        id = lastUpdatedDateTime.first ?: "",
        disName = "Last Updated:",
        currentValue = lastUpdatedDateTime.second
    )
}

fun getLastUpdatedTime(
    nodeAddress: String,
    subscriber: NonTempProfileViewModel
): Pair<String, String>? {
    val lastUpdatedObject: Pair<String, Date>? = when {
        nodeAddress.equals(Tags.CLOUD, ignoreCase = true) ->
            getLastReceivedTimeForCloudConnectivity(subscriber)

        nodeAddress.length < 4 ->
            getLastReceivedTimeForModBusAndBacnet(nodeAddress, subscriber)

        else ->
            getLastReceivedTimeForRssi(nodeAddress, subscriber)
    }

    if (lastUpdatedObject == null) return null

    val message = StringBuffer()
    val currTime = Date()

    return if (lastUpdatedObject.second == null) {
        Pair(
            lastUpdatedObject.first, null
        )
    } else if (currTime.date == lastUpdatedObject.second.date) {
        Pair(
            lastUpdatedObject.first,
            HeartBeatUtil.getTimeDifference(currTime, lastUpdatedObject.second, message)
        )
    } else {
        Pair(
            lastUpdatedObject.first,
            HeartBeatUtil.getLastUpdatedTime(message, lastUpdatedObject.second)
        )
    }
}

fun getLastReceivedTimeForRssi(nodeAddr: String,
                               subscriber: NonTempProfileViewModel) : Pair<String, Date>?  {
    if (CCUUtils.isDomainEquip(nodeAddr, "node")) {
        val point = CCUHsApi.getInstance()
            .readEntity("domainName == \"" + DomainName.heartBeat + "\"" +
                    " and group == \"" + nodeAddr + "\"")
        val pointId = point["id"].toString()
        val hisItem = CCUHsApi.getInstance().curRead(pointId)
        HisWriteObservable.subscribe(pointId, subscriber)
        return if (hisItem == null) {
            Pair(pointId, null)
        } else {
            Pair(pointId, hisItem.date)
        }
    }
    val hayStack = CCUHsApi.getInstance()
    val point =
        CCUHsApi.getInstance().readEntity("point and (heartBeat or heartbeat)" +
                " and group == \"$nodeAddr\"")
    if (point.size == 0) {
        return null
    }
    val pointId = point["id"].toString()
    val hisItem = hayStack.curRead(pointId)
    HisWriteObservable.subscribe(pointId, subscriber)
    return if (hisItem == null) {
        Pair(pointId, null)
    } else {
        Pair(pointId, hisItem.date)
    }
}

fun getLastReceivedTimeForModBusAndBacnet(slaveId: String,
                                          subscriber: NonTempProfileViewModel): Pair<String, Date>?  {
    val hayStack = CCUHsApi.getInstance()
    // Check if the slaveId is a valid Modbus slave ID (1-256) or Bacnet device ID (500-999)
    if (slaveId.isNotEmpty() && slaveId.toInt() <= 256) {
        val equipList: List<java.util.HashMap<Any?, Any>> =
            hayStack.readAllEntities("equip and modbus and group == \"$slaveId\"")
        if (equipList.isEmpty()) {
            return null
        }
        for (equip in equipList) {
            if (CCUUtils.isModbusHeartbeatRequired(equip, hayStack)) {
                val heartBeatPoint =
                    hayStack.readEntity("point and (heartbeat or heartBeat)" +
                            " and equipRef == \"" + equip["id"] + "\"")
                if (heartBeatPoint.size > 0) {
                    val pointId = heartBeatPoint["id"].toString()
                    val heartBeatHisItem = hayStack.curRead(pointId)
                    return if (heartBeatHisItem == null) {
                        Pair(pointId, null)
                    } else {
                        Pair(pointId, heartBeatHisItem.date)
                    }
                }
            }
        }
    } else if (slaveId.isNotEmpty() && slaveId.toInt() >= 500 && slaveId.toInt() <= 999) {
        val equip = hayStack.readEntity("equip and bacnet and group == \"$slaveId\"")
        if (equip.isEmpty()) {
            return null
        }

        val heartBeatPoint =
            hayStack.readEntity("point and (heartbeat or heartBeat)" +
                    " and equipRef == \"" + equip["id"] + "\"")
        if (heartBeatPoint.isNotEmpty()) {
            val pointId = heartBeatPoint["id"].toString()
            val heartBeatHisItem = hayStack.curRead(pointId)
            HisWriteObservable.subscribe(pointId, subscriber)
            return if (heartBeatHisItem == null) {
                Pair(pointId, null)
            } else {
                Pair(pointId, heartBeatHisItem.date)
            }
        }
    }
    return null
}

fun getLastReceivedTimeForCloudConnectivity(subscriber: NonTempProfileViewModel): Pair<String, Date>? {
    val hayStack = CCUHsApi.getInstance()
    val cloudConnectivityPoint: Map<Any, Any> =
        hayStack.readEntityByDomainName(DomainName.ccuHeartbeat)
    if (cloudConnectivityPoint.isEmpty()) {
        return null
    }
    val connectivityPointId = cloudConnectivityPoint["id"].toString()
    val heartBeatHisItem = hayStack.curRead(connectivityPointId)
    HisWriteObservable.subscribe(connectivityPointId, subscriber)
    return if (heartBeatHisItem == null) {
        Pair(connectivityPointId, null)
    } else {
        Pair(connectivityPointId, heartBeatHisItem.date)
    }
}

fun heartBeatStatus(
    nodeAddress: String
): Boolean {
    return HeartBeatUtil.isModuleAlive(nodeAddress)
}


fun stopObservingAllEquipHealth(nonTempProfileViewModels: List<NonTempProfileViewModel>) {
    for (nonTempProfileViewModel in nonTempProfileViewModels) {
        nonTempProfileViewModel.stopObservingEquipHealth()
    }
}

fun cleanUpObservableList(nonTempProfileViewModels: List<NonTempProfileViewModel>) {
    for (nonTempProfileViewModel in nonTempProfileViewModels) {
        nonTempProfileViewModel.cleanUp()
    }
}

