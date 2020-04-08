package a75f.io.renatus;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.DaikinIE;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registartion.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/6/18.
 */

public class VavIERtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    
    @BindView(R.id.analog1RTU)CheckBox analog1Cb;
    @BindView(R.id.analog2RTU)CheckBox analog2Cb;
    @BindView(R.id.analog3RTU)CheckBox analog3Cb;
    @BindView(R.id.humidificationCb)CheckBox humidificationCb;
    
    @BindView(R.id.coolingDatMin)Spinner coolingDatMin;
    @BindView(R.id.coolingDatMax)Spinner coolingDatMax;
    @BindView(R.id.heatingDatMin)Spinner heatingDatMin;
    @BindView(R.id.heatingDatMax)Spinner heatingDatMax;
    @BindView(R.id.spMin)Spinner spMin;
    @BindView(R.id.spMax)Spinner spMax;
    @BindView(R.id.equipmentIp) EditText equipAddr;
    
    @BindView(R.id.analog1RTUTest) Spinner coolingTest;
    @BindView(R.id.analog2RTUTest) Spinner spTest;
    @BindView(R.id.analog3RTUTest) Spinner heatingTest;
    @BindView(R.id.humidificationTest) Spinner humidificationTest;
    @BindView(R.id.oaMinTest) Spinner oaMinTest;
    @BindView(R.id.buttonNext)
    Button mNext;
    @BindView(R.id.btnEditIp)
    ImageView btnEditIp;
    
    VavIERtu systemProfile = null;
    String PROFILE = "VAV_IE_RTU";
    Prefs prefs;
    boolean isFromReg = false;
    
    public static VavAnalogRtuProfile newInstance()
    {
        return new VavAnalogRtuProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_rtuie, container, false);
        ButterKnife.bind(this, rootView);
        if(getArguments() != null) {
            isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
        }

        btnEditIp.setOnClickListener(view1 -> equipAddr.setEnabled(true));
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        prefs = new Prefs(getContext().getApplicationContext());
        if ((L.ccu().systemProfile instanceof VavIERtu))
        {
            systemProfile = (VavIERtu) L.ccu().systemProfile;
            analog1Cb.setChecked(systemProfile.getConfigEnabled("analog1") > 0);
            analog2Cb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
            analog3Cb.setChecked(systemProfile.getConfigEnabled("analog3") > 0);
            humidificationCb.setChecked(systemProfile.getConfigEnabled("humidification") > 0);
            setupAnalogLimitSelectors();
            setupEquipAddrEditor();
            
        } else {
            new AsyncTask<String, Void, Void>() {
        
                @Override
                protected void onPreExecute() {
                    ProgressDialogUtils.showProgressDialog(getActivity(),"Loading System Profile");
                    super.onPreExecute();
                }
        
                @Override
                protected Void doInBackground(final String... params) {
                    if (systemProfile != null) {
                        systemProfile.deleteSystemEquip();
                        L.ccu().systemProfile = null; //Makes sure that System Algos dont run until new profile is ready.
                    }
                    systemProfile = new VavIERtu();
                    systemProfile.addSystemEquip();
                    L.ccu().systemProfile = systemProfile;
                    return null;
                }
        
                @Override
                protected void onPostExecute(final Void result) {
                    setupAnalogLimitSelectors();
                    setupEquipAddrEditor();
                    ProgressDialogUtils.hideProgressDialog();
                    CCUHsApi.getInstance().saveTagsData();
                    CCUHsApi.getInstance().syncEntityTree();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        }

        if(isFromReg){
            mNext.setVisibility(View.VISIBLE);
        }
        else {
            mNext.setVisibility(View.GONE);
        }

        mNext.setOnClickListener(v -> {
            goTonext();
        });

        analog1Cb.setOnCheckedChangeListener(this);
        analog2Cb.setOnCheckedChangeListener(this);
        analog3Cb.setOnCheckedChangeListener(this);
        humidificationCb.setOnCheckedChangeListener(this);
    }
    
    public void setupEquipAddrEditor() {
        String eqIp = CCUHsApi.getInstance().readDefaultStrVal("point and system and config and ie and address");
        equipAddr.setText(eqIp);
        equipAddr.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final EditText editText = new EditText(getActivity());
            
                KeyListener keyListener = DigitsKeyListener.getInstance("0123456789.");
                editText.setKeyListener(keyListener);
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                             .setTitle("Internet Equipment IP")
                                             .setMessage(eqIp)
                                             .setView(editText)
                                             .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                                 @Override
                                                 public void onClick(DialogInterface dialog, int which) {
                            
                                                     if (editText.getText().toString().trim().length() == 0)
                                                     {
                                                         Toast.makeText(getActivity(), "IP Address Empty", Toast.LENGTH_SHORT).show();
                                                         return;
                                                     }
                                                     CCUHsApi.getInstance().writeDefaultVal("point and system and config and ie and address",editText.getText().toString().trim());
                                                     equipAddr.setText(editText.getText().toString());
                                                 }
                                             })
                                             .setNegativeButton("Cancel", null)
                                             .create();
                dialog.show();
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
        ArrayList<Double> coolingDatArr = new ArrayList<>();
        for (double cdat = 55;  cdat <= 70.0; cdat++)
        {
            coolingDatArr.add(cdat);
        }
        ArrayAdapter<Double> coolingDatAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, coolingDatArr);
        coolingDatAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        double val = systemProfile.getConfigVal("cooling and dat and min");
        coolingDatMin.setAdapter(coolingDatAdapter);
        coolingDatMin.setSelection(coolingDatAdapter.getPosition(val), false);
        coolingDatMax.setAdapter(coolingDatAdapter);
        val = systemProfile.getConfigVal("cooling and dat and max");
        coolingDatMax.setSelection(val != 0 ? coolingDatAdapter.getPosition(val) : coolingDatArr.size()-1 , false);
    
        ArrayList<Double> spArr = new ArrayList<>();
        for (int sp = 2;  sp <= 10; sp++)
        {
            spArr.add((double)sp/10);
        }
        ArrayAdapter<Double> spAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, spArr);
        spAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spMin.setAdapter(spAdapter);
        val = systemProfile.getConfigVal("staticPressure and min");
        spMin.setSelection(spAdapter.getPosition(val) , false);
    
        spMax.setAdapter(spAdapter);
        val = systemProfile.getConfigVal("staticPressure and max");
        spMax.setSelection(val != 0 ? spAdapter.getPosition(val) : spArr.size()-1, false);
    
        ArrayList<Double> heatingDatArr = new ArrayList<>();
        for (double hdat = 75;  hdat <= 130.0; hdat++)
        {
            heatingDatArr.add(hdat);
        }
        ArrayAdapter<Double> heatingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingDatArr);
        heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingDatMin.setAdapter(heatingAdapter);
        val = systemProfile.getConfigVal("heating and dat and min");
        heatingDatMin.setSelection(heatingAdapter.getPosition(val), false);
    
        heatingDatMax.setAdapter(heatingAdapter);
        val = systemProfile.getConfigVal("heating and dat and max");
        heatingDatMax.setSelection(val != 0 ? heatingAdapter.getPosition(val) : heatingAdapter.getPosition(100.0), false);
    
        
        ArrayAdapter<Double> coolingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, coolingDatArr);
        coolingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingTest.setAdapter(coolingSatTestAdapter);
        
        ArrayAdapter<Double> spTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, spArr);
        spTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spTest.setAdapter(spTestAdapter);
    
        ArrayAdapter<Double> heatingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingDatArr);
        heatingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingTest.setAdapter(heatingSatTestAdapter);
    
        ArrayList<Double> zoroToHundred = new ArrayList<>();
        for (double i = 0;  i <= 100.0; i++)
        {
            zoroToHundred.add(i);
        }
        ArrayAdapter<Double> humidificationTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        humidificationTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        humidificationTest.setAdapter(humidificationTestAdapter);
    
        ArrayAdapter<Double> oaMinTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        oaMinTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        oaMinTest.setAdapter(oaMinTestAdapter);
    
    
        coolingDatMin.setOnItemSelectedListener(this);
        coolingDatMax.setOnItemSelectedListener(this);
        spMin.setOnItemSelectedListener(this);
        spMax.setOnItemSelectedListener(this);
        heatingDatMin.setOnItemSelectedListener(this);
        heatingDatMax.setOnItemSelectedListener(this);
        coolingTest.setOnItemSelectedListener(this);
        spTest.setOnItemSelectedListener(this);
        heatingTest.setOnItemSelectedListener(this);
        humidificationTest.setOnItemSelectedListener(this);
        oaMinTest.setOnItemSelectedListener(this);
        
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.analog1RTU:
                setSelectionBackground("analog1", isChecked);
                break;
            case R.id.analog2RTU:
                setSelectionBackground("analog2", isChecked);
                break;
            case R.id.analog3RTU:
                setSelectionBackground("analog3", isChecked);
                break;
            case R.id.humidificationCb:
                setSelectionBackground("humidification", isChecked);
        }
    }
    
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        double val = Double.parseDouble(arg0.getSelectedItem().toString());
        switch (arg0.getId())
        {
            
            case R.id.coolingDatMin:
                setConfigBackground("cooling and dat and min", val);
                break;
            case R.id.coolingDatMax:
                setConfigBackground("cooling and dat and max", val);
                break;
            case R.id.spMin:
                setConfigBackground("staticPressure and min", val);
                break;
            case R.id.spMax:
                setConfigBackground("staticPressure and max", val);
                break;
            case R.id.heatingDatMin:
                setConfigBackground("heating and dat and min", val);
                break;
            case R.id.heatingDatMax:
                setConfigBackground("heating and dat and max", val);
                break;
            case R.id.analog1RTUTest:
                DaikinIE.sendCoolingDATAutoControl(val);
                break;
            case R.id.analog2RTUTest:
                DaikinIE.sendStaticPressure(val);
                break;
            case R.id.analog3Spinner:
                DaikinIE.sendCoolingDATAutoControl(val);
                break;
            case R.id.humidificationTest:
                DaikinIE.sendHumidityInput(val);
                break;
            case R.id.oaMinTest:
                DaikinIE.sendOAMinPos(val);
                break;
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
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
    
    private void setSelectionBackground(String analog, boolean selected) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected void onPreExecute() {
                ProgressDialogUtils.showProgressDialog(getActivity(),"Saving VAV System Configuration");
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
}
