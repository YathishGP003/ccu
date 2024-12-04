package a75f.io.renatus.profiles.oao

import a75f.io.logic.bo.building.oao.OAOProfileConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class OAOViewState {
    var outsideDamperMinDrivePos by mutableStateOf (0.0)
    var outsideDamperMaxDrivePos by mutableStateOf (0.0)
    var returnDamperMinDrivePos by mutableStateOf (0.0)
    var returnDamperMaxDrivePos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringRecirculationPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringConditioningPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringFanLowPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringFanMediumPos by mutableStateOf (0.0)
    var outsideDamperMinOpenDuringFanHighPos by mutableStateOf (0.0)
    var returnDamperMinOpenPos by mutableStateOf (0.0)
    var exhaustFanStage1ThresholdPos by mutableStateOf (0.0)
    var exhaustFanStage2ThresholdPos by mutableStateOf (0.0)
    var currentTransformerTypePos by mutableStateOf (0.0)
    var co2ThresholdVal by mutableStateOf (0.0)
    var exhaustFanHysteresisPos by mutableStateOf (0.0)
    var usePerRoomCO2SensingState by mutableStateOf (false)
    var systemPurgeOutsideDamperMinPos by mutableStateOf (0.0)
    var enhancedVentilationOutsideDamperMinOpenPos by mutableStateOf (0.0)


    companion object {
        fun fromOaoProfileConfig(config : OAOProfileConfiguration) : OAOViewState {
            return OAOViewState().apply {
                outsideDamperMinDrivePos = config.outsideDamperMinDrive.currentVal
                outsideDamperMaxDrivePos = config.outsideDamperMaxDrive.currentVal
                returnDamperMinDrivePos = config.returnDamperMinDrive.currentVal
                returnDamperMaxDrivePos = config.returnDamperMaxDrive.currentVal
                usePerRoomCO2SensingState = config.usePerRoomCO2Sensing.enabled
                outsideDamperMinOpenDuringRecirculationPos = config.outsideDamperMinOpenDuringRecirculation.currentVal
                outsideDamperMinOpenDuringConditioningPos = config.outsideDamperMinOpenDuringConditioning.currentVal
                outsideDamperMinOpenDuringFanLowPos = config.outsideDamperMinOpenDuringFanLow.currentVal
                outsideDamperMinOpenDuringFanMediumPos = config.outsideDamperMinOpenDuringFanMedium.currentVal
                outsideDamperMinOpenDuringFanHighPos = config.outsideDamperMinOpenDuringFanHigh.currentVal
                returnDamperMinOpenPos = config.returnDamperMinOpen.currentVal
                exhaustFanStage1ThresholdPos = config.exhaustFanStage1Threshold.currentVal
                exhaustFanStage2ThresholdPos = config.exhaustFanStage2Threshold.currentVal
                currentTransformerTypePos = config.currentTransformerType.currentVal
                co2ThresholdVal = config.co2Threshold.currentVal
                exhaustFanHysteresisPos = config.exhaustFanHysteresis.currentVal
                systemPurgeOutsideDamperMinPos = config.systemPurgeOutsideDamperMinPos.currentVal
                enhancedVentilationOutsideDamperMinOpenPos = config.enhancedVentilationOutsideDamperMinOpen.currentVal
            }
        }
    }

    fun updateConfigFromViewState(profileConfiguration: OAOProfileConfiguration) {
        profileConfiguration.outsideDamperMinDrive.currentVal = this.outsideDamperMinDrivePos
        profileConfiguration.outsideDamperMaxDrive.currentVal = this.outsideDamperMaxDrivePos
        profileConfiguration.returnDamperMinDrive.currentVal = this.returnDamperMinDrivePos
        profileConfiguration.returnDamperMaxDrive.currentVal = this.returnDamperMaxDrivePos
        profileConfiguration.usePerRoomCO2Sensing.enabled = this.usePerRoomCO2SensingState
        profileConfiguration.outsideDamperMinOpenDuringRecirculation.currentVal = this.outsideDamperMinOpenDuringRecirculationPos
        profileConfiguration.outsideDamperMinOpenDuringConditioning.currentVal = this.outsideDamperMinOpenDuringConditioningPos
        profileConfiguration.outsideDamperMinOpenDuringFanLow.currentVal = this.outsideDamperMinOpenDuringFanLowPos
        profileConfiguration.outsideDamperMinOpenDuringFanMedium.currentVal = this.outsideDamperMinOpenDuringFanMediumPos
        profileConfiguration.outsideDamperMinOpenDuringFanHigh.currentVal = this.outsideDamperMinOpenDuringFanHighPos
        profileConfiguration.returnDamperMinOpen.currentVal = this.returnDamperMinOpenPos
        profileConfiguration.exhaustFanStage1Threshold.currentVal = this.exhaustFanStage1ThresholdPos
        profileConfiguration.exhaustFanStage2Threshold.currentVal = this.exhaustFanStage2ThresholdPos
        profileConfiguration.currentTransformerType.currentVal = this.currentTransformerTypePos
        profileConfiguration.co2Threshold.currentVal = this.co2ThresholdVal
        profileConfiguration.exhaustFanHysteresis.currentVal = this.exhaustFanHysteresisPos
        profileConfiguration.systemPurgeOutsideDamperMinPos.currentVal = this.systemPurgeOutsideDamperMinPos
        profileConfiguration.enhancedVentilationOutsideDamperMinOpen.currentVal = this.enhancedVentilationOutsideDamperMinOpenPos
    }

    override fun toString(): String {
        return "outsideDamperMinDrive: $outsideDamperMinDrivePos,"+
                "outsideDamperMaxDrive: $outsideDamperMaxDrivePos,"+
                "returnDamperMinDrive: $returnDamperMinDrivePos,"+
                "returnDamperMaxDrive: $returnDamperMaxDrivePos,"+
                "outsideDamperMinOpenDuringRecirculation: $outsideDamperMinOpenDuringRecirculationPos,"+
                "outsideDamperMinOpenDuringConditioning: $outsideDamperMinOpenDuringConditioningPos,"+
                "outsideDamperMinOpenDuringFanLow: $outsideDamperMinOpenDuringFanLowPos,"+
                "outsideDamperMinOpenDuringFanMedium: $outsideDamperMinOpenDuringFanMediumPos,"+
                "outsideDamperMinOpenDuringFanHigh: $outsideDamperMinOpenDuringFanHighPos,"+
                "returnDamperMinOpen: $returnDamperMinOpenPos,"+
                "exhaustFanStage1Threshold: $exhaustFanStage1ThresholdPos,"+
                "exhaustFanStage2Threshold: $exhaustFanStage2ThresholdPos,"+
                "currentTransformerType: $currentTransformerTypePos,"+
                "co2Threshold: $co2ThresholdVal,"+
                "exhaustFanHysteresis: $exhaustFanHysteresisPos,"+
                "usePerRoomCO2Sensing: $usePerRoomCO2SensingState,"+
                "systemPurgeOutsideDamperMinPos: $systemPurgeOutsideDamperMinPos,"+
                "enhancedVentilationOutsideDamperMinOpen: $enhancedVentilationOutsideDamperMinOpenPos,"
    }
}