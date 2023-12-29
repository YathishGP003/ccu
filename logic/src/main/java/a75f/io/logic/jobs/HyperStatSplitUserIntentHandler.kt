package a75f.io.logic.jobs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logic.L
import a75f.io.logic.interfaces.ZoneDataInterface
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.Pipe2RelayAssociation
import a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.util.RxjavaUtil
import android.util.Log
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import kotlin.collections.set


/**
 * Created for HyperStat by Manjunath K on 11-08-2021.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */
class HyperStatSplitUserIntentHandler {

    companion object {

        var zoneDataInterface: ZoneDataInterface? = null
        var hyperStatSplitStatus: HashMap<String, String> = HashMap()

        private fun getHyperStatSplitStatusString(equipRef: String?): String? {
            return if (hyperStatSplitStatus.size > 0 && hyperStatSplitStatus.containsKey(equipRef))
                hyperStatSplitStatus[equipRef] else "OFF"
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
            basicSettings: BasicSettings
        ) {
            var status: String
            status = when (temperatureState) {
                ZoneTempState.RF_DEAD -> "RF Signal dead "
                ZoneTempState.TEMP_DEAD -> "Zone Temp Dead "
                ZoneTempState.EMERGENCY -> "Emergency "
                ZoneTempState.NONE -> ""
                ZoneTempState.FAN_OP_MODE_OFF -> "OFF "
            }

            if (portStages.containsKey(Stage.COOLING_1.displayName)
                && portStages.containsKey(Stage.COOLING_2.displayName)
                && portStages.containsKey(Stage.COOLING_3.displayName)
            ) {
                status += "Cooling 1,2&3 ON "
            } else if (portStages.containsKey(Stage.COOLING_1.displayName)
                && portStages.containsKey(Stage.COOLING_2.displayName)
            ) {
                status += "Cooling 1&2 ON "
            } else if (portStages.containsKey(Stage.COOLING_1.displayName)
                && portStages.containsKey(Stage.COOLING_3.displayName)
            ) {
                status += "Cooling 1&3 ON "
            } else if (portStages.containsKey(Stage.COOLING_2.displayName)
                && portStages.containsKey(Stage.COOLING_3.displayName)
            ) {
                status += "Cooling 2&3 ON "
            } else if (portStages.containsKey(Stage.COOLING_1.displayName)) {
                status += "Cooling 1 ON "
            } else if (portStages.containsKey(Stage.COOLING_2.displayName)) {
                status += "Cooling 2 ON "
            } else if (portStages.containsKey(Stage.COOLING_3.displayName)) {
                status += "Cooling 3 ON "
            }
            if (portStages.containsKey(Stage.HEATING_1.displayName)
                && portStages.containsKey(Stage.HEATING_2.displayName)
                && portStages.containsKey(Stage.HEATING_3.displayName)
            ) {
                status += "Heating 1,2&3 ON "
            } else if (portStages.containsKey(Stage.HEATING_1.displayName)
                && portStages.containsKey(Stage.HEATING_2.displayName)
            ) {
                status += "Heating 1&2 ON "
            } else if (portStages.containsKey(Stage.HEATING_1.displayName)
                && portStages.containsKey(Stage.HEATING_3.displayName)
            ) {
                status += "Heating 1&3 ON "
            } else if (portStages.containsKey(Stage.HEATING_2.displayName)
                && portStages.containsKey(Stage.HEATING_3.displayName)
            ) {
                status += "Heating 2&3 ON "
            } else if (portStages.containsKey(Stage.HEATING_1.displayName)) {
                status += "Heating 1 ON "
            } else if (portStages.containsKey(Stage.HEATING_2.displayName)) {
                status += "Heating 2 ON "
            } else if (portStages.containsKey(Stage.HEATING_3.displayName)) {
                status += "Heating 3 ON "
            }

            if (portStages.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.name)
                && portStages.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.name)
            ) {
                status += "Aux Heating 1&2 ON"
            } else if (portStages.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.name)) {
                status += "Aux Heating1 ON"
            } else if (portStages.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.name)) {
                status += "Aux Heating2 ON"
            }
            if (portStages.containsKey(Pipe2RelayAssociation.WATER_VALVE.name)){
                status += if (status.isNotBlank())
                    ", Water Valve ON"
                else
                    "Water Valve ON"
            }

            if (temperatureState != ZoneTempState.FAN_OP_MODE_OFF && temperatureState != ZoneTempState.TEMP_DEAD) {
                if (status == "OFF " && portStages.size > 0) status = ""

                if ((portStages.containsKey(Stage.FAN_1.displayName)
                    || portStages.containsKey(Stage.FAN_2.displayName)
                    || portStages.containsKey(Stage.FAN_3.displayName)) && status.isNotBlank()){
                    status +=", "
                }
                if (portStages.containsKey(Stage.FAN_1.displayName)
                    && portStages.containsKey(Stage.FAN_2.displayName)
                    && portStages.containsKey(Stage.FAN_3.displayName)
                ) {
                    status += "Fan 1,2&3 ON"

                } else if (portStages.containsKey(Stage.FAN_1.displayName)
                    && portStages.containsKey(Stage.FAN_2.displayName)
                ) {
                    status += "Fan 1&2 ON"
                } else if (portStages.containsKey(Stage.FAN_1.displayName)
                    && portStages.containsKey(Stage.FAN_3.displayName)
                ) {
                    status += "Fan 1&3 ON"
                } else if (portStages.containsKey(Stage.FAN_2.displayName)
                    && portStages.containsKey(Stage.FAN_3.displayName)
                ) {
                    status += "Fan 2&3 ON"
                } else if (portStages.containsKey(Stage.FAN_1.displayName)) {
                    status += "Fan 1 ON"
                } else if (portStages.containsKey(Stage.FAN_2.displayName)) {
                    status += "Fan 2 ON"
                } else if (portStages.containsKey(Stage.FAN_3.displayName)) {
                    status += "Fan 3 ON"
                }

            }

            if(analogOutStages.isNotEmpty()){
                if (status == "OFF " || status.isBlank()) status = "" else status += ", "

                if(analogOutStages.containsKey(AnalogOutput.COOLING.name))
                    status += "Cooling Analog ON"

                if(analogOutStages.containsKey(AnalogOutput.HEATING.name)) {
                    if(status.isNotBlank()
                        && (status[status.length-2]!=',')
                        && (status[status.length-2]!='|'))
                        status += " | "
                    status += "Heating Analog ON"
                }

                if(analogOutStages.containsKey(AnalogOutput.FAN_SPEED.name)) {
                    if(status.isNotBlank()
                        && (status[status.length-2]!=',')
                        && (status[status.length-2]!='|'))
                        status += " | "
                    status += "Fan Analog ON"
                }

                if(analogOutStages.containsKey(AnalogOutput.OAO_DAMPER.name)) {
                    if(status.isNotBlank()
                        && (status[status.length-2]!=',')
                        && (status[status.length-2]!='|'))
                        status += ", "

                    /**
                     * OAO behavior is surprisingly hard to explain in a status message.
                     * I *think* this code captures all the possible edge cases.
                      */
                    if (outsideAirFinalLoopOutput > outsideDamperMinOpen && !(condensateOverflow > 0.0)) {
                        if(economizingLoopOutput > outsideDamperMinOpen && (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY)) {
                            status += "Free Cooling ON"
                            if (dcvLoopOutput > outsideDamperMinOpen) {
                                status += ", DCV ON"
                            }
                        } else if (dcvLoopOutput > outsideDamperMinOpen) {
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

            if (getHyperStatSplitStatusString(equipId) != status) {
                if (hyperStatSplitStatus.containsKey(equipId)) hyperStatSplitStatus.remove(
                    equipId
                )
                hyperStatSplitStatus[equipId] = status
                if (status.isEmpty()) status = " OFF"

                CCUHsApi.getInstance().writeDefaultVal(
                    "point and status and message and writable and equipRef == \"$equipId\"", status
                )
                zoneDataInterface?.refreshScreen("", false)
            }
        }

        fun updateHyperStatSplitUIPoints(equipRef: String, command: String, value: Double, who: String) {

            val haystack: CCUHsApi = CCUHsApi.getInstance()
            RxjavaUtil.executeBackgroundTask(
                { },
                {
                    val currentData = haystack.readEntity(
                        "point and $command and equipRef == \"$equipRef\""
                    )
                    if (currentData?.get("id") != null) {

                        val id: String = currentData["id"].toString()
                        val pointDetails = Point.Builder().setHashMap(haystack.readMapById(id)).build()

                        if(pointDetails.markers.contains("writable")){
                            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, " updated point write $id")
                            haystack.pointWrite(
                                HRef.copy(id),
                                TunerConstants.UI_DEFAULT_VAL_LEVEL,
                                who,
                                HNum.make(value),
                                HNum.make(0)
                            )
                        }
                        if(pointDetails.markers.contains("his")){
                            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, " updated his write $id")
                            haystack.writeHisValById(id, value)
                        }
                    }
                    Log.i(L.TAG_CCU_HSSPLIT_CPUECON, " update HyperStat Split UI Points work done")

                },
                {

                }
            )

        }

    }
}