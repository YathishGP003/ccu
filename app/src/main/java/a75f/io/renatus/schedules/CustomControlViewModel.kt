package a75f.io.renatus.schedules

import PointDefinition
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.getCurrentDateTime
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.pointscheduling.model.EventSchedule
import a75f.io.logic.bo.building.pointscheduling.model.RecurringSchedule
import a75f.io.logic.bo.util.fetchPointsWithCustomScheduleOrEventByZone
import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.joda.time.LocalDate
import org.joda.time.LocalTime


class CustomControlViewModel(application: Application) : AndroidViewModel(application) {
    val isScheduleLoading = mutableStateOf(true)
    val isPointDropdownClicked = mutableStateMapOf<Int, Boolean>()
    val isDialogExpanded = mutableStateOf(false)
    val expandedDialogInfo = mutableStateMapOf<String, String>()

    val dialogTitle = mutableStateOf("")

    data class PointCustomControlInfo(
        val pointId: String,
        val pointName: String,
        val equipName: String,
        val group: String,
        val scheduleGroup: Int?,
        val scheduleName: String?,
        val pointDefinitionRef: String?,
        val eventDefinitionRefs: List<String>?,
        val enums: String?,
        val unit: String?
    )

    data class EventInfo(
        val eventId: String,
        val eventName: String,
        val eventDateTimeRange: String,
        val eventCustomValue: Double
    )

    // Under Development
    val pointCustomControlInfoMap = mutableStateListOf<PointCustomControlInfo>()
    val pointDefinitionMap = mutableStateMapOf<String, PointDefinition>()
    val eventDefinitionMap = mutableStateMapOf<String, EventInfo>()

    fun configModelView(roomRef: String) {
        viewModelScope.launch {
            populateViewModelData(roomRef)
            isScheduleLoading.value = false
        }
    }

    private fun populateViewModelData(roomRef: String) {
        val hayStack: CCUHsApi = CCUHsApi.getInstance()

        dialogTitle.value = "${ hayStack.readMapById(roomRef)[Tags.DIS]?.toString()
            ?: "Zone Name Not Found" 
        } - Schedulable Points"


        val tempPointCustomControlInfoMap = mutableListOf<PointCustomControlInfo>()
        val tempPointDefinitionMap = mutableMapOf<String, PointDefinition>()
        val tempEventDefinitionMap = mutableMapOf<String, EventInfo>()

        fetchPointsWithCustomScheduleOrEventByZone(roomRef).forEach { pointMap ->

            val pointScheduleEntity: RecurringSchedule? = pointMap[Tags.SCHEDULE_REF]?.let { scheduleRef ->
                val scheduleDict = hayStack.readHDictById(scheduleRef.toString())
                if(scheduleDict == null){
                    Toast
                        .makeText(
                            getApplication(),
                            "Schedule not found for point ${pointMap[Tags.ID]}",
                            Toast.LENGTH_LONG
                        ).show()
                    return
                }
                RecurringSchedule().dictToPointSchedule(scheduleDict)
            }
            var scheduleGroup = 0
            var scheduleName = "Unknown Schedule"
            val pointDefinitionRef = pointMap[Tags.POINT_DEFINITION_REF]?.toString()

            pointScheduleEntity?.let {
                scheduleName = it.dis ?: "Unnamed Schedule"
                pointDefinitionRef?.let { pointDefinitionRef ->
                    it.pointDefinitions.find { pointDefinition ->
                        pointDefinition.id == pointDefinitionRef
                    }?.let { foundDef ->
                        scheduleGroup = foundDef.scheduleGroup
                        tempPointDefinitionMap.putIfAbsent(pointDefinitionRef, foundDef)
                    }
                }
            }

            val eventRefs = pointMap[Tags.EVENT_REF]?.toString()?.replace("[","")
                ?.replace("]","")
                ?.split(",")
            var eventDefinitionRefs : MutableList<String>? = null
            val datetime = getCurrentDateTime()
            val currentDate = LocalDate.parse(datetime.first)
            var currentTime = LocalTime.parse(datetime.second)
            eventRefs?.let { eventIdList ->
                eventDefinitionRefs = mutableListOf()
                hayStack.readEventsDict(eventIdList).forEach outer@ { eventDict ->
                    val eventSchedule = EventSchedule().dictToEventSchedule(eventDict)
                     if(!isActiveEvent(eventSchedule, currentDate, currentTime)) return@outer
                     eventSchedule.eventDefinitions.forEach inner@ { eventDefinition ->
                         if(hayStack.readEntity("${eventDefinition.query} and id == ${pointMap[Tags.ID].toString()}").isNotEmpty()) {
                             eventDefinitionRefs!! += eventDefinition.id.toString()
                             tempEventDefinitionMap.putIfAbsent(
                                 eventDefinition.id.toString(),
                                 EventInfo(
                                     eventId = eventSchedule.id.toString(),
                                     eventName = eventSchedule.dis.toString(),
                                     eventDateTimeRange = eventSchedule.range.toString(),
                                     eventCustomValue = eventDefinition.defaultValue
                                 )
                             )?.let { return@inner }
                         }
                    }
                }
            }

            tempPointCustomControlInfoMap += PointCustomControlInfo(
                pointId = pointMap[Tags.ID].toString(),
                pointName = pointMap[Tags.SHORTDIS].toString(),
                equipName = getEquipName(hayStack.readMapById(pointMap[Tags.EQUIPREF].toString())[Tags.DIS].toString()),
                group = pointMap[Tags.GROUP].toString(),
                scheduleGroup = scheduleGroup,
                scheduleName = scheduleName,
                pointDefinitionRef = pointDefinitionRef,
                eventDefinitionRefs = eventDefinitionRefs,
                enums = pointMap["enum"]?.toString(),
                unit = pointMap["unit"]?.toString()
            )
        }

        for(i in 0 until tempPointCustomControlInfoMap.size) {
            isPointDropdownClicked[i] = false
        }

        pointCustomControlInfoMap.clear()
        pointCustomControlInfoMap.addAll(tempPointCustomControlInfoMap)

        pointDefinitionMap.clear()
        pointDefinitionMap.putAll(tempPointDefinitionMap)

        eventDefinitionMap.clear()
        eventDefinitionMap.putAll(tempEventDefinitionMap)
    }

    fun setIsDialogExpanded(isExpanded: Boolean, dialogData: Map<String, String> = mutableMapOf()) {
        isDialogExpanded.value = isExpanded
        if(isExpanded) {
            expandedDialogInfo.putAll(dialogData)
        } else {
            expandedDialogInfo.clear()
        }
    }

    private fun getEquipName(equipDis: String): String {
        val lastHyphenIndex: Int = equipDis.lastIndexOf('-')
        val secondLastHyphenIndex: Int = equipDis.lastIndexOf('-', lastHyphenIndex - 1)

        return equipDis.substring(secondLastHyphenIndex + 1, lastHyphenIndex)
    }

    private fun isActiveEvent(
        eventSchedule: EventSchedule,
        currentDate: LocalDate,
        currentTime: LocalTime
    ): Boolean {

        val startDate = eventSchedule.range.stdt?.let { LocalDate.parse(it) }
        val endDate = eventSchedule.range.etdt?.let { LocalDate.parse(it) }
        val startHour = formatTimeValue(eventSchedule.range.sthh.toString())
        var endHour = formatTimeValue(eventSchedule.range.ethh.toString())
        val startMinute = formatTimeValue(eventSchedule.range.stmm.toString())
        var endMinute = formatTimeValue(eventSchedule.range.etmm.toString())

        if (startDate == null || endDate == null ||
            startHour == null || endHour == null ||
            startMinute == null || endMinute == null
        ) {
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "Event skipped due to missing range info.")
            return false;
        }

        if (endHour == "24") {
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

        return isCurrent || isFuture
    }


    private fun formatTimeValue(value: String?): String {
        val number = value?.toDoubleOrNull()
        return number?.toInt()?.toString()?.padStart(2, '0') ?: "00"
    }
}