package a75f.io.device.daikin

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.vav.VavIERtu
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.tuners.TunerUtil


fun getIEUrl(hayStack : CCUHsApi): String? {
    return hayStack.readDefaultStrVal("point and system and config and ie and ipAddress")
}

fun fahrenheitToCelsius(T: Double): Double {
    return CCUUtils.roundToTwoDecimal((T - 32) * 5/9)
}

fun celsiusToFahrenheit(T: Double): Double {
    return CCUUtils.roundToTwoDecimal((T * 9/5) + 32)
}

fun inchToPascal(P: Double): Double {
    return CCUUtils.roundToTwoDecimal(P / 0.0040146)
}

fun isConditioningRequired(hayStack : CCUHsApi) : Boolean  {
    return getSystemLoopOp(Tags.COOLING, hayStack) > 0 || getSystemLoopOp(Tags.HEATING, hayStack) > 0
}

fun getSystemLoopOp(loopType: String, hayStack : CCUHsApi) : Double {
    return hayStack.readHisValByQuery("point and system and loop and output and his and $loopType")
}

fun isMultiZoneEnabled(hayStack : CCUHsApi) : Boolean {
    return hayStack.readDefaultVal("point and system and config and multiZone") > 0
}

fun getDuctStaticPressureTarget(systemProfile: VavIERtu) : Double {
    return systemProfile.getCmdSignal("staticPressure")
}

fun getFanSpeedTarget(systemProfile: VavIERtu) : Double {
    return systemProfile.getCmdSignal("fan")
}

fun getHumidityTarget() : Double {
    return TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")
}

fun getIEMacAddress(hayStack : CCUHsApi) : String {
    return hayStack.readDefaultStrVal("system and point and ie and macAddress")
}
/**
 * Daikin IE requires to tbe set 'occupied' if 75F system is occupied
 * or when there is conditioning during 'unoccupied' time.
 */
fun isSystemOccupied(systemProfile: VavIERtu, hayStack: CCUHsApi) : Boolean {
    return (systemProfile.systemController.getSystemState() != SystemController.State.OFF
            && (systemProfile.isSystemOccupied || systemProfile.isReheatActive(hayStack)))
            || systemProfile.systemCoolingLoopOp > 10
            || systemProfile.systemHeatingLoopOp > 10
}