package a75f.io.logic;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.ScheduleMode;
import a75f.io.bo.serial.AddressedStruct;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.MessageType;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.LZoneProfile.getScheduledVal;
import static a75f.io.logic.LZoneProfile.resolveZoneProfileLogicalValue;

/**
 * Created by Yinten isOn 8/17/2017.
 */

class LSmartNode
{
    
    private static final short TODO = 0;
    
    
    public static short nextSmartNodeAddress()
    {
        short currentBand = ccu().getSmartNodeAddressBand();
        int amountOfNodes = 0;
        for (Floor floors : ccu().getFloors())
        {
            for (Zone zone : floors.mRoomList)
            {
                for (ZoneProfile zp : zone.mZoneProfiles)
                {
                    amountOfNodes += zp.getNodeAddresses().size();
                }
            }
        }
        return (short) (currentBand + amountOfNodes);
    }
    
    
    public static AddressedStruct[] getExtraMessages(Floor floor, Zone zone)
    {
        return new AddressedStruct[0];
    }
    
    
    /**************************** TEST MESSAGES ****************************/
    public static ArrayList<Struct> getTestMessages(Zone zone)
    {
        ArrayList<Struct> retVal = new ArrayList<>();
        retVal.addAll(getSeedMessages(zone));
        retVal.addAll(getControlMessages(zone));
        return retVal;
    }
    
    
    /***************************** SEED MESSAGES ****************************/
    
    public static ArrayList<CcuToCmOverUsbDatabaseSeedSnMessage_t> getSeedMessages(Zone zone)
    {
        //This needs to pair using module tuners as well..
        HashSet<Short> addresses = zone.getNodes();
        ArrayList<CcuToCmOverUsbDatabaseSeedSnMessage_t> seedMessages = new ArrayList<>();
        int i = 0;
        for (Short address : addresses)
        {
            seedMessages.add(getSeedMessage(zone, address));
            i++;
        }
        return seedMessages;
    }
    
    
    /********************************CONTROLS MESSAGES*************************************/
    
    public static Collection<CcuToCmOverUsbSnControlsMessage_t> getControlMessages(Zone zone)
    {
        HashMap<Short, CcuToCmOverUsbSnControlsMessage_t> controlMessagesHash = new HashMap<>();
        for (ZoneProfile zp : zone.mZoneProfiles)
        {
            for (short node : zp.getNodeAddresses())
            {
                CcuToCmOverUsbSnControlsMessage_t controlsMessage_t;
                if (controlMessagesHash.containsKey(node))
                {
                    controlsMessage_t = controlMessagesHash.get(node);
                }
                else
                {
                    controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
                    controlMessagesHash.put(node, controlsMessage_t);
                    controlsMessage_t.smartNodeAddress.set(node);
                    controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
                }
                for (Output output : zp.getProfileConfiguration(node).getOutputs())
                {
                    short outputMapped = 0;
                    switch (zp.getProfileType())
                    {
                        case LIGHT:
                            outputMapped = zp.isCircuitTest() ? output.getTestVal()
                                                   : mapLightCircuit(zone, zp, output);
                            break;
                        case SSE:
                            outputMapped = zp.isCircuitTest() ? output.getTestVal()
                                                   : mapLightCircuit(zone, zp, output);
                            break;
                        case GENERIC:
                            break;
                    }
                    getSmartNodePort(controlsMessage_t, output.getPort()).set(outputMapped);
                }
            }
        }
        return controlMessagesHash.values();
    }
    
    
    public static CcuToCmOverUsbDatabaseSeedSnMessage_t getSeedMessage(Zone zone, short address)
    {
        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
        seedMessage.settings.roomName.set(zone.roomName);
        seedMessage.smartNodeAddress.set(address);
        seedMessage.putEncrptionKey(L.getEncryptionKey());
        for (ZoneProfile zp : zone.mZoneProfiles)
        {
            switch (zp.getProfileType())
            {
                case LIGHT:
                    mapLightProfileSeed(zone, seedMessage);
                    break;
                case SSE:
                    break;
                case GENERIC:
                    break;
                case TEST:
                    mapLightProfileSeed(zone, seedMessage);
                    break;
            }
        }
        return seedMessage;
    }
    
    
    public static short mapLightCircuit(Zone zone, ZoneProfile zoneProfile, Output output)
    {
        short dimmablePercent = resolveZoneProfileLogicalValue(zoneProfile, output);
        //The smartnode circuit is in override mode, check to see if a schedule hasn't crossed a
        // bound.  If a schedule did cross a bound remove the override and continue.
        switch (output.getOutputType())
        {
            case Relay:
                return output.mapDigital(dimmablePercent != 0);
            case Analog:
                return output.mapAnalog(dimmablePercent);
        }
        return (short) 0;
    }
    
    
    private static Struct.Unsigned8 getSmartNodePort(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                                     Port smartNodePort)
    {
        switch (smartNodePort)
        {
            case ANALOG_OUT_ONE:
                return controlsMessage_t.controls.analogOut1;
            case ANALOG_OUT_TWO:
                return controlsMessage_t.controls.analogOut2;
            case RELAY_ONE:
                return controlsMessage_t.controls.digitalOut1;
            case RELAY_TWO:
                return controlsMessage_t.controls.digitalOut2;
            default:
                return null;
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
                .set((short) resolveTuningParameter(zone, "lightingIntensityOccupantDetected"));
        seedMessage.settings.minLightingControlOverrideTimeInMinutes
                .set((short) resolveTuningParameter(zone, "minLightControlOverInMinutes"));
        seedMessage.settings.profileBitmap.lightingControl.set((short) 1);
    }
    
    
    /********************************END SEED MESSAGES**************************************/
    
    private static Object resolveTuningParameter(Zone zone, String key)
    {
        if (zone.getTuningParameters().containsKey(key))
        {
            return zone.getTuningParameters().get(key);
        }
        else
        {
            return ccu().getDefaultCCUTuners().get(key);
        }
    }
    
    
    public static short mapSSECircuit(Zone zone, Output output)
    {
        short temperature =
                resolveZoneProfileLogicalValue(zone.findProfile(ProfileType.SSE), output);
        //The smartnode circuit is in override mode, check to see if a schedule hasn't crossed a
        // bound.  If a schedule did cross a bound remove the override and continue.
        switch (output.getOutputType())
        {
            case Relay:
                switch (output.mOutputRelayActuatorType)
                {
                    case NormallyClose:
                        switch (output.getPort())
                        {
                            case RELAY_ONE:
                                //this is the compressor
                                return TODO;
                            case RELAY_TWO:
                                //This is the fan port
                                return TODO;
                        }
                        ///Defaults to normally open
                    case NormallyOpen:
                        //Normal one
                        switch (output.getPort())
                        {
                            case RELAY_ONE:
                                //This is the compressor port
                                return TODO;
                            case RELAY_TWO:
                                //This is the fan port
                                return TODO;
                        }
                        return TODO;
                }
                break;
        }
        return (short) 0;
    }
    
    
    public static void mapSSEControls(Zone zone,
                                      CcuToCmOverUsbSnControlsMessage_t controlsMessage_t)
    {
        controlsMessage_t.controls.setTemperature
                .set((short) (getScheduledVal(zone.findProfile(ProfileType.SSE)) * 2));
    }
    
    
    public static void mapSSESeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        seedMessage.settings.profileBitmap.singleStageEquipment.set((short) 1);
    }
}
