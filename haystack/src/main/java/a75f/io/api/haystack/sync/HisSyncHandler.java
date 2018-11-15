package a75f.io.api.haystack.sync;

import android.util.Log;

import org.projecthaystack.HDict;
import org.projecthaystack.HRef;

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
    HashMap<String, String> tsData;
    
    public HisSyncHandler(CCUHsApi api) {
        hayStack = api;
    }
    
    public synchronized void doSync() {
        
        Log.d("CCU", "doHisSync ->");
        ArrayList<HashMap> points = hayStack.readAll("point and his");
        if (points.size() == 0) {
            return;
        }
    
        tsData = new HashMap<>();
        
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
    
            
            
            HisItem sItem = hisItems.get(hisItems.size()-1);//TODO - Writing just the last val for now
            tsData.put(m.get("dis").toString(), String.valueOf(sItem.getVal()));
            
            
            //TODO - Influxdb java lib does not compile on SDK-19. Using POST method until it is sorted out.
            /*HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
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
    
    
    
            BatchPoints.Builder batchPointsBuilder = BatchPoints
                                                             .database(TS.getInstance().getTimeSeriesDBName());
    
            for (HHisItem hItem : hHisItems) {
                org.influxdb.dto.Point.Builder measurement = org.influxdb.dto.Point.measurement(hayStack.getGUID(pointID).replace("@",""));
                measurement.time(hItem.ts.millis(), TimeUnit.MILLISECONDS);
                measurement.addField("TZ", hItem.ts.tz.name);
                Iterator iterator = hItem.iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    String key = entry.getKey().toString();
                    if (!entry.getKey().toString().equals("ts")) {
                        HVal hVal = (HVal) entry.getValue();
                        if (hVal instanceof HNum) {
                            double curVal = ((HNum) hVal).val;
                            measurement.addField(key, curVal);
                        } else {
                            measurement.addField(key, hVal.toString());
                        }
                    }
                }
        
                batchPointsBuilder.point(measurement.build());
            }
    
            TS.getInstance().getTS().write(batchPointsBuilder.build());*/
            
            
            
            /*HDictBuilder b = new HDictBuilder();
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
            }*/
    
            for (HisItem item: hisItems)
            {
                item.setSyncStatus(true);
            }
            hayStack.tagsDb.setHisItemSyncStatus(hisItems);
        }
        
        if (tsData.size() > 0)
        {
            String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("renatus-influxiprvgkeeqfgys.centralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("75f@75f.io").setPassword("7575").buildUrl();
            InfluxDbUtil.writeData(url, "01RENATUS_CCU", tsData, System.currentTimeMillis());
        }
        
        Log.d("CCU","<- doHisSync");
    }
}
