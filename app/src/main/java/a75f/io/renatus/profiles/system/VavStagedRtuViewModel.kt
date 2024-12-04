package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.vav.VavStagedRtu
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.profiles.oao.updateOaoPoints
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VavStagedRtuViewModel : StagedRtuProfileViewModel() {
    fun init(context: Context, hayStack : CCUHsApi) {
        super.init(context, ModelLoader.getVavStageRtuModelDef(), hayStack)

        CcuLog.i(Domain.LOG_TAG, "VavStagedRtuViewModel Init")

        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule") //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        if (systemEquip["profile"].toString() == "vavStagedRtu" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_STAGED_RTU.name) {
            profileConfiguration = StagedRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as VavStagedRtu
            stagedRtu.updateStagesSelected()
            CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
            viewState.value = StagedRtuViewState.fromProfileConfig(profileConfiguration)
            CcuLog.i(Domain.LOG_TAG, "Active profile config loaded")
        } else {
            profileConfiguration = StagedRtuProfileConfig(model).getDefaultConfiguration()
            viewState.value = StagedRtuViewState.fromProfileConfig(profileConfiguration)
            CcuLog.i(Domain.LOG_TAG, "Default profile config Loaded")
        }
        modelLoadedState.postValue(true)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavStagedRtu")
    }

    override fun saveConfiguration() {
        hayStack.resetCcuReady()
        var systemEquipId : String? = null
        ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
        viewModelScope.launch (highPriorityDispatcher) {
            val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
            if (systemEquip["profile"].toString() != "vavStagedRtu" &&
                systemEquip["profile"].toString() != ProfileType.SYSTEM_VAV_STAGED_RTU.name) {
                viewState.value.updateConfigFromViewState(profileConfiguration)
                deleteSystemProfile(systemEquip["id"].toString())
                systemEquipId = createNewEquip(systemEquip["id"].toString())
                L.ccu().systemProfile = VavStagedRtu()
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
            }else{
                super.saveConfiguration()
            }
            withContext(Dispatchers.Main) {
                ProgressDialogUtils.hideProgressDialog()
            }
            val stagedRtu = L.ccu().systemProfile as VavStagedRtu
            stagedRtu.updateStagesSelected()
            DesiredTempDisplayMode.setSystemModeForVav(hayStack)
            updateSystemMode()
            systemEquipId?.let { L.ccu().systemProfile.updateAhuRef(it) }
            updateOaoPoints()
            hayStack.syncEntityTree()
            hayStack.setCcuReady()
        }
    }

    fun reset() {
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
        if (systemEquip["profile"].toString() == "vavStagedRtu" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_STAGED_RTU.name) {
            profileConfiguration = StagedRtuProfileConfig(model).getActiveConfiguration()
            val stagedRtu = L.ccu().systemProfile as VavStagedRtu
            stagedRtu.updateStagesSelected()
            CcuLog.i(Domain.LOG_TAG, "Active profile config Loaded")
        }else{
            profileConfiguration = StagedRtuProfileConfig(model).getDefaultConfiguration()
            CcuLog.i(Domain.LOG_TAG, "Default profile config Loaded")
        }
        viewState.value = StagedRtuViewState.fromProfileConfig(profileConfiguration)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavStagedRtu")
    }
}