package a75f.io.renatus.schedules

import a75f.io.renatus.R
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class DefaultScheduleEditDialog(
    private val mOnScheduleUpdateListener: ScheduleGroupFragment,
    private val scheduleGroupModel: ScheduleGroupModel
) : DialogFragment() {
    private lateinit var rootView: View
    private lateinit var saveSchedule: TextView
    private lateinit var cancelSchedule: TextView
    private lateinit var scheduleGroupFragment: ScheduleGroupFragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView =
            inflater.inflate(R.layout.fragment_default_schedule_edit_dialog, container, false)
        saveSchedule = rootView.findViewById(R.id.saveSchedule)
        cancelSchedule = rootView.findViewById(R.id.cancelSchedule)
        prepareDefaultLayout()
        saveSchedule.setOnClickListener {
            scheduleGroupModel.mSchedule.scheduleGroup = getScheduleGroup()
            scheduleGroupModel.mScheduleGroup = getScheduleGroup()
            if(mOnScheduleUpdateListener.validateSchedule(scheduleGroupModel.mSchedule,
                    scheduleGroupFragment, this, mOnScheduleUpdateListener)) {

                dismiss()
            }
        }
        cancelSchedule.setOnClickListener {
            if(mOnScheduleUpdateListener.onScheduleGroupChangeCancel(
                    getScheduleGroup(), scheduleGroupModel.mSchedule, scheduleGroupFragment)) {
                dismiss()
            }
        }
        return rootView
    }
    fun forceTrimmedSchedule() {
        mOnScheduleUpdateListener.onScheduleSave(
            scheduleGroupModel.mSchedule,
            scheduleGroupModel.mSchedule.scheduleGroup
        )
        dismiss()
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }
    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1165
            dialog.window!!.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun prepareDefaultLayout() {
        scheduleGroupFragment =
            ScheduleGroupFragment().showNamedScheduleDialogLayout(scheduleGroupModel.mSchedule)
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.defaultScheduleDialog, scheduleGroupFragment).commit()
    }

    private fun getScheduleGroup(): Int {
        return ScheduleGroupChangeOver(
            requireActivity(),
            R.id.radio_everyday,
            R.id.radio_weekday_weekend,
            R.id.radio_weekday_saturday_sunday,
            R.id.radio_seven_day,
            null
        ).getScheduleGroupById(scheduleGroupFragment.radioGroupSelectedId).ordinal
    }
}