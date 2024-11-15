package a75f.io.renatus.bacnet;

import static a75f.io.device.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE;
import static a75f.io.device.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_FD;
import static a75f.io.device.bacnet.BacnetConfigConstants.BACNET_FD_CONFIGURATION;
import static a75f.io.device.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_CONFIGURATION_TYPE;
import static a75f.io.renatus.UtilityApplication.context;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

import java.lang.ref.WeakReference;

import a75f.io.logger.CcuLog;

public class BacnetBackgroundTaskHandler {

    public static final String BACNET_FD_INTERVAL = "interval";
    public static final String BACNET_FD_IS_AUTO_ENABLED = "isAutoEnabled";
    private static final String MESSAGE = "message";
    private static final String DATA = "data";
    private static final String BACNET_FD_LABEL = "Foreign Device";
    private static final String TAG = "CCU_BACNET";

    private static class BacnetHandler extends Handler {
        private final WeakReference<BacnetBackgroundTaskHandler> handlerReference;

        BacnetHandler(BacnetBackgroundTaskHandler handler, Looper looper) {
            super(looper);
            handlerReference = new WeakReference<>(handler);
        }

        @Override
        public void handleMessage(Message msg) {
            BacnetBackgroundTaskHandler handler = handlerReference.get();
            if (handler != null) {
                if (msg.what == 1) {
                    handleFdConfig(msg);
                }
            }
        }

        private void handleFdConfig(Message msg) {
            int time = msg.getData().getInt(BACNET_FD_INTERVAL);
            boolean isAutoEnabled = msg.getData().getBoolean(BACNET_FD_IS_AUTO_ENABLED);
            Context applicationContext = context.getApplicationContext();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            String bacnetDeviceType = sharedPreferences.getString(BACNET_DEVICE_TYPE, null);


            CcuLog.d(TAG, "------handleMessage sendBroadcastToBacApp----" + time + "<--isAutoEnabled-->" + isAutoEnabled);
            if (isAutoEnabled && bacnetDeviceType!= null && bacnetDeviceType.equalsIgnoreCase(BACNET_DEVICE_TYPE_FD)) {
                sendBroadcastToBacApp();
                Message message = Message.obtain();
                message.what = 1;
                Bundle bundle = new Bundle();
                bundle.putInt(BACNET_FD_INTERVAL, time);
                bundle.putBoolean(BACNET_FD_IS_AUTO_ENABLED, isAutoEnabled);
                message.setData(bundle);
                handler.sendMessageDelayed(message, time * 1000L);
            } else {
                CcuLog.d(TAG, "auto enabled is false its time to remove messages and clear data");
                handler.removeMessages(1);
                clearFdData();
            }
        }

        private void sendBroadcastToBacApp() {
            Context applicationContext = context.getApplicationContext();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            String config = sharedPreferences.getString(BACNET_FD_CONFIGURATION, null);
            CcuLog.d(TAG, "------sendBroadcastToBacApp----"+config);
            if (config != null) {
                Intent intent = new Intent(BROADCAST_BACNET_APP_CONFIGURATION_TYPE);
                intent.putExtra(MESSAGE, BACNET_FD_LABEL);
                intent.putExtra(DATA, config);
                applicationContext.sendBroadcast(intent);
            }
        }

        private static void clearFdData() {
            Context applicationContext = context.getApplicationContext();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            sharedPreferences.edit().putString(BACNET_FD_CONFIGURATION, "").apply();
        }
    }


    private static HandlerThread handlerThread;
    private static BacnetHandler handler;

    public BacnetBackgroundTaskHandler() {
        initializeHandler();
    }

    private synchronized void initializeHandler() {
        if (handlerThread == null || !handlerThread.isAlive()) {
            handlerThread = new HandlerThread("BackgroundHandlerThread");
            handlerThread.start();
            handler = new BacnetHandler(this, handlerThread.getLooper());
        }
    }
    public static void stopThread() {
        if (handlerThread != null) {
            handler.removeCallbacksAndMessages(null);
            handlerThread.quitSafely();
            handlerThread = null;
            handler = null;
        }
    }

    public void postTask(Runnable task) {
        handler.post(task);
    }

    public void sendMessageToHandler(String data) {
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = data;
        handler.sendMessage(msg);
    }

    public void sendBroadCastToBacApp() {
        handler.sendBroadcastToBacApp();
    }

    public void removeOldMessages(int what) {
        handler.removeMessages(what);
    }

    public void removeCallBacks() {
        handler.removeCallbacksAndMessages(null);
    }

    public void sendMessageDelayed(Message message, long delay) {
        handler.sendMessageDelayed(message, delay);
    }

    public void stopHandler() {
        stopThread();
    }
}

