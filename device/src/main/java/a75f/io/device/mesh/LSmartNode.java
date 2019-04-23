package a75f.io.device.mesh;

import android.util.Log;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
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
import a75f.io.device.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartNodeControls_t;
import a75f.io.device.serial.SmartNodeSettings_t;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemProfile;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.L.TAG_CCU_DEVICE;

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
        //retVal.addAll(getSeedMessages(zone));
        retVal.addAll(getControlMessages(zone));
        return retVal;
    }
    
    
    /***************************** SEED MESSAGES ****************************/
    
    public static ArrayList<CcuToCmOverUsbDatabaseSeedSnMessage_t> getSeedMessages(Zone zone, String equipRef)
    {
        ArrayList<CcuToCmOverUsbDatabaseSeedSnMessage_t> seedMessages = new ArrayList<>();
        int i = 0;
        for (Device d : HSUtil.getDevices(zone.getId()))
        {
            seedMessages.add(getSeedMessage(zone, Short.parseShort(d.getAddr()),equipRef));
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
                            Log.d(TAG_CCU_DEVICE, " Set " + p.getPort() + " type " + p.getType() + " logicalVal: " + logicalVal + " mappedVal " + mappedVal);
                            LSmartNode.getSmartNodePort(controlsMessage_t.controls, p.getPort()).set(mappedVal);
                            
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
    public static CcuToCmOverUsbSnControlsMessage_t getControlMessage(Zone zone, short node, String equipRef)
    {
        CcuToCmOverUsbSnControlsMessage_t controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
        controlsMessage_t.smartNodeAddress.set(node);
        controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
        fillSmartNodeControls(controlsMessage_t.controls,zone,node,equipRef);
        return controlsMessage_t;
    }
    
    public static CcuToCmOverUsbDatabaseSeedSnMessage_t getSeedMessage(Zone zone, short address,String equipRef)
    {
        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
        seedMessage.smartNodeAddress.set(address);
        seedMessage.putEncrptionKey(L.getEncryptionKey());
        fillSmartNodeSettings(seedMessage.settings,zone,address,equipRef);
        fillCurrentUpdatedTime(seedMessage.controls);
        fillSmartNodeControls(seedMessage.controls,zone,address,equipRef);
        return seedMessage;
    }
    public static CcuToCmOverUsbSnSettingsMessage_t getSettingsMessage(Zone zone, short address, String equipRef)
    {
        CcuToCmOverUsbSnSettingsMessage_t settingsMessage =
                new CcuToCmOverUsbSnSettingsMessage_t();
        settingsMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS);
        settingsMessage.smartNodeAddress.set(address);
        fillSmartNodeSettings(settingsMessage.settings,zone,address,equipRef);
        return settingsMessage;
    }
    private static void fillSmartNodeSettings(SmartNodeSettings_t settings,Zone zone, short address, String equipRef){

        settings.maxUserTem.set((short)getMaxUserTempLimits(equipRef));
        settings.minUserTemp.set((short)getMinUserTempLimits(equipRef));
        settings.maxDamperOpen.set((short)100);//TODO Default 100, need to change
        settings.minDamperOpen.set((short)40);//TODO Default 40, need to change
        settings.temperatureOffset.set((short)getTempOffset(address));
        //TODO need to update for diff profiles
        settings.profileBitmap.dynamicAirflowBalancing.set((short) 1);
        settings.roomName.set(zone.getDisplayName());
    }
    private static void fillSmartNodeControls(SmartNodeControls_t controls_t,Zone zone, short node, String equipRef){


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
                        controls_t.damperPosition.set((short)logicalVal);
                        mappedVal = 0;
                    }
                    Log.d(TAG_CCU_DEVICE, " Set " + p.getPort() + " type " + p.getType() + " logicalVal: " + logicalVal + " mappedVal " + mappedVal);
                    LSmartNode.getSmartNodePort(controls_t, p.getPort()).set(mappedVal);

                }
            }
            controls_t.setTemperature.set((short) (getDesiredTemp(node) * 2));
            if (L.ccu().systemProfile instanceof VavSystemProfile)
            {
                controls_t.conditioningMode.set((short) (VavSystemController.getInstance().getSystemState() == VavSystemController.State.HEATING ? 1 : 0));
            }
        }
    }

    public static CcuToCmOverUsbSnControlsMessage_t getCurrentTimeForControlMessage(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t)
    {
        fillCurrentUpdatedTime(controlsMessage_t.controls);
        return controlsMessage_t;
    }
    public static double getTempOffset(short addr) {
        try
        {
            return CCUHsApi.getInstance().readDefaultVal("point and zone and config and temperature and offset and group == \"" + addr + "\"");
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
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
        if (val < 0) {
            val = 0;//TODO-
        }
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
            default:
                String [] arrOfStr = type.split("-");
                if (arrOfStr.length == 2)
                {
                    int min = (int)Double.parseDouble(arrOfStr[0]);
                    int max = (int)Double.parseDouble(arrOfStr[1]);
                    if (max > min) {
                        return (short) (min * 10 + (max - min ) * 10 * val/100);
                    } else {
                        return (short) (min * 10 - (min - max ) * 10 * val/100);
                    }
                }
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
    
    
    public static Struct.Unsigned8 getSmartNodePort(SmartNodeControls_t controls,
                                                    String port)
    {
        switch (port)
        {
            case ANALOG_OUT_ONE:
                return controls.analogOut1;
            case ANALOG_OUT_TWO:
                return controls.analogOut2;
            case RELAY_ONE:
                return controls.digitalOut1;
            case RELAY_TWO:
                return controls.digitalOut2;
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
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and group == \""+node+"\"");
        if (point == null || point.size() == 0) {
            Log.d(TAG_CCU_DEVICE, " Desired Temp point does not exist for equip , sending 0");
            return 0;
        }
        return CCUHsApi.getInstance().readDefaultValById(point.get("id").toString());
    }


    public static int getCurrentDayOfWeekWithMondayAsStart() {
        Calendar calendar = GregorianCalendar.getInstance();
        switch (calendar.get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
        }
        return 0;
    }
    public static void fillCurrentUpdatedTime(SmartNodeControls_t controls){

        Calendar curDate = GregorianCalendar.getInstance();
        controls.time.day.set ((byte)(getCurrentDayOfWeekWithMondayAsStart() & 0xff));
        controls.time.hours.set((byte)(curDate.get(Calendar.HOUR_OF_DAY) & 0xff));
        controls.time.minutes.set((byte)(curDate.get(Calendar.MINUTE) & 0xff));
    }

    private static double getMaxUserTempLimits(String equipId){
        double deadband = TunerUtil.readTunerValByQuery("cooling and deadband", equipId);
       double maxCool =  TunerUtil.readTunerValByQuery("zone and cooling and user and limit and max",equipId);
       return maxCool- deadband;
    }

    private static double getMinUserTempLimits(String equipId){
        double deadband = TunerUtil.readTunerValByQuery("heating and deadband", equipId);
        double maxHeat =  TunerUtil.readTunerValByQuery("zone and heating and user and limit and max",equipId);
        return maxHeat+ deadband;
    }
    /********************************END SEED MESSAGES**************************************/
    
}
