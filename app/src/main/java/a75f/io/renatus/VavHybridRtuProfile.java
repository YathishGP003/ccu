package a75f.io.renatus;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.tuners.TunerConstants;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 2/11/19.
 */

public class VavHybridRtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    
    @BindView(R.id.relay1Cb)
    CheckBox relay1Cb;
    @BindView(R.id.relay2Cb)
    CheckBox relay2Cb;
    @BindView(R.id.relay3Cb)
    CheckBox relay3Cb;
    @BindView(R.id.relay4Cb)
    CheckBox relay4Cb;
    @BindView(R.id.relay5Cb)
    CheckBox relay5Cb;
    @BindView(R.id.relay6Cb)
    CheckBox relay6Cb;
    @BindView(R.id.relay7Cb)
    CheckBox relay7Cb;
    
    @BindView(R.id.relay1Spinner)
    Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner)
    Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner)
    Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner)
    Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner)
    Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner)
    Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner)
    Spinner relay7Spinner;
    
    @BindView(R.id.ahuAnalog1Min) Spinner analog1Min;
    @BindView(R.id.ahuAnalog1Max) Spinner analog1Max;
    @BindView(R.id.ahuAnalog2Min) Spinner analog2Min;
    @BindView(R.id.ahuAnalog2Max) Spinner analog2Max;
    @BindView(R.id.ahuAnalog3Min) Spinner analog3Min;
    @BindView(R.id.ahuAnalog3Max) Spinner analog3Max;
    
    @BindView(R.id.ahuAnalog1Cb) CheckBox ahuAnalog1Cb;
    @BindView(R.id.ahuAnalog2Cb) CheckBox ahuAnalog2Cb;
    @BindView(R.id.ahuAnalog3Cb) CheckBox ahuAnalog3Cb;
    @BindView(R.id.ahuAnalog4Cb) CheckBox ahuAnalog4Cb;
    
    @BindView(R.id.ahuAnalog1Test) Spinner ahuAnalog1Test;
    @BindView(R.id.ahuAnalog2Test) Spinner ahuAnalog2Test;
    @BindView(R.id.ahuAnalog3Test) Spinner ahuAnalog3Test;
    @BindView(R.id.ahuAnalog4Test) Spinner ahuAnalog4Test;
    
    @BindView(R.id.ahuAnalog4MinCooling) Spinner analog4MinCooling;
    @BindView(R.id.ahuAnalog4MaxCooling) Spinner analog4MaxCooling;
    @BindView(R.id.ahuAnalog4MinHeating) Spinner analog4MinHeating;
    @BindView(R.id.ahuAnalog4MaxHeating) Spinner analog4MaxHeating;
    
    @BindView(R.id.relay1Test)ToggleButton relay1Test;
    @BindView(R.id.relay2Test)ToggleButton relay2Test;
    @BindView(R.id.relay3Test)ToggleButton relay3Test;
    @BindView(R.id.relay4Test)ToggleButton relay4Test;
    @BindView(R.id.relay5Test)ToggleButton relay5Test;
    @BindView(R.id.relay6Test)ToggleButton relay6Test;
    @BindView(R.id.relay7Test)ToggleButton relay7Test;
    
    
    VavAdvancedHybridRtu systemProfile = null;
    public static VavStagedRtuProfile newInstance()
    {
        return new VavStagedRtuProfile();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_hybridrtu, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        if (L.ccu().systemProfile instanceof VavAdvancedHybridRtu)
        {
            systemProfile = (VavAdvancedHybridRtu) L.ccu().systemProfile;
            relay1Cb.setChecked(systemProfile.getConfigEnabled("relay1") > 0);
            relay2Cb.setChecked(systemProfile.getConfigEnabled("relay2") > 0);
            relay3Cb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
            relay4Cb.setChecked(systemProfile.getConfigEnabled("relay4") > 0);
            relay5Cb.setChecked(systemProfile.getConfigEnabled("relay5") > 0);
            relay6Cb.setChecked(systemProfile.getConfigEnabled("relay6") > 0);
            relay7Cb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);
    
            ahuAnalog1Cb.setChecked(systemProfile.getConfigEnabled("analog1") > 0);
            ahuAnalog2Cb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
            ahuAnalog3Cb.setChecked(systemProfile.getConfigEnabled("analog3") > 0);
            ahuAnalog4Cb.setChecked(systemProfile.getConfigEnabled("analog4") > 0);
            
            setUpCheckBoxes();
            setUpSpinners();
        }
        else
        {
            new AsyncTask<String, Void, Void>()
            {
                
                ProgressDialog progressDlg = new ProgressDialog(getActivity());
                @Override
                protected void onPreExecute()
                {
                    progressDlg.setMessage("Loading System Profile");
                    progressDlg.show();
                    super.onPreExecute();
                }
                
                @Override
                protected Void doInBackground(final String... params)
                {
                    if (systemProfile != null)
                    {
                        systemProfile.deleteSystemEquip();
                        L.ccu().systemProfile = null;
                    }
                    systemProfile = new VavAdvancedHybridRtu();
                    systemProfile.addSystemEquip();
                    L.ccu().systemProfile = systemProfile;
                    return null;
                }
                @Override
                protected void onPostExecute(final Void result)
                {
                    setUpCheckBoxes();
                    setUpSpinners();
                    progressDlg.dismiss();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        }
    }
    
    private void setUpCheckBoxes()
    {
        relay1Cb.setOnCheckedChangeListener(this);
        relay2Cb.setOnCheckedChangeListener(this);
        relay3Cb.setOnCheckedChangeListener(this);
        relay4Cb.setOnCheckedChangeListener(this);
        relay5Cb.setOnCheckedChangeListener(this);
        relay6Cb.setOnCheckedChangeListener(this);
        relay7Cb.setOnCheckedChangeListener(this);
    
        ahuAnalog1Cb.setOnCheckedChangeListener(this);
        ahuAnalog2Cb.setOnCheckedChangeListener(this);
        ahuAnalog3Cb.setOnCheckedChangeListener(this);
        ahuAnalog4Cb.setOnCheckedChangeListener(this);
    }
    
    private void setUpSpinners()
    {
        relay1Spinner.setSelection((int) systemProfile.getConfigAssociation("relay1"));
        relay2Spinner.setSelection((int) systemProfile.getConfigAssociation("relay2"));
        relay3Spinner.setSelection((int) systemProfile.getConfigAssociation("relay3"));
        relay4Spinner.setSelection((int) systemProfile.getConfigAssociation("relay4"));
        relay5Spinner.setSelection((int) systemProfile.getConfigAssociation("relay5"));
        relay6Spinner.setSelection((int) systemProfile.getConfigAssociation("relay6"));
        relay7Spinner.setSelection((int) systemProfile.getConfigAssociation("relay7"));
        relay1Spinner.setEnabled(relay1Cb.isChecked());
        relay2Spinner.setEnabled(relay2Cb.isChecked());
        relay3Spinner.setEnabled(relay3Cb.isChecked());
        relay4Spinner.setEnabled(relay4Cb.isChecked());
        relay5Spinner.setEnabled(relay5Cb.isChecked());
        relay6Spinner.setEnabled(relay6Cb.isChecked());
        relay7Spinner.setEnabled(relay7Cb.isChecked());
        relay1Spinner.setOnItemSelectedListener(this);
        relay2Spinner.setOnItemSelectedListener(this);
        relay3Spinner.setOnItemSelectedListener(this);
        relay4Spinner.setOnItemSelectedListener(this);
        relay5Spinner.setOnItemSelectedListener(this);
        relay6Spinner.setOnItemSelectedListener(this);
        relay7Spinner.setOnItemSelectedListener(this);
    
        relay1Test.setOnCheckedChangeListener(this);
        relay2Test.setOnCheckedChangeListener(this);
        relay3Test.setOnCheckedChangeListener(this);
        relay4Test.setOnCheckedChangeListener(this);
        relay5Test.setOnCheckedChangeListener(this);
        relay6Test.setOnCheckedChangeListener(this);
        relay7Test.setOnCheckedChangeListener(this);
        
        setupAnalogSpinners();
    }
    
    private void setupAnalogSpinners()
    {
        ArrayList<Integer> analogArray = new ArrayList<>();
        for (int a = 0; a <= 10; a++)
        {
            analogArray.add(a);
        }
        ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, analogArray);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        analog1Min.setAdapter(analogAdapter);
        analog1Min.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog1 and cooling and min")));
        analog1Max.setAdapter(analogAdapter);
        double analogVal = systemProfile.getConfigVal("analog1 and cooling and max");
        analog1Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1);
        analog2Min.setAdapter(analogAdapter);
        analog2Min.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog2 and fan and min")));
        analog2Max.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog2 and fan and max");
        analog2Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1);
        analog3Min.setAdapter(analogAdapter);
        analog3Min.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog3 and heating and min")));
        analog3Max.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog3 and heating and max");
        analog3Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1);
        analog4MinCooling.setAdapter(analogAdapter);
        analog4MinCooling.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog4 and cooling and min")));
        analog4MaxCooling.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog4 and cooling and max");
        analog4MaxCooling.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1);
        analog4MinHeating.setAdapter(analogAdapter);
        analog4MinHeating.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog4 and heating and min")));
        analog4MaxHeating.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog4 and heating and max");
        analog4MaxHeating.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1);
    
        ArrayList<Double> zoroToHundred = new ArrayList<>();
        for (double val = 0;  val <= 100.0; val++)
        {
            zoroToHundred.add(val);
        }
        ArrayAdapter<Double> coolingTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        coolingTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ahuAnalog1Test.setAdapter(coolingTestAdapter);
    
        ArrayAdapter<Double> fanTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        fanTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ahuAnalog2Test.setAdapter(fanTestAdapter);
    
        ArrayAdapter<Double> heatingTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        heatingTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ahuAnalog3Test.setAdapter(heatingTestAdapter);
    
        ArrayAdapter<Double> compositeTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        compositeTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ahuAnalog4Test.setAdapter(compositeTestAdapter);
        
    
        analog1Min.setOnItemSelectedListener(this);
        analog1Max.setOnItemSelectedListener(this);
        analog2Min.setOnItemSelectedListener(this);
        analog2Max.setOnItemSelectedListener(this);
        analog3Min.setOnItemSelectedListener(this);
        analog3Max.setOnItemSelectedListener(this);
        analog4MinCooling.setOnItemSelectedListener(this);
        analog4MaxCooling.setOnItemSelectedListener(this);
        analog4MinHeating.setOnItemSelectedListener(this);
        analog4MaxHeating.setOnItemSelectedListener(this);
        ahuAnalog1Test.setOnItemSelectedListener(this);
        ahuAnalog2Test.setOnItemSelectedListener(this);
        ahuAnalog3Test.setOnItemSelectedListener(this);
        ahuAnalog4Test.setOnItemSelectedListener(this);
    }
        
        @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.relay1Cb:
                relay1Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay1", isChecked ? 1 : 0);
                break;
            case R.id.relay2Cb:
                relay2Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay2", isChecked ? 1 : 0);
                break;
            case R.id.relay3Cb:
                relay3Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay3", isChecked ? 1 : 0);
                break;
            case R.id.relay4Cb:
                relay4Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay4", isChecked ? 1 : 0);
                break;
            case R.id.relay5Cb:
                relay5Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay5", isChecked ? 1 : 0);
                break;
            case R.id.relay6Cb:
                relay6Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay6", isChecked ? 1 : 0);
                break;
            case R.id.relay7Cb:
                relay7Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay7", isChecked ? 1 : 0);
                break;
            case R.id.ahuAnalog1Cb:
                setConfigEnabledBackground("analog1", isChecked ? 1 : 0);
                break;
            case R.id.ahuAnalog2Cb:
                setConfigEnabledBackground("analog2", isChecked ? 1 : 0);
                break;
            case R.id.ahuAnalog3Cb:
                setConfigEnabledBackground("analog3", isChecked ? 1 : 0);
                break;
            case R.id.ahuAnalog4Cb:
                setConfigEnabledBackground("analog4", isChecked ? 1 : 0);
                break;
            case R.id.relay1Test:
                sendRelayActivationTestSignal((short) (relay1Test.isChecked() ? 1: 0));
                break;
            case R.id.relay2Test:
                sendRelayActivationTestSignal((short)(relay2Test.isChecked() ? 1 << 1 : 0));
                break;
            case R.id.relay3Test:
                sendRelayActivationTestSignal((short)(relay2Test.isChecked() ? 1 << 2 : 0));
                break;
            case R.id.relay4Test:
                sendRelayActivationTestSignal((short)(relay2Test.isChecked() ? 1 << 3 : 0));
                break;
            case R.id.relay5Test:
                sendRelayActivationTestSignal((short)(relay2Test.isChecked() ? 1 << 4 : 0));
                break;
            case R.id.relay6Test:
                sendRelayActivationTestSignal((short)(relay2Test.isChecked() ? 1 << 5 : 0));
                break;
            case R.id.relay7Test:
                sendRelayActivationTestSignal((short)(relay2Test.isChecked() ? 1 << 6 : 0));
                break;
        }
    }
    
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
    {
        //Stage s = Stage.getEnum(arg0.getSelectedItem().toString());
        switch (arg0.getId())
        {
            case R.id.relay1Spinner:
                if (relay1Cb.isChecked())
                {
                    setConfigAssociationBackground("relay1", relay1Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay2Spinner:
                if (relay2Cb.isChecked())
                {
                    setConfigAssociationBackground("relay2", relay2Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay3Spinner:
                if (relay3Cb.isChecked())
                {
                    setConfigAssociationBackground("relay3", relay3Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay4Spinner:
                if (relay4Cb.isChecked())
                {
                    setConfigAssociationBackground("relay4", relay4Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay5Spinner:
                if (relay5Cb.isChecked())
                {
                    setConfigAssociationBackground("relay5", relay5Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay6Spinner:
                if (relay6Cb.isChecked())
                {
                    setConfigAssociationBackground("relay6", relay6Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay7Spinner:
                if (relay7Cb.isChecked())
                {
                    setConfigAssociationBackground("relay7", relay7Spinner.getSelectedItemPosition());
                }
                break;
    
            case R.id.ahuAnalog1Min:
                setConfigBackground("analog1 and cooling and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog1Max:
                setConfigBackground("analog1 and cooling and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog2Min:
                setConfigBackground("analog2 and fan and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog2Max:
                setConfigBackground("analog2 and fan and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog3Min:
                setConfigBackground("analog3 and heating and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog3Max:
                setConfigBackground("analog3 and heating and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog4MinCooling:
                setConfigBackground("analog4 and cooling and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog4MaxCooling:
                setConfigBackground("analog4 and cooling and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog4MinHeating:
                setConfigBackground("analog4 and heating and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog4MaxHeating:
                setConfigBackground("analog4 and heating and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog1Test:
                sendAnalog1OutTestSignal(Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog2Test:
                sendAnalog2OutTestSignal(Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog3Test:
                sendAnalog3OutTestSignal(Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.ahuAnalog4Test:
                sendAnalog4OutTestSignal(Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0)
    {
        // TODO Auto-generated method stub
    }
    
    private void setConfigEnabledBackground(String config, double val)
    {
        new AsyncTask<String, Void, Void>()
        {
            @Override
            protected Void doInBackground(final String... params)
            {
                systemProfile.setConfigEnabled(config, val);
                return null;
            }
            
            @Override
            protected void onPostExecute(final Void result)
            {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
    
    private void setConfigAssociationBackground(String config, double val)
    {
        new AsyncTask<String, Void, Void>()
        {
            @Override
            protected Void doInBackground(final String... params)
            {
                systemProfile.setConfigAssociation(config, val);
                return null;
            }
            
            @Override
            protected void onPostExecute(final Void result)
            {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
    
    private void setConfigBackground(String tags, int level, double val) {
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
    
    public void sendRelayActivationTestSignal(short val) {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        msg.relayBitmap.set(val);
        MeshUtil.sendStructToCM(msg);
    }
    
    public void sendAnalog1OutTestSignal(double val) {
        double analogMin = systemProfile.getConfigVal("analog1 and cooling and min");
        double analogMax = systemProfile.getConfigVal("analog1 and cooling and max");
        
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        
        short signal;
        if (analogMax > analogMin)
        {
            signal = (short) (10 * (analogMin + (analogMax - analogMin) * val/100));
        } else {
            signal = (short) (10 * (analogMin - (analogMin - analogMax) * val/100));
        }
        msg.analog0.set(signal);
        MeshUtil.sendStructToCM(msg);
    }
    
    public void sendAnalog2OutTestSignal(double val) {
        double analogMin = systemProfile.getConfigVal("analog2 and fan and min");
        double analogMax = systemProfile.getConfigVal("analog2 and fan and max");
        
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        
        short signal;
        if (analogMax > analogMin)
        {
            signal = (short) (10 * (analogMin + (analogMax - analogMin) * val/100));
        } else {
            signal = (short) (10 * (analogMin - (analogMin - analogMax) * val/100));
        }
        msg.analog1.set(signal);
        MeshUtil.sendStructToCM(msg);
    }
    
    public void sendAnalog3OutTestSignal(double val) {
        double analogMin = systemProfile.getConfigVal("analog3 and heating and min");
        double analogMax = systemProfile.getConfigVal("analog3 and heating and max");
        
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        
        short signal;
        if (analogMax > analogMin)
        {
            signal = (short) (10 * (analogMin + (analogMax - analogMin) * val/100));
        } else {
            signal = (short) (10 * (analogMin - (analogMin - analogMax) * val/100));
        }
        msg.analog2.set(signal);
        MeshUtil.sendStructToCM(msg);
    }
    
    public void sendAnalog4OutTestSignal(double val) {
        
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        msg.analog3.set((short) val);
        MeshUtil.sendStructToCM(msg);
    }
    
}

