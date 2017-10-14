package a75f.io.logic;
/**
 * Created by Yinten on 10/13/2017.
 */

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.kinvey.android.push.KinveyGCMService;

public class GCMService extends KinveyGCMService {
    @Override
    public void onMessage(String message) {
        displayNotification(message);
    }
    @Override
    public void onError(String error) {
        displayNotification(error);
    }
    @Override
    public void onDelete(String deleted) {
        displayNotification(deleted);
    }
    @Override
    public void onRegistered(String gcmID) {
        displayNotification(gcmID);
    }
    @Override
    public void onUnregistered(String oldID) {
        displayNotification(oldID);
    }
    //This method will return the WakefulBroadcastReceiver class you define in the next step
    public Class getReceiver() {
        return GCMReceiver.class;
    }
    private void displayNotification(String message){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                                                      .setContentTitle(getApplicationContext().getResources().getString(R.string.app_name))
                                                      .setContentText(message);
        NotificationManager
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }
}
