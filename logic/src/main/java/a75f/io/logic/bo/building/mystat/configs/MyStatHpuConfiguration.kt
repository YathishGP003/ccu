package a75f.io.logic.bo.building.mystat.configs

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.MinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatHpuConfiguration(nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective): MyStatConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType, model = model) {

    lateinit var analogOut1MinMaxConfig: MyStatHpuMinMaxConfig
    lateinit var analogOut1FanSpeedConfig: MyStatFanConfig

    override fun getActiveConfiguration(): MyStatConfiguration {
        TODO("Not yet implemented")
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }


    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        analogOuts[DomainName.analog1Out] = Pair(isAnalogExternalMapped(analogOut1Enabled, analogOut1Association), analogType(analogOut1Enabled))
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

    private fun getPortType(association: AssociationConfig, minMaxConfig: MyStatHpuMinMaxConfig): String {
        val portType: String
        when (association.associationVal) {
            MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal -> {
                portType = "${minMaxConfig.compressorSpeed.min.currentVal.toInt()}-${minMaxConfig.compressorSpeed.max.currentVal.toInt()}v"
            }

            MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal -> {
                portType = "${minMaxConfig.fanSpeedConfig.min.currentVal.toInt()}-${minMaxConfig.fanSpeedConfig.max.currentVal.toInt()}v"
            }

            MyStatHpuAnalogOutMapping.DCV_DAMPER.ordinal -> {
                portType = "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                portType = "0-10v"
            }
        }
        return portType
    }

    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == MyStatHpuRelayMapping.EXTERNALLY_MAPPED.ordinal)

    private fun isAnalogExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == MyStatHpuAnalogOutMapping.EXTERNALLY_MAPPED.ordinal)

}


data class MyStatHpuMinMaxConfig (
    val compressorSpeed: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)


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
    EXTERNALLY_MAPPED("Externally Mapped")
}

enum class MyStatHpuAnalogOutMapping(val displayName: String) {
    COMPRESSOR_SPEED("Compressor Speed"),
    FAN_SPEED("Fan Speed"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper") // will use later
}