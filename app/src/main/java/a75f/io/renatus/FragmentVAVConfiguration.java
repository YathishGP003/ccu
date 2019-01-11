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

import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.vav.VavParallelFanProfile;
import a75f.io.logic.bo.building.vav.VavProfile;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.logic.bo.building.vav.VavReheatProfile;
import a75f.io.logic.bo.building.vav.VavSeriesFanProfile;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.ButterKnife;

import static a75f.io.logic.bo.building.definitions.ReheatType.OneStage;
import static a75f.io.logic.bo.building.definitions.ReheatType.TwoStage;

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
    //Spinner      damper2Type;
    //Spinner      damperActuator;
    //Spinner      reheatPort;
    //Spinner      reheatActuator;
    Spinner reheatType;
    LinearLayout reheatOptionLayout;
    Button       setButton;
    Spinner      zonePriority;
    NumberPicker maxCoolingDamperPos;
    NumberPicker minCoolingDamperPos;
    NumberPicker maxHeatingDamperPos;
    NumberPicker minHeatingDamperPos;
    
    Damper mDamper;
    
    private ProfileType mProfileType;
    private VavProfile mVavProfile;
    private VavProfileConfiguration mProfileConfig;
    
    private ArrayList<Damper.Parameters> mDampers = new ArrayList<Damper.Parameters>();
    ArrayAdapter<String> damperTypesAdapter;
    ArrayAdapter<String> reheatTypesAdapter;
    
    //ArrayAdapter<String> damperActuatorAdapter;
    //ArrayAdapter<String> relayActuatorAdapter;
    //ArrayAdapter<String> reheatPortAdapter;
    
    DamperType damperTypeSelected;
    //int damperActuatorSelection;
    ReheatType reheatTypeSelected;
    //int reheatActuatorSelection;
    
    String floorRef;
    String zoneRef;
    
    public FragmentVAVConfiguration()
    {
    }
    
    public static FragmentVAVConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType)
    {
        FragmentVAVConfiguration f = new FragmentVAVConfiguration();
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
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        //mZone = L.findZoneByName(mFloorName, mRoomName);
        mProfileType = ProfileType.values()[getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)];
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        reheatOptionLayout = (LinearLayout)view.findViewById(R.id.dabReheatOptions);
        
        reheatType = view.findViewById(R.id.vavReheatType);
        //damperActuator = (Spinner) view.findViewById(R.id.vavDamperActuator);
        //reheatPort = view.findViewById(R.id.vavReheatPort);
        //reheatActuator = (Spinner)view.findViewById(R.id.vavReheatActuator);
        setButton = (Button) view.findViewById(R.id.setBtn);
    
        mVavProfile = (VavProfile) L.getProfile(mSmartNodeAddress);
    
        if (mVavProfile != null) {
            Log.d("VAVConfig", "Get Config: ");
            mProfileConfig = (VavProfileConfiguration) mVavProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            Log.d("VAVConfig", "Create Profile: ");
            switch (mProfileType) {
                case VAV_REHEAT:
                    mVavProfile = new VavReheatProfile();
                    break;
                case VAV_SERIES_FAN:
                    mVavProfile = new VavSeriesFanProfile();
                    break;
                case VAV_PARALLEL_FAN:
                    mVavProfile = new VavParallelFanProfile();
                    break;
            }
            
        }
        
        //fillDamperDetails();
    
        damperType = view.findViewById(R.id.damperType);
    
        ArrayList<String> damperTypes = new ArrayList<>();
        for (DamperType damper : DamperType.values()) {
            damperTypes.add(damper.displayName);
        }
        damperTypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, damperTypes);
        damperTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        damperType.setAdapter(damperTypesAdapter);
        
        ArrayList<String> reheatTypes = new ArrayList<>();
        for (ReheatType actuator : ReheatType.values()) {
            reheatTypes.add(actuator.displayName);
        }
        reheatTypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, reheatTypes);
        reheatTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        reheatType.setAdapter(reheatTypesAdapter);
    
        if (mProfileConfig != null) {
            for (Output op : mProfileConfig.getOutputs()) {
                Log.d("VAVConfig", " Config Outputs: "+op.getPort());
                if (op.getPort() == Port.ANALOG_OUT_ONE) {
                    damperType.setSelection(damperTypesAdapter.getPosition(op.getAnalogActuatorType()), false);
                    damperTypeSelected = DamperType.getEnum(op.getAnalogActuatorType());
                } else if (op.getPort() == Port.ANALOG_OUT_TWO) {
                    reheatTypeSelected = ReheatType.getEnum(op.getAnalogActuatorType());
                    reheatType.setSelection(2, false);
                    reheatType.setAdapter(reheatTypesAdapter);
                    reheatType.setSelection(reheatTypesAdapter.getPosition(op.getAnalogActuatorType()), false);
                } else if (op.getPort() == Port.RELAY_ONE) {
                    reheatTypeSelected = OneStage;
                    reheatType.setSelection(0, false);
                    reheatType.setAdapter(reheatTypesAdapter);
                    reheatType.setSelection(reheatTypesAdapter.getPosition(op.getRelayActuatorType()), false);
                } else if (op.getPort() == Port.RELAY_TWO) {
                    reheatTypeSelected = TwoStage;
                    reheatType.setSelection(1, false);
                    reheatType.setAdapter(reheatTypesAdapter);
                    reheatType.setSelection(reheatTypesAdapter.getPosition(op.getRelayActuatorType()), false);
                }
            }
        }
    
        damperType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                damperTypeSelected = DamperType.values()[position];
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
    
        reheatType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reheatTypeSelected = ReheatType.values()[position];
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        damper1layout  = (LinearLayout)view.findViewById(R.id.damper1layout);
        
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
       
        final SwitchCompat useOccupancyDetection = (SwitchCompat) view.findViewById(R.id.useOccupancyDetection);
        //useOccupancyDetection.setChecked(mFSVData.getUseOccupancyDetection());
    
        final SwitchCompat ignoreSetpoint = (SwitchCompat) view.findViewById(R.id.ignoreSetpoint);
        //ignoreSetpoint.setChecked(mFSVData.getIgnoreSetpointChange());
    
        LinearLayout zonePriorityLayout = (LinearLayout) view.findViewById(R.id.zonePriorityLayout);
        //zonePriorityLayout.setVisibility((SystemSettingsData.getTier().ordinal() <= CCU_TIER.EXPERT.ordinal()) ? View.VISIBLE : View.GONE);
    
        if (mProfileConfig != null) {
            minCoolingDamperPos.setValue(mProfileConfig.getMinDamperCooling());
            maxCoolingDamperPos.setValue(mProfileConfig.getMaxDamperCooliing());
            minHeatingDamperPos.setValue(mProfileConfig.getMinDamperHeating());
            maxHeatingDamperPos.setValue(mProfileConfig.getMaxDamperHeating());
            zonePriority.setSelection(mProfileConfig.getPriority().ordinal());
        } else {
            reheatType.setSelection(2, false);
            zonePriority.setSelection(1);//LOW
        }
    
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
    
                new AsyncTask<Void, Void, Void>() {
    
                    ProgressDialog progressDlg = new ProgressDialog(getActivity());
    
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        progressDlg.setMessage("Saving VAV Configuration");
                        progressDlg.show();
                        super.onPreExecute();
                    }
    
                    @Override
                    protected Void doInBackground( final Void ... params ) {
                        setupVavZoneProfile();
                        L.saveCCUState();
            
                        return null;
                    }
        
                    @Override
                    protected void onPostExecute( final Void result ) {
                        progressDlg.dismiss();
                        FragmentVAVConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                
            }
        });
    
    }
    
    private void setupVavZoneProfile() {
        
        VavProfileConfiguration vavConfig = new VavProfileConfiguration();
        vavConfig.setNodeType(mNodeType);
        vavConfig.setNodeAddress(mSmartNodeAddress);
        vavConfig.setPriority(ZonePriority.values()[zonePriority.getSelectedItemPosition()]);
        vavConfig.setMinDamperCooling(minCoolingDamperPos.getValue());
        vavConfig.setMaxDamperCooliing(maxCoolingDamperPos.getValue());
        vavConfig.setMinDamperHeating(minHeatingDamperPos.getValue());
        vavConfig.setMaxDamperHeating(maxHeatingDamperPos.getValue());
    
        Output analog1Op = new Output();
        analog1Op.setAddress(mSmartNodeAddress);
        analog1Op.setPort(Port.ANALOG_OUT_ONE);
        analog1Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(damperTypeSelected.displayName);
        vavConfig.getOutputs().add(analog1Op);
    
        switch (reheatTypeSelected) {
            case ZeroToTenV:
            case TwoToTenV:
            case TenToZeroV:
            case TenToTwov:
            case Pulse:
                Output analog2Op = new Output();
                analog2Op.setAddress(mSmartNodeAddress);
                analog2Op.setPort(Port.ANALOG_OUT_TWO);
                analog2Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(reheatTypeSelected.displayName);
                vavConfig.getOutputs().add(analog2Op);
                break;
            case OneStage:
                Output relay2Op = new Output();
                relay2Op.setAddress(mSmartNodeAddress);
                relay2Op.setPort(Port.RELAY_TWO);
                relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
                vavConfig.getOutputs().add(relay2Op);
            case TwoStage:
                Output relay1Op = new Output();
                relay1Op.setAddress(mSmartNodeAddress);
                relay1Op.setPort(Port.RELAY_ONE);
                relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;;
                vavConfig.getOutputs().add(relay1Op);
                break;
                
        }
        mVavProfile.getProfileConfiguration().put(mSmartNodeAddress, vavConfig);
        if (mProfileConfig == null) {
            BuildingTuners.getInstance().addDefaultVavTuners();
            mVavProfile.addLogicalMapAndPoints(mSmartNodeAddress, vavConfig, floorRef, zoneRef);
        } else
        {
            mVavProfile.updateLogicalMapAndPoints(mSmartNodeAddress, vavConfig);
        }
        L.ccu().zoneProfiles.add(mVavProfile);
        Log.d("VAVConfig", "Set Config: Profiles - "+L.ccu().zoneProfiles.size());
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
            /*case R.id.damperType2:
                damper2Type.setSelection(position);
                if(position == 4)
                    damper2layout.setVisibility(View.INVISIBLE);
                else
                    damper2layout.setVisibility(View.VISIBLE);
                damper2layout.invalidate();
                break;*/
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
    
    
    public void fillDamperDetails() {
        mDampers.add(Damper.TYPE.GENERIC_0To10V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_0To10V.ordinal(),
                                                                                        Damper.TYPE.GENERIC_0To10V.toString(),
                                                                                        0, 0, 0, 0, 0));
        mDampers.add(Damper.TYPE.GENERIC_2TO10V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_2TO10V.ordinal(),
                                                                                        Damper.TYPE.GENERIC_2TO10V.toString(),
                                                                                        0, 0, 0, 0, 0));
        mDampers.add(Damper.TYPE.GENERIC_10To0V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To0V.ordinal(),
                                                                                        Damper.TYPE.GENERIC_10To0V.toString(),
                                                                                        0, 0, 0, 0, 0));
        mDampers.add(Damper.TYPE.GENERIC_10To2V.ordinal(), new Damper.Parameters(Damper.TYPE.GENERIC_10To2V.ordinal(),
                                                                                        Damper.TYPE.GENERIC_10To2V.toString(),
                                                                                        0, 0, 0, 0, 0));
        mDampers.add(Damper.TYPE.MAT_RADIAL1.ordinal(), new Damper.Parameters(Damper.TYPE.MAT_RADIAL1.ordinal(),
                                                                                     Damper.TYPE.MAT_RADIAL1.toString(),
                                                                                     Damper.TYPE.MAT_RADIAL1.getDefaultMotorRPM(),
                                                                                     Damper.TYPE.MAT_RADIAL1.getDefaultOperatingCurrent(),
                                                                                     Damper.TYPE.MAT_RADIAL1.getDefaultStallCurrent(),
                                                                                     Damper.TYPE.MAT_RADIAL1.getDefaultForwardBacklash(),
                                                                                     Damper.TYPE.MAT_RADIAL1.getDefaultReverseBacklash()));
    }
    
    
    
    
    
    
    
}
