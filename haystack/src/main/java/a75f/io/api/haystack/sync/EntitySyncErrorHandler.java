package a75f.io.api.haystack.sync;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;

import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

class EntitySyncErrorHandler {
    
    /* This is a temporary work-around till we have transaction error codes implemented.
     * Try to sync the entities again here if the server says they are not valid.
     */
    public static void handle400HttpError(CCUHsApi hsApi, String respErrorString) {
        CcuLog.d("CCU_HS", "handle400HttpError "+respErrorString);
        HGrid unSyncedGrid = new HZincReader(respErrorString).readGrid();
        Iterator unSyncedGridIterator = unSyncedGrid.iterator();
        
        while (unSyncedGridIterator.hasNext()) {
            HRow itemRow = (HRow) unSyncedGridIterator.next();
            String itemId = itemRow.getRef("id").toString();
            String itemErrorType = itemRow.getStr("error");
            CcuLog.d("CCU_HS", "handle400HttpError "+itemId+" "+itemErrorType);
            if (itemId == null || itemErrorType == null) {
                CcuLog.d("CCU_HS", "Invalid 400 error response item "+itemRow.toString());
                continue;
            }
            if (itemErrorType.equals("NOT_FOUND")) {
                hsApi.getSyncStatusService().addUnSyncedEntity(itemId);
            }
        }
        
        if (unSyncedGrid.numRows() > 0) {
            hsApi.scheduleSync();
        }
    }
}
