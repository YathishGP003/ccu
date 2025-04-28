
package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.device.cm.TemperatureSensorBusMapping
import a75f.io.device.cm.getAdvancedAhuAnalogInputMappings
import a75f.io.device.cm.getAdvancedAhuThermistorMappings
import a75f.io.device.cm.getCo2DomainName
import a75f.io.device.cm.getHumidityDomainName
import a75f.io.device.cm.getOccupancyDomainName
import a75f.io.device.cm.getTemperatureDomainName
import a75f.io.device.cm.getUniversalInputSensorMapping
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.equips.TIEquip
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuRelayAssociationType
import a75f.io.logic.bo.building.system.UniversalInputAssociationType
import a75f.io.logic.bo.building.system.getDomainPressure
import a75f.io.logic.bo.building.system.getPressureDomainForAnalogOut
import a75f.io.logic.bo.building.system.relayAssociationDomainNameToType
import a75f.io.logic.bo.building.system.relayAssociationToDomainName
import a75f.io.logic.bo.building.system.util.AdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.CmConfiguration
import a75f.io.logic.bo.building.system.util.ConnectConfiguration
import a75f.io.logic.bo.building.system.util.SensorAssociationConfig
import android.text.Html
import android.text.Spanned


/**
 * Created by Manjunath K on 04-07-2024.
 */


data class Sensors(
        var temp: List<String?>, var occupancy: List<String?>, var co2: List<String?>,
        var humidity: List<String?>, var analogIn1: String?, var analogIn2: String?,
        var th1: String?, var th2: String?, var universalInputs: List<String?>)

private fun isAnalogOutMapped(enabled: EnableConfig, association: AssociationConfig, mappedTo: AdvancedAhuAnalogOutAssociationType) =
        enabled.enabled && association.associationVal == mappedTo.ordinal

private fun isAnalogOutMappedConnectModule(enabled: EnableConfig, association: AssociationConfig, mappedTo: ConnectControlType) =
    enabled.enabled && association.associationVal == mappedTo.ordinal

private fun isUniversalMappedConnectModule(enabled: EnableConfig, association: AssociationConfig, mappedTo: UniversalInputAssociationType) =
    enabled.enabled && association.associationVal == mappedTo.ordinal

private fun isSensorBusMappedConnectModule(enabled: EnableConfig, association:SensorAssociationConfig, mappedTo: TemperatureSensorBusMapping) =
    enabled.enabled && association.temperatureAssociation.associationVal == mappedTo.ordinal

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

private fun isAnyAnalogOutMappedConnectModule(config:ConnectConfiguration, mappedTo: ConnectControlType): Boolean {
    return listOf(
        config.analogOut1Enabled to config.analogOut1Association,
        config.analogOut2Enabled to config.analogOut2Association,
        config.analogOut3Enabled to config.analogOut3Association,
        config.analogOut4Enabled to config.analogOut4Association
    ).any { (enabled, association) -> isAnalogOutMappedConnectModule(enabled, association, mappedTo) }
}

private fun isAnyUniversalMapped(config:ConnectConfiguration , mappedTo: UniversalInputAssociationType): Boolean {
    return listOf(
        config.universal1InEnabled to config.universal1InAssociation,
        config.universal2InEnabled to config.universal2InAssociation,
        config.universal3InEnabled to config.universal3InAssociation,
        config.universal4InEnabled to config.universal4InAssociation,
        config.universal5InEnabled to config.universal5InAssociation,
        config.universal6InEnabled to config.universal6InAssociation,
        config.universal7InEnabled to config.universal7InAssociation,
        config.universal8InEnabled to config.universal8InAssociation
    ).any { (enabled, association) -> isUniversalMappedConnectModule(enabled, association, mappedTo) }
}
private fun isAnySensorBusMappedTempSensor(config:ConnectConfiguration, mappedTo: TemperatureSensorBusMapping): Boolean {
    return listOf(
        config.address0Enabled to config.address1SensorAssociation,
        config.address1Enabled to config.address1SensorAssociation,
        config.address2Enabled to config.address1SensorAssociation,
        config.address3Enabled to config.address1SensorAssociation
    ).any { (enabled, association) -> isSensorBusMappedConnectModule(enabled,association, mappedTo) }
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

fun findPressureDuplicate(pressureData: Triple<String?, String?, String?>): Pair<Boolean, Spanned> {
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

fun validateDuplicatePressure(primary: String?, mapping1: String?, mapping2: String?): Pair<Boolean, Spanned> {
    if (primary != null) {
        if (mapping1 != null && mapping1.contentEquals(primary)) {
            return Pair(false, duplicateError(getDisForDomain(primary)))
        }
        if (mapping2 != null && mapping2.contentEquals(primary)) {
            return Pair(false, duplicateError(getDisForDomain(primary)))
        }
        if (primary.contains("1_") && ((mapping1 != null && mapping1.contains("1_")) || (mapping2 != null && mapping2.contains("1_")))) {
            return Pair(false, duplicateError(getDisForDomain(primary)))
        }
        if (primary.contains("2_") && ((mapping1 != null && mapping1.contains("2_")) || (mapping2 != null && mapping2.contains("2_")))) {
            return Pair(false, duplicateError(getDisForDomain(primary)))
        }
        if (primary.contains("3_") && ((mapping1 != null && mapping1.contains("3_")) || (mapping2 != null && mapping2.contains("3_")))) {
            return Pair(false, duplicateError(getDisForDomain(primary)))
        }
    }

    return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
}

fun isValidSatSensorSelection(config: CmConfiguration): Pair<Boolean, Spanned> {
    val sensorsMapping = getSensorMapping(config)
    val list = mutableListOf<String?>()

    list.apply {
        addAll(sensorsMapping.temp)
        addAll(sensorsMapping.occupancy)
        addAll(sensorsMapping.co2)
        addAll(sensorsMapping.humidity)
        if (sensorsMapping.analogIn1 != null) {
            add(sensorsMapping.analogIn1!!)
        }
        if (sensorsMapping.analogIn2 != null) {
            add(sensorsMapping.analogIn2!!)
        }
        if (sensorsMapping.th1 != null) {
            add(sensorsMapping.th1!!)
        }
        if (sensorsMapping.th2 != null) {
            add(sensorsMapping.th2!!)
        }
    }

    val satSequence = validateSatSequence(sensorsMapping)
    if (!satSequence.first) {
        return satSequence
    }

    if (isAOHeatingSatAvailable(config)) {
        if (!isSatSensorAvailable(sensorsMapping))
            return Pair(false, Html.fromHtml(NO_SAT_HEATING_SENSOR, Html.FROM_HTML_MODE_LEGACY))
    }
    if (isAOCoolingSatAvailable(config)) {
        if (!isSatSensorAvailable(sensorsMapping))
            return Pair(false, Html.fromHtml(NO_SAT_COOLING_SENSOR, Html.FROM_HTML_MODE_LEGACY))
    }

    val duplicateSensor = findAndGetDuplicate(list)
    if (duplicateSensor != null) {
        return Pair(false, duplicateError(duplicateSensor))
    }
    return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
}

private fun isSatSensorAvailable(sensors: Sensors): Boolean {
    val satSensors = getSatSensors(sensors)
    for (item in satSensors) {
        when (item) {
            DomainName.supplyAirTemperature1,
            DomainName.supplyAirTemperature2,
            DomainName.supplyAirTemperature3 -> return true
        }
    }
    return false
}

fun findAndGetDuplicate(list: MutableList<String?>): String? {
    val seen = mutableSetOf<String?>()
    for (item in list) {
        if (item == null) continue
        if (!seen.add(item)) {
            return item
        }
    }
    return null
}

fun getSensorMapping(config: CmConfiguration): Sensors {
    val sensors = Sensors(emptyList(), emptyList(), emptyList(), emptyList(), null, null, null, null, emptyList())
    val temp = mutableListOf<String?>()
    val occupancy = mutableListOf<String?>()
    val co2 = mutableListOf<String?>()
    val humidity = mutableListOf<String?>()
    if (config.address0Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address0SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address0SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address0SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address0SensorAssociation.humidityAssociation.associationVal))
    }
    if (config.address1Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address1SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address1SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address1SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address1SensorAssociation.humidityAssociation.associationVal))
    }
    if (config.address2Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address2SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address2SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address2SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address2SensorAssociation.humidityAssociation.associationVal))
    }
    if (config.address3Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address3SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address3SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address3SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address3SensorAssociation.humidityAssociation.associationVal))
    }

    sensors.apply {
        this.temp = temp
        this.occupancy = occupancy
        this.co2 = co2
        this.humidity = humidity
    }

    val analogSensorList = getAdvancedAhuAnalogInputMappings()
    val thermistorList = getAdvancedAhuThermistorMappings()
    if (config.analog1InEnabled.enabled) {
        val index = config.analog1InAssociation.associationVal
        sensors.analogIn1 = if (index in 1 until analogSensorList.size) analogSensorList[index]!!.domainName else null
    }
    if (config.analog2InEnabled.enabled) {
        val index = config.analog2InAssociation.associationVal
        sensors.analogIn2 = if (index in 1 until analogSensorList.size) analogSensorList[index]!!.domainName else null
    }
    if (config.thermistor1Enabled.enabled) {
        val index = config.thermistor1Association.associationVal
        sensors.th1 = if (index in 1 until thermistorList.size) thermistorList[index]!!.domainName else null
    }
    if (config.thermistor2Enabled.enabled) {
        val index = config.thermistor2Association.associationVal
        sensors.th2 = if (index in 1 until thermistorList.size) thermistorList[index]!!.domainName else null
    }
    return sensors
}


fun getSensorMapping(config: ConnectConfiguration): Sensors {
    val sensors = Sensors(emptyList(), emptyList(), emptyList(), emptyList(), null, null, null, null, emptyList())
    val temp = mutableListOf<String?>()
    val occupancy = mutableListOf<String?>()
    val co2 = mutableListOf<String?>()
    val humidity = mutableListOf<String?>()
    val universalInput = mutableListOf<String?>()

    if (config.address0Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address0SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address0SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address0SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address0SensorAssociation.humidityAssociation.associationVal))
    }
    if (config.address1Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address1SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address1SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address1SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address1SensorAssociation.humidityAssociation.associationVal))
    }
    if (config.address2Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address2SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address2SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address2SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address2SensorAssociation.humidityAssociation.associationVal))
    }
    if (config.address3Enabled.enabled) {
        temp.add(getTemperatureDomainName(config.address3SensorAssociation.temperatureAssociation.associationVal))
        occupancy.add(getOccupancyDomainName(config.address3SensorAssociation.occupancyAssociation.associationVal))
        co2.add(getCo2DomainName(config.address3SensorAssociation.co2Association.associationVal))
        humidity.add(getHumidityDomainName(config.address3SensorAssociation.humidityAssociation.associationVal))
    }

    val universalInputs = getUniversalInputSensorMapping()
    listOf(Pair(config.universal1InEnabled.enabled, config.universal1InAssociation.associationVal), Pair(config.universal2InEnabled.enabled, config.universal2InAssociation.associationVal), Pair(config.universal3InEnabled.enabled, config.universal3InAssociation.associationVal), Pair(config.universal4InEnabled.enabled, config.universal4InAssociation.associationVal), Pair(config.universal5InEnabled.enabled, config.universal5InAssociation.associationVal), Pair(config.universal6InEnabled.enabled, config.universal6InAssociation.associationVal), Pair(config.universal7InEnabled.enabled, config.universal7InAssociation.associationVal), Pair(config.universal8InEnabled.enabled, config.universal8InAssociation.associationVal)).forEach {
        if (it.first) {
            universalInput.add(if (it.second in 1 until universalInputs.size) universalInputs[it.second]!!.domainName else null)
        }
    }
    sensors.apply {
        this.temp = temp
        this.occupancy = occupancy
        this.co2 = co2
        this.humidity = humidity
        this.universalInputs = universalInput
    }
    return sensors
}


fun getSatSensors(sensors: Sensors): MutableList<String?> {
    val satSensors = mutableListOf<String?>()
    satSensors.addAll(sensors.temp)
    satSensors.add(sensors.analogIn1)
    satSensors.add(sensors.analogIn2)
    satSensors.add(sensors.th1)
    satSensors.add(sensors.th2)
    return satSensors
}

fun validateSatSequence(sensors: Sensors): Pair<Boolean, Spanned> {
    val satSensors = getSatSensors(sensors)
    var hasTemp1 = false
    var hasTemp2 = false
    var hasTemp3 = false

    for (item in satSensors) {
        when (item) {
            DomainName.supplyAirTemperature1 -> hasTemp1 = true
            DomainName.supplyAirTemperature2 -> hasTemp2 = true
            DomainName.supplyAirTemperature3 -> hasTemp3 = true
        }
    }
    if (hasTemp3 && !hasTemp2) {
        return Pair(false, Html.fromHtml(SAT_2_MUST_ERROR, Html.FROM_HTML_MODE_LEGACY))
    }
    if (hasTemp2 && !hasTemp1) {
        return Pair(false, Html.fromHtml(SAT_1_MUST_ERROR, Html.FROM_HTML_MODE_LEGACY))
    }
    return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
}

fun checkTIValidation(profileConfiguration: AdvancedHybridAhuConfig): Pair<Boolean, Spanned> {
    val tiEquip = CCUHsApi.getInstance().readEntity("equip and ti")
    if (tiEquip.isNullOrEmpty())
        return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
    val tiDomainEquip = TIEquip(tiEquip[Tags.ID].toString())

    val th1isUsedInTI = (tiDomainEquip.roomTemperatureType.readDefaultVal()
        .toInt() == 1 || tiDomainEquip.supplyAirTemperatureType.readDefaultVal().toInt() == 1)
    val th2isUsedInTI = (tiDomainEquip.roomTemperatureType.readDefaultVal()
        .toInt() == 2 || tiDomainEquip.supplyAirTemperatureType.readDefaultVal().toInt() == 2)
    val addressBusIsUsedInTI = (tiDomainEquip.roomTemperatureType.readDefaultVal().toInt() == 0)

    if (profileConfiguration.cmConfiguration.thermistor1Enabled.enabled && th1isUsedInTI) {
        return Pair(
            false,
            Html.fromHtml(
                "Th1 is used in TI zone please desable to use it here",
                Html.FROM_HTML_MODE_LEGACY
            )
        )
    }
    if (profileConfiguration.cmConfiguration.thermistor2Enabled.enabled && th2isUsedInTI) {
        return Pair(
            false,
            Html.fromHtml(
                "Th1 is used in TI zone please desable to use it here",
                Html.FROM_HTML_MODE_LEGACY
            )
        )
    }
    if ((profileConfiguration.cmConfiguration.address0SensorAssociation.temperatureAssociation.associationVal > 0 ||
                profileConfiguration.cmConfiguration.address1SensorAssociation.temperatureAssociation.associationVal > 0 ||
                profileConfiguration.cmConfiguration.address2SensorAssociation.temperatureAssociation.associationVal > 0 ||
                profileConfiguration.cmConfiguration.address3SensorAssociation.temperatureAssociation.associationVal > 0
                ) && addressBusIsUsedInTI
    ) {
        return Pair(
            false,
            Html.fromHtml(
                "Address bus is used in TI zone please desable to use it here",
                Html.FROM_HTML_MODE_LEGACY
            )
        )
    }
    return Pair(true, Html.fromHtml("success", Html.FROM_HTML_MODE_LEGACY))
}


fun isValidateConfiguration(
    profileConfiguration: AdvancedHybridAhuConfig,
    analogOutMappedToOaoDamper: Boolean): Pair<Boolean, Spanned> {

    var isPressureAvailable = false
    if (isRelayPressureFanAvailable(profileConfiguration.cmConfiguration)) {
        if (!isAOPressureAvailable(profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml(PRESSURE_RELAY_ERROR, Html.FROM_HTML_MODE_LEGACY))
        }
        if (!isPressureSensorAvailable(profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml(NO_PRESSURE_SENSOR_ERROR, Html.FROM_HTML_MODE_LEGACY))
        }
        isPressureAvailable = true
    }
    if (isAOPressureAvailable(profileConfiguration.cmConfiguration)) {
        if (!isPressureSensorAvailable(profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml(NO_PRESSURE_SENSOR_ERROR, Html.FROM_HTML_MODE_LEGACY))
        }
        isPressureAvailable = true
    }

    val pressureDomainName = getDomainPressure(profileConfiguration.cmConfiguration.sensorBus0PressureEnabled.enabled, profileConfiguration.cmConfiguration.address0SensorAssociation.pressureAssociation!!.associationVal)
    val analogIn1DomainName = getPressureDomainForAnalogOut(profileConfiguration.cmConfiguration.analog1InEnabled.enabled, profileConfiguration.cmConfiguration.analog1InAssociation.associationVal)
    val analogIn2DomainName = getPressureDomainForAnalogOut(profileConfiguration.cmConfiguration.analog2InEnabled.enabled, profileConfiguration.cmConfiguration.analog2InAssociation.associationVal)

    if (isPressureAvailable && pressureDomainName == null && analogIn1DomainName == null && analogIn2DomainName == null) {
        return Pair(false, Html.fromHtml(NO_PRESSURE_SENSOR_ERROR, Html.FROM_HTML_MODE_LEGACY))
    }

    val pressureStatus = findPressureDuplicate(Triple(pressureDomainName, analogIn1DomainName, analogIn2DomainName))
    if (!pressureStatus.first) {
        return pressureStatus
    }

    if (isRelaySatCoolingAvailable(profileConfiguration.cmConfiguration)) {
        if (!isAOCoolingSatAvailable(profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml(COOLING_CONFIG_ERROR, Html.FROM_HTML_MODE_LEGACY))
        }
    }

    if (isRelaySatHeatingAvailable(profileConfiguration.cmConfiguration)) {
        if (!isAOHeatingSatAvailable(profileConfiguration.cmConfiguration)) {
            return Pair(false, Html.fromHtml(HEATING_CONFIG_ERROR, Html.FROM_HTML_MODE_LEGACY))
        }
    }

    val otherSensorStatus = isValidSatSensorSelection(profileConfiguration.cmConfiguration)
    if (!otherSensorStatus.first) {
        return otherSensorStatus
    }

    val connectModuleStatus = validateConnectModule(profileConfiguration)
    if (!connectModuleStatus.first) {
        return connectModuleStatus
    }

    /*if (!checkTIValidation(profileConfiguration).first) {
        return checkTIValidation(profileConfiguration)
    }*/
    // added the check only when the connect module is paired
    if(profileConfiguration.connectConfiguration.connectEnabled) {
        if (analogOutMappedToOaoDamper &&
            !profileConfiguration.connectConfiguration.enableOutsideAirOptimization.enabled
        ) {
            return Pair(false, Html.fromHtml(OAO_DAMPER_ERROR, Html.FROM_HTML_MODE_LEGACY))
        }
        if (!analogOutMappedToOaoDamper &&
            profileConfiguration.connectConfiguration.enableOutsideAirOptimization.enabled
        ) {
            return Pair(
                false,
                Html.fromHtml(OUTSIDE_AIR_OPTIMIZATION_ERROR, Html.FROM_HTML_MODE_LEGACY)
            )
        }

        if (isAnyAnalogOutMappedConnectModule(profileConfiguration.connectConfiguration,ConnectControlType.RETURN_DAMPER) && !analogOutMappedToOaoDamper) {
            return Pair(
                false,
                Html.fromHtml(RETURN_DAMPER_OAO_DAMPER_ERROR, Html.FROM_HTML_MODE_LEGACY)
            )
        }
        //MAT, SAT AND OAT MAPPING VALIDATION
        if (analogOutMappedToOaoDamper && (!(isAnyUniversalMapped(profileConfiguration.connectConfiguration,UniversalInputAssociationType.MIXED_AIR_TEMPERATURE) || isAnySensorBusMappedTempSensor(profileConfiguration.connectConfiguration,TemperatureSensorBusMapping.mixedAirTemperature))) ||
            (!(isSupplyAirTemperatureMappedInSensorBusOrUniversal(profileConfiguration).first || isSupplyAirTemperatureMappedInSensorBusOrUniversal(profileConfiguration).second)) ||
            (!isAnyUniversalMapped(profileConfiguration.connectConfiguration,UniversalInputAssociationType.OUTSIDE_TEMPERATURE))
        ) {
            return Pair(false, Html.fromHtml(MAT_OAT_SAT_NOT_MAPPED, Html.FROM_HTML_MODE_LEGACY))
        }
    }
    return Pair(true, Html.fromHtml("Success", Html.FROM_HTML_MODE_LEGACY))
}

fun validateConnectModule(profileConfiguration: AdvancedHybridAhuConfig): Pair<Boolean, Spanned> {
    val sensorsMapping = getSensorMapping(profileConfiguration.connectConfiguration)
    val list = mutableListOf<String?>()
    list.apply {
        addAll(sensorsMapping.temp)
        addAll(sensorsMapping.occupancy)
        addAll(sensorsMapping.co2)
        addAll(sensorsMapping.humidity)
        addAll(sensorsMapping.universalInputs)
    }
    val duplicateSensor = findAndGetDuplicate(list)
    if (duplicateSensor != null) {
        return Pair(false, duplicateError(duplicateSensor))
    }
    return Pair(true, Html.fromHtml("Success", Html.FROM_HTML_MODE_LEGACY))
}

fun isSupplyAirTemperatureMappedInSensorBusOrUniversal(profileConfiguration: AdvancedHybridAhuConfig): Pair<Boolean, Boolean> {
    val connectConfig = profileConfiguration.connectConfiguration
    val universalInputOrdinal1 = UniversalInputAssociationType.SUPPLY_AIR_TEMPERATURE1.ordinal
    val supplyAirTemperature1 = TemperatureSensorBusMapping.supplyAirTemperature1.ordinal
    val universalInputOrdinal2 = UniversalInputAssociationType.SUPPLY_AIR_TEMPERATURE2.ordinal
    val supplyAirTemperature2 = TemperatureSensorBusMapping.supplyAirTemperature2.ordinal
    val universalInputOrdinal3 = UniversalInputAssociationType.SUPPLY_AIR_TEMPERATURE3.ordinal
    val supplyAirTemperature3 = TemperatureSensorBusMapping.supplyAirTemperature3.ordinal

    val sensorBus = listOf(
        connectConfig.address0Enabled to connectConfig.address0SensorAssociation.temperatureAssociation,
        connectConfig.address1Enabled to connectConfig.address1SensorAssociation.temperatureAssociation,
        connectConfig.address2Enabled to connectConfig.address2SensorAssociation.temperatureAssociation,
        connectConfig.address3Enabled to connectConfig.address3SensorAssociation.temperatureAssociation
    )

    val universalInputs = listOf(
        connectConfig.universal1InEnabled to connectConfig.universal1InAssociation,
        connectConfig.universal2InEnabled to connectConfig.universal2InAssociation,
        connectConfig.universal3InEnabled to connectConfig.universal3InAssociation,
        connectConfig.universal4InEnabled to connectConfig.universal4InAssociation,
        connectConfig.universal5InEnabled to connectConfig.universal5InAssociation,
        connectConfig.universal6InEnabled to connectConfig.universal6InAssociation,
        connectConfig.universal7InEnabled to connectConfig.universal7InAssociation,
        connectConfig.universal8InEnabled to connectConfig.universal8InAssociation
    )


    val satUniversalInput = universalInputs.any { (enabled, association) ->
        enabled.enabled && (association.associationVal == universalInputOrdinal1 || association.associationVal == universalInputOrdinal2 || association.associationVal == universalInputOrdinal3)
    }
    val satSensorBus = sensorBus.any { (enabled, association) ->
        enabled.enabled && (association.associationVal == supplyAirTemperature1 || association.associationVal == supplyAirTemperature2 || association.associationVal == supplyAirTemperature3)
    }
    return Pair(satSensorBus, satUniversalInput)
}
