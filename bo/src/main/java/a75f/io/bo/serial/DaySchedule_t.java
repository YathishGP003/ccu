package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan on 8/1/17.
 */

public class DaySchedule_t extends Struct
{
	public final Unsigned8 day = new Unsigned8();
	
	public final Unsigned8 coolOccutime = new Unsigned8(); // time in 15 min intervals since midnight. e.g 01:30 is 6 and 18:00 is 72
	
	public final Unsigned8 coolOccutemp = new Unsigned8(); // temp in 2x F
	
	public final Unsigned8 coolUnoccutime = new Unsigned8();
	
	public final Unsigned8 coolUnoccutemp = new Unsigned8();
	
	public final Unsigned8 heatOccutime = new Unsigned8();
	
	public final Unsigned8 heatOccutemp = new Unsigned8();
	
	public final Unsigned8 heatUnoccutime = new Unsigned8();
	
	public final Unsigned8 heatUnoccutemp = new Unsigned8();
}
