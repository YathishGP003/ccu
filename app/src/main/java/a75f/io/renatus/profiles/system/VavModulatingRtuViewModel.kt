package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.device.mesh.DeviceUtil
import a75f.io.device.mesh.MeshUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.hasChanges
import a75f.io.domain.util.CommonQueries
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.profiles.oao.updateOaoPoints
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.TestSignalManager
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class VavModulatingRtuViewModel : ModulatingRtuViewModel() {

    fun init(context: Context, hayStack: CCUHsApi) {
        super.init(context, ModelLoader.getVavModulatingRtuModelDef(), hayStack)
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        if (systemEquip["profile"].toString() == "vavFullyModulatingAhu"
            || systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_ANALOG_RTU.name
        ) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for vavFullyModulatingAhu")
            profileConfiguration = ModulatingRtuProfileConfig(model).getActiveConfiguration()
            viewState.value = ModulatingRtuViewState.fromProfileConfig(profileConfiguration)
            CcuLog.i(Domain.LOG_TAG, "Active vavFullyModulatingAhu profile config loaded")
        } else {
            profileConfiguration = ModulatingRtuProfileConfig(model).getDefaultConfiguration()
            viewState.value = ModulatingRtuViewState.fromProfileConfig(profileConfiguration)
            CcuLog.i(Domain.LOG_TAG, "Default vavFullyModulatingAhu profile config Loaded")
        }
        initialPortValues = HashMap(profileConfiguration.unusedPorts)
        modelLoadedState.postValue(true)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavFullyModulatingAhu")
    }

    override fun saveConfiguration() {
        hayStack.resetCcuReady()
        var systemEquipId : String? = null
        ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
        viewModelScope.launch (highPriorityDispatcher) {
            val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
            if (systemEquip["profile"].toString() != "vavFullyModulatingAhu"
                && systemEquip["profile"].toString() != ProfileType.SYSTEM_VAV_ANALOG_RTU.name
            ) {
                viewState.value.updateConfigFromViewState(profileConfiguration)
                CcuLog.d(Tags.ADD_REMOVE_PROFILE, "VavModulatingRtuViewModel removing profile with it -->${systemEquip["id"].toString()}")
                deleteSystemProfile(systemEquip["id"].toString())
                systemEquipId = createNewEquip(systemEquip["id"].toString())
                L.ccu().systemProfile!!.deleteSystemEquip()
                L.ccu().systemProfile = VavFullyModulatingRtu()
                L.ccu().systemProfile.removeSystemEquipModbus()
                L.ccu().systemProfile.removeSystemEquipBacnet()
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


            DesiredTempDisplayMode.setSystemModeForVav(hayStack)
            updateSystemMode()
            systemEquipId?.let {
                L.ccu().systemProfile.updateAhuRef(it)
            }
            updateOaoPoints()
            hayStack.syncEntityTree()
            hayStack.setCcuReady()
            initialPortValues = HashMap(profileConfiguration.unusedPorts)
        }
    }

    override fun sendAnalogRelayTestSignal(tag: String, value: Double) {
        if(L.ccu().systemProfile.profileName == ProfileName) {
            val systemProfile = L.ccu().systemProfile as VavFullyModulatingRtu
            Globals.getInstance().isTestMode = true
            if (tag.contains("analog")) {
                when (tag) {
                    DomainName.analog1Out ->{
                        TestSignalManager.backUpPoint(Domain.cmBoardDevice.analog1Out)
                        Domain.cmBoardDevice.analog1Out.writePointValue(
                            DeviceUtil.getModulatedAnalogVal(
                                systemProfile.systemEquip.analog1MinCooling.readPriorityVal(),
                                systemProfile.systemEquip.analog1MaxCooling.readPriorityVal(),
                                value
                            ).toDouble())
                    }
                    DomainName.analog2Out -> {
                        TestSignalManager.backUpPoint(Domain.cmBoardDevice.analog2Out)
                        Domain.cmBoardDevice.analog2Out.writePointValue(
                            DeviceUtil.getModulatedAnalogVal(
                                systemProfile.systemEquip.analog2MinStaticPressure.readPriorityVal(),
                                systemProfile.systemEquip.analog2MaxStaticPressure.readPriorityVal(),
                                value
                            ).toDouble()
                        )
                    }
                    DomainName.analog3Out -> {
                        TestSignalManager.backUpPoint(Domain.cmBoardDevice.analog2Out)
                        Domain.cmBoardDevice.analog3Out.writePointValue(
                            DeviceUtil.getModulatedAnalogVal(
                                systemProfile.systemEquip.analog3MinHeating.readPriorityVal(),
                                systemProfile.systemEquip.analog3MaxHeating.readPriorityVal(),
                                value
                            ).toDouble()
                        )
                    }

                    DomainName.analog4Out -> {
                        TestSignalManager.backUpPoint(Domain.cmBoardDevice.analog2Out)
                        Domain.cmBoardDevice.analog4Out.writePointValue(
                            DeviceUtil.getModulatedAnalogVal(
                                systemProfile.systemEquip.analog4MinOutsideDamper.readPriorityVal(),
                                systemProfile.systemEquip.analog4MaxOutsideDamper.readPriorityVal(),
                                value
                            ).toDouble()
                        )
                    }
                }
            } else if (tag.contains("relay")) {
                ControlMote.setRelayState(tag, value)
            }
            MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage())
        }
    }
    fun reset() {
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (systemEquip["profile"].toString() == "vavFullyModulatingAhu"
            || systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_ANALOG_RTU.name
        ) {
            profileConfiguration = ModulatingRtuProfileConfig(model).getActiveConfiguration()
            CcuLog.i(Domain.LOG_TAG, "Active vavFullyModulatingAhu profile config Loaded")
        }else{
            profileConfiguration = ModulatingRtuProfileConfig(model).getDefaultConfiguration()
            CcuLog.i(Domain.LOG_TAG, "Default vavFullyModulatingAhu profile config Loaded")
        }
        viewState.value = ModulatingRtuViewState.fromProfileConfig(profileConfiguration)
        viewState.value.isSaveRequired = !systemEquip["profile"].toString().contentEquals("vavFullyModulatingAhu")
    }

    fun hasUnsavedChanges(): Boolean {
        return try {
            val newConfiguration = ModulatingRtuProfileConfig(model).getDefaultConfiguration()
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