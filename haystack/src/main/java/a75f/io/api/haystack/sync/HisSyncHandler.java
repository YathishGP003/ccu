package a75f.io.api.haystack.sync;

import android.util.Log;

import org.joda.time.DateTime;
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
    private static final String TAG = "CCU_HS_SYNC";
    
    CCUHsApi hayStack;
    
    public boolean entitySyncRequired = false;
    
    public HisSyncHandler(CCUHsApi api) {
        hayStack = api;
    }
    
    public synchronized void doSync() {
        CcuLog.d(TAG, "doHisSync ->");
        sendHisToInflux();

        if (entitySyncRequired) {
            CcuLog.d(TAG,"doHisSync : entitySyncRequired");
            CCUHsApi.getInstance().syncEntityTree();
            entitySyncRequired = false;
        }
        CcuLog.d(TAG,"<- doHisSync");
    }
    
    /**
    * Single measurement is created for an equip with current time as time stamp.
    * Most recent unsynced value is sent for each point.
    * All the local history entries marked 'synced' once the data is wrote to influx.
    * */
    private void sendHisToInflux() {
        if (!CCUHsApi.getInstance().isCCURegistered()){
            return;
        }
        ArrayList<HashMap> equips = hayStack.readAll("equip");
        DateTime now = new DateTime();
        
        for (HashMap equip : equips) {
            CcuLog.d(TAG," sendHisToInflux Equip "+equip.get("dis"));
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
                    CcuLog.d(TAG,"Skip hisSync; point does not have GUID "+pointID);
                    if(pointID != null) {
                        HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                        CcuLog.d(TAG, "" + point);
                    }
                    entitySyncRequired = true;
                    continue;
        
                }
                if ((now.getMinuteOfDay() % 15) == 0) {
                    HisItem hisVal = hayStack.tagsDb.getLastHisItem(HRef.copy(pointID));
                    if (hisVal == null || !hisVal.initialized) {
                        CcuLog.d(TAG, "His val not found : "+m.get("dis"));
                        continue;
                    }
                    tsData.put( pointGUID.replace("@",""), String.valueOf(hisVal.getVal()));
                    hayStack.tagsDb.removeHisItems(HRef.copy(pointID));
                }else {
                    ArrayList<HisItem> hisItems = (ArrayList<HisItem>) hayStack.tagsDb.getUnSyncedHisItems(HRef.copy(pointID));
                    if (hisItems.size() > 0) {
                        HisItem sItem = hisItems.get(hisItems.size() - 1); //Writing just the recent val?
                        tsData.put(pointGUID.replace("@", ""), String.valueOf(sItem.getVal()));
                        for (HisItem item : hisItems) {
                            item.setSyncStatus(true);
                        }
                        hayStack.tagsDb.setHisItemSyncStatus(hisItems);
                        hayStack.tagsDb.removeHisItems(HRef.copy(pointID));
                    }else
                        continue;
                }
            
            }
            
            if (tsData.size() > 0)
            {
                if(CCUHsApi.getInstance().isNetworkConnected()) {
                    CcuLog.d(TAG, " sendHisToInflux tsData111= " + tsData.size() + "," + tsData.toString());
//                    InfluxDbUtil.writeData(CCUHsApi.getInstance().getInfluxUrl(), CCUHsApi.getInstance().getGUID(equip.get("id").toString()).toString().replace("@", "")
//                            , tsData, System.currentTimeMillis());
                }
            }
        
        }

        boolean syncAllHisItemsNow = ((now.getMinuteOfDay() % 15) == 0);
        sendDeviceHisData(syncAllHisItemsNow);

    }
    
    private void sendDeviceHisData(boolean syncAllHisItemsNow) {
        ArrayList<HashMap> devices = hayStack.readAll("device");
        for (HashMap device : devices) {
            if (device.get("ccu") != null) {
                continue;
            }
            CcuLog.d(TAG," sendHisToInflux device "+device.get("dis"));
            ArrayList<HashMap> points = hayStack.readAll("point and his and deviceRef == \""+device.get("id")+"\"");
            if (CCUHsApi.getInstance().getGUID(device.get("id").toString()) == null) {
                entitySyncRequired = true;
                continue;
            }
        
            HashMap tsData = new HashMap<>();
        
            for (Map m : points)
            {
                String pointID = m.get("id").toString();
                String pointGUID = CCUHsApi.getInstance().getGUID(pointID);
                if (pointGUID == null) {
                    CcuLog.d(TAG,"Skip hisSync; point does not have GUID "+pointID);
                    if(pointID != null) { //Crash because Floor is deleted and corresponding points also deleted
                        HDict point = hayStack.hsClient.readById(HRef.copy(pointID));
                        CcuLog.d(TAG, "" + point);
                    }
                    entitySyncRequired = true;
                    continue;
                
                }
                if(syncAllHisItemsNow){
                    HisItem hisVal = hayStack.tagsDb.getLastHisItem(HRef.copy(pointID));
                    if (hisVal == null || !hisVal.initialized) {
                        CcuLog.d(TAG, "His val not found : "+m.get("dis"));
                        continue;
                    }
                    tsData.put( pointGUID.replace("@",""), String.valueOf(hisVal.getVal()));

                    hayStack.tagsDb.removeHisItems(HRef.copy(pointID));
                }else {
                    ArrayList<HisItem> hisItems = (ArrayList<HisItem>) hayStack.tagsDb.getUnSyncedHisItems(HRef.copy(pointID));
                    if (hisItems.size() > 0) {
                        HisItem sItem = hisItems.get(hisItems.size() - 1);//TODO - Writing just the recent his val?
                        tsData.put(pointGUID.replace("@", ""), String.valueOf(sItem.getVal()));

                        for (HisItem item : hisItems) {
                            item.setSyncStatus(true);
                        }
                        hayStack.tagsDb.setHisItemSyncStatus(hisItems);
                        hayStack.tagsDb.removeHisItems(HRef.copy(pointID));
                    }else
                        continue;
                }
            }
        
            if (tsData.size() > 0)
            {
                if(CCUHsApi.getInstance().isNetworkConnected()) {
                    CcuLog.d(TAG, " sendHisToInflux device tsData= " + tsData.size() + "," + tsData.toString());
//                    InfluxDbUtil.writeData(CCUHsApi.getInstance().getInfluxUrl(), CCUHsApi.getInstance().getGUID(device.get("id").toString()).toString().replace("@", "")
//                            , tsData, System.currentTimeMillis());
                }
            }
        }
    }
}
