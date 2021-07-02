package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
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


import java.util.ArrayList;

import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseConfiguration;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseEquip;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseProfile;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.plc.PlcProfileConfiguration;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnCheckedChanged;

/*
* created by spoorthidev on 30-May-2021
 */


public class HyperStatSenseFragment extends BaseDialogFragment {

    public static final String ID = HyperStatSenseFragment.class.getSimpleName();
    public static final String LOG_TAG = HyperStatSenseFragment.class.getSimpleName();
    private HyperStatSenseVM mHyperStatSenseVM;
    private HyperStatSenseProfile mHSSenseProfile;
    HyperStatSenseConfiguration mHSSenseConfig;
    static final int TEMP_OFFSET_LIMIT = 100;

    short mNodeAddress;
    String mRoomName;
    String mFloorName;
    Boolean misPaired;
    String mProfileName;

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


    public static HyperStatSenseFragment newInstance(short meshAddress, String roomName, String floorName, ProfileType profileType) {
        HyperStatSenseFragment f = new HyperStatSenseFragment();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.name());
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

        mHSSenseProfile = (HyperStatSenseProfile) L.getProfile(mNodeAddress);
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
        mTemperatureOffset.setWrapSelectorWheel(false);
        if (mHSSenseProfile != null) {
            CcuLog.d(L.TAG_CCU_UI, "Get HyperStat SenseConfig: ");
            mHSSenseConfig = (HyperStatSenseConfiguration) mHSSenseProfile.getProfileConfiguration(mNodeAddress);
            mTemperatureOffset.setValue((int) (mHSSenseConfig.temperatureOffset + TEMP_OFFSET_LIMIT));

            mThermostat1Sp.setEnabled(mHSSenseConfig.isTh1Enable);
            mThermostat2Sp.setEnabled(mHSSenseConfig.isTh2Enable);
            mAnalog1Sp.setEnabled(mHSSenseConfig.isAnalog1Enable);
            mAnalog2Sp.setEnabled(mHSSenseConfig.isAnalog2Enable);

            mTherm1toggle.setChecked(mHSSenseConfig.isTh1Enable);
            mTherm2toggle.setChecked(mHSSenseConfig.isTh2Enable);
            mAnalog1toggle.setChecked(mHSSenseConfig.isAnalog1Enable);
            mAnalog2toggle.setChecked(mHSSenseConfig.isAnalog2Enable);

            if (mHSSenseConfig.isTh1Enable) {
                mThermostat1Sp.setSelection(mHSSenseConfig.th1Sensor);
            }
            if (mHSSenseConfig.isTh2Enable) {
                mThermostat2Sp.setSelection(mHSSenseConfig.th2Sensor);
            }
            if (mHSSenseConfig.isAnalog1Enable) {
                mAnalog1Sp.setSelection(mHSSenseConfig.analog1Sensor);
            }
            if (mHSSenseConfig.isAnalog2Enable) {
                mAnalog2Sp.setSelection(mHSSenseConfig.analog2Sensor);
            }

        } else {
            CcuLog.d(L.TAG_CCU_UI, "Create Hyperstatsense Profile: ");
            mHSSenseProfile = new HyperStatSenseProfile();
            /** Spinner id disabled by default */
            mThermostat1Sp.setEnabled(false);
            mThermostat2Sp.setEnabled(false);
            mAnalog1Sp.setEnabled(false);
            mAnalog2Sp.setEnabled(false);
        }
     /*   mHyperStatSenseVM = new ViewModelProvider(this).get(HyperStatSenseVM.class);
        mHyperStatSenseVM.init();
        mHyperStatSenseVM.get().observe(this, new Observer<HyperStatSenseModel>() {
            @Override
            public void onChanged(HyperStatSenseModel model) {
                   //TODO
                Log.d("Spoo", "in onchange");
                updateView();
            }
        });*/

    }


    @OnCheckedChanged({R.id.th1, R.id.th2, R.id.anlg1, R.id.anlg2})
    public void th1Onchange(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.th1:
                if (isChecked) mThermostat1Sp.setEnabled(true);
                else mThermostat1Sp.setEnabled(false);
                break;
            case R.id.th2:
                if (isChecked) mThermostat2Sp.setEnabled(true);
                else mThermostat2Sp.setEnabled(false);
                break;
            case R.id.anlg1:
                if (isChecked) mAnalog1Sp.setEnabled(true);
                else mAnalog1Sp.setEnabled(false);
                break;
            case R.id.anlg2:
                if (isChecked) mAnalog2Sp.setEnabled(true);
                else mAnalog2Sp.setEnabled(false);
                break;
        }
    }

    @OnClick(R.id.setBtn)
    void setOnClick(View v) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected void onPreExecute() {
                mSetbtn.setEnabled(false);
                ProgressDialogUtils.showProgressDialog(getActivity(), "Saving HyperStat Sense Configuration");
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(final String... params) {
                setupSenseProfile();
                L.saveCCUState();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                ProgressDialogUtils.hideProgressDialog();
                HyperStatSenseFragment.this.closeAllBaseDialogFragments();
                getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                LSerial.getInstance().sendSeedMessage(false, false, mNodeAddress, mRoomName, mFloorName);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void setupSenseProfile() {

        HyperStatSenseConfiguration hssense = new HyperStatSenseConfiguration();
        hssense.temperatureOffset = mTemperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        hssense.isTh1Enable = mTherm1toggle.isChecked();
        hssense.isTh2Enable = mTherm2toggle.isChecked();
        hssense.isAnalog1Enable = mAnalog1toggle.isChecked();
        hssense.isAnalog2Enable = mAnalog2toggle.isChecked();
        hssense.th1Sensor = mThermostat1Sp.getSelectedItemPosition();
        hssense.th2Sensor = mThermostat2Sp.getSelectedItemPosition();
        hssense.analog1Sensor = mAnalog1Sp.getSelectedItemPosition();
        hssense.analog2Sensor = mAnalog2Sp.getSelectedItemPosition();

        mHSSenseProfile.getProfileConfiguration().put(mNodeAddress, hssense);
        if (mHSSenseConfig == null) {
            Log.d(LOG_TAG, "Creating new config");
            mHSSenseProfile.addHyperStatSenseEquip(ProfileType.HYPERSTAT_SENSE, mNodeAddress, hssense, mFloorName, mRoomName);
        } else {
            Log.d(LOG_TAG, " Update config");
            mHSSenseProfile.updateHyperStatSenseEquip(ProfileType.HYPERSTAT_SENSE, mNodeAddress, hssense, mFloorName, mRoomName);
        }
        L.ccu().zoneProfiles.add(mHSSenseProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set Hyperstat sense Config: Profiles - " + L.ccu().zoneProfiles.size());
    }


    private void setSpinnerListItem() {
        ArrayList<String> analogArr = new ArrayList<>();
        for (Sensor r : SensorManager.getInstance().getExternalSensorList()) {
            analogArr.add(r.sensorName + " " + r.engineeringUnit);
        }
        ArrayList<String> thArr = new ArrayList<>();
        for (Thermistor m : Thermistor.getThermistorList()) {
            thArr.add(m.sensorName + " " + m.engineeringUnit);
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
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }


}
