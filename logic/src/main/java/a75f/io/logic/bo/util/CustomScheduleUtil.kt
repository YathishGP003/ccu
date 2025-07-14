package a75f.io.logic.bo.util

import PointDefinition
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.MockTime
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.util.TimeUtil
import a75f.io.api.haystack.util.hayStack
import a75f.io.logic.bo.building.pointscheduling.model.Day
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import io.seventyfivef.ph.core.Tags
import org.joda.time.DateTime
import org.joda.time.Interval
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
        var pointStatus = singleStatusLine.trim()
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
        var scheduledInterval: Interval? = null
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
    return point.isNotEmpty() && (point.containsKey("scheduleRef") || point.containsKey("eventRef"))
}

fun fetchForceOverrideLevelValueAndEndTimeIfAvailable(pointId: String, enumString: String?, unit: String?): Triple<String, String, Double> {
    var valueString: String = ""
    var endTimeString: String = ""
    var highestPriorityValue: Double = 0.0

    var highestLevelValFound = false
    var forceOverrideLevelFound = false
    for ((index, hashMap) in hayStack.readPoint(pointId)?.withIndex() ?: emptyList()) {
        hashMap["val"]?.let { valObject ->
            if (!highestLevelValFound) {
                highestPriorityValue = valObject.toString().toDouble()
                highestLevelValFound = true
            }
            if (index == HayStackConstants.FORCE_OVERRIDE_LEVEL - 1) {
                forceOverrideLevelFound = true
                valueString = getValueByEnum(valObject.toString().toDouble(), enumString, unit)
                val date = Date(hashMap["duration"].toString().toLong())
                val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.ENGLISH)
                endTimeString = sdf.format(date)
            }
        }
        if (highestLevelValFound && forceOverrideLevelFound) {
            break
        }
    }
    return Triple(valueString, endTimeString, highestPriorityValue)
}