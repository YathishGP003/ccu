package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by pramodhalliyavar isOn 8/12/2025.
 */

public class CcuToCmOverUsbPcnFirmwareEraseMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 lwMeshAddress = new Unsigned16();

	public final Unsigned8 serverId = new Unsigned8(); /* The type of PCN module */

	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
