package a75f.io.serial;

import android.app.PendingIntent;
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

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import javolution.io.Struct;

import a75f.io.bo.interfaces.ISerial;
import a75f.io.util.GlobalUtils;

import static a75f.io.serial.SerialCommManager.*;
import static a75f.io.serial.SerialCommManager.MESSAGETYPE.CCU_CLOCK_UPDATE;

/**
 * Created by samjithsadasivan on 7/24/17.
 */

public class SerialCommService extends Service {

    public static final String TAG = "SerialCommService";

    public static final int SERIAL_COMM_DATA_READ = 1;
    public static final int SERIAL_COMM_DATA_WRITE = 2;
    public static final int SERIAL_COMM_CLOCK_UPDATE = 3;
    public static final int SERIAL_COMM_HEARTBEAT_UPDATE = 4;

    public static final long SERIAL_READ_POLLING_INTERVAL = 200;
    public static final long SERIAL_CLOCK_UPDATE_INTERVAL = 60 * 1000;
    public static final long SERIAL_HEARTBEAT_UPDATE_INTERVAL = 60 * 1000;

    public static final String ACTION_USB_PERMISSION =
                            "a75f.io.renatus.action.USB_PERMISSION";

    public static final boolean DEBUG_SERIAL_XFER = true;
                                    //Log.isLoggable(TAG, Log.VERBOSE);


    private static final String FTDI_VID_PID = "0403:6001";
    static final String CM_VID_PID = "03EB:2404";


    UsbDevice mDevice = null;
    UsbEndpoint mEpIN = null;
    UsbEndpoint mEpOUT = null;
    UsbDeviceConnection mUsbConnection = null;
    UsbManager mUsbManager = null;

    private int mDataLength = 0;
    private int mCurIndex = 0;
    private int mCRC = 0;
    private int mDataBuffer[] = new int[1024];

    static private SerialCommService mSerialService = null;

    SerialCommHandlerThread serialCommThread;

    private Handler serialCommHandler;

    static public SerialCommService getSerialService() {
        return mSerialService;
    }

    public void sendData(Struct payload) {
        Log.i("BYTE_BUFFER", payload.toString());

        byte[] bytes = new byte[payload.size()];


        for(int i = 0; i < payload.size(); i++)
        {
            bytes[i] = payload.getByteBuffer().get(i);
        }
        Log.i("BYTE_BUFFER", "Converted payload to bytes");

        sendSerialData(bytes);

    }

    class SerialCommHandlerThread extends HandlerThread {

        public SerialCommHandlerThread(String name, int priority) {
            super(name, priority);
        }

        @Override
        protected void onLooperPrepared() {
            serialCommHandler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {

                    switch (msg.what) {
                        case SERIAL_COMM_DATA_READ:
                            if (DEBUG_SERIAL_XFER)
                                Log.v(TAG, "SERIAL_COMM_DATA_READ");
                            receiveSerialData();
                            sendEmptyMessageDelayed(SERIAL_COMM_DATA_READ, SERIAL_READ_POLLING_INTERVAL);
                            break;
                        case SERIAL_COMM_DATA_WRITE:
                            if (DEBUG_SERIAL_XFER)
                                Log.v(TAG, "SERIAL_COMM_DATA_WRITE");
                            sendSerialData((byte[]) msg.obj);
                            break;
                        case SERIAL_COMM_CLOCK_UPDATE:
                            if (DEBUG_SERIAL_XFER)
                                Log.v(TAG, "SERIAL_COMM_CLOCK_UPDATE");
                            sendSerialData(new ClockUpdatePacket().build().toBytes());
                            sendEmptyMessageDelayed(SERIAL_COMM_CLOCK_UPDATE, SERIAL_CLOCK_UPDATE_INTERVAL);
                            break;
                        case SERIAL_COMM_HEARTBEAT_UPDATE:
                            //send heartbeat update
                            sendEmptyMessageDelayed(SERIAL_COMM_HEARTBEAT_UPDATE, SERIAL_HEARTBEAT_UPDATE_INTERVAL);
                            break;
                        default:
                            //place holder
                    }
                }
            };
        }
    }

    public Handler getSerialCommHandler (){
        if (serialCommHandler == null) {
            throw new IllegalStateException("Handler not Ready");
        }
        return serialCommHandler;
    }

    //TODO : Implement stopservice and quit handlerthread
    public void sendData(ISerial payLoad){
        serialCommHandler.obtainMessage (SERIAL_COMM_DATA_WRITE ,
                            payLoad.toBytes()).sendToTarget();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    //CCUApp.setScreenOn(true, true);
                    Log.e(TAG, "Usb Device dettached" + device.getDeviceName() + device.getClass() + device.getVendorId() + device.getProductId());
                    Toast.makeText(getApplicationContext(), R.string.cm_stopped, Toast.LENGTH_SHORT).show();
                    //cleanUp();
                    stopSelf();
                    mSerialService = null;

                }
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                Log.d(TAG, "ACTION_USB_PERMISSION: " + permission);
                if (permission) {
                    if (mUsbConnection == null) {
                        if (openDevice(mDevice) == false)
                            stopSelf();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.controller_notfound, Toast.LENGTH_SHORT).show();
                        stopSelf();

                    }
                }
            }

        }
    };


    @Override
    public void onCreate() {
        IntentFilter filter1 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter1.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // TODO move CM detection to appropriate place.
        mDevice = (UsbDevice) intent.getParcelableExtra("USB_DEVICE");

        if (mDevice == null) {
            Log.d(TAG, "CM Device not connected. SerialService cant continue");
            stopSelf();
            return START_NOT_STICKY;
        }

        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

        //TODO : Need to find a way to grant usb access permission by defualt.
        //This is temporary hack for testing purpose.
        boolean hasPermission = mUsbManager.hasPermission(mDevice);
        if (!hasPermission) {
            Log.d(TAG, "Request USB access permission");

            mUsbManager.requestPermission (mDevice, PendingIntent.getBroadcast
                            (getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0));

        } else {
            if (mUsbConnection == null) {
                if (openDevice(mDevice) == false)
                    stopSelf();
            } else {
                Toast.makeText(this, R.string.controller_notfound, Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        }

        mSerialService = this;
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mSerialService = null;
        if (serialCommThread != null)
            serialCommThread.quit();
        unregisterReceiver(mUsbReceiver);
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

    private boolean openDevice(UsbDevice usbDevice) {

        mUsbConnection = mUsbManager.openDevice(usbDevice);
        if (mUsbConnection == null) {
            Log.e(TAG, "Failed to open device, Shutting down service");
            return false;
        }
        for (int ifIndex = 0; ifIndex < usbDevice.getInterfaceCount(); ifIndex++) {

            if (!mUsbConnection.claimInterface(usbDevice.getInterface(ifIndex), true)) {
                Log.e(TAG, "Claim interface failed for " + ifIndex);
                continue;
            }
            if (String.format("%04X:%04X", usbDevice.getVendorId(),
                                        usbDevice.getProductId()).equals(FTDI_VID_PID)) {
                if (mUsbConnection.controlTransfer(0x40, 0, 0, 0, null, 0, 0) < 0)//reset
                    Log.d(TAG, "control transfer 1 failed");
                if (mUsbConnection.controlTransfer(0x40, 0, 1, 0, null, 0, 0) < 0)//clear Rx
                    Log.d(TAG, "control transfer 2 failed");
                if (mUsbConnection.controlTransfer(0x40, 0, 2, 0, null, 0, 0) < 0)
                    Log.d(TAG, "control transfer 3 failed");
                if (mUsbConnection.controlTransfer(0x40, 0x03, 0xC04E, 0, null, 0, 0) < 0)//baudrate 38400
                    Log.d(TAG, "control transfer 4 failed");
            }
            UsbInterface usbIf = usbDevice.getInterface(ifIndex);
            for (int epIndex = 0; epIndex < usbIf.getEndpointCount(); epIndex++) {
                switch(usbIf.getEndpoint(epIndex).getType()) {
                    case UsbConstants.USB_ENDPOINT_XFER_BULK:
                        Log.d(TAG, "Bulk Endpoint");
                        if (usbIf.getEndpoint(epIndex).getDirection() == UsbConstants.USB_DIR_IN)
                            mEpIN = usbIf.getEndpoint(epIndex);
                        else
                            mEpOUT = usbIf.getEndpoint(epIndex);
                        break;
                    case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
                        Log.d(TAG, "Control Endpoint");
                        break;
                    case UsbConstants.USB_ENDPOINT_XFER_INT:
                        Log.d(TAG, "Interrupt Endpoint");
                        break;
                    default:
                        Log.d(TAG, "Endpoint invalid");
                }

            }
        }
        if ((mEpIN == null) || (mEpOUT == null)) {
            Toast.makeText(this, R.string.no_endpoints_found, Toast.LENGTH_SHORT).show();
            return false;
        } else
            Toast.makeText(this, String.format("Endpoints found IN: 0x%02X, OUT: 0x%02X", mEpIN.getAddress(), mEpOUT.getAddress()), Toast.LENGTH_SHORT).show();

        serialCommThread = new SerialCommHandlerThread("SerialCommThread", Thread.NORM_PRIORITY);
        serialCommThread.start();

        return true;
    }

    //Could be moved to SerialCommTxrThread
    private void sendSerialData(byte[] byteArray) {

        byte buffer[] = new byte[1024];
        byte crc = 0;
        byte nOffset = 0;
        int len = byteArray.length;
        buffer[nOffset++] = (byte) (ESC_BYTE & 0xff);
        buffer[nOffset++] = (byte) (SOF_BYTE & 0xff);
        buffer[nOffset++] = (byte) (len & 0xff);

        for (int i = 0; i < len; i++) {
            buffer[i + nOffset] = byteArray[i]; // add payload to the tx buffer
            crc ^= byteArray[i];             // calculate the new crc
            if (byteArray[i] == (byte) (ESC_BYTE & 0xff)) // if the data is equal to ESC byte then add another instance of that
            {
                nOffset++;
                buffer[i + nOffset] = byteArray[i];
            }
        }
        buffer[nOffset + len] = (byte) (crc & 0xff);
        nOffset++;
        buffer[nOffset + len] = (byte) (ESC_BYTE & 0xff);
        nOffset++;
        buffer[nOffset + len] = (byte) (EOF_BYTE & 0xff);
        nOffset++;

        if (DEBUG_SERIAL_XFER) {
            String dp = "";
            for (int n = 0; n < nOffset + len; n++)
                dp = dp + " " + String.valueOf((int) (buffer[n] & 0xff));
            Calendar curDate = GregorianCalendar.getInstance();
            Log.v(TAG, "[" + (nOffset + len) + "]-[" + curDate.get(Calendar.HOUR_OF_DAY) + ":" + curDate.get(Calendar.MINUTE) + "] :" + dp);
        }

        if (mUsbConnection != null)
            mUsbConnection.bulkTransfer(mEpOUT, buffer, nOffset + len, 0);

    }

    private void receiveSerialData(){

        byte[] rcvArray = new byte[1024];

        int rcvRet = mUsbConnection.bulkTransfer(mEpIN, rcvArray, rcvArray.length, 0);
        if (rcvRet <= 2)
            return;

        if (DEBUG_SERIAL_XFER) {
            String dp = "";
            for (int n = 0; n < rcvRet; n++)
                dp = dp + " " + String.valueOf((int) (rcvArray[n] & 0xff));
            Calendar curDate = GregorianCalendar.getInstance();
            Log.v(TAG, "Raw Packet[Length:" + rcvRet + "]-[Time:" + curDate.get(Calendar.HOUR_OF_DAY) + ":" + curDate.get(Calendar.MINUTE) + "] :" + dp);
        }

        STATES curState = STATES.PARSE_INIT;

        for(int byteIndex = 0; byteIndex < rcvRet ; byteIndex++) {
            int inData = (int) (rcvArray[byteIndex] & 0xff);
            switch (curState) {
                case PARSE_INIT:
                    if (inData == ESC_BYTE)
                        curState = STATES.ESC_BYTE_RCVD;
                    break;
                case ESC_BYTE_RCVD:
                    if (inData == SOF_BYTE)
                        curState = STATES.SOF_BYTE_RCVD;
                    else
                        curState = STATES.BAD_PACKET;
                    break;
                case SOF_BYTE_RCVD:
                    mDataLength = inData;
                    curState = STATES.LEN_BYTE_RCVD;
                    break;
                case LEN_BYTE_RCVD:
                    if (mCurIndex == mDataLength) {
                        int incomingCRC = inData;
                        if (incomingCRC == mCRC)
                            curState = STATES.CRC_RCVD;
                        else {
                            if (DEBUG_SERIAL_XFER)
                                Log.d(TAG, "CRC Mismatch: Incoming: " + incomingCRC + "Calculated: " + mCRC);
                            curState = STATES.BAD_PACKET;
                        }
                    } else if (mCurIndex < mDataLength) {
                        mDataBuffer[mCurIndex] = inData;
                        mCRC ^= inData;
                        mCurIndex++;
                        if (inData == ESC_BYTE)
                            curState = STATES.ESC_BYTE_IN_DATA_RCVD;
                    } else
                        curState = STATES.BAD_PACKET;
                    break;
                case ESC_BYTE_IN_DATA_RCVD:
                    if (inData == ESC_BYTE)
                        curState = STATES.LEN_BYTE_RCVD;
                    else
                        curState = STATES.BAD_PACKET;
                    break;
                case CRC_RCVD:
                    if (inData == ESC_BYTE)
                        curState = STATES.ESC_BYTE_AS_END_OF_PACKET_RCVD;
                    else
                        curState = STATES.BAD_PACKET;
                    break;
                case ESC_BYTE_AS_END_OF_PACKET_RCVD:
                    if (inData == EOF_BYTE)
                        curState = STATES.DATA_AVAILABLE;
                    else
                        curState = STATES.BAD_PACKET;
                    break;

            }
            if (curState == STATES.DATA_AVAILABLE) {
                //broadcastToClients(inDataBuffer, nCurIndex);
                mCurIndex = 0;
                mCRC = 0;
                curState = STATES.PARSE_INIT;
            }

            if (curState == STATES.BAD_PACKET) {
                if (DEBUG_SERIAL_XFER)
                    Log.d(TAG, "*******BAD PACKET RECEIVED*****");
                mCurIndex = 0;
                mCRC = 0;
                curState = STATES.PARSE_INIT;
            }
        }

    }


}
