package a75f.io.logic.bo.building.system.dab;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.DomainNameKt;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.SystemState;
import a75f.io.logic.tuners.DcwbTuners;
import a75f.io.logic.tuners.SystemTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.tuners.TunerConstants.DEFAULT_MODE_CHANGEOVER_HYSTERESIS;
import static a75f.io.logic.tuners.TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL;

public abstract class DabSystemProfile extends SystemProfile
{
    
    public void addSystemLoopOpPoints(String equipRef)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        addSystemLoopOpPoint("cooling", siteRef, equipRef, equipDis, tz, BacnetIdKt.COOLINGLOOPOUTPUTID);
        addSystemLoopOpPoint("heating", siteRef, equipRef, equipDis, tz,BacnetIdKt.HEATINGLOOPOUTPUTID);
        addSystemLoopOpPoint("fan", siteRef, equipRef, equipDis, tz ,BacnetIdKt.FANLOOPOUTPUTID);
        addSystemLoopOpPoint("co2", siteRef, equipRef, equipDis, tz,BacnetIdKt.CO2LOOPOUTPUTID);
        addRTUSystemPoints(siteRef, equipRef, equipDis, tz);
        addDabSystemPoints(siteRef, equipRef, equipDis, tz);
    }
    
    private void addSystemLoopOpPoint(String loop, String siteRef, String equipref, String equipDis, String tz,int bacnetId)
    {
            CCUHsApi hayStack = CCUHsApi.getInstance();
        Point relay1Op = new Point.Builder().setDisplayName(equipDis + "-" + loop + "LoopOutput")
                    .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system")
                    .addMarker(loop).addMarker("loop").addMarker("output").addMarker("his").addMarker("sp")
                    .addMarker("writable").setUnit("%").setTz(tz).setBacnetId(bacnetId).setBacnetType(BacnetUtilKt.ANALOG_VALUE).build();
            String loopOPPointId = hayStack.addPoint(relay1Op);
            hayStack.writeDefaultValById(loopOPPointId,0.0);
    }
    
    private void addDabSystemPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point weightedAverageLoadMA = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageChangeOverLoadMA ").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("weighted").addMarker("average").addMarker("moving").addMarker("load").addMarker("his").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().writeHisValById(CCUHsApi.getInstance().addPoint(weightedAverageLoadMA), 0.0);
        Point weightedAverageCoolingLoadPostML = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageCoolingLoadPostML").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("weighted").addMarker("average").addMarker("cooling").addMarker("load").addMarker("his").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().writeHisValById(CCUHsApi.getInstance().addPoint(weightedAverageCoolingLoadPostML), 0.0);
        Point weightedAverageHeatingLoadPostML = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageHeatingLoadPostML").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("weighted").addMarker("average").addMarker("heating").addMarker("load").addMarker("his").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().writeHisValById(CCUHsApi.getInstance().addPoint(weightedAverageHeatingLoadPostML), 0.0);
    }
    
    public void setSystemPoint(String tags, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and his and " + tags, val);
    }
    
    public void setSystemLoopOp(String loop, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and loop and output and his and " + loop, val);
    }
    
    public void addDabSystemTuners(String equipref)
    {
        addSystemTuners();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();
        
        Point targetCumulativeDamper = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "targetCumulativeDamper").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp")
                .setUnit("%").setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP).setTz(tz).build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        TunerUtil.copyDefaultBuildingTunerVal(targetCumulativeDamperId, DomainNameKt.dabTargetCumulativeDamper, hayStack);
        
        Point analogFanSpeedMultiplier = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "analogFanSpeedMultiplier").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                .setMinVal("0.1").setMaxVal("3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP).setTz(tz).build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        TunerUtil.copyDefaultBuildingTunerVal(analogFanSpeedMultiplierId, DomainNameKt.dabAnalogFanSpeedMultiplier, hayStack);
        
        Point humidityHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "humidityHysteresis").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                .setUnit("%").setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP).setTz(tz).build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        TunerUtil.copyDefaultBuildingTunerVal(humidityHysteresisId, DomainNameKt.dabHumidityHysteresis, hayStack);
        
        Point relayDeactivationHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "relayDeactivationHysteresis").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                .setUnit("%").setMinVal("0").setMaxVal("60").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP).setTz(tz).build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        TunerUtil.copyDefaultBuildingTunerVal(relayDeactivationHysteresisId, DomainNameKt.dabRelayDeactivationHysteresis, hayStack);
    
        Point rebalanceHoldTime = new Point.Builder()
                .setDisplayName(HSUtil.getDis(equipref)+"-DAB-"+"rebalanceHoldTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                .addMarker("rebalance").addMarker("hold").addMarker("time").addMarker("sp").addMarker("system")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String rebalanceHoldTimeId = hayStack.addPoint(rebalanceHoldTime);
        TunerUtil.copyDefaultBuildingTunerVal(rebalanceHoldTimeId, DomainNameKt.dabRebalanceHoldTime, hayStack);
    
        
        addNewTunerPoints(equipref);
    
        SystemTuners.addPITuners(equipref, TunerConstants.DAB_TUNER_GROUP, Tags.DAB, CCUHsApi.getInstance());
    
        DcwbTuners.addEquipDcwbTuners(hayStack, siteRef, HSUtil.getDis(equipref), equipref, tz);
    }
    
    public void addNewTunerPoints(String equipRef) {
        CcuLog.d(L.TAG_CCU_SYSTEM," DabSystemProfile addNewTunerPoints ");
        addModeChangeHysteresisChangeOverTuner(equipRef);
        addStageUpTimerCounterTuner(equipRef);
        addStageDownTimerCounterTuner(equipRef);
        addEffectiveSatConditioningPoint(equipRef, CCUHsApi.getInstance());
    }
    
    private void addModeChangeHysteresisChangeOverTuner(String equipRef) {
        HashMap<Object, Object> modeChangeOverHysteresisPoint = CCUHsApi.getInstance()
                                                                        .readEntity("tuner and system and dab and mode and " +
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
                                                 .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                                 .addMarker("mode").addMarker("changeover").addMarker("hysteresis").addMarker("sp").addMarker("system")
                                                 .setMinVal("0").setMaxVal("5").setIncrementVal("0.5")
                                                 .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                 .setTz(tz)
                                                 .build();
            String modeChangeoverHysteresisId = hayStack.addPoint(modeChangeoverHysteresis);

            TunerUtil.copyDefaultBuildingTunerVal(modeChangeoverHysteresisId, DomainNameKt.dabModeChangeoverHysteresis, hayStack);
        }
    }
    
    private void addStageUpTimerCounterTuner(String equipRef) {
        HashMap<Object, Object> stageUpTimerCounterPoint = CCUHsApi.getInstance()
                                                                        .readEntity("tuner and system and dab and " +
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
                                                 .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                                 .addMarker("stageUp").addMarker("timer").addMarker("counter")
                                                 .addMarker("sp").addMarker("system")
                                                 .setMinVal("0").setMaxVal("60").setIncrementVal("1")
                                                 .setUnit("m")
                                                 .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                 .setTz(tz)
                                                 .build();
            String stageUpTimerCounterId = hayStack.addPoint(stageUpTimerCounter);
            TunerUtil.copyDefaultBuildingTunerVal(stageUpTimerCounterId, DomainNameKt.dabStageUpTimerCounter, hayStack);
        }
    }
    
    private void addStageDownTimerCounterTuner(String equipRef) {
        HashMap<Object, Object> stageDownTimerCounterPoint = CCUHsApi.getInstance()
                                                                   .readEntity("tuner and system and dab and " +
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
                                            .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                            .addMarker("stageDown").addMarker("timer").addMarker("counter")
                                            .addMarker("sp").addMarker("system")
                                            .setMinVal("0").setMaxVal("60").setIncrementVal("1")
                                            .setUnit("m")
                                            .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                            .setTz(tz)
                                            .build();
            String stageDownTimerCounterId = hayStack.addPoint(stageDownTimerCounter);
            TunerUtil.copyDefaultBuildingTunerVal(stageDownTimerCounterId, DomainNameKt.dabStageDownTimerCounter, hayStack);
        }
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
        Point systemState = new Point.Builder().setDisplayName(equipDis + "-" + "conditioningMode").setBacnetId(BacnetIdKt.CONDITIONINGMODEID).setBacnetType(BacnetUtilKt.MULTI_STATE_VALUE)
                .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("conditioning").addMarker("mode").addMarker("sp").addMarker("his").setEnums("off,auto,coolonly,heatonly").setTz(tz).build();
        String systemStateId = CCUHsApi.getInstance().addPoint(systemState);
        CCUHsApi.getInstance().writePointForCcuUser(systemStateId, TunerConstants.UI_DEFAULT_VAL_LEVEL, (double) SystemState.OFF.ordinal(), 0);
        CCUHsApi.getInstance().writeHisValById(systemStateId, (double) SystemState.OFF.ordinal());
        Point targetMaxInsideHumidty = new Point.Builder().setDisplayName(equipDis + "-" + "targetMaxInsideHumidty").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("max").addMarker("his").setUnit("%").setTz(tz).addMarker("inside").addMarker("humidity").addMarker("sp").build();
        String targetMaxInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMaxInsideHumidty);
        CCUHsApi.getInstance().writePointForCcuUser(targetMaxInsideHumidtyId, TunerConstants.UI_DEFAULT_VAL_LEVEL, TunerConstants.TARGET_MAX_INSIDE_HUMIDITY, 0);
        CCUHsApi.getInstance().writeHisValById(targetMaxInsideHumidtyId, TunerConstants.TARGET_MAX_INSIDE_HUMIDITY);
        Point targetMinInsideHumidty = new Point.Builder().setDisplayName(equipDis + "-" + "targetMinInsideHumidty").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").setHisInterpolate("cov").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("min").addMarker("inside").addMarker("humidity").setUnit("%").addMarker("sp").addMarker("his").setTz(tz).build();
        String targetMinInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMinInsideHumidty);
        CCUHsApi.getInstance().writePointForCcuUser(targetMinInsideHumidtyId, TunerConstants.UI_DEFAULT_VAL_LEVEL, TunerConstants.TARGET_MIN_INSIDE_HUMIDITY, 0);
        CCUHsApi.getInstance().writeHisValById(targetMinInsideHumidtyId, TunerConstants.TARGET_MIN_INSIDE_HUMIDITY);
        Point compensateHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "compensateHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").setHisInterpolate("cov").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("compensate").addMarker("humidity").setEnums("false,true").setTz(tz).build();
        String compensateHumidityId = CCUHsApi.getInstance().addPoint(compensateHumidity);
        CCUHsApi.getInstance().writePointForCcuUser(compensateHumidityId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
        CCUHsApi.getInstance().writeHisValById(compensateHumidityId, 0.0);
        
        Point demandResponseMode = new Point.Builder().setDisplayName(equipDis + "-" + "demandResponseMode").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").setHisInterpolate("cov").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("demand").addMarker("response").setEnums("false,true").setTz(tz).build();
        String demandResponseModeId = CCUHsApi.getInstance().addPoint(demandResponseMode);
        CCUHsApi.getInstance().writePointForCcuUser(demandResponseModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
        CCUHsApi.getInstance().writeHisValById(demandResponseModeId, 0.0);
    }
    
    public double getUserIntentVal(String tags)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userIntent and " + tags);
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null)
                {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
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
        hayStack.writePointForCcuUser(id, SYSTEM_DEFAULT_VAL_LEVEL, val, 0);
    }
    
    @Override
    public double getCo2LoopOp() {
        return DabSystemController.getInstance().getWACo2LoopOp();
    }
    
    @Override
    public void reset() {
        getSystemController().reset();
    }

    private void addEffectiveSatConditioningPoint(String equipRef, CCUHsApi hayStack) {
        if (hayStack.readEntity("system and effective and sat and conditioning").isEmpty()) {
            Site site = hayStack.getSite();
            Point effectiveSatConditioning = new Point.Builder()
                    .setDisplayName(HSUtil.getDis(equipRef) + "-effectiveSatConditioning")
                    .setSiteRef(site.getId())
                    .setEquipRef(equipRef)
                    .setHisInterpolate("cov")
                    .addMarker("system").addMarker("dab").addMarker("his")
                    .addMarker("effective").addMarker("sat").addMarker("conditioning").addMarker("sp")
                    .setEnums("not_available,cooling,heating")
                    .setTz(site.getTz())
                    .build();
            String effectiveSatConditioningId = hayStack.addPoint(effectiveSatConditioning);
            hayStack.writeHisValById(effectiveSatConditioningId, 0.0);
        }
    }
}
