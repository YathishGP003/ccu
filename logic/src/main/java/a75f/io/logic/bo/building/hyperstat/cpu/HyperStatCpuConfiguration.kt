package a75f.io.logic.bo.building.hyperstat.cpu

import a75f.io.logic.bo.building.BaseProfileConfiguration

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

   var analogOut1State = AnalogOutState(false, CpuAnalogOutAssociation.COOLING, 2.0, 10.0,30.0,60.0,100.0)
   var analogOut2State = AnalogOutState(false, CpuAnalogOutAssociation.FAN_SPEED, 2.0, 10.0,30.0,60.0,100.0)
   var analogOut3State = AnalogOutState(false, CpuAnalogOutAssociation.HEATING, 2.0, 10.0,30.0,60.0,100.0)

   var isEnableAirFlowTempSensor = false
   var isEnableDoorWindowSensor = false

   var analogIn1State = AnalogInState(false, CpuAnalogInAssociation.KEY_CARD_SENSOR)
   var analogIn2State = AnalogInState(false, CpuAnalogInAssociation.CURRENT_TX_0_20)

   var zoneCO2DamperOpeningRate = 10.0
   var zoneCO2Threshold = 800.0
   var zoneCO2Target = 1000.0

   companion object {
      fun default(): HyperStatCpuConfiguration = HyperStatCpuConfiguration()
   }
}

/**
 * State for a single relay: enabled and its mapping.
 */
data class RelayState(
   val enabled: Boolean,
   val association: CpuRelayAssociation
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
   val association: CpuAnalogInAssociation
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

// Order is important -- see comment above.
enum class CpuAnalogOutAssociation {
   COOLING,
   FAN_SPEED,
   HEATING,
   DCV_DAMPER
}

// Order is important -- see comment above.
enum class CpuAnalogInAssociation {
   CURRENT_TX_0_10,
   CURRENT_TX_0_20,
   CURRENT_TX_0_50,
   KEY_CARD_SENSOR,
   DOOR_WINDOW_SENSOR
}

