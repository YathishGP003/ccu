package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
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
import android.util.Log
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
        updatePortStatus(receivedPoint.domainName, pointNewValue, activeConfiguration.connectConfiguration)
    } else {
        updatePortStatus(receivedPoint.domainName, pointNewValue, activeConfiguration.cmConfiguration)
    }
}

private fun updatePortStatus(domainName: String, pointValue: Double, config: ProfileConfiguration) {

    config.getEnableConfigs().forEach {
        if (it.domainName == domainName) {
            setEnabledStatus(it, pointValue)
            return
        }
    }

    config.getAssociationConfigs().forEach {
        if (it.domainName == domainName) {
            setAssociation(it, pointValue)
            return
        }
    }

    config.getValueConfigs().forEach {
        if (it.domainName == domainName) {
            it.currentVal = pointValue
            return
        }
    }

}

private fun setEnabledStatus(config: EnableConfig, pointValue: Double) {
    config.enabled = pointValue == 1.0
    Log.i("manju", "setEnabledStatus: received $pointValue ${config.domainName} ${config.enabled}")
}

private fun setAssociation(association: AssociationConfig, pointValue: Double) {
    association.associationVal = pointValue.toInt()
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
