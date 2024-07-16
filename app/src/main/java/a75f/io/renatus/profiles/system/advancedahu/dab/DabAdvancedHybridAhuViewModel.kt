package a75f.io.renatus.profiles.system.advancedahu.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.dab.config.DabAdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.deleteSystemConnectModule
import a75f.io.logic.bo.building.system.util.getDabConnectEquip
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuViewModel
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import io.seventyfivef.ph.core.Tags
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
        CcuLog.i(Domain.LOG_TAG, "DabAdvancedAhuViewModel Init")
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule") //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        profileConfiguration = if (systemEquip["profile"].toString().contentEquals("dabAdvancedHybridAhuV2_cmBoard")) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for systemEquip")
            DabAdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration() as DabAdvancedHybridAhuConfig
        } else {
            CcuLog.i(Domain.LOG_TAG, "Get default config for systemEquip")
            DabAdvancedHybridAhuConfig(cmModel, connectModel)
        }
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.cmConfiguration.toString())
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.connectConfiguration.toString())
        viewState = mutableStateOf(DabAdvancedAhuState.fromProfileConfigToState(profileConfiguration as DabAdvancedHybridAhuConfig))
        CcuLog.i(Domain.LOG_TAG, "DabAdvancedAhuViewModel Loaded")
    }

    private fun createNewEquip(id: String): String {
        hayStack.deleteEntityTree(id)
        val cmEquipDis = "${hayStack.siteName}-${cmModel.name}"
        val cmEquipId = cmEquipBuilder.buildEquipAndPoints(profileConfiguration.cmConfiguration, cmModel, hayStack.site!!.id, cmEquipDis)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }
        val cmDeviceDis = hayStack.siteName + "-" + cmDeviceModel.name
        CcuLog.i(Domain.LOG_TAG, " CM buildDeviceAndPoints")
        cmDeviceBuilder.buildDeviceAndPoints(profileConfiguration.cmConfiguration, cmDeviceModel, cmEquipId, hayStack.site!!.id, cmDeviceDis)
        deleteSystemConnectModule()
        if (profileConfiguration.connectConfiguration.connectEnabled) {
            val connectEquipDis = "${hayStack.siteName}-${connectModel.name}"
            val connectEquipId = connectEquipBuilder.buildEquipAndPoints(profileConfiguration.connectConfiguration, connectModel, hayStack.site!!.id, connectEquipDis)

            val connectDevice = hayStack.readEntity("domainName == \"" + DomainName.connectModuleDevice + "\"")
            if (connectDevice.isNotEmpty()) {
                hayStack.deleteEntityTree(connectDevice["id"].toString())
            }
            val connectDeviceDis = hayStack.siteName + "-" + connectDeviceModel.name
            CcuLog.i(Domain.LOG_TAG, " ConnectModule buildDeviceAndPoints")
            connectDeviceBuilder.buildDeviceAndPoints(profileConfiguration.connectConfiguration, connectDeviceModel, connectEquipId, hayStack.site!!.id, connectDeviceDis)
        }

        hayStack.syncEntityTree()
        DomainManager.addSystemDomainEquip(hayStack)
        DomainManager.addCmBoardDevice(hayStack)
        return cmEquipId
    }

    override fun saveConfiguration() {
        ((viewState.value) as DabAdvancedAhuState).fromStateToProfileConfig(profileConfiguration as DabAdvancedHybridAhuConfig)
        CcuLog.i(L.TAG_CCU_SYSTEM, profileConfiguration.toString())
        isEquipAvailable(ProfileType.SYSTEM_DAB_ADVANCED_AHU)
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving profile configuration")
            withContext(Dispatchers.IO) {
                val profile = L.ccu().systemProfile
                if (profile == null) {
                    newEquipConfiguration()
                } else {
                    if (profile is DabAdvancedAhu) {
                        updateConfiguration()
                    } else {
                        newEquipConfiguration()
                    }
                }
                L.saveCCUState()
                CCUHsApi.getInstance().setCcuReady()
                CCUHsApi.getInstance().syncEntityTree()
                DomainManager.addSystemDomainEquip(hayStack)
                DomainManager.addCmBoardDevice(hayStack)
                (L.ccu().systemProfile as DabAdvancedAhu).updateDomainEquip(Domain.systemEquip as DabAdvancedHybridSystemEquip)
                withContext(Dispatchers.Main) {
                    showToast("Configuration saved successfully", context)
                    ProgressDialogUtils.hideProgressDialog()
                }
            }
        }

    }

    private fun newEquipConfiguration() {
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
        val newEquipId = createNewEquip(systemEquip[Tags.ID].toString())
        L.ccu().systemProfile = DabAdvancedAhu()
        L.ccu().systemProfile.addSystemEquip()
        L.ccu().systemProfile.updateAhuRef(newEquipId)
        val dabAdvancedAhuProfile = L.ccu().systemProfile as DabAdvancedAhu
         dabAdvancedAhuProfile.updateStagesSelected()
    }

    private fun updateConfiguration() {
        cmEquipBuilder.updateEquipAndPoints(profileConfiguration.cmConfiguration, cmModel, CCUHsApi.getInstance().site!!.id, CCUHsApi.getInstance().siteName + "-" + cmModel.name, isReconfiguration = true)

        val connectDevices = hayStack.readAllEntities("domainName == \"" + DomainName.connectModuleDevice + "\"")
        connectDevices.forEach { connectDevice ->
            if (connectDevice.containsKey(Tags.ID) && connectDevice[Tags.ID] != null) {
                hayStack.deleteEntityTree(connectDevice[Tags.ID].toString())
            }
        }

        if (profileConfiguration.connectConfiguration.connectEnabled) {

            val existingConnectEquip = getDabConnectEquip()
            if (existingConnectEquip.isNotEmpty()) {
                connectEquipBuilder.updateEquipAndPoints(profileConfiguration.connectConfiguration, connectModel, CCUHsApi.getInstance().site!!.id, CCUHsApi.getInstance().siteName + "-" + connectModel.name, isReconfiguration = true)
            } else {
                val connectEquipDis = "${hayStack.siteName}-${connectModel.name}"
                connectEquipBuilder.buildEquipAndPoints(profileConfiguration.connectConfiguration, connectModel, hayStack.site!!.id, connectEquipDis)
            }

            val connectEquip = hayStack.readEntity("domainName == \"" + DomainName.dabAdvancedHybridAhuV2_connectModule + "\"")
            val connectEquipId = if (connectEquip.isNotEmpty()) connectEquip[Tags.ID].toString() else ""

            val connectDeviceDis = hayStack.siteName + "-" + connectDeviceModel.name
            CcuLog.i(Domain.LOG_TAG, " ConnectModule buildDeviceAndPoints")
            connectDeviceBuilder.buildDeviceAndPoints(profileConfiguration.connectConfiguration, connectDeviceModel, connectEquipId, hayStack.site!!.id, connectDeviceDis)
        } else {
            deleteSystemConnectModule(ModelNames.dabAdvancedHybridAhuV2_connectModule)
        }
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