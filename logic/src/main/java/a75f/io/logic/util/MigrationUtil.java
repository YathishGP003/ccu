package a75f.io.logic.util;

import static a75f.io.logic.L.TAG_CCU_MIGRATION_UTIL;
import static a75f.io.logic.bo.building.dab.DabReheatPointsKt.createReheatType;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_ONE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_TWO;
import static a75f.io.logic.tuners.DabReheatTunersKt.createEquipReheatTuners;
import static a75f.io.logic.tuners.TunerConstants.TUNER_EQUIP_VAL_LEVEL;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ConfigUtil;
import a75f.io.logic.bo.building.ccu.SupplyTempSensor;
import a75f.io.logic.bo.building.dab.DabEquip;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.definitions.Units;
import a75f.io.logic.bo.building.dualduct.DualDuctEquip;
import a75f.io.logic.bo.building.hyperstat.common.HyperStatPointsUtil;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.sse.InputActuatorType;
import a75f.io.logic.bo.building.sse.SingleStageConfig;
import a75f.io.logic.bo.building.truecfm.TrueCFMPointsHandler;
import a75f.io.logic.bo.building.vav.VavEquip;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.bo.haystack.device.DeviceUtil;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.ccu.restore.RestoreCCU;
import a75f.io.logic.diag.DiagEquip;
import a75f.io.logic.migration.hyperstat.CpuPointsMigration;
import a75f.io.logic.migration.hyperstat.MigratePointsUtil;
import a75f.io.logic.migration.point.PointMigrationHandler;
import a75f.io.logic.pubnub.hyperstat.HyperStatReconfigureUtil;
import a75f.io.logic.tuners.TrueCFMTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.VavTuners;
import a75f.io.logic.tuners.VrvTuners;
import kotlin.Pair;

public class MigrationUtil {
    private static final String TAG = "MIGRATION_UTIL";
    private static final String MIGRATION_DEBUG = "MIGRATION_DEBUG";

    /**
     * All the migration tasks needed to be run during an application version upgrade should be called from here.
     *
     * This approach has a drawback the migration gets invoked when there is version downgrade
     * THis will be fixed by using longVersionCode after migrating to API30. (dev going in another branch)
     */
    public static void doMigrationTasksIfRequired() {
        /*if (checkVersionUpgraded()) {
            updateAhuRefForBposEquips(CCUHsApi.getInstance());
            PreferenceUtil.setMigrationVersion()
        }*/
        if(!PreferenceUtil.isSenseAndPILoopAnalogPointDisMigrationDone()){
            updateAnalogInputDisplayNameForSense();
            updateAnalogInputDisplayNameForPILOOP();
            PreferenceUtil.setSenseAndPILoopAnalogPointDisMigrationDone(true);
        }

        if (!PreferenceUtil.isBposAhuRefMigrationDone()) {
            updateAhuRefForBposEquips(CCUHsApi.getInstance());
            PreferenceUtil.setBposAhuRefMigrationStatus(true);
        }

        if (!PreferenceUtil.areDuplicateAlertsRemoved()) {
            removeDuplicateAlerts(AlertManager.getInstance());
            PreferenceUtil.removedDuplicateAlerts();
        }

        if (!PreferenceUtil.getEnableZoneScheduleMigration()) {
            updateZoneScheduleTypes(CCUHsApi.getInstance());
            PreferenceUtil.setEnableZoneScheduleMigration();
        }

        if (!PreferenceUtil.getCleanUpDuplicateZoneSchedule()) {
            cleanUpDuplicateZoneSchedules(CCUHsApi.getInstance());
            PreferenceUtil.setCleanUpDuplicateZoneSchedule();
        }

        if (!PreferenceUtil.isCCUHeartbeatMigrationDone()) {
            addCCUHeartbeatDiagPoint();
            PreferenceUtil.setCCUHeartbeatMigrationStatus(true);
        }

        if(!PreferenceUtil.isPressureUnitMigrationDone()){
            pressureUnitMigration(CCUHsApi.getInstance());
            PreferenceUtil.setPressureUnitMigrationDone();
        }
        if(!PreferenceUtil.isAirflowVolumeUnitMigrationDone()){
            airflowUnitMigration(CCUHsApi.getInstance());
            PreferenceUtil.setAirflowVolumeUnitMigrationDone();
        }
        if (!PreferenceUtil.isTimerCounterAndCFMCoolingMigrationDone()) {
            timerCounterAndCFMCoolingMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTimerCounterAndCFMCoolingMigrationDone();
        }
        if (!PreferenceUtil.isRelayDeactivationAndReheatZoneToDATMigrationDone()) {
            relayDeactivationAndReheatZoneToDATMigration(CCUHsApi.getInstance());
            PreferenceUtil.setRelayDeactivationAndReheatZoneToDATMinMigrationDone();
        }
        if (!PreferenceUtil.isTrueCFMVAVMigrationDone()) {
            trueCFMVAVMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTrueCFMVAVMigrationDone();
        }

        if (!PreferenceUtil.isTrueCFMDABMigrationDone()) {
            trueCFMDABMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTrueCFMDABMigrationDone();
        }

        if (!PreferenceUtil.getDamperFeedbackMigration()) {
            doDamperFeedbackMigration(CCUHsApi.getInstance());
            PreferenceUtil.setDamperFeedbackMigration();
        }

        if(!PreferenceUtil.getAddedUnitToTuners()){
            addUnitToTuners(CCUHsApi.getInstance());
            PreferenceUtil.setUnitAddedToTuners();
        }

        if(!PreferenceUtil.getVocPm2p5Migration()){
            migrateVocPm2p5(CCUHsApi.getInstance());
            PreferenceUtil.setVocPm2p5Migration();
        }

        if(!PreferenceUtil.getDiagEquipMigration()){
            doDiagPointsMigration(CCUHsApi.getInstance());
            PreferenceUtil.setDiagEquipMigration();
        }

        if(!PreferenceUtil.getScheduleRefactorMigration()) {
            scheduleRefactorMigration(CCUHsApi.getInstance());
            PreferenceUtil.setScheduleRefactorMigration();
        }

        if(!PreferenceUtil.getNewOccupancy()){
            Log.d(TAG_CCU_MIGRATION_UTIL, "AutoForceOcccupied and Autoaway build less");
            migrateNewOccupancy(CCUHsApi.getInstance());
            PreferenceUtil.setNewOccupancy();
        }

        if(isFanControlDelayDefaultValueUpdated(CCUHsApi.getInstance())){
            updateFanControlDefaultValue(CCUHsApi.getInstance());
        }

        if(!PreferenceUtil.getSiteNameEquipMigration()){
            ControlMote.updateOnSiteNameChange();
            PreferenceUtil.setDiagEquipMigration();
        }

        if(!isTIThermisterMigrated()){
            Log.d(TAG,"isTIThermisterMigrated return true");
            addTIThermisters(CCUHsApi.getInstance());
        }else{
            Log.d(TAG,"isTIThermisterMigrated is false");
        }

        if(!PreferenceUtil.getScheduleRefUpdateMigration()){
            updateScheduleRefs(CCUHsApi.getInstance());
            PreferenceUtil.setScheduleRefUpdateMigration();
        }

        if(!PreferenceUtil.getScheduleTypeUpdateMigration()){
            updateScheduleType(CCUHsApi.getInstance());
            PreferenceUtil.setScheduleTypeUpdateMigration();
        }

        if(!PreferenceUtil.getScheduleRefForZoneMigration()){
            updateScheduleRefForZones(CCUHsApi.getInstance());
            PreferenceUtil.setScheduleRefForZoneMigration();
        }

        if(!PreferenceUtil.getVocPm2p5MigrationV1()){
            migrateHisInterpolateIssueFix(CCUHsApi.getInstance());
            PreferenceUtil.setVocPm2p5MigrationV1();
       }

        if(!PreferenceUtil.getStageTimerForDABMigration()){
            updateStageTimerForDAB(CCUHsApi.getInstance());
            PreferenceUtil.setStageTimerForDABMigration();
        }

        if(hasTIProfile(CCUHsApi.getInstance()) && !PreferenceUtil.getTIUpdate()) {
            Log.d(TAG, "hasTIProfile");
            MigrateTIChanges(CCUHsApi.getInstance());
            PreferenceUtil.setTIUpdate();
        }

        if(!PreferenceUtil.getDCWBPointsMigration()){
            migrateDCWBPoints(CCUHsApi.getInstance());
            PreferenceUtil.setDCWBPointsMigration();
        }

        if(!PreferenceUtil.getSmartStatPointsMigration()){
            doSmartStatPointsMigration(CCUHsApi.getInstance());
            PreferenceUtil.setSmartStatPointsMigration();
        }

        if(!PreferenceUtil.getBPOSToOTNMigration()){
            migrateBPOSToOTN(CCUHsApi.getInstance());
            PreferenceUtil.setBPOSToOTNMigration();
        }

        if(!PreferenceUtil.getHyperStatDeviceDisplayConfigurationPointsMigration()){
            createHyperStatDeviceDisplayConfigurationPointsMigration(CCUHsApi.getInstance());
            PreferenceUtil.setHyperStatDeviceDisplayConfigurationPointsMigration();
        }
        Log.i(MIGRATION_DEBUG, "Migration started :getVavDiscargeTunerMigration Key exist ?"+PreferenceUtil.getVavDiscargeTunerMigration());
        if(!PreferenceUtil.getVavDiscargeTunerMigration()){
            migrationForVAVTunerDefaultValue(CCUHsApi.getInstance());
            PreferenceUtil.setVavDiscargeTunerMigration();
        }

        if (!PreferenceUtil.getDabReheatSupportMigrationStatus()) {
            doDabReheatMigration(CCUHsApi.getInstance());
            PreferenceUtil.setDabReheatSupportMigrationStatus();
        }

        if(!PreferenceUtil.getHyperStatCpuTagMigration()){
            CpuPointsMigration.Companion.doMigrationForProfilePoints();
            PreferenceUtil.setHyperStatCpuTagMigration();
        }

        if(!PreferenceUtil.getAutoAwayAutoForcedPointMigration()){
            autoAwayAutoForcedMigration(CCUHsApi.getInstance());
            PreferenceUtil.setAutoAwayAutoForcedPointMigration();
        }


        if(!PreferenceUtil.getSmartNodeDamperMigration()){
            doMigrateForSmartNodeDamperType(CCUHsApi.getInstance());
            PreferenceUtil.setSmartNodeDamperMigration();
        }

        if(!PreferenceUtil.getHyperStatCpuAirTagMigration()){
            doAirTagMigration(CCUHsApi.getInstance());
            PreferenceUtil.setHyperStatCpuAirTagMigration();
        }


        if(!PreferenceUtil.getFreeInternalDiskStorageMigration()){
            createFreeInternalDiskStorageDiagPointMigration(CCUHsApi.getInstance());
            PreferenceUtil.setFreeInternalDiskStorageMigration();
        }


        if(!PreferenceUtil.getNewOccupancyMode()) {
            addBuildingLimitsBreachedOccupancy(CCUHsApi.getInstance());
            PreferenceUtil.setNewOccupancyMode();
        }


        if(!PreferenceUtil.getSSEFanStageMigration()){
            SSEFanStageMigration(CCUHsApi.getInstance());
            PreferenceUtil.setSSEFanStageMigration();
        }

        if (!PreferenceUtil.getAirflowSampleWaitTImeMigration()) {
            airflowSampleWaitTimeMigration(CCUHsApi.getInstance());
            PreferenceUtil.setAirflowSampleWaitTimeMigration();
        }

        if (!PreferenceUtil.getstaticPressureSpTrimMigration()) {
            staticPressureSpTrimMigration(CCUHsApi.getInstance());
            PreferenceUtil.setStaticPressureSpTrimMigration();
        }

        if (!PreferenceUtil.getMinorTagMigration()) {
            MinorTagMigration(CCUHsApi.getInstance());
            PreferenceUtil.setMinorTagMigration();
        }

        if (!PreferenceUtil.getCorruptedNamedScheduleRemoval()) {
            try {
                removeCorruptedNamedSchedules(CCUHsApi.getInstance());
                PreferenceUtil.setCorruptedNamedScheduleRemoval();
            } catch (Exception e) {
                e.printStackTrace();
                CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Failed to remove corrupted named schedule. Will be retried during next app start");
            }
        }

        if (!PreferenceUtil.getStandaloneHeatingOffsetMigration()) {
            standaloneHeatingOffsetMigration(CCUHsApi.getInstance());
            PreferenceUtil.setStandaloneHeatingOffsetMigration();
        }


        if(!PreferenceUtil.getTiProfileMigration()){
            doTiProfileMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTiProfileMigration();
        }


        if(!PreferenceUtil.getDabReheatStage2Migration()) {
            doDabReheatStage2Migration(CCUHsApi.getInstance());
            PreferenceUtil.setDabReheatStage2Migration();
        }

        if(!PreferenceUtil.getstandaloneCoolingAirflowTempLowerOffsetMigration()){
            createStandaloneCoolingAirflowTempLowerOffsetMigration(CCUHsApi.getInstance());
            PreferenceUtil.setstandaloneCoolingAirflowTempLowerOffsetMigration();
        }

        if(!PreferenceUtil.getStandaloneAirflowSampleWaitMigration()){
            createStandaloneAirflowSampleWaitMigration(CCUHsApi.getInstance());
            PreferenceUtil.setAirflowSampleWaitTimeUnitMigration();
        }

        if(!PreferenceUtil.getAutoForcedTagNameCorrectionMigration()){
            changeOccupancyToOccupiedForAutoForcedEnabledPoint(CCUHsApi.getInstance());
            PreferenceUtil.setAutoForcedTagNameCorrectionMigration();
        }

        L.saveCCUState();
    }

    private static void standaloneHeatingOffsetMigration(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> standaloneHEatingOffsetPoints = ccuHsApi.readAllEntities("standalone and heating and offset and stage1");
        for (HashMap<Object, Object> standaloneHeatingOffset : standaloneHEatingOffsetPoints) {
            String updatedMaxValue = "150";
            String updatedMinValue = "0";

            Point updatedStandaloneHeatingOffsetPoint = new Point.Builder().setHashMap(standaloneHeatingOffset).setMaxVal(updatedMaxValue).setMinVal(updatedMinValue).build();
            CCUHsApi.getInstance().updatePoint(updatedStandaloneHeatingOffsetPoint, updatedStandaloneHeatingOffsetPoint.getId());
        }
    }

    private static void SSEFanStageMigration(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> sseEquips = ccuHsApi.readAllEntities("equip and sse");
        for (HashMap<Object, Object> equip : sseEquips) {
            String equipRef = equip.get("id").toString();
            HashMap<Object, Object> currentTemp = ccuHsApi.readEntity("point and current and " +
                    "temp and sse and equipRef == \"" + equipRef + "\"");
            String nodeAddr = currentTemp.get("group").toString();
            HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
            String siteRef = (String) siteMap.get(Tags.ID);
            String siteDis = (String) siteMap.get("dis");
            String tz = siteMap.get("tz").toString();
            String equipDis = siteDis + "-TI-" + nodeAddr;
            SingleStageConfig config = new SingleStageConfig();
            createAnalogInEnablePoint(equipDis, siteRef, equipRef, equip, nodeAddr, tz, config, currentTemp);
            createAnalogIn1AssociationPoint(equipDis, siteRef, equipRef, equip, nodeAddr, tz, config);
            CCUHsApi.getInstance().syncEntityTree();
        }

    }
    private static void createAnalogIn1AssociationPoint(String equipDis, String siteRef, String equipRef, HashMap<Object, Object> actualEquip, String nodeAddr, String tz, SingleStageConfig config) {
        Point analogInAssociation = new Point.Builder()
                .setDisplayName(equipDis + "-analogIn1Association")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(actualEquip.get("roomRef").toString())
                .setFloorRef(actualEquip.get("floorRef").toString())
                .addMarker("config").addMarker("analog1").addMarker("input").addMarker("writable")
                .addMarker("standalone").addMarker("zone").addMarker("sse").addMarker("association")
                .setEnums(InputActuatorType.getEnumStringDefinition())
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();

        String analogInAssociationId = CCUHsApi.getInstance().addPoint(analogInAssociation);
        CCUHsApi.getInstance().writeDefaultValById(analogInAssociationId, 0.0);

    }

    private static void createAnalogInEnablePoint(String equipDis, String siteRef, String equipRef, HashMap<Object, Object> actualEquip, String nodeAddr, String tz, SingleStageConfig config, HashMap<Object, Object> currentTemp) {
        Point analogIn = new Point.Builder()
                .setDisplayName(equipDis + "-analogIn1Enabled")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(actualEquip.get("roomRef").toString())
                .setFloorRef(actualEquip.get("floorRef").toString())
                .addMarker("config").addMarker("writable").addMarker("zone").addMarker("input")
                .addMarker("analog1").addMarker("enabled").addMarker("sse").addMarker("standalone")
                .setEnums("false,true")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();

        String analogIn1Id = CCUHsApi.getInstance().addPoint(analogIn);
        CCUHsApi.getInstance().writeDefaultValById(analogIn1Id, (config.analogIn1 ? 1.0 : 0));
        SmartNode.setPointEnabled(Integer.parseInt(nodeAddr),Port.ANALOG_IN_ONE.name(), false);
        SmartNode.updatePhysicalPointType(Integer.parseInt(nodeAddr), Port.ANALOG_IN_ONE.name(), String.valueOf(8));
        SmartNode.updatePhysicalPointRef(Integer.parseInt(nodeAddr), Port.ANALOG_IN_ONE.name(), analogIn1Id);


    }

    private static void doTiProfileMigration(CCUHsApi instance) {

        ArrayList<HashMap<Object, Object>> tiEquips = instance.readAllEntities("equip and ti");
        for (HashMap<Object, Object> equipMap : tiEquips) {
            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            HashMap<Object,Object> currentTemp = instance.readEntity("point and current and " +
                    "temp and ti and equipRef == \""+equip.getId()+"\"");
            String nodeAddress = currentTemp.get("group").toString();
            deleteExistingLogicalAndConfigPoints(instance, equip);
            createNewLogicalPoints(equip, nodeAddress);
        }

    }

    private static void createNewLogicalPoints(Equip equip, String nodeAddress) {

        Point roomTempSensorPoint = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-RoomTemperature")
                .setEquipRef(equip.getId())
                .setSiteRef(equip.getSiteRef())
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                .addMarker("ti").addMarker("temp").addMarker("space").addMarker("cur").addMarker("sensor")
                .addMarker("logical").addMarker("zone").addMarker("his").addMarker("air")
                .setGroup(String.valueOf(nodeAddress))
                .setUnit("\u00B0F")
                .setTz(CCUHsApi.getInstance().getTimeZone())
                .build();
        String roomTempSensorId = CCUHsApi.getInstance().addPoint(roomTempSensorPoint);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(roomTempSensorId,0.0);

        Point supplyAirTemperatureType = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-supplyAirTemperatureType")
                .setEquipRef(equip.getId()).setRoomRef(equip.getRoomRef())
                .setSiteRef(equip.getSiteRef()).setFloorRef(equip.getFloorRef())
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("supply").addMarker("sp").addMarker("type").addMarker("temp").addMarker("air")
                .setGroup(String.valueOf(nodeAddress)).setEnums(SupplyTempSensor.getEnumStringDefinition())
                .setTz(CCUHsApi.getInstance().getTimeZone())
                .build();
        String supplyAirTempTypeId =CCUHsApi.getInstance().addPoint(supplyAirTemperatureType);
        CCUHsApi.getInstance().writeDefaultValById(supplyAirTempTypeId, 0.0);

        Point roomTemperatureType = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-roomTemperatureType")
                .setEquipRef(equip.getId()).setRoomRef(equip.getRoomRef())
                .setSiteRef(equip.getSiteRef()).setFloorRef(equip.getFloorRef())
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("space").addMarker("sp").addMarker("type").addMarker("temp")
                .setGroup(String.valueOf(nodeAddress)).setEnums(SupplyTempSensor.getEnumStringDefinition())
                .setTz(CCUHsApi.getInstance().getTimeZone())
                .build();
        String roomTempTypeId =CCUHsApi.getInstance().addPoint(roomTemperatureType);
        CCUHsApi.getInstance().writeDefaultValById(roomTempTypeId, 0.0);

        HashMap<Object, Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);

        CCUHsApi.getInstance().syncEntityTree();

    }

    private static void deleteExistingLogicalAndConfigPoints(CCUHsApi instance, Equip equip) {

        HashMap<Object,Object> mainSensorPoint = instance.readEntity("point and main and " +
                "temperature and ti and equipRef == \""+equip.getId()+"\"");
        if (!mainSensorPoint.isEmpty()) {
            CCUHsApi.getInstance().deleteEntity(mainSensorPoint.get("id").toString());
        }

        HashMap<Object,Object> th1ConfigPoint = instance.readEntity("point and th1 and " +
                "enable and ti and equipRef == \""+equip.getId()+"\"");
        if (!th1ConfigPoint.isEmpty()) {
            CCUHsApi.getInstance().deleteEntity(th1ConfigPoint.get("id").toString());
        }

        HashMap<Object,Object> th2ConfigPoint = instance.readEntity("point and th2 and " +
                "enable and ti and equipRef == \""+equip.getId()+"\"");
        if (!th2ConfigPoint.isEmpty()) {
            CCUHsApi.getInstance().deleteEntity(th2ConfigPoint.get("id").toString());
        }

    }

    private static void doAirTagMigration(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> hsEquips = ccuHsApi.readAllEntities("equip and hyperstat");
        for (HashMap<Object, Object> hsEquip : hsEquips) {
            String equipRef = hsEquip.get("id").toString();
            ArrayList<HashMap<Object, Object>> sensorPoints = ccuHsApi.readAllEntities("point and hyperstat and sensor and (co2 or voc) and equipRef == \"" +equipRef+"\"");
            String updatedTag = "air";
            for (HashMap<Object, Object> sensorPoint : sensorPoints) {
                Point updatedPoint = new Point.Builder().setHashMap(sensorPoint).addMarker(updatedTag).build();
                CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
            }
        }
    }

    private static void MigrateTIChanges(CCUHsApi instance) {
        ArrayList<HashMap<Object, Object>> tiEquips = instance.readAllEntities("equip and ti");
        for (HashMap<Object, Object> equip:tiEquips) {
            String equipRef = equip.get("id").toString();
            HashMap<Object,Object> currentTemp = instance.readEntity("point and current and " +
                    "temp and ti and equipRef == \""+equipRef+"\"");
            String nodeAddress = currentTemp.get("group").toString();
            ControlMote.setPointEnabled(Integer.valueOf(nodeAddress), Port.TH1_IN.name(), false);
            ControlMote.setPointEnabled(Integer.valueOf(nodeAddress), Port.TH2_IN.name(), false);
            ControlMote.updatePhysicalPointRef(Integer.valueOf(nodeAddress), Port.SENSOR_RT.name(),
                    currentTemp.get("id").toString());
        }
    }

    private static boolean hasTIProfile(CCUHsApi instance) {
        ArrayList<HashMap<Object, Object>> tiEquips = instance.readAllEntities("equip and ti");
        return !tiEquips.isEmpty();
    }

    private static void migrateHisInterpolateIssueFix(CCUHsApi instance) {
        ArrayList<HashMap<Object, Object>> hyperstatEquips = instance.readAllEntities("equip and hyperstat");
        hyperstatEquips.forEach(rawEquip -> {
            Equip equip = new Equip.Builder().setHashMap(rawEquip).build();

            HashMap<Object, Object> covThresholdPoint = instance.readEntity ("point and hyperstat and voc and threshold and equipRef == \"" +equip.getId()+"\"");
            HashMap<Object, Object> covTargetPoint = instance.readEntity ("point and hyperstat and voc and target and equipRef == \"" +equip.getId()+"\"" );
            HashMap<Object, Object> pm2p5ThresholdPoint = instance.readEntity ("point and hyperstat and pm2p5 and threshold and equipRef == \"" +equip.getId()+"\"" );
            HashMap<Object, Object> pm2p5TargetPoint = instance.readEntity ("point and hyperstat and pm2p5 and target and equipRef == \"" +equip.getId()+"\"" );
            HashMap<Object, Object> damperOpeningRate = instance.readEntity ("point and hyperstat and damper and opening and rate and equipRef == \"" +equip.getId()+"\"" );
            HashMap<Object, Object> co2Target = instance.readEntity ("point and hyperstat and target and co2 and equipRef == \"" +equip.getId()+"\"" );
            HashMap<Object, Object> co2Threshold = instance.readEntity ("point and hyperstat and threshold and co2 and equipRef == \"" +equip.getId()+"\"" );


            if(!covThresholdPoint.isEmpty()) {
                Point covThresholdRowPoint = new Point.Builder().setHashMap(covThresholdPoint).setHisInterpolate("cov").build();
                instance.updatePoint(covThresholdRowPoint, covThresholdRowPoint.getId());
            }
            if(!covTargetPoint.isEmpty()) {
                Point covTargetRowPoint = new Point.Builder().setHashMap(covTargetPoint).setHisInterpolate("cov").build();
                instance.updatePoint(covTargetRowPoint,covTargetRowPoint.getId());
            }
            if(!pm2p5ThresholdPoint.isEmpty()) {
                Point pm2p5ThresholdRowPoint = new Point.Builder().setHashMap(pm2p5ThresholdPoint).setHisInterpolate("cov").build();
                instance.updatePoint(pm2p5ThresholdRowPoint,pm2p5ThresholdRowPoint.getId());
            }
            if(!pm2p5TargetPoint.isEmpty()) {
                Point pm2p5TargetRowPoint = new Point.Builder().setHashMap(pm2p5TargetPoint).setHisInterpolate("cov").build();
                instance.updatePoint(pm2p5TargetRowPoint, pm2p5TargetRowPoint.getId());
            }

            if(!damperOpeningRate.isEmpty()) {
                Point damperOpeningRawRate = new Point.Builder().setHashMap(damperOpeningRate).setHisInterpolate("cov").build();
                instance.updatePoint(damperOpeningRawRate, damperOpeningRawRate.getId());
            }
            if(!co2Threshold.isEmpty()) {
                Point co2ThresholdRawPoint = new Point.Builder().setHashMap(co2Threshold).setHisInterpolate("cov").build();
                instance.updatePoint(co2ThresholdRawPoint, co2ThresholdRawPoint.getId());
            }
            if(!co2Target.isEmpty()) {
                Point co2RawTarget = new Point.Builder().setHashMap(co2Target).setHisInterpolate("cov").build();
                instance.updatePoint(co2RawTarget, co2RawTarget.getId());
            }
        });
    }
    private static void migrateVocPm2p5(CCUHsApi instance) {
        ArrayList<HashMap<Object, Object>> hyperstatEquips = instance.readAllEntities("equip and hyperstat");
        hyperstatEquips.forEach(rawEquip -> {
            Equip equip = new Equip.Builder().setHashMap(rawEquip).build();

            boolean isCovThresholdExist = isPointExist ("point and hyperstat and voc and threshold and equipRef == \"" +equip.getId()+"\"" ,instance);
            boolean isCovTargetExist = isPointExist ("point and hyperstat and voc and target and equipRef == \"" +equip.getId()+"\"" ,instance);
            boolean isPm2p5ThresholdExist = isPointExist ("point and hyperstat and pm2p5 and threshold and equipRef == \"" +equip.getId()+"\"" ,instance);
            boolean isPm2p5TargetExist = isPointExist ("point and hyperstat and pm2p5 and target and equipRef == \"" +equip.getId()+"\"" ,instance);

            HyperStatPointsUtil hyperStatPointsUtil = HyperStatReconfigureUtil.Companion.getEquipPointsUtil(equip, instance);

            List<Pair<Point, Object>> list = hyperStatPointsUtil.createPointVOCPmConfigPoint(
                    equip.getDisplayName(), 1000, 1000, 1000, 1000
            );
            list.forEach(pointObjectPair -> {
                if((pointObjectPair.getFirst().getDisplayName().contains("zoneVOCThreshold") && !isCovThresholdExist)
                        ||(pointObjectPair.getFirst().getDisplayName().contains("zoneVOCTarget") && !isCovTargetExist)
                        ||(pointObjectPair.getFirst().getDisplayName().contains("zonePm2p5Threshold") && !isPm2p5ThresholdExist)
                        ||(pointObjectPair.getFirst().getDisplayName().contains("zonePm2p5Target") && !isPm2p5TargetExist)){
                    pushPointToHS(hyperStatPointsUtil,pointObjectPair);
                }
            });
        });
    }

    private static void pushPointToHS(HyperStatPointsUtil hyperStatPointsUtil, Pair<Point, Object> pointDetails ){
        String pointId = hyperStatPointsUtil.addPointToHaystack(pointDetails.getFirst());
        if (pointDetails.getFirst().getMarkers().contains("his")) {
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, pointDetails.getSecond());
        }
        if (pointDetails.getFirst().getMarkers().contains("writable")) {
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, pointDetails.getSecond());
        }
    }

    private static boolean isPointExist(String query, CCUHsApi ccuHsApi){
      return !ccuHsApi.readEntity(query).isEmpty();
    }

    private static void updateAnalogInputDisplayNameForSense(){
        if(!CCUHsApi.getInstance().readEntity(Tags.SITE).isEmpty()) {
            PointMigrationHandler.updateSenseAnalogInputUnitPointDisplayName(Tags.ANALOG1);
            PointMigrationHandler.updateSenseAnalogInputUnitPointDisplayName(Tags.ANALOG2);
        }
    }

    private static void updateAnalogInputDisplayNameForPILOOP(){
        if(!CCUHsApi.getInstance().readEntity(Tags.SITE).isEmpty()) {
            PointMigrationHandler.updatePILoopAnalog1InputUnitPointDisplayName();
            PointMigrationHandler.updatePILoopAnalog2InputUnitPointDisplayName();
        }
    }

    private static void addTIThermisters(CCUHsApi ccuHsApi) {
        Log.d(TAG,"addTIThermisters++");
        HashMap<Object,Object> tiEquip = ccuHsApi.readEntity("equip and ti");
        if(!tiEquip.isEmpty()) {
            Log.d(TAG,"ti isnt empty");
            String tiEquipRef = tiEquip.get("id").toString();
            HashMap<Object, Object> currentTemp = ccuHsApi.readEntity("point and current and " +
                    "temp and ti and equipRef == \"" + tiEquipRef + "\"");
            String nodeAddress = currentTemp.get("group").toString();
            createTIThermisterPoints(tiEquipRef,nodeAddress);
        }
    }

    private static boolean isTIThermisterMigrated() {
        Log.d(TAG,"isTIThermisterMigrated");
        HashMap<Object,Object> th1Config = CCUHsApi.getInstance().readEntity("point and ti and " +
                "config and th1");
        HashMap<Object,Object> th2Config = CCUHsApi.getInstance().readEntity("point and ti and " +
                "config and th2");
        return !th1Config.isEmpty() && !th2Config.isEmpty();
    }

    private static void doDiagPointsMigration(CCUHsApi ccuHsApi) {

        // approach is deleting all the daig point which does not have any gateway reff.
        // Because in server we will never get to know these diag points are belongs which ccu
        // Create create fresh daig points.

        HashMap<Object, Object> ccu = ccuHsApi.readEntity("device and ccu");
        if (ccu.isEmpty()) {
            Log.i(TAG_CCU_MIGRATION_UTIL, "doDiagPointsMigration: ");
            return;
        }

        HashMap<Object, Object> diag = ccuHsApi.readEntity("equip and diag");
        if (!diag.isEmpty()) {
            Log.i(TAG_CCU_MIGRATION_UTIL, "diag points are available ");
            // Diag are present so check with gatewayRef
            Equip diagEquip = new Equip.Builder().setHashMap(diag).build();
            if(!diagEquip.getMarkers().contains("gatewayRef")){
                // Update gateway reff
                Log.i(TAG_CCU_MIGRATION_UTIL, "adding gateway reference");
                ccuHsApi.updateDiagGatewayRef(ccu.get("gatewayRef").toString());
            }
        }else{
            Log.i(TAG_CCU_MIGRATION_UTIL, "Diag points are not available Restoring daig equips");
            // Locally diag points are missing check at silo
            new RestoreCCU().getDiagEquipOfCCU(ccu.get("equipRef").toString());

        }



    }

    private static void airflowUnitMigration(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> airflowPoints = ccuHsApi.readAllEntities("point and airflow and sense and unit");
        String updatedAirflowUnit = "cfm";
        for (HashMap<Object, Object> airflow : airflowPoints) {
            Point updatedPoint = new Point.Builder().setHashMap(airflow).setUnit(updatedAirflowUnit).build();
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        }
    }

    private static void relayDeactivationAndReheatZoneToDATMigration(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> relayDeactivationPointAll = ccuHsApi.readAllEntities("point and tuner and relay and deactivation and hysteresis");
        ArrayList<HashMap<Object, Object>> reheatZoneToDATMinDifferentialPoint = ccuHsApi.readAllEntities("point and tuner and reheat and differential");
        String updatedMaxValue = "60";
        for (HashMap<Object, Object> relayDeactivationHysteresisDab : relayDeactivationPointAll) {
            Point updatedPoint = new Point.Builder().setHashMap(relayDeactivationHysteresisDab).setMaxVal(updatedMaxValue).build();
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        }

        for (HashMap<Object, Object> reheatZoneToDATMinDifferential : reheatZoneToDATMinDifferentialPoint) {
            Point updatedPoint = new Point.Builder().setHashMap(reheatZoneToDATMinDifferential).setMaxVal(updatedMaxValue).build();
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        }

    }

    private static void timerCounterAndCFMCoolingMigration(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> timerCounterPoint = ccuHsApi.readAllEntities("point and tuner and timer and counter");
        ArrayList<HashMap<Object, Object>> maxCFMCoolingPoint = ccuHsApi.readAllEntities("point and trueCfm and cooling and max");
        ArrayList<HashMap<Object, Object>> minCFMCoolingPoint = ccuHsApi.readAllEntities("point and trueCfm and cooling and min");
        String updatedMaxValue = "60";
        String updatedIncrementalValue = "5";
        for (HashMap<Object, Object> timerCounter : timerCounterPoint) {
            Point updatedPoint = new Point.Builder().setHashMap(timerCounter).setMaxVal(updatedMaxValue).build();
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        }
        for (HashMap<Object, Object> maxCfmCooling : maxCFMCoolingPoint) {
            Point updatedPoint = new Point.Builder().setHashMap(maxCfmCooling).setMaxVal(updatedMaxValue).setIncrementVal(updatedIncrementalValue).build();
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        }
        for (HashMap<Object, Object> minCfmCooling : minCFMCoolingPoint) {
            Point updatedPoint = new Point.Builder().setHashMap(minCfmCooling).setIncrementVal(updatedIncrementalValue).build();
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        }
    }

    private static void pressureUnitMigration(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip");
        equips.forEach(equipDetails -> {
            Equip equip = new Equip.Builder().setHashMap(equipDetails).build();
            ArrayList<HashMap<Object, Object>> pressurePoints = ccuHsApi.readAllEntities("point and (pressure or staticPressure) and equipRef == \"" + equip.getId() + "\"");
            String updatedPressureUnit = "inH₂O";
            for (HashMap<Object, Object> pressureMap : pressurePoints
            ) {
                Point updatedPoint = new Point.Builder().setHashMap(pressureMap).setUnit(updatedPressureUnit).build();
                CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
            }
        });
    }

    private static void trueCFMVAVMigration(CCUHsApi haystack) {
       ArrayList<HashMap<Object, Object>> vavEquips = haystack.readAllEntities("equip and vav and not system");
        HashMap<Object,Object> tuner = CCUHsApi.getInstance().readEntity("equip and tuner");
        Equip tunerEquip = new Equip.Builder().setHashMap(tuner).build();
        if(!vavEquips.isEmpty()) {
            doMigrationVav(haystack, vavEquips, tunerEquip);
        }
    }

    private static void doMigrationVav(CCUHsApi haystack, ArrayList<HashMap<Object,Object>>vavEquips, Equip tunerEquip) {
        //        creating default tuners for vav
        TrueCFMTuners.createDefaultTrueCfmTuners(haystack, tunerEquip, TunerConstants.VAV_TAG, TunerConstants.VAV_TUNER_GROUP);
        vavEquips.forEach(vavEquip -> {
            HashMap<Object, Object> enableCFMPoint = haystack.readEntity(
                "enable and point and trueCfm and equipRef == \"" + vavEquip.get("id") + "\"");
            if (enableCFMPoint.get("id") == null) {
                Equip equip = new Equip.Builder().setHashMap(vavEquip).build();
                String fanMarker = "";
                if (equip.getProfile().equals(ProfileType.VAV_SERIES_FAN.name())) {
                    fanMarker = "series";
                } else if (equip.getProfile().equals(ProfileType.VAV_PARALLEL_FAN.name())) {
                    fanMarker = "parallel";
                }
                TrueCFMPointsHandler.createTrueCFMControlPoint(haystack, equip, Tags.VAV, 0, fanMarker);
            }
        });
    }

    private static void trueCFMDABMigration(CCUHsApi haystack) {
        ArrayList<HashMap<Object, Object>> dabEquips = haystack.readAllEntities("equip and dab and not system");
        if(!dabEquips.isEmpty()) {
            doMigrationDab(haystack, dabEquips);
        }
    }
    private static void doMigrationDab(CCUHsApi haystack, ArrayList<HashMap<Object,Object>>dabEquips) {
        dabEquips.forEach(dabEquip -> {
            HashMap<Object, Object> enableCFMPoint = haystack.readEntity(
                "enable and point and trueCfm and dab and equipRef == \"" + dabEquip.get("id") + "\"");
            if (enableCFMPoint.get("id") == null) {
                Equip equip = new Equip.Builder().setHashMap(dabEquip).build();
                TrueCFMPointsHandler.createTrueCFMControlPoint(haystack, equip, Tags.DAB, 0, null);
            }
        });
    }



    private static void addUnitToTuners(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip");
        equips.forEach(equipDetails -> {
            Equip equip = new Equip.Builder().setHashMap(equipDetails).build();
            ArrayList<HashMap<Object, Object>> temperatureProportionalRange = ccuHsApi.readAllEntities("pspread and not standalone and equipRef == \"" + equip.getId() + "\"");
            ArrayList<HashMap<Object, Object>> zonePrioritySpread = ccuHsApi.readAllEntities("zone and priority and spread and equipRef == \"" + equip.getId() + "\"");
            ArrayList<HashMap<Object, Object>> buildingToZoneDifferential = ccuHsApi.readAllEntities("zone and building and differential and equipRef == \"" + equip.getId() + "\"");
            ArrayList<HashMap<Object, Object>> userLimitSpread = ccuHsApi.readAllEntities("user and limit and spread and equipRef == \"" + equip.getId() + "\"");

            for (HashMap<Object, Object> unitMap : temperatureProportionalRange) {
                Point updatedPoint = new Point.Builder().setHashMap(unitMap).setUnit(Units.FAHRENHEIT).build();
                CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
            }

            for (HashMap<Object, Object> unitMap : zonePrioritySpread) {
                Point updatedPoint = new Point.Builder().setHashMap(unitMap).setUnit(Units.FAHRENHEIT).build();
                CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
            }

            for (HashMap<Object, Object> unitMap : buildingToZoneDifferential) {
                Point updatedPoint = new Point.Builder().setHashMap(unitMap).setUnit(Units.FAHRENHEIT).build();
                CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
            }

            for (HashMap<Object, Object> unitMap : userLimitSpread) {
                Point updatedPoint = new Point.Builder().setHashMap(unitMap).setUnit(Units.FAHRENHEIT).build();
                CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
            }
        });
    }

    private static boolean checkAppVersionUpgraded() {

        PackageManager manager = Globals.getInstance().getApplicationContext().getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(Globals.getInstance().getApplicationContext().getPackageName(), 0);
            String appVersion = info.versionName + "." + info.versionCode;
            if (!PreferenceUtil.getMigrationVersion().equals(appVersion)) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void updateAhuRefForBposEquips(CCUHsApi hayStack) {
        ArrayList<HashMap> bposEquips = CCUHsApi.getInstance().readAll("equip and bpos");
        HashMap systemEquip = hayStack.read("equip and system");
        if (systemEquip.isEmpty()) {
            return;
        }
        String systemEquipId = systemEquip.get("id").toString();
        for (HashMap equipMap : bposEquips) {
            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            equip.setAhuRef(systemEquipId);
            CcuLog.i(L.TAG_CCU, "BPOSAhuRef update equip "+equip.getDisplayName()+" "+systemEquipId);
            hayStack.updateEquip(equip, equip.getId());
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

    /**
     * Addresses an issue pre-exisiting builds prior to 1.597.0 where system was following building
     * schedule for certain zones even after enabling zone schedules.
     * @param hayStack
     */
    private static void updateZoneScheduleTypes(CCUHsApi hayStack) {
        List<HashMap<Object,Object>> allScheduleTypePoints = hayStack.readAllEntities("point and scheduleType");
        Set<String> zoneRefs = new HashSet<>();

        allScheduleTypePoints.forEach( scheduleType -> {
            Point scheduleTypePoint = new Point.Builder().setHashMap(scheduleType).build();
            double scheduleTypeVal = hayStack.readPointPriorityVal(scheduleTypePoint.getId());
            if (scheduleTypeVal == ScheduleType.ZONE.ordinal() &&
                scheduleTypePoint.getRoomRef() != null &&
                scheduleTypePoint.getRoomRef() != "SYSTEM") {
                zoneRefs.add(scheduleTypePoint.getRoomRef());
            }
        });

        zoneRefs.forEach( zoneRef -> {
            HashMap<Object, Object> zone = hayStack.readMapById(zoneRef);
            CcuLog.i(L.TAG_CCU_SCHEDULER, " ZoneScheduleMigration "+zone);
            if (zone.get("scheduleRef") != null) {
                Schedule zoneSchedule = hayStack.getScheduleById(zone.get("scheduleRef").toString());
                //If the zone schedule is currently disabled , then enable it.
                if (zoneSchedule != null &&
                    zoneSchedule.isZoneSchedule() &&
                    zoneSchedule.getDisabled()) {
                    zoneSchedule.setDisabled(false);
                    hayStack.updateScheduleNoSync(zoneSchedule, zone.get("id").toString());
                    CcuLog.i(L.TAG_CCU_SCHEDULER, " Migrated Schedule "+zone+" : "+zoneSchedule);
                }
            }
        });

    }

    private static void cleanUpDuplicateZoneSchedules(CCUHsApi hayStack) {
        CcuLog.i("MIGRATION_UTIL", " cleanUpDuplicateZoneSchedules ");
        List<HashMap<Object,Object>> rooms = hayStack.readAllEntities("room");

        rooms.forEach( zoneMap -> {
            Zone zone = new Zone.Builder().setHashMap(zoneMap).build();
            List<HashMap<Object,Object>> zoneSchedules = hayStack.readAllEntities("schedule and not vacation and " +
                                                                                  "roomRef == "+zone.getId());

            //A zone is expected to have only one zone schedule.
            if (zoneSchedules.size() > 1) {
                zoneSchedules.forEach( schedule -> {
                    CcuLog.i("MIGRATION_UTIL", " cleanUpDuplicateZoneSchedules Zone: "+zoneMap+" Schedule "+schedule);
                    HashMap<Object, Object> scheduleHashMap = CCUHsApi.getInstance().readEntity("schedule and roomRef " +
                            "== " +zone.getId());
                    Schedule scheduleById = CCUHsApi.getInstance().getScheduleById(scheduleHashMap.get("id").toString());
                    if (scheduleById == null) {
                        CcuLog.i("MIGRATION_UTIL", " Not ideal , there is a zone without zone schedule !!!!!");
                        Schedule zoneSchedule = hayStack.getScheduleById(schedule.get("id").toString());
                        zone.setScheduleRef(schedule.get("id").toString());
                        hayStack.updateZone(zone, zone.getId());
                        hayStack.updateZoneSchedule(zoneSchedule, zone.getId());
                    } else if (!schedule.get("id").toString().equals(scheduleById)) {
                        hayStack.deleteEntity(schedule.get("id").toString());
                    }
                });
            } else if (zoneSchedules.size() == 1){
                CcuLog.i("MIGRATION_UTIL",
                         " No duplicate schedule for Zone: "+zoneMap+" : "+zoneSchedules.get(0));
            } else {
                CcuLog.i("MIGRATION_UTIL", "No zone schedule for "+zoneMap);
            }
        });
    }

    private static void addCCUHeartbeatDiagPoint(){
        Map<Object,Object> diagEquip = CCUHsApi.getInstance().readEntity("equip and diag");
        if(!diagEquip.isEmpty()){
            Map<Object,Object> cloudConnectivityPoint = CCUHsApi.getInstance().readEntity("cloud and connectivity " +
                    " and diag and point");
            if(!cloudConnectivityPoint.isEmpty()) {
                CCUHsApi.getInstance().deleteEntity(cloudConnectivityPoint.get("id").toString());
            }
            Map<Object,Object> cloudConnectedPoint = CCUHsApi.getInstance().readEntity("cloud and connected" +
                    " and diag and point");
            if(cloudConnectedPoint.isEmpty()){
                String equipRef = diagEquip.get("id").toString();
                String equipDis = "DiagEquip";
                String siteRef = diagEquip.get("siteRef").toString();
                String tz = diagEquip.get("tz").toString();
                CCUHsApi.getInstance().addPoint(DiagEquip.getDiagHeartbeatPoint(equipRef, equipDis, siteRef, tz));
            }
        }
    }


    private static void doDamperFeedbackMigration(CCUHsApi haystack){
        Log.i(TAG_CCU_MIGRATION_UTIL, "doDamperFeedbackMigration: ");
        ArrayList<HashMap<Object, Object>> vavEquips = haystack.readAllEntities("equip and vav and not system");
        ArrayList<HashMap<Object, Object>> dabEquips = haystack.readAllEntities("equip and dab and not system");
        ArrayList<HashMap<Object, Object>> dualDuctEquips = haystack.readAllEntities("equip and dualDuct and not system");
        Log.i(TAG_CCU_MIGRATION_UTIL, "doDamperFeedbackMigration: vavEquips "+vavEquips.size());
        Log.i(TAG_CCU_MIGRATION_UTIL, "doDamperFeedbackMigration: dabEquips "+dabEquips.size());
        Log.i(TAG_CCU_MIGRATION_UTIL, "doDamperFeedbackMigration: dualDuctEquips "+dualDuctEquips.size());
        if(!vavEquips.isEmpty()){
            doMigrateVav(vavEquips,haystack);
        }
        if(!dabEquips.isEmpty()){
            doMigrateDAB(dabEquips,haystack);
        }
        if(!dualDuctEquips.isEmpty()){
            doMigrateDualDuct(dualDuctEquips,haystack);
        }
    }


    private static void doMigrateVav(ArrayList<HashMap<Object, Object>> vavEquips, CCUHsApi haystack){
        vavEquips.forEach(equip -> {
            Log.i(TAG_CCU_MIGRATION_UTIL, "Equip Id : "+equip.get("id"));
            try{
                HashMap<Object, Object> feedbackPoint =
                        CCUHsApi.getInstance().readEntity
                                ("point and damper and sensor and equipRef == \"" + equip.get("id")+"\"");
                if(feedbackPoint.isEmpty()){

                    Log.i(TAG_CCU_MIGRATION_UTIL, "feedbackPoints not found ");
                    Equip actualEquip = new Equip.Builder().setHashMap(equip).build();

                    String fanMarker = "";
                    if (actualEquip.getProfile().equals(ProfileType.VAV_SERIES_FAN.name())) {
                        fanMarker = "series";
                    } else if (actualEquip.getProfile().equals(ProfileType.VAV_PARALLEL_FAN.name())) {
                        fanMarker = "parallel";
                    }

                    Log.i(TAG_CCU_MIGRATION_UTIL, "doMigrateVav  : doing fanMarker " +fanMarker);
                    int nodeAddress = Integer.parseInt(actualEquip.getGroup());
                    String damperFeedbackID = VavEquip.createFeedbackPoint(
                            haystack,nodeAddress,actualEquip.getDisplayName(),actualEquip.getId()
                            ,actualEquip.getSiteRef(),actualEquip.getRoomRef(),actualEquip.getFloorRef(),fanMarker,actualEquip.getTz());
                    RawPoint rawPoint = SmartNode.getPhysicalPoint(nodeAddress, ANALOG_OUT_ONE.toString());
                    SmartNode.setPointEnabled(nodeAddress, Port.ANALOG_IN_ONE.name(),true);
                    SmartNode.updatePhysicalPointRef(nodeAddress,Port.ANALOG_IN_ONE.name(),damperFeedbackID);
                    SmartNode.updatePhysicalPointType(nodeAddress,Port.ANALOG_IN_ONE.name(),rawPoint.getType());
                    haystack.writeHisValueByIdWithoutCOV(damperFeedbackID,0.0);
                    Log.i(TAG_CCU_MIGRATION_UTIL, "doMigrateVav  : Done "+actualEquip.getGroup());
                    Log.i(TAG_CCU_MIGRATION_UTIL, "doMigrateVav  : node.analog1Out.getType() "+rawPoint.getType());

                }else
                    Log.i(TAG_CCU_MIGRATION_UTIL, "feedbackPoints vav are found ");
            }catch (Exception e){
                Log.i(TAG_CCU_MIGRATION_UTIL, "error while doing vav migration  "+e.getMessage());
            }

        });
    }


    private static void doMigrateDAB(ArrayList<HashMap<Object, Object>> dabEquips, CCUHsApi haystack){
        dabEquips.forEach(equip -> {
            try{

                HashMap<Object, Object> feedbackPoint =
                        CCUHsApi.getInstance().readEntity
                                ("point and damper and sensor and equipRef == \"" + equip.get("id")+"\"");
                if(feedbackPoint.isEmpty()) {
                    Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
                    int nodeAddress = Integer.parseInt(actualEquip.getGroup());
                    String damperFeedbackID = DabEquip.createFeedbackPoint(
                            CCUHsApi.getInstance(), nodeAddress, actualEquip.getDisplayName(),
                            actualEquip.getId(),
                            actualEquip.getSiteRef(), actualEquip.getRoomRef(), actualEquip.getFloorRef(), actualEquip.getTz());
                    SmartNode.setPointEnabled(nodeAddress, Port.ANALOG_IN_ONE.name(), true);
                    SmartNode.updatePhysicalPointRef(nodeAddress, Port.ANALOG_IN_ONE.name(), damperFeedbackID);
                    RawPoint rawPoint = SmartNode.getPhysicalPoint(nodeAddress, ANALOG_OUT_ONE.toString());
                    SmartNode.updatePhysicalPointType(nodeAddress,Port.ANALOG_IN_ONE.name(),rawPoint.getType());
                    haystack.writeHisValueByIdWithoutCOV(damperFeedbackID, 0.0);
                    Log.i(TAG_CCU_MIGRATION_UTIL, "doMigrateVav  : Done " + actualEquip.getGroup());
                }else
                    Log.i(TAG_CCU_MIGRATION_UTIL, "feedbackPoints dab are found ");
            }catch (Exception e){
                Log.i(TAG_CCU_MIGRATION_UTIL, "error while doing dab migration  "+e.getMessage());
            }

        });
    }
    private static void doMigrateDualDuct(ArrayList<HashMap<Object, Object>> dualDuctEquips, CCUHsApi haystack){
        dualDuctEquips.forEach(equip -> {
            try{
                HashMap<Object, Object> analog1FeedbackPoint =
                        CCUHsApi.getInstance().readEntity
                                ("point and damper and sensor and analog1 and equipRef == \"" + equip.get("id")+"\"");
                HashMap<Object, Object> analog2FeedbackPoint =
                        CCUHsApi.getInstance().readEntity
                                ("point and damper and sensor and analog1 and equipRef == \"" + equip.get("id")+"\"");

                Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
                int nodeAddress = Integer.parseInt(actualEquip.getGroup());
                if(analog1FeedbackPoint.isEmpty()) {

                    RawPoint rawPoint = SmartNode.getPhysicalPoint(nodeAddress, ANALOG_OUT_ONE.toString());
                    if (rawPoint.getEnabled()) {
                        String damperFeedbackID = DualDuctEquip.createFeedbackPoint(
                                CCUHsApi.getInstance(), nodeAddress, actualEquip.getDisplayName(),
                                actualEquip.getId(),
                                actualEquip.getSiteRef(), actualEquip.getRoomRef(), actualEquip.getFloorRef(),
                                actualEquip.getTz(), "analog1");
                        SmartNode.setPointEnabled(nodeAddress, Port.ANALOG_IN_ONE.name(), true);
                        SmartNode.updatePhysicalPointRef(nodeAddress, Port.ANALOG_IN_ONE.name(), damperFeedbackID);
                        haystack.writeHisValueByIdWithoutCOV(damperFeedbackID, 0.0);
                        SmartNode.updatePhysicalPointType(nodeAddress, Port.ANALOG_IN_ONE.name(), rawPoint.getType());
                    }
                }
                if(analog2FeedbackPoint.isEmpty()){
                    RawPoint rawPoint = SmartNode.getPhysicalPoint(nodeAddress, ANALOG_OUT_TWO.toString());
                    if (rawPoint.getEnabled()) {
                        String damperFeedbackID = DualDuctEquip.createFeedbackPoint(
                                CCUHsApi.getInstance(), nodeAddress, actualEquip.getDisplayName(),
                                actualEquip.getId(),
                                actualEquip.getSiteRef(), actualEquip.getRoomRef(), actualEquip.getFloorRef(),
                                actualEquip.getTz(), "analog1");
                        SmartNode.setPointEnabled(nodeAddress, Port.ANALOG_IN_TWO.name(), true);
                        SmartNode.updatePhysicalPointRef(nodeAddress, Port.ANALOG_IN_TWO.name(), damperFeedbackID);
                        haystack.writeHisValueByIdWithoutCOV(damperFeedbackID, 0.0);
                        SmartNode.updatePhysicalPointType(nodeAddress, Port.ANALOG_IN_TWO.name(), rawPoint.getType());
                    }

                    Log.i(TAG_CCU_MIGRATION_UTIL, "doMigrateDualDuct  : Done " + actualEquip.getGroup());
                }else
                    Log.i(TAG_CCU_MIGRATION_UTIL, "feedbackPoints DualDuct are found ");
            }catch (Exception e){
                Log.i(TAG_CCU_MIGRATION_UTIL, "error while doing DualDuct migration  "+e.getMessage());
            }
        });
    }

    private static void scheduleRefactorMigration(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> rooms = hayStack.readAllEntities("room");
        rooms.forEach( room -> {
            HashMap<Object, Object> occupancyState =
                hayStack.read("point and occupancy and state and roomRef ==\""+room.get("id")+"\"");
            if (occupancyState.isEmpty()) {
                Zone roomEntity = new Zone.Builder().setHashMap(room).build();
                hayStack.addZoneOccupancyPoint(roomEntity.getId(), roomEntity);
            }
        });

        ArrayList<HashMap<Object, Object>> hyperStatCpus = hayStack.readAllEntities("equip and hyperstat and cpu");
        hyperStatCpus.forEach( cpuEquip -> {
            HashMap<Object, Object> keyCardConfig = hayStack.readEntity("keycard and sensing and enabled and equipRef" +
                                                                        " == \""+cpuEquip.get("id")+"\"");

            if (keyCardConfig.isEmpty()) {

                Equip equip = new Equip.Builder().setHashMap(cpuEquip).build();

                HyperStatPointsUtil hyperStatPointsUtil = HyperStatReconfigureUtil.Companion.getEquipPointsUtil(equip, hayStack);

                hyperStatPointsUtil.createKeycardWindowSensingPoints().forEach(
                        point -> pushPointToHS(hyperStatPointsUtil, point)
                );
            }
        });

        ArrayList<HashMap<Object, Object>> occModePoints = hayStack.readAllEntities("occupancy and mode");
        occModePoints.forEach( occMode -> {
            Point occModePoint = new Point.Builder().setHashMap(occMode).build();
            occModePoint.setEnums(Occupancy.getEnumStringDefinition());
            hayStack.updatePoint(occModePoint, occModePoint.getId());
        });
        hayStack.scheduleSync();
    }

    private static void migrateNewOccupancy(CCUHsApi hsApi) {
        Log.d(TAG_CCU_MIGRATION_UTIL, "AutoForceOcccupied and Autoaway migration for DAB ");
        ArrayList<HashMap<Object, Object>> dabEquips = hsApi.readAllEntities("equip and dab and zone");
        ArrayList<HashMap<Object, Object>> dabDualDuctEquips = hsApi.readAllEntities("equip and dualDuct");
        ArrayList<HashMap<Object, Object>> sseEquips = hsApi.readAllEntities("equip and sse");
        ArrayList<HashMap<Object, Object>> vavNoFanEquips = hsApi.readAllEntities("equip and vav and zone and not fanPowered");
        ArrayList<HashMap<Object, Object>> vavSeriesEquips = hsApi.readAllEntities("equip and vav and series");
        ArrayList<HashMap<Object, Object>> vavParallelEquips = hsApi.readAllEntities("equip and vav and parallel");
        ArrayList<HashMap<Object, Object>> ssCPUEquips = hsApi.readAllEntities("equip and cpu and standalone and not hyperstat");
        ArrayList<HashMap<Object, Object>> ssHPUEquips = hsApi.readAllEntities("equip and hpu and standalone");
        ArrayList<HashMap<Object, Object>> ss2PFCUEquips = hsApi.readAllEntities("equip and pipe2 and fcu");
        ArrayList<HashMap<Object, Object>> ss4PFCUEquips = hsApi.readAllEntities("equip and pipe4 and fcu");


        if (!dabEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : dabEquips) {
                String profile = "dab";
                createPoints(equip,"DAB", profile, "");
            }
        }
        if (!dabDualDuctEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : dabDualDuctEquips) {
                String profile = "dualDuct";
                createPoints(equip,"DualDuct",profile,"");
            }
        }
        if (!vavNoFanEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : vavNoFanEquips) {
                String profile = "vav";
                createPoints(equip,"VAV", profile, "");
            }
        }
        if (!vavSeriesEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : vavSeriesEquips) {
                String profile = "vav";
                String tag = "series";
                createPoints(equip,"VAV", profile, tag);
            }
        }
        if (!vavParallelEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : vavParallelEquips) {
                String profile = "vav";
                String tag = "parallel";
                createPoints(equip,"VAV", profile, tag);
            }
        }

        if (!sseEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : sseEquips) {
                String profile = "sse";
                createPoints(equip, "SSE",profile, "");
            }
        }

        if (!ssCPUEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : ssCPUEquips) {
                String profile = "cpu";
                createPointsSS(equip, "CPU",profile, "standalone");
            }
        }
        if (!ssHPUEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : ssHPUEquips) {
                String profile = "hpu";
                createPointsSS(equip, "HPU",profile, "standalone");
            }
        }
        if(!ss2PFCUEquips.isEmpty()){
            for (HashMap<Object,Object> equip:ss2PFCUEquips) {
                String profile = "pipe2";
                createPointsSS(equip,"2PFCU",profile,"fcu");
            }
        }

        if(!ss4PFCUEquips.isEmpty()){
            for (HashMap<Object,Object> equip:ss4PFCUEquips) {
                String profile = "pipe4";
                createPointsSS(equip,"4PFCU",profile,"fcu");
            }
        }
    }

    private static void createPoints(HashMap<Object,Object> equip,String profileDisplayName,String profiletag,String tags){
        String nodeAddr = equip.get("group").toString();
        String floorRef = equip.get("floorRef").toString();
        String equipRef = equip.get("id").toString();
        String roomRef = equip.get("roomRef").toString();
        HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteRef = siteMap.get(Tags.ID).toString();
        String siteDis = siteMap.get("dis").toString();
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-"+profileDisplayName+"-"+nodeAddr;
        SmartNode sn = new SmartNode(Integer.valueOf(nodeAddr));
        ConfigUtil.Companion.addOccupancyPointsSN(sn,profiletag,siteRef,roomRef,floorRef,equipRef,tz,nodeAddr,equipDis);
        ConfigUtil.Companion.addConfigPoints(profiletag,siteRef,roomRef,floorRef,equipRef,tz,nodeAddr,equipDis,tags,0,0);
    }

    private static void createPointsSS(HashMap<Object,Object> equip,String profileDisplayName,String profiletag,String tags){
        String nodeAddr = equip.get("group").toString();
        String floorRef = equip.get("floorRef").toString();
        String equipRef = equip.get("id").toString();
        String roomRef = equip.get("roomRef").toString();
        HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteRef = siteMap.get(Tags.ID).toString();
        String siteDis = siteMap.get("dis").toString();
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-"+profileDisplayName+"-"+nodeAddr;
        ConfigUtil.Companion.addConfigPoints(profiletag,siteRef,roomRef,floorRef,equipRef,tz,nodeAddr,
                equipDis,tags,0,0);
    }

    private static boolean isFanControlDelayDefaultValueUpdated(CCUHsApi hsApi){
        Log.d(TAG_CCU_MIGRATION_UTIL,"FanControl check");
        HashMap<Object, Object> fanControlTuner =
                hsApi.readEntity("point and tuner and fan and control and time and delay");
        return !fanControlTuner.isEmpty();
    }

    private static void updateFanControlDefaultValue(CCUHsApi hsApi){
        Log.d(TAG_CCU_MIGRATION_UTIL,"FanControl update");
        ArrayList<HashMap<Object, Object>> fanControlTunerAll =
                hsApi.readAllEntities("point and tuner and fan and control and time and delay");
        if(!fanControlTunerAll.isEmpty()) {
            for (HashMap<Object, Object> fanControlTuner: fanControlTunerAll) {
                hsApi.clearPointArrayLevel(fanControlTuner.get("id").toString(), TUNER_EQUIP_VAL_LEVEL, false);

                hsApi.writePointForCcuUser(fanControlTuner.get("id").toString(), TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                        TunerConstants.DEFAULT_FAN_ON_CONTROL_DELAY, 0);
            }
        }
    }

    private static void updateScheduleRefs(CCUHsApi hayStack) {
        CcuLog.i("MIGRATION_UTIL", " updateScheduleRefs ");
        List<HashMap<Object,Object>> rooms = hayStack.readAllEntities("room");

        rooms.forEach( zoneMap -> {
            Zone zone = new Zone.Builder().setHashMap(zoneMap).build();
            if (zone.getScheduleRef() == null) {
                CcuLog.i("MIGRATION_UTIL", " updateScheduleRefs : for Zone "+zone.getDisplayName());
                Map<Object,Object> zoneScheduleMap = hayStack.readEntity("schedule and not vacation and " +
                                                                                      "not special and roomRef == "+zone.getId());
                if (!zoneScheduleMap.isEmpty()) {
                    zone.setScheduleRef(zoneScheduleMap.get("id").toString());
                    hayStack.updateZone(zone, zone.getId());
                }
            }
        });
    }

    private static void createTIThermisterPoints(String tiEquipRef,String nodeAddress){
        Log.d("TIThermistor","createTIThermisterPoints");
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteRef = siteMap.get(Tags.ID).toString();
        String siteDis = siteMap.get("dis").toString();
        String equipDis = siteDis + "-TI-" + nodeAddress;
        String tz = siteMap.get("tz").toString();
        Point mainSensor = new Point.Builder()
                .setDisplayName(equipDis+"-mainTemperatureSensor")
                .setEquipRef(tiEquipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("main").addMarker("current").addMarker("temperature").addMarker("sp").addMarker("enable")
                .setGroup((nodeAddress))
                .setTz(tz)
                .build();
        String mainSensorId = CCUHsApi.getInstance().addPoint(mainSensor);
        hayStack.writeDefaultValById(mainSensorId, 1.0);

        Point th1Config = new Point.Builder()
                .setDisplayName(equipDis+"-th1")
                .setEquipRef(tiEquipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("th1").addMarker("sp").addMarker("enable")
                .setGroup((nodeAddress))
                .setTz(tz)
                .build();
        String th1ConfigId = CCUHsApi.getInstance().addPoint(th1Config);
        hayStack.writeDefaultValById(th1ConfigId, 0.0);

        Point th2Config = new Point.Builder()
                .setDisplayName(equipDis+"-th2")
                .setEquipRef(tiEquipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("th2").addMarker("sp").addMarker("enable")
                .setGroup((nodeAddress))
                .setTz(tz)
                .build();
        String th2ConfigId =CCUHsApi.getInstance().addPoint(th2Config);
        hayStack.writeDefaultValById(th2ConfigId, 0.0);

        HashMap<Object,Object> currentTemp = hayStack.readEntity("point and current and " +
                "temp and ti and equipRef == \""+tiEquipRef+"\"");
        ControlMote.setPointEnabled(Integer.valueOf(nodeAddress), Port.TH1_IN.name(), false);
        ControlMote.setPointEnabled(Integer.valueOf(nodeAddress), Port.TH2_IN.name(), false);
        ControlMote.updatePhysicalPointRef(Integer.valueOf(nodeAddress), Port.SENSOR_RT.name(), currentTemp.get("id").toString());

        Log.d("TIThermistor","createTIThermisterPoints completed");
    }

    private static void updateScheduleRefForZones(CCUHsApi hayStack){
        CcuLog.i(TAG_CCU_MIGRATION_UTIL, " updating scheduleRef for room entity ");
        List<HashMap<Object,Object>> rooms = hayStack.readAllEntities("room");
        List<HashMap<Object,Object>> scheduleTypes = hayStack.readAllEntities("scheduleType and point");
        for(HashMap<Object,Object> room : rooms){
            for(HashMap<Object,Object> scheduleType : scheduleTypes){
                if(room.containsKey("scheduleRef") && isRefEqualsToId(scheduleType.get("roomRef").toString(),
                        room.get("id").toString()) &&
                        !isZoneFollowingNamedSchedule(hayStack, scheduleType)){
                    HashMap<Object, Object> zoneScheduleMap = hayStack.readEntity("schedule and not vacation and " +
                            "not special and roomRef == " +room.get("id"));
                    if(zoneScheduleMap.isEmpty()){
                        CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Seems like no schedule entity has roomRef: "+room.get("id"));
                        continue;
                    }
                    Schedule zoneSchedule =
                            CCUHsApi.getInstance().getScheduleById(zoneScheduleMap.get("id").toString());
                    Zone zone = new Zone.Builder().setHashMap(room).build();
                    zone.setScheduleRef(zoneSchedule.getId());
                    hayStack.updateZone(zone, zone.getId());
                }
            }
            if(!room.containsKey("scheduleRef")){
                CcuLog.i(TAG_CCU_MIGRATION_UTIL, room.get("id").toString() +" does not have scheduleRef");
            }
        }
    }

    private static boolean isZoneFollowingNamedSchedule(CCUHsApi hayStack, HashMap<Object,Object> scheduleType){
        return hayStack.readHisValById(scheduleType.get("id").toString()).intValue() == ScheduleType.NAMED.ordinal();
    }

    private static boolean isRefEqualsToId(String ref, String id){
        ref = ref.startsWith("@")? ref.substring(1) : ref;
        id = id.startsWith("@")? id.substring(1) : id;
        return ref.equals(id);
    }

    private static void updateStageTimerForDAB(CCUHsApi haystack) {
        HashMap<Object, Object> systemEquip = haystack.readEntity("equip and dab and system");

        if (!systemEquip.isEmpty()) {
            Equip equipMap = new Equip.Builder().setHashMap(systemEquip).build();
            String MAX_VAL_FOR_STAGE_TIMER = "30";

            HashMap<Object, Object> stageUpTimer = haystack.readEntity("stageUp and system and dab and equipRef == \"" + equipMap.getId() + "\"");
            HashMap<Object, Object> stageDownTimer = haystack.readEntity("stageDown and system and dab and equipRef == \"" + equipMap.getId() + "\"");

            Point updatedPointForStageUp = new Point.Builder().setHashMap(stageUpTimer).setMaxVal(MAX_VAL_FOR_STAGE_TIMER).build();
            Point updatedPointForStageDown = new Point.Builder().setHashMap(stageDownTimer).setMaxVal(MAX_VAL_FOR_STAGE_TIMER).build();

            CCUHsApi.getInstance().updatePoint(updatedPointForStageUp, updatedPointForStageUp.getId());
            CCUHsApi.getInstance().updatePoint(updatedPointForStageDown, updatedPointForStageDown.getId());
        }
    }

    private static void updateScheduleType(CCUHsApi hayStack){
        List<HashMap<Object,Object>> rooms = hayStack.readAllEntities("room");
        for(HashMap<Object,Object> room : rooms){
            String scheduleId = room.containsKey("scheduleRef") ? room.get("scheduleRef").toString() : null;
            if(scheduleId == null){
                continue;
            }
            HashMap<Object, Object> schedule = hayStack.readMapById(scheduleId.replaceFirst("@",""));
            boolean isScheduleRefZoneSchedule = schedule.containsKey("zone") && schedule.containsKey("schedule");
            boolean isScheduleTypeNamedSchedule =
                    hayStack.readHisValByQuery("scheduleType and point and roomRef == \""+room.get("id").toString()+"\"")
                            .intValue() == ScheduleType.NAMED.ordinal();

            if(isScheduleRefZoneSchedule  && isScheduleTypeNamedSchedule) {
                hayStack.writeDefaultVal("scheduleType and point and  roomRef " +
                        "== \"" + room.get("id") + "\"", (double) ScheduleType.BUILDING.ordinal());
                hayStack.writeHisValByQuery("scheduleType and point and  roomRef " +
                        "== \"" + room.get("id") + "\"", (double) ScheduleType.BUILDING.ordinal());
            }
        }
    }

    private static void migrateDCWBPoints(CCUHsApi ccuHsApi){
        boolean isDCWBEnabled = ccuHsApi.readDefaultVal("dcwb and enabled") > 0;
        if(isDCWBEnabled){
            ArrayList<HashMap<Object, Object>> DCWBPoints = new ArrayList<>();
            HashMap<Object, Object> analog4OutputEnabled = ccuHsApi.readEntity("system and analog4 and output and enabled");
            DCWBPoints.add(analog4OutputEnabled);
            HashMap<Object, Object> adaptiveDeltaEnabled = ccuHsApi.readEntity("system and adaptive and delta and enabled");
            DCWBPoints.add(adaptiveDeltaEnabled);
            HashMap<Object, Object> maximizedExitWaterTempEnabled = ccuHsApi.readEntity("system and maximized and exit and water and enabled");
            DCWBPoints.add(maximizedExitWaterTempEnabled);
            HashMap<Object, Object> analog1AtValveClosedPosition = ccuHsApi.readEntity("system and analog1 and valve and closed");
            DCWBPoints.add(analog1AtValveClosedPosition);
            HashMap<Object, Object> analog1AtValveFullPosition = ccuHsApi.readEntity("system and analog1 and valve and full");
            DCWBPoints.add(analog1AtValveFullPosition);
            HashMap<Object, Object> analog4LoopOutputType = ccuHsApi.readEntity("system and analog4 and loop and output");
            DCWBPoints.add(analog4LoopOutputType);
            HashMap<Object, Object> analog4AtMinCoolingLoop = ccuHsApi.readEntity("system and analog4 and min and loop");
            DCWBPoints.add(analog4AtMinCoolingLoop);
            HashMap<Object, Object> analog4AtMaxCoolingLoop = ccuHsApi.readEntity("system and analog4 and max and loop");
            DCWBPoints.add(analog4AtMaxCoolingLoop);
            HashMap<Object, Object> createChilledWaterConfigPoints = ccuHsApi.readEntity("system and chilled and water and target and delta");
            DCWBPoints.add(createChilledWaterConfigPoints);
            HashMap<Object, Object> chilledWaterExitMargin = ccuHsApi.readEntity("system and chilled and water and exit and temp and margin");
            DCWBPoints.add(chilledWaterExitMargin);
            HashMap<Object, Object> chilledWaterMaxFlowRate = ccuHsApi.readEntity("system and chilled and water and max and flow and rate");
            DCWBPoints.add(chilledWaterMaxFlowRate);

            for (HashMap<Object, Object> DCWBPoint: DCWBPoints ) {
                Point updatedPoint = new Point.Builder().setHashMap(DCWBPoint).addMarker(Tags.DCWB).build();
                ccuHsApi.updatePoint(updatedPoint, updatedPoint.getId());
            }
        }else {
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "DCWB IS NOT ENABLED");
        }
    }

    private static void doSmartStatPointsMigration(CCUHsApi haystack){
        ArrayList <HashMap<Object, Object>> equipList = haystack.readAllEntities("equip");

        equipList.forEach(equip -> {
            Equip equipMap = new Equip.Builder().setHashMap(equip).build();
            String nodeAddress = equipMap.getGroup();
            // Below list consists of smartStat points which does not have group tag
            ArrayList<HashMap<Object, Object>> smartStatPoints = CCUHsApi.getInstance().readAllEntities("point and " +
                    "not group and standalone and not tuner and not system and not diag and  " +
                    "equipRef == \""+ equipMap.getId()+"\"");

            for(HashMap<Object, Object> point : smartStatPoints){
                Point up = new Point.Builder().setHashMap(point).setGroup(nodeAddress).build();
                CCUHsApi.getInstance().updatePoint(up,up.getId());
            }
        });
    }

    private static void migrateBPOSToOTN(CCUHsApi haystack){
        ArrayList <HashMap<Object, Object>> equipList = haystack.readAllEntities("bpos and equip");
        equipList.forEach(objectObjectHashMap -> {
            objectObjectHashMap.put("profile", "OTN");
            String displayName = objectObjectHashMap.get("dis").toString();
            displayName = displayName.replace("-BPOS-", "-OTN-");
            objectObjectHashMap.put("dis", displayName);
            Equip updatedEquip =
                    new Equip.Builder().setHashMap(objectObjectHashMap).removeMarker("bpos").addMarker(Tags.OTN).
                            build();
            CCUHsApi.getInstance().updateEquip(updatedEquip, updatedEquip.getId());

            HashMap device = CCUHsApi.getInstance().readEntity("device and equipRef == \"" +
                    updatedEquip.getId()+"\"");
            displayName = device.get("dis").toString();
            displayName = displayName.replace("SN-", "OTN-");
            device.put("dis", displayName);
            Device updatedDevice =
                    new Device.Builder().setHashMap(device).removeMarker("smartnode").addMarker(Tags.OTN).build();
            CCUHsApi.getInstance().updateDevice(updatedDevice, updatedDevice.getId());
        });

        ArrayList <HashMap<Object, Object>> pointList = haystack.readAllEntities("bpos and point");
        pointList.addAll(haystack.readAllEntities("tuner and point"));
        Set<HashMap<Object, Object>> pointSet = new HashSet<>(pointList);
        pointSet.forEach(pointHashMap -> {
            String displayName = pointHashMap.get("dis").toString();
            displayName = displayName.replace("-BPOS-", "-OTN-");
            pointHashMap.put("dis", displayName);
            if(pointHashMap.containsKey("tunerGroup") && pointHashMap.get("tunerGroup").toString().equals("BPOS")){
                pointHashMap.put("tunerGroup", TunerConstants.OTN_TUNER_GROUP);
            }
            Point updatedPoint;
            if(pointHashMap.containsKey("bpos")) {
                updatedPoint = new Point.Builder().setHashMap(pointHashMap).removeMarker("bpos").
                        addMarker(Tags.OTN).build();
            }
            else{
                updatedPoint = new Point.Builder().setHashMap(pointHashMap).build();
            }
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        });
        CCUHsApi.getInstance().scheduleSync();
    }
    private static void createHyperStatDeviceDisplayConfigurationPointsMigration(CCUHsApi haystack){
        ArrayList <HashMap<Object, Object>> equipList = haystack.readAllEntities("hyperstat and cpu and equip");
        for (HashMap<Object, Object> rawEquip : equipList) {
            if(!rawEquip.isEmpty()) {
                createDeviceConfigurationPoints(haystack);
            }
        }
    }
    private static void createDeviceConfigurationPoints(CCUHsApi hayStack){

        ArrayList<HashMap<Object, Object>> hyperStatCpus = hayStack.readAllEntities("equip and hyperstat and cpu");
        hyperStatCpus.forEach( cpuEquip -> {
            HashMap<Object, Object> deviceDisplayConfigurationPoint = hayStack.readEntity("humidity and enabled and equipRef" +
                    " == \"" + cpuEquip.get("id") + "\"");

            if (deviceDisplayConfigurationPoint.isEmpty()) {
                Equip equip1 = new Equip.Builder().setHashMap(cpuEquip).build();
                HyperStatPointsUtil hyperStatPointsUtil = HyperStatReconfigureUtil.Companion.getEquipPointsUtil(equip1, hayStack);
                hyperStatPointsUtil.createDeviceDisplayConfigurationPoints(true,false,false,true).forEach(
                        point -> pushPointToHS(hyperStatPointsUtil, point)
                );
            }
        });
    }
    private static void migrationForVAVTunerDefaultValue(CCUHsApi hayStack) {
        Log.i(MIGRATION_DEBUG, "migrationForVAVTunerDefaultValue we started the migration");
        CCUHsApi api = CCUHsApi.getInstance();
        ArrayList<HashMap<Object, Object>> vavEquips = hayStack.readAllEntities("equip and vav");

        vavEquips.forEach(equip -> {
            Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
            HashMap<Object, Object> dischargeOffset = api.readEntity("air and discharge and offset and vav and tuner and equipRef == \"" + equip.get("id")+"\"");
            HashMap<Object, Object> dischargeMax = api.readEntity("air and discharge and vav and tuner and max and equipRef == \"" + equip.get("id")+"\"");
            Log.i(MIGRATION_DEBUG, "vavEquip "+vavEquip.getDisplayName() );
            Log.i(MIGRATION_DEBUG, "dischargeOffset "+dischargeOffset.isEmpty() );
            if(dischargeOffset.isEmpty()) {
                Point equipTunerPoint = VavTuners.createDischargeTempOffsetTuner(false,
                        vavEquip.getDisplayName(), vavEquip.getId(),
                        vavEquip.getRoomRef(), vavEquip.getSiteRef(),
                        hayStack.getTimeZone());
                Log.i(MIGRATION_DEBUG, "dischargeOffset Points created");
                String equipTunerPointId = hayStack.addPoint(equipTunerPoint);
                VavTuners.writeDischargeOffsetValue(equipTunerPointId);
            }else{
                VavTuners.writeDischargeOffsetValue(dischargeOffset.get(Tags.ID).toString());
            }

            if(dischargeMax.isEmpty()){
                Point reheatZoneMaxDischargeTempOffsetTuner = VavTuners.createMaxDischargeTempTuner(false,
                        vavEquip.getDisplayName(), vavEquip.getId(),
                        vavEquip.getRoomRef(), vavEquip.getSiteRef(),
                        hayStack.getTimeZone());
                String equipTunerPointId = hayStack.addPoint(reheatZoneMaxDischargeTempOffsetTuner);
                VavTuners.writeDischargeMaxValue(equipTunerPointId);
            }else {
                VavTuners.writeDischargeMaxValue(dischargeMax.get(Tags.ID).toString());
            }
        });
    }

    private static void doDabReheatMigration(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> dabEquips = hayStack.readAllEntities("equip and dab and zone");

        dabEquips.forEach( dabEquip -> {
            Equip equip = new Equip.Builder().setHashMap(dabEquip).build();
            createEquipReheatTuners(hayStack, equip);
            createReheatType(equip, 0, hayStack);

            ArrayList<HashMap<Object, Object>> supplyAirTemps = hayStack.readAllEntities("discharge and air and temp and equipRef ==\""+equip.getId()+"\"");
            supplyAirTemps.forEach( supplyAirTemp -> {
                Point supplyAirTempPoint = new Point.Builder().setHashMap(supplyAirTemp).build();
                if (supplyAirTempPoint.getMarkers().contains(Tags.PRIMARY)) {
                    supplyAirTempPoint.setDisplayName("supplyAirTemp1");
                } else {
                    supplyAirTempPoint.setDisplayName("supplyAirTemp2");
                }
                supplyAirTempPoint.getMarkers().remove("discharge");
                supplyAirTempPoint.getMarkers().add("supply");
                hayStack.updatePoint(supplyAirTempPoint, supplyAirTemp.get("id").toString());
            });

        });

    }

    private static void doMigrateForSmartNodeDamperType(CCUHsApi haystack){

        ArrayList<HashMap<Object, Object>> equips = haystack.readAllEntities("equip and (vav or dab) and group");
        equips.forEach(equip -> {
            Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
            int nodeAddress = Integer.parseInt(actualEquip.getGroup());
            RawPoint rawPoint = SmartNode.getPhysicalPoint(nodeAddress, ANALOG_OUT_ONE.toString());
            if(rawPoint!=null && rawPoint.getType().equalsIgnoreCase("MAT")){
                    SmartNode.updatePhysicalPointType(nodeAddress,Port.ANALOG_OUT_ONE.name(),DamperType.MAT.displayName);
            }
            RawPoint rawPoint2 = SmartNode.getPhysicalPoint(nodeAddress, ANALOG_OUT_TWO.toString());
            if(rawPoint2!=null && rawPoint2.getType().equalsIgnoreCase("MAT")){
                SmartNode.updatePhysicalPointType(nodeAddress, ANALOG_OUT_TWO.name(),DamperType.MAT.displayName);
            }
         });
    }
    private static void autoAwayAutoForcedMigration(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> vrvEquips = hayStack.readAllEntities("vrv and equip");
        if (!vrvEquips.isEmpty()) {
            for (HashMap<Object, Object> equip : vrvEquips) {
                Equip vrvEquip = new Equip.Builder().setHashMap(equip).build();
                createPointsAutoAwayAutoForcedOccupiedPoints(hayStack,vrvEquip,"Daikin VRV");
            }
        }
    }

    private static void createPointsAutoAwayAutoForcedOccupiedPoints(CCUHsApi hayStack, Equip equip, String profileName) {
        String nodeAddr = equip.getGroup();
        String floorRef = equip.getFloorRef();
        String equipRef = equip.getId();
        String roomRef = equip.getRoomRef();
        String siteRef = equip.getSiteRef();
        String tz = equip.getTz();
        String displayName = equip.getDisplayName();

        VrvTuners.addVRVTunersAndSensorPoints(hayStack, siteRef, displayName,
                equipRef, roomRef, floorRef, tz,nodeAddr);
        VrvTuners.createOccupancyPoints(hayStack, siteRef, displayName,
                equipRef, roomRef, floorRef, tz,nodeAddr,0.0,0.0);
    }
    private static void createFreeInternalDiskStorageDiagPointMigration(CCUHsApi instance) {
        HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        if(siteMap.size()>0){
            String siteRef = Objects.requireNonNull(siteMap.get(Tags.ID)).toString();
            String tz = Objects.requireNonNull(siteMap.get("tz")).toString();
            HashMap diagEquip = instance.read("equip and diag");
            Point internalDiskStorage = new Point.Builder()
                    .setDisplayName(diagEquip.get("dis")+"-availableInternalDiskStorage")
                    .setEquipRef(diagEquip.get("id")+"")
                    .setSiteRef(siteRef).setHisInterpolate("linear")
                    .addMarker("diag").addMarker("available").addMarker("internal").addMarker("disk").addMarker("storage").addMarker("his").addMarker("cur")
                    .setUnit("MB")
                    .setTz(tz)
                    .build();
            instance.addPoint(internalDiskStorage);

            // adding 'cur' tag to all available memory.
            ArrayList<HashMap<Object, Object>> allMemoryPoints = CCUHsApi.getInstance().readAllEntities("memory and diag");

            for(HashMap<Object, Object> point : allMemoryPoints){
                Point up = new Point.Builder().setHashMap(point).addMarker("cur").build();
                CCUHsApi.getInstance().updatePoint(up,up.getId());
            }
        }
    }

    private static void staticPressureSpTrimMigration(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> staticPressureSPTrimPoint = ccuHsApi.readAllEntities("point and tuner and staticPressure and sptrim and system");
        String updatedMaxVal = "-0.5";
        String updatedMinVal = "-0.01";
        String updatedIncrementalVal = "-0.01";
        for (HashMap<Object,Object> staticPressureSPTrim : staticPressureSPTrimPoint) {
            Point updatedStaticPressureSPTrimPoint = new Point.Builder().setHashMap(staticPressureSPTrim).setMaxVal(updatedMaxVal).setMinVal(updatedMinVal).setIncrementVal(updatedIncrementalVal).build();
            CCUHsApi.getInstance().updatePoint(updatedStaticPressureSPTrimPoint, updatedStaticPressureSPTrimPoint.getId());
        }

    }

    private static void airflowSampleWaitTimeMigration(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> airflowSampleWaitTimePoint = ccuHsApi.readAllEntities("point and tuner and airflow and sample and wait and time and not standalone");
        String updatedMaxVal = "150";
        String updatedMinVal = "0";
        String updatedUnit = "m";
        for (HashMap<Object,Object> airflowSampleWait : airflowSampleWaitTimePoint) {
            Point updatedAirflowSampleWaitTimePoint = new Point.Builder().setHashMap(airflowSampleWait).setMaxVal(updatedMaxVal).setMinVal(updatedMinVal).setUnit(updatedUnit).build();
            CCUHsApi.getInstance().updatePoint(updatedAirflowSampleWaitTimePoint, updatedAirflowSampleWaitTimePoint.getId());
        }

    }


    private static void addBuildingLimitsBreachedOccupancy(CCUHsApi hayStack){
        ArrayList<HashMap<Object, Object>> occModePoints = hayStack.readAllEntities("occupancy and mode");
        occModePoints.forEach( occMode -> {
            Point occModePoint = new Point.Builder().setHashMap(occMode).build();
            occModePoint.setEnums(Occupancy.getEnumStringDefinition());
            hayStack.updatePoint(occModePoint, occModePoint.getId());
        });
        hayStack.scheduleSync();
    }

     /**
      * This is need to recover from few CCUs having a corrupted named scheduled with "building" tag.
      * The corrupted schedules were removed from backend. But some CCUs in the field are still
      * holding on to that and causing schedule related functional issues.
      */
     private static void removeCorruptedNamedSchedules(CCUHsApi hayStack) {
         List<HashMap<Object, Object>> corruptedNamedSchedules = hayStack.readAllEntities("schedule and named and building");
         if (corruptedNamedSchedules.isEmpty()) {
             return;
         }
         //Fix any room linked to the named schedule by mapping back to zoneSchedule
         List<HashMap<Object, Object>> rooms = hayStack.readAllEntities("room");
         rooms.forEach(room -> {
             if (room.get("scheduleRef") != null) {
                 corruptedNamedSchedules.forEach( schedule -> {
                     if (room.get("scheduleRef").toString().equals(schedule.get("id").toString())) {
                         HashMap<Object, Object> zoneSchedule = hayStack.readEntity("schedule and zone and roomRef == \""+room.get("id").toString());
                         Zone updated = new Zone.Builder().setHashMap(room).build();
                         updated.setScheduleRef(zoneSchedule.get("id").toString());
                         CcuLog.i(TAG_CCU_MIGRATION_UTIL, "removeCorruptedNamedSchedules updated ScheduleRef for "+room);
                     }
                 });
             }
         });

        corruptedNamedSchedules.forEach( schedule -> {
             hayStack.deleteEntityLocally(schedule.get("id").toString());
             //CCU is not expected to delete named schedule from backend
             hayStack.getSyncStatusService().setDeletedEntitySynced(schedule.get("id").toString());
             CcuLog.i(TAG_CCU_MIGRATION_UTIL, "removeCorruptedNamedSchedules "+schedule);
         });
     }
    private static void MinorTagMigration(CCUHsApi ccuHsApi) {
        runtimeTagMigration(ccuHsApi);
        otnAutoAwayForcedOccupyPointTagMigration(ccuHsApi);
    }

    private static void doDabReheatStage2Migration(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> dabEquips = hayStack.readAllEntities("equip and dab and zone");

        dabEquips.forEach( dabEquip -> {
            Equip equip = new Equip.Builder().setHashMap(dabEquip).build();
            int reheatType = hayStack.readDefaultVal("reheat and type and equipRef == \""+equip.getId()+"\"").intValue();
            HashMap<Object, Object> reheatCmd = hayStack.readEntity("reheat and cmd and equipRef ==\""+equip.getId()+"\"");

            if ((reheatType - 1) == ReheatType.TwoStage.ordinal() && !reheatCmd.isEmpty()) {
                DeviceUtil.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_TWO.toString(),
                                                                reheatCmd.get("id").toString());
            }

            double damperType = hayStack.readDefaultVal("secondary and damper and type and " +
                    "equipRef == \""+equip.getId()+"\"");
            HashMap<Object, Object> damperPos = hayStack.readEntity("normalized and damper and " +
                    "secondary and cmd and equipRef == \""+equip.getId()+"\"");
            if (damperType != DamperType.MAT.ordinal() && !damperPos.isEmpty()) {
                DeviceUtil.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), ANALOG_OUT_TWO.toString(),
                        damperPos.get("id").toString());
            }

        });

    }
    private static void otnAutoAwayForcedOccupyPointTagMigration(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> equipList = ccuHsApi.readAllEntities("otn and equip");
        String[] markerToAdd = {};
        String[] markerToRemove = {};
        String[] forced = {"forced"};
        equipList.forEach(equip -> {
            removeTag("auto and forced and occupied", equip, markerToAdd, markerToRemove, equip.get(Tags.DIS).toString()+"-autoForceOccupiedEnabled");
            removeTag("auto and away and config", equip, markerToAdd, forced, equip.get(Tags.DIS).toString()+"-autoawayEnabled");
        });
    }
    private static void runtimeTagMigration(CCUHsApi ccuHsApi){
        ArrayList<HashMap<Object, Object>> equipList = ccuHsApi.readAllEntities("hyperstat and equip");
        if(equipList.size() > 0 ) {
            String[] markerToAdd = {};
            String[] markerToRemove = {"runtime"};
            String[] emptyArray = {};

            equipList.forEach(equip -> {
                removeTag("cmd and analog and cooling",equip,markerToAdd,markerToRemove,null);
                removeTag("cmd and analog and heating",equip,markerToAdd,markerToRemove,null);
                removeTag("cmd and analog and fan and speed",equip,markerToAdd,markerToRemove,null);
                removeTag("cmd and analog and dcv and damper",equip,markerToAdd,markerToRemove,null);
                removeTag("cmd and analog and compressor and speed",equip,markerToAdd,markerToRemove,null);
                removeTag("cmd and analog and water and valve",equip,markerToAdd,markerToRemove,null);
                removeTag("config and auto and forced and occupancy",equip,markerToAdd,emptyArray,equip.get(Tags.DIS).toString()+"-autoForceOccupiedEnabled");
                removeTag("config and auto and away",equip,markerToAdd,emptyArray,equip.get(Tags.DIS).toString()+"-autoawayEnabled");
            });
        }
    }

    private static void removeTag(
            String query,
            HashMap<Object, Object> equip,
            String[] markerToAdd,
            String[] markerToRemove,
            String displayName
    ){
        HashMap<Object, Object> point = CpuPointsMigration.Companion.readPoint(query, Objects.requireNonNull(equip.get(Tags.ID)).toString());
        if(point.size() > 0) {
            MigratePointsUtil.Companion.updateMarkers(
                    point, markerToAdd, markerToRemove, displayName
            );
        }
    }


    private static void createStandaloneCoolingAirflowTempLowerOffsetMigration(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> standaloneAirflowTempLowerOffsetPoints = ccuHsApi.
                readAllEntities("point and airflow and standalone and cooling and lower and not stage2");
        String updatedUnit = "\u00B0F";
        for (HashMap<Object, Object> standaloneAirflowTempLowerOffsetPoint : standaloneAirflowTempLowerOffsetPoints) {
            Point updatedPoint = new Point.Builder().setHashMap(standaloneAirflowTempLowerOffsetPoint).setUnit(updatedUnit).build();
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());
        }

    }

    private static void createStandaloneAirflowSampleWaitMigration(CCUHsApi ccuHsApi) {

        ArrayList<HashMap<Object, Object>> standaloneAirflowSampleWaitPoints = ccuHsApi.
                readAllEntities("standalone and airflow and default and sample");
        for (HashMap<Object, Object> standaloneAirflowSampleWaitTime : standaloneAirflowSampleWaitPoints) {
            String updateUnit = "m";
            Point updatedStandaloneAirflowSampleWaitTimePoint = new Point.Builder().
                    setHashMap(standaloneAirflowSampleWaitTime).setUnit(updateUnit).build();

            CCUHsApi.getInstance().updatePoint(updatedStandaloneAirflowSampleWaitTimePoint,
                    updatedStandaloneAirflowSampleWaitTimePoint.getId());
        }
    }

    private static void changeOccupancyToOccupiedForAutoForcedEnabledPoint(CCUHsApi instance) {

        ArrayList<HashMap<Object, Object>> vrvEquips = CCUHsApi.getInstance().readAllEntities("vrv and equip and zone");
        for(HashMap<Object, Object> equip : vrvEquips){

            HashMap<Object, Object> pointHM= CCUHsApi.getInstance().readEntity("vrv and auto and forced and occupancy and equipRef== \"" + equip.get("id") + "\"");
            if(pointHM.size() > 0){
                Point tempPoint = new Point.Builder().setHashMap(pointHM).removeMarker("occupancy").addMarker("occupied").build();
                tempPoint.setDisplayName(equip.get("dis")+"-autoForceOccupiedEnabled");
                instance.updatePoint(tempPoint,tempPoint.getId());
            }

           HashMap<Object, Object> pointHM1 = CCUHsApi.getInstance().readEntity("vrv and auto and away and enabled and equipRef== \"" + equip.get("id") + "\"");
           if(pointHM1.size() > 0){
               Point tempPoint = new Point.Builder().setHashMap(pointHM1).build();
               tempPoint.setDisplayName(equip.get("dis")+"-autoawayEnabled");
               instance.updatePoint(tempPoint,tempPoint.getId());
           }
        }
    }
}