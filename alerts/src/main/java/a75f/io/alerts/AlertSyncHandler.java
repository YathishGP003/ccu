package a75f.io.alerts;

import android.content.Context;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

public class AlertSyncHandler
{
    public static List<Alert> sync(Context c, List<Alert> alerts) {
        ArrayList<Alert> syncedAlerts = new ArrayList<>();
        for (Alert a : alerts)
        {
            Alert clone = AlertBuilder.build(a);
            String siteId = CCUHsApi.getInstance().getGUID(a.siteRef);
            String deviceId = CCUHsApi.getInstance().getGUID(a.deviceRef);
            
            if (siteId == null || deviceId == null) {
                continue;
            }
            clone.setSiteRef(siteId);
            clone.setDeviceRef(deviceId);
            if (a.getGuid().equals(""))
            {
                String response = HttpUtil.sendRequest(c, "createAlert", new Gson().toJson(clone));
                CcuLog.d("CCU_ALERTS", " response " + response);
                if (response != null)
                {
                    try
                    {
                        JSONObject jsonObj = new JSONObject(response);
                        String id = jsonObj.getString("_id");
                        if (id == "" || id == null) {
                            continue;
                        }
                        a.setGuid(id);
                        a.setSyncStatus(true);
                        syncedAlerts.add(a);
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            }else {
                String response = HttpUtil.sendRequest(c, "updateAlert", new Gson().toJson(clone));
                CcuLog.d("CCU_ALERTS", " response " + response);
                if (response != null)
                {
                    a.setSyncStatus(true);
                    syncedAlerts.add(a);
                }
            }
            
        }
        return syncedAlerts;
    }
}
