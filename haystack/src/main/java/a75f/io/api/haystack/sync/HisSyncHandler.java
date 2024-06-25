package a75f.io.api.haystack.sync;


import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HBool;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.HTimeZone;
import org.projecthaystack.HVal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.HisItemCache;
import a75f.io.api.haystack.Kind;
import a75f.io.logger.CcuLog;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HisSyncHandler
{
    private static final String TAG = "CCU_HS_SYNC";
    private static final String SYNC_TYPE_EQUIP = "equip";
    private static final String SYNC_TYPE_DEVICE = "device";

    private static final int HIS_ITEM_BATCH_SIZE = 500;

    //For testing
    private int totalHisItemCount = 0;
    int numberOfPoints = 0;

    CCUHsApi ccuHsApi;

    public boolean entitySyncRequired = false;
    Lock syncLock = new ReentrantLock();

    private volatile boolean purgeStatus = false;
    
    Timer     mSyncTimer     = new Timer();
    TimerTask mSyncTimerTask = null;
    
    private boolean nonCovSyncPending = false;

    private long lastPurgeTime  = 0;
    
    public HisSyncHandler(CCUHsApi api) {
        ccuHsApi = api;
    }

    private void sync() {
        CcuLog.d(TAG, "doHisSync ->");

        HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity("site");
        if (!site.isEmpty()) {
            String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();

            CcuLog.d(TAG,"Site is found during sync.");

            if (StringUtils.isNotBlank(siteUID)) {
                CcuLog.d(TAG,"Site GUID" + siteUID + " is found");
                
                if (CCUHsApi.getInstance().isCCURegistered() && CCUHsApi.getInstance().isNetworkConnected()) {
                   doSync(nonCovSyncPending);
                }

                if (entitySyncRequired) {
                    CcuLog.d(TAG,"doHisSync : entitySyncRequired");
                    CCUHsApi.getInstance().scheduleSync();
                    entitySyncRequired = false;
                }
            }
        }
        CcuLog.d(TAG,"<- doHisSync");
    }
    
    private void doSync(boolean syncAllData) {
        if(ccuHsApi.readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d("CCU_HS"," Skip his sync in offlineMode");
            return;
        }

        int cacheSyncFrequency = Math.max(ccuHsApi.getCacheSyncFrequency(), 1);
        int numberOfHisEntryPerPoint = getNumberOfHisEntriesPerPoint();
        CcuLog.d(TAG,"Processing sync for equips and devices: syncAllData "+syncAllData+" cacheSyncFrequency "+cacheSyncFrequency);
        if (syncAllData) {
            nonCovSyncPending = false;
        }
        syncCachedHisData();

        if (ccuHsApi.getAppAliveMinutes() % cacheSyncFrequency == 0) {
            CcuLog.d(TAG,"syncDBHisData");
            //Device sync is initiated concurrently on Rx thread
            Observable.fromCallable(() -> {
                        syncHistorizedDevicePoints(syncAllData, numberOfHisEntryPerPoint);
                        return true;
                    }).doOnError(throwable -> CcuLog.d(TAG, "Historized device points sync failed: " + throwable.getMessage()))
                    .subscribeOn(Schedulers.io())
                    .subscribe();

            //Equip sync is still happening on the hisSync thread to avoid multiple sync sessions.
            syncHistorizedEquipPoints(syncAllData, numberOfHisEntryPerPoint);

            syncHistorizedZonePoints(syncAllData, numberOfHisEntryPerPoint);

            ccuHsApi.tagsDb.persistUnsyncedCachedItems();

        }
    }
    
    /**
     * Must be invoked for regular period history data sync.
     *
     */
    public void syncHisDataWithPurge() {
        if (syncLock.tryLock()) {
            try {
                sync();
            } catch (Exception e) {
                CcuLog.e(TAG,"HisSync Sync Failed ", e);
            } finally {
                /*
                * Forcing purge to run every 10 minutes
                * */
                if((System.currentTimeMillis() - lastPurgeTime) >= 600000) {
                    CcuLog.e(TAG,"its more than 10 minutes, purge it");
                    doPurge();
                }
                syncLock.unlock();
            }
        } else {
            CcuLog.d(TAG,"No need to start HisSync. Already in progress.");
        }

    }

    private void syncCachedHisData() {
        CcuLog.d(TAG,"syncCachedHisData");
        List<HisItem> unSyncedItems = ccuHsApi.tagsDb.getUnSyncedCachedHisData();
        String pointTimezone = ccuHsApi.getTimeZone();
        List<HisItem> hisItemList = new ArrayList<>();
        List<HDict> hDictList = new ArrayList<>();
        for (HisItem hisItem : unSyncedItems) {
            try {
                HVal pointValue = HNum.make(hisItem.getVal());
                long pointTimestamp = hisItem.getDateInMillis();
                if (!StringUtils.equals("NaN", pointValue.toString())) {
                    HDict hDict = buildHDict(hisItem.getRec(), pointTimezone, pointValue, pointTimestamp);
                    hDictList.add(hDict);
                    hisItemList.add(hisItem);
                    CcuLog.d(TAG, "syncCachedHisData: Adding historized value point ID " + hisItem.getRec() +
                            " value of " + pointValue + " for syncing; hisItem: "+hisItem);
                } else {
                    CcuLog.e(TAG, "syncCachedHisData: Historized point value for point ID " + hisItem.getRec()
                            + " is null. Skipping; hisItem: "+hisItem);
                }
            } catch (Exception e) {
                //Found a corrupted history entry. We don't want to abort the hisWrite session.
                CcuLog.e(TAG, "Invalid hisItem "+hisItem, e);
            }
            if (hisItemList.size() > HIS_ITEM_BATCH_SIZE) {
                writeCachedHisData(hDictList, hisItemList);
                hisItemList.clear();
                hDictList.clear();
            }
        }
        if (!hDictList.isEmpty()) {
            writeCachedHisData(hDictList, hisItemList);
        }

    }

    private void writeCachedHisData(List<HDict> hDictList, List<HisItem> hisItemList) {
        HDict[] hDicts = hDictListToArray(hDictList);

        EntitySyncResponse response = CCUHsApi.getInstance().hisWriteManyToHaystackService(null, hDicts);
        if (response != null) {
            CcuLog.e(TAG, "response " + response.getRespCode() + " : " + response.getErrRespString());
            if (response.getRespCode() == HttpUtil.HTTP_RESPONSE_OK && !hisItemList.isEmpty()) {
                try {
                    ccuHsApi.tagsDb.updateHisItemCache(hisItemList);
                } catch (IllegalArgumentException | OnErrorNotImplementedException e) {
                    CcuLog.e(TAG, "Failed to update HisItem !", e);
                }
            } else if (response.getRespCode() >= HttpUtil.HTTP_RESPONSE_ERR_REQUEST) {
                CcuLog.e(TAG, "His write failed! , Trying to handle the error");
                EntitySyncErrorHandler.handle400HttpError(ccuHsApi, response.getErrRespString());
                // Marking his items are synched to confirm
                ccuHsApi.tagsDb.updateHisItemCache(hisItemList);
            }
        }else{
            CcuLog.e(TAG, "null response");
        }
    }

    private void syncHistorizedEquipPoints(boolean timeForQuarterHourSync, int numberOfHisEntryPerPoint) {
        List<HashMap<Object, Object>> allEquips = ccuHsApi.readAllEntities("equip");
        List<HashMap<Object, Object>> equipsToSync = getEntitiesWithGuidForSyncing(allEquips);
        int totalNumberOfEquipPoints = 0;

        for (HashMap<Object, Object> equipToSync : equipsToSync) {
            String equipId = equipToSync.get("id").toString();
            if (!CCUHsApi.getInstance().hasEntitySynced(equipId)) {
               continue;
            }
            List<HashMap<Object, Object>> allPointsForEquip = ccuHsApi.readAllEntities("point and his and equipRef == \""+ equipId +"\"");
            CcuLog.d(TAG,"Found " + allPointsForEquip.size() + " equip points");
            List<HashMap<Object, Object>> pointsToSyncForEquip = getEntitiesWithGuidForSyncing(allPointsForEquip);
            totalNumberOfEquipPoints = totalNumberOfEquipPoints + pointsToSyncForEquip.size();
            CcuLog.d(TAG,"Found " + pointsToSyncForEquip.size() + " equip points that have a GUID for syncing");
            if (!pointsToSyncForEquip.isEmpty()) {
                syncPoints(equipId, pointsToSyncForEquip, timeForQuarterHourSync, SYNC_TYPE_EQUIP, numberOfHisEntryPerPoint);
            }
        }
        numberOfPoints = numberOfPoints + totalNumberOfEquipPoints;
    }

    /**
     * This is a short cut to get all the occupancy points on rooms to get synced.
     * This must be revisited.
     */
    private void syncHistorizedZonePoints(boolean timeForQuarterHourSync, int numberOfHisEntryPerPoint) {
        List<HashMap<Object, Object>> allZones = ccuHsApi.readAllEntities("room");
        List<HashMap<Object, Object>> zonesToSync = getEntitiesWithGuidForSyncing(allZones);
        int totalNumberOfZonePoints = 0;

        for (HashMap<Object, Object> zone : zonesToSync) {
            String roomId = zone.get("id").toString();

            ArrayList<HashMap<Object, Object>> allPointsForZone =
                ccuHsApi.readAllEntities("point and his and occupancy and state and roomRef == \""+ roomId +"\"");
            ArrayList<HashMap<Object, Object>> schedulablePoints = ccuHsApi.readAllEntities("point and his and schedulable and roomRef == \"" + roomId + "\"");
            allPointsForZone.addAll(schedulablePoints);
            CcuLog.d(TAG,"Found " + allPointsForZone.size() + " zone points");
            List<HashMap<Object, Object>> hvacModePoint =
                    ccuHsApi.readAllEntities("hvacMode and his and zone and roomRef == \""+ roomId +"\"");
            CcuLog.d(TAG,"Found " + allPointsForZone.size() + " zone points"+hvacModePoint);
            List<HashMap<Object, Object>> pointsToSyncForEquip = getEntitiesWithGuidForSyncing(allPointsForZone);
            List<HashMap<Object, Object>> hvacModePointForEquip = getEntitiesWithGuidForSyncing(hvacModePoint);
            totalNumberOfZonePoints = totalNumberOfZonePoints + pointsToSyncForEquip.size() + hvacModePointForEquip.size();
            CcuLog.d(TAG,"Found " + pointsToSyncForEquip.size() + " zone points that have a GUID for syncing");
            if (!pointsToSyncForEquip.isEmpty()) {
                syncPoints(roomId, pointsToSyncForEquip, timeForQuarterHourSync, SYNC_TYPE_EQUIP, numberOfHisEntryPerPoint);
            }
            if (!hvacModePointForEquip.isEmpty()) {
                syncPoints(roomId, hvacModePointForEquip, timeForQuarterHourSync, SYNC_TYPE_EQUIP, numberOfHisEntryPerPoint);
            }
        }
        //For testing purpose, will be removed before merging
        numberOfPoints = 0;
        totalHisItemCount = 0;
    }

    private void syncHistorizedDevicePoints(boolean timeForQuarterHourSync, int numberOfHisEntryPerPoint) {
    List<HashMap<Object, Object>> allEquips = ccuHsApi.readAllEntities("device");
    List<HashMap<Object, Object>> devicesToSync = getEntitiesWithGuidForSyncing(allEquips);
        int totalNumberOfDevicePoints = 0;
        for (HashMap<Object, Object> deviceToSync : devicesToSync) {
            String deviceId = deviceToSync.get("id").toString();
            if (!CCUHsApi.getInstance().hasEntitySynced(deviceId)) {
                continue;
            }
            List<HashMap<Object, Object>> allPointsForDevice = ccuHsApi.readAllEntities("point and his and deviceRef == \""+ deviceId +"\"");
            CcuLog.d(TAG,"Found " + allPointsForDevice.size() + " device points");
            List<HashMap<Object, Object>> pointsToSyncForDevice = getEntitiesWithGuidForSyncing(allPointsForDevice);
            totalNumberOfDevicePoints = totalNumberOfDevicePoints + pointsToSyncForDevice.size();
            CcuLog.d(TAG,"Found " + pointsToSyncForDevice.size() + " device points that have a GUID for syncing");
            if (!pointsToSyncForDevice.isEmpty()) {
                syncPoints(deviceId, pointsToSyncForDevice, timeForQuarterHourSync, SYNC_TYPE_DEVICE, numberOfHisEntryPerPoint);
            }
        }
        numberOfPoints = numberOfPoints + totalNumberOfDevicePoints;
    }


    private void syncPoints(String deviceOrEquipGuid, List<HashMap<Object, Object>> pointList, boolean timeForQuarterHourSync, String syncType, int numberOfHisEntryPerPoint) {

        List<HisItem> hisItemsToSyncForDeviceOrEquip = new ArrayList<>();
        List<HDict> hDictList = new ArrayList<>();
        Date quarterHourSyncDateTimeForDeviceOrEquip = new Date(System.currentTimeMillis());

        for (HashMap<Object, Object> pointToSync : pointList) {
            if (pointToSync.get("tz") == null) {
                CcuLog.e(TAG," His point without TZ cannot be synced "+pointToSync);
                continue;
            }
            String pointID = pointToSync.get("id").toString();
            List<HisItem> unsyncedHisItems;
            String pointDescription = pointToSync.get("dis").toString();

            if (pointToSync.get("tz") == null) {
                CcuLog.d(TAG, "Invalid point without tz "+pointToSync);
                continue;
            }
            String pointTimezone = pointToSync.get("tz").toString();
    
            String kind = pointToSync.get("kind").toString();
            if (kind.equals(Kind.STRING.getValue())) {
                //None of 'his' points are expected to have a string value.
                continue;
            }
            
            boolean isBooleanPoint = ((HStr) pointToSync.get("kind")).val.equals("Bool");


            unsyncedHisItems = ccuHsApi.tagsDb.getUnsyncedHisItemsOrderDesc(pointID, numberOfHisEntryPerPoint);

            if (!unsyncedHisItems.isEmpty()) {
                for (HisItem hisItem : unsyncedHisItems) {

                    HVal pointValue = isBooleanPoint ? HBool.make(hisItem.getVal() > 0) : HNum.make(hisItem.getVal());
                    long pointTimestamp = hisItem.getDateInMillis();

                    if (!StringUtils.equals("NaN", pointValue.toString())) {
                        HDict hDict = buildHDict(pointID, pointTimezone, pointValue, pointTimestamp);
                        hDictList.add(hDict);
                        hisItemsToSyncForDeviceOrEquip.add(hisItem);
                        CcuLog.d(TAG,"Adding historized point value for point ID " + pointID + "; description of "
                                     + pointDescription + "; value of " + pointValue + " for syncing.");
                    } else {
                        CcuLog.e(TAG, "Historized point value for point ID " + pointID + "; description of "
                                      + pointDescription + " is null. Skipping.");
                    }
                }
            } else if (timeForQuarterHourSync && !skipForcedHisWrites(pointToSync)) {

                HisItem latestHisItemToReSync = ccuHsApi.tagsDb.getLastHisItem(HRef.copy(pointID));
                if (latestHisItemToReSync != null) {
                    latestHisItemToReSync.setDate(quarterHourSyncDateTimeForDeviceOrEquip);

                    HVal pointValue = isBooleanPoint ? HBool.make(latestHisItemToReSync.getVal() > 0) : HNum.make(latestHisItemToReSync.getVal());
                    long pointTimestamp = latestHisItemToReSync.getDateInMillis();

                    HDict hDict = buildHDict(pointID, pointTimezone, pointValue, pointTimestamp);
                    hDictList.add(hDict);
                    CcuLog.d(TAG,
                             "There are no unsynced historized items for point " + pointID +  "-" +pointToSync.get("dis")+
                                            " :resyncing with time of " + quarterHourSyncDateTimeForDeviceOrEquip + "; value of " + pointValue);
                } else {
                    HisItem hisItem = new HisItem();
                    hisItem.setDate(new Date(System.currentTimeMillis()));
                    hisItem.setRec(pointID);
                    hisItem.setVal(ccuHsApi.readPointPriorityVal(pointID));
                    HVal pointValue = isBooleanPoint ? HBool.make(hisItem.getVal() > 0)
                            : HNum.make(hisItem.getVal());
                    long pointTimestamp = hisItem.getDateInMillis();
                    HDict hDict = buildHDict(pointID, pointTimezone, pointValue, pointTimestamp);
                    hDictList.add(hDict);
                    HisItemCache.getInstance().add(pointID, hisItem);
                    CcuLog.d(TAG,"LastSyncItem is empty for "+pointToSync.get("dis")+" and value" +
                            " is retrieved from writable array "+hisItem.getVal());
                }
            }
            totalHisItemCount = totalHisItemCount + hisItemsToSyncForDeviceOrEquip.size();
            if (hisItemsToSyncForDeviceOrEquip.size() > HIS_ITEM_BATCH_SIZE) {
                doHisWrite(hDictList, syncType, deviceOrEquipGuid, hisItemsToSyncForDeviceOrEquip );
                hisItemsToSyncForDeviceOrEquip = new ArrayList<>();
                hDictList = new ArrayList<>();
            }
        }

        if (!hDictList.isEmpty()) {
            doHisWrite(hDictList, syncType, deviceOrEquipGuid,hisItemsToSyncForDeviceOrEquip );
        }
    }
    
    private void doHisWrite(List<HDict> hDictList, String syncType, String deviceOrEquipGuid,
                               List<HisItem> hisItemList) {
        if (CCUHsApi.getInstance().getAuthorised()) {

            HDict[] hDicts = hDictListToArray(hDictList);

            HDict hisWriteMetadata = new HDictBuilder()
                    .add("id", HRef.make(StringUtils.stripStart(deviceOrEquipGuid, "@")))
                    .add(syncType)
                    .toDict();

            EntitySyncResponse response = CCUHsApi.getInstance().hisWriteManyToHaystackService(hisWriteMetadata, hDicts);

            CcuLog.e(TAG, "response " + response.getRespCode() + " : " + response.getErrRespString());
            if (response.getRespCode() == HttpUtil.HTTP_RESPONSE_OK && !hisItemList.isEmpty()) {
                try {
                    ccuHsApi.tagsDb.updateHisItemSynced(hisItemList);
                } catch (IllegalArgumentException | OnErrorNotImplementedException e) {
                    /* There is a corner case where this HisItem might have been removed from Objectbox since the
                     * PruneJob runs on a different thread. Object box throws IllegalArgumentException in that
                     * situation.It appears to be safe to ignore now. But we will still track by printing the stack trace to
                     * monitor how frequently this is happening or there is more to it than what we see now.
                     */
                     /*
                        We are catching OnErrorNotImplementedException
                        This commit transaction is not happening for the below reasons:
                        - reports error code 30 (file is in read only)
                        - reports when db is full.
                     */
                    CcuLog.e(TAG, "Failed to update HisItem !", e);
                }
            } else if (response.getRespCode() == HttpUtil.HTTP_RESPONSE_ERR_REQUEST) {
                CcuLog.e(TAG, "His write failed! , Trying to handle the error");
                EntitySyncErrorHandler.handle400HttpError(ccuHsApi, response.getErrRespString());
                ccuHsApi.tagsDb.updateHisItemSynced(hisItemList);
            }
        }
    }
    
    //These points need not be synced unless there is a 'history' entry
    private boolean skipForcedHisWrites(HashMap<Object, Object> pointToSync) {
        return pointToSync.containsKey("heartbeat")
                || pointToSync.containsKey("rssi")
                || (pointToSync.containsKey("system") && pointToSync.containsKey("clock"))
                || (pointToSync.containsKey("occupancy") && pointToSync.containsKey("detection"))
                || pointToSync.containsKey("sensor") && !pointToSync.containsKey("modbus")
                || (pointToSync.containsKey("outside") && pointToSync.containsKey("temp")
                && pointToSync.containsKey("system"));
    }

    private HDict[] hDictListToArray(List<HDict> hDictList) {
        HDict[] hDicts = null;

        if (hDictList != null && !hDictList.isEmpty()) {
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

        return new HDictBuilder()
                .add("id", HRef.make(StringUtils.stripStart(pointGuid,"@")))
                .add("val", pointValue)
                .add("ts", hDateTime)
                .toDict();
    }

    private List<HashMap<Object, Object>> getEntitiesWithGuidForSyncing(List<HashMap<Object, Object>> entityList) {
        List<HashMap<Object, Object>> entitiesWithGuid = new ArrayList<>();

        for (HashMap<Object, Object> entity : entityList) {
            if (CCUHsApi.getInstance().hasEntitySynced(entity.get("id").toString())) {
                entitiesWithGuid.add(entity);
            } else if (!entitySyncRequired) {
                entitySyncRequired = true;
            }
        }

        return entitiesWithGuid;
    }
    
    public void doPurge() {
        
        if (purgeStatus) {
            CcuLog.d(TAG, "doPurge pending:  Skipped ");
            return;
        }

        Thread purgeThread = new Thread() {
            @Override public void run() {
                super.run();
                purgeStatus = true;
                CcuLog.d(TAG, "doPurge ->");
                try {
                    ArrayList<HashMap<Object, Object>> allHisPoints = ccuHsApi.readAllEntities("point and his");

                    int backFillDurationSelected = getBackFillDurationSelected(ccuHsApi);

                    for (HashMap<Object, Object> point : allHisPoints) {
                        ccuHsApi.tagsDb.removeExpiredHisItems(HRef.copy(point.get("id").toString()), backFillDurationSelected);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.d(TAG, "doPurge -> Failed" , e);
                } finally {
                    purgeStatus = false;
                }
                CcuLog.d(TAG, "<- doPurge");
                lastPurgeTime = System.currentTimeMillis();
            }
        };
        purgeThread.start();
    }

    private int getNumberOfHisEntriesPerPoint() {
        return 50;
    }

    private int getBackFillDurationSelected(CCUHsApi ccuHsApi) {
        int backFillDurationSelected = 24;
        String backFillQuery = "backfill and duration";
        if (!ccuHsApi.readEntity(backFillQuery).isEmpty()) {
            backFillDurationSelected = ccuHsApi.readDefaultVal(backFillQuery).intValue();
        }
        return backFillDurationSelected;
    }

    public void scheduleSync(boolean syncAll, long delaySeconds) {
        if (mSyncTimerTask != null) {
            return;
        }
        
        mSyncTimerTask = new TimerTask() {
            public void run() {
                CcuLog.i(TAG, "His Sync Scheduled");
                doSync(syncAll);
                mSyncTimerTask = null;
            }
        };
        mSyncTimer.schedule(mSyncTimerTask, delaySeconds);
    }
    
    public void setNonCovSyncPending() {
        nonCovSyncPending = true;
    }
    
}
