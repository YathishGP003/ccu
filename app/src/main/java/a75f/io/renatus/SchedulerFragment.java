package a75f.io.renatus;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.L;
import a75f.io.renatus.ManualSchedulerDialogFragment.ManualScheduleDialogListener;
import a75f.io.renatus.util.FontManager;

import java.util.ArrayList;


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
public class SchedulerFragment extends Fragment implements AdapterView.OnItemSelectedListener, ManualScheduleDialogListener {

    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";


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
    TextView textViewaddVacations;
    Schedule schedule;

    ConstraintLayout constraintScheduler;
    ArrayList<View> viewTimeLines;

    String mScheduleId;

    final int ID_DIALOG_SCHEDULE = 01;

    String colorMinTemp = "";
    String colorMaxTemp = "";
    private OnExitListener mOnExitListener;

    @Override
    public void onStop() {
        super.onStop();
        if (mOnExitListener != null)
            mOnExitListener.onExit();

    }

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

        textViewScheduletitle = rootView.findViewById(R.id.scheduleTitle);
        textViewaddEntry = rootView.findViewById(R.id.addEntry);
        textViewaddEntryIcon = rootView.findViewById(R.id.addEntryIcon);

        textViewaddEntryIcon.setTypeface(iconFont);
        textViewaddEntryIcon.setText(getString(R.string.icon_plus));


        textViewVacations = rootView.findViewById(R.id.vacationsTitle);
        textViewaddVacations = rootView.findViewById(R.id.addVacations);

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

        textViewaddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(ID_DIALOG_SCHEDULE);
            }
        });
        textViewaddEntryIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(ID_DIALOG_SCHEDULE);
            }
        });





        return rootView;
    }




    private void loadSchedule() {

        if (getArguments() != null && getArguments().containsKey(PARAM_SCHEDULE_ID)) {
            mScheduleId = getArguments().getString(PARAM_SCHEDULE_ID);
            schedule = CCUHsApi.getInstance().getScheduleById(mScheduleId);
        } else {
            schedule = CCUHsApi.getInstance().getSystemSchedule(false);
        }

        hasTextViewChildren();

        ArrayList<Schedule.Days> days = schedule.getDays();
        for (int i = 0; i < days.size(); i++) {
            Schedule.Days daysElement = days.get(i);
            drawSchedule(i, daysElement.getCoolingVal(), daysElement.getHeatingVal(),
                    daysElement.getSthh(), daysElement.getEthh(), DAYS.values()[daysElement.getDay()]);
        }

    }

    private void hasTextViewChildren() {

        for (int i = 0; i < constraintScheduler.getChildCount(); i++) {
            if (constraintScheduler.getChildAt(i).getTag() != null) {
                constraintScheduler.removeViewAt(i);
                hasTextViewChildren();
                break;
            }
        }

    }

    private void showDialog(int id) {
        switch (id) {
            case ID_DIALOG_SCHEDULE:
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("schedule");
                if (prev != null) {
                    ft.remove(prev);
                }
                ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this);
                newFragment.show(ft, "schedule");
        }
    }

    private void showDialog(int id, int position, Schedule.Days day) {
        switch (id) {
            case ID_DIALOG_SCHEDULE:
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("schedule");
                if (prev != null) {
                    ft.remove(prev);
                }
                ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this, position, day);
                newFragment.show(ft, "schedule");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        loadSchedule();
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3) {
        double val = Double.parseDouble(arg0.getSelectedItem().toString());
        switch (arg0.getId()) {

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }


    public boolean onClickSave(int position, double coolingTemp, double heatingTemp, int startTime, int endTime, ArrayList<DAYS> days) {
        Schedule.Days remove = null;
        if (position != ManualSchedulerDialogFragment.NO_REPLACE) {
            remove = schedule.getDays().remove(position);
        }

        ArrayList<Schedule.Days> daysArrayList = new ArrayList<Schedule.Days>();

        for (DAYS day : days) {
            Schedule.Days dayBO = new Schedule.Days();
            dayBO.setEthh(endTime);
            dayBO.setSthh(startTime);
            dayBO.setHeatingVal(heatingTemp);
            dayBO.setCoolingVal(coolingTemp);
            dayBO.setSunset(false);
            dayBO.setSunrise(false);
            dayBO.setDay(day.ordinal());
            daysArrayList.add(dayBO);
        }

        boolean intersection = schedule.checkIntersection(daysArrayList);
        if (intersection) {
            if (remove != null)
                schedule.getDays().add(position, remove);
            Toast.makeText(SchedulerFragment.this.getContext(), "Overlap occured can not add", Toast.LENGTH_SHORT).show();
        } else {

            schedule.getDays().addAll(daysArrayList);
            CCUHsApi.getInstance().updateSchedule(schedule);
            CCUHsApi.getInstance().syncEntityTree();
            loadSchedule();
        }

        return true;
    }

    private void drawSchedule(int position, double heatingTemp, double coolingTemp, int startTime, int endTime, DAYS day) {

        System.out.println("Start Time: " + startTime);
        System.out.println("End Time: " + endTime);
        String strminTemp = FontManager.getColoredSpanned(Double.toString(coolingTemp), colorMinTemp);
        String strmaxTemp = FontManager.getColoredSpanned(Double.toString(heatingTemp), colorMaxTemp);

        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/lato_regular.ttf");

        TextView temperTextView = null;
        switch (day) {
            case MONDAY:
                temperTextView = textViewMonday;
                break;
            case TUESDAY:
                temperTextView = textViewTuesday;
                break;
            case WEDNESDAY:
                temperTextView = textViewWednesday;
                break;
            case THURSDAY:
                temperTextView = textViewThursday;
                break;
            case FRIDAY:
                temperTextView = textViewFriday;
                break;
            case SATURDAY:
                temperTextView = textViewSaturday;
                break;
            case SUNDAY:
                temperTextView = textViewSunday;
                break;
        }


        drawScheduleBlock(position, strminTemp, strmaxTemp, typeface, startTime, endTime, temperTextView);

    }

    private void drawScheduleBlock(int position, String strminTemp, String strmaxTemp, Typeface typeface, int tempStartTime, int tempEndTime, TextView textViewSunday) {
        TextView textViewTemp = new TextView(getActivity());
        textViewTemp.setGravity(Gravity.CENTER);
        textViewTemp.setText(Html.fromHtml(strminTemp + " " + strmaxTemp));
        textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
        textViewTemp.setTypeface(typeface);
        textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0f);
        textViewTemp.setId(View.generateViewId());
        textViewTemp.setSingleLine();
        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        lp.topToTop = textViewSunday.getId();
        lp.bottomToBottom = textViewSunday.getId();
        lp.startToStart = viewTimeLines.get(tempStartTime).getId();
        lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
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
