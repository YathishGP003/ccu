package a75f.io.renatus.ENGG;

import static a75f.io.alerts.model.AlertCauses.CCU_RESTART;
import static a75f.io.renatus.util.CCUUiUtil.UpdateAppRestartCause;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincReader;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Site;
import a75f.io.device.mesh.LSerial;
import a75f.io.domain.api.Domain;
import a75f.io.domain.logic.TunerEquipBuilder;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.filesystem.FileSystemTools;
import a75f.io.logic.logtasks.UploadLogs;
import a75f.io.logic.migration.MigrationHandler;
import a75f.io.logic.tuners.TunerEquip;
import a75f.io.messaging.client.MessagingClient;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.RebootHandlerService;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.anrwatchdog.ANRHandler;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CCUUtils;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.sanity.ui.SanityResultsFragment;
import a75f.io.usbserial.UsbSerialUtil;
import a75f.io.util.ExecutorTask;
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
    private final String TAG = "CCU_DEV_SETTINGS";
    private final String pending = "PENDING";
    private final String failed = "FAILED";
    private final String completed = "COMPLETED";
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
    @BindView(R.id.unsyncedSyncBtn)
    Button unsyncedSyncBtn;
    
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
    @BindView(R.id.access_local_assest) CheckBox accessLocalAssestFile;

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

    public @BindView(R.id.recreateBuildingPoints) Button recreateBuildingPoints;

    public @BindView(R.id.recreateBacnetId) Button recreateBacnetIds;

    public @BindView(R.id.recreateAndSyncBuildingPointsToCloud) Button recreateAndSyncBuildingPointsToCloud;

    // TODO: This is a temporary way to delete building tuners, building equip and all building entities locally for Testing.
    public @BindView(R.id.testDeleteButtonsLayout) LinearLayout testDeleteButtonsLayout;

    public @BindView(R.id.deleteBuildingTuners) Button deleteBuildingTuners;
    public @BindView(R.id.deleteBuildingEquipLocally) Button deleteBuildingEquipLocally;
    public @BindView(R.id.deleteAllBuildingEntitiesLocally) Button deleteBuildingEntitiesLocally;
    public @BindView(R.id.duplicateBuildingEntitiesLocally) Button duplicateBuildingEntitiesLocally;
    public @BindView(R.id.rebootDay) Spinner rebootDaySpinner;
    public @BindView(R.id.rebootHour) Spinner rebootHourSpinner;
    public @BindView(R.id.rebootMinute) Spinner rebootMinuteSpinner;
    public @BindView(R.id.rebootDaysCount) Spinner rebootDaysCountSpinner;
    public @BindView(R.id.rebootDaysCountText) TextView rebootDaysCountText;
    public @BindView(R.id.initiateReboot) Button btnInitiateReboot;
    public @BindView(R.id.rebootLayout1) LinearLayout rebootLayout1;
    public @BindView(R.id.rebootLayout2) LinearLayout rebootLayout2;

    public @BindView(R.id.anrReportText) TextView anrReportText;
    public @BindView(R.id.anrTriggerBtn) Button anrTriggerBtn;
    public @BindView(R.id.executeSanity) Button executeSanity;

    public @BindView(R.id.remoteId) TextView remoteEntity;
    public @BindView(R.id.remoteBtn) Button remoteBtn;
    public @BindView(R.id.scrn_timeout) EditText scrn_timeout;
    public @BindView(R.id.savescrn_timeout) Button savescrn_timeout;

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
         CCUHsApi hayStack = CCUHsApi.getInstance();

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

            alert.setPositiveButton("Ok", (dialog, whichButton) -> ExecutorTask.executeBackground( () -> {
                try {
                    fileSystemTools.writeLogCat(input.getText().toString() + ".txt");
                }
                catch (IOException | SecurityException ex) {
                    ex.printStackTrace();
                    getActivity().runOnUiThread(() -> showErrorDialog(
                            "Unable to save log file: " + ex.getMessage()));
                }
            }));
            alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
                // Canceled.
            });

            alert.show();
        });

        logUploadBtn.setOnClickListener( v -> ExecutorTask.executeBackground(() -> UploadLogs.instanceOf().saveCcuLogs()));

        pullDataBtn.setOnClickListener(view14 -> ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Pulling building tuners to CCU"),
                () -> {
                    Site site = CCUHsApi.getInstance().getSite();
                    HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                    CCUHsApi.getInstance().importBuildingSchedule(site.getId(), hClient);
                    TunerEquip.INSTANCE.syncBuildingTuners(CCUHsApi.getInstance());
                },
                ProgressDialogUtils::hideProgressDialog
        ));

        deleteHis.setOnClickListener(view15 -> {
            CcuLog.d(L.TAG_CCU, " deleteHis data ");
            if (returnDevSettingPreference("his_box_sync").equals(pending)) {
                Toast.makeText(getActivity(), "Clearing the his box history is already running ", Toast.LENGTH_SHORT).show();
                return;
            }
            updateDevSettingPreference("his_box_sync",pending);
            ExecutorTask.executeBackground(
                    () ->   CCUHsApi.getInstance().deleteHistory()
            );
            updateDevSettingPreferencesWithDateAndTime("clear_his_box_history", "clear_hix_box_data", true,"sample");
            Toast.makeText(getActivity(), "Successfully Cleared the local his box data ", Toast.LENGTH_SHORT).show();
            updateDevSettingPreference("his_box_sync", completed);
            ProgressDialogUtils.hideProgressDialog();
        });

        clearHisData.setOnClickListener(view15 -> {
                    if (returnDevSettingPreference("object_box_sync").equals(pending)) {
                        Toast.makeText(getActivity(), "Clearing the object box history is already running ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateDevSettingPreference("object_box_sync", pending);
                    ExecutorTask.executeBackground(
                            () ->   CCUHsApi.getInstance().trimObjectBoxHisStore()
                    );
                    updateDevSettingPreferencesWithDateAndTime("clear_object_box_history", "clear_object_box_data", true,"sample");
                    Toast.makeText(getActivity(), "Successfully Cleared the local object box data ", Toast.LENGTH_SHORT).show();
                    updateDevSettingPreference("object_box_sync", completed);

                }
        );


        forceSyncBtn.setOnClickListener(view112 -> {
            if (returnDevSettingPreference("sync_local_data").equals(pending)) {
                Toast.makeText(getActivity(), "syncing the local data to cloud  is already running ", Toast.LENGTH_SHORT).show();
                return;
            }
            updateDevSettingPreference("sync_local_data", pending);
            ExecutorTask.executeBackground(() -> {
                CcuLog.d(TAG, "Syncing local data to cloud");
                CCUHsApi.getInstance().resyncSiteTree();
            });
            updateDevSettingPreferencesWithDateAndTime("sync_local_data_history", "sync_local_data", true,"sample");
            Toast.makeText(getActivity(), "Successfully syncing the local data to cloud   ", Toast.LENGTH_SHORT).show();
            updateDevSettingPreference("sync_local_data", completed);
        });

        unsyncedSyncBtn.setOnClickListener(view111 -> {
            if (returnDevSettingPreference("clear_unsynced_id_list").equals(pending)) {
                Toast.makeText(getActivity(), "clearing the unsynced id list  is already running ", Toast.LENGTH_SHORT).show();
                return;
            }
            updateDevSettingPreference("clear_unsynced_id_list", pending);
            CCUHsApi.getInstance().getSyncStatusService().clearSyncStatus();
            updateDevSettingPreferencesWithDateAndTime("remove_unsync_list_history", "remove_unsync_list", true,"sample");
            updateDevSettingPreference("clear_unsynced_id_list", completed);
            Toast.makeText(getActivity(), "Successfully cleared  the unsynced id list", Toast.LENGTH_SHORT).show();
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

        boolean isLocalAccessEnabled = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(this.getContext().getString(R.string.prefs_access_local_assests),false);
        accessLocalAssestFile.setChecked(isLocalAccessEnabled);
        accessLocalAssestFile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .edit()
                    .putBoolean(getContext().getString(R.string.prefs_access_local_assests), isChecked)
                    .apply();

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
        advancedAhuConnectPort.setVisibility(View.GONE);


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

        scrn_timeout.setText(String.valueOf(spDefaultPrefs.getInt("screenTimeOut", 3600)));

        etRegisterRequestCount.setOnClickListener(view110 -> {
            etRegisterRequestCount.setCursorVisible(true);
            etRegisterRequestCount.requestFocus();
        });

        scrn_timeout.setOnClickListener(view19 -> {
            scrn_timeout.setCursorVisible(true);
            scrn_timeout.requestFocus();
        });

        savescrn_timeout.setOnClickListener(view19 -> {
            savescrn_timeout.setCursorVisible(true);
            savescrn_timeout.requestFocus();
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

        savescrn_timeout.setOnClickListener(view17 -> {
            spDefaultPrefs.edit().putInt("screenTimeOut",
                    Integer.parseInt(scrn_timeout.getText().toString())).apply();
            scrn_timeout.setCursorVisible(false);
            hideKeyboard(view17);
            Toast.makeText(getActivity(), "Saved.", Toast.LENGTH_SHORT).show();
        });

        anrReporting.setChecked(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting"
                , Context.MODE_PRIVATE).getBoolean("anr_reporting_enabled", false));
        anrReporting.setOnCheckedChangeListener((compoundButton, b) -> {
            Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                    .edit().putBoolean("anr_reporting_enabled", b).apply();
            if(!b) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle("App restart confirmation")
                        .setMessage("The ANR config change would be applied after the next app restart. Do you want to restart now?")
                        .setPositiveButton("Restart", (dialog1, which) -> CCUUiUtil.triggerRestart(getContext()))
                        .setNegativeButton("Cancel", (dialog1, which) -> {
                        })
                        .create();
                dialog.show();
            } else {
                ANRHandler.configureANRWatchdog();
            }
        });

        anrTriggerBtn.setOnClickListener(view1 -> {
            try {
                Thread.sleep(30000); // Block the main thread for 30 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        logLevelSpinner.setSelection(CCUHsApi.getInstance().getCcuLogLevel());
        logLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Domain.ccuEquip.getLogLevel().writeHisVal(position);
                CCUHsApi.getInstance().setCcuLogLevel(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case where no item is selected
            }
        });

        if ( BuildConfig.BUILD_TYPE.equals("staging")
                || BuildConfig.BUILD_TYPE.equals("qa")
                || BuildConfig.BUILD_TYPE.equals("dev_qa")) {
            rebootLayout1.setVisibility(View.VISIBLE);
            rebootLayout2.setVisibility(View.VISIBLE);
        }
        else {
            rebootLayout1.setVisibility(View.GONE);
            rebootLayout2.setVisibility(View.GONE);
        }

        rebootDaySpinner.setSelection(spDefaultPrefs.getInt("rebootDay", 2));
        rebootDaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    rebootDaysCountSpinner.setVisibility(View.VISIBLE);
                    rebootDaysCountText.setVisibility(View.VISIBLE);
                }
                else {
                    rebootDaysCountSpinner.setVisibility(View.INVISIBLE);
                    rebootDaysCountText.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case where no item is selected
            }
        });

        ArrayList<Integer> noOfDaysCount = new ArrayList<>();
        for (int val = 0;  val <= 30; val++)
        {
            noOfDaysCount.add(val);
        }

        ArrayAdapter<Integer> daysCountAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, noOfDaysCount);
        rebootDaysCountSpinner.setAdapter(daysCountAdapter);
        rebootDaysCountSpinner.setSelection(spDefaultPrefs.getInt("rebootDaysCount", 1));

        ArrayList<Integer> hoursCount = new ArrayList<>();
        for (int val = 0;  val <= 23; val++)
        {
            hoursCount.add(val);
        }
        ArrayAdapter<Integer> hoursAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, hoursCount);
        rebootHourSpinner.setAdapter(hoursAdapter);
        rebootHourSpinner.setSelection(spDefaultPrefs.getInt("rebootHour", 23));

        ArrayList<Integer> minutesCount = new ArrayList<>();
        for (int val = 0;  val <= 59; val++)
        {
            minutesCount.add(val);
        }
        ArrayAdapter<Integer> minutesAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, minutesCount);
        rebootMinuteSpinner.setAdapter(minutesAdapter);
        rebootMinuteSpinner.setSelection(spDefaultPrefs.getInt("rebootMinute", 0));

        btnInitiateReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( validateForReboot() ) {
                    spDefaultPrefs.edit().putInt("rebootMinute", rebootMinuteSpinner.getSelectedItemPosition()).apply();
                    spDefaultPrefs.edit().putInt("rebootHour", rebootHourSpinner.getSelectedItemPosition()).apply();
                    spDefaultPrefs.edit().putInt("rebootDay", rebootDaySpinner.getSelectedItemPosition()).apply();
                    spDefaultPrefs.edit().putInt("rebootDaysCount", rebootDaysCountSpinner.getSelectedItemPosition()).apply();
                    RebootHandlerService.scheduleRebootJob(getActivity(),true);
                }
            }
        });
        recreateBuildingPoints.setOnClickListener(recreateButton -> {
                String msg = "The CCU already has all the building points when compared to Model.\nContinuing with this operation will recreate the equip and all points.\nIf you want to sync the existing points with cloud, please use \"Pull Building Tuners\". ";
                executeOperationWithConfirmation(hayStack, msg, this::reconstructLocalBuildingPoints);
        });

        recreateAndSyncBuildingPointsToCloud.setOnClickListener(recreateAndSyncButton -> {
                String msg = "The CCU already has all the building points when compared to Model.\nContinuing with this operation will store the remote building equip in the DB, create new points and then attempt to sync it to cloud.\nIf you want to sync the existing points from cloud, please use \"Pull Building Tuners\". ";
                executeOperationWithConfirmation(hayStack, msg, this::validateAndStartBuildingPointsSyncFromCCUToCloud);
        });

        if(BuildConfig.BUILD_TYPE.equals("dev_qa")|| BuildConfig.BUILD_TYPE.equals("qa")|| BuildConfig.BUILD_TYPE.equals("staging")) {
            testDeleteButtonsLayout.setVisibility(View.VISIBLE);
        }

        //TODO: This is a temporary way to delete building tuners, building equip and all building entities locally for Testing.
        deleteBuildingTuners.setOnClickListener(deleteButton -> ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting building tuners"),
                () -> deleteBuildingTuners(hayStack),
                ProgressDialogUtils::hideProgressDialog
        ));


        deleteBuildingEquipLocally.setOnClickListener(deleteButton -> ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting building equip locally"),
                () -> deleteBuildingEquipLocally(hayStack),
                ProgressDialogUtils::hideProgressDialog
        ));

        deleteBuildingEntitiesLocally.setOnClickListener(deleteButton -> ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Deleting building entities locally"),
                () -> deleteBuildingEntitiesLocally(hayStack),
                ProgressDialogUtils::hideProgressDialog
        ));
        duplicateBuildingEntitiesLocally.setOnClickListener(duplicateButton -> ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(), "Duplicating building entities locally"),
                () -> new TunerEquipBuilder(hayStack).buildEquipAndPoints(hayStack.getSiteIdRef().toString()),
                ProgressDialogUtils::hideProgressDialog
        ));
        recreateBacnetIds.setOnClickListener(recreateButton -> {
            ExecutorTask.executeAsync(
                    () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Recovering BACnet Ids"),
                    () -> {
                        new MigrationHandler(hayStack).updateBacnetProperties(hayStack);
                    },
                    ProgressDialogUtils::hideProgressDialog
            );
        });
        executeSanity.setOnClickListener(view1 -> {
            new SanityResultsFragment().show(getActivity().getSupportFragmentManager(), "SanityDialog");
        });

        remoteBtn.setOnClickListener(view1 -> {
            if (returnDevSettingPreference("remote_id_fetch").equals(pending)) {
                Toast.makeText(getActivity(), "Remote entity fetching from cloud is running ", Toast.LENGTH_SHORT).show();
                remoteEntity.setText("");
                return;
            }
            String remoteId = remoteEntity.getText().toString().trim().replace("@", "");
            remoteEntity.setText("");
            if (remoteId.isEmpty() || remoteId == null || remoteId.length() == 0) {
                Toast.makeText(getActivity(), "please enter the valid ID ", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                returnDevSettingPreference("remote_id_fetch").equals(pending);
                String msg = "Are you sure to do the remote call to pull the remoteEntity for this id : "+ remoteId ;
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(" Operation Required !");
                builder.setMessage(msg);
                builder.setPositiveButton("Continue Anyway", (dialog, which) -> {
                    fetchRemoteEntity(remoteId,hayStack);
                    Toast.makeText(getActivity(), "Remote fetch successfully  ", Toast.LENGTH_LONG).show();
                    returnDevSettingPreference("remote_id_fetch").equals(completed);
                });
                builder.setNegativeButton("Abort", (dialog, which) -> {});
                builder.show();
            } catch (Exception e) {
                CcuLog.i(TAG, "Remote Entity fetch failed " + remoteEntity);
                CcuLog.i(TAG, "Exception " + e.getMessage());
                returnDevSettingPreference("remote_id_fetch").equals(failed);
                Toast.makeText(getActivity(), "Remote fetch failed ", Toast.LENGTH_LONG).show();
            }
        });
    }
    private boolean validateForReboot() {
        Calendar calendar = Calendar.getInstance();

        int currentHour = calendar.get(Calendar.HOUR_OF_DAY); // 24-hour format
        int currentMinute = calendar.get(Calendar.MINUTE);
        if (currentHour >= rebootHourSpinner.getSelectedItemPosition()
                && currentMinute >= rebootMinuteSpinner.getSelectedItemPosition()
                && rebootDaysCountSpinner.getSelectedItemPosition() == 0
                && rebootDaySpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Reboot time is same or lesser than current time. Please select a different time.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean doesCcuLackBuildingPoints(CCUHsApi hayStack) {
        int modelBuildingPoints = TunerEquip.INSTANCE.fetchTotalBuildingPointsFromModel();
        int ccuBuildingPoints = hayStack.readAllEntities("point and default and (tuner or schedulable) and not ccuRef").size();
        boolean isBuildingPointsMissing = modelBuildingPoints > ccuBuildingPoints;
        CcuLog.d(TAG,"Total Building Points in Model: " + modelBuildingPoints);
        CcuLog.d(TAG,"Total Building Points in CCU: " + ccuBuildingPoints);
        CcuLog.d(TAG,"Building Points Missing: " + isBuildingPointsMissing);
        return isBuildingPointsMissing;
    }

    private void executeOperationWithConfirmation(CCUHsApi hayStack, String msg, Consumer<CCUHsApi> operation) {
        if(doesCcuLackBuildingPoints(hayStack) || hayStack.readId("building and equip")==null) {
            operation.accept(hayStack);
        } else {
            showConfirmationDialog(hayStack, msg, operation);
        }
    }

    private void showConfirmationDialog(CCUHsApi hayStack, String msg, Consumer<CCUHsApi> operation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Operation Not Required");
        builder.setMessage(msg);
        builder.setPositiveButton("Continue Anyway", (dialog, which) -> operation.accept(hayStack));
        builder.setNegativeButton("Abort", (dialog, which) -> {});
        builder.show();
    }

    private void reconstructLocalBuildingPoints(CCUHsApi hayStack) {
        ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Recreating building points"),
                () -> {
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date(Calendar.getInstance().getTimeInMillis()));
                    boolean recreateBuildingPointsOperationSuccess = false;
                    reconstructBuildingEquipAndPointsLocally(hayStack, false);
                    if(!doesCcuLackBuildingPoints(hayStack) && hayStack.readId("building and equip")!=null) {
                        CcuLog.d(TAG,"Building entities were recreated successfully");
                        recreateBuildingPointsOperationSuccess = true;
                    }
                    updateDevSettingPreference("btn1_recreate_building_points_locally", (recreateBuildingPointsOperationSuccess?"success_":"failed_")+formattedDate);
                },
                ProgressDialogUtils::hideProgressDialog
        );
    }

    private void reconstructBuildingEquipAndPointsLocally(CCUHsApi hayStack, boolean useRemoteEquip) {
        CcuLog.d(TAG, "Reconstructing building equip and points");
        deleteBuildingEntitiesLocally(hayStack);
        TunerEquip.INSTANCE.initialize(hayStack, useRemoteEquip);
    }

    private void validateAndStartBuildingPointsSyncFromCCUToCloud(CCUHsApi hayStack) {
        ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Validating and syncing building tuners to cloud"),
                () -> {
                   String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date(Calendar.getInstance().getTimeInMillis()));
                   boolean recreateAndSyncBuildingPointsToCloudOperationSuccess = false;
                    try {
                        HashMap<Object, Object> remoteEquip = hayStack.getRemoteBuildingTunerEquip(hayStack.getSiteIdRef().toString());
                        if(remoteEquip ==null || remoteEquip.isEmpty()) {
                            CcuLog.w(TAG, "Building equip not found in the cloud. Aborting building points sync from CCU to Cloud");
                            requireActivity().runOnUiThread(() ->
                                    showErrorDialog("Building equip not found in the cloud. Cannot continue with the operation."));
                        } else if(!Objects.requireNonNull(remoteEquip.get("sourceModelVersion")).toString().equals(TunerEquip.INSTANCE.fetchBuildingEquipSourceModeVersion())) {
                            CcuLog.w(TAG, "CCU's Source Model Version does not match with the remote building equip. Aborting building points sync from CCU to Cloud");
                            requireActivity().runOnUiThread(() ->
                                    showErrorDialog("Aborting operation since this CCU does not contain the latest building equip version. Please update the CCU to the latest version first."));
                        } else {
                            reconstructBuildingEquipAndPointsLocally(hayStack, true);
                            CcuLog.i(TAG,"isCcuMissingBuildingPoints = "+ doesCcuLackBuildingPoints(hayStack));
                            CcuLog.d(TAG,"Starting building points creation and sync from CCU to Cloud.");
                            boolean syncIssueNotFound = executeBuildingPointsSyncFromCCUToCloud(hayStack);
                            if(!doesCcuLackBuildingPoints(hayStack) && hayStack.readId("building and equip")!=null && syncIssueNotFound) {
                                CcuLog.d(TAG,"All points have successfully recreated and added to unsyncedIdList for sync to cloud.");
                                recreateAndSyncBuildingPointsToCloudOperationSuccess = true;
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        CcuLog.e(TAG, "Building equip not found in the cloud. Aborting building points sync from CCU to Cloud");
                        recreateAndSyncBuildingPointsToCloudOperationSuccess = false;
                        requireActivity().runOnUiThread(() -> showErrorDialog("Building equip not found in the cloud. Cannot continue with the operation."));
                    } finally {
                        updateDevSettingPreference("btn2_recreate_and_sync_building_points_to_cloud", (recreateAndSyncBuildingPointsToCloudOperationSuccess?"success_":"failed_")+formattedDate);
                    }
                },
                ProgressDialogUtils::hideProgressDialog
        );
    }

    private boolean executeBuildingPointsSyncFromCCUToCloud(CCUHsApi hayStack) {
        String buildingEquipId = hayStack.readId("building and equip");
        String remotePointsData = hayStack.fetchRemoteEntityByQuery("point and equipRef=="+buildingEquipId+" and siteRef=="+hayStack.getSiteIdRef().toString());
        if(remotePointsData == null) {
            CcuLog.w(TAG, "Remote points data is null. Aborting building points sync from CCU to Cloud");
            requireActivity().runOnUiThread(() ->
                    showErrorDialog("Points have been created locally but failed to fetch remote points data. Cannot continue with the operation. Please try again")
            );
            return false;
        }
        CcuLog.v(TAG,"Syncing missing points to cloud");
        for(HashMap<Object,Object> point: hayStack.readAllEntities("point and equipRef==\""+buildingEquipId+"\"")) {
            if(!remotePointsData.contains(Objects.requireNonNull(point.get("domainName")).toString())) {
                CcuLog.i(TAG,"Attempting to sync this points since it is not found on cloud: "+point);
                hayStack.syncEntity(Objects.requireNonNull(point.get("id")).toString());
            }
        }
        return true;
    }

    private void deleteBuildingTuners(CCUHsApi hayStack) {
        for(HashMap<Object, Object> point: hayStack.readAllEntities("point and default and tuner and not ccuRef")) {
            hayStack.deleteWritablePoint(Objects.requireNonNull(point.get("id")).toString());
        }
    }

    private void deleteBuildingEquipLocally(CCUHsApi hayStack) {
        hayStack.deleteEntityLocally(hayStack.readId("building and equip"));
    }

    private void deleteBuildingEntitiesLocally(CCUHsApi hayStack) {
        for(HashMap<Object, Object> entity: hayStack.readAllEntities("(building and equip) or (point and default and (tuner or schedulable) and not ccuRef)")) {
            if(entity.containsKey("writable")) {
                hayStack.deleteWritablePointLocally(Objects.requireNonNull(entity.get("id")).toString());
            } else {
                hayStack.deleteEntityLocally(Objects.requireNonNull(entity.get("id")).toString());
            }
        }
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
                   /* PreferenceManager.getDefaultSharedPreferences(getActivity())
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
                    dialog.show();*/
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
        UpdateAppRestartCause(CCU_RESTART);
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

    private void updateDevSettingPreference(String key, String value) {
        CcuLog.d(TAG,"Updating dev setting preference: "+key+" with value: "+value);
        Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .edit().putString(key, value).apply();
    }
    private String returnDevSettingPreference(String key) {
        CcuLog.d(TAG,"Updating dev setting preference: "+key);
       return Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE).getString(key, completed);
    }
    private void updateDevSettingPreferencesWithDateAndTime(String historyKey, String buttonKey, boolean value,String id ) {

        String inputValue;
        if (!Objects.equals(id, "sample")) {
            inputValue = id;
        } else {
            inputValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
                    .format(new Date());
        }
        SharedPreferences prefs = Globals.getInstance().getApplicationContext()
                .getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE);
        String history = prefs.getString(historyKey, "");
        List<String> entries = new ArrayList<>(Arrays.asList(history.split(",")));
        if (history.isEmpty()) {
            entries.clear();
        }
        entries.add(inputValue);
        if (entries.size() > 10) {
            entries = entries.subList(entries.size() - 10, entries.size());
        }
        String updatedHistory = TextUtils.join(",", entries);
        prefs.edit()
                .putBoolean(buttonKey, value)
                .putString(historyKey, updatedHistory)
                .apply();
    }

    private void fetchRemoteEntity (String Id,CCUHsApi ccuHsApi){
        ExecutorTask.executeBackground(() -> {
            String response = ccuHsApi.fetchRemoteEntity(Id);
            if (response != null) {
                HZincReader hZincReader = new HZincReader(response);
                Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
                while (hZincReaderIterator.hasNext()) {
                    HRow row = (HRow) hZincReaderIterator.next();
                    ccuHsApi.tagsDb.addHDict((row.get("id").toString()).replace("@", ""), row);
                    CcuLog.i(TAG, "Remote Entity fetch" + row);
                    if (row.get("writable") != null) {
                        HDict PointDict = new HDictBuilder().add("id", HRef.copy(row.get("id").toString())).toDict();
                        List<HDict> hDicts = new ArrayList<>();
                        hDicts.add(PointDict);
                        ccuHsApi.importPointArrays(hDicts);
                        CcuLog.i(TAG, "Remote Point Array  fetch" + row.get("id"));
                    }
                    updateDevSettingPreferencesWithDateAndTime("remote_id_fetch_history", "remote_fetch_ids", true, Id);
                    updateDevSettingPreferencesWithDateAndTime("remote_id_fetch_history_date", "remote_fetch_date", true, "sample");

                }
            }
        });
    }


}