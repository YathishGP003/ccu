package a75f.io.logic;

import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yinten on 9/10/2017.
 */

public class LScheduler
{
	private static final String TAG = "Schedule";
	
	Map<UUID, ScheduledItem>                  mScheduledItems = Collections.synchronizedSortedMap(new TreeMap<UUID, ScheduledItem>());
	SortedSet<Map.Entry<UUID, ScheduledItem>> sortedEntries   = new TreeSet<>(new Comparator<Map.Entry<UUID, ScheduledItem>>()
	{
		@Override
		public int compare(Map.Entry<UUID, ScheduledItem> o1, Map.Entry<UUID, ScheduledItem> o2)
		{
			return o1.getValue().mTimeStamp.compareTo(o2.getValue().mTimeStamp);
		}
	});
	private ScheduledItem   mCurrentScheduledItem;
	private IScheduleAction scheduleAction;
	private ScheduledExecutorService mScheduleExecutorService = Executors.newSingleThreadScheduledExecutor();
	
	public LScheduler(IScheduleAction scheduleAction)
	{
		this.scheduleAction = scheduleAction;
	}
	
	@WorkerThread
	public void add(ScheduledItem scheduledItem)
	{
		
		/*The list contains the key */
		Log.i("Schedule", "add - " + scheduledItem.toString());
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
	@WorkerThread
	private void checkFront()
	{
		/* No items scheduled, abandon ship. */
		if (mScheduledItems.isEmpty())
		{
			return;
		}
		
		/* Synchronize map of entries sorted by timestamp. */
		sortedEntries.addAll(mScheduledItems.entrySet());
		
		/* The item first in the list, the lowest timestamp */
		ScheduledItem frontValue = sortedEntries.first().getValue();
		if (mCurrentScheduledItem == null)
		{
			schedule(frontValue);
		}
		else if (mCurrentScheduledItem != null && !mCurrentScheduledItem.equals(frontValue))
		{
			schedule(frontValue);
		}
	}
	
	/* This method unschedules the current alarm and schedules the first selected item */
	private synchronized void schedule(ScheduledItem itemToSchedule)
	{
		//Logd("schedule for alarm: " + itemToSchedule.toString());
		long lengthUntilNextAlarm = itemToSchedule.mTimeStamp.getMillis() - System.currentTimeMillis();
		mScheduleExecutorService.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				takeAction();
			}
		}, lengthUntilNextAlarm, TimeUnit.MILLISECONDS);
		//set new scheduled item to front
		mCurrentScheduledItem = itemToSchedule;
		//Logd("Next Alarm: " + lengthUntilNextAlarm);
	}
	
	//For test
	public Map<UUID, ScheduledItem> getScheduledItems()
	{
		return mScheduledItems;
	}
	
	public synchronized void takeAction()
	{
		executeCurrentTimeStamps();
		mCurrentScheduledItem = null;
		checkFront();
	}
	private void executeCurrentTimeStamps()
	{
		while (sortedEntries.first() != null && sortedEntries.first().getValue().mTimeStamp.getMillis() <= mCurrentScheduledItem.mTimeStamp.getMillis())
		{
			if (scheduleAction != null)
			{
				scheduleAction.takeAction(mCurrentScheduledItem);
			}
			sortedEntries.remove(sortedEntries.first());
		}
	}
}
	

