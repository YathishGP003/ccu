package a75f.io.logic.diag;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.util.HashMap;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.data.message.MessageDbUtilKt;
import a75f.io.api.haystack.util.SchedulableMigrationKt;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.equips.CCUDiagEquip;
import a75f.io.domain.equips.CCUEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.util.MigrationUtil;
import a75f.io.logic.util.PreferenceUtil;

public class DiagEquip
{
    private static final String TAG = "DiagEquip";
    private static final String CMD_UPDATE_CCU = "update_ccu";
    private static DiagEquip instance = null;
    private static final String SHARED_PREFERENCE_NAME = "remote_status_pref";
    private static final String KEY_SCREEN_SHARING_STATUS = "screen_sharing_status";
    private static int countOfBacnetAppVersionNotFound = 0;
    private DiagEquip(){
    }

    static CCUDiagEquip ccuDiagEquip;
    static CCUEquip ccuEquip;

    public static DiagEquip getInstance() {
        if (instance == null) {
            instance = new DiagEquip();
        }
        return instance;
    }


    private static void addMigrationVersionPoint(CCUHsApi hsApi, String equipDis, String equipRef, String siteRef, String tz) {
        Point appVersion = new Point.Builder()
                .setDisplayName(equipDis+"-migrationVersion")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("diag").addMarker("migration").addMarker("version").addMarker("writable")
                .setUnit("")
                .setTz(tz)
                .setKind(Kind.STRING)
                .build();
        hsApi.addPoint(appVersion);
    }

    public static void createMigrationVersionPoint(CCUHsApi hsApi) {
        /*This will be called at the time of migration so Diag equip is not loaded in Domain yet*/
        HashMap<Object,Object> diagEquipMap = CCUHsApi.getInstance().readEntity("domainName == \"" + DomainName.diagEquip + "\"");
        if (diagEquipMap.isEmpty()) {
            CcuLog.d(L.TAG_CCU, " createMigrationVersionPoint - DiagEquip does not exist. ");
            return;
        }
        HashMap<Object,Object> migrationVerionsMap = CCUHsApi.getInstance().readEntity("point and diag and migration and version");
        if (!migrationVerionsMap.isEmpty()) {
            return;
        }
        Equip diagEquip = new Equip.Builder().setHashMap(diagEquipMap).build();
        addMigrationVersionPoint(hsApi, diagEquip.getDisplayName(), diagEquip.getId(),
                diagEquip.getSiteRef(), diagEquip.getTz());

    }


    public void updatePoints() {
        ccuDiagEquip = Domain.diagEquip;
        ccuEquip = Domain.ccuEquip;

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = Globals.getInstance().getApplicationContext().registerReceiver(null, ifilter);

        boolean isPowerConnected = false;
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
        if((plugged == BatteryManager.BATTERY_PLUGGED_AC) || (plugged == BatteryManager.BATTERY_PLUGGED_USB))
            isPowerConnected = true;

        ccuDiagEquip.getAppRestart().writeHisVal(AlertManager.getInstance().getRepo().checkIfAppRestarted() ? 1.0 : 0.0);

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        ccuDiagEquip.getPowerConnected().writeHisVal(charging ? 1.0 : (isPowerConnected ? 1.0 : 0.0));
        ccuDiagEquip.getChargingStatus().writeHisVal(charging ? 1.0 : 0.0);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        ccuDiagEquip.getBatteryLevel().writeHisVal((level*100)/scale);

        WifiManager wifi = (WifiManager) Globals.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiInfo wifiInfo = wifi.getConnectionInfo();
            if (wifiInfo != null && wifi.isWifiEnabled()) {
                updateWifiStatus(wifiInfo.getLinkSpeed(), wifiInfo.getRssi(), wifi.calculateSignalLevel(wifiInfo.getRssi(), 10));
            } else {
                updateWifiStatus(0, 0, 0);
            }
        }

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(mi);
            updateMemoryStatus(mi);
        }

        PackageManager pm = Globals.getInstance().getApplicationContext().getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0);
            String version = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1,pi.versionName.length() - 2);
            String prevVersion = ccuDiagEquip.getAppVersion().readDefaultStrVal();
            String hisVersion = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1);
            CcuLog.d("CCUDiagEquip","version ="+version+","+pi.versionName+","+pi.versionName.substring(pi.versionName.lastIndexOf('_')+1)+",prevVer="+prevVersion+prevVersion.equals( hisVersion));
            if(!prevVersion.equals(hisVersion)) {
                ccuDiagEquip.getAppVersion().writeDefaultVal(hisVersion);
                MessageDbUtilKt.updateAllRemoteCommandsHandled(Globals.getInstance().getApplicationContext(), CMD_UPDATE_CCU);
                //There has been an upgrade. Reset the BISKIT test mode.
                Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                        .edit().putBoolean("biskit_mode", false).apply();
            }
            if(!PreferenceUtil.isSRMigrationPointUpdated() && SchedulableMigrationKt.validateMigration()) {
                MigrationUtil.createZoneSchedulesIfMissing(CCUHsApi.getInstance());
                MigrationUtil.migrateZoneScheduleIfMissed(CCUHsApi.getInstance());
                PreferenceUtil.setSRMigrationPointUpdated();
            }

        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ccuDiagEquip.getSafeModeStatus().writeHisVal(Globals.getInstance().isSafeMode()? 1 : 0);
        updateFreeInternalStoragePoint();
        updateRemoteSessionStatusPoint();
        ccuEquip.getLogLevel().writeHisVal(ccuEquip.getLogLevel().readHisVal());

        countOfBacnetAppVersionNotFound = 0;
        updateAppVersionPoint(L.HOME_APP_PACKAGE_NAME, ccuDiagEquip.getHomeAppVersion());
        updateAppVersionPoint(L.REMOTE_ACCESS_PACKAGE_NAME, ccuDiagEquip.getRemoteAccessAppVersion());
        updateAppVersionPoint(L.BAC_APP_PACKAGE_NAME_OBSOLETE, ccuDiagEquip.getBacnetAppVersion());
        updateAppVersionPoint(L.BAC_APP_PACKAGE_NAME, ccuDiagEquip.getBacnetAppVersion());

    }

    private void updateMemoryStatus(ActivityManager.MemoryInfo mi) {
        ccuDiagEquip.getAvailableMemory().writeHisVal(mi.availMem / 1048576L);
        ccuDiagEquip.getTotalMemory().writeHisVal(mi.totalMem/1048576L);
        ccuDiagEquip.isLowMemory().writeHisVal(mi.lowMemory? 1.0 :0);
    }

    private void updateWifiStatus(int linkSpeed, int rssi, int signalStrength) {
        ccuDiagEquip.getWifiLinkSpeed().writeHisVal(linkSpeed);
        ccuDiagEquip.getWifiRssi().writeHisVal(rssi);
        ccuDiagEquip.getWifiSignalStrength().writeHisVal(signalStrength);
    }

    private void updateRemoteSessionStatusPoint() {
        SharedPreferences sharedPreferences = Globals.getInstance().getApplicationContext().getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        int screenSharingStatus = sharedPreferences.getInt(KEY_SCREEN_SHARING_STATUS, 0);
        ccuDiagEquip.getRemoteSessionStatus().writeHisVal(screenSharingStatus);
    }

    public void updateFreeInternalStoragePoint(){
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long freeInternalMemorySize = (((availableBlocks * blockSize)/1024)/1024);  // it returns size in MB
        CcuLog.d("CCUDiagEquip","freeInternalStorage "+freeInternalMemorySize);
        ccuDiagEquip.getAvailableInternalDiskStorage().writeHisVal(freeInternalMemorySize);
    }

    private static boolean isDebuggable() {
        return BuildConfig.BUILD_TYPE.equals("qa") ||
                BuildConfig.BUILD_TYPE.equalsIgnoreCase("dev") ||
                BuildConfig.BUILD_TYPE.equalsIgnoreCase("staging") ||
                BuildConfig.BUILD_TYPE.equalsIgnoreCase("dev_qa");
    }

    public static void updateAppVersionPoint(String appPackageName, a75f.io.domain.api.Point appVersionPoint) {
        CcuLog.i(TAG," updateAppVersionPoint - Updating App version point. "+appPackageName+","+appVersionPoint.getDomainName());
        PackageManager pm = Globals.getInstance().getApplicationContext().getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(appPackageName, 0);
            String version = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1,pi.versionName.length() - 2);
            String prevVersion = appVersionPoint.readDefaultStrVal();
            String hisVersion = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1);
            CcuLog.d(TAG,"version ="+version+","+pi.versionName+","+pi.versionName.substring(pi.versionName.lastIndexOf('_')+1)+",prevVer="+prevVersion+prevVersion.equals( hisVersion));
            if(!prevVersion.equals(hisVersion)) {
                appVersionPoint.writeDefaultVal(hisVersion);
            }
        } catch (PackageManager.NameNotFoundException e) {
            CcuLog.e(TAG, "Error getting package info for " + appPackageName);
            handleAppVersionWhenPackageNotFound(appVersionPoint);
            e.printStackTrace();
        }

    }

    private static void handleAppVersionWhenPackageNotFound(a75f.io.domain.api.Point appVersionPoint) {
        try {
            CcuLog.d(TAG, " handleAppVersionWhenPackageNotFound - Package not found for " + appVersionPoint.getDomainName());
            if (appVersionPoint.getDomainName().equals(DomainName.bacnetAppVersion)) {
                countOfBacnetAppVersionNotFound++;
            }
            String prevVersion = appVersionPoint.readDefaultStrVal();
            if (!prevVersion.isEmpty() && (!appVersionPoint.getDomainName().equals(DomainName.bacnetAppVersion) || countOfBacnetAppVersionNotFound == 2)) {
                String id = appVersionPoint.getId();
                CcuLog.d(TAG, "Clearing out the version for " + appVersionPoint.getDomainName() + " since package not found.");
                CCUHsApi.getInstance().clearPointArrayLevel(id, HayStackConstants.DEFAULT_POINT_LEVEL, false);
                if (appVersionPoint.getDomainName().equals(DomainName.bacnetAppVersion)) {
                    countOfBacnetAppVersionNotFound = 0; // we needs to reset the count
                }
            }
        } catch (Exception e) {
            CcuLog.e(TAG, "Error handling app version when package not found for " + e);
        }
    }
}
