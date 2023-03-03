package a75f.io.messaging.service

import a75f.io.data.message.DatabaseHelper
import a75f.io.data.message.Message
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.handler.MessageHandler
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageHandlerService @Inject constructor(val appContext: Context,
                                                val messagingDbHelper : DatabaseHelper){

    //private val appContext = context
    //private val messagingDbHelper : DatabaseHelper = dbHelper
    private val dispatcher = HandlerThread("MessageHandlerService")
                                .apply { start() }
                                .looper.let { Handler(it) }
                                .asCoroutineDispatcher()
    private val messagingScope = CoroutineScope(dispatcher + SupervisorJob())

    private val messageHandlers = mutableListOf<MessageHandler>()

    fun registerMessageHandler(handler : MessageHandler) {
        CcuLog.i(L.TAG_CCU_MESSAGING, "Added handler "+handler.command[0])
        messageHandlers.add(handler)
    }

    fun handleMessages() {
        messagingScope.launch {
            messagingDbHelper.getAllUnhandledMessage().collect {
                for (message in it) {
                    doHandleMessage(messageToJson(message), appContext)
                }
            }
        }
    }

    fun handleMessage(message : Message) {
        CcuLog.i(L.TAG_CCU_MESSAGING,"handleMessage $message")
        messagingScope.launch {
            doHandleMessage(messageToJson(message), appContext)
        }
    }

    private fun messageToJson(message : Message) : JsonObject {
        val gson: Gson = GsonBuilder().create()
        val messageType = object : TypeToken<Message>() {}.type
        return gson.toJsonTree(message, messageType).asJsonObject
    }

    private fun doHandleMessage(msg: JsonObject, context: Context) {
        val cmd = if (msg["command"] != null) msg["command"].asString else ""
        messageHandlers.forEach {
            msg -> CcuLog.i(L.TAG_CCU_MESSAGING, msg.command[0])
        }
        if (cmd.isNotEmpty()) {
            messageHandlers.find { it.command.contains(cmd) }?.handleMessage(msg, context)
                    ?: CcuLog.d(L.TAG_CCU_MESSAGING, "UnSupported Message Command : $cmd")
        } else {
            CcuLog.d(L.TAG_CCU_MESSAGING, "Empty Message cmd : $msg")
        }
    }
}