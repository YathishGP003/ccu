package a75f.io.device.modbus;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import a75f.io.logic.Globals;
import a75f.io.logic.L;

public class ModbusWatchdog {
    
    private static final long SERIAL_WATCHDOG_TIMEOUT_MS = 1000 * 6 * 5;
    private long serialTimeoutReportedTime= 0;
    private ModbusWatchdog() {
    
    }
    private static ModbusWatchdog instance = null;
    
    public static ModbusWatchdog getInstance() {
        if (instance == null) {
            instance = new ModbusWatchdog();
        }
        return instance;
    }
    
    public void pet() {
        serialTimeoutReportedTime = 0;
    }
    
    public void reportTimeout() {
        if (serialTimeoutReportedTime == 0) {
            serialTimeoutReportedTime = System.currentTimeMillis();
        } else if ((System.currentTimeMillis() - serialTimeoutReportedTime) > SERIAL_WATCHDOG_TIMEOUT_MS){
            Log.d(L.TAG_CCU_MODBUS, "##### Modbus Serial Communication not responding. Restart App ###### ");
            //Restart the app if modbus serial comm has been failing for more than 5 mins.
            bite(Globals.getInstance().getApplicationContext());
        }
        
    }
    
    public static void bite(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }
}
