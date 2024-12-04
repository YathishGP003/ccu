package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import com.google.gson.JsonObject


fun reconfigureHSCPUV2(msgObject: JsonObject, configPoint: Point) {

    val hayStack = CCUHsApi.getInstance()
    
    val hyperStatCPUEquip = hayStack.readEntity("equip and id == "+configPoint.equipRef)
    val model = ModelLoader.getHyperStatCpuModel()

    val config = getConfiguration(configPoint.equipRef)?.getActiveConfiguration()//make it common for all
    val equipBuilder = ProfileEquipBuilder(hayStack)

    val pointNewValue = msgObject["val"]
    if(pointNewValue == null || pointNewValue.asString.isEmpty()){
        CcuLog.e(L.TAG_CCU_PUBNUB, "updateConfigPoint for CPU Reconfiguration $config")
    } else {
        updateConfiguration(configPoint.domainName, pointNewValue.asDouble, config!!)
    }
    writePointFromJson(configPoint, msgObject, hayStack)
    equipBuilder.updateEquipAndPoints(config!!,model , hayStack.getSite()!!.id, hyperStatCPUEquip["dis"].toString(), true)

    CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for CPU Reconfiguration $config")

}

private fun writePointFromJson(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
    try {
        val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString
        val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
        val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString
        if (value.isEmpty()) {
            hayStack.clearPointArrayLevel(configPoint.id, level, false)
            hayStack.writeHisValById(configPoint.id, HSUtil.getPriorityVal(configPoint.id))
            return
        }
        val duration =
                if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
        hayStack.writePointLocal(configPoint.id, level, who, value.toDouble(), duration)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}