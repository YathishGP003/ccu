package a75f.io.domain.devices

import a75f.io.api.haystack.RawPoint
import a75f.io.domain.api.*

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
    val currentTemp = PhysicalPoint(DomainName.currentTemp ,deviceRef)
    val humiditySensor = PhysicalPoint(DomainName.humiditySensor ,deviceRef)

    fun getPortsDomainNameWithPhysicalPoint() : HashMap<String, RawPoint> {
        val portsList = HashMap<String, RawPoint>()
        portsList[DomainName.relay1] = relay1.readPoint()
        portsList[DomainName.relay2] = relay2.readPoint()
        portsList[DomainName.relay3] = relay3.readPoint()
        portsList[DomainName.relay4] = relay4.readPoint()
        portsList[DomainName.relay5] = relay5.readPoint()
        portsList[DomainName.relay6] = relay6.readPoint()
        portsList[DomainName.relay7] = relay7.readPoint()
        portsList[DomainName.relay8] = relay8.readPoint()

        portsList[DomainName.analog1Out] = analog1Out.readPoint()
        portsList[DomainName.analog2Out] = analog2Out.readPoint()
        portsList[DomainName.analog3Out] = analog3Out.readPoint()
        portsList[DomainName.analog4Out] = analog4Out.readPoint()

        portsList[DomainName.analog1In] = analog1In.readPoint()
        portsList[DomainName.analog2In] = analog2In.readPoint()

        portsList[DomainName.th1In] = th1In.readPoint()
        portsList[DomainName.th2In] = th2In.readPoint()
        return portsList
    }
}