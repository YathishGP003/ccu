package a75f.io.renatus;

import static a75f.io.device.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED;
import static a75f.io.logic.L.isSimulation;
import static a75f.io.logic.util.PreferenceUtil.getDataSyncProcessing;
import static a75f.io.logic.util.PreferenceUtil.getSyncStartTime;
import static a75f.io.usbserial.UsbServiceActions.ACTION_USB_PRIV_APP_PERMISSION_DENIED;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.raygun.raygun4android.RaygunClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
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
import a75f.io.api.haystack.util.DatabaseAction;
import a75f.io.api.haystack.util.DatabaseEvent;
import a75f.io.device.DeviceUpdateJob;
import a75f.io.device.EveryDaySchedulerService;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.logic.watchdog.Watchdog;
import a75f.io.messaging.MessageHandlerSubscriber;
import a75f.io.messaging.client.MessagingClient;
import a75f.io.messaging.handler.DataSyncHandler;
import a75f.io.messaging.service.MessageCleanUpWork;
import a75f.io.messaging.service.MessageRetryHandlerWork;
import a75f.io.messaging.service.MessagingAckJob;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.ota.OTAUpdateHandlerService;
import a75f.io.renatus.ota.OtaCache;
import a75f.io.renatus.registration.UpdateCCUFragment;
import a75f.io.renatus.schedules.FileBackupService;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.views.RebootDataCache;
import a75f.io.restserver.server.HttpServer;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbConnectService;
import a75f.io.usbserial.UsbModbusService;
import a75f.io.usbserial.UsbService;
import a75f.io.usbserial.UsbServiceActions;

/**
 * Created by rmatt isOn 7/19/2017.
 */

public abstract class UtilityApplication extends Application {
    private static final String TAG = "UtilityApplication";

    public static DhcpInfo dhcpInfo;
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

    private boolean isRoomDbReady = false;
    @Inject
    MessageHandlerSubscriber messageHandlerSubscriber;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED

                    NotificationHandler.setCMConnectionStatus(true);
                    LSerial.getInstance().setResetSeedMessage(true);
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbModbusService.ACTION_USB_MODBUS_DISCONNECTED: // USB DISCONNECTED
                    //NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB Modbus disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbConnectService.ACTION_USB_CONNECT_DISCONNECTED: // USB DISCONNECTED
                    //NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB Connect disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_PRIV_APP_PERMISSION_DENIED:
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, R.string.usb_permission_priv_app_msg, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    private UsbService usbService;
    private UsbModbusService usbModbusService;

    private UsbConnectService usbConnectService;

    private DeviceUpdateJob deviceUpdateJob;
    private static Prefs prefs;
    private static final String LOG_PREFIX = "CCU_UTILITYAPP";
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            try {
                CcuLog.d(LOG_PREFIX, "utility Application -" + arg1.isBinderAlive() + "," + arg1.toString() + "," + arg0.getClassName() + "," + arg1.getInterfaceDescriptor());
                if (arg1.isBinderAlive()) {
                    usbService = ((UsbService.UsbBinder) arg1).getService();
                    LSerial.getInstance().setUSBService(usbService);

                    //TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
                    usbService.setHandler(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    
    private final ServiceConnection usbModbusConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            try {
                CcuLog.d(LOG_PREFIX, "utility Application -" + arg1.isBinderAlive() + "," + arg1.toString() + "," + arg0.getClassName() + "," + arg1.getInterfaceDescriptor());
                if (arg1.isBinderAlive()) {
                    //Todo : modbus USB Serial to tested with real device
                    usbModbusService = ((UsbModbusService.UsbBinder) arg1).getService();
                    LSerial.getInstance().setModbusUSBService(usbModbusService);
                    usbModbusService.setHandler(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbModbusService = null;
        }
    };

    private final ServiceConnection usbConnectConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            try {
                CcuLog.d(LOG_PREFIX, "utility Application -" + arg1.isBinderAlive() + "," + arg1.toString() + "," + arg0.getClassName() + "," + arg1.getInterfaceDescriptor());
                if (arg1.isBinderAlive()) {
                    usbConnectService = ((UsbConnectService.UsbBinder) arg1).getService();
                    LSerial.getInstance().setUsbConnectService(usbConnectService);

                    //TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
                    usbConnectService.setHandler(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
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


    }

    public void setCcuDbReady(boolean isCcuDbReady) {
        isRoomDbReady = isCcuDbReady;
    }

    private void postProcessingInit(){
        CcuLog.i("CCU_DB", "postProcessingInit - start");

        //Remove this Equip Manager once all modbus models are migrated from Domain modeler
        EquipsManager.getInstance(this).setApplicationContext(this);

        /*CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> site = hayStack.readEntity("site");
        if (!site.isEmpty()) {
            Instabug.setUserAttribute("Site",site.get(Tags.DIS).toString());
        }
        HashMap<Object, Object> ccu = hayStack.readEntity("device and ccu");
        if (!ccu.isEmpty()) {
            Instabug.setUserAttribute("CCU",ccu.get(Tags.ID).toString()+":"+ccu.get(Tags.DIS));
        }*/

        Globals.getInstance().startTimerTask();
        isDataSyncRestartRequired();
        UpdateCCUFragment.abortCCUDownloadProcess();

        RaygunClient.setUser(userNameForCrashReportsFromHaystack());

        setUsbFilters();  // Start listening notifications from UsbService
        startService(new Intent(this, OTAUpdateHandlerService.class));  // Start OTA update event + timer handler service
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and

        startUsbModbusService(UsbModbusService.class, usbModbusConnection, null); // Start UsbService(if it was not
        startConnectService(UsbConnectService.class, usbConnectConnection, null);
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
        initMessaging();
        verifyAndroidInitScripts();
        OtaCache cache = new OtaCache();
        cache.restoreOtaRequests(context);
        CCUUtils.setCCUReadyProperty("false");
        CcuLog.i("UI_PROFILING", "UtilityApplication.onCreate Done");
        CcuLog.i("CCU_DB", "postProcessingInit - end");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onDatabaseLoad(DatabaseEvent event) {
        CcuLog.i("CCU_DB", "Event Type:@ " + event.getSerialAction().name());
        if (event.getSerialAction() == DatabaseAction.MESSAGE_DATABASE_LOADED_SUCCESS_INIT_UI) {
            postProcessingInit();
            CcuLog.i("CCU_DB", "post processing done- launch ui now");
            setCcuDbReady(true);
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

        String line = null;
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
                    CcuLog.e(L.TAG_CCU_INIT, "Error checking " + INIT_SCRIPT_FILE + " installation, return code " + String.valueOf(status));
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
                    CcuLog.e(L.TAG_CCU_INIT, "Error checking " + INIT_RC_FILE + " installation, return code " + String.valueOf(status));
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
                handleSafeMode(paramThrowable);
                RaygunClient.send(paramThrowable);
                paramThrowable.printStackTrace();
                CcuLog.e(L.TAG_CCU, "LifeCycleEvent App Crash");
                RenatusApp.closeApp();
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
            CCUHsApi.getInstance().writeHisValByQuery("point and safe and mode and diag and his", 1.0);
        } else if (OOMExceptionHandler.isOOMCausedByFragmentation(paramThrowable)) {
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


    private void setUsbFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        filter.addAction(UsbModbusService.ACTION_USB_MODBUS_DISCONNECTED);
        filter.addAction(UsbServiceActions.ACTION_USB_PRIV_APP_PERMISSION_DENIED);
        filter.addAction(UsbServiceActions.ACTION_USB_REQUIRES_TABLET_REBOOT);
        filter.addAction(UsbConnectService.ACTION_USB_CONNECT_DISCONNECTED);
        registerReceiver(mUsbReceiver, filter);
    }


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            this.startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void startUsbModbusService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbModbusService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            this.startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startConnectService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (isAdvancedAhuProfile()) {
            CcuLog.i(L.TAG_CCU, "Connect module service is started");
            if (!UsbConnectService.SERVICE_CONNECTED) {
                Intent startService = new Intent(this, service);
                if (extras != null && !extras.isEmpty()) {
                    Set<String> keys = extras.keySet();
                    for (String key : keys) {
                        String extra = extras.getString(key);
                        startService.putExtra(key, extra);
                    }
                }
                this.startService(startService);
            }
            Intent bindingIntent = new Intent(this, service);
            bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            CcuLog.i(L.TAG_CCU, "Connect module service is not required");
        }
    }

    @Override
    public void onTerminate() {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
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

    private void initMessaging() {
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
            Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(messagingAckJob.getJobRunnable(), TASK_SEPARATION + 30, MESSAGING_ACK_INTERVAL, TASK_SEPARATION_TIMEUNIT);
        }
    }

    public static MessagingAckJob getMessagingAckJob() {
        return messagingAckJob;
    }

    public static boolean isBACnetIntialized() { return prefs.getBoolean(IS_BACNET_INITIALIZED); }

    public static void stopRestServer() {
        HttpServer.Companion.getInstance(context).stopServer();
    }

    public static void startRestServer() {
        HttpServer.Companion.getInstance(context).startServer();
    }


    public static boolean isAdvancedAhuProfile() {
        if (isSimulation()) return false;
        try {
            HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and system and not modbus and not connectModule");
            if (!equip.isEmpty() && equip.get("profile").toString().equals("vavAdvancedHybridAhuV2")) {
                return true;
            }
        } catch (Exception e){
            // Just not to block anything
            e.printStackTrace();
        }
        return false;
    }
}
