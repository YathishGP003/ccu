package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class CmToCcuOverUsbSnRegularUpdateMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final SnToCmOverAirSnRegularUpdateMessage_t update = inner(new SnToCmOverAirSnRegularUpdateMessage_t());
	
	public final Unsigned8 cmLqi  = new Unsigned8(); /* LQI of this received data packet @ CM */
	public final Signed8   cmRssi = new Signed8(); /* RSSI of this received data packet @ CM */
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
