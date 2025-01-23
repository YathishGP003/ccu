package a75f.io.domain.devices

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint

/**
 * Created by Manjunath K on 21-10-2024.
 */

class HyperStatDevice (deviceRef : String) : DomainDevice (deviceRef) {

    val relay1 = PhysicalPoint(DomainName.relay1 ,deviceRef)
    val relay2 = PhysicalPoint(DomainName.relay2 ,deviceRef)
    val relay3 = PhysicalPoint(DomainName.relay3 ,deviceRef)
    val relay4 = PhysicalPoint(DomainName.relay4 ,deviceRef)
    val relay5 = PhysicalPoint(DomainName.relay5 ,deviceRef)
    val relay6 = PhysicalPoint(DomainName.relay6 ,deviceRef)

    val analog1Out = PhysicalPoint(DomainName.analog1Out ,deviceRef)
    val analog2Out = PhysicalPoint(DomainName.analog2Out ,deviceRef)
    val analog3Out = PhysicalPoint(DomainName.analog3Out ,deviceRef)

    val th1In = PhysicalPoint(DomainName.th1In ,deviceRef)
    val th2In = PhysicalPoint(DomainName.th2In ,deviceRef)

    val analog1In = PhysicalPoint(DomainName.analog1In ,deviceRef)
    val analog2In = PhysicalPoint(DomainName.analog2In ,deviceRef)

    val rssi = PhysicalPoint(DomainName.rssi ,deviceRef)
    val firmwareVersion = PhysicalPoint(DomainName.firmwareVersion ,deviceRef)
    val currentTemp = PhysicalPoint(DomainName.currentTemp ,deviceRef)
    val desiredTemp = PhysicalPoint(DomainName.desiredTemp ,deviceRef)
    val occupancySensor = PhysicalPoint(DomainName.occupancySensor ,deviceRef)
    val illuminanceSensor = PhysicalPoint(DomainName.illuminanceSensor ,deviceRef)
    val humiditySensor = PhysicalPoint(DomainName.humiditySensor ,deviceRef)
    val soundSensor = PhysicalPoint(DomainName.soundSensor ,deviceRef)
    val co2Equivalent = PhysicalPoint(DomainName.co2EquivalentSensor ,deviceRef)
    val co2Sensor = PhysicalPoint(DomainName.co2Sensor ,deviceRef)
    val pm25Sensor = PhysicalPoint(DomainName.pm25Sensor ,deviceRef)
    val pm10Sensor = PhysicalPoint(DomainName.pm10Sensor ,deviceRef)
    val pressureSensor = PhysicalPoint(DomainName.pressureSensor ,deviceRef)
    val uviSensor = PhysicalPoint(DomainName.uviSensor ,deviceRef)
    val vocSensor = PhysicalPoint(DomainName.vocSensor ,deviceRef)

    fun getRelays() : List<PhysicalPoint> {
        return listOf(relay1, relay2, relay3, relay4, relay5, relay6)
    }

    fun getAnalogOuts() : List<PhysicalPoint> {
        return listOf(analog1Out, analog2Out, analog3Out)
    }
}