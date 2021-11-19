package a75f.io.usbserial;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;

public class UsbSerialUtil {
    
    public static final int DEVICE_ID_FTDI = 1027;
    
    private static boolean isBiskitModeEnabled(Context appContext) {
        return appContext.getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                      .getBoolean("biskit_mode", false);
    }
    
    public static boolean isCMDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
        if (deviceVID == 0x0403 ||
            deviceVID == 1003 ||
            (deviceVID == DEVICE_ID_FTDI && isBiskitModeEnabled(context))) {
            return true;
        }
        
        return false;
    }
    
    public static boolean isModbusDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
        if (deviceVID == 4292 ||
            (deviceVID == DEVICE_ID_FTDI && !isBiskitModeEnabled(context))) {
            return true;
        }
        return false;
    }
    
    public static boolean isSerialRetryRequired(CCUHsApi hayStack) {
        List<HashMap<Object, Object>> zoneEquips = hayStack.readAllEntities("equip and zone");
        return zoneEquips.size() > 0 &&
               (BuildConfig.BUILD_TYPE.equals("staging") ||
                BuildConfig.BUILD_TYPE.equals("prod") ||
                BuildConfig.BUILD_TYPE.equals("qa"));
    }
}
