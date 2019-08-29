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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.sse.SingleStageConfig;
import a75f.io.logic.bo.building.sse.SingleStageProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

/**
 * Created by Anilkumar on 8/22/2019.
 */

public class FragmentSSEConfiguration  extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener {
    public static final String ID = FragmentCPUConfiguration.class.getSimpleName();
    static final int TEMP_OFFSET_LIMIT = 100;
    String floorRef;
    String roomRef;
    private ProfileType mProfileType;
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private SingleStageProfile mSSEProfile;
    private SingleStageConfig mProfileConfig;

    ToggleButton switchCoolHeatR1;
    Spinner sseRelay1Actuator;
    Spinner sseRelay2Actuator;
    @BindView(R.id.sseRelay1ForceTestBtn)ToggleButton testCoolHeatRelay1;
    ToggleButton switchFanR2;
    @BindView(R.id.sseRelay2ForceTestBtn)ToggleButton testFanRelay2;
    ToggleButton switchExtTempSensor;
    ToggleButton switchAirflowTempSensor;
    Button setButton;
    NumberPicker temperatureOffset;
    public FragmentSSEConfiguration()
    {
    }

    public static FragmentSSEConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType)
    {
        FragmentSSEConfiguration f = new FragmentSSEConfiguration();
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
        View view = inflater.inflate(R.layout.fragment_sse_control_details, container, false);
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

        mSSEProfile = (SingleStageProfile) L.getProfile(mSmartNodeAddress);

        if (mSSEProfile != null) {
            Log.d("SSEConfig", "Get Config: ");
            mProfileConfig = (SingleStageConfig) mSSEProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            Log.d("SSEConfig", "Create Profile: ");
            mSSEProfile = new SingleStageProfile();

        }

        switchCoolHeatR1 = (ToggleButton) view.findViewById(R.id.sseRelay1Switch);
        switchFanR2 = (ToggleButton) view.findViewById(R.id.sseRelay2Switch);
        switchAirflowTempSensor = (ToggleButton)view.findViewById(R.id.sse_thermister1_switch);
        switchExtTempSensor = (ToggleButton)view.findViewById(R.id.sse_thermister2_switch);
        temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);
        sseRelay1Actuator = (Spinner)view.findViewById(R.id.sseRelay1Actuator);
        sseRelay2Actuator = (Spinner)view.findViewById(R.id.sseRelay2Actuator);
        setDividerColor(temperatureOffset);
        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        temperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);

        ArrayList<Integer> temp = new ArrayList<Integer>();
        for (int pos = 0; pos <= 150; pos++)
            temp.add(pos);
        ArrayAdapter<Integer> tempRange = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_spinner_item, temp);
        tempRange.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> sseRelay1TypeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.sse_relay1_mode, R.layout.spinner_dropdown_item);
        sseRelay1TypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sseRelay1Actuator.setAdapter(sseRelay1TypeAdapter);
        ArrayAdapter<CharSequence> sseRelay2TypeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.sse_relay2_mode, R.layout.spinner_dropdown_item);
        sseRelay2TypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sseRelay2Actuator.setAdapter(sseRelay2TypeAdapter);
        setButton = (Button) view.findViewById(R.id.setBtn);

        if (mProfileConfig != null) {
            int offsetIndex = (int)mProfileConfig.temperaturOffset+TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);
            switchExtTempSensor.setChecked(mProfileConfig.enableThermistor2);
            switchAirflowTempSensor.setChecked(mProfileConfig.enableThermistor1);
            sseRelay1Actuator.setSelection(mProfileConfig.enableRelay1 -1,false);
            sseRelay2Actuator.setSelection(mProfileConfig.enableRelay2,false);
            if(mProfileConfig.getOutputs().size() > 0) {
                for(Output output : mProfileConfig.getOutputs()) {
                    switch (output.getPort()) {
                        case RELAY_ONE:
                            switchCoolHeatR1.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                        case RELAY_TWO:
                            switchFanR2.setChecked(mProfileConfig.isOpConfigured(output.getPort()));
                            break;
                    }
                }
            }
        }else{
            sseRelay2Actuator.setSelection(1,false);
        }
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                new AsyncTask<String, Void, Void>() {

                    ProgressDialog progressDlg = new ProgressDialog(getActivity());

                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        progressDlg.setMessage("Saving SSE Configuration");
                        progressDlg.show();
                        super.onPreExecute();
                    }

                    @Override
                    protected Void doInBackground( final String ... params ) {
                        setupSSEZoneProfile();
                        L.saveCCUState();

                        return null;
                    }

                    @Override
                    protected void onPostExecute( final Void result ) {
                        progressDlg.dismiss();
                        FragmentSSEConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

            }
        });
    }

    private void setupSSEZoneProfile() {

        SingleStageConfig sseConfig = new SingleStageConfig();
        sseConfig.setNodeType(mNodeType);
        sseConfig.setNodeAddress(mSmartNodeAddress);
        sseConfig.setPriority(ZonePriority.NONE);
        sseConfig.temperaturOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        sseConfig.enableThermistor1 = switchAirflowTempSensor.isChecked();
        sseConfig.enableThermistor2 = switchExtTempSensor.isChecked();
        if(switchCoolHeatR1.isChecked()) sseConfig.enableRelay1 = sseRelay1Actuator.getSelectedItemPosition()+1;
        else sseConfig.enableRelay1 = 0;
        if(switchFanR2.isChecked()) sseConfig.enableRelay2 = sseRelay2Actuator.getSelectedItemPosition();
        else sseConfig.enableRelay2 = 0;


        if(switchCoolHeatR1.isChecked()) {
            Output relay1Op = new Output();
            relay1Op.setAddress(mSmartNodeAddress);
            relay1Op.setPort(Port.RELAY_ONE);
            relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            sseConfig.getOutputs().add(relay1Op);
        }
        if(switchFanR2.isChecked()){
            Output relay2Op = new Output();
            relay2Op.setAddress(mSmartNodeAddress);
            relay2Op.setPort(Port.RELAY_TWO);
            relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            sseConfig.getOutputs().add(relay2Op);
        }

        mSSEProfile.getProfileConfiguration().put(mSmartNodeAddress, sseConfig);
        if (mProfileConfig == null) {
            mSSEProfile.addSSEEquip(mSmartNodeAddress, sseConfig, floorRef, roomRef);
        } else
        {
            mSSEProfile.updateSSEEquip(mSmartNodeAddress, sseConfig,roomRef);
        }
        L.ccu().zoneProfiles.add(mSSEProfile);
        Log.d("SSEConfig", "Set Config: Profiles - "+L.ccu().zoneProfiles.size());
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
    @OnCheckedChanged({R.id.sseRelay1ForceTestBtn,R.id.sseRelay2ForceTestBtn})
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId())
        {
            case R.id.sseRelay1ForceTestBtn:
                sendRelayActivationTestSignal();
                break;
            case R.id.sseRelay2ForceTestBtn:
                sendRelayActivationTestSignal();
                break;
        }
    }

    public void sendRelayActivationTestSignal() {
        CcuToCmOverUsbSnControlsMessage_t msg = new CcuToCmOverUsbSnControlsMessage_t();
        msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
        msg.smartNodeAddress.set(mSmartNodeAddress);
        msg.controls.setTemperature.set((short)(getDesiredTemp(mSmartNodeAddress)*2));
        msg.controls.digitalOut1.set((short)(testCoolHeatRelay1.isChecked() ? 1 : 0));
        msg.controls.digitalOut2.set((short)(testFanRelay2.isChecked() ? 1 : 0));
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