package a75f.io.logic.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import a75f.io.bo.building.definitions.MockTime;
import a75f.io.bo.building.definitions.ScheduledItem;
import a75f.io.logic.LZoneProfile;

/**
 * Created by Yinten on 9/10/2017.
 */

public class LScheduler
{
	
	private static final int LSCHEDULER_REQUEST_CODE = LScheduler.class.hashCode();
	private static final int NO_FLAGS                = 0;
	Map<UUID, ScheduledItem> mScheduledItems =
			Collections.synchronizedSortedMap(new TreeMap<UUID, ScheduledItem>());
	private AlarmManager  mAlarmMgr;
	private PendingIntent mAlarmIntent;
	private ScheduledItem mCurrentScheduledItem;
	
	
	public LScheduler(Context context)
	{
		mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, ScheduleAlarmReciever.class);
		mAlarmIntent =
				PendingIntent.getBroadcast(context, LSCHEDULER_REQUEST_CODE, intent, NO_FLAGS);
	}
	
	
	public void add(ScheduledItem scheduledItem)
	{
		/*The list contains the key */
		if (mScheduledItems.containsKey(scheduledItem.mUuid))
		{
			ScheduledItem alreadyAddedScheduledItem = mScheduledItems.get(scheduledItem.mUuid);
			
			/*The list contains the key and the item*/
			if (alreadyAddedScheduledItem.equals(scheduledItem))
			{
				return;
			}
			else // The item was previously added, but needs to be updated.
			{
				mScheduledItems.put(scheduledItem.mUuid, scheduledItem);
				checkFront();
			}
		}
		else // The item hasn't been added, add it to the hashmap and check front.
		{
			mScheduledItems.put(scheduledItem.mUuid, scheduledItem);
			checkFront();
		}
	}
	
	
	/* This method will check the front of the queue to see if it is the currently selected item.
	   If it isn't the currently selected, it will cancel the current alarm and add the currently
	    selected item as the alarm. */
	private void checkFront()
	{
		/* No items scheduled, abandon ship. */
		if (mScheduledItems.isEmpty())
		{
			return;
		}
		
		/* Returns a map of entries sorted by timestamp. */
		SortedSet<Map.Entry<UUID, ScheduledItem>> entries =
				mapOfEntriesSortedByTimeStamp(mScheduledItems);
		
		
		/* The item first in the list, the lowest timestamp */
		Map.Entry<UUID, ScheduledItem> first = entries.first();
		ScheduledItem frontValue = first.getValue();
		if (mCurrentScheduledItem == null)
		{
			schedule(frontValue);
		}
		else if (mCurrentScheduledItem != null && !mCurrentScheduledItem.equals(frontValue))
		{
			schedule(frontValue);
		}
	}
	
	
	static SortedSet<Map.Entry<UUID, ScheduledItem>> mapOfEntriesSortedByTimeStamp(Map<UUID, ScheduledItem> map)
	{
		SortedSet<Map.Entry<UUID, ScheduledItem>> sortedEntries = new TreeSet<>(new Comparator<Map.Entry<UUID, ScheduledItem>>() {
            @Override
            public int compare(Map.Entry<UUID, ScheduledItem> o1, Map.Entry<UUID, ScheduledItem> o2) {
                return o1.getValue().mTimeStamp.compareTo(o2.getValue().mTimeStamp);
            }
        });
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}
	
	
	/* This method unschedules the current alarm and schedules the first selected item */
	private void schedule(ScheduledItem itemToSchedule)
	{
		//cancel the previous alarm
		mAlarmMgr.cancel(mAlarmIntent);
		//set new scheduled item to front
		mCurrentScheduledItem = itemToSchedule;
		//set time to mock time or system time if there is no mock time.
		mAlarmMgr.setTime(MockTime.getInstance().getMockTime());
		//schedule item
		mAlarmMgr.set(AlarmManager.RTC, itemToSchedule.mTimeStamp.getMillis(), mAlarmIntent);

	}
	
	
	public class ScheduleAlarmReciever extends BroadcastReceiver
	{
		
		@Override
		public void onReceive(Context context, Intent intent)
		{
			//Remove mCurrentScheduledItem, Notify LZoneProfile.
			SortedSet<Map.Entry<UUID, ScheduledItem>> entries =
					mapOfEntriesSortedByTimeStamp(mScheduledItems);
			ArrayList<ScheduledItem> removedScheduledItems = new ArrayList<>();
			while (!entries.isEmpty() &&
			       entries.first().getValue().mTimeStamp == mCurrentScheduledItem.mTimeStamp)
			{
				removedScheduledItems.add(mScheduledItems.remove(entries.first().getValue()
						                                                 .mUuid));
				LZoneProfile.handleZoneProfileScheduledEvent(removedScheduledItems);
			}
			mCurrentScheduledItem = null;
			checkFront();
		}
	}
}
	

