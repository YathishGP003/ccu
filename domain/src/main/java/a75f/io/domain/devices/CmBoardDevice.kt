package a75f.io.domain.devices

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point

class CmBoardDevice (deviceRef : String) : DomainDevice (deviceRef) {
    val relay1 = PhysicalPoint(DomainName.relay1 ,deviceRef)
    val relay2 = PhysicalPoint(DomainName.relay2 ,deviceRef)
    val relay3 = PhysicalPoint(DomainName.relay3 ,deviceRef)
    val relay4 = PhysicalPoint(DomainName.relay4 ,deviceRef)
    val relay5 = PhysicalPoint(DomainName.relay5 ,deviceRef)
    val relay6 = PhysicalPoint(DomainName.relay6 ,deviceRef)
    val relay7 = PhysicalPoint(DomainName.relay7 ,deviceRef)
    val relay8 = PhysicalPoint(DomainName.relay8 ,deviceRef)

    val analog1Out = PhysicalPoint(DomainName.analog1Out ,deviceRef)
    val analog2Out = PhysicalPoint(DomainName.analog2Out ,deviceRef)
    val analog3Out = PhysicalPoint(DomainName.analog3Out ,deviceRef)
    val analog4Out = PhysicalPoint(DomainName.analog4Out ,deviceRef)

    val th1In = PhysicalPoint(DomainName.th1In ,deviceRef)
    val th2In = PhysicalPoint(DomainName.th2In ,deviceRef)
    val analog1In = PhysicalPoint(DomainName.analog1In ,deviceRef)
    val analog2In = PhysicalPoint(DomainName.analog2In ,deviceRef)
}