package a75f.io.logic.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.modbusbox.EquipsManager
import android.util.Log
import org.projecthaystack.HStr

/**
 * Created by Manjunath K on 07-08-2023.
 */

const val TAG = "MODBUS_MIGRATION"

fun migrateModbusProfiles() {
    Log.i(TAG, "Modbus migration started")
    val hsApi = CCUHsApi.getInstance()
    val allModbusDevices = hsApi.readAllEntities("equip and modbus and not equipRef")
    Log.i(TAG, "Modbus total ${allModbusDevices.size}")
    allModbusDevices.forEach { equip ->
        val slaveId = equip["group"].toString().toInt()
        Log.i(TAG, "slaveId $slaveId")
        val device = EquipsManager.getInstance().fetchProfileBySlaveId(slaveId)
        device.equips.forEach {
            Log.i(TAG, "child id ${it.equipRef}")
        }/*
        migrateModbusEquip(device,hsApi,equip)
        if (!device.equips.isNullOrEmpty()) {
            device.equips.forEach {childDevice ->
                val childEquip = hsApi.readMapById(childDevice.equipRef)
                migrateModbusEquip(childDevice,hsApi,childEquip)
            }

        }*/
    }
    Log.i(TAG, "Modbus migration end")
    hsApi.scheduleSync()
}


fun migrateModbusEquip(device: EquipmentDevice, hsApi: CCUHsApi, equip: HashMap<Any, Any>){
    try {
        if (!equip.containsKey("equipType")) {
            val updatedEquip = Equip.Builder().setHashMap(equip).setEquipType(device.equipType).build()
            hsApi.updateEquip(updatedEquip,updatedEquip.id)
            Log.i(TAG, "Equip is updated with equip type")
        } else {
            Log.i(TAG, "equipType found")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.i(TAG, "Error: ${e.printStackTrace()}")
    }


    if (device != null) {
        Log.i(TAG, "Parent: ${device.deviceEquipRef}")

        device.registers.forEach { register ->
            val rawPoint = hsApi.readEntity("point and physical and parameterId == \"${register.parameters[0].parameterId}\"")
            try {
                val point = RawPoint.Builder().setHashMap(rawPoint)
                Log.i(TAG, "original getParameterDefinitionType: ${register.getParameterDefinitionType()}")
                Log.i(TAG, "original getMultiplier: ${register.getMultiplier()}")
                Log.i(TAG, "original getWordOrder: ${register.getWordOrder()}")
                Log.i(TAG, "original getBitParamRange: ${register.parameters[0].getBitParamRange()}")
                Log.i(TAG, "original getBitParam: ${register.parameters[0].getBitParam()}")
                if (!point.build().tags.containsKey("parameterDefinitionType")) {
                    point.addTag("parameterDefinitionType", HStr.make(register.getParameterDefinitionType()))
                }
                if (!point.build().tags.containsKey("multiplier")) {
                    point.addTag("multiplier", HStr.make(register.getMultiplier()))
                }
                if (!point.build().tags.containsKey("wordOrder")) {
                    point.addTag("wordOrder", HStr.make(register.getWordOrder()))
                }
                if (!point.build().tags.containsKey("bitParamRange")) {
                    point.addTag("bitParamRange", HStr.make(register.parameters[0].getBitParamRange()))
                }
                if (!point.build().tags.containsKey("bitParam")) {
                    point.addTag("bitParam", HStr.make( if(register.parameters[0].getBitParam() != null)  register.parameters[0].getBitParam().toString() else "0"))
                }
                point.addTag("bitParam", HStr.make( if(register.parameters[0].getBitParam() != null)  register.parameters[0].getBitParam().toString() else "0"))
                Log.i(TAG, "after update point: $point")
                val buildPoint = point.build()
                hsApi.updatePoint(buildPoint,buildPoint.id)

                if (device != null && !device.equips.isNullOrEmpty()) {
                    device.equips.forEach { child ->
                        Log.i(TAG, "child: ${child.deviceEquipRef}")
                        child.registers.forEach { register ->

                            val physicalRawPoint =
                                hsApi.readEntity("point and physical and parameterId == \"${register.parameters[0].parameterId}\"")
                            updateIncValue(register.parameters[0],physicalRawPoint)
                            try {
                                val physicalRawpoint = RawPoint.Builder().setHashMap(physicalRawPoint)

                                Log.i(
                                    TAG,
                                    "original getParameterDefinitionType: ${register.getParameterDefinitionType()}"
                                )
                                Log.i(
                                    TAG,
                                    "original getMultiplier: ${register.getMultiplier()}"
                                )
                                Log.i(TAG, "original getWordOrder: ${register.getWordOrder()}")
                                Log.i(
                                    TAG,
                                    "original getBitParamRange: ${register.parameters[0].getBitParamRange()}"
                                )
                                Log.i(
                                    TAG,
                                    "original getBitParam: ${register.parameters[0].getBitParam()}"
                                )
                                if (!physicalRawpoint.build().tags.containsKey("parameterDefinitionType")) {
                                    physicalRawpoint.addTag(
                                        "parameterDefinitionType",
                                        HStr.make(register.getParameterDefinitionType())
                                    )
                                }
                                if (!physicalRawpoint.build().tags.containsKey("multiplier")) {
                                    physicalRawpoint.addTag(
                                        "multiplier",
                                        HStr.make(register.getMultiplier())
                                    )
                                }
                                if (!physicalRawpoint.build().tags.containsKey("wordOrder")) {
                                    physicalRawpoint.addTag(
                                        "wordOrder",
                                        HStr.make(register.getWordOrder())
                                    )
                                }
                                if (!physicalRawpoint.build().tags.containsKey("bitParamRange")) {
                                    physicalRawpoint.addTag(
                                        "bitParamRange",
                                        HStr.make(register.parameters[0].getBitParamRange())
                                    )
                                }
                                if (!physicalRawpoint.build().tags.containsKey("bitParam")) {
                                    physicalRawpoint.addTag(
                                        "bitParam",
                                        HStr.make(
                                            if (register.parameters[0].getBitParam() != null) register.parameters[0].getBitParam()
                                                .toString() else "0"
                                        )
                                    )
                                }
                                Log.i(TAG, "after update point: $physicalRawpoint")
                                val buildPointValue = physicalRawpoint.build()
                                hsApi.updatePoint(buildPointValue, buildPointValue.id)
                            } catch (e: Exception) {
                                Log.i(TAG, "error ${e.printStackTrace()}")
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "error ${e.printStackTrace()}")
                e.printStackTrace()
            }
        }

    } else {
        Log.i(TAG, "Device is null")
    }
}

fun updateIncValue(param: Parameter, physicalRawPoint: HashMap<Any, Any>) {
    Log.i(TAG, "Modbus updateIncValue stared")
    if (param.getUserIntentPointTags().isNotEmpty()) {
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