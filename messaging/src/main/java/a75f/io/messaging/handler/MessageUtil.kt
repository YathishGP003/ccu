package a75f.io.messaging.handler

import a75f.io.api.haystack.HayStackConstants
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import com.google.gson.JsonObject


class MessageUtil {
    companion object {
        fun returnDurationDiff(msgObject : JsonObject): Double {
            val durationVal =
                if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asLong else 0
            //If duration shows it has already expired, then just write 1ms to force-expire it locally.
            CcuLog.d(L.TAG_CCU_PUBNUB,"id: ${msgObject.get(HayStackConstants.ID)} duration $durationVal")
            return (if (durationVal == 0L) 0 else if ((durationVal - System.currentTimeMillis()) > 0) (durationVal - System.currentTimeMillis()) else 1).toDouble()
        }
    }
}