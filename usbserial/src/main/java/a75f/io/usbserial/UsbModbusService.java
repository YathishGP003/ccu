package a75f.io.usbserial;

import static a75f.io.usbserial.UsbSerialUtil.DEVICE_ID_FTDI;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

/**
 * Created by rmatt isOn 7/30/2017.
 */
public class UsbModbusService extends Service {

    public static final String TAG = "CCU_USB_MODBUS";
    public static final String ACTION_USB_MODBUS_READY =
            "com.felhr.connectivityservices.USB_MODBUS_READY";
    public static final String ACTION_USB_ATTACHED =
            "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED =
            "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED =
            "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED =
            "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED =
            "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_MODBUS_DISCONNECTED =
            "com.felhr.usbservice.USB_MODBUS_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING =
            "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING =
            "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 38400;
    // BaudRate. Change this value if you need
    private static final boolean PARSE_DEBUG = false;
    public static boolean SERVICE_CONNECTED = false;
    private IBinder binder = new UsbBinder();
    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private volatile boolean serialPortConnected;
    
    public SerialInputStream serialInputStream;
    private int reconnectCounter = 0;
    
    private static Timer usbPortScanTimer = new Timer();

    public static String TAG_CCU_SERIAL = "CCU_SERIAL";

    private UsbDeviceItem usbModbusDevice = null;
    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                CcuLog.d(TAG, "OnReceive == " + arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED));
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    //connection = usbManager.openDevice(device);
                    //new ConnectionThread().start();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    CcuLog.d(TAG, "USB PERMISSION NOT GRANTED == " + arg1.getAction());
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                UsbDevice attachedDevice = arg1.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (UsbSerialUtil.getModbusDeviceCom1(attachedDevice, UsbPrefHelper.getUsbDeviceList(context)) != null
                                            && !serialPortConnected) {
                    CcuLog.d(TAG,"Modbus Serial device connected "+attachedDevice.toString());
                    scheduleUsbConnectedEvent();
                    UsbUtil.writeUsbEvent(attachedDevice, "Attached");
                }
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                UsbDevice detachedDevice = arg1.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (UsbSerialUtil.getModbusDeviceCom1(detachedDevice, UsbPrefHelper.getUsbDeviceList(context)) != null) {
                    usbPortScanTimer.cancel();
                    CcuLog.d(TAG,"Modbus Serial device disconnected "+detachedDevice.toString());
                    if (serialPortConnected && serialPort != null) {
                        serialPort.close();
                        serialPort = null;
                    }
                    serialPortConnected = false;
                    Intent intent = new Intent(ACTION_USB_MODBUS_DISCONNECTED);
                    arg0.sendBroadcast(intent);

                    UsbUtil.writeUsbEvent(detachedDevice, "Detached");
                }
            }
            CcuLog.d(TAG,"UsbModbusService: OnReceive == "+arg1.getAction()+","+serialPortConnected);
        }
    };
    
    private void scheduleUsbConnectedEvent() {
        CcuLog.d(TAG, "USB_CONNECTED Event received , Schedule port scan for Modbus device");
        usbPortScanTimer.cancel();
        usbPortScanTimer = new Timer();
        usbPortScanTimer.schedule(new TimerTask() {
            @Override public void run() {
                findModbusSerialPortDevice();
            }
        }, 2000);
    }
    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */

    private UsbSerialInterface.UsbReadCallback modbusCallback =
            (data, mLength) -> {
                if (data.length > 0) {
                    int nMsg;
                    try {
                        nMsg = (data[0] & 0xff);
                        CcuLog.d(TAG, "onReceivedData: Slave: " + nMsg);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        CcuLog.d(TAG,
                                "Modbus Bad message type received: " + String.valueOf(data[0] & 0xff) +
                                        e.getMessage());
                        return;
                    }
                    /*if (data.length < 3) {
                        return; //We need minimum bytes atleast 3 with msg and fsv address causing crash for WRM Pairing
                    }*/

                    messageToClients(Arrays.copyOfRange(data, 0, mLength), true);
                }
            };
    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback =
            new UsbSerialInterface.UsbCTSCallback() {
                @Override
                public void onCTSChanged(boolean state) {
                    if (mHandler != null) {
                        mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
                    }
                }
            };
    /*
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback =
            new UsbSerialInterface.UsbDSRCallback() {
                @Override
                public void onDSRChanged(boolean state) {
                    if (mHandler != null) {
                        mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
                    }
                }
            };

    private void messageToClients(byte[] data, boolean isModbusData) {
        if (!CCUHsApi.getInstance().isCcuReady()) {
            return;
        }
        SerialAction serialAction = SerialAction.MESSAGE_FROM_SERIAL_PORT;
        if (isModbusData)
            serialAction = SerialAction.MESSAGE_FROM_SERIAL_MODBUS1;
        SerialEvent serialEvent = new SerialEvent(serialAction, data);
        EventBus.getDefault().post(serialEvent);
        
        if (serialInputStream != null) {
            CcuLog.d(TAG, " Read data from MB : ");
            serialInputStream.feedSerialData(data);
        }
    }


    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        this.context = this;
        CcuLog.d(TAG, "--onCreate-- UsbModbusService");
        serialPortConnected = false;
        UsbModbusService.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        try {
            findModbusSerialPortDevice();
        } catch (SecurityException e) {
            //Android throws SecurityException if the application process is not granted android.permission.MANAGE_USB
            CcuLog.e(TAG, "USB Security Exception", e);
            Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
            UsbModbusService.this.getApplicationContext().sendBroadcast(intent);
        }
        
        running.start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CcuLog.d(TAG, "--onStartCommand-- UsbModbusService");
        try {
            doModbusUsbManagerMigrationIfNeeded();
        } catch (Exception e) {
            //It can fail if the device does not have a valid serial.
            e.printStackTrace();
            CcuLog.e(TAG, "Modbus USB Manager migration failed", e);
        }
        //scheduleUsbConnectedEvent();
        return Service.START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        CcuLog.d(TAG, "--onDestroy--");
        shouldStop = true;
        running.interrupt();
        unregisterReceiver(usbReceiver);
        serialPortConnected = false;
        UsbModbusService.SERVICE_CONNECTED = false;
        super.onDestroy();
        if (serialPort != null) {
            CcuLog.d(TAG,"Closing serial port");
            serialPort.close();
            serialPort = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        CcuLog.d(TAG, "--onBind-- UsbModbusService");
        return binder;
    }


    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }

    private void findModbusSerialPortDevice() {
        if (serialPortConnected) {
            CcuLog.d(TAG, "findModbusSerialPortDevice : Modbus Serial Port already connected. Returning");
            return;
        }
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        CcuLog.d(TAG, "findMBSerialPortDevice = " + usbDevices.size());
        List<UsbDeviceItem> configuredUsbDevices = UsbPrefHelper.getUsbDeviceList(getApplicationContext());
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                CcuLog.e(TAG, "UsbModbusService.findModbusSerialPortDevice: iterating device : " + device.getSerialNumber());
                usbModbusDevice= UsbSerialUtil.getModbusDeviceCom1(device, configuredUsbDevices);
                if (usbModbusDevice != null) {
                    CcuLog.d(TAG, "Modbus configuration found "+usbModbusDevice);
                }
                if (usbModbusDevice != null) {
                    CcuLog.e(TAG, "UsbModbusService.findModbusSerialPortDevice: Modbus configured device found= " + device.getSerialNumber());
                    boolean success = false;
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                        success = grantRootPermissionToUSBDevice(device);
                    }else {
                        success = true;
                    }
                    connection = usbManager.openDevice(device);
                    if (connection != null) {
                        if (success) {
                            serialPortConnected = true;
                            ModbusRunnable modbusRunnable = new ModbusRunnable(device, connection);
                            new Thread(modbusRunnable).start();
                            Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                            UsbModbusService.this.getApplicationContext().sendBroadcast(intent);
                            keep = true;
                        } else {
                            CcuLog.d(TAG, "Closing the connection "+connection);
                            connection.close();
                            Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                            UsbModbusService.this.getApplicationContext().sendBroadcast(intent);
                            keep = false;
                        }
                        CcuLog.d(TAG, "Opened Serial MODBUS device "+device.toString());
                    } else {
                        CcuLog.d(TAG, "Failed to Open Serial MODBUS device "+device.toString());
                    }
                    CcuLog.d(TAG, "Modbus device found ");
                    break;
                } else {
                    connection = null;
                    device = null;
                    CcuLog.e(TAG, "UsbModbusService.findModbusSerialPortDevice: assigned value to device if it is not modbus= null");
                }
                if (!keep) {
                    break;
                }
                sleep(1000);
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }
    
    /**
     * Scans and initializes serial ports without sending broadcast messages.
     */
    private void scanSerialPortSilentlyForMbDevice() {
    
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        CcuLog.d(TAG, "scanSerialPortSilentlyForMbDevice=" + usbDevices.size());
        if (!usbDevices.isEmpty()) {
            connection = null;
            device = null;
            List<UsbDeviceItem> configuredUsbDevices = UsbPrefHelper.getUsbDeviceList(getApplicationContext());
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                CcuLog.e(TAG, "UsbModbusService.scanSerialPortSilentlyForMbDevice(): device " + device);
                usbModbusDevice= UsbSerialUtil.getModbusDeviceCom1(device, configuredUsbDevices);
                if (usbModbusDevice != null) {
                    CcuLog.d(TAG, "Modbus configuration found "+usbModbusDevice);
                    boolean success = false;
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                        success = grantRootPermissionToUSBDevice(device);
                    }else {
                        success = true;
                    }
                    if(connection != null)
                    {
                        CcuLog.d(TAG, "Closing the connection "+connection);
                        connection.close();
                    }
                    connection = usbManager.openDevice(device);
                    if (connection != null && success) {
                        ModbusRunnable modbusRunnable = new ModbusRunnable(device, connection);
                        new Thread(modbusRunnable).start();
                        CcuLog.d(TAG, "Opened Serial MODBUS device "+device.getProductName());
                    } else {
                        CcuLog.d(TAG, "Failed to Open Serial MODBUS device "+device.getProductName());
                    }
                    CcuLog.d(TAG, "Modbus device found ");
                    break;
                }
                sleep(1000);
            }
        }
    }

    private boolean grantRootPermissionToUSBDevice(UsbDevice device) {
       IBinder b = ServiceManager.getService(Context.USB_SERVICE);
        IUsbManager service = IUsbManager.Stub.asInterface(b);
        CcuLog.i(TAG_CCU_SERIAL, "Try connecting!");
        // There is a device connected to our Android device. Try to open it as a Serial Port.
        try {
            service.grantDevicePermission(device, getApplicationInfo().uid);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }


    public ApplicationInfo getApplicationInfo() {
        PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai = null;
        try {
            ai = pm.getApplicationInfo("a75f.io.renatus", 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return ai;
    }


    public void setDebug(boolean debug) {
        serialPort.debug(debug);
    }

    public void setDebug(UsbSerialDevice usbSerialPort, boolean debug) {
        usbSerialPort.debug(debug);
    }

    /************************************************************************************************************************/
    
    private final LinkedBlockingQueue<byte[]> modbusQueue = new LinkedBlockingQueue<byte[]>();

    private volatile boolean shouldStop = false;

    Thread running = new Thread() {
        @Override
        public void run() {
            super.run();
            byte[] data;

            while (!shouldStop) {
                try {
                    if (!serialPortConnected) {
                        CcuLog.i(TAG, "MB Serial Port is not connected sleeping");
                        //When only modbus is disconnected
                        if (++reconnectCounter >= 30) {
                            CcuLog.i(TAG, "scanSerialPortSilentlyForMbDevice");
                            ArrayList<HashMap<Object, Object>> modbusEquips = CCUHsApi.getInstance()
                                                                                      .readAllEntities("equip and modbus");
                            if (modbusEquips.size() > 0) {
                                scanSerialPortSilentlyForMbDevice();
                            }
                            reconnectCounter = 0;
                        }
                        sleep(30000);
                        continue;
                    }

                    if (serialPort != null ) {
                        data = modbusQueue.poll(1, TimeUnit.SECONDS);
                        if (data != null && data.length > 0) {
                            CcuLog.i(TAG, "Write MB data : " + Arrays.toString(data) +" Hex : "+byteArrayToHex(data));
                            serialPort.write(Arrays.copyOfRange(data, 0, data.length));
                            Thread.sleep(50);
                        }
                    }
                } catch (Exception exception) {
                    CcuLog.i(TAG, "Modbus serial transaction stopped ", exception);
                    exception.printStackTrace();
                }
            }

        }

    };


    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b)).append(" ");

        return sb.toString();
    }

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void write(byte[] data) {
        CcuLog.i(TAG, " write TODO");
    }

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void modbusWrite(byte[] data) {
        
        if (isConnected()) {
            modbusQueue.add(data);

        } else {
            modbusQueue.clear();
            CcuLog.i(TAG, "Serial is disconnected, modbus message discarded");
        }
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public boolean isConnected() {
        return serialPortConnected;
    }


    private enum SerialState {
        PARSE_INIT, ESC_BYTE_RCVD, SOF_BYTE_RCVD, LEN_BYTE_RCVD, ESC_BYTE_IN_DATA_RCVD, CRC_RCVD,
        ESC_BYTE_AS_END_OF_PACKET_RCVD, BAD_PACKET, DATA_AVAILABLE
    }

    public class UsbBinder extends Binder {
        public UsbModbusService getService() {
            return UsbModbusService.this;
        }
    }

    public class ModbusRunnable implements Runnable {
        
        public ModbusRunnable(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection) {
            device = usbDevice;
            connection = usbDeviceConnection;
            CcuLog.e(TAG, "UsbModbusService.ModbusRunnable: assigned value to device = " + device);
        }

        public void run() {
            try {
                configureMbSerialPort();
            } catch (Exception e) {
                //Unstable USB connections would result in configuration failures.
                CcuLog.e(TAG, "Modbus: configureMbSerialPort Failed "+e.getMessage());
                serialPortConnected = false;
                e.printStackTrace();
            }
        }
    
        private void configureMbSerialPort() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            CcuLog.d(TAG," ModbusRunnable : run serialPortMB "+serialPort+" connection: "+connection);
            CcuLog.d(TAG,"device name: "+device.getSerialNumber()+" confguration : "+usbModbusDevice);
            if (serialPort != null && usbModbusDevice != null) {
                if (serialPort.open()) {
                    serialPort.setBaudRate(usbModbusDevice.getModbusConfig().getBaudRate());
                    serialPort.setModbusDevice(true);
                    serialPort.setDataBits(usbModbusDevice.getModbusConfig().getDataBits());
                    serialPort.setStopBits(usbModbusDevice.getModbusConfig().getStopBits());
                    serialPort.setParity(usbModbusDevice.getModbusConfig().getParity());
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(modbusCallback);
                    serialPort.getCTS(ctsCallback);
                    serialPort.getDSR(dsrCallback);
                    try {
                        //sleep(2000); // sleep some. YMMV with different chips.
                        //this.wait(2000);
                        Thread.currentThread().sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_MODBUS_READY);
                    context.sendBroadcast(intent);
                    CcuLog.d(TAG,"device opened: - device: "+device.getSerialNumber());
                } else {
                    CcuLog.d(TAG,"closing USB serial device "+device.getSerialNumber()+"," +
                            " because this device interface can not be claimed at this moment");
                    serialPort.close();
                    serialPort = null;
                    Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                    context.sendBroadcast(intent);
                    serialPortConnected = false;
                }
            } else {
                CcuLog.d(TAG, "Closing USB serial device " + device.getSerialNumber() + "," +
                        " because it is null or no configuration found");
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
                serialPortConnected = false;
            }
        }
    }

    private int getIntPref(String key, int defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    public void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doModbusUsbManagerMigrationIfNeeded() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean migrated = prefs.getBoolean("modbus_usb_manager_migrated", false);
        if (!migrated) {
            int baudRate = getIntPref("mb_baudrate", 0);
            if (baudRate == 0) {
                CcuLog.d(TAG, "No modbus devices configured. No migration needed");
                prefs.edit().putBoolean("modbus_usb_manager_migrated", true).apply();
                return;
            }
            CcuLog.d(TAG, "Modbus USB Manager migration started");

            HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                UsbDevice usbDevice = entry.getValue();
                if (UsbSerialUtil.isModbusDevice(usbDevice, context)) {
                    CcuLog.d(TAG, "Modbus FTDI USB device found "+usbDevice);
                    List<UsbDeviceItem> usbDeviceItems = UsbPrefHelper.getUsbDeviceList(this);
                    if (usbDeviceItems.isEmpty()) {
                        UsbDeviceItem usbDeviceItem = new UsbDeviceItem(usbDevice.getSerialNumber(),
                                String.valueOf(usbDevice.getVendorId()), String.valueOf(usbDevice.getProductId()), "Modbus", "COM 1", usbDevice.getProductName());
                        ModbusConfig usbModbusConfig = new ModbusConfig(Objects.requireNonNull(usbDevice.getSerialNumber()),
                                baudRate,
                                getIntPref("mb_parity", 0),
                                getIntPref("mb_databits", 8),
                                getIntPref("mb_stopbits", 1));
                        CcuLog.d(TAG, "Modbus USB device config "+usbModbusConfig);
                        usbDeviceItem.setModbusConfig(usbModbusConfig);
                        usbDeviceItems.add(usbDeviceItem);
                        UsbPrefHelper.saveUsbDeviceList(this, usbDeviceItems);
                    } else {
                        CcuLog.d(TAG, "UsbManager is already configured:  Skipping modbus migration");
                    }
                    prefs.edit().putBoolean("modbus_usb_manager_migrated", true).apply();
                    CcuLog.d(TAG, "Modbus USB Manager migration completed");
                    break;
                }
            }
        }
        scheduleUsbConnectedEvent();
    }

    public String getActiveComPort() {
        if (usbModbusDevice != null) {
            return usbModbusDevice.getPort();
        }
        return "";
    }

}

