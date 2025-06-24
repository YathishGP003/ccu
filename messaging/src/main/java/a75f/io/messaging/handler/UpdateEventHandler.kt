package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.MessageHandler
import android.content.Context
import com.google.gson.JsonObject
import org.projecthaystack.HDateTime
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRef
import org.projecthaystack.HRow
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter

class UpdateEventHandler : MessageHandler {

    private val UPDATE_EVENT: String = "updateEvent"
    private val ADD_EVENT: String = "addEvent"
    private val DELETE_EVENT: String = "deleteEvent"

    override val command: List<String> = listOf(UPDATE_EVENT, ADD_EVENT, DELETE_EVENT)

    override fun handleMessage(jsonObject: JsonObject, context: Context) {
        CcuLog.d(L.TAG_CCU_MESSAGING, "UpdateEventHandler Handler called")
        if (jsonObject["command"].asString == UpdateEventHandler().DELETE_EVENT && jsonObject["id"] != null) {
            CCUHsApi.getInstance().removeEntity(jsonObject["id"].asString)
            return
        }
        updateEventEntity(jsonObject)
    }

    override fun ignoreMessage(jsonObject: JsonObject, context: Context): Boolean {
        return false
    }
}

fun updateEventEntity(msgObject: JsonObject) {
    val uid = msgObject["id"].asString
    val ccuHsApi = CCUHsApi.getInstance()
    val eventsEntity = ccuHsApi.read("id == " + HRef.make(uid))
    val b = HDictBuilder().add("id", HRef.copy(uid))
    val dictArr = arrayOf(b.toDict())
    val response = HttpUtil.executePost(
        ccuHsApi.hsUrl + "read",
        HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr))
    )
    CcuLog.d(L.TAG_CCU_MESSAGING, "Read event : $response")
    if (response == null || response.isEmpty()) {
        CcuLog.d(L.TAG_CCU_MESSAGING, "Failed to read remote event : $response")
        return
    }
    val sGrid = HZincReader(response).readGrid()
    if (sGrid == null) {
        CcuLog.d(L.TAG_CCU_MESSAGING, "Failed to read remote event : $uid")
        return
    }
    val it = sGrid.iterator()
    while (it.hasNext()) {
        val r = it.next() as HRow
        var lastModifiedDateTime: HDateTime?
        val lastModifiedTimeTag: Any? = r["lastModifiedDateTime", false]
        lastModifiedDateTime = if (lastModifiedTimeTag != null) {
            lastModifiedTimeTag as HDateTime
        } else {
            null
        }
        if (!DataSyncHandler.isCloudScheduleHasLatestValue(eventsEntity, lastModifiedDateTime)) {
            CcuLog.i(L.TAG_CCU_MESSAGING, "CCU HAS LATEST VALUE ")
            return
        }
        val eventDict = HDictBuilder().add(r).toDict()
        CcuLog.d(L.TAG_CCU_MESSAGING, "remote event saved locally: eventDict = $eventDict")
        ccuHsApi.updateEventSchedule(uid, eventDict)
    }
}



