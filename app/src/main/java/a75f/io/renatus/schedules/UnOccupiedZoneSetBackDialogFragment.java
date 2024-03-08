package a75f.io.renatus.schedules;



import static a75f.io.logic.bo.util.UnitUtils.convertingRelativeValueCtoF;
import static a75f.io.logic.bo.util.UnitUtils.convertingRelativeValueFtoC;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.BuildingEquip;
import a75f.io.domain.api.Domain;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.renatus.views.RangeBarView;


@SuppressLint("ValidFragment")
public class UnOccupiedZoneSetBackDialogFragment extends DialogFragment {

    private Schedule.Days mDay;
    private Schedule mSchedule;
    Spinner unOccupiedZoneSetBack;
    RangeBarView rangeSeekBarView;
    Button buttonSave;
    Button buttonCancel;
    ZoneScheduleViewModel zoneScheduleViewModel;

    private UnOccupiedZoneSetBackListener mListener;
    public interface UnOccupiedZoneSetBackListener {
        void onClickSaveSchedule(double unOccupiedZoneSetback, Schedule schedule);

        void onClickCancelSaveSchedule(String scheduleId);
    }

    public UnOccupiedZoneSetBackDialogFragment() {
    }

    public UnOccupiedZoneSetBackDialogFragment(UnOccupiedZoneSetBackListener mListener, Schedule.Days day, Schedule schedule) {
        this.mDay = day;
        this.mListener = mListener;
        this.mSchedule = schedule;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_un_occupied_zone_set_back, null);
        unOccupiedZoneSetBack = view.findViewById(R.id.unoccupiedzonesetback);
        rangeSeekBarView = view.findViewById(R.id.rangeSeekBar);
        rangeSeekBarView.setZoneSchedule(mSchedule);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonCancel = view.findViewById(R.id.buttonCancel);
        Double buildingToZoneDiff = Domain.buildingEquip.getBuildingToZoneDifferential().readPriorityVal();
        Double buildingLimitMax =  Domain.buildingEquip.getBuildingLimitMax().readPriorityVal();
        Double buildingLimitMin =  Domain.buildingEquip.getBuildingLimitMin().readPriorityVal();

        CCUUiUtil.setSpinnerDropDownColor(unOccupiedZoneSetBack,this.getContext());
        zoneScheduleViewModel = new ZoneScheduleViewModel();
        buttonSave.setOnClickListener(v ->{
            StringBuilder heatingLimitWarning = new StringBuilder();
            StringBuilder coolingLimitWarning = new StringBuilder();
            for(int i = 0; i < mSchedule.getDays().size(); i++) {
                heatingLimitWarning.append(zoneScheduleViewModel.validateUnOccupiedZoneSetBack(mSchedule.getDays().get(i).getHeatingUserLimitMin(),
                         unOccupiedZoneSetBack.getSelectedItemPosition(), buildingToZoneDiff,
                        buildingLimitMin, buildingLimitMax, mSchedule.getDays().get(i)));
            }
            if(heatingLimitWarning.length() == 0) {
                for (int i = 0; i < mSchedule.getDays().size(); i++) {
                    coolingLimitWarning.append(zoneScheduleViewModel.validateUnOccupiedZoneSetBackCooling(
                            mSchedule.getDays().get(i).getCoolingUserLimitMax(), unOccupiedZoneSetBack.getSelectedItemPosition(),
                            buildingToZoneDiff, buildingLimitMin, buildingLimitMax, mSchedule.getDays().get(i)));
                }
            }
            if(heatingLimitWarning.length() == 0 && coolingLimitWarning.length() == 0) {
                double unOccupiedSetBackVal = unOccupiedZoneSetBack.getSelectedItemPosition();
                if(isCelsiusTunerAvailableStatus()){
                    unOccupiedSetBackVal = convertingRelativeValueCtoF(unOccupiedZoneSetBack.getSelectedItemPosition());
                }
                mListener.onClickSaveSchedule(Math.round(unOccupiedSetBackVal), mSchedule);
                dismiss();
            }else {
                android.app.AlertDialog.Builder builder =
                        new android.app.AlertDialog.Builder(getActivity());

                if(heatingLimitWarning.length() > 0) {
                    builder.setMessage("Building Limit Min violated:" + "\n\n" + heatingLimitWarning.toString() +
                            "\n" + "please go back and edit the Heating Limit Min temperature / Unoccupied Zone Setback" +
                            " to be within the temperature limits of the building or adjust the temperature limits of the building to accommodate" +
                            " the required Heating Limit Min temperature / Unoccupied Zone Setback.");
                }else {
                    builder.setMessage("Building Limit Max violated:" + "\n\n" + coolingLimitWarning.toString() +
                            "\n" + "please go back and edit the Cooling Limit Max temperature / Unoccupied Zone Setback" +
                            " to be within the temperature limits of the building or adjust the temperature limits of the building to accommodate" +
                            " the required Cooling Limit Max temperature / Unoccupied Zone Setback.");
                }
                builder.setCancelable(false);
                builder.setTitle(R.string.warning_ns);
                builder.setIcon(R.drawable.ic_alert);
                builder.setNegativeButton("OKAY", (dialog1, id) -> {
                    dialog1.dismiss();
                });
                android.app.AlertDialog alert = builder.create();
                alert.show();
            }
        });
        buttonCancel.setOnClickListener(v -> {
            mListener.onClickCancelSaveSchedule("a");
            dismiss();
        });

        ArrayList<Double> zoneSetBack = new ArrayList<>();
        if(isCelsiusTunerAvailableStatus()) {
         double  minValue = convertingRelativeValueFtoC(0);
         double  maxValue = convertingRelativeValueFtoC(20);

                for (double val = minValue;  val <= maxValue; val += 1) {
                    zoneSetBack.add(val);
                }
        }else {
            for (double val = 0; val <= 20; val += 1) {
                zoneSetBack.add(val);
            }
        }
        ArrayAdapter<Double> setBackAdapter = getAdapterValue(zoneSetBack);
        setBackAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        unOccupiedZoneSetBack.setAdapter(setBackAdapter);
        if(mSchedule.getMarkers().contains(Tags.FOLLOW_BUILDING)){
            int unoccupiedZoneSetBackDefaultValue = (int)Domain.buildingEquip.getUnoccupiedZoneSetback().readPriorityVal();
            if(isCelsiusTunerAvailableStatus())
                unoccupiedZoneSetBackDefaultValue = (int) Math.round(convertingRelativeValueFtoC(unoccupiedZoneSetBackDefaultValue));
            unOccupiedZoneSetBack.setSelection(unoccupiedZoneSetBackDefaultValue);
            unOccupiedZoneSetBack.setEnabled(false);
        }else {
            HashMap<Object, Object> unoccupiedZoneObj = CCUHsApi.getInstance().readEntity("unoccupied and zone and setback and schedulable and roomRef == \"" + mSchedule.getRoomRef() + "\"");
            int unoccupiedZoneVal = (int) (CCUHsApi.getInstance().readPointPriorityVal(unoccupiedZoneObj.get("id").toString()));
            if(isCelsiusTunerAvailableStatus())
                unoccupiedZoneVal =(int) Math.round(convertingRelativeValueFtoC(unoccupiedZoneVal));
            unOccupiedZoneSetBack.setSelection(unoccupiedZoneVal);
        }
        unOccupiedZoneSetBack.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                checkTemp(mDay);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        if (mDay != null) {
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

        return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onStart () {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(1165, 500);
        }
    }

    private void checkTemp(Schedule.Days mDay) {
        if (mDay.getCoolingVal() != null) {
            rangeSeekBarView.setLowerCoolingTemp(mDay.getCoolingVal() + (int) Double.parseDouble(unOccupiedZoneSetBack.getSelectedItem().toString()));
        }
        if (mDay.getHeatingVal() != null) {
            rangeSeekBarView.setLowerHeatingTemp(mDay.getHeatingVal() - ((int) Double.parseDouble(unOccupiedZoneSetBack.getSelectedItem().toString())));
        }
        if(isCelsiusTunerAvailableStatus()) {
            rangeSeekBarView.setUnOccupiedSetBack((int) Double.parseDouble(unOccupiedZoneSetBack.getSelectedItem().toString()));
        }
        rangeSeekBarView.setUnOccupiedFragment(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        rangeSeekBarView.setUnOccupiedFragment(false);
    }
    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        return new CustomSpinnerDropDownAdapter(requireContext(), R.layout.spinner_dropdown_item, values);
    }
}

