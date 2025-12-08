package a75f.io.renatus.hyperstatsplit.ui

/**
 * Created by Nick P on 7/10/2023.
 */

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.equips.hyperstatsplit.HyperStatSplitEquip
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.uiutils.updateUserIntentPoints


fun handleConditionMode(
    selectedPosition: Int,
    equip: HyperStatSplitEquip,
    userClickCheck: Boolean
) {
    if(userClickCheck) {

        // CPU/Economizer Profile has combination of conditioning modes
        val actualConditioningMode: Int = getActualConditioningMode(equip, selectedPosition)

        if(actualConditioningMode != -1) {
            updateUserIntentPoints(
                equip.getId(), equip.conditioningMode,
                actualConditioningMode.toDouble(), CCUHsApi.getInstance().ccuUserName
            )
            val roomRef = HSUtil.getZoneIdFromEquipId(equip.getId())
            DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, CCUHsApi.getInstance())
        }
    }
}


// Save the fan mode in cache
fun handleFanMode(
    equip: HyperStatSplitEquip, selectedPosition: Int, userClickCheck: Boolean

) {
    if (userClickCheck) {
        val cacheStorage = FanModeCacheStorage.getHyperStatSplitFanModeCache()
        val actualFanMode: Int = getActualFanMode(equip, selectedPosition)
        if (actualFanMode != -1) {
            updateUserIntentPoints(
                equip.getId(), equip.fanOpMode,
                actualFanMode.toDouble(), CCUHsApi.getInstance().ccuUserName)
            if (selectedPosition != 0 && (selectedPosition % 3 == 0 || isFanModeCurrentOccupied(StandaloneFanStage.values()[actualFanMode])) )
                cacheStorage.saveFanModeInCache(equip.getId(), actualFanMode)
            else
                cacheStorage.removeFanModeFromCache(equip.getId())
        }
    }
}
private fun isFanModeCurrentOccupied(basicSettings: StandaloneFanStage): Boolean {
    return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
}

fun handleHumidityMode(equip: HyperStatSplitEquip, selectedPosition: Int) {
    updateUserIntentPoints(
            equip.equipRef, equip.targetHumidifier, (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}

fun handleDeHumidityMode(equip: HyperStatSplitEquip, selectedPosition: Int) {
    updateUserIntentPoints(
        equip.equipRef, equip.targetDehumidifier, (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}






