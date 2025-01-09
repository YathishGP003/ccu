package a75f.io.sitesequencer

import a75f.io.alerts.log.LogLevel
import a75f.io.alerts.log.LogOperation
import a75f.io.logger.CcuLog
import a75f.io.sitesequencer.log.SequencerLogsCallback
import com.google.gson.Gson

class CustomContext(private val sequencerLogsCallback: SequencerLogsCallback) {
    fun debugLog(key: String, value: String) {
        val message = "$key<-->$value"
        CcuLog.d("CCU_SITE_SEQUENCER", message)

        val resultMap = mutableMapOf<String, String>()
        resultMap[key] = value

        sequencerLogsCallback.logInfo(
            LogLevel.INFO,
            LogOperation.valueOf("SEQUENCER_LOG"),
            message,
            HaystackService.MSG_SUCCESS,
            getResultJson(resultMap)
        )
    }

    private fun getResultJson(hashMap: MutableMap<String, String>): String {
        val list: MutableList<MutableMap<*, *>> =
            ArrayList()
        list.add(hashMap)
        return Gson().toJson(list)
    }
}