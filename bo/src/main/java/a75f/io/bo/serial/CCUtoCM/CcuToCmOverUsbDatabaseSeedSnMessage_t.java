package a75f.io.bo.serial.CCUtoCM;

import javolution.io.Struct;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class CcuToCmOverUsbDatabaseSeedSnMessage_t extends Struct {
    public final Unsigned16 smartNodeAddress = new Unsigned16();
    public final BitField encryptionKey = new BitField(128);
    public final SmartNodeSettings_t settings = inner(new SmartNodeSettings_t());
    public final SmartNodeControls_t controls = inner(new SmartNodeControls_t());
}
