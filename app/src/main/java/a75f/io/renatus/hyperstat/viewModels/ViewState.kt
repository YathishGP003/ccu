package a75f.io.renatus.hyperstat.viewModels

import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.common.AnalogConfigState
import a75f.io.logic.bo.building.hyperstat.common.ConfigState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.*
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.*
import a75f.io.renatus.R

/**
 * Created by Manjunath K on 15-07-2022.
 */

 data class ViewState(
    val profileType: ProfileType,
    val tempOffsetPosition: Int,
    val forceOccupiedEnabled: Boolean,
    val autoAwayEnabled: Boolean,
    val relays: List<ConfigState>,
    val analogOutUis: List<AnalogConfigState>,
    val airflowTempSensorEnabled: Boolean,
    val th2Enabled: Boolean,
    val analogIns: List<ConfigState>,
    var zoneCO2DamperOpeningRatePos: Int,
    var zoneCO2ThresholdPos: Int,
    var zoneCO2TargetPos: Int,
    var zoneVocThresholdPos: Int,
    var zoneVocTargetPos: Int,
    var zonePm2p5ThresholdPos: Int,
    var zonePm2p5TargetPos: Int,
    var isDisplayHumidityEnabled: Boolean,
    var isDisplayVOCEnabled: Boolean,
    var isDisplayPp2p5Enabled: Boolean,
    var isDisplayCo2Enabled: Boolean,
) {

    companion object {
        fun fromConfigTo(config: BaseProfileConfiguration, profileType: ProfileType): ViewState {

            when(profileType){
                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                    val configuration = config as HyperStatCpuConfiguration
                    return fromConfigCPU(configuration)
                }
                ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
                    val configuration = config as HyperStatPipe2Configuration
                    return fromConfigPipe2(configuration)
                }

                else -> {}
            }

            // Assume we dont have config
            return fromConfigCPU(HyperStatCpuConfiguration())
        }

        private fun fromConfigCPU(config: HyperStatCpuConfiguration) = ViewState(
            profileType = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
            tempOffsetPosition = (tempOffsetSpinnerValues().indexOf(config.temperatureOffset.toString())),
            forceOccupiedEnabled = config.isEnableAutoForceOccupied,
            autoAwayEnabled = config.isEnableAutoAway,
            relays = listOf(
                ConfigState(config.relay1State.enabled,config.relay1State.association.ordinal),
                ConfigState(config.relay2State.enabled,config.relay2State.association.ordinal),
                ConfigState(config.relay3State.enabled,config.relay3State.association.ordinal),
                ConfigState(config.relay4State.enabled,config.relay4State.association.ordinal),
                ConfigState(config.relay5State.enabled,config.relay5State.association.ordinal),
                ConfigState(config.relay6State.enabled,config.relay6State.association.ordinal)
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

                ),
            airflowTempSensorEnabled = config.isEnableAirFlowTempSensor,
            th2Enabled = config.isEnableDoorWindowSensor,
            analogIns = listOf(
                ConfigState(config.analogIn1State.enabled,config.analogIn1State.association.ordinal),
                ConfigState(config.analogIn2State.enabled,config.analogIn2State.association.ordinal),
            ),
            zoneCO2DamperOpeningRatePos = co2DCVOpeningDamperSetIndexFromValue(config.zoneCO2DamperOpeningRate),
            zoneCO2ThresholdPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Threshold),
            zoneCO2TargetPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Target),
            zoneVocThresholdPos =  vocSetIndexFromValue(config.zoneVOCThreshold),
            zoneVocTargetPos =  vocSetIndexFromValue(config.zoneVOCTarget),
            zonePm2p5ThresholdPos =  pmSetIndexFromValue(config.zonePm2p5Threshold),
            zonePm2p5TargetPos =  pmSetIndexFromValue(config.zonePm2p5Target),
            isDisplayHumidityEnabled = config.displayHumidity,
            isDisplayCo2Enabled = config.displayCo2,
            isDisplayVOCEnabled = config.displayVOC,
            isDisplayPp2p5Enabled = config.displayPp2p5
        )

        private fun fromConfigPipe2(config: HyperStatPipe2Configuration) = ViewState(
            profileType = ProfileType.HYPERSTAT_TWO_PIPE_FCU,
            tempOffsetPosition = (tempOffsetSpinnerValues().indexOf(config.temperatureOffset.toString())),
            forceOccupiedEnabled = config.isEnableAutoForceOccupied,
            autoAwayEnabled = config.isEnableAutoAway,
            relays = listOf(
                ConfigState(config.relay1State.enabled,config.relay1State.association.ordinal),
                ConfigState(config.relay2State.enabled,config.relay2State.association.ordinal),
                ConfigState(config.relay3State.enabled,config.relay3State.association.ordinal),
                ConfigState(config.relay4State.enabled,config.relay4State.association.ordinal),
                ConfigState(config.relay5State.enabled,config.relay5State.association.ordinal),
                ConfigState(config.relay6State.enabled,config.relay6State.association.ordinal)
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

                ),
            airflowTempSensorEnabled = config.isEnableAirFlowTempSensor,
            th2Enabled = config.isSupplyWaterSensor,
            analogIns = listOf(
                ConfigState(config.analogIn1State.enabled,config.analogIn1State.association.ordinal),
                ConfigState(config.analogIn2State.enabled,config.analogIn2State.association.ordinal),
            ),
            zoneCO2DamperOpeningRatePos = co2DCVOpeningDamperSetIndexFromValue(config.zoneCO2DamperOpeningRate),
            zoneCO2ThresholdPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Threshold),
            zoneCO2TargetPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Target),
            zoneVocThresholdPos =  vocSetIndexFromValue(config.zoneVOCThreshold),
            zoneVocTargetPos =  vocSetIndexFromValue(config.zoneVOCTarget),
            zonePm2p5ThresholdPos =  pmSetIndexFromValue(config.zonePm2p5Threshold),
            zonePm2p5TargetPos =  pmSetIndexFromValue(config.zonePm2p5Target),
            isDisplayHumidityEnabled = config.displayHumidity,
            isDisplayCo2Enabled = config.displayCo2,
            isDisplayVOCEnabled = config.displayVOC,
            isDisplayPp2p5Enabled = config.displayPp2p5
        )

    }


    fun toConfig(): BaseProfileConfiguration{
        when(profileType){
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT-> return toCpuConfig()
            ProfileType.HYPERSTAT_TWO_PIPE_FCU-> return to2PipeConfig()
            else -> {}
        }
        return toCpuConfig()
    }

    private fun toCpuConfig(): HyperStatCpuConfiguration {
        return HyperStatCpuConfiguration().apply {
            temperatureOffset = tempOffsetSpinnerValues()[(tempOffsetPosition)]!!.toDouble()
            isEnableAutoForceOccupied = forceOccupiedEnabled
            isEnableAutoAway = autoAwayEnabled
            isEnableAirFlowTempSensor = airflowTempSensorEnabled
            isEnableDoorWindowSensor = th2Enabled
            relay1State = RelayState(relays[0].enabled,CpuRelayAssociation.values()[relays[0].association])
            relay2State = RelayState(relays[1].enabled,CpuRelayAssociation.values()[relays[1].association])
            relay3State = RelayState(relays[2].enabled,CpuRelayAssociation.values()[relays[2].association])
            relay4State = RelayState(relays[3].enabled,CpuRelayAssociation.values()[relays[3].association])
            relay5State = RelayState(relays[4].enabled,CpuRelayAssociation.values()[relays[4].association])
            relay6State = RelayState(relays[5].enabled,CpuRelayAssociation.values()[relays[5].association])

            analogOut1State = AnalogOutState(
                analogOutUis[0].enabled,
                CpuAnalogOutAssociation.values()[analogOutUis[0].association],
                analogOutUis[0].voltageAtMin,analogOutUis[0].voltageAtMax,
                analogOutUis[0].perAtFanLow,analogOutUis[0].perAtFanMedium,analogOutUis[0].perAtFanHigh
            )
            analogOut2State = AnalogOutState(
                analogOutUis[1].enabled,
                CpuAnalogOutAssociation.values()[analogOutUis[1].association],
                analogOutUis[1].voltageAtMin,analogOutUis[1].voltageAtMax,
                analogOutUis[1].perAtFanLow,analogOutUis[1].perAtFanMedium,analogOutUis[1].perAtFanHigh
            )
            analogOut3State = AnalogOutState(
                analogOutUis[2].enabled,
                CpuAnalogOutAssociation.values()[analogOutUis[2].association],
                analogOutUis[2].voltageAtMin,analogOutUis[2].voltageAtMax,
                analogOutUis[2].perAtFanLow,analogOutUis[2].perAtFanMedium,analogOutUis[2].perAtFanHigh
            )
            analogIn1State = AnalogInState(analogIns[0].enabled,AnalogInAssociation.values()[analogIns[0].association])
            analogIn2State = AnalogInState(analogIns[1].enabled,AnalogInAssociation.values()[analogIns[1].association])

            zoneCO2DamperOpeningRate = co2DCVDamperValueFromIndex(zoneCO2DamperOpeningRatePos)
            zoneCO2Threshold = openingDamperValueFromIndex(zoneCO2ThresholdPos)
            zoneCO2Target = openingDamperValueFromIndex(zoneCO2TargetPos)
            zoneVOCThreshold = vocValueFromIndex(zoneVocThresholdPos)
            zoneVOCTarget = vocValueFromIndex(zoneVocTargetPos)
            zonePm2p5Threshold = pm25ValueFromIndex(zonePm2p5ThresholdPos)
            zonePm2p5Target = pm25ValueFromIndex(zonePm2p5TargetPos)
            displayHumidity = isDisplayHumidityEnabled
            displayCo2 = isDisplayCo2Enabled
            displayVOC = isDisplayVOCEnabled
            displayPp2p5 = isDisplayPp2p5Enabled
        }
    }



    private fun to2PipeConfig(): HyperStatPipe2Configuration {
        return HyperStatPipe2Configuration().apply {
            temperatureOffset = tempOffsetSpinnerValues()[(tempOffsetPosition)]!!.toDouble()
            isEnableAutoForceOccupied = forceOccupiedEnabled
            isEnableAutoAway = autoAwayEnabled
            isEnableAirFlowTempSensor = airflowTempSensorEnabled
            isSupplyWaterSensor = th2Enabled
            relay1State = Pipe2RelayState(relays[0].enabled,Pipe2RelayAssociation.values()[relays[0].association])
            relay2State = Pipe2RelayState(relays[1].enabled,Pipe2RelayAssociation.values()[relays[1].association])
            relay3State = Pipe2RelayState(relays[2].enabled,Pipe2RelayAssociation.values()[relays[2].association])
            relay4State = Pipe2RelayState(relays[3].enabled,Pipe2RelayAssociation.values()[relays[3].association])
            relay5State = Pipe2RelayState(relays[4].enabled,Pipe2RelayAssociation.values()[relays[4].association])
            relay6State = Pipe2RelayState(relays[5].enabled,Pipe2RelayAssociation.values()[relays[5].association])


            analogOut1State = Pipe2AnalogOutState(
                analogOutUis[0].enabled,
                Pipe2AnalogOutAssociation.values()[analogOutUis[0].association],
                analogOutUis[0].voltageAtMin,analogOutUis[0].voltageAtMax,
                analogOutUis[0].perAtFanLow,analogOutUis[0].perAtFanMedium,analogOutUis[0].perAtFanHigh
            )
            analogOut2State = Pipe2AnalogOutState(
                analogOutUis[1].enabled,
                Pipe2AnalogOutAssociation.values()[analogOutUis[1].association],
                analogOutUis[1].voltageAtMin,analogOutUis[1].voltageAtMax,
                analogOutUis[1].perAtFanLow,analogOutUis[1].perAtFanMedium,analogOutUis[1].perAtFanHigh
            )
            analogOut3State = Pipe2AnalogOutState(
                analogOutUis[2].enabled,
                Pipe2AnalogOutAssociation.values()[analogOutUis[2].association],
                analogOutUis[2].voltageAtMin,analogOutUis[2].voltageAtMax,
                analogOutUis[2].perAtFanLow,analogOutUis[2].perAtFanMedium,analogOutUis[2].perAtFanHigh
            )
            analogIn1State = AnalogInState(analogIns[0].enabled,AnalogInAssociation.values()[analogIns[0].association])
            analogIn2State = AnalogInState(analogIns[1].enabled,AnalogInAssociation.values()[analogIns[1].association])

            zoneCO2DamperOpeningRate = co2DCVDamperValueFromIndex(zoneCO2DamperOpeningRatePos)
            zoneCO2Threshold = openingDamperValueFromIndex(zoneCO2ThresholdPos)
            zoneCO2Target = openingDamperValueFromIndex(zoneCO2TargetPos)
            zoneVOCThreshold = vocValueFromIndex(zoneVocThresholdPos)
            zoneVOCTarget = vocValueFromIndex(zoneVocTargetPos)
            zonePm2p5Threshold = pm25ValueFromIndex(zonePm2p5ThresholdPos)
            zonePm2p5Target = pm25ValueFromIndex(zonePm2p5TargetPos)
            displayHumidity = isDisplayHumidityEnabled
            displayCo2 = isDisplayCo2Enabled
            displayVOC = isDisplayVOCEnabled
            displayPp2p5 = isDisplayPp2p5Enabled
        }
    }

}


// CPU Specific

val CpuRelayAssociation.displayName: Int
    get() {
        return when (this) {
            CpuRelayAssociation.COOLING_STAGE_1 -> R.string.cooling_stage_1
            CpuRelayAssociation.COOLING_STAGE_2 -> R.string.cooling_stage_2
            CpuRelayAssociation.COOLING_STAGE_3 -> R.string.cooling_stage_3
            CpuRelayAssociation.HEATING_STAGE_1 -> R.string.heating_stage_1
            CpuRelayAssociation.HEATING_STAGE_2 -> R.string.heating_stage_2
            CpuRelayAssociation.HEATING_STAGE_3 -> R.string.heating_stage_3
            CpuRelayAssociation.FAN_LOW_SPEED -> R.string.fan_low_speed
            CpuRelayAssociation.FAN_MEDIUM_SPEED -> R.string.fan_medium_speed
            CpuRelayAssociation.FAN_HIGH_SPEED -> R.string.fan_high_speed
            CpuRelayAssociation.FAN_ENABLED -> R.string.fan_enabled
            CpuRelayAssociation.OCCUPIED_ENABLED -> R.string.occupied_enabled
            CpuRelayAssociation.HUMIDIFIER -> R.string.humidifier
            CpuRelayAssociation.DEHUMIDIFIER -> R.string.dehumidifier
        }
    }

val CpuAnalogOutAssociation.displayName: Int
    get() {
        return when (this) {
            CpuAnalogOutAssociation.COOLING -> R.string.cooling
            CpuAnalogOutAssociation.FAN_SPEED -> R.string.fan_speed
            CpuAnalogOutAssociation.HEATING -> R.string.heating
            CpuAnalogOutAssociation.DCV_DAMPER -> R.string.dcv_damper
        }
    }

val AnalogInAssociation.displayName: Int
    get() {
        return when (this) {
            AnalogInAssociation.CURRENT_TX_0_10 -> R.string.current_tx_0_10
            AnalogInAssociation.CURRENT_TX_0_20 -> R.string.current_tx_0_20
            AnalogInAssociation.CURRENT_TX_0_50 -> R.string.current_tx_0_50
            AnalogInAssociation.KEY_CARD_SENSOR -> R.string.key_card_sensor
            AnalogInAssociation.DOOR_WINDOW_SENSOR -> R.string.door_window_sensor_ai
        }
    }

data class CpuViewState(
    val tempOffsetPosition: Int,
    val forceOccupiedEnabled: Boolean,
    val autoAwayEnabled: Boolean,
    val relays: List<RelayState>,
    val analogOutUis: List<AnalogOutUiState>,
    val airflowTempSensorEnabled: Boolean,
    val doorWindowSensor1Enabled: Boolean,
    val analogIns: List<AnalogInState>,
    var zoneCO2DamperOpeningRatePos: Int,
    var zoneCO2ThresholdPos: Int,
    var zoneCO2TargetPos: Int,
    var zoneVocThresholdPos: Int,
    var zoneVocTargetPos: Int,
    var zonePm2p5ThresholdPos: Int,
    var zonePm2p5TargetPos: Int,

    ) {

    companion object {
        fun fromConfig(config: HyperStatCpuConfiguration) = CpuViewState(
            tempOffsetPosition = (tempOffsetSpinnerValues().indexOf(config.temperatureOffset.toString())),
            forceOccupiedEnabled = config.isEnableAutoForceOccupied,
            autoAwayEnabled = config.isEnableAutoAway,
            relays = listOf(
                config.relay1State,
                config.relay2State,
                config.relay3State,
                config.relay4State,
                config.relay5State,
                config.relay6State
            ),
            analogOutUis = listOf(
                AnalogOutUiState.from(config.analogOut1State),
                AnalogOutUiState.from(config.analogOut2State),
                AnalogOutUiState.from(config.analogOut3State)
            ),
            airflowTempSensorEnabled = config.isEnableAirFlowTempSensor,
            doorWindowSensor1Enabled = config.isEnableDoorWindowSensor,
            analogIns = listOf(config.analogIn1State, config.analogIn2State),
            zoneCO2DamperOpeningRatePos = co2DCVOpeningDamperSetIndexFromValue(config.zoneCO2DamperOpeningRate),
            zoneCO2ThresholdPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Threshold),
            zoneCO2TargetPos = co2DCVDamperSetIndexFromValue(config.zoneCO2Target),
            zoneVocThresholdPos =  vocSetIndexFromValue(config.zoneVOCThreshold),
            zoneVocTargetPos =  vocSetIndexFromValue(config.zoneVOCTarget),
            zonePm2p5ThresholdPos =  pmSetIndexFromValue(config.zonePm2p5Threshold),
            zonePm2p5TargetPos =  pmSetIndexFromValue(config.zonePm2p5Target)

        )
    }

    /**
     * Function which returns HyperStatCpuConfiguration configuration with existing configurations
     */
    fun toConfig(): HyperStatCpuConfiguration {
        return HyperStatCpuConfiguration().apply {
            temperatureOffset = tempOffsetSpinnerValues()[(tempOffsetPosition)]!!.toDouble()
            isEnableAutoForceOccupied = forceOccupiedEnabled
            isEnableAutoAway = autoAwayEnabled
            isEnableAirFlowTempSensor = airflowTempSensorEnabled
            isEnableDoorWindowSensor = doorWindowSensor1Enabled
            relay1State = relays[0]
            relay2State = relays[1]
            relay3State = relays[2]
            relay4State = relays[3]
            relay5State = relays[4]
            relay6State = relays[5]
            analogOut1State = analogOutUis[0].toOutState()
            analogOut2State = analogOutUis[1].toOutState()
            analogOut3State = analogOutUis[2].toOutState()
            analogIn1State = analogIns[0]
            analogIn2State = analogIns[1]
            zoneCO2DamperOpeningRate = co2DCVDamperValueFromIndex(zoneCO2DamperOpeningRatePos)
            zoneCO2Threshold = openingDamperValueFromIndex(zoneCO2ThresholdPos)
            zoneCO2Target = openingDamperValueFromIndex(zoneCO2TargetPos)
            zoneVOCThreshold = vocValueFromIndex(zoneVocThresholdPos)
            zoneVOCTarget = vocValueFromIndex(zoneVocTargetPos)
            zonePm2p5Threshold = pm25ValueFromIndex(zonePm2p5ThresholdPos)
            zonePm2p5Target = pm25ValueFromIndex(zonePm2p5TargetPos)
        }
    }
}

data class AnalogOutUiState(
    val enabled: Boolean,
    val association: CpuAnalogOutAssociation,
    val testSignalPosition: Int,
    val vAtMinDamperPosition: Int,   // create position to value mapping here.
    val vAtMaxDamperPosition: Int,    // create position to value mapping here.
    val perAtFanLowPosition: Int,    // create position to value mapping here.
    val perAtFanMediumPosition: Int,    // create position to value mapping here.
    val perAtFanHighPosition: Int,    // create position to value mapping here.
) {
    companion object {
        fun from(outState: AnalogOutState) = AnalogOutUiState(
            enabled = outState.enabled,
            association = outState.association,
            testSignalPosition = 0,
            vAtMinDamperPosition = analogVoltageIndexFromValue(outState.voltageAtMin),
            vAtMaxDamperPosition = analogVoltageIndexFromValue(outState.voltageAtMax),
            perAtFanLowPosition = analogFanSpeedIndexFromValue(outState.perAtFanLow),
            perAtFanMediumPosition = analogFanSpeedIndexFromValue(outState.perAtFanMedium),
            perAtFanHighPosition = analogFanSpeedIndexFromValue(outState.perAtFanHigh)
        )
    }

    fun toOutState() = AnalogOutState(
        enabled = enabled,
        association = association,
        voltageAtMin = analogVoltageFromIndex(vAtMinDamperPosition),
        voltageAtMax = analogVoltageFromIndex(vAtMaxDamperPosition),
        perAtFanLow = analogFanSpeedFromIndex(perAtFanLowPosition),
        perAtFanMedium = analogFanSpeedFromIndex(perAtFanMediumPosition),
        perAtFanHigh = analogFanSpeedFromIndex(perAtFanHighPosition)
    )
}

// an extenstion function for the list update functionality.
fun <E> Iterable<E>.updated(index: Int, elem: E) = mapIndexed { i, existing -> if (i == index) elem else existing }


fun getAnalogOutDisplayName(profileType: ProfileType, enumValue: Int): Int{
    when(profileType){
        ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT ->{
            when (enumValue) {
                CpuAnalogOutAssociation.COOLING.ordinal-> return R.string.cooling
                CpuAnalogOutAssociation.FAN_SPEED.ordinal -> return R.string.fan_speed
                CpuAnalogOutAssociation.HEATING.ordinal -> return R.string.heating
                CpuAnalogOutAssociation.DCV_DAMPER.ordinal -> return R.string.dcv_damper
            }
        }
        ProfileType.HYPERSTAT_TWO_PIPE_FCU ->{
            when (enumValue) {
                Pipe2AnalogOutAssociation.FAN_SPEED.ordinal -> return R.string.fan_speed
                Pipe2AnalogOutAssociation.WATER_VALVE.ordinal -> return R.string.water_valve
                Pipe2AnalogOutAssociation.DCV_DAMPER.ordinal -> return R.string.dcv_damper
            }
        }
        else -> {}
    }
    return -1
}



