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
   var address0State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)
   var address1State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)
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
   var analogOut2State = AnalogOutState(false, CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED, 2.0, 10.0,70.0,80.0,100.0)
   var analogOut3State = AnalogOutState(false, CpuEconAnalogOutAssociation.HEATING, 2.0, 10.0,70.0,80.0,100.0)
   var analogOut4State = AnalogOutState(false, CpuEconAnalogOutAssociation.OAO_DAMPER, 2.0, 10.0,70.0,80.0,100.0)

   var universalIn1State = UniversalInState(false, UniversalInAssociation.SUPPLY_AIR_TEMPERATURE)
   var universalIn2State = UniversalInState(false, UniversalInAssociation.MIXED_AIR_TEMPERATURE)
   var universalIn3State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
   var universalIn4State = UniversalInState(false, UniversalInAssociation.DUCT_PRESSURE_0_2)
   var universalIn5State = UniversalInState(false, UniversalInAssociation.CURRENT_TX_0_50)
   var universalIn6State = UniversalInState(false, UniversalInAssociation.CONDENSATE_NO)
   var universalIn7State = UniversalInState(false, UniversalInAssociation.FILTER_NO)
   var universalIn8State = UniversalInState(false, UniversalInAssociation.GENERIC_RESISTANCE)

   var coolingStage1FanState = 7
   var coolingStage2FanState = 10
   var coolingStage3FanState = 10
   var heatingStage1FanState = 7
   var heatingStage2FanState = 10
   var heatingStage3FanState = 10

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

   override fun toString(): String {

      val returnStr: String = "\nHyperStatSplitCpuEconConfiguration: {\n" +
              "temperatureOffset: $temperatureOffset\n" +
              "isEnableAutoForcedOccupied: $isEnableAutoForceOccupied\n" +
              "isEnableAutoAway: $isEnableAutoAway\n\n" +
              "address0State: ${address0State.enabled}, ${address0State.association.name} \n" +
              "address1State: ${address1State.enabled}, ${address1State.association.name} \n" +
              "address2State: ${address2State.enabled}, ${address2State.association.name} \n" +
              "address3State: ${address3State.enabled}, ${address3State.association.name} \n\n" +
              "relay1State: ${relay1State.enabled}, ${relay1State.association.name} \n" +
              "relay2State: ${relay2State.enabled}, ${relay2State.association.name} \n" +
              "relay3State: ${relay3State.enabled}, ${relay3State.association.name} \n" +
              "relay4State: ${relay4State.enabled}, ${relay4State.association.name} \n" +
              "relay5State: ${relay5State.enabled}, ${relay5State.association.name} \n" +
              "relay6State: ${relay6State.enabled}, ${relay6State.association.name} \n" +
              "relay7State: ${relay7State.enabled}, ${relay7State.association.name} \n" +
              "relay8State: ${relay8State.enabled}, ${relay8State.association.name} \n\n" +
              "analogOut1State: ${analogOut1State.enabled}, ${analogOut1State.association.name} \n" +
              "analogOut2State: ${analogOut2State.enabled}, ${analogOut2State.association.name} \n" +
              "analogOut3State: ${analogOut3State.enabled}, ${analogOut3State.association.name} \n" +
              "analogOut4State: ${analogOut4State.enabled}, ${analogOut4State.association.name} \n\n" +
              "universalIn1State: ${universalIn1State.enabled}, ${universalIn1State.association.name} \n" +
              "universalIn2State: ${universalIn2State.enabled}, ${universalIn2State.association.name} \n" +
              "universalIn3State: ${universalIn3State.enabled}, ${universalIn3State.association.name} \n" +
              "universalIn4State: ${universalIn4State.enabled}, ${universalIn4State.association.name} \n" +
              "universalIn5State: ${universalIn5State.enabled}, ${universalIn5State.association.name} \n" +
              "universalIn6State: ${universalIn6State.enabled}, ${universalIn6State.association.name} \n" +
              "universalIn7State: ${universalIn7State.enabled}, ${universalIn7State.association.name} \n" +
              "universalIn8State: ${universalIn8State.enabled}, ${universalIn8State.association.name} \n\n" +
              "coolingStage1FanState: $coolingStage1FanState\n" +              
              "coolingStage2FanState: $coolingStage2FanState\n" +
              "coolingStage3FanState: $coolingStage3FanState\n" +
              "heatingStage1FanState: $heatingStage1FanState\n" +
              "heatingStage2FanState: $heatingStage2FanState\n" +
              "heatingStage3FanState: $heatingStage3FanState\n\n" +
              "zoneCO2DamperOpeningRate: $zoneCO2DamperOpeningRate\n" +
              "zoneCO2Threshold: $zoneCO2Threshold\n" +
              "zoneCO2Target: $zoneCO2Target\n\n" +
              "zoneVOCThreshold: $zoneVOCThreshold\n" +
              "zoneVOCTarget: $zoneVOCTarget\n" +
              "zonePm2p5Threshold: $zonePm2p5Threshold\n" +
              "zonePm2p5Target: $zonePm2p5Target\n\n" +
              "displayHumidity: $displayHumidity\n" +
              "displayVOC: $displayVOC\n" +
              "displayPp2p5: $displayPp2p5\n" +
              "displayCo2: $displayCo2\n" +
              "}\n"

      return returnStr
   }

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
   MODULATING_FAN_SPEED,
   HEATING,
   OAO_DAMPER,
   PREDEFINED_FAN_SPEED
}

// Order is important -- see comment above.
enum class UniversalInAssociation {
   CURRENT_TX_0_10,
   CURRENT_TX_0_20,
   CURRENT_TX_0_50,
   CURRENT_TX_0_100,
   CURRENT_TX_0_150,
   SUPPLY_AIR_TEMPERATURE,
   MIXED_AIR_TEMPERATURE,
   OUTSIDE_AIR_TEMPERATURE,
   FILTER_NC,
   FILTER_NO,
   CONDENSATE_NC,
   CONDENSATE_NO,
   DUCT_PRESSURE_0_1,
   DUCT_PRESSURE_0_2,
   GENERIC_VOLTAGE,
   GENERIC_RESISTANCE
}

enum class CpuEconSensorBusTempAssociation {
   MIXED_AIR_TEMPERATURE_HUMIDITY,
   SUPPLY_AIR_TEMPERATURE_HUMIDITY,
   OUTSIDE_AIR_TEMPERATURE_HUMIDITY
}

enum class CpuEconSensorBusPressAssociation {
   DUCT_PRESSURE
}

