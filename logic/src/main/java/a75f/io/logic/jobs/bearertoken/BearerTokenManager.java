package a75f.io.logic.jobs.bearertoken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BearerTokenManager{

    private static final long REFRESH_INITIAL_DELAY_MILLIS = 15 * 60 * 1000;
    private static BearerTokenManager instance = null;

    private List<OnBearerTokenRefreshListener> tokenRefreshListeners = new ArrayList<>();
    
    private BearerTokenManager() {}
    
    public static BearerTokenManager getInstance() {
        if (instance == null) {
            synchronized(BearerTokenManager.class) {
                instance = new BearerTokenManager();
            }
        }
        return instance;
    }
    
    public void scheduleJob() {
        Context appContext = Globals.getInstance().getApplicationContext();
        AlarmManager alarmMgr = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, BearerTokenManagerService.class);
        PendingIntent alarmIntent = PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                        + REFRESH_INITIAL_DELAY_MILLIS, AlarmManager.INTERVAL_DAY, alarmIntent);
    }
    
    public void fetchToken(CCUHsApi hayStack) {
        String bearerToken = hayStack.getJwt();
        
        CaretakerService service = CaretakerServiceGenerator.createService(CaretakerService.class, bearerToken);
        Call<BearerToken> callAsync = service.getAccessToken(hayStack.getCcuId());
        
        callAsync.enqueue(new Callback<BearerToken>() {
            @Override
            public void onResponse(Call<BearerToken> call, Response<BearerToken> response) {
                CcuLog.d(L.TAG_CCU_JOB, " BearerTokenManagerService : fetchToken response "+response);
                if (response != null && response.isSuccessful()) {
                    BearerToken token = response.body();
                    CcuLog.d(L.TAG_CCU_JOB, "BearerTokenManagerService: Set new token " + token.getAccessToken());
                    hayStack.setJwt(token.getAccessToken());

                    tokenRefreshListeners.forEach( listener -> listener.onTokenRefresh());

                }
                hayStack.updateJwtValidity();
            }
            
            @Override
            public void onFailure(Call<BearerToken> call, Throwable throwable) {
                System.out.println(throwable);
                hayStack.updateJwtValidity();
            }
        });
    }

    public interface OnBearerTokenRefreshListener {
        void onTokenRefresh();
    }

    public void setOnBearerTokenRefreshListener(OnBearerTokenRefreshListener listener) {
        tokenRefreshListeners.add(listener);
    }
}
