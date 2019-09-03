package a75f.io.renatus;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
		AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
		Fabric.with(this, new Crashlytics());
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
	private static boolean isRooted() {
		return findBinary("su");
	}
	public static boolean findBinary(String binaryName) {
		boolean found = false;
		if (!found) {
			String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/","/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
			for (String where : places) {
				if ( new File( where + binaryName ).exists() ) {
					found = true;
					break;
				}
			}
		}
		return found;
	}
	public static void executeAsRoot(String[] commands) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Do the magic


					ApplicationInfo appInfo = RenatusApp.getAppContext().getApplicationInfo();
					Log.d("CCU_DOWNLOAD", "RenatusAPP ExecuteAsRoot===>"+isRooted()+","+(appInfo.flags & ApplicationInfo.FLAG_SYSTEM));
					if(isRooted()) {
						Process p = Runtime.getRuntime().exec("su");
						InputStream es = p.getErrorStream();
						DataOutputStream os = new DataOutputStream(p.getOutputStream());

						for (String command : commands) {
							//Log.i(TAG,command);
							os.writeBytes(command + "\n");
						}
						os.writeBytes("exit\n");
						os.flush();
						os.close();

						int read;
						byte[] buffer = new byte[4096];
						String output = new String();
						while ((read = es.read(buffer)) > 0) {
							output += new String(buffer, 0, read);
						}
						p.waitFor();
						Log.d("CCU_DOWNLOAD", output.trim() + " (" + p.exitValue() + ")");
						ApplicationInfo appInfo2 = RenatusApp.getAppContext().getApplicationInfo();
						Log.d("CCU_DOWNLOAD", "RenatusAPP ExecuteAsRoot END===>"+(appInfo2.flags & ApplicationInfo.FLAG_SYSTEM));
					}
				} catch (IOException e) {
					Log.e("CCU_DOWNLOAD", e.getMessage());
				} catch (InterruptedException e) {
					Log.e("CCU_DOWNLOAD", e.getMessage());
				}
			}
		});
		thread.start();
	}
}
