package a75f.io.device.mesh.hypersplit;

import static a75f.io.logic.bo.building.definitions.Port.*;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.UNOCCUPIED;

import android.util.Log;

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
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.tuners.TunerUtil;

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
        HyperSplit.HyperSplitSettingsMessage_t hyperSplitSettingsMessage_t = getSettingsMessage(zone, address,
                equipRef, temperatureMode);
        HyperSplit.HyperSplitControlsMessage_t hyperSplitControlsMessage_t = getControlMessage(address,
                equipRef, temperatureMode).build();
        HyperSplit.HyperSplitSettingsMessage2_t hyperSplitSettingsMessage2_t = getSetting2Message(address, equipRef);
        HyperSplit.HyperSplitSettingsMessage3_t hyperSplitSettingsMessage3_t = getSetting3Message(address, equipRef);
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitSettingsMessage_t.toByteString().toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitControlsMessage_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitSettingsMessage2_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitSettingsMessage3_t.toString());

        HyperSplit.HyperSplitCcuDatabaseSeedMessage_t seed = HyperSplit.HyperSplitCcuDatabaseSeedMessage_t.newBuilder()
                .setEncryptionKey(ByteString.copyFrom(L.getEncryptionKey()))
                .setSerializedSettingsData(hyperSplitSettingsMessage_t.toByteString())
                .setSerializedHyperSplitControlsData(hyperSplitControlsMessage_t.toByteString())
                .setSerializedHyperSplitSettings2Data(hyperSplitSettingsMessage2_t.toByteString())
                .setSerializedHyperSplitSettings3Data(hyperSplitSettingsMessage3_t.toByteString())
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
    public static HyperSplit.HyperSplitSettingsMessage_t getSettingsMessage(String zone, int address,
                                                                          String equipRef, TemperatureMode mode) {
        boolean singleMode = false;
        if(mode == TemperatureMode.HEATING || mode == TemperatureMode.COOLING){
            singleMode = true;
        }
        HyperSplit.HyperSplitSettingsMessage_t.Builder msg = HyperSplit.HyperSplitSettingsMessage_t.newBuilder()
                .setRoomName(zone)
                .setHeatingDeadBand((int) (getStandaloneHeatingDeadband(equipRef, mode) * 10))
                .setCoolingDeadBand((int) (getStandaloneCoolingDeadband(equipRef, mode) * 10))
                .setMinCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery(
                        HyperSplitSettingsUtil.Companion.getCoolingUserLimitByQuery(mode, "min")))
                .setMaxCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery(
                        HyperSplitSettingsUtil.Companion.getCoolingUserLimitByQuery(mode, "max")))
                .setMinHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery(
                        HyperSplitSettingsUtil.Companion.getHeatingUserLimitByQuery(mode, "min")))
                .setMaxHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery(
                        HyperSplitSettingsUtil.Companion.getHeatingUserLimitByQuery(mode, "max")))
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
                        : HyperSplit.HyperSplitTemperatureMode_e.HYPERSPLIT_TEMP_MODE_DUAL_VARIABLE_DB);

        Log.i("CCU_HSS_MESSAGE",
                "--------------HyperStat Split CPU & Economiser Settings Message: ------------------\n" +
                        "Node address " + address + "\n" +
                        "roomName " + msg.getRoomName() + "\n" +
                        "maxHeatingUserTemp " + msg.getMaxHeatingUserTemp() + "\n" +
                        "minHeatingUserTemp " + msg.getMinHeatingUserTemp() + "\n" +
                        "maxCoolingUserTemp " + msg.getMaxCoolingUserTemp() + "\n" +
                        "minCoolingUserTemp " + msg.getMinCoolingUserTemp() + "\n" +
                        "TemperatureOffset " + msg.getTemperatureOffset() + "\n" +
                        "HeatingDeadBand " + msg.getHeatingDeadBand() + "\n" +
                        "CoolingDeadBand " + msg.getCoolingDeadBand() + "\n" +
                        "TemperatureMode " + msg.getTemperatureModeValue() + "\n" +
                        "Co2AlertTarget " + msg.getCo2AlertTarget() + "\n" +
                        "VocAlertTarget " + msg.getVocAlertTarget() + "\n" +
                        "Pm25AlertTarget " + msg.getPm25AlertTarget() + "\n" +
                        "DisplayCO2 " + msg.getDisplayCO2() + "\n" +
                        "DisplayPM25 " + msg.getDisplayPM25() + "\n" +
                        "DisplayVOC " + msg.getDisplayVOC() + "\n" +
                        "DisplayHumidity " + msg.getDisplayHumidity() + "\n" +
                        "ShowCentigrade " + msg.getShowCentigrade() + "\n" +
                        "BeaconingEnabled " + msg.getBeaconingEnabled() + "\n" +
                        "HumidityMinSetpoint " + msg.getHumidityMinSetpoint() + "\n" +
                        "HumidityMaxSetpoint " + msg.getHumidityMaxSetpoint() + "\n" +
                        "OccupancySensorSensitivityLevel " + msg.getOccupancySensorSensitivityLevel() + "\n");

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

        // TODO: this BasicSettings object does not accurately reflect the EffectiveConditioningMode.
        // EffectiveConditioningMode is not sent to the HyperSplit as part of the settings message,
        // so I think this is okay. Confirm.
        BasicSettings settings = new BasicSettings(
                StandaloneConditioningMode.values()[(int)getFanMode(equipRef)],
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
                setHyperSplitPort(controls, Port.valueOf(rawPoint.getPort()), mappedVal);
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
                        setHyperSplitPort(controls, Port.valueOf(rawPoint.getPort()), mappedVal);
                    });
            Log.i(L.TAG_CCU_DEVICE, "=====================================================");
        }

        Log.i("CCU_HSS_MESSAGE",
                "--------------HyperStat Split CPU & Economiser Controls Message: ------------------\n" +
                        "Node address " + address + "\n" +
                        "setTemp Heating " +  controls.getSetTempHeating() + "\n" +
                        "setTemp Cooling " +  controls.getSetTempCooling() + "\n" +
                        "conditioningMode " +  controls.getConditioningMode() + "\n" +
                        "Fan Mode " +  controls.getFanSpeed() + "\n" +
                        "Relay1 " +  controls.getRelay1() + "\n" +
                        "Relay2 " +  controls.getRelay2() + "\n" +
                        "Relay3 " +  controls.getRelay3() + "\n" +
                        "Relay4 " +  controls.getRelay4() + "\n" +
                        "Relay5 " +  controls.getRelay5() + "\n" +
                        "Relay6 " +  controls.getRelay6() + "\n" +
                        "Relay7 " +  controls.getRelay7() + "\n" +
                        "Relay8 " +  controls.getRelay8() + "\n" +
                        "Analog Out1 " +  controls.getAnalogOut1().getPercent() + "\n" +
                        "Analog Out2 " +  controls.getAnalogOut2().getPercent() + "\n" +
                        "Analog Out3 " +  controls.getAnalogOut3().getPercent() + "\n" +
                        "Analog Out4 " +  controls.getAnalogOut4().getPercent() + "\n" +
                        "-------------------------------------------------------------");

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
            Log.i(L.TAG_CCU_DEVICE, "Exception getDeviceFanMode: ");
        }

        return HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_OFF;
    }


    /*
        Conditioning Mode written to the HyperSplit will be UserIntentConditioningMode, not EffectiveConditioningMode.

        If the HyperSplit is connected to the CCU, the controls message will reflect any lockouts imposed by the Condensate Switch.

        If the HyperSplit is running standalone, it will consider the status of the Condensate Switch in the profile.

        Using the UserIntentConditioningMode prevents the following situation:
        - EffectiveConditioningMode is OFF due to condensate trip
        - ConditioningMode is communicated from CCU as OFF
        - HyperSplit loses connection to CCU

        In this case, HyperSplit would not operate even if condensate switch returns to normal (because last known
        conditioning mode was OFF).
     */
    private static HyperSplit.HyperSplitConditioningMode_e getConditioningMode(BasicSettings settings, int address){
        try {
            if(settings.getUserIntentConditioningMode() == StandaloneConditioningMode.AUTO)
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_AUTO;
            if(settings.getUserIntentConditioningMode() == StandaloneConditioningMode.COOL_ONLY)
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_COOLING;
            if(settings.getUserIntentConditioningMode() == StandaloneConditioningMode.HEAT_ONLY)
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_HEATING;
            else
                return HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_OFF;
        }catch (Exception e){
            Log.i(L.TAG_CCU_DEVICE, "Exception getConditioningMode: ");
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

    private static double getStandaloneCoolingDeadband(String equipRef, TemperatureMode mode) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap collingDeadband = hayStack.read("point and tuner and deadband and base and cooling and equipRef == \""+equipRef+"\"");
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
                hayStack.read("point and tuner and deadband and base and heating and equipRef == \""+equipRef+
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

    public static HyperSplit.HyperSplitControlsMessage_t getHypersplitRebootControl(int address){
        HashMap<Object,Object> equip = CCUHsApi.getInstance().readEntity("equip and hyperstatsplit" +
                " and group == \"" + address + "\"");
        String equipRef =equip.get("id").toString();
        Log.d(L.TAG_CCU_SERIAL,"Reset set to true");
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
