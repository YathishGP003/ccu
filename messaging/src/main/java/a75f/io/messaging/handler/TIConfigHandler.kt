package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logic.bo.building.ccu.RoomTempSensor
import a75f.io.logic.bo.building.ccu.SupplyTempSensor
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.haystack.device.ControlMote
import com.google.gson.JsonObject

class TIConfigHandler {
    companion object {
        fun updateTIConfig(msgobject: JsonObject, configPoint: Point, haystack: CCUHsApi) {
            val value = msgobject.get("val").asInt
            CCUHsApi.getInstance().writeHisValById(configPoint.id, value.toDouble())

            val currentTemp: HashMap<Any, Any>? = haystack.readEntity("point and current and " +
                    "temp and equipRef == \"" + configPoint.equipRef + "\"")
            val nodeAddr = configPoint.group.toInt()

            ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, false)
            ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, false)
            ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)

            ControlMote.setCMPointEnabled(nodeAddr, Port.TH1_IN.name, false)
            ControlMote.setCMPointEnabled(nodeAddr, Port.TH2_IN.name, false)

            if (configPoint.markers.contains("space")) {
                val supplyTempSensorTypePoint = haystack.readDefaultVal("point and supply and " +
                        "type and temp and equipRef == \"" + configPoint.equipRef + "\"").toInt()

                val roomTempSensorPoint: HashMap<Any, Any>? = haystack.readEntity(
                    "point and space and not type and temp and equipRef == \"" + configPoint.equipRef + "\"")

                if (value == SupplyTempSensor.THERMISTOR_1.ordinal || supplyTempSensorTypePoint == RoomTempSensor.THERMISTOR_1.ordinal) {
                    ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, true)
                    ControlMote.setCMPointEnabled(nodeAddr, Port.TH1_IN.name, true)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name, roomTempSensorPoint?.get("id").toString())
                }
                if (value == SupplyTempSensor.THERMISTOR_2.ordinal || supplyTempSensorTypePoint == RoomTempSensor.THERMISTOR_2.ordinal) {
                    ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, true)
                    ControlMote.setCMPointEnabled(nodeAddr, Port.TH2_IN.name, true)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name, roomTempSensorPoint?.get("id").toString())
                }
                if (value == RoomTempSensor.SENSOR_BUS_TEMPERATURE.ordinal) {
                    ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, true)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.SENSOR_RT.name, currentTemp?.get("id").toString())
                }

            } else {
                val roomTempSensorTypePoint = haystack.readDefaultVal("point and space and " +
                        "type and temp and equipRef == \"" + configPoint.equipRef + "\"").toInt()

                val supplyTempSensorPoint: HashMap<Any, Any>? = haystack.readEntity(
                    "point and supply and not type and temp and equipRef == \"" + configPoint.equipRef + "\"")

                if (value == SupplyTempSensor.THERMISTOR_1.ordinal || roomTempSensorTypePoint == RoomTempSensor.THERMISTOR_1.ordinal) {
                    ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, true)
                    ControlMote.setCMPointEnabled(nodeAddr, Port.TH1_IN.name, true)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.SENSOR_RT.name, supplyTempSensorPoint?.get("id").toString())
                }
                if (value == SupplyTempSensor.THERMISTOR_2.ordinal || roomTempSensorTypePoint == RoomTempSensor.THERMISTOR_2.ordinal) {
                    ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, true)
                    ControlMote.setCMPointEnabled(nodeAddr, Port.TH2_IN.name, true)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.SENSOR_RT.name, supplyTempSensorPoint?.get("id").toString())
                }

            }

        }

    }
}


