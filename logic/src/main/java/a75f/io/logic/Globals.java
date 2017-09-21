package a75f.io.logic;

import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.bo.building.CCUApplication;
import a75f.io.dal.DalContext;

import static a75f.io.logic.LLog.Logd;

/**
 * Created by rmatt isOn 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
class Globals
{
	
	private static final int      NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES = 10;
	private static final int      TASK_SEPERATION                           = 3;
	private static final TimeUnit TASK_SERERATION_TIMEUNIT                  = TimeUnit.SECONDS;
	private static Globals                  globals;
	HeartBeatJob        mHeartBeatJob        = new HeartBeatJob();
	MeshUpdateJob		mMeshUpdateJob		= new MeshUpdateJob();
	private        ScheduledExecutorService taskExecutor;
	private        Context                  mApplicationContext;
	private        CCUApplication           mCCUApplication;
	private        LZoneProfile             mLZoneProfile;
	private 	boolean isSimulation = false;
	private Globals()
	{
	}
	public CCUApplication ccu()
	{
		if (getInstance().mCCUApplication == null)
		{
			getInstance().mCCUApplication = LocalStorage.getApplicationSettings();
		}
		return getInstance().mCCUApplication;
	}
	public static Globals getInstance()
	{
		if (globals == null)
		{
			globals = new Globals();
		}
		return globals;
	}
	public Context getApplicationContext()
	{
		return mApplicationContext;
	}
	
	public void setApplicationContext(Context mApplicationContext)
	{
		if (this.mApplicationContext == null)
		{
			this.mApplicationContext = mApplicationContext;
			initilize();
		}
	}
	
	public void initilize()
	{
		taskExecutor = Executors.newScheduledThreadPool(NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES);
		DalContext.instantiate(this.mApplicationContext);
		//5 seconds after application initializes start heart beat
		int HEARTBEAT_INTERVAL = 60;
        Logd("Scheduling ---- HeartBeat Job");
		mHeartBeatJob.scheduleJob("Heartbeat Job", HEARTBEAT_INTERVAL, TASK_SEPERATION, TASK_SERERATION_TIMEUNIT);
        
        Logd("Scheduling ---- MeshUpdate Job");
        mMeshUpdateJob.scheduleJob("Mesh Update Job", HEARTBEAT_INTERVAL, TASK_SEPERATION * 2,
				TASK_SERERATION_TIMEUNIT);
		//5 seconds after heart beat initializes start profile scheduler.
		
        
        //Application in simulation mode notes
        //--Skips BLE
        //--Adds sleeps between any Serial Activity to work with biskit.
        isSimulation = getApplicationContext().getResources().getBoolean(R.bool.simulation);
        Logd("Simulation ----- " + isSimulation);
	}
	
	public ScheduledExecutorService getScheduledThreadPool()
	{
		return getInstance().taskExecutor;
	}
	
	public LZoneProfile getLZoneProfile()
	{
		if (getInstance().mLZoneProfile == null)
		{
			getInstance().mLZoneProfile = new LZoneProfile();
		}
		return getInstance().mLZoneProfile;
	}
	
	
	public boolean isSimulation()
	{
		return isSimulation;
	}
}
