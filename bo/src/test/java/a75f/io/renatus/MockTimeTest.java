package a75f.io.renatus;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import a75f.io.bo.building.definitions.MockTime;

/**
 * Created by Yinten isOn 9/8/2017.
 */

public class MockTimeTest
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
	
	
	
	@Before
	public void setUpMockTime()
	{
		MockTime.getInstance().setMockTime(true, MOCK_TIME);
	}
	
	
	@Test
	public void getMockTimeTest()
	{
		Assert.assertEquals(MockTime.getInstance().getMockTime(), MOCK_TIME);
		
		DateTime mockedTime = new DateTime(MockTime.getInstance().getMockTime());
		Assert.assertEquals(15, mockedTime.hourOfDay().get());
		Assert.assertEquals(8, mockedTime.minuteOfHour().get());
		
		
		
	}
	
	
	@Test
	public void getUnMockedTimeTest()
	{
		MockTime.getInstance().setMockTime(false, 0);
		Assert.assertNotSame(MOCK_TIME, MockTime.getInstance().getMockTime());
		
		//Return test to initial state
		MockTime.getInstance().setMockTime(true, MOCK_TIME);
	}
}
