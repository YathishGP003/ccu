package a75f.io.renatus.schedules;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.renatus.R;
import a75f.io.renatus.views.RangeDecorator;

public class ManualCalendarDialogFragment extends DialogFragment implements View.OnClickListener
{
    private DateTime             mStartDate;
    private DateTime             mEndDate;
    private EditText             mVacationNameEditText;
    private MaterialCalendarView mCalendarView;
    private Button               mButtonSave;
    private Button               mButtonCancel;
    private ImageView            clearText;
    private String               mVacationName;
    private String               mId;
    private String               roomRef;


    public ManualCalendarDialogFragment() {}


    public interface ManualCalendarDialogListener
    {
        boolean onClickSave(String id, String vacationName, DateTime startDate, DateTime endDate);

        boolean onClickCancel(DialogFragment dialog);
    }

    private ManualCalendarDialogListener mListener;

    @SuppressLint("ValidFragment")
    public ManualCalendarDialogFragment(String id, String name, DateTime startDate, DateTime endDate,String roomRef, ManualCalendarDialogListener mListener)
    {
        this.mListener = mListener;
        this.mStartDate = startDate;
        this.mEndDate = endDate;
        this.mId = id;
        this.mVacationName = name;
        this.roomRef = roomRef;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {


        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View           view     = inflater.inflate(R.layout.fragment_vacation_calendar, null);
        mCalendarView = view.findViewById(R.id.calendarView);
        mVacationNameEditText = view.findViewById(R.id.editText_vacationName);
        clearText = view.findViewById(R.id.clearText);

        mButtonSave = view.findViewById(R.id.buttonSaveTwo);
        mButtonCancel = view.findViewById(R.id.buttonCancelTwo);
        clearText.setOnClickListener(this);

        if (mVacationName != null)
        {
            mVacationNameEditText.setText(this.mVacationName);
            mVacationNameEditText.setSelection(this.mVacationName.length());
        }

        if (mStartDate != null && mEndDate != null)
        {
            CalendarDay startDay = CalendarDay.from(mStartDate.getYear(), mStartDate.getMonthOfYear(), mStartDate.getDayOfMonth());
            CalendarDay endDay = CalendarDay.from(mEndDate.getYear(), mEndDate.getMonthOfYear(), mEndDate.getDayOfMonth());

            mCalendarView.selectRange(startDay, endDay);
            List<CalendarDay> dates = mCalendarView.getSelectedDates();
            mCalendarView.removeDecorators();
            RangeDecorator rangeDecorator = new RangeDecorator(getActivity(),R.drawable.square_background,dates);
            mCalendarView.addDecorator(rangeDecorator);

            List<CalendarDay> startDate = new ArrayList<>();
            startDate.add(dates.get(0));
            RangeDecorator rangeDecorator1 = new RangeDecorator(getActivity(),R.drawable.left_circle_orange,startDate);
            mCalendarView.addDecorator(rangeDecorator1);

            List<CalendarDay> endDate = new ArrayList<>();
            endDate.add(dates.get(dates.size()-1));
            RangeDecorator rangeDecorator2 = new RangeDecorator(getActivity(),R.drawable.right_circle_orange,endDate);
            mCalendarView.addDecorator(rangeDecorator2);

            DateTime today = new DateTime();
            if (mStartDate.isBefore(today) && mEndDate.isBefore(today) ) {
                mCalendarView.state().edit().setMinimumDate(startDay).commit();
                mCalendarView.state().edit().setMaximumDate(endDay).commit();
            } else if (mStartDate.isBefore(today)) {
                mCalendarView.state().edit().setMinimumDate(startDay).commit();
            } else {
                mCalendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();
            }
            mCalendarView.setCurrentDate(startDay, true);
            
        } else
        {
            mCalendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();
        }
        
        mButtonSave.setOnClickListener(view1 ->
                                       {
                                           processCalendar();
                                           dismiss();
                                       });

        mButtonCancel.setOnClickListener(view12 -> dismiss());
        mCalendarView.setOnDateChangedListener((calendarView, date, selected) -> calendarView.removeDecorators());
        mCalendarView.setOnRangeSelectedListener((calendarView, dates) -> {
            RangeDecorator rangeDecorator = new RangeDecorator(getActivity(),R.drawable.square_background,dates);
            calendarView.addDecorator(rangeDecorator);

            List<CalendarDay> startDate = new ArrayList<>();
            startDate.add(dates.get(0));
            RangeDecorator rangeDecorator1 = new RangeDecorator(getActivity(),R.drawable.left_circle_orange,startDate);
            calendarView.addDecorator(rangeDecorator1);

            List<CalendarDay> endDate = new ArrayList<>();
            endDate.add(dates.get(dates.size()-1));
            RangeDecorator rangeDecorator2 = new RangeDecorator(getActivity(),R.drawable.right_circle_orange,endDate);
            calendarView.addDecorator(rangeDecorator2);

        });

        AlertDialog builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();


        return builder;
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.buttonSaveTwo:

                boolean success = processCalendar();

                if (success)
                {
                    dismiss();
                }
                break;
            case R.id.buttonCancel:
                dismiss();
                break;
            case R.id.clearText:
                if (!TextUtils.isEmpty(mVacationNameEditText.getText())){
                    mVacationNameEditText.setText("");
                }
                break;
        }
    }

    private boolean processCalendar()
    {

        boolean validated = validate();

        if (!validated)
            return false;

        if (mCalendarView.getSelectedDates().size() == 1)
        {
            List<CalendarDay> selectedDates = mCalendarView.getSelectedDates();

            mListener.onClickSave(mId, mVacationNameEditText.getText().toString(),
                                  new DateTime().withDate(selectedDates.get(0).getYear(), selectedDates.get(0).getMonth(),
                                                          selectedDates.get(0).getDay()).withTimeAtStartOfDay(),
                                  new DateTime().withDate(selectedDates.get(0).getYear(),
                                                          selectedDates.get(0).getMonth(),
                                                          selectedDates.get(0).getDay()).withTime(23, 59, 59, 0));
        } else if (mCalendarView.getSelectedDates().size() > 1)
        {
            List<CalendarDay> selectedDates = mCalendarView.getSelectedDates();

            mListener.onClickSave(mId, mVacationNameEditText.getText().toString(),
                                  new DateTime().withDate(selectedDates.get(0).getYear(), selectedDates.get(0).getMonth(),
                                                          selectedDates.get(0).getDay()).withTimeAtStartOfDay(),
                                  new DateTime().withDate(selectedDates.get(selectedDates.size() - 1).getYear(), selectedDates.get(selectedDates.size() - 1).getMonth(),
                                                          selectedDates.get(selectedDates.size() - 1).getDay()).withTime(23, 59, 59, 0));
        }

        return true;
    }

    private boolean validate()
    {
        if (mVacationNameEditText.getText() == null || mVacationNameEditText.getText().toString().length() == 0)
        {
            Toast.makeText(this.getContext(), "Please enter a vacation name", Toast.LENGTH_SHORT).show();
            return false;
        }


        if (mCalendarView.getSelectedDates() == null || mCalendarView.getSelectedDates().size() == 0)
        {
            Toast.makeText(this.getContext(), "Please select a calendar item", Toast.LENGTH_SHORT).show();
            return false;
        }
    
        ArrayList<Schedule> vacations;
        if (this.roomRef != null) {
            vacations = CCUHsApi.getInstance().getZoneSchedule(roomRef, true);
            for (Schedule v : vacations) {
                if (!v.getId().equals(mId))
                {
                    if (v.getDis().equalsIgnoreCase((mVacationNameEditText.getText().toString().trim())))
                    {
                        Toast.makeText(this.getContext(), "Vacation " + v.getDis() + " already exists", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (v.getStartDate().getYear() == mCalendarView.getSelectedDates().get(0).getYear() && v.getStartDate().getMonthOfYear() == mCalendarView.getSelectedDates().get(0).getMonth() && v.getStartDate().getDayOfMonth() == mCalendarView.getSelectedDates().get(0).getDay())
                    {
                        Toast.makeText(this.getContext(), "Two or more vacations cannot have the same start date", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            }
        } else
        {
            vacations = CCUHsApi.getInstance().getSystemSchedule(true);
            for (Schedule v : vacations)
            {
                if (!v.getId().equals(mId))
                {
                    if (v.getDis().equalsIgnoreCase((mVacationNameEditText.getText().toString().trim())))
                    {
                        Toast.makeText(this.getContext(), "Vacation " + v.getDis() + " already exists", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (v.getStartDate().getYear() == mCalendarView.getSelectedDates().get(0).getYear() && v.getStartDate().getMonthOfYear() == mCalendarView.getSelectedDates().get(0).getMonth() && v.getStartDate().getDayOfMonth() == mCalendarView.getSelectedDates().get(0).getDay())
                    {
                        Toast.makeText(this.getContext(), "Two or more vacations cannot have the same start date", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            }
        }

        return true;
    }

}