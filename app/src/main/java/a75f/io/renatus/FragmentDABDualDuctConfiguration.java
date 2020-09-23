package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;

import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.dualduct.DualDuctAnalogActuator;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.dualduct.DualDuctProfileConfiguration;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FragmentDABDualDuctConfiguration extends BaseDialogFragment {
    
    private static final String TAG = FragmentDABDualDuctConfiguration.class.getSimpleName();
    
    public static final String ID = FragmentDABDualDuctConfiguration.class.getSimpleName();
    
    private static final int DUALDUCT_DIALOG_WIDTH = 1065;
    private static final int DUALDUCT_DIALOG_HEIGHT = 672;
    
    
    private static final double DEFAULT_ANALOG_MIN = 0;
    private static final double DEFAULT_ANALOG_MAX = 10.0;
    
    private static final double DEFAULT_COMPOSITE_ANALOG_HEATING_MIN = 5.0;
    private static final double DEFAULT_COMPOSITE_ANALOG_HEATING_MAX = 0;
    private static final double DEFAULT_COMPOSITE_ANALOG_COOLING_MIN = 5.1;
    private static final double DEFAULT_COMPOSITE_ANALOG_COOLING_MAX = 10.0;
    
    private static final int DEFAULT_DAMPER_MIN = 0;
    private static final int DEFAULT_DAMPER_MAX= 100;
    private static final int DEFAULT_DAMPER_VAL= 20;
    
    static final int TEMP_OFFSET_LIMIT = 100;
    
    @BindView(R.id.analog1OutSpinner) Spinner analog1OutSpinner;
    
    @BindView(R.id.analog2OutSpinner) Spinner analog2OutSpinner;
    
    @BindView(R.id.thermistor2Spinner) Spinner thermistor2Spinner;
    
    @BindView(R.id.ao1MinDamperHeatingTV) TextView ao1MinDamperHeatingTV;
    @BindView(R.id.ao1MinDamperHeatingSpinner) Spinner ao1MinDamperHeatingSpinner;
    @BindView(R.id.ao1MaxDamperHeatingTV) TextView ao1MaxDamperHeatingTV;
    @BindView(R.id.ao1MaxDamperHeatingSpinner) Spinner ao1MaxDamperHeatingSpinner;
    
    @BindView(R.id.ao1MinDamperCoolingTV) TextView ao1MinDamperCoolingTV;
    @BindView(R.id.ao1MinDamperCoolingSpinner) Spinner ao1MinDamperCoolingSpinner;
    @BindView(R.id.ao1MaxDamperCoolingTV) TextView ao1MaxDamperCoolingTV;
    @BindView(R.id.ao1MaxDamperCoolingSpinner) Spinner ao1MaxDamperCoolingSpinner;
    
    @BindView(R.id.ao2MinDamperHeatingTV) TextView ao2MinDamperHeatingTV;
    @BindView(R.id.ao2MinDamperHeatingSpinner) Spinner ao2MinDamperHeatingSpinner;
    @BindView(R.id.ao2MaxDamperHeatingTV) TextView ao2MaxDamperHeatingTV;
    @BindView(R.id.ao2MaxDamperHeatingSpinner) Spinner ao2MaxDamperHeatingSpinner;
    
    @BindView(R.id.ao2MinDamperCoolingTV) TextView ao2MinDamperCoolingTV;
    @BindView(R.id.ao2MinDamperCoolingSpinner) Spinner ao2MinDamperCoolingSpinner;
    @BindView(R.id.ao2MaxDamperCoolingTV) TextView ao2MaxDamperCoolingTV;
    @BindView(R.id.ao2MaxDamperCoolingSpinner) Spinner ao2MaxDamperCoolingSpinner;
    
    
    @BindView(R.id.temperatureOffset) NumberPicker temperatureOffset;
    @BindView(R.id.maxCoolingDamperPos) NumberPicker maxCoolingDamperPos;
    @BindView(R.id.minCoolingDamperPos) NumberPicker minCoolingDamperPos;
    @BindView(R.id.maxHeatingDamperPos) NumberPicker maxHeatingDamperPos;
    @BindView(R.id.minHeatingDamperPos) NumberPicker minHeatingDamperPos;
    
    @BindView(R.id.enableOccupancyControl) ToggleButton enableOccupancyControl;
    @BindView(R.id.enableCO2Control) ToggleButton enableCO2Control;
    @BindView(R.id.enableIAQControl) ToggleButton enableIAQControl;
    
    @BindView(R.id.setBtn) Button setButton;
    
    private short    mSmartNodeAddress;
    private NodeType        mNodeType;
    private DualDuctProfile              mDualDuctProfile;
    private DualDuctProfileConfiguration mProfileConfig;
    
    String floorRef;
    String zoneRef;
    
    ArrayAdapter<Double> analogOutAdapter = null;
    
    public static FragmentDABDualDuctConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType)
    {
        FragmentDABDualDuctConfiguration fragment = new FragmentDABDualDuctConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
        bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal());
        fragment.setArguments(bundle);
        return fragment;
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
            dialog.getWindow().setLayout(DUALDUCT_DIALOG_WIDTH, DUALDUCT_DIALOG_HEIGHT);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_dab_dual_duct_config, container, false);
        Objects.requireNonNull(getDialog().getWindow()).requestFeature(Window.FEATURE_NO_TITLE);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Nullable
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDualDuctProfile = (DualDuctProfile) L.getProfile(mSmartNodeAddress);
        if (mDualDuctProfile != null) {
            CcuLog.d(L.TAG_CCU_UI, "Get DualDuctConfig: ");
            mProfileConfig = (DualDuctProfileConfiguration) mDualDuctProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            CcuLog.d(L.TAG_CCU_UI, "Create DualDuct Profile: ");
            mDualDuctProfile = new DualDuctProfile();
        }
        
        setupAnalogOutSpinners();
        setUpNumberPickers();
        
        if (mProfileConfig != null) {
            restoreViews();
        } else {
            initializeViews();
        }
        configureSetButton();
    
        analog1OutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DualDuctAnalogActuator actuator = DualDuctAnalogActuator.values()[position];
                analog1OutSpinner.setSelection(position);
                handleAnalog1Selection(actuator);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
    
        analog2OutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DualDuctAnalogActuator actuator = DualDuctAnalogActuator.values()[position];
                analog2OutSpinner.setSelection(position);
                handleAnalog2Selection(actuator);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
    }
    
    private void handleAnalog1Selection(DualDuctAnalogActuator actuator) {
        if (actuator == DualDuctAnalogActuator.NOT_USED) {
            updateAO1ConfigVisibility(View.GONE);
        } else if (actuator == DualDuctAnalogActuator.COOLING) {
            updateAO1ConfigVisibility(View.VISIBLE);
            enableAO1CoolingConfig(true, false);
            enableAO1HeatingConfig(false, false);
        } else if (actuator == DualDuctAnalogActuator.HEATING) {
            updateAO1ConfigVisibility(View.VISIBLE);
            enableAO1CoolingConfig(false, false);
            enableAO1HeatingConfig(true, false);
        } else if (actuator == DualDuctAnalogActuator.COMPOSITE) {
            updateAO1ConfigVisibility(View.VISIBLE);
            enableAO1CoolingConfig(true, true);
            enableAO1HeatingConfig(true, true);
        }
    }
    
    private void handleAnalog2Selection(DualDuctAnalogActuator actuator) {
        
        if (actuator == DualDuctAnalogActuator.NOT_USED) {
            updateAO2ConfigVisibility(View.GONE);
        } else if (actuator == DualDuctAnalogActuator.COOLING) {
            updateAO2ConfigVisibility(View.VISIBLE);
            enableAO2CoolingConfig(true, false);
            enableAO2HeatingConfig(false, false);
        } else if (actuator == DualDuctAnalogActuator.HEATING) {
            updateAO2ConfigVisibility(View.VISIBLE);
            enableAO2CoolingConfig(false, false);
            enableAO2HeatingConfig(true, false);
        } else if (actuator == DualDuctAnalogActuator.COMPOSITE) {
            updateAO2ConfigVisibility(View.VISIBLE);
            enableAO2CoolingConfig(true, true);
            enableAO2HeatingConfig(true, true);
        }
    }
    
    private void setUpNumberPickers() {
        
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
        
        setNumberPickerDividerColor(maxCoolingDamperPos);
        maxCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxCoolingDamperPos.setMinValue(DEFAULT_DAMPER_MIN);
        maxCoolingDamperPos.setMaxValue(DEFAULT_DAMPER_MAX);
        maxCoolingDamperPos.setValue(DEFAULT_DAMPER_MAX);
        maxCoolingDamperPos.setWrapSelectorWheel(false);
        
        setNumberPickerDividerColor(minCoolingDamperPos);
        minCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minCoolingDamperPos.setMinValue(DEFAULT_DAMPER_MIN);
        minCoolingDamperPos.setMaxValue(DEFAULT_DAMPER_MAX);
        minCoolingDamperPos.setValue(DEFAULT_DAMPER_VAL);
        minCoolingDamperPos.setWrapSelectorWheel(false);
        
        setNumberPickerDividerColor(maxHeatingDamperPos);
        maxHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxHeatingDamperPos.setMinValue(DEFAULT_DAMPER_MIN);
        maxHeatingDamperPos.setMaxValue(DEFAULT_DAMPER_MAX);
        maxHeatingDamperPos.setValue(DEFAULT_DAMPER_MAX);
        maxHeatingDamperPos.setWrapSelectorWheel(false);
        
        setNumberPickerDividerColor(minHeatingDamperPos);
        minHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minHeatingDamperPos.setMinValue(DEFAULT_DAMPER_MIN);
        minHeatingDamperPos.setMaxValue(DEFAULT_DAMPER_MAX);
        minHeatingDamperPos.setValue(DEFAULT_DAMPER_VAL);
        minHeatingDamperPos.setWrapSelectorWheel(false);
    
        setDividerColor(temperatureOffset);
        setDividerColor(maxCoolingDamperPos);
        setDividerColor(minCoolingDamperPos);
        setDividerColor(maxHeatingDamperPos);
        setDividerColor(minHeatingDamperPos);
    }
    
    private ArrayAdapter<Double> getAnalogOutAdapter() {
        
        if (analogOutAdapter != null) {
            return analogOutAdapter;
        }
        
        ArrayList<Double> voltages = new ArrayList<>();
        for (int val = 0; val <= 100; val++) {
            voltages.add((double)val/10);
        }
        analogOutAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, voltages);
        analogOutAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return analogOutAdapter;
    }
    
    private void setupAnalogOutSpinners() {
    
        ArrayAdapter<Double> analogOutAdapter = getAnalogOutAdapter();
        ao1MinDamperHeatingSpinner.setAdapter(analogOutAdapter);
        ao1MaxDamperHeatingSpinner.setAdapter(analogOutAdapter);
        ao1MinDamperCoolingSpinner.setAdapter(analogOutAdapter);
        ao1MaxDamperCoolingSpinner.setAdapter(analogOutAdapter);
        ao2MinDamperHeatingSpinner.setAdapter(analogOutAdapter);
        ao2MaxDamperHeatingSpinner.setAdapter(analogOutAdapter);
        ao2MinDamperCoolingSpinner.setAdapter(analogOutAdapter);
        ao2MaxDamperCoolingSpinner.setAdapter(analogOutAdapter);
    }
    
    private void updateAO1ConfigVisibility(int visibility) {
        ao1MinDamperHeatingTV.setVisibility(visibility);
        ao1MinDamperHeatingSpinner.setVisibility(visibility);
        ao1MaxDamperHeatingTV.setVisibility(visibility);
        ao1MaxDamperHeatingSpinner.setVisibility(visibility);
        ao1MinDamperCoolingTV.setVisibility(visibility);
        ao1MinDamperCoolingSpinner.setVisibility(visibility);
        ao1MaxDamperCoolingTV.setVisibility(visibility);
        ao1MaxDamperCoolingSpinner.setVisibility(visibility);
    }
    
    private void enableAO1HeatingConfig(boolean enabled, boolean isComposite) {
        ao1MinDamperHeatingTV.setEnabled(enabled);
        ao1MinDamperHeatingSpinner.setEnabled(enabled);
        ao1MaxDamperHeatingTV.setEnabled(enabled);
        ao1MaxDamperHeatingSpinner.setEnabled(enabled);
        if (enabled && isComposite) {
            ao1MinDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_HEATING_MIN), false);
            ao1MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_HEATING_MAX), false);
        } else if (enabled) {
            ao1MinDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MIN), false);
            ao1MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MAX), false);
        }
    }
    
    
    private void enableAO1CoolingConfig(boolean enabled, boolean isComposite) {
        ao1MinDamperCoolingTV.setEnabled(enabled);
        ao1MinDamperCoolingSpinner.setEnabled(enabled);
        ao1MaxDamperCoolingTV.setEnabled(enabled);
        ao1MaxDamperCoolingSpinner.setEnabled(enabled);
        if (enabled && isComposite) {
            ao1MinDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_COOLING_MIN), false);
            ao1MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_COOLING_MAX), false);
        } else if (enabled) {
            ao1MinDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MIN), false);
            ao1MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MAX), false);
        }
    }
    
    private void enableAO2HeatingConfig(boolean enabled, boolean isComposite) {
        ao2MinDamperHeatingTV.setEnabled(enabled);
        ao2MinDamperHeatingSpinner.setEnabled(enabled);
        ao2MaxDamperHeatingTV.setEnabled(enabled);
        ao2MaxDamperHeatingSpinner.setEnabled(enabled);
        if (enabled && isComposite) {
            ao2MinDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_HEATING_MIN), false);
            ao2MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_HEATING_MAX), false);
        } else if (enabled) {
            ao2MinDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MIN), false);
            ao2MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MAX), false);
        }
    }
    
    
    private void enableAO2CoolingConfig(boolean enabled, boolean isComposite) {
        ao2MinDamperCoolingTV.setEnabled(enabled);
        ao2MinDamperCoolingSpinner.setEnabled(enabled);
        ao2MaxDamperCoolingTV.setEnabled(enabled);
        ao2MaxDamperCoolingSpinner.setEnabled(enabled);
        if (enabled && isComposite) {
            ao2MinDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_COOLING_MIN), false);
            ao2MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_COOLING_MAX), false);
        } else if (enabled) {
            ao2MinDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MIN), false);
            ao2MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MAX), false);
        }
    }
    
    private void updateAO2ConfigVisibility(int visibility) {
        ao2MinDamperHeatingTV.setVisibility(visibility);
        ao2MinDamperHeatingSpinner.setVisibility(visibility);
        ao2MaxDamperHeatingTV.setVisibility(visibility);
        ao2MaxDamperHeatingSpinner.setVisibility(visibility);
        ao2MinDamperCoolingTV.setVisibility(visibility);
        ao2MinDamperCoolingSpinner.setVisibility(visibility);
        ao2MaxDamperCoolingTV.setVisibility(visibility);
        ao2MaxDamperCoolingSpinner.setVisibility(visibility);
    }
    
    private void restoreViews() {
        
        analog1OutSpinner.setSelection(mProfileConfig.getAnalogOut1Config(), false);
        DualDuctAnalogActuator analog1Selection = DualDuctAnalogActuator.values()[mProfileConfig.getAnalogOut1Config()];
        handleAnalog1Selection(analog1Selection);
        
        analog2OutSpinner.setSelection(mProfileConfig.getAnalogOut2Config(), false);
        DualDuctAnalogActuator analog2Selection = DualDuctAnalogActuator.values()[mProfileConfig.getAnalogOut2Config()];
        handleAnalog2Selection(analog2Selection);
        
        thermistor2Spinner.setSelection(mProfileConfig.getThermistor2Config(), false);
        
        ArrayAdapter<Double> analogOutAdapter = getAnalogOutAdapter();
        
        ao1MinDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog1OutAtMinDamperHeating()), false);
        ao1MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog1OutAtMaxDamperHeating()), false);
        ao1MinDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog1OutAtMinDamperCooling()), false);
        ao1MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog1OutAtMaxDamperCooling()), false);
        ao2MinDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog2OutAtMinDamperHeating()), false);
        ao2MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog2OutAtMaxDamperHeating()), false);
        ao2MinDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog2OutAtMinDamperCooling()), false);
        ao2MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(mProfileConfig.getAnalog2OutAtMaxDamperCooling()), false);
    
        enableOccupancyControl.setChecked(mProfileConfig.isEnableOccupancyControl());
        enableCO2Control.setChecked(mProfileConfig.isEnableCO2Control());
        enableIAQControl.setChecked(mProfileConfig.isEnableIAQControl());
        
        int offsetIndex = (int)mProfileConfig.getTemperatureOffset()+TEMP_OFFSET_LIMIT;
        temperatureOffset.setValue(offsetIndex);
        minCoolingDamperPos.setValue(mProfileConfig.getMinCoolingDamperPos());
        maxCoolingDamperPos.setValue(mProfileConfig.getMaxCoolingDamperPos());
        minHeatingDamperPos.setValue(mProfileConfig.getMinHeatingDamperPos());
        maxHeatingDamperPos.setValue(mProfileConfig.getMaxHeatingDamperPos());
    }
    
    private void initializeViews() {
        
        analog1OutSpinner.setSelection(DualDuctAnalogActuator.COMPOSITE.getVal());
        ArrayAdapter<Double> analogOutAdapter = getAnalogOutAdapter();
        ao2MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MAX), false);
        ao2MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_ANALOG_MAX), false);
        setDefaultAnalog1CompositeConfig(analogOutAdapter);
    }
    
    private void setDefaultAnalog1CompositeConfig(ArrayAdapter<Double> analogOutAdapter) {
        ao1MinDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_HEATING_MIN), false);
        ao1MaxDamperHeatingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_HEATING_MAX), false);
        ao1MaxDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_COOLING_MAX), false);
        ao1MinDamperCoolingSpinner.setSelection(analogOutAdapter.getPosition(DEFAULT_COMPOSITE_ANALOG_COOLING_MIN), false);
    }
    
    private void setDividerColor(NumberPicker picker) {
        Field[] numberPickerFields = NumberPicker.class.getDeclaredFields();
        for (Field field : numberPickerFields) {
            if (field.getName().equals("mSelectionDivider")) {
                field.setAccessible(true);
                try {
                    field.set(picker, getResources().getDrawable(R.drawable.divider_np));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
    
    private void setNumberPickerDividerColor(NumberPicker pk) {
        Class<?> numberPickerClass;
        try {
            numberPickerClass = Class.forName("android.widget.NumberPicker");
            Field selectionDivider = numberPickerClass.getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            selectionDivider.set(pk, getResources().getDrawable(R.drawable.line_959595));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void configureSetButton() {
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                
                new AsyncTask<Void, Void, Void>() {
                    
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        ProgressDialogUtils.showProgressDialog(getActivity(), "Saving DualDuct Configuration");
                        super.onPreExecute();
                    }
                    
                    @Override
                    protected Void doInBackground( final Void ... params ) {
                        setupDualDuctZoneProfile();
                        L.saveCCUState();
                        return null;
                    }
                    
                    @Override
                    protected void onPostExecute( final Void result ) {
                        ProgressDialogUtils.hideProgressDialog();
                        FragmentDABDualDuctConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                        sendSeedMessage();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                
            }
        });
    }
    
    private void sendSeedMessage() {
        new Thread(() -> LSerial.getInstance().sendSeedMessage(false, false, mSmartNodeAddress, zoneRef, floorRef)).start();
    }
    
    private DualDuctProfileConfiguration createDualDuctConfig() {
        DualDuctProfileConfiguration dualductConfig = new DualDuctProfileConfiguration();
        dualductConfig.setAnalogOut1Config(analog1OutSpinner.getSelectedItemPosition());
        dualductConfig.setAnalogOut2Config(analog2OutSpinner.getSelectedItemPosition());
    
        dualductConfig.setThermistor2Config(thermistor2Spinner.getSelectedItemPosition());
    
        dualductConfig.setNodeType(mNodeType);
        dualductConfig.setNodeAddress(mSmartNodeAddress);
        dualductConfig.setEnableOccupancyControl(enableOccupancyControl.isChecked());
        dualductConfig.setEnableCO2Control(enableCO2Control.isChecked());
        dualductConfig.setEnableIAQControl(enableIAQControl.isChecked());
    
        dualductConfig.setMinHeatingDamperPos(minHeatingDamperPos.getValue());
        dualductConfig.setMaxHeatingDamperPos(maxHeatingDamperPos.getValue());
        dualductConfig.setMinCoolingDamperPos(minCoolingDamperPos.getValue());
        dualductConfig.setMaxCoolingDamperPos(maxCoolingDamperPos.getValue());
    
        dualductConfig.setTemperatureOffset(temperatureOffset.getValue() - TEMP_OFFSET_LIMIT);
    
        dualductConfig.setAnalog1OutAtMinDamperHeating(Double.parseDouble
                                                                  (ao1MinDamperHeatingSpinner.getSelectedItem().toString()));
        dualductConfig.setAnalog1OutAtMaxDamperHeating(Double.parseDouble
                                                                  (ao1MaxDamperHeatingSpinner.getSelectedItem().toString()));
        dualductConfig.setAnalog1OutAtMinDamperCooling(Double.parseDouble
                                                                  (ao1MinDamperCoolingSpinner.getSelectedItem().toString()));
        dualductConfig.setAnalog1OutAtMaxDamperCooling(Double.parseDouble
                                                                  (ao1MaxDamperCoolingSpinner.getSelectedItem().toString()));
    
        dualductConfig.setAnalog2OutAtMinDamperHeating(Double.parseDouble
                                                                  (ao2MinDamperHeatingSpinner.getSelectedItem().toString()));
        dualductConfig.setAnalog2OutAtMaxDamperHeating(Double.parseDouble
                                                                  (ao2MaxDamperHeatingSpinner.getSelectedItem().toString()));
        dualductConfig.setAnalog2OutAtMinDamperCooling(Double.parseDouble
                                                                  (ao2MinDamperCoolingSpinner.getSelectedItem().toString()));
        dualductConfig.setAnalog2OutAtMaxDamperCooling(Double.parseDouble
                                                                  (ao2MaxDamperCoolingSpinner.getSelectedItem().toString()));
    
        Output analog1Op = new Output();
        analog1Op.setAddress(mSmartNodeAddress);
        analog1Op.setPort(Port.ANALOG_OUT_ONE);
        analog1Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        dualductConfig.getOutputs().add(analog1Op);
    
        Output analog2Op = new Output();
        analog2Op.setAddress(mSmartNodeAddress);
        analog2Op.setPort(Port.ANALOG_OUT_TWO);
        analog2Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        dualductConfig.getOutputs().add(analog2Op);
    
        return dualductConfig;
    }
    
    private void setupDualDuctZoneProfile() {
        DualDuctProfileConfiguration dualDuctConfig = createDualDuctConfig();
        mDualDuctProfile.getProfileConfiguration().put(mSmartNodeAddress, dualDuctConfig);
        if (mProfileConfig == null) {
            mDualDuctProfile.addDualDuctEquip(mSmartNodeAddress, dualDuctConfig, floorRef, zoneRef );
        } else {
            mDualDuctProfile.updateDualDuctEquip(dualDuctConfig);
        }
        L.ccu().zoneProfiles.add(mDualDuctProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set DualDuct Config: Profiles - "+L.ccu().zoneProfiles.size());
    }
    
}
