package a75f.io.serial;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * Created by samjithsadasivan on 7/25/17.
 */

public class UsbPluginReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        //TODO - Ignore USB types other than CM.
        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            Intent serialIntent = new Intent(context, SerialCommService.class);
            serialIntent.putExtra("USB_DEVICE", device);
            context.startService(serialIntent);
        }
    }
}
