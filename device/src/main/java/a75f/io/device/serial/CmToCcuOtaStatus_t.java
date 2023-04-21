package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by Manjunath K on 07-03-2023.
 */

public class CmToCcuOtaStatus_t extends Struct{
    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
    public final Unsigned8 currentState = new Unsigned8(3);

    public final Unsigned16 data = new Unsigned16();


    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
