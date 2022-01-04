package a75f.io.device.mesh.hyperstat;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
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
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings;
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_ONE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_THREE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_TWO;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_FIVE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_FOUR;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_ONE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_SIX;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_THREE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_TWO;

import android.util.Log;

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
        HyperStatSettingsMessage_t hyperStatSettingsMessage_t = getSettingsMessage(zone, address, equipRef);
        HyperStatControlsMessage_t hyperStatControlsMessage_t = getControlMessage(address, equipRef);
        HyperStat.HyperStatSettingsMessage2_t hyperStatSettingsMessage2_t = getSetting2Message(address, equipRef);
        HyperStat.HyperStatSettingsMessage3_t hyperStatSettingsMessage3_t = getSetting3Message(address, equipRef);
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+hyperStatSettingsMessage_t.toByteString().toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+hyperStatControlsMessage_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+hyperStatSettingsMessage2_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+hyperStatSettingsMessage3_t.toString());

        HyperStatCcuDatabaseSeedMessage_t seed = HyperStatCcuDatabaseSeedMessage_t.newBuilder()
                .setEncryptionKey(ByteString.copyFrom(L.getEncryptionKey()))
                .setSerializedSettingsData(hyperStatSettingsMessage_t.toByteString())
                .setSerializedControlsData(hyperStatControlsMessage_t.toByteString())
                .setSerializedSettings2Data(hyperStatSettingsMessage2_t.toByteString())
                .setSerializedSettings3Data(hyperStatSettingsMessage3_t.toByteString())
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
            .setHeatingDeadBand((int) (getStandaloneHeatingDeadband(equipRef) * 10))
            .setCoolingDeadBand((int) (getStandaloneCoolingDeadband(equipRef) * 10))
            .setMinCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("cooling and user and limit and min"))
            .setMaxCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("cooling and user and limit and max"))
                // Changed by Manjunath K. change in requirement on 17-11-2021
                // TODO Needs to for all the profiles (Smart node and smartstat)
            .setMinHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("heating and user and limit and max"))
            .setMaxHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("heating and user and limit and min"))
            .setTemperatureOffset((int) (DeviceHSUtil.getTempOffset(address)))
            .setHumidityMinSetpoint(getHumidityMinSp(address, CCUHsApi.getInstance()))
            .setHumidityMaxSetpoint(getHumidityMaxSp(address, CCUHsApi.getInstance()))
            .setTemperatureMode(HyperStat.HyperStatTemperatureMode_e.HYPERSTAT_TEMP_MODE_DUAL_VARIABLE_DB)
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

        // Sense profile does not have control messages
        if(device.containsKey("sense")) return HyperStat.HyperStatControlsMessage_t.newBuilder().build();

        HyperStatControlsMessage_t.Builder controls = HyperStat.HyperStatControlsMessage_t.newBuilder();
        controls.setSetTempCooling((int)(getDesiredTempCooling(equipRef) * 2));
        controls.setSetTempHeating((int)(getDesiredTempHeating(equipRef) * 2));
        BasicSettings settings = HSHaystackUtil.Companion.getBasicSettings(address);
        Log.i(L.TAG_CCU_DEVICE,
                "Desired Heat temp "+((int)getDesiredTempHeating(equipRef) * 2)+
                 "\n Desired Cool temp "+((int)getDesiredTempCooling(equipRef) * 2)+
                 "\n DeviceFanMode "+getDeviceFanMode(settings).name()+
                 "\n ConditioningMode"+getConditioningMode(settings,address).name());
        controls.setFanSpeed(getDeviceFanMode(settings));
        controls.setConditioningMode(getConditioningMode(settings,address));

        if (!device.isEmpty()) {
            DeviceHSUtil.getEnabledCmdPointsWithRefForDevice(device, hayStack).forEach(rawPoint -> {
                double logicalVal = hayStack.readHisValById(rawPoint.getPointRef());
                int mappedVal;
                       /* = (DeviceUtil.isAnalog(rawPoint.getPort()) ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                                     : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));*/
                if (Globals.getInstance().isTemporaryOverrideMode()) {
                    mappedVal = (short)logicalVal;
                } else {
                    mappedVal = (DeviceUtil.isAnalog(rawPoint.getPort())
                            ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                            : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));
                }

                hayStack.writeHisValById(rawPoint.getId(), (double) mappedVal);
                setHyperStatPort(controls, Port.valueOf(rawPoint.getPort()), mappedVal);
            });
            Log.i(L.TAG_CCU_DEVICE, "===================Device Layer==================================");
               DeviceHSUtil.getEnabledCmdPointsWithRefForDevice(device, hayStack)
                        .forEach( rawPoint -> {
                          double logicalVal = hayStack.readHisValById(rawPoint.getPointRef());
                          int mappedVal = (DeviceUtil.isAnalog(rawPoint.getPort())
                                               ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                                               : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));
                            hayStack.writeHisValById(rawPoint.getId(), Double.valueOf(mappedVal));
                            Log.i(L.TAG_CCU_DEVICE,
                                    rawPoint.getType()+" "+logicalVal+" Port "+rawPoint.getPort() +" =  "+mappedVal);
                          setHyperStatPort(controls, Port.valueOf(rawPoint.getPort()), mappedVal);
                      });
            Log.i(L.TAG_CCU_DEVICE, "=====================================================");
        }
        return controls.build();
    }
    
    private static double getDesiredTempCooling(String equipRef) {
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery("desired and temp and " +
                    "cooling and equipRef == \"" + equipRef + "\"");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private static double getDesiredTempHeating(String equipRef) {
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery("desired and temp and " +
                    "heating and equipRef == \"" + equipRef +
                    "\"");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return 0;
    }
    

    private static void setHyperStatPort(HyperStat.HyperStatControlsMessage_t.Builder controls,
                                         Port port, double val) {
        if (port == RELAY_ONE) {
            controls.setRelay1(val > 0);
        } else if (port == RELAY_TWO) {
            controls.setRelay2(val > 0);
        } else if (port == RELAY_THREE) {
            controls.setRelay3(val > 0);
        } else if (port == RELAY_FOUR) {
            controls.setRelay4(val > 0);
        } else if (port == RELAY_FIVE) {
            controls.setRelay5(val > 0);
        } else if (port == RELAY_SIX) {
            controls.setRelay6(val > 0);
        }else if (port == ANALOG_OUT_ONE) {
            controls.setAnalogOut1(getAnalogOutValue(val));
        }else if (port == ANALOG_OUT_TWO) {
            controls.setAnalogOut2(getAnalogOutValue(val));
        }else if (port == ANALOG_OUT_THREE) {
            controls.setAnalogOut3(getAnalogOutValue(val));
        }

    }

    private static HyperStat.HyperStatAnalogOutputControl_t getAnalogOutValue(double value){
        return HyperStat.HyperStatAnalogOutputControl_t.newBuilder().setPercent((int) value).build();
    }

    private static HyperStat.HyperStatFanSpeed_e getDeviceFanMode(BasicSettings settings){
        try {
            switch (settings.getFanMode()){
                case OFF: return HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_OFF;
                case AUTO: return HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO;
                case LOW_ALL_TIME:
                case LOW_CUR_OCC:
                case LOW_OCC: return HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_LOW;
                case MEDIUM_ALL_TIME:
                case MEDIUM_CUR_OCC:
                case MEDIUM_OCC: return HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_MED;
                case HIGH_ALL_TIME:
                case HIGH_CUR_OCC:
                case HIGH_OCC: return HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_HIGH;
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.i(L.TAG_CCU_DEVICE, "Exception getDeviceFanMode: ");
        }

        return HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_OFF;
    }

    private static HyperStat.HyperStatConditioningMode_e getConditioningMode(BasicSettings settings,int address){
        try {
            if(settings.getConditioningMode() == StandaloneConditioningMode.AUTO)
                return HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_AUTO;
            if(settings.getConditioningMode() == StandaloneConditioningMode.COOL_ONLY)
                return HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_COOLING;
            if(settings.getConditioningMode() == StandaloneConditioningMode.HEAT_ONLY)
                return HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_HEATING;
            else
                return HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_OFF;
        }catch (Exception e){
            Log.i(L.TAG_CCU_DEVICE, "Exception getConditioningMode: ");
            e.printStackTrace();
        }
        return HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_OFF;
    }
        private static int getHumidityMinSp(int address, CCUHsApi hayStack) {
            try{
                return hayStack.readDefaultVal("config and humidity and min and group == \"" + address + "\"").intValue();
            }catch (Exception e){
                Log.i(L.TAG_CCU_DEVICE, " "+e.getMessage()+ " address : "+address);
            }
            return 0;
        }

        private static int getHumidityMaxSp(int address, CCUHsApi hayStack) {
            try {
                return hayStack.readDefaultVal("config and humidity and max and group == \"" + address + "\"").intValue();
            }catch (Exception e){
                Log.i(L.TAG_CCU_DEVICE, " "+e.getMessage()+ " address : "+address);
            }
            return 0;
        }

    private static double getStandaloneCoolingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap collingDeadband = hayStack.read("point and tuner and deadband and base and cooling and equipRef == \""+equipRef+"\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(collingDeadband.get("id")).toString());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return 0;
        }

    }

    public static double getStandaloneHeatingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap deadbandPoint =
                hayStack.read("point and tuner and deadband and base and heating and equipRef == \""+equipRef+
                "\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(deadbandPoint.get("id")).toString());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return 0;
        }

    }

    public static HyperStat.HyperStatSettingsMessage2_t getSetting2Message(int address, String equipRef){
        return  HyperStatSettingsUtil.Companion.getSetting2Message(address,equipRef,CCUHsApi.getInstance());
    }
    public static HyperStat.HyperStatSettingsMessage3_t getSetting3Message(int address, String equipRef){
        return  HyperStatSettingsUtil.Companion.getSetting3Message(address,equipRef);
    }

}
