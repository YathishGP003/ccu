package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class WrmPairingDataMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 wrmAddress = new Unsigned16();
	
	public final Unsigned8[] encryptionKey = array(new Unsigned8[MessageConstants.APP_KEY_LENGTH]);
	
	public final WrmSettings_t settings = inner(new WrmSettings_t());
	
	public final DaySchedule_t[] weeklySchedule = array(new DaySchedule_t[7]);
	
	public final ScheduleStatus_t status = inner(new ScheduleStatus_t());
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
