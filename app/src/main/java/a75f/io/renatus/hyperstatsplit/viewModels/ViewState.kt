package a75f.io.renatus.hyperstatsplit.viewModels

import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.common.AnalogConfigState
import a75f.io.logic.bo.building.hyperstatsplit.common.ConfigState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.AnalogOutState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconRelayAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusPressAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.RelayState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusPressState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusTempState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.UniversalInAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.UniversalInState

import a75f.io.renatus.R
import android.util.Log

/**
 * Created for HyperStat by Manjunath K on 15-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

 data class ViewState(
    val profileType: ProfileType,
    val tempOffsetPosition: Int,
    val forceOccupiedEnabled: Boolean,
    val autoAwayEnabled: Boolean,
    val relays: List<ConfigState>,
    val analogOutUis: List<AnalogConfigState>,
    val universalIns: List<ConfigState>,
    val sensorBusTemps: List<ConfigState>,
    val sensorBusPress: List<ConfigState>,
    var outsideDamperMinOpenPos: Int,
    var exhaustFanStage1ThresholdPos: Int,
    var exhaustFanStage2ThresholdPos: Int,
    var exhaustFanHysteresisPos: Int,
    var zoneCO2DamperOpeningRatePos: Int,
    var zoneCO2ThresholdPos: Int,
    var zoneCO2TargetPos: Int,
    var zoneVocThresholdPos: Int,
    var zoneVocTargetPos: Int,
    var zonePm2p5TargetPos: Int,
    var isDisplayHumidityEnabled: Boolean,
    var isDisplayVOCEnabled: Boolean,
    var isDisplayPp2p5Enabled: Boolean,
    var isDisplayCo2Enabled: Boolean,
    var stagedFanUis: List<Int>
) {

    companion object {
        fun fromConfigTo(config: BaseProfileConfiguration, profileType: ProfileType): ViewState {

            when (profileType) {
                ProfileType.HYPERSTATSPLIT_CPU -> {
                    val configuration = config as HyperStatSplitCpuEconConfiguration
                    return fromConfigCPUEcon(configuration)
                }

                else -> {}
            }

            // Assume we don't have config
            return fromConfigCPUEcon(HyperStatSplitCpuEconConfiguration())
        }

        private fun fromConfigCPUEcon(config: HyperStatSplitCpuEconConfiguration) = ViewState(
            profileType = ProfileType.HYPERSTATSPLIT_CPU,
            tempOffsetPosition = (tempOffsetSpinnerValues().indexOf(config.temperatureOffset.toString())),
            forceOccupiedEnabled = config.isEnableAutoForceOccupied,
            autoAwayEnabled = config.isEnableAutoAway,
            relays = listOf(
                ConfigState(config.relay1State.enabled, config.relay1State.association.ordinal),
                ConfigState(config.relay2State.enabled, config.relay2State.association.ordinal),
                ConfigState(config.relay3State.enabled, config.relay3State.association.ordinal),
                ConfigState(config.relay4State.enabled, config.relay4State.association.ordinal),
                ConfigState(config.relay5State.enabled, config.relay5State.association.ordinal),
                ConfigState(config.relay6State.enabled, config.relay6State.association.ordinal),
                ConfigState(config.relay7State.enabled, config.relay7State.association.ordinal),
                ConfigState(config.relay8State.enabled, config.relay8State.association.ordinal)
            ),
            sensorBusTemps = listOf(
                ConfigState(config.address0State.enabled, config.address0State.association.ordinal),
                ConfigState(config.address1State.enabled, config.address1State.association.ordinal),
                ConfigState(config.address2State.enabled, config.address2State.association.ordinal),
            ),
            sensorBusPress = listOf(
                ConfigState(config.address3State.enabled, config.address3State.association.ordinal)
            ),
            analogOutUis = listOf(
                AnalogConfigState(
                    config.analogOut1State.enabled,
                    config.analogOut1State.association.ordinal,
                    config.analogOut1State.voltageAtMin,
                    config.analogOut1State.voltageAtMax,
                    config.analogOut1State.perAtFanLow,
                    config.analogOut1State.perAtFanMedium,
                    config.analogOut1State.perAtFanHigh,
                ),
                AnalogConfigState(
                    config.analogOut2State.enabled,
                    config.analogOut2State.association.ordinal,
                    config.analogOut2State.voltageAtMin,
                    config.analogOut2State.voltageAtMax,
                    config.analogOut2State.perAtFanLow,
                    config.analogOut2State.perAtFanMedium,
                    config.analogOut2State.perAtFanHigh,
                ),
                AnalogConfigState(
                    config.analogOut3State.enabled,
                    config.analogOut3State.association.ordinal,
                    config.analogOut3State.voltageAtMin,
                    config.analogOut3State.voltageAtMax,
                    config.analogOut3State.perAtFanLow,
                    config.analogOut3State.perAtFanMedium,
                    config.analogOut3State.perAtFanHigh,
                ),
                AnalogConfigState(
                    config.analogOut4State.enabled,
                    config.analogOut4State.association.ordinal,
                    config.analogOut4State.voltageAtMin,
                    config.analogOut4State.voltageAtMax,
                    config.analogOut4State.perAtFanLow,
                    config.analogOut4State.perAtFanMedium,
                    config.analogOut4State.perAtFanHigh,
                ),

                ),
            universalIns = listOf(
                ConfigState(
                    config.universalIn1State.enabled,
                    config.universalIn1State.association.ordinal
                ),
                ConfigState(
                    config.universalIn2State.enabled,
                    config.universalIn2State.association.ordinal
                ),
                ConfigState(
                    config.universalIn3State.enabled,
                    config.universalIn3State.association.ordinal
                ),
                ConfigState(
                    config.universalIn4State.enabled,
                    config.universalIn4State.association.ordinal
                ),
                ConfigState(
                    config.universalIn5State.enabled,
                    config.universalIn5State.association.ordinal
                ),
                ConfigState(
                    config.universalIn6State.enabled,
                    config.universalIn6State.association.ordinal
                ),
                ConfigState(
                    config.universalIn7State.enabled,
                    config.universalIn7State.association.ordinal
                ),
                ConfigState(
                    config.universalIn8State.enabled,
                    config.universalIn8State.association.ordinal
                ),
            ),
            outsideDamperMinOpenPos = outsideDamperMinOpenSetIndexFromValue(config.outsideDamperMinOpen),
            exhaustFanStage1ThresholdPos = exhaustFanStage1ThresholdSetIndexFromValue(config.exhaustFanStage1Threshold),
            exhaustFanStage2ThresholdPos = exhaustFanStage2ThresholdSetIndexFromValue(config.exhaustFanStage2Threshold),
            exhaustFanHysteresisPos = exhaustFanHysteresisSetIndexFromValue(config.exhaustFanHysteresis),
            zoneCO2DamperOpeningRatePos = co2DCVOpeningDamperSetIndexFromValue(config.zoneCO2DamperOpeningRate),
            zoneCO2ThresholdPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Threshold),
            zoneCO2TargetPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Target),
            zoneVocThresholdPos = vocSetIndexFromValue(config.zoneVOCThreshold),
            zoneVocTargetPos = vocSetIndexFromValue(config.zoneVOCTarget),
            zonePm2p5TargetPos = pmSetIndexFromValue(config.zonePm2p5Target),
            isDisplayHumidityEnabled = config.displayHumidity,
            isDisplayCo2Enabled = config.displayCo2,
            isDisplayVOCEnabled = config.displayVOC,
            isDisplayPp2p5Enabled = config.displayPp2p5,
            stagedFanUis = listOf(
                config.coolingStage1FanState, config.coolingStage2FanState, config.coolingStage3FanState,
                config.heatingStage1FanState, config.heatingStage2FanState, config.heatingStage3FanState
            )
        )

    }

    fun toConfig(): BaseProfileConfiguration {
        when (profileType) {
            ProfileType.HYPERSTATSPLIT_CPU -> return toCpuEconConfig()
            else -> {}
        }
        return toCpuEconConfig()
    }

    private fun toCpuEconConfig(): HyperStatSplitCpuEconConfiguration {
        return HyperStatSplitCpuEconConfiguration().apply {

            temperatureOffset = tempOffsetSpinnerValues()[(tempOffsetPosition)]!!.toDouble()
            isEnableAutoForceOccupied = forceOccupiedEnabled
            isEnableAutoAway = autoAwayEnabled
            relay1State = RelayState(
                relays[0].enabled,
                CpuEconRelayAssociation.values()[relays[0].association]
            )
            relay2State = RelayState(
                relays[1].enabled,
                CpuEconRelayAssociation.values()[relays[1].association]
            )
            relay3State = RelayState(
                relays[2].enabled,
                CpuEconRelayAssociation.values()[relays[2].association]
            )
            relay4State = RelayState(
                relays[3].enabled,
                CpuEconRelayAssociation.values()[relays[3].association]
            )
            relay5State = RelayState(
                relays[4].enabled,
                CpuEconRelayAssociation.values()[relays[4].association]
            )
            relay6State = RelayState(
                relays[5].enabled,
                CpuEconRelayAssociation.values()[relays[5].association]
            )
            relay7State = RelayState(
                relays[6].enabled,
                CpuEconRelayAssociation.values()[relays[6].association]
            )
            relay8State = RelayState(
                relays[7].enabled,
                CpuEconRelayAssociation.values()[relays[7].association]
            )

            analogOut1State = AnalogOutState(
                analogOutUis[0].enabled,
                CpuEconAnalogOutAssociation.values()[analogOutUis[0].association],
                analogOutUis[0].voltageAtMin,
                analogOutUis[0].voltageAtMax,
                analogOutUis[0].perAtFanLow,
                analogOutUis[0].perAtFanMedium,
                analogOutUis[0].perAtFanHigh
            )
            analogOut2State = AnalogOutState(
                analogOutUis[1].enabled,
                CpuEconAnalogOutAssociation.values()[analogOutUis[1].association],
                analogOutUis[1].voltageAtMin,
                analogOutUis[1].voltageAtMax,
                analogOutUis[1].perAtFanLow,
                analogOutUis[1].perAtFanMedium,
                analogOutUis[1].perAtFanHigh
            )
            analogOut3State = AnalogOutState(
                analogOutUis[2].enabled,
                CpuEconAnalogOutAssociation.values()[analogOutUis[2].association],
                analogOutUis[2].voltageAtMin,
                analogOutUis[2].voltageAtMax,
                analogOutUis[2].perAtFanLow,
                analogOutUis[2].perAtFanMedium,
                analogOutUis[2].perAtFanHigh
            )
            analogOut4State = AnalogOutState(
                analogOutUis[3].enabled,
                CpuEconAnalogOutAssociation.values()[analogOutUis[3].association],
                analogOutUis[3].voltageAtMin,
                analogOutUis[3].voltageAtMax,
                analogOutUis[3].perAtFanLow,
                analogOutUis[3].perAtFanMedium,
                analogOutUis[3].perAtFanHigh
            )

            address0State = SensorBusTempState(sensorBusTemps[0].enabled, CpuEconSensorBusTempAssociation.values()[sensorBusTemps[0].association])
            address1State = SensorBusTempState(sensorBusTemps[1].enabled, CpuEconSensorBusTempAssociation.values()[sensorBusTemps[1].association])
            address2State = SensorBusTempState(sensorBusTemps[2].enabled, CpuEconSensorBusTempAssociation.values()[sensorBusTemps[2].association])
            address3State = SensorBusPressState(sensorBusPress[0].enabled, CpuEconSensorBusPressAssociation.values()[sensorBusPress[0].association])

            universalIn1State = UniversalInState(
                universalIns[0].enabled,
                UniversalInAssociation.values()[universalIns[0].association]
            )
            universalIn2State = UniversalInState(
                universalIns[1].enabled,
                UniversalInAssociation.values()[universalIns[1].association]
            )
            universalIn3State = UniversalInState(
                universalIns[2].enabled,
                UniversalInAssociation.values()[universalIns[2].association]
            )
            universalIn4State = UniversalInState(
                universalIns[3].enabled,
                UniversalInAssociation.values()[universalIns[3].association]
            )
            universalIn5State = UniversalInState(
                universalIns[4].enabled,
                UniversalInAssociation.values()[universalIns[4].association]
            )
            universalIn6State = UniversalInState(
                universalIns[5].enabled,
                UniversalInAssociation.values()[universalIns[5].association]
            )
            universalIn7State = UniversalInState(
                universalIns[6].enabled,
                UniversalInAssociation.values()[universalIns[6].association]
            )
            universalIn8State = UniversalInState(
                universalIns[7].enabled,
                UniversalInAssociation.values()[universalIns[7].association]
            )

            coolingStage1FanState = stagedFanUis[0]
            coolingStage2FanState = stagedFanUis[1]
            coolingStage3FanState = stagedFanUis[2]
            heatingStage1FanState = stagedFanUis[3]
            heatingStage2FanState = stagedFanUis[4]
            heatingStage3FanState = stagedFanUis[5]

            outsideDamperMinOpen = outsideDamperMinOpenValueFromIndex(outsideDamperMinOpenPos)
            exhaustFanStage1Threshold = exhaustFanStage1ThresholdValueFromIndex(exhaustFanStage1ThresholdPos)
            exhaustFanStage2Threshold = exhaustFanStage2ThresholdValueFromIndex(exhaustFanStage2ThresholdPos)
            exhaustFanHysteresis = exhaustFanHysteresisFromIndex(exhaustFanHysteresisPos)

            zoneCO2DamperOpeningRate = co2DCVDamperValueFromIndex(zoneCO2DamperOpeningRatePos)
            zoneCO2Threshold = openingDamperValueFromIndex(zoneCO2ThresholdPos)
            zoneCO2Target = openingDamperValueFromIndex(zoneCO2TargetPos)
            zoneVOCThreshold = vocValueFromIndex(zoneVocThresholdPos)
            zoneVOCTarget = vocValueFromIndex(zoneVocTargetPos)
            zonePm2p5Target = pm25ValueFromIndex(zonePm2p5TargetPos)
            displayHumidity = isDisplayHumidityEnabled
            displayCo2 = isDisplayCo2Enabled
            displayVOC = isDisplayVOCEnabled
            displayPp2p5 = isDisplayPp2p5Enabled
        }

    }

}

// CPU & Econ Specific
val CpuEconRelayAssociation.displayName: Int
    get() {
        return when (this) {
            CpuEconRelayAssociation.COOLING_STAGE_1 -> R.string.cooling_stage_1
            CpuEconRelayAssociation.COOLING_STAGE_2 -> R.string.cooling_stage_2
            CpuEconRelayAssociation.COOLING_STAGE_3 -> R.string.cooling_stage_3
            CpuEconRelayAssociation.HEATING_STAGE_1 -> R.string.heating_stage_1
            CpuEconRelayAssociation.HEATING_STAGE_2 -> R.string.heating_stage_2
            CpuEconRelayAssociation.HEATING_STAGE_3 -> R.string.heating_stage_3
            CpuEconRelayAssociation.FAN_LOW_SPEED -> R.string.fan_low_speed
            CpuEconRelayAssociation.FAN_MEDIUM_SPEED -> R.string.fan_medium_speed
            CpuEconRelayAssociation.FAN_HIGH_SPEED -> R.string.fan_high_speed
            CpuEconRelayAssociation.FAN_ENABLED -> R.string.fan_enabled
            CpuEconRelayAssociation.OCCUPIED_ENABLED -> R.string.occupied_enabled
            CpuEconRelayAssociation.HUMIDIFIER -> R.string.humidifier
            CpuEconRelayAssociation.DEHUMIDIFIER -> R.string.dehumidifier
            CpuEconRelayAssociation.EXHAUST_FAN_STAGE_1 -> R.string.exhaust_fan_stage_1
            CpuEconRelayAssociation.EXHAUST_FAN_STAGE_2 -> R.string.exhaust_fan_stage_2
        }
    }

val CpuEconAnalogOutAssociation.displayName: Int
    get() {
        return when (this) {
            CpuEconAnalogOutAssociation.COOLING -> R.string.cooling
            CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED -> R.string.linear_fan_speed
            CpuEconAnalogOutAssociation.HEATING -> R.string.heating
            CpuEconAnalogOutAssociation.OAO_DAMPER -> R.string.oao_damper
            CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED -> R.string.staged_fan_speed
        }
    }

val UniversalInAssociation.displayName: Int
    get() {
        return when (this) {
            UniversalInAssociation.SUPPLY_AIR_TEMPERATURE -> R.string.supply_air_temperature
            UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE -> R.string.outside_air_temperature
            UniversalInAssociation.MIXED_AIR_TEMPERATURE -> R.string.mixed_air_temperature
            UniversalInAssociation.FILTER_NC -> R.string.filter_nc
            UniversalInAssociation.FILTER_NO -> R.string.filter_no
            UniversalInAssociation.CONDENSATE_NC -> R.string.condensate_nc
            UniversalInAssociation.CONDENSATE_NO -> R.string.condensate_no
            UniversalInAssociation.CURRENT_TX_0_10 -> R.string.current_tx_0_10
            UniversalInAssociation.CURRENT_TX_0_20 -> R.string.current_tx_0_20
            UniversalInAssociation.CURRENT_TX_0_50 -> R.string.current_tx_0_50
            UniversalInAssociation.CURRENT_TX_0_100 -> R.string.current_tx_0_100
            UniversalInAssociation.CURRENT_TX_0_150 -> R.string.current_tx_0_150
            UniversalInAssociation.DUCT_PRESSURE_0_1 -> R.string.duct_pressure_0_1
            UniversalInAssociation.DUCT_PRESSURE_0_2 -> R.string.duct_pressure_0_2
            UniversalInAssociation.GENERIC_VOLTAGE -> R.string.generic_voltage
            UniversalInAssociation.GENERIC_RESISTANCE -> R.string.generic_resistance
        }
    }

// an extension function for the list update functionality.
fun <E> Iterable<E>.updated(index: Int, elem: E) =
    mapIndexed { i, existing -> if (i == index) elem else existing }


fun getAnalogOutDisplayName(profileType: ProfileType, enumValue: Int): Int {
    when (profileType) {
        ProfileType.HYPERSTATSPLIT_CPU -> {
            when (enumValue) {
                CpuEconAnalogOutAssociation.COOLING.ordinal -> return R.string.cooling
                CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED.ordinal -> return R.string.linear_fan_speed
                CpuEconAnalogOutAssociation.HEATING.ordinal -> return R.string.heating
                CpuEconAnalogOutAssociation.OAO_DAMPER.ordinal -> return R.string.oao_damper
                CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal -> return R.string.staged_fan_speed
            }
        }

        else -> {}
    }
    return -1
}



