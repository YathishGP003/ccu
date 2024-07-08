package a75f.io.domain

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.VavEquip

class VavAcbEquip (equipRef: String): VavEquip(equipRef) {

    val valveType = Point(DomainName.valveType, equipRef)
    val thermistor2Type = Point(DomainName.thermistor2Type, equipRef)

    val relay1OutputEnable = Point(DomainName.relay1OutputEnable, equipRef)
    val relay1OutputAssociation = Point(DomainName.relay1OutputAssociation, equipRef)
    val relay2OutputEnable = Point(DomainName.relay2OutputEnable, equipRef)
    val relay2OutputAssociation = Point(DomainName.relay2OutputAssociation, equipRef)

    val condensateNC = Point(DomainName.condensateNC, equipRef)
    val condensateNO = Point(DomainName.condensateNO, equipRef)

    val chwValveCmd = Point(DomainName.chilledWaterValve, equipRef)
    val chilledWaterValveIsolationCmdPointNO = Point(DomainName.chilledWaterValveIsolationCmdPointNO, equipRef)
    val chilledWaterValveIsolationCmdPointNC = Point(DomainName.chilledWaterValveIsolationCmdPointNC, equipRef)

    val relayActivationHysteresis = Point(DomainName.relayActivationHysteresis, equipRef)

}