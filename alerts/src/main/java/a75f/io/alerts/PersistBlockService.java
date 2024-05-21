package a75f.io.alerts;

import a75f.io.logger.CcuLog;
import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

public class PersistBlockService {
    private static String defId;
    private PersistBlockService() {

    }

    private static PersistBlockService persistBlockServiceInstance = null;

    public static synchronized PersistBlockService getInstance(String definitionId) {
        defId = definitionId;
        if (persistBlockServiceInstance == null)
            persistBlockServiceInstance = new PersistBlockService();

        return persistBlockServiceInstance;
    }

    public void create(String key, Object value, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "map: create: key: "+key+" value: "+value + " defId: "+defId);
        AlertManager.getInstance().initValue(defId+key, value);
    }

    public void set(String key, Object value, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "map: set: key: "+key+" value: "+value + " defId: "+defId);
        AlertManager.getInstance().putValue(defId+key, value);
    }

    public Object get(String key, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "map: get: key: "+key+" defId: "+defId + " value: "+AlertManager.getInstance().getValue(defId+key));
        return AlertManager.getInstance().getValue(defId+key);
    }
}
