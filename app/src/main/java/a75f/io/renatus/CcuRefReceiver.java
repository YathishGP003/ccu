package a75f.io.renatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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

            String pin = intent.getStringExtra("pin");

            if (pin != null) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(
                        SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("pin", pin);
                editor.apply();
            }
            String ccuRef = CCUHsApi.getInstance().getCcuId();
            String bearerToken = CCUHsApi.getInstance().getJwt();
            Intent responseIntent = new Intent(SEND_CCU_REF_ACTION);
            responseIntent.putExtra("ccuRef", ccuRef);
            responseIntent.putExtra("bearerToken", bearerToken);
            responseIntent.putExtra("pin",pin);
            context.sendBroadcast(responseIntent);

        }
    }
}
