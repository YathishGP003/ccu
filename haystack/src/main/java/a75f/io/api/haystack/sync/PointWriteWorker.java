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

public class PointWriteWorker extends Worker {
    
    private static final String TAG = "CCU_HS_PointWriteWorker";
    
    public static final int WRITABLE_POINT_BATCH_SIZE = 20;
    
    public static final String ENDPOINT_POINT_WRITE_MANY = "pointWriteMany";
    
    SyncStatusService syncStatusService;
    
    public PointWriteWorker(Context appContext, WorkerParameters workerParams) {
        super(appContext, workerParams);
        syncStatusService = SyncStatusService.getInstance(getApplicationContext());
    }
    
    @NonNull @Override
    public Result doWork() {
        
        CcuLog.i(TAG, "doWork : PointWriteWorker");
        
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
            //TODO - Should check if point is synced ?
            ArrayList<HDict> valDictList = getWriteArrDict(pointDict.get(Tags.ID).toString());
            if (valDictList.size() > 0) {
                pointValList.addAll(valDictList);
            }
        }
        if (pointValList.size() > 0) {
            HGrid gridData = HGridBuilder.dictsToGrid(pointValList.toArray(new HDict[0]));
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl()+ENDPOINT_POINT_WRITE_MANY,
                                                   HZincWriter.gridToString(gridData));
            return response != null;
        }
        return true;
    }
    
    public ArrayList<HDict> getWriteArrDict(String pointId) {
        ArrayList<HashMap> pointArr = CCUHsApi.getInstance().readPoint(pointId);
        ArrayList<HDict> dictArr = new ArrayList<>();
        for (HashMap valMap : pointArr) {
            if (valMap.get("val") != null)
            {
                String value = Objects.toString(valMap.get("val"), "");
                String level = Objects.toString(valMap.get("level"), "");
                String who = Objects.toString(valMap.get("who"), "");
                boolean isDouble = false;
                double numValue = 0.0;
                
                try
                {
                    numValue = Double.parseDouble( value );
                    isDouble = true;
                }
                catch (NumberFormatException e)
                {
                    CcuLog.d("CCU_HS", "Writable Val is not Double " + value);
                }
                
                HDictBuilder b =
                    new HDictBuilder().add("id", HRef.copy(pointId))
                                      .add("level", (int) Double.parseDouble(level))
                                      .add("who", who)
                                      .add("val", isDouble? HNum.make(numValue) :
                                                                               HStr.make(value));
                dictArr.add(b.toDict());
            }
        }
        return dictArr;
    }
}
