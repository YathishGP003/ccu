package a75f.io.logic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;


import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.autocommission.remoteSession.RemoteSessionStatus;

public class RemoteAccessSessionBroadCastReceiver extends BroadcastReceiver {

    private static final String REMOTE_SESSION_STATUS = "remote_session_status";
    private static final String CUSTOM_BROADCAST_ACTION = "com.remote.SESSION_PROGRESS";
    private static final String SHARED_PREFERENCE_NAME = "remote_status_pref";
    private static final String KEY_SCREEN_SHARING_STATUS = "screen_sharing_status";
    public static final String ACTION_REMOTE_APP_REGISTERED = "REMOTE_APP_REGISTERED";
    public static final String ACTION_SCREEN_SHARING_AVAILABLE = "SCREEN_SHARING_AVAILABLE";
    public static final String ACTION_SCREEN_SHARING_START = "SCREEN_SHARING_START";
    public static final String ACTION_SCREEN_SHARING_STOP = "SCREEN_SHARING_STOP";
    public static final String ACTION_SCREEN_TIMEOUT = "SCREEN_SHARING_TIMEOUT";
    public static final String ACTION_SCREEN_SHARING_PERMISSION_NEEDED = "SCREEN_SHARING_PERMISSION_NEEDED";
    public static final String REMOTE_APP_KILLED = "REMOTE_APP_KILLED";
    public  static boolean isTimeOutBroadcast = false;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();

        if (action.equals(CUSTOM_BROADCAST_ACTION)) {

            String remoteSessionStatus = intent.getStringExtra(REMOTE_SESSION_STATUS);
            if (remoteSessionStatus != null) {
                updateRemoteSessionStatus(context, remoteSessionStatus);
            }
        }
    }

    private void updateRemoteSessionStatus(Context context, String remoteSessionStatus) {
        CcuLog.d(L.CCU_REMOTE_ACCESS, "remoteSessionStatus: "+remoteSessionStatus);
        switch (remoteSessionStatus) {
            case ACTION_REMOTE_APP_REGISTERED:
                updateDiagPoint(context, RemoteSessionStatus.REGISTERED.ordinal());
                break;
            case ACTION_SCREEN_SHARING_AVAILABLE:
                updateDiagPoint(context, RemoteSessionStatus.AVAILABLE.ordinal());
                break;
            case ACTION_SCREEN_SHARING_START:
                updateDiagPoint(context, RemoteSessionStatus.IN_PROGRESS.ordinal());
                break;
            case ACTION_SCREEN_SHARING_STOP:
               //not updating diag status here when screen sharing timeout broadcast occurs
                if(!isTimeOutBroadcast){
                    updateDiagPoint(context, RemoteSessionStatus.STOPPED.ordinal());
                }
                isTimeOutBroadcast = false;
                break;
            case ACTION_SCREEN_TIMEOUT:
                isTimeOutBroadcast = true;
                updateDiagPoint(context, RemoteSessionStatus.TIMEOUT.ordinal());
                break;
            case ACTION_SCREEN_SHARING_PERMISSION_NEEDED:
            case REMOTE_APP_KILLED:
                break;
            default:
                updateDiagPoint(context, RemoteSessionStatus.NOT_AVAILABLE.ordinal());
        }
    }

    public void updateDiagPoint(Context context,double val) {
        CcuLog.d(L.CCU_REMOTE_ACCESS, "updateRemoteSessionStatus: value "+val);
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_SCREEN_SHARING_STATUS, (int) val);
        editor.apply();

        CCUHsApi.getInstance().writeHisValByQuery("point and diag and remote and status", val);
    }
}
