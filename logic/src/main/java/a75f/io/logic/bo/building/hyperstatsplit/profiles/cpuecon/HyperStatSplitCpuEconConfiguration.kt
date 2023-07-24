package a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon

import a75f.io.logic.bo.building.BaseProfileConfiguration

/**
 * Models just the configuration for HyperStat Split CPU/Economiser
 *
 * @author tcase@75f.io (HyperStat CPU)
 * Created on 7/7/21.
 *
 * Created for HyperStat Split CPU/Economiser by Nick P on 07-24-2023.
 */
class HyperStatSplitCpuEconConfiguration : BaseProfileConfiguration() {

   var temperatureOffset = 0.0

   var isEnableAutoForceOccupied = false
   var isEnableAutoAway = false

   /*
      Addresses of each device on the sensor bus needs to be modeled here because they are part of the configuration screen.
      (This is new for our profiles. Previously, the CCU did not have any knowledge of what was happening on the sensor bus.
      It was completely within the node/stat firmware.)

      This info is purely for configuration and should not need to be incorporated into control messages.
    */
   var address0State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)
   var address1State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)
   var address2State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)
   var address3State = SensorBusPressState(false, CpuEconSensorBusPressAssociation.DUCT_PRESSURE)

   var relay1State = RelayState(false, CpuEconRelayAssociation.COOLING_STAGE_1)
   var relay2State = RelayState(false, CpuEconRelayAssociation.COOLING_STAGE_2)
   var relay3State = RelayState(false, CpuEconRelayAssociation.FAN_LOW_SPEED)
   var relay4State = RelayState(false, CpuEconRelayAssociation.HEATING_STAGE_1)
   var relay5State = RelayState(false, CpuEconRelayAssociation.HEATING_STAGE_2)
   var relay6State = RelayState(false, CpuEconRelayAssociation.FAN_HIGH_SPEED)
   var relay7State = RelayState(false, CpuEconRelayAssociation.EXHAUST_FAN_STAGE_1)
   var relay8State = RelayState(false, CpuEconRelayAssociation.EXHAUST_FAN_STAGE_2)

   var analogOut1State = AnalogOutState(false, CpuEconAnalogOutAssociation.COOLING, 2.0 ,10.0,70.0,80.0,100.0)
   var analogOut2State = AnalogOutState(false, CpuEconAnalogOutAssociation.FAN_SPEED, 2.0, 10.0,70.0,80.0,100.0)
   var analogOut3State = AnalogOutState(false, CpuEconAnalogOutAssociation.HEATING, 2.0, 10.0,70.0,80.0,100.0)
   var analogOut4State = AnalogOutState(false, CpuEconAnalogOutAssociation.OAO_DAMPER, 2.0, 10.0,70.0,80.0,100.0)

   var universalIn1State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
   var universalIn2State = UniversalInState(false, UniversalInAssociation.MIXED_AIR_TEMPERATURE)
   var universalIn3State = UniversalInState(false, UniversalInAssociation.SUPPLY_AIR_TEMPERATURE)
   var universalIn4State = UniversalInState(false, UniversalInAssociation.CURRENT_TX_0_50)
   var universalIn5State = UniversalInState(false, UniversalInAssociation.CONDENSATE_NC)
   var universalIn6State = UniversalInState(false, UniversalInAssociation.FILTER_NC)
   var universalIn7State = UniversalInState(false, UniversalInAssociation.DUCT_PRESSURE_0_2)
   var universalIn8State = UniversalInState(false, UniversalInAssociation.CURRENT_TX_0_50)

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
      fun default(): HyperStatSplitCpuEconConfiguration = HyperStatSplitCpuEconConfiguration()
   }
}

/**
 * State for a single relay: enabled and its mapping.
 */
data class RelayState(
   val enabled: Boolean,
   var association: CpuEconRelayAssociation
)

data class AnalogOutState(
   val enabled: Boolean,
   val association: CpuEconAnalogOutAssociation,
   val voltageAtMin: Double,   // create position to value mapping here.
   val voltageAtMax: Double,    // create position to value mapping here.
   val perAtFanLow: Double,
   val perAtFanMedium: Double,
   val perAtFanHigh: Double,
)

/*
   Universal Input states are the same as other configurable inputs.
   There are more possible mappings for Universal Ins (thermistor, digital, or analog), but they
   are always determined by the Association.
 */
data class UniversalInState(
   val enabled: Boolean,
   val association: UniversalInAssociation
)

/*
   Per spec, Sensor Bus Temps are to reside on Addresses 0-2, and Pressures are to reside on Address 3.
   So, configurations need to be kept separate.
 */
data class SensorBusTempState(
   val enabled: Boolean,
   val association: CpuEconSensorBusTempAssociation
)

/*
   Per spec, Sensor Bus Temps are to reside on Addresses 0-2, and Pressures are to reside on Address 3.
   So, configurations need to be kept separate.
 */
data class SensorBusPressState(
   val enabled: Boolean,
   val association: CpuEconSensorBusPressAssociation
)

// Order is important for this enum -- it matches the UI as set in xml & strings.xml and ordinal is saved in data storage.
// Do not change order without a migration.
enum class CpuEconRelayAssociation {
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
   DEHUMIDIFIER,
   EXHAUST_FAN_STAGE_1,
   EXHAUST_FAN_STAGE_2
}

// Order is important -- FAN_HIGH_SPEED
enum class CpuEconAnalogOutAssociation {
   COOLING,
   FAN_SPEED,
   HEATING,
   OAO_DAMPER
}

// Order is important -- see comment above.
// TODO: Still need to finalize these inputs
enum class UniversalInAssociation {
   SUPPLY_AIR_TEMPERATURE,
   OUTSIDE_AIR_TEMPERATURE,
   MIXED_AIR_TEMPERATURE,
   CONDENSATE_NC,
   CONDENSATE_NO,
   CURRENT_TX_0_10,
   CURRENT_TX_0_20,
   CURRENT_TX_0_50,
   CURRENT_TX_0_100,
   CURRENT_TX_0_150,
   DUCT_PRESSURE_0_1,
   DUCT_PRESSURE_0_2,
   FILTER_NC,
   FILTER_NO
}

enum class CpuEconSensorBusTempAssociation {
   MIXED_AIR_TEMPERATURE_HUMIDITY,
   SUPPLY_AIR_TEMPERATURE_HUMIDITY,
   OUTSIDE_AIR_TEMPERATURE_HUMIDITY
}

enum class CpuEconSensorBusPressAssociation {
   DUCT_PRESSURE
}

