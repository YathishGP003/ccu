package a75f.io.usbserial;

import android.content.Context;

public class UsbUtils {
    
    public static final int DEVICE_ID_FTDI = 1027;
    
    
    
    public static boolean isBiskitMode(Context appContext) {
        return appContext.getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                      .getBoolean("biskit_mode", false);
    }
    
}
