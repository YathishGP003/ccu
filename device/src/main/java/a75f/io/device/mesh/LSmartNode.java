package a75f.io.device.mesh;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.serial.AddressedStruct;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public class LSmartNode
{
    
    private static final short  TODO = 0;
    private static final String TAG  = "LSmartNode";
    
    
    public static short nextSmartNodeAddress()
    {
        short currentBand = L.ccu().getSmartNodeAddressBand();
        int amountOfNodes = 0;
        for (Floor floors : L.ccu().getFloors())
        {
            for (Zone zone : floors.mZoneList)
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
            //zp.updateZonePoints();
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
    
                CCUHsApi hayStack = CCUHsApi.getInstance();
                HashMap device = hayStack.read("device and addr == \""+node+"\"");
                if (device != null && device.size() > 0)
                {
                    ArrayList<HashMap> physicalOpPoints= hayStack.readAll("point and physical and output and deviceRef == \""+device.get("id")+"\"");
                    
                    for (HashMap opPoint : physicalOpPoints) {
                        HashMap logicalOpPoint = hayStack.read("point and id == "+opPoint.get("pointRef"));
                        double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());
                        
                        String port = opPoint.get("port").toString();
    
                        short mappedVal = (isAnalog(port) ? mapAnalogOut(opPoint.get("type").toString(), (short)logicalVal)
                                                        : mapDigitalOut(opPoint.get("type").toString(), logicalVal > 0));
                        hayStack.writeHisValById(opPoint.get("id").toString(), (double)mappedVal);
    
                        LSmartNode.getSmartNodePort(controlsMessage_t, port)
                                  .set(mappedVal);
                        
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
                /*case ProfileType.LIGHT:
                    Log.i(TAG, "Mapping Light Profile Seed messages");
                    LLights.mapLightProfileSeed(zone, seedMessage);
                    break;
                case ProfileType.SSE:
                    Log.i(TAG, "Mapping SSE Profile Seed messages");
                    LSSE.mapSSESeed(zone, seedMessage);
                    break;
                case ProfileType.VAV_REHEAT:
                case ProfileType.VAV_SERIES_FAN:
                case ProfileType.VAV_PARALLEL_FAN:
                    Log.i(TAG, "Mapping VAV Profile Seed messages");
                    LVAV.mapVAVSeed(zone, seedMessage);
                    break;
                case ProfileType.TEST:
                    Log.i(TAG, "Mapping TEST Profile Seed messages");
                    LTest.mapTestProfileSeed(zone, seedMessage);
                    break;*/
            }
        }
        seedMessage.settings.profileBitmap.lightingControl.set((short) 1);
        return seedMessage;
    }
    
    
    private static void mapTestCircuits(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                        short nodeAddress, ZoneProfile zp)
    {
        /*for (Output output : zp.getProfileConfiguration(nodeAddress).getOutputs())
        {
            short outputMapped = output.getTestVal();
            getSmartNodePort(controlsMessage_t, output.getPort()).set(outputMapped);
        }*/
    }
    
    public static boolean isAnalog(String port) {
        switch (port) {
            case "ANALOG_OUT_ONE":
            case "ANALOG_OUT_TWO":
            case "ANALOG_IN_ONE":
            case "ANALOG_IN_TWO":
                return true;
        }
        return false;
    }
    
    public static short mapAnalogOut(String type, short val) {
        switch (type)
        {
            case "0-10v":
                return val;
            case "10-0v":
                return (short) (100 - val);
            case "2-10v":
                return (short) (20 + scaleAnalog(val, 80));
            case "10-2v":
                return (short) (100 - scaleAnalog(val, 80));
        }
        return (short) 0;
    }
    
    public static short mapDigitalOut(String type, boolean val)
    {
        
        switch (type)
        {
            case "NC":
                return (short) (val ? 0 : 1);
            ///Defaults to normally open
            case "NO":
                return (short) (val ? 1 : 0);
        }
        
        return 0;
    }
    
    protected static short scaleAnalog(short analog, int scale)
    {
        return (short) ((float) scale * ((float) analog / 100.0f));
    }
    
    
    public static Struct.Unsigned8 getSmartNodePort(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                                    String port)
    {
        switch (port)
        {
            case "ANALOG_OUT_ONE":
                return controlsMessage_t.controls.analogOut1;
            case "ANALOG_OUT_TWO":
                return controlsMessage_t.controls.analogOut2;
            case "RELAY_ONE":
                return controlsMessage_t.controls.digitalOut1;
            case "RELAY_TWO":
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
