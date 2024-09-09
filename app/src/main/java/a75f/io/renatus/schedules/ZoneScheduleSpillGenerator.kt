package a75f.io.renatus.schedules

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Schedule
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L.TAG_CCU_SCHEDULE
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRow
import org.projecthaystack.client.HClient

class ZoneScheduleSpillGenerator {
    private val ccuHsApi = CCUHsApi.getInstance()
    data class ZoneScheduleSpill(
        val zoneRef: String,
        val zoneDis: String,
        val zoneProfile: String,
        val floorDis: String,
        val schedule: Schedule
    )
    fun generateSpill() : List<ZoneScheduleSpill>{
        CcuLog.d(TAG_CCU_SCHEDULE, "Schedule Spill Generation started")
        val finalResponse = mutableListOf<HGrid>()
        val equipFilter = "equip and roomRef != \"" + "SYSTEM" + "\" and siteRef == " + ccuHsApi.siteIdRef
        val namedScheduleFilter = "named and schedule and organization == \"" +
                CCUHsApi.getInstance().site!!.getOrganization() + "\""
        val scheduleFilter = "schedule and days and siteRef == " + ccuHsApi.siteIdRef
        val roomFloorFilter = "(room or floor) and siteRef == " + ccuHsApi.siteIdRef
        val combinedFilter : List<String> = listOf(equipFilter, namedScheduleFilter, scheduleFilter, roomFloorFilter)
        combinedFilter.forEach { filter ->
            val combinedDetails = HDictBuilder().add("filter", filter).toDict()
            val combinedDetailsGrid = HGridBuilder.dictToGrid(combinedDetails)
            val hClient = HClient(ccuHsApi.hsUrl, HayStackConstants.USER, HayStackConstants.PASS)
            val combinedDetailsResponse: HGrid = hClient.call("read", combinedDetailsGrid)
            finalResponse.add(combinedDetailsResponse)
        }
        CcuLog.d(TAG_CCU_SCHEDULE, "Imported Schedules, Rooms, Floors, Equips")


        val listOfRooms = mutableListOf<HashMap<Any, Any>>()
        val listOfFloors = mutableListOf<HashMap<Any, Any>>()
        val listOfSchedules = mutableListOf<HDict>()
        val listOfEquips = mutableListOf<HashMap<Any, Any>>()

        val zoneScheduleSpillList = mutableListOf<ZoneScheduleSpill>()

        finalResponse.forEach {
            val finalList = ccuHsApi.HGridToList(it)
            findSchedulesInResponse(it, listOfSchedules)
            finalList.forEach { row ->
                if (row.containsKey(Tags.ROOM)) {
                    listOfRooms.add(row)
                } else if (row.containsKey(Tags.FLOOR)) {
                    listOfFloors.add(row)
                } else if (row.containsKey(Tags.EQUIP)) {
                    listOfEquips.add(row)
                }
            }
        }
        CcuLog.d(TAG_CCU_SCHEDULE, "Separated Schedules, Rooms, Floors, Equips")

        listOfRooms.forEach { room ->
            val zoneRef = room[Tags.ID].toString()
            val zoneDis = room[Tags.DIS].toString()
            val zoneProfile = listOfEquips.filter { it[Tags.ROOMREF] == room[Tags.ID] }[0][Tags.PROFILE].toString()
            val floorDis = listOfFloors.filter { it[Tags.ID] == room[Tags.FLOORREF] }[0][Tags.DIS].toString()
            val schedule = listOfSchedules.filter { room[Tags.SCHEDULE_REF] == it[Tags.ID] }[0]
            val scheduleDict = Schedule.Builder().setHDict(schedule).build()
            val zoneScheduleSpill = ZoneScheduleSpill(zoneRef, zoneDis, zoneProfile, floorDis, scheduleDict)
            zoneScheduleSpillList.add(zoneScheduleSpill)
        }
        CcuLog.d(TAG_CCU_SCHEDULE, "Generated Zone Schedule Spill")
        return zoneScheduleSpillList
    }

    private fun findSchedulesInResponse(finalList: HGrid, listOfSchedules: MutableList<HDict>) {
        val iterator = finalList.iterator()
        while (iterator.hasNext()) {
            val row = iterator.next() as HRow

            if (row.has(Tags.SCHEDULE)) {
                listOfSchedules.add(row as HDict)
            }
        }
    }
}