package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Queries.Companion.ANALOG1_IN
import a75f.io.api.haystack.Queries.Companion.ANALOG1_OUT
import a75f.io.api.haystack.Queries.Companion.ANALOG2_IN
import a75f.io.api.haystack.Queries.Companion.ANALOG2_OUT
import a75f.io.api.haystack.Queries.Companion.ANALOG3_OUT
import a75f.io.api.haystack.Queries.Companion.IN
import a75f.io.api.haystack.Queries.Companion.OUT
import a75f.io.api.haystack.Tags.AIR
import a75f.io.api.haystack.Tags.ANALOG1
import a75f.io.api.haystack.Tags.ANALOG2
import a75f.io.api.haystack.Tags.ANALOG3
import a75f.io.api.haystack.Tags.CMD
import a75f.io.api.haystack.Tags.CONFIG
import a75f.io.api.haystack.Tags.CPU
import a75f.io.api.haystack.Tags.DISCHARGE
import a75f.io.api.haystack.Tags.HPU
import a75f.io.api.haystack.Tags.PIPE2
import a75f.io.api.haystack.Tags.RELAY1
import a75f.io.api.haystack.Tags.RELAY2
import a75f.io.api.haystack.Tags.RELAY3
import a75f.io.api.haystack.Tags.RELAY4
import a75f.io.api.haystack.Tags.RELAY5
import a75f.io.api.haystack.Tags.RELAY6
import a75f.io.api.haystack.Tags.TEMP
import a75f.io.api.haystack.Tags.TH1
import a75f.io.api.haystack.Tags.TH2
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuReconfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HpuReconfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.Pipe2Reconfiguration
import a75f.io.logic.bo.haystack.device.DeviceUtil
import com.google.gson.JsonObject

/**
 * Created by Manjunath K on 19-10-2021.
 */
class HyperStatReconfigureUtil {

    companion object {

        // Reconfiguration functions

        // Function which updates the config points
        fun updateConfigPoint(msgObject: JsonObject, configPoint: Point, haystack: CCUHsApi) {

            // Common for all the profiles
            if ((configPoint.markers.contains("auto") && configPoint.markers.contains("forced"))
                || (configPoint.markers.contains("auto") && configPoint.markers.contains("away"))
                || (configPoint.markers.contains("offset"))
            ) {
                val updatedConfigValue = msgObject.get("val").asDouble
                val configPointId = msgObject.get("id").asString
                haystack.writeDefaultValById(configPointId, updatedConfigValue)
                CcuLog.i(L.TAG_CCU_HSCPU, "Reconfiguration Updated : $configPointId -> $updatedConfigValue")

            } else {
                val equip = getEquip(configPoint.equipRef, haystack)
                val updatedConfigValue = msgObject.get("val").asDouble
                val portType = portType(configPoint.markers)
                handleCreateReconfig(updatedConfigValue, equip, portType!!)
            }

        }


        // change in value configuration function
        fun updateConfigValues(msgObject: JsonObject, haystack: CCUHsApi, configPoint: Point) {
            val updatedConfigValue = msgObject.get("val").asDouble
            val configPointId = msgObject.get("id").asString
            val level = msgObject.get("level").asInt
            val duration = if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null)
            msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
            val who = msgObject.get("who").asString
            haystack.writePointLocal(configPointId, level, who, updatedConfigValue, duration)
            val equip = getEquip(configPoint.equipRef, haystack)
            val configType = configType(configPoint.markers)
            val portType = portType(configPoint.markers)
            if(configType != null && portType != null) {
                updateAnalogActuatorType(configPoint, equip, haystack, portType, configType)
            }
            CcuLog.i(L.TAG_CCU_HSCPU, "Reconfiguration change Updated : $configPointId -> $updatedConfigValue")
            haystack.scheduleSync()
        }

        fun updateAssociationPoint(msgObject: JsonObject, associationPoint: Point, haystack: CCUHsApi) {
            val updatedConfigValue = msgObject.get("val").asDouble
            val whichConfig = configType(associationPoint.markers)
            val equip = getEquip(associationPoint.equipRef, haystack)
            val portType = portType(associationPoint.markers)
            CcuLog.i(L.TAG_CCU_HSCPU, "Reconfiguration for Association Points $whichConfig $portType")
            handleAssociationConfig(portType!!, updatedConfigValue, equip)
        }


        // If Configuration is changed for any of the analog out configuration we need
        // We need to update the analog actuator types
        // Based on actuator type analog voltage signals are decided
        private fun updateAnalogActuatorType(
            point: Point,
            equip: Equip,
            haystack: CCUHsApi,
            portType: Port,
            configType: String
        ) {
        // It is not to any profile specific it works for all the profiles (HS CPU,HPU,PIPE2,PIPE4)
            if (point.markers.contains("output")) {
                val analogOutTag = when (configType) {
                    ANALOG1_OUT -> "analog1"
                    ANALOG2_OUT -> "analog2"
                    ANALOG3_OUT -> "analog3"
                    else -> null
                }
                if (analogOutTag != null) {

                    val minPointValQuery = "point and config and " +
                            "$analogOutTag and output and min and equipRef == \"${equip.id}\""

                    val maxPointValQuery = "point and config and " +
                            "$analogOutTag and output and max and equipRef == \"${equip.id}\""
                    var minPointValue = 0
                    var maxPointValue = 10

                    if (haystack.readEntity(minPointValQuery).isNotEmpty()) {
                         minPointValue = haystack.readDefaultVal(minPointValQuery).toInt()
                    }
                    if (haystack.readEntity(maxPointValQuery).isNotEmpty()) {
                         maxPointValue = haystack.readDefaultVal(maxPointValQuery).toInt()
                    }

                    val pointType = "${minPointValue}-${maxPointValue}v"
                    CcuLog.i(L.TAG_CCU_HSCPU, "updateAnalogActuatorType: $pointType")
                    DeviceUtil.updatePhysicalPointType(equip.group.toInt(), portType.name, pointType)
                }
            }
        }

        // handle When we change the association
        private fun handleCreateReconfig(
                updatedConfigValue: Double,
                equip: Equip,
                portType: Port
        ) {
            if (equip.markers.contains(CPU)) {
                CpuReconfiguration.updateConfiguration(
                    updatedConfigValue, equip, portType,
                )
            }

            if (equip.markers.contains(PIPE2)) {
                Pipe2Reconfiguration.updateConfiguration(
                    updatedConfigValue, equip, portType,
                )
            }
            if (equip.markers.contains(HPU)) {
                HpuReconfiguration.updateConfiguration(
                    updatedConfigValue, equip, portType,
                )
            }
        }

        private fun handleAssociationConfig(
                portType: Port,
                updatedConfigValue: Double,
                equip: Equip,
        ) {
            if (equip.markers.contains(CPU)) {
                CpuReconfiguration.configAssociationPoint(
                    portType, updatedConfigValue, equip
                )
            }
            if (equip.markers.contains(PIPE2)) {
                Pipe2Reconfiguration.configAssociationPoint(
                    portType, updatedConfigValue, equip
                )
            }
            if (equip.markers.contains(HPU)) {
                HpuReconfiguration.configAssociationPoint(
                    portType, updatedConfigValue, equip
                )
            }

        }

        // Get Equip Points Util
         fun getEquipPointsUtil(equip: Equip, haystack: CCUHsApi): HyperStatPointsUtil {
            return HyperStatPointsUtil(
                profileName = CPU,
                equipRef = equip.id,
                floorRef = equip.floorRef,
                roomRef = equip.roomRef,
                siteRef = equip.siteRef,
                tz = equip.tz,
                nodeAddress = equip.tz,
                equipDis = equip.displayName,
                hayStackAPI = haystack
            )
        }

        // Function to Read point id
        fun readPointID(markers: String, equipRef: String, haystack: CCUHsApi): String? {
            val pointMap: HashMap<*, *> = haystack.read(
                "point and $markers and equipRef == \"$equipRef\""
            )
            return pointMap["id"] as String?
        }

        // Util function to get Equip
        fun getEquip(equipRef: String, haystack: CCUHsApi): Equip {
            val equipMap: HashMap<*, *> = haystack.readMapById(equipRef)
            return Equip.Builder().setHashMap(equipMap).build()
        }

        // Function to get Config type
        private fun configType(markers: ArrayList<String>): String? {

            if (markers.contains(RELAY1)) return RELAY1
            if (markers.contains(RELAY2)) return RELAY2
            if (markers.contains(RELAY3)) return RELAY3
            if (markers.contains(RELAY4)) return RELAY4
            if (markers.contains(RELAY5)) return RELAY5
            if (markers.contains(RELAY6)) return RELAY6

            if (markers.contains(ANALOG1) && markers.contains(OUT)) return ANALOG1_OUT
            if (markers.contains(ANALOG2) && markers.contains(OUT)) return ANALOG2_OUT
            if (markers.contains(ANALOG3) && markers.contains(OUT)) return ANALOG3_OUT

            if (markers.contains(ANALOG1) && markers.contains(IN)) return ANALOG1_IN
            if (markers.contains(ANALOG2) && markers.contains(IN)) return ANALOG2_IN

            if (isAirflowConfig(markers)) return TH1
            if (markers.contains(TH2)) return TH2
            return null
        }

        private fun portType(markers: ArrayList<String>): Port? {
            if (markers.contains(RELAY1)) return Port.RELAY_ONE
            if (markers.contains(RELAY2)) return Port.RELAY_TWO
            if (markers.contains(RELAY3)) return Port.RELAY_THREE
            if (markers.contains(RELAY4)) return Port.RELAY_FOUR
            if (markers.contains(RELAY5)) return Port.RELAY_FIVE
            if (markers.contains(RELAY6)) return Port.RELAY_SIX

            if (markers.contains(ANALOG1) && markers.contains(OUT)) return Port.ANALOG_OUT_ONE
            if (markers.contains(ANALOG2) && markers.contains(OUT)) return Port.ANALOG_OUT_TWO
            if (markers.contains(ANALOG3) && markers.contains(OUT)) return Port.ANALOG_OUT_THREE

            if (markers.contains(ANALOG1) && markers.contains(IN)) return Port.ANALOG_IN_ONE
            if (markers.contains(ANALOG2) && markers.contains(IN)) return Port.ANALOG_IN_TWO


            if (isAirflowConfig(markers)) return Port.TH1_IN
            if (markers.contains(TH2)) return Port.TH2_IN

            return null
        }

        private fun isAirflowConfig(markers: ArrayList<String>): Boolean{
            return (markers.contains(TEMP) && markers.contains(CMD) && markers.contains(CONFIG)
                    && markers.contains(AIR) && markers.contains(DISCHARGE))
        }

    }

}