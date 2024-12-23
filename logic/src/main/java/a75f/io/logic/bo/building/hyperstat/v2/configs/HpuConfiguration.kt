package a75f.io.logic.bo.building.hyperstat.v2.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HpuConfiguration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String,
    floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective
) : HyperStatConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model) {

    lateinit var analogOut1MinMaxConfig: HpuMinMaxConfig
    lateinit var analogOut2MinMaxConfig: HpuMinMaxConfig
    lateinit var analogOut3MinMaxConfig: HpuMinMaxConfig

    lateinit var analogOut1FanSpeedConfig: FanConfig
    lateinit var analogOut2FanSpeedConfig: FanConfig
    lateinit var analogOut3FanSpeedConfig: FanConfig


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

    private fun addAnalogMinMaxToList(config: HpuMinMaxConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.compressorConfig.min)
            add(config.compressorConfig.max)
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



    override fun getActiveConfiguration(): HpuConfiguration {

        var hpuRawEquip = Domain.hayStack.readEntity("domainName == \"${ModelNames.hyperStatHpu}\" and group == \"$nodeAddress\"")
        // Remove the bellow code after migration all the hyperstat hpu modules
        if (hpuRawEquip.isEmpty()) {
            hpuRawEquip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        }
        if (hpuRawEquip.isEmpty()) {
            return this
        }


        val hpuEquip = HpuV2Equip(hpuRawEquip[Tags.ID].toString())
        val configuration = this.getDefaultConfiguration()
        configuration.getActiveConfiguration(hpuEquip)
        readHpuActiveConfiguration(hpuEquip)
        return this
    }

    private fun readHpuActiveConfiguration(equip: HpuV2Equip) {

        analogOut1MinMaxConfig.apply {
            compressorConfig.min.currentVal = getActivePointValue(equip.analog1MinCompressorSpeed, compressorConfig.min)
            compressorConfig.max.currentVal = getActivePointValue(equip.analog1MaxCompressorSpeed, compressorConfig.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog1MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog1MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog1MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog1MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut2MinMaxConfig.apply {
            compressorConfig.min.currentVal = getActivePointValue(equip.analog2MinCompressorSpeed, compressorConfig.min)
            compressorConfig.max.currentVal = getActivePointValue(equip.analog2MaxCompressorSpeed, compressorConfig.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog2MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog2MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog2MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog2MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut3MinMaxConfig.apply {
            compressorConfig.min.currentVal = getActivePointValue(equip.analog3MinCompressorSpeed, compressorConfig.min)
            compressorConfig.max.currentVal = getActivePointValue(equip.analog3MaxCompressorSpeed, compressorConfig.max)
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
        val configuration = super.getDefaultConfiguration()
        configuration.apply {
            analogOut1MinMaxConfig = HpuMinMaxConfig(
                getMinMax(DomainName.analog1MinCompressorSpeed, DomainName.analog1MaxCompressorSpeed),
                getMinMax(DomainName.analog1MinDCVDamper, DomainName.analog1MaxDCVDamper),
                getMinMax(DomainName.analog1MinFanSpeed, DomainName.analog1MaxFanSpeed),
            )
            analogOut2MinMaxConfig = HpuMinMaxConfig(
                getMinMax(DomainName.analog2MinCompressorSpeed, DomainName.analog2MaxCompressorSpeed),
                getMinMax(DomainName.analog2MinDCVDamper, DomainName.analog1MaxDCVDamper),
                getMinMax(DomainName.analog2MinFanSpeed, DomainName.analog2MaxFanSpeed),
            )

            analogOut3MinMaxConfig = HpuMinMaxConfig(
                getMinMax(DomainName.analog3MinCompressorSpeed, DomainName.analog3MaxCompressorSpeed),
                getMinMax(DomainName.analog3MinDCVDamper, DomainName.analog3MaxDCVDamper),
                getMinMax(DomainName.analog3MinFanSpeed, DomainName.analog3MaxFanSpeed),
            )

            analogOut1FanSpeedConfig = FanConfig(
                getDefaultValConfig(DomainName.analog1FanLow, model),
                getDefaultValConfig(DomainName.analog1FanMedium, model),
                getDefaultValConfig(DomainName.analog1FanHigh, model)
            )
            analogOut2FanSpeedConfig = FanConfig(
                getDefaultValConfig(DomainName.analog2FanLow, model),
                getDefaultValConfig(DomainName.analog2FanMedium, model),
                getDefaultValConfig(DomainName.analog2FanHigh, model)
            )
            analogOut3FanSpeedConfig = FanConfig(
                getDefaultValConfig(DomainName.analog3FanLow, model),
                getDefaultValConfig(DomainName.analog3FanMedium, model),
                getDefaultValConfig(DomainName.analog3FanHigh, model)

            )

        }
        return configuration
    }

    private fun getMinMax(minDomainName: String, maxDomainName: String): MinMaxConfig {
        return MinMaxConfig(getDefaultValConfig(minDomainName, model), getDefaultValConfig(maxDomainName, model))
    }

    fun getHighestCompressorStage(): HsHpuRelayMapping {
        val highestSelected = getHighestStage(HsHpuRelayMapping.COMPRESSOR_STAGE1.ordinal, HsHpuRelayMapping.COMPRESSOR_STAGE2.ordinal, HsHpuRelayMapping.COMPRESSOR_STAGE3.ordinal)
        return HsHpuRelayMapping.values()[highestSelected]
    }

    fun getHighestFanStage(): HsHpuRelayMapping {
        val highestSelected = getHighestStage(HsHpuRelayMapping.FAN_LOW_SPEED.ordinal, HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal, HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        return HsHpuRelayMapping.values()[highestSelected]
    }

    fun getLowestFanStage(): HsHpuRelayMapping {
        val highestSelected = getLowestStage(HsHpuRelayMapping.FAN_LOW_SPEED.ordinal, HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal, HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        return HsHpuRelayMapping.values()[highestSelected]
    }


}

data class HpuMinMaxConfig(
    val compressorConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig
)


enum class HsHpuAnalogOutMapping(val displayName: String) {
    COMPRESSOR_SPEED("Compressor Speed"),
    FAN_SPEED("Fan Speed"),
    DCV_DAMPER("DCV Damper"),
    EXTERNALLY_MAPPED("Externally Mapped")
}

enum class HsHpuRelayMapping(val displayName: String) {
    COMPRESSOR_STAGE1("Compressor Stage 1"),
    COMPRESSOR_STAGE2("Compressor Stage 2"),
    COMPRESSOR_STAGE3("Compressor Stage 3"),
    AUX_HEATING_STAGE1("Aux Heating Stage 1"),
    AUX_HEATING_STAGE2("Aux Heating Stage 2"),
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_MEDIUM_SPEED("Fan Medium Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    CHANGE_OVER_O_COOLING("Change Over O Cooling"),
    CHANGE_OVER_B_HEATING("Change Over B Heating"),
    EXTERNALLY_MAPPED("Externally Mapped")
}

data class HpuAnalogOutConfigs(
        val enabled: Boolean,
        val association: Int,
        val minMax: HpuMinMaxConfig,
        val fanSpeed: FanConfig
)