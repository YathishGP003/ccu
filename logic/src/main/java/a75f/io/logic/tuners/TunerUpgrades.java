package a75f.io.logic.tuners;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

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

import static a75f.io.logic.tuners.TunerConstants.TUNER_BUILDING_VAL_LEVEL;
import static a75f.io.logic.tuners.TunerConstants.TUNER_EQUIP_VAL_LEVEL;

/**
 * Tuners are normally created when an equip is created.
 * Adding a tuner on upgrade builds would need to create them on existing equips by hand.
 *
 */
public class TunerUpgrades {
    
    private static final String PREF_TUNER_EQUIP_LEVEL_RESET = "buildingTunerEquipLevelReset";
    
    /**
     * Takes care creating new tuners on existing equips during an upgrade.
     */
    public static void handleTunerUpgrades(CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_TUNER, " handleTunerUpgrades ");
        upgradeReheatZoneToDATMinDifferential(hayStack);
        upgradeDcwbBuildingTuners(hayStack);
        createDefaultTempLockoutPoints(hayStack);
        addVavDischargeTempTuners(hayStack);
    }
    
    /**
     * Takes care of upgrades for vav specific tuner reheatZoneToDATMinDifferential
     * All the builds upto 1.556 needs this migration
     */
    private static void upgradeReheatZoneToDATMinDifferential(CCUHsApi hayStack) {
    
        //Make sure the tuner does not exist before creating it. A duplicate tuner if ever created can be hard to
        //track and fix.
        ArrayList<HashMap<Object, Object>> reheatTuners = hayStack.readAllEntities("point and tuner and reheat and dat and min and differential");
        if (reheatTuners.size() > 0) {
            CcuLog.e(L.TAG_CCU_TUNER, "reheatZoneToDATMinDifferential exists");
            return;
        }
    
        CcuLog.e(L.TAG_CCU_TUNER, "create ReheatZoneToDATMinDifferential ");
        //Create the tuner point on building tuner equip.
        HashMap<Object, Object> buildTuner = hayStack.readEntity(Queries.EQUIP_AND_TUNER);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();
        Point buildingTunerPoint = VavTuners.createReheatZoneToDATMinDifferentialTuner(true,
                                                                                       tunerEquip.getDisplayName(), tunerEquip.getId(),
                                                                                        null, tunerEquip.getSiteRef(),
                                                                                       hayStack.getTimeZone());
    
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
                                                                                        vavEquip.getRoomRef(), vavEquip.getSiteRef(),
                                                                                        hayStack.getTimeZone());
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
        if (rdcwbTuners.size() > 0) {
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
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readEntity("equip and system");
        if (!equipMap.isEmpty()) {
            Equip systemEquip = new Equip.Builder().setHashMap(equipMap).build();
            if (ProfileType.valueOf(systemEquip.getProfile()) == ProfileType.SYSTEM_DAB_ANALOG_RTU ) {
                DcwbTuners.addEquipDcwbTuners(hayStack, systemEquip.getSiteRef(), systemEquip.getDisplayName(),
                                              systemEquip.getId(), systemEquip.getTz());
            }
        }
        
    }
    
    /**
     * Some of the building level tuners have incorrectly been written to level 8.
     * We have now changed it write to level 16.
     * This method does the clean up job to clear those level 8 writes.
     * This could be removed in future once all the Sites are migrated to 1.568.0 or later versions of CCU.
     */
    public static void handleBuildingTunerForceClear(Context context, CCUHsApi hayStack) {
        boolean isPendingBuildingTunerReset = PreferenceManager.getDefaultSharedPreferences(context)
                                                   .getBoolean(PREF_TUNER_EQUIP_LEVEL_RESET, false);
        if (!isPendingBuildingTunerReset) {
            forceClearBuildingTunerEquipLevel(hayStack);
    
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                             .putBoolean(PREF_TUNER_EQUIP_LEVEL_RESET, true)
                             .apply();
        }
    
    }
    
    private static void forceClearBuildingTunerEquipLevel(CCUHsApi hayStack) {
        HashMap<Object, Object> buildingCoolingUpperLimit = hayStack.readEntity("point and limit and max and cooling and user");
        forceExpireEquipLevel(buildingCoolingUpperLimit.get("id").toString(), hayStack);
        HashMap<Object, Object> buildingHeatingUpperLimit = hayStack.readEntity("point and limit and max and heating and user");
        forceExpireEquipLevel(buildingHeatingUpperLimit.get("id").toString(), hayStack);
        HashMap<Object, Object> buildingCoolingLowerLimit = hayStack.readEntity("point and limit and min and cooling and user");
        forceExpireEquipLevel(buildingCoolingLowerLimit.get("id").toString(), hayStack);
        HashMap<Object, Object> buildingHeatingLowerLimit = hayStack.readEntity("point and limit and min and heating and user");
        forceExpireEquipLevel(buildingHeatingLowerLimit.get("id").toString(), hayStack);
        HashMap<Object, Object> buildingMin = hayStack.readEntity("building and limit and min");
        forceExpireEquipLevel(buildingMin.get("id").toString(), hayStack);
        HashMap<Object, Object> buildingMax = hayStack.readEntity("building and limit and max");
        forceExpireEquipLevel(buildingMax.get("id").toString(), hayStack);
    }
    
    private static void forceExpireEquipLevel(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> equipLevelVal = HSUtil.getPriorityLevel(id, TUNER_EQUIP_VAL_LEVEL);
        if (equipLevelVal.isEmpty() ||
            equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_WHO) == null ||
            equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_VAL) == null) {
            CcuLog.i(L.TAG_CCU_TUNER,"Level 8 does not exist for "+id);
            return;
        }
        hayStack.writePoint(id, TUNER_BUILDING_VAL_LEVEL,
                            equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_WHO).toString(),
                            Double.parseDouble(equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_VAL).toString()),
                            (int)Double.parseDouble(equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_DURATION).toString()));
    
        hayStack.clearPointArrayLevel(id, TUNER_EQUIP_VAL_LEVEL, false);
        CcuLog.i(L.TAG_CCU_TUNER,"Cleared level 8 : "+id);
    }

    /*
    To change min and max of heating desired temperature
     */
    public static void updateHeatingMinMax(CCUHsApi hayStack) {

        Log.d("TunerUpdate", "updateHeatingMinMax ++");
        HashMap<Object, Object> heatDTMin = hayStack.readEntity("point and limit and min and heating " +
                "and user");
        HashMap<Object, Object> heatDTMax = hayStack.readEntity("point and limit and max and heating " +
                "and user");
        String minId = heatDTMin.get("id").toString();
        String maxId = heatDTMax.get("id").toString();
        forceExpireBuildingLevel(minId,hayStack);
        forceExpireBuildingLevel(maxId,hayStack);

        hayStack.writePointForCcuUser(maxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 72.0,
                0);
        hayStack.writeHisValById(maxId, 72.0);

        hayStack.writePointForCcuUser(minId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 67.0,
                0);
        hayStack.writeHisValById(minId, 67.0);

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
                "air and discharge and offset and vav and tuner and max and default");

        //Create the tuner point on building tuner equip.
        HashMap<Object, Object> buildTuner = hayStack.readEntity(Queries.EQUIP_AND_TUNER);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();
        //Create the tuner point on all vav equips
        ArrayList<HashMap<Object, Object>> vavEquips = hayStack.readAllEntities("equip and vav");

            if (maxDischargeTempTuner.isEmpty()) {
                CcuLog.i(L.TAG_CCU_TUNER,"maxDischargeTempTuner tuners not found, Creating new tuners");
                Point reheatZoneMaxDischargeTempTuner = VavTuners.createMaxDischargeTempTuner(true,   tunerEquip.getDisplayName(), tunerEquip.getId(),
                        null, tunerEquip.getSiteRef(),
                        hayStack.getTimeZone());

                String maxDischargeTempTunerID = hayStack.addPoint(reheatZoneMaxDischargeTempTuner);
                hayStack.writePointForCcuUser(maxDischargeTempTunerID, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                        BuildingTunerFallback.getDefaultTunerVal("discharge and air and temp and max"), 0);
                CCUHsApi.getInstance().writeHisValById(maxDischargeTempTunerID, BuildingTunerFallback.getDefaultTunerVal("discharge and air and temp and max"));

                CcuLog.i(L.TAG_CCU_TUNER,"maxDischargeTempTuner tuners not found, Creating new tuners");
                vavEquips.forEach(equip -> {
                    Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
                    Point equipTunerPoint = VavTuners.createMaxDischargeTempTuner(false,
                            vavEquip.getDisplayName(), vavEquip.getId(),
                            vavEquip.getRoomRef(), vavEquip.getSiteRef(),
                            hayStack.getTimeZone());

                    String equipTunerPointId = hayStack.addPoint(equipTunerPoint);
                    BuildingTunerUtil.updateTunerLevels(equipTunerPointId, vavEquip.getRoomRef(), hayStack);
                    hayStack.writeHisValById(equipTunerPointId, HSUtil.getPriorityVal(equipTunerPointId));
                });
            }else{
                CcuLog.i(L.TAG_CCU_TUNER,"maxDischargeTempTuner found "+maxDischargeTempTuner.size());
            }

            if (dischargeTempOffsetTuner.isEmpty()) {
                Point reheatZoneMaxDischargeTempOffsetTuner = VavTuners.createDischargeTempOffsetTuner(true,
                        tunerEquip.getDisplayName(), tunerEquip.getId(),
                        null, tunerEquip.getSiteRef(),
                        hayStack.getTimeZone());

                String reheatZoneDischargeTempOffSetTunerId = hayStack.addPoint(reheatZoneMaxDischargeTempOffsetTuner);
                hayStack.writePointForCcuUser(reheatZoneDischargeTempOffSetTunerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                        BuildingTunerFallback.getDefaultTunerVal("discharge and air and temp and offset"), 0);
                hayStack.writeHisValById(reheatZoneDischargeTempOffSetTunerId,
                        BuildingTunerFallback.getDefaultTunerVal("discharge and air and temp and offset"));


                vavEquips.forEach(equip -> {
                    Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
                    Point equipTunerPoint = VavTuners.createDischargeTempOffsetTuner(false,
                            vavEquip.getDisplayName(), vavEquip.getId(),
                            vavEquip.getRoomRef(), vavEquip.getSiteRef(),
                            hayStack.getTimeZone());

                    String equipTunerPointId = hayStack.addPoint(equipTunerPoint);
                    BuildingTunerUtil.updateTunerLevels(equipTunerPointId, vavEquip.getRoomRef(), hayStack);
                    hayStack.writeHisValById(equipTunerPointId, HSUtil.getPriorityVal(equipTunerPointId));
                  });
            }else
                CcuLog.i(L.TAG_CCU_TUNER,"dischargeTempOffsetTuner found "+dischargeTempOffsetTuner.size());

        }

    }
