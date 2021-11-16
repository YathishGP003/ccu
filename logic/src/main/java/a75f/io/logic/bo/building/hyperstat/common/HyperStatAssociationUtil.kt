package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.cpu.*
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
                1 -> CpuAnalogOutAssociation.FAN_SPEED
                2 -> CpuAnalogOutAssociation.HEATING
                3 -> CpuAnalogOutAssociation.DCV_DAMPER
                // assuming it never going to call
                else -> CpuAnalogOutAssociation.COOLING
            }
        }

        // Function which returns the Relay Mapped state
        fun getAnalogInStage(state: Int): CpuAnalogInAssociation {
            return when (state) {
                // Order is important here
                0 -> CpuAnalogInAssociation.CURRENT_TX_0_10
                1 -> CpuAnalogInAssociation.CURRENT_TX_0_20
                2 -> CpuAnalogInAssociation.CURRENT_TX_0_50
                3 -> CpuAnalogInAssociation.KEY_CARD_SENSOR
                4 -> CpuAnalogInAssociation.DOOR_WINDOW_SENSOR

                // assuming it never going to call
                else -> CpuAnalogInAssociation.CURRENT_TX_0_10
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
            return (analogOut.association == CpuAnalogOutAssociation.FAN_SPEED)
        }

        //Function which checks the Analog out is Associated  to HEATING
        fun isAnalogOutAssociatedToHeating(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuAnalogOutAssociation.HEATING)
        }

        //Function which checks the Analog out is Associated  to DCV Damper
        fun isAnalogOutAssociatedToDcvDamper(analogOut: AnalogOutState): Boolean {
            return (analogOut.association == CpuAnalogOutAssociation.DCV_DAMPER)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToCurrentTX10(analogIn: AnalogInState): Boolean {
            return (analogIn.association == CpuAnalogInAssociation.CURRENT_TX_0_10)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToCurrentTX20(analogIn: AnalogInState): Boolean {
            return (analogIn.association == CpuAnalogInAssociation.CURRENT_TX_0_20)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToCurrentTX50(analogIn: AnalogInState): Boolean {
            return (analogIn.association == CpuAnalogInAssociation.CURRENT_TX_0_50)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToDoorWindowSensor(analogIn: AnalogInState): Boolean {
            return (analogIn.association == CpuAnalogInAssociation.DOOR_WINDOW_SENSOR)
        }

        //Function which checks the Analog in is Associated  to CURRENT_TX_0_10
        fun isAnalogInAssociatedToKeyCardSensor(analogIn: AnalogInState): Boolean {
            return (analogIn.association == CpuAnalogInAssociation.KEY_CARD_SENSOR)
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
                (isAnalogOutAssociatedToFanSpeed(analogOut1)) -> {
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

        // checks two Relay configurations and return based on the match
        fun isBothAnalogInHasSameConfigs(analogIn1: AnalogInState, analogIn2: AnalogInState): Boolean {
            when {
                (analogIn1.enabled != analogIn2.enabled) -> return false
                (analogIn1.association != analogIn2.association) -> return false
            }
            return true
        }


        // Function which checks that any of the relay is associated to cooling
        fun isAnyRelayAssociatedToCooling(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (isRelayAssociatedToCoolingStage(configuration.relay1State)) -> true
                (isRelayAssociatedToCoolingStage(configuration.relay2State)) -> true
                (isRelayAssociatedToCoolingStage(configuration.relay3State)) -> true
                (isRelayAssociatedToCoolingStage(configuration.relay4State)) -> true
                (isRelayAssociatedToCoolingStage(configuration.relay5State)) -> true
                (isRelayAssociatedToCoolingStage(configuration.relay6State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay is associated to Heating
        fun isAnyRelayAssociatedToHeating(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (isRelayAssociatedToHeatingStage(configuration.relay1State)) -> true
                (isRelayAssociatedToHeatingStage(configuration.relay2State)) -> true
                (isRelayAssociatedToHeatingStage(configuration.relay3State)) -> true
                (isRelayAssociatedToHeatingStage(configuration.relay4State)) -> true
                (isRelayAssociatedToHeatingStage(configuration.relay5State)) -> true
                (isRelayAssociatedToHeatingStage(configuration.relay6State)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay is associated to Fan (Stage1,Stage2,Stage3)
        fun isAnyRelayEnabledAssociatedToFan(configuration: HyperStatCpuConfiguration): Boolean {
            return when {
                (configuration.relay1State.enabled && isRelayAssociatedToFan(configuration.relay1State)) -> true
                (configuration.relay1State.enabled && isRelayAssociatedToFan(configuration.relay2State)) -> true
                (configuration.relay1State.enabled && isRelayAssociatedToFan(configuration.relay3State)) -> true
                (configuration.relay1State.enabled && isRelayAssociatedToFan(configuration.relay4State)) -> true
                (configuration.relay1State.enabled && isRelayAssociatedToFan(configuration.relay5State)) -> true
                (configuration.relay1State.enabled && isRelayAssociatedToFan(configuration.relay6State)) -> true
                else -> false
            }
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

            if (configuration.relay1State.enabled && isRelayAssociatedToFan(configuration.relay1State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay1State.association, fanEnabledStages)

            if (configuration.relay2State.enabled && isRelayAssociatedToFan(configuration.relay2State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay2State.association, fanEnabledStages)

            if (configuration.relay3State.enabled && isRelayAssociatedToFan(configuration.relay3State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay3State.association, fanEnabledStages)

            if (configuration.relay4State.enabled && isRelayAssociatedToFan(configuration.relay4State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay4State.association, fanEnabledStages)

            if (configuration.relay5State.enabled && isRelayAssociatedToFan(configuration.relay5State))
                fanEnabledStages = updateSelectedFanLevel(configuration.relay5State.association, fanEnabledStages)

            if (configuration.relay6State.enabled && isRelayAssociatedToFan(configuration.relay6State))
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

        // function which checks the any of the relay is associated to cooling heating fan stage or fan enabled
        fun isRelayAssociatedToAnyOfConditioningModes(relayState: RelayState): Boolean{
            if(!relayState.enabled) return false
            if(isRelayAssociatedToCoolingStage(relayState)) return true
            if(isRelayAssociatedToHeatingStage(relayState)) return true
            if(isRelayAssociatedToFan(relayState)) return true
            if(isRelayAssociatedToFanEnabled(relayState)) return true
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
                      //  if (selectedFan in 1..4)
                            return StandaloneFanStage.values()[selectedFan - 3].ordinal
                    }
                    (fanLevel == 8) -> {
                        // R.array.hyperstate_only_high_fanmode
                     //   if (selectedFan in 1..4)
                            return StandaloneFanStage.values()[selectedFan - 6].ordinal
                    }
                    (fanLevel == 13) -> {
                        // When fan low and mediam are selected
                        //R.array.smartstat_2pfcu_fanmode_medium
                       // if (selectedFan in 1..7)
                            return StandaloneFanStage.values()[selectedFan].ordinal
                    }
                    (fanLevel == 15) -> {
                        // Medium and high fan speeds are selected
                        // R.array.hyperstate_medium_high_fanmode
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

        fun getSensorNameByType(sensorInputs: CpuAnalogInAssociation): String {
            /**
             * These sensor names are constant do not change please refer Sensor Manager class for more info
             * a75f.io.logic.bo.building.sensors.SensorManager.getExternalSensorListV2
             */

            return when (sensorInputs) {
                // These positions are constant sensor position which are available in Sensor manager class
                CpuAnalogInAssociation.CURRENT_TX_0_10 -> "8"
                CpuAnalogInAssociation.CURRENT_TX_0_20 -> "9"
                CpuAnalogInAssociation.CURRENT_TX_0_50 -> "10"
                CpuAnalogInAssociation.KEY_CARD_SENSOR -> "12"
                CpuAnalogInAssociation.DOOR_WINDOW_SENSOR -> "13"
            }
        }

    }

}