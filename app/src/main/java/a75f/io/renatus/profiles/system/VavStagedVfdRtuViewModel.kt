package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.hasChanges
import a75f.io.domain.util.CommonQueries
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.system.setFanTypeToStages
import a75f.io.logic.bo.building.system.vav.VavStagedRtu
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.compose.showErrorDialog
import a75f.io.renatus.profiles.oao.updateOaoPoints
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import android.text.Html
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VavStagedVfdRtuViewModel : StagedRtuProfileViewModel() {
    fun init(context: Context, hayStack : CCUHsApi) {
        super.init(context, ModelLoader.getVavStagedVfdRtuModelDef(), hayStack)

        CcuLog.i(Domain.LOG_TAG, "VavStagedVfdRtuViewModel Init")

        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE) //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        if (systemEquip["profile"].toString() == "vavStagedRtuVfdFan" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for systemEquip")
            profileConfiguration = StagedVfdRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as VavStagedRtuWithVfd
            stagedRtu.updateStagesSelected()
            CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
            val vfdProfileConfig = profileConfiguration as StagedVfdRtuProfileConfig
            viewState.value = StagedRtuVfdViewState.fromProfileConfig(vfdProfileConfig)
            CcuLog.i(Domain.LOG_TAG, "Active profile config loaded")
        } else {
            profileConfiguration = StagedVfdRtuProfileConfig(model).getDefaultConfiguration()
            val vfdProfileConfig = profileConfiguration as StagedVfdRtuProfileConfig
            CcuLog.i(Domain.LOG_TAG, vfdProfileConfig.toString())
            viewState.value = StagedRtuVfdViewState.fromProfileConfig(vfdProfileConfig)
            CcuLog.i(Domain.LOG_TAG, "Default profile config loaded")
        }
        initialPortValues = HashMap(profileConfiguration.unusedPorts)
        modelLoadedState.postValue(true)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavStagedRtuVfdFan")
    }




    private fun isValidConfiguration(): Boolean {
        val config = profileConfiguration as StagedVfdRtuProfileConfig
        val compressorAvailable =
            config.isAnyRelayEnabledAndMapped(Stage.COMPRESSOR_1.ordinal) ||
                    config.isAnyRelayEnabledAndMapped(Stage.COMPRESSOR_2.ordinal) ||
                    config.isAnyRelayEnabledAndMapped(Stage.COMPRESSOR_3.ordinal) ||
                    config.isAnyRelayEnabledAndMapped(Stage.COMPRESSOR_4.ordinal) ||
                    config.isAnyRelayEnabledAndMapped(Stage.COMPRESSOR_5.ordinal) ||
                    config.isAnyAnalogOut2EnabledAndMapped(StagedRTUAnalogOutMappings.COMPRESSOR_SPEED.ordinal)

        val changeOverCooling =
            profileConfiguration.isAnyRelayEnabledAndMapped(Stage.CHANGE_OVER_COOLING.ordinal)
        val changeOverHeating =
            profileConfiguration.isAnyRelayEnabledAndMapped(Stage.CHANGE_OVER_HEATING.ordinal)

        if (compressorAvailable) {
            if (!changeOverCooling && !changeOverHeating) {
                showErrorDialog(
                    context,
                    Html.fromHtml(
                        "<b>The compressor is mapped, but the O/B changeover relay is not mapped.</b>",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                ){ viewState.value.isStateChanged = true; }
                return false
            }
            if (changeOverCooling && changeOverHeating) {
                showErrorDialog(
                    context,
                    Html.fromHtml(
                        "<b>The O/B changeover relay is mapped for both cooling and heating. Please select only one.</b>",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                ){ viewState.value.isStateChanged = true; }
                return false
            }

        } else {
            if (changeOverCooling || changeOverHeating) {
                showErrorDialog(
                    context,
                    Html.fromHtml(
                        "<b>The O/B changeover relay is mapped, but the compressor is not mapped.</b>",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )
                return false
            }
        }
        return true
    }


    override fun saveConfiguration() {



        val vfdViewState = viewState.value as StagedRtuVfdViewState
        val vfdConfig = profileConfiguration as StagedVfdRtuProfileConfig
        vfdViewState.updateConfigFromViewState(vfdConfig)

        if (!isValidConfiguration()) {
            viewState.value.isSaveRequired = true
            viewState.value.isStateChanged = true
            return
        }


        hayStack.resetCcuReady()
        var systemEquipId : String? = null
        ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
        viewModelScope.launch (highPriorityDispatcher) {
            val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
            if (systemEquip["profile"].toString() != "vavStagedRtuVfdFan" &&
                    systemEquip["profile"].toString() != ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name) {
                vfdViewState.updateConfigFromViewState(vfdConfig)
                CcuLog.d(Tags.ADD_REMOVE_PROFILE, "VavStagedVfdRtuViewModel removing profile with it -->${systemEquip["id"].toString()}")
                deleteSystemProfile(systemEquip["id"].toString())
                systemEquipId = createNewEquip()
                L.ccu().systemProfile!!.deleteSystemEquip()
                L.ccu().systemProfile = VavStagedRtuWithVfd()
                L.ccu().systemProfile.removeSystemEquipModbus()
                L.ccu().systemProfile.removeSystemEquipBacnet()
                L.ccu().systemProfile.addSystemEquip()
                UnusedPortsModel.saveUnUsedPortStatusOfSystemProfile(profileConfiguration, hayStack)
                profileConfiguration.unusedPorts.clear()
                profileConfiguration.unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
                viewState.value.unusedPortState = profileConfiguration.unusedPorts
            }else{
                vfdViewState.updateConfigFromViewState(vfdConfig)
                val equipDis = "${hayStack.siteName}-${model.name}"
                equipBuilder.updateEquipAndPoints(vfdConfig, model, hayStack.site!!.id, equipDis, isReconfiguration = true)

                deviceBuilder.updateDeviceAndPoints(
                    vfdConfig,
                    deviceModel,
                    Domain.systemEquip.equipRef,
                    hayStack.site!!.id,
                    hayStack.siteName +"-"+ deviceModel.name
                )
                DomainManager.addSystemDomainEquip(hayStack)
                UnusedPortsModel.saveUnUsedPortStatusOfSystemProfile(profileConfiguration, hayStack)
                profileConfiguration.unusedPorts.clear()
                profileConfiguration.unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
                viewState.value.unusedPortState = profileConfiguration.unusedPorts
             }

            withContext(Dispatchers.Main) {
                ProgressDialogUtils.hideProgressDialog()
            }
            val stagedRtu = L.ccu().systemProfile as VavStagedRtu
            stagedRtu.updateStagesSelected()
            DesiredTempDisplayMode.setSystemModeForVav(hayStack)
            updateSystemMode()
            systemEquipId?.let {
                L.ccu().systemProfile.updateAhuRef(it)
            }
            updateOaoPoints()
            hayStack.syncEntityTree()
            hayStack.setCcuReady()
            initialPortValues = HashMap(profileConfiguration.unusedPorts)
            setFanTypeToStages(profileConfiguration)
        }
    }

    fun reset() {
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (systemEquip["profile"].toString() == "vavStagedRtuVfdFan" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name) {
            profileConfiguration = StagedVfdRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as VavStagedRtuWithVfd
            stagedRtu.updateStagesSelected()
            val vfdProfileConfig = profileConfiguration as StagedVfdRtuProfileConfig
            viewState.value = StagedRtuVfdViewState.fromProfileConfig(vfdProfileConfig)
            CcuLog.i(Domain.LOG_TAG, "Active profile config Loaded")
        }else{
            profileConfiguration = StagedVfdRtuProfileConfig(model).getDefaultConfiguration()
            val vfdProfileConfig = profileConfiguration as StagedVfdRtuProfileConfig
            viewState.value = StagedRtuVfdViewState.fromProfileConfig(vfdProfileConfig)
            CcuLog.i(Domain.LOG_TAG, "Default profile config Loaded")
        }
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavStagedRtuVfdFan")
    }

    fun hasUnsavedChanges(): Boolean {
        return try {
            val newConfiguration = StagedVfdRtuProfileConfig(model).getDefaultConfiguration()
            viewState.value.updateConfigFromViewState(newConfiguration)
            return hasChanges(
                profileConfiguration,
                newConfiguration
            ) || (initialPortValues.toMap() != viewState.value.unusedPortState.toMap())
        } catch (e: Exception) {
            false
        }
    }
}