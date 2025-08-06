package a75f.io.domain.devices

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point


class HyperStatSplitDevice(device: String) : DomainDevice(device) {


    var relay1 = PhysicalPoint(DomainName.relay1, device)
    var relay2 = PhysicalPoint(DomainName.relay2, device)
    var relay3 = PhysicalPoint(DomainName.relay3, device)
    var relay4 = PhysicalPoint(DomainName.relay4, device)
    var relay5 = PhysicalPoint(DomainName.relay5, device)
    var relay6 = PhysicalPoint(DomainName.relay6, device)
    var relay7 = PhysicalPoint(DomainName.relay7, device)
    var relay8 = PhysicalPoint(DomainName.relay8, device)

    var analog1Out = PhysicalPoint(DomainName.analog1Out, device)
    var analog2Out = PhysicalPoint(DomainName.analog2Out, device)
    var analog3Out = PhysicalPoint(DomainName.analog3Out, device)
    var analog4Out = PhysicalPoint(DomainName.analog4Out, device)

    var th1In = PhysicalPoint(DomainName.th1In, device)
    var th2In = PhysicalPoint(DomainName.th2In, device)

    var analog1In = PhysicalPoint(DomainName.analog1In, device)
    var analog2In = PhysicalPoint(DomainName.analog2In, device)

    var rssi = PhysicalPoint(DomainName.rssi, device)
    var firmwareVersion = Point(DomainName.firmwareVersion, device)
    var currentTemp = Point(DomainName.currentTemp, device)
    var desiredTemp = Point(DomainName.desiredTemp, device)
    var occupancySensor = Point(DomainName.occupancySensor, device)

}