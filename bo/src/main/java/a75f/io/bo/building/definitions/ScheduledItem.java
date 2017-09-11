package a75f.io.bo.building.definitions;

import org.joda.time.DateTime;

import java.util.UUID;

/**
 * Created by Yinten on 9/10/2017.
 */

public class ScheduledItem
{
	public UUID            mUuid;
	public LScheduleAction lScheduleAction;
	public DateTime        mTimeStamp;
	
	@Override
	public String toString()
	{
		return "ScheduledItem{" + "mUuid=" + mUuid + ", lScheduleAction=" + lScheduleAction +
		       ", mTimeStamp=" + mTimeStamp + '}';
	}
}
