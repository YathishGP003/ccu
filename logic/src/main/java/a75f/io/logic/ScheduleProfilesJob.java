package a75f.io.logic;

import android.util.Log;

import static com.kinvey.android.Client.TAG;

/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
class ScheduleProfilesJob extends BaseJob
{


	//This task should run every minute.
	protected void doJob()
	{
		if (LSerial.getInstance().isConnected())
		{
			try
			{
				Globals.getInstance().getLZoneProfile().scheduleProfiles();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			Log.d(TAG, "Serial is not connected, rescheduling profile scheduling");
		}
	}

}

	