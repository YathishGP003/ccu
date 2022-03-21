package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.*
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToCooling
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToHeating
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.isAnyRelayEnabledAssociatedToCooling
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.isAnyRelayEnabledAssociatedToHeating
import a75f.io.logic.bo.building.hyperstat.cpu.HyperStatCpuEquip
import a75f.io.logic.jobs.ScheduleProcessJob
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import java.lang.Exception
import java.lang.NullPointerException
import java.util.*

/**
 * Created by Manjunath K on 06-08-2021.
 */
class HSHaystackUtil(
    private val profileName: String,
    private val equipRef: String,
    private val haystack: CCUHsApi

) {
    companion object {

        fun getBasicSettings(node: Int): BasicSettings {
            try {
                val equip = HyperStatCpuEquip.getHyperstatEquipRef(node.toShort())
                return if (equip.equipRef != null) {
                    BasicSettings(
                        StandaloneConditioningMode.values()[equip.hsHaystackUtil!!.getCurrentConditioningMode().toInt()],
                        StandaloneFanStage.values()[equip.hsHaystackUtil!!.getCurrentFanMode().toInt()]
                    )
                } else {
                    BasicSettings(StandaloneConditioningMode.OFF, StandaloneFanStage.OFF)
                }
            }catch (e:Exception){
                e.printStackTrace()
                Log.i(L.TAG_CCU_HSCPU, "Exception getBasicSettings: ${e.localizedMessage} for $node ")
            }
            return BasicSettings(StandaloneConditioningMode.OFF, StandaloneFanStage.OFF)
        }

        fun getPossibleConditioningModeSettings(node: Int): PossibleConditioningMode {
            var status = PossibleConditioningMode.OFF
            try {
                val equip = HyperStatCpuEquip.getHyperstatEquipRef(node.toShort())
                // Add conditioning status
                val config = equip.getConfiguration()
                if ((isAnyRelayEnabledAssociatedToCooling(config) || isAnyAnalogOutEnabledAssociatedToCooling(config))
                    && (isAnyRelayEnabledAssociatedToHeating(config)|| isAnyAnalogOutEnabledAssociatedToHeating(config) )) {
                    status = PossibleConditioningMode.BOTH
                } else if (isAnyRelayEnabledAssociatedToCooling(config) || isAnyAnalogOutEnabledAssociatedToCooling(config)) {
                    status = PossibleConditioningMode.COOLONLY
                } else if (isAnyRelayEnabledAssociatedToHeating(config)||isAnyAnalogOutEnabledAssociatedToHeating(config)) {
                    status = PossibleConditioningMode.HEATONLY
                }

            }catch (e: Exception){
                Log.i(L.TAG_CCU_HSCPU, "Exception getPossibleConditioningModeSettings: ${e.message}")
            }
            Log.i(L.TAG_CCU_HSCPU, "getPossibleConditioningModeSettings: $status")
            return status
        }

        fun getActualConditioningMode(nodeAddress: String, selectedConditioningMode: Int): Int{
            if(selectedConditioningMode == 0)
                return StandaloneConditioningMode.OFF.ordinal
            return when(getPossibleConditioningModeSettings(nodeAddress.toInt())){
                PossibleConditioningMode.BOTH-> {
                    StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
                }
                PossibleConditioningMode.COOLONLY->{
                    StandaloneConditioningMode.COOL_ONLY.ordinal
                }
                PossibleConditioningMode.HEATONLY->{
                    StandaloneConditioningMode.HEAT_ONLY.ordinal
                }
                PossibleConditioningMode.OFF->{
                    StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
                }
            }
        }

        fun getSelectedConditioningMode(nodeAddress: String, actualConditioningMode: Int): Int{
            if(actualConditioningMode == 0)
                return StandaloneConditioningMode.OFF.ordinal
            return if(getPossibleConditioningModeSettings(nodeAddress.toInt()) ==  PossibleConditioningMode.BOTH)
                StandaloneConditioningMode.values()[actualConditioningMode].ordinal
            else
                1 // always it will be 1 because possibility is Off,CoolOnly | Off,Heatonly

        }


        fun getPossibleFanModeSettings(node: Int): PossibleFanMode {
            try {
                val equip = HyperStatCpuEquip.getHyperstatEquipRef(node.toShort())
                val fanLevel = HyperStatAssociationUtil.getSelectedFanLevel(equip.getConfiguration())
                if (fanLevel == 6) return PossibleFanMode.LOW
                if (fanLevel == 7) return PossibleFanMode.MED
                if (fanLevel == 8) return PossibleFanMode.HIGH
                if (fanLevel == 13) return PossibleFanMode.LOW_MED
                if (fanLevel == 14) return PossibleFanMode.LOW_HIGH
                if (fanLevel == 15) return PossibleFanMode.MED_HIGH
                if (fanLevel == 21) return PossibleFanMode.LOW_MED_HIGH
            }catch (e:Exception){
                Log.i(L.TAG_CCU_HSCPU, "Exception getPossibleFanModeSettings: ${e.localizedMessage}")
            }
            return PossibleFanMode.OFF
        }

        fun getActualFanMode(nodeAddress: String, position: Int): Int{
            val equip = HyperStatCpuEquip.getHyperstatEquipRef(nodeAddress.toShort())
            return HyperStatAssociationUtil.getSelectedFanModeByLevel(
                fanLevel = HyperStatAssociationUtil.getSelectedFanLevel(equip.getConfiguration()),
                selectedFan = position
            ).ordinal

        }

        fun getFanSelectionMode(nodeAddress: String, position: Int): Int{
            val equip = HyperStatCpuEquip.getHyperstatEquipRef(nodeAddress.toShort())
            return HyperStatAssociationUtil.getSelectedFanMode(
                fanLevel = HyperStatAssociationUtil.getSelectedFanLevel(equip.getConfiguration()),
                selectedFan = position
            )
        }
    }

    fun getEquipLiveStatus(): String? {
        val equipStatusPointId = readPointID("status and message")
        if (equipStatusPointId != null) {
            return haystack.readDefaultStrValById(equipStatusPointId)
        }
        return null
    }

    fun readConfigStatus(equipRef: String, markers: String): Double {
        return haystack.readDefaultVal(
            "point and hyperstat and $profileName and config and $markers and enabled and equipRef == \"$equipRef\""
        )
    }

    fun readConfigAssociation(equipRef: String, markers: String): Double {
        return haystack.readDefaultVal(
            "point and hyperstat and $profileName and config and $markers and association and equipRef == \"$equipRef\""
        )
    }

    fun readPointID(markers: String): String? {
        val pointMap: HashMap<*, *> = haystack.read(
            "point and hyperstat and $profileName and $markers and equipRef == \"$equipRef\""
        )
        return pointMap["id"] as String?
    }


    fun readConfigPointValue(markers: String): Double {
        return haystack.readDefaultVal(
            "point and hyperstat and $profileName and config and $markers and equipRef == \"$equipRef\""
        )
    }

    fun readPointValue(markers: String): Double {
        return haystack.readDefaultVal(
            "point and hyperstat and $profileName and $markers and equipRef == \"$equipRef\""
        )
    }

    fun readPointPriorityVal(markers: String): Double {
        return haystack.readPointPriorityValByQuery(
            "point and hyperstat and $profileName and $markers and equipRef == \"$equipRef\""
        )
    }

    fun readHisVal(markers: String): Double {
        return haystack.readHisValByQuery(
            "point and hyperstat and $profileName and $markers and equipRef == \"$equipRef\""
        )
    }

    fun writeDefaultVal(markers: String, value: Double) {
        haystack.writeDefaultVal(
            "point and hyperstat and $profileName and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun writeDefaultVal(markers: String, value: String) {
        haystack.writeDefaultVal(
            "point and hyperstat and $profileName and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun writeHisValueByID(id: String, value: Double) {
        haystack.writeHisValById(id, value)
    }

    private fun writeDefaultValueByID(id: String, value: Double) {
        haystack.writeDefaultValById(id, value)
    }

    private fun writeDefaultWithHisValue(id: String, value: Double) {
        writeHisValueByID(id, value)
        writeDefaultValueByID(id, value)
    }

    fun removePoint(pointId: String) {
        haystack.deleteEntityTree(pointId)
    }


    fun getCurrentTemp(): Double {
        return haystack.readHisValByQuery(
            "point and air and temp and sensor and current and equipRef == \"$equipRef\""
        )
    }

    fun setCurrentTemp(roomTemp: Double) {
        haystack.writeHisValByQuery(
            "point and air and temp and sensor and current and equipRef == \"$equipRef\"", roomTemp
        )

    }

    private fun readPointIdWithAll(markers: String): String {
        val points: ArrayList<*> = haystack.readAll(
            "point and $markers and sp and equipRef == \"$equipRef\""
        )
        return (points[0] as HashMap<*, *>)["id"].toString()
    }


    fun getDesiredTemp(): Double {
        val id = readPointIdWithAll("air and temp and desired and average")
        require(!(id === ""))
        return haystack.readDefaultValById(id)
    }

    fun setDesiredTemp(desiredTemp: Double) {
        val id = readPointIdWithAll("air and temp and desired and average")
        require(!(id === ""))
        writeDefaultWithHisValue(id, desiredTemp)
    }

    fun getDesiredTempCooling(): Double {
        val id = readPointIdWithAll("air and temp and desired and cooling")
        require(!(id === ""))
        val values: ArrayList<*>? = haystack.readPoint(id)
        if (values != null && values.size > 0) {
            for (l in 1..values.size) {
                val valMap = values[l - 1] as HashMap<*, *>
                if (valMap["val"] != null) {
                    return valMap["val"].toString().toDouble()
                }
            }
        }
        return 0.0
    }

    fun setDesiredTempCooling(desiredTemp: Double) {
        val id = readPointIdWithAll("air and temp and desired and cooling")
        require(!(id === ""))
        haystack.pointWriteForCcuUser(
            HRef.copy(id),
            HayStackConstants.DEFAULT_POINT_LEVEL,
            HNum.make(desiredTemp),
            HNum.make(0)
        )
        haystack.writeHisValById(id, desiredTemp)
    }

    fun getDesiredTempHeating(): Double {

        val id = readPointIdWithAll("air and temp and desired and heating")
        require(!(id === ""))

        val values: ArrayList<*>? = CCUHsApi.getInstance().readPoint(id)
        if (values != null && values.size > 0) {
            for (l in 1..values.size) {
                val valMap = values[l - 1] as HashMap<*, *>
                if (valMap["val"] != null) {
                    return valMap["val"].toString().toDouble()
                }
            }
        }
        return 0.0
    }

    fun setDesiredTempHeating(desiredTemp: Double) {
        val id = readPointIdWithAll("air and temp and desired and heating")
        require(!(id === ""))
        haystack.pointWriteForCcuUser(
            HRef.copy(id),
            HayStackConstants.DEFAULT_POINT_LEVEL,
            HNum.make(desiredTemp),
            HNum.make(0)
        )
        haystack.writeHisValById(id, desiredTemp)
    }

    fun getCurrentConditioningMode(): Double {
        return readPointPriorityVal("zone and temp and mode and conditioning")
    }

    fun getCurrentFanMode(): Double {
        return readPointPriorityVal("zone and fan and mode and operation")
    }

    fun getOccupancyStatus(): Occupied {
        return ScheduleProcessJob.getOccupiedModeCache(HSUtil.getZoneIdFromEquipId(equipRef))
    }

    fun getTargetMinInsideHumidity(): Double {
        return readPointPriorityVal("target and humidifier and his")
    }

    fun getTargetMaxInsideHumidity(): Double {
        return readPointPriorityVal("target and dehumidifier and his")
    }

    fun getEquipStatus(): Double {
        return readHisVal("status and not message and his")
    }

    fun setEquipStatus(status: Double) {
        haystack.writeHisValByQuery(
            "point and status and not message and his and equipRef == \"$equipRef\"", status
        )
    }

    fun getAnalogFanSpeedMultiplier(): Double {
        return TunerUtil.readTunerValByQuery(
            "hyperstat and analog and fan and speed and multiplier and equipRef == \"$equipRef\"")
    }

    fun getHumidity(): Double {
        return haystack.readHisValByQuery(
            "point and air and humidity and sensor and current and equipRef == \"$equipRef\""
        )

    }

    fun readCo2Value(): Double {
        return haystack.readHisValByQuery(
            "point and co2 and  sensor and hyperstat and equipRef == \"$equipRef\""
        )
    }

    fun setHumidity(humidity: Double) {
        haystack.writeHisValByQuery(
            "point and air and humidity and sensor and current and equipRef == \"$equipRef\"", humidity
        )

    }

    fun setProfilePoint(markers: String, value: Double) {
        haystack.writeHisValByQuery(
            "point and hyperstat and $profileName and his and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun getStatus(): Double {
        return haystack.readHisValByQuery(
            "point and status and not message and his and equipRef == \"$equipRef\""
        )
    }

    fun setStatus(status: Double) {
        haystack.writeHisValByQuery(
            "point and status and not message and his and equipRef == \"$equipRef\"", status
        )
    }

    fun getOccupancySensorPointValue(): Double {
        return haystack.readHisValByQuery(
            "point and hyperstat and $profileName and occupancy and sensor and his and equipRef == \"$equipRef\""
        )
    }

    fun getOccupancyModePointValue(): Double {
        return haystack.readHisValByQuery(
            "point and hyperstat and $profileName and occupancy " +
                    "and mode and his and equipRef == \"$equipRef\""
        )
    }

    fun setOccupancyMode(status: Double) {
        haystack.writeHisValByQuery(
            "point and hyperstat and $profileName and occupancy " +
                    "and mode and his and equipRef == \"$equipRef\"", status
        )
    }


    fun getHeatingDeadbandPoint(): HashMap<*, *>? {
        return haystack.read(
            "point and air and temp and desired and heating and sp and equipRef == \"$equipRef\""
        )
    }

    fun getCoolingDeadbandPoint(): HashMap<*, *>? {
        return haystack.read(
            "point and air and temp and desired and cooling and sp and equipRef == \"$equipRef\""
        )
    }

    fun getAvgDesiredTempPoint(): HashMap<*, *>? {
        return haystack.read(
            "point and air and temp and desired and average and sp and equipRef == \"$equipRef\""
        )
    }

    fun updateOccupancyDetection() {
        val detectionPointId = readPointID("occupancy and detection and his")
        if (getOccupancySensorPointValue() > 0)
            haystack.writeHisValueByIdWithoutCOV(detectionPointId, 1.0)
    }

    fun readDetectionPointDetails(): HashMap<*, *> {
        return haystack.read(
            "point and hyperstat and $profileName" +
                    " and occupancy and detection and his and equipRef == \"$equipRef\""
        )
    }

    fun getSensorPointValue(markers: String): Double {
        return haystack.readHisValByQuery(
            "point and hyperstat and $profileName and $markers and his and equipRef == \"$equipRef\""
        )
    }

    fun updateAllLoopOutput(coolingLoop: Int, heatingLoop: Int, fanLoop: Int) {
        haystack.writeHisValByQuery(
            "point and hyperstat and $profileName and his and cooling and loop and output and modulating " +
                    "and equipRef  == \"$equipRef\"", coolingLoop.toDouble()
        )
        haystack.writeHisValByQuery(
            "point and hyperstat and $profileName and his and heating and loop and output and modulating " +
                    "and equipRef  == \"$equipRef\"", heatingLoop.toDouble()
        )
        haystack.writeHisValByQuery(
            "point and hyperstat and $profileName and his and fan and loop and output and modulating " +
                    "and equipRef  == \"$equipRef\"", fanLoop.toDouble()
        )
    }

    fun setSensorOccupancyPoint(value: Double) {
        haystack.writeHisValByQuery(
            "point and hyperstat and $profileName and occupancy and sensor and his and equipRef == \"$equipRef\"",
            value
        )
        updateOccupancyDetection()
    }

    fun reWriteOccupancy(equipReff: String){
        val ocupancyDetectionHscpu = CCUHsApi.getInstance().read(
            "point and hyperstat and cpu and occupancy and detection and his and equipRef  ==" +
                    " \"" + equipReff + "\"");
        if (ocupancyDetectionHscpu.size> 0) {

             val pointValue = CCUHsApi.getInstance().readHisValById(ocupancyDetectionHscpu["id"].toString());
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(ocupancyDetectionHscpu["id"].toString(), pointValue);
        }
    }

     fun getDesiredTempCoolingPriorityValue(equipRef: String): Double {
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                "desired and temp and cooling and equipRef == \"$equipRef\""
            )
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return 0.0
    }

     fun getDesiredTempHeatingPriorityValue(equipRef: String): Double {
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                "desired and temp and heating and equipRef == \"$equipRef\""
            )

        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return 0.0
    }

     fun getAverageDesiredTempPriorityValue(equipRef: String): Double {
        try {
            return CCUHsApi.getInstance().readPointPriorityValByQuery(
                ("point and desired and average and temp and equipRef == \"$equipRef\"")
            )
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return 0.0
    }

}