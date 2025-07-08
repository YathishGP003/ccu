package a75f.io.logic.bo.building.system.dab;

import java.util.HashMap;

import a75.io.algos.ControlLoop;
import a75f.io.algos.dcwb.AdaptiveDeltaTControlAlgo;
import a75f.io.algos.dcwb.AdaptiveDeltaTInput;
import a75f.io.algos.dcwb.MaximizedDeltaTControl;
import a75f.io.algos.dcwb.MaximizedDeltaTInput;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.Domain;
import a75f.io.domain.equips.DabModulatingRtuSystemEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.bo.util.UnitUtils;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Class that abstracts the exact type of algorithm used.
 * It in internally uses adaptiveDelta or maximizedExitTemp methods based on the configuration.
 */
class DcwbAlgoHandler {
    
    private static final int MAX_PI_LOOP_OUTPUT = 100;
    private static final int MIN_PI_LOOP_OUTPUT = 0;

    public DcwbAlgoHandler(boolean isAdaptiveDelta, String equipRef, CCUHsApi hsApi) {
        hayStack = hsApi;
        adaptiveDelta = isAdaptiveDelta;
        dcwbControlLoop = new ControlLoop();
        systemEquipRef = equipRef;
    }
    
    private ControlLoop dcwbControlLoop;
    private String systemEquipRef;
    private CCUHsApi hayStack;
    
    private double chilledWaterTargetDelta;
    private double chilledWaterExitTemperatureMargin;
    private double chilledWaterMaxFlowRate;
    
    private boolean adaptiveDelta;
    
    private double chilledWaterValveLoopOutput;
    
    private double adaptiveComfortThresholdMargin = TunerConstants.ADAPTIVE_COMFORT_THRESHOLD_MARGIN;

    public void setAdaptiveDelta(boolean adaptiveDelta) {
        this.adaptiveDelta = adaptiveDelta;
    }
    public DabModulatingRtuSystemEquip systemEquip;

    /**
     * Initialize tuners.
     * This needs to need to be called every time before the loop , otherwise a tuner update that was
     * received via pubnub wont be picked till next app-restart.
     */
    private void initializeTuners() {
        systemEquip = (DabModulatingRtuSystemEquip) Domain.systemEquip;
        dcwbControlLoop.useNegativeProportionalError(false);
        dcwbControlLoop.setProportionalGain(TunerUtil.readTunerValByQuery("dcwb and pgain", systemEquipRef));
        dcwbControlLoop.setIntegralGain(TunerUtil.readTunerValByQuery("dcwb and igain", systemEquipRef));
        dcwbControlLoop.setProportionalSpread((int)TunerUtil.readTunerValByQuery("dcwb and pspread", systemEquipRef));
        dcwbControlLoop.setIntegralMaxTimeout((int)TunerUtil.readTunerValByQuery("dcwb and itimeout", systemEquipRef));
        
        adaptiveComfortThresholdMargin = systemEquip.getAdaptiveComfortThresholdMargin().readPriorityVal();
        chilledWaterTargetDelta = systemEquip.getChilledWaterTargetDelta().readPriorityVal();
        chilledWaterExitTemperatureMargin = systemEquip.getChilledWaterExitTemperatureMargin().readPriorityVal();
        chilledWaterMaxFlowRate = systemEquip.getChilledWaterMaxFlowRate().readPriorityVal();
    }
    
    /**
     * Runs the appropriate DCWB algorithm and update system chilledWaterValveLoopOutput.
     */
    public void runLoopAlgorithm() {

        CcuLog.i(L.TAG_CCU_SYSTEM, "DCWB runLoopAlgorithm");
        initializeTuners();
        DcwbBtuMeterDao btuDao = DcwbBtuMeterDao.getInstance();
        
        if (btuDao.getCWMaxFlowRate(hayStack) < chilledWaterMaxFlowRate) {
            chilledWaterValveLoopOutput = adaptiveDelta ? AdaptiveDeltaTControlAlgo.getChilledWaterAdaptiveDeltaTValveLoop(getAdaptiveDeltaDto(btuDao))
                                            : MaximizedDeltaTControl.getChilledWaterMaximizedDeltaTValveLoop(getMaximizedDeltaTDto(btuDao));
    
            //PI Loop may run beyond the normal limits based on the tuner values. Restrict it to the 0-100 range here.
            chilledWaterValveLoopOutput = Math.max(chilledWaterValveLoopOutput, MIN_PI_LOOP_OUTPUT);
            chilledWaterValveLoopOutput = Math.min(chilledWaterValveLoopOutput, MAX_PI_LOOP_OUTPUT);
            CcuLog.i(L.TAG_CCU_SYSTEM, "DCWB PI loop : " + chilledWaterValveLoopOutput);
                    //PI loop operates with the intention of maintaining delta T. So we should invert the loop Output.
            if (adaptiveDelta && !AdaptiveDeltaTControlAlgo.Companion.getLinearModeLoop()) {
                chilledWaterValveLoopOutput = 100 - chilledWaterValveLoopOutput;
            }
        } else {
            CcuLog.i(L.TAG_CCU_SYSTEM, "DCWB disabled. Flow rate is higher than threshold "+btuDao.getCWMaxFlowRate(hayStack));
            chilledWaterValveLoopOutput = hayStack.readHisValByQuery("dcwb and valve and loop and output");
        }
    }
    
    public double getChilledWaterValveLoopOutput() {
        return chilledWaterValveLoopOutput;
    }
    
    public void resetChilledWaterValveLoop() {
        dcwbControlLoop.reset();
    }
    
    public double getChilledWaterTargetExitTemperature() {
        return adaptiveDelta ? 0 :
                     (SystemTemperatureUtil.getAverageCoolingDesiredTemp() - chilledWaterExitTemperatureMargin);
    }
    
    private AdaptiveDeltaTInput getAdaptiveDeltaDto(DcwbBtuMeterDao btuDao) {
        double inletWaterTemp = UnitUtils.celsiusToFahrenheit(btuDao.getInletWaterTemperature(hayStack));
        double outletWaterTemp = UnitUtils.celsiusToFahrenheit(btuDao.getOutletWaterTemperature(hayStack));
        double averageCoolingDesiredTemp = SystemTemperatureUtil.getAverageCoolingDesiredTemp();
        return new AdaptiveDeltaTInput(inletWaterTemp,
                                 outletWaterTemp,
                                 chilledWaterTargetDelta,
                                 adaptiveComfortThresholdMargin,
                                     averageCoolingDesiredTemp,
                                 dcwbControlLoop);
    }
    
    private MaximizedDeltaTInput getMaximizedDeltaTDto(DcwbBtuMeterDao btuDao) {
        double outletWaterTemp = UnitUtils.celsiusToFahrenheit(btuDao.getOutletWaterTemperature(hayStack));
        double averageCoolingDesiredTemp = SystemTemperatureUtil.getAverageCoolingDesiredTemp();
        return new MaximizedDeltaTInput(outletWaterTemp,
                                      averageCoolingDesiredTemp,
                                     chilledWaterExitTemperatureMargin,
                                     dcwbControlLoop);
    }
    
    private double getChilledWaterConfig(String tags, CCUHsApi hayStack) {
        HashMap chilledWaterConfig = hayStack.read("point and system and config and chilled and water and " + tags);
        if(!chilledWaterConfig.isEmpty()) {
            return hayStack.readPointPriorityVal(chilledWaterConfig.get("id").toString());
        }
        return 0;
    }
    
    public void resetValveControlLoop() {
        dcwbControlLoop.reset();
    }
    
}
