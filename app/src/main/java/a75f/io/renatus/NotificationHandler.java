package a75f.io.renatus;


import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import a75f.io.logic.Globals;

public class NotificationHandler {

    private NotificationManager mNM = null;
    private int mServerConnectionStatusID = 1;
    private int mCMConnectionStatusID = 2;

    private Notification.Builder mServerConnectionStatus =
            new Notification.Builder(Globals.getInstance().getApplicationContext())
                    .setSmallIcon(R.drawable.cloud_offline)
                    .setContentTitle("Cloud Connection Status");

    private Notification.Builder mCMConnectionStatus =
            new Notification.Builder(Globals.getInstance().getApplicationContext())
                    .setSmallIcon(R.drawable.newserial)
                    .setContentTitle("CM Connection Status");

    static private NotificationHandler mHandler = new NotificationHandler();

    private NotificationHandler () {
        Context context = Globals.getInstance().getApplicationContext();
        mNM =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void clearAllNotifications() {

        mHandler.mNM.cancel(mHandler.mServerConnectionStatusID);
        mHandler.mNM.cancel(mHandler.mCMConnectionStatusID);
        mHandler.mNM.cancelAll();
    }

    public static void setCloudConnectionStatus(boolean bIsConnected) {
        mHandler.mServerConnectionStatus.setContentText(bIsConnected ? "Online" : "Offline");
        mHandler.mServerConnectionStatus.setSmallIcon(bIsConnected ? R.drawable.cloud_online : R.drawable.cloud_offline);
        Notification noti = mHandler.mServerConnectionStatus.build();
        noti.when = new Date().getTime();
        noti.flags |= Notification.FLAG_NO_CLEAR;
        mHandler.mNM.notify(mHandler.mServerConnectionStatusID, noti);
    }

    public static void setCMConnectionStatus(boolean bIsConnected) {
        if (!bIsConnected) {
            mHandler.mNM.cancel(mHandler.mCMConnectionStatusID);
            return;
        }
        mHandler.mCMConnectionStatus.setContentText("Online");
        Notification noti = mHandler.mCMConnectionStatus.build();
        noti.when = new Date().getTime();
        noti.flags |= Notification.FLAG_NO_CLEAR;
        mHandler.mNM.notify(mHandler.mCMConnectionStatusID, noti);
    }

}

