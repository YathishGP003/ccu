package a75f.io.device.cm

import a75f.io.device.mesh.LSerial
import a75f.io.device.serial.MessageType


fun sendControlMoteMessage(messageType: MessageType, dataBytes : ByteArray)   {
    val msgBytes  = ByteArray(dataBytes.size + 1)
    msgBytes[0] = messageType.ordinal.toByte()
    System.arraycopy(dataBytes, 0, msgBytes, 1, dataBytes.size)
    LSerial.getInstance().sendSerialBytesToCM(msgBytes)
}