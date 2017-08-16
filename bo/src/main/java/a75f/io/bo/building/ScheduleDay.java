package a75f.io.bo.building;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yinten on 8/15/2017.
 */

public class ScheduleDay
{
	
	/* 0 - 7 days of the week */
	public int dayOfWeek;
	
	public List<ScheduleEvent> scheduleEvents = new ArrayList<ScheduleEvent>();
}
