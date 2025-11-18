package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common

import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.equips.unitVentilator.Pipe2UVEquip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.CpuSensorBusType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.SensorTempHumidityAssociationConfig
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

        fun isAnySensorBusAddressMappedToOutsideAir(
            config: HyperStatSplitConfiguration
        ): Boolean {
            return config.address0Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address0SensorAssociation)
                    || config.address1Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address1SensorAssociation)
                    || config.address2Enabled.enabled && isSensorBusAddressAssociatedToOutsideAir(config.address2SensorAssociation)
        }


        fun getHssProfileFanLevel(
            equip: HyperStatSplitEquip,
        ): Int {
            return when (equip) {
                is HsSplitCpuEquip , is Pipe4UVEquip , is Pipe2UVEquip -> getEconSelectedFanLevel(equip)
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

            if (fanLevel == 0 && (hssEquip.fanEnable.pointExists() || hssEquip.occupiedEnable.pointExists())) {
                fanLevel = 1
            }

            return fanLevel
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

    }
}