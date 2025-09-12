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

class UpdateRecurringScheduleHandler : MessageHandler {

    private val ADD_POINT_SCHEDULE: String = "addPointSchedule"
    private val UPDATE_POINT_SCHEDULE: String = "updatePointSchedule"
    private val DELETE_POINT_SCHEDULE: String = "deletePointSchedule"

    override val command: List<String> = listOf(ADD_POINT_SCHEDULE, UPDATE_POINT_SCHEDULE, DELETE_POINT_SCHEDULE)

    override fun handleMessage(jsonObject: JsonObject, context: Context) {
        CcuLog.d(L.TAG_CCU_MESSAGING, "UpdateRecurringScheduleHandler Handler called")
        if (jsonObject["command"].asString == UpdateRecurringScheduleHandler().DELETE_POINT_SCHEDULE && jsonObject["id"] != null) {
            CCUHsApi.getInstance().removeEntity(jsonObject["id"].asString)
            CCUHsApi.getInstance().removeCustomScheduleView();
            return
        }
        updateRecurringScheduleEntity(jsonObject)
    }

    override fun ignoreMessage(jsonObject: JsonObject, context: Context): Boolean {
        return false
    }
}

fun updateRecurringScheduleEntity(msgObject: JsonObject) {
    val uid = msgObject["id"].asString
    val ccuHsApi = CCUHsApi.getInstance()
    val eventsEntity = ccuHsApi.read("id == " + HRef.make(uid))
    val b = HDictBuilder().add("id", HRef.copy(uid))
    val dictArr = arrayOf(b.toDict())
    val response = HttpUtil.executePost(
        ccuHsApi.hsUrl + "read",
        HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr))
    )
    CcuLog.d(L.TAG_CCU_MESSAGING, "Read recurring/point schedule : $response")
    if (response == null || response.isEmpty()) {
        CcuLog.d(L.TAG_CCU_MESSAGING, "Failed to read remote recurring/point entity : $response")
        return
    }
    val sGrid = HZincReader(response).readGrid()
    if (sGrid == null) {
        CcuLog.d(L.TAG_CCU_MESSAGING, "Failed to read remote recurring/point entity : $uid")
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
        val recurringScheduleDict = HDictBuilder().add(r).toDict()
        CcuLog.d(L.TAG_CCU_MESSAGING, "remote point/recurring schedule entity saved locally:  $recurringScheduleDict")
        ccuHsApi.updateRecurringSchedule(uid, recurringScheduleDict)
    }
}



