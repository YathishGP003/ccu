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
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Zone;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatSettingsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartStatConditioningMode_t;
import a75f.io.device.serial.SmartStatControls_t;
import a75f.io.device.serial.SmartStatFanSpeed_t;
import a75f.io.device.serial.SmartStatProfileMap_t;
import a75f.io.device.serial.SmartStatSettings_t;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.StandaloneTunerUtil;

import static a75f.io.logic.L.TAG_CCU_DEVICE;

/**
 * Created by Anilkumar isOn 1/10/2019.
 */
public class LSmartStat {

    private static final String TAG  = "LSmartStat";

    /***************************** SEED MESSAGES ****************************/

    public static ArrayList<CcuToCmOverUsbDatabaseSeedSmartStatMessage_t> getSeedMessages(Zone zone,String equipId,String profile)
    {
        ArrayList<CcuToCmOverUsbDatabaseSeedSmartStatMessage_t> seedMessages = new ArrayList<>();
        int i = 0;
        for (Device d : HSUtil.getDevices(zone.getId()))
        {
            seedMessages.add(getSeedMessage(zone, Short.parseShort(d.getAddr()),equipId,profile));
            i++;
        }
        return seedMessages;
    }


    /********************************CONTROLS MESSAGES*************************************/

    public static Collection<CcuToCmOverUsbSmartStatControlsMessage_t> getControlMessages(Zone zone)
    {
        HashMap<Short, CcuToCmOverUsbSmartStatControlsMessage_t> controlMessagesHash = new HashMap<>();
        
        for (Iterator<ZoneProfile> it = L.ccu().zoneProfiles.iterator(); it.hasNext();)
        {
            ZoneProfile zp = it.next();
            if(zp.getProfileType().name().startsWith("SMARTSTAT")) {
                for (short node : zp.getNodeAddresses()) {
                    CcuToCmOverUsbSmartStatControlsMessage_t controlsMessage_t;
                    if (controlMessagesHash.containsKey(node)) {
                        controlsMessage_t = controlMessagesHash.get(node);
                    } else {
                        controlsMessage_t = new CcuToCmOverUsbSmartStatControlsMessage_t();
                        controlMessagesHash.put(node, controlsMessage_t);
                        controlsMessage_t.address.set(node);
                        controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
                    }

                    CCUHsApi hayStack = CCUHsApi.getInstance();
                    HashMap device = hayStack.read("device and addr == \"" + node + "\"");
                    controlsMessage_t.controls.setTemperature.set((short) 144); //for Smartstat we always send desired temp as fixed value.
                    controlsMessage_t.controls.fanSpeed.set(SmartStatFanSpeed_t.values()[(int) getOperationalMode("fan",zp.getEquip().getId())]);
                    controlsMessage_t.controls.conditioningMode.set(SmartStatConditioningMode_t.values()[(int) getOperationalMode("temp",zp.getEquip().getId())]);
                    if (device != null && device.size() > 0) {
                        ArrayList<HashMap> physicalOpPoints = hayStack.readAll("point and physical and cmd and deviceRef == \"" + device.get("id") + "\"");
                        for (HashMap opPoint : physicalOpPoints) {
                            if (opPoint.get("enabled").toString().equals("true")) {
                                RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                                HashMap logicalOpPoint = hayStack.read("point and id == " + p.getPointRef());
                                if (logicalOpPoint.get("id") != null) {
                                    double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());
                                    short mappedVal = (mapDigitalOut(p.getType(), logicalVal > 0));
                                    hayStack.writeHisValById(p.getId(), (double) mappedVal);

                                    LSmartStat.getSmartStatPort(controlsMessage_t.controls, p.getPort()).set(mappedVal);
                                }

                            }
                        }
                    }
                }
            }
        }
        return controlMessagesHash.values();
    }
    public static CcuToCmOverUsbSmartStatControlsMessage_t getControlMessage(Zone zone,short node, String equipId)
    {
            CcuToCmOverUsbSmartStatControlsMessage_t controlsMessage_t = new CcuToCmOverUsbSmartStatControlsMessage_t();
            controlsMessage_t.address.set(node);
            controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
            fillSmartStatControls(controlsMessage_t.controls,equipId,node);
            return controlsMessage_t;
    }
    public static CcuToCmOverUsbSmartStatControlsMessage_t getCurrentTimeForControlMessage(CcuToCmOverUsbSmartStatControlsMessage_t controlsMessage_t)
    {
        fillCurrentUpdatedTime(controlsMessage_t.controls);
        return controlsMessage_t;
    }
    public static CcuToCmOverUsbDatabaseSeedSmartStatMessage_t getSeedMessage(Zone zone, short address,String equipId,String profile)
    {
        CcuToCmOverUsbDatabaseSeedSmartStatMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedSmartStatMessage_t();
        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SMART_STAT);
        seedMessage.address.set(address);
        try {
            seedMessage.putEncrptionKey(L.getEncryptionKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
        fillSmartStatSettings(seedMessage.settings,equipId,address,zone,profile);
        fillCurrentUpdatedTime(seedMessage.controls);
        fillSmartStatControls(seedMessage.controls,equipId,address);
        return seedMessage;
    }
    public static CcuToCmOverUsbSmartStatSettingsMessage_t getSettingsMessage(Zone zone, short address,String equipRef,String profile)
    {

        CcuToCmOverUsbSmartStatSettingsMessage_t settingsMessage =
                new CcuToCmOverUsbSmartStatSettingsMessage_t();
        settingsMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_SETTINGS);
        settingsMessage.address.set(address);
        fillSmartStatSettings(settingsMessage.settings,equipRef,address,zone,profile);
        return settingsMessage;
    }
    private static void fillSmartStatSettings(SmartStatSettings_t settings_t, String equipId, short address, Zone zone,String profile){

        settings_t.roomName.set(zone.getDisplayName());
        if(profile == null)
            profile = "cpu";
        switch (profile){
            case "cpu":
                settings_t.profileBitmap.set(SmartStatProfileMap_t.CONVENTIONAL_PACKAGE_UNIT);
                break;
            case "compu":
                settings_t.profileBitmap.set(SmartStatProfileMap_t.COMMERCIAL_PACKAGE_UNIT);
                break;
            case "hpu":
                settings_t.profileBitmap.set(SmartStatProfileMap_t.HEAT_PUMP_UNIT);
                break;
            case "2pfcu":
                settings_t.profileBitmap.set(SmartStatProfileMap_t.PIPE_FAN_COIL_UNI_2);
                break;
            case "4pfcu":
                settings_t.profileBitmap.set(SmartStatProfileMap_t.PIPE_FAN_COIL_UNIT_4);
                break;

        }
        double hdb = StandaloneTunerUtil.getStandaloneHeatingDeadband(equipId);
        double cdb = StandaloneTunerUtil.getStandaloneCoolingDeadband(equipId);
        settings_t.minUserTemp.set((short)getMinUserTempLimits(equipId, hdb));
        settings_t.maxUserTemp.set((short)getMaxUserTempLimits(equipId, cdb));
        settings_t.temperatureOffset.set((byte)getTempOffset(address));
        try {
            Log.d("LSmartStat","sch status="+equipId+","+zone.getId());
            Occupied occuStatus = ScheduleProcessJob.getOccupiedModeCache(zone.getId());
            if(occuStatus != null)
            Log.d("LSmartStat","sch status22="+occuStatus.getCoolingVal()+","+occuStatus.getHeatingVal()+","+occuStatus.getHeatingDeadBand()+","+occuStatus.getCoolingDeadBand());
            settings_t.heatingDeadBand.set((short) (occuStatus.getHeatingDeadBand() * 10)); //Send in multiples of 10
            settings_t.coolingDeadBand.set((short) (occuStatus.getCoolingDeadBand() * 10));
        }catch (Exception e){
            settings_t.heatingDeadBand.set((short)20);//default deadband is 2.0, sending in multiples
            settings_t.coolingDeadBand.set((short)20);
        }
        //TODO need to set current occupied times slots here // ANILK
		settings_t.holdTimeInMinutes.set((short)0);
        settings_t.changeToOccupiedTime.set((short)0);
        settings_t.changeToUnoccupiedTime.set((short)0);
        settings_t.lightingIntensityForOccupantDetected.set((short)0);


        settings_t.enabledRelaysBitmap.relay1.set(getConfigEnabled(Port.RELAY_ONE.name(),address));
        settings_t.enabledRelaysBitmap.relay2.set(getConfigEnabled(Port.RELAY_TWO.name(),address));
        settings_t.enabledRelaysBitmap.relay3.set(getConfigEnabled(Port.RELAY_THREE.name(),address));
        settings_t.enabledRelaysBitmap.relay4.set(getConfigEnabled(Port.RELAY_FOUR.name(),address));
        settings_t.enabledRelaysBitmap.relay5.set(getConfigEnabled(Port.RELAY_FIVE.name(),address));
        settings_t.enabledRelaysBitmap.relay6.set(getConfigEnabled(Port.RELAY_SIX.name(),address));
        settings_t.otherBitMaps.centigrade.set((short)0);
        settings_t.otherBitMaps.occupancySensor.set((byte)getOccupancyEnable(address));
        settings_t.otherBitMaps.heatPumpUnitChangeOverB.set((short)0);
        settings_t.otherBitMaps.enableExternal10kTempSensor.set(getConfigEnabled(Port.TH2_IN.name(),address));
        settings_t.otherBitMaps.enableBeaconing.set((short)0);
    }
    private static void fillSmartStatControls(SmartStatControls_t controls, String equipId, short node){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \"" + node + "\"");
        controls.setTemperature.set((short)(getDesiredTemp(node) * 2)); //for Smartstat we always send desired temp as fixed values??? doubts over here? kumar
        controls.fanSpeed.set(SmartStatFanSpeed_t.values()[(int) getOperationalMode("fan",equipId)]);
        controls.conditioningMode.set(SmartStatConditioningMode_t.values()[(int) getOperationalMode("temp",equipId)]);
        if (device != null && device.size() > 0) {
            ArrayList<HashMap> physicalOpPoints = hayStack.readAll("point and physical and cmd and deviceRef == \"" + device.get("id") + "\"");
            for (HashMap opPoint : physicalOpPoints) {
                if (opPoint.get("enabled").toString().equals("true")) {
                    RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                    HashMap logicalOpPoint = hayStack.read("point and id == " + p.getPointRef());
                    Log.d("LSmartStat", "getCtrlMsgs=" + p.getDisplayName() + "," + p.getPointRef() + "," + logicalOpPoint.get("id") + "," + p.getType());
                    if (logicalOpPoint.get("id") != null) {
                        double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());
                        short mappedVal = (mapDigitalOut(p.getType(), logicalVal > 0));
                        hayStack.writeHisValById(p.getId(), (double) mappedVal);

                        LSmartStat.getSmartStatPort(controls, p.getPort()).set(mappedVal);
                    }

                }
            }
        }
    }
    public static double getTempOffset(short addr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and temperature and offset and group == \""+addr+"\"");
    }

    public static double getOccupancyEnable(short addr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and occupancy and enable and group == \""+addr+"\"");
    }

    public static double getOperationalMode(String cmd, String equipRef){

        return CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and operation and mode and his and "+cmd+" and equipRef== \"" + equipRef + "\"");
    }

    private static short getConfigEnabled(String relays, short address){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \"" + address + "\"");
        if (device != null && device.size() > 0) {
            ArrayList<HashMap> physicalOpPoints = hayStack.readAll("point and physical and deviceRef == \"" + device.get("id") + "\" and port == \""+relays+"\"");
            for (HashMap opPoint : physicalOpPoints) {
                if (opPoint.get("enabled").toString().equals("true")) {
                    return 1;

                }else
                    return 0;
            }
        }
        return 0;

    }
    public static short mapDigitalOut(String type, boolean val)
    {

        switch (type)
        {
            case "Relay N/O":
                return (short) (val ? 1 : 0);
            case "Relay N/C":
                return (short) (val ? 0 : 1);
        }

        return 0;
    }
    public static Struct.Unsigned8 getSmartStatPort(SmartStatControls_t controlsMessage_t,
                                                    String port)
    {
        switch (Port.valueOf(port))
        {
            case RELAY_ONE:
                return controlsMessage_t.relay1;
            case RELAY_TWO:
                return controlsMessage_t.relay2;
            case RELAY_THREE:
                return controlsMessage_t.relay3;
            case RELAY_FOUR:
                return controlsMessage_t.relay4;
            case RELAY_FIVE:
                return controlsMessage_t.relay5;
            case RELAY_SIX:
                return controlsMessage_t.relay6;
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

    public static void fillCurrentUpdatedTime(SmartStatControls_t controls){

        Calendar curDate = GregorianCalendar.getInstance();
        controls.time.day.set ((byte)(getCurrentDayOfWeekWithMondayAsStart() & 0xff));
        controls.time.hours.set((byte)(curDate.get(Calendar.HOUR_OF_DAY) & 0xff));
        controls.time.minutes.set((byte)(curDate.get(Calendar.MINUTE) & 0xff));
    }



    private static double getMaxUserTempLimits(String equipId, double deadband){
        double maxCool =  StandaloneTunerUtil.readTunerValByQuery("zone and cooling and user and limit and max",equipId);
        return maxCool- deadband;
    }

    private static double getMinUserTempLimits(String equipId, double deadband){
        double maxHeat =  StandaloneTunerUtil.readTunerValByQuery("zone and heating and user and limit and max",equipId);
        return maxHeat+ deadband;
    }
}
