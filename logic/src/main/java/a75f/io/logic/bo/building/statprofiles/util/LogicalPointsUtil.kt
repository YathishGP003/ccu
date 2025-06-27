package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.hayStack
import a75f.io.domain.api.Point
import a75f.io.domain.devices.HyperStatDevice
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_ONE
import a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_THREE
import a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_TWO
import a75f.io.logic.bo.building.definitions.Port.RELAY_FIVE
import a75f.io.logic.bo.building.definitions.Port.RELAY_FOUR
import a75f.io.logic.bo.building.definitions.Port.RELAY_ONE
import a75f.io.logic.bo.building.definitions.Port.RELAY_SIX
import a75f.io.logic.bo.building.definitions.Port.RELAY_THREE
import a75f.io.logic.bo.building.definitions.Port.RELAY_TWO
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import org.projecthaystack.UnknownRecException

/**
 * Created by Manjunath K on 28-04-2025.
 */

fun getHSRelayOutputPoints(equip: Pipe2V2Equip): HashMap<Int, String> {
    val relayStatus: HashMap<Int, String> = HashMap()

    putPointToMap(equip.fanLowSpeed, relayStatus, HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal)
    putPointToMap(equip.fanMediumSpeed, relayStatus, HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal)
    putPointToMap(equip.fanHighSpeed, relayStatus, HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)
    putPointToMap(equip.fanEnable, relayStatus, HsPipe2RelayMapping.FAN_ENABLED.ordinal)
    putPointToMap(equip.occupiedEnable, relayStatus, HsPipe2RelayMapping.OCCUPIED_ENABLED.ordinal)
    putPointToMap(equip.humidifierEnable, relayStatus, HsPipe2RelayMapping.HUMIDIFIER.ordinal)
    putPointToMap(equip.dehumidifierEnable, relayStatus, HsPipe2RelayMapping.DEHUMIDIFIER.ordinal)
    putPointToMap(equip.waterValve, relayStatus, HsPipe2RelayMapping.WATER_VALVE.ordinal)
    putPointToMap(equip.auxHeatingStage1, relayStatus, HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
    putPointToMap(equip.auxHeatingStage2, relayStatus, HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal)
    return relayStatus
}

fun getHSAnalogOutputPoints(equip: Pipe2V2Equip): HashMap<Int, String> {
    val outputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.dcvDamperModulating, outputPoints, HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal)
    putPointToMap(equip.fanSignal, outputPoints, HsPipe2AnalogOutMapping.FAN_SPEED.ordinal)
    putPointToMap(equip.modulatingWaterValve, outputPoints, HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
    return outputPoints
}


fun getHSRelayStatus(equip: HpuV2Equip): HashMap<Int, String> {
    val relayStatus: HashMap<Int, String> = HashMap()
    putPointToMap(equip.compressorStage1, relayStatus, HsHpuRelayMapping.COMPRESSOR_STAGE1.ordinal)
    putPointToMap(equip.compressorStage2, relayStatus, HsHpuRelayMapping.COMPRESSOR_STAGE2.ordinal)
    putPointToMap(equip.compressorStage3, relayStatus, HsHpuRelayMapping.COMPRESSOR_STAGE3.ordinal)
    putPointToMap(equip.auxHeatingStage1, relayStatus, HsHpuRelayMapping.AUX_HEATING_STAGE1.ordinal)
    putPointToMap(equip.auxHeatingStage2, relayStatus, HsHpuRelayMapping.AUX_HEATING_STAGE2.ordinal)
    putPointToMap(equip.fanLowSpeed, relayStatus, HsHpuRelayMapping.FAN_LOW_SPEED.ordinal)
    putPointToMap(equip.fanMediumSpeed, relayStatus, HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal)
    putPointToMap(equip.fanHighSpeed, relayStatus, HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
    putPointToMap(equip.fanEnable, relayStatus, HsHpuRelayMapping.FAN_ENABLED.ordinal)
    putPointToMap(equip.occupiedEnable, relayStatus, HsHpuRelayMapping.OCCUPIED_ENABLED.ordinal)
    putPointToMap(equip.humidifierEnable, relayStatus, HsHpuRelayMapping.HUMIDIFIER.ordinal)
    putPointToMap(equip.dehumidifierEnable, relayStatus, HsHpuRelayMapping.DEHUMIDIFIER.ordinal)
    putPointToMap(equip.changeOverCooling, relayStatus, HsHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)
    putPointToMap(equip.changeOverHeating, relayStatus, HsHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
    return relayStatus
}

fun getHSAnalogOutputPoints(equip: HpuV2Equip): HashMap<Int, String> {
    val analogOutputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.compressorSpeed, analogOutputPoints, HsHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal)
    putPointToMap(equip.fanSignal, analogOutputPoints, HsHpuAnalogOutMapping.FAN_SPEED.ordinal)
    putPointToMap(equip.dcvDamperModulating, analogOutputPoints, HsHpuAnalogOutMapping.DCV_DAMPER.ordinal)
    return analogOutputPoints
}


fun getHSLogicalPointList(
    equip: HyperStatEquip, config: HyperStatConfiguration
): HashMap<Port, String> {

    val device = Domain.getEquipDevices()[equip.equipRef] as? HyperStatDevice
        ?: run {
            val deviceMap = hayStack.readEntity("device and equipRef== \"${equip.equipRef}\"")
            HyperStatDevice(deviceMap["id"].toString())
        }
    val logicalPoints = HashMap<Port, String>()

    val points = listOf(
        Triple(RELAY_ONE, device.relay1.readPoint().pointRef, config.relay1Enabled.enabled),
        Triple(RELAY_TWO, device.relay2.readPoint().pointRef, config.relay2Enabled.enabled),
        Triple(RELAY_THREE, device.relay3.readPoint().pointRef, config.relay3Enabled.enabled),
        Triple(RELAY_FOUR, device.relay4.readPoint().pointRef, config.relay4Enabled.enabled),
        Triple(RELAY_FIVE, device.relay5.readPoint().pointRef, config.relay5Enabled.enabled),
        Triple(RELAY_SIX, device.relay6.readPoint().pointRef, config.relay6Enabled.enabled),
        Triple(ANALOG_OUT_ONE, device.analog1Out.readPoint().pointRef, config.analogOut1Enabled.enabled),
        Triple(ANALOG_OUT_TWO, device.analog2Out.readPoint().pointRef, config.analogOut2Enabled.enabled),
        Triple(ANALOG_OUT_THREE, device.analog3Out.readPoint().pointRef, config.analogOut3Enabled.enabled)
    )

    for ((port, pointRef, isEnabled) in points) {
        if (isEnabled && !pointRef.isNullOrEmpty()) {
            logicalPoints[port] = pointRef
        }
    }
    return logicalPoints
}


/**
 * MyStat logical points
 */

fun getMyStatHpuRelayOutputPoints(equip: MyStatHpuEquip): HashMap<Int, String> {
    val relayStatus: HashMap<Int, String> = HashMap()

    putPointToMap(equip.compressorStage1, relayStatus, MyStatHpuRelayMapping.COMPRESSOR_STAGE1.ordinal)
    putPointToMap(equip.compressorStage2, relayStatus, MyStatHpuRelayMapping.COMPRESSOR_STAGE2.ordinal)
    putPointToMap(equip.auxHeatingStage1, relayStatus, MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal)
    putPointToMap(equip.fanLowSpeed, relayStatus, MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal)
    putPointToMap(equip.fanHighSpeed, relayStatus, MyStatHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
    putPointToMap(equip.fanEnable, relayStatus, MyStatHpuRelayMapping.FAN_ENABLED.ordinal)
    putPointToMap(equip.occupiedEnable, relayStatus, MyStatHpuRelayMapping.OCCUPIED_ENABLED.ordinal)
    putPointToMap(equip.humidifierEnable, relayStatus, MyStatHpuRelayMapping.HUMIDIFIER.ordinal)
    putPointToMap(equip.dehumidifierEnable, relayStatus, MyStatHpuRelayMapping.DEHUMIDIFIER.ordinal)
    putPointToMap(equip.changeOverCooling, relayStatus, MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)
    putPointToMap(equip.changeOverHeating, relayStatus, MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
    putPointToMap(equip.dcvDamper, relayStatus, MyStatHpuRelayMapping.DCV_DAMPER.ordinal)

    return relayStatus
}

fun getMyStatHpuAnalogOutputPoints(equip: MyStatHpuEquip): HashMap<Int, String> {
    val analogOutputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.compressorSpeed, analogOutputPoints, MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal)
    putPointToMap(equip.dcvDamperModulating, analogOutputPoints, MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)
    putPointToMap(equip.fanSignal, analogOutputPoints, MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal)
    return analogOutputPoints
}


fun getMyStatLogicalPointList(
    equip: MyStatEquip,
    config: MyStatConfiguration
): HashMap<Port, String> {

    val device = Domain.getEquipDevices()[equip.equipRef] as? MyStatDevice
        ?: run {
            val deviceMap = hayStack.readEntity("device and equipRef== \"${equip.equipRef}\"")
            MyStatDevice(deviceMap["id"].toString())
        }

    val logicalPoints = HashMap<Port, String>()

    val points = listOf(
        Triple(RELAY_ONE, device.relay1.readPoint().pointRef, config.relay1Enabled.enabled),
        Triple(RELAY_TWO, device.relay2.readPoint().pointRef, config.relay2Enabled.enabled),
        Triple(RELAY_THREE, device.relay3.readPoint().pointRef, config.relay3Enabled.enabled),
        Triple(RELAY_FOUR, device.relay4.readPoint().pointRef, config.relay4Enabled.enabled),
        Triple(
            ANALOG_OUT_ONE,
            device.analog1Out.readPoint().pointRef,
            config.analogOut1Enabled.enabled
        )
    )

    for ((port, pointRef, isEnabled) in points) {
        if (isEnabled && !pointRef.isNullOrEmpty()) {
            logicalPoints[port] = pointRef
        }
    }
    return logicalPoints
}


fun getMyStatRelayOutputPoints(equip: MyStatPipe2Equip): HashMap<Int, String> {
    val relayStatus: HashMap<Int, String> = HashMap()

    putPointToMap(equip.fanLowSpeed, relayStatus, MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal)
    putPointToMap(equip.fanHighSpeed, relayStatus, MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)
    putPointToMap(equip.fanEnable, relayStatus, MyStatPipe2RelayMapping.FAN_ENABLED.ordinal)
    putPointToMap(equip.occupiedEnable, relayStatus, MyStatPipe2RelayMapping.OCCUPIED_ENABLED.ordinal)
    putPointToMap(equip.humidifierEnable, relayStatus, MyStatPipe2RelayMapping.HUMIDIFIER.ordinal)
    putPointToMap(equip.dehumidifierEnable, relayStatus, MyStatPipe2RelayMapping.DEHUMIDIFIER.ordinal)
    putPointToMap(equip.waterValve, relayStatus, MyStatPipe2RelayMapping.WATER_VALVE.ordinal)
    putPointToMap(equip.auxHeatingStage1, relayStatus, MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
    putPointToMap(equip.dcvDamper, relayStatus, MyStatPipe2RelayMapping.DCV_DAMPER.ordinal)
    return relayStatus
}

fun getMyStatAnalogOutputPoints(equip: MyStatPipe2Equip): HashMap<Int, String> {
    val analogOutputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.modulatingWaterValve, analogOutputPoints, MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
    putPointToMap(equip.dcvDamperModulating, analogOutputPoints, MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)
    putPointToMap(equip.fanSignal, analogOutputPoints, MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal)
    return analogOutputPoints
}

fun putPointToMap(point: Point, outputPointMap: HashMap<Int, String>, mapping: Int) {
    try {
        if (point.pointExists()) outputPointMap[mapping] = point.id
    } catch (e: UnknownRecException) {
        CcuLog.e(L.TAG_CCU_MSHST, "logical point not found ${point.domainName}", e)
    }
}
