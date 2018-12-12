package a75f.io.renatus;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.system.SystemEquip;
import a75f.io.logic.bo.building.system.VavStagedRtu;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/6/18.
 */

public class VavStagedRtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    
    @BindView(R.id.relay1Cb)CheckBox  relay1Cb;
    @BindView(R.id.relay2Cb)CheckBox  relay2Cb;
    @BindView(R.id.relay3Cb)CheckBox  relay3Cb;
    @BindView(R.id.relay4Cb)CheckBox  relay4Cb;
    @BindView(R.id.relay5Cb)CheckBox  relay5Cb;
    @BindView(R.id.relay6Cb)CheckBox  relay6Cb;
    @BindView(R.id.relay7Cb)CheckBox  relay7Cb;
    @BindView(R.id.analog1Cb)CheckBox  analog1Cb;
    @BindView(R.id.analog2Cb)CheckBox  analog2Cb;
    @BindView(R.id.analog3Cb)CheckBox  analog3Cb;
    
    @BindView(R.id.relay1Spinner)Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner)Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner)Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner)Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner)Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner)Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner)Spinner relay7Spinner;
    
    
    public static VavStagedRtuProfile newInstance()
    {
        return new VavStagedRtuProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_stagedrtu, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        if (!(L.ccu().systemProfile instanceof VavStagedRtu))
        {
            L.ccu().systemProfile = new VavStagedRtu();
        }
        
        setUpCheckBoxes();
        setUpSpinners();
    }
    
    private void setUpCheckBoxes() {
        //VavStagedRtu p = (VavStagedRtu) L.ccu().systemProfile;
        relay1Cb.setChecked(SystemEquip.getInstance().getRelaySelection("relay1") > 0);
        relay2Cb.setChecked(SystemEquip.getInstance().getRelaySelection("relay2") > 0);
        relay3Cb.setChecked(SystemEquip.getInstance().getRelaySelection("relay3") > 0);
        relay4Cb.setChecked(SystemEquip.getInstance().getRelaySelection("relay4") > 0);
        relay5Cb.setChecked(SystemEquip.getInstance().getRelaySelection("relay5") > 0);
        relay6Cb.setChecked(SystemEquip.getInstance().getRelaySelection("relay6") > 0);
        relay7Cb.setChecked(SystemEquip.getInstance().getRelaySelection("relay7") > 0);
        analog1Cb.setChecked(SystemEquip.getInstance().getRelaySelection("analog1") > 0);
        analog2Cb.setChecked(SystemEquip.getInstance().getRelaySelection("analog2") > 0);
        analog3Cb.setChecked(SystemEquip.getInstance().getRelaySelection("analog3") > 0);
    
        relay1Cb.setOnCheckedChangeListener(this);
        relay2Cb.setOnCheckedChangeListener(this);
        relay3Cb.setOnCheckedChangeListener(this);
        relay4Cb.setOnCheckedChangeListener(this);
        relay5Cb.setOnCheckedChangeListener(this);
        relay6Cb.setOnCheckedChangeListener(this);
        relay7Cb.setOnCheckedChangeListener(this);
        analog1Cb.setOnCheckedChangeListener(this);
        analog2Cb.setOnCheckedChangeListener(this);
        analog3Cb.setOnCheckedChangeListener(this);
    }
    
    private void setUpSpinners() {
        relay1Spinner.setSelection((int)SystemEquip.getInstance().getRelaySelection("relay1")-1);
        relay2Spinner.setSelection((int)SystemEquip.getInstance().getRelaySelection("relay2")-1);
        relay3Spinner.setSelection((int)SystemEquip.getInstance().getRelaySelection("relay3")-1);
        relay4Spinner.setSelection((int)SystemEquip.getInstance().getRelaySelection("relay4")-1);
        relay5Spinner.setSelection((int)SystemEquip.getInstance().getRelaySelection("relay5")-1);
        relay6Spinner.setSelection((int)SystemEquip.getInstance().getRelaySelection("relay6")-1);
        relay7Spinner.setSelection((int)SystemEquip.getInstance().getRelaySelection("relay7")-1);
        
        relay1Spinner.setOnItemSelectedListener(this);
        relay2Spinner.setOnItemSelectedListener(this);
        relay3Spinner.setOnItemSelectedListener(this);
        relay4Spinner.setOnItemSelectedListener(this);
        relay5Spinner.setOnItemSelectedListener(this);
        relay6Spinner.setOnItemSelectedListener(this);
        relay7Spinner.setOnItemSelectedListener(this);
    }
    
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        VavStagedRtu p = (VavStagedRtu) L.ccu().systemProfile;
        switch (buttonView.getId())
        {
            case R.id.relay1Cb:
                relay1Spinner.setEnabled(relay1Cb.isChecked());
                setSelectionBackground("relay1", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.relay2Cb:
                relay2Spinner.setEnabled(relay2Cb.isChecked());
                setSelectionBackground("relay2", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.relay3Cb:
                relay3Spinner.setEnabled(relay3Cb.isChecked());
                setSelectionBackground("relay3", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.relay4Cb:
                relay4Spinner.setEnabled(relay4Cb.isChecked());
                setSelectionBackground("relay4", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.relay5Cb:
                relay5Spinner.setEnabled(relay5Cb.isChecked());
                setSelectionBackground("relay5", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.relay6Cb:
                relay6Spinner.setEnabled(relay6Cb.isChecked());
                setSelectionBackground("relay6", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.relay7Cb:
                relay7Spinner.setEnabled(relay7Cb.isChecked());
                setSelectionBackground("relay7", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.analog1Cb:
                setSelectionBackground("analog1", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.analog2Cb:
                setSelectionBackground("analog2", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
            case R.id.analog3Cb:
                setSelectionBackground("analog3", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
                break;
                
        }
    }
    
    
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        Stage s = Stage.getEnum(arg0.getSelectedItem().toString());
        switch (arg0.getId())
        {
            case R.id.relay1Spinner:
                if (relay1Cb.isChecked())
                {
                    setSelectionBackground("relay1", relay1Spinner.getSelectedItemPosition() + 1);
                }
                break;
            case R.id.relay2Spinner:
                if (relay2Cb.isChecked())
                {
                    setSelectionBackground("relay2", relay1Spinner.getSelectedItemPosition() + 1);
                }
                break;
            case R.id.relay3Spinner:
                if (relay3Cb.isChecked())
                {
                    setSelectionBackground("relay3", relay1Spinner.getSelectedItemPosition() + 1);
                }
                break;
            case R.id.relay4Spinner:
                if (relay4Cb.isChecked())
                {
                    setSelectionBackground("relay4", relay1Spinner.getSelectedItemPosition() + 1);
                }
                break;
            case R.id.relay5Spinner:
                if (relay5Cb.isChecked())
                {
                    setSelectionBackground("relay5", relay1Spinner.getSelectedItemPosition() + 1);
                }
                break;
            case R.id.relay6Spinner:
                if (relay6Cb.isChecked())
                {
                    setSelectionBackground("relay6", relay1Spinner.getSelectedItemPosition() + 1);
                }
                break;
            case R.id.relay7Spinner:
                if (relay7Cb.isChecked())
                {
                    setSelectionBackground("relay7", relay1Spinner.getSelectedItemPosition() + 1);
                }
                break;
                
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
    }
    
    private void saveSettings() {
        setSelectionBackground("relay1", relay1Cb.isChecked() ?relay1Spinner.getSelectedItemPosition()+1 : 0);
    }
    
    private void setSelectionBackground(String tuner, double val) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground( final Void ... params ) {
                SystemEquip.getInstance().setRelaySelection(tuner, val);
                
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }
}
