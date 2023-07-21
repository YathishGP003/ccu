package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.api.haystack.*
import a75f.io.api.haystack.Queries.Companion.ANALOG1_IN
import a75f.io.api.haystack.Queries.Companion.ANALOG1_OUT
import a75f.io.api.haystack.Queries.Companion.ANALOG2_IN
import a75f.io.api.haystack.Queries.Companion.ANALOG2_OUT
import a75f.io.api.haystack.Queries.Companion.ANALOG3_OUT
import a75f.io.api.haystack.Queries.Companion.ANALOG4_OUT
import a75f.io.api.haystack.Queries.Companion.IN
import a75f.io.api.haystack.Queries.Companion.OUT
import a75f.io.api.haystack.Tags.*
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuReconfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HpuReconfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.Pipe2Reconfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconReconfiguration
import a75f.io.logic.bo.haystack.device.DeviceUtil
import android.util.Log
import com.google.gson.JsonObject
import java.util.*

/**
 * Created by Manjunath K on 19-10-2021.
 */
class HyperStatSplitReconfigureUtil {

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
                Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Reconfiguration Updated : $configPointId -> $updatedConfigValue")

            } else {
                val equip = getEquip(configPoint.equipRef, haystack)
                val updatedConfigValue = msgObject.get("val").asDouble
                val configType = configType(configPoint.markers)
                val portType = portType(configPoint.markers)
                handleCreateReconfig(configPoint, updatedConfigValue, haystack, equip, configType!!, portType!!)
            }

        }


        // change in value configuration function
        fun updateConfigValues(msgObject: JsonObject, haystack: CCUHsApi, configPoint: Point) {
            val updatedConfigValue = msgObject.get("val").asDouble
            val configPointId = msgObject.get("id").asString
            haystack.writeDefaultValById(configPointId, updatedConfigValue)
            val equip = getEquip(configPoint.equipRef, haystack)
            val configType = configType(configPoint.markers)
            val portType = portType(configPoint.markers)
            updateAnalogActuatorType(configPoint, equip, haystack, portType!!, configType!!)
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Reconfiguration change Updated : $configPointId -> $updatedConfigValue")
            haystack.scheduleSync()
        }

        fun updateAssociationPoint(msgObject: JsonObject, associationPoint: Point, haystack: CCUHsApi) {
            val updatedConfigValue = msgObject.get("val").asDouble
            val whichConfig = configType(associationPoint.markers)
            val equip = getEquip(associationPoint.equipRef, haystack)
            val portType = portType(associationPoint.markers)
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Reconfiguration for Association Points $whichConfig $portType")
            handleAssociationConfig(whichConfig!!, portType!!, associationPoint, updatedConfigValue,equip)
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
            if (point.markers.contains("output")) {
                val analogOutTag = when (configType) {
                    ANALOG1_OUT -> "analog1"
                    ANALOG2_OUT -> "analog2"
                    ANALOG3_OUT -> "analog3"
                    ANALOG3_OUT -> "analog4"
                    else -> null
                }
                if (analogOutTag != null) {
                    val minPointValue = haystack.readDefaultVal(
                        "point and config and " +
                                "$analogOutTag and output and min and equipRef == \"${equip.id}\""
                    ).toInt()
                    val maxPointValue = haystack.readDefaultVal(
                        "point and config and " +
                                "$analogOutTag and output and max and equipRef == \"${equip.id}\""
                    ).toInt()

                    val pointType = "${minPointValue}-${maxPointValue}v"
                    Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "updateAnalogActuatorType: $pointType")
                    DeviceUtil.updatePhysicalPointType(equip.group.toInt(), portType.name, pointType)
                }
            }
        }

        // handle When we change the association
        private fun handleCreateReconfig(
            point: Point,
            updatedConfigValue: Double,
            haystack: CCUHsApi,
            equip: Equip,
            configType: String,
            portType: Port
        ) {
            // TODO: verify
            if (equip.markers.contains(CPUECON)) {
                CpuEconReconfiguration.updateConfiguration(
                    updatedConfigValue, equip, portType,
                )
            }
        }

        private fun handleAssociationConfig(
            configType: String,
            portType: Port,
            point: Point,
            updatedConfigValue: Double,
            equip: Equip,
        ) {
            // TODO: verify
            if (equip.markers.contains(CPUECON)) {
                CpuEconReconfiguration.configAssociationPoint(
                    portType, updatedConfigValue, equip
                )
            }
        }

        // Get Equip Points Util
         fun getEquipPointsUtil(equip: Equip, haystack: CCUHsApi): HyperStatSplitPointsUtil {
            return HyperStatSplitPointsUtil(
                profileName = CPUECON,
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
        fun readPointID(markers: String, equipRef: String, profileName: String, haystack: CCUHsApi): String? {
            val pointMap: HashMap<*, *> = haystack.read(
                "point and $markers and equipRef == \"$equipRef\""
            )
            return pointMap["id"] as String?
        }

        // Function to create Points
        fun createPoint(pointData: Point, pointsUtil: HyperStatSplitPointsUtil, defaultValue: Double): String {

            val pointId = pointsUtil.addPointToHaystack(pointData)
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON,
                   "Point name ${pointData.displayName} \n" +
                        "Point name ${pointData.shortDis} \n" +
                        " $pointId: ${pointData.markers} $defaultValue \n"
            )
            if (pointData.markers.contains("his")) {
                pointsUtil.addDefaultHisValueForPoint(pointId, defaultValue)
            }
            if (pointData.markers.contains("writable")) {
                pointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
            Log.i("CCU_HSSPLIT", "Read ${CCUHsApi.getInstance().readDefaultValById(pointId)}")
            return pointId
        }

        // Util function to get Equip
        fun getEquip(equipRef: String, haystack: CCUHsApi): Equip {
            val equipMap: HashMap<*, *> = haystack.readMapById(equipRef)
            return Equip.Builder().setHashMap(equipMap).build()
        }

        // Function to Read Default value
        fun readAssociationPointValue(
            markers: String,
            equipRef: String,
            profileName: String,
            haystack: CCUHsApi
        ): Double {
            return haystack.readDefaultVal(
                "point and association and config and $markers and equipRef == " +
                        "\"$equipRef\""
            )
        }

        // Function to get Config type
        private fun configType(markers: ArrayList<String>): String? {

            if (markers.contains(RELAY1)) return RELAY1
            if (markers.contains(RELAY2)) return RELAY2
            if (markers.contains(RELAY3)) return RELAY3
            if (markers.contains(RELAY4)) return RELAY4
            if (markers.contains(RELAY5)) return RELAY5
            if (markers.contains(RELAY6)) return RELAY6
            if (markers.contains(RELAY7)) return RELAY7
            if (markers.contains(RELAY8)) return RELAY8

            if (markers.contains(ANALOG1) && markers.contains(OUT)) return ANALOG1_OUT
            if (markers.contains(ANALOG2) && markers.contains(OUT)) return ANALOG2_OUT
            if (markers.contains(ANALOG3) && markers.contains(OUT)) return ANALOG3_OUT
            if (markers.contains(ANALOG4) && markers.contains(OUT)) return ANALOG4_OUT

            // TODO: verify
            if (markers.contains(UNIVERSAL1) && markers.contains(IN)) return UNIVERSAL1
            if (markers.contains(UNIVERSAL2) && markers.contains(IN)) return UNIVERSAL2
            if (markers.contains(UNIVERSAL3) && markers.contains(IN)) return UNIVERSAL3
            if (markers.contains(UNIVERSAL4) && markers.contains(IN)) return UNIVERSAL4
            if (markers.contains(UNIVERSAL5) && markers.contains(IN)) return UNIVERSAL5
            if (markers.contains(UNIVERSAL6) && markers.contains(IN)) return UNIVERSAL6
            if (markers.contains(UNIVERSAL7) && markers.contains(IN)) return UNIVERSAL7
            if (markers.contains(UNIVERSAL8) && markers.contains(IN)) return UNIVERSAL8

            return null
        }

        private fun portType(markers: ArrayList<String>): Port? {
            if (markers.contains(RELAY1)) return Port.RELAY_ONE
            if (markers.contains(RELAY2)) return Port.RELAY_TWO
            if (markers.contains(RELAY3)) return Port.RELAY_THREE
            if (markers.contains(RELAY4)) return Port.RELAY_FOUR
            if (markers.contains(RELAY5)) return Port.RELAY_FIVE
            if (markers.contains(RELAY6)) return Port.RELAY_SIX
            if (markers.contains(RELAY7)) return Port.RELAY_SEVEN
            if (markers.contains(RELAY8)) return Port.RELAY_EIGHT

            if (markers.contains(ANALOG1) && markers.contains(OUT)) return Port.ANALOG_OUT_ONE
            if (markers.contains(ANALOG2) && markers.contains(OUT)) return Port.ANALOG_OUT_TWO
            if (markers.contains(ANALOG3) && markers.contains(OUT)) return Port.ANALOG_OUT_THREE
            if (markers.contains(ANALOG4) && markers.contains(OUT)) return Port.ANALOG_OUT_FOUR

            if (markers.contains(UNIVERSAL1) && markers.contains(IN)) return Port.UNIVERSAL_IN_ONE
            if (markers.contains(UNIVERSAL2) && markers.contains(IN)) return Port.UNIVERSAL_IN_TWO
            if (markers.contains(UNIVERSAL3) && markers.contains(IN)) return Port.UNIVERSAL_IN_THREE
            if (markers.contains(UNIVERSAL4) && markers.contains(IN)) return Port.UNIVERSAL_IN_FOUR
            if (markers.contains(UNIVERSAL5) && markers.contains(IN)) return Port.UNIVERSAL_IN_FIVE
            if (markers.contains(UNIVERSAL6) && markers.contains(IN)) return Port.UNIVERSAL_IN_SIX
            if (markers.contains(UNIVERSAL7) && markers.contains(IN)) return Port.UNIVERSAL_IN_SEVEN
            if (markers.contains(UNIVERSAL8) && markers.contains(IN)) return Port.UNIVERSAL_IN_EIGHT

            return null
        }

    }

}