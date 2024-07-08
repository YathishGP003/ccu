package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuRelayAssociationType
import a75f.io.logic.bo.building.system.relayAssociationDomainNameToType
import a75f.io.logic.bo.building.system.relayAssociationToDomainName
import a75f.io.logic.bo.building.system.vav.config.CmConfiguration

/**
 * Created by Manjunath K on 04-07-2024.
 */


fun isAnalogOutMapped(enabled: EnableConfig, association: AssociationConfig, mappedTo: AdvancedAhuAnalogOutAssociationType) = (enabled.enabled && association.associationVal == mappedTo.ordinal)

fun isRelayMapped(enabled: EnableConfig, association: AssociationConfig, mappedTo: AdvancedAhuRelayAssociationType) = (enabled.enabled && (relayAssociationDomainNameToType(relayAssociationToDomainName(association.associationVal))).ordinal == mappedTo.ordinal)


fun isAnyAOMapped(config: CmConfiguration, mappedTo: AdvancedAhuAnalogOutAssociationType): Boolean {
    if (isAnalogOutMapped(config.analogOut1Enabled, config.analogOut1Association, mappedTo)) return true
    if (isAnalogOutMapped(config.analogOut2Enabled, config.analogOut2Association, mappedTo)) return true
    if (isAnalogOutMapped(config.analogOut3Enabled, config.analogOut3Association, mappedTo)) return true
    if (isAnalogOutMapped(config.analogOut4Enabled, config.analogOut4Association, mappedTo)) return true
    return false
}

fun isAnyRelayMapped(config: CmConfiguration, mappedTo: AdvancedAhuRelayAssociationType): Boolean {
    if (isRelayMapped(config.relay1Enabled, config.relay1Association, mappedTo)) return true
    if (isRelayMapped(config.relay2Enabled, config.relay2Association, mappedTo)) return true
    if (isRelayMapped(config.relay3Enabled, config.relay3Association, mappedTo)) return true
    if (isRelayMapped(config.relay4Enabled, config.relay4Association, mappedTo)) return true
    if (isRelayMapped(config.relay5Enabled, config.relay5Association, mappedTo)) return true
    if (isRelayMapped(config.relay6Enabled, config.relay6Association, mappedTo)) return true
    if (isRelayMapped(config.relay7Enabled, config.relay7Association, mappedTo)) return true
    if (isRelayMapped(config.relay8Enabled, config.relay8Association, mappedTo)) return true
    return false
}

fun isPressureSensorAvailable(config: CmConfiguration): Boolean {
    if (config.address0SensorAssociation.pressureAssociation?.associationVal!! > 0) // dedicated for pressure
        return true
    // 12..20 is index range of pressure sensors at analog input association
    // Please do not change this range without consulting with the team
    if (config.analog1InEnabled.enabled && config.analog1InAssociation.associationVal in 12..20)
        return true
    if (config.analog2InEnabled.enabled && config.analog2InAssociation.associationVal in 12..20)
        return true
    return false
}


fun isAOPressureAvailable(config: CmConfiguration) = isAnyAOMapped(config, AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN)
fun isAOCoolingSatAvailable(config: CmConfiguration) = isAnyAOMapped(config, AdvancedAhuAnalogOutAssociationType.SAT_COOLING)
fun isAOHeatingSatAvailable(config: CmConfiguration) = isAnyAOMapped(config, AdvancedAhuAnalogOutAssociationType.SAT_HEATING)

fun isRelayPressureFanAvailable(config: CmConfiguration) = isAnyRelayMapped(config, AdvancedAhuRelayAssociationType.FAN_PRESSURE)
fun isRelaySatCoolingAvailable(config: CmConfiguration) = isAnyRelayMapped(config, AdvancedAhuRelayAssociationType.SAT_COOLING)
fun isRelaySatHeatingAvailable(config: CmConfiguration) = isAnyRelayMapped(config, AdvancedAhuRelayAssociationType.SAT_HEATING)


fun isValidateConfiguration(viewModel: AdvancedHybridAhuViewModel): Pair<Boolean, String> {

    if (isRelayPressureFanAvailable(viewModel.profileConfiguration.cmConfiguration)) {
        if (!isAOPressureAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, "Relay Pressure Fan is mapped but Analog Out Pressure Fan is not mapped")
        }
        if (!isPressureSensorAvailable(viewModel.profileConfiguration.cmConfiguration)) {
            return Pair(false, "Pressure configuration mapped but Pressure Sensor is not available")
        }
    }

    // TODO it all validations for all the configurations
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

    val pressureStatus = findPressureDuplicate(viewModel.profileConfiguration.cmConfiguration)
    if (!pressureStatus.first) {
        return pressureStatus
    }
    return Pair(true, "Success")
}

fun getDomainPressure(config: CmConfiguration): String? {
    if (config.sensorBus0PressureEnabled.enabled) {
        return when(config.address0SensorAssociation.pressureAssociation?.associationVal!!.toInt()) {
            1 -> DomainName.ductStaticPressureSensor1_2
            2 -> DomainName.ductStaticPressureSensor2_2
            3 -> DomainName.ductStaticPressureSensor3_2
            else -> null
        }
    }
    return null
}
fun getAnalog1Domain(config: CmConfiguration): String? {
    return getDomainForAnalogOut(config.analog1InEnabled, config.analog1InAssociation)
}

fun getAnalog2Domain(config: CmConfiguration): String? {
    return getDomainForAnalogOut(config.analog2InEnabled, config.analog2InAssociation)
}

fun getDomainForAnalogOut(enabled: EnableConfig, association: AssociationConfig): String? {
    if (enabled.enabled) {
        return when (association.associationVal) {
            12 -> DomainName.ductStaticPressureSensor1_1
            13 -> DomainName.ductStaticPressureSensor1_2
            14 -> DomainName.ductStaticPressureSensor1_10
            15 -> DomainName.ductStaticPressureSensor2_1
            16 -> DomainName.ductStaticPressureSensor2_2
            17 -> DomainName.ductStaticPressureSensor2_10
            18 -> DomainName.ductStaticPressureSensor3_1
            19 -> DomainName.ductStaticPressureSensor3_2
            20 -> DomainName.ductStaticPressureSensor3_10
            else -> null
        }
    }
    return null
}

fun findPressureDuplicate(config: CmConfiguration): Pair<Boolean,String> {
    val pressureDomainName = getDomainPressure(config)
    val analogIn1DomainName = getAnalog1Domain(config)
    val analogIn2DomainName = getAnalog2Domain(config)

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
    if(primary.contains("_3")) {
        if (mapping1 != null && (!mapping1.contains("_2")) && mapping2 != null && (!mapping2.contains("_2"))) {
            return Pair(false, "In order to select Pressure Sensor 3, You should select sensor 2 !")
        }
    }
    if(primary.contains("_2")) {
        if (mapping1 != null && (!mapping1.contains("_1")) && mapping2 != null && (!mapping2.contains("_1"))) {
            return Pair(false, "In order to select Pressure Sensor 2, You should select Pressure Sensor 1 !")
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
    }
    return Pair(true, "success")
}

fun getDisForDomain(domainName: String): String {
    return when(domainName) {
        DomainName.ductStaticPressureSensor1_1, DomainName.ductStaticPressureSensor2_1, DomainName.ductStaticPressureSensor3_1 -> "Duct Static Pressure Sensor (0-1in.WC)"
        DomainName.ductStaticPressureSensor1_2, DomainName.ductStaticPressureSensor2_2, DomainName.ductStaticPressureSensor3_2 -> "Duct Static Pressure Sensor (0-2in.WC)"
        DomainName.ductStaticPressureSensor1_10, DomainName.ductStaticPressureSensor2_10, DomainName.ductStaticPressureSensor3_10 -> "Duct Static Pressure Sensor (0-10in.WC)"
        else -> "Unknown"
    }
}



