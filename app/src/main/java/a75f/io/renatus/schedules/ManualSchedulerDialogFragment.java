package a75f.io.renatus.schedules;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.lang.reflect.Method;
import java.util.ArrayList;

import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.TimeUtils;
import a75f.io.renatus.views.RangeBarView;


@SuppressLint("ValidFragment")
public class ManualSchedulerDialogFragment extends DialogFragment {

    private Schedule.Days mDay;
    private ArrayList<Schedule.Days> mDays;
    public static int NO_REPLACE = -1;
    private int mPosition;
    private Schedule mSchedule;
    Prefs prefs;

    public interface ManualScheduleDialogListener {
        boolean onClickSave(int position, double minTemp, double maxTemp, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute,
                            ArrayList<DAYS> days);

        boolean onClickCancel(String scheduleId);
    }

    private ManualScheduleDialogListener mListener;

    public ManualSchedulerDialogFragment(ManualScheduleDialogListener mListener,Schedule schedule) {
        this.mListener = mListener;
        this.mSchedule = schedule;
    }

    public ManualSchedulerDialogFragment(ManualScheduleDialogListener mListener, int position, Schedule.Days day,Schedule schedule) {
        this.mPosition = position;
        this.mDay = day;
        this.mListener = mListener;
        this.mSchedule = schedule;
    }

    public ManualSchedulerDialogFragment(ManualScheduleDialogListener mListener, int position, ArrayList<Schedule.Days> days,Schedule schedule) {
        this.mPosition = position;
        //this.mDay = days.get(0);
        this.mListener = mListener;
        this.mDays = days;
        this.mSchedule = schedule;
    }

    NumberPicker npStartTime;
    NumberPicker npEndTime;

    CheckBox checkBoxMonday;
    CheckBox checkBoxTuesday;
    CheckBox checkBoxWednesday;
    CheckBox checkBoxThursday;
    CheckBox checkBoxFriday;
    CheckBox checkBoxSaturday;
    CheckBox checkBoxSunday;

    Button buttonSave;
    Button buttonCancel;
    RangeBarView rangeSeekBarView;
    int nMinValForStartTime = 0;
    int nMaxValForStartTime = 95;
    int nMinValForEndTime = 1;
    int nMaxValForEndTime = 96;

    Boolean booleanIsMonday = false;
    Boolean booleanIsTuesday = false;
    Boolean booleanIsWednesday = false;
    Boolean booleanIsThursday = false;
    Boolean booleanIsFriday = false;
    Boolean booleanIsSaturday = false;
    Boolean booleanIsSunday = false;

   
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        prefs = new Prefs(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_manualschedule, null);
        int daySelectionBackground = CCUUiUtil.getDayselectionBackgroud(getContext());
        ImageButton deleteButton = view.findViewById(R.id.buttonDelete);
        rangeSeekBarView = view.findViewById(R.id.rangeSeekBar);
        rangeSeekBarView.setZoneSchedule(mSchedule);

        if (mDay == null && mDays == null) {
            deleteButton.setVisibility(View.INVISIBLE);
        } else if (mDay != null || mDays != null){
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.INVISIBLE);
        }

        deleteButton.setOnClickListener(v -> showDeleteAlert());


        npStartTime = view.findViewById(R.id.np1);
        npEndTime = view.findViewById(R.id.np2);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonCancel = view.findViewById(R.id.buttonCancel);

        checkBoxMonday = view.findViewById(R.id.checkBoxMon);
        checkBoxTuesday = view.findViewById(R.id.checkBoxTue);
        checkBoxWednesday = view.findViewById(R.id.checkBoxWed);
        checkBoxThursday = view.findViewById(R.id.checkBoxThu);
        checkBoxFriday = view.findViewById(R.id.checkBoxFri);
        checkBoxSaturday = view.findViewById(R.id.checkBoxSat);
        checkBoxSunday = view.findViewById(R.id.checkBoxSun);

        if (mDay != null || mDays != null){
            checkBoxMonday.setEnabled(false);
            checkBoxTuesday.setEnabled(false);
            checkBoxWednesday.setEnabled(false);
            checkBoxThursday.setEnabled(false);
            checkBoxFriday.setEnabled(false);
            checkBoxSaturday.setEnabled(false);
            checkBoxSunday.setEnabled(false);
        }

        npStartTime.setMinValue(nMinValForStartTime);
        npStartTime.setMaxValue(nMaxValForStartTime);

        npStartTime.setValue(32);
        npStartTime.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npStartTime.setVisibility(View.VISIBLE);
        npStartTime.setWrapSelectorWheel(false);
        npStartTime.setFormatter(TimeUtils::valToTime);
        npStartTime.setOnLongClickListener(view13 -> true);
        npStartTime.setOnTouchListener((view16, motionEvent) -> false);
        npStartTime.setOnClickListener(view15 -> {});

        try {
            Method method = npStartTime.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npStartTime, true);
        } catch (Exception e) {
            //Log.e("Crash", e.getMessage());
        }

        npEndTime.setMinValue(nMinValForEndTime);
        npEndTime.setMaxValue(nMaxValForEndTime);

        npEndTime.setValue(70);
        npEndTime.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npEndTime.setVisibility(View.VISIBLE);
        npEndTime.setWrapSelectorWheel(false);
        npEndTime.setOnLongClickListener(view13 -> true);
        npEndTime.setOnTouchListener((view14, motionEvent) -> false);
        npEndTime.setOnClickListener(view15 -> {});
        npEndTime.setFormatter(TimeUtils::valToTime);

        try {
            Method method = npEndTime.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npEndTime, true);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_CRASH, "Reflection Crash?");
        }


        checkBoxMonday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanIsMonday = true;
                checkBoxMonday.setTextColor(Color.parseColor("#ffffff"));
                //checkBoxMonday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
                checkBoxMonday.setBackgroundResource(daySelectionBackground);
            } else {
                booleanIsMonday = false;
                checkBoxMonday.setTextColor(Color.parseColor("#000000"));
                checkBoxMonday.setBackground(null);
            }
        });
        checkBoxTuesday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanIsTuesday = true;
                checkBoxTuesday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxTuesday.setBackgroundResource(daySelectionBackground);
            } else {
                booleanIsTuesday = false;
                checkBoxTuesday.setTextColor(Color.parseColor("#000000"));
                checkBoxTuesday.setBackground(null);
            }
        });
        checkBoxWednesday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanIsWednesday = true;
                checkBoxWednesday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxWednesday.setBackgroundResource(daySelectionBackground);
            } else {
                booleanIsWednesday = false;
                checkBoxWednesday.setTextColor(Color.parseColor("#000000"));
                checkBoxWednesday.setBackground(null);
            }
        });
        checkBoxThursday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanIsThursday = true;
                checkBoxThursday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxThursday.setBackgroundResource(daySelectionBackground);
            } else {
                booleanIsThursday = false;
                checkBoxThursday.setTextColor(Color.parseColor("#000000"));
                checkBoxThursday.setBackground(null);
            }
        });
        checkBoxFriday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanIsFriday = true;
                checkBoxFriday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxFriday.setBackgroundResource(daySelectionBackground);
            } else {
                booleanIsFriday = false;
                checkBoxFriday.setTextColor(Color.parseColor("#000000"));
                checkBoxFriday.setBackground(null);
            }
        });
        checkBoxSaturday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanIsSaturday = true;
                checkBoxSaturday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxSaturday.setBackgroundResource(daySelectionBackground);
            } else {
                booleanIsSaturday = false;
                checkBoxSaturday.setTextColor(Color.parseColor("#000000"));
                checkBoxSaturday.setBackground(null);
            }
        });
        checkBoxSunday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanIsSunday = true;
                checkBoxSunday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxSunday.setBackgroundResource(daySelectionBackground);
            } else {
                booleanIsSunday = false;
                checkBoxSunday.setTextColor(Color.parseColor("#000000"));
                checkBoxSunday.setBackground(null);
            }
        });

        buttonSave.setOnClickListener(view1 ->
        {
            ArrayList<DAYS> days = new ArrayList<>();
            if (booleanIsMonday) days.add(DAYS.MONDAY);
            if (booleanIsTuesday) days.add(DAYS.TUESDAY);
            if (booleanIsWednesday) days.add(DAYS.WEDNESDAY);
            if (booleanIsThursday) days.add(DAYS.THURSDAY);
            if (booleanIsFriday) days.add(DAYS.FRIDAY);
            if (booleanIsSaturday) days.add(DAYS.SATURDAY);
            if (booleanIsSunday) days.add(DAYS.SUNDAY);


            int startHour = (npStartTime.getValue() - (npStartTime.getValue() % 4)) / 4;
            int startMinutes = (npStartTime.getValue() % 4) * 15;

            int endHour = (npEndTime.getValue() - (npEndTime.getValue() % 4)) / 4;
            int endMinutes = (npEndTime.getValue() % 4) * 15;

            if (startHour == endHour && startMinutes == endMinutes) {
                Toast.makeText(ManualSchedulerDialogFragment.this.getContext(), "Start time and End time cannot be the same", Toast.LENGTH_SHORT).show();
                return;
            }

            if (days.isEmpty()) {
                Toast.makeText(ManualSchedulerDialogFragment.this.getContext(), "Select one or more days to apply the schedule", Toast.LENGTH_SHORT).show();
                return;
            }
            if (prefs.getBoolean(getString(R.string.USE_SAME_TEMP_ALL_DAYS))){
                new AlertDialog.Builder(getActivity())
                        .setCancelable(false)
                        .setTitle(mSchedule.isZoneSchedule()? "Zone Schedule" : "Building Schedule")
                        .setMessage(mSchedule.isZoneSchedule()? "Are you sure you want to update same occupied temperature for all days?" : "Are you sure you want to apply these changes to building? " +
                                "Which will update same occupied temperature for all days in this building.")
                        .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                            for (Schedule.Days sDays: mSchedule.getDays()){
                                sDays.setCoolingVal((double) rangeSeekBarView.getCoolValue());
                                sDays.setHeatingVal((double)rangeSeekBarView.getHeatValue());
                            }
                            mListener.onClickSave(mDay == null ? NO_REPLACE : mPosition, rangeSeekBarView.getCoolValue(),
                                    rangeSeekBarView.getHeatValue(),
                                    startHour, endHour, startMinutes, endMinutes, days);
                            dismiss();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                return;
            }

            if (!mListener.onClickSave(mDay == null ? NO_REPLACE : mPosition, rangeSeekBarView.getCoolValue(),
                    rangeSeekBarView.getHeatValue(),
                    startHour, endHour, startMinutes, endMinutes, days)) {
                return;
            }

            dismiss();
        });

        buttonCancel.setOnClickListener(view12 ->{
            mListener.onClickCancel(mSchedule.getId());
            dismiss();
        });

        AlertDialog builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();

        if (mDay != null) {
            checkDays(mDay);
            checkTime(mDay);

            new CountDownTimer(150, 150) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    checkTemp(mDay);
                }
            }.start();
        }

        if (mDays != null && (!mDays.isEmpty())) {
            for(Schedule.Days d : mDays) {
                checkDays(d);
            }
            new CountDownTimer(150, 150) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    checkTemp(mDays.get(0));
                }
            }.start();

            checkTime(mDays.get(0));
        }

        return builder;
    }
    private void showDeleteAlert() {
        final Dialog alertDialog = new Dialog(getActivity());
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCancelable(false);
        alertDialog.setContentView(R.layout.dialog_delete_schedule);
        alertDialog.findViewById(R.id.btnCancel).setOnClickListener(view -> alertDialog.dismiss());
        alertDialog.findViewById(R.id.btnProceed).setOnClickListener(view -> {
            ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting schedule...");
            mListener.onClickSave(mPosition, 74, 72, 0, 0, 0, 0, null);
            alertDialog.dismiss();
            new Handler().postDelayed(() -> ProgressDialogUtils.hideProgressDialog(), 1000);
            dismiss();
        });

        alertDialog.show();
    }

    private void checkTemp(Schedule.Days mDay) {
        if (mDay.getCoolingVal() != null)
            rangeSeekBarView.setLowerCoolingTemp(mDay.getCoolingVal());

        if (mDay.getHeatingVal() != null)
            rangeSeekBarView.setLowerHeatingTemp(mDay.getHeatingVal());
    }

    private void checkTime(Schedule.Days mDay) {
        int startTimePosition = mDay.getSthh() * 4 + mDay.getStmm() / 15;
        CcuLog.i(L.TAG_CCU_SCHEDULE, "StartTime Position: " + startTimePosition);
        int endTimePosition = mDay.getEthh() * 4 + mDay.getEtmm() / 15;
        CcuLog.i(L.TAG_CCU_SCHEDULE, "EndTime Position: " + endTimePosition);


        npStartTime.setValue(startTimePosition);
        npEndTime.setValue(endTimePosition);
    }

    private void checkDays(Schedule.Days days) {

        if (days.getDay() == DAYS.MONDAY.ordinal()) checkBoxMonday.setChecked(true);
        else if (days.getDay() == DAYS.TUESDAY.ordinal()) checkBoxTuesday.setChecked(true);
        else if (days.getDay() == DAYS.WEDNESDAY.ordinal()) checkBoxWednesday.setChecked(true);
        else if (days.getDay() == DAYS.THURSDAY.ordinal()) checkBoxThursday.setChecked(true);
        else if (days.getDay() == DAYS.FRIDAY.ordinal()) checkBoxFriday.setChecked(true);
        else if (days.getDay() == DAYS.SATURDAY.ordinal()) checkBoxSaturday.setChecked(true);
        else if (days.getDay() == DAYS.SUNDAY.ordinal()) checkBoxSunday.setChecked(true);
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(1165, 646);
        }
    }
}