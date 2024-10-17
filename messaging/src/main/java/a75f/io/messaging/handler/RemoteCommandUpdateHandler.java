package a75f.io.messaging.handler;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.interfaces.RemoteCommandHandleInterface;
import a75f.io.logic.interfaces.SafeModeInterface;
import a75f.io.messaging.MessageHandler;

public class RemoteCommandUpdateHandler implements MessageHandler
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
    public static final String RESET_PASSWORD = "reset_password";
    public static final String RESTART_MODULE = "restart_module";
    public static final String OTA_UPDATE_HS = "ota_update_hyperStat";
    public static final String OTA_UPDATE_CM = "ota_update_CM";
    public static final String OTA_UPDATE_HN = "ota_update_helioNode";
    public static final String OTA_UPDATE_CONNECT = "ota_update_connectModule";
    public static final String EXIT_SAFE_MODE = "exit_safe_mode";
    private static RemoteCommandHandleInterface remoteCommandInterface = null;
    private static SafeModeInterface safeModeInterface = null;
    public static final String OTA_UPDATE_BAC_APP = "update_bacapp";//"backappInstallOrUpgrade";
    public static final String OTA_UPDATE_REMOTE_ACCESS_APP = "ota_update_remoteAccess";
    public static final String OTA_UPDATE_HOME_APP = "ota_update_homeApp";
    public static final String UPDATE_CCU_LOG_LEVEL = "update_ccu_log_level";
    /**
     * Maintain Queue request for all the OTA request and process one by one
     */
    public static final String SAVE_SEQUENCER_LOGS = "save_sequencer_logs";

    @Override
    public void handleMessage(@NonNull JsonObject msgObject, @NonNull Context context) {
        try {
            String cmdType = msgObject.get(CMD_TYPE).getAsString();
            String cmdLevel = msgObject.get("remoteCmdLevel").getAsString();
            String systemId = cmdLevel.equals("system")? (msgObject.get("id").isJsonNull() ? "":msgObject.get("id").getAsString()) : "";
            String ccuUID = CCUHsApi.getInstance().getCcuRef().toString().replace("@","");
            String messageId = msgObject.get("messageId").getAsString();

            CcuLog.d("RemoteCommand","handle Msgs="+cmdType+","+cmdLevel+","+systemId+","+remoteCommandInterface);
            switch (cmdLevel){
                case "site":
                case "system":
                    if(cmdLevel.equals("site") || (!systemId.isEmpty() && systemId.equals(ccuUID) )) {
                        switch (cmdType) {

                            case RESTART_CCU:
                            case RESTART_TABLET:
                            case RESTART_MODULE:
                                CcuLog.d("RemoteCommand", " handle Restart CCU=");
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                else if(safeModeInterface != null)
                                    safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                break;
                            case UPDATE_CCU:
                            case OTA_UPDATE_BAC_APP:
                            case OTA_UPDATE_REMOTE_ACCESS_APP:
                            case OTA_UPDATE_HOME_APP:
                                CcuLog.d("RemoteCommand", " handle ota app update; type=" + cmdType + " APK=" + msgObject.get("version").getAsString());
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, msgObject.get("version").getAsString());
                                else if(safeModeInterface != null)
                                    safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, msgObject.get("version").getAsString());
                                break;
                            case RESET_CM:
                                CcuLog.d("RemoteCommand", " handle Restart reset cm=");
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                else if(safeModeInterface != null)
                                    safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                break;
                            case OTA_UPDATE_ITM:
                            case OTA_UPDATE_SD:
                            case OTA_UPDATE_HS:
                            case OTA_UPDATE_CM:
                            case OTA_UPDATE_HN:
                            case OTA_UPDATE_CONNECT:
                                Intent otaUpdateIntent = new Intent(Globals.IntentActions.PUBNUB_MESSAGE);
                                otaUpdateIntent.putExtra("id", msgObject.get("remoteCmdLevel").getAsString()); // site id
                                otaUpdateIntent.putExtra("firmwareVersion", msgObject.get("version").getAsString());
                                otaUpdateIntent.putExtra("cmdLevel", cmdLevel);
                                otaUpdateIntent.putExtra("messageId", messageId);
                                otaUpdateIntent.putExtra("remoteCmdType", cmdType);

                                context.sendBroadcast(otaUpdateIntent);
                                break;
                            case SAVE_CCU_LOGS:
                                CcuLog.d("RemoteCommand", " handle save logs");
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                else if(safeModeInterface != null)
                                    safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                break;
                            case SAVE_SEQUENCER_LOGS: {
                                String ccuId = msgObject.get("id").getAsString();
                                String sequenceId = msgObject.get("sequenceId").getAsString();
                                CcuLog.d("RemoteCommand", " handle save sequencer logs");
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, ccuId+"_"+sequenceId);
                                else if (safeModeInterface != null)
                                    safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, ccuId+"_"+sequenceId);
                            }
                                break;
                            case RESET_PASSWORD:
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                else if(safeModeInterface != null)
                                    safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, "");
                                break;
                            case EXIT_SAFE_MODE:
                                CcuLog.d("RemoteCommand", "RemoteCommand handle exit_safe_mode");
                                if (safeModeInterface != null)
                                    safeModeInterface.handleExitSafeMode();
                                break;
                            case UPDATE_CCU_LOG_LEVEL:
                                if (remoteCommandInterface != null)
                                    remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, msgObject.get("loglevel").getAsString());
                                else if(safeModeInterface != null)
                                    safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, msgObject.get("loglevel").getAsString());
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
                        case OTA_UPDATE_HN:
                        case OTA_UPDATE_CONNECT:
                            Intent otaUpdateIntent = new Intent(Globals.IntentActions.PUBNUB_MESSAGE);
                            otaUpdateIntent.putExtra("id", id);
                            otaUpdateIntent.putExtra("firmwareVersion", msgObject.get("version").getAsString());
                            otaUpdateIntent.putExtra("cmdLevel", cmdLevel);
                            otaUpdateIntent.putExtra("messageId", messageId);
                            otaUpdateIntent.putExtra("remoteCmdType", cmdType);
                            context.sendBroadcast(otaUpdateIntent);
                            break;
                        case RESTART_MODULE:
                            CcuLog.d("RemoteCommand"," handle zone level:"+cmdType);
                            if(remoteCommandInterface != null)
                                remoteCommandInterface.updateRemoteCommands(cmdType, cmdLevel, id);
                            else if(safeModeInterface != null)
                                safeModeInterface.updateRemoteCommands(cmdType, cmdLevel, id);
                            break;
                    }
                    break;
            }

        } catch(NullPointerException e) {
            CcuLog.e(L.TAG_CCU_MESSAGING, "Parsing error" , e);
        }
    }

    public static void setRemoteCommandInterface(RemoteCommandHandleInterface in){
        remoteCommandInterface = in;
    }

    public static void setSafeInterface(SafeModeInterface in){
        safeModeInterface = in;
    }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Collections.singletonList(CMD);
    }

    @Override
    public boolean ignoreMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {
        return false;
    }
}
