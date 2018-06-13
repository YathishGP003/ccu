package a75f.io.logic;

import android.util.Log;

import a75.io.algos.TrimResponseProcessor;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.VAVSystemProfile;
import a75f.io.bo.building.VavProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.hvac.VavUnit;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

/**
 * Created by samjithsadasivan on 6/1/18.
 */

public class LVAV
{
    public static void mapVAVCircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                      short nodeAddress, Zone zone, VavProfile vavProfile) {
    
        float desiredTemperature = 72;/*resolveZoneProfileLogicalValue(vavProfile);
        boolean occupied = desiredTemperature > 0;
        if (!occupied)
        {
            desiredTemperature = LZoneProfile.resolveAnyValue(vavProfile);
        }*/
        
        VavUnit vavControls = vavProfile.getVavControls(desiredTemperature);
        if (vavControls == null) {
            Log.d("VAV ","Vav controls do not exist");
            return;
        }
        
        if (L.ccu().systemProfile instanceof VAVSystemProfile)
        {
            VAVSystemProfile p = (VAVSystemProfile) L.ccu().systemProfile;
            p.updateSATRequest(vavProfile.getSATRequest());
            
            //TODO - TEMP Should be done right after creating a profile.
            TrimResponseProcessor tp = p.getSystemTRProcessor();
            if (!tp.trListeners.contains(vavProfile)) {
                tp.addTRListener(vavProfile);
                Log.d("VAV ","Add TR Listener");
            } else {
                Log.d("VAV ","TR Listener already added");
            }
            
            //TODO - TEMP for testing
            Log.d("VAV ","Update System SAT , New SAT "+p.getCurrentSAT());
            controlsMessage_t.controls.analogOut3.set((short)p.getCurrentSAT());
        }
        
        for (Output output : vavProfile.getProfileConfiguration(nodeAddress).getOutputs()) {
            if (output.getPort() == Port.ANALOG_OUT_ONE) {
                LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort())
                          .set(LSmartNode.mapRawValue(output, (short)vavControls.vavDamper.currentPosition));
            }
            
            if (output.getPort() == Port.ANALOG_OUT_TWO) {
               LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort())
                          .set(LSmartNode.mapRawValue(output, (short)vavControls.reheatValve.currentPosition));
            }
        }
        controlsMessage_t.controls.conditioningMode.set((short)vavProfile.getConditioningMode());
        Log.d("LVAV", " valve : "+vavControls.reheatValve.currentPosition+", damper : "+vavControls.vavDamper.currentPosition);
        
    }
    
    public static void mapVAVSeed(Zone zone, CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        //seedMessage.settings.profileBitmap.singleStageEquipment.set((short) 1);
    }
}
