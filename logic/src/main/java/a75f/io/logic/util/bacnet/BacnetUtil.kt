package a75f.io.logic.util.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Tags.BACNET_DEVICE_JOB
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.util.bacnet.BacnetConfigConstants.APDU_SEGMENT_TIMEOUT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.APDU_TIMEOUT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.APPLICATION_SOFTWARE_VERSION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_HEART_BEAT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_ID
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_START
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DAYLIGHT_SAVING_STATUS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESCRIPTION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.FIRMWARE_REVISION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS_VAL
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_OBJECT_NAME
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_STACK_INITIALIZED
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_GLOBAL
import a75f.io.logic.util.bacnet.BacnetConfigConstants.LOCAL_NETWORK_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.LOCATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MODEL_NAME
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
import org.json.JSONException
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Calendar
import java.util.Date
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

    fun checkBacnetHealth() {
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        val bacnetLastHeartBeatTime = preferences.getLong(BACNET_HEART_BEAT, 0)
        CcuLog.d(L.TAG_CCU_BACNET, "Last Bacnet HeartBeat Time: $bacnetLastHeartBeatTime,  time: "+(System.currentTimeMillis() - bacnetLastHeartBeatTime))
        val isBACnetIntialized = preferences.getBoolean(IS_BACNET_INITIALIZED, false)
        if(isBACnetIntialized) {
            if ((System.currentTimeMillis() - bacnetLastHeartBeatTime) > 300000) {
                preferences.edit().putLong(BACNET_HEART_BEAT, System.currentTimeMillis()).apply()  // resetting the timer again
                launchBacApp(Globals.getInstance().applicationContext, BROADCAST_BACNET_APP_START, "Start BACnet App", "")
            } else if ((System.currentTimeMillis() - bacnetLastHeartBeatTime) > 60000) {
                // If the BACnet is not responding for more than 1 minute, then update the server status to offline
                CcuLog.d(L.TAG_CCU_BACNET, "BACnet is not responding, Heart beat lost")
                updateBacnetServerStatus(BacnetServerStatus.INITIALIZED_OFFLINE.ordinal)
            }
        } else {
            CcuLog.d(L.TAG_CCU_BACNET, "BACnet is not initialized")
            updateBacnetServerStatus(BacnetServerStatus.NOT_INITIALIZED.ordinal)
        }
    }

   fun launchBacApp(context: Context, intentAction: String, message: String, deviceId: String) {
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

fun scheduleJobToCheckBacnetDeviceOnline(){
    val workRequest = PeriodicWorkRequestBuilder<BacnetDeviceJob>(15, TimeUnit.MINUTES)
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