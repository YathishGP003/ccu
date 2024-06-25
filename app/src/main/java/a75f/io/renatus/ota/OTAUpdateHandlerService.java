package a75f.io.renatus.ota;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import java.util.Timer;
import java.util.TimerTask;

import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.usbserial.UsbService;

public class OTAUpdateHandlerService extends Service {


    private static final int THREE_MINUTE = 60000 * 3;
    private static final long TWELVE_HR_IN_MS = 12 * 60 * 60 * 1000;
    private boolean mIsTimerStarted = false;
    private Timer mTimeoutTimer;
    static long lastOTAUpdateTime = 0;

    private final BroadcastReceiver mOtaUpdateEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent passIntent = new Intent(getApplicationContext(), OTAUpdateService.class);
            passIntent.setAction(intent.getAction());
            passIntent.putExtras(intent);

            startService(passIntent);
            // Handle special timer-specific events
            switch(intent.getAction()) {
                case Globals.IntentActions.OTA_UPDATE_START:
                    startOtaUpdateTimeoutTimer();
                    break;
                case UsbService.ACTION_USB_DETACHED:
                    if (!OTAUpdateService.isCmOtaInProgress())
                        stopOtaUpdateTimeoutTimer();
                    break;

                case Globals.IntentActions.ACTIVITY_RESET:
                case Globals.IntentActions.OTA_UPDATE_NODE_REBOOT:
                case Globals.IntentActions.OTA_UPDATE_COMPLETE:
                    stopOtaUpdateTimeoutTimer();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        setOtaUpdateEventFilters();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mOtaUpdateEventReceiver);
    }

    private void setOtaUpdateEventFilters() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(Globals.IntentActions.ACTIVITY_MESSAGE);
        filter.addAction(Globals.IntentActions.ACTIVITY_RESET);
        filter.addAction(Globals.IntentActions.PUBNUB_MESSAGE);
        filter.addAction(UsbService.ACTION_USB_DETACHED);
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(Globals.IntentActions.LSERIAL_MESSAGE);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_START);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_CM_ACK);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_PACKET_REQ);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_NODE_REBOOT);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_TIMED_OUT);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_COMPLETE);

        registerReceiver(mOtaUpdateEventReceiver, filter);
    }

    private void startOtaUpdateTimeoutTimer() {
        if (!mIsTimerStarted) {
            mIsTimerStarted = true;
            mTimeoutTimer = new Timer();
             TimerTask otaTimeOutTask = new TimerTask() {
                @Override
                public void run() {
                   CcuLog.i(L.TAG_CCU_OTA_PROCESS, "OTA timeout check is running lastOTAUpdateTime :"+lastOTAUpdateTime);
                    if (lastOTAUpdateTime != 0 && ( System.currentTimeMillis()-lastOTAUpdateTime ) > TWELVE_HR_IN_MS) {
                        stopOtaUpdateTimeoutTimer();
                        sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_TIMED_OUT));
                    }
                }
            };
            mTimeoutTimer.schedule(otaTimeOutTask, 0, THREE_MINUTE);
        }
    }

     void stopOtaUpdateTimeoutTimer() {
        CcuLog.i(L.TAG_CCU_OTA_PROCESS, "OTA timeout stopOtaUpdateTimeoutTimer");
        lastOTAUpdateTime = 0;
        if(mTimeoutTimer != null ) {
            mTimeoutTimer.cancel();
        }
        mIsTimerStarted = false;
    }
}
