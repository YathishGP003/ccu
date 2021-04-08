package a75f.io.renatus;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
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

import org.javolution.text.Text;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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

    @BindView(R.id.relay3Test) ToggleButton relay3Test;
    @BindView(R.id.relay7Test) ToggleButton relay7Test;
    @BindView(R.id.imageRTUInput) ImageView imageView;
    
    @BindView(R.id.dcwbEnableToggle) ToggleButton dcwbEnableToggle;
    @BindView(R.id.dabAnalog) ViewGroup dabLayout;
    @BindView(R.id.dcwbLayout)     ViewGroup dcwbLayout;
    @BindView(R.id.dcwbEnableText) TextView  dcwbText;
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
    
    Prefs prefs;
    @BindView(R.id.buttonNext)
    Button mNext;
    String PROFILE = "DAB_FULLY_MODULATING";
    boolean isFromReg = false;
    DabFullyModulatingRtu systemProfile = null;
    
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
            ahuAnalog1Tb.setChecked(systemProfile.getConfigEnabled("analog1") > 0);
            ahuAnalog2Tb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
            ahuAnalog3Tb.setChecked(systemProfile.getConfigEnabled("analog3") > 0);
            relay3Tb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
            relay7Tb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);
            boolean dcwbEnabled = systemProfile.isDcwbEnabled();
            if (dcwbEnabled) {
                ahuAnalog4Tb.setChecked(systemProfile.getConfigEnabled("analog4") > 0);
                dcwbEnableToggle.setChecked(true);
            }
            handleDabDwcbEnabled(dcwbEnabled);
            setupAnalogLimitSelectors();
            
        } else {
            
            new AsyncTask<String, Void, Void>() {

                @Override
                protected void onPreExecute() {
                    ProgressDialogUtils.showProgressDialog(getActivity(),"Loading System Profile");
                    super.onPreExecute();
                }
            
                @Override
                protected Void doInBackground( final String ... params ) {
                    if (systemProfile != null) {
                        systemProfile.deleteSystemEquip();
                        L.ccu().systemProfile = null; //Makes sure that System Algos dont run until new profile is ready.
                    }
                    systemProfile = new DabFullyModulatingRtu();
                    systemProfile.addSystemEquip();
                    L.ccu().systemProfile = systemProfile;
                    return null;
                }
                @Override
                protected void onPostExecute( final Void result ) {
                    setupAnalogLimitSelectors();
                    ProgressDialogUtils.hideProgressDialog();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        }
    
        ahuAnalog1Tb.setOnCheckedChangeListener(this);
        ahuAnalog2Tb.setOnCheckedChangeListener(this);
        ahuAnalog3Tb.setOnCheckedChangeListener(this);
        relay3Tb.setOnCheckedChangeListener(this);
        relay7Tb.setOnCheckedChangeListener(this);


        if(isFromReg){
            mNext.setVisibility(View.VISIBLE);
        }
        else {
            mNext.setVisibility(View.GONE);
        }

        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                goTonext();
                mNext.setEnabled(false);
            }
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
    
        dcwbEnableToggle.setOnCheckedChangeListener((compoundButton, b) -> {
            //TODO- Use a foreground progress bar to avoid user clicking this toggle too soon.
            if (b) {
                systemProfile.enableDcwb(CCUHsApi.getInstance());
            } else {
                systemProfile.disableDcwb(CCUHsApi.getInstance());
            }
            setConfigBackground("dcwb and enabled",b ? 1 : 0);
            handleDabDwcbEnabled(b);
        });
        
    }
    
    
    private void handleDabDwcbEnabled(boolean dcwbEnabled) {
        if (dcwbEnabled) {
            dcwbLayout.setVisibility(View.VISIBLE);
            dabLayout.setVisibility(View.GONE);
            dcwbText.setText(getString(R.string.label_dcwb_enabled));
            analog4View.setVisibility(View.VISIBLE);
            analog1Text.setText(getString(R.string.label_analog1_dcwb));
            initializeDcwbSpinners();
        } else {
            dcwbLayout.setVisibility(View.GONE);
            dabLayout.setVisibility(View.VISIBLE);
            dcwbText.setText(getString(R.string.label_dcwb_enable));
            analog4View.setVisibility(View.GONE);
            analog1Text.setText(getString(R.string.label_cooling));
            
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
        ArrayList<Integer> analogArray = new ArrayList<>();
        for (int a = 0; a <= 10; a++)
        {
            analogArray.add(a);
        }
        ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, analogArray);
        analogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
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
        ahuAnalog1Test.setSelection(0,false);
        
        ahuAnalog2Test.setAdapter(testValAdapter);
        ahuAnalog2Test.setSelection(0,false);
        
        ahuAnalog3Test.setAdapter(testValAdapter);
        ahuAnalog3Test.setSelection(0,false);
        
        
        analog1Min.setOnItemSelectedListener(this);
        analog1Max.setOnItemSelectedListener(this);
        analog2Min.setOnItemSelectedListener(this);
        analog2Max.setOnItemSelectedListener(this);
        analog3Min.setOnItemSelectedListener(this);
        analog3Max.setOnItemSelectedListener(this);
        
        ahuAnalog1Test.setOnItemSelectedListener(this);
        ahuAnalog2Test.setOnItemSelectedListener(this);
        ahuAnalog3Test.setOnItemSelectedListener(this);
        
        relay7Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                setHumidifierrConfigBackground("relay7 and humidifier and type", i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
        
        relay3Test.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                sendAnalogOutTestSignal();
            }
        });
        relay7Test.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                sendAnalogOutTestSignal();
            }
        });
    
        analog4Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setAnalog4LoopType(analog4Spinner.getSelectedItemPosition());
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }
    
    private ArrayAdapter<Double> getArrayAdapter(double start, double end, double increment) {
        ArrayList<Double> zoroToHundred = new ArrayList<>();
        for (double val = start;  val <= end; val += increment) {
            zoroToHundred.add(val);
        }
        ArrayAdapter<Double> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }
    
    /**
     * Called from OnCreateView if dcwb is already enabled.
     * Otherwise gets called when dcwb toggle button is enabled.
     */
    private void initializeDcwbSpinners() {
        
        ArrayList<Integer> analogVoltageArray = new ArrayList<>();
        for (int analogVal = 0; analogVal <= 10; analogVal++) {
            analogVoltageArray.add(analogVal);
        }
        ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item,
                                                                 analogVoltageArray);
        analogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    
        
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
        
        ArrayAdapter<Double> flowRateAdapter = getArrayAdapter(0, 100, 1);
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
    
        cwTargetDeltaTSpinner.setOnItemSelectedListener(this);
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
        
        @Override
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
    
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        double val = Double.parseDouble(arg0.getSelectedItem().toString());;
       
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
            case R.id.analog2Spinner:
            case R.id.analog3Spinner:
                sendAnalogOutTestSignal();
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
    }
    
    //TODO - rxjava
    private void setAnalog4LoopType( double val) {
        new Thread(() -> systemProfile.updateDcwbAnalog4Mapping(val));
    }
    
    private void setConfigBackground(String tags, double val) {
        new AsyncTask<String, Void, Void>() {
            
            @Override
            protected Void doInBackground( final String ... params ) {
                systemProfile.setConfigVal(tags, val);
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
    private void setHumidifierrConfigBackground(String tags, double val) {
        new AsyncTask<String, Void, Void>() {
            
            @Override
            protected void onPreExecute() {
                ProgressDialogUtils.showProgressDialog(getActivity(),"Saving DAB System Configuration");
                super.onPreExecute();
            }
            
            @Override
            protected Void doInBackground( final String ... params ) {
                systemProfile.setHumidifierConfigVal(tags, val);
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
                ProgressDialogUtils.hideProgressDialog();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
    
    private void setSelectionBackground(String analog, boolean selected) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected void onPreExecute() {
                ProgressDialogUtils.showProgressDialog(getActivity(),"Saving DAB System Configuration");
                super.onPreExecute();
            }
            @Override
            protected Void doInBackground( final String ... params ) {
                if (systemProfile.isDcwbEnabled() &&
                    (analog.contains(Tags.ANALOG1) || analog.contains(Tags.ANALOG4))) {
                    systemProfile.setDcwbConfigEnabled(analog, selected ? 1: 0);
                } else {
                    systemProfile.setConfigEnabled(analog, selected ? 1 : 0);
                }
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                if (!selected) {
                    updateSystemMode();
                }
                ProgressDialogUtils.hideProgressDialog();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
    
    private void setUserIntentBackground(String query, double val) {
        
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                TunerUtil.writeSystemUserIntentVal(query, val);
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
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
            || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable()))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);//, AlertDialog.THEME_HOLO_DARK);
            String str = "Conditioning Mode changed from '" + systemMode.name() + "' to '" + SystemMode.OFF.name() + "' based on changed equipment selection.";
            str = str + "\nPlease select appropriate conditioning mode from System Settings.";
            builder.setCancelable(false)
                   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           dialog.cancel();
                       }
                   })
                   .setTitle("System Conditioning Mode Changed")
                   .setMessage(str);
            
            AlertDialog dlg = builder.create();
            dlg.show();
            setUserIntentBackground("conditioning and mode", SystemMode.OFF.ordinal());
        }
    }
    
    
    public void sendAnalogOutTestSignal() {
        
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        
        msg.analog0.set(getAnalogVal(systemProfile.getConfigVal("cooling and min"), systemProfile.getConfigVal("cooling and max"),
                Double.parseDouble(ahuAnalog1Test.getSelectedItem().toString())));
        
        msg.analog1.set(getAnalogVal(systemProfile.getConfigVal("fan and min"), systemProfile.getConfigVal("fan and max"),
                Double.parseDouble(ahuAnalog2Test.getSelectedItem().toString())));
        
        msg.analog2.set(getAnalogVal(systemProfile.getConfigVal("heating and min"), systemProfile.getConfigVal("heating and max"),
                Double.parseDouble(ahuAnalog3Test.getSelectedItem().toString())));
    
        short relayStatus = (short) ((relay3Test.isChecked() ? 1 << 2 : 0) | (relay7Test.isChecked() ? 1 << 6 : 0));
        msg.relayBitmap.set(relayStatus);
        MeshUtil.sendStructToCM(msg);

        ControlMote.setAnalogOut("analog1",Double.parseDouble(ahuAnalog1Test.getSelectedItem().toString()));
        ControlMote.setAnalogOut("analog2",Double.parseDouble(ahuAnalog2Test.getSelectedItem().toString()));
        ControlMote.setAnalogOut("analog3",Double.parseDouble(ahuAnalog3Test.getSelectedItem().toString()));
        ControlMote.setRelayState("relay3",relay3Test.isChecked() ? 1 : 0);
        ControlMote.setRelayState("relay7",relay7Test.isChecked() ? 1 : 0);

        if (relayStatus > 0 || Double.parseDouble(ahuAnalog1Test.getSelectedItem().toString()) > 0 || Double.parseDouble(ahuAnalog2Test.getSelectedItem().toString()) > 0
                || Double.parseDouble(ahuAnalog3Test.getSelectedItem().toString()) > 0) {
            if (!Globals.getInstance().isTestMode()) {
                Globals.getInstance().setTestMode(true);
            }
        } else {
            if (Globals.getInstance().isTestMode()) {
                Globals.getInstance().setTestMode(false);
            }
        }
    }
    
    
    short getAnalogVal(double min, double max, double val) {
        return max > min ? (short) (10 * (min + (max - min) * val/100)) : (short) (10 * (min - (min - max) * val/100));
    }
}
