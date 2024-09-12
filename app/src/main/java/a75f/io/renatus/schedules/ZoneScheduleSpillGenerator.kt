package a75f.io.renatus.schedules

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Schedule
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L.TAG_ZONE_SCHEDULE_SPILL
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRow
import org.projecthaystack.client.HClient
import org.projecthaystack.util.WebUtil

class ZoneScheduleSpillGenerator {
    private val ccuHsApi = CCUHsApi.getInstance()
    private val pageSize = 100
    data class ZoneScheduleSpill(
        val zoneRef: String,
        val zoneDis: String,
        val zoneProfile: String,
        val floorDis: String,
        val schedule: Schedule
    )
    fun generateSpill() : List<ZoneScheduleSpill>{
        var pageNo = 0
        CcuLog.d(TAG_ZONE_SCHEDULE_SPILL, "Schedule Spill Generation started")
        val finalResponse = mutableListOf<HGrid>()

        val organization = ccuHsApi.site!!.getOrganization()
        val siteIdRef = ccuHsApi.siteIdRef
        val combinedFilter= """((schedule and days) or (room) or (floor) or (equip and roomRef != "SYSTEM")) and siteRef == $siteIdRef or (named and schedule and organization == "$organization")"""

        val combinedDetails = HDictBuilder().add("filter", combinedFilter).toDict()
        val combinedDetailsGrid = HGridBuilder.dictToGrid(combinedDetails)
        val hClient = HClient(ccuHsApi.hsUrl, HayStackConstants.USER, HayStackConstants.PASS)
        val combinedDetailsResponse: HGrid = hClient.call("read", combinedDetailsGrid, pageNo, pageSize)
        ccuHsApi.HGridToList(combinedDetailsResponse)

        finalResponse.add(combinedDetailsResponse)
        val responsePageSize = WebUtil.getResponsePageSize(combinedDetailsResponse, pageSize)
        CcuLog.d(TAG_ZONE_SCHEDULE_SPILL, "Response received from backend:" +
                " no of remaining pages to be fetched $responsePageSize")

        if (responsePageSize > 0) {
            pageNo = 1
            while (pageNo <= responsePageSize) {
                val nextReadChangesResponse =
                    hClient.call("read", combinedDetailsResponse, pageNo, pageSize)
                finalResponse.add(nextReadChangesResponse)
                CcuLog.d(TAG_ZONE_SCHEDULE_SPILL, "iteration response size " + finalResponse.size)
                pageNo++
            }
        }
        CcuLog.d(TAG_ZONE_SCHEDULE_SPILL, "Imported Schedules, Rooms, Floors, Equips")

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
        CcuLog.d(TAG_ZONE_SCHEDULE_SPILL, "Separated Schedules, Rooms, Floors, Equips")

        listOfRooms.forEach { room ->
            val zoneRef = room[Tags.ID].toString()
            val zoneDis = room[Tags.DIS].toString()
            val filteredEquips = listOfEquips.filter { it[Tags.ROOMREF] == room[Tags.ID] }

            val zoneProfile = if (filteredEquips.isNotEmpty()) {
                filteredEquips[0][Tags.PROFILE]?.toString() ?: ""
            } else {
                ""
            }
            if(zoneProfile.isEmpty()){
                CcuLog.d(TAG_ZONE_SCHEDULE_SPILL,
                    "Zone Profile is empty for zone $zoneDis so avoiding this zone to calculate spill"
                )
                return@forEach
            }
            val floorDis = listOfFloors.filter { it[Tags.ID] == room[Tags.FLOORREF] }[0][Tags.DIS].toString()
            val schedule = listOfSchedules.filter { room[Tags.SCHEDULE_REF] == it[Tags.ID] }[0]
            val scheduleDict = Schedule.Builder().setHDict(schedule).build()
            val zoneScheduleSpill = ZoneScheduleSpill(zoneRef, zoneDis, zoneProfile, floorDis, scheduleDict)
            zoneScheduleSpillList.add(zoneScheduleSpill)
        }
        CcuLog.d(TAG_ZONE_SCHEDULE_SPILL, "Generated Zone Schedule Spill")
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