package a75f.io.logic.oao;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.oao.FreeCoolingPoint;

public class OAOFreeCoolingReasonMigration {
    private final static String OAO_FREE_COOLING_REASON_MIGRATION = "OAO_FREE_COOLING_REASON_MIGRATION";
    private final static String MAT = "mat";
    private final static String DCV = "dcv";

    public static void OAOFreeCoolingReasonMigration() {
        new OAOFreeCoolingReasonMigration().checkForOAOFreeCoolingReasonMigration();
    }

    private void checkForOAOFreeCoolingReasonMigration(){
        if (!CCUHsApi.getInstance().isOAOFreeCoolingPointsMigrationDone()) {
            Log.i(OAO_FREE_COOLING_REASON_MIGRATION,"OAO free cooling reason migration started ");
            upgradeOAOWithFreeCoolingPoints(CCUHsApi.getInstance());
            CCUHsApi.getInstance().setOAOFreeCoolingPointsMigrationStatus(true);
        }
    }

    private void upgradeOAOWithFreeCoolingPoints(CCUHsApi hayStack){
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteDis = (String) siteMap.get("dis");
        String[] coolingReasons = {"dcv", "mat"};
        HashMap equipment = hayStack.read("equip and oao");
        Equip equip = new Equip.Builder().setHashMap(equipment).build();
        String nodeAddress = equip.getGroup();
        for(String coolingReason : coolingReasons){
            if(!isFreeCoolingPointCreated(hayStack, nodeAddress, coolingReason)){
                hayStack.addPoint(FreeCoolingPoint.getFreeCoolingPoint(coolingReason+"Available",
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
