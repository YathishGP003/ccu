package a75f.io.logic.util.uiutils

import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.interfaces.ZoneDataInterface
import kotlin.collections.set


/**
 * Created by Manjunath K on 11-08-2021.
 */
class MyStatUserIntentHandler {

    companion object {

        var zoneDataInterface: ZoneDataInterface? = null

        val myStatStatus = mutableMapOf<String, String>()

        private fun getMyStatStatusString(equipRef: String?): String {
            return myStatStatus[equipRef] ?: "OFF"
        }

        fun updateMyStatStatus(
            equipId: String,
            portStages: HashMap<String, Int>,
            analogOutStages: HashMap<String, Int>,
            temperatureState: ZoneTempState,
            equip: MyStatEquip
        ) {
            var equipStatusMsg = getStatusMsg(portStages, analogOutStages, temperatureState)
            equipStatusMsg = equipStatusMsg.replace("Aux Heating 1 ON", "Aux Heating ON")

            if (!myStatStatus.containsKey(equipId) || !getMyStatStatusString(equipId).contentEquals(
                    equipStatusMsg
                )
            ) {
                if (myStatStatus.containsKey(equipId)) myStatStatus.remove(equipId)
                myStatStatus[equipId] = equipStatusMsg

                equip.equipStatusMessage.writeDefaultVal(equipStatusMsg)
                zoneDataInterface?.refreshScreen("", false)
            }
            CcuLog.i(L.TAG_CCU_MSHST, "Equip status message : $equipStatusMsg")

        }



    }
}

