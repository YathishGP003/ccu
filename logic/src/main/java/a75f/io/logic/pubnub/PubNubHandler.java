package a75f.io.logic.pubnub;

import android.content.Context;

import com.google.gson.JsonObject;

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
            default:
                CcuLog.d(L.TAG_CCU_PUBNUB, "UnSupported PubNub Command Received"+cmd);
                
        }
        
    }
}
