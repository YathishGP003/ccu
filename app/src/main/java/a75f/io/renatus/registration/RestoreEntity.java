package a75f.io.renatus.registration;


import android.util.Log;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import a75f.io.api.haystack.RetryCountCallback;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.ccu.restore.CCU;
import a75f.io.logic.ccu.restore.EquipResponseCallback;
import a75f.io.logic.ccu.restore.ReplaceCCUTracker;
import a75f.io.logic.ccu.restore.RestoreCCU;

public class RestoreEntity implements Runnable{
    private CCU ccu;
    private RestoreCCU restoreCCU;
    private String equipId;
    private Map<String, Set<String>> floorAndZoneIds;
    private EquipResponseCallback equipResponseCallback;
    private RetryCountCallback retryCountCallback;
    private AtomicInteger deviceCount;
    private ReplaceCCUTracker replaceCCUTracker;

    public RestoreEntity(CCU ccu, RestoreCCU restoreCCU, String equipId, Map<String, Set<String>> floorAndZoneIds,
                         EquipResponseCallback equipResponseCallback, AtomicInteger deviceCount,
                         ReplaceCCUTracker replaceCCUTracker, RetryCountCallback retryCountCallback){
        this.ccu = ccu;
        this.restoreCCU = restoreCCU;
        this.equipId = equipId;
        this.floorAndZoneIds = floorAndZoneIds;
        this.equipResponseCallback = equipResponseCallback;
        this.deviceCount = deviceCount;
        this.replaceCCUTracker = replaceCCUTracker;
        this.retryCountCallback = retryCountCallback;
    }


    @Override
    public void run() {
        try{
            switch (equipId){
                case RestoreCCU.CCU_DEVICE:
                //restoreCCU.restoreCCUDevice(ccu, deviceCount, equipResponseCallback, replaceCCUTracker);
                    break;
                case RestoreCCU.SYSTEM_PROFILE:
                    restoreCCU.restoreSystemProfile(ccu, deviceCount, equipResponseCallback, replaceCCUTracker, retryCountCallback);
                    break;
                case RestoreCCU.ZONES:
                    if(floorAndZoneIds.containsKey(Tags.ROOMREF)){
                        restoreCCU.restoreZones(floorAndZoneIds.get(Tags.ROOMREF), deviceCount, equipResponseCallback,
                                replaceCCUTracker, retryCountCallback);
                    }
                    break;
                case RestoreCCU.ZONE_SCHEDULE:
                    if(floorAndZoneIds.containsKey(Tags.ROOMREF)){
                        restoreCCU.getZoneSchedules(floorAndZoneIds.get(Tags.ROOMREF), deviceCount, equipResponseCallback,
                            replaceCCUTracker, retryCountCallback);
                    }
                    break;
                case RestoreCCU.FLOORS:
                    if(floorAndZoneIds.containsKey(Tags.FLOORREF)){
                        restoreCCU.restoreFloors(floorAndZoneIds.get(Tags.FLOORREF), deviceCount, equipResponseCallback,
                            replaceCCUTracker, retryCountCallback);
                    }
                    break;
                case RestoreCCU.PAIRING_ADDRESS:
                    restoreCCU.setStartingPairAddress(deviceCount, equipResponseCallback, replaceCCUTracker);
                    break;
                default:
                    restoreCCU.restoreEquip(equipId, deviceCount, equipResponseCallback, replaceCCUTracker, retryCountCallback);
                    break;
            }
        } catch(Exception ex){
            CcuLog.e(L.TAG_CCU_REPLACE, "Thread interrupted "+ Log.getStackTraceString(ex));
        }
    }
}
