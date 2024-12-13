package a75f.io.device.mesh.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.constants.WhoFiledConstants
import a75f.io.device.HyperStat
import a75f.io.device.HyperStat.HyperStatLocalControlsOverrideMessage_t
import a75f.io.device.HyperStat.HyperStatRegularUpdateMessage_t
import a75f.io.device.HyperStat.SensorReadingPb_t
import a75f.io.device.mesh.DeviceUtil
import a75f.io.device.mesh.LSerial
import a75f.io.device.mesh.MeshUtil
import a75f.io.device.mesh.Pulse
import a75f.io.device.mesh.ThermistorUtil
import a75f.io.device.serial.CcuToCmOverUsbDeviceTempAckMessage_t
import a75f.io.device.serial.MessageType
import a75f.io.domain.api.Domain.getDomainEquip
import a75f.io.domain.api.Domain.hayStack
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.readPhysicalPoint
import a75f.io.domain.devices.HyperStatDevice
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.util.PointsUtil
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.PossibleConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.PossibleFanMode
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuProfile
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuEquipToBeDeleted
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuProfile
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2EquipToBeDeleted
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getCpuFanLevel
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPossibleConditionMode
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPossibleFanModeSettings
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.sensors.SensorType
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.interfaces.ZoneDataInterface
import a75f.io.logic.jobs.HyperStatUserIntentHandler.Companion.updateHyperStatUIPoints

/**
 * Created by Manjunath K on 22-10-2024.
 */

fun handleRegularUpdate(regularUpdateMessage: HyperStatRegularUpdateMessage_t, device: HashMap<Any, Any>, nodeAddress: Int, refresh: ZoneDataInterface?) {
    val equipRef = device[Tags.EQUIPREF].toString()
    val hsDevice = getHyperStatDomainDevice(device[Tags.ID].toString(), equipRef)

    CcuLog.d(L.TAG_CCU_DEVICE, "HyperStat RegularUpdate: nodeAddress $nodeAddress :  $regularUpdateMessage")
    if (Globals.getInstance().isTemporaryOverrideMode) {
        updateHsRssi(hsDevice, regularUpdateMessage.rssi)
        refresh?.refreshHeartBeatStatus(nodeAddress.toString())
        return
    }
    updateHsRssi(hsDevice, regularUpdateMessage.rssi)
    updateTemp(hsDevice, regularUpdateMessage.roomTemperature.toDouble())

    updatePhysicalInputs(hsDevice.analog1In, regularUpdateMessage.externalAnalogVoltageInput1.toDouble(), equipRef)
    updatePhysicalInputs(hsDevice.analog2In, regularUpdateMessage.externalAnalogVoltageInput2.toDouble(), equipRef)
    updatePhysicalInputs(hsDevice.th1In, regularUpdateMessage.externalThermistorInput1.toDouble(), equipRef)
    updatePhysicalInputs(hsDevice.th2In, regularUpdateMessage.externalThermistorInput2.toDouble(), equipRef)

    updateOccupancy(hsDevice, regularUpdateMessage.occupantDetected)
    updateIlluminance(hsDevice, regularUpdateMessage.illuminance.toDouble())
    updateHumidity(hsDevice, regularUpdateMessage.humidity.toDouble())
    updateSound(hsDevice, regularUpdateMessage.illuminance.toDouble())
    updateDynamicSensors(hsDevice, regularUpdateMessage.sensorReadingsList, equipRef, nodeAddress)

    CcuLog.d(L.TAG_CCU_DEVICE, "HyperStat Regular Update completed")

    refresh?.refreshHeartBeatStatus(nodeAddress.toString())
}

fun handleOverrideMsg(message: HyperStatLocalControlsOverrideMessage_t, nodeAddress: Int, equipRef: String) {

    CcuLog.d(L.TAG_CCU_DEVICE, "HyperStat Override: nodeAddress $nodeAddress :  $message")
    val equip = getDomainEquip(equipRef) as HyperStatEquip
    updateDesiredTemp(equip, message)
    updateModes(equip, message, nodeAddress)
    runProfileAlgo(nodeAddress.toShort())
    HyperStatMessageSender.sendControlMessage(nodeAddress, equipRef)
    sendAcknowledge(nodeAddress)
}

private fun updateDesiredTemp(equip: HyperStatEquip, message: HyperStatLocalControlsOverrideMessage_t) {

    val coolingDesiredTemp = message.setTempCooling.toDouble() / 2
    val heatingDesiredTemp = message.setTempHeating.toDouble() / 2
    val avgDesiredTemp = (coolingDesiredTemp + heatingDesiredTemp) / 2
    equip.desiredTempCooling.writePointValue(coolingDesiredTemp)
    equip.desiredTempHeating.writePointValue(heatingDesiredTemp)
    equip.desiredTemp.writePointValue(avgDesiredTemp)

    val coolingPoint = Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(equip.desiredTempCooling.id)).build()
    val heatingPoint = Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(equip.desiredTempHeating.id)).build()
    val desiredPoint = Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(equip.desiredTemp.id)).build()
    CcuLog.d(L.TAG_CCU_DEVICE, "device overriding CoolingDesiredTemp: $coolingDesiredTemp HeatingDesiredTemp: $heatingDesiredTemp AvgDesiredTemp: $avgDesiredTemp")
    DeviceUtil.updateDesiredTempFromDevice(coolingPoint, heatingPoint, desiredPoint, coolingDesiredTemp, heatingDesiredTemp, avgDesiredTemp, hayStack, WhoFiledConstants.HYPERSTAT_WHO)
}

private fun updateModes(equip: HyperStatEquip, message: HyperStatLocalControlsOverrideMessage_t, nodeAddress: Int) {
    var possibleMode = PossibleConditioningMode.OFF
    var possibleFanMode = PossibleFanMode.OFF

    when (L.getProfile(nodeAddress.toShort())) {
        is HyperStatCpuProfile -> {
            val configs = getConfiguration(equip.equipRef) as CpuConfiguration
            possibleFanMode = getPossibleFanModeSettings(getCpuFanLevel(configs))
            possibleMode = getPossibleConditionMode(configs)
        }

        is HyperStatPipe2Profile, is HyperStatHpuProfile -> {}
    }
    updateFanMode(possibleFanMode, message.fanSpeed, equip.equipRef)
    updateConditioningMode(possibleMode, message.conditioningMode, equip)
}

private fun updateConditioningMode(
        possibleMode: PossibleConditioningMode,
        mode: HyperStat.HyperStatConditioningMode_e,
        equip: HyperStatEquip
) {

    val isValidMode = when (possibleMode) {
        PossibleConditioningMode.OFF -> (mode == HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_OFF)
        PossibleConditioningMode.COOLONLY -> (mode != HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_HEATING)
        PossibleConditioningMode.HEATONLY -> (mode != HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_COOLING)
        PossibleConditioningMode.BOTH -> true // anything is fine
    }

    if (!isValidMode) {
        CcuLog.i(L.TAG_CCU_DEVICE, "Invalid selected $mode possibleMode $possibleMode Invalid conditioning mode ")
        return
    }
    updateHyperStatUIPoints(equip.equipRef, "domainName == \"${DomainName.conditioningMode}\"", mode.ordinal.toDouble(), WhoFiledConstants.HYPERSTAT_WHO)
    CcuLog.d(L.TAG_CCU_DEVICE, "${equip.nodeAddress} conditioning mode updated to $mode")
}

private fun updateFanMode(
        possibleMode: PossibleFanMode, selectedMode: HyperStat.HyperStatFanSpeed_e, equipRef: String) {

    fun getFanLevel(lowActive: Boolean = false, mediumActive: Boolean = false, highActive: Boolean = false): Int {
        return when (selectedMode) {
            HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_OFF -> StandaloneFanStage.OFF.ordinal
            HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO -> if (lowActive || mediumActive || highActive) StandaloneFanStage.AUTO.ordinal else -1
            HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_LOW -> if (lowActive) StandaloneFanStage.LOW_CUR_OCC.ordinal else -1
            HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_MED -> if (mediumActive) StandaloneFanStage.MEDIUM_CUR_OCC.ordinal else -1
            HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_HIGH -> if (highActive) StandaloneFanStage.HIGH_CUR_OCC.ordinal else -1
            else -> -1
        }
    }

    val fanMode = when (possibleMode) {
        PossibleFanMode.OFF -> getFanLevel()
        PossibleFanMode.LOW -> getFanLevel(lowActive = true)
        PossibleFanMode.MED -> getFanLevel(mediumActive = true)
        PossibleFanMode.HIGH -> getFanLevel(highActive = true)
        PossibleFanMode.LOW_MED -> getFanLevel(lowActive = true, mediumActive = true)
        PossibleFanMode.LOW_HIGH -> getFanLevel(lowActive = true, highActive = true)
        PossibleFanMode.MED_HIGH -> getFanLevel(mediumActive = true, highActive = true)
        PossibleFanMode.LOW_MED_HIGH -> getFanLevel(lowActive = true, mediumActive = true, highActive = true)
    }
    if (fanMode != -1) {
        updateHyperStatUIPoints(equipRef, "domainName == \"${DomainName.fanOpMode}\"", fanMode.toDouble(), WhoFiledConstants.HYPERSTAT_WHO)
        CcuLog.d(L.TAG_CCU_DEVICE, "$equipRef fan mode updated to $selectedMode")
    } else {
        CcuLog.e(L.TAG_CCU_DEVICE, "Invalid fan mode possibleFanMode $possibleMode selectedMode $selectedMode")
    }
}

private fun updatePhysicalInputs(point: PhysicalPoint, value: Double, equipRef: String) {
    try {
        val physicalPoint = point.readPoint()
        if (physicalPoint.pointRef != null) {
            when (point.domainName) {
                DomainName.analog1In, DomainName.analog2In -> {
                    point.writeHisVal(value)
                    updateAnalogOut(physicalPoint.pointRef, (value / 1000), equipRef)
                }

                DomainName.th1In, DomainName.th2In -> {
                    point.writeHisVal((value / 100))
                    updateThermistor(physicalPoint.pointRef, value, equipRef)
                }
            }
        }
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Error in updatePhysicalInputs $point", e)
    }

}

private fun updateAnalogOut(logicalPointId: String, value: Double, equipRef: String) {
    val logicalPoint = hayStack.readMapById(logicalPointId)
    val logicalDomainName = logicalPoint[Tags.DOMAIN_NAME].toString()
    CcuLog.d(L.TAG_CCU_DEVICE, "Analog mapping: $logicalDomainName Raw value: $value equipRef: $equipRef")

    val logicalValue = when (logicalDomainName) {
        DomainName.keyCardSensor, DomainName.doorWindowSensorTitle24 -> if (value >= 2) 1.0 else 0.0
        else -> getSensorMappedValue(logicalPoint, value)
    }
    CcuLog.d(L.TAG_CCU_DEVICE, "DomainName: $logicalDomainName , Raw value: $value, logicalValue : $logicalValue")
    updateSensorData(logicalDomainName, equipRef, logicalValue)
}

private fun updateThermistor(logicalPointId: String, value: Double, equipRef: String) {
    val logicalPoint = hayStack.readMapById(logicalPointId)
    val logicalDomainName = logicalPoint[Tags.DOMAIN_NAME].toString()
    val logicalValue = when (logicalDomainName) {
        DomainName.doorWindowSensorNCTitle24, DomainName.genericAlarmNC,
        DomainName.genericAlarmNC_th1, DomainName.genericAlarmNC_th2 -> {
            if ((value * 10) >= 10000) 1.0 else 0.0
        }

        DomainName.genericAlarmNO, DomainName.genericAlarmNO_th1, DomainName.genericAlarmNO_th2 -> {
            if ((value * 10) <= 10000) 1.0 else 0.0
        }

        else -> {
            CCUUtils.roundToOneDecimal(ThermistorUtil.getThermistorValueToTemp(value * 10))
        }
    }

    CcuLog.d(L.TAG_CCU_DEVICE, "DomainName: $logicalDomainName , Raw value: $value, logicalValue : $logicalValue")
    updateSensorData(logicalDomainName, equipRef, logicalValue)
}

private fun updateHsRssi(device: HyperStatDevice, rssi: Int) {
    val rssiPoint = device.rssi
    rssiPoint.writeHisValueByIdWithoutCOV(rssi.toDouble())
    val logicalPoint = rssiPoint.readPoint()
    if (logicalPoint.pointRef != null) {
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(logicalPoint.pointRef, 1.0)
    }
}

private fun updateTemp(device: HyperStatDevice, temp: Double) {
    val currentTemp = device.currentTemp
    currentTemp.writeHisVal(temp)
    updateLogicalPoint(currentTemp, Pulse.getRoomTempConversion(temp))
}

private fun updateOccupancy(device: HyperStatDevice, occupancyDetected: Boolean) {
    val occupancy = device.occupancySensor
    occupancy.writeHisVal(if (occupancyDetected) 1.0 else 0.0)
    updateLogicalPoint(occupancy, if (occupancyDetected) 1.0 else 0.0)
}

private fun updateIlluminance(device: HyperStatDevice, illuminance: Double) {
    val illuminanceSensor = device.illuminanceSensor
    illuminanceSensor.writeHisVal(illuminance)
    updateLogicalPoint(illuminanceSensor, illuminance)
}

private fun updateHumidity(device: HyperStatDevice, humidity: Double) {
    val humiditySensor = device.humiditySensor
    humiditySensor.writeHisVal(humidity)
    updateLogicalPoint(humiditySensor, CCUUtils.roundToOneDecimal(humidity / 10.0))
}

private fun updateSound(device: HyperStatDevice, sound: Double) {
    val soundSensor = device.soundSensor
    soundSensor.writeHisVal(sound)
    updateLogicalPoint(soundSensor, sound)
}

private fun updateLogicalPoint(point: PhysicalPoint, value: Double) {
    CcuLog.d(L.TAG_CCU_DEVICE, "PhysicalPoint: ${point.domainName}, value: $value")
    val logicalPoint = point.readPoint()
    if (logicalPoint.pointRef != null) {
        CCUHsApi.getInstance().writeHisValById(logicalPoint.pointRef, value)
    }
}

private fun updateSensorData(domainName: String, equipRef: String, value: Double) {
    hayStack.writeHisValByQuery("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"", value)
}


private fun getSensorMappedValue(point: HashMap<Any, Any>, rawValue: Double): Double {
    /*Analog's range is 0 to 10, if CCU receive 5v it means its 50 perc so we need to multiply by 10*/
    val deciVolts = rawValue * 10

    try {
        val min = point[Tags.MIN_VAL].toString().toDouble()
        val max = point[Tags.MAX_VAL].toString().toDouble()
        val analogConversion = ((max - min) * (deciVolts / 100.0)) + min
        return CCUUtils.roundToTwoDecimal(analogConversion)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Error in getSensorMappedValue $point", e)
    }
    return 0.0
}

private fun updateDynamicSensors(device: HyperStatDevice, sensorReadings: List<SensorReadingPb_t>, equipRef: String, nodeAddress: Int) {
    sensorReadings.forEach { readings ->
        try {
            val sensorType = SensorType.values()[readings.sensorType]
            var physicalValue = readings.sensorData.toDouble()
            var logicalValue = readings.sensorData.toDouble()
            val port = sensorType.sensorPort

            if (sensorType == SensorType.NONE )
                return@forEach

            val sensorPoint = getSensorPoint(device, sensorType)
            if (sensorPoint == null) {
                CcuLog.e(L.TAG_CCU_DEVICE, "Sensor type not found for $sensorType")
                return@forEach
            }

            when(sensorType) {
                SensorType.HUMIDITY -> {
                    physicalValue /= 10
                    logicalValue /= 10
                }
                SensorType.PRESSURE -> logicalValue = Pulse.convertPressureFromPaToInH2O(physicalValue)
                else -> { }
            }

            updateSensorValue(sensorPoint, sensorType, physicalValue, logicalValue, equipRef)
            CcuLog.d(L.TAG_CCU_DEVICE, "sensorType: $sensorType, sensorPoint: ${sensorPoint.domainName} physicalValue: $physicalValue, logicalValue: $logicalValue, port: $port")
        } catch (e: Exception) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Error in updateDynamicSensors $readings", e)
        }
    }
}

private fun getSensorPoint(device: HyperStatDevice, sensorType: SensorType): PhysicalPoint? {
    return when (sensorType) {
        SensorType.CO2 -> device.co2Sensor
        SensorType.HUMIDITY -> device.humiditySensor
        SensorType.OCCUPANCY -> device.occupancySensor
        SensorType.SOUND -> device.soundSensor
        SensorType.CO2_EQUIVALENT -> device.co2Equivalent
        SensorType.ILLUMINANCE -> device.illuminanceSensor
        SensorType.PM2P5 -> device.pm25Sensor
        SensorType.PM10 -> device.pm10Sensor
        SensorType.PRESSURE -> device.pressureSensor
        SensorType.UVI -> device.uviSensor
        else -> { null }
    }
}


fun updateSensorValue(
        physicalPoint: PhysicalPoint?, sensorType: SensorType,
        physicalValue: Double, logicalValue: Double, equipRef: String) {

    fun getLogicalSensorForPhysicalPoint(physicalDomainName: String): String? {
        return when (physicalDomainName) {
            DomainName.co2Sensor -> DomainName.zoneCo2
            DomainName.humiditySensor -> DomainName.zoneHumidity
            DomainName.occupancySensor -> DomainName.zoneOccupancy
            DomainName.soundSensor -> DomainName.zoneSound
            DomainName.co2EquivalentSensor -> DomainName.zoneCo2Equivalent
            DomainName.illuminanceSensor -> DomainName.zoneIlluminance
            DomainName.pm25Sensor -> DomainName.zonePm25
            DomainName.pm10Sensor -> DomainName.zonePm10
            DomainName.uviSensor -> DomainName.zoneUvi
            DomainName.pressureSensor -> DomainName.zonePressureSensor
            else -> null
        }
    }

    fun createSensorPoint(domainName: String, physicalPoint: PhysicalPoint) {
        val pointsUtil = PointsUtil(CCUHsApi.getInstance())
        val equip = HSUtil.getEquipInfo(equipRef)
        val pointRef: String?
        val isSensorExist = isSensorExist(domainName, equipRef)
        if (isSensorExist != null) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Sensor point already exist for $domainName")
            pointRef = isSensorExist
        } else {
            pointRef = pointsUtil.createDynamicSensorEquipPoint(equip, domainName, getConfiguration(equipRef)!!)
            if (pointRef == null) {
                CcuLog.e(L.TAG_CCU_DEVICE, "Unable to create sensor point for $domainName")
                return
            }
        }
        CcuLog.e(L.TAG_CCU_DEVICE, "Dynamic sensor created for $domainName : id : $pointRef")
        val rowPoint = (RawPoint.Builder().setHashMap(physicalPoint.domainName.readPhysicalPoint(physicalPoint.deviceRef) as HashMap)).setPointRef(pointRef).build()
        CCUHsApi.getInstance().updatePoint(rowPoint, rowPoint.id)
        CCUHsApi.getInstance().scheduleSync()
    }

    if (physicalPoint == null) {
        CcuLog.e(L.TAG_CCU_DEVICE, "PhysicalPoint Sensor not found for $sensorType")
        return
    }
    CcuLog.e(L.TAG_CCU_DEVICE, "PhysicalPoint Sensor not found for ${physicalPoint.id}")

    if (physicalPoint.readPoint().pointRef == null) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Logical sensor point is not found creating new point ${physicalPoint.domainName}")
        val logicalSensorName = getLogicalSensorForPhysicalPoint(physicalPoint.domainName)
        if (logicalSensorName != null) {
            createSensorPoint(logicalSensorName, physicalPoint)
        } else {
            CcuLog.e(L.TAG_CCU_DEVICE, "Logical sensor point is not available for ${physicalPoint.domainName}")
        }
    }
    physicalPoint.writeHisVal(physicalValue)
    updateLogicalPoint(physicalPoint , logicalValue)

}


fun sendAcknowledge(address: Int) {
    CcuLog.i(L.TAG_CCU_DEVICE, "Sending Acknowledgement")
    if (!LSerial.getInstance().isConnected) {
        CcuLog.d(L.TAG_CCU_DEVICE, "Device not connected !!")
        return
    }
    val msg = CcuToCmOverUsbDeviceTempAckMessage_t()
    msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SET_TEMPERATURE_ACK)
    msg.smartNodeAddress.set(address)
    MeshUtil.sendStructToCM(msg)
}

fun runProfileAlgo(nodeAddress: Short) {
    when (val profile = L.getProfile(nodeAddress)) {
        is HyperStatCpuProfile -> {
            profile.processHyperStatCPUProfile(profile.getProfileDomainEquip(nodeAddress.toInt()))
        }

        is HyperStatHpuProfile -> {
            profile.processHyperStatHpuProfile(profile.getHyperStatEquip(nodeAddress) as HyperStatHpuEquipToBeDeleted)
        }

        is HyperStatPipe2Profile -> {
            profile.processHyperStatPipeProfile(profile.getHyperStatEquip(nodeAddress) as HyperStatPipe2EquipToBeDeleted)
        }
    }
}

fun isSensorExist(domainName:String, equipRef: String): String?{
    val sensorPoint = hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    if (sensorPoint.isNotEmpty())
        return sensorPoint[Tags.ID].toString()
    return null
}