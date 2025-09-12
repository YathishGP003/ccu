package a75f.io.logic.bo.building.pointscheduling.model

import EventDefinition
import PointDefinition
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.TimeUtil
import a75f.io.api.haystack.util.getCurrentDateTime
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.util.fetchForceOverrideLevelValueAndEndTimeIfAvailable
import a75f.io.logic.bo.util.getValueByEnum
import a75f.io.logic.bo.util.writeToPhysicalIfRequired
import a75f.io.logic.interfaces.ModbusWritableDataInterface
import org.joda.time.Duration
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
    val reconfiguredRooms = mutableListOf<String>()
    var areRoomEquipsReconfigured = false


    companion object {
        private var instance: CustomScheduleManager? = null
        var modbusWritableDataInterface: ModbusWritableDataInterface? = null
        var isWriteToDeviceReq: Boolean = false
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
        var doSendValueToDevice: Boolean
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
                        doSendValueToDevice = true
                    } else {
                        pointEntity.eventRef =
                            HList.make(
                                pointEventRef.filter { it != expiredEventRef }
                                    .map { HRef.make(it.replace("@","")) }
                            )

                        doSendValueToDevice = pointEntity.eventRef.size() != pointEntity.eventDefinitions.size()
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
                    if(doSendValueToDevice && pointEntity.scheduleRef == null) {
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "Clearing point array level 8 for point: ${pointEntity.id} as it has no scheduleRef and sending value to device"
                        )
                        haystack.clearPointArrayLevel(pointEntity.id, HayStackConstants.DEFAULT_POINT_LEVEL, false)
                        writeToPhysicalIfRequired (
                            doSendValueToDevice = true,
                            equip = haystack.readMapById(point.getStr(Tags.EQUIPREF)),
                            point = point,
                            priorityValue = haystack.readPointPriorityVal(point.id().`val`),
                            isImmediate = true
                        )
                    }
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
        CCUHsApi.getInstance().syncEntityTree()
    }

    fun processPointSchedules() {
        val rooms = haystack.readAllEntities("room")
        rooms.forEach { room ->
            val roomId = room["id"].toString()
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Processing room: ${room["dis"].toString()} and id $roomId")
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "reconfigured rooms $reconfiguredRooms")
            if(reconfiguredRooms.contains(roomId)) {
                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Room ${room["dis"].toString()} is reconfigured. id: $roomId")
                areRoomEquipsReconfigured = true
            }
            val equips = haystack
                .readAllEntities("equip and (modbus or connectModule or bacnet or pcn) " +
                        "and roomRef == \"" + room["id"].toString()+"\"")
            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Fetched ${equips.size} equip(s) for room: ${room["dis"].toString()}"
            )
            equips.forEach EquipLoop@{ equip ->
                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "fetched>>>>>>>>> Equip: ${equip["dis"].toString()}")
                // kumar-to-do  -- below query we need to change - mandate schedulable tag
                val points = haystack.readAllHDictByQuery(
                    "point and writable and schedulable and (scheduleRef or eventRef)" +
                            "and equipRef== \"${equip["id"].toString()}\""
                )
                if (points.size == 0) {
                    haystack.writeDefaultVal(
                        "point and scheduleStatus" +
                                " and equipRef== \"${equip["id"].toString()}\"",
                        "NA"
                    )
                    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "No points found for equip: ${equip["dis"].toString()}")
                    return@EquipLoop
                }
                val equipScheduleStatus = StringBuilder()
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,"\nFetched ${points.size} point(s) for equipment: '${equip["dis"]}'"
                )
                points.forEach { point ->
                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,"\n__________________________________________________\n" +
                                "point: ${point.id()}")
                    try {
                        isWriteToDeviceReq = false
                        val priorityValueLevelPair = fetchForceOverrideLevelValueAndEndTimeIfAvailable (
                            pointId = point.id().toString(),
                            unit = if (point.has("unit")) point.get("unit").toString() else "",
                            enumString = point.get("enum", false)?.toString()
                        )
                        if (point.has(Tags.EVENT_REF)) {
                            val eventProcessTime = measureTimeMillis {
                                processEvent(point, equipScheduleStatus, equip, priorityValueLevelPair)
                            }
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "Event processing time: $eventProcessTime ms for the point ${point.id()}"
                            )
                        } else if (point.has(Tags.SCHEDULE_REF)) {
                            val recurringScheduleProcessTime = measureTimeMillis {
                                processRecurringSchedule(point, equipScheduleStatus, equip, priorityValueLevelPair)
                            }
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "Recurring schedule processing time: $recurringScheduleProcessTime" +
                                        " ms for the point ${point.id()}"
                            )
                        }

                        writeToPhysicalIfRequired (
                            doSendValueToDevice = isWriteToDeviceReq,
                            equip = equip,
                            point = point,
                            priorityValue =  haystack.readPointPriorityVal(point.id().`val`),
                        )
                    } catch (e: Exception) {
                        // we don't want to crash the app if there is an error in processing this point
                        // we want to continue processing the other points
                        e.printStackTrace()
                        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "@@Error processing point: $point \n ${e.stackTrace} \n ${e.message}")
                    }
                }

                val scheduleStatusMessage = if(equipScheduleStatus.isEmpty()) "NA" else equipScheduleStatus.toString()
                haystack.writeDefaultVal(
                    "point and scheduleStatus" +
                            " and equipRef== \"${equip["id"].toString()}\"",
                    scheduleStatusMessage
                )
            }

            areRoomEquipsReconfigured = false
            reconfiguredRooms.remove(roomId)
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "updated reconfigured rooms $reconfiguredRooms")
        }
    }

    private fun processEvent(point: HDict, equipScheduleStatus: StringBuilder, equip: HashMap<Any, Any>, priorityLevelValuePair: Pair<String, String>) {
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
                processRecurringSchedule(point, equipScheduleStatus, equip, priorityLevelValuePair)
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
                    var defaultValue: Double
                    for (matchingPoint in matchingPoints) {
                        if (matchingPoint.get("id").toString() == pointId) {
                            defaultValue = haystack.readDefaultValById(pointId)
                            val unitStr =
                                if (point.has("unit")) point.get("unit").toString() else ""
                            val (updatedValue, _) = if (point.has("enum")) {
                                val enumStr = point.get("enum").toString()
                                val enum = getValueByEnum(customValue, enumStr, unitStr)
                                val customVal = enum.ifEmpty { customValue }

                                val enum2 = getValueByEnum(defaultValue, enumStr, unitStr)
                                val defaultVal = enum2.ifEmpty { defaultValue }

                                customVal to defaultVal
                            } else {
                                (customValue.toString() + unitStr) to (defaultValue.toString() + unitStr)
                            }

                            if(priorityLevelValuePair.first.isNotEmpty()) {
                                val (forceOverrideValueString, forceOverrideEndTimeString) = priorityLevelValuePair
                                equipScheduleStatus.append(
                                    (pointDis[pointDis.size - 1]) + " is " + forceOverrideValueString + ", and user intent temporary override ends on ${forceOverrideEndTimeString};\n"
                                )
                            } else {
                                equipScheduleStatus.append(
                                    (pointDis[pointDis.size - 1]) + " is " + updatedValue + ", and event ends " +
                                            "at ${formatTimeValue(eventSchedule.range.ethh)}:${
                                                formatTimeValue(
                                                    eventSchedule.range.etmm
                                                )
                                            };\n"
                                )
                            }

                            if (areRoomEquipsReconfigured) {
                                CcuLog.d(
                                    L.TAG_CCU_POINT_SCHEDULE,
                                    "reconfigured so, writing value ($customValue) to ${point.id()}, eventScheduleId: $eventScheduleId"
                                )
                                if (point.has("his")) {
                                    val priorityValue = haystack.readPointPriorityVal(pointId)
                                    haystack.writeHisValById(
                                        pointId,
                                        priorityValue
                                    )
                                }
                                haystack.writeDefaultValById(pointId, customValue)
                                isWriteToDeviceReq = true
                                valueWritten = true
                                break
                            } else if (defaultValue == customValue) {
                                CcuLog.d(
                                    L.TAG_CCU_POINT_SCHEDULE,
                                    "No change in value for pointId=$pointId, currentDefaultVal=$defaultValue  value=$customValue"
                                )
                            } else {
                                CcuLog.d(
                                    L.TAG_CCU_POINT_SCHEDULE,
                                    "writing value ($customValue) to ${point.id()}, eventScheduleId: $eventScheduleId"
                                )
                                if (point.has("his")) {
                                    val priorityValue = haystack.readPointPriorityVal(pointId)
                                    haystack.writeHisValById(
                                        pointId,
                                        priorityValue
                                    )
                                }
                                haystack.writeDefaultValById(pointId, customValue)
                                isWriteToDeviceReq = true
                                valueWritten = true
                                break
                            }
                        }
                    }
                    // if no event found for this point, write the default value
                    if (!valueWritten) {
                        defaultValue = haystack.readDefaultValById(pointId)

                        if (areRoomEquipsReconfigured) {
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "reconfigured so, writing default value ($defaultValue) to ${point.id()}"
                            )
                            if (point.has("his")) {
                                val priorityValue = haystack.readPointPriorityVal(pointId)
                                haystack.writeHisValById(
                                    pointId,
                                    priorityValue
                                )
                            }
                            haystack.writeDefaultValById(pointId, defaultValue)
                            isWriteToDeviceReq = true
                        } else if (defaultValue == customValue) {
                            CcuLog.d(
                                L.TAG_CCU_POINT_SCHEDULE,
                                "No change in value for pointId=$pointId, " +
                                        "currentDefaultVal=$defaultValue  value=$customValue"
                            )
                        } else {
                            if (point.has("his")) {
                                val priorityValue = haystack.readPointPriorityVal(pointId)
                                haystack.writeHisValById(
                                    pointId,
                                    priorityValue
                                )
                            }
                            haystack.writeDefaultValById(pointId, defaultValue)
                            isWriteToDeviceReq = true
                        }
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
        val currentTime = LocalTime.parse(datetime.second)

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

        val expiredEventRefs = mutableListOf<String>()
        val result =  events.filter { dict ->
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
            } else {
                // its passed event hence removing the eventRef and eventDefinitionRef from the point
                expiredEventRefs.add(dict["id"].toString())
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

        // its passed event hence removing the eventRef and eventDefinitionRef from the point
       val removeRefsTime = measureTimeMillis {
           removeEventRefFromPoint(
               expiredEventRefs
           )
       }

        CcuLog.d(
            L.TAG_CCU_POINT_SCHEDULE,
            "Removed expired eventRefs from points in ${removeRefsTime} ms"
        )

        return result
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
            val durationMinutes = Duration(startDateTime, endDateTime).standardMinutes

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
                Duration(nowDateTime, eventStartDateTime).abs().millis

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

    private fun processRecurringSchedule(pointDict: HDict, equipScheduleStatus: StringBuilder, equip: HashMap<Any, Any>, priorityLevelValuePair: Pair<String, String>) {
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
                val isPointScheduleExist : Map<Boolean, Day> = isPointScheduleActiveNow(pointDefinition)
                val pointID = pointDict["id"].toString()
                val pointDis = pointDict.get("dis").toString().split("-")
                val currentDefaultVal = haystack.readDefaultValById(pointID)
                if(isPointScheduleExist.containsKey(true)) {
                    val day : Day = isPointScheduleExist[true]!!
                    val value = day?.`val`.toString().toDouble()
                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "Point schedule found: pointId=$pointID, value=$value"
                    )
                    if (areRoomEquipsReconfigured) {
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "reconfigured so, writing value ($value) to ${pointID}"
                        )
                        if (pointDict.has("his")) {
                            haystack.writeHisValById(
                                pointID,
                                value
                            )
                        }
                        haystack.writeDefaultValById(pointID, value)
                        isWriteToDeviceReq = true
                    } else if (currentDefaultVal == value) {
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "No change in value for pointId=$pointID, currentDefaultVal=$currentDefaultVal  value=$value"
                        )
                    } else {
                        if (pointDict.has("his")) {
                            haystack.writeHisValById(
                                pointID,
                                value
                            )
                        }
                        haystack.writeDefaultValById(pointID, value)
                       isWriteToDeviceReq = true
                    }
                    // this below code is get the next default value if continuous schedule is found
                    // 38571 observation1
                    val pointDefinitionCustomVal = findNextValFromCurrentDay(day, pointDefinition.days)


                    var pointDefinitionDefaultVal = pointDefinition.defaultValue
                    if(pointDefinitionCustomVal != value){
                        pointDefinitionDefaultVal = pointDefinitionCustomVal!!
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "Upcoming schedule's custom value: $pointDefinitionCustomVal"
                        )
                    }

                    CcuLog.d(
                        L.TAG_CCU_POINT_SCHEDULE,
                        "pointDefinitionCustomVal : $pointDefinitionCustomVal"
                    )

                    // Converts a value and default value based on enum and unit information,
                    //falling back to the original value if conversion fails or enum is missing.

                    val unitStr = if (pointDict.has("unit")) pointDict.get("unit").toString() else ""
                    val (updatedValue, updatedDefaultVal) = if (pointDict.has("enum")) {
                        val enumStr = pointDict.get("enum").toString()
                        val enum = getValueByEnum(value, enumStr, unitStr)
                        val customVal = enum.ifEmpty { value }

                        val enum2 = getValueByEnum(pointDefinitionDefaultVal, enumStr, unitStr)
                        val defaultVal = enum2.ifEmpty { pointDefinitionDefaultVal }

                        customVal to defaultVal
                    } else {
                        (value.toString() + unitStr) to (pointDefinitionDefaultVal.toString() + unitStr)
                    }

                    if(priorityLevelValuePair.first.isNotEmpty()) {
                        val (forceOverrideValueString, forceOverrideEndTimeString) = priorityLevelValuePair
                        equipScheduleStatus.append(
                            (pointDis[pointDis.size - 1]) + " is " + forceOverrideValueString + ", and user intent temporary override ends on ${forceOverrideEndTimeString};\n"
                        )
                    } else {
                        equipScheduleStatus.append(
                            (pointDis[pointDis.size - 1]) + " is " + updatedValue + ", and changes to " + updatedDefaultVal +
                                    " at " + formatTimeValue(day?.ethh.toString()) + ":" + formatTimeValue(day?.etmm.toString()) + ";\n"
                        )
                    }

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

                    if(priorityLevelValuePair.first.isNotEmpty()) {
                        val (forceOverrideValueString, forceOverrideEndTimeString) = priorityLevelValuePair
                        equipScheduleStatus.append(
                            (pointDis[pointDis.size - 1]) + " is " + forceOverrideValueString + ", and user intent temporary override ends on ${forceOverrideEndTimeString};\n"
                        )
                    } else {
                        equipScheduleStatus.append(
                            (pointDis[pointDis.size - 1]) + " is " + updatedDefaultVal+ ", and changes to " + updatedValue +
                                    " at " + formatTimeValue(day?.sthh.toString()) + ":" + formatTimeValue(day?.stmm.toString()) + ";\n"
                        )
                    }
                    if (areRoomEquipsReconfigured) {
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "reconfigured so, writing value (${pointDefinition.defaultValue}) to ${pointID}"
                        )
                        if (pointDict.has("his")) {
                            haystack.writeHisValById(
                                pointID,
                                pointDefinition.defaultValue
                            )
                        }
                        haystack.writeDefaultValById(pointID, pointDefinition.defaultValue)
                        isWriteToDeviceReq = true
                    } else if (currentDefaultVal == pointDefinition.defaultValue) {
                        CcuLog.d(
                            L.TAG_CCU_POINT_SCHEDULE,
                            "No change in value for pointId=$pointID, currentDefaultVal=$currentDefaultVal  value=$value"
                        )
                    } else {
                        if (pointDict.has("his")) {
                            haystack.writeHisValById(
                                pointID,
                                pointDefinition.defaultValue
                            )
                        }
                        haystack.writeDefaultValById(pointID, pointDefinition.defaultValue)
                        isWriteToDeviceReq = true
                    }
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
                continue
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

            //if current day is monday (0) then previous day is sunday (6)
            // this below previous day check is for overnight schedule #40186
            val previousDay = if (currentDay == 0) 6 else currentDay - 1
            if (scheduledDay != currentDay && scheduledDay != previousDay) continue
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
                "Current total minutes: $currentTotalMinutes, " +
                        "Start total minutes: $startTotalMinutes, " +
                        "End total minutes: $endTotalMinutes, "+
                        "Scheduled day: $scheduledDay, "+
                        "Current day: $currentDay "
            )


            // overnight schedule
            if (endTotalMinutes <= startTotalMinutes) {
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "Detected overnight schedule: scheduledDay=$scheduledDay, " +
                            "previousDay=$previousDay, currentDay=$currentDay, " +
                            "startTotalMinutes=$startTotalMinutes, endTotalMinutes=$endTotalMinutes"
                )
                if (scheduledDay == previousDay && currentTotalMinutes <= endTotalMinutes) {
                    // After midnight part of yesterday’s overnight schedule
                    val value = dayItem.`val`.toString().toDouble()
                    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "overnight(previous day) value=$value")
                    return mapOf(true to dayItem)
                }

                if (scheduledDay == currentDay && currentTotalMinutes >= startTotalMinutes) {
                    // Before midnight part of today’s overnight schedule
                    val value = dayItem.`val`.toString().toDouble()
                    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "overnight(current day) value=$value")
                    return mapOf(true to dayItem)
                }

                CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "no active overnight schedule match.")
            }


            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Checking schedule: stt:stm - $startHour:$startMinute to ett:etm - $endHour:$endMinute for day $scheduledDay"
            )

            if (scheduledDay == currentDay &&
                currentTotalMinutes in startTotalMinutes..endTotalMinutes
            ) {
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
        for (offset in 0..7) {
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

    private fun findNextValFromCurrentDay(
        currentDayObj: Day,
        allDays: List<Day>
    ): Double {
        val currentDay = currentDayObj.day
        val endHour = currentDayObj.ethh
        val endMinute = currentDayObj.etmm

        val nextDay = (currentDay + 1) % 7

        val candidates = allDays.filter {
            (it.day == currentDay || it.day == nextDay) &&
                    it.sthh == endHour &&
                    it.stmm == endMinute
        }

        return candidates.firstOrNull()?.`val` ?: currentDayObj.`val`
    }

}