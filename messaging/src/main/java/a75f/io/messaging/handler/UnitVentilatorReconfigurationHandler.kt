package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.Pipe2UVEquip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.domain.equips.unitVentilator.UnitVentilatorEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorConfiguration
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.PossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.UvFanStages
import a75f.io.logic.bo.building.statprofiles.util.getPossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.messaging.handler.MessageUtil.Companion.returnDurationDiff
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created on 23/07/25
 * Mukesh Kumar K
 */

fun reconfigureUnitVentilator(msgObject: JsonObject, configPoint: Point) {

    val hayStack = CCUHsApi.getInstance()
    val unitVentilatorEquip = hayStack.readEntity("equip and id == " + configPoint.equipRef)

    val model = getUnitVentilatorModelByEquipRef(configPoint.equipRef)
    if (model == null) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "model is null for $configPoint")
        return
    }

    val config = getSplitConfiguration(configPoint.equipRef) as UnitVentilatorConfiguration
    val equipBuilder = ProfileEquipBuilder(hayStack)
    val deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective
    val entityMapper = EntityMapper(model as SeventyFiveFProfileDirective)
    val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
    val deviceDis = "${hayStack.siteName}-HSS-${config.nodeAddress}"


    val pointNewValue = msgObject["val"]
    if (pointNewValue == null || pointNewValue.asString.isEmpty()) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Point value is null or empty for $configPoint")

    } else {
        updateConfiguration(configPoint.domainName, pointNewValue.asDouble, config)
        equipBuilder.updateEquipAndPoints(
            config,
            model,
            hayStack.site!!.id,
            unitVentilatorEquip["dis"].toString(),
            true
        )

        if (configPoint.domainName == DomainName.fanOpMode) {
            uvUpdateFanMode(configPoint.equipRef, pointNewValue.asInt)
        }
        deviceBuilder.updateDeviceAndPoints(
            config,
            deviceModel,
            unitVentilatorEquip["id"].toString(),
            hayStack.site!!.id,
            deviceDis
        )

        if (configPoint.domainName != DomainName.fanOpMode) {
            updateFanMode(configPoint.equipRef)
        }
    }
    writePointFromJson(configPoint, msgObject, hayStack)
    config.apply { setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap()) }
    DesiredTempDisplayMode.setModeType(configPoint.roomRef, CCUHsApi.getInstance())

    if ((pointNewValue == null || pointNewValue.asString.isEmpty()) && configPoint.domainName == DomainName.fanOpMode) {
        uvUpdateFanMode(configPoint.equipRef, HSUtil.getPriorityVal(configPoint.id).toInt())
    }

    CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for Unit ventilator Reconfiguration $config")
}


fun getUnitVentilatorModelByEquipRef(equipRef: String): ModelDirective? {
    return when (Domain.getDomainEquip(equipRef) as UnitVentilatorEquip) {
        is Pipe4UVEquip -> ModelLoader.getSplitPipe4Model()
        is Pipe2UVEquip -> ModelLoader.getSplitPipe2Model()
        else -> null
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
        val durationDiff = returnDurationDiff(msgObject)
        hayStack.writePointLocal(configPoint.id, level, who, value.toDouble(), durationDiff)
        hayStack.writeHisValById(configPoint.id, value.toDouble())
        CcuLog.d(
            L.TAG_CCU_PUBNUB,
            "Unit Ventilator: writePointFromJson - level: $level who: $who val: $value  durationDiff: $durationDiff"
        )
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}

fun uvUpdateFanMode(equipRef: String, fanMode: Int) {
    CcuLog.i(L.TAG_CCU_PUBNUB, "updateFanMode $fanMode")
    val cache = FanModeCacheStorage.getHyperStatSplitFanModeCache()
    if (fanMode != 0 && (fanMode % 3 == 0 || isFanModeCurrentOccupied(fanMode))) {
        cache.saveFanModeInCache(equipRef, fanMode)
    } else {
        cache.removeFanModeFromCache(equipRef)
    }
}

private fun isFanModeCurrentOccupied(value: Int): Boolean {
    val basicSettings = UvFanStages.values()[value]
    return (basicSettings == UvFanStages.LOW_CUR_OCC || basicSettings == UvFanStages.HIGH_CUR_OCC || basicSettings == UvFanStages.MEDIUM_CUR_OCC)
}

fun updateFanMode(equipId: String) {
    val equip = HyperStatSplitEquip(equipId)
    fun resetFanToOff() = equip.fanOpMode.writePointValue(StandaloneFanStage.OFF.ordinal.toDouble())
    val possibleFanMode = getPossibleFanMode(equip)
    val currentFanMode = StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()]
    fun isWithinLow(): Boolean {
        return (currentFanMode.ordinal in listOf(
            StandaloneFanStage.LOW_ALL_TIME.ordinal,
            StandaloneFanStage.LOW_OCC.ordinal,
            StandaloneFanStage.LOW_CUR_OCC.ordinal
        ))
    }

    fun isWithinMedium(): Boolean {
        return (currentFanMode.ordinal in listOf(
            StandaloneFanStage.MEDIUM_ALL_TIME.ordinal,
            StandaloneFanStage.MEDIUM_OCC.ordinal,
            StandaloneFanStage.MEDIUM_CUR_OCC.ordinal
        ))
    }

    fun isWithinHigh(): Boolean {
        return (currentFanMode.ordinal in listOf(
            StandaloneFanStage.HIGH_ALL_TIME.ordinal,
            StandaloneFanStage.HIGH_OCC.ordinal,
            StandaloneFanStage.HIGH_CUR_OCC.ordinal
        ))
    }
    if (currentFanMode != StandaloneFanStage.AUTO) {
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
                if (isWithinLow()) {
                    resetFanToOff()
                }
            }

            else -> {}
        }
    }
}


