package a75f.io.logic.pubnub;

import android.content.Context;
import android.content.Intent;

import com.google.gson.JsonObject;

import a75f.io.logic.Globals;

public class RemoteCommandUpdateHandler
{
    public static final String CMD = "remoteCmdUpdate";
    
    public static void handleMessage(JsonObject msgObject, Context context) {
        try {
            String cmdType = msgObject.get("remoteCmdType").getAsString();
            String cmdLevel = msgObject.get("level").getAsString();

            if (cmdType.startsWith("ota_update")) {
                // Format is "ota_update 1234 SmartNode_v1.0"
                String[] cmdParams = cmdType.split(" ");

                Intent otaUpdateIntent = new Intent(Globals.IntentActions.PUBNUB_MESSAGE);
                otaUpdateIntent.putExtra("lwMeshAddress", cmdParams[1]);
                otaUpdateIntent.putExtra("firmwareVersion", cmdParams[2]);
                otaUpdateIntent.putExtra("cmdLevel", cmdLevel);

                context.sendBroadcast(otaUpdateIntent);
            }

        } catch(NullPointerException e) {
            // Command parsing failed
            e.printStackTrace();
        }
    }
}
