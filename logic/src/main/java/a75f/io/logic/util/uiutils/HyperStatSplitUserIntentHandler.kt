package a75f.io.logic.util.uiutils

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings

import a75f.io.logic.interfaces.ZoneDataInterface
import kotlin.collections.set


/**
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */
class HyperStatSplitUserIntentHandler {

    companion object {

        var zoneDataInterface: ZoneDataInterface? = null
        var hyperStatSplitStatus: HashMap<String, String> = HashMap()
        private fun getHyperStatSplitStatusString(equipRef: String?): String {
            return hyperStatSplitStatus[equipRef] ?: "OFF"
        }

        fun updateHyperStatSplitStatus(
            equipId: String,
            portStages: HashMap<String, Int>,
            analogOutStages: HashMap<String, Int>,
            temperatureState: ZoneTempState,
            economizingLoopOutput: Int,
            dcvLoopOutput: Int,
            outsideDamperMinOpen: Int,
            outsideAirFinalLoopOutput: Int,
            condensateOverflow: Double,
            filterDirty: Double,
            basicSettings: BasicSettings,
            epidemicState: EpidemicState
        ) {

            var status = getStatusMsg(portStages, analogOutStages, temperatureState, epidemicState)
            val haystack: CCUHsApi = CCUHsApi.getInstance()
            if(analogOutStages.containsKey(StatusMsgKeys.OAO_DAMPER.name)) {
                if(status.isNotBlank()
                    && (status[status.length-2]!=',')
                    && (status[status.length-2]!='|'))
                    status += ", "

                /**
                 * OAO behavior is surprisingly hard to explain in a status message.
                 * I *think* this code captures all the possible edge cases.
                 */
                if (outsideAirFinalLoopOutput > outsideDamperMinOpen && !(condensateOverflow > 0.0)) {
                    if(economizingLoopOutput > outsideDamperMinOpen && (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                                || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY)) {
                        status += "Free Cooling ON"
                        if (dcvLoopOutput > outsideDamperMinOpen) {
                            if (!status.contains("DCV ON")) {
                                status += ", DCV ON"
                            }
                        }
                    } else if (dcvLoopOutput > outsideDamperMinOpen) {
                        if (!status.contains("DCV ON")) {
                        status += "DCV ON"
                            }
                    }
                }
            }

            /**
             *  Condensate Overflow and Filter Status should both still display even if status is OFF.
             *      - Conditioning Mode is forced to OFF when a Condensate Alarm occurs.
             *      So, the cause should be displayed when it occurs.
             *      - For a Dirty Filter condition, some pressure switches require a manual reset
             *      by the operator after they are tripped. So, it is possible that a filter switch
             *      could accurately be in an alarm condition even with the unit off, and we should
             *      allow for this on the zone screen.
             */
            if (condensateOverflow > 0.0) {
                if(status.isBlank()) status = "OFF"
                if(status.isNotBlank()
                    && (status[status.length-2]!=',')
                    && (status[status.length-2]!='|'))
                    status += ", "

                status += "Condensate Overflow"
            }
            if (filterDirty > 0.0) {
                if(status.isBlank()) status = "OFF"
                if(status.isNotBlank()
                    && (status[status.length-2]!=',')
                    && (status[status.length-2]!='|'))
                    status += ", "

                status += "Replace Filter"
            }
            if (status.endsWith(",")) {
                status = status.dropLast(1)
            }

            if (!hyperStatSplitStatus.containsKey(equipId)
                || !getHyperStatSplitStatusString(equipId).contentEquals(status)) {
                if (hyperStatSplitStatus.containsKey(equipId)) hyperStatSplitStatus.remove(equipId)
                hyperStatSplitStatus[equipId] = status

                haystack.writeDefaultVal(
                    "point and domainName == \"" + DomainName.equipStatusMessage + "\" and equipRef == \"$equipId\"",
                    status
                )
                zoneDataInterface?.refreshScreen("", false)
            }

            CcuLog.i(L.TAG_CCU_ZONE, "HSS equip status $status")
        }

        fun isFanModeChangeUnnecessary(equipRef: String, userIntentFanMode: Int): Boolean {
            val haystack: CCUHsApi = CCUHsApi.getInstance()
            val currentFanMode = haystack.readPointPriorityValByQuery("point and domainName == \"" + DomainName.fanOpMode + "\" and equipRef == \"$equipRef\"").toInt()
            // Ignore a change to OFF if current Fan Mode is OFF
            val isAlreadyFanOff = userIntentFanMode == StandaloneFanStage.OFF.ordinal
                    && currentFanMode == StandaloneFanStage.OFF.ordinal

            // Ignore a change to AUTO if current Fan Mode is AUTO
            val isAlreadyFanAuto = userIntentFanMode == StandaloneFanStage.AUTO.ordinal
                    && currentFanMode == StandaloneFanStage.AUTO.ordinal

            // Ignore a change to LOW_CUR_OCC if current Fan Mode is LOW_CUR_OCC, LOW_OCC, or LOW_ALL_TIMES
            val isAlreadyFanLow = userIntentFanMode == StandaloneFanStage.LOW_CUR_OCC.ordinal &&
                    (currentFanMode == StandaloneFanStage.LOW_CUR_OCC.ordinal ||
                            currentFanMode == StandaloneFanStage.LOW_OCC.ordinal ||
                            currentFanMode == StandaloneFanStage.LOW_ALL_TIME.ordinal)

            // Ignore a change to MEDIUM_CUR_OCC if current Fan Mode is MEDIUM_CUR_OCC, MEDIUM_OCC, or MEDIUM_ALL_TIMES
            val isAlreadyFanMedium = userIntentFanMode == StandaloneFanStage.MEDIUM_CUR_OCC.ordinal &&
                    (currentFanMode == StandaloneFanStage.MEDIUM_CUR_OCC.ordinal ||
                            currentFanMode == StandaloneFanStage.MEDIUM_OCC.ordinal ||
                            currentFanMode == StandaloneFanStage.MEDIUM_ALL_TIME.ordinal)

            // Ignore a change to HIGH_CUR_OCC if current Fan Mode is HIGH_CUR_OCC, HIGH_OCC, or HIGH_ALL_TIMES
            val isAlreadyFanHigh = userIntentFanMode == StandaloneFanStage.HIGH_CUR_OCC.ordinal &&
                    (currentFanMode == StandaloneFanStage.HIGH_CUR_OCC.ordinal ||
                            currentFanMode == StandaloneFanStage.HIGH_OCC.ordinal ||
                            currentFanMode == StandaloneFanStage.HIGH_ALL_TIME.ordinal)

            return (isAlreadyFanOff || isAlreadyFanAuto || isAlreadyFanLow || isAlreadyFanMedium || isAlreadyFanHigh)
        }
    }
}