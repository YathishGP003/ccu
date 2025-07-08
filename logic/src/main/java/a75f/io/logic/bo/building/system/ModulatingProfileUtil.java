package a75f.io.logic.bo.building.system;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.hvac.ModulatingProfileAnalogMapping;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuAnalogOutMinMaxConfig;

public class ModulatingProfileUtil {

    private static final int ANALOG_SCALE = 10;

    public static int getHeatingSignal(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig, ModulatingProfileAnalogMapping analogMapping,
            boolean heatingLockoutActive, double systemHeatingLoopOp) {
        double analogMin = getAnalogMin(minMaxConfig, analogMapping);
        double analogMax = getAnalogMax(minMaxConfig, analogMapping);
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" HeatingSignal : "+ VavSystemController.getInstance().getHeatingSignal());
        if (heatingLockoutActive) {
            return  (int)(analogMin * ANALOG_SCALE);
        } else {
            return getModulatedAnalogVal(analogMin, analogMax, systemHeatingLoopOp);
        }
    }

    public static int getFanSpeedSignal(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig, ModulatingProfileAnalogMapping analogMapping,
                                  double systemFanLoopOp) {
        double analogMin = getAnalogMin(minMaxConfig, analogMapping);
        double analogMax = getAnalogMax(minMaxConfig, analogMapping);
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemFanLoopOp: "+systemFanLoopOp);
        return getModulatedAnalogVal(analogMin, analogMax, systemFanLoopOp);
    }

    public static int getOutsideAirDamperSignal(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig, ModulatingProfileAnalogMapping analogMapping
                            , double systemCo2LoopOp) {
        double analogMin = getAnalogMin(minMaxConfig, analogMapping);
        double analogMax = getAnalogMax(minMaxConfig, analogMapping);
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemCo2Loop: "+systemCo2LoopOp);
        return getModulatedAnalogVal(analogMin, analogMax, systemCo2LoopOp);
    }

    public static int getChilledWaterValveSignal(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig, ModulatingProfileAnalogMapping analogMapping,
                                double systemChilledWaterLoopOp) {
        double analogMin = getAnalogMin(minMaxConfig, analogMapping);
        double analogMax = getAnalogMax(minMaxConfig, analogMapping);
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemChilledWaterLoopOp: "+systemChilledWaterLoopOp);
        return getModulatedAnalogVal(analogMin, analogMax, systemChilledWaterLoopOp);
    }

    public static int getCompressorSpeedSignal(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig, ModulatingProfileAnalogMapping analogMapping,
                                double systemCompressorLoopOp) {
        double analogMin = getAnalogMin(minMaxConfig, analogMapping);
        double analogMax = getAnalogMax(minMaxConfig, analogMapping);
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemCompressorLoopOp: "+systemCompressorLoopOp);
        return getModulatedAnalogVal(analogMin, analogMax, systemCompressorLoopOp);
    }

    public static double getAnalogMin(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig,
                                ModulatingProfileAnalogMapping analogMapping) {
        double analogMin = 2;
        switch (analogMapping) {
            case FanSpeed:
                analogMin = minMaxConfig.getFanSignalConfig().getMin();
                break;
            case CompressorSpeed:
                analogMin = minMaxConfig.getCompressorSpeedConfig().getMin();
                break;
            case OutsideAirDamper:
                analogMin = minMaxConfig.getOutsideAirDamperConfig().getMin();
                break;
            case Cooling:
                analogMin = minMaxConfig.getCoolingSignalConfig().getMin();
                break;
            case Heating:
                analogMin = minMaxConfig.getHeatingSignalConfig().getMin();
                break;
            case ChilledWaterValve:
                analogMin = minMaxConfig.getChilledWaterValveConfig().getMin();
                break;
        }
        return analogMin;
    }

    public static double getAnalogMax(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig,
                                ModulatingProfileAnalogMapping analogMapping) {
        double analogMax = 10;
        switch (analogMapping) {
            case FanSpeed:
                analogMax = minMaxConfig.getFanSignalConfig().getMax();
                break;
            case CompressorSpeed:
                analogMax = minMaxConfig.getCompressorSpeedConfig().getMax();
                break;
            case OutsideAirDamper:
                analogMax = minMaxConfig.getOutsideAirDamperConfig().getMax();
                break;
            case Cooling:
                analogMax = minMaxConfig.getCoolingSignalConfig().getMax();
                break;
            case Heating:
                analogMax = minMaxConfig.getHeatingSignalConfig().getMax();
                break;
            case ChilledWaterValve:
                analogMax = minMaxConfig.getChilledWaterValveConfig().getMax();
                break;
        }
        return analogMax;
    }

    public static int getModulatedAnalogValDuringEcon(double analogMin, double analogMax, double val, double economizingToMainCoolingLoopMap) {
        int modulatedVal;
        if(val < economizingToMainCoolingLoopMap) {
            return (int) (analogMin * ANALOG_SCALE);
        }
        if(economizingToMainCoolingLoopMap >= 100) {
            return (int) (analogMax * ANALOG_SCALE);
        }
        if (analogMax > analogMin) {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * ((val - economizingToMainCoolingLoopMap) / (100 - economizingToMainCoolingLoopMap))));
        } else {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * ((val - economizingToMainCoolingLoopMap) / (100 - economizingToMainCoolingLoopMap))));
        }
        return modulatedVal;
    }

    public static int getModulatedAnalogVal(double analogMin, double analogMax, double val) {
        int modulatedVal;
        if (analogMax > analogMin) {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (val/100)));
        } else {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (val/100)));
        }
        return modulatedVal;
    }


}
