package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToCooling
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyAnalogOutEnabledAssociatedToHeating
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayEnabledAssociatedToCooling
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayEnabledAssociatedToHeating

import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconEquip
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import org.projecthaystack.HNum
import org.projecthaystack.HRef

/**
 * Created by Manjunath K on 06-08-2021.
 */
class HSSplitHaystackUtil(
    val equipRef: String,
    private val haystack: CCUHsApi

) {
    companion object {

        fun getPossibleConditioningModeSettings(node: Int): PossibleConditioningMode {
            var status = PossibleConditioningMode.OFF
            try {
                val equip = HyperStatSplitCpuEconEquip.getHyperStatSplitEquipRef(node.toShort())
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
                val equip = HyperStatSplitCpuEconEquip.getHyperStatSplitEquipRef(node.toShort())
                val fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(equip.getConfiguration())
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
            val equip = HyperStatSplitCpuEconEquip.getHyperStatSplitEquipRef(nodeAddress.toShort())
            return HyperStatSplitAssociationUtil.getSelectedFanModeByLevel(
                fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(equip.getConfiguration()),
                selectedFan = position
            ).ordinal

        }

        fun getFanSelectionMode(nodeAddress: String, position: Int): Int{
            val equip = HyperStatSplitCpuEconEquip.getHyperStatSplitEquipRef(nodeAddress.toShort())
            return HyperStatSplitAssociationUtil.getSelectedFanMode(
                fanLevel = HyperStatSplitAssociationUtil.getSelectedFanLevel(equip.getConfiguration()),
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

    fun readConfigStatus(markers: String): Double {
        return haystack.readDefaultVal(
            "point and config and $markers and enabled and equipRef == \"$equipRef\""
        )
    }

    fun readConfigAssociation(markers: String): Double {
        return haystack.readDefaultVal(
            "point and config and $markers and association and equipRef == \"$equipRef\""
        )
    }

    fun readPointID(markers: String): String? {
        val pointMap: HashMap<*, *> = haystack.read(
            "point and $markers and equipRef == \"$equipRef\""
        )
        return pointMap["id"] as String?
    }

    fun readConfigPointValue(markers: String): Double {
        return haystack.readDefaultVal(
            "point and config and $markers and equipRef == \"$equipRef\""
        )
    }

    fun readPointValue(markers: String): Double {
        return haystack.readDefaultVal(
            "point and $markers and equipRef == \"$equipRef\""
        )
    }

    fun readPointPriorityVal(markers: String): Double {
        return haystack.readPointPriorityValByQuery(
            "point and $markers and equipRef == \"$equipRef\""
        )
    }

    fun readHisVal(markers: String): Double {
        return haystack.readHisValByQuery(
            "point and $markers and equipRef == \"$equipRef\""
        )
    }

    fun writeDefaultVal(markers: String, value: Double) {
        haystack.writeDefaultVal(
            "point and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun writeDefaultVal(markers: String, value: String) {
        haystack.writeDefaultVal(
            "point and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun writeHisValueByID(id: String, value: Double) {
        haystack.writeHisValById(id, value)
    }

    private fun writeDefaultValueByID(id: String, value: Double) {
        haystack.writeDefaultValById(id, value)
    }

    fun writeDefaultWithHisValue(id: String, value: Double) {
        writeHisValueByID(id, value)
        writeDefaultValueByID(id, value)
    }

    fun removePoint(pointId: String) {
        haystack.deleteEntityTree(pointId)
    }


    fun getCurrentTemp(): Double {
        return haystack.readHisValByQuery(
            "point and air and current and temp and sensor and equipRef == \"$equipRef\""
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
        return readPointPriorityVal("zone and sp and conditioning and mode")
    }

    fun getCurrentFanMode(): Double {
        return readPointPriorityVal("zone and fan and mode and operation")
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
            "point and status and not message and not ota and his and equipRef == \"$equipRef\"", status
        )
    }

    fun getAnalogFanSpeedMultiplier(): Double {
        return TunerUtil.readTunerValByQuery(
            "analog and fan and speed and multiplier and equipRef == \"$equipRef\"")
    }

    fun getHumidity(): Double {
        return haystack.readHisValByQuery(
            "point and air and humidity and sensor and equipRef == \"$equipRef\""
        )

    }

    fun readCo2Value(): Double {
        return haystack.readHisValByQuery(
            "point and co2 and sensor and equipRef == \"$equipRef\""
        )
    }

    fun setHumidity(humidity: Double) {
        haystack.writeHisValByQuery(
            "point and air and humidity and sensor and equipRef == \"$equipRef\"", humidity
        )

    }

    fun setProfilePoint(markers: String, value: Double) {
        haystack.writeHisValByQuery(
            "point and  his and $markers and equipRef == \"$equipRef\"",
            value
        )
    }

    fun getStatus(): Double {
        return haystack.readHisValByQuery(
            "point and status and not message and not ota and his and equipRef == \"$equipRef\""
        )
    }

    fun setStatus(status: Double) {
        haystack.writeHisValByQuery(
            "point and status and not message and not ota and his and equipRef == \"$equipRef\"", status
        )
    }

    private fun getOccupancySensorPointValue(): Double {
        return haystack.readHisValByQuery(
            "point and occupancy and sensor and his and equipRef == \"$equipRef\""
        )
    }

    fun getOccupancyModePointValue(): Double {
        return haystack.readHisValByQuery(
            "point and occupancy " +
                    "and mode and his and equipRef == \"$equipRef\""
        )
    }


    fun updateOccupancyDetection() {
        val detectionPointId = readPointID("occupancy and detection and his")
        if (getOccupancySensorPointValue() > 0)
            haystack.writeHisValueByIdWithoutCOV(detectionPointId, 1.0)
    }

    fun getSensorPointValue(markers: String): Double {
        return haystack.readHisValByQuery(
            "point and $markers and his and equipRef == \"$equipRef\""
        )
    }

    fun updateConditioningLoopOutput(coolingLoop: Int, heatingLoop: Int, fanLoop: Int, isHpuProfile: Boolean, compressorLoop: Int) {
        haystack.writeHisValByQuery(
            "point and  his and cooling and loop and output and modulating " +
                    "and equipRef  == \"$equipRef\"", coolingLoop.toDouble()
        )
        haystack.writeHisValByQuery(
            "point and  his and heating and loop and output and modulating " +
                    "and equipRef  == \"$equipRef\"", heatingLoop.toDouble()
        )
        haystack.writeHisValByQuery(
            "point and his and fan and loop and output and modulating " +
                    "and equipRef  == \"$equipRef\"", fanLoop.toDouble()
        )

        if(isHpuProfile){
            haystack.writeHisValByQuery(
                "point and his and compressor and loop and output and modulating " +
                        "and equipRef  == \"$equipRef\"", compressorLoop.toDouble()
            )
        }
    }

    fun updateOaoLoopOutput(economizingLoop: Int, dcvLoop: Int, oaoLoop: Int, oaoFinalLoop: Int) {
        haystack.writeHisValByQuery(
            "point and  his and economizing and loop and output " +
                    "and equipRef  == \"$equipRef\"", economizingLoop.toDouble()
        )


    }


    fun reWriteOccupancy() {
        val ocupancyDetection = CCUHsApi.getInstance().read(
            "point and cpu and occupancy and detection and his and equipRef  ==" +
                    " \"" + equipRef + "\"")
        if (ocupancyDetection.size> 0) {
            val pointValue = CCUHsApi.getInstance().readHisValById(ocupancyDetection["id"].toString())
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(ocupancyDetection["id"].toString(), pointValue)
        }
    }

    fun getTempOffValue(): Double{
        return (haystack.readDefaultVal("point and temp and offset and equipRef == \"$equipRef\"")/ 10)
    }
    fun isAutoForceOccupyEnabled(): Boolean{
        return (readConfigStatus("auto and forced and control and occupied or occupancy").toInt() == 1)
    }
    fun isAutoAwayEnabled(): Boolean{
        return (readConfigStatus("auto and away").toInt() == 1)
    }
    fun getCo2DamperOpeningConfigValue(): Double{
        return haystack.readDefaultVal(
            "point and co2 and opening and rate and equipRef == \"$equipRef\"")
    }
    fun getCo2DamperThresholdConfigValue(): Double{
        return  haystack.readDefaultVal(
            "point and co2 and threshold and equipRef == \"$equipRef\""
        )
    }
    fun getCo2TargetConfigValue(): Double{
        return  haystack.readDefaultVal(
            "point and co2 and target and equipRef == \"$equipRef\""
        )
    }
    fun getVocThresholdConfigValue(): Double{
        return  haystack.readDefaultVal(
            "point and voc and threshold and equipRef == \"$equipRef\""
        )
    }
    fun getVocTargetConfigValue(): Double{
        return  haystack.readDefaultVal(
            "point and voc and target and equipRef == \"$equipRef\""
        )
    }
    fun getPm2p5ThresholdConfigValue(): Double{
        return haystack.readDefaultVal(
            "point and pm2p5 and threshold and equipRef == \"$equipRef\""
        )
    }
    fun getPm2p5TargetConfigValue(): Double{
        return  haystack.readDefaultVal(
            "point and pm2p5 and target and equipRef == \"$equipRef\""
        )
    }

    fun updateKeycardValues(keycardEnabled: Double, keycardSensor: Double){
        haystack.writeDefaultVal("keycard and sensing and enabled and equipRef == \"$equipRef\"",keycardEnabled)
        haystack.writeHisValByQuery("keycard and sensor and input and equipRef == \"$equipRef\"",keycardSensor)
    }

    fun updateDoorWindowValues(doorWindowEnabled: Double, doorWindowSensor: Double){
        haystack.writeDefaultVal("window and sensing and enabled and equipRef == \"$equipRef\"",doorWindowEnabled)
        haystack.writeHisValByQuery("window and sensor and input and equipRef == \"$equipRef\"",doorWindowSensor)
    }

    fun getDisplayHumidity(): Double{
        return  haystack.readDefaultVal(
            "point and humidity and enabled and equipRef == \"$equipRef\""
        )
    }

    fun getDisplayCo2(): Double{
        return  haystack.readDefaultVal(
            "point and co2 and enabled and equipRef == \"$equipRef\""
        )
    }

    fun getDisplayVoc(): Double{
        return  haystack.readDefaultVal(
            "point and voc and enabled and equipRef == \"$equipRef\""
        )
    }

    fun getDisplayP2p5(): Double{
        return  haystack.readDefaultVal(
            "point and pm2p5 and enabled and equipRef == \"$equipRef\""
        )
    }
}
