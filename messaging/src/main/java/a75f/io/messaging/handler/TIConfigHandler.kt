package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
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

            if (configPoint.markers.contains("space")) {
                val roomTempSensorPoint: HashMap<Any, Any>? = haystack.readEntity(
                    "point and space and not type and temp and equipRef == \"" + configPoint.equipRef + "\"")

                when (value) {
                    0 -> {
                        ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, true)
                        ControlMote.updatePhysicalPointRef(nodeAddr, Port.SENSOR_RT.name, currentTemp?.get("id").toString())
                    }
                    1 -> {
                        ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, true)
                        ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)
                        ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name, roomTempSensorPoint?.get("id").toString())
                    }
                    2 -> {
                        ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, true)
                        ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)
                        ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name, roomTempSensorPoint?.get("id").toString())
                    }
                }
            } else if (configPoint.markers.contains("supply")) {
                val supplyTempSensorPoint: HashMap<Any, Any>? = haystack.readEntity(
                    "point and supply and not type and temp and equipRef == \"" + configPoint.equipRef + "\"")

                when (value) {
                    0 -> {
                        ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)
                    }
                    1 -> {
                        ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, true)
                        ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)
                        ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name, supplyTempSensorPoint?.get("id").toString())
                    }
                    2 -> {
                        ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, false)
                        ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, true)
                        ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)
                        ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name, supplyTempSensorPoint?.get("id").toString())
                    }
                }
            }
        }

    }
}


