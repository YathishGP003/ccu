package a75f.io.renatus.ota

import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

/**
 * Created by Pramod Halliyavar on 06-09-2025.
 */

class SeqCache {

    private var sharedPreferences: SharedPreferences =
        Globals.getInstance().applicationContext.getSharedPreferences(
                SEQ_CACHE,
                Context.MODE_PRIVATE
            )

    fun clear() {
        val success = sharedPreferences.edit().clear().commit() // Use commit
        if (!success) {
            logIt("Failed to clear shared preferences sequence")
        } else {
            logIt("Shared preferences sequence cleared successfully")
        }
    }

    fun saveRunningDeviceDetails(
        node: Int,
        deviceType: Int,
        firmwareName: String,
        seqVersion: String,
        mLwMeshAddresses: ArrayList<Int>
    ) {
        sharedPreferences.edit().putInt(NODE_ADDRESS, node).apply()
        sharedPreferences.edit().putInt(DEVICE_TYPE, deviceType).apply()
        sharedPreferences.edit().putString(FIRMWARE_NAME, firmwareName).apply()
        sharedPreferences.edit().putString(META_NAME, seqVersion).apply()

        val meshList = HashSet<String>()
        mLwMeshAddresses.forEach { i -> meshList.add(i.toString()) }
        sharedPreferences.edit().putStringSet(MESH_LIST, meshList).apply()
    }

    fun getRunningNodeAddress(): Int {
        return sharedPreferences.getInt(NODE_ADDRESS, -1)
    }

    fun getRunningNodeAddressString(): Int {
        return sharedPreferences.getString(NODE_ADDRESS, null)?.toIntOrNull() ?: -1
    }

    fun getDeviceType(): Int {
        return sharedPreferences.getInt(DEVICE_TYPE, -1)
    }

    fun getFirmwareVersion(): Int {
        return sharedPreferences.getInt(FIRMWARE_NAME, -1)
    }

    fun getMetaVersion(): Int {
        return sharedPreferences.getInt(META_NAME, -1)
    }

    fun getMeshList(): MutableSet<String>? {
        return sharedPreferences.getStringSet(MESH_LIST, emptySet())
    }

    private fun getRequestList(): String? {
        return sharedPreferences.getString(REQUEST_LIST, null)
    }


    private fun saveRequestList(requestMap: LinkedTreeMap<String, LinkedTreeMap<String, String>>) {
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
        map[FIRMWARE_NAME] = intent.getStringExtra(FIRMWARE_NAME)!!
        map[META_NAME] = intent.getStringExtra(META_NAME)!!
        map[CMD_LEVEL] = intent.getStringExtra(CMD_LEVEL)!!
        map[MESSAGE_ID] = intent.getStringExtra(MESSAGE_ID)!!
        map[ERASE_SEQUENCE] = intent.getStringExtra(ERASE_SEQUENCE)!!
        map[CMD_TYPE] = intent.getStringExtra(CMD_TYPE)!!
        map[NODE_ADDRESS] = intent.getStringExtra(NODE_ADDRESS) // Since not applicable for firmware OTA type, no need of !!
        map[SEQ_NAME] = intent.getStringExtra(SEQ_NAME) // Since not applicable for firmware OTA type, no need of !!
        map[SEQ_VERSION] = intent.getStringExtra(SEQ_VERSION) // Since not applicable for firmware OTA type, no need of !!
        map[SEQ_SERVER_ID] = intent.getStringExtra(SEQ_SERVER_ID) ?: "" // Since not applicable for firmware OTA type, no need of !!
        map[RETRY_COUNT] = "0"
        return map
    }
    fun getIntentFromRequest(request: LinkedTreeMap<String,String>): Intent {
        val otaUpdateIntent = Intent(Globals.IntentActions.SEQUENCE_UPDATE_START)
        otaUpdateIntent.putExtra(ID, request[ID])
        otaUpdateIntent.putExtra(FIRMWARE_NAME,request[FIRMWARE_NAME])
        otaUpdateIntent.putExtra(META_NAME,request[META_NAME])
        otaUpdateIntent.putExtra(CMD_LEVEL, request[CMD_LEVEL])
        otaUpdateIntent.putExtra(MESSAGE_ID, request[MESSAGE_ID])
        otaUpdateIntent.putExtra(ERASE_SEQUENCE, request[ERASE_SEQUENCE])
        otaUpdateIntent.putExtra(CMD_TYPE, request[CMD_TYPE] ?: "")
        otaUpdateIntent.putExtra(SEQ_NAME, request[SEQ_NAME] ?: "") // Since SEQ_VERSION might not be applicable for firmware OTA type
        otaUpdateIntent.putExtra(NODE_ADDRESS, request[NODE_ADDRESS] ?: "") // Since NODE_ADDRESS might not be applicable for firmware OTA type
        otaUpdateIntent.putExtra(SEQ_VERSION, request[SEQ_VERSION] ?: "") // Since SEQ_VERSION might not be applicable for firmware OTA type
        otaUpdateIntent.putExtra(SEQ_SERVER_ID, request[SEQ_SERVER_ID] ?: "") // Since SEQ_SERVER_ID might not be applicable for firmware OTA type
        otaUpdateIntent.putExtra(RETRY_COUNT, request[RETRY_COUNT])
        return otaUpdateIntent
    }

    fun removeRequest(nodeAddress: String) {
        val requestMap = getRequestMap()
        if (requestMap.containsKey(nodeAddress)) {
            requestMap.remove(nodeAddress)
            saveRequestList(requestMap)
            logIt("$nodeAddress has been removed from request map")
        } else {
            logIt("Request ID $nodeAddress not found in map")
        }
    }

    fun updateRetryCount(messageId: String) {
        val requestMap = getRequestMap()
        val request = requestMap[messageId]
        if (request != null) {
            request[RETRY_COUNT] = (request[RETRY_COUNT]!!.toInt() + 1).toString()
            saveRequestList(requestMap)
        }
    }

    fun getRequestMap(): LinkedTreeMap<String, LinkedTreeMap<String, String>> {
        val currentValues = getRequestList()
        return try {
            if (!currentValues.isNullOrBlank()) {
                val type = object : TypeToken<LinkedTreeMap<String, LinkedTreeMap<String, String>>>() {}.type
                Gson().fromJson(currentValues, type)
            } else {
                LinkedTreeMap()
            }
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            LinkedTreeMap()
        }
    }

    fun restoreSeqRequests (context: Context) {
       val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        appScope.launch {
            try {
                delay(FIVE_MINUTES)
                val cache = SeqCache()
                val seqRequests = cache.getRequestMap()
                logIt("Seq Request list $seqRequests")
                if (!seqRequests.isEmpty()) {
                    seqRequests.forEach { (_: String?, request: LinkedTreeMap<String, String>) ->
                        val intent = cache.getIntentFromRequest(request)
                        context.sendBroadcast(intent)
                    }
                }
            } catch (e : InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun logIt(msg: String) {
        CcuLog.i(SEQ_CACHE, msg)
    }

    companion object {
        const val SEQ_CACHE = "seqCache"
        const val NODE_ADDRESS = "NODE_ADDRESS"
        const val DEVICE_TYPE = "DEVICE_TYPE"
        const val MESH_LIST = "MESH_LIST"
        const val REQUEST_LIST = "REQUEST_LIST"
        const val ID = "id"
        const val DEVICE_LIST = "deviceList"
        const val REMOVE_LIST = "removedDeviceList"
        const val META_NAME = "metaFileName"
        const val EMPTY_META_NAME = "emptyMetaFileName"
        const val FIRMWARE_NAME = "firmwareName"
        const val ERASE_SEQUENCE = "eraseSequence"
        const val ERASE_SEQUENCE_TRUE = "true"
        const val ERASE_SEQUENCE_FALSE = "false"
        const val EMPTY_FIRMWARE_NAME = "emptyFirmwareName"
        const val CMD_LEVEL = "cmdLevel"
        const val MESSAGE_ID = "messageId"
        const val CMD_TYPE = "remoteCmdType"
        const val RETRY_COUNT = "retryCount"
        const val SEQ_VERSION = "seqVersion"
        const val SEQ_SERVER_ID = "seqServerId"
        const val SEQ_NAME = "seqName"
        const val FIVE_MINUTES = 5 * 60000L
    }

}