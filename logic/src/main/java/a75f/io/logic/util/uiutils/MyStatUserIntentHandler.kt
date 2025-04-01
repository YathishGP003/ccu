package a75f.io.logic.util.uiutils

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.api.Point
import a75f.io.domain.api.readPoint
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.interfaces.ZoneDataInterface
import a75f.io.util.ExecutorTask
import kotlin.collections.set


/**
 * Created by Manjunath K on 11-08-2021.
 */
class MyStatUserIntentHandler {

    companion object {

        var zoneDataInterface: ZoneDataInterface? = null
        var myStatStatus: HashMap<String, String> = HashMap()

        private fun getMyStatStatusString(equipRef: String?): String? {
            return if (myStatStatus.size > 0 && myStatStatus.containsKey(equipRef)) myStatStatus[equipRef] else "OFF"
        }

        fun updateMyStatStatus(
            equipId: String,
            portStages: HashMap<String, Int>,
            analogOutStages: HashMap<String, Int>,
            temperatureState: ZoneTempState,
            equip: MyStatEquip
        ) {
            var status = StringBuilder()
            when (temperatureState) {
                ZoneTempState.RF_DEAD -> status.append("RF Signal dead ")
                ZoneTempState.TEMP_DEAD -> status.append("Zone Temp Dead ")
                ZoneTempState.EMERGENCY -> status.append("Emergency ")
                ZoneTempState.NONE -> status.append("")
                ZoneTempState.FAN_OP_MODE_OFF -> status.append("OFF ")
            }

            if (portStages.containsKey(Stage.COOLING_1.displayName) && portStages.containsKey(Stage.COOLING_2.displayName)) {
                status.append("Cooling 1&2 ON")
            } else if (portStages.containsKey(Stage.COOLING_1.displayName)) {
                status.append("Cooling 1 ON")
            } else if (portStages.containsKey(Stage.COOLING_2.displayName)) {
                status.append("Cooling 2 ON")
            }

            if (portStages.containsKey(Stage.HEATING_1.displayName) && portStages.containsKey(Stage.HEATING_2.displayName)) {
                status.append("Heating 1&2 ON")
            } else if (portStages.containsKey(Stage.HEATING_1.displayName)) {
                status.append("Heating 1 ON")
            } else if (portStages.containsKey(Stage.HEATING_2.displayName)) {
                status.append("Heating 2 ON")
            }

            if ((portStages.containsKey(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.name)) && status.isNotBlank()) {
                status.append(", ")
            }

            if (portStages.containsKey(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.name)) {
                status.append("Aux Heating ON")
            }

            if (portStages.containsKey(MyStatPipe2RelayMapping.WATER_VALVE.name)) {
                if (status.isNotBlank()) status.append(", Water Valve ON")
                else status.append("Water Valve ON")
            }

            if (temperatureState != ZoneTempState.FAN_OP_MODE_OFF && temperatureState != ZoneTempState.TEMP_DEAD) {
                if (status.contentEquals("OFF") && portStages.size > 0) status = StringBuilder()

                if ((portStages.containsKey(Stage.FAN_1.displayName) || portStages.containsKey(Stage.FAN_2.displayName)) && status.isNotBlank()
                ) {
                    status.append(", ")
                }
                if (portStages.containsKey(Stage.FAN_1.displayName) && portStages.containsKey(Stage.FAN_2.displayName)) {
                    status.append("Fan 1&2 ON")
                } else if (portStages.containsKey(Stage.FAN_1.displayName)) {
                    status.append("Fan 1 ON")
                } else if (portStages.containsKey(Stage.FAN_2.displayName)) {
                    status.append("Fan 2 ON")
                }

            }

            if (analogOutStages.isNotEmpty()) {
                if (status.contentEquals("OFF") || status.isBlank()) status =
                    StringBuilder() else status.append(", ")

                if (analogOutStages.containsKey(AnalogOutput.COOLING.name)) status.append("Cooling Analog ON")

                if (analogOutStages.containsKey(AnalogOutput.HEATING.name)) {
                    if (status.isNotBlank() && (status[status.length - 2] != ',') && (status[status.length - 2] != '|')) status.append(
                        " | "
                    )
                    status.append("Heating Analog ON")
                }

                if (analogOutStages.containsKey(AnalogOutput.FAN_SPEED.name)) {
                    if (status.isNotBlank() && (status[status.length - 2] != ',') && (status[status.length - 2] != '|')) status.append(
                        " | "
                    )
                    status.append("Fan Analog ON")
                }

                if (analogOutStages.containsKey(AnalogOutput.DCV_DAMPER.name)) {
                    if (status.isNotBlank() && (status[status.length - 2] != ',') && (status[status.length - 2] != '|')) status.append(
                        " | "
                    )
                    status.append("DCV ON")
                }
            }

            val trimmed = status.toString().trim()
            status.setLength(0)
            status.append(trimmed)

            if (status.isNotEmpty() && status.last() == ',') {
                status.setLength(status.length - 1)
            }

            if (status.trim().endsWith(",")) {
                status.setLength(status.length - 1)
            }

            if (!getMyStatStatusString(equipId).contentEquals(status.toString())) {
                if (myStatStatus.containsKey(equipId)) myStatStatus.remove(equipId)
                myStatStatus[equipId] = status.toString()
                if (status.isEmpty()) status = StringBuilder(" OFF ")

                equip.equipStatusMessage.writeDefaultVal(status.toString())
                zoneDataInterface?.refreshScreen("", false)
            }
            CcuLog.i(L.TAG_CCU_MSHST, "Equip status message : $status")

        }

        fun updateMyStatUserIntentPoints(
            equipRef: String,
            point: Point,
            value: Double,
            who: String
        ) {
            ExecutorTask.executeAsync({ }, {
                val pointData = point.domainName.readPoint(equipRef)
                if (pointData.containsKey("writable")) {
                    point.pointWriteByUser(value, who)
                }
                CcuLog.i(L.TAG_CCU_MSHST, "updated ${point.domainName} value : $value")
                val roomRef = HSUtil.getZoneIdFromEquipId(equipRef)
                DesiredTempDisplayMode.setModeTypeOnUserIntentChange(
                    roomRef,
                    CCUHsApi.getInstance()
                )
            })
        }
    }
}