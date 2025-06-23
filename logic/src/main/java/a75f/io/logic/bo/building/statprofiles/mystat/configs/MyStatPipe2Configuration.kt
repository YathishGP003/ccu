package a75f.io.logic.bo.building.statprofiles.mystat.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.util.MinMaxConfig
import a75f.io.logic.bo.building.statprofiles.util.MyStatPipe2MinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatPipe2Configuration(nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective): MyStatConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType, model = model) {


    lateinit var analogOut1MinMaxConfig: MyStatPipe2MinMaxConfig
    lateinit var analogOut1FanSpeedConfig: MyStatFanConfig

    override fun getActiveConfiguration(): MyStatConfiguration {
        val pipe2RawEquip =
            Domain.hayStack.readEntity("domainName == \"${ModelNames.myStatPipe2}\" and group == \"$nodeAddress\"")
        if (pipe2RawEquip.isEmpty()) {
            return this
        }

        val myStatPipe2Equip = MyStatPipe2Equip(pipe2RawEquip[Tags.ID].toString())
        val configuration = this.getDefaultConfiguration()
        configuration.getActiveConfiguration(myStatPipe2Equip)
        readPipe2ActiveConfig(myStatPipe2Equip)
        return this
    }

    private fun readPipe2ActiveConfig(equip: MyStatPipe2Equip) {
        analogOut1MinMaxConfig.apply {
            waterModulatingValue.min.currentVal = getActivePointValue(equip.analog1MinWaterValve, waterModulatingValue.min)
            waterModulatingValue.max.currentVal = getActivePointValue(equip.analog1MaxWaterValve, waterModulatingValue.max)
            fanSpeedConfig.min.currentVal = getActivePointValue(equip.analog1MinFanSpeed, fanSpeedConfig.min)
            fanSpeedConfig.max.currentVal = getActivePointValue(equip.analog1MaxFanSpeed, fanSpeedConfig.max)
            dcvDamperConfig.min.currentVal = getActivePointValue(equip.analog1MinDCVDamper, dcvDamperConfig.min)
            dcvDamperConfig.max.currentVal = getActivePointValue(equip.analog1MaxDCVDamper, dcvDamperConfig.max)
        }

        analogOut1FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog1FanLow, low)
            high.currentVal = getActivePointValue(equip.analog1FanHigh, high)
        }

    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            analogOut1MinMaxConfig.apply {
                add(waterModulatingValue.min)
                add(waterModulatingValue.max)
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
                add(waterModulatingValue.min)
                add(waterModulatingValue.max)
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
            analogOut1MinMaxConfig = MyStatPipe2MinMaxConfig(
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinWaterValve, model), getDefaultValConfig(DomainName.analog1MaxWaterValve, model)),
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

    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == MyStatPipe2RelayMapping.EXTERNALLY_MAPPED.ordinal)

    private fun isAnalogExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == MyStatPipe2AnalogOutMapping.EXTERNALLY_MAPPED.ordinal)

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
        analogOuts[DomainName.analog1Out] = Pair(isAnalogExternalMapped(analogOut1Enabled, analogOut1Association), analogType(analogOut1Enabled))
        return analogOuts
    }

    private fun analogType(analogOutPort: EnableConfig): String {
        return when (analogOutPort) {
            analogOut1Enabled -> getPortType(analogOut1Association, analogOut1MinMaxConfig)
            else -> "0-10v"
        }
    }

    private fun getPortType(association: AssociationConfig, minMaxConfig: MyStatPipe2MinMaxConfig): String {
        val portType: String
        when (association.associationVal) {

            MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal -> {
                portType = "${minMaxConfig.waterModulatingValue.min.currentVal.toInt()}-${minMaxConfig.waterModulatingValue.max.currentVal.toInt()}v"
            }

            MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal -> {
                portType = "${minMaxConfig.fanSpeedConfig.min.currentVal.toInt()}-${minMaxConfig.fanSpeedConfig.max.currentVal.toInt()}v"
            }

            MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal -> {
                portType = "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                portType = "0-10v"
            }
        }
        return portType
    }

    fun getLowestFanSelected(): MyStatPipe2RelayMapping? {
        val lowestSelected = getLowestStage(MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal,  MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)
        if (lowestSelected == -1) {
            return null
        }
        return MyStatPipe2RelayMapping.values()[lowestSelected]
    }

    private fun getHighestFanSelected(): MyStatPipe2RelayMapping {
        val highestSelected = getHighestStage(MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal, MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)
        return MyStatPipe2RelayMapping.values()[highestSelected]
    }

    fun getHighestFanStageCount() = getHighestFanSelected().ordinal + 1
}


enum class MyStatPipe2RelayMapping(val displayName: String) {
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    AUX_HEATING_STAGE1("Aux Heating"),
    WATER_VALVE("Water Valve"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper"),
}


enum class MyStatPipe2AnalogOutMapping(val displayName: String) {
    WATER_MODULATING_VALUE("Water modulating valve"),
    FAN_SPEED("Fan Speed"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER_MODULATION("DCV Damper")
}