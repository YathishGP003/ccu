package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class SmartNodeLightingSchedule_t extends Struct
{
	
	public final UTF8String logicalName = new UTF8String(MessageConstants.MAX_LIGHTING_CONTROL_CIRCUIT_LOGICAL_NAME_BYTES);
	
	public final Unsigned8 normallyOpen = new Unsigned8(); /* 0 = No (Normally Closed) and 1 = Yes (Normally Open) */
	
	public final Unsigned8 numEntries = new Unsigned8();
	
	public final LightingScheduleEntry_t[] entries = array(new LightingScheduleEntry_t[MessageConstants.MAX_LIGHTING_CONTROL_CIRCUIT_SCHEDULE_ENTRIES]);
}
