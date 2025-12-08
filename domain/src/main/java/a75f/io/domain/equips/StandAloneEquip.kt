package a75f.io.domain.equips

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.logger.CcuLog

/**
 * Author: Manjunath Kundaragi
 * Created on: 23-10-2025
 */
open class StandAloneEquip(equipRef : String) : DomainEquip(equipRef)  {
    var nodeAddress = -1
    var floorRef: String? = null
    var roomRef: String? = null

    val relayStages = HashMap<String, Int>()
    val analogOutStages = HashMap<String, Int>()


    fun isCondensateTripped(): Boolean = condensateStatusNC.readHisVal() > 0.0 || condensateStatusNO.readHisVal() > 0.0

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
    val prePurgeEnable = Point(DomainName.prePurgeEnable, equipRef)
    val prePurgeStatus = Point(DomainName.prePurgeStatus, equipRef)

    val sensorBusAddress0Enable = Point(DomainName.sensorBusAddress0Enable, equipRef)
    val sensorBusPressureEnable = Point(DomainName.sensorBusPressureEnable, equipRef)
    val sensorBusAddress1Enable = Point(DomainName.sensorBusAddress1Enable, equipRef)
    val sensorBusAddress2Enable = Point(DomainName.sensorBusAddress2Enable, equipRef)
    val sensorBusAddress3Enable = Point(DomainName.sensorBusAddress3Enable, equipRef)

    val temperatureSensorBusAdd0 = Point(DomainName.temperatureSensorBusAdd0, equipRef)
    val temperatureSensorBusAdd1 = Point(DomainName.temperatureSensorBusAdd1, equipRef)
    val temperatureSensorBusAdd2 = Point(DomainName.temperatureSensorBusAdd2, equipRef)
    val humiditySensorBusAdd0 = Point(DomainName.humiditySensorBusAdd0, equipRef)
    val humiditySensorBusAdd1 = Point(DomainName.humiditySensorBusAdd1, equipRef)
    val humiditySensorBusAdd2 = Point(DomainName.humiditySensorBusAdd2, equipRef)
    val pressureSensorBusAdd0 = Point(DomainName.pressureSensorBusAdd0, equipRef)

    val relay1OutputEnable = Point(DomainName.relay1OutputEnable, equipRef)
    val relay2OutputEnable = Point(DomainName.relay2OutputEnable, equipRef)
    val relay3OutputEnable = Point(DomainName.relay3OutputEnable, equipRef)
    val relay4OutputEnable = Point(DomainName.relay4OutputEnable, equipRef)
    val relay5OutputEnable = Point(DomainName.relay5OutputEnable, equipRef)
    val relay6OutputEnable = Point(DomainName.relay6OutputEnable, equipRef)
    val relay7OutputEnable = Point(DomainName.relay7OutputEnable, equipRef)
    val relay8OutputEnable = Point(DomainName.relay8OutputEnable, equipRef)

    val relay1OutputAssociation = Point(DomainName.relay1OutputAssociation, equipRef)
    val relay2OutputAssociation = Point(DomainName.relay2OutputAssociation, equipRef)
    val relay3OutputAssociation = Point(DomainName.relay3OutputAssociation, equipRef)
    val relay4OutputAssociation = Point(DomainName.relay4OutputAssociation, equipRef)
    val relay5OutputAssociation = Point(DomainName.relay5OutputAssociation, equipRef)
    val relay6OutputAssociation = Point(DomainName.relay6OutputAssociation, equipRef)
    val relay7OutputAssociation = Point(DomainName.relay7OutputAssociation, equipRef)
    val relay8OutputAssociation = Point(DomainName.relay8OutputAssociation, equipRef)

    val analog1OutputEnable = Point(DomainName.analog1OutputEnable, equipRef)
    val analog2OutputEnable = Point(DomainName.analog2OutputEnable, equipRef)
    val analog3OutputEnable = Point(DomainName.analog3OutputEnable, equipRef)
    val analog4OutputEnable = Point(DomainName.analog4OutputEnable, equipRef)

    val analog1OutputAssociation = Point(DomainName.analog1OutputAssociation, equipRef)
    val analog2OutputAssociation = Point(DomainName.analog2OutputAssociation, equipRef)
    val analog3OutputAssociation = Point(DomainName.analog3OutputAssociation, equipRef)
    val analog4OutputAssociation = Point(DomainName.analog4OutputAssociation, equipRef)

    val analog1InputEnable = Point(DomainName.analog1InputEnable, equipRef)
    val analog2InputEnable = Point(DomainName.analog2InputEnable, equipRef)
    val analog1InputAssociation = Point(DomainName.analog1InputAssociation, equipRef)
    val analog2InputAssociation = Point(DomainName.analog2InputAssociation, equipRef)

    val thermistor1InputEnable = Point(DomainName.thermistor1InputEnable, equipRef)
    val thermistor2InputEnable = Point(DomainName.thermistor2InputEnable, equipRef)
    val thermistor1InputAssociation = Point(DomainName.thermistor1InputAssociation, equipRef)
    val thermistor2InputAssociation = Point(DomainName.thermistor2InputAssociation, equipRef)

    val universalOut1Enable = Point(DomainName.universal1OutputEnable, equipRef)
    val universalOut2Enable = Point(DomainName.universal2OutputEnable, equipRef)

    val universalOut1Association = Point(DomainName.universal1OutputAssociation, equipRef)
    val universalOut2Association = Point(DomainName.universal2OutputAssociation, equipRef)

    val universalIn1Enable = Point(DomainName.universalIn1Enable, equipRef)
    val universalIn2Enable = Point(DomainName.universalIn2Enable, equipRef)
    val universalIn3Enable = Point(DomainName.universalIn3Enable, equipRef)
    val universalIn4Enable = Point(DomainName.universalIn4Enable, equipRef)
    val universalIn5Enable = Point(DomainName.universalIn5Enable, equipRef)
    val universalIn6Enable = Point(DomainName.universalIn6Enable, equipRef)
    val universalIn7Enable = Point(DomainName.universalIn7Enable, equipRef)
    val universalIn8Enable = Point(DomainName.universalIn8Enable, equipRef)

    val universalIn1Association = Point(DomainName.universalIn1Association, equipRef)
    val universalIn2Association = Point(DomainName.universalIn2Association, equipRef)
    val universalIn3Association = Point(DomainName.universalIn3Association, equipRef)
    val universalIn4Association = Point(DomainName.universalIn4Association, equipRef)
    val universalIn5Association = Point(DomainName.universalIn5Association, equipRef)
    val universalIn6Association = Point(DomainName.universalIn6Association, equipRef)
    val universalIn7Association = Point(DomainName.universalIn7Association, equipRef)
    val universalIn8Association = Point(DomainName.universalIn8Association, equipRef)

    val disableTouch = Point(DomainName.disableTouch, equipRef)
    val enableBrightness = Point(DomainName.enableBrightness, equipRef)
    val enableCo2Display = Point(DomainName.enableCo2Display, equipRef)
    val enableBacklight = Point(DomainName.enableBacklight, equipRef)
    val enableHumidityDisplay = Point(DomainName.enableHumidityDisplay, equipRef)
    val enablePm25Display = Point(DomainName.enablePm25Display, equipRef)
    val enableSpaceTempDisplay = Point(DomainName.enableSpaceTempDisplay, equipRef)
    val enableDesiredTempDisplay = Point(DomainName.enableDesiredTempDisplay, equipRef)
    val enableConditioningModeFanAccess = Point(DomainName.enableConditioningModeFanAccess, equipRef)
    val pinLockConditioningModeFanAccess = Point(DomainName.pinLockConditioningModeFanAccess, equipRef)
    val pinLockInstallerAccess = Point(DomainName.pinLockInstallerAccess, equipRef)
    val installerPinEnable = Point(DomainName.enableInstallerAccess, equipRef)

    val equipStatus = Point(DomainName.equipStatus, equipRef)
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val equipScheduleStatus = Point(DomainName.equipScheduleStatus, equipRef)
    val heartBeat = Point(DomainName.heartBeat, equipRef)
    val occupancyDetection = Point(DomainName.occupancyDetection, equipRef)
    val desiredTemp = Point(DomainName.desiredTemp, equipRef)
    val occupancyMode = Point(DomainName.occupancyMode, equipRef)
    val operatingMode = Point(DomainName.operatingMode, equipRef)


    //====================================================================================
    // MIN MAX points

    val analog1MinFanSpeed = Point(DomainName.analog1MinFanSpeed, equipRef)
    val analog2MinFanSpeed = Point(DomainName.analog2MinFanSpeed, equipRef)
    val analog3MinFanSpeed = Point(DomainName.analog3MinFanSpeed, equipRef)
    val analog4MinFanSpeed = Point(DomainName.analog4MinFanSpeed, equipRef)
    val analog1MaxFanSpeed = Point(DomainName.analog1MaxFanSpeed, equipRef)
    val analog2MaxFanSpeed = Point(DomainName.analog2MaxFanSpeed, equipRef)
    val analog3MaxFanSpeed = Point(DomainName.analog3MaxFanSpeed, equipRef)
    val analog4MaxFanSpeed = Point(DomainName.analog4MaxFanSpeed, equipRef)

    val analog1FanLow = Point(DomainName.analog1FanLow, equipRef)
    val analog2FanLow = Point(DomainName.analog2FanLow, equipRef)
    val analog3FanLow = Point(DomainName.analog3FanLow, equipRef)
    val analog4FanLow = Point(DomainName.analog4FanLow, equipRef)
    val analog1FanMedium = Point(DomainName.analog1FanMedium, equipRef)
    val analog2FanMedium = Point(DomainName.analog2FanMedium, equipRef)
    val analog3FanMedium = Point(DomainName.analog3FanMedium, equipRef)
    val analog4FanMedium = Point(DomainName.analog4FanMedium, equipRef)
    val analog1FanHigh = Point(DomainName.analog1FanHigh, equipRef)
    val analog2FanHigh = Point(DomainName.analog2FanHigh, equipRef)
    val analog3FanHigh = Point(DomainName.analog3FanHigh, equipRef)
    val analog4FanHigh = Point(DomainName.analog4FanHigh, equipRef)

    val fanOutCoolingStage1 = Point(DomainName.fanOutCoolingStage1, equipRef)
    val fanOutCoolingStage2 = Point(DomainName.fanOutCoolingStage2, equipRef)
    val fanOutCoolingStage3 = Point(DomainName.fanOutCoolingStage3, equipRef)
    val fanOutHeatingStage1 = Point(DomainName.fanOutHeatingStage1, equipRef)
    val fanOutHeatingStage2 = Point(DomainName.fanOutHeatingStage2, equipRef)
    val fanOutHeatingStage3 = Point(DomainName.fanOutHeatingStage3, equipRef)
    val fanOutCompressorStage1 = Point(DomainName.fanOutCompressorStage1, equipRef)
    val fanOutCompressorStage2 = Point(DomainName.fanOutCompressorStage2, equipRef)
    val fanOutCompressorStage3 = Point(DomainName.fanOutCompressorStage3, equipRef)
    val analog1FanRecirculate = Point(DomainName.analog1FanRecirculate, equipRef)
    val analog2FanRecirculate = Point(DomainName.analog2FanRecirculate, equipRef)
    val analog3FanRecirculate = Point(DomainName.analog3FanRecirculate, equipRef)
    val fanOutEconomizer = Point(DomainName.fanOutEconomizer, equipRef)
    val fanOutRecirculate = Point(DomainName.fanOutRecirculate, equipRef)
    val minFanRuntimePostConditioning = Point(DomainName.minFanRuntimePostConditioning, equipRef)

    val analog1MinCooling = Point(DomainName.analog1MinCooling, equipRef)
    val analog1MaxCooling = Point(DomainName.analog1MaxCooling, equipRef)
    val analog2MinCooling = Point(DomainName.analog2MinCooling, equipRef)
    val analog2MaxCooling = Point(DomainName.analog2MaxCooling, equipRef)
    val analog3MinCooling = Point(DomainName.analog3MinCooling, equipRef)
    val analog3MaxCooling = Point(DomainName.analog3MaxCooling, equipRef)
    val analog4MinCooling = Point(DomainName.analog4MinCooling, equipRef)
    val analog4MaxCooling = Point(DomainName.analog4MaxCooling, equipRef)

    val analog1MinHeating = Point(DomainName.analog1MinHeating, equipRef)
    val analog1MaxHeating = Point(DomainName.analog1MaxHeating, equipRef)
    val analog2MinHeating = Point(DomainName.analog2MinHeating, equipRef)
    val analog2MaxHeating = Point(DomainName.analog2MaxHeating, equipRef)
    val analog3MinHeating = Point(DomainName.analog3MinHeating, equipRef)
    val analog3MaxHeating = Point(DomainName.analog3MaxHeating, equipRef)
    val analog4MinHeating = Point(DomainName.analog4MinHeating, equipRef)
    val analog4MaxHeating = Point(DomainName.analog4MaxHeating, equipRef)

    val analog1MinLinearFanSpeed = Point(DomainName.analog1MinLinearFanSpeed, equipRef)
    val analog1MaxLinearFanSpeed = Point(DomainName.analog1MaxLinearFanSpeed, equipRef)
    val analog2MinLinearFanSpeed = Point(DomainName.analog2MinLinearFanSpeed, equipRef)
    val analog2MaxLinearFanSpeed = Point(DomainName.analog2MaxLinearFanSpeed, equipRef)
    val analog3MinLinearFanSpeed = Point(DomainName.analog3MinLinearFanSpeed, equipRef)
    val analog3MaxLinearFanSpeed = Point(DomainName.analog3MaxLinearFanSpeed, equipRef)
    val analog4MinLinearFanSpeed = Point(DomainName.analog4MinLinearFanSpeed, equipRef)
    val analog4MaxLinearFanSpeed = Point(DomainName.analog4MaxLinearFanSpeed, equipRef)

    val analog1MinCompressorSpeed = Point(DomainName.analog1MinCompressorSpeed, equipRef)
    val analog1MaxCompressorSpeed = Point(DomainName.analog1MaxCompressorSpeed, equipRef)
    val analog2MinCompressorSpeed = Point(DomainName.analog2MinCompressorSpeed, equipRef)
    val analog2MaxCompressorSpeed = Point(DomainName.analog2MaxCompressorSpeed, equipRef)
    val analog3MinCompressorSpeed = Point(DomainName.analog3MinCompressorSpeed, equipRef)
    val analog3MaxCompressorSpeed = Point(DomainName.analog3MaxCompressorSpeed, equipRef)
    val analog4MinCompressorSpeed = Point(DomainName.analog4MinCompressorSpeed, equipRef)
    val analog4MaxCompressorSpeed = Point(DomainName.analog4MaxCompressorSpeed, equipRef)

    val analog1MaxChilledWaterValve = Point(DomainName.analog1MaxChilledWaterValve, equipRef)
    val analog2MaxChilledWaterValve = Point(DomainName.analog2MaxChilledWaterValve, equipRef)
    val analog3MaxChilledWaterValve = Point(DomainName.analog3MaxChilledWaterValve, equipRef)
    val analog4MaxChilledWaterValve = Point(DomainName.analog4MaxChilledWaterValve, equipRef)

    val analog1MinChilledWaterValve = Point(DomainName.analog1MinChilledWaterValve, equipRef)
    val analog2MinChilledWaterValve = Point(DomainName.analog2MinChilledWaterValve, equipRef)
    val analog3MinChilledWaterValve = Point(DomainName.analog3MinChilledWaterValve, equipRef)
    val analog4MinChilledWaterValve = Point(DomainName.analog4MinChilledWaterValve, equipRef)

    val analog1MaxHotWaterValve = Point(DomainName.analog1MaxHotWaterValve, equipRef)
    val analog2MaxHotWaterValve = Point(DomainName.analog2MaxHotWaterValve, equipRef)
    val analog3MaxHotWaterValve = Point(DomainName.analog3MaxHotWaterValve, equipRef)
    val analog4MaxHotWaterValve = Point(DomainName.analog4MaxHotWaterValve, equipRef)

    val analog1MinHotWaterValve = Point(DomainName.analog1MinHotWaterValve, equipRef)
    val analog2MinHotWaterValve = Point(DomainName.analog2MinHotWaterValve, equipRef)
    val analog3MinHotWaterValve = Point(DomainName.analog3MinHotWaterValve, equipRef)
    val analog4MinHotWaterValve = Point(DomainName.analog4MinHotWaterValve, equipRef)

    val analog1MinWaterValve = Point(DomainName.analog1MinWaterValve, equipRef)
    val analog2MinWaterValve = Point(DomainName.analog2MinWaterValve, equipRef)
    val analog3MinWaterValve = Point(DomainName.analog3MinWaterValve, equipRef)
    val analog4MinWaterValve = Point(DomainName.analog4MinWaterValve, equipRef)

    val analog1MaxWaterValve = Point(DomainName.analog1MaxWaterValve, equipRef)
    val analog2MaxWaterValve = Point(DomainName.analog2MaxWaterValve, equipRef)
    val analog3MaxWaterValve = Point(DomainName.analog3MaxWaterValve, equipRef)
    val analog4MaxWaterValve = Point(DomainName.analog4MaxWaterValve, equipRef)

    val analog1MinDCVDamper = Point(DomainName.analog1MinDCVDamper, equipRef)
    val analog2MinDCVDamper = Point(DomainName.analog2MinDCVDamper, equipRef)
    val analog3MinDCVDamper = Point(DomainName.analog3MinDCVDamper, equipRef)
    val analog4MinDCVDamper = Point(DomainName.analog4MinDCVDamper, equipRef)
    val analog1MaxDCVDamper = Point(DomainName.analog1MaxDCVDamper, equipRef)
    val analog2MaxDCVDamper = Point(DomainName.analog2MaxDCVDamper, equipRef)
    val analog3MaxDCVDamper = Point(DomainName.analog3MaxDCVDamper, equipRef)
    val analog4MaxDCVDamper = Point(DomainName.analog4MaxDCVDamper, equipRef)

    val analog1MinOAODamper = Point(DomainName.analog1MinOAODamper, equipRef)
    val analog1MaxOAODamper = Point(DomainName.analog1MaxOAODamper, equipRef)
    val analog2MinOAODamper = Point(DomainName.analog2MinOAODamper, equipRef)
    val analog2MaxOAODamper = Point(DomainName.analog2MaxOAODamper, equipRef)
    val analog3MinOAODamper = Point(DomainName.analog3MinOAODamper, equipRef)
    val analog3MaxOAODamper = Point(DomainName.analog3MaxOAODamper, equipRef)
    val analog4MinOAODamper = Point(DomainName.analog4MinOAODamper, equipRef)
    val analog4MaxOAODamper = Point(DomainName.analog4MaxOAODamper, equipRef)

    val analog1MinReturnDamper = Point(DomainName.analog1MinReturnDamper, equipRef)
    val analog1MaxReturnDamper = Point(DomainName.analog1MaxReturnDamper, equipRef)
    val analog2MinReturnDamper = Point(DomainName.analog2MinReturnDamper, equipRef)
    val analog2MaxReturnDamper = Point(DomainName.analog2MaxReturnDamper, equipRef)
    val analog3MinReturnDamper = Point(DomainName.analog3MinReturnDamper, equipRef)
    val analog3MaxReturnDamper = Point(DomainName.analog3MaxReturnDamper, equipRef)
    val analog4MinReturnDamper = Point(DomainName.analog4MinReturnDamper, equipRef)
    val analog4MaxReturnDamper = Point(DomainName.analog4MaxReturnDamper, equipRef)

    //=============================LoopOutput points=============================================//
    val coolingLoopOutput = Point(DomainName.coolingLoopOutput, equipRef)
    val heatingLoopOutput = Point(DomainName.heatingLoopOutput, equipRef)
    val fanLoopOutput = Point(DomainName.fanLoopOutput, equipRef)
    val dcvLoopOutput = Point(DomainName.dcvLoopOutput, equipRef)
    val compressorLoopOutput = Point(DomainName.compressorLoopOutput, equipRef)

    val economizingLoopOutput = Point(DomainName.economizingLoopOutput, equipRef)
    val saTempLoopOutput = Point(DomainName.saTemperingLoopOutput, equipRef)
    val outsideAirLoopOutput = Point(DomainName.outsideAirLoopOutput, equipRef)
    val outsideAirFinalLoopOutput = Point(DomainName.outsideAirFinalLoopOutput, equipRef)


    // Possible relay logical points
    val coolingStage1 = Point(DomainName.coolingStage1, equipRef)
    val coolingStage2 = Point(DomainName.coolingStage2, equipRef)
    val coolingStage3 = Point(DomainName.coolingStage3, equipRef)
    val heatingStage1 = Point(DomainName.heatingStage1, equipRef)
    val heatingStage2 = Point(DomainName.heatingStage2, equipRef)
    val heatingStage3 = Point(DomainName.heatingStage3, equipRef)
    val coolingSignal = Point(DomainName.coolingSignal, equipRef)
    val heatingSignal = Point(DomainName.heatingSignal, equipRef)
    val fanEnable = Point(DomainName.fanEnable, equipRef)
    val fanLowSpeed = Point(DomainName.fanLowSpeed, equipRef)
    val fanLowSpeedVentilation = Point(DomainName.fanLowSpeedVentilation, equipRef)
    val fanMediumSpeed = Point(DomainName.fanMediumSpeed, equipRef)
    val fanHighSpeed = Point(DomainName.fanHighSpeed, equipRef)
    val occupiedEnable = Point(DomainName.occupiedEnable, equipRef)
    val humidifierEnable = Point(DomainName.humidifierEnable, equipRef)
    val dehumidifierEnable = Point(DomainName.dehumidifierEnable, equipRef)
    val dcvDamper = Point(DomainName.dcvDamper, equipRef)
    val auxHeatingStage1 = Point(DomainName.auxHeatingStage1, equipRef)
    val auxHeatingStage2 = Point(DomainName.auxHeatingStage2, equipRef)
    val compressorStage1 = Point(DomainName.compressorStage1, equipRef)
    val compressorStage2 = Point(DomainName.compressorStage2, equipRef)
    val compressorStage3 = Point(DomainName.compressorStage3, equipRef)
    val changeOverCooling = Point(DomainName.changeOverCooling, equipRef)
    val changeOverHeating = Point(DomainName.changeOverHeating, equipRef)
    val waterValve = Point(DomainName.waterValve, equipRef)
    val hotWaterHeatValve = Point(DomainName.hotWaterHeatValve, equipRef)
    val chilledWaterCoolValve = Point(DomainName.chilledWaterCoolValve, equipRef)
    val exhaustFanStage1 = Point(DomainName.exhaustFanStage1, equipRef)
    val exhaustFanStage2 = Point(DomainName.exhaustFanStage2, equipRef)

    // Possible analog logical points
    val fanSignal = Point(DomainName.fanSignal, equipRef)
    val linearFanSpeed = Point(DomainName.linearFanSpeed, equipRef)
    val stagedFanSpeed = Point(DomainName.stagedFanSpeed, equipRef)
    val dcvDamperModulating = Point(DomainName.dcvDamperModulating, equipRef)
    val hotWaterModulatingHeatValve = Point(DomainName.hotWaterModulatingHeatValve, equipRef)
    val chilledWaterModulatingCoolValve = Point(DomainName.chilledWaterModulatingCoolValve, equipRef)
    val modulatingWaterValve = Point(DomainName.modulatingWaterValve, equipRef)
    val compressorSpeed = Point(DomainName.compressorSpeed, equipRef)
    val oaoDamper = Point(DomainName.oaoDamper, equipRef)
    val returnDamperPosition = Point(DomainName.returnDamperPosition, equipRef)


    // Tuner Points
    val forcedOccupiedTime = Point(DomainName.forcedOccupiedTime, equipRef)
    val autoAwayTime = Point(DomainName.autoAwayTime, equipRef)
    val autoAwaySetback = Point(DomainName.autoAwaySetback, equipRef)
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
    val standaloneIntegralKFactor = Point(DomainName.standaloneIntegralKFactor, equipRef)
    val auxHeating1Activate = Point(DomainName.auxHeating1Activate, equipRef)
    val auxHeating2Activate = Point(DomainName.auxHeating2Activate, equipRef)
    val hyperstatStageDownTimerCounter = Point(DomainName.hyperstatStageDownTimerCounter, equipRef)
    val hyperstatStageUpTimerCounter = Point(DomainName.hyperstatStageUpTimerCounter, equipRef)
    val faceBypassDamperRelayActivationHysteresis = Point(DomainName.faceBypassDamperRelayActivationHysteresis, equipRef)

    val hyperstatPipe2FancoilHeatingThreshold = Point(DomainName.hyperstatPipe2FancoilHeatingThreshold, equipRef)
    val hyperstatPipe2FancoilCoolingThreshold = Point(DomainName.hyperstatPipe2FancoilCoolingThreshold, equipRef)
    val waterValveSamplingOnTime = Point(DomainName.waterValveSamplingOnTime, equipRef)
    val waterValveSamplingWaitTime = Point(DomainName.waterValveSamplingWaitTime, equipRef)
    val waterValveSamplingLoopDeadbandOnTime = Point(DomainName.waterValveSamplingLoopDeadbandOnTime, equipRef)
    val waterValveSamplingLoopDeadbandWaitTime = Point(DomainName.waterValveSamplingLoopDeadbandWaitTime, equipRef)

    // OAO and dcv related points
    val outsideDamperMinOpenDuringRecirculation = Point(DomainName.outsideDamperMinOpenDuringRecirculation, equipRef)
    val outsideDamperMinOpenDuringConditioning = Point(DomainName.outsideDamperMinOpenDuringConditioning, equipRef)
    val outsideDamperMinOpenDuringFanLow = Point(DomainName.outsideDamperMinOpenDuringFanLow, equipRef)
    val outsideDamperMinOpenDuringFanMedium = Point(DomainName.outsideDamperMinOpenDuringFanMedium, equipRef)
    val outsideDamperMinOpenDuringFanHigh = Point(DomainName.outsideDamperMinOpenDuringFanHigh, equipRef)

    val exhaustFanStage1Threshold = Point(DomainName.exhaustFanStage1Threshold, equipRef)
    val exhaustFanStage2Threshold = Point(DomainName.exhaustFanStage2Threshold, equipRef)
    val exhaustFanHysteresis = Point(DomainName.exhaustFanHysteresis, equipRef)
    val insideEnthalpy = Point(DomainName.insideEnthalpy, equipRef)
    val outsideEnthalpy = Point(DomainName.outsideEnthalpy, equipRef)
    val economizingAvailable = Point(DomainName.economizingAvailable, equipRef)
    val matThrottle = Point(DomainName.matThrottle, equipRef)

    val enableOutsideAirOptimization = Point(DomainName.enableOutsideAirOptimization, equipRef)
    val outsideAirCalculatedMinDamper = Point(DomainName.outsideAirCalculatedMinDamper, equipRef)
    val standaloneEconomizingToMainCoolingLoopMap = Point(DomainName.standaloneEconomizingToMainCoolingLoopMap, equipRef)
    val standaloneEconomizingMinTemperature = Point(DomainName.standaloneEconomizingMinTemperature, equipRef)
    val standaloneEconomizingMaxTemperature = Point(DomainName.standaloneEconomizingMaxTemperature, equipRef)
    val standaloneEconomizingMinHumidity = Point(DomainName.standaloneEconomizingMinHumidity, equipRef)
    val standaloneEconomizingMaxHumidity = Point(DomainName.standaloneEconomizingMaxHumidity, equipRef)
    val standaloneEconomizingDryBulbThreshold = Point(DomainName.standaloneEconomizingDryBulbThreshold, equipRef)
    val standaloneEnthalpyDuctCompensationOffset = Point(DomainName.standaloneEnthalpyDuctCompensationOffset, equipRef)
    val standaloneOutsideDamperMixedAirTarget = Point(DomainName.standaloneOutsideDamperMixedAirTarget, equipRef)
    val standaloneOutsideDamperMixedAirMinimum = Point(DomainName.standaloneOutsideDamperMixedAirMinimum, equipRef)
    val standaloneDuctTemperatureOffset = Point(DomainName.standaloneDuctTemperatureOffset, equipRef)
    val standalonePrePurgeRuntimeTuner = Point(DomainName.standalonePrePurgeRuntimeTuner, equipRef)
    val standalonePrePurgeOccupiedTimeOffsetTuner = Point(DomainName.standalonePrePurgeOccupiedTimeOffsetTuner, equipRef)
    val standalonePrePurgeFanSpeedTuner = Point(DomainName.standalonePrePurgeFanSpeedTuner, equipRef)
    val prePurgeOutsideDamperOpen = Point(DomainName.prePurgeOutsideDamperOpen, equipRef)
    val dcvAvailable = Point(DomainName.dcvAvailable, equipRef)

    // Threshold target points
    val co2Threshold = Point(DomainName.co2Threshold, equipRef)
    val pm25Threshold = Point(DomainName.pm25Threshold, equipRef)
    val co2Target = Point(DomainName.co2Target, equipRef)
    val pm25Target = Point(DomainName.pm25Target, equipRef)
    val pm10Target = Point(DomainName.pm10Target, equipRef)
    val co2DamperOpeningRate = Point(DomainName.co2DamperOpeningRate, equipRef)

    // Sensor Points
    val zoneHumidity = Point(DomainName.zoneHumidity, equipRef)
    val zoneCo2 = Point(DomainName.zoneCo2, equipRef)
    val zonePm25 = Point(DomainName.zonePm25, equipRef)
    val zoneIlluminance = Point(DomainName.zoneIlluminance, equipRef)
    val zoneOccupancy = Point(DomainName.zoneOccupancy, equipRef)
    val zoneSound = Point(DomainName.zoneSound, equipRef)
    val currentTemp = Point(DomainName.currentTemp, equipRef)
    val dischargeAirTemperature = Point(DomainName.dischargeAirTemperature, equipRef)
    val airFlowSensor = Point(DomainName.airFlowSensor, equipRef)
    val mixedAirTemperature = Point(DomainName.mixedAirTemperature, equipRef)
    val outsideTemperature = Point(DomainName.outsideTemperature, equipRef)
    val supplyAirHumidity = Point(DomainName.supplyAirHumidity, equipRef)
    val mixedAirHumidity = Point(DomainName.mixedAirHumidity, equipRef)
    val outsideHumidity = Point(DomainName.outsideHumidity, equipRef)
    val leavingWaterTemperature = Point(DomainName.leavingWaterTemperature, equipRef)
    val ductStaticPressureSensor = Point(DomainName.ductStaticPressureSensor, equipRef)
    val emergencyShutoffNO = Point(DomainName.emergencyShutoffNO, equipRef)
    val emergencyShutoffNC = Point(DomainName.emergencyShutoffNC, equipRef)
    val filterStatusNO = Point(DomainName.filterStatusNO, equipRef)
    val filterStatusNC = Point(DomainName.filterStatusNC, equipRef)
    val condensateStatusNO = Point(DomainName.condensateStatusNO, equipRef)
    val condensateStatusNC = Point(DomainName.condensateStatusNC, equipRef)
    val ductStaticPressureSensor1_1 = Point(DomainName.ductStaticPressureSensor1_1, equipRef)
    val ductStaticPressureSensor1_2 = Point(DomainName.ductStaticPressureSensor1_2, equipRef)
    val ductStaticPressureSensor1_10 = Point(DomainName.ductStaticPressureSensor1_10, equipRef)


    // User Indent Points
    val targetHumidifier = Point(DomainName.targetHumidifier, equipRef)
    val targetDehumidifier = Point(DomainName.targetDehumidifier, equipRef)
    val conditioningMode = Point(DomainName.conditioningMode, equipRef)
    val fanOpMode = Point(DomainName.fanOpMode, equipRef)
    val scheduleType = Point(DomainName.scheduleType, equipRef)
    val desiredTempCooling = Point(DomainName.desiredTempCooling, equipRef)
    val desiredTempHeating = Point(DomainName.desiredTempHeating, equipRef)

    // Door Window & Keycard points
    val keyCardSensingEnable = Point(DomainName.keyCardSensingEnable, equipRef)
    val doorWindowSensingEnable = Point(DomainName.doorWindowSensingEnable, equipRef)
    val doorWindowSensorInput = Point(DomainName.doorWindowSensorInput, equipRef)
    val keyCardSensorInput = Point(DomainName.keyCardSensorInput, equipRef)

    val doorWindowSensorNCTitle24 = Point(DomainName.doorWindowSensorNCTitle24, equipRef)
    val doorWindowSensorNOTitle24 = Point(DomainName.doorWindowSensorNOTitle24, equipRef)
    val doorWindowSensorNC = Point(DomainName.doorWindowSensorNC, equipRef)
    val doorWindowSensorNO = Point(DomainName.doorWindowSensorNO, equipRef)
    val doorWindowSensorTitle24 = Point(DomainName.doorWindowSensorTitle24, equipRef)
    val doorWindowSensor = Point(DomainName.doorWindowSensor, equipRef)
    val keyCardSensor = Point(DomainName.keyCardSensor, equipRef)
    val keyCardSensorNO = Point(DomainName.keyCardSensorNO, equipRef)
    val keyCardSensorNC = Point(DomainName.keyCardSensorNC, equipRef)
}