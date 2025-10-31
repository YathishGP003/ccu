package a75f.io.device.mesh

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.device.pcn
import a75f.io.device.pcn.PCN_Settings_t
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.pcn.PCNUtil
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.getPCNConfiguration
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.getPCNForZone

class PCNSettingsUtil {
}

/**
 * Builds and returns a `PCN_Settings_t` message that encapsulates the base configuration
 * of a PCN device and its associated connect modules for a given zone.
 * This is used to define network communication parameters, profile,
 * and slave configurations for Modbus servers.
 *
 * Steps performed:
 *
 * 1. **Retrieve PCN & Configuration**
 *    - Fetch the PCN device for the given zone using `getPCNForZone`.
 *    - Retrieve PCN configuration metadata via `getPCNConfiguration`.
 *
 * 2. **Initialize Settings Builder**
 *    - Set the profile map to `PROFILE_MAP_PCN`.
 *    - Define RS485 communication parameters (baud rate, parity, stop bits).
 *
 * 3. **Add PCN Native Device Config (Slave ID = 100)**
 *    - If the PCN configuration has equipment (`pcnEquips` is not empty),
 *      a base slave configuration is created for the PCN itself.
 *    - Diagnostic information is enabled by default.
 *
 * 4. **Add Connect Module Configurations**
 *    - Iterate over the `connectModuleList` in PCN configuration.
 *    - For each connect module:
 *      - Assign its `serverId` as the slave ID.
 *      - Enable diagnostic information collection.
 *      - Insert configurations after the PCN config if present.
 *
 * 5. **Build and Return**
 *    - Finalize the `PCN_Settings_t` object via `builder.build()`.
 *
 * @param zone The string zone identifier for which the PCN settings are being created.
 * @return A complete `PCN_Settings_t` object containing communication and connect settings.
 */
fun getPcnSettingsMessage(zone: String): PCN_Settings_t {
    fun fromIndex(value: Double): pcn.Rs485BaudRate_t? {
        val index = value.toInt()
        return pcn.Rs485BaudRate_t.values().getOrNull(index)
    }
    val pcnDevice = getPCNForZone(zone, CCUHsApi.getInstance())
    var pcnConfig = getPCNConfiguration(pcnDevice[Tags.ID].toString())
    var pcnModelAdded = false

    // Build the settings message
    val builder = PCN_Settings_t.newBuilder()
        .setProfileMap2(pcn.profileMap2.PROFILE_MAP_PCN)
        .setRs485Config(
            pcn.RS485_Config_t.newBuilder()
                .setBaudRate(pcn.Rs485BaudRate_t.values().getOrNull(pcnConfig.baudRate.doubleValue.toInt()))
                .setParity(pcn.Rs485Parity_t.values().getOrNull(pcnConfig.parity.doubleValue.toInt()))
                .setStopBits(pcn.Rs485StopBits_t.values().getOrNull(pcnConfig.stopBits.doubleValue.toInt()))
                .build()
        )

    if(pcnConfig.pcnEquips.isNotEmpty()) {
        pcnModelAdded = true
        builder.addConnectConfigurationT(
        0, pcn.ConnectModbusServersConfiguration_t.newBuilder()
            .setSlaveID(100) // PCN always has slave ID 100
            .setDiagnosticInformationNeeded(true)
            .build()
        )}

    val indexOffset = if (pcnModelAdded) 1 else 0

    // Add connect configurations dynamically
    pcnConfig.connectModuleList.forEachIndexed { index, cn ->

        builder.addConnectConfigurationT(
            index + indexOffset,
            pcn.ConnectModbusServersConfiguration_t.newBuilder()
                .setSlaveID(cn.serverId)
                .setDiagnosticInformationNeeded(true)
                .build()
        )
    }

    return builder.build()
}
