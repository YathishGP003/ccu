package a75f.io.sitesequencer;

import static a75f.io.sitesequencer.HaystackService.MSG_SUCCESS;

import a75f.io.alerts.log.LogLevel;
import a75f.io.alerts.log.LogOperation;
import a75f.io.sitesequencer.log.SequencerLogsCallback;

public class SequencerJsUtil {

    SiteSequencerDefinition def;

    private SequencerLogsCallback sequenceLogsCallback;

    SequencerJsUtil(SequencerJsCallback sequencerJsCallback, SequencerLogsCallback sequenceLogsCallback){
        this.mSequencerJsCallback = sequencerJsCallback;
        this.sequenceLogsCallback = sequenceLogsCallback;
    }
    SequencerJsCallback mSequencerJsCallback;

    public void triggerAlert(String blockId, String notificationMsg, String message,
                                               String entityId, Object contextHelper) {
        String logMessage = "alert triggered "+ blockId + " " + notificationMsg + " " + message + " " + entityId;
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("TRIGGER_ALERT"), logMessage, MSG_SUCCESS);
        mSequencerJsCallback.triggerAlert(blockId, notificationMsg, message, entityId, contextHelper, def);
    }

}


