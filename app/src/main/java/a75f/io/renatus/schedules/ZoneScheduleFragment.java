package a75f.io.renatus.schedules;

import androidx.fragment.app.Fragment;

import static a75f.io.api.haystack.util.TimeUtil.getEndTimeHr;
import static a75f.io.api.haystack.util.TimeUtil.getEndTimeMin;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.validateDesiredTemp;
import static a75f.io.usbserial.UsbModbusService.TAG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import org.javolution.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.interfaces.BuildingScheduleListener;
import a75f.io.messaging.handler.UpdateScheduleHandler;

import a75f.io.renatus.R;

import a75f.io.renatus.util.FontManager;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;
import a75f.io.renatus.views.RangeBar;


public class ZoneScheduleFragment extends DialogFragment implements ZoneScheduleDialogFragment.ZoneScheduleDialogListener, BuildingScheduleListener, UnOccupiedZoneSetBackDialogFragment.UnOccupiedZoneSetBackListener {

    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";
    private static final String PARAM_ROOM_REF = "PARAM_ROOM_REF";
    private Drawable mDrawableBreakLineLeft;
    private Drawable mDrawableBreakLineRight;
    private Drawable mDrawableTimeMarker;
    private float mPixelsBetweenAnHour;
    private float mPixelsBetweenADay;
    TextView textViewMonday;
    TextView textViewTuesday;
    TextView textViewWednesday;
    TextView textViewThursday;
    TextView textViewFriday;
    TextView textViewSaturday;
    TextView textViewSunday;
    View view00, view02, view04, view06, view08, view10, view12, view14, view16, view18, view20, view22, view24;
    View view01, view03, view05, view07, view09, view11, view13, view15, view17, view19, view21, view23;
    TextView textViewScheduletitle;
    TextView textViewaddEntry;
    Schedule schedule;
    ConstraintLayout constraintScheduler;
    ArrayList<View> viewTimeLines;
    String mScheduleId;
    String colorMinTemp = "";
    String colorMaxTemp = "";
    NestedScrollView scheduleScrollView;
    ZoneScheduleViewModel zoneScheduleViewModel;
    private static final int ID_DIALOG_OCCUPIED_SCHEDULE = 1;
    private static final int ID_DIALOG_UN_OCCUPIED_SCHEDULE = 2;
    private ZoneScheduleFragment.OnExitListener mOnExitListener;

    @Override
    public void onStop() {
        super.onStop();
        if (mOnExitListener != null)
            mOnExitListener.onExit();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && scheduleScrollView != null){
            scheduleScrollView.post(() -> scheduleScrollView.smoothScrollTo(0,0));
        }
        if (isVisibleToUser) {
            new Handler().post(() -> loadSchedule());
            UpdateScheduleHandler.setBuildingScheduleListener(this);
        } else {
            UpdateScheduleHandler.setBuildingScheduleListener(null);
        }
    }


    public ZoneScheduleFragment(){

    }
    public ZoneScheduleFragment(String roomRef) {
        HashMap<Object, Object> zoneHashMap = CCUHsApi.getInstance().readMapById(roomRef);
        Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();
        mScheduleId = build.getScheduleRef();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, mScheduleId);
    }

    public static ZoneScheduleFragment newInstance() {
        return new ZoneScheduleFragment();
    }

    public static ZoneScheduleFragment newInstance(String scheduleId) {
        ZoneScheduleFragment schedulerFragment = new ZoneScheduleFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, scheduleId);
        schedulerFragment.setArguments(args);
        return schedulerFragment;
    }

    public static ZoneScheduleFragment newInstance(String scheduleId, String roomRef) {
        ZoneScheduleFragment schedulerFragment = new ZoneScheduleFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, scheduleId);
        args.putString(PARAM_ROOM_REF, roomRef);
        schedulerFragment.setArguments(args);
        return schedulerFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            setShowsDialog(true);
        } else {
            setShowsDialog(false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getArguments()!= null) {
            outState.putString(PARAM_ROOM_REF, getArguments().getString(PARAM_ROOM_REF));
        }
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
        View rootView = inflater.inflate(R.layout.fragment_zone_schedule, container, false);

        //Scheduler Layout
        constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);
        textViewScheduletitle = rootView.findViewById(R.id.scheduleTitle);
        textViewaddEntry = rootView.findViewById(R.id.addEntry);
        scheduleScrollView = rootView.findViewById(R.id.scheduleScrollView);
        scheduleScrollView.post(() -> scheduleScrollView.smoothScrollTo(0,0));

        mDrawableBreakLineLeft = AppCompatResources.getDrawable(getContext(), R.drawable.ic_break_line_left_svg);
        mDrawableBreakLineRight = AppCompatResources.getDrawable(getContext(), R.drawable.ic_break_line_right_svg);
        mDrawableTimeMarker = AppCompatResources.getDrawable(getContext(), R.drawable.ic_time_marker_svg);

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
        viewTimeLines = new ArrayList<View>();
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

        colorMinTemp = getResources().getString(0 + R.color.min_temp);
        colorMinTemp = "#" + colorMinTemp.substring(3);
        colorMaxTemp = getResources().getString(0 + R.color.max_temp);
        colorMaxTemp = "#" + colorMaxTemp.substring(3);


        textViewaddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
                Fragment buildingOccupancyFragment = getChildFragmentManager().findFragmentByTag("popup");
                if(buildingOccupancyFragment != null){
                    fragmentTransaction.remove(buildingOccupancyFragment);
                }
                showDialog();
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
                mPixelsBetweenADay = constraintScheduler.getHeight() / 7;

                //Leave 20% for padding.
                mPixelsBetweenADay = mPixelsBetweenADay - (mPixelsBetweenADay * .2f);
                if (mPixelsBetweenAnHour != 0) {
                    loadSchedule();
                    drawCurrentTime();
                }

            }
        });
        return rootView;
    }


    private void loadSchedule()
    {
        if (mScheduleId != null) {
            schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
            updateUI();
        } else {
            ArrayList<Schedule> buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancySchedule();
            if(buildingOccupancy.size() == 0){
                schedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
            }else{
                schedule = CCUHsApi.getInstance().getBuildingOccupancySchedule().get(0);
            }

            CcuLog.d(L.TAG_CCU_UI," Loaded System Schedule " + schedule.toString());
        }

    }

    private void updateUI() {
        schedule.populateIntersections();
        new Handler(Looper.getMainLooper()).post(() -> {
            hasTextViewChildren();
            List<Schedule.Days> days = schedule.getDays();
            days.sort((lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
            days.sort((lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
            zoneScheduleViewModel = new ZoneScheduleViewModel();
            List<UnOccupiedDays> unoccupiedDays = zoneScheduleViewModel.getUnoccupiedDays(days);
            for (int i = 0; i < days.size(); i++) {
                Schedule.Days occupiedDaysElement = days.get(i);
                if (occupiedDaysElement.getSthh() > occupiedDaysElement.getEthh()) {
                    for (int j = 0; j < unoccupiedDays.size(); j++) {
                        UnOccupiedDays daysElement1 = unoccupiedDays.get(j);
                        if (occupiedDaysElement.getDay() == daysElement1.getDay() && occupiedDaysElement.getEthh() == daysElement1.getSthh() ){
                            if(unoccupiedDays.get(j).getDay() == 6){
                                unoccupiedDays.remove(j);
                                unoccupiedDays.get(0).setSthh(daysElement1.getSthh());
                                unoccupiedDays.get(0).setStmm(daysElement1.getStmm());
                            }else {
                                unoccupiedDays.remove(j);
                                unoccupiedDays.get(j).setSthh(daysElement1.getSthh());
                                unoccupiedDays.get(j).setStmm(daysElement1.getStmm());
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < unoccupiedDays.size(); i++) {
                UnOccupiedDays daysElement = unoccupiedDays.get(i);
                drawSchedule(i, 0,0,daysElement.getSthh(), daysElement.getEthh(),
                        daysElement.getStmm(), daysElement.getEtmm(),
                        DAYS.values()[daysElement.getDay()], daysElement.isIntersection(), false);
            }

            for (int i = 0; i < days.size(); i++) {
                Schedule.Days daysElement = days.get(i);
                drawSchedule(i, daysElement.getCoolingVal(), daysElement.getHeatingVal(),
                        daysElement.getSthh(), daysElement.getEthh(),
                        daysElement.getStmm(), daysElement.getEtmm(),
                        DAYS.values()[daysElement.getDay()], daysElement.isIntersection(), true);
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

    private void showDialog() {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment prev = getParentFragmentManager().findFragmentByTag("popup");
        if (prev != null) {
            ft.remove(prev);
        }
        ZoneScheduleDialogFragment newFragment = new ZoneScheduleDialogFragment(this, schedule);
        newFragment.show(ft, "popup");
    }

    private void showDialog( Schedule.Days day) {

        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment scheduleFragment = getParentFragmentManager().findFragmentByTag("popup");
        if (scheduleFragment != null) {
            ft.remove(scheduleFragment);
        }
        ZoneScheduleDialogFragment newFragment = new ZoneScheduleDialogFragment(this, schedule, day);
        newFragment.show(ft, "popup");
    }

    private void showDialog(int id, int position, Schedule schedule) {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Schedule.Days occupiedDays;
        UnOccupiedDays unOccupiedDays;

        switch (id) {
            case ID_DIALOG_OCCUPIED_SCHEDULE:
                occupiedDays = schedule.getDays().get(position);
                Fragment scheduleFragment = getParentFragmentManager().findFragmentByTag("popup");
                if (scheduleFragment != null) {
                    ft.remove(scheduleFragment);
                }
                ZoneScheduleDialogFragment newFragment = new ZoneScheduleDialogFragment(this, position, occupiedDays, schedule);
                newFragment.show(ft, "popup");
                break;

            case ID_DIALOG_UN_OCCUPIED_SCHEDULE:
                Fragment unOccupiedSetBackFragment = getParentFragmentManager().findFragmentByTag("popup");
                if (unOccupiedSetBackFragment != null) {
                    ft.remove(unOccupiedSetBackFragment);
                }
                List<Schedule.Days> days = schedule.getDays();
                days.sort((lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
                days.sort((lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
                List<UnOccupiedDays> unoccupiedDays = zoneScheduleViewModel.getUnoccupiedDays(days);
                unOccupiedDays = unoccupiedDays.get(position);
                int occupiedSlotsSize = schedule.getDays().size();
                Schedule.Days nextDay = getNextOccupiedSlot(position, unOccupiedDays, occupiedSlotsSize);
                UnOccupiedZoneSetBackDialogFragment unOccupiedZoneSetBackDialogFragment = new UnOccupiedZoneSetBackDialogFragment(this, nextDay, schedule);
                unOccupiedZoneSetBackDialogFragment.show(ft, "popup");

        }
    }

    private Schedule.Days getNextOccupiedSlot(int position, UnOccupiedDays unOccupiedDays,
                                              int size){

    ArrayList<Schedule.Days> occupiedDays = schedule.getDays();
    if(position - unOccupiedDays.getDay() < size) {
        if(unOccupiedDays.getEthh() == 24 && unOccupiedDays.getEtmm() == 0){
            if(occupiedDays.get(occupiedDays.size() - 1).getDay() <= unOccupiedDays.getDay()){
                return occupiedDays.get(0);
            }
            for (int i = 0; i < occupiedDays.size(); i++) {
                for(int j= 1; j < 6;j++) {
                    if (occupiedDays.get(i).getDay() == unOccupiedDays.getDay() + j) {
                        return occupiedDays.get(i);
                    }
                }
            }
        }
        for (int i = 0; i < occupiedDays.size(); i++) {
            if (occupiedDays.get(i).getDay() == unOccupiedDays.getDay()) {
                if (unOccupiedDays.getEthh() == occupiedDays.get(i).getSthh()) {
                    if (unOccupiedDays.getEtmm() == occupiedDays.get(i).getStmm()) {
                        return occupiedDays.get(i);
                    }
                }
            }
        }
    }
    return occupiedDays.get(0);
}

    private void showDialog(int position, ArrayList<Schedule.Days> days) {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment scheduleFragment = getParentFragmentManager().findFragmentByTag("popup");
        if (scheduleFragment != null) {
            ft.remove(scheduleFragment);
        }
        ZoneScheduleDialogFragment newFragment = new ZoneScheduleDialogFragment(this, position, days, schedule);
        newFragment.show(ft, "popup");
    }


    Schedule.Days removeEntry = null;
    public boolean onClickSave(int position, double coolingTemp, double heatingTemp, int startTimeHour,
                               int endTimeHour, int startTimeMinute, int endTimeMinute, ArrayList<DAYS> days,
                               Double heatingUserLimitMaxVal, Double heatingUserLimitMinVal, Double coolingUserLimitMaxVal,
                               Double coolingUserLimitMinVal, Double heatingDeadBandVal, Double coolingDeadBandVal,
                               boolean followBuilding, Schedule.Days mDay) {

        if (followBuilding) {
            HashMap<Object, Object> coolUL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and cooling and user and default");
            HashMap<Object, Object> heatUL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and heating and user and default");
            HashMap<Object, Object> coolLL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and cooling and user and default");
            HashMap<Object, Object> heatLL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and heating and user and default");
            HashMap<Object, Object> coolDB = CCUHsApi.getInstance().readEntity("schedulable and point and deadband and cooling and default");
            HashMap<Object, Object> heatDB = CCUHsApi.getInstance().readEntity("schedulable and point and deadband and heating and default");


            ArrayList<Schedule.Days> daysInSchedule = schedule.getDays();

            for (Schedule.Days eachOccupied : daysInSchedule) {

                if (validateDesiredTemp(eachOccupied.getCoolingVal(), eachOccupied.getHeatingVal(), HSUtil.getLevelValueFrom16(coolLL.get("id").toString()),
                        HSUtil.getLevelValueFrom16(coolUL.get("id").toString()), HSUtil.getLevelValueFrom16(heatLL.get("id").toString()),
                        HSUtil.getLevelValueFrom16(heatUL.get("id").toString()),
                        HSUtil.getLevelValueFrom16(heatDB.get("id").toString()),
                        HSUtil.getLevelValueFrom16(coolDB.get("id").toString())) != null) {
                    if (eachOccupied == mDay) {
                        heatingTemp = HSUtil.getLevelValueFrom16
                                (heatUL.get("id").toString()) - HSUtil.getLevelValueFrom16(heatDB.get("id").toString());
                        coolingTemp = HSUtil.getLevelValueFrom16
                                (coolLL.get("id").toString()) + HSUtil.getLevelValueFrom16(coolDB.get("id").toString());
                    }
                    schedule.getDay(eachOccupied).setCoolingVal(HSUtil.getLevelValueFrom16
                            (coolLL.get("id").toString()) + HSUtil.getLevelValueFrom16(coolDB.get("id").toString()));
                    schedule.getDay(eachOccupied).setHeatingVal(HSUtil.getLevelValueFrom16
                            (heatUL.get("id").toString()) - HSUtil.getLevelValueFrom16(heatDB.get("id").toString()));

                }
            }
        }

        if (position != ZoneScheduleDialogFragment.NO_REPLACE) {
            //sort schedule days according to the start hour of the day
            try {
                Collections.sort(schedule.getDays(), (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
                Collections.sort(schedule.getDays(), (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
                removeEntry = schedule.getDays().remove(position);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.d(TAG, "onClickSave: " + e.getMessage());
            }
        } else {
            removeEntry = null;
        }

        CcuLog.d(L.TAG_CCU_UI, " onClickSave " + "startTime " + startTimeHour + ":" + startTimeMinute + " endTime " + endTimeHour + ":" + endTimeMinute + " removeEntry " + removeEntry);

        ArrayList<Schedule.Days> daysArrayList = new ArrayList<>();
        zoneScheduleViewModel.doFollowBuildingUpdate(followBuilding, schedule);
        if(!schedule.getMarkers().contains(Tags.FOLLOW_BUILDING)) {
            if (days != null) {
                for (DAYS day : days) {
                    Schedule.Days dayBO = new Schedule.Days();
                    dayBO.setEthh(endTimeHour);
                    dayBO.setSthh(startTimeHour);
                    dayBO.setEtmm(endTimeMinute);
                    dayBO.setStmm(startTimeMinute);
                    dayBO.setHeatingVal(heatingTemp);
                    dayBO.setCoolingVal(coolingTemp);
                    dayBO.setSunset(false);
                    dayBO.setSunrise(false);
                    dayBO.setHeatingUserLimitMin(heatingUserLimitMinVal);
                    dayBO.setHeatingUserLimitMax(heatingUserLimitMaxVal);
                    dayBO.setCoolingUserLimitMin(coolingUserLimitMinVal);
                    dayBO.setCoolingUserLimitMax(coolingUserLimitMaxVal);
                    dayBO.setHeatingDeadBand(heatingDeadBandVal);
                    dayBO.setCoolingDeadBand(coolingDeadBandVal);
                    dayBO.setDay(day.ordinal());
                    daysArrayList.add(dayBO);
                }
            }
        }else {
            for (DAYS day : days) {
                Schedule.Days dayBO = new Schedule.Days();
                dayBO.setEthh(endTimeHour);
                dayBO.setSthh(startTimeHour);
                dayBO.setEtmm(endTimeMinute);
                dayBO.setStmm(startTimeMinute);
                dayBO.setHeatingVal(heatingTemp);
                dayBO.setCoolingVal(coolingTemp);
                dayBO.setSunset(false);
                dayBO.setSunrise(false);
                dayBO.setHeatingUserLimitMin(mDay == null ? 67 : mDay.getHeatingUserLimitMin());
                dayBO.setHeatingUserLimitMax(mDay == null ? 72 : mDay.getHeatingUserLimitMax());
                dayBO.setCoolingUserLimitMin(mDay == null ? 72 : mDay.getCoolingUserLimitMin());
                dayBO.setCoolingUserLimitMax(mDay == null ? 77 : mDay.getCoolingUserLimitMax());
                dayBO.setHeatingDeadBand(mDay == null ? 2 : mDay.getHeatingDeadBand());
                dayBO.setCoolingDeadBand(mDay == null ? 2 : mDay.getCoolingDeadBand());
                dayBO.setDay(day.ordinal());
                daysArrayList.add(dayBO);
            }
        }

        for (Schedule.Days d : daysArrayList) {
            CcuLog.d(L.TAG_CCU_UI, " daysArrayList  " + d);
        }

        boolean intersection = schedule.checkIntersection(daysArrayList);
        if (intersection) {

            StringBuilder overlapDays = new StringBuilder();
            for (Schedule.Days day : daysArrayList) {
                ArrayList<Interval> overlaps = schedule.getOverLapInterval(day);
                for (Interval overlap : overlaps) {
                    Log.d("CCU_UI", " overLap " + overlap);
                    overlapDays.append(getDayString(overlap.getStart()) + "(" + overlap.getStart().hourOfDay().get() + ":" + (overlap.getStart().minuteOfHour().get() == 0 ? "00" : overlap.getStart().minuteOfHour().get())
                            + " - " + (getEndTimeHr(overlap.getEnd().hourOfDay().get(), overlap.getEnd().minuteOfHour().get())) + ":" + (getEndTimeMin(overlap.getEnd().hourOfDay().get(), overlap.getEnd().minuteOfHour().get()) == 0 ? "00" : overlap.getEnd().minuteOfHour().get()) + ") ");
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("The current settings cannot be overridden because the following duration of the schedules are overlapping \n" + overlapDays.toString())
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_dialog_alert)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (removeEntry != null)
                                schedule.getDays().add(position, removeEntry);
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
            return false;

        }


        HashMap<String, ArrayList<Interval>> spillsMap =
                zoneScheduleViewModel.getScheduleSpills(daysArrayList, schedule);

        if (spillsMap != null && spillsMap.size() > 0) {
            if (schedule.isZoneSchedule()) {
                StringBuilder spillZones = new StringBuilder();
                for (String zone : spillsMap.keySet()) {
                    for (Interval i : spillsMap.get(zone)) {
                        spillZones.append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek())).append(" (").append(i.getStart().hourOfDay().get()).append(":").append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()).append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get())).append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Zone Schedule is outside building occupancy currently set. " +
                        "Proceed with trimming the zone schedules to be within the building occupancy \n" + spillZones)
                        .setCancelable(false)
                        .setTitle("Schedule Errors")
                        .setIcon(R.drawable.ic_dialog_alert)
                        .setNegativeButton("Re-Edit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showDialog(position, daysArrayList);
                            }
                        })
                        .setPositiveButton("Force-Trim", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                schedule.getDays().addAll(daysArrayList);
                                if (schedule.isZoneSchedule()) {
                                    ScheduleUtil.trimZoneSchedule(schedule, spillsMap);
                                } else {
                                    ScheduleUtil.trimZoneSchedules(spillsMap);
                                }
                                zoneScheduleViewModel.doScheduleUpdate(schedule);
                                updateUI();
                            }
                        });

                AlertDialog alert = builder.create();
                alert.show();
            }
            return true;
        }

        schedule.getDays().addAll(daysArrayList);
        zoneScheduleViewModel.doScheduleUpdate(schedule);
        updateUI();
        return true;
    }



    @Override
    public void onClickSaveSchedule(int unOccupiedZoneSetBackVal, Schedule schedule) {
        schedule.setUnoccupiedZoneSetback((double)unOccupiedZoneSetBackVal);
        CCUHsApi.getInstance().updateZoneSchedule(schedule, schedule.getRoomRef());
        Occupied occ = schedule.getCurrentValues();
        occ.setUnoccupiedZoneSetback(schedule.getUnoccupiedZoneSetback());
    }

    @Override
    public void onClickCancelSaveSchedule(String scheduleId) {
        RangeBar.setUnOccupiedFragment(true);
    }


    private String getDayString(DateTime d) {
        return ScheduleUtil.getDayString(d.getDayOfWeek());
    }
    private static float roundToHalf(float d) {
        return Math.round(d * 2) / 2.0f;
    }

    private void drawSchedule(int position, double heatingTemp, double coolingTemp, int startTimeHH,
                              int endTimeHH, int startTimeMM, int endTimeMM, DAYS day,
                              boolean intersection, boolean isOccupied) {

        String unit = "\u00B0F";
        if(isCelsiusTunerAvailableStatus()) {
            coolingTemp = roundToHalf((float) fahrenheitToCelsius(coolingTemp));
            heatingTemp = roundToHalf((float) fahrenheitToCelsius(heatingTemp));
            unit = "\u00B0C";
        }

        String strMinTemp = FontManager.getColoredSpanned(Double.toString(coolingTemp) + unit, colorMinTemp);
        String strMaxTemp = FontManager.getColoredSpanned(Double.toString(heatingTemp) + unit, colorMaxTemp);

        Typeface typeface=Typeface.DEFAULT;
        try {
            typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/lato_regular.ttf");
        }catch (Exception e){
            e.printStackTrace();
        }

        if (startTimeHH > endTimeHH || (startTimeHH == endTimeHH && startTimeMM > endTimeMM)) {
            drawScheduleBlock(position, strMinTemp, strMaxTemp, typeface, startTimeHH,
                    24, startTimeMM, 0,
                    getTextViewFromDay(day), false, true, intersection, isOccupied);
            drawScheduleBlock(position, strMinTemp, strMaxTemp, typeface, 0,
                    endTimeHH, 0, endTimeMM,
                    getTextViewFromDay(day.getNextDay()),
                    true, false, intersection, isOccupied);
        } else {
            drawScheduleBlock(position, strMinTemp, strMaxTemp,
                    typeface, startTimeHH, endTimeHH, startTimeMM,
                    endTimeMM, getTextViewFromDay(day),
                    false, false, intersection, isOccupied);
        }
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


    private void drawCurrentTime() {

        DateTime now = new DateTime(MockTime.getInstance().getMockTime());
        DAYS day = DAYS.values()[now.getDayOfWeek() - 1];
        CcuLog.d("Scheduler", "DAY: " + day.toString());
        int hh = now.getHourOfDay();
        int mm = now.getMinuteOfHour();
        AppCompatImageView imageView = new AppCompatImageView(getActivity());
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
                                   boolean leftBreak, boolean rightBreak, boolean intersection, boolean isOccupied) {

        CcuLog.d(L.TAG_CCU_UI, "position: "+position+" tempStartTime: " + tempStartTime + " tempEndTime: " + tempEndTime + " startTimeMM: " + startTimeMM + " endTimeMM " + endTimeMM+"isOccupied "+isOccupied);

        if(getContext() == null) return;
        AppCompatTextView textViewTemp = new AppCompatTextView(getContext());
        textViewTemp.setGravity(Gravity.CENTER_HORIZONTAL);
        if(isOccupied) {
            textViewTemp.setText(Html.fromHtml(strminTemp + " " + strmaxTemp));
        }
        if(typeface != null)
            textViewTemp.setTypeface(typeface);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(textViewTemp, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        textViewTemp.setMaxLines(2);
        textViewTemp.setContentDescription(textView.getText().toString()+"_"+tempStartTime+":"+startTimeMM+"-"+tempEndTime+":"+endTimeMM);
        textViewTemp.setId(ViewCompat.generateViewId());
        textViewTemp.setTag(position);

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int) mPixelsBetweenADay);
        lp.baselineToBaseline = textView.getId();


        int leftMargin = startTimeMM > 0 ? (int) ((startTimeMM / 60.0) * mPixelsBetweenAnHour) : lp.leftMargin;
        int rightMargin = endTimeMM > 0 ? (int) (((60 - endTimeMM) / 60.0) * mPixelsBetweenAnHour) : lp.rightMargin;

        lp.leftMargin = leftMargin;
        lp.rightMargin = rightMargin;

        Drawable drawableCompat = null;

        if (leftBreak) {
            drawableCompat = getResources().getDrawable(R.drawable.occupancy_background);
            if (intersection) {
                Drawable rightGreyBar = getResources().getDrawable(R.drawable.vline);
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(mDrawableBreakLineLeft, null, rightGreyBar, null);
            }else
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
            drawableCompat = getResources().getDrawable(R.drawable.occupancy_background);
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
                Drawable rightGreyBar = getResources().getDrawable(R.drawable.vline);
                textViewTemp.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        rightGreyBar, null);
            }
            drawableCompat = getResources().getDrawable(isOccupied ? R.drawable.occupancy_background :
                    R.drawable.occupancy_background_unoccupied, null);

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
                int clickedPosition = (int)v.getTag();
                ArrayList<Schedule.Days> days = schedule.getDays();
                try {
                    Collections.sort(days, (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
                    Collections.sort(days, (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
                    if(isOccupied) {
                        showDialog(ID_DIALOG_OCCUPIED_SCHEDULE, clickedPosition, schedule);
                    }else {
                        showDialog(ID_DIALOG_UN_OCCUPIED_SCHEDULE, clickedPosition, schedule);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    CcuLog.d(TAG, "onClick: " + e.getMessage());
                }
            }
        });
    }


    public void onClickCancel(String mScheduleId) {
        updateUI();
    }

    public void setOnExitListener(ZoneScheduleFragment.OnExitListener onExitListener) {
        this.mOnExitListener = onExitListener;
    }

    public interface OnExitListener {
        void onExit();
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(this::loadSchedule,1500);
        UpdateScheduleHandler.setBuildingScheduleListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        UpdateScheduleHandler.setBuildingScheduleListener(null);
    }
    public void refreshScreen() {
        if(getActivity() != null) {
            getActivity().runOnUiThread(this::loadSchedule);
        }
    }
}