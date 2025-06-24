package a75f.io.logic.bo.building.pointscheduling

import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.getCurrentDateTime
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.junit.Test
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HList
import org.projecthaystack.HRef
import org.projecthaystack.HStr


class EventScheduleLogicTest {

    @Test
    fun `Test  scenario 1`() {


        val range0 = HDictBuilder().add("startTime", HStr.make("00:00")).add("endTime", HStr.make("23:59"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val range1 = HDictBuilder().add("startTime", HStr.make("17:00")).add("endTime", HStr.make("21:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()


        val eventDefinition = mutableListOf<HDict>()

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        val eventScheduleDict0 = HDictBuilder()
            .add("id", HRef.copy("es0")).add("siteRef", HRef.copy("site0")).add("dis", "Christmas Holiday")
            .add("orgRef", HRef.copy("org0")).add("range", range0).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val eventScheduleDict1 = HDictBuilder()
            .add("id", HRef.copy("es1")).add("siteRef", HRef.copy("site1")).add("dis", "Christmas Party")
            .add("orgRef", HRef.copy("org1")).add("range", range1).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val events = listOf(eventScheduleDict0, eventScheduleDict1)
        val activeEvent = findActiveEvent(events)

        println("SELECTED EVENT-------------: $activeEvent")

    }

    @Test
    fun `Test scenario 2`() {

        val range0 = HDictBuilder().add("startTime", HStr.make("18:00")).add("endTime", HStr.make("21:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val range1 = HDictBuilder().add("startTime", HStr.make("18:00")).add("endTime", HStr.make("21:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()


        val eventDefinition = mutableListOf<HDict>()

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        val eventScheduleDict0 = HDictBuilder()
            .add("id", HRef.copy("es0")).add("siteRef", HRef.copy("site0")).add("dis", "Event Schedule event0")
            .add("orgRef", HRef.copy("org0")).add("range", range0).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val eventScheduleDict1 = HDictBuilder()
            .add("id", HRef.copy("es1")).add("siteRef", HRef.copy("site1")).add("dis", "Event Schedule event1")
            .add("orgRef", HRef.copy("org1")).add("range", range1).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()+1000))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val events = listOf(eventScheduleDict0, eventScheduleDict1)
        val activeEvent = findActiveEvent(events)

        println("SELECTED EVENT-------------: $activeEvent")

    }


    @Test
    fun `Test scenario 3`() {

        val range0 = HDictBuilder().add("startTime", HStr.make("8:00")).add("endTime", HStr.make("18:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val range1 = HDictBuilder().add("startTime", HStr.make("16:00")).add("endTime", HStr.make("18:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val range2 = HDictBuilder().add("startTime", HStr.make("17:00")).add("endTime", HStr.make("19:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val eventDefinition = mutableListOf<HDict>()

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        val eventScheduleDict0 = HDictBuilder()
            .add("id", HRef.copy("es0")).add("siteRef", HRef.copy("site0")).add("dis", "Event Schedule event0")
            .add("orgRef", HRef.copy("org0")).add("range", range0).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val eventScheduleDict1 = HDictBuilder()
            .add("id", HRef.copy("es1")).add("siteRef", HRef.copy("site1")).add("dis", "Event Schedule event1")
            .add("orgRef", HRef.copy("org1")).add("range", range1).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()

        val eventScheduleDict2 = HDictBuilder()
            .add("id", HRef.copy("es2")).add("siteRef", HRef.copy("site2")).add("dis", "Event Schedule event2")
            .add("orgRef", HRef.copy("org2")).add("range", range2).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val events = listOf(eventScheduleDict0, eventScheduleDict1, eventScheduleDict2)
        val activeEvent = findActiveEvent(events)

        println("SELECTED EVENT-------------: $activeEvent")

    }

    @Test
    fun `Test scenario 4 with all above`() {

        val range0 = HDictBuilder().add("startTime", HStr.make("00:00")).add("endTime", HStr.make("23:59"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val range1 = HDictBuilder().add("startTime", HStr.make("18:00")).add("endTime", HStr.make("21:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val range2 = HDictBuilder().add("startTime", HStr.make("18:00")).add("endTime", HStr.make("21:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val range3 = HDictBuilder().add("startTime", HStr.make("18:30")).add("endTime", HStr.make("21:00"))
            .add("startDate", HStr.make("2025-04-24")).add("endDate", HStr.make("2025-04-25")).toDict()

        val eventDefinition = mutableListOf<HDict>()

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        eventDefinition.add(
            HDictBuilder()
                .add("id", HRef.copy("e518a994-9fc6-457a-b07b-960181e605de2"))
                .add("dis", "Temp Sensor1")
                .add("haystackQuery", "temp and sensor1 and sensor2")
                .add("tags", HList.make(listOf(HStr.make("sensor"), HStr.make("sensor1"))))
                .add("value", 10.5)
                .add("builderType", "Builder")
                .toDict()
        )

        val eventScheduleDict0 = HDictBuilder()
            .add("id", HRef.copy("es0")).add("siteRef", HRef.copy("site0")).add("dis", "Event Schedule event0")
            .add("orgRef", HRef.copy("org0")).add("range", range0).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val eventScheduleDict1 = HDictBuilder()
            .add("id", HRef.copy("es1")).add("siteRef", HRef.copy("site1")).add("dis", "Event Schedule event1")
            .add("orgRef", HRef.copy("org1")).add("range", range1).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()

        val eventScheduleDict2 = HDictBuilder()
            .add("id", HRef.copy("es2")).add("siteRef", HRef.copy("site2")).add("dis", "Event Schedule event2")
            .add("orgRef", HRef.copy("org2")).add("range", range2).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()+1000))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()

        val eventScheduleDict3 = HDictBuilder()
            .add("id", HRef.copy("es2")).add("siteRef", HRef.copy("site2")).add("dis", "Event Schedule event3")
            .add("orgRef", HRef.copy("org2")).add("range", range3).add("event").add("point")
            .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
            .add("eventDefinitions", HList.make(eventDefinition)).toDict()


        val events = listOf(eventScheduleDict0, eventScheduleDict1, eventScheduleDict2, eventScheduleDict3)
        val activeEvent = findActiveEvent(events)

        println("SELECTED EVENT-------------: $activeEvent")

    }


    private fun findActiveEvent(dicts: List<HDict>): HDict? {
        val datetime = getCurrentDateTime()

        val currentDate = LocalDate.parse(datetime.first)
        var currentTime = LocalTime.parse(datetime.second)

        // Log current date and time for debugging
        println("Current Date: $currentDate")
        println("Current Time: $currentTime")

        // Filter events that fall within the valid date and time range
        val filteredEvents = filterEventsByDateTimeRange(dicts, currentDate, currentTime)
        println("Filtered events: ${filteredEvents.map { it["dis"] }}")

        val selectedEvent = when {
            filteredEvents.size > 1 -> {
                // Sort the filtered events to resolve overlap/priority
                val sortedEvents = sortEvents(filteredEvents)
                println("Sorted matching events: ${sortedEvents.map { it["dis"] }}")
                sortedEvents.firstOrNull()
            }

            else -> filteredEvents.firstOrNull()
        }

        // Log and return the selected event
        println("Selected event: ${selectedEvent?.get("dis")}")
        return selectedEvent
    }

    private fun filterEventsByDateTimeRange(
        events: List<HDict>,
        currentDate: LocalDate,
        currentTime: LocalTime
    ): List<HDict> {
        return events.filter { dict ->
            val range = dict["range"] as? HDict ?: return@filter false
            val startDate = (range["startDate"]?.toString())?.let { LocalDate.parse(it) }
            val endDate = (range["endDate"]?.toString())?.let { LocalDate.parse(it) }
            val startTime = (range["startTime"]?.toString())?.let { LocalTime.parse(it) }
            val endTime = (range["endTime"]?.toString())?.let { LocalTime.parse(it) }

            // Skip event if any date/time info is missing
            if (startDate == null || endDate == null || startTime == null || endTime == null) {
                println("Event skipped due to missing range info.")
                return@filter false
            }

            // Check if the current date/time is within the event's range
            val isInDateRange = currentDate in startDate..endDate
            val isInTimeRange = currentTime in startTime..endTime
            isInDateRange && isInTimeRange
        }
    }

    private fun sortEvents(events: List<HDict>): List<HDict> {
        println("Sorting ${events.size} events...")

        return events.sortedWith(
            compareBy<HDict> {
                val range = it["range"] as? HDict
                val startTime = LocalTime.parse(range?.get("startTime").toString())
                val endTime = LocalTime.parse(range?.get("endTime").toString())
                val today = LocalDate.now()

                val startDateTime = startTime.toDateTime(today.toDateTimeAtStartOfDay())
                val endDateTime = endTime.toDateTime(today.toDateTimeAtStartOfDay())
                val durationMinutes = org.joda.time.Duration(startDateTime, endDateTime).standardMinutes

                println("Event: ${it["dis"]}, Duration: $durationMinutes mins")
                durationMinutes
            }
                .thenComparator { d1, d2 ->
                    val t1 = d1[Tags.LAST_MODIFIED_TIME]?.toString()
                    val t2 = d2[Tags.LAST_MODIFIED_TIME]?.toString()
                    if (t1 != null && t2 != null) {
                        val cmp = HDateTime.make(t2).millis().compareTo(HDateTime.make(t1).millis())
                        println("Comparing by lastModifiedTime:${d1["dis"]} -> [$t1] with ${d2["dis"]} -> [$t2] -> $cmp")
                        cmp
                    } else {
                        println("lastModifiedTime missing for comparison: [$t1], [$t2]")
                        0
                    }
                }
                .thenBy {
                    val range = it[Tags.RANGE] as? HDict
                    val startTime = LocalTime.parse(range?.get("startTime").toString())
                    val today = LocalDate.now()
                    val eventStartDateTime = startTime.toDateTime(today.toDateTimeAtStartOfDay())
                    val nowDateTime = LocalDateTime.now().toDateTime()
                    val diffMillis = org.joda.time.Duration(nowDateTime, eventStartDateTime).abs().millis

                    println("Event: ${it["dis"]}, Millis to now: $diffMillis")
                    diffMillis
                }
        ).also { sorted ->
            println("Sorting completed. Order:")
            sorted.forEachIndexed { index, event ->
                val id = event["id"] ?: "unknown"
                println("  $index -> Event ID: $id")
            }
        }
    }




    /* private fun findActiveEvent(dicts: List<HDict>): HDict? {
         val localDateTime = org.joda.time.LocalDateTime.now()

         val currentDate = localDateTime.toLocalDate()
         val currentTime = localDateTime
             .withHourOfDay(18)
             .withMinuteOfHour(30)
             .withSecondOfMinute(16)
             .withMillisOfSecond(28)
             .toLocalTime()

         // Log current date and time for debugging
         println("Current Date: $currentDate")
         println("Current Time: $currentTime")

         // Filter events that fall within the valid date and time range
         val filteredEvents = filterEventsByDateTimeRange(dicts, currentDate, currentTime)
         println("Filtered events: ${filteredEvents.map { it["dis"] }}")

         val selectedEvent = when {
             filteredEvents.size > 1 -> {
                 // Sort the filtered events to resolve overlap/priority
                 val sortedEvents = sortEvents(filteredEvents)
                 println("Sorted matching events: ${sortedEvents.map { it["dis"] }}")
                 sortedEvents.firstOrNull()
             }

             else -> filteredEvents.firstOrNull()
         }

         // Log and return the selected event
         println("Selected event: ${selectedEvent?.get("dis")}")
         return selectedEvent
     }


     private fun filterEventsByDateTimeRange(
         events: List<HDict>,
         currentDate: org.joda.time.LocalDate,
         currentTime: org.joda.time.LocalTime
     ): List<HDict> {
         return events.filter { dict ->

             val range = dict["range"] as? HDict ?: return@filter false
             val startDate = (range["startDate"]?.toString())?.let { org.joda.time.LocalDate.parse(it) }
             val endDate = (range["endDate"]?.toString())?.let { org.joda.time.LocalDate.parse(it) }
             val startTime = (range["startTime"]?.toString())?.let { org.joda.time.LocalTime.parse(it) }
             val endTime = (range["endTime"]?.toString())?.let { org.joda.time.LocalTime.parse(it) }

             println(dict.get("dis").toString()+"=====Start Date: $startDate End Date: $endDate" +
                     " Start Time: $startTime End Time: $endTime")
             // Skip event if any date/time info is missing
             if (startDate == null || endDate == null || startTime == null || endTime == null) {
                 println("Event skipped due to missing range info.")
                 return@filter false
             }

             // Check if the current date/time is within the event's range
             val isInDateRange = currentDate in startDate..endDate
             val isInTimeRange = currentTime in startTime..endTime

             println("isInDateRange: $isInDateRange isInTimeRange: $isInTimeRange")
             isInDateRange && isInTimeRange
         }
     }


     private fun sortEvents(events: List<HDict>): List<HDict> {
         return events.sortedWith(
             compareBy<HDict> {
                 val range = it["range"] as? HDict
                 val startTime = LocalTime.parse(range?.get("startTime").toString())
                 val endTime = LocalTime.parse(range?.get("endTime").toString())
                 val today = LocalDate.now()

                 // Convert LocalDate + LocalTime to DateTime
                 val startDateTime = startTime.toDateTime(today.toDateTimeAtStartOfDay())
                 val endDateTime = endTime.toDateTime(today.toDateTimeAtStartOfDay())

                 // Calculate the duration in minutes using Joda Time Duration
                 org.joda.time.Duration(startDateTime, endDateTime).standardMinutes
             }
                 .thenComparator { d1, d2 ->
                     val t1 = d1[Tags.LAST_MODIFIED_TIME]?.toString()
                     val t2 = d2[Tags.LAST_MODIFIED_TIME]?.toString()
                     if (t1 != null && t2 != null)
                         HDateTime.make(t2).millis().compareTo(HDateTime.make(t1).millis())
                     else 0  // Leave unchanged if either is null
                 }
                 .thenBy {
                     val range = it[Tags.RANGE] as? HDict
                     val startTime = LocalTime.parse(range?.get("startTime").toString())
                     val today = LocalDate.now()
                     val eventStartDateTime = startTime.toDateTime(today.toDateTimeAtStartOfDay())
                     val nowDateTime = LocalDateTime.now().toDateTime()

                     // Calculate the duration to now using Joda Time Duration
                     org.joda.time.Duration(nowDateTime, eventStartDateTime).abs().millis
                 }
         )
     }*/





    /* private fun findActiveEvent(dicts: List<HDict>): HDict? {
             val  localDateTime = LocalDateTime.now()
             val currentDate = localDateTime.toLocalDate()
             var currentTime = localDateTime.toLocalTime()

             // Static current time setup (remove if dynamic is desired)
             currentTime = localDateTime.withHour(18).withMinute(30)
                 .withSecond(16).withNano(28_000_000).toLocalTime()

             println("Current Date: $currentDate")
             println("Current Time: $currentTime")

             // Filter events based on date/time range
             val filteredEvents = filterEventsByDateTimeRange(dicts, currentDate, currentTime)

             // Sort the filtered events
             val sortedEvents = sortEvents(filteredEvents)

             // Log sorted events
             println("Sorted matching events: ${sortedEvents.map { it["dis"] }}")

             val selectedEvent = sortedEvents.firstOrNull()

             println("Selected event: ${selectedEvent?.get("dis")}")

             // Return the first event or null
            return sortedEvents.firstOrNull()

     }

     private fun filterEventsByDateTimeRange(
         events: List<HDict>,
         currentDate: LocalDate,
         currentTime: LocalTime
     ): List<HDict> {
         return events.filter { dict ->
             val range = dict["range"] as? HDict ?: return@filter false
             val startDate = (range["startDate"]?.toString())?.let { LocalDate.parse(it) }
             val endDate = (range["endDate"]?.toString())?.let { LocalDate.parse(it) }
             val startTime = (range["startTime"]?.toString())?.let { LocalTime.parse(it) }
             val endTime = (range["endTime"]?.toString())?.let { LocalTime.parse(it) }

             // Skip event if any date/time info is missing
             if (startDate == null || endDate == null || startTime == null || endTime == null) {
                 println("Event skipped due to missing range info.")
                 return@filter false
             }

             // Check if the current date/time is within the event's range
             val isInDateRange = currentDate in startDate..endDate
             val isInTimeRange = currentTime in startTime..endTime
             isInDateRange && isInTimeRange
         }
     }

     private fun sortEvents(events: List<HDict>): List<HDict> {
         return events.sortedWith(
             compareBy<HDict> {
                 val range = it["range"] as? HDict
                 val startTime = LocalTime.parse(range?.get("startTime").toString())
                 val endTime = LocalTime.parse(range?.get("endTime").toString())
                 val startDateTime = LocalDateTime.of(LocalDate.now(), startTime)
                 val endDateTime = LocalDateTime.of(LocalDate.now(), endTime)
                 Duration.between(startDateTime, endDateTime).toMinutes()  // Sort by duration
             }
                 .thenComparator { d1, d2 ->
                     val t1 = d1[Tags.LAST_MODIFIED_TIME]?.toString()
                     val t2 = d2[Tags.LAST_MODIFIED_TIME]?.toString()
                     if (t1 != null && t2 != null)
                         HDateTime.make(t2).millis().compareTo(HDateTime.make(t1).millis())
                     else 0  // Leave unchanged if either is null
                 }
                 .thenBy {
                     val range = it[Tags.RANGE] as? HDict
                     val startTime = LocalTime.parse(range?.get("startTime").toString())
                     val eventStartDateTime = LocalDateTime.of(LocalDate.now(), startTime)
                     Duration.between(LocalDateTime.now(), eventStartDateTime).abs()
                         .toMillis()  // Sort by closest event to current time
                 }
         )
     }*/
}