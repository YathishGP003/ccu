package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by Brady Anderson on 07/25/2019
 */

public class CmToCcuOverUsbFirmwareUpdateAckMessage_t extends Struct {

    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());

    public final Unsigned16 lwMeshAddress = new Unsigned16();

    public ByteOrder byteOrder() { return ByteOrder.LITTLE_ENDIAN; }
}
