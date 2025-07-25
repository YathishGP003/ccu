package a75f.io.renatus.schedules;

import static a75f.io.logic.bo.util.CCUUtils.getTruncatedString;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusRelative;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.base.BaseInterval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.schedule.ScheduleGroup;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.util.CommonTimeSlotFinder;
import a75f.io.renatus.R;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;

public class NamedSchedule extends DialogFragment {
    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";
    private static final String PARAM_ROOM_REF = "PARAM_ROOM_REF";
    private static final String PARAM_SCHED_NAME = "PARAM_SCHED_NAME";
    private static final String PARAM_SCHED_SET = "PARAM_SCHED_SET";
    private static final String TAG = "NAMED_SCHEDULE";
    TextView textViewScheduletitle;
    TextView scheduleGroupTitle;
    Schedule schedule;
    ConstraintLayout constraintScheduler;
    String colorMinTemp = "";
    String colorMaxTemp = "";
    NestedScrollView scheduleScrollView;
    private OnExitListener mOnExitListener;
    private OnCancelButtonClickListener OnCancel;
    Button setButton;
    Button cancelButton;
    ImageView closeButton;
    private boolean NamedScheduleScreenCheck = false;
    private String mScheduleId;

    @Override
    public void onStop() {
        super.onStop();
        if (mOnExitListener != null) mOnExitListener.onExit();

    }

    public void setOnExitListener(OnExitListener onExitListener) {
        this.mOnExitListener = onExitListener;

    }

    public interface OnExitListener {
        void onExit();
    }

    public void setOnCancelButtonClickListener(OnCancelButtonClickListener listener) {
        this.OnCancel = listener;
    }

    public interface OnCancelButtonClickListener {
        void onCancelButtonClicked();
    }


    public static NamedSchedule getInstance(String scheduleId, String roomRef, String scheduleName, boolean isSet) {
        NamedSchedule namedSchedule = new NamedSchedule();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, scheduleId);
        args.putString(PARAM_ROOM_REF, roomRef);
        args.putString(PARAM_SCHED_NAME, scheduleName);
        args.putBoolean(PARAM_SCHED_SET, isSet);
        namedSchedule.setArguments(args);
        return namedSchedule;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setShowsDialog(args != null);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();

        if (dialog != null) {
            int width = 1165;
            Objects.requireNonNull(dialog.getWindow()).setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.named_schedule_preview, container, false);
        initialiseViews(rootView);
        loadSchedule();

        setButton = rootView.findViewById(R.id.setButton);
        closeButton = rootView.findViewById(R.id.btnCloseNamed);
        cancelButton = rootView.findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(view -> {
            OnCancel.onCancelButtonClicked();
            dismiss();
        });

        if (getArguments() != null && !getArguments().getBoolean(PARAM_SCHED_SET)) {
            setButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);
            closeButton.setOnClickListener(view -> dismiss());
        }
        prepareNamedScheduleLayout();
        setButton.setOnClickListener(v -> {
            NamedScheduleScreenCheck = true;
            if (validateNamedSchedule()) {
                CcuLog.d(TAG, "Valid Named Schedule");
                try {
                    assert getArguments() != null;
                    String roomRef = getArguments().getString(PARAM_ROOM_REF);
                    CcuLog.d(TAG, "roomref = " + roomRef);
                    List<HashMap<Object, Object>> scheduleTypePoints = CCUHsApi.getInstance().readAllEntities("scheduleType and roomRef ==\"" + getArguments().getString(PARAM_ROOM_REF) + "\"");
                    if (!scheduleTypePoints.isEmpty()) {
                        for (HashMap<Object, Object> scheduleType : scheduleTypePoints) {
                            CCUHsApi.getInstance().writeDefaultValById(Objects.requireNonNull(scheduleType.get("id")).toString(), 2.0);
                            CCUHsApi.getInstance().writeHisValById(Objects.requireNonNull(scheduleType.get("id")).toString(), 2.0);
                        }
                    }
                    HashMap<Object, Object> room = CCUHsApi.getInstance().readMapById(roomRef);
                    Zone zone = HSUtil.getZone(roomRef, Objects.requireNonNull(room.get("floorRef")).toString());
                    if (zone != null) {
                        zone.setScheduleRef(getArguments().getString(PARAM_SCHEDULE_ID));
                        CCUHsApi.getInstance().updateZone(zone, roomRef);
                    }
                    CCUHsApi.getInstance().scheduleSync();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setMessage(R.string.success_alert).setCancelable(false).setPositiveButton("OKAY", (dialog, which) -> {
                    dialog.dismiss();
                    dismiss();
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        return rootView;
    }

    private void prepareNamedScheduleLayout() {
        ScheduleGroupFragment scheduleGroupFragment = new
                ScheduleGroupFragment().showNamedSchedulePreviewLayout(
                        CCUHsApi.getInstance().getScheduleById((getArguments()).getString(PARAM_SCHEDULE_ID)));
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.namedScheduleLayout, scheduleGroupFragment).commit();
    }

    private void initialiseViews(View rootView) {
        constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);
        textViewScheduletitle = rootView.findViewById(R.id.scheduleTitle);
        scheduleGroupTitle = rootView.findViewById(R.id.namedScheduleGroupTitle);
        scheduleScrollView = rootView.findViewById(R.id.scheduleScrollView);
        scheduleScrollView.post(() -> scheduleScrollView.smoothScrollTo(0, 0));

        colorMinTemp = String.valueOf(ContextCompat.getColor(requireContext(), R.color.min_temp));
        colorMaxTemp = String.valueOf(ContextCompat.getColor(requireContext(), R.color.max_temp));
    }
    private void loadSchedule() {
        if (getArguments() != null && getArguments().containsKey(PARAM_SCHEDULE_ID)) {
            mScheduleId = getArguments().getString(PARAM_SCHEDULE_ID);
            schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
        } else {
            schedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
            CcuLog.d(L.TAG_CCU_UI, " Loaded System Schedule - NamedSchedule " + schedule.toString());
        }
        CcuLog.d(TAG, "PARAM_SCHEDULE_ID " + mScheduleId);
        String namedScheduleDis = getArguments().getString(PARAM_SCHED_NAME);
        String title;
        String scheduledName = getArguments().getString(PARAM_SCHED_NAME);
        boolean isScheduledSet = getArguments().getBoolean(PARAM_SCHED_SET);

        if (!isScheduledSet) {
            if (scheduledName != null && namedScheduleDis != null && !namedScheduleDis.isEmpty() && namedScheduleDis.length() > 25) {
                title = getTruncatedString(scheduledName,25,0,25);
            } else {
                title = scheduledName;
            }
        } else {
            if (scheduledName != null && namedScheduleDis != null && namedScheduleDis.length() > 25) {
                title = getTruncatedString("Preview : " + scheduledName,25,0,25);
            } else {
                title = "Preview : " + scheduledName;
            }
        }

        textViewScheduletitle.setText(title);
        scheduleGroupTitle.setText(ScheduleGroup.values()[schedule.getScheduleGroup()].getGroup());

    }

    private void separateOverNightSchedule(ArrayList<Interval> zoneIntervals) {
        for (int i = 0; i < zoneIntervals.size(); i++) {
            Interval interval = zoneIntervals.get(i);
            LocalTime startTimeOfDay = interval.getStart().toLocalTime();
            LocalTime endTimeOfDay = interval.getEnd().toLocalTime();
            // Check if the start time is after the end time and separating the overnight schedule
            if (startTimeOfDay.isAfter(endTimeOfDay)) {
                zoneIntervals.set(i, ScheduleUtil.OverNightEnding(interval));
                zoneIntervals.add(ScheduleUtil.OverNightStarting(interval));
            }
        }
    }


    private boolean validateNamedSchedule() {
        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
        List<List<CommonTimeSlotFinder.TimeSlot>> intervalSpills = new ArrayList<>();
        CCUHsApi hsApi = CCUHsApi.getInstance();
        ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals();
        StringBuilder warningMessage = new StringBuilder("No such Schedule");
        boolean isValid = true;

        if (systemIntervals.isEmpty()) {
            warningMessage = new StringBuilder("Building occupancy is empty, Cannot apply any Schedule.");
            showWarningMessage(warningMessage);
            return false;
        } else {
            if (getArguments() != null && getArguments().containsKey(PARAM_SCHEDULE_ID)) {
                warningMessage = new StringBuilder();
                Schedule namedSchedule = hsApi.getScheduleById(getArguments().getString(PARAM_SCHEDULE_ID));
                String roomRef = getArguments().getString(PARAM_ROOM_REF);
                ArrayList<Interval> zoneIntervals = namedSchedule.getScheduledIntervals();
                if(zoneIntervals.isEmpty()){
                    warningMessage = new StringBuilder("Selected shared schedule has no occupied blocks, please define occupied slots in the shared schedule to apply");
                    showWarningMessage(warningMessage);
                    return false;
                }
                separateOverNightSchedule(zoneIntervals);
                zoneIntervals.sort(Comparator.comparingLong(BaseInterval::getStartMillis));
                Interval ZonelastInterval = zoneIntervals.get(zoneIntervals.size() - 1);
                LocalDate ZoneLastTimeOfDay = ZonelastInterval.getStart().toDateTime().toLocalDate();
                Interval systemLastInterval = systemIntervals.get(systemIntervals.size() - 1);
                LocalDate systemLastTimeOfDay = systemLastInterval.getStart().toDateTime().toLocalDate();
                // checking for overnight for sunday ,if it is has overnight sch for sunday
                // we need to add the building occupancy for next week monday also
                if (ZoneLastTimeOfDay.isAfter(systemLastTimeOfDay)) {
                    Interval nextWeekDaySystemInterval = ScheduleUtil.AddingNextWeekDayForOverNight(systemSchedule);
                    if(nextWeekDaySystemInterval!=null) {
                        systemIntervals.add(nextWeekDaySystemInterval);
                    }
                }
                updateIntervalSpills(intervalSpills);
                if (addIntervalSpillWarning(warningMessage, hsApi, namedSchedule)) {
                    isValid = false;
                }
                if (!deadbandValidation(roomRef, warningMessage, namedSchedule)) {
                    isValid = false;
                }

                StringBuilder desiredTempWarning = new StringBuilder();
                boolean isDesiredTempValid = isValidDesiredTemp(desiredTempWarning, namedSchedule);
                if (!isDesiredTempValid) {
                    invalidDesiredTempError(warningMessage, desiredTempWarning);
                    isValid = false;
                }
                StringBuilder userLimitWarning = new StringBuilder();
                boolean isLimitValid = isValidUserLimit(userLimitWarning, namedSchedule);
                if (!isLimitValid) {
                    invalidUserLimitError(warningMessage, userLimitWarning);
                    isValid = false;
                }
                StringBuilder deadBandWarningMsg = new StringBuilder();
                boolean isDeadBandValid = isDeadBandValid(deadBandWarningMsg, namedSchedule);
                if (!isDeadBandValid) {
                    invalidDeadBandError(warningMessage, deadBandWarningMsg);
                    isValid = false;
                }
            }
        }


        if (!isValid) {
            warningMessage.append("\n\n").append(getText(R.string.pls_goback));
            showWarningMessage(warningMessage);
        }
        return isValid;
    }

    private boolean deadbandValidation(String roomRef, StringBuilder warningMessage, Schedule namedSchedule) {
        double coolingDeadband = TunerUtil.getZoneCoolingDeadband(roomRef);
        double heatingDeadband = TunerUtil.getZoneHeatingDeadband(roomRef);
        if (isCelsiusTunerAvailableStatus()) {
            coolingDeadband = fahrenheitToCelsiusRelative(coolingDeadband);
            heatingDeadband = fahrenheitToCelsiusRelative(heatingDeadband);
            for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
                double deadbandNamedSched = namedSchedDay.getCoolingVal() - namedSchedDay.getHeatingVal();
                deadbandNamedSched = fahrenheitToCelsiusRelative(deadbandNamedSched);
                if (deadbandNamedSched < coolingDeadband + heatingDeadband) {
                    warningMessage.append(getText(R.string.deadband_warning)).append("\n\t").append("Deadband in Shared schedule - ").append(fahrenheitToCelsiusRelative(deadbandNamedSched)).append("°C").append("\n\t").append("Deadband for zone: CoolingDeadband - ").append(coolingDeadband).append("°C").append("\n\t\t").append("HeatingDeadband - ").append(heatingDeadband).append("°C").append("\n\n");
                    return false;
                }
            }
        } else {
            for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
                double deadbandNamedSched = namedSchedDay.getCoolingVal() - namedSchedDay.getHeatingVal();
                if (deadbandNamedSched < coolingDeadband + heatingDeadband) {
                    warningMessage.append(getText(R.string.deadband_warning)).append("\n\t").append("Deadband in Shared schedule - ").append(deadbandNamedSched).append("°F").append("\n\t").append("Deadband for zone: CoolingDeadband - ").append(coolingDeadband).append("°F").append("\n\t\t").append("HeatingDeadband - ").append(heatingDeadband).append("°F").append("\n\n");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean addIntervalSpillWarning(StringBuilder warningMessage, CCUHsApi ccuHsApi, Schedule namedSchedule) {
        CommonTimeSlotFinder commonTimeSlotFinder = new CommonTimeSlotFinder();
        List<List<CommonTimeSlotFinder.TimeSlot>> commonIntervals = commonTimeSlotFinder.getCommonTimeSlot(
                schedule.getScheduleGroup(),
                ccuHsApi.getSystemSchedule(false).get(0).getDays(),
                namedSchedule.getDays(),
                false
        );

        List<List<CommonTimeSlotFinder.TimeSlot>> uncommonIntervals = commonTimeSlotFinder.getUnCommonTimeSlot(
                schedule.getScheduleGroup(),
                commonIntervals,
                namedSchedule.getDays()
        );
        if (commonTimeSlotFinder.isUncommonIntervalsHasAnySpills(uncommonIntervals)) {
            warningMessage.append(getText(R.string.warning_msg)).append("\n\t").
                    append(commonTimeSlotFinder.getSpilledZones(namedSchedule, uncommonIntervals)).append("\n\n");
            return true;
        } else {
            return false;
        }
    }

    private void updateIntervalSpills(List<List<CommonTimeSlotFinder.TimeSlot>> intervalSpills) {

    }

    private boolean isValidDesiredTemp(StringBuilder desiredTempWarning, Schedule namedSchedule) {
        for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
            if (!(namedSchedDay.getHeatingVal() <= namedSchedDay.getHeatingUserLimitMax() && namedSchedDay.getHeatingVal() >= namedSchedDay.getHeatingUserLimitMin() && namedSchedDay.getCoolingVal() <= namedSchedDay.getCoolingUserLimitMax() && namedSchedDay.getCoolingVal() >= namedSchedDay.getCoolingUserLimitMin())) {
                String[] dayName = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                if (isCelsiusTunerAvailableStatus()) {
                    desiredTempWarning.append("\t").append(dayName[namedSchedDay.getDay()]).append("-").append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm()).append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm()).append("(CDT - ").append(fahrenheitToCelsius(namedSchedDay.getCoolingVal())).append("°C").append(";").append("HDT - ").append(fahrenheitToCelsius(namedSchedDay.getHeatingVal())).append("°C").append(")").append("Schedule user limit : Heating - ").append((fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMin()))).append("°C").append("~").append((fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMax()))).append("°C").append("\n\t Cooling - ").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMin())).append("°C").append("~").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMax())).append("°C").append("\n\n").append("\n\t\t");
                } else {
                    desiredTempWarning.append("\t\t").append(dayName[namedSchedDay.getDay()]).append("-").append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm()).append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm()).append("(CDT - ").append(namedSchedDay.getCoolingVal()).append("°F").append(";").append("HDT - ").append(namedSchedDay.getHeatingVal()).append("°F").append(")").append("Schedule user limit : Heating  - ").append(namedSchedDay.getHeatingUserLimitMin()).append("°F").append("~").append(namedSchedDay.getHeatingUserLimitMax()).append("°F").append("\n\t\t Cooling - ").append(namedSchedDay.getCoolingUserLimitMin()).append("°F").append("~").append(namedSchedDay.getCoolingUserLimitMax()).append("°F").append("\n\n");
                }
                return false;
            }
        }
        return true;
    }

    private void invalidDesiredTempError(StringBuilder warningMessage, StringBuilder desiredTempWarning) {
        if (isCelsiusTunerAvailableStatus()) {
            warningMessage.append(getText(R.string.desiredTemp_warning)).append("\n\t").append("Shared schedule desired temperature : \n\t\t").append(desiredTempWarning);
        } else {
            warningMessage.append(getText(R.string.desiredTemp_warning)).append("\n\t").append("Shared schedule desired temperature : \n\t\t").append(desiredTempWarning);
        }
    }

    private boolean isValidUserLimit(StringBuilder userLimitWarning, Schedule namedSchedule) {
        StringBuilder warningMessage = new StringBuilder();
        boolean isValid = true;
        String[] dayName = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
            // Check if the day should be validated based on the schedule group
            if (shouldValidateDay(namedSchedule.getScheduleGroup(), namedSchedDay.getDay())) {
                if (!MasterControlUtil.validateNamed(
                        namedSchedDay.getHeatingUserLimitMin(),
                        namedSchedDay.getCoolingUserLimitMax(),
                        namedSchedule.getUnoccupiedZoneSetback())) {

                    isValid = false;
                    String dayString = getDayString(namedSchedule.getScheduleGroup(), namedSchedDay.getDay(), dayName);
                    appendWarningMessage(warningMessage, dayString, namedSchedDay, isCelsiusTunerAvailableStatus());
                }
            }
        }

        if (!isValid) {
            userLimitWarning.append(warningMessage);
        }

        return isValid;
    }

    // Determine if the day should be validated based on the schedule group
    private boolean shouldValidateDay(int scheduleGroup, int day) {
        if(scheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
            return day == DAYS.MONDAY.ordinal();
        } else if(scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()) {
            return day == DAYS.MONDAY.ordinal() || day == DAYS.SATURDAY.ordinal();
        } else if(scheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()) {
            return day == DAYS.MONDAY.ordinal() || day == DAYS.SATURDAY.ordinal() || day == DAYS.SUNDAY.ordinal();
        } else {
            return true;
        }
    }

    // Get the day string based on the schedule group
    private String getDayString(int scheduleGroup, int day, String[] dayName) {
        if (scheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
            return "AllDays";
        } else if (scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()) {
            return (day == DAYS.MONDAY.ordinal()) ? "Weekday" : "Weekend";
        } else if (scheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()) {
            return (day == DAYS.MONDAY.ordinal()) ? "Weekday" :
                    (day == DAYS.SATURDAY.ordinal()) ? "Saturday" : "Weekend";
        } else {
            return dayName[day];
        }
    }

    // Append the warning message based on the day and temperature settings
    private void appendWarningMessage(StringBuilder warningMessage, String dayString, Schedule.Days namedSchedDay, boolean isCelsius) {
        String timeRange = String.format("%02d:%02d-%02d:%02d",
                namedSchedDay.getSthh(), namedSchedDay.getStmm(),
                namedSchedDay.getEthh(), namedSchedDay.getEtmm());
        if (isCelsius) {
            warningMessage.append("\t\t").append(dayString).append("-").append(timeRange)
                    .append("(Heating User Limit Min - ").append(fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMin())).append("°C")
                    .append(" Cooling User Limit Max - ").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMax())).append("°C)")
                    .append("\n\t\t");
        } else {
            warningMessage.append("\t\t").append(dayString).append("-").append(timeRange)
                    .append("(Heating User Limit Min - ").append(namedSchedDay.getHeatingUserLimitMin()).append("°F")
                    .append(" Cooling User Limit Max - ").append(namedSchedDay.getCoolingUserLimitMax()).append("°F)")
                    .append("\n\t\t");
        }
    }

    private void invalidUserLimitError(StringBuilder warningMessage, StringBuilder userLimitWarning) {
        if (isCelsiusTunerAvailableStatus()) {
            warningMessage.append(getText(R.string.limits_warning)).append("\n\t").append("Shared schedule limits : \n\t\t").append(userLimitWarning).append("Building limits :").append((fahrenheitToCelsius(BuildingTunerCache.getInstance().getBuildingLimitMin()))).append("°C").append("~").append(fahrenheitToCelsius(BuildingTunerCache.getInstance().getBuildingLimitMax())).append("°C").append("\n\n");
        } else {
            warningMessage.append(getText(R.string.limits_warning)).append("\n\t").append("Shared schedule limits : \n\t\t").append(userLimitWarning).append("Building limits : ").append(BuildingTunerCache.getInstance().getBuildingLimitMin()).append("°F").append("~").append(BuildingTunerCache.getInstance().getBuildingLimitMax()).append("°F").append("\n\n");
        }
    }

    private boolean isDeadBandValid(StringBuilder deadBandWarningMsg, Schedule namedSchedule) {
        StringBuilder warningMessage = new StringBuilder();
        boolean isValid = true;
        String[] dayName = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
            // Check if the day should be validated based on the schedule group
            if (shouldValidateDay(namedSchedule.getScheduleGroup(), namedSchedDay.getDay())) {
                if (!MasterControlUtil.validateNamedDeadBand(namedSchedDay.getHeatingUserLimitMin(),
                        namedSchedDay.getHeatingUserLimitMax(), namedSchedDay.getCoolingUserLimitMin(),
                        namedSchedDay.getCoolingUserLimitMax(), namedSchedDay.getHeatingDeadBand(),
                        namedSchedDay.getCoolingDeadBand())) {

                    isValid = false;
                    String dayString = getDayString(namedSchedule.getScheduleGroup(), namedSchedDay.getDay(), dayName);
                    appendWarningMessage(warningMessage, dayString, namedSchedDay, isCelsiusTunerAvailableStatus());
                }
            }
        }

        if (!isValid) {
            deadBandWarningMsg.append(warningMessage);
        }

        return isValid;
    }

    private void invalidDeadBandError(StringBuilder warningMessage, StringBuilder deadBandWarning) {
        warningMessage.append("Shared schedule limits and deadbands are viloating on below zones: \n\t\t")
                .append(deadBandWarning).append("\n\t\tThe difference in limit maximum and minimum to be more or than or equal to the deadband and "
                        + "\n\t\tHeating Limit Max + deadband (heating + cooling) should be less than or equal to Cooling Limit Max"
                        + "\n\t\tCooling Limit min - deadband (heating + cooling) should be greater than or equal to Heating Limit Min ")
                .append("\n\n");
    }

    private void showWarningMessage(StringBuilder warningMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(warningMessage.toString())
                .setCancelable(false)
                .setTitle(R.string.warning_ns)
                .setIcon(R.drawable.ic_alert)
                .setNegativeButton(R.string.okay, (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!NamedScheduleScreenCheck && OnCancel != null) OnCancel.onCancelButtonClicked();
    }
}
