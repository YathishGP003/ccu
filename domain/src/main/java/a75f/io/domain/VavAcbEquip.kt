package a75f.io.domain

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.VavEquip

class VavAcbEquip (equipRef: String): VavEquip(equipRef) {

    val valveType = Point(DomainName.valveType, equipRef)
    val thermistor2Type = Point(DomainName.thermistor2Type, equipRef)

    val condensateNC = Point(DomainName.condensateNC, equipRef)
    val condensateNO = Point(DomainName.condensateNO, equipRef)

    val chwValveCmd = Point(DomainName.chilledWaterValve, equipRef)
    val chwShutOffValve = Point(DomainName.chilledWaterShutOffValve, equipRef)

    val relayActivationHysteresis = Point(DomainName.relayActivationHysteresis, equipRef)

}