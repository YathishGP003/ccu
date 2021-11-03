package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.device.mesh.DeviceUtil;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.SystemProfileUtil;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

import static a75f.io.renatus.util.RxjavaUtil.executeBackground;

/**
 * Created by samjithsadasivan on 11/8/18.
 */

public class DABFullyAHUProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{

    @BindView(R.id.analog1Min) Spinner analog1Min;
    @BindView(R.id.analog1Max) Spinner analog1Max;
    @BindView(R.id.analog2Min) Spinner analog2Min;
    @BindView(R.id.analog2Max) Spinner analog2Max;
    @BindView(R.id.analog3Min) Spinner analog3Min;
    @BindView(R.id.analog3Max) Spinner analog3Max;

    @BindView(R.id.toggleAnalog1) ToggleButton ahuAnalog1Tb;
    @BindView(R.id.toggleAnalog2) ToggleButton ahuAnalog2Tb;
    @BindView(R.id.toggleAnalog3) ToggleButton ahuAnalog3Tb;
    @BindView(R.id.toggleAnalog4) ToggleButton ahuAnalog4Tb;

    @BindView(R.id.toggleRelay3) ToggleButton relay3Tb;
    @BindView(R.id.toggleRelay7) ToggleButton relay7Tb;

    @BindView(R.id.relay7Spinner) Spinner relay7Spinner;

    @BindView(R.id.analog1Spinner) Spinner ahuAnalog1Test;
    @BindView(R.id.analog2Spinner) Spinner ahuAnalog2Test;
    @BindView(R.id.analog3Spinner) Spinner ahuAnalog3Test;
    @BindView(R.id.analog4TestSpinner) Spinner ahuAnalog4Test;

    @BindView(R.id.relay3Test) ToggleButton relay3Test;
    @BindView(R.id.relay7Test) ToggleButton relay7Test;
    @BindView(R.id.imageRTUInput) ImageView imageView;
    
    @BindView(R.id.dcwbEnableToggle) ToggleButton dcwbEnableToggle;
    @BindView(R.id.dabAnalog) ViewGroup dabLayout;
    @BindView(R.id.dcwbLayout)     ViewGroup dcwbLayout;
    @BindView(R.id.dcwbEnableText) TextView  dcwbText;
    @BindView(R.id.textMeterStatus) TextView textMeterStatus;
    @BindView(R.id.analog1MappingText) TextView  analog1Text;
    @BindView(R.id.tableRowAnalog4) TableRow analog4View;
    @BindView(R.id.chilledWaterTargetExitTemp) ViewGroup chilledWaterTargetExitTemp;
    @BindView(R.id.chilledWaterTargetDeltaT) ViewGroup chilledWaterTargetDeltaT;
    
    @BindView(R.id.adaptiveDeltaEnable) ToggleButton adaptiveDeltaEnable;
    @BindView(R.id.maxExitWaterTemp) ToggleButton maxExitWaterTemp;
    @BindView(R.id.cwTargetDeltaTSpinner) Spinner cwTargetDeltaTSpinner;
    @BindView(R.id.cwExitTempMarginSpinner) Spinner cwExitTempMarginSpinner;
    @BindView(R.id.cwMaxFlowRateSpinner) Spinner cwMaxFlowRateSpinner;
    @BindView(R.id.analog1InAtValveClosedSpinner) Spinner analog1InAtValveClosedSpinner;
    @BindView(R.id.analog1InAtValveFullSpinner) Spinner analog1InAtValveFullSpinner;
    
    @BindView(R.id.analog1OutAtMinCHWSpinner) Spinner analog1OutAtMinCHWSpinner;
    @BindView(R.id.analog1OutAtMaxCHWSpinner) Spinner analog1OutAtMaxCHWSpinner;
    @BindView(R.id.analog2OutAtMinFanSpeed) Spinner analog2OutAtMinFanSpeed;
    @BindView(R.id.analog2OutAtMaxFanSpeed) Spinner analog2OutAtMaxFanSpeed;
    @BindView(R.id.analog3OutAtMinHeating) Spinner analog3OutAtMinHeating;
    @BindView(R.id.analog3OutMaxHeating) Spinner analog3OutMaxHeating;
    @BindView(R.id.analog4OutAtMinCooling) Spinner analog4OutAtMinCooling;
    @BindView(R.id.analog4OutMaxCooling) Spinner analog4OutMaxCooling;
    @BindView(R.id.analog4OutAtMinCo2) Spinner analog4OutAtMinCo2;
    @BindView(R.id.analog4OutMaxCo2) Spinner analog4OutMaxCo2;
    
    @BindView(R.id.analog4Spinner) Spinner analog4Spinner;
    @BindView(R.id.analog4OutCooling) ViewGroup analog4OutConfigCooling;
    @BindView(R.id.analog4OutCo2) ViewGroup analog4OutConfigCo2;
    
    Prefs prefs;
    @BindView(R.id.buttonNext)
    Button mNext;
    String PROFILE = "DAB_FULLY_MODULATING";
    boolean isFromReg = false;
    DabFullyModulatingRtu systemProfile = null;
    private boolean dcwbEnabled;
    
    public static DABFullyAHUProfile newInstance()
    {
        return new DABFullyAHUProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_dab_fully_ahu, container, false);
        ButterKnife.bind(this, rootView);
        if(getArguments() != null) {
            isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
        }
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {

        prefs = new Prefs(getContext().getApplicationContext());
        if (L.ccu().systemProfile instanceof DabFullyModulatingRtu) {
            systemProfile = (DabFullyModulatingRtu) L.ccu().systemProfile;
            boolean dcwbEnabled = systemProfile.isDcwbEnabled();
            handleDabDwcbEnabled(dcwbEnabled);
            setupAnalogLimitSelectors();
        } else {
    
            RxjavaUtil.executeBackgroundTask( () -> ProgressDialogUtils.showProgressDialog(getActivity(),
                                                                                  "Saving DAB System Configuration"),
                                              () -> { if (systemProfile != null) {
                                                          systemProfile.deleteSystemEquip();
                                                          L.ccu().systemProfile = null; //Makes sure that System Algos dont run until new profile is ready.
                                                          }
                                                      systemProfile = new DabFullyModulatingRtu();
                                                      systemProfile.addSystemEquip();
                                                      L.ccu().systemProfile = systemProfile;
                                                    },
                                              ()-> { setupAnalogLimitSelectors();
                                                     ProgressDialogUtils.hideProgressDialog();
                                              }
            );
        }
    
        ahuAnalog1Tb.setOnCheckedChangeListener(this);
        ahuAnalog2Tb.setOnCheckedChangeListener(this);
        ahuAnalog3Tb.setOnCheckedChangeListener(this);
        ahuAnalog4Tb.setOnCheckedChangeListener(this);
        relay3Tb.setOnCheckedChangeListener(this);
        relay7Tb.setOnCheckedChangeListener(this);


        if(isFromReg){
            mNext.setVisibility(View.VISIBLE);
        } else {
            mNext.setVisibility(View.GONE);
        }

        mNext.setOnClickListener(v -> {
            goTonext();
            mNext.setEnabled(false);
        });

        if (getResources().getDisplayMetrics().xdpi == (float)149.824){
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(248,325);
            lp.setMargins(0, 38, 0, 0);
            imageView.setLayoutParams(lp);
        }
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (display.getMode().getRefreshRate() == (float)59.28){
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(248,325);
                lp.setMargins(0, 42, 0, 0);
                imageView.setLayoutParams(lp);
            }
        }
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
    
        configureDcwbEnableButton();
        setSpinnerDropDownIcon();
    }
    
    private void configureDcwbEnableButton() {
        
        dcwbEnableToggle.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                AlertDialog.Builder btuConfigDialog = new AlertDialog.Builder(getActivity());
                btuConfigDialog.setTitle(getString(R.string.label_configure_btu));
                btuConfigDialog.setPositiveButton(HtmlCompat.fromHtml("<font color='#E24301'>PROCEED</font>",
                                                                      HtmlCompat.FROM_HTML_MODE_LEGACY), (dialog, which) -> {
                    systemProfile.enableDcwb(CCUHsApi.getInstance());
                    handleDabDwcbEnabled(true);
                });
                btuConfigDialog.setNegativeButton(HtmlCompat.fromHtml("<font color='#E24301'>CANCEL</font>",
                                                                      HtmlCompat.FROM_HTML_MODE_LEGACY), (dialog, which) -> {
                    dcwbEnableToggle.setChecked(false);
                });
                btuConfigDialog.setIcon(android.R.drawable.ic_dialog_alert);
                btuConfigDialog.setCancelable(false);
                btuConfigDialog.show();
            } else {
                if (!systemProfile.isDcwbEnabled()) {
                    return;
                }
                systemProfile.disableDcwb(CCUHsApi.getInstance());
                handleDabDwcbEnabled(false);
            }
        
        });
    }
    
    /**
     * Called from onViewCreated to initialize toggle buttons.
     */
    private void initializeToggleButtons(boolean dcwbEnabled) {
        this.dcwbEnabled = dcwbEnabled;
        ahuAnalog1Tb.setChecked(systemProfile.getConfigEnabled("analog1") > 0);
        ahuAnalog2Tb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
        ahuAnalog3Tb.setChecked(systemProfile.getConfigEnabled("analog3") > 0);
        relay3Tb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
        relay7Tb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);
        
        if (dcwbEnabled) {
            ahuAnalog4Tb.setChecked(systemProfile.getConfigEnabled("analog4") > 0);
            dcwbEnableToggle.setChecked(true);
        }
    }
    
    /**
     * Handle UI updates when DCWB is enabled/disabled.
     * @param dcwbEnabled
     */
    private void handleDabDwcbEnabled(boolean dcwbEnabled) {
        if (dcwbEnabled) {
            dcwbLayout.setVisibility(View.VISIBLE);
            dabLayout.setVisibility(View.GONE);
            dcwbText.setText(getString(R.string.label_dcwb_enabled));
            analog4View.setVisibility(View.VISIBLE);
            analog1Text.setText(getString(R.string.label_analog1_dcwb));
            initializeDcwbSpinners();
            double loopType = systemProfile.getConfigVal("analog4 and loop and type");
            
            textMeterStatus.setText(CCUHsApi.getInstance().read("btu and equip").isEmpty() ?
                                         getString(R.string.text_btu_meter_error_status) :
                                         getString(R.string.text_btu_meter_status));
            updateAnalog4ConfigUI(loopType);
        } else {
            dcwbLayout.setVisibility(View.GONE);
            dabLayout.setVisibility(View.VISIBLE);
            dcwbText.setText(getString(R.string.label_dcwb_enable));
            analog4View.setVisibility(View.GONE);
            analog1Text.setText(getString(R.string.label_cooling));
            
        }
        initializeToggleButtons(dcwbEnabled);
    }
    
    /**
     * Change analog4Type visibility based on the loopType chosen.
     * When analog4 is not enabled , show Cooling config.
     */
    private void updateAnalog4ConfigUI(double loopType) {
        CcuLog.d("CCU_UI", "loopType "+loopType);
        if (loopType > 0) {
            analog4OutConfigCooling.setVisibility(View.GONE);
            analog4OutConfigCo2.setVisibility(View.VISIBLE);
        } else {
            analog4OutConfigCooling.setVisibility(View.VISIBLE);
            analog4OutConfigCo2.setVisibility(View.GONE);
        }
    }

    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        prefs.setBoolean("PROFILE_SETUP",true);
        prefs.setString("PROFILE",PROFILE);
        ((FreshRegistration)getActivity()).selectItem(19);
    }
    
    private void setupAnalogLimitSelectors() {
        ArrayAdapter<Integer> analogAdapter = getIntegerArrayAdapter(0 , 10, 1);
        
        analog1Min.setAdapter(analogAdapter);
        analog1Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("cooling and min")), false);
        analog1Max.setAdapter(analogAdapter);
        analog1Max.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("cooling and max")), false);
        
        analog2Min.setAdapter(analogAdapter);
        analog2Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("fan and min")), false);
        analog2Max.setAdapter(analogAdapter);
        analog2Max.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("fan and max")), false);
        
        analog3Min.setAdapter(analogAdapter);
        analog3Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("heating and min")), false);
        analog3Max.setAdapter(analogAdapter);
        analog3Max.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("heating and max")), false);
        
        ArrayList<String> humidifierOptions = new ArrayList<>();
        humidifierOptions.add("Humidifier");
        humidifierOptions.add("De-Humidifier");
        
        ArrayAdapter<String> humidityAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, humidifierOptions);
        humidityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        
        relay7Spinner.setAdapter(humidityAdapter);
        int selection = (int)systemProfile.getConfigVal("humidifier and type");
        relay7Spinner.setSelection(selection, false);
        
        ArrayAdapter<Double> testValAdapter = getArrayAdapter(0,100,1);
        ahuAnalog1Test.setAdapter(testValAdapter);
        ahuAnalog1Test.setSelection(ControlMote.getAnalog1Out(), false);
        
        ahuAnalog2Test.setAdapter(testValAdapter);
        ahuAnalog2Test.setSelection(ControlMote.getAnalog2Out(),false);
        
        ahuAnalog3Test.setAdapter(testValAdapter);
        ahuAnalog3Test.setSelection(ControlMote.getAnalog3Out(),false);
    
        ahuAnalog4Test.setAdapter(testValAdapter);
        ahuAnalog4Test.setSelection(ControlMote.getAnalog4Out(),false);
        
        
        analog1Min.setOnItemSelectedListener(this);
        analog1Max.setOnItemSelectedListener(this);
        analog2Min.setOnItemSelectedListener(this);
        analog2Max.setOnItemSelectedListener(this);
        analog3Min.setOnItemSelectedListener(this);
        analog3Max.setOnItemSelectedListener(this);
        
        ahuAnalog1Test.setOnItemSelectedListener(this);
        ahuAnalog2Test.setOnItemSelectedListener(this);
        ahuAnalog3Test.setOnItemSelectedListener(this);
        
        relay7Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                setConfigBackgroundWithProgress("relay7 and humidifier and type", i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
        
        relay3Test.setChecked(ControlMote.getRelay3());
        relay7Test.setChecked(ControlMote.getRelay7());
        relay3Test.setOnCheckedChangeListener((compoundButton, b) ->
                                                  sendAnalogRelayTestSignal(Tags.RELAY3, b ? 1.0: 0.0));
        relay7Test.setOnCheckedChangeListener((compoundButton, b) ->
                                                  sendAnalogRelayTestSignal(Tags.RELAY7, b ? 1.0: 0.0));
        
    }
    
    private ArrayAdapter<Integer> getIntegerArrayAdapter(int start, int end, int increment) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int val = start;  val <= end; val += increment) {
            list.add(val);
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, list);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }
    
    private ArrayAdapter<Double> getArrayAdapter(double start, double end, double increment) {
        ArrayList<Double> list = new ArrayList<>();
        for (double val = start;  val <= end; val += increment) {
            list.add(val);
        }
        ArrayAdapter<Double> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, list);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }
    
    /**
     * Called from OnCreateView if dcwb is already enabled.
     * Otherwise gets called when dcwb toggle button is enabled.
     */
    private void initializeDcwbSpinners() {
    
        ArrayAdapter<Integer> analogAdapter = getIntegerArrayAdapter(0 , 10, 1);
    
        adaptiveDeltaEnable.setChecked(systemProfile.getConfigVal("adaptive and delta and enabled") > 0);
        maxExitWaterTemp.setChecked(systemProfile.getConfigVal("maximized and exit and temp and enabled") > 0);
    
        chilledWaterTargetDeltaT.setVisibility(adaptiveDeltaEnable.isChecked() ? View.VISIBLE : View.GONE);
        chilledWaterTargetExitTemp.setVisibility(maxExitWaterTemp.isChecked() ? View.VISIBLE : View.GONE);
        
        adaptiveDeltaEnable.setOnCheckedChangeListener(this);
        maxExitWaterTemp.setOnCheckedChangeListener(this);
        
        ArrayAdapter<Double> deltaTAdapter = getArrayAdapter(0, 30, 1);
        cwTargetDeltaTSpinner.setAdapter(deltaTAdapter);
        cwTargetDeltaTSpinner.setSelection(deltaTAdapter.getPosition(systemProfile.getConfigVal("target and " +
                                                                                                     "delta")), false);
        
        ArrayAdapter<Double> flowRateAdapter = getArrayAdapter(0, 200, 1);
        cwMaxFlowRateSpinner.setAdapter(flowRateAdapter);
        cwMaxFlowRateSpinner.setSelection(flowRateAdapter.getPosition(systemProfile.getConfigVal("max and " +
                                                                                                     "flow and rate")), false);
        ArrayAdapter<Double> exitTempMarginAdapter = getArrayAdapter(0, 15, 1);
        cwExitTempMarginSpinner.setAdapter(exitTempMarginAdapter);
        cwExitTempMarginSpinner.setSelection(exitTempMarginAdapter.getPosition(systemProfile.getConfigVal("exit and " +
                                                                                              "temp and margin")), false);
        analog1InAtValveClosedSpinner.setAdapter(analogAdapter);
        analog1InAtValveClosedSpinner.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("valve" +
                                                                                                             " and closed")), false);
        analog1InAtValveFullSpinner.setAdapter(analogAdapter);
        analog1InAtValveFullSpinner.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("valve" +
                                                                                                           " and full")), false);
        analog1OutAtMinCHWSpinner.setAdapter(analogAdapter);
        analog1OutAtMinCHWSpinner.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog1" +
                                                                                                           " and min")), false);
        analog1OutAtMaxCHWSpinner.setAdapter(analogAdapter);
        analog1OutAtMaxCHWSpinner.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog1" +
                                                                                                           " and max")), false);
        analog2OutAtMinFanSpeed.setAdapter(analogAdapter);
        analog2OutAtMinFanSpeed.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog2" +
                                                                                                         " and min")), false);
        analog2OutAtMaxFanSpeed.setAdapter(analogAdapter);
        analog2OutAtMaxFanSpeed.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog2" +
                                                                                                         " and max")), false);
        analog3OutAtMinHeating.setAdapter(analogAdapter);
        analog3OutAtMinHeating.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog3" +
                                                                                                       " and min")), false);
        analog3OutMaxHeating.setAdapter(analogAdapter);
        analog3OutMaxHeating.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog3" +
                                                                                                       " and max")), false);
        analog4OutAtMinCooling.setAdapter(analogAdapter);
        analog4OutAtMinCooling.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog4" +
                                                                                                      " and min")), false);
        analog4OutMaxCooling.setAdapter(analogAdapter);
        analog4OutMaxCooling.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog4" +
                                                                                                    " and max")), false);
        analog4OutAtMinCo2.setAdapter(analogAdapter);
        analog4OutAtMinCo2.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog4" +
                                                                                                      " and min")), false);
        analog4OutMaxCo2.setAdapter(analogAdapter);
        analog4OutMaxCo2.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("analog4" +
                                                                                                      " and max")), false);
    
        configureAnalog4Spinner();
        configureDcwbListeners();
        
    }
    
    private void configureAnalog4Spinner() {
        analog4Spinner.setSelection((int)systemProfile.getConfigVal("analog4 and loop and type"), false);
    
        analog4Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                setAnalog4LoopType(position);
                updateAnalog4ConfigUI(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }
    
    /**
     * Set all the DCWB specific listeners.
     */
    private void configureDcwbListeners() {
        cwTargetDeltaTSpinner.setOnItemSelectedListener(this);
        cwExitTempMarginSpinner.setOnItemSelectedListener(this);
        cwMaxFlowRateSpinner.setOnItemSelectedListener(this);
        analog1InAtValveClosedSpinner.setOnItemSelectedListener(this);
        analog1InAtValveFullSpinner.setOnItemSelectedListener(this);
        analog1OutAtMinCHWSpinner.setOnItemSelectedListener(this);
        analog1OutAtMaxCHWSpinner.setOnItemSelectedListener(this);
        analog2OutAtMinFanSpeed.setOnItemSelectedListener(this);
        analog2OutAtMaxFanSpeed.setOnItemSelectedListener(this);
        analog3OutAtMinHeating.setOnItemSelectedListener(this);
        analog3OutMaxHeating.setOnItemSelectedListener(this);
        analog4OutAtMinCooling.setOnItemSelectedListener(this);
        analog4OutMaxCooling.setOnItemSelectedListener(this);
        analog4OutAtMinCo2.setOnItemSelectedListener(this);
        analog4OutMaxCo2.setOnItemSelectedListener(this);
    }
        
    @SuppressLint("NonConstantResourceId") @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.toggleAnalog1:
                setSelectionBackground("analog1", isChecked);
                break;
            case R.id.toggleAnalog2:
                setSelectionBackground("analog2", isChecked);
                break;
            case R.id.toggleAnalog3:
                setSelectionBackground("analog3", isChecked);
                break;
            case R.id.toggleAnalog4:
                setSelectionBackground("analog4", isChecked);
                break;
            case R.id.toggleRelay3:
                setSelectionBackground("relay3", isChecked);
                break;
            case R.id.toggleRelay7:
                setSelectionBackground("relay7", isChecked);
                break;
            case R.id.adaptiveDeltaEnable:
                setConfigBackground("adaptive and delta", isChecked ? 1 : 0);
                enableAdaptiveDeltaTAlgorithm(isChecked);
                break;
            case R.id.maxExitWaterTemp:
                setConfigBackground("maximized and exit and temp", isChecked ? 1 : 0);
                enableMaxExitWaterTempAlgorithm(isChecked);
                break;
        }
    }
    
    @SuppressLint("NonConstantResourceId") @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        double val = Double.parseDouble(arg0.getSelectedItem().toString());
       
        switch (arg0.getId())
        {
            
            case R.id.analog1Min:
                setConfigBackground("cooling and min", val);
                break;
            case R.id.analog1Max:
                setConfigBackground("cooling and max", val);
                break;
            case R.id.analog2Min:
                setConfigBackground("fan and min", val);
                break;
            case R.id.analog2Max:
                setConfigBackground("fan and max", val);
                break;
            case R.id.analog3Min:
                setConfigBackground("heating and min", val);
                break;
            case R.id.analog3Max:
                setConfigBackground("heating and max", val);
                break;
            case R.id.analog1Spinner:
                sendAnalogRelayTestSignal(Tags.ANALOG1, val);
                break;
            case R.id.analog2Spinner:
                sendAnalogRelayTestSignal(Tags.ANALOG2, val);
                break;
            case R.id.analog3Spinner:
                sendAnalogRelayTestSignal(Tags.ANALOG3, val);
                break;
            case R.id.analog4TestSpinner:
                sendAnalogRelayTestSignal(Tags.ANALOG4, val);
                break;
            case R.id.cwTargetDeltaTSpinner:
                setConfigBackground("target and delta", val);
                break;
            case R.id.cwMaxFlowRateSpinner:
                setConfigBackground("max and flow and rate", val);
                break;
            case R.id.cwExitTempMarginSpinner:
                setConfigBackground("exit and temp and margin", val);
                break;
            case R.id.analog1InAtValveClosedSpinner:
                setConfigBackground("valve and closed", val);
                break;
            case R.id.analog1InAtValveFullSpinner:
                setConfigBackground("valve and full", val);
                break;
            case R.id.analog1OutAtMinCHWSpinner:
                setConfigBackground("analog1 and min", val);
                break;
            case R.id.analog1OutAtMaxCHWSpinner:
                setConfigBackground("analog1 and max", val);
                break;
            case R.id.analog2OutAtMinFanSpeed:
                setConfigBackground("analog2 and min", val);
                break;
            case R.id.analog2OutAtMaxFanSpeed:
                setConfigBackground("analog2 and max", val);
                break;
            case R.id.analog3OutAtMinHeating:
                setConfigBackground("analog3 and min", val);
                break;
            case R.id.analog3OutMaxHeating:
                setConfigBackground("analog3 and max", val);
                break;
            case R.id.analog4OutAtMinCooling:
            case R.id.analog4OutAtMinCo2:
                setConfigBackground("analog4 and min", val);
                break;
            case R.id.analog4OutMaxCooling:
            case R.id.analog4OutMaxCo2:
                setConfigBackground("analog4 and max", val);
                break;
        }
    }
    
    private void enableAdaptiveDeltaTAlgorithm(boolean enabled) {
    
        maxExitWaterTemp.setChecked(!enabled);
        if (enabled) {
            chilledWaterTargetDeltaT.setVisibility(View.VISIBLE);
            chilledWaterTargetExitTemp.setVisibility(View.GONE);
        } else {
            chilledWaterTargetDeltaT.setVisibility(View.GONE);
            chilledWaterTargetExitTemp.setVisibility(View.VISIBLE);
        }
        systemProfile.invalidateAlgorithmLoop();
    }
    
    private void enableMaxExitWaterTempAlgorithm(boolean enabled) {
        adaptiveDeltaEnable.setChecked(!enabled);
        if (enabled) {
            chilledWaterTargetDeltaT.setVisibility(View.GONE);
            chilledWaterTargetExitTemp.setVisibility(View.VISIBLE);
        } else {
            chilledWaterTargetDeltaT.setVisibility(View.VISIBLE);
            chilledWaterTargetExitTemp.setVisibility(View.GONE);
        }
        systemProfile.invalidateAlgorithmLoop();
    }
    
    private void setAnalog4LoopType( double val) {
        executeBackground(() -> systemProfile.updateDcwbAnalog4Mapping(val));
    }
    
    private void setConfigBackground(String tags, double val) {
        executeBackground(() -> systemProfile.setConfigVal(tags, val));
    }
    
    private void setConfigBackgroundWithProgress(String tags, double val) {
    
        RxjavaUtil.executeBackgroundTask( () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Saving System " +
                                                                                                     "Configuration"),
                                          () -> systemProfile.setConfigVal(tags, val),
                                          ()-> ProgressDialogUtils.hideProgressDialog());
        
    }
    
    private void setSelectionBackground(String analog, boolean selected) {
    
        RxjavaUtil.executeBackgroundTask( () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Saving System " +
                                                                                                     "Configuration"),
                                          () -> {  if (systemProfile.isDcwbEnabled() &&
                                                      (analog.contains(Tags.ANALOG1) || analog.contains(Tags.ANALOG4))) {
                                                      systemProfile.setDcwbConfigEnabled(analog, selected ? 1: 0);
                                                  } else {
                                                      systemProfile.setConfigEnabled(analog, selected ? 1 : 0);
                                                  }
                                                },
                                          ()-> { if (!selected)
                                                  updateSystemMode();
                                                 ProgressDialogUtils.hideProgressDialog();
                                               }
                                          );
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
    }
    
    public void updateSystemMode() {
        SystemMode systemMode = SystemMode.values()[(int)systemProfile.getUserIntentVal("conditioning and mode")];
        if (systemMode == SystemMode.OFF) {
            return;
        }
        if ((systemMode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable() || !systemProfile.isHeatingAvailable()))
            || (systemMode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable())
            || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable())) {
            SystemProfileUtil.showConditioningDisabledDialog(getActivity(), systemMode);
        }
    }
    
    private void sendAnalogRelayTestSignal(String tag, double val) {
        
        Globals.getInstance().setTestMode(true);
        if (tag.contains("analog")) {
            ControlMote.setAnalogOut(tag, DeviceUtil.getModulatedAnalogVal(systemProfile.getConfigVal(tag + " and min"),
                                                                           systemProfile.getConfigVal(tag+" and max"),
                                                                           val));
        } else if (tag.contains("relay")) {
            ControlMote.setRelayState(tag, val);
        }
        
        MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage());
    }

    private void setSpinnerDropDownIcon(){

        CCUUiUtil.setSpinnerDropDownColor(analog1Min,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog1Max,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog2Min,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog2Max,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog3Min,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog3Max,getContext());
        CCUUiUtil.setSpinnerDropDownColor(relay7Spinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(ahuAnalog1Test,getContext());
        CCUUiUtil.setSpinnerDropDownColor(ahuAnalog2Test,getContext());
        CCUUiUtil.setSpinnerDropDownColor(ahuAnalog3Test,getContext());
        CCUUiUtil.setSpinnerDropDownColor(ahuAnalog4Test,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog3Max,getContext());
        CCUUiUtil.setSpinnerDropDownColor(cwTargetDeltaTSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(cwExitTempMarginSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(cwMaxFlowRateSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog1InAtValveClosedSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog1InAtValveFullSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog1OutAtMinCHWSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog1OutAtMaxCHWSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog2OutAtMinFanSpeed,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog2OutAtMaxFanSpeed,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog3OutAtMinHeating,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog3OutMaxHeating,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog4OutAtMinCooling,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog4OutMaxCooling,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog4OutAtMinCo2,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog4OutMaxCo2,getContext());
        CCUUiUtil.setSpinnerDropDownColor(analog4Spinner,getContext());

    }
}
