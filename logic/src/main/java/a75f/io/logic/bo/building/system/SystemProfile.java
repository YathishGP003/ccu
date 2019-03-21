package a75f.io.logic.bo.building.system;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.tr.TRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
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
    
    public String equipRef = null;
    
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
    
}
