package a75f.io.device.mesh;

import android.util.Log;

import java.util.Arrays;

import a75f.io.device.HyperStat.HyperStatCcuDatabaseSeedMessage_t;
import a75f.io.device.HyperStat.HyperStatCcuToCmSerializedMessage_t;
import a75f.io.device.HyperStat.HyperStatControlsMessage_t;
import a75f.io.device.HyperStat.HyperStatSettingsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

import static a75f.io.device.serial.MessageType.HYPERSTAT_CCU_DATABASE_SEED_MESSAGE;

public class HyperStatMessageSender {
    
    /**
     * Send seed message based on the node's state in database.
     * Message will be sent only the current state is different from last sent state.
     * @param zone
     * @param address
     * @param equipRef
     * @param profile
     */
    public static void sendSeedMessage(String zone,int address,String equipRef, String profile) {
        HyperStatCcuDatabaseSeedMessage_t seedMessage = HyperStatMessageGenerator.getSeedMessage(zone, address,
                                                                                                         equipRef, profile);
        writeSeedMessage(seedMessage, address, true);
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
        writeMessageBytesToUsb(HYPERSTAT_CCU_DATABASE_SEED_MESSAGE, seedMessage.toByteArray());
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + seedMessage);
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
        HyperStatCcuToCmSerializedMessage_t message = HyperStatCcuToCmSerializedMessage_t
                                                          .newBuilder()
                                                          .setAddress(address)
                                                          .setProtocolMessageType(MessageType.HYPERSTAT_SETTINGS_MESSAGE.ordinal())
                                                          .setSerializedMessageData(settings.toByteString())
                                                          .build();
    
        writeSerializedMessage(message, address, MessageType.HYPERSTAT_SETTINGS_MESSAGE, true);
    }
    
    /**
     * Send control message based on the node's state in database.
     * Message will be sent only the current message is different from last sent state
     * @param address
     * @param equipRef
     */
    public static void sendControlMessage(int address, String equipRef) {
        HyperStatControlsMessage_t controls = HyperStatMessageGenerator.getControlMessage(address, equipRef);
        
        HyperStatCcuToCmSerializedMessage_t message = HyperStatCcuToCmSerializedMessage_t
                                                          .newBuilder()
                                                          .setAddress(address)
                                                          .setProtocolMessageType(MessageType.HYPERSTAT_CONTROLS_MESSAGE.ordinal())
                                                          .setSerializedMessageData(controls.toByteString())
                                                          .build();
    
        writeSerializedMessage(message, address, MessageType.HYPERSTAT_CONTROLS_MESSAGE, true);
    }
    
    public static void writeSerializedMessage(HyperStatCcuToCmSerializedMessage_t message, int address,
                                              MessageType msgType, boolean checkDuplicate) {
        if (checkDuplicate) {
            Integer messageHash = Arrays.hashCode(message.toByteArray());
            if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatCcuToCmSerializedMessage_t.class.getSimpleName(),
                                                                   messageHash)) {
                CcuLog.d(L.TAG_CCU_SERIAL, HyperStatCcuToCmSerializedMessage_t.class.getSimpleName() +
                                           " was already sent, returning , type "+msgType);
                return;
            }
        }
    
        writeMessageBytesToUsb(msgType, message.toByteArray());
        CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + msgType);
    }
    
    private static void writeMessageBytesToUsb(MessageType msgType, byte[] dataBytes) {
        
        byte[] msgBytes = new byte[dataBytes.length+1];
        
        //CM currently supports both legacy byte array and protobuf encoding. Message type is kept as raw byte at the start to help CM determine which type
        //of decoding to be used.
        msgBytes[0] = (byte)msgType.ordinal();
        
        System.arraycopy(dataBytes, 0, msgBytes, 1, dataBytes.length);
        
        LSerial.getInstance().sendSerialBytesToCM(msgBytes);
        Log.d(L.TAG_CCU_SERIAL, Arrays.toString(msgBytes));
        
    }
    
}
