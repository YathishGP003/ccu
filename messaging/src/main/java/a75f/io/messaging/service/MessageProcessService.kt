package a75f.io.messaging.service

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.database.Message
import a75f.io.messaging.database.MessageDatabaseBuilder
import a75f.io.messaging.database.MessageDatabaseHelper
import a75f.io.messaging.handler.*
import a75f.io.messaging.handler.AlertMessageHandler.Companion.instanceOf
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

class MessageProcessService : Service() {

    private var alertMessageHandler: AlertMessageHandler? = null
    private fun alertMessageHandler(): AlertMessageHandler? {
        if (alertMessageHandler == null) {
            alertMessageHandler = instanceOf()
        }
        return alertMessageHandler
    }

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        CcuLog.i(L.TAG_CCU_MESSAGING, "MessagingProcessService started" )
        val messagingDbHelper = MessageDatabaseHelper(MessageDatabaseBuilder.getInstance(this))
        val context = this
        appScope.launch {
            messagingDbHelper.getAllUnhandledMessage().collect() {
                for (message in it) {
                    CcuLog.i(L.TAG_CCU_MESSAGING, " handleMessage $message");
                    handleMessage(messageToJson(message), context)
                }
            }
            delay(100)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun messageToJson(message : Message) : JsonObject {
        val gson: Gson = GsonBuilder().create()
        val messageType = object : TypeToken<Message>() {}.type
        return gson.toJsonTree(message, messageType).asJsonObject
    }

    private fun handleMessage(msg: JsonObject, context: Context) {
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