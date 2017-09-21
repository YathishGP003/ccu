package a75f.io.renatus;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import a75f.io.logic.UtilityApplication;
import io.fabric.sdk.android.Fabric;

/**
 * Created by ryanmattison isOn 7/24/17.
 */

public class RenatusApp extends UtilityApplication
{

	private static final String    TAG           = RenatusApp.class.getSimpleName();
<<<<<<< HEAD
	public               boolean   isProvisioned = false;
	public               SmartNode mSmartNode    = null;


=======
	
	
>>>>>>> ryan
	@Override
	public void onCreate()
	{
		super.onCreate();
		Fabric.with(this, new Crashlytics());
		
		Log.i(TAG, "RENATUS APP INITIATED");
	}


	@Override
	protected void attachBaseContext(Context base)
	{
		 
		super.attachBaseContext(base);
		MultiDex.install(this);
	}
}
