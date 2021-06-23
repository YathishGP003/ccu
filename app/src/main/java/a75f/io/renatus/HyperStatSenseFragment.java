package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;

import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseEquip;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseProfile;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.objectbox.android.AndroidScheduler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class HyperStatSenseFragment extends BaseDialogFragment {

    public static final String ID = HyperStatSenseFragment.class.getSimpleName();
    private  HyperStatSenseVM mHyperStatSenseVM;
    static final int TEMP_OFFSET_LIMIT = 100;

    short        mNodeAddress;
    String       mRoomName;
    String       mFloorName;
    Boolean      misPaired;
    String       mProfileName;

    @BindView(R.id.temperatureOffset)
    NumberPicker mTemperatureOffset;

    @BindView(R.id.th1)
    ToggleButton mTherm1toggle;

    @BindView(R.id.th2)
    ToggleButton mTherm2toggle;

    @BindView(R.id.anlg1)
    ToggleButton mAnalog1toggle;

    @BindView(R.id.anlg2)
    ToggleButton mAnalog2toggle;

    @BindView(R.id.th1select)
    Spinner mThermostat1Sp;

    @BindView(R.id.th2select)
    Spinner mThermostat2Sp;

    @BindView(R.id.analog1select)
    Spinner mAnalog1Sp;

    @BindView(R.id.analog2select)
    Spinner mAnalog2Sp;

    @BindView(R.id.setBtn)
    Button mSetbtn;


    @Override
    public String getIdString() {
        return ID;
    }


    public static HyperStatSenseFragment newInstance(short meshAddress, String roomName, String floorName, ProfileType profileType){
        HyperStatSenseFragment f = new HyperStatSenseFragment();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.name() );
        f.setArguments(bundle);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hssense_config, container, false);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mProfileName = getArguments().getString(FragmentCommonBundleArgs.PROFILE_TYPE);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHyperStatSenseVM = new ViewModelProvider(this).get(HyperStatSenseVM.class);
        mHyperStatSenseVM.init();
        mHyperStatSenseVM.get().observe(this, new Observer<HyperStatSenseModel>() {
            @Override
            public void onChanged(HyperStatSenseModel model) {
                   //TODO
            }
        });
        setSpinnerListItem();

        /** Setting temperature offset limit */
        mTemperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        mTemperatureOffset.setDisplayedValues(nums);
        mTemperatureOffset.setMinValue(0);
        mTemperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        mTemperatureOffset.setValue(TEMP_OFFSET_LIMIT);

        /** Spinner id disabled by default */
        mThermostat1Sp.setEnabled(false);
        mThermostat2Sp.setEnabled(false);
        mAnalog1Sp.setEnabled(false);
        mAnalog2Sp.setEnabled(false);

        mTherm1toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mThermostat1Sp.setEnabled(true);
                else mThermostat1Sp.setEnabled(false);
            }
        });

        mTherm1toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mThermostat1Sp.setEnabled(true);
                else mThermostat1Sp.setEnabled(false);
            }
        });

        mTherm2toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mThermostat2Sp.setEnabled(true);
                else mThermostat2Sp.setEnabled(false);
            }
        });

        mAnalog1toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mAnalog1Sp.setEnabled(true);
                else mAnalog1Sp.setEnabled(false);
            }
        });

        mAnalog2toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mAnalog2Sp.setEnabled(true);
                else mAnalog2Sp.setEnabled(false);
            }
        });

        mSetbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }



    private void setSpinnerListItem() {
        ArrayList<String> analogArr = new ArrayList<>();
        for (Sensor r : SensorManager.getInstance().getExternalSensorList()) {
            analogArr.add(r.sensorName+" "+r.engineeringUnit);
        }
        ArrayList<String> thArr = new ArrayList<>();
        for (Thermistor m : Thermistor.getThermistorList()) {
            thArr.add(m.sensorName+" "+m.engineeringUnit);
        }

        ArrayAdapter<String> analogAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, analogArr);
        analogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAnalog1Sp.setAdapter(analogAdapter);
        mAnalog2Sp.setAdapter(analogAdapter);


        ArrayAdapter<String> thAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, thArr);
        thAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mThermostat1Sp.setAdapter(thAdapter);
        mThermostat2Sp.setAdapter(thAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }


}
