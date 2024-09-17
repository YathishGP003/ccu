package a75f.io.logic.bo.building.system.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.AdvancedHybridSystemEquip
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.domain.util.ModelLoader.getDabAdvancedAhuCmModelV2
import a75f.io.domain.util.ModelLoader.getDabAdvancedAhuConnectModelV2
import a75f.io.domain.util.ModelLoader.getVavAdvancedAhuCmModelV2
import a75f.io.domain.util.ModelLoader.getVavAdvancedAhuConnectModelV2
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import android.annotation.SuppressLint
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import java.util.ArrayList
import kotlin.system.measureTimeMillis

/**
 * Created by Manjunath K on 23-05-2024.
 */

/**
 * This function is used to get the modulated output based on the loop output
 */
fun getModulatedOutput(loopOutput: Double, min: Double, max: Double) = (((max - min) * (loopOutput / 100.0)) + min)

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
fun getAllConnectDevice(): ArrayList<HashMap<Any, Any>> {
    return CCUHsApi.getInstance().readAllEntities("domainName == \"" + ModelNames.connectModuleDevice + "\"")
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

fun getConnectEquip(): ConnectModuleEquip {
    return if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        (Domain.systemEquip as VavAdvancedHybridSystemEquip).connectEquip1
    } else {
        (Domain.systemEquip as DabAdvancedHybridSystemEquip).connectEquip1
    }
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
    return CCUHsApi.getInstance().readEntity("system and equip and not modbus and not connectModule")
}

fun getAdvanceAhuModels():  Pair<SeventyFiveFProfileDirective, SeventyFiveFProfileDirective> {
    return when (L.ccu().systemProfile) {
        is DabAdvancedAhu -> Pair(getDabAdvancedAhuCmModelV2() as SeventyFiveFProfileDirective, getDabAdvancedAhuConnectModelV2() as SeventyFiveFProfileDirective)
        else -> Pair(getVavAdvancedAhuCmModelV2() as SeventyFiveFProfileDirective, getVavAdvancedAhuConnectModelV2() as SeventyFiveFProfileDirective)
    }
}

