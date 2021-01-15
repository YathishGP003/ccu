package a75f.io.logic.jobs.bearertoken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BearerTokenManager{
    
    private static BearerTokenManager instance = null;
    
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
        Log.d(L.TAG_CCU_JOB, " BearerTokenManager : scheduleJob");
        Context appContext = Globals.getInstance().getApplicationContext();
        AlarmManager alarmMgr = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, BearerTokenManagerService.class);
        PendingIntent alarmIntent = PendingIntent.getService(appContext, 0, intent, 0);
    
        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                     SystemClock.elapsedRealtime() + 0 /*AlarmManager.INTERVAL_FIFTEEN_MINUTES*/,
                                     AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
    }
    
    public void fetchToken(CCUHsApi hayStack) {
        Log.d(L.TAG_CCU_JOB, " BearerTokenManagerService : fetchToken");
        
        GateKeeperService service = GateKeeperServiceGenerator.createService(GateKeeperService.class, hayStack.getJwt());
        Call<BearerToken> callAsync = service.getAccessToken(hayStack.getCCUGuid());
        
        callAsync.enqueue(new Callback<BearerToken>() {
            @Override
            public void onResponse(Call<BearerToken> call, Response<BearerToken> response) {
                Log.d(L.TAG_CCU_JOB, " BearerTokenManagerService : fetchToken response "+response);
                if (response != null && response.isSuccessful()) {
                    BearerToken token = response.body();
                    CcuLog.d(L.TAG_CCU_JOB, "Set new token " + token.getAccessToken());
                    hayStack.setJwt(token.getAccessToken());
                }
            }
            
            @Override
            public void onFailure(Call<BearerToken> call, Throwable throwable) {
                System.out.println(throwable);
            }
        });
    }
}
