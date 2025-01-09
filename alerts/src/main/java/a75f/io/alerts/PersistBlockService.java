package a75f.io.alerts;

import a75f.io.alerts.log.LogLevel;
import a75f.io.alerts.log.LogOperation;
import a75f.io.alerts.log.SequenceLogs;
import a75f.io.alerts.log.SequenceMethodLog;
import a75f.io.logger.CcuLog;
import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

import java.util.Date;

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
        SequenceLogs seqLogs = AlertManager.getInstance().sequenceLogsMap.get(defId);
        if(seqLogs != null) {
            seqLogs.addLog(new SequenceMethodLog(LogLevel.INFO, LogOperation.GENERIC_INFO,
                    "persist block create--key--" + key + "--value--" + value.toString(), "success", new Date().toString(), new Date().toString(), null));
            AlertManager.getInstance().sequenceLogsMap.put(defId, seqLogs);
        }
        AlertManager.getInstance().initValue(defId+key, value);
    }

    public void set(String key, Object value, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "map: set: key: "+key+" value: "+value + " defId: "+defId);

        SequenceLogs seqLogs = AlertManager.getInstance().sequenceLogsMap.get(defId);
        if(seqLogs != null){
            seqLogs.addLog(new SequenceMethodLog(LogLevel.INFO, LogOperation.GENERIC_INFO,
                    "persist block set--key--"+key+"--value--"+value.toString(), "success", new Date().toString(), new Date().toString(), null));
            AlertManager.getInstance().sequenceLogsMap.put(defId, seqLogs);
        }
        AlertManager.getInstance().putValue(defId+key, value);
    }

    public Object get(String key, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "map: get: key: "+key+" defId: "+defId + " value: "+AlertManager.getInstance().getValue(defId+key));
        Object returnObj = AlertManager.getInstance().getValue(defId+key);
        SequenceLogs seqLogs = AlertManager.getInstance().sequenceLogsMap.get(defId);
        if(seqLogs != null) {
            seqLogs.addLog(new SequenceMethodLog(LogLevel.INFO, LogOperation.GENERIC_INFO,
                    "persist block get--key--" + key + "--value--" + returnObj.toString(), "success", new Date().toString(), new Date().toString(), null));
            AlertManager.getInstance().sequenceLogsMap.put(defId, seqLogs);
        }
        return returnObj;
    }
}
