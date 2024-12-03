package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.util.AdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.getAdvanceAhuModels
import a75f.io.logic.bo.building.system.util.getDabCmEquip
import a75f.io.logic.bo.building.system.util.getDabConnectEquip
import a75f.io.logic.bo.building.system.util.getVavCmEquip
import a75f.io.logic.bo.building.system.util.getVavConnectEquip
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import com.google.gson.JsonObject

/**
 * Created by Manjunath K on 16-09-2024.
 */

fun reconfigureAdvanceAhuV2(msgObject: JsonObject, configPoint: Point) {

    val hayStack = CCUHsApi.getInstance()
    val (cmModel, connectModel) = getAdvanceAhuModels()
    val config = AdvancedHybridAhuConfig(cmModel, connectModel).getActiveConfiguration()
    val equipBuilder = ProfileEquipBuilder(hayStack)
    val (cmEquip, connectEquip) = getAhuEquip()
    val pointNewValue = msgObject["val"].asDouble
    updateAhuConfiguration(msgObject, configPoint, config)
    if (configPoint.markers.contains(Tags.CONNECTMODULE)) {
        equipBuilder.updateEquipAndPoints(config.connectConfiguration, connectModel, hayStack.getSite()!!.id, connectEquip["dis"].toString(), true)
    } else {
        equipBuilder.updateEquipAndPoints(config.cmConfiguration, cmModel, hayStack.getSite()!!.id, cmEquip["dis"].toString(), true)
    }
    CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for Advance AHU$config")
    DomainManager.addSystemDomainEquip(hayStack)
    ConfigPointUpdateHandler.removeWritableTagFromCMDevicePort(configPoint, hayStack, pointNewValue)
}


private fun updateAhuConfiguration(msgObject: JsonObject, receivedPoint: Point, activeConfiguration: AdvancedHybridAhuConfig) {
    val pointNewValue = msgObject["val"].asDouble

    if (receivedPoint.markers.contains(Tags.CONNECTMODULE)) {
        updateConfiguration(receivedPoint.domainName, pointNewValue, activeConfiguration.connectConfiguration)
    } else {
        updateConfiguration(receivedPoint.domainName, pointNewValue, activeConfiguration.cmConfiguration)
    }
}

private fun getAhuEquip(): Pair<HashMap<Any, Any>, HashMap<Any, Any>> {
    return if (L.ccu().systemProfile is VavAdvancedAhu) {
        Pair(getVavCmEquip(), getVavConnectEquip())
    } else {
        Pair(getDabCmEquip(), getDabConnectEquip())
    }
}

fun isAdvanceAhuV2Profile(): Boolean {
    return (L.ccu().systemProfile is VavAdvancedAhu || L.ccu().systemProfile is DabAdvancedAhu)
}
