package a75f.io.logic.bo.building.mystat.configs

import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.MinMaxConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatCpuConfiguration(nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective): MyStatConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType, model = model) {

    lateinit var analogOut1MinMaxConfig: MyStatCpuMinMaxConfig
    lateinit var analogOut1FanSpeedConfig: MyStatFanConfig

    override fun getActiveConfiguration(): MyStatConfiguration {
        TODO("Not yet implemented")
    }

    override fun getRelayMap(): Map<String, Boolean> {
        TODO("Not yet implemented")
    }

    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        TODO("Not yet implemented")
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    fun isCoolingAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    fun isHeatingAvailable(): Boolean {
        TODO("Not yet implemented")
    }
}

data class MyStatCpuMinMaxConfig (
    val cooling: MinMaxConfig,
    val heating: MinMaxConfig,
    val linearFanSpeed: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)

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
}

enum class MyStatCpuAnalogOutMapping(val displayName: String) {
    COOLING("Cooling"),
    HEATING("Heating"),
    LINEAR_FAN_SPEED("Linear Fan speed"),
    STAGED_FAN_SPEED("Staged Fan"),
    EXTERNALLY_MAPPED("Externally Mapped"),
    DCV_DAMPER("Dcv Damper") // will use later
}