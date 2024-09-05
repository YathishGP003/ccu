package a75f.io.renatus.schedules

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.Schedule
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.logic.DefaultSchedules
import a75f.io.logic.schedule.ScheduleGroup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HList
import org.projecthaystack.HRef
import java.util.UUID

class CommonTimeSlotFinderTest {

    private lateinit var commonTimeSlotFinder: CommonTimeSlotFinder
    private var hayStack = MockCcuHsApi()
    lateinit var scheduleId: String
    lateinit var schedule: Schedule
    @After
    fun tearDown() {
        hayStack.closeDb()
        hayStack.clearDb()
    }
    @Before
    fun setUp() {
        commonTimeSlotFinder = CommonTimeSlotFinder()
        val s75f = Site.Builder()
            .setDisplayName("testSite")
            .addMarker("site")
            .setGeoCity("siteCity")
            .setGeoState("siteState")
            .setTz("Calcutta")
            .setGeoZip("580118")
            .setGeoCountry("india")
            .setOrgnization("org")
            .setInstaller("inst.@gm.cm")
            .setFcManager("fc@gm.cm")
            .setGeoAddress("bangalre")
            .setGeoFence("2.0")
            .setArea(10000).build()

        val siteId = hayStack.addSite(s75f)

        val days = arrayOfNulls<HDict>(7)

        days[0] = DefaultSchedules.getDefaultForDay(DAYS.MONDAY.ordinal, null)
        days[1] = DefaultSchedules.getDefaultForDay(DAYS.TUESDAY.ordinal, null)
        days[2] = DefaultSchedules.getDefaultForDay(DAYS.WEDNESDAY.ordinal, null)
        days[3] = DefaultSchedules.getDefaultForDay(DAYS.THURSDAY.ordinal, null)
        days[4] = DefaultSchedules.getDefaultForDay(DAYS.FRIDAY.ordinal, null)

        days[5] = DefaultSchedules.getDefaultForDay(DAYS.SATURDAY.ordinal, null)
        days[6] = DefaultSchedules.getDefaultForDay(DAYS.SUNDAY.ordinal, null)


        val hList = HList.make(days)

        val localId = HRef.make(UUID.randomUUID().toString())
        val defaultSchedule: HDictBuilder = HDictBuilder()
            .add("id", localId)
            .add("kind", "Number")
            .add("zone")
            .add("temp")
            .add("schedule")
            .add("heating")
            .add("cooling")
            .add("dis","Zone Schedule")
            .add("days", hList)
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedBy", CCUHsApi.getInstance().ccuUserName)
            .add("siteRef", HRef.make(siteId))
            defaultSchedule.add(Tags.FOLLOW_BUILDING)

            defaultSchedule.add("scheduleGroup", ScheduleGroup.WEEKDAY_WEEKEND.ordinal.toLong())
        scheduleId = localId.toString()
        schedule =  Schedule.Builder().setHDict(defaultSchedule.toDict()).build()

    }


    @Test
    fun testGetCommonIntervals() {
        val schedule = mapOf(
            "Monday" to listOf(
                CommonTimeSlotFinder.TimeSlot(9, 0, 11, 0),
                CommonTimeSlotFinder.TimeSlot(13, 0, 15, 0)
            ),
            "Tuesday" to listOf(
                CommonTimeSlotFinder.TimeSlot(10, 0, 12, 0),
                CommonTimeSlotFinder.TimeSlot(14, 0, 16, 0)
            )
        )

        val expectedCommonIntervals = listOf(
            CommonTimeSlotFinder.TimeSlot(10, 0, 11, 0),
            CommonTimeSlotFinder.TimeSlot(14, 0, 15, 0)
        )

        val result = commonTimeSlotFinder.getCommonIntervals(schedule)
        assertEquals(expectedCommonIntervals, result)
    }

    @Test
    fun testGetUncommonIntervals() {
        val commonIntervals = listOf(
            CommonTimeSlotFinder.TimeSlot(10, 0, 11, 0),
            CommonTimeSlotFinder.TimeSlot(14, 0, 15, 0)
        )
        val startHour = 9
        val startMinute = 0
        val endHour = 16
        val endMinute = 0

        val expectedUncommonIntervals = listOf(
            CommonTimeSlotFinder.TimeSlot(9, 0, 10, 0),
            CommonTimeSlotFinder.TimeSlot(11, 0, 14, 0),
            CommonTimeSlotFinder.TimeSlot(15, 0, 16, 0)
        )

        val result = commonTimeSlotFinder.getUncommonIntervals(
            commonIntervals, startHour, startMinute, endHour, endMinute
        )
        assertEquals(expectedUncommonIntervals, result)
    }

    @Test
    fun add(){
        val a = 1
        val b = 2
        val result = a + b
        assertEquals(3, result)
    }

    @Test
    fun testGetCommonTimeSlotEveryday() {
        val scheduleGroup = ScheduleGroup.EVERYDAY.ordinal
        val days = schedule.days
        val startHour = 9
        val startMinute = 0
        val endHour = 17
        val endMinute = 0

        val result = commonTimeSlotFinder.getCommonTimeSlot(
            scheduleGroup, days, startHour, startMinute, endMinute
        )
        val expectedCommonIntervals = listOf(
            CommonTimeSlotFinder.TimeSlot(9, 0, 17, 0)
        )

        assertEquals(listOf(expectedCommonIntervals), result)
    }

    @Test
    fun testGetSpilledZonesEveryday() {
        val schedule = schedule
        val uncommonIntervals = listOf(listOf(CommonTimeSlotFinder.TimeSlot(9, 0, 10, 0)))

        val result = commonTimeSlotFinder.getSpilledZones(schedule, uncommonIntervals)
        val expected = StringBuilder("Everyday (09:00 - 10:00)")

        assertEquals(expected.toString(), result.toString())
    }

    // Add more tests for other methods and scenarios
}
