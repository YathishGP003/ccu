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
    val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString.toInt()

    when(configPoint.domainName) {
        DomainName.analog1InputType -> config.analog1InputType.currentVal = value.toDouble()
        DomainName.thermistor1InputType -> config.thermistor1InputType.currentVal = value.toDouble()
        DomainName.nativeSensorType -> config.nativeSensorType.currentVal = value.toDouble()
        DomainName.analog2InputType -> config.analog2InputType.currentVal = value.toDouble()
        DomainName.useAnalogIn2ForSetpoint -> config.useAnalogIn2ForSetpoint.enabled = value > 0
        DomainName.relay1OutputEnable -> config.relay1OutputEnable.enabled = value > 0
        DomainName.relay2OutputEnable -> config.relay2OutputEnable.enabled = value > 0
        else -> {CcuLog.e(L.TAG_CCU_PUBNUB, "Not a dependent/associate point : $configPoint")}
    }
    CcuLog.i(L.TAG_CCU_PUBNUB, "updatePIConfigPoint value : $value \n CurrentConfig : $config")
    addBaseProfileConfig(DomainName.analog1InputType, config, model)
    addBaseProfileConfig(DomainName.thermistor1InputType, config, model)
    addBaseProfileConfig(DomainName.nativeSensorType, config, model)
    //addBaseProfileConfig(DomainName.analog2InputType, config, model)
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
    }

    writePointFromJson(configPoint, msgObject, CCUHsApi.getInstance())
}

private fun addBaseProfileConfig(domainName: String, config: PlcProfileConfig, model: SeventyFiveFProfileDirective) {
    when (domainName) {
        DomainName.analog1InputType -> {
            val analog1Input = getInputSensorPoint(domainName, config.analog1InputType.currentVal.toInt(), model)
            if (analog1Input.isNotEmpty()) {
                config.baseConfigs.add(EntityConfig(analog1Input))
            }
        }
        DomainName.thermistor1InputType -> {
            val thermistor1Input = getInputSensorPoint(domainName, config.thermistor1InputType.currentVal.toInt(), model)
            if (thermistor1Input.isNotEmpty()) {
                config.baseConfigs.add(EntityConfig(thermistor1Input))
            }
        }
        DomainName.nativeSensorType -> {
            val nativeSensorType = getInputSensorPoint(domainName, config.nativeSensorType.currentVal.toInt(), model)
            if (nativeSensorType.isNotEmpty()) {
                config.baseConfigs.add(EntityConfig(nativeSensorType))
            }
        }
        /*DomainName.analog2InputType -> {
            val analog2Input = getInputSensorPoint(domainName, config.analog2InputType.currentVal.toInt(), model)
            if (analog2Input.isNotEmpty()) {
                config.baseConfigs.add(EntityConfig(analog2Input))
            }
        }*/
        DomainName.useAnalogIn2ForSetpoint -> {
            if (config.useAnalogIn2ForSetpoint.enabled) {
                val analog2Input = getInputSensorPoint(DomainName.analog2InputType, config.analog2InputType.currentVal.toInt(), model)
                if (analog2Input.isNotEmpty()) {
                    config.baseConfigs.add(EntityConfig(analog2Input))
                } else {
                    CcuLog.e(L.TAG_CCU_PUBNUB, "Analog2Input not found for ${config.analog2InputType.currentVal}")
                }
            }
        }
    }

}

private fun getInputSensorPoint(domainName : String, index : Int, model: SeventyFiveFProfileDirective): String {
    //Models have a dummy entry "not used" at index 0 which are not associated with any point.
    if (index == 0) {
        return ""
    }
    return when (domainName) {
        DomainName.analog1InputType -> {
            val analog1InputPoint = model.points.find { it.domainName == DomainName.analog1InputType }
            (analog1InputPoint?.valueConstraint as MultiStateConstraint).allowedValues[index].value
        }
        DomainName.thermistor1InputType -> {
            val thermistor1InputPoint = model.points.find { it.domainName == DomainName.thermistor1InputType }
            (thermistor1InputPoint?.valueConstraint as MultiStateConstraint).allowedValues[index].value
        }
        DomainName.nativeSensorType -> {
            val nativeSensorTypePoint = model.points.find { it.domainName == DomainName.nativeSensorType }
            (nativeSensorTypePoint?.valueConstraint as MultiStateConstraint).allowedValues[index].value
        }
        DomainName.analog2InputType -> {
            val analog2InputPoint = model.points.find { it.domainName == DomainName.analog2InputType }
            (analog2InputPoint?.valueConstraint as MultiStateConstraint).allowedValues[index].value
        }
        else -> ""
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
        val duration =
            if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
        hayStack.writePointLocal(configPoint.id, level, who, `val`, duration)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}