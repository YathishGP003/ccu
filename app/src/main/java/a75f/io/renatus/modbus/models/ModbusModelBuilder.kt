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

            registersMap.forEach {
                val rawMap = RawPoint.Builder().setHashMap(it).build()
                val logicalPointMap = CCUHsApi.getInstance().readMapById(rawMap.pointRef)

                val logicalPoint = Point.Builder().setHashMap(logicalPointMap).build()
                val register = Register()
                register.registerNumber = rawMap.registerNumber
                register.registerAddress = rawMap.registerAddress.toInt()
                register.registerType = rawMap.registerType
                register.parameterDefinitionType = null
                register.wordOrder = null
                register.multiplier = null
                val param = Parameter()
                param.parameterId = rawMap.parameterId
                param.name = rawMap.shortDis
                param.startBit = rawMap.startBit.toInt()
                param.endBit = rawMap.endBit.toInt()
                param.bitParamRange = null
                param.bitParam = null
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

    }

}