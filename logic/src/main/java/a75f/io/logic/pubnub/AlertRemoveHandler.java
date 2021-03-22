package a75f.io.logic.pubnub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import a75f.io.alerts.AlertManager;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class AlertRemoveHandler
{
    public static final String REM_ALERT_CMD = "removeAlert";
    public static final String REMOVE_DEF_CMD = "removeAlertDefinition";
    public static final String CLR_SITEDEF_CMD = "clearSiteDefs";
    
    public static void handleMessage(String cmd, JsonObject msgObject)
    {
        try {
            JsonArray ids = msgObject.getAsJsonArray("_id");
            for (JsonElement id : ids) {
                switch (cmd) {
                    case REM_ALERT_CMD:
                        AlertManager.getInstance().deleteAlertInternal(id.getAsString());
                        CcuLog.d(L.TAG_CCU_PUBNUB," Deleted Alert: "+id);
                        break;
                    case REMOVE_DEF_CMD:
                    case CLR_SITEDEF_CMD:
                        AlertManager.getInstance().deleteAlertDefinition(id.getAsString());
                        CcuLog.d(L.TAG_CCU_PUBNUB," Deleted Alert Definition: "+id);
                        break;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            CcuLog.d(L.TAG_CCU_PUBNUB, " Failed to parse removeEntity Json " + msgObject);
        }
    }
}
