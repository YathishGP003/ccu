package a75f.io.logic.bo.building.system.vav;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.SystemState;
import a75f.io.logic.tuners.TunerConstants;

/**
 * Created by samjithsadasivan on 1/10/19.
 */

public abstract class VavSystemProfile extends SystemProfile
{
    public double systemCoolingLoopOp;
    public double systemHeatingLoopOp;
    public double systemFanLoopOp;
    
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
        addSystemPoints(siteRef, equipRef, equipDis, tz);
        addTrTargetPoints(siteRef,equipRef,equipDis,tz);
    }
    
    private void addSystemLoopOpPoint(String loop, String siteRef, String equipref, String equipDis, String tz)
    {
        Point relay1Op = new Point.Builder().setDisplayName(equipDis + "-" + loop + "LoopOutput").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker(loop).addMarker("loop").addMarker("output").addMarker("his").addMarker("writable").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(relay1Op);
    }
    
    private void addSystemPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point systemOccupancy = new Point.Builder().setDisplayName(equipDis + "-" + "occupancy").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("occupancy").addMarker("status").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(systemOccupancy);
        Point systemOperatingMode = new Point.Builder().setDisplayName(equipDis + "-" + "operatingMode").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("operating").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(systemOperatingMode);
        Point ciRunning = new Point.Builder().setDisplayName(equipDis + "-" + "systemCI").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("ci").addMarker("running").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(ciRunning);
        Point averageHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "averageHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("average").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(averageHumidity);
        Point averageTemperature = new Point.Builder().setDisplayName(equipDis + "-" + "averageTemperature").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("average").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(averageTemperature);
        Point weightedAverageCoolingLoadMA = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageCoolingLoadMA").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("moving").addMarker("average").addMarker("cooling").addMarker("load").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(weightedAverageCoolingLoadMA);
        Point weightedAverageHeatingLoadMA = new Point.Builder().setDisplayName(equipDis + "-" + "weightedAverageHeatingLoadMA").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("moving").addMarker("average").addMarker("heating").addMarker("load").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(weightedAverageHeatingLoadMA);
    }
    
    public void setSystemPoint(String tags, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and his and " + tags, val);
    }
    
    public void setSystemLoopOp(String loop, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and loop and output and his and " + loop, val);
    }
    
    public void addVavSystemTuners(String equipref)
    {
        addSystemTuners();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();
        Point targetCumulativeDamper = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "targetCumulativeDamper").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp").addMarker("equipHis").setTz(tz).build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        HashMap targetCumulativeDamperP = hayStack.read("point and tuner and default and vav and target and cumulative and damper");
        ArrayList<HashMap> targetCumulativeDamperArr = hayStack.readPoint(targetCumulativeDamperP.get("id").toString());
        for (HashMap valMap : targetCumulativeDamperArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(targetCumulativeDamperId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(targetCumulativeDamperId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        Point analogFanSpeedMultiplier = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "analogFanSpeedMultiplier").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp").addMarker("equipHis").setTz(tz).build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        HashMap analogFanSpeedMultiplierP = hayStack.read("point and tuner and default and vav and analog and fan and speed and multiplier");
        ArrayList<HashMap> analogFanSpeedMultiplierArr = hayStack.readPoint(analogFanSpeedMultiplierP.get("id").toString());
        for (HashMap valMap : analogFanSpeedMultiplierArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(analogFanSpeedMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(analogFanSpeedMultiplierId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        Point humidityHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "humidityHysteresis").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("humidity").addMarker("hysteresis").addMarker("sp").addMarker("equipHis").setTz(tz).build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        HashMap humidityHysteresisPoint = hayStack.read("point and tuner and default and vav and humidity and hysteresis");
        ArrayList<HashMap> humidityHysteresisArr = hayStack.readPoint(humidityHysteresisPoint.get("id").toString());
        for (HashMap valMap : humidityHysteresisArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(humidityHysteresisId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(humidityHysteresisId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        Point relayDeactivationHysteresis = new Point.Builder().setDisplayName(HSUtil.getDis(equipref) + "-" + "relayDeactivationHysteresis").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp").addMarker("equipHis").setTz(tz).build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        HashMap relayDeactivationHysteresisPoint = hayStack.read("point and tuner and default and vav and relay and deactivation and hysteresis");
        ArrayList<HashMap> relayDeactivationHysteresisArr = hayStack.readPoint(relayDeactivationHysteresisPoint.get("id").toString());
        for (HashMap valMap : relayDeactivationHysteresisArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(relayDeactivationHysteresisId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(relayDeactivationHysteresisId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    }
    
    protected void addUserIntentPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        Point desiredCI = new Point.Builder().setDisplayName(equipDis + "-" + "desiredCI").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("ci").addMarker("desired").addMarker("sp").addMarker("his").addMarker("equipHis").setTz(tz).build();
        String desiredCIId = CCUHsApi.getInstance().addPoint(desiredCI);
        CCUHsApi.getInstance().writePoint(desiredCIId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_DEFAULT_CI, 0);
        CCUHsApi.getInstance().writeHisValById(desiredCIId, TunerConstants.SYSTEM_DEFAULT_CI);
        Point systemState = new Point.Builder().setDisplayName(equipDis + "-" + "systemMode").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("rtu").addMarker("mode").addMarker("sp").addMarker("his").addMarker("equipHis").setTz(tz).build();
        String systemStateId = CCUHsApi.getInstance().addPoint(systemState);
        CCUHsApi.getInstance().writePoint(systemStateId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", (double) SystemState.OFF.ordinal(), 0);
        CCUHsApi.getInstance().writeHisValById(systemStateId, (double) SystemState.OFF.ordinal());
        Point targetMaxInsideHumidty = new Point.Builder().setDisplayName(equipDis + "-" + "targetMaxInsideHumidty").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("max").addMarker("his").addMarker("equipHis").setTz(tz).addMarker("inside").addMarker("humidity").addMarker("sp").build();
        String targetMaxInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMaxInsideHumidty);
        CCUHsApi.getInstance().writePoint(targetMaxInsideHumidtyId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_MAX_INSIDE_HUMIDITY, 0);
        CCUHsApi.getInstance().writeHisValById(targetMaxInsideHumidtyId, TunerConstants.TARGET_MAX_INSIDE_HUMIDITY);
        Point targetMinInsideHumidty = new Point.Builder().setDisplayName(equipDis + "-" + "targetMinInsideHumidty").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("min").addMarker("inside").addMarker("humidity").addMarker("sp").addMarker("his").addMarker("equipHis").setTz(tz).build();
        String targetMinInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMinInsideHumidty);
        CCUHsApi.getInstance().writePoint(targetMinInsideHumidtyId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_MIN_INSIDE_HUMIDITY, 0);
        CCUHsApi.getInstance().writeHisValById(targetMaxInsideHumidtyId, TunerConstants.TARGET_MIN_INSIDE_HUMIDITY);
        Point compensateHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "compensateHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("compensate").addMarker("humidity").setTz(tz).build();
        String compensateHumidityId = CCUHsApi.getInstance().addPoint(compensateHumidity);
        CCUHsApi.getInstance().writePoint(compensateHumidityId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", 0.0, 0);
        CCUHsApi.getInstance().writeHisValById(compensateHumidityId, 0.0);
        Point demandResponseMode = new Point.Builder().setDisplayName(equipDis + "-" + "demandResponseMode").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("demand").addMarker("response").setTz(tz).build();
        String demandResponseModeId = CCUHsApi.getInstance().addPoint(demandResponseMode);
        CCUHsApi.getInstance().writePoint(demandResponseModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", 0.0, 0);
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
        hayStack.writePoint(id, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", val, 0);
    }
    
    public void addTrTargetPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point sat = new Point.Builder().setDisplayName(equipDis + "-" + "satTRSp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("tr").addMarker("sat").addMarker("target").addMarker("his").addMarker("sp").addMarker("equipHis").setUnit("\u00B0F").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(sat);
        Point co2 = new Point.Builder().setDisplayName(equipDis + "-" + "co2TRSp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("tr").addMarker("co2").addMarker("target").addMarker("his").addMarker("sp").addMarker("equipHis").setUnit("\u00B0ppm").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(co2);
        Point sp = new Point.Builder().setDisplayName(equipDis + "-" + "staticPressureTRSp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("tr").addMarker("staticPressure").addMarker("target").addMarker("his").addMarker("sp").addMarker("equipHis").setUnit("\u00B0in").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(sp);
    }
    
    public void setTrTargetVals()
    {
        if(trSystem == null) {
            Log.d(L.TAG_CCU_SYSTEM, " TRSystem not initialized , Skip trSPUpdate");
            return;
        }
        CCUHsApi.getInstance().writeHisValByQuery("point and system and tr and target and his and sat", trSystem.satTRProcessor.getSetPoint());
        CCUHsApi.getInstance().writeHisValByQuery("point and system and tr and target and his and co2", trSystem.co2TRProcessor.getSetPoint());
        CCUHsApi.getInstance().writeHisValByQuery("point and system and tr and target and his and staticPressure", trSystem.spTRProcessor.getSetPoint());
    }
    
    
    
}
