package a75f.io.logic.migration.heartbeat;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.util.PreferenceUtil;

public class HeartbeatTagMigration {
    private final static String LOG_TAG = "HeartbeatTagMigration";


    public static void initHeartbeatTagMigration() {
        new HeartbeatTagMigration().checkForHeartbeatTagMigration();
    }

    private void checkForHeartbeatTagMigration() {
        if (!PreferenceUtil.isHeartbeatMigrationDone()) {
            Log.i(LOG_TAG, "heartbeat migration started ");
            upgradePointswithHeartbeatTag(CCUHsApi.getInstance());
            PreferenceUtil.setHeartbeatTagMigrationStatus(true);
        }
    }

    private void upgradePointswithHeartbeatTag(CCUHsApi hayStack) {
        if (isHeartbeattunerCreated(hayStack)) {
            deleteHeartbeattunerPoint(hayStack);
            createwithNewtag(hayStack);
        }

    }

    private void createwithNewtag(CCUHsApi hayStack) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteDis = (String) siteMap.get("dis");

        //zone level and standalone
        createHeartbeatPoint(hayStack, "cpu", siteDis + "-CPU-", true);
        createHeartbeatPoint(hayStack, "hpu", siteDis + "-HPU-", true);
        createHeartbeatPoint(hayStack, "pipe2", siteDis + "-2PFCU-", true);
        createHeartbeatPoint(hayStack, "pipe4", siteDis + "-4PFCU-", true);
        createHeartbeatPoint(hayStack, "sse", siteDis + "-SSE-", true);

        //zone level and not standalone
        createHeartbeatPoint(hayStack, "ti", siteDis + "-TI-", false);
        createHeartbeatPoint(hayStack, "dab", siteDis + "-DAB-", false);
        createHeartbeatPoint(hayStack, "dualDuct", siteDis + "-DualDuct-", false);
        createHeartbeatPoint(hayStack, "emr", siteDis + "-EMR-", false);
        createHeartbeatPoint(hayStack, "oao", siteDis + "-OAO-", false);
        createHeartbeatPoint(hayStack, "pid", siteDis + "-PID-", false);
        createHeartbeatPoint(hayStack, "vav", siteDis + "-VAV-", false);
        createHeartbeatPoint(hayStack, "sense", siteDis + "-SENSE-", false);

    }

    private void createHeartbeatPoint(CCUHsApi hayStack, String profile, String equipDis,
                                      boolean isStandAlone) {
        String query = "equip and zone and " + profile;
        if (profile.equals("oao")) {
            query = "equip and " + "oao";
        }
        List<HashMap> equips = hayStack.readAll(query);
        equips.forEach(equipment -> {
            Equip equip = new Equip.Builder().setHashMap(equipment).build();
            String nodeAddress = equip.getGroup();
            //String equipDisplay = equipDis+nodeAddress;
            Point cmHeartBeatInterval = new Point.Builder()
                    .setDisplayName(equipDis + "-" + "cmHeartBeatInterval")
                    .setSiteRef(equip.getSiteRef())
                    .setEquipRef(equip.getId()).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his"
                    ).addMarker("his")
                    .addMarker("cm").addMarker("heartbeat").addMarker("interval").addMarker("sp")
                    .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                    .setUnit("m")
                    .setTz(equip.getTz())
                    .build();
            String cmHeartBeatIntervalId = hayStack.addPoint(cmHeartBeatInterval);
            hayStack.writePointForCcuUser(cmHeartBeatIntervalId,
                    TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 1.0, 0);
            hayStack.writeHisValById(cmHeartBeatIntervalId, 1.0);
        });
    }


    private boolean isHeartbeattunerCreated(CCUHsApi hayStack) {
        return hayStack.read("point and tuner and default and cm and heart and beat and interval") != null;
    }

    private boolean isHeartbeatToskipwithtagCreated(CCUHsApi hayStack, String nodeAddress) {
        return hayStack.read("point and heartbeat and group == \"" + nodeAddress + "\"").size() > 0;
    }

    private void deleteHeartbeattunerPoint(CCUHsApi hayStack) {
        hayStack.deleteEntityTree(hayStack.read("\"point and tuner and default and cm and heart " +
                "and beat and interval").get("id").toString());
    }

}
