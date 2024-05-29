package a75f.io.logic.bo.building.system

import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.devices.ConnectDevice
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip

/**
 * Created by Manjunath K on 22-05-2024.
 */


enum class AdvancedAhuAnalogOutAssociationTypeConnect {
    LOAD_COOLING, LOAD_HEATING, LOAD_FAN, COMPOSITE_SIGNAL, CO2_DAMPER
}

fun getConnectRelayAssociationMap(equip: DomainEquip) : Map<Point, Point> {
    val associations: MutableMap<Point, Point> = HashMap()
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    if (systemEquip.connectEquip1.equipRef.contentEquals("null")) {
        return associations
    }
    associations[systemEquip.connectEquip1.relay1OutputEnable] = systemEquip.connectEquip1.relay1OutputAssociation
    associations[systemEquip.connectEquip1.relay2OutputEnable] = systemEquip.connectEquip1.relay2OutputAssociation
    associations[systemEquip.connectEquip1.relay3OutputEnable] = systemEquip.connectEquip1.relay3OutputAssociation
    associations[systemEquip.connectEquip1.relay4OutputEnable] = systemEquip.connectEquip1.relay4OutputAssociation
    associations[systemEquip.connectEquip1.relay5OutputEnable] = systemEquip.connectEquip1.relay5OutputAssociation
    associations[systemEquip.connectEquip1.relay6OutputEnable] = systemEquip.connectEquip1.relay6OutputAssociation
    associations[systemEquip.connectEquip1.relay7OutputEnable] = systemEquip.connectEquip1.relay7OutputAssociation
    associations[systemEquip.connectEquip1.relay8OutputEnable] = systemEquip.connectEquip1.relay8OutputAssociation
    return associations
}


fun getConnectAnalogAssociationMap(equip: DomainEquip): Map<Point, Point> {
    val associations: MutableMap<Point, Point> = HashMap()
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    if (systemEquip.connectEquip1.equipRef.contentEquals("null")) {
        return associations
    }
    associations[systemEquip.connectEquip1.analog1OutputEnable] = systemEquip.connectEquip1.analog1OutputAssociation
    associations[systemEquip.connectEquip1.analog2OutputEnable] = systemEquip.connectEquip1.analog2OutputAssociation
    associations[systemEquip.connectEquip1.analog3OutputEnable] = systemEquip.connectEquip1.analog3OutputAssociation
    associations[systemEquip.connectEquip1.analog4OutputEnable] = systemEquip.connectEquip1.analog4OutputAssociation

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


