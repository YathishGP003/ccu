package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.truecfm.TrueCFMConstants;
import a75f.io.renatus.util.RxjavaUtil;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;

import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.DamperShape;
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
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;



/**
 * Created by samjithsadasivan on 6/5/18.
 */

public class FragmentVAVConfiguration extends BaseDialogFragment implements AdapterView.OnItemSelectedListener, CheckBox.OnCheckedChangeListener
{
    public static final String ID = FragmentVAVConfiguration.class.getSimpleName();
    
    static final int TEMP_OFFSET_LIMIT = 100;
    static final int STEP = 10;
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private Zone     mZone;
    
    LinearLayout damper1layout;
    LinearLayout damper2layout;
    Spinner      damperType;
    Spinner      damperSize;
    Spinner      damperShape;
    //Spinner      damper2Type;
    Spinner reheatType;
    LinearLayout reheatOptionLayout;
    Button       setButton;
    Spinner      zonePriority;
    NumberPicker temperatureOffset;
    NumberPicker maxCoolingDamperPos;
    NumberPicker minCoolingDamperPos;
    NumberPicker maxHeatingDamperPos;
    NumberPicker minHeatingDamperPos;
    ToggleButton enableOccupancyControl;
    ToggleButton enableCO2Control;
    ToggleButton enableIAQControl;
    LinearLayout minCFMReheating;
    LinearLayout maxCFMReheating;
    LinearLayout maxCFMCooling;
    LinearLayout minCFMCooling;
    LinearLayout minDamperPosCooling;
    LinearLayout maxDamperPosCooling;
    LinearLayout minDamperPosHeating;
    TextView textEnableCFM;
    TextView textKFactor;
    ToggleButton enableCFMControl;
    NumberPicker numMaxCFMCooling;
    NumberPicker numMinCFMCooling;
    NumberPicker numMinCFMReheating;
    NumberPicker numMaxCFMReheating;
    Spinner kFactor;
    
    Damper mDamper;
    
    private ProfileType mProfileType;
    private VavProfile mVavProfile;
    private VavProfileConfiguration mProfileConfig;
    
    private ArrayList<Damper.Parameters> mDampers = new ArrayList<Damper.Parameters>();
    ArrayAdapter<String> damperTypesAdapter;
    ArrayAdapter<String> reheatTypesAdapter;
    ArrayAdapter<CharSequence> damperSizeAdapter;
    ArrayAdapter<String> damperShapeAdapter;
    ArrayAdapter<String> kFactorValues;
    
    DamperType damperTypeSelected;
    //int damperActuatorSelection;
    ReheatType reheatTypeSelected;
    //int reheatActuatorSelection;
    
    String floorRef;
    String zoneRef;
    
    @BindView(R.id.textTitleFragment) TextView configTitle;
    @BindView(R.id.relay1TextView) TextView relay1TextView;
    @BindView(R.id.relay1TextVal) TextView relay1TextVal;
    @BindView(R.id.relay2TextView) TextView relay2TextView;
    @BindView(R.id.relay2TextVal) TextView relay2TextVal;
    
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
            titleView.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
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
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        mProfileType = ProfileType.values()[getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)];
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        reheatOptionLayout = (LinearLayout)view.findViewById(R.id.dabReheatOptions);
        
        reheatType = view.findViewById(R.id.vavReheatType);
        setButton = (Button) view.findViewById(R.id.setBtn);
    
        mVavProfile = (VavProfile) L.getProfile(mSmartNodeAddress);
    
        if (mVavProfile != null) {
            CcuLog.d(L.TAG_CCU_UI,  "Get VavConfig: ");
            mProfileConfig = (VavProfileConfiguration) mVavProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            CcuLog.d(L.TAG_CCU_UI, "Create Vav Profile: ");
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
    
        initializeAdapters(view);
        
        initializeNumberPickers(view);

        enableOccupancyControl = view.findViewById(R.id.enableOccupancyControl);
        enableCO2Control = view.findViewById(R.id.enableCO2Control);
        enableIAQControl = view.findViewById(R.id.enableIAQControl);

        textEnableCFM=view.findViewById(R.id.textEnableCFM);
        enableCFMControl=view.findViewById(R.id.enableCFMControl);
        textKFactor=view.findViewById(R.id.textKFactor);
        kFactor=view.findViewById(R.id.spinKFactor);
        maxCFMCooling=view.findViewById(R.id.maxCFMCooling);
        minCFMCooling=view.findViewById(R.id.minCFMCooling);
        minCFMReheating=view.findViewById(R.id.minCFMReheating);
        maxCFMReheating=view.findViewById(R.id.maxCFMReheating);
        maxDamperPosCooling=view.findViewById(R.id.maxDamperPosCooling);
        minDamperPosCooling=view.findViewById(R.id.minDamperPosCooling);
        minDamperPosHeating=view.findViewById(R.id.minDamperPosHeating);

        enableCFMControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (enableCFMControl.isChecked()) {
                maxCFMReheating.setVisibility(View.VISIBLE);
                maxCFMCooling.setVisibility(View.VISIBLE);
                minCFMReheating.setVisibility(View.VISIBLE);
                minCFMCooling.setVisibility(View.VISIBLE);
                kFactor.setVisibility(View.VISIBLE);
                textKFactor.setVisibility(View.VISIBLE);
                minDamperPosHeating.setVisibility(View.GONE);
                minDamperPosCooling.setVisibility(View.GONE);
                maxDamperPosCooling.setVisibility(View.GONE);
            } else {
                maxCFMReheating.setVisibility(View.GONE);
                maxCFMCooling.setVisibility(View.GONE);
                minCFMReheating.setVisibility(View.GONE);
                minCFMCooling.setVisibility(View.GONE);
                kFactor.setVisibility(View.GONE);
                textKFactor.setVisibility(View.GONE);
                minDamperPosHeating.setVisibility(View.VISIBLE);
                minDamperPosCooling.setVisibility(View.VISIBLE);
                maxDamperPosCooling.setVisibility(View.VISIBLE);
            }
        });





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
                setReheatTypeText(reheatTypeSelected);
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
    
        initProfileConfig();
        
        configureSetButton();
        
        initializeViews();
    
    }
    
    private void initializeAdapters(View view) {
        damperSize = view.findViewById(R.id.damperSize);
        damperSizeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.damper_size, R.layout.spinner_dropdown_item);
        damperSizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damperSize.setAdapter(damperSizeAdapter);
    
        ArrayList<String> damperShapes = new ArrayList<>();
        for (DamperShape shape : DamperShape.values()) {
            damperShapes.add(shape.displayName);
        }
        damperShapeAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_dropdown_item, damperShapes);
        damperShapeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damperShape = view.findViewById(R.id.damperShape);
        damperShape.setAdapter(damperShapeAdapter);
    
        ArrayList<String> damperTypes = new ArrayList<>();
        for (DamperType damper : DamperType.values()) {
            damperTypes.add(damper.displayName);
        }
        damperTypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, damperTypes);
        damperTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        damperType = view.findViewById(R.id.damperType);
        damperType.setAdapter(damperTypesAdapter);
    
        ArrayList<String> reheatTypes = new ArrayList<>();
        for (ReheatType actuator : ReheatType.values()) {
            reheatTypes.add(actuator.displayName);
        }
    
        if (mProfileType != ProfileType.VAV_REHEAT) {
            reheatTypes.remove(ReheatType.TwoStage.displayName);
        }
        reheatTypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, reheatTypes);
        reheatTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    
        reheatType.setAdapter(reheatTypesAdapter);
    
        zonePriority = view.findViewById(R.id.zonePriority);
        ArrayAdapter<CharSequence> zonePriorityAdapter = ArrayAdapter.createFromResource(getActivity(),
                                                                                         R.array.zone_priority, R.layout.spinner_dropdown_item);
        zonePriorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        zonePriority.setAdapter(zonePriorityAdapter);
        
    }
    
    private void initializeNumberPickers(View view) {
        temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);

        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        temperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);
    
        maxCoolingDamperPos = view.findViewById(R.id.maxDamperPos);

        maxCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxCoolingDamperPos.setMinValue(0);
        maxCoolingDamperPos.setMaxValue(100);
        maxCoolingDamperPos.setValue(100);
        maxCoolingDamperPos.setWrapSelectorWheel(false);
    
        minCoolingDamperPos = view.findViewById(R.id.minDamperPos);

        minCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minCoolingDamperPos.setMinValue(0);
        minCoolingDamperPos.setMaxValue(100);
        minCoolingDamperPos.setValue(20);
        minCoolingDamperPos.setWrapSelectorWheel(false);
    
        maxHeatingDamperPos = view.findViewById(R.id.maxHeatingDamperPos);

        maxHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxHeatingDamperPos.setMinValue(0);
        maxHeatingDamperPos.setMaxValue(100);
        maxHeatingDamperPos.setValue(100);
        maxHeatingDamperPos.setWrapSelectorWheel(false);
    
        minHeatingDamperPos = view.findViewById(R.id.minHeatingDamperPos);

        minHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minHeatingDamperPos.setMinValue(0);
        minHeatingDamperPos.setMaxValue(100);
        minHeatingDamperPos.setValue(20);
        minHeatingDamperPos.setWrapSelectorWheel(false);

        numMaxCFMCooling = view.findViewById(R.id.numMaxCFMCooling);
        String[] numberValues = new String[TrueCFMConstants.MAX_VAL - TrueCFMConstants.MIN_VAL + 1];
        for (int i = 0; i < numberValues.length; i++) {
            numberValues[i] = String.valueOf(i * STEP);
        }
        numMaxCFMCooling.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        numMaxCFMCooling.setMinValue(0);
        numMaxCFMCooling.setMaxValue(150);
        numMaxCFMCooling.setValue(TrueCFMConstants.DEFAULT_VALUE);
        numMaxCFMCooling.setDisplayedValues(numberValues);
        numMaxCFMCooling.setWrapSelectorWheel(false);

        numMinCFMCooling = view.findViewById(R.id.numMinCFMCooling);
        numMinCFMCooling.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        numMinCFMCooling.setMinValue(0);
        numMinCFMCooling.setMaxValue(50);
        numMinCFMCooling.setValue(TrueCFMConstants.DEFAULT_VALUE);
        numMinCFMCooling.setWrapSelectorWheel(false);
        numMinCFMCooling.setDisplayedValues(numberValues);

        numMinCFMReheating = view.findViewById(R.id.numMinCFMReheating);
        numMinCFMReheating.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        numMinCFMReheating.setMinValue(0);
        numMinCFMReheating.setMaxValue(50);
        numMinCFMReheating.setValue(TrueCFMConstants.DEFAULT_VALUE);
        numMinCFMReheating.setWrapSelectorWheel(false);
        numMinCFMReheating.setDisplayedValues(numberValues);

        numMaxCFMReheating = view.findViewById(R.id.numMaxCFMReheating);
        numMaxCFMReheating.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        numMaxCFMReheating.setMinValue(0);
        numMaxCFMReheating.setMaxValue(150);
        numMaxCFMReheating.setValue(TrueCFMConstants.DEFAULT_VALUE);
        numMaxCFMReheating.setWrapSelectorWheel(false);
        numMaxCFMReheating.setDisplayedValues(numberValues);

        kFactor = view.findViewById(R.id.spinKFactor);
        textKFactor=view.findViewById(R.id.textKFactor);
        kFactor = view.findViewById(R.id.spinKFactor);
        ArrayList<String> spinnerArray = new ArrayList<>();

        DecimalFormat df = new DecimalFormat("0.00");
        for (double i = TrueCFMConstants.MIN_VAL_FOR_K_Factor; i < TrueCFMConstants.MAX_VAL_FOR_K_Factor; i = i + TrueCFMConstants.STEP_K_FACTOR) {
            spinnerArray.add(df.format(i));
        }
        kFactorValues = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, spinnerArray);
        kFactorValues.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        kFactor.setAdapter(kFactorValues);
        kFactor.setSelection(TrueCFMConstants.DEFAULT_VAL_FOR_K_FACTOR);

    }

    
    private void initializeViews() {
        if (mProfileConfig != null) {
            damperType.setSelection(damperTypesAdapter.getPosition(DamperType.values()[mProfileConfig.damperType].displayName), false);
            damperSize.setSelection(damperSizeAdapter.getPosition(String.valueOf(mProfileConfig.damperSize)), false);
            damperShape.setSelection(damperShapeAdapter.getPosition(DamperShape.values()[mProfileConfig.damperShape].displayName), false);
            reheatType.setSelection(reheatTypesAdapter.getPosition(ReheatType.values()[mProfileConfig.reheatType].displayName), false);
            enableOccupancyControl.setChecked(mProfileConfig.enableOccupancyControl);
            enableCO2Control.setChecked(mProfileConfig.enableCO2Control);
            enableIAQControl.setChecked(mProfileConfig.enableIAQControl);
            zonePriority.setSelection(mProfileConfig.getPriority().ordinal());
            int offsetIndex = (int)mProfileConfig.temperaturOffset+TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);
            minCoolingDamperPos.setValue(mProfileConfig.minDamperCooling);
            maxCoolingDamperPos.setValue(mProfileConfig.maxDamperCooling);
            minHeatingDamperPos.setValue(mProfileConfig.minDamperHeating);
            maxHeatingDamperPos.setValue(mProfileConfig.maxDamperHeating);
            setReheatTypeText(ReheatType.values()[reheatType.getSelectedItemPosition()]);

            enableCFMControl.setChecked(mProfileConfig.enableCFMControl);
            if (!enableCFMControl.isChecked()) {
                numMaxCFMCooling.setValue(50);
                numMinCFMCooling.setValue(50);
                numMaxCFMReheating.setValue(50);
                numMinCFMReheating.setValue(50);
                kFactor.setSelection(100);
            } else {
                numMinCFMCooling.setValue((mProfileConfig.numMinCFMCooling)/STEP);
                numMaxCFMCooling.setValue((mProfileConfig.nuMaxCFMCooling)/STEP);
                numMinCFMReheating.setValue((mProfileConfig.numMinCFMReheating)/STEP);
                numMinCFMCooling.setMaxValue((mProfileConfig.nuMaxCFMCooling)/STEP);
                numMinCFMReheating.setMaxValue((mProfileConfig.numMaxCFMReheating)/STEP);
                numMaxCFMReheating.setValue((mProfileConfig.numMaxCFMReheating)/STEP);
                //this converts value of KFactor to position
                kFactor.setSelection((int) Math.ceil(((mProfileConfig.kFactor)*100)-100));
                minCoolingDamperPos.setValue(20);
                maxCoolingDamperPos.setValue(20);
                minHeatingDamperPos.setValue(20);
            }
        
        } else {
            zonePriority.setSelection(2);//NORMAL
        }
    }
    
    private void configureSetButton() {
        
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
            
                new AsyncTask<String, Void, Void>() {
                
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        ProgressDialogUtils.showProgressDialog(getActivity(),"Saving VAV Configuration");
                        super.onPreExecute();
                    }
                
                    @Override
                    protected Void doInBackground( final String ... params ) {
                        CCUHsApi.getInstance().resetCcuReady();
                        setupVavZoneProfile();
                        L.saveCCUState();
                        CCUHsApi.getInstance().syncEntityTree();
                        CCUHsApi.getInstance().setCcuReady();
                        return null;
                    }
                
                    @Override
                    protected void onPostExecute( final Void result ) {
                        ProgressDialogUtils.hideProgressDialog();
                        FragmentVAVConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                        RxjavaUtil.executeBackground(() -> LSerial.getInstance()
                                   .sendSeedMessage(false,false, mSmartNodeAddress, zoneRef,floorRef));
                        
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            
            }
        });
    }
    
    private void updateRelayMappingVisibility(int visibility) {
        relay1TextView.setVisibility(visibility);
        relay1TextVal.setVisibility(visibility);
        relay2TextView.setVisibility(visibility);
        relay2TextVal.setVisibility(visibility);
    }
    
    private void initProfileConfig() {
        
        if (mProfileType == ProfileType.VAV_REHEAT) {
            
            configTitle.setText(R.string.title_vavnofan);
            updateRelayMappingVisibility(View.GONE);
            
        } else if (mProfileType == ProfileType.VAV_SERIES_FAN) {
            
            configTitle.setText(R.string.title_vav_seriesfan);
            updateRelayMappingVisibility(View.VISIBLE);
            relay2TextVal.setText(R.string.vav_label_series_fan);
            
        } else if (mProfileType == ProfileType.VAV_PARALLEL_FAN) {
            
            configTitle.setText(R.string.title_vav_parallelfan);
            updateRelayMappingVisibility(View.VISIBLE);
            relay2TextVal.setText(R.string.vav_label_parallel_fan);
        }
    }
    
    private void setReheatTypeText(ReheatType reheatType) {
        if (reheatType == ReheatType.ZeroToTenV ||
            reheatType == ReheatType.TenToZeroV ||
            reheatType == ReheatType.TwoToTenV ||
            reheatType == ReheatType.TenToTwov) {
            relay1TextView.setText(R.string.vav_label_analog_out_2);
            relay1TextVal.setText(R.string.vav_label_modulating_reheat);
        } else {
            relay1TextView.setText(R.string.vav_label_relay1);
            relay1TextVal.setText(R.string.vav_label_staged_heater);
        }
    }
    
    private void setupVavZoneProfile() {

        VavProfileConfiguration vavConfig = new VavProfileConfiguration();
        vavConfig.damperType = damperTypeSelected.ordinal();
        vavConfig.damperSize = Integer.parseInt(damperSize.getSelectedItem().toString());
        vavConfig.damperShape = DamperType.values()[damperShape.getSelectedItemPosition()].ordinal();
        vavConfig.reheatType = reheatTypeSelected.ordinal();
        vavConfig.setNodeType(mNodeType);
        vavConfig.setNodeAddress(mSmartNodeAddress);
        vavConfig.enableOccupancyControl = enableOccupancyControl.isChecked();
        vavConfig.enableCO2Control = enableCO2Control.isChecked();
        vavConfig.enableIAQControl = enableIAQControl.isChecked();
        vavConfig.setPriority(ZonePriority.values()[zonePriority.getSelectedItemPosition()]);
        vavConfig.minDamperCooling = (minCoolingDamperPos.getValue());
        vavConfig.maxDamperCooling = (maxCoolingDamperPos.getValue());
        vavConfig.minDamperHeating = (minHeatingDamperPos.getValue());
        vavConfig.maxDamperHeating = (maxHeatingDamperPos.getValue());
        vavConfig.temperaturOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        vavConfig.numMinCFMCooling=numMinCFMCooling.getValue()*STEP;
        vavConfig.nuMaxCFMCooling=numMaxCFMCooling.getValue()*STEP;
        vavConfig.numMinCFMReheating=numMinCFMReheating.getValue()*STEP;
        vavConfig.numMaxCFMReheating=numMaxCFMReheating.getValue()*STEP;
        vavConfig.enableCFMControl=enableCFMControl.isChecked();
        vavConfig.kFactor = (((kFactor.getSelectedItemPosition()-100)*(.01))+2);
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
            case TwoStage:
                Output relay2Op = new Output();
                relay2Op.setAddress(mSmartNodeAddress);
                relay2Op.setPort(Port.RELAY_TWO);
                relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
                vavConfig.getOutputs().add(relay2Op);
            case OneStage:
                Output relay1Op = new Output();
                relay1Op.setAddress(mSmartNodeAddress);
                relay1Op.setPort(Port.RELAY_ONE);
                relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;;
                vavConfig.getOutputs().add(relay1Op);
                break;
                
        }
        
        if (mProfileType != ProfileType.VAV_REHEAT) {
            Output relay2Op = new Output();
            relay2Op.setAddress(mSmartNodeAddress);
            relay2Op.setPort(Port.RELAY_TWO);
            relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
            vavConfig.getOutputs().add(relay2Op);
        }
        
        mVavProfile.getProfileConfiguration().put(mSmartNodeAddress, vavConfig);
        if (mProfileConfig == null) {
            mVavProfile.addLogicalMapAndPoints(mSmartNodeAddress, vavConfig, floorRef, zoneRef);
        } else
        {
            mVavProfile.updateLogicalMapAndPoints(mSmartNodeAddress, vavConfig);
        }
        L.ccu().zoneProfiles.add(mVavProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set Vav Config: Profiles - "+L.ccu().zoneProfiles.size());
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
                /*if(position == 4)
                    damper1layout.setVisibility(View.INVISIBLE);
                else
                    damper1layout.setVisibility(View.VISIBLE);
                damper1layout.invalidate();*/
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
}
