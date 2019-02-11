package a75f.io.device.serial;

import org.javolution.io.Struct;
import org.javolution.io.Struct.Enum4;
import org.javolution.io.Struct.Unsigned8;

import java.nio.ByteOrder;

/**
 * Created by James Liu on 1/12/18.
 */

public class SmartStatToCmOverAirSmartStatLocalControlsOverrideMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned8 setTemperature = new Unsigned8();
	
	public final Enum4<SmartStatFanSpeed_t> fanSpeed = new Enum4<>(SmartStatFanSpeed_t.values());
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}