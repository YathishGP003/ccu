package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Schedule;
import a75f.io.renatus.util.TimeUtils;


@SuppressLint("ValidFragment")
public class ManualCalendarDialogFragment extends DialogFragment implements View.OnClickListener  {

    private static final String SCHEDULE_KEY = "SCHEDULE_KEY";
    private Schedule.Days mDay;
    public static int NO_REPLACE = -1;
    private int mPosition;
    private DateTime mStartDate;
    private DateTime mEndDate;
    private EditText mVacationName;
    private MaterialCalendarView mCalendarView;
    private Button mButtonSave;
    private Button mButtonCancel;



    public interface ManualCalendarDialogListener {
        boolean onClickSave(int position, String vacationName, DateTime startDate, DateTime endDate);
        boolean onClickCancel(DialogFragment dialog);
    }

    private ManualCalendarDialogListener mListener;

    public ManualCalendarDialogFragment(ManualCalendarDialogListener mListener) {
        this.mListener = mListener;
    }

    public ManualCalendarDialogFragment(ManualCalendarDialogListener mListener, String name, int position, DateTime startDate, DateTime endDate) {
        this.mPosition = position;
        this.mListener = mListener;
        mStartDate = startDate;
        mEndDate = endDate;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_vacation_calendar, null);
        mCalendarView = view.findViewById(R.id.calendarView);
        mVacationName = view.findViewById(R.id.editText_vacationName);

        mButtonSave = view.findViewById(R.id.buttonSaveTwo);
        mButtonCancel = view.findViewById(R.id.buttonCancelTwo);

        mButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mCalendarView.getSelectedDates();
                dismiss();
            }
        });

        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        AlertDialog builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();


        return builder;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.buttonSaveTwo:

                boolean success = processCalendar();

                if(success) {
                    dismiss();
                }
                break;
            case R.id.buttonCancel:
                dismiss();
                break;
        }
    }

    private boolean processCalendar() {



        boolean validated = validate();

        if(!validated)
            return false;



        if(mCalendarView.getSelectedDates().size() == 1)
        {
            List<CalendarDay> selectedDates = mCalendarView.getSelectedDates();

            mListener.onClickSave(NO_REPLACE, mVacationName.getText().toString(),
                    new DateTime(selectedDates.get(0).getDate().toEpochDay()), new DateTime(selectedDates.get(0).getDate().toEpochDay()));
        }
        else if(mCalendarView.getSelectedDates().size() > 1)
        {
            List<CalendarDay> selectedDates = mCalendarView.getSelectedDates();

            mListener.onClickSave(NO_REPLACE, mVacationName.getText().toString(),
                    new DateTime(selectedDates.get(0).getDate().toEpochDay()), new DateTime(selectedDates.get(selectedDates.size() - 1).getDate().toEpochDay()));
        }


        return true;
    }

    private boolean validate() {
        if(mVacationName.getText() == null || mVacationName.getText().toString().length() == 0)
        {
            Toast.makeText(this.getContext(), "Please enter a vacation name", Toast.LENGTH_SHORT).show();
            return false;
        }


        if(mCalendarView.getSelectedDates() == null || mCalendarView.getSelectedDates().size() == 0) {
            Toast.makeText(this.getContext(), "Please select a calendar item", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

}