package a75f.io.logic.bo.building.system.dab;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.SystemState;
import a75f.io.logic.tuners.DcwbTuners;
import a75f.io.logic.tuners.SystemTuners;
import a75f.io.logic.tuners.TunerConstants;

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
        addSystemLoopOpPoint("cooling", siteRef, equipRef, equipDis, tz);
        addSystemLoopOpPoint("heating", siteRef, equipRef, equipDis, tz);
        addSystemLoopOpPoint("fan", siteRef, equipRef, equipDis, tz);
        addSystemLoopOpPoint("co2", siteRef, equipRef, equipDis, tz);
        addRTUSystemPoints(siteRef, equipRef, equipDis, tz);
        addDabSystemPoints(siteRef, equipRef, equipDis, tz);
    }
    
    private void addSystemLoopOpPoint(String loop, String siteRef, String equipref, String equipDis, String tz)
    {
        Point relay1Op = new Point.Builder().setDisplayName(equipDis + "-" + loop + "LoopOutput").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker(loop).addMarker("loop").addMarker("output").addMarker("his").addMarker("sp").setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(relay1Op);
    }
    
    private void addDabSystemPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point weightedAverageLoadMA = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageLoadMA ").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("weighted").addMarker("average").addMarker("moving").addMarker("load").addMarker("his").addMarker("sp").setTz(tz).build();
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
        HashMap targetCumulativeDamperP = hayStack.read("point and tuner and default and dab and target and cumulative and damper");
        ArrayList<HashMap> targetCumulativeDamperArr = hayStack.readPoint(targetCumulativeDamperP.get("id").toString());
        for (HashMap valMap : targetCumulativeDamperArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(targetCumulativeDamperId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                //hayStack.writeHisValById(targetCumulativeDamperId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        hayStack.writeHisValById(targetCumulativeDamperId, HSUtil.getPriorityVal(targetCumulativeDamperId));
        
        Point analogFanSpeedMultiplier = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "analogFanSpeedMultiplier").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                .setMinVal("0.1").setMaxVal("3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP).setTz(tz).build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        HashMap analogFanSpeedMultiplierP = hayStack.read("point and tuner and default and dab and analog and fan and speed and multiplier");
        ArrayList<HashMap> analogFanSpeedMultiplierArr = hayStack.readPoint(analogFanSpeedMultiplierP.get("id").toString());
        for (HashMap valMap : analogFanSpeedMultiplierArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(analogFanSpeedMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                //hayStack.writeHisValById(analogFanSpeedMultiplierId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        hayStack.writeHisValById(analogFanSpeedMultiplierId, HSUtil.getPriorityVal(analogFanSpeedMultiplierId));
        
        Point humidityHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "humidityHysteresis").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                .setUnit("%").setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP).setTz(tz).build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        HashMap humidityHysteresisPoint = hayStack.read("point and tuner and default and dab and humidity and hysteresis");
        ArrayList<HashMap> humidityHysteresisArr = hayStack.readPoint(humidityHysteresisPoint.get("id").toString());
        for (HashMap valMap : humidityHysteresisArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(humidityHysteresisId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                //hayStack.writeHisValById(humidityHysteresisId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        hayStack.writeHisValById(humidityHysteresisId, HSUtil.getPriorityVal(humidityHysteresisId));
        
        Point relayDeactivationHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "relayDeactivationHysteresis").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                .setUnit("%").setMinVal("0").setMaxVal("10").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP).setTz(tz).build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        HashMap relayDeactivationHysteresisPoint = hayStack.read("point and tuner and default and dab and relay and deactivation and hysteresis");
        ArrayList<HashMap> relayDeactivationHysteresisArr = hayStack.readPoint(relayDeactivationHysteresisPoint.get("id").toString());
        for (HashMap valMap : relayDeactivationHysteresisArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(relayDeactivationHysteresisId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                //hayStack.writeHisValById(relayDeactivationHysteresisId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        hayStack.writeHisValById(relayDeactivationHysteresisId, HSUtil.getPriorityVal(relayDeactivationHysteresisId));
    
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

        HashMap rebalanceHoldTimePoint = hayStack.read("point and tuner and default and rebalance and hold and time");
        ArrayList<HashMap> rebalanceHoldTimeArr = hayStack.readPoint(rebalanceHoldTimePoint.get("id").toString());
        for (HashMap valMap : rebalanceHoldTimeArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(rebalanceHoldTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(rebalanceHoldTimeId, HSUtil.getPriorityVal(rebalanceHoldTimeId));
    
        
        addNewTunerPoints(equipref);
    
        SystemTuners.addPITuners(equipref, TunerConstants.DAB_TUNER_GROUP, Tags.DAB, CCUHsApi.getInstance());
    
        DcwbTuners.addEquipDcwbTuners(hayStack, siteRef, HSUtil.getDis(equipref), equipref, tz);
    }
    
    public void addNewTunerPoints(String equipRef) {
        CcuLog.d(L.TAG_CCU_SYSTEM," DabSystemProfile addNewTunerPoints ");
        addModeChangeHysteresisChangeOverTuner(equipRef);
        addStageUpTimerCounterTuner(equipRef);
        addStageDownTimerCounterTuner(equipRef);
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
        
            HashMap defaultModeChangeoverHysteresisPoint = hayStack.read("point and tuner and default and mode and " +
                                                                         "changeover and hysteresis");
        
            if (defaultModeChangeoverHysteresisPoint.isEmpty()) {
                hayStack.pointWriteForCcuUser(HRef.copy(modeChangeoverHysteresisId), SYSTEM_DEFAULT_VAL_LEVEL,
                                    HNum.make(DEFAULT_MODE_CHANGEOVER_HYSTERESIS), HNum.make(0));
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
                                                 .setMinVal("0").setMaxVal("5").setIncrementVal("1")
                                                 .setUnit("m")
                                                 .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                 .setTz(tz)
                                                 .build();
            String stageUpTimerCounterId = hayStack.addPoint(stageUpTimerCounter);
            
            HashMap defaultStageUpTimerCounterPoint = hayStack.read("point and tuner and dab and default and " +
                                                                    "stageUp and timer and counter");
    
            ArrayList<HashMap> defaultStageUpTimerCounterPointArr =
                hayStack.readPoint(defaultStageUpTimerCounterPoint.get("id").toString());
            for (HashMap valMap : defaultStageUpTimerCounterPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(stageUpTimerCounterId), (int) Double.parseDouble(valMap.get("level").toString()),
                                        valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(stageUpTimerCounterId, HSUtil.getPriorityVal(stageUpTimerCounterId));
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
                                            .setMinVal("0").setMaxVal("5").setIncrementVal("1")
                                            .setUnit("m")
                                            .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                            .setTz(tz)
                                            .build();
            String stageDownTimerCounterId = hayStack.addPoint(stageDownTimerCounter);
            
            HashMap defaultStageUpTimerCounterPoint = hayStack.read("point and tuner and dab and default and " +
                                                                    "stageDown and timer and counter");
    
            ArrayList<HashMap> defaultStageDownTimerCounterPointArr =
                hayStack.readPoint(defaultStageUpTimerCounterPoint.get("id").toString());
            for (HashMap valMap : defaultStageDownTimerCounterPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(stageDownTimerCounterId), (int) Double.parseDouble(valMap.get("level").toString()),
                                        valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(stageDownTimerCounterId, HSUtil.getPriorityVal(stageDownTimerCounterId));
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
        Point systemState = new Point.Builder().setDisplayName(equipDis + "-" + "conditioningMode").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("conditioning").addMarker("mode").addMarker("sp").addMarker("his").setEnums("off,auto,coolonly,heatonly").setTz(tz).build();
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
}
