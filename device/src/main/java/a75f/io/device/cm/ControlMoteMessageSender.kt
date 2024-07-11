package a75f.io.device.cm

import a75f.io.device.mesh.LSerial
import a75f.io.device.serial.MessageType
import a75f.io.logger.CcuLog
import a75f.io.logic.L


fun sendControlMoteMessage(messageType: MessageType, dataBytes : ByteArray)   {
    val msgBytes  = ByteArray(dataBytes.size + 1)
    msgBytes[0] = messageType.ordinal.toByte()
    System.arraycopy(dataBytes, 0, msgBytes, 1, dataBytes.size)
    LSerial.getInstance().sendSerialBytesToCM(msgBytes)
}

fun sendTestModeMessage() {

    val cmSettingsMessage = getCMSettingsMessage()
    CcuLog.d(L.TAG_CCU_DEVICE, "CM Proto Settings Message: $cmSettingsMessage")
    sendControlMoteMessage(MessageType.CCU_TO_CM_OVER_USB_CM_SERIAL_SETTINGS, cmSettingsMessage.toByteArray())

    val cmControlMessage = getCMControlsMessage()
    CcuLog.d(L.TAG_CCU_DEVICE, "CM Proto Control Message: $cmControlMessage")
    sendControlMoteMessage(MessageType.CCU_TO_CM_OVER_USB_CM_SERIAL_CONTROLS, cmControlMessage.toByteArray())
}