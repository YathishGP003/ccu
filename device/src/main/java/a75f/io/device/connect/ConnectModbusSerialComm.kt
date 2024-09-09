package a75f.io.device.connect

import a75f.io.device.mesh.LSerial
import a75f.io.device.serial.MessageType
import a75f.io.domain.devices.ConnectDevice
import a75f.io.logger.CcuLog
import a75f.io.logic.BuildConfig
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.util.getConnectEquip
import com.x75f.modbus4j.msg.ModbusResponse
import com.x75f.modbus4j.msg.ReadInputRegistersRequest
import com.x75f.modbus4j.msg.WriteRegistersRequest
import com.x75f.modbus4j.serial.rtu.RtuMessageRequest
import com.x75f.modbus4j.serial.rtu.RtuMessageResponse
import com.x75f.modbus4j.sero.util.queue.ByteQueue

/**
 * All the read/write operations in this class are over the same serial port.
 * So the writing thread should be blocked until we get response.
 * Ideally all the operations in this interface should sequentially happen on a single thread.
 */
object ConnectModbusSerialComm {
    private const val SERIAL_COMM_TIMEOUT = 1000L
    private val connectModbusCommLock = CommLock("ConnectModbusSerialComm")

    private fun writeUniversalInputMappingConfig(byteArray: ByteArray) {
        val request = WriteRegistersRequest(ADVANCED_AHU_CONNECT1_SLAVE_ADDR, UNIN_MAPPING_CONFIG_START_ADDR, byteArray)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.WRITE_UNIVERSAL_INPUT_MAPPING_CONFIG)
    }

    private fun writeAnalogOutMappingConfig(byteArray: ByteArray) {
        val request = WriteRegistersRequest(ADVANCED_AHU_CONNECT1_SLAVE_ADDR, AOUT_MAPPING_CONFIG_START_ADDR, byteArray)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.WRITE_ANALOG_OUT_MAPPING_CONFIG)
    }

    private fun writeRelayMappingConfig(byteArray: ByteArray) {
        val request = WriteRegistersRequest(ADVANCED_AHU_CONNECT1_SLAVE_ADDR, RELAY_MAPPING_CONFIG_START_ADDR, byteArray)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.WRITE_RELAY_MAPPING_CONFIG)
    }

    // Read 8 Float values over 16 registers
    // Register type : Input Register (0x03)
    private fun readUniversalInputMappedValues() {
        val request = ReadInputRegistersRequest(ADVANCED_AHU_CONNECT1_SLAVE_ADDR, UNIN_MAPPED_VAL_START_ADDR, UNIN_MAPPED_VAL_REG_COUNT)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.READ_UNIVERSAL_INPUT_MAPPED_VALUES)
    }

    private fun readSensorBusValues() {
        val request = ReadInputRegistersRequest(ADVANCED_AHU_CONNECT1_SLAVE_ADDR, SENSOR_BUS_START_ADDR, SENSOR_BUS_REG_COUNT)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.READ_SENSOR_BUS_VALUES)
    }

    // Read 8 Float values over 8 registers
    // Register type : Holding Register (0x04)
    private fun writeRelayOutputMappedValues(byteArray: ByteArray) {
        val request = WriteRegistersRequest(ADVANCED_AHU_CONNECT1_SLAVE_ADDR, RELAY_MAPPED_VAL_START_ADDR, byteArray)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.WRITE_RELAY_OUTPUT_MAPPED_VALUES)
    }

    // Read 5 Float values over 8 registers
    // Register type : Input Register (0x03)
    private fun writeAnalogOutputMappedValues(byteArray: ByteArray) {
        val request = WriteRegistersRequest(ADVANCED_AHU_CONNECT1_SLAVE_ADDR, AOUT_MAPPED_VAL_START_ADDR, byteArray)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.WRITE_ANALOG_OUTPUT_MAPPED_VALUES)
    }

    private fun sendRequestAndLockComm(request: RtuMessageRequest, ops: ConnectModbusOps) {
        CcuLog.d(L.TAG_CCU_SERIAL_CONNECT, "sendRequestAndLockComm $ops "+request.messageData.toHexString())
        LSerial.getInstance().sendSerialBytesToConnect(request.messageData)

        if (L.isSimulation()
            && (BuildConfig.BUILD_TYPE.equals("dev", true)
                    || BuildConfig.BUILD_TYPE.equals("local", true)
                    || BuildConfig.BUILD_TYPE.equals("dev_qa", true)
                    || BuildConfig.BUILD_TYPE.equals("qa", true))) {

            val modbusMessage = getModbusConnectMessage(request.messageData)
            LSerial.getInstance().sendSerialBytesToCM(modbusMessage)
        }
        lockComm(ops)
    }


    /**
     * Lock the communication channel to prevent other transactions.
     */
    private fun lockComm(ops: ConnectModbusOps) {
        connectModbusCommLock.lock(SERIAL_COMM_TIMEOUT, ops.ordinal)
    }

    private fun unlockComm() {
        connectModbusCommLock.unlock()
    }

    @JvmStatic
    fun testReadOp() {
        val request = ReadInputRegistersRequest(98, 60000, 1)
        sendRequestAndLockComm(RtuMessageRequest(request), ConnectModbusOps.TEST_OPERATION)
    }

    @JvmStatic
    fun sendSettingConfig() {
        val connectEquip1 = getConnectEquip()
        writeUniversalInputMappingConfig(getUniversalInputMappingConfig(connectEquip1))
        writeRelayMappingConfig(getRelayMappingConfig(connectEquip1))
        writeAnalogOutMappingConfig(getAnalogOutMappingConfig(connectEquip1))
    }

    @JvmStatic
    fun sendControlsMessage(device : ConnectDevice) {
        writeRelayOutputMappedValues(getRelayOutputMappedValues(device))
        writeAnalogOutputMappedValues(getAnalogOutputMappedValues(device))
    }

    @JvmStatic
    fun getRegularUpdate() {
        readSensorBusValues()
        readUniversalInputMappedValues()
    }

    @JvmStatic
    fun handleModbusResponse(data : ByteArray) {
        try {
            val queue = ByteQueue(data)
            val response = ModbusResponse.createModbusResponse(queue)
            val rtuResponse = RtuMessageResponse(response)
            CcuLog.d(L.TAG_CCU_SERIAL_CONNECT, "handleModbusResponse : "+rtuResponse.messageData.toHexString())
            // Check the CRC
            //ModbusUtils.checkCRC(rtuResponse.getModbusMessage(), queue);
            val currentModbusOps = connectModbusCommLock.getOperationCode()
            unlockComm()
            if (!rtuResponse.modbusResponse.isException) {
                CcuLog.d(L.TAG_CCU_SERIAL_CONNECT, "Response success==" + rtuResponse.modbusMessage.toString())
                handleModbusResponse(rtuResponse.modbusMessage.slaveId, rtuResponse, currentModbusOps.toEnum<ConnectModbusOps>()!!)
            } else {
                CcuLog.e(
                    L.TAG_CCU_SERIAL_CONNECT, "handlingResponse, exception-" + rtuResponse.modbusResponse.exceptionMessage
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ByteArray.toHexString() = joinToString(" ") { "%02x".format(it) }

    private fun getModbusConnectMessage(requestBytes: ByteArray): ByteArray {
        val msgBytes = ByteArray(requestBytes.size + 1)
        msgBytes[0] = MessageType.MODBUS_MESSAGE.ordinal.toByte()
        System.arraycopy(requestBytes, 0, msgBytes, 1, requestBytes.size)
        return msgBytes
    }
}
