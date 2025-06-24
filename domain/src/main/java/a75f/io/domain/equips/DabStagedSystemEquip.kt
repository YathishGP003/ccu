package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

open class DabStagedSystemEquip(equipRef : String) : DabSystemEquip(equipRef) {

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

    val conditioningStages = ConditioningStages(equipRef)
}