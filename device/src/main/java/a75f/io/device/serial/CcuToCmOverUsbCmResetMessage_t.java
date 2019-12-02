package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by mahesh on 02-12-2019.
 */
public class CcuToCmOverUsbCmResetMessage_t extends Struct {

    public final Struct.Enum8<MessageType> messageType = new Struct.Enum8<>(MessageType.values());
    public final Struct.Unsigned8 reset = new Struct.Unsigned8(1);
    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
