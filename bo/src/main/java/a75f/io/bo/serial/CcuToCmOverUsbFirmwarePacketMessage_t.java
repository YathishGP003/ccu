package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan on 8/2/17.
 */

public class CcuToCmOverUsbFirmwarePacketMessage_t extends Struct {

    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());

    public final Unsigned16 lwMeshAddress = new Unsigned16();

    public final Unsigned16 sequenceNumber = new Unsigned16();

    public final Unsigned8[] packet = array(new Unsigned8[MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE]);

    @Override
    public ByteOrder byteOrder() {  return ByteOrder.LITTLE_ENDIAN;
    }

}
