package a75f.io.messaging.handler

import a75f.io.api.haystack.*
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstat.common.HyperStatReconfigureUtil
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import android.util.Log
import com.google.gson.JsonObject

/**
 * Created by Manjunath K on 14-10-2021.
 */
class HyperstatReconfigurationHandler {

    companion object {

        fun handleHyperStatConfigChange(msgObject: JsonObject, configPoint: Point, hayStack: CCUHsApi) {
            try {

                CcuLog.i(
                    L.TAG_CCU_PUBNUB, "\n **** Reconfiguration ****${L.TAG_CCU_HSCPU} " + configPoint + " " + msgObject.toString()
                            + " Markers =" + configPoint.markers +"\n "
                )
                val pointVal = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString
                if (pointVal.isEmpty()) {
                    val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
                    //When a level is deleted, it currently generates a message with empty value.
                    //Handle it here.
                    hayStack.clearPointArrayLevel(configPoint.id, level, true)
                    hayStack.writeHisValById(configPoint.id, HSUtil.getPriorityVal(configPoint.id))
                    return
                }

                when {
                    configPoint.markers.contains(Tags.ENABLED) -> {
                        HyperStatReconfigureUtil.updateConfigPoint(msgObject, configPoint, hayStack)
                        Log.i(L.TAG_CCU_HSCPU, "Reconfiguration for config Points")
                    }
                    configPoint.markers.contains(Tags.ASSOCIATION) -> {
                        HyperStatReconfigureUtil.updateAssociationPoint(msgObject, configPoint,hayStack)
                        Log.i(L.TAG_CCU_HSCPU, "Reconfiguration for Association Points")
                    }
                    else -> {
                        Log.i(L.TAG_CCU_HSCPU, "Reconfiguration for Points values")
                        if (!configPoint.markers.contains(Tags.VRV)) {
                            HyperStatReconfigureUtil.updateConfigValues(
                                msgObject,
                                hayStack,
                                configPoint
                            )
                        }
                    }
                }
                for (i in 1.. 6 ) {
                    if (configPoint.markers.contains("relay$i")) {
                        DesiredTempDisplayMode.setModeType(configPoint.roomRef, CCUHsApi.getInstance())
                    }
                }
                if((configPoint.markers.contains("analog1") || configPoint.markers.contains("analog2")
                            || configPoint.markers.contains("analog3")) && configPoint.markers.contains("output")) {
                    DesiredTempDisplayMode.setModeType(configPoint.roomRef, CCUHsApi.getInstance());
                }
                if (configPoint.markers.contains(Tags.USERINTENT)
                    && configPoint.markers.contains(Tags.FAN)
                    && configPoint.markers.contains(Tags.MODE)
                ) {
                    val configVal = msgObject["val"].asInt
                    val cache = FanModeCacheStorage()
                    if (configVal != 0 && configVal % 3 == 0) //Save only Fan occupied period mode alone, else no need.
                        cache.saveFanModeInCache(
                            configPoint.equipRef,
                            configVal
                        ) else cache.removeFanModeFromCache(configPoint.equipRef)
                }
            } catch (e: NullPointerException){
            e.printStackTrace()
                Log.i(L.TAG_CCU_HSCPU, "updateConfigPoint: ${e.localizedMessage}")
            }
            pointUpdateOwner(configPoint, msgObject, hayStack)
            hayStack.scheduleSync()

        }


        private fun pointUpdateOwner(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
            try {
                val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString
                val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
                val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asDouble
                val duration =
                    if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null)
                        msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
                hayStack.writePointLocal(configPoint.id, level, who, value, duration)
            } catch (e: Exception) {
                e.printStackTrace()
                CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
            }
        }
    }
}