package a75f.io.alerts;

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


