package a75f.io.logic;

/**
 * Created by samjithsadasivan on 6/1/18.
 */

public class LVAV
{
    /*public static void mapVAVCircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                      short nodeAddress, Zone zone, VavProfile vavProfile) {
        
        VavUnit vavControls = vavProfile.getVavControls(nodeAddress);
        if (vavControls == null) {
            Log.d("VAV ","Vav controls do not exist");
            return;
        }
        
        if (L.ccu().systemProfile.trSystem instanceof VavTRSystem)
        {
            VavTRSystem p = (VavTRSystem) L.ccu().systemProfile.trSystem;
            p.updateSATRequest(vavProfile.getSATRequest(nodeAddress));
            p.updateCO2Request(vavProfile.getCO2Requests(nodeAddress));
            p.updateSpRequest(vavProfile.getSpRequests(nodeAddress));
            p.updateHwstRequest(vavProfile.getHwstRequests(nodeAddress));
            
            //TODO - Should be done from VavProfile constructor.But that introduces cyclic dependency per current design.
            TrimResponseProcessor tpSAT = p.getSystemSATTRProcessor();
            if (!tpSAT.trListeners.contains(vavProfile.getSatResetListener())) {
                tpSAT.addTRListener(vavProfile.getSatResetListener());
                Log.d("VAV ","SAT TR Listener added");
            }
    
            TrimResponseProcessor tpCO2 = p.getSystemCO2TRProcessor();
            if (!tpCO2.trListeners.contains(vavProfile.getCO2ResetListener())) {
                tpCO2.addTRListener(vavProfile.getCO2ResetListener());
                Log.d("VAV ","CO2 TR Listener added");
            }
    
            TrimResponseProcessor tpSp = p.getSystemSpTRProcessor();
            if (!tpSp.trListeners.contains(vavProfile.getSpResetListener())) {
                tpSp.addTRListener(vavProfile.getSpResetListener());
                Log.d("VAV ","SP TR Listener added");
            }
    
            TrimResponseProcessor tpHwst = p.getSystemHwstTRProcessor();
            if (!tpHwst.trListeners.contains(vavProfile.getHwstResetListener())) {
                tpHwst.addTRListener(vavProfile.getHwstResetListener());
                Log.d("VAV ","SP TR Listener added");
            }
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
            
            if (output.getPort() == Port.RELAY_ONE) {
                switch (vavProfile.getProfileType()) {
                    case VAV_SERIES_FAN:
                        LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort())
                                  .set(output.mapDigital(((SeriesFanVavUnit)vavControls).fanStart));
                        break;
                    case VAV_PARALLEL_FAN:
                        LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort())
                                  .set(output.mapDigital(((ParallelFanVavUnit)vavControls).fanStart));
                        break;
                }
            }
            
        }
        controlsMessage_t.controls.conditioningMode.set((short)vavProfile.getConditioningMode());
        Log.d("LVAV", "Generate SN control, valve : "+vavControls.reheatValve.currentPosition+", damper : "+vavControls.vavDamper.currentPosition);
        
    }
    
    public static void mapVAVSeed(Zone zone, CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        //seedMessage.settings.profileBitmap.singleStageEquipment.set((short) 1);
    }*/
}
