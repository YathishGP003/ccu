package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.api.Domain
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.messaging.handler.MessageUtil.Companion.returnDurationDiff
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective


/*
* created by spoorthidev on 20-July-2021
*/
internal object HyperStatMonitoringConfigHandler {
    @JvmStatic
    fun reconfigureMonitoring(msgObject: JsonObject, configPoint: Point) {
        val hayStack = CCUHsApi.getInstance()
        val hyperStatMonitoringEquip =
            hayStack.readEntity("equip and id == " + configPoint.equipRef)

        val profileModel = ModelLoader.getHyperStatMonitoringModel() as SeventyFiveFProfileDirective
        val deviceModel = ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(profileModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val config = getHsConfiguration(configPoint.equipRef)?.getActiveConfiguration()//make it common for all

        val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${config!!.nodeAddress}"

        val pointNewValue = msgObject["val"].asDouble
        updateConfiguration(configPoint.domainName, pointNewValue, config)

        equipBuilder.updateEquipAndPoints(
            config,
            profileModel,
            hayStack.getSite()!!.id,
            hyperStatMonitoringEquip["dis"].toString(),
            true
        )
        CcuLog.i(Domain.LOG_TAG, " updated Monitoring Equip and Points")
        writePointFromJson(configPoint, msgObject, hayStack)
        deviceBuilder.updateDeviceAndPoints(
            config,
            deviceModel,
            hyperStatMonitoringEquip["id"].toString(),
            hayStack.site!!.id,
            deviceDis
        )
        CcuLog.i(L.TAG_CCU_PUBNUB, "updated Monitoring Device and Points $config")
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
            CcuLog.d(
                L.TAG_CCU_PUBNUB,
                "HS Monitoring : writePointFromJson - level: $level who: $who val: $value  durationDiff: $durationDiff"
            )
        } catch (e: Exception) {
            CcuLog.e(
                L.TAG_CCU_PUBNUB,
                "Failed to parse tuner value : " + msgObject + " ; " + e.message
            )
        }
    }
}
