package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESTINATION_IP
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESTINATION_PORT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DEVICE_ID
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DEVICE_NETWORK
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MAC_ADDRESS
import a75f.io.logic.util.bacnet.ObjectType
import a75f.io.logic.util.bacnet.sendWriteRequestToMstpEquip
import a75f.io.util.BacnetRequestUtil
import android.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException

class BacnetServicesUtils: BacnetRequestUtil {
    private val TAG = BacnetServicesUtils::class.java.simpleName
    fun sendWriteRequest(
        bacnetWriteRequest: BacnetWriteRequest,
        ipAddress: String,
        remotePointUpdateInterface: RemotePointUpdateInterface,
        selectedValue: String,
        id: String,
        isMstpEquip: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = if (isMstpEquip) ServiceManager.makeCcuServiceForMSTP(ipAddress)
                              else ServiceManager.makeCcuService(ipAddress)
                val response = service.write(bacnetWriteRequest)
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received write response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            if (readResponse != null) {
                                if (readResponse.error != null) {
                                    val errorCode = BacNetConstants.BacnetErrorCodes.from(readResponse.error.errorCode.toInt())
                                    val errorClass = BacNetConstants.BacnetErrorClasses.from(readResponse.error.errorClass.toInt())
                                    remotePointUpdateInterface.updateMessage("error code->${errorCode}--error class->${errorClass}", id, selectedValue)
                                } else if(readResponse.errorAbort != null){
                                    remotePointUpdateInterface.updateMessage("abort reason->${BacNetConstants.BacnetAbortErrors.from(readResponse.errorAbort.abortReason.toInt())}", id, selectedValue)
                                }else if(readResponse.errorBacApp != null){
                                    remotePointUpdateInterface.updateMessage("abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.errorBacApp.abortReason.toInt())}", id, selectedValue)
                                }else if(readResponse.errorReject != null){
                                    remotePointUpdateInterface.updateMessage("abort reason->${BacNetConstants.BacnetRejectErrors.from(readResponse.errorReject.abortReason.toInt())}", id, selectedValue)
                                }else if(readResponse.errorASide != null){
                                    remotePointUpdateInterface.updateMessage("abort reason->${readResponse.errorASide.abortReason}", id, selectedValue)
                                }else if(readResponse.bacappError != null){
                                    remotePointUpdateInterface.updateMessage("abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.bacappError.errorCode.toInt())}", id, selectedValue)
                                }else {
                                    remotePointUpdateInterface.updateMessage("Successfully updated value--> $selectedValue for point id -$id", id, selectedValue)
                                }
                            }
                        }
                    } else {
                        remotePointUpdateInterface.updateMessage("null response", id, selectedValue)
                        CcuLog.d(TAG, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG, "--error--${resp.error}")
                    remotePointUpdateInterface.updateMessage("-error--${resp.error}", id, selectedValue)
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.e(TAG, "--SocketTimeoutException--${e.message}")
                remotePointUpdateInterface.updateMessage(("SocketTimeoutException"), id, selectedValue)
            } catch (e: ConnectException) {
                CcuLog.e(TAG, "--ConnectException--${e.message}")
                remotePointUpdateInterface.updateMessage("ConnectException", id, selectedValue)
            } catch (e: Exception) {
                CcuLog.e(TAG, "--connection time out--${e.message}")
                remotePointUpdateInterface.updateMessage("connection time out", id, selectedValue)
            }
        }
    }

    fun sendCovSubscription( subscribeCovRequest: BacnetMstpSubscribeCov , ipAddress: String) {

        val service = ServiceManager.makeCcuServiceForMSTP(ipAddress)
      CcuLog.d(TAG, "sendCovSubscription called with request: $subscribeCovRequest")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.subscribeCov(subscribeCovRequest)
                val resp = BaseResponse(response)
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

    fun getServerIpAddress() : String?{
        var ipAddress : String? = null
        val bacnetServerConfig = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
            .getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
        if (bacnetServerConfig != null) {
            try {
                val config = JSONObject(bacnetServerConfig)
                val networkObject = config.getJSONObject("network")
                ipAddress =  networkObject.getString(BacnetConfigConstants.IP_ADDRESS)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return ipAddress
    }
     fun generateWriteObject(
        configMap: Map<String, String>,
        objectId: Int,
        selectedValue: String,
        objectType: String,
        priority: String,
        isMstpEquip: Boolean
    ): BacnetWriteRequest {

        val macAddress = configMap[MAC_ADDRESS] ?: ""

        val destinationMultiRead = DestinationMultiRead(
            requireNotNull(configMap.getOrDefault(DESTINATION_IP,"")),
            requireNotNull(configMap.getOrDefault(DESTINATION_PORT, "0")),
            requireNotNull(configMap.getOrDefault(DEVICE_ID, "0")),
            requireNotNull(configMap.getOrDefault(DEVICE_NETWORK,"0")),
            macAddress
        )

        val objectTypeEnum = BacNetConstants.ObjectType.valueOf(objectType)

         val dataType: Int = when (objectTypeEnum.value) {
             ObjectType.OBJECT_ANALOG_VALUE.value,
             ObjectType.OBJECT_ANALOG_INPUT.value,
             ObjectType.OBJECT_ANALOG_OUTPUT.value -> {
                 BacNetConstants.DataTypes.BACNET_DT_REAL.ordinal + 1
             }
             ObjectType.OBJECT_BINARY_VALUE.value,
             ObjectType.OBJECT_BINARY_INPUT.value,
             ObjectType.OBJECT_BINARY_OUTPUT.value -> {
                 BacNetConstants.DataTypes.BACNET_DT_ENUM.ordinal + 1
             }
             else -> {
                 if (isMstpEquip) {
                     BacNetConstants.DataTypes.BACNET_DT_UNSIGNED32.ordinal + 1
                 } else {
                     BacNetConstants.DataTypes.BACNET_DT_UNSIGNED.ordinal + 1
                 }
             }
         }

         val selectedValueAsPerType: String = if (
            objectTypeEnum.value == ObjectType.OBJECT_ANALOG_VALUE.value ||
            objectTypeEnum.value == ObjectType.OBJECT_ANALOG_INPUT.value ||
            objectTypeEnum.value == ObjectType.OBJECT_ANALOG_OUTPUT.value ||
            objectTypeEnum.value == ObjectType.OBJECT_BINARY_VALUE.value ||
            objectTypeEnum.value == ObjectType.OBJECT_BINARY_INPUT.value ||
            objectTypeEnum.value == ObjectType.OBJECT_BINARY_OUTPUT.value
        ) {
            selectedValue
        } else {
            (selectedValue.toDouble().toInt()).toString()
        }

        val objectIdentifierBacNet = ObjectIdentifierBacNet(objectTypeEnum.value, objectId.toString())
        val propertyValueBacNet = PropertyValueBacNet(dataType, selectedValueAsPerType)
        val writeRequest = WriteRequest(
            objectIdentifierBacNet,
            propertyValueBacNet,
            priority,
            BacNetConstants.PropertyType.valueOf("PROP_PRESENT_VALUE").value,
            null
        )

        return BacnetWriteRequest(destinationMultiRead, writeRequest)
    }
    fun getConfig(configString: String): MutableMap<String, String> {
        val pairs: Array<String> =
            configString.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val configMap: MutableMap<String, String> = java.util.HashMap()
        for (pair in pairs) {
            val keyValue = pair.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (keyValue.size != 2) continue  // Skip invalid key value pairs (e.g. "destinationIp:
            val key = keyValue[0]
            val value = keyValue[1]
            configMap[key] = value
        }
        return configMap
    }

//    override fun sendMultiWriteRequest(
//        data: BacnetWriteRequest,
//        ipAddress: String,
//        selectedValue: String,
//        id: String
//    ) {
//        sendWriteRequest(data, ipAddress, remotePointUpdateInterface, selectedValue, id)
//    }

    private val remotePointUpdateInterface =
        RemotePointUpdateInterface { message: String?, id: String?, value: String ->
            CcuLog.d("BACNET", "--updateMessage::>> " + message);
//            getActivity().runOnUiThread(Runnable {
//                Toast.makeText(
//                    requireContext(),
//                    message,
//                    Toast.LENGTH_SHORT
//                ).show()
//            })
            CCUHsApi.getInstance().writeDefaultValById(id, value.toDouble())
            CCUHsApi.getInstance().writeHisValById(id, value.toDouble())
        }

    override fun callbackForBacnetMstpRequest(id: String, level: String, value: String) {
        CcuLog.d("CCU_BACNET", "--callbackForBacnetMstpRequest::>> id: $id, level: $level, value: $value")
        sendWriteRequestToMstpEquip(id,level,value)
    }
}