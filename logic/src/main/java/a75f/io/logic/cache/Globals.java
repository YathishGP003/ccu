package a75f.io.logic.cache;

import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import a75f.io.bo.building.CCUApplication;
import a75f.io.logic.cache.prefs.LocalStorage;
import a75f.io.logic.scheduler.LScheduler;

/**
 * Created by rmatt isOn 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
public class Globals
{
	
	private static final int NUMBER_OF_TASKS_RENATUS_REQUIRES = 10;
	private static Globals                  globals;
	private        ScheduledExecutorService scheduleTaskExecutor;
	private        Context                  mApplicationContext;
	private        CCUApplication           mCCUApplication;
	private        LScheduler               mLScheduler;
	
	
	private Globals()
	{
	}
	
	
	public static Globals getInstance()
	{
		if (globals == null)
		{
			globals = new Globals();
		}
		return globals;
	}
	
	
	public CCUApplication getCCUApplication()
	{
		if (mCCUApplication == null)
		{
			mCCUApplication = LocalStorage.getApplicationSettings();
		}
		return mCCUApplication;
	}
	
	
	public LScheduler getLScheduler()
	{
		return mLScheduler;
	}	public Context getApplicationContext()
	{
		return mApplicationContext;
	}
	
	
	public ScheduledExecutorService getScheduledThreadPool()
	{
		return scheduleTaskExecutor;
	}
	
	public void setApplicationContext(Context mApplicationContext)
	{
		if (this.mApplicationContext == null)
		{
			this.mApplicationContext = mApplicationContext;
			initilize();
		}
	}
	
	
	private void initilize()
	{
		scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
		mLScheduler = new LScheduler();
	}
	
	

	
	

}
