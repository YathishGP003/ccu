package a75f.io.device.mesh;


import a75f.io.api.haystack.Zone;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedCnMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public class LConnectNode
{
    public static CcuToCmOverUsbDatabaseSeedCnMessage_t getSeedMessage(Zone zone, short address, String equipRef, String profile)
    {
        CcuToCmOverUsbDatabaseSeedCnMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedCnMessage_t();
        seedMessage.messageType.set(MessageType.CONNECT_CCU_DATABASE_SEED_MESSAGE);
        seedMessage.smartNodeAddress.set(address);
        seedMessage.putEncrptionKey(L.getEncryptionKey());
        return seedMessage;
    }
}
