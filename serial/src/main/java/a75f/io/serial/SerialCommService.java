package a75f.io.serial;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

import a75f.io.bo.interfaces.ISerial;

/**
 * Created by samjithsadasivan on 7/24/17.
 */

public class SerialCommService extends Service {

    private static final String FTDI_VID_PID = "0403:6001";
    private static final String TAG = "SerialCommService";
    static final String CM_VID_PID = "03EB:2404";


    UsbDevice device = null;
    UsbEndpoint epIN = null;
    UsbEndpoint epOUT = null;
    UsbDeviceConnection conn = null;

    private boolean usbDetachReceiverRegistered = false;

    static private SerialCommService mSerialService = null;

    SerialCommHandlerThread serialCommThread;

    private Handler serialCommHandler;


    static public SerialCommService getSerialService() {
        return mSerialService;
    }

    class SerialCommHandlerThread extends HandlerThread {

        public static final String TAG = "SerialCommHandlerThread";
        public static final int SERIAL_COMM_DATA_READ = 1;
        public static final int SERIAL_COMM_DATA_WRITE = 2;
        public static final int SERIAL_COMM_CLOCK_UPDATE = 3;
        public static final int SERIAL_COMM_HEARTBEAT_UPDATE = 4;

        public static final long SERIAL_READ_POLLING_INTERVAL = 200;
        public static final long SERIAL_CLOCK_UPDATE_INTERVAL = 60 * 1000;
        public static final long SERIAL_HEARBEAT_UPDATE_INTERVAL = 60 * 1000;


        public SerialCommHandlerThread(String name, int priority) {
            super(name, priority);
        }

        @Override
        protected void onLooperPrepared() {
            serialCommHandler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {

                    switch(msg.what) {
                        case SERIAL_COMM_DATA_READ:
                            Log.v(TAG, "SERIAL_COMM_DATA_READ");
                            // read USB Data

                            sendEmptyMessageDelayed(SERIAL_COMM_DATA_READ,SERIAL_READ_POLLING_INTERVAL);
                            break;
                        case SERIAL_COMM_DATA_WRITE:
                            Log.v(TAG,("SERIAL_COMM_DATA_WRITE");
                            break;
                        case SERIAL_COMM_CLOCK_UPDATE:
                            //send current time
                            sendEmptyMessageDelayed(SERIAL_COMM_CLOCK_UPDATE, SERIAL_CLOCK_UPDATE_INTERVAL);
                            break;
                        case SERIAL_COMM_HEARTBEAT_UPDATE:
                            //send heartbeat update
                            sendEmptyMessageDelayed(SERIAL_COMM_HEARTBEAT_UPDATE, SERIAL_HEARBEAT_UPDATE_INTERVAL);
                            break;
                        default:
                            //place holder
                    }
                }
            };
        }

        public Handler getHandler (){

            if (serialCommHandler == null) {
                throw new IllegalStateException("Handler not Ready");
            }

            return serialCommHandler;

        }

    }

    private void sendData(ISerial payLoad){
    serialCommHandler.obtainMessage( SerialCommHandlerThread.SERIAL_COMM_DATA_WRITE ,
                            payLoad.toBytes());


    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    //CCUApp.setScreenOn(true, true);
                    Log.w("SERIAL_DEBUG", "Usb Device dettached" + device.getDeviceName() + device.getClass() + device.getVendorId() + device.getProductId());
                    Toast.makeText(getApplicationContext(), R.string.cm_stopped, Toast.LENGTH_SHORT).show();
                    //cleanUp();
                    stopSelf();

                }
            }
        }
    };


    @Override
    public void onCreate() {
        IntentFilter filter1 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter1);
        usbDetachReceiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mSerialService = this;
        if (conn == null) {
            if (intent != null)
                device = (UsbDevice) intent.getParcelableExtra("USB_DEVICE");
            if (device != null) {
                if (openDevice(device) == false)
                    stopSelf();
            }
            else {
                Toast.makeText(this, R.string.controller_notfound, Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //cleanUp();
    }

    public class LocalBinder extends Binder {
        SerialCommService getService() {
            return SerialCommService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private boolean openDevice(UsbDevice d) {
        UsbManager usbm = (UsbManager) getSystemService(USB_SERVICE);
        conn = usbm.openDevice(d);
        if (conn == null) {
            Log.e("SERIAL_DEBUG", "Failed to open device, Shutting down service");
            return false;
        }
        for (int n = 0; n < d.getInterfaceCount(); n++) {

            if (!conn.claimInterface(d.getInterface(n), true)) {
                Log.d("SERIAL_DEBUG", "Claim interface failed for " + n);
                continue;
            }
            if (String.format("%04X:%04X", d.getVendorId(), d.getProductId()).equals(FTDI_VID_PID)) {
                if (conn.controlTransfer(0x40, 0, 0, 0, null, 0, 0) < 0)//reset
                    Log.d("SERIAL_DEBUG", "control transfer 1 failed");
                if (conn.controlTransfer(0x40, 0, 1, 0, null, 0, 0) < 0)//clear Rx
                    Log.d("SERIAL_DEBUG", "control transfer 2 failed");
                if (conn.controlTransfer(0x40, 0, 2, 0, null, 0, 0) < 0)
                    Log.d("SERIAL_DEBUG", "control transfer 3 failed");
                if (conn.controlTransfer(0x40, 0x03, 0xC04E, 0, null, 0, 0) < 0)//baudrate 38400
                    Log.d("SERIAL_DEBUG", "control transfer 4 failed");
            }
            UsbInterface usbIf = d.getInterface(n);
            for (int i = 0; i < usbIf.getEndpointCount(); i++) {
                if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    Log.d("SERIAL_DEBUG", "Bulk Endpoint");
                    if (usbIf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
                        epIN = usbIf.getEndpoint(i);
                    else
                        epOUT = usbIf.getEndpoint(i);
                } else if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                    Log.d("SERIAL_DEBUG", "Control Endpoint");
                } else if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    Log.d("SERIAL_DEBUG", "Interrupt Endpoint");
                } else {
                    Log.d("SERIAL_DEBUG", "Not Bulk");
                }
            }
        }
        if ((epIN == null) || (epOUT == null)) {
            Toast.makeText(this, R.string.no_endpoints_found, Toast.LENGTH_SHORT).show();
            return false;
        } else
            Toast.makeText(this, String.format("Endpoints found IN: 0x%02X, OUT: 0x%02X", epIN.getAddress(), epOUT.getAddress()), Toast.LENGTH_SHORT).show();

        serialCommThread = new SerialCommHandlerThread("SerialCommThread", Thread.NORM_PRIORITY);
        serialCommThread.start();
        return true;
    }

    //Could be moved to SerialCommTxrThread
    public void sendData(byte[] byteArray) {

        byte buffer[] = new byte[1024];
        byte crc = 0;
        byte nOffset = 0;
        int len = byteArray.length;
        buffer[nOffset++] = (byte) (SerialCommManager.ESC_BYTE & 0xff);
        buffer[nOffset++] = (byte) (SerialCommManager.SOF_BYTE & 0xff);
        buffer[nOffset++] = (byte) (len & 0xff);

        for (int i = 0; i < len; i++) {
            buffer[i + nOffset] = byteArray[i]; // add payload to the tx buffer
            crc ^= byteArray[i];             // calculate the new crc
            if (byteArray[i] == (byte) (SerialCommManager.ESC_BYTE & 0xff)) // if the data is equal to ESC byte then add another instance of that
            {
                nOffset++;
                buffer[i + nOffset] = byteArray[i];
            }
        }
        buffer[nOffset + len] = (byte) (crc & 0xff);
        nOffset++;
        buffer[nOffset + len] = (byte) (SerialCommManager.ESC_BYTE & 0xff);
        nOffset++;
        buffer[nOffset + len] = (byte) (SerialCommManager.EOF_BYTE & 0xff);
        nOffset++;

        if (true) {
            String dp = "";
            for (int n = 0; n < nOffset + len; n++)
                dp = dp + " " + String.valueOf((int) (buffer[n] & 0xff));
            Calendar curDate = GregorianCalendar.getInstance();
            Log.d("SERIAL_OUT", "[" + (nOffset + len) + "]-[" + curDate.get(Calendar.HOUR_OF_DAY) + ":" + curDate.get(Calendar.MINUTE) + "] :" + dp);
        }

        if (conn != null)
            conn.bulkTransfer(epOUT, buffer, nOffset + len, 0);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



}
