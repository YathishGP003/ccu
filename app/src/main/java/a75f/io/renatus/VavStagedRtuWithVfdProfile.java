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
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 2/18/19.
 */

public class VavStagedRtuWithVfdProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    @BindView(R.id.relay1Cb) CheckBox relay1Cb;
    @BindView(R.id.relay2Cb) CheckBox relay2Cb;
    @BindView(R.id.relay3Cb) CheckBox relay3Cb;
    @BindView(R.id.relay4Cb) CheckBox relay4Cb;
    @BindView(R.id.relay5Cb) CheckBox relay5Cb;
    @BindView(R.id.relay6Cb) CheckBox relay6Cb;
    @BindView(R.id.relay7Cb) CheckBox relay7Cb;
    @BindView(R.id.analog2Cb) CheckBox analog2Cb;
    
    
    @BindView(R.id.relay1Spinner)Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner)Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner)Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner)Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner)Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner)Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner)Spinner relay7Spinner;
    @BindView(R.id.analog2TestSpinner)Spinner analog2TestSpinner;
    
    @BindView(R.id.analog2Economizer) Spinner analog2Economizer;
    @BindView(R.id.analog2Recirculate) Spinner analog2Recirculate;
    @BindView(R.id.analog2CoolStage1) Spinner analog2CoolStage1;
    @BindView(R.id.analog2CoolStage2) Spinner analog2CoolStage2;
    @BindView(R.id.analog2CoolStage3) Spinner analog2CoolStage3;
    @BindView(R.id.analog2CoolStage4) Spinner analog2CoolStage4;
    @BindView(R.id.analog2CoolStage5) Spinner analog2CoolStage5;
    @BindView(R.id.analog2HeatStage1) Spinner analog2HeatStage1;
    @BindView(R.id.analog2HeatStage2) Spinner analog2HeatStage2;
    @BindView(R.id.analog2HeatStage3) Spinner analog2HeatStage3;
    @BindView(R.id.analog2HeatStage4) Spinner analog2HeatStage4;
    @BindView(R.id.analog2HeatStage5) Spinner analog2HeatStage5;
    
    @BindView(R.id.relay1Test) ToggleButton relay1Test;
    @BindView(R.id.relay2Test) ToggleButton relay2Test;
    @BindView(R.id.relay3Test) ToggleButton relay3Test;
    @BindView(R.id.relay4Test) ToggleButton relay4Test;
    @BindView(R.id.relay5Test) ToggleButton relay5Test;
    @BindView(R.id.relay6Test) ToggleButton relay6Test;
    @BindView(R.id.relay7Test) ToggleButton relay7Test;
    
    
    VavStagedRtuWithVfd systemProfile = null;
    public static VavStagedRtuWithVfdProfile newInstance()
    {
        return new VavStagedRtuWithVfdProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_vav_staged_vfd, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        if (L.ccu().systemProfile instanceof VavStagedRtuWithVfd) {
            systemProfile = (VavStagedRtuWithVfd) L.ccu().systemProfile;
            relay1Cb.setChecked(systemProfile.getConfigEnabled("relay1") > 0);
            relay2Cb.setChecked(systemProfile.getConfigEnabled("relay2") > 0);
            relay3Cb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
            relay4Cb.setChecked(systemProfile.getConfigEnabled("relay4") > 0);
            relay5Cb.setChecked(systemProfile.getConfigEnabled("relay5") > 0);
            relay6Cb.setChecked(systemProfile.getConfigEnabled("relay6") > 0);
            relay7Cb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);
            analog2Cb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
            setUpCheckBoxes();
            setUpSpinners();
        } else {
            
            new AsyncTask<Void, Void, Void>() {
                
                ProgressDialog progressDlg = new ProgressDialog(getActivity());
                @Override
                protected void onPreExecute() {
                    progressDlg.setMessage("Loading System Profile");
                    progressDlg.show();
                    super.onPreExecute();
                }
                
                @Override
                protected Void doInBackground( final Void ... params ) {
                    if (systemProfile != null) {
                        systemProfile.deleteSystemEquip();
                    }
                    systemProfile = new VavStagedRtuWithVfd();
                    systemProfile.addSystemEquip();
                    L.ccu().systemProfile = systemProfile;
                    return null;
                }
                @Override
                protected void onPostExecute( final Void result ) {
                    setUpCheckBoxes();
                    setUpSpinners();
                    progressDlg.dismiss();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        }
    }
    
    private void setUpCheckBoxes() {
        
        relay1Cb.setOnCheckedChangeListener(this);
        relay2Cb.setOnCheckedChangeListener(this);
        relay3Cb.setOnCheckedChangeListener(this);
        relay4Cb.setOnCheckedChangeListener(this);
        relay5Cb.setOnCheckedChangeListener(this);
        relay6Cb.setOnCheckedChangeListener(this);
        relay7Cb.setOnCheckedChangeListener(this);
        analog2Cb.setOnCheckedChangeListener(this);
        
        relay1Test.setOnCheckedChangeListener(this);
        relay2Test.setOnCheckedChangeListener(this);
        relay3Test.setOnCheckedChangeListener(this);
        relay4Test.setOnCheckedChangeListener(this);
        relay5Test.setOnCheckedChangeListener(this);
        relay6Test.setOnCheckedChangeListener(this);
        relay7Test.setOnCheckedChangeListener(this);
    }
    
    private void setUpSpinners() {
        relay1Spinner.setSelection((int)systemProfile.getConfigAssociation("relay1"));
        relay2Spinner.setSelection((int)systemProfile.getConfigAssociation("relay2"));
        relay3Spinner.setSelection((int)systemProfile.getConfigAssociation("relay3"));
        relay4Spinner.setSelection((int)systemProfile.getConfigAssociation("relay4"));
        relay5Spinner.setSelection((int)systemProfile.getConfigAssociation("relay5"));
        relay6Spinner.setSelection((int)systemProfile.getConfigAssociation("relay6"));
        relay7Spinner.setSelection((int)systemProfile.getConfigAssociation("relay7"));
        
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
        
        
    
        ArrayList<Integer> analogArray = new ArrayList<>();
        for (int a = 0; a <= 10; a++)
        {
            analogArray.add(a);
        }
        ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, analogArray);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        
        analog2TestSpinner.setAdapter(analogAdapter);
    
        analog2Economizer.setAdapter(analogAdapter);
        analog2Economizer.setSelection((int)systemProfile.getConfigVal("analog2 and economizer"));
        analog2Recirculate.setAdapter(analogAdapter);;
        analog2Recirculate.setSelection((int)systemProfile.getConfigVal("analog2 and recirculate"));
        analog2CoolStage1.setAdapter(analogAdapter);
        analog2CoolStage1.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage1"));
        analog2CoolStage2.setAdapter(analogAdapter);
        analog2CoolStage2.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage2"));
        analog2CoolStage3.setAdapter(analogAdapter);
        analog2CoolStage3.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage3"));
        analog2CoolStage4.setAdapter(analogAdapter);
        analog2CoolStage4.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage4"));
        analog2CoolStage5.setAdapter(analogAdapter);
        analog2CoolStage5.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage5"));
    
        analog2HeatStage1.setAdapter(analogAdapter);
        analog2HeatStage1.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage1"));
        analog2HeatStage2.setAdapter(analogAdapter);
        analog2HeatStage2.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage2"));
        analog2HeatStage3.setAdapter(analogAdapter);
        analog2HeatStage3.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage3"));
        analog2HeatStage4.setAdapter(analogAdapter);
        analog2HeatStage4.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage4"));
        analog2HeatStage5.setAdapter(analogAdapter);
        analog2HeatStage5.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage5"));
        
        analog2TestSpinner.setOnItemSelectedListener(this);
        updateAnalogOptions();
    }
    
    private void updateAnalogOptions() {
        systemProfile.updateStagesSelected();
        boolean analogEnabled = analog2Cb.isChecked();
        analog2Economizer.setEnabled(false);
        analog2Recirculate.setEnabled(false);
        analog2CoolStage1.setEnabled(analogEnabled && systemProfile.coolingStages == 1);
        analog2CoolStage2.setEnabled(analogEnabled && systemProfile.coolingStages == 2);
        analog2CoolStage3.setEnabled(analogEnabled && systemProfile.coolingStages == 3);
        analog2CoolStage4.setEnabled(analogEnabled && systemProfile.coolingStages == 4);
        analog2CoolStage5.setEnabled(analogEnabled && systemProfile.coolingStages == 5);
    
        analog2HeatStage1.setEnabled(analogEnabled && systemProfile.heatingStages == 1);
        analog2HeatStage2.setEnabled(analogEnabled && systemProfile.heatingStages == 2);
        analog2HeatStage3.setEnabled(analogEnabled && systemProfile.heatingStages == 3);
        analog2HeatStage4.setEnabled(analogEnabled && systemProfile.heatingStages == 4);
        analog2HeatStage5.setEnabled(analogEnabled && systemProfile.heatingStages == 5);
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.relay1Cb:
                relay1Spinner.setEnabled(relay1Cb.isChecked());
                setConfigEnabledBackground("relay1",relay1Cb.isChecked() ? 1: 0);
                break;
            case R.id.relay2Cb:
                relay2Spinner.setEnabled(relay2Cb.isChecked());
                setConfigEnabledBackground("relay2",relay2Cb.isChecked() ? 1: 0);
                break;
            case R.id.relay3Cb:
                relay3Spinner.setEnabled(relay3Cb.isChecked());
                setConfigEnabledBackground("relay3",relay3Cb.isChecked() ? 1: 0);
                break;
            case R.id.relay4Cb:
                relay4Spinner.setEnabled(relay4Cb.isChecked());
                setConfigEnabledBackground("relay4",relay4Cb.isChecked() ? 1: 0);
                break;
            case R.id.relay5Cb:
                relay5Spinner.setEnabled(relay5Cb.isChecked());
                setConfigEnabledBackground("relay5",relay5Cb.isChecked() ? 1: 0);
                break;
            case R.id.relay6Cb:
                relay6Spinner.setEnabled(relay6Cb.isChecked());
                setConfigEnabledBackground("relay6",relay6Cb.isChecked() ? 1: 0);
                break;
            case R.id.relay7Cb:
                relay7Spinner.setEnabled(relay7Cb.isChecked());
                setConfigEnabledBackground("relay7",relay7Cb.isChecked() ? 1: 0);
                break;
            case R.id.analog2Cb:
                setConfigEnabledBackground("analog2",analog2Cb.isChecked() ? 1: 0);
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
        updateAnalogOptions();
    }
    
    
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        switch (arg0.getId())
        {
            case R.id.relay1Spinner:
                if (relay1Cb.isChecked())
                {
                    setConfigAssociationBackground("relay1", relay1Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay2Spinner:
                if (relay2Cb.isChecked())
                {
                    setConfigAssociationBackground("relay2", relay2Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay3Spinner:
                if (relay3Cb.isChecked())
                {
                    setConfigAssociationBackground("relay3", relay3Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay4Spinner:
                if (relay4Cb.isChecked())
                {
                    setConfigAssociationBackground("relay4", relay4Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay5Spinner:
                if (relay5Cb.isChecked())
                {
                    setConfigAssociationBackground("relay5", relay5Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay6Spinner:
                if (relay6Cb.isChecked())
                {
                    setConfigAssociationBackground("relay6", relay6Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay7Spinner:
                if (relay7Cb.isChecked())
                {
                    setConfigAssociationBackground("relay7", relay7Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.analog2TestSpinner:
                sendAnalog2OutTestSignal(Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2Economizer:
                setConfigBackground("analog2 and economizer", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2Recirculate:
                setConfigBackground("analog2 and recirculate", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2CoolStage1:
                setConfigBackground("analog2 and cool and stage1", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2CoolStage2:
                setConfigBackground("analog2 and cool and stage2", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2CoolStage3:
                setConfigBackground("analog2 and cool and stage3", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2CoolStage4:
                setConfigBackground("analog2 and cool and stage4", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2CoolStage5:
                setConfigBackground("analog2 and cool and stage5", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2HeatStage1:
                setConfigBackground("analog2 and cool and stage1", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2HeatStage2:
                setConfigBackground("analog2 and cool and stage2", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2HeatStage3:
                setConfigBackground("analog2 and cool and stage3", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2HeatStage4:
                setConfigBackground("analog2 and cool and stage4", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            case R.id.analog2HeatStage5:
                setConfigBackground("analog2 and cool and stage5", Double.parseDouble(arg0.getSelectedItem().toString()));
                break;
            
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
    }
    
    private void setConfigEnabledBackground(String config, double val) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground( final Void ... params ) {
                systemProfile.setConfigEnabled(config, val);
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }
    
    private void setConfigAssociationBackground(String config, double val) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground( final Void ... params ) {
                systemProfile.setConfigAssociation(config, val);
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }
    
    private void setConfigBackground(String config, double val) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground( final Void ... params ) {
                systemProfile.setConfigVal(config, val);
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }
    
    public void sendRelayActivationTestSignal(short val) {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        msg.relayBitmap.set(val);
        MeshUtil.sendStructToCM(msg);
    }
    
    public void sendAnalog2OutTestSignal(double val) {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        msg.analog2.set((short)(val * 10));
        MeshUtil.sendStructToCM(msg);
    }
}
