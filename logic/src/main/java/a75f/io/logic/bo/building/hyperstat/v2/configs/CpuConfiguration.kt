package a75f.io.logic.bo.building.hyperstat.v2.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 26-09-2024.
 */

class CpuConfiguration(
        nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective
) : HyperStatConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType, model = model) {

    lateinit var analogOut1MinMaxConfig: CpuMinMaxConfig
    lateinit var analogOut2MinMaxConfig: CpuMinMaxConfig
    lateinit var analogOut3MinMaxConfig: CpuMinMaxConfig

    lateinit var analogOut1FanSpeedConfig: FanConfig
    lateinit var analogOut2FanSpeedConfig: FanConfig
    lateinit var analogOut3FanSpeedConfig: FanConfig

    lateinit var coolingStageFanConfig: CpuStagedConfig
    lateinit var heatingStageFanConfig: CpuStagedConfig
    lateinit var recirculateFanConfig: CpuRecirculateConfig

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(zoneCO2DamperOpeningRate)
            addAll(addAnalogMinMaxToList(analogOut1MinMaxConfig))
            addAll(addAnalogMinMaxToList(analogOut2MinMaxConfig))
            addAll(addAnalogMinMaxToList(analogOut3MinMaxConfig))
            addAll(addFanConfigList(analogOut1FanSpeedConfig))
            addAll(addFanConfigList(analogOut2FanSpeedConfig))
            addAll(addFanConfigList(analogOut3FanSpeedConfig))
            addAll(addStagedConfigList(coolingStageFanConfig))
            addAll(addStagedConfigList(heatingStageFanConfig))
            addAll(addRecirculateConfigList(recirculateFanConfig))
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
            addAll(addStagedConfigList(coolingStageFanConfig))
            addAll(addStagedConfigList(heatingStageFanConfig))
            addAll(addRecirculateConfigList(recirculateFanConfig))
        }
    }

    private fun addAnalogMinMaxToList(config: CpuMinMaxConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.coolingConfig.min)
            add(config.coolingConfig.max)
            add(config.linearFanSpeedConfig.min)
            add(config.linearFanSpeedConfig.max)
            add(config.heatingConfig.min)
            add(config.heatingConfig.max)
            add(config.dcvDamperConfig.min)
            add(config.dcvDamperConfig.max)
            add(config.stagedFanSpeedConfig.min)
            add(config.stagedFanSpeedConfig.max)
        }
    }

    private fun addFanConfigList(config: FanConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.low)
            add(config.medium)
            add(config.high)
        }
    }

    private fun addStagedConfigList(config: CpuStagedConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.stage1)
            add(config.stage2)
            add(config.stage3)
        }
    }

    private fun addRecirculateConfigList(config: CpuRecirculateConfig): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(config.analogOut1)
            add(config.analogOut2)
            add(config.analogOut3)
        }
    }

    override fun getActiveConfiguration(): CpuConfiguration {

        var cpuRawEquip = Domain.hayStack.readEntity("domainName == \"${ModelNames.hyperStatCpu}\" and group == \"$nodeAddress\"")
        // Remove the bellow code after migration all the hyperStat cpu modules
        if (cpuRawEquip.isEmpty()) {
            cpuRawEquip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        }
        if (cpuRawEquip.isEmpty()) {
            return this
        }

        val cpuEquip = CpuV2Equip(cpuRawEquip[Tags.ID].toString())
        val configuration = this.getDefaultConfiguration()
        configuration.getActiveConfiguration(cpuEquip)
        readCpuActiveConfiguration(cpuEquip)
        return this
    }

    private fun readCpuActiveConfiguration(equip: CpuV2Equip) {

        analogOut1MinMaxConfig.apply {
            coolingConfig.min.currentVal = getActivePointValue(equip.analog1MinCooling, coolingConfig.min)
            coolingConfig.max.currentVal = getActivePointValue(equip.analog1MaxCooling, coolingConfig.max)
            linearFanSpeedConfig.min.currentVal = getActivePointValue(equip.analog1MinLinearFanSpeed, linearFanSpeedConfig.min)
            linearFanSpeedConfig.max.currentVal = getActivePointValue(equip.analog1MaxLinearFanSpeed, linearFanSpeedConfig.max)
            heatingConfig.min.currentVal = getActivePointValue(equip.analog1MinHeating, heatingConfig.min)
            heatingConfig.max.currentVal = getActivePointValue(equip.analog1MaxHeating, heatingConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog1MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog1MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut2MinMaxConfig.apply {
            coolingConfig.min.currentVal = getActivePointValue(equip.analog2MinCooling, coolingConfig.min)
            coolingConfig.max.currentVal = getActivePointValue(equip.analog2MaxCooling, coolingConfig.max)
            linearFanSpeedConfig.min.currentVal = getActivePointValue(equip.analog2MinLinearFanSpeed, linearFanSpeedConfig.min)
            linearFanSpeedConfig.max.currentVal = getActivePointValue(equip.analog2MaxLinearFanSpeed, linearFanSpeedConfig.max)
            heatingConfig.min.currentVal = getActivePointValue(equip.analog2MinHeating, heatingConfig.min)
            heatingConfig.max.currentVal = getActivePointValue(equip.analog2MaxHeating, heatingConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog2MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog2MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut3MinMaxConfig.apply {
            coolingConfig.min.currentVal = getActivePointValue(equip.analog3MinCooling, coolingConfig.min)
            coolingConfig.max.currentVal = getActivePointValue(equip.analog3MaxCooling, coolingConfig.max)
            linearFanSpeedConfig.min.currentVal = getActivePointValue(equip.analog3MinLinearFanSpeed, linearFanSpeedConfig.min)
            linearFanSpeedConfig.max.currentVal = getActivePointValue(equip.analog3MaxLinearFanSpeed, linearFanSpeedConfig.max)
            heatingConfig.min.currentVal = getActivePointValue(equip.analog3MinHeating, heatingConfig.min)
            heatingConfig.max.currentVal = getActivePointValue(equip.analog3MaxHeating, heatingConfig.max)
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

        coolingStageFanConfig.apply {
            stage1.currentVal = getActivePointValue(equip.fanOutCoolingStage1, stage1)
            stage2.currentVal = getActivePointValue(equip.fanOutCoolingStage2, stage2)
            stage3.currentVal = getActivePointValue(equip.fanOutCoolingStage3, stage3)
        }

        heatingStageFanConfig.apply {
            stage1.currentVal = getActivePointValue(equip.fanOutHeatingStage1, stage1)
            stage2.currentVal = getActivePointValue(equip.fanOutHeatingStage2, stage2)
            stage3.currentVal = getActivePointValue(equip.fanOutHeatingStage3, stage3)
        }

        recirculateFanConfig.apply {
            analogOut1.currentVal = getActivePointValue(equip.analog1FanRecirculate, analogOut1)
            analogOut2.currentVal = getActivePointValue(equip.analog2FanRecirculate, analogOut2)
            analogOut3.currentVal = getActivePointValue(equip.analog3FanRecirculate, analogOut3)
        }

    }

    override fun getDefaultConfiguration(): HyperStatConfiguration {
        val configuration = super.getDefaultConfiguration()
        configuration.apply {
            analogOut1MinMaxConfig = CpuMinMaxConfig(
                    getMinMax(DomainName.analog1MinCooling, DomainName.analog1MaxCooling),
                    getMinMax(DomainName.analog1MinLinearFanSpeed, DomainName.analog1MaxLinearFanSpeed),
                    getMinMax(DomainName.analog1MinHeating, DomainName.analog1MaxHeating),
                    getMinMax(DomainName.analog1MinDCVDamper, DomainName.analog1MaxDCVDamper),
                    getMinMax(DomainName.analog1MinOAODamper, DomainName.analog1MaxOAODamper),
            )
            analogOut2MinMaxConfig = CpuMinMaxConfig(
                    getMinMax(DomainName.analog2MinCooling, DomainName.analog2MaxCooling),
                    getMinMax(DomainName.analog2MinLinearFanSpeed, DomainName.analog2MaxLinearFanSpeed),
                    getMinMax(DomainName.analog2MinHeating, DomainName.analog2MaxHeating),
                    getMinMax(DomainName.analog2MinDCVDamper, DomainName.analog2MaxDCVDamper),
                    getMinMax(DomainName.analog2MaxOAODamper, DomainName.analog2MaxOAODamper),
            )
            analogOut3MinMaxConfig = CpuMinMaxConfig(
                    getMinMax(DomainName.analog3MinCooling, DomainName.analog3MaxCooling),
                    getMinMax(DomainName.analog3MinLinearFanSpeed, DomainName.analog3MaxLinearFanSpeed),
                    getMinMax(DomainName.analog3MinHeating, DomainName.analog3MaxHeating),
                    getMinMax(DomainName.analog3MinDCVDamper, DomainName.analog3MaxDCVDamper),
                    getMinMax(DomainName.analog3MaxOAODamper, DomainName.analog3MaxOAODamper),
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
            coolingStageFanConfig = CpuStagedConfig(
                    getDefaultValConfig(DomainName.fanOutCoolingStage1, model),
                    getDefaultValConfig(DomainName.fanOutCoolingStage2, model),
                    getDefaultValConfig(DomainName.fanOutCoolingStage3, model)
            )
            heatingStageFanConfig = CpuStagedConfig(
                    getDefaultValConfig(DomainName.fanOutHeatingStage1, model),
                    getDefaultValConfig(DomainName.fanOutHeatingStage2, model),
                    getDefaultValConfig(DomainName.fanOutHeatingStage3, model)
            )
            recirculateFanConfig = CpuRecirculateConfig(
                    getDefaultValConfig(DomainName.analog1FanRecirculate, model),
                    getDefaultValConfig(DomainName.analog2FanRecirculate, model),
                    getDefaultValConfig(DomainName.analog3FanRecirculate, model)
            )

        }

        return configuration
    }

    private fun getMinMax(minDomainName: String, maxDomainName: String): MinMaxConfig {
        return MinMaxConfig(getDefaultValConfig(minDomainName, model), getDefaultValConfig(maxDomainName, model))
    }

    fun isCoolingAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return (isAnyRelayEnabledAssociated(relays, HsCpuRelayMapping.COOLING_STAGE_1.ordinal)
                || isAnyRelayEnabledAssociated(relays, HsCpuRelayMapping.COOLING_STAGE_2.ordinal)
                || isAnyRelayEnabledAssociated(relays, HsCpuRelayMapping.COOLING_STAGE_3.ordinal)
                || isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.COOLING.ordinal))

    }

    fun isHeatingAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return (isAnyRelayEnabledAssociated(relays, HsCpuRelayMapping.HEATING_STAGE_1.ordinal)
                || isAnyRelayEnabledAssociated(relays, HsCpuRelayMapping.HEATING_STAGE_2.ordinal)
                || isAnyRelayEnabledAssociated(relays, HsCpuRelayMapping.HEATING_STAGE_3.ordinal)
                || isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.HEATING.ordinal))
    }

    fun getHighestCoolingStage(): HsCpuRelayMapping {
        val highestSelected = getHighestStage(HsCpuRelayMapping.COOLING_STAGE_1.ordinal, HsCpuRelayMapping.COOLING_STAGE_2.ordinal, HsCpuRelayMapping.COOLING_STAGE_3.ordinal)
        return HsCpuRelayMapping.values()[highestSelected]
    }

    fun getHighestHeatingStage(): HsCpuRelayMapping {
        val highestSelected = getHighestStage(HsCpuRelayMapping.HEATING_STAGE_1.ordinal, HsCpuRelayMapping.HEATING_STAGE_2.ordinal, HsCpuRelayMapping.HEATING_STAGE_3.ordinal)
        return HsCpuRelayMapping.values()[highestSelected]
    }

    fun getLowestFanSelected(): HsCpuRelayMapping {
        val lowestSelected = getLowestStage(HsCpuRelayMapping.FAN_LOW_SPEED.ordinal, HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal, HsCpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        return HsCpuRelayMapping.values()[lowestSelected]
    }

    fun getHighestFanSelected(): HsCpuRelayMapping {
        val highestSelected = getHighestStage(HsCpuRelayMapping.FAN_LOW_SPEED.ordinal, HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal, HsCpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        return HsCpuRelayMapping.values()[highestSelected]
    }

}

// Order is important DO NOT CHANGE

enum class HsCpuAnalogOutMapping(val displayName: String) {
    COOLING("Cooling"),
    LINEAR_FAN_SPEED("Linear Fan Speed"),
    HEATING("Heating"),
    DCV_DAMPER("DCV Damper"),
    STAGED_FAN_SPEED("Staged Fan Speed"),
    EXTERNALLY_MAPPED("Externally Mapped")
}

enum class HsCpuRelayMapping(val displayName: String) {
    COOLING_STAGE_1("Cooling Stage 1"),
    COOLING_STAGE_2("Cooling Stage 2"),
    COOLING_STAGE_3("Cooling Stage 3"),
    HEATING_STAGE_1("Heating Stage 1"),
    HEATING_STAGE_2("Heating Stage 2"),
    HEATING_STAGE_3("Heating Stage 3"),
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_MEDIUM_SPEED("Fan Medium Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    EXTERNALLY_MAPPED("Externally Mapped"),
}

enum class Th2InputAssociation {
    DOOR_WINDOW_SENSOR_NC_TITLE_24,
    GENERIC_FAULT_NC,
    GENERIC_FAULT_NO
}

enum class AnalogInputAssociation {
    CURRENT_TX_0_10,
    CURRENT_TX_0_20,
    CURRENT_TX_0_50,
    KEY_CARD_SENSOR,
    DOOR_WINDOW_SENSOR_TITLE_24,
}

data class CpuMinMaxConfig(
        val coolingConfig: MinMaxConfig,
        val linearFanSpeedConfig: MinMaxConfig,
        val heatingConfig: MinMaxConfig,
        val dcvDamperConfig: MinMaxConfig,
        val stagedFanSpeedConfig: MinMaxConfig
)

data class CpuStagedConfig(
        val stage1: ValueConfig,
        val stage2: ValueConfig,
        val stage3: ValueConfig
)

data class CpuRecirculateConfig(
        val analogOut1: ValueConfig,
        val analogOut2: ValueConfig,
        val analogOut3: ValueConfig
)

data class AnalogOutConfigs(
        val enabled: Boolean,
        val association: Int,
        val minMax: CpuMinMaxConfig,
        val fanSpeed: FanConfig,
        val recirculateConfig: Double
)