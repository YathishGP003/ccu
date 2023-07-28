package a75f.io.renatus.modbus.models

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

/**
 * Created by Manjunath K on 24-07-2023.
 */

class ModbusModelBuilder {
    companion object {



        fun buildModbusModel(zoneRef: String): EquipmentDevice {
            val parentMap = CCUHsApi.getInstance().readEntity("equip and modbus and not equipRef and roomRef == \"$zoneRef\"")
            val equipDevice =  buildEquipModel(parentMap)
            val childEquipMaps = CCUHsApi.getInstance().readAllEntities("equip and modbus and equipRef== \"${parentMap["id"].toString()}\"")
            childEquipMaps.forEach {
                equipDevice.equips.add(buildEquipModel(it))
            }
            return equipDevice
        }

        fun buildModbusModel(slaveId: Int): EquipmentDevice {
            val parentMap = CCUHsApi.getInstance().readEntity("equip and modbus and not equipRef and group == \"$slaveId\"")
            val equipDevice =  buildEquipModel(parentMap)
            val childEquipMaps = CCUHsApi.getInstance().readAllEntities("equip and modbus and equipRef== \"${parentMap["id"].toString()}\"")
            childEquipMaps.forEach {
                equipDevice.equips.add(buildEquipModel(it))
            }
            return equipDevice
        }

        private fun buildEquipModel(parentMap: HashMap<Any, Any>) : EquipmentDevice {
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
            equipDevice.equipRef = null
            equipDevice.cell = equip.cell
            equipDevice.capacity = equip.capacity
            equipDevice.slaveId = equip.group.toInt()
            equipDevice.zoneRef = equip.roomRef
            equipDevice.floorRef = equip.floorRef
            equipDevice.deviceEquipRef = null
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
                logicalPoint.markers.forEach { marker ->
                    val logicPoint = LogicalPointTags()
                    logicPoint.tagName = marker
                    if (marker.equals("unit"))
                        logicPoint.tagValue = logicalPoint.unit
                    if (marker.equals("hisInterpolate"))
                        logicPoint.tagValue = logicalPoint.hisInterpolate
                    if (marker.equals("minVal"))
                        logicPoint.tagValue = logicalPoint.minVal
                    if (marker.equals("maxVal"))
                        logicPoint.tagValue = logicalPoint.maxVal
                    if (marker.equals("incrementVal"))
                        logicPoint.tagValue = logicalPoint.incrementVal
                    if (marker.equals("cell"))
                        logicPoint.tagValue = logicalPoint.cell
                    param.logicalPointTags.add(logicPoint)
                }
               register.parameters = mutableListOf(param)
               equipDevice.registers.add(register)
            }
            return equipDevice
        }

        private fun strToDouble(str: String?): Int {
            if (str != null) {
                if (str.contains(".")){
                    return str.toDouble().toInt()
                }
                return str.toInt()
            } else
                return 0
        }
    }
}