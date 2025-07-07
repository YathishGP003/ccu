package a75f.io.renatus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.util.CommonQueries
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.renatus.ota.OTAUpdateHandlerService
import a75f.io.usbserial.UsbConnectService
import a75f.io.usbserial.UsbModbusService
import a75f.io.usbserial.UsbService
import a75f.io.usbserial.UsbServiceActions
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackgroundServiceInitiator(val context: Context = UtilityApplication.context){

    private var usbService: UsbService? = null
    private var usbModbusService: UsbModbusService? = null
    private var usbConnectService: UsbConnectService? = null

    private val TAG_CCU_SERVICE_INIT = "CCU_SERVICE_INIT"

    companion object {
        @Volatile
        private var instance: BackgroundServiceInitiator? = null

        /**
         * Returns the singleton.
         * The *application* context is stored, so no Android component
         * (Activity, Service, etc.) can be leaked.
         */
        fun getInstance(context: Context = UtilityApplication.context): BackgroundServiceInitiator {
            // Always coerce to applicationContext
            val appContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: BackgroundServiceInitiator(appContext).also { instance = it }
            }
        }

        /** Convenience entry point */
        fun initializeServices(context: Context = UtilityApplication.context) {
            getInstance(context).initServices()
        }
    }

    fun initServices() {
        CoroutineScope(Dispatchers.IO).launch {
            // This while loop has been added to wait for the Room DB to be ready before starting the services.
            // I am not sure whether removing this loop will cause any issues, so I am keeping it for now.
            // During QE, please check if this is causing any issues.
            while (!RenatusApp.isRoomDbReady) {
                try {
                    CcuLog.d(TAG_CCU_SERVICE_INIT, "CCU DB not ready yet. Waiting for 1 sec")
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            setUsbFilters() // Start listening notifications from UsbService
            CcuLog.d(TAG_CCU_SERVICE_INIT, "startService for OTAUpdateHandlerService")
            context.startService(
                Intent(
                    context,
                    OTAUpdateHandlerService::class.java
                )
            ) // Start OTA update event + timer handler service
            CcuLog.d(TAG_CCU_SERVICE_INIT, "startService for UsbService")
            startUsbService(
                UsbService::class.java,
                usbConnection,
                null
            ) // Start UsbService(if it was not started before) and
            CcuLog.d(TAG_CCU_SERVICE_INIT, "startService for UsbModbusService")
            startUsbModbusService(
                UsbModbusService::class.java,
                usbModbusConnection,
                null
            ) // Start UsbService(if it was not
            CcuLog.d(TAG_CCU_SERVICE_INIT, "startService for UsbConnectService")
            startConnectService(UsbConnectService::class.java, usbConnectConnection, null)
        }
    }

    private fun setUsbFilters() {
        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        filter.addAction(UsbModbusService.ACTION_USB_MODBUS_DISCONNECTED)
        filter.addAction(UsbServiceActions.ACTION_USB_PRIV_APP_PERMISSION_DENIED)
        filter.addAction(UsbServiceActions.ACTION_USB_REQUIRES_TABLET_REBOOT)
        filter.addAction(UsbConnectService.ACTION_USB_CONNECT_DISCONNECTED)
        context.registerReceiver(mUsbReceiver, filter)
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED -> {
                    NotificationHandler.setCMConnectionStatus(true)
                    LSerial.getInstance().setResetSeedMessage(true)
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show()
                }

                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED -> {
                    NotificationHandler.setCMConnectionStatus(false)
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show()
                }

                UsbService.ACTION_NO_USB -> {
                    NotificationHandler.setCMConnectionStatus(false)
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show()
                }

                UsbService.ACTION_USB_DISCONNECTED -> {
                    NotificationHandler.setCMConnectionStatus(false)
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                }

                UsbModbusService.ACTION_USB_MODBUS_DISCONNECTED ->                     //NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB Modbus disconnected", Toast.LENGTH_SHORT).show()

                UsbConnectService.ACTION_USB_CONNECT_DISCONNECTED ->                     //NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB Connect disconnected", Toast.LENGTH_SHORT).show()

                UsbService.ACTION_USB_NOT_SUPPORTED -> {
                    NotificationHandler.setCMConnectionStatus(false)
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show()
                }

                UsbServiceActions.ACTION_USB_PRIV_APP_PERMISSION_DENIED -> {
                    NotificationHandler.setCMConnectionStatus(false)
                    Toast.makeText(context, R.string.usb_permission_priv_app_msg, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private val usbConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            try {
                CcuLog.d(
                    TAG_CCU_SERVICE_INIT,
                    "BackgroundServiceInitiator -" + arg1.isBinderAlive + "," + arg1.toString() + "," + arg0.className + "," + arg1.interfaceDescriptor
                )
                if (arg1.isBinderAlive) {
                    usbService = (arg1 as UsbService.UsbBinder).service
                    LSerial.getInstance().setUSBService(usbService)

                    //TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
                    usbService?.setHandler(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    private fun startUsbService(
        service: Class<*>,
        serviceConnection: ServiceConnection,
        extras: Bundle?
    ) {
        if (!UsbService.SERVICE_CONNECTED) {
            val startService = Intent(context, service)
            if (extras != null && !extras.isEmpty()) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            context.startService(startService)
        }
        val bindingIntent = Intent(context, service)
        context.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startUsbModbusService(
        service: Class<*>,
        serviceConnection: ServiceConnection,
        extras: Bundle?
    ) {
        if (!UsbModbusService.SERVICE_CONNECTED) {
            val startService = Intent(context, service)
            if (extras != null && !extras.isEmpty()) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            context.startService(startService)
        }
        val bindingIntent = Intent(context, service)
        context.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val usbModbusConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            try {
                CcuLog.d(
                    TAG_CCU_SERVICE_INIT,
                    "BackgroundServiceInitiator -" + arg1.isBinderAlive + "," + arg1.toString() + "," + arg0.className + "," + arg1.interfaceDescriptor
                )
                if (arg1.isBinderAlive) {
                    //Todo : modbus USB Serial to tested with real device
                    usbModbusService = (arg1 as UsbModbusService.UsbBinder).service
                    LSerial.getInstance().setModbusUSBService(usbModbusService)
                    usbModbusService?.setHandler(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbModbusService = null
        }
    }

    private fun startConnectService(
        service: Class<*>,
        serviceConnection: ServiceConnection,
        extras: Bundle?
    ) {
        if (isAdvancedAhuProfile() || isConnectNodeDevice()) {
            CcuLog.i(L.TAG_CCU, "Connect module service is started")
            if (!UsbConnectService.SERVICE_CONNECTED) {
                val startService = Intent(context, service)
                if (extras != null && !extras.isEmpty()) {
                    val keys = extras.keySet()
                    for (key in keys) {
                        val extra = extras.getString(key)
                        startService.putExtra(key, extra)
                    }
                }
                context.startService(startService)
            }
            val bindingIntent = Intent(context, service)
            context.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            CcuLog.i(L.TAG_CCU, "Connect module service is not required")
        }
    }

    private fun isAdvancedAhuProfile(): Boolean {
        if (L.isSimulation()) return false
        try {
            val equip = CCUHsApi.getInstance()
                .readEntity(CommonQueries.SYSTEM_PROFILE)
            if (equip.isNotEmpty() && (equip["profile"].toString() == "vavAdvancedHybridAhuV2" || equip["profile"].toString() == "dabAdvancedHybridAhuV2")) {
                return true
            }
        } catch (e: Exception) {
            // Just not to block anything
            e.printStackTrace()
        }
        return false
    }

    private fun isConnectNodeDevice(): Boolean {
        if (L.isSimulation()) return false
        try {
            if (ConnectNodeUtil.Companion.isConnectNodeAvailable()) {
                return true
            }
        } catch (e: Exception) {
            // Just not to block anything
            e.printStackTrace()
        }
        return false
    }

    private val usbConnectConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            try {
                CcuLog.d(
                    TAG_CCU_SERVICE_INIT,
                    "BackgroundServiceInitiator -" + arg1.isBinderAlive + "," + arg1.toString() + "," + arg0.className + "," + arg1.interfaceDescriptor
                )
                if (arg1.isBinderAlive) {
                    usbConnectService = (arg1 as UsbConnectService.UsbBinder).service
                    LSerial.getInstance().setUsbConnectService(usbConnectService)

                    //TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
                    usbConnectService?.setHandler(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    fun unbindServices() {
        try {
            CcuLog.d(TAG_CCU_SERVICE_INIT, "Unbinding services")
            context.unregisterReceiver(mUsbReceiver)
            context.unbindService(usbConnection)
            context.unbindService(usbModbusConnection)
            context.unbindService(usbConnectConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}