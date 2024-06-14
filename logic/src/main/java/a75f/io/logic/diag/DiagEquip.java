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
import java.util.Objects;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Queries;
import a75f.io.api.haystack.Tags;
import a75f.io.data.message.MessageDbUtilKt;
import a75f.io.api.haystack.util.SchedulableMigrationKt;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.autocommission.AutoCommissioningState;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.util.MigrationUtil;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.logic.autocommission.remoteSession.RemoteSessionStatus;

public class DiagEquip
{
    private static final String CMD_UPDATE_CCU = "update_ccu";
    private static DiagEquip instance = null;
    private static final String SHARED_PREFERENCE_NAME = "remote_status_pref";
    private static final String KEY_SCREEN_SHARING_STATUS = "screen_sharing_status";
    private DiagEquip(){
    }
    
    public static DiagEquip getInstance() {
        if (instance == null) {
            instance = new DiagEquip();
        }
        return instance;
    }
    
    public String create() {
        HashMap<Object,Object> diagEquip = CCUHsApi.getInstance().readEntity("equip and diag");
        if (diagEquip.size() > 0) {
            CcuLog.d(L.TAG_CCU," DIAG Equip already created");
            return diagEquip.get("id").toString();
        }
        HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteRef = Objects.requireNonNull(siteMap.get(Tags.ID)).toString();
        String siteDis = Objects.requireNonNull(siteMap.get("dis")).toString();
        String tz = Objects.requireNonNull(siteMap.get("tz")).toString();
        Equip b = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-DiagEquip")
                          .addMarker("equip").addMarker("diag")
                          .setTz(tz).build();
        String equipRef = CCUHsApi.getInstance().addEquip(b);
        addPoints(equipRef, "DiagEquip");
        return equipRef;
    }

    public static Point getDiagHeartbeatPoint(String equipRef, String equipDis, String siteRef, String tz){
        return new Point.Builder()
                .setDisplayName(equipDis+"-ccuHeartbeat")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("linear")
                .addMarker("diag").addMarker("cloud").addMarker("connected").addMarker("his")
                .setTz(tz)
                .build();
    }
    public void addPoints(String equipRef, String equipDis) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        HashMap<Object,Object> siteMap = hsApi.readEntity(Tags.SITE);
        String siteRef = Objects.requireNonNull(siteMap.get(Tags.ID)).toString();
        String tz = Objects.requireNonNull(siteMap.get("tz")).toString();

        hsApi.addPoint(getDiagSafeModePoint(equipRef,equipDis,siteRef,tz));
        addLogLevelPoint(hsApi);

        Point batteryLevel = new Point.Builder()
                                           .setDisplayName(equipDis+"-batteryLevel")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef).setHisInterpolate("linear")
                                           .addMarker("diag").addMarker("battery").addMarker("level").addMarker("his")
                                           .setUnit("%")
                                           .setTz(tz)
                                           .build();
        hsApi.addPoint(batteryLevel);

        Point autoCommission = new Point.Builder()
                .setDisplayName(equipDis+"-autoCommissioning")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("linear").addMarker("cur")
                .addMarker("diag").addMarker("auto").addMarker("commissioning").addMarker("his").addMarker("writable")
                .setTz(tz)
                .setEnums(AutoCommissioningState.getEnum())
                .build();
        String autoCommissionPintId = hsApi.addPoint(autoCommission);
        CCUHsApi.getInstance().writeDefaultValById(autoCommissionPintId, 0.0);
        CCUHsApi.getInstance().writeHisValById(autoCommissionPintId, 0.0);
        AutoCommissioningUtil.setAutoCommissionState(AutoCommissioningState.NOT_STARTED);

        Point chargingStatus = new Point.Builder()
                                     .setDisplayName(equipDis+"-chargingStatus")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef).setHisInterpolate("linear")
                                     .addMarker("diag").addMarker("charging").addMarker("status").addMarker("his")
                                     .setTz(tz)
                                     .build();
        hsApi.addPoint(chargingStatus);

        Point powerConnected = new Point.Builder()
                .setDisplayName(equipDis+"-powerConnected")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("linear")
                .addMarker("diag").addMarker("power").addMarker("connected").addMarker("his")
                .setTz(tz)
                .build();
        hsApi.addPoint(powerConnected);
    
        Point wifiRssi = new Point.Builder()
                                       .setDisplayName(equipDis+"-wifiRssi")
                                       .setEquipRef(equipRef)
                                       .setSiteRef(siteRef).setHisInterpolate("linear")
                                       .addMarker("diag").addMarker("wifi").addMarker("rssi").addMarker("his")
                                       .setUnit("dB")
                                       .setTz(tz)
                                       .build();
        hsApi.addPoint(wifiRssi);

        Point ccuHeartbeat = getDiagHeartbeatPoint(equipRef, equipDis, siteRef, tz);
        hsApi.addPoint(ccuHeartbeat);
    
        Point wifiLinkSpeed = new Point.Builder()
                                 .setDisplayName(equipDis+"-wifiLinkSpeed")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef).setHisInterpolate("linear")
                                 .addMarker("diag").addMarker("wifi").addMarker("link").addMarker("speed").addMarker("his")
                                 .setUnit("mbps")
                                 .setTz(tz)
                                 .build();
        hsApi.addPoint(wifiLinkSpeed);
    
        Point wifiSignalStrength = new Point.Builder()
                                      .setDisplayName(equipDis+"-wifiSignalStrength")
                                      .setEquipRef(equipRef)
                                      .setSiteRef(siteRef).setHisInterpolate("linear")
                                      .addMarker("diag").addMarker("wifi").addMarker("signal").addMarker("strength").addMarker("his")
                                      .setUnit("dBm")
                                      .setTz(tz)
                                      .build();
        hsApi.addPoint(wifiSignalStrength);
    
        Point availableMemory = new Point.Builder()
                                           .setDisplayName(equipDis+"-availableMemory")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef).setHisInterpolate("linear")
                                           .addMarker("diag").addMarker("available").addMarker("memory").addMarker("his").addMarker("cur")
                                           .setUnit("MB")
                                           .setTz(tz)
                                           .build();
        hsApi.addPoint(availableMemory);
    
        Point totalMemory = new Point.Builder()
                                        .setDisplayName(equipDis+"-totalMemory")
                                        .setEquipRef(equipRef)
                                        .setSiteRef(siteRef).setHisInterpolate("linear")
                                        .addMarker("diag").addMarker("total").addMarker("memory").addMarker("his").addMarker("cur")
                                        .setUnit("MB")
                                        .setTz(tz)
                                        .build();
        hsApi.addPoint(totalMemory);
    
        Point isLowMemory = new Point.Builder()
                                    .setDisplayName(equipDis+"-isLowMemory")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef).setHisInterpolate("linear")
                                    .addMarker("diag").addMarker("low").addMarker("memory").addMarker("his").addMarker("cur")
                                    .setTz(tz)
                                    .build();
        hsApi.addPoint(isLowMemory);
    
        Point serialConnection = new Point.Builder()
                                    .setDisplayName(equipDis+"-serialConnection")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef).setHisInterpolate("linear")
                                    .addMarker("diag").addMarker("serial").addMarker("connection").addMarker("his")
                                    .setTz(tz)
                                    .build();
        hsApi.addPoint(serialConnection);
    
        Point firmwareUpdate = new Point.Builder()
                                         .setDisplayName(equipDis+"-firmwareUpdate")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef).setHisInterpolate("linear")
                                         .addMarker("diag").addMarker("firmware").addMarker("update").addMarker("his")
                                         .setTz(tz)
                                         .build();
        hsApi.addPoint(firmwareUpdate);

        Point appRestart = new Point.Builder()
                .setDisplayName(equipDis+"-appRestart")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("linear")
                .addMarker("diag").addMarker("app").addMarker("restart").addMarker("his")
                .setUnit("")
                .setTz(tz)
                .build();
        hsApi.addPoint(appRestart);
        Point appVersion = new Point.Builder()
                .setDisplayName(equipDis+"-appVersion")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("diag").addMarker("app").addMarker("version").addMarker("writable")
                .setUnit("")
                .setTz(tz)
                .setKind(Kind.STRING)
                .build();
        hsApi.addPoint(appVersion);

        Point internalDiskStorage = new Point.Builder()
                .setDisplayName(equipDis+"-availableInternalDiskStorage")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("linear")
                .addMarker("diag").addMarker("available").addMarker("internal").addMarker("disk")
                .addMarker("storage").addMarker("his").addMarker("cur")
                .setUnit("MB")
                .setTz(tz)
                .build();
        hsApi.addPoint(internalDiskStorage);

        Point remoteSessionStatus = new Point.Builder()
                .setDisplayName(equipDis+"-remoteSessionStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("cov")
                .addMarker("diag").addMarker("remote").addMarker("status").addMarker("sp")
                .addMarker("storage").addMarker("his").addMarker("cur")
                .setEnums(RemoteSessionStatus.getEnum())
                .setTz(tz)
                .build();
        hsApi.addPoint(remoteSessionStatus);

        addMigrationVersionPoint(hsApi, equipDis, equipRef, siteRef, tz);
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
        HashMap<Object,Object> diagEquipMap = CCUHsApi.getInstance().readEntity("equip and diag");
        if (diagEquipMap.isEmpty()) {
            CcuLog.d(L.TAG_CCU," createMigrationVersionPoint - DiagEquip does not exist. ");
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
        HashMap<Object,Object> diagEquip = CCUHsApi.getInstance().readEntity("equip and diag");
        if (diagEquip.size() == 0) {
            CcuLog.d(L.TAG_CCU," DIAG Equip does not exist");
            return;
        }
    
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = Globals.getInstance().getApplicationContext().registerReceiver(null, ifilter);

        boolean isPowerConnected = false;
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
        if((plugged == BatteryManager.BATTERY_PLUGGED_AC) || (plugged == BatteryManager.BATTERY_PLUGGED_USB))
            isPowerConnected = true;

        setDiagHisVal("app and restart", AlertManager.getInstance().getRepo().checkIfAppRestarted() ? 1.0 : 0.0);

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL;
        setDiagHisVal("charging and status", charging ? 1.0 : 0);

        setDiagHisVal("power and connected", (charging ? 1.0 : (isPowerConnected ? 1.0 : 0)));
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        setDiagHisVal("battery and level", (level*100)/scale);
        
        WifiManager wifi = (WifiManager) Globals.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiInfo wifiInfo = wifi.getConnectionInfo();
            if (wifiInfo != null && wifi.isWifiEnabled()) {
                setDiagHisVal("wifi and link and speed", wifiInfo.getLinkSpeed());
                setDiagHisVal("wifi and rssi", wifiInfo.getRssi());
                setDiagHisVal("wifi and signal and strength", wifi.calculateSignalLevel(wifiInfo.getRssi(), 10));
            } else {
                setDiagHisVal("wifi and link and speed", 0);
                setDiagHisVal("wifi and rssi", 0);
                setDiagHisVal("wifi and signal and strength", 0);
            }
        }
        
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(mi);
            setDiagHisVal("available and memory",mi.availMem / 1048576L);
            setDiagHisVal("total and memory", mi.totalMem/1048576L);
            setDiagHisVal("low and memory",  mi.lowMemory? 1.0 :0);
        }

        PackageManager pm = Globals.getInstance().getApplicationContext().getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0);
            String version = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1,pi.versionName.length() - 2);
            String prevVersion = CCUHsApi.getInstance().readDefaultStrVal("point and diag and app and version");
            String hisVersion = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1);
            CcuLog.d("DiagEquip","version ="+version+","+pi.versionName+","+pi.versionName.substring(pi.versionName.lastIndexOf('_')+1)+",prevVer="+prevVersion+prevVersion.equals( hisVersion));
            if(!prevVersion.equals( hisVersion)) {
                CCUHsApi.getInstance().writeDefaultVal("point and diag and app and version", hisVersion);
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

        setDiagHisVal("safe and mode and status", Globals.getInstance().isSafeMode()? 1 : 0);
        updateFreeInternalStoragePoint();
        updateRemoteSessionStatusPoint();
        setDiagHisVal("log and level", CCUHsApi.getInstance().readHisValByQuery(Queries.LOG_LEVEL_QUERY));

    }

    private void updateRemoteSessionStatusPoint() {
        SharedPreferences sharedPreferences = Globals.getInstance().getApplicationContext().getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        int screenSharingStatus = sharedPreferences.getInt(KEY_SCREEN_SHARING_STATUS, 0);
        setDiagHisVal("remote and status",screenSharingStatus);
    }

    public void setDiagHisVal(String tag, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and diag and "+tag, val);
    }

    public void updateFreeInternalStoragePoint(){
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long freeInternalMemorySize = (((availableBlocks * blockSize)/1024)/1024);  // it returns size in MB
        CcuLog.d("DiagEquip","freeInternalStorage "+freeInternalMemorySize);
        setDiagHisVal("internal and disk",freeInternalMemorySize);
    }

    public static Point getDiagSafeModePoint(String equipRef, String equipDis, String siteRef, String tz){
        return  new Point.Builder()
                .setDisplayName(equipDis+"-safeModeStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("cov")
                .addMarker("diag").addMarker("safe").addMarker("mode").addMarker("his")
                .addMarker("cur").addMarker("sp").addMarker("status")
                .setKind(Kind.NUMBER)
                .setTz(tz)
                .build();
    }

    public static void addLogLevelPoint(CCUHsApi hsApi) {
        HashMap<Object,Object> diagEquipMap = CCUHsApi.getInstance().readEntity("equip and diag");
        if (diagEquipMap.isEmpty()) {
            CcuLog.d(L.TAG_CCU," createLoglevel - DiagEquip does not exist. ");
            return;
        }

        HashMap<Object,Object> logLevelPoint = CCUHsApi.getInstance().readEntity(Queries.LOG_LEVEL_QUERY);
        if (!logLevelPoint.isEmpty()) {
            return;
        }
        double defaultLogLevel = 4.0;
        if(isDebuggable()) {
            defaultLogLevel = 0.0;
        }

        Equip diagEquip = new Equip.Builder().setHashMap(diagEquipMap).build();

        Point logLevel = new Point.Builder()
                .setDisplayName("DiagEquip" + "-LogLevel")
                .setEquipRef(diagEquip.getId())
                .setSiteRef(diagEquip.getSiteRef())
                .addMarker("diag").addMarker("log").addMarker("level")
                .addMarker("cur").addMarker("sp").addMarker("his")
                .setHisInterpolate("cov")
                .setUnit("")
                .setTz(diagEquip.getTz())
                .setEnums("verbose,debug,info,warn,error")
                .build();
        String logLevelId = hsApi.addPoint(logLevel);
        hsApi.writeHisValById(logLevelId, defaultLogLevel);


    }

    private static boolean isDebuggable() {
        return BuildConfig.BUILD_TYPE.equals("qa") ||
                BuildConfig.BUILD_TYPE.equalsIgnoreCase("dev") ||
                BuildConfig.BUILD_TYPE.equalsIgnoreCase("staging") ||
                BuildConfig.BUILD_TYPE.equalsIgnoreCase("dev_qa");
    }

}
