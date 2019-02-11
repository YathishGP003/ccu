package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by James Liu on 1/11/2018.
 */

public class CmToWrmOverAirWrmPairingResponseMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 wrmAddress = new Unsigned16();
	
	public final Unsigned8[] encryptionKey = array(new Unsigned8[MessageConstants.APP_KEY_LENGTH]);
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
