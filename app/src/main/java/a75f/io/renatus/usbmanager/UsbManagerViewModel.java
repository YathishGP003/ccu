package a75f.io.renatus.usbmanager;

import static a75f.io.renatus.UtilityApplication.context;
import static a75f.io.usbserial.UsbService.ACTION_USB_PERMISSION_GRANTED;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.bo.building.system.util.AdvancedAhuUtilKt;
import a75f.io.usbserial.UsbDeviceItem;
import a75f.io.usbserial.UsbPrefHelper;

public class UsbManagerViewModel extends AndroidViewModel {
    private final MutableLiveData<List<UsbDeviceItem>> usbDevices = new MutableLiveData<>();
    private final List<UsbDeviceItem> _usbDevices = new ArrayList<>();
    private final UsbManager usbManager;
    private final BroadcastReceiver usbReceiver;
    public UsbManagerViewModel(@NonNull Application application) {
        super(application);
        usbManager = (UsbManager) application.getSystemService(Context.USB_SERVICE);

        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                CcuLog.i(L.TAG_USB_MANAGER," UsbManagerViewModel onReceive : "+action);
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
                        UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    loadUsbData();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION_GRANTED);
        application.registerReceiver(usbReceiver, filter);
        loadUsbData();
    }

    public void loadUsbData() {
        CcuLog.i(L.TAG_USB_MANAGER," loadUsbData ");
        _usbDevices.clear();
        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
        connectedDevices.forEach((key, device) -> {
            CcuLog.i(L.TAG_USB_MANAGER,"Connected device "+key+" "+device);
            if (device.getSerialNumber() != null && !device.getSerialNumber().isEmpty()) {
                _usbDevices.add(new UsbDeviceItem(device.getSerialNumber(),
                        String.valueOf(device.getVendorId()), String.valueOf(device.getProductId()), "Not Assigned", "NA", device.getProductName()));
            } else if (device.getVendorId() == 1003 && device.getProductId() == 9220){
                _usbDevices.add(new UsbDeviceItem("75FCM",
                        String.valueOf(device.getVendorId()), String.valueOf(device.getProductId()), "Not Assigned", "NA", ""));
                CcuLog.i(L.TAG_USB_MANAGER,"Added CM");
            } else {
                CcuLog.i(L.TAG_USB_MANAGER,"Device filtered , no serial info VID : "+device.getVendorId());
            }
        });

        final List<UsbDeviceItem> savedDevices = UsbPrefHelper.getUsbDeviceList(context);
        _usbDevices.forEach( device -> {
            UsbDeviceItem savedDevice = savedDevices.stream()
                    .filter(d -> d.getSerial().equals(device.getSerial()))
                    .findFirst()
                    .orElse(null);
            if (savedDevice != null) {
                device.setProtocol(savedDevice.getProtocol());
                device.setPort(savedDevice.getPort());
                device.setModbusConfig(savedDevice.getModbusConfig());
                device.setBacnetConfig(savedDevice.getBacnetConfig());
            }
        });

        for (int i = 0; i < _usbDevices.size(); i++) {
            UsbDeviceItem item = _usbDevices.get(i);
            if (isCMDevice(item)) {
                item.setProtocol("NA");
                _usbDevices.remove(i);
                _usbDevices.add(0, item);
                break;
            }
        }

        usbDevices.setValue(_usbDevices);
        CcuLog.i(L.TAG_USB_MANAGER, " loaded devices "+usbDevices.getValue());
    }
    public LiveData<List<UsbDeviceItem>> getUsbDevices() {
        return usbDevices;
    }

    public List<String> getCMConnectedDevices() {
        List<String> cmDevices = new ArrayList<>();
        if (AdvancedAhuUtilKt.isConnectModuleAvailable()) {
            cmDevices.add("Advanced AHU");
        }

        if (ConnectNodeUtil.Companion.isConnectNodeAvailable()) {
            cmDevices.add("Connect Node");
        }

        return cmDevices;
    }
    public void saveUsbConfiguration(Context context) {
        UsbPrefHelper.saveUsbDeviceList(context, _usbDevices);
    }

    private boolean isCMDevice(UsbDeviceItem deviceItem) {
        return deviceItem.getVendor().equals("1003");
    }
}
