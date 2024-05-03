package a75f.io.renatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.projecthaystack.HRef;

import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;

public class CcuRefReceiver extends BroadcastReceiver {
    public static final String REQUEST_CCU_REF_ACTION = "com.remote.REQUEST_CCU_REF";
    public static final String SEND_CCU_REF_ACTION = "com.remote.SEND_CCU_REF";
    private static final String SHARED_PREFERENCE_NAME = "remote_status_pref";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (Objects.requireNonNull(intent.getAction()).equals(REQUEST_CCU_REF_ACTION)) {
            Intent responseIntent = new Intent(SEND_CCU_REF_ACTION);
            try {
                CCUHsApi instance = CCUHsApi.getInstance();
                String ccuId = instance.getCcuId();
                String ccuName = instance.getCcuName();
                HRef site = instance.getSiteIdRef();
                String jwt = instance.getJwt();

                if (ccuId != null && ccuName != null && site != null && jwt != null) {
                    responseIntent.putExtra("ccuId", instance.getCcuId());
                    responseIntent.putExtra("ccuName", instance.getCcuName());
                    responseIntent.putExtra("siteId", instance.getSiteIdRef().toVal());
                    responseIntent.putExtra("bearerToken", instance.getJwt());
                    context.sendBroadcast(responseIntent);
                } else {
                    Log.w("CcuRefReceiver", "CCU information incomplete, not sending RAA broadcast");
                }
            } catch(Exception ex) {
                Log.w("CcuRefReceiver", "Exception while attempting to broadcast CCU information: " + ex.getMessage());
            }
        }
    }
}
