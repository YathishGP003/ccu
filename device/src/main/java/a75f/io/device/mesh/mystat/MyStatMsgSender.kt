package a75f.io.device.mesh.mystat

import a75f.io.device.BuildConfig
import a75f.io.device.HyperStat.HyperStatCcuDatabaseSeedMessage_t
import a75f.io.device.MyStat
import a75f.io.device.MyStat.MyStatCcuDatabaseSeedMessage_t
import a75f.io.device.MyStat.MyStatControlsMessage_t
import a75f.io.device.MyStat.MyStatSettingsMessage2_t
import a75f.io.device.mesh.DLog
import a75f.io.device.mesh.LSerial
import a75f.io.device.serial.MessageType
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by Manjunath K on 13-01-2025.
 */

object MyStatMsgSender {

    private const val FIXED_INT_BYTES_SIZE: Int = 4

    var ccuControlMessageTimer: Long = 0
        get() {
            if (field == 0L) {
                ccuControlMessageTimer = System.currentTimeMillis()
            }
            return field
        }

    fun writeSeedMessage(
        seedMessage: MyStatCcuDatabaseSeedMessage_t, address: Int, checkDuplicate: Boolean
    ) {
        if (checkDuplicate) {
            val messageHash = seedMessage.toByteArray().contentHashCode()
            if (MyStatMsgCache.checkAndInsert(
                    address,
                    MyStatCcuDatabaseSeedMessage_t::class.java.simpleName,
                    messageHash
                )
            ) {
                CcuLog.d(
                    L.TAG_CCU_SERIAL, HyperStatCcuDatabaseSeedMessage_t::class.java.simpleName +
                            " was already sent, returning " + address
                )
                return
            }
        }
        writeMessageBytesToUsb(
            address,
            MessageType.MYSTAT_CCU_DATABASE_SEED_MESSAGE,
            seedMessage.toByteArray()
        )
    }

    fun sendSeedMessage(
        zone: String, address: Int, equipRef: String, checkDuplicate: Boolean
    ) {
        val seedMessage = getMyStatSeedMessage(zone, address, equipRef)
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(
                L.TAG_CCU_SERIAL,
                "Send Proto Buf Message " + MessageType.MYSTAT_CCU_DATABASE_SEED_MESSAGE
            )
            CcuLog.i(L.TAG_CCU_SERIAL, seedMessage.serializedSettingsData.toString())
        }
        writeSeedMessage(seedMessage, address, checkDuplicate)
    }

    private fun writeMessageBytesToUsb(address: Int, msgType: MessageType, dataBytes: ByteArray) {
        CcuLog.d(L.TAG_CCU_SERIAL, "writeMessageBytesToUsb")
        ccuControlMessageTimer = System.currentTimeMillis()
        val msgBytes = ByteArray(dataBytes.size + FIXED_INT_BYTES_SIZE * 2 + 1)
        //CM currently supports both legacy byte array and protobuf encoding. Message type is kept as raw byte at the start to help CM determine which type
        //of decoding to be used.
        msgBytes[0] = MessageType.HYPERSTAT_CCU_TO_CM_SERIALIZED_MESSAGE.ordinal.toByte()

        //Network requires un-encoded node address occupying the first 4 bytes
        System.arraycopy(getByteArrayFromInt(address), 0, msgBytes, 1, FIXED_INT_BYTES_SIZE)

        //Network requires un-encoded message type occupying the next 4 bytes
        System.arraycopy(
            getByteArrayFromInt(msgType.ordinal),
            0,
            msgBytes,
            FIXED_INT_BYTES_SIZE + 1,
            FIXED_INT_BYTES_SIZE
        )

        //Now fill the serialized protobuf messages
        System.arraycopy(
            dataBytes, 0, msgBytes, 2 * FIXED_INT_BYTES_SIZE + 1, dataBytes.size
        )

        LSerial.getInstance().sendSerialBytesToCM(msgBytes)
        CcuLog.d(L.TAG_CCU_SERIAL, msgBytes.contentToString())
    }

    private fun getByteArrayFromInt(integerVal: Int): ByteArray {
        return ByteBuffer.allocate(FIXED_INT_BYTES_SIZE)
            .order(ByteOrder.LITTLE_ENDIAN).putInt(integerVal).array()
    }

    fun sendControlMessage(address: Int) {
        val controls = getMyStatControlMessage(address).build()
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, controls.toString())
        }
        writeControlMessage(controls, address, MessageType.MYSTAT_CONTROLS_MESSAGE, true)
    }

    fun sendSettingMessage(address: Int, equipRef: String, zone: String) {
        val settings = getMyStatSettingsMessage(equipRef, zone)
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, settings.toString())
        }
        writeSettingMessage(settings, address, MessageType.MYSTAT_SETTINGS_MESSAGE, true)
    }

    fun sendSetting2Message(address: Int, equipRef: String) {
        val settings = getMyStatSettings2Message(equipRef)
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, settings.toString())
        }
        writeSetting2Message(settings, address, MessageType.MYSTAT_SETTINGS2_MESSAGE, true)
    }

    fun writeControlMessage(
        message: MyStatControlsMessage_t, address: Int,
        msgType: MessageType, checkDuplicate: Boolean
    ) {
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message $msgType")
        if ((BuildConfig.BUILD_TYPE == "staging") || BuildConfig.BUILD_TYPE == "prod") {
            if (checkDuplicate && MyStatMsgCache.checkControlMessage(address, message)) {
                CcuLog.d(
                    L.TAG_CCU_SERIAL,
                    msgType::class.java.simpleName +
                            " was already sent, returning , type " + msgType
                )
                return
            }
        } else if (checkDuplicate) {
            val messageHash = message.toByteArray().contentHashCode()
            if (MyStatMsgCache.checkAndInsert(
                    address, MyStatControlsMessage_t::class.java.simpleName, messageHash
                )
            ) {
                CcuLog.d(
                    L.TAG_CCU_SERIAL, (MyStatControlsMessage_t::class.java.simpleName +
                            " was already sent, returning , type " + msgType)
                )
                return
            }
        }
        CcuLog.d(L.TAG_CCU_DEVICE, "sending ==> $message")
        writeMessageBytesToUsb(address, msgType, message.toByteArray())
    }


    fun writeSettingMessage(
        message: MyStat.MyStatSettingsMessage_t, address: Int,
        msgType: MessageType, checkDuplicate: Boolean
    ) {
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message $msgType")
        if (checkDuplicate) {
            val messageHash = message.toByteArray().contentHashCode()
            if (MyStatMsgCache.checkAndInsert(
                    address, MyStat.MyStatSettingsMessage_t::class.java.simpleName,
                    messageHash
                )
            ) {
                CcuLog.d(
                    L.TAG_CCU_SERIAL, MyStat.MyStatSettingsMessage_t::class.java.simpleName +
                            " was already sent, returning , type " + msgType
                )
                return
            }
        }
        writeMessageBytesToUsb(address, msgType, message.toByteArray())
    }

    private fun writeSetting2Message(
        message: MyStatSettingsMessage2_t, address: Int,
        msgType: MessageType, checkDuplicate: Boolean
    ) {
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message $msgType")
        if (checkDuplicate) {
            val messageHash = message.toByteArray().contentHashCode()
            if (MyStatMsgCache.checkAndInsert(
                    address, MyStatSettingsMessage2_t::class.java.simpleName,
                    messageHash
                )
            ) {
                CcuLog.d(
                    L.TAG_CCU_SERIAL, MyStatSettingsMessage2_t::class.java.simpleName +
                            " was already sent, returning , type " + msgType
                )
                return
            }
        }
        writeMessageBytesToUsb(address, msgType, message.toByteArray())
    }

    fun sendRestartModuleCommand(address: Int) {
        writeControlMessage(
            getMyStatRebootControl(address), address, MessageType.MYSTAT_CONTROLS_MESSAGE, false
        )
    }
}


