package a75f.io.renatus.util.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.pubnub.DataSyncHandler;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.NotificationHandler;

/**
 * Created by mahesh on 05-11-2020.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {

    Boolean isConnected = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Connection_Info", "Result Action: " + intent.getAction());
        //here, check that the network connection is available. If yes, restart point write.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            if (isConnected != null && info.isConnected()) {
                CcuLog.i("CCU_READ_CHANGES", " CONNECTION CHANGE RECEIVER");
                if (!DataSyncHandler.isMessageTimeExpired(PreferenceUtil.getLastCCUUpdatedTime())) {
                    CcuLog.i("CCU_READ_CHANGES", " DATA SYNC NOT IN PROGRESS");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            CCUHsApi.getInstance().syncEntityWithPointWrite();
                        }
                    }, 300000);
                }
            }
            isConnected = info.isConnected();
        }
    }
}
