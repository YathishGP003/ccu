package a75f.io.renatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartStatConditioningMode_t;
import a75f.io.device.serial.SmartStatFanSpeed_t;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.sshpu.HeatPumpUnitConfiguration;
import a75f.io.logic.bo.building.sshpu.HeatPumpUnitProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;


public class FragmentHeatPumpConfiguration extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener{
    public static final String ID = FragmentHeatPumpConfiguration.class.getSimpleName();
    static final int TEMP_OFFSET_LIMIT = 100;
    String floorRef;
    String roomRef;
    private ProfileType mProfileType;
    private short mSmartNodeAddress;
    private NodeType mNodeType;
    private HeatPumpUnitProfile mHPUProfile;
    private HeatPumpUnitConfiguration mProfileConfig;


    ToggleButton switchThermistor1;
    ToggleButton switchCoolingY1;
    @BindView(R.id.testHpuRelay1)ToggleButton testComY1;
    ToggleButton switchCoolingY2;
    @BindView(R.id.testHpuRelay2)ToggleButton testComY2;
    ToggleButton switchFanLowG;
    @BindView(R.id.testHpuRelay3)ToggleButton testFanLowG;
    ToggleButton switchHeatingW1;
    @BindView(R.id.testHpuRelay4)ToggleButton testAuxHeating;
    ToggleButton switchFanHigh;
    @BindView(R.id.testHpuRelay5)ToggleButton testFanHighOb;
    ToggleButton switchHpChangeOver;
    @BindView(R.id.testHpuRelay6)ToggleButton testHeatChangeOver;
    ToggleButton switchOccSensor;
    ToggleButton switchExtTempSensor;
    Button setButton;
    Button cancelButton;
    Spinner hpChangeOverTypeSpinner;
    Spinner fanHumiDSpinner;
    NumberPicker temperatureOffset;

    public FragmentHeatPumpConfiguration() {
    }

    public static FragmentHeatPumpConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType) {
        FragmentHeatPumpConfiguration f = new FragmentHeatPumpConfiguration();
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
    public String getIdString() {
        return ID;
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
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
        /*TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(getResources().getColor(R.color.progress_color_orange));
        }*/
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.fragment_heatpump_config, container, false);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        roomRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        mProfileType = ProfileType.values()[getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)];
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mHPUProfile = (HeatPumpUnitProfile) L.getProfile(mSmartNodeAddress);

        if (mHPUProfile != null) {
            Log.d("CPUConfig", "Get Config: "+mHPUProfile.getProfileType()+","+mHPUProfile.getProfileConfiguration(mSmartNodeAddress)+","+mSmartNodeAddress);
            mProfileConfig = (HeatPumpUnitConfiguration) mHPUProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            Log.d("CPUConfig", "Create Profile: ");
            mHPUProfile = new HeatPumpUnitProfile();

        }

        switchCoolingY1 = (ToggleButton) view.findViewById(R.id.toggleComStage1);
        switchCoolingY2 = (ToggleButton) view.findViewById(R.id.toggleComStage2);
        switchHeatingW1 = (ToggleButton) view.findViewById(R.id.toggleAuxHeat);
        switchFanHigh = (ToggleButton) view.findViewById(R.id.toggleFanHigh);
        switchFanLowG = (ToggleButton) view.findViewById(R.id.toggleFanLow);
        switchHpChangeOver = (ToggleButton) view.findViewById(R.id.toggleHeatPump);
        switchThermistor1 = (ToggleButton) view.findViewById(R.id.toggleAirflow);
        switchExtTempSensor = (ToggleButton) view.findViewById(R.id.toogleExtSensor);
        hpChangeOverTypeSpinner = (Spinner)view.findViewById(R.id.spinnerPumpChange);
        fanHumiDSpinner = (Spinner)view.findViewById(R.id.spinnerFanHigh);
        temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);
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


        switchOccSensor = view.findViewById(R.id.toggleOccupancy);

        setButton = (Button) view.findViewById(R.id.set);

        if (mProfileConfig != null) {
            switchOccSensor.setChecked(mProfileConfig.enableOccupancyControl);
            int offsetIndex = (int) mProfileConfig.temperatureOffset + TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);
            switchExtTempSensor.setChecked(mProfileConfig.enableThermistor2);
            switchThermistor1.setChecked(mProfileConfig.enableThermistor1);
            if (mProfileConfig.getOutputs().size() > 0) {
                for (Output output : mProfileConfig.getOutputs()) {
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
                            switchFanHigh.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_SIX:
                            switchHpChangeOver.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                    }
                }
            }
            hpChangeOverTypeSpinner.setSelection(mProfileConfig.changeOverRelay6Type - 1);
            fanHumiDSpinner.setSelection(mProfileConfig.fanRelay5Type - 1);
        }
        /*hpChangeOverTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                hpChangeOverTypeSpinner.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });*/
        setButton.setOnClickListener(new View.OnClickListener() {
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
                    protected Void doInBackground(final String... params) {
                        setupCPUZoneProfile();
                        L.saveCCUState();

                        return null;
                    }

                    @Override
                    protected void onPostExecute(final Void result) {
                        progressDlg.dismiss();
                        FragmentHeatPumpConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

            }
        });
    }

    private void setupCPUZoneProfile() {

        HeatPumpUnitConfiguration hpuConfig = new HeatPumpUnitConfiguration();
        hpuConfig.setNodeType(mNodeType);
        hpuConfig.setNodeAddress(mSmartNodeAddress);
        hpuConfig.enableOccupancyControl = switchOccSensor.isChecked();
        hpuConfig.setPriority(ZonePriority.NONE);
        hpuConfig.temperatureOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        hpuConfig.enableThermistor1 = switchThermistor1.isChecked();
        hpuConfig.enableThermistor2 = switchExtTempSensor.isChecked();
        hpuConfig.changeOverRelay6Type = hpChangeOverTypeSpinner.getSelectedItemPosition()+1;
        hpuConfig.fanRelay5Type = fanHumiDSpinner.getSelectedItemPosition()+1;

        if (switchCoolingY1.isChecked()) {
            Output relay1Op = new Output();
            relay1Op.setAddress(mSmartNodeAddress);
            relay1Op.setPort(Port.RELAY_ONE);
            relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            hpuConfig.getOutputs().add(relay1Op);
        }
        hpuConfig.enableRelay1 = switchCoolingY1.isChecked();
        if (switchCoolingY2.isChecked()) {
            Output relay2Op = new Output();
            relay2Op.setAddress(mSmartNodeAddress);
            relay2Op.setPort(Port.RELAY_TWO);
            relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            hpuConfig.getOutputs().add(relay2Op);
        }
        hpuConfig.enableRelay2 = switchCoolingY2.isChecked();
        if (switchFanLowG.isChecked()) {
            Output relay3Op = new Output();
            relay3Op.setAddress(mSmartNodeAddress);
            relay3Op.setPort(Port.RELAY_THREE);
            relay3Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            hpuConfig.getOutputs().add(relay3Op);
        }

        hpuConfig.enableRelay3 = switchFanLowG.isChecked();
        if (switchHeatingW1.isChecked()) {
            Output relay4Op = new Output();
            relay4Op.setAddress(mSmartNodeAddress);
            relay4Op.setPort(Port.RELAY_FOUR);
            relay4Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            hpuConfig.getOutputs().add(relay4Op);
        }

        hpuConfig.enableRelay4 = switchHeatingW1.isChecked();
        if (switchFanHigh.isChecked()) {
            Output relay5Op = new Output();
            relay5Op.setAddress(mSmartNodeAddress);
            relay5Op.setPort(Port.RELAY_FIVE);
            relay5Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            hpuConfig.getOutputs().add(relay5Op);
        }
        hpuConfig.enableRelay5 = switchFanHigh.isChecked();
        if (switchHpChangeOver.isChecked()) {
            Output relay6Op = new Output();
            relay6Op.setAddress(mSmartNodeAddress);
            relay6Op.setPort(Port.RELAY_SIX);
            relay6Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            hpuConfig.getOutputs().add(relay6Op);
        }
        hpuConfig.enableRelay6 = switchHpChangeOver.isChecked();

        mHPUProfile.getProfileConfiguration().put(mSmartNodeAddress, hpuConfig);
        if (mProfileConfig == null) {
            mHPUProfile.addLogicalMapAndPoints(mSmartNodeAddress, hpuConfig, floorRef, roomRef);
        } else {
            mHPUProfile.updateLogicalMapAndPoints(mSmartNodeAddress, hpuConfig, roomRef);
        }
        L.ccu().zoneProfiles.add(mHPUProfile);
        Log.d("CPUConfig", "Set Config: Profiles - " + L.ccu().zoneProfiles.size());
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
            Log.e("class not found", e.toString());
        } catch (NoSuchFieldException e) {
            Log.e("NoSuchFieldException", e.toString());
        } catch (IllegalAccessException e) {
            Log.e("IllegalAccessException", e.toString());
        } catch (Exception e) {
            Log.e("dividerexception", e.getMessage().toString());
        }
    }

    @Override
    @OnCheckedChanged({R.id.testHpuRelay1,R.id.testHpuRelay2,R.id.testHpuRelay3,R.id.testHpuRelay4,R.id.testHpuRelay5,R.id.testHpuRelay6})
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId())
        {
            case R.id.testHpuRelay1:
                sendRelayActivationTestSignal( (short) (testComY1.isChecked() ? 1: 0), Port.RELAY_ONE);
                break;
            case R.id.testHpuRelay2:
                sendRelayActivationTestSignal( (short) (testComY2.isChecked() ? 1: 0), Port.RELAY_TWO);
                break;
            case R.id.testHpuRelay3:
                sendRelayActivationTestSignal( (short) (testFanLowG.isChecked() ? 1: 0), Port.RELAY_THREE);
                break;
            case R.id.testHpuRelay4:
                sendRelayActivationTestSignal( (short) (testAuxHeating.isChecked() ? 1: 0), Port.RELAY_FOUR);
                break;
            case R.id.testHpuRelay5:
                sendRelayActivationTestSignal( (short) (testFanHighOb.isChecked() ? 1: 0), Port.RELAY_FIVE);
                break;
            case R.id.testHpuRelay6:
                sendRelayActivationTestSignal( (short) (testHeatChangeOver.isChecked() ? 1: 0), Port.RELAY_SIX);
                break;
        }
    }


    public void sendRelayActivationTestSignal(short val,Port port) {
        Log.d(L.TAG_CCU_UI, "sendRelayActivationTestSignal val : "+val);
        CcuToCmOverUsbSmartStatControlsMessage_t msg = new CcuToCmOverUsbSmartStatControlsMessage_t();
        msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
        msg.address.set(mSmartNodeAddress);
        msg.controls.setTemperature.set((short)(getDesiredTemp(mSmartNodeAddress)*2));
        msg.controls.conditioningMode.set(SmartStatConditioningMode_t.CONDITIONING_MODE_AUTO);
        msg.controls.fanSpeed.set(SmartStatFanSpeed_t.FAN_SPEED_AUTO);
        switch (port){
            case RELAY_ONE:
                msg.controls.relay1.set(val);
                break;
            case RELAY_TWO:
                msg.controls.relay2.set(val);
                break;
            case RELAY_THREE:
                msg.controls.relay3.set(val);
                break;
            case RELAY_FOUR:
                msg.controls.relay4.set(val);
                break;
            case RELAY_FIVE:
                msg.controls.relay5.set(val);
                break;
            case RELAY_SIX:
                msg.controls.relay6.set(val);
                break;
        }
        MeshUtil.sendStructToCM(msg);
    }
    public static double getDesiredTemp(short node)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and group == \""+node+"\"");
        if (point == null || point.size() == 0) {
            Log.d("HPU", " Desired Temp point does not exist for equip , sending 0");
            return 72;
        }
        return CCUHsApi.getInstance().readPointPriorityVal(point.get("id").toString());
    }
}
