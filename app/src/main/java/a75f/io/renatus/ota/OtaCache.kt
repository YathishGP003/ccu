package a75f.io.renatus.ota

import a75f.io.logic.Globals
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.*

/**
 * Created by Manjunath K on 07-04-2023.
 */

class OtaCache {

    private var sharedPreferences: SharedPreferences =
        Globals.getInstance().applicationContext.getSharedPreferences(
                OTA_CACHE,
                Context.MODE_PRIVATE
            )

    fun clear(){
        sharedPreferences.edit().clear().apply()
    }
    fun saveRunningDeviceDetails(
        node: Int,
        deviceType: Int,
        majorVersion: Int,
        minorVersion: Int,
        mLwMeshAddresses: ArrayList<Int>
    ) {
        sharedPreferences.edit().putInt(NODE_ADDRESS, node).apply()
        sharedPreferences.edit().putInt(DEVICE_TYPE, deviceType).apply()
        sharedPreferences.edit().putInt(MAJOR_VERSION, majorVersion).apply()
        sharedPreferences.edit().putInt(MINOR_VERSION, minorVersion).apply()

        val meshList = HashSet<String>()
        mLwMeshAddresses.forEach { i -> meshList.add(i.toString()) }
        sharedPreferences.edit().putStringSet(MESH_LIST, meshList).apply()
    }

    fun getRunningNodeAddress(): Int {
        return sharedPreferences.getInt(NODE_ADDRESS, -1)
    }

    fun getDeviceType(): Int {
        return sharedPreferences.getInt(DEVICE_TYPE, -1)
    }

    fun getMajorVersion(): Int {
        return sharedPreferences.getInt(MAJOR_VERSION, -1)
    }

    fun getMinorVersion(): Int {
        return sharedPreferences.getInt(MINOR_VERSION, -1)
    }

    fun getMeshList(): MutableSet<String>? {
        return sharedPreferences.getStringSet(MESH_LIST, emptySet())
    }

    private fun getRequestList(): String? {
        return sharedPreferences.getString(REQUEST_LIST, null)
    }

    private fun saveRequestList(requestMap: LinkedTreeMap<String, LinkedTreeMap<String,String>>) {
        val requestMapString = Gson().toJson(requestMap)
        sharedPreferences.edit().putString(REQUEST_LIST, requestMapString).apply()
    }

    fun saveRequest(intent: Intent) {
        val request = getRequestFromIntent(intent)
        val requestMap = getRequestMap()
        requestMap[request[MESSAGE_ID]] = request
        saveRequestList(requestMap)
        logIt("${request[MESSAGE_ID]} New request saved")
    }

    private fun getRequestFromIntent(intent: Intent): LinkedTreeMap<String,String> {
        val map = LinkedTreeMap<String,String>()
        map[ID] = intent.getStringExtra(ID)!!
        map[FIRMWARE_VERSION] = intent.getStringExtra(FIRMWARE_VERSION)!!
        map[CMD_LEVEL] = intent.getStringExtra(CMD_LEVEL)!!
        map[MESSAGE_ID] = intent.getStringExtra(MESSAGE_ID)!!
        map[CMD_TYPE] = intent.getStringExtra(CMD_TYPE)!!
        map[RETRY_COUNT] = "0"
        return map
    }
    fun getIntentFromRequest(request: LinkedTreeMap<String,String>): Intent {
        val otaUpdateIntent = Intent(Globals.IntentActions.PUBNUB_MESSAGE)
        otaUpdateIntent.putExtra(ID, request[ID])
        otaUpdateIntent.putExtra(FIRMWARE_VERSION,request[FIRMWARE_VERSION])
        otaUpdateIntent.putExtra(CMD_LEVEL, request[CMD_LEVEL])
        otaUpdateIntent.putExtra(MESSAGE_ID, request[MESSAGE_ID])
        otaUpdateIntent.putExtra(CMD_TYPE, request[CMD_TYPE])
        otaUpdateIntent.putExtra(RETRY_COUNT, request[RETRY_COUNT])
        return otaUpdateIntent
    }

    fun removeRequest(messageId: String) {
        val requestMap = getRequestMap()
        requestMap.remove(messageId)
        saveRequestList(requestMap)
        logIt("$messageId has been removed")
    }

    fun updateRetryCount(messageId: String) {
        try {
            val requestMap = getRequestMap()

            val request = requestMap[messageId]
            if (request != null) {
                request[RETRY_COUNT] = (request[RETRY_COUNT]!!.toInt() + 1).toString()
                saveRequestList(requestMap)
            }
        } catch (e: Exception){
            logIt(e.message!!)
        }
    }

    fun getRequestMap(): LinkedTreeMap<String, LinkedTreeMap<String,String>> {
        val currentValues = getRequestList()
        try {
            if (!currentValues.isNullOrBlank()) return Gson().fromJson(
                currentValues,
                LinkedTreeMap<String, LinkedTreeMap<String,String>>().javaClass
            )
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
        return LinkedTreeMap()
    }

    fun restoreOtaRequests (context: Context) {
       val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        appScope.launch {
            val dispatcher = this.coroutineContext
            CoroutineScope(dispatcher).launch {
                try {
                    withContext(Dispatchers.IO) {
                        Thread.sleep(FIVE_MINUTES)
                    }
                    val cache = OtaCache()
                    val otaRequests = cache.getRequestMap()
                    logIt("Request list $otaRequests")
                    if (!otaRequests.isEmpty()) {
                        otaRequests.forEach { (_: String?, request: LinkedTreeMap<String, String>) ->
                            val intent = cache.getIntentFromRequest(request)
                            context.sendBroadcast(intent)
                        }
                    }
                } catch (e : InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun logIt(msg: String) {
        Log.i(OTA_CACHE, msg)
    }

    companion object {
        const val OTA_CACHE = "otaCache"
        const val NODE_ADDRESS = "NODE_ADDRESS"
        const val DEVICE_TYPE = "DEVICE_TYPE"
        const val MAJOR_VERSION = "MAJOR_VERSION"
        const val MINOR_VERSION = "MINOR_VERSION"
        const val MESH_LIST = "MESH_LIST"
        const val REQUEST_LIST = "REQUEST_LIST"
        const val ID = "id"
        const val FIRMWARE_VERSION = "firmwareVersion"
        const val CMD_LEVEL = "cmdLevel"
        const val MESSAGE_ID = "messageId"
        const val CMD_TYPE = "remoteCmdType"
        const val RETRY_COUNT = "retryCount"
        const val FIVE_MINUTES = 5 * 60000L
    }

}