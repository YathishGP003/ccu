package a75f.io.logic.util;

import android.content.Context;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;

import java.util.Date;

import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;

public class PreferenceUtil {
    private static Context context;
    private static final String SMART_NODE_MIGRATION ="smartNodeMigration";
    private static final String AUTOAWAYSETBACK = "autoAwaySetBackTuner";

    private static final String TRUE_CFM_PRESSURE_UNIT_TAG_MIGRATION = "TrueCfmPressureUnitTagMigration";
    private static final String AUTOAWAY_SETBACK_CPU = "autoAwaySetBackTunerCPU";
    private static final String CCUREF_TAG_MIGRATION = "ccuRefTagMigration1";
    private static final String CCUREF_TAG_MIGRATION_DIAG = "ccuRefTagMigrationDiag1";
    private static final String LAST_TIME_TOKEN = "lastTimeToken";
    private static final String SYNC_START_TIME = "syncStartTime";
    private static final String DATA_SYNC_PROCESSING = "dataSyncProcessing";
    private static final String UPDATE_CCU_IN_PROGRESS = "updateCCUInProcessing";
    private static final String INSTALL_CCU_IN_PROGRESS = "installCCUInProcessing";
    private static final String ENABLE_TEMPERATURE_TI_PORT= "enableTemperatureTIPort";
    private static final String HSS_OUTSIDE_DAMPER_MIN_OPEN = "hssOutsideDamperMinOpenMigration";
    private static final String LAST_USERINTENT_CONDITIONING_MODE = "lastUserIntentConditioningMode";
    public static final String FIRMWARE_VERSION_POINT_MIGRATION = "firmwareVersionRemotePointMigrationIssueFix";
    private static final String CLEAN_OTHER_CCU_ZONE_SCHEDULES = "removeOtherCcuZoneSchedules";
    private static final String DATA_MIGRATION_POP_UP = "dataMigrationPopUp";
    private static final String SR_MIGRATION_POINT = "srMigrationPoint";

    /**
      * Below preference key is being changed from "zoneEquipPointFloorRefRoomRefMigration" to "zoneEquipPointFloorRefRoomRefMigration_v2"
      * This is done to let the migration code run once for those points which were not corrected in the previous migration due to "@" prefix in the floorRef/roomRef.
      * In the future, we can remove the older key from the shared preference file if deemed necessary.
    **/
    public static final String ZONE_EQUIP_CONFIG_POINT_MIGRATION = "zoneEquipPointFloorRefRoomRefMigration_v2";
    public static final String TITLE_24_OAO_POINTS_MIGRATION = "title24OaoPointsMigration";
    public static final String TITLE_24_HSS_POINTS_MIGRATION = "title24HssPointsMigration";

    public static final String CARRIER_POINT_MIGRATION_DAB_TO_VVT = "carrierPointDabToVvtMigration";
    private static final String IS_CCU_LAUNCHED = "isCcuLaunched";
    private static final String IS_NEW_EXTERNAL_AHU = "isNewExternalAhu";
    public static final String UPDATE_HIS_ITEMS = "updateHisItems";
    private static final String IS_CCU_REBOOT_STARTED = "isCcuRebootedStarted";

    public static final String MODULATING_FANSPEED_MIGRATION = "modulatingFanSpeedMigration";
    public static final String SINGLE_DUAL_MIGRATION = "singleDualMigration";

    public static final String VAV_REHEAT_RELAY_ACTIVATION_HYSTERESIS_VALUE_MIGRATION = "vavReheatRelayActivationHysteresisValueMigration";
    public static final String HS_USER_INTENT_AND_WRITABLE_MARKER_POINTS_MIGRATION = "hsUserIntentAndWritableMarkerPointMigration";
    public static final String HS_TH_CONFIG_MIGRATION = "hsThConfigMigration";
    public static final String HS_MONITORING_GENERIC_FAULT_ENUM_MIGRATION = "hsMonitoringGenericFaultEnumMigration";

    public static final String LOCALLY_SYNC_SCHEDULE = "locallySyncedSchedules";
    public static final String TEMP_MODE_MIGRATION = "tempModeMigration";

    public static final String MODBUS_ENUM_CORRECTION = "modbusEnumCorrection";

    public static final String ACB_RELAY_LOGICAL_POINTS_MIGRATION = "acbRelayLogicalPointsMigration";
    public static final String RECOVER_HELIO_NODE_ACB_TUNERS_MIGRATION = "recoverHelioNodeACBTunersMigration";
    public static final String ACB_COND_SENSOR_MIGRATION = "acbCondensateSensorMigration";
    public static final String DM_TO_DM_CLEANUP_MIGRATION = "dmToDmCleanupMigration";
    public static final String HSS_GATEWAY_REF_MIGRATION = "hssGatewayRefMigration";
    public static final String HIS_TAG_REMOVAL_FROM_NON_DM_DEVICES = "hisTagRemovalFromNonDmDevices";
    public static final String VAV_CFM_ON_EDGE_MIGRATION = "vavCfmOnEdgeMigration";
    public static final String DEAD_BAND_MIGRATION = "deadBandMigration";
    public static final String ZONE_CO2_MIGRATION = "ZONE_CO2_MIGRATION";
    public static final String APP_VERSION_POINTS_MIGRATION = "appVersionPointsMigration";


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
        return sharedPreferences.getBoolean(FIRMWARE_VERSION_POINT_MIGRATION,false);
    }

    public static void updateMigrationStatus(String migrationName, boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(migrationName, isMigrated);
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
        CcuLog.d("heartbeattag", "isHeartbeatTagMigrationDone return "+sharedPreferences.getBoolean("heartbeattagMigration",false));
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

    private static boolean getBooleanPreference(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
    }

    private static void setBooleanPreference(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static String getStringPreference(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, "");
    }

    public static void setStringPreference(String key, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static StandaloneConditioningMode getLastUserIntentConditioningMode() {
        switch (getStringPreference(LAST_USERINTENT_CONDITIONING_MODE)) {
            case "OFF": return StandaloneConditioningMode.OFF;
            case "HEAT_ONLY": return StandaloneConditioningMode.HEAT_ONLY;
            case "COOL_ONLY": return StandaloneConditioningMode.COOL_ONLY;
            default: return StandaloneConditioningMode.AUTO;
        }
    }
    public static void setLastUserIntentConditioningMode(StandaloneConditioningMode mode) {
        String lastUserIntentConditioningModeValue;
        if (mode == StandaloneConditioningMode.OFF) {
            lastUserIntentConditioningModeValue = "OFF";
        } else if (mode == StandaloneConditioningMode.HEAT_ONLY) {
            lastUserIntentConditioningModeValue = "HEAT_ONLY";
        } else if (mode == StandaloneConditioningMode.COOL_ONLY) {
            lastUserIntentConditioningModeValue = "COOL_ONLY";
        } else {
            lastUserIntentConditioningModeValue = "AUTO";
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(LAST_USERINTENT_CONDITIONING_MODE, lastUserIntentConditioningModeValue);
        editor.apply();

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

    public static void setSmartNodeMigration() {
        setBooleanPreference(SMART_NODE_MIGRATION, true);
    }

    public static boolean getZoneEquipConfigPointMigration() {
        return getBooleanPreference(ZONE_EQUIP_CONFIG_POINT_MIGRATION);
    }

    public static void setAutoAwaySetBackMigration() {
        setBooleanPreference(AUTOAWAYSETBACK, true);
    }

    public static boolean getAutoAwaySetBackCpuMigration() {
        return getBooleanPreference(AUTOAWAY_SETBACK_CPU);
    }
    public static void setAutoAwaySetBackCpuMigration() {
        setBooleanPreference(AUTOAWAY_SETBACK_CPU, true);
    }

    public static boolean getTrueCfmPressureUnitTagMigration() {
        return getBooleanPreference(TRUE_CFM_PRESSURE_UNIT_TAG_MIGRATION);
    }

    public static void setTrueCfmPressureUnitTagMigration() {
        setBooleanPreference(TRUE_CFM_PRESSURE_UNIT_TAG_MIGRATION, true);
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


    public static boolean getCcuRefTagMigration() {
        return getBooleanPreference(CCUREF_TAG_MIGRATION);
    }

    public static void setCcuRefTagMigration(boolean status) {
        setBooleanPreference(CCUREF_TAG_MIGRATION, status);
    }

    public static boolean getCcuRefTagMigrationForDiag() {
        return getBooleanPreference(CCUREF_TAG_MIGRATION_DIAG);
    }

    public static void setCcuRefTagMigrationForDiag(boolean status) {
        setBooleanPreference(CCUREF_TAG_MIGRATION_DIAG, status);
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
        CcuLog.i("CCU_READ_CHANGES", "setLastCCUUpdatedTime " + new Date(lastTimeToken));
        setLongPreference(LAST_TIME_TOKEN, lastTimeToken);
    }

    private static long getLongPreference(String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(key, 0);
    }

    public static long getSyncStartTime() {
        return getLongPreference(SYNC_START_TIME);
    }
    public static void setSyncStartTime(long syncStartTime) {
        CcuLog.i("CCU_READ_CHANGES", "syncStartTime " + new Date(syncStartTime));
        setLongPreference(SYNC_START_TIME, syncStartTime);
    }

    public static void setDataSyncRunning() {
        setBooleanPreference(DATA_SYNC_PROCESSING, true);
    }
    public static void setDataSyncStopped() {
        setBooleanPreference(DATA_SYNC_PROCESSING, false);
    }
    public static boolean getDataSyncProcessing() {
        return getBooleanPreference(DATA_SYNC_PROCESSING);
    }
    public static boolean getUpdateCCUStatus() {
        return getBooleanPreference(UPDATE_CCU_IN_PROGRESS);
    }
    public static void  startUpdateCCU() {
        setBooleanPreference(UPDATE_CCU_IN_PROGRESS, true);
    }
    public static void stopUpdateCCU() {
        setBooleanPreference(UPDATE_CCU_IN_PROGRESS, false);
    }
    public static boolean isCCUInstalling() {
        return getBooleanPreference(INSTALL_CCU_IN_PROGRESS);
    }
    public static void installCCU() {
        setBooleanPreference(INSTALL_CCU_IN_PROGRESS, true);
    }
    public static void installationCompleted() {
        setBooleanPreference(INSTALL_CCU_IN_PROGRESS, false);
    }

    public static boolean getCleanUpOtherCcuZoneSchedules() {
       return getBooleanPreference(CLEAN_OTHER_CCU_ZONE_SCHEDULES);
    }

    public static void setCleanUpOtherCcuZoneSchedules() {
        setBooleanPreference(CLEAN_OTHER_CCU_ZONE_SCHEDULES, true);
    }
    public static boolean getTemperatureTIPortEnabled() {
        return getBooleanPreference(ENABLE_TEMPERATURE_TI_PORT);
    }
    public static void setTemperatureTIPortEnabled() {
        setBooleanPreference(ENABLE_TEMPERATURE_TI_PORT, true);
    }

    public static boolean isHSSOutsideDamperMinOpenMigrationDone() {
        return getBooleanPreference(HSS_OUTSIDE_DAMPER_MIN_OPEN);
    }
    public static void setHSSOutsideDamperMinOpenMigrationDone() {
        setBooleanPreference(HSS_OUTSIDE_DAMPER_MIN_OPEN, true);
    }

    public static boolean isDataMigrationPopUpClosed() {
        return getBooleanPreference(DATA_MIGRATION_POP_UP);
    }
    public static void setDataMigrationPopUpClosed() {
        setBooleanPreference(DATA_MIGRATION_POP_UP, true);
    }

    public static boolean isSRMigrationPointUpdated() {
        return getBooleanPreference(SR_MIGRATION_POINT);
    }
    public static void setSRMigrationPointUpdated() {
        setBooleanPreference(SR_MIGRATION_POINT, true);
    }
    public static void setZoneEquipConfigPointMigrationDone() {
        setBooleanPreference(ZONE_EQUIP_CONFIG_POINT_MIGRATION, true);
    }

    public static boolean isTitle24OaoPointsMigrationDone() {
        return getBooleanPreference(TITLE_24_OAO_POINTS_MIGRATION);
    }

    public static void setTitle24OaoPointsMigrationDone() {
        setBooleanPreference(TITLE_24_OAO_POINTS_MIGRATION, true);
    }

    public static boolean isTitle24HssPointsMigrationDone() {
        return getBooleanPreference(TITLE_24_HSS_POINTS_MIGRATION);
    }

    public static void setTitle24HssPointsMigrationDone() {
        setBooleanPreference(TITLE_24_HSS_POINTS_MIGRATION, true);
    }

    public static boolean getCarrierDabToVvtMigration() {
        return getBooleanPreference(CARRIER_POINT_MIGRATION_DAB_TO_VVT);
    }

    public static void setCarrierDabToVvtMigrationDone() {
        setBooleanPreference(CARRIER_POINT_MIGRATION_DAB_TO_VVT, true);
    }

    public static boolean getIsCcuLaunched() {
        return getBooleanPreference(IS_CCU_LAUNCHED);
    }

    public static void setIsCcuLaunched(Boolean condition) {
        setBooleanPreference(IS_CCU_LAUNCHED, condition);
    }

    public static boolean getIsNewExternalAhu() {
        return getBooleanPreference(IS_NEW_EXTERNAL_AHU);
    }

    public static void setIsNewExternalAhu(Boolean condition) {
        setBooleanPreference(IS_NEW_EXTERNAL_AHU, condition);
    }

    public static boolean getHisItemsUpdatedStatus() {
        return getBooleanPreference(UPDATE_HIS_ITEMS);
    }

    public static void setHisItemsUpdatedStatus() {
         setBooleanPreference(UPDATE_HIS_ITEMS, true);
    }

    public static boolean getModulatingFanSpeedMigrationStatus() {
        return getBooleanPreference(MODULATING_FANSPEED_MIGRATION);
    }

    public static boolean getSingleDualMigrationStatus() {
        return getBooleanPreference(SINGLE_DUAL_MIGRATION);
    }

    public static void setSingleDualMigrationStatus() {
        setBooleanPreference(SINGLE_DUAL_MIGRATION, true);
    }

    public static boolean getVavReheatRelayActivationHysteresisValueMigration() {
        return getBooleanPreference(VAV_REHEAT_RELAY_ACTIVATION_HYSTERESIS_VALUE_MIGRATION);
    }

    public static void setVavReheatRelayActivationHysteresisValueMigration() {
        setBooleanPreference(VAV_REHEAT_RELAY_ACTIVATION_HYSTERESIS_VALUE_MIGRATION, true);
    }

    public static boolean getHsUserIntentAndWritableMarkerPointsMigration() {
        return getBooleanPreference(HS_USER_INTENT_AND_WRITABLE_MARKER_POINTS_MIGRATION);
    }

    public static void setHsUserIntentAndWritableMarkerPointsMigration() {
        setBooleanPreference(HS_USER_INTENT_AND_WRITABLE_MARKER_POINTS_MIGRATION, true);
    }


    public static boolean areZonesLocallySynced() {
        return getBooleanPreference(LOCALLY_SYNC_SCHEDULE);
    }

    public static void setZonesLocallySynced() {
        setBooleanPreference(LOCALLY_SYNC_SCHEDULE, true);
    }

    public static boolean getHyperStatThermistorConfigMigration() {
        return getBooleanPreference(HS_TH_CONFIG_MIGRATION);
    }

    public static void setHyperStatThermistorConfigMigration() {
        setBooleanPreference(HS_TH_CONFIG_MIGRATION, true);
    }

    public static boolean getACBRelayLogicalPointsMigration() {
        return getBooleanPreference(ACB_RELAY_LOGICAL_POINTS_MIGRATION);
    }

    public static void setACBRelayLogicalPointsMigration() {
        setBooleanPreference(ACB_RELAY_LOGICAL_POINTS_MIGRATION, true);
    }

    public static boolean getRecoverHelioNodeACBTunersMigration() {
        return getBooleanPreference(RECOVER_HELIO_NODE_ACB_TUNERS_MIGRATION);
    }

    public static void setRecoverHelioNodeACBTunersMigration() {
        setBooleanPreference(RECOVER_HELIO_NODE_ACB_TUNERS_MIGRATION, true);
    }

    public static boolean getACBCondensateSensorMigration() {
        return getBooleanPreference(ACB_COND_SENSOR_MIGRATION);
    }

    public static void setACBCondensateSensorMigration() {
        setBooleanPreference(ACB_COND_SENSOR_MIGRATION, true);
    }

    public static boolean getHSMonitoringGenericFaultEnumMigration() {
        return getBooleanPreference(HS_MONITORING_GENERIC_FAULT_ENUM_MIGRATION);
    }

    public static boolean getDmToDmCleanupMigration() {
        return getBooleanPreference(DM_TO_DM_CLEANUP_MIGRATION);
    }

    public static void setDmToDmCleanupMigration() {
        setBooleanPreference(DM_TO_DM_CLEANUP_MIGRATION, true);
    }

    public static void setHSMonitoringGenericFaultEnumMigration() {
        setBooleanPreference(HS_MONITORING_GENERIC_FAULT_ENUM_MIGRATION, true);
    }

    public static void setTempModeMigrationNotRequired() {
        setBooleanPreference(TEMP_MODE_MIGRATION, true);
    }
    public static boolean isTempModeMigrationRequired() {
        return getBooleanPreference(TEMP_MODE_MIGRATION);
    }
    public static boolean isModbusEnumCorrectionRequired() {
        return getBooleanPreference(MODBUS_ENUM_CORRECTION);
    }

    public static void setModbusEnumCorrectionDone() {
        setBooleanPreference(MODBUS_ENUM_CORRECTION, true);
    }
    public static boolean isHyperStatSplitGatewayRefMigrationDone() {
        return getBooleanPreference(HSS_GATEWAY_REF_MIGRATION);
    }
    public static void setHyperStatSplitGatewayRefMigrationDone() {
        setBooleanPreference(HSS_GATEWAY_REF_MIGRATION, true);
    }
    public static boolean isHisTagRemovalFromNonDmDevicesDone() {
        return getBooleanPreference(HIS_TAG_REMOVAL_FROM_NON_DM_DEVICES);
    }
    public static void setHisTagRemovalFromNonDmDevicesDone() {
        setBooleanPreference(HIS_TAG_REMOVAL_FROM_NON_DM_DEVICES, true);
    }
    public static boolean getIsCcuRebootStarted() {
        return getBooleanPreference(IS_CCU_REBOOT_STARTED);
    }

    public static void setIsCcuRebootStarted(boolean isRebooted) {
        setBooleanPreference(IS_CCU_REBOOT_STARTED, isRebooted);
    }
    public static boolean isVavCfmOnEdgeMigrationDone() {
        return getBooleanPreference(VAV_CFM_ON_EDGE_MIGRATION);
    }
    public static void setVavCfmOnEdgeMigrationDone() {
        setBooleanPreference(VAV_CFM_ON_EDGE_MIGRATION, true);
    }

    public static boolean isDeadBandMigrationRequired() {
        return getBooleanPreference(DEAD_BAND_MIGRATION);
    }
    public static void setDeadBandMigrationNotRequired() {
        setBooleanPreference(DEAD_BAND_MIGRATION, true);
    }
    public static boolean getZoneCo2Migration() {
        return getBooleanPreference(ZONE_CO2_MIGRATION);
    }
    public static void setZoneCo2Migration() {
        setBooleanPreference(ZONE_CO2_MIGRATION, true);
    }

    public static boolean getAppVersionPointsMigration() {
        return getBooleanPreference(APP_VERSION_POINTS_MIGRATION);
    }

    public static void setAppVersionPointsMigration() {
        setBooleanPreference(APP_VERSION_POINTS_MIGRATION, true);
    }
}