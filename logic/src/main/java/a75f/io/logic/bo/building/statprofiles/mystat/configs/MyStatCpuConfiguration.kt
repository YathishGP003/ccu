package a75f.io.logic.bo.building.statprofiles.mystat.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.util.MinMaxConfig
import a75f.io.logic.bo.building.statprofiles.util.MyStatCpuMinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatCpuConfiguration(nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective): MyStatConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType, model = model) {

    lateinit var analogOut1MinMaxConfig: MyStatCpuMinMaxConfig
    lateinit var analogOut1FanSpeedConfig: MyStatFanConfig
    lateinit var coolingStageFanConfig: MyStatCpuStagedConfig
    lateinit var heatingStageFanConfig: MyStatCpuStagedConfig
    lateinit var recirculateFanConfig: ValueConfig

    override fun getDefaultConfiguration(): MyStatConfiguration {
        val configuration = super.getDefaultConfiguration()
        configuration.apply {
            analogOut1MinMaxConfig = MyStatCpuMinMaxConfig(
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinCooling, model), getDefaultValConfig(DomainName.analog1MaxCooling, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinLinearFanSpeed, model), getDefaultValConfig(DomainName.analog1MaxLinearFanSpeed, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinHeating, model), getDefaultValConfig(DomainName.analog1MaxHeating, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinDCVDamper, model), getDefaultValConfig(DomainName.analog1MaxDCVDamper, model))
            )
            analogOut1FanSpeedConfig =
                MyStatFanConfig(
                    getDefaultValConfig(DomainName.analog1FanLow, model),
                    getDefaultValConfig(DomainName.analog1FanHigh, model)
                )
            coolingStageFanConfig =
                MyStatCpuStagedConfig(
                    getDefaultValConfig(DomainName.fanOutCoolingStage1, model),
                    getDefaultValConfig(DomainName.fanOutCoolingStage2, model),
                )
            heatingStageFanConfig =
                MyStatCpuStagedConfig(
                    getDefaultValConfig(DomainName.fanOutHeatingStage1, model),
                    getDefaultValConfig(DomainName.fanOutHeatingStage2, model),
                )
            recirculateFanConfig = getDefaultValConfig(DomainName.analog1FanRecirculate, model)
        }
        return configuration
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            addAll(super.getValueConfigs())
            analogOut1MinMaxConfig.apply {
                add(cooling.min)
                add(cooling.max)
                add(linearFanSpeed.min)
                add(linearFanSpeed.max)
                add(heating.min)
                add(heating.max)
                add(dcvDamperConfig.min)
                add(dcvDamperConfig.max)
                add(coolingStageFanConfig.stage1)
                add(coolingStageFanConfig.stage2)
                add(heatingStageFanConfig.stage1)
                add(heatingStageFanConfig.stage2)
                add(recirculateFanConfig)
            }

            analogOut1FanSpeedConfig.apply {
                add(low)
                add(high)
            }
        }
    }

    override fun getActiveConfiguration(): MyStatConfiguration {
        val cpuEquip =
            Domain.hayStack.readEntity("domainName == \"${ModelNames.myStatCpu}\" and group == \"$nodeAddress\"")
        if (cpuEquip.isEmpty()) {
            return this
        }

        val myStatCpuEquip = MyStatCpuEquip(cpuEquip[Tags.ID].toString())
        val configuration = this.getDefaultConfiguration()
        configuration.getActiveConfiguration(myStatCpuEquip)
        readCpuActiveConfig(myStatCpuEquip)
        return this
    }

    private fun readCpuActiveConfig(equip: MyStatCpuEquip) {
        analogOut1MinMaxConfig.apply {
            cooling.min.currentVal =
                getActivePointValue(equip.analog1MinCooling, cooling.min)
            cooling.max.currentVal =
                getActivePointValue(equip.analog1MaxCooling, cooling.max)
            linearFanSpeed.min.currentVal =
                getActivePointValue(equip.analog1MinLinearFanSpeed, linearFanSpeed.min)
            linearFanSpeed.max.currentVal =
                getActivePointValue(equip.analog1MaxLinearFanSpeed, linearFanSpeed.max)
            heating.min.currentVal =
                getActivePointValue(equip.analog1MinHeating, heating.min)
            heating.max.currentVal =
                getActivePointValue(equip.analog1MaxHeating, heating.max)
            dcvDamperConfig.min.currentVal =
                getActivePointValue(equip.analog1MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal =
                getActivePointValue(equip.analog1MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut1FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog1FanLow, low)
            high.currentVal = getActivePointValue(equip.analog1FanHigh, high)
        }
        coolingStageFanConfig.apply {
            stage1.currentVal = getActivePointValue(equip.fanOutCoolingStage1, stage1)
            stage2.currentVal = getActivePointValue(equip.fanOutCoolingStage2, stage2)
        }
        heatingStageFanConfig.apply {
            stage1.currentVal = getActivePointValue(equip.fanOutHeatingStage1, stage1)
            stage2.currentVal = getActivePointValue(equip.fanOutHeatingStage2, stage2)
        }

        recirculateFanConfig.apply {
            recirculateFanConfig.currentVal = getActivePointValue(equip.analog1FanRecirculate, recirculateFanConfig)
        }
    }

    override fun getRelayMap(): Map<String, Boolean> {
        val relays = mutableMapOf<String, Boolean>()
        relays[DomainName.relay1] = isRelayExternalMapped(relay1Enabled, relay1Association)
        relays[DomainName.relay2] = isRelayExternalMapped(relay2Enabled, relay2Association)
        relays[DomainName.relay3] = isRelayExternalMapped(relay3Enabled, relay3Association)
        relays[DomainName.relay4] = isRelayExternalMapped(relay4Enabled, relay4Association)
        return relays
    }

    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        analogOuts[DomainName.analog1Out] = Pair(
            isAnalogExternalMapped(
                analogOut1Enabled,
                analogOut1Association
            ),
            analogType(analogOut1Enabled)
        )
        return analogOuts
    }


    private fun analogType(analogOutPort: EnableConfig): String {
        return when (analogOutPort) {
            analogOut1Enabled -> getPortType(analogOut1Association, analogOut1MinMaxConfig)
            else -> "0-10v"
        }
    }

    private fun getPortType(
        association: AssociationConfig,
        minMaxConfig: MyStatCpuMinMaxConfig
    ): String {
        val portType: String
        when (association.associationVal) {
            MyStatCpuAnalogOutMapping.COOLING.ordinal -> {
                portType =
                    "${minMaxConfig.cooling.min.currentVal.toInt()}-${minMaxConfig.cooling.max.currentVal.toInt()}v"
            }

            MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal -> {
                portType =
                    "${minMaxConfig.linearFanSpeed.min.currentVal.toInt()}-${minMaxConfig.linearFanSpeed.max.currentVal.toInt()}v"
            }

            MyStatCpuAnalogOutMapping.HEATING.ordinal -> {
                portType =
                    "${minMaxConfig.heating.min.currentVal.toInt()}-${minMaxConfig.heating.max.currentVal.toInt()}v"
            }

            MyStatCpuAnalogOutMapping.DCV_DAMPER.ordinal -> {
                portType =
                    "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                // Staged Fan is also covered in else part. Because staged will be based on the actual voltage in the configuration. Like during stage1 stage2.
                portType = "0-10v"
            }
        }
        return portType
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            analogOut1MinMaxConfig.apply {
                add(cooling.min)
                add(cooling.max)
                add(linearFanSpeed.min)
                add(linearFanSpeed.max)
                add(heating.min)
                add(heating.max)
                add(dcvDamperConfig.min)
                add(dcvDamperConfig.max)
                add(coolingStageFanConfig.stage1)
                add(coolingStageFanConfig.stage2)
                add(heatingStageFanConfig.stage1)
                add(heatingStageFanConfig.stage2)
                add(recirculateFanConfig)
            }

            analogOut1FanSpeedConfig.apply {
                add(low)
                add(high)
            }
        }
    }

    fun isCoolingAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return (isAnyRelayEnabledAssociated(relays, MyStatCpuRelayMapping.COOLING_STAGE_1.ordinal)
                || isAnyRelayEnabledAssociated(relays, MyStatCpuRelayMapping.COOLING_STAGE_2.ordinal)
                || isAnalogEnabledAssociated(association = MyStatCpuAnalogOutMapping.COOLING.ordinal))
    }

    fun isHeatingAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return (isAnyRelayEnabledAssociated(relays, MyStatCpuRelayMapping.HEATING_STAGE_1.ordinal)
                || isAnyRelayEnabledAssociated(relays, MyStatCpuRelayMapping.HEATING_STAGE_2.ordinal)
                || isAnalogEnabledAssociated(association = MyStatCpuAnalogOutMapping.HEATING.ordinal))
    }

    fun getLowestFanSelected(): MyStatCpuRelayMapping? {
        val lowestSelected = getLowestStage(MyStatCpuRelayMapping.FAN_LOW_SPEED.ordinal,  MyStatCpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        if (lowestSelected == -1) {
            return null
        }
        return MyStatCpuRelayMapping.values()[lowestSelected]
    }

    private fun getHighestCoolingStage(): MyStatCpuRelayMapping {
        val highestSelected = getHighestStage(MyStatCpuRelayMapping.COOLING_STAGE_1.ordinal, MyStatCpuRelayMapping.COOLING_STAGE_2.ordinal)
        return MyStatCpuRelayMapping.values()[highestSelected]
    }

    private fun getHighestHeatingStage(): MyStatCpuRelayMapping {
        val highestSelected = getHighestStage(MyStatCpuRelayMapping.HEATING_STAGE_1.ordinal, MyStatCpuRelayMapping.HEATING_STAGE_2.ordinal)
        return MyStatCpuRelayMapping.values()[highestSelected]
    }

    fun getHighestCoolingStageCount(): Int {
        return try {
            getHighestCoolingStage().ordinal + 1
        } catch (e: ArrayIndexOutOfBoundsException) {
            0
        }
    }

    fun getHighestHeatingStageCount(): Int {
        return try {
            getHighestHeatingStage().ordinal - 1
        } catch (e: ArrayIndexOutOfBoundsException) {
            0
        }
    }

    override fun getHighestFanStageCount(): Int {
        val found = getHighestStage(
            MyStatCpuRelayMapping.FAN_LOW_SPEED.ordinal,
            MyStatCpuRelayMapping.FAN_HIGH_SPEED.ordinal
        )
        return when (found) {
            MyStatCpuRelayMapping.FAN_LOW_SPEED.ordinal -> 1
            MyStatCpuRelayMapping.FAN_HIGH_SPEED.ordinal -> 2
            else -> 0
        }
    }
}

private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) =
    (enabled.enabled && association.associationVal == MyStatCpuRelayMapping.EXTERNALLY_MAPPED.ordinal)

private fun isAnalogExternalMapped(enabled: EnableConfig, association: AssociationConfig) =
    (enabled.enabled && association.associationVal == MyStatCpuAnalogOutMapping.EXTERNALLY_MAPPED.ordinal)


enum class MyStatCpuRelayMapping(val displayName: String) {
    COOLING_STAGE_1("Cooling Stage 1"),
    COOLING_STAGE_2("Cooling Stage 2"),
    HEATING_STAGE_1("Heating Stage 1"),
    HEATING_STAGE_2("Heating Stage 2"),
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper")
}

enum class MyStatCpuAnalogOutMapping(val displayName: String) {
    COOLING("Cooling"),
    LINEAR_FAN_SPEED("Linear Fan"),
    HEATING("Heating"),
    STAGED_FAN_SPEED("Staged Fan"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper")
}

data class MyStatCpuAnalogOutConfigs(
    val enabled: Boolean,
    val association: Int,
    val minMax: MyStatCpuMinMaxConfig,
    val fanSpeed: MyStatFanConfig,
    val recirculateConfig: Double
)

data class MyStatCpuStagedConfig(
    val stage1: ValueConfig,
    val stage2: ValueConfig,
)