package a75f.io.logic.ccu.restore;

import static a75f.io.logic.bo.util.CCUUtils.isCCUOfflinePropertySet;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.RestoreCCUHsApi;
import a75f.io.api.haystack.RetryCountCallback;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.exception.NullHGridException;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.tuners.TunerEquip;

public class RestoreCCU {

    private RestoreCCUHsApi restoreCCUHsApi = RestoreCCUHsApi.getInstance();
    private final String TAG = "CCU_REPLACE";
    private HGrid ccuHGrid;
    private static final String SYSTEM = "SYSTEM";
    public static final String CONFIG_FILES = "CONFIG_FILES";
    public static final String SYNC_SITE = "SYNC_SITE";
    public static final String CCU_DEVICE = "CCU_DEVICE";
    public static final String SYSTEM_PROFILE = "SYSTEM_PROFILE";
    public static final String ZONES = "ZONES";
    public static final String ZONE_SCHEDULE = "ZONE_SCHEDULE";
    public static final String FLOORS = "FLOORS";
    public static final String PAIRING_ADDRESS = "PAIRING_ADDRESS";



    private HGrid getCcuHGrid(String siteCode, RetryCountCallback retryCountCallback) {
        if(ccuHGrid == null){
            ccuHGrid =  restoreCCUHsApi.getAllCCUs(siteCode, retryCountCallback);
            if(ccuHGrid == null){
                throw new NullHGridException("Null occurred while fetching CCU List.");
            }
        }
        return ccuHGrid;
    }

    private boolean isCCUOnline(Date lastUpdatedDate){
        if(isCCUOfflinePropertySet()){
            return false;
        }
        return new Date().getTime() - lastUpdatedDate.getTime() <= (15*60000);
    }

    public List<CCU> getCCUList(String siteCode, JSONArray ccuArray) {
        CcuLog.i(TAG, "Fetching all the CCU details");
      RetryCountCallback retryCountCallback = retryCount -> CcuLog.d(TAG, "retrying to get CCU list with the retry count "+retryCount);
        HGrid ccuGrid = getCcuHGrid(siteCode, retryCountCallback);
        Iterator ccuGridIterator = ccuGrid.iterator();
        if(!ccuGridIterator.hasNext()){
            return new ArrayList<>();
        }
        List<String> equipRefs = new LinkedList<>();
        /*
        key - ccuId
        Value - equipRef
         */
        Map<String, String> ccuIdMap = new HashMap<>();
        while (ccuGridIterator.hasNext()) {
            HRow ccuRow = (HRow) ccuGridIterator.next();
            String equipRef = ccuRow.get("equipRef").toString();
            equipRefs.add(equipRef);
            ccuIdMap.put(ccuRow.get("id").toString(), equipRef);
        }
        /*
        key - equipRef
        Value - ccuVersion
         */
        Map<String, String> ccuVersionMap = restoreCCUHsApi.getCCUVersion(equipRefs);
        List<CCU> ccuList = new ArrayList<>();
        for (int index = 0; index < ccuArray.length(); index++) {
            try {
                boolean isCCUOnline = false;
                JSONObject ccuDetails = ccuArray.getJSONObject(index);
                String lastUpdatedDatetimeString = ccuDetails.getString("lastUpdatedDatetime").trim();
                if(!lastUpdatedDatetimeString.isEmpty()){
                    Date lastUpdatedDatetime = new Date(HDateTime.make(lastUpdatedDatetimeString).millis());
                    lastUpdatedDatetimeString =
                            new SimpleDateFormat("MMM dd, yyyy | HH:mm:ss").format(lastUpdatedDatetime);
                    isCCUOnline = isCCUOnline(lastUpdatedDatetime);

                }else{
                    lastUpdatedDatetimeString = "n/a";
                }
                String ccuId = ccuDetails.getString("deviceId");
                if(null != ccuVersionMap.get(ccuIdMap.get(ccuId))) {
                    ccuList.add(new CCU(siteCode, ccuId, ccuDetails.getString("deviceName"),
                            ccuVersionMap.get(ccuIdMap.get(ccuId)), lastUpdatedDatetimeString, isCCUOnline));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ccuList.sort(Comparator.comparing(CCU::getName));
        return ccuList;
    }

    public void restoreCCUDevice(CCU ccu){
        RetryCountCallback retryCountCallback = retryCount -> CcuLog.d(TAG, "Retry count while restoring CCU device "+ retryCount);
        restoreCCUHsApi.readCCUDevice(ccu.getCcuId(), retryCountCallback);
        getCCUEquipAndPoints(ccu, retryCountCallback);
        getDiagEquipOfCCU(ccu.getCcuId(), ccu.getSiteCode(), retryCountCallback);
        L.saveCCUState();
    }

    private void getCCUEquipAndPoints(CCU ccu, RetryCountCallback retryCountCallback) {
        HGrid ccuEquipGrid = restoreCCUHsApi.readCCUEquipAndItsPoints(ccu.getCcuId(), ccu.getSiteCode(), retryCountCallback);
        if(ccuEquipGrid == null){
            throw new NullHGridException("Null occurred while fetching diag equip.");
        }
        Iterator ccuEquipGridIterator = ccuEquipGrid.iterator();
        while(ccuEquipGridIterator.hasNext()) {
            HRow ccuEquipRow = (HRow) ccuEquipGridIterator.next();
            getEquipAndPoints(ccuEquipRow, retryCountCallback);
        }
    }


    public void restoreSystemProfile(CCU ccu, AtomicInteger deviceCount, EquipResponseCallback equipResponseCallback,
                                     ReplaceCCUTracker replaceCCUTracker, RetryCountCallback retryCountCallback){
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.SYSTEM_PROFILE, ReplaceStatus.RUNNING.toString());
        getSystemProfileOfCCU(ccu.getCcuId(), ccu.getSiteCode(), retryCountCallback);
        getCMDeviceOfCCU(ccu.getCcuId(), ccu.getSiteCode(), retryCountCallback);
        L.saveCCUState();
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.SYSTEM_PROFILE, ReplaceStatus.COMPLETED.toString());
        equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
    }

    private HGrid getAllEquips(String ccuId, String siteCode){
        RetryCountCallback retryCountCallback = retryCount -> CcuLog.d(TAG, "Retry count while restoring all the equips "+ retryCount);
        String gatewayRef = getGatewayRefFromCCU(ccuId, siteCode, retryCountCallback);
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode, retryCountCallback);
        CcuLog.i("CCU_DEBUG", "getAllEquips: gatewayRef : "+gatewayRef);
        CcuLog.i("CCU_DEBUG", "getAllEquips: ahuRef : "+ahuRef);
        CcuLog.i("CCU_DEBUG", "getAllEquips: ccuId : "+ccuId);
        CcuLog.i("CCU_DEBUG", "getAllEquips: siteCode : "+siteCode);
        return restoreCCUHsApi.getAllEquips(ahuRef, gatewayRef, retryCountCallback);
    }

    private void initReplaceStatusForSystemEntities(SharedPreferences.Editor editor){
        editor.putString(CONFIG_FILES, ReplaceStatus.PENDING.toString());
        editor.putString(SYNC_SITE, ReplaceStatus.PENDING.toString());
        editor.putString(SYSTEM_PROFILE, ReplaceStatus.PENDING.toString());
        editor.putString(ZONES, ReplaceStatus.PENDING.toString());
        editor.putString(ZONE_SCHEDULE, ReplaceStatus.PENDING.toString());
        editor.putString(FLOORS, ReplaceStatus.PENDING.toString());
        editor.putString(PAIRING_ADDRESS, ReplaceStatus.PENDING.toString());
    }

    public Map<String, Set<String>> getEquipDetailsOfCCU(String ccuId, String siteCode,
                                                         SharedPreferences.Editor editor, boolean isReplaceClosed){

        HGrid equipGrid = getAllEquips(ccuId, siteCode);
        if(equipGrid == null){
            throw new NullHGridException("Null occurred while fetching count of Zone equips.");
        }
        Iterator equipGridIterator = equipGrid.iterator();
        Map<String, Set<String>> floorAndZoneIds = new HashMap<>();
        Set<String> floorRefSet = new HashSet<>();
        Set<String> roomRefSet = new HashSet<>();
        if(!isReplaceClosed) {
            initReplaceStatusForSystemEntities(editor);
        }
        while(equipGridIterator.hasNext()) {
            HRow equipRow = (HRow) equipGridIterator.next();
            String equipID = equipRow.get(Tags.ID).toString();
            if(!isReplaceClosed) {
                editor.putString(equipID, ReplaceStatus.PENDING.toString());
            }
            if(equipRow.has(Tags.ROOMREF) && !equipRow.get(Tags.ROOMREF).toString().contains(SYSTEM)){
                roomRefSet.add(equipRow.get(Tags.ROOMREF).toString());
            }
            if(equipRow.has(Tags.FLOORREF) && !equipRow.get(Tags.FLOORREF).toString().contains(SYSTEM)){
                floorRefSet.add(equipRow.get(Tags.FLOORREF).toString());
            }
        }
        floorAndZoneIds.put(Tags.ROOMREF, roomRefSet);
        floorAndZoneIds.put(Tags.FLOORREF, floorRefSet);
        editor.commit();
        return floorAndZoneIds;
    }

    private String getAhuRefFromCCU(String ccuId, String siteCode, RetryCountCallback retryCountCallback){
        HGrid ccuGrid = getCcuHGrid(siteCode, retryCountCallback);
        Iterator ccuGridIterator = ccuGrid.iterator();
        while (ccuGridIterator.hasNext()) {
            HRow ccuRow = (HRow) ccuGridIterator.next();
            if(ccuId.equals(ccuRow.get("id").toString())){
                return ccuRow.get("ahuRef").toString();
            }
        }
        return null;
    }

    private String getGatewayRefFromCCU(String ccuId, String siteCode, RetryCountCallback retryCountCallback){
        HGrid ccuGrid = getCcuHGrid(siteCode, retryCountCallback);
        Iterator ccuGridIterator = ccuGrid.iterator();
        while (ccuGridIterator.hasNext()) {
            HRow ccuRow = (HRow) ccuGridIterator.next();
            if(ccuId.equals(ccuRow.get("id").toString())){
                return ccuRow.get("gatewayRef").toString();
            }
        }
        return null;
    }
    private String getEquipRefFromCCU(String ccuId, String siteCode, RetryCountCallback retryCountCallback){
        HGrid ccuGrid = getCcuHGrid(siteCode, retryCountCallback);
        Iterator ccuGridIterator = ccuGrid.iterator();
        while (ccuGridIterator.hasNext()) {
            HRow ccuRow = (HRow) ccuGridIterator.next();
            if(ccuId.equals(ccuRow.get("id").toString())){
                return ccuRow.get("equipRef").toString();
            }
        }
        return null;
    }

    public void getSystemProfileOfCCU(String ccuId, String siteCode, RetryCountCallback retryCountCallback){
        CcuLog.i(TAG, "Restoring system profile of CCU started");
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode, retryCountCallback);
        HGrid systemGrid = restoreCCUHsApi.getCCUSystemEquip(ahuRef, retryCountCallback);
        if (systemGrid == null){
            throw new NullHGridException("Null occurred while fetching system profile.");
        }
        Iterator systemGridIterator = systemGrid.iterator();
        while(systemGridIterator.hasNext()){
            HRow systemEquipRow = (HRow) systemGridIterator.next();
            getEquipAndPoints(systemEquipRow, retryCountCallback);
        }
        CcuLog.i(TAG, "Restoring system profile of CCU completed");
    }

    public void getCMDeviceOfCCU(String ccuId, String siteCode, RetryCountCallback retryCountCallback){
        CcuLog.i(TAG, "Restoring CM device is started");
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode, retryCountCallback);
        HGrid cmDeviceGrid = restoreCCUHsApi.getDevice(ahuRef, retryCountCallback);
        if(cmDeviceGrid == null){
            throw new NullHGridException("Null occurred while fetching CM device details.");
        }
        Iterator cmDeviceGridIterator = cmDeviceGrid.iterator();
        while(cmDeviceGridIterator.hasNext()) {
            HRow cmDeviceRow = (HRow) cmDeviceGridIterator.next();
            getDeviceAndPoints(cmDeviceRow, retryCountCallback);
        }
        CcuLog.i(TAG, "Restoring CM device is completed");
    }

    public void getDiagEquipOfCCU(String ccuId, String siteCode, RetryCountCallback retryCountCallback){
        getDiagEquipOfCCU(getEquipRefFromCCU(ccuId, siteCode, retryCountCallback), retryCountCallback);
    }

    public void getDiagEquipOfCCU(String equipRef, RetryCountCallback retryCountCallback){
        CcuLog.i(TAG, "Restoring Diag equip is started");
        HGrid diagEquipGrid = restoreCCUHsApi.getDiagEquipByEquipId(equipRef, retryCountCallback);
        if(diagEquipGrid == null){
            throw new NullHGridException("Null occurred while fetching diag equip.");
        }
        Iterator diagEquipGridIterator = diagEquipGrid.iterator();
        while(diagEquipGridIterator.hasNext()) {
            HRow diagEquipRow = (HRow) diagEquipGridIterator.next();
            getEquipAndPoints(diagEquipRow, retryCountCallback);
        }
        CcuLog.i(TAG, "Restoring Diag equip is completed");

    }
//
    public void restoreEquip(String equipId, AtomicInteger deviceCount, EquipResponseCallback equipResponseCallback,
                             ReplaceCCUTracker replaceCCUTracker, RetryCountCallback retryCountCallback){
        replaceCCUTracker.updateReplaceStatus(equipId, ReplaceStatus.RUNNING.toString());
        HGrid equipGrid = getEquipGrid(equipId, retryCountCallback);
        if(equipGrid  == null){
            throw new NullHGridException("Null occurred while fetching modbus");
        }
        if(restoreCCUHsApi.isCCUEquip(equipId, DomainName.ccuConfiguration)){
            /*For ccu Equip there is no device mapped so just complete the replace process
            * once equip is imported*/
            replaceCCUTracker.updateReplaceStatus(equipId, ReplaceStatus.COMPLETED.toString());
            equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
        } else{
            getDeviceFromEquip(equipId, equipGrid, deviceCount, equipResponseCallback, replaceCCUTracker,
                    retryCountCallback);
        }
        L.saveCCUState();
    }

    public void setStartingPairAddress(AtomicInteger deviceCount, EquipResponseCallback equipResponseCallback,
                                       ReplaceCCUTracker replaceCCUTracker){
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.PAIRING_ADDRESS, ReplaceStatus.RUNNING.toString());
        HashMap pointMaps = (HashMap) Domain.readPoint(DomainName.addressBand);
        int pairingAddress = 1000;
        if(pointMaps.isEmpty()){
            ArrayList<HashMap<Object, Object>> devices = CCUHsApi.getInstance().readAllEntities("device and addr");
            for(HashMap device : devices){
                int devicePairingAddress  = Integer.parseInt(device.get("addr").toString());
                if(devicePairingAddress > pairingAddress){
                    pairingAddress = devicePairingAddress;
                    break;
                }
            }
            pairingAddress = (pairingAddress/100) * 100;
        } else {
            pairingAddress = CCUHsApi.getInstance().readDefaultValById(pointMaps.get("id").toString()).intValue();
        }
        L.ccu().setAddressBand((short) pairingAddress);
        L.saveCCUState();
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.PAIRING_ADDRESS, ReplaceStatus.COMPLETED.toString());
        equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
    }
    public void restoreFloors(Set floorRefSet, AtomicInteger deviceCount, EquipResponseCallback equipResponseCallback,
                              ReplaceCCUTracker replaceCCUTracker, RetryCountCallback retryCountCallback){
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.FLOORS, ReplaceStatus.RUNNING.toString());
        restoreCCUHsApi.importFloors(floorRefSet, retryCountCallback);
        L.saveCCUState();
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.FLOORS, ReplaceStatus.COMPLETED.toString());
        equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
    }

    public void restoreZones(Set roomRefSet, AtomicInteger deviceCount, EquipResponseCallback equipResponseCallback,
                             ReplaceCCUTracker replaceCCUTracker, RetryCountCallback retryCountCallback){
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.ZONES, ReplaceStatus.RUNNING.toString());
        restoreCCUHsApi.importZones(roomRefSet, retryCountCallback);
        L.saveCCUState();
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.ZONES, ReplaceStatus.COMPLETED.toString());
        equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
    }

    private void getDeviceFromEquip(String equipId, HGrid equipGrid, AtomicInteger deviceCount,
                                    EquipResponseCallback equipResponseCallback, ReplaceCCUTracker replaceCCUTracker,
                                    RetryCountCallback retryCountCallback) {
        if(equipGrid != null){
            Iterator equipGridIterator = equipGrid.iterator();
            while(equipGridIterator.hasNext()){
                HRow equipRow = (HRow) equipGridIterator.next();
                getEquipAndPoints(equipRow, retryCountCallback);
                if(equipRow.has(Tags.MODBUS)){
                    HGrid subEquipGrid = restoreCCUHsApi.getModBusSubEquips(equipId, retryCountCallback);
                    if(subEquipGrid == null){
                        throw new NullHGridException("Null occurred while fetching subequip details for "+ equipId);
                    }
                    Iterator subEquipGridIterator = subEquipGrid.iterator();
                    List<HRow> subEquipRowList = new ArrayList<>();
                    while(subEquipGridIterator.hasNext()){
                        HRow subEquipRow = (HRow) subEquipGridIterator.next();
                        subEquipRowList.add(subEquipRow);
                        getEquipAndPoints(subEquipRow, retryCountCallback);
                        HGrid deviceGrid = restoreCCUHsApi.getDevice( subEquipRow.get(Tags.ID).toString(), retryCountCallback);
                        if(deviceGrid == null){
                            throw new NullHGridException("Null occurred while fetching device.");
                        }
                        Iterator deviceGridIterator = deviceGrid.iterator();
                        while(deviceGridIterator.hasNext()) {
                            HRow zoneDeviceRow = (HRow) deviceGridIterator.next();
                            getDeviceAndPoints(zoneDeviceRow, retryCountCallback);
                        }
                    }
                }
                else if (equipRow.has(Tags.BACNET)) { // For Bacnet Client Equipment we didn't have device.
                    replaceCCUTracker.updateReplaceStatus(equipId, ReplaceStatus.COMPLETED.toString());
                    equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
                    return;
                }
                getDevicesFromEquips(equipId, equipRow.get(Tags.ID).toString(), deviceCount, equipResponseCallback,
                        replaceCCUTracker, retryCountCallback);
            }
        }
    }

    private HGrid getEquipGrid(String equipId, RetryCountCallback retryCountCallback){
        return restoreCCUHsApi.getEquip(equipId, retryCountCallback);
    }

    private void getDevicesFromEquips(String equipId, String equipRef, AtomicInteger deviceCount,
                                      EquipResponseCallback equipResponseCallback,
                                      ReplaceCCUTracker replaceCCUTracker,RetryCountCallback retryCountCallback){
        HGrid deviceGrid = restoreCCUHsApi.getDevice(equipRef, retryCountCallback);
        if(deviceGrid == null){
            throw new NullHGridException("Null occurred while fetching device.");
        }
        Iterator deviceGridIterator = deviceGrid.iterator();
        while(deviceGridIterator.hasNext()) {
            HRow zoneDeviceRow = (HRow) deviceGridIterator.next();
            getDeviceAndPoints(zoneDeviceRow, retryCountCallback);
            replaceCCUTracker.updateReplaceStatus(equipId, ReplaceStatus.COMPLETED.toString());
            equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
        }
    }

    private void getDeviceAndPoints(HRow deviceRow, RetryCountCallback retryCountCallback){
        restoreCCUHsApi.importDevice(deviceRow, retryCountCallback);
    }

    private void getEquipAndPoints(HRow equipRow, RetryCountCallback retryCountCallback){
        restoreCCUHsApi.importEquip(equipRow, retryCountCallback);
    }

    public void getZoneSchedules(Set<String> roomRefSet, AtomicInteger deviceCount,
                                 EquipResponseCallback equipResponseCallback, ReplaceCCUTracker replaceCCUTracker,
                                 RetryCountCallback retryCountCallback){
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.ZONE_SCHEDULE, ReplaceStatus.RUNNING.toString());
        restoreCCUHsApi.importZoneSchedule(roomRefSet, retryCountCallback);
        restoreCCUHsApi.importNamedSchedule(retryCountCallback);
        restoreCCUHsApi.importZoneSpecialSchedule(roomRefSet, retryCountCallback);
        restoreCCUHsApi.importSchedulablePoints(roomRefSet,retryCountCallback);

        L.saveCCUState();
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.ZONE_SCHEDULE, ReplaceStatus.COMPLETED.toString());
        equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
    }

    public void syncExistingSite(String siteCode, AtomicInteger deviceCount, EquipResponseCallback equipResponseCallback,
                                 ReplaceCCUTracker replaceCCUTracker, RetryCountCallback retryCountCallback){
        CcuLog.i(TAG, "Saving site details started");
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.SYNC_SITE, ReplaceStatus.RUNNING.toString());
        restoreCCUHsApi.syncExistingSite(siteCode, retryCountCallback);
        TunerEquip.INSTANCE.initialize(CCUHsApi.getInstance(), false);
        TunerEquip.INSTANCE.syncBuildingTuners(CCUHsApi.getInstance());
        L.saveCCUState();
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.SYNC_SITE, ReplaceStatus.COMPLETED.toString());
        equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
        CcuLog.i(TAG, "Saving site details completed");
    }

    public static boolean isReplaceCCUUnderProcess(){
        ReplaceCCUTracker replaceCCUTracker = new ReplaceCCUTracker();
        ConcurrentHashMap<String, ?> currentReplacementProgress =
                new ConcurrentHashMap<> (replaceCCUTracker.getReplaceCCUStatus());
        for (String equipId : currentReplacementProgress.keySet()) {
            if(currentReplacementProgress.get(equipId).toString().equals(ReplaceStatus.RUNNING.toString())){
                replaceCCUTracker.updateReplaceStatus(equipId, ReplaceStatus.PENDING.toString());
                return true;
            }
            else if(currentReplacementProgress.get(equipId).toString().equals(ReplaceStatus.PENDING.toString())){
                return true;
            }
        }return false;
    }

    public static boolean isReplaceCCUCompleted(){
        ReplaceCCUTracker replaceCCUTracker = new ReplaceCCUTracker();
        ConcurrentHashMap<String, ?> currentReplacementProgress =
                new ConcurrentHashMap<> (replaceCCUTracker.getReplaceCCUStatus());
        for (String equipId : currentReplacementProgress.keySet()) {
            if(currentReplacementProgress.get(equipId).toString().equals(ReplaceStatus.RUNNING.toString())){
                return false;
            }
            else if(currentReplacementProgress.get(equipId).toString().equals(ReplaceStatus.PENDING.toString())){
                return false;
            }
        }
        return true;
    }

}
