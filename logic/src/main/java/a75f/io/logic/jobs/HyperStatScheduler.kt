package a75f.io.logic.jobs

import a75f.io.api.haystack.*
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.Occupancy
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.pubnub.ZoneDataInterface
import a75f.io.logic.tuners.StandaloneTunerUtil
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.tuners.TunerUtil
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import java.lang.IllegalArgumentException
import a75f.io.api.haystack.HSUtil
import org.joda.time.DateTime


/**
 * Created by Manjunath K on 11-08-2021.
 */
class HyperStatScheduler {

    companion object {

        private val configurationDisposable = CompositeDisposable()
        var zoneDataInterface: ZoneDataInterface? = null

        var hyperstatStatus: HashMap<String, String> = HashMap()

        private fun getHyperstatStatusString(equipRef: String?): String? {
            return if (hyperstatStatus.size > 0 && hyperstatStatus.containsKey(equipRef))
                hyperstatStatus[equipRef] else "OFF"
        }

        fun updateHyperstatStatus(
            equipId: String,
            state: ZoneState,
            portStages: HashMap<String, Int>,
            temperatureState: ZoneTempState
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

            if (temperatureState != ZoneTempState.FAN_OP_MODE_OFF && temperatureState != ZoneTempState.TEMP_DEAD) {
                if (status == "OFF " && portStages.size > 0) status = ""

                if (portStages.containsKey(Stage.FAN_1.displayName)
                    && portStages.containsKey(Stage.FAN_2.displayName)
                    && portStages.containsKey(Stage.FAN_3.displayName)
                ) {
                    status += "Fan 1,2&3 ON "
                } else if (portStages.containsKey(Stage.FAN_1.displayName)
                    && portStages.containsKey(Stage.FAN_2.displayName)
                ) {
                    status += "Fan 1&2 ON "
                } else if (portStages.containsKey(Stage.FAN_1.displayName)
                    && portStages.containsKey(Stage.FAN_3.displayName)
                ) {
                    status += "Fan 1&3 ON "
                } else if (portStages.containsKey(Stage.FAN_2.displayName)
                    && portStages.containsKey(Stage.FAN_3.displayName)
                ) {
                    status += "Fan 2&3 ON "
                } else if (portStages.containsKey(Stage.FAN_1.displayName)) {
                    status += "Fan 1 ON "
                } else if (portStages.containsKey(Stage.FAN_2.displayName)) {
                    status += "Fan 2 ON "
                } else if (portStages.containsKey(Stage.FAN_3.displayName)) {
                    status += "Fan 3 ON "
                }

            }

            if (getHyperstatStatusString(equipId) != status) {
                if (hyperstatStatus.containsKey(equipId)) hyperstatStatus.remove(
                    equipId
                )
                hyperstatStatus[equipId] = status
                if (status.isEmpty()) status = " OFF "
                updateHyperstateEquipStatus(equipId, status, state)
            }
        }

        fun processEquip(equip: Equip, equipSchedule: Schedule, vacation: Schedule?){
            val occ = equipSchedule.currentValues
            //When schedule is deleted
            if (occ == null) {
                ScheduleProcessJob.occupiedHashMap.remove(equip.roomRef)
                return
            }
            if (vacation != null) occ.isOccupied = false
            occ.vacation = vacation

            val haystack = CCUHsApi.getInstance()

            val occupancyStatus = haystack.readHisValByQuery(
                "point and hyperstat and occupancy and mode and his and equipRef == \"${equip.id}\""
            )

            val heatingDeadBand = StandaloneTunerUtil.readTunerValByQuery(
                "heating and deadband and base and not multiplier", equip.id
            )

            val coolingDeadBand = StandaloneTunerUtil.readTunerValByQuery(
                "cooling and deadband and base and not multiplier", equip.id
            )

            val setback = TunerUtil.readTunerValByQuery("unoccupied and setback", equip.id)
            val curOccupancy = Occupancy.values()[occupancyStatus.toInt()]
            //val autoawaysetback = TunerUtil.readTunerValByQuery("auto and away and setback")


            occ.unoccupiedZoneSetback = setback
            occ.heatingDeadBand = heatingDeadBand
            occ.coolingDeadBand = coolingDeadBand

            Log.i(L.TAG_CCU_HSCPU, "curOccupancy $curOccupancy ")
            if (curOccupancy == Occupancy.PRECONDITIONING)
                occ.isPreconditioning = true
            else if (curOccupancy == Occupancy.FORCEDOCCUPIED)
                occ.isForcedOccupied = true

            CcuLog.d(
                L.TAG_CCU_HSCPU,
                "Equip: " + equip.displayName +
                        "\n equip address : " + equip.group+
                        "\n isPreconditioning: " + occ.isPreconditioning +
                        "\n isForcedOccupied:" + occ.isForcedOccupied +
                        "\n isOccupied : " + occ.isOccupied

            )

            if (ScheduleProcessJob.putOccupiedModeCache(equip.roomRef, occ)) {
                val avgTemp = (occ.coolingVal + occ.heatingVal) / 2.0



                var coolingTemp = if (occ.isOccupied || occ.isPreconditioning)
                    occ.coolingVal
                else
                    occ.coolingVal + occ.unoccupiedZoneSetback

                var heatingTemp = if (occ.isOccupied || occ.isPreconditioning)
                    occ.heatingVal
                else
                    occ.heatingVal - occ.unoccupiedZoneSetback

                Log.i(L.TAG_CCU_HSCPU,"${occ.coolingVal} ${occ.heatingVal} $setback")

                if (equip.markers.contains("cpu") && occupancyStatus == Occupancy.AUTOAWAY.ordinal.toDouble()){
                    handleAutoaway(equip, occ.isForcedOccupied);
                }
                setDesiredTemp(equip, coolingTemp, "cooling", occ.isForcedOccupied)
                setDesiredTemp(equip, heatingTemp, "heating", occ.isForcedOccupied)
                setDesiredTemp(equip, avgTemp, "average", occ.isForcedOccupied)
            }
        }

        private fun updateHyperstateEquipStatus(equipId: String, status: String?, state: ZoneState?) {
            CCUHsApi.getInstance().writeDefaultVal(
                "point and status and message and writable and equipRef == \"$equipId\"", status
            )
            zoneDataInterface?.refreshScreen("")
        }


        fun setDesiredTemp(equip: Equip, desiredTemp: Double?, flag: String, isForcedOccupied: Boolean) {

            val points: ArrayList<*>? = CCUHsApi.getInstance().readAll(
                "point and air and temp and " + flag + " and desired and sp and equipRef  ==  \"${equip.id}\""
            )
            if (points == null || points.size == 0) {
                return  //Equip might have been deleted.
            }
            val id = (points[0] as java.util.HashMap<*, *>)["id"].toString()
            if (isForcedOccupied) {
                CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id))
                CcuLog.d(L.TAG_CCU_SCHEDULER, flag + "FC DesiredTemp not changed : Skip PointWrite=")
                return
            } else if (HSUtil.getPriorityLevelVal(id, 8) == desiredTemp) {
                CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id))
                CcuLog.d(L.TAG_CCU_SCHEDULER, flag + "DesiredTemp not changed : Skip PointWrite")
                return
            }
            try {
                CCUHsApi.getInstance().pointWrite(
                    HRef.make(id.replace("@", "")),
                    8,
                    "Scheduler",
                    if (desiredTemp != null) HNum.make(desiredTemp) else HNum.make(0),
                    HNum.make(0)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id))
        }


        fun updateHyperstatUIPoints(equipRef: String, command: String, value: Double) {

            val haystack: CCUHsApi = CCUHsApi.getInstance()

            class PointUpdateTask : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg void: Void?): Void? {

                    // On progress of execution
                    Log.i(L.TAG_CCU_HSCPU, "On progress update Hyperstat UI Points: ")
                    val currentData = haystack.read(
                        "point and hyperstat and $command and equipRef == \"$equipRef\""
                    )
                    if (currentData?.get("id") != null) {

                        val id: String = currentData["id"].toString()
                        val pointDetails = Point.Builder().setHashMap(haystack.readMapById(id)).build()

                        if(pointDetails.markers.contains("writable")){
                            Log.i(L.TAG_CCU_HSCPU, " updated point write $id")
                            haystack.pointWriteForCcuUser(
                                HRef.copy(id),
                                TunerConstants.UI_DEFAULT_VAL_LEVEL,
                                HNum.make(value),
                                HNum.make(0)
                            )
                        }
                        if(pointDetails.markers.contains("his")){
                            Log.i(L.TAG_CCU_HSCPU, " updated his write $id")
                            haystack.writeHisValById(id, value)
                        }
                    }
                    Log.i(L.TAG_CCU_HSCPU, " update Hyperstat UI Points work done")
                    return null
                }

                override fun onPostExecute(result: Void?) {
                    super.onPostExecute(result)
                    zoneDataInterface?.refreshScreen("")
                    configurationDisposable.dispose()
                }
            }
            PointUpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }


        private fun handleAutoaway(equip: Equip, isForcedOccupied: Boolean) {
            val coolingDtPoint = CCUHsApi.getInstance().read(
                "point and air and temp and " +
                        "desired and cooling and sp and equipRef == \"" + equip.id + "\""
            )
            if (coolingDtPoint == null || coolingDtPoint.size == 0) {
                throw IllegalArgumentException()
            }
            val heatingDtPoint = CCUHsApi.getInstance().read(
                ("point and air and temp and " +
                        "desired and heating and sp and equipRef == \"" + equip.id + "\"")
            )
            if (heatingDtPoint == null || heatingDtPoint.size == 0) {
                throw IllegalArgumentException()
            }
            val autoAwaySetBack = TunerUtil.readTunerValByQuery("auto and away and setback")
            Log.i(L.TAG_CCU_HSCPU,"autoAwaySetbackTemp $autoAwaySetBack");

            val heatingDT = getPriorityDesiredTemp(heatingDtPoint["id"].toString())
            val coolingDT = getPriorityDesiredTemp(coolingDtPoint["id"].toString())
            val hp = Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(heatingDtPoint.get("id").toString()))
                .build()
            val cp =
                Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(coolingDtPoint["id"].toString())).build()


            setDesiredatlevel3(hp,(heatingDT - autoAwaySetBack), isForcedOccupied, equip, "heating")
            setDesiredatlevel3(cp,(coolingDT + autoAwaySetBack), isForcedOccupied, equip, "cooling")
        }

        private fun setDesiredatlevel3(
            p: Point,
            desiredTemp: Double,
            isForcedOccupied: Boolean,
            equip: Equip,
            flag: String
        ) {
            val occ = ScheduleProcessJob.getOccupiedModeCache(p.roomRef)
            CcuLog.d(
                L.TAG_CCU_SCHEDULER,
                "setDesiredatlevel3 Equip: " + equip.displayName + " Temp: " + desiredTemp + " Flag: " + flag + "," + isForcedOccupied
            )
            val point = CCUHsApi.getInstance()
                .read("point and air and temp and " + flag + " and desired and sp and equipRef == \"" + equip.id + "\"")
            if (point == null || point.size == 0) {
                return  //Equip might have been deleted.
            }
            val id = point["id"].toString()
            if (isForcedOccupied) return
            if (HSUtil.getPriorityLevelVal(id, 3) == desiredTemp) {
                CcuLog.d(L.TAG_CCU_SCHEDULER, flag + "DesiredTemp not changed : Skip PointWrite")
                return
            }
            val day = occ.currentlyOccupiedSchedule ?: return
            val overrideExpiry = DateTime(MockTime.getInstance().mockTime)
                .withHourOfDay(day.ethh)
                .withMinuteOfHour(day.etmm)
                .withDayOfWeek(day.day + 1)
                .withSecondOfMinute(0)
            CCUHsApi.getInstance().pointWrite(
                HRef.make(id.replace("@", "")), 3,
                "Scheduler", HNum.make(desiredTemp), HNum.make(
                    overrideExpiry.millis
                            - System.currentTimeMillis(), "ms"
                )
            )
            CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id))
            SystemScheduleUtil.setAppOverrideExpiry(p, overrideExpiry.millis)
        }

        private fun getPriorityDesiredTemp(id: String): Double {
            val values = CCUHsApi.getInstance().readPoint(id)
            if (values != null && values.size > 0) {
                for (l in 4..values.size) {
                    val valMap = values[l - 1]
                    if (valMap["val"] != null) {
                        return valMap["val"].toString().toDouble()
                    }
                }
            }
            return 0.0        }
    }
}