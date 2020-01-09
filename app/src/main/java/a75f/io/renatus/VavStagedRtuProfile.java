package a75f.io.renatus;


import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registartion.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;


public class VavStagedRtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{

    @BindView(R.id.toggleRelay1)ToggleButton  relay1Cb;
    @BindView(R.id.toggleRelay2)ToggleButton  relay2Cb;
    @BindView(R.id.toggleRelay3)ToggleButton  relay3Cb;
    @BindView(R.id.toggleRelay4)ToggleButton  relay4Cb;
    @BindView(R.id.toggleRelay5)ToggleButton  relay5Cb;
    @BindView(R.id.toggleRelay6)ToggleButton  relay6Cb;
    @BindView(R.id.toggleRelay7)ToggleButton  relay7Cb;

    @BindView(R.id.relay1Spinner)Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner)Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner)Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner)Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner)Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner)Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner)Spinner relay7Spinner;

    @BindView(R.id.relay1Test)ToggleButton relay1Test;
    @BindView(R.id.relay2Test)ToggleButton relay2Test;
    @BindView(R.id.relay3Test)ToggleButton relay3Test;
    @BindView(R.id.relay4Test)ToggleButton relay4Test;
    @BindView(R.id.relay5Test)ToggleButton relay5Test;
    @BindView(R.id.relay6Test)ToggleButton relay6Test;
    @BindView(R.id.relay7Test)ToggleButton relay7Test;
    @BindView(R.id.imageRTUInput)
    ImageView imageView;

    @BindView(R.id.buttonNext)
    Button mNext;
    VavStagedRtu systemProfile = null;
    String TAG = "VAV_STAGED_RTU";
    String PROFILE = "VAV_STAGED_RTU";
    boolean isFromReg = false;
            Prefs prefs;
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
        if(getArguments() != null) {
            isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
        }
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        prefs = new Prefs(getContext().getApplicationContext());

            Log.i(TAG,"isVisibletoUser:"+getUserVisibleHint());
            if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_RTU) {
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
                        systemProfile = new VavStagedRtu();
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
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(255,475);
            lp.setMargins(0, 62, 0, 0);
            imageView.setLayoutParams(lp);
        }
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (display.getMode().getRefreshRate() == (float)59.28){
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(255,475);
                lp.setMargins(0, 66, 0, 0);
                imageView.setLayoutParams(lp);
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
        relay1Spinner.setSelection((int)systemProfile.getConfigAssociation("relay1"), false);
        relay2Spinner.setSelection((int)systemProfile.getConfigAssociation("relay2"), false);
        relay3Spinner.setSelection((int)systemProfile.getConfigAssociation("relay3"), false);
        relay4Spinner.setSelection((int)systemProfile.getConfigAssociation("relay4"), false);
        relay5Spinner.setSelection((int)systemProfile.getConfigAssociation("relay5"), false);
        relay6Spinner.setSelection((int)systemProfile.getConfigAssociation("relay6"), false);
        relay7Spinner.setSelection((int)systemProfile.getConfigAssociation("relay7"), false);

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

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.toggleRelay1:
                relay1Spinner.setEnabled(relay1Cb.isChecked());
                setConfigEnabledBackground("relay1",relay1Cb.isChecked() ? 1: 0);
                /*if (relay1Cb.isChecked())
                {
                    setConfigAssociationBackground("relay1", relay1Spinner.getSelectedItemPosition());
                }*/
                break;
            case R.id.toggleRelay2:
                relay2Spinner.setEnabled(relay2Cb.isChecked());
                setConfigEnabledBackground("relay2",relay2Cb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay3:
                relay3Spinner.setEnabled(relay3Cb.isChecked());
                setConfigEnabledBackground("relay3",relay3Cb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay4:
                relay4Spinner.setEnabled(relay4Cb.isChecked());
                setConfigEnabledBackground("relay4",relay4Cb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay5:
                relay5Spinner.setEnabled(relay5Cb.isChecked());
                setConfigEnabledBackground("relay5",relay5Cb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay6:
                relay6Spinner.setEnabled(relay6Cb.isChecked());
                setConfigEnabledBackground("relay6",relay6Cb.isChecked() ? 1: 0);
                break;
            case R.id.toggleRelay7:
                relay7Spinner.setEnabled(relay7Cb.isChecked());
                setConfigEnabledBackground("relay7",relay7Cb.isChecked() ? 1: 0);
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
            protected void onPreExecute() {
                ProgressDialogUtils.showProgressDialog(getActivity(),"Saving VAV Configuration");
                super.onPreExecute();
            }
            @Override
            protected Void doInBackground( final String ... params ) {
                systemProfile.setConfigAssociation(config, val);
                systemProfile.updateStagesSelected();
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                updateSystemMode();
                ProgressDialogUtils.hideProgressDialog();
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
        
        msg.relayBitmap.set(relayStatus);
        MeshUtil.sendStructToCM(msg);
    }
}