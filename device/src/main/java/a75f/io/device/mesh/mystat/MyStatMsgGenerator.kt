package a75f.io.device.mesh.mystat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.device.MyStat
import a75f.io.device.MyStat.MyStatControlsMessage_t
import a75f.io.device.mesh.DeviceUtil.mapAnalogOut
import a75f.io.device.mesh.getCoolingDeadBand
import a75f.io.device.mesh.getCoolingUserLimit
import a75f.io.device.mesh.getHeatingDeadBand
import a75f.io.device.mesh.getHeatingUserLimit
import a75f.io.device.mesh.getPin
import a75f.io.device.util.DeviceConfigurationUtil.Companion.getUserConfiguration
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.readValAtLevelByDomain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.equips.mystat.MyStatPipe4Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4RelayMapping
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.getMyStatDomainDeviceByEquipRef
import a75f.io.logic.bo.util.TemperatureMode
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import com.google.protobuf.ByteString

/**
 * Created by Manjunath K on 13-01-2025.
 */

fun getMyStatSeedMessage(zone: String, address: Int, equipRef: String): MyStat.MyStatCcuDatabaseSeedMessage_t {
    val device = getMyStatDevice(address)
    if (device == null) {
        CcuLog.e(L.TAG_CCU_SERIAL, "Device not found for address $address")
        return MyStat.MyStatCcuDatabaseSeedMessage_t.getDefaultInstance()
    }
    val myStatSettingsMessage = getMyStatSettingsMessage(equipRef, zone)
    val myStatControlsMessage = getMyStatControlMessage(address).build()
    val myStatSettingsMessage2 = getMyStatSettings2Message(equipRef)
    val myStatSettingsMessage3 = getMyStatSetting3Message(equipRef)
    CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t" + myStatSettingsMessage.toByteString().toString())
    CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t$myStatControlsMessage")
    CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t$myStatSettingsMessage2")
    CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t$myStatSettingsMessage3")

    return MyStat.MyStatCcuDatabaseSeedMessage_t.newBuilder()
        .setEncryptionKey(ByteString.copyFrom(L.getEncryptionKey()))
        .setSerializedSettingsData(myStatSettingsMessage.toByteString())
        .setSerializedControlsData(myStatControlsMessage.toByteString())
        .setSerializedSettings2Data(myStatSettingsMessage2.toByteString())
        .setSerializedSettings3Data(myStatSettingsMessage3.toByteString())
        .build()
}


fun getMyStatControlMessage(address: Int): MyStatControlsMessage_t.Builder {

    val deviceMap = getMyStatDevice(address)
    if (deviceMap == null) {
        CcuLog.e(L.TAG_CCU_SERIAL, "Device not found for address $address")
        return MyStatControlsMessage_t.newBuilder()
    }
    val controls = MyStatControlsMessage_t.newBuilder()
    val equipRef = deviceMap[Tags.EQUIPREF].toString()
    val myStatEquip = Domain.getDomainEquip(equipRef) as MyStatEquip
    controls.apply {
        setSetTempCooling((myStatEquip.desiredTempCooling.readPriorityVal() * 2).toInt())
        setSetTempHeating((myStatEquip.desiredTempHeating.readPriorityVal() * 2).toInt())
        setFanSpeed(getDeviceFanMode(myStatEquip))
        setConditioningMode(getConditioningMode(myStatEquip))
        setUnoccupiedMode(isInUnOccupiedMode(myStatEquip))
        setOperatingMode(getOperatingMode(myStatEquip))
    }
    fillMyStatControls(controls, equipRef, deviceMap[Tags.ID].toString())
    return controls
}


private fun fillMyStatControls(
    buildr: MyStatControlsMessage_t.Builder,
    equipRef: String,
    deviceRef: String
): MyStatControlsMessage_t.Builder {

    val device = getMyStatDomainDevice(deviceRef, equipRef)
    fun getAnalogOutValue(value: Double): MyStat.MyStatAnalogOutputControl_t {
        return MyStat.MyStatAnalogOutputControl_t.newBuilder().setPercent(value.toInt()).build()
    }

    fun getPortValue(port: PhysicalPoint, isRelay: Boolean, isWritable: Boolean): Double {
        val logicalPointRef = port.readPoint().pointRef
        val hayStack = CCUHsApi.getInstance()
        if (!Globals.getInstance().isTestMode) {  // Skip logical point ref update when test mode is on
            if (logicalPointRef == null) {
                CcuLog.e(L.TAG_CCU_DEVICE, "Logical point ref is missing for ${port.domainName}, hence not updating its priority array or his data.")
            } else {
                // For Relay we have Relay N/O and for analog we have 0-10V (configured)
                if (isRelay.not()) {
                    val logicalValue = hayStack.readPointValue(logicalPointRef).toInt().toShort()
                    val voltage = mapAnalogOut(port.readPoint().type, logicalValue)
                    port.writePointValue(voltage.toDouble())
                } else {
                    // it is relay
                    port.writePointValue(hayStack.readPointValue(logicalPointRef))
                }
            }
        }

        var mappedVal = port.readHisVal()

        if (isWritable) {
            mappedVal = port.readPriorityVal()
            port.writeHisVal(mappedVal)
            if (Globals.getInstance().isTemporaryOverrideMode) {
                return mappedVal
            }
        }
        return mappedVal
    }

    try {
        var relayBitmap = 0
        device.getAllPorts().forEachIndexed { index, port ->
            val (isRelay, isWritable) = device.getTypeAndIsWritable(port)
            val portStatus = getPortValue(port, isRelay, isWritable)
            Log.i("CCU_MST", "fillMyStatControls: portStatus$portStatus  index $index Port: ${port.domainName}, isRelay: $isRelay, isWritable: $isWritable")
            if (isRelay) {
                if (portStatus > 0) {
                    relayBitmap = relayBitmap or (1 shl index)
                }
            } else {
                // it is analog
                when (index) {
                    4 -> buildr.setAnalogOut1(getAnalogOutValue(portStatus)) // universalOut2
                    3 -> buildr.setAnalogOut2(getAnalogOutValue(portStatus)) // universalOut1
                }
            }
        }
        buildr.setRelayBitmap(relayBitmap)
    } catch (e: NullPointerException) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Exception fillMyStatControls: ", e)
    }
    return buildr
}

fun getMyStatSettings2Message(equipRef: String): MyStat.MyStatSettingsMessage2_t {
    val settings2 = MyStat.MyStatSettingsMessage2_t.newBuilder()
    val myStatEquip = Domain.getDomainEquip(equipRef) as MyStatEquip
    val myStatDevice = getMyStatDomainDeviceByEquipRef(equipRef)
    settings2.apply {
        when (myStatEquip) {
            is MyStatCpuEquip -> settings2.profile = MyStat.MyStatProfiles_t.MYSTAT_PROFILE_CONVENTIONAL_PACKAGE_UNIT
            is MyStatHpuEquip -> settings2.profile = MyStat.MyStatProfiles_t.MYSTAT_PROFILE_HEAT_PUMP_UNIT
            is MyStatPipe2Equip -> settings2.profile = MyStat.MyStatProfiles_t.MYSTAT_PROFILE_2_PIPE_FANCOIL_UNIT
            is MyStatPipe4Equip -> settings2.profile = MyStat.MyStatProfiles_t.MYSTAT_PROFILE_4_PIPE_FANCOIL_UNIT
        }

        enableForceOccupied = myStatEquip.autoForceOccupied.readDefaultVal() == 1.0
        enableAutoAway = myStatEquip.autoAway.readDefaultVal() == 1.0
        zoneCO2DamperOpeningRate = myStatEquip.co2DamperOpeningRate.readDefaultVal().toInt()
        proportionalConstant = (TunerUtil.getProportionalGain(equipRef) * 100).toInt()
        integralConstant = (TunerUtil.getIntegralGain(equipRef) * 100).toInt()
        proportionalTemperatureRange = (TunerUtil.getProportionalSpread(equipRef) * 10).toInt()
        integrationTime = TunerUtil.getIntegralTimeout(equipRef).toInt()
        zoneCO2Target = myStatEquip.co2Target.readDefaultVal().toInt()
        zoneCO2Threshold = myStatEquip.co2Threshold.readDefaultVal().toInt()
        getRelayConfigs(myStatEquip,this)
        setMyStatAnalogOutConfig(getAnalogOutConfigs(myStatEquip, myStatEquip.universalOut1Enable, myStatEquip.universalOut1Association))

        if (myStatEquip is MyStatPipe2Equip) {
            mystatUniversalInConfig = 1 // always map to supply water temp
        } else if (myStatEquip.universalIn1Enable.readDefaultVal() == 1.0) {
            val logicalSensor = Domain.hayStack.readHDictById(myStatDevice.universal1In.readPoint().pointRef)
            val logicalDomainName = logicalSensor?.getStr("domainName")
            if (logicalDomainName != null) {
                mystatUniversalInConfig = when (logicalDomainName) {
                    DomainName.dischargeAirTemperature -> MyStatConfiguration.UniversalMapping.UIN_TH_AIR_TEMP.ordinal
                    DomainName.genericAlarmNO -> MyStatConfiguration.UniversalMapping.UIN_TH_GENERIC_ALARM_NO.ordinal
                    DomainName.genericAlarmNC -> MyStatConfiguration.UniversalMapping.UIN_TH_GENERIC_ALARM_NC.ordinal
                    DomainName.keyCardSensor -> MyStatConfiguration.UniversalMapping.UIN_AN_KEYCARD.ordinal
                    DomainName.doorWindowSensorNCTitle24 -> MyStatConfiguration.UniversalMapping.UIN_TH_DOOR_WINDOW_NC_TITLE24.ordinal
                    DomainName.doorWindowSensor -> MyStatConfiguration.UniversalMapping.UIN_AN_DOOR_WINDOW.ordinal
                    DomainName.fanRunSensorNO -> MyStatConfiguration.UniversalMapping.UIN_TH_FAN_RUN_SENSOR_NO.ordinal
                    DomainName.fanRunSensorNC -> MyStatConfiguration.UniversalMapping.UIN_TH_FAN_RUN_SENSOR_NC.ordinal
                    DomainName.doorWindowSensorNOTitle24 -> MyStatConfiguration.UniversalMapping.UIN_TH_DOOR_WINDOW_NO_TITLE24.ordinal
                    DomainName.doorWindowSensorNO -> MyStatConfiguration.UniversalMapping.UIN_TH_DOOR_WINDOW_SENSOR_NC.ordinal
                    DomainName.doorWindowSensorNC -> MyStatConfiguration.UniversalMapping.UIN_TH_DOOR_WINDOW_SENSOR_NO.ordinal
                    DomainName.keyCardSensorNO -> MyStatConfiguration.UniversalMapping.UIN_TH_KEY_CARD_SENSOR_NO.ordinal
                    DomainName.keyCardSensorNC -> MyStatConfiguration.UniversalMapping.UIN_TH_KEY_CARD_SENSOR_NC.ordinal
                    DomainName.chilledWaterLeavingTempSensor -> MyStatConfiguration.UniversalMapping.UIN_TH_CHILLED_WATER_SUPPLY_TEMP.ordinal
                    DomainName.hotWaterLeavingTempSensor -> MyStatConfiguration.UniversalMapping.UIN_TH_HOT_WATER_SUPPLY_TEMP.ordinal
                    DomainName.currentTx10 -> MyStatConfiguration.UniversalMapping.UIN_AI_CURRENT_TX_10.ordinal
                    DomainName.currentTx20 -> MyStatConfiguration.UniversalMapping.UIN_AI_CURRENT_TX_20.ordinal
                    DomainName.currentTx50 -> MyStatConfiguration.UniversalMapping.UIN_AI_CURRENT_TX_50.ordinal
                    DomainName.voltageInput -> MyStatConfiguration.UniversalMapping.UIN_AI_GENERIC_VOLTAGE_INPUT.ordinal
                    DomainName.thermistorInput -> MyStatConfiguration.UniversalMapping.UIN_TH_GENERIC_THERMISTOR_INPUT.ordinal
                    else -> 0
                }
            }
        } else {
            mystatUniversalInConfig = 0 // Disabled
        }
        genericTuners = getCommonTuners(equipRef)
        when (myStatEquip) {
            is MyStatCpuEquip -> myStatConfigsCpu = getStagedFanDetails(myStatEquip)
            is MyStatHpuEquip -> fcuTuners = getHpuTunerDetails(equipRef)
            is MyStatPipe2Equip, is MyStatPipe4Equip -> fcuTuners = getFcuTunerDetails(equipRef)

        }
    }
    return settings2.build()
}

fun getMyStatSetting3Message(equipRef: String): MyStat.MyStatSettingsMessage3_t {
    val equip = Domain.getDomainEquip(equipRef) as MyStatEquip
    val settings3 = MyStat.MyStatSettingsMessage3_t.newBuilder()
    settings3.apply {
        mystatRelay5Config = getRelayConfig(equip.universalOut1Enable, equip.universalOut1Association, equip)
        myStatAnalogOut2Config = getAnalogOutConfigs(equip, equip.universalOut2Enable, equip.universalOut2Association)
        stageUpTimer = equip.mystatStageUpTimerCounter.readPriorityVal().toInt()
        stageDownTimer = equip.mystatStageDownTimerCounter.readPriorityVal().toInt()
    }
    return settings3.build()
}

private fun getStagedFanDetails(equip: MyStatCpuEquip): MyStat.MyStatConfigsCpu_t {
    return MyStat.MyStatConfigsCpu_t.newBuilder().apply {
        coolingStage1FanAnalogVoltage = (equip.fanOutCoolingStage1.readPriorityVal() * 10).toInt()
        coolingStage2FanAnalogVoltage = (equip.fanOutCoolingStage2.readPriorityVal() * 10).toInt()
        heatingStage1FanAnalogVoltage = (equip.fanOutHeatingStage1.readPriorityVal() * 10).toInt()
        heatingStage2FanAnalogVoltage = (equip.fanOutHeatingStage2.readPriorityVal() * 10).toInt()
        analogoutAtRecFanAnalogVoltage = ((CCUHsApi.getInstance()
            .readPointPriorityValByQuery("config and recirculate and equipRef == \"${equip.equipRef}\"") * 10)
            .toInt()) // this needs to be checked again.
    }.build()
}


private fun getHpuTunerDetails(equipRef: String): MyStat.MyStatTunersFcu_t {
    return MyStat.MyStatTunersFcu_t.newBuilder().apply {
        auxHeatingActivate = getTunerByDomain(DomainName.mystatAuxHeating1Activate, equipRef).toInt()
    }.build()
}

private fun getFcuTunerDetails(equipRef: String): MyStat.MyStatTunersFcu_t {

    return MyStat.MyStatTunersFcu_t.newBuilder().apply {
        auxHeatingActivate = getTunerByDomain(DomainName.mystatAuxHeating1Activate, equipRef).toInt()
        twoPipeHeatingThreshold = getTunerByDomain(DomainName.mystatPipe2FancoilHeatingThreshold, equipRef).toInt()
        twoPipeCoolingThreshold = getTunerByDomain(DomainName.mystatPipe2FancoilCoolingThreshold, equipRef).toInt()
        waterValveSamplingOnTime = getTunerByDomain(DomainName.mystatWaterValveSamplingOnTime, equipRef).toInt()
        waterValveSamplingWaitTime = getTunerByDomain(DomainName.mystatWaterValveSamplingWaitTime, equipRef).toInt()
        waterValveSamplingDuringLoopDeadbandOnTime = getTunerByDomain(DomainName.mystatWaterValveSamplingLoopDeadbandOnTime, equipRef).toInt()
        waterValveSamplingDuringLoopDeadbandWaitTime = getTunerByDomain(DomainName.mystatWaterValveSamplingLoopDeadbandWaitTime, equipRef).toInt()
    }.build()
}

private fun getCommonTuners(equipRef: String): MyStat.MyStatTunersGeneric_t {
    val equip = CCUHsApi.getInstance().readHDictById(equipRef)
    return MyStat.MyStatTunersGeneric_t.newBuilder().apply {
        unoccupiedSetback = (CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and zone and unoccupied and setback and roomRef == \"" + equip.get("roomRef").toString() + "\"") * 10).toInt()
        minFanRuntimePostconditioning = getTunerByDomain(DomainName.minFanRuntimePostConditioning, equipRef).toInt()
        relayActivationHysteresis = TunerUtil.getHysteresisPoint("hysteresis and activation", equipRef).toInt()
        analogFanSpeedMultiplier = (getTunerByDomain(DomainName.standaloneAnalogFanSpeedMultiplier, equipRef) * 10).toInt()
        humidityHysteresis = getTunerByDomain(DomainName.standaloneHumidityHysteresis, equipRef).toInt()
        autoAwayZoneSetbackTemp = (getTunerByDomain(DomainName.autoAwaySetback, equipRef) * 10).toInt()
        autoAwayTime = getTunerByDomain(DomainName.autoAwayTime, equipRef).toInt()
        forcedOccupiedTime = getTunerByDomain(DomainName.forcedOccupiedTime, equipRef).toInt()
    }.build()
}

fun getMyStatSettingsMessage(equipRef: String, zone: String): MyStat.MyStatSettingsMessage_t {
    val myStatEquip = Domain.getDomainEquip(equipRef) as MyStatEquip
    val zoneId = HSUtil.getZoneIdFromEquipId(equipRef)
    val modeType: Int = CCUHsApi.getInstance().readHisValByQuery(
        "zone and hvacMode and roomRef" +
                " == \"" + zoneId + "\""
    ).toInt()
    val hvacMode = TemperatureMode.values()[modeType]
    val msg =  MyStat.MyStatSettingsMessage_t.newBuilder()
        .setRoomName(zone)
        .setHeatingDeadBand((getHeatingDeadBand(zoneId) * 10).toInt())
        .setCoolingDeadBand((getCoolingDeadBand(zoneId) * 10).toInt())
        .setTemperatureOffset((myStatEquip.temperatureOffset.readPriorityVal() * 10).toInt())
        .setHumidityMinSetpoint(myStatEquip.targetHumidifier.readPriorityVal().toInt())
        .setHumidityMaxSetpoint(myStatEquip.targetDehumidifier.readPriorityVal().toInt())
        .setShowCentigrade(getUserConfiguration() == 1.0)
        .setMystatLinearFanSpeeds(setLinearFanSpeedDetails(myStatEquip))
        .setMystatStagedFanSpeeds(setStagedFanSpeedDetails(myStatEquip))
        .setTemperatureMode(getTempMode())
        .setMiscSettings1(if (myStatEquip.enableDesiredTempDisplay.isEnabled()) (1 shl 3) else 0)
            //sending MiscSettings1 always 001
            // bit 0: enableExternal10kTemperatureSensor
            // bit 1: disableTouch
            // bit 2: brightnessVariationEnable
        .setInstallerLockPin(getPin(myStatEquip.pinLockInstallerAccess))
        .setUserLockPin(getPin(myStatEquip.pinLockConditioningModeFanAccess))
    // based on hvacMode sending the user limits
    when (hvacMode) {
        TemperatureMode.DUAL -> {
            msg.setMinCoolingUserTemp(getCoolingUserLimit("min", zoneId))
                .setMaxCoolingUserTemp(getCoolingUserLimit("max", zoneId))
                .setMinHeatingUserTemp(getHeatingUserLimit("min", zoneId))
                .setMaxHeatingUserTemp(getHeatingUserLimit("max", zoneId))
        }

        TemperatureMode.COOLING -> {
            msg.setMinCoolingUserTemp(getCoolingUserLimit("min", zoneId))
                .setMaxCoolingUserTemp(getCoolingUserLimit("max", zoneId))
        }

        TemperatureMode.HEATING -> {
            msg.setMinHeatingUserTemp(getHeatingUserLimit("min", zoneId))
                .setMaxHeatingUserTemp(getHeatingUserLimit("max", zoneId))
        }

        else -> {}
    }
        return  msg.build()
}


private fun setLinearFanSpeedDetails(equip: MyStatEquip): MyStat.MyStatLinearFanSpeeds_t {
    val linearFanSpeedBuilder = MyStat.MyStatLinearFanSpeeds_t.newBuilder()
    equip.apply {
        if (universalOut1Enable.readDefaultVal() == 1.0) {
            val association = universalOut1Association.readDefaultVal().toInt()
            if (this is MyStatCpuEquip && association == MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal ||
                this is MyStatHpuEquip && association == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal ||
                this is MyStatPipe2Equip && association == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal ||
                this is MyStatPipe4Equip && association == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal) {
                linearFanSpeedBuilder.linearFanLowSpeedLevel = analog1FanLow.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanHighSpeedLevel = analog1FanHigh.readPriorityVal().toInt()

            }
        }
        if (universalOut2Enable.readDefaultVal() == 1.0) {
            val association = universalOut2Association.readDefaultVal().toInt()
            if (this is MyStatCpuEquip && association == MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal ||
                this is MyStatHpuEquip && association == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal ||
                this is MyStatPipe2Equip && association == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal ||
                this is MyStatPipe4Equip && association == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal) {
                linearFanSpeedBuilder.linearFanLowSpeedLevel = analog2FanLow.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanHighSpeedLevel = analog2FanHigh.readPriorityVal().toInt()

            }
        }
    }
    return linearFanSpeedBuilder.build()
}

private fun setStagedFanSpeedDetails(equip: MyStatEquip): MyStat.MyStatStagedFanSpeeds_t {
    val stagedFanSpeedBuilder = MyStat.MyStatStagedFanSpeeds_t.newBuilder()
    equip.apply {
        if (this is MyStatCpuEquip) {
            if (universalOut1Enable.readDefaultVal() == 1.0 && universalOut1Association.readDefaultVal().toInt() == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog1FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog1FanHigh.readPriorityVal().toInt()
            }
            if (universalOut2Enable.readDefaultVal() == 1.0 && universalOut2Association.readDefaultVal().toInt() == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog2FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog2FanHigh.readPriorityVal().toInt()
            }
        }
    }
    return stagedFanSpeedBuilder.build()
}

fun getRelayConfig(enable: Point, association: Point, equip: MyStatEquip): MyStat.MyStatRelay_t {
    val relayConfig = MyStat.MyStatRelay_t.newBuilder()

    relayConfig.relayEnable = enable.readDefaultVal() == 1.0
    if (relayConfig.relayEnable) {
        val associationVal = association.readDefaultVal().toInt()
        when (equip) {
            is MyStatCpuEquip -> {
                if (associationVal < MyStatCpuRelayMapping.values().size) {
                    relayConfig.cpuRelay = when (associationVal) {
                        MyStatCpuRelayMapping.EXTERNALLY_MAPPED.ordinal -> MyStat.CpuRelayMappings_e.CPU_NONE
                        MyStatCpuRelayMapping.DCV_DAMPER.ordinal -> MyStat.CpuRelayMappings_e.CPU_DCV_DAMPER
                        else -> MyStat.CpuRelayMappings_e.values()[associationVal + 1]
                    }
                }
            }

            is MyStatHpuEquip -> {
                if (associationVal < MyStatHpuRelayMapping.values().size) {
                    relayConfig.hpuRelay = when (associationVal) {
                        MyStatHpuRelayMapping.EXTERNALLY_MAPPED.ordinal -> MyStat.HpuRelayMappings_e.HPU_NONE
                        MyStatHpuRelayMapping.DCV_DAMPER.ordinal -> MyStat.HpuRelayMappings_e.HPU_DCV_DAMPER
                        else -> MyStat.HpuRelayMappings_e.values()[associationVal + 1]
                    }
                }
            }

            is MyStatPipe2Equip -> {
                if (associationVal < MyStatPipe2RelayMapping.values().size) {
                    relayConfig.twoPipeRelay = when (associationVal) {
                        MyStatPipe2RelayMapping.EXTERNALLY_MAPPED.ordinal -> MyStat.TwoPipeRelayMappings_e.P2_NONE
                        MyStatPipe2RelayMapping.DCV_DAMPER.ordinal -> MyStat.TwoPipeRelayMappings_e.P2_DCV_DAMPER
                        MyStatPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal -> MyStat.TwoPipeRelayMappings_e.P2_FAN_LOW_VENTILATION
                        else -> MyStat.TwoPipeRelayMappings_e.values()[associationVal + 1]
                    }
                }
            }

            is MyStatPipe4Equip -> {
                if (associationVal < MyStatPipe4RelayMapping.values().size) {
                    relayConfig.fourPipeRelay = when (associationVal) {
                        MyStatPipe4RelayMapping.HOT_WATER_VALVE.ordinal -> MyStat.FourPipeRelayMappings_e.P4_WATER_VALVE_HEAT
                        MyStatPipe4RelayMapping.CHILLED_WATER_VALVE.ordinal -> MyStat.FourPipeRelayMappings_e.P4_WATER_VALVE_COOL
                        MyStatPipe4RelayMapping.AUX_HEATING_STAGE1.ordinal -> MyStat.FourPipeRelayMappings_e.P4_AUX_HEAT_1
                        MyStatPipe4RelayMapping.EXTERNALLY_MAPPED.ordinal -> MyStat.FourPipeRelayMappings_e.P4_NONE
                        MyStatPipe4RelayMapping.DCV_DAMPER.ordinal -> MyStat.FourPipeRelayMappings_e.P4_DCV_DAMPER
                        MyStatPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal -> MyStat.FourPipeRelayMappings_e.P4_FAN_LOW_VENTILATION
                        else -> MyStat.FourPipeRelayMappings_e.values()[associationVal + 1]
                    }
                }
            }
        }
    }
    return relayConfig.build()
}

private fun getRelayConfigs(
    equip: MyStatEquip,
    settings2: MyStat.MyStatSettingsMessage2_t.Builder
) {
    settings2.addMystatRelayConfig(getRelayConfig(equip.relay1OutputEnable, equip.relay1OutputAssociation, equip))
    settings2.addMystatRelayConfig(getRelayConfig(equip.relay2OutputEnable, equip.relay2OutputAssociation, equip))
    settings2.addMystatRelayConfig(getRelayConfig(equip.relay3OutputEnable, equip.relay3OutputAssociation, equip))
    settings2.addMystatRelayConfig(getRelayConfig(equip.universalOut2Enable, equip.universalOut2Association, equip))

}
private fun getAnalogOutConfigs(equip: MyStatEquip, enable: Point, association: Point): MyStat.MyStatAnalogOut_t  {
    val analogOutConfigs = MyStat.MyStatAnalogOut_t.newBuilder()
    val hsApi = CCUHsApi.getInstance()
    analogOutConfigs.apply {

        val analogOutMappingValue = association.readDefaultVal().toInt()
        when(equip) {
            is MyStatCpuEquip -> {
                if (analogOutMappingValue >= MyStatCpuRelayMapping.values().size) {
                    cpuAoutMapping = when (analogOutMappingValue) {
                        MyStatCpuAnalogOutMapping.COOLING.ordinal -> MyStat.CpuAoutMappings_e.CPU_AOUT_COOLING
                        MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal -> MyStat.CpuAoutMappings_e.CPU_AOUT_FANSPEED
                        MyStatCpuAnalogOutMapping.HEATING.ordinal -> MyStat.CpuAoutMappings_e.CPU_AOUT_HEATING
                        MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal -> MyStat.CpuAoutMappings_e.CPU_AOUT_FANSPEEDSTAGED
                        MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal -> MyStat.CpuAoutMappings_e.CPU_AOUT_DCVDAMPER
                        else -> MyStat.CpuAoutMappings_e.CPU_AOUT_NONE
                    }
                    setAnalogOutEnable(enable.readDefaultVal() == 1.0)
                }
            }
            is MyStatHpuEquip -> {
                if (analogOutMappingValue >= MyStatHpuRelayMapping.values().size) {
                    hpuAoutMapping = when (analogOutMappingValue) {
                        MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal -> MyStat.HpuAoutMappings_e.HPU_AOUT_COMPSPEED
                        MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal -> MyStat.HpuAoutMappings_e.HPU_AOUT_FANSPEED
                        MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal -> MyStat.HpuAoutMappings_e.HPU_AOUT_DCVDAMPER
                        else -> MyStat.HpuAoutMappings_e.HPU_AOUT_NONE
                    }
                    setAnalogOutEnable(enable.readDefaultVal() == 1.0)
                }
            }
            is MyStatPipe2Equip -> {
                if (analogOutMappingValue >= MyStatPipe2RelayMapping.values().size) {
                    twoPipeAoutMapping = when (analogOutMappingValue) {
                        MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal -> MyStat.TwoPipeAoutMappings_e.P2_AOUT_WATER_VALVE
                        MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal -> MyStat.TwoPipeAoutMappings_e.P2_FAN_SPEED
                        MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal -> MyStat.TwoPipeAoutMappings_e.P2_AOUT_DCV_DAMPER
                        else -> MyStat.TwoPipeAoutMappings_e.P2_AOUT_NONE
                    }
                    setAnalogOutEnable(enable.readDefaultVal() == 1.0)
                }
            }

            is MyStatPipe4Equip -> {
                if (analogOutMappingValue >= MyStatPipe4RelayMapping.values().size) {
                    fourPipeAoutMapping = when (analogOutMappingValue) {
                        MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.ordinal -> MyStat.FourPipeAoutMappings_e.P4_AOUT_WATER_VALVE_COOL
                        MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE.ordinal -> MyStat.FourPipeAoutMappings_e.P4_AOUT_WATER_VALVE_HEAT
                        MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal -> MyStat.FourPipeAoutMappings_e.P4_FAN_SPEED
                        MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal -> MyStat.FourPipeAoutMappings_e.P4_AOUT_DCV_DAMPER
                        else -> MyStat.FourPipeAoutMappings_e.P4_AOUT_NONE
                    }
                    setAnalogOutEnable(enable.readDefaultVal() == 1.0)
                }
            }
        }
        val index = if (enable.domainName == DomainName.universal1OutputEnable) 1 else 2
        setAnalogOutAtMinSetting(hsApi.readDefaultVal("min and analog == $index and equipRef == \"${equip.equipRef}\"").toInt() * 10)
        setAnalogOutAtMaxSetting(hsApi.readDefaultVal("max and analog == $index and equipRef == \"${equip.equipRef}\"").toInt() * 10)
    }
    return analogOutConfigs.build()
}


private fun getDeviceFanMode(equip: MyStatEquip): MyStat.MyStatFanSpeed_e {
    try {
        val fanMode = MyStatFanStages.values()[equip.fanOpMode.readPriorityVal().toInt()]
        return when (fanMode) {
            MyStatFanStages.AUTO -> MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_AUTO
            MyStatFanStages.LOW_ALL_TIME, MyStatFanStages.LOW_CUR_OCC, MyStatFanStages.LOW_OCC -> MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_LOW
            MyStatFanStages.HIGH_ALL_TIME, MyStatFanStages.HIGH_CUR_OCC, MyStatFanStages.HIGH_OCC -> MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_HIGH
            else -> MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_OFF
        }
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Exception getDeviceFanMode: ", e)
    }
    return MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_OFF
}


private fun getConditioningMode(equip: MyStatEquip): MyStat.MyStatConditioningMode_e {
    try {
        val conditioningMode = StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal().toInt()]
        return when (conditioningMode) {
            StandaloneConditioningMode.AUTO -> MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_AUTO
            StandaloneConditioningMode.COOL_ONLY -> MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_COOLING
            StandaloneConditioningMode.HEAT_ONLY -> MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_HEATING
            else -> MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_OFF
        }
    } catch (e: java.lang.Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Exception getConditioningMode: ", e)
    }
    return MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_OFF
}

private fun isInUnOccupiedMode(equip: MyStatEquip): Boolean {
    return equip.occupancyMode.readHisVal().toInt().let {
        (it == Occupancy.UNOCCUPIED.ordinal || it == Occupancy.AUTOAWAY.ordinal)
    }
}

private fun getTempMode(): MyStat.MyStatTemperatureMode_e {
    val temperatureMode = readValAtLevelByDomain(DomainName.temperatureMode, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL).toInt()
    return if (temperatureMode == 0)
        MyStat.MyStatTemperatureMode_e.MYSTAT_TEMP_MODE_DUAL_FIXED_DB
    else
        MyStat.MyStatTemperatureMode_e.MYSTAT_TEMP_MODE_DUAL_VARIABLE_DB
}

private fun getOperatingMode(equip: MyStatEquip): MyStat.MyStatOperatingMode_e {
    val operatingMode = equip.operatingMode.readHisVal().toInt()
    return when (operatingMode) {
        1 -> MyStat.MyStatOperatingMode_e.MYSTAT_OPERATING_MODE_COOLING
        2 -> MyStat.MyStatOperatingMode_e.MYSTAT_OPERATING_MODE_HEATING
        else -> MyStat.MyStatOperatingMode_e.MYSTAT_OPERATING_MODE_OFF
    }
}

fun getMyStatRebootControl(address: Int): MyStatControlsMessage_t {
    CcuLog.d(L.TAG_CCU_SERIAL, "Reset set to true")
    return getMyStatControlMessage(address).setReset(true).build()
}

private fun getTunerByDomain(domainName: String, equipRef: String) = TunerUtil.readTunerValByQuery("domainName ==\"$domainName\"", equipRef)
