package a75f.io.logic;

import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleMode;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.kinveybo.AlgoTuningParameters;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.LZoneProfile.resolveZoneProfileLogicalValue;

/**
 * Created by ryant on 10/10/2017.
 */

public class LLights
{
    public static void mapLightCircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                        short nodeAddress, Zone zone, ZoneProfile zp)
    {
        for (Output output : zp.getProfileConfiguration(nodeAddress).getOutputs())
        {
            short dimmablePercent = (short)resolveZoneProfileLogicalValue(zp, output);
            LSmartNode.getSmartNodePort(controlsMessage_t, output.getPort())
                      .set(LSmartNode.mapRawValue(output, dimmablePercent));
        }
    }


    public static void mapLightProfileSeed(Zone zone,
                                           CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        //If a light profile doesn't have a schedule applied to it.   Inject the system schedule.
        //Following, resolve the logical value for the output using the zone profile.
        //This will check if the circuit should release an override or not, or if the circuit has
        //a schedule.
        LightProfile lightProfile = (LightProfile) zone.findProfile(ProfileType.LIGHT);
        if (!lightProfile.hasSchedules())
        {
            lightProfile.addSchedules(ccu().getDefaultLightSchedule(), ScheduleMode.ZoneSchedule);
        }
        seedMessage.settings.lightingIntensityForOccupantDetected
                .set((short)(int) L.resolveTuningParameter(zone, AlgoTuningParameters.LightTuners
                                                                     .LIGHT_INTENSITY_OCCUPANT_DETECTED));
        seedMessage.settings.minLightingControlOverrideTimeInMinutes
                .set((short) (int)L.resolveTuningParameter(zone, AlgoTuningParameters.LightTuners
                                                                             .LIGHT_MIN_LIGHTING_CONTROL_OVERRIDE_IN_MINUTES));
        seedMessage.settings.profileBitmap.lightingControl.set((short) 1);
    }
}
