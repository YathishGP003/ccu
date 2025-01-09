package a75f.io.alerts

import a75f.io.alerts.log.LogLevel
import a75f.io.alerts.log.LogOperation
import a75f.io.alerts.log.SequencerLogsCallback
import a75f.io.logger.CcuLog
import com.google.gson.Gson

class CustomContext(private val sequencerLogsCallback: SequencerLogsCallback) {
    fun debugLog(key: String, value: String) {
        val message = "$key<-->$value"
        CcuLog.d(AlertProcessor.TAG_CCU_ALERTS, message)

        val resultMap = mutableMapOf<String, String>()
        resultMap[key] = value

        sequencerLogsCallback.logInfo(
            LogLevel.INFO,
            LogOperation.valueOf("SEQUENCER_LOG"),
            message,
            HaystackService.MSG_SUCCESS, getResultJson(resultMap)
        )
    }

    private fun getResultJson(hashMap: MutableMap<String, String>): String {
        val list: MutableList<MutableMap<*, *>> =
            ArrayList()
        list.add(hashMap)
        return Gson().toJson(list)
    }
}