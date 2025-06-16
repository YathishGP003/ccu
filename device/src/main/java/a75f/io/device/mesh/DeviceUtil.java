package a75f.io.device.mesh;

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
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_ENERGY_METER;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_ILLUMINANCE;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_NO;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_OCCUPANCY;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_PM10;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_PM2P5;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_PRESSURE;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_RH;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_RT;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_SOUND;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_UVI;
import static a75f.io.logic.bo.building.definitions.Port.SENSOR_VOC;
import static a75f.io.logic.bo.building.definitions.Port.TH1_IN;
import static a75f.io.logic.bo.building.definitions.Port.TH2_IN;

import org.projecthaystack.HDict;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Zone;
import a75f.io.constants.DeviceFieldConstants;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.PhysicalPoint;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.system.dab.DabAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.diag.otastatus.OtaStatus;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.tuners.BuildingTunerCache;

public class DeviceUtil {
    
    public static boolean isAnalog(String port) {
       return (port.equals(Port.ANALOG_OUT_ONE.name())
               ||port.equals(Port.ANALOG_OUT_TWO.name())
               ||port.equals(Port.ANALOG_OUT_THREE.name())
               ||port.equals(Port.ANALOG_OUT_FOUR.name()));
    }

    public static boolean isAnalog(RawPoint rawPoint) {
        return (rawPoint.getDomainName().equals(DomainName.analog1Out)
                ||rawPoint.getDomainName().equals(DomainName.analog2Out)
                ||rawPoint.getDomainName().equals(DomainName.analog3Out)
                ||rawPoint.getDomainName().equals(DomainName.analog4Out));
    }

    public static short mapAnalogOut(String type, short val) {
        val = (short)Math.min(val, 100);
        val = (short)Math.max(val, 0);
        switch (type) {
            case "0-10v":
                return val;
            case "10-0v":
                return (short) (100 - val);
            default:
            String [] arrOfStr = type.split("-");
            if (arrOfStr.length == 2)
            {
                if (arrOfStr[1].contains("v")) {
                    arrOfStr[1] = arrOfStr[1].replace("v", "");
                }

                int min = (int) Double.parseDouble(arrOfStr[0]);
                int max = (int) Double.parseDouble(arrOfStr[1]);

                return (short) ((((max - min) * (val / 100.0)) + min) * 10);

                /*
                This logic will not work when max is 0
                if (max > min) {
                    return (short) (min * 10 + (max - min ) * 10 * val/100);
                } else {
                    return (short) (min * 10 - (min - max ) * 10 * val/100);
                }*/
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

    public static short getMaxUserTempLimits(double deadband, String zoneId){
        double maxCool = CCUHsApi.getInstance().readPointPriorityValByQuery("cooling and user and limit and max and roomRef == \""+zoneId+"\"");
        return (short)(maxCool- deadband);
    }
    
    public static short getMinUserTempLimits(double deadband, String zoneId){
        double maxHeat = CCUHsApi.getInstance().readPointPriorityValByQuery("heating and user and limit and min and roomRef == \""+zoneId+"\"");
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
                     "validateDesiredTempUserLimits desiredTemp "+desiredTemp+" coolingDeadband "+coolingDeadband+
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

    public static short getModulatedAnalogVal(double min, double max, double val) {
        return max > min ? (short) (10 * (min + (max - min) * val/100)) : (short) (10 * (min - (min - max) * val/100));
    }
    
    public static CcuToCmOverUsbCmRelayActivationMessage_t getCMControlsMessage() {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);

        if ((L.ccu().systemProfile instanceof VavStagedRtu && !(L.ccu().systemProfile instanceof VavAdvancedHybridRtu)) ||
                L.ccu().systemProfile instanceof VavStagedRtuWithVfd ||
                L.ccu().systemProfile instanceof VavAdvancedAhu ||
                L.ccu().systemProfile instanceof VavFullyModulatingRtu ||
                (L.ccu().systemProfile instanceof DabStagedRtu && !(L.ccu().systemProfile instanceof DabAdvancedHybridRtu)) ||
                L.ccu().systemProfile instanceof DabStagedRtuWithVfd ||
                L.ccu().systemProfile instanceof DabFullyModulatingRtu) {

            msg.analog0.set(getPortValueAndUpdateHisData(Domain.cmBoardDevice.getAnalog1Out(), ccuHsApi));
            msg.analog1.set(getPortValueAndUpdateHisData(Domain.cmBoardDevice.getAnalog2Out(), ccuHsApi));
            msg.analog2.set(getPortValueAndUpdateHisData(Domain.cmBoardDevice.getAnalog3Out(), ccuHsApi));
            msg.analog3.set(getPortValueAndUpdateHisData(Domain.cmBoardDevice.getAnalog4Out(), ccuHsApi));

            int relayBitmap = 0;
            for (int i = 1; i <= 8; i++) {
                if (isRelayActivated(ccuHsApi, "relay"+i)) {
                    relayBitmap |= 1 << MeshUtil.getRelayMapping(i);
                }
            }
            msg.relayBitmap.set((short) relayBitmap);

        } else {
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
        }

        
        return msg;
    }

    private static boolean isRelayActivated(CCUHsApi ccuHsApi, String relay) {
        String relayQuery = ("point and physical and deviceRef == \""
                + Domain.cmBoardDevice.getId() + "\" " +
                "and domainName == \""+relay+"\"");
        HashMap map = ccuHsApi.readEntity(relayQuery);
        CcuLog.d("CCU_DEVICE", "test-writable isRelayActivated read:=======relay====<--dis-->> "+ relay + "<-iswritable->"+map.containsKey(Tags.WRITABLE) + "<--is unused-->"+map.containsKey(Tags.UNUSED));
        double value;
        if(map.containsKey(Tags.WRITABLE)){
            value = ccuHsApi.readPointPriorityValByQuery(relayQuery);
            CcuLog.d("CCU_DEVICE", "test-writable isRelayActivated read:=======relay====<--dis-->> "+ relay + "<--readPointPriorityVal-->"+value);
            CcuLog.d("CCU_DEVICE", "test-writable isRelayActivated write:=======relay====<--dis-->> "+ relay + "<--writeHisValById-->"+value);
            ccuHsApi.writeHisValById(map.get("id").toString(), value);
        } else {
            value = CCUHsApi.getInstance().readHisValByQuery(relayQuery);
            CcuLog.d("CCU_DEVICE", "test-writable isRelayActivated read 2 :=======relay====<--dis-->> "+ relay + "<--readHisValByQuery-->"+value);
        }
        return value > 0;
    }

    private static short getPortValue(PhysicalPoint physicalPoint, CCUHsApi ccuHsApi) {
        if(physicalPoint.readPoint().getMarkers().contains(Tags.WRITABLE)){
            return (short) ccuHsApi.readPointPriorityVal(physicalPoint.readPoint().getId());
        }
        return (short) physicalPoint.readHisVal();
    }

    private static short getPortValueAndUpdateHisData(PhysicalPoint physicalPoint, CCUHsApi ccuHsApi) {
        double value;
        if (physicalPoint.readPoint().getMarkers().contains(Tags.WRITABLE)) {
            value = ccuHsApi.readPointPriorityVal(physicalPoint.readPoint().getId());
            CcuLog.d(L.TAG_CCU_DEVICE, "test-writable Physical READ getPortValueAndUpdateHisData: writable id->"+physicalPoint.readPoint().getId()+"<value:>"+value+"<-dis->"+physicalPoint.readPoint().getDisplayName());
        } else {
            value = physicalPoint.readHisVal();
            CcuLog.d(L.TAG_CCU_DEVICE, "test-writable Physical READ getPortValueAndUpdateHisData: not writable id->"+physicalPoint.readPoint().getId()+"<value:>"+value+"<-dis->"+physicalPoint.readPoint().getDisplayName());
        }
        CcuLog.d(L.TAG_CCU_DEVICE, "test-writable Physical WRITE getPortValueAndUpdateHisData: write his value ->"+physicalPoint.readPoint().getId()+"<value:>"+value+"<-dis->"+physicalPoint.readPoint().getDisplayName());
        ccuHsApi.writeHisValById(physicalPoint.readPoint().getId(), value);
        return (short) value;
    }

    public static double getPercentageFromVoltage(double physicalVoltage, String analogType) {
        String [] arrOfStr = analogType.split("-");
        if (arrOfStr.length == 2) {
            if (arrOfStr[1].contains("v")) {
                arrOfStr[1] = arrOfStr[1].replace("v", "");
            }
            double minVoltage =  Double.parseDouble(arrOfStr[0]);
            double maxVoltage =  Double.parseDouble(arrOfStr[1]);

            CcuLog.i(L.TAG_CCU_DEVICE, "Feedback physicalVoltage"+physicalVoltage +"Min = "+minVoltage+" Max = "+maxVoltage);
            double feedbackPercent = ((physicalVoltage - minVoltage) / (maxVoltage - minVoltage)) * 100;
            CcuLog.i(L.TAG_CCU_DEVICE, "Actual Feedback Result"+ feedbackPercent);
            return feedbackPercent;
        }
        CcuLog.i(L.TAG_CCU_DEVICE, "invalid analogType "+analogType);
        return 0;
    }

    public static String parseNodeStatusMessage(int data){

        String binaryValue = String.format("%08d",(Integer.parseInt(Integer.toBinaryString(data))));
        int message = Integer.parseInt(binaryValue.substring(0,5),2);
        int msgType = Integer.parseInt(binaryValue.substring(7),2);
        if(msgType == 1) return getCause(message);
        return DeviceFieldConstants.NO_INFO;
    }

    public static OtaStatus getNodeStatus(int data){

        String binaryValue = String.format("%08d",(Integer.parseInt(Integer.toBinaryString(data))));
        int message = Integer.parseInt(binaryValue.substring(0,5),2);
        int msgType = Integer.parseInt(binaryValue.substring(7),2);
        CcuLog.i(L.TAG_CCU_OTA_PROCESS, "getNodeStatus: message : "+message + " msgType : "+msgType);
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
            HDict coolDT = hayStack.readHDict("point and temp and desired and cooling and equipRef == \""
                    + equip.get("id").toString() + "\"");
            HDict heatDT = hayStack.readHDict("point and temp and desired and heating and equipRef == \""
                    + equip.get("id").toString() + "\"");
            HDict avgDT = hayStack.readHDict("point and temp and desired and (avg or average) and equipRef == \""
                    + equip.get("id").toString() + "\"");

            Point coolDtPoint = new Point.Builder().setHDict(coolDT).build();
            Point heatDtPoint = new Point.Builder().setHDict(heatDT).build();
            Point avgDtPoint = new Point.Builder().setHDict(avgDT).build();
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
            case "sensor_pm10": return SENSOR_PM10;
            case "sensor_energy_meter": return SENSOR_ENERGY_METER;

            case "rssi": return RSSI;
            default: return null;
        }
    }
    public static double getMaxCoolingUserLimit(String zoneId){
        return  CCUHsApi.getInstance().readPointPriorityValByQuery("cooling and user and limit and max and roomRef == \""+zoneId+"\"");
    }

    public static double getMinCoolingUserLimit(String zoneId){
        return  CCUHsApi.getInstance().readPointPriorityValByQuery("cooling and user and limit and min and roomRef == \""+zoneId+"\"");
    }

    public static double getMaxHeatingUserLimit(String zoneId){
        return  CCUHsApi.getInstance().readPointPriorityValByQuery("heating and user and limit and max and roomRef == \""+zoneId+"\"");
    }

    public static double getMinHeatingUserLimit(String zoneId){
        return  CCUHsApi.getInstance().readPointPriorityValByQuery("heating and user and limit and min and roomRef == \""+zoneId+"\"");
    }

}
