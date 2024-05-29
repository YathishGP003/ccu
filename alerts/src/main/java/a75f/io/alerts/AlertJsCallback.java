package a75f.io.alerts;

public interface AlertJsCallback {
    boolean triggerAlert(String blockId, String notificationMsg, String message,
                         String entityId, Object contextHelper, AlertDefinition def);
}


