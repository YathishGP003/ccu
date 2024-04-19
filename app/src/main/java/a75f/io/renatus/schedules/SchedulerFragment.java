package a75f.io.renatus.schedules;

import static a75f.io.api.haystack.util.TimeUtil.getEndTimeHr;
import static a75f.io.api.haystack.util.TimeUtil.getEndTimeMin;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
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
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;
import org.javolution.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projecthaystack.HDict;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.interfaces.BuildingScheduleListener;
import a75f.io.logic.schedule.SpecialSchedule;
import a75f.io.logic.util.OfflineModeUtilKt;
import a75f.io.messaging.handler.UpdateScheduleHandler;
import a75f.io.renatus.R;
import a75f.io.renatus.schedules.ManualSchedulerDialogFragment.ManualScheduleDialogListener;
import a75f.io.renatus.tuners.TunerFragment;
import a75f.io.renatus.util.FontManager;
import a75f.io.renatus.util.Marker;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.views.RangeBar;

public class SchedulerFragment extends DialogFragment implements ManualScheduleDialogListener, BuildingScheduleListener, NamedScheduleOccupiedDialogFragment.NamedScheduleOccupiedDialogFragmentListener
, UnOccupiedZoneSetBackDialogFragment.UnOccupiedZoneSetBackListener {

    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";
    private static final String PARAM_IS_VACATION = "PARAM_IS_VACATION";
    private static final String PARAM_IS_SPECIAL_SCHEDULE = "PARAM_IS_SPECIAL_SCHEDULE";
    private static final String PARAM_ROOM_REF = "PARAM_ROOM_REF";
    private static final int ID_DIALOG_VACATION = 02;
    private static final int ID_DIALOG_SCHEDULE = 01;

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
    TextView textViewVacations;
    Button textViewaddVacations;
    Button textViewAddSpecialSchedule;
    Schedule schedule;
    ConstraintLayout constraintScheduler;
    ArrayList<View> viewTimeLines;
    String mScheduleId;
    String colorMinTemp = "";
    String colorMaxTemp = "";
    RecyclerView mVacationRecycler;
    NestedScrollView scheduleScrollView;
    private OnExitListener mOnExitListener;
    private VacationAdapter mVacationAdapter;
    private RecyclerView specialScheduleRecycler;
    FrameLayout frameLayout;

    LinearLayout title_Layout;

    ZoneScheduleViewModel zoneScheduleViewModel;


    private Handler scheduleFragmentHandler;

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
            ScheduleManager.getInstance().setBuildingScheduleListener(this);
        } else {
            UpdateScheduleHandler.setBuildingScheduleListener(null);
            ScheduleManager.getInstance().setBuildingScheduleListener(null);
        }
    }

    private ConstraintLayout mVacationLayout;

    public SchedulerFragment() {

    }

    public static SchedulerFragment newInstance() {
        return new SchedulerFragment();
    }

    public static SchedulerFragment newInstance(String scheduleId) {
        SchedulerFragment schedulerFragment = new SchedulerFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, scheduleId);
        schedulerFragment.setArguments(args);
        return schedulerFragment;
    }

    public static SchedulerFragment newInstance(String scheduleId, boolean isVacation, String roomRef,
                                                boolean isSpecialSchedule) {
        SchedulerFragment schedulerFragment = new SchedulerFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, scheduleId);
        args.putBoolean(PARAM_IS_VACATION, isVacation);
        args.putBoolean(PARAM_IS_SPECIAL_SCHEDULE, isSpecialSchedule);
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
        View rootView = inflater.inflate(R.layout.fragment_scheduler, container, false);
        scheduleFragmentHandler = new Handler(Looper.getMainLooper());

        Typeface iconFont = FontManager.getTypeface(getActivity(), FontManager.FONTAWESOME);

        specialScheduleRecycler = rootView.findViewById(R.id.specialScheduleRecycler);

        //Scheduler Layout
        constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);
        mVacationRecycler = rootView.findViewById(R.id.vacationRecycler);
        textViewScheduletitle = rootView.findViewById(R.id.scheduleTitle);
        textViewaddEntry = rootView.findViewById(R.id.addEntry);
        mVacationLayout = rootView.findViewById(R.id.constraintLt_Vacations);
        scheduleScrollView = rootView.findViewById(R.id.scheduleScrollView);
        scheduleScrollView.post(() -> scheduleScrollView.smoothScrollTo(0,0));


        textViewVacations = rootView.findViewById(R.id.vacationsTitle);
        textViewaddVacations= rootView.findViewById(R.id.addVacations);
        textViewAddSpecialSchedule = rootView.findViewById(R.id.addSpecialSchedule);

        textViewaddVacations.setOnClickListener(v -> showVacationDialog());
        textViewAddSpecialSchedule.setOnClickListener( specialSchedule -> showSpecialScheduleDialog(null));
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
        frameLayout = rootView.findViewById(R.id.zoneScheduleFragmentContainer);
        title_Layout = rootView.findViewById(R.id.header_title);

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
                showDialogNamed();
            }
        });
        textViewScheduletitle.setFocusable(true);

        boolean isOffline =
                CCUHsApi.getInstance().readDefaultVal("offline and mode") > 0;

        if(isOffline) {
            if((getArguments() != null && getArguments().containsKey(PARAM_ROOM_REF))
                    && !((getArguments().getBoolean(PARAM_IS_SPECIAL_SCHEDULE)) ||
                    getArguments().getBoolean(PARAM_IS_VACATION))) {
                replaceLayout();
            }
        }else{
           replaceLayout();
        }

        return rootView;
    }

    private void replaceLayout(){
        constraintScheduler.setVisibility(View.GONE);
        Fragment childFragment;
        if (getArguments() != null && getArguments().containsKey(PARAM_ROOM_REF)) {
            childFragment = new ZoneScheduleFragment(getArguments().getString(PARAM_ROOM_REF));
        } else {
            childFragment = new ZoneScheduleFragment();
        }
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.zoneScheduleFragmentContainer, childFragment).commit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Measure the amount of pixels between an hour after the constraintScheduler layout draws the bars for the first time.
        //After they are measured d the schedule.
        ViewTreeObserver vto = constraintScheduler.getViewTreeObserver();
        WeakReference<ConstraintLayout> constraintLayoutWeakReference = new WeakReference<>(constraintScheduler);
        WeakReference<ArrayList<View>> viewTimeLinesWeakReference = new WeakReference<>(viewTimeLines);

        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ConstraintLayout constraintLayout = constraintLayoutWeakReference.get();
                if(constraintLayout != null){
                    constraintLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                    mPixelsBetweenADay = (float) constraintLayoutWeakReference.get().getHeight() / 7;
                }
                ArrayList<View> listOfView = viewTimeLinesWeakReference.get();
                if(listOfView != null){
                    View viewHourOne = listOfView.get(1);
                    View viewHourTwo = listOfView.get(2);
                    mPixelsBetweenAnHour = viewHourTwo.getX() - viewHourOne.getX();
                }
                //Leave 20% for padding.
                mPixelsBetweenADay = mPixelsBetweenADay - (mPixelsBetweenADay * .2f);
                loadSchedule();
                drawCurrentTime();
                return true;
            }
        });
    }

    private void loadSchedule()
    {

        if (getArguments() != null && getArguments().containsKey(PARAM_SCHEDULE_ID)) {
            mScheduleId = getArguments().getString(PARAM_SCHEDULE_ID);
            schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId) == null ?
                    CCUHsApi.getInstance().getDefaultNamedSchedule() :
                    CCUHsApi.getInstance().getScheduleById(mScheduleId);
        } else {
            ArrayList<Schedule> buildingScheduleList = CCUHsApi.getInstance().getSystemSchedule(false);
            if(buildingScheduleList.size() > 0) {
                schedule = buildingScheduleList.get(0);
            }
            Log.d("CCU_UI"," Loaded System Schedule "+schedule.toString());
        }

        if((getArguments() != null && getArguments().containsKey(PARAM_ROOM_REF))
        && !((getArguments().getBoolean(PARAM_IS_SPECIAL_SCHEDULE)) ||
                getArguments().getBoolean(PARAM_IS_VACATION)))
        {
            textViewScheduletitle.setText("Zone Schedule");
            constraintScheduler.setVisibility(View.GONE);
            updateUI();
        }else {
            boolean isOffline =
                    CCUHsApi.getInstance().readDefaultVal("offline and mode") > 0;
            if(isOffline && getContext()!= null){
                // read default named schedule
                //set  it to schedule
                HashMap<Object,Object> nm = CCUHsApi.getInstance().readEntity("named and default and schedule and organization");
                if(nm.isEmpty()){
                    disappearScheduleLayout();
                    Toast.makeText(getContext(), "Default Named Schedule not available", Toast.LENGTH_LONG).show();
                }else {
                    mScheduleId = nm.get("id").toString();
                    schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
                    textViewScheduletitle.setText("Default - " + CCUHsApi.getInstance().getSiteName());
                    textViewScheduletitle.setTypeface(Typeface.DEFAULT_BOLD);
                    textViewaddEntry.setVisibility(View.VISIBLE);
                    constraintScheduler.setVisibility(View.VISIBLE);
                    frameLayout.setVisibility(View.GONE);
                    updateUINamed();
                }
            }else {
                disappearScheduleLayout();
            }
        }
        if( getArguments() != null && (getArguments().getBoolean(PARAM_IS_VACATION) ||
                getArguments().getBoolean(PARAM_IS_SPECIAL_SCHEDULE))){
            disappearScheduleLayout();
        }
        loadVacations();
        loadSpecialSchedules();
    }

    private void disappearScheduleLayout(){
        title_Layout.setVisibility(View.GONE);
        frameLayout.setVisibility(View.GONE);
        textViewScheduletitle.setVisibility(View.GONE);
        constraintScheduler.setVisibility(View.GONE);
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

    private void showDialog(int id) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        switch (id) {
            case ID_DIALOG_SCHEDULE:
                Fragment prev = getFragmentManager().findFragmentByTag("popup");
                if (prev != null) {
                    ft.remove(prev);
                }
                ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this, schedule);
                newFragment.show(ft, "popup");
                break;

            case ID_DIALOG_VACATION:

                break;

        }
    }

    private void showVacationDialog()
    {
        showVacationDialog(null);
    }

    private void showSpecialScheduleDialog(String specialScheduleId){
        String roomRef = "";
        if (getArguments() != null && getArguments().containsKey(PARAM_ROOM_REF)) {
            roomRef = getArguments().getString(PARAM_ROOM_REF);
        }
        showSpecialScheduleDialog(roomRef, specialScheduleId);
    }

    private void showSpecialScheduleDialog(String roomRef, String specialScheduleId){
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment specialScheduleFragment = getChildFragmentManager().findFragmentByTag("popup");
        if(specialScheduleFragment != null){
            fragmentTransaction.remove(specialScheduleFragment);
        }
        SpecialScheduleDialogFragment  specialScheduleDialogFragment =
                new SpecialScheduleDialogFragment(specialScheduleId, roomRef,
                        (scheduleName, startDate, endDate, coolVal, heatVal,
                         coolingUserLimitMax,
                         coolingUserLimitMin,
                         heatingUserLimitMax,
                         heatingUserLimitMin,
                         coolingDeadband,
                         heatingDeadband) -> {
                    String warning = SpecialSchedule.validateSpecialSchedule(coolVal, heatVal,coolingUserLimitMax,
                            coolingUserLimitMin,
                            heatingUserLimitMax,
                            heatingUserLimitMin,
                            coolingDeadband,
                            heatingDeadband);
                    if(warning != null){
                        android.app.AlertDialog.Builder builder =
                                new android.app.AlertDialog.Builder(getActivity());
                        builder.setMessage(warning);
                        builder.setCancelable(false);
                        builder.setTitle(R.string.warning_ns);
                        builder.setIcon(R.drawable.ic_alert);
                        builder.setNegativeButton("OKAY", (dialog1, id) -> {
                            dialog1.dismiss();
                        });

                        AlertDialog alert = builder.create();
                        alert.show();

                        return;
                    }

                            ProgressDialogUtils.showProgressDialog(SchedulerFragment.this.getActivity(),
                                    "Adding Special Schedule...");
                            if (StringUtils.isEmpty(roomRef)) {
                                SpecialSchedule.createSpecialSchedule(specialScheduleId, scheduleName, startDate,
                                        endDate, coolVal, heatVal,coolingUserLimitMax,
                                        coolingUserLimitMin,
                                        heatingUserLimitMax,
                                        heatingUserLimitMin,
                                        coolingDeadband,
                                        heatingDeadband, false, null);
                            } else {
                                SpecialSchedule.createSpecialSchedule(specialScheduleId, scheduleName, startDate,
                                        endDate, coolVal, heatVal,
                                        coolingUserLimitMax,
                                        coolingUserLimitMin,
                                        heatingUserLimitMax,
                                        heatingUserLimitMin,
                                        coolingDeadband,
                                        heatingDeadband,true, roomRef);
                            }

                            CCUHsApi.getInstance().saveTagsData();
                            ScheduleManager.getInstance().updateSchedules();
                            CCUHsApi.getInstance().syncEntityTree();
                            SchedulerFragment.this.loadSpecialSchedules();
                            ProgressDialogUtils.hideProgressDialog();
                        });
        specialScheduleDialogFragment.show(fragmentTransaction, "popup");
    }

    private void showVacationDialog(String vacationId)
    {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Schedule vacationSchedule = CCUHsApi.getInstance().getScheduleById(vacationId);

        Fragment vacationFragment = getFragmentManager().findFragmentByTag("popup");
        if(vacationFragment != null)
        {
            ft.remove(vacationFragment);
        }

        ManualCalendarDialogFragment calendarDialogFragment = new ManualCalendarDialogFragment(vacationSchedule != null ? vacationSchedule.getId() : null,
                vacationSchedule != null ? vacationSchedule.getDis() : null,
                vacationSchedule != null ? vacationSchedule.getStartDate() : null, vacationSchedule != null ? vacationSchedule.getEndDate() : null, getArguments() != null ? getArguments().getString(PARAM_ROOM_REF) : null,
                new ManualCalendarDialogFragment.ManualCalendarDialogListener() {
            @Override
            public boolean onClickSave(String vacationId, String vacationName, DateTime startDate, DateTime endDate)
            {
                ProgressDialogUtils.showProgressDialog(getActivity(), "Adding vacation...");
                if (vacationSchedule != null && !TextUtils.isEmpty(vacationSchedule.getRoomRef()) && vacationSchedule.getRoomRef()!= null) {
                    DefaultSchedules.upsertZoneVacation(vacationId, vacationName, startDate, endDate, vacationSchedule.getRoomRef());
                } else if ((vacationSchedule == null && getArguments() != null && getArguments().containsKey(PARAM_ROOM_REF))){
                    DefaultSchedules.upsertZoneVacation(vacationId, vacationName, startDate, endDate, getArguments().getString(PARAM_ROOM_REF));
                }
                else
                {
                    DefaultSchedules.upsertVacation(vacationId, vacationName, startDate, endDate);
                }

                CCUHsApi.getInstance().saveTagsData();
                ScheduleManager.getInstance().updateSchedules();
                CCUHsApi.getInstance().syncEntityTree();
                Runnable runnable = () -> {
                    loadVacations();
                    ProgressDialogUtils.hideProgressDialog();
                };
                scheduleFragmentHandler.postDelayed(runnable, 3000);

                return false;
            }

            @Override
            public boolean onClickCancel(DialogFragment dialog) {
                return false;
            }
        });

        calendarDialogFragment.show(ft, "popup");

    }

    ImageButton.OnClickListener mEditOnClickListener = v -> {
        String id = v.getTag().toString();
        showVacationDialog(id);
    };

    ImageButton.OnClickListener mDeleteOnClickListener = v ->  {
            String id = v.getTag().toString();
            showDeleteVacationAlert(id);
    };

    private void showDeleteSpecialScheduleAlert(String scheduleId){
        HDict specialSchedule = CCUHsApi.getInstance().getScheduleDictById(scheduleId);
        final Dialog alertDialog = new Dialog(getActivity());
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCancelable(false);
        alertDialog.setContentView(R.layout.dialog_delete_schedule);
        TextView messageTv = alertDialog.findViewById(R.id.tvMessage);
        messageTv.setText("Are you sure you want to delete the Special Schedule: " + specialSchedule.get(Tags.DIS)+"?");
        alertDialog.findViewById(R.id.btnCancel).setOnClickListener(view -> alertDialog.dismiss());
        alertDialog.findViewById(R.id.btnProceed).setOnClickListener(view -> {
            ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting special Schedule...");

            CCUHsApi.getInstance().deleteEntity(scheduleId);
            ScheduleManager.getInstance().updateSchedules();
            CCUHsApi.getInstance().syncEntityTree();
            alertDialog.dismiss();
            SchedulerFragment.this.loadSpecialSchedules();
            ProgressDialogUtils.hideProgressDialog();
        });
        alertDialog.show();
    }
    private void showDeleteVacationAlert(String vacationId) {
        Schedule vacationSchedule = CCUHsApi.getInstance().getScheduleById(vacationId);
        final Dialog alertDialog = new Dialog(getActivity());
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCancelable(false);
        alertDialog.setContentView(R.layout.dialog_delete_schedule);
        TextView messageTv = alertDialog.findViewById(R.id.tvMessage);
        messageTv.setText("Are you sure you want to delete the vacation: " + vacationSchedule.getDis()+"?");
        alertDialog.findViewById(R.id.btnCancel).setOnClickListener(view -> alertDialog.dismiss());
        alertDialog.findViewById(R.id.btnProceed).setOnClickListener(view -> {
            ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting vacation...");

            CCUHsApi.getInstance().deleteEntity("@"+vacationId);
            ScheduleManager.getInstance().updateSchedules();
            CCUHsApi.getInstance().syncEntityTree();
            alertDialog.dismiss();

            Runnable runnable = () -> {
                loadVacations();
                ProgressDialogUtils.hideProgressDialog();
            };
            scheduleFragmentHandler.postDelayed(runnable, 3000);
        });

        alertDialog.show();
    }
    private List<HashMap<Object, Object>> getSpecialSchedule(){
        List<HashMap<Object, Object>> specialScheduleList;
        if (getArguments() != null && getArguments().containsKey(PARAM_ROOM_REF)) {
            String roomRef = getArguments().getString(PARAM_ROOM_REF);
            specialScheduleList = CCUHsApi.getInstance().getSpecialSchedules(roomRef);
        }
        else{
            specialScheduleList = CCUHsApi.getInstance().getSpecialSchedules(null);
        }
        if(specialScheduleList != null){
            Collections.sort(specialScheduleList, (lhss, rhss) -> {
                HDict lhrange = (HDict) lhss.get(Tags.RANGE);
                HDict rhrange = (HDict) rhss.get(Tags.RANGE);
                String lhStartDate = lhrange.get(Tags.STDT).toString();
                String rhStartDate = rhrange.get(Tags.STDT).toString();
                if(lhStartDate.equals(rhStartDate)){
                    int lhStartHour = SpecialSchedule.getInt(lhrange.get(Tags.STHH).toString());
                    int rhStartHour = SpecialSchedule.getInt(rhrange.get(Tags.STHH).toString());
                    if(lhStartHour == rhStartHour){
                        return SpecialSchedule.getInt(lhrange.get(Tags.STMM).toString()) -
                                SpecialSchedule.getInt((rhrange.get(Tags.STMM).toString()));
                    }
                    return lhStartHour - rhStartHour;
                }
                return lhrange.get(Tags.STDT).toString().compareTo(rhrange.get(Tags.STDT).toString());
            });
        }
        return specialScheduleList;
    }

    private void setSpecialScheduleAdapter(List<HashMap<Object, Object>> specialScheduleList){
        if(specialScheduleList != null){
            SpecialScheduleAdapter specialScheduleAdapter = new SpecialScheduleAdapter(specialScheduleList,
                    view -> {
                        String id = view.getTag().toString();
                        showDeleteSpecialScheduleAlert(id);
                    },
                    view -> {
                        String id = view.getTag().toString();
                        showSpecialScheduleDialog(id);
                    });
            specialScheduleRecycler.setAdapter(specialScheduleAdapter);
            specialScheduleRecycler.setLayoutManager(new LinearLayoutManager(this.getContext()));
        }
    }

    private void loadSpecialSchedules(){
        final List<HashMap<Object, Object>>[] specialScheduleList = new List[]{null};
        RxjavaUtil.executeBackgroundTask(() -> specialScheduleList[0] = SchedulerFragment.this.getSpecialSchedule(),
                ()-> setSpecialScheduleAdapter(specialScheduleList[0]));


    }
    private void loadVacations() {
        
        ArrayList<Schedule> vacations;
        if (schedule.isZoneSchedule() && schedule.getRoomRef()!= null) {
            vacations = CCUHsApi.getInstance().getZoneSchedule(schedule.getRoomRef(), true);
            textViewVacations.setText("Zone Vacations");
        } else if (getArguments() != null && getArguments().containsKey(PARAM_ROOM_REF)){
            String roomRef = getArguments().getString(PARAM_ROOM_REF);
            vacations = CCUHsApi.getInstance().getZoneSchedule(roomRef, true);
            textViewVacations.setText("Zone Vacations");
        } else
        {
            vacations = CCUHsApi.getInstance().getSystemSchedule(true);
            textViewVacations.setText("Vacations");
        }
        
        if(vacations != null) {
            Collections.sort(vacations, (lhs, rhs) -> lhs.getStartDate().compareTo(rhs.getStartDate()));
            Collections.sort(vacations, (lhs, rhs) -> lhs.getEndDate().compareTo(rhs.getEndDate()));
            mVacationAdapter = new VacationAdapter(vacations, mEditOnClickListener, mDeleteOnClickListener);
            mVacationRecycler.setAdapter(mVacationAdapter);
            mVacationRecycler.setLayoutManager(new LinearLayoutManager(this.getContext()));
        }
    }

    private void showDialog(int id, int position, Schedule.Days day) {

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        schedule = CCUHsApi.getInstance().getScheduleById(schedule.getId()) == null ?
                CCUHsApi.getInstance().getDefaultNamedSchedule() : CCUHsApi.getInstance().getScheduleById(schedule.getId());
        switch (id) {
            case ID_DIALOG_SCHEDULE:
                Fragment scheduleFragment = getFragmentManager().findFragmentByTag("popup");
                if (scheduleFragment != null) {
                    ft.remove(scheduleFragment);
                }
                ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this, position, day,schedule);
                newFragment.show(ft, "popup");
                break;
        }
    }
    
    private void showDialog(int id, int position, ArrayList<Schedule.Days> days) {

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        
        switch (id) {
            case ID_DIALOG_SCHEDULE:
                Fragment scheduleFragment = getFragmentManager().findFragmentByTag("popup");
                if (scheduleFragment != null) {
                    ft.remove(scheduleFragment);
                }
                ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this, position, days,schedule);
                newFragment.show(ft, "popup");
                break;
        }
    }
    Schedule.Days removeEntry = null;
    public boolean onClickSave(int position, double coolingTemp, double heatingTemp, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute, ArrayList<DAYS> days) {
        
        if (position != ManualSchedulerDialogFragment.NO_REPLACE) {
            //sort schedule days according to the start hour of the day
            try {
                Collections.sort(schedule.getDays(), (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
                Collections.sort(schedule.getDays(), (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
                removeEntry = schedule.getDays().remove(position);
            }catch (ArrayIndexOutOfBoundsException e) {
                Log.d(TAG, "onClickSave: " + e.getMessage());
            }
        } else {
            removeEntry = null;
        }

        Log.d("CCU_UI"," onClickSave "+"startTime "+startTimeHour+":"+startTimeMinute+" endTime "+endTimeHour+":"+endTimeMinute+" removeEntry "+removeEntry);

        ArrayList<Schedule.Days> daysArrayList = new ArrayList<Schedule.Days>();

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
                dayBO.setDay(day.ordinal());
                daysArrayList.add(dayBO);
                }
        }
    
        for (Schedule.Days d : daysArrayList) {
            Log.d("CCU_UI", " daysArrayList  "+d);
        }

        boolean intersection = schedule.checkIntersection(daysArrayList);
        if (intersection) {
            
            StringBuilder overlapDays = new StringBuilder();
            for (Schedule.Days day : daysArrayList) {
                ArrayList<Interval> overlaps = schedule.getOverLapInterval(day);
                for (Interval overlap : overlaps) {
                    Log.d("CCU_UI"," overLap "+overlap);
                    overlapDays.append(getDayString(overlap.getStart())+"("+overlap.getStart().hourOfDay().get()+":"+(overlap.getStart().minuteOfHour().get() == 0 ? "00" : overlap.getStart().minuteOfHour().get())
                                       +" - " +(getEndTimeHr(overlap.getEnd().hourOfDay().get(), overlap.getEnd().minuteOfHour().get()))+":"+(getEndTimeMin(overlap.getEnd().hourOfDay().get(), overlap.getEnd().minuteOfHour().get())  == 0 ? "00": overlap.getEnd().minuteOfHour().get())+ ") ");
                }
            }
        
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("The current settings cannot be overridden because the following duration of the schedules are overlapping \n"+overlapDays.toString())
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
        
        HashMap<String, ArrayList<Interval>> spillsMap = days == null ? getRemoveScheduleSpills(removeEntry):
                                                                              getScheduleSpills(daysArrayList);
        if (spillsMap != null && spillsMap.size() > 0) {
            if (schedule.isZoneSchedule()) {
                StringBuilder spillZones = new StringBuilder();
                for (String zone : spillsMap.keySet())
                {
                    for (Interval i : spillsMap.get(zone))
                    {
                        spillZones.append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek())+" (" + i.getStart().hourOfDay().get() + ":" + (i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()) + " - " + getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) + ":" + (getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()) + ") \n");
                    }
                }
                
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Zone Schedule is outside building occupancy currently set. " +
                                   "Proceed with trimming the zone schedules to be within the building occupancy \n"+spillZones)
                       .setCancelable(false)
                       .setTitle("Schedule Errors")
                       .setIcon(R.drawable.ic_dialog_alert)
                       .setNegativeButton("Re-Edit", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               if (removeEntry != null) {
                                   showDialog(ID_DIALOG_SCHEDULE, position, removeEntry);
                               } else {
                                   showDialog(ID_DIALOG_SCHEDULE, position, daysArrayList);
                               }
                           }
                       })
                       .setPositiveButton("Force-Trim", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               schedule.getDays().addAll(daysArrayList);
                               if (schedule.isZoneSchedule()) {
                                   ScheduleUtil.trimZoneSchedule(schedule, spillsMap);
                               } else{
                                   ScheduleUtil.trimZoneSchedules(spillsMap);
                               }
                               schedule = CCUHsApi.getInstance().getScheduleById(schedule.getId());
                               doScheduleUpdate();
                           }
                       });
    
                AlertDialog alert = builder.create();
                alert.show();
            } else if (schedule.isBuildingSchedule()) {
                StringBuilder spillZones = new StringBuilder();
                StringBuilder spillNamedZones = new StringBuilder();
                ArrayList<String> namedheaders = new ArrayList<>();
                ArrayList<String> zoneheaders = new ArrayList<>();
                String schedules = "";
                for (String zone : spillsMap.keySet())
                {
                    for (Interval i : spillsMap.get(zone))
                    {
                        Zone z = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(zone)).build();
                        Floor f = new Floor.Builder().setHashMap(CCUHsApi.getInstance().readMapById(z.getFloorRef())).build();
                        if((CCUHsApi.getInstance().getScheduleById(z.getScheduleRef())).isNamedSchedule()){
                            schedules = schedules.concat("named");
                            if (!namedheaders.contains(f.getDisplayName())) {
                                spillNamedZones.append("\t").append(f.getDisplayName()).append("->\n");
                                namedheaders.add(f.getDisplayName());
                            }
                            spillNamedZones.append("\t\t\tZone ").append(z.getDisplayName()).append(" ").append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek())).append(" (").append(i.getStart().hourOfDay().get()).append(":").append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()).append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get())).append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
                        }else {
                            schedules = schedules.concat(Tags.ZONE);
                            if (!zoneheaders.contains(f.getDisplayName())) {
                                spillZones.append("\t").append(f.getDisplayName()).append("->\n");
                                zoneheaders.add(f.getDisplayName());
                            }
                            spillZones.append("\t\t\tZone ").append(z.getDisplayName()).append(" ").append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek())).append(" (").append(i.getStart().hourOfDay().get()).append(":").append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()).append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get())).append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
                        }
                    }
                }

                String namedSchedulesWarning = "" ;
                String zoneSchedulesWarning = "" ;
                if (schedules.contains("named")) {
                    namedSchedulesWarning = "Named Schedule for below zone(s) is outside updated " +
                            "building occupancy.\n"
                            + ((spillNamedZones.toString()).equals("") ? "" : "\tThe Schedule is " +
                            "outside by \n\t" + spillNamedZones.toString()+"\n");
                    if(schedules.contains("zone")){
                        zoneSchedulesWarning = "Zone Schedule for below zone(s) is outside updated " +
                                "building occupancy.\n" + (spillZones.toString().equals("") ? "" : "\tThe Schedule " +
                                "is outside by \n\t" + spillZones.toString());
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(namedSchedulesWarning + zoneSchedulesWarning)
                            .setCancelable(false)
                            .setTitle("Schedule Errors")
                            .setIcon(R.drawable.ic_dialog_alert)
                            .setNegativeButton("Re-Edit", (dialog, id) -> {
                                if (removeEntry != null) {
                                    showDialog(ID_DIALOG_SCHEDULE, position, removeEntry);
                                } else {
                                    showDialog(ID_DIALOG_SCHEDULE, position, daysArrayList);
                                }
                            });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else if(schedules.contains("zone")){
                    zoneSchedulesWarning = "Zone Schedule for below zone(s) is outside updated " +
                            "building occupancy.\n" + (spillZones.toString().equals("") ? "" : "\tThe Schedule " +
                            "is outside by \n\t" + spillZones.toString());
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(zoneSchedulesWarning)
                            .setCancelable(false)
                            .setTitle("Schedule Errors")
                            .setIcon(R.drawable.ic_dialog_alert)
                            .setNegativeButton("Re-Edit", (dialog, id) -> {
                                if (removeEntry != null) {
                                    showDialog(ID_DIALOG_SCHEDULE, position, removeEntry);
                                } else {
                                    showDialog(ID_DIALOG_SCHEDULE, position, daysArrayList);
                                }
                            })
                            .setPositiveButton("Force-Trim", (dialog, id) -> {
                                schedule.getDays().addAll(daysArrayList);
                                ScheduleUtil.trimZoneSchedules(spillsMap);
                                if (schedule.getDays().contains(removeEntry)) {
                                    schedule.getDays().remove(position);
                                }
                                doScheduleUpdate();
                            });

                    AlertDialog alert = builder.create();
                    alert.show();
                }


            }
            return true;
            
        }

        schedule.getDays().addAll(daysArrayList);
        doScheduleUpdate();
        return true;
    }
    
    private void doScheduleUpdate() {
        if (schedule.isZoneSchedule())
        {
            CCUHsApi.getInstance().updateZoneSchedule(schedule, schedule.getRoomRef());
        } else
        {
            CCUHsApi.getInstance().updateSchedule(schedule);
        }
        CCUHsApi.getInstance().syncEntityTree();
        updateUI();
        ScheduleManager.getInstance().updateSchedules();
    }
    
    private HashMap<String,ArrayList<Interval>> getRemoveScheduleSpills(Schedule.Days d) {
        if (!schedule.isBuildingSchedule()) {
            return null;
        }
        return getScheduleSpills(null);
    }
    
    private HashMap<String,ArrayList<Interval>> getScheduleSpills(ArrayList<Schedule.Days> daysArrayList) {

        LinkedHashMap<String,ArrayList<Interval>> spillsMap = new LinkedHashMap<>();
        if (schedule.isZoneSchedule()) {
            Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);

            ArrayList<Interval> intervalSpills = new ArrayList<>();
            ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals(daysArrayList);

            for (Interval v : systemIntervals)
            {
                CcuLog.d(L.TAG_CCU_UI,"Merged System interval " + v);
            }

            ArrayList<Interval> zoneIntervals = schedule.getScheduledIntervals(daysArrayList);

            for (Interval v : zoneIntervals)
            {
                CcuLog.d(L.TAG_CCU_UI, "Zone interval "+v);
            }

            for(Interval z : zoneIntervals) {
                boolean add = true;
               for (Interval s: systemIntervals) {
                    if (s.contains(z)) {
                        add = false;
                        break;
                    } else if (s.overlaps(z)) {
                        /*if(z.getStartMillis() < s.getStartMillis() && z.getEndMillis() > s.getEndMillis()){
                            intervalSpills.add(new Interval(z.getStartMillis(), s.getStartMillis()));
                            intervalSpills.add(new Interval(s.getEndMillis(), z.getEndMillis()));
                        } else if (z.getStartMillis() < s.getStartMillis()) {
                            intervalSpills.add(new Interval(z.getStartMillis(), s.getStartMillis()));
                        } else if (z.getEndMillis() > s.getEndMillis()) {
                            intervalSpills.add(new Interval(s.getEndMillis(), z.getEndMillis()));
                        }*/
                        add = false;
                        for (Interval i: disconnectedIntervals(systemIntervals,z)){
                            if (!intervalSpills.contains(i)){
                                intervalSpills.add(i);
                            }
                        }

                    }
                }
                if (add)
                {
                    intervalSpills.add(z);
                    CcuLog.d(L.TAG_CCU_UI, " Zone Interval not contained "+z);
                }
            }
            if (intervalSpills.size() > 0)
            {
                spillsMap.put(schedule.getRoomRef(), intervalSpills);
            }

        } else if (schedule.isBuildingSchedule()) {
            ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("room");
            Collections.sort(zones, (lhs, rhs) -> lhs.get("floorRef").toString().compareTo(rhs.get("floorRef").toString()));
            for (HashMap m : zones) {
                ArrayList<Interval> intervalSpills = new ArrayList<>();
                if(m.containsKey("scheduleRef")) {
                    Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(m.get("scheduleRef").toString());
                    CcuLog.d(L.TAG_CCU_UI, "Zone " + m + " " + zoneSchedule.toString());
                    if (zoneSchedule.getMarkers().contains("disabled")) {
                        continue;
                    }

                    ArrayList<Interval> zoneIntervals = zoneSchedule.getScheduledIntervals();

                    for (Interval v : zoneIntervals) {
                        CcuLog.d(L.TAG_CCU_UI, "Zone interval " + v);
                    }

                    ArrayList<Interval> systemIntervals = schedule.getMergedIntervals();
                    if (daysArrayList != null) {
                        systemIntervals.addAll(schedule.getScheduledIntervals(daysArrayList));
                    }
                    ArrayList<Interval> splitSchedules = new ArrayList<>();
                    for (Interval v : systemIntervals) {
                        if (v.getStart().getDayOfWeek() == 7 && v.getEnd().getDayOfWeek() == 1) {
                            long now = MockTime.getInstance().getMockTime();
                            DateTime startTime = new DateTime(now)
                                    .withHourOfDay(0)
                                    .withMinuteOfHour(0)
                                    .withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(1);

                            DateTime endTime = new DateTime(now).withHourOfDay(v.getEnd().getHourOfDay())
                                    .withMinuteOfHour(v.getEnd().getMinuteOfHour())
                                    .withSecondOfMinute(v.getEnd().getSecondOfMinute())
                                    .withMillisOfSecond(v.getEnd().getMillisOfSecond()).withDayOfWeek(1);
                            splitSchedules.add(new Interval(startTime, endTime));
                        }
                    }
                    systemIntervals.addAll(splitSchedules);
                    for (Interval v : systemIntervals) {
                        CcuLog.d(L.TAG_CCU_UI, "Merged System interval " + v);
                    }

                    for (Interval z : zoneIntervals) {
                        boolean contains = false;
                        for (Interval s : systemIntervals) {
                            if (s.contains(z)) {
                                contains = true;
                                break;
                            }
                        }

                        if (!contains) {
                            for (Interval s : systemIntervals) {
                                if (s.overlaps(z)) {
                                    /*if(z.getStartMillis() < s.getStartMillis() && z.getEndMillis() > s.getEndMillis()){
                                        intervalSpills.add(new Interval(z.getStartMillis(), s.getStartMillis()));
                                        intervalSpills.add(new Interval(s.getEndMillis(), z.getEndMillis()));
                                    } else if (z.getStartMillis() < s.getStartMillis()) {
                                        intervalSpills.add(new Interval(z.getStartMillis(), s.getStartMillis()));
                                    } else if (z.getEndMillis() > s.getEndMillis()) {
                                        intervalSpills.add(new Interval(s.getEndMillis(), z.getEndMillis()));
                                    }*/
                                    for (Interval i: disconnectedIntervals(systemIntervals,z)){
                                        if (!intervalSpills.contains(i)){
                                            intervalSpills.add(i);
                                        }
                                    }
                                    contains = true;
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            intervalSpills.add(z);
                            CcuLog.d(L.TAG_CCU_UI, " Zone Interval not contained " + z);
                        }

                    }

                    if (intervalSpills.size() > 0) {
                        spillsMap.put(m.get("id").toString(), intervalSpills);
                    }
                }
            }
        }
        return spillsMap;
    }

    public List<Interval> disconnectedIntervals(List<Interval> intervals, Interval r) {
        List<Interval> result = new ArrayList<>();
        ArrayList<Marker> markers = new ArrayList<>();

        for (Interval i : intervals) {
            markers.add(new Marker(i.getStartMillis(), true));
            markers.add(new Marker(i.getEndMillis(), false));
        }

        Collections.sort(markers, (a, b) -> Long.compare(a.val, b.val));

        int overlap = 0;
        boolean endReached = false;

        if (markers.size() > 0 && markers.get(0).val > r.getStartMillis()) {
            result.add(new Interval(r.getStartMillis(), markers.get(0).val));
        }

        for (int i = 0; i < markers.size() - 1; i++) {
            Marker m = markers.get(i);

            overlap += m.start ? 1 : -1;
            Marker next = markers.get(i + 1);

            if (m.val != next.val && overlap == 0 && next.val > r.getStartMillis()) {
                long start = m.val > r.getStartMillis() ? m.val : r.getStartMillis();
                long end = next.val;
                if (next.val > r.getEndMillis()) {
                    end = r.getEndMillis();
                    endReached = true;
                }
                if (end > start) {
                    result.add(new Interval(start, end));
                }
                if (endReached)
                    break;
            }
        }

        if (!endReached) {
            Marker m = markers.get(markers.size() - 1);
            if (r.getEndMillis() > m.val) {
                result.add(new Interval(m.val, r.getEndMillis()));
            }
        }

        return result;
    }
    
    private String getDayString(DateTime d) {
        return ScheduleUtil.getDayString(d.getDayOfWeek());
    }
    private String getDayString(Schedule.Days day) {
        return ScheduleUtil.getDayString(day.getDay()+1);
    }
    private static float roundToHalf(float d) {
        return Math.round(d * 2) / 2.0f;
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

            case SUNDAY:
                return textViewSunday;

            default:
                return textViewSunday;
        }
    }


    private void drawCurrentTime() {
        try {
            DateTime now = new DateTime(MockTime.getInstance().getMockTime());


            DAYS day = DAYS.values()[now.getDayOfWeek() - 1];
            Log.i("Scheduler", "DAY: " + day.toString());
            int hh = now.getHourOfDay();
            int mm = now.getMinuteOfHour();


            AppCompatImageView imageView = new AppCompatImageView(requireContext());

            imageView.setImageResource(R.drawable.ic_time_marker_svg);
            imageView.setId(View.generateViewId());
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            //imageView.setPadding(0, 50,0, 0);
            //imageView.setForegroundGravity(Gravity.CENTER);
            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int) mPixelsBetweenADay);
            //lp.topMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
            //lp.bottomMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
            lp.bottomToBottom = getTextViewFromDay(day).getId();
            lp.topToTop = getTextViewFromDay(day).getId();
            lp.startToStart = viewTimeLines.get(hh).getId();

            lp.leftMargin = (int) ((mm / 60.0) * mPixelsBetweenAnHour);

            constraintScheduler.addView(imageView, lp);
        }catch(IllegalStateException exception){
            // if context is null we will get this exception, some rare scenario we get this.
            Log.e(L.TAG_CCU_UI,exception.getMessage());
        }

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
                if(schedule.isNamedSchedule() && !OfflineModeUtilKt.isOfflineMode()){
                    Toast.makeText(getContext(), R.string.taost_for_fallback, Toast.LENGTH_SHORT).show();
                    return;
                }

                int clickedPosition = (int)v.getTag();
                HashMap<Object,Object> scheduleObject = CCUHsApi.getInstance().readEntity("named and default and schedule and organization");
                mScheduleId = scheduleObject.get("id").toString();
                schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
                ArrayList<Schedule.Days> days = schedule.getDays();
                try {
                    Collections.sort(days, (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
                    Collections.sort(days, (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
                    if(isOccupied) {
                        showDialogNamed(0, clickedPosition, schedule);
                    }else {
                        showDialogNamed(1, clickedPosition, schedule);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    CcuLog.d(TAG, "onClick: " + e.getMessage());
                }
            }
        });

    }

    @Override
    public boolean onClickSaveNamed(int position, double coolingTemp, double heatingTemp, int startTimeHour, int endTimeHour,
                                    int startTimeMinute, int endTimeMinute, ArrayList<DAYS> days, Double heatingUserLimitMaxVal,
                                    Double heatingUserLimitMinVal, Double coolingUserLimitMaxVal, Double coolingUserLimitMinVal,
                                    Double heatingDeadBandVal, Double coolingDeadBandVal, Schedule.Days mDay) {
        if (position != NamedScheduleOccupiedDialogFragment.NO_REPLACE) {
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
            if (schedule.isNamedSchedule()) {
                StringBuilder spillZones = new StringBuilder();
                for (String zone : spillsMap.keySet()) {
                    for (Interval i : spillsMap.get(zone)) {
                        spillZones.append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek())).append(" (").append(i.getStart().hourOfDay().get()).append(":").append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()).append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get())).append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Named Schedule is outside building occupancy currently set. " +
                                "Proceed with trimming the zone schedules to be within the building occupancy \n" + spillZones)
                        .setCancelable(false)
                        .setTitle("Schedule Errors")
                        .setIcon(R.drawable.ic_dialog_alert)
                        .setNegativeButton("Re-Edit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showDialogNamed(position, daysArrayList);
                            }
                        })
                        .setPositiveButton("Force-Trim", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                schedule.getDays().addAll(daysArrayList);
                                ScheduleUtil.trimZoneSchedule(schedule, spillsMap);
                                zoneScheduleViewModel.doScheduleUpdate(schedule);
                                updateUINamed();
                            }
                        });

                AlertDialog alert = builder.create();
                alert.show();
            }
            return true;
        }

        schedule.getDays().addAll(daysArrayList);
        zoneScheduleViewModel.doScheduleUpdate(schedule);
        updateUINamed();
        return true;
    }

    public boolean onClickCancel(String mScheduleId) {
        schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
        updateUI();
        return true;
    }

    public void onClickCancelNamed(String mScheduleId) {
        schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
        updateUINamed();

    }

    public void setOnExitListener(OnExitListener onExitListener) {
        this.mOnExitListener = onExitListener;

    }

    @Override
    public void onClickSaveSchedule(double unOccupiedZoneSetBackVal, Schedule schedule) {
        schedule.setUnoccupiedZoneSetback(unOccupiedZoneSetBackVal);
        CCUHsApi.getInstance().updateSchedule(schedule);
        Occupied occ = schedule.getCurrentValues();
        occ.setUnoccupiedZoneSetback(schedule.getUnoccupiedZoneSetback());
    }

    @Override
    public void onClickCancelSaveSchedule(String scheduleId) {
        RangeBar.setUnOccupiedFragment(true);
    }

    public interface OnExitListener {
        void onExit();
    }

    @Override
    public void onResume() {
        super.onResume();
        Runnable runnable = () -> loadSchedule();
        scheduleFragmentHandler.postDelayed(runnable, 1500);
        UpdateScheduleHandler.setBuildingScheduleListener(this);
        ScheduleManager.getInstance().setBuildingScheduleListener(this);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        UpdateScheduleHandler.setBuildingScheduleListener(null);
        ScheduleManager.getInstance().setBuildingScheduleListener(null);
    }
    public void refreshScreen() {
        if(getActivity() != null) {
            getActivity().runOnUiThread(() -> loadSchedule());
        }
    }




    private void showDialogNamed() {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment prev = getParentFragmentManager().findFragmentByTag("popup");
        if (prev != null) {
            ft.remove(prev);
        }
        NamedScheduleOccupiedDialogFragment newFragment = new NamedScheduleOccupiedDialogFragment(this, schedule);
        newFragment.show(ft, "popup");
    }

    private void showDialogNamed( Schedule.Days day) {

        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment scheduleFragment = getParentFragmentManager().findFragmentByTag("popup");
        if (scheduleFragment != null) {
            ft.remove(scheduleFragment);
        }
        NamedScheduleOccupiedDialogFragment newFragment = new NamedScheduleOccupiedDialogFragment(this, schedule, day);
        newFragment.show(ft, "popup");
    }

    private void showDialogNamed(int id, int position, Schedule schedule) {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Schedule.Days occupiedDays;
        UnOccupiedDays unOccupiedDays;

        switch (id) {
            case 0:
                occupiedDays = schedule.getDays().get(position);
                Fragment scheduleFragment = getParentFragmentManager().findFragmentByTag("popup");
                if (scheduleFragment != null) {
                    ft.remove(scheduleFragment);
                }
                NamedScheduleOccupiedDialogFragment newFragment = new NamedScheduleOccupiedDialogFragment(this, position, occupiedDays, schedule);
                newFragment.show(ft, "popup");
                break;

            case 1:
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
                if(schedule.getDays().size() == 0)
                    break;
                Schedule.Days nextDay = getNextOccupiedSlot(position, unOccupiedDays, occupiedSlotsSize);
                UnOccupiedZoneSetBackDialogFragment unOccupiedZoneSetBackDialogFragment = new UnOccupiedZoneSetBackDialogFragment(this, nextDay, schedule);
                unOccupiedZoneSetBackDialogFragment.show(ft, "popup");

        }
    }

    private void showDialogNamed(int position, ArrayList<Schedule.Days> days) {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment scheduleFragment = getParentFragmentManager().findFragmentByTag("popup");
        if (scheduleFragment != null) {
            ft.remove(scheduleFragment);
        }
        NamedScheduleOccupiedDialogFragment newFragment = new NamedScheduleOccupiedDialogFragment(this, position, days, schedule);
        newFragment.show(ft, "popup");
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

    private void updateUINamed() {
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

    @Override
    public void onDestroyView() {
        scheduleFragmentHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        scheduleFragmentHandler = null;
        super.onDestroy();
    }
}
