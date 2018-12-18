package a75f.io.logic.bo.building.system;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.tuners.SystemTunerUtil;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

public class VavStagedRtu extends SystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
    private static final int ANALOG_SCALE = 10;
    
    @JsonIgnore
    public String getProfileName()
    {
        return "VAV Staged RTU";
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
        
        SystemEquip systemEquip = SystemEquip.getInstance();
        
        double analogMin = SystemTunerUtil.getTuner("analog1", "min");
        double analogMax = SystemTunerUtil.getTuner("analog1", "max");
        
        int coolingSignal = 0;
        if (DxCIController.getInstance().getDxCIRtuState() == DxCIController.State.COOLING)
        {
            if (analogMax > analogMin)
            {
                coolingSignal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (SystemConstants.COOLING_SAT_CONFIG_MAX - getSystemSAT()) / 100));
            }
            else
            {
                coolingSignal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (SystemConstants.COOLING_SAT_CONFIG_MAX - getSystemSAT()) / 100));
            }
            
        }
        if (systemEquip.getAnalogOutSelection("analog1") > 0)
        {
            ControlMote.setAnalogOut("analog1", coolingSignal);
        }
        
        analogMin = SystemTunerUtil.getTuner("analog2", "min");
        analogMax = SystemTunerUtil.getTuner("analog2", "max");
        
        int heatingSignal = 0;
        if (DxCIController.getInstance().getDxCIRtuState() == DxCIController.State.HEATING)
        {
            if (analogMax > analogMin)
            {
                heatingSignal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (DxCIController.getInstance().getHeatingSignal()) / 100));
            }
            else
            {
                heatingSignal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (DxCIController.getInstance().getHeatingSignal()) / 100));
            }
        } else {
            heatingSignal = 0;
        }
        if (systemEquip.getAnalogOutSelection("analog2") > 0)
        {
            ControlMote.setAnalogOut("analog2", heatingSignal);
        }
    
        if (systemEquip.getAnalogOutSelection("analog3") > 0) {
            if (heatingSignal > 0 || coolingSignal > 0) {
                ControlMote.setAnalogOut("analog3", 100);
            }
        }
        
        int coolingStages = getCoolingStages();
        int heatingStages = getHeatingStages();
        int fanStages = getFanStages();
    
        Log.d("CCU", "coolingSignal: "+coolingSignal + " heatingSignal: " + heatingSignal);
        Log.d("CCU", "coolingStages: "+coolingStages + " heatingStages: "+heatingStages+" fanStages: "+fanStages);
        for (int i = 1; i <=7 ;i++)
        {
            switch (Stage.values()[(int)systemEquip.getRelaySelection("relay"+i)-1])
            {
                case COOLING_1:
                    if (coolingStages > 0 && coolingSignal > 0) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "COOLING_1 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case COOLING_2:
                    if (coolingStages > 0 && coolingSignal >= 100/coolingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "COOLING_2 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case COOLING_3:
                    if (coolingStages > 0 && coolingSignal >= 100 * 2/coolingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "COOLING_3 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case COOLING_4:
                    if (coolingStages > 0 && coolingSignal >= 100 * 3/coolingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "COOLING_4 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case COOLING_5:
                    if (coolingStages > 0 && coolingSignal >= 100 * 4/coolingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "COOLING_5 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case HEATING_1:
                    if (heatingStages > 0 && heatingSignal > 0) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "HEATING_1 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case HEATING_2:
                    if (heatingStages > 0 && heatingSignal >= 100/heatingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "HEATING_2 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case HEATING_3:
                    if (heatingStages > 0 && heatingSignal >= 100 * 2/heatingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "HEATING_3 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case HEATING_4:
                    if (heatingStages > 0 && heatingSignal >= 100 * 3/heatingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "HEATING_4 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case HEATING_5:
                    if (heatingStages > 0 && heatingSignal >= 100 * 4/heatingStages) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", " HEATING_5 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case FAN_1:
                    if (fanStages > 0 && (heatingSignal > 0 || coolingSignal > 0)) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", " FAN_1 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
                case FAN_2:
                    if (fanStages > 0 && (heatingSignal > 50 || coolingSignal > 50)) {
                        ControlMote.setRelayState("relay"+i, 1);
                        Log.d("CCU", "FAN_2 setRelay "+i);
                    } else {
                        ControlMote.setRelayState("relay"+i, 0);
                    }
                    break;
            }
        }
        
    }
    
    public int getCoolingStages()
    {
        int stage = 0;
        for (int i = 1; i < 8; i++)
        {
            int val = (int) SystemEquip.getInstance().getRelaySelection("relay" + i);
            if (val <= Stage.COOLING_5.getValue() && val > stage)
            {
                stage = val;
            }
        }
        return stage;
    }
    
    public int getHeatingStages()
    {
        int stage = 0;
        for (int i = 1; i < 8; i++)
        {
            int val = (int) SystemEquip.getInstance().getRelaySelection("relay" + i);
            System.out.println("relay"+i+" : val "+val+" Stage.HEATING_1.getValue() "+Stage.HEATING_1.getValue());
            if (val >= Stage.HEATING_1.getValue() && val <= Stage.HEATING_5.getValue() && val > stage)
            {
                stage = val;
            }
        }
        return stage != 0 ? stage - Stage.HEATING_1.ordinal() : stage ;
    }
    
    public int getFanStages()
    {
        int stage = 0;
        for (int i = 1; i < 8; i++)
        {
            int val = (int) SystemEquip.getInstance().getRelaySelection("relay" + i);
            System.out.print("relay"+i+" : val "+val);
            if (val >= Stage.FAN_1.getValue() && val > stage)
            {
                stage = val;
            }
        }
        return stage != 0 ? stage - Stage.FAN_1.ordinal() : stage ;
    }
}
