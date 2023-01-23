package a75f.io.logic.bo.building.hyperstat.profiles.hpu

import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.Thermistor
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuConfiguration

/**
 * Created by Manjunath K on 28-12-2022.
 */

class HyperStatHpuConfiguration: BaseProfileConfiguration() {

    var temperatureOffset = 0.0
    var isEnableAutoForceOccupied = false
    var isEnableAutoAway = false

    var relay1State = HpuRelayState(false, HpuRelayAssociation.COMPRESSOR_STAGE1)
    var relay2State = HpuRelayState(false, HpuRelayAssociation.COMPRESSOR_STAGE2)
    var relay3State = HpuRelayState(false, HpuRelayAssociation.FAN_LOW_SPEED)
    var relay4State = HpuRelayState(false, HpuRelayAssociation.AUX_HEATING_STAGE1)
    var relay5State = HpuRelayState(false, HpuRelayAssociation.FAN_MEDIUM_SPEED)
    var relay6State = HpuRelayState(false, HpuRelayAssociation.CHANGE_OVER_O_COOLING)

    var analogOut1State = HpuAnalogOutState(false, HpuAnalogOutAssociation.COMPRESSOR_SPEED, 2.0, 10.0,70.0,80.0,100.0)
    var analogOut2State = HpuAnalogOutState(false, HpuAnalogOutAssociation.FAN_SPEED, 2.0, 10.0,70.0,80.0,100.0)
    var analogOut3State = HpuAnalogOutState(false, HpuAnalogOutAssociation.DCV_DAMPER, 2.0, 10.0,70.0,80.0,100.0)

    var isEnableAirFlowTempSensor = false
    var isEnableDoorWindowSensor = false

    var analogIn1State = AnalogInState(false, AnalogInAssociation.KEY_CARD_SENSOR)
    var analogIn2State = AnalogInState(false, AnalogInAssociation.DOOR_WINDOW_SENSOR)

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

    companion object {
        fun default(): HyperStatCpuConfiguration = HyperStatCpuConfiguration()
    }
}


/**
 * State for a single relay: enabled and its mapping.
 */

data class HpuAnalogOutState(
    val enabled: Boolean,
    val association: HpuAnalogOutAssociation,
    val voltageAtMin: Double,   // create position to value mapping here.
    val voltageAtMax: Double,    // create position to value mapping here.
    val perAtFanLow: Double,
    val perAtFanMedium: Double,
    val perAtFanHigh: Double,
)
data class HpuRelayState(
    val enabled: Boolean,
    val association: HpuRelayAssociation
)

// Order is important for this enum -- it matches the UI as set in xml & strings.xml and ordinal is saved in data storage.
// Do not change order without a migration.
enum class HpuRelayAssociation {
    COMPRESSOR_STAGE1,
    COMPRESSOR_STAGE2,
    COMPRESSOR_STAGE3,
    AUX_HEATING_STAGE1,
    AUX_HEATING_STAGE2,
    FAN_LOW_SPEED,
    FAN_MEDIUM_SPEED,
    FAN_HIGH_SPEED,
    FAN_ENABLED,
    OCCUPIED_ENABLED,
    HUMIDIFIER,
    DEHUMIDIFIER,
    CHANGE_OVER_O_COOLING,
    CHANGE_OVER_B_HEATING
}

// Order is important -- see comment above.
enum class HpuAnalogOutAssociation {
    COMPRESSOR_SPEED,
    FAN_SPEED,
    DCV_DAMPER
}
