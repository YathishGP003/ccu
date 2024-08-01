package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToCooling
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToHeating
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayEnabledAssociatedToCooling
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayEnabledAssociatedToHeating
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuProfileConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created for HyperStat by Manjunath K on 06-08-2021.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */
class HSSplitHaystackUtil(
    val equipRef: String,
    private val haystack: CCUHsApi

) {
    companion object {

        fun getPossibleConditioningModeSettings(node: Int): PossibleConditioningMode {
            var status = PossibleConditioningMode.OFF
            try {
                val equip = CCUHsApi.getInstance().readEntity("equip and group == \"${node}\"")

                if (!equip.isEmpty() && equip.containsKey("profile")) {
                    // Add conditioning status
                    val config = when (equip.get("profile").toString()) {
                        ProfileType.HYPERSTATSPLIT_CPU.name -> HyperStatSplitCpuProfileConfiguration(
                            node, NodeType.HYPERSTATSPLIT.name, 0,
                            equip.get("roomRef").toString(), equip.get("floorRef").toString(),
                            ProfileType.HYPERSTATSPLIT_CPU, ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
                        ).getActiveConfiguration()

                        // this case should never happen
                        else -> HyperStatSplitCpuProfileConfiguration(
                            node, NodeType.HYPERSTATSPLIT.name, 0,
                            equip.get("roomRef").toString(), equip.get("floorRef").toString(),
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
                if ((isAnyRelayEnabledAssociatedToCooling(hssEquip) || isAnyAnalogOutEnabledAssociatedToCooling(hssEquip))
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

        fun getActualConditioningMode(hssEquip: HyperStatSplitEquip, selectedConditioningMode: Int): Int{
            if(selectedConditioningMode == 0)
                return StandaloneConditioningMode.OFF.ordinal
            return when(getPossibleConditioningModeSettings(hssEquip)){
                PossibleConditioningMode.BOTH-> {
                    StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
                }
                PossibleConditioningMode.COOLONLY->{
                    StandaloneConditioningMode.COOL_ONLY.ordinal
                }
                PossibleConditioningMode.HEATONLY->{
                    StandaloneConditioningMode.HEAT_ONLY.ordinal
                }
                PossibleConditioningMode.OFF->{
                    StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
                }
            }
        }

        fun getSelectedConditioningMode(hssEquip: HyperStatSplitEquip, actualConditioningMode: Int): Int{
            if(actualConditioningMode == 0)
                return StandaloneConditioningMode.OFF.ordinal
            return if(getPossibleConditioningModeSettings(hssEquip) ==  PossibleConditioningMode.BOTH)
                StandaloneConditioningMode.values()[actualConditioningMode].ordinal
            else
                1 // always it will be 1 because possibility is Off,CoolOnly | Off,Heatonly

        }

        fun getPossibleFanModeSettings(node: Int): PossibleFanMode {
            try {

                val equip = CCUHsApi.getInstance().readEntity("equip and group == \"${node}\"")

                if (!equip.isEmpty() && equip.containsKey("profile")) {
                    val config = when (equip.get("profile").toString()) {
                        ProfileType.HYPERSTATSPLIT_CPU.name -> HyperStatSplitCpuProfileConfiguration(
                            node,
                            NodeType.HYPERSTATSPLIT.name,
                            0,
                            equip.get("roomRef").toString(),
                            equip.get("floorRef").toString(),
                            ProfileType.HYPERSTATSPLIT_CPU,
                            ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
                        ).getActiveConfiguration()

                        // this case should never happen
                        else -> HyperStatSplitCpuProfileConfiguration(
                            node,
                            NodeType.HYPERSTATSPLIT.name,
                            0,
                            equip.get("roomRef").toString(),
                            equip.get("floorRef").toString(),
                            ProfileType.HYPERSTATSPLIT_CPU,
                            ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
                        ).getActiveConfiguration()

                    }

                    val fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(config as HyperStatSplitCpuProfileConfiguration)
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

        fun getActualFanMode(nodeAddress: String, position: Int): Int{
            val hssCpuProfile = L.getProfile(nodeAddress.toShort()) as HyperStatSplitCpuEconProfile
            return HyperStatSplitAssociationUtil.getSelectedFanModeByLevel(
                fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(hssCpuProfile.domainProfileConfiguration),
                selectedFan = position
            ).ordinal

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

    fun readPointPriorityVal(markers: String): Double {
        return haystack.readPointPriorityValByQuery(
            "point and $markers and equipRef == \"$equipRef\""
        )
    }

    fun readHisVal(markers: String): Double {
        return haystack.readHisValByQuery(
            "point and $markers and equipRef == \"$equipRef\""
        )
    }

    fun writeDefaultVal(markers: String, value: Double) {
        haystack.writeDefaultVal(
            "point and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun writeDefaultVal(markers: String, value: String) {
        haystack.writeDefaultVal(
            "point and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun writeHisValueByID(id: String, value: Double) {
        haystack.writeHisValById(id, value)
    }

    fun getCurrentTemp(): Double {
        return haystack.readHisValByQuery(
            "point and air and current and temp and sensor and equipRef == \"$equipRef\""
        )
    }

    fun setProfilePoint(markers: String, value: Double) {
        haystack.writeHisValByQuery(
            "point and  his and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

}
