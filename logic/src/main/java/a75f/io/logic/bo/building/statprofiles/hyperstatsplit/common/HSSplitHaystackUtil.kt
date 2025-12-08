package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common

import a75f.io.domain.equips.hyperstatsplit.HyperStatSplitEquip
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorConfiguration
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getCpuPossibleConditioningModeSettings
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getUvPossibleConditioningMode

/**
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */
class HSSplitHaystackUtil(
    val equipRef: String,

) {
    companion object {

        fun getActualConditioningMode(
            hssEquip: HyperStatSplitEquip,
            selectedConditioningMode: Int
        ): Int {
            if (selectedConditioningMode == 0) return StandaloneConditioningMode.OFF.ordinal
            return when (getHssProfileConditioningMode(getSplitConfiguration(hssEquip.getId()))) {
                PossibleConditioningMode.BOTH -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
                PossibleConditioningMode.COOLONLY -> StandaloneConditioningMode.COOL_ONLY.ordinal
                PossibleConditioningMode.HEATONLY -> StandaloneConditioningMode.HEAT_ONLY.ordinal
                PossibleConditioningMode.OFF -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
            }
        }

        fun getSelectedConditioningMode(
            actualConditioningMode: Int,
            config: HyperStatSplitConfiguration
        ): Int {
            if (actualConditioningMode == 0)
                return StandaloneConditioningMode.OFF.ordinal
            return if (getHssProfileConditioningMode(config) == PossibleConditioningMode.BOTH)
                StandaloneConditioningMode.values()[actualConditioningMode].ordinal
            else
                1 // always it will be 1 because possibility is Off,CoolOnly | Off,Heatonly
        }

        fun getHssProfileConditioningMode(config: HyperStatSplitConfiguration?): PossibleConditioningMode {
            return when (config) {
                is HyperStatSplitCpuConfiguration -> getCpuPossibleConditioningModeSettings(config)
                else -> getUvPossibleConditioningMode(config as UnitVentilatorConfiguration)
            }
        }

        fun getActualFanMode(equip: HyperStatSplitEquip, position: Int): Int{
            return HyperStatSplitAssociationUtil.getSelectedFanModeByLevel(
                fanLevel = HyperStatSplitAssociationUtil.getHssProfileFanLevel(equip),
                selectedFan = position
            ).ordinal

        }

        fun getFanSelectionMode(hssEquip: HyperStatSplitEquip, position: Int): Int{
            return HyperStatSplitAssociationUtil.getSelectedFanMode(
                fanLevel = HyperStatSplitAssociationUtil.getHssProfileFanLevel(hssEquip),
                selectedFan = position
            )
        }
    }

}
