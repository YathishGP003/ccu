package a75f.io.logic.bo.building.hyperstat.profiles.pipe2

import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2AnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2RelayMapping

/**
 * Created by Manjunath K on 13-11-2024.
 */


fun getRelayOutputPoints(equip: Pipe2V2Equip): HashMap<Int, String> {
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

fun getAnalogOutputPoints(equip: Pipe2V2Equip): HashMap<Int, String> {
    val analogOutputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.dcvDamperModulating, analogOutputPoints, HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal)
    putPointToMap(equip.fanSignal, analogOutputPoints, HsPipe2AnalogOutMapping.FAN_SPEED.ordinal)
    putPointToMap(equip.modulatingWaterValve, analogOutputPoints, HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
    return analogOutputPoints
}

fun putPointToMap(point: Point, outputPointMap: HashMap<Int, String>, mapping: Int) {
    if (point.pointExists()) outputPointMap[mapping] = point.id
}