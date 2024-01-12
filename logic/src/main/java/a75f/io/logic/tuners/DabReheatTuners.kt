package a75f.io.logic.tuners

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.building.definitions.Units


fun createReheatTempOffsetTuner(hayStack : CCUHsApi, siteRef : String, equipRef : String, equipDis : String,
                                tz : String, defaultTag : String, roomRef : String?, floorRef : String?) {

    val reheatTempOffset = Point.Builder()
        .setDisplayName(SystemTuners.getDisplayNameFromVariation("$equipDis-DAB-reheatTempOffset"))
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("temp").addMarker("offset").addMarker("sp")
        .setMinVal("0").setMaxVal("10").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.FAHRENHEIT)
    roomRef?.let { reheatTempOffset.setRoomRef(it) }
    floorRef?.let { reheatTempOffset.setFloorRef(it) }
    val reheatTempOffsetId = hayStack.addPoint(reheatTempOffset.build())
    hayStack.writePointForCcuUser(
        reheatTempOffsetId,
        TunerConstants.VAV_DEFAULT_VAL_LEVEL,
        TunerConstants.DAB_REHEAT_OFFSET_DEFAULT_VAL,
        0
    )
    hayStack.writeHisValById(reheatTempOffsetId, TunerConstants.DAB_REHEAT_OFFSET_DEFAULT_VAL)

    if (defaultTag != Tags.DEFAULT) {
        BuildingTunerUtil.updateTunerLevels(reheatTempOffsetId, roomRef, hayStack)
    }
}

fun createRelayActivationHysteresisTuner(hayStack : CCUHsApi, siteRef : String, equipRef : String, equipDis : String,
                                tz : String, defaultTag : String, roomRef : String?, floorRef : String?) {

    val relayActivationHysteresis = Point.Builder()
        .setDisplayName(SystemTuners.getDisplayNameFromVariation("$equipDis-DAB-reheatRelayActivationHysteresis"))
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("relay").addMarker("activation").addMarker("hysteresis")
        .addMarker("sp")
        .setMinVal("0").setMaxVal("100").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.PERCENTAGE)
    roomRef?.let { relayActivationHysteresis.setRoomRef(it) }
    floorRef?.let { relayActivationHysteresis.setFloorRef(it) }
    val relayActivationHysteresisId = hayStack.addPoint(relayActivationHysteresis.build())
    hayStack.writePointForCcuUser(
        relayActivationHysteresisId,
        TunerConstants.VAV_DEFAULT_VAL_LEVEL,
        TunerConstants.DAB_RELAY_HYSTERESIS_DEFAULT_VAL,
        0
    )
    hayStack.writeHisValById(relayActivationHysteresisId, TunerConstants.DAB_RELAY_HYSTERESIS_DEFAULT_VAL)

    if (defaultTag != Tags.DEFAULT) {
        BuildingTunerUtil.updateTunerLevels(relayActivationHysteresisId, roomRef, hayStack)
    }

}

fun createTemperatureProportionalRangeTuner(hayStack : CCUHsApi, siteRef : String, equipRef : String, equipDis : String,
                                         tz : String, defaultTag : String, roomRef : String?, floorRef : String?) {

    val temperatureProportionalRange = Point.Builder()
        .setDisplayName(SystemTuners.getDisplayNameFromVariation("$equipDis-DAB-reheatTemperatureProportionalRange"))
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("temp").addMarker("pspread")
        .addMarker("sp")
        .setMinVal("0").setMaxVal("100").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.FAHRENHEIT)
    roomRef?.let { temperatureProportionalRange.setRoomRef(it) }
    floorRef?.let { temperatureProportionalRange.setFloorRef(it) }
    val temperatureProportionalRangeId = hayStack.addPoint(temperatureProportionalRange.build())
    hayStack.writePointForCcuUser(
        temperatureProportionalRangeId,
        TunerConstants.VAV_DEFAULT_VAL_LEVEL,
        TunerConstants.DEFAULT_PROPORTIONAL_SPREAD,
        0
    )
    hayStack.writeHisValById(temperatureProportionalRangeId, TunerConstants.DEFAULT_PROPORTIONAL_SPREAD)

    if (defaultTag != Tags.DEFAULT) {
        BuildingTunerUtil.updateTunerLevels(temperatureProportionalRangeId, roomRef, hayStack)
    }
}

fun createTemperatureIntegralTimeTuner(hayStack : CCUHsApi, siteRef : String, equipRef : String, equipDis : String,
                                            tz : String, defaultTag : String, roomRef : String?, floorRef : String?) {

    val temperatureIntegralTime = Point.Builder()
        .setDisplayName(SystemTuners.getDisplayNameFromVariation("$equipDis-DAB-reheatTemperatureIntegralTime"))
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("temp").addMarker("itimeout")
        .addMarker("sp")
        .setMinVal("0").setMaxVal("100").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.MINUTES)
    roomRef?.let { temperatureIntegralTime.setRoomRef(it) }
    floorRef?.let { temperatureIntegralTime.setFloorRef(it) }
    val temperatureIntegralTimeId = hayStack.addPoint(temperatureIntegralTime.build())
    hayStack.writePointForCcuUser(
        temperatureIntegralTimeId,
        TunerConstants.VAV_DEFAULT_VAL_LEVEL,
        TunerConstants.DEFAULT_INTEGRAL_TIMEOUT,
        0
    )
    hayStack.writeHisValById(temperatureIntegralTimeId, TunerConstants.DEFAULT_INTEGRAL_TIMEOUT)

    if (defaultTag != Tags.DEFAULT) {
        BuildingTunerUtil.updateTunerLevels(temperatureIntegralTimeId, roomRef, hayStack)
    }
}

fun createKFactorTuner(hayStack : CCUHsApi, siteRef : String, equipRef : String, equipDis : String,
                                       tz : String, defaultTag : String, type :String, roomRef : String?, floorRef : String?) {

    val typeName = if (type == "igain")  "Integral" else "Proportional";
    val temperatureIntegralTime = Point.Builder()
        .setDisplayName(SystemTuners.getDisplayNameFromVariation("$equipDis-DAB-reheat${typeName}KFactor"))
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker(type)
        .addMarker("sp")
        .setMinVal("0.1").setMaxVal("1").setIncrementVal("0.1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz)
    roomRef?.let { temperatureIntegralTime.setRoomRef(it) }
    floorRef?.let { temperatureIntegralTime.setFloorRef(it) }
    val temperatureIntegralTimeId = hayStack.addPoint(temperatureIntegralTime.build())
    hayStack.writePointForCcuUser(
        temperatureIntegralTimeId,
        TunerConstants.VAV_DEFAULT_VAL_LEVEL,
        TunerConstants.DEFAULT_PROPORTIONAL_GAIN,
        0
    )
    hayStack.writeHisValById(temperatureIntegralTimeId, TunerConstants.DEFAULT_PROPORTIONAL_GAIN)

    if (defaultTag != Tags.DEFAULT) {
        BuildingTunerUtil.updateTunerLevels(temperatureIntegralTimeId, roomRef, hayStack)
    }
}

fun createDefaultReheatTuners(hayStack: CCUHsApi, buildingTunerEquip : Equip) {
    if (hayStack.readEntity("default and reheat and temp and offset and not vav").isNotEmpty()) {
        return
    }
    createReheatTempOffsetTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, null, null)
    createRelayActivationHysteresisTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, null, null)
    createTemperatureProportionalRangeTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, null, null)
    createTemperatureIntegralTimeTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, null, null)
    createKFactorTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, "igain", null, null)
    createKFactorTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, "pgain" ,null, null)
}

fun createEquipReheatTuners(hayStack: CCUHsApi, equip : Equip) {
    createReheatTempOffsetTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef, equip.floorRef)
    createRelayActivationHysteresisTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef, equip.floorRef)
    createTemperatureProportionalRangeTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef, equip.floorRef)
    createTemperatureIntegralTimeTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef, equip.floorRef)
    createKFactorTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, "igain", equip.roomRef, equip.floorRef)
    createKFactorTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, "pgain" ,equip.roomRef, equip.floorRef)
}
