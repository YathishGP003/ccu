package a75f.io.logic.migration.heartbeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.util.PreferenceUtil;

public class HeartbeatTagMigration {
    private final static String LOG_TAG = "HeartbeatTagMigration";


    public static void initHeartbeatTagMigration() {
        CcuLog.i("heartbeattag", "In initHeartbeatTagMigration++ ");
        new HeartbeatTagMigration().checkForHeartbeatTagMigration();
    }

    private void checkForHeartbeatTagMigration() {
        CcuLog.i(LOG_TAG, "In checkForHeartbeatTagMigration++ ");
        if (!PreferenceUtil.isHeartbeatTagMigrationDone()) {
            CcuLog.i(LOG_TAG, "heartbeat migration started ");
            upgradePointswithHeartbeatTag(CCUHsApi.getInstance());
            PreferenceUtil.setHeartbeatTagMigrationStatus(true);
        }
    }

    private void upgradePointswithHeartbeatTag(CCUHsApi hayStack) {
        CcuLog.i(LOG_TAG, "upgradePointswithHeartbeatTag");

        List<HashMap> updateHBPointList = new ArrayList<>();
        List<HashMap> updateHBSPointList = new ArrayList<>();

        HashMap defaultCmHB = hayStack.read("point and tuner and default and cm and heart " +
                "and beat and interval");
        if(defaultCmHB.get("id") != null) updateHBPointList.add(defaultCmHB);
        HashMap defaultHBS =  hayStack.read("point and tuner and default and heart and beats " +
                "and skip");
        if(defaultHBS.get("id")  != null) updateHBSPointList.add(defaultHBS);
        HashMap sysCmHB = hayStack.read("point and tuner and system and cm and heart " +
                "and beat and interval");
        if(sysCmHB.get("id")  != null) updateHBPointList.add(sysCmHB);
        HashMap sysHBS =  hayStack.read("point and tuner and system and heart and beats " +
                "and skip");
        if(sysHBS.get("id")  != null) updateHBSPointList.add(sysHBS);

        for (HashMap item:updateHBPointList) {
            Point updatePoint = new Point.Builder().setHashMap(item).removeMarker("heart").removeMarker("beat").addMarker("heartbeat").build();
            CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + updatePoint.getDisplayName() + "," + updatePoint.getMarkers().toString() + "," + item.get("id").toString() + "," + updatePoint.getId());
            CCUHsApi.getInstance().updatePoint(updatePoint,item.get("id").toString());
        }

        for (HashMap item:updateHBSPointList) {
            Point updatePoint = new Point.Builder().setHashMap(item).removeMarker("heart").removeMarker("beats")
                    .removeMarker("to").removeMarker("skip")
                    .addMarker("heartbeat").build();
            CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + updatePoint.getDisplayName() + "," + updatePoint.getMarkers().toString() + "," + item.get("id").toString() + "," + updatePoint.getId());
            CCUHsApi.getInstance().updatePoint(updatePoint,item.get("id").toString());
        }
        CCUHsApi.getInstance().scheduleSync();

    }
}
