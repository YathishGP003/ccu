package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.plc.PLCConstants.AIR_TEMP_SENSOR_100K_OHMS
import a75f.io.logic.bo.building.plc.PLCConstants.CURRENT_TX10_AI1
import a75f.io.logic.bo.building.plc.PLCConstants.CURRENT_TX20_AI1
import a75f.io.logic.bo.building.plc.PLCConstants.CURRENT_TX50_AI1
import a75f.io.logic.bo.building.plc.PLCConstants.EXTERNAL_AIR_TEMP_SENSOR
import a75f.io.logic.bo.building.plc.PLCConstants.VOLTAGE_INPUT_AI1
import a75f.io.logic.bo.building.plc.PLCConstants.ZONE_CO_AI1
import a75f.io.logic.bo.building.plc.PLCConstants.ZONE_HUMIDITY_AI1
import a75f.io.logic.bo.building.plc.PLCConstants.ZONE_NO2_AI1
import a75f.io.logic.bo.building.plc.PlcProfile
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import a75f.io.logic.bo.building.plc.addBaseProfileConfig
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

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
    val sensorTypeValue =
        msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString.toDouble().toInt()
    when (configPoint.domainName) {
        DomainName.analog1InputType -> {
            if (sensorTypeValue.toDouble() == 0.0)
                return  // ignoring this message
            config.analog1InputType.currentVal = sensorTypeValue.toDouble()
            config.thermistor1InputType.currentVal = 0.0
            config.nativeSensorType.currentVal = 0.0

            val sensorMinVal = config.getMinVal(sensorTypeValue, model, "analog1InputType")
            config.pidTargetValue.currentVal = sensorMinVal[0].toDouble()
            config.pidProportionalRange.currentVal = sensorMinVal[0].toDouble()
        }

        DomainName.thermistor1InputType -> {
            if (sensorTypeValue.toDouble() == 0.0)
                return  // ignoring this message
            config.analog1InputType.currentVal = 0.0
            config.thermistor1InputType.currentVal = sensorTypeValue.toDouble()
            config.nativeSensorType.currentVal = 0.0

            val sensorMinVal = config.getMinVal(sensorTypeValue, model, "thermistor1InputType")
            config.pidTargetValue.currentVal = sensorMinVal[0].toDouble()
            config.pidProportionalRange.currentVal = sensorMinVal[0].toDouble()
        }

        DomainName.nativeSensorType -> {
            if (sensorTypeValue.toDouble() == 0.0)
                return  // ignoring this message
            config.analog1InputType.currentVal = 0.0
            config.thermistor1InputType.currentVal = 0.0
            config.nativeSensorType.currentVal = sensorTypeValue.toDouble()

            val sensorMinVal = config.getMinVal(sensorTypeValue, model, "nativeSensorType")
            config.pidTargetValue.currentVal = sensorMinVal[0].toDouble()
            config.pidProportionalRange.currentVal = sensorMinVal[0].toDouble()
        }

        DomainName.analog2InputType -> config.analog2InputType.currentVal =
            sensorTypeValue.toDouble()

        DomainName.useAnalogIn2ForSetpoint -> config.useAnalogIn2ForSetpoint.enabled =
            sensorTypeValue > 0

        DomainName.relay1OutputEnable -> config.relay1OutputEnable.enabled = sensorTypeValue > 0
        DomainName.relay2OutputEnable -> config.relay2OutputEnable.enabled = sensorTypeValue > 0
        else -> {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Not a dependent/associate point : $configPoint")
        }
    }
    CcuLog.i(
        L.TAG_CCU_PUBNUB,
        "updatePIConfigPoint value : $sensorTypeValue \n CurrentConfig : $config"
    )
    addBaseProfileConfig(DomainName.analog1InputType, config, model)
    addBaseProfileConfig(DomainName.thermistor1InputType, config, model)
    addBaseProfileConfig(DomainName.nativeSensorType, config, model)
    addBaseProfileConfig(DomainName.useAnalogIn2ForSetpoint, config, model)

    config.baseConfigs.forEach {
        CcuLog.i(L.TAG_CCU_PUBNUB, "BaseConfig added : ${it.domainName}")
    }


    if (profileReconfigurationRequired(configPoint)) {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        equipBuilder.updateEquipAndPoints(
            config,
            model,
            equip.siteRef,
            equip.displayName, true
        )


        val device = hayStack.readEntity("device and addr == \"$address\"")
        val deviceModel =
            getModelForDomainName(device["domainName"].toString()) as SeventyFiveFDeviceDirective
        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        deviceBuilder.updateDeviceAndPoints(
            config, deviceModel, equip.id, equip.siteRef,
            device[Tags.DIS].toString(), model
        )
        config.updateTypeForAnalog1Out(config)
        config.updatePortConfiguration(hayStack, config, deviceBuilder, deviceModel)
    }

    writePointFromJson(configPoint, msgObject, CCUHsApi.getInstance())
    profile.init()
}

private fun profileReconfigurationRequired(configPoint: Point): Boolean {
    return configPoint.domainName == DomainName.analog1InputType ||
            configPoint.domainName == DomainName.thermistor1InputType ||
            configPoint.domainName == DomainName.nativeSensorType ||
            configPoint.domainName == DomainName.analog2InputType ||
            configPoint.domainName == DomainName.useAnalogIn2ForSetpoint ||
            configPoint.domainName == DomainName.relay1OutputEnable ||
            configPoint.domainName == DomainName.relay2OutputEnable
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