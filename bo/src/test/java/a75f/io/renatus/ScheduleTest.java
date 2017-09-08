package a75f.io.renatus;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import a75f.io.bo.building.Schedule;
import a75f.io.bo.building.definitions.MockTime;

/**
 * Created by Yinten on 9/8/2017.
 */

public class ScheduleTest
{
	
	
	/*
	local date
	08 Sep 2017

	UTC time
	20:08:59

	local time
	15:08:59

	UNIX time

	1504901339999

 

	local timezone (UTC-5h)
	Central Daylight
	 */
	
	private static final long MOCK_TIME = 1504901339999L;
	
	
	
	/*
	
	+++x +++++++++
	FRI
	2017 Sep 08 23:22:00 UTC
	FRI
	2017 Sep 08 18:22:00 UTC-5:00
	----  -  -- -- -- --
	*/
	
	private static final long OUT_OF_SCHEDULE_MOCK_TIME = 1504912920000L;
	
	private Schedule schedule;
	
	
	@Before
	public void setUpMockSchedule()
	{
		//Mock schedule M-F, 8AM - 5:30PM turn on lights to value 100.
		schedule = new Schedule();
		schedule.setDays(new int[]{1, 2, 3, 4, 5});
		schedule.setDefaultVal(0);
		schedule.setSt("8:00");
		schedule.setEt("17:30");
		schedule.setVal(100);
	}
	
	
	@Test
	public void getMockScheduleValidTest()
	{
		//Set Mock Time to 08 Sep 2017 @ 15:08:59.
		//MockTime.getInstance().setMockTime(true, MOCK_TIME);
		MockTime.getInstance().setMockTime(true, MOCK_TIME);
		//Assert that this is in fact the mock time
		Assert.assertEquals(MockTime.getInstance().getMockTime(), MOCK_TIME);
		Assert.assertTrue(schedule.isInSchedule());
	}
	
	
	@Test
	public void getMockScheduleInvalidTest()
	{
		//Set Mock Time to 08 Sep 2017 @ 15:08:59.
		//MockTime.getInstance().setMockTime(true, MOCK_TIME);
		MockTime.getInstance().setMockTime(true, MOCK_TIME);
		Assert.assertTrue(schedule.isInSchedule());
	}
	
	
	@Test
	public void getUnMockedTimeTest()
	{
		//2017 Sep 08 18:22:00 UTC-5:00
		MockTime.getInstance().setMockTime(true, OUT_OF_SCHEDULE_MOCK_TIME);
		//Assert that the mocktime changed.
		Assert.assertNotSame(MOCK_TIME, MockTime.getInstance().getMockTime());
		boolean scheduled = schedule.isInSchedule();
		//This should be false and out of the mock time.
		Assert.assertFalse(scheduled);
	}
}

