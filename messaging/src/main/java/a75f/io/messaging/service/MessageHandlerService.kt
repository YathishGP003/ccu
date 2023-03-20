package a75f.io.messaging.service

import a75f.io.data.message.*
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.MessageHandler
import a75f.io.messaging.exceptions.InvalidMessageFormatException
import a75f.io.messaging.handler.RemoteCommandUpdateHandler
import a75f.io.messaging.jsonToMessage
import a75f.io.messaging.messageToJson
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
                        doHandleMessage(message, appContext)
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
            doHandleMessage(message, appContext)
        }
    }
    private fun doHandleMessage(message: Message, context: Context) {

        val messageHandler = messageHandlers.find { it.command.contains(message.command) }
        if (messageHandler != null) {
            CcuLog.i(L.TAG_CCU_MESSAGING, "Handler Found for ${message.command}")
            try {
                if (shouldUpdateMessageBeforeHandling(message)) {
                    updateMessageHandled(message, context)
                }
                messageHandler.handleMessage(messageToJson(message), context)
                //Update command is processed asynchronously. The message will be updated once
                //handling is complete.OTA commands would also be tracked separately.
                if (message.remoteCmdType != null &&
                                        message.remoteCmdType == RemoteCommandUpdateHandler.UPDATE_CCU) {
                    updateMessageRetryStatus(message)
                } else {
                    updateMessageHandled(message, context)
                }
            //All the handlers do not have proper exception handling. We will use an umbrella
            //handler to avoid app crashing due to an invalid message.
            } catch (e : Exception) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "Failed to handle message $message", e)
                updateMessageFailed(message, e)
            }
        } else {
            CcuLog.d(L.TAG_CCU_MESSAGING, "UnSupported Message Command : $message")
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

    private fun updateMessageFailed(message: Message, e : java.lang.Exception) {
        message.error = e.message
        message.retryCount++
        updateMessage(message)
    }

    private fun updateMessageRetryStatus(message: Message) {
        message.retryCount++
        updateMessage(message)
    }

    /**
     * Only the commands which can result in an app closure are considered here.
     */
    private fun shouldUpdateMessageBeforeHandling(message : Message) : Boolean {
        return message.remoteCmdType != null &&
            (message.remoteCmdType == RemoteCommandUpdateHandler.RESTART_CCU
                    || message.remoteCmdType == RemoteCommandUpdateHandler.RESTART_TABLET)
    }
}