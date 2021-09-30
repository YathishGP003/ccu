package a75f.io.device.mesh.hyperstat;

import com.google.protobuf.ByteString;

import java.util.HashMap;


import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.HyperStat.HyperStatCcuDatabaseSeedMessage_t;
import a75f.io.device.HyperStat.HyperStatControlsMessage_t;
import a75f.io.device.HyperStat.HyperStatSettingsMessage_t;
import a75f.io.device.HyperStat;
import a75f.io.device.mesh.DeviceHSUtil;
import a75f.io.device.mesh.DeviceUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.definitions.Port.RELAY_FIVE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_FOUR;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_ONE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_SIX;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_THREE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_TWO;

public class HyperStatMessageGenerator {
    
    /**
     * Generates seed message for a node from haystack data.
     * @param zone
     * @param address
     * @param equipRef
     * @param profile
     * @return
     */
    public static HyperStatCcuDatabaseSeedMessage_t getSeedMessage(String zone, int address, String equipRef, String profile) {
        HyperStatCcuDatabaseSeedMessage_t seed = HyperStatCcuDatabaseSeedMessage_t
                                               .newBuilder()
                                               .setEncryptionKey(ByteString.copyFrom(L.getEncryptionKey()))
                                               .setSerializedSettingsData(getSettingsMessage(zone, address, equipRef).toByteString())
                                               .setSerializedControlsData(getControlMessage(address, equipRef).toByteString())
                                               .build();
        
        return seed;
    }
    
    /**
     * Generate settings message for a node from haystack data.
     * @param zone
     * @param address
     * @param equipRef
     * @return
     */
    public static HyperStatSettingsMessage_t getSettingsMessage(String zone, int address, String equipRef) {
        
        //TODO - Proto file does not define profile bitmap, enabledRelay.
        HyperStatSettingsMessage_t settings = HyperStatSettingsMessage_t.newBuilder()
                                            .setRoomName(zone)
                                            .setHeatingDeadBand((int) StandaloneTunerUtil.getStandaloneHeatingDeadband(equipRef))
                                            .setCoolingDeadBand((int)StandaloneTunerUtil.getStandaloneCoolingDeadband(equipRef))
                                            .setMinCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("cooling and user " +
                                                                                                               "and limit and min"))
                                            .setMaxCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("cooling and user " +
                                                                                                               "and limit and max"))
                                            .setMinHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("heating and user " +
                                                                                                               "and limit and min"))
                                            .setMaxHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("heating and user " +
                                                                                                               "and limit and max"))
                                            .setTemperatureOffset((int) (DeviceHSUtil.getTempOffset(address)))
                                            .build();
        return settings;
    }
    
    /**
     * Generate control message for a node from haystack data.
     * @param address
     * @param equipRef
     * @return
     */
    public static HyperStatControlsMessage_t getControlMessage(int address, String equipRef) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        
        HashMap device = hayStack.read("device and addr == \"" + address + "\"");
        
        HyperStatControlsMessage_t.Builder controls = HyperStat.HyperStatControlsMessage_t.newBuilder();
        controls.setSetTempCooling((int)getDesiredTempCooling(equipRef) * 2);
        controls.setSetTempHeating((int)getDesiredTempHeating(equipRef) * 2);
        controls.setFanSpeed(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO) ;//TODO
        controls.setConditioningMode(HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_AUTO);
        
        if (!device.isEmpty()) {
            DeviceHSUtil.getEnabledCmdPointsWithRefForDevice(device, hayStack)
                        .forEach( rawPoint -> {
                          double logicalVal = hayStack.readHisValById(rawPoint.getPointRef());
    
                          /*int mappedVal = (DeviceUtil.isAnalog(rawPoint.getPort())
                                               ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                                               : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));*/
                            int mappedVal;

                            if (Globals.getInstance().isTemproryOverrideMode()) {
                                mappedVal = (short)logicalVal;
                            } else {
                                mappedVal = (DeviceUtil.isAnalog(rawPoint.getPort())
                                        ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                                        : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));
                            }
                          hayStack.writeHisValById(rawPoint.getId(), (double) mappedVal);
                          setHyperStatPort(controls, Port.valueOf(rawPoint.getPort()), mappedVal > 0);
                      });
        }
        
        return controls.build();
    }
    
    private static double getDesiredTempCooling(String equipRef) {
        return CCUHsApi.getInstance().readDefaultVal("desired and temp and cooling and equipRef == \""+equipRef+"\"");
    }
    
    private static double getDesiredTempHeating(String equipRef) {
        return CCUHsApi.getInstance().readDefaultVal("desired and temp and heating and equipRef == \""+equipRef+"\"");
    }
    
    private static void setHyperStatPort(HyperStat.HyperStatControlsMessage_t.Builder controls,
                                         Port port, boolean val) {
        if (port == RELAY_ONE) {
            controls.setRelay1(val);
        } else if (port == RELAY_TWO) {
            controls.setRelay2(val);
        } else if (port == RELAY_THREE) {
            controls.setRelay3(val);
        } else if (port == RELAY_FOUR) {
            controls.setRelay4(val);
        } else if (port == RELAY_FIVE) {
            controls.setRelay5(val);
        } else if (port == RELAY_SIX) {
            controls.setRelay6(val);
        }
    }
}
