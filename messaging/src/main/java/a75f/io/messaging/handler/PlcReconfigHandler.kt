package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.plc.PlcProfile
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import a75f.io.logic.bo.building.plc.addBaseProfileConfig
import a75f.io.messaging.handler.MessageUtil.Companion.returnDurationDiff
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

        DomainName.relay1OutputEnable -> {
            val relay1ConfigEnabled = sensorTypeValue > 0
            config.relay1OutputEnable.enabled = relay1ConfigEnabled
            if(!relay1ConfigEnabled) {
                val deviceId = hayStack.readId("device and addr == \"$address\"")
                (PhysicalPoint(DomainName.relay1, deviceId)).writePointValue(0.0)
            }
        }
        DomainName.relay2OutputEnable -> {
            val relay2ConfigEnabled = sensorTypeValue > 0
            config.relay2OutputEnable.enabled = relay2ConfigEnabled
            if(!relay2ConfigEnabled) {
                val deviceId = hayStack.readId("device and addr == \"$address\"")
                (PhysicalPoint(DomainName.relay2, deviceId)).writePointValue(0.0)
            }
        }
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
    } else if (configPoint.domainName == DomainName.analog1MinOutput || configPoint.domainName == DomainName.analog1MaxOutput) {
        CcuLog.d(L.TAG_CCU_PUBNUB, "Update analog Out val")
        when (configPoint.domainName) {
            DomainName.analog1MinOutput -> {
                config.analog1MinOutput.currentVal = sensorTypeValue.toDouble()
            }
            DomainName.analog1MaxOutput -> {
                config.analog1MaxOutput.currentVal = sensorTypeValue.toDouble()
            }
        }
        config.updateTypeForAnalog1Out(config)
    }

    writePointFromJson(configPoint, msgObject, CCUHsApi.getInstance())
    profile.init()
    val updatedConf = profile.domainProfileConfiguration as PlcProfileConfig
    when (configPoint.domainName) {
        DomainName.analog1InputType -> {
            val index = config.analog1InputType.currentVal.toInt()
            updatedConf.updateMinMax(DomainName.analog1InputType, model, index, equip.id)
        }

        DomainName.thermistor1InputType -> {
            val index = config.thermistor1InputType.currentVal.toInt()
            updatedConf.updateMinMax(DomainName.thermistor1InputType, model, index, equip.id)
        }

        DomainName.nativeSensorType -> {
            val index = config.nativeSensorType.currentVal.toInt()
            updatedConf.updateMinMax(DomainName.nativeSensorType, model, index, equip.id)
        }
    }

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
        val durationDiff = returnDurationDiff(msgObject)
        hayStack.writePointLocal(id, level, who, `val`, durationDiff)
        CcuLog.d(
            L.TAG_CCU_PUBNUB,
            "plc: writePointFromJson - level: $level who: $who val: $`val`  durationDiff: $durationDiff"
        )
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}