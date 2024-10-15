package a75f.io.sitesequencer;

import a75f.io.alerts.AlertManager;
import a75f.io.logger.CcuLog;

public class PersistBlockService {
    private static String defId;
    private static final String TAG = SequencerParser.TAG_CCU_SITE_SEQUENCER;
//    private PersistBlockService(String definitionId) {
//        defId = definitionId;
//    }

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
        CcuLog.d(TAG, "map: create: key: "+key+" value: "+value + " defId: "+defId);
        SequenceManager.getInstance().initValue(defId+key, value);
    }

    public void set(String key, Object value, Object contextHelper) {
        CcuLog.d(TAG, "map: set: key: "+key+" value: "+value + " defId: "+defId);
        SequenceManager.getInstance().putValue(defId+key, value);
    }

    public Object get(String key, Object contextHelper) {
        CcuLog.d(TAG, "map: get: key: "+key+" defId: "+defId + " value: "+AlertManager.getInstance().getValue(defId+key));
        return SequenceManager.getInstance().getValue(defId+key);
    }
}
