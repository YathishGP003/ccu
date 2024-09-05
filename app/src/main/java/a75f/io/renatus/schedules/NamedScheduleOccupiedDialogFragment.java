package a75f.io.renatus.schedules;

import static a75f.io.logic.bo.util.UnitUtils.celsiusToFahrenheitTuner;
import static a75f.io.logic.bo.util.UnitUtils.convertingDeadBandValueCtoF;
import static a75f.io.logic.bo.util.UnitUtils.convertingRelativeValueFtoC;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.logic.bo.util.UnitUtils.roundToPointFive;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterVal;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterValDeadBand;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Schedule;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.TimeUtils;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;
import a75f.io.renatus.views.RangeBarView;

public class NamedScheduleOccupiedDialogFragment  extends  DialogFragment{

    private Schedule.Days mDay;
    private ArrayList<Schedule.Days> mDays;
    public static int NO_REPLACE = -1;
    private int mPosition;
    private Schedule mSchedule;
    Prefs prefs;

    private NamedScheduleOccupiedDialogFragmentListener mListener;

    public interface NamedScheduleOccupiedDialogFragmentListener {
        boolean onClickSaveNamed(int position, double minTemp, double maxTemp, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute,
                                 ArrayList<DAYS> days, Double heatingUserLimitMaxVal, Double heatingUserLimitMinVal,
                                 Double coolingUserLimitMaxVal, Double coolingUserLimitMinVal, Double heatingDeadBandVal,
                                 Double coolingDeadBandVal, Schedule.Days mDay);

        void onClickCancelNamed(String scheduleId);
    }

    public NamedScheduleOccupiedDialogFragment(NamedScheduleOccupiedDialogFragmentListener mListener, Schedule schedule) {
        this.mListener = mListener;
        this.mSchedule = schedule;

    }

    public NamedScheduleOccupiedDialogFragment(NamedScheduleOccupiedDialogFragmentListener mListener, Schedule schedule, Schedule.Days day) {
        this.mListener = mListener;
        this.mSchedule = schedule;
        this.mDay = day;
    }

    public NamedScheduleOccupiedDialogFragment(NamedScheduleOccupiedDialogFragmentListener mListener, int position, Schedule.Days day,Schedule schedule) {
        this.mPosition = position;
        this.mDay = day;
        this.mListener = mListener;
        this.mSchedule = schedule;
    }

    public NamedScheduleOccupiedDialogFragment(NamedScheduleOccupiedDialogFragmentListener mListener, int position, ArrayList<Schedule.Days> days,Schedule schedule) {
        this.mPosition = position;
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

    Spinner heatingUserLimitMax;
    Spinner coolingUserLimitMax;
    Spinner heatingUserLimitMin;
    Spinner coolingUserLimitMin;
    Spinner heatingDeadBand;
    Spinner coolingDeadBand;
    HashMap<Object,Object> coolDB;
    HashMap<Object,Object> heatDB;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        prefs = new Prefs(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.named_edit, null);
        int daySelectionBackGround = CCUUiUtil.getDaySelectionBackground(getContext());
        ImageButton deleteButton = view.findViewById(R.id.buttonDelete);
        rangeSeekBarView = view.findViewById(R.id.rangeSeekBar);
        Schedule schedule = CCUHsApi.getInstance().getScheduleById(mSchedule.getId());
        rangeSeekBarView.setZoneSchedule(schedule);
        rangeSeekBarView.setmDay(mDay);

        rangeSeekBarView.setEnabled(false);

        if (mDay == null && mDays == null) {
            deleteButton.setVisibility(View.INVISIBLE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
        }

        deleteButton.setOnClickListener(v -> showDeleteAlert());

        buttonSave = view.findViewById(R.id.buttonSave);
        buttonCancel = view.findViewById(R.id.buttonCancel);

        checkBoxMonday = view.findViewById(R.id.checkBoxMon);
        checkBoxTuesday = view.findViewById(R.id.checkBoxTue);
        checkBoxWednesday = view.findViewById(R.id.checkBoxWed);
        checkBoxThursday = view.findViewById(R.id.checkBoxThu);
        checkBoxFriday = view.findViewById(R.id.checkBoxFri);
        checkBoxSaturday = view.findViewById(R.id.checkBoxSat);
        checkBoxSunday = view.findViewById(R.id.checkBoxSun);
        npStartTime = view.findViewById(R.id.np1);
        npEndTime = view.findViewById(R.id.np2);
        heatingUserLimitMin = view.findViewById(R.id.heatinglimmin);
        heatingUserLimitMin.setDropDownWidth(70);
        heatingUserLimitMax = view.findViewById(R.id.heatinglimmax);
        heatingUserLimitMax.setDropDownWidth(70);
        coolingUserLimitMin = view.findViewById(R.id.coolinglimmin);
        coolingUserLimitMin.setDropDownWidth(70);
        coolingUserLimitMax = view.findViewById(R.id.coolinglimmax);
        coolingUserLimitMax.setDropDownWidth(70);
        heatingDeadBand = view.findViewById(R.id.heatingdeadband);
        heatingDeadBand.setDropDownWidth(70);
        coolingDeadBand = view.findViewById(R.id.coolingdeadband);
        coolingDeadBand.setDropDownWidth(70);

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


        ArrayList<String> heatingAndCoolingLimit = new ArrayList<>();
        ArrayList<String> deadBand = new ArrayList<>();

        double minDeadBandVal = ScheduleUtil.getDeadBandValue("minVal", mSchedule.getRoomRef());
        double maxDeadBandVal = ScheduleUtil.getDeadBandValue("maxVal", mSchedule.getRoomRef());

        if(isCelsiusTunerAvailableStatus()){
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add( fahrenheitToCelsius(val) + "\u00B0C");
            }

            double minVal = convertingRelativeValueFtoC(minDeadBandVal);
            if(minVal < 0.5){
                minVal = 0.5;
            }
            double maxVal = convertingRelativeValueFtoC(maxDeadBandVal);
            for (double val = minVal;  val <= maxVal; val += 0.5) {
                deadBand.add( ((val)) + "\u00B0C");
            }

        }else{
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add(val+"\u00B0F");
            }
            for (double val = minDeadBandVal;  val <= maxDeadBandVal; val += 0.5) {
                deadBand.add((val) + "\u00B0F");
            }
        }

        coolDB = CCUHsApi.getInstance().readEntity("point and cooling and deadband and schedulable and default");
        heatDB = CCUHsApi.getInstance().readEntity("point and heating and deadband and schedulable and default");

        ArrayAdapter<String> heatingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingAndCoolingLimit);
        heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingUserLimitMax.setAdapter(heatingAdapter);
        heatingUserLimitMin.setAdapter(heatingAdapter);

        ArrayAdapter<String> coolingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingAndCoolingLimit);
        coolingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingUserLimitMin.setAdapter(coolingAdapter);
        coolingUserLimitMax.setAdapter(coolingAdapter);

        ArrayAdapter<String> deadBandAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, deadBand);
        deadBandAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingDeadBand.setAdapter(deadBandAdapter);
        heatingDeadBand.setAdapter(deadBandAdapter);
        heatingUserLimitMax.setEnabled(true);
        heatingUserLimitMin.setEnabled(true);
        coolingUserLimitMax.setEnabled(true);
        coolingUserLimitMin.setEnabled(true);
        heatingDeadBand.setEnabled(true);
        coolingDeadBand.setEnabled(true);

        setDefaultUserLimits(heatingAdapter, coolingAdapter, deadBandAdapter);
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
                checkBoxMonday.setBackgroundResource(daySelectionBackGround);
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
                checkBoxTuesday.setBackgroundResource(daySelectionBackGround);
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
                checkBoxWednesday.setBackgroundResource(daySelectionBackGround);
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
                checkBoxThursday.setBackgroundResource(daySelectionBackGround);
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
                checkBoxFriday.setBackgroundResource(daySelectionBackGround);
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
                checkBoxSaturday.setBackgroundResource(daySelectionBackGround);
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
                checkBoxSunday.setBackgroundResource(daySelectionBackGround);
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

            double heatingUserLimitMaxVal;
            double heatingUserLimitMinVal;
            double coolingUserLimitMaxVal;
            double coolingUserLimitMinVal;
            double heatingDeadBandVal;
            double coolingDeadBandVal;
            if(isCelsiusTunerAvailableStatus()) {
                heatingUserLimitMaxVal = celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(heatingUserLimitMax.getSelectedItem().toString(), "\u00B0C" )));
                heatingUserLimitMinVal = celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(heatingUserLimitMin.getSelectedItem().toString(), "\u00B0C" )));
                coolingUserLimitMaxVal = celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(coolingUserLimitMax.getSelectedItem().toString(), "\u00B0C" )));
                coolingUserLimitMinVal = celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(coolingUserLimitMin.getSelectedItem().toString(), "\u00B0C" )));
                heatingDeadBandVal = roundToPointFive(convertingDeadBandValueCtoF(Double.parseDouble(StringUtils.substringBefore(heatingDeadBand.getSelectedItem().toString(), "\u00B0C" ))));
                coolingDeadBandVal = roundToPointFive(convertingDeadBandValueCtoF(Double.parseDouble(StringUtils.substringBefore(coolingDeadBand.getSelectedItem().toString(), "\u00B0C" ))));

            }else{
                heatingUserLimitMaxVal = MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMax.getSelectedItem().toString());
                heatingUserLimitMinVal = MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMin.getSelectedItem().toString());
                coolingUserLimitMaxVal = MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMax.getSelectedItem().toString());
                coolingUserLimitMinVal = MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMin.getSelectedItem().toString());
                heatingDeadBandVal = MasterControlUtil.getAdapterFarhenheitVal(heatingDeadBand.getSelectedItem().toString());
                coolingDeadBandVal = MasterControlUtil.getAdapterFarhenheitVal(coolingDeadBand.getSelectedItem().toString());
            }
            if (startHour == endHour && startMinutes == endMinutes) {
                Toast.makeText(this.getContext(), "Start time and End time cannot be the same", Toast.LENGTH_SHORT).show();
                return;
            }

            if (days.isEmpty()) {
                Toast.makeText(this.getContext(), "Select one or more days to apply the schedule", Toast.LENGTH_SHORT).show();
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
                            mListener.onClickSaveNamed(mDay == null ? NO_REPLACE : mPosition, rangeSeekBarView.getCoolValue(),
                                    rangeSeekBarView.getHeatValue(), startHour, endHour, startMinutes,
                                    endMinutes, days, heatingUserLimitMaxVal, heatingUserLimitMinVal, coolingUserLimitMaxVal,
                                    coolingUserLimitMinVal, heatingDeadBandVal, coolingDeadBandVal, mDay);

                            dismiss();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                return;
            }

            double buildingLimitMax = Domain.buildingEquip.getBuildingLimitMax().readPriorityVal();
            double buildingLimitMin =  Domain.buildingEquip.getBuildingLimitMin().readPriorityVal();
            Double unoccupiedZoneSetback = schedule.getUnoccupiedZoneSetback();
            double buildingToZoneDiff = Domain.buildingEquip.getBuildingToZoneDifferential().readPriorityVal();
            double coolingTemp = rangeSeekBarView.getCoolValue();
            double heatingTemp = rangeSeekBarView.getHeatValue();
            String warning;

            CcuLog.i(L.TAG_CCU, "buildingLimitMin "+buildingLimitMin);
            CcuLog.i(L.TAG_CCU, "heatingUserLimitMinVal "+heatingUserLimitMinVal);
            CcuLog.i(L.TAG_CCU, "buildingToZoneDiff "+buildingToZoneDiff);
            CcuLog.i(L.TAG_CCU, "unoccupiedZoneSetback "+unoccupiedZoneSetback);
            CcuLog.i(L.TAG_CCU, "buildingLimitMax "+buildingLimitMax);
            CcuLog.i(L.TAG_CCU, "coolingUserLimitMaxVal "+coolingUserLimitMaxVal);

            warning = MasterControlUtil.validateDesiredTemp(coolingTemp, heatingTemp, coolingUserLimitMinVal,
                    coolingUserLimitMaxVal, heatingUserLimitMinVal, heatingUserLimitMaxVal, heatingDeadBandVal, coolingDeadBandVal);
            if (warning == null) {
                warning = MasterControlUtil.validateZone(buildingLimitMin, heatingUserLimitMinVal,
                        buildingToZoneDiff, unoccupiedZoneSetback, buildingLimitMax, coolingUserLimitMaxVal);
                if (warning == null) {
                    warning = MasterControlUtil.validateLimits(heatingUserLimitMaxVal, heatingUserLimitMinVal,
                            heatingDeadBandVal, coolingUserLimitMaxVal, coolingUserLimitMinVal, coolingDeadBandVal);
                }
            }

            if(warning == null) {
                if (!mListener.onClickSaveNamed(mDay == null ? NO_REPLACE : mPosition, rangeSeekBarView.getCoolValue(),
                        rangeSeekBarView.getHeatValue(), startHour, endHour, startMinutes, endMinutes,
                        days, heatingUserLimitMaxVal, heatingUserLimitMinVal, coolingUserLimitMaxVal,
                        coolingUserLimitMinVal, heatingDeadBandVal, coolingDeadBandVal, mDay)) {
                    return;
                }
                dismiss();
            }else {
                android.app.AlertDialog.Builder builder =
                        new android.app.AlertDialog.Builder(getActivity());
                builder.setMessage(warning);
                builder.setCancelable(false);
                builder.setTitle(R.string.warning_ns);
                builder.setIcon(R.drawable.ic_alert);
                builder.setNegativeButton("OKAY", (dialog1, id) -> dialog1.dismiss());

                android.app.AlertDialog alert = builder.create();
                alert.show();
            }
        });
        heatingUserLimitMin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (isCelsiusTunerAvailableStatus()) {
                    rangeSeekBarView.setHeatingLimitMin(celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(heatingUserLimitMin.getSelectedItem().toString(), "\u00B0C"))));
                } else {
                    rangeSeekBarView.setHeatingLimitMin(MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMin.getSelectedItem().toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        coolingUserLimitMax.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                if (isCelsiusTunerAvailableStatus()) {
                    rangeSeekBarView.setCoolingLimitMax(celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(coolingUserLimitMax.getSelectedItem().toString(), "\u00B0C"))));
                } else {
                    rangeSeekBarView.setCoolingLimitMax(MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMax.getSelectedItem().toString()));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        heatingUserLimitMax.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (isCelsiusTunerAvailableStatus()) {
                    rangeSeekBarView.setHeatingLimitMax(celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(heatingUserLimitMax.getSelectedItem().toString(), "\u00B0C"))));
                } else {
                    rangeSeekBarView.setHeatingLimitMax(MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMax.getSelectedItem().toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        coolingUserLimitMin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                if (isCelsiusTunerAvailableStatus()) {
                    rangeSeekBarView.setCoolingLimitMin(celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(coolingUserLimitMin.getSelectedItem().toString(), "\u00B0C"))));
                } else {
                    rangeSeekBarView.setCoolingLimitMin(MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMin.getSelectedItem().toString()));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        heatingDeadBand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (isCelsiusTunerAvailableStatus()) {
                    rangeSeekBarView.setHeatingDeadBand(celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(heatingDeadBand.getSelectedItem().toString(), "\u00B0C"))));
                } else {
                    rangeSeekBarView.setHeatingDeadBand(MasterControlUtil.getAdapterFarhenheitVal(heatingDeadBand.getSelectedItem().toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        coolingDeadBand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (isCelsiusTunerAvailableStatus()) {
                    rangeSeekBarView.setCoolingDeadBand(celsiusToFahrenheitTuner(Double.parseDouble(StringUtils.substringBefore(coolingDeadBand.getSelectedItem().toString(), "\u00B0C"))));
                } else {
                    rangeSeekBarView.setCoolingDeadBand(MasterControlUtil.getAdapterFarhenheitVal(coolingDeadBand.getSelectedItem().toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        buttonCancel.setOnClickListener(view12 ->{mListener.onClickCancelNamed(mSchedule.getId());
            dismiss();
        });


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

        }else if(mDay != null){

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

        }else {

            new CountDownTimer(150, 150) {
                @Override
                public void onTick(long l) {
                }
                @Override
                public void onFinish() {
                    checkTemp();
                }
            }.start();
        }

        return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();

    }

    private void setDefaultUserLimits(ArrayAdapter<String> heatingAdapter, ArrayAdapter<String> coolingAdapter, ArrayAdapter<String> deadBandAdapter) {

            heatingUserLimitMax.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(72, true)));
            heatingUserLimitMin.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(67, true)));
            coolingUserLimitMax.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(77, true)));
            coolingUserLimitMin.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(72, true)));
            heatingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(2, true)));
            coolingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(2, true)));

    }

    private void resetRangeBarLimits() {
        if (isCelsiusTunerAvailableStatus()) {
            rangeSeekBarView.setHeatingLimitMinForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(heatingUserLimitMin.getSelectedItem().toString(), "\u00B0C"))));
            rangeSeekBarView.setHeatingLimitMaxForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(heatingUserLimitMax.getSelectedItem().toString(), "\u00B0C"))));
            rangeSeekBarView.setCoolingLimitMaxForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(coolingUserLimitMax.getSelectedItem().toString(), "\u00B0C"))));
            rangeSeekBarView.setCoolingLimitMinForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(coolingUserLimitMin.getSelectedItem().toString(), "\u00B0C"))));
        } else {
            rangeSeekBarView.setHeatingLimitMinForced(MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMin.getSelectedItem().toString()));
            rangeSeekBarView.setHeatingLimitMaxForced(MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMax.getSelectedItem().toString()));
            rangeSeekBarView.setCoolingLimitMaxForced(MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMax.getSelectedItem().toString()));
            rangeSeekBarView.setCoolingLimitMinForced(MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMin.getSelectedItem().toString()));
        }
    }

    private void showDeleteAlert() {
        final Dialog alertDialog = new Dialog(getActivity());
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCancelable(false);
        alertDialog.setContentView(R.layout.dialog_delete_schedule);
        alertDialog.findViewById(R.id.btnCancel).setOnClickListener(view -> alertDialog.dismiss());
        alertDialog.findViewById(R.id.btnProceed).setOnClickListener(view -> {
            ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting schedule...");
            mListener.onClickSaveNamed(mPosition, 74, 72, 0, 0, 0, 0, null,
                    72.0, 67.0, 77.0, 72.0, 2.0,
                    2.0,  null);
            alertDialog.dismiss();
            new Handler().postDelayed(ProgressDialogUtils::hideProgressDialog, 1000);
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

    private void checkTemp() {
        resetRangeBarLimits();
    }

    private void checkTime(Schedule.Days mDay) {
        int startTimePosition = mDay.getSthh() * 4 + mDay.getStmm() / 15;
        CcuLog.d(L.TAG_CCU_SCHEDULE, "StartTime Position: " + startTimePosition);
        int endTimePosition = mDay.getEthh() * 4 + mDay.getEtmm() / 15;
        CcuLog.d(L.TAG_CCU_SCHEDULE, "EndTime Position: " + endTimePosition);

        npStartTime.setValue(startTimePosition);
        npEndTime.setValue(endTimePosition);
        ArrayList<String> heatingAndCoolingLimit = new ArrayList<>();
        ArrayList<String> deadBand = new ArrayList<>();

        double minDeadBandVal = ScheduleUtil.getDeadBandValue("minVal", mSchedule.getRoomRef());
        double maxDeadBandVal = ScheduleUtil.getDeadBandValue("maxVal", mSchedule.getRoomRef());

        if(isCelsiusTunerAvailableStatus()){
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add(fahrenheitToCelsius(val) + "\u00B0C");
            }

            double minVal = convertingRelativeValueFtoC(minDeadBandVal);
            if(minVal < 0.5){
                minVal = 0.5;
            }
            double maxVal = convertingRelativeValueFtoC(maxDeadBandVal);
            for (double val = minVal;  val <= maxVal; val += 0.5) {
                deadBand.add( ((val)) + "\u00B0C");
            }

        }else{
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add(val+"\u00B0F");
            }

            for (double val = minDeadBandVal;  val <= maxDeadBandVal; val += 0.5) {
                deadBand.add(val+"\u00B0F");
            }
        }

        ArrayAdapter<String> heatingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingAndCoolingLimit);
        heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingUserLimitMax.setAdapter(heatingAdapter);
        heatingUserLimitMin.setAdapter(heatingAdapter);

        ArrayAdapter<String> coolingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingAndCoolingLimit);
        coolingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingUserLimitMin.setAdapter(coolingAdapter);
        coolingUserLimitMax.setAdapter(coolingAdapter);

        ArrayAdapter<String> deadBandAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, deadBand);
        deadBandAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingDeadBand.setAdapter(deadBandAdapter);
        heatingDeadBand.setAdapter(deadBandAdapter);

            heatingUserLimitMax.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(mDay.getHeatingUserLimitMax(), true)));
            heatingUserLimitMin.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(mDay.getHeatingUserLimitMin(), true)));
            coolingUserLimitMax.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(mDay.getCoolingUserLimitMax(), true)));
            coolingUserLimitMin.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(mDay.getCoolingUserLimitMin(), true)));
            heatingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(mDay.getHeatingDeadBand(), true)));
            coolingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(mDay.getCoolingDeadBand(), true)));

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

