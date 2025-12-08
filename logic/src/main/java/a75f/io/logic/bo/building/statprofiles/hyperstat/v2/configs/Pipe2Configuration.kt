package a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstat.HsPipe2Equip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.util.FanConfig
import a75f.io.logic.bo.building.statprofiles.util.MinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 26-09-2024.
 */

class Pipe2Configuration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String,
    floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective
) : HyperStatConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model){

    lateinit var analogOut1MinMaxConfig: Pipe2MinMaxConfig

    lateinit var analogOut2MinMaxConfig: Pipe2MinMaxConfig
    lateinit var analogOut3MinMaxConfig: Pipe2MinMaxConfig

    lateinit var thermistor2EnableConfig: EnableConfig


    override fun getActiveConfiguration() : Pipe2Configuration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val hssEquip = HsPipe2Equip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        getActiveEnableConfigs(hssEquip)
        getActiveAssociationConfigs(hssEquip)
        getGenericZoneConfigs(hssEquip)
        getActiveDynamicConfigs(hssEquip)
        equipId = hssEquip.equipRef
        isDefault = false
        return this
    }

    private fun getActiveDynamicConfigs(equip: HsPipe2Equip) {

        analogOut1MinMaxConfig.apply {
            waterModulatingValue.min.currentVal = getActivePointValue(equip.analog1MinWaterValve, waterModulatingValue.min)
            waterModulatingValue.max.currentVal = getActivePointValue(equip.analog1MaxWaterValve, waterModulatingValue.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog1MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog1MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog1MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog1MaxDCVDamper, dcvDamperConfig.max)

        }

        analogOut2MinMaxConfig.apply {
            waterModulatingValue.min.currentVal = getActivePointValue(equip.analog2MinWaterValve, waterModulatingValue.min)
            waterModulatingValue.max.currentVal = getActivePointValue(equip.analog2MaxWaterValve, waterModulatingValue.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog2MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog2MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog2MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog2MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut3MinMaxConfig.apply {
            waterModulatingValue.min.currentVal = getActivePointValue(equip.analog3MinWaterValve, waterModulatingValue.min)
            waterModulatingValue.max.currentVal = getActivePointValue(equip.analog3MaxWaterValve, waterModulatingValue.max)
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
        val configuration = super.getDefaultConfiguration() as HyperStatConfiguration
        configuration.apply {
            analogOut1MinMaxConfig =
                Pipe2MinMaxConfig(
                    getMinMax(DomainName.analog1MinWaterValve, DomainName.analog1MaxWaterValve),
                    getMinMax(DomainName.analog1MinFanSpeed, DomainName.analog1MaxFanSpeed),
                    getMinMax(DomainName.analog1MinDCVDamper, DomainName.analog1MaxDCVDamper),
                )
            analogOut2MinMaxConfig =
                Pipe2MinMaxConfig(
                    getMinMax(DomainName.analog2MinWaterValve, DomainName.analog2MaxWaterValve),
                    getMinMax(DomainName.analog2MinFanSpeed, DomainName.analog2MaxFanSpeed),
                    getMinMax(DomainName.analog2MinDCVDamper, DomainName.analog2MaxDCVDamper),
                )

            analogOut3MinMaxConfig =
                Pipe2MinMaxConfig(
                    getMinMax(DomainName.analog3MinWaterValve, DomainName.analog3MaxWaterValve),
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
            thermistor2EnableConfig = EnableConfig(DomainName.thermistor2InputEnable, true)
        }

        return configuration
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

    private fun getPortType(association: AssociationConfig, minMaxConfig: Pipe2MinMaxConfig): String {
        val portType: String
        when (association.associationVal) {

            HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal -> {
                portType = "${minMaxConfig.waterModulatingValue.min.currentVal.toInt()}-${minMaxConfig.waterModulatingValue.max.currentVal.toInt()}v"
            }

            HsPipe2AnalogOutMapping.FAN_SPEED.ordinal -> {
                portType = "${minMaxConfig.fanSpeedConfig.min.currentVal.toInt()}-${minMaxConfig.fanSpeedConfig.max.currentVal.toInt()}v"
            }

            HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal -> {
                portType = "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                portType = "0-10v"
            }
        }
        return portType
    }

    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == HsPipe2RelayMapping.EXTERNALLY_MAPPED.ordinal)

    private fun isAnalogExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == HsPipe2AnalogOutMapping.EXTERNALLY_MAPPED.ordinal)


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
            superConfigs.remove(thermistor2Enabled)

            // Add all the other configs from the superclass
            addAll(superConfigs)

            // Add thermistor2EnableConfig at the end
            add(thermistor2EnableConfig)
        }
    }

    private fun addAnalogMinMaxToList(config: Pipe2MinMaxConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.waterModulatingValue.min)
            add(config.waterModulatingValue.max)
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

    private fun getHighestFanSelected(): HsPipe2RelayMapping? {
        if(isAnyRelayEnabledAssociated(association = HsPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal)) {
            val highestSelected = getHighestStage(HsPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal, HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal, HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)
            if (highestSelected == -1) return null
            return HsPipe2RelayMapping.values()[highestSelected]
        } else {
            val highestSelected = getHighestStage(HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal, HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal, HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)
            if (highestSelected == -1) return null
            return HsPipe2RelayMapping.values()[highestSelected]
        }
    }

    override fun getHighestFanStageCount(): Int {
       return when(getHighestFanSelected()) {
            HsPipe2RelayMapping.FAN_HIGH_SPEED -> 3
            HsPipe2RelayMapping.FAN_MEDIUM_SPEED -> 2
            HsPipe2RelayMapping.FAN_LOW_SPEED, HsPipe2RelayMapping.FAN_LOW_VENTILATION -> 1
            else -> 0
        }
    }

    override fun isCoolingAvailable() = true

    override fun isHeatingAvailable() = true
}

data class Pipe2MinMaxConfig (
    val waterModulatingValue: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)

enum class HsPipe2AnalogOutMapping(val displayName: String) {
    WATER_MODULATING_VALUE("Water modulating valve"),
    FAN_SPEED("Fan Speed"),
    DCV_DAMPER("DCV Damper"),
    EXTERNALLY_MAPPED("Externally Mapped")
}

enum class HsPipe2RelayMapping(val displayName: String) {
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_MEDIUM_SPEED("Fan Medium Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    AUX_HEATING_STAGE1("Aux Heating Stage 1"),
    AUX_HEATING_STAGE2("Aux Heating Stage 2"),
    WATER_VALVE("Water Valve"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper"),
    FAN_LOW_VENTILATION("Fan Low Ventilation Speed"),
}

data class Pipe2AnalogOutConfigs(
    val enabled: Boolean,
    val association: Int,
    val minMax: Pipe2MinMaxConfig,
    val fanSpeed: FanConfig
)



