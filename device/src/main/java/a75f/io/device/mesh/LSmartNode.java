package a75f.io.device.mesh;

import android.util.Log;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Zone;
import a75f.io.device.serial.AddressedStruct;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemProfile;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public class LSmartNode
{
    public static final String ANALOG_OUT_ONE = "ANALOG_OUT_ONE";
    public static final String ANALOG_OUT_TWO = "ANALOG_OUT_TWO";
    public static final String ANALOG_IN_ONE = "ANALOG_IN_ONE";
    public static final String ANALOG_IN_TWO = "ANALOG_IN_TWO";
    
    public static final String RELAY_ONE ="RELAY_ONE";
    public static final String RELAY_TWO ="RELAY_TWO";
    public static final String PULSE ="Pulsed Electric";
    public static final String MAT ="MAT";
    
    
    private static final short  TODO = 0;
    private static final String TAG  = "LSmartNode";
    
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
        ArrayList<CcuToCmOverUsbDatabaseSeedSnMessage_t> seedMessages = new ArrayList<>();
        int i = 0;
        for (Device d : HSUtil.getDevices(zone.getId()))
        {
            seedMessages.add(getSeedMessage(zone, Short.parseShort(d.getAddr())));
            i++;
        }
        return seedMessages;
    }
    
    
    /********************************CONTROLS MESSAGES*************************************/
    
    public static Collection<CcuToCmOverUsbSnControlsMessage_t> getControlMessages(Zone zone)
    {
        HashMap<Short, CcuToCmOverUsbSnControlsMessage_t> controlMessagesHash = new HashMap<>();
        for (ZoneProfile zp : L.ccu().zoneProfiles)
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
                    ArrayList<HashMap> physicalOpPoints= hayStack.readAll("point and physical and cmd and deviceRef == \""+device.get("id")+"\"");
                    
                    for (HashMap opPoint : physicalOpPoints)
                    {
                        if (opPoint.get("enabled").toString().equals("true"))
                        {
                            RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                            HashMap logicalOpPoint = hayStack.read("point and id == " + p.getPointRef());
                            double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());
                            
                            //TODO - Assuming Relay1 & Relay 2 are enabled for staged out put.
                            short mappedVal = (isAnalog(p.getPort()) ? mapAnalogOut(p.getType(), (short) logicalVal) : mapDigitalOut(p.getType(),
                                            p.getPort().equals(RELAY_TWO) ? logicalVal > 50 : logicalVal > 0));
                            hayStack.writeHisValById(p.getId(), (double) mappedVal);
                            
                            if (isAnalog(p.getPort()) && p.getType().equals(PULSE) && logicalVal > 0) {
                                mappedVal |= 0x80;
                            }
    
                            if (isAnalog(p.getPort()) && p.getType().equals(MAT) && logicalVal > 0) {
                                controlsMessage_t.controls.damperPosition.set(mappedVal);
                                mappedVal = 0;
                            }
                            Log.d("CCU"," Set "+p.getPort()+" type "+p.getType()+" logicalVal: "+logicalVal+ " mappedVal "+mappedVal);
                            LSmartNode.getSmartNodePort(controlsMessage_t, p.getPort()).set(mappedVal);
                            
                        }
                    }
                    controlsMessage_t.controls.setTemperature.set((short) (getDesiredTemp(node) * 2));
                    if (L.ccu().systemProfile instanceof VavSystemProfile)
                    {
                        controlsMessage_t.controls.conditioningMode.set((short) (VavSystemController.getInstance().getSystemState() == VavSystemController.State.HEATING ? 1 : 0));
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
        seedMessage.settings.roomName.set(zone.getDisplayName());
        seedMessage.smartNodeAddress.set(address);
        seedMessage.putEncrptionKey(L.getEncryptionKey());
        //TODO-TEST
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
            case ANALOG_OUT_ONE:
            case ANALOG_OUT_TWO:
            case ANALOG_IN_ONE:
            case ANALOG_IN_TWO:
                return true;
        }
        return false;
    }
    
    public static short mapAnalogOut(String type, short val) {
        switch (type)
        {
            case "0-10v":
            case PULSE:
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
            case "Relay N/O":
                return (short) (val ? 0 : 1);
            ///Defaults to normally open
            case "Relay N/C":
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
    
    public static double getDesiredTemp(short node)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \""+node+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    
    /********************************END SEED MESSAGES**************************************/
    
}
