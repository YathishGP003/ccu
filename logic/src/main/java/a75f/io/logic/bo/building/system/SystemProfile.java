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
            if(q.getMarkers().contains("dab") || q.getMarkers().contains("vav"))
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
    
        Point userLimitSpread = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "userLimitSpread").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("user").addMarker("limit").addMarker("spread").addMarker("sp").addMarker("equipHis").setTz(tz).build();
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
    
        /*Point buildingLimitMin = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "buildingLimitMin").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("building").addMarker("limit").addMarker("min").addMarker("sp").addMarker("equipHis").setUnit("\u00B0F").setTz(tz).build();
        String buildingLimitMinId = hayStack.addPoint(buildingLimitMin);
        HashMap buildingLimitMinPoint = hayStack.read("point and tuner and default and building and limit and min");
        ArrayList<HashMap> buildingLimitMinPointArr = hayStack.readPoint(buildingLimitMinPoint.get("id").toString());
        for (HashMap valMap : buildingLimitMinPointArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(buildingLimitMinId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(buildingLimitMinId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point buildingLimitMax = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "buildingLimitMax").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("building").addMarker("limit").addMarker("max").addMarker("sp").addMarker("equipHis").setUnit("\u00B0F").setTz(tz).build();
        String buildingLimitMaxId = hayStack.addPoint(buildingLimitMax);
        HashMap buildingLimitMaxPoint = hayStack.read("point and tuner and default and building and limit and max");
        ArrayList<HashMap> buildingLimitMaxPointArr = hayStack.readPoint(buildingLimitMaxPoint.get("id").toString());
        for (HashMap valMap : buildingLimitMaxPointArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(buildingLimitMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(buildingLimitMaxId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point buildingToZoneDifferential = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "buildingToZoneDifferential").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("building").addMarker("zone").addMarker("differential").addMarker("sp").addMarker("equipHis").setTz(tz).build();
        String buildingToZoneDifferentialId = hayStack.addPoint(buildingToZoneDifferential);
        HashMap buildingToZoneDifferentialPoint = hayStack.read("point and tuner and default and building and zone and differential");
        ArrayList<HashMap> buildingToZoneDifferentialPointArr = hayStack.readPoint(buildingToZoneDifferentialPoint.get("id").toString());
        for (HashMap valMap : buildingToZoneDifferentialPointArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(buildingToZoneDifferentialId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(buildingToZoneDifferentialId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point zoneTemperatureDeadLeeway = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "zoneTemperatureDeadLeeway").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("zone").addMarker("temp").addMarker("dead").addMarker("leeway").addMarker("sp").addMarker("equipHis").setTz(tz).build();
        Point zoneTemperatureDeadLeeway = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "zoneTemperatureDeadLeeway").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("zone").addMarker("temp").addMarker("dead").addMarker("leeway").addMarker("sp").addMarker("equipHis")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String zoneTemperatureDeadLeewayId = hayStack.addPoint(zoneTemperatureDeadLeeway);
        HashMap zoneTemperatureDeadLeewayPoint = hayStack.read("point and tuner and default and zone and temp and dead and leeway");
        ArrayList<HashMap> zoneTemperatureDeadLeewayPointArr = hayStack.readPoint(zoneTemperatureDeadLeewayPoint.get("id").toString());
        for (HashMap valMap : zoneTemperatureDeadLeewayPointArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneTemperatureDeadLeewayId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(zoneTemperatureDeadLeewayId, Double.parseDouble(valMap.get("val").toString()));
            }
        }*/
        
        Point heatingPreconditioningRate = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "heatingPreconditioningRate").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("heating").addMarker("precon").addMarker("rate").addMarker("sp").addMarker("equipHis")
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
        Point coolingPreconditioningRate = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "coolingPreconditioningRate").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("cooling").addMarker("precon").addMarker("rate").addMarker("sp").addMarker("equipHis")
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

        Point cmTempInfPercentileZonesDead = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "cmTempPercentDeadZonesAllowed").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("zone").addMarker("percent").addMarker("dead").addMarker("influence").addMarker("sp").addMarker("equipHis")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String cmTempInfPercentileZonesDeadId = hayStack.addPoint(cmTempInfPercentileZonesDead);
        HashMap cmTempInfPercentileZonesDeadPoint = hayStack.read("point and tuner and default and zone and percent and dead and influence");
        ArrayList<HashMap> cmTempInfPercentileZonesDeadPointArr = hayStack.readPoint(cmTempInfPercentileZonesDeadPoint.get("id").toString());
        for (HashMap valMap : cmTempInfPercentileZonesDeadPointArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(cmTempInfPercentileZonesDeadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(cmTempInfPercentileZonesDeadId, Double.parseDouble(valMap.get("val").toString()));
            }
        }

        addDefaultDabSystemTuners();
    }

    public void addDefaultDabSystemTuners() {
        Point targetCumulativeDamper = new Point.Builder()
                .setDisplayName(equipDis + "-DAB-" + "targetCumulativeDamper")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp")
                .setUnit("%")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                .setTz(tz)
                .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        hayStack.writePoint(targetCumulativeDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_CUMULATIVE_DAMPER, 0);
        hayStack.writeHisValById(targetCumulativeDamperId, TunerConstants.TARGET_CUMULATIVE_DAMPER);

        Point analogFanSpeedMultiplier = new Point.Builder()
                .setDisplayName(equipDis + "-DAB-" + "analogFanSpeedMultiplier")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                .setMinVal("0.1").setMaxVal("3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                .setTz(tz)
                .build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        hayStack.writePoint(analogFanSpeedMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ANALOG_FANSPEED_MULTIPLIER, 0);
        hayStack.writeHisValById(analogFanSpeedMultiplierId, TunerConstants.ANALOG_FANSPEED_MULTIPLIER);

        Point humidityHysteresis = new Point.Builder()
                .setDisplayName(equipDis + "-DAB-" + "humidityHysteresis")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                .setUnit("%")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                .setTz(tz)
                .build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        hayStack.writePoint(humidityHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.HUMIDITY_HYSTERESIS_PERCENT, 0);
        hayStack.writeHisValById(humidityHysteresisId, TunerConstants.HUMIDITY_HYSTERESIS_PERCENT);

        Point relayDeactivationHysteresis = new Point.Builder()
                .setDisplayName(equipDis + "-DAB-" + "relayDeactivationHysteresis")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                .setUnit("%")
                .setMinVal("0").setMaxVal("10").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                .setTz(tz)
                .build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        hayStack.writePoint(relayDeactivationHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.RELAY_DEACTIVATION_HYSTERESIS, 0);
        hayStack.writeHisValById(relayDeactivationHysteresisId, TunerConstants.RELAY_DEACTIVATION_HYSTERESIS);

        Point humidityCompensationOffset = new Point.Builder()
                .setDisplayName(equipDis+"-"+"humidityCompensationOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("system").addMarker("humidity").addMarker("compensation").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz)
                .build();
        String humidityCompensationOffsetId = hayStack.addPoint(humidityCompensationOffset);
        hayStack.writePoint(humidityCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 0.0, 0);
        hayStack.writeHisValById(humidityCompensationOffsetId, 0.0);
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
		Point cmCurrentTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmCurrentTemp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("current").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        String ctID = CCUHsApi.getInstance().addPoint(cmCurrentTemp);
        CCUHsApi.getInstance().writeHisValById(ctID, 0.0);
        Point cmCoolDesiredTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmCoolingDesiredTemp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("cooling").addMarker("desired").addMarker("temp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        String cmCoolDesiredTempId = CCUHsApi.getInstance().addPoint(cmCoolDesiredTemp);
        CCUHsApi.getInstance().writeDefaultValById(cmCoolDesiredTempId, 0.0);
        CCUHsApi.getInstance().writeHisValById(cmCoolDesiredTempId, 0.0);
        Point cmHeatDesiredTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmHeatingDesiredTemp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("heating").addMarker("desired").addMarker("temp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
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
        
        Point outsideTemperature = new Point.Builder().setDisplayName(equipDis + "-" + "outsideTemperature").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("outside").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(outsideTemperature);
    
        Point outsideHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "outsideHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("outside").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(outsideHumidity);
    }
    
    //VAV & DAB System profile common points are added here.
    public void addRTUSystemPoints(String siteRef, String equipref, String equipDis, String tz) {
        addDefaultSystemPoints(siteRef, equipref, equipDis, tz);
        Point systemOccupancy = new Point.Builder().setDisplayName(equipDis + "-" + "occupancy").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("occupancy").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(systemOccupancy);
        Point systemOperatingMode = new Point.Builder().setDisplayName(equipDis + "-" + "operatingMode").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("operating").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").setEnums("off,cooling,heating").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(systemOperatingMode);
        Point ciRunning = new Point.Builder().setDisplayName(equipDis + "-" + "systemCI").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("ci").addMarker("running").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(ciRunning);
        Point averageHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "averageHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("average").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(averageHumidity);
        Point cmHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "cmHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).setUnit("%").build();
        CCUHsApi.getInstance().addPoint(cmHumidity);
        Point averageTemperature = new Point.Builder().setDisplayName(equipDis + "-" + "averageTemperature").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("average").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
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
