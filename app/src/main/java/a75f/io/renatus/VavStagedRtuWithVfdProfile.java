package a75f.io.renatus;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registartion.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 2/18/19.
 */

public class VavStagedRtuWithVfdProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    @BindView(R.id.toggleRelay1) ToggleButton relay1Tb;
    @BindView(R.id.toggleRelay2) ToggleButton relay2Tb;
    @BindView(R.id.toggleRelay3) ToggleButton relay3Tb;
    @BindView(R.id.toggleRelay4) ToggleButton relay4Tb;
    @BindView(R.id.toggleRelay5) ToggleButton relay5Tb;
    @BindView(R.id.toggleRelay6) ToggleButton relay6Tb;
    @BindView(R.id.toggleRelay7) ToggleButton relay7Tb;
    @BindView(R.id.toggleAnalog2) ToggleButton analog2Tb;


    @BindView(R.id.relay1Spinner)Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner)Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner)Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner)Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner)Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner)Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner)Spinner relay7Spinner;
    @BindView(R.id.fanspeedSpinner)Spinner analog2TestSpinner;

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
    String TAG = "VAV_STAGED_RTU_VFD";
    @BindView(R.id.buttonNext)
    Button mNext;
    String PROFILE = "VAV_STAGED_RTU_VFD";
    Prefs prefs;
    boolean isFromReg = false;
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
        if(getArguments() != null) {
            isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
        }
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        prefs = new Prefs(getContext().getApplicationContext());
        //if(getUserVisibleHint()) {
            Log.i(TAG,"isVisibletoUser:"+getUserVisibleHint());
            if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU) {
                systemProfile = (VavStagedRtuWithVfd) L.ccu().systemProfile;
                relay1Tb.setChecked(systemProfile.getConfigEnabled("relay1") > 0);
                relay2Tb.setChecked(systemProfile.getConfigEnabled("relay2") > 0);
                relay3Tb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
                relay4Tb.setChecked(systemProfile.getConfigEnabled("relay4") > 0);
                relay5Tb.setChecked(systemProfile.getConfigEnabled("relay5") > 0);
                relay6Tb.setChecked(systemProfile.getConfigEnabled("relay6") > 0);
                relay7Tb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);
                analog2Tb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
                setUpCheckBoxes();
                setUpSpinners();
            } else {

                new AsyncTask<String, Void, Void>() {

                    ProgressDialog progressDlg = new ProgressDialog(getActivity());

                    @Override
                    protected void onPreExecute() {
                        progressDlg.setMessage("Loading System Profile");
                        progressDlg.setCancelable(false);
                        progressDlg.show();
                        super.onPreExecute();
                    }

                    @Override
                    protected Void doInBackground(final String... params) {
                        if (systemProfile != null) {
                            systemProfile.deleteSystemEquip();
                            L.ccu().systemProfile = null;
                        }
                        systemProfile = new VavStagedRtuWithVfd();
                        systemProfile.addSystemEquip();
                        L.ccu().systemProfile = systemProfile;
                        return null;
                    }

                    @Override
                    protected void onPostExecute(final Void result) {
                        setUpCheckBoxes();
                        setUpSpinners();
                        CCUHsApi.getInstance().saveTagsData();
                        CCUHsApi.getInstance().syncEntityTree();
                        progressDlg.dismiss();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            }
        //}



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
    }

    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        prefs.setBoolean("PROFILE_SETUP",true);
        prefs.setString("PROFILE",PROFILE);
        ((FreshRegistration)getActivity()).selectItem(18);
    }

    private void setUpCheckBoxes() {

        relay1Tb.setOnCheckedChangeListener(this);
        relay2Tb.setOnCheckedChangeListener(this);
        relay3Tb.setOnCheckedChangeListener(this);
        relay4Tb.setOnCheckedChangeListener(this);
        relay5Tb.setOnCheckedChangeListener(this);
        relay6Tb.setOnCheckedChangeListener(this);
        relay7Tb.setOnCheckedChangeListener(this);
        analog2Tb.setOnCheckedChangeListener(this);

        relay1Test.setOnCheckedChangeListener(this);
        relay2Test.setOnCheckedChangeListener(this);
        relay3Test.setOnCheckedChangeListener(this);
        relay4Test.setOnCheckedChangeListener(this);
        relay5Test.setOnCheckedChangeListener(this);
        relay6Test.setOnCheckedChangeListener(this);
        relay7Test.setOnCheckedChangeListener(this);
    }

    private void setUpSpinners() {
        relay1Spinner.setSelection((int)systemProfile.getConfigAssociation("relay1"), false);
        relay2Spinner.setSelection((int)systemProfile.getConfigAssociation("relay2"), false);
        relay3Spinner.setSelection((int)systemProfile.getConfigAssociation("relay3"), false);
        relay4Spinner.setSelection((int)systemProfile.getConfigAssociation("relay4"), false);
        relay5Spinner.setSelection((int)systemProfile.getConfigAssociation("relay5"), false);
        relay6Spinner.setSelection((int)systemProfile.getConfigAssociation("relay6"), false);
        relay7Spinner.setSelection((int)systemProfile.getConfigAssociation("relay7"), false);

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
        analog2CoolStage1.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage1"), false);
        analog2CoolStage2.setAdapter(analogAdapter);
        analog2CoolStage2.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage2"), false);
        analog2CoolStage3.setAdapter(analogAdapter);
        analog2CoolStage3.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage3"), false);
        analog2CoolStage4.setAdapter(analogAdapter);
        analog2CoolStage4.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage4"), false);
        analog2CoolStage5.setAdapter(analogAdapter);
        analog2CoolStage5.setSelection((int)systemProfile.getConfigVal("analog2 and cooling and stage5"), false);

        analog2HeatStage1.setAdapter(analogAdapter);
        analog2HeatStage1.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage1"), false);
        analog2HeatStage2.setAdapter(analogAdapter);
        analog2HeatStage2.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage2"), false);
        analog2HeatStage3.setAdapter(analogAdapter);
        analog2HeatStage3.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage3"), false);
        analog2HeatStage4.setAdapter(analogAdapter);
        analog2HeatStage4.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage4"), false);
        analog2HeatStage5.setAdapter(analogAdapter);
        analog2HeatStage5.setSelection((int)systemProfile.getConfigVal("analog2 and heating and stage5"), false);

        analog2TestSpinner.setOnItemSelectedListener(this);
        updateAnalogOptions();
    }

    private void updateAnalogOptions() {
        systemProfile.updateStagesSelected();
        boolean analogEnabled = analog2Tb.isChecked();
        analog2Economizer.setEnabled(false);
        analog2Recirculate.setEnabled(false);
        analog2CoolStage1.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.COOLING_1));
        analog2CoolStage2.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.COOLING_2));
        analog2CoolStage3.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.COOLING_3));
        analog2CoolStage4.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.COOLING_4));
        analog2CoolStage5.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.COOLING_5));

        analog2HeatStage1.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.HEATING_1));
        analog2HeatStage2.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.HEATING_2));
        analog2HeatStage3.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.HEATING_3));
        analog2HeatStage4.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.HEATING_4));
        analog2HeatStage5.setEnabled(analogEnabled && systemProfile.isStageEnabled(Stage.HEATING_5));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.toggleRelay1:
                relay1Spinner.setEnabled(relay1Tb.isChecked());
                setConfigEnabledBackground("relay1",relay1Tb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay2:
                relay2Spinner.setEnabled(relay2Tb.isChecked());
                setConfigEnabledBackground("relay2",relay2Tb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay3:
                relay3Spinner.setEnabled(relay3Tb.isChecked());
                setConfigEnabledBackground("relay3",relay3Tb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay4:
                relay4Spinner.setEnabled(relay4Tb.isChecked());
                setConfigEnabledBackground("relay4",relay4Tb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay5:
                relay5Spinner.setEnabled(relay5Tb.isChecked());
                setConfigEnabledBackground("relay5",relay5Tb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay6:
                relay6Spinner.setEnabled(relay6Tb.isChecked());
                setConfigEnabledBackground("relay6",relay6Tb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay7:
                relay7Spinner.setEnabled(relay7Tb.isChecked());
                setConfigEnabledBackground("relay7",relay7Tb.isChecked() ? 1: 0);
                break;
            case R.id.toggleAnalog2:
                setConfigEnabledBackground("analog2",analog2Tb.isChecked() ? 1: 0);
                break;
            case R.id.relay1Test:
            case R.id.relay2Test:
            case R.id.relay3Test:
            case R.id.relay4Test:
            case R.id.relay5Test:
            case R.id.relay6Test:
            case R.id.relay7Test:
                sendRelayActivationTestSignal();
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
                if (relay1Tb.isChecked())
                {
                    setConfigAssociationBackground("relay1", relay1Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay2Spinner:
                if (relay2Tb.isChecked())
                {
                    setConfigAssociationBackground("relay2", relay2Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay3Spinner:
                if (relay3Tb.isChecked())
                {
                    setConfigAssociationBackground("relay3", relay3Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay4Spinner:
                if (relay4Tb.isChecked())
                {
                    setConfigAssociationBackground("relay4", relay4Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay5Spinner:
                if (relay5Tb.isChecked())
                {
                    setConfigAssociationBackground("relay5", relay5Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay6Spinner:
                if (relay6Tb.isChecked())
                {
                    setConfigAssociationBackground("relay6", relay6Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.relay7Spinner:
                if (relay7Tb.isChecked())
                {
                    setConfigAssociationBackground("relay7", relay7Spinner.getSelectedItemPosition());
                    updateAnalogOptions();
                }
                break;
            case R.id.fanspeedSpinner:
                sendRelayActivationTestSignal();
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

    public void updateSystemMode() {
        SystemMode systemMode = SystemMode.values()[(int)systemProfile.getUserIntentVal("rtu and mode")];
        if (systemMode == SystemMode.OFF) {
            return;
        }
        if ((systemMode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable() || !systemProfile.isHeatingAvailable()))
                || (systemMode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable())
                || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable()))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);//, AlertDialog.THEME_HOLO_DARK);
            String str = "Operational Mode changed from '" + systemMode.name() + "' to '" + SystemMode.OFF.name() + "' based on changed equipment selection.";
            str = str + "\nPlease select appropriate operational mode from System Settings.";
            builder.setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .setTitle("Operational Mode Changed")
                    .setMessage(str);

            AlertDialog dlg = builder.create();
            dlg.show();
            setUserIntentBackground("rtu and mode", SystemMode.OFF.ordinal());
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

    private void setConfigEnabledBackground(String config, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                systemProfile.setConfigEnabled(config, val);
                systemProfile.updateStagesSelected();
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                if (val == 0) {
                    updateSystemMode();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void setConfigAssociationBackground(String config, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                systemProfile.setConfigAssociation(config, val);
                systemProfile.updateStagesSelected();
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                updateSystemMode();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void setConfigBackground(String config, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                systemProfile.setConfigVal(config, val);
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    public void sendRelayActivationTestSignal() {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        short relayStatus = (short) ((relay1Test.isChecked() ? 1 : 0)
                                     | (relay2Test.isChecked() ? 1 << 1 : 0)
                                     | (relay3Test.isChecked() ? 1 << 2 : 0)
                                     | (relay4Test.isChecked() ? 1 << 3 : 0)
                                     | (relay5Test.isChecked() ? 1 << 4 : 0)
                                     | (relay6Test.isChecked() ? 1 << 5 : 0)
                                     | (relay7Test.isChecked() ? 1 << 6 : 0));
    
        msg.analog1.set((short)(10 * Double.parseDouble(analog2TestSpinner.getSelectedItem().toString())));
        msg.relayBitmap.set(relayStatus);
        MeshUtil.sendStructToCM(msg);
    }
}
