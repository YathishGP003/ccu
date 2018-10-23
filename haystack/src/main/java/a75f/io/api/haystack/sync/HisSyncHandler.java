package a75f.io.api.haystack.sync;

import android.util.Log;

import org.projecthaystack.HBool;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.HVal;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;

/**
 * Created by samjithsadasivan on 10/18/18.
 */

public class HisSyncHandler
{
    CCUHsApi hayStack;
    
    public HisSyncHandler(CCUHsApi api) {
        hayStack = api;
    }
    
    public synchronized void doSync() {
        ArrayList<HashMap> points = hayStack.readAll("point and his");
        if (points.size() == 0) {
            return;
        }
        
        for (Map m : points)
        {
            //TODO- send all points in single call?
            String pointID = m.get("id").toString();
            if (CCUHsApi.getInstance().getGUID(pointID) == null) {
                Log.d("CCU"," Point does not have GUID "+pointID);
                HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                System.out.println(point);
                continue;
                
            }
            ArrayList<HisItem> hisItems = (ArrayList<HisItem>) CCUHsApi.getInstance().tagsDb.getUnSyncedHisItems(HRef.copy(pointID));
            if (hisItems.size() == 0) {
                continue;
            }
            
            HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
            System.out.println(point);
            boolean isBool = ((HStr) point.get("kind")).val.equals("Bool");
            ArrayList acc = new ArrayList();
            for (HisItem item : hisItems)
            {
                HVal val = isBool ? HBool.make(item.getVal() > 0) : HNum.make(item.getVal());
                HDict hsItem = HHisItem.make(HDateTime.make(item.getDate().getTime()), val);
                acc.add(hsItem);
            }
            
            HHisItem[] hHisItems = (HHisItem[]) acc.toArray(new HHisItem[acc.size()]);
            
            HDictBuilder b = new HDictBuilder();
            b.add("id", HRef.copy(CCUHsApi.getInstance().getGUID(pointID)));
            HGrid itemGrid = HGridBuilder.hisItemsToGrid(b.toDict(), hHisItems);
            itemGrid.dump();
            for (HisItem i : hisItems)
            {
                i.dump();
            }
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "hisWrite", HZincWriter.gridToString(itemGrid));
            System.out.println("Response :\n"+response);
            //TODO- success ?
            if (response.contains("empty")) {
                for (HisItem item: hisItems)
                {
                    item.setSyncStatus(true);
                }
            }
            hayStack.tagsDb.setHisItemSyncStatus(hisItems);
        }
    }
}
