package a75f.io.device.mesh.mystat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.constants.WhoFiledConstants
import a75f.io.device.MyStat
import a75f.io.device.mesh.DLog
import a75f.io.device.mesh.DeviceUtil
import a75f.io.device.mesh.Pulse
import a75f.io.device.mesh.ThermistorUtil
import a75f.io.device.mesh.getSensorMappedValue
import a75f.io.device.mesh.hyperstat.sendAcknowledge
import a75f.io.device.mesh.mystat.MyStatMsgSender.sendControlMessage
import a75f.io.device.mesh.updateCo2
import a75f.io.device.mesh.updateHsRssi
import a75f.io.device.mesh.updateHumidity
import a75f.io.device.mesh.updateLogicalPoint
import a75f.io.device.mesh.updateOccupancy
import a75f.io.device.mesh.updateTemp
import a75f.io.domain.api.Domain.getDomainEquip
import a75f.io.domain.api.Domain.hayStack
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.hvac.MyStatFanStages
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.mystat.profiles.fancoilunit.pipe2.MyStatPipe2Profile
import a75f.io.logic.bo.building.mystat.profiles.packageunit.cpu.MyStatCpuProfile
import a75f.io.logic.bo.building.mystat.profiles.packageunit.hpu.MyStatHpuProfile
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatPossibleConditioningMode
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatPossibleFanMode
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatCpuFanLevel
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatHpuFanLevel
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatPipe2FanLevel
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatPossibleFanModeSettings
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.interfaces.ZoneDataInterface
import a75f.io.logic.util.uiutils.updateUserIntentPoints
import java.util.Calendar

/**
 * Created by Manjunath K on 13-01-2025.
 */

fun handleMyStatRegularUpdate(
    regularUpdateMsg: MyStat.MyStatRegularUpdateMessage_t,
    address: Int,
    reference: ZoneDataInterface?
) {
    if (DLog.isLoggingEnabled()) {
        CcuLog.i(L.TAG_CCU_DEVICE, "handleRegularUpdate: $regularUpdateMsg")
    }

    val device = getMyStatDevice(address)!!
    Pulse.mDeviceUpdate[address.toShort()] = Calendar.getInstance().timeInMillis

    val equipRef = device[Tags.EQUIPREF].toString()
    val myStatDevice = getMyStatDomainDevice(device[Tags.ID].toString(), equipRef)

    CcuLog.d(
        L.TAG_CCU_DEVICE, "MyStat RegularUpdate: nodeAddress $address :  $regularUpdateMsg"
    )
    if (Globals.getInstance().isTemporaryOverrideMode) {
        updateHsRssi(myStatDevice.rssi, regularUpdateMsg.regularUpdateCommon.rssi)
        reference?.refreshHeartBeatStatus(address.toString())
        return
    }

    regularUpdateMsg.apply {
        updateHsRssi(myStatDevice.rssi, regularUpdateCommon.rssi)
        updateTemp(myStatDevice.currentTemp, regularUpdateCommon.roomTemperature.toDouble(), address, reference)
        updateOccupancy(myStatDevice.occupancySensor, regularUpdateCommon.occupantDetected)
        updateHumidity(myStatDevice.humiditySensor, regularUpdateCommon.humidity.toDouble())
        updateCo2(myStatDevice.co2Sensor, regularUpdateCommon.co2.toDouble())
        updateDoorWindowStatus(address, doorWindowSensor)
        updateUniversalInput(myStatDevice.universal1In, universalInput1Value)
    }
    
    

    CcuLog.d(L.TAG_CCU_DEVICE, "MyStat Regular Update completed")
    reference?.refreshHeartBeatStatus(address.toString())
}

fun handleMyStatOverrideMessage(
    message: MyStat.MyStatLocalControlsOverrideMessage_t, nodeAddress: Int
) {
    val device = getMyStatDevice(nodeAddress)
    val hsEquip = Equip.Builder().setHashMap(device).build()
    if (hsEquip.markers.contains(Tags.MYSTAT)) {
        handleOverrideMsg(message, nodeAddress, hsEquip.equipRef)
    }
}

fun handleOverrideMsg(
    message: MyStat.MyStatLocalControlsOverrideMessage_t, nodeAddress: Int, equipRef: String
) {

    CcuLog.d(L.TAG_CCU_DEVICE, "MyStat Override: nodeAddress $nodeAddress :  $message")
    val equip = getDomainEquip(equipRef) as MyStatEquip
    updateDesiredTemp(equip, message)
    updateModes(equip, message, nodeAddress)
    runProfileAlgo(nodeAddress.toShort())
    sendControlMessage(nodeAddress)
    sendAcknowledge(nodeAddress) // reuse same message because there is no difference
}


fun runProfileAlgo(nodeAddress: Short) {
    when (val profile = L.getProfile(nodeAddress)) {
        is MyStatPipe2Profile -> {
            profile.processPipe2Profile(profile.getProfileDomainEquip(nodeAddress.toInt()))
        }
        is MyStatHpuProfile -> {
            profile.processHpuProfile(profile.getProfileDomainEquip(nodeAddress.toInt()))
        }
        is MyStatCpuProfile -> {
            profile.processCpuProfile(profile.getProfileDomainEquip(nodeAddress.toInt()))
        }
    }
}

private fun updateDesiredTemp(
    equip: MyStatEquip, message: MyStat.MyStatLocalControlsOverrideMessage_t
) {

    val coolingDesiredTemp = message.setTempCooling.toDouble() / 2
    val heatingDesiredTemp = message.setTempHeating.toDouble() / 2
    val avgDesiredTemp = (coolingDesiredTemp + heatingDesiredTemp) / 2
    equip.desiredTempCooling.writePointValue(coolingDesiredTemp)
    equip.desiredTempHeating.writePointValue(heatingDesiredTemp)
    equip.desiredTemp.writePointValue(avgDesiredTemp)
    val haystack = CCUHsApi.getInstance()

    val coolingPoint = Point.Builder().setHashMap(haystack.readMapById(equip.desiredTempCooling.id)).build()
    val heatingPoint = Point.Builder().setHashMap(haystack.readMapById(equip.desiredTempHeating.id)).build()
    val desiredPoint = Point.Builder().setHashMap(haystack.readMapById(equip.desiredTemp.id)).build()
    CcuLog.d(
        L.TAG_CCU_DEVICE,
        "device overriding CoolingDesiredTemp: $coolingDesiredTemp HeatingDesiredTemp: $heatingDesiredTemp AvgDesiredTemp: $avgDesiredTemp"
    )
    DeviceUtil.updateDesiredTempFromDevice(
        coolingPoint,
        heatingPoint,
        desiredPoint,
        coolingDesiredTemp,
        heatingDesiredTemp,
        avgDesiredTemp,
        hayStack,
        WhoFiledConstants.HYPERSTAT_WHO
    )
}

private fun updateModes(
    equip: MyStatEquip, message: MyStat.MyStatLocalControlsOverrideMessage_t, nodeAddress: Int
) {
    var possibleMode = MyStatPossibleConditioningMode.OFF
    var possibleFanMode = MyStatPossibleFanMode.OFF

    when (L.getProfile(nodeAddress.toShort())) {
        is MyStatCpuProfile -> {
            val configs = getMyStatConfiguration(equip.equipRef) as MyStatCpuConfiguration
            possibleFanMode = getMyStatPossibleFanModeSettings(getMyStatCpuFanLevel(configs))
            possibleMode = getMyStatPossibleConditionMode(configs)
        }

        is MyStatHpuProfile -> {
            val configs = getMyStatConfiguration(equip.equipRef) as MyStatHpuConfiguration
            possibleFanMode = getMyStatPossibleFanModeSettings(getMyStatHpuFanLevel(configs))
            possibleMode = getMyStatPossibleConditionMode(configs)
        }

        is MyStatPipe2Profile -> {
            val configs = getMyStatConfiguration(equip.equipRef) as MyStatPipe2Configuration
            possibleFanMode = getMyStatPossibleFanModeSettings(getMyStatPipe2FanLevel(configs))
            possibleMode = getMyStatPossibleConditionMode(configs)
        }
    }
    updateFanMode(possibleFanMode, message.fanSpeed, equip)
    updateConditioningMode(possibleMode, message.conditioningMode, equip)
}


private fun updateConditioningMode(
    possibleMode: MyStatPossibleConditioningMode,
    mode: MyStat.MyStatConditioningMode_e,
    equip: MyStatEquip
) {

    val isValidMode = when (possibleMode) {
        MyStatPossibleConditioningMode.OFF -> (mode == MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_OFF)
        MyStatPossibleConditioningMode.COOL_ONLY -> (mode != MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_HEATING)
        MyStatPossibleConditioningMode.HEAT_ONLY -> (mode != MyStat.MyStatConditioningMode_e.MYSTAT_CONDITIONING_MODE_COOLING)
        MyStatPossibleConditioningMode.BOTH -> true // anything is fine
    }

    if (!isValidMode) {
        CcuLog.i(
            L.TAG_CCU_DEVICE,
            "Invalid selected $mode possibleMode $possibleMode Invalid conditioning mode "
        )
        return
    }
    updateUserIntentPoints(
        equip.equipRef,
        equip.conditioningMode,
        mode.ordinal.toDouble(),
        WhoFiledConstants.MYSTAT_WHO
    )
    CcuLog.d(L.TAG_CCU_DEVICE, "${equip.nodeAddress} conditioning mode updated to $mode")
}


private fun updateFanMode(
    possibleMode: MyStatPossibleFanMode, selectedMode: MyStat.MyStatFanSpeed_e, equip: MyStatEquip
) {

    fun getFanLevel(lowActive: Boolean = false, highActive: Boolean = false, isFanEnabledPresent: Boolean = false): Int {
        return when (selectedMode) {
            MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_OFF -> MyStatFanStages.OFF.ordinal
            MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_AUTO -> if (lowActive || highActive || isFanEnabledPresent ) MyStatFanStages.AUTO.ordinal else -1
            MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_LOW -> if (lowActive) MyStatFanStages.LOW_CUR_OCC.ordinal else -1
            MyStat.MyStatFanSpeed_e.MYSTAT_FAN_SPEED_HIGH -> if (highActive) MyStatFanStages.HIGH_CUR_OCC.ordinal else -1
            else -> -1
        }
    }

    val fanMode = when (possibleMode) {
        MyStatPossibleFanMode.OFF -> getFanLevel()
        MyStatPossibleFanMode.LOW -> getFanLevel(lowActive = true)
        MyStatPossibleFanMode.HIGH -> getFanLevel(highActive = true)
        MyStatPossibleFanMode.LOW_HIGH -> getFanLevel(lowActive = true, highActive = true)
        MyStatPossibleFanMode.AUTO -> getFanLevel(isFanEnabledPresent = true)
    }
    if (fanMode != -1) {
        updateUserIntentPoints(
            equip.equipRef,
            equip.fanOpMode,
            fanMode.toDouble(),
            WhoFiledConstants.MYSTAT_WHO
        )
        CcuLog.d(L.TAG_CCU_DEVICE, "${equip.equipRef} fan mode updated to $selectedMode")
    } else {
        CcuLog.e(
            L.TAG_CCU_DEVICE,
            "Invalid fan mode possibleFanMode $possibleMode selectedMode $selectedMode"
        )
    }
}


/*

Universal Input:

        In a Normally open sensor if the resistance values are like this
        > 10,000 ohm - 0
        < 10,000 ohm - 1
        and in a normally closed sensor if the resistance values are like this
        < 10,000 ohm - 0
        > 10,000 ohm - 1

 */

private fun updateUniversalInput(point: PhysicalPoint, value: Int) {
    CcuLog.d(L.TAG_CCU_DEVICE, "Update input -> ${point.domainName} = $value")
    try {

        fun updatePhysicalInputs(isThermistor: Boolean, rawValue: Double) {
            hayStack.writeHisValById(
                point.id, if (isThermistor) (rawValue / 100) else (rawValue / 1000)
            )
        }

        val physicalPoint = point.readPoint()
        if (physicalPoint.pointRef != null) {

            val logicalPoint = hayStack.readMapById(physicalPoint.pointRef)
            val logicalDomainName = logicalPoint[Tags.DOMAIN_NAME].toString()
            val isThermistor = getBit(value) == 1  // 15th bit is 0 then voltage input if 1 then thermistor
            val rawData = (getBits(value)).toDouble()
            CcuLog.d(
                L.TAG_CCU_DEVICE,
                "updateUniversalInput: rawData $rawData logicalDomainName $logicalDomainName"
            )
            updatePhysicalInputs(isThermistor, rawData)

            fun deriveValue(): Double {
                return if (isThermistor) {
                    if ((rawData * 10) <= 10000) 0.0 else 1.0
                } else {
                    val analogData = rawData / 1000
                    if (analogData >= 2) 1.0 else 0.0
                }
            }

            val logicalValue = when (logicalDomainName) {
                DomainName.leavingWaterTemperature,
                DomainName.dischargeAirTemperature -> {
                    CCUUtils.roundToOneDecimal(ThermistorUtil.getThermistorValueToTemp(rawData * 10))
                }
                DomainName.doorWindowSensorNCTitle24,
                DomainName.genericAlarmNC -> { if ((rawData * 10) >= 10000) 1.0 else 0.0 }

                DomainName.genericAlarmNO -> { if ((rawData * 10) <= 10000) 1.0 else 0.0 }
                DomainName.keyCardSensor,
                DomainName.doorWindowSensorTitle24 -> { deriveValue() }
                else -> {
                    if (isThermistor) {
                        CCUUtils.roundToOneDecimal(ThermistorUtil.getThermistorValueToTemp(rawData * 10))
                    } else {
                        getSensorMappedValue(logicalPoint, rawData)
                    }
                }
            }
            CcuLog.d(L.TAG_CCU_DEVICE, "sensor input point : $logicalDomainName, isThermistor: $isThermistor Raw Value: $rawData logicalValue: $logicalValue")
            updateLogicalPoint(point, logicalValue)
        }
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Error in updatePhysicalInputs $point", e)
    }
}

/**
 * initial discussion is to have door window using bluetooth sensor for mystat pipe 2 profile
 *
 */
private fun updateDoorWindowStatus(address: Int, value: Int) {
    val profile = L.getProfile(address.toShort())
    if (profile is MyStatPipe2Profile && value == 1) {
        val myStatEquip = profile.getProfileDomainEquip(address)
        // TODO check what will be the actual values
        profile.doorWindowIsOpen(1.0, 1.0, myStatEquip)
    }

}

private fun getBit(n: Int, index: Int = 15) = ((n shr index) and 1)

private fun getBits(n: Int, start: Int = 0, end: Int = 14) =
    n and (((1 shl (end - start + 1)) - 1) shl start)


