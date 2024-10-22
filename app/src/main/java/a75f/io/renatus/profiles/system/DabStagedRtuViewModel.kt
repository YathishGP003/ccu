package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.dab.DabStagedRtu
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DabStagedRtuViewModel : DabStagedRtuBaseViewModel()  {
    fun init(context: Context, hayStack : CCUHsApi) {
        super.init(context, ModelLoader.getDabStageRtuModelDef(), hayStack)

        CcuLog.i(Domain.LOG_TAG, "DabStagedRtuViewModel Init")
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule") //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        if (systemEquip["profile"].toString() == "dabStagedRtu" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_DAB_STAGED_RTU.name) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for systemEquip")
            profileConfiguration = StagedRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as DabStagedRtu
            stagedRtu.updateStagesSelected()
            CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
            viewState.value = StagedRtuViewState.fromProfileConfig(profileConfiguration)
            CcuLog.i(Domain.LOG_TAG, "Active profile config loaded")
        } else {
            CcuLog.i(Domain.LOG_TAG, "Get default config for systemEquip")
            profileConfiguration = StagedRtuProfileConfig(model).getDefaultConfiguration()
            viewState.value = StagedRtuViewState.fromProfileConfig(profileConfiguration)
            CcuLog.i(Domain.LOG_TAG, "Default profile config Loaded")
        }
        modelLoadedState.postValue(true)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("dabStagedRtu")
    }

    override fun saveConfiguration() {
        hayStack.resetCcuReady()
        var systemEquipId : String? = null
        if(BuildConfig.BUILD_TYPE == CARRIER_PROD){
            ProgressDialogUtils.showProgressDialog(context,"Saving VVT-C Configuration")
        } else {
            ProgressDialogUtils.showProgressDialog(context, "Saving DAB Configuration")
        }
        viewModelScope.launch (highPriorityDispatcher) {
            val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
            if (systemEquip["profile"].toString() != "dabStagedRtu" &&
                systemEquip["profile"].toString() != ProfileType.SYSTEM_DAB_STAGED_RTU.name) {
                viewState.value.updateConfigFromViewState(profileConfiguration)
                deleteSystemProfile(systemEquip["id"].toString())
                systemEquipId = createNewEquip(systemEquip["id"].toString())
                L.ccu().systemProfile = DabStagedRtu()
                L.ccu().systemProfile.removeSystemEquipModbus()
                L.ccu().systemProfile.addSystemEquip()
                CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
                UnusedPortsModel.saveUnUsedPortStatusOfSystemProfile(
                    profileConfiguration,
                    hayStack
                )
                profileConfiguration.unusedPorts.clear()
                profileConfiguration.unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
                viewState.value.unusedPortState = profileConfiguration.unusedPorts
            } else {
                super.saveConfiguration()
            }
            withContext(Dispatchers.Main) {
                ProgressDialogUtils.hideProgressDialog()
            }
            val stagedRtu = L.ccu().systemProfile as DabStagedRtu
            stagedRtu.updateStagesSelected()
            DesiredTempDisplayMode.setSystemModeForDab(hayStack)
            updateSystemMode()
            systemEquipId?.let { L.ccu().systemProfile.updateAhuRef(it) }
            hayStack.syncEntityTree()
            hayStack.setCcuReady()
        }
    }

    fun reset() {
        val systemEquip = Domain.hayStack.readEntity("system and equip and not modbus and not connectModule")
        if (systemEquip["profile"].toString() == "dabStagedRtu" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_DAB_STAGED_RTU.name) {
            profileConfiguration = StagedRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as DabStagedRtu
            stagedRtu.updateStagesSelected()
            CcuLog.i(Domain.LOG_TAG, "Active profile config Loaded")
        }else{
            profileConfiguration = StagedRtuProfileConfig(model).getDefaultConfiguration()
            CcuLog.i(Domain.LOG_TAG, "Default profile config Loaded")
        }
        viewState.value = StagedRtuViewState.fromProfileConfig(profileConfiguration)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("dabStagedRtu")
    }
}