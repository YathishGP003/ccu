package a75f.io.device.mesh;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatSettingsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.device.mesh.MeshUtil.checkDuplicateStruct;
import static a75f.io.device.mesh.MeshUtil.sendStruct;
import static a75f.io.device.mesh.MeshUtil.sendStructToCM;
import static a75f.io.device.mesh.MeshUtil.sendStructToNodes;

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
        double maxHeat =  BuildingTunerCache.getInstance().getMaxHeatingUserLimit();
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
        double coolingDeadband = TunerUtil.readBuildingTunerValByQuery("cooling and deadband and base and equipRef == \""
                                                  + equip.getId()+"\"");
    
        double heatingDeadband = TunerUtil.readBuildingTunerValByQuery("heating and deadband and base and equipRef == \""
                                                  +equip.getId()+"\"");
        
        BuildingTunerCache buildingTuner = BuildingTunerCache.getInstance();
        if ((desiredTemp + coolingDeadband) > buildingTuner.getMaxCoolingUserLimit() ||
            (desiredTemp + coolingDeadband) < buildingTuner.getMinCoolingUserLimit() ||
            (desiredTemp - heatingDeadband) > buildingTuner.getMinHeatingUserLimit() ||
            (desiredTemp - heatingDeadband < buildingTuner.getMaxHeatingUserLimit())) {
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
}
