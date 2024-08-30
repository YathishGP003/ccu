package a75f.io.renatus;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;
import static a75f.io.logic.L.TAG_CCU_DOWNLOAD;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import dagger.hilt.android.HiltAndroidApp;
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

	public static void executeAsRoot(String[] commands, String packageToLaunch, boolean restartCCUAppAfterInstall, boolean restartTabletAfterInstall) {
		Thread thread = new Thread(() -> {
			try {
				// Do the magic
				ApplicationInfo appInfo = RenatusApp.getAppContext().getApplicationInfo();
				CcuLog.d(TAG_CCU_DOWNLOAD, "RenatusAPP ExecuteAsRoot===>rooted="+isRooted()+", system flag="+(appInfo.flags & ApplicationInfo.FLAG_SYSTEM));
				if(isRooted()) {
					for (String command : commands) {
						Process p = Runtime.getRuntime().exec("su");
						InputStream stdout = p.getInputStream();
						InputStream es = p.getErrorStream();
						DataOutputStream os = new DataOutputStream(p.getOutputStream());
						CcuLog.d(TAG_CCU_DOWNLOAD, "ExecuteAsRoot: Executing command: '" + command + "'");
						os.writeBytes(command + "\n");
						os.writeBytes("exit $?\n");
						os.flush();
						os.close();

						int read;
						byte[] buffer = new byte[4096];
						String errorOutput = new String();

						// Read stderr
						while ((read = es.read(buffer)) > 0) {
							errorOutput += new String(buffer, 0, read);
						}
						errorOutput = errorOutput.trim();

						// Read stdout
						String stdOutput = new String();
						while ((read = stdout.read(buffer)) > 0) {
							stdOutput += new String(buffer, 0, read);
						}
						stdOutput = stdOutput.trim();

						p.waitFor();

						CcuLog.d(TAG_CCU_DOWNLOAD, String.format("ExecuteAsRoot command status: %d %s",
								p.exitValue(),
								p.exitValue() == 0 ? "Success" : "*** FAILURE ***"
						));
						if (!stdOutput.isEmpty()) {
							CcuLog.d(TAG_CCU_DOWNLOAD, "ExecuteAsRoot stdout: " + stdOutput);
						}
						if (!errorOutput.isEmpty()) {
							CcuLog.d(TAG_CCU_DOWNLOAD, "ExecuteAsRoot stderr: " + errorOutput);
						}
					}

					if (packageToLaunch != null) {
						try {
							Context context = RenatusApp.getAppContext();
							PackageManager pm = context.getPackageManager();

							// Look for a normal launch intent
							Intent launchIntent = pm.getLaunchIntentForPackage(packageToLaunch);

							if (launchIntent != null) {
								CcuLog.i(TAG_CCU_DOWNLOAD, String.format("Launching package %s", packageToLaunch));
								context.startActivity(launchIntent);
							} else {
								CcuLog.w(TAG_CCU_DOWNLOAD, "Unable to get launch intent for package " + packageToLaunch);
							}
						} catch(Exception e) {
							CcuLog.e(TAG_CCU_DOWNLOAD, String.format("Unable to launch package %s: %s", packageToLaunch, e.getMessage()));
						}
					}

					ApplicationInfo appInfo2 = RenatusApp.getAppContext().getApplicationInfo();
					CcuLog.d(TAG_CCU_DOWNLOAD, "RenatusAPP ExecuteAsRoot END===>"+(appInfo2.flags & ApplicationInfo.FLAG_SYSTEM));

					if (restartTabletAfterInstall) {
						CcuLog.i(TAG_CCU_DOWNLOAD, "Tablet reboot requested");
						rebootTablet();
					} else if (restartCCUAppAfterInstall) {
						CcuLog.i(TAG_CCU_DOWNLOAD, "CCU app restart requested");
						restartApp();
					}
				} else {
					// Two semicolons in case one of the commands is actually multiple commands separated by a semicolon
					CcuLog.e(TAG_CCU_DOWNLOAD, "Tablet is NOT rooted, unable to execute remote commands:" + String.join(";; ", commands));
				}
			} catch (IOException e) {
				CcuLog.e(TAG_CCU_DOWNLOAD, e.getMessage());
			} catch (InterruptedException e) {
				CcuLog.e(TAG_CCU_DOWNLOAD, e.getMessage());
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
			CcuLog.d("CCU_DEBUG", "************Houston, May Day, May Day, May Day, Bailing Out!!!************");
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

		CcuLog.d("CCU_DEBUG", "************Houston, CCU Is Going Down-CloseApp!!!************");
		NotificationHandler.clearAllNotifications();
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}

	public void debugLog(String tag, String value){
		CcuLog.d(TAG_CCU_ALERTS, "tag =>"+tag+"<==value==>"+value);
	}
}
