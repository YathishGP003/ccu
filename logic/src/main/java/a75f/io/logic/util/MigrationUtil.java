package a75f.io.logic.util;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.truecfm.TrueCFMConfigPoints;
import a75f.io.logic.tuners.TrueCFMTuners;

public class MigrationUtil {
    
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

        if (!PreferenceUtil.isTrueCFMVAVMigrationDone()) {
            trueCFMVAVMigration(CCUHsApi.getInstance());
            PreferenceUtil.setTrueCFMVAVMigrationDone();
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
        doMigrationVav(haystack, vavEquips, tunerEquip);

    }
    private static void doMigrationVav(CCUHsApi haystack, ArrayList<HashMap<Object,Object>>vavEquips, Equip tunerEquip) {
        //        creating default tuners for vav
        TrueCFMTuners.createTrueCFMVavTunerPoints(haystack,tunerEquip);
        vavEquips.forEach(vavEquip -> {
            HashMap<Object, Object> enableCFMPoint = haystack.readEntity("enabled and point and cfm and equipRef== \"" + vavEquip.get("id") + "\"");
            if (enableCFMPoint.get("id")==null) {
                Equip equip = new Equip.Builder().setHashMap(vavEquip).build();
                String fanMarker = "";
                if (equip.getProfile().equals(ProfileType.VAV_SERIES_FAN.name())) {
                    fanMarker = "series";
                } else if (equip.getProfile().equals(ProfileType.VAV_PARALLEL_FAN.name())) {
                    fanMarker = "parallel";
                }
                TrueCFMConfigPoints.createTrueCFMControlPoint(haystack, equip, Tags.VAV,
                        0, fanMarker);
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
                    if (zone.getScheduleRef() == null) {
                        CcuLog.i("MIGRATION_UTIL", " Not ideal , there is a zone without zone schedule !!!!!");
                        Schedule zoneSchedule = hayStack.getScheduleById(schedule.get("id").toString());
                        zone.setScheduleRef(schedule.get("id").toString());
                        hayStack.updateZone(zone, zone.getId());
                        hayStack.updateZoneSchedule(zoneSchedule, zone.getId());
                    } else if (!schedule.get("id").toString().equals(zone.getScheduleRef())) {
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
            Map<Object,Object> cloudConnectivityPoint = CCUHsApi.getInstance().readEntity("cloud and connectivity" +
                    " and diag and point");
            if(cloudConnectivityPoint.isEmpty()){
                CCUHsApi.getInstance().addPoint(new Point.Builder()
                        .setDisplayName("DiagEquip-ccuHeartbeat")
                        .setEquipRef(diagEquip.get("id").toString())
                        .setSiteRef(diagEquip.get("siteRef").toString())
                        .addMarker("diag").addMarker("cloud").addMarker("connectivity").addMarker("his")
                        .setTz(diagEquip.get("tz").toString())
                        .build());
            }
        }
    }
    
}