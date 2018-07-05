package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;

import a75f.io.bo.building.NodeType;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.VavProfile;
import a75f.io.bo.building.VavProfileConfiguration;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.hvac.Damper;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 6/5/18.
 */

public class FragmentVAVConfiguration extends BaseDialogFragment implements AdapterView.OnItemSelectedListener, CheckBox.OnCheckedChangeListener
{
    public static final String ID = FragmentVAVConfiguration.class.getSimpleName();
    
    static final int TEMP_OFFSET_LIMIT = 100;
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private Zone     mZone;
    
    LinearLayout damper1layout;
    LinearLayout damper2layout;
    Spinner      damperType;
    Spinner      damper2Type;
    Spinner      damperActuator;
    Spinner      reheatActuator;
    LinearLayout reheatOptionLayout;
    Button       setButton;
    Spinner      zonePriority;
    NumberPicker maxCoolingDamperPos;
    NumberPicker minCoolingDamperPos;
    NumberPicker maxHeatingDamperPos;
    NumberPicker minHeatingDamperPos;
    
    Damper mDamper;
    
    private VavProfile mVavProfile;
    private VavProfileConfiguration mProfileConfig;
    
    private ArrayList<Damper.Parameters> mDampers = new ArrayList<Damper.Parameters>();
    ArrayAdapter<String> analogoutActuatorAdapter;
    ArrayAdapter<String> relayActuatorAdapter;
    int damperActuatorSelection;
    int reheatActuatorSelection;
    
    public FragmentVAVConfiguration()
    {
    }
    
    public static FragmentVAVConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName)
    {
        FragmentVAVConfiguration f = new FragmentVAVConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
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
            int width = ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        setTitle();
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
        View view = inflater.inflate(R.layout.fragment_vav_config, container, false);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        String mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        String mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        mZone = L.findZoneByName(mFloorName, mRoomName);
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        //fsvDabReheatMode = (CheckBox)view.findViewById(R.id.fsvDabReheatOption);
        //fsvDabReheatMode.setOnCheckedChangeListener(this);
        reheatOptionLayout = (LinearLayout)view.findViewById(R.id.dabReheatOptions);
        //reheatPort = (Spinner)view.findViewById(R.id.dabReheatSpinner);
        damperActuator = (Spinner) view.findViewById(R.id.vavDamperActuator);
        reheatActuator = (Spinner)view.findViewById(R.id.vavReheatActuator);
        setButton = (Button) view.findViewById(R.id.setBtn);
    
        mVavProfile = (VavProfile) mZone.findProfile(ProfileType.VAV);
    
        if (mVavProfile != null) {
            mProfileConfig = (VavProfileConfiguration) mVavProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            mVavProfile = new VavProfile();
        }
    
    
        fillDamperDetails();
    
        /* ArrayAdapter<CharSequence> relay1Adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.reheat_port, R.layout.spinner_dropdown_item);
        relay1Adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        reheatPort.setAdapter(relay1Adapter);
        reheatPort.setOnItemSelectedListener(this);
        */
        ArrayList<String> analogTypes = new ArrayList<>();
        for (OutputAnalogActuatorType actuator : OutputAnalogActuatorType.values()) {
            analogTypes.add(actuator.displayName);
        }
        analogoutActuatorAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, analogTypes);
        analogoutActuatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    
        ArrayList<String> relays = new ArrayList<>();
        for (OutputRelayActuatorType actuator : OutputRelayActuatorType.values()) {
            relays.add(actuator.displayName);
        }
        relayActuatorAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, relays);
        relayActuatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    
    
        //reheatPort.setSelection(0);//TODO
        damperActuator.setAdapter(analogoutActuatorAdapter);
        damperActuator.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                damperActuatorSelection = position;
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        reheatActuator.setAdapter(analogoutActuatorAdapter);
    
        reheatActuator.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reheatActuatorSelection = position;
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
    
        damper1layout  = (LinearLayout)view.findViewById(R.id.damper1layout);
        damperType = (Spinner) view.findViewById(R.id.damperType);
        ArrayAdapter<Damper.Parameters> damperTypeAdapter = new ArrayAdapter<Damper.Parameters>(getActivity(), R.layout.spinner_dropdown_item, mDampers);
        damperTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damperType.setAdapter(damperTypeAdapter);
        damperType.setOnItemSelectedListener(this);
        //damperType.setSelection(mFSVData.getDamperType());
        //if(mFSVData.getDamperType() != 4)
            damper1layout.setVisibility(View.VISIBLE);
    
        final Spinner damperSize = (Spinner) view.findViewById(R.id.damperSize);
        ArrayAdapter<CharSequence> damperSizeAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.damper_size, R.layout.spinner_dropdown_item);
        damperSizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damperSize.setAdapter(damperSizeAdapter);
        //damperSize.setSelection((mFSVData.getDamperSize()-4)/2);
        String[] damp_shape_arr = {"Round", "Square"};
        ArrayAdapter<String> damperShapeAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_dropdown_item, damp_shape_arr);
        damperShapeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
    
        /*// Add second damper details
        damper2Type = (Spinner) view.findViewById(R.id.damperType2);
        ArrayAdapter<Damper.Parameters> damper2TypeAdapter = new ArrayAdapter<Damper.Parameters>(getActivity(), R.layout.spinner_dropdown_item, mDampers);
        damper2TypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damper2Type.setAdapter(damper2TypeAdapter);
        damper2Type.setOnItemSelectedListener(this);
        //damper2Type.setSelection(mFSVData.getDamper2Type());
        damper2layout = (LinearLayout)view.findViewById(R.id.damper2layout);
        final Spinner damper2Size = (Spinner) view.findViewById(R.id.damperSize2);
        //if(mFSVData.getDamper2Type() != 4)
            damper2layout.setVisibility(View.VISIBLE);
    
        ArrayAdapter<CharSequence> damper2SizeAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.damper_size, R.layout.spinner_dropdown_item);
        damper2SizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damper2Size.setAdapter(damper2SizeAdapter);
        //damper2Size.setSelection((mFSVData.getDamper2Size() - 4) / 2);
    
        final Spinner damper2Shape = (Spinner) view.findViewById(R.id.damperShape2);
        damper2Shape.setAdapter(damperShapeAdapter);*/
    
        final Spinner damperShape = (Spinner) view.findViewById(R.id.damperShape);
        damperShape.setAdapter(damperShapeAdapter);
        //damperShape.setSelection(mFSVData.getDamperShape());
        //damper2Shape.setSelection(mFSVData.getDamper2Shape());
        final NumberPicker temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);
        setNumberPickerDividerColor(temperatureOffset);
        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        //temperatureOffset.setValue(mFSVData.getAttachedDamper().getTemperatureOffset() + TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);
    
        maxCoolingDamperPos = view.findViewById(R.id.maxDamperPos);
        setNumberPickerDividerColor(maxCoolingDamperPos);
        maxCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxCoolingDamperPos.setMinValue(0);
        maxCoolingDamperPos.setMaxValue(100);
        maxCoolingDamperPos.setValue(80);
        maxCoolingDamperPos.setWrapSelectorWheel(false);
    
        minCoolingDamperPos = view.findViewById(R.id.minDamperPos);
        setNumberPickerDividerColor(minCoolingDamperPos);
        minCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minCoolingDamperPos.setMinValue(0);
        minCoolingDamperPos.setMaxValue(100);
        minCoolingDamperPos.setValue(40);
        minCoolingDamperPos.setWrapSelectorWheel(false);
    
        maxHeatingDamperPos = view.findViewById(R.id.maxHeatingDamperPos);
        setNumberPickerDividerColor(maxHeatingDamperPos);
        maxHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxHeatingDamperPos.setMinValue(0);
        maxHeatingDamperPos.setMaxValue(100);
        maxHeatingDamperPos.setValue(80);
        maxHeatingDamperPos.setWrapSelectorWheel(false);
    
        minHeatingDamperPos = view.findViewById(R.id.minHeatingDamperPos);
        setNumberPickerDividerColor(minHeatingDamperPos);
        minHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minHeatingDamperPos.setMinValue(0);
        minHeatingDamperPos.setMaxValue(100);
        minHeatingDamperPos.setValue(40);
        minHeatingDamperPos.setWrapSelectorWheel(false);
    
    
        zonePriority = view.findViewById(R.id.zonePriority);
        ArrayAdapter<CharSequence> zonePriorityAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.zone_priority, R.layout.spinner_dropdown_item);
        zonePriorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        zonePriority.setAdapter(zonePriorityAdapter);
        if (mProfileConfig != null)
        {
            zonePriority.setSelection(mProfileConfig.getPriority());
        } else {
            zonePriority.setSelection(1);//LOW
        }
    
        final SwitchCompat useOccupancyDetection = (SwitchCompat) view.findViewById(R.id.useOccupancyDetection);
        //useOccupancyDetection.setChecked(mFSVData.getUseOccupancyDetection());
    
        final SwitchCompat ignoreSetpoint = (SwitchCompat) view.findViewById(R.id.ignoreSetpoint);
        //ignoreSetpoint.setChecked(mFSVData.getIgnoreSetpointChange());
    
        LinearLayout zonePriorityLayout = (LinearLayout) view.findViewById(R.id.zonePriorityLayout);
        //zonePriorityLayout.setVisibility((SystemSettingsData.getTier().ordinal() <= CCU_TIER.EXPERT.ordinal()) ? View.VISIBLE : View.GONE);
    
        if (mVavProfile.getProfileConfiguration(mSmartNodeAddress) != null) {
            VavProfileConfiguration config = (VavProfileConfiguration) mVavProfile.getProfileConfiguration(mSmartNodeAddress);
            minCoolingDamperPos.setValue(config.getMinDamperCooling());
            maxCoolingDamperPos.setValue(config.getMaxDamperCooliing());
            minHeatingDamperPos.setValue(config.getMinDamperHeating());
            maxHeatingDamperPos.setValue(config.getMaxDamperHeating());
        }
    
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setupVavZoneProfile();
            
                L.saveCCUState();
                getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                FragmentVAVConfiguration.this.closeAllBaseDialogFragments();
            }
        });
    
    }
    
    private void setupVavZoneProfile() {
        Output analogOne = new Output();
        analogOne.setAddress(mSmartNodeAddress);
        analogOne.setPort(Port.ANALOG_OUT_ONE);
        analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.values()[damperActuatorSelection];
        Output analogTwo = new Output();
        analogTwo.setAddress(mSmartNodeAddress);
        analogTwo.setPort(Port.ANALOG_OUT_TWO);
        analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.values()[reheatActuatorSelection];
        
        VavProfileConfiguration vavConfig = new VavProfileConfiguration();
        vavConfig.setNodeType(mNodeType);
        vavConfig.setNodeAddress(mSmartNodeAddress);
        vavConfig.setPriority(zonePriority.getSelectedItemPosition());
        vavConfig.setMinDamperCooling(minCoolingDamperPos.getValue());
        vavConfig.setMaxDamperCooliing(maxCoolingDamperPos.getValue());
        vavConfig.setMinDamperHeating(minHeatingDamperPos.getValue());
        vavConfig.setMaxDamperHeating(maxHeatingDamperPos.getValue());
        vavConfig.getOutputs().add(analogOne);
        vavConfig.getOutputs().add(analogTwo);
        
        mVavProfile.getProfileConfiguration().put(mSmartNodeAddress, vavConfig);
        
        if (mZone.findProfile(ProfileType.VAV) == null)
            mZone.mZoneProfiles.add(mVavProfile);
    
        Log.d("VAVConfig", "Set Config: ");
    
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
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        
        switch (parent.getId()){
            case R.id.damperType:
                damperType.setSelection(position);
                if(position == 4)
                    damper1layout.setVisibility(View.INVISIBLE);
                else
                    damper1layout.setVisibility(View.VISIBLE);
                damper1layout.invalidate();
                break;
            case R.id.damperType2:
                damper2Type.setSelection(position);
                if(position == 4)
                    damper2layout.setVisibility(View.INVISIBLE);
                else
                    damper2layout.setVisibility(View.VISIBLE);
                damper2layout.invalidate();
                break;
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.fsvDabReheatOption:
                /*if(isChecked)
                    reheatOptionLayout.setVisibility(View.VISIBLE);
                else
                    reheatOptionLayout.setVisibility(View.INVISIBLE);
                reheatOptionLayout.invalidate();*/
        }
    }
    
    //TODO - TEMP
    public void fillDamperDetails() {
        //SharedPreferences damperSettings = CCUApp.getAppContext().getSharedPreferences(SHARED_PREF_FILE, 0);
        int nNumOfDampers = 5;// damperSettings.getInt(NO_OF_DAMPERS, 0);
    
        if (nNumOfDampers == 0 || nNumOfDampers == 5) {
        
            mDampers.add(Damper.TYPE.MAT_RADIAL1.ordinal(), new Damper.Parameters(Damper.TYPE.MAT_RADIAL1.ordinal(),
                                                                                        Damper.TYPE.MAT_RADIAL1.toString(),
                                                                                        Damper.TYPE.MAT_RADIAL1.getDefaultMotorRPM(),
                                                                                        Damper.TYPE.MAT_RADIAL1.getDefaultOperatingCurrent(),
                                                                                        Damper.TYPE.MAT_RADIAL1.getDefaultStallCurrent(),
                                                                                        Damper.TYPE.MAT_RADIAL1.getDefaultForwardBacklash(),
                                                                                        Damper.TYPE.MAT_RADIAL1.getDefaultReverseBacklash()));
            mDampers.add(Damper.TYPE.MAT_RADIAL2.ordinal(), new Damper.Parameters(Damper.TYPE.MAT_RADIAL2.ordinal(),
                                                                                        Damper.TYPE.MAT_RADIAL2.toString(),
                                                                                        Damper.TYPE.MAT_RADIAL2.getDefaultMotorRPM(),
                                                                                        Damper.TYPE.MAT_RADIAL2.getDefaultOperatingCurrent(),
                                                                                        Damper.TYPE.MAT_RADIAL2.getDefaultStallCurrent(),
                                                                                        Damper.TYPE.MAT_RADIAL2.getDefaultForwardBacklash(),
                                                                                        Damper.TYPE.MAT_RADIAL2.getDefaultReverseBacklash()));
            mDampers.add(Damper.TYPE.GENERIC_0To10V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_0To10V.ordinal(),
                                                                                           Damper.TYPE.GENERIC_0To10V.toString(),
                                                                                           0, 0, 0, 0, 0));
            mDampers.add(Damper.TYPE.GENERIC_2TO10V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_2TO10V.ordinal(),
                                                                                           Damper.TYPE.GENERIC_2TO10V.toString(),
                                                                                           0, 0, 0, 0, 0));
            mDampers.add(Damper.TYPE.NOT_INSTALLED.ordinal(), new Damper.Parameters(Damper.TYPE.NOT_INSTALLED.ordinal(),
                                                                                          Damper.TYPE.NOT_INSTALLED.toString(),
                                                                                          0, 0, 0, 0, 0));
            mDampers.add(Damper.TYPE.GENERIC_10To0V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To0V.ordinal(),
                                                                                           Damper.TYPE.GENERIC_10To0V.toString(),
                                                                                           0, 0, 0, 0, 0));
            mDampers.add(Damper.TYPE.GENERIC_10To2V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To2V.ordinal(),
                                                                                           Damper.TYPE.GENERIC_10To2V.toString(),
                                                                                           0, 0, 0, 0, 0));
            //save();
        }
        else {
           /* for (int nCount = 0; nCount < nNumOfDampers; nCount++) {
                int nDamperType = damperSettings.getInt("damper_type"+ String.valueOf(nCount), -1);
                String sName = damperSettings.getString("damper_name" + String.valueOf(nCount), "");
                if (sName.compareToIgnoreCase(DAMPER_TYPE.values()[nCount].toString()) != 0)
                    sName = DAMPER_TYPE.values()[nCount].toString();
                int nMotorRPM = damperSettings.getInt("motor_rpm" + String.valueOf(nCount), 0);
                int nOperatingCurrent = damperSettings.getInt("operating_current" + String.valueOf(nCount), 0);
                int nStallCurrent = damperSettings.getInt("stall_current" + String.valueOf(nCount), 0);
                int nForwardBacklash = damperSettings.getInt("forward_backlash" + String.valueOf(nCount), 0);
                int nReverseBacklash = damperSettings.getInt("reverse_backlash" + String.valueOf(nCount), 0);
                if (nDamperType != -1)
                    mDampers.add(nDamperType, new DamperParameters(nDamperType, sName, nMotorRPM, nOperatingCurrent, nStallCurrent, nForwardBacklash, nReverseBacklash));
            }*/
            if(nNumOfDampers == 5){
                mDampers.add(Damper.TYPE.GENERIC_10To0V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To0V.ordinal(),
                                                                                               Damper.TYPE.GENERIC_10To0V.toString(),
                                                                                               0, 0, 0, 0, 0));
                mDampers.add(Damper.TYPE.GENERIC_10To2V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To2V.ordinal(),
                                                                                               Damper.TYPE.GENERIC_10To2V.toString(),
                                                                                               0, 0, 0, 0, 0));
                //save();
            }else if(nNumOfDampers == 6){
                mDampers.clear();
                mDampers.add(Damper.TYPE.MAT_RADIAL1.ordinal(), new Damper.Parameters(Damper.TYPE.MAT_RADIAL1.ordinal(),
                                                                                            Damper.TYPE.MAT_RADIAL1.toString(),
                                                                                            Damper.TYPE.MAT_RADIAL1.getDefaultMotorRPM(),
                                                                                            Damper.TYPE.MAT_RADIAL1.getDefaultOperatingCurrent(),
                                                                                            Damper.TYPE.MAT_RADIAL1.getDefaultStallCurrent(),
                                                                                            Damper.TYPE.MAT_RADIAL1.getDefaultForwardBacklash(),
                                                                                            Damper.TYPE.MAT_RADIAL1.getDefaultReverseBacklash()));
                mDampers.add(Damper.TYPE.MAT_RADIAL2.ordinal(), new Damper.Parameters(Damper.TYPE.MAT_RADIAL2.ordinal(),
                                                                                            Damper.TYPE.MAT_RADIAL2.toString(),
                                                                                            Damper.TYPE.MAT_RADIAL2.getDefaultMotorRPM(),
                                                                                            Damper.TYPE.MAT_RADIAL2.getDefaultOperatingCurrent(),
                                                                                            Damper.TYPE.MAT_RADIAL2.getDefaultStallCurrent(),
                                                                                            Damper.TYPE.MAT_RADIAL2.getDefaultForwardBacklash(),
                                                                                            Damper.TYPE.MAT_RADIAL2.getDefaultReverseBacklash()));
                mDampers.add(Damper.TYPE.GENERIC_0To10V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_0To10V.ordinal(),
                                                                                               Damper.TYPE.GENERIC_0To10V.toString(),
                                                                                               0, 0, 0, 0, 0));
                mDampers.add(Damper.TYPE.GENERIC_2TO10V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_2TO10V.ordinal(),
                                                                                               Damper.TYPE.GENERIC_2TO10V.toString(),
                                                                                               0, 0, 0, 0, 0));
                mDampers.add(Damper.TYPE.NOT_INSTALLED.ordinal(), new Damper.Parameters(Damper.TYPE.NOT_INSTALLED.ordinal(),
                                                                                              Damper.TYPE.NOT_INSTALLED.toString(),
                                                                                              0, 0, 0, 0, 0));
                mDampers.add(Damper.TYPE.GENERIC_10To0V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To0V.ordinal(),
                                                                                               Damper.TYPE.GENERIC_10To0V.toString(),
                                                                                               0, 0, 0, 0, 0));
                mDampers.add(Damper.TYPE.GENERIC_10To2V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To2V.ordinal(),
                                                                                               Damper.TYPE.GENERIC_10To2V.toString(),
                                                                                               0, 0, 0, 0, 0));
                //save();
            }
        }
    }
    
    
    
    
    
    
    
}
