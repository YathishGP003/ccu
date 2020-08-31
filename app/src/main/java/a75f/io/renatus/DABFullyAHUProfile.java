package a75f.io.renatus;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
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
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

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

    @BindView(R.id.toggleRelay3) ToggleButton relay3Tb;
    @BindView(R.id.toggleRelay7) ToggleButton relay7Tb;

    @BindView(R.id.relay7Spinner) Spinner relay7Spinner;

    @BindView(R.id.analog1Spinner) Spinner ahuAnalog1Test;
    @BindView(R.id.analog2Spinner) Spinner ahuAnalog2Test;
    @BindView(R.id.analog3Spinner) Spinner ahuAnalog3Test;

    @BindView(R.id.relay3Test) ToggleButton relay3Test;
    @BindView(R.id.relay7Test) ToggleButton relay7Test;
    @BindView(R.id.imageRTUInput)
    ImageView imageView;

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
        double analogVal = systemProfile.getConfigVal("cooling and max");
        analog1Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1 , false);
        
        analog2Min.setAdapter(analogAdapter);
        analog2Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("fan and min")), false);
        
        analog2Max.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("fan and max");
        analog2Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1, false);
        
        analog3Min.setAdapter(analogAdapter);
        analog3Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("heating and min")), false);
        
        analog3Max.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("heating and max");
        analog3Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1, false);
        
        ArrayList<String> humidifierOptions = new ArrayList<>();
        humidifierOptions.add("Humidifier");
        humidifierOptions.add("De-Humidifier");
        
        ArrayAdapter<String> humidityAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, humidifierOptions);
        humidityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        
        relay7Spinner.setAdapter(humidityAdapter);
        int selection = (int)systemProfile.getConfigVal("humidifier and type");
        relay7Spinner.setSelection(selection, false);
        
        
        ArrayList<Double> zoroToHundred = new ArrayList<>();
        for (double val = 0;  val <= 100.0; val++)
        {
            zoroToHundred.add(val);
        }
        ArrayAdapter<Double> coolingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        coolingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ahuAnalog1Test.setAdapter(coolingSatTestAdapter);
        ahuAnalog1Test.setSelection(0,false);
        
        ArrayAdapter<Double> spTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        spTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ahuAnalog2Test.setAdapter(spTestAdapter);
        ahuAnalog2Test.setSelection(0,false);
        
        ArrayAdapter<Double> heatingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        heatingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ahuAnalog3Test.setAdapter(heatingSatTestAdapter);
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
            case R.id.toggleRelay3:
                setSelectionBackground("relay3", isChecked);
                break;
            case R.id.toggleRelay7:
                setSelectionBackground("relay7", isChecked);
                break;
        }
    }
    
    @Override
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
            case R.id.analog2Spinner:
            case R.id.analog3Spinner:
                sendAnalogOutTestSignal();
                break;
        }
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
                systemProfile.setConfigEnabled(analog, selected ? 1: 0);
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
