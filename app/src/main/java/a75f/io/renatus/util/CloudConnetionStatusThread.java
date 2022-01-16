package a75f.io.renatus.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import a75f.io.logic.Globals;
import a75f.io.renatus.NotificationHandler;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;

public class CloudConnetionStatusThread extends Thread {

    static final int CLOUD_DEAD_SKIP_COUNT = 10;
    static final int WIFI_TOGGLE_SKIP_COUNT = 15;

    boolean bStopThread = false;
    boolean bIsThreadRunning = false;
    public void stopThread() {
        bStopThread = true;
        interrupt();
    }

    private long mCloudAlive = 0;
    private boolean mWifiAlive = false;
    private long mAlertID = -1;

    public boolean isCloudAlive() {
        return (mCloudAlive <= CLOUD_DEAD_SKIP_COUNT);
    }

    public boolean isWifiAlive() {
        return mWifiAlive;
    }

    @Override
    public void run() {
        Log.d("CCU_CLOUDSTATUS", "Cloud connection status thread started with " + String.valueOf(bIsThreadRunning));
        while (!bStopThread) {
            bIsThreadRunning = true;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){
                @Override
                public void run() {
                    pingCloudServer();
                }
            }, 10);
            NotificationHandler.setCloudConnectionStatus(NetworkUtil.isConnectedToInternet(UtilityApplication.context));
            try {
                sleep(40*1000);
            } catch (InterruptedException e) {
                bStopThread = true;
                Log.d("CCU_CLOUDSTATUS", "Cloud connection status thread interrupted: " + e.getMessage());
            }
        }
        Log.d("CCU_CLOUDSTATUS", "Cloud connection status thread exited");
        bIsThreadRunning = false;
        bStopThread = false;
    }

    private synchronized void pingCloudServer() {
        final ConnectivityManager connMgr = (ConnectivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (netInfo != null && netInfo.isConnected() || (networkInfo != null && networkInfo.isConnected())) {
            //  Some sort of connection is open, check if server is reachable
            mWifiAlive = true;
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
            spDefaultPrefs.edit().putBoolean("75fNetworkAvailable", true).commit();
        }
        else {

            mWifiAlive = false;
            setCloudStatus(false);
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
            spDefaultPrefs.edit().putBoolean("75fNetworkAvailable", false).commit();
        }
    }

    private void setCloudStatus(boolean bAlive) {
        if (bAlive) {
            mCloudAlive = 0;
        }
        else {
            mCloudAlive++;
            if (mCloudAlive >= WIFI_TOGGLE_SKIP_COUNT) {
                if (mCloudAlive % WIFI_TOGGLE_SKIP_COUNT == 0) {
                    WifiManager wifi = (WifiManager) Globals.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    wifi.setWifiEnabled(false);
                    wifi.setWifiEnabled(true);
                }
                if(mCloudAlive > (WIFI_TOGGLE_SKIP_COUNT * 20))
                    RenatusApp.rebootTablet();
            }
        }
    }

}
