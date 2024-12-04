package a75f.io.messaging.handler

import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import android.util.Log

/**
 * Created by Manjunath K on 13-11-2024.
 */


fun updateConfiguration(domainName: String, pointValue: Double, config: ProfileConfiguration) {

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