package a75f.io.device.mesh;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import a75f.io.device.HyperStat.*;
import a75f.io.device.HyperStat.HyperStatCcuDatabaseSeedMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

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
        writeSeedMessage(seedMessage, address, false);
    }
    
    public static void writeSeedMessage(HyperStatCcuDatabaseSeedMessage_t seedMessage, int address,
                                        boolean resend) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            seedMessage.writeTo(os);
    
            if (!resend) {
                Integer messageHash = Arrays.hashCode(os.toByteArray());
                if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName(),
                                                                       messageHash)) {
                    CcuLog.d(L.TAG_CCU_SERIAL, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName() +
                                               " was already " +
                                               "sent, returning");
                    return;
                }
            }
            writeMessageBytesToUsb((byte) MessageType.HYPERSTAT_CCU_DATABASE_SEED_MESSAGE.ordinal(), os);
            CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + seedMessage);
        
        }catch (IOException e) {
            CcuLog.e(L.TAG_CCU_SERIAL, "writeSeedMessage Exception ", e);
        }
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
        writeSettingsMessage(settings, address, false);
    }
    
    public static void writeSettingsMessage(HyperStatSettingsMessage_t settings, int address, boolean resend) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            settings.writeTo(os);
    
            if (!resend) {
                Integer messageHash = Arrays.hashCode(os.toByteArray());
                if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName(),
                                                                       messageHash)) {
                    CcuLog.d(L.TAG_CCU_SERIAL, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName() +
                                               " was already " +
                                               "sent, returning");
                    return;
                }
            }
            
            writeMessageBytesToUsb((byte)MessageType.HYPERSTAT_SETTINGS_MESSAGE.ordinal(), os);
            CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + settings);
        
        } catch (IOException e) {
            CcuLog.e(L.TAG_CCU_SERIAL, "writeSettingsMessage Exception ", e);
        }
    }
    
    /**
     * Send control message based on the node's state in database.
     * Message will be sent only the current message is different from last sent state
     * @param address
     * @param equipRef
     */
    public static void sendControlMessage(int address, String equipRef) {
        HyperStatControlsMessage_t controls = HyperStatMessageGenerator.getControlMessage(address, equipRef);
        writeControlMessage(controls, address, false);
    }
    
    public static void writeControlMessage(HyperStatControlsMessage_t controls, int address, boolean resend) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            controls.writeTo(os);
            if (!resend) {
                Integer messageHash = Arrays.hashCode(os.toByteArray());
                if (HyperStatMessageCache.getInstance().checkAndInsert(address, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName(),
                                                                       messageHash)) {
                    CcuLog.d(L.TAG_CCU_SERIAL, HyperStatCcuDatabaseSeedMessage_t.class.getSimpleName() +
                                               " was already " +
                                               "sent, returning");
                    return;
                }
            }
            
            writeMessageBytesToUsb((byte)MessageType.HYPERSTAT_CONTROLS_MESSAGE.ordinal(), os);
            CcuLog.i(L.TAG_CCU_SERIAL, "Send Proto Buf Message " + controls);
        
        }catch (IOException e) {
            CcuLog.e(L.TAG_CCU_SERIAL, "writeControlMessage Exception ", e);
        }
    }
    
    private static void writeMessageBytesToUsb(byte msgType, ByteArrayOutputStream os) {
        
        byte[] tempBytes = os.toByteArray();
        byte[] msgBytes = new byte[tempBytes.length+1];
        
        //CM currently supports both legacy byte array and protobuf encoding. Message type is kept as raw byte at the start to help CM determine which type
        //of decoding to be used.
        msgBytes[0] = msgType;
        
        System.arraycopy(tempBytes, 0, msgBytes, 1, tempBytes.length);
        
        LSerial.getInstance().sendSerialBytesToCM(msgBytes);
        Log.d("CCU_SERIAL", Arrays.toString(msgBytes));
        
    }
    
}
