package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.util.doPointWriteForSchedulable
import a75f.io.api.haystack.util.hayStack
import a75f.io.api.haystack.util.importSchedules
import a75f.io.api.haystack.util.setDiagMigrationVal
import a75f.io.logic.L
import a75f.io.logic.util.RxjavaUtil
import a75f.io.messaging.MessageHandler
import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import a75f.io.logic.migration.schedulerevamp.handleMessage


class SchedulerRevampMigrationHandler : MessageHandler{

    override val command: List<String>
        get() = listOf(CMD)

    override fun handleMessage(jsonObject: JsonObject, context: Context) {
        Log.d(L.TAG_SCHEDULABLE,"call handle SCHEDULE_MIGRATED")
        handleMessage()
    }

    companion object {
        val CMD: String =  "SCHEDULE_MIGRATED"
    }


}