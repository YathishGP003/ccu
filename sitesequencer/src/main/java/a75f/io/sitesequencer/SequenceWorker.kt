package a75f.io.sitesequencer

import a75f.io.alerts.AlertDefinition
import a75f.io.alerts.AlertManager
import a75f.io.alerts.log.LogLevel
import a75f.io.alerts.log.LogOperation
import a75f.io.alerts.log.SequenceLogs
import a75f.io.alerts.log.SequenceMethodLog
import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.util.hayStack
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.sitesequencer.log.SequencerLogsCallback
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Object
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.function.Consumer

class SequenceWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    private val TAG = SequencerParser.TAG_CCU_SITE_SEQUENCER
    private val coroutineTimeOut: Long = 3 * 60 * 1000
    private lateinit var sequenceLogs: SequenceLogs
    var mapOfPastAlerts  = mutableMapOf<String, AlertData>()
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        try {
            if(Globals.getInstance().isTestMode || Globals.getInstance().isTemporaryOverrideMode){
                CcuLog.d(TAG, "---test mode or temp override is on interrupt this sequence---")
                Result.failure()
            }else{
                withTimeout(coroutineTimeOut) {
                    processSequenceLogs()
                    Result.success()
                }
            }
        } catch (e: TimeoutCancellationException) {
            // If the task takes longer than 3 minutes, it will be cancelled and return failure
            Result.failure()
        }
    }

    private suspend fun processSequenceLogs() {
        try {
            mapOfPastAlerts.clear()
            val context = applicationContext
            // Retrieve data passed to the worker
            val inputData = inputData
            val seqId = inputData.getString("seqId")
            val frequency = inputData.getString("frequency")
            CcuLog.d(TAG, "fetch sequence with seqId: $seqId")
            val siteSequencerDefinition = SequenceManager.getInstance().getSequenceById(seqId!!)
            if (siteSequencerDefinition != null && siteSequencerDefinition.enabled) {
                if (!frequency.isNullOrEmpty() && frequency == "EVERY_MONTH" || frequency == "WEEKLY") {
                    SequencerSchedulerUtil.scheduleJob(context, siteSequencerDefinition)
                }

                CcuLog.d(
                    TAG,
                    "##starting evaluation for seqId: $seqId <--name--> ${siteSequencerDefinition.seqName} using j2v8 --isEnabled->${siteSequencerDefinition.enabled}"
                )
                sequencerJsUtil.def = siteSequencerDefinition
                sequenceLogs = SequenceLogs(
                    siteSequencerDefinition.seqId,
                    siteSequencerDefinition.seqId,
                    siteSequencerDefinition.siteRef,
                    CCUHsApi.getInstance().ccuId,
                    CCUHsApi.getInstance().ccuName
                )

                sequenceLogs.addLog(
                    SequenceMethodLog(
                        LogLevel.INFO,
                        LogOperation.GENERIC_INFO,
                        "Starting sequence evaluation using j2v8",
                        "pending",
                        Date(Calendar.getInstance().timeInMillis).toString(),
                        "expiresAt"
                    )
                )

//                evaluateJs(
//                    siteSequencerDefinition, siteSequencerDefinition.snippet, applicationContext,
//                    sequencerJsUtil, sequencerLogsCallback
//                )

                evaluateJsJ2v8(
                    siteSequencerDefinition, siteSequencerDefinition.snippet, applicationContext,
                    sequencerJsUtil, sequencerLogsCallback
                )

                filterAlerts(siteSequencerDefinition)
                createAlert()

                sequenceLogs.addLog(
                    SequenceMethodLog(
                        LogLevel.INFO, LogOperation.GENERIC_INFO,
                        "Ending sequence evaluation", "success", getTime(), "expiresAt"
                    )
                )

                CcuLog.d(
                    TAG,
                    "##ending evaluation for seqId: $seqId <--name--> ${siteSequencerDefinition.seqName}"
                )

                val fileName =
                    "seq_" + CCUHsApi.getInstance().ccuId + "_" + siteSequencerDefinition.seqId + ".json"
                SequencerLogUtil.dumpLogs(applicationContext, fileName, sequenceLogs)

            } else {
                CcuLog.d(TAG, "sequence definition not found for  seqId: $seqId")
            }
            Result.success()
        } catch (e: Exception) {
            CcuLog.e(TAG, "Error in doWork: ${e.message}")
            Result.failure() // Return failure if an error occurs
        }
    }

    private val sequencerLogsCallback = object : SequencerLogsCallback {
        override fun logVerbose(
            logLevel: LogLevel,
            operationName: LogOperation,
            message: String,
            result: String
        ) {
            sequenceLogs.addLog(
                SequenceMethodLog(
                    logLevel, operationName,
                    message, result, Date().toString(), Date().toString()
                )
            )
        }

        override fun logWarn(
            logLevel: LogLevel,
            operationName: LogOperation,
            message: String,
            result: String
        ) {
            sequenceLogs.addLog(
                SequenceMethodLog(
                    logLevel, operationName,
                    message, result, Date().toString(), Date().toString()
                )
            )
        }

        override fun logInfo(
            logLevel: LogLevel,
            operationName: LogOperation,
            message: String,
            result: String
        ) {
            sequenceLogs.addLog(
                SequenceMethodLog(
                    logLevel, operationName,
                    message, result, Date().toString(), Date().toString()
                )
            )
        }

        override fun logInfo(
            logLevel: LogLevel,
            operationName: LogOperation,
            message: String,
            result: String,
            resultJson: String?
        ) {
            sequenceLogs.addLog(
                SequenceMethodLog(
                    logLevel, operationName,
                    message, result, Date().toString(), Date().toString(), resultJson
                )
            )
        }

        override fun logError(
            logLevel: LogLevel,
            operationName: LogOperation,
            message: String,
            result: String
        ) {
            sequenceLogs.addLog(
                SequenceMethodLog(
                    logLevel, operationName,
                    message, result, Date().toString(), Date().toString()
                )
            )
        }

        override fun logDebug(
            logLevel: LogLevel,
            operationName: LogOperation,
            message: String,
            result: String
        ) {
            sequenceLogs.addLog(
                SequenceMethodLog(
                    logLevel, operationName,
                    message, result, Date().toString(), Date().toString()
                )
            )
        }
    }

    private val sequencerJsUtil = object : SequencerJsUtil(object : SequencerJsCallback {
        override fun triggerAlert(
            blockId: String?,
            notificationMsg: String?,
            message: String?,
            entityId: String?,
            contextHelper: Any?,
            def: SiteSequencerDefinition?
        ): Boolean {
            CcuLog.d(
                TAG,
                "triggerAlert blockId: $blockId notificationMsg: $notificationMsg message: $message entityId: $entityId"
            )

            val sequenceAlert = SequencerSchedulerUtil.findAlertByBlockId(def, blockId)
            if (sequenceAlert != null) {
                val alertDefinition = SequencerSchedulerUtil.createAlertDefinition(sequenceAlert)
                alertDefinition.alert.mMessage = message
                alertDefinition.alert.mNotificationMsg = notificationMsg
                val tempId = entityId!!.replaceFirst("@".toRegex(), "")
                mapOfPastAlerts["${alertDefinition.alert.alertDefId}:$blockId:$tempId"] = AlertData(sequenceAlert.title,
                    message.toString(), entityId, "sequencer", blockId, alertDefinition)
                if (def != null) {
                    CcuLog.d(TAG, "add this sequencer to alert def ui-->${def.seqId}")
                    alertDefinition.alert.alertDefId = def.seqId
                    alertDefinition.alert.setCreator("blockly")
                    val key = "${alertDefinition.alert.alertDefId}:$blockId:$tempId"
                    AlertManager.getInstance().addAlertDef(key, alertDefinition)
                    AlertManager.getInstance().sequenceLogsMap[def.seqId] = sequenceLogs
                }else{
                    CcuLog.d(TAG, "add this sequencer to alert def ui failed as def is not there")
                }
            } else {
                CcuLog.d(TAG, "sequenceAlert not found for blockId: $blockId")
            }
            return true
        }
    }, sequencerLogsCallback) {}

    private fun filterAlerts(siteSequencerDefinition: SiteSequencerDefinition) {

        siteSequencerDefinition.seqAlerts?.forEach { sequenceAlert ->
            // this change is there if next time alert is not there then it will be fixed,
            // suppose alerts A, B, C are there in db and now only A, C are triggered then B will be fixed
            AlertManager.getInstance()
                .getActiveAlertsByCreatorAndBlockId("sequencer", sequenceAlert.alertBlockId)
                .forEach(Consumer { alert: Alert ->
                    val keyFromDb = alert.alertDefId + ":"+ alert.blockId +":" + alert.equipId
                    if (!mapOfPastAlerts.containsKey(keyFromDb)) {

                        CcuLog.d(
                            TAG,
                            "No alert triggered fix alert for this definition equipId: ${alert.equipId} <--blockId--> ${alert.blockId}"
                        )
                        sequenceLogs.addLog(
                            SequenceMethodLog(
                                LogLevel.INFO,
                                LogOperation.GENERIC_INFO,
                                "No alert triggered fix alert for this definition equipId ->${alert.equipId} blockId-> ${alert.blockId}",
                                "success",
                                getTime(),
                                "expiresAt"
                            )
                        )
                        AlertManager.getInstance().fixAlert(alert)
                        AlertManager.getInstance().removeAlertDef(keyFromDb)
                    }
                })
        }
    }

    private fun createAlert() {
        val alertData = mapOfPastAlerts.values
        for (alert in alertData) {
            if (alert.alertDefinition.isMuted(
                    hayStack.ccuRef.`val`,
                    alert.entityId.replaceFirst("@", "")
                )
            ) {
                CcuLog.d(
                    TAG,
                    "Alert is muted for this definition entityId: ${alert.entityId} <--blockId--> ${alert.blockId}"
                )
                continue
            }
            CcuLog.d(
                TAG,
                "##create alert title -> ${alert.title} <-blockId-> ${alert.blockId} <entityId> ${alert.entityId} <message>${alert.message} <notificationMessage>${alert.alertDefinition.alert.mNotificationMsg}"
            )
            sequenceLogs.addLog(
                SequenceMethodLog(
                    LogLevel.INFO,
                    LogOperation.GENERIC_INFO,
                    "create alert title -> ${alert.title} <-blockId-> ${alert.blockId} <entityId> ${alert.entityId}",
                    "success",
                    getTime(),
                    "expiresAt"
                )
            )
            AlertManager.getInstance().generateAlertSequencerBlockly(
                alert.title,
                alert.message,
                alert.entityId,
                alert.s2,
                alert.blockId,
                alert.alertDefinition
            )
        }
    }

    private fun getTime(): String {
        val currentDateTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentDateTime)
    }
    private suspend fun evaluateJsJ2v8(
        def: SiteSequencerDefinition,
        javascriptSnippet: String?,
        mContext: Context,
        alertJsUtil: SequencerJsUtil,
        sequenceLogUtil: SequencerLogsCallback
    ) {
        V8.createV8Runtime(null, mContext.applicationInfo.dataDir).use { runtime ->
            runtime.use { runtime ->
                val lastRunId = UUID.randomUUID().toString()
                val haystackService = HaystackService(sequenceLogUtil, lastRunId, def.seqId)
                val haystackServiceObject = V8Object(runtime)

                registerAllMethods(haystackService, haystackServiceObject)

                haystackServiceObject.add("fetchValueById", haystackService.fetchValueById(runtime))

                runtime.add("haystack", haystackServiceObject)

                val persistBlockService = PersistBlockService.getInstance(def.seqId)
                val persistBlockServiceObject = V8Object(runtime)
                registerAllMethods(persistBlockService, persistBlockServiceObject)
                runtime.add("persistBlock", persistBlockServiceObject)

                val alertJsUtilJsObject = V8Object(runtime)
                registerAllMethods(alertJsUtil, alertJsUtilJsObject)
                runtime.add("alerts", alertJsUtilJsObject)

                val contextJsObject = V8Object(runtime)
                val customContext = CustomContext(sequenceLogUtil)
                registerAllMethods(customContext, contextJsObject)
                runtime.add("ctx", contextJsObject)

                runtime.executeVoidScript(javascriptSnippet)

                haystackService.release()
                haystackServiceObject.close()
                persistBlockServiceObject.close()
                alertJsUtilJsObject.close()
                contextJsObject.close()
                //runtime.close()

                if(haystackServiceObject.isReleased){
                    CcuLog.d(TAG, "haystackServiceObject is released")
                }
                if(persistBlockServiceObject.isReleased){
                    CcuLog.d(TAG, "persistBlockServiceObject is released")
                }
                if(alertJsUtilJsObject.isReleased){
                    CcuLog.d(TAG, "alertJsUtilJsObject is released")
                }
                if(contextJsObject.isReleased){
                    CcuLog.d(TAG, "contextJsObject is released")
                }
                if(runtime.isReleased){
                    CcuLog.d(TAG, "runtime is released")
                }
            }
        }
    }


    @Throws(Exception::class)
    fun registerAllMethods(javaObject: Any, v8Object: V8Object) {
        val clazz: Class<*> = javaObject.javaClass
        for (method in clazz.methods) {
            val methodName = method.name
            val parameterTypes = method.parameterTypes

            // Use reflection to get method parameter types and register it
            v8Object.registerJavaMethod(javaObject, methodName, methodName, parameterTypes)
        }
    }

}

data class AlertData(
    val title: String,
    val message: String,
    val entityId: String,
    val s2: String,
    val blockId: String?,
    val alertDefinition: AlertDefinition
)


class ConsolePrint {
    fun print(message: String?) {
        println(message)
    }
}
