package a75f.io.logic.bo.building.system

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.logic.bo.building.system.util.DuctPressureSensorSource

enum class AdvancedAhuRelayAssociationType {
    LOAD_COOLING, LOAD_HEATING, LOAD_FAN, HUMIDIFIER, DEHUMIDIFIER, SAT_COOLING, SAT_HEATING, FAN_PRESSURE, OCCUPIED_ENABLE, FAN_ENABLE, AHU_FRESH_AIR_FAN_COMMAND;

    fun isConditioningStage() = this == LOAD_COOLING || this == SAT_COOLING || this == LOAD_HEATING || this == SAT_HEATING
    fun isSatStage() = this == SAT_COOLING || this == SAT_HEATING
    fun isLoadStage() = this == LOAD_COOLING || this == LOAD_HEATING
}

enum class AdvancedAhuAnalogOutAssociationType {
    PRESSURE_FAN, SAT_COOLING, SAT_HEATING, LOAD_COOLING, LOAD_HEATING, LOAD_FAN, CO2_DAMPER, COMPOSITE_SIGNAL
}

data class UserIntentConfig(
        var isSatHeatingAvailable: Boolean = false,
        var isSatCoolingAvailable: Boolean = false,
        var isPressureControlAvailable: Boolean = false,
        var isCo2DamperControlAvailable: Boolean = false,
)



fun getCMRelayAssociationMap(equip: DomainEquip): Map<Point, Point> {
    val associations: MutableMap<Point, Point> = HashMap()
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }

    associations[systemEquip.relay1OutputEnable] = systemEquip.relay1OutputAssociation
    associations[systemEquip.relay2OutputEnable] = systemEquip.relay2OutputAssociation
    associations[systemEquip.relay3OutputEnable] = systemEquip.relay3OutputAssociation
    associations[systemEquip.relay4OutputEnable] = systemEquip.relay4OutputAssociation
    associations[systemEquip.relay5OutputEnable] = systemEquip.relay5OutputAssociation
    associations[systemEquip.relay6OutputEnable] = systemEquip.relay6OutputAssociation
    associations[systemEquip.relay7OutputEnable] = systemEquip.relay7OutputAssociation
    associations[systemEquip.relay8OutputEnable] = systemEquip.relay8OutputAssociation
    return associations
}

fun getCMAnalogAssociationMap(equip: DomainEquip): Map<Point, Point> {
    val associations: MutableMap<Point, Point> = HashMap()

    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }

    associations[systemEquip.analog1OutputEnable] = systemEquip.analog1OutputAssociation
    associations[systemEquip.analog2OutputEnable] = systemEquip.analog2OutputAssociation
    associations[systemEquip.analog3OutputEnable] = systemEquip.analog3OutputAssociation
    associations[systemEquip.analog4OutputEnable] = systemEquip.analog4OutputAssociation

    return associations
}


enum class AdvancedAhuThermistorAssociationType {
    NONE,
    THERMISTOR_INPUT,
    CHILLED_WATER_INLET_TEMP,
    CHILLED_WATER_OUTLET_TEMP,
    SUPPLY_AIR_TEMP,
    MIXED_AIR_TEMPERATURE,
    OUTSIDE_TEMPERATURE,
    RETURN_AIR_TEMPERATURE,
    SPACE_TEMP,
    SUPPLY_AIR_TEMPERATURE_1,
    SUPPLY_AIR_TEMPERATURE_2,
    SUPPLY_AIR_TEMPERATURE_3,
    DISCHARGE_FAN_AM_STATUS,
    DISCHARGE_FAN_RUN_STATUS,
    DISCHARGE_FAN_TRIP_STATUS,
    EXHAUST_FAN_AM_STATUS,
    UV_AM_STATUS,
    EXHAUST_FAN_RUN_STATUS,
    EXHAUST_FAN_TRIP_STATUS,
    FILTER_STATUS_1_NO,
    FILTER_STATUS_1_NC,
    FILTER_STATUS_2_NO,
    FILTER_STATUS_2_NC,
    FIRE_ALARM_STATUS,
    FIRE_DAMPER_STATUS_1,
    FIRE_DAMPER_STATUS_2,
    FIRE_DAMPER_STATUS_3,
    FIRE_DAMPER_STATUS_4,
    FIRE_DAMPER_STATUS_5,
    FIRE_DAMPER_STATUS_6,
    FIRE_DAMPER_STATUS_7,
    FIRE_DAMPER_STATUS_8,
    HIGH_DIFFERENTIAL_PRESSURE_SWITCH,
    LOW_DIFFERENTIAL_PRESSURE_SWITCH,
    OUTSIDE_FAN_RUN_STATUS,
    OUTSIDE_FAN_TRIP_STATUS,
    OUTSIDE_FAN_AM_STATUS,
    RETURN_FAN_RUN_STATUS,
    RETURN_FAN_TRIP_STATUS,
    RETURN_FAN_AM_STATUS,
    UV_RUN_STATUS,
    UV_TRIP_STATUS,
    CONDENSATE_STATUS_NO,
    CONDENSATE_STATUS_NC,
    EMERGENCY_SHUTOFF_NO,
    EMERGENCY_SHUTOFF_NC
}


fun thermistorAssociationDomainName(associationIndex: Int, equip: VavAdvancedHybridSystemEquip): Point? {
    when (associationIndex) {
        1 -> return equip.thermistorInput
        2 -> return equip.chilledWaterInletTemp
        3 -> return equip.chilledWaterOutletTemp
        4 -> return equip.supplyAirTemp
        5 -> return equip.mixedAirTemperature
        6 -> return equip.outsideTemperature
        7 -> return equip.returnAirTemperature
        8 -> return equip.spaceTemp
        9 -> return equip.supplyAirTemperature1
        10 -> return equip.supplyAirTemperature2
        11 -> return equip.supplyAirTemperature3
        12 -> return equip.dischargeFanAMStatus
        13 -> return equip.dischargeFanRunStatus
        14 -> return equip.dischargeFanTripStatus
        15 -> return equip.exhaustFanAMStatus
        16 -> return equip.uvAMStatus
        17 -> return equip.exhaustFanRunStatus
        18 -> return equip.exhaustFanTripStatus
        19 -> return equip.filterStatus1No
        20 -> return equip.filterStatus1Nc
        21 -> return equip.filterStatus2No
        22 -> return equip.filterStatus2Nc
        23 -> return equip.fireAlarmStatus
        24 -> return equip.fireDamperStatus1
        25 -> return equip.fireDamperStatus2
        26 -> return equip.fireDamperStatus3
        27 -> return equip.fireDamperStatus4
        28 -> return equip.fireDamperStatus5
        29 -> return equip.fireDamperStatus6
        30 -> return equip.fireDamperStatus7
        31 -> return equip.fireDamperStatus8
        32 -> return equip.highDifferentialPressureSwitch
        33 -> return equip.lowDifferentialPressureSwitch
        34 -> return equip.outsideFanRunStatus
        35 -> return equip.outsideFanTripStatus
        36 -> return equip.outsideFanAMStatus
        37 -> return equip.returnFanRunStatus
        38 -> return equip.returnFanTripStatus
        39 -> return equip.returnFanAMStatus
        40 -> return equip.uvRunStatus
        41 -> return equip.uvTripStatus
        42 -> return equip.condensateStatusNO
        43 -> return equip.condensateStatusNC
        44 -> return equip.emergencyShutoffNO
        45 -> return equip.emergencyShutoffNC
        else -> return null
    }
}

fun isMappedToEmergencyShutoff(associationIndex: Int): Boolean {
    return associationIndex == AdvancedAhuThermistorAssociationType.EMERGENCY_SHUTOFF_NC.ordinal
            || associationIndex == AdvancedAhuThermistorAssociationType.EMERGENCY_SHUTOFF_NO.ordinal
}


fun isUniversalMappedToEmergencyShutoff(associationIndex: Int): Boolean {
    return associationIndex == UniversalInputAssociationType.EMERGENCY_SHUTOFF_NC.ordinal
            || associationIndex == UniversalInputAssociationType.EMERGENCY_SHUTOFF_NO.ordinal
}


fun universalAssociationDomainName(associationIndex: Int, equip: ConnectModuleEquip): Point? {
    return when (associationIndex) { // TODO Add if all the associations are required
        UniversalInputAssociationType.EMERGENCY_SHUTOFF_NO.ordinal -> equip.emergencyShutoffNO
        UniversalInputAssociationType.EMERGENCY_SHUTOFF_NC.ordinal -> equip.emergencyShutoffNC
        else -> null
    }
}

enum class UniversalInputAssociationType {
    NOT_CONNECTED,
    VOLTAGE_INPUT,
    THERMISTOR_INPUT,
    BUILDING_STATIC_PRESSURE_SENSOR_1,
    BUILDING_STATIC_PRESSURE_SENSOR_2,
    BUILDING_STATIC_PRESSURE_SENSOR_10,
    CHILLED_WATER_INLET_TEMP,
    CHILLED_WATER_OUTLET_TEMP,
    COOLING_VALVE_POSITION_FEEDBACK,
    DISCHARGE_AIR_DAMPER_FEEDBACK,
    DISCHARGE_AIR_FLOW_SENSOR_10,
    DISCHARGE_AIR_FLOW_SENSOR_20,
    SUPPLY_AIR_HUMIDITY,
    DISCHARGE_AIR_PM25,
    SUPPLY_AIR_TEMP,
    DISCHARGE_FAN_VFD_FEEDBACK,
    DUCT_STATIC_PRESSURE_SENSOR1_1,
    DUCT_STATIC_PRESSURE_SENSOR1_2,
    DUCT_STATIC_PRESSURE_SENSOR1_10,
    DUCT_STATIC_PRESSURE_SENSOR2_1,
    DUCT_STATIC_PRESSURE_SENSOR2_2,
    DUCT_STATIC_PRESSURE_SENSOR2_10,
    DUCT_STATIC_PRESSURE_SENSOR3_1,
    DUCT_STATIC_PRESSURE_SENSOR3_2,
    DUCT_STATIC_PRESSURE_SENSOR3_10,
    HEATING_VALVE_POSITION_FEEDBACK,
    MINIMUM_OUTSIDE_AIR_DAMPER_FEEDBACK,
    MIXED_AIR_DAMPER_FEEDBACK,
    MIXED_AIR_TEMPERATURE,
    OUTSIDE_AIR_DAMPER_FEEDBACK,
    OUTSIDE_AIR_FLOW_SENSOR_10,
    OUTSIDE_AIR_FLOW_SENSOR_20,
    OUTSIDE_HUMIDITY,
    OUTSIDE_TEMPERATURE,
    RETURN_AIR_CO2,
    MIXED_AIR_CO2,
    RETURN_AIR_DAMPER_FEEDBACK,
    RETURN_AIR_HUMIDITY,
    RETURN_AIR_TEMPERATURE,
    SPACE_TEMP,
    MIXED_AIR_HUMIDITY,
    CURRENT_TX10,
    CURRENT_TX20,
    CURRENT_TX30,
    CURRENT_TX50,
    CURRENT_TX60,
    CURRENT_TX100,
    CURRENT_TX120,
    CURRENT_TX150,
    CURRENT_TX200,
    EXHAUST_FAN_VFD_FEEDBACK,
    OUTSIDE_FAN_VFD_FEEDBACK,
    RETURN_FAN_VFD_FEEDBACK,
    DISCHARGE_FAN_AM_STATUS,
    DISCHARGE_FAN_RUN_STATUS,
    DISCHARGE_FAN_TRIP_STATUS,
    EXHAUST_FAN_AM_STATUS,
    UV_AM_STATUS,
    EXHAUST_FAN_RUN_STATUS,
    EXHAUST_FAN_TRIP_STATUS,
    FILTER_STATUS1NO,
    FILTER_STATUS1NC,
    FILTER_STATUS2NO,
    FILTER_STATUS2NC,
    FIRE_ALARM_STATUS,
    FIRE_DAMPER_STATUS1,
    FIRE_DAMPER_STATUS2,
    FIRE_DAMPER_STATUS3,
    FIRE_DAMPER_STATUS4,
    FIRE_DAMPER_STATUS5,
    FIRE_DAMPER_STATUS6,
    FIRE_DAMPER_STATUS7,
    FIRE_DAMPER_STATUS8,
    HIGH_DIFFERENTIAL_PRESSURE_SWITCH,
    LOW_DIFFERENTIAL_PRESSURE_SWITCH,
    OUTSIDE_FAN_RUN_STATUS,
    OUTSIDE_FAN_TRIP_STATUS,
    OUTSIDE_FAN_AM_STATUS,
    RETURN_FAN_RUN_STATUS,
    RETURN_FAN_TRIP_STATUS,
    RETURN_FAN_AM_STATUS,
    UV_RUN_STATUS,
    UV_TRIP_STATUS,
    CONDENSATE_STATUS_NO,
    CONDENSATE_STATUS_NC,
    SUPPLY_AIR_TEMPERATURE1,
    SUPPLY_AIR_HUMIDITY1,
    SUPPLY_AIR_TEMPERATURE2,
    SUPPLY_AIR_HUMIDITY2,
    SUPPLY_AIR_TEMPERATURE3,
    SUPPLY_AIR_HUMIDITY3,
    EMERGENCY_SHUTOFF_NO,
    EMERGENCY_SHUTOFF_NC
}


/**
 * This function maps the association index to the domain name.
 * Should maintain the order as in the domainModel.
 */
fun relayAssociationToDomainName(associationIndex : Int) : String {
    return when(associationIndex) {
        0 -> DomainName.loadCoolingStage1
        1 -> DomainName.loadCoolingStage2
        2 -> DomainName.loadCoolingStage3
        3 -> DomainName.loadCoolingStage4
        4 -> DomainName.loadCoolingStage5
        5 -> DomainName.loadHeatingStage1
        6 -> DomainName.loadHeatingStage2
        7 -> DomainName.loadHeatingStage3
        8 -> DomainName.loadHeatingStage4
        9 -> DomainName.loadHeatingStage5
        10 -> DomainName.loadFanStage1
        11 -> DomainName.loadFanStage2
        12 -> DomainName.loadFanStage3
        13 -> DomainName.loadFanStage4
        14 -> DomainName.loadFanStage5
        15 -> DomainName.humidifierEnable
        16 -> DomainName.dehumidifierEnable
        17 -> DomainName.satCoolingStage1
        18 -> DomainName.satCoolingStage2
        19 -> DomainName.satCoolingStage3
        20 -> DomainName.satCoolingStage4
        21 -> DomainName.satCoolingStage5
        22 -> DomainName.satHeatingStage1
        23 -> DomainName.satHeatingStage2
        24 -> DomainName.satHeatingStage3
        25 -> DomainName.satHeatingStage4
        26 -> DomainName.satHeatingStage5
        27 -> DomainName.fanPressureStage1
        28 -> DomainName.fanPressureStage2
        29 -> DomainName.fanPressureStage3
        30 -> DomainName.fanPressureStage4
        31 -> DomainName.fanPressureStage5
        32 -> DomainName.occupiedEnable
        33 -> DomainName.fanEnable
        34 -> DomainName.ahuFreshAirFanRunCommand
        else -> throw IllegalArgumentException("Invalid association index $associationIndex")
    }
}

fun analogOutAssociationToDomainName(associationIndex: Int) : String {
    return when(associationIndex) {
        0 -> DomainName.pressureBasedFanControl
        1 -> DomainName.satBasedCoolingControl
        2 -> DomainName.satBasedHeatingControl
        3 -> DomainName.loadBasedCoolingControl
        4 -> DomainName.loadBasedHeatingControl
        5 -> DomainName.loadBasedFanControl
        6 -> DomainName.co2BasedDamperControl
        7 -> DomainName.compositeSignal
        else -> throw IllegalArgumentException("Invalid association index $associationIndex")
    }
}
fun connectAnalogOutAssociationToDomainName(associationIndex: Int) : String {
    return when(associationIndex) {
        0 -> DomainName.loadBasedCoolingControl
        1 -> DomainName.loadBasedHeatingControl
        2 -> DomainName.loadBasedFanControl
        3 -> DomainName.compositeSignal
        4 -> DomainName.co2BasedDamperControl
        else -> throw IllegalArgumentException("Invalid association index $associationIndex")
    }
}
fun relayAssociationDomainNameToType (domainName : String) : AdvancedAhuRelayAssociationType {
    when (domainName) {
        DomainName.loadCoolingStage1 , DomainName.loadCoolingStage2 , DomainName.loadCoolingStage3 ,
        DomainName.loadCoolingStage4 , DomainName.loadCoolingStage5 -> return AdvancedAhuRelayAssociationType.LOAD_COOLING

        DomainName.loadHeatingStage1 , DomainName.loadHeatingStage2 , DomainName.loadHeatingStage3 ,
        DomainName.loadHeatingStage4 , DomainName.loadHeatingStage5 -> return AdvancedAhuRelayAssociationType.LOAD_HEATING

        DomainName.loadFanStage1 , DomainName.loadFanStage2 , DomainName.loadFanStage3 ,
        DomainName.loadFanStage4 , DomainName.loadFanStage5 -> return AdvancedAhuRelayAssociationType.LOAD_FAN

        DomainName.humidifierEnable -> return AdvancedAhuRelayAssociationType.HUMIDIFIER
        DomainName.dehumidifierEnable -> return AdvancedAhuRelayAssociationType.DEHUMIDIFIER

        DomainName.satCoolingStage1 , DomainName.satCoolingStage2 , DomainName.satCoolingStage3 ,
        DomainName.satCoolingStage4 , DomainName.satCoolingStage5 -> return AdvancedAhuRelayAssociationType.SAT_COOLING

        DomainName.satHeatingStage1 , DomainName.satHeatingStage2 , DomainName.satHeatingStage3 ,
        DomainName.satHeatingStage4 , DomainName.satHeatingStage5 -> return AdvancedAhuRelayAssociationType.SAT_HEATING

        DomainName.fanPressureStage1 , DomainName.fanPressureStage2 , DomainName.fanPressureStage3 ,
        DomainName.fanPressureStage4 , DomainName.fanPressureStage5 -> return AdvancedAhuRelayAssociationType.FAN_PRESSURE

        DomainName.occupiedEnable -> return AdvancedAhuRelayAssociationType.OCCUPIED_ENABLE
        DomainName.fanEnable -> return AdvancedAhuRelayAssociationType.FAN_ENABLE
        DomainName.ahuFreshAirFanRunCommand -> return AdvancedAhuRelayAssociationType.AHU_FRESH_AIR_FAN_COMMAND
        else -> throw IllegalArgumentException("Invalid domain name $domainName")


    }
}

fun satControlIndexToDomainPoint(index: Int, equip: DomainEquip) : Point {
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    return when(index) {
        0 -> systemEquip.supplyAirTemperature1
        1 -> systemEquip.supplyAirTemperature2
        2 -> systemEquip.supplyAirTemperature3
        3 -> systemEquip.averageSat
        4 -> systemEquip.minSat
        5 -> systemEquip.maxSat
        else -> throw IllegalArgumentException("Invalid index $index")
    }
}

fun getPressureInputSensor(sensorType: DuctPressureSensorSource, systemEquip: VavAdvancedHybridSystemEquip): Point? {
    val (pressure, analogIn1, analogIn2) = getPressureMappings(systemEquip) // Reads if enabled only else it will be null

    fun getSensor(type: String): Point? {
        if (pressure != null && pressure.domainName.contains(type))
            return pressure
        if (analogIn1 != null && analogIn1.domainName.contains(type))
            return analogIn1
        if (analogIn2 != null && analogIn2.domainName.contains(type))
            return analogIn2
        return null
    }

    return when (sensorType) {
        DuctPressureSensorSource.DUCT_STATIC_PRESSURE_SENSOR_1 -> getSensor("1_")
        DuctPressureSensorSource.DUCT_STATIC_PRESSURE_SENSOR_2 -> getSensor("2_")
        DuctPressureSensorSource.DUCT_STATIC_PRESSURE_SENSOR_3 -> getSensor("3_")
        else -> null
    }
}

fun getPressureMappings(systemEquip: VavAdvancedHybridSystemEquip): Triple<Point?, Point?, Point?> {
    return Triple(
            getPointForPressureFromDomain(
                    getDomainPressure(systemEquip.sensorBus0PressureEnable.readDefaultVal() > 0, systemEquip.sensorBus0PressureAssociation.readDefaultVal().toInt()), systemEquip),
            getPointForPressureFromDomain(
                    getDomainForAnalogOut(systemEquip.analog1InputEnable.readDefaultVal() > 0, systemEquip.analog1InputAssociation.readDefaultVal().toInt()), systemEquip),
            getPointForPressureFromDomain(
                    getDomainForAnalogOut(systemEquip.analog2InputEnable.readDefaultVal() > 0, systemEquip.analog2InputAssociation.readDefaultVal().toInt()), systemEquip
            )
    )
}

fun pressureFanControlIndexToDomainPoint(index: Int, equip: DomainEquip): Point? {
    val systemEquip = equip as? VavAdvancedHybridSystemEquip
            ?: throw IllegalArgumentException("Invalid system equip type")

    val sourceOption = DuctPressureSensorSource.values().getOrNull(index)
            ?: return null

    return when (sourceOption) {
        DuctPressureSensorSource.DUCT_STATIC_PRESSURE_SENSOR_1,
        DuctPressureSensorSource.DUCT_STATIC_PRESSURE_SENSOR_2,
        DuctPressureSensorSource.DUCT_STATIC_PRESSURE_SENSOR_3 -> getPressureInputSensor(sourceOption, systemEquip)
        DuctPressureSensorSource.AVERAGE_PRESSURE -> systemEquip.averagePressure
        DuctPressureSensorSource.MIN_PRESSURE -> systemEquip.minPressure
        DuctPressureSensorSource.MAX_PRESSURE -> systemEquip.maxPressure
    }
}

fun co2DamperControlTypeToDomainPoint(index: Int, equip: DomainEquip) : Point {
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    return when(index) {
        0 -> systemEquip.zoneAvgCo2
        1 -> systemEquip.returnAirCo2
        2 -> systemEquip.mixedAirCo2
        else -> throw IllegalArgumentException("Invalid index $index")
    }
}

fun getDomainPointForName(name: String, systemEquip: VavAdvancedHybridSystemEquip): Point {
    return when (name) {
        DomainName.loadCoolingStage1 -> systemEquip.loadCoolingStage1
        DomainName.loadCoolingStage2 -> systemEquip.loadCoolingStage2
        DomainName.loadCoolingStage3 -> systemEquip.loadCoolingStage3
        DomainName.loadCoolingStage4 -> systemEquip.loadCoolingStage4
        DomainName.loadCoolingStage5 -> systemEquip.loadCoolingStage5
        DomainName.loadHeatingStage1 -> systemEquip.loadHeatingStage1
        DomainName.loadHeatingStage2 -> systemEquip.loadHeatingStage2
        DomainName.loadHeatingStage3 -> systemEquip.loadHeatingStage3
        DomainName.loadHeatingStage4 -> systemEquip.loadHeatingStage4
        DomainName.loadHeatingStage5 -> systemEquip.loadHeatingStage5
        DomainName.loadFanStage1 -> systemEquip.loadFanStage1
        DomainName.loadFanStage2 -> systemEquip.loadFanStage2
        DomainName.loadFanStage3 -> systemEquip.loadFanStage3
        DomainName.loadFanStage4 -> systemEquip.loadFanStage4
        DomainName.loadFanStage5 -> systemEquip.loadFanStage5
        DomainName.humidifierEnable -> systemEquip.humidifierEnable
        DomainName.dehumidifierEnable -> systemEquip.dehumidifierEnable
        DomainName.satCoolingStage1 -> systemEquip.satCoolingStage1
        DomainName.satCoolingStage2 -> systemEquip.satCoolingStage2
        DomainName.satCoolingStage3 -> systemEquip.satCoolingStage3
        DomainName.satCoolingStage4 -> systemEquip.satCoolingStage4
        DomainName.satCoolingStage5 -> systemEquip.satCoolingStage5
        DomainName.satHeatingStage1 -> systemEquip.satHeatingStage1
        DomainName.satHeatingStage2 -> systemEquip.satHeatingStage2
        DomainName.satHeatingStage3 -> systemEquip.satHeatingStage3
        DomainName.satHeatingStage4 -> systemEquip.satHeatingStage4
        DomainName.satHeatingStage5 -> systemEquip.satHeatingStage5
        DomainName.fanPressureStage1 -> systemEquip.fanPressureStage1
        DomainName.fanPressureStage2 -> systemEquip.fanPressureStage2
        DomainName.fanPressureStage3 -> systemEquip.fanPressureStage3
        DomainName.fanPressureStage4 -> systemEquip.fanPressureStage4
        DomainName.fanPressureStage5 -> systemEquip.fanPressureStage5
        DomainName.occupiedEnable -> systemEquip.occupiedEnable
        DomainName.fanEnable -> systemEquip.fanEnable
        DomainName.ahuFreshAirFanRunCommand -> systemEquip.ahuFreshAirFanRunCommand
        DomainName.pressureBasedFanControl -> systemEquip.pressureBasedFanControl
        DomainName.satBasedCoolingControl -> systemEquip.satBasedCoolingControl
        DomainName.satBasedHeatingControl -> systemEquip.satBasedHeatingControl
        DomainName.loadBasedCoolingControl -> systemEquip.loadBasedCoolingControl
        DomainName.loadBasedHeatingControl -> systemEquip.loadBasedHeatingControl
        DomainName.loadBasedFanControl -> systemEquip.loadBasedFanControl
        DomainName.co2BasedDamperControl -> systemEquip.co2BasedDamperControl
        DomainName.compositeSignal -> systemEquip.compositeSignal
        else -> throw IllegalArgumentException("Invalid point $name")
    }
}

fun getDomainPointForName(name: String, connectEquip: ConnectModuleEquip): Point {
    return when (name) {
        DomainName.loadCoolingStage1 -> connectEquip.loadCoolingStage1
        DomainName.loadCoolingStage2 -> connectEquip.loadCoolingStage2
        DomainName.loadCoolingStage3 -> connectEquip.loadCoolingStage3
        DomainName.loadCoolingStage4 -> connectEquip.loadCoolingStage4
        DomainName.loadCoolingStage5 -> connectEquip.loadCoolingStage5
        DomainName.loadHeatingStage1 -> connectEquip.loadHeatingStage1
        DomainName.loadHeatingStage2 -> connectEquip.loadHeatingStage2
        DomainName.loadHeatingStage3 -> connectEquip.loadHeatingStage3
        DomainName.loadHeatingStage4 -> connectEquip.loadHeatingStage4
        DomainName.loadHeatingStage5 -> connectEquip.loadHeatingStage5
        DomainName.loadFanStage1 -> connectEquip.loadFanStage1
        DomainName.loadFanStage2 -> connectEquip.loadFanStage2
        DomainName.loadFanStage3 -> connectEquip.loadFanStage3
        DomainName.loadFanStage4 -> connectEquip.loadFanStage4
        DomainName.loadFanStage5 -> connectEquip.loadFanStage5
        DomainName.humidifierEnable -> connectEquip.humidifierEnable
        DomainName.dehumidifierEnable -> connectEquip.dehumidifierEnable
        DomainName.occupiedEnable -> connectEquip.occupiedEnable
        DomainName.fanEnable -> connectEquip.fanEnable
        DomainName.ahuFreshAirFanRunCommand -> connectEquip.ahuFreshAirFanRunCommand
        DomainName.loadBasedCoolingControl -> connectEquip.loadBasedCoolingControl
        DomainName.loadBasedHeatingControl -> connectEquip.loadBasedHeatingControl
        DomainName.loadBasedFanControl -> connectEquip.loadBasedFanControl
        DomainName.co2BasedDamperControl -> connectEquip.co2BasedDamperControl
        DomainName.compositeSignal -> connectEquip.compositeSignal
        else -> throw IllegalArgumentException("Invalid point $name")
    }
}

fun getStageIndex(point: Point) : Int {
    return when (point.domainName) {
        DomainName.loadCoolingStage1, DomainName.loadHeatingStage1, DomainName.loadFanStage1,
        DomainName.satCoolingStage1, DomainName.satHeatingStage1, DomainName.fanPressureStage1 -> 0

        DomainName.loadCoolingStage2, DomainName.loadHeatingStage2, DomainName.loadFanStage2,
        DomainName.satCoolingStage2, DomainName.satHeatingStage2, DomainName.fanPressureStage2 -> 1

        DomainName.loadCoolingStage3, DomainName.loadHeatingStage3, DomainName.loadFanStage3,
        DomainName.satCoolingStage3, DomainName.satHeatingStage3, DomainName.fanPressureStage3 -> 2

        DomainName.loadCoolingStage4, DomainName.loadHeatingStage4, DomainName.loadFanStage4,
        DomainName.satCoolingStage4, DomainName.satHeatingStage4, DomainName.fanPressureStage4 -> 3

        DomainName.loadCoolingStage5, DomainName.loadHeatingStage5, DomainName.loadFanStage5,
        DomainName.satCoolingStage5, DomainName.satHeatingStage5, DomainName.fanPressureStage5 -> 4
        else -> {0}
    }
}

fun getDomainForAnalogOut(enabled: Boolean, association: Int): String? {
    if (enabled) {
        return when (association) {
            12 -> DomainName.ductStaticPressureSensor1_1
            13 -> DomainName.ductStaticPressureSensor1_2
            14 -> DomainName.ductStaticPressureSensor1_10
            15 -> DomainName.ductStaticPressureSensor2_1
            16 -> DomainName.ductStaticPressureSensor2_2
            17 -> DomainName.ductStaticPressureSensor2_10
            18 -> DomainName.ductStaticPressureSensor3_1
            19 -> DomainName.ductStaticPressureSensor3_2
            20 -> DomainName.ductStaticPressureSensor3_10
            else -> null
        }
    }
    return null
}


fun getDomainPressure(
        enabled: Boolean,
        association: Int
): String? {
    if (enabled) {
        return when(association) {
            1 -> DomainName.ductStaticPressureSensor1_2
            2 -> DomainName.ductStaticPressureSensor2_2
            3 -> DomainName.ductStaticPressureSensor3_2
            else -> null
        }
    }
    return null
}
fun getPointForPressureFromDomain(domainName: String?, systemEquip: VavAdvancedHybridSystemEquip): Point? {
    if (domainName != null) {
        return when (domainName) {
            DomainName.ductStaticPressureSensor1_1 -> systemEquip.ductStaticPressureSensor11
            DomainName.ductStaticPressureSensor1_2 -> systemEquip.ductStaticPressureSensor12
            DomainName.ductStaticPressureSensor1_10 -> systemEquip.ductStaticPressureSensor110
            DomainName.ductStaticPressureSensor2_1 -> systemEquip.ductStaticPressureSensor21
            DomainName.ductStaticPressureSensor2_2 -> systemEquip.ductStaticPressureSensor22
            DomainName.ductStaticPressureSensor2_10 -> systemEquip.ductStaticPressureSensor210
            DomainName.ductStaticPressureSensor3_1 -> systemEquip.ductStaticPressureSensor31
            DomainName.ductStaticPressureSensor3_2 -> systemEquip.ductStaticPressureSensor32
            DomainName.ductStaticPressureSensor3_10 -> systemEquip.ductStaticPressureSensor310
            else -> null
        }
    }
    return null
}


