package a75f.io.device.mesh.mystat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getDomainEquip
import a75f.io.domain.api.Domain.getEquipDevices
import a75f.io.domain.api.DomainName
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip

/**
 * Created by Manjunath K on 13-01-2025.
 */


fun getMyStatDevice(nodeAddress: Int): HashMap<Any, Any>? {
    return CCUHsApi.getInstance()
        .readEntity("domainName == \"" + DomainName.mystatDevice + "\" and addr == \"" + nodeAddress + "\"")
}

fun getMyStatDomainDevice(deviceRef: String, equipRef: String): MyStatDevice {
    val devices = getEquipDevices()
    if (devices.containsKey(equipRef)) {
        return devices[equipRef] as MyStatDevice
    } else {
        Domain.devices[equipRef] = MyStatDevice(deviceRef)
    }
    return devices[equipRef] as MyStatDevice
}

fun getMyStatEquip(equipRef: String): MyStatEquip? {
    val equip = getDomainEquip(equipRef)
    if (equip is MyStatCpuEquip) return equip
    if (equip is MyStatHpuEquip) return equip
    if (equip is MyStatPipe2Equip) return equip
    return null
}