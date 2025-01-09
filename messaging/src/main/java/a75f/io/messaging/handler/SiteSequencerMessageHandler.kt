@file:JvmName("SiteSequencerMessageHandlers")

package a75f.io.messaging.handler

import a75f.io.alerts.AlertManager
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.messaging.MessageHandler
import a75f.io.sitesequencer.SequenceManager
import a75f.io.sitesequencer.SequencerLogUtil
import a75f.io.sitesequencer.SequencerParser
import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val CMD_SEQUENCE_CREATED = "sequenceCreated"
const val CMD_SEQUENCE_UPDATED = "sequencesUpdated"
const val CMD_SEQUENCE_DELETED = "sequencesDeleted"

//@Singleton
class SiteSequencerMessageHandler(
    private val sequenceManager: SequenceManager,
    private val haystackApi: CCUHsApi
) {

    class SequenceCreatedHandler : MessageHandler {

        override val command = listOf(CMD_SEQUENCE_CREATED)

        @SuppressLint("CheckResult")
        override fun handleMessage(jsonObject: JsonObject, context: Context) {
            CcuLog.d(TAG, "SequenceCreatedHandler: $jsonObject")

            val siteIdRef = CCUHsApi.getInstance().siteIdRef.`val`
            CcuLog.d(TAG, "SequenceCreatedHandler: $siteIdRef")

            val sequenceId = instanceOf().sequenceManager.getSequenceId(jsonObject)

            // Launch a coroutine to perform the work on a background thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Switch to the IO context for the network call
                    val response = instanceOf().sequenceManager.siteSequencerService.getSiteDefinitions(siteIdRef)
                    response.forEach { siteSequencerDefinition ->
                        if (siteSequencerDefinition.seqId == sequenceId) {
                            CcuLog.d(
                                TAG,
                                "SequenceCreatedHandler get created sequence=> ${siteSequencerDefinition.seqId}"
                            )
                            instanceOf().sequenceManager.addSequencerDefinition(siteSequencerDefinition)
                        }
                    }
                } catch (e: Exception) {
                    // Log and handle any errors that occur during the network request
                    CcuLog.e(TAG, "#Error fetching site definitions", e)
                }
            }
        }
    }


    class SequenceUpdatedHandler : MessageHandler {
        override val command = listOf(CMD_SEQUENCE_UPDATED)

        @SuppressLint("CheckResult")
        override fun handleMessage(jsonObject: JsonObject, context: Context) {
            CcuLog.d(TAG, "SequenceUpdatedHandler jsonObject=> $jsonObject")

            val siteIdRef = CCUHsApi.getInstance().siteIdRef.`val`
            CcuLog.d(TAG, "SequenceUpdatedHandler: $siteIdRef")

            instanceOf().sequenceManager.getSequenceIds(jsonObject)?.forEach { sequenceId ->
                CcuLog.d(TAG, "SequenceUpdatedHandler message content=> ${sequenceId.asString}")
                instanceOf().sequenceManager.removePendingIntent(sequenceId.asString)
                instanceOf().sequenceManager.getSequenceById(sequenceId.asString)?.let {
                    instanceOf().sequenceManager.fixAlertsBySequenceId(it)
                }
                instanceOf().sequenceManager.deleteDefinition(sequenceId.asString)
                val fileName = "seq_" + CCUHsApi.getInstance().ccuId + "_" + sequenceId.asString + ".json"
                SequencerLogUtil.deleteJsonFile(context, fileName)
            }

            val updatedIdsMap = instanceOf().sequenceManager.getSequenceIdsMap(jsonObject)


            // Launch a coroutine to perform the work on a background thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Switch to the IO context for the network call
                    val response = instanceOf().sequenceManager.siteSequencerService.getSiteDefinitions(siteIdRef)
                    response.forEach { siteSequencerDefinition ->
                        if (updatedIdsMap.containsKey(siteSequencerDefinition.seqId)) {
                            // this is updated sequence
                            CcuLog.d(
                                TAG,
                                "SequenceUpdatedHandler get updated sequence=> ${siteSequencerDefinition.seqId} --isEnabled--${siteSequencerDefinition.enabled}"
                            )
                            if(siteSequencerDefinition.enabled){
                                instanceOf().sequenceManager.addSequencerDefinition(siteSequencerDefinition)
                            }else{
                                // sequence is disabled remove it and fix alerts remove log file
                                deleteSequence(siteSequencerDefinition.seqId, context)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log and handle any errors that occur during the network request
                    CcuLog.e(TAG, "###Error fetching site definitions", e)
                }
            }
        }
    }

    class SequenceDeletedHandler : MessageHandler {
        override val command = listOf(CMD_SEQUENCE_DELETED)
        override fun handleMessage(jsonObject: JsonObject, context: Context) {
            CcuLog.d(TAG, "SequenceDeletedHandler: $jsonObject")
            instanceOf().sequenceManager.getSequenceIds(jsonObject)?.forEach { deletedSeqId ->
                deleteSequence(deletedSeqId.asString, context)
            }
        }
    }

    companion object {

        val TAG: String? = SequencerParser.TAG_CCU_SITE_SEQUENCER

        // we use this method until we implement dependency injection.
        @JvmStatic
        fun instanceOf() = SiteSequencerMessageHandler(
            SequenceManager.getInstance(),
            CCUHsApi.getInstance()
        )

        fun deleteSequence(deletedSeqId: String, context: Context) {
            instanceOf().sequenceManager.removePendingIntent(deletedSeqId)
            instanceOf().sequenceManager.getSequenceById(deletedSeqId)?.let {
                instanceOf().sequenceManager.fixAlertsBySequenceId(it)
            }
            AlertManager.getInstance().removeAlertDefUsingAlertDefId(deletedSeqId)
            instanceOf().sequenceManager.deleteDefinition(deletedSeqId)
            val fileName = "seq_" + CCUHsApi.getInstance().ccuId + "_" + deletedSeqId + ".json"
            SequencerLogUtil.deleteJsonFile(context, fileName)
        }
    }

}
