package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common

import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.CpuSensorBusType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.SensorTempHumidityAssociationConfig
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration

/**
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

class HyperStatSplitAssociationUtil {
    companion object {


        //Function which checks the Sensor bus address is Associated  to OUTSIDE_AIR_TEMPERATURE_HUMIDITY
        private fun isSensorBusAddressAssociatedToOutsideAir(association: SensorTempHumidityAssociationConfig): Boolean {
            return (association.temperatureAssociation.associationVal == CpuSensorBusType.OUTSIDE_AIR.ordinal)
        }

        //Function which checks the Analog out is Associated  to FAN_SPEED
        private fun isAnalogOutAssociatedToFanSpeed(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuAnalogControlType.LINEAR_FAN.ordinal)
        }


        //Function which checks the Analog out is Associated  to STAGED_FAN_SPEED
        private fun isAnalogOutAssociatedToStagedFanSpeed(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuAnalogControlType.STAGED_FAN.ordinal)
        }

        fun isAnyRelayAssociatedToFanEnabled(config: HyperStatSplitCpuConfiguration): Boolean {
            return isAnyRelayEnabledMapped(config, CpuRelayType.FAN_ENABLED)
        }


        private fun isAnyRelayEnabledMapped(config: HyperStatSplitCpuConfiguration, association: CpuRelayType): Boolean{
            return when {
                (config.relay1Enabled.enabled && config.relay1Association.associationVal == association.ordinal) -> true
                (config.relay2Enabled.enabled && config.relay2Association.associationVal == association.ordinal) -> true
                (config.relay3Enabled.enabled && config.relay3Association.associationVal == association.ordinal) -> true
                (config.relay4Enabled.enabled && config.relay4Association.associationVal == association.ordinal) -> true
                (config.relay5Enabled.enabled && config.relay5Association.associationVal == association.ordinal) -> true
                (config.relay6Enabled.enabled && config.relay6Association.associationVal == association.ordinal) -> true
                (config.relay7Enabled.enabled && config.relay7Association.associationVal == association.ordinal) -> true
                (config.relay8Enabled.enabled && config.relay8Association.associationVal == association.ordinal) -> true
                else -> false
            }
        }



        fun isAnySensorBusAddressMappedToOutsideAir(
            config: HyperStatSplitConfiguration
        ): Boolean {
            return config.address0Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address0SensorAssociation)
                    || config.address1Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address1SensorAssociation)
                    || config.address2Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address2SensorAssociation)
        }

        fun getSelectedFanLevel(config: HyperStatSplitCpuConfiguration): Int {

            var fanLevel = 0
            var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
                first = false,  //  Fan low
                second = false, //  Fan Medium
                third = false   //  Fan High
            )

            if(isAnyAnalogOutEnabledAssociatedToFanSpeed(config) || isAnyAnalogOutMappedToStagedFan(config)) return 21 // All options are enabled due to
            // analog fan speed

            if (config.relay1Enabled.enabled && config.isRelayAssociatedToFan(config.relay1Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay1Association, fanEnabledStages)

            if (config.relay2Enabled.enabled && config.isRelayAssociatedToFan(config.relay2Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay2Association, fanEnabledStages)

            if (config.relay3Enabled.enabled && config.isRelayAssociatedToFan(config.relay3Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay3Association, fanEnabledStages)

            if (config.relay4Enabled.enabled && config.isRelayAssociatedToFan(config.relay4Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay4Association, fanEnabledStages)

            if (config.relay5Enabled.enabled && config.isRelayAssociatedToFan(config.relay5Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay5Association, fanEnabledStages)

            if (config.relay6Enabled.enabled && config.isRelayAssociatedToFan(config.relay6Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay6Association, fanEnabledStages)

            if (config.relay7Enabled.enabled && config.isRelayAssociatedToFan(config.relay7Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay7Association, fanEnabledStages)

            if (config.relay8Enabled.enabled && config.isRelayAssociatedToFan(config.relay8Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay8Association, fanEnabledStages)

            if (fanEnabledStages.first) fanLevel += 6
            if (fanEnabledStages.second) fanLevel += 7
            if (fanEnabledStages.third) fanLevel += 8

            if (fanLevel == 0 && (isAnyRelayEnabledMapped(config,CpuRelayType.FAN_ENABLED))) {
                fanLevel = 1
            }

            return fanLevel
        }


        fun getHssProfileFanLevel(
            equip: HyperStatSplitEquip,
        ): Int {
            return when (equip) {
                is HsSplitCpuEquip , is Pipe4UVEquip -> getEconSelectedFanLevel(equip)
                else -> 0
            }
        }

         fun getEconSelectedFanLevel(hssEquip: HyperStatSplitEquip): Int {

            var fanLevel = 0

            if(hssEquip.linearFanSpeed.pointExists() || hssEquip.stagedFanSpeed.pointExists() || hssEquip.fanSignal.pointExists()) return 21 // All options are enabled due to
            // analog fan speed

            if (hssEquip.fanLowSpeed.pointExists() || hssEquip.fanLowSpeedVentilation.pointExists()) fanLevel += 6
            if (hssEquip.fanMediumSpeed.pointExists()) fanLevel += 7
            if (hssEquip.fanHighSpeed.pointExists()) fanLevel += 8

            if (fanLevel == 0 && hssEquip.fanEnable.pointExists()) {
                fanLevel = 1
            }

            return fanLevel
        }

        private fun updateSelectedFanLevel(
            association: AssociationConfig, currentFoundDetails: Triple<Boolean, Boolean, Boolean>
        ): Triple<Boolean, Boolean, Boolean> {
            var currentStatus = currentFoundDetails
            if (!currentStatus.first) {
                currentStatus = currentStatus.copy(
                    first = association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal
                )
            }
            if (!currentStatus.second) {
                currentStatus = currentStatus.copy(
                    second = association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal
                )
            }
            if (!currentStatus.third) {
                currentStatus = currentStatus.copy(
                    third = association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal
                )
            }
            return currentStatus
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToHeating(config: HyperStatSplitCpuConfiguration): Boolean {
            return when {
                (config.relay1Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay1Association)) -> true
                (config.relay2Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay2Association)) -> true
                (config.relay3Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay3Association)) -> true
                (config.relay4Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay4Association)) -> true
                (config.relay5Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay5Association)) -> true
                (config.relay6Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay6Association)) -> true
                (config.relay7Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay7Association)) -> true
                (config.relay8Enabled.enabled &&
                        config.isRelayAssociatedToHeatingStage(config.relay8Association)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToHeating(hssEquip: HsSplitCpuEquip): Boolean {
            return hssEquip.heatingStage1.pointExists() || hssEquip.heatingStage2.pointExists() || hssEquip.heatingStage3.pointExists()
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToCooling(config: HyperStatSplitCpuConfiguration): Boolean {
            return when {
                (config.relay1Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay1Association)) -> true
                (config.relay2Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay2Association)) -> true
                (config.relay3Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay3Association)) -> true
                (config.relay4Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay4Association)) -> true
                (config.relay5Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay5Association)) -> true
                (config.relay6Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay6Association)) -> true
                (config.relay7Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay7Association)) -> true
                (config.relay8Enabled.enabled &&
                        config.isRelayAssociatedToCoolingStage(config.relay8Association)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToCooling(hssEquip: HsSplitCpuEquip): Boolean {
            return hssEquip.coolingStage1.pointExists() || hssEquip.coolingStage2.pointExists() || hssEquip.coolingStage3.pointExists()
        }

        // Function which checks that any of the Analog Out is mapped to Cooling
        fun isAnyAnalogOutEnabledAssociatedToCooling(configuration: HyperStatSplitCpuConfiguration): Boolean {
            return when {
                (configuration.analogOut1Enabled.enabled &&
                        configuration.analogOut1Association.associationVal == CpuAnalogControlType.COOLING.ordinal) -> true
                (configuration.analogOut2Enabled.enabled &&
                        configuration.analogOut2Association.associationVal == CpuAnalogControlType.COOLING.ordinal) -> true
                (configuration.analogOut3Enabled.enabled &&
                        configuration.analogOut3Association.associationVal == CpuAnalogControlType.COOLING.ordinal) -> true
                (configuration.analogOut4Enabled.enabled &&
                        configuration.analogOut4Association.associationVal == CpuAnalogControlType.COOLING.ordinal) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyAnalogOutEnabledAssociatedToCooling(hssEquip: HsSplitCpuEquip): Boolean {
            return hssEquip.coolingSignal.pointExists()
        }

        // Function which checks that any of the Analog Out is mapped to Heating
        fun isAnyAnalogOutEnabledAssociatedToHeating(configuration: HyperStatSplitCpuConfiguration): Boolean {
            return when {
                (configuration.analogOut1Enabled.enabled &&
                        configuration.analogOut1Association.associationVal == CpuAnalogControlType.HEATING.ordinal) -> true
                (configuration.analogOut2Enabled.enabled &&
                        configuration.analogOut2Association.associationVal == CpuAnalogControlType.HEATING.ordinal) -> true
                (configuration.analogOut3Enabled.enabled &&
                        configuration.analogOut3Association.associationVal == CpuAnalogControlType.HEATING.ordinal) -> true
                (configuration.analogOut4Enabled.enabled &&
                        configuration.analogOut4Association.associationVal == CpuAnalogControlType.HEATING.ordinal) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Fan speed
        private fun isAnyAnalogOutEnabledAssociatedToFanSpeed(configuration: HyperStatSplitCpuConfiguration): Boolean {
            return when {
                (configuration.analogOut1Enabled.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut1Association)) -> true
                (configuration.analogOut2Enabled.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut2Association)) -> true
                (configuration.analogOut3Enabled.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut3Association)) -> true
                (configuration.analogOut4Enabled.enabled &&
                        isAnalogOutAssociatedToFanSpeed(configuration.analogOut4Association)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to heating
        fun isAnyAnalogOutEnabledAssociatedToHeating(hssEquip: HsSplitCpuEquip): Boolean {
            return hssEquip.heatingSignal.pointExists()
        }

        /**
         * Determines the lowest fan stage based on the relay states in the given CPU configuration.
         *
         * @param config the CPU configuration to analyze.
         * @return the lowest fan stage, represented as a [CpuRelayType] value.
         */
        fun getLowestFanStage(config: HyperStatSplitCpuConfiguration): CpuRelayType {
            var lowestValue = 0xFF
            lowestValue = verifyFanStateLowValue(config.relay1Enabled, config.relay1Association, lowestValue,config)
            lowestValue = verifyFanStateLowValue(config.relay2Enabled, config.relay2Association, lowestValue,config)
            lowestValue = verifyFanStateLowValue(config.relay3Enabled, config.relay3Association, lowestValue,config)
            lowestValue = verifyFanStateLowValue(config.relay4Enabled, config.relay4Association, lowestValue,config)
            lowestValue = verifyFanStateLowValue(config.relay5Enabled, config.relay5Association, lowestValue,config)
            lowestValue = verifyFanStateLowValue(config.relay6Enabled, config.relay6Association, lowestValue,config)
            lowestValue = verifyFanStateLowValue(config.relay7Enabled, config.relay7Association, lowestValue,config)
            lowestValue = verifyFanStateLowValue(config.relay8Enabled, config.relay8Association, lowestValue,config)

            return CpuRelayType.values().getOrNull(lowestValue) ?: CpuRelayType.DEHUMIDIFIER
        }

        /**
         * Verifies the lowest fan state value based on the given relay state and the current lowest value.
         *
         * @param lowestValue the current lowest value.
         * @return the updated lowest value, considering the relay state.
         */
        private fun verifyFanStateLowValue(enabled: EnableConfig, association: AssociationConfig, lowestValue: Int, config: HyperStatSplitCpuConfiguration): Int {
            if (enabled.enabled && config.isRelayAssociatedToFan(association)
                && association.associationVal < lowestValue
            )
                return association.associationVal
            return lowestValue
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
                CcuLog.e(L.TAG_CCU_HSSPLIT_CPUECON, "Error getSelectedFan function ${e.localizedMessage}",e)
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
                CcuLog.e(L.TAG_CCU_HSSPLIT_CPUECON, "Error getSelectedFan function ${e.localizedMessage}",e)
            }
            return StandaloneFanStage.OFF.ordinal
        }

        private fun isAnyAnalogOutMappedToStagedFan(
            hyperStatConfig: HyperStatSplitCpuConfiguration,
        ): Boolean {
            return when {
                (hyperStatConfig.analogOut1Enabled.enabled &&
                        isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut1Association)) -> true
                (hyperStatConfig.analogOut2Enabled.enabled &&
                        isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut2Association)) -> true
                (hyperStatConfig.analogOut3Enabled.enabled &&
                        isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut3Association)) -> true
                (hyperStatConfig.analogOut4Enabled.enabled &&
                        isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut4Association)) -> true
                else -> false
            }
        }

    }
}