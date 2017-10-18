package a75f.io.logic;

import a75f.io.bo.building.Output;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

import static a75f.io.logic.LZoneProfile.resolveZoneProfileLogicalValue;

/**
 * Created by Yinten on 10/18/2017.
 */

public class LTest
{
    public static void mapLightCircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                        short nodeAddress, Zone zone, ZoneProfile zp)
    {
        for (Output output : zp.getProfileConfiguration(nodeAddress).getOutputs())
        {
            short dimmablePercent = resolveZoneProfileLogicalValue(zp, output);
            LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort())
                      .set(LSmartNode.mapRawValue(output, dimmablePercent));
        }
    }
    
    
    public static void mapTestProfileSeed(Zone zone,
                                           CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        //If a light profile doesn't have a schedule applied to it.   Inject the system schedule.
        //Following, resolve the logical value for the output using the zone profile.
        //This will check if the circuit should release an override or not, or if the circuit has
        //a schedule.
        
    }
}
