package a75f.io.renatus.ENGG;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.projecthaystack.client.HClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Queries;
import a75f.io.api.haystack.Site;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.filesystem.FileSystemTools;
import a75f.io.logic.logtasks.UploadLogs;
import a75f.io.logic.tuners.TunerEquip;
import a75f.io.logic.util.RxTask;
import a75f.io.messaging.client.MessagingClient;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CCUUtils;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.usbserial.UsbSerialUtil;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
  * Created by samjithsadasivan on 12/18/18.
  */
public class DevSettings extends Fragment implements AdapterView.OnItemSelectedListener
{
    public static DevSettings newInstance(){
        return new DevSettings();
    }
    private int previousControlLoopFrequency = 0;

    private int prevoiusAhuConnectPort = 0;

    @BindView(R.id.EnableANRLayout)
    LinearLayout EnableANRLayout;
                                    
    @BindView(R.id.biskitModBtn)
    ToggleButton biskitModeBtn;
    @BindView(R.id.biskitModLayout)
    LinearLayout biskitModLayout;
    @BindView(R.id.loopspinner)
    Spinner loopSpinner;
    
    @BindView(R.id.logCaptureBtn)
    Button logCaptureBtn;

    @BindView(R.id.logUploadBtn)
    Button logUploadBtn;
    
    @BindView(R.id.pullDataBtn)
    Button pullDataBtn;
    
    @BindView(R.id.deleteHis)
    Button deleteHis;

    @BindView(R.id.clearHisData)
    Button clearHisData;

    @BindView(R.id.forceSyncBtn)
    Button forceSyncBtn;
    
    @BindView(R.id.testModBtn)
    ToggleButton testModBtn;
    
    @BindView(R.id.testModLayout)
    LinearLayout testModLayout;
    
    @BindView(R.id.outsideTemp)
    Spinner outsideTemp;
    
    @BindView(R.id.outsideHumidity)
    Spinner outsideHumidity;
    
    @BindView(R.id.btuProxyBtn) ToggleButton btuProxyBtn;
    @BindView(R.id.inletWaterTemp) Spinner inletWaterTemp;
    @BindView(R.id.outletWaterTemp) Spinner outletWaterTemp;
    @BindView(R.id.cwFlowRate) Spinner cwFlowRate;
    @BindView(R.id.btuProxyLayout) LinearLayout btuProxyLayout;
    
    @BindView(R.id.imageCMSerial) ImageView cmSerial;
    @BindView(R.id.imageMBSerial) ImageView mbSerial;

    @BindView(R.id.imageConnectSerial) ImageView connectSerial;
    @BindView(R.id.reconnectSerial) Button reconnectSerial;
    public  @BindView(R.id.daikin_theme_config) CheckBox daikinThemeConfig;
    public  @BindView(R.id.carrier_theme_config) CheckBox carrierThemeConfig;

    @BindView(R.id.ackdMessagingBtn) ToggleButton ackdMessagingBtn;

    @BindView(R.id.crashButton) Button crashButton;
    public @BindView(R.id.btnRestart) Button btnRestart;

    public @BindView(R.id.resetPassword) Button resetPassword;

    public @BindView(R.id.resetAppBtn) Button resetAppBtn;

    public @BindView(R.id.serialCommTimeOut) EditText etSerialCommTimeOut;

    public @BindView(R.id.saveSerialCommTimeOut) Button btnSerialCommTimeOut;

    public @BindView(R.id.registerRequestCount) EditText etRegisterRequestCount;

    public @BindView(R.id.saveRegisterRequestCount) Button btnregisterRequestCount;

    public @BindView(R.id.cacheSyncFrequency) Spinner cacheSyncFrequency;

    public @BindView(R.id.loglevel) Spinner logLevelSpinner;

    public @BindView(R.id.advancedAhuConnectPort) Spinner advancedAhuConnectPort;
    SharedPreferences spDefaultPrefs = null;

    private final CompositeDisposable disposable = new CompositeDisposable();

    public @BindView(R.id.anrReportBtn) ToggleButton anrReporting;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                                  Bundle savedInstanceState) {
         View rootView = inflater.inflate(R.layout.fragment_dev_settings, container, false);
         ButterKnife.bind(this , rootView);
         spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
         return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
         super.onViewCreated(view, savedInstanceState);

        biskitModeBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                    .edit().putBoolean("biskit_mode", b).apply();

            if(BuildConfig.BUILD_TYPE.equals("qa") || BuildConfig.BUILD_TYPE.equals("dev_qa")) {
                biskitModLayout.setVisibility(b?View.VISIBLE :View.INVISIBLE);
            }
        });
        biskitModeBtn.setChecked(Globals.getInstance().isSimulation());
        if(BuildConfig.BUILD_TYPE.equals("qa") || BuildConfig.BUILD_TYPE.equals("dev_qa")) {
            biskitModLayout.setVisibility(Globals.getInstance().isSimulation() ? View.VISIBLE : View.INVISIBLE);
        }
        List<Integer> controlLoopFrequency = IntStream.rangeClosed(1,60)
                .boxed().collect(Collectors.toList());
        ArrayAdapter<Integer> ControlLoopAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, controlLoopFrequency);
        ControlLoopAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        loopSpinner.setAdapter(ControlLoopAdapter);
        previousControlLoopFrequency = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getInt("control_loop_frequency", 60);
        loopSpinner.setSelection(ControlLoopAdapter.getPosition(previousControlLoopFrequency));
        loopSpinner.setOnItemSelectedListener(this);
        logCaptureBtn.setOnClickListener(view13 -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle("Log File Name ");
            FileSystemTools fileSystemTools = new FileSystemTools(getContext().getApplicationContext());
            String date = fileSystemTools.timeStamp();

            //alert.setMessage(date);
            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText("Logs_"+date);
            input.setTextSize(20);
            alert.setView(input);

            alert.setPositiveButton("Ok", (dialog, whichButton) -> new Thread() {
                @Override
                public void run() {
                    try {
                        fileSystemTools.writeLogCat(input.getText().toString() + ".txt");
                    }
                    catch (IOException | SecurityException ex) {
                        ex.printStackTrace();
                        getActivity().runOnUiThread(() -> showErrorDialog(
                                "Unable to save log file: " + ex.getMessage()));
                    }
                }
            }.start());

            alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
                // Canceled.
            });

            alert.show();
        });

        logUploadBtn.setOnClickListener( v -> new Thread() {
            @Override
            public void run() {
                UploadLogs.instanceOf().saveCcuLogs();
            }
        }.start());

        pullDataBtn.setOnClickListener(view14 -> disposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Pulling building tuners to CCU"),
                () -> {
                    Site site = CCUHsApi.getInstance().getSite();
                    HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                    CCUHsApi.getInstance().importBuildingSchedule(site.getId(), hClient);
                    TunerEquip.INSTANCE.syncBuildingTuners(CCUHsApi.getInstance());
                },
                ProgressDialogUtils::hideProgressDialog
        )));
        
        deleteHis.setOnClickListener(view15 -> {
            CcuLog.d(L.TAG_CCU," deleteHis data ");
            disposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                    () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting history data"),
                    () -> CCUHsApi.getInstance().deleteHistory(),
                    ProgressDialogUtils::hideProgressDialog
            ));
        });

        clearHisData.setOnClickListener(view15 -> {
            CcuLog.d(L.TAG_CCU," Clear History data(Respecting Backfill time) ");
            RxTask.executeAsync(() -> CCUHsApi.getInstance().trimObjectBoxHisStore());
        });

        forceSyncBtn.setOnClickListener(view112 -> {
            CcuLog.d(L.TAG_CCU," forceSync site data ");

            CCUHsApi.getInstance().resyncSiteTree();
        });

        ackdMessagingBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                    .edit().putBoolean("ackd_messaging_enabled", b).apply();

            MessagingClient.getInstance().init();
        });
        ackdMessagingBtn.setChecked(Globals.getInstance().isAckdMessagingEnabled());

        testModBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                   .edit().putBoolean("weather_test", b).apply();
            testModLayout.setVisibility(b?View.VISIBLE :View.INVISIBLE);
        });
        testModBtn.setChecked(Globals.getInstance().isWeatherTest());
        testModLayout.setVisibility(Globals.getInstance().isWeatherTest()?View.VISIBLE :View.INVISIBLE);

        List<Integer> zoroToHundred = IntStream.rangeClosed(-20,120)
                .boxed().collect(Collectors.toList());
        ArrayAdapter<Integer> zeroToHundredDataAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        zeroToHundredDataAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        outsideTemp.setAdapter(zeroToHundredDataAdapter);
        outsideTemp.setOnItemSelectedListener(this);
        outsideTemp.setSelection(zeroToHundredDataAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                                                             .getInt("outside_temp", 0)));
        outsideHumidity.setAdapter(zeroToHundredDataAdapter);
        outsideHumidity.setOnItemSelectedListener(this);
        outsideHumidity.setSelection(zeroToHundredDataAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                                                             .getInt("outside_humidity", 0)));
        
        cmSerial.setImageResource(LSerial.getInstance().isConnected() ? android.R.drawable.checkbox_on_background
                                                                      : android.R.drawable.checkbox_off_background);
        mbSerial.setImageResource(LSerial.getInstance().isModbusConnected() ? android.R.drawable.checkbox_on_background
                                      : android.R.drawable.checkbox_off_background);

        connectSerial.setImageResource(LSerial.getInstance().isConnectModuleConnected() ? android.R.drawable.checkbox_on_background
                : android.R.drawable.checkbox_off_background);
        reconnectSerial.setOnClickListener(view111 -> triggerRebirth(getActivity()));

        if (BuildConfig.BUILD_TYPE.equals("local")
            || BuildConfig.BUILD_TYPE.equals("dev")
            || BuildConfig.BUILD_TYPE.equals("qa")
            || BuildConfig.BUILD_TYPE.equals("dev_qa")) {

            crashButton.setVisibility(View.VISIBLE);
            crashButton.setOnClickListener(view1 -> {
                throw new RuntimeException("Test Crash"); // Force a crash
            });
        }

        configureBtuProxy(zeroToHundredDataAdapter);

        btnRestart.setOnClickListener(v -> CCUUiUtil.triggerRestart(getContext()));

        daikinThemeConfig.setChecked(CCUUiUtil.isDaikinThemeEnabled(getContext()));
        daikinThemeConfig.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .edit()
                    .putBoolean(getContext().getString(R.string.prefs_theme_key), isChecked)
                    .apply();

            // Disable carrierThemeConfig checkbox when daikinThemeConfig is checked
            if (isChecked) {
                carrierThemeConfig.setChecked(false);
            }
        });

        carrierThemeConfig.setChecked(CCUUiUtil.isCarrierThemeEnabled(getContext()));
        carrierThemeConfig.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .edit()
                    .putBoolean(getContext().getString(R.string.prefs_carrier_theme_key), isChecked)
                    .apply();

            if (isChecked) {
                daikinThemeConfig.setChecked(false);
            }
        });


        resetPassword.setOnClickListener(view12 -> {
            final EditText taskEditText = new EditText(getActivity());
            KeyListener keyListener = DigitsKeyListener.getInstance("0123456789");
            taskEditText.setKeyListener(keyListener);

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Enter passcode")
                    .setMessage("Enter default 75F password to reset application security passwords.")
                    .setView(taskEditText)
                    .setPositiveButton("Done", (dialog1, which) -> {
                        if (taskEditText.getText().toString().trim().equals("7575")) {
                            dialog1.dismiss();
                            CCUUtils.resetPasswords(RenatusApp.getAppContext());
                            Toast.makeText(getActivity(), "Password reset succeeded", Toast.LENGTH_SHORT).show();
                        } else {
                            taskEditText.getText().clear();
                            Toast.makeText(getActivity(), "Incorrect passcode , Try Again!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setCancelable(false)
                    .create();
            dialog.show();
        });

        ArrayList<Integer> oneToFifteen = new ArrayList<>();
        for (int val = 1;  val <= 15; val++)
        {
            oneToFifteen.add(val);
        }
        ArrayAdapter<Integer> oneToFifteenAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, oneToFifteen);
        oneToFifteenAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        cacheSyncFrequency.setAdapter(oneToFifteenAdapter);
        cacheSyncFrequency.setOnItemSelectedListener(this);
        cacheSyncFrequency.setSelection(oneToFifteenAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getInt("cacheSyncFrequency", 1)));


        ArrayAdapter<String> portListAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, new String[]{"No Connect Module","CCU_USB_PORT", "CM_COM_PORT2"});
        oneToFifteenAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        advancedAhuConnectPort.setAdapter(portListAdapter);
        prevoiusAhuConnectPort= UsbSerialUtil.getPreferredConnectModuleSerialType(getContext());
        advancedAhuConnectPort.setSelection(prevoiusAhuConnectPort);
        advancedAhuConnectPort.setOnItemSelectedListener(this);


        resetAppBtn.setOnClickListener((View.OnClickListener) view16 -> {
            CcuLog.d(L.TAG_CCU," ResetAppState ");
            L.ccu().systemProfile.reset();
            for (ZoneProfile p : L.ccu().zoneProfiles) {
                p.reset();
            }
            L.ccu().zoneProfiles.clear();
            Globals.getInstance().loadEquipProfiles();
        });


        etRegisterRequestCount.setText(String.valueOf(spDefaultPrefs.getInt("registerRequestCount", 3)));
        etSerialCommTimeOut.setText(String.valueOf(spDefaultPrefs.getInt("serialCommTimeOut", 300)));

        etRegisterRequestCount.setOnClickListener(view110 -> {
            etRegisterRequestCount.setCursorVisible(true);
            etRegisterRequestCount.requestFocus();
        });

        etSerialCommTimeOut.setOnClickListener(view19 -> {
            etSerialCommTimeOut.setCursorVisible(true);
            etSerialCommTimeOut.requestFocus();
        });

        btnregisterRequestCount.setOnClickListener(view18 -> {
            spDefaultPrefs.edit().putInt("registerRequestCount",
                    Integer.parseInt(etRegisterRequestCount.getText().toString())).apply();
            etRegisterRequestCount.setCursorVisible(false);
            hideKeyboard(view18);
            Toast.makeText(getActivity(), "Saved.", Toast.LENGTH_SHORT).show();
        });

        btnSerialCommTimeOut.setOnClickListener(view17 -> {
            spDefaultPrefs.edit().putInt("serialCommTimeOut",
                    Integer.parseInt(etSerialCommTimeOut.getText().toString())).apply();
            etSerialCommTimeOut.setCursorVisible(false);
            hideKeyboard(view17);
            Toast.makeText(getActivity(), "Saved.", Toast.LENGTH_SHORT).show();
        });

        //Disable ANR reporting UI till we figure out an alternative for instabug.
        EnableANRLayout.setVisibility(View.GONE);

        anrReporting.setChecked(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting"
                , Context.MODE_PRIVATE).getBoolean("anr_reporting_enabled", false));
        anrReporting.setOnCheckedChangeListener((compoundButton, b) -> {

            /*CrashReporting.setState(b? Feature.State.ENABLED : Feature.State.DISABLED);
            CrashReporting.setAnrState(b? Feature.State.ENABLED : Feature.State.DISABLED);*/
            Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                    .edit().putBoolean("anr_reporting_enabled", b).apply();
        });

        logLevelSpinner.setSelection(CCUHsApi.getInstance().getCcuLogLevel());
        logLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CCUHsApi.getInstance().writeHisValByQuery(Queries.LOG_LEVEL_QUERY, (double) position);
                CCUHsApi.getInstance().setCcuLogLevel(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case where no item is selected
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        switch (arg0.getId())
        {
            case R.id.loopspinner:
                if(previousControlLoopFrequency != Integer.parseInt(loopSpinner.getSelectedItem().toString())) {
                    previousControlLoopFrequency = Integer.parseInt(loopSpinner.getSelectedItem().toString());
                    writePref("control_loop_frequency", Integer.parseInt(loopSpinner.getSelectedItem().toString()));
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle("App restart confirmation")
                            .setMessage("The updated control loop frequency will be applied after the next app restart. Do you want to continue?")
                            .setPositiveButton("Restart", (dialog1, which) -> CCUUiUtil.triggerRestart(getContext()))
                            .setNegativeButton("Cancel", (dialog1, which) -> {
                            })
                            .create();
                    dialog.show();
                }
                break;
            case R.id.outsideTemp:
                writePref("outside_temp", Integer.parseInt(outsideTemp.getSelectedItem().toString()));
                break;
    
            case R.id.outsideHumidity:
                writePref("outside_humidity", Integer.parseInt(outsideHumidity.getSelectedItem().toString()));
                break;
            case R.id.inletWaterTemp:
                writePref("inlet_waterTemp", Integer.parseInt(inletWaterTemp.getSelectedItem().toString()));
                break;
            case R.id.outletWaterTemp:
                writePref("outlet_waterTemp", Integer.parseInt(outletWaterTemp.getSelectedItem().toString()));
                break;
            case R.id.cwFlowRate:
                writePref("cw_FlowRate", Integer.parseInt(cwFlowRate.getSelectedItem().toString()));
                break;
            case R.id.cacheSyncFrequency:
                writePref("cacheSyncFrequency", Integer.parseInt(cacheSyncFrequency.getSelectedItem().toString()));
                break;
            case R.id.advancedAhuConnectPort:
                if(prevoiusAhuConnectPort != advancedAhuConnectPort.getSelectedItemPosition()) {
                    prevoiusAhuConnectPort = advancedAhuConnectPort.getSelectedItemPosition();
                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .edit()
                            .putInt("connect_serial_port", advancedAhuConnectPort.getSelectedItemPosition())
                            .commit();
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle("App restart confirmation")
                            .setMessage("This change requires an app restart. Do you want to continue?")
                            .setPositiveButton("Restart", (dialog1, which) -> CCUUiUtil.triggerRestart(getContext()))
                            .setNegativeButton("Cancel", (dialog1, which) -> {
                            })
                            .create();
                    dialog.show();
                }
                break;
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
    }
    
    private void writePref(String prefName, int val) {
        Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
               .edit().putInt(prefName, val).apply();
    }

    // Shows a simple error dialog for the given message.  (We should have a general tool for this.)
    private void showErrorDialog(String msg) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Error")
                .setIcon(R.drawable.ic_alert)
                .setMessage(msg)
                .show();
    }
    
    public static void triggerRebirth(Context context) {
        CCUHsApi.getInstance().saveTagsData(true);
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }
    
    private void configureBtuProxy(ArrayAdapter<Integer> dataAdapter) {
        btuProxyBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                   .edit().putBoolean("btu_proxy", b).apply();
            btuProxyLayout.setVisibility(b?View.VISIBLE :View.INVISIBLE);
        });
        boolean btuProxyEnabled = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting"
                        , Context.MODE_PRIVATE).getBoolean("btu_proxy", false);
        btuProxyBtn.setChecked(btuProxyEnabled);
        btuProxyLayout.setVisibility(btuProxyEnabled ? View.VISIBLE :View.INVISIBLE);
    
        inletWaterTemp.setAdapter(dataAdapter);
        inletWaterTemp.setOnItemSelectedListener(this);
        inletWaterTemp.setSelection(dataAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                                                                .getInt("inlet_waterTemp", 0)));
        outletWaterTemp.setAdapter(dataAdapter);
        outletWaterTemp.setOnItemSelectedListener(this);
        outletWaterTemp.setSelection(dataAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                                                                 .getInt("outlet_waterTemp", 0)));
        cwFlowRate.setAdapter(dataAdapter);
        cwFlowRate.setOnItemSelectedListener(this);
        cwFlowRate.setSelection(dataAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                                                    .getInt("cw_FlowRate", 0)));
    
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    private void hideKeyboard(View view){
        try {
            InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}