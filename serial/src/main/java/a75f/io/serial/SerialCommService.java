package a75f.io.serial;

import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.GregorianCalendar;

import a75f.io.bo.interfaces.ISerial;
import javolution.io.Struct;

import static a75f.io.serial.SerialCommManager.EOF_BYTE;
import static a75f.io.serial.SerialCommManager.ESC_BYTE;
import static a75f.io.serial.SerialCommManager.MESSAGETYPE;
import static a75f.io.serial.SerialCommManager.SOF_BYTE;
import static a75f.io.serial.SerialCommManager.STATES;

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


    private CMSerialDevice mCMDevice = null;

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

    public void sendData(byte[] bytes) {
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

            serialCommHandler.sendEmptyMessageDelayed(SERIAL_COMM_DATA_READ, 30000);
            serialCommHandler.sendEmptyMessageDelayed(SERIAL_COMM_CLOCK_UPDATE, 20000);
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

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mCMDevice = new CMSerialDevice();

        UsbDevice device = intent.getParcelableExtra("USB_DEVICE");

        if (mCMDevice.open(device)) {
            Log.d(TAG, "CM Device not connected. SerialService cant continue");
            stopSelf();
            return START_NOT_STICKY;
        }

        mSerialService = this;
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mSerialService = null;
        if (serialCommThread != null)
            serialCommThread.quit();

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

    public void broadcastSerialEvent(int[] data, int length){

        SerialCommManager.SerialCommEvent serialEvent = null;
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putIntArray("DATA", data);
        msg.setData(bundle);

        switch( MESSAGETYPE.values()[(data[0] & 0xff)]) {
            case FSV_PAIRING_CONFIRM:
                serialEvent = new SerialCommManager.SNPairingConfirmEvent();
                serialEvent.setMessage(msg);
                break;
            case FSV_REGULAR_UPDATE:
                break;
            case CM_REGULAR_UPDATE:
                break;
            case FSV_REBOOT:
                break;
            case CM_ERROR_REPORT:
                break;
            case CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE:
                break;
            case CM_TO_CCU_OVER_USB_SN_SET_TEMPERATURE_UPDATE:
                break;
            case CM_TO_CCU_OVER_USB_SN_REBOOT:
                break;
            case CM_TO_CCU_OVER_USB_FIRMWARE_PACKET_REQUEST:
                break;

        }

        EventBus.getDefault().post(serialEvent);

    }

    private void sendSerialData(byte[] byteArray) {
        mCMDevice.write(byteArray);
    }

    private void receiveSerialData(){

        byte[] rcvArray = new byte[1024];
        int rcvRet = mCMDevice.read(rcvArray);
        Log.d(TAG, "readStatus : "+rcvRet);
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
                broadcastSerialEvent(mDataBuffer, mCurIndex);
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
