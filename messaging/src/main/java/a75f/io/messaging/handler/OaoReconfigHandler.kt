package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.oao.OAOProfile
import a75f.io.logic.bo.building.oao.OAOProfileConfiguration
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

fun updateOaoDevicePoints(msgObject: JsonObject, configPoint: Point) {
    CcuLog.i(
        L.TAG_CCU_PUBNUB, "update OAO point: " + configPoint + " " + msgObject.toString()
                + " Markers =" + configPoint.markers
    )
    val hayStack = CCUHsApi.getInstance()
    val address = configPoint.group.toShort()
    val oaoProfile = L.ccu().oaoProfile as OAOProfile
    val oaoModel = ModelLoader.getSmartNodeOAOModelDef() as SeventyFiveFProfileDirective
    val deviceModel = ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective
    val profileConfiguration = OAOProfileConfiguration(
        address.toInt(), NodeType.SMART_NODE.name, 0,
        Tags.SYSTEM, Tags.SYSTEM, oaoProfile.profileType, oaoModel
    ).getActiveConfiguration()

    profileConfiguration.updateDevicePoints(
        hayStack,
        profileConfiguration,
        DeviceBuilder(hayStack, EntityMapper(oaoModel)),
        deviceModel,
        true
    )
}