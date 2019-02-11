package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 7/31/17.
 */

public class CmToCcuOverUsbWrmRegularUpdateMessage_t extends Struct
{

	public final Enum8<MessageType>               messageType = new Enum8<>(MessageType.values());
	public final WrmToCmOverAirWrmRegularUpdateMessage_t update      = inner(new WrmToCmOverAirWrmRegularUpdateMessage_t());
	public final Unsigned8                               cmLqui      = new Unsigned8(); //lqi of this received data packet @CM
	public final Signed8                                 cmRssi      = new Signed8(); // rssi of this received data packet @ CM

	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
