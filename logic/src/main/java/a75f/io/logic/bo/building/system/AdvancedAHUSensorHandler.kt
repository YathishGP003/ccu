package a75f.io.logic.bo.building.system

import a75f.io.domain.equips.DomainEquip
import a75f.io.logic.bo.building.system.util.getAdvancedAhuSystemEquip


fun updatePressureSensorDerivedPoints() {
    val systemEquip = getAdvancedAhuSystemEquip()
    val (pressure, analogIn1, analogIn2) = getPressureMappings(systemEquip)
    val availableSensors = listOfNotNull(pressure?.readHisVal(), analogIn1?.readHisVal(), analogIn2?.readHisVal())
    if (availableSensors.isNotEmpty()) {
        if (systemEquip.averagePressure.pointExists()) {
            systemEquip.averagePressure.writeHisVal(availableSensors.average())
        }
        if (systemEquip.minPressure.pointExists()) {
            systemEquip.minPressure.writeHisVal(availableSensors.min())
        }
        if (systemEquip.maxPressure.pointExists()) {
            systemEquip.maxPressure.writeHisVal(availableSensors.max())
        }
    }
}

fun updateTemperatureSensorDerivedPoints(equip: DomainEquip) {
    val systemEquip = getAdvancedAhuSystemEquip()
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