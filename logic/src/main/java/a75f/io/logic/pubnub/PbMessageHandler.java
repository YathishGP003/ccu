package a75f.io.logic.pubnub;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

public class PbMessageHandler
{
    public static final String CM_RESET = "CM RESET";
    public static final String PRE_DEF_ALERT = "predefinedAlertDefinition";
    
    public static void handlePunubMessage(JsonElement receivedMessageObject, Context appContext) {
        
        CcuLog.d(L.TAG_CCU_PUBNUB, "handlePunubMessage: " + receivedMessageObject.toString());
        
        try {
            PbMessageHandler.handleMessage(receivedMessageObject.getAsJsonObject(), appContext);
        } catch (NumberFormatException e) {
            Log.d(L.TAG_CCU_PUBNUB, "Invalid data format, igoring PubNub Message " + e.getMessage());
        }
    }
    
    public static void handleMessage(JsonObject msg, Context context){
        String cmd = msg.get("command") != null ? msg.get("command").getAsString(): "";
        switch (cmd) {
            case UpdatePointHandler.CMD:
                UpdatePointHandler.handleMessage(msg);
                break;
            case SiteSyncHandler.CMD:
                SiteSyncHandler.handleMessage(msg);
                break;
            case UpdateScheduleHandler.CMD:
                UpdateScheduleHandler.handleMessage(msg);
                break;
            case RemoveEntityHandler.CMD:
                RemoveEntityHandler.handleMessage(msg);
                break;
            case RemoteCommandUpdateHandler.CMD:
                RemoteCommandUpdateHandler.handleMessage(msg, context);
                break;
            case AlertDefinitionHandler.CMD:
                AlertDefinitionHandler.handleMessage(msg);
                break;
            case AlertRemoveHandler.REM_ALERT_CMD:
            case AlertRemoveHandler.REMOVE_DEF_CMD:
            case AlertRemoveHandler.CLR_SITEDEF_CMD:
                AlertRemoveHandler.handleMessage(cmd, msg);
                break;
            case PRE_DEF_ALERT:
                AlertManager.getInstance(Globals.getInstance().getApplicationContext()).fetchPredefinedAlerts();
                break;
            default:
                CcuLog.d(L.TAG_CCU_PUBNUB, "UnSupported PubNub Command : "+cmd);
                
        }
        
    }
}
