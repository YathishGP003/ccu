package a75f.io.renatus;
import static a75f.io.logic.util.PreferenceUtil.getDataSyncProcessing;
import static a75f.io.messaging.handler.DataSyncHandler.getMessageExpiryTime;
import static a75f.io.messaging.handler.DataSyncHandler.isMessageTimeExpired;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.cloudconnectivity.CloudConnectivityListener;

import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.messaging.handler.DataSyncHandler;
import a75f.io.renatus.util.Prefs;

public class NotificationHandler {
    private static final long DELAY_FOR_DATA_SYNC = 480000;
    private static final long DELAY_FOR_POINT_WRITE = 120000;
    private NotificationManager mNM = null;
    private static final Prefs prefs = new Prefs(Globals.getInstance().getApplicationContext());
    static private final NotificationHandler mHandler = new NotificationHandler();
    private static CloudConnectivityListener cloudConnectivityListener;
    private static final String INTERNET_DISCONNECTED_TIMESTAMP = "disconnectedInternetTime";
    private final int mServerConnectionStatusID = 1;
    private final int mCMConnectionStatusID = 2;
    private final Notification.Builder mServerConnectionStatus =
            new Notification.Builder(Globals.getInstance().getApplicationContext())
                    .setSmallIcon(R.drawable.offline_cloud_notification)
                    .setColor((Color.RED))
                    .setContentTitle("Cloud Connection Status");
    private final Notification.Builder mCMConnectionStatus =
            new Notification.Builder(Globals.getInstance().getApplicationContext())
                    .setSmallIcon(R.drawable.newserial)
                    .setContentTitle("CM Connection Status");


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
        if (bIsConnected) {
            CCUHsApi.getInstance().writeHisValByQuery("point and diag and cloud and connected", 1.0);
            CcuLog.i(L.TAG_CCU_READ_CHANGES, "CCU IS CONNECTED TO WIFI " + new Date(System.currentTimeMillis()));
            long lastCCUUpdateTime = PreferenceUtil.getLastCCUUpdatedTime();
            CcuLog.i(L.TAG_CCU_READ_CHANGES,"Is message expired "+isMessageTimeExpired(lastCCUUpdateTime) +
                    "Is data sync processing"+getDataSyncProcessing());
            if(getDataSyncProcessing()){
                CcuLog.i(L.TAG_CCU_READ_CHANGES,"Data sync is already processing ");
                return;
            }
            if (isMessageTimeExpired(lastCCUUpdateTime)) {
                DataSyncHandler dataSyncHandler = new DataSyncHandler();
                dataSyncHandler.syncCCUData(lastCCUUpdateTime);
            }else {
                CcuLog.i(L.TAG_CCU_READ_CHANGES,"Set last updated date time "+new Date(System.currentTimeMillis()));
                PreferenceUtil.setLastCCUUpdatedTime(System.currentTimeMillis());
            }
            prefs.setString(INTERNET_DISCONNECTED_TIMESTAMP, "");
        } else {
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
        double ccuNetworkWatchdogTimeoutTunerValue =  TunerUtil.readTunerValByQuery("network and watchdog and timeout");
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

