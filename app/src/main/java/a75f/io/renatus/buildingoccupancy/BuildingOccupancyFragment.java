package a75f.io.renatus.buildingoccupancy;


import static a75f.io.api.haystack.util.SchedulableMigrationKt.validateMigration;
import static a75f.io.renatus.util.extension.FragmentContextKt.showMigrationErrorDialog;
import static a75f.io.usbserial.UsbModbusService.TAG;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;

import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.schedule.BuildingOccupancy;

import a75f.io.logic.L;
import a75f.io.renatus.R;
import a75f.io.renatus.buildingoccupancy.viewmodels.BuildingOccupancyViewModel;
import a75f.io.renatus.buildingoccupancy.BuildingOccupancyDialogFragment.BuildingOccupancyDialogListener;
import a75f.io.renatus.schedules.ManualSchedulerDialogFragment;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.util.NetworkUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;


public class BuildingOccupancyFragment extends DialogFragment implements BuildingOccupancyDialogListener {


    private TextView addEntry;
    private TextView textViewMonday;
    private TextView textViewTuesday;
    private TextView textViewWednesday;
    private TextView textViewThursday;
    private TextView textViewFriday;
    private TextView textViewSaturday;
    private TextView textViewSunday;
    private View view00, view02, view04, view06, view08, view10, view12, view14, view16, view18, view20, view22, view24;
    private View view01, view03, view05, view07, view09, view11, view13, view15, view17, view19, view21, view23;
    private List<View> viewTimeLines;
    ConstraintLayout constraintScheduler;
    private float mPixelsBetweenAnHour;
    private float mPixelsBetweenADay;

    private Drawable mDrawableBreakLineLeft;
    private Drawable mDrawableBreakLineRight;

    private BuildingOccupancy buildingOccupancy;
    private BuildingOccupancyViewModel buildingOccupancyViewModel;
    String errorMessage;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        buildingOccupancyViewModel = new BuildingOccupancyViewModel();
        View rootView = inflater.inflate(R.layout.fragment_building_occupancy, container, false);
        constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);

        // Add button
        addEntry = rootView.findViewById(R.id.addEntry);
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

        mDrawableBreakLineLeft = AppCompatResources.getDrawable(getContext(), R.drawable.ic_break_line_left_svg);
        mDrawableBreakLineRight = AppCompatResources.getDrawable(getContext(), R.drawable.ic_break_line_right_svg);

        addEntry.setOnClickListener(view -> {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            Fragment buildingOccupancyFragment = getChildFragmentManager().findFragmentByTag("popup");
            if(buildingOccupancyFragment != null){
                fragmentTransaction.remove(buildingOccupancyFragment);
            }
            BuildingOccupancyDialogFragment buildingOccupancyDialogFragment =
                    new BuildingOccupancyDialogFragment(BuildingOccupancyFragment.this);
            buildingOccupancyDialogFragment.show(fragmentTransaction, "popup");
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
                mPixelsBetweenADay = constraintScheduler.getHeight() / 7f;

                //Leave 20% for padding.
                mPixelsBetweenADay = mPixelsBetweenADay - (mPixelsBetweenADay * .2f);

                buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancy();
                if(buildingOccupancy == null && !validateMigration()){
                    showMigrationErrorDialog(requireContext());
                }else {
                    drawBuildingOccupancy();
                    drawCurrentTime();
                }
            }
        });


        return rootView;
    }

    public void onClickCancel() {
        buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancy();
        drawBuildingOccupancy();
    }

    BuildingOccupancy.Days removeEntry = null;
    public boolean onClickSave(int position, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute,
                               ArrayList<DAYS> days){

        boolean isCloudConnected = CCUHsApi.getInstance().readHisValByQuery("cloud and connected and diag and point") > 0;

        if (!NetworkUtil.isNetworkConnected(getActivity()) || !isCloudConnected ) {
            Toast.makeText(getActivity(), "Building Occupancy cannot be edited when CCU is offline. Please " +
                    "connect to network.", Toast.LENGTH_LONG).show();
            return false;
        }


        if (position != ManualSchedulerDialogFragment.NO_REPLACE) {
            //sort schedule days according to the start hour of the day
            try {
                Collections.sort(buildingOccupancy.getDays(), (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
                Collections.sort(buildingOccupancy.getDays(), (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
                removeEntry = buildingOccupancy.getDays().remove(position);
            }catch (ArrayIndexOutOfBoundsException e) {
                Log.d(TAG, "onClickSave: " + e.getMessage());
            }
        } else {
            removeEntry = null;
        }

        Log.d("CCU_UI"," onClickSave "+"startTime "+startTimeHour+":"+startTimeMinute+" endTime "+endTimeHour+":"+endTimeMinute+" removeEntry "+removeEntry);

        List<BuildingOccupancy.Days> daysList = buildingOccupancyViewModel.constructBuildingOccupancyDays(startTimeHour,
                endTimeHour,  startTimeMinute, endTimeMinute, days);
        for (BuildingOccupancy.Days d : daysList) {
            Log.d("CCU_UI", " daysArrayList  "+d);
        }

        boolean intersection = buildingOccupancy.checkIntersection(daysList);
        if (intersection) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("The current settings cannot be overridden because the following duration of the " +
                            "Building Occupancy are overlapping \n"+
                            buildingOccupancyViewModel.getScheduleOverlapMessage(daysList,buildingOccupancy))
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_dialog_alert)
                    .setPositiveButton("OK", (dialog, id) -> {
                        if (removeEntry != null)
                            buildingOccupancy.getDays().add(position, removeEntry);
                    });

            AlertDialog alert = builder.create();
            alert.show();
            return false;

        }

        HashMap<String, ArrayList<Interval>> spillsMap =days == null ? buildingOccupancyViewModel.getRemoveScheduleSpills(buildingOccupancy):
                buildingOccupancyViewModel.getScheduleSpills(daysList,buildingOccupancy);

        if (spillsMap != null && spillsMap.size() > 0 && position != ManualSchedulerDialogFragment.NO_REPLACE) {
            RxjavaUtil.executeBackgroundTask( () -> ProgressDialogUtils.showProgressDialog(getActivity(),
                            "Fetching Zone Schedules..."),
                    () -> {
                        errorMessage = buildingOccupancyViewModel.getWarningMessage(spillsMap);},
                    ()-> {
                        ProgressDialogUtils.hideProgressDialog();
                        if (errorMessage != null && !errorMessage.equals("")) {
                            if (errorMessage.contains("Named Schedule")) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(errorMessage)
                                        .setCancelable(false)
                                        .setTitle("Schedule Errors")
                                        .setIcon(R.drawable.ic_dialog_alert)
                                        .setNegativeButton("Re-Edit", (dialog, id) -> {
                                            showdialog(position);
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            } else if (errorMessage.contains("zone")) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(errorMessage)
                                        .setCancelable(false)
                                        .setTitle("Schedule Errors")
                                        .setIcon(R.drawable.ic_dialog_alert)
                                        .setNegativeButton("Re-Edit", (dialog, id) -> {
                                            showdialog(position);
                                        })
                                        .setPositiveButton("Force-Trim", (dialog, id) -> {
                                            buildingOccupancy.getDays().addAll(daysList);
                                            ScheduleUtil.trimZoneSchedules(spillsMap);
                                            if (buildingOccupancy.getDays().contains(removeEntry)) {
                                                buildingOccupancy.getDays().remove(removeEntry);
                                            }
                                            doScheduleUpdate();
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            }
                        } else {
                            buildingOccupancy.getDays().addAll(daysList);
                            doScheduleUpdate();
                            buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancy();
                        }
                    });
        }else{
            ProgressDialogUtils.hideProgressDialog();
            buildingOccupancy.getDays().addAll(daysList);
            doScheduleUpdate();
            buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancy();
        }
        return true;
    }

    private void drawBuildingOccupancy(){
        buildingOccupancy.populateIntersections();
        new Handler(Looper.getMainLooper()).post(() -> {

            hasTextViewChildren();
            List<BuildingOccupancy.Days> days = buildingOccupancy.getDays();
            Collections.sort(days, Comparator.comparingInt(BuildingOccupancy.Days::getSthh));
            Collections.sort(days, Comparator.comparingInt(BuildingOccupancy.Days::getDay));


            List<BuildingOccupancy.Days> unoccupiedDays = buildingOccupancyViewModel.getUnoccupiedDays(days);
            for (int i = 0; i < days.size(); i++) {
                BuildingOccupancy.Days occupiedDaysElement = days.get(i);
                if (occupiedDaysElement.getSthh() > occupiedDaysElement.getEthh()) {
                    for (int j = 0; j < unoccupiedDays.size(); j++) {
                        BuildingOccupancy.Days daysElement1 = unoccupiedDays.get(j);
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
                BuildingOccupancy.Days daysElement = unoccupiedDays.get(i);
                drawSchedule(i, daysElement.getSthh(), daysElement.getEthh(), daysElement.getStmm(), daysElement.getEtmm(),
                        DAYS.values()[daysElement.getDay()], daysElement.isIntersection(), false);
            }

            for (int i = 0; i < days.size(); i++) {
                BuildingOccupancy.Days daysElement = days.get(i);
                drawSchedule(i, daysElement.getSthh(), daysElement.getEthh(), daysElement.getStmm(), daysElement.getEtmm(),
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

    private void drawSchedule(int position, int startTimeHH, int endTimeHH, int startTimeMM, int endTimeMM, DAYS day,
                              boolean intersection, boolean isOccupied) {
        Typeface typeface=Typeface.DEFAULT;
        try {
            typeface = Typeface.createFromAsset(requireActivity().getAssets(), "fonts/lato_regular.ttf");
        }catch (Exception e){
            e.printStackTrace();
        }

        if (startTimeHH > endTimeHH || (startTimeHH == endTimeHH && startTimeMM > endTimeMM)) {
            drawScheduleBlock(position, typeface, startTimeHH, 24, startTimeMM, 0,
                    getTextViewFromDay(day), false, true, intersection, isOccupied);
            drawScheduleBlock(position, typeface, 0, endTimeHH, 0, endTimeMM,
                    getTextViewFromDay(day.getNextDay()), true, false, intersection, isOccupied);
        } else {
            drawScheduleBlock(position, typeface, startTimeHH, endTimeHH, startTimeMM,
                    endTimeMM, getTextViewFromDay(day), false, false, intersection, isOccupied);
        }
    }

    private void drawScheduleBlock(int position, Typeface typeface, int tempStartTime, int tempEndTime,
                                   int startTimeMM, int endTimeMM, TextView textView,
                                   boolean leftBreak, boolean rightBreak, boolean intersection, boolean isOccupied) {

        Log.i(L.TAG_CCU_UI, "position: "+position+" tempStartTime: " + tempStartTime + " tempEndTime: " + tempEndTime + " startTimeMM: " + startTimeMM + " endTimeMM " + endTimeMM);

        if(getContext()==null) return;
        AppCompatTextView textViewTemp = new AppCompatTextView(getContext());
        textViewTemp.setGravity(Gravity.CENTER_HORIZONTAL);
        if(typeface!=null)
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

        Drawable drawableCompat;

        if (leftBreak) {
            drawableCompat = getResources().getDrawable(R.drawable.occupancy_background_left, null);
            if (intersection) {
                Drawable rightGreyBar = getResources().getDrawable(R.drawable.vline, null);
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
            drawableCompat = getResources().getDrawable(R.drawable.occupancy_background_right, null);
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
                Drawable rightGreyBar = getResources().getDrawable(R.drawable.vline, null);
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
        if(!isOccupied){
            return;
        }
        textViewTemp.setOnClickListener(view -> {
            int clickedPosition = (int)view.getTag();
            buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancy();
            List<BuildingOccupancy.Days> days = buildingOccupancy.getDays();
            Collections.sort(days, Comparator.comparingInt(BuildingOccupancy.Days::getSthh));
            Collections.sort(days, Comparator.comparingInt(BuildingOccupancy.Days::getDay));
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            Fragment buildingOccupancyFragment = getChildFragmentManager().findFragmentByTag("popup");
            if(buildingOccupancyFragment != null){
                fragmentTransaction.remove(buildingOccupancyFragment);
            }
            BuildingOccupancyDialogFragment buildingOccupancyDialogFragment =
                    new BuildingOccupancyDialogFragment(BuildingOccupancyFragment.this,
                            clickedPosition, days.get(clickedPosition));
            buildingOccupancyDialogFragment.show(fragmentTransaction, "popup");
        });
    }

    private void drawCurrentTime() {

        DateTime now = new DateTime(MockTime.getInstance().getMockTime());


        DAYS day = DAYS.values()[now.getDayOfWeek() - 1];
        Log.i("Scheduler", "DAY: " + day.toString());
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

    private void doScheduleUpdate() {
        CCUHsApi.getInstance().updateBuildingOccupancy(buildingOccupancy);
        CCUHsApi.getInstance().syncEntityTree();
        drawBuildingOccupancy();
        buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancy();
    }

    private void showdialog(int position){
        buildingOccupancy = CCUHsApi.getInstance().getBuildingOccupancy();
        List<BuildingOccupancy.Days> alldays = buildingOccupancy.getDays();
        Collections.sort(alldays, Comparator.comparingInt(BuildingOccupancy.Days::getSthh));
        Collections.sort(alldays, Comparator.comparingInt(BuildingOccupancy.Days::getDay));
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment buildingOccupancyFragment = getChildFragmentManager().findFragmentByTag("popup");
        if(buildingOccupancyFragment != null){
            fragmentTransaction.remove(buildingOccupancyFragment);
        }
        BuildingOccupancyDialogFragment buildingOccupancyDialogFragment =
                new BuildingOccupancyDialogFragment(BuildingOccupancyFragment.this,
                        position, alldays.get(position));
        buildingOccupancyDialogFragment.show(fragmentTransaction, "popup");
    }


}
