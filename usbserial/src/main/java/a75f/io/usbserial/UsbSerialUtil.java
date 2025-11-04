package a75f.io.usbserial;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.preference.PreferenceManager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

public class UsbSerialUtil {
    
    public static final int DEVICE_ID_FTDI = 1027;
    private static final int VENDOR_ID_CONNECT = 1027;
    private static final int PRODUCT_ID_CONNECT = 24577;
    
    public static boolean isBiskitModeEnabled(Context appContext) {
        return appContext.getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                      .getBoolean("biskit_mode", false);
    }
    
    public static boolean isCMDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
        if (deviceVID == 1003) {
            return true;
        }
        return isBiskitDevice(device, context) && isBiskitModeEnabled(context);
    }

    public static boolean isBiskitDevice(UsbDevice device, Context context) {
        List<UsbDeviceItem> configs = UsbPrefHelper.getUsbDeviceList(context);
        for (UsbDeviceItem config : configs) {
            if (config.getVendor().equals(String.valueOf(device.getVendorId())) &&
                    config.getProductId().equals(String.valueOf(device.getProductId())) &&
                    config.getProtocol().equalsIgnoreCase("Biskit")) {
                CcuLog.i("USB_MANAGER","isBiskitDevice: true for "+device.getVendorId()+"-"+device.getProductId());
                return true;
            }
        }
        return false;
    }
    
    public static boolean isModbusDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
    
        if ((deviceVID == DEVICE_ID_FTDI && isBiskitModeEnabled(context))) {
            return false;
        }

        return deviceVID == 4292 || deviceVID == DEVICE_ID_FTDI;
    }

    public static boolean isModbusDevice(UsbDevice device, List<UsbDeviceItem> configs) {
        for (UsbDeviceItem config : configs) {
            if (config.getVendor().equals(String.valueOf(device.getVendorId())) &&
                    config.getProductId().equals(String.valueOf(device.getProductId())) &&
                    config.getProtocol().equalsIgnoreCase("Modbus")) {
                return true;
            }
        }
        return false;
    }

    public static UsbDeviceItem getModbusDevice(UsbDevice device, List<UsbDeviceItem> configs) {
        for (UsbDeviceItem config : configs) {
            if (config.getSerial().equals(device.getSerialNumber()) &&
                    config.getVendor().equals(String.valueOf(device.getVendorId())) &&
                    config.getProductId().equals(String.valueOf(device.getProductId())) &&
                    config.getProtocol().equalsIgnoreCase("Modbus")) {
                return config;
            }
        }
        return null;
    }

    public static UsbDeviceItem getModbusDeviceCom1(UsbDevice device, List<UsbDeviceItem> configs) {
        UsbDeviceItem com1Device = configs.stream()
                .filter(config -> "Modbus".equalsIgnoreCase(config.getProtocol()))
                .min(Comparator.comparingInt(config -> {
                    try {
                        // Extract digits from the port string, e.g. "COM12" -> 12
                        return Integer.parseInt(config.getPort().replaceAll("\\D", ""));
                    } catch (NumberFormatException e) {
                        return Integer.MAX_VALUE; // non-numeric ports go to the end
                    }
                }))
                .orElse(null);
        CcuLog.i("CCU_USB_MODBUS","getModbusDeviceCom1: com1Device "+(com1Device != null? com1Device : "null"));
        if (com1Device != null && com1Device.getSerial().equals(device.getSerialNumber()) &&
                com1Device.getVendor().equals(String.valueOf(device.getVendorId())) &&
                com1Device.getProductId().equals(String.valueOf(device.getProductId())) &&
                com1Device.getProtocol().equalsIgnoreCase("Modbus")) {
            return com1Device;
        }
        return null;
    }

    public static UsbDeviceItem getModbusDeviceCom2(UsbDevice device, List<UsbDeviceItem> configs) {
        //Com2 Service need not run when there is only one modbus config.
        if (configs.stream()
                .filter(config -> "Modbus".equalsIgnoreCase(config.getProtocol())).count() < 2) {
            return null;
        }

        UsbDeviceItem com2Device = configs.stream()
                .filter(config -> "Modbus".equalsIgnoreCase(config.getProtocol()))
                .max(Comparator.comparingInt(config -> {
                    try {
                        // Extract digits from the port string, e.g. "COM12" -> 12
                        return Integer.parseInt(config.getPort().replaceAll("\\D", ""));
                    } catch (NumberFormatException e) {
                        return Integer.MAX_VALUE; // non-numeric ports go to the end
                    }
                }))
                .orElse(null);
        CcuLog.i("CCU_USB_MODBUS2","getModbusDeviceCom2: com2Device "+(com2Device != null? com2Device : "null"));
        if (com2Device != null && com2Device.getSerial().equals(device.getSerialNumber()) &&
                com2Device.getVendor().equals(String.valueOf(device.getVendorId())) &&
                com2Device.getProductId().equals(String.valueOf(device.getProductId())) &&
                com2Device.getProtocol().equalsIgnoreCase("Modbus")) {
            return com2Device;
        }
        return null;
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
        return sharedPreferences.getInt("connect_serial_port", ConnectSerialPort.CM_VIRTUAL_PORT2.ordinal()); // Default to CM_VIRTUAL_PORT2 preferred for connect module serial port (Ref portTask 30888)
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

enum ConnectSerialPort {
    NO_CONNECT_MODULE,
    CCU_PORT,
    CM_VIRTUAL_PORT2
}