package a75f.io.logic.util.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Tags.BACNET_DEVICE_JOB
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.system.BacnetWhoIsRequest
import a75f.io.logic.bo.building.system.BroadCast
import a75f.io.logic.bo.building.system.WhoIsRequest
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.CcuService
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_JOB_TASK_TYPE
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_PERIODIC_RESUBSCRIBE_COV
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_DEVICE_ID
import android.content.Context
import android.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException

class BacnetDeviceJob(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        CcuLog.d(BACNET_DEVICE_JOB, "BacnetDeviceJob Work is running...")
        when (val taskType = inputData.getString(BACNET_JOB_TASK_TYPE)) {
            BACNET_DEVICE_JOB -> {
                CcuLog.d(BACNET_DEVICE_JOB, "BacnetDeviceJob task type is BACNET_DEVICE_JOB")
                handleDoWork(Globals.getInstance().applicationContext)
            }
            BACNET_PERIODIC_RESUBSCRIBE_COV -> {
                CcuLog.d(BACNET_DEVICE_JOB, "BacnetDeviceJob task type is BACNET_PERIODIC_RESUBSCRIBE_COV")
                // Handle periodic resubscribe COV logic here if needed
                updateBacnetMstpLinearAndCovSubscription(true)
            }
            else -> CcuLog.d(BACNET_DEVICE_JOB, "BacnetDeviceJob unknown task type: $taskType")
        }
        return Result.success()
    }

    companion object {
        private var bacnetEquip: HashMap<Any, Any> = HashMap()
        fun handleDoWork(context: Context) {
            bacnetEquip = findPoint("system and equip and bacnetCur and not emr and not btu")
            if (bacnetEquip != null) {
                CcuLog.d(
                    BACNET_DEVICE_JOB,
                    "--globals bacnet equip found attached with external ahu---"
                )
                fetchConnectedDeviceGlobally(bacnetEquip, context)
            }
        }

        private fun findPoint(query: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(query)
        }

        private fun fetchConnectedDeviceGlobally(bacnetEquip: HashMap<Any, Any>, context: Context) {
            if (bacnetEquip.isEmpty()) {
                CcuLog.d(BACNET_DEVICE_JOB, "----bacnetEquip is empty returning from here----")
                return
            }
            val bacnetDeviceId = bacnetEquip["bacnetDeviceId"].toString()
            val isMstp = bacnetEquip.contains(Tags.BACNET_MSTP)
            val lowLimit = bacnetDeviceId.toInt()
            val highLimit = bacnetDeviceId.toInt()

            val networkDetailsCurrentDevice = getNetworkDetails(context)
            if (networkDetailsCurrentDevice != null) {
                val serverIpAddress =
                    networkDetailsCurrentDevice.getString(BacnetConfigConstants.IP_ADDRESS)
                val devicePort =
                    networkDetailsCurrentDevice.getInt(BacnetConfigConstants.PORT).toString()
                val service = if (isMstp) ServiceManager.makeCcuServiceForMSTP() else ServiceManager.makeCcuService()
                CcuLog.d(
                    Tags.BACNET_DEVICE_JOB,
                    "--fetchConnectedDeviceGlobally for ip --> $serverIpAddress --- service--$service---devicePort-->$devicePort <--lowLimit-->$lowLimit<--highLimit-->$highLimit"
                )
                try {

                    val srcDeviceID = PreferenceManager.getDefaultSharedPreferences(context).getInt(
                        PREF_MSTP_DEVICE_ID,0).toString()
                    val broadCastValue = "global"
                    val bacnetWhoIsRequest = BacnetWhoIsRequest(
                        WhoIsRequest("$lowLimit", "$highLimit"),
                        BroadCast(broadCastValue),
                        devicePort,
                        serverIpAddress,
                        srcDeviceId = srcDeviceID
                    )
                    val request = Gson().toJson(bacnetWhoIsRequest)
                    CcuLog.d(BACNET_DEVICE_JOB, "this is the broadcast request-->$request")
                    sendRequest(bacnetWhoIsRequest, service)
                } catch (e: NumberFormatException) {
                    CcuLog.d(BACNET_DEVICE_JOB, "please provide valid input - ${e.message}")
                }
            } else {
                CcuLog.d(BACNET_DEVICE_JOB, "---no networkDetailsCurrentDevice--")
            }
        }

        private fun sendRequest(bacnetWhoIsRequest: BacnetWhoIsRequest, service: CcuService) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = service.whois(bacnetWhoIsRequest)
                    val resp = BaseResponse(response)
                    if (response.isSuccessful) {
                        val result = resp.data
                        if (result != null) {
                            val readResponse = result.body()
                            CcuLog.d(BACNET_DEVICE_JOB, "received response->${readResponse}")
                            CoroutineScope(Dispatchers.Main).launch {
                                if (!readResponse?.whoIsResponseList.isNullOrEmpty()) {
                                    CcuLog.d(BACNET_DEVICE_JOB, "---device found---")
                                    updateHeartBeatPoint(1.0)
                                } else {
                                    CcuLog.d(BACNET_DEVICE_JOB, "no devices found")
                                    updateHeartBeatPoint(0.0)
                                }
                            }
                        } else {
                            CcuLog.d(BACNET_DEVICE_JOB, "--null response--")
                            updateHeartBeatPoint(0.0)
                        }
                    } else {
                        CcuLog.d(BACNET_DEVICE_JOB, "--error--${resp.error}")
                        updateHeartBeatPoint(0.0)
                    }
                } catch (e: SocketTimeoutException) {
                    CcuLog.d(BACNET_DEVICE_JOB, "--SocketTimeoutException--${e.message}")
                    updateHeartBeatPoint(0.0)
                } catch (e: ConnectException) {
                    CcuLog.d(BACNET_DEVICE_JOB, "--ConnectException--${e.message}")
                    updateHeartBeatPoint(0.0)
                } catch (e: Exception) {
                    CcuLog.d(BACNET_DEVICE_JOB, "--connection time out--${e.message}")
                    updateHeartBeatPoint(0.0)
                }
            }
        }

        private fun updateHeartBeatPoint(newValue: Double) {
            val bacnetEquipId = bacnetEquip?.get("id").toString()
            val heartBeatPointId = CCUHsApi.getInstance().readEntity("point and heartbeat and equipRef== \"${bacnetEquipId}\"")["id"]
            CcuLog.d(
                BACNET_DEVICE_JOB,
                "--updateHeartBeatPoint--$newValue<--heartBeatPointId-->$heartBeatPointId <--bacnetEquipId-->$bacnetEquipId"
            )
            if (heartBeatPointId.toString().isNotEmpty()) {
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(heartBeatPointId.toString(), newValue)
                CcuLog.d(BACNET_DEVICE_JOB, "--updateHeartBeatPoint--updated successfully -->Value-->$newValue")
            }
        }

        private fun getNetworkDetails(context: Context): JSONObject? {
            val bacnetServerConfig =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
            if (bacnetServerConfig != null) {
                try {
                    val config = JSONObject(bacnetServerConfig)
                    return config.getJSONObject("network")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            return null
        }
    }
}