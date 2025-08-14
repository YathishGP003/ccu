package a75f.io.renatus.profiles.connectnode

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.device.modbus.buildModbusModel
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.connectnode.ConnectNodeConfiguration
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusEquip
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.profiles.CopyConfiguration
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class ConnectNodeViewModel(application: Application) : AndroidViewModel(application) {
    private var deviceAddress by Delegates.notNull<Short>()

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var nodeType: NodeType
    lateinit var context: Context
    lateinit var hayStack: CCUHsApi
    lateinit var deviceModel: SeventyFiveFDeviceDirective
    lateinit var pairingCompleteListener: OnPairingCompleteListener
    private val _isDisabled = MutableLiveData(false)
    val isDisabled: LiveData<Boolean> = _isDisabled
    private val _isReloadRequired = MutableLiveData(false)
    val isReloadRequired: LiveData<Boolean> = _isReloadRequired
    var equipRef: String? = null
    private var saveJob: Job? = null
    lateinit var equipmentDeviceList: List<EquipmentDevice>

    fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        this.context = context
        this.hayStack = hayStack
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        deviceModel = ModelLoader.getConnectNodeDeviceModel() as SeventyFiveFDeviceDirective
        loadEquips()
        isCopiedConfigurationAvailable()
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpConnectNode()
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
            }
        }
    }

    private fun setUpConnectNode() {
        if (equipmentDeviceList.isEmpty()) {
            val deviceBuilder = DeviceBuilder(hayStack, null)
            val config = ConnectNodeConfiguration(
                nodeAddress = deviceAddress.toInt(),
                nodeType = nodeType.name,
                priority = 0,
                roomRef = zoneRef,
                floorRef = floorRef,
                profileType = ProfileType.CONNECTNODE,
            ).getDefaultConfiguration()

            fun getDeviceDis() = "${hayStack.siteName}-${deviceModel.name}-${config.nodeAddress}"

            deviceBuilder.buildCnDeviceAndPoints(
                config,
                deviceModel,
                hayStack.site!!.id,
                getDeviceDis()
            )
        } else {
            equipmentDeviceList.forEach { equipModel ->
                val modbusProfile = ModbusEquip(
                    ProfileType.CONNECTNODE,
                    equipModel.slaveId.toShort()
                )
                modbusProfile.updateHaystackPoints(
                    equipModel.deviceEquipRef,
                    getParametersList(equipModel)
                )
            }
        }
    }


    fun isCopiedConfigurationAvailable() {
        val selectedConfiguration = CopyConfiguration.getCopiedConnectNodeConfiguration()
            ?.sortedBy { it.name }

        val profileTypeCopied = CopyConfiguration.getSelectedProfileType()
        CcuLog.i(L.TAG_CONNECT_NODE, "Checking copied configuration: ProfileType :$profileTypeCopied")
        if (!selectedConfiguration.isNullOrEmpty() && profileTypeCopied == ProfileType.CONNECTNODE &&
            checkSelectedConfigurationAndCurrentEquipListSame(selectedConfiguration)) {
            disablePasteConfiguration()
        }
    }


    private fun checkSelectedConfigurationAndCurrentEquipListSame(selectedConfiguration: List<EquipmentDevice>): Boolean {
        val sortedEquipmentList = equipmentDeviceList.sortedBy { it.name }
        if (equipmentDeviceList.size != selectedConfiguration.size) return false
        for (i in sortedEquipmentList.indices) {
            val currentEquip = sortedEquipmentList[i]
            val copiedEquip = selectedConfiguration[i]

            if (currentEquip.registers.size != copiedEquip.registers.size ||
                currentEquip.name != copiedEquip.name
            ) {
                CcuLog.i(L.TAG_CONNECT_NODE, "Copied configuration does not match current equipment list")
                return false
            }
        }
        CcuLog.i(L.TAG_CONNECT_NODE, "Copied configuration matches current equipment list")
        return true
    }

    fun applyCopiedConfiguration() {

        val copiedEquipList = CopyConfiguration.getCopiedConnectNodeConfiguration()
        if( copiedEquipList.isNullOrEmpty()) {
            CcuLog.i(L.TAG_CONNECT_NODE, "No copied configuration available to apply")
            return
        }
        val paramIndex = 0

        equipmentDeviceList.forEachIndexed { equipIndex, equip ->
            val newRegisters = copiedEquipList[equipIndex].registers
            equip.registers.forEachIndexed { regIndex, reg ->

                val newParam = newRegisters.find { it.parameters[0].name == reg.parameters[0].name}!!.parameters[0]
                reg.parameters[paramIndex] .apply {
                    isDisplayInUI = newParam.isDisplayInUI
                    userIntentPointTags = newParam.userIntentPointTags
                    isSchedulable = newParam.isSchedulable
                }


            }
        }
        disablePasteConfiguration()

    }
    fun disablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = !_isDisabled.value!!
        }
    }

    private fun loadEquips() {
        equipmentDeviceList = buildModbusModel(zoneRef)
            .let { ConnectNodeUtil.reorderEquipments(it) }
    }
}