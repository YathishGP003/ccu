package a75f.io.usbserial;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

public class UsbSerialUtil {
    
    public static final int DEVICE_ID_FTDI = 1027;
    private static final int VENDOR_ID_CONNECT = 1027;
    private static final int PRODUCT_ID_CONNECT = 24577;
    
    private static boolean isBiskitModeEnabled(Context appContext) {
        return appContext.getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                      .getBoolean("biskit_mode", false);
    }
    
    public static boolean isCMDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
        if (deviceVID == 1003) {
            return true;
        }
        // Enabled Biskit connection for all higher variants
        return deviceVID == DEVICE_ID_FTDI && isBiskitModeEnabled(context);
    }
    
    public static boolean isModbusDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
    
        if ((deviceVID == DEVICE_ID_FTDI && isBiskitModeEnabled(context))) {
            return false;
        }

        return deviceVID == 4292 || deviceVID == DEVICE_ID_FTDI;
    }

    public static boolean isSerialRetryRequired(CCUHsApi hayStack, Context context) {
        List<HashMap<Object, Object>> zoneEquips = hayStack.readAllEntities("equip and zone");
        return zoneEquips.size() > 0 &&
                (BuildConfig.BUILD_TYPE.equals("staging") ||
                        BuildConfig.BUILD_TYPE.equals("prod") || BuildConfig.BUILD_TYPE.equals("daikin_prod")
                        || BuildConfig.BUILD_TYPE.equals("carrier_prod")
                        || BuildConfig.BUILD_TYPE.equals("airoverse_prod")
                ) && !isBiskitModeEnabled(context)
        ;
    }

    public static int getPreferredConnectModuleSerialType(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("connect_serial_port", 0);
    }

    public static boolean isConnectDevice(UsbDevice device, Context context) {
        // Connect device and FTDI cable has the same vendor id 1027 and product id 24577
        if ((BuildConfig.BUILD_TYPE.equals("qa") ||
                BuildConfig.BUILD_TYPE.equals("dev_qa") ||
                BuildConfig.BUILD_TYPE.equals("dev") ||
                BuildConfig.BUILD_TYPE.equals("local")) && (device.getVendorId() == DEVICE_ID_FTDI && isBiskitModeEnabled(context))) {
            return false;
        }
        return device.getVendorId() == VENDOR_ID_CONNECT && device.getProductId() == PRODUCT_ID_CONNECT;
    }
}

enum UsbSerialType {
    MODBUS,
    CONNECT,
    CM
}

enum ConnectSerialPort {
    NO_CONNECT_MODULE,
    CCU_PORT,
    CM_VIRTUAL_PORT2
}