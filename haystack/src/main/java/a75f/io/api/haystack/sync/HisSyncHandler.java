package a75f.io.api.haystack.sync;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;
import org.projecthaystack.HBool;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.HTimeZone;
import org.projecthaystack.HVal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HisSyncHandler
{
    private static final String TAG = "CCU_HS_SYNC";
    private static final String SYNC_TYPE_EQUIP = "equip";
    private static final String SYNC_TYPE_DEVICE = "device";
    
    CCUHsApi ccuHsApi;
    
    public boolean entitySyncRequired = false;
    Lock syncLock = new ReentrantLock();
    
    public HisSyncHandler(CCUHsApi api) {
        ccuHsApi = api;
    }
    
    private void sync() {
        CcuLog.d(TAG, "doHisSync ->");

        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() > 0) {
            String siteGUID = CCUHsApi.getInstance().getGlobalSiteId();

            CcuLog.d(TAG,"Site is found during sync.");

            if (StringUtils.isNotBlank(siteGUID)) {
                CcuLog.d(TAG,"Site GUID" + siteGUID + " is found");

                DateTime now = new DateTime();
                boolean timeForQuarterHourSync = now.getMinuteOfDay() % 15 == 0 ? true : false;

                if (CCUHsApi.getInstance().isCCURegistered() && CCUHsApi.getInstance().isNetworkConnected()) {
                    CcuLog.d(TAG,"Processing sync for equips and devices");
                    syncHistorizedEquipPoints(timeForQuarterHourSync);
                    syncHistorizedDevicePoints(timeForQuarterHourSync);
                }

                if (entitySyncRequired) {
                    CcuLog.d(TAG,"doHisSync : entitySyncRequired");
                    CCUHsApi.getInstance().syncEntityTree();
                    entitySyncRequired = false;
                }
            }
        }
        doPurge();
        CcuLog.d(TAG,"<- doHisSync");
    }
    
    public void syncData() {
        if (syncLock.tryLock()) {
            try {
                sync();
            }
            finally {
                syncLock.unlock();
            }
        } else {
            CcuLog.d(TAG,"No need to start HisSync. Already in progress.");
        }
    }

    private void syncHistorizedEquipPoints(boolean timeForQuarterHourSync) {
        List<HashMap> allEquips = ccuHsApi.readAll("equip");
        List<HashMap> equipsToSync = getEntitiesWithGuidForSyncing(allEquips);

        for (HashMap equipToSync : equipsToSync) {
            String equipId = equipToSync.get("id").toString();
            String equipGuid = CCUHsApi.getInstance().getGUID(equipId);
            List<HashMap> allPointsForEquip = ccuHsApi.readAll("point and his and equipRef == \""+ equipId +"\"");
            CcuLog.d(TAG,"Found " + allPointsForEquip.size() + " equip points");
            List<HashMap> pointsToSyncForEquip = getEntitiesWithGuidForSyncing(allPointsForEquip);
            CcuLog.d(TAG,"Found " + pointsToSyncForEquip.size() + " equip points that have a GUID for syncing");
            if (!pointsToSyncForEquip.isEmpty()) {
                syncPoints(equipGuid, pointsToSyncForEquip, timeForQuarterHourSync, SYNC_TYPE_EQUIP);
            }
        }
    }

    private void syncHistorizedDevicePoints(boolean timeForQuarterHourSync) {
    List<HashMap> allEquips = ccuHsApi.readAll("device");
    List<HashMap> devicesToSync = getEntitiesWithGuidForSyncing(allEquips);

        for (HashMap deviceToSync : devicesToSync) {
            String deviceId = deviceToSync.get("id").toString();
            String deviceGuid = CCUHsApi.getInstance().getGUID(deviceId);
            List<HashMap> allPointsForDevice = ccuHsApi.readAll("point and his and deviceRef == \""+ deviceId +"\"");
            CcuLog.d(TAG,"Found " + allPointsForDevice.size() + " device points");
            List<HashMap> pointsToSyncForDevice = getEntitiesWithGuidForSyncing(allPointsForDevice);
            CcuLog.d(TAG,"Found " + pointsToSyncForDevice.size() + " device points that have a GUID for syncing");
            if (!pointsToSyncForDevice.isEmpty()) {
                syncPoints(deviceGuid, pointsToSyncForDevice, timeForQuarterHourSync, SYNC_TYPE_DEVICE);
            }
        }
    }


    private void syncPoints(String deviceOrEquipGuid, List<HashMap> pointList, boolean timeForQuarterHourSync, String syncType) {

        List<HisItem> hisItemsToSyncForDeviceOrEquip = new ArrayList<>();
        List<HDict> hDictList = new ArrayList<>();
        Date quarterHourSyncDateTimeForDeviceOrEquip = new Date(System.currentTimeMillis());

        for (HashMap pointToSync : pointList) {

            List<HisItem> unsyncedHisItems = new ArrayList<>();

            String pointID = pointToSync.get("id").toString();
            String pointDescription = pointToSync.get("dis").toString();
            String pointGuid = CCUHsApi.getInstance().getGUID(pointID);
            boolean isBooleanPoint = ((HStr) pointToSync.get("kind")).val.equals("Bool");

            unsyncedHisItems = ccuHsApi.tagsDb.getUnsyncedHisItemsOrderDesc(pointID);

            if (!unsyncedHisItems.isEmpty()) {
                for (HisItem hisItem : unsyncedHisItems) {

                    String pointTimezone = pointToSync.get("tz").toString();
                    HVal pointValue = isBooleanPoint ? HBool.make(hisItem.getVal() > 0) : HNum.make(hisItem.getVal());
                    long pointTimestamp = hisItem.getDateInMillis();

                    if (!StringUtils.equals("NaN", pointValue.toString())) {
                        HDict hDict = buildHDict(pointGuid, pointTimezone, pointValue, pointTimestamp);
                        hDictList.add(hDict);
                        hisItemsToSyncForDeviceOrEquip.add(hisItem);
                        CcuLog.d(TAG,"Adding historized point value for GUID " + pointGuid + "; point ID " + pointID + "; description of " + pointDescription + "; value of " + pointValue + " for syncing.");
                    } else {
                        CcuLog.e(TAG,"Historized point value for GUID " + pointGuid + "; point ID " + pointID + "; description of " + pointDescription + " is null. Skipping.");
                    }
                }
            } else if (unsyncedHisItems.isEmpty() && timeForQuarterHourSync) {

                HisItem latestHisItemToReSync = ccuHsApi.tagsDb.getLastHisItem(HRef.copy(pointID));

                if (latestHisItemToReSync != null) {
                    // TODO Matt Rudd - Feels a bit wrong to duplicate this code from the for loop for unsynced items, but I don't want to mutate objects or have a method just to set variables
                    latestHisItemToReSync.setDate(quarterHourSyncDateTimeForDeviceOrEquip);

                    String pointTimezone = pointToSync.get("tz").toString();
                    HVal pointValue = isBooleanPoint ? HBool.make(latestHisItemToReSync.getVal() > 0) : HNum.make(latestHisItemToReSync.getVal());
                    long pointTimestamp = latestHisItemToReSync.getDateInMillis();

                    HDict hDict = buildHDict(pointGuid, pointTimezone, pointValue, pointTimestamp);
                    hDictList.add(hDict);
                    CcuLog.d(TAG,
                             "There are no unsynced historized items for point GUID " + pointGuid +  "-" +pointToSync.get("dis")+
                                            " :resyncing with time of " + quarterHourSyncDateTimeForDeviceOrEquip + "; value of " + pointValue);
                } else {
                    CcuLog.d(TAG,"LastSyncItem is empty for "+pointToSync.get("dis"));
                }
            }
        }

        if (!hDictList.isEmpty()) {
            CcuLog.d(TAG,"Syncing a point list of size " + pointList.size());

            HDict[] hDicts = hDictListToArray(hDictList);

            HDict hisWriteMetadata = new HDictBuilder()
                    .add("id", HRef.make(StringUtils.stripStart(deviceOrEquipGuid,"@")))
                    .add(syncType)
                    .toDict();

            String response = CCUHsApi.getInstance().hisWriteManyToHaystackService(hisWriteMetadata, hDicts);
            if (response != null && !hisItemsToSyncForDeviceOrEquip.isEmpty()) {
                try {
                    ccuHsApi.tagsDb.updateHisItemSynced(hisItemsToSyncForDeviceOrEquip);
                } catch (IllegalArgumentException e) {
                    /* There is a corner case where this HisItem might have been removed from Objectbox since the
                     * PruneJob runs on a different thread. Object box throws IllegalArgumentException in that
                     * situation.It appears to be safe to ignore now. But we will still track by printing the stack trace to
                     * monitor how frequently this is happening or there is more to it than what we see now.
                     */
                    e.printStackTrace();
                }
            }
        }
    }

    private HDict[] hDictListToArray(List<HDict> hDictList) {
        HDict[] hDicts = null;

        if (hDictList != null && hDictList.size() > 0) {
            hDicts = new HDict[hDictList.size()];
            int hDictIterator = 0;
            for (HDict hDict : hDictList) {
                hDicts[hDictIterator] = hDict;
                hDictIterator++;
            }
        }

        return hDicts;
    }

    private HDict buildHDict(String pointGuid, String pointTimezone, HVal pointValue, long pointTimestamp) {

        HTimeZone hTimeZone = HTimeZone.make(pointTimezone);
        HDateTime hDateTime = HDateTime.make(pointTimestamp, hTimeZone);

        HDict hDict = new HDictBuilder()
                .add("id", HRef.make(StringUtils.stripStart(pointGuid,"@")))
                .add("val", pointValue)
                .add("ts", hDateTime)
                .toDict();

        return hDict;
    }

    private List<HashMap> getEntitiesWithGuidForSyncing(List<HashMap> entityList) {
        List<HashMap> entitiesWithGuid = new ArrayList<>();

        for (HashMap entity : entityList) {
            if (CCUHsApi.getInstance().getGUID(entity.get("id").toString()) != null) {
                entitiesWithGuid.add(entity);
            } else if (!entitySyncRequired) {
                entitySyncRequired = true;
            }
        }

        return entitiesWithGuid;
    }
    
    private void doPurge() {
    
        DateTime now = new DateTime();
        boolean timeForPurge = now.getMinuteOfDay() % 10 == 0 ? true : false;
        
        if (timeForPurge) {
            Thread purgeThread = new Thread() {
                @Override public void run() {
                    super.run();
                    CcuLog.d(TAG, "doPurge ->");
                    ArrayList<HashMap<Object, Object>> allHisPoints = ccuHsApi.readAllEntities("point and his");
                    for (HashMap<Object, Object> point : allHisPoints) {
                        ccuHsApi.tagsDb.removeExpiredHisItems(HRef.copy(point.get("id").toString()));
                    }
                    CcuLog.d(TAG, "<- doPurge");
                }
            };
            purgeThread.start();
        }
    }
}
