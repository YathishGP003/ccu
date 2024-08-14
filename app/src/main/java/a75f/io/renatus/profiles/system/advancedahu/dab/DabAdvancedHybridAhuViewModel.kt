package a75f.io.renatus.profiles.system.advancedahu.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelNames
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.dab.config.DabAdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.deleteCurrentSystemProfile
import a75f.io.logic.bo.building.system.util.getCurrentSystemEquip
import a75f.io.logic.bo.building.system.util.getDabConnectEquip
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuViewModel
import a75f.io.renatus.profiles.system.advancedahu.isValidateConfiguration
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 19-05-2024.
 */

class DabAdvancedHybridAhuViewModel : AdvancedHybridAhuViewModel() {

    init {
        viewState = mutableStateOf(DabAdvancedAhuState())
    }
    fun init(context: Context, hayStack: CCUHsApi) {
        super.init(context, ModelLoader.getDabAdvancedAhuCmModelV2(), ModelLoader.getDabAdvancedAhuConnectModelV2(), hayStack, ProfileType.SYSTEM_DAB_ADVANCED_AHU)
        val systemEquip = getCurrentSystemEquip()
        profileConfiguration = if (systemEquip["profile"].toString().contentEquals("dabAdvancedHybridAhuV2")) {
            DabAdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration() as DabAdvancedHybridAhuConfig
        } else {
            DabAdvancedHybridAhuConfig(cmModel, connectModel)
        }
        viewState = mutableStateOf(DabAdvancedAhuState.fromProfileConfigToState(profileConfiguration as DabAdvancedHybridAhuConfig))
    }


    override fun saveConfiguration() {
        ((viewState.value) as DabAdvancedAhuState).fromStateToProfileConfig(profileConfiguration as DabAdvancedHybridAhuConfig)
        val validConfig = isValidateConfiguration(this@DabAdvancedHybridAhuViewModel)
        if (!validConfig.first) {
            showErrorDialog(context,validConfig.second)
            viewState.value.isSaveRequired = true
            viewState.value.isStateChanged = true
            return
        }
        viewState.value.isSaveRequired = false
        viewState.value.isStateChanged = false

        isEquipAvailable(ProfileType.SYSTEM_DAB_ADVANCED_AHU)
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving profile configuration")
            saveJob = viewModelScope.launch (highPriorityDispatcher) {
                hayStack.resetCcuReady()
                val profile = L.ccu().systemProfile
                if (profile == null) {
                    newEquipConfiguration()
                } else {
                    if (profile is DabAdvancedAhu) {
                        updateConfiguration(getDabConnectEquip(), ModelNames.dabAdvancedHybridAhuV2_connectModule)
                    } else {
                        newEquipConfiguration()
                    }
                }
                (L.ccu().systemProfile as DabAdvancedAhu).updateDomainEquip(Domain.systemEquip as DabAdvancedHybridSystemEquip)
                withContext(Dispatchers.Main) {
                    if (ProgressDialogUtils.isDialogShowing()) {
                        ProgressDialogUtils.hideProgressDialog()
                    }
                }
                L.saveCCUState()
                viewState.value.isSaveRequired = false
                viewState.value.isStateChanged = false
                saveJob = null
                showToast("Configuration saved successfully", context)
                CCUHsApi.getInstance().setCcuReady()
                CCUHsApi.getInstance().syncEntityTree()
            }
        }
    }

    private suspend fun newEquipConfiguration() {
        val deleteJob = viewModelScope.launch(Dispatchers.IO) {
            deleteCurrentSystemProfile()
        }
        deleteJob.join()
        val newEquipJob = viewModelScope.launch(Dispatchers.IO) {
            launch {
                addConnectModule()
            }
            val newEquipId = addAdvanceAHUEquip()
            L.ccu().systemProfile = DabAdvancedAhu()
            L.ccu().systemProfile.addSystemEquip()
            L.ccu().systemProfile.updateAhuRef(newEquipId)
            val dabAdvancedAhuProfile = L.ccu().systemProfile as DabAdvancedAhu
            dabAdvancedAhuProfile.updateStagesSelected()
            launch { L.ccu().systemProfile.removeSystemEquipModbus() }
        }
        newEquipJob.join()
    }

    override fun reset() {
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
        profileConfiguration = if (systemEquip["profile"].toString().contentEquals("dabAdvancedHybridAhuV2")) {
            DabAdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration() as DabAdvancedHybridAhuConfig
        } else {
            DabAdvancedHybridAhuConfig(cmModel, connectModel)
        }
        viewState.value = DabAdvancedAhuState.fromProfileConfigToState(profileConfiguration as DabAdvancedHybridAhuConfig)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("dabAdvancedHybridAhuV2")
    }
}