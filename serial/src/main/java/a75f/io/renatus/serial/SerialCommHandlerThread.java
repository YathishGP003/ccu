package a75f.io.renatus.serial;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * Created by samjithsadasivan on 7/25/17.
 */

public class SerialCommHandlerThread extends HandlerThread {

    public static final String TAG = "SerialCommHandlerThread";
    public static final int SERIAL_COMM_DATA_READ = 1;
    public static final int SERIAL_COMM_DATA_WRITE = 2;
    public static final int SERIAL_COMM_CLOCK_UPDATE = 3;
    public static final int SERIAL_COMM_HEARTBEAT_UPDATE = 4;

    public static final long SERIAL_READ_POLLING_INTERVAL = 200;
    public static final long SERIAL_CLOCK_UPDATE_INTERVAL = 60 * 1000;
    public static final long SERIAL_HEARBEAT_UPDATE_INTERVAL = 60 * 1000;


    private Handler serialCommHandler;

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
