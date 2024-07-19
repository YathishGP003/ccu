package a75f.io.device.mesh.hypersplit;

import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_FOUR;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_ONE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_THREE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_TWO;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_EIGHT;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_FIVE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_FOUR;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_ONE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_SEVEN;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_SIX;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_THREE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_TWO;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.UNOCCUPIED;


import com.google.protobuf.ByteString;

import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.device.HyperSplit;
import a75f.io.device.mesh.DeviceHSUtil;
import a75f.io.device.mesh.DeviceUtil;
import a75f.io.device.util.DeviceConfigurationUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.util.TemperatureMode;

public class HyperSplitMessageGenerator {

    /**
     * Generates seed message for a node from haystack data.
     * @param zone
     * @param address
     * @param equipRef
     * @return
     */
    public static HyperSplit.HyperSplitCcuDatabaseSeedMessage_t getSeedMessage(String zone, int address,
                                                                             String equipRef, TemperatureMode temperatureMode) {
        // HSS Seed message has a Settings3 field, but it is not filled anymore. This is to prevent edge cases that result from excessive message length.
        // Settings3 is now sent separately from the Seed Message.
        HyperSplit.HyperSplitSettingsMessage_t hyperSplitSettingsMessage_t = getSettingsMessage(zone, address,
                equipRef, temperatureMode);
        HyperSplit.HyperSplitControlsMessage_t hyperSplitControlsMessage_t = getControlMessage(address,
                equipRef, temperatureMode).build();
        HyperSplit.HyperSplitSettingsMessage2_t hyperSplitSettingsMessage2_t = getSetting2Message(address, equipRef);
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitSettingsMessage_t.toByteString().toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitControlsMessage_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitSettingsMessage2_t);

        return HyperSplit.HyperSplitCcuDatabaseSeedMessage_t.newBuilder()
                .setEncryptionKey(ByteString.copyFrom(L.getEncryptionKey()))
                .setSerializedSettingsData(hyperSplitSettingsMessage_t.toByteString())
                .setSerializedHyperSplitControlsData(hyperSplitControlsMessage_t.toByteString())
                .setSerializedHyperSplitSettings2Data(hyperSplitSettingsMessage2_t.toByteString())
                .build();

    }

    /**
     * Generate settings message for a node from haystack data.
     * @param zone
     * @param address
     * @param equipRef
     * @return
     */
    public static HyperSplit.HyperSplitSettingsMessage_t getSettingsMessage(String zone, int address,
                                                                          String equipRef, TemperatureMode mode) {
        boolean singleMode = mode == TemperatureMode.HEATING || mode == TemperatureMode.COOLING;

        int minCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getCoolingUserLimitByQuery(mode, "min", equipRef)).intValue();
        int maxCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getCoolingUserLimitByQuery(mode, "max", equipRef)).intValue();
        int minHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getHeatingUserLimitByQuery(mode, "min", equipRef)).intValue();
        int maxHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getHeatingUserLimitByQuery(mode, "max", equipRef)).intValue();

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


        HyperSplit.HyperSplitSettingsMessage_t.Builder msg = HyperSplit.HyperSplitSettingsMessage_t.newBuilder()
                .setRoomName(zone)
                .setHeatingDeadBand((int) (getStandaloneHeatingDeadband(equipRef, mode) * 10))
                .setCoolingDeadBand((int) (getStandaloneCoolingDeadband(equipRef) * 10))
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
                .setPm25AlertTarget((int)readPm2p5TargetValue(equipRef))
                .setVocAlertTarget((int)readVocThresholdValue(equipRef))
                .setTemperatureMode(singleMode ? HyperSplit.HyperSplitTemperatureMode_e.HYPERSPLIT_TEMP_MODE_SINGLE
                        : HyperSplit.HyperSplitTemperatureMode_e.HYPERSPLIT_TEMP_MODE_DUAL_VARIABLE_DB)
                .setHyperstatLinearFanSpeeds(HyperSplitSettingsUtil.Companion.getLinearFanSpeedDetails(equipRef))
                .setHyperstatStagedFanSpeeds(HyperSplitSettingsUtil.Companion.getStagedFanSpeedDetails(equipRef));

        return msg.build();

    }

    /**
     * Generate control message for a node from haystack data.
     * @param address
     * @param equipRef
     * @return
     */
    public static HyperSplit.HyperSplitControlsMessage_t.Builder getControlMessage(int address, String equipRef,
                                                                                 TemperatureMode mode) {

        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \"" + address + "\"");

        HyperSplit.HyperSplitControlsMessage_t.Builder controls = HyperSplit.HyperSplitControlsMessage_t.newBuilder();
        controls.setSetTempCooling((int)(getDesiredTempCooling(equipRef, mode) * 2));
        controls.setSetTempHeating((int)(getDesiredTempHeating(equipRef, mode) * 2));

        BasicSettings settings = new BasicSettings(
                StandaloneConditioningMode.values()[(int)getConditioningMode(equipRef)],
                StandaloneFanStage.values()[(int)getFanMode(equipRef)]
        );
        controls.setFanSpeed(getDeviceFanMode(settings));
        controls.setConditioningMode(getConditioningMode(settings,address));
        controls.setUnoccupiedMode(isInUnOccupiedMode(equipRef));
        controls.setOperatingMode(getOperatingMode(equipRef));
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
                setHyperSplitPort(controls, Port.valueOf(rawPoint.getPort()), mappedVal);
            });
            CcuLog.i(L.TAG_CCU_DEVICE, "===================Device Layer==================================");
            DeviceHSUtil.getEnabledCmdPointsWithRefForDevice(device, hayStack)
                    .forEach( rawPoint -> {
                        double logicalVal = hayStack.readHisValById(rawPoint.getPointRef());
                        int mappedVal = (DeviceUtil.isAnalog(rawPoint.getPort())
                                ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                                : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));
                        hayStack.writeHisValById(rawPoint.getId(), (double) mappedVal);
                        CcuLog.i(L.TAG_CCU_DEVICE,
                                rawPoint.getType()+" "+logicalVal+" Port "+rawPoint.getPort() +" =  "+mappedVal);
                        setHyperSplitPort(controls, Port.valueOf(rawPoint.getPort()), mappedVal);
                    });
            CcuLog.i(L.TAG_CCU_DEVICE, "=====================================================");
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
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getDesiredTempCooling", e);
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
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getDesiredTempHeating", e);
        }
        return 0;
    }

    private static HyperSplit.HyperSplitOperatingMode_e getOperatingMode(String equipRef){
        double operatingMode = CCUHsApi.getInstance().readHisValByQuery("operating and mode and equipRef == \"" + equipRef + "\"");
        if (operatingMode == 1) {
            return HyperSplit.HyperSplitOperatingMode_e.HYPERSPLIT_OPERATING_MODE_COOLING;
        }
        if (operatingMode == 2) {
            return HyperSplit.HyperSplitOperatingMode_e.HYPERSPLIT_OPERATING_MODE_HEATING;
        }
        return HyperSplit.HyperSplitOperatingMode_e.HYPERSPLIT_OPERATING_MODE_OFF;
    }

    private static void setHyperSplitPort(HyperSplit.HyperSplitControlsMessage_t.Builder controls,
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
        } else if (port == RELAY_SEVEN) {
            controls.setRelay7(val > 0);
        } else if (port == RELAY_EIGHT) {
            controls.setRelay8(val > 0);
        }else if (port == ANALOG_OUT_ONE) {
            controls.setAnalogOut1(getAnalogOutValue(val));
        }else if (port == ANALOG_OUT_TWO) {
            controls.setAnalogOut2(getAnalogOutValue(val));
        }else if (port == ANALOG_OUT_THREE) {
            controls.setAnalogOut3(getAnalogOutValue(val));
        }else if (port == ANALOG_OUT_FOUR) {
            controls.setAnalogOut4(getAnalogOutValue(val));
        }

    }

    private static HyperSplit.HyperSplitAnalogOutputControl_t getAnalogOutValue(double value){
        return HyperSplit.HyperSplitAnalogOutputControl_t.newBuilder().setPercent((int) value).build();
    }

    private static HyperSplit.HyperSplitFanSpeed_e getDeviceFanMode(a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings settings){
        try {
            switch (settings.getFanMode()){
                case OFF: return HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_OFF;
                case AUTO: return HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO;
                case LOW_ALL_TIME:
                case LOW_CUR_OCC:
                case LOW_OCC: return HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW;
                case MEDIUM_ALL_TIME:
                case MEDIUM_CUR_OCC:
                case MEDIUM_OCC: return HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED;
                case HIGH_ALL_TIME:
                case HIGH_CUR_OCC:
                case HIGH_OCC: return HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH;
            }
        }catch (Exception e){
            e.printStackTrace();
            CcuLog.i(L.TAG_CCU_DEVICE, "Exception getDeviceFanMode: ");
        }

        return HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_OFF;
    }

    private static HyperSplit.HyperSplitConditioningMode_e getConditioningMode(BasicSettings settings, int address){
        try {
            if(settings.getConditioningMode() == StandaloneConditioningMode.AUTO)
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_AUTO;
            if(settings.getConditioningMode() == StandaloneConditioningMode.COOL_ONLY)
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_COOLING;
            if(settings.getConditioningMode() == StandaloneConditioningMode.HEAT_ONLY)
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_HEATING;
            else
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_OFF;
        }catch (Exception e){
            CcuLog.i(L.TAG_CCU_DEVICE, "Exception getConditioningMode: ");
            e.printStackTrace();
        }
        return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_OFF;
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

    private static double getStandaloneCoolingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap collingDeadband = hayStack.read("point and deadband and cooling and zone and not multiplier and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+"\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(collingDeadband.get("id")).toString());
        } catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getStandaloneCoolingDeadband",e);
            return 0;
        }

    }

    public static double getStandaloneHeatingDeadband(String equipRef, TemperatureMode mode) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap deadbandPoint =
                hayStack.read("point and deadband and heating and zone and not multiplier and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+
                        "\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(deadbandPoint.get("id")).toString());
        } catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getStandaloneHeatingDeadband", e);
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

    public static double readPm2p5TargetValue(String equipRef) {
        return CCUHsApi.getInstance().readDefaultVal(
                "point and pm2p5 and target and equipRef == \""+equipRef+ "\"");
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

    public static HyperSplit.HyperSplitSettingsMessage2_t getSetting2Message(int address, String equipRef){
        return HyperSplitSettingsUtil.Companion.getSetting2Message(address,equipRef,CCUHsApi.getInstance());
    }
    public static HyperSplit.HyperSplitSettingsMessage3_t getSetting3Message(int address, String equipRef){
        return HyperSplitSettingsUtil.Companion.getSetting3Message(address,equipRef,CCUHsApi.getInstance());
    }
    public static HyperSplit.HyperSplitSettingsMessage4_t getSetting4Message(int address, String equipRef){
        return HyperSplitSettingsUtil.Companion.getSetting4Message(address, equipRef, CCUHsApi.getInstance());
    }

    public static HyperSplit.HyperSplitControlsMessage_t getHypersplitRebootControl(int address){
        HashMap<Object,Object> equip = CCUHsApi.getInstance().readEntity("equip and hyperstatsplit" +
                " and group == \"" + address + "\"");
        String equipRef =equip.get("id").toString();
        CcuLog.d(L.TAG_CCU_SERIAL,"Reset set to true");
        int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                " == \"" + equip.get("roomRef").toString() + "\"").intValue();
        return getControlMessage(address ,equipRef, TemperatureMode.values()[modeType]).setReset(true).build();
    }

    public static double getConditioningMode(String equipRef){
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                    "point and zone and sp and conditioning and mode and equipRef == \"" + equipRef + "\"");
        }catch (NullPointerException e){
            return 0.0;
        }
    }

    public static double getFanMode(String equipRef){
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                    "point and zone and fan and mode and operation and equipRef == \""+equipRef+ "\"");
        }catch (NullPointerException e){
            return 0.0;
        }
    }

}
