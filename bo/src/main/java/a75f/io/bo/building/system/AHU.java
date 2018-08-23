package a75f.io.bo.building.system;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.javolution.io.Struct;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.bo.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * RP1455 Compliant AHU control System Profile
 */
public class AHU extends SystemProfile
{
    public AHU() {
        trSystem =  new VavTRSystem();
    }
    
    @JsonIgnore
    @Override
    public Struct getSystemControlMsg() {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        VavTRSystem tr = (VavTRSystem) trSystem;
        msg.analog0.set((short)tr.getCurrentSAT());
        msg.analog1.set((short)(tr.getCurrentSp()*10));
        msg.analog2.set((short)tr.getCurrentCO2());
        return msg;
    }

}
