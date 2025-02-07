package a75f.io.logic.jobs.bearertoken;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.util.OfflineModeUtilKt;

public class BearerTokenManagerService extends Service {
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CcuLog.i(L.TAG_CCU_INIT, "BearerTokenManagerService onStartCommand executed");
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            if (!hayStack.getJwt().isEmpty() && !OfflineModeUtilKt.isOfflineMode()) {
                BearerTokenManager.getInstance().fetchToken(hayStack);
            }
        } catch (IllegalStateException e) {
            CcuLog.e(L.TAG_CCU_INIT, "Haystack not initialized: Skip Bearer token refresh", e);
            e.printStackTrace();
        } catch (kotlin.UninitializedPropertyAccessException e) {
            CcuLog.e(L.TAG_CCU_INIT, "Domain.ccuEquip is not initialized properly", e);
            e.printStackTrace();
        }
        CcuLog.i(L.TAG_CCU_INIT, "BearerTokenManagerService onStartCommand completed");
        return Service.START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }
}
