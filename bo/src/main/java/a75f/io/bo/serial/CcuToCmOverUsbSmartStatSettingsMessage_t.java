package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan on 8/9/17.
 */

public class CcuToCmOverUsbSmartStatSettingsMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 address = new Unsigned16();
	
	public final SmartStatSettings_t settings = inner(new SmartStatSettings_t());
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
