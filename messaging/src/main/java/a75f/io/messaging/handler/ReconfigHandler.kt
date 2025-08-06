package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import android.util.Log
import org.projecthaystack.HDict

/**
 * Created by Manjunath K on 13-11-2024.
 */


fun updateConfiguration(domainName: String, pointValue: Double, config: ProfileConfiguration) {

    CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: received $pointValue $domainName $config")
    config.getEnableConfigs().forEach {
        if (it.domainName.equals(domainName)) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: updated config")
            setEnabledStatus(it, pointValue)
            return
        }
    }

    config.getAssociationConfigs().forEach {
        if (it.domainName.equals(domainName)) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: updated association")
            setAssociation(it, pointValue)
            return
        }
    }

    config.getValueConfigs().forEach {
        if (it.domainName.equals(domainName)) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: updated getValueConfigs")
            it.currentVal = pointValue
            return
        }
    }

}

private fun setEnabledStatus(config: EnableConfig, pointValue: Double) {
    config.enabled = pointValue == 1.0
    Log.i("Reconfiguration", "setEnabledStatus: received $pointValue ${config.domainName} ${config.enabled}")
}

private fun setAssociation(association: AssociationConfig, pointValue: Double) {
    association.associationVal = pointValue.toInt()
    Log.i("Reconfiguration", "setEnabledStatus: received $pointValue ${association.domainName} ${pointValue.toInt()}")
}

fun setPortConfiguration(
    nodeAddress: Int,
    relays: Map<String, Boolean>,
    analogOuts: Map<String, Pair<Boolean, String>>,
) {

    val hayStack = CCUHsApi.getInstance()
    val device = getDeviceByNodeAddress(nodeAddress)
    val deviceRef = device.id() ?: run {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Device not found for node address: $nodeAddress")
        return
    }

    fun getPort(portName: String): RawPoint.Builder? {
        val port =
            hayStack.readHDict("point and deviceRef == \"$deviceRef\" and domainName == \"$portName\"")
        if (port.isEmpty) return null
        return RawPoint.Builder().setHDict(port)
    }

    fun updatePort(port: RawPoint.Builder, type: String, isWritable: Boolean) {
        port.setType(type)
        if (isWritable) {
            port.addMarker(Tags.WRITABLE)
            port.addMarker(Tags.UNUSED)
        } else {
            port.removeMarkerIfExists(Tags.WRITABLE)
            port.removeMarkerIfExists(Tags.UNUSED)
            hayStack.clearAllAvailableLevelsInPoint(port.build().id)
        }
        val buildPoint = port.build()
        hayStack.updatePoint(buildPoint, buildPoint.id)
    }

    relays.forEach { (relayName, externallyMapped) ->
        val port = getPort(relayName)
        if (port != null) updatePort(
            port, OutputRelayActuatorType.NormallyOpen.displayName, externallyMapped
        )
    }

    analogOuts.forEach { (analogName, config) ->
        val port = getPort(analogName)
        if (port != null) {
            updatePort(port, config.second, config.first)
        }
    }

}

fun getDeviceByNodeAddress(nodeAddress: Int): HDict {
    val hayStack = CCUHsApi.getInstance()
    return hayStack.readHDict("device and addr == \"$nodeAddress\"");
}