package a75f.io.logic.bo.building.modbus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.modbus.Command
import a75f.io.api.haystack.modbus.Condition
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.LogicalPointTags
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.api.haystack.modbus.Register
import a75f.io.api.haystack.modbus.UserIntentPointTags
import a75f.io.domain.api.Room
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil

/**
 * Created by Manjunath K on 31-07-2023.
 */

const val ID = "id"
const val PARAM_DEFINITION_TYPE = "parameterDefinitionType"
const val WORD_ORDER = "wordOrder"
const val MULTIPLIER = "multiplier"
const val BIT_PARAM_RANGE = "bitParamRange"
const val BIT_PARAM = "bitParam"
const val DISPLAY_UI = "displayInUi"
const val UNIT = "unit"
const val HIS_INTERPOLATE = "hisInterpolate"
const val MIN_VAL = "minVal"
const val MAX_VAL = "maxVal"
const val INCREMENTAL_VAL = "incrementVal"
const val CELL = "cell"
const val VERSION = "version"
const val DEFAULT_VALUE = "defaultValue"

/**
 * @param zoneRef
 * returns EquipmentDevice list for a given zone ref
 */
fun buildModbusModel(zoneRef: String): List<EquipmentDevice> {
    val equipListMap = getParentEquipMapByZone(zoneRef)
    val equips = mutableListOf<EquipmentDevice>()
    equipListMap.forEach { equipMap ->
        val equipDevice = buildEquipModel(equipMap, null)
        equipDevice.equips.addAll(getChildEquips(equipMap[ID].toString()))
        equips.add(equipDevice)
    }
    return equips
}

/**
 * @param slaveId
 * returns EquipmentDevice for give slave id
 */
fun buildModbusModel(slaveId: Int): EquipmentDevice {
    val parentMap = getParentEquipMapBySlaveId(slaveId)
    val equipDevice = buildEquipModel(parentMap, null)
    equipDevice.equips.addAll(getChildEquips(parentMap[ID].toString()))
    return equipDevice
}

fun buildModbusModel(slaveId: Int, zoneRef: String): EquipmentDevice {
    val parentMap = getParentEquipMapBySlaveAndZone(slaveId, zoneRef)
    val equipDevice = buildEquipModel(parentMap, null)
    equipDevice.equips.addAll(getChildEquips(parentMap[ID].toString()))
    return equipDevice
}

/**
 * @param equipRef
 * returns EquipmentDevice for given equip ref
 */
fun buildModbusModelByEquipRef(equipRef: String): EquipmentDevice {
    val parentMap = CCUHsApi.getInstance().readMapById(equipRef)
    val equipDevice = buildEquipModel(parentMap, null)
    equipDevice.equips.addAll(getChildEquips(parentMap[ID].toString()))
    return equipDevice
}

/**
 * @param equipMap
 * @param parentEquipRef
 * returns the EquipmentDevice object with all the details
 */
private fun getEquipByMap(equipMap: HashMap<Any, Any>, parentEquipRef: String?): EquipmentDevice {
    val equipDevice = EquipmentDevice()
    val equip = Equip.Builder().setHashMap(equipMap).build()
    equipDevice.modbusEquipIdId = if (equip.tags.containsKey(VERSION)) equip.tags[VERSION].toString() else "0.0.0" // We are not using model id so just holding version in model id
    equipDevice.description = null
    equipDevice.id = 0L
    equipDevice.name = getModelName(equip.displayName, equip.group)
    equipDevice.equipType = equip.equipType
    equipDevice.vendor = equip.vendor
    equipDevice.modelNumbers = mutableListOf<String>(equip.model)
    equipDevice.registers = mutableListOf<Register>()
    equipDevice.equips = mutableListOf<EquipmentDevice>()
    equipDevice.equipRef = parentEquipRef
    equipDevice.cell = equip.cell
    equipDevice.capacity = equip.capacity
    equipDevice.slaveId = equip.group.toInt()
    equipDevice.zoneRef = equip.roomRef
    equipDevice.floorRef = equip.floorRef
    equipDevice.isPaired = (equipDevice.deviceEquipRef == null)
    equipDevice.slaveId = equip.group.toInt()
    equipDevice.deviceEquipRef = equip.id
    equipDevice.port = equip.tags?.get("port")?.toString() ?: ""
    return equipDevice
}

/**
 * @param parentEquipRef
 * returns all the child equip details for the given parent ref
 */
private fun getChildEquips(parentEquipRef: String): List<EquipmentDevice> {
    val devices = mutableListOf<EquipmentDevice>()
    val childEquipMaps = getChildEquipMap(parentEquipRef)
    childEquipMaps.forEach { devices.add(buildEquipModel(it, parentEquipRef)) }
    return devices
}


private fun buildEquipModel(parentMap: HashMap<Any, Any>, parentEquipRef: String?): EquipmentDevice {
    val equipId = parentMap[ID].toString()
    val equipDevice = getEquipByMap(parentMap, parentEquipRef)
    val isConnectNodeAsZone = ConnectNodeUtil.getConnectNodeForZone(equipDevice.zoneRef, CCUHsApi.getInstance()).isNotEmpty()
    val isConnectModule = parentMap.containsKey(Tags.CONNECTMODULE)
    val isPCN = parentMap.containsKey(Tags.PCN)
    val isLowCodeEquip = (isPCN || isConnectNodeAsZone || isConnectModule)
    val registersMapList = getRegistersMap(equipId, isLowCodeEquip)
    registersMapList.forEach { registerMap -> equipDevice.registers.add(getRegister(registerMap, isLowCodeEquip)) }
    return equipDevice
}

/**
 * @param equipId
 * return all the register map list for the the equip
 */
private fun getRegistersMap(equipId: String, isLowCodeEquip: Boolean): ArrayList<HashMap<Any, Any>> {
    val hsApi = CCUHsApi.getInstance()
    if (isLowCodeEquip) {
        return hsApi.readAllEntities("registerNumber and point and equipRef == \"$equipId\"")
    }
    val deviceId = hsApi.readId("device and modbus and equipRef == \"$equipId\"")
    return hsApi.readAllEntities("physical and point and deviceRef == \"$deviceId\"")
}

/**
 * @param rawMap
 * returns register object with all the register details
 */
private fun getRegister(rawMap: HashMap<Any, Any>, isLowCodeEquip: Boolean): Register {
    val physicalPoint = RawPoint.Builder().setHashMap(rawMap).build()
    val register = Register()
    register.registerNumber = physicalPoint.registerNumber
    register.registerAddress = physicalPoint.registerAddress.toInt()
    register.registerType = physicalPoint.registerType
    register.parameterDefinitionType = getValue(rawMap, PARAM_DEFINITION_TYPE)
    register.wordOrder = getValue(rawMap, WORD_ORDER)
    register.multiplier = getValue(rawMap, MULTIPLIER)
    register.parameters = mutableListOf(getParameter(physicalPoint, rawMap, isLowCodeEquip))
    register.defaultValue = if(rawMap.containsKey(DEFAULT_VALUE)) rawMap[DEFAULT_VALUE].toString().toDouble() else null
    register.parameters = mutableListOf(getParameter(physicalPoint, rawMap, isLowCodeEquip))
    return register
}


/**
 * @param physicalPoint
 * @param rawMap
 * returns Parameter object with all the parameter details
 */
private fun getParameter(
    physicalPoint: RawPoint,
    rawMap: HashMap<Any, Any>,
    isLowCodeEquip: Boolean
): Parameter {
    val param = getBasicParamData(physicalPoint, rawMap, isLowCodeEquip)

    val logicalPoint = if (isLowCodeEquip) {
        Point.Builder().setHashMap(rawMap).build()
    } else {
        Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(physicalPoint.pointRef)).build()
    }

    param.isDisplayInUI = logicalPoint.markers.contains(DISPLAY_UI)
    param.logicalId = logicalPoint.id
    param.isSchedulable = logicalPoint.markers.contains(Tags.SCHEDULABLE)
    val isWritable = logicalPoint.markers.contains(Tags.WRITABLE)
    logicalPoint.markers.forEach { marker ->
        param.logicalPointTags.add(getLogicalPointTags(marker, null))
        if (isWritable)
            param.userIntentPointTags.add(getUserIntentPointTags(marker, null))
    }
    logicalPoint.tags.forEach { (marker , value) ->
        param.logicalPointTags.add(getLogicalPointTags(marker, value.toString()))
        if (isWritable)
            param.userIntentPointTags.add(getUserIntentPointTags(marker, value.toString()))
    }
    addTagValues(param,logicalPoint.unit,UNIT,isWritable)
    addTagValues(param,logicalPoint.hisInterpolate,HIS_INTERPOLATE,isWritable)
    addTagValues(param,logicalPoint.minVal,MIN_VAL,isWritable)
    addTagValues(param,logicalPoint.maxVal, MAX_VAL,isWritable)
    addTagValues(param,logicalPoint.incrementVal,INCREMENTAL_VAL,isWritable)
    addTagValues(param,logicalPoint.cell, CELL,isWritable)
    if (!logicalPoint.enums.isNullOrEmpty()) {
        if (logicalPoint.markers.contains(Tags.CMD) || logicalPoint.markers.contains(Tags.SP))
            param.commands = getCommands(logicalPoint.enums)
        else
            param.conditions = getConditions(logicalPoint.enums)
    }
    return param
}


/**
 * @param physicalPoint
 * @param rawMap
 * returns Parameter object with basic parameter details
 */
private fun getBasicParamData(
    physicalPoint: RawPoint,
    rawMap: HashMap<Any, Any>,
    isLowCodeEquip: Boolean
): Parameter {
    val param = Parameter()
    param.parameterId = physicalPoint.parameterId
    param.name = physicalPoint.shortDis
    param.startBit = if (!isLowCodeEquip) physicalPoint.startBit.toInt() else {0}
    param.endBit = if (!isLowCodeEquip) physicalPoint.endBit.toInt() else {0}
    param.bitParamRange = getValue(rawMap, BIT_PARAM_RANGE)
    param.bitParam = if (rawMap.containsKey(BIT_PARAM)) strToDouble(rawMap[BIT_PARAM].toString()) else 0
    param.logicalPointTags = mutableListOf<LogicalPointTags>()
    param.userIntentPointTags = mutableListOf<UserIntentPointTags>()
    return param
}


/**
 * @param param  Parameter reference
 * @param value
 * @param key
 * @param isWritable to push into user intent
 * return
 */
private fun addTagValues(param: Parameter,value: String?, key: String, isWritable: Boolean) {
    if (!value.isNullOrEmpty()) {
        param.logicalPointTags.add(getLogicalPointTags(key, value))
        if (isWritable)
            param.userIntentPointTags.add(getUserIntentPointTags(key, value))
    }
}

/**
 * @param tag user intent tag
 * @param value value for the tag
 * returns LogicalPointTags object
 */
private fun getLogicalPointTags(tag: String?, value: String?): LogicalPointTags {
    val logicalTag = LogicalPointTags()
    if (!tag.isNullOrEmpty())
        logicalTag.tagName = tag
    if (!value.isNullOrEmpty())
        logicalTag.tagValue = value
    return logicalTag
}

/**
 * @param tag user intent tag
 * @param value value for the tag
 * returns UserIntentPointTags object
 */
private fun getUserIntentPointTags(tag: String?, value: String?): UserIntentPointTags {
    val userIntentTag = UserIntentPointTags()
    if (!tag.isNullOrEmpty())
        userIntentTag.tagName = tag
    if (!value.isNullOrEmpty())
        userIntentTag.tagValue = value
    return userIntentTag
}

/**
 * @param enum
 * function takes enum as input and convert into commands
 * return list of commands
 */
private fun getConditions(enum: String?): List<Condition> {
    val conditionsList = mutableListOf<Condition>()
    if (!enum.isNullOrEmpty()) {
        val data = enum.split(",")
        if (data.isNotEmpty()) {
            data.forEach {
                val condition = Condition()
                val keyValue = it.split("=")
                if (keyValue.isNotEmpty()) {
                    condition.name = keyValue[0]
                    condition.bitValues = keyValue[1]
                }
                conditionsList.add(condition)
            }
        }
    }
    return conditionsList
}

/**
 * @param enum
 * Function takes enum convert into commands
 * returns list of commands
 */
private fun getCommands(enum: String?): List<Command> {
    val commandsList = mutableListOf<Command>()
    if (!enum.isNullOrEmpty()) {
        val data = enum.split(",")
        if (data.isNotEmpty()) {
            data.forEach {
                val command = Command()
                val keyValue = it.split("=")
                if (keyValue.isNotEmpty()) {
                    command.name = keyValue[0]
                    command.bitValues = keyValue[1]
                }
                commandsList.add(command)
            }
        }
    }
    return commandsList
}

/**
 * function if string contains dot . convert to double else return 0
 */
private fun strToDouble(str: String?): Int {
    if (str != null) {
        if (str.contains("."))
            return str.toDouble().toInt()
        return str.toInt()
    } else
        return 0
}

/**
 * Function returns map value if key exist
 */
private fun getValue(map: HashMap<Any, Any>, key: String): String? {
    return if(map.containsKey(key)) map[key].toString() else null
}

/**
 * @param slaveId
 * Read parent equip details by slave id
 * return hashmap of equips
 */
fun getParentEquipMapBySlaveId(slaveId: Int): HashMap<Any, Any> {
    return CCUHsApi.getInstance()
        .readEntity("equip and modbus and not equipRef and group == \"$slaveId\"")
}

/**
 * @param zoneRef
 * Read parent equip details by zone id
 * return list of hashmaps for all the parent equips
 */
private fun getParentEquipMapByZone(zoneRef: String): ArrayList<HashMap<Any, Any>> {
    return CCUHsApi.getInstance()
        .readAllEntities("equip and (modbus or connectModule) and not equipRef and roomRef == \"$zoneRef\"")
}

/**
 * @param equipRef
 * Read all the child equipments for the parent equip
 * return list of child equip hashmaps for all selected parent map
 */
private fun getChildEquipMap(equipRef: String): ArrayList<HashMap<Any, Any>> {
    return CCUHsApi.getInstance()
        .readAllEntities("equip and modbus and equipRef== \"$equipRef\"")
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

fun getParentEquipMapBySlaveAndZone(slaveId: Int, roomRef: String): HashMap<Any, Any> {
    return CCUHsApi.getInstance()
        .readEntity("equip and modbus and not equipRef and group == \"$slaveId\" and roomRef == \"$roomRef\"")
}