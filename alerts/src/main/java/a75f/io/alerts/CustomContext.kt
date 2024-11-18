package a75f.io.alerts

import a75f.io.alerts.log.LogLevel
import a75f.io.alerts.log.LogOperation
import a75f.io.alerts.log.SequencerLogsCallback
import a75f.io.logger.CcuLog

class CustomContext(private val sequencerLogsCallback: SequencerLogsCallback) {
    fun debugLog(key: String, value: String) {
        val message = "$key<-->$value"
        CcuLog.d("CCU_SITE_SEQUENCER", message)
        sequencerLogsCallback.logInfo(
            LogLevel.INFO,
            LogOperation.valueOf("SEQUENCER_LOG"),
            message,
            HaystackService.MSG_SUCCESS
        )
    }
}