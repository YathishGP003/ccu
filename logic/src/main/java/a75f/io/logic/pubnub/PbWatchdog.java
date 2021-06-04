package a75f.io.logic.pubnub;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Pubnub SDK rarely stops delivering messages even while subscribed.
 * We here monitor the active pubnub message handling to track this and restart the app in an attempt to recover.
 * Ideally a CCU with any valid configuration should receive pubnubs frequently for its own updates or other
 * CCUs in the site. We wait for an hour before deciding to restart.
 */
public class PbWatchdog {
    
    private PbWatchdog() {
    }
    
    private static PbWatchdog instance = null;
    
    public static PbWatchdog getInstance() {
        if (instance == null) {
            instance = new PbWatchdog();
        }
        return instance;
    }
    
    /**
     * Monitor pubnub handling every 30 minutes to check if there has been any message handled in the last one hour.
     * @param context
     */
    public void startMonitoring(Context context, PbSubscriptionHandler pbSubscription) {
        Observable.interval(5, TimeUnit.MINUTES)
                  .subscribeOn(Schedulers.io())
                  .subscribe ( i -> {
                      //PubNub timeToken is in multiples of 10000.
                      Long lastHandledPbToken = PbPreferences.getLastHandledTimeToken(context)/10000;
                      if (lastHandledPbToken == 0) {
                          return;
                      }
                      
                      Long watchdogBiteTimeToken = System.currentTimeMillis() - 60 * 60 * 1000;
                      
                      if (pbSubscription.isPubnubSubscribed() && lastHandledPbToken < watchdogBiteTimeToken) {
                          CcuLog.d(L.TAG_CCU_PUBNUB, "PbWatchdog bite! PubNub not received for 60 minutes. " +
                                                     "Restart the app !!!!");
                          killRenatusApp();
                      } else {
                          CcuLog.d(L.TAG_CCU_PUBNUB,"PbWatchdog bark");
                      }
                  });
    }
    
    private void killRenatusApp() {
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        System.exit(10);
    }
    
}
