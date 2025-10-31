package a75f.io.domain.devices

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint

class PCNDevice (deviceRef: String) : ConnectNodeDevice(deviceRef) {
    val baudRate = PhysicalPoint(DomainName.modbusBaudRate, deviceRef)
    val parity = PhysicalPoint(DomainName.modbusParity, deviceRef)
    val dataBits = PhysicalPoint(DomainName.modbusDataBits, deviceRef)
    val stopBits = PhysicalPoint(DomainName.modbusStopBits, deviceRef)
}