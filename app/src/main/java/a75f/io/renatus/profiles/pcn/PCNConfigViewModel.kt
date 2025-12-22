package a75f.io.renatus.profiles.pcn

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.device.mesh.LSerial
import a75f.io.domain.devices.PCNDevice
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.service.DomainService
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.connectnode.ConnectNodeConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusEquip
import a75f.io.logic.bo.building.pcn.ConnectModule
import a75f.io.logic.bo.building.pcn.ExternalEquip
import a75f.io.logic.bo.building.pcn.PCNUtil
import a75f.io.logic.bo.building.pcn.PCNViewState
import a75f.io.logic.bo.building.pcn.PCNViewStateUtil
import a75f.io.logic.bo.building.pcn.PcnConfiguration
import a75f.io.logic.connectnode.EquipModel
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.modbus.util.formattedToastMessage
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.profiles.CopyConfiguration
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.pcn.PcnCopyPasteUtil.Companion.checkSelectedConfigurationAndCurrentEquipListSame
import a75f.io.renatus.util.Option
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDevicePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class PCNConfigViewModel (application: Application) : AndroidViewModel(application) {

    var viewState = mutableStateOf(PCNViewState())
    var deviceAddress by Delegates.notNull<Short>()

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var nodeType: NodeType
    lateinit var context: Context
    lateinit var hayStack: CCUHsApi
    lateinit var cnDeviceModel: SeventyFiveFDeviceDirective
    lateinit var pcnDeviceModel : SeventyFiveFDeviceDirective
    lateinit var pairingCompleteListener: OnPairingCompleteListener
    private val _isDisabled = MutableLiveData(false)
    val isDisabled: LiveData<Boolean> = _isDisabled
    private val _isReloadRequired = MutableLiveData(false)
    val isReloadRequired: LiveData<Boolean> = _isReloadRequired
    private var saveJob: Job? = null
    var domainService = DomainService()

    // TODO : AMAR  TEST AND REMOVE
    var isFreshDraw = false

    // these are temporary variables to hold the device and model data while fetching
    lateinit var equipmentDevice: EquipmentDevice
    lateinit var deviceModelList: List<ModelMetaData>
    var deviceList = mutableStateOf(emptyList<Pair<String, Int>>())
    var equipModel = mutableStateOf(EquipModel())

     private lateinit var pcnConfiguration: PcnConfiguration


    fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        this.context = context
        this.hayStack = hayStack
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        cnDeviceModel = ModelLoader.getConnectNodeDeviceModel() as SeventyFiveFDeviceDirective
        pcnDeviceModel = ModelLoader.getPcnDeviceModel() as SeventyFiveFDeviceDirective

        pcnConfiguration = PcnConfiguration(
            nodeAddress = deviceAddress.toInt(),
            nodeType = nodeType.name,
            priority = 0,
            roomRef = zoneRef,
            floorRef = floorRef,
            profileType = ProfileType.PCN,
        ).getActiveConfiguration()

        viewState.value = PCNViewStateUtil.configToState(pcnConfiguration, PCNViewState())

        isCopiedConfigurationAvailable()
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPCN()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }

                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
                // Send the write update to serial for PCN
                LSerial.getInstance().isWritePcnUpdate = true
            }
        }
    }

    private fun setUpPCN() {
        pcnConfiguration = PCNViewStateUtil.stateToConfig(viewState.value, pcnConfiguration)
        val deviceBuilder = DeviceBuilder(hayStack, null)

        if (pcnConfiguration.pcnDeviceMap.isEmpty()) {
            val config = PcnConfiguration(
                nodeAddress = deviceAddress.toInt(),
                nodeType = nodeType.name,
                priority = 0,
                roomRef = zoneRef,
                floorRef = floorRef,
                profileType = ProfileType.PCN,
            ).getActiveConfiguration()

            fun getDeviceDis() = "${hayStack.siteName}-${pcnDeviceModel.name}-${config.nodeAddress}"

             val pcnDeviceId = deviceBuilder.buildLowCodeDeviceAndPoints(
                config,
                pcnDeviceModel,
                hayStack.site!!.id,
                getDeviceDis()
            )
            pcnConfiguration.pcnDeviceMap = hayStack.readMapById(pcnDeviceId)
        }

        pcnConfiguration.deletedCNList.forEach { serverId ->
            PCNUtil.deleteConnectModuleTree(hayStack, serverId, pcnConfiguration.pcnDeviceMap[Tags.ID].toString())
        }
        pcnConfiguration.deletedCNList.clear()


        pcnConfiguration.deletedExternalEquipList.forEach { serverId ->
            PCNUtil.deleteExternalEquipTree(
                hayStack,
                serverId,
                pcnConfiguration.pcnDeviceMap[Tags.ID].toString()
            )
        }
        pcnConfiguration.deletedCNList.clear()

        if (pcnConfiguration.connectModuleList.toList().isNotEmpty()) {
            pcnConfiguration.connectModuleList.filter { it.newConfiguration }.forEach { connectModule ->
                val config = ConnectNodeConfiguration(
                    nodeAddress = connectModule.serverId,
                    nodeType = nodeType.name,
                    priority = 0,
                    roomRef = zoneRef,
                    floorRef = floorRef,
                    profileType = ProfileType.CONNECTNODE,
                ).getDefaultConfiguration()

                fun getDeviceDis() = "${hayStack.siteName}-${cnDeviceModel.name}-${config.nodeAddress}"

                deviceBuilder.buildLowCodeDeviceAndPoints(
                    config,
                    cnDeviceModel,
                    hayStack.site!!.id,
                    getDeviceDis(),
                    pcnConfiguration.pcnDeviceMap[Tags.ID].toString()
                )
            }
        }
        if (pcnConfiguration.externalEquipList.toList().isNotEmpty()) {
            pcnConfiguration.externalEquipList.filter { it.newConfiguration }.forEach { externalEquip ->
                ModbusEquip(ProfileType.MODBUS_EMR, externalEquip.serverId.toShort()).createEntities(floorRef, zoneRef,
                    externalEquip.equipModel.equipDevice.value.apply { slaveId =  externalEquip.serverId}, getParametersList(externalEquip.equipModel),
                    null, false, Tags.ZONE, null, false, false, true,
                    null, true, "", null, null, pcnConfiguration.pcnDeviceMap[Tags.ID].toString(),"")

            }
        }

        listOf(
            ProfileType.PCN to pcnConfiguration.pcnEquips.filter { !it.newConfiguration }
                .map { it.equipData },
            ProfileType.CONNECTNODE to pcnConfiguration.connectModuleList.filter { !it.newConfiguration }
                .map { it.equipData },
            ProfileType.MODBUS_EMR to pcnConfiguration.externalEquipList.filter { !it.newConfiguration }
                .map { it.equipData }
        ).forEach { (profileType, equipList) ->
            equipList.forEach { equip ->
                val modbusProfile = ModbusEquip(
                    profileType,
                    equip.serverId.toShort()
                )
                updateEquip(equip.equipModel, modbusProfile)
            }
        }

        saveRSBridgingDetails(pcnConfiguration)
    }

    private fun updateEquip(equipModel: List<EquipModel>, modbusProfile: ModbusEquip) {
        equipModel.forEach { equip ->
            modbusProfile.updateHaystackPoints(
                "",
                getParametersList(equip)
            )
        }
    }

    private fun saveRSBridgingDetails(pcnConfig: PcnConfiguration) {
        val pcnDevice = PCNDevice(pcnConfig.pcnDeviceMap[Tags.ID].toString())

        pcnDevice.parity.writeDefaultVal(pcnConfig.parity.doubleValue)
        pcnDevice.baudRate.writeDefaultVal(pcnConfig.baudRate.doubleValue)
        pcnDevice.dataBits.writeDefaultVal(pcnConfig.dataBits.doubleValue)
        pcnDevice.stopBits.writeDefaultVal(pcnConfig.stopBits.doubleValue)

    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    fun isCopiedConfigurationAvailable() {
        val selectedProfileType = CopyConfiguration.getSelectedProfileType()
        if (selectedProfileType != null && selectedProfileType == ProfileType.PCN) {
            val selectedConfiguration = CopyConfiguration.getCopiedConfiguration()

            CcuLog.i(
                L.TAG_PCN,
                "Checking copied configuration: ProfileType :$selectedProfileType"
            )
            if (checkSelectedConfigurationAndCurrentEquipListSame(pcnConfiguration, selectedConfiguration as PcnConfiguration)) {
                disablePasteConfiguration()
            }
        }
    }

    fun applyCopiedConfiguration() {

        if(CopyConfiguration.getSelectedProfileType() == ProfileType.PCN) {
            val copiedConfig = CopyConfiguration.getCopiedConfiguration() as PcnConfiguration
            PcnCopyPasteUtil.copyState(viewState, copiedConfig)
        }

        reloadUiRequired()
        disablePasteConfiguration()
        formattedToastMessage(context.getString(R.string.Toast_Success_Message_paste_Configuration), context)
    }



    private fun reloadUiRequired() {
        _isReloadRequired.value = !_isReloadRequired.value!!
    }

    fun disablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = !_isDisabled.value!!
        }
    }

    fun addConnectModule() {
        val newId = getNextServerId()
        viewState.value.connectModuleList.add(
            ConnectModule(
                newId,
                newConfiguration = true,
                modelUpdated = false
            )
        )
    }

    fun removeConnectModule(serverId: Int) {
        if (viewState.value.connectModuleList.first {it.serverId == serverId }.newConfiguration) {
            viewState.value.connectModuleList.removeIf { it.serverId == serverId }
        } else {
            viewState.value.connectModuleList.removeIf { it.serverId == serverId }
            viewState.value.deletedCNList.add(serverId)
        }
    }

    fun removeExternalEquip(serverId: Int) {
        if (viewState.value.externalEquipList.first {it.serverId == serverId }.newConfiguration) {
            viewState.value.externalEquipList.removeIf { it.serverId == serverId }
        } else {
            viewState.value.externalEquipList.removeIf { it.serverId == serverId }
            viewState.value.deletedExternalEquipList.add(serverId)
        }
    }

    fun getDisabledIndices(isServerId: Boolean, selectedOption: Int): List<Int> {
        return if (isServerId) {
            val usedIndices = sequenceOf(
                viewState.value.connectModuleList.map { it.serverId }.filter { it != selectedOption },
                viewState.value.externalEquipList.map { it.serverId }.filter { it != selectedOption },
                listOf(pcnConfiguration.nodeAddress.toString().takeLast(2).toInt())
            ).flatten().toList()
            PCNUtil.getUsedIndices(usedIndices)
        }
        else {
            emptyList()
        }

    }
    fun getModelIdByName(name: String): String {
        return deviceModelList.find { it.name == name }!!.id
    }
    fun getVersionByID(id: String): String {
        return deviceModelList.find { it.id == id }!!.version
    }
    fun fetchModelDetails(selectedDevice: String) {
       PCNRepository.fetchModelDetails(selectedDevice, this, context)
    }

    fun addExternalEquip(context: Context, onResult: (Boolean) -> Unit) {
        if (deviceList.value.isNotEmpty()) {
            onResult (true)
        } else {
            PCNRepository.readModbusModelsList(context, this, onResult)
        }
    }

    fun serverIdChange(selected: Int, previousSelected: Int) {

        val connectIndex = viewState.value.connectModuleList
            .indexOfFirst { it.serverId == previousSelected }
        val externalEquipIndex = viewState.value.externalEquipList
            .indexOfFirst { it.serverId == previousSelected }

        if (connectIndex != -1) {
            val old = viewState.value.connectModuleList[connectIndex]
            viewState.value.connectModuleList[connectIndex] =
                old.copy(serverId = selected)
        } else if (externalEquipIndex != -1) {
            val old = viewState.value.externalEquipList[externalEquipIndex]
            viewState.value.externalEquipList[externalEquipIndex] =
                old.copy(serverId = selected)
        }
    }

    fun getNextServerId(): Int {
        val pairedId = viewState.value.connectModuleList.map { it.serverId } + viewState.value.externalEquipList.map { it.serverId } + pcnConfiguration.nodeAddress.toString().takeLast(2).toInt()
        var serverId = 1
        while (pairedId.contains(serverId)) {
            serverId++
        }
        return serverId
    }

    fun getAllowedValues(domainName: String, model: SeventyFiveFDeviceDirective): List<Option> {
        val pointDef = getPointByDomainName(model, domainName) ?: return emptyList()
        return if (pointDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            val constraint = pointDef.valueConstraint as MultiStateConstraint
            val enums = mutableListOf<Option>()
            constraint.allowedValues.forEach {
                enums.add(Option(it.index, it.value, it.dis))
            }
            enums
        } else {
            emptyList()
        }
    }
    private fun getPointByDomainName(
        modelDefinition: SeventyFiveFDeviceDirective,
        domainName: String
    ): SeventyFiveFDevicePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }

    fun getServerIdList(): List<Option> {
        return listOf(
            Option(0, "1", "1"), Option(1, "2", "2"),
            Option(2, "3", "3"), Option(3, "4", "4"), Option(4, "5", "5")
        )
    }

    fun getPairedConnectModules(): List<ConnectModule> {
        return viewState.value.connectModuleList.filter { !it.newConfiguration }.let { list ->
            list.sortedBy { it.serverId }
        }
    }

    fun getNewConnectModules(): List<ConnectModule> {
        return viewState.value.connectModuleList.filter { it.newConfiguration }.let { list ->
            list.sortedBy { it.serverId }
        }
    }


    fun getPairedExternalEquips(): List<ExternalEquip> {
        return viewState.value.externalEquipList.filter { !it.newConfiguration }.let { list ->
            list.sortedBy { it.serverId }
        }
    }
    fun getNewExternalEquips(): List<ExternalEquip> {
        return viewState.value.externalEquipList.filter { it.newConfiguration }.let { list ->
            list.sortedBy { it.serverId }
        }
    }

}