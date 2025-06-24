package a75f.io.logic.bo.building.pointscheduling.model

import EventDefinition
import PointDefinition
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.TimeUtil
import a75f.io.api.haystack.util.getCurrentDateTime
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.util.getValueByEnum
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import org.projecthaystack.HList
import org.projecthaystack.HRef
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.system.measureTimeMillis

class CustomScheduleManager {

    val haystack: CCUHsApi = CCUHsApi.getInstance()
    // this tells the active found for that room or not
    var activeEventAvailability: HashMap<String, Boolean> = hashMapOf()


    companion object {
        private var instance: CustomScheduleManager? = null
        fun getInstance(): CustomScheduleManager {
            if (instance == null) {
                instance = CustomScheduleManager()
            }
            return instance!!
        }
    }

    /*
    Deletes events with endDate over a year ago from today,
    and removes their eventRef from the point
    added as part of 35756.
       */
    fun cleanEvents() {
        CcuLog.i(L.TAG_CCU_POINT_SCHEDULE, "Cleaning up events older than a year")
        val events = getEvents()

        if(events.isEmpty()) {
            CcuLog.i(L.TAG_CCU_POINT_SCHEDULE, "No events found to clean up")
            return
        }

        val currentDate = TimeUtil.getCurrentDate()
        val expiredEventRefs = mutableListOf<String>()
        events.forEach { event ->
            val eventSchedule = EventSchedule().dictToEventSchedule(event)
            CcuLog.i(
                L.TAG_CCU_POINT_SCHEDULE,
                "Current date: $currentDate\n" +
                        "Event: ${event.dis()}\n" +
                        "Id: ${event.id()}\n" +
                        "End date: ${eventSchedule.range.etdt}"
            )
            val isEventExpired = isMoreThanOneYearOld(
                eventSchedule.range.etdt, currentDate
            )
            CcuLog.i(L.TAG_CCU_POINT_SCHEDULE, "Is more than one year old?: $isEventExpired")
            if (isEventExpired) {
                CcuLog.i(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "Deleting event: ${eventSchedule.dis} with id: ${eventSchedule.id}"
                )
                haystack.deleteEntity(eventSchedule.id.toString().replace("@", ""))
                expiredEventRefs.add(eventSchedule.id.toString())
            }
        }


        if(expiredEventRefs.isNotEmpty()) removeEventRefFromPoint(expiredEventRefs)
    }

    private fun isMoreThanOneYearOld(
        eventEndDate: String?, currentDate: String?
    ): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        if(eventEndDate == null || currentDate == null) {
            CcuLog.e(L.TAG_CCU_POINT_SCHEDULE, "Event end date or current date is null")
            return false
        }

        val endDate = sdf.parse(eventEndDate) ?: return false
        val currDate = sdf.parse(currentDate) ?: return false
        return Calendar.getInstance().apply {
            time = currDate
            add(Calendar.YEAR, -1)
        }.time.after(endDate)
    }

    private fun getEvents(): List<HDict> {
        return haystack.readAllHDictByQuery("event and point")
    }

    /*
    1. get all the points
    2. get the eventRef from the point
    3. check if the eventRef is in the expiredEventRefs
    4. if yes, remove the eventRef from the point
    5. also remove the eventDefinitions from the point, filter out the expiredEventRef from the eventDefinitions
       if the eventRef is present in the eventDefinitions
    5. update the point

     -- if eventRef is only one, remove the eventRefs from the point by setting it to null
     */
    private fun removeEventRefFromPoint(expiredEventRefs: MutableList<String>) {
        val points = haystack.readAllHDictByQuery("point and eventRef")
        points.forEach { point ->
            val pointEntity = Point.Builder().setHDict(point).build()
            val pointEventRef: List<String> =
                pointEntity.eventRef?.toString()?.replace("[", "")?.replace("]", "")?.split(",") ?: emptyList()
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "event refs in point: $pointEventRef")
            expiredEventRefs.forEach { expiredEventRef ->
                if (pointEventRef.contains(expiredEventRef)) {

                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "before event definition ${pointEntity.eventDefinitions} for point: ${pointEntity.id}")

                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "before updating point $point")

                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "Removing expired eventRef: $expiredEventRef from point: ${pointEntity.id}"
                    )
                    if (pointEventRef.size == 1) {
                        pointEntity.eventRef = null
                        pointEntity.eventDefinitions = null
                    } else {
                        pointEntity.eventRef =
                            HList.make(
                                pointEventRef.filter { it != expiredEventRef }
                                    .map { HRef.make(it.replace("@","")) }
                            )

                        val filteredHDicts: ArrayList<HDict> = ArrayList()

                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "pointEntity.eventDefinitions.size() = ${pointEntity.eventDefinitions.size()}" +
                                    " for point: ${pointEntity.id}"
                        )

                        for (i in 0 until pointEntity.eventDefinitions.size()) {
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "Processing event definition at index: $i for point: ${pointEntity.id}"
                            )

                            val dict = pointEntity.eventDefinitions.get(i) as? HDict ?: continue
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "Processing eventRef in event definition: ${dict["eventRef"]} for point: ${pointEntity.id}"
                            )
                            val eventRef = dict["eventRef"] as? HRef
                            if (eventRef == null) {
                                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "eventRef is null for dict: $dict")
                                continue
                            }
                            if (eventRef.toString() != expiredEventRef) {
                                CcuLog.d(
                                    L.TAG_CCU_POINT_SCHEDULE,
                                    "Keeping event definition: ${dict["eventRef"]} for point: ${pointEntity.id}"
                                )
                                filteredHDicts.add(dict)
                            }
                        }
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "Filtered event definitions: $filteredHDicts for point: ${pointEntity.id}"
                        )
                        if(filteredHDicts.isEmpty())  pointEntity.eventDefinitions = null
                        else pointEntity.eventDefinitions = HList.make(filteredHDicts)
                    }

                    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "updating event refs: ${pointEntity.eventRef} for the point: ${pointEntity.id}")
                    haystack.updatePoint(pointEntity, pointEntity.id.toString())

                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "after updating point $point")

                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "after event definition ${pointEntity.eventDefinitions} for point: ${pointEntity.id}")
                }
            }
        }
    }

    fun processPointSchedules() {
        val rooms = haystack.readAllEntities("room")
        rooms.forEach { room ->
            val equips = haystack
                .readAllEntities("equip and (modbus or connect or bacnet or pcn) and roomRef == \"" + room["id"].toString()+"\"")
            activeEventAvailability[room["id"].toString()] = false
            equips.forEach { equip ->
                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "fetched>>>>>>>>> Equip: ${equip["dis"].toString()}")
                // kumar-to-do  -- below query we need to change - mandate schedulable tag
                val points = haystack.readAllHDictByQuery(
                    "point and writable and schedulable and (scheduleRef or eventRef) " +
                            "and equipRef== \"${equip["id"].toString()}\""
                )
                if (points.size == 0) {
                    haystack.writeDefaultVal(
                        "point and scheduleStatus" +
                                " and equipRef== \"${equip["id"].toString()}\"",
                        "NA"
                    )
                    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "No points found for equip: ${equip["dis"].toString()}")
                    return@forEach
                }
                val equipScheduleStatus = StringBuilder()
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,"\nFetched ${points.size} point(s) for equipment: '${equip["dis"]}'"
                )
                points.forEach { point ->
                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE, "\npoint: ${point.id()}"
                    )
                    try {
                        if (point.has(Tags.EVENT_REF)) {
                            val eventProcessTime = measureTimeMillis {
                                processEvent(point, equipScheduleStatus)
                            }
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "Event processing time: $eventProcessTime ms for the point ${point.id()}"
                            )
                        } else if (point.has(Tags.SCHEDULE_REF)) {
                            val recurringScheduleProcessTime = measureTimeMillis {
                                processRecurringSchedule(point, equipScheduleStatus)
                            }
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "Recurring schedule processing time: $recurringScheduleProcessTime" +
                                        " ms for the point ${point.id()}"
                            )
                            activeEventAvailability[room["id"].toString()] = true
                        } else {
                            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "No point schedule or event found: $point")
                        }
                    } catch (e: Exception) {
                        // we don't want to crash the app if there is an error in processing this point
                        // we want to continue processing the other points
                        CcuLog.e(L.TAG_CCU_POINT_SCHEDULE, "Error processing point: $point", e)
                        e.printStackTrace()
                    }
                }

                val scheduleStatusMessage = if(equipScheduleStatus.isEmpty()) "NA" else equipScheduleStatus.toString()
                haystack.writeDefaultVal(
                    "point and scheduleStatus" +
                            " and equipRef== \"${equip["id"].toString()}\"",
                    scheduleStatusMessage
                )
            }

            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Check active event availability for any point in this room ${room["dis"]}: ${activeEventAvailability[room["id"].toString()]}"
            )
        }
    }

    private fun processEvent(point: HDict, equipScheduleStatus: StringBuilder) {
        val eventRefs = point[Tags.EVENT_REF].toString()
            .replace("[","")
            .replace("]","")
            .split(",")
        //get all the events
        val events = CCUHsApi.getInstance().readEventsDict(eventRefs)
        //find the active event
        val activeEvent = findActiveEvent(events, point)

        if (activeEvent== null) {
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "No active event found for point: ${point.id()}")
            // if point has eventRef but not actively available then go for point/recurring schedule if available
            if(point.has(Tags.SCHEDULE_REF)) {
                processRecurringSchedule(point, equipScheduleStatus)
            }
            return
        }
       // get the schedule object
        val eventSchedule = EventSchedule().dictToEventSchedule(activeEvent)
        val pointId = point[Tags.ID].toString()
        val equipRef = point[Tags.EQUIPREF].toString()
        val eventScheduleId = eventSchedule.id.toString()
        val pointDis = point.get(Tags.DIS).toString().split("-")


        // if point has eventDefinitions
        if (point.has("eventDefinitions")) {
            val eventDefinitions = point["eventDefinitions"] as HList

            if (eventDefinitions.size() == 0) {
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "No event definitions found for point: ${point.id()}"
                )
                return
            }

            for (i in 0 until eventDefinitions.size()) {
                val eventDefDict = eventDefinitions.get(i) as HDict
                val eventRef = eventDefDict["eventRef"]
                val eventDefinitionRef = eventDefDict["eventDefinitionRef"]

                if (eventRef.toString() == activeEvent.id().toString()) {
                    val activeEventDefinition = getActiveEventDefinition(eventSchedule, eventDefinitionRef.toString())
                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "Active event definition: $activeEventDefinition"
                    )
                    val customValue = activeEventDefinition?.defaultValue ?: 0.0
                    if (activeEventDefinition == null) {
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "No event definition found for point: ${point.id()}"
                        )
                        continue
                    }
                    // process the CUSTOM event definition
                    val hayStackQuery = buildString {
                        append("(" + activeEventDefinition.query.toString())
                        append(") and equipRef == \"$equipRef\"")
                    }

                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "query: $hayStackQuery"
                    )
                    val matchingPoints = haystack.readAllHDictByQuery(hayStackQuery)
                    var valueWritten = false
                    var defaultValue = 0.0
                    for (matchingPoint in matchingPoints) {
                        if (matchingPoint.get("id").toString() == pointId) {
                            defaultValue = haystack.readPointPriorityVal(pointId)
                            val unitStr =
                                if (point.has("unit")) point.get("unit").toString() else ""
                            val (updatedValue, updatedDefaultVal) = if (point.has("enum")) {
                                val enumStr = point.get("enum").toString()
                                val enum = getValueByEnum(customValue, enumStr, unitStr)
                                val customVal = enum.ifEmpty { customValue }

                                val enum2 = getValueByEnum(defaultValue, enumStr, unitStr)
                                val defaultVal = enum2.ifEmpty { defaultValue }

                                customVal to defaultVal
                            } else {
                                (customValue.toString() + unitStr) to (defaultValue.toString() + unitStr)
                            }

                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "writing value ($customValue) to ${point.id()}, eventScheduleId: $eventScheduleId"
                            )
                            haystack.writeDefaultValById(pointId, customValue)

                            equipScheduleStatus.append(
                                (pointDis[pointDis.size - 1]) + " is " + updatedValue + ", and event ends " +
                                        "at ${formatTimeValue(eventSchedule.range.ethh)}:${
                                            formatTimeValue(
                                                eventSchedule.range.etmm
                                            )
                                        };\n"
                            )

                            valueWritten = true
                            break
                        }
                    }
                    // if no event found for this point, write the default value
                    if (!valueWritten) {
                        defaultValue = haystack.readPointPriorityVal(pointId)
                        haystack.writeDefaultValById(pointId, defaultValue)
                    }

                }
            }
        }
    }

    private fun getActiveEventDefinition(
        eventSchedule: EventSchedule,
        eventDefId: String
    ): EventDefinition? {
        eventSchedule.eventDefinitions.forEach {
            if (it.id == eventDefId) {
                return it
            }
        }
        return null
    }


    private fun findActiveEvent(dicts: List<HDict>, point: HDict): HDict? {
        val datetime = getCurrentDateTime()

        val currentDate = LocalDate.parse(datetime.first)
        var currentTime = LocalTime.parse(datetime.second)

        // Filter events that fall within the valid date and time range
        val filteredEvents = filterEventsByDateTimeRange(dicts, currentDate, currentTime, point)
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Filtered events: ${filteredEvents.map { it["dis"] }}")

        val selectedEvent = when {
            filteredEvents.size > 1 -> {
                // Sort the filtered events to resolve overlap/priority
                val sortedEvents = sortEvents(filteredEvents)
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "Sorted matching events: ${sortedEvents.map { it["dis"] }}"
                )
                sortedEvents.firstOrNull()
            }

            else -> filteredEvents.firstOrNull()
        }

        // Log and return the selected event
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Selected event: ${selectedEvent?.get("dis")} with ID: ${selectedEvent?.get("id")}")
        return selectedEvent
    }

    private fun filterEventsByDateTimeRange(
        events: List<HDict>, currentDate: LocalDate, currentTime: LocalTime, point: HDict
    ): List<HDict> {
        return events.filter { dict ->
            val range = dict[Tags.RANGE] as? HDict ?: return@filter false

            val startDate = range[Tags.STDT]?.toString()?.let { LocalDate.parse(it) }
            val endDate = range[Tags.ETDT]?.toString()?.let { LocalDate.parse(it) }

            fun formatTimeValue(value: String?): String {
                val number = value?.toDoubleOrNull()
                return number?.toInt()?.toString()?.padStart(2, '0') ?: "00"
            }

            val startHour = formatTimeValue(range[Tags.START_HOUR]?.toString())
            var endHour = formatTimeValue(range[Tags.END_HOUR]?.toString())
            val startMinute = formatTimeValue(range[Tags.STMM]?.toString())
            var endMinute = formatTimeValue(range[Tags.ETMM]?.toString())

            if (startDate == null || endDate == null ||
                startHour == null || endHour == null ||
                startMinute == null || endMinute == null) {
                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Event skipped due to missing range info.")
                return@filter false
            }

            if(endHour == "24") {
                endHour = "23"
                endMinute = "59"
            }
            val startTime = LocalTime.parse("$startHour:$startMinute")
            val endTime = LocalTime.parse("$endHour:$endMinute")

            val isInDateRange = currentDate in startDate..endDate
            val isInTimeRange = currentTime in startTime..endTime

            val isCurrent = isInDateRange && isInTimeRange
            // Check for future event (starts later today or on a later date)
            val isFuture = currentDate < startDate ||
                    (currentDate == startDate && currentTime < startTime)

            if(isCurrent || isFuture){
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "Event is active for point: ${point[Tags.DIS]} with ID: ${dict["id"]}"
                )
                activeEventAvailability[point[Tags.ROOMREF].toString()] = true
            }

            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Event: ${dict["dis"]} Id: ${dict["id"]}  , Date Range: [$startDate, $endDate], " +
                        "Time Range: [$startTime, $endTime], " +
                        "Current Date: $currentDate, Current Time: $currentTime, " +
                        "isInDateRange?: $isInDateRange, isInTimeRange?: $isInTimeRange"
            )
            isCurrent
        }
    }

    // Sort the list of events using precedence rules
    private fun sortEvents(events: List<HDict>): List<HDict> {
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Sorting ${events.size} events...")

        return events.sortedWith(compareBy<HDict> {
            val range = it[Tags.RANGE] as? HDict ?: return@compareBy 0

            fun formatTimeValue(value: String?): String {
                val number = value?.toDoubleOrNull()
                return number?.toInt()?.toString()?.padStart(2, '0') ?: "00"
            }

            val startHour = formatTimeValue(range[Tags.START_HOUR]?.toString())
            var endHour = formatTimeValue(range[Tags.END_HOUR]?.toString())
            val startMinute = formatTimeValue(range[Tags.STMM]?.toString())
            var endMinute = formatTimeValue(range[Tags.ETMM]?.toString())

            // If endHour is "24", treat it as 23:59 (the end of the day)
            if(endHour == "24") {
                endHour = "23"
                endMinute = "59"
            }
            val startTime = LocalTime.parse("$startHour:$startMinute")
            val endTime = LocalTime.parse("$endHour:$endMinute")

            val today = LocalDate.now()

            val startDateTime = startTime.toDateTime(today.toDateTimeAtStartOfDay())
            val endDateTime = endTime.toDateTime(today.toDateTimeAtStartOfDay())

            // Calculate the duration in minutes between start and end
            val durationMinutes = org.joda.time.Duration(startDateTime, endDateTime).standardMinutes

            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE, "Event: ${it["dis"]}, Duration: $durationMinutes mins"
            )
            durationMinutes
        }.thenComparator { d1, d2 ->
            val t1 = d1[Tags.LAST_MODIFIED_TIME]?.toString()
            val t2 = d2[Tags.LAST_MODIFIED_TIME]?.toString()
            if (t1 != null && t2 != null) {
                val cmp = HDateTime.make(t2).millis().compareTo(HDateTime.make(t1).millis())
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "Comparing by lastModifiedTime:${d1["dis"]} -> [$t1] with ${d2["dis"]} -> [$t2] -> $cmp"
                )
                cmp
            } else {
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "lastModifiedTime missing for comparison: [$t1], [$t2]"
                )
                0
            }
        }.thenBy {
            val range = it[Tags.RANGE] as? HDict
            val startTime = LocalTime.parse(range?.get(Tags.START_TIME).toString())
            val today = LocalDate.now()
            val eventStartDateTime = startTime.toDateTime(today.toDateTimeAtStartOfDay())
            val nowDateTime = LocalDateTime.now().toDateTime()
            val diffMillis =
                org.joda.time.Duration(nowDateTime, eventStartDateTime).abs().millis

            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE, "Event: ${it["dis"]}, Millis to now: $diffMillis"
            )
            diffMillis
        }).also { sorted ->
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Sorting completed. Order:")
            sorted.forEachIndexed { index, event ->
                val id = event["id"] ?: "unknown"
                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "  $index -> Event ID: $id Event Dis: (${event["dis"]})")
            }
        }
    }

    private fun processRecurringSchedule(pointDict: HDict, equipScheduleStatus: StringBuilder) {
        if (!pointDict.has("pointDefinitionRef") || !pointDict.has("scheduleRef")) {
            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "no scheduleRef or pointDefinitionRef found for point: $pointDict"
            )
            return
        }

        val scheduleRef = pointDict["scheduleRef"].toString()
        val pointDefinitionRef = pointDict["pointDefinitionRef"].toString()
        val pointScheduleDict = haystack.readHDictById(scheduleRef)

        if (pointScheduleDict == null) {
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "point schedule not found: $scheduleRef")
            return
        }

        CcuLog.d(
            L.TAG_CCU_POINT_SCHEDULE,
            "Point schedule dict: ${pointScheduleDict}"
        )

        val pointSchedule = RecurringSchedule().dictToPointSchedule(pointScheduleDict)
        for (pointDefinition in pointSchedule.pointDefinitions) {
            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Fetched point definition: id=${pointDefinition.id}, ref=$pointDefinitionRef"
            )
            if (pointDefinition.id == pointDefinitionRef) {
                val isPointScheduleExist = isPointScheduleActiveNow(pointDefinition)
                val pointID = pointDict["id"].toString()
                val pointDis = pointDict.get("dis").toString().split("-")
                if(isPointScheduleExist.containsKey(true)) {
                    val day = isPointScheduleExist[true]
                    val value = day?.`val`.toString().toDouble()
                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "Point schedule found: pointId=$pointID, value=$value"
                    )

                    haystack.writeDefaultValById(pointID, value)

                    // Converts a value and default value based on enum and unit information,
                    //falling back to the original value if conversion fails or enum is missing.

                    val unitStr = if (pointDict.has("unit")) pointDict.get("unit").toString() else ""
                    val (updatedValue, updatedDefaultVal) = if (pointDict.has("enum")) {
                        val enumStr = pointDict.get("enum").toString()
                        val enum = getValueByEnum(value, enumStr, unitStr)
                        val customVal = enum.ifEmpty { value }

                        val enum2 = getValueByEnum(pointDefinition.defaultValue, enumStr, unitStr)
                        val defaultVal = enum2.ifEmpty { pointDefinition.defaultValue }

                        customVal to defaultVal
                    } else {
                        (value.toString() + unitStr) to (pointDefinition.defaultValue.toString() + unitStr)
                    }

                    equipScheduleStatus.append(
                        (pointDis[pointDis.size - 1]) + " is " + updatedValue + ", and changes to " + updatedDefaultVal +
                                " at " + formatTimeValue(day?.ethh.toString()) + ":" + formatTimeValue(day?.etmm.toString()) + ";\n"
                    )
                } else {
                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "No active point schedule found for pointId=$pointID. " +
                                "Applying default value: ${pointDefinition.defaultValue}"
                    )

                    val calendar = Calendar.getInstance()
                    val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) - 2).let { if (it < 0) 6 else it }
                    val currentMins = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                    val day = upcomingSlot(currentDay, currentMins, pointDefinition.days)

                    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE,"Next occupied slot: $day")

                    val value = day?.`val`.toString().toDouble()
                    val unitStr = if (pointDict.has("unit")) pointDict.get("unit").toString() else ""
                    val (updatedValue, updatedDefaultVal) = if (pointDict.has("enum")) {
                        val enumStr = pointDict.get("enum").toString()
                        val enum = getValueByEnum(value, enumStr, unitStr)
                        val customVal = enum.ifEmpty { value }

                        val enum2 = getValueByEnum(pointDefinition.defaultValue, enumStr, unitStr)
                        val defaultVal = enum2.ifEmpty { pointDefinition.defaultValue }

                        customVal to defaultVal
                    } else {
                        (value.toString() + unitStr) to (pointDefinition.defaultValue.toString() + unitStr)
                    }

                    equipScheduleStatus.append(
                        (pointDis[pointDis.size - 1]) + " is " + updatedDefaultVal+ ", and changes to " + updatedValue +
                                " at " + formatTimeValue(day?.sthh.toString()) + ":" + formatTimeValue(day?.stmm.toString()) + ";\n"
                    )
                    haystack.writeDefaultValById(pointID, pointDefinition.defaultValue)
                }
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "Point schedule processed: pointId=$pointID, default value =${pointDefinition.defaultValue}"
                )
                break
            } else {
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "Ref check failed - pointId=${pointDict.id()}," +
                            " pointDefinitionRef=$pointDefinitionRef," +
                            " actualId=${pointDefinition.id}"
                )
            }
        }
    }

    private fun isPointScheduleActiveNow(pointDef: PointDefinition): Map<Boolean, Day> {

        val dis = pointDef.dis
        val id = pointDef.id
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Processing pointDefinitionId: $id, display name: $dis")

        val calendar = Calendar.getInstance()
        val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) - 2).let { if (it < 0) 6 else it }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        CcuLog.d(
            L.TAG_CCU_POINT_SCHEDULE,
            "Current day/time: Day=$currentDay, Time=%02d:%02d".format(currentHour, currentMinute)
        )
        val days = pointDef.days as? ArrayList ?: return mapOf(false to Day())
        val result = mutableMapOf(false to Day())

        for (i in 0 until days.size) {
            val dayItem = days[i]

            val scheduledDay = dayItem.day.toString().toInt()
            if (scheduledDay != currentDay) continue
            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Processing day item: ${dayItem.day}," +
                        " start: ${dayItem.sthh}:${dayItem.stmm}," +
                        " end: ${dayItem.ethh}:${dayItem.etmm}"
            )
            val startHour = dayItem.sthh.toString().toDouble().toInt()
            val startMinute = dayItem.stmm.toString().toDouble().toInt()
            val endHour = dayItem.ethh.toString().toDouble().toInt()
            val endMinute = dayItem.etmm.toString().toDouble().toInt()

            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute

            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Checking schedule: stt:stm - $startHour:$startMinute to ett:etm - $endHour:$endMinute for day $scheduledDay"
            )

            if (currentTotalMinutes in startTotalMinutes..endTotalMinutes) {
                val value = dayItem.`val`.toString().toDouble()
                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "custom value found. value=$value")
                return mapOf(true to dayItem)
            }
        }

        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "No active schedule found.")
        return result
    }


    private fun createEquipScheduleStatusPoint(equip: Equip, roomRef: String, floorRef: String) {

        val equipScheduleStatus = Point.Builder()
            .setDisplayName(equip.displayName + "-equipScheduleStatus")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef).setHisInterpolate("cov")
            .addMarker("scheduleStatus").addMarker("logical").addMarker("modbus").addMarker("zone").addMarker("writable")
            .addMarker("his")
            .addMarker("point")
            .setTz(equip.tz)
            .setKind(Kind.STRING)
            .build()
        CCUHsApi.getInstance().addPoint(equipScheduleStatus)

    }

    private fun formatTimeValue(value: String?): String {
        val number = value?.toDoubleOrNull()
        return number?.toInt()?.toString()?.padStart(2, '0') ?: "00"
    }

    private fun upcomingSlot(currentDay: Int, currentMins: Int, list: List<Day>): Day? {
        for (offset in 0..6) {
            val dayToCheck = (currentDay + offset) % 7
            val slots = list.filter { it.day == dayToCheck }
                .sortedBy { it.sthh * 60 + it.stmm }

            for (slot in slots) {
                val startMins = slot.sthh * 60 + slot.stmm
                if (offset > 0 || startMins > currentMins) {
                    return slot
                }
            }
        }
        return null
    }

}