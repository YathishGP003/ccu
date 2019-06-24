package a75f.io.logic.pubnub;

import android.content.BroadcastReceiver;
import android.content.Intent;

import com.google.gson.JsonObject;

import a75f.io.logic.Globals;

public class RemoteCommandUpdateHandler
{
    public static String getCmd() { return "remoteCmdUpdate"; }

    public static void handleMessage(JsonObject msgObject) {
        String cmdType = msgObject.get("remoteCmdType").getAsString();

        if (cmdType.startsWith("ota_update")) {
            //format is "ota_update 1234 SmartNode_v1.0"
            String[] cmdParams = cmdType.split(" ");

            Intent otaUpdateIntent = new Intent(Globals.IntentActions.PUBNUB_MESSAGE);
            //otaUpdateIntent.setComponent(new ComponentName("a75f.io.renatus",
            //                                                "a75f.io.device.mesh.OTAUpdateService"));
            otaUpdateIntent.putExtra("lwMeshAddress", Integer.parseInt(cmdParams[1]));
            otaUpdateIntent.putExtra("firmwareVersion", cmdParams[2]);

            //sendBroadcast(otaUpdateIntent);   //TODO how to get context here?
            //mApplicationContext.startService(otaUpdateIntent);
        }
    }
}
