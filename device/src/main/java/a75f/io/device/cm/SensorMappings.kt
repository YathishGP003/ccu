package a75f.io.device.cm

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip

/**
 * These are specific to Advanced AHU configuration and sensors that could be connected to thermistor/analog
 * inputs on the CM or connect module for the profile.
 * The SensorType and other apis from a75f.io.logic.bo.building.sensors as specific to module level sensors.
 * There is some level of overlapping, but too much of change to combine them to have single sensor component.
 */
enum class SensorType {
    ANALOG, THERMISTOR
}

/**
 * RESET - Reset value
 * LINEAR - Linear scaling
 * LOOKUP - Lookup table
 * DIRECT - Direct value -no conversion
 * BOOLEAN - 0 when val < threshold , 1 otherwise
 * BOOLEAN_INVERSE - 1 when val < threshold , 1 otherwise
 */
enum class SensorScaling {
    RESET, LINEAR, LOOKUP, DIRECT, BOOLEAN, BOOLEAN_INVERSE
}

enum class ThermistorLookup {
    NTC, PTC
}

interface SensorInput {
    val domainName: String
    val sensorType: SensorType
    val sensorScaling: SensorScaling
    val index: Int
}

data class ThermistorInput(override val domainName : String,
                           override val sensorType: SensorType = SensorType.THERMISTOR,
                           override val sensorScaling: SensorScaling,
                           val threshold : Int = 0,
                           val thermistorLookup: ThermistorLookup = ThermistorLookup.NTC,
                           override val index: Int = 0) : SensorInput

data class AnalogInput(override val domainName : String,
                       override val sensorType: SensorType = SensorType.ANALOG,
                       override val sensorScaling: SensorScaling,
                       val minValue: Double = 0.0,
                       val maxValue: Double = 0.0,
                       val minVoltage : Double = 0.0,
                       val maxVoltage : Double = 0.0,
                       override val index: Int = 0) : SensorInput



/**
 * This is mapping of "index" to the sensor input.
 * Universal input
 */

fun getAdvancedAhuSensorInputMappings() : Map<Int, SensorInput> {
    return mapOf(
        0 to AnalogInput(
            domainName = "None",
            sensorScaling = SensorScaling.RESET,
        ),
        1 to AnalogInput(
            domainName = DomainName.voltageInput,
            sensorScaling = SensorScaling.DIRECT,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        2 to ThermistorInput(
            domainName = DomainName.thermistorInput,
            sensorScaling = SensorScaling.DIRECT,
        ),
        3 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        4 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        5 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        6 to ThermistorInput(
            domainName = DomainName.chilledWaterInletTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        7 to ThermistorInput(
            domainName = DomainName.chilledWaterOutletTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        8 to AnalogInput(
            domainName = DomainName.coolingValvePositionFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        9 to AnalogInput(
            domainName = DomainName.dischargeAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        10 to AnalogInput(
            domainName = DomainName.dischargeAirFlowSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        11 to AnalogInput(
            domainName = DomainName.dischargeAirFlowSensor_20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        12 to AnalogInput(
            domainName = DomainName.supplyAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        13 to AnalogInput(
            domainName = DomainName.dischargeAirPm25,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        14 to ThermistorInput(
            domainName = DomainName.supplyAirTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        15 to AnalogInput(
            domainName = DomainName.dischargeFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        16 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        17 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        18 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        19 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        20 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        21 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        22 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        23 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        24 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        25 to AnalogInput(
            domainName = DomainName.heatingValvePositionFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        26 to AnalogInput(
            domainName = DomainName.minimumOutsideAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        27 to AnalogInput(
            domainName = DomainName.mixedAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        28 to ThermistorInput (
            domainName = DomainName.mixedAirTemperature,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        29 to AnalogInput(
            domainName = DomainName.outsideAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        30 to AnalogInput(
            domainName = DomainName.outsideAirFlowSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        31 to AnalogInput(
            domainName = DomainName.outsideAirFlowSensor_20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        32 to AnalogInput(
            domainName = DomainName.outsideHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        33 to ThermistorInput(
            domainName = DomainName.outsideTemperature,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        34 to AnalogInput(
            domainName = DomainName.returnAirCo2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2000.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        35 to AnalogInput(
            domainName = DomainName.mixedAirCo2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2000.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        36 to AnalogInput(
            domainName = DomainName.returnAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        37 to AnalogInput(
            domainName = DomainName.returnAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        38 to ThermistorInput(
            domainName = DomainName.returnAirTemperature,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        39 to ThermistorInput(
            domainName = DomainName.spaceTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        40 to AnalogInput(
            domainName = DomainName.mixedAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        41 to AnalogInput(
            domainName = DomainName.currentTx10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        42 to AnalogInput(
            domainName = DomainName.currentTx20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        43 to AnalogInput(
            domainName = DomainName.currentTx30,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 30.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        44 to AnalogInput(
            domainName = DomainName.currentTx50,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 50.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        45 to AnalogInput(
            domainName = DomainName.currentTx60,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 60.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        46 to AnalogInput(
            domainName = DomainName.currentTx100,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        47 to AnalogInput(
            domainName = DomainName.currentTx120,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 120.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        48 to AnalogInput(
            domainName = DomainName.currentTx150,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 150.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        49 to AnalogInput(
            domainName = DomainName.currentTx200,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 200.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        50 to AnalogInput(
            domainName = DomainName.exhaustFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        51 to AnalogInput(
            domainName = DomainName.outsideFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        52 to AnalogInput(
            domainName = DomainName.returnFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        53 to ThermistorInput(
            domainName = DomainName.dischargeFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        54 to ThermistorInput(
            domainName = DomainName.dischargeFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        55 to ThermistorInput(
            domainName = DomainName.dischargeFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        56 to ThermistorInput(
            domainName = DomainName.exhaustFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        57 to ThermistorInput(
            domainName = DomainName.uvAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        58 to ThermistorInput(
            domainName = DomainName.exhaustFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        59 to ThermistorInput(
            domainName = DomainName.exhaustFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        60 to ThermistorInput(
            domainName = DomainName.filterStatus1NO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        61 to ThermistorInput(
            domainName = DomainName.filterStatus1NC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        62 to ThermistorInput(
            domainName = DomainName.filterStatus2NO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        63 to ThermistorInput(
            domainName = DomainName.filterStatus2NC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        64 to ThermistorInput(
            domainName = DomainName.fireAlarmStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        65 to ThermistorInput(
            domainName = DomainName.fireDamperStatus1,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        66 to ThermistorInput(
            domainName = DomainName.fireDamperStatus2,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        67 to ThermistorInput(
            domainName = DomainName.fireDamperStatus3,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        68 to ThermistorInput(
            domainName = DomainName.fireDamperStatus4,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        69 to ThermistorInput(
            domainName = DomainName.fireDamperStatus5,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        70 to ThermistorInput(
            domainName = DomainName.fireDamperStatus6,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        71 to ThermistorInput(
            domainName = DomainName.fireDamperStatus7,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        72 to ThermistorInput(
            domainName = DomainName.fireDamperStatus8,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        73 to ThermistorInput(
            domainName = DomainName.highDifferentialPressureSwitch,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        74 to ThermistorInput(
            domainName = DomainName.lowDifferentialPressureSwitch,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        75 to ThermistorInput(
            domainName = DomainName.outsideFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        76 to ThermistorInput(
            domainName = DomainName.outsideFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        77 to ThermistorInput(
            domainName = DomainName.outsideFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        78 to ThermistorInput(
            domainName = DomainName.returnFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        79 to ThermistorInput(
            domainName = DomainName.returnFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        80 to ThermistorInput(
            domainName = DomainName.returnFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        81 to ThermistorInput(
            domainName = DomainName.uvRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        82 to ThermistorInput(
            domainName = DomainName.uvTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        83 to ThermistorInput(
            domainName = DomainName.condensateStatusNO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        84 to ThermistorInput(
            domainName = DomainName.condensateStatusNC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        85 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature1,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        86 to AnalogInput(
            domainName = DomainName.supplyAirHumidity1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        87 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature2,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        88 to AnalogInput(
            domainName = DomainName.supplyAirHumidity2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        89 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature3,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        90 to AnalogInput(
            domainName = DomainName.supplyAirHumidity3,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        91 to ThermistorInput(
            domainName = DomainName.emergencyShutoffNO,
            sensorScaling = SensorScaling.BOOLEAN,
        ),
        92 to ThermistorInput(
            domainName = DomainName.emergencyShutoffNC,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
        ),

    )
}
fun getAdvancedAhuThermistorMappings() : Map<Int, ThermistorInput> {
    return mapOf(
        0 to ThermistorInput(
            domainName = "None",
            sensorScaling = SensorScaling.RESET,
        ),
        1 to ThermistorInput(
            domainName = DomainName.thermistorInput,
            sensorScaling = SensorScaling.DIRECT,
        ),
        2 to ThermistorInput(
            domainName = DomainName.chilledWaterInletTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        3 to ThermistorInput(
            domainName = DomainName.chilledWaterOutletTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        4 to ThermistorInput(
            domainName = DomainName.supplyAirTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        5 to ThermistorInput(
            domainName = DomainName.mixedAirTemperature,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        6 to ThermistorInput(
            domainName = DomainName.outsideTemperature,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        7 to ThermistorInput(
            domainName = DomainName.returnAirTemperature,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        8 to ThermistorInput(
            domainName = DomainName.spaceTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        9 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature1,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        10 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature2,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        11 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature3,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        12 to ThermistorInput(
            domainName = DomainName.dischargeFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        13 to ThermistorInput(
            domainName = DomainName.dischargeFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        14 to ThermistorInput(
            domainName = DomainName.dischargeFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        15 to ThermistorInput(
            domainName = DomainName.exhaustFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        16 to ThermistorInput(
            domainName = DomainName.uvAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        17 to ThermistorInput(
            domainName = DomainName.exhaustFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        18 to ThermistorInput(
            domainName = DomainName.exhaustFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        19 to ThermistorInput(
            domainName = DomainName.filterStatus1NO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        20 to ThermistorInput(
            domainName = DomainName.filterStatus1NC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        21 to ThermistorInput(
            domainName = DomainName.filterStatus2NO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        22 to ThermistorInput(
            domainName = DomainName.filterStatus2NC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),

        23 to ThermistorInput(
            domainName = DomainName.fireAlarmStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        24 to ThermistorInput(
            domainName = DomainName.fireDamperStatus1,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        25 to ThermistorInput(
            domainName = DomainName.fireDamperStatus2,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        26 to ThermistorInput(
            domainName = DomainName.fireDamperStatus3,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        27 to ThermistorInput(
            domainName = DomainName.fireDamperStatus4,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        28 to ThermistorInput(
            domainName = DomainName.fireDamperStatus5,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        29 to ThermistorInput(
            domainName = DomainName.fireDamperStatus6,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        30 to ThermistorInput(
            domainName = DomainName.fireDamperStatus7,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        31 to ThermistorInput(
            domainName = DomainName.fireDamperStatus8,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        32 to ThermistorInput(
            domainName = DomainName.highDifferentialPressureSwitch,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        33 to ThermistorInput(
            domainName = DomainName.lowDifferentialPressureSwitch,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        34 to ThermistorInput(
            domainName = DomainName.outsideFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        35 to ThermistorInput(
            domainName = DomainName.outsideFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        36 to ThermistorInput(
            domainName = DomainName.outsideFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        37 to ThermistorInput(
            domainName = DomainName.returnFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        38 to ThermistorInput(
            domainName = DomainName.returnFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        39 to ThermistorInput(
            domainName = DomainName.returnFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        40 to ThermistorInput(
            domainName = DomainName.uvRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        41 to ThermistorInput(
            domainName = DomainName.uvTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        42 to ThermistorInput(
            domainName = DomainName.condensateStatusNO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        43 to ThermistorInput(
            domainName = DomainName.condensateStatusNC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        44 to ThermistorInput(
            domainName = DomainName.emergencyShutoffNO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        45 to ThermistorInput(
            domainName = DomainName.emergencyShutoffNC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        )
    )
}
fun getAdvancedAhuThermistorDomainNameMappings() : Map<String, ThermistorInput> {
    return mapOf(
        "thermistorInput" to ThermistorInput(
            domainName = "thermistorInput",
            sensorScaling = SensorScaling.DIRECT,
        ),
        DomainName.chilledWaterInletTemp to ThermistorInput(
            domainName = DomainName.chilledWaterInletTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.chilledWaterOutletTemp to ThermistorInput(
            domainName = DomainName.chilledWaterOutletTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.dischargeAirTemp to ThermistorInput(
            domainName = DomainName.dischargeAirTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.mixedAirTemperature to ThermistorInput(
            domainName = DomainName.dischargeAirTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.outsideAirTemp to ThermistorInput(
            domainName = DomainName.outsideAirTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.returnAirTemperature to ThermistorInput(
            domainName = DomainName.returnAirTemperature,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.spaceTemp to ThermistorInput(
            domainName = DomainName.spaceTemp,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.supplyAirTemperature1 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature1,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.supplyAirTemperature2 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature2,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.supplyAirTemperature3 to ThermistorInput(
            domainName = DomainName.supplyAirTemperature3,
            sensorScaling = SensorScaling.LOOKUP,
        ),
        DomainName.dischargeFanAMStatus to ThermistorInput(
            domainName = DomainName.dischargeFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.dischargeFanRunStatus to ThermistorInput(
            domainName = DomainName.dischargeFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.dischargeFanTripStatus to ThermistorInput(
            domainName = DomainName.dischargeFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.exhaustFanAMStatus to ThermistorInput(
            domainName = DomainName.exhaustFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.uvAMStatus to ThermistorInput(
            domainName = DomainName.uvAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.exhaustFanRunStatus to ThermistorInput(
            domainName = DomainName.exhaustFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.exhaustFanTripStatus to ThermistorInput(
            domainName = DomainName.exhaustFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.filterStatus1NO to ThermistorInput(
            domainName = DomainName.filterStatus1NO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        DomainName.filterStatus1NC to ThermistorInput(
            domainName = DomainName.filterStatus1NC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.filterStatus2NO to ThermistorInput(
            domainName = DomainName.filterStatus2NO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        DomainName.filterStatus2NC to ThermistorInput(
            domainName = DomainName.filterStatus2NC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),

        DomainName.fireAlarmStatus to ThermistorInput(
            domainName = DomainName.fireAlarmStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus1 to ThermistorInput(
            domainName = DomainName.fireDamperStatus1,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus2 to ThermistorInput(
            domainName = DomainName.fireDamperStatus2,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus3 to ThermistorInput(
            domainName = DomainName.fireDamperStatus3,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus4 to ThermistorInput(
            domainName = DomainName.fireDamperStatus4,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus5 to ThermistorInput(
            domainName = DomainName.fireDamperStatus5,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus6 to ThermistorInput(
            domainName = DomainName.fireDamperStatus6,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus7 to ThermistorInput(
            domainName = DomainName.fireDamperStatus1,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.fireDamperStatus8 to ThermistorInput(
            domainName = DomainName.fireDamperStatus1,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.highDifferentialPressureSwitch to ThermistorInput(
            domainName = DomainName.highDifferentialPressureSwitch,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.lowDifferentialPressureSwitch to ThermistorInput(
            domainName = DomainName.lowDifferentialPressureSwitch,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.outsideFanRunStatus to ThermistorInput(
            domainName = DomainName.outsideFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.outsideFanTripStatus to ThermistorInput(
            domainName = DomainName.outsideFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.outsideFanAMStatus to ThermistorInput(
            domainName = DomainName.outsideFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.returnFanRunStatus to ThermistorInput(
            domainName = DomainName.returnFanRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.returnFanTripStatus to ThermistorInput(
            domainName = DomainName.returnFanTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.returnFanAMStatus to ThermistorInput(
            domainName = DomainName.returnFanAMStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.uvRunStatus to ThermistorInput(
            domainName = DomainName.uvRunStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.uvTripStatus to ThermistorInput(
            domainName = DomainName.uvTripStatus,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.condensateStatusNO to ThermistorInput(
            domainName = DomainName.condensateStatusNO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
            threshold = 10000
        ),
        DomainName.condensateStatusNC to ThermistorInput(
            domainName = DomainName.condensateStatusNC,
            sensorScaling = SensorScaling.BOOLEAN,
            threshold = 10000
        ),
        DomainName.emergencyShutoffNC to ThermistorInput(
            domainName = DomainName.emergencyShutoffNC,
            sensorScaling = SensorScaling.BOOLEAN,
        ),
        DomainName.emergencyShutoffNO to ThermistorInput(
            domainName = DomainName.emergencyShutoffNO,
            sensorScaling = SensorScaling.BOOLEAN_INVERSE,
        )
    )
}

fun getAdvancedAhuAnalogInputMappings() : Map<Int, AnalogInput> {
    return mapOf(
        0 to AnalogInput(
            domainName = "None",
            sensorScaling = SensorScaling.RESET,
        ),
        1 to AnalogInput(
            domainName = DomainName.voltageInput,
            sensorScaling = SensorScaling.DIRECT,
        ),
        2 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        3 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        4 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        5 to AnalogInput(
            domainName = DomainName.coolingValvePositionFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        6 to AnalogInput(
            domainName = DomainName.dischargeAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        7 to AnalogInput(
            domainName = DomainName.dischargeAirFlowSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        8 to AnalogInput(
            domainName = DomainName.dischargeAirFlowSensor_20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        9 to AnalogInput(
            domainName = DomainName.supplyAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        10 to AnalogInput(
            domainName = DomainName.dischargeAirPm25,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        11 to AnalogInput(
            domainName = DomainName.dischargeFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        12 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        13 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        14 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        15 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        16 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        17 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        18 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        19 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        20 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        21 to AnalogInput(
            domainName = DomainName.heatingValvePositionFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        22 to AnalogInput(
            domainName = DomainName.minimumOutsideAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        23 to AnalogInput(
            domainName = DomainName.mixedAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        24 to AnalogInput(
            domainName = DomainName.outsideAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        25 to AnalogInput(
            domainName = DomainName.outsideAirFlowSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        26 to AnalogInput(
            domainName = DomainName.outsideAirFlowSensor_20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        27 to AnalogInput(
            domainName = DomainName.outsideHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        28 to AnalogInput(
            domainName = DomainName.mixedAirCo2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2000.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        29 to AnalogInput(
            domainName = DomainName.returnAirCo2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2000.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        30 to AnalogInput(
            domainName = DomainName.returnAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        31 to AnalogInput(
            domainName = DomainName.returnAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        32 to AnalogInput(
            domainName = DomainName.mixedAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        33 to AnalogInput(
            domainName = DomainName.currentTx10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        34 to AnalogInput(
            domainName = DomainName.currentTx20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        35 to AnalogInput(
            domainName = DomainName.currentTx30,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 30.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        36 to AnalogInput(
            domainName = DomainName.currentTx50,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 50.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        37 to AnalogInput(
            domainName = DomainName.currentTx60,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 60.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        38 to AnalogInput(
            domainName = DomainName.currentTx100,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        39 to AnalogInput(
            domainName = DomainName.currentTx120,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 120.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        40 to AnalogInput(
            domainName = DomainName.currentTx150,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 150.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        41 to AnalogInput(
            domainName = DomainName.currentTx200,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 200.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        42 to AnalogInput(
            domainName = DomainName.exhaustFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        43 to AnalogInput(
            domainName = DomainName.outsideFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        44 to AnalogInput(
            domainName = DomainName.returnFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        45 to AnalogInput(
            domainName = DomainName.supplyAirHumidity1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        46 to AnalogInput(
            domainName = DomainName.supplyAirHumidity2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        47 to AnalogInput(
            domainName = DomainName.supplyAirHumidity3,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ))
}
fun getAdvancedAhuAnalogInputDomainNameMappings() : Map<String, AnalogInput> {
    return mapOf(
        "voltageInput" to AnalogInput(
            domainName = "voltageInput",
            sensorScaling = SensorScaling.DIRECT,
        ),
        DomainName.buildingStaticPressureSensor_1 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.buildingStaticPressureSensor_2 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.buildingStaticPressureSensor_10 to AnalogInput(
            domainName = DomainName.buildingStaticPressureSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.coolingValvePositionFeedback to AnalogInput(
            domainName = DomainName.coolingValvePositionFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.dischargeAirDamperFeedback to AnalogInput(
            domainName = DomainName.dischargeAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.dischargeAirFlowSensor_10 to AnalogInput(
            domainName = DomainName.dischargeAirFlowSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.dischargeAirFlowSensor_20 to AnalogInput(
            domainName = DomainName.dischargeAirFlowSensor_20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.dischargeAirHumidity to AnalogInput(
            domainName = DomainName.dischargeAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.dischargeAirPm25 to AnalogInput(
            domainName = DomainName.dischargeAirPm25,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.dischargeFanVfdFeedback to AnalogInput(
            domainName = DomainName.dischargeFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor1_1 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor1_2 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor1_10 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor1_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor2_1 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor2_2 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor2_10 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor2_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor3_1 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 1.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor3_2 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.ductStaticPressureSensor3_10 to AnalogInput(
            domainName = DomainName.ductStaticPressureSensor3_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.heatingValvePositionFeedback to AnalogInput(
            domainName = DomainName.heatingValvePositionFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.minimumOutsideAirDamperFeedback to AnalogInput(
            domainName = DomainName.minimumOutsideAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.mixedAirDamperFeedback to AnalogInput(
            domainName = DomainName.mixedAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.outsideAirDamperFeedback to AnalogInput(
            domainName = DomainName.outsideAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.outsideAirFlowSensor_10 to AnalogInput(
            domainName = DomainName.outsideAirFlowSensor_10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.outsideAirFlowSensor_20 to AnalogInput(
            domainName = DomainName.outsideAirFlowSensor_20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.outsideAirHumidity to AnalogInput(
            domainName = DomainName.outsideAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.mixedAirCo2 to AnalogInput(
            domainName = DomainName.mixedAirCo2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2000.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.returnAirCo2 to AnalogInput(
            domainName = DomainName.returnAirCo2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 2000.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.returnAirDamperFeedback to AnalogInput(
            domainName = DomainName.returnAirDamperFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.returnAirHumidity to AnalogInput(
            domainName = DomainName.returnAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.mixedAirHumidity to AnalogInput(
            domainName = DomainName.mixedAirHumidity,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx10 to AnalogInput(
            domainName = DomainName.currentTx10,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 10.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx20 to AnalogInput(
            domainName = DomainName.currentTx20,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 20.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx30 to AnalogInput(
            domainName = DomainName.currentTx30,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 30.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx50 to AnalogInput(
            domainName = DomainName.currentTx50,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 50.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx60 to AnalogInput(
            domainName = DomainName.currentTx60,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 60.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx100 to AnalogInput(
            domainName = DomainName.currentTx100,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx120 to AnalogInput(
            domainName = DomainName.currentTx120,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 120.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx150 to AnalogInput(
            domainName = DomainName.currentTx150,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 150.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.currentTx200 to AnalogInput(
            domainName = DomainName.currentTx200,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 200.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.exhaustFanVfdFeedback to AnalogInput(
            domainName = DomainName.exhaustFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.outsideFanVfdFeedback to AnalogInput(
            domainName = DomainName.outsideFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.returnFanVfdFeedback to AnalogInput(
            domainName = DomainName.returnFanVfdFeedback,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.supplyAirHumidity1 to AnalogInput(
            domainName = DomainName.supplyAirHumidity1,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.supplyAirHumidity2 to AnalogInput(
            domainName = DomainName.supplyAirHumidity2,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ),
        DomainName.supplyAirHumidity3 to AnalogInput(
            domainName = DomainName.supplyAirHumidity3,
            sensorScaling = SensorScaling.LINEAR,
            minValue = 0.0,
            maxValue = 100.0,
            minVoltage = 0.0,
            maxVoltage = 10.0
        ))
}

fun getSensorDomainPointFromName(name : String, equip : DomainEquip) : Point? {
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        //is ConnectModuleEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
        }
    return when(name) {
        DomainName.voltageInput -> systemEquip.voltageInput
        DomainName.buildingStaticPressureSensor_1 -> systemEquip.buildingStaticPressureSensor1
        DomainName.buildingStaticPressureSensor_2 -> systemEquip.buildingStaticPressureSensor2
        DomainName.buildingStaticPressureSensor_10 -> systemEquip.buildingStaticPressureSensor10
        DomainName.chilledWaterInletTemp -> systemEquip.chilledWaterInletTemp
        DomainName.chilledWaterOutletTemp -> systemEquip.chilledWaterOutletTemp
        DomainName.coolingValvePositionFeedback -> systemEquip.coolingValvePositionFeedback
        DomainName.dischargeAirDamperFeedback -> systemEquip.dischargeAirDamperFeedback
        DomainName.dischargeAirFlowSensor_10 -> systemEquip.dischargeAirFlowSensor10
        DomainName.dischargeAirFlowSensor_20 -> systemEquip.dischargeAirFlowSensor20
        DomainName.dischargeAirHumidity -> systemEquip.dischargeAirHumidity
        DomainName.dischargeAirPm25 -> systemEquip.dischargeAirPM25
        DomainName.dischargeAirTemp -> systemEquip.dischargeAirTemp
        DomainName.dischargeFanVfdFeedback -> systemEquip.dischargeFanVFDFeedback
        DomainName.ductStaticPressureSensor1_1 -> systemEquip.ductStaticPressureSensor11
        DomainName.ductStaticPressureSensor1_2 -> systemEquip.ductStaticPressureSensor12
        DomainName.ductStaticPressureSensor1_10 -> systemEquip.ductStaticPressureSensor110
        DomainName.ductStaticPressureSensor2_1 -> systemEquip.ductStaticPressureSensor21
        DomainName.ductStaticPressureSensor2_2 -> systemEquip.ductStaticPressureSensor22
        DomainName.ductStaticPressureSensor2_10 -> systemEquip.ductStaticPressureSensor210
        DomainName.ductStaticPressureSensor3_1 -> systemEquip.ductStaticPressureSensor31
        DomainName.ductStaticPressureSensor3_2 -> systemEquip.ductStaticPressureSensor32
        DomainName.ductStaticPressureSensor3_10 -> systemEquip.ductStaticPressureSensor310
        DomainName.heatingValvePositionFeedback -> systemEquip.heatingValvePositionFeedback
        DomainName.minimumOutsideAirDamperFeedback -> systemEquip.minimumOutsideAirDamperFeedback
        DomainName.mixedAirDamperFeedback -> systemEquip.mixedAirDamperFeedback
        DomainName.mixedAirTemperature -> systemEquip.mixedAirTemperature
        DomainName.outsideAirDamperFeedback -> systemEquip.outsideAirDamperFeedback
        DomainName.outsideAirFlowSensor_10 -> systemEquip.outsideAirFlowSensor10
        DomainName.outsideAirFlowSensor_20 -> systemEquip.outsideAirFlowSensor20
        DomainName.outsideHumidity -> systemEquip.outsideHumidity
        DomainName.outsideTemperature -> systemEquip.outsideTemperature
        DomainName.mixedAirCo2 -> systemEquip.mixedAirCo2
        DomainName.returnAirCo2 -> systemEquip.returnAirCo2
        DomainName.returnAirDamperFeedback -> systemEquip.returnAirDamperFeedback
        DomainName.returnAirHumidity -> systemEquip.returnAirHumidity
        DomainName.returnAirTemperature -> systemEquip.returnAirTemperature
        DomainName.spaceTemp -> systemEquip.spaceTemp
        DomainName.currentTx10 -> systemEquip.currentTx10
        DomainName.currentTx20 -> systemEquip.currentTx20
        DomainName.currentTx30 -> systemEquip.currentTx30
        DomainName.currentTx50 -> systemEquip.currentTx50
        DomainName.currentTx60 -> systemEquip.currentTx60
        DomainName.currentTx100 -> systemEquip.currentTx100
        DomainName.currentTx120 -> systemEquip.currentTx120
        DomainName.currentTx150 -> systemEquip.currentTx150
        DomainName.currentTx200 -> systemEquip.currentTx200
        DomainName.exhaustFanVfdFeedback -> systemEquip.exhaustFanVfdFeedback
        DomainName.outsideFanVfdFeedback -> systemEquip.outsideFanVfdFeedback
        DomainName.returnFanVfdFeedback -> systemEquip.returnFanVfdFeedback
        DomainName.dischargeFanAMStatus -> systemEquip.dischargeFanAMStatus
        DomainName.dischargeFanRunStatus -> systemEquip.dischargeFanRunStatus
        DomainName.dischargeFanTripStatus -> systemEquip.dischargeFanTripStatus
        DomainName.exhaustFanAMStatus -> systemEquip.exhaustFanAMStatus
        DomainName.uvAMStatus -> systemEquip.uvAMStatus
        DomainName.exhaustFanRunStatus -> systemEquip.exhaustFanRunStatus
        DomainName.exhaustFanTripStatus -> systemEquip.exhaustFanTripStatus
        DomainName.filterStatus1NO -> systemEquip.filterStatus1NO
        DomainName.filterStatus1NC -> systemEquip.filterStatus1NC
        DomainName.filterStatus2NO -> systemEquip.filterStatus2NO
        DomainName.filterStatus2NC -> systemEquip.filterStatus2NC
        DomainName.fireAlarmStatus -> systemEquip.fireAlarmStatus
        DomainName.fireDamperStatus1 -> systemEquip.fireDamperStatus1
        DomainName.fireDamperStatus2 -> systemEquip.fireDamperStatus2
        DomainName.fireDamperStatus3 -> systemEquip.fireDamperStatus3
        DomainName.fireDamperStatus4 -> systemEquip.fireDamperStatus4
        DomainName.fireDamperStatus5 -> systemEquip.fireDamperStatus5
        DomainName.fireDamperStatus6 -> systemEquip.fireDamperStatus6
        DomainName.fireDamperStatus7 -> systemEquip.fireDamperStatus7
        DomainName.fireDamperStatus8 -> systemEquip.fireDamperStatus8
        DomainName.highDifferentialPressureSwitch -> systemEquip.highDifferentialPressureSwitch
        DomainName.lowDifferentialPressureSwitch -> systemEquip.lowDifferentialPressureSwitch
        DomainName.outsideFanRunStatus -> systemEquip.outsideFanRunStatus
        DomainName.outsideFanTripStatus -> systemEquip.outsideFanTripStatus
        DomainName.outsideFanAMStatus -> systemEquip.outsideFanAMStatus
        DomainName.returnFanRunStatus -> systemEquip.returnFanRunStatus
        DomainName.returnFanTripStatus -> systemEquip.returnFanTripStatus
        DomainName.returnFanAMStatus -> systemEquip.returnFanAMStatus
        DomainName.uvRunStatus -> systemEquip.uvRunStatus
        DomainName.uvTripStatus -> systemEquip.uvTripStatus
        DomainName.condensateStatusNO -> systemEquip.condensateStatusNO
        DomainName.condensateStatusNC -> systemEquip.condensateStatusNC
        DomainName.supplyAirHumidity1 -> systemEquip.supplyAirHumidity1
        DomainName.supplyAirHumidity2 -> systemEquip.supplyAirHumidity2
        DomainName.supplyAirHumidity3 -> systemEquip.supplyAirHumidity3
        DomainName.supplyAirTemperature1 -> systemEquip.supplyAirTemperature1
        DomainName.supplyAirTemperature2 -> systemEquip.supplyAirTemperature2
        DomainName.supplyAirTemperature3 -> systemEquip.supplyAirTemperature3
        DomainName.emergencyShutoffNC -> systemEquip.emergencyShutoffNC
        DomainName.emergencyShutoffNO -> systemEquip.emergencyShutoffNO
        else -> return null


    }
}