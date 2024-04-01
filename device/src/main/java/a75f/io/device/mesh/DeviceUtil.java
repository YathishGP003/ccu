package a75f.io.device.mesh;

import static a75f.io.device.mesh.MeshUtil.sendStructToNodes;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_IN_ONE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_IN_TWO;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_FOUR;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_ONE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_THREE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_TWO;
import static a75f.io.logic.bo.building.definitions.Port.DESIRED_TEMP;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_EIGHT;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_FIVE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_FOUR;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_ONE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_SEVEN;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_SIX;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_THREE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_TWO;
import static a75f.io.logic.bo.building.definitions.Port.RSSI;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_CO;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_CO2;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_CO2_EQUIVALENT;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_ILLUMINANCE;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_NO;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_OCCUPANCY;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_PM2P5;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_PRESSURE;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_RH;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_RT;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_SOUND;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_UVI;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_VOC;
import static a75f.io.logic.bo.building.definitions.Port.TH1_IN;
import static a75f.io.logic.bo.building.definitions.Port.TH2_IN;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Zone;
import a75f.io.constants.DeviceFieldConstants;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.diag.otastatus.OtaStatus;
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

public class DeviceUtil {
    
    public static boolean isAnalog(String port) {
       return (port.equals(Port.ANALOG_OUT_ONE.name())
               ||port.equals(Port.ANALOG_OUT_TWO.name())
               ||port.equals(Port.ANALOG_OUT_THREE.name())
               ||port.equals(Port.ANALOG_OUT_FOUR.name()));
    }
    
    public static short mapAnalogOut(String type, short val) {
        val = (short)Math.min(val, 100);
        val = (short)Math.max(val, 0);
        switch (type) {
            case "0-10v":
                return val;
            case "10-0v":
                return (short) (100 - val);
           /* case "2-10v":
                return (short) (20 + scaleAnalog(val, 80));
            case "10-2v":
                return (short) (100 - scaleAnalog(val, 80));*/
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
    
    public static short mapDigitalOut(String type, boolean val) {
        if (type.equals("Relay N/O")) {
            return (short) (val ? 1 : 0);
        } else if (type.equals("Relay N/C")) {
            return (short) (val ? 0 : 1);
        }
        return 0;
    }
    
    public static int scaleAnalog(short analog, int scale) {
        return (int) ((float) scale * ((float) analog / 100.0f));
    }
    
    public static short getMaxUserTempLimits(double deadband){
        double maxCool = BuildingTunerCache.getInstance().getMaxCoolingUserLimit();
        return (short)(maxCool- deadband);
    }
    
    public static short getMinUserTempLimits(double deadband){
        double maxHeat =  BuildingTunerCache.getInstance().getMinHeatingUserLimit();
        return (short)(maxHeat+ deadband);
    }
    
    /**
     * Validate if the desired temp set from a smartnode/smartstat module is accepatable based on
     * user settings on CCU.
     * @param nodeAddr
     * @param desiredTemp
     * @return
     */
    public static boolean validateDesiredTempUserLimits(Short nodeAddr, double desiredTemp) {
        Equip equip = HSUtil.getEquipForModule(nodeAddr);
        if (equip == null) {
            return false;
        }
        double coolingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("cooling and deadband and schedulable and roomRef == \""+equip.getRoomRef()+"\"");
    
        double heatingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("heating and deadband and schedulable and roomRef == \""+equip.getRoomRef()+"\"");
        
        BuildingTunerCache buildingTuner = BuildingTunerCache.getInstance();
        if ((desiredTemp + coolingDeadband) > buildingTuner.getMaxCoolingUserLimit() ||
                (desiredTemp + coolingDeadband) < buildingTuner.getMinCoolingUserLimit() ||
                (desiredTemp - heatingDeadband) < buildingTuner.getMinHeatingUserLimit() ||
                (desiredTemp - heatingDeadband > buildingTuner.getMaxHeatingUserLimit())) {
            CcuLog.d(L.TAG_CCU_DEVICE,
                     "validateDesiredTempUserLimits desiredTemp "+desiredTemp+" coolingDeadband "+coolingDeadband+"" +
                             " heatingDeadband "+heatingDeadband+" maxCoolingLimit "+buildingTuner.getMaxCoolingUserLimit()+
                             " minCoolingLimit "+buildingTuner.getMinCoolingUserLimit()+
                             " minHeatingLimit "+buildingTuner.getMinHeatingUserLimit()+
                             " maxHeatingLimit "+buildingTuner.getMaxHeatingUserLimit());
            return false;
        }
        return true;
    }

    public static double getValidDesiredCoolingTemp(double desiredTemp,
                                                    double coolingDeadband,
                                                    double maxCoolingUserLimit,
                                                    double minCoolingUserLimit
                                                    ) {
        double calculateCoolingDesiredTemp = desiredTemp + coolingDeadband;
        if (calculateCoolingDesiredTemp <= maxCoolingUserLimit &&
                calculateCoolingDesiredTemp >= minCoolingUserLimit)
            return desiredTemp + coolingDeadband;
        else if (calculateCoolingDesiredTemp < minCoolingUserLimit)
            return minCoolingUserLimit;
        else
            return maxCoolingUserLimit;

    }
    public static double getValidDesiredHeatingTemp(double desiredTemp,
                                                    double heatingDeadband,
                                                    double maxHeatingUserLimit,
                                                    double minHeatingUserLimit) {

        double calculateHeatingDesiredTemp = desiredTemp - heatingDeadband;
        if (calculateHeatingDesiredTemp <= maxHeatingUserLimit &&
                calculateHeatingDesiredTemp >= minHeatingUserLimit)
            return calculateHeatingDesiredTemp;
        else if (calculateHeatingDesiredTemp < minHeatingUserLimit)
            return minHeatingUserLimit;
        else
            return maxHeatingUserLimit;
    }


        public static void sendSeedMessage(Short nodeAddr, boolean isSmartStat) {
        Equip equip = HSUtil.getEquipForModule(nodeAddr);
        if (equip == null) {
            return;
        }
        LSerial.getInstance().sendSeedMessage(isSmartStat, false, nodeAddr, equip.getRoomRef(), equip.getFloorRef());
    }
    
    /**
     * Send control message to smartstat or smartnode , bypassing the duplicate check.
     * @param nodeAddr
     * @param isSmartNode
     */
    
    public static void sendControlsMessage(Short nodeAddr, boolean isSmartNode) {
        
        Equip equip = HSUtil.getEquipForModule(nodeAddr);
        if (equip == null) {
            return;
        }
        Zone zone = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(equip.getRoomRef())).build();
        
        if (isSmartNode) {
            CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SN Controls =====================");
            CcuToCmOverUsbSnControlsMessage_t snControlsMessage =
                LSmartNode.getControlMessage(zone, Short.parseShort(equip.getGroup()), equip.getId());
            snControlsMessage = LSmartNode.getCurrentTimeForControlMessage(snControlsMessage);
            sendStructToNodes(snControlsMessage);
        } else {
            CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SS Controls =====================");
            CcuToCmOverUsbSmartStatControlsMessage_t ssControlsSSMessage =
                                    LSmartStat.getControlMessage(zone, Short.parseShort(equip.getGroup()),equip.getId());
            ssControlsSSMessage = LSmartStat.getCurrentTimeForControlMessage(ssControlsSSMessage);
            sendStructToNodes(ssControlsSSMessage);
        }
    }
    
    public static short getModulatedAnalogVal(double min, double max, double val) {
        return max > min ? (short) (10 * (min + (max - min) * val/100)) : (short) (10 * (min - (min - max) * val/100));
    }
    
    public static CcuToCmOverUsbCmRelayActivationMessage_t getCMControlsMessage() {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        msg.analog0.set((short) ControlMote.getAnalogOut("analog1"));
        msg.analog1.set((short) ControlMote.getAnalogOut("analog2"));
        msg.analog2.set((short) ControlMote.getAnalogOut("analog3"));
        msg.analog3.set((short) ControlMote.getAnalogOut("analog4"));
        int relayBitmap = 0;
        for (int i = 1; i <= 7; i++)
        {
            if (ControlMote.getRelayState("relay" + i) > 0)
            {
                relayBitmap |= 1 << MeshUtil.getRelayMapping(i);
            }
        }
        msg.relayBitmap.set((short) relayBitmap);
        
        return msg;
    }


    public static double getPercentageFromVoltage(double physicalVoltage, String analogType) {
        String [] arrOfStr = analogType.split("-");
        if (arrOfStr.length == 2) {
            if (arrOfStr[1].contains("v")) {
                arrOfStr[1] = arrOfStr[1].replace("v", "");
            }
            double minVoltage =  Double.parseDouble(arrOfStr[0]);
            double maxVoltage =  Double.parseDouble(arrOfStr[1]);

            Log.i(L.TAG_CCU_DEVICE, "Feedback physicalVoltage"+physicalVoltage +"Min = "+minVoltage+" Max = "+maxVoltage);
            double feedbackPercent = ((physicalVoltage - minVoltage) / (maxVoltage - minVoltage)) * 100;
            Log.i(L.TAG_CCU_DEVICE, "Actual Feedback Result"+ feedbackPercent);
            return feedbackPercent;
        }
        Log.i(L.TAG_CCU_DEVICE, "invalid analogType "+analogType);
        return 0;
    }

    public static String parseNodeStatusMessage(int data, int nodeAddress){

        String binaryValue = String.format("%08d",(Integer.parseInt(Integer.toBinaryString(data))));
        int message = Integer.parseInt(binaryValue.substring(0,5),2);
        int msgType = Integer.parseInt(binaryValue.substring(5),2);
        if(msgType == 1) return getCause(message);
        return DeviceFieldConstants.NO_INFO;
    }

    public static OtaStatus getNodeStatus(int data){

        String binaryValue = String.format("%08d",(Integer.parseInt(Integer.toBinaryString(data))));
        int message = Integer.parseInt(binaryValue.substring(0,5),2);
        int msgType = Integer.parseInt(binaryValue.substring(5),2);
        Log.i(L.TAG_CCU_OTA_PROCESS, "getNodeStatus: message : "+message + " msgType : "+msgType);
        if(msgType == 1)
            return getStatus(message);
        return OtaStatus.NO_INFO;
    }


    public static OtaStatus getStatus(int msgType) {
        switch (msgType) {
            case 0: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_SUCCESSFUL;
            case 1: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_REBOOT_INTERRUPTION;
            case 2: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_NOT_FOR_ME_DEV_TYPE;
            case 3: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_NOT_FOR_ME_FW_VERSION;
            case 4: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_IMAGE_SIZE;
            case 5: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_EXT_FLASH_ERROR;
            case 6: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_IMAGE_VERIFICATION;
            case 7: return OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_INACTIVITY_TIMEOUT;
            default: return OtaStatus.NO_INFO;
        }
    }

    public static String  getCause(int msgType) {

        switch (msgType) {
            case 0:
                return DeviceFieldConstants.CAUSE0;
            case 1:
                return DeviceFieldConstants.CAUSE1;
            case 2:
                return DeviceFieldConstants.CAUSE2;
            case 3:
                return DeviceFieldConstants.CAUSE3;
            case 4:
                return DeviceFieldConstants.CAUSE4;
            case 5:
                return DeviceFieldConstants.CAUSE5;
            case 6:
                return DeviceFieldConstants.CAUSE6;
            case 7:
                return DeviceFieldConstants.CAUSE7;
            default:
                return DeviceFieldConstants.NO_INFO;
        }
    }

    public static void updateDesiredTempFromDevice(Point coolPoint, Point heatPoint, Point avgPoint, double coolVal,
                                                   double heatVal, double avgVal, CCUHsApi hayStack, String who) {
        List<HashMap<Object, Object>> equipsInZone =
                CCUHsApi.getInstance().readAllEntities("equip and zone and roomRef ==\"" + coolPoint.getRoomRef() + "\"");

        if (equipsInZone.size() == 1) {
            CcuLog.i(L.TAG_CCU_DEVICE,"updateDesiredTempFromDevice : "+coolVal+" "+heatVal);
            SystemScheduleUtil.handleManualDesiredTempUpdate(coolPoint, heatPoint, avgPoint, coolVal, heatVal, avgVal
                    , who);
            return;
        }

        equipsInZone.forEach( equip -> {
            HashMap<Object,Object> coolDT = hayStack.readEntity("point and temp and desired and cooling and equipRef == \""
                    + equip.get("id").toString() + "\"");
            HashMap<Object,Object> heatDT = hayStack.readEntity("point and temp and desired and heating and equipRef == \""
                    + equip.get("id").toString() + "\"");
            HashMap<Object,Object> avgDT = hayStack.readEntity("point and temp and desired and (avg or average) and equipRef == \""
                    + equip.get("id").toString() + "\"");

            Point coolDtPoint = new Point.Builder().setHashMap(coolDT).build();
            Point heatDtPoint = new Point.Builder().setHashMap(heatDT).build();
            Point avgDtPoint = new Point.Builder().setHashMap(avgDT).build();
            CcuLog.i(L.TAG_CCU_DEVICE,"updateDesiredTempFromDevice for : "+equip+", "+coolVal+" "+heatVal);
            SystemScheduleUtil.handleManualDesiredTempUpdate(coolDtPoint, heatDtPoint, avgDtPoint, coolVal, heatVal,
                    avgVal, who);
        });

    }

    public static Port getPortFromDomainName(String domainName) {
        switch (domainName) {
            case "analog1Out": return ANALOG_OUT_ONE;
            case "analog2Out": return ANALOG_OUT_TWO;
            case "analog3Out": return ANALOG_OUT_THREE;
            case "analog4Out": return ANALOG_OUT_FOUR;
            case "relay1": return RELAY_ONE;
            case "relay2": return RELAY_TWO;
            case "relay3": return RELAY_THREE;
            case "relay4": return RELAY_FOUR;
            case "relay5": return RELAY_FIVE;
            case "relay6": return RELAY_SIX;
            case "relay7": return RELAY_SEVEN;
            case "relay8": return RELAY_EIGHT;

            case "th1In": return TH1_IN;
            case "th2In": return TH2_IN;

            case "analog1In": return ANALOG_IN_ONE;
            case "analog2In": return ANALOG_IN_TWO;

            case "currentTemp": return SENSOR_RT;
            case "humiditySensor": return SENSOR_RH;
            case "desiredTemp": return DESIRED_TEMP;

            case "co2Sensor": return SENSOR_CO2;
            case "vocSensor": return SENSOR_VOC;
            case "occupancySensor": return SENSOR_OCCUPANCY;
            case "illuminanceSensor": return SENSOR_ILLUMINANCE;
            case "uviSensor": return SENSOR_UVI;
            case "pressureSensor": return SENSOR_PRESSURE;

            case "soundSensor": return SENSOR_SOUND;
            case "coSensor": return SENSOR_CO;
            case "noSensor": return SENSOR_NO;
            case "co2EquivalentSensor": return SENSOR_CO2_EQUIVALENT;
            case "pm25Sensor": return SENSOR_PM2P5;

            case "rssi": return RSSI;
            default: return null;
        }
    }


}
