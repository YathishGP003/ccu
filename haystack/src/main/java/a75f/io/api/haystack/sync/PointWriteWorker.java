package a75f.io.api.haystack.sync;

import android.content.Context;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

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
        
        HGridIterator writablePointsIterator = new HGridIterator(writablePointsGrid);
        
        while (writablePointsIterator.hasNext()) {
            
            HGrid pointGrid = writablePointsIterator.next(WRITABLE_POINT_BATCH_SIZE);
            boolean status = sendPointArrayBatch(pointGrid);
            
            if (!status) {
                return false;
            }
        }
        return true;
    }
    
    private boolean sendPointArrayBatch(HGrid pointGrid) {
        
        ArrayList<HDict> pointValList = new ArrayList<>();
        Iterator iterator = pointGrid.iterator();
        
        while (iterator.hasNext()) {
            HDict pointDict = (HDict) iterator.next();
            String pointId = pointDict.get(Tags.ID).toString();
            if (!syncStatusService.hasEntitySynced(pointId)) {
                continue;
            }
            ArrayList<HDict> valDictList = PointWriteUtil.getWriteArrDict(pointDict.get(Tags.ID).toString());
            if (valDictList.size() > 0) {
                pointValList.addAll(valDictList);
            }
        }
        if (pointValList.size() > 0) {
            HGrid gridData = HGridBuilder.dictsToGrid(pointValList.toArray(new HDict[0]));
            EntitySyncResponse response = HttpUtil.executeEntitySync(CCUHsApi.getInstance().getHSUrl()+ENDPOINT_POINT_WRITE_MANY,
                                                   HZincWriter.gridToString(gridData), CCUHsApi.getInstance().getJwt());
            
            if (response.getRespCode() == HttpUtil.HTTP_RESPONSE_OK) {
                return true;
            } else if (response.getRespCode() >= HttpUtil.HTTP_RESPONSE_ERR_REQUEST) {
                EntitySyncErrorHandler.handle400HttpError(CCUHsApi.getInstance(), response.getErrRespString());
            }
            return response != null;
        }
        return true;
    }
    
    
}
