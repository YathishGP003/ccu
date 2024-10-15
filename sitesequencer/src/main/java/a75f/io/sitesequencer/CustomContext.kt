package a75f.io.sitesequencer

import a75f.io.logger.CcuLog
import a75f.io.sitesequencer.log.LogLevel
import a75f.io.sitesequencer.log.LogOperation
import a75f.io.sitesequencer.log.SequencerLogsCallback

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