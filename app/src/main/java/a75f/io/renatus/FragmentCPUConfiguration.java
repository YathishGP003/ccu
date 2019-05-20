package a75f.io.renatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitConfiguration;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitProfile;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.ButterKnife;

public class FragmentCPUConfiguration extends BaseDialogFragment {
    public static final String ID = FragmentCPUConfiguration.class.getSimpleName();
    static final int TEMP_OFFSET_LIMIT = 100;
    String floorRef;
    String roomRef;
    private ProfileType mProfileType;
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private ConventionalUnitProfile mCPUProfile;
    private ConventionalUnitConfiguration mProfileConfig;


    ToggleButton switchThermistor1;
    ToggleButton switchCoolingY1;
    ToggleButton testCoolingY1;
    ToggleButton switchCoolingY2;
    ToggleButton testCoolingY2;
    ToggleButton switchFanLowG;
    ToggleButton testFanLowG;
    ToggleButton switchHeatingW1;
    ToggleButton testHeatingW1;
    ToggleButton switchHeatingW2;
    ToggleButton testHeatingW2;
    ToggleButton switchFanHighOb;
    ToggleButton testFanHighOb;
    ToggleButton switchOccSensor;
    ToggleButton switchExtTempSensor;
    Button setButton;
    Button cancelButton;
    NumberPicker temperatureOffset;
    Spinner fanHumiDSpinner;

    public FragmentCPUConfiguration()
    {
    }

    public static FragmentCPUConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType)
    {
        FragmentCPUConfiguration f = new FragmentCPUConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
        bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal());
        f.setArguments(bundle);
        return f;
    }

    @Override
    public String getIdString()
    {
        return ID;
    }


    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = 1165;//ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = 720;//ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        setTitle();
    }

    private void setTitle() {
        Dialog dialog = getDialog();

        if (dialog == null) {
            return;
        }
        int titleDividerId = getContext().getResources()
                .getIdentifier("titleDivider", "id", "android");

        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null) {
            titleDivider.setBackgroundColor(getContext().getResources()
                    .getColor(R.color.transparent));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.fragment_cpu_config, container, false);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        roomRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        mProfileType = ProfileType.values()[getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)];
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {

        mCPUProfile = (ConventionalUnitProfile) L.getProfile(mSmartNodeAddress);

        if (mCPUProfile != null) {
            Log.d("CPUConfig", "Get Config: ");
            mProfileConfig = (ConventionalUnitConfiguration) mCPUProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            Log.d("CPUConfig", "Create Profile: ");
            mCPUProfile = new ConventionalUnitProfile();

        }

        switchCoolingY1 = (ToggleButton)view.findViewById(R.id.toggleCoolStage1);
        switchCoolingY2 = (ToggleButton)view.findViewById(R.id.toggleCoolStage2);
        switchHeatingW1 = (ToggleButton)view.findViewById(R.id.toggleHeatStage1);
        switchHeatingW2 = (ToggleButton)view.findViewById(R.id.toggleHeatStage2);
        switchFanLowG = (ToggleButton)view.findViewById(R.id.toggleCpuFanLow);
        switchFanHighOb = (ToggleButton)view.findViewById(R.id.toggleCpuFanHigh);
        switchThermistor1 = (ToggleButton)view.findViewById(R.id.toggleCpuAirflow);
        switchExtTempSensor = (ToggleButton)view.findViewById(R.id.toogleCpuExtSensor);
        temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);
        fanHumiDSpinner = (Spinner)view.findViewById(R.id.spinnerCpuFanHigh);
        setNumberPickerDividerColor(temperatureOffset);
        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        temperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);


        switchOccSensor = view.findViewById(R.id.toggleCpuOccupancy);


        setButton = (Button) view.findViewById(R.id.setBtn);

        if (mProfileConfig != null) {
            switchOccSensor.setChecked(mProfileConfig.enableOccupancyControl);
            int offsetIndex = (int)mProfileConfig.temperatureOffset+TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);
            switchExtTempSensor.setChecked(mProfileConfig.enableThermistor2);
            switchThermistor1.setChecked(mProfileConfig.enableThermistor1);
            if(mProfileConfig.getOutputs().size() > 0) {
                for(Output output : mProfileConfig.getOutputs()) {
                    switch (output.getPort()) {
                        case RELAY_ONE:
                            switchCoolingY1.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_TWO:
                            switchCoolingY2.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_THREE:
                            switchFanLowG.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_FOUR:
                            switchHeatingW1.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_FIVE:
                            switchHeatingW2.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_SIX:
                            switchFanHighOb.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                    }
                }
            }

            fanHumiDSpinner.setSelection(mProfileConfig.relay6Type - 1);
        }
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                new AsyncTask<String, Void, Void>() {

                    ProgressDialog progressDlg = new ProgressDialog(getActivity());

                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        progressDlg.setMessage("Saving CPU Configuration");
                        progressDlg.show();
                        super.onPreExecute();
                    }

                    @Override
                    protected Void doInBackground( final String ... params ) {
                        setupCPUZoneProfile();
                        L.saveCCUState();

                        return null;
                    }

                    @Override
                    protected void onPostExecute( final Void result ) {
                        progressDlg.dismiss();
                        FragmentCPUConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

            }
        });
    }

    private void setupCPUZoneProfile() {

        ConventionalUnitConfiguration cpuConfig = new ConventionalUnitConfiguration();
        cpuConfig.setNodeType(mNodeType);
        cpuConfig.setNodeAddress(mSmartNodeAddress);
        cpuConfig.enableOccupancyControl = switchOccSensor.isChecked();
        cpuConfig.setPriority(ZonePriority.NONE);
        cpuConfig.temperatureOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        cpuConfig.enableThermistor1 = switchThermistor1.isChecked();
        cpuConfig.enableThermistor2 = switchExtTempSensor.isChecked();
        cpuConfig.relay6Type = fanHumiDSpinner.getSelectedItemPosition()+1;


        if(switchCoolingY1.isChecked()) {
            Output relay1Op = new Output();
            relay1Op.setAddress(mSmartNodeAddress);
            relay1Op.setPort(Port.RELAY_ONE);
            relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            cpuConfig.getOutputs().add(relay1Op);
        }
        cpuConfig.enableRelay1 = switchCoolingY1.isChecked();
        if(switchCoolingY2.isChecked()){
            Output relay2Op = new Output();
            relay2Op.setAddress(mSmartNodeAddress);
            relay2Op.setPort(Port.RELAY_TWO);
            relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            cpuConfig.getOutputs().add(relay2Op);
        }
        cpuConfig.enableRelay2 = switchCoolingY2.isChecked();
        if(switchFanLowG.isChecked()) {
            Output relay3Op = new Output();
            relay3Op.setAddress(mSmartNodeAddress);
            relay3Op.setPort(Port.RELAY_THREE);
            relay3Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            cpuConfig.getOutputs().add(relay3Op);
        }

        cpuConfig.enableRelay3 = switchFanLowG.isChecked();
        if(switchHeatingW1.isChecked()){
            Output relay4Op = new Output();
            relay4Op.setAddress(mSmartNodeAddress);
            relay4Op.setPort(Port.RELAY_FOUR);
            relay4Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            cpuConfig.getOutputs().add(relay4Op);
        }

        cpuConfig.enableRelay4 = switchHeatingW1.isChecked();
        if(switchHeatingW2.isChecked()) {
            Output relay5Op = new Output();
            relay5Op.setAddress(mSmartNodeAddress);
            relay5Op.setPort(Port.RELAY_FIVE);
            relay5Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            cpuConfig.getOutputs().add(relay5Op);
        }
        cpuConfig.enableRelay5 = switchHeatingW2.isChecked();
        if(switchFanHighOb.isChecked()){
            Output relay6Op = new Output();
            relay6Op.setAddress(mSmartNodeAddress);
            relay6Op.setPort(Port.RELAY_SIX);
            relay6Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            cpuConfig.getOutputs().add(relay6Op);
        }
        cpuConfig.enableRelay6 = switchFanHighOb.isChecked();

        mCPUProfile.getProfileConfiguration().put(mSmartNodeAddress, cpuConfig);
        if (mProfileConfig == null) {
            mCPUProfile.addLogicalMapAndPoints(mSmartNodeAddress, cpuConfig, floorRef, roomRef);
        } else
        {
            mCPUProfile.updateLogicalMapAndPoints(mSmartNodeAddress, cpuConfig,roomRef);
        }
        L.ccu().zoneProfiles.add(mCPUProfile);
        Log.d("CPUConfig", "Set Config: Profiles - "+L.ccu().zoneProfiles.size());
    }
    private void setNumberPickerDividerColor(NumberPicker pk) {
        Class<?> numberPickerClass = null;
        try {
            numberPickerClass = Class.forName("android.widget.NumberPicker");
            Field selectionDivider = numberPickerClass.getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            //if(!CCUUtils.isxlargedevice(getActivity())) {
            selectionDivider.set(pk, getResources().getDrawable(R.drawable.line_959595));
            //}else{
            //   selectionDivider.set(pk, getResources().getDrawable(R.drawable.connect_192x48_orange));
            //}

        } catch (ClassNotFoundException e) {
            Log.e("class not found",e.toString());
        } catch (NoSuchFieldException e) {
            Log.e("NoSuchFieldException",e.toString());
        } catch (IllegalAccessException e) {
            Log.e("IllegalAccessException",e.toString());
        }catch (Exception e){
            Log.e("dividerexception",e.getMessage().toString());
        }
    }
}
