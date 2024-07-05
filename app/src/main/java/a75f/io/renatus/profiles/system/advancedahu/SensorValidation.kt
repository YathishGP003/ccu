package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuRelayAssociationType
import a75f.io.logic.bo.building.system.vav.config.CmConfiguration
import a75f.io.logic.bo.building.system.vav.config.VavAdvancedHybridAhuConfig

/**
 * Created by Manjunath K on 04-07-2024.
 */


fun isAnalogOutMapped(enabled: EnableConfig, association: AssociationConfig, mappedTo: AdvancedAhuAnalogOutAssociationType) = (enabled.enabled && association.associationVal == mappedTo.ordinal)
fun isRelayMapped(enabled: EnableConfig, association: AssociationConfig, mappedTo: AdvancedAhuRelayAssociationType) = (enabled.enabled && association.associationVal == mappedTo.ordinal)

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


fun isValidateConfiguration(config: VavAdvancedHybridAhuConfig): Pair<Boolean, String> {

    if (isRelayPressureFanAvailable(config.cmConfiguration)) {
        if (!isAOPressureAvailable(config.cmConfiguration)) {
            return Pair(false, "Relay Pressure Fan is mapped but Analog Out Pressure Fan is not mapped")
        }
        if (!isPressureSensorAvailable(config.cmConfiguration)) {
            return Pair(false, "Pressure configuration mapped but Pressure Sensor is not available")
        }
    }

    // TODO it all validations for all the configurations
    if (isRelaySatCoolingAvailable(config.cmConfiguration)) {
        if (!isAOCoolingSatAvailable(config.cmConfiguration)) {
            return Pair(false, "Relay Sat Cooling is mapped but Analog Out Sat Cooling is not mapped")
        }
    }

    if (isRelaySatHeatingAvailable(config.cmConfiguration)) {
        if (!isAOHeatingSatAvailable(config.cmConfiguration)) {
            return Pair(false, "Relay Sat Heating is mapped but Analog Out Sat Heating is not mapped")
        }
    }

    val pressureStatus = findPressureDuplicate(config.cmConfiguration)
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
        val status = validatePressure(pressureDomainName, analogIn1DomainName, analogIn2DomainName)
        if (!status.first) {
            return status
        }
    }
    if (analogIn1DomainName != null) {
        val status = validatePressure(analogIn1DomainName, pressureDomainName , analogIn2DomainName)
        if (!status.first) {
            return status
        }
    }
    if (analogIn2DomainName != null) {
        val status = validatePressure(analogIn2DomainName, pressureDomainName, analogIn1DomainName)
        if (!status.first) {
            return status
        }
    }
    return Pair(true, "success")
}

fun validatePressure(primary: String?, mapping1: String?, mapping2: String?):Pair<Boolean,String> {
    if (primary != null) {
        if (mapping1 != null && mapping1.contentEquals(primary)) {
            return Pair(false, "Duplicate selection for $primary")
        }
        if (mapping2 != null && mapping2.contentEquals(primary)) {
            return Pair(false, "Duplicate selection for $primary")
        }
    }
    return Pair(true, "success")
}



