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

        fun updateHyperStatStatus(
            equipId: String,
            portStages: HashMap<String, Int>,
            analogOutStages: HashMap<String, Int>,
            temperatureState: ZoneTempState,
            equip: HyperStatEquip
        ) {
            val equipStatusMsg = getStatusMsg(portStages, analogOutStages, temperatureState)

            if (!hyperStatStatus.containsKey(equipId) || !getHyperStatStatusString(equipId).contentEquals(
                    equipStatusMsg
                )
            ) {
                if (hyperStatStatus.containsKey(equipId)) hyperStatStatus.remove(equipId)
                hyperStatStatus[equipId] = equipStatusMsg

                equip.equipStatusMessage.writeDefaultVal(equipStatusMsg)
                MyStatUserIntentHandler.zoneDataInterface?.refreshScreen("", false)
            }
            CcuLog.i(L.TAG_CCU_HSHST, "Equip status message : $equipStatusMsg")

        }

    }
}