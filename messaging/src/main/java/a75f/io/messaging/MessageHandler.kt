package a75f.io.messaging

import android.content.Context
import com.google.gson.JsonObject

/**
 * A message handler that implements a handler for specific message command and updates message
 * database with the status of consumption. Specific handler implementation should provide a list
 * with or more commands it supports.
 * handleMessage should update the status of message consumption whenever possible.
 * (It may not be possible where handling is asynchronous, in such cases the message must be marked as
 * handled when the command execution is complete , eg, CCU app upgrades, OTA of Nodes.
 */
interface MessageHandler {
    val command : List<String>
    fun handleMessage(jsonObject: JsonObject, context: Context)
}