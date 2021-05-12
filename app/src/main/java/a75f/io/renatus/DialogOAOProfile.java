package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.oao.OAOProfile;
import a75f.io.logic.bo.building.oao.OAOProfileConfiguration;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

import static a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType.TwoToTenV;

/**
 * Created by samjithsadasivan on 6/5/18.
 */

public class DialogOAOProfile extends BaseDialogFragment
{
    public static final String ID = DialogOAOProfile.class.getSimpleName();
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private OAOProfile mProfile;
    private OAOProfileConfiguration mProfileConfig;
    
    private Button setButton;
    
    String floorRef;
    String zoneRef;
    
    static int CT_INDEX_START = 8;
    
    @BindView(R.id.oaDamperAtMin)         Spinner oaDamperAtMin;
    @BindView(R.id.returnDamperAtMin)     Spinner returnDamperAtMin;
    @BindView(R.id.oaDamperMinOpen)       Spinner      oaDamperMinOpen;
    @BindView(R.id.exFanStage1Threshold)  Spinner      exFanStage1Threshold;
    @BindView(R.id.currentTransformerType)Spinner      currentTransformerType;
    @BindView(R.id.exFanHysteresis)       Spinner      exFanHysteresis;
    @BindView(R.id.oaDamperAtMax)         Spinner      oaDamperAtMax;
    @BindView(R.id.returnDamperAtMax)     Spinner      returnDamperAtMax;
    @BindView(R.id.returnDamperMinOpen)   Spinner      returnDamperMinOpen;
    @BindView(R.id.exFanStage2Threshold)  Spinner      exFanStage2Threshold;
    @BindView(R.id.co2Threshold)          Spinner      co2Threshold;
    @BindView(R.id.roomCO2Sensing)        ToggleButton roomCO2Sensing;
    @BindView(R.id.smartPurgeOutsideDamperMinOpen) Spinner smartPurgeOutsideDamperMinOpen;
    @BindView(R.id.enhancedVentiOutsideDamperMinOpen) Spinner enhancedVentilationOutsideDamperMinOpen;

    public DialogOAOProfile()
    {
    }
    
    public static DialogOAOProfile newInstance(short smartNodeAddress, String roomRef, String floorRef)
    {
        DialogOAOProfile f = new DialogOAOProfile();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomRef);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorRef);
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
        if (dialog != null) {
            int width = 1165;//ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = 672;//ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        //setTitle();
    }
    
    private void setTitle() {
        Dialog dialog = getDialog();
        
        if (dialog == null) {
            return;
        }
        dialog.setTitle("VAV Configuration");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(getResources().getColor(R.color.accent75F));
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
        View view = inflater.inflate(R.layout.fragment_oao_config, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        ButterKnife.bind(this, view);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        setButton = view.findViewById(R.id.setBtn);
        
        mProfile = L.ccu().oaoProfile;
        
        setButton.setOnClickListener(v -> {

            setButton.setEnabled(false);
            ProgressDialogUtils.showProgressDialog(getActivity(),"Saving OAO Configuration");

            new Thread(() -> {
                setUpOAOProfile();
                L.saveCCUState();
            }).start();

            new Handler().postDelayed(() -> {
                ProgressDialogUtils.hideProgressDialog();
                DialogOAOProfile.this.closeAllBaseDialogFragments();
                getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
            }, 12000);

        });
    
        ArrayList<Integer> voltsArray = new ArrayList<>();
        for (int val = 0; val <= 10; val++) {
            voltsArray.add(val);
        }
        ArrayAdapter<Integer> voltsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, voltsArray);
        oaDamperAtMin.setAdapter(voltsAdapter);
        returnDamperAtMin.setAdapter(voltsAdapter);
        oaDamperAtMax.setAdapter(voltsAdapter);
        returnDamperAtMax.setAdapter(voltsAdapter);
    
        ArrayList<Integer> percentArray = new ArrayList<>();
        for (int val = 0; val <= 100; val++) {
            percentArray.add(val);
        }
        ArrayAdapter<Integer> percentAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, percentArray);
        oaDamperMinOpen.setAdapter(percentAdapter);
        returnDamperMinOpen.setAdapter(percentAdapter);
        exFanStage1Threshold.setAdapter(percentAdapter);
        exFanStage2Threshold.setAdapter(percentAdapter);
        exFanHysteresis.setAdapter(percentAdapter);
        smartPurgeOutsideDamperMinOpen.setAdapter(percentAdapter);
        enhancedVentilationOutsideDamperMinOpen.setAdapter(percentAdapter);
    
        ArrayList<Integer> co2Array = new ArrayList<>();
        for (int val = 0; val <= 2000; val+=10) {
            co2Array.add(val);
        }
        ArrayAdapter<Integer> co2Adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, co2Array);
        co2Threshold.setAdapter(co2Adapter);
    
    
        ArrayList<String> ctArr = new ArrayList<>();
        ctArr.add("0-10 (A)");
        ctArr.add("0-20 (A)");
        ctArr.add("0-50 (A)");
        ArrayAdapter<String> ctAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, ctArr);
        currentTransformerType.setAdapter(ctAdapter);
        
        if (mProfile != null) {
            CcuLog.d(L.TAG_CCU_UI,  "Get OAOProfile: ");
            mProfileConfig = (OAOProfileConfiguration) mProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            CcuLog.d(L.TAG_CCU_UI, "Create OAOProfile: ");
            mProfile = new OAOProfile();
        }
    
        if (mProfileConfig != null) {
            oaDamperAtMin.setSelection((int)mProfileConfig.outsideDamperAtMinDrive);
            oaDamperAtMax.setSelection((int)mProfileConfig.outsideDamperAtMaxDrive);
            returnDamperAtMin.setSelection((int)mProfileConfig.returnDamperAtMinDrive);
            returnDamperAtMax.setSelection((int)mProfileConfig.returnDamperAtMaxDrive);
            oaDamperMinOpen.setSelection((int)mProfileConfig.outsideDamperMinOpen);
            returnDamperMinOpen.setSelection((int)mProfileConfig.returnDamperMinOpen);
            exFanStage1Threshold.setSelection((int)mProfileConfig.exhaustFanStage1Threshold);
            exFanStage2Threshold.setSelection((int)mProfileConfig.exhaustFanStage2Threshold);
            exFanHysteresis.setSelection((int)mProfileConfig.exhaustFanHysteresis);
            co2Threshold.setSelection(co2Adapter.getPosition((int)mProfileConfig.co2Threshold));
            currentTransformerType.setSelection((int)mProfileConfig.currentTranformerType-CT_INDEX_START);
            roomCO2Sensing.setChecked(mProfileConfig.usePerRoomCO2Sensing);
            smartPurgeOutsideDamperMinOpen.setSelection((int)mProfileConfig.smartPurgeMinDamperOpen,false);
            enhancedVentilationOutsideDamperMinOpen.setSelection((int)mProfileConfig.enhancedVentilationMinDamperOpen,false);
        } else {
            oaDamperAtMin.setSelection(2);
            oaDamperAtMax.setSelection(10);
            returnDamperAtMin.setSelection(2);
            returnDamperAtMax.setSelection(10);
            oaDamperMinOpen.setSelection(0);
            returnDamperMinOpen.setSelection(0);
            exFanStage1Threshold.setSelection(50);
            exFanStage2Threshold.setSelection(90);
            exFanHysteresis.setSelection(5);
            co2Threshold.setSelection(co2Adapter.getPosition(1000));
            currentTransformerType.setSelection(1);
            smartPurgeOutsideDamperMinOpen.setSelection(100,false);
            enhancedVentilationOutsideDamperMinOpen.setSelection(50,false);
        }
    }
    
    private void setUpOAOProfile() {
       
        OAOProfileConfiguration oaoConfig = new OAOProfileConfiguration();
    
        Output analog1Op = new Output();
        analog1Op.setAddress(mSmartNodeAddress);
        analog1Op.setPort(Port.ANALOG_OUT_ONE);
        analog1Op.mOutputAnalogActuatorType = TwoToTenV;
        oaoConfig.getOutputs().add(analog1Op);
    
        Output analog2Op = new Output();
        analog2Op.setAddress(mSmartNodeAddress);
        analog2Op.setPort(Port.ANALOG_OUT_TWO);
        analog2Op.mOutputAnalogActuatorType = TwoToTenV;
        oaoConfig.getOutputs().add(analog2Op);
    
        Output relay1Op = new Output();
        relay1Op.setAddress(mSmartNodeAddress);
        relay1Op.setPort(Port.RELAY_ONE);
        relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;;
        oaoConfig.getOutputs().add(relay1Op);
    
        Output relay2Op = new Output();
        relay2Op.setAddress(mSmartNodeAddress);
        relay2Op.setPort(Port.RELAY_TWO);
        relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
        oaoConfig.getOutputs().add(relay2Op);
        
        oaoConfig.outsideDamperAtMinDrive = Double.parseDouble(oaDamperAtMin.getSelectedItem().toString());
        oaoConfig.outsideDamperAtMaxDrive = Double.parseDouble(oaDamperAtMax.getSelectedItem().toString());
        oaoConfig.returnDamperAtMinDrive = Double.parseDouble(returnDamperAtMin.getSelectedItem().toString());
        oaoConfig.returnDamperAtMaxDrive = Double.parseDouble(returnDamperAtMax.getSelectedItem().toString());
        oaoConfig.co2Threshold = Double.parseDouble(co2Threshold.getSelectedItem().toString());
        oaoConfig.exhaustFanStage1Threshold = Double.parseDouble(exFanStage1Threshold.getSelectedItem().toString());
        oaoConfig.exhaustFanStage2Threshold = Double.parseDouble(exFanStage2Threshold.getSelectedItem().toString());
        oaoConfig.exhaustFanHysteresis = Double.parseDouble(exFanHysteresis.getSelectedItem().toString());
        oaoConfig.smartPurgeMinDamperOpen = Double.parseDouble(smartPurgeOutsideDamperMinOpen.getSelectedItem().toString());
        oaoConfig.enhancedVentilationMinDamperOpen = Double.parseDouble(enhancedVentilationOutsideDamperMinOpen.getSelectedItem().toString());
        oaoConfig.currentTranformerType = CT_INDEX_START + currentTransformerType.getSelectedItemPosition();
        oaoConfig.usePerRoomCO2Sensing = roomCO2Sensing.isChecked();
        oaoConfig.outsideDamperMinOpen = Double.parseDouble(oaDamperMinOpen.getSelectedItem().toString());
        oaoConfig.returnDamperMinOpen = Double.parseDouble(returnDamperMinOpen.getSelectedItem().toString());
        if (mProfileConfig == null) {
            mProfile.addOaoEquip(mSmartNodeAddress, oaoConfig, floorRef, zoneRef );
        } else {
            mProfile.updateOaoEquip(oaoConfig);
        }
        L.ccu().oaoProfile = mProfile;
        CcuLog.d(L.TAG_CCU_UI, "Set OAO Config");
        
    }

    
}
