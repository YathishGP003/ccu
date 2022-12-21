package a75f.io.renatus;


import static a75f.io.renatus.UtilityApplication.context;

import java.util.Date;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import org.joda.time.DateTime;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.cloudconnectivity.CloudConnectivityListener;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.util.Prefs;

import org.joda.time.DateTime;

import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.util.Prefs;
public class NotificationHandler {

    private NotificationManager mNM = null;
    private int mServerConnectionStatusID = 1;
    private int mCMConnectionStatusID = 2;
    private static CloudConnectivityListener cloudConnectivityListener;
    private static final String INTERNET_DISCONNECTED_TIMESTAMP = "disconnectedInternetTime";
    private static Prefs prefs  = new Prefs(Globals.getInstance().getApplicationContext());
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
    private static Prefs prefs  = new Prefs(Globals.getInstance().getApplicationContext());


    private NotificationHandler () {
        Context context = Globals.getInstance().getApplicationContext();
        mNM =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void clearAllNotifications() {

        mHandler.mNM.cancel(mHandler.mServerConnectionStatusID);
        mHandler.mNM.cancel(mHandler.mCMConnectionStatusID);
        mHandler.mNM.cancelAll();
    }

    public static void setCloudConnectivityListener(CloudConnectivityListener listener){
        cloudConnectivityListener = listener;
    }

    public static void refreshCloudConnectivityLastUpdatedTime(){
        if(cloudConnectivityListener != null){
            cloudConnectivityListener.refreshData();
        }
    }

    public static void setCloudConnectionStatus(boolean bIsConnected) {
        if(bIsConnected){
            CCUHsApi.getInstance().writeHisValByQuery("point and diag and cloud and connected", 1.0);
            prefs.setString(INTERNET_DISCONNECTED_TIMESTAMP,"");
        }else{
            rebootDeviceByCCUNetworkWatchdogTimeoutTuner();
        }
        refreshCloudConnectivityLastUpdatedTime();
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

    private static void rebootDeviceByCCUNetworkWatchdogTimeoutTuner() {
        double ccuNetworkWatchdogTimeoutTunerValue =  TunerUtil.readTunerValByQuery("network and watchdog and tuner");
        CcuLog.i("NotificationHandler", "ccuNetworkWatchdogTimeoutTunerValue "+ccuNetworkWatchdogTimeoutTunerValue);
        if(ccuNetworkWatchdogTimeoutTunerValue != 0) { // 0 - ccuNetworkWatchdogTimeout is disabled
            DateTime currentDateTime = new DateTime();
            String internetDisconnectedTimeStamp = prefs.getString(INTERNET_DISCONNECTED_TIMESTAMP);
            if (!internetDisconnectedTimeStamp.equalsIgnoreCase("")) {
                long diffInMinutes = (currentDateTime.getMillis() - DateTime.parse(internetDisconnectedTimeStamp).getMillis()) / (60 * 1000) ;
                CcuLog.i("NotificationHandler", "currenttime " + currentDateTime + " internetDisconnectedTimeStamp " + internetDisconnectedTimeStamp + " diffInMinutes " + diffInMinutes);
                if (diffInMinutes >= ccuNetworkWatchdogTimeoutTunerValue) {
                    prefs.setString(INTERNET_DISCONNECTED_TIMESTAMP,"");
                    CcuLog.i("NotificationHandler", "Device Restarted");
                    RenatusApp.rebootTablet();
                }
            } else {
                prefs.setString(INTERNET_DISCONNECTED_TIMESTAMP,currentDateTime.toString());
            }
        }
    }

}

