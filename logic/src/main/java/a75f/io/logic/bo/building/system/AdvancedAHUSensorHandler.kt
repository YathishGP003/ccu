package a75f.io.logic.bo.building.system

import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip


fun updatePressureSensorDerivedPoints(equip: DomainEquip) {
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    if (systemEquip.averagePressure.pointExists()) {
        val avgPressure = (systemEquip.ductStaticPressureSensor12.readHisVal() +
                systemEquip.ductStaticPressureSensor22.readHisVal() +
                systemEquip.ductStaticPressureSensor32.readHisVal()) / 3
        systemEquip.averagePressure.writeHisVal(avgPressure)
    }
    if (systemEquip.minPressure.pointExists()) {
        val minPressure = minOf(systemEquip.ductStaticPressureSensor12.readHisVal(),
            systemEquip.ductStaticPressureSensor22.readHisVal(),
            systemEquip.ductStaticPressureSensor32.readHisVal())
        systemEquip.minPressure.writeHisVal(minPressure)
    }
    if (systemEquip.maxPressure.pointExists()) {
        val maxPressure = maxOf(systemEquip.ductStaticPressureSensor12.readHisVal(),
            systemEquip.ductStaticPressureSensor22.readHisVal(),
            systemEquip.ductStaticPressureSensor32.readHisVal())
        systemEquip.maxPressure.writeHisVal(maxPressure)
    }
}

fun updateTemperatureSensorDerivedPoints(equip: DomainEquip) {
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    if (systemEquip.averageSat.pointExists()) {
        val avgSat = (systemEquip.supplyAirTemperature1.readHisVal() +
                systemEquip.supplyAirTemperature2.readHisVal() +
                systemEquip.supplyAirTemperature3.readHisVal()) / 3
        systemEquip.averageSat.writeHisVal(avgSat)
    }
    if (systemEquip.minSat.pointExists()) {
        val minSat = minOf(systemEquip.supplyAirTemperature1.readHisVal(),
            systemEquip.supplyAirTemperature2.readHisVal(),
            systemEquip.supplyAirTemperature3.readHisVal())
        systemEquip.minSat.writeHisVal(minSat)
    }
    if (systemEquip.maxSat.pointExists()) {
        val maxSat = maxOf(systemEquip.supplyAirTemperature1.readHisVal(),
            systemEquip.supplyAirTemperature2.readHisVal(),
            systemEquip.supplyAirTemperature3.readHisVal())
        systemEquip.maxSat.writeHisVal(maxSat)
    }
}