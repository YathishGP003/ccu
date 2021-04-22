package a75f.io.logic.tuners;

import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;

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
}
