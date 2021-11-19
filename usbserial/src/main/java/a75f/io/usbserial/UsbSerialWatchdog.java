package a75f.io.usbserial;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import a75f.io.api.haystack.CCUHsApi;

public class UsbSerialWatchdog {
    
    private final int USB_TABLET_REBOOT_TIMEOUT_MINS = 2;
    private static UsbSerialWatchdog instance = null;
    private UsbSerialWatchdog(){ }
    
    public static UsbSerialWatchdog getInstance() {
        if (instance == null) {
            instance = new UsbSerialWatchdog();
        }
        return instance;
    }
    
    public void pet(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                         .putInt("usbSerialWatchdogCounter", 0)
                         .apply();
    }
    
    public void bark(Context context, CCUHsApi hayStack) {
        incrementTimeoutCounter(context);
        if (getTimeoutCounter(context) >= USB_TABLET_REBOOT_TIMEOUT_MINS) {
            Log.i("CCU_SERIAL", "USB watch dog triggering tablet reboot.");
            if (UsbSerialUtil.isSerialRetryRequired(hayStack)) {
                Intent intent = new Intent(UsbServiceActions.ACTION_USB_REQUIRES_TABLET_REBOOT);
                context.sendBroadcast(intent);
            }
            pet(context);
        }
    }
    
    private static int getTimeoutCounter(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                                .getInt("usbSerialWatchdogCounter",0);
    }
    
    private static void incrementTimeoutCounter(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int counter = sharedPreferences.getInt("usbSerialWatchdogCounter",0);
        sharedPreferences.edit()
                         .putInt("usbSerialWatchdogCounter", ++counter)
                         .apply();
        
    }
}
