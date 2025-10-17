package a75f.io.logic.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Date;

import a75f.io.logger.CcuLog;
import a75f.io.logic.migration.mystatv2migration.MyStatV2Migration;

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
    private static final String UPDATE_CCU_IN_PROGRESS_IN_ABOUT_SCREEN = "updateCCUInProcessingInAboutScreen";
    private static final String INSTALL_CCU_IN_PROGRESS_IN_ABOUT_SCREEN = "installCCUInProcessingInAboutScreen";
    private static final String ENABLE_TEMPERATURE_TI_PORT= "enableTemperatureTIPort";
    private static final String HSS_OUTSIDE_DAMPER_MIN_OPEN = "hssOutsideDamperMinOpenMigration";
    private static final String LAST_USERINTENT_CONDITIONING_MODE = "lastUserIntentConditioningMode";
    public static final String FIRMWARE_VERSION_POINT_MIGRATION = "firmwareVersionRemotePointMigrationIssueFix";
    private static final String CLEAN_OTHER_CCU_ZONE_SCHEDULES = "removeOtherCcuZoneSchedules";
    private static final String DATA_MIGRATION_POP_UP = "dataMigrationPopUp";
    private static final String SR_MIGRATION_POINT = "srMigrationPoint";
    private static final String UPDATE_BACNET_ID_FOR_ROOM = "updateBacnetIdForRoom";
    private static final String CONNECTION_CHANGE_TIMESTAMP = "connectionChangeTimestamp";

    /**
      * Below preference key is being changed from "zoneEquipPointFloorRefRoomRefMigration" to "zoneEquipPointFloorRefRoomRefMigration_v2"
      * This is done to let the migration code run once for those points which were not corrected in the previous migration due to "@" prefix in the floorRef/roomRef.
      * In the future, we can remove the older key from the shared preference file if deemed necessary.
    **/
    public static final String ZONE_EQUIP_CONFIG_POINT_MIGRATION = "zoneEquipPointFloorRefRoomRefMigration_v2";
    public static final String CARRIER_POINT_MIGRATION_DAB_TO_VVT = "carrierPointDabToVvtMigration";
    private static final String IS_CCU_LAUNCHED = "isCcuLaunched";
    private static final String IS_NEW_EXTERNAL_AHU = "isNewExternalAhu";
    public static final String UPDATE_HIS_ITEMS = "WritingMissingHisWriteValues";
    private static final String IS_CCU_REBOOT_STARTED = "isCcuRebootedStarted";

    public static final String SINGLE_DUAL_MIGRATION = "singleDualMigration";

    public static final String VAV_REHEAT_RELAY_ACTIVATION_HYSTERESIS_VALUE_MIGRATION = "vavReheatRelayActivationHysteresisValueMigration";
    public static final String HS_MONITORING_GENERIC_FAULT_ENUM_MIGRATION = "hsMonitoringGenericFaultEnumMigration";

    public static final String LOCALLY_SYNC_SCHEDULE = "locallySyncedSchedules";
    public static final String TEMP_MODE_MIGRATION = "tempModeMigration";

    public static final String MODBUS_ENUM_CORRECTION = "modbusEnumCorrection";

    public static final String ACB_RELAY_LOGICAL_POINTS_MIGRATION = "acbRelayLogicalPointsMigration";
    public static final String RECOVER_HELIO_NODE_ACB_TUNERS_MIGRATION = "recoverHelioNodeACBTunersMigration";
    public static final String ACB_COND_SENSOR_MIGRATION = "acbCondensateSensorMigration";
    /**
     * Below preference key is being changed from "dmToDmCleanupMigration" to "dmToDmCleanupMigration_v2"
     * This is done to let the migration code run once for those points which were not corrected in the previous migration as part of the bug 31099.
     **/
    public static final String DM_TO_DM_CLEANUP_MIGRATION = "dmToDmCleanupMigration_v2";
    public static final String HSS_GATEWAY_REF_MIGRATION = "hssGatewayRefMigration";
    public static final String HIS_TAG_REMOVAL_FROM_NON_DM_DEVICES = "hisTagRemovalFromNonDmDevices";
    public static final String VAV_CFM_ON_EDGE_MIGRATION = "vavCfmOnEdgeMigration";
    public static final String DEAD_BAND_MIGRATION = "deadBandMigration";
    public static final String ZONE_CO2_MIGRATION = "ZONE_CO2_MIGRATION";
    public static final String MIGRATE_HIS_INTERPOLATE_FOR_DEVICE_POINTS = "migrateHisInterpolateForDeviceEntities";
    public static final String MIGRATE_ANALOG_INPUT_TYPE_FOR_VAV_DAB_DEVICE_POINT = "migrateAnalogInputTypeForVavOrDabDevicePointAndPointRef";
    public static final String DELETE_REDUNDANT_SETBACK_POINTS_FROM_HN_ACB_EQUIPS  = "deleteRedundantSetbackPointsFromHnAcbEquips";
    public static final String BACKFILL_DEFAULT_VALUE_MIGRATION = "backfillDefaultValueMigration";
    public static final String DAB_DAMPER_SIZE_MIGRATION = "dabDamperSizeMigration";
    public static final String BACNET_ID_MIGRATION = "bacnetIdAndTypeUpdateMigrationNew";
    private static final String INSTALL_TYPE = "INSTALL_TYPE";
    private static final String RESTORE_BYPASS_DAMPER_AFTER_REPLACE = "restoreBypassDamperAfterReplace";
    private static final String TITLE_24_REDUNDANT_POINT_MIGRATION = "title24ReduntPointMigration";
    private static final String LOCK_OUT_HIS_UPDATE = "lockoutHisUpdate";
    public static final String DAB_DAMPER_SIZE_MIGRATION2 = "dabDamperSizeMigration2";
    private static final String UPDTAE_LOCAL_BUILDING_TUNERS = "UPDTAE_LOCAL_BUILDING_TUNERS";
    private static final String MIGRATE_HYPERSTATSPLIT_FAN_MODE_CACHE = "migrateHyperStatSplitFanModeCache";
    private static final String BACNET_SETTING_POINT_DELETED = "bacnetSettingPointDeleted";
    private static final String OTA_STATUS_POINT_REMOVAL = "otaStatusPointRemoval";
    private static final String PRANGE_POINT_MIGRATION_FLAG = "prangePointMigrationFlag";
    private static final String CORRECT_RELAY2_POINTREF_SERIES_PARALLEL = "correctRelay2PointRefSeriesParallel";
    private static final String REMOVE_REDUNDANT_SSE_DEVICE_POINTS = "removeRedundantSseDevicePoints";
    private static final String NULL_ID_REMOVAL_STATUS = "nullIdRemovalStatus";
    private static final String UPDATE_HISINTERPOLATE_COV = "hisinterpolatecov";
    private static final String SIDE_APPS_UPDATE_STATUS ="sideAppsUpdateStatus";
    private static final String RESTORE_SOURCE_MODEL_TAGS_FOR_OAO = "restoreSourceModelTagsForOao";
    private static final String UPDATE_CORRUPTED_DATATYPE_POINTS = "updateCorruptedPoints";
    private static final String UNOCCUPIED_SETBACK_MAX_MIGRATION = "UNOCCUPIED_SETBACK_MAX_MIGRATION";
    private static final String PLC_POINTS_UPDATE_STATUS = "plcPointsUpdateStatus";
    private static final String OLD_PORT_ENABLED_MIGRATION_STATUS = "oldPortEnabledMigrationStatus";
    private static final String RELAY2_port_ENABLED_STATUS = "relay2PortEnabledStatus";
    private static final String PROFILE_TYPE_IN_CCU_CONFIG_STATUS = "profileTypeInCcuConfigStatus";
    private static final String DAB_EQUIP_POINTS_UPDATE = "dabEquipPointsUpdate";
    private static final String ADDING_VOC_SENSOR_POINT = "addingVocSensorPoint";
    private static final String MIGRATE_BACNET_NETWORK_INTERFACE = "migrateBacnetNetworkInterface";
    private static final String DEVICE_POINTS_MIGRATION_STATUS = "devicePointsMigrationStatus";
    private static final String ADDRESS_BAND_INIT_COMPLETED = "addressBandInit";
    private static final String MIGRATE_UPDATE_HSS_POINTS = "migrateUpdateHssPoints";
    private static final String MIGRATE_HSCPU_DATA_CORREPTION = "MIGRATE_HSCPU_DATA_CORREPTION";
    private static final String REMOVE_BUILDING_AND_SYSTEM_EQUIP_POINTS = "removeBuildingAndSystemEquipPoints";
    private static final String  DUAL_DUCT_SENSOR_POINT_REMOVE = "dualDuctSensorPointRemove";

    private static final String CONNECT_MODULE_OAO_POINTS = "connectModuleOaoPoint";
    private static final String EQUIP_SCHEDULESTATUS_FOR_ZONE_EXTERNAL_EQUIP = "equipScheduleStatusForZoneExternalEquip";

    private static final String NON_DM_POINTS_REMOVE_STATUS = "nonDmPointsRemoveStatus";
    private static final String FLOOR_REF_UPDATE_STATUS = "floorRefUpdateStatus";
    private static final String MODBUS_KVTAGS_DATA_TYPE_UPDATED = "modbusKVTagsDataTypeUpdated";
    private static final String  UPDATE_ENUM_VALUES_FOR_TERMINAL_SYSTEM_PROFILE = "updatingEnumValuesForTerminalSystemProfile";
    private static final String CLEAR_UNSYNCED_LIST = "clearUnSyncedLists";


    public static final String SELECTED_PROFILE_WITH_AHU = "selectedProfileWithAhu";

    private static final String MIGRATE_UPDATE_DAB_AND_VAV_POINTS = "migrateUpdateVavAndDabPoints";
    private static final String MIGRATE_UPDATE_BYPASS_DAMPER_POINTS = "migrateUpdateBypassDamperPoints";

    private static final String MIGRATE_COPY_MODBUS_POINTS = "copyModbusPoints";


    private static final String MIGRATE_DELETE_REDUNDANT_OAO_POINTS_BY_SYSTEM_EQUIP = "migrateDeleteRedundantOaoPointsBySystemEquip";
    private static final String HIS_WRITE_ACTIVATION_POINT = "hisWriteActivation";

    private static final String MIGRATE_VFD_FAN_MODE = "migrateVfdFanMode";
    private static final String UPDATE_MYSTAT_GATEWAY_REF_FLAG = "updateMystatGatewayRefFlag";
    private static final String IS_REBOOT_REQUIRED_AFTER_REPLACE = "isRebootRequiredAfterReplace";
    private static final String MIGRATE_MODULATING_PROFILE_NORMALIZATION = "migrateModulatingProfileNormalization";
    private static final String MIGRATE_STAGE1_DAB_REHEAT_MAPPING = "migrateStage1ReheatMapping";
    private static final String MIGRATION_SYSTEM_FLAG_CCU_REBOOT = "migrationSystemFlagCcuReboot";
    private static final String MIGRATION_BACNET_EQUIP = "migrationBacnetEquips";
    private static final String MIGRATION_ZONE_STATUS = "migrationZoneStatus";
    private static final String MIGRATION_CLEAR_INVALID_HIS_DATA = "migrationClearInvalidHisData";
    private static final String MIGRATE_PCN_TARGET_ERROR = "migratePcnTargetError";

    private static final String MIGRATE_SYSTEM_TUNER_SYNC = "migrateMissingSystemTunerValues";
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
    private static void setBooleanPreferenceByCommitting(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(key, value);
        editor.commit();
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

    public static int getIntPreference(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(key, 0);
    }
    public static void setIntPreference(String key, int value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(key, value);
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
        setBooleanPreferenceByCommitting(UPDATE_CCU_IN_PROGRESS, true);
    }
    public static void stopUpdateCCU() {
        setBooleanPreferenceByCommitting(UPDATE_CCU_IN_PROGRESS, false);
    }
    public static boolean isCCUInstalling() {
        return getBooleanPreference(INSTALL_CCU_IN_PROGRESS);
    }
    public static void installCCU() {
        setBooleanPreferenceByCommitting(INSTALL_CCU_IN_PROGRESS, true);
    }
    public static void installationCompleted() {
        setBooleanPreferenceByCommitting(INSTALL_CCU_IN_PROGRESS, false);
    }

    // Preference management for about screen
    public static boolean getUpdateCCUStatusInAboutScreen() {
        return getBooleanPreference(UPDATE_CCU_IN_PROGRESS_IN_ABOUT_SCREEN);
    }
    public static void  startUpdateCCUInAboutScreen() {
        setBooleanPreferenceByCommitting(UPDATE_CCU_IN_PROGRESS_IN_ABOUT_SCREEN, true);
    }
    public static void stopUpdateCCUInAboutScreen() {
        setBooleanPreferenceByCommitting(UPDATE_CCU_IN_PROGRESS_IN_ABOUT_SCREEN, false);
    }
    public static boolean isCCUInstallingInAboutScreen() {
        return getBooleanPreference(INSTALL_CCU_IN_PROGRESS_IN_ABOUT_SCREEN);
    }
    public static void installCCUInAboutScreen() {
        setBooleanPreferenceByCommitting(INSTALL_CCU_IN_PROGRESS_IN_ABOUT_SCREEN, true);
    }
    public static void installationCompletedInAboutScreen() {
        setBooleanPreferenceByCommitting(INSTALL_CCU_IN_PROGRESS_IN_ABOUT_SCREEN, false);
    }

    public static boolean getCleanUpOtherCcuZoneSchedules() {
       return getBooleanPreference(CLEAN_OTHER_CCU_ZONE_SCHEDULES);
    }

    public static void setCleanUpOtherCcuZoneSchedules() {
        setBooleanPreference(CLEAN_OTHER_CCU_ZONE_SCHEDULES, true);
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

    public static boolean areZonesLocallySynced() {
        return getBooleanPreference(LOCALLY_SYNC_SCHEDULE);
    }

    public static void setZonesLocallySynced() {
        setBooleanPreference(LOCALLY_SYNC_SCHEDULE, true);
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

    public static boolean getDeleteRedundantSetbackPointsFromHnAcbEquips() {
        return getBooleanPreference(DELETE_REDUNDANT_SETBACK_POINTS_FROM_HN_ACB_EQUIPS);
    }

    public static void setDeleteRedundantSetbackPointsFromHnAcbEquips() {
        setBooleanPreference(DELETE_REDUNDANT_SETBACK_POINTS_FROM_HN_ACB_EQUIPS, true);
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

    public static boolean getMigrateHisInterpolateForDevicePoints() {
        return getBooleanPreference(MIGRATE_HIS_INTERPOLATE_FOR_DEVICE_POINTS);
    }

    public static void setMigrateHisInterpolateForDevicePoints() {
        setBooleanPreference(MIGRATE_HIS_INTERPOLATE_FOR_DEVICE_POINTS, true);
    }

    public static boolean getMigrateAnalogInputTypeForVavOrDabDevicePoint() {
        return getBooleanPreference(MIGRATE_ANALOG_INPUT_TYPE_FOR_VAV_DAB_DEVICE_POINT);
    }

    public static void setMigrateAnalogInputTypeForVavOrDabDevicePoint() {
        setBooleanPreference(MIGRATE_ANALOG_INPUT_TYPE_FOR_VAV_DAB_DEVICE_POINT, true);
    }
    public static boolean isBackFillValueUpdateRequired() {
        return getBooleanPreference(BACKFILL_DEFAULT_VALUE_MIGRATION);
    }

    public static void setBackFillValueUpdateDone() {
        setBooleanPreference(BACKFILL_DEFAULT_VALUE_MIGRATION, true);
    }

    public static boolean getDamperSizeMigrationFlagStatus() {
        return getBooleanPreference(DAB_DAMPER_SIZE_MIGRATION);
    }

    public static void setDamperSizeMigrationFlagStatus() {
        setBooleanPreference(DAB_DAMPER_SIZE_MIGRATION, true);
    }

    public static boolean isBacnetIdMigrationDone() {
        return getBooleanPreference(BACNET_ID_MIGRATION);
    }

    public static void setBacnetIdMigrationDone() {
        setBooleanPreference(BACNET_ID_MIGRATION, true);
    }

    public static String getCcuInstallType() {
        return getStringPreference(INSTALL_TYPE);
    }

    public static boolean getRestoreBypassDamperAfterReplace() {
        return getBooleanPreference(RESTORE_BYPASS_DAMPER_AFTER_REPLACE);
    }

    public static void setRestoreBypassDamperAfterReplace() {
        setBooleanPreference(RESTORE_BYPASS_DAMPER_AFTER_REPLACE, true);
    }

    public static void setUpdateBacnetIdForRoom() {
        setBooleanPreference(UPDATE_BACNET_ID_FOR_ROOM, true);
    }

    public static boolean getUpdateBacnetIdForRoom() {
        return getBooleanPreference(UPDATE_BACNET_ID_FOR_ROOM);
    }

    public static boolean getTitle24RedundantPointMigrationStatus() {
        return getBooleanPreference(TITLE_24_REDUNDANT_POINT_MIGRATION);
    }

    public static void setTitle24ReduntPointMigrationStatus() {
        setBooleanPreference(TITLE_24_REDUNDANT_POINT_MIGRATION, true);
    }

    public static boolean getLockOutHisUpdate() {
        return getBooleanPreference(LOCK_OUT_HIS_UPDATE);
    }

    public static void setLockOutHisUpdate() {
        setBooleanPreference(LOCK_OUT_HIS_UPDATE, true);
    }

    public static boolean getBacnetSettingPointDeleted() {
        return getBooleanPreference(BACNET_SETTING_POINT_DELETED);
    }

    public static void setBacnetSettingPointDeleted() {
        setBooleanPreference(BACNET_SETTING_POINT_DELETED, true);
    }

    public static boolean getLocalBuildingTunersUpdate() {
        return getBooleanPreference(UPDTAE_LOCAL_BUILDING_TUNERS);
    }

    public static void setLocalBuildingTunersUpdate() {
        setBooleanPreference(UPDTAE_LOCAL_BUILDING_TUNERS, true);
    }
    public static boolean getMigrateHyperStatSplitFanModeCache() {
        return getBooleanPreference(MIGRATE_HYPERSTATSPLIT_FAN_MODE_CACHE);
    }
    public static void setMigrateHyperStatSplitFanModeCache() {
        setBooleanPreference(MIGRATE_HYPERSTATSPLIT_FAN_MODE_CACHE, true);
    }

    public static boolean getPrangePointMigrationFlag() {
        return getBooleanPreference(PRANGE_POINT_MIGRATION_FLAG);
    }

    public static void setPrangePointMigrationFlag() {
        setBooleanPreference(PRANGE_POINT_MIGRATION_FLAG, true);
    }

    public static boolean getDamperSizeMigration2FlagStatus() {
        return getBooleanPreference(DAB_DAMPER_SIZE_MIGRATION2);
    }

    public static void setDamperSizeMigration2FlagStatus() {
        setBooleanPreference(DAB_DAMPER_SIZE_MIGRATION2, true);
    }

    public static boolean getNonDmOtaPointDeletionStatus() {
        return getBooleanPreference(OTA_STATUS_POINT_REMOVAL);
    }

    public static void setNonDmOtaPointDeletionStatus() {
        setBooleanPreference(OTA_STATUS_POINT_REMOVAL, true);
    }

    public static boolean getCorrectRelay2PointRefSeriesParallel() {
        return getBooleanPreference(CORRECT_RELAY2_POINTREF_SERIES_PARALLEL);
    }

    public static void setCorrectRelay2PointRefSeriesParallel() {
        setBooleanPreference(CORRECT_RELAY2_POINTREF_SERIES_PARALLEL, true);
    }

    public static boolean getRemoveRedundantSseDevicePoints() {
        return getBooleanPreference(REMOVE_REDUNDANT_SSE_DEVICE_POINTS);
    }

    public static void setRemoveRedundantSseDevicePoints() {
        setBooleanPreference(REMOVE_REDUNDANT_SSE_DEVICE_POINTS, true);
    }

    public static boolean getNullIdRemovalStatus() {
        return getBooleanPreference(NULL_ID_REMOVAL_STATUS);
    }

    public static void setNullIdRemovalStatus() {
        setBooleanPreference(NULL_ID_REMOVAL_STATUS, true);
    }

    public static boolean getHisInterpolateCOV() {
        return getBooleanPreference(UPDATE_HISINTERPOLATE_COV);
    }

    public static void setHisInterpolateCOV() {
        setBooleanPreference(UPDATE_HISINTERPOLATE_COV, true);
    }

    public static boolean isSideAppsUpdateFinished() {
        return getBooleanPreference(SIDE_APPS_UPDATE_STATUS);
    }

    public static void setSideAppsUpdateFinished() {
        setBooleanPreference(SIDE_APPS_UPDATE_STATUS, true);
    }

    public static boolean getRestoreSourceModelTagsForOao() {
        return getBooleanPreference(RESTORE_SOURCE_MODEL_TAGS_FOR_OAO);
    }

    public static void setRestoreSourceModelTagsForOao() {
        setBooleanPreference(RESTORE_SOURCE_MODEL_TAGS_FOR_OAO, true);
    }

    public static boolean unoccupiedSetbackMaxUpdate() {
        return getBooleanPreference(UNOCCUPIED_SETBACK_MAX_MIGRATION);
    }

    public static void setUnoccupiedSetbackMaxUpdate() {
        setBooleanPreference(UNOCCUPIED_SETBACK_MAX_MIGRATION, true);
    }

    public static boolean getUpdatePointsFlagStatus() {
        return getBooleanPreference(UPDATE_CORRUPTED_DATATYPE_POINTS);
    }

    public static void setUpdatePointsFlagStatus() {
        setBooleanPreference(UPDATE_CORRUPTED_DATATYPE_POINTS, true);
    }

    public static boolean getPlcUpdatePointStatus() {
        return getBooleanPreference(PLC_POINTS_UPDATE_STATUS);
    }

    public static void setPlcUpdatePointStatus() {
        setBooleanPreference(PLC_POINTS_UPDATE_STATUS, true);
    }

    public static void setConnectionChangeTime(long lastTimeToken) {
        setLongPreference(CONNECTION_CHANGE_TIMESTAMP, lastTimeToken);
    }

    public static boolean getOldPortEnabledMigrationStatus() {
        return getBooleanPreference(OLD_PORT_ENABLED_MIGRATION_STATUS);
    }

    public static void setOldPortEnabledMigrationStatus() {
        setBooleanPreference(OLD_PORT_ENABLED_MIGRATION_STATUS, true);
    }

    public static boolean getRelay2PortEnabledStatus() {
        return getBooleanPreference(RELAY2_port_ENABLED_STATUS);
    }

    public static void setRelay2PortEnabledStatus() {
        setBooleanPreference(RELAY2_port_ENABLED_STATUS, true);
    }


    public static void setAddressBandInitCompleted() {
        setBooleanPreference(ADDRESS_BAND_INIT_COMPLETED, true);
    }

    public static boolean isAddressBandInitCompleted() {
        return getBooleanPreference(ADDRESS_BAND_INIT_COMPLETED);
    }
    public static boolean isProfileTypeCorrectedInCCUConfigEquip() {
        return getBooleanPreference(PROFILE_TYPE_IN_CCU_CONFIG_STATUS);
    }

    public static void setProfileTypeCorrectedInCCUConfigEquip() {
        setBooleanPreference(PROFILE_TYPE_IN_CCU_CONFIG_STATUS, true);
    }
    public static boolean getDabEquipPointsUpdate() {
        return getBooleanPreference(DAB_EQUIP_POINTS_UPDATE);
    }

    public static void setDabEquipPointsUpdate() {
        setBooleanPreference(DAB_EQUIP_POINTS_UPDATE, true);
    }

    public static boolean getVocSensorPointAdded() {
        return getBooleanPreference(ADDING_VOC_SENSOR_POINT);
    }

    public static void setVocSensorPointAdded() {
        setBooleanPreference(ADDING_VOC_SENSOR_POINT, true);
    }


    public static boolean getUpdateBacnetNetworkInterface() {
        return getBooleanPreference(MIGRATE_BACNET_NETWORK_INTERFACE);
    }

    public static void setUpdateBacnetNetworkInterface() {
        setBooleanPreference(MIGRATE_BACNET_NETWORK_INTERFACE, true);
    }
    public static boolean getRecoverCpuFromCorrecption() {
        return getBooleanPreference(MIGRATE_HSCPU_DATA_CORREPTION);
    }

    public static void setRecoverCpuFromCorrecption() {
        setBooleanPreference(MIGRATE_HSCPU_DATA_CORREPTION, true);
    }
    public static boolean getDevicePointsMigrationStatus() {
        return getBooleanPreference(DEVICE_POINTS_MIGRATION_STATUS);
    }

    public static void setDevicePointsMigrationStatus() {
        setBooleanPreference(DEVICE_POINTS_MIGRATION_STATUS, true);
    }


    public static boolean getMigrateHssPoints() {
        return getBooleanPreference(MIGRATE_UPDATE_HSS_POINTS);
    }

    public static void setMigrateHssPoints() {
        setBooleanPreference(MIGRATE_UPDATE_HSS_POINTS, true);
    }

    public static boolean isDuplicateBuildingAndSystemPointsAreRemoved() {
        return getBooleanPreference(REMOVE_BUILDING_AND_SYSTEM_EQUIP_POINTS);
    }
    public static void setDuplicateBuildingAndSystemPointsAreRemoved() {
        setBooleanPreference(REMOVE_BUILDING_AND_SYSTEM_EQUIP_POINTS, true);
    }
    public static void setVavAndDabEquipAnalog1InPointsMigrated() {
        setBooleanPreference(MIGRATE_UPDATE_DAB_AND_VAV_POINTS, true);
    }
    public static boolean isVavAndDabEquipAnalog1InPointsMigrationRequired() {
        return getBooleanPreference(MIGRATE_UPDATE_DAB_AND_VAV_POINTS);

    }
    public static void setBypassDamperEquipPointsMigrated() {
        setBooleanPreference(MIGRATE_UPDATE_BYPASS_DAMPER_POINTS, true);
    }
    public static boolean isBypassDamperEquipPointsMigrationRequired() {
        return getBooleanPreference(MIGRATE_UPDATE_BYPASS_DAMPER_POINTS);

    }

    public static String getSelectedProfileWithAhu() {
        return getStringPreference(SELECTED_PROFILE_WITH_AHU);
    }

    public static void setSelectedProfileWithAhu(String profile) {
        setStringPreference(SELECTED_PROFILE_WITH_AHU, profile);
    }
    public static boolean isDuplicateDualDuctSensorPointsAreRemoved() {
        return getBooleanPreference(DUAL_DUCT_SENSOR_POINT_REMOVE);
    }
    public static void SetDuplicateDualDuctSensorPointsAreRemoved() {
        setBooleanPreference(DUAL_DUCT_SENSOR_POINT_REMOVE, true);
    }

    public static boolean isConnectModuleOAOPointDeleted() {
        return getBooleanPreference(CONNECT_MODULE_OAO_POINTS);
    }

    public static void setConnectModuleOAOPointDeleted() {
        setBooleanPreference(CONNECT_MODULE_OAO_POINTS, true);
    }

    public static boolean nonDmPointRemoveStatus() {
        return getBooleanPreference(NON_DM_POINTS_REMOVE_STATUS);
    }

    public static void setNonDmPointRemoveStatus() {
        setBooleanPreference(NON_DM_POINTS_REMOVE_STATUS, true);
    }

    public static boolean getFloorRefUpdateStatus() {
        return getBooleanPreference(FLOOR_REF_UPDATE_STATUS);
    }

    public static void setFloorRefUpdateStatus() {
        setBooleanPreference(FLOOR_REF_UPDATE_STATUS, true);
    }

    public static boolean getModbusKvtagsDataTypeUpdated() {
        return getBooleanPreference(MODBUS_KVTAGS_DATA_TYPE_UPDATED);
    }

    public static void setModbusKvtagsDataTypeUpdate() {
        setBooleanPreference(MODBUS_KVTAGS_DATA_TYPE_UPDATED, true);
    }

    public static void setClearUnSyncedList() {
        setBooleanPreference(CLEAR_UNSYNCED_LIST, true);
    }

    public static boolean getClearUnSyncedList() {
        return getBooleanPreference(CLEAR_UNSYNCED_LIST);
    }

    public static boolean getMigrateDeleteRedundantOaoPointsBySystemEquip() {
        return getBooleanPreference(MIGRATE_DELETE_REDUNDANT_OAO_POINTS_BY_SYSTEM_EQUIP);
    }
    public static void setMigrateDeleteRedundantOaoPointsBySystemEquip() {
        setBooleanPreference(MIGRATE_DELETE_REDUNDANT_OAO_POINTS_BY_SYSTEM_EQUIP, true);
    }
    
    public static boolean getDRMigrationStatus() {
        return getBooleanPreference(HIS_WRITE_ACTIVATION_POINT);
    }
    public static void setDRMigrationStatus() {
        setBooleanPreference(HIS_WRITE_ACTIVATION_POINT, true);
    }

    public static boolean getMigrateVfdFanMode() {
        return getBooleanPreference(MIGRATE_VFD_FAN_MODE);
    }
    public static void setMigrateVfdFanMode() {
        setBooleanPreference(MIGRATE_VFD_FAN_MODE, true);
    }

    public static boolean getCopyModbusPoints() {
        return getBooleanPreference(MIGRATE_COPY_MODBUS_POINTS);
    }

    public static void setCopyModbusPoints() {
        setBooleanPreference(MIGRATE_COPY_MODBUS_POINTS, true);
    }

    public static boolean isEquipScheduleStatusForZoneExternalEquipDone() {
        return getBooleanPreference(EQUIP_SCHEDULESTATUS_FOR_ZONE_EXTERNAL_EQUIP);
    }

    public static void setEquipScheduleStatusForZoneExternalEquipDone() {
        setBooleanPreference(EQUIP_SCHEDULESTATUS_FOR_ZONE_EXTERNAL_EQUIP, true);
    }

    public static boolean getIsRebootRequiredAfterReplaceFlag() {
        return getBooleanPreference(IS_REBOOT_REQUIRED_AFTER_REPLACE);
    }

    public static void setIsRebootRequiredAfterReplaceFlag(boolean isRequired) {
        setBooleanPreferenceByCommitting(IS_REBOOT_REQUIRED_AFTER_REPLACE, isRequired);
    }

    public static boolean getUpdateMystatGatewayRefFlag() {
        return getBooleanPreference(UPDATE_MYSTAT_GATEWAY_REF_FLAG);
    }

    public static void setUpdateMystatGatewayRefFlag() {
        setBooleanPreference(UPDATE_MYSTAT_GATEWAY_REF_FLAG, true);
    }
    public static boolean getUpdateRestartSystemFlag() {
        return getBooleanPreference(MIGRATION_SYSTEM_FLAG_CCU_REBOOT);
    }

    public static void setUpdateRestartSystemFlag() {
        setBooleanPreference(MIGRATION_SYSTEM_FLAG_CCU_REBOOT, true);
    }

    public static boolean getUpdateEnumValuesForTerminalProfile() {
        return getBooleanPreference(UPDATE_ENUM_VALUES_FOR_TERMINAL_SYSTEM_PROFILE);

    }
    public static void setUpdateEnumValuesForTerminalProfile() {
        setBooleanPreference(UPDATE_ENUM_VALUES_FOR_TERMINAL_SYSTEM_PROFILE, true);
    }

    public static boolean getMigrateModulatingProfileNormalization() {
        return getBooleanPreference(MIGRATE_MODULATING_PROFILE_NORMALIZATION);
    }
    public static void setMigrateModulatingProfileNormalization() {
        setBooleanPreference(MIGRATE_MODULATING_PROFILE_NORMALIZATION, true);
    }
    public static boolean getMigrateStage1DabReheatMapping() {
        return getBooleanPreference(MIGRATE_STAGE1_DAB_REHEAT_MAPPING);
    }
    public static void setMigrateStage1DabReheatMapping() {
        setBooleanPreference(MIGRATE_STAGE1_DAB_REHEAT_MAPPING, true);
    }

    public static boolean getBacnetEquipGatewayUpdation() {
        return getBooleanPreference(MIGRATION_BACNET_EQUIP);
    }
    public static void setBacnetEquipGatewayUpdation() {
        setBooleanPreference(MIGRATION_BACNET_EQUIP, true);
    }

    public static boolean getEquipScheduleStatusMigrationStatus() {
        return getBooleanPreference(MIGRATION_ZONE_STATUS);
    }

    public static void setEquipScheduleStatusMigrationStatus() {
        setBooleanPreference(MIGRATION_ZONE_STATUS, true);
    }

    public static boolean getMigrateSystemTunerSync() {
        return getBooleanPreference(MIGRATE_SYSTEM_TUNER_SYNC);
    }
    public static void setMigrateSystemTunerSync() {
        setBooleanPreference(MIGRATE_SYSTEM_TUNER_SYNC, true);
    }

    public static boolean getMigrationClearInvalidHisData() {
        return getBooleanPreference(MIGRATION_CLEAR_INVALID_HIS_DATA);
    }

    public static void setMigrationClearInvalidHisData() {
        setBooleanPreference(MIGRATION_CLEAR_INVALID_HIS_DATA, true);
    }

    public static boolean getMyStatV2Migration() {
        return getBooleanPreference(MyStatV2Migration.MYSTAT_V2_MIGRATION);
    }

    public static void setMyStatV2Migration() {
        setBooleanPreference(MyStatV2Migration.MYSTAT_V2_MIGRATION, true);
    }

    public static boolean getPLCTargetAndErrorRangeMigrationStatus() {
        return getBooleanPreference(MIGRATE_PCN_TARGET_ERROR);
    }

    public static void setPLCTargetAndErrorRangeMigrationStatus() {
        setBooleanPreference(MIGRATE_PCN_TARGET_ERROR, true);
    }
}