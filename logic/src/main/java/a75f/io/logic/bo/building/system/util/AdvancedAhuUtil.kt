package a75f.io.logic.bo.building.system.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.AdvancedHybridSystemEquip
import a75f.io.domain.equips.ConditioningStages
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.domain.util.CommonQueries
import a75f.io.domain.util.ModelLoader.getDabAdvancedAhuCmModelV2
import a75f.io.domain.util.ModelLoader.getDabAdvancedAhuConnectModelV2
import a75f.io.domain.util.ModelLoader.getVavAdvancedAhuCmModelV2
import a75f.io.domain.util.ModelLoader.getVavAdvancedAhuConnectModelV2
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.AdvAhuEconAlgoHandler
import a75f.io.logic.bo.building.system.AdvancedAhuAlgoHandler
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.dab.DabSystemController
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.building.system.vav.VavSystemController
import android.annotation.SuppressLint
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import kotlin.system.measureTimeMillis

/**
 * Created by Manjunath K on 23-05-2024.
 */

/**
 * This function is used to get the modulated output based on the loop output
 */
fun getModulatedOutput(loopOutput: Double, min: Double, max: Double) = (((max - min) * (loopOutput / 100.0)) + min)

/**
 * The function follows these rules:
 * 1. If `loopOutput` is less than `economizingToMainCoolingLoopMap`, return `min`.
 * 2. If `economizingToMainCoolingLoopMap` is 100.0 or greater, return 100.0.
 * 3. Otherwise, calculate the output using the formula:
 *    `(((max - min) * ((loopOutput - economizingToMainCoolingLoopMap) / (100.0 - economizingToMainCoolingLoopMap))) + min`
 *
 * @param loopOutput The current output value from the loop.
 * @param min The minimum allowable output value.
 * @param max The maximum allowable output value.
 * @param economizingToMainCoolingLoopMap The threshold value used to determine the economizer behavior.
 *                                        If `loopOutput` is below this threshold, the function returns `min`.
 *                                        If this threshold is 100.0 or above, the function returns 100.0.
 * @return The modulated output value, constrained between `min` and `max`.
 *
 */
fun getModulatedOutputDuringEcon(
    loopOutput: Double,
    min: Double,
    max: Double,
    economizingToMainCoolingLoopMap: Double,
): Double {
    if(loopOutput < economizingToMainCoolingLoopMap) {
        return min
    }
    if(economizingToMainCoolingLoopMap >= 100.0) {
        return 100.0
    }
    return (((max - min) * ((loopOutput - economizingToMainCoolingLoopMap) / (100.0 - economizingToMainCoolingLoopMap))) + min)
}

/**
 * This function is get the mid point of min max
 */
fun getComposeMidPoint(minMax: Pair<Double, Double>) = (((minMax.first + minMax.second) / 2).coerceIn(0.0, 10.0))

data class AhuTuners(var relayAActivationHysteresis: Double, var relayDeactivationHysteresis: Double, var humidityHysteresis: Double)

data class AhuSettings(
    var systemEquip: AdvancedHybridSystemEquip,
    var connectEquip1: ConnectModuleEquip,
    var conditioningMode: SystemMode,
    var isMechanicalCoolingAvailable: Boolean,
    var isMechanicalHeatingAvailable: Boolean,
    var isEmergencyShutoffActive: Boolean,
    var isEconomizationAvailable: Boolean,
    var systemState: SystemController.State
)

enum class DuctPressureSensorSource {
    DUCT_STATIC_PRESSURE_SENSOR_1,
    DUCT_STATIC_PRESSURE_SENSOR_2,
    DUCT_STATIC_PRESSURE_SENSOR_3,
    AVERAGE_PRESSURE,
    MIN_PRESSURE,
    MAX_PRESSURE
}

data class UserIntentConfig(
        var isSatHeatingAvailable: Boolean = false,
        var isSatCoolingAvailable: Boolean = false,
        var isPressureControlAvailable: Boolean = false,
        var isCo2DamperControlAvailable: Boolean = false,
)

data class StagesCounts(
    var loadCoolingStages: CalibratedPoint = CalibratedPoint("loadCoolingStages", "", 0.0),
    var loadHeatingStages: CalibratedPoint = CalibratedPoint("loadHeatingStages", "", 0.0),
    var loadFanStages: CalibratedPoint = CalibratedPoint("loadFanStages", "", 0.0),
    var compressorStages: CalibratedPoint = CalibratedPoint("compressorStages", "", 0.0),
    var satCoolingStages: CalibratedPoint = CalibratedPoint("satCoolingStages", "", 0.0),
    var satHeatingStages: CalibratedPoint = CalibratedPoint("satHeatingStages", "", 0.0),
    var pressureFanStages: CalibratedPoint = CalibratedPoint("pressureFanStages", "", 0.0),
) {
    fun resetCounts() {
        loadCoolingStages.data = 0.0
        loadHeatingStages.data = 0.0
        loadFanStages.data = 0.0
        satCoolingStages.data = 0.0
        satHeatingStages.data = 0.0
        pressureFanStages.data = 0.0
        compressorStages.data = 0.0
    }
}

@SuppressLint("DefaultLocale")
fun roundOff(value: Double): Double {
    return String.format("%.2f", value).toDouble()
}

fun readEntity(domainName: String): HashMap<Any, Any> {
    return CCUHsApi.getInstance().readEntity("domainName == \"$domainName\"")
}

fun getConnectDevice(): HashMap<Any, Any> {
    return readEntity(ModelNames.connectModuleDevice)
}

fun getDis(name: String): String = "${CCUHsApi.getInstance().siteName}-$name"

fun isConnectModuleAvailable(): Boolean = getConnectDevice().isNotEmpty()

fun needToUpdateConditioningMode(systemMode: SystemMode, isCoolingAvailable: Boolean, isHeatingAvailable: Boolean): Boolean {
    return when (systemMode) {
        SystemMode.AUTO -> !(isCoolingAvailable && isHeatingAvailable)
        SystemMode.COOLONLY -> !isCoolingAvailable
        SystemMode.HEATONLY -> !isHeatingAvailable
        else -> true
    }
}

fun getAdvancedAhuSystemEquip(): AdvancedHybridSystemEquip {
    return if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        (Domain.systemEquip as VavAdvancedHybridSystemEquip).cmEquip
    } else {
        (Domain.systemEquip as DabAdvancedHybridSystemEquip).cmEquip
    }
}

fun getConnectEquip(): ConnectModuleEquip? {
    if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        return (Domain.systemEquip as VavAdvancedHybridSystemEquip).connectEquip1
    }

    if (Domain.systemEquip is DabAdvancedHybridSystemEquip) {
        return (Domain.systemEquip as DabAdvancedHybridSystemEquip).connectEquip1
    }
    return null
}

fun getConnectModuleDomain(): String? {
    return when(L.ccu().systemProfile) {
        is VavAdvancedAhu -> ModelNames.vavAdvancedHybridAhuV2_connectModule
        is DabAdvancedAhu -> ModelNames.dabAdvancedHybridAhuV2_connectModule
        else -> { null }
    }
}

fun getSystemDomain(): String? {
    return when(L.ccu().systemProfile) {
        is VavAdvancedAhu -> ModelNames.vavAdvancedHybridAhuV2
        is DabAdvancedAhu -> ModelNames.dabAdvancedHybridAhuV2
        else -> { null }
    }
}

fun deleteSystemConnectModule() {
    deleteSystemConnectModule(ModelNames.vavAdvancedHybridAhuV2_connectModule)
    deleteSystemConnectModule(ModelNames.dabAdvancedHybridAhuV2_connectModule)
}

fun deleteCurrentSystemProfile() {
    val deleteTime = measureTimeMillis {
        val systemEquip = getCurrentSystemEquip()
        if (systemEquip.isNotEmpty()) {
            CCUHsApi.getInstance().deleteEntityTree(systemEquip[Tags.ID].toString())
        }
    }
    CcuLog.i(L.TAG_CCU_DOMAIN, "Time taken to delete entities: $deleteTime")
}

fun deleteSystemConnectModule(modelName: String) {
    val hayStack = CCUHsApi.getInstance()

    val connectEquip = readEntity(modelName)
    if (connectEquip.isNotEmpty()) {
        hayStack.deleteEntityTree(connectEquip[Tags.ID].toString())
    }

    val connectDevice = getConnectDevice()
    if (connectDevice.isNotEmpty()) {
        hayStack.deleteEntityTree(connectDevice[Tags.ID].toString())
    }
}

fun getVavCmEquip(): HashMap<Any, Any> = readEntity(ModelNames.vavAdvancedHybridAhuV2)

fun getDabCmEquip(): HashMap<Any, Any> = readEntity(ModelNames.dabAdvancedHybridAhuV2)

fun getVavConnectEquip(): HashMap<Any, Any> = readEntity(ModelNames.vavAdvancedHybridAhuV2_connectModule)

fun getDabConnectEquip(): HashMap<Any, Any> = readEntity(ModelNames.dabAdvancedHybridAhuV2_connectModule)

fun getCurrentSystemEquip(): HashMap<Any, Any> {
    return CCUHsApi.getInstance().readEntity(CommonQueries.SYSTEM_PROFILE)
}

fun getAdvanceAhuModels():  Pair<SeventyFiveFProfileDirective, SeventyFiveFProfileDirective> {
    return when (L.ccu().systemProfile) {
        is DabAdvancedAhu -> Pair(getDabAdvancedAhuCmModelV2() as SeventyFiveFProfileDirective, getDabAdvancedAhuConnectModelV2() as SeventyFiveFProfileDirective)
        else -> Pair(getVavAdvancedAhuCmModelV2() as SeventyFiveFProfileDirective, getVavAdvancedAhuConnectModelV2() as SeventyFiveFProfileDirective)
    }
}

private fun needToShowFreeCooling(): Boolean {
    return (!L.ccu().systemProfile.isSystemOccupied && L.ccu().systemProfile.isLockoutActiveDuringUnoccupied)
}

fun isConnectModuleExist(): Boolean {
    if(L.ccu().systemProfile is VavAdvancedAhu)
        return getVavConnectEquip().isNotEmpty()
    if(L.ccu().systemProfile is DabAdvancedAhu)
        return getDabConnectEquip().isNotEmpty()
    return false
}

 fun getConnectModuleSystemStatus(
     connectEquip: ConnectModuleEquip, advancedAhuImpl: AdvancedAhuAlgoHandler,
     coolingLoopOutput: Double, analogControlsEnabled: Set<AdvancedAhuAnalogOutAssociationType>
 ): String {
    val systemEquip = getAdvancedAhuSystemEquip()
    if (advancedAhuImpl.isEmergencyShutOffEnabledAndActive(systemEquip = systemEquip, connectEquip1 = connectEquip)) {
        return "Emergency Shut Off mode is active"
    }
     val systemStages = connectEquip.conditioningStages
     val connectModuleSystemStatus = StringBuilder().apply {
         append(if (systemStages.loadFanStage1.readHisVal() > 0) "1" else "")
         append(if (systemStages.loadFanStage2.readHisVal() > 0) ",2" else "")
         append(if (systemStages.loadFanStage3.readHisVal() > 0) ",3" else "")
         append(if (systemStages.loadFanStage4.readHisVal() > 0) ",4" else "")
         append(if (systemStages.loadFanStage5.readHisVal() > 0) ",5" else "")
     }

    if (connectModuleSystemStatus.isNotEmpty()) {
        if (connectModuleSystemStatus[0] == ',') {
            connectModuleSystemStatus.deleteCharAt(0)
        }
        connectModuleSystemStatus.insert(0, "Fan Stage ")
        connectModuleSystemStatus.append(" ON ")
    }
     val systemState = if(getSystemDomain() == ModelNames.dabAdvancedHybridAhuV2) {
         DabSystemController.getInstance().systemState
     } else {
         VavSystemController.getInstance().systemState
     }

     val coolingStatus = StringBuilder().apply {
         if (isCoolingActive(systemStages) || systemState == SystemController.State.COOLING && isCompressorActive(systemStages)) {
             append(if (systemStages.loadCoolingStage1.readHisVal() > 0 || systemStages.compressorStage1.readHisVal() > 0) "1" else "")
             append(if (systemStages.loadCoolingStage2.readHisVal() > 0 || systemStages.compressorStage2.readHisVal() > 0) ",2" else "")
             append(if (systemStages.loadCoolingStage3.readHisVal() > 0 || systemStages.compressorStage3.readHisVal() > 0) ",3" else "")
             append(if (systemStages.loadCoolingStage4.readHisVal() > 0 || systemStages.compressorStage4.readHisVal() > 0) ",4" else "")
             append(if (systemStages.loadCoolingStage5.readHisVal() > 0 || systemStages.compressorStage5.readHisVal() > 0) ",5" else "")
         }
     }
     if (coolingStatus.isNotEmpty()) {
         if (coolingStatus[0] == ',') {
             coolingStatus.deleteCharAt(0)
         }
         coolingStatus.insert(0, "Cooling Stage ")
         coolingStatus.append(" ON ")
     }

     val heatingStatus = StringBuilder().apply {
         if (isHeatingActive(systemStages) || systemState == SystemController.State.HEATING && isCompressorActive(systemStages)) {
             append(if (systemStages.loadHeatingStage1.readHisVal() > 0 || systemStages.compressorStage1.readHisVal() > 0) "1" else "")
             append(if (systemStages.loadHeatingStage2.readHisVal() > 0 || systemStages.compressorStage2.readHisVal() > 0) ",2" else "")
             append(if (systemStages.loadHeatingStage3.readHisVal() > 0 || systemStages.compressorStage3.readHisVal() > 0) ",3" else "")
             append(if (systemStages.loadHeatingStage4.readHisVal() > 0 || systemStages.compressorStage4.readHisVal() > 0) ",4" else "")
             append(if (systemStages.loadHeatingStage5.readHisVal() > 0 || systemStages.compressorStage5.readHisVal() > 0) ",5" else "")
         }
     }
    if (heatingStatus.isNotEmpty()) {
        if (heatingStatus[0] == ',') {
            heatingStatus.deleteCharAt(0)
        }
        heatingStatus.insert(0, "Heating Stage ")
        heatingStatus.append(" ON ")
    }
    val isEconActive = L.ccu().systemProfile.coolingLoopOp > 0 &&(AdvAhuEconAlgoHandler.isFreeCoolingOn() || (L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable))
    if (isEconActive && !needToShowFreeCooling()) {
        connectModuleSystemStatus.insert(0, "Free Cooling Used | ")
    }

    val humidifierStatus = getHumidifierStatus(connectEquip1 = connectEquip)
    val dehumidifierStatus = getDehumidifierStatus(connectEquip1 = connectEquip)

    val analogStatus = StringBuilder()
    if ((analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_FAN))
        && connectEquip.fanLoopOutput.readHisVal() > 0 && !(!L.ccu().systemProfile.isSystemOccupied && L.ccu().systemProfile.isLockoutActiveDuringUnoccupied)) {
        analogStatus.append("| Fan ON ")
    }

     if (isOaEconActive() || isConnectEconActive()) {
         // Only if te systemCoolingLoopOp greater than economizingToMainCoolingLoopMap, update the analog cooling status
         var economizingToMainCoolingLoopMap = 30.0
         if (isOaEconActive()) {
             economizingToMainCoolingLoopMap = L.ccu().oaoProfile.oaoEquip.economizingToMainCoolingLoopMap.readPriorityVal()
         }
         if (isConnectEconActive()) {
             economizingToMainCoolingLoopMap = connectEquip.economizingToMainCoolingLoopMap.readPriorityVal()
         }

         if ((systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) && ((analogControlsEnabled.contains(
                 AdvancedAhuAnalogOutAssociationType.LOAD_COOLING
             ))) && connectEquip.coolingLoopOutput.readHisVal() > 0 && coolingLoopOutput >= economizingToMainCoolingLoopMap
         ) {
             analogStatus.append("| Cooling ON ")
         }
     } else {
         if ((systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) && ((analogControlsEnabled.contains(
                 AdvancedAhuAnalogOutAssociationType.LOAD_COOLING
             ))) && connectEquip.coolingLoopOutput.readHisVal() > 0
         ) {
             analogStatus.append("| Cooling ON ")
         }
     }

    if ((systemEquip.mechanicalHeatingAvailable.readHisVal() > 0) && ((analogControlsEnabled.contains(
            AdvancedAhuAnalogOutAssociationType.LOAD_HEATING
        )) && connectEquip.heatingLoopOutput.readHisVal() > 0)
    ) {
        analogStatus.append("| Heating ON ")
    }
    if (analogStatus.isNotEmpty()) {
        analogStatus.insert(0, " | Analog ")
    }
    connectModuleSystemStatus.append(coolingStatus)
        .append(heatingStatus)
        .append(analogStatus)

    return if (connectModuleSystemStatus.toString() == "") "System OFF$humidifierStatus$dehumidifierStatus" else connectModuleSystemStatus.toString() + humidifierStatus + dehumidifierStatus
}

 fun getHumidifierStatus(
    systemEquip: AdvancedHybridSystemEquip? = null,
    connectEquip1: ConnectModuleEquip? = null
): String {
    if (systemEquip != null && systemEquip.conditioningStages.humidifierEnable.pointExists()) {
        return if (systemEquip.conditioningStages.humidifierEnable.readHisVal() > 0) {
            " | Humidifier ON "

        } else " | Humidifier OFF "
    }
    if (connectEquip1 != null && connectEquip1.conditioningStages.humidifierEnable.pointExists()) {
        return if (connectEquip1.conditioningStages.humidifierEnable.readHisVal() > 0) {
            " | Humidifier ON "
        } else " | Humidifier OFF "
    }
    // just for the case when dehumidifierEnable point is not available
    return ""
}

 fun getDehumidifierStatus(
    systemEquip: AdvancedHybridSystemEquip? = null,
    connectEquip1: ConnectModuleEquip? = null
): String {
    if (systemEquip != null && systemEquip.conditioningStages.dehumidifierEnable.pointExists()) {
        return if (systemEquip.conditioningStages.dehumidifierEnable.readHisVal() > 0) {
            " | DeHumidifier ON "

        } else " | DeHumidifier OFF "
    }
    if (connectEquip1 != null && connectEquip1.conditioningStages.dehumidifierEnable.pointExists()) {
        return if (connectEquip1.conditioningStages.dehumidifierEnable.readHisVal() > 0) {
            " | DeHumidifier ON "
        } else " | DeHumidifier OFF "
    }
    // just for the case when humidifierEnable point is not available
    return ""
}

private fun isCoolingActive(systemStages: ConditioningStages): Boolean {
    return systemStages.loadCoolingStage1.readHisVal() > 0 ||
            systemStages.loadCoolingStage2.readHisVal() > 0 ||
            systemStages.loadCoolingStage3.readHisVal() > 0 ||
            systemStages.loadCoolingStage4.readHisVal() > 0 ||
            systemStages.loadCoolingStage5.readHisVal() > 0
}
private fun isHeatingActive(systemStages: ConditioningStages): Boolean {
    return systemStages.loadHeatingStage1.readHisVal() > 0 ||
            systemStages.loadHeatingStage2.readHisVal() > 0 ||
            systemStages.loadHeatingStage3.readHisVal() > 0 ||
            systemStages.loadHeatingStage4.readHisVal() > 0 ||
            systemStages.loadHeatingStage5.readHisVal() > 0
}

private fun isCompressorActive(systemStages: ConditioningStages): Boolean {
    return systemStages.compressorStage1.readHisVal() > 0 ||
            systemStages.compressorStage2.readHisVal() > 0 ||
            systemStages.compressorStage3.readHisVal() > 0 ||
            systemStages.compressorStage4.readHisVal() > 0 ||
            systemStages.compressorStage5.readHisVal() > 0
}

 fun getAdvAhuConnectModule(
    nodeAddress: String,
    hayStack: CCUHsApi = CCUHsApi.getInstance()): HashMap<Any, Any> {
    val connectModuleEquip = hayStack.readEntity("domainName == \"${ModelNames.dabAdvancedHybridAhuV2_connectModule}\" or domainName == \"${ModelNames.vavAdvancedHybridAhuV2_connectModule}\" " +
            "and addr == \"$nodeAddress\"")
    return connectModuleEquip
 }

fun isOaEconActive(): Boolean {
    return (L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable)
}

fun isConnectEconActive() = AdvAhuEconAlgoHandler.isFreeCoolingOn()

fun getOaoEconToMainCoolingLoop(): Double {
    return L.ccu().oaoProfile.oaoEquip.economizingToMainCoolingLoopMap.readPriorityVal()
}

fun getConnectEconToMainCoolingLoop(connectEquip: ConnectModuleEquip): Double {
    return connectEquip.economizingToMainCoolingLoopMap.readPriorityVal()
}