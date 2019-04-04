package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

public class CcuToCmOverUsbDeviceTempAckMessage_t extends Struct{
    public final Struct.Enum8<MessageType> messageType      = new Struct.Enum8<>(MessageType.values());
    public final Struct.Unsigned16 smartNodeAddress = new Struct.Unsigned16(); /* LW Mesh Address of the Smart Node sending the message */
    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
