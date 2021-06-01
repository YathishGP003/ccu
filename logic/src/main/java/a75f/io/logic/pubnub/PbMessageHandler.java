package a75f.io.logic.pubnub;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

import static a75f.io.logic.pubnub.AlertMessageHandlers.*;

public class PbMessageHandler
{
    public static final String CM_RESET = "CM RESET";

    private static PbMessageHandler instance = null;
    private HandlerThread handlerThread;
    private Handler       messageHandler;

    // do not use directly for now.  Use getter for lazy initialization, until DI implemented.
    private AlertMessageHandler alertMessageHandler;
    private AlertMessageHandler alertMessageHandler() {
        if (alertMessageHandler == null) {
            alertMessageHandler = AlertMessageHandler.instanceOf();
        }
        return alertMessageHandler;
    }
    
    /**
     * Handler thread makes sure that the pubnub messages are processed sequentially on the CCU.
     * Concurrent handling can lead to unexpected behaviour when one of the messages is a command to reboot
     * the tablet.
     */
    private PbMessageHandler() {
        
        handlerThread = new HandlerThread("UpdatePointHandlerThread"){
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                CcuLog.i(L.TAG_CCU_PUBNUB,"UpdatePointHandler : Ready");
            }
        };
        
        handlerThread.start();
        
        messageHandler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                PbMessage pbMessage = (PbMessage) msg.obj;
                try {
                    handlePbMessage(pbMessage.msg, pbMessage.cxt);
                } catch (Exception e) {
                    //We do understand the consequences of doing this.
                    //But the system could still continue to work in standalone mode controlling the hvac system
                    //even if there are failures in handling a pubnub message.
                    CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to handle pubnub !", e);
                }
            }
        };
        
    }
    public static PbMessageHandler getInstance() {
        if (instance == null) {
            synchronized(UpdatePointHandler.class) {
                if (instance == null) {
                    instance = new PbMessageHandler();
                }
            }
        }
        return instance;
    }
    
    public void handlePunubMessage(JsonElement receivedMessageObject, Long timeToken, Context appContext) {
        
        CcuLog.d(L.TAG_CCU_PUBNUB, "handlePunubMessage: " + receivedMessageObject.toString());
        Message message = messageHandler.obtainMessage();
        PbMessage pbMessage = new PbMessage();
        try {
            pbMessage.msg = receivedMessageObject.getAsJsonObject();
            pbMessage.cxt = appContext;
            message.obj = pbMessage;
            messageHandler.sendMessage(message);
            //PbMessageHandler.handlePbMessage(, appContext);
        } catch (NumberFormatException e) {
            Log.d(L.TAG_CCU_PUBNUB, "Invalid data format, ignoring PubNub Message " + e.getMessage());
        }
    
        PbPreferences.setLastHandledTimeToken(timeToken, appContext);
    }
    
    private void handlePbMessage(JsonObject msg, Context context){
        String cmd = msg.get("command") != null ? msg.get("command").getAsString(): "";
        switch (cmd) {
            case UpdatePointHandler.CMD:
                UpdatePointHandler.handleMessage(msg);
                break;
            case SiteSyncHandler.CMD:
                SiteSyncHandler.handleMessage(msg, context);
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
            case CREATE_CUSTOM_ALERT_DEF_CMD:
            case UPDATE_CUSTOM_ALERT_DEF_CMD:
                alertMessageHandler().handleCustomAlertDefMessage(msg);
                break;
            case REMOVE_ALERT_CMD:
            case REMOVE_ALERTS_CMD:
                alertMessageHandler().handleAlertRemoveMessage(cmd, msg);
                break;
            case CREATE_PREDEFINED_ALERT_DEF_CMD:
            case UPDATE_PREDEFINED_ALERT_DEF_CMD:
            case DELETE_PREDEFINED_ALERT_DEF_CMD:
                alertMessageHandler().handlePredefinedAlertDefMessage(msg);
                break;
            case DELETE_CUSTOM_ALERT_DEF_CMD:
            case DELETE_SITE_DEFS_CMD:
                alertMessageHandler().handleAlertDefRemoveMessage(cmd, msg);
                break;
            default:
                CcuLog.d(L.TAG_CCU_PUBNUB, "UnSupported PubNub Command : "+cmd);
        }
    }
    
    class PbMessage {
        JsonObject msg;
        Context cxt;
    }
}
