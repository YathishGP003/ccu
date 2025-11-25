package a75f.io.logic.util.bacnet

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags.BACNET_CONFIG
import a75f.io.api.haystack.Tags.BACNET_DEVICE_JOB
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.util.bacnet.BacnetClientJob.scheduleJob
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED
import android.content.Context
import android.preference.PreferenceManager
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

val TAG = "BacnetUtility"

object BacnetUtility {

    fun getNetworkDetails(context: Context): JSONObject? {
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

    fun isBacnetInitialized(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        return preferences.getBoolean(IS_BACNET_INITIALIZED, false)
    }

    fun isBacnetMstpInitialized(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
        return preferences.getBoolean(BacnetConfigConstants.IS_BACNET_MSTP_INITIALIZED, false)
    }

    fun parseBacnetConfigData (equip : HashMap<Any, Any>): MutableMap<String, String> {
        val configMap = mutableMapOf<String, String>()
        equip[BACNET_CONFIG].let {
            it.toString().split(",").forEach { configItem ->
                val configKeyValueList = configItem.split(":")
                if (configKeyValueList.size == 2) {
                    configMap[configKeyValueList[0]] = configKeyValueList[1]
                }
            }
        }
        return configMap
    }

    fun updateBacnetHeartBeatPoint(newValue: Double, bacnetEquipId : String) {
        val heartBeatPointId = CCUHsApi.getInstance().readEntity("point and heartbeat and equipRef== \"${bacnetEquipId}\"")["id"]
        if (heartBeatPointId.toString().isNotEmpty()) {
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(heartBeatPointId.toString(), newValue)
            CcuLog.d(TAG, "--updateHeartBeatPoint--updated successfully --> ${heartBeatPointId.toString()}" )
        }
    }

    fun checkAndScheduleJobForBacnetClient() {
        if (isBacnetInitialized()) {
            val listOfBacnetEquip = CCUHsApi.getInstance().readAll("equip and bacnet and bacnetCur")
            if (listOfBacnetEquip.isNotEmpty()) {
                CcuLog.i(TAG, "Bacnet IP is initialized and Client device found, scheduling job to Monitor BACnet Client Devices")
                scheduleJob("BACnetClientJob", 60, 45, TimeUnit.SECONDS)
            } else {
                CcuLog.i(TAG, "Bacnet IP is initialized but no Client device found, skipping to schedule job to Monitor BACnet Client Devices")
            }
        } else {
            CcuLog.i(TAG, "Bacnet IP is not initialized, skipping to schedule job to Monitor BACnet Client Devices")
        }
    }
}