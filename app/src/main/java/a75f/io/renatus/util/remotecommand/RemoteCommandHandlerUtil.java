package a75f.io.renatus.util.remotecommand;

import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.DOWNLOAD_BAC_APP;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESET_CM;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESET_PASSWORD;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESTART_CCU;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESTART_MODULE;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.RESTART_TABLET;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.SAVE_CCU_LOGS;
import static a75f.io.messaging.handler.RemoteCommandUpdateHandler.UPDATE_CCU;
import static a75f.io.renatus.ENGG.AppInstaller.DOWNLOAD_BASE_URL;


import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import a75f.io.alerts.AlertManager;
import a75f.io.alerts.AlertsConstantsKt;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
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
    private static final String TAG_DOWNLOAD_BAC_APP = "DOWNLOAD_BAC_APP";
    private static String bacAppApkName = "BACapp_qa_0.2.1.apk";
    private static String bacAppPackageName = "com.example.ccu_bacapp";

    private static String privAppPath = "/system/priv-app/";
    private static long bacAppDownloadId = -1;

    public static void handleRemoteCommand(String commands, String cmdLevel, String id) {
        CcuLog.d("RemoteCommand", "RemoteCommandHandlerUtil=" + commands + "," + cmdLevel);
        switch (commands) {
            case RESTART_CCU:
                AlertManager.getInstance().generateAlert(AlertsConstantsKt.CCU_RESTART, "CCU Restart request sent for  - " + CCUHsApi.getInstance().getCcuName());
                RenatusApp.closeApp();
                break;
            case RESTART_TABLET:
                AlertManager.getInstance().generateAlert(AlertsConstantsKt.DEVICE_RESTART, "Tablet Restart request sent for  - " + CCUHsApi.getInstance().getCcuName());
                RenatusApp.rebootTablet();
                break;
            case SAVE_CCU_LOGS:
                new Thread() {
                    @Override
                    public void run() {
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
                            Log.i("Remote Command","Command is not valid" + commands);
                        break;
                }
                break;
            case DOWNLOAD_BAC_APP:
                Log.i("Remote Command","DOWNLOAD_BAC_APP bac app version for download->" + id);
                bacAppApkName = id;
                updateCCU(id, null, null);
                setDownloadIdBacApp(downloadFile(DOWNLOAD_BASE_URL + bacAppApkName, bacAppApkName));
                break;
        }
    }

    private static void setDownloadIdBacApp(long downLoadId){
        bacAppDownloadId = downLoadId;
    }
    private static synchronized long downloadFile(String url, String apkFile) {
        Log.d(TAG_DOWNLOAD_BAC_APP,"--DOWNLOAD_BAC_APP--"+url);
        DownloadManager manager =
                (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Downloading bac app software");
        request.setTitle("Downloading bac app");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), apkFile);
        if (file.exists())
        {
            file.delete();
        }
        request.setDestinationInExternalFilesDir(RenatusApp.getAppContext(), null, apkFile);
        long downloadId = manager.enqueue(request);
        CcuLog.d(TAG_DOWNLOAD_BAC_APP, "downloading file: "+downloadId+","+url);
        return downloadId;
    }
    private static void copyFile(String apkFile, String destination){

        Log.d(TAG_DOWNLOAD_BAC_APP, "----copyFile----"+apkFile+"<--destination-->"+destination);
        File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), apkFile);
        if (!file.exists())
        {
            Log.d(TAG_DOWNLOAD_BAC_APP, "----file not found to copy@!@----");
            return;
        }

        String apkFilePath = file.getAbsolutePath();

        try {
            // Request root access
            if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
                // Remount the system partition as read-write
                String remountCommand = "mount -o rw,remount /system";
                Command remountCmd = new Command(0, remountCommand);
                RootTools.getShell(true).add(remountCmd);

                // Copy the APK file from data folder to system/priv-app
                String copyCommand = "cp " + apkFilePath + " " + destination + apkFile;
                Command copyCmd = new Command(0, copyCommand);
                RootTools.getShell(true).add(copyCmd);

                String changePermissionCommand = "chmod 644 "+ "/system/priv-app/"+apkFile;
                Command changePermissionCmd = new Command(0, changePermissionCommand);
                RootTools.getShell(true).add(changePermissionCmd);

                // Close the root shell
                RootTools.closeAllShells();

                Log.d(TAG_DOWNLOAD_BAC_APP, "----root access  granted file is copied reboot after 5  seconds----");

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        RenatusApp.rebootTablet();
                    }
                }, 5000);

            } else {
                Log.d(TAG_DOWNLOAD_BAC_APP, "----root access not granted----");
            }
        } catch (IOException | TimeoutException | RootDeniedException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateCCU(String id, Fragment currentFragment, FragmentActivity activity) {
        Log.d("CCU_DOWNLOAD", "got command to install update--" + DownloadManager.EXTRA_DOWNLOAD_ID + "," + id);
        RenatusApp.getAppContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    if (downloadId == AppInstaller.getHandle().getCCUAppDownloadId()) {
                        if(CCUHsApi.getInstance().isCCURegistered()) {
                            RxTask.executeAsync(() -> UtilityApplication.getMessagingAckJob().doMessageAck());
                        }
                        if (AppInstaller.getHandle().getDownloadedFileVersion(downloadId) > 1) {
                            AppInstaller.getHandle().install(null, false, true, true);
                        } else {
                            CcuLog.d("CCU_DOWNLOAD", "Update command ignored, Invalid version downloaded");
                            Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
                        }
                    } else if (downloadId == AppInstaller.getHandle().getHomeAppDownloadId()) {
                        int homeAppVersion = AppInstaller.getHandle().getDownloadedFileVersion(downloadId);
                        if (homeAppVersion >= 1) {
                            PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit().putInt("home_app_version", homeAppVersion).commit();
                            AppInstaller.getHandle().install(null, true, false, true);
                        }
                    } else if(downloadId == bacAppDownloadId){
                        new Thread(() -> {
                            boolean isBacAppInstalled = isPackageInstalled(bacAppPackageName, RenatusApp.context.getPackageManager());
                            CcuLog.d("CCU_DOWNLOAD", "isBacAppInstalled -->"+isBacAppInstalled);
                            if(isBacAppInstalled){
                                updateBacApp(bacAppApkName);
                            }else{
                                copyFile(bacAppApkName, privAppPath);
                            }
                        }).start();

                    }
                }
            }

        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        if (id.startsWith("75f") || id.startsWith("75F"))
            AppInstaller.getHandle().downloadHomeInstall(id);
        else if (id.startsWith("RENATUS_CCU") || id.startsWith("CCU") || id.startsWith("DAIKIN_CCU")) {
            if (System.currentTimeMillis() > Globals.getInstance().getCcuUpdateTriggerTimeToken() + 5 * 60 * 1000) {
                Globals.getInstance().setCcuUpdateTriggerTimeToken(System.currentTimeMillis());
                AppInstaller.getHandle().downloadCCUInstall(id, currentFragment, activity);
            } else {
                CcuLog.d("CCU_DOWNLOAD", "Update command ignored , previous update in progress "
                        + Globals.getInstance().getCcuUpdateTriggerTimeToken());
            }

        }
    }

    private static void updateBacApp(String apkName) {
        File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), apkName);
        final String[] commands = {"pm install -r -d -g " + file.getAbsolutePath()};
        CcuLog.d(L.TAG_CCU_DOWNLOAD, "Install AppInstall silent invokeInstallerIntent===>>>"  + "," + file.getAbsolutePath());
        RenatusApp.executeAsRoot(commands);
    }

    private static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
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
