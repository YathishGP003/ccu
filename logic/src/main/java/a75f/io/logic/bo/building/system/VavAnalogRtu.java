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
    
    private static final int ANALOG_SCALE = 10;
    
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
        return (int)SystemEquip.getInstance().getAnalogOut("analog1");
    }
    
    @JsonIgnore
    public int getAnalog2Out() {
        return (int)SystemEquip.getInstance().getAnalogOut("analog2");
    }
    
    @JsonIgnore
    public int getAnalog3Out() {
        return (int)SystemEquip.getInstance().getAnalogOut("analog3");
    }
    
    @JsonIgnore
    public int getAnalog4Out() {
        return (int)SystemEquip.getInstance().getAnalogOut("analog4");
    }
    
    @JsonIgnore
    public String getProfileName() {
        return "VAV Analog RTU";
    }
    
    @Override
    public void doSystemControl() {
        if (trSystem != null) {
            trSystem.processResetResponse();
        }
        DxCIController.getInstance().runDxCIAlgo();
        updateSystemPoints();
    }
    
    private void updateSystemPoints() {
    
        SystemEquip.getInstance().setSat(getSystemSAT());
        SystemEquip.getInstance().setCo2(getSystemCO2());
        SystemEquip.getInstance().setSp(getStaticPressure());
        SystemEquip.getInstance().setHwst(0);
        
        double analogMin = SystemTunerUtil.getTuner("analog1", "min");
        double analogMax = SystemTunerUtil.getTuner("analog1", "max");
        Log.d("CCU", "analogMin: "+analogMin+" analogMax: "+analogMax+" SAT: "+getSystemSAT());
        int signal;
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (SystemConstants.COOLING_SAT_CONFIG_MAX - getSystemSAT()) / 10));
        } else {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (SystemConstants.COOLING_SAT_CONFIG_MAX - getSystemSAT()) / 10));
        }
        SystemEquip.getInstance().setAnalogOut("analog1", signal);
    
        analogMin = SystemTunerUtil.getTuner("analog2", "min");
        analogMax = SystemTunerUtil.getTuner("analog2", "max");
    
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (DxCIController.getInstance().getHeatingSignal()) / 100));
        } else {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (DxCIController.getInstance().getHeatingSignal()) / 100));
        }
        SystemEquip.getInstance().setAnalogOut("analog2", signal);
    
        analogMin = (int)SystemTunerUtil.getTuner("analog3", "min");
        analogMax = (int)SystemTunerUtil.getTuner("analog3", "max");
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (getSystemCO2() - SystemConstants.CO2_CONFIG_MIN) / 200));
        } else {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (getSystemCO2() - SystemConstants.CO2_CONFIG_MIN) / 200));
        }
        SystemEquip.getInstance().setAnalogOut("analog3", signal);
    
        analogMin = SystemTunerUtil.getTuner("analog4", "min");
        analogMax = SystemTunerUtil.getTuner("analog4", "max");
    
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (getStaticPressure() - SystemConstants.SP_CONFIG_MIN) / 15.0));
        } else {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (getStaticPressure() - SystemConstants.SP_CONFIG_MIN) / 15.0));
        }
        SystemEquip.getInstance().setAnalogOut("analog4", signal);
    }
}
