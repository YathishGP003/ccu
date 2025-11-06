package a75f.io.logic.util.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Tags.BACNET_DEVICE_JOB
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.L.TAG_CCU_BACNET
import a75f.io.logic.L.TAG_CCU_BACNET_MSTP
import a75f.io.logic.bo.building.system.BacNetConstants
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCov
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCovForAllDevices
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCovRequest
import a75f.io.logic.bo.building.system.BacnetReadRequestMultiple
import a75f.io.logic.bo.building.system.BacnetServicesUtils
import a75f.io.logic.bo.building.system.DestinationMultiRead
import a75f.io.logic.bo.building.system.ObjectIdentifierBacNet
import a75f.io.logic.bo.building.system.PropertyReference
import a75f.io.logic.bo.building.system.ReadRequestMultiple
import a75f.io.logic.bo.building.system.RpmRequest
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.MultiReadResponse
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.logic.bo.building.system.doMakeRequest
import a75f.io.logic.bo.util.isPointFollowingScheduleOrEvent
import a75f.io.logic.util.bacnet.BacnetConfigConstants.APDU_SEGMENT_TIMEOUT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.APDU_TIMEOUT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.APPLICATION_SOFTWARE_VERSION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_BBMD_CONFIGURATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_BBMD
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_FD
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_NORMAL
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_HEART_BEAT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_ID
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_JOB_TASK_TYPE
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_MSTP_CONFIGURATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_MSTP_HEART_BEAT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_PERIODIC_RESUBSCRIBE_COV
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_CONFIGURATION_TYPE
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_START
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_STOP
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DAYLIGHT_SAVING_STATUS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESCRIPTION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.FIRMWARE_REVISION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS_VAL
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_OBJECT_NAME
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_MSTP_INITIALIZED
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_STACK_INITIALIZED
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_GLOBAL
import a75f.io.logic.util.bacnet.BacnetConfigConstants.LOCAL_NETWORK_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.LOCATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MODEL_NAME
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MSTP_CONFIGURATION_INTENT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_APDU_RETRIES
import a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_NOTIFICATION_CLASS_OBJECTS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_OFFSET_VALUES
import a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_SCHEDULE_OBJECTS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_TREND_LOG_OBJECTS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PASSWORD
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PORT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PORT_VAL
import a75f.io.logic.util.bacnet.BacnetConfigConstants.SERIAL_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.UTC_OFFSET
import a75f.io.logic.util.bacnet.BacnetConfigConstants.VENDOR_ID
import a75f.io.logic.util.bacnet.BacnetConfigConstants.VENDOR_ID_VALUE
import a75f.io.logic.util.bacnet.BacnetConfigConstants.VENDOR_NAME
import a75f.io.logic.util.bacnet.BacnetConfigConstants.VENDOR_NAME_VALUE
import a75f.io.logic.util.bacnet.BacnetConfigConstants.VIRTUAL_NETWORK_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.ZONE_TO_VIRTUAL_DEVICE_MAPPING
import a75f.io.util.getConfig
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.preference.PreferenceManager
import android.text.format.Formatter
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.projecthaystack.HDict
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.ConnectException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs


fun sendBroadCast(context: Context, intentAction: String, message: String) {
        CcuLog.i("sendBroadCast", ""+intentAction)
        val intent = Intent(intentAction)
        intent.putExtra("message", message)
        context.sendBroadcast(intent)
    }

    fun sendBroadCast(context: Context, intentAction: String, message: String, deviceId: String) {
        CcuLog.i("sendBroadCast", ""+intentAction)
        val intent = Intent(intentAction)
        intent.putExtra("message", message)
        intent.putExtra("deviceId", deviceId)
        context.sendBroadcast(intent)
    }

    fun populateBacnetConfigurationObject() : JSONObject {

        val configObject = JSONObject()

        // "config" property
        configObject.put(ZONE_TO_VIRTUAL_DEVICE_MAPPING, false)

        // "network" object
        val networkObject = JSONObject()
        networkObject.put(IP_ADDRESS, getIpAddress())
        networkObject.put(LOCAL_NETWORK_NUMBER, JSONObject.NULL)
        networkObject.put(VIRTUAL_NETWORK_NUMBER, JSONObject.NULL)
        networkObject.put(PORT, PORT_VAL)

        // "device" object
        val deviceObject = JSONObject()
        deviceObject.put(IP_DEVICE_INSTANCE_NUMBER, getAddressBand())
        deviceObject.put(IP_DEVICE_OBJECT_NAME, getDeviceObjectName())
        deviceObject.put(VENDOR_ID, VENDOR_ID_VALUE)
        deviceObject.put(VENDOR_NAME, VENDOR_NAME_VALUE)
        deviceObject.put(MODEL_NAME, Build.MODEL)
        deviceObject.put(APPLICATION_SOFTWARE_VERSION, getCcuVersion())
        deviceObject.put(FIRMWARE_REVISION, getCmBoardVersion())
        deviceObject.put(LOCATION, JSONObject.NULL)
        deviceObject.put(PASSWORD, JSONObject.NULL)
        deviceObject.put(APDU_TIMEOUT, JSONObject.NULL)
        deviceObject.put(NUMBER_OF_APDU_RETRIES, JSONObject.NULL)
        deviceObject.put(APDU_SEGMENT_TIMEOUT, JSONObject.NULL)
        deviceObject.put(UTC_OFFSET, getUtcOffset())
        deviceObject.put(DAYLIGHT_SAVING_STATUS, getDayLightSavingStatus())
        deviceObject.put(SERIAL_NUMBER, getSerialNumber())
        deviceObject.put(DESCRIPTION, JSONObject.NULL)


        // "objectConf" object
        val objectConf = JSONObject()
        objectConf.put(NUMBER_OF_NOTIFICATION_CLASS_OBJECTS, 0) //These objects are not required as of now
        objectConf.put(NUMBER_OF_TREND_LOG_OBJECTS, 0)
        objectConf.put(NUMBER_OF_SCHEDULE_OBJECTS, 0)
        objectConf.put(NUMBER_OF_OFFSET_VALUES, 1)


        configObject.put("device", deviceObject)
        configObject.put("network", networkObject)
        configObject.put("objectConf", objectConf)

        return configObject
    }

    fun getAddressBand(): Int {
        return L.ccu().addressBand.toInt() + 99
    }

    fun getUtcOffset(): Int {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault())
        val timeZone = TimeZone.getDefault()
        val offsetInMillis = timeZone.getOffset(calendar.timeInMillis)
        val offset: String
        val inHrs = abs(offsetInMillis / 3600000)
        val totalMins = inHrs * 60 + abs(offsetInMillis / 60000 % 60)
        offset = (if (offsetInMillis >= 0) "+" else "-") + totalMins
        return offset.toInt()
    }

    fun getCmBoardVersion(): Any {
       return  "Android "+Build.VERSION.RELEASE+" - "+Build.DISPLAY
    }

    fun getCcuVersion(): Any {
        val pm: PackageManager = Globals.getInstance().applicationContext.packageManager
        val pi: PackageInfo
        return try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0)
            pi.versionName + "." + pi.versionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            JSONObject.NULL
        }
    }

    fun getDeviceObjectName() = CCUHsApi.getInstance().getSiteName() +"_"+ CCUHsApi.getInstance().getCcuName()
    fun getDayLightSavingStatus() = if(TimeZone.getTimeZone(TimeZone.getDefault().id).inDaylightTime(Date())) 1 else 0
    fun getSerialNumber() = CCUHsApi.getInstance().getCcuRef().toString()


    fun getIpAddress(): String {

        val ethernetIP = getEthernetIPAddress()
        if(ethernetIP != null)
            return ethernetIP

        val wifiManager = Globals.getInstance().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return if (ipAddress != 0) {
            Formatter.formatIpAddress(ipAddress)
        } else {
            IP_ADDRESS_VAL
        }
    }


    fun getEthernetIPAddress(): String? {
        try {
            val interfaces: List<NetworkInterface> = NetworkInterface.getNetworkInterfaces().toList()
            for (networkInterface in interfaces) {
                if (networkInterface.isUp && networkInterface.hardwareAddress != null && networkInterface.name.startsWith("eth")) {
                    val addresses = networkInterface.inetAddresses.toList()
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address is InetAddress && address.isSiteLocalAddress && address.address.size == 4) {
                            return address.hostAddress // Return IPv4 address
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
         return null
    }


    fun readExternalBacnetJsonFile(): String {
        val sharedPreferences: SharedPreferences =  PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        return sharedPreferences.getString(BACNET_CONFIGURATION, null).toString()
    }

    fun updateBacnetHeartBeat() {
        val sharedPreferences: SharedPreferences =  PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        sharedPreferences.edit().putLong(BACNET_HEART_BEAT, System.currentTimeMillis()).apply()

        // If Stack initialized then update the bacnet server status to online
        val isBacnetStackInitialized = sharedPreferences.getBoolean(IS_BACNET_STACK_INITIALIZED, false)
        if (isBacnetStackInitialized) {
            updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_ONLINE.ordinal)
        } else {
            updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_OFFLINE.ordinal)
        }
    }

     fun updateBacnetMstpHeartBeat() {
         val sharedPreferences: SharedPreferences =  PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
         sharedPreferences.edit().putLong(BACNET_MSTP_HEART_BEAT, System.currentTimeMillis()).apply()
     }

    fun checkBacnetHealth() {
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        val bacnetLastHeartBeatTime = preferences.getLong(BACNET_HEART_BEAT, 0)
        CcuLog.d(TAG_CCU_BACNET, "Last Bacnet Ip stack HeartBeat Time: $bacnetLastHeartBeatTime,  time: "+(System.currentTimeMillis() - bacnetLastHeartBeatTime))
        val isBACnetIntialized = preferences.getBoolean(IS_BACNET_INITIALIZED, false)
        if(isBACnetIntialized) {
            if ((System.currentTimeMillis() - bacnetLastHeartBeatTime) > 300000) {
                preferences.edit().putLong(BACNET_HEART_BEAT, System.currentTimeMillis()).apply()  // resetting the timer again
                updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_OFFLINE.ordinal)
                launchBacApp(Globals.getInstance().applicationContext, BROADCAST_BACNET_APP_START, "Start BACnet App", "")
            } else if ((System.currentTimeMillis() - bacnetLastHeartBeatTime) > 60000) {
                // If the BACnet is not responding for more than 1 minute, then update the server status to offline
                CcuLog.d(TAG_CCU_BACNET, "BACnet is not responding, Heart beat lost")
                updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_OFFLINE.ordinal)
            } else {
                CcuLog.d(TAG_CCU_BACNET, "BACnet IP stack is healthy, Heart beat received")
                updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_ONLINE.ordinal)
            }
        } else {
            CcuLog.d(L.TAG_CCU_BACNET, "BACnet IP stack is not initialized")
            updateBacnetServerStatus(BacnetServerStatus.NOT_INITIALIZED.ordinal)
        }

        val isBacnetMstpInitialized = preferences.getBoolean(IS_BACNET_MSTP_INITIALIZED, false)
        if (isBacnetMstpInitialized) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP stack is initialized")
            val bacnetMstpLastHeartBeatTime = preferences.getLong(BACNET_MSTP_HEART_BEAT, 0)
            CcuLog.d(TAG_CCU_BACNET_MSTP, "Last Bacnet Mstp stack HeartBeat Time: $bacnetMstpLastHeartBeatTime,  time: "+(System.currentTimeMillis() - bacnetMstpLastHeartBeatTime))

            if ((System.currentTimeMillis() - bacnetMstpLastHeartBeatTime) > 300000) {
                preferences.edit().putLong(BACNET_MSTP_HEART_BEAT, System.currentTimeMillis()).apply()  // resetting the timer again
                launchBacApp(Globals.getInstance().applicationContext, BROADCAST_BACNET_APP_START, "Start BACnet MSTP App", "",true)
            } else if ((System.currentTimeMillis() - bacnetMstpLastHeartBeatTime) > 60000) {
                // If the BACnet is not responding for more than 1 minute, then update the server status to offline
                CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP stack is not responding, Heart beat lost")
            }
        } else {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP stack is not initialized")
        }
    }

    fun reInitialiseBacnetStack() {
        val intent = Intent(BROADCAST_BACNET_APP_STOP)
        intent.putExtra("message", "Watch Id was changed. Reinitialise BACnet Stack")
        CcuLog.d(L.TAG_CCU_BACNET, "reInitialiseBacnetStack: Watch Id was changed. Reinitialise BACnet Stack")
        Globals.getInstance().applicationContext.sendBroadcast(intent)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        val isBacnetMstpInitialized = sharedPreferences.getBoolean(IS_BACNET_MSTP_INITIALIZED, false)
        launchBacApp(Globals.getInstance().applicationContext, BROADCAST_BACNET_APP_START, "Start BACnet App", "",isBacnetMstpInitialized)
    }

   fun launchBacApp(context: Context, intentAction: String, message: String, deviceId: String, isMstpConfigRequired: Boolean = false) {
    CcuLog.d(L.TAG_CCU_BACNET, "launchBacApp started")

    try {
        var packageName = L.BAC_APP_PACKAGE_NAME
        var bacAppLaunchIntent = context.packageManager.getLaunchIntentForPackage(L.BAC_APP_PACKAGE_NAME)

        // Fallback to the obsolete package if the current package is unavailable
        if (bacAppLaunchIntent == null) {
            CcuLog.d(L.TAG_CCU_BACNET, "Unable to acquire BacApp launch intent for " + L.BAC_APP_PACKAGE_NAME + ". Trying " + L.BAC_APP_PACKAGE_NAME_OBSOLETE)
            packageName = L.BAC_APP_PACKAGE_NAME_OBSOLETE
            bacAppLaunchIntent =
                context.packageManager.getLaunchIntentForPackage(L.BAC_APP_PACKAGE_NAME_OBSOLETE)
        }

        if (bacAppLaunchIntent == null) {
            CcuLog.d(L.TAG_CCU_BACNET, "Unable to acquire BacApp launch intent for " + L.BAC_APP_PACKAGE_NAME_OBSOLETE)
            return
        }

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        val isGlobal = sharedPreferences.getBoolean(IS_GLOBAL, true)

        if (isMstpConfigRequired) {
            val mstpConfig = sharedPreferences.getString(BACNET_MSTP_CONFIGURATION, "")
            CcuLog.d(L.TAG_CCU_BACNET, "MSTP Configuration Intent: $mstpConfig")
            bacAppLaunchIntent.putExtra(MSTP_CONFIGURATION_INTENT, mstpConfig)
        }

        // Set the intent action and extras
        bacAppLaunchIntent.setAction(intentAction)
        bacAppLaunchIntent.putExtra("message", message)
        bacAppLaunchIntent.putExtra(IS_GLOBAL, isGlobal)
        if (deviceId.isNotEmpty()) {
            bacAppLaunchIntent.putExtra("deviceId", deviceId)
        }

        context.startActivity(bacAppLaunchIntent)
        CcuLog.d(L.TAG_CCU_BACNET, "BacApp ($packageName) will open now.")

    } catch (e: Exception) {
        CcuLog.d(L.TAG_CCU_BACNET, "launchBacApp exception: $e")
    }
}
    fun generateBacnetIdForRoom(zoneID: String): Int {
        var bacnetID = 1
        var isBacnetIDUsed = true
        try {
            val currentRoom = CCUHsApi.getInstance().readMapById(zoneID)
            if (currentRoom.containsKey(BACNET_ID) && currentRoom[BACNET_ID] != 0) {
                val bacnetID2 = (currentRoom[BACNET_ID].toString() + "").toDouble()
                CcuLog.d(L.TAG_CCU_BACNET, "Already have bacnetID $bacnetID2")
                return bacnetID2.toInt()
            }
            val rooms = CCUHsApi.getInstance().readAllEntities("room")
            if (rooms.size == 0) {
                CcuLog.d(L.TAG_CCU_BACNET, "rooms size : 0 ")
                return bacnetID
            }
            while (isBacnetIDUsed) {
                for (room in rooms) {
                    if (room.containsKey(BACNET_ID)
                        && room[BACNET_ID] != 0
                        && (room[Tags.BACNET_ID].toString() + "").toDouble() == bacnetID.toDouble()
                    ) {
                        CcuLog.d(L.TAG_CCU_BACNET,"In looping over - {bacnetID: ${room[BACNET_ID]} ,tempBacnetID: $bacnetID} - room object: $room")
                        bacnetID += 1
                        isBacnetIDUsed = true
                        break
                    } else {
                        isBacnetIDUsed = false
                    }
                }
            }
            CcuLog.d(L.TAG_CCU_BACNET, "Generated bacnetID: $bacnetID")
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return bacnetID
    }

    fun addBacnetTags(
        context: Context?,
        floorRef: String,
        roomRef: String
    ) {
       updateZone(roomRef, floorRef)
       CCUHsApi.getInstance().syncEntityTree()
    }

    private fun updateZone(roomRef: String, floorRef: String){
        try {
            val bacnetId = generateBacnetIdForRoom(roomRef)
            val zone = HSUtil.getZone(roomRef, floorRef)
            zone.bacnetId = bacnetId
            zone.bacnetType = Tags.DEVICE
            CCUHsApi.getInstance().updateZone(zone, zone.id)
        } catch (e: NullPointerException) {
            CcuLog.d(L.TAG_CCU_BACNET, "Unable to update zone:  " + e.message)
            e.printStackTrace()
        }
    }

    fun getUpdatedExistingBacnetConfigDeviceData(configString: String): Pair<String, Boolean> {
        var updatedConfigString = configString
        var isConfigChanged = false

        try {
            val bacnetConfig = JSONObject(configString)
            val deviceData = bacnetConfig.getJSONObject(Tags.DEVICE)

            val applicationSoftwareVersion = getCcuVersion().toString()
            val utcOffset = getUtcOffset()
            val tabletModel = Build.MODEL
            val firmwareRevision = getCmBoardVersion().toString()

            if (deviceData.getString(APPLICATION_SOFTWARE_VERSION) != applicationSoftwareVersion) {
                deviceData.put(APPLICATION_SOFTWARE_VERSION, getCcuVersion())
                isConfigChanged = true
            }
            if (deviceData.getInt(UTC_OFFSET) != utcOffset) {
                deviceData.put(UTC_OFFSET, utcOffset)
                deviceData.put(DAYLIGHT_SAVING_STATUS, getDayLightSavingStatus())
                isConfigChanged = true
            }
            if (deviceData.getString(MODEL_NAME) != tabletModel) {
                deviceData.put(MODEL_NAME, tabletModel)
                isConfigChanged = true
            }
            if (deviceData.getString(FIRMWARE_REVISION) != firmwareRevision) {
                deviceData.put(FIRMWARE_REVISION, firmwareRevision)
                isConfigChanged = true
            }
            updatedConfigString = bacnetConfig.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return Pair(updatedConfigString, isConfigChanged)
    }

fun updateBacnetServerStatus(status: Int) {
    CcuLog.d(L.TAG_CCU_BACNET, "Updating Bacnet Server Status to: $status")
    Domain.diagEquip.bacnetServerStatus.writeHisValueByIdWithoutCOV(status.toDouble())
}

fun updateBacnetStackInitStatus(isStackInitSuccess: Boolean) {
    val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
    if (isStackInitSuccess) {
        CcuLog.d(L.TAG_CCU_BACNET, "BACnet Stack initialized successfully")
        updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_ONLINE.ordinal)
        sharedPreferences.edit().putBoolean(IS_BACNET_STACK_INITIALIZED, true).apply()

    } else {
        CcuLog.d(L.TAG_CCU_BACNET, "BACnet Stack initialization failed")
        updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_OFFLINE.ordinal)
        sharedPreferences.edit().putBoolean(IS_BACNET_STACK_INITIALIZED, false).apply()
    }
}

fun updateBacnetMstpLinearAndCovSubscription( isInitProcessRequired: Boolean = true ) {
    CcuLog.d(TAG_CCU_BACNET_MSTP, "--[1]-- mstp rpm inside updateBacnetMstpLinearAndCovSubscription ")
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
    val isBacnetMstpInitialized = sharedPreferences.getBoolean(IS_BACNET_MSTP_INITIALIZED, false)

    if (!isBacnetMstpInitialized) {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "--[2]-- BACnet MSTP stack is not initialized, skipping Linear and COV Subscription")
        return
    }

    if (isInitProcessRequired) {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP COV Subscription and Linear Read Request Initialization started")
    } else {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP Polling started for Linear RPM")
    }

    val serviceUtils = BacnetServicesUtils()
    val serverIpAddress = serviceUtils.getServerIpAddress()
    CcuLog.d(TAG_CCU_BACNET_MSTP, "--[3]-- BACnet MSTP Subscription Server IP Address: $serverIpAddress")
    val bacnetMstpEquip = CCUHsApi.getInstance().readAllHDictByQuery("equip and bacnetMstp")
    handleLinearPoints(bacnetMstpEquip)
    CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP Subscription for ${bacnetMstpEquip.size} devices")
    var subscribeCovForAllDevices = mutableListOf<BacnetMstpSubscribeCov>()
    bacnetMstpEquip.forEach { t1 ->

        val deviceId = t1["bacnetDeviceId"]?.toString() ?: ""
        val deviceMacAddress = t1["bacnetDeviceMacAddr"]?.toString() ?: "0"
        val destination = DestinationMultiRead("", "0", deviceId ,"", deviceMacAddress)

        CcuLog.d(TAG_CCU_BACNET_MSTP,"Destination for Subscription: $destination")
        val points = CCUHsApi.getInstance().readAllHDictByQuery("point and bacnetId and equipRef==\"${t1.id()}\"")
        CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP Subscription for device: $deviceId with mac address: $deviceMacAddress and points: ${points.size}")
        val objectIdentifierListForCov = mutableListOf<ObjectIdentifierBacNet>()
        val readAccessSpecification = mutableListOf<ReadRequestMultiple>()

        points.forEach {
            try {
                CcuLog.d(TAG_CCU_BACNET_MSTP, "Point: ${it.id()}")
                val objectId = it[Tags.BACNET_OBJECT_ID]?.toString()?.toDouble()?.toInt()
                val objectType =
                        it[Tags.BACNET_TYPE]?.toString()?.let { BacnetTypeMapper.getObjectType(it) }

                if (objectId == null || objectType == null) {

                    CcuLog.e(TAG_CCU_BACNET_MSTP, "Object ID or Object Type is null for point: ${it.id()}")

                } else {
                    CcuLog.d(TAG_CCU_BACNET_MSTP, "Point: ${it.id()} with objectId: $objectId and objectType: $objectType")

                    if (isInitProcessRequired) {
                        // Add to linear list to update the point present value
                        val objectIdentifier = getDetailsFromObjectLayout(objectType, objectId.toString())
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

                            CcuLog.d(TAG_CCU_BACNET_MSTP, "Point ${it.id()} is COV enabled")
                            val objectIdentifier =
                                    getDetailsFromObjectLayout(objectType, objectId.toString())
                            CcuLog.d(TAG_CCU_BACNET_MSTP, "Object Identifier for Cov: $objectIdentifier")
                            objectIdentifierListForCov.add(objectIdentifier)
                            CcuLog.d(TAG_CCU_BACNET_MSTP, "Object Identifier List for Cov: $objectIdentifierListForCov")
                        }
                    } else {

                        /*if ( it["hisInterpolate"]?.toString() == Tags.LINEAR ) {
                            CcuLog.d(TAG_CCU_BACNET_MSTP, "Point ${it.id()} is Linear enabled")
                            val objectIdentifier = getDetailsFromObjectLayout(objectType, objectId.toString())
                            CcuLog.d(TAG_CCU_BACNET_MSTP, "Object Identifier for Linear: $objectIdentifier")
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
                        }*/
                    }
                }
            } catch (e: Exception) {
                CcuLog.e(TAG_CCU_BACNET_MSTP, "Error processing point: ${it.id()} - ${e.message}")
            }
        }

        if (objectIdentifierListForCov.isNotEmpty()) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "BACnet MSTP COV Subscription for device: $deviceId with mac address: $deviceMacAddress and object identifiers: $objectIdentifierListForCov")
            subscribeCovForAllDevices.add(BacnetMstpSubscribeCov(
                    destination,
                    BacnetMstpSubscribeCovRequest(1,objectIdentifierListForCov)
            ))

        } else {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "No COV enabled points found for device: $deviceId")
        }

        /*if (serverIpAddress != null && readAccessSpecification.isNotEmpty()) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "Sending RPM for device: $deviceId with mac address: $deviceMacAddress and read access specification: $readAccessSpecification")
            val rpmRequest = RpmRequest(readAccessSpecification)
            sendRequestMultipleRead(BacnetReadRequestMultiple(destination, rpmRequest), serverIpAddress , deviceId, t1.id().toString())
        }*/

    }
        if (subscribeCovForAllDevices.isEmpty()) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "No COV subscription found for any device")
        } else {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "Sending COV subscription for all devices: $subscribeCovForAllDevices")
            serviceUtils.sendCovSubscription(BacnetMstpSubscribeCovForAllDevices(subscribeCovForAllDevices), "")
        }

}

private fun sendRequestMultipleRead(
    rpmRequest: BacnetReadRequestMultiple,
    deviceId: String,
    equipId: String
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val service =  ServiceManager.makeCcuServiceForMSTP()
            val response = service.multiread(rpmRequest)
            val resp = BaseResponse(response)
            if (response.isSuccessful) {
                val result = resp.data
                if (result != null) {
                    val readResponse = result.body()
                    CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM received response->${readResponse}")
                    updatePointPresentValue(readResponse, deviceId, equipId)
                } else {
                    CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM --null response--")
                }
            } else {
                CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM --error--${resp.error}")
            }
        } catch (e: SocketTimeoutException) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM --SocketTimeoutException--${e.message}")
        } catch (e: ConnectException) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM --ConnectException--${e.message}")
        } catch (e: java.lang.Exception) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM --connection time out--${e.message}")
        }
    }
}

var equipsToProcess = 0
var equipProcessed = 0
fun handleLinearPoints(bacnetMstpEquip: MutableList<HDict>) {
    CcuLog.d(TAG_CCU_BACNET_MSTP, "--[4]-- mstp rpm handleLinearPoints called, check whether it needs to process or not")

    // If all equips already processed, reset and start again
    if (equipProcessed >= equipsToProcess && equipsToProcess > 0) {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "--[5]-- mstp rpm All $equipsToProcess equips processed. Resetting counters for next run...")
        equipsToProcess = 0
        equipProcessed = 0
    }

    if (equipProcessed < equipsToProcess || equipsToProcess == 0) {
        equipsToProcess = bacnetMstpEquip.size
        equipProcessed = 0
        var equip : HDict
        for (i in 0 until equipsToProcess) {
            equip = bacnetMstpEquip[i]
            CcuLog.d(TAG_CCU_BACNET_MSTP, "--[6]-- mstp rpm Processing equip no-->$i <--id-->${equip.id()} <--dis-->${equip.dis()}")
            handlePointsWithDelay(equip)
            equipProcessed = i + 1
        }
    } else {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "--[7]-- mstp rpm Already processed all $equipsToProcess equipments. Skipping...")
    }
}

fun handlePointsWithDelay(
    t1: HDict
) {
    CcuLog.d(TAG_CCU_BACNET_MSTP, "--[8]-- mstp rpm --inside handlePointsWithDelay---checking if equip has linear points")
    val points = CCUHsApi.getInstance()
            .readAllHDictByQuery("hisInterpolate == \"linear\" and point and bacnetId and equipRef==\"${t1.id()}\"")

    if (points.isEmpty()) {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "--[9]-- mstp rpm No linear points found for ${t1.id()}")
        return
    }else{
        CcuLog.d(TAG_CCU_BACNET_MSTP, "--[9.1]-- mstp rpm points found for processing ${points.size}")
    }

    val deviceId = t1["bacnetDeviceId"]?.toString() ?: ""
    val deviceMacAddress = t1["bacnetDeviceMacAddr"]?.toString() ?: "0"
    val destination = DestinationMultiRead("", "0", deviceId, "", deviceMacAddress)

    points.chunked(10).forEachIndexed { batchIndex, chunk ->
        val readAccessSpecification = mutableListOf<ReadRequestMultiple>()

        chunk.forEach { point ->
            try {
                val objectId = point[Tags.BACNET_OBJECT_ID]?.toString()?.toDouble()?.toInt()
                val objectType = point[Tags.BACNET_TYPE]?.toString()?.let { BacnetTypeMapper.getObjectType(it) }

                if (objectId != null && objectType != null) {
                    val objectIdentifier = getDetailsFromObjectLayout(objectType, objectId.toString())
                    val propertyReference = listOf(
                        PropertyReference(BacNetConstants.PropertyType.PROP_PRESENT_VALUE.value, -1)
                    )
                    readAccessSpecification.add(ReadRequestMultiple(objectIdentifier, propertyReference as MutableList<PropertyReference>))
                } else {
                    CcuLog.e(TAG_CCU_BACNET_MSTP, "--[10]-- mstp rpm Invalid point: ${point.id()}")
                }
            } catch (e: Exception) {
                CcuLog.e(TAG_CCU_BACNET_MSTP, "--[11]-- mstp rpm Error processing point: ${point.id()} - ${e.message}")
            }
        }

        if (readAccessSpecification.isNotEmpty()) {
            val rpmRequest = RpmRequest(readAccessSpecification)
            CcuLog.d(TAG_CCU_BACNET_MSTP, "--[12]-- mstp rpm Sending RPM batch ${batchIndex + 1} for device $deviceId")
            sendRequestMultipleRead(
                BacnetReadRequestMultiple(destination, rpmRequest),
                deviceId,
                t1.id().toString()
            )
        }else{
            CcuLog.d(TAG_CCU_BACNET_MSTP, "--[12]-- mstp rpm either serverIpAddress is null or readAccessSpecification is empty")
        }

        CcuLog.d(TAG_CCU_BACNET_MSTP, "--[13]-- mstp rpm waiting for 10 seconds")
        // wait 5 seconds before sending next batch
        if (batchIndex < (points.size / 10)) {
            sleep(5000)
        }
    }
}


fun updateHeartBeatPoint(id: String, isEquipId: Boolean = false) {
    var pointId = id
    if(!pointId.startsWith("@")){
        pointId = "@$pointId"
    }
    val point = CCUHsApi.getInstance().readMapById(pointId)
    val isBacnetClientPoint = point["bacnetCur"]
    val isBacnetMstpPoint = point["bacnetMstp"]
    if ((isBacnetClientPoint != null && isBacnetClientPoint.toString().isNotEmpty()) || ( isBacnetMstpPoint != null && isBacnetMstpPoint.toString().isNotEmpty()) ){
        val equipId = if (!isEquipId) point["equipRef"] //if given id is point id then get equipRef from point
                      else id    //if given id is equip id then use it directly
        CcuLog.i(TAG_CCU_BACNET, "updateHeartBeatPoint for equip: $equipId  isIp -> $isBacnetClientPoint isMstp-> $isBacnetMstpPoint" )
        val heartBeatPointId = CCUHsApi.getInstance().readEntity("point and heartbeat and (bacnet or bacnetCur or bacnetMstp) and equipRef==\"$equipId\"")["id"]
        if(heartBeatPointId != null && heartBeatPointId.toString().isNotEmpty()){
            CcuLog.i(TAG_CCU_BACNET, "updateHeartBeatPoint for equip: $equipId with heartBeatPointId: $heartBeatPointId")
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(heartBeatPointId.toString(), 1.0)
        }
    }
}

private fun updatePointPresentValue(readResponse: MultiReadResponse?,deviceId: String, equipId: String) {
    if (readResponse != null) {
        if (readResponse.error != null) {
            val errorCode = BacNetConstants.BacnetErrorCodes.from(readResponse.error!!.errorCode.toInt())
            val errorClass = BacNetConstants.BacnetErrorClasses.from(readResponse.error!!.errorClass.toInt())
           CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM error->${readResponse.error!!.errorCode} and error class->${readResponse.error!!.errorClass} and error code->${errorCode} and error class->${errorClass}")
        } else if(readResponse.errorAbort != null){
            CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM abort reason->${BacNetConstants.BacnetAbortErrors.from(
                readResponse.errorAbort!!.abortReason.toInt())}")
        }else if(readResponse.errorBacApp != null){
            CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.errorBacApp!!.abortReason.toInt())}")
        }else if(readResponse.errorReject != null){
            CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM abort reason->${BacNetConstants.BacnetRejectErrors.from(
                readResponse.errorReject!!.abortReason.toInt())}")
        }else if(readResponse.errorASide != null){
            CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM abort reason->${readResponse.errorASide!!.abortReason}")
        }else {
            if(readResponse.rpResponse != null){
                for (item in readResponse.rpResponse.listOfItems) {

                    item.results.forEach {
                          it.propertyValue?.value?.let { value ->
                              var updatedObjectType = getBacNetType( ObjectType.values()[item.objectIdentifier.objectType.toInt()].key.replace("OBJECT_", ""))
                              CcuLog.i(TAG_CCU_BACNET_MSTP, "MSTP -> updatedObjectType: $updatedObjectType")
                              CcuLog.i(TAG_CCU_BACNET_MSTP, "MSTP -> objectId: ${item.objectIdentifier.objectInstance}")
                              val query = "point and bacnetDeviceId == $deviceId and bacnetObjectId==${item.objectIdentifier.objectInstance} and bacnetType==\"$updatedObjectType\""
                              CcuLog.i(TAG_CCU_BACNET_MSTP, "MSTP -> query: $query")
                              val point = CCUHsApi.getInstance().read(query)
                              if (point.isEmpty()) {
                                  CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM response for point ${item.objectIdentifier.objectInstance} is not found")
                                  return@forEach
                              }
                              val id = point?.get("id")?.toString()
                              CcuLog.d(TAG_CCU_BACNET_MSTP, "RPM response for point $id is $value")
                              // Update the point present value in the database
                              CCUHsApi.getInstance().writePointValue(point,value.toDouble())
                          }
                    }
                }
                updateHeartBeatPoint(equipId,true)
            }else{
                CcuLog.d(TAG_CCU_BACNET_MSTP," RPM response is null")
            }
        }
    }
}

fun scheduleJobToCheckBacnetDeviceOnline(){
    val workRequest = PeriodicWorkRequestBuilder<BacnetDeviceJob>(15, TimeUnit.MINUTES)
        .setInputData(workDataOf(BACNET_JOB_TASK_TYPE to BACNET_DEVICE_JOB))
        .build()

    WorkManager.getInstance(Globals.getInstance().applicationContext)
        .enqueueUniquePeriodicWork(
            BACNET_DEVICE_JOB,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    CcuLog.d(BACNET_DEVICE_JOB, "--created work request for looking bacnet device for system profile--")
}

fun cancelScheduleJobToCheckBacnet(reason : String){
    CcuLog.d(BACNET_DEVICE_JOB, "--cancelScheduleJobToCheckBacnet--$reason")
    WorkManager.getInstance(Globals.getInstance().applicationContext).cancelUniqueWork(BACNET_DEVICE_JOB)
}

fun scheduleJobToResubscribeBacnetMstpCOV() {
    val workRequest = PeriodicWorkRequestBuilder<BacnetDeviceJob>(1, TimeUnit.HOURS)
        .setInputData(workDataOf(BACNET_JOB_TASK_TYPE to BACNET_PERIODIC_RESUBSCRIBE_COV))
        .build()


    WorkManager.getInstance(Globals.getInstance().applicationContext)
        .enqueueUniquePeriodicWork(
            BACNET_PERIODIC_RESUBSCRIBE_COV,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    CcuLog.d(TAG_CCU_BACNET_MSTP, "--created work request for resubscribing bacnet mstp cov--")
}

fun cancelScheduleJobToResubscribeBacnetMstpCOV(reason: String) {
    CcuLog.d(TAG_CCU_BACNET_MSTP, "--cancelScheduleJobToResubscribeBacnetMstpCOV--$reason")
    WorkManager.getInstance(Globals.getInstance().applicationContext).cancelUniqueWork(BACNET_PERIODIC_RESUBSCRIBE_COV)
}

fun updateBacnetIpModeConfigurations(isStackInitSuccess: Boolean) {
    val context = Globals.getInstance().applicationContext
    val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    if (isStackInitSuccess) {
        val deviceType = sharedPreferences.getString(BACNET_DEVICE_TYPE, BACNET_DEVICE_TYPE_NORMAL)
        CcuLog.d(L.TAG_CCU_BACNET, "BACnet Stack initialized successfully and device configured as $deviceType. Sending broadcast.....")
        when (deviceType) {
            BACNET_DEVICE_TYPE_BBMD -> {
                val jsonString = sharedPreferences.getString(BACNET_BBMD_CONFIGURATION, null).toString()
                val intent = Intent(BROADCAST_BACNET_APP_CONFIGURATION_TYPE)
                intent.putExtra("message", "BBMD")
                intent.putExtra("data", jsonString)
                context.sendBroadcast(intent)
            }
            BACNET_DEVICE_TYPE_FD -> {
                val jsonString = sharedPreferences.getString(BACNET_BBMD_CONFIGURATION, null).toString()
                val intent = Intent(BROADCAST_BACNET_APP_CONFIGURATION_TYPE)
                intent.putExtra("message", "Foreign Device")
                intent.putExtra("data", jsonString)
                context.sendBroadcast(intent)
            }
            else -> {
                sendBroadCast(context, BROADCAST_BACNET_APP_CONFIGURATION_TYPE, "Normal")
            }
        }
    }
}

fun initializeMSTPStack(isStackInitSuccess: Boolean) {
    val context = Globals.getInstance().applicationContext
    val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)

    val isMstpInitialized = sharedPreferences.getBoolean(IS_BACNET_MSTP_INITIALIZED, false)

    if (isStackInitSuccess && isMstpInitialized) {
        val mstpConfig = sharedPreferences.getString(BACNET_MSTP_CONFIGURATION, "")
        CcuLog.d(L.TAG_CCU_BACNET, "MSTP output-->$mstpConfig")
        val intent = Intent("MSTP_CONFIGURATION")
        intent.putExtra("message", "MSTP")
        intent.putExtra("data", mstpConfig)
        context.sendBroadcast(intent)
        CcuLog.d(L.TAG_CCU_BACNET, "MSTP Stack initialized successfully")
    } else {
        CcuLog.d(L.TAG_CCU_BACNET, "MSTP Stack not initialized ")
    }
}

fun isAppRunning(packageName: String): Boolean {
    return try {
        val process = Runtime.getRuntime().exec("su") // root shell
        val os = DataOutputStream(process.outputStream)
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        os.writeBytes("ps -A | grep $packageName\n")
        os.writeBytes("exit\n")
        os.flush()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.contains(packageName)) {
                CcuLog.d("AppCheck", "App is running: $line")
                return true
            }
        }

        process.waitFor()
        reader.close()
        os.close()
        false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun sendWriteRequestToMstpEquip(id : String, level: String, value: String, isMstpEquip : Boolean = true) {
    var pointId = id
    if(!pointId.startsWith("@")){
        pointId = "@$pointId"
    }
    val pointMap = CCUHsApi.getInstance().readMapById(pointId)
    if (pointMap.isEmpty()) {
        CcuLog.e(L.TAG_CCU_BACNET, "Point with id $id not found")
        return
    }

    var bacnetId = pointMap[Tags.BACNET_OBJECT_ID]?.toString()?.toDouble()?.toInt()
    val group = pointMap[Tags.GROUP]?.toString()?.toIntOrNull()
    val objectType = pointMap[Tags.BACNET_TYPE]?.toString()?.let { BacnetTypeMapper.getObjectType(it) }
    if (bacnetId == null || group == null || objectType == null) {
        CcuLog.e(L.TAG_CCU_BACNET, "Bacnet ID or Group or Bacnet Type not found for point with id $id")
        return
    }

    val equipId = pointMap[Tags.EQUIPREF]?.toString()
    val equip = CCUHsApi.getInstance().readMapById(equipId ?: "")
    val bacnetConfig = equip["bacnetConfig"].toString()

    val bacnetService = BacnetServicesUtils()
    val serverIpAddress: String? = bacnetService.getServerIpAddress()
    if(serverIpAddress != null){
        CcuLog.d(L.TAG_CCU_BACNET, "--writeRequest-->BacnetId-->$bacnetId<--newValue-->$value-->objectType-->$objectType<-pointId->$pointId<---serverIpAddress-->$serverIpAddress")
        val remotePointUpdateInterface =
            RemotePointUpdateInterface { message: String?, id: String?, value: String ->
                CcuLog.d(L.TAG_CCU_BACNET, "--updateMessage:: write request >> $message")
            }

        bacnetService.sendWriteRequest(
            bacnetService.generateWriteObject(getConfig(bacnetConfig), bacnetId, value,
            "OBJECT_$objectType", level, true),
            serverIpAddress, remotePointUpdateInterface, value, pointId, isMstpEquip)
    }

}

fun sendWriteRequestToBacnetEquip(
    id: String,
    level: String,
    value: String
) {
    var pointId = id
    if (!pointId.startsWith("@")) {
        pointId = "@$pointId"
    }
    val pointMap = CCUHsApi.getInstance().readMapById(pointId)
    if (pointMap.isEmpty()) {
        CcuLog.e(L.TAG_CCU_BACNET, "Point with id $id not found")
        return
    }
    var defaultPriority = pointMap["defaultWriteLevel"].toString()
    if (isPointFollowingScheduleOrEvent(pointId)) {
        defaultPriority = level
    }
    var bacnetObjectId = pointMap[Tags.BACNET_OBJECT_ID]?.toString()?.toDouble()?.toInt()
    val group = pointMap[Tags.GROUP]?.toString()?.toIntOrNull()
    val objectType =
        pointMap[Tags.BACNET_TYPE]?.toString()?.let { BacnetTypeMapper.getObjectType(it) }
    val objectTypeWithSuffix = "OBJECT_$objectType"
    CcuLog.d(TAG_CCU_BACNET, "sendWriteRequestToBacnetEquip--->$objectType<-->$objectTypeWithSuffix<-bacnetObjectId->$bacnetObjectId<-pointId->$pointId<-bacnetObjectId->$bacnetObjectId")
    if (bacnetObjectId == null || group == null || objectType == null) {
        CcuLog.e(
            L.TAG_CCU_BACNET,
            "Bacnet ID or Group or Bacnet Type not found for point with id $id"
        )
        return
    }

    val equipId = pointMap[Tags.EQUIPREF]?.toString()
    val equip = CCUHsApi.getInstance().readMapById(equipId ?: "")
    val bacnetConfig = equip["bacnetConfig"].toString()
    if (BacNetConstants.ObjectType.OBJECT_MULTI_STATE_VALUE.key == objectTypeWithSuffix
        || BacNetConstants.ObjectType.OBJECT_MULTI_STATE_OUTPUT.key == objectTypeWithSuffix
        || BacNetConstants.ObjectType.OBJECT_MULTI_STATE_INPUT.key == objectTypeWithSuffix
        || BacNetConstants.ObjectType.OBJECT_BINARY_VALUE.key == objectTypeWithSuffix
        || BacNetConstants.ObjectType.OBJECT_BINARY_OUTPUT.key == objectTypeWithSuffix
        || BacNetConstants.ObjectType.OBJECT_BINARY_INPUT.key == objectTypeWithSuffix
        ) {
        val wholeNumber = value.toInt()
        doMakeRequest(
            BacnetServicesUtils().getConfig(bacnetConfig),
            bacnetObjectId,
            wholeNumber.toString(),
            objectTypeWithSuffix,
            defaultPriority,
            pointId, false
        )
    } else {
        doMakeRequest(
            BacnetServicesUtils().getConfig(bacnetConfig),
            bacnetObjectId,
            value.toString(),
            objectTypeWithSuffix,
            defaultPriority,
            pointId, false
        )
    }
}

fun encodeBacnetId(slaveId: Int, objectType: Int, objectId: Int): Int {
    return "$slaveId$objectType$objectId".toInt()
}

fun decodeBacnetId(bacnetId: Int,objectType: Int): Int{
    var bacnetIdStr = bacnetId.toString().substring(3)

    bacnetIdStr = bacnetIdStr.replaceFirst(objectType.toString(),"")
    val objectId = bacnetIdStr.toInt()

    return objectId
}

fun isMstpDevice(bacnetSystemEquip: HashMap<Any, Any>) : Boolean{
    return bacnetSystemEquip.containsKey("bacnetMstp")
}
fun isValidMstpMacAddress(value: String): Boolean {

    return try {
        val macAddress = value.toInt()
        macAddress in 0..254
    } catch (e: NumberFormatException) {
        false // If it cannot be parsed as an integer, it's invalid
    }
}

fun isThisMasterDevice(value: String): Boolean{
    return try {
        val macAddress = value.toInt()
        macAddress in 1..127
    } catch (e: NumberFormatException) {
        false // If it cannot be parsed as an integer, it's invalid
    }
}

fun validateInputdata(lowLimt:Int,highLimit:Int, value:Int): Boolean {
    return when {
        value < lowLimt -> {
            CcuLog.d(L.TAG_CCU_BACNET, "Value $value is below the low limit $lowLimt")
            false
        }
        value > highLimit -> {
            CcuLog.d(L.TAG_CCU_BACNET, "Value $value is above the high limit $highLimit")
            false
        }
        else -> {
            CcuLog.d(L.TAG_CCU_BACNET, "Value $value is within the limits ($lowLimt, $highLimit)")
            true
        }
    }
}

fun getDetailsFromObjectLayout(objectType : String, objectId: String): ObjectIdentifierBacNet {
    return ObjectIdentifierBacNet(
        BacNetConstants.ObjectType.valueOf("OBJECT_$objectType").value,
        objectId
    )
}

fun getBacNetType(objectType: String): String {
    return when (objectType.uppercase(Locale.getDefault())) {
        "ANALOG_INPUT" -> "AnalogInput"
        "ANALOG_OUTPUT" -> "AnalogOutput"
        "ANALOG_VALUE" -> "AnalogValue"
        "BINARY_INPUT" -> "BinaryInput"
        "BINARY_OUTPUT" -> "BinaryOutput"
        "BINARY_VALUE" -> "BinaryValue"
        "CALENDAR" -> "Calendar"
        "COMMAND" -> "Command"
        "DEVICE" -> "Device"
        "EVENT_ENROLLMENT" -> "EventEnrollment"
        "FILE" -> "File"
        "GROUP" -> "Group"
        "LOOP" -> "Loop"
        "MULTI_STATE_INPUT" -> "MultiStateInput"
        "MULTI_STATE_OUTPUT" -> "MultiStateOutput"
        "NOTIFICATION_CLASS" -> "NotificationClass"
        "PROGRAM" -> "Program"
        "SCHEDULE" -> "Schedule"
        "AVERAGING" -> "Averaging"
        "MULTI_STATE_VALUE" -> "MultiStateValue"
        "TRENDLOG" -> "TrendLog"
        "LIFE_SAFETY_POINT" -> "LifeSafetyPoint"
        "LIFE_SAFETY_ZONE" -> "LifeSafetyZone"
        "ACCUMULATOR" -> "Accumulator"
        "PULSE_CONVERTER" -> "PulseConverter"
        "EVENT_LOG" -> "EventLog"
        "GLOBAL_GROUP" -> "GlobalGroup"
        "TREND_LOG_MULTIPLE" -> "TrendLogMultiple"
        "LOAD_CONTROL" -> "LoadControl"
        "STRUCTURED_VIEW" -> "StructuredView"
        "ACCESS_DOOR" -> "AccessDoor"
        "TIMER" -> "Timer"
        "ACCESS_CREDENTIAL" -> "AccessCredential"
        "ACCESS_POINT" -> "AccessPoint"
        "ACCESS_RIGHTS" -> "AccessRights"
        "ACCESS_USER" -> "AccessUser"
        "ACCESS_ZONE" -> "AccessZone"
        "CREDENTIAL_DATA_INPUT" -> "CredentialDataInput"
        "NETWORK_SECURITY" -> "NetworkSecurity"
        "BITSTRING_VALUE" -> "BitStringValue"
        "CHARACTERSTRING_VALUE" -> "CharacterStringValue"
        "DATE_PATTERN_VALUE" -> "DatePatternValue"
        "DATE_VALUE" -> "DateValue"
        "DATETIME_PATTERN_VALUE" -> "DateTimePatternValue"
        "DATETIME_VALUE" -> "DateTimeValue"
        "INTEGER_VALUE" -> "IntegerValue"
        "LARGE_ANALOG_VALUE" -> "LargeAnalogValue"
        "OCTETSTRING_VALUE" -> "OctetStringValue"
        "POSITIVE_INTEGER_VALUE" -> "PositiveIntegerValue"
        "TIME_PATTERN_VALUE" -> "TimePatternValue"
        "TIME_VALUE" -> "TimeValue"
        "NOTIFICATION_FORWARDER" -> "NotificationForwarder"
        "ALERT_ENROLLMENT" -> "AlertEnrollment"
        "CHANNEL" -> "Channel"
        "LIGHTING_OUTPUT" -> "LightingOutput"
        "BINARY_LIGHTING_OUTPUT" -> "BinaryLightingOutput"
        "NETWORK_PORT" -> "NetworkPort"
        "ELEVATOR_GROUP" -> "ElevatorGroup"
        "ESCALATOR" -> "Escalator"
        "LIFT" -> "Lift"
        else -> "UnknownType"
    }
}