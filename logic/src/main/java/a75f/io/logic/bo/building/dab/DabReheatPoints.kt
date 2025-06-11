package a75f.io.logic.bo.building.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.domain.util.ModelLoader.getSmartNodeDabModel
import a75f.io.domain.util.ModelLoader.getSmartNodeDevice
import a75f.io.logger.CcuLog
import a75f.io.logic.ANALOG_VALUE
import a75f.io.logic.L
import a75f.io.logic.REHEATCMDID
import a75f.io.logic.addBacnetTags
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective


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

fun createReheatPosPoint(equip : Equip, defaultVal : Double, hayStack: CCUHsApi , bacnetId : Int , bacnetType : String ) : String {
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
    addBacnetTags(reheatPos, REHEATCMDID, ANALOG_VALUE, equip.group.toInt())
    val reheatPosId = hayStack.addPoint(reheatPos)
    hayStack.writeHisValById(reheatPosId, defaultVal)
    return reheatPosId
}

fun updateReheatType(reheatType : Double, minDamper : Double, equipRef : String, hayStack : CCUHsApi,bacnetId: Int,bacnetType: String) {
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
                val reheatPosId = createReheatPosPoint(dabEquip, 0.0, hayStack,bacnetId,bacnetType)
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
                    else -> {
                        DeviceUtil.updatePhysicalPointRef(
                            dabEquip.group.toInt(),
                            Port.ANALOG_OUT_TWO.name,
                            reheatLoop["id"].toString())
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
    val damperPosPoint = hayStack.readEntity("normalized and damper and cmd and secondary and equipRef == \"$equipRef\"")
    DeviceUtil.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_TWO.name, damperPosPoint["id"].toString())
}

fun updateReheatTypeByDomain(
    msgObject: JsonObject,
    typeVal: Double,
    hayStack: CCUHsApi,
    address: Int,
    configPoint: Point
) {
    val who: String = msgObject.get("who").asString
    val level: Int = msgObject.get("level").asInt
    val value: Double = msgObject.get("val").asDouble
    val durationVal =
        if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asLong else 0
    //If duration shows it has already expired, then just write 1ms to force-expire it locally.
    val durationDiff = (if (durationVal == 0L) 0 else if ((durationVal - System.currentTimeMillis()) > 0) (durationVal - System.currentTimeMillis()) else 1).toDouble()
    hayStack.writePointLocal(configPoint.id, level, who, value, durationDiff)
    CcuLog.d(
        L.TAG_CCU_PUBNUB,
        "DAB : writePointFromJson - level: $level who: $who val: $value durationVal: $durationVal durationDiff: $durationDiff"
    )
    if (configPoint.markers.contains(Tags.REHEAT) && configPoint.markers.contains(Tags.TYPE)) {
            val profile = L.getProfile(address.toShort()) as DabProfile
            val equip = profile.equip
            var deviceModel = getSmartNodeDevice() as SeventyFiveFDeviceDirective
            var profileModel = getSmartNodeDabModel() as SeventyFiveFProfileDirective
            if (equip.markers.contains("helionode")) {
                deviceModel = getSmartNodeDevice() as SeventyFiveFDeviceDirective
                profileModel = getSmartNodeDabModel() as SeventyFiveFProfileDirective
            }
            val entityMapper = EntityMapper(profileModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            val config = profile.domainProfileConfiguration as DabProfileConfiguration
            config.reheatType.currentVal = typeVal
            val damper2Type =
                hayStack.readDefaultVal("point and domainName == \"" + DomainName.damper2Cmd + "\" and  group == \"" + configPoint.group + "\"")
           config.damper2Type.currentVal = damper2Type
            val equipBuilder = ProfileEquipBuilder(hayStack)
            equipBuilder.updateEquipAndPoints(
                config,
                getModelForDomainName(equip.domainName),
                equip.siteRef,
                equip.displayName, true
            )
            setOutputTypes(hayStack, config, deviceBuilder,deviceModel)
            DesiredTempDisplayMode.setModeType(configPoint.roomRef, hayStack)
    }
}

fun setOutputTypes(
    hayStack: CCUHsApi,
    config: DabProfileConfiguration,
    deviceBuilder: DeviceBuilder,
    deviceModel: SeventyFiveFDeviceDirective
) {
    val deviceEntity = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")
    val device = Device.Builder().setHDict(hayStack.readHDictById(deviceEntity["id"].toString())).build()
    val reheatType = config.reheatType.currentVal.toInt() - 1
    val reheatCmdPoint = hayStack.read("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.reheatCmd + "\"")
    val normalizedDamper1Cmd = hayStack.read("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.normalizedDamper1Cmd + "\"")
    val normalizedDamper2Cmd = hayStack.read("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.normalizedDamper2Cmd + "\"")
    val analog2Def = deviceModel.points.find { it.domainName == DomainName.analog2Out }
    val analog2out = getDevicePointDict(DomainName.analog2Out, deviceEntity["id"].toString(), hayStack)
    val relay1Def = deviceModel.points.find { it.domainName == DomainName.relay1 }
    val relay1 = getDevicePointDict(DomainName.relay1, deviceEntity["id"].toString(), hayStack)
    val relay2Def = deviceModel.points.find { it.domainName == DomainName.relay2 }
    val relay2 = getDevicePointDict(DomainName.relay2, deviceEntity["id"].toString(), hayStack)
    val analog1Def = deviceModel.points.find { it.domainName == DomainName.analog1Out }
    val analog1out = getDevicePointDict(DomainName.analog1Out, deviceEntity["id"].toString(), hayStack)
    val analog1InDef = deviceModel.points.find { it.domainName == DomainName.analog1In }
    val analog1In = getDevicePointDict(DomainName.analog1In, deviceEntity["id"].toString(), hayStack)
    val damperType1 = hayStack.readPointPriorityValByQuery("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.damper1Type + "\"")
    val damperType2 = hayStack.readPointPriorityValByQuery("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.damper2Type + "\"")


    val analog2OpEnabled = reheatType in listOf(
        ReheatType.ZeroToTenV.ordinal,
        ReheatType.TwoToTenV.ordinal,
        ReheatType.TenToTwov.ordinal,
        ReheatType.TenToZeroV.ordinal,
        ReheatType.Pulse.ordinal
    )

    setPortConfiguration(
        rawPoint = analog1out,
        analogType = getDamperTypeString(config.damper1Type.currentVal.toInt()),
        port = Port.ANALOG_OUT_ONE.toString(),
        portEnabled = (damperType1 != 4.0),
        pointRef = if(damperType1 != 4.0) normalizedDamper1Cmd["id"].toString() else  null
    )
    deviceBuilder.updatePoint(analog1Def!!, config, device, analog1out)

    setPortConfiguration(
        rawPoint = analog2out,
        analogType = if(reheatType == -1) getDamperTypeString(config.damper2Type.currentVal.toInt()) else getReheatTypeString(config),
        port = Port.ANALOG_OUT_TWO.toString(),
        portEnabled = if((damperType2 == 4.0 && reheatType == -1)) false else if(damperType2 == 4.0 && analog2OpEnabled) true else if(damperType2 == 4.0 && reheatType == 0) false  else if(damperType2 != 4.0) true else false,
        pointRef = if(damperType2 == 4.0 && analog2OpEnabled) normalizedDamper2Cmd["id"].toString() else  if((damperType2 == 4.0 && reheatType >=5)) reheatCmdPoint["id"].toString() else if(damperType2 != 4.0) normalizedDamper2Cmd["id"].toString() else null
    )
    deviceBuilder.updatePoint(analog2Def!!, config, device, analog2out)

    setPortConfiguration(
        rawPoint = relay1,
        analogType = null,
        port = Port.RELAY_ONE.toString(),
        portEnabled = false,
        pointRef = null
    )
    deviceBuilder.updatePoint(relay1Def!!, config, device, relay1)

    setPortConfiguration(
        rawPoint = relay2,
        analogType = null,
        port = Port.RELAY_TWO.toString(),
        portEnabled = false,
        pointRef = null
    )
    deviceBuilder.updatePoint(relay2Def!!, config, device, relay2)

    when (reheatType) {
        ReheatType.OneStage.ordinal -> {
            setPortConfiguration(
                rawPoint = relay1,
                analogType = "Relay N/C",
                port = Port.RELAY_ONE.toString(),
                portEnabled = true,
                pointRef = reheatCmdPoint["id"].toString()
            )
            deviceBuilder.updatePoint(relay1Def!!, config, device, relay1)
        }
        ReheatType.TwoStage.ordinal -> {
            setPortConfiguration(
                rawPoint = relay1,
                analogType = "Relay N/C",
                port = Port.RELAY_ONE.toString(),
                portEnabled = true,
                pointRef = reheatCmdPoint["id"].toString()
            )
            deviceBuilder.updatePoint(relay1Def!!, config, device, relay1)

            setPortConfiguration(
                rawPoint = relay2,
                analogType = "Relay N/C",
                port = Port.RELAY_TWO.toString(),
                portEnabled = true,
                pointRef = reheatCmdPoint["id"].toString(),
            )
            deviceBuilder.updatePoint(relay2Def!!, config, device, relay2)
        }
    }
    analog1In["analogType"] = getDamperTypeString(config.damper1Type.currentVal.toInt());
    deviceBuilder.updatePoint(analog1InDef!!, config, device, analog1In)
}

private fun getDamperTypeString(currentVal : Int) : String {
    return when(currentVal) {
        0 -> "0-10v"
        1 -> "2-10v"
        2 -> "10-2v"
        3 -> "10-0v"
        4 -> "Smart Damper"
        5 -> "0-5v"
        else -> { "0-10v" }
    }
}

fun getReheatTypeString(config: DabProfileConfiguration): String {
    return when (config.reheatType.currentVal.toInt()) {
        1 -> "0-10v"
        2 -> "2-10v"
        3 -> "10-2v"
        4 -> "10-0v"
        5 -> "Pulsed Electric"
        else -> {
            "0-10v"
        }
    }
}

fun getDevicePointDict(domainName: String, deviceId: String, hayStack: CCUHsApi): HashMap<Any, Any> {
  return  hayStack.read("point and physical and deviceRef == \"" + deviceId + "\" and domainName == \"" + domainName + "\"");
}

fun setPortConfiguration(
    rawPoint: HashMap<Any, Any>,
    analogType: String?,
    port: String?,
    portEnabled: Boolean,
    pointRef: String?
) {
    if(analogType != null) rawPoint["analogType"] = analogType else rawPoint.remove("analogType")
    if(port != null) rawPoint["port"] = port else rawPoint.remove("port")
    rawPoint["portEnabled"] = portEnabled
    if(pointRef != null) rawPoint["pointRef"] = pointRef else rawPoint.remove("pointRef")
}