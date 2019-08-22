package a75f.io.alerts;

import android.content.Context;

import com.google.gson.Gson;

import java.util.List;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

public class AlertSyncHandler
{
    private static AlertSyncHandler instance = new AlertSyncHandler();
    
    private AlertSyncHandler(){
    }
    
    public AlertSyncHandler getInstance() {
        return instance;
    }
    
    public static void sync(Context c, List<Alert> alerts) {
        for (Alert a : alerts)
        {
            String siteId = CCUHsApi.getInstance().getGUID(a.siteRef);
            String response = HttpUtil.sendRequest(c, "createAlert", new Gson().toJson(a));
            CcuLog.d("CCU_ALERTS"," response "+response);
        }
    }
}
