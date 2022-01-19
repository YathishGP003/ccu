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
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;

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
        upgradeReheatZoneToDATMinDifferential(hayStack);
        upgradeDcwbBuildingTuners(hayStack);
    }
    
    /**
     * Takes care of upgrades for vav specific tuner reheatZoneToDATMinDifferential
     * All the builds upto 1.556 needs this migration
     */
    private static void upgradeReheatZoneToDATMinDifferential(CCUHsApi hayStack) {
    
        //Make sure the tuner does not exist before creating it. A duplicate tuner if ever created can be hard to
        //track and fix.
        ArrayList<HashMap> reheatTuners = hayStack.readAll("point and tuner and reheat and dat and min and differential");
        if (reheatTuners.size() > 0) {
            CcuLog.e(L.TAG_CCU_TUNER, "reheatZoneToDATMinDifferential exists");
            return;
        }
    
        CcuLog.e(L.TAG_CCU_TUNER, "create ReheatZoneToDATMinDifferential ");
        //Create the tuner point on building tuner equip.
        HashMap buildTuner = hayStack.read("equip and tuner");
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
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav");
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
        ArrayList<HashMap> rdcwbTuners = hayStack.readAll("point and tuner and dcwb and default");
        if (rdcwbTuners.size() > 0) {
            CcuLog.e(L.TAG_CCU_TUNER, "dcwbTuners exist");
            return;
        }
    
        CcuLog.e(L.TAG_CCU_TUNER, "create dcwbTuners ");
        //Create the tuner points on building tuner equip.
        HashMap buildTuner = hayStack.read("equip and tuner");
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();
        DcwbTuners.addDefaultDcwbTuners(hayStack, tunerEquip.getSiteRef(), tunerEquip.getId(),
                                        tunerEquip.getDisplayName(), tunerEquip.getTz());
    
    
        //If the current profile is DABFullyModulating, create new system equip tuner points.
        HashMap equipMap = CCUHsApi.getInstance().read("equip and system");
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
        HashMap buildingCoolingUpperLimit = hayStack.read("point and limit and max and cooling and user");
        forceExpireEquipLevel(buildingCoolingUpperLimit.get("id").toString(), hayStack);
        HashMap buildingHeatingUpperLimit = hayStack.read("point and limit and max and heating and user");
        forceExpireEquipLevel(buildingHeatingUpperLimit.get("id").toString(), hayStack);
        HashMap buildingCoolingLowerLimit = hayStack.read("point and limit and min and cooling and user");
        forceExpireEquipLevel(buildingCoolingLowerLimit.get("id").toString(), hayStack);
        HashMap buildingHeatingLowerLimit = hayStack.read("point and limit and min and heating and user");
        forceExpireEquipLevel(buildingHeatingLowerLimit.get("id").toString(), hayStack);
        HashMap buildingMin = hayStack.read("building and limit and min");
        forceExpireEquipLevel(buildingMin.get("id").toString(), hayStack);
        HashMap buildingMax = hayStack.read("building and limit and max");
        forceExpireEquipLevel(buildingMax.get("id").toString(), hayStack);
    }
    
    private static void forceExpireEquipLevel(String id, CCUHsApi hayStack) {
        HashMap equipLevelVal = HSUtil.getPriorityLevel(id, TUNER_EQUIP_VAL_LEVEL);
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
        HashMap<Object, Object> heatDTMin = hayStack.read("point and limit and min and heating " +
                "and user");
        HashMap<Object, Object> heatDTMax = hayStack.read("point and limit and max and heating " +
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
        HashMap buildingLevelVal = HSUtil.getPriorityLevel(id, TUNER_BUILDING_VAL_LEVEL);
        if (buildingLevelVal.isEmpty() ||
                buildingLevelVal.get(HayStackConstants.WRITABLE_ARRAY_WHO) == null ||
                buildingLevelVal.get(HayStackConstants.WRITABLE_ARRAY_VAL) == null) {
            CcuLog.i(L.TAG_CCU_TUNER,"Level 16 does not exist for "+id);
            return;
        }
        hayStack.clearPointArrayLevel(id, TUNER_BUILDING_VAL_LEVEL, false);
        CcuLog.i(L.TAG_CCU_TUNER,"Cleared level 16 : "+id);
    }
}
