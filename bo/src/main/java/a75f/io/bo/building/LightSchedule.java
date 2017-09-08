package a75f.io.bo.building;

import a75f.io.bo.building.definitions.ScheduleMode;
import a75f.io.bo.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;

/**
 * Created by samjithsadasivan on 9/7/17.
 */

public class LightSchedule extends Schedule
{
	public ScheduleMode mode;
	
	public int startTime;
	
	public int endTime;
	
	public int intensityVal;
	
	public short getScheduleDaysBitmap() {
		return 1;//TODO - retrieve from Schedule
	}
	
}
