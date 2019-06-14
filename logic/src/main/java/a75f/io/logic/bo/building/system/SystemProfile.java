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
            Equip q = new Equip.Builder().setHashMap(m).setAhuRef(systemEquipId).build();
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
}
