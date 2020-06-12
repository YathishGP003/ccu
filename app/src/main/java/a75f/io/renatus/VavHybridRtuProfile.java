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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 2/11/19.
 */

public class VavHybridRtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    @BindView(R.id.toggleRelay1) ToggleButton relay1Tb;
    @BindView(R.id.toggleRelay2) ToggleButton relay2Tb;
    @BindView(R.id.toggleRelay3) ToggleButton relay3Tb;
    @BindView(R.id.toggleRelay4) ToggleButton relay4Tb;
    @BindView(R.id.toggleRelay5) ToggleButton relay5Tb;
    @BindView(R.id.toggleRelay6) ToggleButton relay6Tb;
    @BindView(R.id.toggleRelay7) ToggleButton relay7Tb;

    @BindView(R.id.relay1Spinner) Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner) Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner) Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner) Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner) Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner) Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner) Spinner relay7Spinner;

    @BindView(R.id.ahuAnalog1Min) Spinner analog1Min;
    @BindView(R.id.ahuAnalog1Max) Spinner analog1Max;
    @BindView(R.id.ahuAnalog2Min) Spinner analog2Min;
    @BindView(R.id.ahuAnalog2Max) Spinner analog2Max;
    @BindView(R.id.ahuAnalog3Min) Spinner analog3Min;
    @BindView(R.id.ahuAnalog3Max) Spinner analog3Max;
    @BindView(R.id.ahuAnalog4MinCooling) Spinner analog4MinCooling;
    @BindView(R.id.ahuAnalog4MaxCooling) Spinner analog4MaxCooling;
    @BindView(R.id.ahuAnalog4MinHeating) Spinner analog4MinHeating;
    @BindView(R.id.ahuAnalog4MaxHeating) Spinner analog4MaxHeating;

    @BindView(R.id.toggleAnalog1) ToggleButton ahuAnalog1Tb;
    @BindView(R.id.toggleAnalog2) ToggleButton ahuAnalog2Tb;
    @BindView(R.id.toggleAnalog3) ToggleButton ahuAnalog3Tb;
    @BindView(R.id.toggleAnalog4) ToggleButton ahuAnalog4Tb;

    @BindView(R.id.ahuAnalog1Test) Spinner ahuAnalog1Test;
    @BindView(R.id.ahuAnalog2Test) Spinner ahuAnalog2Test;
    @BindView(R.id.ahuAnalog3Test) Spinner ahuAnalog3Test;
    @BindView(R.id.ahuAnalog4Test) Spinner ahuAnalog4Test;



    @BindView(R.id.relay1Test)ToggleButton relay1Test;
    @BindView(R.id.relay2Test)ToggleButton relay2Test;
    @BindView(R.id.relay3Test)ToggleButton relay3Test;
    @BindView(R.id.relay4Test)ToggleButton relay4Test;
    @BindView(R.id.relay5Test)ToggleButton relay5Test;
    @BindView(R.id.relay6Test)ToggleButton relay6Test;
    @BindView(R.id.relay7Test)ToggleButton relay7Test;
    @BindView(R.id.tableRow4)
    TableRow tableRow4;
    @BindView(R.id.tableRow8)
    TableRow tableRow8;
    @BindView(R.id.imageRTUInput)
    ImageView imageView;

    @BindView(R.id.buttonNext)
    Button mNext;
    String PROFILE = "VAV_HYBRID_RTU";
    boolean isFromReg = false;
    Prefs prefs;
    VavAdvancedHybridRtu systemProfile = null;
    public static VavHybridRtuProfile newInstance()
    {
        return new VavHybridRtuProfile();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_hybridrtu, container, false);
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
        if (getUserVisibleHint()) {
            if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_HYBRID_RTU) {
                systemProfile = (VavAdvancedHybridRtu) L.ccu().systemProfile;
                relay1Tb.setChecked(systemProfile.getConfigEnabled("relay1") > 0);
                relay2Tb.setChecked(systemProfile.getConfigEnabled("relay2") > 0);
                relay3Tb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
                relay4Tb.setChecked(systemProfile.getConfigEnabled("relay4") > 0);
                relay5Tb.setChecked(systemProfile.getConfigEnabled("relay5") > 0);
                relay6Tb.setChecked(systemProfile.getConfigEnabled("relay6") > 0);
                relay7Tb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);

                ahuAnalog1Tb.setChecked(systemProfile.getConfigEnabled("analog1") > 0);
                ahuAnalog2Tb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
                ahuAnalog3Tb.setChecked(systemProfile.getConfigEnabled("analog3") > 0);
                ahuAnalog4Tb.setChecked(systemProfile.getConfigEnabled("analog4") > 0);

                setUpCheckBoxes();
                setUpSpinners();
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
                            L.ccu().systemProfile = null;
                        }
                        systemProfile = new VavAdvancedHybridRtu();
                        systemProfile.addSystemEquip();
                        L.ccu().systemProfile = systemProfile;
                        return null;
                    }

                    @Override
                    protected void onPostExecute(final Void result) {
                        setUpCheckBoxes();
                        setUpSpinners();
                        ProgressDialogUtils.hideProgressDialog();
                        CCUHsApi.getInstance().saveTagsData();
                        CCUHsApi.getInstance().syncEntityTree();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            }
        }

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
            }
        });

        if (getResources().getDisplayMetrics().xdpi == (float)149.824){
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(253,654);
            lp.setMargins(0, 63, 0, 0);
            imageView.setLayoutParams(lp);
        }
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (display.getMode().getRefreshRate() == (float)59.28){
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(253,654);
                lp.setMargins(0, 68, 0, 0);
                imageView.setLayoutParams(lp);

                TableLayout.LayoutParams tr = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
                tr.setMargins(0, 14, 0, 0);
                tableRow4.setLayoutParams(tr);

                tr.setMargins(0, 13, 0, 0);
                tableRow8.setLayoutParams(tr);
            }
        }
    }

    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        prefs.setBoolean("PROFILE_SETUP",true);
        prefs.setString("PROFILE",PROFILE);
        ((FreshRegistration)getActivity()).selectItem(19);
    }
    private void setUpCheckBoxes()
    {
        relay1Tb.setOnCheckedChangeListener(this);
        relay2Tb.setOnCheckedChangeListener(this);
        relay3Tb.setOnCheckedChangeListener(this);
        relay4Tb.setOnCheckedChangeListener(this);
        relay5Tb.setOnCheckedChangeListener(this);
        relay6Tb.setOnCheckedChangeListener(this);
        relay7Tb.setOnCheckedChangeListener(this);

        ahuAnalog1Tb.setOnCheckedChangeListener(this);
        ahuAnalog2Tb.setOnCheckedChangeListener(this);
        ahuAnalog3Tb.setOnCheckedChangeListener(this);
        ahuAnalog4Tb.setOnCheckedChangeListener(this);
    }
    
    private void setUpSpinners()
    {
        relay1Spinner.setSelection((int) systemProfile.getConfigAssociation("relay1"), false);
        relay2Spinner.setSelection((int) systemProfile.getConfigAssociation("relay2"), false);
        relay3Spinner.setSelection((int) systemProfile.getConfigAssociation("relay3"), false);
        relay4Spinner.setSelection((int) systemProfile.getConfigAssociation("relay4"), false);
        relay5Spinner.setSelection((int) systemProfile.getConfigAssociation("relay5"), false);
        relay6Spinner.setSelection((int) systemProfile.getConfigAssociation("relay6"), false);
        relay7Spinner.setSelection((int) systemProfile.getConfigAssociation("relay7"), false);
        relay1Spinner.setEnabled(relay1Tb.isChecked());
        relay2Spinner.setEnabled(relay2Tb.isChecked());
        relay3Spinner.setEnabled(relay3Tb.isChecked());
        relay4Spinner.setEnabled(relay4Tb.isChecked());
        relay5Spinner.setEnabled(relay5Tb.isChecked());
        relay6Spinner.setEnabled(relay6Tb.isChecked());
        relay7Spinner.setEnabled(relay7Tb.isChecked());
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
        analog1Min.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog1 and cooling and min")), false);
        analog1Max.setAdapter(analogAdapter);
        double analogVal = systemProfile.getConfigVal("analog1 and cooling and max");
        analog1Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1 , false);
        analog2Min.setAdapter(analogAdapter);
        analog2Min.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog2 and fan and min")), false);
        analog2Max.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog2 and fan and max");
        analog2Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1);
        analog3Min.setAdapter(analogAdapter);
        analog3Min.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog3 and heating and min")), false);
        analog3Max.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog3 and heating and max");
        analog3Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1);
        analog4MinCooling.setAdapter(analogAdapter);
        analog4MinCooling.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog4 and cooling and min")), false);
        analog4MaxCooling.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog4 and cooling and max");
        analog4MaxCooling.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1, false);
        analog4MinHeating.setAdapter(analogAdapter);
        analog4MinHeating.setSelection(analogAdapter.getPosition((int) systemProfile.getConfigVal("analog4 and heating and min")), false);
        analog4MaxHeating.setAdapter(analogAdapter);
        analogVal = systemProfile.getConfigVal("analog4 and heating and max");
        analog4MaxHeating.setSelection(analogVal != 0 ? analogAdapter.getPosition((int) analogVal) : analogArray.size() - 1, false);
    
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
            case R.id.toggleRelay1:
                relay1Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay1", isChecked ? 1 : 0);
                break;
            case R.id.toggleRelay2:
                relay2Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay2", isChecked ? 1 : 0);
                break;
            case R.id.toggleRelay3:
                relay3Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay3", isChecked ? 1 : 0);
                break;
            case R.id.toggleRelay4:
                relay4Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay4", isChecked ? 1 : 0);
                break;
            case R.id.toggleRelay5:
                relay5Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay5", isChecked ? 1 : 0);
                break;
            case R.id.toggleRelay6:
                relay6Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay6", isChecked ? 1 : 0);
                break;
            case R.id.toggleRelay7:
                relay7Spinner.setEnabled(isChecked);
                setConfigEnabledBackground("relay7", isChecked ? 1 : 0);
                break;
            case R.id.toggleAnalog1:
                setConfigEnabledBackground("analog1", isChecked ? 1 : 0);
                break;
            case R.id.toggleAnalog2:
                setConfigEnabledBackground("analog2", isChecked ? 1 : 0);
                break;
            case R.id.toggleAnalog3:
                setConfigEnabledBackground("analog3", isChecked ? 1 : 0);
                break;
            case R.id.toggleAnalog4:
                setConfigEnabledBackground("analog4", isChecked ? 1 : 0);
                break;
            case R.id.relay1Test:
            case R.id.relay2Test:
            case R.id.relay3Test:
            case R.id.relay4Test:
            case R.id.relay5Test:
            case R.id.relay6Test:
            case R.id.relay7Test:
                sendAnalogRelayTestSignal();
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
                if (relay1Tb.isChecked())
                {
                    setConfigAssociationBackground("relay1", relay1Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay2Spinner:
                if (relay2Tb.isChecked())
                {
                    setConfigAssociationBackground("relay2", relay2Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay3Spinner:
                if (relay3Tb.isChecked())
                {
                    setConfigAssociationBackground("relay3", relay3Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay4Spinner:
                if (relay4Tb.isChecked())
                {
                    setConfigAssociationBackground("relay4", relay4Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay5Spinner:
                if (relay5Tb.isChecked())
                {
                    setConfigAssociationBackground("relay5", relay5Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay6Spinner:
                if (relay6Tb.isChecked())
                {
                    setConfigAssociationBackground("relay6", relay6Spinner.getSelectedItemPosition());
                }
                break;
            case R.id.relay7Spinner:
                if (relay7Tb.isChecked())
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
            case R.id.ahuAnalog2Test:
            case R.id.ahuAnalog3Test:
            case R.id.ahuAnalog4Test:
                sendAnalogRelayTestSignal();
                break;
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0)
    {
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
    
    private void setUserIntentBackground(String query, double val) {
        
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground( final Void ... params ) {
                TunerUtil.writeSystemUserIntentVal(query, val);
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    private void setConfigEnabledBackground(String config, double val)
    {
        new AsyncTask<String, Void, Void>()
        {
            @Override
            protected Void doInBackground(final String... params)
            {
                systemProfile.setConfigEnabled(config, val);
                systemProfile.updateStagesSelected();
                return null;
            }
            
            @Override
            protected void onPostExecute(final Void result)
            {
                if (val == 0) {
                    updateSystemMode();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    private void setConfigAssociationBackground(String config, double val)
    {
        new AsyncTask<String, Void, Void>()
        {
            @Override
            protected void onPreExecute() {
                ProgressDialogUtils.showProgressDialog(getActivity(),"Saving VAV Configuration");
                super.onPreExecute();
            }
            @Override
            protected Void doInBackground(final String... params)
            {
                systemProfile.setConfigAssociation(config, val);
                systemProfile.updateStagesSelected();
                return null;
            }
            
            @Override
            protected void onPostExecute(final Void result)
            {
                ProgressDialogUtils.hideProgressDialog();
                updateSystemMode();
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
    
    public void sendAnalogRelayTestSignal() {
        
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
    
        msg.analog0.set(getAnalogVal(systemProfile.getConfigVal("analog1 and cooling and min"), systemProfile.getConfigVal("analog1 and cooling and max"),
                Double.parseDouble(ahuAnalog1Test.getSelectedItem().toString())));
    
        msg.analog1.set(getAnalogVal(systemProfile.getConfigVal("analog2 and fan and min"), systemProfile.getConfigVal("analog2 and fan and max"),
                Double.parseDouble(ahuAnalog2Test.getSelectedItem().toString())));
    
        msg.analog2.set(getAnalogVal(systemProfile.getConfigVal("analog3 and heating and min"), systemProfile.getConfigVal("analog3 and heating and max"),
                Double.parseDouble(ahuAnalog3Test.getSelectedItem().toString())));
    
        msg.analog3.set((short)(Double.parseDouble(ahuAnalog4Test.getSelectedItem().toString())));
        
        short relayStatus = (short) ((relay1Test.isChecked() ? 1 : 0)
                                     | (relay2Test.isChecked() ? 1 << 1 : 0)
                                     | (relay3Test.isChecked() ? 1 << 2 : 0)
                                     | (relay4Test.isChecked() ? 1 << 3 : 0)
                                     | (relay5Test.isChecked() ? 1 << 4 : 0)
                                     | (relay6Test.isChecked() ? 1 << 5 : 0)
                                     | (relay7Test.isChecked() ? 1 << 6 : 0));
    
        msg.relayBitmap.set(relayStatus);
        
        MeshUtil.sendStructToCM(msg);
    }
    
    
    short getAnalogVal(double min, double max, double val) {
        return max > min ? (short) (10 * (min + (max - min) * val/100)) : (short) (10 * (min - (min - max) * val/100));
    }
}

