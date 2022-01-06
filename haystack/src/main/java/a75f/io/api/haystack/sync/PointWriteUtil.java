package a75f.io.api.haystack.sync;

import org.apache.commons.collections4.ListUtils;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;

public class PointWriteUtil {
    
    public static final int WRITABLE_POINT_BATCH_SIZE = 20;
    
    public static final String ENDPOINT_POINT_WRITE_MANY = "pointWriteMany";
    
    public static ArrayList<HDict> getWriteArrDict(String pointId) {
        ArrayList<HashMap> pointArr = CCUHsApi.getInstance().readPoint(pointId);
        ArrayList<HDict> dictArr = new ArrayList<>();
        for (HashMap valMap : pointArr) {
            if (valMap.get("val") != null) {
                String value = Objects.toString(valMap.get("val"), "");
                String level = Objects.toString(valMap.get("level"), "");
                String who = Objects.toString(valMap.get("who"), "");
                boolean isDouble = false;
                double numValue = 0.0;
                
                try {
                    numValue = Double.parseDouble( value );
                    isDouble = true;
                } catch (NumberFormatException e) {
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
    
    public static void sendPointArrayData(List<String> idList) {
    
        List<String> writableIdList = idList.stream()
                                           .filter(id -> CCUHsApi.getInstance().readMapById(id).containsKey(Tags.WRITABLE))
                                           .collect(Collectors.toList());
        
        List<List<String>> pointListBatches = ListUtils.partition(writableIdList, WRITABLE_POINT_BATCH_SIZE);
        
        for (List<String> pointList : pointListBatches) {
            ArrayList<HDict> pointValList = new ArrayList<>();
            for (String pointId : pointList) {
                ArrayList<HDict> valDictList = PointWriteUtil.getWriteArrDict(pointId);
                if (valDictList.size() > 0) {
                    pointValList.addAll(valDictList);
                }
            }
            if (pointValList.size() > 0) {
                HGrid gridData = HGridBuilder.dictsToGrid(pointValList.toArray(new HDict[0]));
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + ENDPOINT_POINT_WRITE_MANY,
                                                       HZincWriter.gridToString(gridData));
                //We ignore failure here as point arrays are written during every application restart.
                CcuLog.i("CCU_HS", "PointWrite response " + response);
            }
        }
    }
}
