package a75f.io.renatus;

import static a75f.io.device.bacnet.BacnetUtilKt.addBacnetTags;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartStatConditioningMode_t;
import a75f.io.device.serial.SmartStatFanSpeed_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.ss2pfcu.TwoPipeFanCoilUnitConfiguration;
import a75f.io.logic.bo.building.ss2pfcu.TwoPipeFanCoilUnitProfile;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.views.CustomCCUSwitch;
import a75f.io.util.ExecutorTask;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Fragment2PipeFanCoilUnitConfig extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener {
    public static final String ID = Fragment2PipeFanCoilUnitConfig.class.getSimpleName();
    static final int TEMP_OFFSET_LIMIT = 100;
    String floorRef;
    String roomRef;
    private ProfileType mProfileType;
    private short mSmartNodeAddress;
    private NodeType mNodeType;
    private TwoPipeFanCoilUnitProfile twoPfcuProfile;
    private TwoPipeFanCoilUnitConfiguration mProfileConfig;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    CustomCCUSwitch switchThermistor1;
    CustomCCUSwitch switchFanMediumY1;
    @BindView(R.id.test2pfcuRelay1)ToggleButton testFanMediumY1;
    CustomCCUSwitch switchFanHighY2;
    @BindView(R.id.test2pfcuRelay2)ToggleButton testFanHighY2;
    CustomCCUSwitch switchFanLowG;
    @BindView(R.id.test2pfcuRelay3)ToggleButton testFanLowG;
    CustomCCUSwitch switchHeatingW1;
    @BindView(R.id.test2pfcuRelay4)ToggleButton testAuxHeating;
    CustomCCUSwitch switchWaterValve;
    @BindView(R.id.test2pfcuRelay6)ToggleButton testWaterValve;
    @BindView(R.id.lt_enableLabel)
    LinearLayout ltEnableLabel;
    CustomCCUSwitch switchOccSensor;
    CustomCCUSwitch switchExtTempSensor;
    Button setButton;
    NumberPicker temperatureOffset;

    CustomCCUSwitch toggleAutoaway;
    CustomCCUSwitch toggleAutoForceOccupied;

    public Fragment2PipeFanCoilUnitConfig() {
    }

    public static Fragment2PipeFanCoilUnitConfig newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType) {
        Fragment2PipeFanCoilUnitConfig f = new Fragment2PipeFanCoilUnitConfig();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
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
        View view = inflater.inflate(R.layout.fragment_2pfcu_config, container, false);
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

        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (L.getProfile(mSmartNodeAddress) != null && L.getProfile(mSmartNodeAddress) instanceof TwoPipeFanCoilUnitProfile) {
            twoPfcuProfile = (TwoPipeFanCoilUnitProfile) L.getProfile(mSmartNodeAddress);
        }

        if (twoPfcuProfile != null) {
            CcuLog.d("CPUConfig", "Get Config: "+twoPfcuProfile.getProfileType()+","+twoPfcuProfile.getProfileConfiguration(mSmartNodeAddress)+","+mSmartNodeAddress);
            mProfileConfig = (TwoPipeFanCoilUnitConfiguration) twoPfcuProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            CcuLog.d("CPUConfig", "Create Profile: ");
            twoPfcuProfile = new TwoPipeFanCoilUnitProfile();

        }

        switchFanMediumY1 = view.findViewById(R.id.toggleFanMed2pfcu);
        switchFanHighY2 = view.findViewById(R.id.toggleFanHigh2pfcu);
        switchHeatingW1 = view.findViewById(R.id.toggleAuxHeat2pfcu);
        switchFanLowG = view.findViewById(R.id.toggleFanLow);
        switchWaterValve = view.findViewById(R.id.toggleWaterValve2pfcu);
        switchThermistor1 = view.findViewById(R.id.toggleAirflow);
        switchExtTempSensor = view.findViewById(R.id.toogleExtSensor);
        temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);

        toggleAutoaway = view.findViewById(R.id.toggleAutoAway);
        toggleAutoForceOccupied = view.findViewById(R.id.toggleAFO);


        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        temperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);


        switchFanLowG.setOnCheckedChangeListener(this);
        switchFanMediumY1.setOnCheckedChangeListener(this);
        switchFanHighY2.setOnCheckedChangeListener(this);
        switchWaterValve.setChecked(true);
        switchWaterValve.setClickable(false);
        switchExtTempSensor.setClickable(false);
        switchExtTempSensor.setChecked(true);
        switchOccSensor = view.findViewById(R.id.toggleOccupancy);

        setButton = (Button) view.findViewById(R.id.set2PfcuBtn);

        if (mProfileConfig != null) {
            switchOccSensor.setChecked(mProfileConfig.enableOccupancyControl);
            int offsetIndex = (int) mProfileConfig.temperatureOffset + TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);

            toggleAutoForceOccupied.setChecked(mProfileConfig.enableAutoForceOccupied);
            toggleAutoaway.setChecked(mProfileConfig.enableAutoAway);

            switchThermistor1.setChecked(mProfileConfig.enableThermistor1);

                switchFanMediumY1.setChecked(mProfileConfig.isOpConfigured(Port.RELAY_ONE));
                switchFanHighY2.setChecked(mProfileConfig.isOpConfigured(Port.RELAY_TWO));
                switchFanLowG.setChecked(mProfileConfig.isOpConfigured(Port.RELAY_THREE));
                onCheckedChanged(switchFanLowG,mProfileConfig.isOpConfigured(Port.RELAY_THREE));
                switchHeatingW1.setChecked(mProfileConfig.isOpConfigured(Port.RELAY_FOUR));
                switchWaterValve.setChecked(mProfileConfig.isOpConfigured(Port.RELAY_SIX));

        }else{
            switchFanLowG.setEnabled(true);
            switchFanMediumY1.setEnabled(false);
            switchFanHighY2.setEnabled(false);
        }
        setButton.setOnClickListener(v -> {

            setButton.setEnabled(false);

            ExecutorTask.executeAsync(
                    ()->{
                        ProgressDialogUtils.showProgressDialog(getActivity(),"Saving 2PFCU Configuration");},
                    ()->{
                        CCUHsApi.getInstance().resetCcuReady();
                        setup2PFCUZoneProfile();
                        L.saveCCUState();
                        LSerial.getInstance().sendSeedMessage(true, false, mSmartNodeAddress, roomRef, floorRef);
                        CCUHsApi.getInstance().setCcuReady();
                        DesiredTempDisplayMode.setModeType(roomRef, CCUHsApi.getInstance());
                        },
                    ()->{
                        addBacnetTags(requireContext(), floorRef, roomRef);
                        ProgressDialogUtils.hideProgressDialog();
                        Fragment2PipeFanCoilUnitConfig.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
            );

        });
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewAttachedToWindow(View view) {
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                if (Globals.getInstance().isTestMode()) {
                    Globals.getInstance().setTestMode(false);
                }
            }
        });
    }

    private void setup2PFCUZoneProfile() {

        TwoPipeFanCoilUnitConfiguration twoPfcuConfig = new TwoPipeFanCoilUnitConfiguration();
        twoPfcuConfig.setNodeType(mNodeType);
        twoPfcuConfig.setNodeAddress(mSmartNodeAddress);
        twoPfcuConfig.enableOccupancyControl = switchOccSensor.isChecked();
        twoPfcuConfig.setPriority(ZonePriority.NONE);
        twoPfcuConfig.temperatureOffset = (double) temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        twoPfcuConfig.enableThermistor1 = switchThermistor1.isChecked();
        twoPfcuConfig.enableThermistor2 = switchExtTempSensor.isChecked();

        twoPfcuConfig.enableAutoAway = toggleAutoaway.isChecked();
        twoPfcuConfig.enableAutoForceOccupied = toggleAutoForceOccupied.isChecked();

        if (switchFanMediumY1.isChecked()) {
            Output relay1Op = new Output();
            relay1Op.setAddress(mSmartNodeAddress);
            relay1Op.setPort(Port.RELAY_ONE);
            relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            twoPfcuConfig.getOutputs().add(relay1Op);
        }
        twoPfcuConfig.enableRelay1 = switchFanMediumY1.isChecked();
        if (switchFanHighY2.isChecked()) {
            Output relay2Op = new Output();
            relay2Op.setAddress(mSmartNodeAddress);
            relay2Op.setPort(Port.RELAY_TWO);
            relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            twoPfcuConfig.getOutputs().add(relay2Op);
        }
        twoPfcuConfig.enableRelay2 = switchFanHighY2.isChecked();
        if (switchFanLowG.isChecked()) {
            Output relay3Op = new Output();
            relay3Op.setAddress(mSmartNodeAddress);
            relay3Op.setPort(Port.RELAY_THREE);
            relay3Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            twoPfcuConfig.getOutputs().add(relay3Op);
        }

        twoPfcuConfig.enableRelay3 = switchFanLowG.isChecked();
        if (switchHeatingW1.isChecked()) {
            Output relay4Op = new Output();
            relay4Op.setAddress(mSmartNodeAddress);
            relay4Op.setPort(Port.RELAY_FOUR);
            relay4Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            twoPfcuConfig.getOutputs().add(relay4Op);
        }

        twoPfcuConfig.enableRelay4 = switchHeatingW1.isChecked();
        twoPfcuConfig.enableRelay5 = false;
        if (switchWaterValve.isChecked()) {
            Output relay6Op = new Output();
            relay6Op.setAddress(mSmartNodeAddress);
            relay6Op.setPort(Port.RELAY_SIX);
            relay6Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            twoPfcuConfig.getOutputs().add(relay6Op);
        }
        twoPfcuConfig.enableRelay6 = switchWaterValve.isChecked();

        twoPfcuProfile.getProfileConfiguration().put(mSmartNodeAddress, twoPfcuConfig);
        if (mProfileConfig == null) {
            twoPfcuProfile.addLogicalMapAndPoints(mSmartNodeAddress, twoPfcuConfig, floorRef, roomRef);
        } else {
            twoPfcuProfile.updateLogicalMapAndPoints(mSmartNodeAddress, twoPfcuConfig);
        }
        L.ccu().zoneProfiles.add(twoPfcuProfile);
        CcuLog.d("CPUConfig", "Set Config: Profiles - " + L.ccu().zoneProfiles.size());
    }

    @Override
    @OnCheckedChanged({R.id.test2pfcuRelay1,R.id.test2pfcuRelay2,R.id.test2pfcuRelay3,R.id.test2pfcuRelay4,R.id.test2pfcuRelay6})
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId())
        {
            case R.id.test2pfcuRelay1:
            case R.id.test2pfcuRelay2:
            case R.id.test2pfcuRelay3:
            case R.id.test2pfcuRelay4:
            case R.id.test2pfcuRelay6:
                sendRelayActivationTestSignal();
                break;
            case R.id.toggleFanLow:
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
            case R.id.toggleFanMed2pfcu:
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
        updateSmartStatForceTestControls(mSmartNodeAddress);

        if (testFanMediumY1.isChecked() || testFanHighY2.isChecked() || testFanLowG.isChecked()
                || testAuxHeating.isChecked() || testWaterValve.isChecked()) {
            if (!Globals.getInstance().isTestMode()) {
                Globals.getInstance().setTestMode(true);
            }
        } else {
            if (Globals.getInstance().isTestMode()) {
                Globals.getInstance().setTestMode(false);
            }
        }
    }
    public static double getDesiredTemp(short node)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and group == \""+node+"\"");
        if (point == null || point.isEmpty()) {
            CcuLog.d("2PFCU", " Desired Temp point does not exist for equip , sending 0");
            return 72;
        }
        return CCUHsApi.getInstance().readPointPriorityVal(point.get("id").toString());
    }

    public void updateSmartStatForceTestControls(short node) {
        if (mProfileConfig != null) {
            twoPfcuProfile.setCmdSignal("fan and low", testFanLowG.isChecked() ? 1 : 0, node);
            twoPfcuProfile.setCmdSignal("fan and medium", testFanMediumY1.isChecked() ? 1 : 0, node);
            twoPfcuProfile.setCmdSignal("fan and high", testFanHighY2.isChecked() ? 1 : 0, node);
            twoPfcuProfile.setCmdSignal("pipe2 and fcu and water and valve", testWaterValve.isChecked() ? 1 : 0, node);
            twoPfcuProfile.setCmdSignal("aux and heating", testAuxHeating.isChecked() ? 1 : 0, node);
        }
    }
}
