package a75f.io.domain.cutover

object VavZoneProfileCutOverMapping {
    val entries = mapOf(
        "damperType" to "damperType",
        "damperShape" to "damperShape",
        "reheatType" to "reheatType",
        "damperSize" to "damperSize",

        "autoawayEnabled" to "autoAway",
        "autoForceOccupiedEnabled" to "autoForceOccupied",
        "enableCFMControl" to "enableCFMControl",
        "enableIAQControl" to "enableIAQControl",
        "enableCO2Control" to "enableCO2Control",

        "minCoolingDamperPos" to "minCoolingDamperPos",
        "minHeatingDamperPos" to "minHeatingDamperPos",
        "maxHeatingDamperPos" to "maxHeatingDamperPos",
        "maxCoolingDamperPos" to "maxCoolingDamperPos",
        "temperatureOffset" to "temperatureOffset",

        "zonePriorityMultiplier" to "vavZonePriorityMultiplier",
        "zonePrioritySpread" to "vavZonePrioritySpread",
        "zoneDeadTime" to "zoneDeadTime",
        "zonePriority" to "zonePriority",
        "coolingAirflowTemp" to "coolingAirflowTemp",
        "abnormalCurTempRiseTrigger" to "abnormalCurTempRiseTrigger",
        "heatingAirflowTemp" to "heatingAirflowTemp",
        "forcedOccupiedTime" to "forcedOccupiedTime",
        "autoAwayTime" to "autoAwayTime",
        "autoAwaySetback" to "autoAwaySetback",
        "constantTempAlertTime" to "constantTempAlertTime",

        "desiredTemp" to "desiredTemp",
        "desiredTempCooling" to "desiredTempCooling",
        "desiredTempHeating" to "desiredTempHeating",

        "currentTemp" to "currentTemp",
        "co2" to "zoneCO2",
        "humidity" to "zoneHumidity",
        "voc" to "zoneVoc",
        "pressure" to "pressureSensor",
        "occupancyDetection" to "occupancyDetection",
        "uvi" to "zoneUvi",
        "occupancySensor" to "zoneOccupancy",
        "illuminance" to "zoneIlluminance",


        "damperPos" to "damperCmd",
        "normalizedDamperPos" to "normalizedDamperCmd",
        "damperFeedback" to "damperFeedback",
        "reheatPos" to "reheatCmd",
        "occupancy" to "occupancyMode",


        "heatingLoopOp" to "heatingLoopOutput",
        "coolingLoopOp" to "coolingLoopOutput",
        "enteringAirTemp" to "enteringAirTemp",
        "dischargeAirTemp" to "dischargeAirTemp",
        "dischargeSp" to "dischargeAirTempSetpoint",

        "coolingDeadbandMultiplier" to "vavCoolingDeadbandMultiplier",
        "heatingDeadbandMultiplier" to "vavHeatingDeadbandMultiplier",

        "proportionalKFactor" to "vavProportionalKFactor",
        "integralKFactor" to "vavIntegralKfactor",
        "temperatureProportionalRange" to "vavTemperatureProportionalRange",
        "temperatureIntegralTime" to "vavTemperatureIntegralTime",

        "scheduleType" to "scheduleType",
        "equipStatus" to "equipStatus",
        "equipScheduleStatus" to "equipScheduleStatus",
        "equipStatusMessage" to "equipStatusMessage",

        "zoneCO2Target" to "vavZoneCo2Target",
        "zoneCO2Threshold" to "vavZoneCo2Threshold",
        "zoneVOCThreshold" to "vavZoneVocThreshold",
        "zoneVOCTarget" to "vavZoneVocTarget",

        "zoneDynamicPriority" to "zoneDynamicPriority",

        "heartBeat" to "heartBeat",
        "otaStatus" to "otaStatus",

        "satCurrentRequest" to "satCurrentRequest",
        "satRequestPercentage" to "satRequestPercentage",
        "co2CurrentRequest" to "co2CurrentRequest",
        "co2RequestPercentage" to "co2RequestPercentage",
        "spCurrentRequest" to "pressureCurrentRequest",
        "staticRequestPercentage" to "pressureRequestPercentage",

        "valveActuationStartDamperPosDuringSysHeating" to "valveActuationStartDamperPosDuringSysHeating",
        "reheatZoneMaxDischargeTemp" to "reheatZoneMaxDischargeTemp",
        "reheatZoneToDATMinDifferential" to "reheatZoneToDATMinDifferential",
        "reheatZoneDischargeTempOffset" to "reheatZoneDischargeTempOffset",

        "minCFMCooling" to "minCFMCooling",
        "minCFMReheating" to "minCFMReheating",
        "airflowCFMIntegralTime" to "vavAirFlowCfmIntegralTime",
        "maxCFMCooling" to "maxCFMCooling",
        "airflowCFMProportionalKFactor" to "vavAirFlowCfmProportionalKFactor",
        "kFactor" to "kFactor",
        "maxCFMReheating" to "maxCFMReheating",
        "airVelocity" to "airVelocity",
        "airflowCFMIntegralKFactor" to "vavAirFlowCfmIntegralKfactor",
        "airflowCFMProportionalRange" to "vavAirflowCFMProportionalRange",
        "airflow" to "airFlowSensor",

        "seriesFan" to "seriesFanCmd",
        "parallelFan" to "parallelFanCmd",
        "fanControlOnFixedTimeDelay" to "fanControlOnFixedTimeDelay"
    )
}