package a75f.io.renatus.schedules

import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.Schedule
import a75f.io.logic.DefaultSchedules
import a75f.io.logic.schedule.ScheduleGroup
import a75f.io.renatus.R
import android.app.Activity
import android.app.AlertDialog
import android.widget.Button
import android.widget.RadioGroup
import androidx.core.text.HtmlCompat

class ScheduleGroupChangeOver(
    private val activity: Activity,
    private val everyDayId: Int,
    private val weekDayWeekendId: Int,
    private val weekDaySaturdaySundayId: Int,
    private val sevenDayId: Int,
    private val mOnScheduleUpdateListener: ScheduleGroupFragment?
) {
    interface OnScheduleUpdate {
        fun onScheduleSave(schedule: Schedule, scheduleGroup: Int)
        fun onScheduleGroupChangeCancel(
            mScheduleGroup: Int,
            mSchedule: Schedule,
            scheduleGroupFragment: ScheduleGroupFragment
        ) : Boolean
    }

    fun showGroupChangeOverAlert(
        radioGroup: RadioGroup,
        checkedId: Int,
        schedule: Schedule,
        mScheduleGroup: Int
    ) {
        val builder = AlertDialog.Builder(activity)
        // for named schedule , read schedule group from schedule
        val scheduleGroupLocal = if(schedule.isNamedSchedule){
             schedule.scheduleGroup
        } else {
            mScheduleGroup
        }
        if(checkedId == getIdByScheduleGroup(scheduleGroupLocal)) {
            showSchedule(schedule, scheduleGroupLocal)
            return
        }
        builder.setMessage(
            HtmlCompat.fromHtml(
                "Changing the schedule group would erase the added occupied slots of <b>"
                        + ScheduleGroup.values()[schedule.scheduleGroup].group +
                        "</b> schedule group. Are you sure you want to proceed?", HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setCancelable(false)
            .setTitle("Confirmation")
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
                radioGroup.check(
                    getIdByScheduleGroup(
                        schedule.scheduleGroup
                    )
                )
            }
            .setPositiveButton("CONFIRM") { dialog, _ ->
                dialog.dismiss()
                /*while changing group erase and add fresh days*/
                schedule.days.clear()
                schedule.days.addAll(DefaultSchedules.getDefaultDays(schedule.roomRef,DAYS.MONDAY.ordinal,
                    DAYS.SUNDAY.ordinal, 8,0,17,30))
                showSchedule(schedule, getScheduleGroupById(
                    checkedId
                ).ordinal)
            }
        val alert = builder.create()
        alert.show()
    }

    private fun showSchedule(schedule: Schedule, scheduleGroup: Int) {
        mOnScheduleUpdateListener?.onScheduleSave(schedule, scheduleGroup)
    }
    fun getIdByScheduleGroup(
        scheduleGroup: Int,
    ): Int {
        return when (scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> everyDayId
            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> weekDayWeekendId
            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> weekDaySaturdaySundayId
            else -> sevenDayId
        }
    }
    fun getScheduleGroupById(
            checkId: Int,
        ): ScheduleGroup {
            return when (checkId) {
                everyDayId -> ScheduleGroup.EVERYDAY
                weekDayWeekendId -> ScheduleGroup.WEEKDAY_WEEKEND
                weekDaySaturdaySundayId -> ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY
                else -> ScheduleGroup.SEVEN_DAY
            }
        }
    fun isScheduleGroupSelectedValid(
        checkedId: Int,
        daysPresentInBuildingOccupancy: List<Int>
    ): Boolean {

        return when (checkedId) {
            everyDayId -> {
                !ScheduleUtil.isAnyDaysNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)
            }
            weekDayWeekendId -> {
                !ScheduleUtil.isWeekDaysWeekendNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)
            }
            weekDaySaturdaySundayId -> {
                !ScheduleUtil.isWeekDaysSatAndSunNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)
            }
            sevenDayId ->{
                !ScheduleUtil.isAllDaysNotPresentInBuildingOccupancy(daysPresentInBuildingOccupancy)
            }

            else -> {
                true
            }
        }
    }

    fun showInValidScheduleGroupSelectedAlert(
        checkedId: Int,
        radioGroup: RadioGroup,
        schedule: Schedule
    ) {
        if(checkedId == getIdByScheduleGroup(checkedId)) {
            showSchedule(schedule, checkedId)
            return
        }
        getScheduleGroupById(checkedId)
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(
            HtmlCompat.fromHtml(
                getWarningMessageForScheduleGroupChangeOver(checkedId), HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setCancelable(false)
        val inflater = activity.layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_button_center, null)
        builder.setView(dialogLayout)
        val alert = builder.create()
        alert.show()
        val okayButton: Button = dialogLayout.findViewById(R.id.button_okay)
        okayButton.setOnClickListener {
            alert.dismiss()
            mOnScheduleUpdateListener?.radioGroupUpdateRequired = false
            radioGroup.check(
                getIdByScheduleGroup(
                    schedule.scheduleGroup
                )
            )
            mOnScheduleUpdateListener?.radioGroupUpdateRequired = true
        }
    }
    private fun getWarningMessageForScheduleGroupChangeOver(checkedId: Int): String {
        val selectedGroup = getScheduleGroupById(checkedId).group
        val reasonForValidation = when (checkedId) {
            everyDayId -> {
                "all 7 days."
            }
            weekDayWeekendId -> {
                "all Weekdays and Weekends."
            }
            weekDaySaturdaySundayId -> {
                "all Weekdays, Saturdays and Sundays."
            }
            else -> {
                "any days"
            }
        }
        return "Changing to the <b>"+selectedGroup+"</b> schedule group is not allowed because the" +
                " building occupancy does not have occupied slots for "+reasonForValidation
    }
}