package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.*
import a75f.io.logic.bo.building.sensors.SensorType
import android.util.Log

/**
 * Created for HyperStat by Manjunath K on 30-07-2021.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

class HyperStatSplitAssociationUtil {
    companion object {

        //Function which checks the Relay is Associated  to Fan or Not
        fun isRelayAssociatedToFan(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.FAN_LOW_SPEED
                    || relayState.association == CpuEconRelayAssociation.FAN_MEDIUM_SPEED
                    || relayState.association == CpuEconRelayAssociation.FAN_HIGH_SPEED)
        }

        //Function which checks the Relay is Associated  to Cooling Stage
        fun isRelayAssociatedToCoolingStage(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.COOLING_STAGE_1
                    || relayState.association == CpuEconRelayAssociation.COOLING_STAGE_2
                    || relayState.association == CpuEconRelayAssociation.COOLING_STAGE_3)

        }

        //Function which checks the Relay is Associated  to Heating Stage
        fun isRelayAssociatedToHeatingStage(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.HEATING_STAGE_1
                    || relayState.association == CpuEconRelayAssociation.HEATING_STAGE_2
                    || relayState.association == CpuEconRelayAssociation.HEATING_STAGE_3)

        }

        // Function which returns the Relay Mapped state
        fun getRelayAssociatedStage(state: Int): CpuEconRelayAssociation {
            return when (state) {
                // Order is important here
                0 -> CpuEconRelayAssociation.COOLING_STAGE_1
                1 -> CpuEconRelayAssociation.COOLING_STAGE_2
                2 -> CpuEconRelayAssociation.COOLING_STAGE_3
                3 -> CpuEconRelayAssociation.HEATING_STAGE_1
                4 -> CpuEconRelayAssociation.HEATING_STAGE_2
                5 -> CpuEconRelayAssociation.HEATING_STAGE_3
                6 -> CpuEconRelayAssociation.FAN_LOW_SPEED
                7 -> CpuEconRelayAssociation.FAN_MEDIUM_SPEED
                8 -> CpuEconRelayAssociation.FAN_HIGH_SPEED
                9 -> CpuEconRelayAssociation.FAN_ENABLED
                10 -> CpuEconRelayAssociation.OCCUPIED_ENABLED
                11 -> CpuEconRelayAssociation.HUMIDIFIER
                12 -> CpuEconRelayAssociation.DEHUMIDIFIER
                13 -> CpuEconRelayAssociation.EXHAUST_FAN_STAGE_1
                14 -> CpuEconRelayAssociation.EXHAUST_FAN_STAGE_2
                // assuming it never going to call
                else -> CpuEconRelayAssociation.COOLING_STAGE_1
            }

        }

        // Function which returns the Relay Mapped state
        fun getAnalogOutAssociatedStage(state: Int): CpuEconAnalogOutAssociation {
            return when (state) {
                // Order is important here
                0 -> CpuEconAnalogOutAssociation.COOLING
                1 -> CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED
                2 -> CpuEconAnalogOutAssociation.HEATING
                3 -> CpuEconAnalogOutAssociation.OAO_DAMPER
                4 -> CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED

                // assuming it never going to call
                else -> CpuEconAnalogOutAssociation.COOLING
            }
        }

        // Function which returns the Relay Mapped state
        fun getUniversalInStage(state: Int): UniversalInAssociation {
            return when (state) {
                // Order is important here
                0 -> UniversalInAssociation.CURRENT_TX_0_10
                1 -> UniversalInAssociation.CURRENT_TX_0_20
                2 -> UniversalInAssociation.CURRENT_TX_0_50
                3 -> UniversalInAssociation.CURRENT_TX_0_100
                4 -> UniversalInAssociation.CURRENT_TX_0_150
                5 -> UniversalInAssociation.SUPPLY_AIR_TEMPERATURE
                6 -> UniversalInAssociation.MIXED_AIR_TEMPERATURE
                7 -> UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE
                8 -> UniversalInAssociation.FILTER_NC
                9 -> UniversalInAssociation.FILTER_NO
                10 -> UniversalInAssociation.CONDENSATE_NC
                11 -> UniversalInAssociation.CONDENSATE_NO
                12 -> UniversalInAssociation.DUCT_PRESSURE_0_1
                13 -> UniversalInAssociation.DUCT_PRESSURE_0_2
                14 -> UniversalInAssociation.GENERIC_VOLTAGE
                15 -> UniversalInAssociation.GENERIC_RESISTANCE

                // assuming it never going to call
                else -> UniversalInAssociation.SUPPLY_AIR_TEMPERATURE
            }

        }

        // Function which returns the Sensor Bus Mapped state on the Temp/Humidity addresses (0-2)
        fun getSensorBusTempStage(state: Int): CpuEconSensorBusTempAssociation {
            return when (state) {
                // Order is important here

                0 -> CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY
                1 -> CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY
                2 -> CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY
                // assuming it never going to call
                else -> CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY
            }

        }

        // Function which returns the Sensor Bus Mapped state on the Pressure addresses (3)
        fun getSensorBusPressStage(state: Int): CpuEconSensorBusPressAssociation {
            return when (state) {
                // Order is important here
                0 -> CpuEconSensorBusPressAssociation.DUCT_PRESSURE
                // assuming it never going to call
                else -> CpuEconSensorBusPressAssociation.DUCT_PRESSURE
            }

        }

        //Function which checks the Sensor bus address is Associated  to SUPPLY_AIR_TEMPERATURE_HUMIDITY
        fun isSensorBusAddressAssociatedToSupplyAir(sensorBusState: SensorBusTempState): Boolean {
            return (sensorBusState.association == CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY && sensorBusState.enabled)
        }
        //Function which checks the Sensor bus address is Associated  to MIXED_AIR_TEMPERATURE_HUMIDITY
        fun isSensorBusAddressAssociatedToMixedAir(sensorBusState: SensorBusTempState): Boolean {
            return (sensorBusState.association == CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY && sensorBusState.enabled)
        }
        //Function which checks the Sensor bus address is Associated  to OUTSIDE_AIR_TEMPERATURE_HUMIDITY
        fun isSensorBusAddressAssociatedToOutsideAir(sensorBusState: SensorBusTempState): Boolean {
            return (sensorBusState.association == CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY && sensorBusState.enabled)
        }
        //Function which checks the Sensor bus address is Associated  to DUCT_PRESSURE
        fun isSensorBusAddressAssociatedToDuctPressure(sensorBusState: SensorBusPressState): Boolean {
            return (sensorBusState.association == CpuEconSensorBusPressAssociation.DUCT_PRESSURE && sensorBusState.enabled)
        }

        //Function which checks the Relay is Associated  to Fan Enabled
        fun isRelayAssociatedToFanEnabled(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.FAN_ENABLED)
        }

        //Function which checks the Relay is Associated  to OCCUPIED ENABLED
        fun isRelayAssociatedToOccupiedEnabled(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.OCCUPIED_ENABLED)
        }

        //Function which checks the Relay is Associated  to HUMIDIFIER
        fun isRelayAssociatedToHumidifier(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.HUMIDIFIER)
        }

        //Function which checks the Relay is Associated  to DEHUMIDIFIER
        fun isRelayAssociatedToDeHumidifier(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.DEHUMIDIFIER)
        }

        //Function which checks the Relay is Associated  to EXHAUST_FAN_STAGE_1
        fun isRelayAssociatedToExhaustFanStage1(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.EXHAUST_FAN_STAGE_1)
        }

        //Function which checks the Relay is Associated  to EXHAUST_FAN_STAGE_2
        fun isRelayAssociatedToExhaustFanStage2(relayState: RelayState): Boolean {
            return (relayState.association == CpuEconRelayAssociation.EXHAUST_FAN_STAGE_2)
        }

        //Function which checks the Analog out is Associated  to Cooling
        fun isAnalogOutAssociatedToCooling(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuEconAnalogOutAssociation.COOLING)
        }

        //Function which checks the Analog out is Associated  to FAN_SPEED
        fun isAnalogOutAssociatedToFanSpeed(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED)
        }

        //Function which checks the Analog out is Associated  to HEATING
        fun isAnalogOutAssociatedToHeating(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuEconAnalogOutAssociation.HEATING)
        }

        //Function which checks the Analog out is Associated  to OAO Damper
        fun isAnalogOutAssociatedToOaoDamper(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuEconAnalogOutAssociation.OAO_DAMPER)
        }

        //Function which checks the Analog out is Associated  to STAGED_FAN_SPEED
        fun isAnalogOutAssociatedToStagedFanSpeed(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED)
        }

        //Function which checks the Universal in is Associated  to SUPPLY_AIR_TEMPERATURE
        fun isUniversalInAssociatedToSupplyAirTemperature(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.SUPPLY_AIR_TEMPERATURE)
        }
        //Function which checks the Universal in is Associated  to MIXED_AIR_TEMPERATURE
        fun isUniversalInAssociatedToMixedAirTemperature(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.MIXED_AIR_TEMPERATURE)
        }
        //Function which checks the Universal in is Associated  to OUTSIDE_AIR_TEMPERATURE
        fun isUniversalInAssociatedToOutsideAirTemperature(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
        }
        //Function which checks the Universal in is Associated  to CONDENSATE_NO
        // TODO: verify this point is included
        fun isUniversalInAssociatedToCondensateNO(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.CONDENSATE_NO)
        }
        //Function which checks the Universal in is Associated  to CONDENSATE_NC
        // TODO: verify this point is included
        fun isUniversalInAssociatedToCondensateNC(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.CONDENSATE_NC)
        }
        //Function which checks the Universal in is Associated  to CURRENT_TX_0_10
        fun isUniversalInAssociatedToCurrentTX10(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.CURRENT_TX_0_10)
        }
        //Function which checks the Universal in is Associated  to CURRENT_TX_0_20
        fun isUniversalInAssociatedToCurrentTX20(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.CURRENT_TX_0_20)
        }
        //Function which checks the Universal in is Associated  to CURRENT_TX_0_50
        fun isUniversalInAssociatedToCurrentTX50(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.CURRENT_TX_0_50)
        }
        //Function which checks the Universal in is Associated  to CURRENT_TX_0_100
        fun isUniversalInAssociatedToCurrentTX100(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.CURRENT_TX_0_100)
        }
        //Function which checks the Universal in is Associated  to CURRENT_TX_0_150
        fun isUniversalInAssociatedToCurrentTX150(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.CURRENT_TX_0_150)
        }
        //Function which checks the Universal in is Associated  to DUCT_PRESSURE_0_1
        fun isUniversalInAssociatedToDuctPressure1In(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.DUCT_PRESSURE_0_1)
        }
        //Function which checks the Universal in is Associated  to DUCT_PRESSURE_0_2
        fun isUniversalInAssociatedToDuctPressure2In(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.DUCT_PRESSURE_0_2)
        }
        //Function which checks the Universal in is Associated  to FILTER_NO
        fun isUniversalInAssociatedToFilterNO(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.FILTER_NO)
        }
        //Function which checks the Universal in is Associated  to FILTER_NC
        fun isUniversalInAssociatedToFilterNC(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.FILTER_NC)
        }
        //Function which checks the Universal in is Associated  to GENERIC_VOLTAGE
        fun isUniversalInAssociatedToGenericVoltage(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.GENERIC_VOLTAGE)
        }
        //Function which checks the Universal in is Associated  to GENERIC_RESISTANCE
        fun isUniversalInAssociatedToGenericResistance(universalIn: UniversalInState): Boolean {
            return (universalIn.association == UniversalInAssociation.GENERIC_RESISTANCE)
        }

        fun isAnyRelayAssociatedToCoolingStage1(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.COOLING_STAGE_1)
        }
        fun isAnyRelayAssociatedToCoolingStage2(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.COOLING_STAGE_2)
        }
        fun isAnyRelayAssociatedToCoolingStage3(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.COOLING_STAGE_3)
        }
        fun isAnyRelayAssociatedToHeatingStage1(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.HEATING_STAGE_1)
        }
        fun isAnyRelayAssociatedToHeatingStage2(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.HEATING_STAGE_2)
        }
        fun isAnyRelayAssociatedToHeatingStage3(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.HEATING_STAGE_3)
        }
        fun isAnyRelayAssociatedToFanLow(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.FAN_LOW_SPEED)
        }
        fun isAnyRelayAssociatedToFanMedium(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.FAN_MEDIUM_SPEED)
        }
        fun isAnyRelayAssociatedToFanHigh(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.FAN_HIGH_SPEED)
        }
        fun isAnyRelayAssociatedToFanEnabled(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.FAN_ENABLED)
        }
        fun isAnyRelayAssociatedToOccupiedEnabled(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.OCCUPIED_ENABLED)
        }
        fun isAnyRelayEnabledAssociatedToHumidifier(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.HUMIDIFIER)
        }
        fun isAnyRelayEnabledAssociatedToDeHumidifier(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.DEHUMIDIFIER)
        }
        fun isAnyRelayEnabledAssociatedToExhaustFanStage1(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.EXHAUST_FAN_STAGE_1)
        }
        fun isAnyRelayEnabledAssociatedToExhaustFanStage2(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuEconRelayAssociation.EXHAUST_FAN_STAGE_2)
        }
        // Function which checks that any of the relay is associated to Humidifier
        fun isAnyRelayAssociatedToHumidifier(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (isRelayAssociatedToHumidifier(configuration.relay1State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay2State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay3State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay4State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay5State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay6State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay7State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay8State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay is associated to DeHumidifier
        fun isAnyRelayAssociatedToDeHumidifier(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (isRelayAssociatedToDeHumidifier(configuration.relay1State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay2State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay3State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay4State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay5State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay6State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay7State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay8State)) -> true
                else -> false
            }
        }

        private fun isAnyRelayMapped(config: HyperStatSplitCpuEconConfiguration, association: CpuEconRelayAssociation): Boolean{
            return when {
                (config.relay1State.enabled && config.relay1State.association == association) -> true
                (config.relay2State.enabled && config.relay2State.association == association) -> true
                (config.relay3State.enabled && config.relay3State.association == association) -> true
                (config.relay4State.enabled && config.relay4State.association == association) -> true
                (config.relay5State.enabled && config.relay5State.association == association) -> true
                (config.relay6State.enabled && config.relay6State.association == association) -> true
                (config.relay7State.enabled && config.relay7State.association == association) -> true
                (config.relay8State.enabled && config.relay8State.association == association) -> true
                else -> false
            }
        }
        fun isAnyAnalogAssociatedToCooling(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuEconAnalogOutAssociation.COOLING)
        }
        fun isAnyAnalogAssociatedToHeating(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuEconAnalogOutAssociation.HEATING)
        }
        fun isAnyAnalogAssociatedToFan(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED)
        }
        fun isAnyAnalogAssociatedToOAO(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuEconAnalogOutAssociation.OAO_DAMPER)
        }
        fun isAnyAnalogAssociatedToStaged(config: HyperStatSplitCpuEconConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED)
        }
        private fun isAnalogOutMapped(config: HyperStatSplitCpuEconConfiguration, association: CpuEconAnalogOutAssociation): Boolean{
            return when {
                (config.analogOut1State.enabled && config.analogOut1State.association == association) -> true
                (config.analogOut2State.enabled && config.analogOut2State.association == association) -> true
                (config.analogOut3State.enabled && config.analogOut3State.association == association) -> true
                (config.analogOut4State.enabled && config.analogOut4State.association == association) -> true
                else -> false
            }
        }

        private fun isUniversalInMapped(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
            association: UniversalInAssociation
        ): Boolean{
            return when {
                (ui1.enabled && ui1.association == association) -> true
                (ui2.enabled && ui2.association == association) -> true
                (ui3.enabled && ui3.association == association) -> true
                (ui4.enabled && ui4.association == association) -> true
                (ui5.enabled && ui5.association == association) -> true
                (ui6.enabled && ui6.association == association) -> true
                (ui7.enabled && ui7.association == association) -> true
                (ui8.enabled && ui8.association == association) -> true

                else -> false
            }
        }
        fun isAnyUniversalInMappedToSupplyAirTemperature(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.SUPPLY_AIR_TEMPERATURE)
        }
        fun isAnyUniversalInMappedToOutsideAirTemperature(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
        }
        fun isAnyUniversalInMappedToMixedAirTemperature(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.MIXED_AIR_TEMPERATURE)
        }
        fun isAnyUniversalInMappedToFilterNC(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.FILTER_NC)
        }
        fun isAnyUniversalInMappedToFilterNO(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.FILTER_NO)
        }
        fun isAnyUniversalInMappedToCondensateNC(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.CONDENSATE_NC)
        }
        fun isAnyUniversalInMappedToCondensateNO(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.CONDENSATE_NO)
        }
        fun isAnyUniversalInMappedToGenericVoltage(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.GENERIC_VOLTAGE)
        }
        fun isAnyUniversalInMappedToGenericResistance(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.GENERIC_RESISTANCE)
        }
        fun isAnyUniversalInMappedToCT10(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.CURRENT_TX_0_10)
        }
        fun isAnyUniversalInMappedToCT20(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.CURRENT_TX_0_20)
        }
        fun isAnyUniversalInMappedToCT50(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.CURRENT_TX_0_50)
        }
        fun isAnyUniversalInMappedToCT100(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.CURRENT_TX_0_100)
        }
        fun isAnyUniversalInMappedToCT150(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.CURRENT_TX_0_150)
        }
        fun isAnyUniversalInMappedToDuctPressure0to1(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.DUCT_PRESSURE_0_1)
        }
        fun isAnyUniversalInMappedToDuctPressure0to2(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
        ): Boolean {
            return isUniversalInMapped(ui1,ui2,ui3,ui4,ui5,ui6,ui7,ui8,UniversalInAssociation.DUCT_PRESSURE_0_2)
        }

        private fun isUniversalInDuplicated(
            ui1: UniversalInState, ui2: UniversalInState,
            ui3: UniversalInState, ui4: UniversalInState,
            ui5: UniversalInState, ui6: UniversalInState,
            ui7: UniversalInState, ui8: UniversalInState,
            association: UniversalInAssociation
        ): Boolean{
            var nMappings = 0

            if (ui1.enabled && ui1.association==association) nMappings++
            if (ui2.enabled && ui2.association==association) nMappings++
            if (ui3.enabled && ui3.association==association) nMappings++
            if (ui4.enabled && ui4.association==association) nMappings++
            if (ui5.enabled && ui5.association==association) nMappings++
            if (ui6.enabled && ui6.association==association) nMappings++
            if (ui7.enabled && ui7.association==association) nMappings++
            if (ui8.enabled && ui8.association==association) nMappings++

            return nMappings > 1
        }
        private fun isSensorBusTempDuplicated(
            addr0: SensorBusTempState,
            addr1: SensorBusTempState,
            addr2: SensorBusTempState,
            association: CpuEconSensorBusTempAssociation
        ): Boolean{
            var nMappings = 0

            if (addr0.enabled && addr0.association==association) nMappings++
            if (addr1.enabled && addr1.association==association) nMappings++
            if (addr2.enabled && addr2.association==association) nMappings++

            return nMappings > 1
        }

        fun isOutsideAirTemperatureDuplicated(
            addr0State: SensorBusTempState,
            addr1State: SensorBusTempState,
            addr2State: SensorBusTempState,
            uniIn1State: UniversalInState,
            uniIn2State: UniversalInState,
            uniIn3State: UniversalInState,
            uniIn4State: UniversalInState,
            uniIn5State: UniversalInState,
            uniIn6State: UniversalInState,
            uniIn7State: UniversalInState,
            uniIn8State: UniversalInState
        ): Boolean {
            if (isSensorBusTempDuplicated(
                    addr0State, addr1State, addr2State,
                    CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)) return true

            if (isAnySensorBusAddressMappedToOutsideAir(addr0State,addr1State,addr2State)
                        && isAnyUniversalInMappedToOutsideAirTemperature(
                            uniIn1State, uniIn2State,
                            uniIn3State, uniIn4State,
                            uniIn5State, uniIn6State,
                            uniIn7State, uniIn8State)) return true

            return false
        }
        fun isSupplyAirTemperatureDuplicated(
            addr0State: SensorBusTempState,
            addr1State: SensorBusTempState,
            addr2State: SensorBusTempState,
            uniIn1State: UniversalInState,
            uniIn2State: UniversalInState,
            uniIn3State: UniversalInState,
            uniIn4State: UniversalInState,
            uniIn5State: UniversalInState,
            uniIn6State: UniversalInState,
            uniIn7State: UniversalInState,
            uniIn8State: UniversalInState
        ): Boolean {
            if (isSensorBusTempDuplicated(
                    addr0State, addr1State, addr2State,
                    CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.SUPPLY_AIR_TEMPERATURE)) return true

            if (isAnySensorBusAddressMappedToSupplyAir(addr0State,addr1State,addr2State)
                && isAnyUniversalInMappedToSupplyAirTemperature(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) return true

            return false
        }
        fun isMixedAirTemperatureDuplicated(
            addr0State: SensorBusTempState,
            addr1State: SensorBusTempState,
            addr2State: SensorBusTempState,
            uniIn1State: UniversalInState,
            uniIn2State: UniversalInState,
            uniIn3State: UniversalInState,
            uniIn4State: UniversalInState,
            uniIn5State: UniversalInState,
            uniIn6State: UniversalInState,
            uniIn7State: UniversalInState,
            uniIn8State: UniversalInState
        ): Boolean {
            if (isSensorBusTempDuplicated(
                    addr0State, addr1State, addr2State,
                    CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.MIXED_AIR_TEMPERATURE)) return true

            if (isAnySensorBusAddressMappedToMixedAir(addr0State,addr1State,addr2State)
                && isAnyUniversalInMappedToMixedAirTemperature(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) return true

            return false
        }
        fun isDuctPressureDuplicated(
            addr3State: SensorBusPressState,
            uniIn1State: UniversalInState,
            uniIn2State: UniversalInState,
            uniIn3State: UniversalInState,
            uniIn4State: UniversalInState,
            uniIn5State: UniversalInState,
            uniIn6State: UniversalInState,
            uniIn7State: UniversalInState,
            uniIn8State: UniversalInState
        ): Boolean {
            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.DUCT_PRESSURE_0_1)
                || isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.DUCT_PRESSURE_0_2)
                || (isUniversalInMapped(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.DUCT_PRESSURE_0_1) &&
                        isUniversalInMapped(
                            uniIn1State, uniIn2State,
                            uniIn3State, uniIn4State,
                            uniIn5State, uniIn6State,
                            uniIn7State, uniIn8State,
                            UniversalInAssociation.DUCT_PRESSURE_0_2))) return true

            if(isSensorBusAddressAssociatedToDuctPressure(addr3State) &&
                        (isUniversalInMapped(
                            uniIn1State, uniIn2State,
                            uniIn3State, uniIn4State,
                            uniIn5State, uniIn6State,
                            uniIn7State, uniIn8State,
                            UniversalInAssociation.DUCT_PRESSURE_0_1) ||
                        isUniversalInMapped(
                            uniIn1State, uniIn2State,
                            uniIn3State, uniIn4State,
                            uniIn5State, uniIn6State,
                            uniIn7State, uniIn8State,
                            UniversalInAssociation.DUCT_PRESSURE_0_2))) return true

            return false
        }

        fun isFilterStatusDuplicated(
            uniIn1State: UniversalInState,
            uniIn2State: UniversalInState,
            uniIn3State: UniversalInState,
            uniIn4State: UniversalInState,
            uniIn5State: UniversalInState,
            uniIn6State: UniversalInState,
            uniIn7State: UniversalInState,
            uniIn8State: UniversalInState
        ): Boolean {
            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.FILTER_NC)) return true

            if (isUniversalInDuplicated(
                uniIn1State, uniIn2State,
                uniIn3State, uniIn4State,
                uniIn5State, uniIn6State,
                uniIn7State, uniIn8State,
                UniversalInAssociation.FILTER_NO)) return true

            if (isAnyUniversalInMappedToFilterNO(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)
                && isAnyUniversalInMappedToFilterNC(
                uniIn1State, uniIn2State,
                uniIn3State, uniIn4State,
                uniIn5State, uniIn6State,
                uniIn7State, uniIn8State)) return true

            return false
        }

        fun isCondensateOverflowStatusDuplicated(
            uniIn1State: UniversalInState,
            uniIn2State: UniversalInState,
            uniIn3State: UniversalInState,
            uniIn4State: UniversalInState,
            uniIn5State: UniversalInState,
            uniIn6State: UniversalInState,
            uniIn7State: UniversalInState,
            uniIn8State: UniversalInState
        ): Boolean {
            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.CONDENSATE_NC)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.CONDENSATE_NO)) return true

            if (isAnyUniversalInMappedToCondensateNO(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)
                && isAnyUniversalInMappedToCondensateNC(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) return true

            return false
        }
        fun isCurrentTXDuplicated(
            uniIn1State: UniversalInState,
            uniIn2State: UniversalInState,
            uniIn3State: UniversalInState,
            uniIn4State: UniversalInState,
            uniIn5State: UniversalInState,
            uniIn6State: UniversalInState,
            uniIn7State: UniversalInState,
            uniIn8State: UniversalInState
        ): Boolean {
            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.CURRENT_TX_0_10)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.CURRENT_TX_0_20)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.CURRENT_TX_0_50)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.CURRENT_TX_0_100)) return true

            if (isUniversalInDuplicated(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State,
                    UniversalInAssociation.CURRENT_TX_0_150)) return true

            var nMappings = 0

            if (isAnyUniversalInMappedToCT10(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) nMappings++

            if (isAnyUniversalInMappedToCT20(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) nMappings++

            if (isAnyUniversalInMappedToCT50(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) nMappings++

            if (isAnyUniversalInMappedToCT100(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) nMappings++

            if (isAnyUniversalInMappedToCT150(
                    uniIn1State, uniIn2State,
                    uniIn3State, uniIn4State,
                    uniIn5State, uniIn6State,
                    uniIn7State, uniIn8State)) nMappings++

            return nMappings > 1
        }

        fun isAnySensorBusAddressMappedToMixedAir(
            addr0: SensorBusTempState, 
            addr1: SensorBusTempState,
            addr2: SensorBusTempState
        ): Boolean {
            return isSensorBusAddressAssociatedToMixedAir(addr0)
                    || isSensorBusAddressAssociatedToMixedAir(addr1)
                    || isSensorBusAddressAssociatedToMixedAir(addr2)
        }
        fun isAnySensorBusAddressMappedToSupplyAir(
            addr0: SensorBusTempState,
            addr1: SensorBusTempState,
            addr2: SensorBusTempState
        ): Boolean {
            return isSensorBusAddressAssociatedToSupplyAir(addr0)
                    || isSensorBusAddressAssociatedToSupplyAir(addr1)
                    || isSensorBusAddressAssociatedToSupplyAir(addr2)
        }
        fun isAnySensorBusAddressMappedToOutsideAir(
            addr0: SensorBusTempState,
            addr1: SensorBusTempState,
            addr2: SensorBusTempState
        ): Boolean {
            return isSensorBusAddressAssociatedToOutsideAir(addr0)
                    || isSensorBusAddressAssociatedToOutsideAir(addr1)
                    || isSensorBusAddressAssociatedToOutsideAir(addr2)
        }
        fun isAnySensorBusAddressMappedToDuctPressure(
            addr3: SensorBusPressState
        ): Boolean {
            return isSensorBusAddressAssociatedToDuctPressure(addr3)
        }

        // checks two Relay configurations and return based on the match
        fun isBothRelayHasSameConfigs(relayState1: RelayState, relayState2: RelayState): Boolean {
            when {
                (relayState1.enabled != relayState2.enabled) -> return false
                (relayState1.association != relayState2.association) -> return false
            }
            return true
        }

        // checks two Analog out configurations and return based on the match
        fun isBothAnalogOutHasSameConfigs(analogOut1: AnalogOutState, analogOut2: AnalogOutState): Boolean {
            when {
                (analogOut1.enabled != analogOut2.enabled) -> return false
                (analogOut1.association != analogOut2.association) -> return false
                (analogOut1.voltageAtMin != analogOut2.voltageAtMin) -> return false
                (analogOut1.voltageAtMax != analogOut2.voltageAtMax) -> return false
                (isAnalogOutAssociatedToFanSpeed(analogOut1) || isAnalogOutAssociatedToStagedFanSpeed(analogOut1)) -> {
                    when {
                        (analogOut1.perAtFanLow != analogOut2.perAtFanLow) -> return false
                        (analogOut1.perAtFanMedium != analogOut2.perAtFanMedium) -> return false
                        (analogOut1.perAtFanHigh != analogOut2.perAtFanHigh) -> return false
                    }
                }
            }
            return true
        }

        // Function finds the analog out changes
        fun findChangeInAnalogOutConfig(analogOut1: AnalogOutState, analogOut2: AnalogOutState): AnalogOutChanges{
                when {
                    (analogOut1.enabled != analogOut2.enabled) -> return AnalogOutChanges.ENABLED
                    (analogOut1.association != analogOut2.association) -> return AnalogOutChanges.MAPPING
                    (analogOut1.voltageAtMin != analogOut2.voltageAtMin) -> return AnalogOutChanges.MIN
                    (analogOut1.voltageAtMax != analogOut2.voltageAtMax) -> return AnalogOutChanges.MAX
                    (isAnalogOutAssociatedToFanSpeed(analogOut1)) -> {
                        when {
                            (analogOut1.perAtFanLow != analogOut2.perAtFanLow) -> return AnalogOutChanges.LOW
                            (analogOut1.perAtFanMedium != analogOut2.perAtFanMedium) -> return AnalogOutChanges.MED
                            (analogOut1.perAtFanHigh != analogOut2.perAtFanHigh) -> return AnalogOutChanges.HIGH
                        }
                    }
                }
                return AnalogOutChanges.NOCHANGE
        }

        // checks two SensorBusTemp configurations and return based on the match
        fun isBothSensorBusAddressHasSameConfigs(addr1: SensorBusTempState, addr2: SensorBusTempState): Boolean {
            when {
                (addr1.enabled != addr2.enabled) -> return false
                (addr1.association != addr2.association) -> return false
            }
            return true
        }

        // checks two SensorBusPress configurations and return based on the match
        fun isBothSensorBusAddressHasSameConfigs(addr1: SensorBusPressState, addr2: SensorBusPressState): Boolean {
            when {
                (addr1.enabled != addr2.enabled) -> return false
                (addr1.association != addr2.association) -> return false
            }
            return true
        }

        // checks two Universal In configurations and return based on the match
        fun isBothUniversalInHasSameConfigs(universalIn1: UniversalInState, universalIn2: UniversalInState): Boolean {
            when {
                (universalIn1.enabled != universalIn2.enabled) -> return false
                (universalIn1.association != universalIn2.association) -> return false
            }
            return true
        }


        fun getSelectedFanLevel(configuration: HyperStatSplitCpuEconConfiguration): Int {

            var fanLevel = 0
            var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
                first = false,  //  Fan low
                second = false, //  Fan Medium
                third = false   //  Fan High
            )

            if(isAnyAnalogOutEnabledAssociatedToFanSpeed(configuration) || isAnyAnalogOutMappedToStagedFan(configuration)) return 21 // All options are enabled due to
            // analog fan speed

            if (isRelayEnabledAssociatedToFan(configuration.relay1State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay1State.association, fanEnabledStages)

            if (isRelayEnabledAssociatedToFan(configuration.relay2State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay2State.association, fanEnabledStages)

            if (isRelayEnabledAssociatedToFan(configuration.relay3State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay3State.association, fanEnabledStages)

            if (isRelayEnabledAssociatedToFan(configuration.relay4State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay4State.association, fanEnabledStages)

            if (isRelayEnabledAssociatedToFan(configuration.relay5State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay5State.association, fanEnabledStages)

            if (isRelayEnabledAssociatedToFan(configuration.relay6State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay6State.association, fanEnabledStages)

            if (fanEnabledStages.first) fanLevel += 6
            if (fanEnabledStages.second) fanLevel += 7
            if (fanEnabledStages.third) fanLevel += 8
            return fanLevel
        }

        private fun updateSelectedFanLevel(
            association: CpuEconRelayAssociation, currentFoundDetails: Triple<Boolean, Boolean, Boolean>
        ): Triple<Boolean, Boolean, Boolean> {
            var currentStatus = currentFoundDetails
            if (!currentStatus.first) {
                currentStatus = currentStatus.copy(
                    first = association.ordinal == CpuEconRelayAssociation.FAN_LOW_SPEED.ordinal
                )
            }
            if (!currentStatus.second) {
                currentStatus = currentStatus.copy(
                    second = association.ordinal == CpuEconRelayAssociation.FAN_MEDIUM_SPEED.ordinal
                )
            }
            if (!currentStatus.third) {
                currentStatus = currentStatus.copy(
                    third = association.ordinal == CpuEconRelayAssociation.FAN_HIGH_SPEED.ordinal
                )
            }
            return currentStatus
        }

        // Function which checks that any of the relay Enabled and is associated to Heating
        fun isAnyRelayEnabledAssociatedToFan(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (configuration.relay1State.enabled &&
                        isRelayAssociatedToFan(configuration.relay1State)) -> true
                (configuration.relay2State.enabled &&
                        isRelayAssociatedToFan(configuration.relay2State)) -> true
                (configuration.relay3State.enabled &&
                        isRelayAssociatedToFan(configuration.relay3State)) -> true
                (configuration.relay4State.enabled &&
                        isRelayAssociatedToFan(configuration.relay4State)) -> true
                (configuration.relay5State.enabled &&
                        isRelayAssociatedToFan(configuration.relay5State)) -> true
                (configuration.relay6State.enabled &&
                        isRelayAssociatedToFan(configuration.relay6State)) -> true
                (configuration.relay7State.enabled &&
                        isRelayAssociatedToFan(configuration.relay7State)) -> true
                (configuration.relay8State.enabled &&
                        isRelayAssociatedToFan(configuration.relay8State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to Heating
        fun isAnyRelayEnabledAssociatedToHeating(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (configuration.relay1State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay1State)) -> true
                (configuration.relay2State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay2State)) -> true
                (configuration.relay3State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay3State)) -> true
                (configuration.relay4State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay4State)) -> true
                (configuration.relay5State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay5State)) -> true
                (configuration.relay6State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay6State)) -> true
                (configuration.relay7State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay7State)) -> true
                (configuration.relay8State.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay8State)) -> true
                else -> false
            }
        }

        // function to check the input relay is enabled and associated to fan speed
        private fun isRelayEnabledAssociatedToFan(relayState: RelayState): Boolean {
         return (relayState.enabled && isRelayAssociatedToFan(relayState))
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToCooling(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (configuration.relay1State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay1State)) -> true
                (configuration.relay2State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay2State)) -> true
                (configuration.relay3State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay3State)) -> true
                (configuration.relay4State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay4State)) -> true
                (configuration.relay5State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay5State)) -> true
                (configuration.relay6State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay6State)) -> true
                (configuration.relay7State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay7State)) -> true
                (configuration.relay8State.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay8State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Cooling
        fun isAnyAnalogOutEnabledAssociatedToCooling(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (configuration.analogOut1State.enabled &&
                        isAnalogOutAssociatedToCooling(configuration.analogOut1State)) -> true
                (configuration.analogOut2State.enabled &&
                        isAnalogOutAssociatedToCooling(configuration.analogOut2State)) -> true
                (configuration.analogOut3State.enabled &&
                        isAnalogOutAssociatedToCooling(configuration.analogOut3State)) -> true
                (configuration.analogOut4State.enabled &&
                        isAnalogOutAssociatedToCooling(configuration.analogOut4State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Heating
        fun isAnyAnalogOutEnabledAssociatedToHeating(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (configuration.analogOut1State.enabled &&
                        isAnalogOutAssociatedToHeating(configuration.analogOut1State)) -> true
                (configuration.analogOut2State.enabled &&
                        isAnalogOutAssociatedToHeating(configuration.analogOut2State)) -> true
                (configuration.analogOut3State.enabled &&
                        isAnalogOutAssociatedToHeating(configuration.analogOut3State)) -> true
                (configuration.analogOut4State.enabled &&
                        isAnalogOutAssociatedToHeating(configuration.analogOut4State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Fan speed
        fun isAnyAnalogOutEnabledAssociatedToFanSpeed(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (configuration.analogOut1State.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut1State)) -> true
                (configuration.analogOut2State.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut2State)) -> true
                (configuration.analogOut3State.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut3State)) -> true
                (configuration.analogOut4State.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut4State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to OAO Damper
        fun isAnyAnalogOutEnabledAssociatedToOaoDamper(configuration: HyperStatSplitCpuEconConfiguration): Boolean {
            return when {
                (configuration.analogOut1State.enabled &&
                        isAnalogOutAssociatedToOaoDamper(configuration.analogOut1State)) -> true
                (configuration.analogOut2State.enabled &&
                        isAnalogOutAssociatedToOaoDamper(configuration.analogOut2State)) -> true
                (configuration.analogOut3State.enabled &&
                        isAnalogOutAssociatedToOaoDamper(configuration.analogOut3State)) -> true
                (configuration.analogOut4State.enabled &&
                        isAnalogOutAssociatedToOaoDamper(configuration.analogOut4State)) -> true
                else -> false
            }
        }


        // function which checks the any of the relay is associated to any conditioning
        fun isRelayAssociatedToAnyOfConditioningModes(relayState: RelayState): Boolean{
            if(isRelayAssociatedToCoolingStage(relayState)) return true
            if(isRelayAssociatedToHeatingStage(relayState)) return true
            return false
        }

        // function which checks the any of the relay is associated to cooling heating fan stage or fan enabled
        fun isAnalogAssociatedToAnyOfConditioningModes(analogOut: AnalogOutState): Boolean{
            if(isAnalogOutAssociatedToCooling(analogOut)) return true
            if(isAnalogOutAssociatedToHeating(analogOut)) return true
            return false
        }

        // Function returns highest selected cooling stage
        fun getHighestCoolingStage(configuration: HyperStatSplitCpuEconConfiguration): CpuEconRelayAssociation {
            var highestValue = 0
            highestValue = verifyCoolingState(configuration.relay1State, highestValue)
            highestValue = verifyCoolingState(configuration.relay2State, highestValue)
            highestValue = verifyCoolingState(configuration.relay3State, highestValue)
            highestValue = verifyCoolingState(configuration.relay4State, highestValue)
            highestValue = verifyCoolingState(configuration.relay5State, highestValue)
            highestValue = verifyCoolingState(configuration.relay6State, highestValue)
            highestValue = verifyCoolingState(configuration.relay7State, highestValue)
            highestValue = verifyCoolingState(configuration.relay8State, highestValue)

            return CpuEconRelayAssociation.values()[highestValue]
        }

        private fun verifyCoolingState(state: RelayState, highestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToCoolingStage(state)
                && state.association.ordinal > highestValue
            ) return state.association.ordinal
            return highestValue
        }

        fun getHighestHeatingStage(configuration: HyperStatSplitCpuEconConfiguration): CpuEconRelayAssociation {
            var highestValue = 0
            highestValue = verifyHeatingState(configuration.relay1State, highestValue)
            highestValue = verifyHeatingState(configuration.relay2State, highestValue)
            highestValue = verifyHeatingState(configuration.relay3State, highestValue)
            highestValue = verifyHeatingState(configuration.relay4State, highestValue)
            highestValue = verifyHeatingState(configuration.relay5State, highestValue)
            highestValue = verifyHeatingState(configuration.relay6State, highestValue)
            highestValue = verifyHeatingState(configuration.relay7State, highestValue)
            highestValue = verifyHeatingState(configuration.relay8State, highestValue)

            return CpuEconRelayAssociation.values()[highestValue]
        }

        private fun verifyHeatingState(state: RelayState, highestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToHeatingStage(state)
                && state.association.ordinal > highestValue
            )
                return state.association.ordinal
            return highestValue
        }

        fun getHighestFanStage(configuration: HyperStatSplitCpuEconConfiguration): CpuEconRelayAssociation {
            var highestValue = 0
            highestValue = verifyFanState(configuration.relay1State, highestValue)
            highestValue = verifyFanState(configuration.relay2State, highestValue)
            highestValue = verifyFanState(configuration.relay3State, highestValue)
            highestValue = verifyFanState(configuration.relay4State, highestValue)
            highestValue = verifyFanState(configuration.relay5State, highestValue)
            highestValue = verifyFanState(configuration.relay6State, highestValue)
            highestValue = verifyFanState(configuration.relay7State, highestValue)
            highestValue = verifyFanState(configuration.relay8State, highestValue)

            return CpuEconRelayAssociation.values()[highestValue]
        }
        private fun verifyFanState(state: RelayState, highestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToFan(state)
                && state.association.ordinal > highestValue
            )
                return state.association.ordinal
            return highestValue
        }



        fun getSelectedFanModeByLevel(fanLevel: Int, selectedFan: Int): StandaloneFanStage {
            try {
                when {
                    (selectedFan == 0) -> {
                        // No fan stages are selected so only off can present here
                        return StandaloneFanStage.OFF
                    }
                    (selectedFan == 1) -> {
                        // No fan stages are selected so only off can present here
                        return StandaloneFanStage.AUTO
                    }
                    (fanLevel == 21) -> {
                        // When fan level is 12 it means it is saying the all stages of fans are selected
                        // directly we can select from the selected list
                        return StandaloneFanStage.values()[selectedFan]
                    }
                    (fanLevel == 6) -> {
                        // Only fan low are selected
                        //  R.array.smartstat_fanmode_low
                        if (selectedFan in 1..4)
                            return StandaloneFanStage.values()[selectedFan]
                    }
                    (fanLevel == 7) -> {
                        // R.array.hyperstate_only_medium_fanmode
                        if (selectedFan in 1..4)
                            return StandaloneFanStage.values()[selectedFan + 3]
                    }
                    (fanLevel == 8) -> {
                        // R.array.hyperstate_only_high_fanmode
                        if (selectedFan in 1..4)
                            return StandaloneFanStage.values()[selectedFan + 6]
                    }
                    (fanLevel == 13) -> {
                        // When fan low and mediam are selected
                        //R.array.smartstat_2pfcu_fanmode_medium
                        if (selectedFan in 1..7)
                            return StandaloneFanStage.values()[selectedFan]
                    }
                    (fanLevel == 15) -> {
                        // Medium and high fan speeds are selected
                        // R.array.hyperstate_medium_high_fanmode
                        return StandaloneFanStage.values()[selectedFan + 3]

                    }
                    (fanLevel == 14) -> {
                        // low high selected
                        return if (selectedFan < 5)
                            StandaloneFanStage.values()[selectedFan]
                        else
                            StandaloneFanStage.values()[selectedFan + 3]
                    }
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Error getSelectedFan function ${e.localizedMessage}")
            }
            return StandaloneFanStage.OFF
        }

        fun getSelectedFanMode(fanLevel: Int, selectedFan: Int): Int {
            try {
                when {
                    (selectedFan == 0) -> {
                        // No fan stages are selected so only off can present here
                        return StandaloneFanStage.OFF.ordinal
                    }
                    (selectedFan == 1) -> {
                        // No fan stages are selected so only off can present here
                        return StandaloneFanStage.AUTO.ordinal
                    }
                    (fanLevel == 21) -> {
                        // When fan level is 12 it means it is saying the all stages of fans are selected
                        // directly we can select from the selected list
                        return StandaloneFanStage.values()[selectedFan].ordinal
                    }
                    (fanLevel == 6) -> {
                        // Only fan low are selected
                        //  R.array.smartstat_fanmode_low
                        if (selectedFan in 1..4)
                            return StandaloneFanStage.values()[selectedFan].ordinal
                    }
                    (fanLevel == 7) -> {
                        // R.array.hyperstate_only_medium_fanmode
                            return StandaloneFanStage.values()[selectedFan - 3].ordinal
                    }
                    (fanLevel == 8) -> {
                        // R.array.hyperstate_only_high_fanmode
                            return StandaloneFanStage.values()[selectedFan - 6].ordinal
                    }
                    (fanLevel == 13) -> {
                        // When fan low and mediam are selected
                        //R.array.smartstat_2pfcu_fanmode_medium
                            return StandaloneFanStage.values()[selectedFan].ordinal
                    }
                    (fanLevel == 15) -> {
                        // Medium and high fan speeds are selected
                        return StandaloneFanStage.values()[selectedFan - 3].ordinal

                    }
                    (fanLevel == 14) -> {
                        // low high selected
                        return if (selectedFan < 5)
                            StandaloneFanStage.values()[selectedFan].ordinal
                        else
                            StandaloneFanStage.values()[selectedFan - 3].ordinal
                    }
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Error getSelectedFan function ${e.localizedMessage}")
            }
            return StandaloneFanStage.OFF.ordinal
        }

        fun getPhysicalPointUnit(assoc: UniversalInAssociation): String {

            return when (assoc) {

                UniversalInAssociation.CURRENT_TX_0_10 -> "mV"
                UniversalInAssociation.CURRENT_TX_0_20 -> "mV"
                UniversalInAssociation.CURRENT_TX_0_50 -> "mV"
                UniversalInAssociation.CURRENT_TX_0_100 -> "mV"
                UniversalInAssociation.CURRENT_TX_0_150 -> "mV"
                UniversalInAssociation.SUPPLY_AIR_TEMPERATURE -> "kOhm"
                UniversalInAssociation.MIXED_AIR_TEMPERATURE -> "kOhm"
                UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE -> "kOhm"
                UniversalInAssociation.FILTER_NC -> "kOhm"
                UniversalInAssociation.FILTER_NO -> "kOhm"
                UniversalInAssociation.CONDENSATE_NC -> "kOhm"
                UniversalInAssociation.CONDENSATE_NO -> "kOhm"
                UniversalInAssociation.DUCT_PRESSURE_0_1 -> "mV"
                UniversalInAssociation.DUCT_PRESSURE_0_2 -> "mV"
                UniversalInAssociation.GENERIC_VOLTAGE -> "mV"
                UniversalInAssociation.GENERIC_RESISTANCE -> "kOhm"

            }

        }

        // Updated Sensor Manager class accordingly. 0-2" duct pressure is already included in HyperStat Sense.
        fun getSensorNameByType(sensorInputs: UniversalInAssociation): String {
            /**
             * These sensor names are constant do not change please refer Sensor Manager class for more info
             * a75f.io.logic.bo.building.sensors.SensorManager.getExternalSensorListV2
             */

            return when (sensorInputs) {
                // These positions are constant sensor position which are available in Sensor manager class
                UniversalInAssociation.CURRENT_TX_0_10 -> "8"
                UniversalInAssociation.CURRENT_TX_0_20 -> "9"
                UniversalInAssociation.CURRENT_TX_0_50 -> "10"
                UniversalInAssociation.CURRENT_TX_0_100 -> "12"
                UniversalInAssociation.CURRENT_TX_0_150 -> "13"
                UniversalInAssociation.DUCT_PRESSURE_0_1 -> "14"
                UniversalInAssociation.DUCT_PRESSURE_0_2 -> "1"
                UniversalInAssociation.GENERIC_VOLTAGE -> "0"
                else -> "8"
            }
        }

        private fun updateSelectedFanLevels(
            association: Int, currentFoundDetails: Triple<Boolean, Boolean, Boolean>,
            low: Int, medium: Int, high: Int
        ): Triple<Boolean, Boolean, Boolean> {
            var currentStatus = currentFoundDetails
            if (!currentStatus.first) {
                currentStatus = currentStatus.copy( first = association == low)
            }
            if (!currentStatus.second) {
                currentStatus = currentStatus.copy( second = association == medium)
            }
            if (!currentStatus.third) {
                currentStatus = currentStatus.copy( third = association == high)
            }
            return currentStatus
        }

        fun getTempPort(state: SensorBusTempState): Port {
            when (state.association) {
                CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY -> {
                    return Port.SENSOR_SAT
                }
                CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY -> {
                    return Port.SENSOR_OAT
                }
                CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY -> {
                    return Port.SENSOR_MAT
                }
                // This case should never occur
                else -> { return Port.SENSOR_SAT }
            }
        }

        fun getHumidityPort(state: SensorBusTempState): Port {
            when (state.association) {
                CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY -> {
                    return Port.SENSOR_SAH
                }
                CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY -> {
                    return Port.SENSOR_OAH
                }
                CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY -> {
                    return Port.SENSOR_MAH
                }
                // This case should never occur
                else -> { return Port.SENSOR_SAH }
            }
        }

        fun getPressPort(state: SensorBusPressState): Port {
            when (state.association) {
                CpuEconSensorBusPressAssociation.DUCT_PRESSURE -> {
                    return Port.SENSOR_PRESSURE
                }
                // This case should never occur
                else -> { return Port.SENSOR_PRESSURE }
            }

        }

        fun isStagedFanEnabled(
            hyperStatConfig: HyperStatSplitCpuEconConfiguration,
            fanStage: CpuEconRelayAssociation
        ): Boolean {
            return  hyperStatConfig.relay1State.enabled && hyperStatConfig.relay1State.association == fanStage ||
                    hyperStatConfig.relay2State.enabled && hyperStatConfig.relay2State.association == fanStage ||
                    hyperStatConfig.relay3State.enabled && hyperStatConfig.relay3State.association == fanStage ||
                    hyperStatConfig.relay4State.enabled && hyperStatConfig.relay4State.association == fanStage ||
                    hyperStatConfig.relay5State.enabled && hyperStatConfig.relay5State.association == fanStage ||
                    hyperStatConfig.relay6State.enabled && hyperStatConfig.relay6State.association == fanStage ||
                    hyperStatConfig.relay7State.enabled && hyperStatConfig.relay7State.association == fanStage ||
                    hyperStatConfig.relay8State.enabled && hyperStatConfig.relay8State.association == fanStage
        }

        fun isAnyAnalogOutMappedToStagedFan(
            hyperStatConfig: HyperStatSplitCpuEconConfiguration,
        ): Boolean {
            return when {
                (hyperStatConfig.analogOut1State.enabled &&
                        isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut1State)) -> true
                (hyperStatConfig.analogOut2State.enabled &&
                        isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut2State)) -> true
                (hyperStatConfig.analogOut3State.enabled &&
                        isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut3State)) -> true
                else -> false
            }
        }

    }

}