package a75f.io.device.mesh.hyperstat;

import static a75f.io.device.mesh.hyperstat.HyperStatControlUtilKt.getHyperStatControlMessage;
import static a75f.io.device.mesh.hyperstat.HyperStatSettingsUtilKt.getHyperStatSettings2Message;
import static a75f.io.device.mesh.hyperstat.HyperStatSettingsUtilKt.getHyperStatSettings3Message;
import static a75f.io.device.mesh.hyperstat.HyperStatSettingsUtilKt.getHyperStatSettingsMessage;

import com.google.protobuf.ByteString;

import java.util.HashMap;

import a75f.io.device.HyperStat;
import a75f.io.device.HyperStat.HyperStatCcuDatabaseSeedMessage_t;
import a75f.io.device.HyperStat.HyperStatControlsMessage_t;
import a75f.io.device.HyperStat.HyperStatSettingsMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class HyperStatMessageGenerator {
    
    /**
     * Generates seed message for a node from haystack data.
     *
     * @param zone
     * @param address
     * @param equipRef
     * @return
     */
    public static HyperStatCcuDatabaseSeedMessage_t getSeedMessage(String zone, int address,
                                                                   String equipRef) {
        HyperStatSettingsMessage_t hyperStatSettingsMessage_t = getSettingsMessage(zone,
                equipRef);
        HyperStatControlsMessage_t hyperStatControlsMessage_t = getControlMessage(address
        ).build();
        HyperStat.HyperStatSettingsMessage2_t hyperStatSettingsMessage2_t = getHyperStatSettings2Message(equipRef);
        HyperStat.HyperStatSettingsMessage3_t hyperStatSettingsMessage3_t = getHyperStatSettings3Message(equipRef);

        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+hyperStatSettingsMessage_t.toByteString().toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+hyperStatControlsMessage_t.toString());
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+ hyperStatSettingsMessage2_t);
        CcuLog.i(L.TAG_CCU_SERIAL, "Seed Message t"+ hyperStatSettingsMessage3_t);

        return HyperStatCcuDatabaseSeedMessage_t.newBuilder()
                .setEncryptionKey(ByteString.copyFrom(L.getEncryptionKey()))
                .setSerializedSettingsData(hyperStatSettingsMessage_t.toByteString())
                .setSerializedControlsData(hyperStatControlsMessage_t.toByteString())
                .setSerializedSettings2Data(hyperStatSettingsMessage2_t.toByteString())
                .setSerializedSettings3Data(hyperStatSettingsMessage3_t.toByteString())
                .build();
    }
    
    /**
     * Generate settings message for a node from haystack data.
     * @param zone
     * @param equipRef
     * @return
     */
    public static HyperStatSettingsMessage_t getSettingsMessage(String zone, String equipRef) {
        return getHyperStatSettingsMessage(equipRef, zone);
    }
    public static HyperStat.HyperStatSettingsMessage2_t getSetting2Message(String equipRef){
        return getHyperStatSettings2Message(equipRef);
    }

    public static HyperStat.HyperStatSettingsMessage3_t getSetting3Message(String equipRef){
        return getHyperStatSettings3Message(equipRef);
    }
    /**
     * Generate control message for a node from haystack data.
     *
     * @param address
     * @return
     */
    public static HyperStatControlsMessage_t.Builder getControlMessage(int address) {
        HashMap device = HyperStatControlUtilKt.getHyperStatDevice(address);
        return getHyperStatControlMessage(device);
    }

    public static HyperStatControlsMessage_t getHyperstatRebootControl(int address){
        CcuLog.d(L.TAG_CCU_SERIAL,"Reset set to true");
        return getControlMessage(address).setReset(true).build();
    }

}
