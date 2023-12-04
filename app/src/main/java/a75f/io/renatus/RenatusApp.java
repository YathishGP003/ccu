package a75f.io.renatus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.renatus.util.Prefs;
import dagger.hilt.android.HiltAndroidApp;

import androidx.multidex.MultiDex;
import androidx.appcompat.app.AppCompatDelegate;
import android.util.Log;


import com.instabug.crash.CrashReporting;
import com.instabug.library.Feature;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
/**
 * Created by ryanmattison isOn 7/24/17.
 */

@HiltAndroidApp
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

		initInstabug();
	}

	private void initInstabug() {
		new Instabug.Builder(this, "a92457eeb44e965eabf019b4373e8216")
				.setInvocationEvents(InstabugInvocationEvent.NONE)
				.build();

		boolean anrReporting = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting"
				, Context.MODE_PRIVATE).getBoolean("anr_reporting_enabled", false);
		if (anrReporting) {
			CrashReporting.setAnrState(Feature.State.ENABLED);
			CrashReporting.setState(Feature.State.ENABLED);
		} else {
			CrashReporting.setAnrState(Feature.State.DISABLED);
			CrashReporting.setState(Feature.State.DISABLED);
		}
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
					Log.d("CCU_DOWNLOAD", "APP ExecuteAsRoot===>"+isRooted()+","+(appInfo.flags & ApplicationInfo.FLAG_SYSTEM));
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
						Log.d("CCU_DOWNLOAD", "APP ExecuteAsRoot END===>"+(appInfo2.flags & ApplicationInfo.FLAG_SYSTEM));
						closeApp();
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
	public static void setIntentToRestartCCU() {
		Intent intent = new Intent(getAppContext(), SplashActivity.class);
		PendingIntent pending = PendingIntent.getActivity(getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getAppContext().getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 5000, pending);
	}

	public static void rebootTablet() {
		CCUHsApi.getInstance().tagsDb.persistUnsyncedCachedItems();
		boolean persistImmediate = true;
		CCUHsApi.getInstance().saveTagsData(persistImmediate);
		setIntentToRestartCCU();
		try {
			Log.d("CCU_DEBUG", "************Houston, May Day, May Day, May Day, Bailing Out!!!************");
			Runtime.getRuntime().exec("chmod 755 /system/xbin/su");
			Runtime.getRuntime().exec("reboot");
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void closeApp() {
		CCUHsApi.getInstance().tagsDb.persistUnsyncedCachedItems();
		boolean persistImmediate = true;
		CCUHsApi.getInstance().saveTagsData(persistImmediate);
		setIntentToRestartCCU();

		Log.d("CCU_DEBUG", "************Houston, CCU Is Going Down-CloseApp!!!************");
		NotificationHandler.clearAllNotifications();
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}
}
