package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by pramod isOn 22-09-2025.
 */

public class CmToCcuOverUsbSnDiagonasticUpdateMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	public final SnToCmOverAirSnDiagonasticUpdateMessage_t update = inner(new SnToCmOverAirSnDiagonasticUpdateMessage_t());
	public final Unsigned8 cmLqi  = new Unsigned8(); /* LQI of this received data packet @ CM */
	public final Signed8   cmRssi = new Signed8(); /* RSSI of this received data packet @ CM */
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
