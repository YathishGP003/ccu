package a75f.io.logic.util;

import android.content.Context;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;
import android.util.Log;

public class PreferenceUtil {
    private static Context context;
    private static String REMOVED_DUPLICATE_ALERTS = "removedDuplicateAlerts";
    private static String ENABLE_ZONE_SCHEDULE_MIGRATION = "enableZoneScheduleMigration";
    private static String CLEAN_UP_DUPLICATE_ZONE_SCHEDULE = "cleanUpDuplicateZoneSchedule";
    
    public static void setContext(Context c) {
        context= c;
    }

    public static String getOTPGeneratedToAddOrReplaceCCU() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("OTPToAddOrReplaceCCU","");
    }

    public static void setOTPGeneratedToAddOrReplaceCCU(String otp) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("OTPToAddOrReplaceCCU", otp);
        editor.apply();
    }

    public static boolean isHeartbeatMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("heartbeatMigration",false);
    }

    public static void setHeartbeatMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("heartbeatMigration", isMigrated);
        editor.apply();
    }

    public static boolean isHeartbeatMigrationAsDiagDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("heartbeatMigrationAsDiagWithRssi",false);
    }

    public static void setHeartbeatMigrationAsDiagStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("heartbeatMigrationAsDiagWithRssi", isMigrated);
        editor.apply();
    }

    public static boolean isFirmwareVersionPointMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("firmwareVersionPointMigration",false);
    }

    public static void setFirmwareVersionPointMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firmwareVersionPointMigration", isMigrated);
        editor.apply();
    }

    public static boolean isOAODamperOpenPointsMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("OAODamperOpenPointsMigration",false);
    }

    public static void setOAODamperOpenPointsMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("OAODamperOpenPointsMigration", isMigrated);
        editor.apply();
    }

    public static boolean isHeartbeatTagMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("heartbeattag", "isHeartbeatTagMigrationDone return "+sharedPreferences.getBoolean("heartbeattagMigration",false));
        return sharedPreferences.getBoolean("heartbeattagMigration",false);
    }

    public static void setHeartbeatTagMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("heartbeattagMigration", isMigrated);
        editor.apply();
    }
    
    public static String getTunerVersion() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("tunerVersion","");
    }
    
    public static void setTunerVersion(String version) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("tunerVersion", version);
        editor.apply();
    }
    
    public static boolean isBposAhuRefMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("bposAhuRefMigration",false);
    }
    
    public static void setBposAhuRefMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("bposAhuRefMigration", isMigrated);
        editor.apply();
    }
    
    public static String getMigrationVersion() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("migrationVersion","");
    }
    
    public static void setMigrationVersion(String version) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("migrationVersion", version);
        editor.apply();
    }

    public static boolean areDuplicateAlertsRemoved() {
        return getBooleanPreference(REMOVED_DUPLICATE_ALERTS);
    }

    public static void removedDuplicateAlerts() {
        setBooleanPreference(REMOVED_DUPLICATE_ALERTS, true);
    }

    private static boolean getBooleanPreference(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
    }

    private static void setBooleanPreference(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
    
    public static boolean getEnableZoneScheduleMigration() {
        return getBooleanPreference(ENABLE_ZONE_SCHEDULE_MIGRATION);
    }
    
    public static void setEnableZoneScheduleMigration() {
        setBooleanPreference(ENABLE_ZONE_SCHEDULE_MIGRATION, true);
    }
    
    public static boolean getCleanUpDuplicateZoneSchedule() {
        return getBooleanPreference(CLEAN_UP_DUPLICATE_ZONE_SCHEDULE);
    }
    
    public static void setCleanUpDuplicateZoneSchedule() {
        setBooleanPreference(CLEAN_UP_DUPLICATE_ZONE_SCHEDULE, true);
    }

    public static boolean isIduPointsMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("iduMigration",false);
    }

    public static void setIduMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("iduMigration", isMigrated);
        editor.apply();
    }
}