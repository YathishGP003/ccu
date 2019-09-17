package a75f.io.logic.bo.building.system;

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
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.building.system.dab.DabSystemProfile;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemProfile;

/**
 * Created by Yinten isOn 8/15/2017.
 */
public abstract class SystemProfile
{
   
    public Schedule schedule = new Schedule();
    
    public TRSystem trSystem;
    
    public SystemEquip sysEquip;
    private String equipRef = null;
    
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
        return CCUHsApi.getInstance().readHisValByQuery(tags+" and equipRef == \""+getSystemEquipRef()+"\"");
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
    
    public String getSystemEquipRef() {
        if (equipRef == null)
        {
            HashMap equip = CCUHsApi.getInstance().read("equip and system");
            equipRef = equip.get("id").toString();
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
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();
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
    
        Point buildingLimitMin = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "buildingLimitMin").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("building").addMarker("limit").addMarker("min").addMarker("sp").addMarker("equipHis").setTz(tz).build();
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
    
        Point buildingLimitMax = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "buildingLimitMax").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("building").addMarker("limit").addMarker("max").addMarker("sp").addMarker("equipHis").setTz(tz).build();
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
        }
        
        Point heatingPreconditioningRate = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "heatingPreconditioningRate").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("heating").addMarker("precon").addMarker("rate").addMarker("sp").addMarker("equipHis").setTz(tz).build();
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
        Point coolingPreconditioningRate = new Point.Builder().setDisplayName(HSUtil.getDis(equipRef) + "-" + "coolingPreconditioningRate").setSiteRef(siteRef).setEquipRef(equipRef).addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("cooling").addMarker("precon").addMarker("rate").addMarker("sp").addMarker("equipHis").setTz(tz).build();
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
        Point cmAlive = new Point.Builder().setDisplayName(equipDis + "-" + "cmAlive").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("alive").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(cmAlive);
        Point cmCurrentTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmCurrentTemp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("current").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(cmCurrentTemp);
        Point cmDesiredTemp = new Point.Builder().setDisplayName(equipDis + "-" + "cmDesiredTemp").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("desired").addMarker("temp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        String cmDesiredTempId = CCUHsApi.getInstance().addPoint(cmDesiredTemp);
        CCUHsApi.getInstance().writeDefaultValById(cmDesiredTempId, 0.0);
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
    }
    
    //VAV & DAB System profile common points are added here.
    public void addRTUSystemPoints(String siteRef, String equipref, String equipDis, String tz) {
        addDefaultSystemPoints(siteRef, equipref, equipDis, tz);
        Point systemOccupancy = new Point.Builder().setDisplayName(equipDis + "-" + "occupancy").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("occupancy").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(systemOccupancy);
        Point systemOperatingMode = new Point.Builder().setDisplayName(equipDis + "-" + "operatingMode").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("operating").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(systemOperatingMode);
        Point ciRunning = new Point.Builder().setDisplayName(equipDis + "-" + "systemCI").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("ci").addMarker("running").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(ciRunning);
        Point averageHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "averageHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("average").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(averageHumidity);
        Point outsideHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "outsideHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("outside").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(outsideHumidity);
        Point calculatedHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "calculatedHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("calculated").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(calculatedHumidity);
        Point targetHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "targetHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("target").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(targetHumidity);
        Point cmHumidity = new Point.Builder().setDisplayName(equipDis + "-" + "cmHumidity").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("cm").addMarker("humidity").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(cmHumidity);
        Point averageTemperature = new Point.Builder().setDisplayName(equipDis + "-" + "averageTemperature").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("average").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(averageTemperature);
        Point outsideTemperature = new Point.Builder().setDisplayName(equipDis + "-" + "outsideTemperature").setSiteRef(siteRef).setEquipRef(equipref).addMarker("system").addMarker("outside").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("sp").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(outsideTemperature);
        
        addCMPoints(siteRef, equipref, equipDis, tz);
    }
    
    public void reset() {
    
    }
}
