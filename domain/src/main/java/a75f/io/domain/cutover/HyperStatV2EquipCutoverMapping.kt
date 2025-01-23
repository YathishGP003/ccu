package a75f.io.domain.cutover

import a75f.io.domain.api.DomainName


object HyperStatV2EquipCutoverMapping {

    private val commonEntries = linkedMapOf(
            "temperatureOffset" to DomainName.temperatureOffset,
            "autoawayEnabled" to DomainName.autoAway,
            "autoForceOccupiedEnabled" to DomainName.autoForceOccupied,

            "relay1OutputEnabled" to DomainName.relay1OutputEnable,
            "relay2OutputEnabled" to DomainName.relay2OutputEnable,
            "relay3OutputEnabled" to DomainName.relay3OutputEnable,
            "relay4OutputEnabled" to DomainName.relay4OutputEnable,
            "relay5OutputEnabled" to DomainName.relay5OutputEnable,
            "relay6OutputEnabled" to DomainName.relay6OutputEnable,

            "relay1OutputAssociation" to DomainName.relay1OutputAssociation,
            "relay2OutputAssociation" to DomainName.relay2OutputAssociation,
            "relay3OutputAssociation" to DomainName.relay3OutputAssociation,
            "relay4OutputAssociation" to DomainName.relay4OutputAssociation,
            "relay5OutputAssociation" to DomainName.relay5OutputAssociation,
            "relay6OutputAssociation" to DomainName.relay6OutputAssociation,

            "analogIn1Enabled" to DomainName.analog1InputEnable,
            "analogIn2Enabled" to DomainName.analog2InputEnable,
            "analogIn1Association" to DomainName.analog1InputAssociation,
            "analogIn2Association" to DomainName.analog2InputAssociation,

            "analogOut1Enabled" to DomainName.analog1OutputEnable,
            "analogOut2Enabled" to DomainName.analog2OutputEnable,
            "analogOut3Enabled" to DomainName.analog3OutputEnable,
            "analogOut1Association" to DomainName.analog1OutputAssociation,
            "analogOut2Association" to DomainName.analog2OutputAssociation,
            "analogOut3Association" to DomainName.analog3OutputAssociation,

            "thIn1Enabled" to DomainName.thermistor1InputEnable,
            "thIn2Enabled" to DomainName.thermistor2InputEnable,

            "thIn1Association" to DomainName.thermistor1InputAssociation,
            "thIn2Association" to DomainName.thermistor2InputAssociation,

            "equipStatus" to DomainName.equipStatus,
            "equipStatusMessage" to DomainName.equipStatusMessage,
            "equipScheduleStatus" to DomainName.equipScheduleStatus,
            "OperatingMode" to DomainName.operatingMode,
            "co2DamperOpeningRate" to DomainName.co2DamperOpeningRate,
            "zoneCO2Threshold" to DomainName.co2Threshold,
            "zoneCO2Target" to DomainName.co2Target,
            "zonePm2p5Target" to DomainName.pm25Target,

            "coolingLoopOutput" to DomainName.coolingLoopOutput,
            "heatingLoopOutput" to DomainName.heatingLoopOutput,
            "fanLoopOutput" to DomainName.fanLoopOutput,
            "FanOpMode" to DomainName.fanOpMode,
            "ConditioningMode" to DomainName.conditioningMode,
            "targetHumidifier" to DomainName.targetHumidifier,
            "targetDehumidifier" to DomainName.targetDehumidifier,
            "humidityDisplayEnabled" to DomainName.enableHumidityDisplay,
            "co2DisplayEnabled" to DomainName.enableCO2Display,
            "pm25DisplayEnabled" to DomainName.enablePm25Display,
            "heartBeat" to DomainName.heartBeat,
            "scheduleType" to DomainName.scheduleType,
            "desiredTemp" to DomainName.desiredTemp,
            "desiredTempCooling" to DomainName.desiredTempCooling,
            "desiredTempHeating" to DomainName.desiredTempHeating,

            "fanEnabled" to DomainName.fanEnable,
            "occupiedEnabled" to DomainName.occupiedEnable,
            "humidifierEnableCmd" to DomainName.humidifierEnable,
            "dehumidifierEnableCmd" to DomainName.dehumidifierEnable,
            "fanlowspeed" to DomainName.fanLowSpeed,
            "fanMediumSpeed" to DomainName.fanMediumSpeed,
            "fanHighSpeed" to DomainName.fanHighSpeed,

            "dcvDamper" to DomainName.dcvDamperModulating,
            "analog1Mindcvdamper" to DomainName.analog1MinDCVDamper,
            "analog1Maxdcvdamper" to DomainName.analog1MaxDCVDamper,
            "analog2Mindcvdamper" to DomainName.analog2MinDCVDamper,
            "analog2Maxdcvdamper" to DomainName.analog2MaxDCVDamper,
            "analog3Mindcvdamper" to DomainName.analog3MinDCVDamper,
            "analog3Maxdcvdamper" to DomainName.analog3MaxDCVDamper,

            "analog1FanLow" to DomainName.analog1FanLow,
            "analog2FanLow" to DomainName.analog2FanLow,
            "analog3FanLow" to DomainName.analog3FanLow,
            "analog1FanMedium" to DomainName.analog1FanMedium,
            "analog2FanMedium" to DomainName.analog2FanMedium,
            "analog3FanMedium" to DomainName.analog3FanMedium,
            "analog1FanHigh" to DomainName.analog1FanHigh,
            "analog2FanHigh" to DomainName.analog2FanHigh,
            "analog3FanHigh" to DomainName.analog3FanHigh,
            "currentTemp" to DomainName.currentTemp,
            "airflowTempSensor" to DomainName.dischargeAirTemperature,
            "genericFaultNC" to DomainName.genericAlarmNC,
            "genericFaultNO" to DomainName.genericAlarmNO,

            "currentDrawn_10" to DomainName.currentTx10,
            "currentDrawn_20" to DomainName.currentTx20,
            "currentDrawn_50" to DomainName.currentTx50,
            "keyCardSensor" to DomainName.keyCardSensor,
            "keyCardSensor_2" to DomainName.keyCardSensor,

            "doorWindowSensor" to DomainName.doorWindowSensorNCTitle24, // thermistor
            "doorWindowSensor_2" to DomainName.doorWindowSensorTitle24, // analogin1
            "doorWindowSensor_3" to DomainName.doorWindowSensorTitle24, // analogi22

            "keycardSensingEnabled" to DomainName.keyCardSensingEnable,
            "keycardSensorInput" to DomainName.keyCardSensorInput,
            "windowSensingEnabled" to DomainName.doorWindowSensingEnable,
            "windowSensorInput" to DomainName.doorWindowSensorInput,
            "standaloneHeatingDeadbandMultiplier" to DomainName.standaloneHeatingDeadbandMultiplier,
            "abnormalCurTempRiseTrigger" to DomainName.abnormalCurTempRiseTrigger,
            "standaloneCoolingDeadbandMultiplier" to DomainName.standaloneCoolingDeadbandMultiplier,
            "forcedOccupiedTime" to DomainName.forcedOccupiedTime,
            "autoAwayTime" to DomainName.autoAwayTime,
            "autoAwaySetback" to DomainName.autoAwaySetback,
            "standaloneTemperatureProportionalRange" to DomainName.standaloneTemperatureProportionalRange,
            "standaloneTemperatureIntegralTime" to DomainName.standaloneTemperatureIntegralTime,
            "constantTempAlertTime" to DomainName.constantTempAlertTime,
            "standaloneHumidityHysteresis" to DomainName.standaloneHumidityHysteresis, // ok
            "standaloneRelayActivationHysteresis" to DomainName.standaloneRelayActivationHysteresis,
            "standaloneAnalogFanSpeedMultiplier" to DomainName.standaloneAnalogFanSpeedMultiplier,
            "standaloneMinFanRuntimePostConditioning" to DomainName.minFanRuntimePostConditioning,
            "standaloneProportionalKFactor" to DomainName.standaloneProportionalKFactor,
            "standaloneCoolingPreconditioningRate" to DomainName.standaloneCoolingPreconditioningRate,
            "standaloneHeatingPreconditioningRate" to DomainName.standaloneHeatingPreconditioningRate,
            "zoneDeadTime" to DomainName.zoneDeadTime,
            "coolingDeadband" to DomainName.coolingDeadband,
            "standaloneIntegralKFactor" to DomainName.standaloneIntegralKFactor,
            "DemandResponseSetback" to DomainName.demandResponseSetback,
            "otaStatus" to DomainName.otaStatus,
            "co2" to DomainName.zoneCo2,
            "occupancysensor" to DomainName.zoneOccupancy,
            "zoneilluminance" to DomainName.zoneIlluminance,
            "zonehumidity" to DomainName.zoneHumidity,
            "pm2p5" to DomainName.zonePm25,
            "sound" to DomainName.zoneSound,
            "co2Equivalent" to DomainName.zoneCo2Equivalent,
            "uvi" to DomainName.zoneUvi,
            "pm10" to DomainName.zonePm10,
            "pressure" to DomainName.zonePressureSensor,
            "voc" to DomainName.zoneVoc,
            "zoneOccupancy" to DomainName.occupancyMode,
            "occupancyDetection" to DomainName.occupancyDetection,
    )


    private val cpuEntries = linkedMapOf(

            "coolingStage1" to DomainName.coolingStage1,
            "coolingStage2" to DomainName.coolingStage2,
            "coolingStage3" to DomainName.coolingStage3,
            "heatingStage1" to DomainName.heatingStage1,
            "heatingStage2" to DomainName.heatingStage2,
            "heatingStage3" to DomainName.heatingStage3,

            "modulatingCooling" to DomainName.coolingSignal,
            "analog1Mincooling" to DomainName.analog1MinCooling,
            "analog1Maxcooling" to DomainName.analog1MaxCooling,
            "analog2Mincooling" to DomainName.analog2MinCooling,
            "analog2Maxcooling" to DomainName.analog2MaxCooling,
            "analog3Mincooling" to DomainName.analog3MinCooling,
            "analog3Maxcooling" to DomainName.analog3MaxCooling,

            "modulatingHeating" to DomainName.heatingSignal,
            "analog1Minheating" to DomainName.analog1MinHeating,
            "analog1Maxheating" to DomainName.analog1MaxHeating,
            "analog2Minheating" to DomainName.analog2MinHeating,
            "analog2Maxheating" to DomainName.analog2MaxHeating,
            "analog3Minheating" to DomainName.analog3MinHeating,
            "analog3Maxheating" to DomainName.analog3MaxHeating,

            "modulatingFanSpeed" to DomainName.linearFanSpeed,
            "analog1Minfanspeed" to DomainName.analog1MinLinearFanSpeed,
            "analog1Maxfanspeed" to DomainName.analog1MaxLinearFanSpeed,
            "analog2Minfanspeed" to DomainName.analog2MinLinearFanSpeed,
            "analog2Maxfanspeed" to DomainName.analog2MaxLinearFanSpeed,
            "analog3Minfanspeed" to DomainName.analog3MinLinearFanSpeed,
            "analog3Maxfanspeed" to DomainName.analog3MaxLinearFanSpeed,

            "predefinedFanSpeed" to DomainName.stagedFanSpeed,
            "fanOutCoolingStage1" to DomainName.fanOutCoolingStage1,
            "fanOutCoolingStage2" to DomainName.fanOutCoolingStage2,
            "fanOutCoolingStage3" to DomainName.fanOutCoolingStage3,
            "fanOutHeatingStage1" to DomainName.fanOutHeatingStage1,
            "fanOutHeatingStage2" to DomainName.fanOutHeatingStage2,
            "fanOutHeatingStage3" to DomainName.fanOutHeatingStage3,
            "fanOutRecirculateAnalog1" to DomainName.analog1FanRecirculate,
            "fanOutRecirculateAnalog2" to DomainName.analog2FanRecirculate,
            "fanOutRecirculateAnalog3" to DomainName.analog3FanRecirculate,
    )

    private val hpuEntries = linkedMapOf(
            "compressorStage1" to DomainName.compressorStage1,
            "compressorStage2" to DomainName.compressorStage2,
            "compressorStage3" to DomainName.compressorStage3,

            "auxHeatingstage1" to DomainName.auxHeatingStage1,
            "auxHeatingstage2" to DomainName.auxHeatingStage2,

            "compressorSpeed" to DomainName.compressorSpeed,
            "fanSpeed" to DomainName.fanSignal,
            "analog1Maxfanspeed" to DomainName.analog1MaxFanSpeed,
            "analog1Minfanspeed" to DomainName.analog1MinFanSpeed,
            "analog2Maxfanspeed" to DomainName.analog2MaxFanSpeed,
            "analog2Minfanspeed" to DomainName.analog2MinFanSpeed,
            "analog3Maxfanspeed" to DomainName.analog3MaxFanSpeed,
            "analog3Minfanspeed" to DomainName.analog3MinFanSpeed,

            "analog1Mincompressorspeed" to DomainName.analog1MinCompressorSpeed,
            "analog1Maxcompressorspeed" to DomainName.analog1MaxCompressorSpeed,
            "analog2Mincompressorspeed" to DomainName.analog2MinCompressorSpeed,
            "analog2Maxcompressorspeed" to DomainName.analog2MaxCompressorSpeed,
            "analog3Mincompressorspeed" to DomainName.analog3MinCompressorSpeed,
            "analog3Maxcompressorspeed" to DomainName.analog3MaxCompressorSpeed,
            "compressorLoopOutput" to DomainName.compressorLoopOutput,
            "auxHeating1Activate" to DomainName.auxHeating1Activate,
            "auxHeating2Activate" to DomainName.auxHeating2Activate,
            "coolingAirflowTemp" to DomainName.coolingAirflowTemp,
            "changeOverCooling" to DomainName.changeOverCooling,
            "changeOverHeating" to DomainName.changeOverHeating,
            "heatingAirflowTemp" to DomainName.heatingAirflowTemp,
    )

    private val pipe2Entries = linkedMapOf(
            // Relay mapping
            "waterValve" to DomainName.waterValve,
            "auxHeatingstage1" to DomainName.auxHeatingStage1,
            "auxHeatingstage2" to DomainName.auxHeatingStage2,

            //min max
            "analog1Minwater" to DomainName.analog1MinWaterValve,
            "analog2Minwater" to DomainName.analog2MinWaterValve,
            "analog3Minwater" to DomainName.analog3MinWaterValve,

            "analog1Maxwater" to DomainName.analog1MaxWaterValve,
            "analog2Maxwater" to DomainName.analog2MaxWaterValve,
            "analog3Maxwater" to DomainName.analog3MaxWaterValve,

            "analog1Minfanspeed" to DomainName.analog1MinFanSpeed,
            "analog2Minfanspeed" to DomainName.analog2MinFanSpeed,
            "analog3Minfanspeed" to DomainName.analog3MinFanSpeed,

            "analog1Maxfanspeed" to DomainName.analog1MaxFanSpeed,
            "analog2Maxfanspeed" to DomainName.analog2MaxFanSpeed,
            "analog3Maxfanspeed" to DomainName.analog3MaxFanSpeed,

            // 2pipe specific points tuners
            "auxHeating1Activate" to DomainName.auxHeating1Activate,
            "auxHeating2Activate" to DomainName.auxHeating2Activate,
            "fanSpeed" to DomainName.fanSignal,

            "coolingAirflowTemp" to DomainName.coolingAirflowTemp,
            "heatingAirflowTemp" to DomainName.heatingAirflowTemp,
            "modulatingWaterValve" to DomainName.modulatingWaterValve,
            "supplyWaterTemp" to DomainName.leavingWaterTemperature,
            "2PipeFancoilHeatingThreshold" to DomainName.hyperstatPipe2FancoilHeatingThreshold,
            "2PipeFancoilCoolingThreshold" to DomainName.hyperstatPipe2FancoilCoolingThreshold,
            "waterValveSamplingOnTime" to DomainName.waterValveSamplingOnTime,
            "waterValveSamplingWaitTime" to DomainName.waterValveSamplingWaitTime,
            "waterValveSamplingDuringLoopDeadbandOnTime" to DomainName.waterValveSamplingLoopDeadbandOnTime,
            "waterValveSamplingDuringLoopDeadbandWaitTime" to DomainName.waterValveSamplingLoopDeadbandWaitTime,

            )
    private val monitoringEntries = linkedMapOf(
            // Equip - base Points
            "temperatureOffset" to DomainName.temperatureOffset,

            "isThermister1enabled" to DomainName.thermistor1InputEnable,
            "isThermister2enabled" to DomainName.thermistor2InputEnable,

            //
            "isAnalog1enaled" to DomainName.analog1InputEnable,
            "isAnalog2enabled" to DomainName.analog2InputEnable,

            "th1InputSensor" to DomainName.thermistor1InputAssociation,
            "th2InputSensor" to DomainName.thermistor2InputAssociation,

            "analog1InputSensor" to DomainName.analog1InputAssociation,
            "analog2InputSensor" to DomainName.analog2InputAssociation,

            "currentTemp" to DomainName.currentTemp,
            "occupancy" to DomainName.zoneOccupancy,
            "illuminance" to DomainName.zoneIlluminance,
            "heartBeat" to DomainName.heartBeat,
            "otaStatus" to DomainName.otaStatus,
            "scheduleType" to DomainName.scheduleType,

                // For Equip Associated Points we are handling separately
                "pm2p5" to DomainName.zonePm25,
                "sound" to DomainName.zoneSound,
                "co2Equivalent" to DomainName.zoneCo2Equivalent,
                "uvi" to DomainName.zoneUvi,
                "pm10" to DomainName.zonePm10,
                "pressure" to DomainName.zonePressureSensor,
                "voc" to DomainName.zoneVoc
                )
        fun getCPUEntries(): Map<String, String> {
                return commonEntries + cpuEntries
        }

        fun getMonitoringTh1Entries(): Map<String, String> {
                val th1 = linkedMapOf(
                        "10Ktype2probe" to DomainName.externalAirTempSensor_th1,
                        //Below "100)kohms" is the point of Generic(1-100)kohms,
                        "100)kohms" to DomainName.airTempSensor100kOhms_th1,
                        "GenericFault(NC)" to DomainName.genericAlarmNC_th1,
                        "GenericFault(NO)" to DomainName.genericAlarmNO_th1,)
                return th1
        }

        fun getMonitoringTh2Entries(): Map<String, String> {
                val th1 = linkedMapOf(
                        "10Ktype2probe" to DomainName.externalAirTempSensor_th2,
                        //Below "100)kohms" is the point of Generic(1-100)kohms,
                        "100)kohms" to DomainName.airTempSensor100kOhms_th2,
                        "GenericFault(NC)" to DomainName.genericAlarmNC_th2,
                        "GenericFault(NO)" to DomainName.genericAlarmNO_th2,
                        )
                return th1
        }
        fun getMonitoringAnalog1Entries(): Map<String, String> {
                val analog = linkedMapOf(
                "Generic(0:10)V" to DomainName.voltageInput_ai1,
                "PressureSensor(0:2)inH₂O" to DomainName.ductStaticPressureSensor1_2_ai1,
                "DifferentialPressureSensor(0:0.25)inH₂O" to DomainName.differentialAirPressureSensor_025_ai1,
                "AirflowSensor(0:1000)cfm" to DomainName.airFlowSensor_ai1,
                "Humidity(0:100)%" to DomainName.zoneHumidity_ai1,
                "CO2(0:2000)ppm" to DomainName.zoneCo2_ai1,
                "CO(0:100)ppm" to DomainName.zoneCo_ai1,
                "NO2(0:5)ppm" to DomainName.zoneNo2_ai1,
                "CT(0:10)amps" to DomainName.currentTx10_ai1,
                "CT(0:20)amps" to DomainName.currentTx20_ai1,
                "CT(0:50)amps" to DomainName.currentTx50_ai1,
                "IONMeter(0:1Million)ions/cc" to DomainName.genericIonSensorPoint_ai1,
                        )
                return analog
        }
        fun getMonitoringAnalog2Entries(): Map<String, String> {
                val analog = linkedMapOf(
                        "Generic(0:10)V" to DomainName.voltageInput_ai2,
                        "PressureSensor(0:2)inH₂O" to DomainName.ductStaticPressureSensor1_2_ai2,
                        "DifferentialPressureSensor(0:0.25)inH₂O" to DomainName.differentialAirPressureSensor_025_ai2,
                        "AirflowSensor(0:1000)cfm" to DomainName.airFlowSensor_ai2,
                        "Humidity(0:100)%" to DomainName.zoneHumidity_ai2,
                        "CO2(0:2000)ppm" to DomainName.zoneCo2_ai2,
                        "CO(0:100)ppm" to DomainName.zoneCo_ai2,
                        "NO2(0:5)ppm" to DomainName.zoneNo2_ai2,
                        "CT(0:10)amps" to DomainName.currentTx10_ai2,
                        "CT(0:20)amps" to DomainName.currentTx20_ai2,
                        "CT(0:50)amps" to DomainName.currentTx50_ai2,
                        "IONMeter(0:1Million)ions/cc" to DomainName.genericIonSensorPoint_ai2,)
                return analog
        }
    fun getMonitoringEntries(): Map<String, String> {
        return monitoringEntries
    }

    fun getHPUEntries(): Map<String, String> {
        return commonEntries + hpuEntries
    }

    fun getPipe2Entries(): Map<String, String> {
        return commonEntries + pipe2Entries
    }
}

