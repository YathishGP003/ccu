package a75f.io.api.haystack.sync;

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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 10/18/18.
 */

public class HisSyncHandler
{
    CCUHsApi hayStack;
    HashMap<String, String> tsData;
    
    public boolean entitySyncRequired = false;
    
    public HisSyncHandler(CCUHsApi api) {
        hayStack = api;
    }
    
    public synchronized void doSync() {
    
        CcuLog.d("CCU_HS", "doHisSync ->");
        //sendHisToHaystack();
        sendHisToInflux();
        //sendHisToInfluxBatched();
        
        if (entitySyncRequired) {
            CcuLog.d("CCU_HS","doHisSync : entitySyncRequired");
            CCUHsApi.getInstance().syncEntityTree();
            entitySyncRequired = false;
        }
        CcuLog.d("CCU_HS","<- doHisSync");
        
    }
    
    /**
     * His data is uploaded to haystack server , which then writes to influx.
     */
    private void sendHisToHaystack() {
        ArrayList<HashMap> points = hayStack.readAll("point and his");
        if (points.size() == 0) {
            return;
        }
        for (Map m : points)
        {
            //TODO- send all points in single call?
            String pointID = m.get("id").toString();
            if (CCUHsApi.getInstance().getGUID(pointID) == null) {
                CcuLog.d("CCU_HS"," Point does not have GUID "+pointID);
                HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                CcuLog.d("CCU_HS", ""+point);
                entitySyncRequired = true;
                continue;
            
            }
            ArrayList<HisItem> hisItems = (ArrayList<HisItem>) CCUHsApi.getInstance().tagsDb.getUnSyncedHisItems(HRef.copy(pointID));
            if (hisItems.size() == 0) {
                continue;
            }
        
            HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
            CcuLog.d("CCU_HS",""+point);
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
            CcuLog.d("CCU_HS","Response :\n"+response);
            if (response != null) {
                for (HisItem item: hisItems)
                {
                    item.setSyncStatus(true);
                }
                hayStack.tagsDb.setHisItemSyncStatus(hisItems);
            }
            
        }
        
    }
    
    /**
    * Single measurement is created for an equip with current time as time stamp.
    * Most recent unsynced value is sent for each point.
    * All the local history entries marked 'synced' once the data is wrote to influx.
    * */
    private void sendHisToInflux() {
        
        ArrayList<HashMap> equips = hayStack.readAll("equip");
        for (HashMap equip : equips) {
            CcuLog.d("CCU_HS"," sendHisToInflux Equip "+equip.get("dis"));
            ArrayList<HashMap> points = hayStack.readAll("point and his and equipRef == \""+equip.get("id")+"\"");
            if (CCUHsApi.getInstance().getGUID(equip.get("id").toString()) == null) {
                entitySyncRequired = true;
                continue;
            }
    
            HashMap tsData = new HashMap<>();
        
            for (Map m : points)
            {
                String pointID = m.get("id").toString();
                String pointGUID = CCUHsApi.getInstance().getGUID(pointID);
                if (pointGUID == null) {
                    CcuLog.d("CCU_HS","Skip hisSync; point does not have GUID "+pointID);
                    HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                    CcuLog.d("CCU_HS",""+point);
                    entitySyncRequired = true;
                    continue;
        
                }
                ArrayList<HisItem> hisItems = (ArrayList<HisItem>) hayStack.tagsDb.getUnSyncedHisItems(HRef.copy(pointID));
                if (hisItems.size() == 0) {
                    continue;
                }
    
                HisItem sItem = hisItems.get(hisItems.size()-1);//TODO - Writing just the recent his val?
                tsData.put( pointGUID.replace("@",""), String.valueOf(sItem.getVal()));
                /*for (HisItem item: hisItems)
                {
                    item.setSyncStatus(true);
                }
                hayStack.tagsDb.setHisItemSyncStatus(hisItems);*/
                hayStack.tagsDb.removeHisItems(HRef.copy(pointID));
            
            }
            
            if (tsData.size() > 0)
            {
                String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("renatus-influxiprvgkeeqfgys.centralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("75f@75f.io").setPassword("7575").buildUrl();
                InfluxDbUtil.writeData(url, CCUHsApi.getInstance().getGUID(equip.get("id").toString()).toString().replace("@","")
                                                , tsData, System.currentTimeMillis());
            }
        
        }
    
    
        ArrayList<HashMap> devices = hayStack.readAll("device");
        for (HashMap device : devices) {
            ArrayList<HashMap> points = hayStack.readAll("point and his and deviceRef == \""+device.get("id")+"\"");
            if (CCUHsApi.getInstance().getGUID(device.get("id").toString()) == null) {
                entitySyncRequired = true;
                continue;
            }
        
            HashMap tsData = new HashMap<>();
        
            for (Map m : points)
            {
                String pointID = m.get("id").toString();
                if (CCUHsApi.getInstance().getGUID(pointID) == null) {
                    CcuLog.d("CCU_HS","Skip hisSync; point does not have GUID "+pointID);
                    HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                    CcuLog.d("CCU_HS", ""+point);
                    entitySyncRequired = true;
                    continue;
                
                }
                ArrayList<HisItem> hisItems = (ArrayList<HisItem>) hayStack.tagsDb.getUnSyncedHisItems(HRef.copy(pointID));
                if (hisItems.size() == 0) {
                    continue;
                }
            
                HisItem sItem = hisItems.get(hisItems.size()-1);//TODO - Writing just the recent his val?
                tsData.put( CCUHsApi.getInstance().getGUID(m.get("id").toString()).toString().replace("@",""), String.valueOf(sItem.getVal()));
            
                /*for (HisItem item: hisItems)
                {
                    item.setSyncStatus(true);
                }
                hayStack.tagsDb.setHisItemSyncStatus(hisItems);*/
                hayStack.tagsDb.removeHisItems(HRef.copy(pointID));
            
            }
        
            if (tsData.size() > 0)
            {
                String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("renatus-influxiprvgkeeqfgys.centralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("75f@75f.io").setPassword("7575").buildUrl();
                InfluxDbUtil.writeData(url, CCUHsApi.getInstance().getGUID(device.get("id").toString()).toString().replace("@","")
                        , tsData, System.currentTimeMillis());
            }
        
        }
    }
    
    /**
     * Batched write to influx.
     */
    //TODO - WIP , Cannot be used as Influxdb java lib does not load on SDK-19
    private void sendHisToInfluxBatched() {
        ArrayList<HashMap> equips = hayStack.readAll("equip");
        HashMap tsData = new HashMap<>();
        /*BatchPoints.Builder batchPointsBuilder = BatchPoints
                                                         .database(TS.getInstance().getTimeSeriesDBName());*/
        
        for (HashMap equip : equips) {
            ArrayList<HashMap> points = hayStack.readAll("point and his equipRef == \""+equip.get("id")+"\"");
            if (CCUHsApi.getInstance().getGUID(equip.get("id").toString()) == null) {
                continue;
            }
            org.influxdb.dto.Point.Builder measurement = org.influxdb.dto.Point.measurement(CCUHsApi.getInstance()
                                        .getGUID(equip.get("id").toString()).replace("@",""));
        
            for (Map m : points)
            {
                String pointID = m.get("id").toString();
                if (CCUHsApi.getInstance().getGUID(pointID) == null) {
                    CcuLog.d("CCU_HS"," Point does not have GUID "+pointID);
                    HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                    CcuLog.d("CCU_HS",""+point);
                    continue;
                
                }
                ArrayList<HisItem> hisItems = (ArrayList<HisItem>) CCUHsApi.getInstance().tagsDb.getUnSyncedHisItems(HRef.copy(pointID));
                if (hisItems.size() == 0) {
                    continue;
                }
    
                HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                CcuLog.d("CCU_HS", ""+point);
                boolean isBool = ((HStr) point.get("kind")).val.equals("Bool");
                ArrayList acc = new ArrayList();
                for (HisItem item : hisItems)
                {
                    HVal val = isBool ? HBool.make(item.getVal() > 0) : HNum.make(item.getVal());
                    HDict hsItem = HHisItem.make(HDateTime.make(item.getDate().getTime()), val);
                    acc.add(hsItem);
                }
    
                HHisItem[] hHisItems = (HHisItem[]) acc.toArray(new HHisItem[acc.size()]);
            
                for (HHisItem hItem : hHisItems) {
                    
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
    
                    CcuLog.d("CCU_HS","Write influx point "+measurement.toString());
                    //batchPointsBuilder.point(measurement.build());
                }
                for (HisItem item: hisItems)
                {
                    item.setSyncStatus(true);
                }
                hayStack.tagsDb.setHisItemSyncStatus(hisItems);
            
            }
            //TS.getInstance().getTS().write(batchPointsBuilder.build());
        
        }
    }
}
