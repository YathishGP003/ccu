package a75f.io.logic;

import android.util.Log;

import a75f.io.logic.bo.building.HmpProfile;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;

/**
 * Created by samjithsadasivan on 5/2/18.
 */

public class LHMP
{
    public static void mapHMPCircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                        short nodeAddress, Zone zone, HmpProfile hmpProfile) {
    
        Output output = hmpProfile.getProfileConfiguration(nodeAddress).getOutputs().get(0); // HMP just has one port configured.
    
        short valveControl = (short)Math.round(hmpProfile.getUpdatedHmpValvePosition());
        Log.d("LHMP", "mapHMPCircuits "+valveControl);
        LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort())
                  .set(LSmartNode.mapRawValue(output, valveControl));
        
    }
}
