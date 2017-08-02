package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan on 8/2/17.
 */

public class CcuToCmOverUsbSnSettingsMessage_t extends Struct {

    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());


    public final Unsigned16 smartNodeAddress = new Unsigned16();

    public final SmartNodeSettings_t settings = inner(new SmartNodeSettings_t());

    @Override
    public ByteOrder byteOrder() {  return ByteOrder.LITTLE_ENDIAN;
    }

}
