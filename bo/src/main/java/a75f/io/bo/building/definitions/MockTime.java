package a75f.io.bo.building.definitions;
/**
 * Created by Yinten isOn 9/8/2017.
 */

/**
 * This is a mock time that can be set to inject in mockable times to test what would happen to
 * the state at a future date and time.  If no mock time is set, the system will use System
 * .getCurrentTimeMillis();
 */
public class MockTime
{
	private static MockTime instance     = null;
	private        boolean  isMockedTime = false;
	private        long     mockTime     = 0;
	
	
	public static MockTime getInstance()
	{
		if (instance == null)
		{
			instance = new MockTime();
		}
		return instance;
	}
	
	
	public void setMockTime(boolean isMockedTime, long mockTime)
	{
		this.isMockedTime = isMockedTime;
		this.mockTime = mockTime;
	}
	
	
	public long getMockTime()
	{
		if (isMockTime())
		{
			return mockTime;
		}
		else
		{
			return System.currentTimeMillis();
		}
	}
	
	
	private boolean isMockTime()
	{
		return isMockedTime;
	}
}
