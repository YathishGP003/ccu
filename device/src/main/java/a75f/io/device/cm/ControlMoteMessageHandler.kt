package a75f.io.device.cm

import a75f.io.device.ControlMote
import a75f.io.device.ControlMote.CM_SensorBusReadings_t
import a75f.io.device.mesh.ThermistorUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L

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
        }
        msg.analogOutStatusList.forEach {
            CcuLog.d(L.TAG_CCU_SERIAL, "CM handleCMRegularUpdate : analogOutStatus $it")
        }
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
    if (Domain.systemEquip !is VavAdvancedHybridSystemEquip) {
        CcuLog.e(L.TAG_CCU_SERIAL, "CM updateSensorBusData : Skipped AdvancedAHU not configured.")
        return
    }
    val systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip

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

fun updatePressureSensorPoint(pressureSensorMapping : Int, sensorVal : Double, systemEquip: VavAdvancedHybridSystemEquip) {
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

fun updateTemperatureSensorPoint(tempSensorMapping : Int, sensorVal: Double, systemEquip: VavAdvancedHybridSystemEquip) {
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

fun updateHumiditySensorPoint(humiditySensorMapping : Int, sensorVal: Double, systemEquip: VavAdvancedHybridSystemEquip) {
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

fun updateOccupancySensorPoint(occupancySensorMapping : Int, sensorVal: Double, systemEquip: VavAdvancedHybridSystemEquip) {
    when (occupancySensorMapping) {
        OccupancySensorBusMapping.occupancySensor1.ordinal -> systemEquip.occupancySensor1.writeHisVal(sensorVal)
        OccupancySensorBusMapping.occupancySensor2.ordinal -> systemEquip.occupancySensor2.writeHisVal(sensorVal)
        OccupancySensorBusMapping.occupancySensor3 .ordinal-> systemEquip.occupancySensor3.writeHisVal(sensorVal)
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateOccupancySensorPoint : Invalid occupancy sensor mapping $occupancySensorMapping")
        }
    }
}

fun updateCo2SensorPoint(co2SensorMapping : Int, sensorVal: Double, systemEquip: VavAdvancedHybridSystemEquip) {
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

fun updateThermistorInput(physicalPointName : String, thermistorVal : Double, equip: DomainEquip) {
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    val thermistorAssociation = when(physicalPointName){
        DomainName.th1In -> systemEquip.thermistor1InputAssociation.readDefaultVal().toInt()
        DomainName.th2In -> systemEquip.thermistor2InputAssociation.readDefaultVal().toInt()
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateThermistorInput : Invalid physical point name $physicalPointName")
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

fun updateAnalogInput(physicalPointName : String, thermistorVal : Double, equip: DomainEquip) {
    val systemEquip = when (equip) {
        is VavAdvancedHybridSystemEquip -> equip
        else -> throw IllegalArgumentException("Invalid system equip type")
    }
    val analogAssociation = when(physicalPointName){
        DomainName.analog1In -> systemEquip.analog1InputAssociation.readDefaultVal().toInt()
        DomainName.analog2In -> systemEquip.analog2InputAssociation.readDefaultVal().toInt()
        else -> {
            CcuLog.e(L.TAG_CCU_SERIAL, "CM updateAnalogInput : Invalid physical point name $physicalPointName")
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