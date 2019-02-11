package a75f.io.device.serial;

import org.javolution.io.Struct;
import org.javolution.io.Struct.Enum8;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class SnToCmOverAirSnRegularUpdateMessage_t extends Struct
{

	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());

	public final SNRegularUpdateMessage_t updateMessage = inner(new SNRegularUpdateMessage_t());

	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
