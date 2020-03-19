package a75f.io.logic.bo.building.system;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.tr.TRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.building.system.dab.DabSystemProfile;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemProfile;
import a75f.io.logic.tuners.TunerConstants;

/**
 * Created by Yinten isOn 8/15/2017.
 */
public abstract class SystemProfile
{
   
    public Schedule schedule = new Schedule();
    
    public TRSystem trSystem;
    
    public SystemEquip sysEquip;
    private String equipRef = null;
    private String siteRef;
    private String equipDis;
    private String tz;
    private CCUHsApi hayStack;

    public double systemCoolingLoopOp;
    public double systemHeatingLoopOp;
    public double systemFanLoopOp;
    public double systemCo2LoopOp;
    
    public abstract void doSystemControl();
    
    public abstract void addSystemEquip();
    
    public abstract void deleteSystemEquip();
    
    //Is Cooling enabled in System Profile
    public abstract boolean isCoolingAvailable();
    
    public abstract boolean isHeatingAvailable();
    
    //Is Cooling stage/signal ON now
    public abstract boolean isCoolingActive();
    
    public abstract boolean isHeatingActive();
    
    public abstract ProfileType getProfileType();
    
    public abstract String getStatusMessage();
    
    public  int getSystemSAT() {
        return 0;
    }
    
    public  int getSystemCO2() {
        return 0;
    }
    
    public  int getSystemOADamper() {
        return 0;
    }
    
    public double getStaticPressure() {
        return 0;
    }
    
    public String getProfileName() {
        return "";
    }
    
    public int getAnalog1Out() {
        return 0;
    }
    
    public int getAnalog2Out() {
        return 0;
    }
    
    public int getAnalog3Out() {
        return 0;
    }
    
    public int getAnalog4Out() {
        return 0;
    }
    
    public double getCoolingLoopOp()
    {
        return systemCoolingLoopOp;
    }
    public double getHeatingLoopOp()
    {
        return systemHeatingLoopOp;
    }
    public double getFanLoopOp()
    {
        return systemFanLoopOp;
    }
    public double getCo2LoopOp() {
        return systemCo2LoopOp;
    }
    
    public double getCmd(String tags) {
        return CCUHsApi.getInstance().readHisValByQuery(tags+" and cmd and equipRef == \""+getSystemEquipRef()+"\"");
    }
    
    public SystemController getSystemController() {
        if (this instanceof VavSystemProfile) {
            return VavSystemController.getInstance();
        } else if (this instanceof DabSystemProfile) {
            return DabSystemController.getInstance();
        }
        return DefaultSystemController.getInstance();
    }
    
    public SystemController.State getConditioning() {
        return getSystemController().getSystemState();
    }
    
    public double getAverageTemp() {
        return getSystemController().getAverageSystemTemperature();
    }
    
    public double getWeightedAverageCO2() {
        return getSystemController().getSystemCO2WA();
    }
    
    public String getSystemEquipRef() {
        if (equipRef == null)
        {
            HashMap equip = CCUHsApi.getInstance().read("equip and system");
            equipRef = equip.get("id").toString();
            equipDis = equip.get("dis").toString();
        }
        return equipRef;
    }
    
    public void updateAhuRef(String systemEquipId) {
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
        
        for (HashMap m : equips)
        {
            Equip q = new Equip.Builder().setHashMap(m)/*.setAhuRef(systemEquipId)*/.build();
            if(q.getMarkers().contains("dab") || q.getMarkers().contains("vav")|| q.getMarkers().contains("oao"))
                q.setAhuRef(systemEquipId);
            else q.setGatewayRef(systemEquipId);
            CCUHsApi.getInstance().updateEquip(q, q.getId());
        }
        
        CCUHsApi.getInstance().updateCCUahuRef(systemEquipId);
    }
    
    public void addSystemTuners() {
        
        hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = (String) siteMap.get(Tags.ID);
        tz = siteMap.get("tz").toString();
        equipRef = getSystemEquipRef();
    
        Point userLimitSpread = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "userLimitSpread").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("user").addMarker("limit").addMarker("spread").addMarker("sp").addMarker("equipHis")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();

        String userLimitSpreadId = hayStack.addPoint(userLimitSpread);
        HashMap userLimitSpreadPoint = hayStack.read("point and tuner and default and user and limit and spread");
        ArrayList<HashMap> userLimitSpreadPointArr = hayStack.readPoint(userLimitSpreadPoint.get("id").toString());
        for (HashMap valMap : userLimitSpreadPointArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(userLimitSpreadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(userLimitSpreadId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point heatingPreconditioningRate = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "heatingPreconditioningRate").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("heating").addMarker("precon").addMarker("rate").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String heatingPreconditioningRateId = hayStack.addPoint(heatingPreconditioningRate);
        HashMap heatingPreconditioningRatePoint = hayStack.read("point and tuner and default and heating and precon and rate");
        ArrayList<HashMap> heatingPreconditioningRateArr = hayStack.readPoint(heatingPreconditioningRatePoint.get("id").toString());
        for (HashMap valMap : heatingPreconditioningRateArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(heatingPreconditioningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(heatingPreconditioningRateId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        Point coolingPreconditioningRate = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "coolingPreconditioningRate").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("cooling").addMarker("precon").addMarker("rate").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String coolingPreconditioningRateId = hayStack.addPoint(coolingPreconditioningRate);
        HashMap coolingPreconditioningRatePoint = hayStack.read("point and tuner and default and cooling and precon and rate");
        ArrayList<HashMap> coolingPreconditioningRateArr = hayStack.readPoint(coolingPreconditioningRatePoint.get("id").toString());
        for (HashMap valMap : coolingPreconditioningRateArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(coolingPreconditioningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(coolingPreconditioningRateId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point cmTempInfPercentileZonesDead = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "cmTempPercentDeadZonesAllowed").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("zone").addMarker("percent").addMarker("dead").addMarker("influence").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String cmTempInfPercentileZonesDeadId = hayStack.addPoint(cmTempInfPercentileZonesDead);
        HashMap cmTempInfPercentileZonesDeadPoint = hayStack.read("point and tuner and default and percent and dead and influence");
        ArrayList<HashMap> cmTempInfPercentileZonesDeadPointArr = hayStack.readPoint(cmTempInfPercentileZonesDeadPoint.get("id").toString());
        for (HashMap valMap : cmTempInfPercentileZonesDeadPointArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(cmTempInfPercentileZonesDeadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(cmTempInfPercentileZonesDeadId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point airflowSampleWaitTime = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "airflowSampleWaitTime").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp").addMarker("equipHis")
                .setMinVal("1").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz).build();
        String airflowSampleWaitTimeId = hayStack.addPoint(airflowSampleWaitTime);
        HashMap airflowSampleWaitTimePoint = hayStack.read("point and tuner and default and airflow and sample and wait and time");
        ArrayList<HashMap> airflowSampleWaitTimeArr = hayStack.readPoint(airflowSampleWaitTimePoint.get("id").toString());
        for (HashMap valMap : airflowSampleWaitTimeArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(airflowSampleWaitTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(airflowSampleWaitTimeId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage1CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage1CoolingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-120").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage1CoolingAirflowTempLowerOffset);
        HashMap stage1CoolingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage1 and cooling and airflow and temp and lower and offset");
        ArrayList<HashMap> stage1CoolingAirflowTempLowerOffsetArr = hayStack.readPoint(stage1CoolingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage1CoolingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage1CoolingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage1CoolingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage2CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage2CoolingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage2CoolingAirflowTempLowerOffset);
        HashMap stage2CoolingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage2 and cooling and airflow and temp and lower and offset");
        ArrayList<HashMap> stage2CoolingAirflowTempLowerOffsetArr = hayStack.readPoint(stage2CoolingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage2CoolingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage2CoolingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage2CoolingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage3CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage3CoolingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage3CoolingAirflowTempLowerOffset);
        HashMap stage3CoolingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage3 and cooling and airflow and temp and lower and offset");
        ArrayList<HashMap> stage3CoolingAirflowTempLowerOffsetArr = hayStack.readPoint(stage3CoolingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage3CoolingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage3CoolingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage3CoolingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage4CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage4CoolingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage4CoolingAirflowTempLowerOffset);
        HashMap stage4CoolingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage4 and cooling and airflow and temp and lower and offset");
        ArrayList<HashMap> stage4CoolingAirflowTempLowerOffsetArr = hayStack.readPoint(stage4CoolingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage4CoolingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage4CoolingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage4CoolingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage5CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage5CoolingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage5CoolingAirflowTempLowerOffset);
        HashMap stage5CoolingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage5 and cooling and airflow and temp and lower and offset");
        ArrayList<HashMap> stage5CoolingAirflowTempLowerOffsetArr = hayStack.readPoint(stage5CoolingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage5CoolingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage5CoolingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage5CoolingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage1CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage1CoolingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage1CoolingAirflowTempUpperOffset);
        HashMap stage1CoolingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage1 and cooling and airflow and temp and upper and offset");
        ArrayList<HashMap> stage1CoolingAirflowTempUpperOffsetArr = hayStack.readPoint(stage1CoolingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage1CoolingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage1CoolingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage1CoolingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage2CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage2CoolingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage2CoolingAirflowTempUpperOffset);
        HashMap stage2CoolingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage2 and cooling and airflow and temp and upper and offset");
        ArrayList<HashMap> stage2CoolingAirflowTempUpperOffsetArr = hayStack.readPoint(stage2CoolingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage2CoolingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage2CoolingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage2CoolingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage3CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage3CoolingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage3CoolingAirflowTempUpperOffset);
        HashMap stage3CoolingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage3 and cooling and airflow and temp and upper and offset");
        ArrayList<HashMap> stage3CoolingAirflowTempUpperOffsetArr = hayStack.readPoint(stage3CoolingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage3CoolingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage3CoolingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage3CoolingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage4CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage4CoolingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage4CoolingAirflowTempUpperOffset);
        HashMap stage4CoolingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage4 and cooling and airflow and temp and upper and offset");
        ArrayList<HashMap> stage4CoolingAirflowTempUpperOffsetArr = hayStack.readPoint(stage4CoolingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage4CoolingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage4CoolingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage4CoolingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage5CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage5CoolingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage5CoolingAirflowTempUpperOffset);
        HashMap stage5CoolingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage5 and cooling and airflow and temp and upper and offset");
        ArrayList<HashMap> stage5CoolingAirflowTempUpperOffsetArr = hayStack.readPoint(stage5CoolingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage5CoolingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage5CoolingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage5CoolingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage1HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage1HeatingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage1HeatingAirflowTempLowerOffset);
        HashMap stage1HeatingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage1 and heating and airflow and temp and lower and offset");
        ArrayList<HashMap> stage1HeatingAirflowTempLowerOffsetArr = hayStack.readPoint(stage1HeatingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage1HeatingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage1HeatingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage1HeatingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage2HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage2HeatingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage2HeatingAirflowTempLowerOffset);
        HashMap stage2HeatingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage2 and heating and airflow and temp and lower and offset");
        ArrayList<HashMap> stage2HeatingAirflowTempLowerOffsetArr = hayStack.readPoint(stage2HeatingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage2HeatingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage2HeatingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage2HeatingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage3HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage3HeatingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage3HeatingAirflowTempLowerOffset);
        HashMap stage3HeatingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage3 and heating and airflow and temp and lower and offset");
        ArrayList<HashMap> stage3HeatingAirflowTempLowerOffsetArr = hayStack.readPoint(stage3HeatingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage3HeatingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage3HeatingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage3HeatingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage4HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage4HeatingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage4HeatingAirflowTempLowerOffset);
        HashMap stage4HeatingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage4 and heating and airflow and temp and lower and offset");
        ArrayList<HashMap> stage4HeatingAirflowTempLowerOffsetArr = hayStack.readPoint(stage4HeatingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage4HeatingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage4HeatingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage4HeatingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage5HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage5HeatingAirflowTempLowerOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage5HeatingAirflowTempLowerOffset);
        HashMap stage5HeatingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and stage5 and heating and airflow and temp and lower and offset");
        ArrayList<HashMap> stage5HeatingAirflowTempLowerOffsetArr = hayStack.readPoint(stage5HeatingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : stage5HeatingAirflowTempLowerOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage5HeatingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage5HeatingAirflowTempLowerOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage1HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage1HeatingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage1HeatingAirflowTempUpperOffset);
        HashMap stage1HeatingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage1 and heating and airflow and temp and upper and offset");
        ArrayList<HashMap> stage1HeatingAirflowTempUpperOffsetArr = hayStack.readPoint(stage1HeatingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage1HeatingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage1HeatingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage1HeatingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage2HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage2HeatingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage2HeatingAirflowTempUpperOffset);
        HashMap stage2HeatingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage2 and heating and airflow and temp and upper and offset");
        ArrayList<HashMap> stage2HeatingAirflowTempUpperOffsetArr = hayStack.readPoint(stage2HeatingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage2HeatingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage2HeatingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage2HeatingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage3HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage3HeatingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage3HeatingAirflowTempUpperOffset);
        HashMap stage3HeatingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage3 and heating and airflow and temp and upper and offset");
        ArrayList<HashMap> stage3HeatingAirflowTempUpperOffsetArr = hayStack.readPoint(stage3HeatingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage3HeatingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage3HeatingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage3HeatingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage4HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage4HeatingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage4HeatingAirflowTempUpperOffset);
        HashMap stage4HeatingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage4 and heating and airflow and temp and upper and offset");
        ArrayList<HashMap> stage4HeatingAirflowTempUpperOffsetArr = hayStack.readPoint(stage4HeatingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage4HeatingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage4HeatingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage4HeatingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point stage5HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "stage5HeatingAirflowTempUpperOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage5HeatingAirflowTempUpperOffset);
        HashMap stage5HeatingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and stage5 and heating and airflow and temp and upper and offset");
        ArrayList<HashMap> stage5HeatingAirflowTempUpperOffsetArr = hayStack.readPoint(stage5HeatingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : stage5HeatingAirflowTempUpperOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(stage5HeatingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(stage5HeatingAirflowTempUpperOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point clockUpdateInterval = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "clockUpdateInterval").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("clock").addMarker("update").addMarker("interval").addMarker("sp").addMarker("equipHis")
                .setMinVal("1").setMaxVal("120").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz).build();
        String clockUpdateIntervalId = hayStack.addPoint(clockUpdateInterval);
        HashMap clockUpdateIntervalPoint = hayStack.read("point and tuner and default and clock and update and interval");
        ArrayList<HashMap> clockUpdateIntervalArr = hayStack.readPoint(clockUpdateIntervalPoint.get("id").toString());
        for (HashMap valMap : clockUpdateIntervalArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(clockUpdateIntervalId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(clockUpdateIntervalId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point perDegreeHumidityFactor = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "perDegreeHumidityFactor").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("per").addMarker("degree").addMarker("humidity").addMarker("factor").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("%")
                .setTz(tz).build();
        String perDegreeHumidityFactorId = hayStack.addPoint(perDegreeHumidityFactor);
        HashMap perDegreeHumidityFactorPoint = hayStack.read("point and tuner and default and per and degree and humidity and factor");
        ArrayList<HashMap> perDegreeHumidityFactorArr = hayStack.readPoint(perDegreeHumidityFactorPoint.get("id").toString());
        for (HashMap valMap : perDegreeHumidityFactorArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(perDegreeHumidityFactorId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(perDegreeHumidityFactorId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point ccuAlarmVolumeLevel = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "ccuAlarmVolumeLevel").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("alarm").addMarker("volume").addMarker("level").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("7").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String ccuAlarmVolumeLevelId = hayStack.addPoint(ccuAlarmVolumeLevel);
        HashMap ccuAlarmVolumeLevelPoint = hayStack.read("point and tuner and default and alarm and volume and level");
        ArrayList<HashMap> ccuAlarmVolumeLevelArr = hayStack.readPoint(ccuAlarmVolumeLevelPoint.get("id").toString());
        for (HashMap valMap : ccuAlarmVolumeLevelArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(ccuAlarmVolumeLevelId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(ccuAlarmVolumeLevelId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point cmHeartBeatInterval = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "cmHeartBeatInterval").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("cm").addMarker("heart").addMarker("beat").addMarker("interval").addMarker("level").addMarker("sp").addMarker("equipHis")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz).build();
        String cmHeartBeatIntervalId = hayStack.addPoint(cmHeartBeatInterval);
        HashMap cmHeartBeatIntervalPoint = hayStack.read("point and tuner and default and cm and heart and beat and interval");
        ArrayList<HashMap> cmHeartBeatIntervalArr = hayStack.readPoint(cmHeartBeatIntervalPoint.get("id").toString());
        for (HashMap valMap : cmHeartBeatIntervalArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(cmHeartBeatIntervalId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(cmHeartBeatIntervalId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        Point heartBeatsToSkip = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "heartBeatsToSkip").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("heart").addMarker("beats").addMarker("to").addMarker("skip").addMarker("sp").addMarker("equipHis")
                .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String heartBeatsToSkipId = hayStack.addPoint(heartBeatsToSkip);
        HashMap heartBeatsToSkipPoint = hayStack.read("point and tuner and default and heart and beats and to and skip");
        ArrayList<HashMap> heartBeatsToSkipArr = hayStack.readPoint(heartBeatsToSkipPoint.get("id").toString());
        for (HashMap valMap : heartBeatsToSkipArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(heartBeatsToSkipId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(heartBeatsToSkipId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        Point cmResetCommandTime = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "cmResetCommandTimer").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("reset").addMarker("command").addMarker("time").addMarker("equipHis")
                .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER).setUnit("m")
                .setTz(tz).build();
        String cmResetCommandTimeId = hayStack.addPoint(cmResetCommandTime);
        HashMap cmResetCommandTimePoint = hayStack.read("point and tuner and default and reset and command and time");
        ArrayList<HashMap> cmResetCommandTimeArr = hayStack.readPoint(cmResetCommandTimePoint.get("id").toString());
        for (HashMap valMap : cmResetCommandTimeArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(cmResetCommandTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(cmResetCommandTimeId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        Point zoneTempDeadLeeway = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "zoneTemperatureDeadLeeway").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("temp").addMarker("dead").addMarker("leeway").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP).setUnit("\u00B0F")
                .setTz(tz).build();
        String zoneTempDeadLeewayId = hayStack.addPoint(zoneTempDeadLeeway);
        HashMap zoneTempDeadLeewayPoint = hayStack.read("point and tuner and default and temp and dead and leeway");
        ArrayList<HashMap> zoneTempDeadLeewayArr = hayStack.readPoint(zoneTempDeadLeewayPoint.get("id").toString());
        for (HashMap valMap : zoneTempDeadLeewayArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneTempDeadLeewayId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(zoneTempDeadLeewayId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        Point humidityCompensationOffset = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "humidityCompensationOffset").setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("humidity").addMarker("compensation").addMarker("offset").addMarker("sp").addMarker("equipHis").setTz(tz).build();
        String humidityCompensationOffsetId = hayStack.addPoint(humidityCompensationOffset);
        HashMap humidityCompensationOffsetPoint = hayStack.read("point and tuner and default and humidity and compensation and offset");
        ArrayList<HashMap> humidityCompensationOffsetArr = hayStack.readPoint(humidityCompensationOffsetPoint.get("id").toString());
        for (HashMap valMap : humidityCompensationOffsetArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(humidityCompensationOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                //hayStack.writeHisValById(humidityCompensationOffsetId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        hayStack.writeHisValById(humidityCompensationOffsetId, HSUtil.getPriorityVal(humidityCompensationOffsetId));
    }
    
    public void updateGatewayRef(String systemEquipId)
    {
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
        for (HashMap m : equips)
        {
            Equip q = new Equip.Builder().setHashMap(m)/*.setGatewayRef(systemEquipId)*/.build();
            if (q.getMarkers().contains("dab") || q.getMarkers().contains("vav"))
                q.setAhuRef(systemEquipId);
            else q.setGatewayRef(systemEquipId);
            CCUHsApi.getInstance().updateEquip(q, q.getId());
        }
        CCUHsApi.getInstance().updateCCUahuRef(systemEquipId);
    }
    
    public void addCMPoints(String siteRef, String equipref, String equipDis , String tz) {
		Point cmCurrentTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmCurrentTemp").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cm").addMarker("current").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        String ctID = CCUHsApi.getInstance().addPoint(cmCurrentTemp);
        CCUHsApi.getInstance().writeHisValById(ctID, 0.0);
        Point cmCoolDesiredTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmCoolingDesiredTemp").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cm").addMarker("cooling").addMarker("desired").addMarker("temp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        String cmCoolDesiredTempId = CCUHsApi.getInstance().addPoint(cmCoolDesiredTemp);
        CCUHsApi.getInstance().writeDefaultValById(cmCoolDesiredTempId, 0.0);
        CCUHsApi.getInstance().writeHisValById(cmCoolDesiredTempId, 0.0);
        Point cmHeatDesiredTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmHeatingDesiredTemp").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cm").addMarker("heating").addMarker("desired").addMarker("temp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        String cmHeatDesiredTempId = CCUHsApi.getInstance().addPoint(cmHeatDesiredTemp);
        CCUHsApi.getInstance().writeDefaultValById(cmHeatDesiredTempId, 0.0);
        CCUHsApi.getInstance().writeHisValById(cmHeatDesiredTempId, 0.0);
    }
    
    
    public void addDefaultSystemPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point systemStatusMessage = new Point.Builder()
                .setDisplayName(equipDis + "-StatusMessage")
                .setEquipRef(equipref)
                .setSiteRef(siteRef)
                .addMarker("system")
                .addMarker("status")
                .addMarker("message")
                .addMarker("writable")
                .setTz(tz)
                .setKind("string").build();
        CCUHsApi.getInstance().addPoint(systemStatusMessage);
        Point systemScheduleStatus = new Point.Builder()
                .setDisplayName(equipDis + "-ScheduleStatus")
                .setEquipRef(equipref).setSiteRef(siteRef)
                .addMarker("system")
                .addMarker("scheduleStatus")
                .addMarker("writable")
                .setTz(tz)
                .setKind("string").build();
        CCUHsApi.getInstance().addPoint(systemScheduleStatus);
        
        Point outsideTemperature = new Point.Builder().setDisplayName(equipDis + "-" + "outsideTemperature").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("outside").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(outsideTemperature);
    
        Point outsideHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "outsideHumidity").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("outside").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(outsideHumidity);
    }
    
    //VAV & DAB System profile common points are added here.
    public void addRTUSystemPoints(String siteRef, String equipref, String equipDis, String tz) {
        addDefaultSystemPoints(siteRef, equipref, equipDis, tz);
        Point systemOccupancy = new Point.Builder().setDisplayName(equipDis + "-" + "occupancy").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("occupancy").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing").setTz(tz).build();
        String sysOccupancyId = CCUHsApi.getInstance().addPoint(systemOccupancy);
        CCUHsApi.getInstance().writeHisValById(sysOccupancyId, 0.0);
        Point systemOperatingMode = new Point.Builder().setDisplayName(equipDis + "-" + "operatingMode").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("operating").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").setEnums("off,cooling,heating").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(systemOperatingMode);
        Point ciRunning = new Point.Builder().setDisplayName(equipDis + "-" + "systemCI").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("ci").addMarker("running").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(ciRunning);
        Point averageHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "averageHumidity").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("average").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(averageHumidity);
        Point cmHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "cmHumidity").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cm").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).setUnit("%").build();
        String cmHumidityId = CCUHsApi.getInstance().addPoint(cmHumidity);
        CCUHsApi.getInstance().writeHisValById(cmHumidityId, 0.0);
        Point averageTemperature = new Point.Builder().setDisplayName(equipDis + "-" + "averageTemperature").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("average").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(averageTemperature);
        addCMPoints(siteRef, equipref, equipDis, tz);
    }
    
    public void updateOutsideWeatherParams() {
        try {
            CCUHsApi.getInstance().writeHisValByQuery("system and outside and temp", CCUHsApi.getInstance().getExternalTemp());
            CCUHsApi.getInstance().writeHisValByQuery("system and outside and humidity", CCUHsApi.getInstance().getExternalHumidity());
        } catch (Exception e)
        {
            e.printStackTrace();
            Log.d(L.TAG_CCU_OAO, " Failed to read external Temp or Humidity , Disable Economizing");
        }
    }
    
    public void reset() {
    
    }
}
