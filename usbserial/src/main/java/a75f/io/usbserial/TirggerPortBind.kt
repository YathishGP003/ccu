package a75f.io.usbserial

import a75f.io.logger.CcuLog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException


object UsbPortTrigger {
    private const val TAG = "UsbHelper"

    @JvmStatic
    fun triggerUsbSerialBinding(context: Context) {
        try {
            CcuLog.d(TAG, "@@--triggerUsbSerialBinding--")
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            if (availableDrivers.isEmpty()) {
                CcuLog.d(TAG, "No USB serial drivers found")
                return
            }

            val driver = availableDrivers[0]
            val device = driver.device

            // Ask permission if not already granted
            if (!usbManager.hasPermission(device)) {
                val permissionIntent = PendingIntent.getBroadcast(
                    context, 0, Intent("a75f.io.renatus.USB_PERMISSION"),
                    PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(device, permissionIntent)
                CcuLog.d(TAG, "Requesting permission for USB device")
                return
            }

            // Open the port → this triggers kernel binding and /dev/ttyUSB0 creation
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                CcuLog.e(TAG, "Failed to open device")
                return
            }

            val port = driver.ports[0]
            CcuLog.d(TAG, "Port opened — open")
            try {
                port.open(connection)
                port.setParameters(
                    9600,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )
            } catch (e: IOException) {
                CcuLog.w(TAG, "Already claimed by kernel, likely /dev/ttyUSB0 already exists")
            }
            CcuLog.d(TAG, "Port opened — should trigger /dev/ttyUSB0")
            port.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    @JvmStatic
    fun listUsbSerialDevicesOpen(context: Context) {
        CcuLog.d(TAG, "--listUsbSerialDevices--")
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        // Default prober includes CDC/FTDI/PL2303/CH34x drivers
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            CcuLog.d(TAG, "No USB serial devices found.")
            return
        }

        for (driver in availableDrivers) {
            val device: UsbDevice = driver.device
            CcuLog.d(TAG, "Found device: VID=${device.vendorId}, PID=${device.productId}, Name=${device.deviceName}")

            if (!usbManager.hasPermission(device)) {
                val permissionIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    android.content.Intent("com.example.USB_PERMISSION"),
                    PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(device, permissionIntent)
                CcuLog.d(TAG, "Permission requested for ${device.deviceName}")
            } else {
                CcuLog.d(TAG, "Permission already granted for ${device.deviceName}")
            }
        }
    }

    @JvmStatic
    fun listUsbSerialDevices(context: Context): HashMap<String, UsbDevice> {
        CcuLog.d(TAG, "##--listUsbSerialDevices--")
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        // Get all connected USB devices
        val allDevices = usbManager.deviceList
        val serialDeviceMap = HashMap<String, UsbDevice>()

        // Filter only devices supported by the serial prober
        val serialDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        CcuLog.d(TAG, "###--listUsbSerialDevices--serialDrivers ${serialDrivers.size}")
        for (driver in serialDrivers) {
            val device = driver.device

            // Find the device name (key) from the original device list
            val entry = allDevices.entries.find { it.value == device }
            if (entry != null) {
                val deviceName = entry.key
                serialDeviceMap[deviceName] = device

                CcuLog.d(TAG, "Serial device: $deviceName, VID=${device.vendorId}, PID=${device.productId}")

                if (!usbManager.hasPermission(device)) {
                    val permissionIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent("com.example.USB_PERMISSION"),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    usbManager.requestPermission(device, permissionIntent)
                    CcuLog.d(TAG, "Permission requested for $deviceName")
                } else {
                    CcuLog.d(TAG, "Permission already granted for $deviceName")
                }
            }
        }

        return serialDeviceMap
    }
}


