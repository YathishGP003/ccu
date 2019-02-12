package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/9/17.
 */

public class CcuToCmOverUsbSmartStatControlsMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	public final Struct.Unsigned16  address     = new Unsigned16();
	
	public final SmartStatControls_t controls = inner( new SmartStatControls_t());
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
