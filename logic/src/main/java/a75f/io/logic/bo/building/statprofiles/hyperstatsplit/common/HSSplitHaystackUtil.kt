package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToCooling
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToHeating
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayEnabledAssociatedToCooling
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayEnabledAssociatedToHeating
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.PossibleFanMode
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */
class HSSplitHaystackUtil(
    val equipRef: String,

) {
    companion object {

        fun getPossibleConditioningModeSettings(node: Int): PossibleConditioningMode {
            var status = PossibleConditioningMode.OFF
            try {
                val equip = CCUHsApi.getInstance().readEntity("equip and group == \"${node}\"")

                if (equip.isNotEmpty() && equip.containsKey("profile")) {
                    // Add conditioning status
                    val config = when (equip["profile"].toString()) {
                        ProfileType.HYPERSTATSPLIT_CPU.name -> HyperStatSplitCpuConfiguration(
                            node, NodeType.HYPERSTATSPLIT.name, 0,
                            equip["roomRef"].toString(), equip["floorRef"].toString(),
                            ProfileType.HYPERSTATSPLIT_CPU, ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
                        ).getActiveConfiguration()

                        // this case should never happen
                        else -> HyperStatSplitCpuConfiguration(
                            node, NodeType.HYPERSTATSPLIT.name, 0,
                            equip["roomRef"].toString(), equip["floorRef"].toString(),
                            ProfileType.HYPERSTATSPLIT_CPU, ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
                        ).getActiveConfiguration()

                    }

                    if ((isAnyRelayEnabledAssociatedToCooling(config) || isAnyAnalogOutEnabledAssociatedToCooling(config))
                        && (isAnyRelayEnabledAssociatedToHeating(config)|| isAnyAnalogOutEnabledAssociatedToHeating(config) )) {
                        status = PossibleConditioningMode.BOTH
                    } else if (isAnyRelayEnabledAssociatedToCooling(config) || isAnyAnalogOutEnabledAssociatedToCooling(config)) {
                        status = PossibleConditioningMode.COOLONLY
                    } else if (isAnyRelayEnabledAssociatedToHeating(config)||isAnyAnalogOutEnabledAssociatedToHeating(config)) {
                        status = PossibleConditioningMode.HEATONLY
                    }
                }

            }catch (e: Exception){
                CcuLog.e(L.TAG_CCU_HSCPU, "Exception getPossibleConditioningModeSettings: ${e.message}")
            }
            CcuLog.d(L.TAG_CCU_HSCPU, "getPossibleConditioningModeSettings: $status")
            return status
        }

        fun getPossibleConditioningModeSettings(hssEquip: HyperStatSplitEquip): PossibleConditioningMode {
            var status = PossibleConditioningMode.OFF
            try {

                if (hssEquip.compressorSpeed.pointExists() || hssEquip.compressorStage1.pointExists() ||
                    hssEquip.compressorStage2.pointExists() || hssEquip.compressorStage3.pointExists()
                ) {
                   return PossibleConditioningMode.BOTH
                } else if ((isAnyRelayEnabledAssociatedToCooling(hssEquip) || isAnyAnalogOutEnabledAssociatedToCooling(hssEquip))
                    && (isAnyRelayEnabledAssociatedToHeating(hssEquip)|| isAnyAnalogOutEnabledAssociatedToHeating(hssEquip) )) {
                    status = PossibleConditioningMode.BOTH
                } else if (isAnyRelayEnabledAssociatedToCooling(hssEquip) || isAnyAnalogOutEnabledAssociatedToCooling(hssEquip)) {
                    status = PossibleConditioningMode.COOLONLY
                } else if (isAnyRelayEnabledAssociatedToHeating(hssEquip)||isAnyAnalogOutEnabledAssociatedToHeating(hssEquip)) {
                    status = PossibleConditioningMode.HEATONLY
                }
            } catch (e: Exception){
                CcuLog.e(L.TAG_CCU_HSCPU, "Exception getPossibleConditioningModeSettings: ${e.message}")
            }
            CcuLog.d(L.TAG_CCU_HSCPU, "getPossibleConditioningModeSettings: $status")
            return status
        }

        fun getPossibleConditioningModeSettings(hssEquip: HyperStatSplitCpuConfiguration): PossibleConditioningMode {
            val status = PossibleConditioningMode.OFF
            try {
                if (hssEquip.isCompressorStage1Enabled() || hssEquip.isCompressorStage2Enabled() || hssEquip.isCompressorStage3Enabled()) {
                    return PossibleConditioningMode.BOTH
                } else if ((hssEquip.isCoolingStagesAvailable() || hssEquip.isAnalogCoolingEnabled()) &&
                    (hssEquip.isHeatingStagesAvailable() || hssEquip.isAnalogHeatingEnabled())) {
                    return PossibleConditioningMode.BOTH
                } else if (hssEquip.isCoolingStagesAvailable() || hssEquip.isAnalogCoolingEnabled()) {
                    return PossibleConditioningMode.COOLONLY
                } else if (hssEquip.isHeatingStagesAvailable() || hssEquip.isAnalogHeatingEnabled()) {
                    return PossibleConditioningMode.HEATONLY
                }

            } catch (e: Exception) {
                CcuLog.e(
                    L.TAG_CCU_HSCPU,
                    "Exception getPossibleConditioningModeSettings: ${e.message}"
                )
            }
            CcuLog.d(L.TAG_CCU_HSCPU, "getPossibleConditioningModeSettings: $status")
            return status
        }

        fun getActualConditioningMode(hssEquip: HyperStatSplitEquip, selectedConditioningMode: Int): Int{
            if(selectedConditioningMode == 0)
                return StandaloneConditioningMode.OFF.ordinal
            return when(getPossibleConditioningModeSettings(hssEquip)){
                PossibleConditioningMode.BOTH -> {
                    StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
                }
                PossibleConditioningMode.COOLONLY ->{
                    StandaloneConditioningMode.COOL_ONLY.ordinal
                }
                PossibleConditioningMode.HEATONLY ->{
                    StandaloneConditioningMode.HEAT_ONLY.ordinal
                }
                PossibleConditioningMode.OFF ->{
                    StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
                }
            }
        }

        fun getSelectedConditioningMode(hssEquip: HyperStatSplitEquip, actualConditioningMode: Int): Int{
            if(actualConditioningMode == 0)
                return StandaloneConditioningMode.OFF.ordinal
            return if(getPossibleConditioningModeSettings(hssEquip) == PossibleConditioningMode.BOTH)
                StandaloneConditioningMode.values()[actualConditioningMode].ordinal
            else
                1 // always it will be 1 because possibility is Off,CoolOnly | Off,Heatonly

        }

        fun getSplitPossibleFanModeSettings(node: Int): PossibleFanMode {
            try {

                val equip = CCUHsApi.getInstance().readEntity("equip and group == \"${node}\"")

                if (equip.isNotEmpty() && equip.containsKey("profile")) {
                    val config = when (equip["profile"].toString()) {
                        ProfileType.HYPERSTATSPLIT_CPU.name -> HyperStatSplitCpuConfiguration(
                            node,
                            NodeType.HYPERSTATSPLIT.name,
                            0,
                            equip["roomRef"].toString(),
                            equip["floorRef"].toString(),
                            ProfileType.HYPERSTATSPLIT_CPU,
                            ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
                        ).getActiveConfiguration()

                        // this case should never happen
                        else -> HyperStatSplitCpuConfiguration(
                            node,
                            NodeType.HYPERSTATSPLIT.name,
                            0,
                            equip["roomRef"].toString(),
                            equip["floorRef"].toString(),
                            ProfileType.HYPERSTATSPLIT_CPU,
                            ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
                        ).getActiveConfiguration()

                    }

                    val fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(config)
                    if (fanLevel == 1) return PossibleFanMode.AUTO
                    if (fanLevel == 6) return PossibleFanMode.LOW
                    if (fanLevel == 7) return PossibleFanMode.MED
                    if (fanLevel == 8) return PossibleFanMode.HIGH
                    if (fanLevel == 13) return PossibleFanMode.LOW_MED
                    if (fanLevel == 14) return PossibleFanMode.LOW_HIGH
                    if (fanLevel == 15) return PossibleFanMode.MED_HIGH
                    if (fanLevel == 21) return PossibleFanMode.LOW_MED_HIGH

                }

            } catch (e:Exception) {
                CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Exception getPossibleFanModeSettings: ${e.localizedMessage}")
            }
            return PossibleFanMode.OFF
        }


        fun getActualFanMode(hssEquip: HyperStatSplitEquip, position: Int): Int{
            return HyperStatSplitAssociationUtil.getSelectedFanModeByLevel(
                fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(hssEquip),
                selectedFan = position
            ).ordinal

        }

        fun getFanSelectionMode(hssEquip: HyperStatSplitEquip, position: Int): Int{
            return HyperStatSplitAssociationUtil.getSelectedFanMode(
                fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(hssEquip),
                selectedFan = position
            )
        }
    }

}
