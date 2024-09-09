package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.logger.CcuLog
import a75f.io.renatus.ENGG.bacnet.services.client.BaseResponse
import a75f.io.renatus.ENGG.bacnet.services.client.ServiceManager
import a75f.io.renatus.bacnet.RemotePointUpdateInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException

class BacnetServicesUtils {
    private val TAG = BacnetServicesUtils::class.java.simpleName
    fun sendWriteRequest(
        bacnetWriteRequest: BacnetWriteRequest,
        ipAddress: String,
        remotePointUpdateInterface: RemotePointUpdateInterface,
        selectedValue: String,
        id: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = ServiceManager.makeCcuService(ipAddress)
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
}