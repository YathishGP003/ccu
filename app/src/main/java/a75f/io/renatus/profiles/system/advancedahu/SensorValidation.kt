
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
        DomainName.ductStaticPressureSensor1_1, DomainName.ductStaticPressureSensor2_1, DomainName.ductStaticPressureSensor3_1 -> "Duct Static Pressure Sensor (0-1in.WC)"
        DomainName.ductStaticPressureSensor1_2, DomainName.ductStaticPressureSensor2_2, DomainName.ductStaticPressureSensor3_2 -> "Duct Static Pressure Sensor (0-2in.WC)"
        DomainName.ductStaticPressureSensor1_10, DomainName.ductStaticPressureSensor2_10, DomainName.ductStaticPressureSensor3_10 -> "Duct Static Pressure Sensor (0-10in.WC)"
        else -> "Unknown"
    }
}




fun findPressureDuplicate(pressureData: Triple<String?, String?, String?>): Pair<Boolean,String> {
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
    return Pair(true, "success")
}

fun validatePressureSequence(primary: String, mapping1: String?, mapping2: String?) :Pair<Boolean,String> {
    if (primary.contains("3_")) {
        if ((mapping1 != null && (!mapping1.contains("2_")) && mapping2 != null && (!mapping2.contains("2_"))) || (mapping1 == null && mapping2 == null)) {
            return Pair(false, "In order to select Pressure Sensor 3, You should select Pressure sensor 2")
        }
        if (mapping2 == null && mapping1 != null && (!mapping1.contains("2_"))) {
            return Pair(false, "In order to select Pressure Sensor 3, You should select Pressure sensor 2")
        }
        if (mapping1 == null && mapping2 != null && (!mapping2.contains("2_"))) {
            return Pair(false, "In order to select Pressure Sensor 3, You should select Pressure sensor 2")
        }
    }
    if (primary.contains("2_")) {
        if ((mapping1 != null && (!mapping1.contains("1_")) && mapping2 != null && (!mapping2.contains("1_"))) || (mapping1 == null && mapping2 == null)) {
            return Pair(false, "In order to select Pressure Sensor 2, You should select Pressure Sensor 1")
        }
        if (mapping2 == null && mapping1 != null && (!mapping1.contains("1_"))) {
            return Pair(false, "In order to select Pressure Sensor 2, You should select Pressure sensor 1")
        }
        if (mapping1 == null && mapping2 != null && (!mapping2.contains("1_"))) {
            return Pair(false, "In order to select Pressure Sensor 2, You should select Pressure sensor 1")
        }
    }
    return Pair(true, "success")
}
fun validateDuplicatePressure(primary: String?, mapping1: String?, mapping2: String?):Pair<Boolean,String> {
    if (primary != null) {
        if (mapping1 != null && mapping1.contentEquals(primary)) {
            return Pair(false, "Duplicate selection for ${getDisForDomain(primary)} sensor")
        }
        if (mapping2 != null && mapping2.contentEquals(primary)) {
            return Pair(false, "Duplicate selection for ${getDisForDomain(primary)} sensor")
        }
        if (primary.contains("1_") && ((mapping1 != null && mapping1.contains("1_"))
                        || (mapping2 != null && mapping2.contains("1_")))) {
            return Pair(false, "Duplicate selection for ${getDisForDomain(primary)} sensor, ")
        }
        if (primary.contains("2_") && ((mapping1 != null && mapping1.contains("2_"))
                        || (mapping2 != null && mapping2.contains("2_")))) {
            return Pair(false, "Duplicate selection for ${getDisForDomain(primary)} sensor, ")
        }
        if (primary.contains("3_") && ((mapping1 != null && mapping1.contains("3_"))
                        || (mapping2 != null && mapping2.contains("3_")))) {
            return Pair(false, "Duplicate selection for ${getDisForDomain(primary)} sensor, ")
        }
    }
    return Pair(true, "success")
}

fun isValidateConfiguration(viewModel: AdvancedHybridAhuViewModel): Pair<Boolean, String> {

    var isPressureAvailable = false
    if (isRelayPressureFanAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isAOPressureAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, "Relay Pressure Fan is mapped but Analog Output Pressure Fan is not mapped")
        }
        if (!isPressureSensorAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, "Pressure configuration mapped but Pressure Sensor is not available")
        }
        isPressureAvailable = true
    }
    if (isAOPressureAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isPressureSensorAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, "Pressure configuration mapped but Pressure Sensor is not available")
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
        return Pair(false, "Pressure configuration mapped but Pressure Sensor is not available")
    }

    val pressureStatus = findPressureDuplicate(Triple(pressureDomainName, analogIn1DomainName, analogIn2DomainName))
    if (!pressureStatus.first) {
        return pressureStatus
    }

    if (isRelaySatCoolingAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isAOCoolingSatAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, "Relay Sat Cooling is mapped but Analog Out Sat Cooling is not mapped")
        }
    }

    if (isRelaySatHeatingAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isAOHeatingSatAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, "Relay Sat Heating is mapped but Analog Out Sat Heating is not mapped")
        }
    }
    return Pair(true, "Success")
}
