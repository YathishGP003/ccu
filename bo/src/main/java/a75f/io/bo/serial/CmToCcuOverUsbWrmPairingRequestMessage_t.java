package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 7/31/17.
 */
// This request does not require any message content.
public class CmToCcuOverUsbWrmPairingRequestMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	/*The CCU is in Little_Endian, use [Struct].getByteBuffer().get(i) to write to CCU in LITTLE_ENDIAN */
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
