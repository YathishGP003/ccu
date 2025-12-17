package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.HayStackConstants.WRITABLE_ARRAY_VAL
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.observer.HisWriteObservable.notifyChange
import a75f.io.api.haystack.observer.PointWriteObservable.notifyWritableChange
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import com.google.gson.JsonObject
import org.projecthaystack.HNum


class MessageUtil {
    companion object {
        fun returnDurationDiff(msgObject : JsonObject): Double {
            val durationVal =
                if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asLong else 0
            //If duration shows it has already expired, then just write 1ms to force-expire it locally.
            CcuLog.d(L.TAG_CCU_PUBNUB,"id: ${msgObject.get(HayStackConstants.ID)} duration $durationVal")
            return (if (durationVal == 0L) 0 else if ((durationVal - System.currentTimeMillis()) > 0) (durationVal - System.currentTimeMillis()) else 1).toDouble()
        }

        fun updateLocalPointWriteChanges(hayStack: CCUHsApi, pointUid: String, msgObject: JsonObject, localPoint: Point) {
            if (hayStack.isEntityExisting(pointUid)) {
                writePointFromJson(pointUid, msgObject, hayStack)

                //TODO- Should be removed one pubnub is stable
                logPointArray(localPoint)

                try {
                    Thread.sleep(10)
                    UpdatePointHandler.updatePoints(localPoint)
                } catch (e: InterruptedException) {
                    CcuLog.e(L.TAG_CCU_MESSAGING, "Error in thread sleep", e)
                }
            } else {
                CcuLog.d(L.TAG_CCU_PUBNUB, "Received for invalid local point : " + pointUid)
            }
        }

        fun writePointFromJson(id: String, msgObject: JsonObject, hayStack: CCUHsApi) {
            val value = msgObject.get(WRITABLE_ARRAY_VAL).asString
            if (value.isEmpty()) {
                //When a level is deleted, it currently generates a message with empty value.
                //Handle it here.
                val level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt()
                clearPointArrayLevel(id, level, hayStack)
                return
            }
            val who = msgObject.get("who").asString
            val `val` = msgObject.get("val").asDouble
            val level = msgObject.get("level").asInt
            val durationDiff = returnDurationDiff(msgObject)
            hayStack.writePointLocal(id, level, who, `val`, durationDiff)
            CcuLog.d(
                L.TAG_CCU_PUBNUB,
                "updatePoint : writePointFromJson - level: $level who: $who val: $`val` durationDiff: $durationDiff"
            )
            notifyWritableChange(id, HNum.make(`val`))
        }

        fun clearPointArrayLevel(id: String, level: Int, hayStack: CCUHsApi) {
            //When a level is deleted, it currently generates a message with empty value.
            hayStack.clearPointArrayLevel(id, level, true)
            hayStack.writeHisValById(id, HSUtil.getPriorityVal(id))
            notifyWritableChange(id, HNum.make(HSUtil.getPriorityVal(id)))
            notifyChange(id, HSUtil.getPriorityVal(id))
            CcuLog.d(L.TAG_CCU_PUBNUB, "clearPointArrayLevel - id: $id level: $level")
        }

        private fun logPointArray(localPoint: Point) {
            val values: ArrayList<*>? = CCUHsApi.getInstance().readPoint(localPoint.getId())
            if (values != null && !values.isEmpty()) {
                for (l in 1..values.size) {
                    val valMap = (values.get(l - 1) as HashMap<*, *>)
                    if (valMap.get("val") != null) {
                        val duration = valMap.get("duration").toString().toDouble()
                        CcuLog.d(
                            L.TAG_CCU_PUBNUB,
                            ("Updated point " + localPoint.getDisplayName() + " , level: " + l + " , val :" + valMap.get(
                                "val"
                            ).toString()
                                .toDouble() + " duration " + (if (duration > 0) duration - System.currentTimeMillis() else duration))
                        )
                    }
                }
            }
        }
    }
}