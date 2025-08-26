package a75f.io.sanity.cases

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.util.PointsUtil
import a75f.io.logger.CcuLog
import a75f.io.sanity.framework.SANITTY_TAG
import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityResultSeverity
import org.projecthaystack.HDict
import java.util.function.Consumer

class SchedulableAndTunersCheck : SanityCase {
    val VAV_COOLING_DB: Double = 2.0 //Default deadband value based on dual temp diff 70 and 74 ((74-70)/2.0)
    val DEFAULT_VAL_LEVEL: Int = 17
    val SYSTEM_DEFAULT_VAL_LEVEL: Int = 17
    val UI_DEFAULT_VAL_LEVEL: Int = 8
    val SYSTEM_BUILDING_VAL_LEVEL: Int = 16
    val ZONE_UNOCCUPIED_SETBACK: Double = 5.0
    val ZONE_HEATING_USERLIMIT_MIN: Double = 67.0
    val ZONE_HEATING_USERLIMIT_MAX: Double = 72.0
    val ZONE_COOLING_USERLIMIT_MIN: Double = 72.0
    val ZONE_COOLING_USERLIMIT_MAX: Double = 77.0
    var pointsFixed = 0

    override fun getName(): String = "SchedulableAndTunersCheck"

    fun createMissingScheduleAblePoints(ccuHsApi: CCUHsApi) {
        CcuLog.i("CCU_SCHEDULABLE", "createMissingScheduleAblePoints")
        val rooms: List<HashMap<Any?, Any>> = ccuHsApi.readAllEntities("room")
        rooms.forEach(Consumer { room: HashMap<Any?, Any> ->
            var missingSchedulableLimits = false
            val scheduleAbleLimits: MutableList<ArrayList<HashMap<Any, Any>>> =
                ArrayList()
            scheduleAbleLimits.add(
                ccuHsApi.readAllEntities(
                    "schedulable and roomRef ==\""
                            + room["id"].toString() + "\""
                )
            )
            findMissingSchedulableLimits(
                ccuHsApi.readAllEntities(
                    "schedulable and roomRef ==\""
                            + room["id"].toString() + "\""
                ), room["id"].toString(), room["floorRef"].toString(), ccuHsApi
            )
        })
    }

    private fun getSchedulablePrefixDis(
        roomMap: java.util.HashMap<Any, Any>,
        ccuHsApi: CCUHsApi
    ): String {
        val floor = ccuHsApi.readMapById(roomMap["floorRef"].toString())
        val siteMap = ccuHsApi.readEntity(Tags.SITE)
        val siteDis = siteMap["dis"].toString()
        return siteDis + "-" + floor["dis"].toString() + "-" + roomMap["dis"]
    }
    /**
     * Finds and creates missing schedulable limit points for a room.
     *
     * This function checks for various types of schedulable limits (deadbands, setbacks, min/max limits)
     * for both heating and cooling systems. If any required limits are missing from the provided list,
     * they are created using the appropriate creation functions.
     *
     * @param scheduleAbleLimits List of existing schedulable limits to check against
     * @param roomRef The reference ID of the room to check and create limits for
     * @param floorRef The reference ID of the floor containing the room
     * @param ccuHsApi The CCUHsApi instance used for reading and creating points
     *
     * The function checks for the following limit types:
     * - Heating deadband
     * - Cooling deadband
     * - Unoccupied setback
     * - Heating minimum limit
     * - Heating maximum limit
     * - Cooling minimum limit
     * - Cooling maximum limit
     *
     */
    private fun findMissingSchedulableLimits(
        scheduleAbleLimits: ArrayList<HashMap<Any, Any>>,
        roomRef: String,
        floorRef: String,
        ccuHsApi: CCUHsApi
    ){
        CcuLog.i("CCU_SCHEDULABLE", "findMissingSchedulableLimits roomRef $roomRef")

        val roomMap = ccuHsApi.readMapById(roomRef)
        val disName: String = getSchedulablePrefixDis(roomMap, ccuHsApi)

        if (notContainsSchedulableLimits(scheduleAbleLimits, "heating", "deadband")) {
            CcuLog.i("CCU_SCHEDULABLE", "create Heating deadband")
            createSchedulableDeadband(roomRef, ccuHsApi, roomMap, "heating", floorRef, disName)
            pointsFixed++
        }

        if (notContainsSchedulableLimits(scheduleAbleLimits, "cooling", "deadband")) {
            CcuLog.i("CCU_SCHEDULABLE", "create cooling deadband")
            createSchedulableDeadband(roomRef, ccuHsApi, roomMap, "cooling", floorRef, disName)
            pointsFixed++
        }

        if (notContainsSchedulableLimits(scheduleAbleLimits, "unoccupied", "setback")) {
            CcuLog.i("CCU_SCHEDULABLE", "create unoccupied setback")
            createUnOccupiedSetBackPoint(roomRef, ccuHsApi, roomMap, floorRef, disName)
            pointsFixed++
        }

        if (notContainsSchedulableLimits(scheduleAbleLimits, "heating", "min")) {
            CcuLog.i("CCU_SCHEDULABLE", "create Heating min")
            createHeatingLimitMinPoint(roomRef, ccuHsApi, roomMap, floorRef, disName)
            pointsFixed++
        }

        if (notContainsSchedulableLimits(scheduleAbleLimits, "heating", "max")) {
            CcuLog.i("CCU_SCHEDULABLE", "create Heating max")
            createHeatingLimitMaxPoint(roomRef, ccuHsApi, roomMap, floorRef, disName)
            pointsFixed++
        }

        if (notContainsSchedulableLimits(scheduleAbleLimits, "cooling", "max")) {
            CcuLog.i("CCU_SCHEDULABLE", "create cooling max")
            createCoolingLimitMaxPoint(roomRef, ccuHsApi, roomMap, floorRef, disName)
            pointsFixed++
        }

        if (notContainsSchedulableLimits(scheduleAbleLimits, "cooling", "min")) {
            CcuLog.i("CCU_SCHEDULABLE", "create cooling min")
            createCoolingLimitMinPoint(roomRef, ccuHsApi, roomMap, floorRef, disName)
            pointsFixed++
        }
    }

    private fun createCoolingLimitMaxPoint(
        roomRef: String, ccuHsApi: CCUHsApi, roomMap: HashMap<Any, Any>,
        floorRef: String, prefixDis: String
    ) {
        val coolingUserLimitMax = Point.Builder()
            .setDisplayName("$prefixDis-coolingUserLimitMax")
            .setSiteRef(roomMap["siteRef"].toString())
            .setHisInterpolate("cov")
            .addMarker("schedulable").addMarker("writable").addMarker("his")
            .addMarker("cooling").addMarker("user").addMarker("limit").addMarker("max")
            .addMarker("sp")
            .setMinVal("50").setMaxVal("100").setIncrementVal("1").addMarker("cur")
            .setUnit("\u00B0F")
            .addMarker("zone").setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(ccuHsApi.timeZone).build()
        val coolingUserLimitMaxId = ccuHsApi.addPoint(coolingUserLimitMax)
        ccuHsApi.writePointForCcuUser(
            coolingUserLimitMaxId,
            SYSTEM_DEFAULT_VAL_LEVEL,
            ZONE_COOLING_USERLIMIT_MAX,
            0
        )
        ccuHsApi.writeHisValById(coolingUserLimitMaxId, ZONE_COOLING_USERLIMIT_MAX)
        val buildingPoint = ccuHsApi.readEntity(
            "schedulable and cooling and user " +
                    "and limit and max and default"
        )
        if (buildingPoint != null && !buildingPoint.isEmpty()) {
            val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
            if (pointVal > 0) ccuHsApi.writePointForCcuUser(
                coolingUserLimitMaxId, SYSTEM_BUILDING_VAL_LEVEL,
                pointVal, 0
            )
        }
    }

    private fun createCoolingLimitMinPoint(
        roomRef: String, ccuHsApi: CCUHsApi, roomMap: HashMap<Any, Any>,
        floorRef: String, prefixDis: String
    ) {
        val coolingUserLimitMin = Point.Builder()
            .setDisplayName("$prefixDis-coolingUserLimitMin")
            .setSiteRef(roomMap["siteRef"].toString())
            .setHisInterpolate("cov")
            .addMarker("writable").addMarker("his").setMinVal("70").setMaxVal("77")
            .setIncrementVal("1").addMarker("schedulable").addMarker("cur")
            .addMarker("cooling").addMarker("user").addMarker("limit").addMarker("min")
            .addMarker("sp")
            .setMinVal("50").setMaxVal("100").setIncrementVal("1")
            .setUnit("\u00B0F")
            .addMarker("zone").setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(ccuHsApi.timeZone).build()
        val coolingUserLimitMinId = ccuHsApi.addPoint(coolingUserLimitMin)
        ccuHsApi.writePointForCcuUser(
            coolingUserLimitMinId,
            SYSTEM_DEFAULT_VAL_LEVEL,
            ZONE_COOLING_USERLIMIT_MIN,
            0
        )
        ccuHsApi.writeHisValById(coolingUserLimitMinId, ZONE_COOLING_USERLIMIT_MIN)
        val buildingPoint = ccuHsApi.readEntity(
            "schedulable and cooling and user " +
                    "and limit and min and default"
        )
        if (buildingPoint != null && !buildingPoint.isEmpty()) {
            val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
            if (pointVal > 0) ccuHsApi.writePointForCcuUser(
                coolingUserLimitMinId, SYSTEM_BUILDING_VAL_LEVEL,
                pointVal, 0
            )
        }
    }

    private fun createHeatingLimitMaxPoint(
        roomRef: String, ccuHsApi: CCUHsApi,
        roomMap: HashMap<Any, Any>, floorRef: String, prefixDis: String
    ) {
        val heatingUserLimitMax = Point.Builder()
            .setDisplayName("$prefixDis-heatingUserLimitMax")
            .setSiteRef(roomMap["siteRef"].toString())
            .setHisInterpolate("cov")
            .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
            .addMarker("heating").addMarker("user").addMarker("limit").addMarker("max")
            .addMarker("sp")
            .setMinVal("50").setMaxVal("100").setIncrementVal("1")
            .setUnit("\u00B0F")
            .addMarker("zone").setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(ccuHsApi.timeZone).build()
        val heatingUserLimitMaxId = ccuHsApi.addPoint(heatingUserLimitMax)
        ccuHsApi.writePointForCcuUser(
            heatingUserLimitMaxId,
            SYSTEM_DEFAULT_VAL_LEVEL,
            ZONE_HEATING_USERLIMIT_MAX,
            0
        )
        ccuHsApi.writeHisValById(heatingUserLimitMaxId, ZONE_HEATING_USERLIMIT_MAX)
        val buildingPoint = ccuHsApi.readEntity(
            "schedulable and heating and user " +
                    "and limit and max and default"
        )
        if (buildingPoint != null && !buildingPoint.isEmpty()) {
            val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
            if (pointVal > 0) ccuHsApi.writePointForCcuUser(
                heatingUserLimitMaxId, SYSTEM_BUILDING_VAL_LEVEL,
                pointVal, 0
            )
        }
    }

    private fun createHeatingLimitMinPoint(
        roomRef: String, ccuHsApi: CCUHsApi,
        roomMap: HashMap<Any, Any>, floorRef: String, prefixDis: String
    ) {
        val heatingUserLimitMin = Point.Builder()
            .setDisplayName("$prefixDis-heatingUserLimitMin")
            .setSiteRef(roomMap["siteRef"].toString())
            .setHisInterpolate("cov")
            .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
            .addMarker("heating").addMarker("user").addMarker("limit").addMarker("min")
            .addMarker("sp")
            .setMinVal("50").setMaxVal("100").setIncrementVal("1")
            .setUnit("\u00B0F")
            .addMarker("zone").setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(ccuHsApi.timeZone).build()
        val heatingUserLimitMinId = ccuHsApi.addPoint(heatingUserLimitMin)
        ccuHsApi.writePointForCcuUser(
            heatingUserLimitMinId,
            SYSTEM_DEFAULT_VAL_LEVEL,
            ZONE_HEATING_USERLIMIT_MIN,
            0
        )
        ccuHsApi.writeHisValById(heatingUserLimitMinId, ZONE_HEATING_USERLIMIT_MIN)
        val buildingPoint = ccuHsApi.readEntity(
            "schedulable and heating and user " +
                    "and limit and min and default"
        )
        if (buildingPoint != null && !buildingPoint.isEmpty()) {
            val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
            if (pointVal > 0) ccuHsApi.writePointForCcuUser(
                heatingUserLimitMinId, SYSTEM_BUILDING_VAL_LEVEL,
                pointVal, 0
            )
        }
    }

    private fun createUnOccupiedSetBackPoint(
        roomRef: String, ccuHsApi: CCUHsApi, roomMap: HashMap<Any, Any>,
        floorRef: String, prefixDis: String
    ) {
        val unoccupiedZoneSetback = Point.Builder()
            .setDisplayName("$prefixDis-unoccupiedZoneSetback")
            .setSiteRef(roomMap["siteRef"].toString())
            .setHisInterpolate("cov")
            .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
            .addMarker("unoccupied").addMarker("setback").addMarker("sp")
            .setMinVal("0").setMaxVal("20").setIncrementVal("1")
            .setUnit("\u00B0F")
            .addMarker("zone").setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(ccuHsApi.timeZone).build()
        val unoccupiedZoneSetbackId = ccuHsApi.addPoint(unoccupiedZoneSetback)
        ccuHsApi.writePointForCcuUser(
            unoccupiedZoneSetbackId,
            DEFAULT_VAL_LEVEL,
            ZONE_UNOCCUPIED_SETBACK,
            0
        )
        ccuHsApi.writeHisValById(unoccupiedZoneSetbackId, ZONE_UNOCCUPIED_SETBACK)
        val buildingPoint = ccuHsApi.readEntity(
            "schedulable and unoccupied and setback " +
                    "and default"
        )
        if (buildingPoint != null && !buildingPoint.isEmpty()) {
            val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
            if (pointVal > 0) ccuHsApi.writePointForCcuUser(
                unoccupiedZoneSetbackId, SYSTEM_BUILDING_VAL_LEVEL,
                pointVal, 0
            )
        }
    }

    private fun createSchedulableDeadband(
        roomRef: String, ccuHsApi: CCUHsApi,
        roomMap: HashMap<Any, Any>, deadBand: String, floorRef: String, prefixDis: String
    ) {
        val deadBandPoint = Point.Builder()
            .setDisplayName(prefixDis + "-" + deadBand + "Deadband")
            .setSiteRef(roomMap["siteRef"].toString())
            .setHisInterpolate("cov")
            .addMarker("writable")
            .addMarker("his")
            .addMarker(deadBand).addMarker("deadband").addMarker("base")
            .addMarker("sp").addMarker("schedulable").addMarker("cur")
            .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5")
            .setUnit("\u00B0F")
            .addMarker("zone").setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(ccuHsApi.timeZone).build()
        val deadBandId = CCUHsApi.getInstance().addPoint(deadBandPoint)
        ccuHsApi.writePointForCcuUser(
            deadBandId,
            DEFAULT_VAL_LEVEL,
            VAV_COOLING_DB,
            0
        )
        ccuHsApi.writeHisValById(deadBandId, VAV_COOLING_DB)
        val buildingPoint = ccuHsApi.readEntity(
            "schedulable and " +
                    deadBand + " and deadband and default"
        )
        if (buildingPoint != null && !buildingPoint.isEmpty()) {
            val pointVal = HSUtil.getPriorityLevelVal(buildingPoint["id"].toString(), 16)
            if (pointVal > 0) ccuHsApi.writePointForCcuUser(
                deadBandId, SYSTEM_BUILDING_VAL_LEVEL,
                pointVal, 0
            )
        }
    }
    private fun notContainsSchedulableLimits(
        listOfMaps: java.util.ArrayList<java.util.HashMap<Any, Any>>,
        tag1: String, tag2: String
    ): Boolean {
        for (map in listOfMaps) {
            if (map.containsKey(tag1) && map.containsKey(tag2)) {
                return false
            }
        }
        return true
    }

    /**
     * Fixes corrupt point values by checking and restoring default values when necessary.
     *
     * This function:
     * 1. Processes each point in the provided list
     * 2. Checks if the point has a valid value at level 17 (index 16) of its value hierarchy
     * 3. For points missing valid values, overwrites them with their default values
     * 4. Maintains a count of successfully fixed points
     *
     * @param pointList List of HDict objects representing points to be checked and potentially fixed.
     *                  Each HDict must contain the necessary fields to construct a valid Point object.
     *
     */
    fun fixCorruptPointValue(pointList: List<HDict>) {
        for (pointDict in pointList) {
            val point = Point.Builder().setHDict(pointDict).build()
            val values = hayStack.readPoint(point.id)
            // Check if the point has a value at level 17
            val valMapAtLevel17 = values[16]
            if(valMapAtLevel17.containsKey("val")) {
                CcuLog.i(SANITTY_TAG, "Point value at level 17: ${point.displayName} val ${valMapAtLevel17.get("val")}")
            } else {
                CcuLog.e(SANITTY_TAG, "Point value at level 17 not found for: ${point.displayName}")
                PointsUtil(hayStack).overWriteDefaultValGeneric(point)
                pointsFixed++
            }
        }
    }

    override fun execute(): Boolean {
//         hayStack.clearPointArrayLevel("4fc2ca9e-7645-4466-a6cb-b484422a3478", 17, false) // For testing purposes
//        hayStack.deleteWritablePoint("030d7ac8-9b92-4be5-a5b2-94be9cef1b9c") // For testing heatingUserLimitMin
        pointsFixed = 0
        // Ensure all rooms have the necessary schedulable points. If not then create it
        createMissingScheduleAblePoints(CCUHsApi.getInstance())
        // 1: Building tuner and schedulable points
        var pointList = CCUHsApi.getInstance().readAllHDictByQuery("schedulable and domainName")
        fixCorruptPointValue(pointList)

        // 2: Zone tuner points
        pointList = CCUHsApi.getInstance().readAllHDictByQuery("tuner and domainName and equipRef")
        fixCorruptPointValue(pointList)

        if(pointsFixed > 0) {
            return false
        } else {
            return true
        }
    }

    override fun report(): String {
        return "No issues found with schedulable points"
    }

    override fun correct(): Boolean {
        // Placeholder for correction logic
        return true
    }

    override fun getSeverity(): SanityResultSeverity {
        return SanityResultSeverity.HIGH
    }

    override fun getDescription(): String {
        if(pointsFixed > 0) {
            return "Found and fixed $pointsFixed corrupt schedulable points."
        }
        return "No corrupt schedulable/tuner points found."
    }
}