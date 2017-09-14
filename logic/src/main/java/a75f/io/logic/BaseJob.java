package a75f.io.logic;

import java.util.concurrent.TimeUnit;

/**
 * Created by Yinten on 9/14/2017.
 */

abstract class BaseJob
{
	protected void scheduleJob(int interval, int taskSeperation, TimeUnit unit)
	{
		// This task runs every minute.
		Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				doJob();
			}
		}, taskSeperation, interval, unit);
	}
	
	protected  abstract void doJob();
}
