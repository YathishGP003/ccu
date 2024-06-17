package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VavStagedVfdRtuViewModel : StagedRtuProfileViewModel() {
    fun init(context: Context, hayStack : CCUHsApi) {
        super.init(context, ModelLoader.getVavStagedVfdRtuModelDef(), hayStack)

        CcuLog.i(Domain.LOG_TAG, "VavStagedVfdRtuViewModel Init")

        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule") //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        if (systemEquip["profile"].toString() == "vavStagedRtuVfdFan" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for systemEquip")
            profileConfiguration = StagedVfdRtuProfileConfig(model).getActiveConfiguration()
        } else {
            CcuLog.i(Domain.LOG_TAG, "Get default config for systemEquip")
            profileConfiguration = StagedVfdRtuProfileConfig(model).getDefaultConfiguration()
            val newEquipId = createNewEquip(systemEquip["id"].toString())
            L.ccu().systemProfile = VavStagedRtuWithVfd()
            L.ccu().systemProfile.removeSystemEquipModbus()
            L.ccu().systemProfile.addSystemEquip()
            L.ccu().systemProfile.updateAhuRef(newEquipId)
        }
        val stagedRtu = L.ccu().systemProfile as VavStagedRtuWithVfd
        stagedRtu.updateStagesSelected()
        val vfdProfileConfig = profileConfiguration as StagedVfdRtuProfileConfig
        CcuLog.i(Domain.LOG_TAG, vfdProfileConfig.toString())
        viewState = StagedRtuVfdViewState.fromProfileConfig(vfdProfileConfig)
        CcuLog.i(Domain.LOG_TAG, "VavStagedRtuViewModel Loaded")
        viewState.unusedPortState = ControlMote.getCMUnusedPorts(Domain.hayStack)
        modelLoaded = true
    }
    override fun saveConfiguration() {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
            withContext(Dispatchers.IO) {
                val vfdViewState = viewState as StagedRtuVfdViewState
                val vfdConfig = profileConfiguration as StagedVfdRtuProfileConfig
                vfdViewState.updateConfigFromViewState(vfdConfig)
                val equipDis = "${hayStack.siteName}-${model.name}"
                equipBuilder.updateEquipAndPoints(vfdConfig, model, hayStack.site!!.id, equipDis, isReconfiguration = true)
                hayStack.syncEntityTree()

                deviceBuilder.updateDeviceAndPoints(
                    vfdConfig,
                    deviceModel,
                    Domain.systemEquip.equipRef,
                    hayStack.site!!.id,
                    hayStack.siteName +"-"+ deviceModel.name
                )
                DomainManager.addSystemDomainEquip(hayStack)
                val stagedRtu = L.ccu().systemProfile as VavStagedRtuWithVfd
                stagedRtu.updateStagesSelected()
                DesiredTempDisplayMode.setSystemModeForVav(hayStack)
                UnusedPortsModel.saveUnUsedPortStatusOfSystemProfile(profileConfiguration, hayStack)
                viewState.unusedPortState = ControlMote.getCMUnusedPorts(Domain.hayStack)

                hayStack.syncEntityTree()
                withContext(Dispatchers.Main) {
                    ProgressDialogUtils.hideProgressDialog()
                    updateSystemMode()
                }
            }
        }
    }
}