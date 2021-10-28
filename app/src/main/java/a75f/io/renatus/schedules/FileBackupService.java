package a75f.io.renatus.schedules;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

import a75f.io.logic.service.FileBackupJobReceiver;

public class FileBackupService {
    public static void scheduleFileBackupServiceJob(Context context){
        AlarmManager fileBackupAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent fileBakupIntent = new Intent(context, FileBackupJobReceiver.class);
        PendingIntent fileBackupAlarmIntent = PendingIntent.getBroadcast(context, 0, fileBakupIntent, 0);
        Calendar backUpTime = Calendar.getInstance();
        backUpTime.setTimeInMillis(System.currentTimeMillis());
        backUpTime.set(Calendar.HOUR_OF_DAY, 0);
        backUpTime.set(Calendar.MINUTE , 0);
        fileBackupAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, backUpTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, fileBackupAlarmIntent);
    }
}
