package a75f.io.logic.util;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.Alert;
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

        if (!PreferenceUtil.areDuplicateAlertsRemoved()) {
            removeDuplicateAlerts(AlertManager.getInstance());
            PreferenceUtil.removedDuplicateAlerts();
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

    /**
     * A duplicate alert exists when an older, *unsynced* alert with the same title and equipId also exists.
     * This keep only the oldest alert, while deleting the others.
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
//                        alertManager.deleteAlert(alert);
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
}