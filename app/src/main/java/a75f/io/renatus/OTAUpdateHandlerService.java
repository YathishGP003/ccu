package a75f.io.renatus;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import a75f.io.logic.Globals;

public class OTAUpdateHandlerService extends Service {

    private boolean mIsTimerStarted = false;
    private CountDownTimer mTimeoutTimer;

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

                case Globals.IntentActions.ACTIVITY_RESET:
                case Globals.IntentActions.OTA_UPDATE_PACKET_REQ:
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
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(Globals.IntentActions.LSERIAL_MESSAGE);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_START);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_PACKET_REQ);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_NODE_REBOOT);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_TIMED_OUT);
        filter.addAction(Globals.IntentActions.OTA_UPDATE_COMPLETE);

        registerReceiver(mOtaUpdateEventReceiver, filter);
    }

    private void startOtaUpdateTimeoutTimer() {
        if (!mIsTimerStarted) {
            mIsTimerStarted = true;

            mTimeoutTimer = new CountDownTimer(300000, 20000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    Log.d(OTAUpdateService.TAG, "[TIMER] Update will time out in " +
                            Math.ceil(millisUntilFinished / 1000) + "s");
                }

                @Override
                public void onFinish() {
                    Log.d(OTAUpdateService.TAG, "[TIMER] Update timed out, resetting");
                    stopOtaUpdateTimeoutTimer();

                    sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_TIMED_OUT));

                    //TODO notify something (PubNub?) that an update has timed out
                }
            }.start();
        }
    }

    private void stopOtaUpdateTimeoutTimer() {
        if(mTimeoutTimer != null ) {
            mTimeoutTimer.cancel();
        }

        mIsTimerStarted = false;
    }
}
