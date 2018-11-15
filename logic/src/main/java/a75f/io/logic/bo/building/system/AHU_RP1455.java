package a75f.io.logic.bo.building.system;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import a75.io.algos.vav.VavTRSystem;


/**
 * RP1455 Compliant AHU_RP1455 control System Profile
 */
public class AHU_RP1455 extends SystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    public AHU_RP1455() {
        trSystem =  new VavTRSystem();
    }
    
    
    public  int getSystemSAT() {
        return ((VavTRSystem)trSystem).getCurrentSAT();
    }
    
    public  int getSystemCO2() {
        return ((VavTRSystem)trSystem).getCurrentCO2();
    }
    
    public  int getSystemOADamper() {
        return (((VavTRSystem)trSystem).getCurrentCO2() - CO2_MIN) * 100 / (CO2_MAX - CO2_MIN);
    }
    
    public int getStaticPressure() {
        return (int)((VavTRSystem)trSystem).getCurrentSp();
    }
    
    /*@JsonIgnore
    @Override
    public Struct getSystemControlMsg() {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        VavTRSystem tr = (VavTRSystem) trSystem;
        msg.analog0.set((short)tr.getCurrentSAT());
        msg.analog1.set((short)(tr.getCurrentSp()*10));
        //Modulate OA damper 0-100 by CO2 MIN-MAX
        int oaDamperPos = (tr.getCurrentCO2() - CO2_MIN) * 100 / (CO2_MAX - CO2_MIN);
        msg.analog2.set((short)oaDamperPos);
        return msg;
    }*/

}
