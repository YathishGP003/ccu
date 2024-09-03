package a75f.io.device;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import a75f.io.device.mesh.LSerial;
import a75f.io.logic.L;

/**
 * Created by Manjunath K on 15-05-2023.
 */

public class EveryDaySchedulerService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(L.TAG_CCU_DEVICE, "EveryDaySchedulerService onReceive: ready to send seed");
        LSerial.getInstance().setResetSeedMessage(true);
    }


    /**
     * This schedules a Job which calls once in a day (24Hr).
     * @param context
     */
    public static void scheduleJobForDay(Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent serviceIntent = new Intent(context, EveryDaySchedulerService.class);
        PendingIntent seekAlarmIntent = PendingIntent.getBroadcast(context, 0, serviceIntent, PendingIntent.FLAG_IMMUTABLE);
        Calendar startTime = Calendar.getInstance();
        startTime.setTimeInMillis(System.currentTimeMillis());
        startTime.set(Calendar.HOUR_OF_DAY, 0);
        startTime.set(Calendar.MINUTE , 0);
        //Reseeding messages to happen every 6 hours.
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, startTime.getTimeInMillis(),
                6 * AlarmManager.INTERVAL_HOUR, seekAlarmIntent);

    }
}
