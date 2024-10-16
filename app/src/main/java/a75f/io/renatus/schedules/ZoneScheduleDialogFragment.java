package a75f.io.renatus.schedules;



import static a75f.io.logic.bo.util.UnitUtils.celsiusToFahrenheitTuner;
import static a75f.io.logic.bo.util.UnitUtils.convertingDeadBandValueCtoF;
import static a75f.io.logic.bo.util.UnitUtils.convertingRelativeValueFtoC;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.logic.bo.util.UnitUtils.roundToPointFive;
import static a75f.io.renatus.schedules.ScheduleUtil.SAT_OR_SUN;
import static a75f.io.renatus.schedules.ScheduleUtil.WEEK_DAY_SATURDAY_OR_SUN;
import static a75f.io.renatus.schedules.ScheduleUtil.WEEK_DAY_SIZE;
import static a75f.io.renatus.schedules.ScheduleUtil.WEEK_DAY_WEEK_END_SIZE;
import static a75f.io.renatus.schedules.ScheduleUtil.WEEK_END_SIZE;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterVal;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterValDeadBand;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.schedule.ScheduleGroup;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.TimeUtils;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;
import a75f.io.renatus.views.RangeBarView;


@SuppressLint("ValidFragment")
public class ZoneScheduleDialogFragment extends DialogFragment {

    private Schedule.Days mDay;
    private ArrayList<Schedule.Days> mDays;
    public static int NO_REPLACE = -1;
    private int mPosition;
    private Schedule mSchedule;
    Prefs prefs;
    private String addOrEdit;

    private ZoneScheduleDialogListener mListener;

    public interface ZoneScheduleDialogListener {
        boolean onClickSave(int position, double minTemp, double maxTemp, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute,
                            ArrayList<DAYS> days, Double heatingUserLimitMaxVal, Double heatingUserLimitMinVal,
                            Double coolingUserLimitMaxVal, Double coolingUserLimitMinVal, Double heatingDeadBandVal,
                            Double coolingDeadBandVal, boolean followBuilding, Schedule.Days mDay, boolean isDelete);

    }

    public ZoneScheduleDialogFragment(ZoneScheduleDialogListener mListener, Schedule schedule) {
        this.mListener = mListener;
        this.mSchedule = schedule;
        this.addOrEdit = "Add Schedule";

    }

    public ZoneScheduleDialogFragment(ZoneScheduleDialogListener mListener, Schedule schedule, Schedule.Days day) {
        this.mListener = mListener;
        this.mSchedule = schedule;
        this.mDay = day;
    }

    public ZoneScheduleDialogFragment(){
    }

    public ZoneScheduleDialogFragment(ZoneScheduleDialogListener mListener, int position, Schedule.Days day,Schedule schedule) {
        this.mPosition = position;
        this.mDay = day;
        this.mListener = mListener;
        this.mSchedule = schedule;
        this.addOrEdit = "Edit " + getLabelToEdit()+"(" + day.getSthh() +":" +day.getStmm() + " to " + mDay.getEthh() + ":"+ mDay.getEtmm() + ")";
    }

    private String getLabelToEdit() {
        if(mSchedule.getScheduleGroup() == ScheduleGroup.SEVEN_DAY.ordinal()){
            return DAYS.values()[mDay.getDay()].name();
        }
        return getLabels(mSchedule.getScheduleGroup(), mDay, mDays).get(0).toString();
    }

    public ZoneScheduleDialogFragment(ZoneScheduleDialogListener mListener, int position, ArrayList<Schedule.Days> days,Schedule schedule) {
        this.mPosition = position;
        this.mListener = mListener;
        this.mDays = days;
        this.mSchedule = schedule;
        this.addOrEdit = getLabelsForOverNightSchedule();
    }

    /*check days condatain mon and sat for weeke weekend*/
    private String getLabelsForOverNightSchedule() {
        List daysList = getLabels(mSchedule.getScheduleGroup(), mDay, mDays);
        if (daysList.size() == 1) {
            return  "Edit " + daysList.get(0) + " (" + mDays.get(0).getSthh() + ":" + mDays.get(0).getStmm() + " to " + mDays.get(0).getEthh() + ":" + mDays.get(0).getEtmm() + ")";
        } else {
            return  "Edit " + daysList + " (" + mDays.get(0).getSthh() + ":" + mDays.get(0).getStmm() + " to " + mDays.get(0).getEthh() + ":" + mDays.get(0).getEtmm() + ")";
        }
    }

    NumberPicker npStartTime;
    NumberPicker npEndTime;
    CheckBox radioButtonFirst;
    CheckBox radioButtonSecond;
    CheckBox radioButtonThird;
    CheckBox radioButtonFourth;
    CheckBox radioButtonFifth;
    CheckBox radioButtonSixth;
    CheckBox radioButtonSeventh;
    TextView addOrEditTextView;
    TextView textViewFollowBuilding;

    Button buttonSave;
    Button buttonCancel;
    RangeBarView rangeSeekBarView;
    int nMinValForStartTime = 0;
    int nMaxValForStartTime = 95;
    int nMinValForEndTime = 1;
    int nMaxValForEndTime = 96;

    Boolean isFirstRadioChipSelected = false;
    Boolean isSecondRadioChipSelected = false;
    Boolean isThirdRadioChipSelected = false;
    Boolean isFourthRadioChipSelected = false;
    Boolean isFifthRadioChipSelected = false;
    Boolean isSixthRadioChipSelected = false;
    Boolean isSeventhRadioChipSelected = false;

    Spinner heatingUserLimitMax;
    Spinner coolingUserLimitMax;
    Spinner heatingUserLimitMin;
    Spinner coolingUserLimitMin;
    Spinner heatingDeadBand;
    Spinner coolingDeadBand;

    ToggleButton followBuilding;
    HashMap<Object,Object> coolDB;
    HashMap<Object,Object> heatDB;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        prefs = new Prefs(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_zone_schedule_dialog, null);
        int daySelectionBackGround = CCUUiUtil.getDaySelectionBackground(getContext());
        ImageButton deleteButton = view.findViewById(R.id.buttonDelete);
        rangeSeekBarView = view.findViewById(R.id.rangeSeekBar);
        rangeSeekBarView.setZoneSchedule(mSchedule);
        if(!mSchedule.getMarkers().contains(Tags.FOLLOW_BUILDING)) {
            rangeSeekBarView.setmDay(mDay);
        }
        rangeSeekBarView.setEnabled(false);

        if (mDay == null && mDays == null) {
            deleteButton.setVisibility(View.INVISIBLE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
        }

        deleteButton.setOnClickListener(v -> showDeleteAlert());

        buttonSave = view.findViewById(R.id.buttonSave);
        buttonCancel = view.findViewById(R.id.buttonCancel);

        addOrEditTextView = view.findViewById(R.id.textViewAddOrEdit);
        addOrEditTextView.setText(addOrEdit);
        radioButtonFirst = view.findViewById(R.id.firstRadioChip);
        radioButtonSecond = view.findViewById(R.id.secondRadioChip);
        radioButtonThird = view.findViewById(R.id.thirdRadioChip);
        radioButtonFourth = view.findViewById(R.id.fourthRadioChip);
        radioButtonFifth = view.findViewById(R.id.fifthRadioChip);
        radioButtonSixth = view.findViewById(R.id.sixthRadioChip);
        radioButtonSeventh = view.findViewById(R.id.seventhRadioChip);
        npStartTime = view.findViewById(R.id.np1);
        npEndTime = view.findViewById(R.id.np2);
        heatingUserLimitMin = view.findViewById(R.id.heatinglimmin);
        heatingUserLimitMax = view.findViewById(R.id.heatinglimmax);
        coolingUserLimitMin = view.findViewById(R.id.coolinglimmin);
        coolingUserLimitMax = view.findViewById(R.id.coolinglimmax);
        heatingDeadBand = view.findViewById(R.id.heatingdeadband);
        coolingDeadBand = view.findViewById(R.id.coolingdeadband);
        followBuilding = view.findViewById(R.id.following_building_toggle);
        textViewFollowBuilding = view.findViewById(R.id.follow_building);
        if(mSchedule.isZoneSchedule()){
            followBuilding.setVisibility(View.VISIBLE);
            textViewFollowBuilding.setVisibility(View.VISIBLE);
        } else {
            followBuilding.setVisibility(View.GONE);
            textViewFollowBuilding.setVisibility(View.GONE);
        }
        setSpinnerDropDownIconColor();
        setUpRadioChips(mSchedule.getScheduleGroup(), getLabels(mSchedule.getScheduleGroup(), mDay, mDays));

        if (mDay != null || mDays != null){
            radioButtonFirst.setEnabled(false);
            radioButtonSecond.setEnabled(false);
            radioButtonThird.setEnabled(false);
            radioButtonFourth.setEnabled(false);
            radioButtonFifth.setEnabled(false);
            radioButtonSixth.setEnabled(false);
            radioButtonSeventh.setEnabled(false);
        }
        setSaveButtonText();

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

        double minValForDeadBand = ScheduleUtil.getDeadBandValue("minVal", mSchedule.getRoomRef());
        double maxValForDeadBand = ScheduleUtil.getDeadBandValue("maxVal", mSchedule.getRoomRef());
        if(isCelsiusTunerAvailableStatus()){
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add( fahrenheitToCelsius(val) + "\u00B0C");
            }

            double minVal = convertingRelativeValueFtoC(minValForDeadBand);
            if(minVal < 0.5){
                minVal = 0.5;
            }
            double maxVal = convertingRelativeValueFtoC(maxValForDeadBand);
            for (double val = minVal;  val <= maxVal; val += 0.5) {
                deadBand.add( ((val)) + "\u00B0C");
            }

        }else{
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add(val+"\u00B0F");
            }
            for (double val = minValForDeadBand;  val <= maxValForDeadBand; val += 0.5) {
                deadBand.add((val) + "\u00B0F");
            }
        }

        coolDB = CCUHsApi.getInstance().readEntity("point and cooling and deadband and schedulable and default");
        heatDB = CCUHsApi.getInstance().readEntity("point and heating and deadband and schedulable and default");

        ArrayAdapter<String> heatingAdapter = getAdapterValue(heatingAndCoolingLimit);
        heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingUserLimitMax.setAdapter(heatingAdapter);
        heatingUserLimitMin.setAdapter(heatingAdapter);

        ArrayAdapter<String> coolingAdapter = getAdapterValue(heatingAndCoolingLimit);
        coolingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingUserLimitMin.setAdapter(coolingAdapter);
        coolingUserLimitMax.setAdapter(coolingAdapter);

        ArrayAdapter<String> deadBandAdapter = getAdapterValue(deadBand);
        deadBandAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingDeadBand.setAdapter(deadBandAdapter);
        heatingDeadBand.setAdapter(deadBandAdapter);

        if(mSchedule.getMarkers().contains(Tags.FOLLOW_BUILDING)){
            heatingUserLimitMax.setEnabled(false);
            heatingUserLimitMin.setEnabled(false);
            coolingUserLimitMax.setEnabled(false);
            coolingUserLimitMin.setEnabled(false);
            heatingDeadBand.setEnabled(false);
            coolingDeadBand.setEnabled(false);
        }else{
            followBuilding.setChecked(false);
            heatingUserLimitMax.setEnabled(true);
            heatingUserLimitMin.setEnabled(true);
            coolingUserLimitMax.setEnabled(true);
            coolingUserLimitMin.setEnabled(true);
            heatingDeadBand.setEnabled(true);
            coolingDeadBand.setEnabled(true);
        }
        setDefaultUserLimits(heatingAdapter, coolingAdapter, deadBandAdapter);
        followBuilding.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(followBuilding.isChecked()){

                    ArrayList<Schedule.Days> dayList = mSchedule.getDays();
                    ArrayList<Integer> dayIndexArr = new ArrayList<>();

                    //collecting the building level heating and cooling desired temperatures
                    double heatingLimitMax =CCUHsApi.getInstance().readPointPriorityValByQuery("limit and default and max and heating and schedulable");
                    double heatingLimitMin =CCUHsApi.getInstance().readPointPriorityValByQuery("limit and default and min and heating and schedulable");
                    double coolingLimitMax =CCUHsApi.getInstance().readPointPriorityValByQuery("limit and default and max and cooling and schedulable");
                    double coolingLimitMin =CCUHsApi.getInstance().readPointPriorityValByQuery("limit and default and min and cooling and schedulable");


                    for(int i= 0; i < dayList.size(); i++) {
                        Schedule.Days tempDay = dayList.get(i);
                        double hdt = tempDay.getHeatingVal();
                        double cdt = tempDay.getCoolingVal();
                        if (!(hdt>=heatingLimitMin && hdt<=heatingLimitMax) || !(cdt>=coolingLimitMin && cdt<=coolingLimitMax)) {
                            dayIndexArr.add(tempDay.getDay());
                        }
                    }
                    if(!dayIndexArr.isEmpty()) {
                        new AlertDialog.Builder(getContext())
                                .setCancelable(false)
                                .setTitle("Follow Building\n")
                                .setIcon(R.drawable.ic_dialog_alert)
                                .setMessage("The desired temperature range for the following day(s) is not within the range of the building.\n\n " +displayDayName(dayIndexArr)+"\n\n"+"Building Heating limits -("+heatingLimitMin+" - "+heatingLimitMax+")"+"\n" +"Building Cooling limits-("+coolingLimitMin+" - "+coolingLimitMax+")" +"\n" + "\n"+"Please edit the Desired Temperatures for Heating and Cooling for the day(s) mentioned above.")
                                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> followBuilding.setChecked(false))
                                .show();
                        dayIndexArr.clear();
                        return;
                    }

                heatingUserLimitMin.setSelection(heatingAdapter.getPosition(
                        getAdapterVal(Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal(), true)));
                heatingUserLimitMax.setSelection(heatingAdapter.getPosition(
                        getAdapterVal(Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal(), true)));
                coolingUserLimitMin.setSelection(coolingAdapter.getPosition(
                        getAdapterVal(Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal(), true)));
                coolingUserLimitMax.setSelection(coolingAdapter.getPosition(
                        getAdapterVal(Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal(), true)));
                heatingDeadBand.setSelection(deadBandAdapter.getPosition(
                        getAdapterValDeadBand(HSUtil.getLevelValueFrom16(heatDB.get("id").toString()), true)));
                coolingDeadBand.setSelection(deadBandAdapter.getPosition(
                        getAdapterValDeadBand(HSUtil.getLevelValueFrom16(coolDB.get("id").toString()), true)));
                resetRangeBarLimits();
                heatingUserLimitMax.setEnabled(false);
                heatingUserLimitMin.setEnabled(false);
                coolingUserLimitMax.setEnabled(false);
                coolingUserLimitMin.setEnabled(false);
                heatingDeadBand.setEnabled(false);
                coolingDeadBand.setEnabled(false);
            }else {
                heatingUserLimitMax.setEnabled(true);
                heatingUserLimitMin.setEnabled(true);
                coolingUserLimitMax.setEnabled(true);
                coolingUserLimitMin.setEnabled(true);
                heatingDeadBand.setEnabled(true);
                coolingDeadBand.setEnabled(true);
                if(mDay != null) {
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
                }else{
                    setDefaultUserLimits(heatingAdapter, coolingAdapter, deadBandAdapter);
                }
                resetRangeBarLimits();
            }
        });

        try {
            Method method = npEndTime.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npEndTime, true);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_CRASH, "Reflection Crash?");
        }
        radioButtonFirst.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isFirstRadioChipSelected = isChecked;
            radioButtonFirst.setChecked(isChecked);
        });
        radioButtonSecond.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSecondRadioChipSelected = isChecked;
            radioButtonSecond.setChecked(isChecked);
        });
        radioButtonThird.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isThirdRadioChipSelected = isChecked;
            radioButtonThird.setChecked(isChecked);
        });
        radioButtonFourth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isFourthRadioChipSelected = isChecked;
            radioButtonFourth.setChecked(isChecked);
        });
        radioButtonFifth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isFifthRadioChipSelected = isChecked;
            radioButtonFifth.setChecked(isChecked);
        });
        radioButtonSixth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSixthRadioChipSelected = isChecked;
            radioButtonSixth.setChecked(isChecked);
        });
        radioButtonSeventh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSeventhRadioChipSelected = isChecked;
            radioButtonSeventh.setChecked(isChecked);
        });

        buttonSave.setOnClickListener(view1 ->
        {
            ArrayList<DAYS> days = getDaysByScheduleGroupAndCheckBoxSelected();

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
                Toast.makeText(ZoneScheduleDialogFragment.this.getContext(), "Start time and End time cannot be the same", Toast.LENGTH_SHORT).show();
                return;
            }

            if ((startHour > endHour || (startHour == endHour && startMinutes > endMinutes))
                    && mSchedule.getScheduleGroup() != ScheduleGroup.SEVEN_DAY.ordinal()) {
                Toast.makeText(ZoneScheduleDialogFragment.this.getContext(),
                        "Overnight schedule creation is only permitted for 7 day schedule group", Toast.LENGTH_LONG).show();
                return;
            }

            if (days.isEmpty()) {
                Toast.makeText(ZoneScheduleDialogFragment.this.getContext(), "Select one or more days to apply the schedule", Toast.LENGTH_SHORT).show();
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
                            mListener.onClickSave((mDay == null && (mDays == null ||(mDays!= null && mDays.size() == 0))) ? NO_REPLACE : mPosition, rangeSeekBarView.getCoolValue(),
                                    rangeSeekBarView.getHeatValue(), startHour, endHour, startMinutes,
                                    endMinutes, days, heatingUserLimitMaxVal, heatingUserLimitMinVal, coolingUserLimitMaxVal,
                                    coolingUserLimitMinVal, heatingDeadBandVal, coolingDeadBandVal,
                                    followBuilding.isChecked(), mDay, false);

                            dismiss();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                return;
            }

            double buildingLimitMax = Domain.buildingEquip.getBuildingLimitMax().readPriorityVal();
            double buildingLimitMin =  Domain.buildingEquip.getBuildingLimitMin().readPriorityVal();
            double unoccupiedZoneSetback = Domain.buildingEquip.getUnoccupiedZoneSetback().readPriorityVal();
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
                if (!mListener.onClickSave((mDay == null && (mDays == null || (mDays != null && mDays.size() == 0))) ? NO_REPLACE : mPosition, rangeSeekBarView.getCoolValue(),
                        rangeSeekBarView.getHeatValue(), startHour, endHour, startMinutes, endMinutes,
                        days, heatingUserLimitMaxVal, heatingUserLimitMinVal, coolingUserLimitMaxVal,
                        coolingUserLimitMinVal, heatingDeadBandVal, coolingDeadBandVal,
                        followBuilding.isChecked(), mDay, false)) {
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
                    rangeSeekBarView.setHeatingDeadBand(roundToPointFive(convertingDeadBandValueCtoF(Double.parseDouble(StringUtils.substringBefore(heatingDeadBand.getSelectedItem().toString(), "\u00B0C")))));
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
                    rangeSeekBarView.setCoolingDeadBand(roundToPointFive(convertingDeadBandValueCtoF(Double.parseDouble(StringUtils.substringBefore(coolingDeadBand.getSelectedItem().toString(), "\u00B0C")))));
                } else {
                    rangeSeekBarView.setCoolingDeadBand(MasterControlUtil.getAdapterFarhenheitVal(coolingDeadBand.getSelectedItem().toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        buttonCancel.setOnClickListener(view12 ->{
            dismiss();
        });


        if (mDays != null && (!mDays.isEmpty())) {
            checkDays(mDays, null, mSchedule.getScheduleGroup());
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

            checkDays(null, mDay, mSchedule.getScheduleGroup());
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

    private void setSaveButtonText() {
        if(mDay != null || mDays != null){
            buttonSave.setText("SAVE");
        } else {
            buttonSave.setText("ADD");
        }
    }

    private ArrayList<DAYS> getDaysByScheduleGroupAndCheckBoxSelected() {
        ArrayList<DAYS> days = new ArrayList<>();
        int scheduleGroup = mSchedule.getScheduleGroup();
        if (scheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
            if(isFirstRadioChipSelected) {
                days.add(DAYS.MONDAY);
                days.add(DAYS.TUESDAY);
                days.add(DAYS.WEDNESDAY);
                days.add(DAYS.THURSDAY);
                days.add(DAYS.FRIDAY);
                days.add(DAYS.SATURDAY);
                days.add(DAYS.SUNDAY);
            }
        } else if(scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()){

            if(isFirstRadioChipSelected) {
                // when re-edit happens then days are added in mDays
                if(mDays != null && mDays.size() > 0) {
                    for(Schedule.Days day : mDays) {
                        days.add(DAYS.values()[day.getDay()]);
                    }
                } else if(mDay == null || mDay.getDay() == 0) {
                    days.add(DAYS.MONDAY);
                    days.add(DAYS.TUESDAY);
                    days.add(DAYS.WEDNESDAY);
                    days.add(DAYS.THURSDAY);
                    days.add(DAYS.FRIDAY);
                } else {
                    days.add(DAYS.SATURDAY);
                    days.add(DAYS.SUNDAY);
                }
            }
            if(isSecondRadioChipSelected) {
                days.add(DAYS.SATURDAY);
                days.add(DAYS.SUNDAY);
            }
        } else if (scheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()) {
            if(isFirstRadioChipSelected) {
                if(mDays != null && mDays.size() > 0) {
                    for(Schedule.Days day : mDays) {
                        days.add(DAYS.values()[day.getDay()]);
                    }
                } else if (mDay == null || mDay.getDay() == 0) {
                    days.add(DAYS.MONDAY);
                    days.add(DAYS.TUESDAY);
                    days.add(DAYS.WEDNESDAY);
                    days.add(DAYS.THURSDAY);
                    days.add(DAYS.FRIDAY);
                } else if (mDay.getDay() == 5) {
                    days.add(DAYS.SATURDAY);
                } else {
                    days.add(DAYS.SUNDAY);
                }
            }
            if(isSecondRadioChipSelected) {
                days.add(DAYS.SATURDAY);
            }
            if(isThirdRadioChipSelected) {
                days.add(DAYS.SUNDAY);
            }
        } else {
            if(isFirstRadioChipSelected) {
                if(mDays == null || mDays.contains(DAYS.MONDAY)) {
                    days.add(DAYS.MONDAY);
                } else {
                    days.add(DAYS.values()[mDays.get(0).getDay()]);
                }
            }
            if(isSecondRadioChipSelected) {
                if(mDays == null || mDays.contains(DAYS.TUESDAY)) {
                    days.add(DAYS.TUESDAY);
                } else {
                    days.add(DAYS.values()[mDays.get(1).getDay()]);
                }
            }
            if(isThirdRadioChipSelected) {
                if(mDays == null || mDays.contains(DAYS.WEDNESDAY)) {
                    days.add(DAYS.WEDNESDAY);
                } else {
                    days.add(DAYS.values()[mDays.get(2).getDay()]);
                }
            }
            if(isFourthRadioChipSelected) {
                if(mDays == null || mDays.contains(DAYS.THURSDAY)) {
                    days.add(DAYS.THURSDAY);
                } else {
                    days.add(DAYS.values()[mDays.get(3).getDay()]);
                }
            }
            if(isFifthRadioChipSelected) {
                if(mDays == null || mDays.contains(DAYS.FRIDAY)) {
                    days.add(DAYS.FRIDAY);
                } else {
                    days.add(DAYS.values()[mDays.get(4).getDay()]);
                }
            }
            if(isSixthRadioChipSelected) {
                if(mDays == null || mDays.contains(DAYS.SATURDAY)) {
                    days.add(DAYS.SATURDAY);
                } else {
                    days.add(DAYS.values()[mDays.get(5).getDay()]);
                }
            }
            if(isSeventhRadioChipSelected) {
                if(mDays == null || mDays.contains(DAYS.SUNDAY)) {
                    days.add(DAYS.SUNDAY);
                } else {
                    days.add(DAYS.values()[mDays.get(6).getDay()]);
                }
            }
        }
        return days;
    }

    public List<String> getLabels(int scheduleGroup, Schedule.Days mDay, ArrayList<Schedule.Days> mDays) {
        List<String> labels = new ArrayList<>();
        List<String> abbreviatedDayNames = Arrays.asList("M", "T", "W", "Th", "F", "Sa", "Su");

        if (scheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
            labels.add("Everyday");
            return labels;
        } else if (scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()) {
            if (mDay != null) {
                labels.add(mDay.getDay() == DAYS.SATURDAY.ordinal() || mDay.getDay() ==
                        DAYS.SUNDAY.ordinal() ? "Weekend" : "Weekday");
            } else if(mDays != null){
                if(mDays.size() == WEEK_DAY_SIZE){
                    labels.add("Weekday");
                } else if (mDays.size() == WEEK_END_SIZE) {
                    labels.add("Weekend");
                }else if (mDays.size() == WEEK_DAY_WEEK_END_SIZE) {
                    labels.add("Weekday");
                    labels.add("Weekend");
                }
            } else{
                labels.add("Weekday");
                labels.add("Weekend");
            }
            return labels;
        } else if (scheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()) {
            if (mDay != null) {
                switch (mDay.getDay()) {
                    case 5:
                        labels.add("Saturday");
                        break;
                    case 6:
                        labels.add("Sunday");
                        break;
                    default:
                        labels.add("Weekday");
                        break;
                }
            } else if(mDays != null){
                if(mDays.size() == WEEK_DAY_SIZE){
                    labels.add("Weekday");
                } else if (mDays.size() == WEEK_END_SIZE) {
                    labels.add("Saturday");
                    labels.add("Sunday");
                } else if(mDays.size() == SAT_OR_SUN){
                    if(mDays.get(0).getDay() == DAYS.SATURDAY.ordinal()){
                        labels.add("Saturday");
                    } else if(mDays.get(0).getDay() == DAYS.SUNDAY.ordinal()) {
                        labels.add("Sunday");
                    }
                }
            } else {
                labels.add("Weekday");
                labels.add("SA");
                labels.add("SU");
            }
            return labels;
        } else {
            if (mDay != null) {
                labels.add(abbreviatedDayNames.get(mDay.getDay()));
            } else if (mDays != null) {
                for (Schedule.Days day : mDays) {
                    labels.add(abbreviatedDayNames.get(day.getDay()));
                }
            }else {
                labels.add("M");
                labels.add("T");
                labels.add("W");
                labels.add("TH");
                labels.add("F");
                labels.add("SA");
                labels.add("SU");
                return labels;
            }
            return labels;
        }

    }


    private void setUpRadioChips(Integer scheduleGroup, List<String> labels) {
        List<CheckBox> radioButtons = new ArrayList<>();
        Collections.addAll(radioButtons, radioButtonFirst, radioButtonSecond,
                radioButtonThird, radioButtonFourth, radioButtonFifth, radioButtonSixth, radioButtonSeventh);
        for (int i = 0; i < radioButtons.size(); i++) {
            CheckBox radioButton = radioButtons.get(i);
            if (i < labels.size()) {
                radioButton.setText(labels.get(i));
                radioButton.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams layoutParams = radioButton.getLayoutParams();

                if (scheduleGroup == ScheduleGroup.SEVEN_DAY.ordinal()) {
                    layoutParams.width = 50;
                } else if (scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal() || scheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
                    layoutParams.width = 129;
                } else {
                    layoutParams.width = (i == 1 || i == 2) ? 50 : 129;
                }

                radioButton.setLayoutParams(layoutParams);
            } else {
                radioButton.setVisibility(View.GONE);
            }
        }


    }


    private String displayDayName(ArrayList<Integer> dayIndexArr) {
        StringBuilder daysName = new StringBuilder();

        for(int i=0; i<dayIndexArr.size(); i++){
            daysName.append(getDayName(dayIndexArr.get(i))).append("\n");
        }
        return daysName.toString();
    }

    private String getDayName(int index) {
        ArrayList<Schedule.Days> daysList = mSchedule.getDays();
        Schedule.Days day;

        switch (index){
            case 0:
                day = daysList.get(0);
                return "Monday - ("+ day.getHeatingVal() + " - " + day.getCoolingVal() + ")";
            case 1:
                day=daysList.get(1);
                return "Tuesday - ("+ day.getHeatingVal() + " - " + day.getCoolingVal() + ")";
            case 2:
                day=daysList.get(2);
                return "Wednesday - ("+ day.getHeatingVal() + " - " + day.getCoolingVal() + ")";
            case 3:
                day=daysList.get(3);
                return "Thursday - ("+ day.getHeatingVal() + " - " + day.getCoolingVal() + ")";
            case 4:
                day=daysList.get(4);
                return "Friday - ("+ day.getHeatingVal() + " - " + day.getCoolingVal() + ")";
            case 5:
                day=daysList.get(5);
                return "Saturday - ("+ day.getHeatingVal() + " - " + day.getCoolingVal() + ")";
            case 6:
                day=daysList.get(6);
                return "Sunday - ("+ day.getHeatingVal() + " - " + day.getCoolingVal() + ")";
        }
        return null;
    }


    private void setDefaultUserLimits(ArrayAdapter<String> heatingAdapter, ArrayAdapter<String> coolingAdapter, ArrayAdapter<String> deadBandAdapter) {
        if(followBuilding.isChecked()){
            heatingUserLimitMin.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal(), true)));
            heatingUserLimitMax.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal(), true)));
            coolingUserLimitMin.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal(), true)));
            coolingUserLimitMax.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal(), true)));
            heatingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(HSUtil.getLevelValueFrom16(heatDB.get("id").toString()), true)));
            coolingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(HSUtil.getLevelValueFrom16(coolDB.get("id").toString()), true)));
        } else {
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
    }

    private void resetRangeBarLimits() {
        String heatingUserLimitMinItem =  (heatingUserLimitMin.getSelectedItem() == null) ?
                heatingUserLimitMin.getItemAtPosition(heatingUserLimitMin.getCount() -1).toString() :
                heatingUserLimitMin.getSelectedItem().toString();
        String heatingUserLimitMaxItem = (heatingUserLimitMax.getSelectedItem() == null) ?
                heatingUserLimitMax.getItemAtPosition(heatingUserLimitMax.getCount() -1).toString() :
                heatingUserLimitMax.getSelectedItem().toString();
        String coolingUserLimitMinItem = (coolingUserLimitMin.getSelectedItem() == null) ?
                coolingUserLimitMin.getItemAtPosition(coolingUserLimitMin.getCount() -1).toString() :
                coolingUserLimitMin.getSelectedItem().toString();
        String coolingUserLimitMaxItem = (coolingUserLimitMax.getSelectedItem() == null) ?
                coolingUserLimitMax.getItemAtPosition(coolingUserLimitMax.getCount() -1).toString() :
                coolingUserLimitMax.getSelectedItem().toString();

        if (isCelsiusTunerAvailableStatus()) {
            rangeSeekBarView.setHeatingLimitMinForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(heatingUserLimitMinItem, "\u00B0C"))));
            rangeSeekBarView.setHeatingLimitMaxForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(heatingUserLimitMaxItem, "\u00B0C"))));
            rangeSeekBarView.setCoolingLimitMaxForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(coolingUserLimitMaxItem, "\u00B0C"))));
            rangeSeekBarView.setCoolingLimitMinForced(celsiusToFahrenheitTuner(Double.parseDouble(
                    StringUtils.substringBefore(coolingUserLimitMinItem, "\u00B0C"))));
        } else {
            rangeSeekBarView.setHeatingLimitMinForced(MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMinItem));
            rangeSeekBarView.setHeatingLimitMaxForced(MasterControlUtil.getAdapterFarhenheitVal(heatingUserLimitMaxItem));
            rangeSeekBarView.setCoolingLimitMaxForced(MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMaxItem));
            rangeSeekBarView.setCoolingLimitMinForced(MasterControlUtil.getAdapterFarhenheitVal(coolingUserLimitMinItem));
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
            mListener.onClickSave(mPosition, 74, 72, 0, 0, 0, 0, null,
                    72.0, 67.0, 77.0, 72.0, 2.0,
                    2.0, followBuilding.isChecked(), null, true);
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

        double minDeadBandValue = ScheduleUtil.getDeadBandValue("minVal", mSchedule.getRoomRef());
        double maxDeadBandValue = ScheduleUtil.getDeadBandValue("maxVal", mSchedule.getRoomRef());
        if(isCelsiusTunerAvailableStatus()){
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add(fahrenheitToCelsius(val) + "\u00B0C");
            }

            double minVal = convertingRelativeValueFtoC(minDeadBandValue);
            if(minVal < 0.5){
                minVal = 0.5;
            }
            double maxVal = convertingRelativeValueFtoC(maxDeadBandValue);
            for (double val = minVal;  val <= maxVal; val += 0.5) {
                deadBand.add( ((val)) + "\u00B0C");
            }

        }else{
            for (int val = 50;  val <= 100; val += 1) {
                heatingAndCoolingLimit.add(val+"\u00B0F");
            }

            for (double val = minDeadBandValue;  val <= maxDeadBandValue; val += 0.5) {
                deadBand.add(val+"\u00B0F");
            }
        }

        ArrayAdapter<String> heatingAdapter = getAdapterValue(heatingAndCoolingLimit);
        heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingUserLimitMax.setAdapter(heatingAdapter);
        heatingUserLimitMin.setAdapter(heatingAdapter);

        ArrayAdapter<String> coolingAdapter = getAdapterValue(heatingAndCoolingLimit);
        coolingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingUserLimitMin.setAdapter(coolingAdapter);
        coolingUserLimitMax.setAdapter(coolingAdapter);

        ArrayAdapter<String> deadBandAdapter = getAdapterValue(deadBand);
        deadBandAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingDeadBand.setAdapter(deadBandAdapter);
        heatingDeadBand.setAdapter(deadBandAdapter);
        if(mSchedule.getMarkers().contains(Tags.FOLLOW_BUILDING)){
            heatingUserLimitMin.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal(), true)));
            heatingUserLimitMax.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal(), true)));
            coolingUserLimitMin.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal(), true)));
            coolingUserLimitMax.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal(), true)));
            heatingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(HSUtil.getLevelValueFrom16(heatDB.get("id").toString()), true)));
            coolingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(HSUtil.getLevelValueFrom16(coolDB.get("id").toString()), true)));
        }else{
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
    }
    private void checkDays(ArrayList<Schedule.Days> mDays, Schedule.Days days, int scheduleGroup) {
        List<CheckBox> radioButtons = Arrays.asList(
                radioButtonFirst,
                radioButtonSecond,
                radioButtonThird,
                radioButtonFourth,
                radioButtonFifth,
                radioButtonSixth,
                radioButtonSeventh
        );
        if(mDay!= null) {
            radioButtonFirst.setChecked(true);
        } else {
            if(scheduleGroup == ScheduleGroup.SEVEN_DAY.ordinal()) {
                for (int i = 0; i < mDays.size() && i < radioButtons.size(); i++) {
                    radioButtons.get(i).setChecked(true);
                }
            } else if (scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()) {
                if(mDays.size() == WEEK_DAY_SIZE || mDays.size() == WEEK_END_SIZE) {
                    radioButtonFirst.setChecked(true);
                } else if (mDays.size() == WEEK_DAY_WEEK_END_SIZE) {
                    radioButtonFirst.setChecked(true);
                    radioButtonSecond.setChecked(true);
                }
            } else if (scheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()) {
                if(mDays.size() == WEEK_DAY_SIZE || mDays.size() == SAT_OR_SUN) {
                    radioButtonFirst.setChecked(true);
                } else if (mDays.size() == WEEK_END_SIZE) {
                    radioButtonFirst.setChecked(true);
                    radioButtonSecond.setChecked(true);
                }  else if (mDays.size() == WEEK_DAY_SATURDAY_OR_SUN) {
                    radioButtonFirst.setChecked(true);
                    radioButtonSecond.setChecked(true);
                }else if (mDays.size() == WEEK_DAY_WEEK_END_SIZE) {
                    radioButtonFirst.setChecked(true);
                    radioButtonSecond.setChecked(true);
                    radioButtonThird.setChecked(true);
                }
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(1165, 646);
        }
    }

    private void setSpinnerDropDownIconColor() {
        CCUUiUtil.setSpinnerDropDownColor(coolingUserLimitMin, this.getContext());
        CCUUiUtil.setSpinnerDropDownColor(coolingUserLimitMax, this.getContext());
        CCUUiUtil.setSpinnerDropDownColor(coolingDeadBand, this.getContext());
        CCUUiUtil.setSpinnerDropDownColor(heatingUserLimitMax, this.getContext());
        CCUUiUtil.setSpinnerDropDownColor(heatingUserLimitMin, this.getContext());
        CCUUiUtil.setSpinnerDropDownColor(heatingDeadBand, this.getContext());
    }
    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        return new CustomSpinnerDropDownAdapter(requireContext(), R.layout.spinner_dropdown_item, values);
    }
}
