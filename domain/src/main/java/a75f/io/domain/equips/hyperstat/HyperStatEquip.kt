package a75f.io.domain.equips.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.DomainEquip
import a75f.io.logger.CcuLog

/**
 * Created by Manjunath K on 26-09-2024.
 */

open class HyperStatEquip(equipRef : String) : DomainEquip(equipRef) {

    var nodeAddress = -1
    var floorRef: String? = null
    var roomRef: String? = null

    init {
        try {
            val equipDetails = CCUHsApi.getInstance().readMapById(equipRef)
            nodeAddress = equipDetails[Tags.GROUP].toString().toInt()
            floorRef = equipDetails[Tags.FLOORREF] as String
            roomRef = equipDetails[Tags.ROOMREF] as String
        } catch (e: Exception) {
           CcuLog.e("CCU_HST", "Error in reading equip details")
        }
    }


    val temperatureOffset = Point(DomainName.temperatureOffset, equipRef)
    val autoAway = Point(DomainName.autoAway, equipRef)
    val autoForceOccupied = Point(DomainName.autoForceOccupied, equipRef)

    val relay1OutputEnable = Point(DomainName.relay1OutputEnable ,equipRef)
    val relay2OutputEnable = Point(DomainName.relay2OutputEnable ,equipRef)
    val relay3OutputEnable = Point(DomainName.relay3OutputEnable ,equipRef)
    val relay4OutputEnable = Point(DomainName.relay4OutputEnable ,equipRef)
    val relay5OutputEnable = Point(DomainName.relay5OutputEnable ,equipRef)
    val relay6OutputEnable = Point(DomainName.relay6OutputEnable ,equipRef)
    val relay1OutputAssociation = Point(DomainName.relay1OutputAssociation, equipRef)
    val relay2OutputAssociation = Point(DomainName.relay2OutputAssociation, equipRef)
    val relay3OutputAssociation = Point(DomainName.relay3OutputAssociation, equipRef)
    val relay4OutputAssociation = Point(DomainName.relay4OutputAssociation, equipRef)
    val relay5OutputAssociation = Point(DomainName.relay5OutputAssociation, equipRef)
    val relay6OutputAssociation = Point(DomainName.relay6OutputAssociation, equipRef)

    val analog1OutputEnable = Point(DomainName.analog1OutputEnable, equipRef)
    val analog2OutputEnable = Point(DomainName.analog2OutputEnable, equipRef)
    val analog3OutputEnable = Point(DomainName.analog3OutputEnable, equipRef)
    val analog1OutputAssociation = Point(DomainName.analog1OutputAssociation, equipRef)
    val analog2OutputAssociation = Point(DomainName.analog2OutputAssociation, equipRef)
    val analog3OutputAssociation = Point(DomainName.analog3OutputAssociation, equipRef)

    val analog1InputEnable = Point(DomainName.analog1InputEnable, equipRef)
    val analog2InputEnable = Point(DomainName.analog2InputEnable, equipRef)
    val analog1InputAssociation = Point(DomainName.analog1InputAssociation, equipRef)
    val analog2InputAssociation = Point(DomainName.analog2InputAssociation, equipRef)

    val thermistor1InputEnable = Point(DomainName.thermistor1InputEnable, equipRef)
    val thermistor2InputEnable = Point(DomainName.thermistor2InputEnable, equipRef)
    val thermistor1InputAssociation = Point(DomainName.thermistor1InputAssociation, equipRef)
    val thermistor2InputAssociation = Point(DomainName.thermistor2InputAssociation, equipRef)

    val keyCardSensingEnable = Point(DomainName.keyCardSensingEnable, equipRef)
    val doorWindowSensingEnable = Point(DomainName.doorWindowSensingEnable, equipRef)
    val doorWindowSensorInput = Point(DomainName.doorWindowSensorInput, equipRef)
    val keyCardSensorInput = Point(DomainName.keyCardSensorInput, equipRef)

    val equipStatus = Point(DomainName.equipStatus, equipRef)
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val equipScheduleStatus = Point(DomainName.equipScheduleStatus, equipRef)
    val heartBeat = Point(DomainName.heartBeat, equipRef)
    val scheduleType = Point(DomainName.scheduleType, equipRef)
    val occupancyDetection = Point(DomainName.occupancyDetection, equipRef)
    val desiredTemp = Point(DomainName.desiredTemp, equipRef)
    val desiredTempCooling = Point(DomainName.desiredTempCooling, equipRef)
    val desiredTempHeating = Point(DomainName.desiredTempHeating, equipRef)
    val occupancyMode = Point(DomainName.occupancyMode, equipRef)
    val fanOpMode = Point(DomainName.fanOpMode, equipRef)
    val operatingMode = Point(DomainName.operatingMode, equipRef)
    val conditioningMode = Point(DomainName.conditioningMode, equipRef)
    val forcedOccupiedTime = Point(DomainName.forcedOccupiedTime, equipRef)
    val autoAwayTime = Point(DomainName.autoAwayTime, equipRef)
    val autoAwaySetback = Point(DomainName.autoAwaySetback, equipRef)
    val co2DamperOpeningRate = Point(DomainName.co2DamperOpeningRate, equipRef)
    val zoneHumidity = Point(DomainName.zoneHumidity, equipRef)
    val zoneCo2 = Point(DomainName.zoneCo2, equipRef)
    val zonePm25 = Point(DomainName.zonePm25, equipRef)
    val zoneIlluminance = Point(DomainName.zoneIlluminance, equipRef)
    val zoneOccupancy = Point(DomainName.zoneOccupancy, equipRef)
    val zoneSound = Point(DomainName.zoneSound, equipRef)
    val occupiedEnable = Point(DomainName.occupiedEnable, equipRef)
    val humidifierEnable = Point(DomainName.humidifierEnable, equipRef)
    val dehumidifierEnable = Point(DomainName.dehumidifierEnable, equipRef)
    val dcvDamperModulating = Point(DomainName.dcvDamperModulating, equipRef)
    val targetHumidifier = Point(DomainName.targetHumidifier, equipRef)
    val targetDehumidifier = Point(DomainName.targetDehumidifier, equipRef)
    val co2Threshold = Point(DomainName.co2Threshold, equipRef)
    val pm25Threshold = Point(DomainName.pm25Threshold, equipRef)
    val co2Target = Point(DomainName.co2Target, equipRef)
    val pm25Target = Point(DomainName.pm25Target, equipRef)
    val pm10Target = Point(DomainName.pm10Target, equipRef)
    val enableHumidityDisplay = Point(DomainName.enableHumidityDisplay, equipRef)
    val enableCo2Display = Point(DomainName.enableCO2Display, equipRef)
    val enablePm25Display = Point(DomainName.enablePm25Display, equipRef)

    val standaloneTemperatureProportionalRange = Point(DomainName.standaloneTemperatureProportionalRange, equipRef)
    val standaloneTemperatureIntegralTime = Point(DomainName.standaloneTemperatureIntegralTime, equipRef)
    val constantTempAlertTime = Point(DomainName.constantTempAlertTime, equipRef)
    val abnormalCurTempRiseTrigger = Point(DomainName.abnormalCurTempRiseTrigger, equipRef)
    val standaloneHumidityHysteresis = Point(DomainName.standaloneHumidityHysteresis, equipRef)
    val standaloneRelayActivationHysteresis = Point(DomainName.standaloneRelayActivationHysteresis, equipRef)
    val standaloneAnalogFanSpeedMultiplier = Point(DomainName.standaloneAnalogFanSpeedMultiplier, equipRef)
    val standaloneCoolingDeadbandMultiplier = Point(DomainName.standaloneCoolingDeadbandMultiplier, equipRef)
    val standaloneHeatingDeadbandMultiplier = Point(DomainName.standaloneHeatingDeadbandMultiplier, equipRef)
    val standaloneProportionalKFactor = Point(DomainName.standaloneProportionalKFactor, equipRef)
    val standaloneCoolingPreconditioningRate = Point(DomainName.standaloneCoolingPreconditioningRate, equipRef)
    val standaloneHeatingPreconditioningRate = Point(DomainName.standaloneHeatingPreconditioningRate, equipRef)
    val zoneDeadTime = Point(DomainName.zoneDeadTime, equipRef)
    val coolingDeadband = Point(DomainName.coolingDeadband, equipRef)
    val standaloneIntegralKFactor = Point(DomainName.standaloneIntegralKFactor, equipRef)
    val demandResponseSetback = Point(DomainName.demandResponseSetback, equipRef)
    val heatingLoopOutput = Point(DomainName.heatingLoopOutput, equipRef)
    val fanLoopOutput = Point(DomainName.fanLoopOutput, equipRef)
    val fanEnable = Point(DomainName.fanEnable, equipRef)
    val dischargeAirTemperature = Point(DomainName.dischargeAirTemperature, equipRef)
    val airFlowSensor = Point(DomainName.airFlowSensor, equipRef)
    val currentTx10 = Point(DomainName.currentTx10, equipRef)
    val currentTx20 = Point(DomainName.currentTx20, equipRef)
    val currentTx50 = Point(DomainName.currentTx50, equipRef)
    val currentTemp = Point(DomainName.currentTemp, equipRef)
    val fanLowSpeed = Point(DomainName.fanLowSpeed, equipRef)
    val fanMediumSpeed = Point(DomainName.fanMediumSpeed, equipRef)
    val fanHighSpeed = Point(DomainName.fanHighSpeed, equipRef)
    val doorWindowSensorNCTitle24 = Point(DomainName.doorWindowSensorNCTitle24, equipRef)
    val doorWindowSensorTitle24 = Point(DomainName.doorWindowSensorTitle24, equipRef)
    val keyCardSensor = Point(DomainName.keyCardSensor, equipRef)
    val coolingLoopOutput = Point(DomainName.coolingLoopOutput, equipRef)
    val analog1FanLow = Point(DomainName.analog1FanLow, equipRef)
    val analog2FanLow = Point(DomainName.analog2FanLow, equipRef)
    val analog3FanLow = Point(DomainName.analog3FanLow, equipRef)
    val analog1FanMedium = Point(DomainName.analog1FanMedium, equipRef)
    val analog2FanMedium = Point(DomainName.analog2FanMedium, equipRef)
    val analog3FanMedium = Point(DomainName.analog3FanMedium, equipRef)
    val analog1FanHigh = Point(DomainName.analog1FanHigh, equipRef)
    val analog2FanHigh = Point(DomainName.analog2FanHigh, equipRef)
    val analog3FanHigh = Point(DomainName.analog3FanHigh, equipRef)
    val analog1MinDCVDamper = Point(DomainName.analog1MinDCVDamper, equipRef)
    val analog2MinDCVDamper = Point(DomainName.analog2MinDCVDamper, equipRef)
    val analog3MinDCVDamper = Point(DomainName.analog3MinDCVDamper, equipRef)
    val analog1MaxDCVDamper = Point(DomainName.analog1MaxDCVDamper, equipRef)
    val analog2MaxDCVDamper = Point(DomainName.analog2MaxDCVDamper, equipRef)
    val analog3MaxDCVDamper = Point(DomainName.analog3MaxDCVDamper, equipRef)


}