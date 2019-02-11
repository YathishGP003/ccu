package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class CmToSmartStatOverAirSmartStatSettingsMessage_t extends Struct
{
	public final Enum8<MessageType>  messageType = new Enum8<>(MessageType.values());
	public final SmartStatSettings_t settings    = inner(new SmartStatSettings_t());
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
