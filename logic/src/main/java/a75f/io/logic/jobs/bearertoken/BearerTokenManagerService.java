package a75f.io.logic.jobs.bearertoken;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import a75f.io.api.haystack.CCUHsApi;

public class BearerTokenManagerService extends Service {
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        if (!hayStack.getJwt().isEmpty()) {
            BearerTokenManager.getInstance().fetchToken(hayStack);
        }
        return Service.START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }
}
