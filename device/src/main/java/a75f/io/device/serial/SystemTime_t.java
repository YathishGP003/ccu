package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by ryanmattison isOn 7/25/17.
 */

public class SystemTime_t extends Struct
{

	public final Enum8<DayOfWeek_t> day = new Enum8<>(DayOfWeek_t.values());
	public final Unsigned8 hours   = new Unsigned8();
	public final Unsigned8 minutes = new Unsigned8();
}
