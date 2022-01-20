package a75f.io.renatus.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtil {
    
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
       return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isConnectedToInternet(Context context) {
        int responseCode = 0;
        if (isNetworkConnected(context)) {
            HttpURLConnection urlc;
            try {
                URL url = new URL("https://silo-playground.testbed-75f-service.com/v2/about");
                urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(3000);
                urlc.connect();
                responseCode = urlc.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                return responseCode == 200;
            }
        } else
            return false;
    }

}
