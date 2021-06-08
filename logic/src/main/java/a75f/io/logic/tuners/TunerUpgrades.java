package a75f.io.logic.tuners;

import android.widget.ArrayAdapter;

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
    
    /**
     * Takes care creating new tuners on existing equips during an upgrade.
     */
    public static void handleTunerUpgrades(CCUHsApi hayStack) {
        upgradeReheatZoneToDATMinDifferential(hayStack);
        upgradeDcwbBuildingTuners(hayStack);
        forceClearBuildingTunerEquipLevel(hayStack);
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
     * Some of the building level tuner have incorrectly been writing to written to level 8.
     * We have now changed it write to level 16.
     * It is a clean up job to clear those level 8 writes.
     * This could be removed in future once all the Sites are migrated to 1.568.0 or later versions of CCU.
     */
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
        if (equipLevelVal.isEmpty()) {
            return;
        }
        hayStack.writePoint(id, TUNER_BUILDING_VAL_LEVEL,
                            equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_WHO).toString(),
                            (double)equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_VAL),
                            (int)equipLevelVal.get(HayStackConstants.WRITABLE_ARRAY_DURATION));
    
        hayStack.clearPointArrayLevel(id, TUNER_EQUIP_VAL_LEVEL, false);
    }
}
