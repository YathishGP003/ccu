package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * @author tcase@75f.io
 * Created on 7/7/21 for HyperStat.
 *
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */


enum class HSSplitZoneStatus {
   STATUS,
   TARGET_HUMIDITY,
   TARGET_DEHUMIDIFY,
   CONDITIONING_ENABLED,
   CONDITIONING_MODE,
   FAN_MODE,
   FAN_LEVEL,
   DISCHARGE_AIRFLOW,
   MIXED_AIR_TEMP

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
data class HyperStatSplitProfileTuners(
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
   var minFanRuntimePostConditioning: Int = 5, // (min)
   var autoAwayZoneSetbackTemp: Int = 2, // (°F)
   var exhaustFanStage1Threshold: Int = 50, // %
   var exhaustFanStage2Threshold: Int = 90, // %
   var exhaustFanHysteresis: Int = 5, // %
)

enum class PossibleConditioningMode{
   OFF,COOLONLY,HEATONLY,BOTH
}

enum class PossibleFanMode{
   OFF,LOW,MED,HIGH,LOW_MED_HIGH,LOW_MED,LOW_HIGH,MED_HIGH
}

data class ConfigState(
   val enabled: Boolean,
   val association: Int
)