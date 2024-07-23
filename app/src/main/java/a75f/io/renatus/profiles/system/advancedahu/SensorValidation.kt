
package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuRelayAssociationType
import a75f.io.logic.bo.building.system.getDomainForAnalogOut
import a75f.io.logic.bo.building.system.getDomainPressure
import a75f.io.logic.bo.building.system.relayAssociationDomainNameToType
import a75f.io.logic.bo.building.system.relayAssociationToDomainName
import a75f.io.logic.bo.building.system.vav.config.CmConfiguration
import android.text.Html
import android.text.Spanned


/**
 * Created by Manjunath K on 04-07-2024.
 */


private fun isAnalogOutMapped(enabled: EnableConfig, association: AssociationConfig, mappedTo: AdvancedAhuAnalogOutAssociationType) =
        enabled.enabled && association.associationVal == mappedTo.ordinal

private fun isRelayMapped(enabled: EnableConfig, association: AssociationConfig, mappedTo: AdvancedAhuRelayAssociationType) =
        enabled.enabled && relayAssociationDomainNameToType(relayAssociationToDomainName(association.associationVal)).ordinal == mappedTo.ordinal

private fun isAnyAnalogOutMapped(config: CmConfiguration, mappedTo: AdvancedAhuAnalogOutAssociationType): Boolean {
    return listOf(
            config.analogOut1Enabled to config.analogOut1Association,
            config.analogOut2Enabled to config.analogOut2Association,
            config.analogOut3Enabled to config.analogOut3Association,
            config.analogOut4Enabled to config.analogOut4Association
    ).any { (enabled, association) -> isAnalogOutMapped(enabled, association, mappedTo) }
}

private fun isAnyRelayMapped(config: CmConfiguration, mappedTo: AdvancedAhuRelayAssociationType): Boolean {
    return listOf(
            config.relay1Enabled to config.relay1Association,
            config.relay2Enabled to config.relay2Association,
            config.relay3Enabled to config.relay3Association,
            config.relay4Enabled to config.relay4Association,
            config.relay5Enabled to config.relay5Association,
            config.relay6Enabled to config.relay6Association,
            config.relay7Enabled to config.relay7Association,
            config.relay8Enabled to config.relay8Association
    ).any { (enabled, association) -> isRelayMapped(enabled, association, mappedTo) }
}

private fun isPressureSensorAvailable(config: CmConfiguration): Boolean {
    return config.address0SensorAssociation.pressureAssociation?.associationVal?.let { it > 0 } == true ||
            listOf(config.analog1InEnabled to config.analog1InAssociation, config.analog2InEnabled to config.analog2InAssociation)
                    .any { (enabled, association) -> enabled.enabled && association.associationVal in 12..20 }
}


fun isAOPressureAvailable(config: CmConfiguration) = isAnyAnalogOutMapped(config, AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN)
fun isAOCoolingSatAvailable(config: CmConfiguration) = isAnyAnalogOutMapped(config, AdvancedAhuAnalogOutAssociationType.SAT_COOLING)
fun isAOHeatingSatAvailable(config: CmConfiguration) = isAnyAnalogOutMapped(config, AdvancedAhuAnalogOutAssociationType.SAT_HEATING)

fun isRelayPressureFanAvailable(config: CmConfiguration) = isAnyRelayMapped(config, AdvancedAhuRelayAssociationType.FAN_PRESSURE)
fun isRelaySatCoolingAvailable(config: CmConfiguration) = isAnyRelayMapped(config, AdvancedAhuRelayAssociationType.SAT_COOLING)
fun isRelaySatHeatingAvailable(config: CmConfiguration) = isAnyRelayMapped(config, AdvancedAhuRelayAssociationType.SAT_HEATING)

private fun getDisForDomain(domainName: String): String {
    return when (domainName) {
        DomainName.ductStaticPressureSensor1_1, DomainName.ductStaticPressureSensor1_2, DomainName.ductStaticPressureSensor1_10 -> "Duct Static Pressure Sensor 1"
        DomainName.ductStaticPressureSensor2_1, DomainName.ductStaticPressureSensor2_2, DomainName.ductStaticPressureSensor2_10 -> "Duct Static Pressure Sensor 2"
        DomainName.ductStaticPressureSensor3_1, DomainName.ductStaticPressureSensor3_2, DomainName.ductStaticPressureSensor3_10 -> "Duct Static Pressure Sensor 3"
        else -> "Unknown"
    }
}




fun findPressureDuplicate(pressureData: Triple<String?, String?, String?>): Pair<Boolean,Spanned> {
    val pressureDomainName = pressureData.first
    val analogIn1DomainName = pressureData.second
    val analogIn2DomainName = pressureData.third

    if (pressureDomainName != null) {
        val status = validateDuplicatePressure(pressureDomainName, analogIn1DomainName, analogIn2DomainName)
        if (!status.first) {
            return status
        }
        val sequenceStatus = validatePressureSequence(pressureDomainName, analogIn1DomainName, analogIn2DomainName)
        if (!sequenceStatus.first) {
            return sequenceStatus
        }
    }
    if (analogIn1DomainName != null) {
        val status = validateDuplicatePressure(analogIn1DomainName, pressureDomainName, analogIn2DomainName)
        if (!status.first) {
            return status
        }
        val sequenceStatus = validatePressureSequence(analogIn1DomainName, pressureDomainName, analogIn2DomainName)
        if (!sequenceStatus.first) {
            return sequenceStatus
        }
    }
    if (analogIn2DomainName != null) {
        val status = validateDuplicatePressure(analogIn2DomainName, pressureDomainName, analogIn1DomainName)
        if (!status.first) {
            return status
        }
        val sequenceStatus = validatePressureSequence(analogIn2DomainName, pressureDomainName, analogIn1DomainName)
        if (!sequenceStatus.first) {
            return sequenceStatus
        }
    }
    return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
}

fun validatePressureSequence(primary: String, mapping1: String?, mapping2: String?) :Pair<Boolean,Spanned> {
    if (primary.contains("3_")) {
        if ((mapping1 != null && (!mapping1.contains("2_")) && mapping2 != null && (!mapping2.contains("2_"))) || (mapping1 == null && mapping2 == null)) {
            return Pair(false, Html.fromHtml("Pressure sensor should be <b>selected in sequential order</b>. Please select Pressure Sensor 2 before selecting Pressure Sensor 3 in Sensor Bus and Analog Inputs.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (mapping2 == null && mapping1 != null && (!mapping1.contains("2_"))) {
            return Pair(false, Html.fromHtml("Pressure sensor should be <b>selected in sequential order</b>. Please select Pressure Sensor 2 before selecting Pressure Sensor 3 in Sensor Bus and Analog Inputs.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (mapping1 == null && mapping2 != null && (!mapping2.contains("2_"))) {
            return Pair(false, Html.fromHtml("Pressure sensor should be <b>selected in sequential order</b>. Please select Pressure Sensor 2 before selecting Pressure Sensor 3 in Sensor Bus and Analog Inputs.", Html.FROM_HTML_MODE_LEGACY))
        }
    }
    if (primary.contains("2_")) {
        if ((mapping1 != null && (!mapping1.contains("1_")) && mapping2 != null && (!mapping2.contains("1_"))) || (mapping1 == null && mapping2 == null)) {
            return Pair(false, Html.fromHtml("Pressure sensor should be <b>selected in sequential order</b>. Please select Pressure Sensor 1 before selecting Pressure Sensor 2 in Sensor Bus and Analog Inputs.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (mapping2 == null && mapping1 != null && (!mapping1.contains("1_"))) {
            return Pair(false, Html.fromHtml("Pressure sensor should be <b>selected in sequential order</b>. Please select Pressure Sensor 1 before selecting Pressure Sensor 2 in Sensor Bus and Analog Inputs.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (mapping1 == null && mapping2 != null && (!mapping2.contains("1_"))) {
            return Pair(false, Html.fromHtml("Pressure sensor should be <b>selected in sequential order</b>. Please select Pressure Sensor 1 before selecting Pressure Sensor 2 in Sensor Bus and Analog Inputs.", Html.FROM_HTML_MODE_LEGACY))
        }
    }
    return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
}
fun validateDuplicatePressure(primary: String?, mapping1: String?, mapping2: String?):Pair<Boolean,Spanned> {
    if (primary != null) {
        if (mapping1 != null && mapping1.contentEquals(primary)) {
            return Pair(false, Html.fromHtml("Duplicate selection for <b>${getDisForDomain(primary)}</b> is not allowed.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (mapping2 != null && mapping2.contentEquals(primary)) {
            return Pair(false, Html.fromHtml("Duplicate selection for <b>${getDisForDomain(primary)}</b> is not allowed.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (primary.contains("1_") && ((mapping1 != null && mapping1.contains("1_"))
                        || (mapping2 != null && mapping2.contains("1_")))) {
            return Pair(false, Html.fromHtml("Duplicate selection for <b>${getDisForDomain(primary)}</b> is not allowed.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (primary.contains("2_") && ((mapping1 != null && mapping1.contains("2_"))
                        || (mapping2 != null && mapping2.contains("2_")))) {
            return Pair(false, Html.fromHtml("Duplicate selection for <b>${getDisForDomain(primary)}</b> is not allowed.", Html.FROM_HTML_MODE_LEGACY))
        }
        if (primary.contains("3_") && ((mapping1 != null && mapping1.contains("3_"))
                        || (mapping2 != null && mapping2.contains("3_")))) {
            return Pair(false,  Html.fromHtml("Duplicate selection for ${getDisForDomain(primary)}</b> is not allowed.", Html.FROM_HTML_MODE_LEGACY))
        }
    }
    return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
}

fun isValidateConfiguration(viewModel: AdvancedHybridAhuViewModel): Pair<Boolean, Spanned> {

    var isPressureAvailable = false
    if (isRelayPressureFanAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isAOPressureAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml("Relay based Pressure Fan is mapped but <b>Pressure Fan configuration is not mapped</b>", Html.FROM_HTML_MODE_LEGACY))
        }
        if (!isPressureSensorAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml("Pressure configuration mapped but <b>Pressure Sensor is not available</b>", Html.FROM_HTML_MODE_LEGACY))
        }
        isPressureAvailable = true
    }
    if (isAOPressureAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isPressureSensorAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml("Pressure configuration mapped but <b>Pressure Sensor is not available</b>", Html.FROM_HTML_MODE_LEGACY))
        }
        isPressureAvailable = true
    }

    val pressureDomainName = getDomainPressure(
            viewModel.profileConfiguration.cmConfiguration.sensorBus0PressureEnabled.enabled,
            viewModel.profileConfiguration.cmConfiguration.address0SensorAssociation.pressureAssociation!!.associationVal
    )
    val analogIn1DomainName = getDomainForAnalogOut(
            viewModel.profileConfiguration.cmConfiguration.analog1InEnabled.enabled,
            viewModel.profileConfiguration.cmConfiguration.analog1InAssociation.associationVal
    )
    val analogIn2DomainName = getDomainForAnalogOut(
            viewModel.profileConfiguration.cmConfiguration.analog2InEnabled.enabled,
            viewModel.profileConfiguration.cmConfiguration.analog2InAssociation.associationVal
    )

    if (isPressureAvailable && pressureDomainName == null && analogIn1DomainName == null && analogIn2DomainName == null) {
        return Pair(false, Html.fromHtml("Pressure configuration mapped but <b>Pressure Sensor</b> is not available", Html.FROM_HTML_MODE_LEGACY))
    }

    val pressureStatus = findPressureDuplicate(Triple(pressureDomainName, analogIn1DomainName, analogIn2DomainName))
    if (!pressureStatus.first) {
        return pressureStatus
    }

    if (isRelaySatCoolingAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isAOCoolingSatAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml("Relay based SAT Cooling is mapped but <b>SAT Cooling configuration</b> is not mapped", Html.FROM_HTML_MODE_LEGACY))
        }
    }

    if (isRelaySatHeatingAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isAOHeatingSatAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml("Relay based SAT Heating is mapped but <b>SAT Heating configuration</b> is not mapped", Html.FROM_HTML_MODE_LEGACY))
        }
    }
    return Pair(true, Html.fromHtml("Success", Html.FROM_HTML_MODE_LEGACY))
}
