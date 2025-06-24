package a75f.io.logic.bo.building.pointscheduling

import PointDefinition
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.logic.bo.building.pointscheduling.model.Day
import org.junit.Test
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HList
import org.projecthaystack.HRef
import java.util.Calendar

class RecurringScheduleLogicTest {
    private var mockHayStack = MockCcuHsApi()


    @Test
    fun `Test scenario - 1 pointSchedule`() {
        val days = mutableListOf<HDict>()

        days.add(
            HDictBuilder()
                .add("sthh", 6)
                .add("ethh", 13)
                .add("stmm", 15)
                .add("etmm", 30)
                .add("day", 0)
                .add("val", 11)
                .toDict()
        )

        days.add(
            HDictBuilder()
                .add("sthh", 6)
                .add("ethh", 13)
                .add("stmm", 15)
                .add("etmm", 30)
                .add("day", 1)
                .add("val", 2)
                .toDict()
        )

        days.add(
            HDictBuilder()
                .add("sthh", 6)
                .add("ethh", 13)
                .add("stmm", 15)
                .add("etmm", 30)
                .add("day", 2)
                .add("val", 3)
                .toDict()
        )

        days.add(
            HDictBuilder()
                .add("sthh", 6)
                .add("ethh", 13)
                .add("stmm", 15)
                .add("etmm", 30)
                .add("day", 3)
                .add("val", 4)
                .toDict()
        )

        days.add(
            HDictBuilder()
                .add("sthh", 6)
                .add("ethh", 13)
                .add("stmm", 15)
                .add("etmm", 30)
                .add("day", 4)
                .add("val", 5)
                .toDict()
        )

        days.add(
            HDictBuilder()
                .add("sthh", 6)
                .add("ethh", 13)
                .add("stmm", 15)
                .add("etmm", 30)
                .add("day", 5)
                .add("val", 6)
                .toDict()
        )

        days.add(
            HDictBuilder()
                .add("sthh", 6)
                .add("ethh", 13)
                .add("stmm", 15)
                .add("etmm", 30)
                .add("day", 6)
                .add("val", 7)
                .toDict()
        )

        val pointDefinitions = mutableListOf<HDict>()


        pointDefinitions.add(
            HDictBuilder()
                .add("id", HRef.copy("1a3f58d9-22db-4eac-b75a-ad46a3edaece"))
                .add("scheduleGroup", 2)
                .add("defaultValue", 1)
                .add("dis", "writable and not enum and discharge and air")
                .add("unit", "en,gal/min,inH₂O,°F,cfm,psi,kWh")
                .add("builderType", "CUSTOM")
                .add("query", "writable and not enum and discharge and air")
                .add("days", HList.make(days))
                .toDict()
        )
        pointDefinitions.add(
            HDictBuilder()
                .add("id", HRef.copy("dba7a9ed-0a3a-41a2-8a32-46e0c2e83c37"))
                .add("scheduleGroup", 2)
                .add("defaultValue", 2)
                .add("dis", "writable and enum and operating")
                .add("unit", "en,gal/min,inH₂O,°F,cfm,psi,kWh")
                .add("builderType", "CUSTOM")
                .add("query", "writable and enum and operating")
                .add("days", HList.make(days))
                .toDict()
        )



        val pointScheduleHdict = HDictBuilder()
            .add("ver", "3.0")
            .add("id", HRef.copy("f409123f-d067-4df5-a04f-a7db9bdfd5e1"))
            .add("createdDateTime", HDateTime.make("2025-05-26T11:52:43.545Z"))
            .add("dis", "cust schedule-261")
            .add("lastModifiedBy", "platform_UNKNOWN")
            .add("lastModifiedDateTime", HDateTime.make("2025-05-26T11:52:43.545Z"))
            .add("organization", "test_cs")
            .add("organizationId", HRef.copy("4f7b97cf-d68a-43a0-9ad6-1f28f8ded60b"))
            .add("point")                       // originally M / null
            .add("pointDefinitions", HList.make(pointDefinitions))
            .add("pointSchedule")                // originally M / null
            .add("reason")
            .add("schedule")                     // originally M / null
            .toDict()



        mockHayStack.addHDict("@f409123f-d067-4df5-a04f-a7db9bdfd5e1", pointScheduleHdict)
        val pointDict = mockHayStack
            .readEntity("id == " + HRef.make("f409123f-d067-4df5-a04f-a7db9bdfd5e1"))
        println("point dict = $pointDict")
        val pointDefs = pointDict["pointDefinitions"] as HList

        isPointScheduleActiveNow(PointDefinition().dictToPointDefinition(pointDefs.get(0) as HDict))
    }


    // currently using
    private fun isPointScheduleActiveNow(pointDef: PointDefinition): Map<Boolean, Day> {

        val dis = pointDef.dis
        val id = pointDef.id
        //CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Processing point with ID: $id, display name: $dis")
        println("Processing pointDefinitionId: $id, display name: $dis")

        val calendar = Calendar.getInstance()
        val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) - 2).let { if (it < 0) 6 else it }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        /*CcuLog.d(
            L.TAG_CCU_POINT_SCHEDULE,
            "Current day/time: Day=$currentDay, Time=%02d:%02d".format(currentHour, currentMinute)
        )*/

        println("Current day/time: Day=$currentDay, Time=%02d:%02d".format(currentHour, currentMinute))
        val days = pointDef.days as? ArrayList ?: return mapOf(false to Day())
        val result = mutableMapOf(false to Day())

        for (i in 0 until days.size) {
            val dayItem = days[i]
            /*CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Processing day item: ${dayItem.day}," +
                        " start: ${dayItem.sthh}:${dayItem.stmm}," +
                        " end: ${dayItem.ethh}:${dayItem.etmm}"
            )*/
            println(
                "Processing day item: ${dayItem.day}," +
                        " start: ${dayItem.sthh}:${dayItem.stmm}," +
                        " end: ${dayItem.ethh}:${dayItem.etmm}"
            )
            val scheduledDay = dayItem.day.toString().toInt()
            if (scheduledDay != currentDay) continue

            val startHour = dayItem.sthh.toString().toDouble().toInt()
            val startMinute = dayItem.stmm.toString().toDouble().toInt()
            val endHour = dayItem.ethh.toString().toDouble().toInt()
            val endMinute = dayItem.etmm.toString().toDouble().toInt()

            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute

            /*CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Checking schedule: stt:stm - $startHour:$startMinute to ett:etm - $endHour:$endMinute for day $scheduledDay"
            )*/
            println(
                "Checking schedule: stt:stm - $startHour:$startMinute to ett:etm - $endHour:$endMinute for day $scheduledDay"
            )

            if (currentTotalMinutes in startTotalMinutes..endTotalMinutes) {
                val value = dayItem.`val`.toString().toDouble()
                //CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "custom value found. value=$value")
                println("custom value found. value=$value")
                return mapOf(true to dayItem)
            }
        }

        //CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "No active schedule found.")
        println("No active schedule found.")
        return result
    }
}

