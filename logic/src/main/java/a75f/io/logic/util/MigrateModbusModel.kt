package a75f.io.logic.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.logger.CcuLog
import a75f.io.modbusbox.EquipsManager
import org.projecthaystack.HStr

/**
 * Created by Manjunath K on 07-08-2023.
 */

const val TAG = "MODBUS_MIGRATION"

fun migrateModbusProfiles() {
    val hsApi = CCUHsApi.getInstance()
    val allModbusDevices = hsApi.readAllEntities("equip and modbus and not equipRef")
    CcuLog.i(TAG, "Modbus total devices found ${allModbusDevices.size}")
    allModbusDevices.forEach { equip ->
        val slaveId = equip["group"].toString().toInt()
        val device = EquipsManager.getInstance().fetchProfileBySlaveId(slaveId)
        if (device != null) {
            migrateModbusEquip(device, hsApi, equip)
            if (!device.equips.isNullOrEmpty()) {
                CcuLog.i(TAG, "total child  found ${device.equips}")
                device.equips.forEach { childDevice ->
                    val childEquip = hsApi.readMapById(childDevice.deviceEquipRef)
                    migrateModbusEquip(childDevice, hsApi, childEquip)
                }
            }
        }
    }
    CcuLog.i(TAG, "Modbus migration end")
    hsApi.scheduleSync()
}


fun migrateModbusEquip(device: EquipmentDevice?, hsApi: CCUHsApi, equip: HashMap<Any, Any>) {
    if (device == null) {
        CcuLog.i(TAG, "device is null for $equip")
        return
    }

    CcuLog.i(TAG, "equip migrating ${device.name}")
    try {
        val updatedEquip = Equip.Builder().setHashMap(equip)
        if (!equip.containsKey("equipType")) {
            updatedEquip.setEquipType(device.equipType)
        }
        if (updatedEquip.build().roomRef!!.contentEquals("SYSTEM")) {
            updatedEquip.addMarker("system")
        }
        val buildEquip = updatedEquip.build()
        hsApi.updateEquip(buildEquip, buildEquip.id)
        CcuLog.i(
            TAG, "Equip is updated with equip type" +
                    "\nbefore registers $device" +
                    "\nParent: ${buildEquip.id}"
        )

        val deviceId =
            hsApi.readId("device and modbus and equipRef ==\"${buildEquip.id}\"")
        device.registers.forEach { register ->
            val rawPoint =
                hsApi.readEntity("point and physical and parameterId == \"${register.parameters[0].parameterId}\" and deviceRef == \"$deviceId\"")
            updateIncValue(register.parameters[0], rawPoint)
            val point = RawPoint.Builder().setHashMap(rawPoint)
            CcuLog.i(
                TAG,
                "original getParameterDefinitionType: ${register.getParameterDefinitionType()}" +
                        " Multiplier: ${register.getMultiplier()}" +
                        " WordOrder: ${register.getWordOrder()}" +
                        " BitParamRange: ${register.parameters[0].getBitParamRange()}" +
                        " BitParam: ${register.parameters[0].getBitParam()}"
            )
            if (!point.build().tags.containsKey("parameterDefinitionType")) {
                point.addTag(
                    "parameterDefinitionType",
                    HStr.make(register.getParameterDefinitionType())
                )
            }
            if (!point.build().tags.containsKey("multiplier")) {
                point.addTag("multiplier", HStr.make(register.getMultiplier()))
            }
            if (!point.build().tags.containsKey("wordOrder")) {
                point.addTag("wordOrder", HStr.make(register.getWordOrder()))
            }
            if (!point.build().tags.containsKey("bitParamRange")) {
                point.addTag(
                    "bitParamRange",
                    HStr.make(register.parameters[0].getBitParamRange())
                )
            }
            if (!point.build().tags.containsKey("bitParam")) {
                point.addTag(
                    "bitParam",
                    HStr.make(
                        if (register.parameters[0].getBitParam() != null) register.parameters[0].getBitParam()
                            .toString() else "0"
                    )
                )
            }
            val buildPoint = point.build()
            hsApi.updatePoint(buildPoint, buildPoint.id)
            val afterUpdateRawPoint =
                hsApi.readEntity("point and physical and parameterId == \"${register.parameters[0].parameterId}\"")
            CcuLog.i(
                TAG, "Before update point: $rawPoint" +
                        "\nafter update point: $afterUpdateRawPoint"
            )
        }
    } catch (e: Exception) {
        CcuLog.e(TAG, "Error: ${e.message}",e)
    }
}

fun updateIncValue(param: Parameter, physicalRawPoint: HashMap<Any, Any>) {
    CcuLog.i(TAG, "Modbus updateIncValue stared")
    if (!param.getUserIntentPointTags().isNullOrEmpty()) {
        param.getUserIntentPointTags().forEach { userIntentPointTags ->
            if (userIntentPointTags.tagName!!.contentEquals("incrementVal")) {
                val logicalPointMap =
                    CCUHsApi.getInstance().readMapById(physicalRawPoint["pointRef"].toString())
                val logicalPoint = Point.Builder().setHashMap(logicalPointMap)
                    .setIncrementVal(userIntentPointTags.tagValue).build()
                CCUHsApi.getInstance().updatePoint(logicalPoint, logicalPoint.id)
                return
            }
        }
    }
}