package a75f.io.logic.jobs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Point
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.interfaces.ZoneDataInterface
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.Pipe2RelayAssociation
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.util.RxjavaUtil
import a75f.io.util.ExecutorTask
import android.util.Log
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import kotlin.collections.set
import kotlin.math.log


/**
 * Created by Manjunath K on 11-08-2021.
 */
class HyperStatUserIntentHandler {

    companion object {

        var zoneDataInterface: ZoneDataInterface? = null
        var hyperStatStatus: HashMap<String, String> = HashMap()

        private fun getHyperStatStatusString(equipRef: String?): String? {
            return if (hyperStatStatus.size > 0 && hyperStatStatus.containsKey(equipRef))
                hyperStatStatus[equipRef] else "OFF"
        }

        fun updateHyperStatStatus(
                equipId: String,
                portStages: HashMap<String, Int>,
                analogOutStages: HashMap<String, Int>,
                temperatureState: ZoneTempState,
                equip: HyperStatEquip? = null
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
                status += "Cooling 1,2&3 ON"
            } else if (portStages.containsKey(Stage.COOLING_1.displayName)
                    && portStages.containsKey(Stage.COOLING_2.displayName)
            ) {
                status += "Cooling 1&2 ON"
            } else if (portStages.containsKey(Stage.COOLING_1.displayName)
                    && portStages.containsKey(Stage.COOLING_3.displayName)
            ) {
                status += "Cooling 1&3 ON"
            } else if (portStages.containsKey(Stage.COOLING_2.displayName)
                    && portStages.containsKey(Stage.COOLING_3.displayName)
            ) {
                status += "Cooling 2&3 ON"
            } else if (portStages.containsKey(Stage.COOLING_1.displayName)) {
                status += "Cooling 1 ON"
            } else if (portStages.containsKey(Stage.COOLING_2.displayName)) {
                status += "Cooling 2 ON"
            } else if (portStages.containsKey(Stage.COOLING_3.displayName)) {
                status += "Cooling 3 ON"
            }
            if (portStages.containsKey(Stage.HEATING_1.displayName)
                    && portStages.containsKey(Stage.HEATING_2.displayName)
                    && portStages.containsKey(Stage.HEATING_3.displayName)
            ) {
                status += "Heating 1,2&3 ON"
            } else if (portStages.containsKey(Stage.HEATING_1.displayName)
                    && portStages.containsKey(Stage.HEATING_2.displayName)
            ) {
                status += "Heating 1&2 ON"
            } else if (portStages.containsKey(Stage.HEATING_1.displayName)
                    && portStages.containsKey(Stage.HEATING_3.displayName)
            ) {
                status += "Heating 1&3 ON"
            } else if (portStages.containsKey(Stage.HEATING_2.displayName)
                    && portStages.containsKey(Stage.HEATING_3.displayName)
            ) {
                status += "Heating 2&3 ON"
            } else if (portStages.containsKey(Stage.HEATING_1.displayName)) {
                status += "Heating 1 ON"
            } else if (portStages.containsKey(Stage.HEATING_2.displayName)) {
                status += "Heating 2 ON"
            } else if (portStages.containsKey(Stage.HEATING_3.displayName)) {
                status += "Heating 3 ON"
            }

            if ((portStages.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.name)
                            || portStages.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.name))
                    && status.isNotBlank()) {
                status += ", "
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
            if (portStages.containsKey(Pipe2RelayAssociation.WATER_VALVE.name)) {
                status += if (status.isNotBlank())
                    ", Water Valve ON"
                else
                    "Water Valve ON"
            }

            if (temperatureState != ZoneTempState.FAN_OP_MODE_OFF && temperatureState != ZoneTempState.TEMP_DEAD) {
                if (status == "OFF " && portStages.size > 0) status = ""

                if ((portStages.containsKey(Stage.FAN_1.displayName)
                                || portStages.containsKey(Stage.FAN_2.displayName)
                                || portStages.containsKey(Stage.FAN_3.displayName)) && status.isNotBlank()) {
                    status += ", "
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

            if (analogOutStages.isNotEmpty()) {
                if (status == "OFF " || status.isBlank()) status = "" else status += ", "

                if (analogOutStages.containsKey(AnalogOutput.COOLING.name))
                    status += "Cooling Analog ON"

                if (analogOutStages.containsKey(AnalogOutput.HEATING.name)) {
                    if (status.isNotBlank()
                            && (status[status.length - 2] != ',')
                            && (status[status.length - 2] != '|'))
                        status += " | "
                    status += "Heating Analog ON"
                }

                if (analogOutStages.containsKey(AnalogOutput.FAN_SPEED.name)) {
                    if (status.isNotBlank()
                            && (status[status.length - 2] != ',')
                            && (status[status.length - 2] != '|'))
                        status += " | "
                    status += "Fan Analog ON"
                }

                if (analogOutStages.containsKey(AnalogOutput.DCV_DAMPER.name)) {
                    if (status.isNotBlank()
                            && (status[status.length - 2] != ',')
                            && (status[status.length - 2] != '|'))
                        status += " | "
                    status += "DCV Analog ON"
                }
            }

            if (getHyperStatStatusString(equipId) != status) {
                if (hyperStatStatus.containsKey(equipId)) hyperStatStatus.remove(
                        equipId
                )
                hyperStatStatus[equipId] = status
                if (status.isEmpty()) status = " OFF "

                if (equip != null) {
                    equip.equipStatusMessage.writeDefaultVal(status)
                } else {
                    CCUHsApi.getInstance().writeDefaultVal(
                            "point and status and message and writable and equipRef == \"$equipId\"", status
                    )
                }
                zoneDataInterface?.refreshScreen("", false)
            }
            CcuLog.i(L.TAG_CCU_HSHST, "Equip status message : $status")
        }

        fun updateHyperStatUIPoints(equipRef: String, command: String, value: Double, who: String) {

            val haystack: CCUHsApi = CCUHsApi.getInstance()
            ExecutorTask.executeAsync(
                    { },
                    {
                        val currentData = haystack.readEntity(
                                "point and $command and equipRef == \"$equipRef\""
                        )
                        if (currentData?.get("id") != null) {

                            val id: String = currentData["id"].toString()
                            val pointDetails = Point.Builder().setHashMap(haystack.readMapById(id)).build()

                            if (pointDetails.markers.contains("writable")) {
                                CcuLog.d(L.TAG_CCU_HSHST, " updated point write $id")
                                haystack.pointWrite(
                                        HRef.copy(id),
                                        TunerConstants.UI_DEFAULT_VAL_LEVEL,
                                        who,
                                        HNum.make(value),
                                        HNum.make(0)
                                )
                            }
                            if (pointDetails.markers.contains("his")) {
                                CcuLog.d(L.TAG_CCU_HSHST, " updated his write $id")
                                haystack.writeHisValById(id, value)
                            }
                        }
                        CcuLog.i(L.TAG_CCU_HSHST, " update Hyperstat UI Points work done")
                        val roomRef = HSUtil.getZoneIdFromEquipId(equipRef)
                        DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, CCUHsApi.getInstance())
                    },
            )

        }

    }
}