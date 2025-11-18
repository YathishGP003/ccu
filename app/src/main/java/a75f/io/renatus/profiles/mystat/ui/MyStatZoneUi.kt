package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.getMyStatActualConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe2FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe4FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatSelectedFanMode
import a75f.io.logic.util.uiutils.updateUserIntentPoints


fun handleMyStatConditionMode(
    selectedPosition: Int,
    equipId: String,
    profileType: ProfileType,
    userClickCheck: Boolean,
    configuration: MyStatConfiguration,
    conditioningMode: Point
) {
    if (userClickCheck) {
        var actualConditioningMode = -1

        // [CPU && Pipe 4] Profile has combination of conditioning modes
        if (profileType == ProfileType.MYSTAT_CPU || profileType == ProfileType.MYSTAT_PIPE4) {
            actualConditioningMode =
                getMyStatActualConditioningMode(configuration, selectedPosition)
        } else if (profileType == ProfileType.MYSTAT_PIPE2 || profileType == ProfileType.MYSTAT_HPU) {
            // 2 Pipe & HPU profile will be always has all conditioning modes
            actualConditioningMode = StandaloneConditioningMode.values()[selectedPosition].ordinal
        }
        if (actualConditioningMode != -1) {
            updateUserIntentPoints(
                equipId, conditioningMode,
                actualConditioningMode.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
        }
    }
}

// Save the fan mode in cache
fun handleMyStatFanMode(
    equipId: String,
    selectedPosition: Int,
    profileType: ProfileType,
    userClickCheck: Boolean,
    configuration: MyStatConfiguration,
    fanMode: Point
) {

    val cacheStorage = FanModeCacheStorage.getMyStatFanModeCache()
    fun isFanModeCurrentOccupied(position : Int): Boolean {
        val basicSettings = MyStatFanStages.values()[position]
        return (basicSettings == MyStatFanStages.LOW_CUR_OCC || basicSettings == MyStatFanStages.HIGH_CUR_OCC)
    }

    fun updateFanModeCache(actualFanMode: Int) {

        if (selectedPosition != 0 && ( selectedPosition % 3 == 0 || isFanModeCurrentOccupied(selectedPosition))) cacheStorage.saveFanModeInCache(
            equipId, actualFanMode
        ) // while saving the fan mode, we need to save the actual fan mode instead of selected position
        else cacheStorage.removeFanModeFromCache(equipId)
    }
    if (userClickCheck) {
        val actualFanMode: Int = when (profileType) {
            ProfileType.MYSTAT_CPU -> {
                val selectedFanMode = getMyStatSelectedFanMode(
                    getMyStatCpuFanLevel(configuration as MyStatCpuConfiguration), selectedPosition
                )
                updateFanModeCache(selectedFanMode)
                selectedFanMode
            }

            ProfileType.MYSTAT_HPU -> {
                val selectedFanMode = getMyStatSelectedFanMode(
                    getMyStatHpuFanLevel(configuration as MyStatHpuConfiguration), selectedPosition
                )
                updateFanModeCache(selectedFanMode)
                selectedFanMode
            }

            ProfileType.MYSTAT_PIPE2 -> {
                val selectedFanMode = getMyStatSelectedFanMode(
                    getMyStatPipe2FanLevel(configuration as MyStatPipe2Configuration),
                    selectedPosition
                )
                updateFanModeCache(selectedFanMode)
                selectedFanMode
            }

            ProfileType.MYSTAT_PIPE4 -> {
                val selectedFanMode = getMyStatSelectedFanMode(
                    getMyStatPipe4FanLevel(configuration as MyStatPipe4Configuration),
                    selectedPosition
                )
                updateFanModeCache(selectedFanMode)
                selectedFanMode
            }

            else -> {
                -1
            }
        }
        CcuLog.i(L.TAG_CCU_MSHST, "handleMyStatFanMode: $actualFanMode")
        if (actualFanMode != -1) {
            updateUserIntentPoints(
                equipId, fanMode, actualFanMode.toDouble(), CCUHsApi.getInstance().ccuUserName
            )
            if (selectedPosition != 0 && ( selectedPosition % 3 == 0 || isFanModeCurrentOccupied(selectedPosition))) cacheStorage.saveFanModeInCache(
                equipId, actualFanMode
            ) // while saving the fan mode, we need to save the actual fan mode instead of selected position
            else cacheStorage.removeFanModeFromCache(equipId)
        }
    }
}

fun handleMyStatHumidityMode(
    selectedPosition: Int, equipId: String, equip: MyStatEquip
) {
    updateUserIntentPoints(
        equipId, equip.targetHumidifier,
        (selectedPosition + 1).toDouble(),
        CCUHsApi.getInstance().ccuUserName
    )
}

fun handleMyStatDeHumidityMode(
    selectedPosition: Int, equipId: String, equip: MyStatEquip
) {
    updateUserIntentPoints(
        equipId, equip.targetDehumidifier,
        (selectedPosition + 1).toDouble(),
        CCUHsApi.getInstance().ccuUserName
    )
}


fun getSupplyDirection(nodeAddress: String): String {
    val profile = L.getProfile(nodeAddress.toLong()) as MyStatPipe2Profile
    return profile.supplyDirection()
}