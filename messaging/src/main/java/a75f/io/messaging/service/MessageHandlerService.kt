package a75f.io.messaging.service

import a75f.io.data.message.DatabaseHelper
import a75f.io.data.message.Message
import a75f.io.data.message.messageToJson
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.handler.MessageHandler
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_MESSAGE_RETRY : Int = 5
@Singleton
class MessageHandlerService @Inject constructor(private val appContext: Context,
                                                private val messagingDbHelper : DatabaseHelper){

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

    /**
     * Fetch all unhandled messages from DB and process them.
     */
    fun handleMessages() {
        messagingScope.launch {
            messagingDbHelper.getAllUnhandledMessage().collect {
                for (message in it) {
                    //Converting Message to json can be avoided, but all the handlers currently expect a
                    // json.
                    CcuLog.i(L.TAG_CCU_MESSAGING, "handleUnhandledMessage $message")
                    if (message.retryCount < MAX_MESSAGE_RETRY) {
                        doHandleMessage(messageToJson(message), appContext)
                    }
                }
            }
        }
    }

    /**
     * Handle a single message.
     */
    fun handleMessage(message : Message) {
        CcuLog.i(L.TAG_CCU_MESSAGING,"handleMessage $message")
        messagingScope.launch {
            doHandleMessage(messageToJson(message), appContext)
        }
    }
    private fun doHandleMessage(msg: JsonObject, context: Context) {
        val cmd = if (msg["command"] != null) msg["command"].asString else ""

        if (cmd.isNotEmpty()) {
            messageHandlers.find { it.command.contains(cmd) }?.let{
                CcuLog.i(L.TAG_CCU_MESSAGING, "Handler Found for $cmd")
                it.handleMessage(msg, context)
                    ?: CcuLog.d(L.TAG_CCU_MESSAGING, "UnSupported Message Command : $cmd")
            }
        } else {
            CcuLog.d(L.TAG_CCU_MESSAGING, "Empty Message cmd : $msg")
        }
    }

    fun isCommandSupported(cmd : String?) : Boolean {
        if (cmd?.isEmpty() == true) {
            return false
        }
        messageHandlers.find { it.command.contains(cmd) }?.let{
            return true
        }
        return false
    }
}