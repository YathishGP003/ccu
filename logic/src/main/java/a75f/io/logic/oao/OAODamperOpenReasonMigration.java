package a75f.io.logic.oao;

import android.util.Log;

import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.oao.OAODamperOpenPoint;

public class OAODamperOpenReasonMigration {
    private final static String OAO_DAMPER_OPEN_REASON_MIGRATION = "OAO_DAMPER_OPEN_REASON_MIGRATION";
    private final static String MAT = "mat";
    private final static String DCV = "dcv";

    public static void initOAOFreeCoolingReasonMigration() {
        new OAODamperOpenReasonMigration().checkForOAODamperOpenReasonMigration();
    }

    private void checkForOAODamperOpenReasonMigration(){
        if (!CCUHsApi.getInstance().isOAODamperOpenPointsMigrationDone()) {
            Log.i(OAO_DAMPER_OPEN_REASON_MIGRATION,"OAO Damper open reason point migration started ");
            upgradeOAOWithFreeCoolingPoints(CCUHsApi.getInstance());
            CCUHsApi.getInstance().setOAODamperOpenPointsMigrationStatus(true);
        }
    }

    private void upgradeOAOWithFreeCoolingPoints(CCUHsApi hayStack){
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteDis = (String) siteMap.get("dis");
        String[] coolingReasons = {"dcv", "mat"};
        HashMap equipment = hayStack.read("equip and oao");
        if(equipment.size() == 0){
            return;
        }
        Equip equip = new Equip.Builder().setHashMap(equipment).build();
        String nodeAddress = equip.getGroup();
        for(String coolingReason : coolingReasons){
            if(!isFreeCoolingPointCreated(hayStack, nodeAddress, coolingReason)){
                Log.i("OAO_DAMPER_OPEN_REASON_MIGRATION","point added for OAO for "+coolingReason);
                hayStack.addPoint(OAODamperOpenPoint.getDamperOpenPoint(coolingReason+"Available",
                        siteDis, equip.getSiteRef(), equip.getRoomRef(), equip.getId(), Integer.parseInt(nodeAddress),
                        equip.getFloorRef(),equip.getTz(), coolingReason));
            }
        }

    }
    private boolean isFreeCoolingPointCreated(CCUHsApi hayStack ,String nodeAddress, String freeCoolingReason){
        return hayStack.read("point and oao and  available and "+freeCoolingReason+" and " +
                "group == \""+nodeAddress+"\"").size() > 0;
    }
}
