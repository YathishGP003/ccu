package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.logic.bo.building.ccu.CazEquipUtil
import a75f.io.logic.bo.building.ccu.RoomTempSensor
import a75f.io.logic.bo.building.ccu.SupplyTempSensor
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.haystack.device.ControlMote
import com.google.gson.JsonObject
import java.util.*

class TIConfigHandler {
    companion object {
        fun updateTIConfig(msgobject: JsonObject, configPoint: Point, haystack: CCUHsApi) {
            val value = msgobject.get("val").asInt
            CCUHsApi.getInstance().writeHisValById(configPoint.id, value.toDouble())

            val device: HashMap<Any, Any> = haystack.readEntity("cm and device ")
            val currentTempPoint: HashMap<Any, Any> = haystack.readEntity(
                "point and current and " +
                        "temp and equipRef == \"" + configPoint.equipRef + "\""
            )
            val nodeAddress = configPoint.group.toInt()

            val supplyTempSensorTypePointId = haystack.readEntity("point and supply and " +
                    "type and temp and equipRef == \"" + configPoint.equipRef + "\"")["id"].toString()

            val roomTempSensorTypePointId = haystack.readEntity("point and space and " +
                    "type and temp and equipRef == \"" + configPoint.equipRef + "\"")["id"].toString()

            var supplyTempSensor = haystack.readDefaultValById(supplyTempSensorTypePointId).toInt()
            var roomTempSensor = haystack.readDefaultValById(roomTempSensorTypePointId).toInt()

            if (configPoint.id == supplyTempSensorTypePointId) {
                supplyTempSensor = msgobject.get("val").asInt
            }
            if (configPoint.id == roomTempSensorTypePointId) {
                roomTempSensor = msgobject.get("val").asInt
            }

            val roomTempSensorConfig = RoomTempSensor.values()[roomTempSensor]
            val supplyTempSensorConfig = SupplyTempSensor.values()[supplyTempSensor]

            if (device.isNotEmpty() && currentTempPoint.isNotEmpty()) {

                ControlMote.setPointEnabled(nodeAddress, Port.TH1_IN.name, false)
                ControlMote.setPointEnabled(nodeAddress, Port.TH2_IN.name, false)
                ControlMote.setPointEnabled(nodeAddress, Port.SENSOR_RT.name, false)
                ControlMote.setCMPointEnabled(Port.TH1_IN.name, false)
                ControlMote.setCMPointEnabled(Port.TH2_IN.name, false)
                if (supplyTempSensorConfig == SupplyTempSensor.THERMISTOR_1 || roomTempSensorConfig == RoomTempSensor.THERMISTOR_1) {
                    ControlMote.setPointEnabled(nodeAddress, Port.TH1_IN.name, true)
                    ControlMote.setCMPointEnabled(Port.TH1_IN.name, true)
                }
                if (supplyTempSensorConfig == SupplyTempSensor.THERMISTOR_2 || roomTempSensorConfig == RoomTempSensor.THERMISTOR_2) {
                    ControlMote.setPointEnabled(nodeAddress, Port.TH2_IN.name, true)
                    ControlMote.setCMPointEnabled(Port.TH2_IN.name, true)
                }
                if (roomTempSensorConfig == RoomTempSensor.SENSOR_BUS_TEMPERATURE) {
                    ControlMote.setPointEnabled(nodeAddress, Port.SENSOR_RT.name, true)
                    ControlMote.setCMPointEnabled(Port.SENSOR_RT.name, true)
                }
                updateSensorPoints(supplyTempSensorConfig, roomTempSensorConfig, currentTempPoint, nodeAddress, configPoint.equipRef)
            }
            CCUHsApi.getInstance().scheduleSync()
        }

        private fun updateSensorPoints(
            supplyTempSensor: SupplyTempSensor,
            roomTempSensor: RoomTempSensor,
            currentTempPoint: HashMap<Any, Any>,
            nodeAddr: Int,
            equipRef: String
        ) {

            val hayStack = CCUHsApi.getInstance()
            val equipHash = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddr\"")
            val equip = Equip.Builder().setHashMap(equipHash).build()
            val supplyTempSensorPoint = hayStack.readEntity(
                "point and supply and not type and temp and equipRef == \"$equipRef\""
            )
            val roomTempSensorPoint = hayStack.readEntity(
                "point and space and not type and temp and equipRef == \"$equipRef\""
            )
            val supplyTempSensorPointExists = supplyTempSensorPoint.isNotEmpty()
            when (supplyTempSensor) {
                SupplyTempSensor.NONE -> if (supplyTempSensorPointExists) {
                    CCUHsApi.getInstance().deleteEntity(
                        Objects.requireNonNull(
                            supplyTempSensorPoint["id"]
                        ).toString()
                    )
                }
                SupplyTempSensor.THERMISTOR_1 -> if (!supplyTempSensorPointExists) {
                    val supplyAirTempId = CazEquipUtil.createSupplyAirTempPoint(equip, nodeAddr)
                    ControlMote.updatePhysicalPointRef(
                        nodeAddr,
                        Port.TH1_IN.name,
                        Objects.requireNonNull(supplyAirTempId)
                    )
                } else {
                    ControlMote.updatePhysicalPointRef(
                        nodeAddr, Port.TH1_IN.name, Objects.requireNonNull(
                            supplyTempSensorPoint["id"]
                        ).toString()
                    )
                }
                SupplyTempSensor.THERMISTOR_2 -> if (!supplyTempSensorPointExists) {
                    val supplyAirTempId = CazEquipUtil.createSupplyAirTempPoint(equip, nodeAddr)
                    ControlMote.updatePhysicalPointRef(
                        nodeAddr,
                        Port.TH2_IN.name,
                        Objects.requireNonNull(supplyAirTempId)
                    )
                } else {
                    ControlMote.updatePhysicalPointRef(
                        nodeAddr, Port.TH2_IN.name, Objects.requireNonNull(
                            supplyTempSensorPoint["id"]
                        ).toString()
                    )
                }
            }
            when (roomTempSensor) {
                RoomTempSensor.SENSOR_BUS_TEMPERATURE -> ControlMote.updatePhysicalPointRef(
                    nodeAddr, Port.SENSOR_RT.name, Objects.requireNonNull(
                        currentTempPoint["id"]
                    ).toString()
                )
                RoomTempSensor.THERMISTOR_1 -> ControlMote.updatePhysicalPointRef(
                    nodeAddr, Port.TH1_IN.name, Objects.requireNonNull(
                        roomTempSensorPoint["id"]
                    ).toString()
                )
                RoomTempSensor.THERMISTOR_2 -> ControlMote.updatePhysicalPointRef(
                    nodeAddr, Port.TH2_IN.name, Objects.requireNonNull(
                        roomTempSensorPoint["id"]
                    ).toString()
                )
            }

        }

    }
}


