package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class CcuToCmOverUsbCcuHeartbeatMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned8 interval = new Unsigned8(); // default 1 - minutes between expected heartbeats
	
	public final Unsigned8 multiplier = new Unsigned8(); // default 5 - number of heartbeats to miss before before going to failsafe
	
	public final Signed8 temperatureOffset = new Signed8(); // default 0 - in 1/10 deg F. This is added to the measured temp of the CM temp sensor.@Override
	
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}

}
