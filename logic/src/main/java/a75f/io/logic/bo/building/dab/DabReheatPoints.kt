package a75f.io.logic.bo.building.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.SmartNode

fun createReheatType(equip : Equip, defaultVal : Double, hayStack : CCUHsApi) {
    val reheatType = Point.Builder()
        .setDisplayName(equip.displayName + "-reheatType")
        .setEquipRef(equip.id)
        .setSiteRef(equip.siteRef)
        .setRoomRef(equip.roomRef)
        .setFloorRef(equip.floorRef)
        .addMarker("config").addMarker("dab").addMarker("writable")
        .addMarker("zone").addMarker("reheat").addMarker("type").addMarker("sp")
        .setEnums("NotInstalled,ZeroToTenV,TwoToTenV,TenToTwoV,TenToZeroV,Pulse,OneStage,TwoStage")
        .setGroup(equip.group)
        .setTz(equip.tz)
        .build()

    val reheatTypeId = hayStack.addPoint(reheatType)
    hayStack.writeDefaultValById(reheatTypeId, defaultVal)
}

fun createReheatMinDamper(equip : Equip, defaultVal : Double, hayStack: CCUHsApi) {
    val reheatMinDamper = Point.Builder()
        .setDisplayName(equip.displayName + "-reheatMinDamper")
        .setEquipRef(equip.id)
        .setSiteRef(equip.siteRef)
        .setRoomRef(equip.roomRef)
        .setFloorRef(equip.floorRef)
        .addMarker("config").addMarker("dab").addMarker("writable")
        .addMarker("zone").addMarker("reheat").addMarker("min").addMarker("damper").addMarker("sp")
        .setMinVal("0").setMaxVal("100").setIncrementVal("1")
        .setGroup(equip.group)
        .setTz(equip.tz)
        .build()

    val reheatMinDamperId = hayStack.addPoint(reheatMinDamper)
    hayStack.writeDefaultValById(reheatMinDamperId, defaultVal)
}

fun createReheatPosPoint(equip : Equip, defaultVal : Double, hayStack: CCUHsApi) : String {
    val reheatPos = Point.Builder()
        .setDisplayName(equip.displayName + "-reheatCmd")
        .setEquipRef(equip.id)
        .setSiteRef(equip.siteRef)
        .setRoomRef(equip.roomRef)
        .setFloorRef(equip.floorRef)
        .setHisInterpolate("cov")
        .addMarker("dab").addMarker("his").addMarker("zone").addMarker("writable")
        .addMarker("cmd").addMarker("reheat")
        .setUnit("%")
        .setGroup(equip.group)
        .setTz(equip.tz)
        .build()

    val reheatPosId = hayStack.addPoint(reheatPos)
    hayStack.writeHisValById(reheatPosId, defaultVal)
    return reheatPosId
}

fun updateReheatType(reheatType : Double, minDamper : Double, equipRef : String, hayStack : CCUHsApi) {
    val currentReheatType = hayStack.readDefaultVal("reheat and type and equipRef == \"$equipRef\"")
    if (reheatType != currentReheatType) {
        val reheatDamper = hayStack.readEntity("reheat and min and damper and equipRef ==\"$equipRef\"")
        val reheatLoop = hayStack.readEntity("reheat and cmd and equipRef ==\"$equipRef\"")
        val dabEquip = Equip.Builder().setHashMap(hayStack.readMapById(equipRef)).build()
        if (reheatType > 0) {
            if (reheatDamper.isEmpty()) {
                createReheatMinDamper(dabEquip, minDamper, hayStack)
            }
            if (reheatLoop.isEmpty()) {
                val reheatPosId = createReheatPosPoint(dabEquip, 0.0, hayStack)
                when(reheatType.toInt() - 1) {
                    ReheatType.OneStage.ordinal ->
                        DeviceUtil.updatePhysicalPointRef(dabEquip.group.toInt(), Port.RELAY_ONE.name, reheatPosId)
                    ReheatType.TwoStage.ordinal -> {
                        DeviceUtil.updatePhysicalPointRef(
                            dabEquip.group.toInt(),
                            Port.RELAY_ONE.name,
                            reheatPosId
                        )
                        DeviceUtil.updatePhysicalPointRef(
                            dabEquip.group.toInt(),
                            Port.RELAY_TWO.name,
                            reheatPosId
                        )
                    }
                    else -> {
                        DeviceUtil.updatePhysicalPointRef(
                            dabEquip.group.toInt(),
                            Port.ANALOG_OUT_TWO.name,
                            reheatPosId)
                    }
                }
            } else {
                when (reheatType.toInt() - 1 ) {
                    ReheatType.OneStage.ordinal -> {
                        DeviceUtil.updatePhysicalPointRef(
                            dabEquip.group.toInt(),
                            Port.RELAY_ONE.name,
                            reheatLoop["id"].toString())
                        resetAO2ToSecondaryDamper(hayStack, equipRef, dabEquip.group.toInt())
                    }
                    ReheatType.TwoStage.ordinal -> {
                        DeviceUtil.updatePhysicalPointRef(
                            dabEquip.group.toInt(),
                            Port.RELAY_ONE.name,
                            reheatLoop["id"].toString())
                        DeviceUtil.updatePhysicalPointRef(
                            dabEquip.group.toInt(),
                            Port.RELAY_TWO.name,
                            reheatLoop["id"].toString())
                        resetAO2ToSecondaryDamper(hayStack, equipRef, dabEquip.group.toInt())
                    }
                }
            }
        } else {
            if (reheatDamper.isNotEmpty()) {
                hayStack.deleteEntityTree(reheatDamper["id"].toString())
            }
            if (reheatLoop.isNotEmpty()) {
                hayStack.deleteEntityTree(reheatLoop["id"].toString())
            }
        }
    }
}

fun resetAO2ToSecondaryDamper(hayStack: CCUHsApi, equipRef: String, nodeAddr : Int ) {
    val damperPosPoint = hayStack.readEntity("normalized and damper and cmd and secondary and equipRef == \"$equipRef\"");
    DeviceUtil.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_TWO.name, damperPosPoint["id"].toString())
}