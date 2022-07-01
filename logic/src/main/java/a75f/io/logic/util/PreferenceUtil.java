package a75f.io.logic.util;

import android.content.Context;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;
import android.util.Log;

public class PreferenceUtil {
    private static Context context;
    private static String PRESSURE_UNIT_MIGRATION="pressureUnitMigration";
    private static final String SMART_NODE_MIGRATION ="smartNodeMigration";
    private static final String TRUE_CFM_VAV_MIGRATION="trueCfmVavMigration";
    private static final String AIRFLOW_UNIT_MIGRATION="airflowUnitMigration";
    private static final String TRUE_CFM_DAB_MIGRATION="trueCfmDabMigration";
    private static final String ADDED_UNIT_TO_TUNERS ="unitAddedToTuners";
    private static final String REMOVED_DUPLICATE_ALERTS = "removedDuplicateAlerts";
    private static final String ENABLE_ZONE_SCHEDULE_MIGRATION = "enableZoneScheduleMigration";
    private static final String CLEAN_UP_DUPLICATE_ZONE_SCHEDULE = "cleanUpDuplicateZoneSchedule";
    private static final String DAMPER_FEEDBACK_MIGRATION = "damperFeedbackMigration";
    private static final String VOC_PM2P5_MIGRATION = "VovPm2p5Migration";
    private static final String DIAG_POINTS_MIGRATION = "diagPointsMigration";
    private static final String SMART_STAT_POINTS_MIGRATION = "smartStatPointsMigration";

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

    public static boolean isCCUHeartbeatMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("CCUHeartbeatMigrationWithHisInterpolate", false);
    }

    public static void setCCUHeartbeatMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("CCUHeartbeatMigrationWithHisInterpolate", isMigrated);
        editor.apply();
    }

    public static boolean isSenseAndPILoopAnalogPointDisMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("SenseAndPILoopAnalogPointDisMigrationDone", false);
    }

    public static void setSenseAndPILoopAnalogPointDisMigrationDone(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("SenseAndPILoopAnalogPointDisMigrationDone", isMigrated);
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
    public static boolean isPressureUnitMigrationDone() {
        return getBooleanPreference(PRESSURE_UNIT_MIGRATION);
    }
    public static void setPressureUnitMigrationDone() {
        setBooleanPreference(PRESSURE_UNIT_MIGRATION, true);
    }
    public static boolean isAirflowVolumeUnitMigrationDone() {
        return getBooleanPreference(AIRFLOW_UNIT_MIGRATION);
    }

    public static void setAirflowVolumeUnitMigrationDone() {
        setBooleanPreference(AIRFLOW_UNIT_MIGRATION, true);
    }
    public static boolean isTrueCFMVAVMigrationDone() {
        return getBooleanPreference(TRUE_CFM_VAV_MIGRATION);
    }
    public static void setTrueCFMVAVMigrationDone() {
        setBooleanPreference(TRUE_CFM_VAV_MIGRATION, true);
    }

    public static boolean getAddedUnitToTuners() {
        return getBooleanPreference(ADDED_UNIT_TO_TUNERS);
    }

    public static void setUnitAddedToTuners() {
        setBooleanPreference(ADDED_UNIT_TO_TUNERS, true);
    }

    public static boolean isTrueCFMDABMigrationDone() {
        return getBooleanPreference(TRUE_CFM_DAB_MIGRATION);
    }
    public static void setTrueCFMDABMigrationDone() {
        setBooleanPreference(TRUE_CFM_DAB_MIGRATION, true);
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

    public static boolean getSNMigration() {
        return getBooleanPreference(SMART_NODE_MIGRATION);
    }
    public static void setDamperFeedbackMigration() {
        setBooleanPreference(DAMPER_FEEDBACK_MIGRATION, true);
    }
    public static boolean getDamperFeedbackMigration() {
        return getBooleanPreference(DAMPER_FEEDBACK_MIGRATION);
    }
    public static void setVocPm2p5Migration() {
        setBooleanPreference(VOC_PM2P5_MIGRATION, true);
    }
    public static boolean getVocPm2p5Migration() {
        return getBooleanPreference(VOC_PM2P5_MIGRATION);
    }
    public static void setDiagEquipMigration() {
        setBooleanPreference(DIAG_POINTS_MIGRATION, true);
    }
    public static boolean getDiagEquipMigration() {
        return getBooleanPreference(DIAG_POINTS_MIGRATION);
    }

    public static void setSmartNodeMigration() {
        setBooleanPreference(SMART_NODE_MIGRATION, true);

    }

    public static boolean getSmartStatPointsMigration() {
        return getBooleanPreference(SMART_STAT_POINTS_MIGRATION);
    }

    public static void setSmartStatPointsMigration() {
        setBooleanPreference(SMART_STAT_POINTS_MIGRATION, true);

    }
}