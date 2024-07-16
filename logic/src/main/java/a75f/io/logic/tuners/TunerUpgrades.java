package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Queries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.haystack.TagQueries;
import a75f.io.logic.util.PreferenceUtil;

import static a75f.io.logic.tuners.TunerConstants.GENERIC_TUNER_GROUP;
import static a75f.io.logic.tuners.TunerConstants.TUNER_BUILDING_VAL_LEVEL;

/**
 * Tuners are normally created when an equip is created.
 * Adding a tuner on upgrade builds would need to create them on existing equips by hand.
 *
 */
public class TunerUpgrades {

    /**
     * Takes care creating new tuners on existing equips during an upgrade.
     */
    public static void handleTunerUpgrades(CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_TUNER, " handleTunerUpgrades ");
        upgradeReheatZoneToDATMinDifferential(hayStack);
        upgradeDcwbBuildingTuners(hayStack);
        createDefaultTempLockoutPoints(hayStack);
        addVavDischargeTempTuners(hayStack);
        migrateCelsiusSupportConfiguration(hayStack);
        migrateAutoAwaySetbackTuner(hayStack);
        migrateAutoAwayCpuSetbackTuner(hayStack);
        addVavModeChangeoverHysteresisTuner(hayStack);
        GenericTuners.createCcuNetworkWatchdogTimeoutTuner(hayStack);
        Equip buildingTunerEquip = new Equip.Builder().setHashMap(hayStack.readEntity("equip and tuner")).build();
        DabReheatTunersKt.createDefaultReheatTuners(hayStack, buildingTunerEquip);
    }
    
    /**
     * Takes care of upgrades for vav specific tuner reheatZoneToDATMinDifferential
     * All the builds upto 1.556 needs this migration
     */
    private static void upgradeReheatZoneToDATMinDifferential(CCUHsApi hayStack) {
    
        //Make sure the tuner does not exist before creating it. A duplicate tuner if ever created can be hard to
        //track and fix.
        ArrayList<HashMap<Object, Object>> reheatTuners = hayStack.readAllEntities("point and tuner and reheat and dat and min and differential");
        if (!reheatTuners.isEmpty()) {
            CcuLog.e(L.TAG_CCU_TUNER, "reheatZoneToDATMinDifferential exists");
            return;
        }
    
        CcuLog.e(L.TAG_CCU_TUNER, "create ReheatZoneToDATMinDifferential ");
        //Create the tuner point on building tuner equip.
        HashMap<Object, Object> buildTuner = hayStack.readEntity(Queries.EQUIP_AND_TUNER);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();
        Point buildingTunerPoint = VavTuners.createReheatZoneToDATMinDifferentialTuner(true,
                tunerEquip.getDisplayName(), tunerEquip.getId(),null, null,
                tunerEquip.getSiteRef(), hayStack.getTimeZone());
    
        String buildingReheatZoneToDATMinDifferentialId = hayStack.addPoint(buildingTunerPoint);
        hayStack.writePointForCcuUser(buildingReheatZoneToDATMinDifferentialId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                                      TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL, 0);
        hayStack.writeHisValById(buildingReheatZoneToDATMinDifferentialId, TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL);
        
        //Create the tuner point on all vav equips
        ArrayList<HashMap<Object, Object>> vavEquips = hayStack.readAllEntities("equip and vav");
        vavEquips.forEach(equip -> {
            Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
            Point equipTunerPoint = VavTuners.createReheatZoneToDATMinDifferentialTuner(false,
                    vavEquip.getDisplayName(), vavEquip.getId(),
                    vavEquip.getRoomRef(), vavEquip.getFloorRef(),
                    vavEquip.getSiteRef(), hayStack.getTimeZone());
            String reheatZoneToDATMinDifferentialId = hayStack.addPoint(equipTunerPoint);
            BuildingTunerUtil.updateTunerLevels(reheatZoneToDATMinDifferentialId, vavEquip.getRoomRef(), hayStack);
            hayStack.writeHisValById(reheatZoneToDATMinDifferentialId, HSUtil.getPriorityVal(reheatZoneToDATMinDifferentialId));
        });
    }
    
    /**
     * Takes care of upgrades for DCWB specific tuners
     */
    private static void upgradeDcwbBuildingTuners(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> rdcwbTuners = hayStack.readAllEntities("point and tuner and dcwb and default");
        if (!rdcwbTuners.isEmpty()) {
            CcuLog.e(L.TAG_CCU_TUNER, "dcwbTuners exist");
            return;
        }
    
        CcuLog.e(L.TAG_CCU_TUNER, "create dcwbTuners ");
        //Create the tuner points on building tuner equip.
        HashMap<Object, Object> buildTuner = hayStack.readEntity(Queries.EQUIP_AND_TUNER);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();
        DcwbTuners.addDefaultDcwbTuners(hayStack, tunerEquip.getSiteRef(), tunerEquip.getId(),
                                        tunerEquip.getDisplayName(), tunerEquip.getTz());
    
    
        //If the current profile is DABFullyModulating, create new system equip tuner points.
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readEntity("equip and system and not modbus");
        if (!equipMap.isEmpty()) {
            Equip systemEquip = new Equip.Builder().setHashMap(equipMap).build();
            if (ProfileType.valueOf(systemEquip.getProfile()) == ProfileType.SYSTEM_DAB_ANALOG_RTU ) {
                DcwbTuners.addEquipDcwbTuners(hayStack, systemEquip.getSiteRef(), systemEquip.getDisplayName(),
                                              systemEquip.getId(), systemEquip.getTz());
            }
        }
        
    }

    /*
    To change min and max of heating desired temperature
     */
    public static void updateHeatingMinMax(CCUHsApi hayStack) {

        CcuLog.i("TunerUpdate", "updateHeatingMinMax ++");
        HashMap<Object, Object> heatDTMin = hayStack.readEntity("point and limit and min and heating " +
                "and user");
        HashMap<Object, Object> heatDTMax = hayStack.readEntity("point and limit and max and heating " +
                "and user");
        if (!heatDTMin.isEmpty() && heatDTMin.containsKey("id")) {
            String minId = heatDTMin.get("id").toString();
            forceExpireBuildingLevel(minId,hayStack);
            hayStack.writePointForCcuUser(minId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 67.0,
                    0);
            hayStack.writeHisValById(minId, 67.0);
        }
        if (!heatDTMax.isEmpty() && heatDTMax.containsKey("id")) {
            String maxId = heatDTMax.get("id").toString();
            forceExpireBuildingLevel(maxId,hayStack);
            hayStack.writePointForCcuUser(maxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 72.0,
                    0);
            hayStack.writeHisValById(maxId, 72.0);
        }
    }

    private static void forceExpireBuildingLevel(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> buildingLevelVal = HSUtil.getPriorityLevel(id, TUNER_BUILDING_VAL_LEVEL);
        if (buildingLevelVal.isEmpty() ||
                buildingLevelVal.get(HayStackConstants.WRITABLE_ARRAY_WHO) == null ||
                buildingLevelVal.get(HayStackConstants.WRITABLE_ARRAY_VAL) == null) {
            CcuLog.i(L.TAG_CCU_TUNER,"Level 16 does not exist for "+id);
            return;
        }
        hayStack.clearPointArrayLevel(id, TUNER_BUILDING_VAL_LEVEL, false);
        CcuLog.i(L.TAG_CCU_TUNER,"Cleared level 16 : "+id);
    }

    private static void createDefaultTempLockoutPoints(CCUHsApi hayStack) {

        HashMap<Object, Object> buildTuner = hayStack.readEntity(TagQueries.TUNER_EQUIP);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();
        if (hayStack.readEntity("point and tuner and default and outsideTemp and cooling and lockout and dab").isEmpty()) {
            SystemTuners.createCoolingTempLockoutPoint(hayStack, tunerEquip.getSiteRef(),
                                                       tunerEquip.getId(),
                                                       tunerEquip.getDisplayName(),
                                                       tunerEquip.getTz(),
                                                       Tags.DAB, true);
        }

        if (hayStack.readEntity("point and tuner and default and outsideTemp and heating and lockout and dab").isEmpty()) {
            SystemTuners.createHeatingTempLockoutPoint(hayStack, tunerEquip.getSiteRef(),
                                                       tunerEquip.getId(),
                                                       tunerEquip.getDisplayName(),
                                                       tunerEquip.getTz(),
                                                       Tags.DAB, true);
        }

        if (hayStack.readEntity("point and tuner and default and outsideTemp and cooling and lockout and vav").isEmpty()) {
            SystemTuners.createCoolingTempLockoutPoint(hayStack, tunerEquip.getSiteRef(),
                                                       tunerEquip.getId(),
                                                       tunerEquip.getDisplayName(),
                                                       tunerEquip.getTz(),
                                                       Tags.VAV, true);
        }

        if (hayStack.readEntity("point and tuner and default and outsideTemp and heating and lockout and vav").isEmpty()) {
            SystemTuners.createHeatingTempLockoutPoint(hayStack, tunerEquip.getSiteRef(), tunerEquip.getId(), tunerEquip.getDisplayName()
                , tunerEquip.getTz(), Tags.VAV, true);
        }
    }

    private static void addVavDischargeTempTuners(CCUHsApi hayStack){
        CcuLog.i(L.TAG_CCU_TUNER,"migration addVavDischargeTempTuners");
        ArrayList<HashMap<Object, Object>> dischargeTempOffsetTuner = hayStack.readAllEntities(
                "air and discharge and offset and vav and tuner and default");
        ArrayList<HashMap<Object, Object>> maxDischargeTempTuner = hayStack.readAllEntities(
                "air and discharge and vav and tuner and max and default");

        //Create the tuner point on building tuner equip.
        HashMap<Object, Object> buildTuner = hayStack.readEntity(Queries.EQUIP_AND_TUNER);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();
        //Create the tuner point on all vav equips
        ArrayList<HashMap<Object, Object>> vavEquips = hayStack.readAllEntities("equip and vav");

            if (maxDischargeTempTuner.isEmpty()) {
                CcuLog.i(L.TAG_CCU_TUNER,"maxDischargeTempTuner tuners not found, Creating new tuners");
                Point reheatZoneMaxDischargeTempTuner = VavTuners.createMaxDischargeTempTuner(true,
                        tunerEquip.getDisplayName(), tunerEquip.getId(), null, null,
                        tunerEquip.getSiteRef(), hayStack.getTimeZone());

                String maxDischargeTempTunerID = hayStack.addPoint(reheatZoneMaxDischargeTempTuner);
                hayStack.writePointForCcuUser(maxDischargeTempTunerID, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                        TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP, 0);
                CCUHsApi.getInstance().writeHisValById(maxDischargeTempTunerID, TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP);

                CcuLog.i(L.TAG_CCU_TUNER,"maxDischargeTempTuner tuners not found, Creating new tuners");
                vavEquips.forEach(equip -> {
                    Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
                    Point equipTunerPoint = VavTuners.createMaxDischargeTempTuner(false,
                            vavEquip.getDisplayName(), vavEquip.getId(),
                            vavEquip.getRoomRef(),vavEquip.getFloorRef(), vavEquip.getSiteRef(),
                            hayStack.getTimeZone());

                    String equipTunerPointId = hayStack.addPoint(equipTunerPoint);

                    hayStack.writePointForCcuUser(equipTunerPointId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP, 0);
                    hayStack.writeHisValById(equipTunerPointId, TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP);
                });
            }else{
                CcuLog.i(L.TAG_CCU_TUNER,"maxDischargeTempTuner found "+maxDischargeTempTuner.size());
            }

            if (dischargeTempOffsetTuner.isEmpty()) {
                Point reheatZoneMaxDischargeTempOffsetTuner = VavTuners.createDischargeTempOffsetTuner(true,
                        tunerEquip.getDisplayName(), tunerEquip.getId(), null, null,
                        tunerEquip.getSiteRef(), hayStack.getTimeZone());

                String reheatZoneDischargeTempOffSetTunerId = hayStack.addPoint(reheatZoneMaxDischargeTempOffsetTuner);
                hayStack.writePointForCcuUser(reheatZoneDischargeTempOffSetTunerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                        TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP_OFFSET, 0);
                hayStack.writeHisValById(reheatZoneDischargeTempOffSetTunerId,
                        TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP_OFFSET);


                vavEquips.forEach(equip -> {
                    Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
                    Point equipTunerPoint = VavTuners.createDischargeTempOffsetTuner(false,
                            vavEquip.getDisplayName(), vavEquip.getId(), vavEquip.getRoomRef(), vavEquip.getFloorRef(),
                            vavEquip.getSiteRef(), hayStack.getTimeZone());

                    String equipTunerPointId = hayStack.addPoint(equipTunerPoint);

                    hayStack.writePointForCcuUser(equipTunerPointId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP_OFFSET, 0);
                    hayStack.writeHisValById(equipTunerPointId, TunerConstants.DEFAULT_REHEAT_ZONE_MAX_DISCHAGE_TEMP_OFFSET);
                  });
            }else
                CcuLog.i(L.TAG_CCU_TUNER,"dischargeTempOffsetTuner found "+dischargeTempOffsetTuner.size());

        }

    private static void addVavModeChangeoverHysteresisTuner(CCUHsApi hayStack){
        CcuLog.i(L.TAG_CCU_TUNER,"migration addVavModeChangeoverHysteresisTuner");
        ArrayList<HashMap<Object, Object>> vavModeChangeoverHysteresisTuner = hayStack.readAllEntities(
                "vav and mode and changeover and hysteresis and tuner and default");

        //Create the tuner point on building tuner equip.
        HashMap<Object, Object> buildTuner = hayStack.readEntity(Queries.EQUIP_AND_TUNER);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();

        if (vavModeChangeoverHysteresisTuner.isEmpty()) {
            CcuLog.i(L.TAG_CCU_TUNER,"vavModeChangeoverHysteresis tuner not found, Creating new tuner");
            Point newVavModeChangeoverHysteresisTuner = VavTuners.createDefaultModeChangeoverHysteresisTuner(tunerEquip.getDisplayName(), tunerEquip.getId(),
                    tunerEquip.getSiteRef(),
                    hayStack.getTimeZone());

            String newVavModeChangeoverHysteresisTunerID = hayStack.addPoint(newVavModeChangeoverHysteresisTuner);
            hayStack.writePointForCcuUser(newVavModeChangeoverHysteresisTunerID, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                    TunerConstants.DEFAULT_VAV_MODE_CHANGEOVER_HYSTERESIS, 0);
            CCUHsApi.getInstance().writeHisValById(newVavModeChangeoverHysteresisTunerID, TunerConstants.DEFAULT_VAV_MODE_CHANGEOVER_HYSTERESIS);

        }else{
            CcuLog.i(L.TAG_CCU_TUNER,"vavModeChangeHysteresis tuner found "+vavModeChangeoverHysteresisTuner.size());
        }


    }


   private static void migrateCelsiusSupportConfiguration(CCUHsApi hayStack){
       HashMap<Object, Object> tuner = hayStack.readEntity(TagQueries.TUNER_EQUIP);

       if (tuner != null && tuner.size() > 0) {
           String equipRef = tuner.get("id").toString();
           String equipDis = tuner.get("dis").toString();
           HashMap<Object, Object> siteMap = hayStack.readEntity(Tags.SITE);
           String siteRef = siteMap.get(Tags.ID).toString();
           String tz = siteMap.get("tz").toString();
           HashMap<Object, Object> useCelsiusPoint = CCUHsApi.getInstance().readEntity("displayUnit");
            if(useCelsiusPoint.isEmpty()) {
                Point useCelsius = new Point.Builder()
                        .setDisplayName(equipDis + "-" + "displayUnit")
                        .setSiteRef(siteRef)
                        .setEquipRef(equipRef).setHisInterpolate("cov")
                        .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his").addMarker("displayUnit")
                        .addMarker("system").addMarker("building").addMarker("enabled").addMarker("sp").setIncrementVal("1")
                        .setEnums("false,true").setTunerGroup(GENERIC_TUNER_GROUP)
                        .setMinVal("0")
                        .setMaxVal("1")
                        .setTz(tz)
                        .build();
                String useCelsiusId = hayStack.addPoint(useCelsius);
                hayStack.writePointForCcuUser(useCelsiusId, TunerConstants.TUNER_BUILDING_VAL_LEVEL, TunerConstants.USE_CELSIUS_FLAG_DISABLED, 0);
                hayStack.writeHisValById(useCelsiusId, TunerConstants.USE_CELSIUS_FLAG_DISABLED);
                CcuLog.i(L.TAG_CCU_TUNER, "migrateCelsiusSupportConfiguration: useCelsiusPoint Point created ");
            }else{
                CcuLog.i(L.TAG_CCU_TUNER, "migrateCelsiusSupportConfiguration: useCelsiusPoint already present");
            }
       }

   }

    public static void migrateAutoAwaySetbackTuner(CCUHsApi hayStack) {
            //Create the tuner point on all equips
            ArrayList<HashMap<Object, Object>> vavEquips = hayStack.readAllEntities("equip and vav and zone");
            ArrayList<HashMap<Object, Object>> dabEquips = hayStack.readAllEntities("equip and dab and zone");
            ArrayList<HashMap<Object, Object>> dabDualDuctEquips = hayStack.readAllEntities("equip and dualDuct");
            ArrayList<HashMap<Object, Object>> ssEquips = hayStack.readAllEntities("equip and standalone and smartstat");
            createPoint(hayStack,vavEquips);
            createPoint(hayStack,dabEquips);
            createPoint(hayStack,dabDualDuctEquips);
            createPoint(hayStack,ssEquips);
            PreferenceUtil.setAutoAwaySetBackMigration();
    }
    private static void migrateAutoAwayCpuSetbackTuner(CCUHsApi hayStack) {
        if(!PreferenceUtil.getAutoAwaySetBackCpuMigration()){
            //Create the tuner point on all equips
            ArrayList<HashMap<Object, Object>> cpuEquips = hayStack.readAllEntities("equip and cpu");
            createPoint(hayStack,cpuEquips);
            PreferenceUtil.setAutoAwaySetBackCpuMigration();
        }
    }

    private static void createPoint(CCUHsApi hayStack,ArrayList<HashMap<Object, Object>> equips) {
        for (HashMap<Object, Object> equip : equips) {
            String equipRef = equip.get("id").toString();
            HashMap<Object,Object> autoAwaySetBack =
                    hayStack.readEntity("auto and away and setback and equipRef == \"" +equipRef+"\"");
            if(autoAwaySetBack.isEmpty()){
                String equipdis = equip.get("dis").toString();
                String siteRef = equip.get("siteRef").toString();
                String roomRef = equip.get("roomRef").toString();
                String floorRef = equip.get("floorRef").toString();
                String tz = equip.get("tz").toString();
                createAutoAwayPoint(hayStack,equipdis,siteRef,equipRef,roomRef,floorRef,tz);
            }
        }
    }
    private static void createAutoAwayPoint(CCUHsApi hayStack, String equipdis, String siteRef,
                                            String equipref, String roomRef, String floorRef ,String tz) {
        Point autoAwaySetback = new Point.Builder()
                .setDisplayName(equipdis + "-" + "autoAwaySetback")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("auto").addMarker("away").addMarker("setback").addMarker("sp")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String autoAwaySetbackId = hayStack.addPoint(autoAwaySetback);
        BuildingTunerUtil.updateTunerLevels(autoAwaySetbackId, roomRef, hayStack);
        hayStack.writePointForCcuUser(autoAwaySetbackId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 2.0, 0);
        hayStack.writeHisValById(autoAwaySetbackId, HSUtil.getPriorityVal(autoAwaySetbackId));
    }
}
