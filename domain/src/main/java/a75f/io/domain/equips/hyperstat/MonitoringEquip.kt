package a75f.io.domain.equips.hyperstat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class MonitoringEquip(equipRef : String) : HyperStatEquip(equipRef){
    val tempOffset = Point(DomainName.temperatureOffset, equipRef)

    val thermistor1Enabled = Point(DomainName.thermistor1InputEnable, equipRef)
    val thermistor2Enabled = Point(DomainName.thermistor2InputEnable, equipRef)
    val analogIn1Enabled = Point(DomainName.analog1InputEnable, equipRef)
    val analogIn2Enabled = Point(DomainName.analog2InputEnable, equipRef)

    val thermistor1Association = Point(DomainName.thermistor1InputAssociation, equipRef)
    val thermistor2Association = Point(DomainName.thermistor2InputAssociation, equipRef)
    val analogIn1Association = Point(DomainName.analog1InputAssociation, equipRef)
    val analogIn2Association = Point(DomainName.analog2InputAssociation, equipRef)

}