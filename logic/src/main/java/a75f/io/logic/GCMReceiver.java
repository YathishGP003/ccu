package a75f.io.logic;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by Yinten on 10/13/2017.
 */

public class GCMReceiver extends WakefulBroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName
                comp = new ComponentName(context.getPackageName(), a75f.io.logic.GCMService.class
                                                                           .getName());
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}