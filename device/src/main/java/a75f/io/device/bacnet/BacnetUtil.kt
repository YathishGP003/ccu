package a75f.io.device.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.device.bacnet.BacnetConfigConstants.APDU_SEGMENT_TIMEOUT
import a75f.io.device.bacnet.BacnetConfigConstants.APDU_TIMEOUT
import a75f.io.device.bacnet.BacnetConfigConstants.APPLICATION_SOFTWARE_VERSION
import a75f.io.device.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION
import a75f.io.device.bacnet.BacnetConfigConstants.BACNET_HEART_BEAT
import a75f.io.device.bacnet.BacnetConfigConstants.BACNET_ID
import a75f.io.device.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_START
import a75f.io.device.bacnet.BacnetConfigConstants.BROADCAST_BACNET_ZONE_ADDED
import a75f.io.device.bacnet.BacnetConfigConstants.DAYLIGHT_SAVING_STATUS
import a75f.io.device.bacnet.BacnetConfigConstants.DESCRIPTION
import a75f.io.device.bacnet.BacnetConfigConstants.FIRMWARE_REVISION
import a75f.io.device.bacnet.BacnetConfigConstants.IP_ADDRESS
import a75f.io.device.bacnet.BacnetConfigConstants.IP_ADDRESS_VAL
import a75f.io.device.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER
import a75f.io.device.bacnet.BacnetConfigConstants.IP_DEVICE_OBJECT_NAME
import a75f.io.device.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED
import a75f.io.device.bacnet.BacnetConfigConstants.LOCAL_NETWORK_NUMBER
import a75f.io.device.bacnet.BacnetConfigConstants.LOCATION
import a75f.io.device.bacnet.BacnetConfigConstants.MODEL_NAME
import a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_APDU_RETRIES
import a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_NOTIFICATION_CLASS_OBJECTS
import a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_OFFSET_VALUES
import a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_SCHEDULE_OBJECTS
import a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_TREND_LOG_OBJECTS
import a75f.io.device.bacnet.BacnetConfigConstants.PASSWORD
import a75f.io.device.bacnet.BacnetConfigConstants.PORT
import a75f.io.device.bacnet.BacnetConfigConstants.PORT_VAL
import a75f.io.device.bacnet.BacnetConfigConstants.SERIAL_NUMBER
import a75f.io.device.bacnet.BacnetConfigConstants.UTC_OFFSET
import a75f.io.device.bacnet.BacnetConfigConstants.VENDOR_ID
import a75f.io.device.bacnet.BacnetConfigConstants.VENDOR_ID_VALUE
import a75f.io.device.bacnet.BacnetConfigConstants.VENDOR_NAME
import a75f.io.device.bacnet.BacnetConfigConstants.VENDOR_NAME_VALUE
import a75f.io.device.bacnet.BacnetConfigConstants.VIRTUAL_NETWORK_NUMBER
import a75f.io.device.bacnet.BacnetConfigConstants.ZONE_TO_VIRTUAL_DEVICE_MAPPING
import a75f.io.logic.Globals
import a75f.io.logic.L
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.preference.PreferenceManager
import android.text.format.Formatter
import android.util.Log
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*


fun sendBroadCast(context: Context, intentAction: String, message: String) {
        Log.i("sendBroadCast", ""+intentAction)
        val intent = Intent(intentAction)
        intent.putExtra("message", message)
        context.sendBroadcast(intent)
    }

    fun sendBroadCast(context: Context, intentAction: String, message: String, deviceId: String) {
        Log.i("sendBroadCast", ""+intentAction)
        val intent = Intent(intentAction)
        intent.putExtra("message", message)
        intent.putExtra("deviceId", deviceId)
        context.sendBroadcast(intent)
    }

    fun populateBacnetConfigurationObject() : JSONObject {

        val configObject = JSONObject();

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
        objectConf.put(NUMBER_OF_NOTIFICATION_CLASS_OBJECTS, 1)
        objectConf.put(NUMBER_OF_TREND_LOG_OBJECTS, 1)
        objectConf.put(NUMBER_OF_SCHEDULE_OBJECTS, 1)
        objectConf.put(NUMBER_OF_OFFSET_VALUES, 0)


        configObject.put("device", deviceObject)
        configObject.put("network", networkObject)
        configObject.put("objectConf", objectConf)

        return configObject;
    }

    fun getAddressBand(): Int {
        return L.ccu().smartNodeAddressBand.toInt() + 99
    }

    fun getUtcOffset(): Int {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault())
        val timeZone = TimeZone.getDefault()
        val offsetInMillis = timeZone.getOffset(calendar.timeInMillis)
        var offset = String.format(
            "%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs(
                offsetInMillis / 60000 % 60
            )
        )
        val inHrs = Math.abs(offsetInMillis / 3600000)
        val totalMins = inHrs * 60 + Math.abs(offsetInMillis / 60000 % 60)
        offset = (if (offsetInMillis >= 0) "+" else "-") + totalMins
        //Log.i("Bacnet", " offset:$offset inHrs:$inHrs total Offset Mins:$totalMins")
        return offset.toInt()
    }

    fun getCmBoardVersion(): Any {
       return  "Android "+Build.VERSION.RELEASE+" - "+Build.DISPLAY
    }

    fun getCcuVersion(): Any {
        val pm: PackageManager = Globals.getInstance().applicationContext.packageManager
        val pi: PackageInfo
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0)
            return pi.versionName + "." + pi.versionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return JSONObject.NULL
        }
    }

    fun getDeviceObjectName() = CCUHsApi.getInstance().getSiteName() +"_"+ CCUHsApi.getInstance().getCcuName();
    fun getDayLightSavingStatus() = if(TimeZone.getTimeZone(TimeZone.getDefault().id).inDaylightTime(Date())) 1 else 0
    fun getSerialNumber() = CCUHsApi.getInstance().getCcuRef().toString()


    fun getIpAddress(): String {

        val ethernetIP = getEthernetIPAddress();
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
        sharedPreferences.edit().putLong(BACNET_HEART_BEAT, System.currentTimeMillis()).apply();
    }

    fun checkBacnetHealth() {
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        val bacnetLastHeartBeatTime = preferences.getLong(BACNET_HEART_BEAT, 0)
        Log.d(L.TAG_CCU_BACNET, "Last Bacnet HeartBeat Time: $bacnetLastHeartBeatTime,  time: "+(System.currentTimeMillis() - bacnetLastHeartBeatTime))
        val isBACnetIntialized = preferences.getBoolean(IS_BACNET_INITIALIZED, false)
        if((isBACnetIntialized && (System.currentTimeMillis() - bacnetLastHeartBeatTime) > 300000)) {
            preferences.edit().putLong(BACNET_HEART_BEAT, System.currentTimeMillis()).apply()  // resetting the timer again
            sendBroadCast(Globals.getInstance().applicationContext, BROADCAST_BACNET_APP_START, "Start BACnet App")
        }
    }

    fun generateBacnetIdForRoom(zoneID: String): Int {
        var bacnetID = 1
        var isBacnetIDUsed = true
        try {
            val currentRoom = CCUHsApi.getInstance().readMapById(zoneID)
            if (currentRoom.containsKey(BACNET_ID) && currentRoom[BACNET_ID] != 0) {
                val bacnetID2 = (currentRoom[BACNET_ID].toString() + "").toDouble()
                Log.d(L.TAG_CCU_BACNET, "Already have bacnetID $bacnetID2")
                return bacnetID2.toInt()
            }
            val rooms = CCUHsApi.getInstance().readAllEntities("room")
            if (rooms.size == 0) {
                Log.d(L.TAG_CCU_BACNET, "rooms size : 0 ")
                return bacnetID
            }
            while (isBacnetIDUsed) {
                for (room in rooms) {
                    if (room.containsKey(BACNET_ID)
                        && room[BACNET_ID] != 0
                        && (room[Tags.BACNET_ID].toString() + "").toDouble() == bacnetID.toDouble()
                    ) {
                        Log.d(L.TAG_CCU_BACNET,"In looping over - {bacnetID: ${room[BACNET_ID]} ,tempBacnetID: $bacnetID} - room object: $room")
                        bacnetID += 1
                        isBacnetIDUsed = true
                        break
                    } else {
                        isBacnetIDUsed = false
                    }
                }
            }
            Log.d(L.TAG_CCU_BACNET, "Generated bacnetID: $bacnetID")
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return bacnetID
    }

    fun addBacnetTags(
        context: Context,
        floorRef: String,
        roomRef: String
    ) {
       updateZone(context, roomRef, floorRef)
       CCUHsApi.getInstance().syncEntityTree()
    }

    private fun updateZone(context: Context, roomRef: String, floorRef: String){
        try {
            val bacnetId = generateBacnetIdForRoom(roomRef)
            val zone = HSUtil.getZone(roomRef, floorRef)
            if(zone.bacnetId == 0){
                sendBroadCast(
                    context,
                    BROADCAST_BACNET_ZONE_ADDED,
                    zone.id
                )
            }
            zone.bacnetId = bacnetId;
            zone.bacnetType = Tags.DEVICE;
            CCUHsApi.getInstance().updateZone(zone, zone.id);
        } catch (e: NullPointerException) {
            Log.d(L.TAG_CCU_BACNET, "Unable to update zone:  " + e.message)
            e.printStackTrace()
        }
    }