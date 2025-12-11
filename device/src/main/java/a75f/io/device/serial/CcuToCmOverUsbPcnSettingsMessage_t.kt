package a75f.io.device.serial

import a75f.io.device.serial.MessageType.CCU_TO_CM_MODBUS_SERVER_REGULAR_UPDATE_REGISTER_TYPE_SETTINGS
import a75f.io.device.serial.MessageType.CCU_TO_CM_MODBUS_SERVER_REGULAR_UPDATE_SETTINGS
import a75f.io.device.serial.MessageType.CCU_TO_CM_MODBUS_SERVER_WRITE_REGISTER_SETTINGS
import a75f.io.device.serial.MessageType.CCU_TO_CM_MODBUS_SERVER_WRITE_REGISTER_TYPE_SETTINGS
import a75f.io.device.serial.MessageType.CCU_TO_CM_OVER_USB_MODBUS_SERVER_WRITE_REGISTER
import a75f.io.device.util.calculateModbusCrc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

enum class PCNDeviceTypes {
    PCN_DEVICE_NONE,
    PCN_NATIVE_DEVICE,
    PCN_CONNECT_MODULE,
    PCN_EXTERNAL_EQUIP,
}

/** Plain Kotlin model for a slave config (dynamic-length registers). */
data class SlaveConfig(
    var slaveId: Int = 0,
    var numRegs: Int = 0
) {
    var registers: MutableList<UShort> = MutableList(numRegs) { 0u }
    var pointId: MutableList<String> = MutableList(numRegs) { "" }
    var registerType: MutableList<UShort> = MutableList(numRegs) { 0u }
    lateinit var deviceType: PCNDeviceTypes

    init {
        require(numRegs >= 0) { "numRegs cannot be negative" }
    }

    fun resizeRegisters(newSize: Int) {
        require(newSize >= 0) { "newSize cannot be negative" }
        // preserve existing values up to min(old, new), fill new entries with 0u
        val old = registers
        registers = MutableList(newSize) { index ->
            if (index < old.size) old[index] else 0u
        }
        numRegs = newSize
    }

    fun setRegisterValue(index: Int, value: Int) {
        require(index in 0 until numRegs) { "Index $index out of bounds (size=$numRegs)" }
        registers[index] = value.toUShort()
    }

    fun setRegisterType(index: Int, value: Int) {
        require(index in 0 until numRegs) { "Index $index out of bounds (size=$numRegs)" }
        registerType[index] = value.toUShort()
    }

    fun getRegisterValue(index: Int): Int {
        require(index in 0 until numRegs) { "Index $index out of bounds (size=$numRegs)" }
        return registers[index].toInt()
    }

    fun sortSlaveConfigsByRegisters() {
        val combined = pointId.zip(registers)
            .sortedBy { it.second } // Sort by register value
        pointId = combined.map { it.first }.toMutableList()
        registers = combined.map { it.second }.toMutableList()
    }

    override fun toString(): String =
        "SlaveConfig(slaveId=$slaveId, numRegs=$numRegs, registers=$registers)"
}

/**
 * Message with dynamic slave configs.
 * Note: This class **does not** attempt to create Struct fields dynamically.
 * Use toByteArray() when you need raw Little-Endian bytes for transport.
 */
class CcuToCmOverUsbPcnSettingsMessage_t {
    var messageType:Int = 0
    var nodeAddress: Int = 0
    var intervalInSecs: Int = 0
    var crcLo: Byte = 0
    var crcHi: Byte = 0
    val slaveConfigs = mutableListOf<SlaveConfig>()

    fun addSlaveConfig(slaveId: Int, numRegs: Int): SlaveConfig {
        val cfg = SlaveConfig(slaveId = slaveId, numRegs = max(0, numRegs)).apply {
            // ensure registers list is exactly numRegs long
            registers = MutableList(this.numRegs) { 0u }
        }
        slaveConfigs.add(cfg)
        return cfg
    }

    fun getSlaveConfig(index: Int): SlaveConfig {
        require(index in slaveConfigs.indices) { "Invalid index $index" }
        return slaveConfigs[index]
    }

    fun getAllConfigs(): List<SlaveConfig> = slaveConfigs.toList()

    /**
     * Collect all register info from slaveConfigs.
     * Returns: List of Triple<deviceType, equipId, registers>
     */
    fun getAllRegInfo(): List<Triple<PCNDeviceTypes, String, UShort>> {
        return slaveConfigs.flatMap { cfg ->
            cfg.registers.mapIndexed {index, reg ->
                Triple(cfg.deviceType, cfg.pointId[index], reg)
            }
        }
    }

    fun getCrc(): Int {
        return ((crcHi.toInt() and 0xFF) shl 8) or (crcLo.toInt() and 0xFF)
    }

    fun clearConfigs() {
        slaveConfigs.clear()
    }

    /**
     * Serialize the message to bytes (Little Endian):
     * Format:
     * [ intervalInSecs (1) ]
     * for each slave:
     *   [ slaveId (1) ][ numRegs (1) ][ registers... (numRegs * 2) ]
     */
    fun toByteArray(): ByteArray {
        // compute total size: 1 (interval) + slaves
        val totalSize = 1 + 2 + 1 + slaveConfigs.sumOf { 1 + 1 + it.numRegs * 2 }
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        // messageType -> 1 byte
        buf.put((messageType and 0xFF).toByte())

        // 2 byte node address
        buf.putShort((nodeAddress and 0xFFFF).toShort()) // nodeAddress -> 2 bytes

        // intervalInSecs -> 1 byte
        buf.put((intervalInSecs and 0xFF).toByte())

        // each slave
        for (cfg in slaveConfigs) {
            buf.put((cfg.slaveId and 0xFF).toByte())     // 1 byte
            buf.put((cfg.numRegs and 0xFF).toByte())     // 1 byte
            for (reg in cfg.registers) {
                buf.putShort((reg.toInt() and 0xFFFF).toShort()) // 2 bytes each
            }
        }
        val slicedBuf = buf.duplicate().apply {
            position(3) // Skip messageType byte and node address
        }
        calculateModbusCrc(slicedBuf).let { crc ->
            crcHi = (((crc ushr 8) and 0xFF).toByte()) // CRC high byte
            crcLo = ((crc and 0xFF).toByte())         // CRC low byte
        }
        return buf.array()
    }

    fun regTypeByteArray(): ByteArray {
        val totalSize = 1 + 2 + slaveConfigs.sumOf { it.numRegs } + 2 // 1 byte messageType + 2 byte node address + reg types + 2 bytes CRC
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        // messageType -> 1 byte
        if(messageType == CCU_TO_CM_MODBUS_SERVER_REGULAR_UPDATE_SETTINGS.ordinal){
            buf.put((CCU_TO_CM_MODBUS_SERVER_REGULAR_UPDATE_REGISTER_TYPE_SETTINGS.ordinal and 0xFF).toByte())
        } else if(messageType == CCU_TO_CM_MODBUS_SERVER_WRITE_REGISTER_SETTINGS.ordinal){
            buf.put((CCU_TO_CM_MODBUS_SERVER_WRITE_REGISTER_TYPE_SETTINGS.ordinal and 0xFF).toByte())
        } else {
            buf.put(0.toByte())
        }

        // 2 byte node address
        buf.putShort((nodeAddress and 0xFFFF).toShort()) // nodeAddress -> 2 bytes

        for (cfg in slaveConfigs) {
            for (regType in cfg.registerType) {
                buf.put((regType.toInt() and 0xFF).toByte()) // 1 byte each
            }
        }
        buf.put(crcHi)          // CRC high byte
        buf.put(crcLo)         // CRC low byte

        return buf.array()
    }

    override fun toString(): String =
        "CcuToCmOverUsbPcnSettingsMessage_t(intervalInSecs=$intervalInSecs, slaveConfigs=$slaveConfigs)"
}