package a75f.io.api.haystack.sync;

import android.content.Context;
import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Iterator;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static a75f.io.api.haystack.sync.PointWriteUtil.ENDPOINT_POINT_WRITE_MANY;
import static a75f.io.api.haystack.sync.PointWriteUtil.WRITABLE_POINT_BATCH_SIZE;

public class PointWriteWorker extends Worker {
    
    private static final String TAG = "CCU_HS_PointWriteWorker";
    
    SyncStatusService syncStatusService;
    
    public PointWriteWorker(Context appContext, WorkerParameters workerParams) {
        super(appContext, workerParams);
        syncStatusService = SyncStatusService.getInstance(getApplicationContext());
    }
    
    @NonNull @Override
    public Result doWork() {
        
        CcuLog.i(TAG, "doWork : PointWriteWorker");
    
        if (!CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.e(TAG, "Abort PointWrite : CCU Not registered");
            return Result.failure();
        }
        
        if (!sendPointArrays()) {
            return Result.retry();
        }
        
        return Result.success();
    }
    
    private boolean sendPointArrays() {
        HGrid writablePointsGrid = CCUHsApi.getInstance().readGrid("point and writable");
        if(writablePointsGrid != null) {
            CcuLog.d(TAG, "Writable Points Count = " + writablePointsGrid.numRows());
        }
        HGridIterator writablePointsIterator = new HGridIterator(writablePointsGrid);

        boolean overallPointWriteStatus = true;
        int batchCount = 0;
        while (writablePointsIterator.hasNext()) {

            CcuLog.d(TAG, "sendPointArrays.batch = " + ++batchCount);
            HGrid pointGrid = writablePointsIterator.next(WRITABLE_POINT_BATCH_SIZE);
            if(pointGrid != null) {
                CcuLog.d(TAG, "PointWrite Batch size = " + pointGrid.numRows());
            }
            boolean status = sendPointArrayBatch(pointGrid);
            
            if (!status) {
                CcuLog.d(TAG, "PointWrite failed for batch = " + batchCount);
                overallPointWriteStatus = false;
            }
        }
        CcuLog.d(TAG, "PointWrite status = " + overallPointWriteStatus);
        return overallPointWriteStatus;
    }
    
    private boolean sendPointArrayBatch(HGrid pointGrid) {

        if (CCUHsApi.getInstance().getAuthorised()) {
            ArrayList<HDict> pointValList = new ArrayList<>();
            Iterator iterator = pointGrid.iterator();

            while (iterator.hasNext()) {
                HDict pointDict = (HDict) iterator.next();
                String pointId = pointDict.get(Tags.ID).toString();
                if (!syncStatusService.hasEntitySynced(pointId)) {
                    continue;
                }
                ArrayList<HDict> valDictList = PointWriteUtil.getWriteArrDict(pointDict.get(Tags.ID).toString());
                if (!valDictList.isEmpty()) {
                    pointValList.addAll(valDictList);
                }
            }
            if (!pointValList.isEmpty()) {
                HGrid gridData = HGridBuilder.dictsToGrid(pointValList.toArray(new HDict[0]));
                EntitySyncResponse response = HttpUtil.executeEntitySync(CCUHsApi.getInstance().getHSUrl() + ENDPOINT_POINT_WRITE_MANY,
                        HZincWriter.gridToString(gridData), CCUHsApi.getInstance().getJwt());
                if (response.getRespCode() == HttpUtil.HTTP_RESPONSE_OK) {
                    return true;
                } else if (response.getRespCode() == HttpUtil.HTTP_RESPONSE_ERR_REQUEST) {
                    if(EntitySyncErrorHandler.handle400HttpError(CCUHsApi.getInstance(), response.getErrRespString()).isEmpty()) {
                        CcuLog.d(TAG, "Since no ids are mentioned in the error response or present in CCU, assuming the batch write was successful.");
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
        return true;
    }
    
    
}
