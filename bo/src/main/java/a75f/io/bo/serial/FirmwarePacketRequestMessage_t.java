package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class FirmwarePacketRequestMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 lwMeshAddress = new Unsigned16();
	
	public final Unsigned16 sequenceNumber = new Unsigned16();
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
