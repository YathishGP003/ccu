package a75f.io.renatus;


import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;

import a75f.io.logic.Globals;

public class NotificationHandler {

    private NotificationManager mNM = null;
    private int mServerConnectionStatusID = 1;
    private int mCMConnectionStatusID = 2;

    private Notification.Builder mServerConnectionStatus =
            new Notification.Builder(Globals.getInstance().getApplicationContext())
                    .setSmallIcon(R.drawable.offline_cloud_notification)
                    .setColor((Color.RED))
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
        mHandler.mServerConnectionStatus.setSmallIcon(bIsConnected ? R.drawable.online_cloud_notification :
                R.drawable.offline_cloud_notification);
        mHandler.mServerConnectionStatus.setColor(bIsConnected ? Color.BLUE : Color.RED);
        Notification notification = mHandler.mServerConnectionStatus.build();
        notification.when = new Date().getTime();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        mHandler.mNM.notify(mHandler.mServerConnectionStatusID, notification);
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

