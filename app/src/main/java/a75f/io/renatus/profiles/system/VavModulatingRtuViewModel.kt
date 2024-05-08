package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.DeviceUtil
import a75f.io.device.mesh.MeshUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class VavModulatingRtuViewModel : ModulatingRtuViewModel() {

    fun init(context: Context, hayStack: CCUHsApi) {
        super.init(context, ModelLoader.getVavModulatingRtuModelDef(), hayStack)

        var systemEquip = hayStack.readEntity("system and equip and not modbus")
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        if (systemEquip["profile"].toString() == "vavFullyModulatingAhu"
            || systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_ANALOG_RTU.name
        ) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for vavFullyModulatingAhu")
            profileConfiguration = ModulatingRtuProfileConfig(model).getActiveConfiguration()
        } else {
            CcuLog.i(Domain.LOG_TAG, "Get default config for vavFullyModulatingAhu")
            profileConfiguration = ModulatingRtuProfileConfig(model).getDefaultConfiguration()
            val newEquipId = createNewEquip(systemEquip["id"].toString())
            L.ccu().systemProfile = VavFullyModulatingRtu()
            L.ccu().systemProfile.addSystemEquip()
            L.ccu().systemProfile.updateAhuRef(newEquipId)
        }
        if(L.ccu().systemProfile.sysEquip == null){
            L.ccu().systemProfile = VavFullyModulatingRtu()
            L.ccu().systemProfile.addSystemEquip()
        }
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        viewState = ModulatingRtuViewState.fromProfileConfig(profileConfiguration)
        CcuLog.i(Domain.LOG_TAG, "VavModulatingRtuViewModel Loaded")
        modelLoaded = true
    }

    override fun saveConfiguration() {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration...")
            withContext(Dispatchers.IO) {
                super.saveConfiguration()
                DesiredTempDisplayMode.setSystemModeForVav(hayStack)
                hayStack.syncEntityTree()
                withContext(Dispatchers.Main) {
                    ProgressDialogUtils.hideProgressDialog()
                    updateSystemMode()
                }
            }
        }
    }

    override fun sendAnalogRelayTestSignal(tag: String, value: Double) {
        val systemProfile = L.ccu().systemProfile as VavFullyModulatingRtu
        Globals.getInstance().setTestMode(true)
        if (tag.contains("analog")) {
            when(tag){
                DomainName.analog1Out ->
                    Domain.cmBoardDevice.analog1Out.writeHisVal(
                        DeviceUtil.getModulatedAnalogVal(
                        systemProfile.systemEquip.analog1MinCooling.readPriorityVal(),
                        systemProfile.systemEquip.analog1MaxCooling.readPriorityVal(),
                        value).toDouble())
                DomainName.analog2Out ->
                    Domain.cmBoardDevice.analog2Out.writeHisVal(
                        DeviceUtil.getModulatedAnalogVal(
                        systemProfile.systemEquip.analog2MinStaticPressure.readPriorityVal(),
                        systemProfile.systemEquip.analog2MaxStaticPressure.readPriorityVal(),
                        value).toDouble())
                DomainName.analog3Out ->
                    Domain.cmBoardDevice.analog3Out.writeHisVal(
                        DeviceUtil.getModulatedAnalogVal(
                        systemProfile.systemEquip.analog3MinHeating.readPriorityVal(),
                        systemProfile.systemEquip.analog3MaxHeating.readPriorityVal(),
                        value
                    ).toDouble())
                DomainName.analog4Out ->
                    Domain.cmBoardDevice.analog4Out.writeHisVal(
                        DeviceUtil.getModulatedAnalogVal(
                        systemProfile.systemEquip.analog4MinOutsideDamper.readPriorityVal(),
                        systemProfile.systemEquip.analog4MaxOutsideDamper.readPriorityVal(),
                        value).toDouble())
            }
        } else if (tag.contains("relay")) {
            ControlMote.setRelayState(tag, value)
        }
        MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage())
    }
}