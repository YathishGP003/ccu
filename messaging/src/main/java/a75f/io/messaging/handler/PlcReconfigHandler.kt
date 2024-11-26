package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.plc.PlcProfile
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint

fun updateConfigPoint(msgObject: JsonObject, configPoint: Point) {
    CcuLog.i(
        L.TAG_CCU_PUBNUB, "updatePIConfigPoint " + configPoint + " " + msgObject.toString()
                + " Markers =" + configPoint.markers
    )

    val hayStack = CCUHsApi.getInstance()
    val address = configPoint.group.toShort()
    val profile = L.getProfile(address) as PlcProfile

    val equip = profile.equip
    val config = profile.domainProfileConfiguration as PlcProfileConfig
    val model = getModelForDomainName(equip.domainName) as SeventyFiveFProfileDirective
    when (configPoint.domainName) {
        DomainName.analog1InputType -> {
            val analog1InputPoint = model.points.find { it.domainName == DomainName.analog1InputType }
            return (analog1InputPoint?.valueConstraint as MultiStateConstraint).allowedValues[viewState.analog1InputType.toInt()].value
            config.baseConfigs.add(EntityConfig(processVariablePoint))
        }
    }

    val equipBuilder = ProfileEquipBuilder(hayStack)
    equipBuilder.updateEquipAndPoints(
        config,
        model,
        equip.siteRef,
        equip.displayName, true
    )


    val deviceModel =
        getModelForDomainName(Domain.systemEquip.equipRef) as SeventyFiveFDeviceDirective
    val entityMapper = EntityMapper(model)
    val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
    val device = hayStack.readEntity("device and addr == \"$address\"")
    deviceBuilder.updateDeviceAndPoints(
        config, deviceModel, equip.id, equip.siteRef,
        device[Tags.DIS].toString(), model
    )


    /*if(configPoint.getMarkers().contains(INPUT_TAG)){
            PlcRelayConfigHandler.updateInputSensor(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(TARGET_TAG)){
            PlcRelayConfigHandler.updateTargetValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(RANGE_TAG)){
            PlcRelayConfigHandler.updateRangeValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(MIDPOINT_TAG)){
            PlcRelayConfigHandler.updateMidpointValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(INVERSION_TAG)){
            PlcRelayConfigHandler.updateInversionValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(ANALOG2_TAG) &&
                configPoint.getMarkers().contains(ENABLED_TAG)) {
            PlcRelayConfigHandler.updateEnableAn2Value(msgObject, configPoint);
        }else if(configPoint.getMarkers().contains(SETPOINT_TAG) &&
                configPoint.getMarkers().contains(SENSOR_TAG)) {
            PlcRelayConfigHandler.updateAn2SetpointValue(msgObject, configPoint);
        }else if(configPoint.getMarkers().contains(ANALOG1_TAG) &&
                configPoint.getMarkers().contains(OUTPUT_TAG)) {
            PlcRelayConfigHandler.updateAn1OutputValue(msgObject, configPoint);
        }else if(configPoint.getMarkers().contains(RELAY1_TAG)
        ||configPoint.getMarkers().contains(RELAY2_TAG)) {
            PlcRelayConfigHandler.updateRelayValue(msgObject, configPoint);
        }*/
    writePointFromJson(configPoint, msgObject, CCUHsApi.getInstance())
}

private fun writePointFromJson(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
    try {
        val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString
        val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
        val id = configPoint.id
        if (value.isEmpty()) {
            //When a level is deleted, it currently generates a pubnub with empty value.
            //Handle it here.
            hayStack.clearPointArrayLevel(id, level, false)
            hayStack.writeHisValById(id, HSUtil.getPriorityVal(id))
            return
        }
        val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString

        val `val` = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asDouble
        val duration =
            if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
        hayStack.writePointLocal(configPoint.id, level, who, `val`, duration)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}