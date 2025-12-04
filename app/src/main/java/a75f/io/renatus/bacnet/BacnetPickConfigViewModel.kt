package a75f.io.renatus.bacnet


import a75f.io.domain.service.DomainService
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.bacnet.BacnetProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.BacnetReadRequestMultiple
import a75f.io.logic.bo.building.system.BacnetWhoIsRequest
import a75f.io.logic.bo.building.system.BroadCast
import a75f.io.logic.bo.building.system.DestinationMultiRead
import a75f.io.logic.bo.building.system.PropertyReference
import a75f.io.logic.bo.building.system.ReadRequestMultiple
import a75f.io.logic.bo.building.system.RpmRequest
import a75f.io.logic.bo.building.system.WhoIsRequest
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.CcuService
import a75f.io.logic.bo.building.system.client.RpResponseMultiReadItem
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.logic.bo.building.system.client.WhoIsResponseItem
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PORT
import a75f.io.logic.util.bacnet.getDetailsFromObjectLayout
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants
import a75f.io.renatus.bacnet.models.BacnetDevice
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.bacnet.util.IP_CONFIGURATION
import a75f.io.renatus.bacnet.util.MSTP_CONFIGURATION
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.modbus.util.OnItemSelectBacnetDevice
import a75f.io.renatus.modbus.util.getBacnetPoints
import a75f.io.renatus.modbus.util.isAllParamsSelectedBacNet
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.util.ProgressDialogUtils
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import java.net.ConnectException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import kotlin.properties.Delegates


class BacnetPickConfigViewModel(application: Application) : AndroidViewModel(application) {

    var deviceId = mutableStateOf("")
    private lateinit var service: CcuService
    var destinationIp = mutableStateOf("")
    var destinationPort = mutableStateOf("")
    var destinationMacAddress = mutableStateOf("")
    var dnet = mutableStateOf("0")
    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context
    lateinit var deviceIp: String
    lateinit var devicePort: String
    private lateinit var bacnetProfile: BacnetProfile
    var bacnetModel = mutableStateOf(BacnetModel())
    var connectedDevices = mutableStateOf(emptyList<BacnetDevice>())
    var deviceSelectionMode = mutableStateOf(0)
    var mstpDeviceSelectionMode = mutableStateOf(0) // 0 is Slave
    val TAG = "BacNetPickConfigViewModel"
    var deviceList = mutableStateOf(emptyList<String>())
    val isConnectedDevicesSearchFinished = mutableStateOf(false)
    val isAutoFetchSelected = mutableStateOf(false)
    var isErrorMsg = mutableStateOf(false)
    var errorMsg = ""
    var isDeviceIdValid = mutableStateOf(true)
    var isDestinationIpValid = mutableStateOf(true)


    val isUpdateMode = mutableStateOf(false)
    val displayInUi = mutableStateOf(true)
    val deviceValue = mutableStateOf(false)
    val bacnetRequestFailed = mutableStateOf(false)
    val bacnetPropertiesFetched = mutableStateOf(false)
    var isStateChanged = mutableStateOf(false)
    var isDeviceValueSelected = mutableStateOf(false)

    private val _searchFinished = MutableStateFlow(false)
    val searchFinished: StateFlow<Boolean> = _searchFinished
    private val _searchFailed = MutableStateFlow(false)
    val searchFailed: StateFlow<Boolean> = _searchFailed

    var modelName = mutableStateOf("Select Model")
    var configurationType = mutableStateOf("Select Configuration Type")
    private lateinit var pairingCompleteListener: OnPairingCompleteListener

    // private lateinit var bacnetProfile: BacnetProfile
    private lateinit var filer: String

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var moduleLevel: String
    lateinit var profileType: ProfileType
    lateinit var deviceModelList: List<ModelMetaData>

//    lateinit var deviceIp: String
//    lateinit var devicePort: String
//    @SuppressLint("StaticFieldLeak")
//    lateinit var context: Context

    private var selectedSlaveId by Delegates.notNull<Long>()
    private var domainService = DomainService()
    private val _isDialogOpen = MutableLiveData<Boolean>()
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen
    private val _isDisabled = MutableLiveData(false)
    val isDisabled: LiveData<Boolean> = _isDisabled

    val bacNetItemsMap = hashMapOf<String, RpResponseMultiReadItem>()

    var openModelSelectionAfterDeviceClick = mutableStateOf(false)
    //var selectedDeviceForModel: BacnetDevice? = null
    var selectedDeviceForModel = mutableStateOf<BacnetDevice?>(null)

    fun clearSelectedDeviceForModel() {
        selectedDeviceForModel.value = null
    }

    private val _selectedConfig = MutableStateFlow("Select Configuration")
    val selectedConfig: StateFlow<String> = _selectedConfig

    private val _isAutoDiscoveryEnabled = MutableStateFlow(false)
    val isAutoDiscoveryEnabled: StateFlow<Boolean> = _isAutoDiscoveryEnabled

    fun setConfig(config: String) {
        _selectedConfig.value = config
        _isAutoDiscoveryEnabled.value = config != "Select Configuration"

    }

    //Designed to read network configuration details from SharedPreferences and return them as a Triple<String, String, String>

    fun getNetworkDetails(): Triple<String, String, String>? {
        val configJson = Globals.getInstance().applicationContext
            .getSharedPreferences(
                Globals.getInstance().defaultSharedPreferencesName,
                Context.MODE_PRIVATE
            )
            .getString(BacnetConfigConstants.BACNET_CONFIGURATION, "")

        return configJson?.let {
            try{
                val configMap = Gson().fromJson(it, Map::class.java)

                val networkMap = Gson().fromJson(Gson().toJson(configMap["network"]), Map::class.java)
                val deviceMap = Gson().fromJson(Gson().toJson(configMap["device"]), Map::class.java)

                val ipAddress = networkMap[IP_ADDRESS]?.toString() ?: ""
                val port = networkMap[PORT]?.toString() ?: ""
                val deviceId = deviceMap[IP_DEVICE_INSTANCE_NUMBER]?.toString() ?: ""

                Triple(ipAddress, port, deviceId)

            }catch (Exception: Exception ){
                null
            }


        }
    }

    fun sendWhoIsBroadcast(context: Context,configurationType: String) {
        val service = if (configurationType == BacnetConfigConstants.MSTP_CONFIGURATION) {
            ServiceManager.makeCcuServiceForMSTP()
        } else {
            ServiceManager.makeCcuService()
        }

        val networkDetails = getNetworkDetails()
        if (networkDetails == null || networkDetails.first.isEmpty()) {
            CcuLog.d(TAG, "ERROR: Could not fetch device IP")
            return
        }


        val (localIp, port, deviceId) = networkDetails
        val broadCastValue = "global"

        CcuLog.d(TAG, "LOCAL DEVICE IP → $localIp, PORT → $port, DEVICE ID → $deviceId")

        //Builds WHO-IS Request JSON
        val req = if (configurationType == BacnetConfigConstants.MSTP_CONFIGURATION) {
            BacnetWhoIsRequest(
                WhoIsRequest(
                    "",
                    ""
                ),
                BroadCast(broadCastValue),
                port,
                localIp
            )
        } else {
            BacnetWhoIsRequest(
                WhoIsRequest(
                    "",
                    ""
                ),
                BroadCast(broadCastValue),
                port,
                localIp,
                deviceId
            )
        }

        CcuLog.d(TAG, "WHO-IS REQUEST JSON → ${Gson().toJson(req)}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.whois(req)//Sends WHO-IS via Retrofit
                val base = BaseResponse(response)

                if (!response.isSuccessful) {
                    CcuLog.d(TAG, "WHO-IS ERROR → ${base.error}")
                    return@launch
                }

                val body = base.data?.body()
                if (body == null) {
                    CcuLog.d(TAG, "WHO-IS: NULL body")
                    return@launch
                }

                CoroutineScope(Dispatchers.Main).launch {
                    if (body != null && !body.whoIsResponseList.isNullOrEmpty()) {
                        populateBacnetDevices(body.whoIsResponseList)//Handles success
                        _searchFinished.value = true

                    }else{
                        CcuLog.d(TAG, "no devices found")
                        showToastMessage("No devices found check configuration and initialize bacnet again")
                        _searchFailed.value = true
                    }
                }

                val list = body.whoIsResponseList
                CcuLog.d(TAG, "WHO-IS DEVICES → $list")

            } catch (e: Exception) {
                CcuLog.d(TAG, "WHO-IS EXCEPTION → ${e.message}")

                CoroutineScope(Dispatchers.Main).launch {
                    _searchFailed.value = true
                }
            }

        }
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
        CcuLog.d(TAG, "connectedDevices-->${connectedDevices.value}")

        if (connectedDevices.value.isEmpty() ) {
            ProgressDialogUtils.hideProgressDialog()
            Toast.makeText(context, "No devices found", Toast.LENGTH_SHORT).show()
            _searchFailed.value = true
        }

        fetchDeviceNames()

    }

    private fun fetchDeviceNames(){
        connectedDevices.value.forEach {
            CcuLog.d(TAG, "--deviceId-->${it.deviceId}<--deviceIp-->${it.deviceIp}<--deviceName-->${it.deviceName}<--deviceMacAddress-->${it.deviceMacAddress}<--deviceNetwork-->${it.deviceNetwork}")
            var destinationMacAddress = ""
            if(it.deviceMacAddress != null && it.deviceMacAddress.trim().isNotEmpty()){
                destinationMacAddress = macAddressToByteArray(it.deviceMacAddress)
            }
            service = if(configurationType.value == MSTP_CONFIGURATION) {
                ServiceManager.makeCcuServiceForMSTP()
            } else {
                ServiceManager.makeCcuService()
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
                                updateDeviceNameOnUi(deviceName.toString(), objectInstance)
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

    val onBacnetDeviceSelect = object : OnItemSelectBacnetDevice {
        override fun onItemSelected(index: Int, item: BacnetDevice) {
            CcuLog.d(TAG,"onItemSelected-->$item")
            // Existing behavior
            destinationIp.value = item.deviceIp
            deviceId.value = item.deviceId
            destinationPort.value = item.devicePort
            destinationMacAddress.value = if(configurationType.value == IP_CONFIGURATION)
                item.deviceMacAddress?.let { macAddressToByteArray(it) } ?: ""
            else item.deviceMacAddress ?: ""

            dnet.value = item.deviceNetwork

            // Trigger next dialog via flag
            if (openModelSelectionAfterDeviceClick.value) {
                selectedDeviceForModel.value = item  // optional storage
                openModelSelectionAfterDeviceClick.value = false // reset
            }
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

    private fun macAddressToByteArray(mac: String): String {
        var byteArray = mac.split(":")
            .map { it.toInt(16).toByte() }
            .toByteArray()
        return byteArray.joinToString(".") { (it.toInt() and 0xFF).toString() }

    }

    private fun showToastMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            isErrorMsg.value = true
            errorMsg = message
        }
    }

    fun resetSearchFlag() {
        _searchFinished.value = false
    }
    fun resetSearchFailedFlag() {
        _searchFailed.value = false
    }
}