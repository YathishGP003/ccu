package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.logic.L
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 01-04-2024.
 */

open class AdvancedHybridAhuState {

    var isConnectEnabled by mutableStateOf(false)
    var pendingDeleteConnect by mutableStateOf(false)
    var connectAddress by mutableStateOf(L.ccu().addressBand + 98)

    var sensorAddress0 by mutableStateOf(SensorState(false))
    var sensorAddress1 by mutableStateOf(SensorState(false))
    var sensorAddress2 by mutableStateOf(SensorState(false))
    var sensorAddress3 by mutableStateOf(SensorState(false))
    var sensorBusPressureEnable by mutableStateOf(false)

    var analogIn1Config by mutableStateOf(ConfigState(false, 0))
    var analogIn2Config by mutableStateOf(ConfigState(false, 0))

    var thermistor1Config by mutableStateOf(ConfigState(false, 0))
    var thermistor2Config by mutableStateOf(ConfigState(false, 0))

    var relay1Config by mutableStateOf(ConfigState(false, 0))
    var relay2Config by mutableStateOf(ConfigState(false, 0))
    var relay3Config by mutableStateOf(ConfigState(false, 0))
    var relay4Config by mutableStateOf(ConfigState(false, 0))
    var relay5Config by mutableStateOf(ConfigState(false, 0))
    var relay6Config by mutableStateOf(ConfigState(false, 0))
    var relay7Config by mutableStateOf(ConfigState(false, 0))
    var relay8Config by mutableStateOf(ConfigState(false, 0))

   var isStateChanged by mutableStateOf(false)
   var isSaveRequired by mutableStateOf(false)
    var noOfAnalogOutDynamic = 0

    var analogOut1Enabled by mutableStateOf(false)
    var analogOut2Enabled by mutableStateOf(false)
    var analogOut3Enabled by mutableStateOf(false)
    var analogOut4Enabled by mutableStateOf(false)

    var analogOut1Association by mutableStateOf(0)
    var analogOut2Association by mutableStateOf(0)
    var analogOut3Association by mutableStateOf(0)
    var analogOut4Association by mutableStateOf(0)

    var pressureConfig by mutableStateOf(PressureConfig(0, 0.0, 0.0))
    var satConfig by mutableStateOf(SatConfig(0, 0.0, 0.0, 0.0, 0.0))
    var damperConfig by mutableStateOf(DamperConfig(0, 0.0, 0.0, 0.0))

    var analogOut1MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10,
            0, 10, 0, 10,
            0, 10, 0, 10,
            0, 10,0,10,0,10

        )
    )

    var analogOut2MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10,
            0, 10, 0, 10, 0,
            10, 0, 10, 0,
            10,0,10,0,10
        )
    )

    var analogOut3MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10,
            0, 10, 0, 10,
            0, 10, 0, 10,
            0, 10,0,10,0,10
        )
    )

    var analogOut4MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10,
            0, 10, 0, 10, 0,
            10, 0, 10, 0,
            10,0,10,0,10
        )
    )

    var connectSensorAddress0 by mutableStateOf(SensorState(false))
    var connectSensorAddress1 by mutableStateOf(SensorState(false))
    var connectSensorAddress2 by mutableStateOf(SensorState(false))
    var connectSensorAddress3 by mutableStateOf(SensorState(false))
    var connectSensorBusPressureEnable by mutableStateOf(false)

    var connectUniversalIn1Config by mutableStateOf(ConfigState(false, 0))
    var connectUniversalIn2Config by mutableStateOf(ConfigState(false, 0))
    var connectUniversalIn3Config by mutableStateOf(ConfigState(false, 0))
    var connectUniversalIn4Config by mutableStateOf(ConfigState(false, 0))
    var connectUniversalIn5Config by mutableStateOf(ConfigState(false, 0))
    var connectUniversalIn6Config by mutableStateOf(ConfigState(false, 0))
    var connectUniversalIn7Config by mutableStateOf(ConfigState(false, 0))
    var connectUniversalIn8Config by mutableStateOf(ConfigState(false, 0))

    var connectRelay1Config by mutableStateOf(ConfigState(false, 0))
    var connectRelay2Config by mutableStateOf(ConfigState(false, 0))
    var connectRelay3Config by mutableStateOf(ConfigState(false, 0))
    var connectRelay4Config by mutableStateOf(ConfigState(false, 0))
    var connectRelay5Config by mutableStateOf(ConfigState(false, 0))
    var connectRelay6Config by mutableStateOf(ConfigState(false, 0))
    var connectRelay7Config by mutableStateOf(ConfigState(false, 0))
    var connectRelay8Config by mutableStateOf(ConfigState(false, 0))

    var connectAnalogOut1Enabled by mutableStateOf(false)
    var connectAnalogOut2Enabled by mutableStateOf(false)
    var connectAnalogOut3Enabled by mutableStateOf(false)
    var connectAnalogOut4Enabled by mutableStateOf(false)

    var connectAnalogOut1Association by mutableStateOf(0)
    var connectAnalogOut2Association by mutableStateOf(0)
    var connectAnalogOut3Association by mutableStateOf(0)
    var connectAnalogOut4Association by mutableStateOf(0)

    var connectDamperConfig by mutableStateOf(DamperConfig(0, 0.0, 0.0, 0.0))
    var connectAnalogOut1MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10,0,10,0,10
        )
    )

    var connectAnalogOut2MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10,0,10,0,10
        )
    )

    var connectAnalogOut3MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10,0,10,0,10
        )
    )

    var connectAnalogOut4MinMax by mutableStateOf(
        MinMaxVoltage(
            0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10, 0, 10,0,10,0,10
        )
    )
    //OAO state
    var analog1MinOaoDamper by mutableStateOf(0.0)
    var analog1MaxOaoDamper by mutableStateOf(0.0)
    var analog2MinOaoDamper by mutableStateOf(0.0)
    var analog2MaxOaoDamper by mutableStateOf(0.0)
    var analog3MinOaoDamper by mutableStateOf(0.0)
    var analog3MaxOaoDamper by mutableStateOf(0.0)
    var analog4MinOaoDamper by mutableStateOf(0.0)
    var analog4MaxOaoDamper by mutableStateOf(0.0)

    var analog1MinReturnDamper by mutableStateOf(0.0)
    var analog1MaxReturnDamper by mutableStateOf(0.0)
    var analog2MinReturnDamper by mutableStateOf(0.0)
    var analog2MaxReturnDamper by mutableStateOf(0.0)
    var analog3MinReturnDamper by mutableStateOf(0.0)
    var analog3MaxReturnDamper by mutableStateOf(0.0)
    var analog4MinReturnDamper by mutableStateOf(0.0)
    var analog4MaxReturnDamper by mutableStateOf(0.0)


    var outsideDamperMinOpenDuringRecirculationPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringConditioningPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringFanLowPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringFanMediumPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringFanHighPos by mutableStateOf (0.0)
    var returnDamperMinOpenPos by mutableStateOf (0.0)
    var exhaustFanStage1ThresholdPos by mutableStateOf (0.0)
    var exhaustFanStage2ThresholdPos by mutableStateOf (0.0)
    var currentTransformerTypePos by mutableStateOf (0.0)
    var oaoCo2ThresholdVal by mutableStateOf (0.0)
    var exhaustFanHysteresisPos by mutableStateOf (0.0)
    var usePerRoomCO2SensingState by mutableStateOf (false)
    var systemPurgeOutsideDamperMinPos by mutableStateOf (0.0)
    var enhancedVentilationOutsideDamperMinOpenPos by mutableStateOf (0.0)

    var enableOutsideAirOptimization by mutableStateOf (false)



}

class SensorState(enabled: Boolean) {
    var enabled by mutableStateOf(enabled)
    var temperatureAssociation by mutableStateOf(0)
    var humidityAssociation by mutableStateOf(0)
    var occupancyAssociation by mutableStateOf(0)
    var co2Association by mutableStateOf(0)
    var pressureAssociation by mutableStateOf(0)
}

class ConfigState(enabled: Boolean,association: Int) {
    var enabled by mutableStateOf(enabled)
    var association by mutableStateOf(association)
}

data class MinMaxVoltage(
    var staticPressureMinVoltage: Int, var staticPressureMaxVoltage: Int,
    var satCoolingMinVoltage: Int, var satCoolingMaxVoltage: Int,
    var satHeatingMinVoltage: Int, var satHeatingMaxVoltage: Int,
    var heatingMinVoltage: Int, var heatingMaxVoltage: Int,
    var coolingMinVoltage: Int, var coolingMaxVoltage: Int,
    var compositeCoolingMinVoltage: Int, var compositeCoolingMaxVoltage: Int,
    var compositeHeatingMinVoltage: Int, var compositeHeatingMaxVoltage: Int,
    var fanMinVoltage: Int, var fanMaxVoltage: Int,
    var damperPosMinVoltage: Int, var damperPosMaxVoltage: Int,
)

data class PressureConfig(
    var pressureControlAssociation: Int, var staticMinPressure: Double, var staticMaxPressure: Double,
)

data class SatConfig(
    var satControlAssociation: Int,
    var systemSatCoolingMin: Double, var systemSatCoolingMax: Double,
    var systemSatHeatingMin: Double, var systemSatHeatingMax: Double,
)

data class DamperConfig(
    var damperControlAssociation: Int,
    var co2Threshold: Double,
    var co2Target: Double,
    var openingRate: Double
)




