package a75f.io.usbserial

import a75f.io.logger.CcuLog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.preference.PreferenceManager

class UsbDisconnectReceiver : BroadcastReceiver() {
    companion object {
        private const val KEY_DISCONNECT_TIME = "mstp_disconnect_time"

        private fun saveDisconnectTime(context: Context, time: Long) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putLong(KEY_DISCONNECT_TIME, time).apply()
        }

        fun clearDisconnectTime(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().remove(KEY_DISCONNECT_TIME).apply()
        }

        fun getMstpDisconnectTime(context: Context): Long {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getLong(KEY_DISCONNECT_TIME, 0L)
        }

    }

    override fun onReceive(context: Context, intent: Intent) {

        val mstpDevice = UsbPrefHelper.getMstpDeviceConfig(context)

        if (mstpDevice == null) {
            CcuLog.d("UsbDisconnectReceiver", "No MSTP device configured")
            return
        }
        if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            if (device != null && device.serialNumber == mstpDevice.serial) {
                CcuLog.d("UsbDisconnectReceiver", "MSTP adapter disconnected: ${device.serialNumber}")
                saveDisconnectTime(context, System.currentTimeMillis())
            }
        }

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            if (device != null && device.serialNumber == mstpDevice.serial) {
                CcuLog.d("UsbDisconnectReceiver", "MSTP adapter connected: ${device.serialNumber}")
                clearDisconnectTime(context)
                val bacnetForceDisabled = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("bacnetMstpForceDisabled", false)

                if (bacnetForceDisabled) {
                    CcuLog.d("UsbDisconnectReceiver", "Clearing bacnetMstpForceDisabled flag")
                    val intent = Intent(UsbServiceActions.ACTION_USB_MSTP_WAS_DISABLED)
                    context.sendBroadcast(intent)
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit().putBoolean("bacnetMstpForceDisabled", false).apply()
                }
            }
        }
    }
}
