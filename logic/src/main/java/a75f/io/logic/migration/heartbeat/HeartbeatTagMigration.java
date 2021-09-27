package a75f.io.logic.migration.heartbeat;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.util.PreferenceUtil;

public class HeartbeatTagMigration {
    private final static String LOG_TAG = "HeartbeatTagMigration";


    public static void initHeartbeatTagMigration() {
        Log.d("heartbeattag", "In initHeartbeatTagMigration++ ");
        new HeartbeatTagMigration().checkForHeartbeatTagMigration();
    }

    private void checkForHeartbeatTagMigration() {
        Log.d("heartbeattag", "In checkForHeartbeatTagMigration++ ");
        if (!PreferenceUtil.isHeartbeatTagMigrationDone()) {
            Log.d(LOG_TAG, "heartbeat migration started ");
            upgradePointswithHeartbeatTag(CCUHsApi.getInstance());
            PreferenceUtil.setHeartbeatTagMigrationStatus(true);
        }
    }

    private void upgradePointswithHeartbeatTag(CCUHsApi hayStack) {
        Log.d("heartbeattag", "upgradePointswithHeartbeatTag");

            deleteHeartbeattunerPoint(hayStack);
            createwithNewtag(hayStack);


    }

    private void createwithNewtag(CCUHsApi hayStack) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteDis = (String) siteMap.get("dis");
        createHeartbeatPoint(hayStack,siteDis);
        createHeartbeatskipPoint(hayStack,siteDis);
    }

    private void createHeartbeatPoint(CCUHsApi hayStack,String site) {
        String query = "equip ";
        List<HashMap> equips = hayStack.readAll(query);
        HashMap sysequip = CCUHsApi.getInstance().read("equip and system");
        String equipRef = sysequip.get("id").toString();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();



        equips.forEach(equipment -> {
            Equip equip = new Equip.Builder().setHashMap(equipment).build();
            String nodeAddress = equip.getGroup();

            Point cmHeartBeatInterval  = new Point.Builder()
                    .setDisplayName(HSUtil.getDis(equip.getId())+"-"+"BuildingTuner"+"-"+"cmHeartBeatInterval")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                    .addMarker("cm").addMarker("heartbeat").addMarker("interval").addMarker("sp")
                    .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                    .setUnit("m")
                    .setTz(tz)
                    .build();
            String cmHeartBeatIntervalId = hayStack.addPoint(cmHeartBeatInterval);
            hayStack.writePointForCcuUser(cmHeartBeatIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,1.0, 0);
            hayStack.writeHisValById(cmHeartBeatIntervalId, 1.0);

            Point syscmHeartBeatInterval = new Point.Builder().setDisplayName(HSUtil.getDis(equip.getId()) + "-" + "cmHeartBeatInterval")
                    .setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner")
                    .addMarker("writable").addMarker("his").addMarker("cm").addMarker("heartbeat").addMarker("interval").addMarker("level").addMarker("sp")
                    .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                    .setUnit("m")
                    .setTz(tz).build();
            String syscmHeartBeatIntervalId = hayStack.addPoint(syscmHeartBeatInterval);
           /* HashMap cmHeartBeatIntervalPoint = hayStack.read("point and tuner and default and cm and heartbeat and interval");
            ArrayList<HashMap> cmHeartBeatIntervalArr = hayStack.readPoint(cmHeartBeatIntervalPoint.get("id").toString());
            for (HashMap valMap : cmHeartBeatIntervalArr)
            {
                if (valMap.get("val") != null)
                {
                    hayStack.pointWrite(HRef.copy(syscmHeartBeatIntervalId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                    hayStack.writeHisValById(syscmHeartBeatIntervalId, Double.parseDouble(valMap.get("val").toString()));
                }
            }*/



        });
    }

    private void createHeartbeatskipPoint(CCUHsApi hayStack, String site) {
        String query = "equip ";
        List<HashMap> equips = hayStack.readAll(query);
        HashMap sysequip = CCUHsApi.getInstance().read("equip and system");
        String equipRef = sysequip.get("id").toString();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();

        equips.forEach(equipment -> {
            Equip equip = new Equip.Builder().setHashMap(equipment).build();
            Point defheartBeatsToSkip  = new Point.Builder()
                    .setDisplayName(HSUtil.getDis(equip.getId())+"-"+"BuildingTuner"+"-"+"heartBeatsToSkip")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                    .addMarker("heartbeat").addMarker("sp")
                    .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                    .setTz(tz)
                    .build();
            String defheartBeatsToSkipId = hayStack.addPoint(defheartBeatsToSkip);
            hayStack.writePointForCcuUser(defheartBeatsToSkipId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,5.0, 0);
            hayStack.writeHisValById(defheartBeatsToSkipId, 5.0);

            Point heartBeatsToSkip = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "heartBeatsToSkip")
                    .setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner")
                    .addMarker("writable").addMarker("his").addMarker("heartbeat").addMarker("sp")
                    .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                    .setTz(tz).build();
            String heartBeatsToSkipId = hayStack.addPoint(heartBeatsToSkip);
            HashMap heartBeatsToSkipPoint = hayStack.read("point and tuner and default and heartbeat");
            ArrayList<HashMap> heartBeatsToSkipArr = hayStack.readPoint(heartBeatsToSkipPoint.get("id").toString());
            for (HashMap valMap : heartBeatsToSkipArr)
            {
                if (valMap.get("val") != null)
                {
                    hayStack.pointWrite(HRef.copy(heartBeatsToSkipId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                    hayStack.writeHisValById(heartBeatsToSkipId, Double.parseDouble(valMap.get("val").toString()));
                }
            }





        });
    }


    private boolean isHeartbeattunerCreated(CCUHsApi hayStack) {
        Log.d("heartbeattag", "isHeartbeattunerCreated"+ (hayStack.read("point and tuner and cm and heart and beat and interval") != null));
        return hayStack.read("point and tuner and cm and heart and beat and interval") != null;
    }

    private boolean isHeartbeatToskipwithtagCreated(CCUHsApi hayStack, String nodeAddress) {
        return hayStack.read("point and heartbeat and group == \"" + nodeAddress + "\"").size() > 0;
    }

    private void deleteHeartbeattunerPoint(CCUHsApi hayStack) {

        List<HashMap> delList = new ArrayList<HashMap>();

        HashMap defaultCmHB = hayStack.read("point and tuner and default and cm and heart " +
                "and beat and interval");
        if(defaultCmHB.get("id") != null) delList.add(defaultCmHB);
        HashMap defaultHBS =  hayStack.read("point and tuner and default and heart and beats " +
                "and skip");
        if(defaultHBS.get("id")  != null) delList.add(defaultHBS);
        HashMap sysCmHB = hayStack.read("point and tuner and system and cm and heart " +
                "and beat and interval");
        if(sysCmHB.get("id")  != null) delList.add(sysCmHB);
        HashMap sysHBS =  hayStack.read("point and tuner and system and heart and beats " +
                "and skip");
        if(sysHBS.get("id")  != null) delList.add(sysHBS);

        for (HashMap item:delList) {
            hayStack.deleteEntityTree(item.get("id").toString());
        }


    }

}
