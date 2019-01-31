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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
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
    
    @BindView(R.id.relay1Spinner)Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner)Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner)Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner)Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner)Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner)Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner)Spinner relay7Spinner;
    
    VavStagedRtu systemProfile = null;
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
        if (L.ccu().systemProfile instanceof VavStagedRtu) {
            systemProfile = (VavStagedRtu) L.ccu().systemProfile;
            relay1Cb.setChecked(systemProfile.getConfigEnabled("relay1") > 0);
            relay2Cb.setChecked(systemProfile.getConfigEnabled("relay2") > 0);
            relay3Cb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
            relay4Cb.setChecked(systemProfile.getConfigEnabled("relay4") > 0);
            relay5Cb.setChecked(systemProfile.getConfigEnabled("relay5") > 0);
            relay6Cb.setChecked(systemProfile.getConfigEnabled("relay6") > 0);
            relay7Cb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);
    
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
                    systemProfile = new VavStagedRtu();
                    L.ccu().systemProfile = systemProfile;
                    systemProfile.initTRSystem();
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
    }
    
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.relay1Cb:
                relay1Spinner.setEnabled(relay1Cb.isChecked());
                setConfigEnabledBackground("relay1",relay1Cb.isChecked() ? 1: 0);
                /*if (relay1Cb.isChecked())
                {
                    setConfigAssociationBackground("relay1", relay1Spinner.getSelectedItemPosition());
                }*/
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
}
