package a75f.io.logic.util;

import android.content.Context;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

public class PreferenceUtil {
    private static final String AIRFLOW_SAMPLE_WAIT_TIME_MIGRATION = "airflowSampleWaitTimeMigration";
    private static final String HYPERSTAT_AIR_TAG_MIGRATION = "hyperstatAirTagMigration";
    private static final String STANDALONE_HEATING_OFFSET = "standaloneHeatingOffset";
    private static final String STANDALONE_COOLING_AIRFLOW_TEMP_LOWER_OFFSET = "standaloneCoolingAirflowTempLowerOffset";
    private static final String STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME = "standaloneAirflowSampleWaitTime";
    private static final String BACKFILL_DURATION_MIGRATION = "backFillDuration";
    private static Context context;
    private static String PRESSURE_UNIT_MIGRATION = "pressureUnitMigration";
    private static final String SMART_NODE_MIGRATION ="smartNodeMigration";
    private static final String TRUE_CFM_VAV_MIGRATION="trueCfmVavMigration";
    private static final String AIRFLOW_UNIT_MIGRATION="airflowUnitMigration";
    private static final String TIMER_COUNTER_MIGRATION="stageUpTimerCounterTimerMigration";
    private static final String REHEAT_ZONE_TO_DAT_MIN = "reheatZoneToDATMinMigration";
    private static final String RELAY_DEACTIVATION_MIGRATION = "relayDeactivationMigration";
    private static final String MAX_CFM_COOLING_MIGRATION = "maxCFMCoolingMigration";
    private static final String MIN_CFM_COOLING_MIGRATION = "minCFMCoolingMigration";
    private static final String TRUE_CFM_DAB_MIGRATION="trueCfmDabMigration";
    private static final String ADDED_UNIT_TO_TUNERS ="unitAddedToTuners";
    private static final String REMOVED_DUPLICATE_ALERTS = "removedDuplicateAlerts";
    private static final String ENABLE_ZONE_SCHEDULE_MIGRATION = "enableZoneScheduleMigration";
    private static final String CLEAN_UP_DUPLICATE_ZONE_SCHEDULE = "cleanUpDuplicateZoneSchedule";
    private static final String DAMPER_FEEDBACK_MIGRATION = "damperFeedbackMigration";
    private static final String VOC_PM2P5_MIGRATION = "VovPm2p5Migration";
    private static final String DIAG_POINTS_MIGRATION = "diagPointsMigration";
    private static final String SCHEDULE_REFACTOR_MIGRATION = "scheduleRefactorMigration";
    private static final String SCHEDULE_REF_FOR_ZONE_MIGRATION = "scheduleRefsForZoneReMigration";
    private static final String VOC_PM2P5_MIGRATION_V1 = "VovPm2p5Migration_V1";

    private static final String UPDATE_SCHEDULE_REFS = "reUpdateScheduleRef";
    private static final String SITE_NAME_MIGRATION = "siteNameMigration";
    private static final String STAGE_UP_TIMER_FOR_DAB = "stageUpTimerForDab";
    private static final String TI_UPDATE = "updateTIThermister";
    private static final String UPDATE_SCHEDULE_TYPE = "reUpdateSchedulesTypes";


    private static final String AUTOAWAY_AUTOFORCEOCCUPUED_POINTS_MIGRATION = "RerunOccupancyPointsMigration";
    private static final String DCWB_POINTS_MIGRATION = "DCWBPointsMigration";
    private static final String SMART_STAT_POINTS_MIGRATION = "smartStatPointsMigration";
    private static final String BPOS_TO_OTN_MIGRATION = "bposToOtnMigration";
    private static final String AUTOAWAYSETBACK = "autoAwaySetBackTuner";
    private static final String HYPERSTAT_DEVICE_DISPLAY_CONFIGURATON_POINTSMIGRATION = "HyperStatDeviceDisplayConfigurationPointsMigration";
    private static final String HYPERSTAT_CPU_TAG_MIGRATION = "HyperStatCpuTagMigration";
    private static final String AUTOAWAY_SETBACK_CPU = "autoAwaySetBackTunerCPU";
    private static final String VAV_DISCHARGE_TUNER_MIGRATION = "vavDischargeTunersMigration";
    private static final String AUTO_COMMISSIONING_MIGRATION = "autoCommissioningMigration";
    private static final String SMART_NODE_DAMPER_MIGRATION = "SmartNodeDamperMigration";
    private static final String FREE_INTERNAL_DISK_STORAGE_MIGRATION = "freeInternalDiskStorageMigration";
    private static final String STATIC_SP_TRIM_MIGRATION = "staticPressureSPTrimMigration";
    private static final String VRV_AUTO_AWAY_AUTO_FORCED_MIGRATION = "autoAwayAutoForcedMigration";

    private static final String BUILDING_BREACH_MIGRATION = "buildingLimitsBreachedOccupancy";

    private static final String SAFE_MODE_DIAG = "safeModeDiag";

    private static final String DAB_REHEAT_SUPPORT = "dabReheatSupport";
    private static final String SSE_FAN_STAGE_MIGRATION = "sseFanStageMigration";

    private static final String SINGLE_AND_DUAL_TEMP_SUPPORT="singleAndDualTempSupport";
    private static final String REMOVE_CORRUPTED_NAMED_SCHEDULE = "removeCorruptedNamedSchedule";
    private static final String TI_PROFILE_MIGRATION = "ti_profile_migration";
    private static final String OCCUPANCY_MODE_POINT_MIGRATION = "occupancy_mode_point_migration";

    private static final String DAB_REHEAT_STAGE2_FIX_MIGRATION = "dabReheatStage2FixMigration";

    private static final String TAG_MINOR_MIGRATION = "MinorTagCorrectionMigration";
    private static final String OTA_STATUS_MIGRATION = "OtaStatusMigration";
    private static final String CCUREF_TAG_MIGRATION = "ccuRefTagMigration";
    private static final String LAST_TIME_TOKEN = "lastTimeToken";
    private static final String SCHEDULES_MIGRATION = "schedulesMigration";

    private static final String AUTO_FORCED_TAG_CORRECTION_VRV_MIGRATION ="Auto_forced_tag_correction_vrv_migration";

    private static final String KIND_CORRECTION ="Kind_Correction";

    private static final String REMOTE_DUP_COOLING_LOCKOUT_TUNER = "removeDupCoolingLockoutTuner";
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
        return sharedPreferences.getBoolean("firmwareVersionPointMigrationWithStringKind",false);
    }

    public static void setFirmwareVersionPointMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firmwareVersionPointMigrationWithStringKind", isMigrated);
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
    public static boolean isTimerCounterAndCFMCoolingMigrationDone() {
        return getBooleanPreference(TIMER_COUNTER_MIGRATION) || getBooleanPreference(MAX_CFM_COOLING_MIGRATION) || getBooleanPreference(MIN_CFM_COOLING_MIGRATION);
    }
    public static boolean isRelayDeactivationAndReheatZoneToDATMigrationDone() {
        return getBooleanPreference(RELAY_DEACTIVATION_MIGRATION) || getBooleanPreference(REHEAT_ZONE_TO_DAT_MIN);
    }
    public static void setRelayDeactivationAndReheatZoneToDATMinMigrationDone() {
        setBooleanPreference(RELAY_DEACTIVATION_MIGRATION, true);
        setBooleanPreference(REHEAT_ZONE_TO_DAT_MIN,true);
    }
    public static void setAirflowVolumeUnitMigrationDone() {
        setBooleanPreference(AIRFLOW_UNIT_MIGRATION, true);
    }
    public static void setTimerCounterAndCFMCoolingMigrationDone() {
        setBooleanPreference(TIMER_COUNTER_MIGRATION, true);
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
    public static void setSiteNameEquipMigration() {
        setBooleanPreference(SITE_NAME_MIGRATION, true);
    }
    public static boolean getSiteNameEquipMigration() {
        return getBooleanPreference(SITE_NAME_MIGRATION);
    }

    public static void setSmartNodeMigration() {
        setBooleanPreference(SMART_NODE_MIGRATION, true);
    }

    public static boolean getScheduleRefactorMigration() {
        return getBooleanPreference(SCHEDULE_REFACTOR_MIGRATION);
    }

    public static void setScheduleRefactorMigration() {
        setBooleanPreference(SCHEDULE_REFACTOR_MIGRATION, true);

    }

    public static void setNewOccupancy() {
        setBooleanPreference(AUTOAWAY_AUTOFORCEOCCUPUED_POINTS_MIGRATION, true);
    }
    public static boolean getNewOccupancy() {
        return getBooleanPreference(AUTOAWAY_AUTOFORCEOCCUPUED_POINTS_MIGRATION);
    }

    public static void setScheduleRefForZoneMigration() {
        setBooleanPreference(SCHEDULE_REF_FOR_ZONE_MIGRATION, true);
    }
    public static boolean getScheduleRefForZoneMigration() {
        return getBooleanPreference(SCHEDULE_REF_FOR_ZONE_MIGRATION);
    }

    public static boolean getScheduleRefUpdateMigration() {
        return getBooleanPreference(UPDATE_SCHEDULE_REFS);
    }

    public static void setScheduleRefUpdateMigration() {
        setBooleanPreference(UPDATE_SCHEDULE_REFS, true);
    }

    public static void setVocPm2p5MigrationV1() {
        setBooleanPreference(VOC_PM2P5_MIGRATION_V1, true);
    }
    public static boolean getVocPm2p5MigrationV1() {
        return getBooleanPreference(VOC_PM2P5_MIGRATION_V1);
    }

    public static boolean getStageTimerForDABMigration() {
        return getBooleanPreference(STAGE_UP_TIMER_FOR_DAB);
    }

    public static void setStageTimerForDABMigration() {
        setBooleanPreference(STAGE_UP_TIMER_FOR_DAB, true);
    }


    public static void setTIUpdate() {
        setBooleanPreference(TI_UPDATE, true);
    }
    public static boolean getTIUpdate() {
        return getBooleanPreference(TI_UPDATE);
    }

    public static boolean getDabReheatSupportMigrationStatus() {
        return getBooleanPreference(DAB_REHEAT_SUPPORT);
    }
    public static void setDabReheatSupportMigrationStatus() {
        setBooleanPreference(DAB_REHEAT_SUPPORT, true);
    }

    public static boolean getScheduleTypeUpdateMigration() {
        return getBooleanPreference(UPDATE_SCHEDULE_TYPE);
    }

    public static void setScheduleTypeUpdateMigration() {
        setBooleanPreference(UPDATE_SCHEDULE_TYPE, true);
    }

    public static void setDCWBPointsMigration() {
        setBooleanPreference(DCWB_POINTS_MIGRATION, true);
    }
    public static boolean getDCWBPointsMigration() {
        return getBooleanPreference(DCWB_POINTS_MIGRATION);
    }

    public static boolean getSmartStatPointsMigration() {
        return getBooleanPreference(SMART_STAT_POINTS_MIGRATION);
    }

    public static void setSmartStatPointsMigration() {
        setBooleanPreference(SMART_STAT_POINTS_MIGRATION, true);

    }

    public static boolean getBPOSToOTNMigration() {
        return getBooleanPreference(BPOS_TO_OTN_MIGRATION);
    }

    public static void setBPOSToOTNMigration() {
        setBooleanPreference(BPOS_TO_OTN_MIGRATION, true);

    }

    public static boolean getAutoAwaySetBackMigration() {
        return getBooleanPreference(AUTOAWAYSETBACK);
    }

    public static void setAutoAwaySetBackMigration() {
        setBooleanPreference(AUTOAWAYSETBACK, true);
    }

    public static boolean getHyperStatDeviceDisplayConfigurationPointsMigration() {
        return getBooleanPreference(HYPERSTAT_DEVICE_DISPLAY_CONFIGURATON_POINTSMIGRATION);
    }
    public static boolean getAutoAwaySetBackCpuMigration() {
        return getBooleanPreference(AUTOAWAY_SETBACK_CPU);
    }
    public static void setAutoAwaySetBackCpuMigration() {
        setBooleanPreference(AUTOAWAY_SETBACK_CPU, true);
    }

    public static void setHyperStatDeviceDisplayConfigurationPointsMigration() {
        setBooleanPreference(HYPERSTAT_DEVICE_DISPLAY_CONFIGURATON_POINTSMIGRATION, true);
    }
    public static boolean getVavDiscargeTunerMigration() {
        return getBooleanPreference(VAV_DISCHARGE_TUNER_MIGRATION);
    }

    public static void setVavDiscargeTunerMigration() {
        setBooleanPreference(VAV_DISCHARGE_TUNER_MIGRATION, true);
    }

    public static boolean getHyperStatCpuTagMigration() {
        return getBooleanPreference(HYPERSTAT_CPU_TAG_MIGRATION);
    }

    public static void setHyperStatCpuTagMigration() {
        setBooleanPreference(HYPERSTAT_CPU_TAG_MIGRATION, true);
     }

    public static boolean getAutoCommissioningMigration() {
        return getBooleanPreference(AUTO_COMMISSIONING_MIGRATION);
    }

    public static void setAutoCommissioningMigration() {
        setBooleanPreference(AUTO_COMMISSIONING_MIGRATION, true);

    }
    public static long getScheduledStopDatetime(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(key, 0);
    }

    public static void setScheduledStopDatetime(String key, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(key, value);
        editor.apply();
    }
    public static void setFreeInternalDiskStorageMigration() {
        setBooleanPreference(FREE_INTERNAL_DISK_STORAGE_MIGRATION, true);
    }
    public static boolean getAutoAwayAutoForcedPointMigration() {
        return getBooleanPreference(VRV_AUTO_AWAY_AUTO_FORCED_MIGRATION);
    }

    public static void setAutoAwayAutoForcedPointMigration() {
        setBooleanPreference(VRV_AUTO_AWAY_AUTO_FORCED_MIGRATION, true);
    }

    public static boolean getFreeInternalDiskStorageMigration() {
        return getBooleanPreference(FREE_INTERNAL_DISK_STORAGE_MIGRATION);
    }

    public static boolean getHyperStatCpuAirTagMigration() {
        return false;
    }

    public static void setHyperStatCpuAirTagMigration() {
        setBooleanPreference(HYPERSTAT_AIR_TAG_MIGRATION, true);
    }
    public static boolean getSmartNodeDamperMigration() {
        return getBooleanPreference(SMART_NODE_DAMPER_MIGRATION);
    }

    public static void setSmartNodeDamperMigration() {
        setBooleanPreference(SMART_NODE_DAMPER_MIGRATION,true);
    }

    public static boolean isZonesMigratedForSingleAndDualTempSupport() {
        return getBooleanPreference(SINGLE_AND_DUAL_TEMP_SUPPORT);
    }
    public static void setZonesMigratedForSingleAndDualTempSupport() {
        setBooleanPreference(SINGLE_AND_DUAL_TEMP_SUPPORT,true);
    }
    public static boolean getSSEFanStageMigration() {
        return getBooleanPreference(SSE_FAN_STAGE_MIGRATION);
    }

    public static void setSSEFanStageMigration() {
        setBooleanPreference(SSE_FAN_STAGE_MIGRATION, true);
    }
    public static boolean getAirflowSampleWaitTImeMigration() {
        return getBooleanPreference(AIRFLOW_SAMPLE_WAIT_TIME_MIGRATION);
    }

    public static void setAirflowSampleWaitTimeMigration() {
        setBooleanPreference(AIRFLOW_SAMPLE_WAIT_TIME_MIGRATION, true);
    }
    public static boolean getstaticPressureSpTrimMigration() {
        return getBooleanPreference(STATIC_SP_TRIM_MIGRATION);
    }

    public static void setStaticPressureSpTrimMigration() {
        setBooleanPreference(STATIC_SP_TRIM_MIGRATION, true);
    }

    public static boolean getOccupancyModePointMigration() {
        return getBooleanPreference(OCCUPANCY_MODE_POINT_MIGRATION);
    }

    public static void setOccupancyModePointMigration() {
        setBooleanPreference(OCCUPANCY_MODE_POINT_MIGRATION, true);
    }

    public static boolean getNewOccupancyMode() {
        return getBooleanPreference(BUILDING_BREACH_MIGRATION);
    }

    public static void setNewOccupancyMode() {
         setBooleanPreference(BUILDING_BREACH_MIGRATION,true);
    }

    public static boolean getCorruptedNamedScheduleRemoval() {
        return getBooleanPreference(REMOVE_CORRUPTED_NAMED_SCHEDULE);
     }
     public static void setCorruptedNamedScheduleRemoval() {
         setBooleanPreference(REMOVE_CORRUPTED_NAMED_SCHEDULE, true);
     }

    public static boolean getTiProfileMigration() {
        return getBooleanPreference(TI_PROFILE_MIGRATION);
    }

    public static void setTiProfileMigration() {
        setBooleanPreference(TI_PROFILE_MIGRATION, true);
    }

    public static boolean getDabReheatStage2Migration() {
        return getBooleanPreference(DAB_REHEAT_STAGE2_FIX_MIGRATION);
    }

    public static void setDabReheatStage2Migration() {
        setBooleanPreference(DAB_REHEAT_STAGE2_FIX_MIGRATION, true);
    }

    public static boolean getStandaloneHeatingOffsetMigration() {
        return getBooleanPreference(STANDALONE_HEATING_OFFSET);
    }

    public static void setStandaloneHeatingOffsetMigration() {
        setBooleanPreference(STANDALONE_HEATING_OFFSET, true);
    }
    public static boolean getMinorTagMigration() {
        return getBooleanPreference(TAG_MINOR_MIGRATION);
    }

    public static void setMinorTagMigration() {
        setBooleanPreference(TAG_MINOR_MIGRATION,true);
    }

    public static boolean getstandaloneCoolingAirflowTempLowerOffsetMigration() {
        return getBooleanPreference(STANDALONE_COOLING_AIRFLOW_TEMP_LOWER_OFFSET);
    }

    public static void setstandaloneCoolingAirflowTempLowerOffsetMigration() {
        setBooleanPreference(STANDALONE_COOLING_AIRFLOW_TEMP_LOWER_OFFSET, true);
    }

    public static boolean getStandaloneAirflowSampleWaitMigration() {
        return getBooleanPreference(STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME);
    }

    public static void setAirflowSampleWaitTimeUnitMigration() {
        setBooleanPreference(STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME, true);
    }

    public static boolean getSafeModeMigration() {
        return getBooleanPreference(SAFE_MODE_DIAG);
    }

    public static void setSafeModeMigration() {
        setBooleanPreference(SAFE_MODE_DIAG,true);
    }

    public static boolean getAutoForcedTagNameCorrectionMigration() {
        return getBooleanPreference(AUTO_FORCED_TAG_CORRECTION_VRV_MIGRATION);
    }

    public static void setAutoForcedTagNameCorrectionMigration() {
        setBooleanPreference(AUTO_FORCED_TAG_CORRECTION_VRV_MIGRATION, true);
    }

    public static boolean getOtaStatusMigration() {
        return getBooleanPreference(OTA_STATUS_MIGRATION);
    }

    public static void setOtaStatusMigration() {
        setBooleanPreference(OTA_STATUS_MIGRATION, true);
    }

    public static boolean getKindCorrectionMigration() {
        return getBooleanPreference(KIND_CORRECTION);
    }

    public static void setKindCorrectionMigration() {
        setBooleanPreference(KIND_CORRECTION, true);
    }

    public static boolean getCcuRefTagMigration() {
        return getBooleanPreference(CCUREF_TAG_MIGRATION);
    }

    public static void setCcuRefTagMigration(boolean status) {
        setBooleanPreference(CCUREF_TAG_MIGRATION, status);
    }

    private static void setLongPreference(String key, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static long getLastCCUUpdatedTime() {
        return getLongPreference(LAST_TIME_TOKEN);
    }

    public static void setLastCCUUpdatedTime(long lastTimeToken) {
        Log.i("CCU_READ_CHANGES", "setLastCCUUpdatedTime " + new Date(lastTimeToken));
        setLongPreference(LAST_TIME_TOKEN, lastTimeToken);
    }

    private static long getLongPreference(String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(key, 0);
    }

    public static void setScheduleMigration() {
        setBooleanPreference(SCHEDULES_MIGRATION, true);
    }
    public static boolean getScheduleMigration() {
        return getBooleanPreference(SCHEDULES_MIGRATION);
    }

    public static void setRemoveDupCoolingLockoutTuner() {
        setBooleanPreference(REMOTE_DUP_COOLING_LOCKOUT_TUNER, true);
    }
    public static boolean getRemoveDupCoolingLockoutTuner() {
        return getBooleanPreference(REMOTE_DUP_COOLING_LOCKOUT_TUNER);
    }
}