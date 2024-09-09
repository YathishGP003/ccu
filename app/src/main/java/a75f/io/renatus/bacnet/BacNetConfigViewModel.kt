package a75f.io.renatus.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.bacnet.parser.BacnetPoint
import a75f.io.api.haystack.bacnet.parser.BacnetProperty
import a75f.io.device.bacnet.buildBacnetModel
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.bacnet.BacnetProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants
import a75f.io.renatus.ENGG.bacnet.services.BacnetReadRequestMultiple
import a75f.io.renatus.ENGG.bacnet.services.BacnetWhoIsRequest
import a75f.io.renatus.ENGG.bacnet.services.BroadCast
import a75f.io.renatus.ENGG.bacnet.services.DestinationMultiRead
import a75f.io.renatus.ENGG.bacnet.services.MultiReadResponse
import a75f.io.renatus.ENGG.bacnet.services.ObjectIdentifierBacNet
import a75f.io.renatus.ENGG.bacnet.services.PropertyReference
import a75f.io.renatus.ENGG.bacnet.services.ReadRequestMultiple
import a75f.io.renatus.ENGG.bacnet.services.RpResponseMultiReadItem
import a75f.io.renatus.ENGG.bacnet.services.RpmRequest
import a75f.io.renatus.ENGG.bacnet.services.WhoIsRequest
import a75f.io.renatus.ENGG.bacnet.services.WhoIsResponseItem
import a75f.io.renatus.ENGG.bacnet.services.client.BaseResponse
import a75f.io.renatus.ENGG.bacnet.services.client.CcuService
import a75f.io.renatus.ENGG.bacnet.services.client.ServiceManager
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.bacnet.models.BacnetDevice
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.bacnet.models.BacnetPointState
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
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.util.ProgressDialogUtils
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.Objects
import kotlin.properties.Delegates


class BacNetConfigViewModel(application: Application) : AndroidViewModel(application) {

    var deviceSelectionMode = mutableStateOf(0)
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

    val isUpdateMode = mutableStateOf(false)
    val displayInUi = mutableStateOf(true)
    val deviceValue = mutableStateOf(false)
    val bacnetRequestFailed = mutableStateOf(false)
    val bacnetPropertiesFetched = mutableStateOf(false)
    var isStateChanged = mutableStateOf(false)
    var isDeviceValueSelected = mutableStateOf(false)

    var modelName = mutableStateOf("Select Model")

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

    val bacNetItemsMap = hashMapOf<String, RpResponseMultiReadItem>()
    private lateinit var service : CcuService

    val onItemSelect = object : OnItemSelect {
        override fun onItemSelected(index: Int, item: String) {
            modelName.value = item
            ProgressDialogUtils.showProgressDialog( context, "Fetching $item details")
            fetchModelDetails(item)
        }
    }

    val onBacnetDeviceSelect = object : OnItemSelectBacnetDevice {
        override fun onItemSelected(index: Int, item: BacnetDevice) {
            CcuLog.d(TAG, "onItemSelected-->$item")
            destinationIp.value = item.deviceIp
            deviceId.value = item.deviceId
            destinationPort.value = item.devicePort
            destinationMacAddress.value = item.deviceMacAddress?.let { convertMacAddress(it) } ?: ""
        }
    }

    fun convertMacAddress(macAddress: String): String {
        val hexArray = macAddress.split(":")
        val sb = StringBuilder()

        for (i in hexArray.indices.reversed()) {
            // Convert each hex value to an integer
            val decimalValue = hexArray[i].toInt(16)
            sb.append(decimalValue)

            if (i > 0) {
                sb.append(".")
            }
        }

        return sb.toString()
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
                model.selectAllParameters.value = isAllParamsSelected(equipmentDevice[0])
                model.points = getBacnetPoints(equipmentDevice[0].points.toImmutableList()) //equipmentDevice[0].points
                bacnetModel.value = model
                bacnetModel.value.isDevicePaired = true


                model.equipDevice.value.bacnetConfig?.let { config ->
                    config.split(",").forEach {
                        val configParam = it.split(":")
                        when(configParam[0]){
                            "deviceId" -> deviceId.value = configParam[1]
                            "destinationIp" -> destinationIp.value = configParam[1]
                            "destinationPort" -> destinationPort.value = configParam[1]
                            "macAddress" -> destinationMacAddress.value = configParam[1]
                        }
                    }
                }
                CcuLog.d(TAG, "--> deviceId ${deviceId.value} destinationIp ${destinationIp.value} destinationPort ${destinationPort.value} macAddress ${destinationMacAddress.value}")
                model.equipDevice.value.modelConfig?.let { config ->
                    config.split(",").forEach {
                        val configParam = it.split(":")
                        when (configParam[0]) {
                            "modelName" -> modelName.value = configParam[1]
                        }
                    }
                }
            }
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

    private fun isAllParamsSelected(equipDevice: BacnetModelDetailResponse) : Boolean {
        var isAllSelected = true
        if (equipDevice.points.isNotEmpty()) {
            equipDevice.points.forEach {
                if (!it.protocolData?.bacnet?.displayInUIDefault!!)
                    isAllSelected = false
            }
        }
        return isAllSelected
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
        domainService.readBacNetModelsList(/*filer*/"BACNET", object : ResponseCallback {
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
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET, true)
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
                            model.points = getBacnetPoints(equipmentDevice.points)
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
        bacnetModel.value.equipDevice.value.points.forEach { bacnetPoint ->
            if(bacnetPoint.id == item.id){
                CcuLog.d(TAG, "updateSelectAll--${bacnetPoint.id}--${item.id} value== $b")
                bacnetPoint.protocolData?.bacnet?.displayInUIDefault  = b
            }
        }
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

    private fun getBacnetPoints(points: List<BacnetPoint>): MutableList<BacnetPointState> {
        val parameterList = mutableListOf<BacnetPointState>()
        if (Objects.nonNull(points)) {
            for (bacnetPoint in points) {
                val bacnetPointState = BacnetPointState(
                    bacnetPoint.id,
                    bacnetPoint.name,
                    bacnetPoint.domainName,
                    bacnetPoint.kind,
                    bacnetPoint.valueConstraint,
                    bacnetPoint.hisInterpolate,
                    bacnetPoint.protocolData,
                    bacnetPoint.defaultUnit,
                    bacnetPoint.defaultValue,
                    bacnetPoint.equipTagNames,
                    bacnetPoint.rootTagNames,
                    bacnetPoint.descriptiveTags,
                    bacnetPoint.equipTagsList,
                    bacnetPoint.bacnetProperties,
                    displayInUi = mutableStateOf(bacnetPoint.protocolData?.bacnet!!.displayInUIDefault),
                    disName = bacnetPoint.disName
                )
                parameterList.add(bacnetPointState)
            }
        }
        return parameterList
    }

    fun updateData(){
        updateConfiguration()
    }

    private fun updateConfiguration(){
        bacnetProfile.updateBacnetEquip(bacnetProfile.equip.id, bacnetModel.value.equipDevice.value.points)
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
                        L.saveCCUState()
                        CCUHsApi.getInstance().setCcuReady()
                    }

                    // Update UI on the main thread
                    ProgressDialogUtils.hideProgressDialog()
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast(SAVED, context)
                    _isDialogOpen.value = false
                } catch (e: Exception) {
                    // Handle exceptions if needed
                    ProgressDialogUtils.hideProgressDialog()
                    showToast("Error saving configuration: ${e.message}", context)
                }
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
        CcuLog.d(TAG, "setUpBacnetProfile node address ${deviceId.value}")
        val parentMap = getBacnetEquipMap(deviceId.value)
        if (parentMap.isNullOrEmpty()) {

            bacnetProfile = BacnetProfile()
            val configParam = "deviceId:${deviceId.value},destinationIp:${destinationIp.value},destinationPort:${destinationPort.value},macAddress:${destinationMacAddress.value}"
            val modelConfig = "modelName:${modelName.value},modelId:${getModelIdByName(modelName.value)}"
            bacnetProfile.addBacAppEquip(configParam, modelConfig, deviceId.value, deviceId.value, floorRef, zoneRef,
                bacnetModel.value.equipDevice.value,
                profileType,moduleLevel,bacnetModel.value.version.value)

            L.ccu().zoneProfiles.add(bacnetProfile)
            L.saveCCUState()
            CcuLog.d(TAG, "setUpBacnetProfile completed")
        } else {
            updateBacnetProfile()
        }
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

        isDeviceIdValid.value = !isDeviceIdAlreadyInUse(deviceId.value)

        isDestinationIpValid.value = isValidIpAddress(destinationIp.value)
        CcuLog.d(TAG, "--------------fetchData--isDestinationIpInvalidValid-----$isDestinationIpValid--------")
        if(isDestinationIpValid.value.not()){
            return
        }

        deviceValue.value = false
        bacnetPropertiesFetched.value = false
        CcuLog.d(TAG, "--------------fetchData--isDestinationIpInvalidValid-----$deviceIp--------")
        service = ServiceManager.makeCcuService(ipAddress = deviceIp)
        val destination =
            DestinationMultiRead(destinationIp.value, destinationPort.value, deviceId.value, "2", destinationMacAddress.value)
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

    private fun getDetailsFromObjectLayout(objectType : String, objectId: String): ObjectIdentifierBacNet {
        return ObjectIdentifierBacNet(
            BacNetConstants.ObjectType.valueOf("OBJECT_$objectType").value,
            objectId
        )
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
        if(bacnetPointState.equipTagNames.contains("writable")){
            for (i in 1..16) {
                list.add(
                    PropertyReference(
                        87,
                        i
                    )
                )
            }
        }
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
    }

    private fun fetchConnectedDeviceGlobally() {
        service = ServiceManager.makeCcuService(deviceIp)
        CcuLog.d(TAG, "fetchConnectedDevice for ${deviceId.value} -- ${destinationIp.value} -- ${destinationPort.value} -- ${destinationMacAddress.value}")
        try {
            val broadCastValue = "global"

            val bacnetWhoIsRequest = BacnetWhoIsRequest(
                WhoIsRequest(
                    "",
                    ""
                ),
                BroadCast(broadCastValue),
                devicePort,
                deviceIp
            )
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
                val errorCode = BacNetConstants.BacnetErrorCodes.from(readResponse.error.errorCode.toInt())
                val errorClass = BacNetConstants.BacnetErrorClasses.from(readResponse.error.errorClass.toInt())
                showToastMessage("error code->${errorCode}--error class->${errorClass}")
            } else if(readResponse.errorAbort != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAbortErrors.from(readResponse.errorAbort.abortReason.toInt())}")
            }else if(readResponse.errorBacApp != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.errorBacApp.abortReason.toInt())}")
            }else if(readResponse.errorReject != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetRejectErrors.from(readResponse.errorReject.abortReason.toInt())}")
            }else if(readResponse.errorASide != null){
                showToastMessage("abort reason->${readResponse.errorASide.abortReason}")
            }else {
                if(readResponse.rpResponse != null){
                    for (item in readResponse.rpResponse.listOfItems) {
                        item.results.forEach() {
                            if(it.propertyIdentifier == "87"){
                                if(it.propertyValue != null && it.propertyValue.type == "34"){
                                    val key = "${item.objectIdentifier.objectInstance}-${it.propertyIdentifier}"
                                    bacNetItemsMap.putIfAbsent(key, item)
                                    //bacNetItemsMap[key] = item
                                }
                            }else{
                                val key = "${item.objectIdentifier.objectInstance}-${it.propertyIdentifier}"
                                bacNetItemsMap[key] = item
                            }
                        }
                    }

                    CcuLog.d(TAG,"result map size-->${bacNetItemsMap.size}")
                    updateBacnetProperties()
                    bacnetPropertiesFetched.value = true
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
            val writableLevelKey = "${point.protocolData?.bacnet?.objectId}-87"
            if(bacNetItemsMap[writableLevelKey] != null){
                bacNetItemsMap[writableLevelKey]?.results?.get(0)?.propertyArrayIndex?.let {
                    point.defaultWriteLevel = it
                }
            }
            point.bacnetProperties?.forEach { bacnetProperty ->
                val key = "${point.protocolData?.bacnet?.objectId}-${bacnetProperty.id}"
                bacnetProperty.fetchedValue =
                    bacNetItemsMap[key]?.results?.get(0)?.propertyValue?.value ?: "NA"

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
                if (b) {
                    bacnetProperty.selectedValue = 1
                } else {
                    bacnetProperty.selectedValue = 0
                }
            }
        }
        bacnetModel.value = testBacnetModel
        isStateChanged.value = !isStateChanged.value

        for (point in bacnetModel.value.equipDevice.value.points) {
            point.bacnetProperties.forEach() { bacnetProperty ->
                if (b) {
                    bacnetProperty.selectedValue = 1
                } else {
                    bacnetProperty.selectedValue = 0
                }
            }
        }
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
        bacnetModel.value.points = updatedPoints.toMutableList()
        CcuLog.d(TAG, "Updated point is $pointId and property is ${bacnetProperty.value.id} value is $i")
    }
}
