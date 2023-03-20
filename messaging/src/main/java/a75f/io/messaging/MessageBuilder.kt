package a75f.io.messaging

import a75f.io.data.message.*
import a75f.io.messaging.exceptions.InvalidMessageFormatException
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

    var messagePojo = Message(messageId)
    messagePojo.command = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_COMMAND)?.asString

    if (messagePojo.command == null) {
        throw InvalidMessageFormatException("Invalid Command")
    }

    if (messagePojo.command.equals("sync")) {
        messagePojo.id = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_SITE_ID)?.asString
    } else {
        messagePojo.id = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_ID)?.asString
    }

    val gsonBuilder = GsonBuilder().create()
    if (messagePojo.command.equals("removeEntity")) {
        messagePojo.ids = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_IDS)?.asJsonArray?.let {
            val listType = object : TypeToken<List<Map<String?, String?>?>?>() {}.type
            gsonBuilder.fromJson(it, listType)
        }
    } else {
        messagePojo.ids = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_IDS)?.asJsonArray?.let {
            gsonBuilder.fromJson(it, Array<String>::class.java).toList()
        }
    }

    messagePojo.value = messageContent.asJsonObject.get("val")?.asString
    messagePojo.who = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_WHO)?.asString
    messagePojo.remoteCmdType = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_REMOTE_CMD_TYPE)?.asString
    if (messagePojo.remoteCmdType != null) {
        messagePojo.remoteCmdLevel = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_LEVEL)?.asString
        messagePojo.version = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_VERSION)?.asString
    } else {
        messagePojo.level = messageContent.asJsonObject.get(MESSAGE_ATTRIBUTE_LEVEL)?.asInt
    }
    messagePojo.timeToken = msgJson.get("timetoken").asLong

    return messagePojo
}
