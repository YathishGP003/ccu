package a75f.io.device.mesh.mystat

import a75f.io.device.MyStat
import a75f.io.device.serial.MessageType
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.interfaces.ZoneDataInterface
import com.google.protobuf.InvalidProtocolBufferException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays

/**
 * Created by Manjunath K on 13-01-2025.
 */


private const val MYSTAT_MSG_ADDR_START_INDEX = 1
private const val MYSTAT_MSG_ADDR_END_INDEX = 5
private const val MYSTAT_MSG_TYPE_INDEX = 13
private const val MYSTAT_SERIALIZED_MSG_START_INDEX = 17
private var currentTempInterface: ZoneDataInterface? = null


fun processMessage(data: ByteArray) {
    try {
        CcuLog.e(
            L.TAG_CCU_DEVICE,
            "MyStat Receiver processMessage processMessage :" + data.contentToString()
        )

        /* Message Type - 1 byte
        * Address - 4 bytes
        * CM lqi - 4 bytes
        * CM rssi - 4 bytes
        * Message types - 4 bytes
        * Actual Serialized data starts at index 17.
        */
        val addrArray =
            Arrays.copyOfRange(data, MYSTAT_MSG_ADDR_START_INDEX, MYSTAT_MSG_ADDR_END_INDEX)
        val address = ByteBuffer.wrap(addrArray).order(ByteOrder.LITTLE_ENDIAN).getInt()
        val msgType = MessageType.values()[data[MYSTAT_MSG_TYPE_INDEX].toInt()]
        val msgArray = Arrays.copyOfRange(data, MYSTAT_SERIALIZED_MSG_START_INDEX, data.size)

        when (msgType) {

            MessageType.MYSTAT_REGULAR_UPDATE_MESSAGE -> {
                val regularUpdate = MyStat.MyStatRegularUpdateMessage_t.parseFrom(msgArray)
                handleMyStatRegularUpdate(regularUpdate, address, currentTempInterface)
            }

            MessageType.MYSTAT_LOCAL_CONTROLS_OVERRIDE_MESSAGE -> {
                val overrideMessage =
                    MyStat.MyStatLocalControlsOverrideMessage_t.parseFrom(msgArray)
                handleMyStatOverrideMessage(overrideMessage, address, currentTempInterface)
            }

            else -> {
                CcuLog.e(L.TAG_CCU_DEVICE, "Unknown message type: $msgType")
            }
        }

    } catch (e: InvalidProtocolBufferException) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Cant parse protobuf data: " + e.message)
    }
}
fun setCurrentTempInterface(reference: ZoneDataInterface?) {
    currentTempInterface = reference
}