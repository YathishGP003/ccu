package a75f.io.messaging.service

import a75f.io.data.message.DatabaseHelper
import a75f.io.data.message.Message
import a75f.io.data.message.updateMessage
import a75f.io.data.message.updateMessageHandled
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.MessageHandler
import a75f.io.messaging.handler.RemoteCommandUpdateHandler
import a75f.io.messaging.messageToJson
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_MESSAGE_RETRY : Int = 5
const val UPDATE_CCU_RETRY_MILLIS : Long = 15 * 60 * 1000
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
                    if (message.retryCount >= MAX_MESSAGE_RETRY) {
                        continue
                    }
                    CcuLog.i(L.TAG_CCU_MESSAGING, "handleUnhandledMessage $message  -  " +
                            "                               ${System.currentTimeMillis()}")

                    //Since the precessing time for Update_ccu is network dependent,
                    //it is given a longer time before retrying.
                    if (isUpdateCcuCommand(message)) {
                        if (System.currentTimeMillis() > (message.receivedTime +
                                                    message.retryCount * UPDATE_CCU_RETRY_MILLIS)) {
                            doHandleMessage(message, appContext)
                        }
                    } else {
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
                if (isUpdateCcuCommand(message)) {
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

    private fun isUpdateCcuCommand(message: Message) : Boolean {
        return message.remoteCmdType != null &&
                message.remoteCmdType == RemoteCommandUpdateHandler.UPDATE_CCU
    }
}