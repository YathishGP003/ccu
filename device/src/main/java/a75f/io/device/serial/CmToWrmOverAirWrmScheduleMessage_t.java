package a75f.io.device.serial;

import java.nio.ByteOrder;

import org.javolution.io.Struct;

/**
 * Based on
 * https://gitlab.com/75f/firmware/cm3/blob/master/cm3/includes/protocols_wrm.h
 * 
 * @author Yinten
 *
 */
public class CmToWrmOverAirWrmScheduleMessage_t extends Struct {

	public final DaySchedule_t[] weeklySchedule = array(new DaySchedule_t[7]);

	public final ScheduleStatus_t status = inner(new ScheduleStatus_t());

	@Override
	public ByteOrder byteOrder() {
		return ByteOrder.LITTLE_ENDIAN;
	}

}
