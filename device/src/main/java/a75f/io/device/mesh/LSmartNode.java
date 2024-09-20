package a75f.io.device.mesh;


import static a75f.io.device.mesh.MeshUtil.getSetTemp;
import static a75f.io.device.serial.DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT;
import static a75f.io.logic.L.TAG_CCU_DEVICE;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettings2Message_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.device.serial.CondensateSensor_t;
import a75f.io.device.serial.DamperActuator_t;
import a75f.io.device.serial.DamperShape_t;
import a75f.io.device.serial.InputSensorType_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.ProfileMap_t;
import a75f.io.device.serial.SmartNodeControls_t;
import a75f.io.device.serial.SmartNodeSettings2_t;
import a75f.io.device.serial.SmartNodeSettings_t;
import a75f.io.device.util.DeviceConfigurationUtil;
import a75f.io.domain.BypassDamperEquip;
import a75f.io.domain.VavAcbEquip;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.equips.VavEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public class LSmartNode
{
    public static final String ANALOG_OUT_ONE = "ANALOG_OUT_ONE";
    public static final String ANALOG_OUT_TWO = "ANALOG_OUT_TWO";
    public static final String ANALOG_IN_ONE = "ANALOG_IN_ONE";
    public static final String ANALOG_IN_TWO = "ANALOG_IN_TWO";
    
    public static final String RELAY_ONE ="RELAY_ONE";
    public static final String RELAY_TWO ="RELAY_TWO";
    public static final String PULSE ="Pulsed Electric";
    public static final String MAT ="Smart Damper";


    public static CcuToCmOverUsbSnControlsMessage_t getControlMessage(Zone zone, short node, String equipRef)
    {
        CcuToCmOverUsbSnControlsMessage_t controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
        controlsMessage_t.smartNodeAddress.set(node);
        controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
        fillSmartNodeControls(controlsMessage_t.controls,zone,node,equipRef);
        return controlsMessage_t;
    }
    
    public static CcuToCmOverUsbDatabaseSeedSnMessage_t getSeedMessage(Zone zone, short address,String equipRef,String profile)
    {
        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
        seedMessage.smartNodeAddress.set(address);
        seedMessage.putEncrptionKey(L.getEncryptionKey());
        fillSmartNodeSettings(seedMessage.settings,zone,address,equipRef,profile);
        fillCurrentUpdatedTime(seedMessage.controls);
        fillSmartNodeControls(seedMessage.controls,zone,address,equipRef);
        return seedMessage;
    }
    public static CcuToCmOverUsbSnSettingsMessage_t getSettingsMessage(Zone zone, short address, String equipRef,String profile)
    {
        CcuToCmOverUsbSnSettingsMessage_t settingsMessage =
                new CcuToCmOverUsbSnSettingsMessage_t();
        settingsMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS);
        settingsMessage.smartNodeAddress.set(address);
        fillSmartNodeSettings(settingsMessage.settings,zone,address,equipRef, profile);
        return settingsMessage;
    }
    private static void fillSmartNodeSettings(SmartNodeSettings_t settings,Zone zone, short address, String equipRef,String profile) {

        try
        {
            double coolingDeadband =
                    CCUHsApi.getInstance().readPointPriorityValByQuery("cooling and deadband and schedulable and roomRef == \""+zone.getId()+"\"");
            settings.maxUserTem.set(DeviceUtil.getMaxUserTempLimits(coolingDeadband,zone.getId()));
    
            double heatingDeadband =
                    CCUHsApi.getInstance().readPointPriorityValByQuery("heating and deadband and schedulable and roomRef == \""+zone.getId()+"\"");
            
            settings.minUserTemp.set(DeviceUtil.getMinUserTempLimits(heatingDeadband, zone.getId()));
        } catch (Exception e) {
            //Equips not having user temps are bound to throw exception
            settings.maxUserTem.set((short) 75);
            settings.minUserTemp.set((short) 69);
        }

        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(equipRef);
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();

        if (profile.equals("bypass")) {
            settings.minDamperOpen.set(Short.parseShort(String.valueOf(CCUHsApi.getInstance().readDefaultVal("point and domainName == \"" + DomainName.damperMinPosition + "\" and equipRef == \"" + equip.getId() + "\"").intValue())));
            settings.maxDamperOpen.set(Short.parseShort(String.valueOf(CCUHsApi.getInstance().readDefaultVal("point and domainName == \"" + DomainName.damperMaxPosition + "\" and equipRef == \"" + equip.getId() + "\"").intValue())));
        } else if (equip.getProfile().equals("PLC")) {
            settings.minDamperOpen.set(Short.parseShort(String.valueOf(10*CCUHsApi.getInstance().readDefaultVal("point and config and analog1 and min and output and group == \"" + address + "\"").intValue())));
            settings.maxDamperOpen.set(Short.parseShort(String.valueOf(10*CCUHsApi.getInstance().readDefaultVal("point and config and analog1 and max and output and group == \"" + address + "\"").intValue())));
        } else if (isEquipType("vav", address) && TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), equipRef)) {
            // VAVs with TrueCFM enabled only have heating min/max damper positions
            // These will only apply when System is Heating. When System is Cooling, CFM loop runs on edge with no damper limits.
            settings.maxDamperOpen.set((short)getDamperLimit("heating", "max", address));
            settings.minDamperOpen.set((short)getDamperLimit("heating", "min", address));
        } else {
            if (getStatus(address) == ZoneState.HEATING.ordinal()) {
                settings.maxDamperOpen.set((short)getDamperLimit("heating", "max", address));
                settings.minDamperOpen.set((short)getDamperLimit("heating", "min", address));
            } else {
                settings.maxDamperOpen.set((short)getDamperLimit("cooling", "max", address));
                settings.minDamperOpen.set((short)getDamperLimit("cooling", "min", address));
            }
        }

         boolean isVav = equip.getProfile().equals(ProfileType.VAV_REHEAT.name())
                || equip.getProfile().equals(ProfileType.VAV_SERIES_FAN.name())
                || equip.getProfile().equals(ProfileType.VAV_PARALLEL_FAN.name())
                || equip.getProfile().equals(ProfileType.VAV_ACB.name())
                || equip.getProfile().equals(ProfileType.VAV_REHEAT.name());

        boolean isDab = equip.getProfile().equals(ProfileType.DAB.name());

        if (isVav || isDab) {
            settings.temperatureOffset.set((short)(10*getTempOffset(address)));
        } else {
            settings.temperatureOffset.set((short)(getTempOffset(address)));
        }

        
        if(profile == null)
            profile = "dab";

        switch (profile){
            case "dab":
                settings.profileBitmap.dynamicAirflowBalancing.set((short)1);
                setupDamperType(address, settings);
                break;
            case "lcm":
                settings.profileBitmap.lightingControl.set((short)1);
                break;
            case "oao":
                settings.profileBitmap.outsideAirOptimization.set((short)1);
                break;
            case "sse":
                settings.profileBitmap.singleStageEquipment.set((short)1);
                break;
            case "iftt":
                settings.profileBitmap.customControl.set((short)1);
                break;
            case "bypass":
                setupBypassDamperActuator(settings, equip.getId());

        }
        settings.roomName.set(zone.getDisplayName());
        
        settings.forwardMotorBacklash.set((short)5);
        settings.reverseMotorBacklash.set((short)5);

        String equipId = SystemTemperatureUtil.getEquip(address).getId();
        if (profile.equals("bypass")) {
            try {
                settings.proportionalConstant.set((short)(TunerUtil.getProportionalGain(equipId) * 100));
                settings.integralConstant.set((short)(TunerUtil.getIntegralGain(equipId) * 100));
                settings.proportionalTemperatureRange.set((short)20);
                settings.integrationTime.set((short)TunerUtil.getIntegralTimeout(equipId));
            } catch (Exception e) {
                //Equips not having PI tuners are bound to throw exception
                settings.proportionalConstant.set((short)50);
                settings.integralConstant.set((short)50);
                settings.proportionalTemperatureRange.set((short)15);
                settings.integrationTime.set((short)30);
            }
        } else {
            try {
                settings.proportionalConstant.set((short)(TunerUtil.getProportionalGain(equipId) * 100));
                settings.integralConstant.set((short)(TunerUtil.getIntegralGain(equipId) * 100));
                settings.proportionalTemperatureRange.set((short)(TunerUtil.getProportionalSpread(equipId) * 10));
                settings.integrationTime.set((short)TunerUtil.getIntegralTimeout(equipId));
            } catch (Exception e) {
                //Equips not having PI tuners are bound to throw exception
                settings.proportionalConstant.set((short)50);
                settings.integralConstant.set((short)50);
                settings.proportionalTemperatureRange.set((short)15);
                settings.integrationTime.set((short)30);
            }
        }
        
        settings.airflowHeatingTemperature.set((short)105);
        settings.airflowCoolingTemperature.set((short)60);
    
        settings.showCentigrade.set((short)(DeviceConfigurationUtil.Companion.getUserConfiguration()));
        settings.displayHold.set((short)0);
        settings.militaryTime.set((short)0);
        try
        {
            settings.enableOccupationDetection.set((short) getConfigNumVal("enable and occupancy", address));
        } catch (Exception e) {
            settings.enableOccupationDetection.set((short)0);
        }
    }

    public static CcuToCmOverUsbSnSettings2Message_t getSettings2Message(Zone zone, short address, String equipRef, String profile)
    {
        CcuToCmOverUsbSnSettings2Message_t settingsMessage =
                new CcuToCmOverUsbSnSettings2Message_t();
        settingsMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS2);
        settingsMessage.smartNodeAddress.set(address);
        fillSmartNodeSettings2(settingsMessage.settings2,zone,address,equipRef, profile);
        return settingsMessage;
    }
    private static void fillSmartNodeSettings2(SmartNodeSettings2_t settings2, Zone zone, short address, String equipRef, String profile) {

        int kFactor = 200;
        int minCFMCooling = 50;
        int maxCFMCooling = 250;
        int minCFMReheating = 50;
        int maxCFMReheating = 250;
        DamperShape_t damperShape = DamperShape_t.DAMPER_SHAPE_ROUND;
        CondensateSensor_t condensateSensor = CondensateSensor_t.CONDENSATE_SENSOR_NORMALLY_OPEN;
        int damperSize = 4;
        int airflowCFMProportionalRange = 200;
        int airflowCFMProportionalKFactor = 50;
        int airflowCFMIntegralTime = 30;
        int airflowCFMIntegralKFactor = 50;
        int enableCFM = 0;

        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(equipRef);
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();

        settings2.profileMap2.set(getProfileMap2(equip.getProfile()));

        if (equip.getProfile().equals("VAV_ACB")) {
            VavAcbEquip acbEquip = new VavAcbEquip(equipRef);
            kFactor = (int) (100 * acbEquip.getKFactor().readPriorityVal());
            minCFMCooling = (int) (acbEquip.getMinCFMCooling().readPriorityVal());
            maxCFMCooling = (int) (acbEquip.getMaxCFMCooling().readPriorityVal());
            minCFMReheating = (int) (acbEquip.getMinCFMReheating().readPriorityVal());
            maxCFMReheating = (int) (acbEquip.getMaxCFMReheating().readPriorityVal());
            damperShape = DamperShape_t.values()[(int) (acbEquip.getDamperShape().readPriorityVal())];
            condensateSensor = CondensateSensor_t.values()[(int) (acbEquip.getThermistor2Type().readPriorityVal())];
            damperSize = getDamperSizeInInches((int) (acbEquip.getDamperSize().readPriorityVal()));
            airflowCFMProportionalRange = (int) (acbEquip.getVavAirflowCFMProportionalRange().readPriorityVal()); // fallback
            airflowCFMProportionalKFactor = (int) (100 * acbEquip.getVavAirflowCFMProportionalKFactor().readPriorityVal()); // fallback
            airflowCFMIntegralTime = (int) (acbEquip.getVavAirflowCFMIntegralTime().readPriorityVal()); // fallback
            airflowCFMIntegralKFactor = (int) (100 * acbEquip.getVavAirflowCFMIntegralKFactor().readPriorityVal()); // fallback
            enableCFM = (int) (acbEquip.getEnableCFMControl().readPriorityVal());

            settings2.relayBitmap.relay1.set((int) acbEquip.getRelay1OutputAssociation().readDefaultVal());
            // Relay 2 will always be zero, because relay 2 is not used in ACB and no relay 2 association point will exist

        } else if (isEquipType("vav", address)) {
            VavEquip vavEquip = new VavEquip(equipRef);
            kFactor = (int) (100 * vavEquip.getKFactor().readPriorityVal());
            minCFMCooling = (int) (vavEquip.getMinCFMCooling().readPriorityVal());
            maxCFMCooling = (int) (vavEquip.getMaxCFMCooling().readPriorityVal());
            minCFMReheating = (int) (vavEquip.getMinCFMReheating().readPriorityVal());
            maxCFMReheating = (int) (vavEquip.getMaxCFMReheating().readPriorityVal());
            damperShape = DamperShape_t.values()[(int) (vavEquip.getDamperShape().readPriorityVal())];
            damperSize = getDamperSizeInInches((int) (vavEquip.getDamperSize().readPriorityVal()));
            airflowCFMProportionalRange = (int) (vavEquip.getVavAirflowCFMProportionalRange().readPriorityVal()); // fallback
            airflowCFMProportionalKFactor = (int) (100 * vavEquip.getVavAirflowCFMProportionalKFactor().readPriorityVal()); // fallback
            airflowCFMIntegralTime = (int) (vavEquip.getVavAirflowCFMIntegralTime().readPriorityVal()); // fallback
            airflowCFMIntegralKFactor = (int) (100 * vavEquip.getVavAirflowCFMIntegralKFactor().readPriorityVal()); // fallback
            enableCFM = (int) (vavEquip.getEnableCFMControl().readPriorityVal());

        } else if (equip.getProfile().equals("PLC")) {
            CCUHsApi hsApi = CCUHsApi.getInstance();
            settings2.inputSensor1.set(getInputSensor1(hsApi, address));
            InputSensorType_t inputSensor2 = getInputSensor2(hsApi.readDefaultVal("point and config and analog2 and sensor and group == \""+address+"\"").intValue());
            settings2.inputSensor2.set(inputSensor2);

            double rawSpSensorOffset = hsApi.readDefaultVal("point and config and setpoint and sensor and offset and group == \""+address+"\"");
            settings2.setpointSensorOffset.set(getInputSensor1Multiplier(inputSensor2, rawSpSensorOffset));

            settings2.genericPiProportionalRange.set(hsApi.readDefaultVal("point and config and pid and prange and group == \""+address+"\"").shortValue());
            settings2.turnOnRelay1.set(hsApi.readDefaultVal("point and config and relay1 and on and threshold and group == \""+address+"\"").shortValue());
            settings2.turnOnRelay2.set(hsApi.readDefaultVal("point and config and relay2 and on and threshold and group == \""+address+"\"").shortValue());
            settings2.turnOffRelay1.set(hsApi.readDefaultVal("point and config and relay1 and off and threshold and group == \""+address+"\"").shortValue());
            settings2.turnOffRelay2.set(hsApi.readDefaultVal("point and config and relay2 and off and threshold and group == \""+address+"\"").shortValue());
            settings2.expectedZeroErrorAtMidpoint.set(hsApi.readDefaultVal("point and config and zero and error and midpoint and group == \""+address+"\"").shortValue());
            settings2.invertControlLoopOutput.set(hsApi.readDefaultVal("point and config and control and loop and inversion and group == \""+address+"\"").shortValue());
            settings2.useAnalogIn2ForDynamicSetpoint.set(hsApi.readDefaultVal("point and config and analog2 and setpoint and enabled and group == \""+address+"\"").shortValue());
            settings2.relay1Enable.set(hsApi.readDefaultVal("point and config and relay1 and enabled and group == \""+address+"\"").shortValue());
            settings2.relay2Enable.set(hsApi.readDefaultVal("point and config and relay2 and enabled and group == \""+address+"\"").shortValue());
            // PI Loop runs all the time for PI Profile
            settings2.runPILoopOnNode.set((short)0);
        } else if (equip.getProfile().equals("BYPASS_DAMPER")) {
            BypassDamperEquip bdEquip = new BypassDamperEquip(equipRef);
            settings2.inputSensor1.set(bdEquip.getPressureSensorType().readDefaultVal() > 0 ? InputSensorType_t.INPUT_SENSOR_GENERIC_0_10V : InputSensorType_t.INPUT_SENSOR_NATIVE_PRESSURE);
            settings2.genericPiProportionalRange.set((short)(10*bdEquip.getExpectedPressureError().readDefaultVal()));
            // systemFanLoopOutput > 0 is the condition for enabling the PI on the CCU; send this same value to the node here
            settings2.runPILoopOnNode.set(CCUHsApi.getInstance().readHisValByQuery("point and fan and system and loop and output and not tuner") > 0.0 ? (short)1 : (short)0);
            settings2.minVolt.set((short)(10*bdEquip.getSensorMinVoltageOutput().readDefaultVal()));
            settings2.maxVolt.set((short)(10*bdEquip.getSensorMaxVoltageOutput().readDefaultVal()));
            settings2.minEngVal.set((short)(10*bdEquip.getPressureSensorMinVal().readDefaultVal()));
            settings2.maxEngVal.set((short)(10*bdEquip.getPressureSensorMaxVal().readDefaultVal()));
        }

        settings2.kFactor.set(enableCFM > 0 ? kFactor : 200);
        settings2.minCFMCooling.set(enableCFM > 0 ? minCFMCooling : 50);
        settings2.maxCFMCooling.set(enableCFM > 0 ? maxCFMCooling : 250);
        settings2.minCFMReheating.set(enableCFM > 0 ? minCFMReheating : 50);
        settings2.maxCFMReheating.set(enableCFM > 0 ? maxCFMReheating : 250);
        settings2.damperShape.set(damperShape);
        settings2.condensateSensor.set(condensateSensor);
        settings2.damperSize.set((short)damperSize);
        settings2.airflowCFMProportionalRange.set(enableCFM > 0 ? airflowCFMProportionalRange : 200);
        settings2.airflowCFMProportionalKFactor.set(enableCFM > 0 ? (short)airflowCFMProportionalKFactor : (short)50);
        settings2.airflowCFMIntegralTime.set(enableCFM > 0 ? (short)airflowCFMIntegralTime : (short)30);
        settings2.airflowCFMIntegralKFactor.set(enableCFM > 0 ? (short)airflowCFMIntegralKFactor : (short)50);
        settings2.enableCFM.set((short)enableCFM);

        if (isEquipType("vav", address)) {
            CCUHsApi hsApi = CCUHsApi.getInstance();
            settings2.maxDischargeAirTemperature.set(getMaxDischargeTemp(hsApi, equipRef));
            settings2.runPILoopOnNode.set(TrueCFMUtil.isCfmOnEdgeActive(hsApi, equipRef) ? (short)1 : (short)0);
        }

    }

    private static ProfileMap_t getProfileMap2(String profString) {
        if (profString.equals(ProfileType.VAV_REHEAT.name())) {
            return ProfileMap_t.PROFILE_MAP_VAV_NO_FAN;
        } else if (profString.equals(ProfileType.VAV_SERIES_FAN.name())) {
            return ProfileMap_t.PROFILE_MAP_VAV_SERIES_FAN;
        } else if (profString.equals(ProfileType.VAV_PARALLEL_FAN.name())) {
            return ProfileMap_t.PROFILE_MAP_VAV_PARALLEL_FAN;
        } else if (profString.equals(ProfileType.VAV_ACB.name())) {
            return ProfileMap_t.PROFILE_MAP_VAV_ACTIVE_CHILLED_BEAM;
        } else if (profString.equals(ProfileType.PLC.name())) {
            return ProfileMap_t.PROFILE_MAP_GENERIC_PI_CONTROL;
        } else if (profString.equals(ProfileType.SSE.name())) {
            return ProfileMap_t.PROFILE_MAP_SINGLE_STAGE_EQUIPMENT;
        } else if (profString.equals(ProfileType.BYPASS_DAMPER.name())) {
            return ProfileMap_t.PROFILE_MAP_BYPASS_DAMPER_CONTROL;
        }

        return ProfileMap_t.PROFILE_MAP_NOT_AVAILABLE;
    }

    private static InputSensorType_t getInputSensor1(CCUHsApi hsApi, short address) {

        int ai1Input = hsApi.readDefaultVal("point and config and analog1 and sensor and group == \""+address+"\"").intValue();
        switch (ai1Input) {
            case 1: return InputSensorType_t.INPUT_SENSOR_GENERIC_0_10V;
            case 2: return InputSensorType_t.INPUT_SENSOR_PRESSURE_SENSOR_0_2;
            case 3: return InputSensorType_t.INPUT_SENSOR_DIFF_PRESSURE_SENSOR_0_0P25;
            case 4: return InputSensorType_t.INPUT_SENSOR_AIRFLOW_SENSOR_0_1000;
            case 5: return InputSensorType_t.INPUT_SENSOR_HUMIDITY_0_100;
            case 6: return InputSensorType_t.INPUT_SENSOR_CO2_0_2000;
            case 7: return InputSensorType_t.INPUT_SENSOR_CO_0_100;
            case 8: return InputSensorType_t.INPUT_SENSOR_NO2_0_5;
            case 9: return InputSensorType_t.INPUT_SENSOR_CT_0_10;
            case 10: return InputSensorType_t.INPUT_SENSOR_CT_0_20;
            case 11: return InputSensorType_t.INPUT_SENSOR_CT_0_50;
            case 12: return InputSensorType_t.INPUT_SENSOR_ION_METER_1_1M;
        }

        int th1Input = hsApi.readDefaultVal("point and config and th1 and sensor and group == \""+address+"\"").intValue();
        switch (th1Input) {
            case 1: return InputSensorType_t.INPUT_SENSOR_10K_TYPE2_PROBE;
            case 2: return InputSensorType_t.INPUT_SENSOR_GENERIC_1K_100K;
        }

        int nativeSensorInput = hsApi.readDefaultVal("point and config and native and sensor and group == \""+address+"\"").intValue();
        switch (nativeSensorInput) {
            case 1: return InputSensorType_t.INPUT_SENSOR_NATIVE_TEMP;
            case 2: return InputSensorType_t.INPUT_SENSOR_NATIVE_HUMIDITY;
            case 3: return InputSensorType_t.INPUT_SENSOR_NATIVE_CO2;
            case 4: return InputSensorType_t.INPUT_SENSOR_NATIVE_CO;
            case 5: return InputSensorType_t.INPUT_SENSOR_NATIVE_NO;
            case 6: return InputSensorType_t.INPUT_SENSOR_NATIVE_VOC;
            case 7: return InputSensorType_t.INPUT_SENSOR_NATIVE_PRESSURE;
            case 8: return InputSensorType_t.INPUT_SENSOR_NATIVE_SOUND;
            case 9: return InputSensorType_t.INPUT_SENSOR_NATIVE_OCCUPANCY;
            case 10: return InputSensorType_t.INPUT_SENSOR_NATIVE_ILLUMINANCE;
            case 11: return InputSensorType_t.INPUT_SENSOR_NATIVE_CO2_EQUIVALENT;
            case 12: return InputSensorType_t.INPUT_SENSOR_NATIVE_UVI;
            case 13: return InputSensorType_t.INPUT_SENSOR_NATIVE_PM2P5;
            case 14: return InputSensorType_t.INPUT_SENSOR_NATIVE_PM10;
        }

        return InputSensorType_t.INPUT_SENSOR_NOT_USED;
    }

    private static InputSensorType_t getInputSensor2(int index) {
        switch (index) {
            case 0: return InputSensorType_t.INPUT_SENSOR_GENERIC_0_10V;
            case 1: return InputSensorType_t.INPUT_SENSOR_PRESSURE_SENSOR_0_2;
            case 2: return InputSensorType_t.INPUT_SENSOR_DIFF_PRESSURE_SENSOR_0_0P25;
            case 3: return InputSensorType_t.INPUT_SENSOR_AIRFLOW_SENSOR_0_1000;
            case 4: return InputSensorType_t.INPUT_SENSOR_HUMIDITY_0_100;
            case 5: return InputSensorType_t.INPUT_SENSOR_CO2_0_2000;
            case 6: return InputSensorType_t.INPUT_SENSOR_CO_0_100;
            case 7: return InputSensorType_t.INPUT_SENSOR_NO2_0_5;
            case 8: return InputSensorType_t.INPUT_SENSOR_CT_0_10;
            case 9: return InputSensorType_t.INPUT_SENSOR_CT_0_20;
            case 10: return InputSensorType_t.INPUT_SENSOR_CT_0_50;
            default: return InputSensorType_t.INPUT_SENSOR_NOT_USED;
        }
    }

    private static int getDamperSizeInInches(int index) {
        switch (index) {
            case 0: return 4;
            case 1: return 6;
            case 2: return 8;
            case 3: return 10;
            case 4: return 12;
            case 5: return 14;
            case 6: return 16;
            case 7: return 18;
            case 8: return 20;
            case 9: return 22;
            default: return 0;
        }
    }

    public static void setupDamperType(short address, SmartNodeSettings_t settings){
        CCUHsApi hsApi = CCUHsApi.getInstance();
        Equip equip = new Equip.Builder().setHashMap(hsApi.readEntity("equip and group == \"" + address + "\"")).build();
        if (equip.getMarkers().contains("dab")) {
            int damperConfig = hsApi.readDefaultVal("point and domainName == \"" + DomainName.damper1Type + "\" and group == \""+address+"\"").intValue();
            int damper2Config = hsApi.readDefaultVal("point and domainName == \"" + DomainName.damper2Type + "\" and group == \""+address+"\"").intValue();
            int reheatConfig = hsApi.readDefaultVal("point and domainName == \"" + DomainName.reheatType + "\" and group == \""+address+"\"").intValue() - 1;
            setupDamperActuator(settings, damperConfig, damper2Config, reheatConfig, "dab");
        } else if (equip.getMarkers().contains("vav")) {
            int damperConfig = hsApi.readDefaultVal("point and config and vav and  damper and type and group == \""+address+"\"").intValue();

            int reheatConfig;
            boolean isACB = false;
            if (equip.getDomainName().equals(DomainName.smartnodeActiveChilledBeam) || equip.getDomainName().equals(DomainName.helionodeActiveChilledBeam)) {
                isACB = true;
                reheatConfig = hsApi.readDefaultVal("point and domainName == \"" + DomainName.valveType + "\" and group == \""+address+"\"").intValue();
            } else {
                reheatConfig = hsApi.readDefaultVal("point and config and type and reheat and group == \""+address+"\"").intValue();
            }

            // With DM integration, reheatType enum is incremented by 1. ("notInstalled" was -1, now it's zero). This is why we are subtracting 1 from the value here.
            setupDamperActuator(settings, damperConfig, 0, reheatConfig-1, isACB ? "acb" : "vav");
        }
    }

    public static void setupDamperActuator(
            SmartNodeSettings_t settings,
            int damperConfig, int damper2Config, int reheatConfig, String profileType
    ){
        Map<DamperType, DamperActuator_t> damperTypeMap = new HashMap<>();
        damperTypeMap.put(DamperType.ZeroToTenV, DamperActuator_t.DAMPER_ACTUATOR_0_10V);
        damperTypeMap.put(DamperType.TwoToTenV, DamperActuator_t.DAMPER_ACTUATOR_2_10V);
        damperTypeMap.put(DamperType.TenToTwov, DamperActuator_t.DAMPER_ACTUATOR_10_2V);
        damperTypeMap.put(DamperType.TenToZeroV, DamperActuator_t.DAMPER_ACTUATOR_10_0V);
        damperTypeMap.put(DamperType.MAT, DamperActuator_t.DAMPER_ACTUATOR_MAT);
        damperTypeMap.put(DamperType.ZeroToFiveV, DamperActuator_t.DAMPER_ACTUATOR_0_5V);

        Map<ReheatType, DamperActuator_t> reheatTypeMap = new HashMap<>();
        reheatTypeMap.put(ReheatType.ZeroToTenV, DamperActuator_t.DAMPER_ACTUATOR_0_10V);
        reheatTypeMap.put(ReheatType.TwoToTenV, DamperActuator_t.DAMPER_ACTUATOR_2_10V);
        reheatTypeMap.put(ReheatType.TenToTwov, DamperActuator_t.DAMPER_ACTUATOR_10_2V);
        reheatTypeMap.put(ReheatType.TenToZeroV, DamperActuator_t.DAMPER_ACTUATOR_10_0V);
        reheatTypeMap.put(ReheatType.Pulse, DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT_REHEAT_PULSED);
        reheatTypeMap.put(ReheatType.OneStage, DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT_REHEAT_ONE_STAGE);
        reheatTypeMap.put(ReheatType.TwoStage, DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT_REHEAT_TWO_STAGE);

        settings.outsideAirOptimizationDamperActuatorType.set(Objects.requireNonNull(damperTypeMap.get(DamperType.values()[damperConfig])));

        //ReheatType migration should address this, but the value can be -2 or -1 based on this code runs before or after migration.
        if ((profileType.equals("vav") || profileType.equals("acb")) && reheatConfig >= 0){
            settings.returnAirDamperActuatorType.set(Objects.requireNonNull(reheatTypeMap.get(ReheatType.values()[reheatConfig])));
        } else if ((profileType.equals("vav") || profileType.equals("acb")) && reheatConfig == -1) {
            settings.returnAirDamperActuatorType.set(DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT);
        } else {
            settings.returnAirDamperActuatorType.set(getReheatType(damper2Config,reheatConfig,damperTypeMap));
        }
    }
    private static DamperActuator_t getReheatType(int damper2Config, int reheatConfig, Map<DamperType, DamperActuator_t> damperTypeMap) {
        DamperType damperType = DamperType.values()[damper2Config];
        if (reheatConfig == -1 ) {
            return (damperTypeMap.get(DamperType.values()[damper2Config]));
        } else {
            if (damperType == DamperType.ZeroToTenV && reheatConfig == ReheatType.OneStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_0_10V_REHEAT_ONE_STAGE;
            }
            if (damperType == DamperType.ZeroToTenV && reheatConfig == ReheatType.TwoStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_0_10V_REHEAT_TWO_STAGE;
            }
            if (damperType == DamperType.TwoToTenV && reheatConfig == ReheatType.OneStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_2_10V_REHEAT_ONE_STAGE;
            }
            if (damperType == DamperType.TwoToTenV && reheatConfig == ReheatType.TwoStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_2_10V_REHEAT_TWO_STAGE;
            }
            if (damperType == DamperType.TenToTwov && reheatConfig == ReheatType.OneStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_10_2V_REHEAT_ONE_STAGE;
            }
            if (damperType == DamperType.TenToTwov && reheatConfig == ReheatType.TwoStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_10_2V_REHEAT_TWO_STAGE;
            }
            if (damperType == DamperType.TenToZeroV && reheatConfig == ReheatType.OneStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_10_0V_REHEAT_ONE_STAGE;
            }
            if (damperType == DamperType.TenToZeroV && reheatConfig == ReheatType.TwoStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_10_0V_REHEAT_TWO_STAGE;
            }
            if (damperType == DamperType.ZeroToFiveV && reheatConfig == ReheatType.OneStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_0_5V_REHEAT_ONE_STAGE;
            }
            if (damperType == DamperType.ZeroToFiveV && reheatConfig == ReheatType.TwoStage.ordinal()) {
                return DamperActuator_t.DAMPER_ACTUATOR_0_5V_REHEAT_TWO_STAGE;
            }
            ReheatType config = ReheatType.values()[reheatConfig];
            if (damperType == DamperType.MAT ) {
                switch (config) {
                    case ZeroToTenV : return DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_0_10V;
                    case TwoToTenV : return DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_2_10V;
                    case TenToTwov : return DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_10_2V;
                    case TenToZeroV : return DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_10_0V;
                    case Pulse : return DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_PULSED;
                    case OneStage : return DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_ONE_STAGE;
                    case TwoStage : return DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_TWO_STAGE;
                }
            }
        }
        return DAMPER_ACTUATOR_NOT_PRESENT;
    }

    public static void setupBypassDamperActuator(SmartNodeSettings_t settings, String equipRef) {
        int damperConfig = CCUHsApi.getInstance().readDefaultVal("point and domainName == \"" + DomainName.damperType + "\" and equipRef == \""+equipRef+"\"").intValue();

        switch (damperConfig) {
            case 0: settings.outsideAirOptimizationDamperActuatorType.set(DamperActuator_t.DAMPER_ACTUATOR_0_10V); break;
            case 1: settings.outsideAirOptimizationDamperActuatorType.set(DamperActuator_t.DAMPER_ACTUATOR_2_10V); break;
            case 2: settings.outsideAirOptimizationDamperActuatorType.set(DamperActuator_t.DAMPER_ACTUATOR_10_0V); break;
            case 3: settings.outsideAirOptimizationDamperActuatorType.set(DamperActuator_t.DAMPER_ACTUATOR_10_2V); break;
            case 4: settings.outsideAirOptimizationDamperActuatorType.set(DamperActuator_t.DAMPER_ACTUATOR_MAT); break;
            case 5: settings.outsideAirOptimizationDamperActuatorType.set(DamperActuator_t.DAMPER_ACTUATOR_0_5V); break;
            default: settings.outsideAirOptimizationDamperActuatorType.set(DAMPER_ACTUATOR_NOT_PRESENT);
        }
    }

    public static short getMaxDischargeTemp(CCUHsApi hsApi, String equipRef) {
        short maxDischargeTemp = hsApi.readPointPriorityValByQuery("point and domainName == \"" + DomainName.reheatZoneMaxDischargeTemp + "\" and equipRef == \"" + equipRef + "\"").shortValue();
        return maxDischargeTemp > 0 ? maxDischargeTemp : 90;
    }

    private static void fillSmartNodeControls(SmartNodeControls_t controls_t,Zone zone, short node, String equipRef){


        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \""+node+"\"");

        if (device != null && !device.isEmpty())
        {
            ArrayList<HashMap> physicalOpPoints= hayStack.readAll("point and physical and cmd and deviceRef == \""+device.get("id")+"\"");

            for (HashMap opPoint : physicalOpPoints)
            {
                if (opPoint.get("portEnabled").toString().equals("true"))
                {
                    RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                    HashMap logicalOpPoint = hayStack.read("point and id == " + p.getPointRef());
                    if (logicalOpPoint.isEmpty()) {
                        CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for "+p.getDisplayName());
                        //Disabled output port should reset its val
                        hayStack.writeHisValById(opPoint.get("id").toString(), 0.0);
                        continue;
                    }
                    double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());

                    short mappedVal = 0;
                    if (isEquipType("vav", node))
                    {
                        //In case of vav - series/paralle fan, relay-2 maps to fan
                        if (isEquipType("series", node) || isEquipType("parallel", node)) {
                            double relay1Threshold = 0;
                            double relayActivationHysteresis = TunerUtil.readTunerValByQuery("domainName==\"" + DomainName.vavReheatRelayActivationHysteresis + "\"", equipRef);
                            if (!isRelayTwo(p) && !isAnalog(p)) {
                                if (hayStack.readHisValById(p.getId()) == 0) { relay1Threshold += relayActivationHysteresis; }
                            }

                            mappedVal = (isAnalog(p) ? mapAnalogOut(p.getType(), (short) logicalVal) :
                                                                 mapDigitalOut(p.getType(), logicalVal > relay1Threshold)
                            );
                        } else if (isEquipType("chilledBeam", node)) {
                            // In case of vav - acb, relay-1 maps to shut-off valve. No relay 2.
                            mappedVal = (isAnalog(p) ? mapAnalogOut(p.getType(), (short) logicalVal) :
                                    mapDigitalOut(p.getType(), (logicalVal > 0)));
                        } else {
                            double relay1Threshold = 0;
                            double relay2Threshold = 50;
                            double relayActivationHysteresis = TunerUtil.readTunerValByQuery("domainName==\"" + DomainName.vavReheatRelayActivationHysteresis + "\"", equipRef);
                            if (isRelayTwo(p)) {
                                if (hayStack.readHisValById(p.getId()) == 0) {
                                    relay2Threshold += relayActivationHysteresis;
                                }
                            } else if (!isAnalog(p)) {
                                if (hayStack.readHisValById(p.getId()) == 0) {
                                    relay1Threshold += relayActivationHysteresis;
                                }
                            }

                            //In case of vav - no fan, relay-2 maps to stage-2
                            mappedVal = (isAnalog(p) ? mapAnalogOut(p.getType(), (short) logicalVal) :
                                    mapDigitalOut(p.getType(), isRelayTwo(p) ?
                                            logicalVal > relay2Threshold : logicalVal > relay1Threshold)
                            );
                        }
                    }else if (isEquipType("sse", node))
                    {
                        //In case of sse , relay actuator maps to normally open by default
                        mappedVal = mapSSEDigitalOut(p.getType(), logicalVal > 0);
                    } else if (isEquipType("dab", node)) {
                        double relay2Threshold = 50;
                        if (isRelayTwo(p)) {
                            double relayActivationHysteresis = TunerUtil.readTunerValByQuery("relay and activation and hysteresis", equipRef);
                            if (hayStack.readHisValById(p.getId()) == 0) {
                                relay2Threshold = 50 + relayActivationHysteresis;
                            }
                        }

                        mappedVal = (isAnalog(p) ? mapAnalogOut(p.getType(), (short) logicalVal) :
                                                            mapDigitalOut(p.getType(), isRelayTwo(p) ?
                                                             logicalVal > relay2Threshold : logicalVal > 0));
                        //Analog2 on DAB profile could be mapped to reheat or damper2. When damper 2 is MAT, type is not configured via
                        //analog.
                        if (p.getPort().equals(ANALOG_OUT_TWO)) {
                            double damperType = hayStack.readDefaultVal("point and domainName == \"" + DomainName.damper2Type + "\" and group == \""+node+"\"");
                            if (damperType == DamperType.MAT.ordinal()) {
                                double damperPos = hayStack.readHisValByQuery("point and domainName == \"" + DomainName.normalizedDamper2Cmd + "\"  and group == \""+node+"\"");
                                controls_t.damperPosition.set((short)damperPos);
                            }
                        }

                    } else {
                        mappedVal = (isAnalog(p) ? mapAnalogOut(p.getType(), (short) logicalVal) : mapDigitalOut(p.getType(), logicalVal > 0));
                    }

                    if (isAnalog(p) && p.getType().equals(PULSE) && logicalVal > 0) {
                        mappedVal |= 0x80;
                    }

                    //TODo -MAT is currently configured on analog2 , what if reheat is also configured.
                    if (isAnalog(p) && p.getType().equals(MAT) && logicalVal >= 0) {
                        controls_t.damperPosition.set((short)logicalVal);
                        mappedVal = 0;
                    }

                    //Mapping not required during override.
                    if (Globals.getInstance().isTemporaryOverrideMode()) {
                        double physicalVal = hayStack.readHisValById(p.getId());
                        mappedVal = (short) physicalVal;
                    } else {
                        hayStack.writeHisValById(p.getId(), (double) mappedVal);
                    }

                    CcuLog.d(TAG_CCU_DEVICE, "Set "+logicalOpPoint.get("dis") +" "+ p.getPort() + " type " + p.getType() + " logicalVal: " + logicalVal + " mappedVal " + mappedVal);
                    Struct.Unsigned8 port = getDevicePort(device, controls_t, p);
                    if (port != null) {
                        port.set(mappedVal);
                    } else {
                        CcuLog.d(L.TAG_CCU_DEVICE, "Unknown port info for "+p.getDisplayName());
                    }

                } else if (opPoint.containsKey(Tags.WRITABLE)) {
                    RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                    double rawPointValue = hayStack.readPointPriorityVal(opPoint.get("id").toString());
                    CcuLog.d(TAG_CCU_DEVICE, " Raw Point: " + p.getDisplayName() +
                                    " is unused port and has value: " +rawPointValue);
                    Struct.Unsigned8 port = getDevicePort(device, controls_t, p);
                    if (port != null) {
                        port.set((short) rawPointValue);
                    }
                }
                    else {
                    //Disabled output port should reset its val
                    hayStack.writeHisValById(opPoint.get("id").toString(), 0.0);
                }
            }
            controls_t.setTemperature.set((short)(getSetTemp(equipRef) > 0 ? (getSetTemp(equipRef) * 2) : 144));
            controls_t.conditioningMode.set((short) (L.ccu().systemProfile.getSystemController().getSystemState() == HEATING ? 1 : 0));
            controls_t.targetValue.set(getTargetValue(equipRef));

            if (isEquipType("vav", node) && TrueCFMUtil.isCfmOnEdgeActive(hayStack, equipRef)) {
                controls_t.cfmAirflowSetPoint.set((short)getAirflowSetpoint(hayStack, equipRef));
                controls_t.datSetPoint.set((short)getDatSetpoint(hayStack, equipRef));
            }
        }
    }

    private static Struct.Unsigned8 getDevicePort(HashMap device, SmartNodeControls_t controls_t, RawPoint physicalPoint) {
        if(device.containsKey(Tags.HELIO_NODE)){
            return LHelioNode.Companion.getHelioNodePort(controls_t, physicalPoint);
        } else {
            return LSmartNode.getSmartNodePort(controls_t, physicalPoint);
        }
    }

    public static CcuToCmOverUsbSnControlsMessage_t getCurrentTimeForControlMessage(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t)
    {
        fillCurrentUpdatedTime(controlsMessage_t.controls);
        return controlsMessage_t;
    }
    public static double getTempOffset(short addr) {
        try
        {
            return CCUHsApi.getInstance().readDefaultVal("point and zone and config and (temp or temperature) and offset and group == \"" + addr + "\"");
        } catch (Exception e) {
            CcuLog.e(TAG_CCU_DEVICE," Error ",e);
            return 0;
        }
    }

    public static boolean isAnalog(RawPoint point) {

        String domainName = point.getDomainName();
        if (domainName != null) {
            return domainName.equals(DomainName.analog1Out)
                    || domainName.equals(DomainName.analog2Out)
                    || domainName.equals(DomainName.analog1In)
                    || domainName.equals(DomainName.analog2In);
        }

        String port = point.getPort();
        if (port != null) {
            return port.equals(ANALOG_OUT_ONE)
                    || port.equals(ANALOG_OUT_TWO)
                    || port.equals(ANALOG_IN_ONE)
                    || port.equals(ANALOG_IN_TWO);
        }

        return false;
    }

    public static boolean isRelayTwo(RawPoint point) {

        String domainName = point.getDomainName();
        if (domainName != null) {
            return domainName.equals(DomainName.relay2);
        }

        String port = point.getPort();
        if (port != null) {
            return port == RELAY_TWO;
        }

        return false;
    }

    public static short mapAnalogOut(String type, short val) {
        val = (short)Math.min(val, 100);
        val = (short)Math.max(val, 0);
        switch (type)
        {
            case "0-10v":
            case PULSE:
                return val;
            case "10-0v":
                return (short) (100 - val);
            case "2-10v":
                return (short) (20 + scaleAnalog(val, 80));
            case "10-2v":
                return (short) (100 - scaleAnalog(val, 80));
            case "0-5v":
                return scaleAnalog(val, 50);
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
    public static short mapSSEDigitalOut(String type, boolean val)
    {

        switch (type)
        {
            case "Relay N/O":
                return (short) (val ? 1 : 0);
            ///Defaults to normally open
            case "Relay N/C":
                return (short) (val ? 0 : 1);
        }

        return 0;
    }
    public static short mapDigitalOut(String type, boolean val)
    {
        
        switch (type)
        {
            case "Relay N/O":
                return (short) (val ? 0 : 1);
            ///Defaults to normally open
            case "Relay N/C":
                return (short) (val ? 1 : 0);
        }
        
        return 0;
    }
    
    protected static short scaleAnalog(short analog, int scale)
    {
        return (short) ((float) scale * ((float) analog / 100.0f));
    }

    public static Struct.Unsigned8 getSmartNodePort(SmartNodeControls_t controls, RawPoint p) {

        String domainName = p.getDomainName();
        if (domainName != null) {
            switch (domainName) {
                case DomainName.analog1Out:
                    return controls.analogOut1;
                case DomainName.analog2Out:
                    return controls.analogOut2;
                case DomainName.relay1:
                    return controls.digitalOut1;
                case DomainName.relay2:
                    return controls.digitalOut2;
            }
        }
        
        String port = p.getPort();
        if (port != null) {
            switch (port)
            {
                case ANALOG_OUT_ONE:
                    return controls.analogOut1;
                case ANALOG_OUT_TWO:
                    return controls.analogOut2;
                case RELAY_ONE:
                    return controls.digitalOut1;
                case RELAY_TWO:
                    return controls.digitalOut2;
                default:
                    return null;
            }
        }

        return null;
    }
    
    public static double getDesiredTemp(short node)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and (average or avg) and sp and group == \""+node+"\"");
        if (point == null || point.isEmpty()) {
            CcuLog.d(TAG_CCU_DEVICE, " Desired Temp point does not exist for equip , sending 0");
            return 0;
        }
        return CCUHsApi.getInstance().readPointPriorityVal(point.get("id").toString());
    }

    public static short getTargetValue(String equipRef) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(equipRef);
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();

        try {
            if (equip.getProfile().equals(ProfileType.BYPASS_DAMPER.name())) {
                double target = CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + DomainName.ductStaticPressureSetpoint + "\" and equipRef == \"" + equipRef + "\"");
                return Short.parseShort(String.valueOf((int)(10*target)));
            } else if  (equip.getProfile().equals(ProfileType.PLC.name())) {
                double target = CCUHsApi.getInstance().readPointPriorityValByQuery("point and config and pid and target and value and equipRef == \"" + equipRef + "\"");
                InputSensorType_t sensorType = getInputSensor1(CCUHsApi.getInstance(), Short.parseShort(equip.getGroup()));
                return getInputSensor1Multiplier(sensorType, target);
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    private static Short getInputSensor1Multiplier(InputSensorType_t sensorType, double val) {
        switch (sensorType) {
            case INPUT_SENSOR_GENERIC_0_10V:
            case INPUT_SENSOR_PRESSURE_SENSOR_0_2:
            case INPUT_SENSOR_NO2_0_5:
            case INPUT_SENSOR_CT_0_10:
            case INPUT_SENSOR_CT_0_20:
            case INPUT_SENSOR_CT_0_50:
            case INPUT_SENSOR_10K_TYPE2_PROBE:
            case INPUT_SENSOR_GENERIC_1K_100K:
            case INPUT_SENSOR_NATIVE_TEMP:
            case INPUT_SENSOR_NATIVE_NO:
            case INPUT_SENSOR_NATIVE_PRESSURE:
            case INPUT_SENSOR_NATIVE_UVI:
                return Short.parseShort(String.valueOf((int)(10*val)));
            case INPUT_SENSOR_DIFF_PRESSURE_SENSOR_0_0P25:
                return Short.parseShort(String.valueOf((int)(100*val)));
            case INPUT_SENSOR_AIRFLOW_SENSOR_0_1000:
            case INPUT_SENSOR_HUMIDITY_0_100:
            case INPUT_SENSOR_CO2_0_2000:
            case INPUT_SENSOR_CO_0_100:
            case INPUT_SENSOR_NATIVE_HUMIDITY:
            case INPUT_SENSOR_NATIVE_SOUND:
            case INPUT_SENSOR_NATIVE_CO:
            case INPUT_SENSOR_NATIVE_CO2:
            case INPUT_SENSOR_NATIVE_OCCUPANCY:
            case INPUT_SENSOR_NATIVE_ILLUMINANCE:
            case INPUT_SENSOR_NATIVE_PM2P5:
            case INPUT_SENSOR_NATIVE_PM10:
                Short.parseShort(String.valueOf((int)(val)));
            case INPUT_SENSOR_NATIVE_CO2_EQUIVALENT:
                return Short.parseShort(String.valueOf((int)(val/10)));
            case INPUT_SENSOR_ION_METER_1_1M:
            case INPUT_SENSOR_NATIVE_VOC:
                return Short.parseShort(String.valueOf((int)(val/1000)));
            default:
                return 0;
        }
    }

    public static int getCurrentDayOfWeekWithMondayAsStart() {
        Calendar calendar = GregorianCalendar.getInstance();
        switch (calendar.get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
        }
        return 0;
    }
    public static void fillCurrentUpdatedTime(SmartNodeControls_t controls){

        Calendar curDate = GregorianCalendar.getInstance();
        controls.time.day.set ((byte)(getCurrentDayOfWeekWithMondayAsStart() & 0xff));
        controls.time.hours.set((byte)(curDate.get(Calendar.HOUR_OF_DAY) & 0xff));
        controls.time.minutes.set((byte)(curDate.get(Calendar.MINUTE) & 0xff));
    }
    
    public static double getDamperLimit(String coolHeat, String minMax, short nodeAddr)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and config and damper and pos and "+coolHeat+" and "+minMax+" and group == \""+nodeAddr+"\"");
        if (points.isEmpty()) {
            ArrayList domainPoints = CCUHsApi.getInstance().readAll("point and domainName == \"" + getDamperLimitDomainName(coolHeat, minMax) + "\" and group == \""+nodeAddr+"\"");
            if (domainPoints.isEmpty()) {
                CcuLog.d("CCU","DamperLimit: Invalid point Send Default");
                return minMax.equals("max") ? 100 : 40 ;
            } else {
                String id = ((HashMap)domainPoints.get(0)).get("id").toString();
                return CCUHsApi.getInstance().readPointPriorityVal(id);
            }
        }
        String id = ((HashMap)points.get(0)).get("id").toString();
        return CCUHsApi.getInstance().readDefaultValById(id);
    }

    public static String getDamperLimitDomainName(String coolHeat, String minMax) {
        if (coolHeat.equals("heating")) {
            if (minMax.equals("min")) {
                return DomainName.minHeatingDamperPos;
            } else {
                return DomainName.maxHeatingDamperPos;
            }
        } else {
            if (minMax.equals("min")) {
                return DomainName.minCoolingDamperPos;
            } else {
                return DomainName.maxCoolingDamperPos;
            }
        }
    }

    public static double getAirflowSetpoint(CCUHsApi hayStack, String equipRef) {
        return hayStack.readHisValByQuery("point and domainName == \"" + DomainName.airFlowSetpoint + "\" and equipRef == \"" + equipRef + "\"");
    }

    public static double getDatSetpoint(CCUHsApi hayStack, String equipRef) {
        return hayStack.readHisValByQuery("point and domainName == \"" + DomainName.dischargeAirTempSetpoint + "\" and equipRef == \"" + equipRef + "\"");
    }

    public static double getStatus(short nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and not ota and status and his and group == \""+nodeAddr+"\"");
    }
    
    public static double getConfigNumVal(String tags, short nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and "+tags+" and group == \""+nodeAddr+"\"");
    }
    
    public static boolean isEquipType(String type, short nodeAddr) {
        Equip q = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group ==\""+nodeAddr+"\"")).build();
        return q.getMarkers().contains(type);
    }
    
    /********************************END SEED MESSAGES**************************************/



    /*
    This method returns the controlMessage for the given equip
    param : String - node address
    returns the control message if the node address exits
    else nul
     */
    public static CcuToCmOverUsbSnControlsMessage_t getControlMessageforEquip(String node,CCUHsApi hayStack){

        CcuToCmOverUsbSnControlsMessage_t controlsMessage;
        controlsMessage = new CcuToCmOverUsbSnControlsMessage_t();
        controlsMessage.smartNodeAddress.set(Short.parseShort(node));
        controlsMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);

        HashMap<Object,Object> device = hayStack.readEntity("device and addr == \""+node+"\"");

        if (device != null && !device.isEmpty())
        {
            ArrayList<HashMap<Object, Object>> physicalOpPoints= hayStack.readAllEntities("point and physical and cmd and deviceRef == \""+device.get("id")+"\"");

            for (HashMap<Object,Object> opPoint : physicalOpPoints)
            {
                if (opPoint.get("portEnabled").toString().equals("true"))
                {
                    RawPoint p = new RawPoint.Builder().setHashMap(opPoint).build();
                    HashMap<Object,Object> logicalOpPoint = hayStack.readEntity("point and id == " + p.getPointRef());
                    if (logicalOpPoint.isEmpty()) {
                        CcuLog.d(TAG_CCU_DEVICE, " Logical point does not exist for "+opPoint.get("dis"));
                        hayStack.writeHisValById(opPoint.get("id").toString(), 0.0);
                        continue;
                    }
                    double logicalVal = hayStack.readHisValById(logicalOpPoint.get("id").toString());
                    short mappedVal;
                    if (isEquipType("vav", Short.parseShort(node)))
                    {
                        //IN case of vav , relay-2 maps to stage-2
                        mappedVal = (isAnalog(p) ? mapAnalogOut(p.getType(), (short) logicalVal) : mapDigitalOut(p.getType(), isRelayTwo(p) ? logicalVal > 50 : logicalVal > 0));
                    } else {
                        mappedVal = (isAnalog(p) ? mapAnalogOut(p.getType(), (short) logicalVal) : mapDigitalOut(p.getType(), logicalVal > 0));
                    }

                    if (isAnalog(p) && p.getType().equals(PULSE) && logicalVal > 0) {
                        mappedVal |= 0x80;
                    }

                    if (isAnalog(p) && p.getType().equals(MAT) && logicalVal > 0) {
                        controlsMessage.controls.damperPosition.set(mappedVal);
                        mappedVal = 0;
                    }

                    if (!Globals.getInstance().isTemporaryOverrideMode()) {
                        hayStack.writeHisValById(p.getId(), (double) mappedVal);
                    }

                    CcuLog.d(TAG_CCU_DEVICE, " Set " + p.getPort() + " type " + p.getType() + " logicalVal: " + logicalVal + " mappedVal " + mappedVal);
                    Struct.Unsigned8 smartNodePort =LSmartNode.getSmartNodePort(controlsMessage.controls, p);
                    if(smartNodePort != null)
                        smartNodePort.set(mappedVal);

                } else {
                    //Disabled output port should reset its val
                    hayStack.writeHisValById(opPoint.get("id").toString(), 0.0);
                }
            }
            Equip equip = HSUtil.getEquipForModule(Short.valueOf((device.get("addr").toString())));
            controlsMessage.controls.setTemperature.set((short)(getSetTemp(equip.getId()) > 0 ? (getSetTemp(equip.getId()) * 2) : 144));
            controlsMessage.controls.conditioningMode.set((short) (L.ccu().systemProfile.getSystemController().getSystemState() == HEATING ? 1 : 0));
        }
        return controlsMessage;
    }
    
}
