package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

public class PubNubHandler
{
    public static void handleMessage(JsonObject msg){
        String cmd = msg.get("command") != null ? msg.get("command").getAsString(): "";
        
        if (cmd.equals(UpdatePointHandler.getCmd())) {
            UpdatePointHandler.handleMessage(msg);
        } else if (cmd.equals(SiteSyncHandler.getCmd())) {
            SiteSyncHandler.handleMessage(msg);
        } else if (cmd.equals(UpdateScheduleHandler.getCmd())) {
            UpdateScheduleHandler.handleMessage(msg);
        }
    }
}
