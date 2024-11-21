package a75f.io.api.haystack.sync;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

class EntitySyncErrorHandler {
    
    /* This is a temporary work-around till we have transaction error codes implemented.
     * Try to sync the entities again here if the server says they are not valid.
     */
    public static List<String> handle400HttpError(CCUHsApi hsApi, String respErrorString) {
        CcuLog.d("CCU_HS", "handle400HttpError "+respErrorString);
        List<String> retryIdList = new ArrayList<>();
        try {
            HGrid unSyncedGrid = new HZincReader(respErrorString).readGrid();
    
            Iterator unSyncedGridIterator = unSyncedGrid.iterator();
    
            while (unSyncedGridIterator.hasNext()) {
                HRow itemRow = (HRow) unSyncedGridIterator.next();
                String itemId = itemRow.getRef("id").toString();
                String itemErrorType = itemRow.getStr("error");
                CcuLog.d("CCU_HS", "handle400HttpError "+itemId+" "+itemErrorType);
                if (itemErrorType == null) {
                    CcuLog.d("CCU_HS", "Invalid 400 error response item "+ itemRow);
                    continue;
                }
                if (itemErrorType.equals("NOT_FOUND") && !hsApi.isBuildingTunerPoint(itemId)) {
                    hsApi.getSyncStatusService().addUnSyncedEntity(itemId);
                }
                if(hsApi.readId("id == " + itemId) != null) {
                    retryIdList.add(itemId);
                }
            }
    
            if (unSyncedGrid.numRows() > 0) {
                hsApi.scheduleSync();
            }
        } catch (Exception e) {
            CcuLog.d("CCU_HS", "Invalid error message! ", e);
        }
        return retryIdList;
    }

}
