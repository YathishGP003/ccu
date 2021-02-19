package a75f.io.logic.tuners;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

import static a75f.io.logic.tuners.TunerConstants.DEFAULT_MODE_CHANGEOVER_HYSTERESIS;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_DOWN_TIMER_COUNTER;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_UP_TIMER_COUNTER;

/**
 * Created by samjithsadasivan on 10/5/18.
 */

public class BuildingTuners
{
    
    private String equipRef;
    private String equipDis;
    private String siteRef;
    private String tz;
    CCUHsApi hayStack;
    
    private static BuildingTuners instance = null;
    private BuildingTuners(){
        addBuildingTunerEquip();
    }
    
    public static BuildingTuners getInstance() {
        if (instance == null) {
            instance = new BuildingTuners();
        }
        return instance;
    }
    
    public void addBuildingTunerEquip() {
        hayStack = CCUHsApi.getInstance();
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        if (tuner != null && tuner.size() > 0) {
            equipRef = tuner.get("id").toString();
            equipDis = tuner.get("dis").toString();
            HashMap siteMap = hayStack.read(Tags.SITE);
            siteRef = siteMap.get(Tags.ID).toString();
            tz = siteMap.get("tz").toString();
            CcuLog.d(L.TAG_CCU_SYSTEM,"BuildingTuner equip already present");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"BuildingTuner Equip does not exist. Create Now");
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = siteMap.get(Tags.ID).toString();
        String siteDis = siteMap.get("dis").toString();
        Equip tunerEquip= new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-BuildingTuner")
                          .addMarker("equip").addMarker("tuner").addMarker("his")
                          .setTz(siteMap.get("tz").toString())
                          .build();
        equipRef = hayStack.addEquip(tunerEquip);
        equipDis = siteDis+"-BuildingTuner";
        tz = siteMap.get("tz").toString();

        addDefaultBuildingTuners();
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    /**
     * All the new tuners with are being added here.
     * This should be done neatly.
     */
    public void updateBuildingTuners() {
        TITuners.addDefaultTiTuners(hayStack, siteRef, equipRef, equipDis, tz);
        DualDuctTuners.addDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz);
        OAOTuners.updateNewTuners(hayStack, siteRef,equipRef, equipDis,tz,false);
        updateDabBuildingTuners();
        updateVavBuildingTuners();
        checkForTunerMigration();
    }

    private void checkForTunerMigration() {
        PackageManager manager = Globals.getInstance().getApplicationContext().getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(Globals.getInstance().getApplicationContext().getPackageName(), 0);
            String tunerVersion = info.versionName + "." + info.versionCode;

            if (!CCUHsApi.getInstance().getTunerVersion().equals(tunerVersion)) {
                CCUHsApi.getInstance().setTunerVersion(tunerVersion);
                doTunerMigrationJob();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void doTunerMigrationJob() {
        ArrayList<HashMap> tunersList = CCUHsApi.getInstance().readAll("tuner and tunerGroup");

        for (HashMap tunerMap : tunersList) {
            String tunerMapDis = tunerMap.get("dis").toString();
            String tunerMapShortDis = tunerMapDis.substring(tunerMapDis.lastIndexOf("-") + 1).trim();
            TunerMigration.migrateTunerIfRequired(tunerMap.get("id").toString(), tunerMapShortDis);
        }
    }

    public void addDefaultBuildingTuners() {
    
        GenericTuners.addDefaultGenericTuners(hayStack, siteRef, equipRef, equipDis, tz);
        VavTuners.addDefaultVavTuners(hayStack, siteRef, equipRef, equipDis, tz);
        PlcTuners.addDefaultPlcTuners(hayStack, siteRef, equipRef, equipDis, tz);
        StandAloneTuners.addDefaultStandaloneTuners(hayStack, siteRef, equipRef, equipDis, tz);
        DabTuners.addDefaultDabTuners(hayStack, siteRef, equipRef, equipDis, tz);
        TITuners.addDefaultTiTuners(hayStack, siteRef, equipRef, equipDis, tz);
        OAOTuners.addDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz);
        DualDuctTuners.addDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz);
        AlertTuners.addDefaultAlertTuners(hayStack, siteRef, equipRef, equipDis, tz);
        TemperatureLimitTuners.addDefaultTempLimitTuners(hayStack, siteRef, equipRef, equipDis, tz);
        TimerTuners.addDefaultTimerTuners(hayStack, siteRef, equipRef, equipDis, tz);
    }

    private void updateDabBuildingTuners() {
        HashMap<Object, Object> modeChangeoverHysteresisPoint = CCUHsApi.getInstance()
                                                                        .readEntity("tuner and default and mode and " +
                                                                                    "changeover and hysteresis");
        if (modeChangeoverHysteresisPoint.isEmpty()) {
            Point modeChangeoverHysteresis = new Point.Builder().setDisplayName(equipDis + "-DAB-" +
                                                                                "modeChangeoverHysteresis")
                                                                .setSiteRef(siteRef)
                                                                .setEquipRef(equipRef)
                                                                .setHisInterpolate("cov")
                                                                .addMarker("tuner").addMarker("dab")
                                                                .addMarker("default").addMarker("writable").addMarker("his")
                                                                .addMarker("his").addMarker("mode").addMarker("changeover")
                                                                .addMarker("hysteresis").addMarker("sp")
                                                                .setMinVal("0")
                                                                .setMaxVal("5")
                                                                .setIncrementVal("0.5")
                                                                .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                                .setTz(tz)
                                                                .build();
            String modeChangeoverHysteresisId = hayStack.addPoint(modeChangeoverHysteresis);
            hayStack.writePointForCcuUser(modeChangeoverHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                DEFAULT_MODE_CHANGEOVER_HYSTERESIS, 0);
            hayStack.writeHisValById(modeChangeoverHysteresisId, DEFAULT_MODE_CHANGEOVER_HYSTERESIS);
        }

        HashMap<Object, Object> stageUpTimerCounterPoint = CCUHsApi.getInstance()
                                                                        .readEntity("tuner and default and dab and " +
                                                                                    "stageUp and timer and counter");
        if (stageUpTimerCounterPoint.isEmpty()) {
            Point stageUpTimerCounter = new Point.Builder().setDisplayName(equipDis + "-DAB-" + "stageUpTimerCounter")
                                                                .setSiteRef(siteRef)
                                                                .setEquipRef(equipRef)
                                                                .setHisInterpolate("cov")
                                                                .addMarker("tuner").addMarker("dab")
                                                                .addMarker("default").addMarker("writable").addMarker("his")
                                                                .addMarker("stageUp")
                                                                .addMarker("timer").addMarker("counter").addMarker("sp")
                                                                .setMinVal("0")
                                                                .setMaxVal("30")
                                                                .setIncrementVal("1")
                                                                .setUnit("m")
                                                                .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                                .setTz(tz)
                                                                .build();
            String stageUpTimerCounterId = hayStack.addPoint(stageUpTimerCounter);
            hayStack.writePointForCcuUser(stageUpTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                DEFAULT_STAGE_UP_TIMER_COUNTER, 0);
            hayStack.writeHisValById(stageUpTimerCounterId, DEFAULT_STAGE_UP_TIMER_COUNTER);
        }

        HashMap<Object, Object> stageDownTimerCounterPoint = CCUHsApi.getInstance()
                                                                   .readEntity("tuner and dab and default and " +
                                                                               "stageDown and timer and counter");
        if (stageDownTimerCounterPoint.isEmpty()) {
            Point stageDownTimerCounter = new Point.Builder().setDisplayName(equipDis + "-DAB-" +
                                                                             "stageDownTimerCounter")
                                                           .setSiteRef(siteRef)
                                                           .setEquipRef(equipRef)
                                                           .setHisInterpolate("cov")
                                                           .addMarker("tuner").addMarker("dab")
                                                           .addMarker("default").addMarker("writable").addMarker("his")
                                                           .addMarker("stageDown")
                                                           .addMarker("timer").addMarker("counter").addMarker("sp")
                                                           .setMinVal("0")
                                                           .setMaxVal("30")
                                                           .setIncrementVal("1")
                                                           .setUnit("m")
                                                           .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                           .setTz(tz)
                                                           .build();
            String stageDownTimerCounterId = hayStack.addPoint(stageDownTimerCounter);
            hayStack.writePointForCcuUser(stageDownTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                DEFAULT_STAGE_DOWN_TIMER_COUNTER, 0);
            hayStack.writeHisValById(stageDownTimerCounterId, DEFAULT_STAGE_DOWN_TIMER_COUNTER);
        }
    }

    private void updateVavBuildingTuners() {

        HashMap<Object, Object> fanControlOnFixedTimeDelayPoint = CCUHsApi.getInstance()
                                                                          .read("tuner and default and fan and " +
                                                                                "control and time and delay");

        if (fanControlOnFixedTimeDelayPoint.isEmpty()) {
            Point fanControlOnFixedTimeDelay  = new Point.Builder()
                                                    .setDisplayName(equipDis + "-VAV-"+"fanControlOnFixedTimeDelay ")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipRef)
                                                    .setHisInterpolate("cov")
                                                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his")
                                                    .addMarker("fan").addMarker("control").addMarker("time").addMarker("delay").addMarker("sp")
                                                    .setMinVal("0")
                                                    .setMaxVal("10")
                                                    .setIncrementVal("1")
                                                    .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                    .setUnit("m")
                                                    .setTz(tz)
                                                    .build();
            String fanControlOnFixedTimeDelayId = CCUHsApi.getInstance().addPoint(fanControlOnFixedTimeDelay);
            CCUHsApi.getInstance().writeDefaultValById(fanControlOnFixedTimeDelayId, 1.0);
            CCUHsApi.getInstance().writeHisValById(fanControlOnFixedTimeDelayId, 1.0);
        }

        HashMap<Object, Object> stageUpTimerCounterPoint = CCUHsApi.getInstance()
                                                                   .readEntity("tuner and vav and default and stageUp" +
                                                                               " and timer and counter");
        if (stageUpTimerCounterPoint.isEmpty()) {
            Point stageUpTimerCounter = new Point.Builder().setDisplayName(equipDis + "-VAV-" + "stageUpTimerCounter")
                                                           .setSiteRef(siteRef)
                                                           .setEquipRef(equipRef)
                                                           .setHisInterpolate("cov")
                                                           .addMarker("tuner").addMarker("vav")
                                                           .addMarker("default").addMarker("writable").addMarker("his")
                                                           .addMarker("stageUp")
                                                           .addMarker("timer").addMarker("counter").addMarker("sp")
                                                           .setMinVal("0")
                                                           .setMaxVal("30")
                                                           .setIncrementVal("1")
                                                           .setUnit("m")
                                                           .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                           .setTz(tz)
                                                           .build();
            String stageUpTimerCounterId = hayStack.addPoint(stageUpTimerCounter);
            hayStack.writePointForCcuUser(stageUpTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                DEFAULT_STAGE_UP_TIMER_COUNTER, 0);
            hayStack.writeHisValById(stageUpTimerCounterId, DEFAULT_STAGE_UP_TIMER_COUNTER);
        }

        HashMap<Object, Object> stageDownTimerCounterPoint = CCUHsApi.getInstance()
                                                                 .readEntity("tuner and vav and default and stageDown" +
                                                                             " and timer and counter");
        if (stageDownTimerCounterPoint.isEmpty()) {
            Point stageDownTimerCounter = new Point.Builder().setDisplayName(equipDis + "-VAV-" + "stageDownTimerCounter")
                                                             .setSiteRef(siteRef)
                                                             .setEquipRef(equipRef)
                                                             .setHisInterpolate("cov")
                                                             .addMarker("tuner").addMarker("vav")
                                                             .addMarker("default").addMarker("writable").addMarker("his")
                                                             .addMarker("stageDown")
                                                             .addMarker("timer").addMarker("counter").addMarker("sp")
                                                             .setMinVal("0")
                                                             .setMaxVal("30")
                                                             .setIncrementVal("1")
                                                             .setUnit("m")
                                                             .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                             .setTz(tz)
                                                             .build();
            String stageDownTimerCounterId = hayStack.addPoint(stageDownTimerCounter);
            hayStack.writePointForCcuUser(stageDownTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                DEFAULT_STAGE_DOWN_TIMER_COUNTER, 0);
            hayStack.writeHisValById(stageDownTimerCounterId, DEFAULT_STAGE_DOWN_TIMER_COUNTER);
        }
    }
}
