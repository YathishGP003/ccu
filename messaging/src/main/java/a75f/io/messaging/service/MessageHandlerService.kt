package a75f.io.messaging.service

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.database.Message
import a75f.io.messaging.database.MessageDatabaseBuilder
import a75f.io.messaging.database.MessageDatabaseHelper
import a75f.io.messaging.handler.*
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

class MessageHandlerService private constructor(context: Context){
    private val appContext = context
    private val dispatcher = HandlerThread("MessageHandlerService")
                                        .apply { start() }
                                        .looper.let { Handler(it) }
                                        .asCoroutineDispatcher()
    private val messagingScope = CoroutineScope(dispatcher + SupervisorJob())
    private val messagingDbHelper = MessageDatabaseHelper(MessageDatabaseBuilder.getInstance(appContext))

    private var alertMessageHandler: AlertMessageHandler? = null
    private fun alertMessageHandler(): AlertMessageHandler? {
        if (alertMessageHandler == null) {
            alertMessageHandler = AlertMessageHandler.instanceOf()
        }
        return alertMessageHandler
    }

    companion object {
        private var instance : MessageHandlerService? = null

        fun  getInstance(context: Context): MessageHandlerService {
            synchronized(this) {
                if (instance == null)  // NOT thread safe!
                    instance = MessageHandlerService(context)
            }
            return instance!!
        }
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
        messagingScope.launch {
            doHandleMessage(messageToJson(message), appContext)
        }
    }

    private fun messageToJson(message : Message) : JsonObject {
        val gson: Gson = GsonBuilder().create()
        val messageType = object : TypeToken<a75f.io.messaging.database.Message>() {}.type
        return gson.toJsonTree(message, messageType).asJsonObject
    }

    private fun doHandleMessage(msg: JsonObject, context: Context) {
        CcuLog.i(L.TAG_CCU_MESSAGING, " handleMessage $msg")
        when (val cmd = if (msg["command"] != null) msg["command"].asString else "") {
            UpdateEntityHandler.CMD -> UpdateEntityHandler.updateEntity(msg)
            UpdatePointHandler.CMD -> UpdatePointHandler.handleMessage(msg)
            SiteSyncHandler.CMD -> SiteSyncHandler.handleMessage(msg, context)
            UpdateScheduleHandler.CMD, UpdateScheduleHandler.ADD_SCHEDULE -> UpdateScheduleHandler.handleMessage(
                msg
            )
            UpdateScheduleHandler.DELETE_SCHEDULE -> CCUHsApi.getInstance()
                .removeEntity(msg["id"].asString)
            RemoveEntityHandler.CMD -> RemoveEntityHandler.handleMessage(msg)
            RemoteCommandUpdateHandler.CMD -> RemoteCommandUpdateHandler.handleMessage(msg, context)
            CREATE_CUSTOM_ALERT_DEF_CMD,
            UPDATE_CUSTOM_ALERT_DEF_CMD -> alertMessageHandler()?.handleCustomAlertDefMessage(
                msg
            )
            REMOVE_ALERT_CMD, REMOVE_ALERTS_CMD -> alertMessageHandler()?.handleAlertRemoveMessage(
                cmd,
                msg
            )
            CREATE_PREDEFINED_ALERT_DEF_CMD,
            UPDATE_PREDEFINED_ALERT_DEF_CMD,
            DELETE_PREDEFINED_ALERT_DEF_CMD -> alertMessageHandler()?.handlePredefinedAlertDefMessage(
                msg
            )
            DELETE_CUSTOM_ALERT_DEF_CMD,
            DELETE_SITE_DEFS_CMD -> alertMessageHandler()?.handleAlertDefRemoveMessage(
                cmd,
                msg
            )
            else -> CcuLog.d(L.TAG_CCU_PUBNUB, "UnSupported PubNub Command : $cmd")
        }
    }
}