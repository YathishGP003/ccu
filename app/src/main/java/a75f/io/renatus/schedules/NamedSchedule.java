package a75f.io.renatus.schedules;

import static a75f.io.api.haystack.util.TimeUtil.getEndTimeHr;
import static a75f.io.api.haystack.util.TimeUtil.getEndTimeMin;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusRelative;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.renatus.schedules.ScheduleUtil.disconnectedIntervals;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Schedule;

import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;

import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.util.FontManager;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;

public class NamedSchedule extends DialogFragment {
    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";
    private static final String PARAM_ROOM_REF = "PARAM_ROOM_REF";
    private static final String PARAM_SCHED_NAME = "PARAM_SCHED_NAME";
    private static final String PARAM_SCHED_SET = "PARAM_SCHED_SET";
    private static final String TAG = "NAMED_SCHEDULE";

    TextView textViewMonday;
    TextView textViewTuesday;
    TextView textViewWednesday;
    TextView textViewThursday;
    TextView textViewFriday;
    TextView textViewSaturday;
    TextView textViewSunday;
    View view00;
    View view02;
    View view04;
    View view06;
    View view08;
    View view10;
    View view12;
    View view14;
    View view16;
    View view18;
    View view20;
    View view22;
    View view24;
    View view01;
    View view03;
    View view05;
    View view07;
    View view09;
    View view11;
    View view13;
    View view15;
    View view17;
    View view19;
    View view21;
    View view23;
    private Drawable mDrawableBreakLineLeft;
    private Drawable mDrawableBreakLineRight;
    TextView textViewScheduletitle;
    Schedule schedule;
    ConstraintLayout constraintScheduler;
    ArrayList<View> viewTimeLines;
    String mScheduleId;
    String colorMinTemp = "";
    String colorMaxTemp = "";
    NestedScrollView scheduleScrollView;
    private float mPixelsBetweenAnHour;
    private float mPixelsBetweenADay;
    private OnExitListener mOnExitListener;
    private OnCancelButtonClickListener OnCancel;
    Button setButton;
    Button cancelButton;
    ImageView closeButton;
    private  boolean flag = false;

    @Override
    public void onStop() {
        super.onStop();
        if (mOnExitListener != null)
            mOnExitListener.onExit();

    }

    public void setOnExitListener(NamedSchedule.OnExitListener onExitListener) {
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


    public static NamedSchedule getInstance(String scheduleId,String roomRef,String scheduleName,boolean isSet) {
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
    public void onCreate( Bundle savedInstanceState) {
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
            int height = 646;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.named_schedule_preview, container, false);
        //Scheduler Layout
        initialiseViews(rootView);

        setButton = rootView.findViewById(R.id.setButton);
        closeButton = rootView.findViewById(R.id.btnCloseNamed);
        cancelButton = rootView.findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(view -> {
            OnCancel.onCancelButtonClicked();
            dismiss();
        });



        if(!getArguments().getBoolean(PARAM_SCHED_SET)) {
            setButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);
            closeButton.setOnClickListener(view -> {
                dismiss();
            });
        }

        setButton.setOnClickListener(v -> {
            flag = true;
            if (validateNamedSchedule()) {
                CcuLog.d(TAG, "Valid Named Schedule");
                try {
                    assert getArguments() != null;
                    String roomRef = getArguments().getString(PARAM_ROOM_REF);
                    CcuLog.d(TAG, "roomref = " + roomRef);
                    List<HashMap<Object, Object>> scheduleTypePoints =
                            CCUHsApi.getInstance().readAllEntities(
                                    "scheduleType and roomRef ==\"" + getArguments().getString(PARAM_ROOM_REF) + "\"");
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
                builder.setMessage(R.string.success_alert)
                        .setCancelable(false)
                        .setPositiveButton("OKAY",(dialog, which) -> {
                            dialog.dismiss();
                            dismiss();
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });


        //Measure the amount of pixels between an hour after the constraintScheduler layout draws the bars for the first time.
        //After they are measured d the schedule.
        ViewTreeObserver vto = constraintScheduler.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                constraintScheduler.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                View viewHourOne = viewTimeLines.get(1);
                View viewHourTwo = viewTimeLines.get(2);

                mPixelsBetweenAnHour = viewHourTwo.getX() - viewHourOne.getX();
                mPixelsBetweenADay = (float) constraintScheduler.getHeight() / 7;

                //Leave 20% for padding.
                mPixelsBetweenADay = mPixelsBetweenADay - (mPixelsBetweenADay * .2f);
                if (mPixelsBetweenAnHour == 0) throw new NullPointerException();

                loadSchedule();
                drawCurrentTime();

            }
        });
        return rootView;
    }

    private void initialiseViews(View rootView) {
        constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);

        textViewScheduletitle = rootView.findViewById(R.id.scheduleTitle);
        scheduleScrollView = rootView.findViewById(R.id.scheduleScrollView);
        scheduleScrollView.post(() -> scheduleScrollView.smoothScrollTo(0,0));


        mDrawableBreakLineLeft = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_break_line_left_svg);
        mDrawableBreakLineRight = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_break_line_right_svg);

        //Week Days
        textViewMonday = rootView.findViewById(R.id.textViewMonday);
        textViewTuesday = rootView.findViewById(R.id.textViewTuesday);
        textViewWednesday = rootView.findViewById(R.id.textViewWednesday);
        textViewThursday = rootView.findViewById(R.id.textViewThursday);
        textViewFriday = rootView.findViewById(R.id.textViewFriday);
        textViewSaturday = rootView.findViewById(R.id.textViewSaturday);
        textViewSunday = rootView.findViewById(R.id.textViewSunday);

        //Time lines with 2 hrs Interval 00:00 to 24:00
        view00 = rootView.findViewById(R.id.view00);
        view02 = rootView.findViewById(R.id.view02);
        view04 = rootView.findViewById(R.id.view04);
        view06 = rootView.findViewById(R.id.view06);
        view08 = rootView.findViewById(R.id.view08);
        view10 = rootView.findViewById(R.id.view10);
        view12 = rootView.findViewById(R.id.view12);
        view14 = rootView.findViewById(R.id.view14);
        view16 = rootView.findViewById(R.id.view16);
        view18 = rootView.findViewById(R.id.view18);
        view20 = rootView.findViewById(R.id.view20);
        view22 = rootView.findViewById(R.id.view22);
        view24 = rootView.findViewById(R.id.view24);

        //Time lines with 1hr Inerval 00:00 to 24:00
        view01 = rootView.findViewById(R.id.view01);
        view03 = rootView.findViewById(R.id.view03);
        view05 = rootView.findViewById(R.id.view05);
        view07 = rootView.findViewById(R.id.view07);
        view09 = rootView.findViewById(R.id.view09);
        view11 = rootView.findViewById(R.id.view11);
        view13 = rootView.findViewById(R.id.view13);
        view15 = rootView.findViewById(R.id.view15);
        view17 = rootView.findViewById(R.id.view17);
        view19 = rootView.findViewById(R.id.view19);
        view21 = rootView.findViewById(R.id.view21);
        view23 = rootView.findViewById(R.id.view23);


        //collecting each timeline to arraylist
        viewTimeLines = new ArrayList<>();
        viewTimeLines.add(view00);
        viewTimeLines.add(view01);
        viewTimeLines.add(view02);
        viewTimeLines.add(view03);
        viewTimeLines.add(view04);
        viewTimeLines.add(view05);
        viewTimeLines.add(view06);
        viewTimeLines.add(view07);
        viewTimeLines.add(view08);
        viewTimeLines.add(view09);
        viewTimeLines.add(view10);
        viewTimeLines.add(view11);
        viewTimeLines.add(view12);
        viewTimeLines.add(view13);
        viewTimeLines.add(view14);
        viewTimeLines.add(view15);
        viewTimeLines.add(view16);
        viewTimeLines.add(view17);
        viewTimeLines.add(view18);
        viewTimeLines.add(view19);
        viewTimeLines.add(view20);
        viewTimeLines.add(view21);
        viewTimeLines.add(view22);
        viewTimeLines.add(view23);
        viewTimeLines.add(view24);

        colorMinTemp = String.valueOf(ContextCompat.getColor(requireContext(), R.color.min_temp));
        colorMaxTemp = String.valueOf(ContextCompat.getColor(requireContext(), R.color.max_temp));
    }

    private boolean validateNamedSchedule() {
        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
        ArrayList<Interval> intervalSpills = new ArrayList<>();
        ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals();
        boolean isValid = true;
        StringBuilder warningMessage = new StringBuilder("No such Schedule");
        if (getArguments() != null && getArguments().containsKey(PARAM_SCHEDULE_ID)) {
            warningMessage = new StringBuilder();
            Schedule namedSchedule =
                    CCUHsApi.getInstance().getScheduleById(getArguments().getString(PARAM_SCHEDULE_ID));
            String roomRef = getArguments().getString(PARAM_ROOM_REF);

            for (Interval v : systemIntervals) {
                CcuLog.d("CCU_UI", "Merged System interval " + v);
            }

            ArrayList<Interval> zoneIntervals = namedSchedule.getScheduledIntervals();
            int size = zoneIntervals.size();

            for (int i = 0; i < size; i++) {
                Interval it = zoneIntervals.get(i);

                LocalTime startTimeOfDay = it.getStart().toLocalTime();
                LocalTime endTimeOfDay = it.getEnd().toLocalTime();

                // Check if the start time is after the end time and separating the overnight schedule
                if (startTimeOfDay.isAfter(endTimeOfDay)) {
                    zoneIntervals.set(i, ScheduleUtil.OverNightEnding(it));
                    zoneIntervals.add(ScheduleUtil.OverNightStarting(it));
                }
            }
            //sorting the zoneInterval
            Collections.sort(zoneIntervals, new Comparator<Interval>() {
                            public int compare(Interval p1, Interval p2) {
                                return Long.compare(p1.getStartMillis(), p2.getStartMillis());
                            }
                        }
                );

            Interval ZonelastInterval = zoneIntervals.get(zoneIntervals.size()-1);
            LocalDate ZoneLastTimeOfDay = ZonelastInterval.getStart().toDateTime().toLocalDate();
            Interval systemLastInterval = systemIntervals.get(systemIntervals.size()-1);
            LocalDate systemLastTimeOfDay = systemLastInterval.getStart().toDateTime().toLocalDate();
            /** checking for overnight for sunday ,if it is has overnight sch for sunday
             we need to add the building occupancy for next week monday also **/
            if(ZoneLastTimeOfDay.isAfter(systemLastTimeOfDay))
            {
                systemIntervals.add(ScheduleUtil.AddingNextWeekDayForOverNight(systemSchedule));
            }

            for (Interval z : zoneIntervals) {
                boolean add = true;
                for (Interval s : systemIntervals) {
                    if (s.contains(z)) {
                        add = false;
                        break;
                    } else if (s.overlaps(z)) {
                        add = false;
                        for (Interval i : disconnectedIntervals(systemIntervals, z)) {
                            if (!intervalSpills.contains(i)) {
                                intervalSpills.add(i);
                            }
                        }

                    }
                }
                if (add) {
                    intervalSpills.add(z);
                    CcuLog.d(TAG, " Zone Interval not contained " + z);
                }
            }


            if (!intervalSpills.isEmpty() ) {
                StringBuilder spillZones = new StringBuilder();
                for (Interval i : intervalSpills) {
                    spillZones.append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek()))
                            .append(" ").append("").append(i.getStart().hourOfDay().get())
                            .append(":").append(i.getStart().minuteOfHour().get() == 0 ? "00" :
                                    i.getStart().minuteOfHour().get()).append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get())).append(":")
                            .append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(" \n");

                }

                warningMessage.append(getText(R.string.warning_msg)).append("\n\t").append(spillZones)
                        .append("\n\n");
                isValid = false;
            }

            double coolingDeadband = TunerUtil.getZoneCoolingDeadband(roomRef);
            double heatingDeadband = TunerUtil.getZoneHeatingDeadband(roomRef);
            if( isCelsiusTunerAvailableStatus()) {
                coolingDeadband = fahrenheitToCelsiusRelative(coolingDeadband);
                heatingDeadband = fahrenheitToCelsiusRelative(heatingDeadband);
                for (Schedule.Days namedSchedDay:namedSchedule.getDays()) {
                    double deadbandNamedSched = namedSchedDay.getCoolingVal()-namedSchedDay.getHeatingVal();
                    deadbandNamedSched=fahrenheitToCelsiusRelative(deadbandNamedSched);
                    if(deadbandNamedSched < coolingDeadband+heatingDeadband){
                        warningMessage.append(getText(R.string.deadband_warning)).append("\n\t").append("Deadband in Named schedule - ")
                                .append(fahrenheitToCelsiusRelative(deadbandNamedSched)).append("\u00B0C").append("\n\t").append("Deadband for zone: CoolingDeadband - ").append(coolingDeadband).append("\u00B0C").append("\n\t\t").append("HeatingDeadband - ").append(heatingDeadband).append("\u00B0C").append("\n\n");
                        isValid = false;
                        break;
                    }
                }
            } else {
                for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
                    double deadbandNamedSched = namedSchedDay.getCoolingVal() - namedSchedDay.getHeatingVal();
                    if (deadbandNamedSched < coolingDeadband + heatingDeadband) {
                        warningMessage.append(getText(R.string.deadband_warning)).append("\n\t").append("Deadband in Named schedule - ")
                                .append(deadbandNamedSched).append("\u00B0F").append("\n\t").append("Deadband for zone: CoolingDeadband - ").append(coolingDeadband).append("\u00B0F").append("\n\t\t").append("HeatingDeadband - ").append(heatingDeadband).append("\u00B0F").append("\n\n");
                        isValid = false;
                        break;
                    }
                }
            }

            StringBuilder desiredTempWarning = new StringBuilder();
            boolean isDesiredTempValid = true;
            for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
                if (!(namedSchedDay.getHeatingVal() <= namedSchedDay.getHeatingUserLimitMax()
                        && namedSchedDay.getHeatingVal() >= namedSchedDay.getHeatingUserLimitMin()
                        && namedSchedDay.getCoolingVal() <= namedSchedDay.getCoolingUserLimitMax()
                        && namedSchedDay.getCoolingVal() >= namedSchedDay.getCoolingUserLimitMin())) {
                    String[] dayName = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
                            "Saturday", "Sunday"};
                    if (isCelsiusTunerAvailableStatus()) {
                        desiredTempWarning.append("\t").append(dayName[namedSchedDay.getDay()]).append("-")
                                .append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm())
                                .append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm())
                                .append("(CDT - ").append(fahrenheitToCelsius(namedSchedDay.getCoolingVal())).append("\u00B0C").append(";")
                                .append("HDT - ").append(fahrenheitToCelsius(namedSchedDay.getHeatingVal())).append("\u00B0C").append(")")
                                .append("Schedule user limit : Heating - ")
                                .append((fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMin()))).append("\u00B0C").append("~").append((fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMax()))).append("\u00B0C")
                                .append("\n\t Cooling - ").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMin())).append("\u00B0C").append("~").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMax())).append("\u00B0C")
                                .append("\n\n").append("\n\t\t");
                    } else {
                        desiredTempWarning.append("\t\t").append(dayName[namedSchedDay.getDay()]).append("-")
                                .append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm())
                                .append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm())
                                .append("(CDT - ").append(namedSchedDay.getCoolingVal()).append("\u00B0F").append(";")
                                .append("HDT - ").append(namedSchedDay.getHeatingVal()).append("\u00B0F").append(")")
                                .append("Schedule user limit : Heating  - ")
                                .append(namedSchedDay.getHeatingUserLimitMin()).append("\u00B0F").append("~").append(namedSchedDay.getHeatingUserLimitMax()).append("\u00B0F")
                                .append("\n\t\t Cooling - ").append(namedSchedDay.getCoolingUserLimitMin()).append("\u00B0F").append("~").append(namedSchedDay.getCoolingUserLimitMax()).append("\u00B0F")
                                .append("\n\n");
                    }
                    isDesiredTempValid = false;
                }
            }

            if (!isDesiredTempValid) {

                if (isCelsiusTunerAvailableStatus()) {
                    warningMessage.append(getText(R.string.desiredTemp_warning)).append("\n\t")
                            .append("Named schedule desired temperature : \n\t\t").append(desiredTempWarning)
                    ;
                } else {
                    warningMessage.append(getText(R.string.desiredTemp_warning)).append("\n\t")
                            .append("Named schedule desired temperature : \n\t\t").append(desiredTempWarning);
                }

                isValid = false;
            }





            StringBuilder userLimitWarning = new StringBuilder();
            boolean isLimitValid = true;
            for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
                if (!(MasterControlUtil.validateNamed(namedSchedDay.getHeatingUserLimitMin(),
                        namedSchedDay.getCoolingUserLimitMax(), namedSchedule.getUnoccupiedZoneSetback()))) {
                    String[] dayName = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
                            "Saturday", "Sunday"};
                    if (isCelsiusTunerAvailableStatus()) {
                        userLimitWarning.append("\t\t").append(dayName[namedSchedDay.getDay()]).append("-")
                                .append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm())
                                .append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm())
                                .append("(Heating User Limit Min  - ").append(fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMin())).append("\u00B0C")
                                .append("Cooling User Limit Max  - ").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMax())).append("\u00B0C").append(")").append("\n\t\t");
                    } else {
                        userLimitWarning.append("\t\t").append(dayName[namedSchedDay.getDay()]).append("-")
                                .append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm())
                                .append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm())
                                .append("(Heating User Limit Min - ").append(namedSchedDay.getHeatingUserLimitMin()).append("\u00B0F")
                               .append("Cooling User Limit Max - ").append(namedSchedDay.getCoolingUserLimitMax()).append("\u00B0F").append(")").append("\n\t\t");
                    }
                    isLimitValid = false;
                }
            }
            if (!isLimitValid) {
                if (isCelsiusTunerAvailableStatus()) {
                    warningMessage.append(getText(R.string.limits_warning)).append("\n\t")
                            .append("Named schedule limits : \n\t\t").append(userLimitWarning)
                            .append("Building limits :")
                            .append((fahrenheitToCelsius(BuildingTunerCache.getInstance().getBuildingLimitMin()))).append("\u00B0C").append("~").append(fahrenheitToCelsius(BuildingTunerCache.getInstance().getBuildingLimitMax())).append("\u00B0C")
                            .append("\n\n");
                } else {
                    warningMessage.append(getText(R.string.limits_warning)).append("\n\t")
                            .append("Named schedule limits : \n\t\t").append(userLimitWarning)
                            .append("Building limits : ")
                            .append(BuildingTunerCache.getInstance().getBuildingLimitMin()).append("\u00B0F").append("~")
                            .append(BuildingTunerCache.getInstance().getBuildingLimitMax()).append("\u00B0F")
                            .append("\n\n");
                }

                isValid = false;
            }


            StringBuilder deadBandWarning = new StringBuilder();
            boolean isDeadBandValid = true;
            for (Schedule.Days namedSchedDay : namedSchedule.getDays()) {
                if (!MasterControlUtil.validateNamedDeadBand(namedSchedDay.getHeatingUserLimitMin(),
                        namedSchedDay.getHeatingUserLimitMax(),
                        namedSchedDay.getCoolingUserLimitMin(),
                        namedSchedDay.getCoolingUserLimitMax(),namedSchedDay.getHeatingDeadBand(),
                namedSchedDay.getCoolingDeadBand())) {
                    String[] dayName = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
                            "Saturday", "Sunday"};
                    if (isCelsiusTunerAvailableStatus()) {
                        deadBandWarning.append("\t\t").append(dayName[namedSchedDay.getDay()]).append("-")
                                .append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm())
                                .append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm())
                                .append("(Heating Lim Min|Max - ").append(fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMin())).append("\u00B0C")
                                .append("|").append(fahrenheitToCelsius(namedSchedDay.getHeatingUserLimitMax())).append("\u00B0C").append(";")
                                .append("\nCooling Lim Min|Max- ").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMin())).append("\u00B0C")
                                .append("|").append(fahrenheitToCelsius(namedSchedDay.getCoolingUserLimitMax())).append("\u00B0C").append(")")
                                .append("\nCooling deadBand- ").append(fahrenheitToCelsius(namedSchedDay.getCoolingDeadBand())).append("\u00B0C")
                                .append(" Heating deadBand- ").append(fahrenheitToCelsius(namedSchedDay.getHeatingDeadBand())).append("\u00B0C")
                                .append("\n\t\t");
                    } else {
                        deadBandWarning.append("\t\t").append(dayName[namedSchedDay.getDay()]).append("-")
                                .append(namedSchedDay.getSthh()).append(":").append(namedSchedDay.getStmm())
                                .append("-").append(namedSchedDay.getEthh()).append(":").append(namedSchedDay.getEtmm())
                                .append("( Heating Lim Min|Max - ").append(namedSchedDay.getHeatingUserLimitMin()).append("\u00B0F")
                                .append("|").append(namedSchedDay.getHeatingUserLimitMax()).append("\u00B0F").append(";")
                                .append("\nCooling Lim Min|Max- ").append(namedSchedDay.getHeatingUserLimitMin()).append("\u00B0F").append("|")
                                .append(namedSchedDay.getCoolingUserLimitMax()).append("\u00B0F")
                                .append("\nCooling deadBand- ").append(namedSchedDay.getCoolingDeadBand()).append("\u00B0F")
                                .append("  Heating deadBand- ").append(namedSchedDay.getHeatingDeadBand()).append("\u00B0F")
                                .append("|").append(namedSchedDay.getCoolingUserLimitMax()).append("\u00B0F").append(")").append("\n\t\t");
                    }
                    isDeadBandValid = false;
                }
            }
            if (!isDeadBandValid) {
                warningMessage.append("Named schedule limits and deadbands are viloating on below zones: \n\t\t").append(deadBandWarning)
                        .append("\n\t\tThe difference in limit maximum and minimum to be more or than or equal to the deadband and " +
                                "\n\t\tHeating Limit Max + deadband (heating + cooling) should be less than or equal to Cooling Limit Max" +
                                "\n\t\tCooling Limit min - deadband (heating + cooling) should be greater than or equal to Heating Limit Min ")
                        .append("\n\n");


                isValid = false;
            }
        }
        if (!isValid) {
            warningMessage.append("\n\n").append(getText(R.string.pls_goback));
            android.app.AlertDialog.Builder builder =
                    new android.app.AlertDialog.Builder(getActivity());
            builder.setMessage(warningMessage.toString())
                    .setCancelable(false)
                    .setTitle(R.string.warning_ns)
                    .setIcon(R.drawable.ic_alert)
                    .setNegativeButton("OKAY", (dialog, id) -> dialog.dismiss());

            AlertDialog alert = builder.create();
            alert.show();
        }
        return isValid;

    }

    private void loadSchedule()
    {

        if (getArguments() != null && getArguments().containsKey(PARAM_SCHEDULE_ID)) {
            mScheduleId = getArguments().getString(PARAM_SCHEDULE_ID);
            schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
        } else {
            schedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
            CcuLog.d("CCU_UI"," Loaded System Schedule "+schedule.toString());
        }
        CcuLog.d(TAG,"PARAM_SCHEDULE_ID "+mScheduleId);
        String namedScheduledis = getArguments().getString(PARAM_SCHED_NAME);
        String title;
        String scheduledName = getArguments().getString(PARAM_SCHED_NAME);
        boolean isScheduledSet = getArguments().getBoolean(PARAM_SCHED_SET);
        int maxLength = 20;

        if (!isScheduledSet) {
            if (namedScheduledis.length() > 20) {
                title = scheduledName.substring(0, 20) + "...";
            } else {
                title = scheduledName;
            }
        } else {
            if (namedScheduledis.length() > 20) {
                title = "Preview : " + scheduledName.substring(0, 20) + "...";
            } else {
                title = "Preview : " + scheduledName;
            }
        }

        textViewScheduletitle.setText(title);
        updateUI();
    }

    private void updateUI() {
        schedule.populateIntersections();

        new Handler(Looper.getMainLooper()).post(() -> {

            hasTextViewChildren();
            ArrayList<Schedule.Days> days = schedule.getDays();
            days.sort(Comparator.comparingInt(Schedule.Days::getSthh));
            days.sort(Comparator.comparingInt(Schedule.Days::getDay));

            for(int i = 0; i < 7; i++){
                drawSchedule(i, 0,0,0, 23, 0, 59, DAYS.values()[i],
                        false,
                        false);
            }

            for (int i = 0; i < days.size(); i++) {
                Schedule.Days daysElement = days.get(i);
                drawSchedule(i, daysElement.getCoolingVal(), daysElement.getHeatingVal(),
                        daysElement.getSthh(), daysElement.getEthh(),
                        daysElement.getStmm(), daysElement.getEtmm(),
                        DAYS.values()[daysElement.getDay()], daysElement.isIntersection(),true);
            }
        });
    }

    private void hasTextViewChildren() {

        for (int i = constraintScheduler.getChildCount() - 1; i >= 0; i--) {
            if (constraintScheduler.getChildAt(i).getTag() != null) {
                constraintScheduler.removeViewAt(i);
            }
        }

    }

    @SuppressLint("LogNotTimber")
    private void drawCurrentTime() {

        DateTime now = new DateTime(MockTime.getInstance().getMockTime());


        DAYS day = DAYS.values()[now.getDayOfWeek() - 1];
        Log.i("Scheduler", "DAY: " + day.toString());
        int hh = now.getHourOfDay();
        int mm = now.getMinuteOfHour();


        AppCompatImageView imageView = new AppCompatImageView(requireActivity());

        imageView.setImageResource(R.drawable.ic_time_marker_svg);
        imageView.setId(View.generateViewId());
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int)mPixelsBetweenADay);
        lp.bottomToBottom = getTextViewFromDay(day).getId();
        lp.topToTop = getTextViewFromDay(day).getId();
        lp.startToStart = viewTimeLines.get(hh).getId();

        lp.leftMargin = (int) ((mm / 60.0) * mPixelsBetweenAnHour);

        constraintScheduler.addView(imageView, lp);


    }

    private void drawScheduleBlock(int position, String strminTemp, String strmaxTemp, Typeface typeface,
                                   int tempStartTime, int tempEndTime,
                                   int startTimeMM, int endTimeMM, TextView textView,
                                   boolean leftBreak, boolean rightBreak, boolean intersection,boolean isOccupied) {

        CcuLog.i(TAG, "position: "+position+" tempStartTime: " + tempStartTime + " tempEndTime: " + tempEndTime + " startTimeMM: " + startTimeMM + " endTimeMM " + endTimeMM);

        if (getContext() == null) return;
        AppCompatTextView textViewTemp = new AppCompatTextView(getContext());


        textViewTemp.setGravity(Gravity.CENTER_HORIZONTAL);
        String celsiusUnitMin = FontManager.getColoredSpanned("\u00B0C", colorMinTemp);
        String celsiusUnitMax = FontManager.getColoredSpanned("\u00B0C", colorMaxTemp);
        String farenUnitMin = FontManager.getColoredSpanned("\u00B0F", colorMinTemp);
        String farenUnitMax = FontManager.getColoredSpanned("\u00B0F", colorMaxTemp);
        if (isOccupied) {
            if (isCelsiusTunerAvailableStatus()) {
                textViewTemp.setText(Html.fromHtml(strminTemp + celsiusUnitMin + " " + strmaxTemp + celsiusUnitMax, Html.FROM_HTML_MODE_LEGACY));
            } else {
                textViewTemp.setText(Html.fromHtml(strminTemp + farenUnitMin + " " + strmaxTemp + farenUnitMax, Html.FROM_HTML_MODE_LEGACY));
            }
        }
        if (typeface != null)
            textViewTemp.setTypeface(typeface);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(textViewTemp, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        textViewTemp.setMaxLines(2);
        textViewTemp.setContentDescription(textView.getText().toString() + "_" + tempStartTime + ":" + startTimeMM + "-" + tempEndTime + ":" + endTimeMM);
        textViewTemp.setId(ViewCompat.generateViewId());
        textViewTemp.setTag(position);


        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int) mPixelsBetweenADay);
        lp.baselineToBaseline = textView.getId();


        int leftMargin = startTimeMM > 0 ? (int) ((startTimeMM / 60.0) * mPixelsBetweenAnHour) : lp.leftMargin;
        int rightMargin = endTimeMM > 0 ? (int) (((60 - endTimeMM) / 60.0) * mPixelsBetweenAnHour) : lp.rightMargin;

        lp.leftMargin = leftMargin;
        lp.rightMargin = rightMargin;

        Drawable drawableCompat;

        if (leftBreak) {
            drawableCompat =  ContextCompat.getDrawable(requireContext(),R.drawable.occupancy_background_left);
            if (intersection) {
                Drawable rightGreyBar = ContextCompat.getDrawable(requireContext(),R.drawable.vline);
                if(isOccupied)
                    textViewTemp.setCompoundDrawablesWithIntrinsicBounds(mDrawableBreakLineLeft, null, rightGreyBar, null);
            }else if(isOccupied)
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(mDrawableBreakLineLeft, null, null, null);

            Space space = new Space(getActivity());
            space.setId(View.generateViewId());
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());


            ConstraintLayout.LayoutParams spaceLP = new ConstraintLayout.LayoutParams((int) px, 10);
            spaceLP.rightToLeft = viewTimeLines.get(tempStartTime).getId();

            constraintScheduler.addView(space, spaceLP);


            if (endTimeMM > 0)
                tempEndTime++;

            lp.startToStart = space.getId();
            lp.endToEnd = viewTimeLines.get(tempEndTime).getId();


        } else if (rightBreak) {
            drawableCompat = ContextCompat.getDrawable(requireContext(),R.drawable.occupancy_background_left);
            if(isOccupied)
              textViewTemp.setCompoundDrawablesWithIntrinsicBounds(null, null, mDrawableBreakLineRight, null);
            Space space = new Space(getActivity());
            space.setId(View.generateViewId());
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());


            ConstraintLayout.LayoutParams spaceLP = new ConstraintLayout.LayoutParams((int) px, 10);
            spaceLP.leftToRight = viewTimeLines.get(tempEndTime).getId();

            constraintScheduler.addView(space, spaceLP);

            lp.startToStart = viewTimeLines.get(tempStartTime).getId();
            lp.endToEnd = space.getId();
        } else {


            if (intersection) {
                Drawable rightGreyBar = ContextCompat.getDrawable(requireContext(),R.drawable.vline);
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        rightGreyBar, null);
            }


            drawableCompat = ContextCompat.getDrawable(requireContext(),isOccupied ? R.drawable.occupancy_background_left
                    :R.drawable.occupancy_background_unoccupied);

            if (endTimeMM > 0)
                tempEndTime++;

            lp.startToStart = viewTimeLines.get(tempStartTime).getId();
            lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
        }

        textViewTemp.setBackground(drawableCompat);
        constraintScheduler.addView(textViewTemp, lp);


        textViewTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOccupied) {
                    FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
                    NamedScheduleUnoccupiedDailog namedScheduleUnoccupiedDailog =
                            NamedScheduleUnoccupiedDailog.newInstance(mScheduleId,(int)v.getTag());
                    namedScheduleUnoccupiedDailog.show(fragmentTransaction, "popup");

                } else {
                    // force refresh schedule
                    if (mScheduleId != null) {
                        schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
                        ArrayList<Schedule.Days> days = schedule.getDays();


                        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
                        Fragment namedSchedulefragment = getChildFragmentManager().findFragmentByTag("popup");
                        if (namedSchedulefragment != null) {
                            fragmentTransaction.remove(namedSchedulefragment);
                        }
                        NamedScheduleDialogFragment namedScheduleDialogFragment =
                                NamedScheduleDialogFragment.newInstance(mScheduleId,(int)v.getTag());
                        namedScheduleDialogFragment.show(fragmentTransaction, "popup");
                        int clickedPosition = (int) v.getTag();


                        try {
                            Collections.sort(days, (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
                            Collections.sort(days, (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));


                        } catch (ArrayIndexOutOfBoundsException e) {
                            Log.d(TAG, "onClick: " + e.getMessage());
                        }
                    }
                }
            }
        });

    }


    private TextView getTextViewFromDay(DAYS day) {
        switch (day) {
            case MONDAY:
                return textViewMonday;

            case TUESDAY:
                return textViewTuesday;

            case WEDNESDAY:
                return textViewWednesday;

            case THURSDAY:
                return textViewThursday;

            case FRIDAY:
                return textViewFriday;

            case SATURDAY:
                return textViewSaturday;

            default:
                return textViewSunday;
        }
    }


    private void drawSchedule(int position, double heatingTemp, double coolingTemp,
                              int startTimeHH, int endTimeHH, int startTimeMM,
                              int endTimeMM, DAYS day, boolean intersection,boolean isOccupied) {


        if(isCelsiusTunerAvailableStatus()) {
            coolingTemp =(fahrenheitToCelsius(coolingTemp));
            heatingTemp=(fahrenheitToCelsius(heatingTemp));
        }
        String strminTemp = FontManager.getColoredSpanned(Double.toString(coolingTemp), colorMinTemp);
        String strmaxTemp = FontManager.getColoredSpanned(Double.toString(heatingTemp), colorMaxTemp);

        Typeface typeface=Typeface.DEFAULT;
        try {
            typeface = Typeface.createFromAsset(requireActivity().getAssets(), "fonts/lato_regular.ttf");
        }catch (Exception e){
            e.printStackTrace();
        }

        if (startTimeHH > endTimeHH || (startTimeHH == endTimeHH && startTimeMM > endTimeMM)) {
            drawScheduleBlock(position, strminTemp, strmaxTemp, typeface, startTimeHH,
                    24, startTimeMM, 0,
                    getTextViewFromDay(day), false, true, intersection,isOccupied);
            drawScheduleBlock(position, strminTemp, strmaxTemp, typeface, 0,
                    endTimeHH, 0, endTimeMM,
                    getTextViewFromDay(day.getNextDay()), true, false, intersection
                    ,isOccupied);
        } else {
            drawScheduleBlock(position, strminTemp, strmaxTemp,
                    typeface, startTimeHH, endTimeHH, startTimeMM,
                    endTimeMM, getTextViewFromDay(day), false, false,
                    intersection,isOccupied);
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(flag == false && OnCancel!=null)
        {
            OnCancel.onCancelButtonClicked();
        }
    }
}
