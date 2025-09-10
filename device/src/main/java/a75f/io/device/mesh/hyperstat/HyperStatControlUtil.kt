package a75f.io.device.mesh.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.device.HyperStat.HyperStatAnalogOutputControl_t
import a75f.io.device.HyperStat.HyperStatConditioningMode_e
import a75f.io.device.HyperStat.HyperStatControlsMessage_t
import a75f.io.device.HyperStat.HyperStatFanSpeed_e
import a75f.io.device.HyperStat.HyperStatOperatingMode_e
import a75f.io.device.mesh.DeviceUtil.mapAnalogOut
import a75f.io.device.mesh.DeviceUtil.mapDigitalOut
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getEquipDevices
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.devices.HyperStatDevice
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.schedules.Occupancy

/**
 * Created by Manjunath K on 22-10-2024.
 */


fun getHyperStatControlMessage(deviceMap: HashMap<Any, Any>): HyperStatControlsMessage_t.Builder {
    val controls = HyperStatControlsMessage_t.newBuilder()
    if (deviceMap.containsKey("monitoring")) return controls

    val equipRef = deviceMap[Tags.EQUIPREF].toString()
    val hyperStatEquip = Domain.getDomainEquip(equipRef) as HyperStatEquip
    controls.apply {
        setSetTempCooling((hyperStatEquip.desiredTempCooling.readPriorityVal() * 2).toInt())
        setSetTempHeating((hyperStatEquip.desiredTempHeating.readPriorityVal() * 2).toInt())
        setFanSpeed(getDeviceFanMode(hyperStatEquip))
        setConditioningMode(getConditioningMode(hyperStatEquip))
        setUnoccupiedMode(isInUnOccupiedMode(hyperStatEquip))
        setOperatingMode(getOperatingMode(hyperStatEquip))
    }
    fillHyperStatControls(controls, equipRef, deviceMap[Tags.ID].toString())
    return controls
}

private fun fillHyperStatControls(buildr: HyperStatControlsMessage_t.Builder, equipRef: String, deviceRef: String): HyperStatControlsMessage_t.Builder {

    val device = getHyperStatDomainDevice(deviceRef, equipRef)
    fun getAnalogOutValue(value: Double): HyperStatAnalogOutputControl_t {
        return HyperStatAnalogOutputControl_t.newBuilder().setPercent(value.toInt()).build()
    }

    fun getPortValue(port: PhysicalPoint, isRelay: Boolean): Double {
        val logicalPointRef = port.readPoint().pointRef
        if (logicalPointRef == null) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Logical point ref is missing for ${port.domainName}")
            port.writePointValue(0.0)
        } else {
            val actualPhysicalValue: Double = if (isRelay) {
                mapDigitalOut(port.readPoint().type, CCUHsApi.getInstance().readHisValById(logicalPointRef) > 0.0).toDouble()
            } else {
                mapAnalogOut(port.readPoint().type,
                    CCUHsApi.getInstance().readHisValById(logicalPointRef).toInt().toShort()
                ).toDouble()
            }
            port.writePointValue(actualPhysicalValue)
        }
        val isWritable = port.isWritable()
        val logicalVal = if (isWritable) {
            port.readPriorityVal()
        } else {
            port.readHisVal()
        }
        val mappedVal = if (Globals.getInstance().isTemporaryOverrideMode) {
            port.readHisVal()
        } else {
            logicalVal
        }
        return mappedVal
    }
    try {
        device.getRelays().forEachIndexed { index, relay ->
            val mappedVal = getPortValue(relay, true) > 0
            when (index) {
                0 -> buildr.setRelay1(mappedVal)
                1 -> buildr.setRelay2(mappedVal)
                2 -> buildr.setRelay3(mappedVal)
                3 -> buildr.setRelay4(mappedVal)
                4 -> buildr.setRelay5(mappedVal)
                5 -> buildr.setRelay6(mappedVal)
            }
        }
        device.getAnalogOuts().forEachIndexed { index, analogOut ->
            val mappedVal = getPortValue(analogOut, false).toInt().toShort()
            when (index) {
                0 -> buildr.setAnalogOut1(getAnalogOutValue(mappedVal.toDouble()))
                1 -> buildr.setAnalogOut2(getAnalogOutValue(mappedVal.toDouble()))
                2 -> buildr.setAnalogOut3(getAnalogOutValue(mappedVal.toDouble()))
            }
        }
    } catch (e: NullPointerException) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Exception fillHyperStatControls: ", e)
    }
    return buildr
}

private fun getDeviceFanMode(equip: HyperStatEquip): HyperStatFanSpeed_e {
    try {
        val fanMode = StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()]
        return when (fanMode) {
            StandaloneFanStage.OFF -> HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_OFF
            StandaloneFanStage.AUTO -> HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO
            StandaloneFanStage.LOW_ALL_TIME, StandaloneFanStage.LOW_CUR_OCC, StandaloneFanStage.LOW_OCC -> HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_LOW
            StandaloneFanStage.MEDIUM_ALL_TIME, StandaloneFanStage.MEDIUM_CUR_OCC, StandaloneFanStage.MEDIUM_OCC -> HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_MED
            StandaloneFanStage.HIGH_ALL_TIME, StandaloneFanStage.HIGH_CUR_OCC, StandaloneFanStage.HIGH_OCC -> HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_HIGH
        }
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Exception getDeviceFanMode: ", e)
    }
    return HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_OFF
}

private fun getConditioningMode(equip: HyperStatEquip): HyperStatConditioningMode_e {
    try {
        val conditioningMode = StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal().toInt()]
        return when (conditioningMode) {
            StandaloneConditioningMode.AUTO -> HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_AUTO
            StandaloneConditioningMode.COOL_ONLY -> HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_COOLING
            StandaloneConditioningMode.HEAT_ONLY -> HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_HEATING
            else -> HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_OFF
        }
    } catch (e: java.lang.Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Exception getConditioningMode: ", e)
    }
    return HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_OFF
}

private fun isInUnOccupiedMode(equip: HyperStatEquip): Boolean {
    return equip.occupancyMode.readHisVal().toInt().let {
        (it == Occupancy.UNOCCUPIED.ordinal || it == Occupancy.AUTOAWAY.ordinal)
    }
}

private fun getOperatingMode(equip: HyperStatEquip): HyperStatOperatingMode_e {
    val operatingMode = equip.operatingMode.readHisVal().toInt()
    return when (operatingMode) {
        1 -> HyperStatOperatingMode_e.HYPERSTAT_OPERATING_MODE_COOLING
        2 -> HyperStatOperatingMode_e.HYPERSTAT_OPERATING_MODE_HEATING
        else -> HyperStatOperatingMode_e.HYPERSTAT_OPERATING_MODE_OFF
    }
}

fun getHyperStatDevice(nodeAddress: Int): HashMap<*, *>? {
    return CCUHsApi.getInstance().readEntity("domainName == \"" + DomainName.hyperstatDevice + "\" and addr == \"" + nodeAddress + "\"")
}

fun getHyperStatDomainDevice(deviceRef: String, equipRef: String): HyperStatDevice {
    val devices = getEquipDevices()
    if (devices.containsKey(equipRef)) {
        return devices[equipRef] as HyperStatDevice
    } else {
        Domain.devices[equipRef] = HyperStatDevice(deviceRef)
    }
    return devices[equipRef] as HyperStatDevice
}