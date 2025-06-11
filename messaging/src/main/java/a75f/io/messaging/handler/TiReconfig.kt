package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.caz.configs.TIConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.messaging.handler.MessageUtil.Companion.returnDurationDiff
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 26-12-2024.
 */


fun tiReconfiguration(msgObject: JsonObject, configPoint: Point) {

    val hayStack = CCUHsApi.getInstance()
    val pointNewValue = msgObject["val"]

    val model = ModelLoader.getTIModel() as SeventyFiveFProfileDirective
    val deviceModel = ModelLoader.getTIDeviceModel() as SeventyFiveFDeviceDirective
    val equipBuilder = ProfileEquipBuilder(hayStack)

    val equipDetails = CCUHsApi.getInstance().readMapById(configPoint.equipRef)
    val nodeAddress = equipDetails[Tags.GROUP].toString().toInt()
    val floorRef = equipDetails[Tags.FLOORREF] as String
    val roomRef = equipDetails[Tags.ROOMREF] as String

    val config = TIConfiguration(
        nodeAddress,
        NodeType.CONTROL_MOTE.name,
        0,
        roomRef,
        floorRef,
        ProfileType.TEMP_INFLUENCE,
        model
    ).getActiveConfiguration()

    val deviceDis = hayStack.siteName + "-TI-" + config.nodeAddress

    if (pointNewValue == null || pointNewValue.asString.isEmpty()) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "point is null $config")
    } else {
        updateConfiguration(configPoint.domainName, pointNewValue.asDouble, config)
        val equipId = equipBuilder.updateEquipAndPoints(
            config,
            model,
            hayStack.getSite()!!.id,
            equipDetails["dis"].toString(),
            true
        )

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceId = deviceBuilder.updateDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
        config.updatePhysicalPointRef(equipId, deviceId)
    }
    writePointFromJson(configPoint, msgObject, hayStack)
    CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for CPU Reconfiguration $config")

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
        val durationDiff = returnDurationDiff(msgObject);
        hayStack.writePointLocal(configPoint.id, level, who, value.toDouble(), durationDiff)
        CcuLog.d(
            L.TAG_CCU_PUBNUB,
            "TI: writePointFromJson - level: $level who: $who val: $value  durationDiff: $durationDiff"
        )
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}