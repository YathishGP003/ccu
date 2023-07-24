package a75f.io.logic.bo.building.hyperstatsplit.actions

import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * Created by Nick P on 07-24-2023.
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

    fun doAnalogOAOAction( port: Port,
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
}