package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.DeviceUtil
import a75f.io.device.mesh.MeshUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.toDouble
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.renatus.util.SystemProfileUtil
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class ModulatingRtuViewModel : ViewModel() {

    lateinit var model: SeventyFiveFProfileDirective
    lateinit var deviceModel: SeventyFiveFDeviceDirective
    lateinit var viewState: ModulatingRtuViewState
    lateinit var profileConfiguration: ModulatingRtuProfileConfig

    lateinit var context: Context
    lateinit var hayStack: CCUHsApi
    lateinit var relay7AssociationList: List<String>

    var modelLoaded by mutableStateOf(false)
    lateinit var equipBuilder: ProfileEquipBuilder
    lateinit var deviceBuilder: DeviceBuilder

    fun init(context: Context, profileModel: ModelDirective, hayStack: CCUHsApi) {
        this.hayStack = hayStack
        equipBuilder = ProfileEquipBuilder(hayStack)

        model = profileModel as SeventyFiveFProfileDirective
        deviceModel = ModelLoader.getCMDeviceModel() as SeventyFiveFDeviceDirective
        val entityMapper = EntityMapper(model)
        deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        this.context = context
        initializeLists()
        CcuLog.i(
            Domain.LOG_TAG, "Vav RTU Init"
        )

    }

    private fun initializeLists() {
        relay7AssociationList =
            Domain.getListByDomainName(DomainName.relay7OutputAssociation, model)
    }

    fun createNewEquip(id: String): String {
        hayStack.deleteEntityTree(id)
        val equipDis = hayStack.siteName + "-${model.name}"
        val equipId = equipBuilder.buildEquipAndPoints(
            profileConfiguration,
            model,
            hayStack.site!!.id,
            equipDis
        )

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }
        val deviceDis = hayStack.siteName + "-" + deviceModel.name
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfiguration,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )

        hayStack.syncEntityTree()
        DomainManager.addSystemDomainEquip(hayStack)
        DomainManager.addCmBoardDevice(hayStack)
        return equipId
    }

    open fun saveConfiguration() {
        viewState.updateConfigFromViewState(profileConfiguration)
        val equipDis = hayStack.siteName + "-${model.name}"
        equipBuilder.updateEquipAndPoints(
            profileConfiguration,
            model,
            hayStack.site!!.id,
            equipDis,
            isReconfiguration = true
        )

        deviceBuilder.updateDeviceAndPoints(
            profileConfiguration,
            deviceModel,
            Domain.systemEquip.equipRef,
            hayStack.site!!.id,
            hayStack.siteName + "-" + deviceModel.name
        )
        DomainManager.addSystemDomainEquip(hayStack)
    }

    fun updateSystemMode() {
        val systemProfile = L.ccu().systemProfile as VavFullyModulatingRtu
        val mode = SystemMode.values()[systemProfile.systemEquip.conditioningMode.readPriorityVal()
            .toInt()]
        if (mode == SystemMode.OFF) {
            return
        }
        if (mode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable || !systemProfile.isHeatingAvailable)
            || mode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable
            || mode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable
        ) {
            SystemProfileUtil.showConditioningDisabledDialog(context as Activity, mode)
        }
    }

    open fun sendAnalogRelayTestSignal(tag: String, value: Double) {
        TODO("Not yet implemented")
    }

    fun getRelayState(relayName: String) : Boolean {
        val physicalPoint = L.ccu().systemProfile.logicalPhysicalMap.values.find { it.domainName == relayName }
        return physicalPoint?.readHisVal() == 1.0
    }

    fun sendTestCommand(relayName: String, testCommand: Boolean) {
        Globals.getInstance().setTestMode(true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val physicalPoint =
                    L.ccu().systemProfile.logicalPhysicalMap.values.find { it.domainName == relayName }
                physicalPoint?.writeHisVal(testCommand.toDouble())
                CcuLog.i(
                    Domain.LOG_TAG,
                    "Send Test Command $relayName $testCommand ${physicalPoint?.readHisVal()}"
                )
                MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage())
            }
        }
    }
}