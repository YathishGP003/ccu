package a75f.io.logic.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags

fun addEquipScheduleStatusPoint(externalEquip: Equip, equipId: String) {
    val hayStack = CCUHsApi.getInstance()
    val isModbusEquip = externalEquip.markers.contains(Tags.MODBUS) && !externalEquip.tags.containsKey(Tags.BACNET_DEVICE_ID)
    val equipMarkerTag = when {
        externalEquip.markers.contains(Tags.CONNECTMODULE) -> Tags.CONNECTMODULE
        isModbusEquip -> Tags.MODBUS
        else -> Tags.BACNET
    }
    val equipScheduleStatus = Point.Builder()
        .setDisplayName(externalEquip.displayName + "-equipScheduleStatus")
        .setEquipRef(equipId)
        .setSiteRef(externalEquip.siteRef)
        .setRoomRef(externalEquip.roomRef)
        .setFloorRef(externalEquip.floorRef).setHisInterpolate("cov")
        .addMarker("scheduleStatus").addMarker(equipMarkerTag).addMarker("zone").addMarker("writable")
        .setGroup(externalEquip.group)
        .setTz(externalEquip.tz)
        .setKind(Kind.STRING)
        .build()
    val equipScheduleStatusId = hayStack.addPoint(equipScheduleStatus)
    hayStack.writeDefaultValById(equipScheduleStatusId, "NA")
}
