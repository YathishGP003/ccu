package a75f.io.logic;

import android.test.InstrumentationTestCase;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;

import a75f.io.bo.building.definitions.LScheduleAction;
import a75f.io.bo.building.definitions.ScheduledItem;
import a75f.io.logic.scheduler.LScheduler;

/**
 * Created by samjithsadasivan on 9/11/17.
 */

public class SchedulerTest extends InstrumentationTestCase
{
	
	private LScheduler mLScheduler;
	
	protected void setup() {
		mLScheduler = new LScheduler(this.getInstrumentation().getTargetContext().getApplicationContext());
	}
	
	@Test
	public void testAddExpiredSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = UUID.randomUUID();
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.mTimeStamp.minus(6000000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(entries.size() ,0);
				
	}
	
	@Test
	public void testAddFutureSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = UUID.randomUUID();
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.mTimeStamp.plus(6000000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(entries.size() ,1);
		
	}
	
	@Test
	public void testAddDuplicateSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = UUID.randomUUID();
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		ScheduledItem item2 = new ScheduledItem();
		item2.mUuid =item1.mUuid;
		item2.mTimeStamp = new LocalDateTime().toDateTime();
		item2.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item2);
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		ScheduledItem orig = (ScheduledItem) entries.get(item1);
		ScheduledItem dup = (ScheduledItem) entries.get(item1);
		Assert.assertNotNull(orig);
		Assert.assertNull(dup);
	}
	
	@Test
	public void testOneValidSchedule() {
		
	}
}
