package a75f.io.api.haystack.bacnet.parser;

import a75f.io.logger.CcuLog;
import a75f.io.util.BacnetRequestUtil;

/**
 * Created by Sathishkumar S on 29-05-2025.
 */

public class BacnetRequestProcessor {

    private static BacnetRequestUtil callback;

    public static void setCallback(BacnetRequestUtil cb) {
        callback = cb;
    }

    public static void sendCallBackMstpWriteRequest(String id, String level, String value) {
        if (callback != null) {
            // Log the request for debugging purposes
            CcuLog.d("CCU_BACNET", "Sending MSTP write request: id=" + id + ", level=" + level + ", value=" + value);
            callback.callbackForBacnetMstpRequest(id, level, value);
        } else {
            CcuLog.e("CCU_BACNET", "Callback is not set. Cannot send MSTP write request.");
        }
    }

    public static void sendCallBackBacnetWriteRequest(String id, String level, String value) {
        if (callback != null) {
            // Log the request for debugging purposes
            CcuLog.d("CCU_BACNET", "Sending BACNET write request: id=" + id + ", level=" + level + ", value=" + value);
            callback.callbackForBacnetRequest(id, level, value);
        } else {
            CcuLog.e("CCU_BACNET", "Callback is not set. Cannot send BACNET write request.");
        }
    }
}
