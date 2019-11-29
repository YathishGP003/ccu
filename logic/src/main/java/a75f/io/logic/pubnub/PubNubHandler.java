package a75f.io.logic.pubnub;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.json.JSONException;
import org.json.JSONObject;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class PubNubHandler
{
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
            default:
                CcuLog.d(L.TAG_CCU_PUBNUB, "UnSupported PubNub Command : "+cmd);
                
        }
        
    }
}
