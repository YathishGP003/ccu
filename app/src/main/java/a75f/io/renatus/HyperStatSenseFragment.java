package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;

public class HyperStatSenseFragment extends BaseDialogFragment {

    public static final String ID = HyperStatSenseFragment.class.getSimpleName();
    private  HyperStatSenseVM mHyperStatSenseVM;
    static final int TEMP_OFFSET_LIMIT = 100;

    Zone mZone;
    LightProfile mLightProfile;
    short        mNodeAddress;
    String       mRoomName;
    String       mFloorName;
    Boolean      misPaired;

    NumberPicker mTemperatureOffset;
    ToggleButton mTherm1toggle;
    ToggleButton mTherm2toggle;
    ToggleButton mAnalog1toggle;
    ToggleButton mAnalog2toggle;
    Spinner mThermostat1;
    Spinner mThermostat2;
    Spinner mAnalog1;
    Spinner mAnalog2;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hssense_config, container, false);
        mHyperStatSenseVM = ViewModelProviders.of(this).get(HyperStatSenseVM.class);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTemperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);
        mThermostat1 = view.findViewById(R.id.th1select);
        mThermostat2 = view.findViewById(R.id.th2select);
        mAnalog1 = view.findViewById(R.id.analog1select);
        mAnalog2 = view.findViewById(R.id.analog2select);
        mTherm1toggle = view.findViewById(R.id.th1);
        mTherm2toggle = view.findViewById(R.id.th2);
        mAnalog1toggle = view.findViewById(R.id.anlg1);
        mAnalog2toggle = view.findViewById(R.id.anlg2);
        mSetbtn = view.findViewById(R.id.setBtn);


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
        mThermostat1.setEnabled(false);
        mThermostat2.setEnabled(false);
        mAnalog1.setEnabled(false);
        mAnalog2.setEnabled(false);

        mTherm1toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mThermostat1.setEnabled(true);
                else mThermostat1.setEnabled(false);
            }
        });

        mTherm1toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mThermostat1.setEnabled(true);
                else mThermostat1.setEnabled(false);
            }
        });

        mTherm2toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mThermostat2.setEnabled(true);
                else mThermostat2.setEnabled(false);
            }
        });

        mAnalog1toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mAnalog1.setEnabled(true);
                else mAnalog1.setEnabled(false);
            }
        });

        mAnalog2toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mAnalog2.setEnabled(true);
                else mAnalog2.setEnabled(false);
            }
        });

        mSetbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });
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
