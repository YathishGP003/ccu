package a75f.io.domain.devices

import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.logger.CcuLog
import org.projecthaystack.HDateTime

class ConnectNodeDevice(deviceRef: String) : DomainDevice(deviceRef) {
    val firmwareVersion = PhysicalPoint(DomainName.firmwareVersion, deviceRef)
    val otaStatus = PhysicalPoint(DomainName.otaStatus, deviceRef)
    val heartBeat = PhysicalPoint(DomainName.heartBeat, deviceRef)
    val sequenceErrorCode = PhysicalPoint(DomainName.sequenceErrorCode, deviceRef)
    val sequenceLastRunTime = PhysicalPoint(DomainName.sequenceLastRunTime, deviceRef)
    val sequenceLongRunTime = PhysicalPoint(DomainName.sequenceLongRunTime, deviceRef)
    val otaStatusSequence = PhysicalPoint(DomainName.otaStatusSequence, deviceRef)
    val sequenceUpdateState = PhysicalPoint(DomainName.sequenceUpdateState, deviceRef)
    val sequenceUpdateError = PhysicalPoint(DomainName.sequenceUpdateError, deviceRef)
    val sequenceRunCount = PhysicalPoint(DomainName.sequenceRunCount, deviceRef)
    val sequenceStatus = PhysicalPoint(DomainName.sequenceStatus, deviceRef)
    val sequenceMetadataName = PhysicalPoint(DomainName.sequenceMetadataName, deviceRef)
    val sequenceMetadataLength = PhysicalPoint(DomainName.sequenceMetadataLength, deviceRef)
    val sequenceMetadataIdentity = PhysicalPoint(DomainName.sequenceMetadataIdentity, deviceRef)
    val sequenceVersion = PhysicalPoint(DomainName.sequenceVersion, deviceRef)


    fun updateDeliveryTime(deliveryReceipt: HDateTime) {
        // Safely get the point
        val point = try {
            otaStatusSequence.readPoint()
        } catch (e: Exception) {
            CcuLog.e("CONNECT_NODE", "Error reading point for device $deviceRef: ${e.message}")
            return
        }
        val id = otaStatusSequence.id
        // Update point and log
        point.tags["deliveryDateTime"] = deliveryReceipt
        hayStack.updatePoint(point, id)
        CcuLog.i("CONNECT_NODE", "Delivery time updated for device $deviceRef: $deliveryReceipt")
    }

    fun updateConfirmedTime(confirmedReceipt: HDateTime?) {
        // Safely get the point
        val point = try {
            otaStatusSequence.readPoint()
        } catch (e: Exception) {
            CcuLog.e("CONNECT_NODE", "Error reading point for device $deviceRef: ${e.message}")
            return
        }

        val id = otaStatusSequence.id
        // Update point and log
        point.tags["confirmedDateTime"] = confirmedReceipt
        hayStack.updatePoint(point, id)
        CcuLog.i("CONNECT_NODE", "Confirmed time updated for device $deviceRef: $confirmedReceipt")
    }
}