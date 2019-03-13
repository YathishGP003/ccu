package a75f.io.logic.bo.building.system;

import a75.io.algos.tr.TRSystem;
import a75f.io.logic.bo.building.Schedule;

/**
 * Created by Yinten isOn 8/15/2017.
 */
public abstract class SystemProfile
{
   
    public Schedule schedule = new Schedule();
    
    public TRSystem trSystem;
    
    public SystemEquip sysEquip;
    
    public abstract void doSystemControl();
    
    public abstract void addSystemEquip();
    
    public abstract void deleteSystemEquip();
    
    public abstract boolean isCoolingAvailable();
    
    public abstract boolean isHeatingAvailable();
    
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
    
    /*@JsonIgnore
    public Struct getSystemControlMsg() {
        return null;
    }*/
}
