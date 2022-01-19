package a75f.io.logic.pubnub;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;

public class RemoteCommandUpdateHandler
{
    public static final String CMD = "remoteCommand";
    public static final String CMD_TYPE = "remoteCmdType";
    public static final String OTA_UPDATE_SD = "ota_update_smartdevice";
    public static final String OTA_UPDATE_ITM = "ota_update_itm";
    public static final String UPDATE_CCU = "update_ccu";
    public static final String RESTART_CCU = "restart_ccu";
    public static final String RESTART_TABLET = "restart_tablet";
    public static final String RESET_CM = "reset_cm";
    public static final String SAVE_CCU_LOGS = "save_ccu_logs";
    public static final String RESTART_MODULE = "restart_module";
    public static final String OTA_UPDATE_HS = "ota_update_hyperStat";
    private static RemoteCommandHandleInterface remoteCommandInterface = null;
    /**
     * Maintain Queue request for all the OTA request and process one by one
     */

    public static void handleMessage(JsonObject msgObject, Context context) {
        try {
            String cmdType = msgObject.get(CMD_TYPE).getAsString();
            String cmdLevel = msgObject.get("level").getAsString();
            String systemId = cmdLevel.equals("system")? (msgObject.get("id").isJsonNull() ? "":msgObject.get("id").getAsString()) : "";
            String ccuUID = CCUHsApi.getInstance().getCcuRef().toString().replace("@","");
            CcuLog.d("RemoteCommand","PUBNUB handle Msgs="+cmdType+","+cmdLevel+","+remoteCommandInterface);
            switch (cmdLevel){
                case "site":
                case "system":
                    if(cmdLevel.equals("site") || (!systemId.isEmpty() && systemId.equals(ccuUID) )) {
                        switch (cmdType) {

                            case RESTART_CCU:
                            case RESTART_TABLET:
                            case RESTART_MODULE:
                                CcuLog.d("RemoteCommand", "PUBNUB handle Restart CCU=");
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                break;
                            case UPDATE_CCU:
                                CcuLog.d("RemoteCommand", "PUBNUB handle update CCU=" + msgObject.get("version").getAsString());
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, msgObject.get("version").getAsString());
                                break;
                            case RESET_CM:
                                CcuLog.d("RemoteCommand", "PUBNUB handle Restart reset cm=");
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                break;
                            case OTA_UPDATE_ITM:
                            case OTA_UPDATE_SD:
                            case OTA_UPDATE_HS:
                                Intent otaUpdateIntent = new Intent(Globals.IntentActions.PUBNUB_MESSAGE);
                                otaUpdateIntent.putExtra("id", msgObject.get("level").getAsString()); // site id
                                otaUpdateIntent.putExtra("firmwareVersion", msgObject.get("version").getAsString());
                                otaUpdateIntent.putExtra("cmdLevel", cmdLevel);

                                context.sendBroadcast(otaUpdateIntent);
                                break;
                            case SAVE_CCU_LOGS:
                                CcuLog.d("RemoteCommand", "PUBNUB handle save logs");
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                break;
                        }
                    }
                    break;
                case "zone":
                case "module":
                    String id = msgObject.get("id").isJsonNull() ? "":msgObject.get("id").getAsString();

                    switch(cmdType){
                        case OTA_UPDATE_SD:
                        case OTA_UPDATE_ITM:
                        case OTA_UPDATE_HS:
                            Intent otaUpdateIntent = new Intent(Globals.IntentActions.PUBNUB_MESSAGE);
                            otaUpdateIntent.putExtra("id", id);
                            otaUpdateIntent.putExtra("firmwareVersion", msgObject.get("version").getAsString());
                            otaUpdateIntent.putExtra("cmdLevel", cmdLevel);
                            context.sendBroadcast(otaUpdateIntent);
                            break;
                        case RESTART_MODULE:
                            CcuLog.d("RemoteCommand","PUBNUB handle zone level:"+cmdType);
                            if(remoteCommandInterface != null)
                                remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, id);
                            break;
                    }
                    break;
            }

        } catch(NullPointerException e) {
            // Command parsing failed
            e.printStackTrace();
        }
    }

    public static void setRemoteCommandInterface(RemoteCommandHandleInterface in){
        remoteCommandInterface = in;
    }
}
