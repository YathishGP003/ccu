package a75f.io.logic.pubnub

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
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
            if (configPoint.markers.contains(Tags.TH1)) {
                if (value > 0) {
                    ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, true)
                    ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, false)
                    ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name, currentTemp?.get(
                            "id").toString())
                }

            } else if (configPoint.markers.contains(Tags.TH2)) {
                if (value > 0) {
                    ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, false)
                    ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, true)
                    ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, false)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name, currentTemp?.get(
                            "id").toString())
                }

            } else if (configPoint.markers.contains("main")) {
                if (value > 0) {
                    ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name, false)
                    ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name, false)
                    ControlMote.setPointEnabled(nodeAddr, Port.SENSOR_RT.name, true)
                    ControlMote.updatePhysicalPointRef(nodeAddr, Port.SENSOR_RT.name, currentTemp?.get(
                            "id").toString())
                }

            }
        }
    }
}


