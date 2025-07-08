package a75f.io.logic.bo.building.system.vav.config

data class ModulatingRtuAnalogOutMinMaxConfig(
    var fanSignalConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var compressorSpeedConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var outsideAirDamperConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var coolingSignalConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var heatingSignalConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var chilledWaterValveConfig: MinMaxConfig = MinMaxConfig(2, 10)
)

data class MinMaxConfig(
    var min: Int,
    var max: Int
)

data class ModulatingRtuAnalogOutMinMaxState(
    var fanSignalConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var compressorSpeedConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var dcvDamperModulatingConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var outsideAirDamperConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var coolingSignalConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var heatingSignalConfig: MinMaxConfig = MinMaxConfig(2, 10),
    var chilledWaterValveConfig: MinMaxConfig = MinMaxConfig(2, 10)
)