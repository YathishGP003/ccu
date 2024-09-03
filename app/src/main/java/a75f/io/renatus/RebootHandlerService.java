package a75f.io.renatus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.util.Calendar;
import java.util.TimeZone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.views.RebootDataCache;

public class RebootHandlerService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("CCU_REBOOT_15_DAYS")) {
            CcuLog.i(L.TAG_CCU, "RebootHandlerService onReceive: CCU Rebooting....");
            PreferenceUtil.setIsCcuRebootStarted(true);
            RebootDataCache rebootDataCache = new RebootDataCache();
            rebootDataCache.storeRebootTimestamp(true);
            RenatusApp.rebootTablet();
        }
    }


    public static void scheduleRebootJob(Context context, Boolean isCallFromDevSettings) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent serviceIntent = new Intent(context, a75f.io.renatus.RebootHandlerService.class);
        serviceIntent.setAction("CCU_REBOOT_15_DAYS");
        PendingIntent seekAlarmIntent = PendingIntent.getBroadcast(context, 0, serviceIntent, PendingIntent.FLAG_IMMUTABLE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());

        int selectedDay = Calendar.MONDAY;
        int numberOfDaysToAdd = 0;
        int startHour = 23;
        int startMinute = 00;

        if (isTestingBuild()) {
            selectedDay = sharedPreferences.getInt("rebootDay", Calendar.MONDAY);
            numberOfDaysToAdd = sharedPreferences.getInt("rebootDaysCount", 0);
            startHour = sharedPreferences.getInt("rebootHour", 23);
            startMinute = sharedPreferences.getInt("rebootMinute", 0);
        }

        Calendar startTime = Calendar.getInstance();
        startTime.setTimeInMillis(System.currentTimeMillis());
        startTime.setTimeZone(TimeZone.getDefault());
        startTime.set(Calendar.HOUR_OF_DAY, startHour);
        startTime.set(Calendar.MINUTE, startMinute);


        Calendar systemTime = Calendar.getInstance(); // System Current Time For logs
        systemTime.setTimeInMillis(System.currentTimeMillis());
        CcuLog.d(L.TAG_CCU, "RebootHandlerService scheduleRebootJob: System Current Time " + systemTime.getTime());

        if (selectedDay > 0) {
            startTime.set(Calendar.DAY_OF_WEEK, systemTime.get(Calendar.DAY_OF_WEEK));

            if (startTime.get(Calendar.DAY_OF_WEEK) != selectedDay) {
                int daysUntilNextSelectedDay = (selectedDay - startTime.get(Calendar.DAY_OF_WEEK) + 7) % 7;
                daysUntilNextSelectedDay = (daysUntilNextSelectedDay == 0) ? 7 : daysUntilNextSelectedDay;
                startTime.add(Calendar.DAY_OF_YEAR, (daysUntilNextSelectedDay + 7));
            } else if (startTime.get(Calendar.DAY_OF_WEEK) == selectedDay) { // Handle the case where today is Monday
                startTime.add(Calendar.DAY_OF_YEAR, 14);
            }
        }
        else {
            if(numberOfDaysToAdd == 0 && startTime.get(Calendar.HOUR_OF_DAY) <= systemTime.get(Calendar.HOUR_OF_DAY) && startTime.get(Calendar.MINUTE) <= systemTime.get(Calendar.MINUTE)){
                sharedPreferences.edit().putInt("rebootDaysCount", 1).apply();
                numberOfDaysToAdd = 1;
            }

            startTime.add(Calendar.DAY_OF_YEAR, numberOfDaysToAdd);
        }

        if (isTestingBuild() && isCallFromDevSettings) {
            Toast.makeText(context, "Reboot Scheduled at " + startTime.getTime(), Toast.LENGTH_SHORT).show();
        }

            CcuLog.d(L.TAG_CCU, "RebootHandlerService scheduleRebootJob: Rebooting at " + startTime.getTime());

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, startTime.getTimeInMillis(),
                    15 * 24 * AlarmManager.INTERVAL_HOUR, seekAlarmIntent);

    }

    private static Boolean isTestingBuild() {
        if ( BuildConfig.BUILD_TYPE.equals("staging")
                || BuildConfig.BUILD_TYPE.equals("qa")
                || BuildConfig.BUILD_TYPE.equals("dev_qa")
                || BuildConfig.BUILD_TYPE.equals("prod")) {
            return true;
        }
        return false;
    }

}



