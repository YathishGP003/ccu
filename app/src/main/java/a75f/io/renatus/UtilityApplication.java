package a75f.io.renatus;

import static a75f.io.alerts.AlertsConstantsKt.DEVICE_RESTART;
import static a75f.io.alerts.model.AlertCauses.CCU_CRASH;
import static a75f.io.api.haystack.Constants.TWENTY_FOUR_HOURS;
import static a75f.io.logic.util.PreferenceUtil.getDataSyncProcessing;
import static a75f.io.logic.util.PreferenceUtil.getSyncStartTime;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.ETHERNET;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_MSTP_INITIALIZED;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NETWORK_INTERFACE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.WIFI;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.reInitialiseBacnetStack;
import static a75f.io.renatus.util.CCUUiUtil.UpdateAppRestartCause;

import static a75f.io.logic.util.bacnet.BacnetUtilKt.cancelScheduleJobToResubscribeBacnetMstpCOV;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.scheduleJobToResubscribeBacnetMstpCOV;
import static a75f.io.util.UtilKt.triggerRebirth;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.raygun.raygun4android.RaygunClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.bacnet.parser.BacnetRequestProcessor;
import a75f.io.api.haystack.util.DatabaseAction;
import a75f.io.api.haystack.util.DatabaseEvent;
import a75f.io.device.DeviceUpdateJob;
import a75f.io.device.EveryDaySchedulerService;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.RootCommandExecuter;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.BacnetServicesUtils;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.cloud.RenatusServicesUrls;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.logic.util.bacnet.BacnetUtility;
import a75f.io.logic.watchdog.Watchdog;
import a75f.io.messaging.MessageHandlerSubscriber;
import a75f.io.messaging.client.MessagingClient;
import a75f.io.messaging.handler.DashboardHandler;
import a75f.io.messaging.handler.DashboardHandlerKt;
import a75f.io.messaging.handler.DataSyncHandler;
import a75f.io.messaging.handler.ProfileConfigurationHandler;
import a75f.io.messaging.service.MessageCleanUpWork;
import a75f.io.messaging.service.MessageRetryHandlerWork;
import a75f.io.messaging.service.MessagingAckJob;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.ota.OtaCache;
import a75f.io.renatus.ota.SeqCache;
import a75f.io.renatus.registration.UpdateCCUFragment;
import a75f.io.renatus.schedules.FileBackupService;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.views.RebootDataCache;
import a75f.io.restserver.server.HttpServer;
import a75f.io.sanity.framework.SanityManager;
import a75f.io.sitesequencer.SequenceManager;
import a75f.io.sitesequencer.SequencerSchedulerUtil;
import a75f.io.usbserial.SerialEvent;
import a75f.io.util.DashboardListener;
import a75f.io.util.DashboardUtilKt;

/**
 * Created by rmatt isOn 7/19/2017.
 */

public abstract class UtilityApplication extends Application implements Globals.LandingActivityListener {

    public static WifiManager wifiManager;
    public static Context context = null;

    private static MessagingAckJob messagingAckJob = null;
    private static final int TASK_SEPARATION = 15;
    private static final TimeUnit TASK_SEPARATION_TIMEUNIT = TimeUnit.SECONDS;
    private static final int MESSAGING_ACK_INTERVAL = 30;

    private static final String INIT_RC_FILE = "init-75f.rc";
    private static final String INIT_RC_FILE_PERMISSIONS = "rw-r--r--";
    private static final String INIT_SCRIPT_FILE = "75f-init.sh";
    private static final String INIT_SCRIPT_FILE_PERMISSIONS = "rwxr-xr-x";
    private static final String MOUNT_COMMAND = "mount -o %s,remount /system";
    private static final String INIT_BIN_DIR = "/system/bin/";
    private static final String INIT_RC_DIR = "/system/etc/init/";
    private final DashboardListener dashboardListener = isDashboardConfigured -> {
        CcuLog.i("DASHBOARD", "dashboardListener : "+isDashboardConfigured);
        if (isDashboardConfigured) {
            if (!HttpServer.Companion.getInstance(context).isServerRunning()) {
                startRestServer();
            }
        } else {
            if (!isBACnetIntialized() && !isBacnetMstpInitialized()) {
                stopRestServer();
            }
        }
    };

    public static boolean isRoomDbReady = false;
    @Inject
    MessageHandlerSubscriber messageHandlerSubscriber;

    private DeviceUpdateJob deviceUpdateJob;
    private static Prefs prefs;
    public static BackgroundServiceInitiator backgroundServiceInitiator;
    private static ConnectivityManager.NetworkCallback ethernetCallback;
    private static ConnectivityManager.NetworkCallback wifiCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        setCcuDbReady(false);
        CcuLog.i("UI_PROFILING", "UtilityApplication.onCreate");
    
        CcuLog.e(L.TAG_CCU, "LifeCycleEvent App Started");
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        context = getApplicationContext();
        prefs = new Prefs(context);

        // initialize crash reports as early as possible
        initializeCrashReporting();
        EventBus.getDefault().register(this);
        Globals.getInstance().setApplicationContext(this);
        backgroundServiceInitiator = new BackgroundServiceInitiator(this);

    }

    public void setCcuDbReady(boolean isCcuDbReady) {
        isRoomDbReady = isCcuDbReady;
    }

    private void postProcessingInit(){
        CcuLog.i("CCU_DB", "postProcessingInit - start");

        //Remove this Equip Manager once all modbus models are migrated from Domain modeler
        EquipsManager.getInstance(this).setApplicationContext(this);
        Globals.getInstance().startTimerTask();
        RenatusServicesUrls renatusServicesUrls = RenatusServicesEnvironment.getInstance().getUrls();
        SequenceManager.getInstance(context, renatusServicesUrls.getSequencerUrl(), ProfileConfigurationHandler.INSTANCE)
                .fetchPredefinedSequencesIfEmpty();
        isDataSyncRestartRequired();
        UpdateCCUFragment.abortCCUDownloadProcess();

        RaygunClient.setUser(userNameForCrashReportsFromHaystack());

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        deviceUpdateJob = new DeviceUpdateJob();
        deviceUpdateJob.scheduleJob("DeviceUpdateJob",Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getInt("control_loop_frequency",60), 15, TimeUnit.SECONDS);
        Watchdog.getInstance().addMonitor(deviceUpdateJob);

        FileBackupService.scheduleFileBackupServiceJob(context);
        EveryDaySchedulerService.scheduleJobForDay(context);
        if (PreferenceUtil.getIsCcuRebootStarted()) {
            RebootDataCache rebootDataCache = new RebootDataCache();
            rebootDataCache.storeRebootTimestamp(false);
            PreferenceUtil.setIsCcuRebootStarted(false);
        }
        if (BuildConfig.BUILD_TYPE == "prod" || BuildConfig.BUILD_TYPE.equals("staging") || BuildConfig.BUILD_TYPE.equals("qa") || BuildConfig.BUILD_TYPE.equals("dev_qa")) {
            RebootHandlerService.scheduleRebootJob(context , false);
            CcuLog.i(L.TAG_CCU, "Reboot service started for build type: " + BuildConfig.BUILD_TYPE);
        } else {
            CcuLog.i(L.TAG_CCU, "Reboot service not started for build type: " + BuildConfig.BUILD_TYPE);
        }
        verifyAndroidInitScripts();
        SeqCache seqCache = new SeqCache();
        seqCache.restoreSeqRequests(context);
        OtaCache cache = new OtaCache();
        cache.restoreOtaRequests(context);
        CCUUtils.setCCUReadyProperty("false");
        registerEthernetListener();
        registerWifiListener();
        checkAndServerStatus();
        DashboardHandler.Companion.setDashboardListener(dashboardListener);
        CcuLog.i("UI_PROFILING", "UtilityApplication.onCreate Done");
        CcuLog.i("CCU_DB", "postProcessingInit - end");
        scheduleAlertCleanUpJob();
        Globals.getInstance().registerLandingActivityListener(this);

        // Initialize the BacnetServicesUtils callback implementation
        CcuLog.i(L.TAG_CCU_INIT, "Initializing BacnetServicesUtils callback implementation");
        BacnetServicesUtils callbackImpl = new BacnetServicesUtils();
        BacnetRequestProcessor.setCallback(callbackImpl);

        //Launch BacApp if BACnet is initialized
        if (isBacnetMstpInitialized()) {
            CcuLog.i(L.TAG_CCU_INIT, "Bacnet MSTP is initialized, scheduling job to resubscribe COV");
            scheduleJobToResubscribeBacnetMstpCOV();
        } else {
            CcuLog.i(L.TAG_CCU_INIT, "Bacnet MSTP is not initialized, skipping resubscribe job");
            cancelScheduleJobToResubscribeBacnetMstpCOV("Bacnet MSTP not initialized");
        }
        BacnetUtility.INSTANCE.checkAndScheduleJobForBacnetClient();

        new SanityManager().scheduleAllSanityPeriodic(getApplicationContext(), TWENTY_FOUR_HOURS);
    }

    private void scheduleAlertCleanUpJob() {
        RenatusServicesUrls renatusServicesUrls = RenatusServicesEnvironment.getInstance().getUrls();
        SequenceManager.getInstance(this, renatusServicesUrls.getSequencerUrl(), ProfileConfigurationHandler.INSTANCE)
                .fetchPredefinedSequencesIfEmpty();
        SequencerSchedulerUtil.Companion.scheduleDailyCleanupTask(this);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onDatabaseLoad(DatabaseEvent event) {
        CcuLog.i("CCU_DB", "Event Type:@ " + event.getSerialAction().name());
        if (event.getSerialAction() == DatabaseAction.MESSAGE_DATABASE_LOADED_SUCCESS_INIT_UI) {
            postProcessingInit();
            CcuLog.i("CCU_DB", "post processing done- launch ui now");
            setCcuDbReady(true);
            Globals.getInstance().checkBacnetSystemProfileStatus();
        }
    }

    public static String readTextFileFromAssets(Context context, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(fileName);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    /**
     * Execute a system command and return the status and results.
     * NOTE:  this method will block for IO.  Expect it to be slow and don't call on main.
     *
     * @throws IOException
     * @throws SecurityException
     */
    protected AbstractMap.SimpleEntry<Integer,String> executeSystemCommand(String command) throws IOException, SecurityException{
        CcuLog.d(L.TAG_CCU_INIT, "Executing system command: " + command);
        Process process = Runtime.getRuntime().exec("su");
        StringBuilder output = new StringBuilder();

        InputStream stdout = process.getInputStream();
        InputStream es = process.getErrorStream();
        DataOutputStream commandInput = new DataOutputStream(process.getOutputStream());
        commandInput.writeBytes(command + "\n");
        commandInput.writeBytes("exit $?\n");
        commandInput.flush();
        commandInput.close();

        int read;
        byte[] buffer = new byte[4096];

        // Read stdout
        while ((read = stdout.read(buffer)) > 0) {
            output.append(new String(buffer, 0, read));
        }

        if ( es.available() > 0) {
            output.append("\n---Error output---\n");
            // Read stderr
            while ((read = es.read(buffer)) > 0) {
                output.append(new String(buffer, 0, read));
            }
        }

        try {
            process.waitFor();
        } catch(InterruptedException e) {
            CcuLog.e(L.TAG_CCU_INIT, "...process interrupted for command " + command + ", " + e.getMessage());
        }

        CcuLog.d(L.TAG_CCU_INIT, "...command exited with status " + process.exitValue());
        return new AbstractMap.SimpleEntry<>(process.exitValue(), output.toString());
    }

    private boolean scriptsAndPermissionsConfiguredCorrectly(String version) {
        boolean installationCorrect = true;
        // Check existence and permissions on the shell script file
        try {
            AbstractMap.SimpleEntry<Integer, String> execResults = executeSystemCommand("ls -l " + INIT_BIN_DIR + INIT_SCRIPT_FILE);
            // 'ls' will return 1 if the file is not found
            int status = execResults.getKey();
            switch(status) {
                case 0: // Success
                    if (!execResults.getValue().contains(INIT_SCRIPT_FILE_PERMISSIONS)) {
                        CcuLog.i(L.TAG_CCU_INIT, INIT_SCRIPT_FILE + " exists but permissions are incorrect.  Assuming incomplete installation.");
                        installationCorrect = false;
                    } else {
                        CcuLog.i(L.TAG_CCU_INIT, INIT_SCRIPT_FILE + " exists and permissions are correct, verifying script version: " + version);
                        String versionCheckCommand = "grep '" + version + "' " + INIT_BIN_DIR + INIT_SCRIPT_FILE;
                        execResults = executeSystemCommand(versionCheckCommand);
                        if (execResults.getKey() != 0) {
                            CcuLog.i(L.TAG_CCU_INIT, INIT_SCRIPT_FILE + " version is outdated, reinstalling.");
                            installationCorrect = false;
                        }
                    }
                    break;
                case 1: // Not found
                    installationCorrect = false;
                    break;
                default: // Some other error
                    CcuLog.e(L.TAG_CCU_INIT, "Error checking " + INIT_SCRIPT_FILE + " installation, return code " + status);
                    installationCorrect = false;
                    break;
            }
        } catch(Exception e) {
            CcuLog.e(L.TAG_CCU_INIT, "Error checking " + INIT_SCRIPT_FILE + " installation: " + e.getMessage());
            installationCorrect = false;
        }

        // Check existence and permissions on the RC file
        try {
            AbstractMap.SimpleEntry<Integer, String> execResults = executeSystemCommand("ls -l " + INIT_RC_DIR + INIT_RC_FILE);
            // 'ls' will return 1 if the file is not found
            int status = execResults.getKey();
            switch(status) {
                case 0: // Success
                    if (!execResults.getValue().contains(INIT_RC_FILE_PERMISSIONS)) {
                        CcuLog.i(L.TAG_CCU_INIT, INIT_RC_FILE + " exists but permissions are incorrect.  Assuming incomplete installation.");
                        installationCorrect = false;
                    } else {
                        CcuLog.i(L.TAG_CCU_INIT, INIT_RC_FILE + " exists and permissions are correct.");
                    }
                    break;
                case 1: // Not found
                    installationCorrect = false;
                    break;
                default: // Some other error
                    CcuLog.e(L.TAG_CCU_INIT, "Error checking " + INIT_RC_FILE + " installation, return code " + status);
                    installationCorrect = false;
                    break;
            }
        } catch(Exception e) {
            CcuLog.e(L.TAG_CCU_INIT, "Error checking " + INIT_RC_FILE + " installation: " + e.getMessage());
            // We return false here to attempt a reinstallation
            installationCorrect = false;
        }

        return installationCorrect;
    }

    private boolean installAndConfigureScripts(String sRCContent, String sScriptContent) {
        String sSourcePathToRC = RenatusApp.getAppContext().getExternalFilesDir(null).getPath()+"/"+INIT_RC_FILE;
        String sSourcePathToInitScript = RenatusApp.getAppContext().getExternalFilesDir(null).getPath()+"/"+INIT_SCRIPT_FILE;
        String sDestPathToRC = "/system/etc/init/" + INIT_RC_FILE;
        String sDestPathToInitScript = "/system/bin/" + INIT_SCRIPT_FILE;

        CcuLog.i(L.TAG_CCU_INIT, "Installing init scripts");

        // Write the files to our external storage
        try {
            FileWriter rcWriter = new FileWriter(sSourcePathToRC);
            rcWriter.write(sRCContent);
            rcWriter.close();

            FileWriter scriptWriter = new FileWriter(sSourcePathToInitScript);
            scriptWriter.write(sScriptContent);
            scriptWriter.close();
        } catch(IOException e) {
            // This IS A critical failure
            CcuLog.e(L.TAG_CCU_INIT, "Error writing script files to external storage: " + e.getMessage());
            return false;
        }

        String[] commands = new String[]{
                String.format(MOUNT_COMMAND, "rw"),                             // Make /system as read-write
                "mv " + sSourcePathToInitScript + " " + sDestPathToInitScript,  // Move our shell script in place and set permissions/ownership
                "chmod 755 " + sDestPathToInitScript,
                "chown root.shell " + sDestPathToInitScript,
                "mv " + sSourcePathToRC + " " + sDestPathToRC,                  // Move our RC script in place and set permissions/ownership
                "chmod 644 " + sDestPathToRC,
                "chown root.root " + sDestPathToRC,
                String.format(MOUNT_COMMAND, "ro",                              // Remount /system as read-only
                        "ls -l /system/bin/75f-init.sh /system/etc/init/init-75f.rc")   // List the new files just because
        };

        RenatusApp.executeAsRoot(commands, null, false, false);

        return true;
    }

    private void verifyAndroidInitScripts() {
        CcuLog.i(L.TAG_CCU_INIT, "Verifying android init script installation");

        String sRCContent = readTextFileFromAssets(context, "init/" + INIT_RC_FILE);
        String sScriptContent = readTextFileFromAssets(context, "init/" + INIT_SCRIPT_FILE);

        // Extract version from RC script file
        // We use the version to determine if we need to install the RC init files onto the tablet
        String currentVersion = "<none>";
        Pattern pattern = Pattern.compile("(Version=\"[^\"]*\")", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sScriptContent);
        boolean matchFound = matcher.find();
        if (matchFound) {
            currentVersion = matcher.group(0);
        } else {
            CcuLog.w(L.TAG_CCU_INIT, "Unable to extract version from init script file, will install by default");
        }

        CcuLog.w(L.TAG_CCU_INIT, "Current init script file version is " + currentVersion);

        // If scripts are already installed, nothing to do here
        if (scriptsAndPermissionsConfiguredCorrectly(currentVersion)) {
            return;
        }

        CcuLog.i(L.TAG_CCU_INIT, "Android init scripts REQUIRE installation");
        installAndConfigureScripts(sRCContent, sScriptContent);
    }

    private void isDataSyncRestartRequired() {
        if(getDataSyncProcessing()) {
            CcuLog.i("CCU_READ_CHANGES", "Data Sync restarted " + new Date(getSyncStartTime()));
            DataSyncHandler dataSyncHandler = new DataSyncHandler();
            dataSyncHandler.syncCCUData(getSyncStartTime());
        }
    }
    private void initializeCrashReporting() {
        CcuLog.i("UI_PROFILING", "UtilityApplication.initializeCrashReporting");

        RaygunClient.init(this);
        RaygunClient.setVersion(versionName());
        RaygunClient.enableCrashReporting();

        if (BuildConfig.BUILD_TYPE.equals("staging") ||
                BuildConfig.BUILD_TYPE.equals("prod") ||
                BuildConfig.BUILD_TYPE.equals("daikin_prod") ||
                BuildConfig.BUILD_TYPE.equals("qa") ||
                BuildConfig.BUILD_TYPE.equals("carrier_prod") ||
                BuildConfig.BUILD_TYPE.equals("airoverse_prod")) {
            Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
                try {
                    CcuLog.e(L.TAG_CCU, "LifeCycleEvent App Crash");
                    paramThrowable.printStackTrace();
                    handleSafeMode(paramThrowable);
                    RaygunClient.send(paramThrowable);
                    RenatusApp.closeApp();
                } catch (Exception e) {
                    //An exception here could lead to ANR, so catch it and let the app get relaunched.
                    e.printStackTrace();
                    CcuLog.e(L.TAG_CCU, "Exception while handling safe mode");
                    triggerRebirth(RenatusApp.getAppContext());
                }
            });
        }
        CcuLog.i("UI_PROFILING", "UtilityApplication.initializeCrashReporting Done");
    
    }

    private void handleSafeMode(Throwable paramThrowable) {
        CcuLog.e(L.TAG_CCU, "RenatusLifeCycleEvent handleSafeMode");
        StringWriter sw = new StringWriter();
        paramThrowable.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        SharedPreferences crashPreference = this.getSharedPreferences("crash_preference", Context.MODE_PRIVATE);
        updateCrashStackTrace(crashPreference,stackTrace);

        String crashMessage = getCrashMessage();
        AlertManager.getInstance().fixPreviousCrashAlert();
        crashPreference.edit().putString("app_restart_cause",CCU_CRASH).commit();
        AlertManager.getInstance().generateCrashAlert(
                "CCU CRASH",
                crashMessage);


        crashPreference.edit().putString("crash_message", crashMessage).commit();

        List<String> crashTimesWithinLastHour = getCrashTimestampsWithinLastHour();

        //add the new crash to the list
        crashTimesWithinLastHour.add(String.valueOf(System.currentTimeMillis()));
        Set<String> timeSet = new HashSet<>(crashTimesWithinLastHour);
        crashPreference.edit().putStringSet("crash", timeSet).commit();

        if (crashPreference.getStringSet("crash", null).size() >= 3 ) {
            if (Domain.diagEquip != null) {
                Domain.diagEquip.getSafeModeStatus().writeHisVal(1.0);
            } else {
                CCUHsApi.getInstance().writeHisValByQuery("domainName == \"safeModeStatus\"", 1.0);
            }
        } else if (OOMExceptionHandler.isOOMCausedByFragmentation(paramThrowable)) {
            UpdateAppRestartCause(DEVICE_RESTART);
            RenatusApp.rebootTablet();
        }
    }
    private List<String> getCrashTimestampsWithinLastHour() {
        List<String> timeList = new ArrayList<>();
        SharedPreferences crashPreference = this.getSharedPreferences("crash_preference", Context.MODE_PRIVATE);
        if (crashPreference != null) {
            Set<String> timeSetFromPreference = crashPreference.getStringSet("crash", null);
            if (timeSetFromPreference != null) {
                for (String time : timeSetFromPreference) {
                    long timeLong = Long.parseLong(time);
                    if ((timeLong < System.currentTimeMillis() &&
                            timeLong > (System.currentTimeMillis() - 3600000)))
                        timeList.add(time);
                }
            }
        }
        return timeList;
    }

    private void updateCrashStackTrace(SharedPreferences crashPreference, String stackTrace){
        if(crashPreference.getStringSet("crash", null) != null ){
            int size = crashPreference.getStringSet("crash", null).size() + 1;
            crashPreference.edit().putString("crash"+size, stackTrace).commit();

        } else {
            crashPreference.edit().putString("crash1", stackTrace).commit();
        }
    }

    private String getCrashMessage(){
        String message;
        List<String> crashTimeList = getCrashTimestampsWithinLastHour();

        if(crashTimeList.size() == 0)
            message = "CCU crashed for the first time";
        else {
            long lastCrashTime = System.currentTimeMillis() - Long.parseLong(crashTimeList.get(crashTimeList.size()-1));
            long timeInMins = ((lastCrashTime) / (1000 * 60)) % 60;
            int count = crashTimeList.size() + 1;
            message = "CCU crashed "+ count +" times in last " + String.valueOf(timeInMins) + " minutes";
        }

        return message;
    }

    private String versionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo("a75f.io.renatus", 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "Version-name-not-found";
    }

    private String userNameForCrashReportsFromHaystack() {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        HashMap site = ccuHsApi.read("site");
        if (site.size() > 0) {
            String siteName = site.get("dis").toString();
            HashMap ccu = ccuHsApi.read("device and ccu");
            if (ccu.size() > 0) {
                String ccuName = ccu.get("dis").toString();
                return siteName + ": " + ccuName;
            } else {
                return siteName + ": " + "no-ccu-name";
            }
        } else {
            return "Unregistered";
        }
    }


    @Override
    public void onTerminate() {
        EventBus.getDefault().unregister(this);
        CcuLog.e(L.TAG_CCU, "LifeCycleEvent App Terminated");
        UtilityApplication.stopRestServer();
        super.onTerminate();
    }


    // Called in a separate thread
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSerialEvent(SerialEvent event) {
        if (CCUHsApi.getInstance().isCcuReady() && !Globals.getInstance().isRecoveryMode() &&
                !Globals.getInstance().isSafeMode() && isRoomDbReady()) {
            LSerial.handleSerialEvent(this, event);
        }
    }

    private boolean isRoomDbReady() {
        return this.isRoomDbReady;
    }

    public static boolean CheckWifi() {
        ConnectivityManager connManager = (ConnectivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return (networkInfo != null && networkInfo.isConnected());
    }

    public void initMessaging() {
        if (CCUHsApi.getInstance().getSite() != null) {
            if (CCUHsApi.getInstance().siteSynced()) {
                MessagingClient.getInstance().init();
            }
        }
        scheduleMessagingAckJob();
        MessageRetryHandlerWork.Companion.scheduleMessageRetryWork(context);
        MessageCleanUpWork.Companion.scheduleMessageCleanUpWork(context);
        messageHandlerSubscriber.subscribeAllHandlers();
    }

    public static void scheduleMessagingAckJob() {
        if (CCUHsApi.getInstance().isCCURegistered() && messagingAckJob == null) {
            String ccuId = CCUHsApi.getInstance().getCcuId().substring(1);
            String messagingUrl = RenatusServicesEnvironment.instance.getUrls().getMessagingUrl();
            messagingAckJob = new MessagingAckJob(ccuId, messagingUrl);
            Globals.getInstance().getScheduledThreadPool().scheduleWithFixedDelay(messagingAckJob.getJobRunnable(), TASK_SEPARATION + 30, MESSAGING_ACK_INTERVAL, TASK_SEPARATION_TIMEUNIT);
        }
    }

    public static MessagingAckJob getMessagingAckJob() {
        return messagingAckJob;
    }

    public static boolean isBACnetIntialized() { return prefs.getBoolean(IS_BACNET_INITIALIZED); }

    public static boolean isBacnetMstpInitialized() {
        return prefs.getBoolean(IS_BACNET_MSTP_INITIALIZED);
    }

    public static void stopRestServer() {
        HttpServer.Companion.getInstance(context).stopServer();
    }

    public static void startRestServer() {
        HttpServer.Companion.getInstance(context).startServer();
    }
    private void registerEthernetListener() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest ethernetRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build();
        ethernetCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                CcuLog.d(L.TAG_CCU, "Ethernet network connected");
                initNetworkConfig();
                updateBacnetIpAddressIfNetworkChanged(ETHERNET);
            }
            @Override
            public void onLost(Network network) {
                CcuLog.d(L.TAG_CCU, "Ethernet network lost");
            }
        };
        connectivityManager.registerNetworkCallback(ethernetRequest, ethernetCallback);
    }
    public static void unRegisterEthernetListener() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (ethernetCallback != null) {
                connectivityManager.unregisterNetworkCallback(ethernetCallback);
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU, "Error unregistering ethernet listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerWifiListener() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest ethernetRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        wifiCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                CcuLog.d(L.TAG_CCU, "Wifi network connected");
                updateBacnetIpAddressIfNetworkChanged(WIFI);
            }
            @Override
            public void onLost(Network network) {
                CcuLog.d(L.TAG_CCU, "Wifi network lost");
            }
        };
        connectivityManager.registerNetworkCallback(ethernetRequest, wifiCallback);
    }
    public static void unRegisterWifiListener() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (wifiCallback != null) {
                connectivityManager.unregisterNetworkCallback(wifiCallback);
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU, "Error unregistering wifi listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateBacnetIpAddressIfNetworkChanged(String networkType) {
        CcuLog.d(L.TAG_CCU, "Checking for network change for " + networkType);
        if (isBACnetIntialized()) {
            String confString = PreferenceManager.getDefaultSharedPreferences(context).getString(BACNET_CONFIGURATION, "");
            JSONObject config;
            JSONObject networkObject;
            try {
                config = new JSONObject(confString);
                networkObject = config.getJSONObject("network");
                String networkInterfaceFromConfig = networkObject.get(NETWORK_INTERFACE).toString();
                String ipAddressFromConfig = networkObject.get(IP_ADDRESS).toString();
                CcuLog.d(L.TAG_CCU, "Network interface and Ip from config: " + networkInterfaceFromConfig + ", " + ipAddressFromConfig);

                String ipAddress = getIpAddress(networkType);
                CcuLog.d(L.TAG_CCU, "Current IP address for " + networkType + ": " + ipAddress);

                if (networkType.equals(networkInterfaceFromConfig) && !ipAddress.isEmpty() && !ipAddress.equals(ipAddressFromConfig)) {
                    CcuLog.i(L.TAG_CCU, "Network change found, updating BACnet IP address");
                    networkObject.put(IP_ADDRESS, ipAddress);
                    config.put("network", networkObject);
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    CcuLog.i(L.TAG_CCU, "BACnet configuration updated with new IP address: " + ipAddress);
                    reInitialiseBacnetStack();
                } else {
                    CcuLog.d(L.TAG_CCU, "No network change detected or IP address is already up-to-date");
                }
            } catch (JSONException e) {
                CcuLog.d(L.TAG_CCU, "Error parsing BACnet configuration: " + e.getMessage());
            }
        } else {
            CcuLog.d(L.TAG_CCU, "BACnet is not initialized, skipping network change check");
        }
    }

    private String getIpAddress(String networkType) {
        String networkInterface = networkType.equals(WIFI) ? "wlan" : "eth";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                if (iface.getName().startsWith(networkInterface)) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                            CcuLog.d(Tags.BACNET, "device interface and ip " + iface.getDisplayName() + " - " + addr.getHostAddress());
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void initNetworkConfig() {
        SharedPreferences sharedPreferences = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE);
        String networkTypePref = sharedPreferences.getString("networkType", "eth0");
        String ipAddressPref = sharedPreferences.getString("ipAddress", "");
        String subnetMaskPref = sharedPreferences.getString("subnetMask", "");
        String broadcastAddressPref = sharedPreferences.getString("broadcastAddress", "");
        String dnsAddressPref = sharedPreferences.getString("dnsAddress", "");
        if (!networkTypePref.isEmpty() && !ipAddressPref.isEmpty()) {
            setNetwork(ipAddressPref, broadcastAddressPref, subnetMaskPref, networkTypePref, dnsAddressPref);
        }

    }

    public static void setNetwork(String ipAddress, String broadcast, String subNet, String networkType, String dnsAddress) {
        try {
            CcuLog.i("NetworkConfig", "Setting network: " + ipAddress + " " + broadcast + " " + subNet + " " + networkType);
            String ipAddrAddCmd = "ip addr add " + ipAddress + " broadcast " + broadcast + " dev " + networkType;
            String ifconfigCmd = "ifconfig " + networkType + " " + ipAddress + " netmask " + subNet + " up ";
            CcuLog.i("NetworkConfig", "Setting network: " + ipAddrAddCmd + " ; " + ifconfigCmd);
            RootCommandExecuter.runRootCommand(ipAddrAddCmd);
            //Weirdly often this needs to be called multiple times to take effect.
            RootCommandExecuter.runRootCommand(ifconfigCmd);
            RootCommandExecuter.runRootCommand(ifconfigCmd);
            RootCommandExecuter.runRootCommand(ifconfigCmd);
            if (!dnsAddress.isEmpty()) {
                RootCommandExecuter.runRootCommand("setprop net.dns1" + dnsAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void checkAndServerStatus() {
        DashboardHandlerKt.getDashboardConfiguration();
        if (DashboardUtilKt.isDashboardConfig(Globals.getInstance().getApplicationContext()) || isBACnetIntialized() || isBacnetMstpInitialized()) {
            startRestServer();
        }
    }

    @Override
    public void onLandingActivityLoaded() {
        CcuLog.i(L.TAG_CCU, "landing activity loaded - init messaging");
        initMessaging();
        Globals.getInstance().unRegisterLandingActivityListener();
    }
}
