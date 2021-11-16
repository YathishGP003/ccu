package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Queries.Companion.ANALOG1_IN
import a75f.io.api.haystack.Queries.Companion.ANALOG1_OUT
import a75f.io.api.haystack.Queries.Companion.ANALOG2_IN
import a75f.io.api.haystack.Queries.Companion.ANALOG2_OUT
import a75f.io.api.haystack.Queries.Companion.ANALOG3_OUT
import a75f.io.api.haystack.Queries.Companion.IN
import a75f.io.api.haystack.Queries.Companion.OUT
import a75f.io.api.haystack.Tags.*
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstat.cpu.CPUReconfiguration
import a75f.io.logic.bo.building.hyperstat.cpu.HyperStatPointsUtil
import android.util.Log
import com.google.gson.JsonObject
import java.util.*

/**
 * Created by Manjunath K on 19-10-2021.
 */
class HSReconfigureUtil {

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
                Log.i(L.TAG_CCU_HSCPU, "Reconfiguration Updated : $configPointId -> $updatedConfigValue")

            } else {
                val equip = getEquip(configPoint.equipRef, haystack)
                val updatedConfigValue = msgObject.get("val").asDouble
                val configPointId = msgObject.get("id").asString
                haystack.writeDefaultValById(configPointId, updatedConfigValue)

                val configType = configType(configPoint.markers)
                val portType = portType(configPoint.markers)
                Log.i(
                    L.TAG_CCU_HSCPU, "Reconfiguration Updated : \n" +
                            "$configPointId -> $updatedConfigValue \n" +
                            "configType $configType"
                )
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
            handleUpdateReconfig(configPoint, equip, haystack, portType!!, configType!!)
            Log.i(L.TAG_CCU_HSCPU, "Reconfiguration change Updated : $configPointId -> $updatedConfigValue")
            haystack.scheduleSync()
        }

        fun updateAssociationPoint(msgObject: JsonObject, associationPoint: Point) {
            val updatedConfigValue = msgObject.get("val").asDouble
            val whichConfig = configType(associationPoint.markers)
            val portType = portType(associationPoint.markers)
            Log.i(L.TAG_CCU_HSCPU, "Reconfiguration for Association Points $whichConfig $portType")
            handleAssociationReconfig(whichConfig!!, portType!!, associationPoint, updatedConfigValue)
        }


        // Handle when we enable Re-Config
        private fun handleUpdateReconfig(
            point: Point,
            equip: Equip,
            haystack: CCUHsApi,
            portType: Port,
            configType: String
        ) {

            // configuration hyperstat cpu
            if (point.markers.contains(CPU)) {
                CPUReconfiguration.updateAnalogActuatorType(
                    point, equip.id, equip.group.toInt(), haystack, portType, configType
                )
            }
            // todo handler for all other profiles like HPU 2pipe 4pipe
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
            if (point.markers.contains(CPU)) {
                CPUReconfiguration.configLogicalPoint(
                    updatedConfigValue, haystack, equip, point, configType, portType,
                )
            }

            // todo handler for all other profiles like HPU 2pipe 4pipe

        }

        private fun handleAssociationReconfig(
            configType: String,
            portType: Port,
            point: Point,
            updatedConfigValue: Double
        ) {
            if (point.markers.contains(CPU)) {
                CPUReconfiguration.configAssociationPoint(
                    configType, portType, updatedConfigValue, point
                )
            }

            // todo handler for all other profiles like HPU 2pipe 4pipe
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
        fun readPointID(markers: String, equipRef: String, profileName: String, haystack: CCUHsApi): String? {
            val pointMap: HashMap<*, *> = haystack.read(
                "point and hyperstat and $profileName and $markers and equipRef == \"$equipRef\""
            )
            return pointMap["id"] as String?
        }

        // Function to create Points
        fun createPoint(pointData: Point, pointsUtil: HyperStatPointsUtil, defaultValue: Double): String {

            val pointId = pointsUtil.addPointToHaystack(pointData)
            Log.i(L.TAG_CCU_HSCPU,
                   "Point name ${pointData.displayName} \n" +
                        "Point name ${pointData.shortDis} \n" +
                        " $pointId: ${pointData.markers} $defaultValue \n"
            )
            if (pointData.markers.contains("his")) {
                pointsUtil.addDefaultHisValueForPoint(pointId, defaultValue)
            }
            pointsUtil.addDefaultValueForPoint(pointId, defaultValue)

            Log.i("CCU_HSC", "Read ${CCUHsApi.getInstance().readDefaultValById(pointId)}")
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
                "point and hyperstat and association and $profileName and config and $markers and equipRef == " +
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

            if (markers.contains(ANALOG1) && markers.contains(OUT)) return ANALOG1_OUT
            if (markers.contains(ANALOG2) && markers.contains(OUT)) return ANALOG2_OUT
            if (markers.contains(ANALOG3) && markers.contains(OUT)) return ANALOG3_OUT

            if (markers.contains(ANALOG1) && markers.contains(IN)) return ANALOG1_IN
            if (markers.contains(ANALOG2) && markers.contains(IN)) return ANALOG2_IN

            if (markers.contains(TH1)) return TH1
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


            if (markers.contains(TH1)) return Port.TH1_IN
            if (markers.contains(TH2)) return Port.TH2_IN

            return null
        }

    }

}