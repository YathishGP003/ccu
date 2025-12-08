package a75f.io.renatus.profiles.hyperstat.util

/**
 * Created by Manjunath K on 25-07-2022.
 */

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.getActualConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSPipe2FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSPipe4FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSSelectedFanMode
import a75f.io.logic.bo.building.statprofiles.util.getHpuFanLevel
import a75f.io.logic.util.uiutils.updateUserIntentPoints


fun handleConditionMode(
        selectedPosition: Int, equipId: String, profileType: ProfileType,
        userClickCheck: Boolean, equip: HyperStatEquip,
        configuration: HyperStatConfiguration
) {
    if (userClickCheck) {
        var actualConditioningMode = -1

        // CPU Profile has combination of conditioning modes
        if (profileType == ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT || profileType == ProfileType.HYPERSTAT_FOUR_PIPE_FCU) {
            actualConditioningMode = getActualConditioningMode(configuration, selectedPosition)
        } else if (profileType == ProfileType.HYPERSTAT_TWO_PIPE_FCU || profileType == ProfileType.HYPERSTAT_HEAT_PUMP_UNIT) {
            // 2 Pipe & HPU profile will be always has all conditioning modes
            actualConditioningMode = StandaloneConditioningMode.values()[selectedPosition].ordinal
        }
        if (actualConditioningMode != -1) {
            updateUserIntentPoints(
                    equipId, equip.conditioningMode, actualConditioningMode.toDouble(),
                    CCUHsApi.getInstance().ccuUserName
            )

        }
    }
}

// Save the fan mode in cache
fun handleFanMode(
    equipId: String,
    selectedPosition: Int,
    profileType: ProfileType,
    userClickCheck: Boolean,
    equip: HyperStatEquip,
    configuration: HyperStatConfiguration
) {
    val cacheStorage = FanModeCacheStorage.getHyperStatFanModeCache()
    fun isFanModeCurrentOccupied(basicSettings: StandaloneFanStage): Boolean {
        return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
    }

    fun updateFanModeCache(actualFanMode: Int) {

        if (selectedPosition != 0 && (selectedPosition % 3 == 0 || isFanModeCurrentOccupied(StandaloneFanStage.values()[actualFanMode])) )
            cacheStorage.saveFanModeInCache(equipId, actualFanMode) // while saving the fan mode, we need to save the actual fan mode instead of selected position
        else
            cacheStorage.removeFanModeFromCache(equipId)
    }

    if (userClickCheck) {
        val actualFanMode: Int = when (profileType) {
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                // TODO Revisit after migrating all profile to clean code
                // Need to do same for all profile once all are migrated to DM
                val fanMode = getHSSelectedFanMode(getCpuFanLevel(configuration as CpuConfiguration), selectedPosition)
                equip.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }

            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
                val fanMode = getHSSelectedFanMode(getHpuFanLevel(configuration as HpuConfiguration), selectedPosition)
                equip.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }

            ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
                val fanMode = getHSSelectedFanMode(getHSPipe2FanLevel(configuration as Pipe2Configuration), selectedPosition)
                equip.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }
            ProfileType.HYPERSTAT_FOUR_PIPE_FCU -> {
                val fanMode = getHSSelectedFanMode(getHSPipe4FanLevel(configuration as HsPipe4Configuration), selectedPosition)
                equip.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }

            else -> {
                -1
            }
        }
        if (actualFanMode != -1) {
            updateUserIntentPoints(equipId, equip.fanOpMode, actualFanMode.toDouble(),
                    CCUHsApi.getInstance().ccuUserName)
            if (selectedPosition != 0 && selectedPosition % 3 == 0)
                cacheStorage.saveFanModeInCache(equipId, actualFanMode) // while saving the fan mode, we need to save the actual fan mode instead of selected position
            else
                cacheStorage.removeFanModeFromCache(equipId)
        }
    }
}

fun handleHumidityMode(selectedPosition: Int, equip: HyperStatEquip) {
    equip.targetHumidifier.writePointValue((selectedPosition + 1).toDouble())
}

fun handleDeHumidityMode(selectedPosition: Int, equip: HyperStatEquip) {
    equip.targetDehumidifier.writePointValue((selectedPosition + 1).toDouble())
}





