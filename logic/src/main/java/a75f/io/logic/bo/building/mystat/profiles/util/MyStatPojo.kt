package a75f.io.logic.bo.building.mystat.profiles.util

import a75f.io.logic.bo.building.hvac.MyStatFanStages
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstat.v2.configs.MinMaxConfig

/**
 * Created by Manjunath K on 17-01-2025.
 */

data class MyStatTuners(
    var coolingDeadband: Double = 2.0, //(°F)
    var heatingDeadband: Double = 2.0, //(°F)
    var coolingDeadbandMultiplier: Double = 0.5,
    var heatingDeadbandMultiplier: Double = 0.5,
    var proportionalSpread: Double = 2.0,  //(°F)
    var integralMaxTimeout: Int = 30,  //(minutes)
    var proportionalGain:Double = 0.5,
    var integralGain: Double = 0.5,
    var relayActivationHysteresis: Int = 10,  //%
    var analogFanSpeedMultiplier: Double = 1.0,
    var humidityHysteresis: Int = 5, //%
    var forcedOccupiedTimer: Int = 120, //(min)
    var autoAwayZoneTimer: Int = 30,
    var autoAwayZoneSetbackTemp: Int = 2, // (°F)
    var minFanRuntimePostConditioning: Int = 5, // (min)

    // 2pipe additional tuners
    var heatingThreshold: Double = 85.0, // (°F)
    var coolingThreshold: Double = 65.0, // (°F)
    var auxHeating1Activate: Double = 3.0,      // (F)
    var auxHeating2Activate: Double = 4.0,      // (F)
    var waterValveSamplingOnTime : Int = 2,   // min
    var waterValveSamplingWaitTime : Int =  3, // 58,   // min
    var waterValveSamplingDuringLoopDeadbandOnTime : Int = 2,   // min
    var waterValveSamplingDuringLoopDeadbandWaitTime : Int = 5,   // min
)

data class MyStatBasicSettings(
    val conditioningMode: StandaloneConditioningMode,
    var fanMode: MyStatFanStages,
)

/**
 * Traditional 75f User Intent values (settings of user in space)
 */
data class MyStatUserIntents(
    val currentTemp: Double,
    val zoneCoolingTargetTemperature: Double,
    val zoneHeatingTargetTemperature: Double,
    val targetMinInsideHumidity: Double,
    val targetMaxInsideHumidity: Double
)

data class MyStatPipe2MinMaxConfig (
    val waterModulatingValue: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)

data class MyStatHpuMinMaxConfig (
    val compressorSpeed: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)

data class MyStatCpuMinMaxConfig (
    val cooling: MinMaxConfig,
    val linearFanSpeed: MinMaxConfig,
    val heating: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)