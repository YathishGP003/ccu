package a75f.io.logic;

import android.util.Log;

import a75f.io.bo.building.BaseProfileConfiguration;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.SingleStageProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.SingleStageMode;
import a75f.io.bo.building.sse.SingleStageLogicalMap;
import a75f.io.bo.kinvey.AlgoTuningParameters;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

import static a75f.io.logic.LZoneProfile.resolveZoneProfileLogicalValue;

/**
 * Created by ryant on 10/10/2017.
 */

public class LSSE
{
    
    private static final String TAG = "LSSE";
    
    
    public static void mapSSECircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                      short nodeAddress, Zone zone, SingleStageProfile zp)
    {
        float desiredTemperature = resolveZoneProfileLogicalValue(zp);
        boolean occupied = desiredTemperature > 0;
        if (!occupied)
        {
            desiredTemperature = LZoneProfile.resolveAnyValue(zp);
        }
        SingleStageLogicalMap logicalMap = zp.getLogicalMap().get(Short.valueOf(nodeAddress));
        BaseProfileConfiguration circuitConfig = zp.getProfileConfiguration(nodeAddress);
        float currentTemperature = logicalMap.getRoomTemperature();
        //Tuners
        int coolingDeadband =
                (int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_COOLING_DEADBAND);
        Log.i(TAG, "Cooling Deadband:" + coolingDeadband); 
        int heatingDeadband =
                (int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_HEATING_DEADBAND);
        Log.i(TAG, "Heating Deadband:" + heatingDeadband);
        int buildingMaxTemp =
                (int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_BUILDING_MAX_TEMP);
        int buildingMinTemp =
                (int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_BUILDING_MIN_TEMP);
        int userMaxTemp =
                (int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_USER_MAX_TEMP);
        int userMinTemp =
                (int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_USER_MIN_TEMP);
        int zoneSetBack =
                (int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_USER_ZONE_SETBACK);
        boolean coolingOn = false;
        boolean fanOn = false;
        boolean heatingOn = false;
        if (occupied) // occupied
        {
            if (currentTemperature > (desiredTemperature + coolingDeadband))
            {
                coolingOn = true;
                fanOn = true;
                //turn on cooling & fan
            }
            else if (currentTemperature < (desiredTemperature - heatingDeadband))
            {
                heatingOn = true;
                fanOn = true;
            }
            //Fan always on for occupied mode.
            fanOn = true;
        }
        else // not occupied
        {
            if (currentTemperature > (desiredTemperature + zoneSetBack))
            {
                coolingOn = true;
                fanOn = true;
            }
            else if (currentTemperature < (desiredTemperature - zoneSetBack))
            {
                heatingOn = true;
                fanOn = true;
            }
        }
        for (Output output : zp.getProfileConfiguration(nodeAddress).getOutputs())
        {
            short circuitMapped = 0;  // off.
            SingleStageMode singleStageComponent = logicalMap.getLogicalMap().get(output.getPort());
            if (singleStageComponent == SingleStageMode.COOLING && coolingOn)
            {
                circuitMapped = output.mapDigital(true);
                controlsMessage_t.controls.conditioningMode.set((short) 0); /* 1 is heating, 0 is cooling - sent from CCU to Smart Node for display indication */
            }
            else if (singleStageComponent == SingleStageMode.HEATING && heatingOn)
            {
                circuitMapped = output.mapDigital(true);
                controlsMessage_t.controls.conditioningMode.set((short) 1); /* 1 is heating, 0 is cooling - sent from CCU to Smart Node for display indication */
            }
            else if (singleStageComponent == SingleStageMode.FAN && fanOn)
            {
                circuitMapped = output.mapDigital(true);
            }
            LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort()).set(circuitMapped);
        }
    }
    
    
    public static void mapSSESeed(Zone zone, CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        seedMessage.settings.profileBitmap.singleStageEquipment.set((short) 1);
    }
}
