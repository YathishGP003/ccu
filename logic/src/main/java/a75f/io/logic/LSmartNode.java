package a75f.io.logic;

import android.util.Log;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.HmpProfile;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.SingleStageProfile;
import a75f.io.bo.building.VavProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.serial.AddressedStruct;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.MessageType;

import static a75f.io.logic.L.ccu;

/**
 * Created by Yinten isOn 8/17/2017.
 */

class LSmartNode
{
    
    private static final short  TODO = 0;
    private static final String TAG  = "LSmartNode";
    
    
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
            zp.updateZoneControls(getDesiredVal(zp));
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
                short outputMapped = 0;
                if (zp.isCircuitTest())
                {
                    mapTestCircuits(controlsMessage_t, node, zp);
                }
                else
                {
                    switch (zp.getProfileType())
                    {
                        case LIGHT:
                            Log.i(TAG, "Mapping Light control messages");
                            LLights.mapLightCircuits(controlsMessage_t, node, zone, zp);
                            break;
                        case SSE:
                            Log.i(TAG, "Mapping SSE control messages");
                            LSSE.mapSSECircuits(controlsMessage_t, node, zone, (SingleStageProfile) zp);
                            break;
                        case HMP:
                            Log.i(TAG, "Mapping HMP control messages");
                            LHMP.mapHMPCircuits(controlsMessage_t, node, zone, (HmpProfile)zp);
                            break;
                        case VAV:
                            Log.i(TAG, "Mapping VAV control messages");
                            LVAV.mapVAVCircuits(controlsMessage_t, node, zone, (VavProfile) zp);
                            break;
                    }
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
                    Log.i(TAG, "Mapping Light Profile Seed messages");
                    LLights.mapLightProfileSeed(zone, seedMessage);
                    break;
                case SSE:
                    Log.i(TAG, "Mapping SSE Profile Seed messages");
                    LSSE.mapSSESeed(zone, seedMessage);
                    break;
                case VAV:
                    Log.i(TAG, "Mapping VAV Profile Seed messages");
                    LVAV.mapVAVSeed(zone, seedMessage);
                    break;
                case TEST:
                    Log.i(TAG, "Mapping TEST Profile Seed messages");
                    LTest.mapTestProfileSeed(zone, seedMessage);
                    break;
            }
        }
        return seedMessage;
    }
    
    
    private static void mapTestCircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                        short nodeAddress, ZoneProfile zp)
    {
        for (Output output : zp.getProfileConfiguration(nodeAddress).getOutputs())
        {
            short outputMapped = output.getTestVal();
            getSmartNodePort(controlsMessage_t, output.getPort()).set(outputMapped);
        }
    }
    
    
    public static Struct.Unsigned8 getSmartNodePort(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
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
    
    
    public static short mapRawValue(Output output, short rawValue)
    {
        switch (output.getOutputType())
        {
            case Relay:
                return output.mapDigital(rawValue != 0);
            case Analog:
                return output.mapAnalog(rawValue);
        }
        return 0;
    }
    
    public static double getDesiredVal(ZoneProfile z) {
        //TODO- TEMP
        return 72.0;
        /*float desiredTemperature = LZoneProfile.resolveZoneProfileLogicalValue(z);
        boolean occupied = desiredTemperature > 0;
        if (!occupied)
        {
            desiredTemperature = LZoneProfile.resolveAnyValue(z);
        }
        return desiredTemperature;*/
    }
    
    /********************************END SEED MESSAGES**************************************/
    
}
