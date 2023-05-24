package a75f.io.logic.bo.building.hyperstat.profiles.cpu

import a75f.io.logic.bo.building.BaseProfileConfiguration
import android.widget.Spinner
import android.widget.TextView

/**
 * Models just the configuration for HyperStat CPU
 *
 * @author tcase@75f.io
 * Created on 7/7/21.
 */
class HyperStatCpuConfiguration : BaseProfileConfiguration() {

   var temperatureOffset = 0.0

   var isEnableAutoForceOccupied = false
   var isEnableAutoAway = false

   var relay1State = RelayState(false, CpuRelayAssociation.COOLING_STAGE_1)
   var relay2State = RelayState(false, CpuRelayAssociation.COOLING_STAGE_2)
   var relay3State = RelayState(false, CpuRelayAssociation.FAN_LOW_SPEED)
   var relay4State = RelayState(false, CpuRelayAssociation.HEATING_STAGE_1)
   var relay5State = RelayState(false, CpuRelayAssociation.HEATING_STAGE_2)
   var relay6State = RelayState(false, CpuRelayAssociation.FAN_HIGH_SPEED)

   var analogOut1State = AnalogOutState(false, CpuAnalogOutAssociation.COOLING, 2.0 ,10.0,70.0,80.0,100.0)
   var analogOut2State = AnalogOutState(false, CpuAnalogOutAssociation.LINEAR_FAN_SPEED, 2.0, 10.0,70.0,80.0,100.0)
   var analogOut3State = AnalogOutState(false, CpuAnalogOutAssociation.HEATING, 2.0, 10.0,70.0,80.0,100.0)

   var isEnableAirFlowTempSensor = false
   var isEnableDoorWindowSensor = false

   var analogIn1State = AnalogInState(false, AnalogInAssociation.KEY_CARD_SENSOR)
   var analogIn2State = AnalogInState(false, AnalogInAssociation.CURRENT_TX_0_20)

   var zoneCO2DamperOpeningRate = 10.0
   var zoneCO2Threshold = 4000.0
   var zoneCO2Target = 4000.0

   var zoneVOCThreshold = 10000.0
   var zoneVOCTarget = 10000.0
   var zonePm2p5Threshold = 1000.0
   var zonePm2p5Target = 1000.0

   var displayHumidity = true
   var displayVOC = false
   var displayPp2p5 = false
   var displayCo2 = true

   companion object {
      fun default(): HyperStatCpuConfiguration = HyperStatCpuConfiguration()
   }
}

/**
 * State for a single relay: enabled and its mapping.
 */
data class RelayState(
   val enabled: Boolean,
   var association: CpuRelayAssociation
)

data class AnalogOutState(
   val enabled: Boolean,
   val association: CpuAnalogOutAssociation,
   val voltageAtMin: Double,   // create position to value mapping here.
   val voltageAtMax: Double,    // create position to value mapping here.
   val perAtFanLow: Double,
   val perAtFanMedium: Double,
   val perAtFanHigh: Double,
)

data class AnalogInState(
   val enabled: Boolean,
   val association: AnalogInAssociation
)

data class StagedFanState(
   val stagedFanLabel: TextView,
   val stagedFanSelector: Spinner
)

// Order is important for this enum -- it matches the UI as set in xml & strings.xml and ordinal is saved in data storage.
// Do not change order without a migration.
enum class CpuRelayAssociation {
   COOLING_STAGE_1,
   COOLING_STAGE_2,
   COOLING_STAGE_3,
   HEATING_STAGE_1,
   HEATING_STAGE_2,
   HEATING_STAGE_3,
   FAN_LOW_SPEED,
   FAN_MEDIUM_SPEED,
   FAN_HIGH_SPEED,
   FAN_ENABLED,
   OCCUPIED_ENABLED,
   HUMIDIFIER,
   DEHUMIDIFIER
}

// Order is important -- FAN_HIGH_SPEED
enum class CpuAnalogOutAssociation {
   COOLING,
   LINEAR_FAN_SPEED,
   HEATING,
   DCV_DAMPER,
   STAGED_FAN_SPEED
}

// Order is important -- see comment above.
enum class AnalogInAssociation {
   CURRENT_TX_0_10,
   CURRENT_TX_0_20,
   CURRENT_TX_0_50,
   KEY_CARD_SENSOR,
   DOOR_WINDOW_SENSOR
}

