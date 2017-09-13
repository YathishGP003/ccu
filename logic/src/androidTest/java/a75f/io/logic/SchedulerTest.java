package a75f.io.logic;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

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
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.LScheduleAction;
import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.ScheduledItem;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.logic.scheduler.LScheduler;

import static java.lang.Thread.sleep;

/**
 * Created by samjithsadasivan on 9/11/17.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
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
			mModuleName = "Test";
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
		mLScheduler = new LScheduler(context);
		mCcuApplication = new CCUApplication();
		SmartNode testSN = new SmartNode();
		testSN.mAddress = 7000;
		testSN.mRoomName = "75F";
		mCcuApplication.smartNodes.add(testSN);
		mCcuApplication.CCUTitle = "Scheduler Test";
		Floor floor = new Floor(1, "webid", "Floor1");
		floor.mRoomList.add(new Zone("75FRoom1"));
		mProfile = new SchedulerTest.TestZoneProfile() ;
		mCcuApplication.floors.add(floor);
		mCcuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(mProfile);
		SmartNodeOutput op1 = new SmartNodeOutput();
		op1.mSmartNodeAddress = testSN.mAddress;
		UUID op1UD = UUID.randomUUID();
		op1.mUniqueID = op1UD;
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op1.mOutput = Output.Analog;
		op1.mName = "Dining Room";
		mProfile.smartNodeOutputs.add(op1);
		
		SmartNodeOutput op2 = new SmartNodeOutput();
		op2.mSmartNodeAddress = testSN.mAddress;
		UUID op2UD = UUID.randomUUID();
		op2.mUniqueID = op2UD;
		op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op2.mOutput = Output.Relay;
		op2.mName = "Kitchen";
		mProfile.smartNodeOutputs.add(op2);
	}
	
	@After
	public void tearDown() {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(mLScheduler.getAlarmIntent());
	}
	
	@Test
	public void testAddExpiredSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.mTimeStamp.minus(6000000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(0, entries.size());
		
	}
	@Test
	public void testAddFutureSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.mTimeStamp.plus(6000000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertNotNull(entries.get(item1.mUuid));
		
	}
	
	@Test
	public void testAddDuplicateSchedule() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		ScheduledItem item2 = new ScheduledItem();
		item2.mUuid = mProfile.uuid;
		item2.mTimeStamp = new LocalDateTime().toDateTime();
		item2.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item2);
		Map<UUID, ScheduledItem> entries = mLScheduler.getScheduledItems();
		Assert.assertEquals(1, entries.size());
	}
	
	@Test
	public void testScheduleFromPendingIntent() {
		
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.mTimeStamp.plus(60000*60);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		//AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, mLScheduler.receiverIntent, 0);
		//am.cancel(pi);
		Assert.assertNotNull(pi);
		
	}
	
	@Test
	public void testCancelSchedule() {
		
	}
	
	@Test
	public void testScheduleExpiredAfterMinute() {
		ScheduledItem item1 = new ScheduledItem();
		item1.mUuid = mProfile.uuid;
		item1.mTimeStamp = new LocalDateTime().toDateTime();
		item1.mTimeStamp.plus(60000);
		item1.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
		
		mLScheduler.add(item1);
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, mLScheduler.receiverIntent, 0);
		Assert.assertNotNull(pi);
		try
		{
			sleep(60000);
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
		PendingIntent piExpired = PendingIntent.getBroadcast(context, 0, mLScheduler.receiverIntent, 0);
		Assert.assertNull(piExpired);
	}
	
	
}
