package a75f.io.logic.bo.building.plc

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.EntityConfig
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint

fun addBaseProfileConfig(domainName: String, config: PlcProfileConfig, model: SeventyFiveFProfileDirective) {
    when (domainName) {
        DomainName.analog1InputType -> {
            if (config.analog1InputType.currentVal.toInt() > 0) {
                val analog1Input = getInputSensorPoint(
                    domainName,
                    config.analog1InputType.currentVal.toInt(),
                    model
                )
                if (analog1Input.isNotEmpty()) {
                    config.baseConfigs.add(EntityConfig(analog1Input))
                }
            }
        }
        DomainName.thermistor1InputType -> {
            if (config.thermistor1InputType.currentVal.toInt() > 0) {
                val thermistor1Input = getInputSensorPoint(
                    domainName,
                    config.thermistor1InputType.currentVal.toInt(),
                    model
                )
                if (thermistor1Input.isNotEmpty()) {
                    config.baseConfigs.add(EntityConfig(thermistor1Input))
                }
            }
        }
        DomainName.nativeSensorType -> {
            if (config.nativeSensorType.currentVal.toInt() > 0) {
                val nativeSensorType = getInputSensorPoint(
                    domainName,
                    config.nativeSensorType.currentVal.toInt(),
                    model
                )
                if (nativeSensorType.isNotEmpty()) {
                    config.baseConfigs.add(EntityConfig(nativeSensorType))
                }
            }
        }
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

fun getInputSensorPoint(domainName : String, index : Int, model: SeventyFiveFProfileDirective): String {
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