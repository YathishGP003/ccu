package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class CcuToCmOverUsbSnLightingScheduleMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 smartNodeAddress = new Unsigned16();
	
	public final Enum8<SmartNodeLightingCircuit_t> circuit = new Enum8<>(SmartNodeLightingCircuit_t.values());
	
	public final SmartNodeLightingSchedule_t lightingSchedule = inner(new SmartNodeLightingSchedule_t());
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
