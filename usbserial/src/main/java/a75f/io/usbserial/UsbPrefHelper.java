package a75f.io.usbserial;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import a75f.io.logger.CcuLog;


public class UsbPrefHelper {
    private static final String KEY_USB_DEVICE_LIST = "usb_device_list";

    public static void saveUsbDeviceList(Context context, List<UsbDeviceItem> deviceList) {
        List<UsbDeviceItem> copy = new ArrayList<>(deviceList);
        copy.removeIf(device -> "NA".equalsIgnoreCase(device.getPort()));
        copy.removeIf(device -> "Not Assigned".equalsIgnoreCase(device.getProtocol()));
        for (UsbDeviceItem usbDeviceItem : copy) {
            if (usbDeviceItem.getProtocol().equalsIgnoreCase("Modbus")){
                usbDeviceItem.setBacnetConfig(null);
            } else if (usbDeviceItem.getProtocol().equalsIgnoreCase("BACnet MSTP")) {
                usbDeviceItem.setModbusConfig(null);
            }
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(copy);
        editor.putString(KEY_USB_DEVICE_LIST, json);
        editor.apply();
    }

    public static void saveUsbDeviceList(Context context, String deviceList) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USB_DEVICE_LIST, deviceList);
        editor.apply();
    }

    public static List<UsbDeviceItem> getUsbDeviceList(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = prefs.getString(KEY_USB_DEVICE_LIST, null);

        if (json != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<UsbDeviceItem>>() {}.getType();
            return gson.fromJson(json, listType);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns the list of modbus COM ports. If none are found, it suggests the next available COM port
     * @param context
     * @return
     */
    public static List<String> getModbusComPorts(Context context) {
        List<UsbDeviceItem> deviceItems = getUsbDeviceList(context);
        List<String> modbusComPorts = new ArrayList<>();
        for (UsbDeviceItem item : deviceItems) {
            if ("Modbus".equalsIgnoreCase(item.getProtocol())) {
                modbusComPorts.add(item.getPort());
            }
        }
        modbusComPorts.sort((a, b) -> {
            try {
                int numA = Integer.parseInt(a.replaceAll("\\D", ""));
                int numB = Integer.parseInt(b.replaceAll("\\D", ""));
                return Integer.compare(numA, numB);
            } catch (NumberFormatException e) {
                return a.compareToIgnoreCase(b);
            }
        });
        if (modbusComPorts.isEmpty()) {
            int nextCom = deviceItems.stream()
                    .map(UsbDeviceItem::getPort)                  // extract COM string
                    .filter(Objects::nonNull)                        // skip nulls
                    .filter(name -> name.startsWith("COM"))          // only COM entries
                    .map(name -> name.substring(3))                  // remove "COM"
                    .mapToInt(numStr -> {
                        try {
                            return Integer.parseInt(numStr);
                        } catch (NumberFormatException e) {
                            return 0; // ignore malformed values
                        }
                    })
                    .max()
                    .orElse(0);

            modbusComPorts.add("COM " + (nextCom + 1));
        }
        return modbusComPorts;
    }

    /**
     * List of all Modbus COM ports that are currently connected
     * @param context
     * @return
     */
    public static List<String> getConnectedModbusComPorts(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();

        List<UsbDeviceItem> usbDeviceItems = UsbPrefHelper.getUsbDeviceList(context);

        Set<String> connectedKeys = new HashSet<>();

        for (UsbDevice device : connectedDevices.values()) {
            try {
                String serial = device.getSerialNumber();
                if (serial != null) {
                    connectedKeys.add(serial);
                } else {
                    // fallback if serial is not readable
                    connectedKeys.add(device.getVendorId() + ":" + device.getProductId());
                }
            } catch (Exception e) {
                connectedKeys.add(device.getVendorId() + ":" + device.getProductId());
            }
        }

        List<String> connectedModbusPorts = new ArrayList<>();

        for (UsbDeviceItem item : usbDeviceItems) {
            boolean isConnected = connectedKeys.contains(item.getSerial())
                    || connectedKeys.contains(item.getVendor() + ":" + item.getProductId());

            if (isConnected && "Modbus".equalsIgnoreCase(item.getProtocol())) {
                connectedModbusPorts.add(item.getPort());
            }
        }

        CcuLog.d("ModbusConfigView", "Connected Modbus Ports: " + connectedModbusPorts);
        connectedModbusPorts.sort((a, b) -> {
            try {
                int numA = Integer.parseInt(a.replaceAll("\\D", ""));
                int numB = Integer.parseInt(b.replaceAll("\\D", ""));
                return Integer.compare(numA, numB);
            } catch (NumberFormatException e) {
                return a.compareToIgnoreCase(b);
            }
        });

        return connectedModbusPorts;
    }

    public static  UsbDeviceItem getMstpDeviceConfig(Context context) {
        List<UsbDeviceItem> deviceItems = getUsbDeviceList(context);
        for (UsbDeviceItem item : deviceItems) {
            if ("BACnet MSTP".equalsIgnoreCase(item.getProtocol())) {
                return item;
            }
        }
        return null;
    }
}
