package a75f.io.renatus.profiles.system.advancedahu.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuRelayAssociationType
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.building.system.vav.config.AdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.vav.config.VavAdvancedHybridAhuConfig
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuViewModel
import a75f.io.renatus.profiles.system.advancedahu.isAnyAOMapped
import a75f.io.renatus.profiles.system.advancedahu.isAnyRelayMapped
import a75f.io.renatus.profiles.system.advancedahu.isValidateConfiguration
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import io.seventyfivef.ph.core.Tags
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
        super.init(context, ModelLoader.getVavAdvancedAhuCmModelV2(), ModelLoader.getVavAdvancedAhuConnectModelV2(), hayStack)
        CcuLog.i(Domain.LOG_TAG, "VavAdvancedAhuViewModel Init")
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule") //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        profileConfiguration = if (systemEquip["profile"].toString().contentEquals("vavAdvancedHybridAhuV2")) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for systemEquip")
            VavAdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration() as VavAdvancedHybridAhuConfig
        } else {
            CcuLog.i(Domain.LOG_TAG, "Get default config for systemEquip")
            VavAdvancedHybridAhuConfig(cmModel, connectModel)
        }
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.cmConfiguration.toString())
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.connectConfiguration.toString())
        viewState = mutableStateOf(VavAdvancedAhuState.fromProfileConfigToState(profileConfiguration as VavAdvancedHybridAhuConfig))
        CcuLog.i(Domain.LOG_TAG, "VavAdvancedAhuViewModel Loaded")
    }

    private fun createNewEquip(id: String): String {
        hayStack.deleteEntityTree(id)
        val cmEquipDis = "${hayStack.siteName}-${cmModel.name}"
        val cmEquipId = cmEquipBuilder.buildEquipAndPoints(
            profileConfiguration.cmConfiguration, cmModel, hayStack.site!!.id, cmEquipDis
        )

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }
        val cmDeviceDis = hayStack.siteName + "-" + cmDeviceModel.name
        CcuLog.i(Domain.LOG_TAG, " CM buildDeviceAndPoints")
        cmDeviceBuilder.buildDeviceAndPoints(
            profileConfiguration.cmConfiguration, cmDeviceModel, cmEquipId, hayStack.site!!.id, cmDeviceDis
        )

        val existingConnectEquip = hayStack.readEntity("domainName == \"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\"")
        if (existingConnectEquip.isNotEmpty()) {
            hayStack.deleteEntityTree(existingConnectEquip["id"].toString())
        }

        if (profileConfiguration.connectConfiguration.connectEnabled) {
            val connectEquipDis = "${hayStack.siteName}-${connectModel.name}"
            val connectEquipId = connectEquipBuilder.buildEquipAndPoints(
                profileConfiguration.connectConfiguration, connectModel, hayStack.site!!.id, connectEquipDis
            )

            val connectDevice = hayStack.readEntity("domainName == \"" + DomainName.connectModuleDevice + "\"")
            if (connectDevice.isNotEmpty()) {
                hayStack.deleteEntityTree(connectDevice["id"].toString())
            }
            val connectDeviceDis = hayStack.siteName + "-" + connectDeviceModel.name
            CcuLog.i(Domain.LOG_TAG, " ConnectModule buildDeviceAndPoints")
            connectDeviceBuilder.buildDeviceAndPoints(
                profileConfiguration.connectConfiguration, connectDeviceModel, connectEquipId, hayStack.site!!.id, connectDeviceDis
            )
        }

        hayStack.syncEntityTree()
        DomainManager.addSystemDomainEquip(hayStack)
        DomainManager.addCmBoardDevice(hayStack)
        return cmEquipId
    }

    override fun saveConfiguration() {
        ((viewState.value) as VavAdvancedAhuState).fromStateToProfileConfig(profileConfiguration as VavAdvancedHybridAhuConfig)
        val validConfig = isValidateConfiguration(this@VavAdvancedHybridAhuViewModel)
        if (!validConfig.first) {
            showErrorDialog(context,validConfig.second)
            return
        }
        CcuLog.i(L.TAG_CCU_SYSTEM, profileConfiguration.toString())
        isEquipAvailable()
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving profile configuration")
            withContext(Dispatchers.IO) {
                val profile = L.ccu().systemProfile
                if (profile == null) {
                    newEquipConfiguration()
                } else {
                    if (profile is VavAdvancedAhu) {
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
                (L.ccu().systemProfile as VavAdvancedAhu).updateDomainEquip(Domain.systemEquip as VavAdvancedHybridSystemEquip)
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
        L.ccu().systemProfile = VavAdvancedAhu()
        L.ccu().systemProfile.addSystemEquip()
        L.ccu().systemProfile.updateAhuRef(newEquipId)
        val vavAdvancedAhuProfile = L.ccu().systemProfile as VavAdvancedAhu
        vavAdvancedAhuProfile.updateStagesSelected()
    }

    private fun updateConfiguration() {
        cmEquipBuilder.updateEquipAndPoints(
            profileConfiguration.cmConfiguration,
            cmModel,
            CCUHsApi.getInstance().site!!.id,
            CCUHsApi.getInstance().siteName + "-" + cmModel.name,
            isReconfiguration = true)

        val connectDevices = hayStack.readAllEntities("domainName == \"" + DomainName.connectModuleDevice + "\"")
        connectDevices.forEach { connectDevice ->
            if (connectDevice.containsKey(Tags.ID) && connectDevice[Tags.ID] != null) {
                hayStack.deleteEntityTree(connectDevice[Tags.ID].toString())
            }
        }

        if (profileConfiguration.connectConfiguration.connectEnabled) {

            val existingConnectEquip = hayStack.readEntity("domainName == \"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\"")
            if (existingConnectEquip.isNotEmpty()) {
                connectEquipBuilder.updateEquipAndPoints(
                    profileConfiguration.connectConfiguration,
                    connectModel,
                    CCUHsApi.getInstance().site!!.id,
                    CCUHsApi.getInstance().siteName + "-" + connectModel.name,
                    isReconfiguration = true
                )
            } else {
                val connectEquipDis = "${hayStack.siteName}-${connectModel.name}"
                connectEquipBuilder.buildEquipAndPoints(
                    profileConfiguration.connectConfiguration, connectModel, hayStack.site!!.id, connectEquipDis
                )
            }

            val connectEquip = hayStack.readEntity("domainName == \"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\"")
            val connectEquipId = if (connectEquip.isNotEmpty()) connectEquip[Tags.ID].toString() else ""

            val connectDeviceDis = hayStack.siteName + "-" + connectDeviceModel.name
            CcuLog.i(Domain.LOG_TAG, " ConnectModule buildDeviceAndPoints")
            connectDeviceBuilder.buildDeviceAndPoints(
                profileConfiguration.connectConfiguration, connectDeviceModel, connectEquipId, hayStack.site!!.id, connectDeviceDis
            )
        } else {
            val connectEquip = hayStack.readEntity("domainName == \"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\"")
            if (connectEquip.isNotEmpty()) {
                hayStack.deleteEntityTree(connectEquip[Tags.ID].toString())
            }

            val connectDevice = hayStack.readEntity("domainName == \"" + DomainName.connectModuleDevice + "\"")
            if (connectDevice.isNotEmpty()) {
                hayStack.deleteEntityTree(connectDevice[Tags.ID].toString())
            }
        }
    }
}


