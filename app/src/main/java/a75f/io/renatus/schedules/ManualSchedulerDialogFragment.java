package a75f.io.renatus.schedules;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Schedule;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
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

        boolean onClickCancel(DialogFragment dialog);
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
    int nMinVal = 0;
    int nMaxVal = 95;

    Boolean booleanisMonday = false;
    Boolean booleanisTuesday = false;
    Boolean booleanisWednesday = false;
    Boolean booleanisThursday = false;
    Boolean booleanisFriday = false;
    Boolean booleanisSaturday = false;
    Boolean booleanisSunday = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        prefs = new Prefs(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_manualschedule, null);

        ImageButton deleteButton = view.findViewById(R.id.buttonDelete);
        rangeSeekBarView = view.findViewById(R.id.rangeSeekBar);
        rangeSeekBarView.setZoneSchedule(mSchedule);

        if (mDay == null) {
            deleteButton.setVisibility(View.INVISIBLE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
        }

        deleteButton.setOnClickListener(v -> showDeleteAlert());


        npStartTime = view.findViewById(R.id.np1);
        npEndTime = view.findViewById(R.id.np2);
        setDividerColor(npStartTime);
        setDividerColor(npEndTime);

        buttonSave = view.findViewById(R.id.buttonSave);
        buttonCancel = view.findViewById(R.id.buttonCancel);

        checkBoxMonday = view.findViewById(R.id.checkBoxMon);
        checkBoxTuesday = view.findViewById(R.id.checkBoxTue);
        checkBoxWednesday = view.findViewById(R.id.checkBoxWed);
        checkBoxThursday = view.findViewById(R.id.checkBoxThu);
        checkBoxFriday = view.findViewById(R.id.checkBoxFri);
        checkBoxSaturday = view.findViewById(R.id.checkBoxSat);
        checkBoxSunday = view.findViewById(R.id.checkBoxSun);

        if (mDay != null){
            checkBoxMonday.setEnabled(false);
            checkBoxTuesday.setEnabled(false);
            checkBoxWednesday.setEnabled(false);
            checkBoxThursday.setEnabled(false);
            checkBoxFriday.setEnabled(false);
            checkBoxSaturday.setEnabled(false);
            checkBoxSunday.setEnabled(false);
        }

        npStartTime.setMinValue(nMinVal);
        npStartTime.setMaxValue(nMaxVal);

        npStartTime.setValue(32);
        npStartTime.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npStartTime.setVisibility(View.VISIBLE);
        npStartTime.setWrapSelectorWheel(false);
        npStartTime.setFormatter(TimeUtils::valToTime);
        npStartTime.setOnLongClickListener(view13 -> true);

        try {
            Method method = npStartTime.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npStartTime, true);
        } catch (Exception e) {
            //Log.e("Crash", e.getMessage());
        }

        npEndTime.setMinValue(nMinVal);
        npEndTime.setMaxValue(nMaxVal);

        npEndTime.setValue(70);
        npEndTime.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npEndTime.setVisibility(View.VISIBLE);
        npEndTime.setWrapSelectorWheel(false);
        npEndTime.setOnLongClickListener(view13 -> true);
        npEndTime.setFormatter(TimeUtils::valToTime);

        try {
            Method method = npEndTime.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npEndTime, true);
        } catch (Exception e) {
            Log.e("Crash", "Reflection Crash?");
        }


        checkBoxMonday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanisMonday = true;
                checkBoxMonday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxMonday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
            } else {
                booleanisMonday = false;
                checkBoxMonday.setTextColor(Color.parseColor("#000000"));
                checkBoxMonday.setBackground(null);
            }
        });
        checkBoxTuesday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanisTuesday = true;
                checkBoxTuesday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxTuesday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
            } else {
                booleanisTuesday = false;
                checkBoxTuesday.setTextColor(Color.parseColor("#000000"));
                checkBoxTuesday.setBackground(null);
            }
        });
        checkBoxWednesday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanisWednesday = true;
                checkBoxWednesday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxWednesday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
            } else {
                booleanisWednesday = false;
                checkBoxWednesday.setTextColor(Color.parseColor("#000000"));
                checkBoxWednesday.setBackground(null);
            }
        });
        checkBoxThursday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanisThursday = true;
                checkBoxThursday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxThursday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
            } else {
                booleanisThursday = false;
                checkBoxThursday.setTextColor(Color.parseColor("#000000"));
                checkBoxThursday.setBackground(null);
            }
        });
        checkBoxFriday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanisFriday = true;
                checkBoxFriday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxFriday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
            } else {
                booleanisFriday = false;
                checkBoxFriday.setTextColor(Color.parseColor("#000000"));
                checkBoxFriday.setBackground(null);
            }
        });
        checkBoxSaturday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanisSaturday = true;
                checkBoxSaturday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxSaturday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
            } else {
                booleanisSaturday = false;
                checkBoxSaturday.setTextColor(Color.parseColor("#000000"));
                checkBoxSaturday.setBackground(null);
            }
        });
        checkBoxSunday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                booleanisSunday = true;
                checkBoxSunday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxSunday.setBackground(getResources().getDrawable(R.drawable.bg_weekdays_selector));
            } else {
                booleanisSunday = false;
                checkBoxSunday.setTextColor(Color.parseColor("#000000"));
                checkBoxSunday.setBackground(null);
            }
        });

        buttonSave.setOnClickListener(view1 ->
        {
            ArrayList<DAYS> days = new ArrayList<>();
            if (booleanisMonday) days.add(DAYS.MONDAY);
            if (booleanisTuesday) days.add(DAYS.TUESDAY);
            if (booleanisWednesday) days.add(DAYS.WEDNESDAY);
            if (booleanisThursday) days.add(DAYS.THURSDAY);
            if (booleanisFriday) days.add(DAYS.FRIDAY);
            if (booleanisSaturday) days.add(DAYS.SATURDAY);
            if (booleanisSunday) days.add(DAYS.SUNDAY);


            int startHour = (npStartTime.getValue() - (npStartTime.getValue() % 4)) / 4;
            int startMinutes = (npStartTime.getValue() % 4) * 15;

            int endHour = (npEndTime.getValue() - (npEndTime.getValue() % 4)) / 4;
            int endMinutes = (npEndTime.getValue() % 4) * 15;

            if (startHour == endHour && startMinutes == endMinutes) {
                Toast.makeText(ManualSchedulerDialogFragment.this.getContext(), "Start time and End time cannot be the same", Toast.LENGTH_SHORT).show();
                return;
            }

            if (days.size() == 0) {
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

        buttonCancel.setOnClickListener(view12 -> dismiss());

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

        if (mDays != null) {
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
            mListener.onClickSave(mPosition, 74, 72, 0, 0, 0, 0, null);
            alertDialog.dismiss();
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
        Log.i("Schedule", "StartTime Position: " + startTimePosition);
        int endTimePosition = mDay.getEthh() * 4 + mDay.getEtmm() / 15;
        Log.i("Schedule", "EndTime Position: " + endTimePosition);


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

    }

    private void setDividerColor(NumberPicker picker) {
        Field[] numberPickerFields = NumberPicker.class.getDeclaredFields();
        for (Field field : numberPickerFields) {
            if (field.getName().equals("mSelectionDivider")) {
                field.setAccessible(true);
                try {
                    field.set(picker, getResources().getDrawable(R.drawable.divider_np));
                } catch (IllegalArgumentException e) {
                    Log.v("NP", "Illegal Argument Exception");
                    e.printStackTrace();
                } catch (Resources.NotFoundException e) {
                    Log.v("NP", "Resources NotFound");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    Log.v("NP", "Illegal Access Exception");
                    e.printStackTrace();
                }
                break;
            }
        }
    }


}