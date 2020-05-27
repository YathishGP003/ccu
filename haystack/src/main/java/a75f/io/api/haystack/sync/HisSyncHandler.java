package a75f.io.api.haystack.sync;

import org.joda.time.DateTime;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HTimeZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HisSyncHandler
{
    private static final String TAG = "CCU_HS_SYNC";
    
    CCUHsApi ccuHsApi;
    
    public boolean entitySyncRequired = false;
    
    public HisSyncHandler(CCUHsApi api) {
        ccuHsApi = api;
    }
    
    public synchronized void sync() {
        CcuLog.d(TAG, "doHisSync ->");

        DateTime now = new DateTime();
        boolean timeForQuarterHourSync = now.getMinuteOfDay() % 15 == 0 ? true : false;

        if (CCUHsApi.getInstance().isCCURegistered() && CCUHsApi.getInstance().isNetworkConnected() && timeForQuarterHourSync) {
            syncHistorizedEquipPoints(timeForQuarterHourSync);
            syncHistorizedDevicePoints(timeForQuarterHourSync);
        }

        if (entitySyncRequired) {
            CcuLog.d(TAG,"doHisSync : entitySyncRequired");
            CCUHsApi.getInstance().syncEntityTree();
            entitySyncRequired = false;
        }
        CcuLog.d(TAG,"<- doHisSync");
    }

    private void syncHistorizedEquipPoints(boolean timeForQuarterHourSync) {
        List<HashMap> allEquips = ccuHsApi.readAll("equip");
        List<HashMap> equipsToSync = getEntitiesWithGuidForSyncing(allEquips);

        for (HashMap equipToSync : equipsToSync) {
            String equipPointId = equipToSync.get("id").toString();
            List<HashMap> allPointsForEquip = ccuHsApi.readAll("point and his and equipRef == \""+ equipPointId +"\"");
            List<HashMap> pointsToSyncForEquip = getEntitiesWithGuidForSyncing(allPointsForEquip);
            if (!pointsToSyncForEquip.isEmpty()) {
                syncPoints(equipPointId, pointsToSyncForEquip, timeForQuarterHourSync);
            }
        }
    }

    private void syncHistorizedDevicePoints(boolean timeForQuarterHourSync) {
    List<HashMap> allEquips = ccuHsApi.readAll("device");
    List<HashMap> devicesToSync = getEntitiesWithGuidForSyncing(allEquips);

    for (HashMap deviceToSync : devicesToSync) {
        String equipPointId = deviceToSync.get("id").toString();
        List<HashMap> allPointsForDevice = ccuHsApi.readAll("point and his and deviceRef == \""+ equipPointId +"\"");
        List<HashMap> pointsToSyncForDevice = getEntitiesWithGuidForSyncing(allPointsForDevice);
        if (!pointsToSyncForDevice.isEmpty()) {
            syncPoints(equipPointId, pointsToSyncForDevice, timeForQuarterHourSync);
        }
    }
}


    private void syncPoints(String deviceOrEquipId, List<HashMap> pointList, boolean timeForQuarterHourSync) {

        HDict[] hDicts = new HDict[pointList.size()];

        List<HisItem> unsyncedHisItems = new ArrayList<>();
        HisItem[] hisItemsToSync = null;

        for (HashMap pointToSync : pointList) {
            String pointID = pointToSync.get("id").toString();

            unsyncedHisItems = ccuHsApi.tagsDb.getUnsyncedHisItemsOrderDesc(pointID);


            if (unsyncedHisItems.isEmpty() && timeForQuarterHourSync) {
                HisItem hisItemToSync = ccuHsApi.tagsDb.getLastHisItem(HRef.copy(pointID));
                hisItemsToSync = new HisItem[]{hisItemToSync};
            } else {
                hisItemsToSync = new HisItem[unsyncedHisItems.size()];
                int hisItemBuilderIterator = 0;
                for (HisItem hisItemToSync : hisItemsToSync) {
                    hisItemsToSync[hisItemBuilderIterator] = hisItemToSync;
                }
            }

            int hdictBuilderIterator = 0;
            for (HisItem hisItem : hisItemsToSync) {
                if (hisItem != null && hisItem.initialized) {
                    String pointGuid = CCUHsApi.getInstance().getGUID(pointID);
                    String pointTimezone = pointToSync.get("tz").toString();
                    Double pointValue = hisItem.getVal();
                    long pointTimestamp = hisItem.getDateInMillis();

                    HDict hDict = buildHDict(pointGuid, pointTimezone, pointValue, pointTimestamp);
                    hDicts[hdictBuilderIterator] = hDict;
                    hdictBuilderIterator++;
                }
            }
            ccuHsApi.tagsDb.removeExpiredHisItems(HRef.copy(pointID));
        }

        if (!pointList.isEmpty()) {
            HDict hisWriteMetadata = new HDictBuilder()
                    .add("id", deviceOrEquipId)
                    .toDict();

            String response = CCUHsApi.getInstance().hisWriteManyToHaystackService(hisWriteMetadata, hDicts);
            if (response != null) {
                ccuHsApi.tagsDb.updateHisItemSynced(unsyncedHisItems);
            }
        }
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
