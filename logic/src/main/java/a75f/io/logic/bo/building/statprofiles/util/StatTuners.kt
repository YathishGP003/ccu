package a75f.io.logic.bo.building.statprofiles.util

/**
 * Created by Manjunath K on 22-04-2025.
 */
// Common base tuners - abstract class holds shared config
abstract class BaseStatTuners(
    var coolingDeadband: Double = 2.0, //(°F)
    var heatingDeadband: Double = 2.0, //(°F)
    var proportionalSpread: Double = 2.0,  //(°F)
    var integralMaxTimeout: Int = 30,  //(minutes)
    var proportionalGain: Double = 0.5,
    var integralGain: Double = 0.5,
    var relayActivationHysteresis: Int = 10,  //%
    var analogFanSpeedMultiplier: Double = 1.0,
    var humidityHysteresis: Int = 5, //%
    var minFanRuntimePostConditioning: Int = 5 // (min)
)


data class HyperStatProfileTuners(
    var heatingThreshold: Double = 85.0, // (°F)
    var coolingThreshold: Double = 65.0, // (°F)
    var auxHeating1Activate: Double = 3.0,      // (F)
    var auxHeating2Activate: Double = 4.0,      // (F)
    var waterValveSamplingOnTime: Int = 2,   // min
    var waterValveSamplingWaitTime: Int = 3, // min
    var waterValveSamplingDuringLoopDeadbandOnTime: Int = 2,   // min
    var waterValveSamplingDuringLoopDeadbandWaitTime: Int = 5  // min
) : BaseStatTuners()

// MyStat tuners (same structure as HyperStatProfileTuners!)
data class MyStatTuners(
    var heatingThreshold: Double = 85.0, // (°F)
    var coolingThreshold: Double = 65.0, // (°F)
    var auxHeating1Activate: Double = 3.0,      // (°F)
    var auxHeating2Activate: Double = 4.0,      // (°F)
    var waterValveSamplingOnTime: Int = 2,   // min
    var waterValveSamplingWaitTime: Int = 3, // min
    var waterValveSamplingDuringLoopDeadbandOnTime: Int = 2,   // min
    var waterValveSamplingDuringLoopDeadbandWaitTime: Int = 5  // min
) : BaseStatTuners()


data class UvTuners(
    var heatingThreshold: Double = 85.0, // (°F)
    var coolingThreshold: Double = 65.0, // (°F)
    var auxHeating1Activate: Double = 3.0,      // (F)
    var auxHeating2Activate: Double = 4.0,      // (F)
    var waterValveSamplingOnTime: Int = 2,   // min
    var waterValveSamplingWaitTime: Int = 3, // min
    var waterValveSamplingDuringLoopDeadbandOnTime: Int = 2,   // min
    var waterValveSamplingDuringLoopDeadbandWaitTime: Int = 5,  // min
    var saTemperingSetpoint: Double = 70.0, // (°F)
    var saTemperingIntegralKFactor: Double = 0.5,
    var saTemperingTemperatureIntegralTime: Int = 30,
    var saTemperingProportionalKFactor: Double = 0.5,
    var saTemperingTemperatureProportionalRange: Double = 10.0, // (°F)
    var economizingToMainCoolingLoopMap: Double = 30.0, // %
    var faceBypassDamperActivationHysteresis: Double = 10.0 // %
) : BaseStatTuners()
