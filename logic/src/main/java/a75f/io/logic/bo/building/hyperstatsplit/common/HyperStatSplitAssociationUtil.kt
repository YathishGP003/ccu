package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.profiles.SensorTempHumidityAssociationConfig
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.*

/**
 * Created for HyperStat by Manjunath K on 30-07-2021.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

class HyperStatSplitAssociationUtil {
    companion object {

        //Function which checks the Relay is Associated  to Fan or Not
        fun isRelayAssociatedToFan(relayConfig: AssociationConfig): Boolean {
            return (relayConfig.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal
                    || relayConfig.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal
                    || relayConfig.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal)
        }
        fun isRelayAssociatedToFanLow(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal
        }
        fun isRelayAssociatedToFanMedium(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal
        }
        fun isRelayAssociatedToFanHigh(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal
        }

        //Function which checks the Relay is Associated  to Cooling Stage
        fun isRelayAssociatedToCoolingStage(relayConfig: AssociationConfig): Boolean {
            return (relayConfig.associationVal == CpuRelayType.COOLING_STAGE1.ordinal
                    || relayConfig.associationVal == CpuRelayType.COOLING_STAGE2.ordinal
                    || relayConfig.associationVal == CpuRelayType.COOLING_STAGE3.ordinal)
        }
        fun isRelayAssociatedToCoolingStage1(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.COOLING_STAGE1.ordinal
        }
        fun isRelayAssociatedToCoolingStage2(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.COOLING_STAGE2.ordinal
        }
        fun isRelayAssociatedToCoolingStage3(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.COOLING_STAGE3.ordinal
        }

        //Function which checks the Relay is Associated  to Heating Stage
        fun isRelayAssociatedToHeatingStage(relayConfig: AssociationConfig): Boolean {
            return (relayConfig.associationVal == CpuRelayType.HEATING_STAGE1.ordinal
                    || relayConfig.associationVal == CpuRelayType.HEATING_STAGE2.ordinal
                    || relayConfig.associationVal == CpuRelayType.HEATING_STAGE3.ordinal)

        }
        fun isRelayAssociatedToHeatingStage1(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.HEATING_STAGE1.ordinal

        }
        fun isRelayAssociatedToHeatingStage2(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.HEATING_STAGE2.ordinal

        }
        fun isRelayAssociatedToHeatingStage3(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.HEATING_STAGE3.ordinal

        }

        //Function which checks the Sensor bus address is Associated  to SUPPLY_AIR_TEMPERATURE_HUMIDITY
        fun isSensorBusAddressAssociatedToSupplyAir(association: SensorTempHumidityAssociationConfig): Boolean {
            return (association.temperatureAssociation.associationVal == CpuSensorBusType.SUPPLY_AIR.ordinal)
        }

        //Function which checks the Sensor bus address is Associated  to OUTSIDE_AIR_TEMPERATURE_HUMIDITY
        fun isSensorBusAddressAssociatedToOutsideAir(association: SensorTempHumidityAssociationConfig): Boolean {
            return (association.temperatureAssociation.associationVal == CpuSensorBusType.OUTSIDE_AIR.ordinal)
        }

        //Function which checks the Relay is Associated to Fan Enabled
        fun isRelayAssociatedToFanEnabled(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.FAN_ENABLED.ordinal
        }

        //Function which checks the Relay is Associated to Occupied Enabled
        fun isRelayAssociatedToOccupiedEnabled(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.OCCUPIED_ENABLED.ordinal
        }

        //Function which checks the Relay is Associated to Humidifier
        fun isRelayAssociatedToHumidifier(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.HUMIDIFIER.ordinal
        }

        //Function which checks the Relay is Associated to Dehumidifier
        fun isRelayAssociatedToDeHumidifier(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.DEHUMIDIFIER.ordinal
        }

        //Function which checks the Relay is Associated to Exhaust Fan Stage 1
        fun isRelayAssociatedToExhaustFanStage1(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.EX_FAN_STAGE1.ordinal
        }

        //Function which checks the Relay is Associated to Fan Enabled
        fun isRelayAssociatedToExhaustFanStage2(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.EX_FAN_STAGE2.ordinal
        }

        //Function which checks the Relay is Associated to Fan Enabled
        fun isRelayAssociatedToDcvDamper(relayConfig: AssociationConfig): Boolean {
            return relayConfig.associationVal == CpuRelayType.DCV_DAMPER.ordinal
        }

        //Function which checks the Analog out is Associated to COOLING
        fun isAnalogOutAssociatedToCooling(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuControlType.COOLING.ordinal)
        }

        //Function which checks the Analog out is Associated  to FAN_SPEED
        fun isAnalogOutAssociatedToFanSpeed(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuControlType.LINEAR_FAN.ordinal)
        }

        //Function which checks the Analog out is Associated to HEATING
        fun isAnalogOutAssociatedToHeating(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuControlType.HEATING.ordinal)
        }

        //Function which checks the Analog out is Associated  to OAO_DAMPER
        fun isAnalogOutAssociatedToOaoDamper(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuControlType.OAO_DAMPER.ordinal)
        }

        //Function which checks the Analog out is Associated  to RETURN_DAMPER
        fun isAnalogOutAssociatedToReturnDamper(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuControlType.RETURN_DAMPER.ordinal)
        }

        //Function which checks the Analog out is Associated  to STAGED_FAN_SPEED
        fun isAnalogOutAssociatedToStagedFanSpeed(analogOut: AssociationConfig): Boolean {
            return (analogOut.associationVal == CpuControlType.STAGED_FAN.ordinal)
        }

        fun isAnyRelayAssociatedToCoolingStage1(config: HyperStatSplitCpuProfileConfiguration) : Boolean {
            return isAnyRelayMapped(config,CpuRelayType.COOLING_STAGE1)
        }
        fun isAnyRelayAssociatedToCoolingStage2(config: HyperStatSplitCpuProfileConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayType.COOLING_STAGE2)
        }
        fun isAnyRelayAssociatedToCoolingStage3(config: HyperStatSplitCpuProfileConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayType.COOLING_STAGE3)
        }
        fun isAnyRelayAssociatedToFanEnabled(config: HyperStatSplitCpuProfileConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayType.FAN_ENABLED)
        }

        fun isAnyRelayAssociatedToHumidifier(config: HyperStatSplitCpuProfileConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayType.HUMIDIFIER)
        }

        fun isAnyRelayAssociatedToDeHumidifier(config: HyperStatSplitCpuProfileConfiguration): Boolean {
            return isAnyRelayMapped(config,CpuRelayType.DEHUMIDIFIER)
        }

        private fun isAnyRelayMapped(config: HyperStatSplitCpuProfileConfiguration, association: CpuRelayType): Boolean{
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

        fun isAnyAnalogAssociatedToOAO(config: HyperStatSplitCpuProfileConfiguration): Boolean {
            return when {
                (config.analogOut1Enabled.enabled &&
                        isAnalogOutAssociatedToOaoDamper(config.analogOut1Association)) -> true
                (config.analogOut2Enabled.enabled &&
                        isAnalogOutAssociatedToOaoDamper(config.analogOut2Association)) -> true
                (config.analogOut3Enabled.enabled &&
                        isAnalogOutAssociatedToOaoDamper(config.analogOut3Association)) -> true
                (config.analogOut4Enabled.enabled &&
                        isAnalogOutAssociatedToOaoDamper(config.analogOut4Association)) -> true
                else -> false
            }
        }

        private fun isUniversalInMapped(
            config: HyperStatSplitCpuProfileConfiguration,
            association: CpuUniInType
        ): Boolean{
            return when {
                (config.universal1InEnabled.enabled && config.universal1InAssociation.associationVal == association.ordinal) -> true
                (config.universal2InEnabled.enabled && config.universal2InAssociation.associationVal == association.ordinal) -> true
                (config.universal3InEnabled.enabled && config.universal3InAssociation.associationVal == association.ordinal) -> true
                (config.universal4InEnabled.enabled && config.universal4InAssociation.associationVal == association.ordinal) -> true
                (config.universal5InEnabled.enabled && config.universal5InAssociation.associationVal == association.ordinal) -> true
                (config.universal6InEnabled.enabled && config.universal6InAssociation.associationVal == association.ordinal) -> true
                (config.universal7InEnabled.enabled && config.universal7InAssociation.associationVal == association.ordinal) -> true
                (config.universal8InEnabled.enabled && config.universal8InAssociation.associationVal == association.ordinal) -> true

                else -> false
            }
        }

        fun isAnySensorBusAddressMappedToOutsideAir(
            config: HyperStatSplitCpuProfileConfiguration
        ): Boolean {
            return config.address0Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address0SensorAssociation)
                    || config.address1Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address1SensorAssociation)
                    || config.address2Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address2SensorAssociation)
        }

        fun getSelectedFanLevel(config: HyperStatSplitCpuProfileConfiguration): Int {

            var fanLevel = 0
            var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
                first = false,  //  Fan low
                second = false, //  Fan Medium
                third = false   //  Fan High
            )

            if(isAnyAnalogOutEnabledAssociatedToFanSpeed(config) || isAnyAnalogOutMappedToStagedFan(config)) return 21 // All options are enabled due to
            // analog fan speed

            if (config.relay1Enabled.enabled && isRelayAssociatedToFan(config.relay1Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay1Association, fanEnabledStages)

            if (config.relay2Enabled.enabled && isRelayAssociatedToFan(config.relay2Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay2Association, fanEnabledStages)

            if (config.relay3Enabled.enabled && isRelayAssociatedToFan(config.relay3Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay3Association, fanEnabledStages)

            if (config.relay4Enabled.enabled && isRelayAssociatedToFan(config.relay4Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay4Association, fanEnabledStages)

            if (config.relay5Enabled.enabled && isRelayAssociatedToFan(config.relay5Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay5Association, fanEnabledStages)

            if (config.relay6Enabled.enabled && isRelayAssociatedToFan(config.relay6Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay6Association, fanEnabledStages)

            if (config.relay7Enabled.enabled && isRelayAssociatedToFan(config.relay7Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay7Association, fanEnabledStages)

            if (config.relay8Enabled.enabled && isRelayAssociatedToFan(config.relay8Association))
                fanEnabledStages = updateSelectedFanLevel(config.relay8Association, fanEnabledStages)

            if (fanEnabledStages.first) fanLevel += 6
            if (fanEnabledStages.second) fanLevel += 7
            if (fanEnabledStages.third) fanLevel += 8
            return fanLevel
        }

        fun getSelectedFanLevel(hssEquip: HyperStatSplitEquip): Int {

            var fanLevel = 0
            var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
                first = false,  //  Fan low
                second = false, //  Fan Medium
                third = false   //  Fan High
            )

            if(hssEquip.linearFanSpeed.pointExists() || hssEquip.stagedFanSpeed.pointExists()) return 21 // All options are enabled due to
            // analog fan speed

            if (hssEquip.fanLowSpeed.pointExists()) fanLevel += 6
            if (hssEquip.fanMediumSpeed.pointExists()) fanLevel += 7
            if (hssEquip.fanHighSpeed.pointExists()) fanLevel += 8

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
        fun isAnyRelayEnabledAssociatedToHeating(configuration: HyperStatSplitCpuProfileConfiguration): Boolean {
            return when {
                (configuration.relay1Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay1Association)) -> true
                (configuration.relay2Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay2Association)) -> true
                (configuration.relay3Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay3Association)) -> true
                (configuration.relay4Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay4Association)) -> true
                (configuration.relay5Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay5Association)) -> true
                (configuration.relay6Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay6Association)) -> true
                (configuration.relay7Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay7Association)) -> true
                (configuration.relay8Enabled.enabled &&
                        isRelayAssociatedToHeatingStage(configuration.relay8Association)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToHeating(hssEquip: HyperStatSplitEquip): Boolean {
            return hssEquip.heatingStage1.pointExists() || hssEquip.heatingStage2.pointExists() || hssEquip.heatingStage3.pointExists()
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToCooling(configuration: HyperStatSplitCpuProfileConfiguration): Boolean {
            return when {
                (configuration.relay1Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay1Association)) -> true
                (configuration.relay2Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay2Association)) -> true
                (configuration.relay3Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay3Association)) -> true
                (configuration.relay4Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay4Association)) -> true
                (configuration.relay5Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay5Association)) -> true
                (configuration.relay6Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay6Association)) -> true
                (configuration.relay7Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay7Association)) -> true
                (configuration.relay8Enabled.enabled &&
                        isRelayAssociatedToCoolingStage(configuration.relay8Association)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyRelayEnabledAssociatedToCooling(hssEquip: HyperStatSplitEquip): Boolean {
            return hssEquip.coolingStage1.pointExists() || hssEquip.coolingStage2.pointExists() || hssEquip.coolingStage3.pointExists()
        }

        // Function which checks that any of the Analog Out is mapped to Cooling
        fun isAnyAnalogOutEnabledAssociatedToCooling(configuration: HyperStatSplitCpuProfileConfiguration): Boolean {
            return when {
                (configuration.analogOut1Enabled.enabled &&
                        configuration.analogOut1Association.associationVal.equals(CpuControlType.COOLING.ordinal)) -> true
                (configuration.analogOut2Enabled.enabled &&
                        configuration.analogOut2Association.associationVal.equals(CpuControlType.COOLING.ordinal)) -> true
                (configuration.analogOut3Enabled.enabled &&
                        configuration.analogOut3Association.associationVal.equals(CpuControlType.COOLING.ordinal)) -> true
                (configuration.analogOut4Enabled.enabled &&
                        configuration.analogOut4Association.associationVal.equals(CpuControlType.COOLING.ordinal)) -> true
                else -> false
            }
        }

        // Function which checks that any of the relay Enabled and is associated to cooling
        fun isAnyAnalogOutEnabledAssociatedToCooling(hssEquip: HyperStatSplitEquip): Boolean {
            return hssEquip.coolingSignal.pointExists()
        }

        // Function which checks that any of the Analog Out is mapped to Heating
        fun isAnyAnalogOutEnabledAssociatedToHeating(configuration: HyperStatSplitCpuProfileConfiguration): Boolean {
            return when {
                (configuration.analogOut1Enabled.enabled &&
                        configuration.analogOut1Association.associationVal.equals(CpuControlType.HEATING.ordinal)) -> true
                (configuration.analogOut2Enabled.enabled &&
                        configuration.analogOut2Association.associationVal.equals(CpuControlType.HEATING.ordinal)) -> true
                (configuration.analogOut3Enabled.enabled &&
                        configuration.analogOut3Association.associationVal.equals(CpuControlType.HEATING.ordinal)) -> true
                (configuration.analogOut4Enabled.enabled &&
                        configuration.analogOut4Association.associationVal.equals(CpuControlType.HEATING.ordinal)) -> true
                else -> false
            }
        }

        // Function which checks that any of the Analog Out is mapped to Fan speed
        fun isAnyAnalogOutEnabledAssociatedToFanSpeed(configuration: HyperStatSplitCpuProfileConfiguration): Boolean {
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
        fun isAnyAnalogOutEnabledAssociatedToHeating(hssEquip: HyperStatSplitEquip): Boolean {
            return hssEquip.heatingSignal.pointExists()
        }

        // Function returns highest selected cooling stage
        fun getHighestCoolingStage(configuration: HyperStatSplitCpuProfileConfiguration): CpuRelayType {
            var highestValue = 0
            highestValue = verifyCoolingState(configuration.relay1Enabled, configuration.relay1Association, highestValue)
            highestValue = verifyCoolingState(configuration.relay2Enabled, configuration.relay2Association, highestValue)
            highestValue = verifyCoolingState(configuration.relay3Enabled, configuration.relay3Association, highestValue)
            highestValue = verifyCoolingState(configuration.relay4Enabled, configuration.relay4Association, highestValue)
            highestValue = verifyCoolingState(configuration.relay5Enabled, configuration.relay5Association, highestValue)
            highestValue = verifyCoolingState(configuration.relay6Enabled, configuration.relay6Association, highestValue)
            highestValue = verifyCoolingState(configuration.relay7Enabled, configuration.relay7Association, highestValue)
            highestValue = verifyCoolingState(configuration.relay8Enabled, configuration.relay8Association, highestValue)

            return CpuRelayType.values()[highestValue]
        }

        private fun verifyCoolingState(enabled: EnableConfig, association: AssociationConfig, highestValue: Int): Int {
            if (enabled.enabled && isRelayAssociatedToCoolingStage(association)
                && association.associationVal > highestValue
            ) return association.associationVal
            return highestValue
        }

        fun getHighestHeatingStage(configuration: HyperStatSplitCpuProfileConfiguration): CpuRelayType {
            var highestValue = 0
            highestValue = verifyHeatingState(configuration.relay1Enabled, configuration.relay1Association, highestValue)
            highestValue = verifyHeatingState(configuration.relay2Enabled, configuration.relay2Association, highestValue)
            highestValue = verifyHeatingState(configuration.relay3Enabled, configuration.relay3Association, highestValue)
            highestValue = verifyHeatingState(configuration.relay4Enabled, configuration.relay4Association, highestValue)
            highestValue = verifyHeatingState(configuration.relay5Enabled, configuration.relay5Association, highestValue)
            highestValue = verifyHeatingState(configuration.relay6Enabled, configuration.relay6Association, highestValue)
            highestValue = verifyHeatingState(configuration.relay7Enabled, configuration.relay7Association, highestValue)
            highestValue = verifyHeatingState(configuration.relay8Enabled, configuration.relay8Association, highestValue)

            return CpuRelayType.values()[highestValue]
        }

        private fun verifyHeatingState(enabled: EnableConfig, association: AssociationConfig, highestValue: Int): Int {
            if (enabled.enabled && isRelayAssociatedToHeatingStage(association)
                && association.associationVal > highestValue
            )
                return association.associationVal
            return highestValue
        }

        /**
         * Determines the lowest fan stage based on the relay states in the given CPU configuration.
         *
         * @param configuration the CPU configuration to analyze.
         * @return the lowest fan stage, represented as a [CpuRelayType] value.
         */
        fun getLowestFanStage(configuration: HyperStatSplitCpuProfileConfiguration): CpuRelayType {
            var lowestValue = 0xFF
            lowestValue = verifyFanStateLowValue(configuration.relay1Enabled, configuration.relay1Association, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay2Enabled, configuration.relay2Association, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay3Enabled, configuration.relay3Association, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay4Enabled, configuration.relay4Association, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay5Enabled, configuration.relay5Association, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay6Enabled, configuration.relay6Association, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay7Enabled, configuration.relay7Association, lowestValue)
            lowestValue = verifyFanStateLowValue(configuration.relay8Enabled, configuration.relay8Association, lowestValue)

            return CpuRelayType.values().getOrNull(lowestValue) ?: CpuRelayType.DEHUMIDIFIER
        }

        /**
         * Verifies the lowest fan state value based on the given relay state and the current lowest value.
         *
         * @param state the relay state to verify.
         * @param lowestValue the current lowest value.
         * @return the updated lowest value, considering the relay state.
         */
        private fun verifyFanStateLowValue(enabled: EnableConfig, association: AssociationConfig, lowestValue: Int): Int {
            if (enabled.enabled && isRelayAssociatedToFan(association)
                && association.associationVal < lowestValue
            )
                return association.associationVal
            return lowestValue
        }

        fun getHighestFanStage(configuration: HyperStatSplitCpuProfileConfiguration): CpuRelayType {
            var highestValue = 0
            highestValue = verifyFanState(configuration.relay1Enabled, configuration.relay1Association, highestValue)
            highestValue = verifyFanState(configuration.relay2Enabled, configuration.relay2Association, highestValue)
            highestValue = verifyFanState(configuration.relay3Enabled, configuration.relay3Association, highestValue)
            highestValue = verifyFanState(configuration.relay4Enabled, configuration.relay4Association, highestValue)
            highestValue = verifyFanState(configuration.relay5Enabled, configuration.relay5Association, highestValue)
            highestValue = verifyFanState(configuration.relay6Enabled, configuration.relay6Association, highestValue)
            highestValue = verifyFanState(configuration.relay7Enabled, configuration.relay7Association, highestValue)
            highestValue = verifyFanState(configuration.relay8Enabled, configuration.relay8Association, highestValue)

            return CpuRelayType.values()[highestValue]
        }
        private fun verifyFanState(enabled: EnableConfig, association: AssociationConfig, highestValue: Int): Int {
            if (enabled.enabled && isRelayAssociatedToFan(association)
                && association.associationVal > highestValue
            )
                return association.associationVal
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

        fun isAnyAnalogOutMappedToStagedFan(
            hyperStatConfig: HyperStatSplitCpuProfileConfiguration,
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