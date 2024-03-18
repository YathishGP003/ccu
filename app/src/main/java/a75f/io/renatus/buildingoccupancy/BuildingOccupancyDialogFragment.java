package a75f.io.renatus.buildingoccupancy;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.logic.util.OfflineModeUtilKt;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.buildingoccupancy.viewmodels.BuildingOccupancyDayViewModel;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.NetworkUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.TimeUtils;

public class BuildingOccupancyDialogFragment extends DialogFragment {
    private static final String TAG = "BUILDING_OCCUPANCY_UI";
    private static final String SAVE = "SAVE";
    private static final String ADD = "ADD";
    private int mPosition;
    private BuildingOccupancy.Days mDay;


    private NumberPicker npStartTime;
    private NumberPicker npEndTime;
    private int nMinVal = 0;
    private int nMaxVal = 96;
    private CheckBox checkBoxMonday;
    private CheckBox checkBoxTuesday;
    private CheckBox checkBoxWednesday;
    private CheckBox checkBoxThursday;
    private CheckBox checkBoxFriday;
    private CheckBox checkBoxSaturday;
    private CheckBox checkBoxSunday;
    
    private TextView buildingOccupancyTitle;

    private Button buttonSave;
    private Button buttonCancel;
    private ImageButton buttonDelete;

    public static int NO_REPLACE = -1;

    private BuildingOccupancyDayViewModel buildingOccupancyDayViewModel;

    public interface BuildingOccupancyDialogListener{
        void onClickCancel();
        boolean onClickSave(int position, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute,
                            ArrayList<DAYS> days);
    }

    private BuildingOccupancyDialogListener buildingOccupancyDialogListener;

    public BuildingOccupancyDialogFragment(BuildingOccupancyDialogListener buildingOccupancyDialogListener){
        this.buildingOccupancyDialogListener = buildingOccupancyDialogListener;
    }

    public BuildingOccupancyDialogFragment(BuildingOccupancyDialogListener buildingOccupancyDialogListener,
                                           int mPosition, BuildingOccupancy.Days mDay){
        this.mPosition = mPosition;
        this.mDay = mDay;
        this.buildingOccupancyDialogListener = buildingOccupancyDialogListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        buildingOccupancyDayViewModel = new BuildingOccupancyDayViewModel();
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_building_occupancy, null);
        int dayselectionBackground = CCUUiUtil.getDayselectionBackgroud(getContext());
        buildingOccupancyTitle = view.findViewById(R.id.buildingOccupancyTitle);
        npStartTime = view.findViewById(R.id.np1);
        npEndTime = view.findViewById(R.id.np2);

        npStartTime.setMinValue(nMinVal);
        npStartTime.setMaxValue(nMaxVal);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonSave.setText(ADD);
        buttonCancel = view.findViewById(R.id.buttonCancel);
        buttonDelete = view.findViewById(R.id.buttonDelete);
        buttonDelete.setVisibility(View.GONE);

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
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Log.e(TAG, "Reflection error for start time");
        }

        npEndTime.setMinValue(nMinVal);
        npEndTime.setMaxValue(nMaxVal);
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
        }  catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Log.e(TAG, "Reflection error for end time");
        }

        checkBoxMonday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                checkBoxMonday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxMonday.setBackgroundResource(dayselectionBackground);
            } else {
                checkBoxMonday.setTextColor(Color.parseColor("#000000"));
                checkBoxMonday.setBackground(null);
            }
        });
        checkBoxTuesday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                checkBoxTuesday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxTuesday.setBackgroundResource(dayselectionBackground);
            } else {
                checkBoxTuesday.setTextColor(Color.parseColor("#000000"));
                checkBoxTuesday.setBackground(null);
            }
        });
        checkBoxWednesday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                checkBoxWednesday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxWednesday.setBackgroundResource(dayselectionBackground);
            } else {
                checkBoxWednesday.setTextColor(Color.parseColor("#000000"));
                checkBoxWednesday.setBackground(null);
            }
        });
        checkBoxThursday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                checkBoxThursday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxThursday.setBackgroundResource(dayselectionBackground);
            } else {
                checkBoxThursday.setTextColor(Color.parseColor("#000000"));
                checkBoxThursday.setBackground(null);
            }
        });
        checkBoxFriday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                checkBoxFriday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxFriday.setBackgroundResource(dayselectionBackground);
            } else {
                checkBoxFriday.setTextColor(Color.parseColor("#000000"));
                checkBoxFriday.setBackground(null);
            }
        });
        checkBoxSaturday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                checkBoxSaturday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxSaturday.setBackgroundResource(dayselectionBackground);
            } else {
                checkBoxSaturday.setTextColor(Color.parseColor("#000000"));
                checkBoxSaturday.setBackground(null);
            }
        });
        checkBoxSunday.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // update your model (or other business logic) based on isChecked
            if (isChecked) {
                checkBoxSunday.setTextColor(Color.parseColor("#ffffff"));
                checkBoxSunday.setBackgroundResource(dayselectionBackground);
            } else {
                checkBoxSunday.setTextColor(Color.parseColor("#000000"));
                checkBoxSunday.setBackground(null);
            }
        });

        buttonSave.setOnClickListener(saveView ->
        {
            ArrayList<DAYS> days = new ArrayList<>();
            if (checkBoxMonday.isChecked()) days.add(DAYS.MONDAY);
            if (checkBoxTuesday.isChecked()) days.add(DAYS.TUESDAY);
            if (checkBoxWednesday.isChecked()) days.add(DAYS.WEDNESDAY);
            if (checkBoxThursday.isChecked()) days.add(DAYS.THURSDAY);
            if (checkBoxFriday.isChecked()) days.add(DAYS.FRIDAY);
            if (checkBoxSaturday.isChecked()) days.add(DAYS.SATURDAY);
            if (checkBoxSunday.isChecked()) days.add(DAYS.SUNDAY);


            int startHour = (npStartTime.getValue() - (npStartTime.getValue() % 4)) / 4;
            int startMinutes = (npStartTime.getValue() % 4) * 15;

            int endHour = (npEndTime.getValue() - (npEndTime.getValue() % 4)) / 4;
            int endMinutes = (npEndTime.getValue() % 4) * 15;

            if (startHour == endHour && startMinutes == endMinutes) {
                Toast.makeText(BuildingOccupancyDialogFragment.this.getContext(), "Start time and End time cannot be the same",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (days.size() == 0) {
                Toast.makeText(BuildingOccupancyDialogFragment.this.getContext(), "Select one or more days to add " +
                        "Building Occupancy", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!buildingOccupancyDialogListener.onClickSave(mDay == null ? NO_REPLACE : mPosition, startHour, endHour,
                    startMinutes, endMinutes, days)) {
                return;
            }

            dismiss();
        });


        buttonCancel.setOnClickListener(cancelView ->{
            buildingOccupancyDialogListener.onClickCancel();
            dismiss();
        });

        if (mDay != null) {
            buttonSave.setText(SAVE);
            buildingOccupancyTitle.setText(buildingOccupancyDayViewModel.constructBuildingOccupancyTitle(mDay));
            checkDays(mDay);
            checkTime(mDay);
            buttonDelete.setVisibility(View.VISIBLE);
        }
        buttonDelete.setOnClickListener(v -> showDeleteAlert());

        return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }
    private void showDeleteAlert() {

        boolean isCloudConnected = CCUHsApi.getInstance().readHisValByQuery("cloud and connected and diag and point") > 0;
        if(BuildConfig.BUILD_TYPE == "qa")
            Log.d(TAG,"isCloudConnected" + isCloudConnected);

        if ((!NetworkUtil.isNetworkConnected(getActivity()) || !isCloudConnected ) && !OfflineModeUtilKt.isOfflineMode()){
            Toast.makeText(getActivity(), "Building Occupancy cannot be deleted when CCU is offline. Please " +
                    "connect to network.", Toast.LENGTH_LONG).show();
            return ;
        }

        final Dialog alertDialog = new Dialog(getActivity());
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCancelable(false);
        alertDialog.setContentView(R.layout.dialog_delete_schedule);
        alertDialog.findViewById(R.id.btnCancel).setOnClickListener(view -> alertDialog.dismiss());
        alertDialog.findViewById(R.id.btnProceed).setOnClickListener(view -> {
            ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting schedule...");
            buildingOccupancyDialogListener.onClickSave(mPosition, 0, 0, 0, 0, null);
            alertDialog.dismiss();
            dismiss();
        });

        alertDialog.show();
    }

    private void checkTime(BuildingOccupancy.Days mDay) {
        int startTimePosition = mDay.getSthh() * 4 + mDay.getStmm() / 15;

        int endTimePosition = mDay.getEthh() * 4 + mDay.getEtmm() / 15;
        Log.i("Schedule", "StartTime Position: " + startTimePosition +"EndTime Position: " + endTimePosition);

        npStartTime.setValue(startTimePosition);
        npEndTime.setValue(endTimePosition);
    }

    private void checkDays(BuildingOccupancy.Days days) {

        if (days.getDay() == DAYS.MONDAY.ordinal()) checkBoxMonday.setChecked(true);
        else if (days.getDay() == DAYS.TUESDAY.ordinal()) checkBoxTuesday.setChecked(true);
        else if (days.getDay() == DAYS.WEDNESDAY.ordinal()) checkBoxWednesday.setChecked(true);
        else if (days.getDay() == DAYS.THURSDAY.ordinal()) checkBoxThursday.setChecked(true);
        else if (days.getDay() == DAYS.FRIDAY.ordinal()) checkBoxFriday.setChecked(true);
        else if (days.getDay() == DAYS.SATURDAY.ordinal()) checkBoxSaturday.setChecked(true);
        else if (days.getDay() == DAYS.SUNDAY.ordinal()) checkBoxSunday.setChecked(true);
    }

}
