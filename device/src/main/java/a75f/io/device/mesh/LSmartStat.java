package a75f.io.device.mesh;


import android.util.Log;

import java.util.ArrayList;

import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;

import org.javolution.io.Struct;
import java.util.Collection;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Zone;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatSettingsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartStatConditioningMode_t;
import a75f.io.device.serial.SmartStatControls_t;
import a75f.io.device.serial.SmartStatFanSpeed_t;
import a75f.io.device.serial.SmartStatSettings_t;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.Port;

/**
 * Created by Anilkumar isOn 1/10/2019.
 */
public class LSmartStat {

    private static final String TAG  = "LSmartStat";

    /***************************** SEED MESSAGES ****************************/

    public static ArrayList<CcuToCmOverUsbDatabaseSeedSmartStatMessage_t> getSeedMessages(Zone zone,String equipId)
    {
        ArrayList<CcuToCmOverUsbDatabaseSeedSmartStatMessage_t> seedMessages = new ArrayList<>();
        int i = 0;
        for (Device d : HSUtil.getDevices(zone.getId()))
        {
            seedMessages.add(getSeedMessage(zone, Short.parseShort(d.getAddr()),equipId));
            i++;
        }
        return seedMessages;
    }


    /********************************CONTROLS MESSAGES*************************************/

    public static Collection<CcuToCmOverUsbSmartStatControlsMessage_t> getControlMessages(Zone zone)
    {
        HashMap<Short, CcuToCmOverUsbSmartStatControlsMessage_t> controlMessagesHash = new HashMap<>();
        for (ZoneProfile zp : L.ccu().zoneProfiles)
        {
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
    private static void fillSmartStatControls(SmartStatControls_t controls, String equipId, short node){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \"" + node + "\"");
        controls.setTemperature.set((short) 144); //for Smartstat we always send desired temp as fixed value.
        controls.fanSpeed.set(SmartStatFanSpeed_t.values()[(int) getOperationalMode("fan",equipId)]);
        controls.conditioningMode.set(SmartStatConditioningMode_t.values()[(int) getOperationalMode("temp",equipId)]);
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

                        LSmartStat.getSmartStatPort(controls, p.getPort()).set(mappedVal);
                    }

                }
            }
        }
    }
    public static CcuToCmOverUsbDatabaseSeedSmartStatMessage_t getSeedMessage(Zone zone, short address,String equipId)
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
        fillSmartStatSettings(seedMessage.settings,equipId,address,zone);
        fillSmartStatControls(seedMessage.controls,equipId,address);
        return seedMessage;
    }
    public static CcuToCmOverUsbSmartStatSettingsMessage_t getSettingsMessage(Zone zone, short address,String equipRef)
    {

        CcuToCmOverUsbSmartStatSettingsMessage_t settingsMessage =
                new CcuToCmOverUsbSmartStatSettingsMessage_t();
        settingsMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_SETTINGS);
        settingsMessage.address.set(address);
        fillSmartStatSettings(settingsMessage.settings,equipRef,address,zone);
        return settingsMessage;
    }
    private static void fillSmartStatSettings(SmartStatSettings_t settings_t, String equipId, short address, Zone zone){

        settings_t.roomName.set(zone.getDisplayName());
        //TODO Need to set profile type currently default set it to CPU
        settings_t.profileBitmap.convetionalPackageUnit.set((short)1);
        settings_t.temperatureOffset.set((byte)getTempOffset(address));
        settings_t.enabledRelaysBitmap.relay1.set((short)1);
        settings_t.enabledRelaysBitmap.relay2.set((short)1);
        settings_t.enabledRelaysBitmap.relay3.set((short)1);
        settings_t.enabledRelaysBitmap.relay4.set((short)1);
        settings_t.enabledRelaysBitmap.relay5.set((short)1);
        settings_t.enabledRelaysBitmap.relay6.set((short)1);
        settings_t.enableOccupancyDetection.set((byte)getOccupancyEnable(address));
    }
    public static double getTempOffset(short addr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and temperature and offset and group == \""+addr+"\"");
    }

    public static double getOccupancyEnable(short addr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and occupancy and enable and group == \""+addr+"\"");
    }

    public static double getOperationalMode(String cmd, String equipRef){

        return  CCUHsApi.getInstance().readHisValByQuery("point and standalone and operation and mode and his and "+cmd+" and equipRef== \"" + equipRef + "\"");
    }

    /*public static double getConfigRelays(String relaydata){

    }*/
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
}
