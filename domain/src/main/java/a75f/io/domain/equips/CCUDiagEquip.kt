package a75f.io.domain.equips

import a75f.io.api.haystack.Tags


import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class CCUDiagEquip (equipRef : String) : DomainEquip(equipRef) {

    val batteryLevel = Point(DomainName.batteryLevel, equipRef)
    val chargingStatus = Point(DomainName.chargingStatus, equipRef)
    val powerConnected = Point(DomainName.powerConnected, equipRef)
    val wifiRssi = Point(DomainName.wifiRssi, equipRef)
    val ccuHeartbeat = Point(DomainName.ccuHeartbeat, equipRef)
    val wifiLinkSpeed = Point(DomainName.wifiLinkSpeed, equipRef)
    val wifiSignalStrength = Point(DomainName.wifiSignalStrength, equipRef)
    val availableMemory = Point(DomainName.availableMemory, equipRef)
    val totalMemory = Point(DomainName.totalMemory, equipRef)
    val isLowMemory = Point(DomainName.isLowMemory, equipRef)
    val serialConnection = Point(DomainName.serialConnection, equipRef)
    val appRestart = Point(DomainName.appRestart, equipRef)
    val appVersion = Point(DomainName.appVersion, equipRef)
    val otaStatus = Point(DomainName.otaStatus, equipRef)
    val safeModeStatus = Point(DomainName.safeModeStatus, equipRef)
    val availableInternalDiskStorage = Point(DomainName.availableInternalDiskStorage, equipRef)
    val autoCommissioning = Point(DomainName.autoCommissioning, equipRef)
    val migrationVersion = Point(DomainName.migrationVersion, equipRef)
    val remoteSessionStatus = Point(DomainName.remoteSessionStatus, equipRef)
    val bacnetAppVersion = Point(DomainName.bacnetAppVersion, equipRef)
    val remoteAccessAppVersion = Point(DomainName.remoteAccessAppVersion, equipRef)
    val homeAppVersion = Point(DomainName.homeAppVersion, equipRef)


    private val diagEquipMap = hayStack.readMapById(equipRef)
    val displayName = diagEquipMap[Tags.DIS].toString()
    val siteRef = diagEquipMap[Tags.SITEREF].toString()
    val tz = diagEquipMap[Tags.TZ].toString()
}