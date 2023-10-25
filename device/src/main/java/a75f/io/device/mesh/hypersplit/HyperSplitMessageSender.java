package a75f.io.device.mesh.hypersplit;

import static a75f.io.device.serial.MessageType.HYPERSPLIT_CCU_DATABASE_SEED_MESSAGE;
import static a75f.io.device.serial.MessageType.HYPERSTAT_CCU_TO_CM_SERIALIZED_MESSAGE;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.BuildConfig;
import a75f.io.device.HyperSplit;
import a75f.io.device.mesh.DLog;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.util.TemperatureMode;

public class HyperSplitMessageSender {

    private static final int FIXED_INT_BYTES_SIZE = 4;

    /**
     * Send seed message based on the node's state in database.
     * Message will be sent only the current state is different from last sent state.
     * @param zone
     * @param address
     * @param equipRef
     */
    public static void sendSeedMessage(String zone, int address, String equipRef,
                                       boolean checkDuplicate, TemperatureMode mode) {
        HyperSplit.HyperSplitCcuDatabaseSeedMessage_t seedMessage = HyperSplitMessageGenerator.getSeedMessage(zone, address,
                equipRef, mode);
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + HYPERSPLIT_CCU_DATABASE_SEED_MESSAGE);
            CcuLog.i(L.TAG_CCU_SERIAL, seedMessage.getSerializedSettingsData().toString());
        }

        writeSeedMessage(seedMessage, address, checkDuplicate);
    }

    public static void writeSeedMessage(HyperSplit.HyperSplitCcuDatabaseSeedMessage_t seedMessage, int address,
                                        boolean checkDuplicate) {

        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(seedMessage.toByteArray());
            if (HyperSplitMessageCache.getInstance().checkAndInsert(address, HyperSplit.HyperSplitCcuDatabaseSeedMessage_t.class.getSimpleName(),
                    messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperSplit.HyperSplitCcuDatabaseSeedMessage_t.class.getSimpleName() +
                        " was already sent, returning "+address);
                return;
            }
        }
        writeMessageBytesToUsb(address, HYPERSPLIT_CCU_DATABASE_SEED_MESSAGE, seedMessage.toByteArray());
    }

    /**
     * Send setting message based on the node's state in database.
     * Message will be sent only the current message is different from last sent state
     * @param zone
     * @param address
     * @param equipRef
     */
    public static void sendSettingsMessage(Zone zone, int address, String equipRef) {
        int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef == \"" + zone.getId() + "\"").intValue();
        HyperSplit.HyperSplitSettingsMessage_t settings = HyperSplitMessageGenerator.getSettingsMessage(
                zone.getDisplayName(), address, equipRef, TemperatureMode.values()[modeType]);
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, settings.toString());
        }

        writeSettingMessage(settings, address, MessageType.HYPERSPLIT_SETTINGS_MESSAGE, true);
    }

    public static void writeSettingMessage(HyperSplit.HyperSplitSettingsMessage_t message, int address,
                                           MessageType msgType, boolean checkDuplicate) {

        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperSplitMessageCache.getInstance().checkAndInsert(address, HyperSplit.HyperSplitSettingsMessage_t.class.getSimpleName(),
                    messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperSplit.HyperSplitSettingsMessage_t.class.getSimpleName() +
                        " was already sent, returning , type "+msgType);
                return;
            }
        }

        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }

    /**
     * Send control message based on the node's state in database.
     * Message will be sent only the current message is different from last sent state
     * @param address
     * @param equipRef
     */
    public static void sendControlMessage(int address, String equipRef) {
        Log.d(L.TAG_CCU_SERIAL, "sendControlMessage("+address+","+equipRef+")");
        Equip equip = HSUtil.getEquipInfo(equipRef);
        int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                " == \"" + equip.getRoomRef() + "\"").intValue();
        HyperSplit.HyperSplitControlsMessage_t controls = HyperSplitMessageGenerator.getControlMessage(address,
                equipRef, TemperatureMode.values()[modeType]).build();

        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, controls.toString());
        }

        writeControlMessage(controls, address, MessageType.HYPERSPLIT_CONTROLS_MESSAGE, true);
    }

    public static void writeControlMessage(HyperSplit.HyperSplitControlsMessage_t message, int address,
                                           MessageType msgType, boolean checkDuplicate) {

        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (BuildConfig.BUILD_TYPE.equals("staging") ||
                BuildConfig.BUILD_TYPE.equals("prod")) {
            if (checkDuplicate && HyperSplitMessageCache.getInstance().checkControlMessage(address
                    , message)) {
                CcuLog.d(L.TAG_CCU_SERIAL,
                        HyperSplit.HyperSplitCcuToCmSerializedMessage_t.class.getSimpleName() +
                                " was already sent, returning , type " + msgType);
                return;
            }
        } else if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperSplitMessageCache.getInstance().checkAndInsert(address,
                    HyperSplit.HyperSplitSettingsMessage_t.class.getSimpleName(),
                    messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperSplit.HyperSplitSettingsMessage_t.class.getSimpleName() +
                        " was already sent, returning , type " + msgType);
                return;
            }

        }

        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }

    // sendIduSeedControlMessage() if VRV is supported at some point
    // writeIduControlMessage() if VRV is supported at some point
    // sendIduSeedSetting() if VRV is supported at some point

    private static void writeMessageBytesToUsb(int address, MessageType msgType, byte[] dataBytes) {
        Log.d(L.TAG_CCU_SERIAL,"writeMessageBytesToUsb");
        HyperSplitSettingsUtil.Companion.setCcuControlMessageTimer(System.currentTimeMillis());
        byte[] msgBytes = new byte[dataBytes.length + FIXED_INT_BYTES_SIZE * 2 + 1];
        //CM currently supports both legacy byte array and protobuf encoding. Message type is kept as raw byte at the start to help CM determine which type
        //of decoding to be used.
        msgBytes[0] = (byte)HYPERSTAT_CCU_TO_CM_SERIALIZED_MESSAGE.ordinal();

        //Network requires un-encoded node address occupying the first 4 bytes
        System.arraycopy(getByteArrayFromInt(address), 0, msgBytes, 1, FIXED_INT_BYTES_SIZE);

        //Network requires un-encoded message type occupying the next 4 bytes
        System.arraycopy(getByteArrayFromInt(msgType.ordinal()),
                0, msgBytes, FIXED_INT_BYTES_SIZE + 1, FIXED_INT_BYTES_SIZE);

        //Now fill the serialized protobuf messages
        System.arraycopy(dataBytes, 0, msgBytes,  2 * FIXED_INT_BYTES_SIZE + 1, dataBytes.length);

        LSerial.getInstance().sendSerialBytesToCM(msgBytes);
        Log.d(L.TAG_CCU_SERIAL, Arrays.toString(msgBytes));

    }

    private static byte[] getByteArrayFromInt(int integerVal) {
        return ByteBuffer.allocate(FIXED_INT_BYTES_SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(integerVal).array();
    }

    public static void sendAdditionalSettingMessages(int address, String equipRef){
        HyperSplit.HyperSplitSettingsMessage2_t settingsMessage2 = HyperSplitMessageGenerator.getSetting2Message(address, equipRef);
        HyperSplit.HyperSplitSettingsMessage3_t settingsMessage3 = HyperSplitMessageGenerator.getSetting3Message(address, equipRef);

        if (DLog.isLoggingEnabled()) {
            CcuLog.d(L.TAG_CCU_DEVICE,"Debugger Enabled");
            CcuLog.i(L.TAG_CCU_SERIAL, settingsMessage2.toString());
            CcuLog.i(L.TAG_CCU_SERIAL, settingsMessage3.toString());
        }

        writeSetting2Message(settingsMessage2, address, MessageType.HYPERSPLIT_SETTINGS2_MESSAGE, true);
        writeSetting3Message(settingsMessage3, address, MessageType.HYPERSPLIT_SETTINGS3_MESSAGE, true);
    }

    public static void writeSetting2Message(HyperSplit.HyperSplitSettingsMessage2_t message, int address,
                                            MessageType msgType, boolean checkDuplicate) {

        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperSplitMessageCache.getInstance().checkAndInsert(address, HyperSplit.HyperSplitSettingsMessage2_t.class.getSimpleName(),
                    messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperSplit.HyperSplitSettingsMessage2_t.class.getSimpleName() +
                        " was already sent, returning , type "+msgType);
                return;
            }
        }

        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }

    public static void writeSetting3Message(HyperSplit.HyperSplitSettingsMessage3_t message, int address,
                                            MessageType msgType, boolean checkDuplicate) {

        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperSplitMessageCache.getInstance().checkAndInsert(address, HyperSplit.HyperSplitSettingsMessage3_t.class.getSimpleName(),
                    messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperSplit.HyperSplitSettingsMessage3_t.class.getSimpleName() +
                        " was already sent, returning , type "+msgType);
                return;
            }
        }

        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }

    public static void sendRestartModuleCommand(int address){
        writeControlMessage(HyperSplitMessageGenerator.getHypersplitRebootControl(address), address,
                MessageType.HYPERSTAT_CONTROLS_MESSAGE,
                false);
    }

}
