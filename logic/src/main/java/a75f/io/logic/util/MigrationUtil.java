package a75f.io.logic.util;


import static a75f.io.logic.L.TAG_CCU_MIGRATION_UTIL;
import static a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD;
import static a75f.io.logic.migration.firmware.FirmwareVersionPointMigration.initFirmwareVersionPointMigration;
import static a75f.io.logic.migration.firmware.FirmwareVersionPointMigration.initRemoteFirmwareVersionPointMigration;
import static a75f.io.logic.util.PreferenceUtil.FIRMWARE_VERSION_POINT_MIGRATION;


import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.util.SchedulableMigrationKt;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.DefaultSchedules;

import a75f.io.logic.L;
import a75f.io.logic.autocommission.remoteSession.RemoteSessionStatus;
import a75f.io.logic.bo.building.ccu.RoomTempSensor;
import a75f.io.logic.bo.building.definitions.Consts;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.migration.VavAndAcbProfileMigration;
import a75f.io.logic.migration.hyperstat.CpuPointsMigration;
import a75f.io.logic.migration.hyperstat.MigratePointsUtil;
import a75f.io.logic.migration.title24.Title24Migration;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.util.ExecutorTask;

public class MigrationUtil {
    private static final String TAG = "MIGRATION_UTIL";
    /**
     * All the migration tasks needed to be run during an application version upgrade should be called from here.
     * This approach has a drawback the migration gets invoked when there is version downgrade
     * THis will be fixed by using longVersionCode after migrating to API30. (dev going in another branch)
     */
    public static void doMigrationTasksIfRequired() {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();


        if(!PreferenceUtil.getTrueCfmPressureUnitTagMigration()) {
            doTrueCfmPressureUnitTagMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTrueCfmPressureUnitTagMigration();
        }


        if (!PreferenceUtil.getCleanUpOtherCcuZoneSchedules()) {
            cleanupOtherCcuZoneSchedules(CCUHsApi.getInstance());
            PreferenceUtil.setCleanUpOtherCcuZoneSchedules();
        }

        if(!PreferenceUtil.getCcuRefTagMigration()){
            CcuLog.i(TAG, "ccuRef migration started");
            CCUUtils.updateCcuSpecificEntitiesWithCcuRef(CCUHsApi.getInstance(), false);
            PreferenceUtil.setCcuRefTagMigration(true);
            CcuLog.i(TAG, "ccuRef migration completed");
        }

        if(!PreferenceUtil.getCcuRefTagMigrationForDiag()){
            CcuLog.i(TAG, "ccuRef migration for diag and system equip started");
            ccuHsApi.addCCURefForDiagAndSystemEntities();
            PreferenceUtil.setCcuRefTagMigrationForDiag(true);
            CcuLog.i(TAG, "ccuRef migration for diag and system equip completed");
        }


        if (!PreferenceUtil.getTemperatureTIPortEnabled()) {
            enableTISensorPort(CCUHsApi.getInstance());
            PreferenceUtil.setTemperatureTIPortEnabled();
        }

        if (!PreferenceUtil.isHSSOutsideDamperMinOpenMigrationDone()) {
            doHSSOutsideDamperMinOpenMigration(CCUHsApi.getInstance());
            PreferenceUtil.setHSSOutsideDamperMinOpenMigrationDone();
        }
        if(!PreferenceUtil.getZoneEquipConfigPointMigration()){
            UpdateFloorRefRoomRefForConfigPoints(CCUHsApi.getInstance());
            PreferenceUtil.setZoneEquipConfigPointMigrationDone();
        }

        if(!PreferenceUtil.getHisItemsUpdatedStatus()){
            updateHisValue(CCUHsApi.getInstance());
        }

        if (!PreferenceUtil.isTitle24OaoPointsMigrationDone()) {
            Title24Migration.Companion.doTitle24OaoPointsMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTitle24OaoPointsMigrationDone();
        }
        if (!PreferenceUtil.isTitle24HssPointsMigrationDone()) {
            Title24Migration.Companion.doTitle24HsPointMigration(CCUHsApi.getInstance());
            Title24Migration.Companion.doTitle24HssPointsMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTitle24HssPointsMigrationDone();
        }

        if(!PreferenceUtil.areZonesLocallySynced()) {
            updateAllZoneSchedulesLocally(ccuHsApi);
            PreferenceUtil.setZonesLocallySynced();
        }

        if (!PreferenceUtil.getHsUserIntentAndWritableMarkerPointsMigration()) {
            migrateUserIntentMarker();
            PreferenceUtil.setHsUserIntentAndWritableMarkerPointsMigration();
        }

        boolean firmwarePointMigrationState = initFirmwareVersionPointMigration();
        removeWritableTagForFloor();
        migrateTIProfileEnum(CCUHsApi.getInstance());
        migrateSenseToMonitoring(ccuHsApi);
        if (!PreferenceUtil.getModulatingFanSpeedMigrationStatus()) {
            migrateHyperStatFanStagedEnum(CCUHsApi.getInstance());
        }

        if (!PreferenceUtil.getVavReheatRelayActivationHysteresisValueMigration()) {
            setVavReheatRelayActivationHysteresisDefaultValues(ccuHsApi);
            PreferenceUtil.setVavReheatRelayActivationHysteresisValueMigration();
        }

        if (!PreferenceUtil.getHyperStatThermistorConfigMigration()) {
            migrateHyperStatThermistorConfig(ccuHsApi);
            PreferenceUtil.setHyperStatThermistorConfigMigration();
        }

        if (!PreferenceUtil.getHSMonitoringGenericFaultEnumMigration()) {
            migrateHyperStatMonitoringGenericFaultEnum(ccuHsApi);
            PreferenceUtil.setHSMonitoringGenericFaultEnumMigration();
        }

        if (!PreferenceUtil.getACBCondensateSensorMigration()) {
            VavAndAcbProfileMigration.Companion.condensateSensorCleanupMigration(ccuHsApi);
            PreferenceUtil.setACBCondensateSensorMigration();
        }
        if (!PreferenceUtil.isHyperStatSplitGatewayRefMigrationDone()) {
            migrateHyperStatSplitGatewayRef(ccuHsApi);
            PreferenceUtil.setHyperStatSplitGatewayRefMigrationDone();
        }

        migrateAirFlowTunerPoints(ccuHsApi);
        migrateZoneScheduleTypeIfMissed(ccuHsApi);
        if(SchedulableMigrationKt.validateMigration()) {
            writeValuesToLevel17ForMissingScheduleAblePoints(ccuHsApi);
        }
        migrateRemoteAccess();
        correctPhysicalAndAnalogMappingForSSE(ccuHsApi);
        L.saveCCUState();
        boolean firmwareRemotePointMigrationState = initRemoteFirmwareVersionPointMigration();
        PreferenceUtil.updateMigrationStatus(FIRMWARE_VERSION_POINT_MIGRATION,
                (firmwarePointMigrationState && firmwareRemotePointMigrationState));
        if(BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD) && (!PreferenceUtil.getCarrierDabToVvtMigration())){
                updateDisForPointsDabToVvt(CCUHsApi.getInstance());
                PreferenceUtil.setCarrierDabToVvtMigrationDone();
        }
        if (!PreferenceUtil.getZoneCo2Migration()) {
            DomainNameMigrationKt.updateDomain("zoneCO2", DomainName.zoneCo2);
            PreferenceUtil.setZoneCo2Migration();
        }
        removeHisTagForEquipStatusMessage(ccuHsApi);
        SchedulableMigrationKt.deleteDuplication( ccuHsApi.readAllEntities("room"));
        createMissingScheduleAblePoints(ccuHsApi);
        deleteDuplicateLimitsifAny(ccuHsApi);
        cleanUpAndCreateZoneSchedules(ccuHsApi);
        syncZoneSchedulesFromLocal(ccuHsApi);

        ccuHsApi.scheduleSync();
    }

    private static void migrateHyperStatSplitGatewayRef(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> hsEquips = hayStack.readAllEntities("equip and hyperstatsplit");
        HashMap<Object, Object> sysEquipMap = hayStack.readEntity("system and equip and not modbus and not connectModule");
        if (sysEquipMap.containsKey("id") && sysEquipMap.get("id") != null) {
            hsEquips.forEach(equipMap -> {
                Equip hsEquip = new Equip.Builder().setHashMap(equipMap).build();
                if (hsEquip.getGatewayRef() != sysEquipMap.get("id").toString()) {
                    Equip updatedEquip = new Equip.Builder().setHashMap(equipMap).setGatewayRef(sysEquipMap.get("id").toString()).build();
                    hayStack.updateEquip(updatedEquip, updatedEquip.getId());
                }
            });
        }
    }

    private static void deleteDuplicateLimitsifAny(CCUHsApi ccuHsApi) {
        List<HashMap<Object, Object>> rooms = ccuHsApi.readAllEntities("room");
        rooms.forEach(room -> {
            SchedulableMigrationKt.deleteDuplicateLimits(room.get("id").toString());
        });
    }

    private static void createMissingScheduleAblePoints(CCUHsApi ccuHsApi) {
        CcuLog.i("CCU_SCHEDULABLE","createMissingScheduleAblePoints");
        List<HashMap<Object, Object>> rooms = ccuHsApi.readAllEntities("room");
        rooms.forEach(room -> {
            List<ArrayList<HashMap<Object, Object>>> scheduleAbleLimits = new ArrayList<>();
            scheduleAbleLimits.add(ccuHsApi.readAllEntities("schedulable and roomRef ==\""
                    + room.get("id").toString() +"\""));
            findMissingSchedulableLimits(ccuHsApi.readAllEntities("schedulable and roomRef ==\""
                    + room.get("id").toString() +"\""), room.get("id").toString(),room.get("floorRef").toString(), ccuHsApi);
        });
    }
    private static void findMissingSchedulableLimits(ArrayList<HashMap<Object,
            Object>> scheduleAbleLimits, String roomRef,String floorRef, CCUHsApi ccuHsApi) {
        CcuLog.i("CCU_SCHEDULABLE","findMissingSchedulableLimits roomRef "+roomRef);
        HashMap<Object, Object> equipMap = ccuHsApi.readEntity(
                "equip and roomRef == \""+ roomRef +"\"");
        if (notContainsSchedulableLimits(scheduleAbleLimits, "heating", "deadband")) {
            CcuLog.i("CCU_SCHEDULABLE","create Heating deadband");
            createSchedulableDeadband(roomRef, ccuHsApi, equipMap, "heating",floorRef);
        }
        if (notContainsSchedulableLimits(scheduleAbleLimits, "cooling", "deadband")) {
            CcuLog.i("CCU_SCHEDULABLE","create cooling deadband");
            createSchedulableDeadband(roomRef, ccuHsApi, equipMap, "cooling",floorRef);
        }
        if (notContainsSchedulableLimits(scheduleAbleLimits, "unoccupied", "setback")) {
            CcuLog.i("CCU_SCHEDULABLE","create unoccupied setback");
            createUnOccupiedSetBackPoint(roomRef, ccuHsApi, equipMap,floorRef);
        }
        if (notContainsSchedulableLimits(scheduleAbleLimits, "heating", "min")) {
            CcuLog.i("CCU_SCHEDULABLE","create Heating min");
            createHeatingLimitMinPoint(roomRef, ccuHsApi, equipMap,floorRef);
        }
        if (notContainsSchedulableLimits(scheduleAbleLimits, "heating", "max")) {
            CcuLog.i("CCU_SCHEDULABLE","create Heating max");
            createHeatingLimitMaxPoint(roomRef, ccuHsApi, equipMap,floorRef);
        }
        if (notContainsSchedulableLimits(scheduleAbleLimits, "cooling", "max")) {
            CcuLog.i("CCU_SCHEDULABLE","create cooling max");
            createCoolingLimitMaxPoint(roomRef, ccuHsApi, equipMap,floorRef);
        }
        if (notContainsSchedulableLimits(scheduleAbleLimits, "cooling", "min")) {
            CcuLog.i("CCU_SCHEDULABLE","create cooling min");
            createCoolingLimitMinPoint(roomRef, ccuHsApi, equipMap,floorRef);
        }
    }
    private static void createCoolingLimitMaxPoint(String roomRef, CCUHsApi ccuHsApi, HashMap<Object, Object> equipMap,
                                                   String floorRef) {
        Point coolingUserLimitMax = new Point.Builder()
                .setDisplayName(equipMap.get("dis").toString()+"-coolingUserLimitMax")
                .setSiteRef(equipMap.get("siteRef").toString())
                .setHisInterpolate("cov")
                .addMarker("schedulable").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                .setMinVal("50").setMaxVal("100").setIncrementVal("1").addMarker("cur")
                .setUnit("\u00B0F")
                .addMarker("zone").setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setTz(ccuHsApi.getTimeZone()).build();
        String coolingUserLimitMaxId = ccuHsApi.addPoint(coolingUserLimitMax);
        ccuHsApi.writePointForCcuUser(coolingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MAX, 0);
        ccuHsApi.writeHisValById(coolingUserLimitMaxId, TunerConstants.ZONE_COOLING_USERLIMIT_MAX);
        HashMap<Object, Object> buildingPoint = ccuHsApi.readEntity("schedulable and cooling and user " +
                "and limit and max and default");
        double pointVal =  HSUtil.getPriorityLevelVal(buildingPoint.get("id").toString(), 16);
        if (pointVal > 0)
            ccuHsApi.writePointForCcuUser(coolingUserLimitMaxId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                    pointVal, 0);
    }
    private static void createCoolingLimitMinPoint(String roomRef, CCUHsApi ccuHsApi, HashMap<Object, Object> equipMap,
                                                   String floorRef) {
        Point coolingUserLimitMin = new Point.Builder()
                .setDisplayName(equipMap.get("dis").toString()+"-coolingUserLimitMin")
                .setSiteRef(equipMap.get("siteRef").toString())
                .setHisInterpolate("cov")
                .addMarker("writable").addMarker("his").setMinVal("70").setMaxVal("77")
                .setIncrementVal("1").addMarker("schedulable").addMarker("cur")
                .addMarker("cooling").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                .setMinVal("50").setMaxVal("100").setIncrementVal("1")
                .setUnit("\u00B0F")
                .addMarker("zone").setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setTz(ccuHsApi.getTimeZone()).build();
        String coolingUserLimitMinId = ccuHsApi.addPoint(coolingUserLimitMin);
        ccuHsApi.writePointForCcuUser(coolingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MIN, 0);
        ccuHsApi.writeHisValById(coolingUserLimitMinId, TunerConstants.ZONE_COOLING_USERLIMIT_MIN);
        HashMap<Object, Object> buildingPoint = ccuHsApi.readEntity("schedulable and cooling and user " +
                "and limit and min and default");
        double pointVal =  HSUtil.getPriorityLevelVal(buildingPoint.get("id").toString(), 16);
        if (pointVal > 0)
            ccuHsApi.writePointForCcuUser(coolingUserLimitMinId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                    pointVal, 0);
    }
    private static void createHeatingLimitMaxPoint(String roomRef, CCUHsApi ccuHsApi,
                                                   HashMap<Object, Object> equipMap, String floorRef) {
        Point heatingUserLimitMax = new Point.Builder()
                .setDisplayName(equipMap.get("dis").toString()+"-heatingUserLimitMax")
                .setSiteRef(equipMap.get("siteRef").toString())
                .setHisInterpolate("cov")
                .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
                .addMarker("heating").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                .setMinVal("50").setMaxVal("100").setIncrementVal("1")
                .setUnit("\u00B0F")
                .addMarker("zone").setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setTz(ccuHsApi.getTimeZone()).build();
        String heatingUserLimitMaxId = ccuHsApi.addPoint(heatingUserLimitMax);
        ccuHsApi.writePointForCcuUser(heatingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MAX, 0);
        ccuHsApi.writeHisValById(heatingUserLimitMaxId, TunerConstants.ZONE_HEATING_USERLIMIT_MAX);
        HashMap<Object, Object> buildingPoint = ccuHsApi.readEntity("schedulable and heating and user " +
                "and limit and max and default");
        double pointVal = HSUtil.getPriorityLevelVal(buildingPoint.get("id").toString(), 16);
        if (pointVal > 0)
            ccuHsApi.writePointForCcuUser(heatingUserLimitMaxId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                    pointVal, 0);
    }
    private static void createHeatingLimitMinPoint(String roomRef, CCUHsApi ccuHsApi,
                                                   HashMap<Object, Object> equipMap,String floorRef) {
        Point heatingUserLimitMin = new Point.Builder()
                .setDisplayName(equipMap.get("dis").toString()+"-heatingUserLimitMin")
                .setSiteRef(equipMap.get("siteRef").toString())
                .setHisInterpolate("cov")
                .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
                .addMarker("heating").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                .setMinVal("50").setMaxVal("100").setIncrementVal("1")
                .setUnit("\u00B0F")
                .addMarker("zone").setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setTz(ccuHsApi.getTimeZone()).build();
        String heatingUserLimitMinId = ccuHsApi.addPoint(heatingUserLimitMin);
        ccuHsApi.writePointForCcuUser(heatingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MIN, 0);
        ccuHsApi.writeHisValById(heatingUserLimitMinId, TunerConstants.ZONE_HEATING_USERLIMIT_MIN);
        HashMap<Object, Object> buildingPoint = ccuHsApi.readEntity("schedulable and heating and user " +
                "and limit and min and default");
        double pointVal = HSUtil.getPriorityLevelVal(buildingPoint.get("id").toString(), 16);
        if (pointVal > 0)
            ccuHsApi.writePointForCcuUser(heatingUserLimitMinId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                    pointVal, 0);
    }
    private static void createUnOccupiedSetBackPoint(String roomRef, CCUHsApi ccuHsApi, HashMap<Object, Object> equipMap,
                                                     String floorRef) {
        Point unoccupiedZoneSetback = new Point.Builder()
                .setDisplayName(equipMap.get("dis").toString()+"-unoccupiedZoneSetback")
                .setSiteRef(equipMap.get("siteRef").toString())
                .setHisInterpolate("cov")
                .addMarker("schedulable").addMarker("writable").addMarker("his").addMarker("cur")
                .addMarker("unoccupied").addMarker("setback").addMarker("sp")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1")
                .setUnit("\u00B0F")
                .addMarker("zone").setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setTz(ccuHsApi.getTimeZone()).build();
        String unoccupiedZoneSetbackId = ccuHsApi.addPoint(unoccupiedZoneSetback);
        ccuHsApi.writePointForCcuUser(unoccupiedZoneSetbackId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_UNOCCUPIED_SETBACK, 0);
        ccuHsApi.writeHisValById(unoccupiedZoneSetbackId, TunerConstants.ZONE_UNOCCUPIED_SETBACK);
        HashMap<Object, Object> buildingPoint = ccuHsApi.readEntity("schedulable and unoccupied and setback " +
                "and default");
        double pointVal = HSUtil.getPriorityLevelVal(buildingPoint.get("id").toString(), 16);
        if (pointVal > 0)
            ccuHsApi.writePointForCcuUser(unoccupiedZoneSetbackId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                    pointVal , 0);
    }
    private static void createSchedulableDeadband(String roomRef, CCUHsApi ccuHsApi,
                                                  HashMap<Object, Object> equipMap, String deadBand, String floorRef) {
        CcuLog.i(TAG_CCU_MIGRATION_UTIL,"createSchedulableDeadband");
        Point deadBandPoint = new Point.Builder()
                .setDisplayName(equipMap.get("dis").toString()+deadBand+"Deadband")
                .setSiteRef(equipMap.get("siteRef").toString())
                .setHisInterpolate("cov")
                .addMarker("writable")
                .addMarker("his")
                .addMarker(deadBand).addMarker("deadband").addMarker("base")
                .addMarker("sp").addMarker("schedulable").addMarker("cur")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5")
                .setUnit("\u00B0F")
                .addMarker("zone").setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setTz(ccuHsApi.getTimeZone()).build();
        String deadBandId = CCUHsApi.getInstance().addPoint(deadBandPoint);
        ccuHsApi.writePointForCcuUser(deadBandId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.VAV_COOLING_DB, 0);
        ccuHsApi.writeHisValById(deadBandId, TunerConstants.VAV_COOLING_DB);
        HashMap<Object, Object> buildingPoint = ccuHsApi.readEntity("schedulable and "+
                deadBand + " and deadband and default");
        double pointVal = HSUtil.getPriorityLevelVal(buildingPoint.get("id").toString(), 16);
        if (pointVal > 0)
            ccuHsApi.writePointForCcuUser(deadBandId, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,
                    pointVal , 0);
    }
    private static boolean notContainsSchedulableLimits(ArrayList<HashMap<Object, Object>> listOfMaps,
                                                        String tag1, String tag2) {
        for (HashMap<Object, Object> map : listOfMaps) {
            if (map.containsKey(tag1) && map.containsKey(tag2)) {
                return false;
            }
        }
        return true;
    }

    private static void correctPhysicalAndAnalogMappingForSSE(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> sseEquips = ccuHsApi.readAllEntities("equip and sse");
        for (HashMap<Object, Object> equip : sseEquips) {
            HashMap<Object, Object> transformerPoint = ccuHsApi.readEntity("(transformer or" +
                    " transformer20 or transformer50) and equipRef == \"" + equip.get("id") + "\"");
            HashMap<Object, Object> dev = ccuHsApi.readEntity("device and equipRef == \"" + equip.get("id") + "\"");
            ArrayList<HashMap<Object, Object>> phyPoints = CCUHsApi.getInstance().readAllEntities("point and physical" +
                    " and sensor and deviceRef == \"" + dev.get("id") + "\"" +" and port == \""+Port.ANALOG_IN_ONE+"\"");
            for(HashMap<Object, Object> ha :phyPoints){
                if(ha.get("port").toString().equals((Port.ANALOG_IN_ONE).toString()) && transformerPoint.size() > 0){
                    if(!ha.get("pointRef").toString().equals(transformerPoint.get("id").toString())){
                        SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.get("group").toString()),
                                Port.ANALOG_IN_ONE.name(), transformerPoint.get("id").toString());
                    }
                }
            }
        }
    }


    private static void removeHisTagForEquipStatusMessage(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> hsEquips = ccuHsApi.readAllEntities("equip " +
                "and (hyperstat or hyperstatsplit)");
        for (HashMap<Object, Object> hsEquip : hsEquips) {
            String equipRef = hsEquip.get("id").toString();
            HashMap<Object, Object> equipStatusMessagePointMap = ccuHsApi.readEntity(
                    "message and status and his and equipRef == \"" + equipRef + "\"");
            Point equipStatusMessagePoint = new Point.Builder().setHashMap(equipStatusMessagePointMap).build();
            if(equipStatusMessagePoint.getMarkers().contains(Tags.HIS)){
                equipStatusMessagePoint.getMarkers().remove(Tags.HIS);
                ccuHsApi.updatePoint(equipStatusMessagePoint, equipStatusMessagePoint.getId());
            }
        }
    }

    private static void writeValuesToLevel17ForMissingScheduleAblePoints(CCUHsApi ccuHsApi) {
        List<HashMap<Object,Object>> rooms = ccuHsApi.readAllEntities("room");
        rooms.forEach(zoneMap -> {
            String roomRef = zoneMap.get("id").toString();
            HashMap<Object, Object> coolingUpperLimit = ccuHsApi.readEntity("schedulable and point" +
                    " and limit and max and cooling and user and roomRef == \""+roomRef+"\"");
            HashMap<Object, Object> heatingUpperLimit = ccuHsApi.readEntity("schedulable and point" +
                    " and limit and min and heating and user and roomRef == \""+roomRef+"\"");
            HashMap<Object, Object> coolingLowerLimit = ccuHsApi.readEntity("schedulable and point" +
                    " and limit and min and cooling and user and roomRef == \""+roomRef+"\"");
            HashMap<Object, Object> heatingLowerLimit = ccuHsApi.readEntity("schedulable and point" +
                    " and limit and max and heating and user and roomRef == \""+roomRef+"\"");
            HashMap<Object, Object> coolingDeadBand = ccuHsApi.readEntity("schedulable and cooling" +
                    " and deadband and roomRef == \""+roomRef+"\"");
            HashMap<Object, Object> heatingDeadBand = ccuHsApi.readEntity("schedulable and heating" +
                    " and deadband and roomRef == \""+roomRef+"\"");

            if (coolingUpperLimit != null && !coolingUpperLimit.isEmpty()) {
                if (HSUtil.getPriorityLevelVal(coolingUpperLimit.get("id").toString(), 17) == 0.0) {
                    ccuHsApi.writePointForCcuUser(coolingUpperLimit.get("id").toString(), TunerConstants.
                            SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MAX, 0);
                }
            }
            if (heatingUpperLimit != null && !heatingUpperLimit.isEmpty()) {
                if (HSUtil.getPriorityLevelVal(heatingUpperLimit.get("id").toString(), 17) == 0.0) {
                    ccuHsApi.writePointForCcuUser(heatingUpperLimit.get("id").toString(), TunerConstants.
                            SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MIN, 0);
                }
            }
            if (coolingLowerLimit != null && !coolingLowerLimit.isEmpty()) {
                if (HSUtil.getPriorityLevelVal(coolingLowerLimit.get("id").toString(), 17) == 0.0) {
                    ccuHsApi.writePointForCcuUser(coolingLowerLimit.get("id").toString(), TunerConstants.
                            SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MIN, 0);
                }
            }
            if (heatingLowerLimit != null && !heatingLowerLimit.isEmpty()) {
                if (HSUtil.getPriorityLevelVal(heatingLowerLimit.get("id").toString(), 17) == 0.0) {
                    ccuHsApi.writePointForCcuUser(heatingLowerLimit.get("id").toString(), TunerConstants.
                            SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MAX, 0);
                }
            }
            if (coolingDeadBand != null && !coolingDeadBand.isEmpty()) {
                if (HSUtil.getPriorityLevelVal(coolingDeadBand.get("id").toString(), 17) == 0.0) {
                    ccuHsApi.writePointForCcuUser(coolingDeadBand.get("id").toString(), TunerConstants.
                            SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.VAV_COOLING_DB, 0);
                }
            }
            if (heatingDeadBand != null && !heatingDeadBand.isEmpty()) {
                if (HSUtil.getPriorityLevelVal(heatingDeadBand.get("id").toString(), 17) == 0.0) {
                    ccuHsApi.writePointForCcuUser(heatingDeadBand.get("id").toString(), TunerConstants.
                            SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.VAV_HEATING_DB, 0);
                }
            }
        });
    }



    private static void migrateAirFlowTunerPoints(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> allSnTuners = ccuHsApi.readAllEntities("sn and tuner");
        allSnTuners.forEach(snTuner -> {
            String pointName = snTuner.get(Tags.DIS).toString();
            /* Remove "sn" tag and change display name from siteName-roomName-profile-nodeAddress-snCoolingAirflowTemp
             to siteName-roomName-profile-nodeAddress-coolingAirflowTemp*/
            int snIndex = pointName.indexOf("sn");
            String modifiedDisplayName = pointName.substring(0, snIndex) +
                    Character.toLowerCase(pointName.charAt(snIndex + 2)) +
                    pointName.substring(snIndex + 3);
            Point modifiedPoint  = new Point.Builder().setHashMap(snTuner).removeMarker("sn")
                    .setDisplayName(modifiedDisplayName).build();
            ccuHsApi.updatePoint(modifiedPoint, modifiedPoint.getId());
        });
    }

    private static void migrateTIProfileEnum(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> tiEquips = ccuHsApi.readAllEntities("equip and ti");
        if (!tiEquips.isEmpty()) {
            for (HashMap<Object, Object> equipMap : tiEquips) {
                Equip equip = new Equip.Builder().setHashMap(equipMap).build();
                HashMap<Object, Object> roomTemperatureTypePoint = ccuHsApi.readEntity("point and " +
                        "temp and ti and space and type and equipRef == \"" + equip.getId() + "\"");
                if (!roomTemperatureTypePoint.get("enum").toString().contains("Sensor Bus Temperature")) {
                    Point enumUpdatedRoomTempTypePoint = new Point.Builder().setHashMap(roomTemperatureTypePoint).build();
                    enumUpdatedRoomTempTypePoint.setEnums(RoomTempSensor.getEnumStringDefinition());
                    CCUHsApi.getInstance().updatePoint(enumUpdatedRoomTempTypePoint, enumUpdatedRoomTempTypePoint.getId());
                }
            }
        }
    }

    private static void migrateHyperStatFanStagedEnum(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> hsCpuEquips = ccuHsApi.readAllEntities("equip and hyperstat and cpu");
        if (!hsCpuEquips.isEmpty()) {
            for (HashMap<Object, Object> equipMap : hsCpuEquips) {
                Equip equip = new Equip.Builder().setHashMap(equipMap).build();
                fanSpeedLogicalPointMigration(equip, ccuHsApi);
                analogOutConfigPointsMigration(equip, ccuHsApi);
            }
        }
    }

    private static void setVavReheatRelayActivationHysteresisDefaultValues(CCUHsApi ccuHsApi) {
        // In 2.4 --> 2.5 migration, vavReheatRelayActivationHysteresis point was added for BuildingTuner and VAV terminal equips.
        // But the terminal equip migration ran first, so this tuner did not get a default value (since there was no Level 17 tuner yet).
        // This migration is included with the feature that starts using this tuner in algos, so there should be no operational issues from this.
        ArrayList<HashMap<Object, Object>> vavZoneHysteresisList = ccuHsApi.readAllEntities("point and tuner and zone and domainName == \"vavReheatRelayActivationHysteresis\"");
        if (!vavZoneHysteresisList.isEmpty()) {
            for (HashMap<Object, Object> vavZoneHysteresis : vavZoneHysteresisList) {
                ccuHsApi.writeDefaultTunerValById(vavZoneHysteresis.get("id").toString(), 10.0);
            }
        }
    }


    /**
     * A duplicate alert exists when an older, *unsynced* alert with the same title and equipId also exists.
     * This will only keep the oldest alert, while deleting the others.
     * */
    public static void removeDuplicateAlerts(AlertManager alertManager) { // Public so it can more easily be tested...
        // Log before delete
        List<Alert> unsyncedAlerts = alertManager.getUnsyncedAlerts();
        logUnsyncedAlerts(true, unsyncedAlerts);

        // Group the alerts by their "unique id" (title and equipId)
        Map<String, Map<String, List<Alert>>> groupedAlerts = unsyncedAlerts.stream()
                .collect(Collectors.groupingBy(Alert::getmTitle,
                                               Collectors.groupingBy(Alert::getSafeEquipId)));

        // If necessary, delete the duplicate alerts
        String logTag = "DURING Data Migration - Remove Duplicate Alerts";
        for (Map.Entry<String, Map<String, List<Alert>>> alertsByTitle : groupedAlerts.entrySet()) {
            for (Map.Entry<String, List<Alert>> alertsByEquip : alertsByTitle.getValue().entrySet()) {

                List<Alert> alerts = alertsByEquip.getValue();

                if (alerts.size() > 1) {
                    // Found duplicate alerts. Keep the oldest alert, but delete the others.
                    alerts.stream().sorted((a1, a2) -> ((Long) a2.getStartTime()).compareTo(a1.getStartTime())); // Sort by start time is ascending order
                    CcuLog.i(logTag, String.format("Duplicate alerts found. Count = %s | mTitle = '%s' | equipId = %s", alerts.size(), alertsByTitle.getKey(), alertsByEquip.getKey()));
                    CcuLog.i(logTag, "Keeping alert = " + alerts.get(0));
                    for (int i = 1; i < alerts.size(); i++) {
                        Alert alert = alerts.get(i);
                        CcuLog.i(logTag, "Deleting alert = " + alert);
                        alertManager.deleteAlert(alert);
                    }
                }
            }
        }

        // Log after delete
        unsyncedAlerts = alertManager.getUnsyncedAlerts();
        logUnsyncedAlerts(false, unsyncedAlerts);
    }

    private static void logUnsyncedAlerts(boolean isBeforeMigration, List<Alert> unsyncedAlerts) {
        String logTag = (isBeforeMigration ? "BEFORE" : "AFTER") + " Data Migration - Remove Duplicate Alerts";
        CcuLog.i(logTag, "Unsynced alert count = "  + unsyncedAlerts.size());
        for (Alert unsyncedAlert : unsyncedAlerts ) {
            CcuLog.i(logTag, unsyncedAlert.toString());
        }
    }


    private static void doTrueCfmPressureUnitTagMigration(CCUHsApi haystack){
        ArrayList<HashMap<Object, Object>> equips = haystack.readAllEntities("equip and (dab or vav) and zone");
        equips.forEach(equip -> {
            Equip actualEquip = new Equip.Builder().setHashMap(equip).build();

            ArrayList<HashMap<Object, Object>> pressureSensors = haystack.readAllEntities("pressure and sensor and equipRef ==\""+actualEquip.getId()+"\"");
            pressureSensors.forEach( pressureSensor -> {
                Point pressureSensorPoint = new Point.Builder().setHashMap(pressureSensor).setUnit(Consts.PRESSURE_UNIT).build();
                haystack.updatePoint(pressureSensorPoint, pressureSensor.get("id").toString());
            });
        });
    }


    private static void removeWritableTagForFloor() {
        ArrayList<HashMap<Object, Object>> floors = CCUHsApi.getInstance().readAllEntities("floor");
        floors.forEach(floorMap -> {
            Floor floor = new Floor.Builder().setHashMap(floorMap).build();
            if (floor.getMarkers().contains("writable")){
                removeWritableMarkerForFloor(floorMap);
            }
        });
    }

    private static void removeWritableMarkerForFloor(HashMap<Object, Object> floorMap) {
        Floor.Builder newFloor = new Floor.Builder().setHashMap(floorMap);
        newFloor.setMarkers(new ArrayList<>());
        Floor markerRemovedFloor = newFloor.build();
        CCUHsApi.getInstance().updateFloor(markerRemovedFloor, markerRemovedFloor.getId());
    }

    private static void migrateUserIntentMarker() {

        ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("hyperstat and equip and (cpu or pipe2 or hpu)");
        equips.forEach(objectObjectHashMap -> {

            if (objectObjectHashMap != null && objectObjectHashMap.containsKey(Tags.ID)) {
                HashMap<Object, Object> fanMode = CpuPointsMigration.Companion.readPoint(
                        "fan and mode", Objects.requireNonNull(objectObjectHashMap.get(Tags.ID)).toString());
                HashMap<Object, Object> conditioningMode = CpuPointsMigration.Companion.readPoint(
                        "conditioning and mode", Objects.requireNonNull(objectObjectHashMap.get(Tags.ID)).toString());
                HashMap<Object, Object> operatingMode = CCUHsApi.getInstance()
                        .readEntity("operating and mode and writable and equipRef== \"" + objectObjectHashMap.get("id") + "\"");
                HashMap<Object, Object> zoneOccupancy = CCUHsApi.getInstance()
                        .readEntity("zone and occupancy and not auto and not sensor and writable and equipRef== \"" + objectObjectHashMap.get("id") + "\"");
                HashMap<Object, Object> autoAwayOccupancyDetection = CCUHsApi.getInstance()
                        .readEntity("auto and away and occupancy and writable and equipRef== \"" + objectObjectHashMap.get("id") + "\"");

                if (!fanMode.isEmpty() && !fanMode.containsKey("userIntent")) {
                    MigratePointsUtil.Companion.updateMarkers(
                            fanMode,
                            new String[]{"userIntent"},
                            new String[]{},
                            null);
                }
                if (!conditioningMode.isEmpty() && !conditioningMode.containsKey("userIntent")) {
                    MigratePointsUtil.Companion.updateMarkers(
                            conditioningMode,
                            new String[]{"userIntent"},
                            new String[]{},
                            null);
                }
                if (!operatingMode.isEmpty()) {
                    MigratePointsUtil.Companion.updateMarkers(
                            operatingMode,
                            new String[]{},
                            new String[]{"writable"},
                            null);
                }
                if (!zoneOccupancy.isEmpty()) {
                    MigratePointsUtil.Companion.updateMarkers(
                            zoneOccupancy,
                            new String[]{},
                            new String[]{"writable"},
                            null);
                }
                if (!autoAwayOccupancyDetection.isEmpty()) {
                    MigratePointsUtil.Companion.updateMarkers(
                            autoAwayOccupancyDetection,
                            new String[]{},
                            new String[]{"writable"},
                            null);
                }
            }
        });
    }

    private static void migrateHyperStatThermistorConfig(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> hsEquips = hayStack.readAllEntities("equip and hyperstat and not monitoring");
        hsEquips.forEach(equip -> {
            if (equip.containsKey("id") && equip.get("id") != null) {

                // TH1 is the same for CPU, HPU, and 2-Pipe Profiles.
                // TH2 is different for the 2-Pipe Profile.

                HashMap<Object, Object> airflowEnabledPoint = hayStack.readEntity("config and air and discharge and temp and enabled and equipRef == \"" + equip.get("id") + "\"");

                boolean isTh1Enabled = false;
                if (airflowEnabledPoint.containsKey("id") && airflowEnabledPoint.get("id") != null) {
                    isTh1Enabled = hayStack.readDefaultValById(airflowEnabledPoint.get("id").toString()) > 0.0;
                    hayStack.deleteEntity(airflowEnabledPoint.get("id").toString());
                }

                Point th1EnabledPoint = new Point.Builder()
                        .setDisplayName(equip.get("dis").toString() + "-thIn1Enabled")
                        .setSiteRef(equip.get("siteRef").toString())
                        .setEquipRef(equip.get("id").toString())
                        .setRoomRef(equip.get("roomRef").toString())
                        .setFloorRef(equip.get("floorRef").toString())
                        .setTz(equip.get("tz").toString())
                        .setGroup(equip.get("group").toString())
                        .setEnums("false,true")
                        .addMarker("config")
                        .addMarker("writable")
                        .addMarker("zone")
                        .addMarker("input")
                        .addMarker("th1")
                        .addMarker("enabled").build();
                String th1EnabledPointId = hayStack.addPoint(th1EnabledPoint);
                hayStack.writeDefaultValById(th1EnabledPointId, isTh1Enabled ? 1.0 : 0.0);

                Point th1AssociationPoint = new Point.Builder()
                        .setDisplayName(equip.get("dis").toString() + "-thIn1Association")
                        .setSiteRef(equip.get("siteRef").toString())
                        .setEquipRef(equip.get("id").toString())
                        .setRoomRef(equip.get("roomRef").toString())
                        .setFloorRef(equip.get("floorRef").toString())
                        .setTz(equip.get("tz").toString())
                        .setGroup(equip.get("group").toString())
                        .setEnums("airflowTemperatureSensor,genericFaultNC,genericFaultNO")
                        .addMarker("config")
                        .addMarker("writable")
                        .addMarker("zone")
                        .addMarker("input")
                        .addMarker("th1")
                        .addMarker("association").build();
                String th1AssociationPointId = hayStack.addPoint(th1AssociationPoint);
                hayStack.writeDefaultValById(th1AssociationPointId, 0.0);

                boolean isTh2Enabled = false;
                String th2AssociationEnum;
                if (equip.containsKey("pipe2")) {
                    isTh2Enabled = true;

                    HashMap<Object, Object> swtEnabledPoint = hayStack.readEntity("config and supply and water and temp and enabled and equipRef == \"" + equip.get("id") + "\"");
                    if (swtEnabledPoint.containsKey("id") && swtEnabledPoint.get("id") != null) {
                        hayStack.deleteEntity(swtEnabledPoint.get("id").toString());
                    }

                    th2AssociationEnum = "supplyWaterTempSensor";
                } else {
                    HashMap<Object, Object> doorWindowEnabledPoint = hayStack.readEntity("config and window and temp and enabled and equipRef == \"" + equip.get("id") + "\"");
                    if (doorWindowEnabledPoint.containsKey("id") && doorWindowEnabledPoint.get("id") != null) {
                        isTh2Enabled = hayStack.readDefaultValById(doorWindowEnabledPoint.get("id").toString()) > 0.0;
                        hayStack.deleteEntity(doorWindowEnabledPoint.get("id").toString());
                    }

                    th2AssociationEnum = "doorWindowTempSensor,genericFaultNC,genericFaultNO";
                }

                Point th2EnabledPoint = new Point.Builder()
                        .setDisplayName(equip.get("dis").toString() + "-thIn2Enabled")
                        .setSiteRef(equip.get("siteRef").toString())
                        .setEquipRef(equip.get("id").toString())
                        .setRoomRef(equip.get("roomRef").toString())
                        .setFloorRef(equip.get("floorRef").toString())
                        .setTz(equip.get("tz").toString())
                        .setGroup(equip.get("group").toString())
                        .setEnums("false,true")
                        .addMarker("config")
                        .addMarker("writable")
                        .addMarker("zone")
                        .addMarker("input")
                        .addMarker("th2")
                        .addMarker("enabled").build();
                String th2EnabledPointId = hayStack.addPoint(th2EnabledPoint);
                hayStack.writeDefaultValById(th2EnabledPointId, isTh2Enabled ? 1.0 : 0.0);

                Point th2AssociationPoint = new Point.Builder()
                        .setDisplayName(equip.get("dis").toString() + "-thIn2Association")
                        .setSiteRef(equip.get("siteRef").toString())
                        .setEquipRef(equip.get("id").toString())
                        .setRoomRef(equip.get("roomRef").toString())
                        .setFloorRef(equip.get("floorRef").toString())
                        .setTz(equip.get("tz").toString())
                        .setGroup(equip.get("group").toString())
                        .setEnums(th2AssociationEnum)
                        .addMarker("config")
                        .addMarker("writable")
                        .addMarker("zone")
                        .addMarker("input")
                        .addMarker("th2")
                        .addMarker("association").build();
                String th2AssociationPointId = hayStack.addPoint(th2AssociationPoint);
                hayStack.writeDefaultValById(th2AssociationPointId, 0.0);

            }
        });

    }

    private static void migrateHyperStatMonitoringGenericFaultEnum(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> hsEquips = hayStack.readAllEntities("equip and hyperstat and monitoring");
        hsEquips.forEach(equip -> {
            ArrayList<HashMap<Object, Object>> faultPointsMap = hayStack.readAllEntities("point and generic and (normallyOpen or normallyClosed) and equipRef == \"" + equip.get("id") + "\"");
            faultPointsMap.forEach(faultPointMap -> {
                Point faultPoint = new Point.Builder().setHashMap(faultPointMap).setEnums("Normal,Fault").build();
                hayStack.updatePoint(faultPoint, faultPointMap.get("id").toString());
            });
        });
    }

    private static void analogOutConfigPointsMigration(Equip equip, CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> analogOutPoints = ccuHsApi.readAllEntities("point and " +
                "(analog1 or analog2 or analog3) and cpu and output and association and equipRef == \"" + equip.getId() + "\"");
        for (HashMap<Object, Object> analogOutPoint : analogOutPoints) {
            if (analogOutPoint.get("enum").toString().contains("fanspeed")) {
                Point enumUpdatedAnalogOutPoint = new Point.Builder().setHashMap(analogOutPoint)
                        .setEnums("cooling,modulatingFanSpeed,heating,dcvdamper,predefinedFanSpeed").build();
                CCUHsApi.getInstance().updatePoint(enumUpdatedAnalogOutPoint, enumUpdatedAnalogOutPoint.getId());
            }
        }
    }

    private static void fanSpeedLogicalPointMigration(Equip equip, CCUHsApi ccuHsApi) {

        HashMap<Object, Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteDis = siteMap.get("dis").toString();
        String equipDis = siteDis + "-hyperstatcpu-" + equip.getGroup();

        HashMap<Object, Object> fanSpeedPointMap = ccuHsApi.readEntity("point and " +
                "fan and run and speed and equipRef == \"" + equip.getId() + "\"");
        if (fanSpeedPointMap != null && !fanSpeedPointMap.isEmpty()) {
            Point fanSpeedPoint = new Point.Builder().setHashMap(fanSpeedPointMap).removeMarker("run")
                    .removeMarker("analog").removeMarker("output").addMarker("modulating").setGroup(equip.getGroup())
                    .addMarker("cpu").addMarker("cur").addMarker("standalone").setDisplayName(equipDis + "-modulatingFanSpeed")
                    .build();
            CCUHsApi.getInstance().updatePoint(fanSpeedPoint, fanSpeedPoint.getId());
        }
    }

    private static void migrateSenseToMonitoring(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> listOfSenseEquips = ccuHsApi.readAllEntities("sense and equip and not device");
        ArrayList<HashMap<Object, Object>> listOfSenseDevices = ccuHsApi.readAllEntities("sense and not equip and device");
        ArrayList<HashMap<Object, Object>> listOfSensePoints = ccuHsApi.readAllEntities("sense and not equip and not device");
        for (HashMap<Object, Object> sensePoint: listOfSensePoints) {
            String displayNameOfSensePoint = sensePoint.get(Tags.DIS).toString();
            String modifiedDisplayNameOfSensePoint = displayNameOfSensePoint.replace("SENSE", Tags.MONITORING);
            Point newSensePoint = new Point.Builder().setHashMap(sensePoint).addMarker(Tags.MONITORING)
                    .removeMarker(Tags.SENSE).setDisplayName(modifiedDisplayNameOfSensePoint).build();
            ccuHsApi.updatePoint(newSensePoint, newSensePoint.getId());
        }
        for(HashMap<Object, Object> senseEquipMap : listOfSenseEquips){
            String displayNameOfSenseEquip = senseEquipMap.get(Tags.DIS).toString();
            String modifiedDisplayNameOfSenseEquip = displayNameOfSenseEquip.replace("SENSE", Tags.MONITORING);
            Equip senseEquip = new Equip.Builder().setHashMap(senseEquipMap).setDisplayName(modifiedDisplayNameOfSenseEquip)
                    .addMarker(Tags.MONITORING).removeMarker(Tags.SENSE).setProfile("HYPERSTAT_MONITORING").build();
            ccuHsApi.updateEquip(senseEquip, senseEquip.getId());
        }
        for(HashMap<Object, Object> senseDeviceMap : listOfSenseDevices){
            String displayNameOfSenseDevice = senseDeviceMap.get(Tags.DIS).toString();
            String modifiedDisplayNameOfSenseDevice = displayNameOfSenseDevice.replace("SENSE", Tags.MONITORING);
            Device senseEquip = new Device.Builder().setHashMap(senseDeviceMap).setDisplayName(modifiedDisplayNameOfSenseDevice)
                    .addMarker(Tags.MONITORING).removeMarker(Tags.SENSE).build();
            ccuHsApi.updateDevice(senseEquip, senseEquip.getId());
        }
    }


    /**
     * There has been a bug in updateScheduleHandler that resulted in zoneSchedules from other CCUs gets saved in
     * all the CCUs when there is an updateSchedule message.
     * This had no functional impact , but can results invalid message and data traffic.
     */
    private static void cleanupOtherCcuZoneSchedules(CCUHsApi hayStack) {
        CcuLog.i(TAG_CCU_MIGRATION_UTIL, "cleanupOtherCcuZoneSchedules ");
        ArrayList<HashMap<Object, Object>> zoneSpecialScheduleList = hayStack.readAllEntities("zone " +
                "and not special and schedule");
        String ccuId = hayStack.getCcuId();
        zoneSpecialScheduleList.forEach( scheduleMap -> {
            Object roomRef = scheduleMap.get(Tags.ROOMREF);
            String scheduleCcuRef = scheduleMap.get("ccuRef").toString();
            String zoneCcuRef = hayStack.getCcuRef().toString();
            if (roomRef != null && !hayStack.isEntityExisting(roomRef.toString())  &&
                    !scheduleCcuRef.equals(zoneCcuRef)) {
                hayStack.deleteEntityLocally(scheduleMap.get(Tags.ID).toString());
                CcuLog.i(TAG_CCU_MIGRATION_UTIL, "delete invalid zone schedule "+scheduleMap);
            } else {
                if(scheduleMap.get("ccuRef") == null || !scheduleMap.get("ccuRef").toString().equals(ccuId))
                {
                    scheduleMap.put("ccuRef", hayStack.getCcuId());
                    Schedule schedule = hayStack.getScheduleById(scheduleMap.get("id").toString());
                    hayStack.updateScheduleNoSync(schedule, scheduleMap.get("roomRef").toString());

                }
            }


        });
    }

    private static void enableTISensorPort(CCUHsApi haystack){
         CcuLog.d(TAG_CCU_MIGRATION_UTIL, "enableTISensorPort migration started");
        HashMap<Object, Object> equipMap = haystack.readEntity("equip and ti");
        if (!equipMap.isEmpty()) {
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "TI exists");
                Equip equip = new Equip.Builder().setHashMap(equipMap).build();
                double roomTempTypeConfigPoint  = haystack.readDefaultVal("point and " +
                        "config and temp and ti and space and type and equipRef == \"" + equip.getId() + "\"");
                HashMap<Object, Object> roomTemperaturePoint = haystack.readEntity("ti and temp and space and not config" +
                        " and equipRef == \""+equip.getId()+"\"");
                HashMap<Object,Object> currentTemp = haystack.readEntity("point and current and " +
                        "temp and ti and equipRef == \""+equip.getId()+"\"");
                String nodeAddress = currentTemp.get("group").toString();
                if (roomTempTypeConfigPoint == 1) {
                    ControlMote.setPointEnabled(Integer.parseInt(nodeAddress), Port.TH1_IN.name(), true);
                    ControlMote.setCMPointEnabled(Port.TH1_IN.name(), true);
                    ControlMote.updatePhysicalPointRef(Integer.parseInt(nodeAddress), Port.TH1_IN.name(), roomTemperaturePoint.get("id").toString());
                } else if (roomTempTypeConfigPoint == 2) {
                    ControlMote.setPointEnabled(Integer.parseInt(nodeAddress), Port.TH2_IN.name(), true);
                    ControlMote.setCMPointEnabled(Port.TH2_IN.name(), true);
                    ControlMote.updatePhysicalPointRef(Integer.parseInt(nodeAddress), Port.TH2_IN.name(), roomTemperaturePoint.get("id").toString());
                } else {
                    ControlMote.setPointEnabled(Integer.parseInt(nodeAddress), Port.SENSOR_RT.name(), true);
                    ControlMote.setCMPointEnabled(Port.SENSOR_RT.name(), true);
                    ControlMote.updatePhysicalPointRef(Integer.parseInt(nodeAddress), Port.SENSOR_RT.name(), currentTemp.get("id").toString());
                }
        }
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "enableTISensorPort migration started");
        CCUHsApi.getInstance().scheduleSync();
    }

    private static void doHSSOutsideDamperMinOpenMigration(CCUHsApi haystack) {
        ArrayList<HashMap<Object, Object>> equips = haystack.readAllEntities("equip and hyperstatsplit");

        equips.forEach(equip -> {
            Equip hssEquip = new Equip.Builder().setHashMap(equip).build();
            HashMap<Object, Object> outsideDamperMinOpen = haystack.readEntity("outside and damper and min and open and equipRef ==\""+hssEquip.getId()+"\"");

            if (!outsideDamperMinOpen.isEmpty()) {
                Point modifiedPoint  = new Point.Builder().setHashMap(outsideDamperMinOpen).addMarker("his").build();
                haystack.updatePoint(modifiedPoint, modifiedPoint.getId());
            }
        });
    }

    public static void createZoneSchedulesIfMissing(CCUHsApi ccuHsApi) {
        List<HashMap<Object, Object>> rooms = ccuHsApi.readAllEntities("room");
        for(HashMap<Object, Object> room : rooms) {
            HashMap<Object, Object> scheduleHashmap = ccuHsApi.readEntity(
                    "schedule and " +
                            "not special and not vacation and roomRef " + "== " + room.get("id"));
            if (scheduleHashmap.size() == 0) {

                String scheduleRef = DefaultSchedules.generateDefaultSchedule(true, room.get("id").toString());
                Double scheduleType = ccuHsApi.readPointPriorityValByQuery("scheduleType and roomRef == \""
                        + room.get("id") +"\"");
                if( scheduleType == null || scheduleType == ScheduleType.ZONE.ordinal()){
                    HashMap<Object, Object> roomToUpdate = ccuHsApi.readMapById(room.get("id").toString());
                    Zone zone = new Zone.Builder().setHashMap(roomToUpdate).build();
                    zone.setScheduleRef(scheduleRef);
                    ccuHsApi.updateZone(zone, zone.getId());
                }
            }
        }
    }

    public static void migrateZoneScheduleIfMissed(CCUHsApi ccuHsApi) {
        List<HashMap<Object, Object>> rooms = ccuHsApi.readAllEntities("room");
        for(HashMap<Object, Object> room : rooms) {
            HashMap<Object, Object> scheduleHashmap = ccuHsApi.readEntity(
                    "schedule and " +
                            "not special and not vacation and roomRef " + "== " + room.get("id"));
            if(scheduleHashmap.size() > 0 && !scheduleHashmap.containsKey("unoccupiedZoneSetback")){
                String oldZoneScheduleId =  scheduleHashmap.get("id").toString();
                String newZoneScheduleId = DefaultSchedules.generateDefaultSchedule(true,
                        scheduleHashmap.get("roomRef").toString());
                Schedule newZoneSchedule = ccuHsApi.getScheduleById(newZoneScheduleId);
                newZoneSchedule.setId(oldZoneScheduleId);
                ccuHsApi.updateSchedule(newZoneSchedule);
                ccuHsApi.deleteEntityItem(newZoneScheduleId);
            }
        }
    }

    public static void migrateZoneScheduleTypeIfMissed(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> rooms = ccuHsApi.readAllEntities("room");

        for(HashMap<Object, Object> room : rooms) {

            String zoneId = room.get("id").toString();
            String scheduleId = room.get("scheduleRef").toString();
            Double scheduleTypeToBeSet = 2.0 ;
            Schedule roomSchedule = ccuHsApi.getScheduleById(scheduleId);
            HashMap<Object, Object> defaultSchedule = ccuHsApi.readEntity("default and schedule");
            if(defaultSchedule.isEmpty() || roomSchedule.isZoneSchedule()){
                scheduleTypeToBeSet = 1.0;
            }

            ArrayList<HashMap<Object, Object>> scheduleTypePoints = ccuHsApi.readAllEntities("scheduleType and roomRef == \"" + zoneId + "\"");
            ArrayList<HashMap<Object, Object>> equips = ccuHsApi.readAllEntities("equip and roomRef == \"" + zoneId + "\"");


            if (scheduleTypePoints.isEmpty() || equips.size() > scheduleTypePoints.size()) {
                //create schedule type point assign it to 2
                //set schedule ref to default name to that zone
                // return deafult named
                ArrayList<HashMap<Object, Object>> allEquip =
                        ccuHsApi.readAllEntities("equip and roomRef == \"" + zoneId + "\"");
                for (HashMap<Object, Object> equip : allEquip) {
                    String profileType = equip.get("profile").toString();
                    createScheduleType(equip, profileType,scheduleTypeToBeSet);
                }

                Zone zone = HSUtil.getZone(zoneId, Objects.requireNonNull(room.get("floorRef")).toString());
                if (zone != null) {
                    if(scheduleTypeToBeSet == 2.0 && !defaultSchedule.isEmpty()) {
                        zone.setScheduleRef(defaultSchedule.get("id").toString());
                        CCUHsApi.getInstance().updateZone(zone, zoneId);
                    }
                }

            }
        }
        ccuHsApi.scheduleSync();

    }


    public static void createScheduleType(HashMap<Object, Object> equip, String profileType,double scheduleType){
        String siteDis = equip.get("dis").toString();
        String nodeAddr = equip.get("group").toString();
        String equipRef = equip.get("id").toString();
        String siteRef = equip.get("siteRef").toString();
        String roomRef = equip.get("roomRef").toString();
        String floorRef = equip.get("floorRef").toString();
        String tz = equip.get("tz").toString();

        Point.Builder equipScheduleType = new Point.Builder()
                .setDisplayName(siteDis+"-scheduleType")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("scheduleType").addMarker("writable").addMarker("his")
                .setGroup((nodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz);

        addTagBasedOnProfile(equipScheduleType,profileType,equip);

        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType.build());
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId,  scheduleType);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(equipScheduleTypeId, scheduleType);

    }

    private static void addTagBasedOnProfile(Point.Builder equipScheduleType, String profileType,HashMap<Object, Object> equip) {
        if(profileType.equals("TEMP_INFLUENCE") )
            equipScheduleType.addMarker("ti");
        else if(profileType.contains("DAB"))
            equipScheduleType.addMarker("dab");
        else if(profileType.contains("DUAL_DUCT"))
            equipScheduleType.addMarker("dualDuct");
        else if(profileType.contains("EMR"))
            equipScheduleType.addMarker("emr");
        else if(profileType.contains("HYPERSTAT"))
            equipScheduleType.addMarker("hyperstat");
        else if(profileType.contains("MODBUS"))
            equipScheduleType.addMarker("modbus");
        else if(profileType.contains("OTN"))
            equipScheduleType.addMarker("otn");
        else if(profileType.contains("PLC"))
            equipScheduleType.addMarker("pid");
        else if(profileType.contains("SMARTSTAT")){
            if(profileType.contains("SMARTSTAT_CONVENTIONAL_PACK_UNIT"))
                equipScheduleType.addMarker("cpu");
            if(profileType.contains("SMARTSTAT_HEAT_PUMP_UNIT"))
                equipScheduleType.addMarker("hpu");
            if(profileType.contains("SMARTSTAT_TWO_PIPE_FCU"))
                equipScheduleType.addMarker("pipe2");
            if(profileType.contains("SMARTSTAT_FOUR_PIPE_FCU"))
                equipScheduleType.addMarker("pipe4");
        }else if(profileType.contains("SSE"))
            equipScheduleType.addMarker("sse");
        else if(profileType.contains("VAV")) {
            if( equip.containsKey("series"))
                equipScheduleType.addMarker("series");
            else
                equipScheduleType.addMarker("parallel");
        }else if(profileType.contains("VRV"))
            equipScheduleType.addMarker("vrv");

    }
    private static void UpdateFloorRefRoomRefForConfigPoints(CCUHsApi haystack){
        ArrayList<HashMap<Object, Object>> equipList = haystack.readAllEntities("zone and equip and (ti or sse or" +
                " dab or dualDuct)");
        for(HashMap<Object, Object> equipMap : equipList){
            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            ArrayList<HashMap<Object, Object>> configPointList = haystack.readAllEntities("point and " +
                    "equipRef== \""+ equip.getId() +"\"");
            for( HashMap<Object, Object> configPointMap : configPointList){
                Point equipPoint = new Point.Builder().setHashMap(configPointMap).build();
                if(equipPoint.getFloorRef().equalsIgnoreCase("SYSTEM") ||
                        (equipPoint.getRoomRef().equalsIgnoreCase("SYSTEM"))) {
                    equipPoint.setFloorRef(equip.getFloorRef());
                    equipPoint.setRoomRef(equip.getRoomRef());
                    haystack.updatePoint(equipPoint, equipPoint.getId());
                    CcuLog.i(TAG_CCU_MIGRATION_UTIL,
                            "FloorRef and RoomRef updated for the point id :" +equipPoint.getId());
                }
            }
        }
    }



    private static void updateDisForPointsDabToVvt(CCUHsApi haystack){
        findDabEquipsAndUpdate(haystack, "equip and dab");
        findDabEquipsAndUpdate(haystack, "equip and tuner");
        findDabPointsAndUpdate(haystack, haystack.readAllEntities("dab and purge"));
        findDabPointsAndUpdate(haystack, haystack.readAllEntities("dab and oao"));
    }

    private static void findDabEquipsAndUpdate(CCUHsApi haystack, String query){
        ArrayList<HashMap<Object, Object>> equipList = haystack.readAllEntities(query);
        for(HashMap<Object, Object> equipMap : equipList){
            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            if(equip.getDisplayName().toLowerCase().contains("dab")){
                Equip tempEquip = new Equip.Builder().setHashMap(equipMap)
                        .setDisplayName(equip.getDisplayName().replaceAll("(?i)dab", "VVT"))
                        .build();
                haystack.updateEquip(tempEquip, tempEquip.getId());
            }

            ArrayList<HashMap<Object, Object>> configPointList = haystack.readAllEntities("point and " +
                    "equipRef== \""+ equip.getId() +"\"");
            findDabPointsAndUpdate(haystack, configPointList);
        }
    }

    private static void findDabPointsAndUpdate(CCUHsApi haystack, ArrayList<HashMap<Object, Object>> configPointList){
        for( HashMap<Object, Object> configPointMap : configPointList){
            Point equipPoint = new Point.Builder().setHashMap(configPointMap).build();
            if(equipPoint.getDisplayName().toLowerCase().contains("dab")){
                equipPoint.setDisplayName(equipPoint.getDisplayName().replaceAll("(?i)dab", "VVT"));
                haystack.updatePoint(equipPoint, equipPoint.getId());
                CcuLog.i(TAG_CCU_MIGRATION_UTIL,
                        "carrier migration dis updated for the point id :" +equipPoint.getId());
            }
        }
    }

    private static void migrateRemoteAccess() {

        CCUHsApi ccuHsApi = CCUHsApi.getInstance();

        Map<Object,Object> diagEquip = ccuHsApi.readEntity("equip and diag");

        if(!diagEquip.isEmpty())    {

            Map<Object,Object> remoteSessionStatusDiagPoint = ccuHsApi.readEntity("remote and status " +
                    " and diag and point");

            if(remoteSessionStatusDiagPoint.isEmpty())  {

                String equipRef = Objects.requireNonNull(diagEquip.get("id")).toString();
                String equipDis = "DiagEquip";
                String siteRef = Objects.requireNonNull(diagEquip.get("siteRef")).toString();
                String tz = Objects.requireNonNull(diagEquip.get("tz")).toString();

                Point remoteSessionStatus = new Point.Builder()
                        .setDisplayName(equipDis+"-remoteSessionStatus")
                        .setEquipRef(equipRef)
                        .setSiteRef(siteRef).setHisInterpolate("cov")
                        .addMarker("diag").addMarker("remote").addMarker("status").addMarker("sp")
                        .addMarker("storage").addMarker("his").addMarker("cur")
                        .setEnums(RemoteSessionStatus.getEnum())
                        .setTz(tz)
                        .build();
                ccuHsApi.addPoint(remoteSessionStatus);
            }
        }
    }

    private static void updateHisValue(CCUHsApi haystack) {
        ExecutorTask.executeBackground(() -> {
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "updateHisValue migration started");
            ArrayList<HashMap<Object, Object>> writablePoints = haystack.readAllEntities("point and writable");
            for (HashMap<Object, Object> map : writablePoints) {
                try{
                    if (map.containsKey("id") && map.get("id") != null) {
                        double pointPriorityVal = haystack.readPointPriorityVal(map.get("id").toString());
                        haystack.writeHisValById(map.get("id").toString(), pointPriorityVal);
                    }
                } catch (NumberFormatException e){
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL, "point not updated: " + map.get("id").toString());
                    e.printStackTrace();
                }
            }
            PreferenceUtil.setHisItemsUpdatedStatus();
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "updateHisValue migration ended");
        });
    }


    private static void syncZoneSchedulesFromLocal(CCUHsApi ccuHsApi) {
        String response = ccuHsApi.fetchRemoteEntityByQuery("zone and schedule and not building and not default and not special and not vacation and ccuRef == " + ccuHsApi.getCcuId());
        if(response == null || response.isEmpty()){
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Failed to read remote entity : " + response);
            return;
        }
        HGrid sGrid = new HZincReader(response).readGrid();
        Iterator it = sGrid.iterator();
        ArrayList<HashMap<Object, Object>> rooms = ccuHsApi.readAllEntities("room");

        //if the number of rooms are equal to the zoneschedules from remote call need not sync anything.
        if(sGrid.numRows() >= rooms.size())
            return;

        ArrayList<String> scheduleIdList = new ArrayList<>();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            Schedule schedule = new Schedule.Builder().setHDict(new HDictBuilder().add(row).toDict()).build();
            scheduleIdList.add(schedule.getId());
        }

        for(HashMap<Object, Object> room : rooms) {
            ArrayList<Schedule> roomSchedules = ccuHsApi.getZoneSchedule(room.get("id").toString(),false);
            if (roomSchedules.size() > 0) {
                Schedule roomSchedule = roomSchedules.get(0);
                if (!scheduleIdList.contains(roomSchedule.getId())) {
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL, "re-sync roomSchedule id : " + roomSchedule.getId());
                    ccuHsApi.updateZoneSchedule(roomSchedule,room.get("id").toString());
                }
            }
        }

    }

    private static void updateAllZoneSchedulesLocally(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> rooms = ccuHsApi.readAllEntities("room");
        for(HashMap<Object, Object> room : rooms) {
            ArrayList<Schedule> roomSchedules = ccuHsApi.getZoneSchedule(room.get("id").toString(), false);
            if (roomSchedules.size() > 0) {
                Schedule roomSchedule = roomSchedules.get(0);
                if (roomSchedule != null && (roomSchedule.getCcuRef() == null || roomSchedule.getUnoccupiedZoneSetback() == null)) {
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Locally updatin g schedule " + roomSchedule.getId());
                    ccuHsApi.updateScheduleNoSync(roomSchedule, room.get("id").toString());
                }
            }
        }
    }


    public static void cleanUpAndCreateZoneSchedules(CCUHsApi hayStack) {
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "cleanUpAndCreateZoneSchedules started");
        String ccuId = hayStack.getCcuId();
        //get all zone schedule and check if it has valid ccuref
        ArrayList<HashMap<Object, Object>> rooms = hayStack.readAllEntities("room");
        for (HashMap<Object, Object> room : rooms) {
            Schedule roomsschedule = hayStack.getScheduleById(room.get("scheduleRef").toString().replace("@", ""));
            if (roomsschedule != null && roomsschedule.getCcuRef() != null) {
                if (!roomsschedule.getCcuRef().equals(ccuId)) {
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL, "correcting ccuref of " + roomsschedule.getId());
                    roomsschedule.setCcuRef(ccuId);
                    hayStack.updateZoneSchedule(roomsschedule,room.get("id").toString());
                }
            }
        }


        // getting zone schedules
        ArrayList<HashMap<Object, Object>> otherCCUSchedules = hayStack.readAllEntities("" +
                "zone and schedule and ccuRef and not building and not default and not special and not vacation and ccuRef != " + ccuId);

        // removing zone schedule entity locally
        otherCCUSchedules.forEach(entry -> {
            String scheduleID = Objects.requireNonNull(entry.get("id")).toString();
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "schedule deleted locally : " + scheduleID);
            hayStack.deleteEntityLocally(scheduleID);
        });

        //fetching all zoneschedules with old structure
        ArrayList<HashMap<Object, Object>> oldZoneSchedules = hayStack.readAllEntities("" +
                "zone and schedule and not building and not default and not special and not vacation and not unoccupiedZoneSetback");

        // removing zone schedule with old structure
        oldZoneSchedules.forEach(entry -> {
            String scheduleID = Objects.requireNonNull(entry.get("id")).toString();
            String ccuref = hayStack.getScheduleById(scheduleID).getCcuRef();
            if (ccuref != null && ccuref.equals(ccuId)) {
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "schedule ccuref : " + ccuref);
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "schedule deleted : " + scheduleID);
                hayStack.deleteEntity(scheduleID);
            }
        });


        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "creating new zone schedule");
        MigrationUtil.createZoneSchedulesIfMissing(hayStack);
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "cleanUpAndCreateZoneSchedules completed");
    }



}