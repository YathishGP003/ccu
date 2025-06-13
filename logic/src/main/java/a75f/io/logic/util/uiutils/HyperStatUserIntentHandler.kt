package a75f.io.logic.util.uiutils

import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.interfaces.ZoneDataInterface
import kotlin.collections.set


/**
 * Created by Manjunath K on 11-08-2021.
 */
class HyperStatUserIntentHandler {

    companion object {

        var zoneDataInterface: ZoneDataInterface? = null

        var hyperStatStatus: HashMap<String, String> = HashMap()

        private fun getHyperStatStatusString(equipRef: String?): String {
            return hyperStatStatus[equipRef] ?: "OFF"
        }

        fun updateHyperStatStatus(temperatureState: ZoneTempState, equip: HyperStatEquip, logTag: String) {

            val equipStatusMsg =
                getStatusMsg(equip.relayStages, equip.analogOutStages, temperatureState)

            if (!hyperStatStatus.containsKey(equip.equipRef)
                || !getHyperStatStatusString(equip.equipRef).contentEquals(equipStatusMsg)
            ) {
                if (hyperStatStatus.containsKey(equip.equipRef)) hyperStatStatus.remove(equip.equipRef)
                hyperStatStatus[equip.equipRef] = equipStatusMsg

                equip.equipStatusMessage.writeDefaultVal(equipStatusMsg)
                MyStatUserIntentHandler.zoneDataInterface?.refreshScreen("", false)
            }
            CcuLog.i(logTag, "Equip status message : $equipStatusMsg")
        }

    }
}