package a75f.io.logic.preconfig

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.CommonQueries
import a75f.io.domain.util.TunerUtil.updateSystemTunerVal
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfileConfiguration
import a75f.io.logic.tuners.TunerUtil

/**
 * TODO - Refactor
 * Duplicate of code from BypassViewModel.
 */
fun overrideTunersForBypassDamper(hayStack: CCUHsApi) {
    updateSystemTunerVal("dab and target and cumulative and damper", 20.0, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("dab and pgain and not reheat", 0.7, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("dab and igain and not reheat", 0.3, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("dab and pspread and not reheat", 1.5, "Bypass Damper Added", hayStack)

    updateSystemTunerVal("dualDuct and target and cumulative and damper", 20.0, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("dualDuct and pgain and not reheat", 0.7, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("dualDuct and igain and not reheat", 0.3, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("dualDuct and pspread and not reheat", 1.5, "Bypass Damper Added", hayStack)

    updateSystemTunerVal("vav and target and cumulative and damper", 20.0, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("vav and pgain and not airflow and not air", 0.7, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("vav and igain and not airflow and not air", 0.3, "Bypass Damper Added", hayStack)
    updateSystemTunerVal("vav and pspread and not airflow and not air", 1.5, "Bypass Damper Added", hayStack)


    CCUHsApi.getInstance().readEntity(CommonQueries.SYSTEM_PROFILE)["id"]?.let { sysEquipId ->
        val childEquips = HSUtil.getEquipsWithAhuRefOnThisCcu(sysEquipId.toString())
        val childEquipsIterator = childEquips.iterator()
        while(childEquipsIterator.hasNext()) {
            val eq = childEquipsIterator.next()

            hayStack.readEntity("point and config and damper and cooling and min and not analog1 and not analog2 and equipRef == \"" + eq.id + "\"")["id"]?.let { minCoolingDamperPosPointId ->
                hayStack.writePointForCcuUser(minCoolingDamperPosPointId.toString(), 8, 10.0, 0, "Bypass Damper Added")
                hayStack.writeHisValById(minCoolingDamperPosPointId.toString(), 10.0)
            }

            hayStack.readEntity("point and config and damper and heating and min and not analog1 and not analog2 and equipRef == \"" + eq.id + "\"")["id"]?.let { minHeatingDamperPosPointId ->
                hayStack.writePointForCcuUser(minHeatingDamperPosPointId.toString(), 8, 10.0, 0, "Bypass Damper Added")
                hayStack.writeHisValById(minHeatingDamperPosPointId.toString(), 10.0)
            }

            if (eq.markers.contains("dualDuct")) {
                val systemPGain = TunerUtil.readTunerValByQuery("system and dab and pgain and not reheat and not default")
                hayStack.readEntity("point and tuner and dualDuct and pgain and not reheat and equipRef == \"" + eq.id + "\"")["id"]?.let { pGainPointId ->
                    hayStack.writePointForCcuUser(pGainPointId.toString(), 14, systemPGain, 0, "Bypass Damper Added")
                }

                val systemIGain = TunerUtil.readTunerValByQuery("system and dab and igain and not reheat and not default")
                val iGainPoint = hayStack.readEntity("point and tuner and dualDuct and igain and not reheat and equipRef == \"" + eq.id + "\"")
                val iGainPointId = iGainPoint["id"].toString()
                hayStack.writePointForCcuUser(iGainPointId, 14, systemIGain, 0, "Bypass Damper Added")

                val systemPSpread = TunerUtil.readTunerValByQuery("system and dab and pspread and not reheat and not default")
                hayStack.readEntity("point and tuner and dualDuct and pspread and not reheat and equipRef == \"" + eq.id + "\"")["id"]?.let { pSpreadPointId ->
                    hayStack.writePointForCcuUser(pSpreadPointId.toString(), 14, systemPSpread, 0, "Bypass Damper Added")
                }
            }
        }
    }
}

fun setPressureSensorRef(config: BypassDamperProfileConfiguration, hayStack: CCUHsApi) {
    val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")
    val equip = hayStack.read("equip and group == \"" + config.nodeAddress + "\"")

    val physPressureSensor = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.pressureSensor + "\"")
    val physPressureSensorPoint = RawPoint.Builder().setHDict(physPressureSensor)
    val analogIn1 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1In + "\"")
    val analogIn1Point = RawPoint.Builder().setHDict(analogIn1)
    val logPressureSensor = hayStack.read("point and equipRef == \""+equip.get("id")+"\" and domainName == \"" + DomainName.ductStaticPressureSensor + "\"")

    if (config.pressureSensorType.currentVal > 0.0) {
        hayStack.updatePoint(analogIn1Point.setPointRef(logPressureSensor.get("id").toString()).build(), analogIn1.get("id").toString())
        hayStack.updatePoint(physPressureSensorPoint.setPointRef(null).build(), physPressureSensor.get("id").toString())
    } else {
        hayStack.updatePoint(analogIn1Point.setPointRef(null).build(), analogIn1.get("id").toString())
        hayStack.updatePoint(physPressureSensorPoint.setPointRef(logPressureSensor.get("id").toString()).build(), physPressureSensor.get("id").toString())
    }
}

fun setOutputTypes(config: BypassDamperProfileConfiguration, hayStack: CCUHsApi) {
    val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

    var analogOut1 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1Out + "\"")
    var analog1Point = RawPoint.Builder().setHDict(analogOut1)
    hayStack.updatePoint(analog1Point.setType(getDamperTypeString(config)).build(), analogOut1.get("id").toString())

}

fun setDamperFeedback(config: BypassDamperProfileConfiguration, hayStack: CCUHsApi) {
    val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

    var analogIn2 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog2In + "\"")
    var analogIn2Point = RawPoint.Builder().setHDict(analogIn2)
    hayStack.updatePoint(analogIn2Point.setType(getDamperTypeString(config)).build(), analogIn2.get("id").toString())
}

private fun getDamperTypeString(config: BypassDamperProfileConfiguration) : String {
    return when(config.damperType.currentVal.toInt()) {
        0 -> "0-10v"
        1 -> "2-10v"
        2 -> "10-0v"
        3 -> "10-2v"
        4 -> "Smart Damper"
        5 -> "0-5v"
        else -> { "0-10v" }
    }
}



