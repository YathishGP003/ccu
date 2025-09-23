package a75f.io.renatus.profiles.system.advancedahu.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.domain.logic.hasChanges
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.setFanTypeToStages
import a75f.io.logic.bo.building.system.util.deleteCurrentSystemProfile
import a75f.io.logic.bo.building.system.util.deleteSystemConnectModule
import a75f.io.logic.bo.building.system.util.getCurrentSystemEquip
import a75f.io.logic.bo.building.system.util.getVavConnectEquip
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.building.system.vav.config.VavAdvancedHybridAhuConfig
import a75f.io.renatus.BackgroundServiceInitiator
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
 * Created by Manjunath K on 20-03-2024.
 */

class VavAdvancedHybridAhuViewModel : AdvancedHybridAhuViewModel() {
    init {
        viewState = mutableStateOf(VavAdvancedAhuState())
    }

    fun init(context: Context, hayStack: CCUHsApi) {
        super.init(context, ModelLoader.getVavAdvancedAhuCmModelV2(), ModelLoader.getVavAdvancedAhuConnectModelV2(), hayStack, ProfileType.SYSTEM_VAV_ADVANCED_AHU)
        val systemEquip = getCurrentSystemEquip()
        profileConfiguration = if (systemEquip["profile"].toString().contentEquals("vavAdvancedHybridAhuV2")) {
            VavAdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration() as VavAdvancedHybridAhuConfig
        } else {
            VavAdvancedHybridAhuConfig(cmModel, connectModel)
        }
        viewState = mutableStateOf(VavAdvancedAhuState.fromProfileConfigToState(profileConfiguration as VavAdvancedHybridAhuConfig))
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavAdvancedHybridAhuV2")
        modelLoadedState.postValue(true)
    }

    override fun saveConfiguration() {
        ((viewState.value) as VavAdvancedAhuState).fromStateToProfileConfig(profileConfiguration as VavAdvancedHybridAhuConfig)
        val isAnalogOutMappedToOaoDamper = isAnalogOutMappedToOaoDamper()
        val validConfig = isValidateConfiguration(profileConfiguration, isAnalogOutMappedToOaoDamper)
        if (!validConfig.first) {
            showErrorDialog(context,validConfig.second)
            viewState.value.isSaveRequired = true
            viewState.value.isStateChanged = true
            return
        }
        viewState.value.isSaveRequired = false
        viewState.value.isStateChanged = false

        isEquipPaired = true
        isConnectModulePaired = viewState.value.isConnectEnabled
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving profile configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                hayStack.resetCcuReady()
                val profile = L.ccu().systemProfile
                if (profile == null) {
                    newEquipConfiguration()
                } else {
                    if (profile is VavAdvancedAhu) {
                        //resetting the epidemic mode points if the user pairing the OAO / removed the OAO in connect Module
                        if (isOaoPairedFirstTimeOrOaoRemoved()) {
                            resettingEpidemicModePoints()
                            CcuLog.i(
                                Domain.LOG_TAG,
                                "OAO newly paired/removed in connectModule, reset the epidemicMode points"
                            )
                        } else {
                            CcuLog.i(Domain.LOG_TAG, "OAO already paired/removed in connectModule")
                        }
                        updateConfiguration(getVavConnectEquip(), ModelNames.vavAdvancedHybridAhuV2_connectModule)
                    } else {
                        newEquipConfiguration()
                    }
                }
                (L.ccu().systemProfile as VavAdvancedAhu).apply {
                    updateDomainEquip(Domain.systemEquip as VavAdvancedHybridSystemEquip)
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
                    // Initialize background services after saving configuration
                    context?.let { BackgroundServiceInitiator.initializeServices(it.applicationContext) }
                }
                updateConditioningMode()
                hayStack.setCcuReady()
                hayStack.syncEntityTree()
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
                deleteSystemConnectModule()
                addConnectModule()
            }
            val newEquipId = addAdvanceAHUEquip()
            L.ccu().systemProfile = VavAdvancedAhu()
            L.ccu().systemProfile.addSystemEquip()
            L.ccu().systemProfile.updateAhuRef(newEquipId)
            val vavAdvancedAhuProfile = L.ccu().systemProfile as VavAdvancedAhu
            vavAdvancedAhuProfile.updateStagesSelected()
            launch {
                L.ccu().systemProfile.removeSystemEquipModbus()
                L.ccu().systemProfile.removeSystemEquipBacnet()
            }
            enableDisableCoolingLockOut(false, profileConfiguration)
        }
        newEquipJob.join()
    }


    override fun reset() {
        val systemEquip = getCurrentSystemEquip()
        profileConfiguration = if (systemEquip["profile"].toString().contentEquals("vavAdvancedHybridAhuV2")) {
            VavAdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration() as VavAdvancedHybridAhuConfig
        } else {
            VavAdvancedHybridAhuConfig(cmModel, connectModel)
        }
        viewState.value = VavAdvancedAhuState.fromProfileConfigToState(profileConfiguration as VavAdvancedHybridAhuConfig)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavAdvancedHybridAhuV2")
    }

    fun hasUnsavedChanges(): Boolean {
        return try {
            val newConfiguration = VavAdvancedHybridAhuConfig(cmModel, connectModel)
            ((viewState.value) as VavAdvancedAhuState).fromStateToProfileConfig(newConfiguration)
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


