package a75f.io.alerts;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

import android.util.Log;

public interface AlertJsCallback {
    public boolean triggerAlert(String blockId, String notificationMsg, String message,
                                String entityId, Object contextHelper, AlertDefinition def);
}


