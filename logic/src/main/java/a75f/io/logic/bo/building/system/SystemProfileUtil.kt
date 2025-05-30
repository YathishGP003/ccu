package a75f.io.logic.bo.building.system

import a75f.io.domain.api.Domain
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.util.ModelLoader.getDabAdvancedAhuCmModelV2
import a75f.io.domain.util.ModelLoader.getDabAdvancedAhuConnectModelV2
import a75f.io.domain.util.ModelLoader.getDabModulatingRtuModelDef
import a75f.io.domain.util.ModelLoader.getDabStageRtuModelDef
import a75f.io.domain.util.ModelLoader.getDabStagedVfdRtuModelDef
import a75f.io.domain.util.ModelLoader.getVavAdvancedAhuCmModelV2
import a75f.io.domain.util.ModelLoader.getVavAdvancedAhuConnectModelV2
import a75f.io.domain.util.ModelLoader.getVavModulatingRtuModelDef
import a75f.io.domain.util.ModelLoader.getVavStageRtuModelDef
import a75f.io.domain.util.ModelLoader.getVavStagedVfdRtuModelDef
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.system.util.AdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.CmConfiguration
import a75f.io.logic.bo.building.system.util.ConnectConfiguration
import a75f.io.logic.bo.building.system.vav.config.DabModulatingRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import java.util.HashMap as HashMap


// This block sets the fan type and stages into hashmap collection.
// The collection is used in the OAO setting message,
// specifically for the defaultOutsideAirOptimizationDamperPosition.
fun setFanTypeToStages(profileConfiguration: ProfileConfiguration?) {
    try {
        setFanTypeToStages(profileConfiguration, null, null)
        L.ccu().systemProfile.fanTypeToStages.forEach { (fanType: FanType?, stages: MutableSet<Stage?>?) ->
            CcuLog.d(
                L.TAG_CCU_SYSTEM,
                profileConfiguration?.profileType + ": Fan Type: $fanType, Stages: $stages"
            )
        }
    } catch (exception: Exception) {
        CcuLog.e(Domain.LOG_TAG, "Error in setFanTypeToStages: " + exception.message)
        exception.printStackTrace()
    }
}

// below function is used to get the active system configuration
fun getActiveSystemConfiguration(systemProfile: String): ProfileConfiguration? {
    CcuLog.d(L.TAG_CCU_SYSTEM, "getting active system $systemProfile")
    if (systemProfile == "vavStagedRtu" || ProfileType.SYSTEM_VAV_STAGED_RTU.name == systemProfile) {
        return StagedRtuProfileConfig(getVavStageRtuModelDef() as SeventyFiveFProfileDirective)
            .getActiveConfiguration()
    } else if (systemProfile == "vavStagedRtuVfdFan" || ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name == systemProfile) {
        return StagedRtuProfileConfig(getVavStagedVfdRtuModelDef() as SeventyFiveFProfileDirective)
            .getActiveConfiguration()
    } else if (systemProfile == "dabStagedRtu" || ProfileType.SYSTEM_DAB_STAGED_RTU.name == systemProfile) {
        return StagedRtuProfileConfig(getDabStageRtuModelDef() as SeventyFiveFProfileDirective)
            .getActiveConfiguration()
    } else if (systemProfile == "dabStagedRtuVfdFan" || ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name == systemProfile) {
        return StagedRtuProfileConfig(getDabStagedVfdRtuModelDef() as SeventyFiveFProfileDirective)
            .getActiveConfiguration()
    } else if (systemProfile == "vavFullyModulatingAhu" || ProfileType.SYSTEM_VAV_ANALOG_RTU.name == systemProfile) {
        return ModulatingRtuProfileConfig(getVavModulatingRtuModelDef() as SeventyFiveFProfileDirective)
            .getActiveConfiguration()
    } else if (systemProfile == "dabFullyModulatingAhu" || ProfileType.SYSTEM_DAB_ANALOG_RTU.name == systemProfile) {
        return ModulatingRtuProfileConfig(getDabModulatingRtuModelDef() as SeventyFiveFProfileDirective)
            .getActiveConfiguration()
    } else if (systemProfile == "vavAdvancedHybridAhuV2") {
        return AdvancedHybridAhuConfig(
            getVavAdvancedAhuCmModelV2() as SeventyFiveFProfileDirective,
            getVavAdvancedAhuConnectModelV2() as SeventyFiveFProfileDirective
        ).getActiveConfiguration().cmConfiguration
    } else if (systemProfile == "dabAdvancedHybridAhuV2") {
        return AdvancedHybridAhuConfig(
            getDabAdvancedAhuCmModelV2() as SeventyFiveFProfileDirective,
            getDabAdvancedAhuConnectModelV2() as SeventyFiveFProfileDirective
        ).getActiveConfiguration().cmConfiguration
    } else {
        CcuLog.d(L.TAG_CCU_SYSTEM, "Fan type to stages not set for system profile $systemProfile")
        return null
    }
}


fun setFanTypeToStages(
    profileConfiguration: ProfileConfiguration?,
    cmConfig: CmConfiguration?, connectConfig: ConnectConfiguration?
) {
    if (profileConfiguration == null) {
        CcuLog.e(L.TAG_CCU_SYSTEM, "ProfileConfiguration is null")
        return
    }

    L.ccu().systemProfile.fanTypeToStages = HashMap()

    if (profileConfiguration.profileType.equals("vavStagedRtu", ignoreCase = true)
        || profileConfiguration.profileType.equals("vavStagedRtuVfdFan", ignoreCase = true)
        || profileConfiguration.profileType.equals("dabStagedRtu", ignoreCase = true)
        || profileConfiguration.profileType.equals("dabStagedRtuVfdFan", ignoreCase = true)
    ) {
        val config = profileConfiguration as StagedRtuProfileConfig

        val stages = HashSet<Stage>()

        // add RELAY fan type
        if (config.relay1Enabled.enabled) {
            addStages(config.relay1Association, stages)
        }
        if (config.relay2Enabled.enabled) {
            addStages(config.relay2Association, stages)
        }
        if (config.relay3Enabled.enabled) {
            addStages(config.relay3Association, stages)
        }
        if (config.relay4Enabled.enabled) {
            addStages(config.relay4Association, stages)
        }
        if (config.relay5Enabled.enabled) {
            addStages(config.relay5Association, stages)
        }
        if (config.relay6Enabled.enabled) {
            addStages(config.relay6Association, stages)
        }
        if (config.relay7Enabled.enabled) {
            addStages(config.relay7Association, stages)
        }

        // add RELAY / NONE fan type
        if (stages.isNotEmpty()) {
            L.ccu().systemProfile.fanTypeToStages[FanType.RELAY] = stages
        }

        // add ANALOG / HYBRID fan type
        if (profileConfiguration.profileType.equals("vavStagedRtuVfdFan", ignoreCase = true)
            || profileConfiguration.profileType.equals("dabStagedRtuVfdFan", ignoreCase = true)
        ) {
            val vfdConfig = profileConfiguration as StagedVfdRtuProfileConfig
            if (vfdConfig.analogOut2Enabled.enabled) {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    stages.add(Stage.FAN_1)
                    L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                } else {
                    removeKeyFromfanTypeToStages()
                    L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                }
            } else {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                }
            }
        } else{
            if (stages.isEmpty()) {
                L.ccu().systemProfile.fanTypeToStages.clear()
                L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
            }
        }
    } else if (profileConfiguration.profileType.equals(
            "vavFullyModulatingAhu",
            ignoreCase = true
        )
    ) {
        val modulatingConfig = profileConfiguration as ModulatingRtuProfileConfig
        val stages = HashSet<Stage>()

        // add ANALOG / HYBRID fan type
        if (modulatingConfig.analog2OutputEnable.enabled) {
            L.ccu().systemProfile.fanTypeToStages.clear()
            stages.add(Stage.FAN_1)
            L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
        } else {
            L.ccu().systemProfile.fanTypeToStages.clear()
            L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
        }
    } else if (profileConfiguration.profileType.equals(
            "dabFullyModulatingAhu",
            ignoreCase = true
        )
    ) {
        val modulatingConfig = profileConfiguration as DabModulatingRtuProfileConfig
        val stages = HashSet<Stage>()
        // add ANALOG / HYBRID fan type
        if (modulatingConfig.analog2OutputEnable.enabled) {
            L.ccu().systemProfile.fanTypeToStages.clear()
            stages.add(Stage.FAN_1)
            L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
        } else {
            L.ccu().systemProfile.fanTypeToStages.clear()
            L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
        }

    } else if (profileConfiguration.profileType.equals("vavAdvancedHybridAhuV2", ignoreCase = true)
        || profileConfiguration.profileType.equals("dabAdvancedHybridAhuV2", ignoreCase = true)
    ) {
        if (cmConfig == null) {
            CcuLog.e(L.TAG_CCU_SYSTEM, "CmConfiguration is null")
            return
        }
        val stages = HashSet<Stage>()
        // add RELAY / NONE fan type - cm configuration
        if (cmConfig.relay1Enabled.enabled) {
            addStagesV2(cmConfig.relay1Association, stages)
        }
        if (cmConfig.relay2Enabled.enabled) {
            addStagesV2(cmConfig.relay2Association, stages)
        }
        if (cmConfig.relay3Enabled.enabled) {
            addStagesV2(cmConfig.relay3Association, stages)
        }
        if (cmConfig.relay4Enabled.enabled) {
            addStagesV2(cmConfig.relay4Association, stages)
        }
        if (cmConfig.relay5Enabled.enabled) {
            addStagesV2(cmConfig.relay5Association, stages)
        }
        if (cmConfig.relay6Enabled.enabled) {
            addStagesV2(cmConfig.relay6Association, stages)
        }
        if (cmConfig.relay7Enabled.enabled) {
            addStagesV2(cmConfig.relay7Association, stages)
        }
        if (cmConfig.relay8Enabled.enabled) {
            addStagesV2(cmConfig.relay8Association, stages)
        }

        if (connectConfig != null) {
            // add RELAY / NONE fan type - connect configuration
            if (connectConfig.relay1Enabled.enabled) {
                addStagesV2(connectConfig.relay1Association, stages)
            }
            if (connectConfig.relay2Enabled.enabled) {
                addStagesV2(connectConfig.relay2Association, stages)
            }
            if (connectConfig.relay3Enabled.enabled) {
                addStagesV2(connectConfig.relay3Association, stages)
            }
            if (connectConfig.relay4Enabled.enabled) {
                addStagesV2(connectConfig.relay4Association, stages)
            }
            if (connectConfig.relay5Enabled.enabled) {
                addStagesV2(connectConfig.relay5Association, stages)
            }
            if (connectConfig.relay6Enabled.enabled) {
                addStagesV2(connectConfig.relay6Association, stages)
            }
            if (connectConfig.relay7Enabled.enabled) {
                addStagesV2(connectConfig.relay7Association, stages)
            }
            if (connectConfig.relay8Enabled.enabled) {
                addStagesV2(connectConfig.relay8Association, stages)
            }
        }
        // add RELAY / NONE fan type
        if (stages.isNotEmpty()) {
            L.ccu().systemProfile.fanTypeToStages[FanType.RELAY] = stages
        }

        // analog based fan type
        if (cmConfig.analogOut1Enabled.enabled) {
            if (cmConfig.analogOut1Association.associationVal == 0
                || cmConfig.analogOut1Association.associationVal == 5
            ) {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    stages.add(Stage.FAN_1)
                    L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                } else {
                    removeKeyFromfanTypeToStages()
                    L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                    return
                }
            } else {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                }
            }
        }

        if (cmConfig.analogOut2Enabled.enabled) {
            if (cmConfig.analogOut2Association.associationVal == 0
                || cmConfig.analogOut2Association.associationVal == 5
            ) {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    stages.add(Stage.FAN_1)
                    L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                } else {
                    removeKeyFromfanTypeToStages()
                    L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                    return
                }
            } else {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                }
            }
        }

        if (cmConfig.analogOut3Enabled.enabled) {
            if (cmConfig.analogOut3Association.associationVal == 0
                || cmConfig.analogOut3Association.associationVal == 5
            ) {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    stages.add(Stage.FAN_1)
                    L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                } else {
                    removeKeyFromfanTypeToStages()
                    L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                    return
                }
            } else {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                }
            }
        }

        if (cmConfig.analogOut4Enabled.enabled) {
            if (cmConfig.analogOut4Association.associationVal == 0
                || cmConfig.analogOut4Association.associationVal == 5
            ) {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    stages.add(Stage.FAN_1)
                    L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                } else {
                    removeKeyFromfanTypeToStages()
                    L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                    return
                }
            } else {
                if (stages.isEmpty()) {
                    L.ccu().systemProfile.fanTypeToStages.clear()
                    L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                }
            }
        }

        if (connectConfig != null) {
            // analog based fan type
            if (connectConfig.analogOut1Enabled.enabled) {
                if (connectConfig.analogOut1Association.associationVal == 2) {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        stages.add(Stage.FAN_1)
                        L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                    } else {
                        removeKeyFromfanTypeToStages()
                        L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                        return
                    }
                    return
                } else {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                    }
                }
            }

            if (connectConfig.analogOut2Enabled.enabled) {
                if (connectConfig.analogOut2Association.associationVal == 2) {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        stages.add(Stage.FAN_1)
                        L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                    } else {
                        removeKeyFromfanTypeToStages()
                        L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                        return
                    }
                } else {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                    }
                }
            }

            if (connectConfig.analogOut3Enabled.enabled) {
                if (connectConfig.analogOut3Association.associationVal == 2) {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        stages.add(Stage.FAN_1)
                        L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                    } else {
                        removeKeyFromfanTypeToStages()
                        L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                        return
                    }
                } else {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                    }
                }
            }

            if (connectConfig.analogOut4Enabled.enabled) {
                if (connectConfig.analogOut4Association.associationVal == 2) {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        stages.add(Stage.FAN_1)
                        L.ccu().systemProfile.fanTypeToStages[FanType.ANALOG] = stages
                    } else {
                        removeKeyFromfanTypeToStages()
                        L.ccu().systemProfile.fanTypeToStages[FanType.HYBRID] = stages
                        return
                    }
                } else {
                    if (stages.isEmpty()) {
                        L.ccu().systemProfile.fanTypeToStages.clear()
                        L.ccu().systemProfile.fanTypeToStages[FanType.NONE] = stages
                    }
                }
            }
        }
    }
}

private fun removeKeyFromfanTypeToStages() {
    // Remove the keys for RELAY, NONE and ANALOG from fanTypeToStages
    if (L.ccu().systemProfile.fanTypeToStages.containsKey(FanType.RELAY)) {
        L.ccu().systemProfile.fanTypeToStages.remove(FanType.RELAY)
    }
    if (L.ccu().systemProfile.fanTypeToStages.containsKey(FanType.NONE)) {
        L.ccu().systemProfile.fanTypeToStages.remove(FanType.NONE)
    }
    if (L.ccu().systemProfile.fanTypeToStages.containsKey(FanType.ANALOG)) {
        L.ccu().systemProfile.fanTypeToStages.remove(FanType.ANALOG)
    }
}

private fun addStages(relay: AssociationConfig, stages: HashSet<Stage>) {
    when (relay.associationVal) {
        Stage.FAN_1.ordinal -> {
            stages.add(Stage.FAN_1)
        }

        Stage.FAN_2.ordinal -> {
            stages.add(Stage.FAN_2)
        }

        Stage.FAN_3.ordinal, Stage.FAN_4.ordinal, Stage.FAN_5.ordinal -> {
            stages.add(Stage.FAN_3)
        }
    }
}

private fun addStagesV2(relay: AssociationConfig, stages: HashSet<Stage>) {
    when (relay.associationVal) {
        // load fan1, pressure fan1
        10, 27 -> {
            stages.add(Stage.FAN_1)
        }
        // load fan2, pressure fan2
        11, 28 -> {
            stages.add(Stage.FAN_2)
        }
        // load fan3, pressure fan3, load fan4, pressure fan4, load fan5, pressure fan5
        12, 29, 13, 30, 14, 31 -> {
            stages.add(Stage.FAN_3)
        }
    }
}

fun isAdvanceV2(): Boolean {
    return L.ccu().systemProfile.profileType.name == "vavAdvancedHybridAhuV2" ||
            L.ccu().systemProfile.profileName.equals("VAV Advanced Hybrid AHU v2") ||
            L.ccu().systemProfile.profileType.name == "dabAdvancedHybridAhuV2" ||
            L.ccu().systemProfile.profileName.equals("DAB Advanced Hybrid AHU v2")
}
