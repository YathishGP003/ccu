package a75f.io.logic.bo.building.hyperstat.profiles.hpu

import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuRelayMapping

/**
 * Created by Manjunath K on 06-11-2024.
 */

fun getRelayStatus(equip: HpuV2Equip): HashMap<Int, String> {
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

fun getAnalogOutputPoints(equip: HpuV2Equip): HashMap<Int, String> {
    val analogOutputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.compressorSpeed, analogOutputPoints, HsHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal)
    putPointToMap(equip.fanSignal, analogOutputPoints, HsHpuAnalogOutMapping.FAN_SPEED.ordinal)
    putPointToMap(equip.dcvDamperModulating, analogOutputPoints, HsHpuAnalogOutMapping.DCV_DAMPER.ordinal)
    return analogOutputPoints
}

fun putPointToMap(point: Point, outputPointMap: HashMap<Int, String>, mapping: Int) {
    if (point.pointExists()) outputPointMap[mapping] = point.id
}