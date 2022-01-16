package a75f.io.renatus.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
       return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isConnectedToInternet(Context context) {
        if (isNetworkConnected(context)) {
            try {
                return Runtime.getRuntime().exec("ping -c 1 www.google.com").waitFor() == 0;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else
            return false;
    }

}
