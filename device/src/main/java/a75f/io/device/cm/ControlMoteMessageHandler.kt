package a75f.io.device.cm

import a75f.io.device.ControlMote
import a75f.io.device.ControlMote.CM_SensorBusReadings_t
import a75f.io.device.mesh.Pulse
import a75f.io.device.mesh.ThermistorUtil
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.devices.CmBoardDevice
import a75f.io.domain.equips.AdvancedHybridSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuRelayMappings
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.util.getAdvancedAhuSystemEquip
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.util.CCUUtils

fun handleCMRegularUpdate(data : ByteArray) {

    CcuLog.d(L.TAG_CCU_SERIAL, "CM handleCMRegularUpdate :" + data.contentToString())
    val messageArray = data.copyOfRange(1, data.size)
    CcuLog.d(
        L.TAG_CCU_SERIAL,
        "CM handleCMRegularUpdate messageArray :" + messageArray.contentToString()
    )

    val updateMsg = ControlMote.CmToCcuOverUsbCmSerialRegularUpdate_t.parseFrom(messageArray)
    CcuLog.d(L.TAG_CCU_SERIAL, "CM handleCMRegularUpdate updateMsg :$updateMsg")
    updateMsg?.let { msg ->
        if (msg.hasSensorBusReadings()) {
            printSensorBusData(msg.sensorBusReadings)
            updateSensorBusData(msg.sensorBusReadings)
        }
        if (msg.hasRelayBitMapStatus()) {
            CcuLog.d(L.TAG_CCU_SERIAL, "CM handleCMRegularUpdate : relayBitMapStatus "+updateMsg.relayBitMapStatus)
            updateRelayFeedBackStatus(updateMsg.relayBitMapStatus)
        }
        updateAnalogOutFeedback(msg.analogOutStatusList)
        updateLoopFeedback(msg.piloopOutputList)
    }
}


fun handleAdvancedAhuCmUpdate(messageT: CmToCcuOverUsbCmRegularUpdateMessage_t) {
    try {
        val cmDevice = Domain.cmBoardDevice
        if (cmDevice.deviceRef.isNotEmpty()) {

            val th1RawValue = messageT.thermistor1.get().toDouble()
            val th2RawValue = messageT.thermistor2.get().toDouble()
            val analog1InRawValue = messageT.analogSense1.get().toDouble()
            val analog2InRawValue = messageT.analogSense2.get().toDouble()
            val humidity = messageT.humidity.get().toDouble()
            val roomTemperature = messageT.roomTemperature.get().toDouble()

            CcuLog.d(L.TAG_CCU_DEVICE, "Cm Regular Update : th1 : $th1RawValue th2 : $th2RawValue"
                    + " analog1 : $analog1InRawValue analog2 : $analog2InRawValue humidity: $humidity " +
                    "roomTemperature : $roomTemperature"  )

            updateThermistorInput(cmDevice.th1In.domainName, th1RawValue)
            cmDevice.th1In.writeHisVal(th1RawValue / 100)

            updateThermistorInput(cmDevice.th2In.domainName, th2RawValue)
            cmDevice.th2In.writeHisVal(th2RawValue / 100)

            updateAnalogInput(cmDevice.analog1In.domainName, analog1InRawValue / 1000)
            cmDevice.analog1In.writeHisVal(analog1InRawValue / 100)

            updateAnalogInput(cmDevice.analog2In.domainName, analog2InRawValue / 1000)
            cmDevice.analog2In.writeHisVal(analog2InRawValue / 100)

            val equip = getAdvancedAhuSystemEquip()
            updateCurrentTemp(roomTemperature, equip,cmDevice)
            updateHumidity(humidity, equip,cmDevice)
            cmDevice.rssi.writeHisVal(1.0)

        } else {
            CcuLog.e(L.TAG_CCU_DEVICE, "handleAdvancedAhuCmUpdate : cmDevice.deviceRef is empty")
        }
    } catch (exception: UninitializedPropertyAccessException) {
        CcuLog.e(L.TAG_CCU_DEVICE, "handleAdvancedAhuCmUpdate : Exception $exception")
    }
}

private fun updateCurrentTemp(roomTemperature: Double, equip: AdvancedHybridSystemEquip, cmDevice: CmBoardDevice) {
    if (equip.cmCurrentTemp.pointExists()) {
        cmDevice.currentTemp.writeHisVal(roomTemperature)
        val currentTemp = Pulse.getCMRoomTempConversion(roomTemperature, 0.0)
        equip.cmCurrentTemp.writeHisVal(currentTemp)
        CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Cm Current temp $currentTemp")
    } else {
        CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : cmCurrentTemp point not found")
    }
}

private fun updateHumidity(humidity: Double, equip: AdvancedHybridSystemEquip, cmDevice: CmBoardDevice) {
    if (equip.outsideHumidity.pointExists()) {
        cmDevice.humiditySensor.writeHisVal(humidity)
        val humidity = CCUUtils.roundToTwoDecimal(humidity / 100)
        equip.cmHumidity.writeHisVal(humidity)
        CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Humidity $humidity")
    } else {
        CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : outsideHumidity point not found")
    }
}

fun printSensorBusData(readings : CM_SensorBusReadings_t) {
    CcuLog.d(
        L.TAG_CCU_SERIAL, "CM printSensorBusReadings_t : readings.sensorAddress0Humidmsgity "+readings.sensorAddress0Humidity+
                " readings.sensorAddress0Temperature "+readings.sensorAddress0Temperature+
                " readings.sensorAddress1Humidity "+readings.sensorAddress1Humidity+
                " readings.sensorAddress1Temperature "+readings.sensorAddress1Temperature+
                " readings.sensorAddress2Humidity "+readings.sensorAddress2Humidity+
                " readings.sensorAddress2Temperature "+readings.sensorAddress2Temperature+
                " readings.sensorAddress3Humidity "+readings.sensorAddress3Humidity+
                " readings.sensorAddress3Temperature "+readings.sensorAddress3Temperature
    )
}

fun updateSensorBusData(readings: CM_SensorBusReadings_t) {
    if (isNotAdvanceAhuProfile()) {
        CcuLog.e(L.TAG_CCU_SERIAL, "CM updateSensorBusData : Skipped AdvancedAHU not configured.")
        return
    }
    val systemEquip = getAdvancedAhuSystemEquip()
    if (systemEquip.sensorBus0PressureEnable.readDefaultVal() > 0) {
        CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress0Pressure "+readings.sensorAddress0Pressure)
        updatePressureSensorPoint(systemEquip.sensorBus0PressureAssociation.readDefaultVal().toInt(), readings.sensorAddress0Pressure.toDouble(), systemEquip)
    }

    if (systemEquip.sensorBusAddress0Enable.readDefaultVal() > 0) {
        if (systemEquip.temperatureSensorBusAdd0.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress0Temperature "+readings.sensorAddress0Temperature)
            updateTemperatureSensorPoint(systemEquip.temperatureSensorBusAdd0.readDefaultVal().toInt(), readings.sensorAddress0Temperature.toDouble(), systemEquip)
        }
        if (systemEquip.humiditySensorBusAdd0.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress0Humidity "+readings.sensorAddress0Humidity)
            updateHumiditySensorPoint(systemEquip.humiditySensorBusAdd0.readDefaultVal().toInt(), readings.sensorAddress0Humidity.toDouble(), systemEquip)
        }
        if (systemEquip.occupancySensorBusAdd0.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateOccupancySensorPoint : readings.occupancySensorBusAdd0 "+readings.sensorAddress0Occupancy)
            updateOccupancySensorPoint(systemEquip.occupancySensorBusAdd0.readDefaultVal().toInt(), readings.sensorAddress0Occupancy.toDouble(), systemEquip)
        }
        if (systemEquip.co2SensorBusAdd0.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress0Co2 "+readings.sensorAddress0Co2)
            updateCo2SensorPoint(systemEquip.co2SensorBusAdd0.readDefaultVal().toInt(), readings.sensorAddress0Co2.toDouble(), systemEquip)
        }
    }
    CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : sensorBusAddress1Enable "+systemEquip.sensorBusAddress1Enable.readDefaultVal())
    if (systemEquip.sensorBusAddress1Enable.readDefaultVal() > 0) {

        if (systemEquip.temperatureSensorBusAdd1.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress1Temperature "+readings.sensorAddress1Temperature)
            updateTemperatureSensorPoint(systemEquip.temperatureSensorBusAdd1.readDefaultVal().toInt(), readings.sensorAddress1Temperature.toDouble(), systemEquip)
        }
        if (systemEquip.humiditySensorBusAdd1.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress1Humidity "+readings.sensorAddress1Humidity)
            updateHumiditySensorPoint(systemEquip.humiditySensorBusAdd1.readDefaultVal().toInt(), readings.sensorAddress1Humidity.toDouble(), systemEquip)
        }
        if (systemEquip.occupancySensorBusAdd1.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateOccupancySensorPoint : readings.occupancySensorBusAdd1 "+readings.sensorAddress1Occupancy)
            updateOccupancySensorPoint(systemEquip.occupancySensorBusAdd1.readDefaultVal().toInt(), readings.sensorAddress1Occupancy.toDouble(), systemEquip)
        }
        if (systemEquip.co2SensorBusAdd1.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress1Co2 "+readings.sensorAddress1Co2)
            updateCo2SensorPoint(systemEquip.co2SensorBusAdd1.readDefaultVal().toInt(), readings.sensorAddress1Co2.toDouble(), systemEquip)
        }
    }
    if (systemEquip.sensorBusAddress2Enable.readDefaultVal() > 0) {

        if (systemEquip.temperatureSensorBusAdd2.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress2Temperature "+readings.sensorAddress2Temperature)
            updateTemperatureSensorPoint(systemEquip.temperatureSensorBusAdd2.readDefaultVal().toInt(), readings.sensorAddress2Temperature.toDouble(), systemEquip)
        }
        if (systemEquip.humiditySensorBusAdd2.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress2Humidity "+readings.sensorAddress2Humidity)
           updateHumiditySensorPoint(systemEquip.humiditySensorBusAdd2.readDefaultVal().toInt(), readings.sensorAddress2Humidity.toDouble(), systemEquip)
        }
        if (systemEquip.occupancySensorBusAdd2.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateOccupancySensorPoint : readings.occupancySensorBusAdd2 "+readings.sensorAddress2Occupancy)
            updateOccupancySensorPoint(systemEquip.occupancySensorBusAdd2.readDefaultVal().toInt(), readings.sensorAddress2Occupancy.toDouble(), systemEquip)
        }
        if (systemEquip.co2SensorBusAdd2.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress2Co2 "+readings.sensorAddress2Co2)
            updateCo2SensorPoint(systemEquip.co2SensorBusAdd2.readDefaultVal().toInt(), readings.sensorAddress2Co2.toDouble(), systemEquip)
        }
    }

    if (systemEquip.sensorBusAddress3Enable.readDefaultVal() > 0) {
        if (systemEquip.temperatureSensorBusAdd3.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress3Temperature "+readings.sensorAddress3Temperature)
            updateTemperatureSensorPoint(systemEquip.temperatureSensorBusAdd3.readDefaultVal().toInt(), readings.sensorAddress3Temperature.toDouble(), systemEquip)
        }
        if (systemEquip.humiditySensorBusAdd3.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress3Humidity "+readings.sensorAddress3Humidity)
            updateHumiditySensorPoint(systemEquip.humiditySensorBusAdd3.readDefaultVal().toInt(), readings.sensorAddress3Humidity.toDouble(), systemEquip)
        }
        if (systemEquip.occupancySensorBusAdd3.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateOccupancySensorPoint : readings.occupancySensorBusAdd0 "+readings.sensorAddress3Occupancy)
            updateOccupancySensorPoint(systemEquip.occupancySensorBusAdd3.readDefaultVal().toInt(), readings.sensorAddress3Occupancy.toDouble(), systemEquip)
        }
        if (systemEquip.co2SensorBusAdd3.pointExists()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData : readings.sensorAddress3Co2 "+readings.sensorAddress3Co2)
           updateCo2SensorPoint(systemEquip.co2SensorBusAdd3.readDefaultVal().toInt(), readings.sensorAddress3Co2.toDouble(), systemEquip)
        }
    }
}

fun updatePressureSensorPoint(pressureSensorMapping : Int, sensorVal : Double, systemEquip: AdvancedHybridSystemEquip) {
    val scaledSensorVal = sensorVal * 0.0040146 //Convert from Pa to inch of wc
    when (pressureSensorMapping) {
        PressureSensorBusMapping.ductStaticPressure12.ordinal -> systemEquip.ductStaticPressureSensor12.writeHisVal(scaledSensorVal)
        PressureSensorBusMapping.ductStaticPressure22.ordinal -> systemEquip.ductStaticPressureSensor22.writeHisVal(scaledSensorVal)
        PressureSensorBusMapping.ductStaticPressure32.ordinal -> systemEquip.ductStaticPressureSensor32.writeHisVal(scaledSensorVal)
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updatePressureSensorPoint : Invalid pressure sensor mapping $pressureSensorMapping")
        }
    }
}

fun updateTemperatureSensorPoint(tempSensorMapping : Int, sensorVal: Double, systemEquip: AdvancedHybridSystemEquip) {
    val scaledSensorVal = sensorVal / 10
    when (tempSensorMapping) {
        TemperatureSensorBusMapping.returnAirTempature.ordinal -> systemEquip.returnAirTemperature.writeHisVal(scaledSensorVal)
        TemperatureSensorBusMapping.mixedAirTemperature.ordinal -> systemEquip.mixedAirTemperature.writeHisVal(scaledSensorVal)
        TemperatureSensorBusMapping.supplyAirTemperature1.ordinal -> systemEquip.supplyAirTemperature1.writeHisVal(scaledSensorVal)
        TemperatureSensorBusMapping.supplyAirTemperature2.ordinal -> systemEquip.supplyAirTemperature2.writeHisVal(scaledSensorVal)
        TemperatureSensorBusMapping.supplyAirTemperature3.ordinal -> systemEquip.supplyAirTemperature3.writeHisVal(scaledSensorVal)
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateTemperatureSensorPoint : Invalid temperature sensor mapping $tempSensorMapping")
        }
    }
}

fun updateHumiditySensorPoint(humiditySensorMapping : Int, sensorVal: Double, systemEquip: AdvancedHybridSystemEquip) {
    val scaledSensorVal = sensorVal / 10
    when (humiditySensorMapping) {
        HumiditySensorBusMapping.returnAirHumidity.ordinal -> systemEquip.returnAirHumidity.writeHisVal(scaledSensorVal)
        HumiditySensorBusMapping.mixedAirHumidity.ordinal -> systemEquip.mixedAirHumidity.writeHisVal(scaledSensorVal)
        HumiditySensorBusMapping.supplyAirHumidity1.ordinal -> systemEquip.supplyAirHumidity1.writeHisVal(scaledSensorVal)
        HumiditySensorBusMapping.supplyAirHumidity2.ordinal -> systemEquip.supplyAirHumidity2.writeHisVal(scaledSensorVal)
        HumiditySensorBusMapping.supplyAirHumidity3.ordinal -> systemEquip.supplyAirHumidity3.writeHisVal(scaledSensorVal)
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateHumiditySensorPoint : Invalid humidity sensor mapping $humiditySensorMapping")
        }
    }
}

fun updateOccupancySensorPoint(occupancySensorMapping : Int, sensorVal: Double, systemEquip: AdvancedHybridSystemEquip) {
    when (occupancySensorMapping) {
        OccupancySensorBusMapping.occupancySensor1.ordinal -> systemEquip.occupancySensor1.writeHisVal(sensorVal)
        OccupancySensorBusMapping.occupancySensor2.ordinal -> systemEquip.occupancySensor2.writeHisVal(sensorVal)
        OccupancySensorBusMapping.occupancySensor3 .ordinal-> systemEquip.occupancySensor3.writeHisVal(sensorVal)
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateOccupancySensorPoint : Invalid occupancy sensor mapping $occupancySensorMapping")
        }
    }
}

fun updateCo2SensorPoint(co2SensorMapping : Int, sensorVal: Double, systemEquip: AdvancedHybridSystemEquip) {
    when (co2SensorMapping) {
        Co2SensorBusMapping.returnAirCo2.ordinal -> systemEquip.returnAirCo2.writeHisVal(sensorVal)
        Co2SensorBusMapping.mixedAirCo2.ordinal -> systemEquip.mixedAirCo2.writeHisVal(sensorVal)
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM update Co2SensorPoint : Invalid co2 sensor mapping $co2SensorMapping")
        }
    }
}

fun doThermistorConversion(thermistorVal : Double, inputMapping : ThermistorInput) : Double {
    return when(inputMapping.sensorScaling) {
        SensorScaling.DIRECT -> return thermistorVal
        SensorScaling.LOOKUP -> return ThermistorUtil.getThermistorValueToTemp(thermistorVal * 10)
        SensorScaling.BOOLEAN -> if ( (thermistorVal * 10) > inputMapping.threshold) 1.0 else 0.0
        SensorScaling.BOOLEAN_INVERSE -> if ((thermistorVal * 10) < inputMapping.threshold) 1.0 else 0.0
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM doThermistorConversion : Invalid sensor scaling ${inputMapping.sensorScaling}")
            0.0
        }
    }
}

fun doAnalogConversion(analogVal : Double, inputMapping : AnalogInput) : Double {
    return when(inputMapping.sensorScaling) {
        SensorScaling.DIRECT -> return analogVal
        SensorScaling.LINEAR -> return (inputMapping.minValue + analogVal *
                    (inputMapping.maxValue - inputMapping.minValue) / (inputMapping.maxVoltage - inputMapping.minVoltage))
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM doAnalogConversion : Invalid sensor scaling ${inputMapping.sensorScaling}")
            0.0
        }
    }
}

// Updating logical value for thermistor input
fun updateThermistorInput(physicalPointDomainName: String, thermistorVal: Double) {
    if (isNotAdvanceAhuProfile()) {
        CcuLog.e(L.TAG_CCU_SERIAL, "CM updateThermistorInput : Skipped AdvancedAHU not configured.")
        return
    }
    val systemEquip = getAdvancedAhuSystemEquip()
    val thermistorAssociation = when(physicalPointDomainName){
        DomainName.th1In -> systemEquip.thermistor1InputAssociation.readDefaultVal().toInt()
        DomainName.th2In -> systemEquip.thermistor2InputAssociation.readDefaultVal().toInt()
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateThermistorInput : Invalid physical point name $physicalPointDomainName")
            return
        }
    }
    val thermistorMapping = getAdvancedAhuThermistorMappings()[thermistorAssociation]
    CcuLog.i(L.TAG_CCU_SERIAL, "CM updateThermistorInput : association $thermistorAssociation mapping $thermistorMapping")
    thermistorMapping?.let {
        val convertedThermistorVal = doThermistorConversion(thermistorVal, thermistorMapping)
        CcuLog.i(L.TAG_CCU_SERIAL, "CM updateThermistorInput : convertedThermistorVal $convertedThermistorVal")
        getSensorDomainPointFromName(thermistorMapping.domainName, systemEquip)?.writeHisVal(convertedThermistorVal)
    } ?: CcuLog.e(L.TAG_CCU_SERIAL, "CM updateThermistorInput : Invalid thermistor association $thermistorAssociation")

}

// Updating logical value for Analog input
fun updateAnalogInput(physicalPointDomainName: String, thermistorVal: Double) {
    if (isNotAdvanceAhuProfile()) {
        CcuLog.e(L.TAG_CCU_SERIAL, "CM updateAnalogInput : Skipped AdvancedAHU not configured.")
        return
    }
    val systemEquip = getAdvancedAhuSystemEquip()
    val analogAssociation = when(physicalPointDomainName){
        DomainName.analog1In -> systemEquip.analog1InputAssociation.readDefaultVal().toInt()
        DomainName.analog2In -> systemEquip.analog2InputAssociation.readDefaultVal().toInt()
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateAnalogInput : Invalid physical point name $physicalPointDomainName")
            return
        }
    }
    val analogMapping = getAdvancedAhuAnalogInputMappings()[analogAssociation]
    CcuLog.i(L.TAG_CCU_SERIAL, "CM updateAnalogInput : analogAssociation $analogAssociation analog mapping $analogMapping")
    analogMapping?.let {
        val convertedAnalogInVal = doAnalogConversion(thermistorVal, analogMapping)
        CcuLog.i(L.TAG_CCU_SERIAL, "CM updateAnalogInput : convertedAnalogInVal $convertedAnalogInVal")
        getSensorDomainPointFromName(analogMapping.domainName, systemEquip)?.writeHisVal(convertedAnalogInVal)
    }?: CcuLog.e(L.TAG_CCU_SERIAL, "CM updateAnalogInput : Invalid analog association $analogAssociation")

}

fun isNotAdvanceAhuProfile(): Boolean {
    return (L.ccu().systemProfile !is VavAdvancedAhu && L.ccu().systemProfile !is DabAdvancedAhu)
}

fun updateRelayFeedBackStatus(status: Int) {
    if (isNotAdvanceAhuProfile()) {
        CcuLog.e(L.TAG_CCU_SERIAL, "CM updateAnalogInput : Skipped AdvancedAHU not configured.")
        return
    }
    val equip = getAdvancedAhuSystemEquip()
    val relayStatus = status.toString(radix = 2).padStart(8, '0').toCharArray()
    fun getRelayStatus(pos: Int) = relayStatus[pos].toString().toInt()
    CcuLog.e(L.TAG_CCU_DEVICE, "cm relay feedback status: $status = ReceivedStatus: ${Integer.toBinaryString(status)} ")

    updateRelayFeedback(getRelayStatus(7), equip, equip.relay1OutputAssociation,1)
    updateRelayFeedback(getRelayStatus(6), equip, equip.relay2OutputAssociation,2)
    updateRelayFeedback(getRelayStatus(5), equip, equip.relay3OutputAssociation,3)
    updateRelayFeedback(getRelayStatus(4), equip, equip.relay6OutputAssociation,6)
    updateRelayFeedback(getRelayStatus(3), equip, equip.relay4OutputAssociation,4)
    updateRelayFeedback(getRelayStatus(2), equip, equip.relay5OutputAssociation,5)
    updateRelayFeedback(getRelayStatus(1), equip, equip.relay7OutputAssociation,7)
    updateRelayFeedback(getRelayStatus(0), equip, equip.relay8OutputAssociation,8)
}

fun updateAnalogOutFeedback(analogFeedback: MutableList<Int>) {
    if (isNotAdvanceAhuProfile()) {
        CcuLog.e(L.TAG_CCU_SERIAL, "CM updateAnalogInput : Skipped AdvancedAHU not configured.")
        return
    }
    val equip = getAdvancedAhuSystemEquip()
    updateAnalogFeedback(analogFeedback[0], equip, equip.analog1OutputAssociation,0)
    updateAnalogFeedback(analogFeedback[1], equip, equip.analog2OutputAssociation,1)
    updateAnalogFeedback(analogFeedback[2], equip, equip.analog3OutputAssociation,2)
    updateAnalogFeedback(analogFeedback[3], equip, equip.analog4OutputAssociation,3)
}

fun updateAnalogFeedback(status: Int, equip: AdvancedHybridSystemEquip, associationPoint: Point, position: Int) {
    CcuLog.e(L.TAG_CCU_DEVICE, "updateAnalogFeedback : $position  status $status")
    when(L.ccu().systemProfile) {
        is VavAdvancedAhu -> (L.ccu().systemProfile as VavAdvancedAhu).setAnalogStatus(position, status.toDouble())
        is DabAdvancedAhu -> (L.ccu().systemProfile as DabAdvancedAhu).setAnalogStatus(position, status.toDouble())
    }

    if (!associationPoint.pointExists()) return
    val association = associationPoint.readDefaultVal().toInt()
    if (association > 3 ) {
        CcuLog.e(L.TAG_CCU_DEVICE, "updateAnalogFeedback : Not sat $association")
        return
    }
    when (association) {
        AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN.ordinal -> writeStatus(equip.pressureBasedFanControlFeedback, status)
        AdvancedAhuAnalogOutAssociationType.SAT_COOLING.ordinal -> writeStatus(equip.satBasedCoolingControlFeedback, status)
        AdvancedAhuAnalogOutAssociationType.SAT_HEATING.ordinal -> writeStatus(equip.satBasedHeatingControlFeedback, status)
    }
}

fun updateRelayFeedback(status: Int, equip: AdvancedHybridSystemEquip, associationPoint: Point, position: Int) {

    // To show test signal status we need to save it
    when(L.ccu().systemProfile) {
        is VavAdvancedAhu -> (L.ccu().systemProfile as VavAdvancedAhu).setCMRelayStatus(position, status)
        is DabAdvancedAhu -> (L.ccu().systemProfile as DabAdvancedAhu).setCMRelayStatus(position, status)
    }
    if (!associationPoint.pointExists()) return
    val association = associationPoint.readDefaultVal().toInt()

    if (association < 17 || association > 31) {
        CcuLog.e(L.TAG_CCU_DEVICE, "updateRelayFeedback : Not sat $association")
        return
    }

    CcuLog.e(L.TAG_CCU_DEVICE, "updateRelayFeedback : status $status assoc $association")
    when (AdvancedAhuRelayMappings.values()[association]) {
        AdvancedAhuRelayMappings.SAT_COOLING_STAGE_1 -> writeStatus(equip.satCoolingStage1Feedback, status)
        AdvancedAhuRelayMappings.SAT_COOLING_STAGE_2 -> writeStatus(equip.satCoolingStage2Feedback, status)
        AdvancedAhuRelayMappings.SAT_COOLING_STAGE_3 -> writeStatus(equip.satCoolingStage3Feedback, status)
        AdvancedAhuRelayMappings.SAT_COOLING_STAGE_4 -> writeStatus(equip.satCoolingStage4Feedback, status)
        AdvancedAhuRelayMappings.SAT_COOLING_STAGE_5 -> writeStatus(equip.satCoolingStage5Feedback, status)
        AdvancedAhuRelayMappings.SAT_HEATING_STAGE_1 -> writeStatus(equip.satHeatingStage1Feedback, status)
        AdvancedAhuRelayMappings.SAT_HEATING_STAGE_2 -> writeStatus(equip.satHeatingStage2Feedback, status)
        AdvancedAhuRelayMappings.SAT_HEATING_STAGE_3 -> writeStatus(equip.satHeatingStage3Feedback, status)
        AdvancedAhuRelayMappings.SAT_HEATING_STAGE_4 -> writeStatus(equip.satHeatingStage4Feedback, status)
        AdvancedAhuRelayMappings.SAT_HEATING_STAGE_5 -> writeStatus(equip.satHeatingStage5Feedback, status)
        AdvancedAhuRelayMappings.FAN_PRESSURE_STAGE_1 -> writeStatus(equip.fanPressureStage1Feedback, status)
        AdvancedAhuRelayMappings.FAN_PRESSURE_STAGE_2 -> writeStatus(equip.fanPressureStage2Feedback, status)
        AdvancedAhuRelayMappings.FAN_PRESSURE_STAGE_3 -> writeStatus(equip.fanPressureStage3Feedback, status)
        AdvancedAhuRelayMappings.FAN_PRESSURE_STAGE_4 -> writeStatus(equip.fanPressureStage4Feedback, status)
        AdvancedAhuRelayMappings.FAN_PRESSURE_STAGE_5 -> writeStatus(equip.fanPressureStage5Feedback, status)
        else -> { /* DO NOTHING **/ }
    }
}

fun updateLoopFeedback(loops: MutableList<Int>) {
    CcuLog.d(L.TAG_CCU_SERIAL, "CM handleCMRegularUpdate : piloopOutputList $loops")
    if (isNotAdvanceAhuProfile()) {
        CcuLog.d(L.TAG_CCU_SERIAL, "CM updateAnalogInput : Skipped AdvancedAHU not configured.")
        return
    }

    val equip = getAdvancedAhuSystemEquip()
    if (equip.coolingLoopOutputFeedback.pointExists()) writeStatus(equip.coolingLoopOutputFeedback, loops[0])
    if (equip.heatingLoopOutputFeedback.pointExists()) writeStatus(equip.heatingLoopOutputFeedback, loops[1])
    if (equip.fanLoopOutputFeedback.pointExists()) writeStatus(equip.fanLoopOutputFeedback, loops[2])
}

fun writeStatus(feedback: Point, status: Int) = feedback.writeHisVal(status.toDouble())
