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
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu

//Decimal values are multiplied by 10 to keep precision since all the values are send as integers.z
const val SERIAL_COMM_SCALE = 10
fun getCMControlsMessage(): ControlMote.CcuToCmOverUsbCmControlMessage_t {

    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

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
        emergencyShutOff = (L.ccu().systemProfile as VavAdvancedAhu)?.isEmergencyShutoffActive()?.toInt() ?: 0
    }
    return msgBuilder.build()
}

fun getCMSettingsMessage() : ControlMote.CcuToCmSettingsMessage_t {

    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

    val msgBuilder = ControlMote.CcuToCmSettingsMessage_t.newBuilder()
    msgBuilder.apply {
        //setTemperatureOffset(systemEquip.temperatureOffset.readHisVal().toInt())
        cmProfile = ControlMote.CMProfiles_e.CM_PROFILE_ADV_AHU
        relayActivationHysteresis = systemEquip.vavRelayDeactivationHysteresis.readHisVal().toInt()
        analogFanSpeedMultiplier = systemEquip.vavAnalogFanSpeedMultiplier.readHisVal().toInt() * 10
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
    }
    return msgBuilder.build()
}

fun addRelayConfigsToSettingsMessage(builder: Builder) {
    /*
        [ Y1 Y2 G1 G2 W1 W2 AUX (mappings)]
        CCU needs to send in this format - [ R1 R2 R3 R6 R4 R5 R7 R8 (mappings)]
        Because in hardware it is mapper in this way. please do not change the position
    */
    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

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

    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

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

    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

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

    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

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
    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

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

    val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
        Domain.systemEquip as VavAdvancedHybridSystemEquip
    } else {
        Domain.systemEquip as DabAdvancedHybridSystemEquip
    }

    builder.apply {
        if (systemEquip.temperatureSensorBusAdd0.pointExists()) {
            addSensorBusMapping(ControlMote.CmSensorBusMappings_e.SENSOR_BUS_SAT1)
        }
        if (systemEquip.temperatureSensorBusAdd1.pointExists()) {
            addSensorBusMapping(ControlMote.CmSensorBusMappings_e.SENSOR_BUS_SAT2)
        }
        if (systemEquip.temperatureSensorBusAdd2.pointExists()) {
            addSensorBusMapping(ControlMote.CmSensorBusMappings_e.SENSOR_BUS_SAT3)
        }
        if (systemEquip.pressureSensorBusAdd0.pointExists()) {
            addSensorBusMapping(ControlMote.CmSensorBusMappings_e.SENSOR_BUS_DSP1)
        }
        if (systemEquip.pressureSensorBusAdd1.pointExists()) {
            addSensorBusMapping(ControlMote.CmSensorBusMappings_e.SENSOR_BUS_DSP2)
        }
        if (systemEquip.pressureSensorBusAdd2.pointExists()) {
            addSensorBusMapping(ControlMote.CmSensorBusMappings_e.SENSOR_BUS_DSP3)
        }
    }
}