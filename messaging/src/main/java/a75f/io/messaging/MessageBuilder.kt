package a75f.io.messaging

import a75f.io.data.message.MESSAGE_ATTRIBUTE_AUTO_CX_STATE
import a75f.io.data.message.MESSAGE_ATTRIBUTE_AUTO_CX_STOP_TIME
import a75f.io.data.message.MESSAGE_ATTRIBUTE_COMMAND
import a75f.io.data.message.MESSAGE_ATTRIBUTE_ID
import a75f.io.data.message.MESSAGE_ATTRIBUTE_IDS
import a75f.io.data.message.MESSAGE_ATTRIBUTE_LEVEL
import a75f.io.data.message.MESSAGE_ATTRIBUTE_LOG_LEVEL
import a75f.io.data.message.MESSAGE_ATTRIBUTE_MESSAGE_ID
import a75f.io.data.message.MESSAGE_ATTRIBUTE_REMOTE_CMD_TYPE
import a75f.io.data.message.MESSAGE_ATTRIBUTE_SEQUENCE_ID
import a75f.io.data.message.MESSAGE_ATTRIBUTE_SITE_ID
import a75f.io.data.message.MESSAGE_ATTRIBUTE_VERSION
import a75f.io.data.message.MESSAGE_ATTRIBUTE_WHO
import a75f.io.data.message.Message
import a75f.io.messaging.exceptions.InvalidMessageFormatException
import a75f.io.messaging.handler.AutoCommissioningStateHandler
import a75f.io.messaging.handler.CREATE_CUSTOM_ALERT_DEF_CMD
import a75f.io.messaging.handler.DELETE_CUSTOM_ALERT_DEF_CMD
import a75f.io.messaging.handler.DELETE_SITE_DEFS_CMD
import a75f.io.messaging.handler.KEY_ALERT_DEF_IDS
import a75f.io.messaging.handler.KEY_ALERT_IDS
import a75f.io.messaging.handler.REMOVE_ALERT_CMD
import a75f.io.messaging.handler.RemoteCommandUpdateHandler
import a75f.io.messaging.handler.RemoveEntityHandler
import a75f.io.messaging.handler.SchedulerRevampMigrationHandler
import a75f.io.messaging.handler.SiteSyncHandler
import a75f.io.messaging.handler.UPDATE_CUSTOM_ALERT_DEF_CMD
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

fun messageToJson(message : Message) : JsonObject {
    val gson: Gson = GsonBuilder().create()
    val messageType = object : TypeToken<Message>() {}.type
    return gson.toJsonTree(message, messageType).asJsonObject
}

fun jsonToMessage(msgJson : JsonObject) : Message {
    val messageId = msgJson.get(MESSAGE_ATTRIBUTE_MESSAGE_ID).asString
    if (messageId.isNullOrEmpty()) {
        throw InvalidMessageFormatException("Invalid messageId")
    }
    val messageContent = msgJson.asJsonObject.get("message")
    if (messageContent.isJsonNull) {
        throw InvalidMessageFormatException("Invalid message")
    }

    val messagePojo = Message(messageId)
    messagePojo.command = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_COMMAND)?.asString



    if (messagePojo.command == null) {
        throw InvalidMessageFormatException("Invalid Command")
    }

    if(messagePojo.command == SchedulerRevampMigrationHandler.CMD)
        return messagePojo

    messagePojo.id = messagePojo.command?.let { parseId(messageContent as JsonObject, it) }
    messagePojo.ids = messagePojo.command?.let { parseIds(messageContent as JsonObject, it) }

    messagePojo.value = messageContent.asJsonObject.get("val")?.asString
    messagePojo.who = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_WHO)?.asString
    messagePojo.remoteCmdType = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_REMOTE_CMD_TYPE)?.asString
    if (messagePojo.remoteCmdType != null) {
        messagePojo.remoteCmdLevel = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_LEVEL)?.asString
    } else {
        messagePojo.level = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_LEVEL)?.asInt
    }
    messagePojo.version = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_VERSION)?.asString
    messagePojo.timeToken = msgJson.get("timetoken").asLong

    if(messagePojo.command.equals(AutoCommissioningStateHandler.CMD)){
        messagePojo.autoCXStopTime = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_AUTO_CX_STOP_TIME)?.asString
        messagePojo.autoCXState = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_AUTO_CX_STATE)?.asInt?: 0
    }

    if(messagePojo.remoteCmdType.equals(RemoteCommandUpdateHandler.UPDATE_CCU_LOG_LEVEL)){
        messagePojo.loglevel = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_LOG_LEVEL)?.asString
    }

    if(messagePojo.remoteCmdType.equals(RemoteCommandUpdateHandler.SAVE_SEQUENCER_LOGS)){
        messagePojo.sequenceId = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_SEQUENCE_ID)?.asString
    }

    return messagePojo
}

private fun parseId(messageContent : JsonObject , command : String) : String?{
    return when(command) {
        AutoCommissioningStateHandler.CMD -> messageContent.asJsonObject.get("ccuId")?.asString
        SiteSyncHandler.CMD -> messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_SITE_ID)?.asString
        CREATE_CUSTOM_ALERT_DEF_CMD,
        UPDATE_CUSTOM_ALERT_DEF_CMD,
        DELETE_CUSTOM_ALERT_DEF_CMD -> messageContent.asJsonObject.get("definitionId")?.asString
        REMOVE_ALERT_CMD -> messageContent.asJsonObject.get("alertId")?.asString
        else -> messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_ID)?.asString
    }
}

private fun parseIds(messageContent : JsonObject , command : String) : List<String>?{
    return when(command) {
        RemoveEntityHandler.CMD -> messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_IDS)?.asJsonArray?.map {
            it.toString() }
        DELETE_SITE_DEFS_CMD -> messageContent.asJsonObject.get(KEY_ALERT_DEF_IDS)?.asJsonArray?.map { it.asString }
        DELETE_SITE_DEFS_CMD -> messageContent.asJsonObject.get(KEY_ALERT_IDS)?.asJsonArray?.map { it.asString }
        else -> messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_IDS)?.asJsonArray?.map { it.asString }
    }
}

