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
import a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd
import a75f.io.logic.bo.building.system.setFanTypeToStages
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.compose.showErrorDialog
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import android.text.Html
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DabStagedVfdRtuViewModel : DabStagedRtuBaseViewModel() {
    fun init(context: Context, hayStack : CCUHsApi) {
        super.init(context, ModelLoader.getDabStagedVfdRtuModelDef(), hayStack)

        CcuLog.i(Domain.LOG_TAG, "DabStagedVfdRtuViewModel Init")
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE) //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")
        if (systemEquip["profile"].toString() == "dabStagedRtuVfdFan" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for systemEquip")
            profileConfiguration = StagedVfdRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as DabStagedRtuWithVfd
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
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("dabStagedRtuVfdFan")

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
                )
                return false
            }
            if (changeOverCooling && changeOverHeating) {
                showErrorDialog(
                    context,
                    Html.fromHtml(
                        "<b>The O/B changeover relay is mapped for both cooling and heating. Please select only one.</b>",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )
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
        if(BuildConfig.BUILD_TYPE == CARRIER_PROD){
            ProgressDialogUtils.showProgressDialog(context,"Saving VVT-C VFD Configuration")
        }else{
            ProgressDialogUtils.showProgressDialog(context, "Saving DAB VFD Configuration")
        }
        viewModelScope.launch (highPriorityDispatcher) {
            val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
            if (systemEquip["profile"].toString() != "dabStagedRtuVfdFan" &&
                systemEquip["profile"].toString() != ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name) {

                CcuLog.d(Tags.ADD_REMOVE_PROFILE, "DabStagedVfdRtuViewModel removing profile with it -->${systemEquip["id"].toString()}")
                deleteSystemProfile(systemEquip["id"].toString())
                systemEquipId = createNewEquip(systemEquip["id"].toString())
                L.ccu().systemProfile!!.deleteSystemEquip()
                L.ccu().systemProfile = DabStagedRtuWithVfd()
                L.ccu().systemProfile.removeSystemEquipModbus()
                L.ccu().systemProfile.removeSystemEquipBacnet()
                L.ccu().systemProfile.addSystemEquip()
                UnusedPortsModel.saveUnUsedPortStatusOfSystemProfile(profileConfiguration, hayStack)
                profileConfiguration.unusedPorts.clear()
                profileConfiguration.unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
                viewState.value.unusedPortState = profileConfiguration.unusedPorts
            }else{
                val vfdViewState = viewState.value as StagedRtuVfdViewState
                val vfdConfig = profileConfiguration as StagedVfdRtuProfileConfig
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
            val stagedRtu = L.ccu().systemProfile as DabStagedRtuWithVfd
            stagedRtu.updateStagesSelected()
            DesiredTempDisplayMode.setSystemModeForDab(hayStack)
            updateSystemMode()
            systemEquipId?.let {
                L.ccu().systemProfile.updateAhuRef(it)
            }
            hayStack.syncEntityTree()
            hayStack.setCcuReady()
            initialPortValues = HashMap(profileConfiguration.unusedPorts)
            setFanTypeToStages(profileConfiguration)
        }
    }

    fun reset() {
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (systemEquip["profile"].toString() == "dabStagedRtuVfdFan" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name) {
            profileConfiguration = StagedVfdRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as DabStagedRtuWithVfd
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
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("dabStagedRtuVfdFan")
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