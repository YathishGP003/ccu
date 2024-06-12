package a75f.io.renatus;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;
import static a75f.io.logic.L.TAG_CCU_DOWNLOAD;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;
import dagger.hilt.android.HiltAndroidApp;

import androidx.multidex.MultiDex;
import androidx.appcompat.app.AppCompatDelegate;
import android.util.Log;


import com.instabug.crash.CrashReporting;
import com.instabug.library.Feature;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
	public static void executeAsRoot(String[] commands, Boolean restartCCUAppAfterInstall) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Do the magic
					ApplicationInfo appInfo = RenatusApp.getAppContext().getApplicationInfo();
					Log.d(TAG_CCU_DOWNLOAD, "RenatusAPP ExecuteAsRoot===>"+isRooted()+","+(appInfo.flags & ApplicationInfo.FLAG_SYSTEM));
					if(isRooted()) {
						Process p = Runtime.getRuntime().exec("su");
						InputStream stdout = p.getInputStream();
						InputStream es = p.getErrorStream();
						DataOutputStream os = new DataOutputStream(p.getOutputStream());

						for (String command : commands) {
							Log.d(TAG_CCU_DOWNLOAD, "ExecuteAsRoot: Preparing command: " + command);
							os.writeBytes(command + "\n");
						}
						os.writeBytes("exit\n");
						os.flush();
						os.close();

						int read;
						byte[] buffer = new byte[4096];
						String errorOutput = new String();

						// Read stderr
						while ((read = es.read(buffer)) > 0) {
							errorOutput += new String(buffer, 0, read);
						}

						// Read stdout
						String stdOutput = new String();
						while ((read = stdout.read(buffer)) > 0) {
							stdOutput += new String(buffer, 0, read);
						}

						p.waitFor();

						Log.d(TAG_CCU_DOWNLOAD, "ExecuteAsRoot stdout: " + stdOutput.trim());
						Log.d(TAG_CCU_DOWNLOAD, "ExecuteAsRoot stderr: " + errorOutput.trim() + " (exit status: " + p.exitValue() + ")");

						ApplicationInfo appInfo2 = RenatusApp.getAppContext().getApplicationInfo();
						Log.d(TAG_CCU_DOWNLOAD, "RenatusAPP ExecuteAsRoot END===>"+(appInfo2.flags & ApplicationInfo.FLAG_SYSTEM));
						if (restartCCUAppAfterInstall) {
							restartApp();
						}
					} else {
						// Two semicolons in case one of the commands is actually multiple commands separated by a semicolon
						Log.e(TAG_CCU_DOWNLOAD, "Tablet is NOT rooted, unable to execute remote commands:" + String.join(";; ", commands));
					}
				} catch (IOException e) {
					Log.e(TAG_CCU_DOWNLOAD, e.getMessage());
				} catch (InterruptedException e) {
					Log.e(TAG_CCU_DOWNLOAD, e.getMessage());
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

	public static void restartApp() {
		AlertManager.getInstance().clearAlertsWhenAppClose();
		AlertManager.getInstance().getRepo().setRestartAppToTrue();
		CCUHsApi.getInstance().writeHisValByQuery("app and restart",1.0);
		closeApp();
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

	public void debugLog(String tag, String value){
		Log.d(TAG_CCU_ALERTS, "tag =>"+tag+"<==value==>"+value);
	}
}
