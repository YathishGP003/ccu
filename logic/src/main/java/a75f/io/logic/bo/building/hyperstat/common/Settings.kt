package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * @author tcase@75f.io
 * Created on 7/7/21.
 */


enum class HSZoneStatus {
   STATUS,
   TARGET_HUMIDITY,
   TARGET_DEHUMIDIFY,
   CONDITIONING_ENABLED,
   CONDITIONING_MODE,
   FAN_MODE,
   FAN_LEVEL,
   DISCHARGE_AIRFLOW,
   CONFIG,
   EQUIP,
   PROFILE_NAME,
   PROFILE_TYPE,
   SUPPLY_TEMP,
}

/**
 * Basic settings a user in the space might set.  These might be included (moved to) UserIntents
 */
data class BasicSettings(
    val conditioningMode: StandaloneConditioningMode,
    var fanMode: StandaloneFanStage,
)

/**
 * Traditional 75f User Intent values (settings of user in space)
 */
data class UserIntents(
   val currentTemp: Double,
   val zoneCoolingTargetTemperature: Double,   // (affected by scheduling of desired temperatures)
   val zoneHeatingTargetTemperature: Double,
   val targetMinInsideHumidity: Double,         // Same as system/ 25% [Available in the UI only if Humidifier/Dehumidifier Option has been configured]
   val targetMaxInsideHumidity: Double
)

/**
 * Tuner values.  Needs more look; I have not really gone through these in the requirements etc.
 */
data class HyperStatProfileTuners(
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



enum class PossibleConditioningMode{
   OFF,COOLONLY,HEATONLY,BOTH
}

enum class PossibleFanMode{
   OFF,LOW,MED,HIGH,LOW_MED_HIGH,LOW_MED,LOW_HIGH,MED_HIGH
}
