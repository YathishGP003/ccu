package a75f.io.renatus.hyperstat

import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.Output
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.cpu.*
import a75f.io.logic.bo.building.hyperstat.cpu.CpuAnalogInAssociation.*
import a75f.io.logic.bo.building.hyperstat.cpu.CpuAnalogOutAssociation.*
import a75f.io.logic.bo.building.hyperstat.cpu.CpuRelayAssociation.*
import a75f.io.renatus.R
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * @author tcase@75f.io
 * Created on 6/18/21.
 */
class HyperStatCpuViewModel(application: Application) : AndroidViewModel(application) {

    var address: Short = 0
    lateinit var roomName: String
    lateinit var floorName: String
    lateinit var nodeType: NodeType
    lateinit var profileType: ProfileType

    val viewState: BehaviorSubject<CpuViewState> = BehaviorSubject.create()

    var hscConfiguration: HyperStatCpuConfiguration? = null
    var hscProfile: HyperStatCpuProfile? = null

    private val currentState: CpuViewState
        get() = viewState.value

    fun initData(address: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType) {
        this.address = address
        this.roomName = roomName
        this.floorName = floorName
        this.nodeType = nodeType
        this.profileType = profileType
        viewState.onNext(initialViewState(address))

    }


    // Save the configuration

    fun setConfigSelected() {
        // get the state fragment state
        val config = currentState.toConfig()
        config.nodeType = nodeType
        config.nodeAddress = address
        config.priority = ZonePriority.NONE

        if (config.relay1State.enabled) config.outputs.add(constructOutputRelay(address, Port.RELAY_ONE))
        if (config.relay2State.enabled) config.outputs.add(constructOutputRelay(address, Port.RELAY_TWO))
        if (config.relay3State.enabled) config.outputs.add(constructOutputRelay(address, Port.RELAY_THREE))
        if (config.relay4State.enabled) config.outputs.add(constructOutputRelay(address, Port.RELAY_FOUR))
        if (config.relay5State.enabled) config.outputs.add(constructOutputRelay(address, Port.RELAY_FIVE))
        if (config.relay6State.enabled) config.outputs.add(constructOutputRelay(address, Port.RELAY_SIX))

        if (config.analogOut1State.enabled) config.outputs.add(constructOutputAnalogOut(address, Port.ANALOG_OUT_ONE))
        if (config.analogOut2State.enabled) config.outputs.add(constructOutputAnalogOut(address, Port.ANALOG_OUT_TWO))
        if (config.analogOut3State.enabled) config.outputs.add(constructOutputAnalogOut(address, Port.ANALOG_OUT_THREE))

        hscProfile?.profileConfiguration?.put(address, config)

        if (hscConfiguration == null) {
            // creating all the Equip and point details for new profile
            hscProfile?.addNewEquip(address, roomName, floorName, config)
        } else {
            // update with latest configuration points
            hscProfile?.getHsCpuEquip(address)?.updateConfigPoints(config)
        }

        // Saving profile details
        L.ccu().zoneProfiles.add(hscProfile)
        L.saveCCUState()
    }

    private fun constructOutputRelay(
        address: Short,
        port: Port
    ): Output {
        var outputRelay = Output()
        outputRelay.address = address
        outputRelay.port = port
        outputRelay.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen
        return outputRelay
    }

    private fun constructOutputAnalogOut(
        address: Short,
        port: Port
    ): Output {
        val analogOut = Output()
        analogOut.address = address
        analogOut.port = port
        analogOut.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV
        return analogOut
    }

    private fun initialViewState(address: Short): CpuViewState {

        // todo: set a loading state

        // We first need to determine if this a new or existing module.  If existing, we need
        // to get the values and fill in.

        hscProfile = getExistingProfile(address)
        return if (hscProfile != null) {
            hscConfiguration = hscProfile!!.getProfileConfiguration(address)
            CpuViewState.fromConfig(hscConfiguration as HyperStatCpuConfiguration)
        } else {
            hscProfile = HyperStatCpuProfile()
            CpuViewState.fromConfig(HyperStatCpuConfiguration())
        }
    }

    private fun getExistingProfile(address: Short): HyperStatCpuProfile? = L.getProfile(address) as HyperStatCpuProfile?

    fun tempOffsetSelected(newVal: Int) {
        viewState.onNext(
            currentState.copy(
                tempOffsetPosition = newVal
            )
        )
    }

    fun enableForceOccupiedSwitchChanged(checked: Boolean) {
        viewState.onNext(
            currentState.copy(forceOccupiedEnabled = checked)
        )
    }

    fun enableAutoAwaySwitchChanged(checked: Boolean) {
        viewState.onNext(
            currentState.copy(autoAwayEnabled = checked)
        )
    }

    fun relaySwitchChanged(index: Int, checked: Boolean) {
        val relays = currentState.relays
        val newRelays = relays.updated(index, relays[index].copy(enabled = checked))
        viewState.onNext(
            currentState.copy(relays = newRelays)
        )
    }

    fun relayMappingSelected(index: Int, position: Int) {
        val relays = currentState.relays
        val newRelays = relays.updated(index, relays[index].copy(association = CpuRelayAssociation.values()[position]))
        viewState.onNext(
            currentState.copy(relays = newRelays)
        )
    }

    fun analogOutSwitchChanged(index: Int, checked: Boolean) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts =
            if (checked) {
                analogOuts.updated(index, analogOuts[index].copy(enabled = true))
            } else {
                // todo: this resetting of test signal to 0 is not working.
                analogOuts.updated(index, analogOuts[index].copy(enabled = false, testSignalPosition = 0))
            }

        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }

    fun analogOutMappingSelected(index: Int, position: Int) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts = analogOuts.updated(
            index, analogOuts[index].copy(association = CpuAnalogOutAssociation.values()[position])
        )
        newAnalogOuts[index].enabled
        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }

    fun analogOutTestSignalSelected(index: Int, position: Int) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts = analogOuts.updated(
            index, analogOuts[index].copy(testSignalPosition = position)
        )
        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }


    fun voltageAtDamperSelected(isMinPosition: Boolean, index: Int, position: Int) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts =
            if (isMinPosition) {
                analogOuts.updated(
                    index, analogOuts[index].copy(vAtMinDamperPosition = position)
                )
            } else {
                analogOuts.updated(
                    index, analogOuts[index].copy(vAtMaxDamperPosition = position)
                )
            }
        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }

    fun updateFanConfigSelected(type: Int, index: Int, position: Int) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts = when {
            (type == 1) -> {
                analogOuts.updated(index, analogOuts[index].copy(perAtFanLowPosition = position))
            }
            (type == 2) -> {
                analogOuts.updated(index, analogOuts[index].copy(perAtFanMediumPosition = position))
            }
            (type == 3) -> {
                analogOuts.updated(index, analogOuts[index].copy(perAtFanHighPosition = position))
            }
            else -> analogOuts
        }

        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }

    fun analogInSwitchChanged(index: Int, checked: Boolean) {
        val analogIns = currentState.analogIns
        val newAnalogIns = analogIns.updated(index, analogIns[index].copy(enabled = checked))
        viewState.onNext(
            currentState.copy(
                analogIns = newAnalogIns
            )
        )
    }

    fun analogInMappingSelected(index: Int, position: Int) {
        val analogIns = currentState.analogIns
        val newAnalogIns = analogIns.updated(
            index, analogIns[index].copy(association = CpuAnalogInAssociation.values()[position])
        )
        viewState.onNext(
            currentState.copy(
                analogIns = newAnalogIns
            )
        )
    }

    fun airflowTempSensorSwitchChanged(checked: Boolean) {
        viewState.onNext(
            currentState.copy(
                airflowTempSensorEnabled = checked
            )
        )
    }

    fun doorWindowSensorSwitchChanged(checked: Boolean) {
        viewState.onNext(
            currentState.copy(
                doorWindowSensor1Enabled = checked
            )
        )
    }


    fun zoneCO2DamperOpeningRateSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneCO2DamperOpeningRatePos = position
            )
        )

    }

    fun zoneCO2ThresholdSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneCO2ThresholdPos = position
            )
        )

    }

    fun zoneCO2TargetSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneCO2TargetPos = position
            )
        )
    }
}

// Dropdown choice value ranges
private val TEMP_OFFSET_LIMIT_MAX = 10
private val TEMP_OFFSET_LIMIT_MIN = -10
private val TEMP_OFFSET_INC = 0.1

private val TEST_SIGNAL_LIMIT_MAX = 100
private val TEST_SIGNAL_LIMIT_MIN = 0
private val TEST_SIGNAL_INC = 1.0

private val ANALOG_VOLTAGE_LIMIT_MAX = 10
private val ANALOG_VOLTAGE_LIMIT_MIN = 0
private val ANALOG_VOLTAGE_INC = 1.0

private val ANALOG_FAN_MAX_SPEED = 100
private val ANALOG_FAN_MIN_SPEED = 0
private val ANALOG_FAN_SPEED_INC = 1.0

private val OpeningRateMin = 0
private val OpeningRateMax = 200

private val CO2Min = 0
private val CO2Max = 2000

private val INC = 10


private fun tempOffsetIndexFromValue(tempOffset: Double) =
    offsetIndexFromValue(TEMP_OFFSET_LIMIT_MIN, TEMP_OFFSET_INC, tempOffset)

private fun tempValueFromIndex(index: Int) =
    offsetFromIndex(TEMP_OFFSET_LIMIT_MIN, TEMP_OFFSET_INC, index)

private fun testSignalIndexFromValue(testSignal: Double) =
    offsetIndexFromValue(TEST_SIGNAL_LIMIT_MIN, TEST_SIGNAL_INC, testSignal)

private fun analogVoltageIndexFromValue(voltage: Double) =
    offsetIndexFromValue(ANALOG_VOLTAGE_LIMIT_MIN, ANALOG_VOLTAGE_INC, voltage)

private fun analogVoltageFromIndex(index: Int) =
    offsetFromIndex(ANALOG_VOLTAGE_LIMIT_MIN, ANALOG_VOLTAGE_INC, index)

private fun offsetIndexFromValue(min: Int, inc: Double, offset: Double): Int {
    val offsetFromZeroCount = (min / inc).toInt()
    return (offset / inc).toInt() - offsetFromZeroCount
}

private fun offsetFromIndex(min: Int, inc: Double, index: Int): Double {
    val offsetFromZeroCount = (min / inc).toInt()
    return String.format("%.2f", (index + offsetFromZeroCount).toDouble() * inc).toDouble()
}

fun tempOffsetSpinnerValues(): Array<String?> =
    offsetSpinnerValues(TEMP_OFFSET_LIMIT_MAX, TEMP_OFFSET_LIMIT_MIN, TEMP_OFFSET_INC)

fun testSignalSpinnerValues(): Array<String?> =
    offsetSpinnerValues(TEST_SIGNAL_LIMIT_MAX, TEST_SIGNAL_LIMIT_MIN, TEST_SIGNAL_INC, true)

fun analogVoltageAtSpinnerValues(): Array<String?> =
    offsetSpinnerValues(ANALOG_VOLTAGE_LIMIT_MAX, ANALOG_VOLTAGE_LIMIT_MIN, ANALOG_VOLTAGE_INC, true, "V")


private fun analogFanSpeedIndexFromValue(percent: Double) =
    offsetIndexFromValue(ANALOG_FAN_MIN_SPEED, ANALOG_FAN_SPEED_INC, percent)

private fun analogFanSpeedFromIndex(index: Int) =
    offsetFromIndex(ANALOG_FAN_MIN_SPEED, ANALOG_FAN_SPEED_INC.toDouble(), index)

fun analogFanLevelSpeedValue(): Array<String?> {
    return offsetSpinnerValues(
        ANALOG_FAN_MAX_SPEED, ANALOG_FAN_MIN_SPEED,
        ANALOG_FAN_SPEED_INC.toDouble(), true, "%"
    )
}

private fun co2DCVDamperValueFromIndex(index: Int) = offsetFromIndex(CO2Min, INC.toDouble(), index)
private fun OpeningDamperValueFromIndex(index: Int) = offsetFromIndex(OpeningRateMin, INC.toDouble(), index)


private fun co2DCVDamperSetIndexFromValue(value: Double) = offsetIndexFromValue(CO2Min, INC.toDouble(), value)
private fun co2DCVOpeningDamperSetIndexFromValue(value: Double) =
    offsetIndexFromValue(OpeningRateMin, INC.toDouble(), value)


fun co2DCVDamperValue(): Array<String?> {
    return offsetSpinnerValues(
        CO2Max, CO2Min,
        INC.toDouble(), true, "ppm"
    )
}

fun co2DCVOpeningDamperValue(): Array<String?> {
    return offsetSpinnerValues(
        OpeningRateMax, OpeningRateMin,
        INC.toDouble(), true, "%"
    )
}


private fun offsetSpinnerValues(
    max: Int,
    min: Int,
    inc: Double,
    displayAsInt: Boolean = false,
    suffix: String = ""
): Array<String?> {

    val range = max - min
    val count = (range / inc).toInt() + 1
    val offsetFromZeroCount = (min / inc).toInt()

    val nums = arrayOfNulls<String>(count)
    for (nNum in 0 until count) {
        var rawValue = (nNum + offsetFromZeroCount).toFloat() * inc
        if (displayAsInt) {
            nums[nNum] = String.format("%.1f", rawValue) + suffix
        } else {
            nums[nNum] = String.format("%.1f", rawValue) + suffix
        }
    }
    return nums
}


val CpuRelayAssociation.displayName: Int
    get() {
        return when (this) {
            COOLING_STAGE_1 -> R.string.cooling_stage_1
            COOLING_STAGE_2 -> R.string.cooling_stage_2
            COOLING_STAGE_3 -> R.string.cooling_stage_3
            HEATING_STAGE_1 -> R.string.heating_stage_1
            HEATING_STAGE_2 -> R.string.heating_stage_2
            HEATING_STAGE_3 -> R.string.heating_stage_3
            FAN_LOW_SPEED -> R.string.fan_low_speed
            FAN_MEDIUM_SPEED -> R.string.fan_medium_speed
            FAN_HIGH_SPEED -> R.string.fan_high_speed
            FAN_ENABLED -> R.string.fan_enabled
            OCCUPIED_ENABLED -> R.string.occupied_enabled
            HUMIDIFIER -> R.string.humidifier
            DEHUMIDIFIER -> R.string.dehumidifier
        }
    }

val CpuAnalogOutAssociation.displayName: Int
    get() {
        return when (this) {
            COOLING -> R.string.cooling
            FAN_SPEED -> R.string.fan_speed
            HEATING -> R.string.heating
            DCV_DAMPER -> R.string.dcv_damper
        }
    }

val CpuAnalogInAssociation.displayName: Int
    get() {
        return when (this) {
            CURRENT_TX_0_10 -> R.string.current_tx_0_10
            CURRENT_TX_0_20 -> R.string.current_tx_0_20
            CURRENT_TX_0_50 -> R.string.current_tx_0_50
            KEY_CARD_SENSOR -> R.string.key_card_sensor
            DOOR_WINDOW_SENSOR -> R.string.door_window_sensor_ai
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

    ) {

    companion object {
        fun fromConfig(config: HyperStatCpuConfiguration) = CpuViewState(
            tempOffsetPosition = tempOffsetIndexFromValue(config.temperatureOffset),
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
        )
    }

    /**
     * Function which returns HyperStatCpuConfiguration configuration with existing configurations
     */
    fun toConfig(): HyperStatCpuConfiguration {
        return HyperStatCpuConfiguration().apply {
            temperatureOffset = tempValueFromIndex(tempOffsetPosition)
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
            zoneCO2Threshold = OpeningDamperValueFromIndex(zoneCO2ThresholdPos)
            zoneCO2Target = OpeningDamperValueFromIndex(zoneCO2TargetPos)
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
