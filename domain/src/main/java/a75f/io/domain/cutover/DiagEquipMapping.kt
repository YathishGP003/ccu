package a75f.io.domain.cutover

import a75f.io.domain.api.DomainName

object DiagEquipMapping {
    val entries = mapOf(
        "batteryLevel" to DomainName.batteryLevel,
        "chargingStatus" to DomainName.chargingStatus,
        "powerConnected" to DomainName.powerConnected,
        "wifiRssi" to DomainName.wifiRssi,
        "ccuHeartbeat" to DomainName.ccuHeartbeat,
        "wifiLinkSpeed" to DomainName.wifiLinkSpeed,
        "wifiSignalStrength" to DomainName.wifiSignalStrength,
        "availableMemory" to DomainName.availableMemory,
        "totalMemory" to DomainName.totalMemory,
        "isLowMemory" to DomainName.isLowMemory,
        "serialConnection" to DomainName.serialConnection,
        "appRestart" to DomainName.appRestart,
        "appVersion" to DomainName.appVersion,
        "otaStatus" to DomainName.otaStatus,
        "safeModeStatus" to DomainName.safeModeStatus,
        "availableInternalDiskStorage" to DomainName.availableInternalDiskStorage,
        "autoCommissioning" to DomainName.autoCommissioning,
        "migrationVersion" to DomainName.migrationVersion,
        "remoteSessionStatus" to DomainName.remoteSessionStatus,
        "bacnetAppVersion" to DomainName.bacnetAppVersion,
        "remoteAccessAppVersion" to DomainName.remoteAccessAppVersion,
        "homeAppVersion" to DomainName.homeAppVersion,
    )
}
