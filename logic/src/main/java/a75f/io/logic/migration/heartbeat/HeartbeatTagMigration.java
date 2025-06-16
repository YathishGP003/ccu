package a75f.io.logic.migration.heartbeat;

import org.projecthaystack.HDict;

import java.util.ArrayList;
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

        List<HDict> updateHBPointList = new ArrayList<>();
        List<HDict> updateHBSPointList = new ArrayList<>();

        HDict defaultCmHB = hayStack.readHDict("point and tuner and default and cm and heart " +
                "and beat and interval");
        if(defaultCmHB.get("id") != null) updateHBPointList.add(defaultCmHB);
        HDict defaultHBS =  hayStack.readHDict("point and tuner and default and heart and beats " +
                "and skip");
        if(defaultHBS.get("id")  != null) updateHBSPointList.add(defaultHBS);
        HDict sysCmHB = hayStack.readHDict("point and tuner and system and cm and heart " +
                "and beat and interval");
        if(sysCmHB.get("id")  != null) updateHBPointList.add(sysCmHB);
        HDict sysHBS =  hayStack.readHDict("point and tuner and system and heart and beats " +
                "and skip");
        if(sysHBS.get("id")  != null) updateHBSPointList.add(sysHBS);

        for (HDict item : updateHBPointList) {
            Point updatePoint = new Point.Builder().setHDict(item).removeMarker("heart").removeMarker("beat").addMarker("heartbeat").build();
            CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + updatePoint.getDisplayName() + "," + updatePoint.getMarkers().toString() + "," + item.get("id").toString() + "," + updatePoint.getId());
            CCUHsApi.getInstance().updatePoint(updatePoint,item.get("id").toString());
        }

        for (HDict item:updateHBSPointList) {
            Point updatePoint = new Point.Builder().setHDict(item).removeMarker("heart").removeMarker("beats")
                    .removeMarker("to").removeMarker("skip")
                    .addMarker("heartbeat").build();
            CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + updatePoint.getDisplayName() + "," + updatePoint.getMarkers().toString() + "," + item.get("id").toString() + "," + updatePoint.getId());
            CCUHsApi.getInstance().updatePoint(updatePoint,item.get("id").toString());
        }
        CCUHsApi.getInstance().scheduleSync();

    }
}
