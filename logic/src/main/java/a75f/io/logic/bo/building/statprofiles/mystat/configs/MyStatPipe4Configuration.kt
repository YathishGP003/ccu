package a75f.io.logic.bo.building.statprofiles.mystat.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.mystat.MyStatPipe4Equip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.util.MinMaxConfig
import a75f.io.logic.bo.building.statprofiles.util.MyStatPipe4MinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import java.util.Collections

class MyStatPipe4Configuration(nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective) : MyStatConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType, model = model) {

    lateinit var analogOut1MinMaxConfig: MyStatPipe4MinMaxConfig
    lateinit var analogOut1FanSpeedConfig: MyStatFanConfig
    lateinit var analogOut2MinMaxConfig: MyStatPipe4MinMaxConfig
    lateinit var analogOut2FanSpeedConfig: MyStatFanConfig

    override fun getActiveConfiguration() : MyStatConfiguration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val msEquip = MyStatPipe4Equip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        getActiveEnableConfigs(msEquip)
        getActiveAssociationConfigs(msEquip)
        getGenericZoneConfigs(msEquip)
        getActiveDynamicConfig(msEquip)
        equipId = msEquip.equipRef
        isDefault = false
        return this
    }

    override fun getDefaultConfiguration(): MyStatConfiguration {
        val configuration = super.getDefaultConfiguration()
        configuration.apply {
            analogOut1MinMaxConfig = MyStatPipe4MinMaxConfig(
               MinMaxConfig(getDefaultValConfig(DomainName.analog1MinHotWaterValve, model), getDefaultValConfig(
                   DomainName.analog1MaxHotWaterValve, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinChilledWaterValve, model), getDefaultValConfig(
                    DomainName.analog1MaxChilledWaterValve, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinFanSpeed, model), getDefaultValConfig(
                    DomainName.analog1MaxFanSpeed, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog1MinDCVDamper, model), getDefaultValConfig(
                    DomainName.analog1MaxDCVDamper, model))
            )
            analogOut1FanSpeedConfig = MyStatFanConfig(
                getDefaultValConfig(DomainName.analog1FanLow, model),
                getDefaultValConfig(DomainName.analog1FanHigh, model)
            )
            analogOut2MinMaxConfig = MyStatPipe4MinMaxConfig(
                MinMaxConfig(getDefaultValConfig(DomainName.analog2MinHotWaterValve, model), getDefaultValConfig(
                    DomainName.analog2MaxHotWaterValve, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog2MinChilledWaterValve, model), getDefaultValConfig(
                    DomainName.analog2MaxChilledWaterValve, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog2MinFanSpeed, model), getDefaultValConfig(
                    DomainName.analog2MaxFanSpeed, model)),
                MinMaxConfig(getDefaultValConfig(DomainName.analog2MinDCVDamper, model), getDefaultValConfig(
                    DomainName.analog2MaxDCVDamper, model))
            )
            analogOut2FanSpeedConfig = MyStatFanConfig(
                getDefaultValConfig(DomainName.analog2FanLow, model),
                getDefaultValConfig(DomainName.analog2FanHigh, model)
            )
        }
        return configuration
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            addAll(super.getValueConfigs())

            analogOut1MinMaxConfig.apply {
                add(hotWaterValve.min)
                add(hotWaterValve.max)
                add(chilledWaterValve.min)
                add(chilledWaterValve.max)
                add(fanSpeedConfig.min)
                add(fanSpeedConfig.max)
                add(dcvDamperConfig.min)
                add(dcvDamperConfig.max)
            }

            analogOut2MinMaxConfig.apply {
                add(hotWaterValve.min)
                add(hotWaterValve.max)
                add(chilledWaterValve.min)
                add(chilledWaterValve.max)
                add(fanSpeedConfig.min)
                add(fanSpeedConfig.max)
                add(dcvDamperConfig.min)
                add(dcvDamperConfig.max)

            }
            analogOut1FanSpeedConfig.apply {
                add(analogOut1FanSpeedConfig.low)
                add(analogOut1FanSpeedConfig.high)
            }
            analogOut2FanSpeedConfig.apply {
                add(analogOut2FanSpeedConfig.low)
                add(analogOut2FanSpeedConfig.high)
            }
        }
    }

    override fun getAnalogStartIndex(): Int = MyStatPipe4RelayMapping.values().size

    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) =
        (enabled.enabled && association.associationVal == MyStatPipe4RelayMapping.EXTERNALLY_MAPPED.ordinal)

    override fun getRelayMap(): Map<String, Boolean> {
        val relayStatus = mutableMapOf<String, Boolean>()
        relayStatus[DomainName.relay1] = isRelayExternalMapped(relay1Enabled, relay1Association)
        relayStatus[DomainName.relay2] = isRelayExternalMapped(relay1Enabled, relay2Association)
        relayStatus[DomainName.relay3] = isRelayExternalMapped(relay3Enabled, relay3Association)
        if (isRelayConfig(universalOut1Association.associationVal)) {
            relayStatus[DomainName.universal1Out] =
                isRelayExternalMapped(universalOut1, universalOut1Association)
        }
        if (isRelayConfig(universalOut2Association.associationVal)) {
            relayStatus[DomainName.universal2Out] =
                isRelayExternalMapped(universalOut2, universalOut2Association)
        }
        return relayStatus
    }

    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        if (isRelayConfig(universalOut1Association.associationVal).not()) {
            analogOuts[DomainName.universal1Out] = Pair(
                isUniversalExternalMapped(universalOut1, universalOut1Association), analogType(universalOut1)
            )
        }
        if (isRelayConfig(universalOut2Association.associationVal).not()) {
            analogOuts[DomainName.universal2Out] = Pair(
                isUniversalExternalMapped(universalOut2, universalOut2Association), analogType(universalOut2)
            )
        }
        return analogOuts
    }

    private fun analogType(analogOutPort: EnableConfig): String {
        return when (analogOutPort) {
            universalOut1 -> getPortType(universalOut1Association, analogOut1MinMaxConfig)
            universalOut2 -> getPortType(universalOut2Association, analogOut2MinMaxConfig)
            else -> "0-10v"
        }
    }

    fun getLowestFanSelected(lowVentilationAvailable: Boolean): MyStatPipe4RelayMapping? {
        if (lowVentilationAvailable) {
            val lowestSelected = getLowestStage(MyStatPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal,  MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal)
            if (lowestSelected != -1) {
                return MyStatPipe4RelayMapping.values()[lowestSelected]
            }
        } else {
            val lowestSelected = getLowestStage(MyStatPipe4RelayMapping.FAN_LOW_SPEED.ordinal,  MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal)
            if (lowestSelected != -1) {
                return MyStatPipe4RelayMapping.values()[lowestSelected]
            }
        }
        return null
    }
    
    private fun getPortType(association: AssociationConfig, minMaxConfig: MyStatPipe4MinMaxConfig): String {
        val portType: String
        when (association.associationVal) {

            MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.ordinal -> {
                portType = "${minMaxConfig.chilledWaterValve.min.currentVal.toInt()}-${minMaxConfig.chilledWaterValve.max.currentVal.toInt()}v"
            }
            MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE.ordinal -> {
                portType = "${minMaxConfig.hotWaterValve.min.currentVal.toInt()}-${minMaxConfig.hotWaterValve.max.currentVal.toInt()}v"
            }

            MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal -> {
                portType = "${minMaxConfig.fanSpeedConfig.min.currentVal.toInt()}-${minMaxConfig.fanSpeedConfig.max.currentVal.toInt()}v"
            }

            MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal -> {
                portType = "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                portType = "0-10v"
            }
        }
        return portType
    }

    private fun isUniversalExternalMapped(
        enabled: EnableConfig, association: AssociationConfig
    ): Boolean {
        return (enabled.enabled && association.associationVal == MyStatPipe4AnalogOutMapping.ANALOG_EXTERNALLY_MAPPED.ordinal)
    }

    override fun getHighestFanStageCount(): Int {
        if (isAnyRelayEnabledAssociated(association = MyStatPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal)) {
            val highestSelected = getHighestStage(
                MyStatPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal,
                MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal
            )
            return when(highestSelected) {
                MyStatPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal -> 1
                MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal -> 2
                else -> 0
            }
        } else {
            val highestSelected = getHighestStage(
                MyStatPipe4RelayMapping.FAN_LOW_SPEED.ordinal,
                MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal
            )
            return when(highestSelected) {
                MyStatPipe4RelayMapping.FAN_LOW_SPEED.ordinal -> 1
                MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal -> 2
                else -> 0
            }
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            Collections.addAll(getAnalogOutMinMax(analogOut1MinMaxConfig))
            Collections.addAll(getAnalogOutMinMax(analogOut2MinMaxConfig))
            add(analogOut1FanSpeedConfig.low)
            add(analogOut1FanSpeedConfig.high)
            add(analogOut2FanSpeedConfig.low)
            add(analogOut2FanSpeedConfig.high)
        }
    }

    private fun getAnalogOutMinMax(analogOutMinMaxConfig: MyStatPipe4MinMaxConfig): MutableList<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(analogOutMinMaxConfig.hotWaterValve.min)
            add(analogOutMinMaxConfig.hotWaterValve.max)
            add(analogOutMinMaxConfig.chilledWaterValve.min)
            add(analogOutMinMaxConfig.chilledWaterValve.max)
            add(analogOutMinMaxConfig.fanSpeedConfig.min)
            add(analogOutMinMaxConfig.fanSpeedConfig.max)
            add(analogOutMinMaxConfig.dcvDamperConfig.min)
            add(analogOutMinMaxConfig.dcvDamperConfig.max)
        }

    }

    private fun getActiveDynamicConfig(equip: MyStatPipe4Equip) {

        analogOut1MinMaxConfig.apply {
            hotWaterValve.min.currentVal = getActivePointValue(
                equip.analog1MinHotWaterValve,
                hotWaterValve.min
            )
            hotWaterValve.max.currentVal = getActivePointValue(
                equip.analog1MaxHotWaterValve,
                hotWaterValve.max
            )

            chilledWaterValve.min.currentVal = getActivePointValue(
                equip.analog1MinChilledWaterValve,
                chilledWaterValve.min
            )
            chilledWaterValve.max.currentVal = getActivePointValue(
                equip.analog1MaxChilledWaterValve,
                chilledWaterValve.max
            )

            fanSpeedConfig.min.currentVal = getActivePointValue(
                equip.analog1MinFanSpeed,
                fanSpeedConfig.min
            )
            fanSpeedConfig.max.currentVal = getActivePointValue(
                equip.analog1MaxFanSpeed,
                fanSpeedConfig.max
            )

            dcvDamperConfig.min.currentVal = getActivePointValue(
                equip.analog1MinDCVDamper,
                dcvDamperConfig.min
            )
            dcvDamperConfig.max.currentVal = getActivePointValue(
                equip.analog1MaxDCVDamper,
                dcvDamperConfig.max
            )
        }

        analogOut2MinMaxConfig.apply {
            hotWaterValve.min.currentVal = getActivePointValue(
                equip.analog2MinHotWaterValve,
                hotWaterValve.min
            )
            hotWaterValve.max.currentVal = getActivePointValue(
                equip.analog2MaxHotWaterValve,
                hotWaterValve.max
            )

            chilledWaterValve.min.currentVal = getActivePointValue(
                equip.analog2MinChilledWaterValve,
                chilledWaterValve.min
            )
            chilledWaterValve.max.currentVal = getActivePointValue(
                equip.analog2MaxChilledWaterValve,
                chilledWaterValve.max
            )

            fanSpeedConfig.min.currentVal = getActivePointValue(
                equip.analog2MinFanSpeed,
                fanSpeedConfig.min
            )
            fanSpeedConfig.max.currentVal = getActivePointValue(
                equip.analog2MaxFanSpeed,
                fanSpeedConfig.max
            )

            dcvDamperConfig.min.currentVal = getActivePointValue(
                equip.analog2MinDCVDamper,
                dcvDamperConfig.min
            )
            dcvDamperConfig.max.currentVal = getActivePointValue(
                equip.analog2MaxDCVDamper,
                dcvDamperConfig.max
            )
        }

        analogOut1FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog1FanLow, low)
            high.currentVal = getActivePointValue(equip.analog1FanHigh, high)
        }

        analogOut2FanSpeedConfig.apply {
            low.currentVal = getActivePointValue(equip.analog2FanLow, low)
            high.currentVal = getActivePointValue(equip.analog2FanHigh, high)
        }
    }

    override fun isCoolingAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return (isAnyRelayEnabledAssociated(relays, MyStatPipe4RelayMapping.CHILLED_WATER_VALVE.ordinal)
                || isAnalogEnabledAssociated(association = MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.ordinal))
    }

    override fun isHeatingAvailable(): Boolean {
        val relays = getRelayEnabledAssociations()
        return ((isAnyRelayEnabledAssociated(relays, MyStatPipe4RelayMapping.HOT_WATER_VALVE.ordinal) ||
                isAnyRelayEnabledAssociated(relays, MyStatPipe4RelayMapping.AUX_HEATING_STAGE1.ordinal))
                || isAnalogEnabledAssociated(association = MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE.ordinal))
    }

}


enum class MyStatPipe4RelayMapping(val displayName: String) {
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    AUX_HEATING_STAGE1("Aux Heating"),
    CHILLED_WATER_VALVE("Chilled Water Valve"),
    HOT_WATER_VALVE("Hot Water Valve"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper"),
    FAN_LOW_VENTILATION("Fan Low Ventilation")
}


enum class MyStatPipe4AnalogOutMapping(val displayName: String) {
    FAN_LOW_SPEED("Fan Low Speed"),
    FAN_HIGH_SPEED("Fan High Speed"),
    AUX_HEATING_STAGE1("Aux Heating"),
    CHILLED_WATER_VALVE("Chilled Water Valve"),
    HOT_WATER_VALVE("Hot Water Valve"),
    FAN_ENABLED("Fan Enabled"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("Dehumidifier"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper"),
    FAN_LOW_VENTILATION("Fan Low Ventilation"),
    CHILLED_MODULATING_VALUE("Chilled modulating valve"),
    HOT_MODULATING_VALUE("Hot Modulating Valve"),
    FAN_SPEED("Fan Speed"),
    ANALOG_EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER_MODULATION("DCV Damper")
}
