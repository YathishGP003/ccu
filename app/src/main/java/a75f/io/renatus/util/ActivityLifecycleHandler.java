package a75f.io.renatus.util;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import a75f.io.alerts.AlertManager;
import a75f.io.logic.diag.DiagEquip;

/**
 * Created by mahesh on 14-11-2019.
 */
public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private Prefs prefs;
    private boolean isAppStarted;

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        prefs = new Prefs(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        isAppStarted = prefs.getBoolean("APP_START");
        if (isAppStarted){
            prefs.setBoolean("APP_RESTART", true);
        } else {
            prefs.setBoolean("APP_RESTART", false);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        prefs.setBoolean("APP_START", true);
        AlertManager.getInstance().clearAlertsWhenAppClose();
    }
}
