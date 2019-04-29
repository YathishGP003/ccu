package a75f.io.renatus.schedules;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.renatus.R;
import a75f.io.renatus.schedules.ManualSchedulerDialogFragment.ManualScheduleDialogListener;
import a75f.io.renatus.util.FontManager;


/***
 *
 * Add method to add new schedules - done
 * Add method to edit a schedule - done
 * Add method to edit a vacation - IP
 * Add validation - done
 * Add UI to edit zone based schedules
 * Get Tuners from haystack for dead bands
 * Speak with Shilpa about where requirements end for this task
 *
 */
public class SchedulerFragment extends Fragment implements ManualScheduleDialogListener {

    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";
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
    TextView textViewaddEntryIcon;
    TextView textViewVacations;
    Button textViewaddVacations;
    Schedule schedule;
    ConstraintLayout constraintScheduler;
    ArrayList<View> viewTimeLines;
    String mScheduleId;
    String colorMinTemp = "";
    String colorMaxTemp = "";
    RecyclerView mVacationRecycler;
    private OnExitListener mOnExitListener;
    private VacationAdapter mVacationAdapter;


    @Override
    public void onStop() {
        super.onStop();
        if (mOnExitListener != null)
            mOnExitListener.onExit();



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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scheduler, container, false);

        Typeface iconFont = FontManager.getTypeface(getActivity(), FontManager.FONTAWESOME);

        //Scheduler Layout
        constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);
        mVacationRecycler = rootView.findViewById(R.id.vacationRecycler);
        textViewScheduletitle = rootView.findViewById(R.id.scheduleTitle);
        textViewaddEntry = rootView.findViewById(R.id.addEntry);
        textViewaddEntryIcon = rootView.findViewById(R.id.addEntryIcon);
        mVacationLayout = rootView.findViewById(R.id.constraintLt_Vacations);

        textViewaddEntryIcon.setTypeface(iconFont);
        textViewaddEntryIcon.setText(getString(R.string.icon_plus));


        textViewVacations = rootView.findViewById(R.id.vacationsTitle);
         textViewaddVacations= rootView.findViewById(R.id.addVacations);


        textViewaddVacations.setOnClickListener(v -> showVacationDialog());
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
        //colorMinTemp = "#" + Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.min_temp));
        colorMaxTemp = getResources().getString(0 + R.color.max_temp);
        colorMaxTemp = "#" + colorMaxTemp.substring(3);
        //colorMaxTemp = "#" + Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.max_temp));

        textViewaddEntry.setOnClickListener(view -> showDialog(ID_DIALOG_SCHEDULE));
        textViewaddEntryIcon.setOnClickListener(view -> showDialog(ID_DIALOG_SCHEDULE));


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
                if (mPixelsBetweenAnHour == 0) throw new RuntimeException();

                loadSchedule();
                drawCurrentTime();

            }
        });

        return rootView;
    }

    private void loadSchedule()
    {
        
        if (getArguments() != null && getArguments().containsKey(PARAM_SCHEDULE_ID)) {
            mScheduleId = getArguments().getString(PARAM_SCHEDULE_ID);
            schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
            Log.d("CCU_UI"," Loaded Zone Schedule "+mScheduleId);
        } else {
            schedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
            Log.d("CCU_UI"," Loaded System Schedule ");
        }


        if(schedule != null && schedule.isZoneSchedule())
        {
            textViewScheduletitle.setText("Zone Schedule");
            //mVacationLayout.setVisibility(View.GONE);
            //textViewVacations.setVisibility(View.GONE);
            //textViewaddVacations.setVisibility(View.GONE);
        }
        
        loadVacations();
        


        schedule.populateIntersections();

        SchedulerFragment.this.getActivity().runOnUiThread(() ->
                                                           {
                                                               hasTextViewChildren();

                                                               ArrayList<Schedule.Days> days = schedule.getDays();
                                                               for (int i = 0; i < days.size(); i++) {
                                                                   Schedule.Days daysElement = days.get(i);
                                                                   drawSchedule(i, daysElement.getCoolingVal(), daysElement.getHeatingVal(),
                                                                           daysElement.getSthh(), daysElement.getEthh(),
                                                                           daysElement.getStmm(), daysElement.getEtmm(),
                                                                           DAYS.values()[daysElement.getDay()], daysElement.isIntersection());
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
                ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this);
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
                vacationSchedule != null ? vacationSchedule.getStartDate() : null, vacationSchedule != null ? vacationSchedule.getEndDate() : null,
                new ManualCalendarDialogFragment.ManualCalendarDialogListener() {
            @Override
            public boolean onClickSave(String vacationId, String vacationName, DateTime startDate, DateTime endDate)
            {

                if (schedule != null && schedule.isZoneSchedule()) {
                    DefaultSchedules.upsertZoneVacation(vacationId, vacationName, startDate, endDate, schedule.getRoomRef());
                } else
                {
                    DefaultSchedules.upsertVacation(vacationId, vacationName, startDate, endDate);
                }
                loadVacations();
                ScheduleProcessJob.updateSchedules();
                CCUHsApi.getInstance().syncEntityTree();
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
            CCUHsApi.getInstance().deleteEntity(id);
            loadVacations();
            ScheduleProcessJob.updateSchedules();
            CCUHsApi.getInstance().syncEntityTree();
    };


    private void loadVacations() {
    
        ArrayList<Schedule> vacations;
        if (schedule.isZoneSchedule()) {
            vacations = CCUHsApi.getInstance().getZoneSchedule(schedule.getRoomRef(), true);
        } else
        {
            vacations = CCUHsApi.getInstance().getSystemSchedule(true);
        }
        if(vacations != null) {
            mVacationAdapter = new VacationAdapter(vacations, mEditOnClickListener, mDeleteOnClickListener);
            mVacationRecycler.setAdapter(mVacationAdapter);
            mVacationRecycler.setLayoutManager(new LinearLayoutManager(this.getContext()));
        }
    }

    private void showDialog(int id, int position, Schedule.Days day) {

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        switch (id) {
            case ID_DIALOG_SCHEDULE:

                Fragment scheduleFragment = getFragmentManager().findFragmentByTag("popup");
                if (scheduleFragment != null) {
                    ft.remove(scheduleFragment);
                }
                ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this, position, day);
                newFragment.show(ft, "popup");
                break;
        }
    }

    public boolean onClickSave(int position, double coolingTemp, double heatingTemp, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute, ArrayList<DAYS> days) {
        Schedule.Days remove = null;
        if (position != ManualSchedulerDialogFragment.NO_REPLACE) {
            remove = schedule.getDays().remove(position);
        }


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

        boolean intersection = schedule.checkIntersection(daysArrayList);
        if (intersection) {
            if (remove != null)
                schedule.getDays().add(position, remove);
            Toast.makeText(SchedulerFragment.this.getContext(), "Overlap occured can not add", Toast.LENGTH_SHORT).show();
        } else {
            schedule.getDays().addAll(daysArrayList);
            if (schedule.isZoneSchedule())
            {
                CCUHsApi.getInstance().updateZoneSchedule(schedule, schedule.getRoomRef());
            } else
            {
                CCUHsApi.getInstance().updateSchedule(schedule);
            }
            CCUHsApi.getInstance().syncEntityTree();
            loadSchedule();
        }
    
        ScheduleProcessJob.updateSchedules();

        return true;
    }
    
    private void saveScheduleData() {
    
    }

    private void drawSchedule(int position, double heatingTemp, double coolingTemp, int startTimeHH, int endTimeHH, int startTimeMM, int endTimeMM, DAYS day, boolean intersection) {


        String strminTemp = FontManager.getColoredSpanned(Double.toString(coolingTemp), colorMinTemp);
        String strmaxTemp = FontManager.getColoredSpanned(Double.toString(heatingTemp), colorMaxTemp);

        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/lato_regular.ttf");


        if (startTimeHH > endTimeHH) {
            drawScheduleBlock(position, strminTemp, strmaxTemp, typeface, endTimeHH,
                    24, endTimeMM, 0,
                    getTextViewFromDay(day), false, true, intersection);
            drawScheduleBlock(position, strminTemp, strmaxTemp, typeface, 0,
                    startTimeHH, 0, startTimeMM,
                    getTextViewFromDay(day.getNextDay()), true, false, intersection);
        } else {
            drawScheduleBlock(position, strminTemp, strmaxTemp,
                    typeface, startTimeHH, endTimeHH, startTimeMM,
                    endTimeMM, getTextViewFromDay(day), false, false, intersection);
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

        DateTime now = new DateTime(MockTime.getInstance().getMockTime());


        DAYS day = DAYS.values()[now.getDayOfWeek() - 1];
        Log.i("Scheduler", "DAY: " + day.toString());
        int hh = now.getHourOfDay();
        int mm = now.getMinuteOfHour();


        AppCompatImageView imageView = new AppCompatImageView(getActivity());

        imageView.setImageResource(R.drawable.ic_time_marker_svg);
        imageView.setId(View.generateViewId());
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        //imageView.setPadding(0, 50,0, 0);
        //imageView.setForegroundGravity(Gravity.CENTER);
        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int)mPixelsBetweenADay);
        //lp.topMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        //lp.bottomMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        lp.bottomToBottom = getTextViewFromDay(day).getId();
        lp.topToTop = getTextViewFromDay(day).getId();
        lp.startToStart = viewTimeLines.get(hh).getId();

        lp.leftMargin = (int) ((mm / 60.0) * mPixelsBetweenAnHour);

        constraintScheduler.addView(imageView, lp);


    }

    private void drawScheduleBlock(int position, String strminTemp, String strmaxTemp, Typeface typeface,
                                   int tempStartTime, int tempEndTime,
                                   int startTimeMM, int endTimeMM, TextView textView,
                                   boolean leftBreak, boolean rightBreak, boolean intersection) {

        Log.i("Scheduler", "tempStartTime: " + tempStartTime + " tempEndTime: " + tempEndTime + " startTimeMM: " + startTimeMM + " endTimeMM " + endTimeMM);


        AppCompatTextView textViewTemp = new AppCompatTextView(getActivity());
        textViewTemp.setGravity(Gravity.CENTER);
        textViewTemp.setText(Html.fromHtml(strminTemp + " " + strmaxTemp));

        textViewTemp.setTypeface(typeface);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(textViewTemp, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        textViewTemp.setMaxLines(2);
        textViewTemp.setId(View.generateViewId());


        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int) mPixelsBetweenADay);
        lp.baselineToBaseline = textView.getId();


        int leftMargin = startTimeMM > 0 ? (int) ((startTimeMM / 60.0) * mPixelsBetweenAnHour) : lp.leftMargin;
        int rightMargin = endTimeMM > 0 ? (int) (((60 - endTimeMM) / 60.0) * mPixelsBetweenAnHour) : lp.rightMargin;

        lp.leftMargin = leftMargin;
        lp.rightMargin = rightMargin;

        Drawable drawableCompat = null;

        if (leftBreak) {
            drawableCompat = getResources().getDrawable(R.drawable.temperature_background_left);
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
            drawableCompat = getResources().getDrawable(R.drawable.temperature_background_right);
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


            drawableCompat = getResources().getDrawable(R.drawable.temperature_background);

            if (endTimeMM > 0)
                tempEndTime++;

            lp.startToStart = viewTimeLines.get(tempStartTime).getId();
            lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
        }

        textViewTemp.setBackground(drawableCompat);
        constraintScheduler.addView(textViewTemp, lp);

        textViewTemp.setTag(position);
        textViewTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int clickedPosition = (Integer) v.getTag();
                Toast.makeText(SchedulerFragment.this.getContext(), "Clicked: " + clickedPosition, Toast.LENGTH_SHORT).show();
                showDialog(ID_DIALOG_SCHEDULE, clickedPosition, schedule.getDays().get(clickedPosition));
            }
        });
    }

    public boolean onClickCancel(DialogFragment dialog) {
        return true;
    }

    public void setOnExitListener(OnExitListener onExitListener) {
        this.mOnExitListener = onExitListener;

    }

    public interface OnExitListener {
        void onExit();
    }
}
