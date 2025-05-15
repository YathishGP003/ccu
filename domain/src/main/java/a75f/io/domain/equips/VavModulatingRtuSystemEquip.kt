package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

open class VavModulatingRtuSystemEquip(equipRef : String) : VavSystemEquip(equipRef) {
    //Base points
    val analog1OutputEnable = Point(DomainName.analog1OutputEnable ,equipRef)
    val analog2OutputEnable = Point(DomainName.analog2OutputEnable ,equipRef)
    val analog3OutputEnable = Point(DomainName.analog3OutputEnable ,equipRef)
    val analog4OutputEnable = Point(DomainName.analog4OutputEnable ,equipRef)
    val relay3OutputEnable = Point(DomainName.relay3OutputEnable ,equipRef)
    val relay7OutputEnable = Point(DomainName.relay7OutputEnable ,equipRef)

    //dependant points
    val coolingSignal = Point(DomainName.coolingSignal ,equipRef)
    val fanSignal = Point(DomainName.fanSignal ,equipRef)
    val heatingSignal = Point(DomainName.heatingSignal ,equipRef)
    val outsideAirDamper = Point(DomainName.outsideAirDamper ,equipRef)
    val fanEnable = Point(DomainName.fanEnable ,equipRef)

    //associated points
    val humidifier = Point(DomainName.humidifierEnable ,equipRef)
    val dehumidifier = Point(DomainName.dehumidifierEnable ,equipRef)

    //dependant points
    val analog1MinCooling = Point(DomainName.analog1MinCooling, equipRef)
    val analog1MaxCooling = Point(DomainName.analog1MaxCooling, equipRef)
    val analog2MinStaticPressure = Point(DomainName.analog2MinStaticPressure, equipRef)
    val analog2MaxStaticPressure = Point(DomainName.analog2MaxStaticPressure, equipRef)
    val analog3MinHeating = Point(DomainName.analog3MinHeating, equipRef)
    val analog3MaxHeating = Point(DomainName.analog3MaxHeating, equipRef)
    val analog4MinOutsideDamper = Point(DomainName.analog4MinOutsideDamper, equipRef)
    val analog4MaxOutsideDamper = Point(DomainName.analog4MaxOutsideDamper, equipRef)

    //Association point
    val relay7OutputAssociation = Point(DomainName.relay7OutputAssociation , equipRef)
    val co2Threshold = Point(DomainName.co2Threshold, equipRef)
}