package a75f.io.renatus.profiles.system.advancedahu.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.logic.hasChanges
import a75f.io.domain.util.CommonQueries
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.dab.config.DabAdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.setFanTypeToStages
import a75f.io.logic.bo.building.system.util.deleteCurrentSystemProfile
import a75f.io.logic.bo.building.system.util.getCurrentSystemEquip
import a75f.io.logic.bo.building.system.util.getDabConnectEquip
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.oao.updateOaoPoints
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuViewModel
import a75f.io.renatus.profiles.system.advancedahu.isValidateConfiguration
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.renatus.util.showErrorDialog
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
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("dabAdvancedHybridAhuV2")
        modelLoadedState.postValue(true)
    }


    override fun saveConfiguration() {
        ((viewState.value) as DabAdvancedAhuState).fromStateToProfileConfig(profileConfiguration as DabAdvancedHybridAhuConfig)
        val isAnalogOutMappedToOaoDamper = isAnalogOutMappedToOaoDamper()
        val validConfig = isValidateConfiguration(
            profileConfiguration,
            isAnalogOutMappedToOaoDamper
        )
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
                        //resetting the epidemic mode points if the user pairing the OAO / removed the OAO in connect Module
                        if (isOaoPairedFirstTimeOrOaoRemoved()) {
                            resettingEpidemicModePoints()
                            CcuLog.i(
                                Domain.LOG_TAG,
                                "OAO newly paired/removed in connectModule reset the epidemicMode points"
                            )
                        } else {
                            CcuLog.i(Domain.LOG_TAG, "OAO already paired/removed in connectModule")
                        }
                        updateConfiguration(getDabConnectEquip(), ModelNames.dabAdvancedHybridAhuV2_connectModule)
                    } else {
                        newEquipConfiguration()
                    }
                }
                (L.ccu().systemProfile as DabAdvancedAhu).apply {
                    updateDomainEquip(Domain.systemEquip as DabAdvancedHybridSystemEquip)
                    updateStagesSelected()
                }
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
                updateOaoPoints()
                if (profileConfiguration.connectConfiguration.connectEnabled) {
                    updateAhuRefForConnectModule()
                    // Send seed message when connect module is added
                    LSerial.getInstance().setResetSeedMessage(true)
                }
                updateConditioningMode()
                hayStack.syncEntityTree()
                hayStack.setCcuReady()
                setFanTypeToStages(
                    profileConfiguration.cmConfiguration,
                    profileConfiguration.cmConfiguration,
                    profileConfiguration.connectConfiguration
                )
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
            launch {
                L.ccu().systemProfile.removeSystemEquipModbus()
                L.ccu().systemProfile.removeSystemEquipBacnet()
            }
            enableDisableCoolingLockOut(false, profileConfiguration)
        }
        newEquipJob.join()
    }

    override fun reset() {
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        profileConfiguration = if (systemEquip["profile"].toString().contentEquals("dabAdvancedHybridAhuV2")) {
            DabAdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration() as DabAdvancedHybridAhuConfig
        } else {
            DabAdvancedHybridAhuConfig(cmModel, connectModel)
        }
        viewState.value = DabAdvancedAhuState.fromProfileConfigToState(profileConfiguration as DabAdvancedHybridAhuConfig)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("dabAdvancedHybridAhuV2")
    }

    fun hasUnsavedChanges(): Boolean {
        return try {
            val newConfiguration = DabAdvancedHybridAhuConfig(cmModel, connectModel)
            ((viewState.value) as DabAdvancedAhuState).fromStateToProfileConfig(newConfiguration)
            return hasChanges(
                profileConfiguration.cmConfiguration,
                newConfiguration.cmConfiguration
            ) || hasChanges(
                profileConfiguration.connectConfiguration,
                newConfiguration.connectConfiguration
            )
        } catch (e: Exception) {
            false
        }
    }
}