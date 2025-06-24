package a75f.io.messaging.handler

import a75f.io.api.haystack.util.hayStack
import a75f.io.data.message.MESSAGE_ATTRIBUTE_MESSAGE_ID
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.util.SequenceApplyPrefHandler
import a75f.io.messaging.MessageHandler
import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/*
* when equips are added to ConnectNode or any device from sequencer page, this handlers gets message
* */
class EntitiesApplyHandler : MessageHandler {
    val cmd: String = "entitiesApplied"

    override val command: List<String> = listOf(cmd)

    override fun handleMessage(jsonObject: JsonObject, context: Context) {
        CcuLog.d(L.TAG_CCU_SEQUENCE_APPLY, "<--------------->")
        CcuLog.d(
            L.TAG_CCU_SEQUENCE_APPLY, "Handle entitiesApplied" +
                    " jsonObject: $jsonObject"
        )
        val appliedDataString = jsonObject["appliedData"].asString

        val appliedDataJson = JsonParser.parseString(appliedDataString).asJsonObject
        val appliedDataArray = appliedDataJson["appliedData"].asJsonArray

        val seqId = appliedDataJson["seqId"].asString
        val messageId = jsonObject.get(MESSAGE_ATTRIBUTE_MESSAGE_ID).asString

        if (SequenceApplyPrefHandler.isMessageBeingHandled(context, messageId, seqId)) {
            CcuLog.i(L.TAG_CCU_SEQUENCE_APPLY, "Message is already being handled")
            return
        }

        SequenceApplyPrefHandler.markMessageAsHandling(context, messageId, seqId)

        val matchingEntry = appliedDataArray.find {
            val ccuRef = JsonParser.parseString(it.asJsonObject["ccuRef"].toString()).asString
            ccuRef == hayStack.ccuId.removePrefix("@")
        }

        if (matchingEntry == null) {
            CcuLog.e(L.TAG_CCU_SEQUENCE_APPLY, "No entry found for this CCU")
            return
        }

        val entryJson = matchingEntry.asJsonObject

        SQMetaDataHandler().handleMetaData(
            entryJson,
            seqId,
            messageId,
            context
        )
    }

}