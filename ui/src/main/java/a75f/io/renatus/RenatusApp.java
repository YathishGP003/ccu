package a75f.io.renatus;

import android.util.Log;

import a75f.io.bo.SmartNode;
import a75f.io.util.UtilityApplication;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class RenatusApp extends UtilityApplication {
    
    private static final String TAG = RenatusApp.class.getSimpleName();
    public boolean isProvisioned = false;
    public SmartNode mSmartNode  = null;
    
    
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "RENATUS APP INITIATED");
    }
}
