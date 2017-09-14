package a75f.io.logic;

import org.joda.time.DateTime;

import java.util.UUID;

import a75f.io.bo.building.definitions.LScheduleAction;

/**
 * Created by Yinten on 9/10/2017.
 */

public class ScheduledItem
{
	public UUID            mUuid;
	public LScheduleAction lScheduleAction;
	public DateTime        mTimeStamp;
	public IScheduleAction iScheduleAction;
	@Override
	public String toString()
	{
		return "ScheduledItem{" + "mUuid=" + mUuid + ", lScheduleAction=" + lScheduleAction +
		       ", mTimeStamp=" + mTimeStamp + '}';
	}
}
