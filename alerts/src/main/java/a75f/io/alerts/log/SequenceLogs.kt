package a75f.io.alerts.log

class FullLogs{
    private val fullLogs: List<SequenceLogs> = mutableListOf()

    fun addLog(log: SequenceLogs){
        (fullLogs as MutableList).add(log)
    }
}

class SequenceLogs(val id: String, val seqId: String, val siteRef: String, val ccuId: String, val ccuName: String?){
    val lastRunId: String = ""
    val logs: List<SequenceMethodLog> = mutableListOf()

    fun addLog(log: SequenceMethodLog){
        (logs as MutableList).add(log)
    }
}

data class SequenceMethodLog(
    val level: LogLevel = LogLevel.INFO,
    val operation: LogOperation,
    val message: String,
    val result: String,
    val timestamp: String,
    val expiresAt: String,
    val resultJson:String? = null
)

enum class LogLevel {
    OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL
}

const val HAYSTACK_SERVICE = "haystackService"
const val ALERTS_SERVICE = "alertsService"
const val SEQUENCE_RUNNER = "Seq_Runner"
const val CONTEXT_SERVICE = "ctx"

enum class LogOperation(val key: String, val value: String) {
    CLEAR_POINT_VALUES(HAYSTACK_SERVICE, "CLEAR_POINT_VALUES"),
    FIND_ENTITY_BY_FILTER(HAYSTACK_SERVICE, "findEntityByFilter"),
    FIND_BY_FILTER(HAYSTACK_SERVICE, "findByFilter"),
    FIND_BY_ID(HAYSTACK_SERVICE, "findById"),
    POINT_WRITE(HAYSTACK_SERVICE, "pointWrite"),
    POINT_WRITE_MANY(HAYSTACK_SERVICE, "pointWriteMany"),
    FETCH_VALUE_BY_ID(HAYSTACK_SERVICE, "fetchValueById"),
    FETCH_VALUE_BY_HIS_READ_MANY(HAYSTACK_SERVICE, "fetchValueByHisReadMany"),
    FETCH_VALUE_BY_POINT_WRITE_MANY(HAYSTACK_SERVICE, "fetchValueByPointWriteMany"),
    HIS_WRITE(HAYSTACK_SERVICE, "hisWrite"),
    HIS_WRITE_MANY(HAYSTACK_SERVICE, "hisWriteMany"),
    FETCH_VALUE_BY_POINT_WRITE_MANY_AT_LEVEL(HAYSTACK_SERVICE, "fetchValueByPointWriteManyAtLevel"),
    HIS_READMANY_INTERPOLATE(HAYSTACK_SERVICE, "hisReadManyInterpolate"),
    AGGREGATE_TIME_SERIES(HAYSTACK_SERVICE, "aggregateTimeSeries"),
    TRIGGER_ALERT(ALERTS_SERVICE, "triggerAlert"),
    FIX_ALERTS(ALERTS_SERVICE, "fixAlerts"),
    SEQUENCER_LOG(CONTEXT_SERVICE, "sequencerLog"),

    /* Generic Operations */
    GENERIC_INFO(SEQUENCE_RUNNER, "runTheSnippet"),
    LOG_HAYSTACK_CALLS(SEQUENCE_RUNNER, "logHaystackCalls"),
    LOG_EXTERNAL_API_CALLS(SEQUENCE_RUNNER, "logExternalApiCalls"),
    SEQUENCE_TERMINATION(SEQUENCE_RUNNER, "termination"),
    EXECUTE_EXTERNAL_API("ExternalApiService", "ExecuteExternalApi");

    companion object {
        infix fun from(value: String): LogOperation? =
            LogOperation.values().firstOrNull { it.value == value }
    }
}