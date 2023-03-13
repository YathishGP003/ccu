package a75f.io.data.message

import a75f.io.messaging.exceptions.InvalidMessageFormat
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
        throw InvalidMessageFormat("Invalid messageId")
    }
    val messageContent = msgJson.asJsonObject.get("message")
    if (messageContent.isJsonNull) {
        throw InvalidMessageFormat("Invalid message")
    }

    var messagePojo = Message(messageId)
    messagePojo.command = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_COMMAND)?.asString

    if (messagePojo.command == null) {
        throw InvalidMessageFormat("Invalid Command")
    }

    if (messagePojo.command.equals("sync")) {
        messagePojo.id = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_SITE_ID)?.asString
    } else {
        messagePojo.id = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_ID)?.asString
    }

    messagePojo.ids = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_IDS)?.asJsonArray?.let {
        val gson = GsonBuilder().create()
        gson.fromJson(it, Array<String>::class.java).toList()
    }

    messagePojo.value = messageContent.asJsonObject.get("val")?.asString
    messagePojo.who = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_WHO)?.asString
    messagePojo.remoteCmdType = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_REMOTE_CMD_TYPE)?.asString
    if (messagePojo.remoteCmdType != null) {
        messagePojo.remoteCmdLevel = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_LEVEL)?.asString
    } else {
        messagePojo.level = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_LEVEL)?.asInt
    }
    messagePojo.timeToken = msgJson.get("timetoken").asLong

    return messagePojo
}
