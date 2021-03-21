package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

/**
 * Tuners are normally created when an equip is created.
 * Adding a tuner on upgrade builds would need to create them on existing equips by hand.
 *
 */
public class TunerUpgrades {
    
    
    public static void handleTunerUpgrades(CCUHsApi hayStack) {
        upgradeReheatZoneToDATMinDifferential(hayStack);
    }
    
    /*
     * Takes care of upgrades for vav specific tuner reheatZoneToDATMinDifferential
     */
    public static void upgradeReheatZoneToDATMinDifferential(CCUHsApi hayStack) {
    
        CcuLog.e(L.TAG_CCU_TUNER, "upgradeReheatZoneToDATMinDifferential ");
        //Create the tuner point on building tuner equip.
        HashMap tuner = hayStack.read("equip and tuner");
        Equip tunerEquip = new Equip.Builder().setHashMap(tuner).build();
        VavTuners.createReheatZoneToDATMinDifferentialTuner(true, tunerEquip.getDisplayName(), tunerEquip.getId(),
                                                                tunerEquip.getSiteRef(), hayStack.getTimeZone());
        
        
        //Create the tuner point on all vav equips
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav");
        vavEquips.forEach(equip -> {
            Equip vavEquip = new Equip.Builder().setHashMap(equip).build();
            VavTuners.createReheatZoneToDATMinDifferentialTuner(true, vavEquip.getDisplayName(), vavEquip.getId(),
                                                                vavEquip.getSiteRef(), hayStack.getTimeZone());
        });
    }
}
