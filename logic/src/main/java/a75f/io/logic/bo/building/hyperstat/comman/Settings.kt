package a75f.io.logic.bo.building.hyperstat.comman

import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * @author tcase@75f.io
 * Created on 7/7/21.
 */

const val PROFILE ="Hyperstat CPU"

enum class HSZoneStatus {
   STATUS,
   TARGET_HUMIDITY,
   TARGET_DEHUMIDIFY,
   CONDITIONING_ENABLED,
   CONDITIONING_MODE,
   FAN_MODE,
   FAN_LEVEL,
   DISCHARGE_AIRFLOW

}

/**
 * Basic settings a user in the space might set.  These might be included (moved to) UserIntents
 */
data class BasicSettings(
   val conditioningMode: StandaloneConditioningMode,
   val fanMode: StandaloneFanStage,
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

)

// Used as key to hold point id
enum class LogicalKeyID {
   CURRENT_TEMP, DESIRED_TEMP, DESIRED_TEMP_COOLING, DESIRED_TEMP_HEATING, EQUIP_STATUS, EQUIP_STATUS_MESSAGE,
   SCHEDULE_TYPE, EQUIP_SCHEDULE_STATUS,
   MIN_COOLING, MAX_COOLING, MIN_HEATING, MAX_HEATING,
   MIN_FAN_SPEED, MAX_FAN_SPEED, MIN_DCV_DAMPER, MAX_DCV_DAMPER,
   FAN_LOW, FAN_MEDIUM, FAN_HIGH
}


enum class PossibleConditioningMode{
   OFF,COOLONLY,HEATONLY,BOTH
}

enum class PossibleFanMode{
   OFF,LOW,MED,HIGH,LOW_MED_HIGH,LOW_MED,LOW_HIGH,MED_HIGH
}

enum class AnalogOutChanges{
   NOCHANGE,ENABLED,MAPPING,MIN,MAX,LOW,MED,HIGH
}

