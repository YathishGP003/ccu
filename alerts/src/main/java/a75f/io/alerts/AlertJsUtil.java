package a75f.io.alerts;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

import android.util.Log;

public class AlertJsUtil {

    AlertDefinition def;

    AlertJsUtil(AlertJsCallback alertJsCallback){
        this.mAlertJsCallback = alertJsCallback;
    }
    AlertJsCallback mAlertJsCallback;

    public void triggerAlert(String blockId, String notificationMsg, String message,
                                               String entityId, Object contextHelper) {
        mAlertJsCallback.triggerAlert(blockId, notificationMsg, message, entityId, contextHelper, def);
    }

}


