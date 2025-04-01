package a75f.io.domain.devices

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint

/**
 * Created by Manjunath K on 13-01-2025.
 */

class MyStatDevice(deviceRef : String) : DomainDevice (deviceRef)  {

    val relay1 = PhysicalPoint(DomainName.relay1 ,deviceRef)
    val relay2 = PhysicalPoint(DomainName.relay2 ,deviceRef)
    val relay3 = PhysicalPoint(DomainName.relay3 ,deviceRef)
    val relay4 = PhysicalPoint(DomainName.relay4 ,deviceRef)

    val analog1Out = PhysicalPoint(DomainName.analog1Out ,deviceRef)
    val universal1In = PhysicalPoint(DomainName.universal1In ,deviceRef)

    val rssi = PhysicalPoint(DomainName.rssi ,deviceRef)
    val firmwareVersion = PhysicalPoint(DomainName.firmwareVersion ,deviceRef)
    val currentTemp = PhysicalPoint(DomainName.currentTemp ,deviceRef)
    val desiredTemp = PhysicalPoint(DomainName.desiredTemp ,deviceRef)
    val occupancySensor = PhysicalPoint(DomainName.occupancySensor ,deviceRef)
    val humiditySensor = PhysicalPoint(DomainName.humiditySensor ,deviceRef)
    val co2Sensor = PhysicalPoint(DomainName.co2Sensor ,deviceRef)
    val pressureSensor = PhysicalPoint(DomainName.pressureSensor ,deviceRef)

    fun getRelays() : List<PhysicalPoint> = listOf(relay1, relay2, relay3, relay4)

}
