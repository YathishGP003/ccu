package a75f.io.device.util

import java.nio.ByteBuffer

/**
 * Calculate 16-bit Modbus CRC (CRC-16-IBM, poly x^16 + x^15 + x^2 + 1)
 * over the remaining bytes in the given ByteBuffer.
 *
 * - Polynomial: 0xA001 (reflected 0x8005)
 * - Initial value: 0xFFFF
 * - No final XOR
 *
 * @param buffer ByteBuffer whose bytes (from position() to limit()) are used.
 *               The buffer's position is NOT modified.
 * @return CRC value as an unsigned 16-bit Int (0..0xFFFF)
 */
fun calculateModbusCrc(buffer: ByteBuffer): Int {
    var crc = 0xFFFF

    // Work on indices so we don't disturb buffer.position()
    val start = buffer.position()
    val end = buffer.limit()

    for (i in start until end) {
        val b = buffer.get(i).toInt() and 0xFF
        crc = crc xor b
        repeat(8) {
            crc = if ((crc and 0x0001) != 0) {
                (crc ushr 1) xor 0xA001
            } else {
                crc ushr 1
            }
        }
    }

    return crc and 0xFFFF
}