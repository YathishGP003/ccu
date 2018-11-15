package a75f.io.logic.bo.building.system;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.logic.tuners.SystemTunerUtil;

/**
 * Default System handles PI controlled op
 */
public class VavAnalogRtu extends SystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
    public boolean analog1Enabled = false;
    public boolean analog2Enabled = false;
    public boolean analog3Enabled = false;
    public boolean analog4Enabled = false;
    public VavAnalogRtu() {
        trSystem =  new VavTRSystem();
    }
    
    
    @JsonIgnore
    public  int getSystemSAT() {
        return ((VavTRSystem)trSystem).getCurrentSAT();
    }
    
    @JsonIgnore
    public  int getSystemCO2() {
        return ((VavTRSystem)trSystem).getCurrentCO2();
    }
    
    @JsonIgnore
    public  int getSystemOADamper() {
        return (((VavTRSystem)trSystem).getCurrentCO2() - CO2_MIN) * 100 / (CO2_MAX - CO2_MIN);
    }
    
    @JsonIgnore
    public int getStaticPressure() {
        return (int)((VavTRSystem)trSystem).getCurrentSp();
    }
    
    @JsonIgnore
    public int getAnalog1Out() {
        int analogMin = (int)SystemTunerUtil.getTuner("analog1", "min");
        int analogMax = (int)SystemTunerUtil.getTuner("analog1", "max");
        Log.d("CCU", "analogMin: "+analogMin+" analogMax: "+analogMax+" SAT: "+getSystemSAT());
        analog1OutSignal = analogMin + (analogMax - analogMin) * (65 - getSystemSAT())/10;
        return analog1OutSignal;
    }
    
    @JsonIgnore
    public int getAnalog2Out() {
        int analogMin = (int)SystemTunerUtil.getTuner("analog2", "min");
        int analogMax = (int)SystemTunerUtil.getTuner("analog2", "max");
        
        analog2OutSignal = analogMin + (analogMax - analogMin) * (DxCIController.getInstance().getHeatingSignal())/100;
        return analog2OutSignal;
    }
    
    @JsonIgnore
    public int getAnalog3Out() {
        int analogMin = (int)SystemTunerUtil.getTuner("analog3", "min");
        int analogMax = (int)SystemTunerUtil.getTuner("analog3", "max");
    
        analog3OutSignal = analogMin + (analogMax - analogMin) * (getSystemCO2() - 800)/200;
        return analog3OutSignal;
    }
    
    @JsonIgnore
    public int getAnalog4Out() {
        int analogMin = (int)SystemTunerUtil.getTuner("analog4", "min");
        int analogMax = (int)SystemTunerUtil.getTuner("analog4", "max");
        
        analog4OutSignal  = analogMin + (analogMax - analogMin) * (10 * getStaticPressure() - 1)/15;
        return analog4OutSignal;
    }
    
    @JsonIgnore
    public String getProfileName() {
        return "VAV Analog RTU";
    }
}
