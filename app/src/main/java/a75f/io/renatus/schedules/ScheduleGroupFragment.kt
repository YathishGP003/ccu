package a75f.io.renatus.schedules

import PointDefinition
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.MockTime
import a75f.io.api.haystack.Schedule
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.DefaultSchedules
import a75f.io.logic.L
import a75f.io.logic.bo.building.pointscheduling.model.Day
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.bo.util.UnitUtils.roundToHalf
import a75f.io.logic.bo.util.formatTimeRange
import a75f.io.logic.bo.util.getValueByEnum
import a75f.io.logic.bo.util.populateIntersections
import a75f.io.logic.schedule.ScheduleGroup
import a75f.io.logic.schedule.SpecialSchedule
import a75f.io.logic.util.CommonTimeSlotFinder
import a75f.io.logic.util.isOfflineMode
import a75f.io.renatus.R
import a75f.io.renatus.util.FontManager
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.views.AlertDialogAdapter
import a75f.io.renatus.views.AlertDialogData
import a75f.io.renatus.views.MasterControl.MasterControlUtil.validateDesiredTemp
import a75f.io.renatus.views.RangeBar
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.joda.time.DateTime
import org.joda.time.Interval

const val ID_DIALOG_OCCUPIED_SCHEDULE = 1
const val ID_DIALOG_UN_OCCUPIED_SCHEDULE = 2
class ScheduleGroupFragment(schedule: Schedule?, scheduleGroup: Int?) : DialogFragment(),
    ScheduleGroupChangeOver.OnScheduleUpdate, ZoneScheduleDialogFragment.ZoneScheduleDialogListener,
    UnOccupiedZoneSetBackDialogFragment.UnOccupiedZoneSetBackListener  {
    private var isGroupShifted: Boolean = false
    private var isZoneNeedsToBeTrimmedOnDefaultZoneSchedule: Boolean = true
    var radioGroupUpdateRequired: Boolean = true
    private var zoneScheduleViewModel: ZoneScheduleViewModel = ZoneScheduleViewModel()

    private var mDrawableBreakLineLeft: Drawable? = null
    private var mDrawableBreakLineRight: Drawable? = null
    private var mDrawableTimeMarker: Drawable? = null
    private var mPixelsBetweenAnHour = 0f
    private var mPixelsBetweenADay = 0f

    private lateinit var textViewFirstRow: TextView
    private lateinit var textViewSecondRow: TextView
    private lateinit var textViewThirdRow: TextView
    private lateinit var textViewFourthRow: TextView
    private lateinit var textViewFifthRow: TextView
    private lateinit var textViewSixthRow: TextView
    private lateinit var textViewSeventhRow: TextView
    private lateinit var textViewScheduleTitle: TextView
    private lateinit var cancelZoneSchedule: TextView
    private lateinit var saveZoneSchedule: TextView
    private lateinit var textViewVacations: TextView
    private lateinit var textViewAddVacations: Button
    private lateinit var textViewAddSpecialSchedule: Button
    private lateinit var addEntry : TextView
    private lateinit var mScheduleGroupTitle : TextView

    private lateinit var view00: View
    private lateinit var view02: View
    private lateinit var view04: View
    private lateinit var view06: View
    private lateinit var view08: View
    private lateinit var view10: View
    private lateinit var view12: View
    private lateinit var view14: View
    private lateinit var view16: View
    private lateinit var view18: View
    private lateinit var view20: View
    private lateinit var view22: View
    private lateinit var view24: View
    private lateinit var view01: View
    private lateinit var view03: View
    private lateinit var view05: View
    private lateinit var view07: View
    private lateinit var view09: View
    private lateinit var view11: View
    private lateinit var view13: View
    private lateinit var view15: View
    private lateinit var view17: View
    private lateinit var view19: View
    private lateinit var view21: View
    private lateinit var view23: View

    private lateinit var specialScheduleTitle: LinearLayout
    private lateinit var vacationTitle: LinearLayout
    private lateinit var zoneScheduleLayout: LinearLayout
    private lateinit var radioGroupLayout: LinearLayout
    private lateinit var cancelUpdateLayout: LinearLayout
    private lateinit var headerLayout: LinearLayout

    private lateinit var constraintSpecialScheduleLayout: ConstraintLayout
    private lateinit var constraintVacationLayout: ConstraintLayout
    private lateinit var constraintScheduler: ConstraintLayout

    lateinit var viewTimeLines: ArrayList<View>
    private lateinit var scheduleScrollView: NestedScrollView
    lateinit var rootView: View
    private lateinit var radioGroup: RadioGroup
    private lateinit var mVacationRecycler: RecyclerView
    private lateinit var specialScheduleRecycler: RecyclerView

    private var heatingDesiredTempColor: String = ""
    private var coolingDesiredTempColor: String = ""
    private var customValueColor: String = ""
    private var mSchedule = schedule
    private var mScheduleGroup = scheduleGroup
    private var mPointDefinition: PointDefinition? = null
    private var mEnumStringForPointSchedule: String? = null
    private var mUnitForPointSchedule: String? = null
    var radioGroupSelectedId = -1
    private var isSpecialSchedule = false
    private var isVacationSchedule = false
    private var mRoomRef: String? = null
    private var isNamedDialogOpen: Boolean = false
    private var currentTimeMarker: AppCompatImageView? = null
    private lateinit var scheduleGroupModel :ScheduleGroupModel
    private var isNamedSchedulePreview: Boolean = false
    private lateinit var everyDayRadioButton: RadioButton
    private lateinit var weekDayWeekendRadioButton: RadioButton
    private lateinit var weekDaySaturdaySundayRadioButton: RadioButton
    private lateinit var sevenDayRadioButton: RadioButton

    var pointScheduleUiClicked: ((Map<String, String>) -> Unit)? = null

    fun showSpecialScheduleLayout(roomRef: String, schedule: Schedule): ScheduleGroupFragment {
        isSpecialSchedule = true
        isVacationSchedule = false
        mRoomRef = roomRef
        mSchedule = schedule
        mScheduleGroup = schedule.scheduleGroup
        return this
    }
    fun showVacationsLayout(roomRef: String, schedule: Schedule): ScheduleGroupFragment {
        isSpecialSchedule = false
        isVacationSchedule = true
        mRoomRef = roomRef
        mSchedule = schedule
        mScheduleGroup = schedule.scheduleGroup
        return this
    }
    fun showNamedScheduleDialogLayout(schedule: Schedule): ScheduleGroupFragment {
        mSchedule = schedule
        mScheduleGroup = schedule.scheduleGroup
        isNamedDialogOpen = true
        return this
    }

    //This is default constructor call's from scheduling tab
    constructor() : this(CCUHsApi.getInstance().defaultNamedSchedule, CCUHsApi.getInstance().defaultNamedSchedule?.scheduleGroup){
        mSchedule = CCUHsApi.getInstance().defaultNamedSchedule
        mScheduleGroup = mSchedule?.scheduleGroup
    }
    fun showNamedSchedulePreviewLayout(schedule: Schedule): ScheduleGroupFragment {
        mSchedule = schedule
        mScheduleGroup = mSchedule!!.scheduleGroup
        isNamedSchedulePreview = true
        return this
    }
    fun showPointCustomSchedulePreviewLayout(scheduleGroup: Int?, pointDefinition: PointDefinition, enumString: String?, unit: String?): ScheduleGroupFragment {
        mScheduleGroup = scheduleGroup
        mPointDefinition = pointDefinition
        mEnumStringForPointSchedule = enumString
        mUnitForPointSchedule = unit
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.schedule_group_layout, container, false)
        scheduleGroupModel= ViewModelProvider(this)[ScheduleGroupModel::class.java]
        /*At the time of site creating Default named schedule is null*/
        if (mSchedule == null || mScheduleGroup == null){
             initZoneSpecialAndVacationLayout()
                showScheduleLayout(View.GONE, View.VISIBLE, View.VISIBLE)
                loadVacations()
                loadSpecialSchedules()
            return rootView
        }
            mPointDefinition?.let {
                initialiseSchedulerLayout(rootView)
                scheduleGroupModel.bindCustomControlData(mScheduleGroup!!, mRoomRef)
                addViewTimeLines()
                prepareCustomSchedulePreviewLayout()
                customValueColor = resources.getString(0 + R.color.white)
                customValueColor = "#" + customValueColor.substring(3)
                return rootView
            }

        scheduleGroupModel.bindData(mSchedule!!, mScheduleGroup!!, mRoomRef)

        initialiseSchedulerLayout(rootView)
        when {
            isSpecialSchedule -> {
                prepareSpecialScheduleLayout()
            }
            isVacationSchedule -> {
                prepareVacationsLayout()
            }
            else -> {
                if (scheduleGroupModel.mSchedule.isNamedSchedule && !isNamedDialogOpen && !isNamedSchedulePreview) {
                    prepareSchedulingTabLayout()
                } else if (scheduleGroupModel.mSchedule.isNamedSchedule && isNamedDialogOpen && !isNamedSchedulePreview) {
                    prepareNamedScheduleOpenDialog()
                } else if (isNamedSchedulePreview && scheduleGroupModel.mSchedule.isNamedSchedule) {
                    prepareNamedSchedulePreviewLayout()
                }
                else {
                    prepareZoneScheduleLayout()
                }
                addViewTimeLines()
                drawTemperatureColorsAndDrawScheduleLayout()
            }
        }
        scheduleGroupModel.disableCheckBoxesBasedOnBuildingOccupancy(
            listOf(everyDayRadioButton, weekDayWeekendRadioButton,
                weekDaySaturdaySundayRadioButton, sevenDayRadioButton), resources, requireContext()
        )
        drawScheduleLayoutBasedOnGroup(scheduleGroupModel.mScheduleGroup)

        return rootView
    }

    override fun onScheduleSave(schedule: Schedule, scheduleGroup: Int) {
        scheduleGroupModel.saveScheduleInModel(schedule, scheduleGroup)
        val dialog = dialog
        if (dialog != null) {
            val width = 1165
            dialog.window!!.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        updateUI()
        if(scheduleGroupModel.mSchedule.isNamedSchedule) {
            mScheduleGroupTitle.text = ScheduleGroup.values()[scheduleGroupModel.mScheduleGroup].group
        }
        isZoneNeedsToBeTrimmedOnDefaultZoneSchedule = true
    }

    override fun onScheduleGroupChangeCancel(
        mScheduleGroup: Int,
        mSchedule: Schedule,
        scheduleGroupFragment: ScheduleGroupFragment
    ): Boolean {
        return handleCancelButton(mScheduleGroup, mSchedule, scheduleGroupFragment)
    }

    override fun onStart() {
        super.onStart()
        setupLayout(dialog, rootView)
    }

    private fun setupLayout(dialog: Dialog?, rootView: View) {
        if (dialog != null) {
            val width = 1165
            dialog.window!!.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        // For named schedule preview layout we dont need to set padding for ScheduleGroup fragment
        if(!isNamedSchedulePreview || isSpecialSchedule || isVacationSchedule) {
            val paddingLeft = 40
            val paddingTop = 38
            rootView.setPadding(paddingLeft, paddingTop, rootView.paddingRight, rootView.paddingBottom)
        }
    }

    private fun drawTemperatureColorsAndDrawScheduleLayout() {
        radioGroup.check(
            ScheduleGroupChangeOver(
                requireActivity(),
                R.id.radio_everyday,
                R.id.radio_weekday_weekend,
                R.id.radio_weekday_saturday_sunday,
                R.id.radio_seven_day,
                null
            ).getIdByScheduleGroup(scheduleGroupModel.mScheduleGroup)
        )
        heatingDesiredTempColor = resources.getString(0 + R.color.min_temp)
        heatingDesiredTempColor = "#" + heatingDesiredTempColor.substring(3)
        coolingDesiredTempColor = resources.getString(0 + R.color.max_temp)
        coolingDesiredTempColor = "#" + coolingDesiredTempColor.substring(3)
        setGlobalLayoutListener()
    }

    private fun setGlobalLayoutListener() {
        val vto = constraintScheduler.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                constraintScheduler.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val viewHourOne = viewTimeLines[1]
                val viewHourTwo = viewTimeLines[2]

                mPixelsBetweenAnHour = viewHourTwo.x - viewHourOne.x

                // Maintain standard padding of 48.0f between two days
                mPixelsBetweenADay = 48.0f
                if (mPixelsBetweenAnHour.toDouble() != 0.0) {
                    updateUI()
                }
            }
        })
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(mSchedule != null && mScheduleGroup != null) {
            initialiseListeners()
        }
        initialiseSpecialAndVacationListeners()
        mPointDefinition?.let {
            view.doOnPreDraw {
                view.post {
                    mPixelsBetweenAnHour = viewTimeLines[2].x - viewTimeLines[1].x
                    // Maintain standard padding of 48.0f between two days
                    mPixelsBetweenADay = 48.0f
                    view.setPadding(0, 0, 0, 0)
                    updateCustomUI()
                }
            }
        }
    }

    private fun initialiseSpecialAndVacationListeners() {
        textViewAddVacations.setOnClickListener {
            showVacationDialog(null)
        }

        textViewAddSpecialSchedule.setOnClickListener {
            showSpecialScheduleDialog(scheduleGroupModel.mRoomRef, null)
        }
    }

    private fun initialiseListeners() {
        saveZoneSchedule.setOnClickListener {
            scheduleGroupModel.mSchedule.scheduleGroup = ScheduleGroupChangeOver(
                requireActivity(),
                R.id.radio_everyday,
                R.id.radio_weekday_weekend,
                R.id.radio_weekday_saturday_sunday,
                R.id.radio_seven_day,
                null
            ).getScheduleGroupById(radioGroup.checkedRadioButtonId).ordinal
            validateSchedule(scheduleGroupModel.mSchedule, this, null, null)
        }

        cancelZoneSchedule.setOnClickListener {
            handleCancelButton(scheduleGroupModel.mScheduleGroup, scheduleGroupModel.mSchedule, this)
        }

        radioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, checkedId ->
            isGroupShifted = true
            if (radioGroupSelectedId == checkedId || (scheduleGroupModel.mSchedule.isNamedSchedule && !isNamedDialogOpen)) {
                return@OnCheckedChangeListener
            }
            if(!radioGroupUpdateRequired){
                radioGroupSelectedId = checkedId
                return@OnCheckedChangeListener
            }
            val daysPresentInBuildingOccupancy = scheduleGroupModel.getDaysPresentInBuildingOccupancy()
            if(ScheduleGroupChangeOver(requireActivity(), R.id.radio_everyday, R.id.radio_weekday_weekend,
                    R.id.radio_weekday_saturday_sunday, R.id.radio_seven_day, this)
                .isScheduleGroupSelectedValid(checkedId, daysPresentInBuildingOccupancy)) {
                radioGroupSelectedId = checkedId
                ScheduleGroupChangeOver(
                    requireActivity(), R.id.radio_everyday, R.id.radio_weekday_weekend,
                    R.id.radio_weekday_saturday_sunday, R.id.radio_seven_day, this
                ).showGroupChangeOverAlert(
                    radioGroup, checkedId, scheduleGroupModel.mSchedule, scheduleGroupModel.mScheduleGroup
                )
            } else {
                radioGroupSelectedId = checkedId
                ScheduleGroupChangeOver(requireActivity(), R.id.radio_everyday, R.id.radio_weekday_weekend,
                    R.id.radio_weekday_saturday_sunday, R.id.radio_seven_day, this)
                    .showInValidScheduleGroupSelectedAlert(checkedId, radioGroup, scheduleGroupModel.mSchedule)
            }
        })
        radioGroupSelectedId = radioGroup.checkedRadioButtonId


        addEntry.setOnClickListener {
            if(scheduleGroupModel.mSchedule.isNamedSchedule && !isNamedDialogOpen) {
                val schedulerFragment = DefaultScheduleEditDialog(this,
                    scheduleGroupModel)
                val childFragmentManager = parentFragmentManager
                childFragmentManager.beginTransaction().commit()
                schedulerFragment.show(childFragmentManager, "dialog")
            } else if ((scheduleGroupModel.mSchedule.isNamedSchedule && !isNamedSchedulePreview) || scheduleGroupModel.mSchedule.isZoneSchedule){
                val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
                val prev: Fragment? = parentFragmentManager.findFragmentByTag("popup")
                if (prev != null) {
                    ft.remove(prev)
                }
                val zoneScheduleDialogFragment = ZoneScheduleDialogFragment(this, scheduleGroupModel.mSchedule)
                zoneScheduleDialogFragment.show(ft, "popup")
            }
        }
    }



    fun validateSchedule(
        schedule: Schedule,
        scheduleGroupFragment: ScheduleGroupFragment,
        defaultScheduleEditDialog: DefaultScheduleEditDialog?,
        mOnScheduleUpdateListener: ScheduleGroupFragment?
    ) : Boolean {
        val commonTimeSlotFinder = CommonTimeSlotFinder()
        val commonIntervals = commonTimeSlotFinder.getCommonTimeSlot(
            schedule.scheduleGroup,
            hayStack.getSystemSchedule(false)[0].days,
            scheduleGroupModel.mSchedule.days,  ((scheduleGroupModel.isNewGroupSelected() && scheduleGroupFragment.isZoneNeedsToBeTrimmedOnDefaultZoneSchedule) || (isGroupShifted && isZoneNeedsToBeTrimmedOnDefaultZoneSchedule))
        )
        val uncommonIntervals = commonTimeSlotFinder.getUnCommonTimeSlot(schedule.scheduleGroup, commonIntervals, scheduleGroupModel.mSchedule.days)
        if (commonTimeSlotFinder.isUncommonIntervalsHasAnySpills(uncommonIntervals)) {
            val spillZones = commonTimeSlotFinder.getSpilledZones(scheduleGroupModel.mSchedule, uncommonIntervals)
            val message = HtmlCompat.fromHtml(
                "Force trim will erase the following time slot(s) of  <b>"
                        + ScheduleGroup.values()[scheduleGroupModel.mScheduleGroup].group +
                        "</b> schedule group. Are you sure you want to proceed?",
                HtmlCompat.FROM_HTML_MODE_LEGACY)

            AlertDialogAdapter(requireContext(), AlertDialogData(
                scheduleGroupModel.getAlertTitle(),
                message,
                spillZones,
                "Force-Trim & Save",
                {
                    scheduleGroupModel.saveSchedule(
                        true,
                        commonIntervals,
                        defaultScheduleEditDialog,
                        null
                    )
                    closeFragment(scheduleGroupFragment)
                },
                "Cancel",
                {
                    // Do nothing
                },
                isOnlyNegativeButton = false,
                isCancelable = false,
                icon = R.drawable.ic_dialog_alert
            )).showCustomDialog()
            return false
        } else {
            scheduleGroupModel.saveSchedule(false, null,
                null, mOnScheduleUpdateListener)

            closeFragment(scheduleGroupFragment)
            return true
        }
    }


    private fun handleCancelButton(
        scheduleGroup: Int,
        schedule: Schedule,
        scheduleGroupFragment: ScheduleGroupFragment
    ): Boolean {
        val savedScheduleGroup = hayStack.getScheduleById(schedule.id).scheduleGroup
        val selectedRadioGroupId = ScheduleGroupChangeOver(
            requireActivity(),
            R.id.radio_everyday,
            R.id.radio_weekday_weekend,
            R.id.radio_weekday_saturday_sunday,
            R.id.radio_seven_day,
            null
        ).getIdByScheduleGroup(scheduleGroup)
        val savedRadioGroupId = ScheduleGroupChangeOver(
            requireActivity(),
            R.id.radio_everyday,
            R.id.radio_weekday_weekend,
            R.id.radio_weekday_saturday_sunday,
            R.id.radio_seven_day,
            null
        ).getIdByScheduleGroup(savedScheduleGroup)
        return if(selectedRadioGroupId != savedRadioGroupId){
            scheduleGroupModel.resetScheduleDays()
            scheduleGroupFragment.radioGroup.check(savedRadioGroupId)
            updateUI()
            false
        } else {
            closeFragment(scheduleGroupFragment)
            scheduleGroupModel.resetScheduleDays()
            true
        }
    }

    private fun addViewTimeLines() {
        viewTimeLines = arrayListOf(
            view00, view01, view02, view03, view04, view05, view06, view07, view08, view09, view10,
            view11, view12, view13, view14, view15, view16, view17, view18, view19, view20, view21,
            view22, view23, view24
        )
    }

    private fun initialiseSchedulerLayout(rootView: View) {
        constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler)
        scheduleScrollView = rootView.findViewById(R.id.scheduleScrollView)
        scheduleScrollView.post { scheduleScrollView.smoothScrollTo(0, 0) }

        mDrawableBreakLineLeft = AppCompatResources.getDrawable(
            requireContext(), R.drawable.ic_break_line_left_svg
        )
        mDrawableBreakLineRight = AppCompatResources.getDrawable(
            requireContext(), R.drawable.ic_break_line_right_svg
        )
        mDrawableTimeMarker =
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_time_marker_svg)
        //Week Days
        radioGroup = rootView.findViewById(R.id.radioGroup)
        cancelZoneSchedule = rootView.findViewById(R.id.cancelZoneSchedule)
        saveZoneSchedule = rootView.findViewById(R.id.saveZoneSchedule)
        initZoneSpecialAndVacationLayout()

        //Scheduler Layout

        addEntry = rootView.findViewById(R.id.addEntry)
        radioGroupLayout = rootView.findViewById(R.id.radioGroupLayout)
        cancelUpdateLayout = rootView.findViewById(R.id.cancel_update)
        mScheduleGroupTitle = rootView.findViewById(R.id.scheduleGroupTitle)
        headerLayout = rootView.findViewById(R.id.headerLayout)

        textViewFirstRow = rootView.findViewById(R.id.textViewMonday)
        textViewSecondRow = rootView.findViewById(R.id.textViewTuesday)
        textViewThirdRow = rootView.findViewById(R.id.textViewWednesday)
        textViewFourthRow = rootView.findViewById(R.id.textViewThursday)
        textViewFifthRow = rootView.findViewById(R.id.textViewFriday)
        textViewSixthRow = rootView.findViewById(R.id.textViewSaturday)
        textViewSeventhRow = rootView.findViewById(R.id.textViewSunday)
        textViewScheduleTitle = rootView.findViewById(R.id.scheduleTitle)

        //Time lines with 2 hrs Interval 00:00 to 24:00
        view00 = rootView.findViewById(R.id.view00)
        view02 = rootView.findViewById(R.id.view02)
        view04 = rootView.findViewById(R.id.view04)
        view06 = rootView.findViewById(R.id.view06)
        view08 = rootView.findViewById(R.id.view08)
        view10 = rootView.findViewById(R.id.view10)
        view12 = rootView.findViewById(R.id.view12)
        view14 = rootView.findViewById(R.id.view14)
        view16 = rootView.findViewById(R.id.view16)
        view18 = rootView.findViewById(R.id.view18)
        view20 = rootView.findViewById(R.id.view20)
        view22 = rootView.findViewById(R.id.view22)
        view24 = rootView.findViewById(R.id.view24)

        //Time lines with 1hr Interval 00:00 to 24:00
        view01 = rootView.findViewById(R.id.view01)
        view03 = rootView.findViewById(R.id.view03)
        view05 = rootView.findViewById(R.id.view05)
        view07 = rootView.findViewById(R.id.view07)
        view09 = rootView.findViewById(R.id.view09)
        view11 = rootView.findViewById(R.id.view11)
        view13 = rootView.findViewById(R.id.view13)
        view15 = rootView.findViewById(R.id.view15)
        view17 = rootView.findViewById(R.id.view17)
        view19 = rootView.findViewById(R.id.view19)
        view21 = rootView.findViewById(R.id.view21)
        view23 = rootView.findViewById(R.id.view23)

        everyDayRadioButton = rootView.findViewById(R.id.radio_everyday)
        weekDayWeekendRadioButton = rootView.findViewById(R.id.radio_weekday_weekend)
        weekDaySaturdaySundayRadioButton = rootView.findViewById(R.id.radio_weekday_saturday_sunday)
        sevenDayRadioButton = rootView.findViewById(R.id.radio_seven_day)
    }

    private fun initZoneSpecialAndVacationLayout() {
        zoneScheduleLayout = rootView.findViewById(R.id.zoneScheduleLayout)

        textViewVacations = rootView.findViewById(R.id.vacationsTitle)
        textViewAddVacations = rootView.findViewById(R.id.addVacations)
        textViewAddSpecialSchedule = rootView.findViewById(R.id.addSpecialSchedule)

        mVacationRecycler = rootView.findViewById(R.id.vacationRecycler)
        specialScheduleTitle = rootView.findViewById(R.id.special_header_title)
        specialScheduleRecycler = rootView.findViewById(R.id.specialScheduleRecycler)

        constraintSpecialScheduleLayout = rootView.findViewById(R.id.constraintLtSpecialSchedule)
        constraintVacationLayout = rootView.findViewById(R.id.constraintLt_Vacations)
        vacationTitle = rootView.findViewById(R.id.vacations_header_title)
    }

    private fun drawScheduleLayoutBasedOnGroup(scheduleGroup: Int) {
        val textViews = arrayOf(textViewFirstRow, textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow, textViewSixthRow, textViewSeventhRow)
        scheduleGroupModel.setScheduleLayout(scheduleGroupModel.getLabels(scheduleGroup), textViews)
    }


    private fun drawCurrentTime() {
        // Remove the existing time marker if it exists
        currentTimeMarker?.let { constraintScheduler.removeView(it) }

        val now = DateTime(MockTime.getInstance().mockTime)
        val day = DAYS.values()[now.dayOfWeek - 1]
        val hh = now.hourOfDay
        val mm = now.minuteOfHour
        val imageView = AppCompatImageView(requireActivity())
        imageView.setImageResource(R.drawable.ic_time_marker_svg)
        imageView.id = View.generateViewId()
        imageView.scaleType = ImageView.ScaleType.FIT_XY
        val lp = ConstraintLayout.LayoutParams(0, mPixelsBetweenADay.toInt())
        lp.bottomToBottom = scheduleGroupModel.getTextViewFromDay(day, textViewFirstRow,
            textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow,
            textViewSixthRow, textViewSeventhRow).id
        lp.startToStart = viewTimeLines[hh].id
        lp.leftMargin = ((mm / 60.0) * mPixelsBetweenAnHour).toInt()
        constraintScheduler.addView(imageView, lp)
        currentTimeMarker = imageView
    }

    private fun hasTextViewChildren() {
        for (i in constraintScheduler.childCount - 1 downTo 0) {
            if (constraintScheduler.getChildAt(i).tag != null) {
                constraintScheduler.removeViewAt(i)
            }
        }
    }

    private fun updateUI() {
        scheduleGroupModel.mSchedule.populateIntersections()
        Handler(Looper.getMainLooper()).post {
            hasTextViewChildren()
            val days: List<Schedule.Days> = scheduleGroupModel.mSchedule.days
            days.sortedBy { it.sthh }
            days.sortedBy { it.day }

            val unOccupiedDays = scheduleGroupModel.getUnOccupiedDays(days, ZoneScheduleViewModel())
            drawScheduleLayoutBasedOnGroup(scheduleGroupModel.mScheduleGroup)
            drawUnoccupiedDaysSchedule(unOccupiedDays, scheduleGroupModel.mScheduleGroup)
            drawOccupiedDaysSchedule(days, scheduleGroupModel.mScheduleGroup)
            drawCurrentTime()
        }
    }

    private fun drawOccupiedDaysSchedule(
        days: List<Schedule.Days>,
        scheduleGroup: Int?
    ) {
        for (i in days.indices) {
            val daysElement = days[i]
            if (scheduleGroupModel.shouldDrawDay(daysElement.day, scheduleGroup ?: ScheduleGroup.EVERYDAY.ordinal)) {
                drawSchedule(
                    i,
                    daysElement.heatingVal,
                    daysElement.coolingVal,
                    daysElement.sthh,
                    daysElement.ethh,
                    daysElement.stmm,
                    daysElement.etmm,
                    DAYS.values()[daysElement.day],
                    daysElement.isIntersection,
                    true
                )
            }
        }
    }

    private fun drawUnoccupiedDaysSchedule(
        unOccupiedDays: MutableList<UnOccupiedDays>,
        scheduleGroup: Int
    ) {
        for (i in unOccupiedDays.indices) {
            val daysElement = unOccupiedDays[i]
            if (scheduleGroupModel.shouldDrawDay(daysElement.day, scheduleGroup)) {
                drawSchedule(
                    daysElement.day, 0.0, 0.0, daysElement.sthh, daysElement.ethh,
                    daysElement.stmm, daysElement.etmm,
                    DAYS.values()[daysElement.day], daysElement.isIntersection, false
                )
            }
        }
    }

    private fun drawSchedule(
        position: Int, heatingTemp: Double, coolingTemp: Double, startTimeHH: Int,
        endTimeHH: Int, startTimeMM: Int, endTimeMM: Int, day: DAYS,
        intersection: Boolean, isOccupied: Boolean
    ) {
        var heatingTempLocal = heatingTemp
        var coolingTempLocal = coolingTemp
        var unit = "\u00B0F"
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            coolingTempLocal =
                roundToHalf(UnitUtils.fahrenheitToCelsius(coolingTempLocal).toFloat()).toDouble()
            heatingTempLocal =
                roundToHalf(UnitUtils.fahrenheitToCelsius(heatingTempLocal).toFloat()).toDouble()
            unit = "\u00B0C"
        }

        val strMinTemp = FontManager.getColoredSpanned(coolingTempLocal.toString() + unit, heatingDesiredTempColor)
        val strMaxTemp = FontManager.getColoredSpanned(heatingTempLocal.toString() + unit, coolingDesiredTempColor)

        var typeface = Typeface.DEFAULT
        try {
            typeface = Typeface.createFromAsset(requireActivity().assets, "font/lato_bold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (startTimeHH > endTimeHH || (startTimeHH == endTimeHH && startTimeMM > endTimeMM)) {
            drawScheduleBlock(
                position, strMinTemp, strMaxTemp, typeface, startTimeHH,
                24, startTimeMM, 0,
                scheduleGroupModel.getTextViewFromDay(day, textViewFirstRow,
                    textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow,
                    textViewSixthRow, textViewSeventhRow),
                leftBreak = false,
                rightBreak = true,
                intersection = intersection,
                isOccupied = isOccupied
            )
            drawScheduleBlock(
                position, strMinTemp, strMaxTemp, typeface, 0,
                endTimeHH, 0, endTimeMM,
                scheduleGroupModel.getTextViewFromDay(day.nextDay, textViewFirstRow,
                    textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow,
                    textViewSixthRow, textViewSeventhRow),
                leftBreak = true, rightBreak = false, intersection = intersection, isOccupied = isOccupied
            )
        } else {
            drawScheduleBlock(
                position, strMinTemp, strMaxTemp,
                typeface, startTimeHH, endTimeHH, startTimeMM,
                endTimeMM, scheduleGroupModel.getTextViewFromDay(day, textViewFirstRow,
                    textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow,
                    textViewSixthRow, textViewSeventhRow),
                leftBreak = false, rightBreak = false, intersection = intersection, isOccupied = isOccupied
            )
        }
    }

    private fun drawScheduleBlock(
        position: Int, strMinTemp: String, strMaxTemp: String, typeface: Typeface?,
        tempStartTime: Int, tempEndTime: Int,
        startTimeMM: Int, endTimeMM: Int, textView: TextView?,
        leftBreak: Boolean, rightBreak: Boolean, intersection: Boolean, isOccupied: Boolean
    ) {
        var tempEndTimeLocal = tempEndTime
        CcuLog.d(
            L.TAG_CCU_UI,
            "position: " + position + " tempStartTime: " + tempStartTime + " tempEndTime: " + tempEndTimeLocal + " startTimeMM: " + startTimeMM + " endTimeMM " + endTimeMM + "isOccupied " + isOccupied
        )

        if (context == null) return
        val textViewTemp = AppCompatTextView(
            requireContext()
        )
        textViewTemp.gravity = Gravity.CENTER_HORIZONTAL
        if (isOccupied) {
            textViewTemp.text = Html.fromHtml("$strMinTemp $strMaxTemp", Html.FROM_HTML_MODE_LEGACY)
        }
        if (typeface != null) textViewTemp.typeface = typeface
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            textViewTemp,
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        textViewTemp.maxLines = 2
        textViewTemp.contentDescription =
            textView!!.text.toString() + "_" + tempStartTime + ":" + startTimeMM + "-" + tempEndTimeLocal + ":" + endTimeMM
        textViewTemp.id = ViewCompat.generateViewId()
        textViewTemp.tag = position

        val lp = ConstraintLayout.LayoutParams(0, mPixelsBetweenADay.toInt())
        lp.bottomToBottom = textView.id


        val leftMargin =
            if (startTimeMM > 0) ((startTimeMM / 60.0) * mPixelsBetweenAnHour).toInt() else lp.leftMargin
        val rightMargin =
            if (endTimeMM > 0) (((60 - endTimeMM) / 60.0) * mPixelsBetweenAnHour).toInt() else lp.rightMargin

        lp.leftMargin = leftMargin
        lp.rightMargin = rightMargin

        val drawableCompat: Drawable?

        if (leftBreak) {
            drawableCompat = ResourcesCompat.getDrawable(resources,R.drawable.occupancy_background, requireContext().theme)
            if (intersection) {
                val rightGreyBar = ResourcesCompat.getDrawable(resources,R.drawable.vline, requireContext().theme)
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                    mDrawableBreakLineLeft,
                    null,
                    rightGreyBar,
                    null
                )
            } else textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                mDrawableBreakLineLeft,
                null,
                null,
                null
            )

            val space = Space(activity)
            space.id = View.generateViewId()
            val px =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)

            val spaceLP = ConstraintLayout.LayoutParams(px.toInt(), 10)
            spaceLP.rightToLeft = viewTimeLines[tempStartTime].id
            constraintScheduler.addView(space, spaceLP)
            if (endTimeMM > 0) tempEndTimeLocal++
            lp.startToStart = space.id
            lp.endToEnd = viewTimeLines[tempEndTimeLocal].id
        } else if (rightBreak) {
            drawableCompat = ResourcesCompat.getDrawable(resources,R.drawable.occupancy_background, requireContext().theme)
            textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                mDrawableBreakLineRight,
                null
            )
            val space = Space(activity)
            space.id = View.generateViewId()
            val px =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
            val spaceLP = ConstraintLayout.LayoutParams(px.toInt(), 10)
            spaceLP.leftToRight = viewTimeLines[tempEndTimeLocal].id
            constraintScheduler.addView(space, spaceLP)
            lp.startToStart = viewTimeLines[tempStartTime].id
            lp.endToEnd = space.id
        } else {
            if (intersection) {
                val rightGreyBar = ResourcesCompat.getDrawable(resources,R.drawable.vline, requireContext().theme)
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                    null, null,
                    rightGreyBar, null
                )
            }
            drawableCompat = ResourcesCompat.getDrawable(resources,
                if (isOccupied) R.drawable.occupancy_background else R.drawable.occupancy_background_unoccupied,
                null
            )
            if (endTimeMM > 0) tempEndTimeLocal++
            lp.startToStart = viewTimeLines[tempStartTime].id
            lp.endToEnd = viewTimeLines[tempEndTimeLocal].id
        }
        textViewTemp.background = drawableCompat
        constraintScheduler.addView(textViewTemp, lp)
        textViewTemp.setOnClickListener { v ->
            if(scheduleGroupModel.isNamedScheduleNotInEditableMode(isNamedSchedulePreview, isNamedDialogOpen)){
                return@setOnClickListener
            }
            val clickedPosition = v.tag as Int
            val days: ArrayList<Schedule.Days> = scheduleGroupModel.mSchedule.getDays()
            try {
                days.sortWith { lhs: Schedule.Days, rhs: Schedule.Days -> lhs.sthh - (rhs.sthh) }
                days.sortWith { lhs: Schedule.Days, rhs: Schedule.Days -> lhs.day - (rhs.day) }
                if (isOccupied) {
                    showDialog(ID_DIALOG_OCCUPIED_SCHEDULE, clickedPosition, scheduleGroupModel.mSchedule)
                } else {
                    showDialog(ID_DIALOG_UN_OCCUPIED_SCHEDULE, clickedPosition, scheduleGroupModel.mSchedule)
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                CcuLog.e("Schedule", "onClick: " + e.message)
            }
        }
    }

    private fun showDialog(id: Int, position: Int, schedule: Schedule) {
        val ft = parentFragmentManager.beginTransaction()

        fun removeFragment(fragmentManager: FragmentManager, tag: String) {
            val fragmentTransaction = fragmentManager.beginTransaction()
            val existingFragment = fragmentManager.findFragmentByTag(tag)
            if (existingFragment != null) {
                fragmentTransaction.remove(existingFragment)
                fragmentTransaction.commit()
            }
        }

        fun sortDays(days: List<Schedule.Days>) {
            days.sortedWith { lhs: Schedule.Days, rhs: Schedule.Days -> lhs.sthh - (rhs.sthh) }
        }

        when (id) {
            ID_DIALOG_OCCUPIED_SCHEDULE -> {
                if (scheduleGroupModel.mSchedule.isNamedSchedule && isNamedSchedulePreview) {
                        scheduleGroupModel.mSchedule.id?.let { scheduleId ->
                            removeFragment(childFragmentManager, "popup")
                            val namedScheduleDialogFragment =
                                NamedScheduleDialogFragment.newInstance(scheduleId, position)
                            namedScheduleDialogFragment.show(
                                childFragmentManager.beginTransaction(),
                                "popup"
                            )
                            try {
                                sortDays(schedule.days)
                            } catch (e: ArrayIndexOutOfBoundsException) {
                                CcuLog.e(L.TAG_CCU_SCHEDULER, e.message)
                            }
                        }
                } else {
                    schedule.scheduleGroup = scheduleGroupModel.mScheduleGroup
                    val occupiedDays = schedule.days[position]
                    removeFragment(parentFragmentManager, "popup")
                    val newFragment = ZoneScheduleDialogFragment(this, position, occupiedDays, schedule)
                    newFragment.show(ft, "popup")
                }
            }

            ID_DIALOG_UN_OCCUPIED_SCHEDULE -> {
                removeFragment(parentFragmentManager, "popup")
                if (scheduleGroupModel.mSchedule.isNamedSchedule && isNamedSchedulePreview) {
                    val namedScheduleUnoccupiedDialog =
                        NamedScheduleUnoccupiedDailog.newInstance(scheduleGroupModel.mSchedule.id, position)
                    namedScheduleUnoccupiedDialog.show(childFragmentManager.beginTransaction(), "popup")
                } else {
                    val days: List<Schedule.Days> = schedule.days
                    sortDays(days)
                    val unoccupiedDays: List<UnOccupiedDays> =
                        scheduleGroupModel.getUnOccupiedDays(days, zoneScheduleViewModel)
                    val unOccupiedDays = unoccupiedDays[position]
                    if (schedule.days.size == 0) return
                    val nextDay: Schedule.Days =
                        scheduleGroupModel.getNextOccupiedSlot(
                            position, unOccupiedDays,
                            schedule.days.size, scheduleGroupModel.mSchedule
                        )
                    val unOccupiedZoneSetBackDialogFragment =
                        UnOccupiedZoneSetBackDialogFragment(this, nextDay, schedule)
                    unOccupiedZoneSetBackDialogFragment.show(ft, "popup")
                }
            }
        }
    }


    private fun prepareZoneScheduleLayout() {
        showScheduleLayout(View.VISIBLE, View.GONE, View.GONE)
        mScheduleGroupTitle.visibility = View.GONE
        cancelUpdateLayout.visibility = View.VISIBLE
    }

    private fun prepareNamedScheduleOpenDialog() {
        radioGroupLayout.visibility = View.VISIBLE
        cancelUpdateLayout.visibility = View.VISIBLE
        constraintSpecialScheduleLayout.visibility = View.GONE
        specialScheduleTitle.visibility = View.GONE
        constraintVacationLayout.visibility = View.GONE
        vacationTitle.visibility = View.GONE
        mScheduleGroupTitle.visibility = View.GONE
        cancelUpdateLayout.visibility = View.GONE
        textViewScheduleTitle.text = getString(R.string.default_schedule_title, CCUHsApi.getInstance().siteName)
    }

    private fun prepareSchedulingTabLayout() {
        if (isOfflineMode()){
            textViewScheduleTitle.text = getString(R.string.default_schedule_title, CCUHsApi.getInstance().siteName)
            addEntry.text = getString(R.string.edit_schedule)
            mScheduleGroupTitle.text = ScheduleGroup.values()[scheduleGroupModel.mScheduleGroup].group
            cancelUpdateLayout.visibility = View.GONE
            radioGroupLayout.visibility = View.GONE
            mScheduleGroupTitle.visibility = View.VISIBLE
            showScheduleLayout(View.VISIBLE, View.VISIBLE, View.VISIBLE)
        } else{
            showScheduleLayout(View.GONE, View.VISIBLE, View.VISIBLE)
        }

        loadVacations()
        loadSpecialSchedules()
    }
    private fun prepareVacationsLayout() {
        loadVacations()
        showScheduleLayout(View.GONE, View.GONE, View.VISIBLE)
    }
    private fun prepareSpecialScheduleLayout() {
        showScheduleLayout(View.GONE, View.VISIBLE, View.GONE)
        specialScheduleRecycler.minimumHeight = 200
        loadSpecialSchedules()
    }
    private fun prepareNamedSchedulePreviewLayout() {
        showScheduleLayout(View.VISIBLE, View.GONE, View.GONE)
        headerLayout.visibility = View.GONE
        cancelUpdateLayout.visibility = View.GONE
        radioGroupLayout.visibility = View.GONE
    }
    private fun loadSpecialSchedules() {
        val specialScheduleList = scheduleGroupModel.getSpecialSchedule()
        setSpecialScheduleAdapter(specialScheduleList)
    }
    private fun setSpecialScheduleAdapter(specialScheduleList: List<HashMap<Any, Any>>?) {
        if (specialScheduleList != null) {
            val specialScheduleAdapter = SpecialScheduleAdapter(specialScheduleList,
                { view: View ->
                    val id = view.tag.toString()
                    showDeleteSpecialScheduleAlert(id)
                },
                { view: View ->
                    val id = view.tag.toString()
                    showSpecialScheduleDialog(scheduleGroupModel.mRoomRef, id)
                })
            specialScheduleRecycler.setAdapter(specialScheduleAdapter)
            specialScheduleRecycler.setLayoutManager(LinearLayoutManager(activity))
        }
    }
    private fun showDeleteSpecialScheduleAlert(scheduleId: String) {
        val specialSchedule = scheduleGroupModel.getScheduleDictById(scheduleId)
        val alertDialog = Dialog(requireActivity())
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        alertDialog.setCancelable(false)
        alertDialog.setContentView(R.layout.dialog_delete_schedule)
        val messageTv = alertDialog.findViewById<TextView>(R.id.tvMessage)
        val specialScheduleConfirmation = requireActivity().getString(R.string.special_schedule_confirmation, specialSchedule[Tags.DIS])
        messageTv.text = specialScheduleConfirmation

        alertDialog.findViewById<View>(R.id.btnCancel)
            .setOnClickListener { alertDialog.dismiss() }
        alertDialog.findViewById<View>(R.id.btnProceed).setOnClickListener {
            scheduleGroupModel.deleteEntity(scheduleId)
            alertDialog.dismiss()
            loadSpecialSchedules()
            ProgressDialogUtils.hideProgressDialog()

        }
        alertDialog.show()
    }
    private fun loadVacations() {
        val vacations = scheduleGroupModel.getVacations()
        textViewVacations.setText(scheduleGroupModel.getVacationsText())
        mVacationRecycler.setAdapter(VacationAdapter(vacations, mEditOnClickListener, mDeleteOnClickListener))
        mVacationRecycler.setLayoutManager(LinearLayoutManager(requireContext()))
    }
    private var mEditOnClickListener: View.OnClickListener = View.OnClickListener { v: View ->
        showVacationDialog(v.tag.toString())
    }
    private var mDeleteOnClickListener: View.OnClickListener = View.OnClickListener { v: View ->
        showDeleteVacationAlert(v.tag.toString())
    }
    private fun showVacationDialog(vacationId: String?) {
        val ft = parentFragmentManager.beginTransaction()
        val vacationSchedule = scheduleGroupModel.getScheduleById(vacationId)

        parentFragmentManager.findFragmentByTag("popup")?.let { ft.remove(it) }

        val calendarDialogFragment = ManualCalendarDialogFragment(
            vacationSchedule,
            mRoomRef,
            createManualCalendarDialogListener()
        )
        calendarDialogFragment.show(ft, "popup")

    }
    private fun createManualCalendarDialogListener() =
        ManualCalendarDialogFragment.ManualCalendarDialogListener { vacationId, vacationName, startDate, endDate ->
            handleSaveVacation(vacationId, vacationName, startDate, endDate)
            false
        }
    private fun handleSaveVacation(
        vacationId: String?,
        vacationName: String,
        startDate: DateTime,
        endDate: DateTime
    ) {
        scheduleGroupModel.saveVacation(vacationId, vacationName, startDate, endDate, requireContext())
        val runnable = Runnable {
            loadVacations()
            ProgressDialogUtils.hideProgressDialog()
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 2000)
    }
    private fun showDeleteVacationAlert(vacationId: String) {
        val vacationSchedule = scheduleGroupModel.getScheduleById(vacationId)
        val alertDialog = Dialog(requireActivity())
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        alertDialog.setCancelable(false)
        alertDialog.setContentView(R.layout.dialog_delete_schedule)
        val messageTv = alertDialog.findViewById<TextView>(R.id.tvMessage)
        messageTv.text = requireActivity().getString(R.string.delete_vacation_confirmation, vacationSchedule?.dis)

        alertDialog.findViewById<View>(R.id.btnCancel)
            .setOnClickListener { alertDialog.dismiss() }
        alertDialog.findViewById<View>(R.id.btnProceed).setOnClickListener {
            scheduleGroupModel.deleteEntity(vacationId)
            alertDialog.dismiss()
            loadVacations()
        }
        alertDialog.show()
    }

    private fun showSpecialScheduleDialog(roomRef: String?, specialScheduleId: String?) {
        val fragmentTransaction = childFragmentManager.beginTransaction()
        childFragmentManager.findFragmentByTag("popup")?.let {
            fragmentTransaction.remove(it)
        }

        val specialScheduleDialogFragment = SpecialScheduleDialogFragment(
            specialScheduleId,
            roomRef,
            createSpecialScheduleDialogListener(specialScheduleId)
        )
        specialScheduleDialogFragment.show(fragmentTransaction, "popup")
    }
    private fun createSpecialScheduleDialogListener(
        specialScheduleId: String?
    ): SpecialScheduleDialogFragment.SpecialScheduleDialogListener {
        return SpecialScheduleDialogFragment.
        SpecialScheduleDialogListener {
                scheduleName, startDate, endDate, coolVal, heatVal,
                coolingUserLimitMax, coolingUserLimitMin, heatingUserLimitMax,
                heatingUserLimitMin, coolingDeadBand, heatingDeadBand ->
            val warning = SpecialSchedule.validateSpecialSchedule(
                coolVal, heatVal, coolingUserLimitMax,
                coolingUserLimitMin,
                heatingUserLimitMax,
                heatingUserLimitMin,
                coolingDeadBand,
                heatingDeadBand
            )
            if (warning != null) {
                showWarningDialog(warning)
                return@SpecialScheduleDialogListener false
            }

            scheduleGroupModel.saveSpecialSchedule(
                specialScheduleId,
                scheduleName,
                startDate,
                endDate,
                coolVal,
                heatVal,
                coolingUserLimitMax,
                coolingUserLimitMin,
                heatingUserLimitMax,
                heatingUserLimitMin,
                coolingDeadBand,
                heatingDeadBand,
                requireContext()
            )

            val runnable = Runnable {
                loadSpecialSchedules()
                ProgressDialogUtils.hideProgressDialog()
            }
            Handler(Looper.getMainLooper()).postDelayed(runnable, 2000)
        }
    }
    private fun showWarningDialog(warning: String) {
        AlertDialog.Builder(activity).apply {
            setMessage(warning)
            setCancelable(false)
            setTitle(R.string.warning_ns)
            setIcon(R.drawable.ic_alert)
            setNegativeButton("OKAY") { dialog, _ -> dialog.dismiss() }
        }.create().show()
    }
    private fun showScheduleLayout(zoneScheduleVisibility :Int, specialScheduleVisibility : Int, vacationVisibility : Int){
        constraintSpecialScheduleLayout.visibility = specialScheduleVisibility
        specialScheduleTitle.visibility = specialScheduleVisibility
        constraintVacationLayout.visibility = vacationVisibility
        vacationTitle.visibility = vacationVisibility
        zoneScheduleLayout.visibility = zoneScheduleVisibility
    }
    private fun closeFragment(scheduleGroupFragment: ScheduleGroupFragment) {
        scheduleGroupFragment.dismiss()
    }
    override fun onClickSave(
        position: Int,
        coolingTemp: Double,
        heatingTemp: Double,
        startTimeHour: Int,
        endTimeHour: Int,
        startTimeMinute: Int,
        endTimeMinute: Int,
        daysToBeAdded: ArrayList<DAYS>?,
        heatingUserLimitMaxVal: Double?,
        heatingUserLimitMinVal: Double?,
        coolingUserLimitMaxVal: Double?,
        coolingUserLimitMinVal: Double?,
        heatingDeadBandVal: Double?,
        coolingDeadBandVal: Double?,
        followBuilding: Boolean,
        mDayVal: Schedule.Days?,
        isDelete: Boolean
    ): Boolean {
        var heatingTempVar = heatingTemp
        var coolingTempVar = coolingTemp
        val removedEntriesWithPositions: List<Pair<Int,Schedule.Days>>?
        removedEntriesWithPositions = arrayListOf()
        var days: List<DAYS>? = null
        if(!isDelete && position != ZoneScheduleDialogFragment.NO_REPLACE) {
            days = scheduleGroupModel.getDaysToBeUpdated(scheduleGroupModel.mScheduleGroup, scheduleGroupModel.mSchedule, position)
        } else if(position == ZoneScheduleDialogFragment.NO_REPLACE) {
            days = daysToBeAdded
        }


        if (followBuilding) {
            val heatUL = Domain.buildingEquip.heatingUserLimitMax.readPriorityVal()
            val heatLL = Domain.buildingEquip.heatingUserLimitMin.readPriorityVal()
            val coolLL = Domain.buildingEquip.coolingUserLimitMin.readPriorityVal()
            val coolUL = Domain.buildingEquip.coolingUserLimitMax.readPriorityVal()
            val coolDB = Domain.buildingEquip.coolingDeadband.readPriorityVal()
            val heatDB = Domain.buildingEquip.heatingDeadband.readPriorityVal()
            val daysInSchedule = scheduleGroupModel.mSchedule.days

            for (eachOccupied in daysInSchedule) {
                if (validateDesiredTemp(coolingTempVar, heatingTempVar, coolLL, coolUL, heatLL, heatUL, heatDB, coolDB) != null) {
                    if (eachOccupied == mDayVal) {
                        heatingTempVar = heatUL - heatDB
                        coolingTempVar = coolLL + coolDB
                    }
                    scheduleGroupModel.mSchedule.getDay(eachOccupied).coolingVal = coolLL + coolDB
                    scheduleGroupModel.mSchedule.getDay(eachOccupied).heatingVal = heatUL - heatDB
                }
            }
        }

        if (position != ZoneScheduleDialogFragment.NO_REPLACE) {
            try {
                scheduleGroupModel.mSchedule.days.sortBy { it.sthh }
                scheduleGroupModel.mSchedule.days.sortBy { it.day }

                val positionsToRemove = scheduleGroupModel.getPositionsToBeRemoved(scheduleGroupModel.mSchedule.scheduleGroup, position, scheduleGroupModel.mSchedule)
                val sortedPositionsToRemove = positionsToRemove.sortedDescending()
                for (pos in sortedPositionsToRemove) {
                    removedEntriesWithPositions.add(Pair(
                        pos,
                        scheduleGroupModel.mSchedule.days.removeAt(pos)
                    )
                    )
                }

            } catch (e: IndexOutOfBoundsException) {
                CcuLog.e("mSchedule", "onClickSave: ${e.message}")
            }
        }

        val daysArrayList = ArrayList<Schedule.Days>()
        zoneScheduleViewModel.doFollowBuildingUpdate(followBuilding, scheduleGroupModel.mSchedule)
        if (!scheduleGroupModel.mSchedule.markers.contains(Tags.FOLLOW_BUILDING)) {
            days?.let {
                for (day in it) {
                    val dayBO = Schedule.Days().apply {
                        ethh = endTimeHour
                        sthh = startTimeHour
                        etmm = endTimeMinute
                        stmm = startTimeMinute
                        heatingVal = heatingTempVar
                        coolingVal = coolingTempVar
                        isSunset = false
                        isSunrise = false
                        heatingUserLimitMin = heatingUserLimitMinVal
                        heatingUserLimitMax = heatingUserLimitMaxVal
                        coolingUserLimitMin = coolingUserLimitMinVal
                        coolingUserLimitMax = coolingUserLimitMaxVal
                        coolingDeadBand = coolingDeadBandVal
                        heatingDeadBand = heatingDeadBandVal
                    }
                    dayBO.day = day.ordinal
                    daysArrayList.add(dayBO)
                }
            }
        } else {
            days?.let {
                for (day in it) {
                    val dayBO = Schedule.Days().apply {
                        ethh = endTimeHour
                        sthh = startTimeHour
                        etmm = endTimeMinute
                        stmm = startTimeMinute
                        heatingVal = heatingTempVar
                        coolingVal = coolingTempVar
                        isSunrise = false
                        isSunset = false
                        heatingUserLimitMin = mDayVal?.heatingUserLimitMin ?: 67.0
                        heatingUserLimitMax = mDayVal?.heatingUserLimitMax ?: 72.0
                        coolingUserLimitMin = mDayVal?.coolingUserLimitMin ?: 72.0
                        coolingUserLimitMax = mDayVal?.coolingUserLimitMax ?: 77.0
                        heatingDeadBand = mDayVal?.heatingDeadBand ?: 2.0
                        coolingDeadBand = mDayVal?.coolingDeadBand ?: 2.0
                    }
                    dayBO.day = day.ordinal
                    daysArrayList.add(dayBO)
                }
            }
        }
        /*while deleting slot no need of validation*/
        if(isDelete){
            updateUI()
            return true
        }
        val intersection = scheduleGroupModel.mSchedule.checkIntersection(daysArrayList)
        if (intersection) {
            val overlapDays = scheduleGroupModel.getOverlapDaysBasedOnScheduleGroup(daysArrayList)

            val overlappingMessage = HtmlCompat.fromHtml("The current settings cannot be" +
                    " overridden because the following duration of the schedules are" +
                    " overlapping", HtmlCompat.FROM_HTML_MODE_LEGACY)
            AlertDialogAdapter(requireContext(), AlertDialogData(
                null,
                overlappingMessage,
                overlapDays,
                null,
                null
                ,
                "Ok",
                {
                    if (removedEntriesWithPositions.isNotEmpty()) {
                        for ((_, entry) in removedEntriesWithPositions) {
                            scheduleGroupModel.mSchedule.days.add(entry)
                        }
                    }
                },
                isOnlyNegativeButton = true,
                isCancelable = false,
                null
            )).showCustomDialog()

            return false
        }

        var spillsMap : HashMap<String, ArrayList<Interval>>? = null
        var uncommonIntervals: List<List<CommonTimeSlotFinder.TimeSlot>>? = null
        val commonTimeSlotFinder = CommonTimeSlotFinder()
        scheduleGroupModel.mSchedule.days.addAll(daysArrayList)

        if(scheduleGroupModel.mScheduleGroup == ScheduleGroup.SEVEN_DAY.ordinal){
            val daysArrayListToCalculate = if(scheduleGroupModel.isNewGroupSelected() || isGroupShifted) {
                scheduleGroupModel.mSchedule.days
            } else {
                daysArrayList
            }
            spillsMap = zoneScheduleViewModel.getScheduleSpills(daysArrayListToCalculate, scheduleGroupModel.mSchedule)
        } else {
            val commonIntervals = commonTimeSlotFinder.getCommonTimeSlot(
                scheduleGroupModel.mSchedule.scheduleGroup,
                hayStack.getSystemSchedule(false)[0].days,
                scheduleGroupModel.mSchedule.days,  false
            )
            uncommonIntervals = commonTimeSlotFinder.getUnCommonTimeSlot(
                scheduleGroupModel.mSchedule.scheduleGroup, commonIntervals, scheduleGroupModel.mSchedule.days)
        }

        isZoneNeedsToBeTrimmedOnDefaultZoneSchedule = false

        val isScheduleSpillExist = if(scheduleGroupModel.mScheduleGroup == ScheduleGroup.SEVEN_DAY.ordinal){
            !spillsMap.isNullOrEmpty()
        }else {
            commonTimeSlotFinder.isUncommonIntervalsHasAnySpills(uncommonIntervals!!)
        }

        if (isScheduleSpillExist) {
            val messages =
                if (scheduleGroupModel.mScheduleGroup == ScheduleGroup.SEVEN_DAY.ordinal) {
                    commonTimeSlotFinder.getSpilledZonesForSevenDayGroup(spillsMap!!)
                } else {
                    commonTimeSlotFinder.getNonSevenDaySpilledZonesList(scheduleGroupModel.mSchedule, uncommonIntervals!!)
                }
            val message = HtmlCompat.fromHtml(
                "Force trim will erase the following time slot(s) of  <b>"
                        + ScheduleGroup.values()[scheduleGroupModel.mScheduleGroup].group +
                        "</b> schedule group. Are you sure you want to proceed?",
                HtmlCompat.FROM_HTML_MODE_LEGACY)

            AlertDialogAdapter(requireContext(), AlertDialogData(
                scheduleGroupModel.getAlertTitle(),
                message,
                messages,
                "Force-Trim",
                {
                    scheduleGroupModel.checkDaysTobeAddedAndDaysList(
                        daysToBeAdded, daysArrayList, uncommonIntervals)
                    scheduleGroupModel.forceTrimSchedule(daysToBeAdded, uncommonIntervals, daysArrayList,commonTimeSlotFinder, spillsMap)
                    updateUI()
                },
                "Re-Edit",
                {
                    if(scheduleGroupModel.isNewGroupSelected() || isGroupShifted) {
                        scheduleGroupModel.mSchedule.days.clear()
                        scheduleGroupModel.mSchedule.days.addAll(
                            DefaultSchedules.getDefaultDays(scheduleGroupModel.mRoomRef,DAYS.MONDAY.ordinal,
                            DAYS.SUNDAY.ordinal, 8,0,17,30))
                    } else {
                        scheduleGroupModel.mSchedule.days.clear()
                        scheduleGroupModel.mSchedule.days.addAll(
                            scheduleGroupModel.getScheduleById(
                                mSchedule!!.id
                            )!!.days
                        )
                    }
                    updateUI()
                    showDialog(position, daysArrayList)
                },
                isOnlyNegativeButton = false,
                isCancelable = false,
                R.drawable.ic_dialog_alert
            )).showCustomDialog()
        }
        updateUI()
        return true
    }


    private fun showDialog(position: Int, days: ArrayList<Schedule.Days>) {
        val ft = parentFragmentManager.beginTransaction()
        val scheduleFragment = parentFragmentManager.findFragmentByTag("popup")
        if (scheduleFragment != null) {
            ft.remove(scheduleFragment)
        }

        val newFragment = ZoneScheduleDialogFragment(this, position, days, scheduleGroupModel.mSchedule)
        newFragment.show(ft, "popup")

    }

    override fun onClickSaveSchedule(unOccupiedZoneSetback: Double, schedule: Schedule) {
        scheduleGroupModel.mSchedule.unoccupiedZoneSetback = unOccupiedZoneSetback
        scheduleGroupModel.saveSchedule(false, null, null, null)
    }

    override fun onClickCancelSaveSchedule(scheduleId: String?) {
        RangeBar.setUnOccupiedFragment(true)
    }
    private fun prepareCustomSchedulePreviewLayout() {
        showScheduleLayout(View.VISIBLE, View.GONE, View.GONE)
        headerLayout.visibility = View.GONE
        cancelUpdateLayout.visibility = View.GONE
        radioGroupLayout.visibility = View.GONE
    }

    private fun updateCustomUI() {
        populateIntersections(mPointDefinition!!)
        Handler(Looper.getMainLooper()).post {
            hasTextViewChildren()
            val days: List<Day> = mPointDefinition!!.days
            days.sortedBy { it.sthh }
            days.sortedBy { it.day }

            val unOccupiedDays = scheduleGroupModel.getUnScheduledDays(days, ZoneScheduleViewModel())
            drawScheduleLayoutBasedOnGroup(scheduleGroupModel.mScheduleGroup)
            drawUnscheduledTimeSlots(unOccupiedDays, getValueByEnum(mPointDefinition!!.defaultValue, mEnumStringForPointSchedule, mUnitForPointSchedule), scheduleGroupModel.mScheduleGroup)
            drawScheduledTimeSlots(days, scheduleGroupModel.mScheduleGroup)
            drawCurrentTime()
        }
    }

    private fun drawPointSchedule(
        position: Int, currentVal: String, startTimeHH: Int,
        endTimeHH: Int, startTimeMM: Int, endTimeMM: Int, day: DAYS,
        intersection: Boolean, isOccupied: Boolean
    ) {
        var currentValLocal = FontManager.getColoredSpanned(currentVal, customValueColor)

        var typeface = Typeface.DEFAULT
        try {
            typeface = Typeface.createFromAsset(requireActivity().assets, "font/Lato-Bold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (startTimeHH > endTimeHH || (startTimeHH == endTimeHH && startTimeMM > endTimeMM)) {
            drawPointScheduleBlock(
                position, currentValLocal, typeface, startTimeHH,
                24, startTimeMM, 0,
                scheduleGroupModel.getTextViewFromDay(day, textViewFirstRow,
                    textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow,
                    textViewSixthRow, textViewSeventhRow),
                leftBreak = false,
                rightBreak = true,
                intersection = intersection,
                isOccupied = isOccupied
            )
            drawPointScheduleBlock(
                position, currentValLocal, typeface, 0,
                endTimeHH, 0, endTimeMM,
                scheduleGroupModel.getTextViewFromDay(day.nextDay, textViewFirstRow,
                    textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow,
                    textViewSixthRow, textViewSeventhRow),
                leftBreak = true, rightBreak = false, intersection = intersection, isOccupied = isOccupied
            )
        } else {
            drawPointScheduleBlock(
                position, currentValLocal,
                typeface, startTimeHH, endTimeHH, startTimeMM,
                endTimeMM, scheduleGroupModel.getTextViewFromDay(day, textViewFirstRow,
                    textViewSecondRow, textViewThirdRow, textViewFourthRow, textViewFifthRow,
                    textViewSixthRow, textViewSeventhRow),
                leftBreak = false, rightBreak = false, intersection = intersection, isOccupied = isOccupied
            )
        }
    }

    private fun drawPointScheduleBlock(
        position: Int, strCurrVal: String, typeface: Typeface?,
        tempStartTime: Int, tempEndTime: Int,
        startTimeMM: Int, endTimeMM: Int, textView: TextView?,
        leftBreak: Boolean, rightBreak: Boolean, intersection: Boolean, isOccupied: Boolean
    ) {
        var tempEndTimeLocal = tempEndTime
        CcuLog.d(
            L.TAG_CCU_UI,
            "position: " + position + " tempStartTime: " + tempStartTime + " tempEndTime: " + tempEndTimeLocal + " startTimeMM: " + startTimeMM + " endTimeMM " + endTimeMM + "isOccupied " + isOccupied
        )

        if (context == null) return
        val textViewTemp = AppCompatTextView(
            requireContext()
        )
        textViewTemp.gravity = Gravity.CENTER_HORIZONTAL
        if (isOccupied) {
            textViewTemp.text = Html.fromHtml("$strCurrVal", Html.FROM_HTML_MODE_LEGACY)
        }
        if (typeface != null) textViewTemp.typeface = typeface
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            textViewTemp,
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        textViewTemp.maxLines = 2
        textViewTemp.contentDescription =
            textView!!.text.toString() + "_" + tempStartTime + ":" + startTimeMM + "-" + tempEndTimeLocal + ":" + endTimeMM
        textViewTemp.id = ViewCompat.generateViewId()
        textViewTemp.tag = position

        val lp = ConstraintLayout.LayoutParams(0, mPixelsBetweenADay.toInt())
        lp.bottomToBottom = textView.id


        val leftMargin =
            if (startTimeMM > 0) ((startTimeMM / 60.0) * mPixelsBetweenAnHour).toInt() else lp.leftMargin
        val rightMargin =
            if (endTimeMM > 0) (((60 - endTimeMM) / 60.0) * mPixelsBetweenAnHour).toInt() else lp.rightMargin

        lp.leftMargin = leftMargin
        lp.rightMargin = rightMargin

        val drawableCompat: Drawable?

        if (leftBreak) {
            drawableCompat = ResourcesCompat.getDrawable(resources,R.drawable.occupancy_background, requireContext().theme)
            if (intersection) {
                val rightGreyBar = ResourcesCompat.getDrawable(resources,R.drawable.dashed_separator, requireContext().theme)
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                    mDrawableBreakLineLeft,
                    null,
                    rightGreyBar,
                    null
                )
            } else textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                mDrawableBreakLineLeft,
                null,
                null,
                null
            )

            val space = Space(activity)
            space.id = View.generateViewId()
            val px =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)

            val spaceLP = ConstraintLayout.LayoutParams(px.toInt(), 10)
            spaceLP.rightToLeft = viewTimeLines[tempStartTime].id
            constraintScheduler.addView(space, spaceLP)
            if (endTimeMM > 0) tempEndTimeLocal++
            lp.startToStart = space.id
            lp.endToEnd = viewTimeLines[tempEndTimeLocal].id
        } else if (rightBreak) {
            drawableCompat = ResourcesCompat.getDrawable(resources,R.drawable.occupancy_background, requireContext().theme)
            textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                mDrawableBreakLineRight,
                null
            )
            val space = Space(activity)
            space.id = View.generateViewId()
            val px =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
            val spaceLP = ConstraintLayout.LayoutParams(px.toInt(), 10)
            spaceLP.leftToRight = viewTimeLines[tempEndTimeLocal].id
            constraintScheduler.addView(space, spaceLP)
            lp.startToStart = viewTimeLines[tempStartTime].id
            lp.endToEnd = space.id
        } else {
            if (intersection) {
                val rightGreyBar = ResourcesCompat.getDrawable(resources,R.drawable.dashed_separator, requireContext().theme)
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(
                    null, null,
                    rightGreyBar, null
                )
            }
            drawableCompat = ResourcesCompat.getDrawable(resources,
                if (isOccupied) R.drawable.occupancy_background else R.drawable.occupancy_background_unoccupied,
                null
            )
            if (endTimeMM > 0) tempEndTimeLocal++
            lp.startToStart = viewTimeLines[tempStartTime].id
            lp.endToEnd = viewTimeLines[tempEndTimeLocal].id
        }
        textViewTemp.background = drawableCompat
        constraintScheduler.addView(textViewTemp, lp)
        textViewTemp.setOnClickListener { v ->
            try {
                if (isOccupied) {
                    pointScheduleUiClicked!!.invoke(mapOf(
                        "scheduleGroup" to ScheduleGroup.values()[scheduleGroupModel.mScheduleGroup].group,
                        "timeRange" to formatTimeRange(tempStartTime, startTimeMM, tempEndTime, endTimeMM),
                        "valueType" to "Custom Value",
                        "value" to Html.fromHtml(strCurrVal, Html.FROM_HTML_MODE_LEGACY).toString()
                    ))
                } else {
                    pointScheduleUiClicked!!.invoke(mapOf(
                        "scheduleGroup" to ScheduleGroup.values()[scheduleGroupModel.mScheduleGroup].group,
                        "timeRange" to formatTimeRange(tempStartTime, startTimeMM, tempEndTime, endTimeMM),
                        "valueType" to "Default Value",
                        "value" to Html.fromHtml(strCurrVal, Html.FROM_HTML_MODE_LEGACY).toString()
                    ))
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                CcuLog.e("Schedule", "onClick: " + e.message)
            }
        }
    }

    private fun drawUnscheduledTimeSlots(
        unOccupiedDays: MutableList<UnOccupiedDays>,
        defaultValue: String,
        scheduleGroup: Int
    ) {
        for (i in unOccupiedDays.indices) {
            val daysElement = unOccupiedDays[i]
            if (scheduleGroupModel.shouldDrawDay(daysElement.day, scheduleGroup)) {
                drawPointSchedule(
                    daysElement.day, defaultValue, daysElement.sthh, daysElement.ethh,
                    daysElement.stmm, daysElement.etmm,
                    DAYS.values()[daysElement.day], daysElement.isIntersection, false
                )
            }
        }
    }

    private fun drawScheduledTimeSlots(
        days: List<Day>,
        scheduleGroup: Int?
    ) {
        for (i in days.indices) {
            val daysElement = days[i]
            if (scheduleGroupModel.shouldDrawDay(daysElement.day, scheduleGroup ?: ScheduleGroup.EVERYDAY.ordinal)) {
                drawPointSchedule(
                    i,
                    getValueByEnum(daysElement.`val`, mEnumStringForPointSchedule, mUnitForPointSchedule),
                    daysElement.sthh,
                    daysElement.ethh,
                    daysElement.stmm,
                    daysElement.etmm,
                    DAYS.values()[daysElement.day],
                    daysElement.intersection,
                    true
                )
            }
        }
    }
}
