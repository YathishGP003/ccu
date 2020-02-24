package a75f.io.logic.diag;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.R;

public class DiagEquip
{
    private static DiagEquip instance = null;
    private DiagEquip(){
    }
    
    public static DiagEquip getInstance() {
        if (instance == null) {
            instance = new DiagEquip();
        }
        return instance;
    }
    
    public String create() {
        HashMap diagEquip = CCUHsApi.getInstance().read("equip and diag");
        if (diagEquip.size() > 0) {
            CcuLog.d(L.TAG_CCU," DIAG Equip already created");
            return null;
        }
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        Equip b = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-DiagEquip")
                          .addMarker("equip").addMarker("diag").addMarker("equipHis")
                          .setTz(tz).build();
        String equipRef = CCUHsApi.getInstance().addEquip(b);
        addPoints(equipRef, "DiagEquip");
        return equipRef;
    }
    
    public void addPoints(String equipRef, String equipDis) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        HashMap siteMap = hsApi.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();
        
        Point batteryLevel = new Point.Builder()
                                           .setDisplayName(equipDis+"-batteryLevel")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .addMarker("diag").addMarker("battery").addMarker("level").addMarker("his").addMarker("equipHis")
                                           .setUnit("%")
                                           .setTz(tz)
                                           .build();
        hsApi.addPoint(batteryLevel);
    
        Point chargingStatus = new Point.Builder()
                                     .setDisplayName(equipDis+"-chargingStatus")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef)
                                     .addMarker("diag").addMarker("charging").addMarker("status").addMarker("his").addMarker("equipHis")
                                     .setTz(tz)
                                     .build();
        hsApi.addPoint(chargingStatus);

        Point powerConnected = new Point.Builder()
                .setDisplayName(equipDis+"-powerConnected")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("diag").addMarker("power").addMarker("connected").addMarker("his").addMarker("equipHis")
                .setTz(tz)
                .build();
        hsApi.addPoint(powerConnected);
    
        Point wifiRssi = new Point.Builder()
                                       .setDisplayName(equipDis+"-wifiRssi")
                                       .setEquipRef(equipRef)
                                       .setSiteRef(siteRef)
                                       .addMarker("diag").addMarker("wifi").addMarker("rssi").addMarker("his").addMarker("equipHis")
                                       .setUnit("dB")
                                       .setTz(tz)
                                       .build();
        hsApi.addPoint(wifiRssi);
    
        Point wifiLinkSpeed = new Point.Builder()
                                 .setDisplayName(equipDis+"-wifiLinkSpeed")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .addMarker("diag").addMarker("wifi").addMarker("link").addMarker("speed").addMarker("his").addMarker("equipHis")
                                 .setUnit("mbps")
                                 .setTz(tz)
                                 .build();
        hsApi.addPoint(wifiLinkSpeed);
    
        Point wifiSignalStrength = new Point.Builder()
                                      .setDisplayName(equipDis+"-wifiSignalStrength")
                                      .setEquipRef(equipRef)
                                      .setSiteRef(siteRef)
                                      .addMarker("diag").addMarker("wifi").addMarker("signal").addMarker("strength").addMarker("his").addMarker("equipHis")
                                      .setUnit("dBm")
                                      .setTz(tz)
                                      .build();
        hsApi.addPoint(wifiSignalStrength);
    
        Point availableMemory = new Point.Builder()
                                           .setDisplayName(equipDis+"-availableMemory")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .addMarker("diag").addMarker("available").addMarker("memory").addMarker("his").addMarker("equipHis")
                                           .setUnit("MB")
                                           .setTz(tz)
                                           .build();
        hsApi.addPoint(availableMemory);
    
        Point totalMemory = new Point.Builder()
                                        .setDisplayName(equipDis+"-totalMemory")
                                        .setEquipRef(equipRef)
                                        .setSiteRef(siteRef)
                                        .addMarker("diag").addMarker("total").addMarker("memory").addMarker("his").addMarker("equipHis")
                                        .setUnit("MB")
                                        .setTz(tz)
                                        .build();
        hsApi.addPoint(totalMemory);
    
        Point isLowMemory = new Point.Builder()
                                    .setDisplayName(equipDis+"-isLowMemory")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .addMarker("diag").addMarker("low").addMarker("memory").addMarker("his").addMarker("equipHis")
                                    .setTz(tz)
                                    .build();
        hsApi.addPoint(isLowMemory);
    
        Point serialConnection = new Point.Builder()
                                    .setDisplayName(equipDis+"-serialConnection")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .addMarker("diag").addMarker("serial").addMarker("connection").addMarker("his").addMarker("equipHis")
                                    .setTz(tz)
                                    .build();
        hsApi.addPoint(serialConnection);
    
        Point firmwareUpdate = new Point.Builder()
                                         .setDisplayName(equipDis+"-firmwareUpdate")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("diag").addMarker("firmware").addMarker("update").addMarker("his").addMarker("equipHis")
                                         .setTz(tz)
                                         .build();
        hsApi.addPoint(firmwareUpdate);

        Point appRestart = new Point.Builder()
                .setDisplayName(equipDis+"-appRestart")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("diag").addMarker("app").addMarker("restart").addMarker("his").addMarker("equipHis")
                .setUnit("")
                .setTz(tz)
                .build();
        hsApi.addPoint(appRestart);
        Point appVersion = new Point.Builder()
                .setDisplayName(equipDis+"-appVersion")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("diag").addMarker("app").addMarker("version").addMarker("his").addMarker("equipHis").addMarker("writable")
                .setUnit("")
                .setKind("string")
                .setTz(tz)
                .build();
        hsApi.addPoint(appVersion);
    }
    
    
    public void updatePoints() {
        HashMap diagEquip = CCUHsApi.getInstance().read("equip and diag");
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
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
        boolean isAppRestart = spDefaultPrefs.getBoolean("APP_RESTART",false);
        if (isAppRestart){
            setDiagHisVal("app and restart",1);
        } else{
            setDiagHisVal("app and restart",0);
        }

        PackageManager pm = Globals.getInstance().getApplicationContext().getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0);
            String version = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1,pi.versionName.length() - 2);
            String prevVersion = CCUHsApi.getInstance().readDefaultStrVal("point and diag and app and version");
            String hisVersion = pi.versionName.substring(pi.versionName.lastIndexOf('_')+1);
            Log.d("DiagEquip","version ="+version+","+pi.versionName+","+pi.versionName.substring(pi.versionName.lastIndexOf('_')+1)+",prevVer="+prevVersion+prevVersion.equals( hisVersion));
            if(!prevVersion.equals( hisVersion)) {
                setDiagHisVal("app and version",Double.parseDouble(version));
                CCUHsApi.getInstance().writeDefaultVal("point and diag and app and version", hisVersion);
            }

        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void setDiagHisVal(String tag, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and diag and "+tag, val);
    }
}
