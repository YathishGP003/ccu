package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.*
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.*
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.*
import a75f.io.logic.bo.building.sensors.SensorType
import android.util.Log

/**
 * Created by Manjunath K on 30-07-2021.
 */

class HyperStatAssociationUtil {
    companion object {

        //Function which checks the Relay is Associated  to Fan or Not
        fun isRelayAssociatedToFan(relayState: RelayState): Boolean {
            return (relayState.association == CpuRelayAssociation.FAN_LOW_SPEED
                    || relayState.association == CpuRelayAssociation.FAN_MEDIUM_SPEED
                    || relayState.association == CpuRelayAssociation.FAN_HIGH_SPEED)
        }

        //Function which checks the Relay is Associated  to Cooling Stage
        fun isRelayAssociatedToCoolingStage(relayState: RelayState): Boolean {
            return (relayState.association == CpuRelayAssociation.COOLING_STAGE_1
                    || relayState.association == CpuRelayAssociation.COOLING_STAGE_2
                    || relayState.association == CpuRelayAssociation.COOLING_STAGE_3)

        }

        //Function which checks the Relay is Associated  to Heating Stage
        fun isRelayAssociatedToHeatingStage(relayState: RelayState): Boolean {
            return (relayState.association == CpuRelayAssociation.HEATING_STAGE_1
                    || relayState.association == CpuRelayAssociation.HEATING_STAGE_2
                    || relayState.association == CpuRelayAssociation.HEATING_STAGE_3)

        }

        // Function which returns the Relay Mapped state
        fun getRelayAssociatedStage(state: Int): CpuRelayAssociation {
            return when (state) {
                // Order is important here
                0 -> CpuRelayAssociation.COOLING_STAGE_1
                1 -> CpuRelayAssociation.COOLING_STAGE_2
                2 -> CpuRelayAssociation.COOLING_STAGE_3
                3 -> CpuRelayAssociation.HEATING_STAGE_1
                4 -> CpuRelayAssociation.HEATING_STAGE_2
                5 -> CpuRelayAssociation.HEATING_STAGE_3
                6 -> CpuRelayAssociation.FAN_LOW_SPEED
                7 -> CpuRelayAssociation.FAN_MEDIUM_SPEED
                8 -> CpuRelayAssociation.FAN_HIGH_SPEED
                9 -> CpuRelayAssociation.FAN_ENABLED
                10 -> CpuRelayAssociation.OCCUPIED_ENABLED
                11 -> CpuRelayAssociation.HUMIDIFIER
                12 -> CpuRelayAssociation.DEHUMIDIFIER
                // assuming it never going to call
                else -> CpuRelayAssociation.COOLING_STAGE_1
            }

        }

        // Function which returns the Relay Mapped state
        fun getAnalogOutAssociatedStage(state: Int): CpuAnalogOutAssociation {
            return when (state) {
                // Order is important here
                0 -> CpuAnalogOutAssociation.COOLING
                1 -> CpuAnalogOutAssociation.MODULATING_FAN_SPEED
                2 -> CpuAnalogOutAssociation.HEATING
                3 -> CpuAnalogOutAssociation.DCV_DAMPER
                4 -> CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED
                // assuming it never going to call
                else -> CpuAnalogOutAssociation.COOLING
            }
        }

        // Function which returns the Relay Mapped state
        fun getAnalogInStage(state: Int): AnalogInAssociation {
            return when (state) {
                // Order is important here
                0 -> AnalogInAssociation.CURRENT_TX_0_10
                1 -> AnalogInAssociation.CURRENT_TX_0_20
                2 -> AnalogInAssociation.CURRENT_TX_0_50
                3 -> AnalogInAssociation.KEY_CARD_SENSOR
                4 -> AnalogInAssociation.DOOR_WINDOW_SENSOR

                // assuming it never going to call
                else -> AnalogInAssociation.CURRENT_TX_0_10
            }

        }

        //Function which checks the Relay is Associated  to Fan Enabled
        fun isRelayAssociatedToFanEnabled(relayState: RelayState): Boolean {
            return (relayState.association == CpuRelayAssociation.FAN_ENABLED)
        }

        //Function which checks the Relay is Associated  to OCCUPIED ENABLED
        fun isRelayAssociatedToOccupiedEnabled(relayState: RelayState): Boolean {
            return (relayState.association == CpuRelayAssociation.OCCUPIED_ENABLED)
        }

        //Function which checks the Relay is Associated  to HUMIDIFIER
        fun isRelayAssociatedToHumidifier(relayState: RelayState): Boolean {
            return (relayState.association == CpuRelayAssociation.HUMIDIFIER)
        }

        //Function which checks the Relay is Associated  to DEHUMIDIFIER
        fun isRelayAssociatedToDeHumidifier(relayState: RelayState): Boolean {
            return (relayState.association == CpuRelayAssociation.DEHUMIDIFIER)
        }

        //Function which checks the Analog out is Associated  to Cooling
        fun isAnalogOutAssociatedToCooling(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuAnalogOutAssociation.COOLING)
        }

        //Function which checks the Analog out is Associated  to FAN_SPEED
        fun isAnalogOutAssociatedToFanSpeed(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuAnalogOutAssociation.MODULATING_FAN_SPEED)
        }

        //Function which checks the Analog out is Associated  to HEATING
        fun isAnalogOutAssociatedToHeating(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuAnalogOutAssociation.HEATING)
        }

        //Function which checks the Analog out is Associated  to DCV Damper
        fun isAnalogOutAssociatedToDcvDamper(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuAnalogOutAssociation.DCV_DAMPER)
        }

        //Function which checks the Analog out is Associated  to STAGED_FAN_SPEED
        fun isAnalogOutAssociatedToStagedFanSpeed(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToCurrentTX10(analogIn: AnalogInState): Boolean {
            return (analogIn.association == AnalogInAssociation.CURRENT_TX_0_10)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToCurrentTX20(analogIn: AnalogInState): Boolean {
            return (analogIn.association == AnalogInAssociation.CURRENT_TX_0_20)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToCurrentTX50(analogIn: AnalogInState): Boolean {
            return (analogIn.association == AnalogInAssociation.CURRENT_TX_0_50)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToDoorWindowSensor(analogIn: AnalogInState): Boolean {
            return (analogIn.association == AnalogInAssociation.DOOR_WINDOW_SENSOR)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToKeyCardSensor(analogIn: AnalogInState): Boolean {
            return (analogIn.association == AnalogInAssociation.KEY_CARD_SENSOR)
        }

        fun isAnyRelayAssociatedToCoolingStage1(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.COOLING_STAGE_1)
        }
        fun isAnyRelayAssociatedToCoolingStage2(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.COOLING_STAGE_2)
        }
        fun isAnyRelayAssociatedToCoolingStage3(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.COOLING_STAGE_3)
        }
        fun isAnyRelayAssociatedToHeatingStage1(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.HEATING_STAGE_1)
        }
        fun isAnyRelayAssociatedToHeatingStage2(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.HEATING_STAGE_2)
        }
        fun isAnyRelayAssociatedToHeatingStage3(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.HEATING_STAGE_3)
        }
        fun isAnyRelayAssociatedToFanLow(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.FAN_LOW_SPEED)
        }
        fun isAnyRelayAssociatedToFanMedium(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.FAN_MEDIUM_SPEED)
        }
        fun isAnyRelayAssociatedToFanHigh(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.FAN_HIGH_SPEED)
        }
        fun isAnyRelayAssociatedToFanEnabled(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.FAN_ENABLED)
        }
        fun isAnyRelayAssociatedToOccupiedEnabled(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.OCCUPIED_ENABLED)
        }
        fun isAnyRelayEnabledAssociatedToHumidifier(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.HUMIDIFIER)
        }
        fun isAnyRelayEnabledAssociatedToDeHumidifier(config: HyperStatCpuConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayAssociation.DEHUMIDIFIER)
        }

        private fun isAnyRelayMapped(config: HyperStatCpuConfiguration, association: CpuRelayAssociation): Boolean{
            return when {
                (config.relay1State.enabled && config.relay1State.association == association) -> true
                (config.relay2State.enabled && config.relay2State.association == association) -> true
                (config.relay3State.enabled && config.relay3State.association == association) -> true
                (config.relay4State.enabled && config.relay4State.association == association) -> true
                (config.relay5State.enabled && config.relay5State.association == association) -> true
                (config.relay6State.enabled && config.relay6State.association == association) -> true
                else -> false
            }
        }
        fun isAnyAnalogAssociatedToCooling(config: HyperStatCpuConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuAnalogOutAssociation.COOLING)
        }
        fun isAnyAnalogAssociatedToHeating(config: HyperStatCpuConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuAnalogOutAssociation.HEATING)
        }
        fun isAnyAnalogAssociatedToFan(config: HyperStatCpuConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuAnalogOutAssociation.MODULATING_FAN_SPEED)
        }
        fun isAnyAnalogAssociatedToDCV(config: HyperStatCpuConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuAnalogOutAssociation.DCV_DAMPER)
        }
        fun isAnyAnalogAssociatedToStaged(config: HyperStatCpuConfiguration): Boolean {
            return isAnalogOutMapped(config,CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED)
        }
        private fun isAnalogOutMapped(config: HyperStatCpuConfiguration, association: CpuAnalogOutAssociation): Boolean{
            return when {
                (config.analogOut1State.enabled && config.analogOut1State.association == association) -> true
                (config.analogOut2State.enabled && config.analogOut2State.association == association) -> true
                (config.analogOut3State.enabled && config.analogOut3State.association == association) -> true
                else -> false
            }
        }

        fun isAnalogAssociatedToStaged(analogOutState: AnalogOutState) : Boolean {
            return (analogOutState.enabled && analogOutState.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED)
        }

        private fun isAnalogInMapped(
            ai1: AnalogInState, ai2: AnalogInState, association: AnalogInAssociation
        ): Boolean{
            return when {
                (ai1.enabled && ai1.association == association) -> true
                (ai2.enabled && ai2.association == association) -> true
                else -> false
            }
        }
        fun isAnyAnalogInMappedCT10(analogIn1State: AnalogInState,analogIn2State: AnalogInState): Boolean {
            return isAnalogInMapped(analogIn1State,analogIn2State,AnalogInAssociation.CURRENT_TX_0_10)
        }
        fun isAnyAnalogInMappedCT20(analogIn1State: AnalogInState,analogIn2State: AnalogInState): Boolean {
            return isAnalogInMapped(analogIn1State,analogIn2State,AnalogInAssociation.CURRENT_TX_0_20)
        }
        fun isAnyAnalogInMappedCT50(analogIn1State: AnalogInState,analogIn2State: AnalogInState): Boolean {
            return isAnalogInMapped(analogIn1State,analogIn2State,AnalogInAssociation.CURRENT_TX_0_50)
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
                (analogOut1.voltageAtRecirculate != analogOut2.voltageAtRecirculate) -> return false
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
                    (analogOut1.voltageAtRecirculate != analogOut2.voltageAtRecirculate) -> return AnalogOutChanges.RECIRCULATE
                    (isAnalogOutAssociatedToFanSpeed(analogOut1) || isAnalogOutAssociatedToStagedFanSpeed(analogOut1)) -> {
                        when {
                            (analogOut1.perAtFanLow != analogOut2.perAtFanLow) -> return AnalogOutChanges.LOW
                            (analogOut1.perAtFanMedium != analogOut2.perAtFanMedium) -> return AnalogOutChanges.MED
                            (analogOut1.perAtFanHigh != analogOut2.perAtFanHigh) -> return AnalogOutChanges.HIGH
                        }
                    }
                }
                return AnalogOutChanges.NOCHANGE
        }

        // checks two Relay configurations and return based on the match
        fun isBothAnalogInHasSameConfigs(analogIn1: AnalogInState, analogIn2: AnalogInState): Boolean {
            when {
                (analogIn1.enabled != analogIn2.enabled) -> return false
                (analogIn1.association != analogIn2.association) -> return false
            }
            return true
        }

        // Function which checks that any of the relay is associated to Humidifier
        fun isAnyRelayAssociatedToHumidifier(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (isRelayAssociatedToHumidifier(configuration.relay1State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay2State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay3State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay4State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay5State)) -> true
                (isRelayAssociatedToHumidifier(configuration.relay6State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay is associated to DeHumidifier
        fun isAnyRelayAssociatedToDeHumidifier(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (isRelayAssociatedToDeHumidifier(configuration.relay1State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay2State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay3State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay4State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay5State)) -> true
                (isRelayAssociatedToDeHumidifier(configuration.relay6State)) -> true
                else -> false
            }
        }

        fun getSelectedFanLevel(configuration: HyperStatCpuConfiguration): Int {

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
            association: CpuRelayAssociation, currentFoundDetails: Triple<Boolean, Boolean, Boolean>
        ): Triple<Boolean, Boolean, Boolean> {
            var currentStatus = currentFoundDetails
            if (!currentStatus.first) {
                currentStatus = currentStatus.copy(
                    first = association.ordinal == CpuRelayAssociation.FAN_LOW_SPEED.ordinal
                )
            }
            if (!currentStatus.second) {
                currentStatus = currentStatus.copy(
                    second = association.ordinal == CpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal
                )
            }
            if (!currentStatus.third) {
                currentStatus = currentStatus.copy(
                    third = association.ordinal == CpuRelayAssociation.FAN_HIGH_SPEED.ordinal
                )
            }
            return currentStatus
        }

        // Function which checks that any of the relay Enabled and is associated to Heating
        fun isAnyRelayEnabledAssociatedToFan(configuration: HyperStatCpuConfiguration): Boolean {
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
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to Heating
        fun isAnyRelayEnabledAssociatedToHeating(configuration: HyperStatCpuConfiguration): Boolean {
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
                else -> false
            }
        }

        // function to check the input relay is enabled and associated to fan speed
        private fun isRelayEnabledAssociatedToFan(relayState: RelayState): Boolean {
         return (relayState.enabled && isRelayAssociatedToFan(relayState))
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToCooling(configuration: HyperStatCpuConfiguration): Boolean {
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
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Cooling
        fun isAnyAnalogOutEnabledAssociatedToCooling(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (configuration.analogOut1State.enabled &&
                        isAnalogOutAssociatedToCooling(configuration.analogOut1State)) -> true
                (configuration.analogOut2State.enabled &&
                        isAnalogOutAssociatedToCooling(configuration.analogOut2State)) -> true
                (configuration.analogOut3State.enabled &&
                        isAnalogOutAssociatedToCooling(configuration.analogOut3State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Heating
        fun isAnyAnalogOutEnabledAssociatedToHeating(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (configuration.analogOut1State.enabled &&
                        isAnalogOutAssociatedToHeating(configuration.analogOut1State)) -> true
                (configuration.analogOut2State.enabled &&
                        isAnalogOutAssociatedToHeating(configuration.analogOut2State)) -> true
                (configuration.analogOut3State.enabled &&
                        isAnalogOutAssociatedToHeating(configuration.analogOut3State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Fan speed
        fun isAnyAnalogOutEnabledAssociatedToFanSpeed(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (configuration.analogOut1State.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut1State)) -> true
                (configuration.analogOut2State.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut2State)) -> true
                (configuration.analogOut3State.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut3State)) -> true
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
        fun getHighestCoolingStage(configuration: HyperStatCpuConfiguration): CpuRelayAssociation {
            var highestValue = 0
            highestValue = verifyCoolingState(configuration.relay1State, highestValue)
            highestValue = verifyCoolingState(configuration.relay2State, highestValue)
            highestValue = verifyCoolingState(configuration.relay3State, highestValue)
            highestValue = verifyCoolingState(configuration.relay4State, highestValue)
            highestValue = verifyCoolingState(configuration.relay5State, highestValue)
            highestValue = verifyCoolingState(configuration.relay6State, highestValue)

            return CpuRelayAssociation.values()[highestValue]
        }

        private fun verifyCoolingState(state: RelayState, highestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToCoolingStage(state)
                && state.association.ordinal > highestValue
            )
                return state.association.ordinal
            return highestValue
        }

        fun getHighestHeatingStage(configuration: HyperStatCpuConfiguration): CpuRelayAssociation {
            var highestValue = 0
            highestValue = verifyHeatingState(configuration.relay1State, highestValue)
            highestValue = verifyHeatingState(configuration.relay2State, highestValue)
            highestValue = verifyHeatingState(configuration.relay3State, highestValue)
            highestValue = verifyHeatingState(configuration.relay4State, highestValue)
            highestValue = verifyHeatingState(configuration.relay5State, highestValue)
            highestValue = verifyHeatingState(configuration.relay6State, highestValue)

            return CpuRelayAssociation.values()[highestValue]
        }

        private fun verifyHeatingState(state: RelayState, highestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToHeatingStage(state)
                && state.association.ordinal > highestValue
            )
                return state.association.ordinal
            return highestValue
        }

        /**
         * Determines the lowest fan stage based on the relay states in the given CPU configuration.
         *
         * @param configuration the CPU configuration to analyze.
         * @return the lowest fan stage, represented as a [CpuRelayAssociation] value.
         */
        fun getLowestFanStage(configuration: HyperStatCpuConfiguration): CpuRelayAssociation {
            var lowestValue = 0xFF
            lowestValue = verifyFanStateLowValue(configuration.relay1State, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay2State, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay3State, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay4State, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay5State, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay6State, lowestValue)

            // If no fan is enabled, return highest enum value (DEHUMIDIFIER)
            return CpuRelayAssociation.values().getOrNull(lowestValue) ?: CpuRelayAssociation.DEHUMIDIFIER
        }

        /**
         * Verifies the lowest fan state value based on the given relay state and the current lowest value.
         *
         * @param state the relay state to verify.
         * @param lowestValue the current lowest value.
         * @return the updated lowest value, considering the relay state.
         */
        private fun verifyFanStateLowValue(state: RelayState, lowestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToFan(state)
                && state.association.ordinal < lowestValue
            )
                return state.association.ordinal
            return lowestValue
        }

        fun getHighestFanStage(configuration: HyperStatCpuConfiguration): CpuRelayAssociation {
            var highestValue = 0
            highestValue = verifyFanState(configuration.relay1State, highestValue)
            highestValue = verifyFanState(configuration.relay2State, highestValue)
            highestValue = verifyFanState(configuration.relay3State, highestValue)
            highestValue = verifyFanState(configuration.relay4State, highestValue)
            highestValue = verifyFanState(configuration.relay5State, highestValue)
            highestValue = verifyFanState(configuration.relay6State, highestValue)

            return CpuRelayAssociation.values()[highestValue]
        }
        private fun verifyFanState(state: RelayState, highestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToFan(state)
                && state.association.ordinal > highestValue
            )
                return state.association.ordinal
            return highestValue
        }


        // Function returns highest selected cooling stage
        fun getHighestCompressorStage(configuration: HyperStatHpuConfiguration): HpuRelayAssociation {
            var highestValue = 0
            highestValue = verifyCompressorState(configuration.relay1State, highestValue)
            highestValue = verifyCompressorState(configuration.relay2State, highestValue)
            highestValue = verifyCompressorState(configuration.relay3State, highestValue)
            highestValue = verifyCompressorState(configuration.relay4State, highestValue)
            highestValue = verifyCompressorState(configuration.relay5State, highestValue)
            highestValue = verifyCompressorState(configuration.relay6State, highestValue)

            return HpuRelayAssociation.values()[highestValue]
        }

        private fun verifyCompressorState(state: HpuRelayState, highestValue: Int): Int {
            if (state.enabled && isRelayAssociatedToCompressorStage(state)
                && state.association.ordinal > highestValue
            )
                return state.association.ordinal
            return highestValue
        }

        /**
         * Determines the lowest fan stage based on the relay states in the given CPU configuration.
         *
         * @param configuration the CPU configuration to analyze.
         * @return the lowest fan stage, represented as a [CpuRelayAssociation] value.
         */
        fun getHpuLowestFanStage(configuration: HyperStatHpuConfiguration): HpuRelayAssociation {
            var lowestValue = 0xFF
            lowestValue = verifyHpuFanStateLowValue(configuration.relay1State, lowestValue)
            lowestValue = verifyHpuFanStateLowValue(configuration.relay2State, lowestValue)
            lowestValue = verifyHpuFanStateLowValue(configuration.relay3State, lowestValue)
            lowestValue = verifyHpuFanStateLowValue(configuration.relay4State, lowestValue)
            lowestValue = verifyHpuFanStateLowValue(configuration.relay5State, lowestValue)
            lowestValue = verifyHpuFanStateLowValue(configuration.relay6State, lowestValue)

            // If no fan is enabled, return highest enum value (DEHUMIDIFIER)
            return HpuRelayAssociation.values().getOrNull(lowestValue) ?: HpuRelayAssociation.DEHUMIDIFIER
        }

        /**
         * Verifies the lowest fan state value based on the given relay state and the current lowest value.
         *
         * @param state the relay state to verify.
         * @param lowestValue the current lowest value.
         * @return the updated lowest value, considering the relay state.
         */
        private fun verifyHpuFanStateLowValue(state: HpuRelayState, lowestValue: Int): Int {
            if (state.enabled && isHpuRelayAssociatedToFan(state)
                && state.association.ordinal < lowestValue
            )
                return state.association.ordinal
            return lowestValue
        }

        fun getHpuHighestFanStage(configuration: HyperStatHpuConfiguration): HpuRelayAssociation {
            var highestValue = 0
            highestValue = verifyHpuFanState(configuration.relay1State, highestValue)
            highestValue = verifyHpuFanState(configuration.relay2State, highestValue)
            highestValue = verifyHpuFanState(configuration.relay3State, highestValue)
            highestValue = verifyHpuFanState(configuration.relay4State, highestValue)
            highestValue = verifyHpuFanState(configuration.relay5State, highestValue)
            highestValue = verifyHpuFanState(configuration.relay6State, highestValue)

            return HpuRelayAssociation.values()[highestValue]
        }
        private fun verifyHpuFanState(state: HpuRelayState, highestValue: Int): Int {
            if (state.enabled && isHpuRelayAssociatedToFan(state)
                && state.association.ordinal > highestValue
            )
                return state.association.ordinal
            return highestValue
        }

        /**
         * Determines the lowest fan stage based on the relay states in the given CPU configuration.
         *
         * @param configuration the CPU configuration to analyze.
         * @return the lowest fan stage, represented as a [CpuRelayAssociation] value.
         */
        fun getPipe2LowestFanStage(configuration: HyperStatPipe2Configuration): Pipe2RelayAssociation {
            var lowestValue = 0xFF
            lowestValue = verifyPipe2FanStateLowValue(configuration.relay1State, lowestValue)
            lowestValue = verifyPipe2FanStateLowValue(configuration.relay2State, lowestValue)
            lowestValue = verifyPipe2FanStateLowValue(configuration.relay3State, lowestValue)
            lowestValue = verifyPipe2FanStateLowValue(configuration.relay4State, lowestValue)
            lowestValue = verifyPipe2FanStateLowValue(configuration.relay5State, lowestValue)
            lowestValue = verifyPipe2FanStateLowValue(configuration.relay6State, lowestValue)

            // If no fan is enabled, return highest enum value (DEHUMIDIFIER)
            return Pipe2RelayAssociation.values().getOrNull(lowestValue) ?: Pipe2RelayAssociation.DEHUMIDIFIER
        }

        /**
         * Verifies the lowest fan state value based on the given relay state and the current lowest value.
         *
         * @param state the relay state to verify.
         * @param lowestValue the current lowest value.
         * @return the updated lowest value, considering the relay state.
         */
        private fun verifyPipe2FanStateLowValue(state: Pipe2RelayState, lowestValue: Int): Int {
            if (state.enabled && isPipe2RelayAssociatedToFan(state)
                && state.association.ordinal < lowestValue
            )
                return state.association.ordinal
            return lowestValue
        }

        fun getPipe2HighestFanStage(configuration: HyperStatPipe2Configuration): Pipe2RelayAssociation {
            var highestValue = 0
            highestValue = verifyPipe2FanState(configuration.relay1State, highestValue)
            highestValue = verifyPipe2FanState(configuration.relay2State, highestValue)
            highestValue = verifyPipe2FanState(configuration.relay3State, highestValue)
            highestValue = verifyPipe2FanState(configuration.relay4State, highestValue)
            highestValue = verifyPipe2FanState(configuration.relay5State, highestValue)
            highestValue = verifyPipe2FanState(configuration.relay6State, highestValue)

            return Pipe2RelayAssociation.values()[highestValue]
        }
        private fun verifyPipe2FanState(state: Pipe2RelayState, highestValue: Int): Int {
            if (state.enabled && isPipe2RelayAssociatedToFan(state)
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
                Log.i(L.TAG_CCU_HSCPU, "Error getSelectedFan function ${e.localizedMessage}")
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
                Log.i(L.TAG_CCU_HSCPU, "Error getSelectedFan function ${e.localizedMessage}")
            }
            return StandaloneFanStage.OFF.ordinal
        }

        fun getSensorNameByType(sensorInputs: AnalogInAssociation): String {
            /**
             * These sensor names are constant do not change please refer Sensor Manager class for more info
             * a75f.io.logic.bo.building.sensors.SensorManager.getExternalSensorListV2
             */

            return when (sensorInputs) {
                // These positions are constant sensor position which are available in Sensor manager class
                AnalogInAssociation.CURRENT_TX_0_10 -> "8"
                AnalogInAssociation.CURRENT_TX_0_20 -> "9"
                AnalogInAssociation.CURRENT_TX_0_50 -> "10"
                AnalogInAssociation.KEY_CARD_SENSOR -> SensorType.KEY_CARD_SENSOR.ordinal.toString()
                AnalogInAssociation.DOOR_WINDOW_SENSOR -> SensorType.DOOR_WINDOW_SENSOR.ordinal.toString()
            }
        }

        // Function which returns the Relay Mapped state
        fun getPipe2RelayAssociatedStage(state: Int): Pipe2RelayAssociation {
            return when (state) {
                // Order is important here
                0 -> Pipe2RelayAssociation.FAN_LOW_SPEED
                1 -> Pipe2RelayAssociation.FAN_MEDIUM_SPEED
                2 -> Pipe2RelayAssociation.FAN_HIGH_SPEED
                3 -> Pipe2RelayAssociation.AUX_HEATING_STAGE1
                4 -> Pipe2RelayAssociation.AUX_HEATING_STAGE2
                5 -> Pipe2RelayAssociation.WATER_VALVE
                6 -> Pipe2RelayAssociation.FAN_ENABLED
                7 -> Pipe2RelayAssociation.OCCUPIED_ENABLED
                8 -> Pipe2RelayAssociation.HUMIDIFIER
                9 -> Pipe2RelayAssociation.DEHUMIDIFIER
                else -> Pipe2RelayAssociation.FAN_LOW_SPEED
            }
        }

        // Function which returns the Relay Mapped state
        fun getPipe2AnalogOutAssociatedStage(state: Int): Pipe2AnalogOutAssociation {
            return when (state) {
                // Order is important here
                0 -> Pipe2AnalogOutAssociation.WATER_VALVE
                1 -> Pipe2AnalogOutAssociation.FAN_SPEED
                2 -> Pipe2AnalogOutAssociation.DCV_DAMPER
                // assuming it never going to call
                else -> Pipe2AnalogOutAssociation.WATER_VALVE
            }
        }

        // Pipe to association
        fun isRelayFanLowSpeed(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.FAN_LOW_SPEED)
        }
        fun isRelayFanMediumSpeed(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.FAN_MEDIUM_SPEED)
        }
        fun isRelayFanHighSpeed(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.FAN_HIGH_SPEED)
        }
        fun isRelayAuxHeatingStage1(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.AUX_HEATING_STAGE1)
        }
        fun isRelayAuxHeatingStage2(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.AUX_HEATING_STAGE2)
        }
        fun isRelayWaterValveStage(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.WATER_VALVE)
        }
        fun isRelayFanEnabledStage(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.FAN_ENABLED)
        }
        fun isRelayOccupiedEnabledStage(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.OCCUPIED_ENABLED)
        }
        fun isRelayHumidifierEnabledStage(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.HUMIDIFIER)
        }
        fun isRelayDeHumidifierEnabledStage(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.DEHUMIDIFIER)
        }
        fun isAnalogOutMappedToFanSpeed(analogOut: Pipe2AnalogOutState): Boolean{
            return analogOut.association == Pipe2AnalogOutAssociation.FAN_SPEED
        }
        fun isAnalogOutMappedToDcvDamper(analogOut: Pipe2AnalogOutState): Boolean{
            return analogOut.association == Pipe2AnalogOutAssociation.DCV_DAMPER
        }
        fun isAnalogOutMappedToWaterValve(analogOut: Pipe2AnalogOutState): Boolean{
            return analogOut.association == Pipe2AnalogOutAssociation.WATER_VALVE
        }

        // checks two Relay configurations and return based on the match
        fun isPipe2BothRelayHasSameConfigs(relayState1: Pipe2RelayState, relayState2: Pipe2RelayState): Boolean {
            when {
                (relayState1.enabled != relayState2.enabled) -> return false
                (relayState1.association != relayState2.association) -> return false
            }
            return true
        }

        fun isHpuBothRelayHasSameConfigs(relayState1: HpuRelayState, relayState2: HpuRelayState): Boolean {
            when {
                (relayState1.enabled != relayState2.enabled) -> return false
                (relayState1.association != relayState2.association) -> return false
            }
            return true
        }

        // checks two Analog out configurations and return based on the match
        fun isPipe2BothAnalogOutHasSameConfigs(analogOut1: Pipe2AnalogOutState, analogOut2: Pipe2AnalogOutState): Boolean {
            when {
                (analogOut1.enabled != analogOut2.enabled) -> return false
                (analogOut1.association != analogOut2.association) -> return false
                (analogOut1.voltageAtMin != analogOut2.voltageAtMin) -> return false
                (analogOut1.voltageAtMax != analogOut2.voltageAtMax) -> return false
                (analogOut1.association == Pipe2AnalogOutAssociation.FAN_SPEED) -> {
                    when {
                        (analogOut1.perAtFanLow != analogOut2.perAtFanLow) -> return false
                        (analogOut1.perAtFanMedium != analogOut2.perAtFanMedium) -> return false
                        (analogOut1.perAtFanHigh != analogOut2.perAtFanHigh) -> return false
                    }
                }
            }
            return true
        }
        fun isHpuBothAnalogOutHasSameConfigs(analogOut1: HpuAnalogOutState, analogOut2: HpuAnalogOutState): Boolean {
            when {
                (analogOut1.enabled != analogOut2.enabled) -> return false
                (analogOut1.association != analogOut2.association) -> return false
                (analogOut1.voltageAtMin != analogOut2.voltageAtMin) -> return false
                (analogOut1.voltageAtMax != analogOut2.voltageAtMax) -> return false
                (analogOut1.association == HpuAnalogOutAssociation.FAN_SPEED) -> {
                    when {
                        (analogOut1.perAtFanLow != analogOut2.perAtFanLow) -> return false
                        (analogOut1.perAtFanMedium != analogOut2.perAtFanMedium) -> return false
                        (analogOut1.perAtFanHigh != analogOut2.perAtFanHigh) -> return false
                    }
                }
            }
            return true
        }

        fun isDeletionRequired(config: HyperStatPipe2Configuration, relayState: Pipe2RelayState):  Boolean{
            var count = 0
            if(config.relay1State.association == relayState.association) count++
            if(config.relay2State.association == relayState.association) count++
            if(config.relay3State.association == relayState.association) count++
            if(config.relay4State.association == relayState.association) count++
            if(config.relay5State.association == relayState.association) count++
            if(config.relay5State.association == relayState.association) count++
            return count < 2
        }

        fun isDeletionRequiredForHpu(config: HyperStatHpuConfiguration, relayState: HpuRelayState):  Boolean{
            var count = 0
            if(config.relay1State.association == relayState.association) count++
            if(config.relay2State.association == relayState.association) count++
            if(config.relay3State.association == relayState.association) count++
            if(config.relay4State.association == relayState.association) count++
            if(config.relay5State.association == relayState.association) count++
            if(config.relay5State.association == relayState.association) count++
            return count < 2
        }

        // Function finds the analog out changes
        fun findPipe2ChangeInAnalogOutConfig(analogOut1: Pipe2AnalogOutState, analogOut2: Pipe2AnalogOutState): AnalogOutChanges{
            when {
                (analogOut1.enabled != analogOut2.enabled) -> return AnalogOutChanges.ENABLED
                (analogOut1.association != analogOut2.association) -> return AnalogOutChanges.MAPPING
                (analogOut1.voltageAtMin != analogOut2.voltageAtMin) -> return AnalogOutChanges.MIN
                (analogOut1.voltageAtMax != analogOut2.voltageAtMax) -> return AnalogOutChanges.MAX
                (analogOut1.association == Pipe2AnalogOutAssociation.FAN_SPEED) -> {
                    when {
                        (analogOut1.perAtFanLow != analogOut2.perAtFanLow) -> return AnalogOutChanges.LOW
                        (analogOut1.perAtFanMedium != analogOut2.perAtFanMedium) -> return AnalogOutChanges.MED
                        (analogOut1.perAtFanHigh != analogOut2.perAtFanHigh) -> return AnalogOutChanges.HIGH
                    }
                }
            }
            return AnalogOutChanges.NOCHANGE
        }

        // Function finds the analog out changes
        fun findHpuChangeInAnalogOutConfig(analogOut1: HpuAnalogOutState, analogOut2: HpuAnalogOutState): AnalogOutChanges{
            when {
                (analogOut1.enabled != analogOut2.enabled) -> return AnalogOutChanges.ENABLED
                (analogOut1.association != analogOut2.association) -> return AnalogOutChanges.MAPPING
                (analogOut1.voltageAtMin != analogOut2.voltageAtMin) -> return AnalogOutChanges.MIN
                (analogOut1.voltageAtMax != analogOut2.voltageAtMax) -> return AnalogOutChanges.MAX
                (analogOut1.association == HpuAnalogOutAssociation.FAN_SPEED) -> {
                    when {
                        (analogOut1.perAtFanLow != analogOut2.perAtFanLow) -> return AnalogOutChanges.LOW
                        (analogOut1.perAtFanMedium != analogOut2.perAtFanMedium) -> return AnalogOutChanges.MED
                        (analogOut1.perAtFanHigh != analogOut2.perAtFanHigh) -> return AnalogOutChanges.HIGH
                    }
                }
            }
            return AnalogOutChanges.NOCHANGE
        }

        fun isAnyRelayAssociatedToAuxHeatingStage1(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.AUX_HEATING_STAGE1)
        }
        fun isAnyRelayAssociatedToAuxHeatingStage2(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.AUX_HEATING_STAGE2)
        }
        fun isAnyRelayAssociatedToWaterValve(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.WATER_VALVE)
        }
        fun isAnyPipe2RelayAssociatedToFanLow(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.FAN_LOW_SPEED)
        }
        fun isAnyPipe2RelayAssociatedToFanMedium(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.FAN_MEDIUM_SPEED)
        }
        fun isAnyPipe2RelayAssociatedToFanHigh(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.FAN_HIGH_SPEED)
        }
        fun isAnyPipe2RelayAssociatedToFanEnabled(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.FAN_ENABLED)
        }
        fun isAnyPipe2RelayAssociatedToOccupiedEnabled(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.OCCUPIED_ENABLED)
        }
        fun isAnyPipe2RelayEnabledAssociatedToHumidifier(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.HUMIDIFIER)
        }
        fun isAnyPipe2RelayEnabledAssociatedToDeHumidifier(config: HyperStatPipe2Configuration): Boolean {
            return isAnyPipe2RelayMapped(config,Pipe2RelayAssociation.DEHUMIDIFIER)
        }

        private fun isAnyPipe2RelayMapped(config: HyperStatPipe2Configuration, association: Pipe2RelayAssociation): Boolean{
            return when {
                (config.relay1State.enabled && config.relay1State.association == association) -> true
                (config.relay2State.enabled && config.relay2State.association == association) -> true
                (config.relay3State.enabled && config.relay3State.association == association) -> true
                (config.relay4State.enabled && config.relay4State.association == association) -> true
                (config.relay5State.enabled && config.relay5State.association == association) -> true
                (config.relay6State.enabled && config.relay6State.association == association) -> true
                else -> false
            }
        }

        fun isAnyPipe2AnalogAssociatedToWaterValve(config: HyperStatPipe2Configuration): Boolean {
            return isPipe2AnalogOutMapped(config,Pipe2AnalogOutAssociation.WATER_VALVE)
        }
        fun isAnyPipe2AnalogAssociatedToFanSpeed(config: HyperStatPipe2Configuration): Boolean {
            return isPipe2AnalogOutMapped(config,Pipe2AnalogOutAssociation.FAN_SPEED)
        }
        fun isAnyPipe2AnalogAssociatedToDCV(config: HyperStatPipe2Configuration): Boolean {
            return isPipe2AnalogOutMapped(config,Pipe2AnalogOutAssociation.DCV_DAMPER)
        }
        private fun isPipe2AnalogOutMapped(config: HyperStatPipe2Configuration, association: Pipe2AnalogOutAssociation): Boolean{
            return when {
                (config.analogOut1State.enabled && config.analogOut1State.association == association) -> true
                (config.analogOut2State.enabled && config.analogOut2State.association == association) -> true
                (config.analogOut3State.enabled && config.analogOut3State.association == association) -> true
                else -> false
            }
        }
        fun isPipe2RelayAssociatedToFan(relayState: Pipe2RelayState): Boolean {
            return (relayState.association == Pipe2RelayAssociation.FAN_LOW_SPEED
                    || relayState.association == Pipe2RelayAssociation.FAN_MEDIUM_SPEED
                    || relayState.association == Pipe2RelayAssociation.FAN_HIGH_SPEED)
        }

        fun isAnalogInMappedTo(analogIn: AnalogInState, analogInAssociation: AnalogInAssociation): Boolean{
            return (analogIn.enabled && analogIn.association == analogInAssociation)
        }

        fun isHpuRelayAssociatedToFan(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.FAN_LOW_SPEED
                    || relayState.association == HpuRelayAssociation.FAN_MEDIUM_SPEED
                    || relayState.association == HpuRelayAssociation.FAN_HIGH_SPEED)
        }


        private fun isHpuAnalogOutMapped(config: HyperStatHpuConfiguration, association: HpuAnalogOutAssociation): Boolean{
            return when {
                (config.analogOut1State.enabled && config.analogOut1State.association == association) -> true
                (config.analogOut2State.enabled && config.analogOut2State.association == association) -> true
                (config.analogOut3State.enabled && config.analogOut3State.association == association) -> true
                else -> false
            }
        }

        fun getPipe2SelectedFanLevel(configuration: HyperStatPipe2Configuration): Int {

            var fanLevel = 0
            var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
                first = false,  //  Fan low
                second = false, //  Fan Medium
                third = false   //  Fan High
            )

            if(isAnyPipe2AnalogAssociatedToFanSpeed(configuration)) return 21 // All options are enabled due to
            // analog fan speed

            if (configuration.relay1State.enabled && isPipe2RelayAssociatedToFan(configuration.relay1State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay1State.association.ordinal, fanEnabledStages,
                    Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay2State.enabled && isPipe2RelayAssociatedToFan(configuration.relay2State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay2State.association.ordinal, fanEnabledStages,
                    Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay3State.enabled && isPipe2RelayAssociatedToFan(configuration.relay3State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay3State.association.ordinal, fanEnabledStages,
                    Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay4State.enabled && isPipe2RelayAssociatedToFan(configuration.relay4State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay4State.association.ordinal, fanEnabledStages,
                    Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay5State.enabled && isPipe2RelayAssociatedToFan(configuration.relay5State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay5State.association.ordinal, fanEnabledStages,
                    Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay6State.enabled && isPipe2RelayAssociatedToFan(configuration.relay6State))
                fanEnabledStages = updateSelectedFanLevels(
                    configuration.relay6State.association.ordinal, fanEnabledStages,
                    Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (fanEnabledStages.first) fanLevel += 6
            if (fanEnabledStages.second) fanLevel += 7
            if (fanEnabledStages.third) fanLevel += 8
            return fanLevel
        }

        fun getHpuSelectedFanLevel(configuration: HyperStatHpuConfiguration): Int {

            var fanLevel = 0
            var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
                first = false,  //  Fan low
                second = false, //  Fan Medium
                third = false   //  Fan High
            )

            if(isAnyHpuAnalogAssociatedToFanSpeed(configuration)) return 21 // All options are enabled due to
            // analog fan speed

            if (configuration.relay1State.enabled && isHpuRelayAssociatedToFan(configuration.relay1State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay1State.association.ordinal, fanEnabledStages,
                    HpuRelayAssociation.FAN_LOW_SPEED.ordinal,
                    HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    HpuRelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay2State.enabled && isHpuRelayAssociatedToFan(configuration.relay2State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay2State.association.ordinal, fanEnabledStages,
                    HpuRelayAssociation.FAN_LOW_SPEED.ordinal,
                    HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    HpuRelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay3State.enabled && isHpuRelayAssociatedToFan(configuration.relay3State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay3State.association.ordinal, fanEnabledStages,
                    HpuRelayAssociation.FAN_LOW_SPEED.ordinal,
                    HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    HpuRelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay4State.enabled && isHpuRelayAssociatedToFan(configuration.relay4State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay4State.association.ordinal, fanEnabledStages,
                    HpuRelayAssociation.FAN_LOW_SPEED.ordinal,
                    HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    HpuRelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay5State.enabled && isHpuRelayAssociatedToFan(configuration.relay5State))
                fanEnabledStages = updateSelectedFanLevels(configuration.relay5State.association.ordinal, fanEnabledStages,
                    HpuRelayAssociation.FAN_LOW_SPEED.ordinal,
                    HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    HpuRelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (configuration.relay6State.enabled && isHpuRelayAssociatedToFan(configuration.relay6State))
                fanEnabledStages = updateSelectedFanLevels(
                    configuration.relay6State.association.ordinal, fanEnabledStages,
                    HpuRelayAssociation.FAN_LOW_SPEED.ordinal,
                    HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal,
                    HpuRelayAssociation.FAN_HIGH_SPEED.ordinal
                )

            if (fanEnabledStages.first) fanLevel += 6
            if (fanEnabledStages.second) fanLevel += 7
            if (fanEnabledStages.third) fanLevel += 8
            return fanLevel
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

        // Hpu to association
        // COMPRESSOR_STAGE1,COMPRESSOR_STAGE2,COMPRESSOR_STAGE3,AUX_HEATING_STAGE1,AUX_HEATING_STAGE2,
        // FAN_LOW_SPEED,FAN_MEDIUM_SPEED,FAN_HIGH_SPEED,FAN_ENABLED,OCCUPIED_ENABLED,HUMIDIFIER,
        // DEHUMIDIFIER,CHANGE_OVER_O_COOLING,CHANGE_OVER_B_HEATING

        fun isHpuRelayCompressorStage1(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.COMPRESSOR_STAGE1)
        }
        fun isHpuRelayCompressorStage2(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.COMPRESSOR_STAGE2)
        }
        fun isHpuRelayCompressorStage3(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.COMPRESSOR_STAGE3)
        }
        fun isHpuRelayAuxHeatingStage1(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.AUX_HEATING_STAGE1)
        }
        fun isHpuRelayAuxHeatingStage2(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.AUX_HEATING_STAGE2)
        }
        fun isHpuRelayFanLowSpeed(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.FAN_LOW_SPEED)
        }
        fun isHpuRelayFanMediumSpeed(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.FAN_MEDIUM_SPEED)
        }
        fun isHpuRelayFanHighSpeed(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.FAN_HIGH_SPEED)
        }
        fun isHpuRelayFanEnabled(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.FAN_ENABLED)
        }
        fun isHpuRelayOccupiedEnabled(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.OCCUPIED_ENABLED)
        }
        fun isHpuRelayHumidifierEnabled(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.HUMIDIFIER)
        }
        fun isHpuRelayDeHumidifierEnabled(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.DEHUMIDIFIER)
        }
        fun isHpuRelayChangeOverCooling(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.CHANGE_OVER_O_COOLING)
        }
        fun isHpuRelayChangeOverHeating(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.CHANGE_OVER_B_HEATING)
        }

        //Function which checks the Relay is Associated  to Compressor Stage
        fun isRelayAssociatedToCompressorStage(relayState: HpuRelayState): Boolean {
            return (relayState.association == HpuRelayAssociation.COMPRESSOR_STAGE1
                    || relayState.association == HpuRelayAssociation.COMPRESSOR_STAGE2
                    || relayState.association == HpuRelayAssociation.COMPRESSOR_STAGE3)

        }

        // Analog mapping
        fun isHpuAnalogOutMappedToFanSpeed(analogOut: HpuAnalogOutState): Boolean{
            return analogOut.association == HpuAnalogOutAssociation.FAN_SPEED
        }
        fun isHpuAnalogOutMappedToDcvDamper(analogOut: HpuAnalogOutState): Boolean{
            return analogOut.association == HpuAnalogOutAssociation.DCV_DAMPER
        }
        fun isHpuAnalogOutMappedToCompressorSpeed(analogOut: HpuAnalogOutState): Boolean{
            return analogOut.association == HpuAnalogOutAssociation.COMPRESSOR_SPEED
        }
        // Function which returns the Relay Mapped state
        fun getHpuRelayAssociatedStage(state: Int): HpuRelayAssociation {
            return when (state) {
                // Order is important here
                0 -> HpuRelayAssociation.COMPRESSOR_STAGE1
                1 -> HpuRelayAssociation.COMPRESSOR_STAGE2
                2 -> HpuRelayAssociation.COMPRESSOR_STAGE3
                3 -> HpuRelayAssociation.AUX_HEATING_STAGE1
                4 -> HpuRelayAssociation.AUX_HEATING_STAGE2
                5 -> HpuRelayAssociation.FAN_LOW_SPEED
                6 -> HpuRelayAssociation.FAN_MEDIUM_SPEED
                7 -> HpuRelayAssociation.FAN_HIGH_SPEED
                8 -> HpuRelayAssociation.FAN_ENABLED
                9 -> HpuRelayAssociation.OCCUPIED_ENABLED
                10 -> HpuRelayAssociation.HUMIDIFIER
                11 -> HpuRelayAssociation.DEHUMIDIFIER
                12 -> HpuRelayAssociation.CHANGE_OVER_O_COOLING
                13 -> HpuRelayAssociation.CHANGE_OVER_B_HEATING
                else -> HpuRelayAssociation.FAN_LOW_SPEED
            }
        }
        // Function which returns the Relay Mapped state
        fun getHpuAnalogOutAssociatedStage(state: Int): HpuAnalogOutAssociation {
            return when (state) {
                // Order is important here
                0 -> HpuAnalogOutAssociation.COMPRESSOR_SPEED
                1 -> HpuAnalogOutAssociation.FAN_SPEED
                2 -> HpuAnalogOutAssociation.DCV_DAMPER

                // assuming it never going to call
                else -> HpuAnalogOutAssociation.COMPRESSOR_SPEED
            }
        }

        fun isAnyHpuRelayAssociatedToCompressorStage1(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.COMPRESSOR_STAGE1)
        }
        fun isAnyHpuRelayAssociatedToCompressorStage2(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.COMPRESSOR_STAGE2)
        }
        fun isAnyHpuRelayAssociatedToCompressorStage3(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.COMPRESSOR_STAGE3)
        }
        fun isAnyHpuRelayAssociatedToAuxHeatingStage1(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.AUX_HEATING_STAGE1)
        }
        fun isAnyHpuRelayAssociatedToAuxHeatingStage2(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.AUX_HEATING_STAGE2)
        }

        fun isAnyHpuRelayAssociatedToFanLow(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.FAN_LOW_SPEED)
        }
        fun isAnyHpuRelayAssociatedToFanMedium(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.FAN_MEDIUM_SPEED)
        }
        fun isAnyHpuRelayAssociatedToFanHigh(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.FAN_HIGH_SPEED)
        }
        fun isAnyHpuRelayAssociatedToFanEnabled(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.FAN_ENABLED)
        }
        fun isAnyHpuRelayAssociatedToOccupiedEnabled(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.OCCUPIED_ENABLED)
        }
        fun isAnyHpuRelayEnabledAssociatedToHumidifier(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.HUMIDIFIER)
        }
        fun isAnyHpuRelayEnabledAssociatedToDeHumidifier(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.DEHUMIDIFIER)
        }
        fun isAnyHpuRelayEnabledAssociatedToChangeOverCooling(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.CHANGE_OVER_O_COOLING)
        }
        fun isAnyHpuRelayEnabledAssociatedToChangeOverHeating(config: HyperStatHpuConfiguration): Boolean {
            return isAnyHpuRelayMapped(config,HpuRelayAssociation.CHANGE_OVER_B_HEATING)
        }


        fun isAnyHpuAnalogAssociatedToCompressorSpeed(config: HyperStatHpuConfiguration): Boolean {
            return isHpuAnalogOutMapped(config,HpuAnalogOutAssociation.COMPRESSOR_SPEED)
        }
        fun isAnyHpuAnalogAssociatedToFanSpeed(config: HyperStatHpuConfiguration): Boolean {
            return isHpuAnalogOutMapped(config,HpuAnalogOutAssociation.FAN_SPEED)
        }
        fun isAnyHpuAnalogAssociatedToDCV(config: HyperStatHpuConfiguration): Boolean {
            return isHpuAnalogOutMapped(config,HpuAnalogOutAssociation.DCV_DAMPER)
        }

        private fun isAnyHpuRelayMapped(config: HyperStatHpuConfiguration, association: HpuRelayAssociation): Boolean{
            return when {
                (config.relay1State.enabled && config.relay1State.association == association) -> true
                (config.relay2State.enabled && config.relay2State.association == association) -> true
                (config.relay3State.enabled && config.relay3State.association == association) -> true
                (config.relay4State.enabled && config.relay4State.association == association) -> true
                (config.relay5State.enabled && config.relay5State.association == association) -> true
                (config.relay6State.enabled && config.relay6State.association == association) -> true
                else -> false
            }
        }


        fun isStagedFanEnabled(
            hyperStatConfig: HyperStatCpuConfiguration,
            fanStage: CpuRelayAssociation
        ): Boolean {
            return  hyperStatConfig.relay1State.enabled && hyperStatConfig.relay1State.association == fanStage ||
                    hyperStatConfig.relay2State.enabled && hyperStatConfig.relay2State.association == fanStage ||
                    hyperStatConfig.relay3State.enabled && hyperStatConfig.relay3State.association == fanStage ||
                    hyperStatConfig.relay4State.enabled && hyperStatConfig.relay4State.association == fanStage ||
                    hyperStatConfig.relay5State.enabled && hyperStatConfig.relay5State.association == fanStage ||
                    hyperStatConfig.relay6State.enabled && hyperStatConfig.relay6State.association == fanStage
        }

        fun isAnyAnalogOutMappedToStagedFan(
            hyperStatConfig: HyperStatCpuConfiguration,
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