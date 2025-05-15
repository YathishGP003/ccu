package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

open class VavStagedSystemEquip(equipRef : String) : VavSystemEquip(equipRef) {

    val relay1OutputEnable = Point(DomainName.relay1OutputEnable ,equipRef)
    val relay2OutputEnable = Point(DomainName.relay2OutputEnable ,equipRef)
    val relay3OutputEnable = Point(DomainName.relay3OutputEnable ,equipRef)
    val relay4OutputEnable = Point(DomainName.relay4OutputEnable ,equipRef)
    val relay5OutputEnable = Point(DomainName.relay5OutputEnable ,equipRef)
    val relay6OutputEnable = Point(DomainName.relay6OutputEnable ,equipRef)
    val relay7OutputEnable = Point(DomainName.relay7OutputEnable ,equipRef)

    val relay1OutputAssociation = Point(DomainName.relay1OutputAssociation ,equipRef)
    val relay2OutputAssociation = Point(DomainName.relay2OutputAssociation ,equipRef)
    val relay3OutputAssociation = Point(DomainName.relay3OutputAssociation ,equipRef)
    val relay4OutputAssociation = Point(DomainName.relay4OutputAssociation ,equipRef)
    val relay5OutputAssociation = Point(DomainName.relay5OutputAssociation ,equipRef)
    val relay6OutputAssociation = Point(DomainName.relay6OutputAssociation ,equipRef)
    val relay7OutputAssociation = Point(DomainName.relay7OutputAssociation ,equipRef)

    val coolingStage1 = Point(DomainName.coolingStage1 ,equipRef)
    val coolingStage2 = Point(DomainName.coolingStage2 ,equipRef)
    val coolingStage3 = Point(DomainName.coolingStage3 ,equipRef)
    val coolingStage4 = Point(DomainName.coolingStage4 ,equipRef)
    val coolingStage5 = Point(DomainName.coolingStage5 ,equipRef)

    val heatingStage1 = Point(DomainName.heatingStage1 ,equipRef)
    val heatingStage2 = Point(DomainName.heatingStage2 ,equipRef)
    val heatingStage3 = Point(DomainName.heatingStage3 ,equipRef)
    val heatingStage4 = Point(DomainName.heatingStage4 ,equipRef)
    val heatingStage5 = Point(DomainName.heatingStage5 ,equipRef)

    val fanStage1 = Point(DomainName.fanStage1 ,equipRef)
    val fanStage2 = Point(DomainName.fanStage2 ,equipRef)
    val fanStage3 = Point(DomainName.fanStage3 ,equipRef)
    val fanStage4 = Point(DomainName.fanStage4 ,equipRef)
    val fanStage5 = Point(DomainName.fanStage5 ,equipRef)

    val humidifierEnable = Point(DomainName.humidifierEnable ,equipRef)
    val dehumidifierEnable = Point(DomainName.dehumidifierEnable ,equipRef)
    val co2Threshold = Point(DomainName.co2Threshold, equipRef)


}