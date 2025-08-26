package a75f.io.sanity.cases

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.Queries
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.util.hayStack
import a75f.io.logger.CcuLog
import a75f.io.sanity.framework.SANITTY_TAG
import a75f.io.sanity.framework.SanityCase
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HList
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import java.util.UUID

enum class ScheduleGroup(val group: String) {
    SEVEN_DAY("7 day"), WEEKDAY_SATURDAY_SUNDAY("Weekday+Saturday+Sunday"),
    WEEKDAY_WEEKEND("Weekday+Weekend"), EVERYDAY("Everyday");
}
enum class ScheduleType {
    BUILDING, ZONE, NAMED
}

class ScheduleRefValidation : SanityCase {
    var DEFAULT_COOLING_TEMP: Double = 74.0
    var DEFAULT_HEATING_TEMP: Double = 70.0
    var issueFound = false
    override fun getName(): String = "ScheduleRefValidation"
    private fun getZoneScheduablePoint(query: String, roomRef: String): Double {
        return CCUHsApi.getInstance().readPointPriorityValByQuery("$query \"$roomRef\"")
    }
    private fun getDefaultForDay(day: Int, zoneId: String?): HDict {
        val hDictDay: HDictBuilder = HDictBuilder()
            .add("day", HNum.make(day))
            .add("sthh", HNum.make(8))
            .add("stmm", HNum.make(0))
            .add("ethh", HNum.make(17))
            .add("etmm", HNum.make(30))
            .add("coolVal", HNum.make(DEFAULT_COOLING_TEMP))
            .add("heatVal", HNum.make(DEFAULT_HEATING_TEMP))
        if (zoneId != null) {
            hDictDay.add(
                Tags.HEATING_USER_LIMIT_MIN, getZoneScheduablePoint(
                    Queries.ZONE_HEATING_USER_LIMIT_MIN,
                    zoneId
                )
            )
            hDictDay.add(
                Tags.HEATING_USER_LIMIT_MAX, getZoneScheduablePoint(
                    Queries.ZONE_HEATING_USER_LIMIT_MAX,
                    zoneId
                )
            )
            hDictDay.add(
                Tags.COOLING_USER_LIMIT_MIN, getZoneScheduablePoint(
                    Queries.ZONE_COOLING_USER_LIMIT_MIN,
                    zoneId
                )
            )
            hDictDay.add(
                Tags.COOLING_USER_LIMIT_MAX, getZoneScheduablePoint(
                    Queries.ZONE_COOLING_USER_LIMIT_MAX,
                    zoneId
                )
            )
            hDictDay.add(
                Tags.COOLING_DEADBAND,
                getZoneScheduablePoint(
                    Queries.ZONE_COOLING_DEADBAND,
                    zoneId
                )
            )
            hDictDay.add(
                Tags.HEATING_DEADBAND,
                getZoneScheduablePoint(
                    Queries.ZONE_HEATING_DEADBAND,
                    zoneId
                )
            )
        }
        return hDictDay.toDict()
    }
    private fun generateDefaultZoneSchedule(zoneId: String?): String {
        val siteId = CCUHsApi.getInstance().siteIdRef

        val days = arrayOfNulls<HDict>(7)

        days[0] = getDefaultForDay(DAYS.MONDAY.ordinal, zoneId)
        days[1] = getDefaultForDay(DAYS.TUESDAY.ordinal, zoneId)
        days[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal, zoneId)
        days[3] = getDefaultForDay(DAYS.THURSDAY.ordinal, zoneId)
        days[4] = getDefaultForDay(DAYS.FRIDAY.ordinal, zoneId)
        days[5] = getDefaultForDay(DAYS.SATURDAY.ordinal, zoneId)
        days[6] = getDefaultForDay(DAYS.SUNDAY.ordinal, zoneId)

        val hList = HList.make(days)

        val localId = HRef.make(UUID.randomUUID().toString())
        val defaultSchedule = HDictBuilder()
            .add("id", localId)
            .add("kind", "Number")
            .add("zone")
            .add("temp")
            .add("schedule")
            .add("heating")
            .add("cooling")
            .add("dis", "Zone Schedule")
            .add("days", hList)
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedBy", CCUHsApi.getInstance().ccuUserName)
            .add("siteRef", siteId)
        if (zoneId != null) {
            defaultSchedule.add("roomRef", HRef.copy(zoneId))
            defaultSchedule.add("ccuRef", HRef.copy(CCUHsApi.getInstance().ccuId))
            defaultSchedule.add(Tags.FOLLOW_BUILDING)
            defaultSchedule.add(
                Tags.UNOCCUPIED_ZONE_SETBACK, getZoneScheduablePoint(
                    Queries.ZONE_UNOCCUPIED_ZONE_SETBACK,
                    zoneId
                )
            )
            defaultSchedule.add("scheduleGroup", ScheduleGroup.WEEKDAY_WEEKEND.ordinal.toLong())
        }

        CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule.toDict())
        return localId.toCode()
    }

    private fun createZoneSchedulesIfMissing(ccuHsApi: CCUHsApi) {
        val rooms: List<HashMap<Any?, Any>> = ccuHsApi.readAllEntities("room")
        for (room: HashMap<Any?, Any> in rooms) {
            val scheduleHashmap = ccuHsApi.readEntity(
                "schedule and " +
                        "not special and not vacation and roomRef " + "== " + room["id"]
            )
            if (scheduleHashmap.size == 0) {
                val scheduleRef: String =
                    generateDefaultZoneSchedule(room["id"].toString())
                val scheduleType = ccuHsApi.readPointPriorityValByQuery(
                    (("scheduleType and roomRef == \""
                            + room["id"] + "\""))
                )
                if (scheduleType == null || scheduleType == ScheduleType.ZONE.ordinal.toDouble()) {
                    val roomToUpdate = ccuHsApi.readMapById(room["id"].toString())
                    val zone = Zone.Builder().setHashMap(roomToUpdate).build()
                    zone.scheduleRef = scheduleRef
                    ccuHsApi.updateZone(zone, zone.id)
                }
            }
        }
    }

    override fun execute(): Boolean {
        issueFound = false
//        hayStack.deleteWritablePoint("030d7ac8-9b92-4be5-a5b2-94be9cef1b9c") // For testing heatingUserLimitMin
//        hayStack.deleteEntity("8c885287-c414-4296-82c6-c0441e730c46") // Delete a schedule point for testing
        // Step 1: Read all points with room query
        val roomList = CCUHsApi.getInstance().readAllHDictByQuery("room")
        // Step 2: Check if the point has a valid scheduleRef
        for (roomDict in roomList) {
            val scheduleRef = roomDict.get("scheduleRef").toString()
            // Check if the room has a scheduleRef
            val scheduleRefPt = CCUHsApi.getInstance().readMapById(scheduleRef)
            val scheduleType = CCUHsApi.getInstance().readPointPriorityValByQuery(
                "scheduleType and roomRef == \"${roomDict.get("id")}\""
            )
            // If the scheduleType is named, then skip validation since it will be taken care of by NamedScheduleValidation
            if(scheduleType == null || scheduleType == ScheduleType.NAMED.ordinal.toDouble()) continue
            // Step 3: Check if the scheduleRef exists in the database and is valid
            if(scheduleRefPt.isEmpty()) {
                CcuLog.e(SANITTY_TAG,"Invalid scheduleRef for room: ${roomDict.get("id")}, creating new zone schedule")
                // Step 4: If the scheduleRef is invalid, log an error and create a new zone schedule
                createZoneSchedulesIfMissing(CCUHsApi.getInstance())
                issueFound = true
                return false
            }
        }

        issueFound = false
        return true
    }

    private fun testScheduleDeletion(
        scheduleRefPt: java.util.HashMap<Any, Any>,
        scheduleRef: String
    ): java.util.HashMap<Any, Any> {
        var scheduleRefPt1 = scheduleRefPt
        val schedules =
            CCUHsApi.getInstance()
                .readAll("schedule and roomRef == " + scheduleRefPt1.get("roomRef").toString())
        for (schedule in schedules) {
            CCUHsApi.getInstance().deleteEntity(schedule["id"].toString())
        }
        scheduleRefPt1 = CCUHsApi.getInstance().readMapById(scheduleRef)
        return scheduleRefPt1
    }

    override fun report(): String {
        return "Schedule reference validation passed"
    }

    override fun correct(): Boolean {
        // Placeholder for correction logic
        return false
    }

    override fun getDescription(): String {
        if(issueFound) {
            return "Found and fixed corrupt zone schedules."
        }
        return "No corrupt zone schedules found."
    }
}