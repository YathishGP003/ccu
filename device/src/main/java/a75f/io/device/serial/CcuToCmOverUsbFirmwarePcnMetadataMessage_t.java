package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by pramod isOn 25/9/25.
 */

public class CcuToCmOverUsbFirmwarePcnMetadataMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());

	public final Unsigned16 lwMeshAddress = new Unsigned16();
	
	public final FirmwareMetdata_t metadata = inner(new FirmwareMetdata_t());

	public final Unsigned8 serverId = new Unsigned8(); /* The type of PCN module */
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
