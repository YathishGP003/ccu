package a75f.io.domain.devices

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint

class ConnectDevice (deviceRef : String) : DomainDevice (deviceRef) {
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

    val universal1In = PhysicalPoint(DomainName.universal1In ,deviceRef)
    val universal2In = PhysicalPoint(DomainName.universal2In ,deviceRef)
    val universal3In = PhysicalPoint(DomainName.universal3In ,deviceRef)
    val universal4In = PhysicalPoint(DomainName.universal4In ,deviceRef)
    val universal5In = PhysicalPoint(DomainName.universal5In ,deviceRef)
    val universal6In = PhysicalPoint(DomainName.universal6In ,deviceRef)
    val universal7In = PhysicalPoint(DomainName.universal7In ,deviceRef)
    val universal8In = PhysicalPoint(DomainName.universal8In ,deviceRef)

}