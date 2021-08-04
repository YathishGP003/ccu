package a75f.io.device.mesh;

import android.util.Log;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
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
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.L.TAG_CCU_DEVICE;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;

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
    
    public static ArrayList<CcuToCmOverUsbDatabaseSeedSnMessage_t> getSeedMessages(Zone zone, String equipRef,String profile)
    {
        ArrayList<CcuToCmOverUsbDatabaseSeedSnMessage_t> seedMessages = new ArrayList<>();
        int i = 0;
        for (Device d : HSUtil.getDevices(zone.getId()))
        {
            seedMessages.add(getSeedMessage(zone, Short.parseShort(d.getAddr()),equipRef,profile));
            i++;
        }
        return seedMessages;
    }
    
    
    /********************************CONTROLS MESSAGES*************************************/
    
    public static Collection<CcuToCmOverUsbSnControlsMessage_t> getControlMessages(Zone zone)
    {
        HashMap<Short, CcuToCmOverUsbSnControlsMessage_t> controlMessagesHash = new HashMap<>();
        
        for (Iterator<ZoneProfile> it = L.ccu().zoneProfiles.iterator(); it.hasNext();)
        {
            ZoneProfile zp = it.next();
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
                        if (opPoint.get("portEnabled").toString().equals("true"))
                        {
                            RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                            HashMap logicalOpPoint = hayStack.read("point and id == " + p.getPointRef());
                            if (logicalOpPoint.isEmpty()) {
                                CcuLog.d(TAG_CCU_DEVICE, " Logical point does not exist for "+opPoint.get("dis"));
                                continue;
                            }
                            double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());
                            short mappedVal = 0;
                            if (isEquipType("vav", node))
                            {
                                //IN case of vav , relay-2 maps to stage-2
                                mappedVal = (isAnalog(p.getPort()) ? mapAnalogOut(p.getType(), (short) logicalVal) : mapDigitalOut(p.getType(), p.getPort().equals(RELAY_TWO) ? logicalVal > 50 : logicalVal > 0));
                            } else {
                                mappedVal = (isAnalog(p.getPort()) ? mapAnalogOut(p.getType(), (short) logicalVal) : mapDigitalOut(p.getType(), logicalVal > 0));
                            }
                            if (!Globals.getInstance().isTemproryOverrideMode())
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
                    controlsMessage_t.controls.conditioningMode.set((short) (L.ccu().systemProfile.getSystemController().getSystemState() == HEATING ? 1 : 0));
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
    
    public static CcuToCmOverUsbDatabaseSeedSnMessage_t getSeedMessage(Zone zone, short address,String equipRef,String profile)
    {
        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
        seedMessage.smartNodeAddress.set(address);
        seedMessage.putEncrptionKey(L.getEncryptionKey());
        fillSmartNodeSettings(seedMessage.settings,zone,address,equipRef,profile);
        fillCurrentUpdatedTime(seedMessage.controls);
        fillSmartNodeControls(seedMessage.controls,zone,address,equipRef);
        return seedMessage;
    }
    public static CcuToCmOverUsbSnSettingsMessage_t getSettingsMessage(Zone zone, short address, String equipRef,String profile)
    {
        CcuToCmOverUsbSnSettingsMessage_t settingsMessage =
                new CcuToCmOverUsbSnSettingsMessage_t();
        settingsMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS);
        settingsMessage.smartNodeAddress.set(address);
        fillSmartNodeSettings(settingsMessage.settings,zone,address,equipRef, profile);
        return settingsMessage;
    }
    private static void fillSmartNodeSettings(SmartNodeSettings_t settings,Zone zone, short address, String equipRef,String profile){

        try
        {
            settings.maxUserTem.set((short) getMaxUserTempLimits(equipRef));
            settings.minUserTemp.set((short) getMinUserTempLimits(equipRef));
        } catch (Exception e) {
            //Equips not having user temps are bound to throw exception
            settings.maxUserTem.set((short) 75);
            settings.minUserTemp.set((short) 69);
        }
        
        if (getStatus(address) == ZoneState.HEATING.ordinal()) {
            settings.maxDamperOpen.set((short)getDamperLimit("heating", "max", address));
            settings.minDamperOpen.set((short)getDamperLimit("heating", "min", address));
        } else {
            settings.maxDamperOpen.set((short)getDamperLimit("cooling", "max", address));
            settings.minDamperOpen.set((short)getDamperLimit("cooling", "min", address));
        }
        
        settings.temperatureOffset.set((short)getTempOffset(address));
        
        if(profile == null)
            profile = "dab";
        switch (profile){
            case "dab":
                settings.profileBitmap.dynamicAirflowBalancing.set((short)1);
                break;
            case "lcm":
                settings.profileBitmap.lightingControl.set((short)1);
                break;
            case "oao":
                settings.profileBitmap.outsideAirOptimization.set((short)1);
                break;
            case "sse":
                settings.profileBitmap.singleStageEquipment.set((short)1);
                break;
            case "iftt":
                settings.profileBitmap.customControl.set((short)1);
                break;

        }
        settings.roomName.set(zone.getDisplayName());
        
        settings.forwardMotorBacklash.set((short)5);
        settings.reverseMotorBacklash.set((short)5);
        
        String equipId = SystemTemperatureUtil.getEquip(address).getId();
        try {
            settings.proportionalConstant.set((short)(TunerUtil.getProportionalGain(equipId) * 100));
            settings.integralConstant.set((short)(TunerUtil.getIntegralGain(equipId) * 100));
            settings.proportionalTemperatureRange.set((short)(TunerUtil.getProportionalSpread(equipId) * 10));
            settings.integrationTime.set((short)TunerUtil.getIntegralTimeout(equipId));
        } catch (Exception e) {
            //Equips not having PI tuners are bound to throw exception
            settings.proportionalConstant.set((short)50);
            settings.integralConstant.set((short)50);
            settings.proportionalTemperatureRange.set((short)15);
            settings.integrationTime.set((short)30);
        }
        
        settings.airflowHeatingTemperature.set((short)105);
        settings.airflowCoolingTemperature.set((short)60);
    
        settings.showCentigrade.set((short)0);
        settings.displayHold.set((short)0);
        settings.militaryTime.set((short)0);
        try
        {
            settings.enableOccupationDetection.set((short) getConfigNumVal("enable and occupancy", address));
        } catch (Exception e) {
            settings.enableOccupationDetection.set((short)0);
        }
    }
    private static void fillSmartNodeControls(SmartNodeControls_t controls_t,Zone zone, short node, String equipRef){


        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \""+node+"\"");

        if (device != null && device.size() > 0)
        {
            ArrayList<HashMap> physicalOpPoints= hayStack.readAll("point and physical and cmd and deviceRef == \""+device.get("id")+"\"");

            for (HashMap opPoint : physicalOpPoints)
            {
                if (opPoint.get("portEnabled").toString().equals("true"))
                {
                    RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                    HashMap logicalOpPoint = hayStack.read("point and id == " + p.getPointRef());
                    if (logicalOpPoint.isEmpty()) {
                        CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for "+p.getDisplayName());
                        continue;
                    }
                    double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());
    
                    short mappedVal = 0;
                    if (isEquipType("vav", node))
                    {
                        //In case of vav - series/paralle fan, relay-2 maps to fan
                        if (isEquipType("series", node) || isEquipType("parallel", node)) {
                            mappedVal = (isAnalog(p.getPort()) ? mapAnalogOut(p.getType(), (short) logicalVal) :
                                                                 mapDigitalOut(p.getType(), logicalVal > 0)
                            );
                        } else {
                            //In case of vav - no fan, relay-2 maps to stage-2
                            mappedVal = (isAnalog(p.getPort()) ? mapAnalogOut(p.getType(), (short) logicalVal) :
                                                                 mapDigitalOut(p.getType(), p.getPort().equals(RELAY_TWO) ?
                                                                                            logicalVal > 50 : logicalVal > 0)
                            );
                        }
                    }else if (isEquipType("sse", node))
                    {
                        //In case of sse , relay actuator maps to normally open by default
                        mappedVal = mapSSEDigitalOut(p.getType(), logicalVal > 0);
                    } else {
                        mappedVal = (isAnalog(p.getPort()) ? mapAnalogOut(p.getType(), (short) logicalVal) : mapDigitalOut(p.getType(), logicalVal > 0));
                    }
                    if (!Globals.getInstance().isTemproryOverrideMode())
                        hayStack.writeHisValById(p.getId(), (double) mappedVal);

                    if (isAnalog(p.getPort()) && p.getType().equals(PULSE) && logicalVal > 0) {
                        mappedVal |= 0x80;
                    }

                    if (isAnalog(p.getPort()) && p.getType().equals(MAT) && logicalVal > 0) {
                        controls_t.damperPosition.set((short)logicalVal);
                        mappedVal = 0;
                    }
                    Log.d(TAG_CCU_DEVICE, "Set "+logicalOpPoint.get("dis") +" "+ p.getPort() + " type " + p.getType() + " logicalVal: " + logicalVal + " mappedVal " + mappedVal);
                    LSmartNode.getSmartNodePort(controls_t, p.getPort()).set(mappedVal);

                }
            }
            controls_t.setTemperature.set((short) (getDesiredTemp(node) * 2));
            controls_t.conditioningMode.set((short) (L.ccu().systemProfile.getSystemController().getSystemState() == HEATING ? 1 : 0));
    
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
        val = (short)Math.min(val, 100);
        val = (short)Math.max(val, 0);
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
                    if (arrOfStr[1].contains("v")) {
                        arrOfStr[1] = arrOfStr[1].replace("v", "");
                    }
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
    public static short mapSSEDigitalOut(String type, boolean val)
    {

        switch (type)
        {
            case "Relay N/O":
                return (short) (val ? 1 : 0);
            ///Defaults to normally open
            case "Relay N/C":
                return (short) (val ? 0 : 1);
        }

        return 0;
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
        //return CCUHsApi.getInstance().readDefaultValById(point.get("id").toString());
        return CCUHsApi.getInstance().readPointPriorityVal(point.get("id").toString());
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
        double deadband = TunerUtil.readBuildingTunerValByQuery("cooling and deadband and base");
       double maxCool =  TunerUtil.readBuildingTunerValByQuery("zone and cooling and user and limit and max");
       return maxCool- deadband;
    }

    private static double getMinUserTempLimits(String equipId){
        double deadband = TunerUtil.readBuildingTunerValByQuery("heating and deadband and base");
        double maxHeat =  TunerUtil.readBuildingTunerValByQuery("zone and heating and user and limit and max");
        return maxHeat+ deadband;
    }
    
    public static double getDamperLimit(String coolHeat, String minMax, short nodeAddr)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and config and damper and pos and "+coolHeat+" and "+minMax+" and group == \""+nodeAddr+"\"");
        if (points.size() == 0) {
            Log.d("CCU","DamperLimit: Invalid point Send Default");
            return minMax.equals("max") ? 100 : 40 ;
        }
        String id = ((HashMap)points.get(0)).get("id").toString();
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    
    public static double getStatus(short nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == \""+nodeAddr+"\"");
    }
    
    public static double getConfigNumVal(String tags, short nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and "+tags+" and group == \""+nodeAddr+"\"");
    }
    
    public static boolean isEquipType(String type, short nodeAddr) {
        Equip q = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group ==\""+nodeAddr+"\"")).build();
        return q.getMarkers().contains(type);
    }
    
    /********************************END SEED MESSAGES**************************************/
    
}
