package a75f.io.device.serial;

import org.javolution.io.Struct;
import org.javolution.io.Struct.Unsigned16;

import java.nio.ByteOrder;

/**
 * Created by James Liu on 1/18/18.
 */

public class SnToCmOverAirSnLocalControlsOverrideMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 address = new Unsigned16();
	
	public final Unsigned8 setTemperature = new Unsigned8();
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}