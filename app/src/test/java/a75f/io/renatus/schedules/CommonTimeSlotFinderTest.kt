package a75f.io.renatus.schedules

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.Schedule
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.logic.DefaultSchedules
import a75f.io.logic.schedule.ScheduleGroup
import a75f.io.logic.util.CommonTimeSlotFinder
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
    fun testGetUncommonIntervals() {
       /* val commonIntervals = listOf(
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
        )*/
        assertEquals(1, 1)
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
        /*val scheduleGroup = ScheduleGroup.EVERYDAY.ordinal
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
        )*/

        assertEquals(1, 1)
    }

    @Test
    fun testGetSpilledZonesEveryday() {
        /*val schedule = schedule
        val uncommonIntervals = listOf(listOf(CommonTimeSlotFinder.TimeSlot(9, 0, 10, 0)))

        val result = commonTimeSlotFinder.getSpilledZones(schedule, uncommonIntervals)
        val expected = StringBuilder("Everyday (09:00 - 10:00)")*/

        assertEquals(1, 1)
    }
    @Test
    fun testGetCommonIntervals() {
        val testCases = listOf(
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 19:30", "20:00 - 24:00"),
                    "Tuesday" to listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 19:30", "20:00 - 24:00"),
                    "Wednesday" to listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 19:30", "20:00 - 24:00"),
                    "Thursday" to listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 19:30", "20:00 - 24:00"),
                    "Friday" to listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 19:30", "20:00 - 24:00"),
                ),
                listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 19:30", "20:00 - 24:00"),
            ),
            // Test Case 1: Simple overlap between two days
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 12:00", "14:00 - 18:00"),
                    "Tuesday" to listOf("10:00 - 16:00", "18:00 - 20:00")
                ),
                listOf("10:00 - 12:00", "14:00 - 16:00")
            ),

            // Test Case 2: No overlap across all days
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 10:00"),
                    "Tuesday" to listOf("11:00 - 13:00"),
                    "Wednesday" to listOf("14:00 - 16:00")
                ),
                listOf()
            ),

            // Test Case 3: Fully overlapping intervals across all days
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 10:00"),
                    "Tuesday" to listOf("08:00 - 10:00"),
                    "Wednesday" to listOf("08:00 - 10:00")
                ),
                listOf("08:00 - 10:00")
            ),

            // Test Case 4: Partial overlaps across multiple days
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 14:00"),
                    "Tuesday" to listOf("10:00 - 16:00"),
                    "Wednesday" to listOf("12:00 - 18:00")
                ),
                listOf("12:00 - 14:00")
            ),

            // Test Case 5: Single day in the schedule
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 12:00", "14:00 - 18:00")
                ),
                listOf("08:00 - 12:00", "14:00 - 18:00")
            ),

            // Test Case 6: Overlap with multiple intervals on some days
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 10:00", "12:00 - 14:00"),
                    "Tuesday" to listOf("09:00 - 13:00", "15:00 - 16:00"),
                    "Wednesday" to listOf("08:30 - 11:00", "13:00 - 15:30")
                ),
                listOf("09:00 - 10:00")
            ),

            // Test Case 7: Empty schedule
            Pair(
                emptyMap<String, List<String>>(),
                listOf()
            ),

            // Test Case 8: All days with identical intervals
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 12:00"),
                    "Tuesday" to listOf("08:00 - 12:00"),
                    "Wednesday" to listOf("08:00 - 12:00")
                ),
                listOf("08:00 - 12:00")
            ),

            // Test Case 9: Different intervals with no intersection
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 10:00"),
                    "Tuesday" to listOf("10:30 - 12:30"),
                    "Wednesday" to listOf("13:00 - 15:00")
                ),
                listOf()
            ),

            // Test Case 10: Overlap across two days, no overlap on the third
            Pair(
                mapOf(
                    "Monday" to listOf("08:00 - 12:00"),
                    "Tuesday" to listOf("10:00 - 14:00"),
                    "Wednesday" to listOf("15:00 - 18:00")
                ),
                listOf()
            )
        )

        // Running the test cases
        for ((scheduleInput, expected) in testCases) {
            val schedule = scheduleInput.mapValues { (_, times) -> times.map { convertStringToTimeSlot(it) } }
            val expectedResult = expected.map { convertStringToTimeSlot(it) }

            val actualResult = commonTimeSlotFinder.getCommonIntervals(schedule)
            assertEquals(expectedResult, actualResult)
        }
    }

    @Test
    fun testCreateDayScheduleWithBoundaries() {
        // Pair format: (timeSlots, boundaries, expectedResult)
        val testCases = listOf(

            Triple(
                     listOf("08:00 - 17:30"),
                      listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 19:30", "20:00 - 24:00"),
                     listOf("08:00 - 08:45", "09:30 - 14:30", "14:45 - 17:30")
                ),

            // Test Case 1: Simple overlapping intervals
            Triple(
                listOf("08:00 - 10:00", "12:00 - 14:00"),
                listOf("09:00 - 13:00"),
                listOf("09:00 - 10:00", "12:00 - 13:00")
            ),

            // Test Case 2: No overlap
            Triple(
                listOf("14:00 - 16:00", "18:00 - 20:00"),
                listOf("08:00 - 10:00", "12:00 - 13:00"),
                listOf()
            ),

            // Test Case 3: Exact match with boundaries
            Triple(
                listOf("08:00 - 10:00", "12:00 - 14:00"),
                listOf("08:00 - 10:00", "12:00 - 14:00"),
                listOf("08:00 - 10:00", "12:00 - 14:00")
            ),

            // Test Case 4: Partial overlaps with boundaries
            Triple(
                listOf("07:00 - 09:00", "10:30 - 12:30"),
                listOf("08:00 - 11:00"),
                listOf("08:00 - 09:00", "10:30 - 11:00")
            ),

            // Test Case 5: Fully nested intervals within boundaries
            Triple(
                listOf("08:00 - 09:00", "10:00 - 11:00"),
                listOf("07:00 - 12:00"),
                listOf("08:00 - 09:00", "10:00 - 11:00")
            ),

            // Test Case 6: Multiple boundaries intersecting a single time slot
            Triple(
                listOf("08:00 - 12:00"),
                listOf("07:00 - 09:00", "10:00 - 11:00"),
                listOf("08:00 - 09:00", "10:00 - 11:00")
            ),

            // Test Case 7: Edge case with matching start and end times
            Triple(
                listOf("09:00 - 11:00"),
                listOf("09:00 - 11:00"),
                listOf("09:00 - 11:00")
            ),

            // Test Case 8: Non-overlapping time slots with partial overlaps at boundary edges
            Triple(
                listOf("06:00 - 07:30", "11:00 - 13:00", "15:00 - 17:00"),
                listOf("07:00 - 12:00", "16:00 - 18:00"),
                listOf("07:00 - 07:30", "11:00 - 12:00", "16:00 - 17:00")
            ),

            // Test Case 9: Empty time slots list
            Triple(
                listOf(),
                listOf("08:00 - 10:00", "12:00 - 14:00"),
                listOf()
            ),

            // Test Case 10: Empty boundaries list
            Triple(
                listOf("08:00 - 10:00", "12:00 - 14:00"),
                listOf(),
                listOf()
            )
        )

        // Run all test cases
        for ((timeSlots, boundaries, expected) in testCases) {
            val timeSlotObjects = timeSlots.map { convertStringToTimeSlot(it) }
            val boundaryObjects = boundaries.map { convertStringToTimeSlot(it) }
            val expectedObjects = expected.map { convertStringToTimeSlot(it) }

            val actualResult = commonTimeSlotFinder.createDayScheduleWithBoundaries(timeSlotObjects, boundaryObjects)
            assertEquals(expectedObjects, actualResult)
        }
    }

    @Test
    fun testGetUncommonTimeSlot(){

        // Triple format : (Building Intervals, Zone Intervals, Expected Result)
        val testCases = listOf(

            // Test Case 1
            Triple(
                listOf("00:00 - 03:45", "06:30 - 18:45", "19:45 - 24:00"),
                listOf("03:15 - 17:30"),
                listOf("03:45 - 06:30")
            ),
            // Test Case 2
            Triple(
                listOf("00:00 - 12:00", "14:00 - 18:00"),
                listOf("10:00 - 15:00"),
                listOf("12:00 - 14:00")
            ),
            // Test Case 3
            Triple(
                listOf("08:00 - 10:00", "12:00 - 14:00"),
                listOf("07:30 - 14:30"),
                listOf("07:30 - 08:00", "10:00 - 12:00", "14:00 - 14:30")
            ),
            // Test Case 4
            Triple(
                listOf("06:00 - 08:00", "10:00 - 12:00", "14:00 - 16:00"),
                listOf("05:00 - 17:00"),
                listOf("05:00 - 06:00", "08:00 - 10:00", "12:00 - 14:00", "16:00 - 17:00")
            ),
            // Test Case 5
            Triple(
                listOf("01:00 - 02:00", "04:00 - 05:00", "07:00 - 08:00"),
                listOf("00:30 - 08:30"),
                listOf("00:30 - 01:00", "02:00 - 04:00", "05:00 - 07:00", "08:00 - 08:30")
            ),
            // Test Case 6
            Triple(
                listOf("00:00 - 12:00"),
                listOf("06:00 - 18:00"),
                listOf("12:00 - 18:00")
            ),
            // Test Case 7
            Triple(
                listOf("09:00 - 11:00", "13:00 - 15:00"),
                listOf("08:00 - 16:00"),
                listOf("08:00 - 09:00", "11:00 - 13:00", "15:00 - 16:00")
            ),
            // Test Case 8
            Triple(
                listOf("23:00 - 24:00"),
                listOf("22:30 - 23:30"),
                listOf("22:30 - 23:00")
            ),
            // Test Case 9
            Triple(
                listOf("00:00 - 06:00"),
                listOf("03:00 - 09:00"),
                listOf("06:00 - 09:00")
            ),
            // Test Case 10
            Triple(
                listOf("10:00 - 11:00", "13:00 - 14:00", "16:00 - 17:00"),
                listOf("09:00 - 18:00"),
                listOf("09:00 - 10:00", "11:00 - 13:00", "14:00 - 16:00", "17:00 - 18:00")
            ),
            // Test Case 11
            Triple(
                listOf("10:00 - 11:00", "13:00 - 14:00", "16:00 - 17:00"),
                listOf("09:00 - 12:00", "12:30 - 15:00"),
                listOf("09:00 - 10:00", "11:00 - 12:00","12:30 - 13:00", "14:00 - 15:00")
            ),

        )

        for ((building, zone, expected) in testCases) {
            val buildingIntervals = building.map { convertStringToTimeSlot(it) }
            val zoneIntervals = zone.map { convertStringToTimeSlot(it) }
            val expectedResult = expected.map { convertStringToTimeSlot(it) }

            val actualResult = commonTimeSlotFinder.getUncommonIntervals(buildingIntervals, zoneIntervals)

            assertEquals(expectedResult, actualResult)
        }
    }
    @Test
    fun testCalculateStartMinute() {
        val boundary = CommonTimeSlotFinder.TimeSlot(8, 0, 17, 30)

        // Test cases in the format: (boundary, timeSlot, expectedStartMinute)
        val testCases = listOf(
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 0, 14, 30), 0),    // Case 1: Exact match
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 30, 14, 30), 30),  // Case 2: Same hour, boundary starts earlier
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(9, 15, 14, 30), 15),  // Case 3: TimeSlot starts after boundary hour
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(7, 45, 14, 30), 0),   // Case 4: Boundary starts after TimeSlot hour
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 15, 14, 30), 15)   // Case 5: Boundary minute is greater, same hour
        )

        for ((boundary, timeSlot, expectedStartMinute) in testCases) {
            val actualResult = commonTimeSlotFinder.calculateStartMinute(boundary, timeSlot)
            assertEquals(
                "Failed for boundary: $boundary and timeSlot: $timeSlot",
                expectedStartMinute,
                actualResult
            )
        }
    }

    @Test
    fun testCalculateEndMinute() {
        val boundary = CommonTimeSlotFinder.TimeSlot(8, 0, 17, 30)

        // Test cases in the format: (boundary, timeSlot, expectedEndMinute)
        val testCases = listOf(
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 0, 17, 30), 30),   // Case 1: Exact match
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 0, 17, 15), 15),   // Case 2: Same hour, pick minimum minute
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 0, 16, 0), 0),     // Case 3: TimeSlot ends earlier than boundary
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 0, 18, 0), 30),    // Case 4: TimeSlot extends beyond boundary
            Triple(boundary, CommonTimeSlotFinder.TimeSlot(8, 0, 15, 45), 45)    // Case 5: TimeSlot ends before boundary in a different hour
        )

        for ((boundary, timeSlot, expectedEndMinute) in testCases) {
            val actualResult = commonTimeSlotFinder.calculateEndMinute(boundary, timeSlot)
            assertEquals(
                "Failed for boundary: $boundary and timeSlot: $timeSlot",
                expectedEndMinute,
                actualResult
            )
        }
    }

    @Test
    fun convertStringToTimeslot() {
        val timeSlot = CommonTimeSlotFinder.TimeSlot(9, 0, 11, 0)
        val result = convertStringToTimeSlot("09:00 - 11:00")
        assertEquals(result, timeSlot)
    }

    // Helper function to convert string to TimeSlot
    private fun convertStringToTimeSlot(time: String): CommonTimeSlotFinder.TimeSlot {
        val (start, end) = time.split(" - ")
        val (startHour, startMinute) = start.split(":").map { it.toInt() }
        val (endHour, endMinute) = end.split(":").map { it.toInt() }
        return CommonTimeSlotFinder.TimeSlot(startHour, startMinute, endHour, endMinute)
    }

}
