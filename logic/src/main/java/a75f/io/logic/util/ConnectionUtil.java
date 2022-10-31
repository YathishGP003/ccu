package a75f.io.logic.util;

import android.content.Context;
import android.net.ConnectivityManager;
import a75f.io.logic.Globals;

public class ConnectionUtil {
    public static boolean isNetworkConnected() {
        Context context = Globals.getInstance().getApplicationContext();
        if (context == null){
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}

