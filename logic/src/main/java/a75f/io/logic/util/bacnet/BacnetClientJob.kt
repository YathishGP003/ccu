package a75f.io.logic.util.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.BaseJob
import a75f.io.logic.Globals
import a75f.io.logic.L.TAG_CCU_BACNET_MSTP
import a75f.io.logic.bo.building.system.BacNetConstants
import a75f.io.logic.bo.building.system.BacnetIpSubscribeCov
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCov
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCovForAllDevices
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCovRequest
import a75f.io.logic.bo.building.system.BacnetWhoIsRequest
import a75f.io.logic.bo.building.system.BroadCast
import a75f.io.logic.bo.building.system.DestinationMultiRead
import a75f.io.logic.bo.building.system.ObjectIdentifierBacNet
import a75f.io.logic.bo.building.system.PropertyReference
import a75f.io.logic.bo.building.system.ReadRequestMultiple
import a75f.io.logic.bo.building.system.WhoIsRequest
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.CcuService
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESTINATION_IP
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESTINATION_PORT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DEVICE_NETWORK
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MAC_ADDRESS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_DEVICE_ID
import a75f.io.logic.util.bacnet.BacnetUtility.getNetworkDetails
import a75f.io.logic.util.bacnet.BacnetUtility.isBacnetInitialized
import a75f.io.logic.util.bacnet.BacnetUtility.parseBacnetConfigData
import a75f.io.logic.util.bacnet.BacnetUtility.updateBacnetHeartBeatPoint
import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.HashMap
import java.util.concurrent.TimeUnit

object BacnetClientJob : BaseJob() {

    private val TAG = "CCU_BACNET_CLIENT"
    private var isBacnetClientJobScheduled = false
    private var count = 0;

    @SuppressLint("SuspiciousIndentation")
    override fun doJob() {
        logIt("BacnetClientJob is started... ${count++}")

        if (isBacnetInitialized()) {

            val listOfClient = CCUHsApi.getInstance().readAllHDictByQuery("point and bacnet and heartbeat")
            if (listOfClient.isNotEmpty()) {
                logIt("Bacnet Clients point found, total clients: ${listOfClient.size}")
                for (point in listOfClient) {
                    // print the point Id
                    logIt("Processing point ID: ${point.id()}")
                    val heartbeatHisData = CCUHsApi.getInstance().hisRead(point.id().toString(),"current")

                    if (heartbeatHisData.isEmpty()) {
                        logIt("No heartbeat history data for point ID: ${point.id()}, skipping...")
                        continue
                    }
                    val lastHeartbeat = heartbeatHisData[0].dateInMillis

                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastHeartbeat

                    if (timeDiff > 300000) {
                        logIt("Heartbeat timeout for point ID: ${point.id()}, time since last heartbeat: ${timeDiff}ms.")

                        val equipId = point.get("equipRef")?.toString() ?: ""
                        if (equipId.isNotEmpty()) {
                            val equip = CCUHsApi.getInstance().readMapById(equipId)
                            val isMstpEquip = equip.containsKey("bacnetMstp")
                            fetchConnectedDeviceGlobally(equip, Globals.getInstance().applicationContext, isMstpEquip)
                        } else {
                            logIt("No equipRef found for point ID: ${point.id()}")
                        }
                    } else {
                        logIt("Heartbeat received for point ID: ${point.id()}, time since last heartbeat: ${timeDiff}ms.")
                    }
                }
            } else {
                logIt("No Bacnet Clients point found.")
            }
        } else {
            logIt("Bacnet is not initialized, skipping BacnetClientJob.")
        }
        logIt("BacnetClientJob is finished...")
    }

    override fun scheduleJob(name : String, interval : Int, taskSeperation:Int, unit : TimeUnit) {
        super.scheduleJob(name,interval,taskSeperation,unit)
        logIt("BacnetClientJob scheduled...")
        isBacnetClientJobScheduled = true
    }

    fun isJobScheduled() : Boolean {
        return isBacnetClientJobScheduled
    }

     private fun fetchConnectedDeviceGlobally(
         equip: HashMap<Any, Any>,
         context: Context,
         isMstpEquip: Boolean
     ) {
         val bacnetDeviceId = equip[Tags.BACNET_DEVICE_ID]?.toString() ?: ""
        if (bacnetDeviceId.isEmpty() || bacnetDeviceId.toDouble().toInt() == 0) {
            logIt("----bacnetDeviceId is empty returning from here----")
            return
        }
        val lowLimit = bacnetDeviceId.toDouble().toInt()
        val highLimit = bacnetDeviceId.toDouble().toInt()

        val networkDetailsCurrentDevice = getNetworkDetails(context)
        if (networkDetailsCurrentDevice != null) {
            val serverIpAddress =
                networkDetailsCurrentDevice.getString(BacnetConfigConstants.IP_ADDRESS)
            val devicePort =
                networkDetailsCurrentDevice.getInt(BacnetConfigConstants.PORT).toString()
            val service = if (isMstpEquip) ServiceManager.makeCcuServiceForMSTP() else ServiceManager.makeCcuService()
            CcuLog.d(
                TAG,
                "--fetchConnectedDeviceGlobally --- service--$service---devicePort-->$devicePort <--lowLimit-->$lowLimit<--highLimit-->$highLimit"
            )
            try {
                val srcDeviceID = PreferenceManager.getDefaultSharedPreferences(context).getInt(
                    PREF_MSTP_DEVICE_ID, 0
                ).toString()
                val broadCastValue = "global"
                val bacnetWhoIsRequest = if (!isMstpEquip) {
                    BacnetWhoIsRequest(
                        WhoIsRequest("$lowLimit", "$highLimit"),
                        BroadCast(broadCastValue),
                        devicePort,
                        serverIpAddress,
                    )
                } else {
                    BacnetWhoIsRequest(
                        WhoIsRequest("$lowLimit", "$highLimit"),
                        BroadCast(broadCastValue),
                        devicePort,
                        serverIpAddress,
                        srcDeviceId = srcDeviceID
                    )
            }
                val request = Gson().toJson(bacnetWhoIsRequest)
                logIt("this is the broadcast request-->$request")
                sendRequest(bacnetWhoIsRequest, service, equip)
            } catch (e: NumberFormatException) {
                logIt("please provide valid input - ${e.message}")
            }
        } else {
            logIt("---no networkDetailsCurrentDevice--")
        }
    }

    private fun sendRequest(
        bacnetWhoIsRequest: BacnetWhoIsRequest,
        service: CcuService,
        equip: HashMap<Any, Any>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.whois(bacnetWhoIsRequest)
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        logIt(" WhoIs received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            if (!readResponse?.whoIsResponseList.isNullOrEmpty()) {
                                logIt("---device found--- request===> ${equip[Tags.BACNET_DEVICE_ID]} Response--> ${readResponse?.whoIsResponseList?.get(0)?.deviceIdentifier} ")
                                readResponse?.whoIsResponseList.let {
                                    it?.forEach { device ->
                                        sendCovSubscriptionRequest(device.deviceIdentifier, service, equip)
                                        updateBacnetHeartBeatPoint(1.0, equip["id"].toString())
                                    }
                                }
                            } else {
                                logIt("no devices found")
                            }
                        }
                    } else {
                        logIt(" WhoIs --null response--")
                    }
                } else {
                    logIt(" WhoIs --error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                logIt(" WhoIs --SocketTimeoutException--${e.message}")
            } catch (e: ConnectException) {
                logIt(" WhoIs --ConnectException--${e.message}")
            } catch (e: Exception) {
                logIt(" WhoIs --connection time out--${e.message}")
            }
        }
    }

    private fun sendCovSubscriptionRequest(
        deviceId: String,
        service: CcuService,
        equip: HashMap<Any, Any>
    ) {
        val bacnetConfig = parseBacnetConfigData(equip)
         val isMstpEquip = equip.containsKey("bacnetMstp")
        if (isMstpEquip) {
            logIt("COV subscription for MSTP device: $deviceId")
            sendCovSubscriptionFprMstpEquip(equip)
            return
        }
        val destination = DestinationMultiRead(bacnetConfig.getOrDefault(DESTINATION_IP,""),
            bacnetConfig.getOrDefault(DESTINATION_PORT,""),
            deviceId,
            bacnetConfig.getOrDefault(DEVICE_NETWORK,""),
            bacnetConfig.getOrDefault(MAC_ADDRESS,""))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.subscribeCovForIp(BacnetIpSubscribeCov(destination))
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        logIt("received response->${readResponse}")
                    } else {
                        logIt(" CovSubscription --null response--")
                    }
                } else {
                    logIt(" CovSubscription --error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                logIt(" CovSubscription --SocketTimeoutException--${e.message}")
            } catch (e: ConnectException) {
                logIt(" CovSubscription --ConnectException--${e.message}")
            } catch (e: java.lang.Exception) {
                logIt(" CovSubscription --connection time out--${e.message}")
            }
        }



    }

    private fun sendCovSubscriptionFprMstpEquip(equip: HashMap<Any, Any>) {
        var subscribeCovForAllDevices = mutableListOf<BacnetMstpSubscribeCov>()
        val deviceId = equip["bacnetDeviceId"]?.toString() ?: ""
        val deviceMacAddress = equip["bacnetDeviceMacAddr"]?.toString() ?: "0"
        val destination = DestinationMultiRead("", "0", deviceId, "", deviceMacAddress)

        CcuLog.d(TAG_CCU_BACNET_MSTP, "Destination for Subscription: $destination")
        val points = CCUHsApi.getInstance()
            .readAllHDictByQuery("point and bacnetId and equipRef==\"${equip["id"]}\"")
        CcuLog.d(
            TAG_CCU_BACNET_MSTP,
            "BACnet MSTP Subscription for device: $deviceId with mac address: $deviceMacAddress and points: ${points.size}"
        )
        val objectIdentifierListForCov = mutableListOf<ObjectIdentifierBacNet>()
        val readAccessSpecification = mutableListOf<ReadRequestMultiple>()

        points.forEach {
            try {
                val objectId = it[Tags.BACNET_OBJECT_ID]?.toString()?.toDouble()?.toInt()
                val objectType =
                    it[Tags.BACNET_TYPE]?.toString()?.let { BacnetTypeMapper.getObjectType(it) }

                if (objectId == null || objectType == null) {
                    CcuLog.e(TAG_CCU_BACNET_MSTP, "Object ID or Object Type is null for point: ${it.id()}")
                } else {

                    val objectIdentifier =
                        getDetailsFromObjectLayout(objectType, objectId.toString())
                    val propertyReference = mutableListOf<PropertyReference>()
                    propertyReference.add(
                        PropertyReference(
                            BacNetConstants.PropertyType.PROP_PRESENT_VALUE.value,
                            -1
                        )
                    )
                    readAccessSpecification.add(
                        ReadRequestMultiple(
                            objectIdentifier,
                            propertyReference
                        )
                    )

                    // if point hisInterpolate is cov then subscribe to COV
                    if (it["hisInterpolate"]?.toString() == Tags.COV) {

//                        CcuLog.d(TAG_CCU_BACNET_MSTP, "Point ${it.id()} is COV enabled")
                        val objectIdentifier =
                            getDetailsFromObjectLayout(objectType, objectId.toString())
                        CcuLog.d(
                            TAG_CCU_BACNET_MSTP,
                            "Object Identifier for Cov: $objectIdentifier"
                        )
                        objectIdentifierListForCov.add(objectIdentifier)
                        CcuLog.d(
                            TAG_CCU_BACNET_MSTP,
                            "Object Identifier List for Cov: $objectIdentifierListForCov"
                        )
                    }
                }

            } catch (e: Exception) {
                CcuLog.e(TAG_CCU_BACNET_MSTP, "Error processing point: ${it.id()} - ${e.message}")
            }
        }

        if (objectIdentifierListForCov.isNotEmpty()) {
            CcuLog.d(
                TAG_CCU_BACNET_MSTP,
                "BACnet MSTP COV Subscription for device: $deviceId with mac address: $deviceMacAddress and object identifiers: $objectIdentifierListForCov"
            )
            subscribeCovForAllDevices.add(
                BacnetMstpSubscribeCov(
                    destination,
                    BacnetMstpSubscribeCovRequest(1, objectIdentifierListForCov)
                )
            )

        } else {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "No COV enabled points found for device: $deviceId")
        }
        if (subscribeCovForAllDevices.isEmpty()) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "No COV subscription found for any device")
        } else {
            CcuLog.d(
                TAG_CCU_BACNET_MSTP,
                "Sending COV subscription for all devices: $subscribeCovForAllDevices"
            )
            val serviceUtils = ServiceManager.makeCcuServiceForMSTP()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = serviceUtils.subscribeCovForMstp(BacnetMstpSubscribeCovForAllDevices(subscribeCovForAllDevices))
                    val resp = BaseResponse(response)
                    if (response.isSuccessful) {
                        val result = resp.data
                        if (result != null) {
                            val readResponse = result.body()
                            CcuLog.d(TAG, "Cov Subscription received response->${readResponse}")
                        } else {
                            CcuLog.d(TAG, "--Cov Subscription received null response--")
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
    }
    
    private fun logIt(message: String) {
        CcuLog.d( TAG, message)
    }
}