package a75f.io.logic.tuners

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.building.definitions.Units


fun createReheatTempOffsetTuner(hayStack : CCUHsApi, siteRef : String, equipRef : String, equipDis : String,
                                tz : String, defaultTag : String, roomRef : String?) {

    val reheatTempOffset = Point.Builder()
        .setDisplayName("$equipDis-DAB-reheatTempOffset")
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("temp").addMarker("offset").addMarker("sp")
        .setMinVal("0").setMaxVal("10").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.FAHRENHEIT)
    roomRef?.let { reheatTempOffset.setRoomRef(it) }
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
                                tz : String, defaultTag : String, roomRef : String?) {

    val relayActivationHysteresis = Point.Builder()
        .setDisplayName("$equipDis-DAB-reheatDelayActivationHysteresis")
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("relay").addMarker("activation").addMarker("hysteresis")
        .addMarker("sp")
        .setMinVal("0").setMaxVal("100").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.PERCENTAGE)
    roomRef?.let { relayActivationHysteresis.setRoomRef(it) }
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
                                         tz : String, defaultTag : String, roomRef : String?) {

    val temperatureProportionalRange = Point.Builder()
        .setDisplayName("$equipDis-DAB-reheatTemperatureProportionalRange")
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("temp").addMarker("proportional").addMarker("spread")
        .addMarker("sp")
        .setMinVal("0").setMaxVal("100").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.FAHRENHEIT)
    roomRef?.let { temperatureProportionalRange.setRoomRef(it) }
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
                                            tz : String, defaultTag : String, roomRef : String?) {

    val temperatureIntegralTime = Point.Builder()
        .setDisplayName("$equipDis-DAB-reheatTemperatureIntegralTime")
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("temp").addMarker("integral").addMarker("time")
        .addMarker("sp")
        .setMinVal("0").setMaxVal("100").setIncrementVal("1")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.MINUTES)
    roomRef?.let { temperatureIntegralTime.setRoomRef(it) }
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
                                       tz : String, defaultTag : String, type :String, roomRef : String?) {

    val temperatureIntegralTime = Point.Builder()
        .setDisplayName("$equipDis-DAB-reheat${type.capitalize()}KFactor")
        .setSiteRef(siteRef)
        .setEquipRef(equipRef).setHisInterpolate("cov")
        .addMarker("tuner").addMarker("writable").addMarker("his").addMarker(defaultTag)
        .addMarker("reheat").addMarker("kFactor").addMarker(type)
        .addMarker("sp")
        .setMinVal("0").setMaxVal("10").setIncrementVal(".5")
        .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
        .setTz(tz).setUnit(Units.MINUTES)
    roomRef?.let { temperatureIntegralTime.setRoomRef(it) }
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
    if (hayStack.readEntity("default and reheat and temp and offset").isNotEmpty()) {
        return
    }
    createReheatTempOffsetTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
            buildingTunerEquip.tz, Tags.DEFAULT, null)
    createRelayActivationHysteresisTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, null)
    createTemperatureProportionalRangeTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, null)
    createTemperatureIntegralTimeTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, null)
    createKFactorTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, "integral", null)
    createKFactorTuner(hayStack, buildingTunerEquip.siteRef, buildingTunerEquip.id, buildingTunerEquip.displayName,
        buildingTunerEquip.tz, Tags.DEFAULT, "proportional" ,null)
}

fun createEquipReheatTuners(hayStack: CCUHsApi, equip : Equip) {
    createReheatTempOffsetTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef)
    createRelayActivationHysteresisTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef)
    createTemperatureProportionalRangeTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef)
    createTemperatureIntegralTimeTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, equip.roomRef)
    createKFactorTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, "integral", equip.roomRef)
    createKFactorTuner(hayStack, equip.siteRef, equip.id, equip.displayName,
        equip.tz, Tags.DAB, "proportional" ,equip.roomRef)
}
