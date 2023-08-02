package a75f.io.device.modbus

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
import java.util.ArrayList

/**
 * Created by Manjunath K on 31-07-2023.
 */


fun buildModbusModel(zoneRef: String): List<EquipmentDevice> {
    val equipListMap = CCUHsApi.getInstance().readAllEntities("equip and modbus and not equipRef and roomRef == \"$zoneRef\"")
    val equips = mutableListOf<EquipmentDevice>()
    equipListMap.forEach { equipMap ->
        val equipDevice =  buildEquipModel(equipMap,null)
        val childEquipMaps = CCUHsApi.getInstance().readAllEntities("equip and modbus and equipRef== \"${equipMap["id"].toString()}\"")
        childEquipMaps.forEach {
            equipDevice.equips.add(buildEquipModel(it, equipMap["id"].toString()))
        }
        equips.add(equipDevice)
    }
    return equips
}

fun buildModbusModel(slaveId: Int): EquipmentDevice {
    val parentMap = CCUHsApi.getInstance().readEntity("equip and modbus and not equipRef and group == \"$slaveId\"")
    val equipDevice =  buildEquipModel(parentMap,null)
    val childEquipMaps = CCUHsApi.getInstance().readAllEntities("equip and modbus and equipRef== \"${parentMap["id"].toString()}\"")
    childEquipMaps.forEach {
        equipDevice.equips.add(buildEquipModel(it,parentMap["id"].toString()))
    }
    return equipDevice
}

fun buildModbusModelByEquipRef(equipRef: String): EquipmentDevice {
    val parentMap = CCUHsApi.getInstance().readMapById(equipRef)
    val equipDevice =  buildEquipModel(parentMap,null)
    val childEquipMaps = CCUHsApi.getInstance().readAllEntities("equip and modbus and equipRef== \"${parentMap["id"].toString()}\"")
    childEquipMaps.forEach {
        equipDevice.equips.add(buildEquipModel(it,parentMap["id"].toString()))
    }
    return equipDevice
}


private fun buildEquipModel(parentMap: HashMap<Any, Any>, parentEquipRef: String?) : EquipmentDevice {
    val equipDevice = EquipmentDevice()
    val equip = Equip.Builder().setHashMap(parentMap).build()
    equipDevice.id = 0L
    equipDevice.modbusEquipIdId = null
    equipDevice.name = equip.displayName
    equipDevice.description = null
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
    val deviceId = CCUHsApi.getInstance()
        .readEntity("device and modbus and equipRef == \"${equip.id}\"")[Tags.ID]

    val registersMap = CCUHsApi.getInstance().readAllEntities(
        "physical and point and deviceRef == \"${deviceId}\""
    )

    registersMap.forEach { rawMap ->
        val rawPhysicalPoint = RawPoint.Builder().setHashMap(rawMap).build()
        val logicalPointMap = CCUHsApi.getInstance().readMapById(rawPhysicalPoint.pointRef)

        val logicalPoint = Point.Builder().setHashMap(logicalPointMap).build()
        val register = Register()
        register.registerNumber = rawPhysicalPoint.registerNumber
        register.registerAddress = rawPhysicalPoint.registerAddress.toInt()
        register.registerType = rawPhysicalPoint.registerType
        register.parameterDefinitionType = if (rawMap.containsKey("parameterDefinitionType")) rawMap["parameterDefinitionType"].toString() else null
        register.wordOrder = if (rawMap.containsKey("wordOrder")) rawMap["wordOrder"].toString() else null
        register.multiplier = if (rawMap.containsKey("multiplier")) rawMap["multiplier"].toString() else null
        val param = Parameter()
        param.parameterId = rawPhysicalPoint.parameterId
        param.name = rawPhysicalPoint.shortDis
        param.startBit = rawPhysicalPoint.startBit.toInt()
        param.endBit = rawPhysicalPoint.endBit.toInt()
        param.bitParamRange = if (rawMap.containsKey("bitParamRange")) rawMap["bitParamRange"].toString() else null
        param.bitParam =  if (rawMap.containsKey("bitParam")) strToDouble(rawMap["bitParam"].toString()) else 0
        param.logicalPointTags = mutableListOf<LogicalPointTags>()
        param.commands = mutableListOf<Command>()
        param.userIntentPointTags = mutableListOf<UserIntentPointTags>()
        param.conditions = mutableListOf<Condition>()
        param.isDisplayInUI = logicalPoint.markers.contains("displayInUi")

        val isWritable = logicalPoint.markers.contains("writable")
        logicalPoint.markers.forEach { marker ->
            param.logicalPointTags.add(getLogicalPointTags(marker,null))
            if (isWritable)
                param.userIntentPointTags.add(getUserIntentPointTags(marker,null))
        }

        if (!logicalPoint.unit.isNullOrEmpty()) {
            param.logicalPointTags.add(getLogicalPointTags("unit",logicalPoint.unit))
            if (isWritable)
                param.userIntentPointTags.add(getUserIntentPointTags("unit",logicalPoint.unit))
        }
        if (!logicalPoint.hisInterpolate.isNullOrEmpty()) {
            param.logicalPointTags.add(getLogicalPointTags("hisInterpolate", logicalPoint.hisInterpolate))
            if (isWritable)
                param.userIntentPointTags.add(getUserIntentPointTags("hisInterpolate", logicalPoint.hisInterpolate))
        }

        if (!logicalPoint.minVal.isNullOrEmpty()) {
            param.logicalPointTags.add(getLogicalPointTags("minVal", logicalPoint.minVal))
            if (isWritable)
                param.userIntentPointTags.add(getUserIntentPointTags("minVal", logicalPoint.minVal))
        }
        if (!logicalPoint.maxVal.isNullOrEmpty()) {
            param.logicalPointTags.add(getLogicalPointTags("maxVal", logicalPoint.maxVal))
            if (isWritable)
                param.userIntentPointTags.add(getUserIntentPointTags("maxVal", logicalPoint.maxVal))
        }
        if (!logicalPoint.incrementVal.isNullOrEmpty()) {
            param.logicalPointTags.add(getLogicalPointTags("incrementVal", logicalPoint.incrementVal))
            if (isWritable)
                param.userIntentPointTags.add(getUserIntentPointTags("incrementVal", logicalPoint.incrementVal))
        }

        if (!logicalPoint.cell.isNullOrEmpty()) {
            param.logicalPointTags.add(getLogicalPointTags("cell", logicalPoint.cell))
            if (isWritable)
                param.userIntentPointTags.add(getUserIntentPointTags("cell", logicalPoint.cell))
        }
        if (!logicalPoint.enums.isNullOrEmpty()) {
            if (logicalPoint.markers.contains("cmd")) {
                param.commands = getCommands(logicalPoint.enums)
            } else {
                param.conditions = getConditions(logicalPoint.enums)
            }
        }
        register.parameters = mutableListOf(param)
        equipDevice.registers.add(register)

    }
    return equipDevice
}

private fun getLogicalPointTags(tag: String?, value: String?): LogicalPointTags {
    val logicalTag = LogicalPointTags()
    if (!tag.isNullOrEmpty())
        logicalTag.tagName = tag
    if (!value.isNullOrEmpty())
        logicalTag.tagValue = value
    return logicalTag
}

private fun getUserIntentPointTags(tag: String?, value: String?): UserIntentPointTags {
    val userIntentTag = UserIntentPointTags()
    if (!tag.isNullOrEmpty())
        userIntentTag.tagName = tag
    if (!value.isNullOrEmpty())
        userIntentTag.tagValue = value
    return userIntentTag
}

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

private fun strToDouble(str: String?): Int {
    if (str != null) {
        if (str.contains("."))
            return str.toDouble().toInt()
        return str.toInt()
    } else
        return 0
}
