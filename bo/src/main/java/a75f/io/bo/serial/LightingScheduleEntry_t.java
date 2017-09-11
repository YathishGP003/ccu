package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class LightingScheduleEntry_t extends Struct
{
	
	public final LightingScheduleDays_t applicableDaysOfTheWeek = inner(new LightingScheduleDays_t());
	
	public final Unsigned8 startTime = new Unsigned8(); /* 15 minute increments from midnight */
	
	public final Unsigned8 stopTime = new Unsigned8(); /* 15 minute increments from midnight */
	
	public final Unsigned8 intensityPercent = new Unsigned8();
}
