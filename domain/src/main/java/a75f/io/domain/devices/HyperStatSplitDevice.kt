package a75f.io.domain.devices

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint


class HyperStatSplitDevice(deviceRef: String) : DomainDevice(deviceRef) {


    var relay1 = PhysicalPoint(DomainName.relay1, deviceRef)
    var relay2 = PhysicalPoint(DomainName.relay2, deviceRef)
    var relay3 = PhysicalPoint(DomainName.relay3, deviceRef)
    var relay4 = PhysicalPoint(DomainName.relay4, deviceRef)
    var relay5 = PhysicalPoint(DomainName.relay5, deviceRef)
    var relay6 = PhysicalPoint(DomainName.relay6, deviceRef)
    var relay7 = PhysicalPoint(DomainName.relay7, deviceRef)
    var relay8 = PhysicalPoint(DomainName.relay8, deviceRef)

    var analog1Out = PhysicalPoint(DomainName.analog1Out, deviceRef)
    var analog2Out = PhysicalPoint(DomainName.analog2Out, deviceRef)
    var analog3Out = PhysicalPoint(DomainName.analog3Out, deviceRef)
    var analog4Out = PhysicalPoint(DomainName.analog4Out, deviceRef)

    var th1In = PhysicalPoint(DomainName.th1In, deviceRef)
    var th2In = PhysicalPoint(DomainName.th2In, deviceRef)

    var analog1In = PhysicalPoint(DomainName.analog1In, deviceRef)
    var analog2In = PhysicalPoint(DomainName.analog2In, deviceRef)

    var rssi = PhysicalPoint(DomainName.rssi, deviceRef)

}