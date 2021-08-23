package a75f.io.logic.bo.building.vrv

import a75f.io.logic.bo.building.BaseProfileConfiguration

data class VrvProfileConfiguration(
    val temperatureOffset : Double,
    val minHumiditySp : Double,
    val maxHumiditySp : Double,
    val masterControllerMode : Double
) : BaseProfileConfiguration()