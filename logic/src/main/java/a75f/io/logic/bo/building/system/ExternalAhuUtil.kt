package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName.airTempCoolingSp
import a75f.io.domain.api.DomainName.airTempHeatingSp
import a75f.io.domain.api.DomainName.co2WeightedAverage
import a75f.io.domain.api.DomainName.conditioningMode
import a75f.io.domain.api.DomainName.dcvDamperCalculatedSetpoint
import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dcvLoopOutput
import a75f.io.domain.api.DomainName.dehumidifierEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.ductStaticPressureSetpoint
import a75f.io.domain.api.DomainName.epidemicModeSystemState
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.fanLoopOutput
import a75f.io.domain.api.DomainName.humidifierEnable
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.operatingMode
import a75f.io.domain.api.DomainName.satSetpointControlEnable
import a75f.io.domain.api.DomainName.staticPressureSPMax
import a75f.io.domain.api.DomainName.staticPressureSPMin
import a75f.io.domain.api.DomainName.staticPressureSetpointControlEnable
import a75f.io.domain.api.DomainName.supplyAirflowTemperatureSetpoint
import a75f.io.domain.api.DomainName.systemCO2DamperOpeningRate
import a75f.io.domain.api.DomainName.systemCO2Target
import a75f.io.domain.api.DomainName.systemCO2Threshold
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemOccupancyMode
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
import a75f.io.domain.api.DomainName.systemtargetMaxInsideHumidty
import a75f.io.domain.api.DomainName.systemtargetMinInsideHumidty
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.autocommission.AutoCommissioningUtil
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface
import a75f.io.logic.bo.building.system.dab.DabExternalAhu
import a75f.io.logic.bo.building.system.vav.VavExternalAhu
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.tuners.TunerUtil
import a75f.io.logic.util.PreferenceUtil
import a75f.io.logic.util.RxjavaUtil
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESTINATION_IP
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DESTINATION_PORT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DEVICE_ID
import a75f.io.logic.util.bacnet.BacnetConfigConstants.DEVICE_NETWORK
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MAC_ADDRESS
import android.content.Intent
import android.preference.PreferenceManager
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects
import kotlin.math.roundToInt

/**
 * Created by Manjunath K on 27-10-2023.
 */

fun mapToSetPoint(min: Double, max: Double, current: Double): Double =
    ((max - min) * (current / 100.0) + min)


const val SINGLE_SAT_SET_POINT = "air and discharge and sp and temp"
const val HEATING_SAT_SET_POINT = "air and heating and sp and temp"
const val COOLING_SAT_SET_POINT = "air and cooling and sp and temp"
const val DUCT_STATIC_PRESSURE = "pressure and air and discharge and sp"
const val DAMPER_CMD = "cmd and outside and dcv and damper"
const val HUMIDIFIER_CMD = "cmd and enable and humidifier"
const val DEHUMIDIFIER_CMD = "cmd and dessicantDehumidifier"
const val OCCUPANCY_MODE = "mode and occupied and sp"
const val DISCHARGE_AIR_TEMP = "air and discharge and temp and sensor"
const val DUCT_STATIC_PRESSURE_SENSOR = "air and discharge and pressure and sensor"
const val OPERATING_MODE = "mode and operating and sp"

private val TAG_BACNET = "ExternalAHU_BACNET"

fun pushSatSetPoints(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushSatSetPoints")
        mapModbusPoint(haystack, SINGLE_SAT_SET_POINT, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushSatSetPoints")
        mapBacnetPoint(haystack, SINGLE_SAT_SET_POINT, equipId, value, setPointsList)
    }
}

fun pushSatCoolingSetPoints(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushSatCoolingSetPoints")
        mapModbusPoint(haystack, COOLING_SAT_SET_POINT, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushSatCoolingSetPoints")
        mapBacnetPoint(haystack, COOLING_SAT_SET_POINT, equipId, value, setPointsList)
    }
}

fun pushSatHeatingSetPoints(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushSatHeatingSetPoints")
        mapModbusPoint(haystack, HEATING_SAT_SET_POINT, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushSatHeatingSetPoints")
        mapBacnetPoint(haystack, HEATING_SAT_SET_POINT, equipId, value, setPointsList)
    }
}

fun pushDuctStaticPressure(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushDuctStaticPressure")
        mapModbusPoint(haystack, DUCT_STATIC_PRESSURE, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushDuctStaticPressure")
        mapBacnetPoint(haystack, DUCT_STATIC_PRESSURE, equipId, value, setPointsList)
    }
}

fun pushDamperCmd(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushDamperCmd")
        mapModbusPoint(haystack, DAMPER_CMD, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushDamperCmd")
        mapBacnetPoint(haystack, DAMPER_CMD, equipId, value, setPointsList)
    }
}

fun pushOccupancyMode(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapBacnetPoint for pushOccupancyMode")
        mapModbusPoint(haystack, OCCUPANCY_MODE, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushOccupancyMode")
        mapBacnetPoint(haystack, OCCUPANCY_MODE, equipId, value, setPointsList)
    }
}

fun pushHumidifierCmd(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushHumidifierCmd")
        mapModbusPoint(haystack, HUMIDIFIER_CMD, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushHumidifierCmd")
        mapBacnetPoint(haystack, HUMIDIFIER_CMD, equipId, value, setPointsList)
    }
}

fun pushDeHumidifierCmd(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushDeHumidifierCmd")
        mapModbusPoint(haystack, DEHUMIDIFIER_CMD, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushDeHumidifierCmd")
        mapBacnetPoint(haystack, DEHUMIDIFIER_CMD, equipId, value, setPointsList)
    }
}

fun pushOperatingMode(
    haystack: CCUHsApi, equipId: String, value: Double, setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("modbus and id == $equipId")
    if(point.isNotEmpty()){
        CcuLog.d(TAG_BACNET, "it is modbus mapModbusPoint for pushOperatingMode")
        mapModbusPoint(haystack, OPERATING_MODE, equipId, value, setPointsList)
    }else{
        CcuLog.d(TAG_BACNET, "it is bacnet mapBacnetPoint for pushOperatingMode")
        mapBacnetPoint(haystack, OPERATING_MODE, equipId, value, setPointsList)
    }
}


fun updateSetPoint(
    systemEquip: Equip,
    setPoint: Double,
    domainName: String,
    externalSpList: ArrayList<String>,
    externalEquipId: String?,
    haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    CcuLog.d(TAG_BACNET, "---------updateSetPoint----------$externalEquipId")
    externalEquipId?.let {
        pushSatSetPoints(haystack, externalEquipId, setPoint, externalSpList)
    }
}

fun updateCoolingSetPoint(
    systemEquip: Equip,
    setPoint: Double,
    domainName: String,
    externalSpList: ArrayList<String>,
    externalEquipId: String?,
    haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    CcuLog.d(TAG_BACNET, "---------updateCoolingSetPoint----------$externalEquipId")
    externalEquipId?.let {
        pushSatCoolingSetPoints(haystack, externalEquipId, setPoint, externalSpList)
    }
}

fun updateHeatingSetPoint(
    systemEquip: Equip,
    setPoint: Double,
    domainName: String,
    externalSpList: ArrayList<String>,
    externalEquipId: String?,
    haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    CcuLog.d(TAG_BACNET, "---------updateHeatingSetPoint----------$externalEquipId")
    externalEquipId?.let {
        pushSatHeatingSetPoints(haystack, externalEquipId, setPoint, externalSpList)
    }
}

fun updateOperatingMode(
    systemEquip: Equip,
    setPoint: Double,
    domainName: String,
    externalSpList: ArrayList<String>,
    externalEquipId: String?,
    haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    CcuLog.d(TAG_BACNET, "---------updateOperatingMode----------$externalEquipId")
    val operatingPointEnumString = haystack.readEntity("point and domainName == \"$domainName\"")["enum"].toString().lowercase()
    val operatingPointEnumStringForExternal = haystack.readEntity("$OPERATING_MODE and equipRef == \"$externalEquipId\"")["enum"].toString().lowercase()
    val setPointValueFromModel = searchRealValueForOperatingMode(operatingPointEnumString, operatingPointEnumStringForExternal , setPoint.toInt().toString())
    externalEquipId?.let {
        pushOperatingMode(haystack, externalEquipId, setPointValueFromModel, externalSpList)
    }
}

private fun searchRealValueForOperatingMode(operatingPointEnumString: String,
                                            operatingPointEnumStringForExternal: String, inputValue : String) : Double{
    CcuLog.d(TAG_BACNET, "---------searchRealValueForOperatingMode------one----$operatingPointEnumString<--two-->$operatingPointEnumStringForExternal<--inputValue-->$inputValue")
    try {
        val mapOne = operatingPointEnumString.split(",").associate {
            val (k, v) = it.split("=")
            k to v
        }

        val mapTwo = operatingPointEnumStringForExternal.split(",").associate {
            val (k, v) = it.split("=")
            k to v
        }
        val reverseMapOne = mapOne.entries.associate { it.value to it.key }
        val matchingKey = reverseMapOne[inputValue]
        val finalValue = matchingKey?.let { mapTwo[it] }
        CcuLog.d(TAG_BACNET, "---------searchRealValueForOperatingMode------finalValue----$finalValue")
        if (finalValue != null) {
            return finalValue.toDouble()
        }
        return inputValue.toDouble()
    }catch (e : Exception){
        e.printStackTrace()
    }
    return inputValue.toDouble()
}

fun mapBacnetPoint(
    haystack: CCUHsApi,
    query: String,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    val equip = haystack.readEntity("id == $equipId")
    val point = haystack.readEntity("$query and equipRef == \"$equipId\"")
    val pointId = point[Tags.ID].toString()
    val objectIdFromPoint = point["bacnetId"].toString().toInt()
    val objectType = point["bacnetType"].toString()
    val bacnetConfig = equip["bacnetConfig"].toString()
    val defaultPriority = point["defaultWriteLevel"].toString()
    //val isSystemPoint = point["system"]
    var objectId = 0
    CcuLog.d(TAG_BACNET, "--checking bool--")
    try {
        objectId = objectIdFromPoint - 1100000

    }catch (e : Exception){
        e.printStackTrace()
        CcuLog.d(TAG_BACNET, "--we landed in error")
    }

    CcuLog.d(TAG_BACNET, "mapBacnetPoint objectId-->$objectId--objectType--$objectType--bacnetConfig--$bacnetConfig--defaultPriority--$defaultPriority--pointId--$pointId")

    if (point.isNotEmpty()) {
        CcuLog.i(TAG_BACNET, " point found $query-----#going to update local point and remote point--value--$value")
        if(BacNetConstants.ObjectType.OBJECT_MULTI_STATE_VALUE.key == getObjectType(objectType)){
            val wholeNumber = value.toInt()
            //val bacnetWholeNumber = wholeNumber
            updatePointValueChanges(pointId, haystack, setPointsList, wholeNumber.toDouble())
            doMakeRequest(BacnetServicesUtils().getConfig(bacnetConfig), objectId, wholeNumber.toString(),getObjectType(objectType), defaultPriority, pointId)
        }else if(BacNetConstants.ObjectType.OBJECT_BINARY_VALUE.key == getObjectType(objectType)){
            val wholeNumber = value.toInt()
            //  val bacnetWholeNumber = wholeNumber - 1
            updatePointValueChanges(pointId, haystack, setPointsList, wholeNumber.toDouble())
            doMakeRequest(BacnetServicesUtils().getConfig(bacnetConfig), objectId, wholeNumber.toString(),getObjectType(objectType), defaultPriority, pointId)
        }else{
            updatePointValueChanges(pointId, haystack, setPointsList, value)
            doMakeRequest(BacnetServicesUtils().getConfig(bacnetConfig), objectId, value.toString(),getObjectType(objectType), defaultPriority, pointId)
        }

    } else {
        CcuLog.i(L.TAG_CCU_MODBUS, " point not found $query")
    }
}

fun getObjectType(objectTypeValue : String) : String{
    var objectType: String
    if (objectTypeValue.equals("MultiStateValue", ignoreCase = true)) {
        objectType =
            BacNetConstants.ObjectType.OBJECT_MULTI_STATE_VALUE.key
    } else if (objectTypeValue.equals("BinaryValue", ignoreCase = true)) {
        objectType =
            BacNetConstants.ObjectType.OBJECT_BINARY_VALUE.key
    } else {
        objectType =
            BacNetConstants.ObjectType.OBJECT_ANALOG_VALUE.key
    }
    return objectType
}

fun mapModbusPoint(
    haystack: CCUHsApi,
    query: String,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("$query and equipRef == \"$equipId\"")
    val pointId = point[Tags.ID].toString()
    if (point.isNotEmpty()) {
        updatePointValueChanges(pointId, haystack, setPointsList, value)
    } else if (getChildEquipMap(equipId).isNotEmpty()) {
        mapSubEquipModbusPoint(equipId, haystack, setPointsList, query, value)
    } else {
            CcuLog.i(L.TAG_CCU_MODBUS, " point not found $query")
        }
    }
fun updatePointValueChanges(
    pointId: String,
    haystack: CCUHsApi,
    setPointsList: ArrayList<String>,
    value: Double
) {
    if (!setPointsList.contains(pointId))
        setPointsList.add(pointId)
    val currentHisValue = haystack.readHisValById(pointId)
    val currentDefaultValue = haystack.readDefaultValById(pointId)
    if (currentHisValue != value)
        haystack.writeHisValById(pointId, value)
    if (currentDefaultValue != value)
        haystack.writeDefaultValById(pointId, value)
}
fun mapSubEquipModbusPoint(
    equipId: String,
    haystack: CCUHsApi,
    setPointsList: ArrayList<String>,
    query: String,
    value: Double
) {
    val subEquips = getChildEquipMap(equipId)
    subEquips.forEach {
        val subEquipId = it["id"]
        val subEquipPoint = haystack.readEntity("$query and equipRef == \"$subEquipId\"")
        val subEquipPointId = subEquipPoint[Tags.ID].toString()
        if (subEquipPoint.isNotEmpty()) {
            updatePointValueChanges(subEquipPointId, haystack, setPointsList, value)
        }
    }
}

fun updateDefaultSetPoints(
    systemEquip: Equip, lastLoopDirection: TempDirection
): Double {
    return when (lastLoopDirection) {
        TempDirection.COOLING -> Domain.getPointByDomain(
            systemEquip, systemCoolingSATMaximum
        )
        else -> Domain.getPointByDomain(systemEquip, systemHeatingSATMinimum)
    }
}


fun getTunerByDomainName(systemEquip: Equip, domainName: String): Double =
    TunerUtil.readTunerValByQuery("domainName == \"$domainName\"", systemEquip.id)

fun getTunerByDomainName(equipRef: String, domainName: String): Double =
        TunerUtil.readTunerValByQuery("domainName == \"$domainName\"", equipRef)

fun isConfigEnabled(systemEquip: Equip, domainName: String): Boolean =
    Domain.getPointByDomain(systemEquip, domainName) == 1.0

fun writePointForCcuUser(hayStack: CCUHsApi, domainName: String, value: Double) {
    val point = Domain.readPointForEquip(domainName, L.ccu().systemProfile.systemEquipRef)
    if (point.isNotEmpty()) {
        RxjavaUtil.executeBackground {
            hayStack.writePointForCcuUser(
                point[Tags.ID].toString(), HayStackConstants.SYSTEM_POINT_LEVEL, value, 0
            )
        }
    }
}

fun calculateSATSetPoints(
    systemEquip: Equip,
    basicConfig: BasicConfig,
    externalEquipId: String?,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    loopRunningDirection: TempDirection,
) {

    logIt("Cooling lockout ${L.ccu().systemProfile.isCoolingLockoutActive} heating lockout ${L.ccu().systemProfile.isHeatingLockoutActive}")
    val isDualSetPointEnabled = isConfigEnabled(systemEquip, dualSetpointControlEnable)
    if (isDualSetPointEnabled) {
        val satSetPointLimits = getDualSetPointMinMax(systemEquip)
        val coolingSetPoint =
            if (basicConfig.coolingLoop.toDouble() == 0.0 || L.ccu().systemProfile.isCoolingLockoutActive) updateDefaultSetPoints(
                systemEquip,
                TempDirection.COOLING
            )
            else mapToSetPoint(
                satSetPointLimits.first.first,
                satSetPointLimits.first.second,
                basicConfig.coolingLoop.toDouble()
            )

        val heatingSp =
            if (basicConfig.heatingLoop.toDouble() == 0.0 || L.ccu().systemProfile.isHeatingLockoutActive) updateDefaultSetPoints(
                systemEquip,
                TempDirection.HEATING
            )
            else mapToSetPoint(
                satSetPointLimits.second.first,
                satSetPointLimits.second.second,
                basicConfig.heatingLoop.toDouble()
            )
        updateCoolingSetPoint(
            systemEquip,
            String.format("%.1f", coolingSetPoint).toDouble(),
            airTempCoolingSp,
            externalSpList,
            externalEquipId,
            haystack
        )
        updateHeatingSetPoint(
            systemEquip,
            String.format("%.1f", heatingSp).toDouble(),
            airTempHeatingSp,
            externalSpList,
            externalEquipId,
            haystack
        )
        logIt(
            "Dual SP  cooling MinMax (${satSetPointLimits.first.first}, ${satSetPointLimits.first.second})" + "heating MinMax (${satSetPointLimits.second.first}, ${satSetPointLimits.second.second})" + " coolingSatSetPointValue $coolingSetPoint heatingSatSetPointValue $heatingSp"
        )
    } else {
        val satSetPointLimits = getDualSetPointMinMax(systemEquip)
        var isLockoutActive = false
        if (loopRunningDirection == TempDirection.COOLING) isLockoutActive =
            L.ccu().systemProfile.isCoolingLockoutActive
        if (loopRunningDirection == TempDirection.HEATING) isLockoutActive =
            L.ccu().systemProfile.isHeatingLockoutActive
        logIt("isLockoutActive:$isLockoutActive ")
        val satSetPointValue: Double =
            if (basicConfig.loopOutput == 0.0 || isLockoutActive) updateDefaultSetPoints(
                systemEquip,
                loopRunningDirection
            )
            else if (loopRunningDirection == TempDirection.COOLING) mapToSetPoint(
                satSetPointLimits.first.first,
                satSetPointLimits.first.second,
                basicConfig.loopOutput
            )
            else mapToSetPoint(
                satSetPointLimits.second.first,
                satSetPointLimits.second.second,
                basicConfig.loopOutput
            )

        updateSetPoint(
            systemEquip,
            String.format("%.1f", satSetPointValue).toDouble(),
            supplyAirflowTemperatureSetpoint,
            externalSpList,
            externalEquipId,
            haystack
        )
        logIt(
            "Single SP Direction $loopRunningDirection min (${satSetPointLimits.first}, ${satSetPointLimits.second})" + " setpoint $satSetPointValue"
        )
    }

}


fun calculateDSPSetPoints(
    systemEquip: Equip,
    loopOutput: Double,
    externalEquipId: String?,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    analogFanMultiplier: Double,
    coolingLoop: Double,
    conditioningMode: SystemMode,
    controller: SystemController
) {
    val profile = L.ccu().systemProfile
    if (!isConfigEnabled(systemEquip, staticPressureSetpointControlEnable)) {
        logIt("StaticPressureSp is disabled")
        return
    }
    val tempDirection = getTempDirection(controller)
    var fanLoop: Double

    if (profile is DabExternalAhu) {
        fanLoop = loopOutput * analogFanMultiplier.coerceAtMost(100.0)
        profile.systemFanLoopOp = fanLoop
    } else {

        val spSpMax = getTunerByDomainName(systemEquip, staticPressureSPMax)
        val spSpMin = getTunerByDomainName(systemEquip, staticPressureSPMin)

        if (isPurgeActive(systemEquip)) {
            profile.systemFanLoopOp = calculatePurseBaseLoop(
                spSpMin,
                spSpMax,
                conditioningMode,
                controller,
                analogFanMultiplier
            )
        } else if (isSystemAtCoolingSide(conditioningMode, controller)) {
            profile.systemFanLoopOp =
                ((profile.staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
        } else if (controller.getSystemState() == SystemController.State.HEATING) {
            profile.systemFanLoopOp =
                (controller.heatingSignal * analogFanMultiplier).toInt().toDouble()
        } else {
            profile.systemFanLoopOp = 0.0
        }
        profile.systemFanLoopOp = profile.systemFanLoopOp.coerceIn(0.0, 100.0)
        fanLoop = profile.systemFanLoopOp
        logIt(
            "SP(min,max) ($spSpMax,$spSpMin) staticPressure: ${profile.staticPressure} " + "Fan Loop $fanLoop"
        )
    }

    if (isFanLoopUpdateRequired(tempDirection, conditioningMode)) {
        fanLoop = checkOaoLoop(coolingLoop)
        logIt("After OAO economization  Fan loop $fanLoop")
    }

    if (AutoCommissioningUtil.isAutoCommissioningStarted()) {
        fanLoop = profile.getSystemLoopOutputValue(a75f.io.api.haystack.Tags.FAN)
        profile.systemFanLoopOp = fanLoop
    }
    logIt("System Fan loop $fanLoop")
    updateDspSetPoint(
        systemEquip,
        fanLoop,
        externalEquipId,
        haystack,
        externalSpList,
        analogFanMultiplier
    )
}

private fun updateDspSetPoint(
    systemEquip: Equip,
    fanLoop: Double,
    externalEquipId: String?,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    analogFanMultiplier: Double,
) {
    val min = Domain.getPointByDomain(systemEquip, systemStaticPressureMinimum)
    val max = Domain.getPointByDomain(systemEquip, systemStaticPressureMaximum)
    var dspSetPoint: Double =  String.format("%.1f",mapToSetPoint(min, max, fanLoop).coerceIn(0.0, max)).toDouble()
    dspSetPoint = (dspSetPoint * 100.0).roundToInt() / 100.0
    updatePointValue(systemEquip, ductStaticPressureSetpoint, dspSetPoint)
    updatePointValue(systemEquip, fanLoopOutput, fanLoop)
    CcuLog.d(TAG_BACNET, "---------updateDspSetPoint----------$externalEquipId")
    externalEquipId?.let {
        pushDuctStaticPressure(haystack, externalEquipId, dspSetPoint, externalSpList)
    }
    logIt("DSP Min: $min DSP Max: $max analogFanMultiplier: $analogFanMultiplier ductStaticPressureSetPoint: $dspSetPoint")
}

private fun calculatePurseBaseLoop(
    spSpMin: Double,
    spSpMax: Double,
    conditioningMode: SystemMode,
    controller: SystemController,
    analogFanMultiplier: Double
): Double {
    val profile = L.ccu().systemProfile
    val smartPurgeDabFanLoopOp = L.ccu().oaoProfile.oaoEquip.systemPurgeVavMinFanLoopOutput.readPriorityVal();
    val sPLoopOutput: Double =
        ((profile.staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
    return if ((isSystemAtCoolingSide(conditioningMode, controller))) {
        if (sPLoopOutput < (spSpMax - spSpMin) * smartPurgeDabFanLoopOp) {
            (spSpMax - spSpMin) * smartPurgeDabFanLoopOp
        } else {
            ((profile.staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
        }
    } else if (controller.getSystemState() == SystemController.State.HEATING) {
        (controller.heatingSignal * analogFanMultiplier).toInt().toDouble()
            .coerceAtLeast(smartPurgeDabFanLoopOp)
    } else {
        smartPurgeDabFanLoopOp
    }
}

private fun isSystemAtCoolingSide(
    conditioningMode: SystemMode, controller: SystemController
): Boolean {
    return (controller.getSystemState() == SystemController.State.COOLING && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO))
}

private fun isPurgeActive(systemEquip: Equip): Boolean {
    val epidemicMode = Domain.getPointByDomain(systemEquip, epidemicModeSystemState)
    val epidemicState = EpidemicState.values()[epidemicMode.toInt()]
    return ((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && L.ccu().oaoProfile != null)
}

fun isFanLoopUpdateRequired(tempDirection: TempDirection, conditioningMode: SystemMode): Boolean {
    return (tempDirection == TempDirection.COOLING && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO) && (L.ccu().oaoProfile != null) && (L.ccu().oaoProfile.isEconomizingAvailable))
}

fun checkOaoLoop(systemCoolingLoopOp: Double): Double {
    val economizingToMainCoolingLoopMap = L.ccu().oaoProfile.oaoEquip.economizingToMainCoolingLoopMap.readPriorityVal();
    logIt("OAO profile is available isEconomizingAvailable ? = ${L.ccu().oaoProfile.isEconomizingAvailable}")
    val updatedFanLoop =
        (systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap).coerceIn(0.0, 100.0)
    logIt("economizingToMainCoolingLoopMap = $economizingToMainCoolingLoopMap updatedFanLoop = $updatedFanLoop ")
    return updatedFanLoop
}

fun handleHumidityOperation(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancyMode: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    humidityHysteresis: Double,
    currentHumidity: Double,
    conditioningMode: SystemMode,
    baseConfig: BasicConfig
) {
    val currentStatus = Domain.getHisByDomain(systemEquip, humidifierEnable)
    var newStatus = 0.0

    if (baseConfig.loopOutput == 0.0 && (occupancyMode == Occupancy.UNOCCUPIED || conditioningMode == SystemMode.OFF
                || occupancyMode == Occupancy.VACATION)) {
        updatePointValue(systemEquip, humidifierEnable, 0.0)
        CcuLog.d(TAG_BACNET, "---------handleHumidityOperation----------1--$externalEquipId")
        externalEquipId?.let {
            pushHumidifierCmd(haystack, externalEquipId, 0.0, externalSpList)
        }
        return
    }

    val isHumidifierEnabled = isConfigEnabled(systemEquip, humidifierOperationEnable)
    if (isHumidifierEnabled) {
        val targetMinInsideHumidity =
            Domain.getPointPriorityValByDomain(systemEquip, systemtargetMinInsideHumidty)

        if (currentHumidity > 0 && currentHumidity < targetMinInsideHumidity) newStatus = 1.0
        else if (currentStatus > 0 && currentHumidity > (targetMinInsideHumidity + humidityHysteresis)) newStatus =
            0.0

        logIt(
            "currentHumidity $currentHumidity  humidityHysteresis: $humidityHysteresis targetMinInsideHumidity $targetMinInsideHumidity Humidifier $newStatus"
        )
    } else {
        logIt("Humidifier control is disabled")
    }

    if (currentStatus != newStatus) {
        updatePointValue(systemEquip, humidifierEnable, newStatus)
        CcuLog.d(TAG_BACNET, "---------handleHumidityOperation----------2---$externalEquipId")
        externalEquipId?.let {
            pushHumidifierCmd(haystack, externalEquipId, newStatus, externalSpList)
        }
    }
}

fun updateSystemStatusPoints(equipRef: String, newValue: String, domainName: String) {
    val currentStatus = Domain.readStrPointValueByDomainName(domainName, equipRef)
    if (!currentStatus.contentEquals(newValue)) {
        Domain.writeDefaultValByDomain(domainName, newValue, equipRef)
        if (domainName.contentEquals(equipStatusMessage)) Globals.getInstance().applicationContext.sendBroadcast(
            Intent(ScheduleUtil.ACTION_STATUS_CHANGE)
        )
    }

}

fun handleDeHumidityOperation(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancyMode: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    humidityHysteresis: Double,
    currentHumidity: Double,
    conditioningMode: SystemMode,
    baseConfig: BasicConfig
) {
    val currentStatus = Domain.getHisByDomain(systemEquip, dehumidifierEnable)
    var newStatus = 0.0

    if (baseConfig.loopOutput == 0.0 && (occupancyMode == Occupancy.UNOCCUPIED || conditioningMode == SystemMode.OFF
                || occupancyMode == Occupancy.VACATION)) {
        updatePointValue(systemEquip, dehumidifierEnable, 0.0)
        CcuLog.d(TAG_BACNET, "---------handleDeHumidityOperation----------1----$externalEquipId")
        externalEquipId?.let {
            pushDeHumidifierCmd(haystack, externalEquipId, 0.0, externalSpList)
        }
        return
    }

    val isDeHumidifierEnabled = isConfigEnabled(systemEquip, dehumidifierOperationEnable)
    if (isDeHumidifierEnabled) {
        val targetMaxInsideHumidity =
            Domain.getPointPriorityValByDomain(systemEquip, systemtargetMaxInsideHumidty)

        if (currentHumidity > 0 && currentHumidity > targetMaxInsideHumidity) newStatus = 1.0
        else if (currentStatus > 0 && currentHumidity < (targetMaxInsideHumidity - humidityHysteresis)) newStatus =
            0.0

        logIt(" targetMaxInsideHumidity: $targetMaxInsideHumidity DeHumidifier $newStatus")
    } else {
        logIt("DeHumidifier control is disabled")
    }

    if (currentStatus != newStatus) {
        updatePointValue(systemEquip, dehumidifierEnable, newStatus)
        CcuLog.d(TAG_BACNET, "---------handleDeHumidityOperation----------2----$externalEquipId")
        externalEquipId?.let {
            pushDeHumidifierCmd(haystack, externalEquipId, newStatus, externalSpList)
        }
    }
}

fun operateDamper(
    systemEquip: Equip,
    co2: Double,
    occupancyMode: Occupancy,
    externalEquipId: String?,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    conditioningMode: SystemMode,
    dabConfig: BasicConfig
) {
    val isDcvControlEnabled = isConfigEnabled(systemEquip, dcvDamperControlEnable)
    Domain.writeHisValByDomain(co2WeightedAverage, co2, systemEquip.id)
    if (!isDcvControlEnabled) {
        logIt("DCV control is disabled")
        return
    }

    val dcvMin = Domain.getPointByDomain(systemEquip, systemDCVDamperPosMinimum)
    val dcvMax = Domain.getPointByDomain(systemEquip, systemDCVDamperPosMaximum)
    val damperOpeningRate = Domain.getPointByDomain(systemEquip, systemCO2DamperOpeningRate)
    val co2Threshold = Domain.getPointByDomain(systemEquip, systemCO2Threshold)

    var damperOperationPercent = 0.0
    if (conditioningMode == SystemMode.OFF) damperOperationPercent = 0.0
    else if (shouldOperateDamper(
            co2,
            occupancyMode,
            co2Threshold,
            dabConfig
        )
    ) damperOperationPercent = calculateDamperOperationPercent(co2, co2Threshold, damperOpeningRate)
    else if (shouldResetDamper(co2, occupancyMode, co2Threshold)) damperOperationPercent = 0.0

    damperOperationPercent = minOf(damperOperationPercent, 100.0)

    val dcvSetPoint = mapToSetPoint(dcvMin, dcvMax, damperOperationPercent).coerceAtMost(dcvMax)

    updatePointValue(systemEquip, dcvLoopOutput, damperOperationPercent)
    updatePointValue(systemEquip, dcvDamperCalculatedSetpoint, dcvSetPoint)

    CcuLog.d(TAG_BACNET, "---------operateDamper----------$externalEquipId")

    externalEquipId?.let {
        pushDamperCmd(haystack, externalEquipId, dcvSetPoint, externalSpList)
    }

    logIt(
        "DCVDamperPosMinimum: $dcvMin DCVDamperPosMaximum: $dcvMax co2: $co2 DamperOpeningRate $damperOpeningRate CO2Threshold: $co2Threshold damperOperationPercent $damperOperationPercent dcvSetPoint: $dcvSetPoint"
    )
}

fun setOccupancyMode(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancy: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    operatingStatus: BasicConfig,
    conditioningMode: SystemMode
) {
    var occupancyMode = if (operatingStatus.loopOutput > 0) 1.0
    else when (occupancy) {
        Occupancy.UNOCCUPIED, Occupancy.VACATION -> 0.0
        else -> 1.0
    }
    if (conditioningMode == SystemMode.OFF) occupancyMode = 0.0

    if (isConfigEnabled(systemEquip, occupancyModeControl)) {
        logIt("Occupancy mode $occupancyMode")
        updatePointValue(systemEquip, systemOccupancyMode, occupancyMode)
        CcuLog.d(TAG_BACNET, "---------setOccupancyMode----------$externalEquipId")
        externalEquipId?.let {
            val operatingPointEnumString = haystack.readEntity("point and domainName == \"$systemOccupancyMode\"")["enum"].toString().lowercase()
            val operatingPointEnumStringForExternal = haystack.readEntity("$OCCUPANCY_MODE and equipRef == \"$externalEquipId\"")["enum"].toString().lowercase()
            val setPointValueFromModel = searchRealValueForOperatingMode(operatingPointEnumString, operatingPointEnumStringForExternal , occupancyMode.toInt().toString())
            pushOccupancyMode(haystack, it, setPointValueFromModel, externalSpList)
        }
    } else {
        logIt("OccupancyModeControlEnabled disabled")
    }
}

private fun shouldOperateDamper(
    sensorCO2: Double, mode: Occupancy, systemCO2Threshold: Double, dabConfig: BasicConfig
): Boolean =
    sensorCO2 > 0 && sensorCO2 > systemCO2Threshold && (dabConfig.loopOutput > 0 || (mode == Occupancy.OCCUPIED || mode == Occupancy.AUTOFORCEOCCUPIED || mode == Occupancy.AUTOAWAY || mode == Occupancy.EMERGENCY_CONDITIONING || mode == Occupancy.PRECONDITIONING || mode == Occupancy.FORCEDOCCUPIED))

private fun shouldResetDamper(
    sensorCO2: Double, mode: Occupancy, systemCO2Threshold: Double
): Boolean =
    mode == Occupancy.UNOCCUPIED || mode == Occupancy.VACATION || sensorCO2 < systemCO2Threshold

private fun calculateDamperOperationPercent(
    sensorCO2: Double, threshold: Double, openingRate: Double
): Double {
    val damperSp = (sensorCO2 - threshold) / openingRate
    return (damperSp * 100.0).roundToInt() / 100.0
}

fun getDualSetPointMinMax(
    equip: Equip
): Pair<Pair<Double, Double>, Pair<Double, Double>> {
    return Pair(
        Pair(
            Domain.getPointByDomain(equip, systemCoolingSATMaximum),
            Domain.getPointByDomain(equip, systemCoolingSATMinimum)
        ), Pair(
            Domain.getPointByDomain(equip, systemHeatingSATMinimum),
            Domain.getPointByDomain(equip, systemHeatingSATMaximum)
        )
    )
}

fun getConditioningMode(systemEquip: Equip) =
    SystemMode.values()[Domain.getPointPriorityValByDomain(systemEquip, conditioningMode).toInt()]

fun updatePointValue(equip: Equip, domainName: String, pointValue: Double) =
    Domain.writePointByDomain(equip, domainName, pointValue)

fun updatePointHistoryAndDefaultValue(domainName: String, value: Double) {
    Domain.writeDefaultValByDomain(domainName, value)
    Domain.writeHisValByDomain(domainName, value)
}

fun getConfiguration(profileDomain: String, profileType: ProfileType): ExternalAhuConfiguration {
    val systemEquip = Domain.getSystemEquipByDomainName(profileDomain)
    val config = ExternalAhuConfiguration(profileType.name)

    if (systemEquip == null) return config

    config.setPointControl.enabled = getConfigByDomainName(systemEquip, satSetpointControlEnable)
    config.dualSetPointControl.enabled =
        getConfigByDomainName(systemEquip, dualSetpointControlEnable)
    config.fanStaticSetPointControl.enabled =
        getConfigByDomainName(systemEquip, staticPressureSetpointControlEnable)
    config.dcvControl.enabled = getConfigByDomainName(systemEquip, dcvDamperControlEnable)
    config.occupancyMode.enabled = getConfigByDomainName(systemEquip, occupancyModeControl)
    config.humidifierControl.enabled = getConfigByDomainName(systemEquip, humidifierOperationEnable)
    config.dehumidifierControl.enabled =
        getConfigByDomainName(systemEquip, dehumidifierOperationEnable)
    config.heatingMinSp.currentVal = getConfigValue(systemHeatingSATMinimum, systemEquip)
    config.heatingMaxSp.currentVal = getConfigValue(systemHeatingSATMaximum, systemEquip)
    config.coolingMinSp.currentVal = getConfigValue(systemCoolingSATMinimum, systemEquip)
    config.coolingMaxSp.currentVal = getConfigValue(systemCoolingSATMaximum, systemEquip)
    config.fanMinSp.currentVal = getConfigValue(systemStaticPressureMinimum, systemEquip)
    config.fanMaxSp.currentVal = getConfigValue(systemStaticPressureMaximum, systemEquip)
    config.dcvMin.currentVal = getConfigValue(systemDCVDamperPosMinimum, systemEquip)
    config.dcvMax.currentVal = getConfigValue(systemDCVDamperPosMaximum, systemEquip)
    config.co2Threshold.currentVal = getConfigValue(systemCO2Threshold, systemEquip)
    config.damperOpeningRate.currentVal = getConfigValue(
        systemCO2DamperOpeningRate, systemEquip
    )
    config.co2Target.currentVal = getConfigValue(systemCO2Target, systemEquip)
    return config
}

fun getExternalEquipId(): String? {
    if(PreferenceUtil.getSelectedProfileWithAhu() == "bacnet"){
        val bacnetEquip =
            CCUHsApi.getInstance().readEntity("system and equip and bacnet and not emr and not btu")
        if (bacnetEquip.isNotEmpty()) {
            return bacnetEquip["id"].toString()
        }
    }else if(PreferenceUtil.getSelectedProfileWithAhu() == "modbus"){
        val modbusEquip =
            CCUHsApi.getInstance().readEntity("system and equip and modbus and not emr and not btu")
        if (modbusEquip.isNotEmpty()) {
            return modbusEquip["id"].toString()
        }
    }
    return null
}

fun addSystemEquip(
    config: ProfileConfiguration?,
    definition: SeventyFiveFProfileDirective?,
    systemProfile: SystemProfile
) {
    CcuLog.d(a75f.io.api.haystack.Tags.ADD_REMOVE_PROFILE, "ExternalAhuUtil----addSystemEquip----")
    CcuLog.i(L.TAG_CCU_SYSTEM, "Adding system equip " + definition?.name)
    val profileEquipBuilder = ProfileEquipBuilder(CCUHsApi.getInstance())
    val equipId = profileEquipBuilder.buildEquipAndPoints(
        config!!,
        definition!!,
        CCUHsApi.getInstance().site!!.id,
        CCUHsApi.getInstance().siteName + "-" + definition.name
    )
    CcuLog.d(a75f.io.api.haystack.Tags.ADD_REMOVE_PROFILE, "ExternalAhuUtil----updateAhuRef----")
    systemProfile.updateAhuRef(equipId)
    ControlMote(equipId)
    if (systemProfile is VavExternalAhu) {
        systemProfile.initTRSystem()
    }
}

fun getModbusPointValue(query: String): String {
    val equipId = getExternalEquipId()
    val point = CCUHsApi.getInstance().readEntity("$query and equipRef == \"$equipId\"")

    if (point.isEmpty()) {
        logIt("$query = point not found $equipId")
        return ""
    }

    val pointId = point["id"].toString()
    val value = CCUHsApi.getInstance().readHisValById(pointId)
    val unit = point["unit"]

    return " $value $unit"
}

fun getConfigValue(domainName: String, modelName: String): Boolean {
    return CCUHsApi.getInstance()
        .readDefaultVal(("point and domainName == \"$domainName\" and equipRef == \"${Domain.systemEquip.equipRef}\"")) > 0
}

fun getSetPoint(domainName: String): String {
    val point = Domain.readPoint(domainName)
    if (point.isEmpty()) return ""
    val unit = Objects.requireNonNull(point["unit"]).toString()
    val value = CCUHsApi.getInstance().readHisValById(point["id"].toString())
    return (" $value $unit")
}

fun getOperatingMode(equipName: String): String {
    val systemEquip = Domain.getSystemEquipByDomainName(equipName)
    val mode = getHisValueByDomain(systemEquip!!, operatingMode).toInt()
    return when (SystemController.State.values()[mode]) {
        SystemController.State.COOLING -> " Cooling"
        SystemController.State.HEATING -> " Heating"
        else -> {
            " Off"
        }
    }

}

fun getConfigValue(domainName: String, equip: Equip): Double =
    getDefaultValueByDomain(equip, domainName)

fun getConfigByDomainName(equip: Equip, domainName: String): Boolean {
    val config = getPointByDomain(equip, domainName)
    config?.let { return config.readDefaultVal() == 1.0 }
    return false
}

fun getDefaultValueByDomain(equip: Equip, domainName: String): Double {
    val config = getPointByDomain(equip, domainName)
    config?.let { return config.readDefaultVal() }
    return 0.0
}

fun getHisValueByDomain(equip: Equip, domainName: String): Double {
    val config = getPointByDomain(equip, domainName)
    config?.let { return config.readHisVal() }
    return 0.0
}

fun getPointByDomain(equip: Equip, domainName: String): Point? =
    equip.points.entries.find { (it.value.domainName.contentEquals(domainName)) }?.value

data class BasicConfig(
    var coolingLoop: Int,
    var heatingLoop: Int,
    var loopOutput: Double,
    val weightedAverageCO2: Double,
)

fun getTempDirection(controller: SystemController): TempDirection {
    return if (controller.systemState == SystemController.State.HEATING) {
        TempDirection.HEATING
    } else {
        TempDirection.COOLING
    }
}

enum class TempDirection {
    COOLING, HEATING
}

fun getPreviousOperatingModeFromDb(systemEquip: Equip, hayStack: CCUHsApi): Int {
    //When only heating or cooling is available, we should move in that direction
    val currentMode = Domain.getHisByDomain(systemEquip, conditioningMode)
    CcuLog.d(L.TAG_CCU_SYSTEM, "Current conditioning mode: $currentMode")
    if (currentMode.toInt() == SystemMode.HEATONLY.ordinal) {
        return SystemController.State.HEATING.ordinal
    } else if (currentMode.toInt() == SystemMode.COOLONLY.ordinal) {
        return SystemController.State.COOLING.ordinal
    }
    //Otherwise fall back to the previous operating mode
    val point = Domain.readPoint(operatingMode)
    val hisItems = hayStack.getHisItems(point["id"].toString(), 0, 2)
    if (hisItems.isEmpty()) {
        return SystemController.State.COOLING.ordinal
    }
    return if (hisItems.size > 1) {
        CcuLog.d(L.TAG_CCU_SYSTEM, "getPreviousOperatingMode: ${hisItems[0]} ${hisItems[1]}")
        maxOf(hisItems[1].`val`.toInt(), SystemController.State.COOLING.ordinal)
    } else {
        maxOf(hisItems[0].`val`.toInt(), SystemController.State.COOLING.ordinal)
    }
}

fun updateAutoCommissionOutput(config: BasicConfig) {
    try {
        val systemProfile = L.ccu().systemProfile
        if (AutoCommissioningUtil.isAutoCommissioningStarted()) {
            if (config.heatingLoop > 0) {
                systemProfile.writeSystemLoopOutputValue(
                    a75f.io.api.haystack.Tags.HEATING, config.heatingLoop.toDouble()
                )
                config.heatingLoop =
                    systemProfile.getSystemLoopOutputValue(a75f.io.api.haystack.Tags.HEATING)
                        .toInt()
            }
            if (config.coolingLoop > 0) {
                systemProfile.writeSystemLoopOutputValue(
                    a75f.io.api.haystack.Tags.COOLING, config.coolingLoop.toDouble()
                )
                config.coolingLoop =
                    systemProfile.getSystemLoopOutputValue(a75f.io.api.haystack.Tags.COOLING)
                        .toInt()
            }
            if (L.ccu().systemProfile.systemFanLoopOp > 0) {
                systemProfile.writeSystemLoopOutputValue(
                    a75f.io.api.haystack.Tags.FAN, systemProfile.systemFanLoopOp
                )
            }
        }
    } catch (e: Exception) {
        logIt("AutoCommissioning is failed for VAV External ${e.message} ")
        e.printStackTrace()
    }

}


private fun getChildEquipMap(equipRef: String): ArrayList<HashMap<Any, Any>> {
    return CCUHsApi.getInstance()
        .readAllEntities("equip and modbus and equipRef== \"$equipRef\"")
}
fun logIt(msg: String) {
    CcuLog.i(L.TAG_CCU_SYSTEM, msg)
}

private fun generateWriteObject(
    configMap: Map<String, String?>,
    objectId: Int,
    selectedValue: String,
    objectType: String,
    priority: String
): BacnetWriteRequest {
    var macAddress: String? = ""
    if (configMap[MAC_ADDRESS] != null) {
        macAddress = configMap[MAC_ADDRESS]
    }
    //OBJECT_MULTI_STATE_VALUE
    val destinationMultiRead = Objects.requireNonNull(
        configMap[DESTINATION_IP]
    )?.let {
        Objects.requireNonNull(
            configMap[DESTINATION_PORT]
        )?.let { it1 ->
            Objects.requireNonNull(
                configMap[DEVICE_ID]
            )?.let { it2 ->
                Objects.requireNonNull(
                    configMap[DEVICE_NETWORK]
                )?.let { it3 ->
                    DestinationMultiRead(
                        it,
                        it1,
                        it2,
                        it3,
                        macAddress!!
                    )
                }
            }
        }
    }
    val dataType: Int
    val selectedValueAsPerType: String
    if (BacNetConstants.ObjectType.valueOf(objectType).value == 2) {
        dataType = BacNetConstants.DataTypes.BACNET_DT_REAL.ordinal + 1
        selectedValueAsPerType = selectedValue
    } else if (BacNetConstants.ObjectType.valueOf(objectType).value == 5) {
        dataType = BacNetConstants.DataTypes.BACNET_DT_ENUM.ordinal + 1
        selectedValueAsPerType = selectedValue //String.valueOf(Integer.parseInt(selectedValue)+1);
    } else {
        dataType = BacNetConstants.DataTypes.BACNET_DT_UNSIGNED.ordinal + 1
        selectedValueAsPerType = (selectedValue.toInt() + 1).toString()
    }
    val objectIdentifierBacNet = ObjectIdentifierBacNet(
        BacNetConstants.ObjectType.valueOf(objectType).value,
        objectId.toString()
    )
    val propertyValueBacNet =
        PropertyValueBacNet(dataType, selectedValueAsPerType)
    val writeRequest = WriteRequest(
        objectIdentifierBacNet, propertyValueBacNet, priority,
        BacNetConstants.PropertyType.valueOf("PROP_PRESENT_VALUE").value, null
    )
    return BacnetWriteRequest(destinationMultiRead!!, writeRequest)
}
fun doMakeRequest(configMap: Map<String, String?>, objectId : Int, newValue : String,
                          objectType: String, priority: String, pointId: String){
    val bacnetServicesUtils = BacnetServicesUtils()
    val serverIpAddress: String? = bacnetServicesUtils.getServerIpAddress()
    if(serverIpAddress != null){
        CcuLog.d(TAG_BACNET, "--doMakeRequest-->$objectId<--newValue-->$newValue-->objectType-->$objectType<-pointId->$pointId<---serverIpAddress-->$serverIpAddress")
        bacnetServicesUtils.sendWriteRequest(generateWriteObject(configMap, objectId, newValue,
            objectType, priority),
            serverIpAddress, remotePointUpdateInterface, newValue, pointId, false)
    }
}

private val remotePointUpdateInterface =
    RemotePointUpdateInterface { message: String?, id: String?, value: String ->
        CcuLog.d(TAG_BACNET, "--updateMessage::>> $message")
        //CCUHsApi.getInstance().writeDefaultValById(id, value.toDouble())
        //CCUHsApi.getInstance().writeHisValById(id, value.toDouble())
    }
