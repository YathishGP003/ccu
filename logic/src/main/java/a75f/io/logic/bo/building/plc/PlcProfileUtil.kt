package a75f.io.logic.bo.building.plc

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.PlcEquip
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint


fun getProcessVariableMappedPoint(plcEquip: PlcEquip, model : ModelDirective) : String {
    if (plcEquip.analog1InputType.readDefaultVal() > 0) {
        val analog1InputPoint = model.points.find { it.domainName == DomainName.analog1InputType }
        return (analog1InputPoint?.valueConstraint as MultiStateConstraint)
                        .allowedValues[plcEquip.analog1InputType.readDefaultVal().toInt()].value
    } else if (plcEquip.thermistor1InputType.readDefaultVal() > 0) {
        val thermistor1InputPoint = model.points.find { it.domainName == DomainName.thermistor1InputType }
        return (thermistor1InputPoint?.valueConstraint as MultiStateConstraint)
                        .allowedValues[plcEquip.thermistor1InputType.readDefaultVal().toInt()].value
    } else if (plcEquip.nativeSensorType.readDefaultVal() > 0) {
        val nativeSensorTypePoint = model.points.find { it.domainName == DomainName.nativeSensorType }
        return (nativeSensorTypePoint?.valueConstraint as MultiStateConstraint)
                        .allowedValues[plcEquip.nativeSensorType.readDefaultVal().toInt()].value
    } else {
        CcuLog.i(Domain.LOG_TAG, "No process variable selected")
        return ""
    }
}

fun getDynamicTargetPoint(plcEquip: PlcEquip, model : ModelDirective) : String{
    if (plcEquip.useAnalogIn2ForSetpoint.readDefaultVal() > 0) {
        val analog2InputPoint = model.points.find { it.domainName == DomainName.analog2InputType }
        return (analog2InputPoint?.valueConstraint as MultiStateConstraint)
            .allowedValues[plcEquip.analog2InputType.readDefaultVal().toInt()].value

    } else {
        CcuLog.i(Domain.LOG_TAG, "Analog2 Sensor point not found")
        return ""
    }
}