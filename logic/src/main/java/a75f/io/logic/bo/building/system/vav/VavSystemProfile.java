package a75f.io.logic.bo.building.system.vav;

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_VAV_MODE_CHANGEOVER_HYSTERESIS;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.SystemState;
import a75f.io.logic.tuners.SystemTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 1/10/19.
 */

public abstract class VavSystemProfile extends SystemProfile
{
    
    public void addSystemLoopOpPoints(String equipRef)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        addSystemLoopOpPoint("cooling", siteRef, equipRef, equipDis, tz, BacnetIdKt.COOLINGLOOPOUTPUTID, BacnetUtilKt.ANALOG_VALUE);
        addSystemLoopOpPoint("heating", siteRef, equipRef, equipDis, tz
                , BacnetIdKt.HEATINGLOOPOUTPUTID, BacnetUtilKt.ANALOG_VALUE);
        addSystemLoopOpPoint("fan", siteRef, equipRef, equipDis, tz
                , BacnetIdKt.FANLOOPOUTPUTID, BacnetUtilKt.ANALOG_VALUE);
        addSystemLoopOpPoint("co2", siteRef, equipRef, equipDis, tz
                , BacnetIdKt.CO2LOOPOUTPUTID, BacnetUtilKt.ANALOG_VALUE);
        addRTUSystemPoints(siteRef, equipRef, equipDis, tz);
        addVavSystemPoints(siteRef, equipRef, equipDis, tz);
        addTrTargetPoints(siteRef,equipRef,equipDis,tz);
    }
    
    private void addSystemLoopOpPoint(String loop, String siteRef, String equipref, String equipDis, String tz,
                                      int bacnetId,String bacnetType)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point relay1Op = new Point.Builder().setDisplayName(equipDis + "-" + loop + "LoopOutput")
                .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system")
                .addMarker(loop).addMarker("loop").addMarker("output").addMarker("his")
                .setBacnetType(bacnetType).setBacnetId(bacnetId)
                .addMarker("writable").addMarker("sp").setUnit("%").setTz(tz).build();
        String loopOPPointId = hayStack.addPoint(relay1Op);
        hayStack.writeDefaultValById(loopOPPointId, 0.0);
    }
    
    private void addVavSystemPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point weightedAverageCoolingLoadMA = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageCoolingLoadMA").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("moving").addMarker("average").addMarker("cooling").addMarker("load").addMarker("his").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(weightedAverageCoolingLoadMA);
        Point weightedAverageHeatingLoadMA = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageHeatingLoadMA").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("moving").addMarker("average").addMarker("heating").addMarker("load").addMarker("his").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(weightedAverageHeatingLoadMA);
    }
    
    public void setSystemPoint(String tags, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and his and " + tags, val);
    }
    
    public void setSystemLoopOp(String loop, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and loop and output and his and not purge and " + loop, val);
    }
    
    public void addVavSystemTuners(String equipref)
    {
        addSystemTuners();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();

        Point targetCumulativeDamper = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-targetCumulativeDamper").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp").setUnit("%")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setTz(tz).build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        TunerUtil.copyDefaultBuildingTunerVal(targetCumulativeDamperId, DomainName.vavTargetCumulativeDamper, hayStack);
        
        Point analogFanSpeedMultiplier = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "analogFanSpeedMultiplier").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                .setMinVal("0.1").setMaxVal("3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setTz(tz).build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        TunerUtil.copyDefaultBuildingTunerVal(analogFanSpeedMultiplierId, DomainName.vavAnalogFanSpeedMultiplier, hayStack);

        Point humidityHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "humidityHysteresis").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setTz(tz).build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        TunerUtil.copyDefaultBuildingTunerVal(humidityHysteresisId, DomainName.vavHumidityHysteresis, hayStack);
        
        Point relayDeactivationHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "relayDeactivationHysteresis").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("0.5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setTz(tz).build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        TunerUtil.copyDefaultBuildingTunerVal(relayDeactivationHysteresisId, DomainName.vavRelayDeactivationHysteresis, hayStack);
        
        addNewTunerPoints(equipref);
        SystemTuners.addPITuners(equipref, TunerConstants.VAV_TUNER_GROUP, Tags.VAV, CCUHsApi.getInstance());
    }
    
    protected void addUserIntentPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();

        Point desiredCI = new Point.Builder().setDisplayName(equipDis + "-" + "desiredCI").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("ci").addMarker("desired").addMarker("sp").addMarker("his").setTz(tz).build();
        String desiredCIId = CCUHsApi.getInstance().addPoint(desiredCI);
        CCUHsApi.getInstance().writePointForCcuUser(desiredCIId, TunerConstants.UI_DEFAULT_VAL_LEVEL, TunerConstants.SYSTEM_DEFAULT_CI, 0);
        CCUHsApi.getInstance().writeHisValById(desiredCIId, TunerConstants.SYSTEM_DEFAULT_CI);
        Point systemState = new Point.Builder().setDisplayName(equipDis + "-" + "conditioningMode").
                setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system")
                .addMarker("userIntent").addMarker("writable").addMarker("conditioning").addMarker("mode")
                .addMarker("sp").addMarker("his").setEnums("off,auto,coolonly,heatonly").setTz(tz)
                .setBacnetId(BacnetIdKt.CONDITIONINGMODEID).setBacnetType(BacnetUtilKt.MULTI_STATE_VALUE).build();
        String systemStateId = CCUHsApi.getInstance().addPoint(systemState);
        CCUHsApi.getInstance().writePointForCcuUser(systemStateId, TunerConstants.UI_DEFAULT_VAL_LEVEL, (double) SystemState.OFF.ordinal(), 0);
        CCUHsApi.getInstance().writeHisValById(systemStateId, (double) SystemState.OFF.ordinal());
        Point targetMaxInsideHumidty = new Point.Builder().setDisplayName(equipDis + "-" + "targetMaxInsideHumidty").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("max").addMarker("his").setUnit("%").setTz(tz).addMarker("inside").addMarker("humidity").addMarker("sp").build();
        String targetMaxInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMaxInsideHumidty);
        CCUHsApi.getInstance().writePointForCcuUser(targetMaxInsideHumidtyId, TunerConstants.UI_DEFAULT_VAL_LEVEL, TunerConstants.TARGET_MAX_INSIDE_HUMIDITY, 0);
        CCUHsApi.getInstance().writeHisValById(targetMaxInsideHumidtyId, TunerConstants.TARGET_MAX_INSIDE_HUMIDITY);
        Point targetMinInsideHumidty = new Point.Builder().setDisplayName(equipDis + "-" + "targetMinInsideHumidty").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("min").addMarker("inside").addMarker("humidity").addMarker("sp").addMarker("his").setUnit("%").setTz(tz).build();
        String targetMinInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMinInsideHumidty);
        CCUHsApi.getInstance().writePointForCcuUser(targetMinInsideHumidtyId, TunerConstants.UI_DEFAULT_VAL_LEVEL, TunerConstants.TARGET_MIN_INSIDE_HUMIDITY, 0);
        CCUHsApi.getInstance().writeHisValById(targetMinInsideHumidtyId, TunerConstants.TARGET_MIN_INSIDE_HUMIDITY);
        Point compensateHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "compensateHumidity").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("compensate").addMarker("humidity").setEnums("false,true").setTz(tz).build();
        String compensateHumidityId = CCUHsApi.getInstance().addPoint(compensateHumidity);
        CCUHsApi.getInstance().writePointForCcuUser(compensateHumidityId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
        CCUHsApi.getInstance().writeHisValById(compensateHumidityId, 0.0);

    }
    
    public void addNewTunerPoints(String equipRef) {
        CcuLog.d(L.TAG_CCU_SYSTEM, "VAV : addNewTunerPoints");
        addStageUpTimerCounterTuner(equipRef);
        addStageDownTimerCounterTuner(equipRef);
        addModeChangeHysteresisChangeOverTuner(equipRef);
    }
    
    private void addStageUpTimerCounterTuner(String equipRef) {
        HashMap<Object, Object> stageUpTimerCounterPoint = CCUHsApi.getInstance()
                                                                   .readEntity("tuner and system and vav and " +
                                                                               "stageUp and timer and counter");
        
        if (stageUpTimerCounterPoint.isEmpty()) {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            HashMap siteMap = hayStack.read(Tags.SITE);
            String siteRef = (String) siteMap.get(Tags.ID);
            String tz = siteMap.get("tz").toString();
            Point stageUpTimerCounter = new Point.Builder()
                                            .setDisplayName(HSUtil.getDis(equipRef)+"-stageUpTimerCounter")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .setHisInterpolate("cov")
                                            .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                            .addMarker("stageUp").addMarker("timer").addMarker("counter")
                                            .addMarker("sp").addMarker("system")
                                            .setMinVal("0").setMaxVal("60").setIncrementVal("1")
                                            .setUnit("m")
                                            .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setTz(tz)
                                            .build();
            String stageUpTimerCounterId = hayStack.addPoint(stageUpTimerCounter);

            TunerUtil.copyDefaultBuildingTunerVal(stageUpTimerCounterId, DomainName.vavStageUpTimerCounter, hayStack);
        }
    }
    
    private void addStageDownTimerCounterTuner(String equipRef) {
        HashMap<Object, Object> stageDownTimerCounterPoint = CCUHsApi.getInstance()
                                                                     .readEntity("tuner and system and vav and " +
                                                                                 "stageDown and timer and counter");
        
        if (stageDownTimerCounterPoint.isEmpty()) {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            HashMap siteMap = hayStack.read(Tags.SITE);
            String siteRef = (String) siteMap.get(Tags.ID);
            String tz = siteMap.get("tz").toString();
            Point stageDownTimerCounter = new Point.Builder()
                                              .setDisplayName(HSUtil.getDis(equipRef)+"-stageDownTimerCounter")
                                              .setSiteRef(siteRef)
                                              .setEquipRef(equipRef)
                                              .setHisInterpolate("cov")
                                              .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                              .addMarker("stageDown").addMarker("timer").addMarker("counter")
                                              .addMarker("sp").addMarker("system")
                                              .setMinVal("0").setMaxVal("60").setIncrementVal("1")
                                              .setUnit("m")
                                              .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                              .setTz(tz)
                                              .build();
            String stageDownTimerCounterId = hayStack.addPoint(stageDownTimerCounter);
            TunerUtil.copyDefaultBuildingTunerVal(stageDownTimerCounterId, DomainName.vavStageDownTimerCounter, hayStack);
        }
    }

    private void addModeChangeHysteresisChangeOverTuner(String equipRef) {
        HashMap<Object, Object> modeChangeOverHysteresisPoint = CCUHsApi.getInstance()
                .readEntity("tuner and system and vav and mode and " +
                        "changeover and hysteresis");

        if (modeChangeOverHysteresisPoint.isEmpty()) {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            HashMap siteMap = hayStack.read(Tags.SITE);
            String siteRef = (String) siteMap.get(Tags.ID);
            String tz = siteMap.get("tz").toString();
            Point modeChangeoverHysteresis = new Point.Builder()
                    .setDisplayName(HSUtil.getDis(equipRef)+"-modeChangeoverHysteresis")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef)
                    .setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("cur")
                    .addMarker("mode").addMarker("changeover").addMarker("hysteresis").addMarker("sp").addMarker("system")
                    .setMinVal("0").setMaxVal("5").setIncrementVal("0.5")
                    .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                    .setTz(tz)
                    .build();
            String modeChangeoverHysteresisId = hayStack.addPoint(modeChangeoverHysteresis);

            HashMap defaultModeChangeoverHysteresisPoint = hayStack.read("point and tuner and default and mode and " +
                    "changeover and hysteresis and vav");

            if (defaultModeChangeoverHysteresisPoint.isEmpty()) {
                hayStack.pointWriteForCcuUser(HRef.copy(modeChangeoverHysteresisId), TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                        HNum.make(DEFAULT_VAV_MODE_CHANGEOVER_HYSTERESIS), HNum.make(0));
            } else {
                ArrayList<HashMap> modeChangeoverHysteresisArr =
                        hayStack.readPoint(defaultModeChangeoverHysteresisPoint.get("id").toString());
                for (HashMap valMap : modeChangeoverHysteresisArr) {
                    if (valMap.get("val") != null) {
                        hayStack.pointWrite(HRef.copy(modeChangeoverHysteresisId), (int) Double.parseDouble(valMap.get("level").toString()),
                                valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                    }
                }
            }
            hayStack.writeHisValById(modeChangeoverHysteresisId, HSUtil.getPriorityVal(modeChangeoverHysteresisId));
        }
    }

    public double getUserIntentVal(String tags)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userIntent and " + tags);
        return HSUtil.getPriorityVal(cdb.get("id").toString());
    }
    
    public void setUserIntentVal(String tags, double val)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userIntent and " + tags);
        String id = cdb.get("id").toString();
        if (id == null || id == "")
        {
            throw new IllegalArgumentException();
        }
        hayStack.writePointForCcuUser(id, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, val, 0);
    }
    
    public void addTrTargetPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point sat = new Point.Builder().setDisplayName(equipDis + "-" + "satTRSp").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tr").addMarker("sat").addMarker("target").addMarker("his").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(sat);
        Point co2 = new Point.Builder().setDisplayName(equipDis + "-" + "co2TRSp").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tr").addMarker("co2").addMarker("target").addMarker("his").addMarker("sp").setUnit("\u00B0ppm").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(co2);
        Point sp = new Point.Builder().setDisplayName(equipDis + "-" + "staticPressureTRSp").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tr").addMarker("staticPressure").addMarker("target").addMarker("his").addMarker("sp").setUnit("\u00B0inH₂O").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(sp);
    }
    
    public void setTrTargetVals()
    {
        if(trSystem == null) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " TRSystem not initialized , Skip trSPUpdate");
            return;
        }
        CCUHsApi.getInstance().writeHisValByQuery("point and system and tr and target and his and sat", trSystem.satTRProcessor.getSetPoint());
        CCUHsApi.getInstance().writeHisValByQuery("point and system and tr and target and his and co2", trSystem.co2TRProcessor.getSetPoint());
        CCUHsApi.getInstance().writeHisValByQuery("point and system and tr and target and his and staticPressure", trSystem.spTRProcessor.getSetPoint());
    }
    
    @Override
    public double getCo2LoopOp() {
        return systemCo2LoopOp;
    }
    
    @Override
    public void reset() {
        getSystemController().reset();
    }
    
    /**
     * Returns true if any of the VAV zones has reheat ON.
     * @return
     */
    public boolean isReheatActive(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> reheatPoints = hayStack
                                                              .readAllEntities("point and vav and reheat and cmd");
        for (HashMap<Object, Object> point : reheatPoints) {
            if (point.isEmpty()) {
                continue;
            }
            double reheatPos = hayStack.readHisValById(point.get("id").toString());
            if (reheatPos > 0.01) {
                CcuLog.i(L.TAG_CCU_SYSTEM,"Reheat Active and requires AHU Fan");
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check whether system is operation without only TI/OTN zones.
     * @param hayStack
     * @return
     */
    protected boolean isSingleZoneTIMode(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> vavEquips = hayStack.readAllEntities("equip and zone and vav");
        if (!vavEquips.isEmpty()) {
            return false;
        }
        
        ArrayList<HashMap<Object, Object>> tiEquips = CCUHsApi
                                                          .getInstance()
                                                          .readAllEntities("(equip and zone and ti) or" +
                                                                           "(equip and zone and otn)"
                                                          );
        if (!tiEquips.isEmpty()) {
            return true;
        }
        return false;
    }
    
    protected double getSingleZoneFanLoopOp(double fanSpeedMultiplier) {
        VavSystemController systemController = (VavSystemController) getSystemController();
        
        double fanLoopOp = 0;
        if (systemController.getSystemState() == COOLING ) {
            fanLoopOp = systemController.getCoolingSignal();
        } else if (systemController.getSystemState() == HEATING) {
            fanLoopOp = systemController.getHeatingSignal() * fanSpeedMultiplier;
        }
        return fanLoopOp;
    }

    public void setPendingTunerChange() {
        VavSystemController systemController = (VavSystemController) getSystemController();
        systemController.setPendingTunerChange();
    }

    public void refreshTRTuners() {
        /*
        We do this lengthy series of set() operations instead of creating a new T&R object.

        If a new object was created:
        - previous values of R from zones would be lost
        - the T&R algo would run once with R=0 before the zone requests are consumed again next minute
         */
        trSystem.satTRResponse.setSP0(((VavTRSystem)trSystem).getSatTRTunerVal("spinit"));
        trSystem.satTRResponse.setSPmin(((VavTRSystem)trSystem).getSatTRTunerVal("spmin"));
        trSystem.satTRResponse.setSPmax(((VavTRSystem)trSystem).getSatTRTunerVal("spmax"));
        trSystem.satTRResponse.setSPtrim(((VavTRSystem)trSystem).getSatTRTunerVal("sptrim"));
        trSystem.satTRResponse.setSPres(((VavTRSystem)trSystem).getSatTRTunerVal("spres"));
        trSystem.satTRResponse.setSPresmax(((VavTRSystem)trSystem).getSatTRTunerVal("spresmax"));
        trSystem.satTRResponse.setI((int)((VavTRSystem)trSystem).getSatTRTunerVal("ignoreRequest"));
        trSystem.satTRResponse.setT((int)((VavTRSystem)trSystem).getSatTRTunerVal("timeInterval"));
        trSystem.satTRResponse.setTd((int)((VavTRSystem)trSystem).getSatTRTunerVal("timeDelay"));

        trSystem.co2TRResponse.setSP0(((VavTRSystem)trSystem).getCO2TRTunerVal("spinit"));
        trSystem.co2TRResponse.setSPmin(((VavTRSystem)trSystem).getCO2TRTunerVal("spmin"));
        trSystem.co2TRResponse.setSPmax(((VavTRSystem)trSystem).getCO2TRTunerVal("spmax"));
        trSystem.co2TRResponse.setSPtrim(((VavTRSystem)trSystem).getCO2TRTunerVal("sptrim"));
        trSystem.co2TRResponse.setSPres(((VavTRSystem)trSystem).getCO2TRTunerVal("spres"));
        trSystem.co2TRResponse.setSPresmax(((VavTRSystem)trSystem).getCO2TRTunerVal("spresmax"));
        trSystem.co2TRResponse.setI((int)((VavTRSystem)trSystem).getCO2TRTunerVal("ignoreRequest"));
        trSystem.co2TRResponse.setT((int)((VavTRSystem)trSystem).getCO2TRTunerVal("timeInterval"));
        trSystem.co2TRResponse.setTd((int)((VavTRSystem)trSystem).getCO2TRTunerVal("timeDelay"));

        trSystem.spTRResponse.setSP0(((VavTRSystem)trSystem).getSpTRTunerVal("spinit"));
        trSystem.spTRResponse.setSPmin(((VavTRSystem)trSystem).getSpTRTunerVal("spmin"));
        trSystem.spTRResponse.setSPmax(((VavTRSystem)trSystem).getSpTRTunerVal("spmax"));
        trSystem.spTRResponse.setSPtrim(((VavTRSystem)trSystem).getSpTRTunerVal("sptrim"));
        trSystem.spTRResponse.setSPres(((VavTRSystem)trSystem).getSpTRTunerVal("spres"));
        trSystem.spTRResponse.setSPresmax(((VavTRSystem)trSystem).getSpTRTunerVal("spresmax"));
        trSystem.spTRResponse.setI((int)((VavTRSystem)trSystem).getSpTRTunerVal("ignoreRequest"));
        trSystem.spTRResponse.setT((int)((VavTRSystem)trSystem).getSpTRTunerVal("timeInterval"));
        trSystem.spTRResponse.setTd((int)((VavTRSystem)trSystem).getSpTRTunerVal("timeDelay"));
    }
}
