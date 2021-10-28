package a75f.io.logic.util;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

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
}
