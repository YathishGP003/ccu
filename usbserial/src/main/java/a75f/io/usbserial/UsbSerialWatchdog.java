package a75f.io.usbserial;

import android.content.Context;
import android.content.Intent;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

/**
 * Reboots the tablet after 30 minutes if usb-serial connection is not active.
 * Serial connection may have been lost due to hardware or firmware issues which are outside the control of app.
 * This is an attempt to recover from that state without user intervention.
 *
 * It is expected to enabled only on staging and prod builds.
 */
public class UsbSerialWatchdog {
    
    private static UsbSerialWatchdog instance = null;
    private int watchdogTimeoutCounter = 0;
    private UsbSerialWatchdog(){ }
    
    public static UsbSerialWatchdog getInstance() {
        if (instance == null) {
            instance = new UsbSerialWatchdog();
        }
        return instance;
    }
    
    public void pet() {
        CcuLog.i("CCU_USB", "USB watch dog pet.");
        watchdogTimeoutCounter = 0;
    }
    
    public void bark(Context context, CCUHsApi hayStack) {
        int USB_TABLET_REBOOT_TIMEOUT_MINS = 30;
        if (++watchdogTimeoutCounter >= USB_TABLET_REBOOT_TIMEOUT_MINS) {
            CcuLog.i("CCU_USB", "USB watch dog triggering tablet reboot.");
            if (UsbSerialUtil.isSerialRetryRequired(hayStack, context)) {
                watchdogTimeoutCounter = 0;
                Intent intent = new Intent(UsbServiceActions.ACTION_USB_REQUIRES_TABLET_REBOOT);
                context.sendBroadcast(intent);
            } else {
                watchdogTimeoutCounter = 0;
            }
        } else {
            CcuLog.i("CCU_USB", "watchdogTimeoutCounter "+watchdogTimeoutCounter);
        }
    }
    
}
