package a75f.io.renatus.util.remotecommand;

import static a75f.io.logic.L.TAG_CCU_DOWNLOAD;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.OTA_UPDATE_HOME_APP;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.OTA_UPDATE_BAC_APP;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.OTA_UPDATE_REMOTE_ACCESS_APP;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESET_CM;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESET_PASSWORD;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESTART_CCU;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESTART_MODULE;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESTART_TABLET;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.SAVE_CCU_LOGS;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.UPDATE_CCU;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.UPDATE_CCU_LOG_LEVEL;
import static a75f.io.renatus.ENGG.AppInstaller.DOWNLOAD_BASE_URL;


import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;


import java.io.File;

import a75f.io.alerts.AlertManager;
import a75f.io.alerts.AlertsConstantsKt;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Queries;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.LSmartNode;
import a75f.io.device.mesh.LSmartStat;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.mesh.hypersplit.HyperSplitMessageSender;
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender;
import a75f.io.device.serial.CcuToCmOverUsbCmResetMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.diag.otastatus.OtaStatus;
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint;
import a75f.io.logic.logtasks.UploadLogs;
import a75f.io.logic.util.RxTask;
import a75f.io.renatus.ENGG.AppInstaller;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.util.CCUUtils;

public class RemoteCommandHandlerUtil {
    private static final String REMOUNT_RW = "mount -o rw,remount /system";
    private static final String REMOVE_FILE = "rm -f %s";
    private static final String MOVE_FILE = "mv %s %s";
    private static final String INSTALL_CMD = "pm install -r -d -g --user 0 %s";
    private static final String UNINSTALL_CMD = "pm uninstall --user 0 %s";
    private static final String SET_HOME_APP_CMD = "cmd package set-home-activity --user 0 \"%s/.MainActivity\"";
    private static final String APPOPS_SET_ALLOW_CMD = "appops set %s %s allow";
    private static final String PM_GRANT_CMD = "pm grant %s %s";
    private static final String BAC_APP_PACKAGE_NAME = "io.seventyfivef.bacapp";
    private static final String REMOTE_ACCESS_PACKAGE_NAME = "io.seventyfivef.remoteaccess";
    private static final String HOME_APP_PACKAGE_NAME_OBSOLETE = "io.seventyfivef.home";
    private static final String HOME_APP_PACKAGE_NAME = "com.x75frenatus.home";

    private static long bacAppDownloadId = -1;
    private static String bacAppApkName = "";

    private static long remoteAccessAppDownloadId = -1;
    private static String remoteAccessApkName = "";

    private static long homeAppDownloadId = -1;
    private static String homeAppApkName = "";

    private static BroadcastReceiver otaBroadcastReceiver = null;

    public static void handleRemoteCommand(String commands, String cmdLevel, String id) {
        CcuLog.i(L.TAG_CCU_REMOTE_COMMAND, "RemoteCommandHandlerUtil=" + commands + "," + cmdLevel);
        switch (commands) {
            case RESTART_CCU:
                RenatusApp.restartApp();
                break;
            case RESTART_TABLET:
                AlertManager.getInstance().generateAlert(AlertsConstantsKt.DEVICE_RESTART, "Tablet Restart request sent for  - " + CCUHsApi.getInstance().getCcuName());
                RenatusApp.rebootTablet();
                break;
            case SAVE_CCU_LOGS:
                new Thread() {
                    @Override
                    public void run() {
                        saveImportantDataBeforeSaveLogs();
                        UploadLogs.instanceOf().saveCcuLogs();
                    }
                }.start();
                break;
            case RESET_PASSWORD:
                CCUUtils.resetPasswords(RenatusApp.getAppContext());
                break;
            case RESET_CM:
                AlertManager.getInstance().generateAlert(AlertsConstantsKt.CM_RESET, "CM Reset request sent for  - " + CCUHsApi.getInstance().getCcuName());
                CcuToCmOverUsbCmResetMessage_t msg = new CcuToCmOverUsbCmResetMessage_t();
                msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_CM_RESET);
                msg.reset.set((short) 1);
                MeshUtil.sendStructToCM(msg);
                LSerial.getInstance().setResetSeedMessage(true);
                break;
            case UPDATE_CCU:
                OtaStatusDiagPoint.Companion.updateCCUOtaStatus(OtaStatus.OTA_REQUEST_RECEIVED);
                updateCCU(id, null, null);
                break;
            case RESTART_MODULE:

                //TODO Send commands to SmartNode
                switch (cmdLevel) {
                    case "system":
                        for (Floor floor : HSUtil.getFloors()) {
                            for (Zone zone : HSUtil.getZones(floor.getId())) {
                                for (Device d : HSUtil.getDevices(zone.getId())) {
                                    if (d.getMarkers().contains("smartstat")) {
                                        sendSmartStatResetMsg(d.getAddr());
                                    } else if (d.getMarkers().contains("smartnode") ||
                                            d.getMarkers().contains("otn") || d.getMarkers().contains(Tags.HELIO_NODE)) {
                                        sendSmarNodeResetMsg(d.getAddr());
                                    } else if (d.getMarkers().contains("hyperstat")) {
                                        HyperStatMessageSender.sendRestartModuleCommand(Integer.parseInt(d.getAddr()));
                                    } else if (d.getMarkers().contains("hyperstatsplit")) {
                                        HyperSplitMessageSender.sendRestartModuleCommand(Integer.parseInt(d.getAddr()));
                                    }
                                }
                            }
                        }
                        break;
                    case "zone":
                        for (Device d : HSUtil.getDevices("@" + id)) {
                            if (d.getMarkers().contains("smartstat")) {
                                sendSmartStatResetMsg(d.getAddr());
                            } else if (d.getMarkers().contains("smartnode") ||
                                    d.getMarkers().contains("otn") || d.getMarkers().contains(Tags.HELIO_NODE)) {
                                sendSmarNodeResetMsg(d.getAddr());
                            } else if (d.getMarkers().contains("hyperstat")) {
                                HyperStatMessageSender.sendRestartModuleCommand(Integer.parseInt(d.getAddr()));
                            } else if (d.getMarkers().contains("hyperstatsplit")) {
                                HyperSplitMessageSender.sendRestartModuleCommand(Integer.parseInt(d.getAddr()));
                            }
                        }
                        break;
                    case "module":
                        Equip equip = HSUtil.getEquipInfo("@" + id);
                        if (equip.getMarkers().contains("smartstat")) {
                            sendSmartStatResetMsg(equip.getGroup());
                        } else if (equip.getMarkers().contains("smartnode") ||
                                equip.getMarkers().contains("otn") || equip.getMarkers().contains(Tags.HELIO_NODE)) {
                            sendSmarNodeResetMsg(equip.getGroup());
                        } else if (equip.getMarkers().contains("hyperstat")) {
                            HyperStatMessageSender.sendRestartModuleCommand(Integer.parseInt(equip.getGroup()));
                        } else if (equip.getMarkers().contains("hyperstatsplit")) {
                            HyperSplitMessageSender.sendRestartModuleCommand(Integer.parseInt(equip.getGroup()));
                        }
                        break;
                    default:
                        CcuLog.i(L.TAG_CCU_REMOTE_COMMAND,"Command is not valid" + commands);
                        break;
                }
                break;
            case OTA_UPDATE_BAC_APP:
                CcuLog.i(L.TAG_CCU_REMOTE_COMMAND,"Downloading BAC App for OTA Update; ApkName=" + id);
                bacAppApkName = id;
                updateCCU(id, null, null);
                setDownloadIdBacApp(downloadFile(DOWNLOAD_BASE_URL + bacAppApkName, bacAppApkName));
                break;
            case OTA_UPDATE_REMOTE_ACCESS_APP:
                CcuLog.i(L.TAG_CCU_REMOTE_COMMAND,"Downloading Remote Access Agent for OTA Update; ApkName=" + id);
                remoteAccessApkName = id;
                updateCCU(id, null, null);
                setRemoteAccessAppDownloadId(downloadFile(DOWNLOAD_BASE_URL + remoteAccessApkName, remoteAccessApkName));
                break;
            case OTA_UPDATE_HOME_APP:
                CcuLog.i(L.TAG_CCU_REMOTE_COMMAND,"Downloading Home App for OTA Update; ApkName=" + id);
                homeAppApkName = id;
                updateCCU(id, null, null);
                setHomeAppDownloadId(downloadFile(DOWNLOAD_BASE_URL + homeAppApkName, homeAppApkName));
                break;
            case UPDATE_CCU_LOG_LEVEL:
                handleCCULogLevelChange(id);
                break;
        }
    }

    private static void handleCCULogLevelChange(String logLevel) {
        if(logLevel == null || logLevel.isEmpty()){
            return;
        }
        double logLevelValue = -1;
        switch (logLevel) {
            case "verbose":
                logLevelValue = 0.0;
                break;
            case "debug":
                logLevelValue = 1.0;
                break;
            case "info":
                logLevelValue = 2.0;
                break;
            case "warn":
                logLevelValue = 3.0;
                break;
            case "error":
                logLevelValue = 4.0;
                break;
        }
        if(logLevelValue == -1)
            return;
        CCUHsApi.getInstance().writeHisValByQuery(Queries.LOG_LEVEL_QUERY,logLevelValue);
        CCUHsApi.getInstance().setCcuLogLevel(logLevelValue);
    }

    private static void saveImportantDataBeforeSaveLogs() {
        try{
            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
            sharedPreferences.edit()
                    .putLong("SAVE_LOG_TIMESTAMP", System.currentTimeMillis())
                    .putLong("HIS_BOX_SIZE", CCUHsApi.getInstance().tagsDb.getBoxStore().boxFor(HisItem.class).count())
                    .putLong("ALERT_BOX_SIZE", CCUHsApi.getInstance().tagsDb.getBoxStore().boxFor(Alert.class).count()).apply();
            PackageManager packageManager = Globals.getInstance().getApplicationContext().getPackageManager();
            ApplicationInfo appInfo = packageManager
                    .getApplicationInfo(Globals.getInstance().getApplicationContext().getPackageName(), 0);
            String dataDir = appInfo.dataDir;
            File dataDirectory = new File(dataDir);
            long dataSize =getDirectorySize(dataDirectory);
            sharedPreferences.edit().putLong("APP_DATA_SIZE", (dataSize/1024)/1024).apply();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static long getDirectorySize(File directory) {
        long size = 0;

        if (directory.exists()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }

        return size;
    }

    private static void setDownloadIdBacApp(long downloadId){
        bacAppDownloadId = downloadId;
    }

    private static void setRemoteAccessAppDownloadId(long downloadId){
        remoteAccessAppDownloadId = downloadId;
    }

    private static void setHomeAppDownloadId(long downloadId){
        homeAppDownloadId = downloadId;
    }

    private static synchronized long downloadFile(String url, String apkFile) {
        CcuLog.d(TAG_CCU_DOWNLOAD,"--DOWNLOAD_APP_URL--"+url);
        DownloadManager manager =
                (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Downloading "+apkFile+" software");
        request.setTitle("Downloading "+apkFile+" app");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), apkFile);
        if (file.exists())
        {
            file.delete();
        }
        request.setDestinationInExternalFilesDir(RenatusApp.getAppContext(), null, apkFile);
        long downloadId = manager.enqueue(request);
        CcuLog.d(TAG_CCU_DOWNLOAD, "downloading file: "+downloadId+","+url);
        return downloadId;
    }

    public static String resolveApkFilename(String apkName) {
        File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), apkName);
        final String filePath = file.getAbsolutePath();
        if (!file.exists()) {
            CcuLog.e(TAG_CCU_DOWNLOAD, String.format("Unable to find APK for %s, file %s does not exist", apkName, filePath));
            return null;
        }
        return filePath;
    }

    public static void updateCCU(String id, Fragment currentFragment, FragmentActivity activity) {
        CcuLog.i(TAG_CCU_DOWNLOAD, "got command to install update--" + DownloadManager.EXTRA_DOWNLOAD_ID + "," + id);
        if (otaBroadcastReceiver == null) {
            CcuLog.i(TAG_CCU_DOWNLOAD, "Registering OTA download BroadcastReceiver");
            otaBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                        if (downloadId == AppInstaller.getHandle().getCCUAppDownloadId()) {
                            if (CCUHsApi.getInstance().isCCURegistered()) {
                                RxTask.executeAsync(() -> UtilityApplication.getMessagingAckJob().doMessageAck());
                            }
                            if (AppInstaller.getHandle().getDownloadedFileVersion(downloadId) > 1) {
                                AppInstaller.getHandle().install(null, false, true, true);
                            } else {
                                CcuLog.w(TAG_CCU_DOWNLOAD, "Update command ignored, Invalid version downloaded");
                                Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
                            }
                        } else if (downloadId == bacAppDownloadId) {
                            String fileName = resolveApkFilename(bacAppApkName);
                            if (fileName != null) {
                                String[] commands = new String[]{
                                        // uninstall the bacapp with older package name
                                        String.format(UNINSTALL_CMD, "com.example.ccu_bacapp"),
                                        String.format(INSTALL_CMD, fileName)
                                };
                                RenatusApp.executeAsRoot(commands, BAC_APP_PACKAGE_NAME, false, false);
                            }
                        } else if (downloadId == remoteAccessAppDownloadId) {
                            String fileName = resolveApkFilename(remoteAccessApkName);
                            if (fileName != null) {
                                int appVersion = AppInstaller.getHandle().getDownloadedFileVersion(remoteAccessAppDownloadId);
                                if (appVersion >= 1) {
                                    CcuLog.i(TAG_CCU_DOWNLOAD, String.format("Updating remote app to version %d", appVersion));
                                }
                                String[] commands = new String[]{
                                        String.format(INSTALL_CMD, fileName),
                                        String.format(APPOPS_SET_ALLOW_CMD, REMOTE_ACCESS_PACKAGE_NAME, "PROJECT_MEDIA"),                     // Screen Capture access
                                        String.format(APPOPS_SET_ALLOW_CMD, REMOTE_ACCESS_PACKAGE_NAME, "SYSTEM_ALERT_WINDOW"),               // Overlay access
                                        String.format(PM_GRANT_CMD, REMOTE_ACCESS_PACKAGE_NAME, "android.permission.WRITE_SECURE_SETTINGS")   // Accessibility access
                                };
                                RenatusApp.executeAsRoot(commands, REMOTE_ACCESS_PACKAGE_NAME, false, false);
                            }
                        } else if (downloadId == homeAppDownloadId) {
                            String fileName = resolveApkFilename(homeAppApkName);
                            int homeAppVersion = AppInstaller.getHandle().getDownloadedFileVersion(homeAppDownloadId);
                            if (homeAppVersion >= 1) {
                                CcuLog.i(TAG_CCU_DOWNLOAD, String.format("Updating home app to version %d", homeAppVersion));
                                PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit().putInt("home_app_version", homeAppVersion).commit();
                            }

                            if (fileName != null) {
                                // Get apk filename
                                String[] pieces = fileName.split("/");
                                String baseName = pieces[pieces.length - 1];
                                String[] commands = new String[]{
                                        REMOUNT_RW,
                                        String.format(UNINSTALL_CMD, HOME_APP_PACKAGE_NAME),            // Necessary because the signing key changed
                                        String.format(REMOVE_FILE, "/system/priv-app/75fHome*.apk"),

                                        String.format(UNINSTALL_CMD, HOME_APP_PACKAGE_NAME_OBSOLETE),   // Necessary because a few have been installed in the field
                                        String.format(REMOVE_FILE, "/system/priv-app/HomeApp*.apk"),

                                        String.format(MOVE_FILE, fileName, "/system/priv-app"),
                                        String.format("chmod 644 /system/priv-app/%s", baseName),
                                        String.format("chown root.root /system/priv-app/%s", baseName),
                                        String.format(INSTALL_CMD, String.format("/system/priv-app/%s", baseName)),
                                        String.format(SET_HOME_APP_CMD, HOME_APP_PACKAGE_NAME),
                                };

                                RenatusApp.executeAsRoot(commands, null, false, true);
                            }
                        }
                    }
                }
            };

            RenatusApp.getAppContext().registerReceiver(otaBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        if (id.startsWith("RENATUS_CCU") || id.startsWith("CCU") || id.startsWith("DAIKIN_CCU")) {
            if (System.currentTimeMillis() > Globals.getInstance().getCcuUpdateTriggerTimeToken() + 5 * 60 * 1000) {
                Globals.getInstance().setCcuUpdateTriggerTimeToken(System.currentTimeMillis());
                AppInstaller.getHandle().downloadCCUInstall(id, currentFragment, activity);
            } else {
                CcuLog.w(TAG_CCU_DOWNLOAD, "Update command ignored , previous update in progress "
                        + Globals.getInstance().getCcuUpdateTriggerTimeToken());
            }
        }
    }

    private static void sendSmartStatResetMsg(String address){
        CcuToCmOverUsbSmartStatControlsMessage_t ssControlsMessage =
                LSmartStat.getControlMessageforEquip(address,CCUHsApi.getInstance());
        ssControlsMessage.controls.reset.set((short)1);
        MeshUtil.sendStructToNodes(ssControlsMessage);
    }

    private static void sendSmarNodeResetMsg(String address){
        CcuToCmOverUsbSnControlsMessage_t snControlsMessage =
                LSmartNode.getControlMessageforEquip(address,CCUHsApi.getInstance());
        snControlsMessage.controls.reset.set((short) 1);
        MeshUtil.sendStructToNodes(snControlsMessage);
    }
}