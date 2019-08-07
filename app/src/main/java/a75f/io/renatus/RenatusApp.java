package a75f.io.renatus;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by ryanmattison isOn 7/24/17.
 */

public class RenatusApp extends UtilityApplication
{

	private static final String    TAG           = RenatusApp.class.getSimpleName();
	static Context mContext = null;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mContext = getApplicationContext();
		//Fabric.with(this, new Crashlytics());
	}

	public static Context getAppContext() {
		return mContext;
	}

	@Override
	protected void attachBaseContext(Context base)
	{

		super.attachBaseContext(base);
		MultiDex.install(this);
	}
}
