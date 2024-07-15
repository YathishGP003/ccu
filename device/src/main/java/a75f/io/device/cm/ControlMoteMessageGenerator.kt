package a75f.io.device.cm

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.ControlMote
import a75f.io.device.ControlMote.CcuToCmSettingsMessage_t.Builder
import a75f.io.device.ControlMote.CmAnalogInMappingStages_e
import a75f.io.device.ControlMote.CmAnalogOutConfig_t
import a75f.io.device.ControlMote.SAToperatingMode_e
import a75f.io.device.mesh.MeshUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.toInt
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.getAnalogOut1MinMax
import a75f.io.logic.bo.building.system.getAnalogOut2MinMax
import a75f.io.logic.bo.building.system.getAnalogOut3MinMax
import a75f.io.logic.bo.building.system.getAnalogOut4MinMax
import a75f.io.logic.bo.building.system.util.getAdvancedAhuSystemEquip
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu

//Decimal values are multiplied by 10 to keep precision since all the values are send as integers.z
const val SERIAL_COMM_SCALE = 10
fun getCMControlsMessage(): ControlMote.CcuToCmOverUsbCmControlMessage_t {

    val systemEquip = getAdvancedAhuSystemEquip()
    val cmDevice = Domain.cmBoardDevice
    val msgBuilder = ControlMote.CcuToCmOverUsbCmControlMessage_t.newBuilder()
    var relayBitmap = 0

    for (relayPos in 1..8) {
        if (CCUHsApi.getInstance().readHisValByQuery(
                "point and physical and deviceRef == \""
                        + Domain.cmBoardDevice.getId() + "\" " +
                        "and domainName == \"relay" + relayPos + "\""
            ) > 0
        ) {
            relayBitmap = relayBitmap or (1 shl MeshUtil.getRelayMapping(relayPos))
        }
    }

    msgBuilder.apply {
        addAnalogOut(cmDevice.analog1Out.readHisVal() .toInt())
        addAnalogOut(cmDevice.analog2Out.readHisVal().toInt())
        addAnalogOut(cmDevice.analog3Out.readHisVal().toInt())
        addAnalogOut(cmDevice.analog4Out.readHisVal().toInt())
        relayBitMap = relayBitmap
        addPiloopSetPoint(systemEquip.airTempCoolingSp.readHisVal().toInt() * 10)
        addPiloopSetPoint(systemEquip.airTempHeatingSp.readHisVal().toInt() * 10)
        addPiloopSetPoint(systemEquip.ductStaticPressureSetpoint.readHisVal().toInt() * 10)
        addPiloopSetPoint(systemEquip.zoneAvgCo2.readHisVal().toInt() * 10)
        saToperatingMode = SAToperatingMode_e.forNumber(systemEquip.operatingMode.readHisVal().toInt())
                    ?: SAToperatingMode_e.SAT_OPERATING_MODE_OFF
        emergencyShutOff = (L.ccu().systemProfile as VavAdvancedAhu)?.isEmergencyShutoffActive()?.toInt() ?: 0 // TODO
        unoccupiedMode = getOccupancy()
    }
    return msgBuilder.build()
}

private fun getOccupancy(): ControlMote.UnoccupiedMode_e {
    val occupancy = (L.ccu().systemProfile as VavAdvancedAhu)?.getOccupancy()  // TODO
    return if (occupancy == 0) {
        ControlMote.UnoccupiedMode_e.UNOCCUPIED_MODE
    } else {
        ControlMote.UnoccupiedMode_e.OCCUPIED_MODE
    }
}

fun getCMSettingsMessage() : ControlMote.CcuToCmSettingsMessage_t {
    val msgBuilder = ControlMote.CcuToCmSettingsMessage_t.newBuilder()
    msgBuilder.apply {
        cmProfile = ControlMote.CMProfiles_e.CM_PROFILE_ADV_AHU
        addTuners(this)
        addRelayConfigsToSettingsMessage(this)
        addAnalogOutConfigsToSettingsMessage(this)
        addAnalogInConfigsToSettingsMessage(this)
        addThermistorConfigsToSettingsMessage(this)
        addPILoopConfiguration(this)
        addPILoopConfiguration(this)
        addPILoopConfiguration(this)
        addPILoopConfiguration(this)
        addSensorConfigs(this)
        addSensorBusMappings(this)
        addTestSignals(this)
    }
    return msgBuilder.build()
}



fun addTuners(builder: Builder) {
    if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        val systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
        builder.apply {
            relayActivationHysteresis = systemEquip.vavRelayDeactivationHysteresis.readHisVal().toInt()
            analogFanSpeedMultiplier = systemEquip.vavAnalogFanSpeedMultiplier.readHisVal().toInt() * 10
            cmStageUpTimer = systemEquip.vavStageUpTimerCounter.readHisVal().toInt()
            cmStageDownTimer = systemEquip.vavStageDownTimerCounter.readHisVal().toInt()
        }
    } else {
        val systemEquip = Domain.systemEquip as DabAdvancedHybridSystemEquip
        builder.apply {
            relayActivationHysteresis = systemEquip.dabRelayDeactivationHysteresis.readHisVal().toInt()
            analogFanSpeedMultiplier = systemEquip.dabAnalogFanSpeedMultiplier.readHisVal().toInt() * 10
            cmStageUpTimer = systemEquip.dabStageUpTimerCounter.readHisVal().toInt()
            cmStageDownTimer = systemEquip.dabStageDownTimerCounter.readHisVal().toInt()
        }
    }
}

fun addRelayConfigsToSettingsMessage(builder: Builder) {
    /*
        [ Y1 Y2 G1 G2 W1 W2 AUX (mappings)]
        CCU needs to send in this format - [ R1 R2 R3 R6 R4 R5 R7 R8 (mappings)]
        Because in hardware it is mapper in this way. please do not change the position
    */
    val systemEquip = getAdvancedAhuSystemEquip()
    builder.apply {
        if (systemEquip.relay1OutputEnable.readDefaultVal() > 0) {
            val relay1OutputAssociation = systemEquip.relay1OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay1OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }
        if (systemEquip.relay2OutputEnable.readDefaultVal() > 0) {
            val relay2OutputAssociation = systemEquip.relay2OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay2OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }
        if (systemEquip.relay3OutputEnable.readDefaultVal() > 0) {
            val relay3OutputAssociation = systemEquip.relay3OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay3OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }
        if (systemEquip.relay6OutputEnable.readDefaultVal() > 0) {
            val relay6OutputAssociation = systemEquip.relay6OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay6OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }

        if (systemEquip.relay4OutputEnable.readDefaultVal() > 0) {
            val relay4OutputAssociation = systemEquip.relay4OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay4OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }
        if (systemEquip.relay5OutputEnable.readDefaultVal() > 0) {
            val relay5OutputAssociation = systemEquip.relay5OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay5OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }

        if (systemEquip.relay7OutputEnable.readDefaultVal() > 0) {
            val relay7OutputAssociation = systemEquip.relay7OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay7OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }
        if (systemEquip.relay8OutputEnable.readDefaultVal() > 0) {
            val relay8OutputAssociation = systemEquip.relay8OutputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmRelayMappingStages_e.forNumber(relay8OutputAssociation + 1) ?: ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED
            addRelayMapping(mappingStage)
        } else {
            addRelayMapping(ControlMote.CmRelayMappingStages_e.RELAY_NOT_ENABLED)
        }
    }
}

fun addAnalogOutConfigsToSettingsMessage(builder: Builder) {

    val systemEquip = getAdvancedAhuSystemEquip()
    builder.apply {
        val analog1OutBuilder = CmAnalogOutConfig_t.newBuilder()
        if (systemEquip.analog1OutputEnable.readDefaultVal() > 0) {
            val analog1OutAssociation =
                systemEquip.analog1OutputAssociation.readDefaultVal().toInt()
            analog1OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.forNumber(analog1OutAssociation + 1)
                    ?: ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED

            val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[analog1OutAssociation]
            val (min, max) = getAnalogOut1MinMax(analogOutAssociationType, systemEquip)
            analog1OutBuilder.analogOutMin = min.toInt() * 10
            analog1OutBuilder.analogOutMax = max.toInt() * 10
        } else {
            analog1OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED
        }
        addAnalogOutConfig(analog1OutBuilder.build())

        val analog2OutBuilder = CmAnalogOutConfig_t.newBuilder()
        if (systemEquip.analog2OutputEnable.readDefaultVal() > 0) {
            val analog2OutAssociation =
                systemEquip.analog2OutputAssociation.readDefaultVal().toInt()
            analog2OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.forNumber(analog2OutAssociation + 1)
                    ?: ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED
            val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[analog2OutAssociation]
            val (min, max) = getAnalogOut2MinMax(analogOutAssociationType, systemEquip)
            analog2OutBuilder.analogOutMin = min.toInt() * 10
            analog2OutBuilder.analogOutMax = max.toInt() * 10
        } else {
            analog2OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED
        }
        addAnalogOutConfig(analog2OutBuilder.build())

        val analog3OutBuilder = CmAnalogOutConfig_t.newBuilder()
        if (systemEquip.analog3OutputEnable.readDefaultVal() > 0) {
            val analog3OutAssociation =
                systemEquip.analog3OutputAssociation.readDefaultVal().toInt()
            analog3OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.forNumber(analog3OutAssociation + 1)
                    ?: ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED
            val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[analog3OutAssociation]
            val (min, max) = getAnalogOut3MinMax(analogOutAssociationType, systemEquip)
            analog3OutBuilder.analogOutMin = min.toInt() * 10
            analog3OutBuilder.analogOutMax = max.toInt() * 10
        } else {
            analog3OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED
        }
        addAnalogOutConfig(analog3OutBuilder.build())

        val analog4OutBuilder = CmAnalogOutConfig_t.newBuilder()
        if (systemEquip.analog4OutputEnable.readDefaultVal() > 0) {
            val analog4OutAssociation =
                systemEquip.analog4OutputAssociation.readDefaultVal().toInt()
            analog4OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.forNumber(analog4OutAssociation + 1)
                    ?: ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED
            val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[analog4OutAssociation]
            val (min, max) = getAnalogOut4MinMax(analogOutAssociationType, systemEquip)
            analog4OutBuilder.analogOutMin = min.toInt() * 10
            analog4OutBuilder.analogOutMax = max.toInt() * 10
        } else {
            analog4OutBuilder.analogOutMapping =
                ControlMote.CmAnalogOutMappingStages_e.AOUT_NOT_ENABLED
        }
        addAnalogOutConfig(analog4OutBuilder.build())
    }
}

fun addAnalogInConfigsToSettingsMessage(builder: Builder) {
    val systemEquip = getAdvancedAhuSystemEquip()
    builder.apply {
        if (systemEquip.analog1InputEnable.readDefaultVal() > 0) {
            val analog1InputAssociation = systemEquip.analog1InputAssociation.readDefaultVal().toInt()
            val mappingStage = CmAnalogInMappingStages_e.forNumber(analog1InputAssociation) ?: CmAnalogInMappingStages_e.AIN_NOT_ENABLED
            addAnalogInMapping(mappingStage)
        } else {
            addAnalogInMapping(CmAnalogInMappingStages_e.AIN_NOT_ENABLED)
        }
        if (systemEquip.analog2InputEnable.readDefaultVal() > 0) {
            val analog2InputAssociation = systemEquip.analog2InputAssociation.readDefaultVal().toInt()
            val mappingStage = CmAnalogInMappingStages_e.forNumber(analog2InputAssociation) ?: CmAnalogInMappingStages_e.AIN_NOT_ENABLED
            addAnalogInMapping(mappingStage)
        } else {
            addAnalogInMapping(CmAnalogInMappingStages_e.AIN_NOT_ENABLED)
        }
    }
}

fun addThermistorConfigsToSettingsMessage(builder: Builder) {
    val systemEquip = getAdvancedAhuSystemEquip()
    builder.apply {
        if (systemEquip.thermistor1InputEnable.readDefaultVal() > 0) {
            val thermistor1InputAssociation = systemEquip.thermistor1InputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmThermistorMappingStages_e.forNumber(thermistor1InputAssociation) ?: ControlMote.CmThermistorMappingStages_e.TH_NOT_ENABLED
            addThermistorMapping(mappingStage)
        } else {
            addThermistorMapping(ControlMote.CmThermistorMappingStages_e.TH_NOT_ENABLED)
        }
        if (systemEquip.thermistor2InputEnable.readDefaultVal() > 0) {
            val thermistor2InputAssociation = systemEquip.thermistor2InputAssociation.readDefaultVal().toInt()
            val mappingStage = ControlMote.CmThermistorMappingStages_e.forNumber(thermistor2InputAssociation) ?: ControlMote.CmThermistorMappingStages_e.TH_NOT_ENABLED
            addThermistorMapping(mappingStage)
        } else {
            addThermistorMapping(ControlMote.CmThermistorMappingStages_e.TH_NOT_ENABLED)
        }

    }
}

fun addPILoopConfiguration(builder: Builder) {

     if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
         val systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
        //TODO - Needed only if SAT/PRESSURE based control needed.
        builder.apply {
            val piLoopConfigBuilder = ControlMote.PiLoopConfiguration_t.newBuilder()
            //piLoopConfigBuilder.piLoopDataType = ControlMote.PiLoopDataType_e.SUPPLY_AIR_TEMPERATURE
            piLoopConfigBuilder.enable = true
            piLoopConfigBuilder.proportionalConstant = (systemEquip.vavProportionalKFactor.readPriorityVal() * SERIAL_COMM_SCALE).toInt() * 10
            piLoopConfigBuilder.integralConstant = (systemEquip.vavIntegralKFactor.readPriorityVal() * SERIAL_COMM_SCALE).toInt() * 10
            piLoopConfigBuilder.proportionalTemperatureRange = systemEquip.vavTemperatureProportionalRange.readPriorityVal().toInt() * 10
            piLoopConfigBuilder.integrationTime = systemEquip.vavTemperatureIntegralTime.readPriorityVal().toInt()
            addPiLoopConfiguration(piLoopConfigBuilder.build())
        }

    }
    // TODO implement for DAB Profile
}

fun addSensorConfigs(builder: Builder) {
    val systemEquip = getAdvancedAhuSystemEquip()
    builder.apply {
        if (systemEquip.supplyAirTempControlOn.pointExists()) {
            supplyAirTempSensorOp = ControlMote.SupplyAirTempOperation_e.
                        forNumber(systemEquip.supplyAirTempControlOn.readDefaultVal().toInt())
        }
        if (systemEquip.pressureBasedFanControlOn.pointExists()) {
            pressureSensorOp = ControlMote.PressureSensorOperation_e.
                        forNumber(systemEquip.pressureBasedFanControlOn.readDefaultVal().toInt())
        }

        if (systemEquip.co2BasedDamperControlOn.pointExists()) {
            damperCo2Control = ControlMote.DamperCo2Control_e.
                        forNumber(systemEquip.co2BasedDamperControlOn.readDefaultVal().toInt())
            co2Threshold = systemEquip.co2Threshold.readPriorityVal().toInt()
            co2Target = systemEquip.co2Target.readPriorityVal().toInt()
        }
    }

}

fun addSensorBusMappings(builder: Builder) {
    val systemEquip = getAdvancedAhuSystemEquip()
    builder.apply {
        this.addSensorBusMapping(addAddressSensorMapping(
                systemEquip.temperatureSensorBusAdd0.readDefaultVal().toInt(),
                systemEquip.humiditySensorBusAdd0.readDefaultVal().toInt(),
                systemEquip.occupancySensorBusAdd0.readDefaultVal().toInt(),
                systemEquip.co2SensorBusAdd0.readDefaultVal().toInt(),
                systemEquip.sensorBus0PressureAssociation.readDefaultVal().toInt()
        ))

        this.addSensorBusMapping(addAddressSensorMapping(
                systemEquip.temperatureSensorBusAdd1.readDefaultVal().toInt(),
                systemEquip.humiditySensorBusAdd1.readDefaultVal().toInt(),
                systemEquip.occupancySensorBusAdd1.readDefaultVal().toInt(),
                systemEquip.co2SensorBusAdd1.readDefaultVal().toInt(),
                -1 // not exist
        ))

        this.addSensorBusMapping(addAddressSensorMapping(
                systemEquip.temperatureSensorBusAdd2.readDefaultVal().toInt(),
                systemEquip.humiditySensorBusAdd2.readDefaultVal().toInt(),
                systemEquip.occupancySensorBusAdd2.readDefaultVal().toInt(),
                systemEquip.co2SensorBusAdd2.readDefaultVal().toInt(),
                -1 // not exist
        ))

        this.addSensorBusMapping(addAddressSensorMapping(
                systemEquip.temperatureSensorBusAdd3.readDefaultVal().toInt(),
                systemEquip.humiditySensorBusAdd3.readDefaultVal().toInt(),
                systemEquip.occupancySensorBusAdd3.readDefaultVal().toInt(),
                systemEquip.co2SensorBusAdd3.readDefaultVal().toInt(),
                -1 // not exist
        ))
    }
}

fun addAddressSensorMapping(
        tempMapping: Int, humidityMapping: Int, occupancyMapping: Int, co2Mapping: Int, pressureMapping: Int
): ControlMote.CmSensorBusMappings_t {
    return ControlMote.CmSensorBusMappings_t.newBuilder().apply {
        setSensorBusMappingTemp(getTemperatureMapping(tempMapping))
        setSensorBusMappingHumi(getHumidityMapping(humidityMapping))
        setSensorBusMappingOccupancy(getOccupancyMapping(occupancyMapping))
        setSensorBusMappingCo2(getCo2Mapping(co2Mapping))
        if (pressureMapping != -1) { setSensorBusMappingPressure(getPressureMapping(pressureMapping)) }
    }.build()
}

fun addTestSignals(builder: Builder) {
    /*
      [ Y1 Y2 G1 G2 W1 W2 AUX (mappings)]
      CCU needs to send in this format - [ R1 R2 R3 R6 R4 R5 R7 R8 (mappings)]
      Because in hardware it is mapper in this way. please do not change the position
      From profile test signal config is
       0 = R1, 1 = R2, 2 = R3, 3 = R6, 4 = R4, 5 = R5, 6 = R7, 7 = R8 AO1 = 8, AO2 = 9, AO3 = 10, AO4 = 11
       So relays will be loops from 0 to 7 and analogs will be from 8 to 11
  */

    var relayBitmap = 0
    var analogBitmap = 0
    val config = (L.ccu().systemProfile as VavAdvancedAhu)?.testConfigs // TODO
    for (relayPos in 0..7) {
        if (config != null) {
            if (config.get(relayPos)) {
                relayBitmap = relayBitmap or (1 shl MeshUtil.getRelayMapping(relayPos + 1 )) // add 1 to get the correct relay position
            }
        }
    }
    for (analogPos in 8..11) {
        if (config != null) {
            if (config.get(analogPos)) {
                analogBitmap = analogBitmap or (1 shl (analogPos - 8)) // subtract 8 to get the correct bit position
            }
        }
    }

    val configs = ControlMote.CmTestSignals_t.newBuilder()
    configs.apply {
        relayTestSignalsBitmap = relayBitmap
        analogOutputsTestSignalsBitmap = analogBitmap
    }
    builder.setCmTestSignals(configs)
}
private fun getTemperatureMapping(mapping: Int): ControlMote.CmSensorBusMappingsTemp_e {
    return when(mapping) {
        1 -> ControlMote.CmSensorBusMappingsTemp_e.SENSOR_BUS_RETURN_AIR_TEMP
        2 -> ControlMote.CmSensorBusMappingsTemp_e.SENSOR_BUS_MIXED_AIR_TEMP
        3 -> ControlMote.CmSensorBusMappingsTemp_e.SENSOR_BUS_SUPPLY_AIR_TEMP_1
        4 -> ControlMote.CmSensorBusMappingsTemp_e.SENSOR_BUS_SUPPLY_AIR_TEMP_2
        5 -> ControlMote.CmSensorBusMappingsTemp_e.SENSOR_BUS_SUPPLY_AIR_TEMP_3
        else -> ControlMote.CmSensorBusMappingsTemp_e.SENSOR_BUS_TEMP_DISABLED
    }
}

private fun getHumidityMapping(mapping: Int): ControlMote.CmSensorBusMappingsHumi_e {
    return when(mapping) {
        1 -> ControlMote.CmSensorBusMappingsHumi_e.SENSOR_BUS_RETURN_AIR_HUMI
        2 -> ControlMote.CmSensorBusMappingsHumi_e.SENSOR_BUS_MIXED_AIR_HUMI
        3 -> ControlMote.CmSensorBusMappingsHumi_e.SENSOR_BUS_SUPPLY_AIR_HUMI_1
        4 -> ControlMote.CmSensorBusMappingsHumi_e.SENSOR_BUS_SUPPLY_AIR_HUMI_2
        5 -> ControlMote.CmSensorBusMappingsHumi_e.SENSOR_BUS_SUPPLY_AIR_HUMI_3
        else -> ControlMote.CmSensorBusMappingsHumi_e.SENSOR_BUS_HUMI_DISABLED
    }
}

private fun getOccupancyMapping(mapping: Int): ControlMote.CmSensorBusMappingsOccupancy_e {
    return when(mapping) {
        1 -> ControlMote.CmSensorBusMappingsOccupancy_e.SENSOR_BUS_OCCUPANCY_1
        2 -> ControlMote.CmSensorBusMappingsOccupancy_e.SENSOR_BUS_OCCUPANCY_2
        3 -> ControlMote.CmSensorBusMappingsOccupancy_e.SENSOR_BUS_OCCUPANCY_3
        else -> ControlMote.CmSensorBusMappingsOccupancy_e.SENSOR_BUS_OCCUPANCY_DISABLED
    }
}

private fun getCo2Mapping(mapping: Int): ControlMote.CmSensorBusMappingsCO2_e {
    return when(mapping) {
        1 -> ControlMote.CmSensorBusMappingsCO2_e.SENSOR_BUS_RETURN_AIR_CO2
        2 -> ControlMote.CmSensorBusMappingsCO2_e.SENSOR_BUS_MIXED_AIR_CO2
        else -> ControlMote.CmSensorBusMappingsCO2_e.SENSOR_BUS_CO2_DISABLED
    }
}

private fun getPressureMapping(mapping: Int): ControlMote.CmSensorBusMappingsPressure_e {
    return when(mapping) {
        1 -> ControlMote.CmSensorBusMappingsPressure_e.SENSOR_BUS_DUCT_STATIC_PRESSURE_1
        2 -> ControlMote.CmSensorBusMappingsPressure_e.SENSOR_BUS_DUCT_STATIC_PRESSURE_2
        3 -> ControlMote.CmSensorBusMappingsPressure_e.SENSOR_BUS_DUCT_STATIC_PRESSURE_3
        else -> ControlMote.CmSensorBusMappingsPressure_e.SENSOR_BUS_PRESSURE_DISABLED
    }
}

