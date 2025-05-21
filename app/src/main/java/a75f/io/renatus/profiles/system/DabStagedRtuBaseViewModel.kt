package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.DeviceUtil
import a75f.io.device.mesh.MeshUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.toDouble
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allSystemProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.hyperstat.common.PossibleConditioningMode
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.dab.DabStagedRtu
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel.Companion.saveUnUsedPortStatusOfSystemProfile
import a75f.io.renatus.util.SystemProfileUtil
import a75f.io.renatus.util.TestSignalManager
import a75f.io.renatus.util.modifyConditioningMode
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

open class DabStagedRtuBaseViewModel : ViewModel() {

    lateinit var model : SeventyFiveFProfileDirective
    lateinit var deviceModel : SeventyFiveFDeviceDirective
    open var viewState: MutableState<StagedRtuViewState> = mutableStateOf(StagedRtuViewState())
    lateinit var profileConfiguration: StagedRtuProfileConfig

    lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var relay1AssociationList : List<String>
    lateinit var relay2AssociationList : List<String>
    lateinit var relay3AssociationList : List<String>
    lateinit var relay4AssociationList : List<String>
    lateinit var relay5AssociationList : List<String>
    lateinit var relay6AssociationList : List<String>
    lateinit var relay7AssociationList : List<String>

    lateinit var equipBuilder : ProfileEquipBuilder
    lateinit var deviceBuilder: DeviceBuilder
    lateinit var initialPortValues: HashMap<String, Boolean>
    fun init(context: Context, profileModel : ModelDirective, hayStack : CCUHsApi) {

        CcuLog.i(
            Domain.LOG_TAG, "DabStagedRtuViewModel Init")

        this.hayStack = hayStack
        equipBuilder = ProfileEquipBuilder(hayStack)

        model = profileModel as SeventyFiveFProfileDirective
        CcuLog.i(Domain.LOG_TAG, "StagedRtuViewModel EquipModel Loaded")
        deviceModel = ModelLoader.getCMDeviceModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "StagedRtuViewModel Device Model Loaded")

        val entityMapper = EntityMapper(model)
        deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        this.context = context
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "DabStagedRtuViewModel Loaded")
    }

    private fun initializeLists() {
        relay1AssociationList = Domain.getListByDomainName(DomainName.relay1OutputAssociation, model)
        relay2AssociationList = Domain.getListByDomainName(DomainName.relay2OutputAssociation, model)
        relay3AssociationList = Domain.getListByDomainName(DomainName.relay3OutputAssociation, model)
        relay4AssociationList = Domain.getListByDomainName(DomainName.relay4OutputAssociation, model)
        relay5AssociationList = Domain.getListByDomainName(DomainName.relay5OutputAssociation, model)
        relay6AssociationList = Domain.getListByDomainName(DomainName.relay6OutputAssociation, model)
        relay7AssociationList = Domain.getListByDomainName(DomainName.relay7OutputAssociation, model)
    }

    fun createNewEquip(id : String) : String{
        hayStack.deleteEntityTree(id)
        val equipDis = "${hayStack.siteName}-${model.name}"
        val equipId = equipBuilder.buildEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        deleteSystemConnectModule()

        val deviceDis = hayStack.siteName +"-"+ deviceModel.name
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

    private fun deleteSystemConnectModule() {
        val connectSystemEquip = hayStack.readEntity("domainName == \"" + DomainName.dabAdvancedHybridAhuV2_connectModule + "\"")
        if (connectSystemEquip.isNotEmpty()) {
            hayStack.deleteEntityTree(connectSystemEquip["id"].toString())
        }

        val connectDevice = hayStack.readEntity("domainName == \"" + DomainName.connectModuleDevice + "\"")
        if (connectDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(connectDevice["id"].toString())
        }
    }

    open fun saveConfiguration() {
        viewState.value.updateConfigFromViewState(profileConfiguration)
        val equipDis = "${hayStack.siteName}-${model.name}"
        equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, isReconfiguration = true)

        deviceBuilder.updateDeviceAndPoints(
            profileConfiguration,
            deviceModel,
            Domain.systemEquip.equipRef,
            hayStack.site!!.id,
            hayStack.siteName +"-"+ deviceModel.name
        )
        DomainManager.addSystemDomainEquip(hayStack)
        saveUnUsedPortStatusOfSystemProfile(profileConfiguration, hayStack)
        profileConfiguration.unusedPorts.clear()
        profileConfiguration.unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
        viewState.value.unusedPortState = profileConfiguration.unusedPorts
    }

    fun getRelayState(relayName: String) : Boolean {
        val physicalPoint = L.ccu().systemProfile.logicalPhysicalMap.values.find { it.domainName == relayName }
        return physicalPoint?.readHisVal() == 1.0
    }

    fun sendTestCommand(relayName : String, testCommand : Boolean) {
        Globals.getInstance().isTestMode = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val physicalPoint = L.ccu().systemProfile.logicalPhysicalMap.values.find { it.domainName == relayName }
                if(physicalPoint != null){
                    CcuLog.i(Domain.LOG_TAG, "Send Test Command## $relayName $testCommand ${physicalPoint?.readHisVal()} -- ${testCommand.toDouble()}")
                    TestSignalManager.backUpRestorePoint(physicalPoint, testCommand)
                }
                physicalPoint?.writePointValue(testCommand.toDouble())
                physicalPoint?.writeHisVal(testCommand.toDouble())
                CcuLog.i(Domain.LOG_TAG, "Send Test Command $relayName $testCommand ${physicalPoint?.readHisVal()}  -- ${testCommand.toDouble()} ")
                MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage())
            }
        }
    }

    fun sendAnalogTestSignal(value: Double) {
        Globals.getInstance().isTestMode = true
        TestSignalManager.backUpPoint(Domain.cmBoardDevice.analog2Out)
        Domain.cmBoardDevice.analog2Out.writePointValue(10 * value)
        MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage())
    }

    fun updateSystemMode() {
        val systemProfile = L.ccu().systemProfile as DabStagedRtu
        val possibleConditioningMode = when {
            systemProfile.isCoolingAvailable && systemProfile.isHeatingAvailable -> PossibleConditioningMode.BOTH
            systemProfile.isCoolingAvailable && !systemProfile.isHeatingAvailable -> PossibleConditioningMode.COOLONLY
            !systemProfile.isCoolingAvailable && systemProfile.isHeatingAvailable -> PossibleConditioningMode.HEATONLY
            else -> PossibleConditioningMode.OFF
        }

        val conditioningMode = Point(DomainName.conditioningMode, Domain.systemEquip.equipRef)
        modifyConditioningMode(possibleConditioningMode.ordinal, conditioningMode, allSystemProfileConditions)

        val mode = SystemMode.values()[systemProfile.systemEquip.conditioningMode.readPriorityVal().toInt()]
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

    fun setStateChanged() {
        viewState.value.isStateChanged = true
        viewState.value.isSaveRequired = true
    }

    fun deleteSystemProfile(systemProfileId: String){
        val deleteTime = measureTimeMillis {
            hayStack.deleteEntityTree(systemProfileId)
        }
        CcuLog.i(L.TAG_CCU_DOMAIN, "Time taken to delete entities: $deleteTime")
    }
}