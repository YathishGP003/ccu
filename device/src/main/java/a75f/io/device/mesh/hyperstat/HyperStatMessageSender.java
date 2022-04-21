package a75f.io.device.mesh.hyperstat;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.HyperStat;
import a75f.io.device.HyperStat.HyperStatCcuDatabaseSeedMessage_t;
import a75f.io.device.HyperStat.HyperStatCcuToCmSerializedMessage_t;
import a75f.io.device.HyperStat.HyperStatControlsMessage_t;
import a75f.io.device.HyperStat.HyperStatSettingsMessage_t;
import a75f.io.device.mesh.DLog;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.vrv.VrvControlMessageCache;

import static a75f.io.device.serial.MessageType.HYPERSTAT_CCU_DATABASE_SEED_MESSAGE;
import static a75f.io.device.serial.MessageType.HYPERSTAT_CCU_TO_CM_SERIALIZED_MESSAGE;

public class HyperStatMessageSender {
    
    private static final int FIXED_INT_BYTES_SIZE = 4;
    
    /**
     * Send seed message based on the node's state in database.
     * Message will be sent only the current state is different from last sent state.
     * @param zone
     * @param address
     * @param equipRef
     * @param profile
     */
    public static void sendSeedMessage(String zone,int address,String equipRef, String profile,
                                       boolean checkDuplicate) {
        HyperStatCcuDatabaseSeedMessage_t seedMessage = HyperStatMessageGenerator.getSeedMessage(zone, address,
                                                                                                 equipRef, profile);
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + HYPERSTAT_CCU_DATABASE_SEED_MESSAGE);
            CcuLog.i(L.TAG_CCU_SERIAL, seedMessage.getSerializedSettingsData().toString());
        }
        
        writeSeedMessage(seedMessage, address, checkDuplicate);
    }
    
    public static void writeSeedMessage(HyperStatCcuDatabaseSeedMessage_t seedMessage, int address,
                                        boolean checkDuplicate) {
    
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(seedMessage.toByteArray());
            if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName(),
                                                                   messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName() +
                                           " was already sent, returning "+address);
                return;
            }
        }
        writeMessageBytesToUsb(address, HYPERSTAT_CCU_DATABASE_SEED_MESSAGE, seedMessage.toByteArray());
    }
    
    /**
     * Send setting message based on the node's state in database.
     * Message will be sent only the current message is different from last sent state
     * @param zone
     * @param address
     * @param equipRef
     */
    public static void sendSettingsMessage(String zone, int address, String equipRef) {
        HyperStatSettingsMessage_t settings = HyperStatMessageGenerator.getSettingsMessage(zone, address,
                                                                                                     equipRef);
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, settings.toString());
        }
    
        writeSettingMessage(settings, address, MessageType.HYPERSTAT_SETTINGS_MESSAGE, true);
    }
    
    public static void writeSettingMessage(HyperStatSettingsMessage_t message, int address,
                                           MessageType msgType, boolean checkDuplicate) {
        
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatSettingsMessage_t.class.getSimpleName(),
                                                                   messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperStatSettingsMessage_t.class.getSimpleName() +
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
        HyperStatControlsMessage_t controls = HyperStatMessageGenerator.getControlMessage(address, equipRef).build();
        
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, controls.toString());
        }
    
        writeControlMessage(controls, address, MessageType.HYPERSTAT_CONTROLS_MESSAGE, true);
    }
    
    
    public static void writeControlMessage(HyperStatControlsMessage_t message, int address,
                                              MessageType msgType, boolean checkDuplicate) {
        
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatCcuToCmSerializedMessage_t.class.getSimpleName(),
                                                                   messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperStatCcuToCmSerializedMessage_t.class.getSimpleName() +
                                           " was already sent, returning , type "+msgType);
                return;
            }
        }
        
        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }
    
    public static void sendIduControlMessage(int address, CCUHsApi hayStack) {
        HyperStat.HyperStatIduControlsMessage_t controls = HyperStatIduMessageHandler.getIduControlMessage(address, hayStack);
        
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, controls.toString());
        }
    
        writeIduControlMessage(controls, address, MessageType.HYPERSTAT_IDU_CONTROLS_MESSAGE, true);
        VrvControlMessageCache.getInstance().updateControlsPending(address);
    }
    
    /**
     * Sends the IDU control message as part of seed, without any duplicate check.
     * @param address
     * @param hayStack
     */
    public static void sendIduSeedControlMessage(int address, CCUHsApi hayStack) {
        HyperStat.HyperStatIduControlsMessage_t controls = HyperStatIduMessageHandler.getIduControlMessage(address, hayStack);
    
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, controls.toString());
        }
    
        writeIduControlMessage(controls, address, MessageType.HYPERSTAT_IDU_CONTROLS_MESSAGE, false);
    }
    
    public static void writeIduControlMessage(HyperStat.HyperStatIduControlsMessage_t message, int address,
                                              MessageType msgType, boolean checkDuplicate) {
        
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatCcuToCmSerializedMessage_t.class.getSimpleName(),
                                                                   messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperStatCcuToCmSerializedMessage_t.class.getSimpleName() +
                                           " was already sent for "+address+": returning , type "+msgType);
                return;
            }
        }
        
        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }
    
    private static void writeMessageBytesToUsb(int address, MessageType msgType, byte[] dataBytes) {
        
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
        HyperStat.HyperStatSettingsMessage2_t settingsMessage2 = HyperStatMessageGenerator.getSetting2Message(address, equipRef);
        HyperStat.HyperStatSettingsMessage3_t settingsMessage3 = HyperStatMessageGenerator.getSetting3Message(address, equipRef);

        if (DLog.isLoggingEnabled()) {
            CcuLog.d(L.TAG_CCU_DEVICE,"Debugger Enabled");
            CcuLog.i(L.TAG_CCU_SERIAL, settingsMessage2.toString());
            CcuLog.i(L.TAG_CCU_SERIAL, settingsMessage3.toString());
        }

        writeSetting2Message(settingsMessage2, address, MessageType.HYPERSTAT_SETTINGS2_MESSAGE, true);
        writeSetting3Message(settingsMessage3, address, MessageType.HYPERSTAT_SETTINGS3_MESSAGE, true);
    }

    public static void writeSetting2Message(HyperStat.HyperStatSettingsMessage2_t message, int address,
                                            MessageType msgType, boolean checkDuplicate) {

        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStat.HyperStatSettingsMessage2_t.class.getSimpleName(),
                    messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperStat.HyperStatSettingsMessage2_t.class.getSimpleName() +
                        " was already sent, returning , type "+msgType);
                return;
            }
        }

        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }

    public static void writeSetting3Message(HyperStat.HyperStatSettingsMessage3_t message, int address,
                                            MessageType msgType, boolean checkDuplicate) {
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStat.HyperStatSettingsMessage3_t.class.getSimpleName(),
                    messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperStat.HyperStatSettingsMessage3_t.class.getSimpleName() +
                        " was already sent, returning , type "+msgType);
                return;
            }
        }
        writeMessageBytesToUsb(address, msgType, message.toByteArray());
    }

    public static void sendRestartModuleCommand(int address){
        writeControlMessage(HyperStatMessageGenerator.getHyperstatRebootControl(address), address,
                MessageType.HYPERSTAT_CONTROLS_MESSAGE,
                false);
    }
}
