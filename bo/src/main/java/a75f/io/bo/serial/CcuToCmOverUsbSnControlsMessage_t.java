package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class CcuToCmOverUsbSnControlsMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 smartNodeAddress = new Unsigned16();
	
	public final SmartNodeControls_t controls = inner(new SmartNodeControls_t());
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
