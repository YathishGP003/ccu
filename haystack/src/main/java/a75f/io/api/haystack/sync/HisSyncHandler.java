package a75f.io.api.haystack.sync;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HTimeZone;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HisSyncHandler
{
    private static final String TAG = "CCU_HS_SYNC";
    private static final String SYNC_TYPE_EQUIP = "equip";
    private static final String SYNC_TYPE_DEVICE = "device";
    
    CCUHsApi ccuHsApi;
    
    public boolean entitySyncRequired = false;
    
    public HisSyncHandler(CCUHsApi api) {
        ccuHsApi = api;
    }
    
    public synchronized void sync() {
        CcuLog.d(TAG, "doHisSync ->");

        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() > 0) {
            String siteLUID = site.get("id").toString();
            String siteGUID = CCUHsApi.getInstance().getGUID(siteLUID);

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

        CcuLog.d(TAG,"<- doHisSync");
    }

    private void syncHistorizedEquipPoints(boolean timeForQuarterHourSync) {
        List<HashMap> allEquips = ccuHsApi.readAll("equip");
        List<HashMap> equipsToSync = getEntitiesWithGuidForSyncing(allEquips);

        for (HashMap equipToSync : equipsToSync) {
            String equipPointId = equipToSync.get("id").toString();
            List<HashMap> allPointsForEquip = ccuHsApi.readAll("point and his and equipRef == \""+ equipPointId +"\"");
            CcuLog.d(TAG,"Found " + allPointsForEquip.size() + " equip points");
            List<HashMap> pointsToSyncForEquip = getEntitiesWithGuidForSyncing(allPointsForEquip);
            CcuLog.d(TAG,"Found " + pointsToSyncForEquip.size() + " equip points that have a GUID for syncing");
            if (!pointsToSyncForEquip.isEmpty()) {
                syncPoints(equipPointId, pointsToSyncForEquip, timeForQuarterHourSync, SYNC_TYPE_EQUIP);
            }
        }
    }

    private void syncHistorizedDevicePoints(boolean timeForQuarterHourSync) {
    List<HashMap> allEquips = ccuHsApi.readAll("device");
    List<HashMap> devicesToSync = getEntitiesWithGuidForSyncing(allEquips);

        for (HashMap deviceToSync : devicesToSync) {
            String equipPointId = deviceToSync.get("id").toString();
            List<HashMap> allPointsForDevice = ccuHsApi.readAll("point and his and deviceRef == \""+ equipPointId +"\"");
            CcuLog.d(TAG,"Found " + allPointsForDevice.size() + " device points");
            List<HashMap> pointsToSyncForDevice = getEntitiesWithGuidForSyncing(allPointsForDevice);
            CcuLog.d(TAG,"Found " + pointsToSyncForDevice.size() + " device points that have a GUID for syncing");
            if (!pointsToSyncForDevice.isEmpty()) {
                syncPoints(equipPointId, pointsToSyncForDevice, timeForQuarterHourSync, SYNC_TYPE_DEVICE);
            }
        }
    }


    private void syncPoints(String deviceOrEquipId, List<HashMap> pointList, boolean timeForQuarterHourSync, String syncType) {

        List<HDict> hDictList = new ArrayList<>();

        List<HisItem> unsyncedHisItems = new ArrayList<>();
        List<HisItem> hisItemsToSync = new ArrayList<>();

        for (HashMap pointToSync : pointList) {
            String pointID = pointToSync.get("id").toString();

            unsyncedHisItems = ccuHsApi.tagsDb.getUnsyncedHisItemsOrderDesc(pointID);

            if (!unsyncedHisItems.isEmpty()) {
                for (HisItem unsyncedHisItem : unsyncedHisItems) {
                    if (unsyncedHisItem != null) {
                        hisItemsToSync.add(unsyncedHisItem);
                    }
                }
                CcuLog.d(TAG,"Processed " + unsyncedHisItems.size() + " and scheduled " + hisItemsToSync.size() + " items for syncing for point ID " + pointID);
            } else if (timeForQuarterHourSync) {
                CcuLog.d(TAG,"There are no unsynced historized items for point ID " + pointID);
                HisItem latestHisItemToSync = ccuHsApi.tagsDb.getLastHisItem(HRef.copy(pointID));

                if (latestHisItemToSync != null) {
                    hisItemsToSync.add(latestHisItemToSync);
                }
                CcuLog.d(TAG,"Processed latest historized value for point ID " + pointID + " and added " + hisItemsToSync.size() + " for syncing.");
            }

            for (HisItem hisItem : hisItemsToSync) {
                String pointGuid = CCUHsApi.getInstance().getGUID(pointID);
                String pointTimezone = pointToSync.get("tz").toString();
                Double pointValue = hisItem.getVal();
                long pointTimestamp = hisItem.getDateInMillis();

                HDict hDict = buildHDict(pointGuid, pointTimezone, pointValue, pointTimestamp);
                hDictList.add(hDict);
            }

            CcuLog.d(TAG,"Items to sync for point ID " + pointID + " is " + hisItemsToSync.size() + ". Current size of entity historized items to sync is " + hDictList.size());
            ccuHsApi.tagsDb.removeExpiredHisItems(HRef.copy(pointID));
        }



        if (!hDictList.isEmpty()) {
            CcuLog.d(TAG,"Syncing a point list of size " + pointList.size());

            HDict[] hDicts = hDictListToArray(hDictList);

            HDict hisWriteMetadata = new HDictBuilder()
                    .add("id", deviceOrEquipId)
                    .add(syncType)
                    .toDict();

            String response = CCUHsApi.getInstance().hisWriteManyToHaystackService(hisWriteMetadata, hDicts);
            if (response != null) {
                ccuHsApi.tagsDb.updateHisItemSynced(unsyncedHisItems);
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

    private HDict buildHDict(String pointGuid, String pointTimezone, Double pointValue, long pointTimestamp) {

        HTimeZone hTimeZone = HTimeZone.make(pointTimezone);
        HDateTime hDateTime = HDateTime.make(pointTimestamp, hTimeZone);

        HDict hDict = new HDictBuilder()
                .add("id", pointGuid)
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
                entitySyncRequired = true;
            }
        }

        return entitiesWithGuid;
    }
}
