package a75f.io.renatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitConfiguration;
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class Fragment4PipeFanCoilUnitConfig extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener {
    public static final String ID = Fragment4PipeFanCoilUnitConfig.class.getSimpleName();
    static final int TEMP_OFFSET_LIMIT = 100;
    String floorRef;
    String roomRef;
    private ProfileType mProfileType;
    private short mSmartNodeAddress;
    private NodeType mNodeType;
    private FourPipeFanCoilUnitProfile fourPfcuProfile;
    private FourPipeFanCoilUnitConfiguration mProfileConfig;

    ToggleButton switchThermistor1;
    ToggleButton switchFanMediumY1;
    @BindView(R.id.test2pfcuRelay1)ToggleButton testFanMediumY1;
    ToggleButton switchFanHighY2;
    @BindView(R.id.test2pfcuRelay2)ToggleButton testFanHighY2;
    ToggleButton switchFanLowG;
    @BindView(R.id.test2pfcuRelay3)ToggleButton testFanLowG;
    ToggleButton switchHeatingWaterValve;
    @BindView(R.id.test2pfcuRelay4)ToggleButton testAuxHeating;
    ToggleButton switchCoolingWaterValve;
    @BindView(R.id.test2pfcuRelay6)ToggleButton testWaterValve;
    @BindView(R.id.textAirflow)TextView textAirflow;
    @BindView(R.id.text2pfcuWaterValve)
    TextView text2pfcuWaterValve;
    @BindView(R.id.lt_enableLabel)
    LinearLayout ltEnableLabel;
    ToggleButton switchOccSensor;
    ToggleButton switchExtTempSensor;
    Button setButton;
    NumberPicker temperatureOffset;

    public Fragment4PipeFanCoilUnitConfig() {
    }

    public static Fragment4PipeFanCoilUnitConfig newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType) {
        Fragment4PipeFanCoilUnitConfig f = new Fragment4PipeFanCoilUnitConfig();
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
            int width = 1165;
            int height = 720;
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.fragment_4pfcu_config, container, false);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        roomRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        mProfileType = ProfileType.values()[getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)];
        ButterKnife.bind(this, view);

        if (getResources().getDisplayMetrics().xdpi == (float)149.824){
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(330, 53, 0, 0);
            ltEnableLabel.setLayoutParams(lp);

            LinearLayout.LayoutParams txtlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            txtlp.setMargins(0, 10, 0, 0);
            text2pfcuWaterValve.setLayoutParams(txtlp);

            LinearLayout.LayoutParams txtAirlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,40);
            txtAirlp.setMargins(0, 50, 0, 0);
            textAirflow.setLayoutParams(txtAirlp);

        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        fourPfcuProfile = (FourPipeFanCoilUnitProfile) L.getProfile(mSmartNodeAddress);

        if (fourPfcuProfile != null) {
            Log.d("CPUConfig", "Get Config: "+fourPfcuProfile.getProfileType()+","+fourPfcuProfile.getProfileConfiguration(mSmartNodeAddress)+","+mSmartNodeAddress);
            mProfileConfig = (FourPipeFanCoilUnitConfiguration) fourPfcuProfile
                    .getProfileConfiguration(mSmartNodeAddress);
        } else {
            Log.d("CPUConfig", "Create Profile: ");
            fourPfcuProfile = new FourPipeFanCoilUnitProfile();

        }

        switchFanMediumY1 = (ToggleButton) view.findViewById(R.id.toggleFanMed4pfcu);
        switchFanHighY2 = (ToggleButton) view.findViewById(R.id.toggleFanHigh4pfcu);
        switchHeatingWaterValve = (ToggleButton) view.findViewById(R.id.toggleHeat4pfcu);
        switchFanLowG = (ToggleButton) view.findViewById(R.id.toggleFanLow4pfcu);
        switchCoolingWaterValve = (ToggleButton) view.findViewById(R.id.toggleCool4pfcu);
        switchThermistor1 = (ToggleButton) view.findViewById(R.id.toggleAirflow);
        switchExtTempSensor = (ToggleButton) view.findViewById(R.id.toogleExtSensor4pfcu);
        temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);
        setDividerColor(temperatureOffset);
        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        temperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);


        switchFanLowG.setOnCheckedChangeListener(this);
        switchFanMediumY1.setOnCheckedChangeListener(this);
        switchOccSensor = view.findViewById(R.id.toggleOccupancy4pfcu);

        setButton = (Button) view.findViewById(R.id.set4PfcuBtn);

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
                            switchFanMediumY1.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_TWO:
                            switchFanHighY2.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_THREE:
                            switchFanLowG.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_FOUR:
                            switchHeatingWaterValve.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_SIX:
                            switchCoolingWaterValve.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                    }
                }
            }
        }else{
            switchFanLowG.setEnabled(true);
            switchFanMediumY1.setEnabled(false);
            switchFanHighY2.setEnabled(false);
        }
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AsyncTask<String, Void, Void>() {

                    ProgressDialog progressDlg = new ProgressDialog(getActivity());

                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        progressDlg.setMessage("Saving 4PFCU Configuration");
                        progressDlg.show();
                        super.onPreExecute();
                    }

                    @Override
                    protected Void doInBackground(final String... params) {
                        setup4PFCUZoneProfile();
                        L.saveCCUState();

                        return null;
                    }

                    @Override
                    protected void onPostExecute(final Void result) {
                        progressDlg.dismiss();
                        Fragment4PipeFanCoilUnitConfig.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

            }
        });
    }

    private void setup4PFCUZoneProfile() {

        FourPipeFanCoilUnitConfiguration fourPfcuConfig = new FourPipeFanCoilUnitConfiguration();
        fourPfcuConfig.setNodeType(mNodeType);
        fourPfcuConfig.setNodeAddress(mSmartNodeAddress);
        fourPfcuConfig.enableOccupancyControl = switchOccSensor.isChecked();
        fourPfcuConfig.setPriority(ZonePriority.NONE);
        fourPfcuConfig.temperatureOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        fourPfcuConfig.enableThermistor1 = switchThermistor1.isChecked();
        fourPfcuConfig.enableThermistor2 = switchExtTempSensor.isChecked();

        if (switchFanMediumY1.isChecked()) {
            Output relay1Op = new Output();
            relay1Op.setAddress(mSmartNodeAddress);
            relay1Op.setPort(Port.RELAY_ONE);
            relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            fourPfcuConfig.getOutputs().add(relay1Op);
        }
        fourPfcuConfig.enableRelay1 = switchFanMediumY1.isChecked();
        if (switchFanHighY2.isChecked()) {
            Output relay2Op = new Output();
            relay2Op.setAddress(mSmartNodeAddress);
            relay2Op.setPort(Port.RELAY_TWO);
            relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            fourPfcuConfig.getOutputs().add(relay2Op);
        }
        fourPfcuConfig.enableRelay2 = switchFanHighY2.isChecked();
        if (switchFanLowG.isChecked()) {
            Output relay3Op = new Output();
            relay3Op.setAddress(mSmartNodeAddress);
            relay3Op.setPort(Port.RELAY_THREE);
            relay3Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            fourPfcuConfig.getOutputs().add(relay3Op);
        }

        fourPfcuConfig.enableRelay3 = switchFanLowG.isChecked();
        if (switchHeatingWaterValve.isChecked()) {
            Output relay4Op = new Output();
            relay4Op.setAddress(mSmartNodeAddress);
            relay4Op.setPort(Port.RELAY_FOUR);
            relay4Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            fourPfcuConfig.getOutputs().add(relay4Op);
        }

        fourPfcuConfig.enableRelay4 = switchHeatingWaterValve.isChecked();
        fourPfcuConfig.enableRelay5 = false;
        if (switchCoolingWaterValve.isChecked()) {
            Output relay6Op = new Output();
            relay6Op.setAddress(mSmartNodeAddress);
            relay6Op.setPort(Port.RELAY_SIX);
            relay6Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            fourPfcuConfig.getOutputs().add(relay6Op);
        }
        fourPfcuConfig.enableRelay6 = switchCoolingWaterValve.isChecked();

        fourPfcuProfile.getProfileConfiguration().put(mSmartNodeAddress, fourPfcuConfig);
        if (mProfileConfig == null) {
            fourPfcuProfile.addLogicalMapAndPoints(mSmartNodeAddress, fourPfcuConfig, floorRef, roomRef);
        } else {
            fourPfcuProfile.updateLogicalMapAndPoints(mSmartNodeAddress, fourPfcuConfig, roomRef);
        }
        L.ccu().zoneProfiles.add(fourPfcuProfile);
        Log.d("CPUConfig", "Set Config: Profiles - " + L.ccu().zoneProfiles.size());
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

    @Override
    @OnCheckedChanged({R.id.test2pfcuRelay1,R.id.test2pfcuRelay2,R.id.test2pfcuRelay3,R.id.test2pfcuRelay4,R.id.test2pfcuRelay6})
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId())
        {
            case R.id.test2pfcuRelay1:
                sendRelayActivationTestSignal();
                break;
            case R.id.test2pfcuRelay2:
                sendRelayActivationTestSignal();
                break;
            case R.id.test2pfcuRelay3:
                sendRelayActivationTestSignal();
                break;
            case R.id.test2pfcuRelay4:
                sendRelayActivationTestSignal();
                break;
            case R.id.test2pfcuRelay6:
                sendRelayActivationTestSignal();
                break;
            case R.id.toggleFanLow4pfcu:
                if(switchFanLowG.isChecked()){
                    switchFanMediumY1.setEnabled(true);
                    if(switchFanMediumY1.isChecked()){
                        switchFanHighY2.setEnabled(true);
                    }else {
                        switchFanHighY2.setEnabled(false);
                        switchFanHighY2.setChecked(false);
                    }
                }else {
                    switchFanMediumY1.setEnabled(false);
                    switchFanMediumY1.setChecked(false);
                    switchFanHighY2.setEnabled(false);
                    switchFanHighY2.setChecked(false);
                }
                break;
            case R.id.toggleFanMed4pfcu:
                if(switchFanMediumY1.isChecked()){
                    switchFanHighY2.setEnabled(true);
                }else {
                    switchFanHighY2.setEnabled(false);
                    switchFanHighY2.setChecked(false);
                }
                break;
        }
    }


    public void sendRelayActivationTestSignal() {
        CcuToCmOverUsbSmartStatControlsMessage_t msg = new CcuToCmOverUsbSmartStatControlsMessage_t();
        msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
        msg.address.set(mSmartNodeAddress);
        msg.controls.setTemperature.set((short)(getDesiredTemp(mSmartNodeAddress)*2));
        msg.controls.conditioningMode.set(SmartStatConditioningMode_t.CONDITIONING_MODE_AUTO);
        msg.controls.fanSpeed.set(SmartStatFanSpeed_t.FAN_SPEED_AUTO);
        msg.controls.relay1.set((short)(testFanMediumY1.isChecked() ? 1 : 0));
        msg.controls.relay2.set((short)(testFanHighY2.isChecked() ? 1 : 0));
        msg.controls.relay3.set((short)(testFanLowG.isChecked() ? 1 : 0));
        msg.controls.relay4.set((short)(testAuxHeating.isChecked() ? 1 : 0));
        msg.controls.relay6.set((short)(testWaterValve.isChecked() ? 1 : 0));
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
