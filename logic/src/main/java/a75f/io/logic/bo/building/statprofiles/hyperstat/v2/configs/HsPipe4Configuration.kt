package a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstat.HsPipe4Equip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.util.FanConfig
import a75f.io.logic.bo.building.statprofiles.util.MinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HsPipe4Configuration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String,
    floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective
) : HyperStatConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model){

    lateinit var analogOut1MinMaxConfig: Pipe4MinMaxConfig
    lateinit var analogOut2MinMaxConfig: Pipe4MinMaxConfig
    lateinit var analogOut3MinMaxConfig: Pipe4MinMaxConfig
    override fun getActiveConfiguration() : HsPipe4Configuration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val hssEquip = HsPipe4Equip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        getActiveEnableConfigs(hssEquip)
        getActiveAssociationConfigs(hssEquip)
        getGenericZoneConfigs(hssEquip)
        getActiveDynamicConfigs(hssEquip)
        equipId = hssEquip.equipRef
        isDefault = false
        return this
    }



    private fun getActiveDynamicConfigs(equip: HsPipe4Equip) {
        analogOut1MinMaxConfig.apply {
            coolingModulatingValue.min.currentVal = getActivePointValue(equip.analog1MinChilledWaterValve, coolingModulatingValue.min)
            coolingModulatingValue.max.currentVal = getActivePointValue(equip.analog1MaxChilledWaterValve, coolingModulatingValue.max)
            heatingModulatingValue.min.currentVal = getActivePointValue(equip.analog1MinHotWaterValve, heatingModulatingValue.min)
            heatingModulatingValue.max.currentVal = getActivePointValue(equip.analog1MaxHotWaterValve, heatingModulatingValue.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog1MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog1MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog1MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog1MaxDCVDamper, dcvDamperConfig.max)

        }

        analogOut2MinMaxConfig.apply {
            coolingModulatingValue.min.currentVal = getActivePointValue(equip.analog2MinChilledWaterValve, coolingModulatingValue.min)
            coolingModulatingValue.max.currentVal = getActivePointValue(equip.analog2MaxChilledWaterValve, coolingModulatingValue.max)
            heatingModulatingValue.min.currentVal = getActivePointValue(equip.analog2MinHotWaterValve, heatingModulatingValue.min)
            heatingModulatingValue.max.currentVal = getActivePointValue(equip.analog2MaxHotWaterValve, heatingModulatingValue.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog2MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog2MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog2MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog2MaxDCVDamper, dcvDamperConfig.max)

        }

        analogOut3MinMaxConfig.apply {
            coolingModulatingValue.min.currentVal = getActivePointValue(equip.analog3MinChilledWaterValve, coolingModulatingValue.min)
            coolingModulatingValue.max.currentVal = getActivePointValue(equip.analog3MaxChilledWaterValve, coolingModulatingValue.max)
            heatingModulatingValue.min.currentVal = getActivePointValue(equip.analog3MinHotWaterValve, heatingModulatingValue.min)
            heatingModulatingValue.max.currentVal = getActivePointValue(equip.analog3MaxHotWaterValve, heatingModulatingValue.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog3MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog3MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog3MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog3MaxDCVDamper, dcvDamperConfig.max)

        }

        analogOut1FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog1FanLow, low)
            medium.currentVal = getActivePointValue(equip.analog1FanMedium, medium)
            high.currentVal = getActivePointValue(equip.analog1FanHigh, high)
        }

        analogOut2FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog2FanLow, low)
            medium.currentVal = getActivePointValue(equip.analog2FanMedium, medium)
            high.currentVal = getActivePointValue(equip.analog2FanHigh, high)
        }

        analogOut3FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog3FanLow, low)
            medium.currentVal = getActivePointValue(equip.analog3FanMedium, medium)
            high.currentVal = getActivePointValue(equip.analog3FanHigh, high)
        }
    }

    override fun getDefaultConfiguration(): HyperStatConfiguration {
        val configuration = super.getDefaultConfiguration() as HsPipe4Configuration
        configuration.apply {
            analogOut1MinMaxConfig =
                Pipe4MinMaxConfig(
                    getMinMax(DomainName.analog1MinChilledWaterValve, DomainName.analog1MaxChilledWaterValve),
                    getMinMax(DomainName.analog1MinHotWaterValve, DomainName.analog1MaxHotWaterValve),
                    getMinMax(DomainName.analog1MinFanSpeed, DomainName.analog1MaxFanSpeed),
                    getMinMax(DomainName.analog1MinDCVDamper, DomainName.analog1MaxDCVDamper),
                )
            analogOut2MinMaxConfig =
                Pipe4MinMaxConfig(
                    getMinMax(DomainName.analog2MinChilledWaterValve, DomainName.analog2MaxChilledWaterValve),
                    getMinMax(DomainName.analog2MinHotWaterValve, DomainName.analog2MaxHotWaterValve),
                    getMinMax(DomainName.analog2MinFanSpeed, DomainName.analog2MaxFanSpeed),
                    getMinMax(DomainName.analog2MinDCVDamper, DomainName.analog2MaxDCVDamper),
                )
            analogOut3MinMaxConfig =
                Pipe4MinMaxConfig(
                    getMinMax(DomainName.analog3MinChilledWaterValve, DomainName.analog3MaxChilledWaterValve),
                    getMinMax(DomainName.analog3MinHotWaterValve, DomainName.analog3MaxHotWaterValve),
                    getMinMax(DomainName.analog3MinFanSpeed, DomainName.analog3MaxFanSpeed),
                    getMinMax(DomainName.analog3MinDCVDamper, DomainName.analog3MaxDCVDamper),
                )

            analogOut1FanSpeedConfig =
                FanConfig(
                    getDefaultValConfig(DomainName.analog1FanLow, model),
                    getDefaultValConfig(DomainName.analog1FanMedium, model),
                    getDefaultValConfig(DomainName.analog1FanHigh, model)
                )
            analogOut2FanSpeedConfig =
                FanConfig(
                    getDefaultValConfig(DomainName.analog2FanLow, model),
                    getDefaultValConfig(DomainName.analog2FanMedium, model),
                    getDefaultValConfig(DomainName.analog2FanHigh, model)
                )
            analogOut3FanSpeedConfig =
                FanConfig(
                    getDefaultValConfig(DomainName.analog3FanLow, model),
                    getDefaultValConfig(DomainName.analog3FanMedium, model),
                    getDefaultValConfig(DomainName.analog3FanHigh, model)

                )
        }

        return configuration
    }

    override fun isCoolingAvailable(): Boolean {
        return (isCoolingStageAvailable() || isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.COOLING.ordinal))
    }

    override fun isHeatingAvailable(): Boolean {
        return (isHeatingStageAvailable() || isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.HEATING.ordinal))
    }
    private fun isCoolingStageAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return (isAnyRelayEnabledAssociated(relays, HsPipe4RelayMapping.COOLING_VALVE.ordinal))
    }

    private fun isHeatingStageAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return (isAnyRelayEnabledAssociated(relays, HsPipe4RelayMapping.HEATING_VALVE.ordinal)
                || isAnyRelayEnabledAssociated(relays, HsPipe4RelayMapping.AUX_HEATING_STAGE1.ordinal)
                || isAnyRelayEnabledAssociated(relays, HsPipe4RelayMapping.AUX_HEATING_STAGE2.ordinal))
    }

    override fun getRelayMap(): Map<String, Boolean> {
        val relays = mutableMapOf<String, Boolean>()
        relays[DomainName.relay1] = isRelayExternalMapped(relay1Enabled, relay1Association)
        relays[DomainName.relay2] = isRelayExternalMapped(relay2Enabled, relay2Association)
        relays[DomainName.relay3] = isRelayExternalMapped(relay3Enabled, relay3Association)
        relays[DomainName.relay4] = isRelayExternalMapped(relay4Enabled, relay4Association)
        relays[DomainName.relay5] = isRelayExternalMapped(relay5Enabled, relay5Association)
        relays[DomainName.relay6] = isRelayExternalMapped(relay6Enabled, relay6Association)
        return relays
    }

    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        analogOuts[DomainName.analog1Out] = Pair(isAnalogExternalMapped(analogOut1Enabled, analogOut1Association), analogType(analogOut1Enabled))
        analogOuts[DomainName.analog2Out] = Pair(isAnalogExternalMapped(analogOut2Enabled, analogOut2Association), analogType(analogOut2Enabled))
        analogOuts[DomainName.analog3Out] = Pair(isAnalogExternalMapped(analogOut3Enabled, analogOut3Association), analogType(analogOut3Enabled))
        return analogOuts
    }

    private fun analogType(analogOutPort: EnableConfig): String {
        return when (analogOutPort) {
            analogOut1Enabled -> getPortType(analogOut1Association, analogOut1MinMaxConfig)
            analogOut2Enabled -> getPortType(analogOut2Association, analogOut2MinMaxConfig)
            analogOut3Enabled -> getPortType(analogOut3Association, analogOut3MinMaxConfig)
            else -> "0-10v"
        }
    }

    private fun getPortType(association: AssociationConfig, minMaxConfig: Pipe4MinMaxConfig): String {
        val portType: String
        when (association.associationVal) {

            HsPipe4AnalogOutMapping.COOLING_MODULATING_VALUE.ordinal -> {
                portType = "${minMaxConfig.coolingModulatingValue.min.currentVal.toInt()}-${minMaxConfig.coolingModulatingValue.max.currentVal.toInt()}v"
            }
            HsPipe4AnalogOutMapping.HEATING_MODULATING_VALUE.ordinal -> {
                portType = "${minMaxConfig.heatingModulatingValue.min.currentVal.toInt()}-${minMaxConfig.heatingModulatingValue.max.currentVal.toInt()}v"
            }

            HsPipe4AnalogOutMapping.FAN_SPEED.ordinal -> {
                portType = "${minMaxConfig.fanSpeedConfig.min.currentVal.toInt()}-${minMaxConfig.fanSpeedConfig.max.currentVal.toInt()}v"
            }

            HsPipe4AnalogOutMapping.DCV_DAMPER.ordinal -> {
                portType = "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                portType = "0-10v"
            }
        }
        return portType
    }

    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == HsPipe4RelayMapping.EXTERNALLY_MAPPED.ordinal)

    private fun isAnalogExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == HsPipe4AnalogOutMapping.EXTERNALLY_MAPPED.ordinal)


    private fun getMinMax(minDomainName: String, maxDomainName: String): MinMaxConfig {
        return MinMaxConfig(
            getDefaultValConfig(minDomainName, model),
            getDefaultValConfig(maxDomainName, model)
        )
    }
    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(zoneCO2DamperOpeningRate)
            addAll(addAnalogMinMaxToList(analogOut1MinMaxConfig))
            addAll(addAnalogMinMaxToList(analogOut2MinMaxConfig))
            addAll(addAnalogMinMaxToList(analogOut3MinMaxConfig))
            addAll(addFanConfigList(analogOut1FanSpeedConfig))
            addAll(addFanConfigList(analogOut2FanSpeedConfig))
            addAll(addFanConfigList(analogOut3FanSpeedConfig))
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            addAll(super.getValueConfigs())
            addAll(addAnalogMinMaxToList(analogOut1MinMaxConfig))
            addAll(addAnalogMinMaxToList(analogOut2MinMaxConfig))
            addAll(addAnalogMinMaxToList(analogOut3MinMaxConfig))
            addAll(addFanConfigList(analogOut1FanSpeedConfig))
            addAll(addFanConfigList(analogOut2FanSpeedConfig))
            addAll(addFanConfigList(analogOut3FanSpeedConfig))
        }
    }

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            // Get the list from the superclass and remove thermistor2EnableConfig if it's present
            val superConfigs = super.getEnableConfigs().toMutableList()

            // Add all the other configs from the superclass
            addAll(superConfigs)
        }
    }

    private fun addAnalogMinMaxToList(config: Pipe4MinMaxConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.coolingModulatingValue.min)
            add(config.coolingModulatingValue.max)
            add(config.heatingModulatingValue.min)
            add(config.heatingModulatingValue.max)
            add(config.fanSpeedConfig.min)
            add(config.fanSpeedConfig.max)
            add(config.dcvDamperConfig.min)
            add(config.dcvDamperConfig.max)
        }
    }
    private fun addFanConfigList(config: FanConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.low)
            add(config.medium)
            add(config.high)
        }
    }

    private fun getHighestFanSelected(): HsPipe4RelayMapping? {
        if(isAnyRelayEnabledAssociated(association = HsPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal)) {
            val highestSelected = getHighestStage(HsPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal, HsPipe4RelayMapping.FAN_MEDIUM_SPEED.ordinal, HsPipe4RelayMapping.FAN_HIGH_SPEED.ordinal)
            if (highestSelected == -1) return null
            return HsPipe4RelayMapping.values()[highestSelected]
        } else {
            val highestSelected = getHighestStage(HsPipe4RelayMapping.FAN_LOW_SPEED.ordinal, HsPipe4RelayMapping.FAN_MEDIUM_SPEED.ordinal, HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)
            if (highestSelected == -1) return null
            return HsPipe4RelayMapping.values()[highestSelected]
        }
    }

    override fun getHighestFanStageCount(): Int {
        return when(getHighestFanSelected()) {
            HsPipe4RelayMapping.FAN_HIGH_SPEED -> 3
            HsPipe4RelayMapping.FAN_MEDIUM_SPEED -> 2
            HsPipe4RelayMapping.FAN_LOW_SPEED, HsPipe4RelayMapping.FAN_LOW_VENTILATION -> 1
            else -> { 0 }
        }
    }
}

data class Pipe4MinMaxConfig (
    val coolingModulatingValue: MinMaxConfig,
    val heatingModulatingValue: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)

enum class HsPipe4AnalogOutMapping(val displayName: String) {
    COOLING_MODULATING_VALUE("Cooling modulating valve"),
    FAN_SPEED("Fan Speed"),
    HEATING_MODULATING_VALUE("Heating modulating valve"),
    DCV_DAMPER("DCV Damper"),
    EXTERNALLY_MAPPED("Externally Mapped")
}

enum class HsPipe4RelayMapping(val displayName: String) {
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_MEDIUM_SPEED("Fan Medium Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    HEATING_VALVE("Heating Valve"),
    COOLING_VALVE("Cooling Valve"),
    AUX_HEATING_STAGE1("Aux Heating Stage 1"),
    AUX_HEATING_STAGE2("Aux Heating Stage 2"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper"),
    FAN_LOW_VENTILATION("Fan Low Ventilation Speed"),
}

data class Pipe4AnalogOutConfigs(
    val enabled: Boolean,
    val association: Int,
    val minMax: Pipe4MinMaxConfig,
    val fanSpeed: FanConfig
)


