package a75f.io.renatus.util.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by mahesh on 05-11-2020.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Connection_Info", "Result Action: " + intent.getAction());
        //here, check that the network connection is available. If yes, restart point write.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            if (info.isConnected()) {
                CCUHsApi.getInstance().syncEntityWithPointWrite();
            }
        }
    }
}
