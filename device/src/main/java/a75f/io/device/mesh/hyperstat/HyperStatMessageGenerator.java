package a75f.io.device.mesh.hyperstat;

import com.google.protobuf.ByteString;

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
import a75f.io.device.util.DeviceConfigurationUtil;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings;
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.UNOCCUPIED;
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
     * @return
     */
    public static HyperStatCcuDatabaseSeedMessage_t getSeedMessage(String zone, int address,
                                                                   String equipRef, TemperatureMode temperatureMode) {
        HyperStatSettingsMessage_t hyperStatSettingsMessage_t = getSettingsMessage(zone, address,
                equipRef, temperatureMode);
        HyperStatControlsMessage_t hyperStatControlsMessage_t = getControlMessage(address,
                equipRef, temperatureMode).build();
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
    public static HyperStatSettingsMessage_t getSettingsMessage(String zone, int address,
                                                                String equipRef, TemperatureMode mode) {
        int temperatureMode = (int) Domain.readDefaultValByDomain(DomainName.temperatureMode);

        int minCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperStatSettingsUtil.Companion.getCoolingUserLimitByQuery(mode, "min", equipRef)).intValue();
        int maxCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperStatSettingsUtil.Companion.getCoolingUserLimitByQuery(mode, "max", equipRef)).intValue();
        int minHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperStatSettingsUtil.Companion.getHeatingUserLimitByQuery(mode, "min", equipRef)).intValue();
        int maxHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperStatSettingsUtil.Companion.getHeatingUserLimitByQuery(mode, "max", equipRef)).intValue();

        if (minCoolingUserTemp == 0) {
            minCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery("point and schedulable and default and cooling and user and limit and min").intValue();
            CcuLog.d(L.TAG_CCU_DEVICE, "Zone min cooling user limit not found; falling back to BuildingTuner value of " + minCoolingUserTemp);
        }
        if (maxCoolingUserTemp == 0) {
            maxCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery("point and schedulable and default and cooling and user and limit and max").intValue();
            CcuLog.d(L.TAG_CCU_DEVICE, "Zone max cooling user limit not found; falling back to BuildingTuner value of " + maxCoolingUserTemp);
        }
        if (minHeatingUserTemp == 0) {
            minHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery("point and schedulable and default and heating and user and limit and min").intValue();
            CcuLog.d(L.TAG_CCU_DEVICE, "Zone min heating user limit not found; falling back to BuildingTuner value of " + minHeatingUserTemp);
        }
        if (maxHeatingUserTemp == 0) {
            maxHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery("point and schedulable and default and heating and user and limit and max").intValue();
            CcuLog.d(L.TAG_CCU_DEVICE, "Zone max heating user limit not found; falling back to BuildingTuner value of " + maxHeatingUserTemp);
        }

        return HyperStatSettingsMessage_t.newBuilder()
            .setRoomName(zone)
                .setHeatingDeadBand((int) (getStandaloneHeatingDeadband(equipRef, mode) * 10))
                .setCoolingDeadBand((int) (getStandaloneCoolingDeadband(equipRef, mode) * 10))
                .setMinCoolingUserTemp(minCoolingUserTemp)
                .setMaxCoolingUserTemp(maxCoolingUserTemp)
                .setMinHeatingUserTemp(minHeatingUserTemp)
                .setMaxHeatingUserTemp(maxHeatingUserTemp)
            .setTemperatureOffset((int) (DeviceHSUtil.getTempOffset(address)))
            .setHumidityMinSetpoint(getHumidityMinSp(address, CCUHsApi.getInstance()))
            .setHumidityMaxSetpoint(getHumidityMaxSp(address, CCUHsApi.getInstance()))
            .setShowCentigrade(DeviceConfigurationUtil.Companion.getUserConfiguration() == 1)
            .setDisplayHumidity(isDisplayHumidity(equipRef))
            .setDisplayCO2(isDisplayCo2(equipRef))
            .setDisplayVOC(isDisplayVov(equipRef))
            .setDisplayPM25(isDisplayP2p5(equipRef))
            .setCo2AlertTarget((int)readCo2ThresholdValue(equipRef))
            .setPm25AlertTarget((int)readPm2p5ThresholdValue(equipRef))
            .setVocAlertTarget((int)readVocThresholdValue(equipRef))
            .setHyperstatLinearFanSpeeds(HyperStatSettingsUtil.Companion.getLinearFanSpeedDetails(equipRef))
            .setHyperstatStagedFanSpeeds(HyperStatSettingsUtil.Companion.getStagedFanSpeedDetails(equipRef))
                .setTemperatureMode(temperatureMode == 0 ? HyperStat.HyperStatTemperatureMode_e.HYPERSTAT_TEMP_MODE_DUAL_FIXED_DB
                        : HyperStat.HyperStatTemperatureMode_e.HYPERSTAT_TEMP_MODE_DUAL_VARIABLE_DB)
            .build();


    }
    
    /**
     * Generate control message for a node from haystack data.
     * @param address
     * @param equipRef
     * @return
     */
    public static HyperStatControlsMessage_t.Builder getControlMessage(int address, String equipRef,
                                                                       TemperatureMode mode) {

        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \"" + address + "\"");

        // Sense profile does not have control messages
        if(device.containsKey("monitoring")) return HyperStat.HyperStatControlsMessage_t.newBuilder();

        HyperStatControlsMessage_t.Builder controls = HyperStat.HyperStatControlsMessage_t.newBuilder();
        controls.setSetTempCooling((int)(getDesiredTempCooling(equipRef, mode) * 2));
        controls.setSetTempHeating((int)(getDesiredTempHeating(equipRef, mode) * 2));
        BasicSettings settings = new BasicSettings(
                StandaloneConditioningMode.values()[(int)getFanMode(equipRef)],
                StandaloneFanStage.values()[(int)getConditioningMode(equipRef)]
        );
        controls.setFanSpeed(getDeviceFanMode(settings));
        controls.setConditioningMode(getConditioningMode(settings,address));
        controls.setUnoccupiedMode(isInUnOccupiedMode(equipRef));
        controls.setOperatingMode(getOperatingMode(equipRef));

        Log.i(L.TAG_CCU_DEVICE,
                "Desired Heat temp "+((int)getDesiredTempHeating(equipRef, mode) * 2)+
                 "\n Desired Cool temp "+((int)getDesiredTempCooling(equipRef, mode) * 2)+
                 "\n DeviceFanMode "+getDeviceFanMode(settings).name()+
                 "\n ConditioningMode"+getConditioningMode(settings,address).name()+
                 "\n occupancyMode :"+isInUnOccupiedMode(equipRef)+
                "\n operatingMode :"+controls.getOperatingMode()+
                 "\n occupancyMode :"+isInUnOccupiedMode(equipRef)+
                "\n TemperatureMode :"+mode);
        controls.setFanSpeed(getDeviceFanMode(settings));
        controls.setConditioningMode(getConditioningMode(settings,address));
        controls.setUnoccupiedMode(isInUnOccupiedMode(equipRef));

        if (!device.isEmpty()) {
            DeviceHSUtil.getEnabledCmdPointsWithRefForDevice(device, hayStack).forEach(rawPoint -> {
                double logicalVal = hayStack.readHisValById(rawPoint.getPointRef());
                int mappedVal;
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
        return controls;
    }
    
    public static double getDesiredTempCooling(String equipRef, TemperatureMode mode) {
        HashMap<Object, Object> desiredTempCooling;
        if(mode == TemperatureMode.HEATING){
            desiredTempCooling = CCUHsApi.getInstance().readEntity("desired and temp and " +
                    "heating and equipRef == \"" + equipRef +
                    "\"");
        }else {
            desiredTempCooling = CCUHsApi.getInstance().readEntity("desired and temp and " +
                    "cooling and equipRef == \"" + equipRef +
                    "\"");
        }
        try {
            return CCUHsApi.getInstance().readPointPriorityVal(desiredTempCooling.get("id").toString());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static double getDesiredTempHeating(String equipRef, TemperatureMode mode) {
        HashMap<Object, Object> desiredTempHeating;
        if(mode == TemperatureMode.COOLING){
            desiredTempHeating = CCUHsApi.getInstance().readEntity("desired and temp and " +
                    "cooling and equipRef == \"" + equipRef +
                    "\"");
        }else {
            desiredTempHeating = CCUHsApi.getInstance().readEntity("desired and temp and " +
                    "heating and equipRef == \"" + equipRef +
                    "\"");
        }
        try {
            return CCUHsApi.getInstance().readPointPriorityVal(desiredTempHeating.get("id").toString());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static HyperStat.HyperStatOperatingMode_e getOperatingMode(String equipRef){
            double operatingMode = CCUHsApi.getInstance().readHisValByQuery("operating and mode and equipRef == \"" + equipRef + "\"");
            if (operatingMode == 1) {
                return HyperStat.HyperStatOperatingMode_e.HYPERSTAT_OPERATING_MODE_COOLING;
            }
            if (operatingMode == 2) {
                return HyperStat.HyperStatOperatingMode_e.HYPERSTAT_OPERATING_MODE_HEATING;
            }
        return HyperStat.HyperStatOperatingMode_e.HYPERSTAT_OPERATING_MODE_OFF;
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
    private static boolean isInUnOccupiedMode(String equipRef){
        double curOccuMode = CCUHsApi.getInstance().readHisValByQuery("point and occupancy and mode and equipRef == \""+equipRef+"\"");
        Occupancy curOccupancyMode = Occupancy.values()[(int)curOccuMode];
        return curOccupancyMode == UNOCCUPIED || curOccupancyMode == AUTOAWAY;
    }
        private static int getHumidityMinSp(int address, CCUHsApi hayStack) {
            return hayStack.readDefaultVal("humidifier and control and group == \"" + address + "\"").intValue();
        }

        private static int getHumidityMaxSp(int address, CCUHsApi hayStack) {
            return hayStack.readDefaultVal("dehumidifier and control and group == \"" + address + "\"").intValue();
        }

    private static double getStandaloneCoolingDeadband(String equipRef, TemperatureMode mode) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap collingDeadband = hayStack.read("point and deadband and cooling and not multiplier and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+"\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(collingDeadband.get("id")).toString());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return 0;
        }

    }

    public static double getStandaloneHeatingDeadband(String equipRef, TemperatureMode mode) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap deadbandPoint =
                hayStack.read("point and deadband and heating and not multiplier and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+
                "\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(deadbandPoint.get("id")).toString());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return 0;
        }

    }
    public static double readCo2ThresholdValue(String equipRef) {
        return CCUHsApi.getInstance().readDefaultVal(
                "point and co2 and threshold and equipRef == \""+equipRef+ "\"");
    }

    public static double readVocThresholdValue(String equipRef) {
        return CCUHsApi.getInstance().readDefaultVal(
                "point and voc and threshold and equipRef == \""+equipRef+ "\"");
    }

    public static double readPm2p5ThresholdValue(String equipRef) {
        return CCUHsApi.getInstance().readDefaultVal(
                "point and pm2p5 and threshold and equipRef == \""+equipRef+ "\"");
    }


    public static boolean isDisplayHumidity(String equipRef){
        return CCUHsApi.getInstance().readDefaultVal(
                "point and config and enabled and humidity and equipRef == \""+equipRef+ "\"") == 1;
    }


    public static boolean isDisplayVov(String equipRef){
        return CCUHsApi.getInstance().readDefaultVal(
                "point and config and enabled and voc and equipRef == \""+equipRef+ "\"") == 1;
    }


    public static boolean isDisplayCo2(String equipRef){
        return CCUHsApi.getInstance().readDefaultVal(
                "point and config and enabled and co2 and equipRef == \""+equipRef+ "\"") == 1;
    }


    public static boolean isDisplayP2p5(String equipRef){
        return CCUHsApi.getInstance().readDefaultVal(
                "point and config and enabled and pm2p5 and equipRef == \""+equipRef+ "\"") == 1;
    }

    public static HyperStat.HyperStatSettingsMessage2_t getSetting2Message(int address, String equipRef){
        return  HyperStatSettingsUtil.Companion.getSetting2Message(address,equipRef,CCUHsApi.getInstance());
    }
    public static HyperStat.HyperStatSettingsMessage3_t getSetting3Message(int address, String equipRef){
        return  HyperStatSettingsUtil.Companion.getSetting3Message(address,equipRef);
    }

    public static HyperStatControlsMessage_t getHyperstatRebootControl(int address){
        HashMap<Object,Object> equip = CCUHsApi.getInstance().readEntity("equip and hyperstat" +
                " and group == \"" + address + "\"");
        String equipRef =equip.get("id").toString();
        Log.d(L.TAG_CCU_SERIAL,"Reset set to true");
        int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                " == \"" + equip.get("roomRef").toString() + "\"").intValue();
        return getControlMessage(address ,equipRef, TemperatureMode.values()[modeType]).setReset(true).build();
    }

    public static double getFanMode(String equipRef){
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                    "point and zone and sp and conditioning and mode and equipRef == \"" + equipRef + "\"");
        }catch (NullPointerException e){
            return 0.0;
        }
    }
    public static double getConditioningMode(String equipRef){
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                "point and zone and fan and mode and operation and equipRef == \""+equipRef+ "\"");
        }catch (NullPointerException e){
            return 0.0;
        }
    }

}
