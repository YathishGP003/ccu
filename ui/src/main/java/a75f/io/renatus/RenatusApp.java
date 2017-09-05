package a75f.io.renatus;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.JobManager;

import a75f.io.bo.SmartNode;
import a75f.io.logic.LogicBLL;
import a75f.io.logic.jobs.CCUJobCreator;
import a75f.io.logic.jobs.HeartBeatJob;
import a75f.io.util.UtilityApplication;
import io.fabric.sdk.android.Fabric;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class RenatusApp extends UtilityApplication
{
	
	private static final String    TAG           = RenatusApp.class.getSimpleName();
	public               boolean   isProvisioned = false;
	public               SmartNode mSmartNode    = null;
	
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		Fabric.with(this, new Crashlytics());
		LogicBLL.initializeKinvey(this);
		JobManager.create(this).addJobCreator(new CCUJobCreator());
		HeartBeatJob.scheduleJob();

		Log.i(TAG, "RENATUS APP INITIATED");
	}
	
	

	
	
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}
}
