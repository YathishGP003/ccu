package a75f.io.messaging.handler

import a75f.io.logic.L
import a75f.io.logic.migration.schedulerevamp.handleMessage
import a75f.io.messaging.MessageHandler
import android.content.Context
import android.util.Log
import com.google.gson.JsonObject


class SchedulerRevampMigrationHandler : MessageHandler{

    override val command: List<String>
        get() = listOf(CMD)

    override fun handleMessage(jsonObject: JsonObject, context: Context) {
        Log.d(L.TAG_SCHEDULABLE,"call handle SCHEDULE_MIGRATED")
        handleMessage()
    }

    companion object {
        const val CMD: String =  "SCHEDULE_MIGRATED"
    }


}