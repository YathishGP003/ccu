package a75f.io.device.daikin

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.Occupancy
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.vav.VavIERtu
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.jobs.ScheduleProcessJob
import a75f.io.logic.tuners.TunerUtil


fun getIEUrl(hayStack : CCUHsApi): String? {
    return hayStack.readDefaultStrVal("point and system and config and ie and ipAddress")
}

fun fahrenheitToCelsius(T: Double): Double {
    return (T - 32) * 5 / 9
}

fun inchToPascal(P: Double): Double {
    return P / 0.0040146
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
    return systemProfile.getCmdSignal("fan")
}

fun getMeanHumidityTarget() : Double {
    return (TunerUtil.readSystemUserIntentVal("target and max and inside and humidity") +
            TunerUtil.readSystemUserIntentVal("target and min and inside and humidity"))/2
}

fun isSystemOccupied(systemProfile: VavIERtu) : Boolean {
    return systemProfile.systemController.getSystemState() != SystemController.State.OFF
            && (systemProfile.systemCoolingLoopOp > 0 || systemProfile.systemHeatingLoopOp > 0)
}