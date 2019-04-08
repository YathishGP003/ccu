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
import a75f.io.logic.bo.building.system.vav.VavSystemController;

/**
 * Created by Yinten isOn 8/15/2017.
 */
public abstract class SystemProfile
{
   
    public Schedule schedule = new Schedule();
    
    public TRSystem trSystem;
    
    private String equipRef = null;
    
    public abstract void doSystemControl();
    
    public abstract void addSystemEquip();
    
    public abstract void deleteSystemEquip();
    
    public abstract boolean isCoolingAvailable();
    
    public abstract boolean isHeatingAvailable();
    
    public abstract ProfileType getProfileType();
    
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
        switch (getProfileType()) {
            case SYSTEM_VAV_ANALOG_RTU:
            case SYSTEM_VAV_STAGED_RTU:
            case SYSTEM_VAV_STAGED_VFD_RTU:
            case SYSTEM_VAV_HYBRID_RTU:
                return VavSystemController.getInstance();
            case SYSTEM_DAB_STAGED_RTU:
                return DabSystemController.getInstance();
            case SYSTEM_DEFAULT:
                return DefaultSystemController.getInstance();
        }
        return DefaultSystemController.getInstance();
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
    }
    
}
