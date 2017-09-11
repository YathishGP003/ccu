package a75f.io.logic.scheduler;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import a75f.io.bo.building.definitions.ScheduledItem;

/**
 * Created by Yinten on 9/10/2017.
 */

public class ScheduledItemComparator implements Comparator<Map.Entry<UUID, ScheduledItem>>
{
	@Override
	public int compare(Map.Entry<UUID, ScheduledItem> siA, Map.Entry<UUID, ScheduledItem> siB)
	{
		return siA.getValue().mTimeStamp < siB.getValue().mTimeStamp ? -1 : siA.getValue().
				                                                                    mTimeStamp ==
		                                                                    siB.getValue().mTimeStamp
				                                                                    ? 0 : 1;
	}
}
