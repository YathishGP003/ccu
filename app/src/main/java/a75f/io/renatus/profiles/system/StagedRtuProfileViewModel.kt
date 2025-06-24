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
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.vav.VavStagedRtu
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

open class StagedRtuProfileViewModel : ViewModel() {

    lateinit var model : SeventyFiveFProfileDirective
    lateinit var deviceModel : SeventyFiveFDeviceDirective
    open var viewState: MutableState<StagedRtuViewState> = mutableStateOf(StagedRtuViewState())
    lateinit var profileConfiguration: StagedRtuProfileConfig

    lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var relay1AssociationList : List<String>
    lateinit var analog2OutputAssociationList : List<String>
    lateinit var thermistor1AssociationList : List<String>
    lateinit var analogIn1AssociationList : List<String>

    var modelLoadedState =  MutableLiveData(false)
    val modelLoaded: LiveData<Boolean> get() = modelLoadedState
    lateinit var equipBuilder : ProfileEquipBuilder
    lateinit var deviceBuilder: DeviceBuilder
    val ProfileNameRTU : String = "VAV Staged RTU"
    val ProfileNameVFD : String = "VAV Staged RTU with VFD Fan"
    lateinit var initialPortValues: HashMap<String, Boolean>
    fun init(context: Context, profileModel : ModelDirective, hayStack : CCUHsApi) {

        CcuLog.i(
            Domain.LOG_TAG, "VavStagedRtuViewModel Init")

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
        CcuLog.i(Domain.LOG_TAG, "VavStagedRtuViewModel Loaded")
    }

    private fun initializeLists() {
        relay1AssociationList = Domain.getListOfDisNameByDomainName(DomainName.relay1OutputAssociation, model)
        analog2OutputAssociationList = Domain.getListOfDisNameByDomainName(DomainName.analog2OutputAssociation, model)
        thermistor1AssociationList = Domain.getListOfDisNameByDomainName(DomainName.thermistor1InputAssociation, model)
        analogIn1AssociationList = Domain.getListOfDisNameByDomainName(DomainName.analog1InputAssociation, model)
    }

    fun createNewEquip(): String{
        val equipDis = "${hayStack.siteName}-${model.name}"
        val equipId = equipBuilder.buildEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        L.ccu().systemProfile.deleteSystemConnectModule()

        val deviceDis = hayStack.siteName +"-"+ deviceModel.name
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfiguration,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )

        DomainManager.addSystemDomainEquip(hayStack)
        DomainManager.addCmBoardDevice(hayStack)
        return equipId
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
        if((L.ccu().systemProfile.profileName == ProfileNameRTU || L.ccu().systemProfile.profileName == ProfileNameVFD) && !viewState.value.isSaveRequired) {
            val physicalPoint = L.ccu().systemProfile.logicalPhysicalMap.values.find { it.domainName == relayName }
            return physicalPoint?.readHisVal() == 1.0
        }
        return false
    }

    fun sendTestCommand(relayName : String, testCommand : Boolean) {
        if ((L.ccu().systemProfile.profileName == ProfileNameRTU || L.ccu().systemProfile.profileName == ProfileNameVFD) && !viewState.value.isSaveRequired) {
            Globals.getInstance().isTestMode = true
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val physicalPoint =
                        L.ccu().systemProfile.logicalPhysicalMap.values.find { it.domainName == relayName }
                    if(physicalPoint != null){
                        TestSignalManager.backUpRestorePoint(physicalPoint, testCommand)
                    }
                    physicalPoint?.writePointValue(testCommand.toDouble())
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
     //this function is only for VFD Profile
    fun sendAnalogTestSignal(value: Double) {
        if(L.ccu().systemProfile.profileName == ProfileNameVFD) {
            Globals.getInstance().isTestMode = true
            TestSignalManager.backUpPoint(Domain.cmBoardDevice.analog2Out)
            Domain.cmBoardDevice.analog2Out.writePointValue(10 * value)
            MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage())
        }
    }

    fun updateSystemMode() {
        val systemProfile = L.ccu().systemProfile as VavStagedRtu
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