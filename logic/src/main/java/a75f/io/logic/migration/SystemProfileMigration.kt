package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.util.CommonQueries
import a75f.io.domain.util.allSystemProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.system.DefaultSystem
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.dab.DabAdvancedHybridRtu
import a75f.io.logic.bo.building.system.dab.DabExternalAhu
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu
import a75f.io.logic.bo.building.system.dab.DabStagedRtu
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu
import a75f.io.logic.bo.building.system.vav.VavBacnetRtu
import a75f.io.logic.bo.building.system.vav.VavExternalAhu
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu
import a75f.io.logic.bo.building.system.vav.VavIERtu
import a75f.io.logic.bo.building.system.vav.VavStagedRtu
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd
import a75f.io.logic.util.modifyConditioningMode

/** created by mukesh on 10-06-2025 **/

class SystemProfileMigration {
    companion object {

        fun loadProfileForMigration() {
            CcuLog.d(L.TAG_CCU_INIT, "Load Equip Profile for Migration")
            val site = CCUHsApi.getInstance().readEntity(Tags.SITE)
            if (site == null || site.size == 0) {
                CcuLog.d(L.TAG_CCU, "Site does not exist. Profiles not loaded")
                return
            }
            val equip = CCUHsApi.getInstance().readEntity(CommonQueries.SYSTEM_PROFILE)
            if (equip != null && equip.size > 0) {
                val eq = Equip.Builder().setHashMap(equip).build()
                CcuLog.d(
                    L.TAG_CCU,
                    "Load SystemEquip " + eq.displayName + " System profile " + eq.profile
                )
                if (eq.profile == "vavStagedRtu") {
                    L.ccu().systemProfile = VavStagedRtu()
                } else if (eq.profile == "vavStagedRtuVfdFan") {
                    L.ccu().systemProfile = VavStagedRtuWithVfd()
                } else if (eq.profile == "vavAdvancedHybridAhuV2") {
                    L.ccu().systemProfile = VavAdvancedAhu()
                } else if (eq.profile == "dabAdvancedHybridAhuV2") {
                    L.ccu().systemProfile = DabAdvancedAhu()
                } else if (eq.profile == "vavFullyModulatingAhu") {
                    L.ccu().systemProfile = VavFullyModulatingRtu()
                } else if (eq.profile == "dabStagedRtu") {
                    L.ccu().systemProfile = DabStagedRtu()
                } else if (eq.profile == "dabStagedRtuVfdFan") {
                    L.ccu().systemProfile = DabStagedRtuWithVfd()
                } else if (eq.profile == "dabFullyModulatingAhu") {
                    L.ccu().systemProfile = DabFullyModulatingRtu()
                } else {
                    when (ProfileType.valueOf(
                        Globals.getInstance().getDomainSafeProfile(eq.profile)
                    )) {
                        ProfileType.SYSTEM_VAV_ANALOG_RTU -> L.ccu().systemProfile =
                            VavFullyModulatingRtu()

                        ProfileType.SYSTEM_VAV_STAGED_RTU -> L.ccu().systemProfile = VavStagedRtu()
                        ProfileType.SYSTEM_VAV_STAGED_VFD_RTU -> L.ccu().systemProfile =
                            VavStagedRtuWithVfd()

                        ProfileType.SYSTEM_VAV_HYBRID_RTU -> L.ccu().systemProfile =
                            VavAdvancedHybridRtu()

                        ProfileType.SYSTEM_VAV_IE_RTU -> L.ccu().systemProfile = VavIERtu()
                        ProfileType.SYSTEM_VAV_BACNET_RTU -> L.ccu().systemProfile = VavBacnetRtu()
                        ProfileType.SYSTEM_DAB_ANALOG_RTU -> L.ccu().systemProfile =
                            DabFullyModulatingRtu()

                        ProfileType.SYSTEM_DAB_STAGED_RTU -> L.ccu().systemProfile = DabStagedRtu()
                        ProfileType.SYSTEM_DAB_STAGED_VFD_RTU -> L.ccu().systemProfile =
                            DabStagedRtuWithVfd()

                        ProfileType.SYSTEM_DAB_HYBRID_RTU -> L.ccu().systemProfile =
                            DabAdvancedHybridRtu()

                        ProfileType.dabExternalAHUController -> L.ccu().systemProfile =
                            DabExternalAhu()

                        ProfileType.vavExternalAHUController -> L.ccu().systemProfile =
                            VavExternalAhu()

                        else -> {
                            L.ccu().systemProfile = DefaultSystem()
                        }
                    }
                }
                L.ccu().systemProfile.setFanTypeToStages(eq.profile)
            } else {
                CcuLog.d(L.TAG_CCU, "System Equip does not exist.Create Default System Profile")
                L.ccu().systemProfile = DefaultSystem().createDefaultSystemEquip()
            }
            //add system equip if to run the stages
            L.ccu().systemProfile.addSystemEquip()
        }

        fun updateEnumValuesForSystemProfile() {

            if (L.ccu().systemProfile == null) {
                CcuLog.d(L.TAG_CCU_INIT, "System Profile is null. Cannot update enum values")
                return
            }
            if (L.ccu().systemProfile is DefaultSystem) {
                CcuLog.d(L.TAG_CCU_INIT, "Default System Profile. Cannot update enum values")
                return
            }


            val systemProfile = L.ccu().systemProfile

            val possibleConditioningMode = when {
                systemProfile.isCoolingAvailable && systemProfile.isHeatingAvailable -> PossibleConditioningMode.BOTH
                systemProfile.isCoolingAvailable && !systemProfile.isHeatingAvailable -> PossibleConditioningMode.COOLONLY
                !systemProfile.isCoolingAvailable && systemProfile.isHeatingAvailable -> PossibleConditioningMode.HEATONLY
                else -> PossibleConditioningMode.OFF
            }
            val conditioningMode = Point(DomainName.conditioningMode, Domain.systemEquip.equipRef)
            modifyConditioningMode(
                possibleConditioningMode.ordinal,
                conditioningMode,
                allSystemProfileConditions
            )
            CcuLog.d(L.TAG_CCU_INIT, "Update Enum Values for System Profile")
        }
    }
}