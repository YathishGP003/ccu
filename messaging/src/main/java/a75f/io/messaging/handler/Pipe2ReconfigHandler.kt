package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import android.util.Log
import com.google.gson.JsonObject


fun reconfigureHSPipe2(msgObject: JsonObject, configPoint: Point) {

    val hayStack = CCUHsApi.getInstance()

    val hyperStatPipe2Equip = hayStack.readEntity("equip and id == "+configPoint.equipRef)
    val model = ModelLoader.getHyperStatPipe2Model()

    val config = getConfiguration(configPoint.equipRef)?.getActiveConfiguration()
    val equipBuilder = ProfileEquipBuilder(hayStack)

    val pointNewValue = msgObject["val"]
    if(pointNewValue == null || pointNewValue.asString.isEmpty()){
        CcuLog.e(L.TAG_CCU_PUBNUB, "updateConfigPoint for Pipe2 Reconfiguration $config")
    } else {
        updatePortStatus(configPoint.domainName, pointNewValue.asDouble, config!!)
    }
    writePointFromJson(configPoint, msgObject, hayStack)
    equipBuilder.updateEquipAndPoints(config!!,model , hayStack.getSite()!!.id, hyperStatPipe2Equip["dis"].toString(), true)

    CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for Pipe2 Reconfiguration $config")

}

// TODO AMAR : This method needs to be made as common code for all the profiles
private fun updatePortStatus(domainName: String, pointValue: Double, config: HyperStatConfiguration) {

    CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: received $pointValue $domainName $config")
    config.getEnableConfigs().forEach {
        if (it.domainName.equals(domainName)) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: updated config")
            setEnabledStatus(it, pointValue)
            return
        }
    }

    config.getAssociationConfigs().forEach {
        if (it.domainName.equals(domainName)) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: updated association")
            setAssociation(it, pointValue)
            return
        }
    }

    config.getValueConfigs().forEach {
        if (it.domainName.equals(domainName)) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "updatePortStatus: updated getValueConfigs")
            it.currentVal = pointValue
            return
        }
    }

}

private fun setEnabledStatus(config: EnableConfig, pointValue: Double) {
    config.enabled = pointValue == 1.0
    Log.i("CPU_Reconfiguration", "setEnabledStatus: received $pointValue ${config.domainName} ${config.enabled}")
}

private fun setAssociation(association: AssociationConfig, pointValue: Double) {
    association.associationVal = pointValue.toInt()
    Log.i("CPU_Reconfiguration", "setEnabledStatus: received $pointValue ${association.domainName} ${pointValue.toInt()}")
}

private fun writePointFromJson(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
    try {
        val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString
        val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
        val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asDouble
        val duration =
            if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null) msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
        hayStack.writePointLocal(configPoint.id, level, who, value, duration)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}