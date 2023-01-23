package a75f.io.logic.bo.building.hyperstat.profiles.pipe2

import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuConfiguration

/**
 * Created by Manjunath K on 26-07-2022.
 */

class HyperStatPipe2Configuration: BaseProfileConfiguration() {

    var temperatureOffset = 0.0

    var isEnableAutoForceOccupied = false
    var isEnableAutoAway = false

    var relay1State = Pipe2RelayState(false, Pipe2RelayAssociation.FAN_MEDIUM_SPEED)
    var relay2State = Pipe2RelayState(false, Pipe2RelayAssociation.FAN_HIGH_SPEED)
    var relay3State = Pipe2RelayState(false, Pipe2RelayAssociation.FAN_LOW_SPEED)
    var relay4State = Pipe2RelayState(false, Pipe2RelayAssociation.AUX_HEATING_STAGE1)
    var relay5State = Pipe2RelayState(false, Pipe2RelayAssociation.AUX_HEATING_STAGE2)
    var relay6State = Pipe2RelayState(false, Pipe2RelayAssociation.WATER_VALVE)

    var analogOut1State = Pipe2AnalogOutState(false, Pipe2AnalogOutAssociation.WATER_VALVE, 2.0, 10.0,30.0,60.0,100.0)
    var analogOut2State = Pipe2AnalogOutState(false, Pipe2AnalogOutAssociation.FAN_SPEED, 2.0, 10.0,30.0,60.0,100.0)
    var analogOut3State = Pipe2AnalogOutState(false, Pipe2AnalogOutAssociation.DCV_DAMPER, 2.0, 10.0,30.0,60.0,100.0)

    var isEnableAirFlowTempSensor = false
    var isSupplyWaterSensor = true

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
data class Pipe2RelayState(
    val enabled: Boolean,
    val association: Pipe2RelayAssociation
)

data class Pipe2AnalogOutState(
    val enabled: Boolean,
    val association: Pipe2AnalogOutAssociation,
    val voltageAtMin: Double,   // create position to value mapping here.
    val voltageAtMax: Double,    // create position to value mapping here.
    val perAtFanLow: Double,
    val perAtFanMedium: Double,
    val perAtFanHigh: Double,
)

// Order is important for this enum -- it matches the UI as set in xml & strings.xml and ordinal is saved in data storage.
// Do not change order without a migration.
enum class Pipe2RelayAssociation {
    FAN_LOW_SPEED,
    FAN_MEDIUM_SPEED,
    FAN_HIGH_SPEED,
    AUX_HEATING_STAGE1,
    AUX_HEATING_STAGE2,
    WATER_VALVE,
    FAN_ENABLED,
    OCCUPIED_ENABLED,
    HUMIDIFIER,
    DEHUMIDIFIER
}

// Order is important -- see comment above.
enum class Pipe2AnalogOutAssociation {
    WATER_VALVE,
    FAN_SPEED,
    DCV_DAMPER
}


