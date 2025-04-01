package a75f.io.logic.bo.building.system

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.devices.ConnectDevice
import a75f.io.domain.equips.ConnectModuleEquip

/**
 * Created by Manjunath K on 22-05-2024.
 */


enum class AdvancedAhuAnalogOutAssociationTypeConnect {
    LOAD_COOLING, LOAD_HEATING, LOAD_FAN, COMPOSITE_SIGNAL, CO2_DAMPER, OAO_DAMPER, RETURN_DAMPER
}

fun getConnectRelayAssociationMap(connectEquip1: ConnectModuleEquip) : Map<Point, Point> {
    val associations: MutableMap<Point, Point> = HashMap()
    if (connectEquip1.equipRef.contentEquals("null")) {
        return associations
    }

    associations[connectEquip1.relay1OutputEnable] = connectEquip1.relay1OutputAssociation
    associations[connectEquip1.relay2OutputEnable] = connectEquip1.relay2OutputAssociation
    associations[connectEquip1.relay3OutputEnable] = connectEquip1.relay3OutputAssociation
    associations[connectEquip1.relay4OutputEnable] = connectEquip1.relay4OutputAssociation
    associations[connectEquip1.relay5OutputEnable] = connectEquip1.relay5OutputAssociation
    associations[connectEquip1.relay6OutputEnable] = connectEquip1.relay6OutputAssociation
    associations[connectEquip1.relay7OutputEnable] = connectEquip1.relay7OutputAssociation
    associations[connectEquip1.relay8OutputEnable] = connectEquip1.relay8OutputAssociation
    return associations
}


fun getConnectAnalogAssociationMap(connectEquip1: ConnectModuleEquip): Map<Point, Point> {
    val associations: MutableMap<Point, Point> = HashMap()

    if (connectEquip1.equipRef.contentEquals("null")) {
        return associations
    }
    associations[connectEquip1.analog1OutputEnable] = connectEquip1.analog1OutputAssociation
    associations[connectEquip1.analog2OutputEnable] = connectEquip1.analog2OutputAssociation
    associations[connectEquip1.analog3OutputEnable] = connectEquip1.analog3OutputAssociation
    associations[connectEquip1.analog4OutputEnable] = connectEquip1.analog4OutputAssociation

    return associations
}

fun getConnectRelayLogicalPhysicalMap(connectEquip : ConnectModuleEquip, device: ConnectDevice): Map<Point, PhysicalPoint> {
    val map: MutableMap<Point, PhysicalPoint> = HashMap()
    if (connectEquip.equipRef.contentEquals("null")) {
        return map
    }
    map[connectEquip.relay1OutputEnable] = device.relay1
    map[connectEquip.relay2OutputEnable] = device.relay2
    map[connectEquip.relay3OutputEnable] = device.relay3
    map[connectEquip.relay4OutputEnable] = device.relay4
    map[connectEquip.relay5OutputEnable] = device.relay5
    map[connectEquip.relay6OutputEnable] = device.relay6
    map[connectEquip.relay7OutputEnable] = device.relay7
    map[connectEquip.relay8OutputEnable] = device.relay8
    return map
}

fun getConnectAnalogOutLogicalPhysicalMap(connectEquip : ConnectModuleEquip, device: ConnectDevice): Map<Point, PhysicalPoint> {
    val map: MutableMap<Point, PhysicalPoint> = HashMap()
    if (connectEquip.equipRef.contentEquals("null")) {
        return map
    }
    map[connectEquip.analog1OutputEnable] = device.analog1Out
    map[connectEquip.analog2OutputEnable] = device.analog2Out
    map[connectEquip.analog3OutputEnable] = device.analog3Out
    map[connectEquip.analog4OutputEnable] = device.analog4Out
    return map
}



fun connectRelayAssociationToDomainName(associationIndex : Int) : String {
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
        17 -> DomainName.occupiedEnable
        18 -> DomainName.fanEnable
        19 -> DomainName.ahuFreshAirFanRunCommand
        20 -> DomainName.exhaustFanStage1
        21 -> DomainName.exhaustFanStage2
        else -> throw IllegalArgumentException("Invalid association index $associationIndex")
    }
}

