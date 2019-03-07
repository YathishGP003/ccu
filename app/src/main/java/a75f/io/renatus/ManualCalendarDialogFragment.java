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
import android.widget.ImageButton;
import android.widget.NumberPicker;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Schedule;
import a75f.io.renatus.util.TimeUtils;


@SuppressLint("ValidFragment")
public class ManualCalendarDialogFragment extends DialogFragment {

    private static final String SCHEDULE_KEY = "SCHEDULE_KEY";
    private Schedule.Days mDay;
    public static int NO_REPLACE = -1;
    private int mPosition;
    private DateTime mStartDate;
    private DateTime mEndDate;

    public interface ManualCalendarDialogListener {
        boolean onClickSave(int position, DateTime startDate, DateTime endDate);
        boolean onClickCancel(DialogFragment dialog);
    }

    private ManualCalendarDialogListener mListener;

    public ManualCalendarDialogFragment(ManualCalendarDialogListener mListener) {
        this.mListener = mListener;
    }

    public ManualCalendarDialogFragment(ManualCalendarDialogListener mListener, int position, DateTime startDate, DateTime endDate) {
        this.mPosition = position;
        this.mListener = mListener;
        mStartDate = startDate;
        mEndDate = endDate;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_vacation_calendar, null);

        Button buttonSave = view.findViewById(R.id.buttonSaveTwo);
        Button buttonCancel = view.findViewById(R.id.buttonCancelTwo);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
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

}