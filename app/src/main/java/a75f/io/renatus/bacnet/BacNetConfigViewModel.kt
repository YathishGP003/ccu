package a75f.io.renatus.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.bacnet.parser.BacnetProperty
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.bacnet.BacnetProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCov
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCovForAllDevices
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCovRequest
import a75f.io.logic.bo.building.system.BacnetReadRequestMultiple
import a75f.io.logic.bo.building.system.BacnetWhoIsRequest
import a75f.io.logic.bo.building.system.BroadCast
import a75f.io.logic.bo.building.system.DestinationMultiRead
import a75f.io.logic.bo.building.system.ObjectIdentifierBacNet
import a75f.io.logic.bo.building.system.PropertyReference
import a75f.io.logic.bo.building.system.ReadRequestMultiple
import a75f.io.logic.bo.building.system.RpmRequest
import a75f.io.logic.bo.building.system.WhoIsRequest
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_DEVICE_ID
import a75f.io.logic.util.bacnet.buildBacnetModel
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.CcuService
import a75f.io.logic.bo.building.system.client.MultiReadResponse
import a75f.io.logic.bo.building.system.client.RpResponseMultiReadItem
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.logic.bo.building.system.client.WhoIsResponseItem
import a75f.io.logic.util.bacnet.getDetailsFromObjectLayout
import a75f.io.logic.util.bacnet.isValidMstpMacAddress
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.bacnet.models.BacnetDevice
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.bacnet.models.BacnetPointState
import a75f.io.renatus.bacnet.util.IP_CONFIGURATION
import a75f.io.renatus.bacnet.util.MSTP_CONFIGURATION
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.util.BACNET_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.modbus.util.NO_INTERNET
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.OK
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.OnItemSelectBacnetDevice
import a75f.io.renatus.modbus.util.SAVED
import a75f.io.renatus.modbus.util.SAVING_BACNET
import a75f.io.renatus.modbus.util.WARNING
import a75f.io.renatus.modbus.util.formattedToastMessage
import a75f.io.renatus.modbus.util.getBacnetPoints
import a75f.io.renatus.modbus.util.isAllParamsSelectedBacNet
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.CopyConfiguration
import a75f.io.renatus.profiles.CopyConfiguration.Companion.getSelectedBacNetModel
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.util.ProgressDialogUtils
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.properties.Delegates


class BacNetConfigViewModel(application: Application) : AndroidViewModel(application) {

    var deviceSelectionMode = mutableStateOf(0)
    var mstpDeviceSelectionMode = mutableStateOf(0) // 0 is Slave
    val TAG = "BacNetConfigViewModel"
    var deviceList = mutableStateOf(emptyList<String>())
    var connectedDevices = mutableStateOf(emptyList<BacnetDevice>())
    val isConnectedDevicesSearchFinished = mutableStateOf(false)
    var bacnetModel = mutableStateOf(BacnetModel())
    var isErrorMsg = mutableStateOf(false)
    var errorMsg = ""
    var deviceId  = mutableStateOf("")
    var isDeviceIdValid = mutableStateOf(true)
    var destinationIp  = mutableStateOf("")
    var isDestinationIpValid = mutableStateOf(true)
    var destinationPort  = mutableStateOf("")
    var destinationMacAddress  = mutableStateOf("")
    var dnet = mutableStateOf("0")

    val isUpdateMode = mutableStateOf(false)
    val displayInUi = mutableStateOf(true)
    val deviceValue = mutableStateOf(false)
    val bacnetRequestFailed = mutableStateOf(false)
    val bacnetPropertiesFetched = mutableStateOf(false)
    var isStateChanged = mutableStateOf(false)
    var isDeviceValueSelected = mutableStateOf(false)

    var modelName = mutableStateOf("Select Model")
    var configurationType = mutableStateOf("Select Configuration Type")
    private lateinit var pairingCompleteListener: OnPairingCompleteListener

    private lateinit var bacnetProfile: BacnetProfile
    private lateinit var filer: String

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var moduleLevel: String
    lateinit var profileType: ProfileType
    lateinit var deviceModelList: List<ModelMetaData>

    lateinit var deviceIp: String
    lateinit var devicePort: String
    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    private var selectedSlaveId by Delegates.notNull<Long>()
    private var domainService = DomainService()
    private val _isDialogOpen = MutableLiveData<Boolean>()
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen
    private val _isDisabled = MutableLiveData(false)
    val isDisabled: LiveData<Boolean> = _isDisabled

    val bacNetItemsMap = hashMapOf<String, RpResponseMultiReadItem>()
    private lateinit var service : CcuService

    val showToast = mutableStateOf(false)

    val onItemSelect = object : OnItemSelect {
        override fun onItemSelected(index: Int, item: String) {
            modelName.value = item
            ProgressDialogUtils.showProgressDialog( context, "Fetching $item details")
            fetchModelDetails(item)
            isCopiedConfigurationAvailable()
        }
    }

    val onBacnetDeviceSelect = object : OnItemSelectBacnetDevice {
        override fun onItemSelected(index: Int, item: BacnetDevice) {
            CcuLog.d(TAG, "onItemSelected-->$item")
            destinationIp.value = item.deviceIp
            deviceId.value = item.deviceId
            destinationPort.value = item.devicePort
            destinationMacAddress.value = if(configurationType.value == IP_CONFIGURATION) item.deviceMacAddress?.let { macAddressToByteArray(it) } ?: ""
                                          else item.deviceMacAddress?:""
            dnet.value = item.deviceNetwork
        }
    }

    private fun macAddressToByteArray(mac: String): String {
        var byteArray = mac.split(":")
            .map { it.toInt(16).toByte() }
            .toByteArray()
        return byteArray.joinToString(".") { (it.toInt() and 0xFF).toString() }

    }
    
    fun configModelDefinition(context: Context) {
        this.context = context

        if (L.getProfile(selectedSlaveId) != null) {

            bacnetProfile = L.getProfile(selectedSlaveId) as BacnetProfile
            if (bacnetProfile != null) {

                selectedSlaveId = bacnetProfile.slaveId
                val equipmentDevice = buildBacnetModel(bacnetProfile.equip.roomRef)
                val model = BacnetModel()
                model.equipDevice.value = equipmentDevice[0]
                model.selectAllParameters.value = isAllParamsSelectedBacNet(equipmentDevice[0])
                displayInUi.value = model.selectAllParameters.value
                model.points = getBacnetPoints(equipmentDevice[0].points.toImmutableList()) //equipmentDevice[0].points
                bacnetModel.value = model
                bacnetModel.value.isDevicePaired = true
                bacnetModel.value.version.value = bacnetProfile.equip.tags.get("version").toString()


                model.equipDevice.value.bacnetConfig?.let { config ->
                    config.split(",").forEach {
                        val configParam = it.split(":")
                        when(configParam[0]){
                            "deviceId" -> deviceId.value = configParam[1]
                            "destinationIp" -> destinationIp.value = configParam[1]
                            "destinationPort" -> destinationPort.value = configParam[1]
                            "macAddress" -> destinationMacAddress.value = configParam[1]
                            "deviceNetwork" -> dnet.value = configParam[1]
                        }
                    }
                }
                CcuLog.d(TAG, "--> deviceId ${deviceId.value} destinationIp ${destinationIp.value} destinationPort ${destinationPort.value} macAddress ${destinationMacAddress.value} dnet ${dnet.value}")
                model.equipDevice.value.modelConfig?.let { config ->
                    config.split(",").forEach {
                        val configParam = it.split(":")
                        when (configParam[0]) {
                            "modelName" -> modelName.value = configParam[1]
                            "version" -> {
                                bacnetModel.value.version.value = configParam[1]
                            }
                        }
                    }
                }
                val isConfigAsMstp = isValidMstpMacAddress(destinationMacAddress.value)
                if (isConfigAsMstp) {
                    configurationType.value = MSTP_CONFIGURATION
                } else {
                    configurationType.value = IP_CONFIGURATION
                }
            }
            isCopiedConfigurationAvailable()
            isUpdateMode.value = true
        } else {
            isUpdateMode.value = false
            ProgressDialogUtils.showProgressDialog(context, "Loading Bacnet Models")
            if(!::deviceModelList.isInitialized || deviceModelList.isEmpty()){
                readDeviceModels()
            }else{
                ProgressDialogUtils.hideProgressDialog()
            }
        }
    }


    fun holdBundleValues(bundle: Bundle) {
        selectedSlaveId = bundle.getString(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)?.toLong() ?: 0
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        filer = bundle.getString(FragmentCommonBundleArgs.MODBUS_FILTER)!!
        val profileOriginalValue = bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)
        profileType = ProfileType.values()[profileOriginalValue]
        val level = ModbusLevel.values()[bundle.getInt(FragmentCommonBundleArgs.MODBUS_LEVEL)]
        moduleLevel = if (level == ModbusLevel.ZONE ) "zone" else "system"
    }

    private fun readDeviceModels() {
        domainService.readBacNetModelsList("BACNET", "", object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                try {
                    if (!response.isNullOrEmpty()) {
                        val itemList = mutableListOf<String>()
                        deviceModelList = getModelListFromJson(response)
                        deviceModelList.forEach {
                            itemList.add(it.name)
                        }
                        deviceList.value = itemList
                    } else {
                        showErrorDialog(context, BACNET_DEVICE_LIST_NOT_FOUND, true)
                    }
                } catch (e: Exception) {
                    showErrorDialog(context, NO_INTERNET, true)
                    CcuLog.d("MSTP", "error in parsing json-- $e")
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET, true)
                CcuLog.d("MSTP", "error in parsing json response -- $response")
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    private fun getVersionByID(id: String): String {
        return deviceModelList.find { it.id == id }!!.version
    }

    fun fetchModelDetails(selectedDevice: String) {
        val modelId = getModelIdByName(selectedDevice)
        val version = getVersionByID(modelId)
        domainService.readBacNetModelById(context, modelId, version, object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                if (!response.isNullOrEmpty()) {
                    try {
                        val equipmentDevice = Gson().fromJson(response, BacnetModelDetailResponse::class.java)
                        if (equipmentDevice != null) {
                            Log.d(TAG, "received data-->$equipmentDevice updating model")
                            val model = BacnetModel()
                            model.jsonContent = response
                            model.equipDevice.value = equipmentDevice
                            model.version.value = version
                            model.points = getBacnetPoints(equipmentDevice.points, isPaired = false)
                            bacnetModel.value = model

                        } else {
                            showErrorDialog(context, NO_MODEL_DATA_FOUND, false)
                        }
                    } catch (e: JsonParseException) {
                        showErrorDialog(context, NO_INTERNET, true)
                    }
                } else {
                    showErrorDialog(context, NO_INTERNET, true)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET, true)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    private fun getModelIdByName(name: String): String {
        return deviceModelList.find { it.name == name }!!.id
    }

    private fun isValidConfiguration(): Boolean {
        if (bacnetModel.value.points.isEmpty()) {
            showToast("Please select bacnet device", context)
            return false
        }
        if (bacnetModel.value.isDevicePaired)
            return true // If it is paired then will not allow the use to to edit slave id

        return true
    }

    private fun getBacnetEquipMap(slaveId: String): HashMap<Any, Any>? {
        return CCUHsApi.getInstance()
            .readEntity("equip and bacnet and not equipRef and group == \"$slaveId\"")
    }

    fun updateSelectAll(b: Boolean, item: BacnetPointState) {
        var isAllSelected = true
        bacnetModel.value.equipDevice.value.points.forEach { bacnetPoint ->
            if(bacnetPoint.id == item.id &&
                bacnetPoint.protocolData?.bacnet?.objectId == item.protocolData?.bacnet?.objectId){
                CcuLog.d(TAG, "updateSelectAll--${bacnetPoint.id}--${item.id} value== $b")
                bacnetPoint.protocolData?.bacnet?.displayInUIDefault  = b
            }
            isAllSelected = isAllSelected && (bacnetPoint.protocolData?.bacnet?.displayInUIDefault == true ||
                    (bacnetPoint.equipTagNames.contains("heartbeat") &&
                            bacnetPoint.equipTagNames.contains("sensor")))
        }
        displayInUi.value = isAllSelected
    }

    fun showErrorDialog(context: Context, message: String, wantToDismiss: Boolean) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(WARNING)
        builder.setIcon(R.drawable.ic_warning)
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton(OK) { dialog, _ ->
            if (wantToDismiss)
                _isDialogOpen.value = false
            dialog.dismiss()
        }
        builder.create().show()
    }

    fun updateData(){
        updateConfiguration()
    }

    private fun updateConfiguration(){
        bacnetProfile.updateBacnetEquip(bacnetProfile.equip.id, bacnetModel.value.equipDevice.value.points)
        pairingCompleteListener.onPairingComplete()
    }

    fun save(){
        saveConfiguration()
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            if (isValidConfiguration()) {
                ProgressDialogUtils.showProgressDialog(context, SAVING_BACNET)

                try {
                    withContext(Dispatchers.IO) {
                        updateOriginalModel()
                        // Perform background tasks
                        CCUHsApi.getInstance().resetCcuReady()
                        setUpBacnetProfile()
                        if (configurationType.value == MSTP_CONFIGURATION) {
                            sendCovSubscription()
                        }
                        L.saveCCUState()
                        CCUHsApi.getInstance().setCcuReady()
                    }

                    // Update UI on the main thread
                    ProgressDialogUtils.hideProgressDialog()
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast(SAVED, context)
                    _isDialogOpen.value = false
                    pairingCompleteListener.onPairingComplete()
                } catch (e: Exception) {
                    // Handle exceptions if needed
                    ProgressDialogUtils.hideProgressDialog()
                    showToast("Error saving configuration: ${e.message}", context)
                    e.printStackTrace()
                }
            }
        }
    }

     fun sendCovSubscription() {

        val destination = DestinationMultiRead(destinationIp.value, destinationPort.value, deviceId.value, dnet.value, destinationMacAddress.value)

        var objectIdentifierList = mutableListOf<ObjectIdentifierBacNet>()
        bacnetModel.value.points.forEach {
            val objectId = it.protocolData?.bacnet?.objectId
            val objectType = it.protocolData?.bacnet?.objectType

            val objectIdentifier = getDetailsFromObjectLayout(objectType!!, objectId.toString())
            objectIdentifierList.add(objectIdentifier)
        }


        val subscribeCovRequest = mutableListOf( BacnetMstpSubscribeCov(
            destination,
            BacnetMstpSubscribeCovRequest(1,objectIdentifierList)
        ))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.subscribeCov(BacnetMstpSubscribeCovForAllDevices( subscribeCovRequest ))
                val resp = BaseResponse(response)
                ProgressDialogUtils.hideProgressDialog()
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response->${readResponse}")
                    } else {
                        CcuLog.d(TAG, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.d(TAG, "--SocketTimeoutException--${e.message}")
            } catch (e: ConnectException) {
                CcuLog.d(TAG, "--ConnectException--${e.message}")
            } catch (e: java.lang.Exception) {
                CcuLog.d(TAG, "--connection time out--${e.message}")
            }
        }
    }

    /*fun saveConfiguration() {
        if (isValidConfiguration()) {
            RxjavaUtil.executeBackgroundTask({
                ProgressDialogUtils.showProgressDialog(context, SAVING_BACNET)
            }, {
                CCUHsApi.getInstance().resetCcuReady()
                setUpBacnetProfile()
                L.saveCCUState()
                CCUHsApi.getInstance().setCcuReady()

            }, {
                ProgressDialogUtils.hideProgressDialog()
                context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                showToast(SAVED, context)
                _isDialogOpen.value = false
            })
        }
    }*/

    private fun setUpBacnetProfile() {
        val groupId = L.generateBacnetNodeAddres()
        CcuLog.d(TAG, "setUpBacnetProfile node address $groupId")

            bacnetProfile = BacnetProfile()
            val deviceID = deviceId.value.ifEmpty { 0 }
            val configParam = "deviceId:${deviceID},destinationIp:${destinationIp.value},destinationPort:${destinationPort.value},macAddress:${destinationMacAddress.value},deviceNetwork:${dnet.value}"
            val modelConfig = "modelName:${modelName.value},modelId:${getModelIdByName(modelName.value)},version:${bacnetModel.value.version.value}"
            bacnetProfile.addBacAppEquip(configParam, modelConfig, deviceID.toString(),
                groupId.toString(), floorRef, zoneRef,
                bacnetModel.value.equipDevice.value,
                profileType,moduleLevel,bacnetModel.value.version.value,configurationType.value, destinationMacAddress.value, false)

            L.ccu().zoneProfiles.add(bacnetProfile)
            L.saveCCUState()
            CcuLog.d(TAG, "setUpBacnetProfile completed")
    }

    //Todo will require in future
    private fun updateBacnetProfile() {
        /*modbusProfile.updateModbusEquip(
            equipModel.value.equipDevice.value.deviceEquipRef,
            equipModel.value.equipDevice.value.slaveId.toShort(),
            getParametersList(equipModel.value)
        )

        equipModel.value.subEquips.forEach {
            modbusProfile.updateModbusEquip(
                it.value.equipDevice.value.deviceEquipRef,
                it.value.equipDevice.value.slaveId.toShort(),
                getParametersList(it.value)
            )
        }
        L.ccu().zoneProfiles.add(modbusProfile)
        L.saveCCUState()*/
    }

    fun fetchData(){

        if(configurationType.value == IP_CONFIGURATION) {
            isDeviceIdValid.value = !isDeviceIdAlreadyInUse(deviceId.value)
            isDestinationIpValid.value = isValidIpAddress(destinationIp.value)
            CcuLog.d(TAG, "--------------fetchData--isDestinationIpInvalidValid-----$isDestinationIpValid--------")
            if(isDestinationIpValid.value.not()){
                return
            }
        } else if (configurationType.value == MSTP_CONFIGURATION) {
               if (destinationMacAddress.value.trim().isEmpty() || destinationMacAddress.value.trim() == "0") {
                   CcuLog.d(TAG,"Mac Address is empty or invalid")
                   return
               } else {
                destinationIp.value = destinationMacAddress.value.trim() + ".00.00.00"
               }

        } else {
            showToastMessage("Please select configuration type")
            return
        }

        deviceValue.value = false
        bacnetPropertiesFetched.value = false
        CcuLog.d(TAG, "--------------fetchData--isDestinationIpInvalidValid-----$deviceIp--------")
        service = if (configurationType.value == MSTP_CONFIGURATION) {
            ServiceManager.makeCcuServiceForMSTP(ipAddress = deviceIp)
        } else {
            ServiceManager.makeCcuService(ipAddress = deviceIp)
        }
        val destination =
            DestinationMultiRead(destinationIp.value, destinationPort.value, deviceId.value, dnet.value, destinationMacAddress.value)
        val readAccessSpecification = mutableListOf<ReadRequestMultiple>()

        bacnetModel.value.points.forEach {
            val objectId = it.protocolData?.bacnet?.objectId
            val objectType = it.protocolData?.bacnet?.objectType

            val objectIdentifier = getDetailsFromObjectLayout(objectType!!, objectId.toString())
            val propertyReference = getDetailsOfProperties(it.bacnetProperties, it)
            readAccessSpecification.add(
                ReadRequestMultiple(
                    objectIdentifier,
                    propertyReference
                )
            )
        }

        val rpmRequest = RpmRequest(readAccessSpecification)
        ProgressDialogUtils.showProgressDialog(context, "Fetching data from device")
        sendRequestMultipleRead(BacnetReadRequestMultiple(destination, rpmRequest))
    }

    private fun fetchDeviceNames(){
        connectedDevices.value.forEach {
            CcuLog.d(TAG, "--deviceId-->${it.deviceId}<--deviceIp-->${it.deviceIp}<--deviceName-->${it.deviceName}<--deviceMacAddress-->${it.deviceMacAddress}<--deviceNetwork-->${it.deviceNetwork}")
            var destinationMacAddress = ""
            if(it.deviceMacAddress != null && it.deviceMacAddress.trim().isNotEmpty()){
                destinationMacAddress = macAddressToByteArray(it.deviceMacAddress)
            }
            service = if(configurationType.value == MSTP_CONFIGURATION) {
                ServiceManager.makeCcuServiceForMSTP(ipAddress = deviceIp)
            } else {
                ServiceManager.makeCcuService(ipAddress = deviceIp)
            }
            val destination = DestinationMultiRead(
                it.deviceIp,
                it.devicePort,
                it.deviceId,
                it.deviceNetwork,
                destinationMacAddress
            )
            val readAccessSpecification = mutableListOf<ReadRequestMultiple>()

            val objectId = it.deviceId
            val objectType = "DEVICE"

            val objectIdentifier = getDetailsFromObjectLayout(objectType, objectId)
            val propertyReference = getPropertyDetailsForDeviceName()
            readAccessSpecification.add(
                ReadRequestMultiple(
                    objectIdentifier,
                    propertyReference
                )
            )

            val rpmRequest = RpmRequest(readAccessSpecification)
            val rpmRequestFinal = BacnetReadRequestMultiple(destination, rpmRequest)
            sendRequestMultipleReadDeviceName(rpmRequestFinal)
        }
    }

    private fun getPropertyDetailsForDeviceName(): MutableList<PropertyReference> {
        val list = mutableListOf<PropertyReference>()
        list.add(
            PropertyReference(
                BacNetConstants.PropertyType.valueOf("PROP_OBJECT_NAME").value,
                -1
            )
        )
        return list
    }

    private fun isDeviceIdAlreadyInUse(deviceId: String): Boolean {
        return isEquipExists(deviceId)
    }

    private fun sendRequestMultipleRead(rpmRequest: BacnetReadRequestMultiple) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.multiread(rpmRequest)
                val resp = BaseResponse(response)
                ProgressDialogUtils.hideProgressDialog()
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            updateUi(readResponse)
                        }
                    } else {
                        CcuLog.d(TAG, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.d(TAG, "--SocketTimeoutException--${e.message}")
                showToastMessage("SocketTimeoutException")
            } catch (e: ConnectException) {
                CcuLog.d(TAG, "--ConnectException--${e.message}")
                showToastMessage("ConnectException")
            } catch (e: java.lang.Exception) {
                CcuLog.d(TAG, "--connection time out--${e.message}")
            }
        }
    }

    private fun sendRequestMultipleReadDeviceName(rpmRequest: BacnetReadRequestMultiple) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.multiread(rpmRequest)
                val resp = BaseResponse(response)
                ProgressDialogUtils.hideProgressDialog()
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response for device names->${readResponse}")
                        if (readResponse != null && readResponse.rpResponse.listOfItems.size > 0) {
                            try {
                                val objectInstance = readResponse.rpResponse.listOfItems[0].objectIdentifier.objectInstance
                                val deviceName = readResponse.rpResponse.listOfItems[0].results[0].propertyValue.value
                                CcuLog.d(TAG, "deviceName->${deviceName} ---->objectInstance<--$objectInstance")
                                updateDeviceNameOnUi(deviceName, objectInstance)
                            }catch (e : Exception){
                                e.printStackTrace()
                            }
                        }
                    } else {
                        CcuLog.d(TAG, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.d(TAG, "--SocketTimeoutException--${e.message}")
                showToastMessage("SocketTimeoutException")
            } catch (e: ConnectException) {
                CcuLog.d(TAG, "--ConnectException--${e.message}")
                showToastMessage("ConnectException")
            } catch (e: java.lang.Exception) {
                CcuLog.d(TAG, "--connection time out--${e.message}")
            }
        }
    }

    private fun updateDeviceNameOnUi(deviceName: String, objectInstance: String) {
        connectedDevices.value = connectedDevices.value.map { device ->
            if (device.deviceId == objectInstance) {
                device.copy(deviceName = deviceName) // Create a new instance
            } else {
                device
            }
        }
    }

    private fun getDetailsOfProperties(
        listOfProperties: MutableList<BacnetProperty>?,
        bacnetPointState: BacnetPointState
    ): MutableList<PropertyReference> {
        val list = mutableListOf<PropertyReference>()
        listOfProperties?.forEach {
            list.add(
                PropertyReference(
                    it.id,
                    -1
                )
            )
        }
//        if(bacnetPointState.equipTagNames.contains("writable")){
//            for (i in 1..16) {
//                list.add(
//                    PropertyReference(
//                        87,
//                        i
//                    )
//                )
//            }
//        }
        return list
    }

    private fun populateBacnetDevices(whoIsResponseList: MutableList<WhoIsResponseItem>) {
        val list = mutableListOf<BacnetDevice>()
        whoIsResponseList.forEach { item ->
            list.add(
                BacnetDevice(
                    item.deviceIdentifier,
                    item.ipAddress,
                    item.networkNumber,
                    item.vendorIdentifier,
                    item.portNumber,
                    item.macAddress
                )
            )
        }
        connectedDevices = mutableStateOf(emptyList())
        connectedDevices.value = list

        if (connectedDevices.value.isEmpty() ) {
            ProgressDialogUtils.hideProgressDialog()
            Toast.makeText(context, "No devices found", Toast.LENGTH_SHORT).show()
        }

        fetchDeviceNames()

    }

    private fun fetchConnectedDeviceGlobally() {
        service = if (configurationType.value == MSTP_CONFIGURATION) {
            ServiceManager.makeCcuServiceForMSTP(deviceIp)
        } else {
            ServiceManager.makeCcuService(deviceIp)
        }
        CcuLog.d(TAG, "fetchConnectedDevice for ${deviceId.value} -- ${destinationIp.value} -- ${destinationPort.value} -- ${destinationMacAddress.value} -- ${dnet.value}")
        try {
            val broadCastValue = "global"

           val srcDeviceID = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_MSTP_DEVICE_ID,0).toString()
            val bacnetWhoIsRequest = if(configurationType.value == IP_CONFIGURATION) {
                BacnetWhoIsRequest(
                    WhoIsRequest(
                        "",
                        ""
                    ),
                    BroadCast(broadCastValue),
                    devicePort,
                    deviceIp
                )
            } else {
                BacnetWhoIsRequest(
                    WhoIsRequest(
                        "",
                        ""
                    ),
                    BroadCast(broadCastValue),
                    devicePort,
                    deviceIp,
                    srcDeviceId = srcDeviceID
                )
            }
            val request = Gson().toJson(
                bacnetWhoIsRequest
            )
            CcuLog.d(TAG, "this is the broadcast request-->$request")
            sendRequest(bacnetWhoIsRequest)
        }catch (e : NumberFormatException){
            CcuLog.d(TAG, "please provide valid input - ${e.message}")
        }
    }

    private fun sendRequest(bacnetWhoIsRequest: BacnetWhoIsRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.whois(bacnetWhoIsRequest)
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            if (readResponse != null && !readResponse.whoIsResponseList.isNullOrEmpty()) {
                                populateBacnetDevices(readResponse.whoIsResponseList)
                                isConnectedDevicesSearchFinished.value = true
                            }else{
                                CcuLog.d(TAG, "no devices found")
                                showToastMessage("No devices found check configuration and initialize bacnet again")
                            }
                        }
                    } else {
                        CcuLog.d(TAG, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.d(TAG, "--SocketTimeoutException--${e.message}")
                showToastMessage("SocketTimeoutException")
            } catch (e: ConnectException) {
                CcuLog.d(TAG, "--ConnectException--${e.message}")
                showToastMessage("ConnectException")
            } catch (e: java.lang.Exception) {
                showToastMessage("connection time out")
                CcuLog.d(TAG, "--connection time out--${e.message}")
            }
            isConnectedDevicesSearchFinished.value = true
        }
    }

    private fun showToastMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            isErrorMsg.value = true
            errorMsg = message
        }
    }

    private fun updateUi(readResponse: MultiReadResponse?) {
        if (readResponse != null) {
            if (readResponse.error != null) {
                val errorCode = BacNetConstants.BacnetErrorCodes.from(readResponse.error!!.errorCode.toInt())
                val errorClass = BacNetConstants.BacnetErrorClasses.from(readResponse.error!!.errorClass.toInt())
                showToastMessage("error code->${errorCode}--error class->${errorClass}")
            } else if(readResponse.errorAbort != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAbortErrors.from(
                    readResponse.errorAbort!!.abortReason.toInt())}")
            }else if(readResponse.errorBacApp != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.errorBacApp!!.abortReason.toInt())}")
            }else if(readResponse.errorReject != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetRejectErrors.from(
                    readResponse.errorReject!!.abortReason.toInt())}")
            }else if(readResponse.errorASide != null){
                showToastMessage("abort reason->${readResponse.errorASide!!.abortReason}")
            }else {
                if(readResponse.rpResponse != null){
                    for (item in readResponse.rpResponse.listOfItems) {
                        item.results.forEach() {
                            if(it.propertyIdentifier == "87"){
                                if(it.propertyValue != null && it.propertyValue.type == "34"){
                                    val key = "${item.objectIdentifier.objectInstance}-${item.objectIdentifier.objectType}-${it.propertyIdentifier}"
                                    bacNetItemsMap.putIfAbsent(key, item)
                                    //bacNetItemsMap[key] = item
                                }
                            }else{
                                val key = "${item.objectIdentifier.objectInstance}-${item.objectIdentifier.objectType}-${it.propertyIdentifier}"
                                bacNetItemsMap[key] = item
                                CcuLog.d(TAG, "key is -->$key and value is -->${bacNetItemsMap[key]}")
                            }
                        }
                    }

                    CcuLog.d(TAG,"result map size-->${bacNetItemsMap.size}")
                    updateBacnetProperties()
                    bacnetPropertiesFetched.value = true
                    Toast.makeText(context, "Fetched Properties from Bacnet", Toast.LENGTH_LONG).show()
                }else{
                    CcuLog.d(TAG,"BAC app failed check logs--")
                    bacnetRequestFailed.value = true
                }
            }
        }
    }

    private fun updateBacnetProperties(){
        bacnetModel.value.points.forEach { point ->
            var isChangeInProperty = false
            point.displayInEditor.value = false
            val writableLevelKey = "${point.protocolData?.bacnet?.objectId}-${BacNetConstants.ObjectType.valueOf("OBJECT_"+point.protocolData?.bacnet?.objectType).value}-87"
            if(bacNetItemsMap[writableLevelKey] != null){
                val results = bacNetItemsMap[writableLevelKey]?.results
                if (results != null) {
                    for (item in results) {
                        if(item.propertyValue.type == "34"){
                            if(item.propertyArrayIndex != null){
                                point.defaultWriteLevel = item.propertyArrayIndex!!
                                CcuLog.d(TAG,"default write level is -->${item.propertyArrayIndex}")
                                break
                            }
                        }
                    }
                }
            }
            point.bacnetProperties?.forEach { bacnetProperty ->
                val key = "${point.protocolData?.bacnet?.objectId}-${BacNetConstants.ObjectType.valueOf("OBJECT_"+point.protocolData?.bacnet?.objectType).value}-${bacnetProperty.id}"
                bacnetProperty.fetchedValue =
                    bacNetItemsMap[key]?.results?.get(0)?.propertyValue?.value ?: "-"
                CcuLog.d(TAG, "Fetching key is -->$key and value is -->${bacNetItemsMap[key]} and fetched value is ${bacnetProperty.fetchedValue} and default value is ${bacnetProperty.defaultValue}")
                if(bacnetProperty.defaultValue != bacnetProperty.fetchedValue && !isChangeInProperty){
                    point.displayInEditor.value = true
                    isChangeInProperty = true
                }
            }
        }
    }

    fun updateDisplayInUiModules(b: Boolean) {
        for (point in bacnetModel.value.points) {
            point.displayInUi.value = b
        }
        bacnetModel.value.equipDevice.value.points.forEach { bacnetPoint ->
            bacnetPoint.protocolData?.bacnet?.displayInUIDefault = b
        }
    }

    fun searchDevices() {
        isConnectedDevicesSearchFinished.value = false
        fetchConnectedDeviceGlobally()
    }

    fun update() {
        bacnetModel = mutableStateOf(bacnetModel.value)
    }

    private fun isValidIpAddress(ip: String): Boolean {
        val octets = ip.split(".")
        if (octets.size != 4) {
            return false
        }
        for (octet in octets) {
            val number = octet.toIntOrNull()
            if (number == null || number < 0 || number > 255) {
                return false
            }
        }
        return true

    }

    private fun isEquipExists(equipId: String): Boolean {
        CcuLog.d(TAG, "isEquipExists --> $equipId")
        val equip: HashMap<*, *> = CCUHsApi.getInstance().read("equip and bacnet and group == \"$equipId\"")
        return equip.isNotEmpty()
    }

    fun updateDeviceValueInUiModules(b: Boolean) {
        isDeviceValueSelected.value = b
        val testBacnetModel = bacnetModel.value
        for (point in testBacnetModel.points) {
            point.bacnetProperties?.forEach { bacnetProperty ->
                if (b && ( bacnetProperty.fetchedValue != "-" && !bacnetProperty.fetchedValue.isNullOrEmpty()) ) {
                    bacnetProperty.selectedValue = 1
                    CcuLog.d(TAG, "bacnetProperty.fetchedValue is ${bacnetProperty.fetchedValue} and selected value is 1")
                } else {
                    bacnetProperty.selectedValue = 0
                    CcuLog.d(TAG, "bacnetProperty.fetchedValue is ${bacnetProperty.fetchedValue} and selected value is 0")
                }
            }
        }

        for (point in bacnetModel.value.equipDevice.value.points) {
            point.bacnetProperties.forEach() { bacnetProperty ->
                if (b && (bacnetProperty.fetchedValue != "-" && !bacnetProperty.fetchedValue.isNullOrEmpty())) {
                    CcuLog.d(TAG, "bacnetProperty.fetchedValue is ${bacnetProperty.fetchedValue} and selected value is 1")
                    bacnetProperty.selectedValue = 1
                } else {
                    CcuLog.d(TAG, "bacnetProperty.fetchedValue is ${bacnetProperty.fetchedValue} and selected value is 0")
                    bacnetProperty.selectedValue = 0
                }
            }
        }

        bacnetModel.value = testBacnetModel
        isStateChanged.value = !isStateChanged.value

    }

    private fun updateOriginalModel() {
        val mapOfModifiedPoints = mutableMapOf<String, BacnetPointState>()
        val testBacnetModel = bacnetModel.value

        for (point in testBacnetModel.points) {
            val key = "${point.id}-${point.protocolData?.bacnet?.objectId}"
            //mapOfModifiedPoints[point.id] = point
            mapOfModifiedPoints[key] = point
        }

        testBacnetModel.equipDevice.value.points.forEach { point ->
            val key = "${point.id}-${point.protocolData?.bacnet?.objectId}"
            //val modifiedPoint = mapOfModifiedPoints[point.id]
            val modifiedPoint = mapOfModifiedPoints[key]
            if (modifiedPoint != null) {
                point.defaultWriteLevel = modifiedPoint.defaultWriteLevel
                point.bacnetProperties = modifiedPoint.bacnetProperties.toMutableList()
            }
        }

    }

    fun updatePropertyStatus(
        i: Int,
        pointId: String,
        bacnetProperty: MutableState<BacnetProperty>,
        objectId: Int?
    ) {
        val updatedPoints = bacnetModel.value.points.map { point ->
            val keyFromPoint = "${point.id}-${point.protocolData?.bacnet?.objectId}"
            val keyFromUpdate = "${pointId}-${objectId}"
            if (keyFromPoint == keyFromUpdate) {
            //if (point.id == pointId) {
                val updatedProperties = point.bacnetProperties.map { bacnetProp ->
                    if (bacnetProp.id == bacnetProperty.value.id) {
                        bacnetProp.copy(selectedValue = i)
                    } else {
                        bacnetProp
                    }
                }
                point.copy(bacnetProperties = updatedProperties.toMutableList())
            } else {
                point
            }
        }
        bacnetModel.value.points = updatedPoints.toMutableStateList()
        CcuLog.d(TAG, "Updated point is $pointId and property is ${bacnetProperty.value.id} value is $i")
    }
    fun applyCopiedConfiguration() {
        val copiedModbusConfiguration = CopyConfiguration.getCopiedBacnetConfiguration()

        if(bacnetModel.value.isDevicePaired){
            bacnetModel.value.points.forEach { bacNetPoint ->
                copiedModbusConfiguration.value.points.forEach {
                    if (bacNetPoint.disName.equals(it.disName,true)) {
                        bacNetPoint.displayInUi.value = it.displayInUi.value
                        bacNetPoint.protocolData?.bacnet?.displayInUIDefault = it.displayInUi.value
                        bacNetPoint.isSchedulable.value = it.isSchedulable.value
                    }
                }
            }
        } else {
            bacnetModel.value.points.forEach { bacNetPoint ->
                copiedModbusConfiguration.value.points.forEach {
                    if (bacNetPoint.domainName.equals(it.disName.replace(" ", "").toLowerCase(), true) || bacNetPoint.name.equals(it.disName, true)) {
                        bacNetPoint.displayInUi.value = it.displayInUi.value
                        bacNetPoint.protocolData?.bacnet?.displayInUIDefault = it.displayInUi.value
                        bacNetPoint.isSchedulable.value = it.isSchedulable.value
                    }
                }
            }
        }
        disablePasteConfiguration()
        formattedToastMessage(context.getString(R.string.Toast_Success_Message_paste_Configuration), context)
    }

    fun isCopiedConfigurationAvailable() {
        if(getSelectedBacNetModel() != null && moduleLevel!="system" && (bacnetModel.value.isDevicePaired == false || bacnetModel.value.isDevicePaired == true) && modelName.value.equals(getSelectedBacNetModel(),true)){
            enablePasteConfiguration()
        }
      else{
            disablePasteConfiguration()
        }
    }

    fun disablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = false
        }
    }
    private fun enablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = true
        }
    }

    fun updateSchedulableEnableState(b: Boolean, item: BacnetPointState) {
        bacnetModel.value.equipDevice.value.points.find { bacnetPoint -> (bacnetPoint.id == item.id &&
                bacnetPoint.protocolData?.bacnet?.objectId == item.protocolData?.bacnet?.objectId) }
            ?. let { bacnetPoint ->
                CcuLog.d(TAG, "update SchedulableEnable State --${bacnetPoint.id}--${item.id} value== $b")
                bacnetPoint.isSchedulable  = b
            }
    }

    fun clearConfigFieldData() {
        deviceId.value = ""
        destinationPort.value = ""
        dnet.value = ""
        destinationIp.value = ""
        destinationMacAddress.value = ""
        connectedDevices = mutableStateOf(emptyList())
    }
}
