package a75f.io.device.connect

import a75f.io.device.cm.Co2SensorBusMapping
import a75f.io.device.cm.HumiditySensorBusMapping
import a75f.io.device.cm.OccupancySensorBusMapping
import a75f.io.device.cm.PressureSensorBusMapping
import a75f.io.device.cm.TemperatureSensorBusMapping
import a75f.io.device.cm.getAdvancedAhuSensorInputMappings
import a75f.io.domain.api.Domain
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import com.x75f.modbus4j.serial.rtu.RtuMessageResponse
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

const val MODBUS_DATA_OFFSET = 3
fun handleModbusResponse(slaveId : Int, response : RtuMessageResponse, ops: ConnectModbusOps) {
    when (ops) {
        ConnectModbusOps.READ_SENSOR_BUS_VALUES -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "READ_SENSOR_BUS_VALUES response received $response")
            updateSensorBusValues(slaveId, response)
        }
        ConnectModbusOps.READ_UNIVERSAL_INPUT_MAPPED_VALUES -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "READ_UNIVERSAL_INPUT_MAPPED_VALUES response received $response")
            updateUniversalInputMappedValues(slaveId, response)
        }
        ConnectModbusOps.WRITE_RELAY_OUTPUT_MAPPED_VALUES -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "WRITE_RELAY_OUTPUT_MAPPED_VALUES completed ")
        }
        ConnectModbusOps.WRITE_ANALOG_OUTPUT_MAPPED_VALUES -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "WRITE_ANALOG_OUTPUT_MAPPED_VALUES completed ")
        }
        ConnectModbusOps.WRITE_UNIVERSAL_INPUT_MAPPING_CONFIG -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "WRITE_UNIVERSAL_INPUT_MAPPING_CONFIG completed")
        }
        ConnectModbusOps.WRITE_ANALOG_OUT_MAPPING_CONFIG -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "WRITE_ANALOG_OUT_MAPPING_CONFIG completed")
        }
        ConnectModbusOps.WRITE_RELAY_MAPPING_CONFIG -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "WRITE_RELAY_MAPPING_CONFIG completed")
        }
        ConnectModbusOps.TEST_OPERATION -> {
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "TEST_OPERATION response received $response")
        }

    }
    //TODO- Successful read operation ->Update heartbeat
}

fun updateUniversalInputMappedValues(slaveId : Int, response : RtuMessageResponse) {
    val messageData = ByteBuffer.wrap(response.messageData.copyOfRange(MODBUS_DATA_OFFSET, response.messageData.size)).order(ByteOrder.BIG_ENDIAN)
    val universalInput1Val = messageData.float
    val universalInput2Val = messageData.float
    val universalInput3Val = messageData.float
    val universalInput4Val = messageData.float
    val universalInput5Val = messageData.float
    val universalInput6Val = messageData.float
    val universalInput7Val = messageData.float
    val universalInput8Val = messageData.float

    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "universalInput1Val $universalInput1Val universalInput2Val " +
            "$universalInput2Val universalInput3Val $universalInput3Val universalInput4Val " +
            "$universalInput4Val universalInput5Val $universalInput5Val universalInput6Val " +
            "$universalInput6Val universalInput7Val $universalInput7Val universalInput8Val $universalInput8Val")

    if (Domain.systemEquip !is VavAdvancedHybridSystemEquip) {
        CcuLog.e(L.TAG_CCU_SERIAL, "CM updateSensorBusData : Skipped AdvancedAHU not configured.")
        return
    }
    val systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
    val connectEquip = systemEquip.connectEquip1
    writeUniversalInputMappedVal(connectEquip.universalIn1Association.readDefaultVal().toInt(), universalInput1Val, connectEquip, Domain.connect1Device.universal1In)
    writeUniversalInputMappedVal(connectEquip.universalIn2Association.readDefaultVal().toInt(), universalInput2Val, connectEquip, Domain.connect1Device.universal2In)
    writeUniversalInputMappedVal(connectEquip.universalIn3Association.readDefaultVal().toInt(), universalInput3Val, connectEquip, Domain.connect1Device.universal3In)
    writeUniversalInputMappedVal(connectEquip.universalIn4Association.readDefaultVal().toInt(), universalInput4Val, connectEquip, Domain.connect1Device.universal4In)
    writeUniversalInputMappedVal(connectEquip.universalIn5Association.readDefaultVal().toInt(), universalInput5Val, connectEquip, Domain.connect1Device.universal5In)
    writeUniversalInputMappedVal(connectEquip.universalIn6Association.readDefaultVal().toInt(), universalInput6Val, connectEquip, Domain.connect1Device.universal6In)
    writeUniversalInputMappedVal(connectEquip.universalIn7Association.readDefaultVal().toInt(), universalInput7Val, connectEquip, Domain.connect1Device.universal7In)
    writeUniversalInputMappedVal(connectEquip.universalIn8Association.readDefaultVal().toInt(), universalInput8Val, connectEquip, Domain.connect1Device.universal8In)
}

private fun writeUniversalInputMappedVal(
        associationVal: Int,
        sensorVal: Float,
        connectEquip: ConnectModuleEquip,
        physicalPoint: PhysicalPoint
) {
    val universalInputMapping = getAdvancedAhuSensorInputMappings()[associationVal]
    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "universalInputVal $sensorVal associationVal $associationVal")
    if (universalInputMapping != null) {
        physicalPoint.writeHisVal(sensorVal.toDouble())
        Domain.writeHisValByDomain(universalInputMapping.domainName, sensorVal.toDouble(), connectEquip.equipRef)
    } else {
        CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "universalInputMapping not found for associationVal $associationVal")
    }

}

fun updateRelayOutputMappedValues(slaveId : Int, response : RtuMessageResponse) {
    val messageData = response.messageData
    val relay1Val = parseIntFromTwoBytes(messageData.copyOfRange(0, 2))
    val relay2Val = parseIntFromTwoBytes(messageData.copyOfRange(2, 4))
    val relay3Val = parseIntFromTwoBytes(messageData.copyOfRange(4, 6))
    val relay4Val = parseIntFromTwoBytes(messageData.copyOfRange(6, 8))
    val relay5Val = parseIntFromTwoBytes(messageData.copyOfRange(8, 10))
    val relay6Val = parseIntFromTwoBytes(messageData.copyOfRange(10, 12))
    val relay7Val = parseIntFromTwoBytes(messageData.copyOfRange(12, 14))
    val relay8Val = parseIntFromTwoBytes(messageData.copyOfRange(14, 16))

    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "relay1Val $relay1Val relay2Val $relay2Val relay3Val " +
            "$relay3Val relay4Val $relay4Val relay5Val $relay5Val relay6Val " +
            "$relay6Val relay7Val $relay7Val relay8Val $relay8Val")

    //TODO- Write to mapped points
}

fun updateAnalogOutputMappedValues(slaveId : Int, response : RtuMessageResponse) {
    val messageData = response.messageData
    val analogOut1Val = parseFloatFromBytes(messageData.copyOfRange(0, 4))
    val analogOut2Val = parseFloatFromBytes(messageData.copyOfRange(4, 8))
    val analogOut3Val = parseFloatFromBytes(messageData.copyOfRange(8, 12))
    val analogOut4Val = parseFloatFromBytes(messageData.copyOfRange(12, 16))

    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "analogOut1Val $analogOut1Val analogOut2Val " +
            "$analogOut2Val analogOut3Val $analogOut3Val analogOut4Val $analogOut4Val")

    //TODO- Write to mapped points
}

fun updateSensorBusValues(slaveId: Int, response: RtuMessageResponse) {

    if (Domain.systemEquip !is VavAdvancedHybridSystemEquip) {
        CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "CM updateSensorBusData : Skipped AdvancedAHU not configured.")
        return
    }
    val systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
    val connectEquip = systemEquip.connectEquip1
    val messageData = response.messageData.copyOfRange(MODBUS_DATA_OFFSET, response.messageData.size)
    if (connectEquip.sensorBusAddress0Enable.readDefaultVal() > 0) {
        CcuLog.i(L.TAG_CCU_SERIAL, "CM updateSensorBusData sensorBusAddress0Enabled")
        if (connectEquip.temperatureSensorBusAdd0.pointExists()) {
            updateConnectTemperatureSensor(slaveId, messageData, connectEquip.temperatureSensorBusAdd0.readDefaultVal().toInt(),
                SENSOR_BUS_TEMPERATURE_1, connectEquip)
        }
        if (connectEquip.humiditySensorBusAdd0.pointExists()) {
            updateConnectHumiditySensor(slaveId, messageData, connectEquip.humiditySensorBusAdd0.readDefaultVal().toInt(),
                SENSOR_BUS_HUMIDITY_1, connectEquip)
        }
        if (connectEquip.co2SensorBusAdd0.pointExists()) {
            updateConnectCo2Sensor(slaveId, messageData, connectEquip.co2SensorBusAdd0.readDefaultVal().toInt(),
                SENSOR_BUS_CO2_1, connectEquip)
        }
        if (connectEquip.occupancySensorBusAdd0.pointExists()) {
            updateConnectOccupancySensor(slaveId, messageData, connectEquip.occupancySensorBusAdd0.readDefaultVal().toInt(),
                SENSOR_BUS_OCCUPANCY_1, connectEquip)
        }
    }
    if (connectEquip.sensorBus0PressureAssociation.pointExists()) {
        updateConnectPressureSensor(slaveId, messageData, connectEquip.sensorBus0PressureAssociation.readDefaultVal().toInt(),
            SENSOR_BUS_PRESSURE_1, connectEquip)
    }

    if (connectEquip.sensorBusAddress1Enable.readDefaultVal() > 0) {
        CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "CM updateSensorBusData sensorBusAddress1Enabled")
        if (connectEquip.temperatureSensorBusAdd1.pointExists()) {
            updateConnectTemperatureSensor(slaveId, messageData, connectEquip.temperatureSensorBusAdd1.readDefaultVal().toInt(),
                SENSOR_BUS_TEMPERATURE_2, connectEquip)
        }
        if (connectEquip.humiditySensorBusAdd1.pointExists()) {
            updateConnectHumiditySensor(slaveId, messageData, connectEquip.humiditySensorBusAdd1.readDefaultVal().toInt(),
                SENSOR_BUS_HUMIDITY_2, connectEquip)
        }
        if (connectEquip.co2SensorBusAdd1.pointExists()) {
            updateConnectCo2Sensor(slaveId, messageData, connectEquip.co2SensorBusAdd1.readDefaultVal().toInt(),
                SENSOR_BUS_CO2_2, connectEquip)
        }
        if (connectEquip.occupancySensorBusAdd1.pointExists()) {
            updateConnectOccupancySensor(slaveId, messageData, connectEquip.occupancySensorBusAdd1.readDefaultVal().toInt(),
                SENSOR_BUS_OCCUPANCY_2, connectEquip)
        }
    }

    if (connectEquip.sensorBusAddress2Enable.readDefaultVal() > 0) {
        CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "CM updateSensorBusData sensorBusAddress2Enabled")
        if (connectEquip.temperatureSensorBusAdd2.pointExists()) {
            updateConnectTemperatureSensor(slaveId, messageData, connectEquip.temperatureSensorBusAdd2.readDefaultVal().toInt(),
                SENSOR_BUS_TEMPERATURE_3, connectEquip)
        }
        if (connectEquip.humiditySensorBusAdd2.pointExists()) {
            updateConnectHumiditySensor(slaveId, messageData, connectEquip.humiditySensorBusAdd2.readDefaultVal().toInt(),
                SENSOR_BUS_HUMIDITY_3, connectEquip)
        }
        if (connectEquip.co2SensorBusAdd2.pointExists()) {
            updateConnectCo2Sensor(slaveId, messageData, connectEquip.co2SensorBusAdd2.readDefaultVal().toInt(),
                SENSOR_BUS_CO2_3, connectEquip)
        }
        if (connectEquip.occupancySensorBusAdd2.pointExists()) {
            updateConnectOccupancySensor(slaveId, messageData, connectEquip.occupancySensorBusAdd2.readDefaultVal().toInt(),
                SENSOR_BUS_OCCUPANCY_3, connectEquip)
        }
    }

    if (connectEquip.sensorBusAddress3Enable.readDefaultVal() > 0) {
        CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "CM updateSensorBusData sensorBusAddress3Enabled")
        if (connectEquip.temperatureSensorBusAdd3.pointExists()) {
            updateConnectTemperatureSensor(slaveId, messageData, connectEquip.temperatureSensorBusAdd3.readDefaultVal().toInt(),
                SENSOR_BUS_TEMPERATURE_4, connectEquip)
        }
        if (connectEquip.humiditySensorBusAdd3.pointExists()) {
            updateConnectHumiditySensor(slaveId, messageData, connectEquip.humiditySensorBusAdd3.readDefaultVal().toInt(),
                SENSOR_BUS_HUMIDITY_4, connectEquip)
        }
        if (connectEquip.co2SensorBusAdd3.pointExists()) {
            updateConnectCo2Sensor(slaveId, messageData, connectEquip.co2SensorBusAdd3.readDefaultVal().toInt(),
                SENSOR_BUS_CO2_4, connectEquip)
        }
        if (connectEquip.occupancySensorBusAdd3.pointExists()) {
            updateConnectOccupancySensor(slaveId, messageData, connectEquip.occupancySensorBusAdd3.readDefaultVal().toInt(),
                SENSOR_BUS_OCCUPANCY_4, connectEquip)
        }
    }
    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "CM updateSensorBusData Updated")
}

fun updateConnectTemperatureSensor(slaveId: Int, responseArray : ByteArray, sensorMapping : Int, sensorBusAddr : Int ,connectEquip : ConnectModuleEquip) {
    val offset = (sensorBusAddr - SENSOR_BUS_START_ADDR) * 2
    val sensorVal = parseIntFromTwoBytes(responseArray.copyOfRange(offset, offset + 2))/10
    when(sensorMapping) {
        TemperatureSensorBusMapping.notConnected.ordinal -> {CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "Temp sensor not connected")}
        TemperatureSensorBusMapping.returnAirTempature.ordinal -> {
            connectEquip.returnAirTemperature.writeHisVal(sensorVal.toDouble())
        }
        TemperatureSensorBusMapping.mixedAirTemperature.ordinal -> {
            connectEquip.mixedAirTemperature.writeHisVal(sensorVal.toDouble())
        }
        TemperatureSensorBusMapping.supplyAirTemperature1.ordinal -> {
            connectEquip.supplyAirTemperature1.writeHisVal(sensorVal.toDouble())
        }
        TemperatureSensorBusMapping.supplyAirTemperature2.ordinal -> {
            connectEquip.supplyAirTemperature2.writeHisVal(sensorVal.toDouble())
        }
        TemperatureSensorBusMapping.supplyAirTemperature3.ordinal -> {
            connectEquip.supplyAirTemperature3.writeHisVal(sensorVal.toDouble())
        }
    }
    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "Connect temperatureVal $sensorVal sensorMapping $sensorMapping sensorBusAddr $sensorBusAddr")
    //TODO- Write to mapped points
}

fun updateConnectHumiditySensor(slaveId: Int, responseArray : ByteArray, sensorMapping : Int, sensorBusAddr : Int ,connectEquip : ConnectModuleEquip) {
    val offset = (sensorBusAddr - SENSOR_BUS_START_ADDR) * 2
    val sensorVal = parseIntFromTwoBytes(responseArray.copyOfRange(offset, offset + 2))/10
    when(sensorMapping) {
        HumiditySensorBusMapping.notConnected.ordinal -> {CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "Humidity sensor not connected")}
        HumiditySensorBusMapping.returnAirHumidity.ordinal -> {
            connectEquip.returnAirHumidity.writeHisVal(sensorVal.toDouble())
        }
        HumiditySensorBusMapping.mixedAirHumidity.ordinal -> {
            connectEquip.mixedAirHumidity.writeHisVal(sensorVal.toDouble())
        }
        HumiditySensorBusMapping.supplyAirHumidity1.ordinal -> {
            connectEquip.supplyAirHumidity1.writeHisVal(sensorVal.toDouble())
        }
        HumiditySensorBusMapping.supplyAirHumidity2.ordinal -> {
            connectEquip.supplyAirHumidity2.writeHisVal(sensorVal.toDouble())
        }
        HumiditySensorBusMapping.supplyAirHumidity3.ordinal -> {
            connectEquip.supplyAirHumidity3.writeHisVal(sensorVal.toDouble())
        }
    }
    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "Connect humidityVal $sensorVal sensorMapping $sensorMapping sensorBusAddr $sensorBusAddr")
    //TODO- Write to mapped points
}

fun updateConnectPressureSensor(slaveId: Int, responseArray : ByteArray, sensorMapping : Int, sensorBusAddr : Int ,connectEquip : ConnectModuleEquip) {
    val offset = (sensorBusAddr - SENSOR_BUS_START_ADDR) * 2
    val sensorVal = parse12BitPressureValue(responseArray.copyOfRange(offset, offset + 2))
    val scaledSensorVal = sensorVal * 0.0040146 //Convert from Pa to inch of wc
    when(sensorMapping) {
        PressureSensorBusMapping.notConnected.ordinal -> {CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "Pressure sensor not connected")}
        PressureSensorBusMapping.ductStaticPressure12.ordinal -> {
            connectEquip.ductStaticPressureSensor12.writeHisVal(scaledSensorVal)
        }
        PressureSensorBusMapping.ductStaticPressure22.ordinal -> {
            connectEquip.ductStaticPressureSensor22.writeHisVal(scaledSensorVal)
        }
        PressureSensorBusMapping.ductStaticPressure32.ordinal -> {
            connectEquip.ductStaticPressureSensor32.writeHisVal(scaledSensorVal)
        }
    }
    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "Connect pressureVal $sensorVal sensorMapping $sensorMapping sensorBusAddr $sensorBusAddr")
    //TODO- Write to mapped points
}

fun updateConnectOccupancySensor(slaveId: Int, responseArray : ByteArray, sensorMapping : Int, sensorBusAddr : Int ,connectEquip : ConnectModuleEquip) {
    val offset = (sensorBusAddr - SENSOR_BUS_START_ADDR) * 2
    val sensorVal = parseIntFromTwoBytes(responseArray.copyOfRange(offset, offset + 2))
    when(sensorMapping) {
        OccupancySensorBusMapping.notConnected.ordinal -> {CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "Occupancy sensor not connected")}
        OccupancySensorBusMapping.occupancySensor1.ordinal -> {
            connectEquip.occupancySensor1.writeHisVal(sensorVal.toDouble())
        }
        OccupancySensorBusMapping.occupancySensor2.ordinal -> {
            connectEquip.occupancySensor2.writeHisVal(sensorVal.toDouble())
        }
        OccupancySensorBusMapping.occupancySensor3.ordinal -> {
            connectEquip.occupancySensor3.writeHisVal(sensorVal.toDouble())
        }
    }
    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "Connect occupancyVal $sensorVal sensorMapping $sensorMapping sensorBusAddr $sensorBusAddr")
    //TODO- Write to mapped points
}

fun updateConnectCo2Sensor(slaveId: Int, responseArray : ByteArray, sensorMapping : Int, sensorBusAddr : Int ,connectEquip : ConnectModuleEquip) {
    val offset = (sensorBusAddr - SENSOR_BUS_START_ADDR) * 2
    val sensorVal = parseIntFromTwoBytes(responseArray.copyOfRange(offset, offset + 2))
    when(sensorMapping) {
        Co2SensorBusMapping.notConnected.ordinal -> {CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "Co2 sensor not connected")}
        Co2SensorBusMapping.returnAirCo2.ordinal -> {
            connectEquip.returnAirCo2.writeHisVal(sensorVal.toDouble())
        }
        Co2SensorBusMapping.mixedAirCo2.ordinal -> {
            connectEquip.mixedAirCo2.writeHisVal(sensorVal.toDouble())
        }
    }
    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "Connect co2Val $sensorVal sensorMapping $sensorMapping sensorBusAddr $sensorBusAddr")
    //TODO- Write to mapped points
}
fun parseIntFromTwoBytes(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).short

fun parse12BitPressureValue(bytes: ByteArray) : Int {
    val pVal = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).short.toInt()
    val isNegative = (pVal and 0x0800) > 0
    return if (isNegative) -1 * (pVal and 0x07FF) else pVal
}
fun parseFloatFromBytes(bytes: ByteArray) : Double{
    var responseVal = 0
    for (i in 0..3) {
        responseVal = responseVal shl java.lang.Long.BYTES
        responseVal = responseVal or bytes[i].toInt()
    }

    var formattedVal = java.lang.Float.intBitsToFloat(responseVal).toDouble()

    formattedVal = if (java.lang.Double.isNaN(formattedVal)) {
        0.0
    } else {
        (formattedVal * 100.0).roundToInt() / 100.0
    }
    return formattedVal
}