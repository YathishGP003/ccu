package a75f.io.logic;

import android.app.AlarmManager;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Floor;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.LScheduleAction;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.ScheduledItem;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.logic.cache.Globals;
import a75f.io.logic.scheduler.LScheduler;

import static java.lang.Thread.sleep;

/**
 * Created by samjithsadasivan on 9/11/17.
 */
@RunWith(AndroidJUnit4.class)
public class SchedulerTest
{
	Context                       context ;
	LScheduler                    mLScheduler;
	CCUApplication                mCcuApplication;
	SchedulerTest.TestZoneProfile mProfile;
	
	class TestZoneProfile extends ZoneProfile
	{
		TestZoneProfile() {
			super();
		}
		@Override
		public List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage()
		{
			return null;
		}
	}
	
	@Before
	public void setup() {
		context = InstrumentationRegistry.getTargetContext().getApplicationContext();
		Globals.getInstance().setApplicationContext(context);
		mLScheduler = Globals.getInstance().getLScheduler();
		mCcuApplication = new CCUApplication();
		SmartNode testSN = new SmartNode();
		testSN.mAddress = 7000;
		//testSN.mRoomName = "75F";
		mCcuApplication.smartNodes.add(testSN);
		mCcuApplication.CCUTitle = "Scheduler Test";
		Floor floor = new Floor(1, "webid", "Floor1");
		floor.mRoomList.add(new Zone("75FRoom1"));
		mProfile = new SchedulerTest.TestZoneProfile() ;
		mCcuApplication.floors.add(floor);
		//mCcuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(mProfile);
		SmartNodeOutput op1 = new SmartNodeOutput();
		op1.mSmartNodeAddress = testSN.mAddress;
		UUID op1UD = UUID.randomUUID();
		//op1.mUniqueID = op1UD;
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		//op1.mOutput = Output.Analog;
		op1.mName = "Dining Room";
		mProfile.smartNodeOutputs.add(op1);
		
		SmartNodeOutput op2 = new SmartNodeOutput();
		op2.mSmartNodeAddress = testSN.mAddress;
		UUID op2UD = UUID.randomUUID();
		//op2.mUniqueID = op2UD;
		op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		//op2.mOutput = Output.Relay;
		op2.mName = "Kitchen";
		mProfile.smartNodeOutputs.add(op2);
	}
	
	@After
	public void tearDown() {
		mLScheduler.getScheduledItems().clear();
	}
	
	@Test
	public void testAddExpiredSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new DateTime(System.currentTimeMillis() - 60000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		mLScheduler.add(item1);//should fire immediately or ignore the schedule
		threadSleep(2);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(0, entries.size());
		
	}
	@Test
	public void testAddFutureSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new DateTime(System.currentTimeMillis() + 60000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(1, entries.size());
		
	}
	
	@Test
	public void testAddInvalidSchedule() {
		ScheduledItem item = new ScheduledItem();
		mLScheduler.add(item);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(0, entries.size());
	}
	
	@Test
	public void testAddDuplicateSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new DateTime(System.currentTimeMillis() + 10000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		ScheduledItem item2 = new ScheduledItem();
		item2.mUuid = mProfile.uuid;
		item2.mTimeStamp = new DateTime(System.currentTimeMillis() + 20000);
		item2.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item2);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(1, entries.size());
	}
	
	@Test
	public void testAddMultipleSchedules() {
		ScheduledItem item = new ScheduledItem();
		
		for (int cnt = 0; cnt < 3; cnt++)
		{
			item.mTimeStamp = new DateTime(System.currentTimeMillis()+60000);
			item.mUuid = UUID.randomUUID();
			item.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
			mLScheduler.add(item);
		}
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(3, entries.size());
	}
	
	@Test
	public void testRemoveSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new DateTime(System.currentTimeMillis() + 60000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(1, entries.size());
		
		mLScheduler.removeSchedule(item1);
		entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(0, entries.size());
		
	}
	
	@Test
	public void testScheduleExpiredAfter10s() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new DateTime(System.currentTimeMillis() +10000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(1, entries.size());
		threadSleep(10);
		entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(0, entries.size());
	}
	
	@Test
	public void test100SchedulesExpiringEvery2Second() {
		ScheduledItem item = new ScheduledItem();
		long time = System.currentTimeMillis();
		
		for (int cnt = 1; cnt <= 3; cnt++)
		{
			item.mTimeStamp = new DateTime(System.currentTimeMillis()+ 10000 *  cnt);
			//item.mTimeStamp.plusSeconds(10);
			item.mUuid = UUID.randomUUID();
			item.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
			mLScheduler.add(item);
		}
		Assert.assertEquals(3, mLScheduler.getScheduledItems().size());
		threadSleep(30);
		Assert.assertEquals(1, mLScheduler.getScheduledItems().size());
		threadSleep(15);
		Assert.assertEquals(0, mLScheduler.getScheduledItems().size());
		
		
		/*Assert.assertEquals(100, mLScheduler.getScheduledItems().size());
		threadSleep(50);
		Assert.assertEquals(75, mLScheduler.getScheduledItems().size());
		threadSleep(50);
		Assert.assertEquals(50, mLScheduler.getScheduledItems().size());
		threadSleep(50);
		Assert.assertEquals(25, mLScheduler.getScheduledItems().size());
		threadSleep(50);
		Assert.assertEquals(0, mLScheduler.getScheduledItems().size());*/
		
	}
	
	private void threadSleep(int seconds) {
		try
		{
			sleep(seconds * 1000);
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
