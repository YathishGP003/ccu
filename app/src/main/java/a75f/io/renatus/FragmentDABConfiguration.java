package a75f.io.renatus;

import static a75f.io.logic.bo.building.definitions.DamperType.ZeroToTenV;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.dab.DabProfileConfiguration;
import a75f.io.logic.bo.building.definitions.DamperShape;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import androidx.annotation.Nullable;

import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.RxjavaUtil;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class FragmentDABConfiguration extends BaseDialogFragment
{
    public static final String ID = FragmentDABConfiguration.class.getSimpleName();

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    static final int TEMP_OFFSET_LIMIT = 100;
    static final int STEP = 10;
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private Zone     mZone;
    


    Spinner      damper1Type;
    Spinner      damper1Size;
    Spinner      damper1Shape;
    Spinner      damper2Type;
    Spinner      damper2Size;
    Spinner      damper2Shape;
    
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
    Spinner kFactor;
    NumberPicker minCFMForIAQPos;
    ToggleButton enableTrueCFMControl;
    TextView textKFactor;
    LinearLayout minCFMForIAQ;
    ToggleButton enableAutoForceOccupied;
    ToggleButton enableAutoAway;

    private ProfileType             mProfileType;
    private DabProfile              mDabProfile;
    private DabProfileConfiguration mProfileConfig;
    
    private ArrayList<Damper.Parameters> mDampers = new ArrayList<Damper.Parameters>();
    ArrayAdapter<String> damper1TypesAdapter;
    ArrayAdapter<String> damper2TypesAdapter;
    ArrayAdapter<String> kFactorValues;
    
    
    DamperType damper1TypeSelected = ZeroToTenV;
    DamperType damper2TypeSelected = ZeroToTenV;
    
    String floorRef;
    String zoneRef;

    private Spinner reheatSpinner;
    private NumberPicker minReheatDamperPos;
    private LinearLayout minReheatDamperPosLayout;
    public FragmentDABConfiguration()
    {
    }
    
    public static FragmentDABConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType)
    {
        FragmentDABConfiguration f = new FragmentDABConfiguration();
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
        
        
        dialog.setTitle("DAB");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
            titleView.setTextSize(28);
            titleView.setTypeface(null, Typeface.BOLD);
            
            ViewGroup viewGroup = (ViewGroup) titleView.getRootView();
            Button button = new Button(getActivity());
            button.setText(" < ");
            button.setTextSize(30);
            button.setTypeface(null, Typeface.BOLD);
            button.setPadding(10,10,10,10);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            button.setLayoutParams(layoutParams);
            button.setGravity(Gravity.LEFT);
            button.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
            button.setOnClickListener(v->removeDialogFragment(ID));
            button.setBackgroundColor(Color.TRANSPARENT);
            viewGroup.addView(button);
            
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
        View view = inflater.inflate(R.layout.fragment_dab_config, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
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
        
        setButton = (Button) view.findViewById(R.id.setBtn);
        
        mDabProfile = (DabProfile) L.getProfile(mSmartNodeAddress);
        
        if (mDabProfile != null) {
            CcuLog.d(L.TAG_CCU_UI,  "Get DABConfig: ");
            mProfileConfig = (DabProfileConfiguration) mDabProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            CcuLog.d(L.TAG_CCU_UI, "Create DABProfile: ");
            mDabProfile = new DabProfile();
        }

        damper1Size = view.findViewById(R.id.damper1Size);
        ArrayAdapter<CharSequence> damperSizeAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.damper_size, R.layout.spinner_dropdown_item);
        damperSizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damper1Size.setAdapter(damperSizeAdapter);
        
        ArrayList<String> damperShapes = new ArrayList<>();
        for (DamperShape shape : DamperShape.values()) {
            damperShapes.add(shape.displayName);
        }
        ArrayAdapter<String> damperShapeAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_dropdown_item, damperShapes);
        damperShapeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damper1Shape = view.findViewById(R.id.damper1Shape);
        damper1Shape.setAdapter(damperShapeAdapter);
    
    
        damper2Size = view.findViewById(R.id.damper2Size);
        damper2Size.setAdapter(damperSizeAdapter);
        damper2Shape = view.findViewById(R.id.damper2Shape);
        damper2Shape.setAdapter(damperShapeAdapter);
        
        
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
        minCoolingDamperPos.setValue(40);
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
        minHeatingDamperPos.setValue(40);
        minHeatingDamperPos.setWrapSelectorWheel(false);
        



        enableOccupancyControl = view.findViewById(R.id.enableOccupancyControl);
        enableCO2Control = view.findViewById(R.id.enableCO2Control);
        enableIAQControl = view.findViewById(R.id.enableIAQControl);
        minCFMForIAQ = view.findViewById(R.id.minCFMForIAQ);
        textKFactor = view.findViewById(R.id.textKFactor);
        kFactor = view.findViewById(R.id.enableKFactor);
        enableAutoForceOccupied = view.findViewById(R.id.enableAFOControl);
        enableAutoAway = view.findViewById(R.id.enableAutoAwayControl);

        reheatSpinner = view.findViewById(R.id.useReheat);

        ArrayList<String> spinnerArray = new ArrayList<>();
        double MIN_VAL_FOR_KFactor = Double.parseDouble(getString(R.string.min_val_for_kfactor));
        double MAX_VAL_FOR_KFactor = Double.parseDouble(getString(R.string.max_val_for_kfactor));
        double step_kfactor = Double.parseDouble(getString(R.string.step_for_kactor_values));
        int defaultValue_kfactor = Integer.parseInt(getString(R.string.default_val_for_kfactor));
        DecimalFormat df = new DecimalFormat("0.00");
        for (double i = MIN_VAL_FOR_KFactor; i < MAX_VAL_FOR_KFactor; i = i + step_kfactor) {
            spinnerArray.add(df.format(i));
        }
        kFactorValues = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, spinnerArray);
        kFactorValues.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        kFactor.setAdapter(kFactorValues);
        kFactor.setSelection(defaultValue_kfactor);
        enableTrueCFMControl = view.findViewById(R.id.enableCFMControl);
        enableTrueCFMControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (enableTrueCFMControl.isChecked()) {
                minCFMForIAQ.setVisibility(View.VISIBLE);
                textKFactor.setVisibility(View.VISIBLE);
                kFactor.setVisibility(View.VISIBLE);
            } else {
                minCFMForIAQ.setVisibility(View.GONE);
                textKFactor.setVisibility(View.GONE);
                kFactor.setVisibility(View.GONE);
            }
        });

        minCFMForIAQPos = view.findViewById(R.id.numMinCFMForIAQ);
        int minValForIAQ = Integer.parseInt(getString(R.string.min_val_for_iaq));
        int maxValForIAQ = Integer.parseInt(getString(R.string.max_val_for_iaq));
        int stepIaq = Integer.parseInt(getString(R.string.step_for_iaq_values));
        int defaultValueIAQ = Integer.parseInt(getString(R.string.default_val_for_iaq));
        String[] numberValues = new String[maxValForIAQ - minValForIAQ + 1];
        for (int i = 0; i < numberValues.length; i++) {
            numberValues[i] = String.valueOf(i * stepIaq);
        }
        minCFMForIAQPos.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        minCFMForIAQPos.setMinValue(minValForIAQ);
        minCFMForIAQPos.setMaxValue(maxValForIAQ);
        minCFMForIAQPos.setWrapSelectorWheel(false);
        minCFMForIAQPos.setDisplayedValues(numberValues);
        minCFMForIAQPos.setValue(defaultValueIAQ);


        zonePriority = view.findViewById(R.id.zonePriority);
        ArrayAdapter<CharSequence> zonePriorityAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.zone_priority, R.layout.spinner_dropdown_item);
        zonePriorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        zonePriority.setAdapter(zonePriorityAdapter);
        
        damper1Type = view.findViewById(R.id.damper1Type);
        ArrayList<String> damper1Types = new ArrayList<>();
        for (DamperType damper : DamperType.values()) {
            damper1Types.add(damper.displayName+ (damper.name().equals("MAT") ? "": " (Analog-out1)"));
        }
        damper1TypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, damper1Types);
        damper1TypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        damper1Type.setAdapter(damper1TypesAdapter);
        
        damper1Type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                damper1TypeSelected = DamperType.values()[position];
    
              /*  if(position == 4)
                damper1layout.setVisibility(View.INVISIBLE);
                else
                damper1layout.setVisibility(View.VISIBLE);
                damper1layout.invalidate();*/
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        damper2Type = view.findViewById(R.id.damper2Type);

        configureDamper2Type();
    
        damper2Type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                damper2TypeSelected = DamperType.values()[position];
                damper2Type.setSelection(position, false);
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        
        if (mProfileConfig != null) {
            damper1Type.setSelection(mProfileConfig.damper1Type, false);
            damper1Size.setSelection(damperSizeAdapter.getPosition(String.valueOf(mProfileConfig.damper1Size)), false);
            damper1Shape.setSelection(damperShapeAdapter.getPosition(DamperShape.values()[mProfileConfig.damper1Shape].displayName), false);
    
            damper2Type.setSelection(mProfileConfig.damper2Type, false);
            damper2Size.setSelection(damperSizeAdapter.getPosition(String.valueOf(mProfileConfig.damper2Size)), false);
            damper2Shape.setSelection(damperShapeAdapter.getPosition(DamperShape.values()[mProfileConfig.damper2Shape].displayName), false);
            
            enableCO2Control.setChecked(mProfileConfig.enableCO2Control);
            enableIAQControl.setChecked(mProfileConfig.enableIAQControl);
            enableAutoForceOccupied.setChecked(mProfileConfig.enableAutoForceOccupied);
            enableAutoAway.setChecked(mProfileConfig.enableAutoAwayControl);
            zonePriority.setSelection(mProfileConfig.getPriority().ordinal());
            int offsetIndex = (int)mProfileConfig.temperaturOffset+TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);
            minCoolingDamperPos.setValue(mProfileConfig.minDamperCooling);
            maxCoolingDamperPos.setValue(mProfileConfig.maxDamperCooling);
            minHeatingDamperPos.setValue(mProfileConfig.minDamperHeating);
            maxHeatingDamperPos.setValue(mProfileConfig.maxDamperHeating);
            enableTrueCFMControl.setChecked(mProfileConfig.enableCFMControl);

            if (!enableTrueCFMControl.isChecked()) {
                minCFMForIAQPos.setValue(10);
                kFactor.setSelection(100);
            } else {
                kFactor.setSelection((int) Math.ceil(((mProfileConfig.kFactor)*100)-100));
                minCFMForIAQPos.setValue(mProfileConfig.minCFMForIAQ/STEP);
            }
        } else {
            zonePriority.setSelection(2);//NORMAL
        }
        
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {


                setButton.setEnabled(false);

                compositeDisposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                        ()->{
                            ProgressDialogUtils.showProgressDialog(getActivity(),"Saving DAB Configuration");
                        },()->{
                            CCUHsApi.getInstance().resetCcuReady();
                            setupDabZoneProfile();
                            L.saveCCUState();
                            CCUHsApi.getInstance().setCcuReady();
                            LSerial.getInstance().sendSeedMessage(false,false, mSmartNodeAddress, zoneRef,floorRef);
                        },()->{
                            ProgressDialogUtils.hideProgressDialog();
                            FragmentDABConfiguration.this.closeAllBaseDialogFragments();
                            getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                        }
                ));
            }
        });
        configSpinnerDropDownColor();
        configureReheatOption();

        reheatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    minReheatDamperPosLayout.setVisibility(View.GONE);
                } else {
                    minReheatDamperPosLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        minReheatDamperPos = view.findViewById(R.id.numMinReheatDamperPos);
        minReheatDamperPosLayout = view.findViewById(R.id.minReheatDamperPos);
        minReheatDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minReheatDamperPos.setMinValue(0);
        minReheatDamperPos.setMaxValue(100);
        minReheatDamperPos.setValue(mProfileConfig != null && reheatSpinner.getSelectedItemPosition() > 0
                                                        ? mProfileConfig.minReheatDamperPos : 40);
        minReheatDamperPos.setWrapSelectorWheel(false);
        minReheatDamperPosLayout.setVisibility(reheatSpinner.getSelectedItemPosition() == 0 ? View.GONE : View.VISIBLE);
    }
    
    private void setupDabZoneProfile() {
        
        DabProfileConfiguration dabConfig = new DabProfileConfiguration();
        dabConfig.damper1Type = damper1TypeSelected.ordinal();
        dabConfig.damper1Size = Integer.parseInt(damper1Size.getSelectedItem().toString());
        dabConfig.damper1Shape = DamperType.values()[damper1Shape.getSelectedItemPosition()].ordinal();
        dabConfig.damper2Type = damper2TypeSelected.ordinal();
        dabConfig.damper2Size = Integer.parseInt(damper2Size.getSelectedItem().toString());
        dabConfig.damper2Shape = DamperType.values()[damper2Shape.getSelectedItemPosition()].ordinal();
        
        dabConfig.setNodeType(mNodeType);
        dabConfig.setNodeAddress(mSmartNodeAddress);
        dabConfig.enableCO2Control = enableCO2Control.isChecked();
        dabConfig.enableIAQControl = enableIAQControl.isChecked();
        dabConfig.enableAutoForceOccupied = enableAutoForceOccupied.isChecked();
        dabConfig.enableAutoAwayControl = enableAutoAway.isChecked();
        dabConfig.setPriority(ZonePriority.values()[zonePriority.getSelectedItemPosition()]);
        dabConfig.minDamperCooling = (minCoolingDamperPos.getValue());
        dabConfig.maxDamperCooling = (maxCoolingDamperPos.getValue());
        dabConfig.minDamperHeating = (minHeatingDamperPos.getValue());
        dabConfig.maxDamperHeating = (maxHeatingDamperPos.getValue());
        dabConfig.temperaturOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        dabConfig.enableCFMControl = enableTrueCFMControl.isChecked();
        dabConfig.minCFMForIAQ = minCFMForIAQPos.getValue()*STEP;
        dabConfig.kFactor = (((kFactor.getSelectedItemPosition()-100)*(.01))+2);
        dabConfig.minReheatDamperPos = minReheatDamperPos.getValue();

        Output analog1Op = new Output();
        analog1Op.setAddress(mSmartNodeAddress);
        analog1Op.setPort(Port.ANALOG_OUT_ONE);
        analog1Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(damper1TypeSelected.displayName);
        dabConfig.getOutputs().add(analog1Op);

        if (reheatSpinner.getSelectedItemPosition() != 0 ) {
            ReheatType reheatTypeSelected = ReheatType.values()[reheatSpinner.getSelectedItemPosition() - 1];
            updateDabReheatConfig(reheatTypeSelected, dabConfig);
        }
        if (reheatSpinner.getSelectedItemPosition() == 0 ||
                    reheatSpinner.getSelectedItemPosition() > ReheatType.OneStage.ordinal()) {
            Output analog2Op = new Output();
            analog2Op.setAddress(mSmartNodeAddress);
            analog2Op.setPort(Port.ANALOG_OUT_TWO);
            analog2Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(damper2TypeSelected.displayName);
            dabConfig.getOutputs().add(analog2Op);
        }


        dabConfig.reheatType = reheatSpinner.getSelectedItemPosition();

        mDabProfile.getProfileConfiguration().put(mSmartNodeAddress, dabConfig);
        if (mProfileConfig == null) {
            mDabProfile.addDabEquip(mSmartNodeAddress, dabConfig, floorRef, zoneRef, mNodeType );
        } else {
            mDabProfile.updateDabEquip(dabConfig);
        }
        L.ccu().zoneProfiles.add(mDabProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set DAB Config: Profiles - "+L.ccu().zoneProfiles.size());
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

    private void configSpinnerDropDownColor(){
        CCUUiUtil.setSpinnerDropDownColor(zonePriority,getContext());
        CCUUiUtil.setSpinnerDropDownColor(damper1Type,getContext());
        CCUUiUtil.setSpinnerDropDownColor(damper1Size,getContext());
        CCUUiUtil.setSpinnerDropDownColor(damper1Shape,getContext());
        CCUUiUtil.setSpinnerDropDownColor(damper2Type,getContext());
        CCUUiUtil.setSpinnerDropDownColor(damper2Size,getContext());
        CCUUiUtil.setSpinnerDropDownColor(damper2Shape,getContext());
        CCUUiUtil.setSpinnerDropDownColor(kFactor, getContext());
        CCUUiUtil.setSpinnerDropDownColor(reheatSpinner, getContext());
    }

    private void configureDamper2Type() {
        ArrayList<String> damper2Types = new ArrayList<>();
        for (DamperType damper : DamperType.values()) {
            damper2Types.add(damper.displayName+ (damper.name().equals("MAT") ? "": " (Analog-out2)"));
        }

        ArrayAdapter<String> damper2TypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, damper2Types) {

            @Override
            public boolean isEnabled(int position) {
                if (position < DamperType.MAT.ordinal()) {
                    return reheatSpinner.getSelectedItemPosition() == 0 ||
                            reheatSpinner.getSelectedItemPosition() > ReheatType.OneStage.ordinal() ;
                }
                return true;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent){
                TextView view = (TextView)super.getDropDownView(position, convertView, parent);
                if (position < DamperType.MAT.ordinal()
                        && reheatSpinner.getSelectedItemPosition() > 0
                        && reheatSpinner.getSelectedItemPosition() <= ReheatType.OneStage.ordinal()) {
                    view.setTextColor(Color.LTGRAY);
                } else {
                    view.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        damper2Type.setAdapter(damper2TypesAdapter);
    }

    private void configureReheatOption() {
        ArrayList<String> reheatTypes = new ArrayList<>();
        reheatTypes.add("Not Installed");
        for (ReheatType actuator : ReheatType.values()) {
            reheatTypes.add(actuator.displayName);
        }

        ArrayAdapter<String> reheatTypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, reheatTypes) {

            @Override
            public boolean isEnabled(int position) {
                if (position > 0 && position <= ReheatType.OneStage.ordinal()) {
                    return damper2Type.getSelectedItemPosition() == DamperType.MAT.ordinal();
                }
                return true;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent){
                TextView view = (TextView)super.getDropDownView(position, convertView, parent);
                if (position > 0
                        && position <= ReheatType.OneStage.ordinal()
                        && damper2Type.getSelectedItemPosition() != DamperType.MAT.ordinal()) {
                            view.setTextColor(Color.LTGRAY);
                } else {
                    view.setTextColor(Color.BLACK);
                }

                return view;
            }
        };

        reheatSpinner.setAdapter(reheatTypesAdapter);
        reheatSpinner.setSelection(mProfileConfig == null ? 0 : mProfileConfig.reheatType, false);
    }

    private void updateDabReheatConfig(ReheatType reheatTypeSelected, DabProfileConfiguration dabConfig) {
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
                dabConfig.getOutputs().add(analog2Op);
                break;
            case TwoStage:
                Output relay2Op = new Output();
                relay2Op.setAddress(mSmartNodeAddress);
                relay2Op.setPort(Port.RELAY_TWO);
                relay2Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
                dabConfig.getOutputs().add(relay2Op);
            case OneStage:
                Output relay1Op = new Output();
                relay1Op.setAddress(mSmartNodeAddress);
                relay1Op.setPort(Port.RELAY_ONE);
                relay1Op.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;;
                dabConfig.getOutputs().add(relay1Op);
                break;
        }
    }
}