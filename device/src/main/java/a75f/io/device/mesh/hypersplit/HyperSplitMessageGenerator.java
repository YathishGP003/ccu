package a75f.io.device.mesh.hypersplit;

import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.UNOCCUPIED;

import com.google.protobuf.ByteString;

import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.util.StringUtil;
import a75f.io.device.HyperSplit;
import a75f.io.device.mesh.DeviceHSUtil;
import a75f.io.device.mesh.DeviceUtil;
import a75f.io.device.util.DeviceConfigurationUtil;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.equips.HyperStatSplitEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings;
import a75f.io.logic.tuners.TunerConstants;

public class HyperSplitMessageGenerator {

    public static HyperSplit.HyperSplitCcuDatabaseSeedMessage_t getSeedMessage(String zone, int address,
                                                                             String equipRef) {
        // HSS Seed message has a Settings3 field, but it is not filled anymore. This is to prevent edge cases that result from excessive message length.
        // Settings3 is now sent separately from the Seed Message.
        HyperSplit.HyperSplitSettingsMessage_t hyperSplitSettingsMessage_t = getSettingsMessage(zone,
                equipRef);
        HyperSplit.HyperSplitControlsMessage_t hyperSplitControlsMessage_t = getControlMessage(address,
                equipRef).build();
        HyperSplit.HyperSplitSettingsMessage2_t hyperSplitSettingsMessage2_t = getSetting2Message(equipRef);
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitSettingsMessage_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitControlsMessage_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + hyperSplitSettingsMessage2_t);

        return HyperSplit.HyperSplitCcuDatabaseSeedMessage_t.newBuilder()
                .setEncryptionKey(ByteString.copyFrom(L.getEncryptionKey()))
                .setSerializedSettingsData(hyperSplitSettingsMessage_t.toByteString())
                .setSerializedHyperSplitControlsData(hyperSplitControlsMessage_t.toByteString())
                .setSerializedHyperSplitSettings2Data(hyperSplitSettingsMessage2_t.toByteString())
                .build();

    }

    public static HyperSplit.HyperSplitSettingsMessage_t getSettingsMessage(String zone,
                                                                            String equipRef) {
        int temperatureMode = (int) Domain.readValAtLevelByDomain(DomainName.temperatureMode,
                TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);

        int minCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getCoolingUserLimitByQuery("min", equipRef)).intValue();
        int maxCoolingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getCoolingUserLimitByQuery("max", equipRef)).intValue();
        int minHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getHeatingUserLimitByQuery("min", equipRef)).intValue();
        int maxHeatingUserTemp = CCUHsApi.getInstance().readPointPriorityValByQuery(HyperSplitSettingsUtil.Companion.getHeatingUserLimitByQuery("max", equipRef)).intValue();

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

        HyperStatSplitEquip equip = (HyperStatSplitEquip) Domain.INSTANCE.getDomainEquip(equipRef);
        HyperSplit.HyperSplitSettingsMessage_t.Builder msg = HyperSplit.HyperSplitSettingsMessage_t.newBuilder()
                .setRoomName(zone)
                .setHeatingDeadBand((int) (getStandaloneHeatingDeadband(equipRef) * 10))
                .setCoolingDeadBand((int) (getStandaloneCoolingDeadband(equipRef) * 10))
                .setMinCoolingUserTemp(minCoolingUserTemp)
                .setMaxCoolingUserTemp(maxCoolingUserTemp)
                .setMinHeatingUserTemp(minHeatingUserTemp)
                .setMaxHeatingUserTemp(maxHeatingUserTemp)
                .setTemperatureOffset((int)(equip.getTemperatureOffset().readDefaultVal() * 10))
                .setHumidityMinSetpoint((int)(equip.getTargetHumidifier().readDefaultVal()))
                .setHumidityMaxSetpoint((int)(equip.getTargetDehumidifier().readDefaultVal()))
                .setShowCentigrade(DeviceConfigurationUtil.Companion.getUserConfiguration() == 1)
                .setDisplayHumidity(equip.getEnableHumidityDisplay().readDefaultVal() > 0)
                .setDisplayCO2(equip.getEnableCO2Display().readDefaultVal() > 0)
                .setDisplayPM25(equip.getEnablePm25Display().readDefaultVal() > 0)
                .setCo2AlertTarget((int)equip.getCo2Threshold().readDefaultVal())
                .setPm25AlertTarget((int)equip.getPm25Target().readDefaultVal())
                .setTemperatureMode(temperatureMode == 0 ? HyperSplit.HyperSplitTemperatureMode_e.HYPERSPLIT_TEMP_MODE_DUAL_FIXED_DB
                        : HyperSplit.HyperSplitTemperatureMode_e.HYPERSPLIT_TEMP_MODE_DUAL_VARIABLE_DB)
                .setHyperstatLinearFanSpeeds(HyperSplitSettingsUtil.Companion.getLinearFanSpeedDetails(equip))
                .setHyperstatStagedFanSpeeds(HyperSplitSettingsUtil.Companion.getStagedFanSpeedDetails(equip))
                .setMiscSettings1(HyperSplitSettingsUtil.Companion.getMisSettings(equipRef))
                .setInstallerLockPin(HyperSplitSettingsUtil.Companion.getPin(equip.getPinLockInstallerAccess()))
                .setUserLockPin(HyperSplitSettingsUtil.Companion.getPin(equip.getPinLockConditioningModeFanAccess()));
        return msg.build();

    }

    public static HyperSplit.HyperSplitControlsMessage_t.Builder getControlMessage(int address, String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.readEntity("device and addr == \"" + address + "\"");

        HyperSplit.HyperSplitControlsMessage_t.Builder controls = HyperSplit.HyperSplitControlsMessage_t.newBuilder();
        controls.setSetTempCooling((int)(getDesiredTempCooling(equipRef) * 2));
        controls.setSetTempHeating((int)(getDesiredTempHeating(equipRef) * 2));

        BasicSettings settings = new BasicSettings(
                StandaloneConditioningMode.values()[(int)getConditioningMode(equipRef)],
                StandaloneFanStage.values()[(int)getFanMode(equipRef)]
        );
        controls.setFanSpeed(getDeviceFanMode(settings));
        controls.setConditioningMode(getConditioningMode(settings));
        controls.setUnoccupiedMode(isInUnOccupiedMode(equipRef));
        controls.setOperatingMode(getOperatingMode(equipRef));
        controls.setFanSpeed(getDeviceFanMode(settings));
        controls.setConditioningMode(getConditioningMode(settings));
        controls.setUnoccupiedMode(isInUnOccupiedMode(equipRef));

        if (!device.isEmpty()) {
            DeviceHSUtil.getEnabledCmdPointsWithRefForDevice(device, hayStack).forEach(rawPoint -> {
                int mappedVal;

                double logicalVal = hayStack.readHisValById(rawPoint.getPointRef());
                if(Globals.getInstance().isTestMode()) {
                    // In test mode, we read the historical value and write it back to the point.
                    Double hisVal = hayStack.readHisValById(rawPoint.getId());
                    if (rawPoint.getMarkers().contains(Tags.WRITABLE)) {
                        Double priorityVal = hayStack.readPointPriorityVal(rawPoint.getId());
                        hayStack.writeHisValById(rawPoint.getId(), priorityVal);
                    }
                    setHyperSplitPort(controls, rawPoint, hisVal);
                }
                else {

                    if (Globals.getInstance().isTemporaryOverrideMode()) {
                        mappedVal = (short) logicalVal;
                    } else {
                        mappedVal = (DeviceUtil.isAnalog(rawPoint)
                                ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                                : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));
                    }
                    if (rawPoint.getMarkers().contains(Tags.WRITABLE)) {
                        hayStack.writeDefaultVal("id==" + StringUtil.addAtSymbolIfMissing(rawPoint.getId()), (double) mappedVal);
                        double value = hayStack.readPointPriorityVal(rawPoint.getId());
                        hayStack.writeHisValById(rawPoint.getId(), value);
                    } else {
                        hayStack.writeHisValById(rawPoint.getId(), (double) mappedVal);
                    }
                    setHyperSplitPort(controls, rawPoint, mappedVal);
                }});
            DeviceHSUtil.getEnabledCmdPointsWithRefForDevice(device, hayStack)
                    .forEach( rawPoint -> {
                        int mappedVal;

                        // Points written from CCU algos will fall under this block.
                        // Updating the physical point value based on logical point value is done here.
                        if(!Globals.getInstance().isTestMode()) {
                            if (!rawPoint.getMarkers().contains(Tags.WRITABLE)) {
                                double logicalVal = hayStack.readHisValById(rawPoint.getPointRef());
                                CcuLog.d(L.TAG_CCU_DEVICE, "test-writable READ hs split $$getControlMessage: not writable id->" + rawPoint.getId() + "<logicalVal:>" + logicalVal);
                                mappedVal = (DeviceUtil.isAnalog(rawPoint)
                                        ? DeviceUtil.mapAnalogOut(rawPoint.getType(), (short) logicalVal)
                                        : DeviceUtil.mapDigitalOut(rawPoint.getType(), logicalVal > 0));
                                CcuLog.i(L.TAG_CCU_DEVICE,
                                        rawPoint.getType() + " " + logicalVal + " domainName " + rawPoint.getDomainName() + " =  " + mappedVal);
                                CcuLog.d(L.TAG_CCU_DEVICE, "test-writable READ hs split $$getControlMessage: not writable id->" + rawPoint.getId() + "<mappedVal:>" + mappedVal);
                            } else {
                                // Points written from Sequencer will fall under this block.
                                // Sequencer writes directly to the physical point, so we just need to read it here.
                                mappedVal = (short) hayStack.readPointPriorityVal(rawPoint.getId());
                                CcuLog.d(L.TAG_CCU_DEVICE, "test-writable READ hs split $$getControlMessage: writable id->" + rawPoint.getId() + "<mappedVal:>" + mappedVal);
                                CcuLog.i(L.TAG_CCU_DEVICE,
                                        rawPoint.getType() + " writing externally mapped val: domainName " + rawPoint.getDomainName() + " =  " + mappedVal);
                            }
                            CcuLog.d(L.TAG_CCU_DEVICE, "test-writable WRITE hs split $$getControlMessage: writeHisValById id->" + rawPoint.getId() + "<mappedVal:>" + mappedVal);
                            hayStack.writeHisValById(rawPoint.getId(), (double) mappedVal);
                            setHyperSplitPort(controls, rawPoint, mappedVal);
                        }});
        }
        return controls;
    }

    public static double getDesiredTempCooling(String equipRef) {
        HashMap<Object, Object> desiredTempCooling;
        desiredTempCooling = CCUHsApi.getInstance().readEntity(
                "point and domainName == \"" + DomainName.desiredTempCooling + "\" and equipRef == \"" + equipRef + "\""
        );
        try {
            return CCUHsApi.getInstance().readPointPriorityVal(desiredTempCooling.get("id").toString());
        } catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getDesiredTempCooling", e);
        }
        return 0;
    }

    public static double getDesiredTempHeating(String equipRef) {
        HashMap<Object, Object> desiredTempHeating;
        desiredTempHeating = CCUHsApi.getInstance().readEntity(
                "point and domainName == \"" + DomainName.desiredTempHeating + "\" and equipRef == \"" + equipRef + "\""
        );
        try {
            return CCUHsApi.getInstance().readPointPriorityVal(desiredTempHeating.get("id").toString());
        } catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getDesiredTempHeating", e);
        }
        return 0;
    }

    private static HyperSplit.HyperSplitOperatingMode_e getOperatingMode(String equipRef){
        double operatingMode = CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.operatingMode + "\" and equipRef == \"" + equipRef + "\"");
        if (operatingMode == 1) {
            return HyperSplit.HyperSplitOperatingMode_e.HYPERSPLIT_OPERATING_MODE_COOLING;
        }
        if (operatingMode == 2) {
            return HyperSplit.HyperSplitOperatingMode_e.HYPERSPLIT_OPERATING_MODE_HEATING;
        }
        return HyperSplit.HyperSplitOperatingMode_e.HYPERSPLIT_OPERATING_MODE_OFF;
    }

    private static void setHyperSplitPort(HyperSplit.HyperSplitControlsMessage_t.Builder controls,
                                          RawPoint rawPoint, double val) {

        if (rawPoint.getDomainName().equals(DomainName.relay1)) {
            controls.setRelay1(val > 0);
        } else if (rawPoint.getDomainName().equals(DomainName.relay2)) {
            controls.setRelay2(val > 0);
        } else if (rawPoint.getDomainName().equals(DomainName.relay3)) {
            controls.setRelay3(val > 0);
        } else if (rawPoint.getDomainName().equals(DomainName.relay4)) {
            controls.setRelay4(val > 0);
        } else if (rawPoint.getDomainName().equals(DomainName.relay5)) {
            controls.setRelay5(val > 0);
        } else if (rawPoint.getDomainName().equals(DomainName.relay6)) {
            controls.setRelay6(val > 0);
        } else if (rawPoint.getDomainName().equals(DomainName.relay7)) {
            controls.setRelay7(val > 0);
        } else if (rawPoint.getDomainName().equals(DomainName.relay8)) {
            controls.setRelay8(val > 0);
        }else if (rawPoint.getDomainName().equals(DomainName.analog1Out)) {
            controls.setAnalogOut1(getAnalogOutValue(val));
        }else if (rawPoint.getDomainName().equals(DomainName.analog2Out)) {
            controls.setAnalogOut2(getAnalogOutValue(val));
        }else if (rawPoint.getDomainName().equals(DomainName.analog3Out)) {
            controls.setAnalogOut3(getAnalogOutValue(val));
        }else if (rawPoint.getDomainName().equals(DomainName.analog4Out)) {
            controls.setAnalogOut4(getAnalogOutValue(val));
        }

    }

    private static HyperSplit.HyperSplitAnalogOutputControl_t getAnalogOutValue(double value){
        // Since users can write any value they want from Sequencer, we trim it to 0-10v here
        return HyperSplit.HyperSplitAnalogOutputControl_t.newBuilder().setPercent((int) Math.min((Math.max(value, 0)), 100)).build();
    }

    private static HyperSplit.HyperSplitFanSpeed_e getDeviceFanMode(BasicSettings settings){
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

    private static HyperSplit.HyperSplitConditioningMode_e getConditioningMode(BasicSettings settings){
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
        double curOccuMode = CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.occupancyMode + "\" and equipRef == \""+equipRef+"\"");
        Occupancy curOccupancyMode = Occupancy.values()[(int)curOccuMode];
        return curOccupancyMode == UNOCCUPIED || curOccupancyMode == AUTOAWAY;
    }

    private static double getStandaloneCoolingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap collingDeadband = hayStack.readEntity("point and deadband and cooling and zone and not multiplier and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+"\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(collingDeadband.get("id")).toString());
        } catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getStandaloneCoolingDeadband",e);
            return 0;
        }

    }

    public static double getStandaloneHeatingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap deadbandPoint =
                hayStack.readEntity("point and deadband and heating and zone and not multiplier and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+
                        "\"");
        try {
            return HSUtil.getPriorityVal(Objects.requireNonNull(deadbandPoint.get("id")).toString());
        } catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DEVICE,"Error getStandaloneHeatingDeadband", e);
            return 0;
        }
    }


    public static HyperSplit.HyperSplitSettingsMessage2_t getSetting2Message(String equipRef){
        return HyperSplitSettingsUtil.Companion.getSetting2Message(equipRef);
    }
    public static HyperSplit.HyperSplitSettingsMessage3_t getSetting3Message(int address, String equipRef){
        return HyperSplitSettingsUtil.Companion.getSetting3Message(address,equipRef,CCUHsApi.getInstance());
    }
    public static HyperSplit.HyperSplitSettingsMessage4_t getSetting4Message(String equipRef){
        return HyperSplitSettingsUtil.Companion.getSetting4Message(equipRef);
    }

    public static HyperSplit.HyperSplitControlsMessage_t getHypersplitRebootControl(int address){
        HashMap<Object,Object> equip = CCUHsApi.getInstance().readEntity("equip and hyperstatsplit" +
                " and group == \"" + address + "\"");
        String equipRef =equip.get("id").toString();
        CcuLog.d(L.TAG_CCU_SERIAL,"Reset set to true");
        return getControlMessage(address ,equipRef).setReset(true).build();
    }

    public static double getConditioningMode(String equipRef){
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                    "point and domainName == \"" + DomainName.conditioningMode + "\" and equipRef == \"" + equipRef + "\"");
        }catch (NullPointerException e){
            return 0.0;
        }
    }

    public static double getFanMode(String equipRef){
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                    "point and domainName == \"" + DomainName.fanOpMode + "\" and equipRef == \""+equipRef+ "\"");
        }catch (NullPointerException e){
            return 0.0;
        }
    }

}
