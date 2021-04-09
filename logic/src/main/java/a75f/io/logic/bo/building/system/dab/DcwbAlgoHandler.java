package a75f.io.logic.bo.building.system.dab;

import java.util.HashMap;

import a75.io.algos.ControlLoop;
import a75f.io.algos.dcwb.AdaptiveDeltaTControl;
import a75f.io.algos.dcwb.AdaptiveDeltaTDto;
import a75f.io.algos.dcwb.MaximizedDeltaTControl;
import a75f.io.algos.dcwb.MaximizedDeltaTDto;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Class that abstracts the exact type of algorithm used.
 * It in internally uses adaptiveDelta or maximizedExitTemp methods based on the configuration.
 */
class DcwbAlgoHandler {
    
    public DcwbAlgoHandler(boolean isAdaptiveDelta, String equipRef, CCUHsApi hsApi) {
        hayStack = hsApi;
        adaptiveDelta = isAdaptiveDelta;
        dcwbControlLoop = new ControlLoop();
        initializeTuners();
    }
    
    ControlLoop dcwbControlLoop;
    String systemEquipRef;
    CCUHsApi hayStack;
    double proportionalGain = TunerConstants.DEFAULT_PROPORTIONAL_GAIN;
    double integralGain =  TunerConstants.DEFAULT_INTEGRAL_GAIN;
    double proportionalSpread = TunerConstants.DEFAULT_PROPORTIONAL_SPREAD;
    double integralTimeout = TunerConstants.DEFAULT_INTEGRAL_TIMEOUT;
    
    double chilledWaterTargetDelta;
    double chilledWaterExitTemperatureMargin;
    double chilledWaterMaxFlowRate;
    
    boolean adaptiveDelta;
    
    double chilledWaterValveLoopOutput;
    
    double adaptiveComfortThresholdMargin = TunerConstants.ADAPTIVE_COMFORT_THRESHOLD_MARGIN;
    
    private void initializeTuners() {
        proportionalGain = TunerUtil.readTunerValByQuery("dcwb and pgain", systemEquipRef);
        integralGain = TunerUtil.readTunerValByQuery("dcwb and igain", systemEquipRef);
        proportionalSpread = TunerUtil.readTunerValByQuery("dcwb and pspread", systemEquipRef);
        integralTimeout = TunerUtil.readTunerValByQuery("dcwb and itimeout", systemEquipRef);
        adaptiveComfortThresholdMargin = TunerUtil.readTunerValByQuery("dcwb and adaptive and threshold",
                                                                               systemEquipRef);
    
        chilledWaterTargetDelta = getChilledWaterConfig("target and delta", hayStack);
        chilledWaterExitTemperatureMargin = getChilledWaterConfig("exit and temp and margin", hayStack);
        chilledWaterMaxFlowRate = getChilledWaterConfig("max and flow and rate", hayStack);
    }
    
    private double getChilledWaterConfig(String tags, CCUHsApi hayStack) {
        HashMap chilledWaterConfig = hayStack.read("point and system and config and chilled and water and " + tags);
        if(!chilledWaterConfig.isEmpty()) {
            return hayStack.readPointPriorityVal(chilledWaterConfig.get("id").toString());
        }
        return 0;
    }
    
    public void runLoopAlgorithm() {
        chilledWaterValveLoopOutput = adaptiveDelta ?
            AdaptiveDeltaTControl.Algo.getChilledWaterAdaptiveDeltaTValveLoop(getAdaptiveDeltaDto())
            : MaximizedDeltaTControl.Algo.getChilledWaterMaximizedDeltaTValveLoop(getMaximizedDeltaTDto());
    }
    
    public double getChilledWaterValveLoopOutput() {
        return chilledWaterValveLoopOutput;
    }
    
    private AdaptiveDeltaTDto getAdaptiveDeltaDto() {
        double inletWaterTemp = 44;
        double averageCoolingDesiredTemp = 74;
        return new AdaptiveDeltaTDto(inletWaterTemp,
                                 chilledWaterTargetDelta,
                                 adaptiveComfortThresholdMargin,
                                     averageCoolingDesiredTemp,
                                 dcwbControlLoop);
    }
    
    private MaximizedDeltaTDto getMaximizedDeltaTDto() {
        double outletWaterTemp = 55;
        double averageCoolingDesiredTemp = 74;
        return new MaximizedDeltaTDto(outletWaterTemp,
                                      averageCoolingDesiredTemp,
                                     chilledWaterExitTemperatureMargin,
                                     dcwbControlLoop);
    }
}
