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
import a75f.io.device.mesh.getSensorMappedValue
import a75f.io.device.mesh.updateHsRssi
import a75f.io.device.mesh.updateHumidity
import a75f.io.device.mesh.updateIlluminance
import a75f.io.device.mesh.updateLogicalPoint
import a75f.io.device.mesh.updateOccupancy
import a75f.io.device.mesh.updateSensorData
import a75f.io.device.mesh.updateSound
import a75f.io.device.mesh.updateTemp
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
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuProfile
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getCpuFanLevel
import a75f.io.logic.bo.building.hyperstat.profiles.util.getHpuFanLevel
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPipe2FanLevel
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPossibleConditionMode
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPossibleFanModeSettings
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.sensors.SensorType
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.interfaces.ZoneDataInterface
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler.Companion.updateHyperStatUIPoints

/**
 * Created by Manjunath K on 22-10-2024.
 */

fun handleRegularUpdate(regularUpdateMessage: HyperStatRegularUpdateMessage_t, device: HashMap<Any, Any>, nodeAddress: Int, refresh: ZoneDataInterface?) {
    val equipRef = device[Tags.EQUIPREF].toString()
    val hsDevice = getHyperStatDomainDevice(device[Tags.ID].toString(), equipRef)

    CcuLog.d(L.TAG_CCU_DEVICE, "HyperStat RegularUpdate: nodeAddress $nodeAddress :  $regularUpdateMessage")
    if (Globals.getInstance().isTemporaryOverrideMode) {
        updateHsRssi(hsDevice.rssi, regularUpdateMessage.rssi)
        refresh?.refreshHeartBeatStatus(nodeAddress.toString())
        return
    }
    updateHsRssi(hsDevice.rssi, regularUpdateMessage.rssi)
    updateTemp(hsDevice.currentTemp, regularUpdateMessage.roomTemperature.toDouble(),nodeAddress,refresh)

    updatePhysicalInputs(hsDevice.analog1In, regularUpdateMessage.externalAnalogVoltageInput1.toDouble(), equipRef)
    updatePhysicalInputs(hsDevice.analog2In, regularUpdateMessage.externalAnalogVoltageInput2.toDouble(), equipRef)
    updatePhysicalInputs(hsDevice.th1In, regularUpdateMessage.externalThermistorInput1.toDouble(), equipRef)
    updatePhysicalInputs(hsDevice.th2In, regularUpdateMessage.externalThermistorInput2.toDouble(), equipRef)

    updateOccupancy(hsDevice.occupancySensor, regularUpdateMessage.occupantDetected)
    updateIlluminance(hsDevice.illuminanceSensor, regularUpdateMessage.illuminance.toDouble())
    updateHumidity(hsDevice.humiditySensor, regularUpdateMessage.humidity.toDouble())
    updateSound(hsDevice.soundSensor, regularUpdateMessage.illuminance.toDouble())
    updateDynamicSensors(hsDevice, regularUpdateMessage.sensorReadingsList, equipRef)

    CcuLog.d(L.TAG_CCU_DEVICE, "HyperStat Regular Update completed")

    refresh?.refreshHeartBeatStatus(nodeAddress.toString())
}

fun handleOverrideMsg(message: HyperStatLocalControlsOverrideMessage_t, nodeAddress: Int, equipRef: String) {

    CcuLog.d(L.TAG_CCU_DEVICE, "HyperStat Override: nodeAddress $nodeAddress :  $message")
    val equip = getDomainEquip(equipRef) as HyperStatEquip
    updateDesiredTemp(equip, message)
    updateModes(equip, message, nodeAddress)
    runProfileAlgo(nodeAddress.toShort())
    HyperStatMessageSender.sendControlMessage(nodeAddress)
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
        is HyperStatHpuProfile -> {
            val configs = getConfiguration(equip.equipRef) as HpuConfiguration
            possibleFanMode = getPossibleFanModeSettings(getHpuFanLevel(configs))
            possibleMode = getPossibleConditionMode(configs)
        }
        is HyperStatPipe2Profile-> {
            val configs = getConfiguration(equip.equipRef) as Pipe2Configuration
            possibleFanMode = getPossibleFanModeSettings(getPipe2FanLevel(configs))
            possibleMode = getPossibleConditionMode(configs)
        }
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
        /*For Generic(1-100)kohms, CCU just need to convertto kOhms*/
        DomainName.airTempSensor100kOhms_th1, DomainName.airTempSensor100kOhms_th2 -> {
            value/100
        }
        DomainName.keyCardSensor -> {
            if ((value * 10) <= 10000) 0.0 else 1.0
        }

        else -> {
            CCUUtils.roundToOneDecimal(ThermistorUtil.getThermistorValueToTemp(value * 10))
        }
    }

    CcuLog.d(L.TAG_CCU_DEVICE, "DomainName: $logicalDomainName , Raw value: $value, logicalValue : $logicalValue")
    updateSensorData(logicalDomainName, equipRef, logicalValue)
}

private fun updateDynamicSensors(device: HyperStatDevice, sensorReadings: List<SensorReadingPb_t>, equipRef: String) {
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
        SensorType.VOC -> device.vocSensor
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
            DomainName.vocSensor -> DomainName.zoneVoc
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
            profile.processHyperStatHpuProfile(profile.getProfileDomainEquip(nodeAddress.toInt()))
        }

        is HyperStatPipe2Profile -> {
            profile.processHyperStatPipeProfile(profile.getProfileDomainEquip(nodeAddress.toInt()))
        }
    }
}

fun isSensorExist(domainName:String, equipRef: String): String?{
    val sensorPoint = hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    if (sensorPoint.isNotEmpty())
        return sensorPoint[Tags.ID].toString()
    return null
}