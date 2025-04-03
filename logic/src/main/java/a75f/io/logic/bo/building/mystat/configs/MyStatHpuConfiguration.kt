package a75f.io.logic.bo.building.mystat.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.MinMaxConfig
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatHpuMinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatHpuConfiguration(
    nodeAddress: Int,
    nodeType: String,
    priority: Int,
    roomRef: String,
    floorRef: String,
    profileType: ProfileType,
    model: SeventyFiveFProfileDirective
) : MyStatConfiguration(
    nodeAddress = nodeAddress,
    nodeType = nodeType,
    priority = priority,
    roomRef = roomRef,
    floorRef = floorRef,
    profileType = profileType,
    model = model
) {

    lateinit var analogOut1MinMaxConfig: MyStatHpuMinMaxConfig
    lateinit var analogOut1FanSpeedConfig: MyStatFanConfig

    override fun getActiveConfiguration(): MyStatConfiguration {
        val hpuEquip =
            Domain.hayStack.readEntity("domainName == \"${ModelNames.myStatHpu}\" and group == \"$nodeAddress\"")
        if (hpuEquip.isEmpty()) {
            return this
        }

        val myStatHpuEquip = MyStatHpuEquip(hpuEquip[Tags.ID].toString())
        val configuration = this.getDefaultConfiguration()
        configuration.getActiveConfiguration(myStatHpuEquip)
        readHpuActiveConfig(myStatHpuEquip)
        return this
    }

    private fun readHpuActiveConfig(equip: MyStatHpuEquip) {
        analogOut1MinMaxConfig.apply {
            compressorSpeed.min.currentVal =
                getActivePointValue(equip.analog1MinCompressorSpeed, compressorSpeed.min)
            compressorSpeed.max.currentVal =
                getActivePointValue(equip.analog1MaxCompressorSpeed, compressorSpeed.max)
            fanSpeedConfig.min.currentVal =
                getActivePointValue(equip.analog1MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal =
                getActivePointValue(equip.analog1MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal =
                getActivePointValue(equip.analog1MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal =
                getActivePointValue(equip.analog1MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut1FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog1FanLow, low)
            high.currentVal = getActivePointValue(equip.analog1FanHigh, high)
        }

    }


    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            analogOut1MinMaxConfig.apply {
                add(compressorSpeed.min)
                add(compressorSpeed.max)
                add(fanSpeedConfig.min)
                add(fanSpeedConfig.max)
                add(dcvDamperConfig.min)
                add(dcvDamperConfig.max)
            }

            analogOut1FanSpeedConfig.apply {
                add(low)
                add(high)
            }
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            addAll(super.getValueConfigs())
            analogOut1MinMaxConfig.apply {
                add(compressorSpeed.min)
                add(compressorSpeed.max)
                add(fanSpeedConfig.min)
                add(fanSpeedConfig.max)
                add(dcvDamperConfig.min)
                add(dcvDamperConfig.max)
            }

            analogOut1FanSpeedConfig.apply {
                add(low)
                add(high)
            }
        }
    }

    override fun getDefaultConfiguration(): MyStatConfiguration {
        val configuration = super.getDefaultConfiguration()
        configuration.apply {
            analogOut1MinMaxConfig = MyStatHpuMinMaxConfig(
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinCompressorSpeed, model), getDefaultValConfig(DomainName.analog1MaxCompressorSpeed, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinFanSpeed, model), getDefaultValConfig(DomainName.analog1MaxFanSpeed, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinDCVDamper, model), getDefaultValConfig(DomainName.analog1MaxDCVDamper, model))
            )
            analogOut1FanSpeedConfig = MyStatFanConfig(
                getDefaultValConfig(DomainName.analog1FanLow, model),
                getDefaultValConfig(DomainName.analog1FanHigh, model)
            )
        }
        return configuration
    }


    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        analogOuts[DomainName.analog1Out] = Pair(
            isAnalogExternalMapped(analogOut1Enabled, analogOut1Association),
            analogType(analogOut1Enabled)
        )
        return analogOuts
    }

    override fun getRelayMap(): Map<String, Boolean> {
        val relays = mutableMapOf<String, Boolean>()
        relays[DomainName.relay1] = isRelayExternalMapped(relay1Enabled, relay1Association)
        relays[DomainName.relay2] = isRelayExternalMapped(relay2Enabled, relay2Association)
        relays[DomainName.relay3] = isRelayExternalMapped(relay3Enabled, relay3Association)
        relays[DomainName.relay4] = isRelayExternalMapped(relay4Enabled, relay4Association)
        return relays
    }

    private fun analogType(analogOutPort: EnableConfig): String {
        return when (analogOutPort) {
            analogOut1Enabled -> getPortType(analogOut1Association, analogOut1MinMaxConfig)
            else -> "0-10v"
        }
    }

    private fun getPortType(
        association: AssociationConfig,
        minMaxConfig: MyStatHpuMinMaxConfig
    ): String {
        val portType: String
        when (association.associationVal) {
            MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal -> {
                portType =
                    "${minMaxConfig.compressorSpeed.min.currentVal.toInt()}-${minMaxConfig.compressorSpeed.max.currentVal.toInt()}v"
            }

            MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal -> {
                portType =
                    "${minMaxConfig.fanSpeedConfig.min.currentVal.toInt()}-${minMaxConfig.fanSpeedConfig.max.currentVal.toInt()}v"
            }

            MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal -> {
                portType =
                    "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                portType = "0-10v"
            }
        }
        return portType
    }

    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) =
        (enabled.enabled && association.associationVal == MyStatHpuRelayMapping.EXTERNALLY_MAPPED.ordinal)

    private fun isAnalogExternalMapped(enabled: EnableConfig, association: AssociationConfig) =
        (enabled.enabled && association.associationVal == MyStatHpuAnalogOutMapping.EXTERNALLY_MAPPED.ordinal)

    fun getLowestFanSelected(): MyStatHpuRelayMapping? {
        val lowestSelected = getLowestStage(MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal,  MyStatHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        if (lowestSelected == -1) {
            return null
        }
        return MyStatHpuRelayMapping.values()[lowestSelected]
    }
}

enum class MyStatHpuRelayMapping(val displayName: String) {
    COMPRESSOR_STAGE1("Compressor Stage 1"),
    COMPRESSOR_STAGE2("Compressor Stage 2"),
    AUX_HEATING_STAGE1("Aux Heating Stage"),
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    CHANGE_OVER_O_COOLING("Change Over O Cooling"),
    CHANGE_OVER_B_HEATING("Change Over B Heating"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper")
}

enum class MyStatHpuAnalogOutMapping(val displayName: String) {
    COMPRESSOR_SPEED("Compressor Speed"),
    FAN_SPEED("Fan Speed"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER_MODULATION("Dcv Damper")
}
