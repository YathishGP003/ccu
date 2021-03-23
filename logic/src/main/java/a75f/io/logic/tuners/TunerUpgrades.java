package a75f.io.logic.tuners;

import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

/**
 * Tuners are normally created when an equip is created.
 * Adding a tuner on upgrade builds would need to create them on existing equips by hand.
 *
 */
public class TunerUpgrades {
    
    //There method could handle more tuners in future.
    public static void handleTunerUpgrades(CCUHsApi hayStack) {
        upgradeReheatZoneToDATMinDifferential(hayStack);
    }
    
    /*
     * Takes care of upgrades for vav specific tuner reheatZoneToDATMinDifferential
     */
    public static void upgradeReheatZoneToDATMinDifferential(CCUHsApi hayStack) {
    
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
                                                                                        tunerEquip.getSiteRef(), hayStack.getTimeZone());
    
        String buildingReheatZoneToDATMinDifferentialId = hayStack.addPoint(buildingTunerPoint);
        hayStack.writeDefaultValById(buildingReheatZoneToDATMinDifferentialId,
                                     TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL);
        hayStack.writeHisValById(buildingReheatZoneToDATMinDifferentialId, TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL);
        
        //Create the tuner point on all vav equips
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav");
        vavEquips.forEach(equip -> {
            Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
            Point equipTunerPoint = VavTuners.createReheatZoneToDATMinDifferentialTuner(false,
                                                                                        vavEquip.getDisplayName(),
                                                                                           vavEquip.getId(), vavEquip.getSiteRef(), hayStack.getTimeZone());
            String reheatZoneToDATMinDifferentialId = hayStack.addPoint(equipTunerPoint);
            hayStack.writeDefaultValById(reheatZoneToDATMinDifferentialId,
                                  TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL);
            hayStack.writeHisValById(reheatZoneToDATMinDifferentialId, TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL);
        });
    }
}
