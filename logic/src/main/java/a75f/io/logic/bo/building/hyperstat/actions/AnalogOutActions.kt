package a75f.io.logic.bo.building.hyperstat.actions

import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings


/**
 * Created by Manjunath K on 11-07-2022.
 */

interface AnalogOutActions {

    fun doAnalogCooling( port: Port,
                         conditioningMode: StandaloneConditioningMode,
                         analogOutStages: HashMap<String, Int>,
                         coolingLoopOutput : Int)

    fun doAnalogHeating( port: Port,
                         conditioningMode: StandaloneConditioningMode,
                         analogOutStages: HashMap<String, Int>,
                         heatingLoopOutput : Int)

    fun doAnalogDCVAction( port: Port,
                           analogOutStages: HashMap<String, Int>,
                           zoneCO2Threshold: Double,
                           zoneCO2DamperOpeningRate: Double,
                           isDoorOpen: Boolean)

    fun doAnalogFanAction( port: Port,
                           fanLowPercent: Int,
                           fanMediumPercent: Int,
                           fanHighPercent: Int,
                           fanMode: StandaloneFanStage,
                           conditioningMode: StandaloneConditioningMode,
                           fanLoopOutput: Int,
                           analogOutStages: HashMap<String, Int>)
    fun doAnalogWaterValveAction( port: Port,
                                  fanMode: StandaloneFanStage,
                                  basicSettings: BasicSettings,
                                  loopOutput: Int,
                                  analogOutStages: HashMap<String, Int>)
}