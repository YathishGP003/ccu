package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class CcuToCmOverUsbWrmScheduleMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 wrmAddress = new Unsigned16();
	
	public final DaySchedule_t[] weeklySchedule = array(new DaySchedule_t[7]);
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
	
	public class Status extends Struct
	{
		
		public final Unsigned8 mode = new Unsigned8(2); // 00 - zone schedule, 01 - System Schedule, 10 - Hold
		
		public final Unsigned8 protectSchedule = new Unsigned8(1);// 1 - do not modify schedule even if set temp changed during occupancy. 0 -false
		
		public final Unsigned8 vacation = new Unsigned8(1); // 1 - true, 0 - false
		
		public final Unsigned8 reserved = new Unsigned8(4);//
	}
}
