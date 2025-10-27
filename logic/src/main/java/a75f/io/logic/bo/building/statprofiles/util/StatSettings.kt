package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * Created by Manjunath K on 22-04-2025.
 */


/**
 * Basic settings a user in the space might set.  These might be included (moved to) UserIntents
 */
data class BasicSettings(
    val conditioningMode: StandaloneConditioningMode,
    var fanMode: StandaloneFanStage,
)

data class MyStatBasicSettings(
    val conditioningMode: StandaloneConditioningMode,
    var fanMode: MyStatFanStages,
)

/**
 * Traditional 75f User Intent values (settings of user in space)
 */
data class UserIntents(
    val currentTemp: Double,
    val coolingDesiredTemp: Double,   // (affected by scheduling of desired temperatures)
    val heatingDesiredTemp: Double,
    val targetMinHumidity: Double,         // Same as system/ 25% [Available in the UI only if Humidifier/Dehumidifier Option has been configured]
    val targetMaxHumidity: Double
)

data class ConfigState(
    val enabled: Boolean, val association: Int
)

data class MinMaxConfig(val min: ValueConfig, val max: ValueConfig)

data class FanConfig(val low: ValueConfig, val medium: ValueConfig, val high: ValueConfig)


data class MyStatPipe2MinMaxConfig(
    val waterModulatingValue: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)

data class MyStatHpuMinMaxConfig(
    val compressorSpeed: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)

data class MyStatCpuMinMaxConfig(
    val cooling: MinMaxConfig,
    val linearFanSpeed: MinMaxConfig,
    val heating: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)
data class MyStatPipe4MinMaxConfig(
    val hotWaterValve: MinMaxConfig,
    val chilledWaterValve: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)