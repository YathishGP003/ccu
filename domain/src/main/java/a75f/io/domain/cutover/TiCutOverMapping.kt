package a75f.io.domain.cutover

/**
 * Created by Manjunath K on 27-12-2024.
 */


object TiCutOverMapping {
    val entries = linkedMapOf(
        "currentTemp" to "currentTemp",
        "humidity" to "zoneHumidity",
        "equipStatus" to "equipStatus",
        "equipStatusMessage" to "equipStatusMessage",
        "equipScheduleStatus" to "equipScheduleStatus",
        "heartBeat" to "heartBeat",
        "scheduleType" to "scheduleType",
        "roomTemperatureOffset" to "temperatureOffset",
        "occupancy" to "occupancyMode",
        "desiredTemp" to "desiredTemp",
        "desiredTempCooling" to "desiredTempCooling",
        "desiredTempHeating" to "desiredTempHeating",
        "zonePriority" to "zonePriority",
        "zoneDynamicPriority" to "zoneDynamicPriority",
        "RoomTemperature" to "roomTemperature",
        "roomTemperatureType" to "roomTemperatureType",
        "supplyAirTemperatureType" to "dischargeAirTemperatureType",
        "zonePrioritySpread" to "tiZonePrioritySpread",
        "constantTempAlertTime" to "constantTempAlertTime",
        "autoAwayTime" to "autoAwayTime",
        "zonePriorityMultiplier" to "tiZonePriorityMultiplier",
        "abnormalCurTempRiseTrigger" to "abnormalCurTempRiseTrigger",
        "forcedOccupiedTime" to "forcedOccupiedTime",
        "zoneDeadTime" to "zoneDeadTime",
        "proportionalKFactor" to "tiProportionalKFactor",
        "temperatureProportionalRange" to "tiTemperatureProportionalRange",
        "heatingDeadbandMultiplier" to "tiHeatingDeadbandMultiplier",
        "temperatureIntegralTime" to "tiTemperatureIntegralTime",
        "coolingDeadbandMultiplier" to "tiCoolingDeadbandMultiplier",
        "DemandResponseSetback" to "demandResponseSetback",
        "integralKFactor" to "tiIntegralKFactor",
        "supplyAirTemperature" to "dischargeAirTemperature"
    )


    val tiDeviceMapping = linkedMapOf(
        "Analog1In" to "analog1In",
        "Analog2In" to "analog2In",
        "Th1In" to "th1In",
        "Th2In" to "th2In",
        "SENSOR_RH" to "humiditySensor",
        "currentTemp" to "currentTemp",
        "rssi" to "rssi",
        "firmwareVersion" to "firmwareVersion"
    )

}