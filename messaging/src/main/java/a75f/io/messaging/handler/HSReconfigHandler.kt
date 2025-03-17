package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstat.common.PossibleFanMode
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getHsFanLevel
import a75f.io.logic.bo.building.hyperstat.profiles.util.getModelByEquipRef
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPossibleFanModeSettings
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective


fun reconfigureHSV2(msgObject: JsonObject, configPoint: Point) {

    val hayStack = CCUHsApi.getInstance()
    
    val hyperStatEquip = hayStack.readEntity("equip and id == "+configPoint.equipRef)
    val model = getModelByEquipRef(configPoint.equipRef)

    if (model == null) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "model is null for $configPoint")
        return
    }

    val config = getConfiguration(configPoint.equipRef)?.getActiveConfiguration()
    val equipBuilder = ProfileEquipBuilder(hayStack)
    val deviceModel = ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
    val entityMapper = EntityMapper(model as SeventyFiveFProfileDirective)
    val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
    val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${config!!.nodeAddress}"


    val pointNewValue = msgObject["val"]
    if(pointNewValue == null || pointNewValue.asString.isEmpty()){
        CcuLog.e(L.TAG_CCU_PUBNUB, "point is null $config")
    } else {
        updateConfiguration(configPoint.domainName, pointNewValue.asDouble, config!!)
        equipBuilder.updateEquipAndPoints(config, model , hayStack.getSite()!!.id, hyperStatEquip["dis"].toString(), true)
        if (configPoint.domainName == DomainName.fanOpMode) {
            updateFanModeCache(configPoint.equipRef, pointNewValue.asInt)
        }
        deviceBuilder.updateDeviceAndPoints(config, deviceModel, hyperStatEquip["id"].toString(), hayStack.site!!.id, deviceDis)
        updateFanMode(configPoint.equipRef, config)
    }
    writePointFromJson(configPoint, msgObject, hayStack)
    config.apply { setPortConfiguration( nodeAddress, getRelayMap(), getAnalogMap()) }
    DesiredTempDisplayMode.setModeType(configPoint.roomRef, CCUHsApi.getInstance())

    /*
    - If we do reconfiguration from portal for fanMode, level 10 updated as ( val = 9)
    - Now if user change the fanMode from CCU, it will update the level 8 as 1
    - Now if we do reconfiguration from portal for fanMode, message will receive for only removing level 8 not for level 10
    - Because level 10 does not have change of value, so silo will never send update entity for level 10
    - Due to this fanMode will not update in CCU's shared preference
     */
    if ((pointNewValue == null || pointNewValue.asString.isEmpty()) && configPoint.domainName == DomainName.fanOpMode) {
        updateFanModeCache(configPoint.equipRef, HSUtil.getPriorityVal(configPoint.id).toInt())
    }

    CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for CPU Reconfiguration $config")

}

fun updateFanModeCache(equipRef: String, fanMode: Int) {
    CcuLog.i(L.TAG_CCU_PUBNUB, "updateFanMode $fanMode")
    val cache = FanModeCacheStorage()
    if (fanMode != 0  && (fanMode % 3 == 0 || isFanModeCurrentOccupied(fanMode)) ) {
        cache.saveFanModeInCache(equipRef, fanMode)
    }
    else {
        cache.removeFanModeFromCache(equipRef)
    }
}

private fun isFanModeCurrentOccupied(value : Int): Boolean {
    val basicSettings = StandaloneFanStage.values()[value]
    return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
}



fun updateFanMode(equipId: String, config: HyperStatConfiguration) {
    val fanLevel = getHsFanLevel(config)
    val equip = HyperStatEquip(equipId)
    fun resetFanToOff() = equip.fanOpMode.writePointValue(StandaloneFanStage.OFF.ordinal.toDouble())
    val possibleFanMode = getPossibleFanModeSettings(fanLevel)
    val currentFanMode = StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()]
    fun isWithinLow(): Boolean {
        return (currentFanMode.ordinal in listOf(
            StandaloneFanStage.LOW_ALL_TIME.ordinal,
            StandaloneFanStage.LOW_OCC.ordinal,
            StandaloneFanStage.LOW_CUR_OCC.ordinal))
    }
    fun isWithinMedium(): Boolean {
        return (currentFanMode.ordinal in listOf(
            StandaloneFanStage.MEDIUM_ALL_TIME.ordinal,
            StandaloneFanStage.MEDIUM_OCC.ordinal,
            StandaloneFanStage.MEDIUM_CUR_OCC.ordinal))
    }
    fun isWithinHigh(): Boolean {
        return (currentFanMode.ordinal in listOf(
            StandaloneFanStage.HIGH_ALL_TIME.ordinal,
            StandaloneFanStage.HIGH_OCC.ordinal,
            StandaloneFanStage.HIGH_CUR_OCC.ordinal))
    }

    when (possibleFanMode) {
        PossibleFanMode.LOW -> {
            if (!isWithinLow()) {
                resetFanToOff()
            }
        }

        PossibleFanMode.MED -> {
            if (!isWithinMedium()) {
                resetFanToOff()
            }
        }

        PossibleFanMode.HIGH -> {
            if (!isWithinHigh()) {
                resetFanToOff()
            }
        }

        PossibleFanMode.LOW_MED -> {
            if (isWithinHigh()) {
                resetFanToOff()
            }
        }

        PossibleFanMode.LOW_HIGH -> {
            if (isWithinMedium()) {
                resetFanToOff()
            }
        }

        PossibleFanMode.MED_HIGH -> {
            if (!isWithinLow()) {
                resetFanToOff()
            }
        }

        else -> {}
    }
}


private fun writePointFromJson(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
    try {
        val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString
        val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
        val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString
        if (value.isEmpty()) {
            hayStack.clearPointArrayLevel(configPoint.id, level, false)
            hayStack.writeHisValById(configPoint.id, HSUtil.getPriorityVal(configPoint.id))
            return
        }
        val duration =
                if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
        hayStack.writePointLocal(configPoint.id, level, who, value.toDouble(), duration)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}