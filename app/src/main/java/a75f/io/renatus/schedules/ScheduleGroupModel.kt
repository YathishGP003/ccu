package a75f.io.renatus.schedules

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.Schedule
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.TimeUtil.getEndTimeHr
import a75f.io.api.haystack.util.TimeUtil.getEndTimeMin
import a75f.io.api.haystack.util.hayStack
import a75f.io.logger.CcuLog
import a75f.io.logic.DefaultSchedules
import a75f.io.logic.L
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.schedule.ScheduleGroup
import a75f.io.logic.schedule.SpecialSchedule
import a75f.io.logic.util.CommonTimeSlotFinder
import a75f.io.renatus.R
import a75f.io.renatus.schedules.ScheduleUtil.getDayString
import a75f.io.renatus.schedules.ScheduleUtil.isAllDaysNotPresentInBuildingOccupancy
import a75f.io.renatus.schedules.ScheduleUtil.isAnyDaysNotPresentInBuildingOccupancy
import a75f.io.renatus.schedules.ScheduleUtil.isWeekDaysSatAndSunNotPresentInBuildingOccupancy
import a75f.io.renatus.schedules.ScheduleUtil.isWeekDaysWeekendNotPresentInBuildingOccupancy
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.Interval
import org.projecthaystack.HDict

class ScheduleGroupModel (application: Application) : AndroidViewModel(application) {
    var mSchedule = Schedule()
    var mScheduleGroup = 0
    var mRoomRef = ""
    private val ccuHsApi: CCUHsApi = CCUHsApi.getInstance()
    fun getUnOccupiedDays(
        days: List<Schedule.Days>,
        zoneScheduleViewModel: ZoneScheduleViewModel
    ): MutableList<UnOccupiedDays> {
        val unoccupiedDays = zoneScheduleViewModel.getUnoccupiedDays(days)
        for (i in days.indices) {
            val occupiedDaysElement = days[i]
            if (occupiedDaysElement.sthh > occupiedDaysElement.ethh) {
                var unOccupiedDayIteration = 0
                while (unOccupiedDayIteration < unoccupiedDays.size) {
                    val daysElement1 = unoccupiedDays[unOccupiedDayIteration]
                    if (occupiedDaysElement.day == daysElement1.day && (occupiedDaysElement.ethh == daysElement1.sthh && occupiedDaysElement.etmm == daysElement1.stmm)) {
                        if (unoccupiedDays[unOccupiedDayIteration].day == 6 && (unOccupiedDayIteration + 1) == unoccupiedDays.size) {
                            unoccupiedDays.removeAt(unOccupiedDayIteration)
                            unoccupiedDays[0].sthh = daysElement1.sthh
                            unoccupiedDays[0].stmm = daysElement1.stmm
                        } else if ((unOccupiedDayIteration + 1 < unoccupiedDays.size && unoccupiedDays[unOccupiedDayIteration + 1].day != daysElement1.day)) {
                            unoccupiedDays.removeAt(unOccupiedDayIteration)
                            unoccupiedDays[unOccupiedDayIteration].sthh = daysElement1.sthh
                            unoccupiedDays[unOccupiedDayIteration].stmm = daysElement1.stmm
                        }
                    }
                    unOccupiedDayIteration++
                }
            }
        }
        return unoccupiedDays
    }


    fun saveSchedule(
        isResetDays: Boolean,
        commonIntervals: List<List<CommonTimeSlotFinder.TimeSlot>>?,
        defaultScheduleEditDialog: DefaultScheduleEditDialog?,
        mOnScheduleUpdateListener: ScheduleGroupFragment?
    ) {
        val mCcuHsApi = CCUHsApi.getInstance()
        if (isResetDays) {
            CommonTimeSlotFinder().trimScheduleTowardCommonTimeSlotAndSync(
                mSchedule, commonIntervals!!, mCcuHsApi
            )
        } else {
            RxjavaUtil.executeBackground {
                if (mSchedule.isZoneSchedule) {
                    mCcuHsApi.updateZoneSchedule(mSchedule, mSchedule.roomRef)
                } else {
                    mCcuHsApi.updateSchedule(mSchedule)
                }
                mCcuHsApi.syncEntityTree()
            }
        }
        defaultScheduleEditDialog?.forceTrimmedSchedule()
        mOnScheduleUpdateListener?.onScheduleSave(
            mSchedule,
            mSchedule.scheduleGroup
        )
    }

    fun deleteEntity(id: String) {
        RxjavaUtil.executeBackground {
            ccuHsApi.deleteEntity(addAtSymbol(id))
            ScheduleManager.getInstance().updateSchedules()
            ccuHsApi.syncEntityTree()
        }
    }

    fun bindData(
        mSchedule: Schedule,
        mScheduleGroup: Int,
        mRoomRef: String?
    ) {
        this.mSchedule = mSchedule
        this.mScheduleGroup = mScheduleGroup
        this.mRoomRef = mRoomRef ?: ""
    }

    private fun getWeekDayTextView(
        scheduleGroup: Int,
        textViewRow: TextView,
        textViewFirstRow: TextView
    ): TextView {
        return if (scheduleGroup != ScheduleGroup.SEVEN_DAY.ordinal) {
            textViewFirstRow
        } else {
            textViewRow
        }
    }

    private fun getSunDayTextView(
        scheduleGroup: Int?,
        textViewSeventhRow: TextView,
        textViewFirstRow: TextView,
        textViewSecondRow: TextView,
        textViewThirdRow: TextView
    ): TextView {
        return when (scheduleGroup) {
            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> return textViewSecondRow
            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> return textViewThirdRow
            ScheduleGroup.EVERYDAY.ordinal -> return textViewFirstRow
            else -> {
                textViewSeventhRow
            }
        }
    }

    private fun getSaturdayDayTextView(
        scheduleGroup: Int?,
        textViewSixthRow: TextView,
        textViewFirstRow: TextView,
        textViewSecondRow: TextView
    ): TextView {
        return when (scheduleGroup) {
            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> return textViewSecondRow
            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> return textViewSecondRow
            ScheduleGroup.EVERYDAY.ordinal -> return textViewFirstRow
            else -> {
                textViewSixthRow
            }
        }
    }

    fun shouldDrawDay(day: Int, scheduleGroup: Int): Boolean {
        return when (scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> day == 0
            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> day == 0 || day == 5
            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> day == 0 || day == 5 || day == 6
            else -> true
        }
    }

    fun getVacations(): List<Schedule> {
        val vacations = if (mRoomRef != "") {
            ccuHsApi.getZoneSchedule(mRoomRef, true)
        } else {
            ccuHsApi.getSystemSchedule(true)

        }
        if (vacations.isNotEmpty()) {
            vacations.sortWith { lhs: Schedule, rhs: Schedule ->
                lhs.startDate.compareTo(rhs.startDate)
            }
            vacations.sortWith { lhs: Schedule, rhs: Schedule ->
                lhs.endDate.compareTo(rhs.endDate)
            }

        }
        return vacations
    }

    fun getVacationsText(): Int {
        return if (mRoomRef != "") {
            R.string.zone_vacations
        } else {
            R.string.vacations
        }
    }

    fun getScheduleById(id: String?): Schedule? {
        return ccuHsApi.getScheduleById(id)
    }

    fun saveVacation(
        vacationId: String?,
        vacationName: String,
        startDate: DateTime,
        endDate: DateTime,
        requireContext: Context
    ) {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(requireContext, "Adding vacation...")
            withContext(Dispatchers.IO) {
                if (mRoomRef != "") {
                    DefaultSchedules.upsertZoneVacation(
                        vacationId, vacationName, startDate, endDate, mRoomRef
                    )
                } else {
                    DefaultSchedules.upsertVacation(
                        vacationId, vacationName, startDate, endDate
                    )
                }
                ccuHsApi.syncEntityTree()
                ScheduleManager.getInstance().updateSchedules()
            }
        }
    }

    fun getSpecialSchedule(): List<HashMap<Any, Any>>? {
        val specialScheduleList: List<HashMap<Any, Any>>? = if (mRoomRef != "") {
            ccuHsApi.getSpecialSchedules(mRoomRef)
        } else {
            ccuHsApi.getSpecialSchedules(null)
        }
        if (!specialScheduleList.isNullOrEmpty()) {
            val sortedList = specialScheduleList.sortedWith { lhs, rhs ->
                val lhsRange = lhs[Tags.RANGE] as? HDict
                val rhsRange = rhs[Tags.RANGE] as? HDict
                lhsRange!!.get(Tags.STDT).toString().compareTo(rhsRange!!.get(Tags.STDT).toString())
            }
            return sortedList
        }
        return specialScheduleList

    }

    fun getScheduleDictById(scheduleId: String): HDict {
        return ccuHsApi.getScheduleDictById(scheduleId)
    }

    fun disableCheckBoxesBasedOnBuildingOccupancy(
        radioGroups: List<RadioButton>,
        resources: Resources,
        context: Context
    ) {
        val disableStateList =
            ResourcesCompat.getColorStateList(resources, R.color.text_disabled, null)
        val leftButtonDrawable = ContextCompat.getDrawable(context, R.drawable.button_left_disabled)
        val centerButtonDrawable =
            ContextCompat.getDrawable(context, R.drawable.button_center_disabled)
        val rightButtonDrawable =
            ContextCompat.getDrawable(context, R.drawable.button_right_disabled)
        val daysPresentInBuildingOccupancy = getDaysPresentInBuildingOccupancy()
        if (isAnyDaysNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)) {
            radioGroups[0].setTextColor(disableStateList)
            radioGroups[0].background = leftButtonDrawable
        }
        if (isWeekDaysWeekendNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)) {
            radioGroups[1].setTextColor(disableStateList)
            radioGroups[1].background = centerButtonDrawable
        }
        if (isWeekDaysSatAndSunNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)) {
            radioGroups[2].setTextColor(disableStateList)
            radioGroups[2].background = centerButtonDrawable
        }
        if (isAllDaysNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)) {
            radioGroups[3].setTextColor(disableStateList)
            radioGroups[3].background = rightButtonDrawable
        }
    }

    fun saveSpecialSchedule(
        specialScheduleId: String?,
        scheduleName: String?,
        startDate: DateTime?,
        endDate: DateTime?,
        coolVal: Double,
        heatVal: Double,
        coolingUserLimitMax: Double,
        coolingUserLimitMin: Double,
        heatingUserLimitMax: Double,
        heatingUserLimitMin: Double,
        coolingDeadBand: Double,
        heatingDeadBand: Double,
        context: Context
    ) {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Adding Special Schedule...")
            withContext(Dispatchers.IO) {
                SpecialSchedule.createSpecialSchedule(
                    specialScheduleId, scheduleName, startDate, endDate, coolVal, heatVal,
                    coolingUserLimitMax, coolingUserLimitMin,
                    heatingUserLimitMax, heatingUserLimitMin,
                    coolingDeadBand, heatingDeadBand,
                    mRoomRef != "", if (mRoomRef == "") null else mRoomRef
                )
                ccuHsApi.saveTagsData()
                ScheduleManager.getInstance().updateSchedules()
                ccuHsApi.syncEntityTree()
            }
        }
    }

    fun getLabels(scheduleGroup: Int): Array<Int> {
        return when (scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                arrayOf(R.string.everyday)
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                arrayOf(R.string.weekday, R.string.weekend)
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                arrayOf(R.string.weekday, R.string.saturday, R.string.sunday)
            }

            else -> {
                arrayOf(
                    R.string.monday,
                    R.string.tuesday,
                    R.string.wednesday,
                    R.string.thursday,
                    R.string.friday,
                    R.string.saturday,
                    R.string.sunday
                )
            }
        }
    }

    fun getTextViewFromDay(
        day: DAYS,
        textViewFirstRow: TextView,
        textViewSecondRow: TextView,
        textViewThirdRow: TextView,
        textViewFourthRow: TextView,
        textViewFifthRow: TextView,
        textViewSixthRow: TextView,
        textViewSeventhRow: TextView
    ): TextView {
        return when (day) {
            DAYS.MONDAY -> getWeekDayTextView(mScheduleGroup, textViewFirstRow, textViewFirstRow)
            DAYS.TUESDAY -> getWeekDayTextView(mScheduleGroup, textViewSecondRow, textViewFirstRow)
            DAYS.WEDNESDAY -> getWeekDayTextView(mScheduleGroup, textViewThirdRow, textViewFirstRow)
            DAYS.THURSDAY -> getWeekDayTextView(mScheduleGroup, textViewFourthRow, textViewFirstRow)
            DAYS.FRIDAY -> getWeekDayTextView(mScheduleGroup, textViewFifthRow, textViewFirstRow)
            DAYS.SATURDAY -> getSaturdayDayTextView(
                mScheduleGroup,
                textViewSixthRow,
                textViewFirstRow,
                textViewSecondRow
            )

            DAYS.SUNDAY -> getSunDayTextView(
                mScheduleGroup,
                textViewSeventhRow,
                textViewFirstRow,
                textViewSecondRow,
                textViewThirdRow
            )
        }
    }

    fun getNextOccupiedSlot(
        position: Int, unOccupiedDays: UnOccupiedDays,
        size: Int, schedule: Schedule
    ): Schedule.Days {
        val occupiedDays: ArrayList<Schedule.Days> = schedule.getDays()
        if (position - unOccupiedDays.day < size) {
            if (unOccupiedDays.ethh == 24 && unOccupiedDays.etmm == 0) {
                if (occupiedDays[occupiedDays.size - 1].day <= unOccupiedDays.day) {
                    return occupiedDays[0]
                }
                for (i in occupiedDays.indices) {
                    for (j in 1..5) {
                        if (occupiedDays[i].day == unOccupiedDays.day + j) {
                            return occupiedDays[i]
                        }
                    }
                }
            }
            for (i in occupiedDays.indices) {
                if (occupiedDays[i].day == unOccupiedDays.day) {
                    if (unOccupiedDays.ethh == occupiedDays[i].sthh) {
                        if (unOccupiedDays.etmm == occupiedDays[i].stmm) {
                            return occupiedDays[i]
                        }
                    }
                }
            }
        }
        return occupiedDays[0]
    }

    fun getPositionsToBeRemoved(
        scheduleGroup: Int?,
        position: Int,
        mSchedule: Schedule
    ): List<Int> {
        val days = mSchedule.getDays()[position]
        val returnList = mutableListOf<Int>()

        when (scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                val scheduleDays = ArrayList(mSchedule.getDays())
                for(mScheduleDay in scheduleDays){
                    if ((mScheduleDay.day == DAYS.MONDAY.ordinal || mScheduleDay.day == DAYS.TUESDAY.ordinal ||
                                mScheduleDay.day == DAYS.WEDNESDAY.ordinal || mScheduleDay.day == DAYS.THURSDAY.ordinal ||
                                mScheduleDay.day == DAYS.FRIDAY.ordinal || mScheduleDay.day == DAYS.SATURDAY.ordinal || mScheduleDay.day == DAYS.SUNDAY.ordinal)
                        && mScheduleDay.sthh == days.sthh && mScheduleDay.stmm == days.stmm && mScheduleDay.ethh == days.ethh && mScheduleDay.etmm == days.etmm
                    ) {
                        returnList.add(mSchedule.getDays().indexOf(mScheduleDay))
                    }
                }

            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                val scheduleDays = ArrayList(mSchedule.getDays())
                for(mScheduleDay in scheduleDays){
                    if(days.day == DAYS.MONDAY.ordinal){
                        if((mScheduleDay.day == DAYS.MONDAY.ordinal || mScheduleDay.day == DAYS.TUESDAY.ordinal ||
                                    mScheduleDay.day == DAYS.WEDNESDAY.ordinal || mScheduleDay.day == DAYS.THURSDAY.ordinal ||
                                    mScheduleDay.day == DAYS.FRIDAY.ordinal)
                            && mScheduleDay.sthh == days.sthh && mScheduleDay.stmm == days.stmm && mScheduleDay.ethh == days.ethh && mScheduleDay.etmm == days.etmm) {
                            returnList.add(mSchedule.getDays().indexOf(mScheduleDay))
                        }
                    } else {
                        if((mScheduleDay.day == DAYS.SATURDAY.ordinal || mScheduleDay.day == DAYS.SUNDAY.ordinal)
                            && mScheduleDay.sthh == days.sthh && mScheduleDay.stmm == days.stmm && mScheduleDay.ethh == days.ethh && mScheduleDay.etmm == days.etmm) {
                            returnList.add(mSchedule.getDays().indexOf(mScheduleDay))
                        }

                    }
                }
            }
            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                val scheduleDays = ArrayList(mSchedule.getDays())
                for(mScheduleDay in scheduleDays){
                    if(days.day == DAYS.MONDAY.ordinal){
                        if((mScheduleDay.day == DAYS.MONDAY.ordinal || mScheduleDay.day == DAYS.TUESDAY.ordinal ||
                                    mScheduleDay.day == DAYS.WEDNESDAY.ordinal || mScheduleDay.day == DAYS.THURSDAY.ordinal ||
                                    mScheduleDay.day == DAYS.FRIDAY.ordinal)
                            && mScheduleDay.sthh == days.sthh && mScheduleDay.stmm == days.stmm && mScheduleDay.ethh == days.ethh && mScheduleDay.etmm == days.etmm) {
                            returnList.add(mSchedule.getDays().indexOf(mScheduleDay))
                        }
                    } else if(days.day == DAYS.SATURDAY.ordinal){
                        if((mScheduleDay.day == DAYS.SATURDAY.ordinal)
                            && mScheduleDay.sthh == days.sthh && mScheduleDay.stmm == days.stmm && mScheduleDay.ethh == days.ethh && mScheduleDay.etmm == days.etmm) {
                            returnList.add(mSchedule.getDays().indexOf(mScheduleDay))
                        }
                    }else {
                        if((mScheduleDay.day == DAYS.SUNDAY.ordinal)
                            && mScheduleDay.sthh == days.sthh && mScheduleDay.stmm == days.stmm && mScheduleDay.ethh == days.ethh && mScheduleDay.etmm == days.etmm) {
                            returnList.add(mSchedule.getDays().indexOf(mScheduleDay))
                        }
                    }
                }
            }

            else -> {
                returnList.add(position)
            }
        }
        return returnList
    }

    fun getDaysToBeUpdated(scheduleGroup: Int?, schedule: Schedule, position: Int): List<DAYS> {
        val days = schedule.getDays()[position]
        when (scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                return listOf(
                    DAYS.MONDAY,
                    DAYS.TUESDAY,
                    DAYS.WEDNESDAY,
                    DAYS.THURSDAY,
                    DAYS.FRIDAY,
                    DAYS.SATURDAY,
                    DAYS.SUNDAY
                )
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                return if (days.day == DAYS.MONDAY.ordinal) {
                    listOf(DAYS.MONDAY, DAYS.TUESDAY, DAYS.WEDNESDAY, DAYS.THURSDAY, DAYS.FRIDAY)
                } else {
                    listOf(DAYS.SATURDAY, DAYS.SUNDAY)
                }
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                return when (days.day) {
                    DAYS.MONDAY.ordinal -> {
                        listOf(
                            DAYS.MONDAY,
                            DAYS.TUESDAY,
                            DAYS.WEDNESDAY,
                            DAYS.THURSDAY,
                            DAYS.FRIDAY
                        )
                    }

                    DAYS.SATURDAY.ordinal -> {
                        listOf(DAYS.SATURDAY)
                    }

                    else -> {
                        listOf(DAYS.SUNDAY)
                    }
                }
            }

            else -> {
                return listOf(DAYS.values()[days.day])
            }
        }
    }

    fun isNamedScheduleNotInEditableMode(
        isNamedSchedulePreview: Boolean,
        isNamedDialogOpen: Boolean
    ): Boolean {
        return mSchedule.isNamedSchedule && !isNamedSchedulePreview && !isNamedDialogOpen
    }

    fun resetScheduleDays() {
        mSchedule.days.clear()
        mSchedule.days.addAll(hayStack.getScheduleById(mSchedule.id).days)
        mScheduleGroup = hayStack.getScheduleById(mSchedule.id).scheduleGroup
        mSchedule.scheduleGroup = mScheduleGroup

    }

    fun saveScheduleInModel(schedule: Schedule, scheduleGroup: Int) {
        mScheduleGroup = scheduleGroup
        mSchedule = schedule
        mSchedule.scheduleGroup = scheduleGroup
    }

    fun setScheduleLayout(
        labels: Array<Int>,
        textViews: Array<TextView>
    ) {
        for (textView in textViews.indices.sortedDescending()) {
            if (textView < labels.size) {
                textViews[textView].setText(labels[textView])
                textViews[textView].visibility = View.VISIBLE
            } else {
                textViews[textView].visibility = View.GONE
            }
        }


    }

    fun getDaysPresentInBuildingOccupancy(): List<Int> {
        val buildingOccupancy = hayStack.getBuildingOccupancy().days
        val daysInBuildingOccupancy = buildingOccupancy.map { it.day }
        val fixedDays = listOf(
            DAYS.MONDAY.ordinal, DAYS.TUESDAY.ordinal,
            DAYS.WEDNESDAY.ordinal, DAYS.THURSDAY.ordinal,
            DAYS.FRIDAY.ordinal, DAYS.SATURDAY.ordinal, DAYS.SUNDAY.ordinal
        )
        return fixedDays.filter { it in daysInBuildingOccupancy }
    }

    fun isNewGroupSelected(): Boolean {
        return mScheduleGroup != hayStack.getScheduleById(mSchedule.id).scheduleGroup
    }

    fun getOverlapDaysBasedOnScheduleGroup(daysArrayList: ArrayList<Schedule.Days>): List<String> {
        val overlapDaysList = mutableMapOf<String, MutableList<String>>()
        val daysArrayListToCheckOverlap = when (mScheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                daysArrayList.filter { it.day == DAYS.MONDAY.ordinal }
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                daysArrayList.filter { it.day == DAYS.MONDAY.ordinal || it.day == DAYS.SATURDAY.ordinal }
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                daysArrayList.filter { it.day == DAYS.MONDAY.ordinal || it.day == DAYS.SATURDAY.ordinal || it.day == DAYS.SUNDAY.ordinal }
            }

            else -> {
                daysArrayList
            }
        }
        for (day in daysArrayListToCheckOverlap) {
            val overlaps = mSchedule.getOverLapInterval(day)
            for (overlap in overlaps) {
                CcuLog.d(L.TAG_CCU_UI, " overLap $overlap")
                val dayString = getDayString(
                    overlap.start.dayOfWeek,
                    mScheduleGroup
                )
                val overLapMessage = (
                    "(${overlap.start.hourOfDay().get()}:${
                        if (overlap.start.minuteOfHour()
                                .get() == 0
                        ) "00" else overlap.start.minuteOfHour().get()
                    } - ${
                        getEndTimeHr(
                            overlap.end.hourOfDay().get(),
                            overlap.end.minuteOfHour().get()
                        )
                    }:${
                        if (getEndTimeMin(
                                overlap.end.hourOfDay().get(),
                                overlap.end.minuteOfHour().get()
                            ) == 0
                        ) "00" else overlap.end.minuteOfHour().get()
                    })"
                )
                overlapDaysList.getOrPut(dayString) { mutableListOf() }.add(overLapMessage)

            }
        }
        val resultList = overlapDaysList.map { (day, intervals) ->
            "$day ${intervals.joinToString(", ")}"
            }
        return resultList
    }

    fun forceTrimSchedule(
        daysToBeAdded: ArrayList<DAYS>?,
        uncommonIntervals: List<List<CommonTimeSlotFinder.TimeSlot>>?,
        daysArrayList: ArrayList<Schedule.Days>,
        commonTimeSlotFinder: CommonTimeSlotFinder,
        spillsMap: HashMap<String, ArrayList<Interval>>?
    ) {
        when (mScheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                commonTimeSlotFinder.forceTrimEveryDaySchedule(
                    mSchedule,
                    daysToBeAdded!!,
                    uncommonIntervals!!,
                    daysArrayList
                )
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                commonTimeSlotFinder.forceTrimWeekdayWeekEndSchedule(
                    mSchedule,
                    daysToBeAdded!!,
                    uncommonIntervals!!,
                    daysArrayList
                )
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                commonTimeSlotFinder.forceTrimWeekdaySaturdayAndSundaySchedule(
                    mSchedule,
                    daysToBeAdded!!,
                    uncommonIntervals!!,
                    daysArrayList
                )
            }

            ScheduleGroup.SEVEN_DAY.ordinal -> {
                commonTimeSlotFinder.forceTrimSevenDaySchedule(
                    mSchedule,
                    spillsMap!!
                )
            }
        }
    }

    fun getAlertTitle(): String {
        return when {
            mSchedule.isNamedSchedule -> "Named Schedule is outside Building Occupancy"
            else -> "Zone Schedule is outside Building Occupancy"
        }
    }
    private fun addAtSymbol(id: String): String {
        return if (!id.startsWith("@")) {
            "@$id"
        } else {
            id
        }
    }


    fun checkDaysTobeAddedAndDaysList(
        daysToBeAdded: ArrayList<DAYS>?,
        daysArrayList: ArrayList<Schedule.Days>,
        uncommonIntervals: List<List<CommonTimeSlotFinder.TimeSlot>>?
    ) {
        if(uncommonIntervals == null || daysToBeAdded == null){
            return
        }
        if(mScheduleGroup == ScheduleGroup.EVERYDAY.ordinal){
            if(uncommonIntervals[0].isNotEmpty()){
                if(daysToBeAdded.isEmpty()) {
                    daysToBeAdded.addAll(DAYS.getAllDays())
                }
                daysArrayList.addAll(mSchedule.days)
            }
        } else if(mScheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal){
            if(uncommonIntervals[0].isNotEmpty()){
                if(daysToBeAdded.isEmpty() || !daysToBeAdded.contains(DAYS.MONDAY)) {
                    daysToBeAdded.addAll(DAYS.getWeekDays())
                    daysArrayList.addAll(mSchedule.days.filter { it.day in DAYS.getWeekDaysOrdinal()})
                }
            }
            if(uncommonIntervals[1].isNotEmpty()){
                if(daysToBeAdded.isEmpty() || !daysToBeAdded.contains(DAYS.SATURDAY)) {
                    daysToBeAdded.addAll(DAYS.getWeekEnds())
                    daysArrayList.addAll(mSchedule.days.filter { it.day in DAYS.getWeekEndsOrdinal()})
                }
            }
        } else if(mScheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal){
            if(uncommonIntervals[0].isNotEmpty()){
                if(daysToBeAdded.isEmpty() || !daysToBeAdded.contains(DAYS.MONDAY)) {
                    daysToBeAdded.addAll(DAYS.getWeekDays())
                    daysArrayList.addAll(mSchedule.days.filter { it.day in DAYS.getWeekDaysOrdinal()})
                }
            }
            if(uncommonIntervals[1].isNotEmpty()){
                if(daysToBeAdded.isEmpty() || !daysToBeAdded.contains(DAYS.SATURDAY)) {
                    daysToBeAdded.add(DAYS.SATURDAY)
                    daysArrayList.addAll(mSchedule.days.filter { it.day == DAYS.SATURDAY.ordinal})
                }
            }
            if(uncommonIntervals[2].isNotEmpty()){
                if(daysToBeAdded.isEmpty() || !daysToBeAdded.contains(DAYS.SUNDAY)) {
                    daysToBeAdded.add(DAYS.SUNDAY)
                    daysArrayList.addAll(mSchedule.days.filter { it.day == DAYS.SUNDAY.ordinal})
                }
            }
        }
    }
}


