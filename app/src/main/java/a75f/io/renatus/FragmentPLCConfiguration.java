package a75f.io.renatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Sensor;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.plc.PlcProfileConfiguration;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 2/22/19.
 */

public class FragmentPLCConfiguration extends BaseDialogFragment
{
    public static final String TAG = "PlcConfig";
    public static final String ID = FragmentPLCConfiguration.class.getSimpleName();
    
    @BindView(R.id.analog1InSensor)
    Spinner analog1InSensorSp;
    
    @BindView(R.id.targetVal)
    Spinner targetValSp;
    
    @BindView(R.id.th1InSensor)
    Spinner th1InSensorSp;
    
    @BindView(R.id.errorRange)
    Spinner errorRangeSp;
    
    @BindView(R.id.analog2InSensor)
    Spinner analog2InSensorSp;
    
    @BindView(R.id.sensorOffset)
    Spinner sensorOffsetSp;
    
    @BindView(R.id.analogout1AtMin)
    Spinner analogout1AtMinSp;
    
    @BindView(R.id.analogout1AtMax)
    Spinner analogout1AtMaxSp;
    
    @BindView(R.id.analog2DynamicSp)
    ToggleButton analog2DynamicSP;
    
    @BindView(R.id.zeroErrorAtMp)
    ToggleButton zeroErrorAtMP;
    
    @BindView(R.id.setBtn)
    Button setButton;

    @BindView(R.id.setPointLayout)
    RelativeLayout setPointLayout;
    
    private ProfileType             mProfileType;
    private PlcProfile              mPlcProfile;
    private PlcProfileConfiguration mProfileConfig;
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    
    String floorRef;
    String zoneRef;
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    public FragmentPLCConfiguration()
    {
    }
    
    public static FragmentPLCConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName)
    {
        FragmentPLCConfiguration f = new FragmentPLCConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
        f.setArguments(bundle);
        return f;
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
        dialog.setTitle("PI LOOP CONTROLLER");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(getResources().getColor(R.color.progress_color_orange));
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
        View view = inflater.inflate(R.layout.fragment_plc_config, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        ButterKnife.bind(this, view);

        if (getResources().getDisplayMetrics().xdpi == (float)149.824){
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 70, 0, 0);
            setPointLayout.setLayoutParams(lp);
        }
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    
        mPlcProfile = (PlcProfile) L.getProfile(mSmartNodeAddress);
    
        if (mPlcProfile != null) {
            CcuLog.d(L.TAG_CCU_UI,  "Get PlcConfig: ");
            mProfileConfig = (PlcProfileConfiguration) mPlcProfile.getProfileConfiguration(mSmartNodeAddress);
        } else
        {
            CcuLog.d(L.TAG_CCU_UI, "Create Plc Profile: ");
            mPlcProfile = new PlcProfile();
        }
        
        ArrayList<String> analog1InArr = new ArrayList<>();
        analog1InArr.add("Not Used");
        for (Sensor r : Sensor.getSensorList()) {
            analog1InArr.add(r.sensorName+" "+r.engineeringUnit);
        }
        ArrayList<String> th1InArr = new ArrayList<>();
        th1InArr.add("Not Used");
        for (Thermistor m : Thermistor.getThermistorList()) {
            th1InArr.add(m.sensorName+" "+m.engineeringUnit);
        }
        
        ArrayAdapter<String> analog1InAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, analog1InArr);
        analog1InAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        analog1InSensorSp.setAdapter(analog1InAdapter);
        analog1InSensorSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                Log.d("CCU_UI"," analog1InSensorSp Selected : "+i);
                if (i == 0) {
                    return;
                }
                ArrayList<Double> targetVal = new ArrayList<Double>();
                Sensor r = Sensor.getSensorList().get(i-1);
                for (int pos = (int)(100*r.minEngineeringValue); pos <= (100*r.maxEngineeringValue); pos+=(100*r.incrementEgineeringValue)) {
                    targetVal.add(pos /100.0);
                }
                ArrayAdapter<Double> targetValAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, targetVal);
                targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                targetValSp.setAdapter(targetValAdapter);
                targetValSp.invalidate();
                if (mProfileConfig != null) {
                    targetValSp.setSelection(targetValAdapter.getPosition(mProfileConfig.pidTargetValue), false);
                } else {
                    targetValSp.setSelection(targetValAdapter.getPosition(5.0), false);
                }
                th1InSensorSp.setSelection(0, false);
                
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
        
        
        ArrayAdapter<String> th1InAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, th1InArr);
        th1InAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        th1InSensorSp.setAdapter(th1InAdapter);
        th1InSensorSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                Log.d("CCU_UI"," th1InSensorSp Selected : "+i);
                if (i == 0) {
                    return;
                }
                ArrayList<Double> targetVal = new ArrayList<Double>();
                Sensor r = Sensor.getSensorList().get(i-1);
                for (int pos = (int)(10*r.minEngineeringValue); pos <= (10*r.maxEngineeringValue); pos+=(10*r.incrementEgineeringValue)) {
                    targetVal.add(pos /10.0);
                }
                ArrayAdapter<Double> targetValAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, targetVal);
                targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                targetValSp.setAdapter(targetValAdapter);
                targetValSp.invalidate();
                if (mProfileConfig != null) {
                    targetValSp.setSelection(targetValAdapter.getPosition(mProfileConfig.pidTargetValue), false);
                } else {
                    targetValSp.setSelection(targetValAdapter.getPosition(5.0), false);
                }
                analog1InSensorSp.setSelection(0, false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
        
        ArrayList<Double> targetVal = new ArrayList<Double>();
        for (int pos = 1; pos <= 100; pos++) {
            targetVal.add((double)pos/10);
        }
        ArrayAdapter<Double> targetValAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, targetVal);
        targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetValSp.setAdapter(targetValAdapter);
        
        ArrayList<Double> errRange = new ArrayList<Double>();
        for (double pos = 1; pos <= 10; pos+=1) {
            errRange.add(pos);
        }
        ArrayAdapter<Double> errRangeAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, errRange);
        errRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        errorRangeSp.setAdapter(errRangeAdapter);
        
        ArrayList<String> analog2InArr = new ArrayList<>();
        for (Sensor r : Sensor.getSensorList()) {
            analog2InArr.add(r.sensorName+" "+r.engineeringUnit);
        }
        ArrayAdapter<String> analgo2InAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, analog2InArr);
        analgo2InAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        analog2InSensorSp.setAdapter(analgo2InAdapter);
        analog2InSensorSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                Log.d("CCU_UI"," analog2InSensorSp Selected : "+i);
                ArrayList<Double> targetVal = new ArrayList<Double>();
                Sensor r = Sensor.getSensorList().get(i);
                for (int pos = (int)(100 * (r.minEngineeringValue < 0 ? r.minEngineeringValue : -1*r.maxEngineeringValue)); pos <= (100* r.maxEngineeringValue); pos+= (100 * r.incrementEgineeringValue)) {
                    targetVal.add(pos /100.0);
                }
                ArrayAdapter<Double> offsetAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, targetVal);
                offsetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sensorOffsetSp.setAdapter(offsetAdapter);
                sensorOffsetSp.invalidate();
                if (mProfileConfig != null) {
                    sensorOffsetSp.setSelection(offsetAdapter.getPosition(mProfileConfig.setpointSensorOffset), false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
        
        
        ArrayList<Integer> analogArray = new ArrayList<>();
        for (int a = 0; a <= 10; a++)
        {
            analogArray.add(a);
        }
        ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, analogArray);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        analogout1AtMinSp.setAdapter(analogAdapter);
        analogout1AtMaxSp.setAdapter(analogAdapter);
        
        ArrayList<Double> offsetArr = new ArrayList<Double>();
        for (double pos = 0.1; pos <= 10; pos+=0.1) {
            offsetArr.add(Math.round(pos * 10) /10.0);
        }
        ArrayAdapter<Double> offsetAdapter = new ArrayAdapter<Double>(getActivity(), R.layout.spinner_dropdown_item, offsetArr);
        offsetAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sensorOffsetSp.setAdapter(offsetAdapter);
        analog2DynamicSP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                analog2InSensorSp.setEnabled(b);
                sensorOffsetSp.setEnabled(b);
                targetValSp.setEnabled(!b);
            }
        });
        
        ArrayList<String> analogTypes = new ArrayList<>();
        for (OutputAnalogActuatorType actuator : OutputAnalogActuatorType.values()) {
            analogTypes.add(actuator.displayName);
        }
        
        zeroErrorAtMP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
            }
        });
    
    
        if (mProfileConfig != null) {
            analog1InSensorSp.setSelection(mProfileConfig.analog1InputSensor, false);
            th1InSensorSp.setSelection(mProfileConfig.th1InputSensor, false);
            targetValSp.setSelection(targetValAdapter.getPosition(mProfileConfig.pidTargetValue), false);
            errorRangeSp.setSelection(errRangeAdapter.getPosition(mProfileConfig.pidProportionalRange), false);
            analog2DynamicSP.setChecked(mProfileConfig.useAnalogIn2ForSetpoint);
            sensorOffsetSp.setSelection(offsetAdapter.getPosition(mProfileConfig.setpointSensorOffset), false);
            analog2InSensorSp.setSelection(mProfileConfig.analog2InputSensor, false);
            
            zeroErrorAtMP.setChecked(mProfileConfig.expectZeroErrorAtMidpoint);
            analogout1AtMinSp.setSelection(analogAdapter.getPosition((int)mProfileConfig.analog1AtMinOutput));
            analogout1AtMaxSp.setSelection(analogAdapter.getPosition((int)mProfileConfig.analog1AtMaxOutput));
    
            analog2InSensorSp.setEnabled(mProfileConfig.useAnalogIn2ForSetpoint);
            sensorOffsetSp.setEnabled(mProfileConfig.useAnalogIn2ForSetpoint);
            if (mProfileConfig.useAnalogIn2ForSetpoint) {
                targetValSp.setEnabled(false);
            }
        } else {
            analogout1AtMaxSp.setSelection(analogAdapter.getPosition(10), false);
            analog1InSensorSp.setSelection(1, false);
            th1InSensorSp.setSelection(0,false);
            analog2InSensorSp.setEnabled(false);
            sensorOffsetSp.setEnabled(false);
            targetValSp.setSelection(targetValAdapter.getPosition(5.0), false);
            errorRangeSp.setSelection(errRangeAdapter.getPosition(2.0), false);
        }
    
        setButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!analog2DynamicSP.isChecked()
                    && analog1InSensorSp.getSelectedItem().toString().equals("Not Used")
                    && th1InSensorSp.getSelectedItem().toString().equals("Not Used")) {
                    Toast.makeText(getActivity(), "Select an Input Sensor",Toast.LENGTH_LONG).show();
                    return;
                }
                
                new AsyncTask<String, Void, Void>() {

        
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        ProgressDialogUtils.showProgressDialog(getActivity(),"Saving PLC Configuration");
                        super.onPreExecute();
                    }
        
                    @Override
                    protected Void doInBackground( final String ... params ) {
                        setupPlcProfile();
                        L.saveCCUState();
            
                        return null;
                    }
        
                    @Override
                    protected void onPostExecute( final Void result ) {
                        ProgressDialogUtils.hideProgressDialog();
                        FragmentPLCConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                        LSerial.getInstance().sendSeedMessage(false,false, mSmartNodeAddress, zoneRef,floorRef);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            }
        });
        
    }
    
    public void setupPlcProfile() {
        PlcProfileConfiguration p = new PlcProfileConfiguration();
        p.analog1InputSensor = analog1InSensorSp.getSelectedItemPosition();
        p.pidTargetValue = Double.parseDouble(targetValSp.getSelectedItem().toString());
        p.th1InputSensor = th1InSensorSp.getSelectedItemPosition();
        p.pidProportionalRange = Double.parseDouble(errorRangeSp.getSelectedItem().toString());
        p.useAnalogIn2ForSetpoint = analog2DynamicSP.isChecked();
        p.analog2InputSensor = analog2InSensorSp.getSelectedItemPosition();
        p.expectZeroErrorAtMidpoint = zeroErrorAtMP.isChecked();
        p.analog1AtMinOutput = Double.parseDouble(analogout1AtMinSp.getSelectedItem().toString());
        p.analog1AtMaxOutput = Double.parseDouble(analogout1AtMaxSp.getSelectedItem().toString());
        p.setpointSensorOffset = Double.parseDouble(sensorOffsetSp.getSelectedItem().toString());
    
        mPlcProfile.getProfileConfiguration().put(mSmartNodeAddress, p);
        if (mProfileConfig == null) {
            mPlcProfile.addPlcEquip(mSmartNodeAddress, p, floorRef, zoneRef );
        } else {
            mPlcProfile.updatePlcEquip(p);
        }
        L.ccu().zoneProfiles.add(mPlcProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set Plc Config: Profiles - "+L.ccu().zoneProfiles.size());
    }
}
