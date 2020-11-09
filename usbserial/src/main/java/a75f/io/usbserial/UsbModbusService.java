package a75f.io.usbserial;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by rmatt isOn 7/30/2017.
 */
public class UsbModbusService extends Service {

    public static final String TAG = UsbModbusService.class.getSimpleName();
    public static final byte ESC_BYTE = (byte) 0xD9;
    public static final byte SOF_BYTE = 0x00;
    public static final byte EOF_BYTE = 0x03;

    public static final String ACTION_USB_READY =
            "com.felhr.connectivityservices.USB_READY";
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
    public static final String ACTION_USB_DISCONNECTED =
            "com.felhr.usbservice.USB_DISCONNECTED";
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
    SerialState curState = SerialState.PARSE_INIT;
    int nCRC = 0;
    int nCurIndex;
    //NOTE: reduced buffer size to 256
    byte[] inDataBuffer = new byte[256];
    int nDataLength = 0;
    private IBinder binder = new UsbBinder();
    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;

    //private HashMap<String, UsbDevice> deviceList;
    private HashMap<UsbDevice, UsbDeviceConnection> connectionList;
    private HashMap<UsbDevice, UsbSerialDevice> serialPortList;

    ;
    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            Log.d(TAG, "OnReceive == " + arg1.getAction() + "," + serialPortConnected);
            Log.d(TAG, "OnReceive == " + arg0.toString() + " arg1:" + arg1.getData());
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                Log.d(TAG, "OnReceive == " + arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED));
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    //connection = usbManager.openDevice(device);
                    //new ConnectionThread().start();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Log.d(TAG, "USB PERMISSION NOT GRANTED == " + arg1.getAction());
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!serialPortConnected) {
                    findModbuSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
                }
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                if (serialPortConnected) {
                    serialPort.close();
                }
                if (serialPortList.size() > 0) {
                    for (Map.Entry<UsbDevice, UsbSerialDevice> usbEntry : serialPortList.entrySet()) {
                        {
                            if (usbEntry.getKey().getVendorId() == 0x0403 || usbEntry.getKey().getVendorId() == 0x1027 ||
                                    usbEntry.getKey().getVendorId() == 1003 || usbEntry.getKey().getVendorId() == 4292) {
                                if (!usbEntry.getValue().open()) {
                                    usbEntry.getValue().close();
                                    serialPortConnected = false;
                                }

                            }

                        }
                    }
                }
            }
        }
    };
    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */

    private UsbSerialInterface.UsbReadCallback modbusCallback =
            new UsbSerialInterface.UsbReadCallback() {
                @Override
                public void onReceivedData(byte[] data, int mLength) {
                    Log.d(TAG," onReceivedData ");
                    if (data.length > 0) {
                        int nMsg;
                        try {
                            nMsg = (data[0] & 0xff);
                            Log.d("CCU_SERIAL_MB",
                                    "Modbus message type received: " + nMsg);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Log.d("CCU_SERIAL_MB",
                                    "Modbus Bad message type received: " + String.valueOf(data[0] & 0xff) +
                                            e.getMessage());
                            return;
                        }
                        if (data.length < 3) {
                            return; //We need minimum bytes atleast 3 with msg and fsv address causing crash for WRM Pairing
                        }

                        Log.d("CCU_SERIAL_MB", "Modbus message type received: " + data.length + "," + String.valueOf(data[0] & 0xff) + "," + String.valueOf(data[1] & 0xff));
                        messageToClients(Arrays.copyOfRange(data, 0, mLength), true);
                    }
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


    private void parseBytes(byte[] inReg) {
        for (int nCount = 0; nCount < inReg.length; nCount++) {
            byte inData = inReg[nCount];
            switch (curState) {
                case PARSE_INIT:
                    if (PARSE_DEBUG) {
                        Log.i(TAG, "Parse INIT");
                    }
                    if (inData == ESC_BYTE) {
                        curState = SerialState.ESC_BYTE_RCVD;
                    }
                    break;
                case ESC_BYTE_RCVD:
                    if (PARSE_DEBUG) {
                        Log.i(TAG, "ESC_BYTE_RCVD");
                    }
                    if (inData == SOF_BYTE) {
                        curState = SerialState.SOF_BYTE_RCVD;
                    } else {
                        curState = SerialState.BAD_PACKET;
                    }
                    break;
                case SOF_BYTE_RCVD:
                    if (PARSE_DEBUG) {
                        Log.i(TAG, "SOF_BYTE_RCVD");
                    }
                    nDataLength = inData;
                    Log.i(TAG, "Data Len: " + nDataLength);
                    curState = SerialState.LEN_BYTE_RCVD;
                    break;
                case LEN_BYTE_RCVD:
                    if (PARSE_DEBUG) {
                        Log.i(TAG, "LEN_BYTE_RCVD");
                    }
                    if (nCurIndex == nDataLength) {
                        int nIncomingCRC = inData;
                        if (nIncomingCRC == nCRC) {
                            curState = SerialState.CRC_RCVD;
                        }
                    } else if (nCurIndex < nDataLength) {
                        inDataBuffer[nCurIndex] = inData;
                        nCRC ^= inData;
                        nCurIndex++;
                        if (inData == ESC_BYTE) {
                            curState = SerialState.ESC_BYTE_IN_DATA_RCVD;
                        }
                    } else {
                        curState = SerialState.BAD_PACKET;
                    }
                    break;
                case ESC_BYTE_IN_DATA_RCVD:
                    if (PARSE_DEBUG) {
                        Log.i(TAG, "END ESC_BYTE_IN_DATA_RCVD");
                    }
                    if (inData == ESC_BYTE) {
                        curState = SerialState.LEN_BYTE_RCVD;
                    } else {
                        curState = SerialState.BAD_PACKET;
                    }
                    break;
                case CRC_RCVD:
                    if (PARSE_DEBUG) {
                        Log.i(TAG, "END CRC RCVD");
                    }
                    if (inData == ESC_BYTE) {
                        curState = SerialState.ESC_BYTE_AS_END_OF_PACKET_RCVD;
                    } else {
                        curState = SerialState.BAD_PACKET;
                    }
                    break;
                case ESC_BYTE_AS_END_OF_PACKET_RCVD:
                    if (PARSE_DEBUG) {
                        Log.i(TAG, "case ESC_BYTE_AS_END_OF_PACKET_RCVD:");
                    }
                    if (inData == EOF_BYTE) {
                        curState = SerialState.DATA_AVAILABLE;
                    } else {
                        curState = SerialState.BAD_PACKET;
                    }
                    break;
            }
            if (curState == SerialState.DATA_AVAILABLE) {
                if (PARSE_DEBUG) {
                    Log.i(TAG, "DataAvailable sneding");
                }
                messageToClients(Arrays.copyOfRange(inDataBuffer, 0, nCurIndex), false);
                nCurIndex = 0;
                nCRC = 0;
                curState = SerialState.PARSE_INIT;
            }
            if (curState == SerialState.BAD_PACKET) {
                Log.d("SERIAL_RAW", "*******BAD PACKET RECEIVED*****");
                nCurIndex = 0;
                nCRC = 0;
                curState = SerialState.PARSE_INIT;
            }
        }
    }


    private void messageToClients(byte[] data, boolean isModbusData) {
        SerialAction serialAction = SerialAction.MESSAGE_FROM_SERIAL_PORT;
        if (isModbusData)
            serialAction = SerialAction.MESSAGE_FROM_SERIAL_MODBUS;
        SerialEvent serialEvent = new SerialEvent(serialAction, data);
        EventBus.getDefault().post(serialEvent);
    }


    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        this.context = this;
        serialPortConnected = false;
        UsbModbusService.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        connectionList = new HashMap<>();
        serialPortList = new HashMap<>();
        findModbuSerialPortDevice();
        /*if (connectionList.size() > 0) {
            startUsbThread();
        }*/
        running.start();
        runningList.start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        UsbModbusService.SERVICE_CONNECTED = false;
    }


    /* MUST READ about services
     * http://developer.android.com/guide/components/services.html
     * http://developer.android.com/guide/components/bound-services.html
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }


    private void findModbuSerialPortDevice() {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        Log.d(TAG, "findMBSerialPortDevice=" + usbDevices.size());
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                Log.i(TAG, "Modbus USB Device VID: " + deviceVID);
                Log.i(TAG, "Modbus USB Device PID: " + devicePID);
                if (deviceVID == 4292 || deviceVID == 1027) {
                    //if (deviceVID == 4292) {
                    boolean success = grantRootPermissionToUSBDevice(device);
                    connection = usbManager.openDevice(device);
                    if (success) {
                        ModbusRunnable modbusRunnable = new ModbusRunnable(device, connection);
                        new Thread(modbusRunnable).start();
                        Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                        UsbModbusService.this.getApplicationContext().sendBroadcast(intent);
                        keep = true;
                    } else {
                        Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                        UsbModbusService.this.getApplicationContext().sendBroadcast(intent);
                        keep = false;
                    }
                    Log.d(TAG, "Successfully Opened device instance "+deviceVID);
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep) {
                    break;
                }
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

    private boolean grantRootPermissionToUSBDevice(UsbDevice device) {
        IBinder b = ServiceManager.getService(Context.USB_SERVICE);
        IUsbManager service = IUsbManager.Stub.asInterface(b);
        Log.i("CCU_SERIAL", "Try connecting!");
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

    private final LinkedBlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<byte[]>();
    private final LinkedBlockingQueue<byte[]> modbusQueue = new LinkedBlockingQueue<byte[]>();
    Thread runningList = new Thread() {
        @Override
        public void run() {
            Log.d(TAG," runningList :run ");
            super.run();
            byte[] data;

            while (true) {
                try {
                    if (!serialPortConnected) {
                        Log.i(TAG, "Serial Port is not connected sleeping");
                        sleep(2000);
                        continue;
                    } else {
                        Log.i(TAG, "USB Serial Port is connected");
                    }
                    //TODO ignoring for now, until we complete modbus test
                    data = messageQueue.take();
                    Log.i(TAG, "USB Serial Port is connected = " + data.length + "," + messageQueue.size() + ","
                                                                                                    + modbusQueue.size());
                    for (Map.Entry<UsbDevice, UsbSerialDevice> usbEntry : serialPortList.entrySet()) {
                        if (usbEntry.getKey().getDeviceId() == 0x0403 || usbEntry.getKey().getDeviceId() == 0x1027 || usbEntry.getKey().getDeviceId() == 1003) {
                            Log.i(TAG, "USB Serial Port is connected = " + data.length + "," + messageQueue.size() + "," + modbusQueue.size());
                            UsbSerialDevice serialPort = usbEntry.getValue();
                            if (serialPort != null && (messageQueue.size() > 0)) {
                                byte buffer[] = new byte[128];
                                byte crc = 0;
                                byte nOffset = 0;
                                int len = data.length;
                                if (len >= 128)
                                    buffer = new byte[160]; //For OTA Updates
                                buffer[nOffset++] = (byte) (ESC_BYTE & 0xff);
                                buffer[nOffset++] = (byte) (SOF_BYTE & 0xff);
                                buffer[nOffset++] = (byte) (len & 0xff);
                                for (int i = 0; i < len; i++) {
                                    buffer[i + nOffset] = data[i]; // add payload to the tx buffer
                                    crc ^= data[i];             // calculate the new crc
                                    if (data[i] == (byte) (ESC_BYTE &
                                            0xff)) // if the data is equal to ESC byte then add another instance of that
                                    {
                                        nOffset++;
                                        buffer[i + nOffset] = data[i];
                                    }
                                }
                                buffer[nOffset + len] = (byte) (crc & 0xff);
                                nOffset++;
                                buffer[nOffset + len] = (byte) (ESC_BYTE & 0xff);
                                nOffset++;
                                buffer[nOffset + len] = (byte) (EOF_BYTE & 0xff);
                                nOffset++;
                                {
                                    String dp = "";
                                    for (int n = 0; n < data.length; n++)
                                        dp = dp + " " + String.valueOf((int) (data[n] & 0xff));
                                    Calendar curDate = GregorianCalendar.getInstance();
                                    Log.d(TAG, "[" + (data.length) + "]-[" + curDate.get(Calendar.HOUR_OF_DAY) + ":"
                                                                                + curDate.get(Calendar.MINUTE) + "] :" + dp);
                                    Log.d(TAG, "[" + (data.length) + "]-[" + curDate.get(Calendar.HOUR_OF_DAY) + ":"
                                                                                + curDate.get(Calendar.MINUTE) + "] :" + dp);
                                }
                                serialPort.write(Arrays.copyOfRange(buffer, 0, len + nOffset));
                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    };


    Thread running = new Thread() {
        @Override
        public void run() {
            super.run();
            byte[] data;

            while (true) {
                try {
                    if (!serialPortConnected) {
                        Log.i(TAG, "MB Serial Port is not connected sleeping");
                        sleep(2000);
                        continue;
                    }
                    UsbSerialDevice serialPort = null;
                    for (Map.Entry<UsbDevice, UsbSerialDevice> usbEntry : serialPortList.entrySet()) {
                        if (usbEntry.getKey().getVendorId() == 4292 || usbEntry.getKey().getVendorId() == 1027) {
                            serialPort = usbEntry.getValue();
                        }
                    }
                    if (serialPort != null && (modbusQueue.size() > 0)) {
                        data = modbusQueue.take();

                        serialPort.write(Arrays.copyOfRange(data, 0, data.length));
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

        }

    };

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void write(byte[] data) {

        if (isConnected()) {
            messageQueue.add(data);
        } else {
            messageQueue.clear();
            Log.i(TAG, "Serial is disconnected, message discarded");
        }
    }

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void modbusWrite(byte[] data) {

        if (isConnected()) {
            modbusQueue.add(data);

        } else {
            modbusQueue.clear();
            Log.i(TAG, "Serial is disconnected, modbus message discarded");
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

    /*
     * A simple thread to open a mobus serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */

    public class ModbusRunnable implements Runnable {

        UsbDevice usbDeviceMB;
        UsbDeviceConnection usbCnnection;
        UsbSerialDevice serialPortMB;

        public ModbusRunnable(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection) {
            usbDeviceMB = usbDevice;
            usbCnnection = usbDeviceConnection;
        }

        public void run() {
            serialPortMB = UsbSerialDevice.createUsbSerialDevice(usbDeviceMB, usbCnnection);
            Log.d(TAG," ModbusRunnable : run serialPortMB "+serialPortMB);
            if (serialPortMB != null) {
                if (serialPortMB.open()) {
                    serialPortConnected = true;
                    serialPortMB.setBaudRate(9600);
                    serialPortMB.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPortMB.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPortMB.setParity(UsbSerialInterface.PARITY_NONE);
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPortMB.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPortMB.read(modbusCallback);
                    serialPortMB.getCTS(ctsCallback);
                    serialPortMB.getDSR(dsrCallback);

                    try {
                        //sleep(2000); // sleep some. YMMV with different chips.
                        //this.wait(2000);
                        Thread.currentThread().sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    serialPortList.put(usbDeviceMB, serialPortMB);
                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                    context.sendBroadcast(intent);
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }

}

