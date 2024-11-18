package a75f.io.logic.bo.building.sse

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.logic.bo.building.dab.getDevicePointDict
import a75f.io.logic.bo.building.definitions.Port
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective

class SseEquipUtil {
    companion object {
        fun updatePortConfiguration(
            hayStack: CCUHsApi,
            config: SseProfileConfiguration,
            deviceBuilder: DeviceBuilder,
            deviceModel: SeventyFiveFDeviceDirective
        ) {
            val deviceEntityId =
                hayStack.readEntity("device and addr == \"${config.nodeAddress}\"")["id"].toString()
            val device = Device.Builder().setHDict(hayStack.readHDictById(deviceEntityId)).build()

            fun updateDevicePoint(
                domainName: String, port: String, analogType: Any,
                isPortEnabled: Boolean = false
            ) {
                val pointDef = deviceModel.points.find { it.domainName == domainName }
                pointDef?.let {
                    val pointDict = getDevicePointDict(domainName, deviceEntityId, hayStack).apply {
                        this["port"] = port
                        this["analogType"] = analogType
                        this["portEnabled"] = isPortEnabled
                    }
                    deviceBuilder.updatePoint(it, config, device, pointDict)
                }
            }

            // Analog In 1
            if (config.analog1InEnabledState.enabled) {
                when (config.analog1InAssociation.associationVal) {
                    0 -> updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 8, true)
                    1 -> updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 9,true)
                    else -> updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 10,true)
                }
            }

            // Relay 1
            if (config.relay1EnabledState.enabled)
                updateDevicePoint(DomainName.relay1, Port.RELAY_ONE.name, "Relay N/O", true)
            else
                updateDevicePoint(DomainName.relay1, Port.RELAY_ONE.name, "Relay N/C", false)

            // Relay 2
            if (config.relay2EnabledState.enabled)
                updateDevicePoint(DomainName.relay2, Port.RELAY_TWO.name, "Relay N/O", true)
            else
                updateDevicePoint(DomainName.relay2, Port.RELAY_TWO.name, "Relay N/C", false)

            // Thermistor 1
            if (config.th1EnabledState.enabled) {
                updateDevicePoint(DomainName.th1In, Port.TH1_IN.name, 0, true)
            } else {
                updateDevicePoint(DomainName.th1In, Port.TH1_IN.name, 0, false)
            }

            // Thermistor 2
            if (config.th2EnabledState.enabled) {
                updateDevicePoint(DomainName.th2In, Port.TH2_IN.name, 0, true)
            } else {
                updateDevicePoint(DomainName.th2In, Port.TH2_IN.name, 0, false)
            }
        }
    }
}