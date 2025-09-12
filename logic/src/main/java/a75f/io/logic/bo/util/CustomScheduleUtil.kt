package a75f.io.logic.bo.util

import PointDefinition
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.MockTime
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.util.TimeUtil
import a75f.io.api.haystack.util.hayStack
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.pointscheduling.model.CustomScheduleManager.Companion.modbusWritableDataInterface
import a75f.io.logic.bo.building.pointscheduling.model.Day
import a75f.io.logic.util.bacnet.sendWriteRequestToMstpEquip
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import io.seventyfivef.ph.core.Tags
import a75f.io.api.haystack.Tags.MODBUS
import a75f.io.api.haystack.Tags.CONNECTMODULE
import a75f.io.api.haystack.Tags.BACNET_CONFIG
import a75f.io.api.haystack.Tags.BACNET
import org.joda.time.DateTime
import org.joda.time.Interval
import org.projecthaystack.HDict
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val SCHEDULE_STATUS = "scheduleStatus"

// TODO: These methods are temporary and will mostly needed to be modified once schema for point schedules are decided.
fun isZoneUsingCustomSchedulesOrEvents(roomRef: String): Boolean {
    return CCUHsApi.getInstance().readEntity(
        "schedulable and writable and point and (scheduleRef or eventRef)" +
                " and roomRef == \"$roomRef\""
    ).isNotEmpty()
}

fun fetchPointsWithCustomScheduleOrEventByZone(roomRef: String): List<HashMap<Any, Any>> {
    return CCUHsApi.getInstance().readAllEntities(
        "schedulable and writable and point and (scheduleRef or eventRef)" +
                " and roomRef == \"$roomRef\""
    )
}

fun fetchSchedulablePointsWithoutCustomControl(roomRef: String): List<String> {
    val pointsDisName: MutableList<String> = ArrayList()
    val pointsMapList: List<HashMap<Any, Any>> = CCUHsApi.getInstance().readAllEntities(
        ("schedulable and writable and point and (not scheduleRef and not eventRef)" +
                " and equipRef and roomRef == \"" + roomRef + "\"")
    )
    if (pointsMapList.isNotEmpty()) {
        for (pointMap: HashMap<Any, Any> in pointsMapList) {
            pointsDisName.add(pointMap[Tags.DIS].toString())
        }
    }
    return pointsDisName
}

fun fetchScheduleStatusMessageForPointsUnderCustomControl(equipRef: String): List<SpannableString?> {
    val equipScheduleStatusString = CCUHsApi.getInstance().readDefaultStrVal(
        "$SCHEDULE_STATUS and ${Tags.EQUIP_REF} == \"$equipRef\""
    )

    val pointScheduleStatusList = equipScheduleStatusString.lines().filter { it.isNotBlank() }.map { singleStatusLine ->
        val pointStatus = singleStatusLine.trim()
        val regex = Regex("""\bis\b""") // Matches "is" as a word
        val match = regex.find(pointStatus)
        val boldEnd = match?.range?.first ?: 0

        SpannableString(pointStatus).apply {
            if (boldEnd > 0) {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    boldEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
    return pointScheduleStatusList
}

fun getValueByEnum(enumVal: Double, enumString: String?, unit: String?) : String{
    var valueString = ""
    enumString?.let {
        val enumStringList = enumString.split(",")
        for(kvString in enumStringList) {
            val kv = kvString.split("=")
            if (kv.size == 2) {
                val key = kv[1].trim()
                val value = kv[0].trim()
                if (key.toInt() == enumVal.toInt()) {
                    valueString = value
                    break
                }
            }
        }
    } ?: run {
        valueString = "$enumVal${ unit ?: "" }"
    }
    return valueString
}

fun formatDate(inputDate: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    val date = inputFormat.parse(inputDate)
    return if (date != null) {
        outputFormat.format(date)
    } else {
        ""
    }
}
fun formatTimeRange(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String {
    val startTime = "${formatTimeComponent(startHour)}:${formatTimeComponent(startMinute)}"
    val endTime = "${formatTimeComponent(endHour)}:${formatTimeComponent(endMinute)}"
    return "$startTime to $endTime"
}

fun formatTimeComponent(timeComponent: Int): String {
    return if (timeComponent < 10) {
        "0$timeComponent"
    } else {
        timeComponent.toString()
    }
}

fun populateIntersections(mPointDefinition: PointDefinition) {
    val scheduledIntervals: MutableList<Interval> = getScheduledIntervals(mPointDefinition.days)
    for (i in scheduledIntervals.indices) {
        for (ii in scheduledIntervals.indices) {
            if (scheduledIntervals[i].endMillis == scheduledIntervals[ii].startMillis) {
                mPointDefinition.days[i].intersection = true
            } else if (scheduledIntervals[i].end.dayOfWeek == scheduledIntervals[ii].start.dayOfWeek && scheduledIntervals[i].end.hourOfDay == scheduledIntervals[ii].start.hourOfDay && scheduledIntervals[i].end.minuteOfHour == scheduledIntervals[ii].start.minuteOfHour) {
                //Multi day schedule intersection check
                mPointDefinition.days[i].intersection = true
            }
        }
    }
}

private fun getScheduledIntervals(daysSorted: MutableList<Day>): MutableList<Interval> {
    val intervals = mutableListOf<Interval>()
    for (day in daysSorted) {
        val now = MockTime.getInstance().mockTime
        val startDateTime = DateTime(now)
            .withHourOfDay(day.sthh)
            .withMinuteOfHour(day.stmm)
            .withDayOfWeek(day.day + 1)
            .withSecondOfMinute(0)
            .withMillisOfSecond(0)
        val endDateTime = DateTime(now)
            .withHourOfDay(TimeUtil.getEndHour(day.ethh))
            .withMinuteOfHour(TimeUtil.getEndMinute(day.ethh, day.etmm))
            .withSecondOfMinute(TimeUtil.getEndSec(day.ethh)).withMillisOfSecond(0)
            .withDayOfWeek(
                day.day +
                        1
            )
        var scheduledInterval: Interval?
        scheduledInterval = if (startDateTime.isAfter(endDateTime)) {
            if (day.day == DAYS.SUNDAY.ordinal) {
                if (startDateTime.weekOfWeekyear >= 52) {
                    Interval(startDateTime, endDateTime.plusDays(1))
                } else Interval(
                    startDateTime,
                    endDateTime.withWeekOfWeekyear(startDateTime.weekOfWeekyear + 1)
                        .withDayOfWeek(DAYS.values()[day.day].nextDay.ordinal + 1)
                )
            } else {
                Interval(
                    startDateTime, endDateTime.withDayOfWeek(
                        DAYS.values()[day.day].nextDay.ordinal + 1
                    )
                )
            }
        } else {
            Interval(startDateTime, endDateTime)
        }
        intervals.add(scheduledInterval)
    }
    return intervals
}

fun formatTimeValue(value: String?): String {
    val number = value?.toDoubleOrNull()
    return number?.toInt()?.toString()?.padStart(2, '0') ?: "00"
}

fun isPointFollowingScheduleOrEvent(pointId: String): Boolean {
    val point = CCUHsApi.getInstance().readMapById(pointId)
    return point.isNotEmpty() && point.containsKey(Tags.SCHEDULABLE)
            && (point.containsKey("scheduleRef") || point.containsKey("eventRef"))
}

fun isPointFollowingScheduleOrEvent(point: Point): Boolean {
    return point.markers.contains(Tags.SCHEDULABLE)
            && (point.scheduleRef != null || point.eventRef != null)
}

fun fetchForceOverrideLevelValueAndEndTimeIfAvailable(pointId: String, enumString: String?, unit: String?): Pair<String, String> {
    var valueString = ""
    var endTimeString = ""
    var forceOverrideLevelFound = false

    for ((index, hashMap) in hayStack.readPoint(pointId)?.withIndex() ?: emptyList()) {
        hashMap["val"]?.let { valObject ->
            if (index == HayStackConstants.FORCE_OVERRIDE_LEVEL - 1) {
                forceOverrideLevelFound = true
                valueString = getValueByEnum(valObject.toString().toDouble(), enumString, unit)
                val date = Date(hashMap["duration"].toString().toDouble().toLong())
                val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.ENGLISH)
                endTimeString = sdf.format(date)
            }
        }
        if (forceOverrideLevelFound) {
            break
        }
    }
    return Pair(valueString, endTimeString)
}

private fun findExternalProfileType(equipMap: HashMap<Any, Any>) : String {
    if(equipMap.containsKey(MODBUS) && !equipMap.containsKey(CONNECTMODULE)) {
        return MODBUS
    } else if (equipMap.containsKey(BACNET_CONFIG)) {
        return BACNET
    } else if (equipMap.containsKey(CONNECTMODULE)) {
        return CONNECTMODULE
    } else {
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "No external profile type found for equipId: ${equipMap["id"]}")
        return ""
    }
}

private fun isSchedulableTagRemovedPostUpdate(newPoint: Point): Boolean {
    return !newPoint.markers.contains(Tags.SCHEDULABLE)
}

private fun isCustomScheduleRemovedPostUpdate(oldPoint: Point, newPoint: Point): Boolean {
    return oldPoint.scheduleRef != null && newPoint.scheduleRef == null
}

private fun isCustomScheduleAndEventUnavailablePostUpdate(oldPoint: Point, newPoint: Point): Boolean {
    return (oldPoint.scheduleRef != null || oldPoint.eventRef != null) &&
            (
                newPoint.scheduleRef == null &&
                        (newPoint.eventRef == null || isEventRemovedPostUpdate(oldPoint, newPoint))
            )
}

private fun isEventRemovedPostUpdate(oldPoint: Point, newPoint: Point): Boolean {
    val oldPointEventRefList = oldPoint.eventRef?.toString()?.replace("[", "")?.replace("]", "")?.split(",") ?: emptyList()
    val newPointEventRefList = newPoint.eventRef?.toString()?.replace("[", "")?.replace("]", "")?.split(",") ?: emptyList()

    return oldPointEventRefList.any { it !in newPointEventRefList }
}

private fun isExternalPointUpdatedToStopCustomControl(oldPoint: Point, newPoint: Point): Boolean {
    return isPointFollowingScheduleOrEvent(oldPoint) &&
            (isSchedulableTagRemovedPostUpdate(newPoint)
                    || isCustomScheduleRemovedPostUpdate(oldPoint, newPoint)
                    || isCustomScheduleAndEventUnavailablePostUpdate(oldPoint, newPoint))
}

fun updateWritableDataUponCustomControlChanges(newPoint: Point) {
    val localPointDict = hayStack.readHDictById(newPoint.id)
    val oldPoint = Point.Builder().setHDict(localPointDict).build()
    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Updating writable data for point: ${newPoint.id} depending on custom control changes")
    if(isExternalPointUpdatedToStopCustomControl(oldPoint, newPoint)) {
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Stopping custom control for point: ${newPoint.id} as it is no longer schedulable or has no custom schedule/event")
        hayStack.clearPointArrayLevel(newPoint.id, HayStackConstants.DEFAULT_POINT_LEVEL, false)
        writeToPhysicalIfRequired(
            doSendValueToDevice = true,
            equip = hayStack.readMapById(localPointDict.getStr("equipRef")),
            point = localPointDict,
            priorityValue = hayStack.readPointPriorityVal(newPoint.id),
            isImmediate = true
        )
    }
    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Writable data updated for point: ${newPoint.id} depending on custom control changes")
}

fun writeToPhysicalIfRequired (
    doSendValueToDevice: Boolean,
    equip: HashMap<Any, Any>,
    point: HDict,
    priorityValue: Double,
    isImmediate: Boolean = false
) {
    CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "writeToPhysicalIfRequired called for point: ${point.id()} with priority value: $priorityValue")
    val profileType = findExternalProfileType(equip)
    val pointDefaultValue: Double? = hayStack.readNullableValueByLevel(point.id().`val`, HayStackConstants.DEFAULT_POINT_LEVEL)
    val isValueOverridden = priorityValue != pointDefaultValue
    val isHisDataMismatching = hayStack.readHisValById(point.id().toString()) != priorityValue

    if(doSendValueToDevice && profileType == BACNET) {
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Writing to device for Bacnet Point: ${point.id()} with priority value: $priorityValue")
        writeToPhysical(
            profileType = profileType,
            equip = equip,
            pointDict = point,
            value = pointDefaultValue
        )
    } else if(isImmediate && (doSendValueToDevice || isValueOverridden || isHisDataMismatching)) {
        CcuLog.d(
            L.TAG_CCU_POINT_SCHEDULE, "Writing to device for Modbus or ConnectModule point: ${point.id()} with priority value: $priorityValue" +
                "\n\treason: isWriteToDeviceReq: ${doSendValueToDevice}, overridenValue: $isValueOverridden, hisDataMismatching: $isHisDataMismatching")
        writeToPhysical(
            profileType = profileType,
            equip = equip,
            pointDict = point,
            value = priorityValue
        )
    }

    if(isHisDataMismatching) {
        CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Updating history value for point: ${point.id()} with priority value: $priorityValue")
        hayStack.writeHisValById(point.id().toString(), priorityValue)
    }
}

private fun writeToPhysical (
    profileType: String,
    equip: HashMap<Any, Any>,
    pointDict: HDict,
    value: Double?,
) {
    val pointID = pointDict["id"].toString()
    when (profileType) {
        MODBUS -> {
            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Writing value to Modbus for pointId=${pointID}, value=$value"
            )
            modbusWritableDataInterface?.writeRegister(pointID)
        }
        BACNET -> {
            value?.let {
                val configMap = mutableMapOf<String, String>()
                equip["bacnetConfig"].toString().split(",").forEach { configItem ->
                    val configKeyValueList = configItem.split(":")
                    if (configKeyValueList.size == 2) {
                        configMap[configKeyValueList[0]] = configKeyValueList[1]
                    }
                }
                val priority = pointDict.getStr("defaultWriteLevel")
                val bacnetValue = value.toString()
                val isMstp = equip.containsKey("bacnetMstp")
                sendWriteRequestToMstpEquip(pointID,priority,bacnetValue,isMstp)
            } ?: run {
                CcuLog.d(
                    L.TAG_CCU_POINT_SCHEDULE,
                    "No value found to write to Bacnet for pointId=${pointID}"
                )
            }
        }
        CONNECTMODULE -> {
            val slaveId = pointDict["group"].toString().toDouble().toInt()
            val registerAddress = pointDict["registerNumber"].toString().toDouble().toInt()
            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "Writing value to Connect Module for " +
                        "pointId=${pointID}, " +
                        "slaveId=$slaveId, " +
                        "registerAddress=$registerAddress, value=$value"
            )
            modbusWritableDataInterface?.writeConnectModbusRegister(
                slaveId, registerAddress, value ?: 0.0
            )
        }
        "" -> {
            CcuLog.d(
                L.TAG_CCU_POINT_SCHEDULE,
                "No external profile type found for equipId: ${equip["id"]}"
            )
        }
    }
}